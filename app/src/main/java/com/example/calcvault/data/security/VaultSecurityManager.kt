package com.example.calcvault.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.calcvault.data.storage.VaultFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class VaultSecurityManager(private val context: Context) {

    private val fileManager: VaultFileManager by lazy {
        VaultFileManager(context)
    }

    private val prefs: SharedPreferences by lazy {
        initSecurePrefs()
    }

    private val _screenGuardFlow by lazy {
        MutableStateFlow(prefs.getBoolean(KEY_SCREEN_GUARD, false))
    }
    val screenGuardFlow: StateFlow<Boolean> get() = _screenGuardFlow

    private val _allowScreenshotsFlow by lazy {
        MutableStateFlow(prefs.getBoolean(KEY_ALLOW_SCREENSHOTS, true))
    }
    val allowScreenshotsFlow: StateFlow<Boolean> get() = _allowScreenshotsFlow

    private val _themeIdFlow by lazy {
        MutableStateFlow(prefs.getString(KEY_THEME_ID, "midnight") ?: "midnight")
    }
    val themeIdFlow: StateFlow<String> get() = _themeIdFlow

    private val _appLanguageFlow by lazy {
        MutableStateFlow(prefs.getString(KEY_APP_LANGUAGE, "en") ?: "en")
    }
    val appLanguageFlow: StateFlow<String> get() = _appLanguageFlow

    val appLanguage: String
        get() = prefs.getString(KEY_APP_LANGUAGE, "en") ?: "en"

    fun setAppLanguage(lang: String) {
        val selected = if (lang == "bn") "bn" else "en"
        prefs.edit().putString(KEY_APP_LANGUAGE, selected).apply()
        _appLanguageFlow.value = selected
    }

    private val _customWallpaperPathFlow by lazy {
        MutableStateFlow(prefs.getString(KEY_CUSTOM_WALLPAPER_PATH, null))
    }
    val customWallpaperPathFlow: StateFlow<String?> get() = _customWallpaperPathFlow

    // Tracks if a system picker (audio, photo, video, Google Sign-in, backup document) is currently launched
    @Volatile
    private var externalPickerActive: Boolean = false

    var isExternalActivityActive: Boolean
        get() = externalPickerActive
        set(value) {
            externalPickerActive = value
        }

    fun extendExternalActivityGracePeriod(ms: Long = 0L) {
        // Keep external activity strictly confined to active system picker lifecycle
    }

    fun clearExternalActivity() {
        externalPickerActive = false
    }

    private fun initSecurePrefs(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "calc_vault_encrypted_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w("VaultSecurityManager", "Fallback to standard hashed prefs due to keystore: ${e.message}")
            context.getSharedPreferences("calc_vault_fallback_prefs", Context.MODE_PRIVATE)
        }
    }

    fun isPinConfigured(): Boolean {
        val pin = prefs.getString(KEY_PIN_HASH, null)
        return !pin.isNullOrEmpty()
    }

    fun getExpectedPinLength(): Int {
        val configured = prefs.getInt(KEY_PIN_LENGTH, 0)
        if (configured > 0) return configured
        val persistent = fileManager.readVaultMeta()?.pinLength
        if (persistent != null && persistent > 0) return persistent
        return 4
    }

    fun setupPin(pin: String): Boolean {
        if (pin.length < 4) return false
        val hash = hashPin(pin)
        val success = prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putInt(KEY_PIN_LENGTH, pin.length)
            .commit()
        if (success) {
            syncMetaToDisk()
        }
        return success
    }

    fun verifyPin(enteredPin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null)
        if (storedHash != null) {
            return hashPin(enteredPin) == storedHash
        }
        // If not in local prefs yet, check persistent vault backup
        val meta = fileManager.readVaultMeta()
        if (meta != null && hashPin(enteredPin) == meta.pinHash) {
            restoreFromPersistentVault(enteredPin)
            return true
        }
        return false
    }

    fun detectPersistentVaultOnDevice(): Boolean {
        if (isPinConfigured()) return false
        return fileManager.hasExistingVaultData()
    }

    fun restoreFromPersistentVault(enteredPin: String): Boolean {
        val meta = fileManager.readVaultMeta()
        if (meta != null && hashPin(enteredPin) == meta.pinHash) {
            prefs.edit()
                .putString(KEY_PIN_HASH, meta.pinHash)
                .putInt(KEY_PIN_LENGTH, meta.pinLength)
                .apply()
            if (!meta.securityQuestion.isNullOrEmpty() && !meta.securityAnswerHash.isNullOrEmpty()) {
                prefs.edit()
                    .putString(KEY_SECURITY_QUESTION, meta.securityQuestion)
                    .putString(KEY_SECURITY_ANSWER_HASH, meta.securityAnswerHash)
                    .apply()
            }
            return true
        }
        return false
    }

    private fun syncMetaToDisk() {
        try {
            val hash = prefs.getString(KEY_PIN_HASH, null) ?: return
            val length = prefs.getInt(KEY_PIN_LENGTH, 4)
            val question = prefs.getString(KEY_SECURITY_QUESTION, null)
            val answerHash = prefs.getString(KEY_SECURITY_ANSWER_HASH, null)
            fileManager.saveVaultMeta(hash, length, question, answerHash)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setSecurityQuestion(question: String, answer: String): Boolean {
        if (question.isBlank() || answer.isBlank()) return false
        val answerHash = hashPin(answer.trim().lowercase())
        val success = prefs.edit()
            .putString(KEY_SECURITY_QUESTION, question.trim())
            .putString(KEY_SECURITY_ANSWER_HASH, answerHash)
            .commit()
        if (success) {
            syncMetaToDisk()
        }
        return success
    }

    fun getSecurityQuestion(): String? {
        return prefs.getString(KEY_SECURITY_QUESTION, "What was your childhood nickname?")
    }

    fun hasSecurityQuestion(): Boolean {
        return prefs.contains(KEY_SECURITY_ANSWER_HASH)
    }

    fun verifySecurityAnswer(answer: String): Boolean {
        val storedHash = prefs.getString(KEY_SECURITY_ANSWER_HASH, null) ?: return false
        return hashPin(answer.trim().lowercase()) == storedHash
    }

    fun resetPinWithAnswer(answer: String, newPin: String): Boolean {
        if (verifySecurityAnswer(answer)) {
            return setupPin(newPin)
        }
        return false
    }

    fun getOrGenerateRecoverySeed(): List<String> {
        return MnemonicSeedManager.getOrGenerateSeed(context, prefs)
    }

    fun hasRecoverySeed(): Boolean {
        return MnemonicSeedManager.hasSeed(prefs)
    }

    fun verifyRecoverySeed(words: List<String>): Boolean {
        return MnemonicSeedManager.verifySeed(prefs, words)
    }

    fun resetPinWithRecoverySeed(words: List<String>, newPin: String): Boolean {
        if (verifyRecoverySeed(words)) {
            return setupPin(newPin)
        }
        return false
    }

    fun regenerateRecoverySeed(): List<String> {
        return MnemonicSeedManager.regenerateSeed(context, prefs)
    }

    fun isStealthModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_STEALTH_MODE, true)
    }

    fun setStealthModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STEALTH_MODE, enabled).apply()
    }

    fun isFlipLockEnabled(): Boolean {
        return prefs.getBoolean(KEY_FLIP_LOCK, false)
    }

    fun setFlipLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FLIP_LOCK, enabled).apply()
    }

    fun isShakeLockEnabled(): Boolean {
        return prefs.getBoolean(KEY_SHAKE_LOCK, false)
    }

    fun setShakeLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHAKE_LOCK, enabled).apply()
    }

    fun isScreenGuardEnabled(): Boolean {
        return _screenGuardFlow.value
    }

    fun setScreenGuardEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_SCREEN_GUARD, enabled)
            .putBoolean(KEY_ALLOW_SCREENSHOTS, !enabled)
            .apply()
        _screenGuardFlow.value = enabled
        _allowScreenshotsFlow.value = !enabled
    }

    fun isAllowScreenshotsEnabled(): Boolean {
        return _allowScreenshotsFlow.value
    }

    fun setAllowScreenshotsEnabled(enabled: Boolean) {
        setScreenGuardEnabled(!enabled)
    }

    fun getSelectedThemeId(): String {
        return _themeIdFlow.value
    }

    fun setSelectedThemeId(themeId: String) {
        prefs.edit().putString(KEY_THEME_ID, themeId).apply()
        _themeIdFlow.value = themeId
    }

    fun getCustomWallpaperPath(): String? {
        return _customWallpaperPathFlow.value
    }

    fun setCustomWallpaperPath(path: String?) {
        prefs.edit().putString(KEY_CUSTOM_WALLPAPER_PATH, path).apply()
        _customWallpaperPathFlow.value = path
    }

    fun isPanicPinConfigured(): Boolean {
        val hash = prefs.getString(KEY_PANIC_PIN_HASH, null)
        return !hash.isNullOrEmpty()
    }

    fun setupPanicPin(pin: String): Boolean {
        if (pin.length < 4) return false
        val hash = hashPin(pin)
        return prefs.edit().putString(KEY_PANIC_PIN_HASH, hash).commit()
    }

    fun removePanicPin(): Boolean {
        return prefs.edit().remove(KEY_PANIC_PIN_HASH).commit()
    }

    fun verifyPanicPin(enteredPin: String): Boolean {
        val storedHash = prefs.getString(KEY_PANIC_PIN_HASH, null) ?: return false
        return hashPin(enteredPin) == storedHash
    }

    suspend fun executeSelfDestructWipe(onFinished: () -> Unit = {}) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Delete all encrypted physical files
                fileManager.deleteAllVaultFiles()
                // 2. Clear Room Database
                val db = com.example.calcvault.data.db.VaultDatabase.getDatabase(context)
                db.mediaDao().clearAllMedia()
                db.noteDao().clearAllNotes()
                // 3. Clear intruder alerts
                val intruderDir = File(context.filesDir, "intruders")
                if (intruderDir.exists()) {
                    intruderDir.deleteRecursively()
                }
                // 4. Remove custom wallpaper
                setCustomWallpaperPath(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        withContext(Dispatchers.Main) {
            onFinished()
        }
    }

    fun isFeatureGuideShown(featureKey: String): Boolean {
        return prefs.getBoolean("guide_shown_$featureKey", false)
    }

    fun setFeatureGuideShown(featureKey: String, shown: Boolean = true) {
        prefs.edit().putBoolean("guide_shown_$featureKey", shown).commit()
    }

    fun isFirstTimeVaultTourShown(): Boolean {
        return prefs.getBoolean("vault_first_time_tour_shown", false)
    }

    fun markFirstTimeVaultTourShown() {
        prefs.edit().putBoolean("vault_first_time_tour_shown", true).commit()
    }

    fun setFirstTimeVaultTourShown(shown: Boolean = true) {
        prefs.edit().putBoolean("vault_first_time_tour_shown", shown).commit()
    }

    fun resetAllGuides() {
        val editor = prefs.edit()
        listOf("photos", "videos", "audios", "documents", "notes", "browser", "applock", "camera", "intruder", "trash", "vault_tour").forEach {
            editor.remove("guide_shown_$it")
        }
        editor.remove("vault_first_time_tour_shown")
        editor.apply()
    }

    private fun hashPin(raw: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        // Salt with package name and fixed secret
        val salted = "CalcVault_Salt_2026_${context.packageName}_$raw"
        val bytes = digest.digest(salted.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_PIN_HASH = "sec_pin_hash"
        private const val KEY_PIN_LENGTH = "sec_pin_length"
        private const val KEY_PANIC_PIN_HASH = "sec_panic_pin_hash"
        private const val KEY_SECURITY_QUESTION = "sec_security_question"
        private const val KEY_SECURITY_ANSWER_HASH = "sec_security_answer_hash"
        private const val KEY_STEALTH_MODE = "sec_stealth_mode"
        private const val KEY_FLIP_LOCK = "sec_flip_lock"
        private const val KEY_SHAKE_LOCK = "sec_shake_lock"
        private const val KEY_SCREEN_GUARD = "sec_screen_guard"
        private const val KEY_ALLOW_SCREENSHOTS = "sec_allow_screenshots"
        private const val KEY_THEME_ID = "sec_vault_theme_id"
        private const val KEY_CUSTOM_WALLPAPER_PATH = "sec_vault_wallpaper_path"
        private const val KEY_APP_LANGUAGE = "sec_app_language"
        const val RECOVERY_CODE = "112233"
    }
}
