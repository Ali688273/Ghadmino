package ir.ghadmino.stepcounter

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
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

        // Ad SDK failures must never prevent the main application from starting.
        runCatching {
            GhadminoAdsManager.initialize(this)
        }.onFailure {
            Log.e("GhadminoAds", "Initial ad SDK setup failed; continuing without ads.", it)
        }

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // Do not prepare an App-Open ad during Activity creation.
                // Some SDK/device combinations can perform UI work too early
                // and crash before the first screen is rendered.
            }

            override fun onActivityStarted(activity: Activity) {
                startedActivities++

                if (startedActivities == 1 && !countedSession) {
                    countedSession = true
                    handleNewAppSession(activity)
                }
            }

            override fun onActivityResumed(activity: Activity) {
                runCatching {
                    GhadminoAdsManager.initialize(activity)
                }.onFailure {
                    Log.e("GhadminoAds", "Ad SDK resume initialization failed.", it)
                }

                val now = System.currentTimeMillis()
                val awayLongEnough = hasBeenBackgrounded && now - lastPausedAt >= 20_000L
                val cooldownElapsed = now - lastAppOpenShownAt >= 10 * 60_000L

                if (!awayLongEnough || !cooldownElapsed || activity.isFinishing) return

                // Let the Activity finish rendering before touching App-Open UI.
                activity.window?.decorView?.postDelayed({
                    if (activity.isFinishing) return@postDelayed

                    runCatching {
                        val placement = AdsConfig.ADIVERY_APP_OPEN
                        if (Adivery.isLoaded(placement)) {
                            lastAppOpenShownAt = System.currentTimeMillis()
                            Adivery.showAppOpenAd(activity, placement)
                        } else {
                            Adivery.prepareAppOpenAd(activity, placement)
                        }
                    }.onFailure {
                        Log.e("GhadminoAds", "App-Open ad failed; continuing normally.", it)
                    }
                }, 800L)
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

        // Never show an automatic fullscreen ad before the first screen is
        // rendered. On every third entry, wait briefly and fail safely.
        if (count % 3 == 0 && !activity.isFinishing) {
            activity.window?.decorView?.postDelayed({
                if (activity.isFinishing) return@postDelayed

                runCatching {
                    GhadminoAdsManager.showInterstitial(activity)
                }.onFailure {
                    Log.e("GhadminoAds", "Automatic interstitial failed; continuing normally.", it)
                }
            }, 1800L)
        }
    }
}
