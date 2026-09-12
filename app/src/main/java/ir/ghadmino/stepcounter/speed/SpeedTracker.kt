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

    private val locationListener =
        object : LocationListener {

            override fun onLocationChanged(
                location: Location
            ) {

                val speedMps =
                    if (
                        location.hasSpeed()
                    ) {
                        location.speed
                    } else {
                        0f
                    }

                val speedKmh =
                    max(
                        0f,
                        speedMps * 3.6f
                    )

                currentSpeedKmh =
                    speedKmh

                // سرعت‌های خیلی پایین
                // را برای آمار حرکتی حساب نمی‌کنیم.
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
            // مجوز مکان وجود ندارد.
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
            // مجوز مکان وجود ندارد.
        }
    }


    fun resetDailyStats() {

        currentSpeedKmh = 0f

        averageSpeedKmh = 0f

        minimumSpeedKmh = 0f

        maximumSpeedKmh = 0f

        speedSamples = 0

        speedTotal = 0f
    }
}
