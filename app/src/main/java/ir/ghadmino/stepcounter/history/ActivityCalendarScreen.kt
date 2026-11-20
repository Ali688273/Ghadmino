package ir.ghadmino.stepcounter.history

import android.icu.util.PersianCalendar
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
import ir.ghadmino.stepcounter.workout.WorkoutRepository
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
    var useSolarCalendar by remember { mutableStateOf(true) }

    val month = remember(monthOffset, useSolarCalendar) { monthCalendar(monthOffset, useSolarCalendar) }
    val days = remember(monthOffset, useSolarCalendar) { monthDays(month) }
    val profile = remember { ProfileRepository.load(context) }
    val liveTodaySteps = remember { mutableIntStateOf(currentTodaySteps(context)) }

    LaunchedEffect(Unit) { liveTodaySteps.intValue = currentTodaySteps(context) }

    val selectedSteps = stepsForDate(context, selectedDate, liveTodaySteps.intValue)
    val manualSteps = ManualActivityRepository.stepsForDate(context, selectedDate)
    val manualMinutes = ManualActivityRepository.minutesForDate(context, selectedDate)
    val manualCalories = ManualActivityRepository.caloriesForDate(context, selectedDate)
    val workouts = WorkoutRepository.load(context).filter { dateOf(it.startedAt) == selectedDate }
    val totalWorkoutDistance = workouts.sumOf { it.distanceMeters }
    val totalWorkoutCalories = workouts.sumOf { it.calories }
    val estimatedDistanceKm = selectedSteps * profile.strideCm.coerceIn(30, 150) / 100000.0
    val estimatedCalories = estimateWalkingCalories(selectedSteps, profile.weightKg)

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = useSolarCalendar, onClick = { useSolarCalendar = true; monthOffset = 0 }, label = { Text("شمسی") })
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = !useSolarCalendar, onClick = { useSolarCalendar = false; monthOffset = 0 }, label = { Text("میلادی") })
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { monthOffset-- }) { Text("‹ ماه قبل") }
            Text(monthLabel(month, useSolarCalendar), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = { monthOffset++ }) { Text("ماه بعد ›") }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEach {
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
                ReportCard("تمرین‌های ثبت‌شده", workouts.size.toString() + " جلسه • " + String.format(Locale.US, "%.2f km • %d kcal", totalWorkoutDistance / 1000.0, totalWorkoutCalories))
            }
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

private fun monthCalendar(offset: Int, solar: Boolean): Calendar {
    val calendar: Calendar = if (solar) PersianCalendar() else Calendar.getInstance()
    calendar.time = Date()
    calendar.add(Calendar.MONTH, offset)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 12)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar
}

private fun monthDays(month: Calendar): List<String?> {
    val firstDay = month.get(Calendar.DAY_OF_WEEK) % 7
    val maxDays = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    val result = MutableList<String?>(firstDay) { null }
    for (day in 1..maxDays) {
        val calendar = month.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, day)
        result.add(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time))
    }
    return result
}

private fun monthLabel(calendar: Calendar, solar: Boolean): String =
    if (solar) "${persianMonthNames[calendar.get(Calendar.MONTH).coerceIn(0, 11)]} ${calendar.get(Calendar.YEAR)}"
    else SimpleDateFormat("MMMM yyyy", Locale.US).format(calendar.time)

private fun dayNumber(date: String, solar: Boolean): String {
    val parsed = parseDate(date) ?: return date.substringAfterLast("-")
    val calendar: Calendar = if (solar) PersianCalendar() else Calendar.getInstance()
    calendar.time = parsed
    return calendar.get(Calendar.DAY_OF_MONTH).toString()
}

private fun formatDate(date: String, solar: Boolean): String {
    val parsed = parseDate(date) ?: return date
    val calendar: Calendar = if (solar) PersianCalendar() else Calendar.getInstance()
    calendar.time = parsed
    return if (solar) String.format(Locale.US, "%04d/%02d/%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
    else SimpleDateFormat("yyyy/MM/dd", Locale.US).format(parsed)
}

private fun parseDate(date: String): Date? =
    try { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) } catch (_: Exception) { null }

private fun dateOf(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))

private fun today(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
