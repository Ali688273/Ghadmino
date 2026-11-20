package ir.ghadmino.stepcounter.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import com.adivery.sdk.Adivery
import com.adivery.sdk.AdiveryAdListener
import com.adivery.sdk.AdiveryBannerAdView
import com.adivery.sdk.AdiveryListener
import com.adivery.sdk.BannerSize
import ir.ghadmino.stepcounter.BuildConfig
import ir.tapsell.plus.AdRequestCallback
import ir.tapsell.plus.AdShowListener
import ir.tapsell.plus.TapsellPlus
import ir.tapsell.plus.TapsellPlusBannerType
import ir.tapsell.plus.model.TapsellPlusAdModel
import ir.tapsell.plus.model.TapsellPlusErrorModel

/**
 * Ghadmino ad flow mirrors ExcelFarsi-Android:
 * Adivery is primary for interstitial/app-open/banner; Tapsell is fallback.
 * Publisher placement IDs remain Ghadmino-specific.
 */
object GhadminoAdsManager {
    private const val TAG = "GhadminoAds"
    private const val MIN_SESSION_BEFORE_INTERSTITIAL_MS = 5 * 60 * 1000L
    private const val INTERSTITIAL_COOLDOWN_MS = 15 * 60 * 1000L
    private const val APP_OPEN_BACKGROUND_MS = 10 * 60 * 1000L
    private const val MIN_NAVIGATIONS = 6
    private const val MIN_ATTEMPT_INTERVAL_MS = 60 * 1000L

    @Volatile private var tapsellInterstitialInFlight = false
    private var navigationCount = 0
    private var sessionStartedAt = 0L
    private var lastInterstitialAt = 0L
    private var lastInterstitialAttemptAt = 0L
    private var lastAppBackgroundAt = 0L

    private val mainHandler = Handler(Looper.getMainLooper())

    fun registerAdiveryListener() {
        Adivery.addGlobalListener(object : AdiveryListener() {
            override fun onInterstitialAdLoaded(placementId: String) { log("Adivery interstitial loaded") }
            override fun onInterstitialAdShown(placementId: String) { log("Adivery interstitial shown") }
            override fun onInterstitialAdClosed(placementId: String) { log("Adivery interstitial closed") }
            override fun onAppOpenAdLoaded(placementId: String) { log("Adivery app-open loaded") }
            override fun onAppOpenAdShown(placementId: String) { log("Adivery app-open shown") }
            override fun onAppOpenAdClosed(placementId: String) { log("Adivery app-open closed") }
            override fun log(placementId: String, message: String) { log("Adivery: $message") }
        })
    }

    /** Prepare Adivery placements early, without showing an ad on cold start. */
    fun prepare(activity: Activity) {
        if (sessionStartedAt == 0L) sessionStartedAt = System.currentTimeMillis()
        if (activity.isFinishing || activity.isDestroyed) return
        runCatching { Adivery.prepareInterstitialAd(activity, AdsConfig.ADIVERY_INTERSTITIAL) }
            .onFailure { Log.w(TAG, "Could not prepare Adivery interstitial", it) }
        runCatching { Adivery.prepareAppOpenAd(activity, AdsConfig.ADIVERY_APP_OPEN) }
            .onFailure { Log.w(TAG, "Could not prepare Adivery app-open", it) }
    }

    /** Compatibility entry point for ad slots created after Activity startup. */
    fun initialize(context: Context) {
        if (sessionStartedAt == 0L) sessionStartedAt = System.currentTimeMillis()
        (context as? Activity)?.let { prepare(it) }
    }

    fun onUserNavigation(activity: Activity) {
        navigationCount++
        val now = System.currentTimeMillis()
        if (navigationCount < MIN_NAVIGATIONS) return
        if (now - sessionStartedAt < MIN_SESSION_BEFORE_INTERSTITIAL_MS) return
        if (now - lastInterstitialAt < INTERSTITIAL_COOLDOWN_MS) return
        if (now - lastInterstitialAttemptAt < MIN_ATTEMPT_INTERVAL_MS) return
        if (activity.isFinishing || activity.isDestroyed) return

        lastInterstitialAttemptAt = now
        if (runCatching { Adivery.isLoaded(AdsConfig.ADIVERY_INTERSTITIAL) }.getOrDefault(false)) {
            runCatching { Adivery.showAd(AdsConfig.ADIVERY_INTERSTITIAL) }
                .onSuccess { lastInterstitialAt = now }
                .onFailure {
                    Log.w(TAG, "Adivery interstitial show failed", it)
                    requestTapsellInterstitial(activity)
                }
        } else {
            requestTapsellInterstitial(activity)
        }
    }

