package ir.ghadmino.stepcounter

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.adivery.sdk.Adivery
import ir.ghadmino.stepcounter.ads.AdsConfig
import ir.ghadmino.stepcounter.ads.GhadminoAdsManager

class GhadminoApplication : Application() {

    private var lastPausedAt = 0L
    private var lastAppOpenShownAt = 0L
    private var hasBeenBackgrounded = false

    private var startedActivities = 0
    private var countedSession = false

    private val adsPrefs by lazy {
        getSharedPreferences("ghadmino_ads", MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()

        GhadminoAdsManager.initialize(this)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                Adivery.prepareAppOpenAd(activity, AdsConfig.ADIVERY_APP_OPEN)
            }

            override fun onActivityStarted(activity: Activity) {
                startedActivities++

                if (startedActivities == 1 && !countedSession) {
                    countedSession = true
                    handleNewAppSession(activity)
                }
            }

            override fun onActivityResumed(activity: Activity) {
                GhadminoAdsManager.initialize(activity)

                val now = System.currentTimeMillis()
                val awayLongEnough = hasBeenBackgrounded && now - lastPausedAt >= 20_000L
                val cooldownElapsed = now - lastAppOpenShownAt >= 10 * 60_000L

                if (!awayLongEnough || !cooldownElapsed || activity.isFinishing) return

                val placement = AdsConfig.ADIVERY_APP_OPEN
                if (Adivery.isLoaded(placement)) {
                    lastAppOpenShownAt = now
                    Adivery.showAppOpenAd(activity, placement)
                } else {
                    Adivery.prepareAppOpenAd(activity, placement)
                }
            }

            override fun onActivityPaused(activity: Activity) {
                lastPausedAt = System.currentTimeMillis()
                hasBeenBackgrounded = true
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities = (startedActivities - 1).coerceAtLeast(0)
                if (startedActivities == 0) {
                    countedSession = false
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    private fun handleNewAppSession(activity: Activity) {
        val count = adsPrefs.getInt("foreground_session_count", 0) + 1
        adsPrefs.edit().putInt("foreground_session_count", count).apply()

        // Automatic monetization: every third app entry/session attempts
        // a fullscreen ad. The AdsManager's cooldown and network fallback
        // prevent back-to-back fullscreen interruptions.
        if (count % 3 == 0 && !activity.isFinishing) {
            GhadminoAdsManager.showInterstitial(activity)
        }
    }
}
