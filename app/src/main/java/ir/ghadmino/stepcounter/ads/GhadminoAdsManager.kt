package ir.ghadmino.stepcounter.ads

import ir.ghadmino.stepcounter.BuildConfig

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.ViewGroup
import com.adivery.sdk.Adivery
import com.adivery.sdk.AdiveryAdListener
import com.adivery.sdk.AdiveryBannerAdView
import com.adivery.sdk.AdiveryListener
import com.adivery.sdk.BannerSize
import ir.tapsell.plus.AdRequestCallback
import ir.tapsell.plus.AdShowListener
import ir.tapsell.plus.TapsellPlus
import ir.tapsell.plus.TapsellPlusBannerType
import ir.tapsell.plus.TapsellPlusInitListener
import ir.tapsell.plus.model.AdNetworkError
import ir.tapsell.plus.model.AdNetworks
import ir.tapsell.plus.model.TapsellPlusAdModel
import ir.tapsell.plus.model.TapsellPlusErrorModel

/**
 * Centralized, low-intrusion ad controller.
 *
 * Rules:
 * - Rewarded ads are user initiated.
 * - Tapsell rewarded is tried first, then Adivery rewarded.
 * - A non-rewarded interstitial is never treated as a rewarded ad.
 * - Full-screen ads have a cooldown.
 * - Ads are never requested from the background step counter service.
 * - Banner is optional and only attached where the UI explicitly asks for it.
 */
object GhadminoAdsManager {

    private const val TAG = "GhadminoAds"

    private const val FULLSCREEN_COOLDOWN_MS = 5 * 60 * 1000L
    private const val LAST_FULLSCREEN_KEY = "ghadmino_ads"
    private const val LAST_FULLSCREEN_TIME = "last_fullscreen"

    @Volatile
    private var initialized = false

    @Volatile
    private var tapsellInitialized = false

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(LAST_FULLSCREEN_KEY, Context.MODE_PRIVATE)

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return

            Adivery.setLoggingEnabled(BuildConfig.DEBUG)
            Adivery.configure(context.applicationContext, AdsConfig.ADIVERY_APP_KEY)

            TapsellPlus.initialize(
                context.applicationContext,
                AdsConfig.TAPSELL_APP_KEY,
                object : TapsellPlusInitListener {
                    override fun onInitializeSuccess(adNetworks: AdNetworks) {
                        tapsellInitialized = true
                        Log.d(TAG, "TapsellPlus initialized: ${adNetworks.name}")
                    }

                    override fun onInitializeFailed(
                        adNetworks: AdNetworks,
                        adNetworkError: AdNetworkError
                    ) {
                        tapsellInitialized = false
                        Log.w(
                            TAG,
                            "TapsellPlus init failed: ${adNetworks.name} - ${adNetworkError.errorMessage}"
                        )
                    }
                }
            )

