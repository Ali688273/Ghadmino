package ir.ghadmino.stepcounter.profile

import android.content.Context
import ir.ghadmino.stepcounter.widget.GhadminoWidgetProvider

data class ProfileExtra(val id: String, val title: String, val emoji: String)

object ProfileExtrasRepository {
    private const val PREFS = "ghadmino_profile_extras"
    private const val FRAME = "selected_frame"
    private const val BADGE = "selected_badge"
    private const val WIDGET = "selected_widget_theme"
    private const val LEGACY = "selected_extra"

    val frames = listOf(
        ProfileExtra("none", "بدون قاب", "◻️"),
        ProfileExtra("frame_blue", "قاب آبی", "🔷"),
        ProfileExtra("frame_gold", "قاب طلایی", "🟡"),
        ProfileExtra("frame_violet", "قاب بنفش", "🟣"),
        ProfileExtra("frame_green", "قاب سبز", "🟢")
    )
    val badges = listOf(
        ProfileExtra("none", "بدون نشان", "—"),
        ProfileExtra("badge_runner", "دونده", "🏃"),
        ProfileExtra("badge_champion", "قهرمان", "🏆"),
        ProfileExtra("badge_streak", "پشتکار", "🔥"),
        ProfileExtra("badge_explorer", "کاوشگر", "🧭")
    )
    val widgetThemes = listOf(
        ProfileExtra("green", "سبز کلاسیک", "🌿"),
        ProfileExtra("ocean", "آبی اقیانوسی", "🌊"),
        ProfileExtra("violet", "بنفش", "💜"),
        ProfileExtra("sunset", "غروب", "🌅"),
        ProfileExtra("midnight", "شب", "🌙")
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun migrate(context: Context) {
        val p = prefs(context)
        if (!p.contains(FRAME) && !p.contains(BADGE) && !p.contains(WIDGET)) {
            when (p.getString(LEGACY, "")) {
                "frame_blue", "frame_gold" -> p.edit().putString(FRAME, p.getString(LEGACY, "none")).apply()
                "badge_runner", "badge_champion" -> p.edit().putString(BADGE, p.getString(LEGACY, "none")).apply()
                "widget_theme" -> p.edit().putString(WIDGET, "violet").apply()
            }
        }
    }

    fun selectedFrame(context: Context): String {
        migrate(context)
        return prefs(context).getString(FRAME, "none") ?: "none"
    }

    fun selectedBadge(context: Context): String {
        migrate(context)
        return prefs(context).getString(BADGE, "none") ?: "none"
    }

    fun selectedWidgetTheme(context: Context): String {
        migrate(context)
        return prefs(context).getString(WIDGET, "green") ?: "green"
    }

    fun selectFrame(context: Context, id: String) {
        if (frames.none { it.id == id }) return
        prefs(context).edit().putString(FRAME, id).apply()
    }

    fun selectBadge(context: Context, id: String) {
        if (badges.none { it.id == id }) return
        prefs(context).edit().putString(BADGE, id).apply()
    }

    fun selectWidgetTheme(context: Context, id: String) {
        if (widgetThemes.none { it.id == id }) return
        prefs(context).edit().putString(WIDGET, id).apply()
        GhadminoWidgetProvider.updateAll(context)
    }

    fun isUnlocked(context: Context, id: String): Boolean = true

    // Kept for compatibility with older profile screens.
    fun selected(context: Context): String = selectedBadge(context)
}
