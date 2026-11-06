package com.zakhrafa.app

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Pure interstitial eligibility rules, kept separate from the SDK so they can
 * be unit tested. The intent: the first ads arrive only after the user has
 * clearly engaged (5 copies), never while the rating dialog is open, never
 * back-to-back (60s floor), and never more than 3 times per session.
 */
internal object AdPolicy {
    const val FIRST_INTERSTITIAL_AT_COPY = 5
    const val EVERY_N_COPIES = 3
    const val MIN_GAP_MS = 60_000L
    const val MAX_PER_SESSION = 3

    fun shouldShow(
        sessionCopies: Int,
        sessionShown: Int,
        lastShownAt: Long,
        now: Long,
        ratingDialogVisible: Boolean,
        adFree: Boolean
    ): Boolean {
        if (adFree || ratingDialogVisible) return false
        if (sessionShown >= MAX_PER_SESSION) return false
        if (sessionCopies < FIRST_INTERSTITIAL_AT_COPY) return false
        if ((sessionCopies - FIRST_INTERSTITIAL_AT_COPY) % EVERY_N_COPIES != 0) return false
        if (lastShownAt > 0 && now - lastShownAt < MIN_GAP_MS) return false
        return true
    }
}

object AdManager {

    const val BANNER_UNIT_ID = "ca-app-pub-5275292033164657/8014031353"
    private const val INTERSTITIAL_UNIT_ID = "ca-app-pub-5275292033164657/5012463498"
    private const val REWARDED_UNIT_ID = "ca-app-pub-5275292033164657/5877008517"

    private const val PREFS_NAME = "zakhrafa_ads"
    private const val AD_FREE_UNTIL_KEY = "ad_free_until"
    private const val LAST_INTERSTITIAL_KEY = "last_interstitial_at"
    private const val AD_FREE_DURATION_MS = 24L * 60 * 60 * 1000
    private const val INTERSTITIAL_RELOAD_DELAY_MS = 30_000L
    private const val INTERSTITIAL_SHOW_DELAY_MS = 500L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val _adFree = MutableStateFlow(false)
    val adFree: StateFlow<Boolean> = _adFree

    private var interstitial: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var rewardedLoading = false
    private var rewardedReloadScheduled = false
    private var sessionCopyCount = 0
    private var sessionInterstitialCount = 0
    private var playServicesAvailable = true
    private val rewardedAvailabilityCallbacks = mutableListOf<(Boolean) -> Unit>()

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun initialize(context: Context) {
        try {
            val appContext = context.applicationContext
            val resultCode = GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(appContext)
            playServicesAvailable = resultCode == ConnectionResult.SUCCESS
            if (playServicesAvailable) {
                MobileAds.initialize(appContext) {}
                loadInterstitial(appContext)
                loadRewarded(appContext)
            }
        } catch (_: Exception) {
            playServicesAvailable = false
        }
        refreshAdFreeStatus(context)
    }

    // ─── Ad-free 24h mode (rewarded) ──────────────────────────

    fun refreshAdFreeStatus(context: Context) {
        val until = prefs(context.applicationContext).getLong(AD_FREE_UNTIL_KEY, 0L)
        _adFree.value = until > System.currentTimeMillis()
    }

    fun startAdFree(context: Context) {
        val until = System.currentTimeMillis() + AD_FREE_DURATION_MS
        prefs(context.applicationContext).edit().putLong(AD_FREE_UNTIL_KEY, until).apply()
        _adFree.value = true
        runCatching {
            com.google.firebase.analytics.FirebaseAnalytics
                .getInstance(context.applicationContext)
                .logEvent("ad_free_earned", null)
        }
    }

    // ─── Banner ───────────────────────────────────────────────

    // The banner AdView is created in AdBanner.kt; unit id is exposed there.

    // ─── Interstitial ─────────────────────────────────────────

