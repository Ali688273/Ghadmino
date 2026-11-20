package ir.ghadmino.stepcounter.achievement

import android.content.Context
import ir.ghadmino.stepcounter.step.StepHistory
import ir.ghadmino.stepcounter.step.StepCounterService

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val unlocked: Boolean
)

object AchievementRepository {
    private val definitions = listOf(
        Achievement("first_1000", "اولین هزار قدم", "در یک روز به ۱۰۰۰ قدم برس.", "🥉", false),
        Achievement("five_k_day", "نیمه‌راه", "در یک روز به ۵۰۰۰ قدم برس.", "🥈", false),
        Achievement("ten_k_day", "ده‌هزاری", "در یک روز به ۱۰۰۰۰ قدم برس.", "🏅", false),
        Achievement("twenty_five_k_day", "ابرپیاده‌رو", "در یک روز به ۲۵۰۰۰ قدم برس.", "🚀", false),
        Achievement("seven_day_streak", "هفته طلایی", "هفت روز پشت‌سرهم حداقل ۳۰۰۰ قدم ثبت کن.", "🔥", false),
        Achievement("thirty_day_streak", "ماه پرتلاش", "۳۰ روز پشت‌سرهم حداقل ۳۰۰۰ قدم ثبت کن.", "📅", false),
        Achievement("lifetime_100k", "صد هزار قدم", "در مجموع ۱۰۰۰۰۰ قدم ثبت کن.", "🏆", false),
        Achievement("lifetime_250k", "یک‌ربع میلیون", "در مجموع ۲۵۰۰۰۰ قدم ثبت کن.", "💎", false),
        Achievement("lifetime_500k", "نیم میلیون", "در مجموع ۵۰۰۰۰۰ قدم ثبت کن.", "👑", false),
        Achievement("lifetime_1m", "یک میلیون قدم", "در مجموع ۱۰۰۰۰۰۰ قدم ثبت کن.", "🌟", false),
        Achievement("fifty_k_day", "ماراتن روزانه", "در یک روز به ۵۰۰۰۰ قدم برس.", "🏃", false),
        Achievement("ninety_day_streak", "سه ماه مداوم", "۹۰ روز پشت‌سرهم حداقل ۳۰۰۰ قدم ثبت کن.", "🔥", false),
        Achievement("lifetime_2m", "دو میلیون قدم", "در مجموع ۲۰۰۰۰۰۰ قدم ثبت کن.", "💠", false),
        Achievement("lifetime_5m", "پنج میلیون قدم", "در مجموع ۵۰۰۰۰۰۰ قدم ثبت کن.", "🏆", false)
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
                "fifty_k_day" -> bestDay >= 50000
                "seven_day_streak" -> streak >= 7
                "thirty_day_streak" -> streak >= 30
                "ninety_day_streak" -> streak >= 90
                "lifetime_100k" -> lifetime >= 100000
                "lifetime_250k" -> lifetime >= 250000
                "lifetime_500k" -> lifetime >= 500000
                "lifetime_1m" -> lifetime >= 1000000
                "lifetime_2m" -> lifetime >= 2000000
                "lifetime_5m" -> lifetime >= 5000000
                else -> false
            }

            item.copy(unlocked = reached)
        }
    }
}