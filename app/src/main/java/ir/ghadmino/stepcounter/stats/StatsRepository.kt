package ir.ghadmino.stepcounter.stats

import android.content.Context
import ir.ghadmino.stepcounter.activity.ManualActivityRepository
import ir.ghadmino.stepcounter.step.StepCounterService
import ir.ghadmino.stepcounter.step.StepHistory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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
        val data = readData(c, days)
        val allTime = readData(c, 3650)

        val total = data.sumOf { it.second }
        val average = if (data.isEmpty()) 0 else total / data.size
        val best = data.maxByOrNull { it.second } ?: (today() to 0)

        val currentStreak = calculateCurrentStreak(data, goal)
        val bestStreak = calculateLongestStreak(allTime, goal)

        val manualSteps = data.sumOf { ManualActivityRepository.stepsForDate(c, it.first) }
        val manualMinutes = data.sumOf { ManualActivityRepository.minutesForDate(c, it.first) }
        val manualCalories = data.sumOf { ManualActivityRepository.caloriesForDate(c, it.first) }

        return GhadminoStats(
            days = data,
            total = total,
            average = average,
            bestDay = best,
            streak = currentStreak,
            goalDays = data.count { it.second >= goal },
            manualSteps = manualSteps,
            manualMinutes = manualMinutes,
            manualCalories = manualCalories,
            longestStreak = bestStreak
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

    private fun readData(c: Context, days: Int): List<Pair<String, Int>> {
        val safeDays = days.coerceIn(1, 3650)
        val raw = StepHistory.recent(c, safeDays).toMutableList()
        val todayDate = today()
        val liveToday = maxOf(
            StepCounterService.todaySteps,
            StepCounterService.persistedTodaySteps(c)
        )

        // همیشه امروز را در گزارش قرار می‌دهیم، حتی اگر هنوز در StepHistory ذخیره نشده باشد.
        val todayIndex = raw.indexOfFirst { it.first == todayDate }
        if (todayIndex >= 0) {
            raw[todayIndex] = todayDate to maxOf(raw[todayIndex].second, liveToday)
        } else {
            raw.add(0, todayDate to liveToday)
        }

        return raw
            .distinctBy { it.first }
            .sortedByDescending { it.first }
            .map { item ->
                item.first to (
                    item.second +
                        ManualActivityRepository.stepsForDate(c, item.first)
                    )
            }
    }

    private fun calculateCurrentStreak(data: List<Pair<String, Int>>, goal: Int): Int {
        var streak = 0
        var previousDate: String? = null

        for ((date, steps) in data) {
            if (steps < goal) break
            if (previousDate != null && !isPreviousCalendarDay(previousDate!!, date)) break
            streak++
            previousDate = date
        }
        return streak
    }

    private fun calculateLongestStreak(data: List<Pair<String, Int>>, goal: Int): RecordInfo {
        val chronological = data.sortedBy { it.first }

        var bestLength = 0
        var bestStart = today()
        var bestEnd = today()
        var currentLength = 0
        var currentStart = today()
        var previousDate: String? = null

        for ((date, steps) in chronological) {
            val consecutive = previousDate != null && isPreviousCalendarDay(date, previousDate!!)
            if (steps >= goal && (currentLength == 0 || consecutive)) {
                if (currentLength == 0) currentStart = date
                currentLength++
                if (currentLength > bestLength) {
                    bestLength = currentLength
                    bestStart = currentStart
                    bestEnd = date
                }
            } else if (steps >= goal) {
                currentLength = 1
                currentStart = date
                if (bestLength == 0) {
                    bestLength = 1
                    bestStart = date
                    bestEnd = date
                }
            } else {
                currentLength = 0
            }
            previousDate = date
        }

        return RecordInfo(bestLength, bestStart, bestEnd)
    }

    private fun isPreviousCalendarDay(newerDate: String, olderDate: String): Boolean {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val newer = format.parse(newerDate) ?: return false
            val older = format.parse(olderDate) ?: return false
            val expected = Calendar.getInstance().apply {
                time = newer
                add(Calendar.DAY_OF_YEAR, -1)
            }.time
            format.format(expected) == format.format(older)
        } catch (_: Exception) {
            false
        }
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
