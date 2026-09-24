package com.example.calcvault.ui.vault.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Language
import com.example.calcvault.data.i18n.VaultStrings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.calcvault.data.backup.GoogleDriveBackupService
import com.example.calcvault.data.backup.VaultBackupManager
import com.example.calcvault.data.camouflage.AppCamouflageManager
import com.example.calcvault.data.security.VaultSecurityManager
import com.example.calcvault.ui.vault.VaultViewModel
import com.example.calcvault.ui.vault.backup.CloudBackupDialog
import com.example.calcvault.ui.vault.cleaner.VaultStorageOptimizerDialog
import com.example.calcvault.ui.vault.security.RecoverySeedDialog

@Composable
fun SecuritySettingsDialog(
    securityManager: VaultSecurityManager,
    viewModel: VaultViewModel,
    camouflageManager: AppCamouflageManager,
    backupManager: VaultBackupManager,
    driveService: GoogleDriveBackupService,
    onDismiss: () -> Unit
) {
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var pinMessage by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // text to isSuccess

    var questionInput by remember { mutableStateOf(securityManager.getSecurityQuestion() ?: "What was your childhood nickname?") }
    var answerInput by remember { mutableStateOf("") }
    var questionMessage by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    var showBackupDialog by remember { mutableStateOf(false) }
    var showSeedDialog by remember { mutableStateOf(false) }
    var showOptimizerDialog by remember { mutableStateOf(false) }

    var isFlipLockEnabled by remember { mutableStateOf(securityManager.isFlipLockEnabled()) }
    var isShakeLockEnabled by remember { mutableStateOf(securityManager.isShakeLockEnabled()) }
    val isScreenshotsAllowed by securityManager.allowScreenshotsFlow.collectAsStateWithLifecycle()
    val appLanguage by securityManager.appLanguageFlow.collectAsStateWithLifecycle()

    var isPanicPinConfigured by remember { mutableStateOf(securityManager.isPanicPinConfigured()) }
    var panicPinInput by remember { mutableStateOf("") }
    var confirmPanicPinInput by remember { mutableStateOf("") }
    var panicPinMessage by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var showPanicSetupSection by remember { mutableStateOf(false) }
    var guideResetMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val activity = remember(context) {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return@remember ctx
            ctx = ctx.baseContext
        }
        null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            securePolicy = if (!isScreenshotsAllowed) SecureFlagPolicy.SecureOn else SecureFlagPolicy.SecureOff
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("settings_dialog"),
            color = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = VaultStrings.get(appLanguage, "settings"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App Language Selector Card (English / বাংলা)
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_app_language"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8)
                            )
                            Text(
                                text = VaultStrings.get(appLanguage, "lang_section_title"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = VaultStrings.get(appLanguage, "lang_section_desc"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // English Option
                            val isEn = appLanguage != "bn"
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { securityManager.setAppLanguage("en") }
                                    .testTag("btn_lang_en"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isEn) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFF0F172A),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isEn) Color(0xFF38BDF8) else Color(0xFF334155)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isEn) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = VaultStrings.get(appLanguage, "lang_english"),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isEn) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isEn) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                                    )
                                }
                            }

                            // Bengali Option
                            val isBn = appLanguage == "bn"
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { securityManager.setAppLanguage("bn") }
                                    .testTag("btn_lang_bn"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isBn) Color(0xFF059669).copy(alpha = 0.25f) else Color(0xFF0F172A),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isBn) Color(0xFF34D399) else Color(0xFF334155)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isBn) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = VaultStrings.get(appLanguage, "lang_bengali"),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isBn) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isBn) Color(0xFF34D399) else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Panic & Stealth Sensors Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFFF43F5E)
                            )
                            Text(
                                text = VaultStrings.get(appLanguage, "sec_stealth_controls"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = VaultStrings.get(appLanguage, "sec_stealth_controls_desc"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Flip-Down to Lock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ScreenLockPortrait,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = VaultStrings.get(appLanguage, "sec_flip_lock"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = VaultStrings.get(appLanguage, "sec_flip_lock_desc"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                            Switch(
                                checked = isFlipLockEnabled,
                                onCheckedChange = { checked ->
                                    isFlipLockEnabled = checked
                                    securityManager.setFlipLockEnabled(checked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF0284C7)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Shake to Lock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = VaultStrings.get(appLanguage, "sec_shake_lock"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = VaultStrings.get(appLanguage, "sec_shake_lock_desc"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                            Switch(
                                checked = isShakeLockEnabled,
                                onCheckedChange = { checked ->
                                    isShakeLockEnabled = checked
                                    securityManager.setShakeLockEnabled(checked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFD97706)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Screenshot Permission Toggle (Allow / Block Screenshots)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (isScreenshotsAllowed) Color(0xFF10B981) else Color(0xFFEF4444),
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = VaultStrings.get(appLanguage, "sec_screenshots"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = VaultStrings.get(
                                            appLanguage,
                                            if (isScreenshotsAllowed) "sec_screenshots_desc_on" else "sec_screenshots_desc_off"
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isScreenshotsAllowed) Color(0xFF34D399) else Color(0xFF94A3B8)
                                    )
                                }
                            }
                            Switch(
                                checked = isScreenshotsAllowed,
                                onCheckedChange = { checked ->
                                    securityManager.setAllowScreenshotsEnabled(checked)
                                    activity?.window?.let { win ->
                                        if (!checked) {
                                            win.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                                        } else {
                                            win.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF059669)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Emergency Self-Destruct Panic PIN Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFEF4444)
                            )
                            Column {
                                Text(
                                    text = VaultStrings.get(appLanguage, "sec_panic_title"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = VaultStrings.get(appLanguage, "sec_panic_desc"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFCA5A5)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = VaultStrings.get(appLanguage, "sec_panic_desc_detail"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Status Badge
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isPanicPinConfigured) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFF59E0B).copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = VaultStrings.get(appLanguage, "sec_panic_status_title"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = VaultStrings.get(
                                        appLanguage,
                                        if (isPanicPinConfigured) "sec_panic_configured" else "sec_panic_not_configured"
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPanicPinConfigured) Color(0xFF10B981) else Color(0xFFF59E0B)
                                )
                            }

                            if (isPanicPinConfigured) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { showPanicSetupSection = !showPanicSetupSection },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (showPanicSetupSection) VaultStrings.get(appLanguage, "sec_panic_close_btn") else VaultStrings.get(appLanguage, "sec_panic_change_btn"),
                                            color = Color(0xFF38BDF8),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            securityManager.removePanicPin()
                                            isPanicPinConfigured = false
                                            panicPinMessage = VaultStrings.get(appLanguage, "sec_panic_removed_success") to true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = VaultStrings.get(appLanguage, "sec_panic_remove_btn"),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Setup or Update Fields (Visible when not configured OR user clicked Change)
                        if (showPanicSetupSection || !isPanicPinConfigured) {
                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = panicPinInput,
                                onValueChange = { if (it.length <= 8 && it.all { ch -> ch.isDigit() }) panicPinInput = it },
                                label = { Text(VaultStrings.get(appLanguage, "sec_panic_input_label")) },
                                placeholder = { Text(VaultStrings.get(appLanguage, "sec_panic_placeholder")) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = confirmPanicPinInput,
                                onValueChange = { if (it.length <= 8 && it.all { ch -> ch.isDigit() }) confirmPanicPinInput = it },
                                label = { Text(VaultStrings.get(appLanguage, "sec_panic_confirm_label")) },
                                placeholder = { Text(VaultStrings.get(appLanguage, "sec_panic_placeholder")) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Properly placed Save / Update Button right under both fields
                            Button(
                                onClick = {
                                    if (panicPinInput.length < 4) {
                                        panicPinMessage = VaultStrings.get(appLanguage, "sec_panic_len_err") to false
                                    } else if (panicPinInput != confirmPanicPinInput) {
                                        panicPinMessage = VaultStrings.get(appLanguage, "sec_panic_mismatch_err") to false
                                    } else if (securityManager.verifyPin(panicPinInput)) {
                                        panicPinMessage = VaultStrings.get(appLanguage, "sec_panic_same_as_vault_err") to false
                                    } else {
                                        val success = securityManager.setupPanicPin(panicPinInput)
                                        if (success) {
                                            isPanicPinConfigured = true
                                            showPanicSetupSection = false
                                            panicPinInput = ""
                                            confirmPanicPinInput = ""
                                            panicPinMessage = VaultStrings.get(appLanguage, "sec_panic_saved_success") to true
                                        } else {
                                            panicPinMessage = VaultStrings.get(appLanguage, "sec_panic_len_err") to false
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_save_panic_pin"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isPanicPinConfigured)
                                        VaultStrings.get(appLanguage, "sec_panic_update_btn")
                                    else
                                        VaultStrings.get(appLanguage, "sec_panic_save_btn"),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        panicPinMessage?.let { (msg, isSuccess) ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = msg,
                                color = if (isSuccess) Color(0xFF10B981) else Color(0xFFEF4444),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 12-Word Hardware Recovery Seed Card (Offline Mnemonic Paper Backup)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24)
                            )
                            Text(
                                text = if (appLanguage == "bn") "১২-শব্দের অফলাইন পেপার সিড কি" else "12-Word Hardware Recovery Seed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (appLanguage == "bn")
                                "ক্রিপ্টো-স্টাইল ১২টি রিকভারি শব্দ। পিন ভুলে গেলে ক্যালকুলেটরে 112233= চেপে এই সিড কোড দিয়ে ভল্ট রিকভার করা যায়। ইন্টারনেট ছাড়াই সম্পূর্ণ অফলাইন।"
                            else
                                "Crypto-standard 12-word seed phrase for disaster recovery. If you ever forget your calculator PIN, enter 112233= and use these words to restore full vault access.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { showSeedDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("btn_view_recovery_seed")
                        ) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (appLanguage == "bn") "১২-শব্দের সিড কি দেখুন" else "View 12-Word Seed Phrase",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Vault Storage Cleaner & Optimizer Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8)
                            )
                            Text(
                                text = if (appLanguage == "bn") "ডুপ্লিকেট ও স্টোরেজ অপ্টিমাইজার" else "Storage & Duplicate Cleaner",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (appLanguage == "bn")
                                "ভল্টের ডুপ্লিকেট ছবি, বড় ভিডিও ও রিসাইকেল বিন স্ক্যান করে দ্রুত ফোনের মেমোরি খালি করুন।"
                            else
                                "Scan duplicate photos, analyze heavy videos over 5MB, and empty trash to reclaim storage space instantly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { showOptimizerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("btn_open_storage_cleaner")
                        ) {
                            Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (appLanguage == "bn") "স্টোরেজ ক্লিনার খুলুন" else "Open Storage Cleaner",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Encrypted Cloud & Drive Backup Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = Color(0xFF10B981)
                            )
                            Text(
                                text = "Encrypted Backup & Restore",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "One-tap AES-256 backup. Save your entire vault directly to phone memory, SD card, or share to Google Drive without any cloud account setup. Fully handles large video collections without memory issues.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Security Type",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "AES-256-GCM Encrypted",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF10B981)
                                )
                            }

                            Button(
                                onClick = { showBackupDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Backup & Restore")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Change PIN Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8)
                            )
                            Text(
                                text = "Change Vault PIN",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = currentPinInput,
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) currentPinInput = it },
                            label = { Text("Current 4-Digit PIN") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newPinInput,
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPinInput = it },
                            label = { Text("New 4-Digit PIN") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = confirmPinInput,
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPinInput = it },
                            label = { Text("Confirm New PIN") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (pinMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = pinMessage!!.first,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (pinMessage!!.second) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (!securityManager.verifyPin(currentPinInput)) {
                                    pinMessage = Pair("Current PIN is incorrect.", false)
                                    return@Button
                                }
                                if (newPinInput.length != 4) {
                                    pinMessage = Pair("New PIN must be 4 digits.", false)
                                    return@Button
                                }
                                if (newPinInput != confirmPinInput) {
                                    pinMessage = Pair("New PINs do not match.", false)
                                    return@Button
                                }
                                securityManager.setupPin(newPinInput)
                                pinMessage = Pair("PIN updated successfully!", true)
                                currentPinInput = ""
                                newPinInput = ""
                                confirmPinInput = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Update PIN")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Security Question Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QuestionAnswer,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B)
                            )
                            Text(
                                text = "PIN Recovery Question",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = questionInput,
                            onValueChange = { questionInput = it },
                            label = { Text("Security Question") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = answerInput,
                            onValueChange = { answerInput = it },
                            label = { Text("Answer") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (questionMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = questionMessage!!.first,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (questionMessage!!.second) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (questionInput.isBlank() || answerInput.isBlank()) {
                                    questionMessage = Pair("Please provide both question and answer.", false)
                                    return@Button
                                }
                                securityManager.setSecurityQuestion(questionInput, answerInput)
                                questionMessage = Pair("Recovery question saved!", true)
                                answerInput = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Save Question")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reset Feature Intro Overlays Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8)
                            )
                            Text(
                                text = VaultStrings.get(appLanguage, "sec_reset_guides"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = VaultStrings.get(appLanguage, "sec_reset_guides_desc"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            guideResetMessage?.let { msg ->
                                Text(
                                    text = msg,
                                    color = Color(0xFF10B981),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                            } ?: Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = {
                                    securityManager.resetAllGuides()
                                    guideResetMessage = VaultStrings.get(appLanguage, "sec_reset_guides_success")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(VaultStrings.get(appLanguage, "sec_reset_guides_btn"))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Privacy & Stealth Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color(0xFF10B981)
                            )
                            Text(
                                text = "Stealth & Privacy Guard",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "• Auto-Lock: The vault automatically locks the moment this app is minimized or placed into background.\n" +
                                   "• Concealed Files: Hidden media is stored in the app's isolated private sandbox with .nomedia flags.\n" +
                                   "• Recovery Shortcut: Typing 112233= on the calculator triggers recovery question if you ever forget your PIN.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            lineHeight = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // In-App OTA Update Checker Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8)
                            )
                            Text(
                                text = "Software Updates",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "CalcVault v2.5.0 (Build 2026.04) • Security Patch: Latest\nDirect zero-telemetry OTA channel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.checkUpdate() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Check for Updates")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Zero-Telemetry Privacy Commitment & About
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Zero-Telemetry Privacy Commitment",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "HideU CalcVault operates entirely on-device with zero advertising trackers, zero analytic telemetry SDKs, and zero third-party telemetry. All encryption keys and biometric credentials stay in hardware keystore. Your privacy is absolute and mathematically guaranteed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.08f))
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lead Developer & Architect",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "TARIKUL ISLAM SUPEL",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showBackupDialog) {
        CloudBackupDialog(
            backupManager = backupManager,
            driveService = driveService,
            securityManager = securityManager,
            onRestoreComplete = { viewModel.onRestoreCompleted() },
            onDismiss = { showBackupDialog = false }
        )
    }

    if (showSeedDialog) {
        RecoverySeedDialog(
            securityManager = securityManager,
            appLanguage = appLanguage,
            onDismiss = { showSeedDialog = false }
        )
    }

    if (showOptimizerDialog) {
        VaultStorageOptimizerDialog(
            viewModel = viewModel,
            appLanguage = appLanguage,
            onDismiss = { showOptimizerDialog = false }
        )
    }
}
