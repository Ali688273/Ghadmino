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
    private const val FULLSCREEN_COOLDOWN_MS = 15 * 60 * 1000L
    private const val MIN_SESSION_MS = 5 * 60 * 1000L
    private const val MIN_NAVIGATIONS = 6
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
    private var sessionStartedAt = 0L
    private var navigationCount = 0

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun initialize(context: Context) {
        if (sessionStartedAt == 0L) sessionStartedAt = System.currentTimeMillis()
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

    /** Adivery first; fallback to Tapsell. Reward only on explicit completion callback. */
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
        var finished = false
        fun finish(message: String) {
            if (finished) return
            finished = true
            onFinished(message)
        }
        fun grant() {
            if (finished) return
            finished = true
            markRewardGranted(activity)
            onReward(rewardCoins)
            onFinished("پاداش شما اضافه شد.")
        }
        fun tapsellFallback() {
            if (finished || isFinishingOrDestroyed(activity) || !tapsellInitialized) {
                finish("فعلاً تبلیغ جایزه‌ای در دسترس نیست.")
                return
            }
            TapsellPlus.requestRewardedVideoAd(activity, AdsConfig.TAPSELL_REWARDED,
                object : AdRequestCallback() {
                    override fun response(ad: TapsellPlusAdModel) {
                        val id = ad.responseId
                        if (id.isNullOrBlank() || finished || isFinishingOrDestroyed(activity)) {
                            finish("فعلاً تبلیغ جایزه‌ای در دسترس نیست.")
                            return
                        }
                        TapsellPlus.showRewardedVideoAd(activity, id, object : AdShowListener() {
                            override fun onOpened(ad: TapsellPlusAdModel) { markFullscreenShown(activity) }
                            override fun onRewarded(ad: TapsellPlusAdModel) { grant() }
                            override fun onClosed(ad: TapsellPlusAdModel) {
                                if (!finished) finish("برای دریافت جایزه باید تماشای تبلیغ کامل شود.")
                            }
                            override fun onError(error: TapsellPlusErrorModel) {
                                if (!finished) finish("نمایش تبلیغ با خطا روبه‌رو شد.")
                            }
                        })
                    }
                    override fun error(message: String) {
                        Log.w(TAG, "Tapsell rewarded unavailable: $message")
                        finish("فعلاً تبلیغ جایزه‌ای در دسترس نیست.")
                    }
                })
        }
        val placement = AdsConfig.ADIVERY_REWARDED
        val listener = object : AdiveryListener() {
            override fun onRewardedAdLoaded(placementId: String) {
                if (!finished && placementId == placement && Adivery.isLoaded(placement)) Adivery.showAd(placement)
            }
            override fun onRewardedAdShown(placementId: String) {
                if (placementId == placement) markFullscreenShown(activity)
            }
            override fun onRewardedAdClicked(placementId: String) = Unit
            override fun onRewardedAdClosed(placementId: String, isRewarded: Boolean) {
                if (placementId != placement) return
                Adivery.removePlacementListener(placement)
                if (isRewarded) grant()
                else finish("برای دریافت جایزه باید تماشای تبلیغ کامل شود.")
            }
            override fun log(placementId: String, message: String) { Log.d(TAG, "Adivery rewarded: $message") }
        }
        Adivery.addPlacementListener(placement, listener)
        Adivery.prepareRewardedAd(activity, placement)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!finished && !Adivery.isLoaded(placement)) {
                Adivery.removePlacementListener(placement)
                tapsellFallback()
            }
        }, 3000L)
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

    /** Safe App-Open placement; never blocks startup. */
    fun showAppOpen(activity: Activity, onFinished: () -> Unit = {}) {
        initialize(activity)
        if (isFinishingOrDestroyed(activity) || !canShowFullscreen(activity)) {
            onFinished()
            return
        }

        val placement = AdsConfig.ADIVERY_APP_OPEN
        var shown = false
        val listener = object : AdiveryListener() {
            override fun onAppOpenAdLoaded(placementId: String) {
                if (placementId == placement && !shown && Adivery.isLoaded(placement)) {
                    Adivery.showAd(placement)
                }
            }
            override fun onAppOpenAdShown(placementId: String) {
                if (placementId == placement) {
                    shown = true
                    markFullscreenShown(activity)
                }
            }
            override fun onAppOpenAdClicked(placementId: String) = Unit
            override fun onAppOpenAdClosed(placementId: String) {
                if (placementId != placement) return
                Adivery.removePlacementListener(placement)
                onFinished()
            }
            override fun log(placementId: String, message: String) {
                Log.d(TAG, "Adivery app-open: $message")
            }
        }

        Adivery.addPlacementListener(placement, listener)
        Adivery.prepareAppOpenAd(activity, placement)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!shown && !Adivery.isLoaded(placement)) {
                Adivery.removePlacementListener(placement)
                onFinished()
            }
        }, 2500L)
    }

    fun showInterstitial(activity: Activity, onFinished: () -> Unit = {}) {
        navigationCount++
        val now = System.currentTimeMillis()
        if (navigationCount < MIN_NAVIGATIONS || now - sessionStartedAt < MIN_SESSION_MS) {
            onFinished()
            return
        }
        showInterstitialInternal(activity) { onFinished() }
    }

    private fun showInterstitialInternal(
        activity: Activity,
        onFinished: (String) -> Unit
    ) {
        initialize(activity)
        if (isFinishingOrDestroyed(activity) || !canShowFullscreen(activity)) {
            onFinished("برای جلوگیری از مزاحمت، تبلیغ تمام‌صفحه فعلاً نمایش داده نشد.")
            return
        }
        var finished = false
        fun finish(message: String) {
            if (finished) return
            finished = true
            onFinished(message)
        }
        fun tapsellFallback(index: Int = 0) {
            val zones = listOf(AdsConfig.TAPSELL_INSTANT_VIDEO, AdsConfig.TAPSELL_INTERSTITIAL)
            if (finished || isFinishingOrDestroyed(activity)) return
            if (!tapsellInitialized || index >= zones.size) {
                finish("فعلاً تبلیغی در دسترس نیست.")
                return
            }
            val zone = zones[index]
            TapsellPlus.requestInterstitialAd(activity, zone, object : AdRequestCallback() {
                override fun response(ad: TapsellPlusAdModel) {
                    val id = ad.responseId
                    if (id.isNullOrBlank() || finished || isFinishingOrDestroyed(activity)) {
                        tapsellFallback(index + 1)
                        return
                    }
                    TapsellPlus.showInterstitialAd(activity, id, object : AdShowListener() {
                        override fun onOpened(ad: TapsellPlusAdModel) { markFullscreenShown(activity) }
                        override fun onClosed(ad: TapsellPlusAdModel) { finish("تبلیغ تمام شد.") }
                        override fun onError(error: TapsellPlusErrorModel) {
                            Log.w(TAG, "Tapsell fullscreen failed: ${error.errorMessage}")
                            tapsellFallback(index + 1)
                        }
                    })
                }
                override fun error(message: String) {
                    Log.w(TAG, "Tapsell zone $zone unavailable: $message")
                    tapsellFallback(index + 1)
                }
            })
        }
        val placement = AdsConfig.ADIVERY_INTERSTITIAL
        var shown = false
        val listener = object : AdiveryListener() {
            override fun onInterstitialAdLoaded(placementId: String) {
                if (!finished && placementId == placement && Adivery.isLoaded(placement)) Adivery.showAd(placement)
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
                finish("تبلیغ تمام شد.")
            }
            override fun log(placementId: String, message: String) { Log.d(TAG, "Adivery interstitial: $message") }
        }
        Adivery.addPlacementListener(placement, listener)
        if (runCatching { Adivery.isLoaded(placement) }.getOrDefault(false)) {
            runCatching { Adivery.showAd(placement) }.onFailure {
                Adivery.removePlacementListener(placement)
                tapsellFallback()
            }
        } else {
            Adivery.prepareInterstitialAd(activity, placement)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (!finished && !shown && !Adivery.isLoaded(placement)) {
                    Adivery.removePlacementListener(placement)
                    tapsellFallback()
                }
            }, 3000L)
        }
    }

    fun loadBanner(activity: Activity, container: ViewGroup) {
        initialize(activity)
        if (container.childCount > 0) return
        loadAdiveryBanner(activity, container) {
            if (tapsellInitialized && !isFinishingOrDestroyed(activity)) {
                requestTapsellBanner(activity, container, AdsConfig.TAPSELL_STANDARD_BANNER)
            }
        }
    }

    private fun requestTapsellBanner(activity: Activity, container: ViewGroup, zone: String) {
        if (container.childCount > 0 || isFinishingOrDestroyed(activity)) return
        TapsellPlus.requestStandardBannerAd(activity, zone, TapsellPlusBannerType.BANNER_320x50,
            object : AdRequestCallback() {
                override fun response(ad: TapsellPlusAdModel) {
                    val id = ad.responseId
                    if (id.isNullOrBlank() || container.childCount > 0 || isFinishingOrDestroyed(activity)) return
                    TapsellPlus.showStandardBannerAd(activity, id, container, object : AdShowListener() {
                        override fun onOpened(ad: TapsellPlusAdModel) = Unit
                        override fun onError(error: TapsellPlusErrorModel) {
                            container.removeAllViews()
                            Log.w(TAG, "Tapsell banner show failed: ${error.errorMessage}")
                        }
                    })
                }
                override fun error(message: String) { Log.w(TAG, "Tapsell banner unavailable: $message") }
            })
    }

    private fun loadAdiveryBanner(activity: Activity, container: ViewGroup, onFailed: () -> Unit = {}) {
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
                    container.removeView(this@apply)
                    onFailed()
                }
            })
        }

        container.addView(banner)
        banner.loadAd()
    }


    fun loadInstantBanner(activity: Activity, container: ViewGroup) {
        initialize(activity)
        if (container.childCount > 0) return
        loadAdiveryBanner(activity, container) {
            if (tapsellInitialized && !isFinishingOrDestroyed(activity)) {
                requestTapsellBanner(activity, container, AdsConfig.TAPSELL_INSTANT_BANNER)
            }
        }
    }

    fun loadNativeBanner(activity: Activity, container: ViewGroup) {
        initialize(activity)
        if (container.childCount > 0) return
        loadAdiveryBanner(activity, container) {
            if (!tapsellInitialized || isFinishingOrDestroyed(activity) || container.childCount > 0) return@loadAdiveryBanner
            TapsellPlus.requestNativeAd(
                activity,
                AdsConfig.TAPSELL_NATIVE_BANNER,
                object : AdRequestCallback() {
                    override fun response(ad: TapsellPlusAdModel) {
                        val id = ad.responseId
                        if (id.isNullOrBlank() || isFinishingOrDestroyed(activity) || container.childCount > 0) return
                        val holder = TapsellPlus.createAdHolder(
                            activity,
                            container,
                            ir.tapsell.plus.R.layout.native_banner
                        )
                        TapsellPlus.showNativeAd(
                            activity,
                            id,
                            holder,
                            object : AdShowListener() {
                                override fun onOpened(ad: TapsellPlusAdModel) = Unit
                                override fun onError(error: TapsellPlusErrorModel) {
                                    Log.w(TAG, "Tapsell native banner unavailable: ${error.errorMessage}")
                                    container.removeAllViews()
                                }
                            }
                        )
                    }
                    override fun error(message: String) {
                        Log.w(TAG, "Tapsell native banner request failed: $message")
                    }
                }
            )
        }
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
