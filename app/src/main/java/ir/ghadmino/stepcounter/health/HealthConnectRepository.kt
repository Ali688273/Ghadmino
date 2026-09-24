package ir.ghadmino.stepcounter.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object HealthConnectRepository {
    const val PROVIDER = "com.google.android.apps.healthdata"

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

    suspend fun todayDistanceMeters(context: Context): Double? {
        val healthClient = client(context) ?: return null
        val zone = ZoneId.systemDefault()
        val start = ZonedDateTime.now(zone).toLocalDate().atStartOfDay(zone).toInstant()
        val result = healthClient.aggregate(
            AggregateRequest(
                metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, Instant.now())
            )
        )
        return result[DistanceRecord.DISTANCE_TOTAL]?.inMeters
    }

    suspend fun todayCalories(context: Context): Double? {
        val healthClient = client(context) ?: return null
        val zone = ZoneId.systemDefault()
        val start = ZonedDateTime.now(zone).toLocalDate().atStartOfDay(zone).toInstant()
        val result = healthClient.aggregate(
            AggregateRequest(
                metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, Instant.now())
            )
        )
        return result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
    }

    suspend fun writeTodaySteps(context: Context, steps: Long): Boolean {
        if (steps <= 0L) return false
        val healthClient = client(context) ?: return false
        val zone = ZoneId.systemDefault()
        val start = ZonedDateTime.now(zone).toLocalDate().atStartOfDay(zone).toInstant()
        val end = Instant.now()
        val record = StepsRecord(
            count = steps,
            startTime = start,
            endTime = end,
            startZoneOffset = zone.rules.getOffset(start),
            endZoneOffset = zone.rules.getOffset(end),
            metadata = androidx.health.connect.client.records.metadata.Metadata.autoRecorded(
                clientRecordId = "ghadmino_steps_" + start.toString().substringBefore("T"),
                clientRecordVersion = System.currentTimeMillis(),
                device = androidx.health.connect.client.records.metadata.Device(
                    type = androidx.health.connect.client.records.metadata.Device.TYPE_PHONE
                )
            )
        )
        healthClient.insertRecords(listOf(record))
        return true
    }

    suspend fun syncRecentDays(context: Context, rows: List<Pair<String, Int>>): Int {
        val healthClient = client(context) ?: return 0
        val zone = ZoneId.systemDefault()
        val records = rows.filter { it.second > 0 }.map { row ->
            val date = java.time.LocalDate.parse(row.first)
            val start = date.atStartOfDay(zone).toInstant()
            val end = if (date == java.time.LocalDate.now(zone)) Instant.now() else date.plusDays(1).atStartOfDay(zone).toInstant()
            StepsRecord(
                count = row.second.toLong(),
                startTime = start,
                endTime = end,
                startZoneOffset = zone.rules.getOffset(start),
                endZoneOffset = zone.rules.getOffset(end),
                metadata = androidx.health.connect.client.records.metadata.Metadata.autoRecorded(
                    clientRecordId = "ghadmino_steps_" + row.first,
                    clientRecordVersion = System.currentTimeMillis(),
                    device = androidx.health.connect.client.records.metadata.Device(
                        type = androidx.health.connect.client.records.metadata.Device.TYPE_PHONE
                    )
                )
            )
        }
        if (records.isEmpty()) return 0
        healthClient.insertRecords(records)
        return records.size
    }

    fun manageDataIntent(context: Context) =
        HealthConnectClient.getHealthConnectManageDataIntent(context, PROVIDER)
}
