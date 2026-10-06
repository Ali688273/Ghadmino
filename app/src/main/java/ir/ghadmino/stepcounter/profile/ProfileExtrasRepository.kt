package ir.ghadmino.stepcounter.profile

import android.content.Context
import ir.ghadmino.stepcounter.widget.GhadminoWidgetProvider

data class ProfileExtra(val id: String, val title: String, val cost: Int, val emoji: String)

object ProfileExtrasRepository {
    val items = listOf(
        ProfileExtra("frame_blue", "قاب آبی", 0, "🔵"),
        ProfileExtra("frame_gold", "قاب طلایی", 0, "🟡"),
        ProfileExtra("badge_runner", "نشان دونده", 0, "🏃"),
        ProfileExtra("badge_champion", "نشان قهرمان", 0, "🏆"),
        ProfileExtra("advanced_stats", "آمار پیشرفته", 0, "📊"),
        ProfileExtra("widget_theme", "تم ویجت", 0, "📱")
    )

    fun buyOrSelect(context: Context, id: String, cost: Int): Boolean {
        context.getSharedPreferences("ghadmino_profile_extras", Context.MODE_PRIVATE)
            .edit().putString("selected_extra", id).apply()
        GhadminoWidgetProvider.updateAll(context)
        return true
    }

    fun isUnlocked(context: Context, id: String): Boolean = true

    fun selected(context: Context): String =
        context.getSharedPreferences("ghadmino_profile_extras", Context.MODE_PRIVATE)
            .getString("selected_extra", "") ?: ""
}
