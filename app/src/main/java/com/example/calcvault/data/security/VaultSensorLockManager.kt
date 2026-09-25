package com.example.calcvault.data.security

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

class VaultSensorLockManager(
    private val context: Context,
    private val securityManager: VaultSecurityManager
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var onLockCallback: ((reason: String) -> Unit)? = null
    private var isListening = false

    // Flip down detection (phone face-down)
    private var faceDownStartTime: Long = 0L
    private val FLIP_DOWN_HOLD_MS = 250L

    // Shake detection (natural wrist shake)
    private var lastShakeTimestamp: Long = 0L
    private var shakeCount = 0
    private val SHAKE_FORCE_THRESHOLD = 1.6f
    private val SHAKE_WINDOW_MS = 1200L
    private var lastTriggerTime: Long = 0L
    private var unlockGracePeriodUntil: Long = 0L

    fun startListening(onLock: (reason: String) -> Unit) {
        if (accelerometer == null || sensorManager == null) {
            Log.w("VaultSensorLock", "Accelerometer sensor not available on this device")
            return
        }
        if (isListening) return

        this.onLockCallback = onLock
        faceDownStartTime = 0L
        shakeCount = 0
        lastShakeTimestamp = 0L
        unlockGracePeriodUntil = System.currentTimeMillis() + 600L // Brief 600ms grace period upon unlock
        try {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            isListening = true
            Log.d("VaultSensorLock", "Sensor lock listener registered successfully")
        } catch (e: Exception) {
            Log.w("VaultSensorLock", "Failed to register sensor: ${e.message}")
        }
    }

    fun stopListening() {
        if (!isListening || sensorManager == null) return
        try {
            sensorManager.unregisterListener(this)
        } catch (_: Exception) {}
        isListening = false
        onLockCallback = null
        faceDownStartTime = 0L
        shakeCount = 0
        Log.d("VaultSensorLock", "Sensor lock listener unregistered")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        if (securityManager.isExternalActivityActive) return

        val now = System.currentTimeMillis()
        if (now < unlockGracePeriodUntil) {
            // Still in brief unlock grace period, ignore triggers
            return
        }
        if (now - lastTriggerTime < 2000L) {
            // Cool-down after triggering lock
            return
        }

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // 1. Check Flip-Down (phone screen facing down towards table or surface)
        if (securityManager.isFlipLockEnabled()) {
            // When face-down: gravity pulls along negative Z (z < -5.0f)
            val isFaceDown = z < -5.2f && abs(x) < 8.2f && abs(y) < 8.2f
            if (isFaceDown) {
                if (faceDownStartTime == 0L) {
                    faceDownStartTime = now
                } else if (now - faceDownStartTime >= FLIP_DOWN_HOLD_MS) {
                    lastTriggerTime = now
                    faceDownStartTime = 0L
                    Log.d("VaultSensorLock", "Flip-down detected! Triggering panic lock.")
                    mainHandler.post {
                        onLockCallback?.invoke("Flip-down detected")
                    }
                    return
                }
            } else {
                faceDownStartTime = 0L
            }
        }

        // 2. Check Shake-to-Lock (vigorous or brisk wrist shaking in any axis)
        if (securityManager.isShakeLockEnabled()) {
            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH
            val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

            if (gForce > SHAKE_FORCE_THRESHOLD) {
                // Brisk single shake (> 2.1g) or 2 rapid shakes within 1.2s window
                if (gForce > 2.05f || (now - lastShakeTimestamp < SHAKE_WINDOW_MS && shakeCount >= 1)) {
                    lastTriggerTime = now
                    shakeCount = 0
                    lastShakeTimestamp = 0L
                    Log.d("VaultSensorLock", "Shake detected! Triggering panic lock.")
                    mainHandler.post {
                        onLockCallback?.invoke("Panic shake detected")
                    }
                    return
                } else {
                    shakeCount = 1
                    lastShakeTimestamp = now
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
