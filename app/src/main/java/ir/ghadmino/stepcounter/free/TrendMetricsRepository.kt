package ir.ghadmino.stepcounter.free

import android.content.Context
import ir.ghadmino.stepcounter.profile.ProfileRepository
import ir.ghadmino.stepcounter.step.StepHistory
import kotlin.math.max

data class TrendMetrics(
    val days: Int,
    val totalSteps: Int,
    val averageSteps: Int,
    val bestDay: Int,
    val previousTotal: Int,
    val changePercent: Int,
    val distanceKm: Double,
    val calories: Int
)

object TrendMetricsRepository {
    fun calculate(context: Context, days: Int): TrendMetrics {
        val safeDays = days.coerceIn(1, 365)
        val current = StepHistory.recent(context, safeDays)
        val previous = StepHistory.recent(context, safeDays * 2).drop(safeDays)
        val total = current.sumOf { it.second }
        val prev = previous.sumOf { it.second }
        val profile = ProfileRepository.load(context)
        val distance = total * profile.strideCm / 100000.0
        val calories = (total * profile.strideCm * profile.weightKg * 0.5 / 100000.0).toInt()
        val change = if (prev <= 0) 0 else (((total - prev).toDouble() / prev) * 100.0).toInt()
        return TrendMetrics(
            days = safeDays,
            totalSteps = total,
            averageSteps = if (current.isEmpty()) 0 else total / current.size,
            bestDay = current.maxOfOrNull { it.second } ?: 0,
            previousTotal = prev,
            changePercent = change,
            distanceKm = distance,
            calories = calories
        )
    }

    fun maxDistanceKm(context: Context, days: Int = 3650): Double {
        val profile = ProfileRepository.load(context)
        val maxSteps = StepHistory.recent(context, days.coerceAtMost(3650)).maxOfOrNull { it.second } ?: 0
        return maxSteps * profile.strideCm / 100000.0
    }
}

object ActivityEstimateRepository {
    fun estimate(context: Context, days: Int = 30): Int {
        val rows = StepHistory.recent(context, days)
        return rows.count { it.second >= 3000 }
    }
}
