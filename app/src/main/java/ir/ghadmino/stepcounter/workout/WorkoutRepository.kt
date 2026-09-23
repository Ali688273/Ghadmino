package ir.ghadmino.stepcounter.workout

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import ir.ghadmino.stepcounter.profile.ProfileRepository
import ir.ghadmino.stepcounter.step.StepCounterService
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

data class WorkoutSummary(
    val steps: Int,
    val distanceMeters: Double,
    val durationMinutes: Int,
    val calories: Int,
    val averageSpeedKmh: Double
)

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
        return WorkoutSummary(steps, distanceMeters, minutes, max(0, calories), speed)
    }

    fun isRunning(): Boolean = running.get()
}
