package ir.ghadmino.stepcounter.plan

import androidx.compose.foundation.layout.*
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
        val start = if (old == target && p.getString(START, null) != null) {
            p.getString(START, today())!!
        } else {
            today()
        }
        p.edit().putInt(TARGET, target).putString(START, start).apply()
    }

    fun dayNumber(context: android.content.Context): Int {
        val start = load(context, 8000).startDate
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val startDate = format.parse(start) ?: return 1
            val now = Calendar.getInstance()
            val begin = Calendar.getInstance().apply { time = startDate }
            val days = ((now.timeInMillis - begin.timeInMillis) / 86400000L).toInt() + 1
            days.coerceIn(1, 30)
        } catch (_: Exception) {
            1
        }
    }

    fun todayTarget(context: android.content.Context): Int {
        val state = load(context, 8000)
        val day = dayNumber(context)
        val progress = (day - 1) / 29f
        return (state.target * (0.75f + progress * 0.25f)).roundToInt()
            .coerceIn(3000, state.target)
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
}

@Composable
fun StepPlanScreen() {
    val context = LocalContext.current
    val profile = remember { ProfileRepository.load(context) }
    var state by remember { mutableStateOf(StepPlanRepository.load(context, profile.dailyGoal)) }
    var target by remember { mutableIntStateOf(state.target) }
    val current = StepCounterService.todaySteps
    val day = StepPlanRepository.dayNumber(context)
    val todayTarget = StepPlanRepository.todayTarget(context)
    val progress = (current.toFloat() / todayTarget).coerceIn(0f, 1f)
    val recent = StepHistory.recent(context, 7)
    val average = if (recent.isEmpty()) 0 else recent.sumOf { it.second } / recent.size

    Column(
        Modifier.fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(18.dp)) {
                Text("برنامه ۳۰ روزه افزایش قدم", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("روز $day از ۳۰")
                Text("هدف نهایی: $target قدم در روز")
                Text("هدف امروز: $todayTarget قدم")
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(progress = progress, Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text(current.toString() + " از " + todayTarget + " قدم")
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
                Text("هدف نهایی: $target قدم")
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        StepPlanRepository.save(context, target)
                        state = StepPlanRepository.load(context, target)
                    },
                    Modifier.fillMaxWidth()
                ) { Text("ذخیره و شروع/ادامه برنامه") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("پیشرفت هفتگی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                for (week in 1..4) {
                    val ratio = 0.75 + week * 0.0625
                    val weekTarget = (target * ratio).roundToInt().coerceAtMost(target)
                    Text("هفته $week: حدود $weekTarget قدم در روز", fontWeight = FontWeight.Bold)
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
                Text("$average قدم در روز", style = MaterialTheme.typography.headlineSmall)
                Text("با افزایش تدریجی هدف، فشار برنامه کنترل‌شده‌تر می‌ماند.")
            }
        }

        Text("این برنامه آموزشی و انگیزشی است و جایگزین توصیه پزشکی نیست.",
            style = MaterialTheme.typography.bodySmall)
    }
}
