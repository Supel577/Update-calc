package com.example.calcvault.data.video

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.example.calcvault.service.SecretVideoRecordingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object SecretVideoRecorderManager {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds.asStateFlow()

    private val _lastSavedFile = MutableStateFlow<File?>(null)
    val lastSavedFile: StateFlow<File?> = _lastSavedFile.asStateFlow()

    var onVideoRecordedCallback: ((File) -> Unit)? = null

    private val processedFiles = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    fun updateRecordingState(recording: Boolean, seconds: Int = 0) {
        _isRecording.value = recording
        _recordingSeconds.value = seconds
    }

    fun onRecordingSaved(file: File) {
        val path = file.absolutePath
        if (processedFiles.contains(path)) return
        processedFiles.add(path)

        _lastSavedFile.value = file
        _isRecording.value = false
        _recordingSeconds.value = 0
        onVideoRecordedCallback?.invoke(file)
    }

    fun startRecording(context: Context, facing: String = "back", showFeedback: Boolean = true) {
        if (_isRecording.value) return

        val secManager = com.example.calcvault.data.security.VaultSecurityManager(context)
        val lang = secManager.appLanguage

        // Check Camera & Audio Permissions
        val hasCam = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasMic = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasCam || !hasMic) {
            if (showFeedback) {
                val msg = if (lang == "bn")
                    "⚠️ ক্যামেরা এবং মাইক্রোফোন পারমিশন প্রয়োজন!"
                else
                    "⚠️ Camera & Microphone permissions required for secret video recording!"
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                }
            }
            return
        }

        try {
            val intent = Intent(context, SecretVideoRecordingService::class.java).apply {
                action = SecretVideoRecordingService.ACTION_START
                putExtra(SecretVideoRecordingService.EXTRA_FACING, facing)
            }
            ContextCompat.startForegroundService(context, intent)
            vibrateSecretly(context, isStarting = true)

            if (showFeedback) {
                val msg = if (lang == "bn")
                    "🔴 গোপন ভিডিও রেকর্ড চালু হয়েছে (স্ক্রিন বন্ধেও রেকর্ড হবে)"
                else
                    "🔴 Secret Video Recording Started (Screen-off supported)"
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopRecording(context: Context, showFeedback: Boolean = true) {
        if (!_isRecording.value) return

        try {
            val intent = Intent(context, SecretVideoRecordingService::class.java).apply {
                action = SecretVideoRecordingService.ACTION_STOP
            }
            context.startService(intent)
            vibrateSecretly(context, isStarting = false)

            if (showFeedback) {
                val secManager = com.example.calcvault.data.security.VaultSecurityManager(context)
                val lang = secManager.appLanguage
                val msg = if (lang == "bn")
                    "✅ গোপন ভিডিও সেভ ও ভল্টে এনক্রিপ্ট করা হয়েছে"
                else
                    "✅ Secret Video Saved into Vault"
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleRecording(context: Context, facing: String = "back", showFeedback: Boolean = true) {
        if (_isRecording.value) {
            stopRecording(context, showFeedback = showFeedback)
        } else {
            startRecording(context, facing = facing, showFeedback = showFeedback)
        }
    }

    private fun vibrateSecretly(context: Context, isStarting: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isStarting) {
                    val timings = longArrayOf(0, 100, 100, 100)
                    val amplitudes = intArrayOf(0, 200, 0, 240)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    vibrator.vibrate(VibrationEffect.createOneShot(300, 250))
                }
            } else {
                @Suppress("DEPRECATION")
                if (isStarting) {
                    vibrator.vibrate(longArrayOf(0, 100, 100, 100), -1)
                } else {
                    vibrator.vibrate(300)
                }
            }
        } catch (_: Exception) {}
    }
}
