package ir.ghadmino.stepcounter.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ir.ghadmino.stepcounter.activity.ManualActivityRepository
import ir.ghadmino.stepcounter.profile.ProfileRepository
import ir.ghadmino.stepcounter.step.StepCounterService
import ir.ghadmino.stepcounter.step.StepHistory
import ir.ghadmino.stepcounter.settings.SettingsRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

private val persianMonthNames = listOf(
    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
)

@Composable
fun ActivityCalendarScreen(goal: Int) {
    val context = LocalContext.current
    var monthOffset by remember { mutableIntStateOf(0) }
    var selectedDate by remember { mutableStateOf(today()) }
    val useSolarCalendar = SettingsRepository.load(context).useSolarCalendar

    val month = remember(monthOffset, useSolarCalendar) { buildCalendarMonth(monthOffset, useSolarCalendar) }
    val days = month.days
    val profile = remember { ProfileRepository.load(context) }
    val liveTodaySteps = remember { mutableIntStateOf(currentTodaySteps(context)) }

    LaunchedEffect(Unit) { liveTodaySteps.intValue = currentTodaySteps(context) }

    val selectedSteps = stepsForDate(context, selectedDate, liveTodaySteps.intValue)
    val manualSteps = ManualActivityRepository.stepsForDate(context, selectedDate)
    val manualMinutes = ManualActivityRepository.minutesForDate(context, selectedDate)
    val manualCalories = ManualActivityRepository.caloriesForDate(context, selectedDate)
    val estimatedDistanceKm = selectedSteps * profile.strideCm.coerceIn(30, 150) / 100000.0
    val estimatedCalories = estimateWalkingCalories(selectedSteps, profile.weightKg)

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { monthOffset-- },
                modifier = Modifier.width(88.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                Text("‹ ماه قبل", maxLines = 1, softWrap = false)
            }
            Text(
                month.label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false
            )
            TextButton(
                onClick = { monthOffset++ },
                modifier = Modifier.width(88.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                Text("ماه بعد ›", maxLines = 1, softWrap = false)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (if (useSolarCalendar) listOf("ش", "ی", "د", "س", "چ", "پ", "ج") else listOf("S", "M", "T", "W", "T", "F", "S")).forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    val value = if (date == null) 0 else stepsForDate(context, date, liveTodaySteps.intValue) + ManualActivityRepository.stepsForDate(context, date)
                    val isSelected = date == selectedDate
                    val cellBackground = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    val dayColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    val distanceColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                    Box(
                        Modifier.weight(1f).aspectRatio(0.88f).background(cellBackground, MaterialTheme.shapes.medium)
                            .clickable(enabled = date != null) { selectedDate = date!! },
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(dayNumber(date, useSolarCalendar), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = dayColor)
                                if (value > 0) {
                                    val distanceKm = value * profile.strideCm.coerceIn(30, 150) / 100000.0
                                    Text(String.format(Locale.US, "%.1f km", distanceKm), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = distanceColor, maxLines = 1)
                                } else {
                                    Text("—", style = MaterialTheme.typography.labelSmall, color = dayColor.copy(alpha = 0.55f))
                                }
                            }
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f).aspectRatio(0.88f)) }
            }
        }

        Divider()

        LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                Text("گزارش روز " + formatDate(selectedDate, useSolarCalendar), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            item { ReportCard("قدم‌های ثبت‌شده", selectedSteps.toString() + " قدم") }
            item { ReportCard("فعالیت دستی", manualSteps.toString() + " قدم • " + manualMinutes + " دقیقه • " + manualCalories + " kcal") }
            item {
                ReportCard("برآورد بر اساس پروفایل", String.format(Locale.US, "%.2f km • %d kcal", estimatedDistanceKm, estimatedCalories))
            }
            item { Text("فاصله بر اساس طول گام ثبت‌شده در پروفایل محاسبه شده و کالری یک برآورد است.", style = MaterialTheme.typography.bodySmall) }
            item {
                val goalState = if (selectedSteps + manualSteps >= goal) "هدف روزانه تکمیل شده" else "هدف روزانه تکمیل نشده"
                ReportCard("هدف " + goal, goalState)
            }
        }
    }
}

