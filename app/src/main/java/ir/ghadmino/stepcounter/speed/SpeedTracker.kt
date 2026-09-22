package ir.ghadmino.stepcounter.speed

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class SpeedTracker(private val context: Context) {

    val currentSpeedKmh: Float
        get() = SpeedTrackerState.currentSpeedKmh

    val averageSpeedKmh: Float
        get() = SpeedTrackerState.averageSpeedKmh

    val minimumSpeedKmh: Float
        get() = SpeedTrackerState.minimumSpeedKmh

    val maximumSpeedKmh: Float
        get() = SpeedTrackerState.maximumSpeedKmh

    @SuppressLint("MissingPermission")
    fun start() {
        SpeedTrackerState.start(context.applicationContext)
    }

    fun stop() {
        // عمدی: ردیابی سرعت متعلق به سرویس پس‌زمینه است و با بسته شدن Activity متوقف نمی‌شود.
    }

    fun resetDailyStats() {
        SpeedTrackerState.reset(context.applicationContext)
    }
}

private object SpeedTrackerState {
    private const val PREFS = "ghadmino_speed"
    private const val KEY_DATE = "date"
    private const val KEY_TOTAL = "speed_total"
    private const val KEY_SAMPLES = "speed_samples"
    private const val KEY_MIN = "speed_min"
    private const val KEY_MAX = "speed_max"

    @Volatile var currentSpeedKmh: Float = 0f
        private set
    @Volatile var averageSpeedKmh: Float = 0f
        private set
    @Volatile var minimumSpeedKmh: Float = 0f
        private set
    @Volatile var maximumSpeedKmh: Float = 0f
        private set

    private var started = false
    private var lastLocation: Location? = null
    private var lastSpeedTimeMillis = 0L

    @SuppressLint("MissingPermission")
    fun start(context: Context) {
        if (started) return

        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) return

        load(context)

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                handleLocation(context, location)
            }
        }

        try {
            var registered = false

            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                manager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    0.5f,
                    listener
                )
                registered = true
            }

            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                manager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1500L,
                    1f,
                    listener
                )
                registered = true
            }

            if (registered) {
                currentListener = listener
                locationManager = manager
                started = true
            }
        } catch (_: SecurityException) {
        }
    }

    private var locationManager: LocationManager? = null
    private var currentListener: LocationListener? = null

    private fun handleLocation(context: Context, location: Location) {
        if (location.hasAccuracy() && location.accuracy > 60f) return

        val now = System.currentTimeMillis()
        var speed = if (location.hasSpeed()) {
            max(0f, location.speed * 3.6f)
        } else 0f

        val previous = lastLocation
        if (previous != null && lastSpeedTimeMillis > 0L) {
            val seconds = (now - lastSpeedTimeMillis) / 1000f
            if (seconds >= 0.5f) {
                val meters = previous.distanceTo(location)
                if (meters >= 0f) {
                    val calculated = (meters / seconds) * 3.6f
                    if (calculated > 0.1f && speed <= 0.1f) speed = calculated
                    else if (calculated > 0.1f) speed = (speed + calculated) / 2f
                }
            }
        }

        lastLocation = Location(location)
        lastSpeedTimeMillis = now

        speed = speed.coerceIn(0f, 25f)
        currentSpeedKmh = speed

        if (speed >= 0.5f) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val date = today()

            if (prefs.getString(KEY_DATE, null) != date) {
                reset(context)
            }

            val samples = prefs.getLong(KEY_SAMPLES, 0L) + 1L
            val total = prefs.getFloat(KEY_TOTAL, 0f) + speed
            val oldMin = prefs.getFloat(KEY_MIN, Float.MAX_VALUE)
            val min = minOf(oldMin, speed)
            val max = maxOf(prefs.getFloat(KEY_MAX, 0f), speed)

            prefs.edit()
                .putString(KEY_DATE, date)
                .putLong(KEY_SAMPLES, samples)
                .putFloat(KEY_TOTAL, total)
                .putFloat(KEY_MIN, min)
                .putFloat(KEY_MAX, max)
                .apply()

            averageSpeedKmh = total / samples
            minimumSpeedKmh = min
            maximumSpeedKmh = max
        }
    }

    private fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_DATE, null) != today()) {
            reset(context)
            return
        }

        val samples = prefs.getLong(KEY_SAMPLES, 0L)
        val total = prefs.getFloat(KEY_TOTAL, 0f)

        averageSpeedKmh = if (samples > 0) total / samples else 0f
        minimumSpeedKmh = if (samples > 0) prefs.getFloat(KEY_MIN, 0f) else 0f
        maximumSpeedKmh = if (samples > 0) prefs.getFloat(KEY_MAX, 0f) else 0f
    }

    fun reset(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_DATE, today())
            .putLong(KEY_SAMPLES, 0L)
            .putFloat(KEY_TOTAL, 0f)
            .putFloat(KEY_MIN, 0f)
            .putFloat(KEY_MAX, 0f)
            .apply()

        currentSpeedKmh = 0f
        averageSpeedKmh = 0f
        minimumSpeedKmh = 0f
        maximumSpeedKmh = 0f
        lastLocation = null
        lastSpeedTimeMillis = 0L
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
