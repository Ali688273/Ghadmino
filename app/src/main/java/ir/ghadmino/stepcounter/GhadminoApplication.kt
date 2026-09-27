package ir.ghadmino.stepcounter

import android.app.Application
import ir.ghadmino.stepcounter.ads.GhadminoAdsManager

class GhadminoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        GhadminoAdsManager.initialize(this)
    }
}
