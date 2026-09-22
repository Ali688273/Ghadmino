package ir.ghadmino.stepcounter.achievement

import android.content.Context
import ir.ghadmino.stepcounter.reward.CoinWallet
import ir.ghadmino.stepcounter.step.StepHistory

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val reward: Int,
    val unlocked: Boolean
)

object AchievementRepository {
    private val definitions = listOf(
        Achievement("first_1000", "اولین هزار قدم", "در یک روز به ۱۰۰۰ قدم برس.", "🥉", 10, false),
        Achievement("five_k_day", "نیمه‌راه", "در یک روز به ۵۰۰۰ قدم برس.", "🥈", 20, false),
        Achievement("ten_k_day", "ده‌هزاری", "در یک روز به ۱۰۰۰۰ قدم برس.", "🏅", 40, false),
        Achievement("seven_day_streak", "هفته طلایی", "هفت روز پشت‌سرهم حداقل ۳۰۰۰ قدم ثبت کن.", "🔥", 75, false),
        Achievement("lifetime_100k", "صد هزار قدم", "در مجموع ۱۰۰۰۰۰ قدم ثبت کن.", "🏆", 150, false),
        Achievement("lifetime_250k", "یک‌ربع میلیون", "در مجموع ۲۵۰۰۰۰ قدم ثبت کن.", "💎", 300, false)
    )

    fun evaluate(context: Context): List<Achievement> {
        val history = StepHistory.recent(context, 365)
        val lifetime = StepHistory.totalLifetime(context)
        val bestDay = history.maxOfOrNull { it.second } ?: 0
        val streak = StepHistory.currentStreak(context, 3000)

        return definitions.map { item ->
            val reached = when (item.id) {
                "first_1000" -> bestDay >= 1000
                "five_k_day" -> bestDay >= 5000
                "ten_k_day" -> bestDay >= 10000
                "seven_day_streak" -> streak >= 7
                "lifetime_100k" -> lifetime >= 100000
                "lifetime_250k" -> lifetime >= 250000
                else -> false
            }
            if (reached) {
                val key = "achievement_claimed_" + item.id
                val prefs = context.getSharedPreferences("ghadmino_achievements", Context.MODE_PRIVATE)
                if (!prefs.getBoolean(key, false)) {
                    CoinWallet.add(context, item.reward)
                    prefs.edit().putBoolean(key, true).apply()
                }
            }
            item.copy(unlocked = reached)
        }
    }
}