            initialized = true
        }
    }

    /**
     * Shows a rewarded ad and calls [onReward] exactly once if the network
     * confirms that the user earned the reward.
     *
     * Fallback order:
     * 1) Tapsell rewarded
     * 2) Adivery rewarded
     *
     * If neither rewarded placement is available, no coin is granted.
     */
    fun showRewarded(
        activity: Activity,
        onReward: () -> Unit,
        onFinished: (message: String) -> Unit = {}
    ) {
        initialize(activity)

        if (isFinishingOrDestroyed(activity)) {
            onFinished("نمایش تبلیغ ممکن نیست.")
            return
        }

        var rewardedDelivered = false

        fun rewardOnce() {
            if (rewardedDelivered) return
            rewardedDelivered = true
            onReward()
        }

        fun showAdiveryRewarded() {
            val placement = AdsConfig.ADIVERY_REWARDED

            val listener = object : AdiveryListener() {
                override fun onRewardedAdLoaded(placementId: String) {
                    if (placementId == placement && Adivery.isLoaded(placementId)) {
                        Adivery.showAd(placementId)
                    }
                }

                override fun onRewardedAdShown(placementId: String) {
                    markFullscreenShown(activity)
                }

                override fun onRewardedAdClicked(placementId: String) = Unit

                override fun onRewardedAdClosed(
                    placementId: String,
                    isRewarded: Boolean
                ) {
                    if (placementId != placement) return
                    Adivery.removePlacementListener(placement)
                    if (isRewarded) {
                        rewardOnce()
                        onFinished("پاداش سکه‌ای شما اضافه شد.")
                    } else {
                        onFinished("تبلیغ کامل تماشا نشد؛ سکه‌ای اضافه نشد.")
                    }
                }

                override fun log(placementId: String, message: String) {
                    Log.d(TAG, "Adivery rewarded: $message")
                }
            }

            Adivery.addPlacementListener(placement, listener)
            Adivery.prepareRewardedAd(activity, placement)

            // Adivery reports the result through the listener. If it is not
            // ready shortly after preparation, the SDK simply remains silent
            // and the caller is not blocked.
        }

        if (!tapsellInitialized) {
            showAdiveryRewarded()
            return
        }

        TapsellPlus.requestRewardedVideoAd(
            activity,
            AdsConfig.TAPSELL_REWARDED,
            object : AdRequestCallback() {
                override fun response(ad: TapsellPlusAdModel) {
                    val responseId = ad.responseId
                    if (responseId.isNullOrBlank()) {
                        showAdiveryRewarded()
                        return
                    }

                    TapsellPlus.showRewardedVideoAd(
                        activity,
                        responseId,
                        object : AdShowListener() {
                            override fun onOpened(ad: TapsellPlusAdModel) {
                                markFullscreenShown(activity)
                            }

                            override fun onClosed(ad: TapsellPlusAdModel) {
                                if (!rewardedDelivered) {
                                    onFinished("تبلیغ تمام شد.")
                                }
                            }

                            override fun onRewarded(ad: TapsellPlusAdModel) {
                                rewardOnce()
                                onFinished("پاداش سکه‌ای شما اضافه شد.")
                            }

                            override fun onError(error: TapsellPlusErrorModel) {
                                if (!rewardedDelivered) {
                                    showAdiveryRewarded()
                                }
                            }
                        }
                    )
                }

                override fun error(message: String) {
                    Log.w(TAG, "Tapsell rewarded unavailable: $message")
                    showAdiveryRewarded()
                }
            }
        )
    }

    /**
     * Shows a normal interstitial only when the app explicitly requests one
     * and the cooldown has elapsed. It never grants a rewarded coin.
     */
    fun showInterstitial(
        activity: Activity,
        onFinished: () -> Unit = {}
    ) {
        initialize(activity)

        if (!canShowFullscreen(activity)) {
            onFinished()
            return
        }

        fun showAdivery() {
            val placement = AdsConfig.ADIVERY_INTERSTITIAL
            val listener = object : AdiveryListener() {
                override fun onInterstitialAdLoaded(placementId: String) {
                    if (placementId == placement && Adivery.isLoaded(placementId)) {
                        Adivery.showAd(placementId)
                    }
                }

                override fun onInterstitialAdShown(placementId: String) {
                    markFullscreenShown(activity)
                }

                override fun onInterstitialAdClicked(placementId: String) = Unit

                override fun onInterstitialAdClosed(placementId: String) {
                    if (placementId == placement) {
                        Adivery.removePlacementListener(placement)
                        onFinished()
                    }
                }

                override fun log(placementId: String, message: String) {
                    Log.d(TAG, "Adivery interstitial: $message")
                }
            }

            Adivery.addPlacementListener(placement, listener)
            Adivery.prepareInterstitialAd(activity, placement)
        }

        if (!tapsellInitialized) {
            showAdivery()
            return
        }

        TapsellPlus.requestInterstitialAd(
            activity,
            AdsConfig.TAPSELL_INTERSTITIAL,
            object : AdRequestCallback() {
                override fun response(ad: TapsellPlusAdModel) {
                    val responseId = ad.responseId
                    if (responseId.isNullOrBlank()) {
                        showAdivery()
                        return
                    }

                    TapsellPlus.showInterstitialAd(
                        activity,
                        responseId,
                        object : AdShowListener() {
                            override fun onOpened(ad: TapsellPlusAdModel) {
                                markFullscreenShown(activity)
                            }

                            override fun onClosed(ad: TapsellPlusAdModel) {
                                onFinished()
                            }

                            override fun onError(error: TapsellPlusErrorModel) {
                                showAdivery()
                            }
                        }
                    )
                }

                override fun error(message: String) {
                    Log.w(TAG, "Tapsell interstitial unavailable: $message")
                    showAdivery()
                }
            }
        )
    }

    /**
     * Adds one small banner to [container]. Tapsell is tried first and
     * Adivery is the fallback. Existing child views are not overwritten.
     */
    fun loadBanner(
        activity: Activity,
        container: ViewGroup
    ) {
        initialize(activity)

        if (container.childCount > 0) return

        if (!tapsellInitialized) {
            loadAdiveryBanner(activity, container)
            return
        }

        TapsellPlus.requestStandardBannerAd(
            activity,
            AdsConfig.TAPSELL_STANDARD_BANNER,
            TapsellPlusBannerType.BANNER_320x50,
            object : AdRequestCallback() {
                override fun response(ad: TapsellPlusAdModel) {
                    val responseId = ad.responseId
                    if (responseId.isNullOrBlank()) {
                        loadAdiveryBanner(activity, container)
                        return
                    }

                    TapsellPlus.showStandardBannerAd(
                        activity,
                        responseId,
                        container,
                        object : AdShowListener() {
                            override fun onOpened(ad: TapsellPlusAdModel) = Unit

                            override fun onError(error: TapsellPlusErrorModel) {
                                container.removeAllViews()
                                loadAdiveryBanner(activity, container)
                            }
                        }
                    )
                }

                override fun error(message: String) {
                    Log.w(TAG, "Tapsell banner unavailable: $message")
                    loadAdiveryBanner(activity, container)
                }
            }
        )
    }

    private fun loadAdiveryBanner(
        activity: Activity,
        container: ViewGroup
    ) {
        if (container.childCount > 0) return

        val banner = AdiveryBannerAdView(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPlacementId(AdsConfig.ADIVERY_BANNER)
            setBannerSize(BannerSize.BANNER)
            setBannerAdListener(object : AdiveryAdListener() {
                override fun onAdLoaded() = Unit
                override fun onAdShown() = Unit
                override fun onAdClicked() = Unit
                override fun onError(reason: String) {
                    Log.w(TAG, "Adivery banner unavailable: $reason")
                }
            })
        }

        container.addView(banner)
        banner.loadAd()
    }

    private fun canShowFullscreen(context: Context): Boolean {
        val last = prefs(context).getLong(LAST_FULLSCREEN_TIME, 0L)
        return SystemClock.elapsedRealtime() - last >= FULLSCREEN_COOLDOWN_MS
    }

    private fun markFullscreenShown(context: Context) {
        prefs(context).edit()
            .putLong(LAST_FULLSCREEN_TIME, SystemClock.elapsedRealtime())
            .apply()
    }

    private fun isFinishingOrDestroyed(activity: Activity): Boolean =
        activity.isFinishing || (android.os.Build.VERSION.SDK_INT >= 17 && activity.isDestroyed)
}
