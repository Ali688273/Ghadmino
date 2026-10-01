package ir.ghadmino.stepcounter.ads

import android.app.Activity
import android.app.Application
import android.content.Context
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
import ir.tapsell.plus.TapsellPlusInitListener
import ir.tapsell.plus.model.AdNetworkError
import ir.tapsell.plus.model.AdNetworks
import ir.tapsell.plus.model.TapsellPlusAdModel
import ir.tapsell.plus.model.TapsellPlusErrorModel

object GhadminoAdsManager {

    private const val TAG = "GhadminoAds"
    private const val FULLSCREEN_COOLDOWN_MS = 5 * 60 * 1000L
    private const val REWARD_COOLDOWN_MS = 60 * 1000L
    private const val MAX_REWARD_ADS_PER_DAY = 5

    private const val PREFS = "ghadmino_ads"
    private const val LAST_FULLSCREEN = "last_fullscreen"
    private const val LAST_REWARD = "last_reward"
    private const val REWARD_DAY = "reward_day"
    private const val REWARD_COUNT = "reward_count"

    @Volatile private var initialized = false
    @Volatile private var tapsellInitializationStarted = false
    @Volatile private var tapsellInitialized = false

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun initialize(context: Context) {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    Adivery.setLoggingEnabled(BuildConfig.DEBUG)
                    Adivery.configure(context.applicationContext as Application, AdsConfig.ADIVERY_APP_KEY)
                    initialized = true
                }
            }
        }

        if (context is Activity && !tapsellInitializationStarted) {
            synchronized(this) {
                if (!tapsellInitializationStarted) {
                    tapsellInitializationStarted = true
                    TapsellPlus.initialize(
                        context.applicationContext as Application,
                        AdsConfig.TAPSELL_APP_KEY,
                        object : TapsellPlusInitListener {
                            override fun onInitializeSuccess(adNetworks: AdNetworks) {
                                tapsellInitialized = true
                                Log.d(TAG, "Tapsell initialized: ${adNetworks.name}")
                            }

                            override fun onInitializeFailed(
                                adNetworks: AdNetworks,
                                adNetworkError: AdNetworkError
                            ) {
                                tapsellInitialized = false
                                Log.w(TAG, "Tapsell init failed: ${adNetworks.name} - ${adNetworkError.errorMessage}")
                            }
                        }
                    )
                }
            }
        }
    }

    /**
     * User-initiated coin reward.
     *
     * Primary:
     * 1) Tapsell rewarded
     * 2) Adivery rewarded
     *
     * Backup reward:
     * 3) Tapsell interstitial
     * 4) Adivery interstitial
     *
     * The backup path is explicitly a smaller fallback reward and is never
     * described as a rewarded-ad event.
     */
    fun showCoinReward(
        activity: Activity,
        rewardCoins: Int = 25,
        fallbackCoins: Int = 10,
        onReward: (coins: Int) -> Unit,
        onFinished: (message: String) -> Unit = {}
    ) {
        initialize(activity)

        if (isFinishingOrDestroyed(activity)) {
            onFinished("نمایش تبلیغ ممکن نیست.")
            return
        }

        if (!canGrantReward(activity)) {
            onFinished("سهمیه تبلیغ جایزه‌ای فعلاً تمام شده یا کمی بعد دوباره امتحان کن.")
            return
        }

        var completed = false

        fun rewardOnce(coins: Int, message: String) {
            if (completed) return
            completed = true
            markRewardGranted(activity)
            onReward(coins)
            onFinished(message)
        }

        fun fallbackInterstitial() {
            showInterstitialInternal(
                activity = activity,
                grantCoins = fallbackCoins,
                onReward = { coins ->
                    rewardOnce(coins, "تبلیغ جایزه‌ای موجود نبود؛ از تبلیغ جایگزین استفاده شد و +$coins سکه اضافه شد.")
                },
                onFinished = { message ->
                    if (!completed) onFinished(message)
                }
            )
        }

        fun adiveryRewarded() {
            val placement = AdsConfig.ADIVERY_REWARDED
            val listener = object : AdiveryListener() {
                override fun onRewardedAdLoaded(placementId: String) {
                    if (placementId == placement && Adivery.isLoaded(placement)) {
                        Adivery.showAd(placement)
                    }
                }

                override fun onRewardedAdShown(placementId: String) {
                    if (placementId == placement) markFullscreenShown(activity)
                }

                override fun onRewardedAdClicked(placementId: String) = Unit

                override fun onRewardedAdClosed(placementId: String, isRewarded: Boolean) {
                    if (placementId != placement) return
                    Adivery.removePlacementListener(placement)
                    if (isRewarded) {
                        rewardOnce(rewardCoins, "پاداش شما اضافه شد.")
                    } else if (!completed) {
                        fallbackInterstitial()
                    }
                }

                override fun log(placementId: String, message: String) {
                    Log.d(TAG, "Adivery rewarded: $message")
                }
            }

            Adivery.addPlacementListener(placement, listener)
            Adivery.prepareRewardedAd(activity, placement)

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (!completed && !Adivery.isLoaded(placement)) {
                    Adivery.removePlacementListener(placement)
                    fallbackInterstitial()
                }
            }, 2500L)
        }

        if (!tapsellInitialized) {
            adiveryRewarded()
            return
        }

        TapsellPlus.requestRewardedVideoAd(
            activity,
            AdsConfig.TAPSELL_REWARDED,
            object : AdRequestCallback() {
                override fun response(ad: TapsellPlusAdModel) {
                    val responseId = ad.responseId
                    if (responseId.isNullOrBlank()) {
                        adiveryRewarded()
                        return
                    }

                    TapsellPlus.showRewardedVideoAd(
                        activity,
                        responseId,
                        object : AdShowListener() {
                            override fun onOpened(ad: TapsellPlusAdModel) {
                                markFullscreenShown(activity)
                            }

                            override fun onRewarded(ad: TapsellPlusAdModel) {
                                rewardOnce(rewardCoins, "پاداش شما اضافه شد.")
                            }

                            override fun onClosed(ad: TapsellPlusAdModel) {
                                if (!completed) adiveryRewarded()
                            }

                            override fun onError(error: TapsellPlusErrorModel) {
                                if (!completed) adiveryRewarded()
                            }
                        }
                    )
                }

                override fun error(message: String) {
                    Log.w(TAG, "Tapsell rewarded unavailable: $message")
                    adiveryRewarded()
                }
            }
        )
    }

    /** Backward-compatible wrapper used by existing UI. */
    fun showRewarded(
        activity: Activity,
        onReward: () -> Unit,
        onFinished: (message: String) -> Unit = {}
    ) {
        showCoinReward(
            activity = activity,
            rewardCoins = 25,
            fallbackCoins = 10,
            onReward = { onReward() },
            onFinished = onFinished
        )
    }

    fun showInterstitial(
        activity: Activity,
        onFinished: () -> Unit = {}
    ) {
        showInterstitialInternal(
            activity = activity,
            grantCoins = null,
            onReward = {},
            onFinished = { onFinished() }
        )
    }

    private fun showInterstitialInternal(
        activity: Activity,
        grantCoins: Int?,
        onReward: (Int) -> Unit,
        onFinished: (String) -> Unit
    ) {
        initialize(activity)

        if (!canShowFullscreen(activity)) {
            onFinished("برای جلوگیری از مزاحمت، تبلیغ تمام‌صفحه فعلاً نمایش داده نشد.")
            return
        }

        fun showAdiveryPlacement(placement: String) {
            var shown = false
            val listener = object : AdiveryListener() {
                override fun onInterstitialAdLoaded(placementId: String) {
                    if (placementId == placement && Adivery.isLoaded(placement)) {
                        Adivery.showAd(placement)
                    }
                }

                override fun onInterstitialAdShown(placementId: String) {
                    if (placementId == placement) {
                        shown = true
                        markFullscreenShown(activity)
                    }
                }

                override fun onInterstitialAdClicked(placementId: String) = Unit

                override fun onInterstitialAdClosed(placementId: String) {
                    if (placementId != placement) return
                    Adivery.removePlacementListener(placement)
                    if (grantCoins != null && shown) onReward(grantCoins)
                    onFinished(if (grantCoins != null && shown) "تبلیغ جایگزین کامل شد." else "تبلیغ تمام شد.")
                }

                override fun log(placementId: String, message: String) {
                    Log.d(TAG, "Adivery interstitial: $message")
                }
            }

            Adivery.addPlacementListener(placement, listener)
            Adivery.prepareInterstitialAd(activity, placement)

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (!shown && !Adivery.isLoaded(placement)) {
                    Adivery.removePlacementListener(placement)
                    if (placement == AdsConfig.ADIVERY_INTERSTITIAL) {
                        showAdiveryPlacement(AdsConfig.ADIVERY_PRE_ROLL)
                    } else {
                        onFinished("فعلاً تبلیغی در دسترس نیست.")
                    }
                }
            }, 2500L)
        }

        fun showAdivery() {
            showAdiveryPlacement(AdsConfig.ADIVERY_INTERSTITIAL)
        }

        if (!tapsellInitialized) {
            showAdivery()
            return
        }

        var shown = false
        var previewTried = false

        fun tryPreviewVideo() {
            if (previewTried) {
                showAdivery()
                return
            }
            previewTried = true
            TapsellPlus.requestInterstitialAd(
                activity,
                AdsConfig.TAPSELL_PREVIEW_VIDEO,
                object : AdRequestCallback() {
                    override fun response(ad: TapsellPlusAdModel) {
                        val id = ad.responseId
                        if (id.isNullOrBlank()) {
                            showAdivery()
                            return
                        }
                        TapsellPlus.showInterstitialAd(
                            activity,
                            id,
                            object : AdShowListener() {
                                override fun onOpened(ad: TapsellPlusAdModel) {
                                    markFullscreenShown(activity)
                                }
                                override fun onClosed(ad: TapsellPlusAdModel) {
                                    onFinished("تبلیغ تمام شد.")
                                }
                                override fun onError(error: TapsellPlusErrorModel) {
                                    showAdivery()
                                }
                            }
                        )
                    }
                    override fun error(message: String) {
                        showAdivery()
                    }
                }
            )
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
                                shown = true
                                markFullscreenShown(activity)
                            }

                            override fun onClosed(ad: TapsellPlusAdModel) {
                                if (grantCoins != null && shown) onReward(grantCoins)
                                onFinished(if (grantCoins != null && shown) "تبلیغ جایگزین کامل شد." else "تبلیغ تمام شد.")
                            }

                            override fun onError(error: TapsellPlusErrorModel) {
                                tryPreviewVideo()
                            }
                        }
                    )
                }

                override fun error(message: String) {
                    tryPreviewVideo()
                }
            }
        )
    }

    fun loadBanner(activity: Activity, container: ViewGroup) {
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
                    loadAdiveryBanner(activity, container)
                }
            }
        )
    }

    private fun loadAdiveryBanner(activity: Activity, container: ViewGroup) {
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


    fun loadInstantBanner(activity: Activity, container: ViewGroup) {
        initialize(activity)
        if (container.childCount > 0) return
        if (!tapsellInitialized) {
            loadAdiveryBanner(activity, container)
            return
        }
        TapsellPlus.requestStandardBannerAd(
            activity,
            AdsConfig.TAPSELL_INSTANT_BANNER,
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
                    loadAdiveryBanner(activity, container)
                }
            }
        )
    }

    fun loadNativeBanner(activity: Activity, container: ViewGroup) {
        initialize(activity)
        if (container.childCount > 0) return
        if (!tapsellInitialized) {
            loadAdiveryBanner(activity, container)
            return
        }

        TapsellPlus.requestNativeAd(
            activity,
            AdsConfig.TAPSELL_NATIVE_BANNER,
            object : AdRequestCallback() {
                override fun response(ad: TapsellPlusAdModel) {
                    val responseId = ad.responseId
                    if (responseId.isNullOrBlank() || isFinishingOrDestroyed(activity)) {
                        if (!isFinishingOrDestroyed(activity)) loadAdiveryBanner(activity, container)
                        return
                    }

                    val holder = TapsellPlus.createAdHolder(
                        activity,
                        container,
                        ir.tapsell.plus.R.layout.native_banner
                    )

                    TapsellPlus.showNativeAd(
                        activity,
                        responseId,
                        holder,
                        object : AdShowListener() {
                            override fun onOpened(ad: TapsellPlusAdModel) = Unit
                            override fun onError(error: TapsellPlusErrorModel) {
                                Log.w(TAG, "Tapsell native banner unavailable: " + error.errorMessage)
                                container.removeAllViews()
                                loadAdiveryBanner(activity, container)
                            }
                        }
                    )
                }

                override fun error(message: String) {
                    Log.w(TAG, "Tapsell native banner request failed: " + message)
                    loadAdiveryBanner(activity, container)
                }
            }
        )
    }

    fun loadNativeVideo(activity: Activity, container: ViewGroup) {
        initialize(activity)
        if (!tapsellInitialized || container.childCount > 0) return

        TapsellPlus.requestNativeVideo(
            activity,
            AdsConfig.TAPSELL_NATIVE_VIDEO,
            object : AdRequestCallback() {
                override fun response(ad: TapsellPlusAdModel) {
                    val responseId = ad.responseId
                    if (responseId.isNullOrBlank() || isFinishingOrDestroyed(activity)) return

                    val holder = ir.tapsell.plus.TapsellPlusVideoAdHolder.Builder()
                        .setContentViewTemplate(ir.ghadmino.stepcounter.R.layout.ghadmino_native_video_ad)
                        .setAppInstallationViewTemplate(
                            ir.tapsell.sdk.R.layout.tapsell_app_installation_video_ad_template
                        )
                        .setAdContainer(container)
                        .build()

                    TapsellPlus.showNativeVideo(
                        activity,
                        responseId,
                        holder,
                        object : AdShowListener() {
                            override fun onOpened(ad: TapsellPlusAdModel) {
                                Log.d(TAG, "Tapsell native video opened")
                            }

                            override fun onError(error: TapsellPlusErrorModel) {
                                Log.w(TAG, "Tapsell native video unavailable: " + error.errorMessage)
                                container.removeAllViews()
                            }
                        }
                    )
                }

                override fun error(message: String) {
                    Log.w(TAG, "Tapsell native video request failed: " + message)
                }
            }
        )
    }

    private fun canShowFullscreen(context: Context): Boolean {
        val last = prefs(context).getLong(LAST_FULLSCREEN, 0L)
        return System.currentTimeMillis() - last >= FULLSCREEN_COOLDOWN_MS
    }

    private fun markFullscreenShown(context: Context) {
        prefs(context).edit()
            .putLong(LAST_FULLSCREEN, System.currentTimeMillis())
            .apply()
    }

    private fun canGrantReward(context: Context): Boolean {
        val p = prefs(context)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())

        if (p.getString(REWARD_DAY, null) != today) {
            p.edit().putString(REWARD_DAY, today).putInt(REWARD_COUNT, 0).apply()
        }

        val count = p.getInt(REWARD_COUNT, 0)
        val last = p.getLong(LAST_REWARD, 0L)

        return count < MAX_REWARD_ADS_PER_DAY &&
            System.currentTimeMillis() - last >= REWARD_COOLDOWN_MS
    }

    private fun markRewardGranted(context: Context) {
        val p = prefs(context)
        p.edit()
            .putLong(LAST_REWARD, System.currentTimeMillis())
            .putInt(REWARD_COUNT, p.getInt(REWARD_COUNT, 0) + 1)
            .apply()
    }

    private fun isFinishingOrDestroyed(activity: Activity): Boolean =
        activity.isFinishing ||
            (android.os.Build.VERSION.SDK_INT >= 17 && activity.isDestroyed)
}
