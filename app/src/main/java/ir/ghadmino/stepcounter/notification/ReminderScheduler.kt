package ir.ghadmino.stepcounter.notification
import android.app.*
import android.content.*
import android.os.Build
import androidx.core.app.NotificationCompat
import ir.ghadmino.stepcounter.MainActivity
import ir.ghadmino.stepcounter.R
import ir.ghadmino.stepcounter.settings.SettingsRepository
import ir.ghadmino.stepcounter.step.StepCounterService
import java.util.Calendar
object ReminderScheduler{
 private const val REQUEST=7070
 fun schedule(c:Context){val s=SettingsRepository.load(c);cancel(c);if(!s.remindersEnabled)return;val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager;val pi=PendingIntent.getBroadcast(c,REQUEST,Intent(c,ReminderReceiver::class.java),PendingIntent.FLAG_UPDATE_CURRENT or flag());val t=Calendar.getInstance();t.set(Calendar.HOUR_OF_DAY,s.reminderHour);t.set(Calendar.MINUTE,s.reminderMinute);t.set(Calendar.SECOND,0);t.set(Calendar.MILLISECOND,0);if(t.timeInMillis<=System.currentTimeMillis())t.add(Calendar.DAY_OF_YEAR,1);am.setInexactRepeating(AlarmManager.RTC_WAKEUP,t.timeInMillis,AlarmManager.INTERVAL_DAY,pi)}
 fun cancel(c:Context){val pi=PendingIntent.getBroadcast(c,REQUEST,Intent(c,ReminderReceiver::class.java),PendingIntent.FLAG_UPDATE_CURRENT or flag());(c.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi)}
 private fun flag()=if(Build.VERSION.SDK_INT>=23)PendingIntent.FLAG_IMMUTABLE else 0
}
class ReminderReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent?){val s=SettingsRepository.load(c);if(!s.remindersEnabled)return;val steps=StepCounterService.todaySteps;val goal=c.getSharedPreferences("ghadmino_ui",Context.MODE_PRIVATE).getInt("daily_goal",8000);if(steps>=goal)return;val m=c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;if(Build.VERSION.SDK_INT>=26)m.createNotificationChannel(NotificationChannel("ghadmino_reminders","یادآوری فعالیت",NotificationManager.IMPORTANCE_DEFAULT));val pi=PendingIntent.getActivity(c,7071,Intent(c,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or if(Build.VERSION.SDK_INT>=23)PendingIntent.FLAG_IMMUTABLE else 0);m.notify(7071,NotificationCompat.Builder(c,"ghadmino_reminders").setSmallIcon(R.drawable.ic_launcher).setContentTitle("وقت حرکت است 👟").setContentText("هنوز "+(goal-steps)+" قدم تا هدف امروز باقی مانده.").setContentIntent(pi).setAutoCancel(true).build())}}