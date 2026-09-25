package com.example.calcvault.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log

/**
 * AdMobManager provides an ad-free experience for SafeCalc vault users.
 *
 * All operations execute seamlessly without external ad network dependencies,
 * avoiding AdServices measurement service binding failures in emulator environments.
 */
class AdMobManager(private val context: Context) {

    fun preloadInterstitialAd() {
        // SafeCalc is fully ad-free for uninterrupted privacy
    }

    fun onUserActionTrigger(activity: Activity?, onFinished: () -> Unit = {}) {
        onFinished()
    }

    fun showInterstitial(activity: Activity, onDismissed: () -> Unit = {}) {
        onDismissed()
    }
}
