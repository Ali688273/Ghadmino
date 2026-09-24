package ir.ghadmino.stepcounter.achievement

import android.content.Context
import ir.ghadmino.stepcounter.step.StepHistory
import ir.ghadmino.stepcounter.step.StepCounterService

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
        Achievement("twenty_five_k_day", "ابرپیاده‌رو", "در یک روز به ۲۵۰۰۰ قدم برس.", "🚀", 100, false),
        Achievement("seven_day_streak", "هفته طلایی", "هفت روز پشت‌سرهم حداقل ۳۰۰۰ قدم ثبت کن.", "🔥", 75, false),
        Achievement("thirty_day_streak", "ماه پرتلاش", "۳۰ روز پشت‌سرهم حداقل ۳۰۰۰ قدم ثبت کن.", "📅", 250, false),
        Achievement("lifetime_100k", "صد هزار قدم", "در مجموع ۱۰۰۰۰۰ قدم ثبت کن.", "🏆", 150, false),
        Achievement("lifetime_250k", "یک‌ربع میلیون", "در مجموع ۲۵۰۰۰۰ قدم ثبت کن.", "💎", 300, false),
        Achievement("lifetime_500k", "نیم میلیون", "در مجموع ۵۰۰۰۰۰ قدم ثبت کن.", "👑", 600, false),
        Achievement("lifetime_1m", "یک میلیون قدم", "در مجموع ۱۰۰۰۰۰۰ قدم ثبت کن.", "🌟", 1200, false)
    )

    fun evaluate(context: Context): List<Achievement> {
        val history = StepHistory.recent(context, 365)
        val lifetime = StepHistory.totalLifetime(context)
        val bestDay = maxOf(
            history.maxOfOrNull { it.second } ?: 0,
            StepCounterService.todaySteps,
            StepCounterService.persistedTodaySteps(context)
        )
        val streak = StepHistory.currentStreak(context, 3000)

        return definitions.map { item ->
            val reached = when (item.id) {
                "first_1000" -> bestDay >= 1000
                "five_k_day" -> bestDay >= 5000
                "ten_k_day" -> bestDay >= 10000
                "twenty_five_k_day" -> bestDay >= 25000
                "seven_day_streak" -> streak >= 7
                "thirty_day_streak" -> streak >= 30
                "lifetime_100k" -> lifetime >= 100000
                "lifetime_250k" -> lifetime >= 250000
                "lifetime_500k" -> lifetime >= 500000
                "lifetime_1m" -> lifetime >= 1000000
                else -> false
            }

            if (reached) {
                val prefs = context.getSharedPreferences("ghadmino_achievements", Context.MODE_PRIVATE)
                val key = "achievement_claimed_" + item.id
                if (!prefs.getBoolean(key, false)) {
                    val marked = prefs.edit().putBoolean(key, true).commit()
                    if (marked) {
                        val coins = context.getSharedPreferences("ghadmino_coins", Context.MODE_PRIVATE)
                        val current = coins.getInt("balance", 0).toLong()
                        val next = (current + item.reward).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                        coins.edit().putInt("balance", next).apply()
                    }
                }
            }

            item.copy(unlocked = reached)
        }
    }
}