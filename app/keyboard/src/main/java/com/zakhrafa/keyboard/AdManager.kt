package com.zakhrafa.keyboard

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.common.GoogleApiAvailability

object AdManager {

    private const val BANNER_UNIT_ID = "ca-app-pub-5275292033164657/7539871794"
    private const val INTERSTITIAL_UNIT_ID = "ca-app-pub-5275292033164657/8389919038"
    private const val REWARDED_UNIT_ID = "ca-app-pub-5275292033164657/7364491834"

    private const val LOCK_INTERVAL_MS = 10L * 24 * 60 * 60 * 1000
    private const val INTERSTITIAL_CAP_MS = 5L * 60 * 1000
    private const val NEVER_UNLOCKED = -1L

    private var interstitial: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var lastInterstitialShowTime = 0L
    private val mainHandler = Handler(Looper.getMainLooper())
    private var playServicesAvailable = true

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences("ad_state", Context.MODE_PRIVATE)

    fun initialize(context: Context) {
        try {
            val appContext = context.applicationContext
            val availability = GoogleApiAvailability.getInstance()
            val resultCode = availability.isGooglePlayServicesAvailable(appContext)
            playServicesAvailable = resultCode == com.google.android.gms.common.ConnectionResult.SUCCESS
            if (playServicesAvailable) {
                MobileAds.initialize(appContext) {}
            }
        } catch (_: Exception) {
            playServicesAvailable = false
        }
    }

    // ─── 10-Day Keyboard Lock ───────────────────────────────────

    fun isKeyboardLocked(context: Context): Boolean {
        val p = prefs(context)
        val lastUnlock = p.getLong("last_unlock_time", NEVER_UNLOCKED)
        if (lastUnlock == NEVER_UNLOCKED) return false
        return System.currentTimeMillis() - lastUnlock > LOCK_INTERVAL_MS
    }

    fun markUnlocked(context: Context) {
        prefs(context).edit().putLong("last_unlock_time", System.currentTimeMillis()).apply()
    }

    fun markFirstUse(context: Context) {
        val p = prefs(context)
        val v = p.getLong("last_unlock_time", NEVER_UNLOCKED)
        if (v == NEVER_UNLOCKED) {
            p.edit().putLong("last_unlock_time", System.currentTimeMillis()).apply()
        }
    }

    fun getDaysLeft(context: Context): Int {
        val lastUnlock = prefs(context).getLong("last_unlock_time", NEVER_UNLOCKED)
        if (lastUnlock == NEVER_UNLOCKED) return 10
        val elapsed = System.currentTimeMillis() - lastUnlock
        val daysUsed = (elapsed / (24 * 60 * 60 * 1000)).toInt()
        return (10 - daysUsed).coerceIn(0, 10)
    }

    // ─── Premium Styles ─────────────────────────────────────────

    fun isPremiumStyleUnlocked(context: Context, styleName: String): Boolean {
        return prefs(context).getBoolean("premium_$styleName", false)
    }

    fun unlockPremiumStyle(context: Context, styleName: String) {
        prefs(context).edit().putBoolean("premium_$styleName", true).apply()
    }

    // ─── Rewarded Themes ────────────────────────────────────────

    fun isPremiumThemeUnlocked(context: Context, themeName: String): Boolean {
        return prefs(context).getBoolean("theme_$themeName", false)
    }

    fun unlockPremiumTheme(context: Context, themeName: String) {
        prefs(context).edit().putBoolean("theme_$themeName", true).apply()
    }

    // ─── Banner ─────────────────────────────────────────────────

    fun loadBanner(adView: AdView) {
        if (!playServicesAvailable) return
        try {
            adView.loadAd(AdRequest.Builder().build())
        } catch (_: Exception) {}
    }

    // ─── Interstitial ───────────────────────────────────────────

    fun loadInterstitial(context: Context) {
        if (!playServicesAvailable) return
        val appContext = context.applicationContext
        try {
            InterstitialAd.load(appContext, INTERSTITIAL_UNIT_ID, AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitial = ad
                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                interstitial = null
                                loadInterstitial(appContext)
                            }
                        }
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitial = null
                        mainHandler.postDelayed({ loadInterstitial(appContext) }, 30_000)
                    }
                })
        } catch (_: Exception) {
            mainHandler.postDelayed({ loadInterstitial(appContext) }, 30_000)
        }
    }

    fun showInterstitialIfReady(activity: Activity): Boolean {
        try {
            if (activity.isFinishing || activity.isDestroyed) return false
            val now = System.currentTimeMillis()
            if (now - lastInterstitialShowTime < INTERSTITIAL_CAP_MS) return false
            val ad = interstitial ?: return false
            lastInterstitialShowTime = now
            ad.show(activity)
            return true
        } catch (_: Exception) {
            interstitial = null
            return false
        }
    }

    // ─── Rewarded ───────────────────────────────────────────────

    fun loadRewarded(context: Context) {
        if (!playServicesAvailable) return
        val appContext = context.applicationContext
        try {
            RewardedAd.load(appContext, REWARDED_UNIT_ID, AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                rewardedAd = null
                                loadRewarded(appContext)
                            }
                        }
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedAd = null
                        mainHandler.postDelayed({ loadRewarded(appContext) }, 30_000)
                    }
                })
        } catch (_: Exception) {
            mainHandler.postDelayed({ loadRewarded(appContext) }, 30_000)
        }
    }

    fun showRewarded(activity: Activity, onReward: () -> Unit): Boolean {
        try {
            if (activity.isFinishing || activity.isDestroyed) return false
            val ad = rewardedAd ?: return false
            ad.show(activity, object : com.google.android.gms.ads.OnUserEarnedRewardListener {
                override fun onUserEarnedReward(rewardItem: RewardItem) { onReward() }
            })
            return true
        } catch (_: Exception) {
            rewardedAd = null
            return false
        }
    }

    fun hasRewardedReady(): Boolean = rewardedAd != null
}
