package ir.ghadmino.stepcounter.challenge

import android.content.Context
import ir.ghadmino.stepcounter.reward.CoinWallet
import ir.ghadmino.stepcounter.step.StepHistory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class Challenge(
    val id: String,
    val title: String,
    val description: String,
    val days: Int,
    val dailyTarget: Int,
    val reward: Int
)

data class ChallengeProgress(
    val completedDays: Int,
    val totalDays: Int,
    val percent: Int,
    val completed: Boolean,
    val claimed: Boolean
)

object ChallengeRepository {
    val challenges = listOf(
        Challenge("seven_day_5k", "چالش ۷ روزه", "۷ روز پیاپی حداقل ۵۰۰۰ قدم", 7, 5000, 150),
        Challenge("seven_day_10k", "چالش ۷ روزه حرفه‌ای", "۷ روز پیاپی حداقل ۱۰۰۰۰ قدم", 7, 10000, 300),
        Challenge("thirty_day_8k", "چالش ۳۰ روزه", "۳۰ روز پیاپی حداقل ۸۰۰۰ قدم", 30, 8000, 1000)
    )

    fun progress(context: Context, challenge: Challenge): Int {
        var count = 0
        for (i in 0 until challenge.days) {
            if (StepHistory.get(context, dateOffset(i)) >= challenge.dailyTarget) {
                count++
            } else {
                break
            }
        }
        return count
    }

    fun details(context: Context, challenge: Challenge): ChallengeProgress {
        val completedDays = progress(context, challenge)
        val total = challenge.days.coerceAtLeast(1)
        return ChallengeProgress(
            completedDays = completedDays,
            totalDays = total,
            percent = ((completedDays * 100f) / total).toInt().coerceIn(0, 100),
            completed = completedDays >= total,
            claimed = isClaimed(context, challenge.id)
        )
    }

    fun isClaimed(context: Context, id: String): Boolean =
        context.getSharedPreferences("ghadmino_challenges", Context.MODE_PRIVATE)
            .getBoolean("claimed_" + id, false)

    fun claim(context: Context, challenge: Challenge): Boolean {
        if (progress(context, challenge) < challenge.days || isClaimed(context, challenge.id)) {
            return false
        }
        CoinWallet.add(context, challenge.reward)
        context.getSharedPreferences("ghadmino_challenges", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("claimed_" + challenge.id, true)
            .apply()
        return true
    }

    private fun dateOffset(daysAgo: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }
}
