package com.example.calcvault.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.calcvault.data.db.VaultDatabase
import com.example.calcvault.data.storage.VaultFileManager
import com.example.calcvault.data.video.SecretVideoRecorderManager
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

class SecretVideoRecordingService : Service() {

    companion object {
        const val ACTION_START = "com.example.calcvault.action.START_SECRET_VIDEO"
        const val ACTION_STOP = "com.example.calcvault.action.STOP_SECRET_VIDEO"
        const val EXTRA_FACING = "extra_camera_facing"
        private const val NOTIFICATION_ID = 2004
        private const val CHANNEL_ID = "vault_stealth_video_svc"
    }

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var dummySurfaceTexture: android.graphics.SurfaceTexture? = null
    private var dummySurface: android.view.Surface? = null

    private var isRecordingActive = false
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var tickerJob: Job? = null
    private var elapsedSeconds = 0

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startBackgroundThread()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopRecording()
                stopSelf()
            }
            ACTION_START -> {
                val facing = intent.getStringExtra(EXTRA_FACING) ?: "back"
                startRecording(facing)
            }
        }
        return START_NOT_STICKY
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("SecretVideoBgThread").apply {
            start()
            backgroundHandler = Handler(looper)
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
        } catch (_: Exception) {}
        backgroundThread = null
        backgroundHandler = null
    }

    @SuppressLint("MissingPermission")
    private fun startRecording(facing: String) {
        if (isRecordingActive || cameraDevice != null) return

        val hasCamera = androidx.core.content.ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasAudio = androidx.core.content.ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasCamera || !hasAudio) {
            stopSelf()
            return
        }

        try {
            // Partial wake lock so CPU keeps running when the physical screen turns off
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "CalcVault:SecretVideoWakeLock"
            ).apply {
                acquire(2 * 60 * 60 * 1000L) // up to 2 hours
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val tempFile = File(cacheDir, "rec_vid_${timeStamp}.mp4")
            currentOutputFile = tempFile

            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val targetFacing = if (facing == "front") {
                CameraCharacteristics.LENS_FACING_FRONT
            } else {
                CameraCharacteristics.LENS_FACING_BACK
            }

            var chosenCameraId: String? = null
            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val lensFacing = chars.get(CameraCharacteristics.LENS_FACING)
                if (lensFacing == targetFacing) {
                    chosenCameraId = id
                    break
                }
            }
            if (chosenCameraId == null && cameraManager.cameraIdList.isNotEmpty()) {
                chosenCameraId = cameraManager.cameraIdList[0]
            }

            if (chosenCameraId == null) {
                stopSelf()
                return
            }

            // Prepare MediaRecorder
            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder = newRecorder

            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(tempFile.absolutePath)
                setVideoEncodingBitRate(3_500_000)
                setVideoFrameRate(30)
                setVideoSize(1280, 720)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                prepare()
            }

            startForegroundNotification()

            cameraManager.openCamera(chosenCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCaptureSession(camera, newRecorder)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                    stopRecording()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    stopRecording()
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            e.printStackTrace()
            isRecordingActive = false
            releaseWakeLock()
            stopSelf()
        }
    }

    private fun createCaptureSession(camera: CameraDevice, recorder: MediaRecorder) {
        try {
            val surface = recorder.surface
            val texture = android.graphics.SurfaceTexture(10).apply {
                setDefaultBufferSize(640, 480)
            }
            dummySurfaceTexture = texture
            val dummy = android.view.Surface(texture)
            dummySurface = dummy

            val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(surface)
                addTarget(dummy)
                set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
            }

            val surfaces = listOf(surface, dummy)
            camera.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    try {
                        session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
                        recorder.start()
                        isRecordingActive = true
                        elapsedSeconds = 0
                        SecretVideoRecorderManager.updateRecordingState(true, 0)

                        tickerJob?.cancel()
                        tickerJob = serviceScope.launch {
                            while (isActive && isRecordingActive) {
                                delay(1000L)
                                elapsedSeconds++
                                SecretVideoRecorderManager.updateRecordingState(true, elapsedSeconds)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        stopRecording()
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    stopRecording()
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            e.printStackTrace()
            stopRecording()
        }
    }

    private fun stopRecording() {
        if (!isRecordingActive && mediaRecorder == null && currentOutputFile == null) {
            return
        }
        isRecordingActive = false
        tickerJob?.cancel()
        tickerJob = null

        try {
            captureSession?.stopRepeating()
            captureSession?.close()
        } catch (_: Exception) {}
        captureSession = null

        try {
            cameraDevice?.close()
        } catch (_: Exception) {}
        cameraDevice = null

        try {
            mediaRecorder?.apply {
                try { stop() } catch (_: Exception) {}
                release()
            }
        } catch (_: Exception) {}
        mediaRecorder = null

        try {
            dummySurface?.release()
        } catch (_: Exception) {}
        dummySurface = null

        try {
            dummySurfaceTexture?.release()
        } catch (_: Exception) {}
        dummySurfaceTexture = null

        releaseWakeLock()

        val savedFile = currentOutputFile
        currentOutputFile = null

        if (savedFile != null && savedFile.exists() && savedFile.length() > 0) {
            // Save & encrypt in database in background
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val db = VaultDatabase.getDatabase(applicationContext)
                    val fileManager = VaultFileManager(applicationContext)
                    val media = fileManager.saveSecretCameraVideo(savedFile)
                    if (media != null) {
                        db.mediaDao().insertMedia(media)
                    }
                    SecretVideoRecorderManager.onRecordingSaved(savedFile)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            SecretVideoRecorderManager.updateRecordingState(recording = false, seconds = 0)
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
        val stopIntent = Intent(this, SecretVideoRecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            205,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            this,
            206,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("System Display Service")
            .setContentText("Display color optimizer active")
            .setContentIntent(appPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var flags = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                startForeground(NOTIFICATION_ID, notification, flags)
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
                "System Video Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Internal display calibration"
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        stopBackgroundThread()
    }
}