    fun loadInterstitial(context: Context) {
        if (!playServicesAvailable) return
        val appContext = context.applicationContext
        try {
            InterstitialAd.load(
                appContext, INTERSTITIAL_UNIT_ID, AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitial = ad
                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                interstitial = null
                                loadInterstitial(appContext)
                            }

                            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                                interstitial = null
                                loadInterstitial(appContext)
                            }
                        }
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitial = null
                        mainHandler.postDelayed({ loadInterstitial(appContext) }, INTERSTITIAL_RELOAD_DELAY_MS)
                    }
                })
        } catch (_: Exception) {
            mainHandler.postDelayed({ loadInterstitial(appContext) }, INTERSTITIAL_RELOAD_DELAY_MS)
        }
    }

    /**
     * Called after every successful copy. Shows an interstitial only when the
     * pure [AdPolicy] rules pass; the short delay lets the "تم النسخ ✓" toast
     * land before the ad takes over the screen.
     */
    fun onSuccessfulCopy(context: Context, ratingDialogVisible: Boolean) {
        sessionCopyCount += 1
        val appContext = context.applicationContext
        if (_adFree.value) return
        val now = System.currentTimeMillis()
        val last = prefs(appContext).getLong(LAST_INTERSTITIAL_KEY, 0L)
        val eligible = AdPolicy.shouldShow(
            sessionCopies = sessionCopyCount,
            sessionShown = sessionInterstitialCount,
            lastShownAt = last,
            now = now,
            ratingDialogVisible = ratingDialogVisible,
            adFree = _adFree.value
        )
        if (!eligible) return
        val activity = context as? Activity ?: return
        mainHandler.postDelayed({
            showInterstitialIfStillEligible(activity, appContext)
        }, INTERSTITIAL_SHOW_DELAY_MS)
    }

    private fun showInterstitialIfStillEligible(activity: Activity, appContext: Context) {
        try {
            if (activity.isFinishing || activity.isDestroyed) return
            if (_adFree.value) return
            val ad = interstitial ?: return
            val now = System.currentTimeMillis()
            val last = prefs(appContext).getLong(LAST_INTERSTITIAL_KEY, 0L)
            if (now - last < AdPolicy.MIN_GAP_MS) return
            if (sessionInterstitialCount >= AdPolicy.MAX_PER_SESSION) return
            interstitial = null
            sessionInterstitialCount += 1
            prefs(appContext).edit().putLong(LAST_INTERSTITIAL_KEY, now).apply()
            ad.show(activity)
        } catch (_: Exception) {
            interstitial = null
        }
    }

    // ─── Rewarded (ad-free 24h) ───────────────────────────────

    /**
     * Reports whether a rewarded ad can be shown now. The returned callback
     * cancels delivery so a screen that is closing never gets a stale result.
     */
    fun requestRewarded(context: Context, onAvailability: (Boolean) -> Unit): () -> Unit {
        var active = true
        val guarded: (Boolean) -> Unit = { available -> if (active) onAvailability(available) }
        if (rewardedAd != null) {
            mainHandler.post { guarded(true) }
        } else if (!playServicesAvailable) {
            mainHandler.post { guarded(false) }
        } else {
            rewardedAvailabilityCallbacks += guarded
            loadRewardedInternal(context)
        }
        return {
            active = false
            rewardedAvailabilityCallbacks.remove(guarded)
        }
    }

    fun loadRewarded(context: Context) {
        loadRewardedInternal(context)
    }

    private fun loadRewardedInternal(context: Context) {
        if (!playServicesAvailable || rewardedAd != null || rewardedLoading) return
        rewardedLoading = true
        val appContext = context.applicationContext
        try {
            RewardedAd.load(
                appContext, REWARDED_UNIT_ID, AdRequest.Builder().build(),
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
        }, INTERSTITIAL_RELOAD_DELAY_MS)
    }

    /**
     * [onReward] fires the instant the reward is earned — persist the unlock
     * there, never in the dismiss callback (which can lag or be skipped).
     */
    fun showRewarded(activity: Activity, onReward: () -> Unit, onDismissed: () -> Unit = {}): Boolean {
        try {
            if (activity.isFinishing || activity.isDestroyed) return false
            val ad = rewardedAd ?: return false
            rewardedAd = null
            ad.show(activity) { rewardItem: RewardItem ->
                mainHandler.post { runCatching { onReward() } }
            }
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    ad.fullScreenContentCallback = null
                    rewardedAd = null
                    loadRewardedInternal(activity.applicationContext)
                    mainHandler.post { runCatching { onDismissed() } }
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    ad.fullScreenContentCallback = null
                    rewardedAd = null
                    loadRewardedInternal(activity.applicationContext)
                    mainHandler.post { runCatching { onDismissed() } }
                }
            }
            return true
        } catch (_: Exception) {
            rewardedAd = null
            loadRewarded(activity.applicationContext)
            return false
        }
    }
}
