package ir.ghadmino.stepcounter.stats

import android.content.Context
import ir.ghadmino.stepcounter.activity.ManualActivityRepository
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
    val goalDays: Int,
    val manualSteps: Int,
    val manualMinutes: Int,
    val manualCalories: Int
)

object StatsRepository {
    fun load(c: Context, goal: Int, days: Int = 30): GhadminoStats {
        val raw = StepHistory.recent(c, days)
        val data = raw.map { item ->
            item.first to (item.second + ManualActivityRepository.stepsForDate(c, item.first))
        }
        val total = data.sumOf { it.second }
        val average = if (data.isEmpty()) 0 else total / data.size
        val best = data.maxByOrNull { it.second } ?: (today() to 0)
        var streak = 0
        for (item in data) {
            if (item.second >= goal) streak++ else break
        }
        val manualSteps = data.sumOf { ManualActivityRepository.stepsForDate(c, it.first) }
        val manualMinutes = data.sumOf { ManualActivityRepository.minutesForDate(c, it.first) }
        val manualCalories = data.sumOf { ManualActivityRepository.caloriesForDate(c, it.first) }
        return GhadminoStats(
            data,
            total,
            average,
            best,
            streak,
            data.count { it.second >= goal },
            manualSteps,
            manualMinutes,
            manualCalories
        )
    }

    fun label(date: String): String = try {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: return date
        SimpleDateFormat("MM/dd", Locale.US).format(d)
    } catch (_: Exception) {
        date
    }

    private fun today() =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
}
