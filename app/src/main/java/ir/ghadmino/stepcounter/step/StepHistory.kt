package ir.ghadmino.stepcounter.step

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object StepHistory {
    private const val PREFS = "ghadmino_history"
    private const val PREFIX = "steps_"
    private const val LIFETIME = "lifetime_total"

    private fun key(date: String) = PREFIX + date

    fun saveToday(context: Context, steps: Int) = saveDate(context, today(), steps)

    fun saveDate(context: Context, date: String, steps: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val safeSteps = steps.coerceAtLeast(0)
        val oldSteps = prefs.getInt(key(date), 0)
        val oldLifetime = prefs.getLong(LIFETIME, -1L)
        val baseLifetime = if (oldLifetime >= 0L) oldLifetime else calculateLifetime(prefs)
        val newLifetime = (baseLifetime + safeSteps - oldSteps).coerceAtLeast(0L)
        prefs.edit()
            .putInt(key(date), safeSteps)
            .putLong(LIFETIME, newLifetime)
            .apply()
    }

    fun get(context: Context, date: String): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(key(date), 0)

    fun recent(context: Context, days: Int = 7): List<Pair<String, Int>> {
        val result = mutableListOf<Pair<String, Int>>()
        for (i in 0 until days) {
            val d = dateOffset(i)
            result.add(d to get(context, d))
        }
        return result
    }

    fun totalLifetime(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getLong(LIFETIME, -1L)
        if (stored >= 0L) return stored.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val total = calculateLifetime(prefs)
        prefs.edit().putLong(LIFETIME, total).apply()
        return total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    fun currentStreak(context: Context, minimumSteps: Int): Int {
        var streak = 0
        for (i in 0 until 3650) {
            if (get(context, dateOffset(i)) >= minimumSteps) streak++ else break
        }
        return streak
    }

    private fun calculateLifetime(prefs: android.content.SharedPreferences, days: Int = 3650): Long {
        var total = 0L
        for (i in 0 until days) {
            total += prefs.getInt(key(dateOffset(i)), 0).coerceAtLeast(0)
        }
        return total
    }

    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun dateOffset(daysAgo: Int): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
    }
}
