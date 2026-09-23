package ir.ghadmino.stepcounter.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateRequest
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object HealthConnectRepository {
    const val PROVIDER = HealthConnectClient.DEFAULT_PROVIDER_PACKAGE_NAME

    fun availability(context: Context): Int =
        HealthConnectClient.getSdkStatus(context, PROVIDER)

    fun client(context: Context): HealthConnectClient? =
        if (availability(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context, PROVIDER)
        } else {
            null
        }

    suspend fun todaySteps(context: Context): Long? {
        val healthClient = client(context) ?: return null
        val zone = ZoneId.systemDefault()
        val start = ZonedDateTime.now(zone)
            .toLocalDate()
            .atStartOfDay(zone)
            .toInstant()
        val end = Instant.now()

        val result = healthClient.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        return result[StepsRecord.COUNT_TOTAL]
    }

    fun manageDataIntent(context: Context) =
        HealthConnectClient.getHealthConnectManageDataIntent(context, PROVIDER)
}
