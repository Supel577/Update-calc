package com.example.calcvault.data.applock

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class LockedAppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable?,
    val isLocked: Boolean,
    val isHidden: Boolean = false,
    val isSystemApp: Boolean
)

class AppLockManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("calcvault_app_lock_prefs", Context.MODE_PRIVATE)

    private val KEY_LOCKED_PACKAGES = "locked_package_names"
    private val KEY_HIDDEN_PACKAGES = "hidden_package_names"
    private val KEY_SERVICE_ENABLED = "app_lock_service_enabled"

    private val _lockedPackagesFlow = MutableStateFlow<Set<String>>(getLockedPackages())
    val lockedPackagesFlow: StateFlow<Set<String>> = _lockedPackagesFlow.asStateFlow()

    private val _hiddenPackagesFlow = MutableStateFlow<Set<String>>(getHiddenPackages())
    val hiddenPackagesFlow: StateFlow<Set<String>> = _hiddenPackagesFlow.asStateFlow()

    // Temporary session unlocks (unlocked until device sleep or app switch)
    private val sessionUnlockedPackages = mutableSetOf<String>()

    fun getLockedPackages(): Set<String> {
        return prefs.getStringSet(KEY_LOCKED_PACKAGES, emptySet())?.toSet() ?: emptySet()
    }

    fun getHiddenPackages(): Set<String> {
        return prefs.getStringSet(KEY_HIDDEN_PACKAGES, emptySet())?.toSet() ?: emptySet()
    }

    fun isServiceEnabled(): Boolean {
        return prefs.getBoolean(KEY_SERVICE_ENABLED, true)
    }

    fun setServiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
        if (enabled) {
            startLockMonitoringService()
        }
    }

    fun isPackageLocked(packageName: String): Boolean {
        return getLockedPackages().contains(packageName)
    }

    fun isPackageHidden(packageName: String): Boolean {
        return getHiddenPackages().contains(packageName)
    }

    fun isPackageProtected(packageName: String): Boolean {
        return isPackageLocked(packageName) || isPackageHidden(packageName)
    }

    fun setPackageLocked(packageName: String, locked: Boolean) {
        val current = getLockedPackages().toMutableSet()
        if (locked) {
            current.add(packageName)
        } else {
            current.remove(packageName)
            sessionUnlockedPackages.remove(packageName)
        }
        prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, current).apply()
        _lockedPackagesFlow.value = current

        if (locked && isServiceEnabled()) {
            startLockMonitoringService()
        }
    }

    fun setPackageHidden(packageName: String, hidden: Boolean) {
        val current = getHiddenPackages().toMutableSet()
        if (hidden) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        prefs.edit().putStringSet(KEY_HIDDEN_PACKAGES, current).apply()
        _hiddenPackagesFlow.value = current

        if (hidden && isServiceEnabled()) {
            startLockMonitoringService()
        }
    }

    fun isUnlockedThisSession(packageName: String): Boolean {
        return sessionUnlockedPackages.contains(packageName)
    }

    fun unlockForSession(packageName: String) {
        sessionUnlockedPackages.add(packageName)
    }

    fun lockAgain(packageName: String) {
        sessionUnlockedPackages.remove(packageName)
    }

    fun clearSessionUnlocks() {
        sessionUnlockedPackages.clear()
    }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageStatsSettings() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getInstalledApps(): List<LockedAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val locked = getLockedPackages()
        val hidden = getHiddenPackages()
        val apps = mutableListOf<LockedAppInfo>()

        try {
            val installed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
            } else {
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
            }

            for (app in installed) {
                // Do not list self
                if (app.packageName == context.packageName) continue

                // Only include launchable applications
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    val appName = pm.getApplicationLabel(app).toString()
                    val icon = try {
                        pm.getApplicationIcon(app)
                    } catch (e: Exception) {
                        null
                    }
                    val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                    apps.add(
                        LockedAppInfo(
                            appName = appName,
                            packageName = app.packageName,
                            icon = icon,
                            isLocked = locked.contains(app.packageName),
                            isHidden = hidden.contains(app.packageName),
                            isSystemApp = isSystem
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Sort: hidden first, then locked, then alphabetically
        apps.sortedWith(
            compareByDescending<LockedAppInfo> { it.isHidden }
                .thenByDescending { it.isLocked }
                .thenBy { it.appName.lowercase() }
        )
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // Mark unlocked for current session so it doesn't immediately lock when launched from inside vault
                unlockForSession(packageName)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun startLockMonitoringService() {
        if (!hasUsageStatsPermission() || !hasOverlayPermission()) {
            // Cannot monitor without required permissions
            return
        }
        try {
            val intent = Intent(context, AppLockMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    companion object {
        @Volatile
        private var instance: AppLockManager? = null

        fun getInstance(context: Context): AppLockManager {
            return instance ?: synchronized(this) {
                instance ?: AppLockManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
