package ir.ghadmino.stepcounter.free

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object WeeklyReportScheduler {
    private const val REQUEST = 7721
    fun schedule(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WeeklyReportReceiver::class.java)
        val pending = PendingIntent.getBroadcast(context, REQUEST, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val now = Calendar.getInstance()
        val next = now.clone() as Calendar
        next.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        next.set(Calendar.HOUR_OF_DAY, 20)
        next.set(Calendar.MINUTE, 0)
        next.set(Calendar.SECOND, 0)
        next.set(Calendar.MILLISECOND, 0)
        if (next.timeInMillis <= now.timeInMillis) next.add(Calendar.WEEK_OF_YEAR, 1)
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, next.timeInMillis, AlarmManager.INTERVAL_DAY * 7, pending)
    }
}
