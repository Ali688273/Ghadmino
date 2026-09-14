package ir.ghadmino.stepcounter.speed

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlin.math.max

class SpeedTracker(
    private val context: Context
) {

    private val locationManager =
        context.getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager

    @Volatile
    var currentSpeedKmh: Float = 0f
        private set

    @Volatile
    var averageSpeedKmh: Float = 0f
        private set

    @Volatile
    var minimumSpeedKmh: Float = 0f
        private set

    @Volatile
    var maximumSpeedKmh: Float = 0f
        private set

    private var speedSamples = 0
    private var speedTotal = 0f

    private var lastLocation: Location? = null

    private var lastSpeedTimeMillis: Long = 0L

    private val locationListener =
        object : LocationListener {

            override fun onLocationChanged(
                location: Location
            ) {

                if (!location.hasAccuracy()) {
                    return
                }

                if (location.accuracy > 50f) {
                    return
                }

                val now = System.currentTimeMillis()

                var speedKmh = 0f

                if (location.hasSpeed()) {

                    speedKmh =
                        max(
                            0f,
                            location.speed * 3.6f
                        )
                }

                val previousLocation =
                    lastLocation

                if (
                    previousLocation != null &&
                    lastSpeedTimeMillis > 0L
                ) {

                    val timeSeconds =
                        (
                            now -
                                lastSpeedTimeMillis
                            ) / 1000f

                    if (timeSeconds >= 0.5f) {

                        val distanceMeters =
                            previousLocation.distanceTo(
                                location
                            )

                        val calculatedSpeedKmh =
                            if (distanceMeters >= 0f) {

                                (
                                    distanceMeters /
                                        timeSeconds
                                ) * 3.6f

                            } else {
                                0f
                            }

                        if (
                            calculatedSpeedKmh >
                            0.1f
                        ) {

                            if (
                                speedKmh <= 0.1f
                            ) {

                                speedKmh =
                                    calculatedSpeedKmh

                            } else {

                                speedKmh =
                                    (
                                        speedKmh +
                                            calculatedSpeedKmh
                                        ) / 2f
                            }
                        }
                    }
                }

                lastLocation =
                    Location(location)

                lastSpeedTimeMillis =
                    now

                speedKmh =
                    speedKmh.coerceIn(
                        0f,
                        25f
                    )

                currentSpeedKmh =
                    speedKmh

                /*
                 * فقط سرعت‌های معقول حرکتی
                 * وارد آمار می‌شوند.
                 */
                if (speedKmh >= 0.5f) {

                    speedSamples++

                    speedTotal +=
                        speedKmh

                    averageSpeedKmh =
                        speedTotal /
                            speedSamples

                    if (
                        minimumSpeedKmh == 0f ||
                        speedKmh <
                        minimumSpeedKmh
                    ) {

                        minimumSpeedKmh =
                            speedKmh
                    }

                    if (
                        speedKmh >
                        maximumSpeedKmh
                    ) {

                        maximumSpeedKmh =
                            speedKmh
                    }
                }
            }
        }

    @SuppressLint("MissingPermission")
    fun start() {

        val fineGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (
            !fineGranted &&
            !coarseGranted
        ) {
            return
        }

        try {

            if (
                locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                )
            ) {

                locationManager.requestLocationUpdates(

                    LocationManager.GPS_PROVIDER,

                    1000L,

                    1f,

                    locationListener
                )
            }

        } catch (
            _: SecurityException
        ) {
            // مجوز مکان در دسترس نیست.
        }
    }

    fun stop() {

        try {

            locationManager.removeUpdates(
                locationListener
            )

        } catch (
            _: SecurityException
        ) {
            // مجوز مکان در دسترس نیست.
        }
    }

    fun resetDailyStats() {

        currentSpeedKmh = 0f

        averageSpeedKmh = 0f

        minimumSpeedKmh = 0f

        maximumSpeedKmh = 0f

        speedSamples = 0

        speedTotal = 0f

        lastLocation = null

        lastSpeedTimeMillis = 0L
    }
}
