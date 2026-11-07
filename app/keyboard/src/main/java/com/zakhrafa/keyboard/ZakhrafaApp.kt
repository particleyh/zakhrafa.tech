package com.zakhrafa.keyboard

import android.app.Application
import android.os.Handler
import android.os.Looper

/**
 * Starts the rewarded-ad preload at process birth so the unlock screen usually
 * has an ad ready the moment it opens. Deferred one hop off the startup path:
 * MobileAds init does binder work against GMS and doing it inline on the IME
 * cold-start path historically contributed to ANRs.
 */
class ZakhrafaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Handler(Looper.getMainLooper()).post {
            runCatching {
                AdManager.initialize(this)
                AdManager.loadRewarded(this)
            }
        }
    }
}
