package ir.ghadmino.stepcounter.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import ir.ghadmino.stepcounter.MainActivity
import ir.ghadmino.stepcounter.R
import ir.ghadmino.stepcounter.analytics.ActivityAnalyticsRepository
import ir.ghadmino.stepcounter.profile.ProfileRepository
import ir.ghadmino.stepcounter.settings.SettingsRepository
import java.util.Calendar

object InactivityScheduler {
 private const val REQUEST=8181
 fun schedule(c:Context){
  cancel(c)
  val s=SettingsRepository.load(c)
  if(!s.inactivityEnabled)return
  val pi=pending(c)
  val alarm=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
  val trigger=Calendar.getInstance().apply{add(Calendar.HOUR_OF_DAY,1);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)}
  alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP,trigger.timeInMillis,AlarmManager.INTERVAL_HOUR,pi)
 }
 fun cancel(c:Context){(c.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending(c))}
 private fun pending(c:Context)=PendingIntent.getBroadcast(c,REQUEST,Intent(c,InactivityReceiver::class.java),PendingIntent.FLAG_UPDATE_CURRENT or if(Build.VERSION.SDK_INT>=23)PendingIntent.FLAG_IMMUTABLE else 0)
}

class InactivityReceiver:BroadcastReceiver(){
 override fun onReceive(c:Context,i:Intent?){
  val s=SettingsRepository.load(c);if(!s.inactivityEnabled)return
  val r=ActivityAnalyticsRepository.report(c);if(r.today>=r.goal)return
  val last=ActivityAnalyticsRepository.lastActivity(c);if(last<=0L)return
  if(System.currentTimeMillis()-last < s.inactivityMinutes*60_000L)return
  val p=c.getSharedPreferences("ghadmino_inactivity_notice",0);val today=java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).format(java.util.Date())
  if(p.getString("date",null)==today)return
  val manager=c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(NotificationChannel("ghadmino_inactivity","یادآوری کم‌تحرکی",NotificationManager.IMPORTANCE_DEFAULT))
  val pi=PendingIntent.getActivity(c,8182,Intent(c,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or if(Build.VERSION.SDK_INT>=23)PendingIntent.FLAG_IMMUTABLE else 0)
  manager.notify(8182,NotificationCompat.Builder(c,"ghadmino_inactivity").setSmallIcon(R.drawable.ic_launcher).setContentTitle("وقت کمی حرکت است").setContentText("هنوز به هدف امروز نرسیده‌ای؛ چند دقیقه پیاده‌روی می‌تواند کمک کند.").setContentIntent(pi).setAutoCancel(true).build())
  p.edit().putString("date",today).apply()
 }
}
