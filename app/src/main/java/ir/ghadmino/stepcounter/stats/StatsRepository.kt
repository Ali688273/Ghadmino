package ir.ghadmino.stepcounter.stats

import android.content.Context
import ir.ghadmino.stepcounter.step.StepHistory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class GhadminoStats(
    val days: List<Pair<String, Int>>,
    val total: Int,
    val average: Int,
    val bestDay: Pair<String, Int>,
    val streak: Int,
    val goalDays: Int
)

object StatsRepository {
    fun load(c: Context, goal: Int, days: Int = 30): GhadminoStats {
        val data = StepHistory.recent(c, days)
        val total = data.sumOf { it.second }
        val average = if (data.isEmpty()) 0 else total / data.size
        val best = data.maxByOrNull { it.second } ?: (today() to 0)
        var streak = 0
        for (item in data) {
            if (item.second >= goal) streak++ else break
        }
        return GhadminoStats(data, total, average, best, streak, data.count { it.second >= goal })
    }

    fun label(date: String): String = try {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: return date
        SimpleDateFormat("MM/dd", Locale.US).format(d)
    } catch (_: Exception) { date }

    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
}
