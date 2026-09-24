package ir.ghadmino.stepcounter.free

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import ir.ghadmino.stepcounter.R
import ir.ghadmino.stepcounter.step.StepHistory

class WeeklyReportReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val channelId = "ghadmino_weekly_report"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(channelId, "گزارش هفتگی", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val total = StepHistory.recent(context, 7).sumOf { row -> row.second }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("گزارش هفتگی قدمینو")
            .setContentText("۷ روز اخیر: " + total + " قدم")
            .setAutoCancel(true)
            .build()
        try { manager.notify(7721, notification) } catch (_: Exception) {}
    }
}
