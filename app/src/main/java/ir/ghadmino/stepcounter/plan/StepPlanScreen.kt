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
import kotlin.math.roundToInt

@Composable
fun StepPlanScreen() {
    val context = LocalContext.current
    val profile = remember { ProfileRepository.load(context) }
    var target by remember { mutableIntStateOf(profile.dailyGoal.coerceIn(3000, 20000)) }
    val current = StepCounterService.todaySteps
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("برنامه ۳۰ روزه افزایش قدم", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("هدف فعلی: $target قدم در روز")
                Spacer(Modifier.height(8.dp))
                Slider(value = target.toFloat(), onValueChange = { target = it.roundToInt() }, valueRange = 3000f..20000f, steps = 16)
                Text("هدف را تدریجی بالا ببر و روندت را از آمار برنامه پیگیری کن.")
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (week in 1..4) {
                    val ratio = 0.75 + week * 0.0625
                    val weekTarget = (target * ratio).roundToInt().coerceAtMost(target)
                    Text("هفته $week: حدود $weekTarget قدم در روز", fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(progress = (current.toFloat() / weekTarget).coerceIn(0f, 1f))
                }
            }
        }
        Text("این برنامه آموزشی و انگیزشی است و جایگزین توصیه پزشکی نیست.", style = MaterialTheme.typography.bodySmall)
    }
}
