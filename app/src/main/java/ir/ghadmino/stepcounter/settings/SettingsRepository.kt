package ir.ghadmino.stepcounter.settings
import android.content.Context
data class GhadminoSettings(val remindersEnabled:Boolean=true,val reminderHour:Int=20,val reminderMinute:Int=0)
object SettingsRepository{
 private const val PREFS="ghadmino_settings"
 fun load(c:Context)=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).let{p->GhadminoSettings(p.getBoolean("reminders_enabled",true),p.getInt("reminder_hour",20),p.getInt("reminder_minute",0))}
 fun save(c:Context,s:GhadminoSettings){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean("reminders_enabled",s.remindersEnabled).putInt("reminder_hour",s.reminderHour.coerceIn(0,23)).putInt("reminder_minute",s.reminderMinute.coerceIn(0,59)).apply()}
}