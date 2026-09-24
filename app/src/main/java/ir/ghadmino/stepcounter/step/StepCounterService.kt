package ir.ghadmino.stepcounter.step

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import ir.ghadmino.stepcounter.MainActivity
import ir.ghadmino.stepcounter.R
import ir.ghadmino.stepcounter.reward.CoinWallet
import ir.ghadmino.stepcounter.analytics.ActivityAnalyticsRepository
import ir.ghadmino.stepcounter.insights.ActivityInsightsRepository
import ir.ghadmino.stepcounter.widget.GhadminoWidgetProvider
import ir.ghadmino.stepcounter.free.DiagnosticsHistoryRepository
import ir.ghadmino.stepcounter.free.StepSource
import ir.ghadmino.stepcounter.free.StepSourceRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StepCounterService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private val prefs by lazy { getSharedPreferences("ghadmino_steps", Context.MODE_PRIVATE) }

    companion object {
        @Volatile var todaySteps = 0

        fun persistedTodaySteps(context: Context): Int {
            val prefs = context.getSharedPreferences("ghadmino_steps", Context.MODE_PRIVATE)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            if (prefs.getString(KEY_DATE, null) != today) return 0

            val baseline = prefs.getLong(KEY_BASELINE, -1L)
            val last = prefs.getLong(KEY_LAST_TOTAL, -1L)
            val accumulated = prefs.getInt(KEY_ACCUMULATED, 0).coerceAtLeast(0)

            if (baseline < 0L || last < baseline) {
                return accumulated.coerceAtMost(Int.MAX_VALUE)
            }

            return (accumulated.toLong() + (last - baseline).coerceAtLeast(0L))
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
        }

        @Volatile var sensorAvailable = false
        private const val CHANNEL_ID = "ghadmino_steps"
        private const val NOTIFICATION_ID = 1001
        private const val KEY_DATE = "date"
        private const val KEY_BASELINE = "baseline"
        private const val KEY_LAST_TOTAL = "last_total"
        private const val KEY_ACCUMULATED = "accumulated_steps"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        sensorAvailable = stepSensor != null
        if (sensorAvailable) DiagnosticsHistoryRepository.markSensor(this)
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        loadToday()
        todaySteps = maxOf(todaySteps, persistedTodaySteps(this))
        if (StepSourceRepository.get(this) != StepSource.HEALTH_CONNECT) StepHistory.saveToday(this, todaySteps)
        DiagnosticsHistoryRepository.markSave(this)
        sendBroadcast(Intent(GhadminoWidgetProvider.ACTION_REFRESH).setPackage(packageName))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val total = event.values.firstOrNull()?.toLong()?.coerceAtLeast(0) ?: return
        val today = currentDate()
        val savedDate = prefs.getString(KEY_DATE, null)
        var baseline = prefs.getLong(KEY_BASELINE, -1L)
        val last = prefs.getLong(KEY_LAST_TOTAL, -1L)
        var accumulated = prefs.getInt(KEY_ACCUMULATED, 0).coerceAtLeast(0)

        if (last >= 0 && total < last) {
            accumulated = maxOf(todaySteps, accumulated).coerceAtLeast(0)
            baseline = total
            prefs.edit()
                .putLong(KEY_BASELINE, baseline)
                .putInt(KEY_ACCUMULATED, accumulated)
                .putLong(KEY_LAST_TOTAL, total)
                .apply()
        }

        if (savedDate != null && savedDate != today && baseline >= 0 && last >= baseline) {
            StepHistory.saveDate(this, savedDate, daySteps(accumulated, baseline, last))
        }

        if (savedDate != today || baseline < 0) {
            baseline = total
            accumulated = 0
            todaySteps = 0
            prefs.edit()
                .putString(KEY_DATE, today)
                .putLong(KEY_BASELINE, baseline)
                .putLong(KEY_LAST_TOTAL, total)
                .putInt(KEY_ACCUMULATED, 0)
                .apply()
        }

        val sensorSteps = (total - baseline)
            .coerceAtLeast(0)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
        val newTodaySteps = (accumulated.toLong() + sensorSteps)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()

        val previousTodaySteps = todaySteps
        val delta = (newTodaySteps - previousTodaySteps).coerceAtLeast(0)
        todaySteps = newTodaySteps

        if (delta > 0) {
            ActivityAnalyticsRepository.markActivity(this)
            ActivityInsightsRepository.recordStepChange(this, delta)
        }

        prefs.edit()
            .putLong(KEY_LAST_TOTAL, total)
            .putInt(KEY_ACCUMULATED, accumulated)
            .apply()

        if (StepSourceRepository.get(this) != StepSource.HEALTH_CONNECT) {
            CoinWallet.syncStepReward(this, todaySteps)
            StepHistory.saveToday(this, todaySteps)
        }
        DiagnosticsHistoryRepository.markSave(this)
        sendBroadcast(Intent(GhadminoWidgetProvider.ACTION_REFRESH).setPackage(packageName))
        updateNotification()
    }

    private fun loadToday() {
        val today = currentDate()
        val savedDate = prefs.getString(KEY_DATE, null)

        if (savedDate != today) {
            if (savedDate != null) {
                val baseline = prefs.getLong(KEY_BASELINE, -1L)
                val last = prefs.getLong(KEY_LAST_TOTAL, -1L)
                if (baseline >= 0 && last >= baseline) {
                    StepHistory.saveDate(
                        this,
                        savedDate,
                        daySteps(
                            prefs.getInt(KEY_ACCUMULATED, 0).coerceAtLeast(0),
                            baseline,
                            last
                        )
                    )
                }
            }

            todaySteps = 0
            prefs.edit()
                .putString(KEY_DATE, today)
                .remove(KEY_BASELINE)
                .remove(KEY_LAST_TOTAL)
                .putInt(KEY_ACCUMULATED, 0)
                .apply()
            return
        }

        val baseline = prefs.getLong(KEY_BASELINE, -1L)
        val last = prefs.getLong(KEY_LAST_TOTAL, -1L)
        val accumulated = prefs.getInt(KEY_ACCUMULATED, 0).coerceAtLeast(0)

        todaySteps =
            if (baseline >= 0 && last >= baseline) {
                (accumulated.toLong() + (last - baseline))
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt()
            } else {
                accumulated
            }
    }

    private fun daySteps(accumulated: Int, baseline: Long, last: Long): Int {
        return (accumulated.toLong() + (last - baseline).coerceAtLeast(0L))
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    private fun currentDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("قدم‌شمار قدمینو")
            .setContentText("$todaySteps قدم امروز")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "شمارش قدم",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "نمایش وضعیت قدم‌شمار قدمینو"
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}