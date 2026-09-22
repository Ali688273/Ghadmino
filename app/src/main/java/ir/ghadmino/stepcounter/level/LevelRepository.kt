package ir.ghadmino.stepcounter.level

import android.content.Context
import ir.ghadmino.stepcounter.achievement.AchievementRepository
import ir.ghadmino.stepcounter.step.StepHistory

data class LevelInfo(
    val level: Int,
    val xp: Int,
    val currentLevelXp: Int,
    val nextLevelXp: Int,
    val progress: Float
)

object LevelRepository {
    fun get(context: Context): LevelInfo {
        val steps = StepHistory.totalLifetime(context).coerceAtLeast(0)
        val missions = context.getSharedPreferences("ghadmino_level", Context.MODE_PRIVATE)
            .getInt("missions_completed", 0)
        val achievements = AchievementRepository.evaluate(context).count { it.unlocked }
        val xp = steps / 100 + missions * 50 + achievements * 100
        val level = (xp / 500 + 1).coerceAtLeast(1)
        val current = (level - 1) * 500
        val next = level * 500
        val progress = ((xp - current).toFloat() / 500f).coerceIn(0f, 1f)
        return LevelInfo(level, xp, current, next, progress)
    }

    fun recordMission(context: Context) {
        val p = context.getSharedPreferences("ghadmino_level", Context.MODE_PRIVATE)
        val current = p.getInt("missions_completed", 0)
        p.edit().putInt("missions_completed", current + 1).apply()
    }
}
