package ir.ghadmino.stepcounter.reward

import android.content.Context
import ir.ghadmino.stepcounter.step.StepHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CoinWallet {
    private const val PREFS = "ghadmino_coins"
    private fun p(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun balance(c: Context) = p(c).getInt("balance", 0)

    fun add(c: Context, amount: Int) {
        if (amount > 0) p(c).edit().putInt("balance", balance(c) + amount).apply()
    }

    fun spend(c: Context, amount: Int): Boolean {
        if (amount <= 0 || balance(c) < amount) return false
        p(c).edit().putInt("balance", balance(c) - amount).apply()
        return true
    }

    fun syncStepReward(c: Context, steps: Int) {
        val prefs = p(c)
        val date = today()
        val blocks = steps.coerceAtLeast(0) / 1000
        val key = "rewarded_blocks_" + date
        var old = prefs.getInt(key, -1)

        if (old < 0) {
            val legacy = prefs.getInt("rewarded_blocks", -1)
            old = if (legacy in 0..blocks) legacy else 0
        }

        if (blocks > old) {
            add(c, (blocks - old) * 5)
            prefs.edit().putInt(key, blocks).putInt("rewarded_blocks", blocks).apply()
        } else if (!prefs.contains(key)) {
            prefs.edit().putInt(key, blocks).apply()
        }

        val lifetime = StepHistory.totalLifetime(c)
        prefs.edit().putInt("lifetime_steps", lifetime).apply()
        checkMilestones(c, lifetime)
    }

    private fun checkMilestones(c: Context, steps: Int) {
        val milestones = listOf(10000 to 25, 25000 to 50, 50000 to 100, 100000 to 250, 250000 to 500)
        milestones.forEach { item ->
            val target = item.first
            val reward = item.second
            val key = "milestone_" + target
            if (steps >= target && !p(c).getBoolean(key, false)) {
                add(c, reward)
                p(c).edit().putBoolean(key, true).apply()
            }
        }
    }

    fun claimDailyMission(c: Context, missionId: String, target: Int, reward: Int, steps: Int): Boolean {
        if (steps < target) return false
        val key = "mission_" + missionId + "_date"
        if (p(c).getString(key, null) == today()) return false
        add(c, reward)
        p(c).edit().putString(key, today()).apply()
        return true
    }

    fun claimGoalReward(c: Context): Boolean {
        if (p(c).getString("goal_date", null) == today()) return false
        add(c, 20)
        p(c).edit().putString("goal_date", today()).apply()
        return true
    }

    fun canClaimAdReward(c: Context) = p(c).getString("ad_date", null) != today()

    fun claimAdReward(c: Context, amount: Int = 100): Boolean {
        if (!canClaimAdReward(c)) return false
        add(c, amount)
        p(c).edit().putString("ad_date", today()).apply()
        return true
    }

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
