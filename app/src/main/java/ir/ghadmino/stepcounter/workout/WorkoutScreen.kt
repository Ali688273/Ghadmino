package ir.ghadmino.stepcounter.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import ir.ghadmino.stepcounter.step.StepCounterService
import kotlinx.coroutines.delay

@Composable
fun WorkoutScreen() {
    val context = LocalContext.current
    val tracker = remember { WorkoutTracker(context) }
    var running by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    var steps by remember { mutableIntStateOf(maxOf(StepCounterService.todaySteps, StepCounterService.persistedTodaySteps(context))) }
    var summary by remember { mutableStateOf<WorkoutSummary?>(null) }
    var sessions by remember { mutableStateOf(WorkoutRepository.load(context)) }

    LaunchedEffect(running) {
        while (running) {
            seconds++
            steps = maxOf(StepCounterService.todaySteps, StepCounterService.persistedTodaySteps(context))
            delay(1000)
        }
    }

    val totalSteps = sessions.sumOf { it.steps }
    val totalDistance = sessions.sumOf { it.distanceMeters }
    val totalCalories = sessions.sumOf { it.calories }
    val bestSteps = sessions.maxOfOrNull { it.steps } ?: 0
    val bestDistance = sessions.maxOfOrNull { it.distanceMeters } ?: 0.0
    val longest = sessions.maxOfOrNull { it.durationMinutes } ?: 0
    val fastest = sessions.maxOfOrNull { it.averageSpeedKmh } ?: 0.0

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(20.dp)) {
                Icon(Icons.Default.DirectionsWalk, null, Modifier.size(44.dp))
                Spacer(Modifier.height(8.dp))
                Text("جلسه پیاده‌روی", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(if (running) "در حال ثبت..." else "برای شروع، دکمه زیر را بزن.")
                Spacer(Modifier.height(12.dp))
                Text("زمان: %02d:%02d".format(seconds / 60, seconds % 60))
                Text("قدم امروز: " + steps)
                Spacer(Modifier.height(12.dp))

                if (!running) {
                    Button(
                        onClick = {
                            seconds = 0
                            tracker.start()
                            running = tracker.isRunning()
                        },
                        Modifier.fillMaxWidth()
                    ) {
                        Text("شروع تمرین")
                    }
                } else {
                    Button(
                        onClick = {
                            val result = tracker.stop()
                            if (result != null) {
                                summary = result
                                WorkoutRepository.save(context, result)
                                sessions = WorkoutRepository.load(context)
                            }
                            running = false
                        },
                        Modifier.fillMaxWidth()
                    ) {
                        Text("پایان تمرین و ذخیره")
                    }
                }
            }
        }

        summary?.let { s ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("نتیجه آخرین تمرین", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("قدم: " + s.steps)
                    Text("مسافت GPS: " + String.format("%.2f km", s.distanceMeters / 1000.0))
                    Text("زمان: " + s.durationMinutes + " دقیقه")
                    Text("کالری تخمینی: " + s.calories + " kcal")
                    Text("سرعت میانگین: " + String.format("%.1f km/h", s.averageSpeedKmh))
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("آمار کلی تمرین‌ها", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("تعداد جلسات: " + sessions.size)
                Text("مجموع قدم تمرینی: " + totalSteps)
                Text("مجموع مسافت: " + String.format("%.2f km", totalDistance / 1000.0))
                Text("مجموع کالری تخمینی: " + totalCalories + " kcal")
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("رکوردهای شخصی", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("بیشترین قدم در یک جلسه: " + bestSteps)
                Text("بیشترین مسافت: " + String.format("%.2f km", bestDistance / 1000.0))
                Text("طولانی‌ترین جلسه: " + longest + " دقیقه")
                Text("بیشترین سرعت میانگین: " + String.format("%.1f km/h", fastest))
            }
        }

        Text("تاریخچه جلسات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        if (sessions.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    "هنوز جلسه‌ای ذخیره نشده است. بعد از پایان اولین تمرین، تاریخچه اینجا ساخته می‌شود.",
                    Modifier.padding(18.dp)
                )
            }
        } else {
            sessions.take(20).forEach { session ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            WorkoutRepository.formatDate(session.startedAt),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            session.steps.toString() + " قدم • " +
                                session.durationMinutes + " دقیقه • " +
                                String.format("%.2f km", session.distanceMeters / 1000.0)
                        )
                        Text(
                            "کالری: " + session.calories +
                                " • میانگین سرعت: " +
                                String.format("%.1f km/h", session.averageSpeedKmh)
                        )
                    }
                }
            }
        }

        Text(
            "جلسه‌ها به‌صورت محلی روی گوشی ذخیره می‌شوند؛ حداکثر ۱۰۰ جلسه آخر نگهداری می‌شود.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