    /** Kept for existing callers; navigation gating is applied here too. */
    fun showInterstitial(activity: Activity, onFinished: () -> Unit = {}) {
        onUserNavigation(activity)
        onFinished()
    }

    private fun requestTapsellInterstitial(activity: Activity) {
        if (!AdsRuntime.tapsellReady || tapsellInterstitialInFlight ||
            activity.isFinishing || activity.isDestroyed
        ) return
        tapsellInterstitialInFlight = true

        fun requestZone(zoneId: String, tryInstantBannerIfEmpty: Boolean) {
            if (activity.isFinishing || activity.isDestroyed) {
                tapsellInterstitialInFlight = false
                return
            }
            TapsellPlus.requestInterstitialAd(activity, zoneId, object : AdRequestCallback() {
                override fun response(ad: TapsellPlusAdModel) {
                    tapsellInterstitialInFlight = false
                    if (activity.isFinishing || activity.isDestroyed) return
                    val responseId = ad.responseId
                    if (responseId.isNullOrBlank()) {
                        if (tryInstantBannerIfEmpty) requestZone(AdsConfig.TAPSELL_INSTANT_BANNER, false)
                        return
                    }
                    TapsellPlus.showInterstitialAd(activity, responseId, object : AdShowListener() {
                        override fun onClosed(ad: TapsellPlusAdModel) {
                            lastInterstitialAt = System.currentTimeMillis()
                            log("Tapsell instant video closed")
                            prepare(activity)
                        }
                        override fun onError(error: TapsellPlusErrorModel) {
                            log("Tapsell instant video show error: $error")
                        }
                    })
                }

                override fun error(message: String) {
                    log("Tapsell request failed ($zoneId): $message")
                    if (tryInstantBannerIfEmpty && AdsRuntime.tapsellReady) {
                        requestZone(AdsConfig.TAPSELL_INSTANT_BANNER, false)
                    } else {
                        tapsellInterstitialInFlight = false
                    }
                }
            })
        }

        runCatching { requestZone(AdsConfig.TAPSELL_INSTANT_VIDEO, true) }
            .onFailure {
                tapsellInterstitialInFlight = false
                Log.w(TAG, "Tapsell instant video request failed", it)
            }
    }

    fun onAppBackgrounded() {
        lastAppBackgroundAt = System.currentTimeMillis()
    }

    fun onAppForegrounded(activity: Activity) {
        val now = System.currentTimeMillis()
        val wasAwayLongEnough = lastAppBackgroundAt > 0L &&
            now - lastAppBackgroundAt >= APP_OPEN_BACKGROUND_MS
        lastAppBackgroundAt = 0L
        if (!wasAwayLongEnough || activity.isFinishing || activity.isDestroyed) return
        if (now - lastInterstitialAt < INTERSTITIAL_COOLDOWN_MS) return

        if (runCatching { Adivery.isLoaded(AdsConfig.ADIVERY_APP_OPEN) }.getOrDefault(false)) {
            runCatching {
                Adivery.showAppOpenAd(activity, AdsConfig.ADIVERY_APP_OPEN)
                lastInterstitialAt = now
            }.onFailure { Log.w(TAG, "Adivery app-open show failed", it) }
        } else {
            runCatching { Adivery.prepareAppOpenAd(activity, AdsConfig.ADIVERY_APP_OPEN) }
                .onFailure { Log.w(TAG, "Could not refresh Adivery app-open", it) }
        }
    }

    fun loadBanner(activity: Activity, container: ViewGroup) {
        initialize(activity)
        if (container.childCount > 0) return
        loadAdiveryBanner(activity, container) {
            requestTapsellStandardBanner(activity, container, 0)
        }
    }

    private fun requestTapsellStandardBanner(activity: Activity, container: ViewGroup, attempt: Int) {
        if (activity.isFinishing || activity.isDestroyed || container.childCount > 0) return
        if (!AdsRuntime.tapsellReady) {
            if (attempt < 10) mainHandler.postDelayed({
                requestTapsellStandardBanner(activity, container, attempt + 1)
            }, 1_000L)
            return
        }
        TapsellPlus.requestStandardBannerAd(
            activity,
            AdsConfig.TAPSELL_STANDARD_BANNER,
            TapsellPlusBannerType.BANNER_320x50,
            object : AdRequestCallback() {
                override fun response(ad: TapsellPlusAdModel) {
                    if (activity.isFinishing || activity.isDestroyed || container.childCount > 0) return
                    TapsellPlus.showStandardBannerAd(activity, ad.responseId, container, object : AdShowListener() {
                        override fun onOpened(ad: TapsellPlusAdModel) { log("Tapsell banner opened") }
                        override fun onError(error: TapsellPlusErrorModel) {
                            Log.w(TAG, "Tapsell banner show failed: $error")
                            container.removeAllViews()
                        }
                    })
                }
                override fun error(message: String) { log("Tapsell banner request failed: $message") }
            }
        )
    }

