package com.example.calcvault.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AdMobManager handles loading, caching, and showing Interstitial Video Ads for SafeCalc.
 *
 * Implements subtle, non-annoying ad frequency capping so users are not spammed.
 * No ads on the calculator screen ever to maintain 100% camouflage!
 */
class AdMobManager(private val context: Context) {

    private var interstitialAd: InterstitialAd? = null
    private var isLoadingAd = false
    private var actionCounter = 0
    private var isInitialized = false
    private var isInitializing = false

    private fun ensureInitialized(onInitialized: () -> Unit = {}) {
        if (isInitialized) {
            onInitialized()
            return
        }
        if (isInitializing) return
        isInitializing = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val requestConfig = RequestConfiguration.Builder().build()
                MobileAds.setRequestConfiguration(requestConfig)
                MobileAds.initialize(context) { initializationStatus ->
                    Log.d("AdMobManager", "AdMob MobileAds initialized: $initializationStatus")
                    isInitialized = true
                    isInitializing = false
                    CoroutineScope(Dispatchers.Main).launch {
                        onInitialized()
                    }
                }
            } catch (t: Throwable) {
                isInitializing = false
                Log.w("AdMobManager", "AdMob init safely handled: ${t.message}")
            }
        }
    }

    /**
     * Preloads an interstitial video ad in the background when requested.
     */
    fun preloadInterstitialAd() {
        if (isLoadingAd || interstitialAd != null) return

        ensureInitialized {
            if (isLoadingAd || interstitialAd != null) return@ensureInitialized

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    isLoadingAd = true
                    val adRequest = AdRequest.Builder().build()
                    val adUnitId = AdMobConfig.interstitialAdUnitId

                    Log.d("AdMobManager", "Preloading interstitial ad with ID: $adUnitId")

                    InterstitialAd.load(
                        context,
                        adUnitId,
                        adRequest,
                        object : InterstitialAdLoadCallback() {
                            override fun onAdLoaded(ad: InterstitialAd) {
                                interstitialAd = ad
                                isLoadingAd = false
                                Log.d("AdMobManager", "Interstitial Video Ad successfully loaded!")
                            }

                            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                interstitialAd = null
                                isLoadingAd = false
                                Log.w("AdMobManager", "Interstitial Ad failed to load: ${loadAdError.message}")
                            }
                        }
                    )
                } catch (t: Throwable) {
                    isLoadingAd = false
                    Log.w("AdMobManager", "Error in preload: ${t.message}")
                }
            }
        }
    }

    /**
     * Increments action counter and shows an ad if threshold is reached.
     * If ad is not ready or threshold is not met, [onFinished] is called immediately.
     */
    fun onUserActionTrigger(activity: Activity?, onFinished: () -> Unit = {}) {
        actionCounter++
        Log.d("AdMobManager", "User action registered. Current count: $actionCounter / ${AdMobConfig.AD_INTERVAL_ACTIONS}")

        if (actionCounter >= AdMobConfig.AD_INTERVAL_ACTIONS) {
            if (activity != null && interstitialAd != null) {
                showInterstitial(activity) {
                    actionCounter = 0
                    onFinished()
                }
                return
            }
        }

        // Preload lazily only after multiple in-vault user actions have occurred
        if (actionCounter >= 2 && interstitialAd == null && !isLoadingAd) {
            preloadInterstitialAd()
        }

        onFinished()
    }

    /**
     * Directly displays the loaded interstitial video ad.
     */
    fun showInterstitial(activity: Activity, onDismissed: () -> Unit = {}) {
        val currentAd = interstitialAd
        if (currentAd != null) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d("AdMobManager", "Interstitial Ad dismissed by user.")
                    interstitialAd = null
                    preloadInterstitialAd()
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w("AdMobManager", "Ad failed to show: ${adError.message}")
                    interstitialAd = null
                    preloadInterstitialAd()
                    onDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d("AdMobManager", "Interstitial Ad is now visible.")
                }
            }
            currentAd.show(activity)
        } else {
            Log.d("AdMobManager", "No ad ready to show, continuing seamlessly.")
            preloadInterstitialAd()
            onDismissed()
        }
    }
}
