package com.example.calcvault.data.audio

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.example.calcvault.service.SecretAudioRecordingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object SecretAudioRecorderManager {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds.asStateFlow()

    private val _lastSavedFile = MutableStateFlow<File?>(null)
    val lastSavedFile: StateFlow<File?> = _lastSavedFile.asStateFlow()

    private val _amplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _amplitude.asStateFlow()

    // Callback when audio finishes so VaultViewModel or repository can import it
    var onAudioRecordedCallback: ((File, String?) -> Unit)? = null

    private val processedFiles = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    fun updateRecordingState(recording: Boolean, seconds: Int = 0, currentAmplitude: Int = 0) {
        _isRecording.value = recording
        _recordingSeconds.value = seconds
        _amplitude.value = currentAmplitude
    }

    fun onRecordingSaved(file: File, title: String? = null) {
        val path = file.absolutePath
        if (processedFiles.contains(path)) {
            return
        }
        processedFiles.add(path)
        _lastSavedFile.value = file
        _isRecording.value = false
        _recordingSeconds.value = 0
        onAudioRecordedCallback?.invoke(file, title)
    }

    fun startRecording(context: Context, showFeedback: Boolean = true) {
        if (_isRecording.value) return

        val secManager = com.example.calcvault.data.security.VaultSecurityManager(context)
        val lang = secManager.appLanguage

        // CRITICAL: Verify microphone permission before touching service to prevent app crash
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            if (showFeedback) {
                val msg = if (lang == "bn")
                    "⚠️ মাইক্রোফোন পারমিশন প্রয়োজন! অডিও সেকশনে গিয়ে পারমিশন চালু করুন।"
                else
                    "⚠️ Microphone permission required! Please grant permission in Secret Audio first."
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                }
            }
            return
        }

        try {
            val intent = Intent(context, SecretAudioRecordingService::class.java).apply {
                action = SecretAudioRecordingService.ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
            vibrateSecretly(context, isStarting = true)

            if (showFeedback) {
                val msg = if (lang == "bn") "🔴 গোপন অডিও রেকর্ড শুরু হয়েছে (স্ক্রিন বন্ধেও রেকর্ড চলবে)" else "🔴 Secret Audio Recording Started (Screen-off supported)"
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopRecording(context: Context, customTitle: String? = null, showFeedback: Boolean = true) {
        if (!_isRecording.value) return
        try {
            val intent = Intent(context, SecretAudioRecordingService::class.java).apply {
                action = SecretAudioRecordingService.ACTION_STOP
                if (!customTitle.isNullOrBlank()) {
                    putExtra(SecretAudioRecordingService.EXTRA_TITLE, customTitle)
                }
            }
            context.startService(intent)
            vibrateSecretly(context, isStarting = false)

            if (showFeedback) {
                val secManager = com.example.calcvault.data.security.VaultSecurityManager(context)
                val lang = secManager.appLanguage
                val msg = if (lang == "bn") "✅ গোপন রেকর্ড সম্পন্ন ও ভল্টে সংরক্ষিত হয়েছে" else "✅ Secret Audio Saved into Vault"
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleRecording(context: Context, showFeedback: Boolean = true) {
        if (_isRecording.value) {
            stopRecording(context, showFeedback = showFeedback)
        } else {
            startRecording(context, showFeedback = showFeedback)
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
                    // Two short distinct buzzes so user feels in pocket that recording started
                    val timings = longArrayOf(0, 70, 90, 70)
                    val amplitudes = intArrayOf(0, 180, 0, 220)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    // One longer confirmation buzz indicating recording stopped and saved
                    vibrator.vibrate(VibrationEffect.createOneShot(260, 240))
                }
            } else {
                @Suppress("DEPRECATION")
                if (isStarting) {
                    vibrator.vibrate(longArrayOf(0, 70, 90, 70), -1)
                } else {
                    vibrator.vibrate(260)
                }
            }
        } catch (_: Exception) {}
    }
}
