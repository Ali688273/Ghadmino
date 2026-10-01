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

    override fun onCreate() {
        super.onCreate()

        GhadminoAdsManager.initialize(this)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                Adivery.prepareAppOpenAd(activity, AdsConfig.ADIVERY_APP_OPEN)
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

            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
