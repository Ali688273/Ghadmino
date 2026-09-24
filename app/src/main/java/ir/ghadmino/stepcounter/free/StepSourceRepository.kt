package ir.ghadmino.stepcounter.free

import android.content.Context

enum class StepSource { PHONE, HEALTH_CONNECT, AUTO }

object StepSourceRepository {
    private const val PREFS = "ghadmino_free_features"
    private const val KEY_SOURCE = "step_source"
    fun get(context: Context): StepSource = when (context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SOURCE, "PHONE")) {
        "HEALTH_CONNECT" -> StepSource.HEALTH_CONNECT
        "AUTO" -> StepSource.AUTO
        else -> StepSource.PHONE
    }
    fun set(context: Context, source: StepSource) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_SOURCE, source.name).apply() }
    fun label(source: StepSource): String = when (source) {
        StepSource.PHONE -> "حسگر خود گوشی"
        StepSource.HEALTH_CONNECT -> "Health Connect"
        StepSource.AUTO -> "خودکار، بدون جمع‌کردن دو منبع"
    }
}
