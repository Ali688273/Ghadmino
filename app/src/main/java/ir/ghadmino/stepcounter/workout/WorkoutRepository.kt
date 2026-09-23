package ir.ghadmino.stepcounter.workout

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import ir.ghadmino.stepcounter.profile.ProfileRepository
import ir.ghadmino.stepcounter.step.StepCounterService
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

data class WorkoutSummary(
    val id: Long,
    val startedAt: Long,
    val steps: Int,
    val distanceMeters: Double,
    val durationMinutes: Int,
    val calories: Int,
    val averageSpeedKmh: Double
)

object WorkoutRepository {
    private const val PREFS = "ghadmino_workouts"
    private const val KEY_SESSIONS = "sessions"

    fun save(context: Context, summary: WorkoutSummary) {
        val old = load(context).toMutableList()
        old.add(0, summary)
        val array = JSONArray()
        old.take(100).forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("startedAt", item.startedAt)
                put("steps", item.steps)
                put("distanceMeters", item.distanceMeters)
                put("durationMinutes", item.durationMinutes)
                put("calories", item.calories)
                put("averageSpeedKmh", item.averageSpeedKmh)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_SESSIONS, array.toString()).apply()
    }

    fun load(context: Context): List<WorkoutSummary> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SESSIONS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(WorkoutSummary(
                        o.optLong("id"),
                        o.optLong("startedAt"),
                        o.optInt("steps"),
                        o.optDouble("distanceMeters"),
                        o.optInt("durationMinutes"),
                        o.optInt("calories"),
                        o.optDouble("averageSpeedKmh")
                    ))
                }
            }.sortedByDescending { it.startedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun totalSteps(context: Context): Int =
        load(context).sumOf { it.steps }.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    fun totalDistanceMeters(context: Context): Double =
        load(context).sumOf { it.distanceMeters }

    fun bestDistance(context: Context): Double =
        load(context).maxOfOrNull { it.distanceMeters } ?: 0.0

    fun bestSteps(context: Context): Int =
        load(context).maxOfOrNull { it.steps } ?: 0

    fun formatDate(timestamp: Long): String =
        SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.US).format(Date(timestamp))
}

class WorkoutTracker(private val context: Context) {
    private val running = AtomicBoolean(false)
    private var locationManager: LocationManager? = null
    private var listener: LocationListener? = null
    private var lastLocation: Location? = null
    private var startSteps = 0
    private var startMillis = 0L
    private var distanceMeters = 0.0

    fun start() {
        if (!running.compareAndSet(false, true)) return
        startSteps = StepCounterService.todaySteps
        startMillis = System.currentTimeMillis()
        distanceMeters = 0.0
        lastLocation = null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val l = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (location.hasAccuracy() && location.accuracy > 60f) return
                val previous = lastLocation
                if (previous != null) {
                    val d = previous.distanceTo(location)
                    if (d in 0.5f..100f) distanceMeters += d.toDouble()
                }
                lastLocation = Location(location)
            }
        }
        try {
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, l)
            } else if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1500L, 2f, l)
            } else {
                running.set(false)
                return
            }
            locationManager = manager
            listener = l
        } catch (_: SecurityException) {
            running.set(false)
        }
    }

    fun stop(): WorkoutSummary {
        try { listener?.let { locationManager?.removeUpdates(it) } } catch (_: SecurityException) {}
        listener = null
        locationManager = null
        running.set(false)

        val steps = (StepCounterService.todaySteps - startSteps).coerceAtLeast(0)
        val minutes = ((System.currentTimeMillis() - startMillis) / 60000L).toInt().coerceAtLeast(1)
        val profile = ProfileRepository.load(context)
        val calories = (steps * (0.035 + (profile.weightKg / 70.0) * 0.005)).toInt()
        val hours = minutes / 60.0
        val speed = if (hours > 0.0) (distanceMeters / 1000.0) / hours else 0.0

        return WorkoutSummary(System.currentTimeMillis(), startMillis, steps, distanceMeters, minutes, max(0, calories), speed)
    }

    fun isRunning(): Boolean = running.get()
}
