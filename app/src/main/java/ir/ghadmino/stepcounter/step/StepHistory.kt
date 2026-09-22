package ir.ghadmino.stepcounter.step

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object StepHistory {
    private const val PREFS = "ghadmino_history"
    private const val PREFIX = "steps_"

    private fun key(date: String) = PREFIX + date

    fun saveToday(context: Context, steps: Int) = saveDate(context, today(), steps)

    fun saveDate(context: Context, date: String, steps: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(key(date), steps.coerceAtLeast(0)).apply()
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

    fun totalLifetime(context: Context, days: Int = 3650): Int {
        var total = 0L
        for (i in 0 until days) {
            total += get(context, dateOffset(i))
            if (total >= Int.MAX_VALUE) return Int.MAX_VALUE
        }
        return total.toInt()
    }

    fun currentStreak(context: Context, minimumSteps: Int): Int {
        var streak = 0
        for (i in 0 until 3650) {
            if (get(context, dateOffset(i)) >= minimumSteps) streak++ else break
        }
        return streak
    }

    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun dateOffset(daysAgo: Int): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
    }
}
