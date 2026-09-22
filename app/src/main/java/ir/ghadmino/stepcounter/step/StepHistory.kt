package ir.ghadmino.stepcounter.step

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StepHistory {

    private const val PREFS = "ghadmino_history"
    private const val PREFIX = "steps_"

    private fun key(date: String) = PREFIX + date

    fun saveToday(context: Context, steps: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(key(today()), steps.coerceAtLeast(0))
            .apply()
    }

    fun get(context: Context, date: String): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(key(date), 0)
    }

    fun recent(context: Context, days: Int = 7): List<Pair<String, Int>> {
        val result = mutableListOf<Pair<String, Int>>()
        for (i in 0 until days) {
            val date = dateOffset(i)
            result.add(date to get(context, date))
        }
        return result
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun dateOffset(daysAgo: Int): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }
}
