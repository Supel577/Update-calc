package com.example.calcvault.data.ads

/**
 * AdMob Configuration for SafeCalc.
 *
 * Easily switch [USE_TEST_ADS] between testing and production!
 */
object AdMobConfig {

    /**
     * Set to true during testing (uses official Google AdMob test IDs to protect your account).
     * Set to false when publishing with your real AdMob IDs.
     */
    const val USE_TEST_ADS = false

    // Official Google AdMob Sample Test IDs:
    // Sample App ID: ca-app-pub-3940256099942544~3347511713
    // Sample Interstitial Video Ad ID: ca-app-pub-3940256099942544/8691691433
    const val TEST_INTERSTITIAL_VIDEO_AD_UNIT_ID = "ca-app-pub-3940256099942544/8691691433"

    // Real AdMob Interstitial Video ID from user's AdMob console:
    const val PROD_INTERSTITIAL_VIDEO_AD_UNIT_ID = "ca-app-pub-9118208957656773/8089858486"

    val interstitialAdUnitId: String
        get() = if (USE_TEST_ADS) TEST_INTERSTITIAL_VIDEO_AD_UNIT_ID else PROD_INTERSTITIAL_VIDEO_AD_UNIT_ID

    /**
     * Minimum user actions before showing an interstitial video ad.
     * E.g. every 4th action (closing media viewer, locking vault, switching folders).
     * Keeps user experience clean and prevents ad fatigue.
     */
    const val AD_INTERVAL_ACTIONS = 4
}
