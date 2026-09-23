package ir.ghadmino.stepcounter.workout

import androidx.compose.foundation.layout.*
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
    var steps by remember { mutableIntStateOf(0) }
    var summary by remember { mutableStateOf<WorkoutSummary?>(null) }

    LaunchedEffect(running) {
        while (running) {
            seconds++
            steps = StepCounterService.todaySteps
            delay(1000)
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(20.dp)) {
                Icon(Icons.Default.DirectionsWalk, null, Modifier.size(44.dp))
                Spacer(Modifier.height(8.dp))
                Text("جلسه پیاده‌روی", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(if (running) "در حال ثبت..." else "برای شروع، دکمه زیر را بزن.")
                Spacer(Modifier.height(12.dp))
                Text("زمان: %02d:%02d".format(seconds / 60, seconds % 60))
                Text("قدم امروز: $steps")
            }
        }
        if (!running) {
            Button(onClick = { tracker.start(); running = tracker.isRunning() }, Modifier.fillMaxWidth()) {
                Text("شروع تمرین")
            }
        } else {
            Button(onClick = { summary = tracker.stop(); running = false }, Modifier.fillMaxWidth()) {
                Text("پایان تمرین")
            }
        }
        summary?.let { s ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("نتیجه تمرین", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("قدم: " + s.steps)
                    Text("مسافت GPS: " + String.format("%.2f km", s.distanceMeters / 1000.0))
                    Text("زمان: " + s.durationMinutes + " دقیقه")
                    Text("کالری تخمینی: " + s.calories + " kcal")
                    Text("سرعت میانگین: " + String.format("%.1f km/h", s.averageSpeedKmh))
                }
            }
        }
        Text("این جلسه تا زمانی که این صفحه فعال است GPS را ثبت می‌کند؛ برای ثبت دائمی در پس‌زمینه، سرویس جداگانه لازم است.", style = MaterialTheme.typography.bodySmall)
    }
}
