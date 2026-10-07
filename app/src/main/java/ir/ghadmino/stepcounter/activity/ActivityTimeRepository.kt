package ir.ghadmino.stepcounter.activity

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

/**
 * Estimates active walking time from real step-counter events.
 *
 * A step event is considered active time only between consecutive steps.
 * Long gaps are capped so standing/phone-idle time is not counted as walking.
 */
object ActivityTimeRepository {
    private const val PREFS = "ghadmino_activity_time"
    private const val KEY_DATE = "date"
    private const val KEY_ACTIVE_SECONDS = "active_seconds"
    private const val KEY_LAST_STEP_MILLIS = "last_step_millis"
    private const val MAX_GAP_SECONDS = 120L

    @Synchronized
    fun onStepEvent(context: Context, deltaSteps: Int, nowMillis: Long = System.currentTimeMillis()) {
        if (deltaSteps <= 0) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = todayKey()
        val savedDate = prefs.getString(KEY_DATE, null)

        if (savedDate != today) {
            prefs.edit()
                .putString(KEY_DATE, today)
                .putLong(KEY_ACTIVE_SECONDS, 0L)
                .putLong(KEY_LAST_STEP_MILLIS, nowMillis)
                .apply()
            return
        }

        val last = prefs.getLong(KEY_LAST_STEP_MILLIS, 0L)
        val gapSeconds = if (last > 0L && nowMillis >= last) {
            ((nowMillis - last) / 1000L).coerceIn(0L, MAX_GAP_SECONDS)
        } else {
            0L
        }

        // If the sensor reports multiple steps at once, include the interval
        // only once; this avoids counting the same elapsed time multiple times.
        val active = prefs.getLong(KEY_ACTIVE_SECONDS, 0L) + gapSeconds
        prefs.edit()
            .putString(KEY_DATE, today)
            .putLong(KEY_ACTIVE_SECONDS, active)
            .putLong(KEY_LAST_STEP_MILLIS, nowMillis)
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
