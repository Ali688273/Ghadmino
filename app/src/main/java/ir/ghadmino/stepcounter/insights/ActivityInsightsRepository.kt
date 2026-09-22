package ir.ghadmino.stepcounter.insights

import android.content.Context
import ir.ghadmino.stepcounter.step.StepCounterService
import ir.ghadmino.stepcounter.step.StepHistory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class ActivityInsights(
    val score: Int,
    val activeHours: Int,
    val peakHour: Int,
    val peakSteps: Int,
    val goalProgress: Int,
    val etaMinutes: Int?,
    val bestDaySteps: Int,
    val bestStreak: Int,
    val message: String
)

object ActivityInsightsRepository {
    private const val PREFS = "ghadmino_hourly"
    private const val PREFIX = "h_"

    fun recordStepChange(context: Context, delta: Int) {
        if (delta <= 0) return
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val key = PREFIX + today() + "_" + hour
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit().putInt(key, p.getInt(key, 0) + delta).apply()
    }

    fun hourly(context: Context): List<Int> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return (0..23).map { p.getInt(PREFIX + today() + "_" + it, 0) }
    }

    fun calculate(context: Context, goal: Int): ActivityInsights {
        val steps = StepCounterService.todaySteps
        val hours = hourly(context)
        val activeHours = hours.count { it >= 100 }
        val peakSteps = hours.maxOrNull() ?: 0
        val peakHour = hours.indexOfFirst { it == peakSteps }.coerceAtLeast(0)
        val goalProgress = if (goal > 0) ((steps * 100f) / goal).roundToInt().coerceIn(0, 100) else 0
        val recent = StepHistory.recent(context, 7)
        val recentAverage = if (recent.isEmpty()) 0 else recent.sumOf { it.second } / recent.size
        val pacePerMinute = if (steps >= 200) {
            val elapsedMinutes = ((System.currentTimeMillis() - startOfDayMillis()) / 60000L).coerceAtLeast(1L)
            steps.toFloat() / elapsedMinutes
        } else if (recentAverage > 0) recentAverage / (12f * 60f) else 0f
        val remaining = (goal - steps).coerceAtLeast(0)
        val eta = if (remaining > 0 && pacePerMinute > 0.05f)
            (remaining / pacePerMinute).roundToInt().coerceAtMost(24 * 60) else null
        val score = (
            goalProgress * 0.6f +
                (activeHours.coerceAtMost(8) / 8f) * 20f +
                (if (steps >= 10000) 10f else steps / 1000f) +
                (if (recentAverage >= goal) 10f else 0f)
            ).roundToInt().coerceIn(0, 100)
        val bestDay = recent.maxOfOrNull { it.second } ?: 0
        val bestStreak = StepHistory.currentStreak(context, goal)
        val message = when {
            score >= 90 -> "امروز عالی پیش رفتی؛ فقط ادامه بده."
            score >= 70 -> "عملکرد امروز خیلی خوب است."
            score >= 40 -> "در مسیر خوبی هستی؛ چند حرکت دیگر امتیازت را بالاتر می‌برد."
            else -> "با چند دقیقه پیاده‌روی می‌توانی امتیازت را بهتر کنی."
        }
        return ActivityInsights(score, activeHours, peakHour, peakSteps, goalProgress, eta, bestDay, bestStreak, message)
    }

    fun formatHour(hour: Int): String = String.format(Locale.US, "%02d:00", hour)

    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun startOfDayMillis(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
}