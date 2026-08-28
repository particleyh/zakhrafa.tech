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

    private const val INTERSTITIAL_CAP_MS = 5L * 60 * 1000

    private var interstitial: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var rewardedLoading = false
    private var rewardedReloadScheduled = false
    private var lastInterstitialShowTime = 0L
    private val mainHandler = Handler(Looper.getMainLooper())
    private var playServicesAvailable = true
    private val rewardedAvailabilityCallbacks = mutableListOf<(Boolean) -> Unit>()

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

    /** Starts a background preload; callers that need an answer should use [requestRewarded]. */
    fun loadRewarded(context: Context) {
        loadRewardedInternal(context)
    }

    /**
     * Reports whether a rewarded ad can be shown now. The returned callback cancels
     * delivery, so an Activity that is closing never receives a stale result.
     */
    fun requestRewarded(context: Context, onAvailability: (Boolean) -> Unit): () -> Unit {
        var active = true
        val guardedCallback: (Boolean) -> Unit = { available ->
            if (active) onAvailability(available)
        }
        if (rewardedAd != null) {
            mainHandler.post { guardedCallback(true) }
        } else if (!playServicesAvailable) {
            mainHandler.post { guardedCallback(false) }
        } else {
            rewardedAvailabilityCallbacks += guardedCallback
            loadRewardedInternal(context)
        }
        return {
            active = false
            rewardedAvailabilityCallbacks.remove(guardedCallback)
        }
    }

    private fun loadRewardedInternal(context: Context) {
        if (!playServicesAvailable || rewardedAd != null || rewardedLoading) return
        rewardedLoading = true
        val appContext = context.applicationContext
        try {
            RewardedAd.load(appContext, REWARDED_UNIT_ID, AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedLoading = false
                        rewardedAd = ad
                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                rewardedAd = null
                                loadRewardedInternal(appContext)
                            }

                            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                                rewardedAd = null
                                loadRewardedInternal(appContext)
                            }
                        }
                        notifyRewardedAvailability(true)
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedLoading = false
                        rewardedAd = null
                        notifyRewardedAvailability(false)
                        scheduleRewardedReload(appContext)
                    }
                })
        } catch (_: Exception) {
            rewardedLoading = false
            rewardedAd = null
            notifyRewardedAvailability(false)
            scheduleRewardedReload(appContext)
        }
    }

    private fun notifyRewardedAvailability(available: Boolean) {
        val callbacks = rewardedAvailabilityCallbacks.toList()
        rewardedAvailabilityCallbacks.clear()
        callbacks.forEach { callback -> mainHandler.post { callback(available) } }
    }

    private fun scheduleRewardedReload(context: Context) {
        if (!playServicesAvailable || rewardedReloadScheduled) return
        rewardedReloadScheduled = true
        mainHandler.postDelayed({
            rewardedReloadScheduled = false
            loadRewardedInternal(context)
        }, 30_000)
    }

    fun showRewarded(activity: Activity, onReward: () -> Unit): Boolean {
        try {
            if (activity.isFinishing || activity.isDestroyed) return false
            val ad = rewardedAd ?: return false
            rewardedAd = null
            ad.show(activity, object : com.google.android.gms.ads.OnUserEarnedRewardListener {
                override fun onUserEarnedReward(rewardItem: RewardItem) { onReward() }
            })
            return true
        } catch (_: Exception) {
            rewardedAd = null
            loadRewarded(activity.applicationContext)
            return false
        }
    }

    fun hasRewardedReady(): Boolean = rewardedAd != null
}
