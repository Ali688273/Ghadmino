package ir.ghadmino.stepcounter.reward

import android.content.Context
import ir.ghadmino.stepcounter.step.StepHistory
import ir.ghadmino.stepcounter.achievement.AchievementRepository
import ir.ghadmino.stepcounter.level.LevelRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CoinWallet {
    private const val PREFS = "ghadmino_coins"
    private fun p(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun balance(c: Context) = p(c).getInt("balance", 0)

    @Synchronized
    fun add(c: Context, amount: Int) {
        if (amount <= 0) return
        val next = balance(c).toLong() + amount.toLong()
        p(c).edit()
            .putInt("balance", next.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            .apply()
    }

    @Synchronized
    fun spend(c: Context, amount: Int): Boolean {
        if (amount <= 0 || balance(c) < amount) return false
        p(c).edit().putInt("balance", balance(c) - amount).apply()
        return true
    }

    @Synchronized
    fun syncStepReward(c: Context, steps: Int) {
        val prefs = p(c)
        val date = today()
        val blocks = steps.coerceAtLeast(0) / 1000
        val key = "rewarded_blocks_" + date
        val old = prefs.getInt(key, 0)

        if (blocks > old) {
            add(c, (blocks - old) * 5)
            prefs.edit().putInt(key, blocks).apply()
        } else if (!prefs.contains(key)) {
            prefs.edit().putInt(key, blocks).apply()
        }

        val lifetime = StepHistory.totalLifetime(c)
        prefs.edit().putInt("lifetime_steps", lifetime).apply()
        checkMilestones(c, lifetime)

        // Achievement evaluation is throttled to meaningful step/date changes
        // so the main refresh loop does not repeatedly scan the full history.
        val achievementCheckKey = "achievement_check_" + date
        val currentCheckKey = date + ":" + blocks
        if (prefs.getString(achievementCheckKey, null) != currentCheckKey) {
            AchievementRepository.evaluate(c)
            prefs.edit().putString(achievementCheckKey, currentCheckKey).apply()
        }
    }

    private fun checkMilestones(c: Context, steps: Int) {
        val milestones = listOf(
            10000 to 25,
            25000 to 50,
            50000 to 100,
            100000 to 250,
            250000 to 500,
            500000 to 750,
            1000000 to 1500
        )

        milestones.forEach { (target, reward) ->
            val key = "milestone_" + target
            if (steps >= target && !p(c).getBoolean(key, false)) {
                add(c, reward)
                p(c).edit().putBoolean(key, true).apply()
            }
        }
    }

    @Synchronized
    fun claimDailyMission(
        c: Context,
        missionId: String,
        target: Int,
        reward: Int,
        steps: Int
    ): Boolean {
        if (steps < target) return false
        val key = "mission_" + missionId + "_date"
        if (p(c).getString(key, null) == today()) return false
        add(c, reward)
        LevelRepository.recordMission(c)
        p(c).edit().putString(key, today()).apply()
        return true
    }

    @Synchronized
    fun claimGoalReward(c: Context): Boolean {
        if (p(c).getString("goal_date", null) == today()) return false
        add(c, 20)
        p(c).edit().putString("goal_date", today()).apply()
        return true
    }

    @Synchronized
    fun unlock(c: Context, id: String, cost: Int): Boolean {
        if (p(c).getBoolean("unlock_" + id, false)) return true
        if (!spend(c, cost)) return false
        p(c).edit().putBoolean("unlock_" + id, true).apply()
        return true
    }

    fun isUnlocked(c: Context, id: String): Boolean =
        p(c).getBoolean("unlock_" + id, false)

    fun setSelectedTheme(c: Context, themeId: String) {
        p(c).edit().putString("selected_theme", themeId).apply()
    }

    fun selectedTheme(c: Context): String =
        p(c).getString("selected_theme", "default") ?: "default"
}
