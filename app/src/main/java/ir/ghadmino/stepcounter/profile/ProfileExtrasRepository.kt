package ir.ghadmino.stepcounter.profile

import android.content.Context
import ir.ghadmino.stepcounter.reward.CoinWallet

data class ProfileExtra(val id: String, val title: String, val cost: Int, val emoji: String)

object ProfileExtrasRepository {
    val items = listOf(
        ProfileExtra("frame_blue", "قاب آبی", 300, "🔵"),
        ProfileExtra("frame_gold", "قاب طلایی", 700, "🟡"),
        ProfileExtra("badge_runner", "نشان دونده", 200, "🏃"),
        ProfileExtra("badge_champion", "نشان قهرمان", 600, "🏆"),
        ProfileExtra("advanced_stats", "آمار پیشرفته", 1500, "📊"),
        ProfileExtra("widget_theme", "تم ویجت", 1000, "📱")
    )

    fun buyOrSelect(context: Context, id: String, cost: Int): Boolean {
        val key = "extra_" + id
        if (!CoinWallet.isUnlocked(context, key)) {
            if (!CoinWallet.unlock(context, key, cost)) return false
        }
        context.getSharedPreferences("ghadmino_profile_extras", Context.MODE_PRIVATE)
            .edit().putString("selected_extra", id).apply()
        return true
    }

    fun isUnlocked(context: Context, id: String): Boolean =
        CoinWallet.isUnlocked(context, "extra_" + id)

    fun selected(context: Context): String =
        context.getSharedPreferences("ghadmino_profile_extras", Context.MODE_PRIVATE)
            .getString("selected_extra", "") ?: ""
}
