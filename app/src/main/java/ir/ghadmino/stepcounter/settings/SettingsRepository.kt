package ir.ghadmino.stepcounter.settings

import android.content.Context

data class GhadminoSettings(
    val remindersEnabled: Boolean = true,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val inactivityEnabled: Boolean = true,
    val inactivityMinutes: Int = 120,
    val useSolarCalendar: Boolean = true
)

object SettingsRepository {
    private const val PREFS = "ghadmino_settings"

    fun load(c: Context): GhadminoSettings {
        val p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return GhadminoSettings(
            remindersEnabled = p.getBoolean("reminders_enabled", true),
            reminderHour = p.getInt("reminder_hour", 20),
            reminderMinute = p.getInt("reminder_minute", 0),
            inactivityEnabled = p.getBoolean("inactivity_enabled", true),
            inactivityMinutes = p.getInt("inactivity_minutes", 120),
            useSolarCalendar = p.getBoolean("use_solar_calendar", true)
        )
    }

    fun save(c: Context, s: GhadminoSettings) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("reminders_enabled", s.remindersEnabled)
            .putInt("reminder_hour", s.reminderHour.coerceIn(0, 23))
            .putInt("reminder_minute", s.reminderMinute.coerceIn(0, 59))
            .putBoolean("inactivity_enabled", s.inactivityEnabled)
            .putInt("inactivity_minutes", s.inactivityMinutes.coerceIn(60, 360))
            .putBoolean("use_solar_calendar", s.useSolarCalendar)
            .apply()
    }
}