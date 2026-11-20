package ir.ghadmino.stepcounter

import android.app.Application
import android.util.Log
import com.adivery.sdk.Adivery
import ir.ghadmino.stepcounter.ads.AdsConfig
import ir.ghadmino.stepcounter.ads.GhadminoAdsManager
import ir.tapsell.plus.TapsellPlus
import ir.tapsell.plus.TapsellPlusInitListener
import ir.tapsell.plus.model.AdNetworkError
import ir.tapsell.plus.model.AdNetworks

class GhadminoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Remove legacy wallet data from users upgrading from older versions.
        getSharedPreferences("ghadmino_coins", MODE_PRIVATE).edit().clear().apply()

        Adivery.setLoggingEnabled(BuildConfig.DEBUG)
        runCatching { Adivery.configure(this, AdsConfig.ADIVERY_APP_KEY) }
            .onFailure { Log.w(TAG, "Adivery initialization failed", it) }

        runCatching {
            TapsellPlus.setDebugMode(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
            TapsellPlus.initialize(this, AdsConfig.TAPSELL_APP_KEY, object : TapsellPlusInitListener {
                override fun onInitializeSuccess(adNetworks: AdNetworks) {
                    AdsRuntime.tapsellReady = true
                    Log.i(TAG, "Tapsell initialized: ${adNetworks.name}")
                }

                override fun onInitializeFailed(adNetworks: AdNetworks, adNetworkError: AdNetworkError) {
                    AdsRuntime.tapsellReady = false
                    Log.w(TAG, "Tapsell initialization failed: ${adNetworks.name} / ${adNetworkError.errorMessage}")
                }
            })
        }.onFailure {
            AdsRuntime.tapsellReady = false
            Log.w(TAG, "Tapsell initialization failed", it)
        }

        GhadminoAdsManager.registerAdiveryListener()
    }

    companion object {
        private const val TAG = "GhadminoAds"
    }
}

