package ir.ghadmino.stepcounter.achievement

import android.content.Context
import ir.ghadmino.stepcounter.step.StepHistory

data class GhadminoMedal(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val requiredAchievements: Int,
    val unlocked: Boolean
)

object MedalRepository {
    fun evaluate(context: Context): List<GhadminoMedal> {
        val achievements = AchievementRepository.evaluate(context)
        val unlocked = achievements.count { it.unlocked }

        return listOf(
            GhadminoMedal(
                "bronze",
                "مدال برنز",
                "حداقل ۳ دستاورد را باز کن.",
                "🥉",
                3,
                unlocked >= 3
            ),
            GhadminoMedal(
                "silver",
                "مدال نقره",
                "حداقل ۵ دستاورد را باز کن.",
                "🥈",
                5,
                unlocked >= 5
            ),
            GhadminoMedal(
                "gold",
                "مدال طلا",
                "حداقل ۸ دستاورد را باز کن.",
                "🥇",
                8,
                unlocked >= 8
            ),
            GhadminoMedal(
                "diamond",
                "مدال الماس",
                "همه دستاوردهای فعلی را باز کن.",
                "💎",
                achievements.size,
                achievements.isNotEmpty() && unlocked >= achievements.size
            )
        )
    }

    fun lifetimeSteps(context: Context): Int =
        StepHistory.totalLifetime(context)
}
