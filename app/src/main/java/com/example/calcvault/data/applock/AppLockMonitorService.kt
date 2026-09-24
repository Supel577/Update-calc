package com.example.calcvault.data.applock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppLockMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var monitorJob: Job? = null
    private lateinit var appLockManager: AppLockManager

    private var activeUnlockedPackage: String? = null
    private var lastSeenActiveTime: Long = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                appLockManager.clearSessionUnlocks()
                activeUnlockedPackage = null
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        appLockManager = AppLockManager.getInstance(this)
        startForegroundNotification()

        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }

        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (monitorJob?.isActive != true) {
            startMonitoring()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundNotification() {
        val channelId = "app_lock_protection_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "CalcVault Protection",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Monitors and protects hidden and locked applications"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Calculator Security Shield")
            .setContentText("Protecting private and locked apps")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    1001,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(1001, notification)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private data class ForegroundEventStatus(
        val latestResumedPackage: String?,
        val latestResumedTime: Long,
        val latestPausedPackage: String?,
        val latestPausedTime: Long
    )

    private fun getForegroundEventStatus(): ForegroundEventStatus {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return ForegroundEventStatus(null, 0L, null, 0L)
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 3500

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        var latestResumed: String? = null
        var latestResumedTime = 0L
        var latestPaused: String? = null
        var latestPausedTime = 0L

        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (event.timeStamp >= latestResumedTime) {
                        latestResumedTime = event.timeStamp
                        latestResumed = event.packageName
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    if (event.timeStamp >= latestPausedTime) {
                        latestPausedTime = event.timeStamp
                        latestPaused = event.packageName
                    }
                }
            }
        }
        return ForegroundEventStatus(latestResumed, latestResumedTime, latestPaused, latestPausedTime)
    }

    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (isActive) {
                try {
                    val status = getForegroundEventStatus()
                    val currentPackage = status.latestResumedPackage

                    val now = System.currentTimeMillis()

                    // 1. Immediately re-lock if user navigated away from the active unlocked app
                    val active = activeUnlockedPackage
                    if (active != null) {
                        if (currentPackage == active) {
                            lastSeenActiveTime = now
                        } else {
                            val wasPaused = (status.latestPausedPackage == active && status.latestPausedTime > 0)
                            val switchedAway = (currentPackage != null && currentPackage != active && currentPackage != packageName)
                            val timedOutAway = (lastSeenActiveTime > 0L && (now - lastSeenActiveTime > 1200L))

                            if (switchedAway || wasPaused || timedOutAway) {
                                appLockManager.lockAgain(active)
                                activeUnlockedPackage = null
                                lastSeenActiveTime = 0L
                            }
                        }
                    }

                    // 2. If current foreground package is a protected app, enforce PIN requirement every time
                    if (currentPackage != null && currentPackage != packageName) {
                        if (appLockManager.isPackageProtected(currentPackage)) {
                            val isUnlocked = appLockManager.isUnlockedThisSession(currentPackage)
                            if (!isUnlocked) {
                                // Must enter PIN! If overlay is not already showing for this package, open it
                                if (!AppLockOverlayActivity.isShowing) {
                                    val intent = Intent(this@AppLockMonitorService, AppLockOverlayActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                        putExtra(AppLockOverlayActivity.EXTRA_PACKAGE_NAME, currentPackage)
                                    }
                                    startActivity(intent)
                                }
                            } else {
                                activeUnlockedPackage = currentPackage
                                lastSeenActiveTime = now
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(150)
            }
        }
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {}
        super.onDestroy()
    }
}
