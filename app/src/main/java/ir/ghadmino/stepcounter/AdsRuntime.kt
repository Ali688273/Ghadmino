package ir.ghadmino.stepcounter

/**
 * Runtime SDK readiness state shared by the Application and ad manager.
 */
object AdsRuntime {
    @Volatile
    var tapsellReady: Boolean = false
}
