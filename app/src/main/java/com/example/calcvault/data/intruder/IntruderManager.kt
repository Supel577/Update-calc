package com.example.calcvault.data.intruder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class IntruderAlert(
    val id: String,
    val file: File,
    val timestamp: Long,
    val formattedDate: String
)

class IntruderManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("calc_vault_intruder_prefs", Context.MODE_PRIVATE)

    private val intrudersDir: File
        get() {
            val dir = File(context.filesDir, "intruders")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val nomedia = File(dir, ".nomedia")
            if (!nomedia.exists()) {
                nomedia.createNewFile()
            }
            return dir
        }

    fun isIntruderSelfieEnabled(): Boolean {
        return prefs.getBoolean(KEY_INTRUDER_ENABLED, true)
    }

    fun setIntruderSelfieEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_INTRUDER_ENABLED, enabled).apply()
    }

    fun recordFailedAttempt(): Int {
        val current = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, current).apply()
        return current
    }

    fun resetFailedAttempts() {
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).apply()
    }

    fun getFailedAttempts(): Int {
        return prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
    }

    fun getIntruderAlerts(): List<IntruderAlert> {
        val dir = intrudersDir
        val files = dir.listFiles { f -> f.isFile && f.extension.equals("jpg", ignoreCase = true) } ?: return emptyList()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())

        return files.mapNotNull { file ->
            val timestamp = file.name.removePrefix("intruder_").removeSuffix(".jpg").toLongOrNull() ?: file.lastModified()
            IntruderAlert(
                id = file.name,
                file = file,
                timestamp = timestamp,
                formattedDate = dateFormat.format(Date(timestamp))
            )
        }.sortedByDescending { it.timestamp }
    }

    fun deleteAlert(alert: IntruderAlert): Boolean {
        return try {
            if (alert.file.exists()) {
                alert.file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun clearAllAlerts(): Boolean {
        return try {
            val files = intrudersDir.listFiles { f -> f.isFile && f.extension.equals("jpg", ignoreCase = true) }
            files?.forEach { it.delete() }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Silently captures a front-camera snapshot without displaying any viewfinder preview.
     */
    fun captureSilentSelfie(
        lifecycleOwner: LifecycleOwner,
        onCaptured: (File?) -> Unit = {}
    ) {
        if (!isIntruderSelfieEnabled()) {
            onCaptured(null)
            return
        }

        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCameraPermission) {
            Log.w("IntruderManager", "Camera permission not granted for intruder capture")
            onCaptured(null)
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Check if front camera is available, fallback to back camera if not
                val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    onCaptured(null)
                    return@addListener
                }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageCapture)

                val timestamp = System.currentTimeMillis()
                val photoFile = File(intrudersDir, "intruder_$timestamp.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                // Small delay to allow sensor warm-up (300ms)
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        imageCapture.takePicture(
                            outputOptions,
                            mainExecutor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    try {
                                        cameraProvider.unbindAll()
                                    } catch (_: Exception) {}
                                    Log.d("IntruderManager", "Intruder selfie captured: ${photoFile.absolutePath}")
                                    onCaptured(photoFile)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    try {
                                        cameraProvider.unbindAll()
                                    } catch (_: Exception) {}
                                    Log.e("IntruderManager", "Intruder selfie failed: ${exception.message}", exception)
                                    onCaptured(null)
                                }
                            }
                        )
                    } catch (e: Exception) {
                        try {
                            cameraProvider.unbindAll()
                        } catch (_: Exception) {}
                        Log.e("IntruderManager", "Error executing takePicture: ${e.message}", e)
                        onCaptured(null)
                    }
                }, 300)

            } catch (e: Exception) {
                Log.e("IntruderManager", "Error binding camera for silent capture: ${e.message}", e)
                onCaptured(null)
            }
        }, mainExecutor)
    }

    companion object {
        private const val KEY_INTRUDER_ENABLED = "key_intruder_selfie_enabled"
        private const val KEY_FAILED_ATTEMPTS = "key_consecutive_failed_attempts"
    }
}
