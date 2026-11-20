package ir.ghadmino.stepcounter

import android.app.Application

/**
 * Application entry point.
 *
 * Deliberately contains no ad-SDK initialization or ad UI work.
 * The first screen must be allowed to start independently of network,
 * advertising SDKs, or ad inventory.
 */
class GhadminoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Clear legacy coin data from upgrades; the app no longer has a coin economy.
        getSharedPreferences("ghadmino_coins", MODE_PRIVATE).edit().clear().apply()
    }
}
