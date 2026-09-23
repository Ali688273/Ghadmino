package ir.ghadmino.stepcounter.plan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import ir.ghadmino.stepcounter.profile.ProfileRepository
import ir.ghadmino.stepcounter.step.StepCounterService
import ir.ghadmino.stepcounter.step.StepHistory
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class StepPlanState(val target: Int, val startDate: String)
data class StepPlanProgress(
    val day: Int,
    val completedDays: Int,
    val elapsedDays: Int,
    val remainingDays: Int,
    val averageSteps: Int
)

object StepPlanRepository {
    private const val PREFS = "ghadmino_step_plan"
    private const val TARGET = "target"
    private const val START = "start"

    fun load(context: android.content.Context, fallback: Int): StepPlanState {
        val p = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        return StepPlanState(
            p.getInt(TARGET, fallback.coerceIn(3000, 20000)),
            p.getString(START, today()) ?: today()
        )
    }

    fun save(context: android.content.Context, target: Int) {
        val p = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        val old = p.getInt(TARGET, 0)
        val existingStart = p.getString(START, null)
        val start = if (old == target && !existingStart.isNullOrBlank()) existingStart else today()
        p.edit().putInt(TARGET, target.coerceIn(3000, 20000)).putString(START, start).apply()
    }

    fun restart(context: android.content.Context, target: Int) {
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).edit()
            .putInt(TARGET, target.coerceIn(3000, 20000))
            .putString(START, today())
            .apply()
    }

    fun progress(context: android.content.Context): StepPlanProgress {
        val start = load(context, 8000).startDate
        val elapsed = elapsedDays(start)
        var completed = 0
        var total = 0L
        for (i in 0 until elapsed) {
            val date = dateOffsetFromStart(start, i) ?: continue
            val steps = StepHistory.get(context, date)
            total += steps.toLong()
            if (steps >= targetForDay(context, i + 1)) completed++
        }
        return StepPlanProgress(
            day = elapsed,
            completedDays = completed,
            elapsedDays = elapsed,
            remainingDays = (30 - elapsed).coerceAtLeast(0),
            averageSteps = if (elapsed > 0) (total / elapsed).toInt() else 0
        )
    }

    fun dayNumber(context: android.content.Context): Int = progress(context).day

    fun todayTarget(context: android.content.Context): Int =
        targetForDay(context, progress(context).day)

    private fun targetForDay(context: android.content.Context, day: Int): Int {
        val finalTarget = load(context, 8000).target
        val ratio = ((day.coerceIn(1, 30) - 1) / 29f)
        return (finalTarget * (0.75f + ratio * 0.25f)).roundToInt()
            .coerceIn(3000, finalTarget)
    }

    private fun elapsedDays(start: String): Int = try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startDate = format.parse(start) ?: return 1
        val begin = Calendar.getInstance().apply {
            time = startDate
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        (((now.timeInMillis - begin.timeInMillis) / 86400000L).toInt() + 1).coerceIn(1, 30)
    } catch (_: Exception) { 1 }

    private fun dateOffsetFromStart(start: String, offset: Int): String? = try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = format.parse(start) ?: return null
        Calendar.getInstance().apply {
            time = date
            add(Calendar.DAY_OF_YEAR, offset)
        }.let { format.format(it.time) }
    } catch (_: Exception) { null }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
}

@Composable
fun StepPlanScreen() {
    val context = LocalContext.current
    val profile = remember { ProfileRepository.load(context) }
    var state by remember { mutableStateOf(StepPlanRepository.load(context, profile.dailyGoal)) }
    var target by remember { mutableIntStateOf(state.target) }
    var refresh by remember { mutableIntStateOf(0) }

    val current = StepCounterService.todaySteps
    val planProgress = remember(refresh, current, state) { StepPlanRepository.progress(context) }
    val todayTarget = StepPlanRepository.todayTarget(context)
    val progress = (current.toFloat() / todayTarget).coerceIn(0f, 1f)
    val recent = StepHistory.recent(context, 7)
    val average = if (recent.isEmpty()) 0 else recent.sumOf { it.second } / recent.size

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("برنامه ۳۰ روزه افزایش قدم", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("روز " + planProgress.day + " از ۳۰")
                Text("شروع برنامه: " + state.startDate)
                Text("هدف نهایی: " + target + " قدم در روز")
                Text("هدف امروز: " + todayTarget + " قدم")
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = progress, Modifier.fillMaxWidth())
                Text(current.toString() + " از " + todayTarget + " قدم")
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("وضعیت برنامه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("روزهای موفق: " + planProgress.completedDays + " از " + planProgress.elapsedDays)
                Text("روزهای باقی‌مانده: " + planProgress.remainingDays)
                Text("میانگین قدم در روزهای برنامه: " + planProgress.averageSteps)
                LinearProgressIndicator(
                    progress = (planProgress.completedDays / 30f).coerceIn(0f, 1f),
                    Modifier.fillMaxWidth()
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("تنظیم هدف نهایی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Slider(
                    value = target.toFloat(),
                    onValueChange = { target = it.roundToInt() },
                    valueRange = 3000f..20000f,
                    steps = 16
                )
                Text("هدف نهایی: " + target + " قدم")
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        StepPlanRepository.save(context, target)
                        state = StepPlanRepository.load(context, target)
                        refresh++
                    },
                    Modifier.fillMaxWidth()
                ) { Text("ذخیره و ادامه برنامه") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        StepPlanRepository.restart(context, target)
                        state = StepPlanRepository.load(context, target)
                        refresh++
                    },
                    Modifier.fillMaxWidth()
                ) { Text("شروع دوباره از روز اول") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("پیشرفت هفتگی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                for (week in 1..4) {
                    val ratio = 0.75 + week * 0.0625
                    val weekTarget = (target * ratio).roundToInt().coerceAtMost(target)
                    Text("هفته " + week + ": حدود " + weekTarget + " قدم در روز", fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = (current.toFloat() / weekTarget).coerceIn(0f, 1f),
                        Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("میانگین ۷ روز اخیر", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(average.toString() + " قدم در روز", style = MaterialTheme.typography.headlineSmall)
                Text("هدف برنامه به‌تدریج افزایش پیدا می‌کند تا روند پیشرفت قابل پیگیری باشد.")
            }
        }

        Text(
            "این برنامه آموزشی و انگیزشی است و جایگزین توصیه پزشکی نیست.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
