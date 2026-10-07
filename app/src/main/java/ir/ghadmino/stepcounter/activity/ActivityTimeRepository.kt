package ir.ghadmino.stepcounter.activity

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

/**
 * Estimates actual walking/activity duration from the number of steps received.
 *
 * The duration is derived from steps and an observed walking cadence instead of
 * treating idle time between sensor events as walking time. This prevents long
 * sensor gaps, phone idle periods, or delayed callbacks from creating fake time.
 */
object ActivityTimeRepository {
    private const val PREFS = "ghadmino_activity_time"
    private const val KEY_DATE = "date"
    private const val KEY_ACTIVE_SECONDS = "active_seconds"
    private const val KEY_CADENCE_SPM = "cadence_spm"

    private const val DEFAULT_CADENCE_SPM = 100.0
    private const val MIN_CADENCE_SPM = 45.0
    private const val MAX_CADENCE_SPM = 150.0

    @Synchronized
    fun onStepEvent(
        context: Context,
        deltaSteps: Int,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        if (deltaSteps <= 0) return

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = todayKey()
        val savedDate = prefs.getString(KEY_DATE, null)

        if (savedDate != today) {
            prefs.edit()
                .putString(KEY_DATE, today)
                .putLong(KEY_ACTIVE_SECONDS, 0L)
                .putFloat(KEY_CADENCE_SPM, DEFAULT_CADENCE_SPM.toFloat())
                .apply()
        }

        val currentCadence = prefs
            .getFloat(KEY_CADENCE_SPM, DEFAULT_CADENCE_SPM.toFloat())
            .toDouble()
            .coerceIn(MIN_CADENCE_SPM, MAX_CADENCE_SPM)

        // Estimate duration from the actual number of new steps.
        // Example: 5,000 steps at 100 steps/min ≈ 50 minutes.
        val activeSecondsForSteps = deltaSteps * 60.0 / currentCadence

        // When callbacks arrive frequently, use them to learn the user's
        // actual cadence. A long callback gap is deliberately ignored so
        // standing/idle time is never converted into walking time.
        val lastEvent = prefs.getLong("last_event_millis", 0L)
        var updatedCadence = currentCadence

        if (lastEvent > 0L && nowMillis > lastEvent) {
            val gapSeconds = (nowMillis - lastEvent) / 1000.0
            if (gapSeconds in 1.0..10.0) {
                val observedCadence = deltaSteps * 60.0 / gapSeconds
                if (observedCadence in MIN_CADENCE_SPM..MAX_CADENCE_SPM) {
                    // Smooth the cadence so one irregular sensor callback
                    // cannot suddenly change the whole day's calculation.
                    updatedCadence = currentCadence * 0.8 + observedCadence * 0.2
                }
            }
        }

        val activeSeconds = prefs.getLong(KEY_ACTIVE_SECONDS, 0L) +
            activeSecondsForSteps.coerceAtLeast(0.0).toLong()

        prefs.edit()
            .putString(KEY_DATE, today)
            .putLong(KEY_ACTIVE_SECONDS, activeSeconds)
            .putFloat(KEY_CADENCE_SPM, updatedCadence.toFloat())
            .putLong("last_event_millis", nowMillis)
            .apply()
    }

    fun todayActiveSeconds(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_DATE, null) != todayKey()) return 0L
        return prefs.getLong(KEY_ACTIVE_SECONDS, 0L).coerceAtLeast(0L)
    }

    fun todayActiveMinutes(context: Context): Int {
        val seconds = todayActiveSeconds(context)
        return if (seconds <= 0L) 0 else ceil(seconds / 60.0).toInt()
    }

    fun activeMinutesForDate(context: Context, date: String): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_DATE, null) != date) return 0
        val seconds = prefs.getLong(KEY_ACTIVE_SECONDS, 0L).coerceAtLeast(0L)
        return if (seconds <= 0L) 0 else ceil(seconds / 60.0).toInt()
    }

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
