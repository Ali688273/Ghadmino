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
import kotlin.math.abs
import kotlin.math.max
import android.os.SystemClock

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
        SpeedTrackerState.stop(context.applicationContext)
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
    private var lastStartElapsed = 0L
    private var lastLocation: Location? = null
    private var lastSpeedTimeMillis = 0L
    private var smoothedSpeedKmh = 0f

    @SuppressLint("MissingPermission")
    fun start(context: Context) {
        if (started) return
        if (SystemClock.elapsedRealtime() - lastStartElapsed < 800L) return
        lastStartElapsed = SystemClock.elapsedRealtime()

        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
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
        val rawGpsSpeed = if (location.hasSpeed()) {
            max(0f, location.speed * 3.6f)
        } else 0f

        val previous = lastLocation
        val seconds = if (lastSpeedTimeMillis > 0L) {
            (now - lastSpeedTimeMillis) / 1000f
        } else 0f

        var calculatedSpeed = 0f
        if (previous != null && seconds >= 0.5f && seconds <= 10f) {
            val meters = previous.distanceTo(location)
            if (meters >= 0f) {
                calculatedSpeed = (meters / seconds) * 3.6f
            }
        }

        var speed = when {
            rawGpsSpeed > 0.2f && calculatedSpeed > 0.2f ->
                (rawGpsSpeed * 0.65f) + (calculatedSpeed * 0.35f)
            rawGpsSpeed > 0.2f -> rawGpsSpeed
            calculatedSpeed > 0.2f -> calculatedSpeed
            else -> 0f
        }

        lastLocation = Location(location)
        lastSpeedTimeMillis = now

        speed = speed.coerceIn(0f, 35f)

        if (speed < 0.4f) {
            smoothedSpeedKmh *= 0.55f
            if (smoothedSpeedKmh < 0.15f) smoothedSpeedKmh = 0f
        } else {
            val jump = abs(speed - smoothedSpeedKmh)
            val factor = when {
                smoothedSpeedKmh == 0f -> 1f
                jump > 12f -> 0.20f
                jump > 6f -> 0.35f
                else -> 0.55f
            }
            smoothedSpeedKmh += (speed - smoothedSpeedKmh) * factor
        }

        currentSpeedKmh = smoothedSpeedKmh.coerceIn(0f, 35f)

        if (currentSpeedKmh >= 0.5f) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val date = today()

            if (prefs.getString(KEY_DATE, null) != date) {
                reset(context)
            }

            val samples = prefs.getLong(KEY_SAMPLES, 0L) + 1L
            val total = prefs.getFloat(KEY_TOTAL, 0f) + currentSpeedKmh
            val oldMin = prefs.getFloat(KEY_MIN, Float.MAX_VALUE)
            val min = minOf(oldMin, currentSpeedKmh)
            val max = maxOf(prefs.getFloat(KEY_MAX, 0f), currentSpeedKmh)

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

    fun stop(context: Context) {
        try {
            currentListener?.let { listener ->
                locationManager?.removeUpdates(listener)
            }
        } catch (_: SecurityException) {
        }
        currentListener = null
        locationManager = null
        started = false
        lastLocation = null
        lastSpeedTimeMillis = 0L
        smoothedSpeedKmh = 0f
        currentSpeedKmh = 0f
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
        smoothedSpeedKmh = 0f
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
