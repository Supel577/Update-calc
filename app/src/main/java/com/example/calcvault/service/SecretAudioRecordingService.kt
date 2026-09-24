package com.example.calcvault.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.calcvault.data.audio.SecretAudioRecorderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SecretAudioRecordingService : Service() {

    companion object {
        const val ACTION_START = "com.example.calcvault.action.START_SECRET_RECORDING"
        const val ACTION_STOP = "com.example.calcvault.action.STOP_SECRET_RECORDING"
        const val EXTRA_TITLE = "extra_audio_title"
        private const val NOTIFICATION_ID = 2002
        private const val CHANNEL_ID = "vault_stealth_audio_svc"
    }

    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var isRecordingActive = false
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var tickerJob: Job? = null
    private var elapsedSeconds = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                val title = intent.getStringExtra(EXTRA_TITLE)
                stopRecording(title)
                stopSelf()
            }
            ACTION_START -> {
                startRecording()
            }
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        if (isRecordingActive || recorder != null) return

        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        try {
            // Acquire partial wake lock so CPU keeps running when user turns screen completely off
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CalcVault:SecretAudioRecordingWakeLock").apply {
                acquire(2 * 60 * 60 * 1000L) // up to 2 hours
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val tempFile = File(cacheDir, "rec_vault_${timeStamp}.m4a")
            currentOutputFile = tempFile

            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(tempFile.absolutePath)
                prepare()
                start()
            }
            recorder = newRecorder
            isRecordingActive = true
            elapsedSeconds = 0

            startForegroundNotification()

            SecretAudioRecorderManager.updateRecordingState(recording = true, seconds = 0, currentAmplitude = 0)

            tickerJob?.cancel()
            tickerJob = serviceScope.launch {
                while (isActive) {
                    delay(1000L)
                    elapsedSeconds++
                    val amp = try {
                        recorder?.maxAmplitude ?: 0
                    } catch (_: Exception) { 0 }
                    SecretAudioRecorderManager.updateRecordingState(
                        recording = true,
                        seconds = elapsedSeconds,
                        currentAmplitude = amp
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isRecordingActive = false
            releaseWakeLock()
            stopSelf()
        }
    }

    private fun stopRecording(customTitle: String? = null) {
        if (!isRecordingActive && recorder == null && currentOutputFile == null) {
            return
        }
        isRecordingActive = false
        tickerJob?.cancel()
        tickerJob = null

        try {
            recorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {}
                release()
            }
        } catch (_: Exception) {}
        recorder = null

        releaseWakeLock()

        val savedFile = currentOutputFile
        currentOutputFile = null // Prevents saving the same file twice if onDestroy() fires
        if (savedFile != null && savedFile.exists() && savedFile.length() > 0) {
            SecretAudioRecorderManager.onRecordingSaved(savedFile, customTitle)
        } else {
            SecretAudioRecorderManager.updateRecordingState(recording = false, seconds = 0, currentAmplitude = 0)
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        wakeLock = null
    }

    private fun startForegroundNotification() {
        val stopIntent = Intent(this, SecretAudioRecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            201,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            this,
            202,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Disguised as a system sound profile service (Microphone only, camera is never accessed)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Vault Sound Recording Service")
            .setContentText("Microphone audio recording active (Camera is 100% OFF)")
            .setContentIntent(appPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "System Sound Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Internal audio background processing"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }
}
