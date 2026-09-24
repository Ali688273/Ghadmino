package ir.ghadmino.stepcounter.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.DataOrigin
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

        val range = TimeRangeFilter.between(start, end)
        val all = healthClient.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = range
            )
        )[StepsRecord.COUNT_TOTAL] ?: 0L
        val own = healthClient.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = range,
                dataOriginFilter = setOf(DataOrigin(context.packageName))
            )
        )[StepsRecord.COUNT_TOTAL] ?: 0L
        return (all - own).coerceAtLeast(0L)
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
                clientRecordVersion = versionFor(context, start.toString().substringBefore("T"), steps),
                device = androidx.health.connect.client.records.metadata.Device(
                    type = androidx.health.connect.client.records.metadata.Device.TYPE_PHONE
                )
            )
        )
        val id = "ghadmino_steps_" + start.toString().substringBefore("T")
        try {
            healthClient.deleteRecords(
                StepsRecord::class,
                recordIdsList = emptyList(),
                    clientRecordIdsList = listOf(id)
            )
        } catch (_: Exception) {
        }
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
                    clientRecordVersion = versionFor(context, row.first, row.second.toLong()),
                    device = androidx.health.connect.client.records.metadata.Device(
                        type = androidx.health.connect.client.records.metadata.Device.TYPE_PHONE
                    )
                )
            )
        }
        if (records.isEmpty()) return 0
        val ids = records.mapNotNull { it.metadata.clientRecordId }.toSet()
        if (ids.isNotEmpty()) {
            try {
                healthClient.deleteRecords(
                    StepsRecord::class,
                    recordIdsList = emptyList(),
                    clientRecordIdsList = ids
                )
            } catch (_: Exception) {
            }
        }
        healthClient.insertRecords(records)
        return records.size
    }

    private fun versionFor(context: Context, date: String, steps: Long): Long {
        val p = context.getSharedPreferences("ghadmino_health_sync", Context.MODE_PRIVATE)
        val key = "version_" + date
        val old = p.getLong(key, 0L)
        val next = maxOf(System.currentTimeMillis(), old + 1L, steps.coerceAtLeast(0L))
        p.edit().putLong(key, next).apply()
        return next
    }

    fun manageDataIntent(context: Context) =
        HealthConnectClient.getHealthConnectManageDataIntent(context, PROVIDER)
}
