package ir.ghadmino.stepcounter.stats

import android.content.Context
import ir.ghadmino.stepcounter.activity.ManualActivityRepository
import ir.ghadmino.stepcounter.step.StepCounterService
import ir.ghadmino.stepcounter.step.StepHistory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class PeriodReport(
    val days: Int,
    val totalSteps: Int,
    val averageSteps: Int,
    val bestDay: Pair<String, Int>,
    val goalDays: Int,
    val goalRate: Int
)

data class RecordInfo(
    val value: Int,
    val startDate: String,
    val endDate: String
)

data class GhadminoStats(
    val days: List<Pair<String, Int>>,
    val total: Int,
    val average: Int,
    val bestDay: Pair<String, Int>,
    val streak: Int,
    val goalDays: Int,
    val manualSteps: Int,
    val manualMinutes: Int,
    val manualCalories: Int,
    val longestStreak: RecordInfo
)

object StatsRepository {
    fun load(c: Context, goal: Int, days: Int = 30): GhadminoStats {
        val raw = StepHistory.recent(c, days).toMutableList()

        if (raw.isNotEmpty() && raw.first().first == today()) {
            val liveToday = maxOf(
                raw.first().second,
                StepCounterService.todaySteps,
                StepCounterService.persistedTodaySteps(c)
            )
            raw[0] = today() to liveToday
        }

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

        val chronological = data.asReversed()
        var bestStreak = 0
        var bestStreakStart = today()
        var bestStreakEnd = today()
        var currentStreak = 0
        var currentStart = today()

        chronological.forEach { item ->
            if (item.second >= goal) {
                if (currentStreak == 0) currentStart = item.first
                currentStreak++
                if (currentStreak > bestStreak) {
                    bestStreak = currentStreak
                    bestStreakStart = currentStart
                    bestStreakEnd = item.first
                }
            } else {
                currentStreak = 0
            }
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
            manualCalories,
            RecordInfo(bestStreak, bestStreakStart, bestStreakEnd)
        )
    }

    fun period(c: Context, goal: Int, days: Int): PeriodReport {
        val safeDays = days.coerceIn(1, 3650)
        val data = load(c, goal, safeDays).days
        val total = data.sumOf { it.second }
        val average = if (data.isEmpty()) 0 else total / data.size
        val best = data.maxByOrNull { it.second } ?: (today() to 0)
        val goalDays = data.count { it.second >= goal }
        val rate = if (data.isEmpty()) 0 else goalDays * 100 / data.size
        return PeriodReport(safeDays, total, average, best, goalDays, rate)
    }

    fun weekly(c: Context, goal: Int): PeriodReport = period(c, goal, 7)

    fun monthly(c: Context, goal: Int): PeriodReport = period(c, goal, 30)

    fun trend(c: Context, goal: Int): Int {
        val current = weekly(c, goal).totalSteps
        val previous = load(c, goal, 14).days.drop(7).sumOf { it.second }
        if (previous == 0) return if (current > 0) 100 else 0
        return (((current - previous) * 100f) / previous).toInt()
    }

    fun label(date: String): String {
        return try {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)
                ?: return date
            SimpleDateFormat("MM/dd", Locale.US).format(d)
        } catch (_: Exception) {
            date
        }
    }

    private fun today() =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
}
