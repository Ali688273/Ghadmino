package ir.ghadmino.stepcounter.settings

import android.content.Context

data class GhadminoSettings(
    val remindersEnabled:Boolean=true,
    val reminderHour:Int=20,
    val reminderMinute:Int=0,
    val inactivityEnabled:Boolean=true,
    val inactivityMinutes:Int=120
)

object SettingsRepository{
 private const val PREFS="ghadmino_settings"
 fun load(c:Context):GhadminoSettings{
  val p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
  return GhadminoSettings(
   p.getBoolean("reminders_enabled",true),
   p.getInt("reminder_hour",20),
   p.getInt("reminder_minute",0),
   p.getBoolean("inactivity_enabled",true),
   p.getInt("inactivity_minutes",120)
  )
 }
 fun save(c:Context,s:GhadminoSettings){
  c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
   .putBoolean("reminders_enabled",s.remindersEnabled)
   .putInt("reminder_hour",s.reminderHour.coerceIn(0,23))
   .putInt("reminder_minute",s.reminderMinute.coerceIn(0,59))
   .putBoolean("inactivity_enabled",s.inactivityEnabled)
   .putInt("inactivity_minutes",s.inactivityMinutes.coerceIn(60,360))
   .apply()
 }
}