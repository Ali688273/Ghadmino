package ir.ghadmino.stepcounter.notification

import android.app.AlarmManager
import android.app.BroadcastReceiver
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import ir.ghadmino.stepcounter.MainActivity
import ir.ghadmino.stepcounter.R
import ir.ghadmino.stepcounter.profile.ProfileRepository
import ir.ghadmino.stepcounter.settings.SettingsRepository
import ir.ghadmino.stepcounter.step.StepCounterService
import java.util.Calendar

object ReminderScheduler {
    private const val REQUEST = 7070

    fun schedule(c: Context) {
        val settings = SettingsRepository.load(c)
        cancel(c)
        if (!settings.remindersEnabled) return

        val alarmManager = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            c,
            REQUEST,
            Intent(c, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or flag()
        )

        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, settings.reminderHour)
            set(Calendar.MINUTE, settings.reminderMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            trigger.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancel(c: Context) {
        val pendingIntent = PendingIntent.getBroadcast(
            c,
            REQUEST,
            Intent(c, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or flag()
        )
        (c.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pendingIntent)
    }

    private fun flag(): Int =
        if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent?) {
        val settings = SettingsRepository.load(c)
        if (!settings.remindersEnabled) return

        val steps = maxOf(
            StepCounterService.todaySteps,
            StepCounterService.persistedTodaySteps(c)
        )
        val goal = ProfileRepository.load(c).dailyGoal
        if (steps >= goal) return

        val manager = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(
                    "ghadmino_reminders",
                    "یادآوری فعالیت",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        val pendingIntent = PendingIntent.getActivity(
            c,
            7071,
            Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )

        manager.notify(
            7071,
            NotificationCompat.Builder(c, "ghadmino_reminders")
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("وقت حرکت است 👟")
                .setContentText("هنوز " + (goal - steps) + " قدم تا هدف امروز باقی مانده.")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        )
    }
}
