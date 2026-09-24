package ir.ghadmino.stepcounter.stats

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object SpeedHistoryRepository {
    private const val PREFS = "ghadmino_speed_history"
    private const val AVG_PREFIX = "avg_"
    private const val MAX_PREFIX = "max_"

    fun save(context: Context, date: String, average: Float, maximum: Float) {
        if (average <= 0f) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putFloat(AVG_PREFIX + date, average)
            .putFloat(MAX_PREFIX + date, maximum)
            .apply()
    }

    fun recent(context: Context, days: Int): List<Pair<String, Float>> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val result = mutableListOf<Pair<String, Float>>()
        val calendar = Calendar.getInstance()
        repeat(days.coerceIn(1, 30)) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
            val value = p.getFloat(AVG_PREFIX + date, 0f)
            if (value > 0f) result.add(date to value)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return result.asReversed()
    }

    fun bestAverage(context: Context, days: Int = 3650): Pair<String, Float>? =
        recent(context, days).maxByOrNull { it.second }

    fun bestMaximum(context: Context, days: Int = 3650): Pair<String, Float>? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var bestDate: String? = null
        var bestValue = 0f
        val calendar = Calendar.getInstance()
        repeat(days.coerceIn(1, 3650)) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
            val value = prefs.getFloat(MAX_PREFIX + date, 0f)
            if (value > bestValue) {
                bestValue = value
                bestDate = date
            }
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return bestDate?.let { it to bestValue }
    }

    fun recent(days: Int): List<Pair<String, Float>> {
        return emptyList()
    }

    fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
