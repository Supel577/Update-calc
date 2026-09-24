package com.example.calcvault.data.security

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

class VaultSensorLockManager(
    private val context: Context,
    private val securityManager: VaultSecurityManager
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var onLockCallback: ((reason: String) -> Unit)? = null
    private var isListening = false

    // Flip down detection (responsive hold for at least 300ms)
    private var faceDownStartTime: Long = 0L
    private val FLIP_DOWN_HOLD_MS = 300L

    // Shake detection (natural wrist shake)
    private var lastShakeTimestamp: Long = 0L
    private var shakeCount = 0
    private val SHAKE_FORCE_THRESHOLD = 2.3f
    private val SHAKE_WINDOW_MS = 800L
    private var lastTriggerTime: Long = 0L

    fun startListening(onLock: (reason: String) -> Unit) {
        if (isListening || accelerometer == null || sensorManager == null) return
        this.onLockCallback = onLock
        faceDownStartTime = 0L
        shakeCount = 0
        lastShakeTimestamp = 0L
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        isListening = true
        Log.d("VaultSensorLock", "Sensor lock listener registered")
    }

    fun stopListening() {
        if (!isListening || sensorManager == null) return
        sensorManager.unregisterListener(this)
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
        if (now - lastTriggerTime < 1500L) {
            // Cool-down after triggering lock
            return
        }

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // 1. Check Flip-Down (phone screen facing the table)
        if (securityManager.isFlipLockEnabled()) {
            // When face-down: gravity pulls along negative Z (z ~ -9.8 m/s²), and phone is reasonably flat
            val isFaceDown = z < -6.0f && abs(x) < 6.8f && abs(y) < 6.8f
            if (isFaceDown) {
                if (faceDownStartTime == 0L) {
                    faceDownStartTime = now
                } else if (now - faceDownStartTime >= FLIP_DOWN_HOLD_MS) {
                    lastTriggerTime = now
                    faceDownStartTime = 0L
                    Log.d("VaultSensorLock", "Flip-down detected! Triggering panic lock.")
                    onLockCallback?.invoke("Flip-down detected")
                    return
                }
            } else {
                faceDownStartTime = 0L
            }
        }

        // 2. Check Shake-to-Lock (vigorous shaking in any axis)
        if (securityManager.isShakeLockEnabled()) {
            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH
            val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

            if (gForce > SHAKE_FORCE_THRESHOLD) {
                if (now - lastShakeTimestamp < SHAKE_WINDOW_MS) {
                    shakeCount++
                    if (shakeCount >= 2) {
                        lastTriggerTime = now
                        shakeCount = 0
                        lastShakeTimestamp = 0L
                        Log.d("VaultSensorLock", "Vigorous shake detected! Triggering panic lock.")
                        onLockCallback?.invoke("Panic shake detected")
                        return
                    }
                } else {
                    shakeCount = 1
                }
                lastShakeTimestamp = now
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