    private fun loadAdiveryBanner(activity: Activity, container: ViewGroup, onFailed: () -> Unit) {
        if (container.childCount > 0 || activity.isFinishing || activity.isDestroyed) return
        val banner = AdiveryBannerAdView(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPlacementId(AdsConfig.ADIVERY_BANNER)
            setBannerSize(BannerSize.BANNER)
            setBannerAdListener(object : AdiveryAdListener() {
                override fun onAdLoaded() { log("Adivery banner loaded") }
                override fun onAdShown() = Unit
                override fun onAdClicked() = Unit
                override fun onError(reason: String) {
                    Log.w(TAG, "Adivery banner failed: $reason")
                    container.removeView(this@apply)
                    onFailed()
                }
            })
        }
        container.addView(banner)
        banner.loadAd()
    }

    fun loadInstantBanner(activity: Activity, container: ViewGroup) {
        loadBanner(activity, container)
    }

    /**
     * Native-slot parity requires a Ghadmino-specific Adivery Native placement ID.
     * Until that ID is configured, retain the existing Ghadmino banner -> Tapsell
     * native-banner fallback rather than borrowing Excel Farsi's publisher ID.
     */
    fun loadNativeBanner(activity: Activity, container: ViewGroup) {
        initialize(activity)
        if (container.childCount > 0) return
        loadAdiveryBanner(activity, container) {
            if (!AdsRuntime.tapsellReady || activity.isFinishing || activity.isDestroyed ||
                container.childCount > 0
            ) return@loadAdiveryBanner
            TapsellPlus.requestNativeAd(activity, AdsConfig.TAPSELL_NATIVE_BANNER, object : AdRequestCallback() {
                override fun response(ad: TapsellPlusAdModel) {
                    val id = ad.responseId
                    if (id.isNullOrBlank() || activity.isFinishing || activity.isDestroyed ||
                        container.childCount > 0
                    ) return
                    val holder = TapsellPlus.createAdHolder(
                        activity, container, ir.tapsell.plus.R.layout.native_banner
                    )
                    TapsellPlus.showNativeAd(activity, id, holder, object : AdShowListener() {
                        override fun onOpened(ad: TapsellPlusAdModel) = Unit
                        override fun onError(error: TapsellPlusErrorModel) {
                            Log.w(TAG, "Tapsell native banner failed: $error")
                            container.removeAllViews()
                        }
                    })
                }
                override fun error(message: String) { log("Tapsell native banner request failed: $message") }
            })
        }
    }

    fun loadNativeVideo(activity: Activity, container: ViewGroup) {
        initialize(activity)
        if (!AdsRuntime.tapsellReady || container.childCount > 0 ||
            activity.isFinishing || activity.isDestroyed
        ) return
        TapsellPlus.requestNativeVideo(activity, AdsConfig.TAPSELL_NATIVE_VIDEO, object : AdRequestCallback() {
            override fun response(ad: TapsellPlusAdModel) {
                val responseId = ad.responseId
                if (responseId.isNullOrBlank() || activity.isFinishing || activity.isDestroyed) return
                val holder = ir.tapsell.plus.TapsellPlusVideoAdHolder.Builder()
                    .setContentViewTemplate(ir.ghadmino.stepcounter.R.layout.ghadmino_native_video_ad)
                    .setAppInstallationViewTemplate(ir.tapsell.sdk.R.layout.tapsell_app_installation_video_ad_template)
                    .setAdContainer(container)
                    .build()
                TapsellPlus.showNativeVideo(activity, responseId, holder, object : AdShowListener() {
                    override fun onOpened(ad: TapsellPlusAdModel) { log("Tapsell native video opened") }
                    override fun onError(error: TapsellPlusErrorModel) {
                        Log.w(TAG, "Tapsell native video failed: ${error.errorMessage}")
                        container.removeAllViews()
                    }
                })
            }
            override fun error(message: String) { log("Tapsell native video request failed: $message") }
        })
    }

    private fun log(message: String) = Log.d(TAG, message)
}
