package ir.ghadmino.stepcounter.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import ir.ghadmino.stepcounter.activity.ManualActivityRepository
import ir.ghadmino.stepcounter.profile.ProfileRepository
import ir.ghadmino.stepcounter.stats.SpeedHistoryRepository
import ir.ghadmino.stepcounter.step.StepCounterService
import ir.ghadmino.stepcounter.step.StepHistory
import ir.ghadmino.stepcounter.workout.WorkoutRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun ActivityCalendarScreen(goal: Int) {
    val context = LocalContext.current
    var monthOffset by remember { mutableIntStateOf(0) }
    var selectedDate by remember { mutableStateOf(today()) }

    val month = remember(monthOffset) { monthCalendar(monthOffset) }
    val days = remember(monthOffset) { monthDays(month) }
    val profile = remember { ProfileRepository.load(context) }
    val liveTodaySteps = remember { mutableIntStateOf(currentTodaySteps(context)) }

    LaunchedEffect(Unit) {
        liveTodaySteps.intValue = currentTodaySteps(context)
    }

    val selectedSteps = stepsForDate(context, selectedDate, liveTodaySteps.intValue)
    val manualSteps = ManualActivityRepository.stepsForDate(context, selectedDate)
    val manualMinutes = ManualActivityRepository.minutesForDate(context, selectedDate)
    val manualCalories = ManualActivityRepository.caloriesForDate(context, selectedDate)
    val workouts = WorkoutRepository.load(context).filter { dateOf(it.startedAt) == selectedDate }
    val totalWorkoutDistance = workouts.sumOf { it.distanceMeters }
    val totalWorkoutCalories = workouts.sumOf { it.calories }
    val dailySpeed = SpeedHistoryRepository.maximumForDate(context, selectedDate)

    val estimatedDistanceKm = selectedSteps * profile.strideCm.coerceIn(30, 150) / 100000.0
    val estimatedCalories = estimateWalkingCalories(selectedSteps, profile.weightKg)

    Column(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = { monthOffset-- }) { Text("‹ ماه قبل") }
            Text(
                monthLabel(month),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { monthOffset++ }) { Text("ماه بعد ›") }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEach {
                Text(it, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }

        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    val value = if (date == null) 0 else stepsForDate(context, date, liveTodaySteps.intValue)
                    Box(
                        Modifier.weight(1f).aspectRatio(1f)
                            .background(
                                if (date == selectedDate) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.shapes.small
                            )
                            .clickable(enabled = date != null) { selectedDate = date!! },
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(dayNumber(date), style = MaterialTheme.typography.labelMedium)
                                if (value > 0) Text(
                                    if (value >= 1000) (value / 1000).toString() + "k" else value.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
                repeat(7 - week.size) {
                    Spacer(Modifier.weight(1f).aspectRatio(1f))
                }
            }
        }

        HorizontalDivider()

        LazyColumn(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Text(
                    "گزارش روز " + formatDate(selectedDate),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                ReportCard("قدم‌های ثبت‌شده", selectedSteps.toString() + " قدم")
            }
            item {
                ReportCard(
                    "فعالیت دستی",
                    manualSteps.toString() + " قدم • " + manualMinutes + " دقیقه • " + manualCalories + " kcal"
                )
            }
            item {
                ReportCard(
                    "برآورد بر اساس پروفایل",
                    String.format(
                        Locale.US,
                        "%.2f km • %d kcal",
                        estimatedDistanceKm,
                        estimatedCalories
                    )
                )
            }
            item {
                Text(
                    "فاصله بر اساس طول گام ثبت‌شده در پروفایل محاسبه شده و کالری یک برآورد است.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            item {
                ReportCard(
                    "تمرین‌های ثبت‌شده",
                    workouts.size.toString() + " جلسه • " +
                        String.format(
                            Locale.US,
                            "%.2f km • %d kcal",
                            totalWorkoutDistance / 1000.0,
                            totalWorkoutCalories
                        )
                )
            }
            item {
                ReportCard(
                    "سرعت ثبت‌شده",
                    if (dailySpeed > 0f) {
                        String.format(Locale.US, "%.1f km/h", dailySpeed)
                    } else {
                        "برای این روز رکورد سرعت ذخیره‌شده‌ای نیست"
                    }
                )
            }
            item {
                val goalState =
                    if (selectedSteps + manualSteps >= goal) "هدف روزانه تکمیل شده"
                    else "هدف روزانه تکمیل نشده"
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
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun stepsForDate(context: android.content.Context, date: String, liveTodaySteps: Int): Int {
    return if (date == today()) liveTodaySteps else StepHistory.get(context, date)
}

private fun currentTodaySteps(context: android.content.Context): Int {
    return max(
        StepCounterService.todaySteps,
        StepCounterService.persistedTodaySteps(context)
    )
}

private fun estimateWalkingCalories(steps: Int, weightKg: Float): Int {
    val safeWeight = weightKg.coerceIn(20f, 250f)
    val perStep = 0.035 + (safeWeight / 70.0) * 0.005
    return (steps.coerceAtLeast(0) * perStep).toInt().coerceAtLeast(0)
}

private fun monthCalendar(offset: Int): Calendar {
    val c = Calendar.getInstance()
    c.add(Calendar.MONTH, offset)
    c.set(Calendar.DAY_OF_MONTH, 1)
    return c
}

private fun monthDays(month: Calendar): List<String?> {
    val firstDay = ((month.get(Calendar.DAY_OF_WEEK) + 6) % 7)
    val max = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    val result = MutableList<String?>(firstDay) { null }
    for (day in 1..max) {
        val c = month.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, day)
        result.add(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time))
    }
    return result
}

private fun monthLabel(calendar: Calendar): String =
    SimpleDateFormat("yyyy/MM", Locale.US).format(calendar.time)

private fun dayNumber(date: String): String =
    date.substringAfterLast("-").trimStart('0').ifBlank { "0" }

private fun formatDate(date: String): String {
    return try {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: return date
        SimpleDateFormat("yyyy/MM/dd", Locale.US).format(d)
    } catch (_: Exception) {
        date
    }
}

private fun dateOf(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))

private fun today(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
