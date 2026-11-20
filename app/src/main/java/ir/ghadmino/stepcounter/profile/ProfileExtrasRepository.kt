package ir.ghadmino.stepcounter.profile

import android.content.Context
import ir.ghadmino.stepcounter.widget.GhadminoWidgetProvider

data class ProfileExtra(val id: String, val title: String, val emoji: String)

object ProfileExtrasRepository {
    val items = listOf(
        ProfileExtra("frame_blue", "قاب آبی", "🔵"),
        ProfileExtra("frame_gold", "قاب طلایی", "🟡"),
        ProfileExtra("badge_runner", "نشان دونده", "🏃"),
        ProfileExtra("badge_champion", "نشان قهرمان", "🏆"),
        ProfileExtra("advanced_stats", "آمار پیشرفته", "📊"),
        ProfileExtra("widget_theme", "تم ویجت", "📱")
    )

    fun select(context: Context, id: String) {
        context.getSharedPreferences("ghadmino_profile_extras", Context.MODE_PRIVATE)
            .edit().putString("selected_extra", id).apply()
        GhadminoWidgetProvider.updateAll(context)
    }

    fun isUnlocked(context: Context, id: String): Boolean = true

    fun selected(context: Context): String =
        context.getSharedPreferences("ghadmino_profile_extras", Context.MODE_PRIVATE)
            .getString("selected_extra", "") ?: ""
}