@Composable
private fun ReportCard(title: String, value: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun stepsForDate(context: android.content.Context, date: String, liveTodaySteps: Int): Int =
    if (date == today()) liveTodaySteps else StepHistory.get(context, date)

private fun currentTodaySteps(context: android.content.Context): Int =
    max(StepCounterService.todaySteps, StepCounterService.persistedTodaySteps(context))

private fun estimateWalkingCalories(steps: Int, weightKg: Float): Int {
    val safeWeight = weightKg.coerceIn(20f, 250f)
    val perStep = 0.035 + (safeWeight / 70.0) * 0.005
    return (steps.coerceAtLeast(0) * perStep).toInt().coerceAtLeast(0)
}

private data class CalendarMonth(val label: String, val days: List<String?>)

private fun buildCalendarMonth(offset: Int, solar: Boolean): CalendarMonth {
    if (solar) {
        // ICU guarantees a real Persian calendar; java.util.Calendar can fall back to Gregorian.
        val calendar = android.icu.util.PersianCalendar(java.util.TimeZone.getDefault(), android.icu.util.ULocale("fa_IR@calendar=persian"))
        calendar.time = Date()
        calendar.add(android.icu.util.Calendar.MONTH, offset)
        calendar.set(android.icu.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(android.icu.util.Calendar.HOUR_OF_DAY, 12)
        calendar.set(android.icu.util.Calendar.MINUTE, 0)
        calendar.set(android.icu.util.Calendar.SECOND, 0)
        calendar.set(android.icu.util.Calendar.MILLISECOND, 0)
        val year = calendar.get(android.icu.util.Calendar.YEAR)
        val monthIndex = calendar.get(android.icu.util.Calendar.MONTH)
        val maxDays = calendar.getActualMaximum(android.icu.util.Calendar.DAY_OF_MONTH)
        // Persian week starts Saturday; ICU DAY_OF_WEEK is Sunday=1 ... Saturday=7.
        val firstDay = (calendar.get(android.icu.util.Calendar.DAY_OF_WEEK) + 1) % 7
        val result = MutableList<String?>(firstDay) { null }
        for (day in 1..maxDays) {
            calendar.set(android.icu.util.Calendar.DAY_OF_MONTH, day)
            result.add(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time))
        }
        return CalendarMonth(persianMonthNames[monthIndex.coerceIn(0, 11)] + " " + year, result)
    }
    val calendar = Calendar.getInstance()
    calendar.time = Date()
    calendar.add(Calendar.MONTH, offset)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 12)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val firstDay = calendar.get(Calendar.DAY_OF_WEEK) - 1
    val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val result = MutableList<String?>(firstDay) { null }
    for (day in 1..maxDays) {
        calendar.set(Calendar.DAY_OF_MONTH, day)
        result.add(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time))
    }
    return CalendarMonth(SimpleDateFormat("MMMM yyyy", Locale.US).format(calendar.time), result)
}

private fun dayNumber(date: String, solar: Boolean): String {
    val parsed = parseDate(date) ?: return date.substringAfterLast("-")
    if (solar) {
        val calendar = android.icu.util.PersianCalendar(java.util.TimeZone.getDefault(), android.icu.util.ULocale("fa_IR@calendar=persian"))
        calendar.time = parsed
        return calendar.get(android.icu.util.Calendar.DAY_OF_MONTH).toString()
    }
    val calendar = Calendar.getInstance()
    calendar.time = parsed
    return calendar.get(Calendar.DAY_OF_MONTH).toString()
}

private fun formatDate(date: String, solar: Boolean): String {
    val parsed = parseDate(date) ?: return date
    if (!solar) return SimpleDateFormat("yyyy/MM/dd", Locale.US).format(parsed)
    val calendar = android.icu.util.PersianCalendar(java.util.TimeZone.getDefault(), android.icu.util.ULocale("fa_IR@calendar=persian"))
    calendar.time = parsed
    val value = String.format(Locale.US, "%04d/%02d/%02d", calendar.get(android.icu.util.Calendar.YEAR), calendar.get(android.icu.util.Calendar.MONTH) + 1, calendar.get(android.icu.util.Calendar.DAY_OF_MONTH))
    return value.map { digit ->
        when (digit) {
            '0' -> '۰'; '1' -> '۱'; '2' -> '۲'; '3' -> '۳'; '4' -> '۴'
            '5' -> '۵'; '6' -> '۶'; '7' -> '۷'; '8' -> '۸'; '9' -> '۹'
            else -> digit
        }
    }.joinToString("")
}
private fun parseDate(date: String): Date? =
    try { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) } catch (_: Exception) { null }

private fun today(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
