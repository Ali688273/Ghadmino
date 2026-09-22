package ir.ghadmino.stepcounter.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun ActivityInsightsScreen(insights: ActivityInsights, hourly: List<Int>) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Analytics, null)
                        Spacer(Modifier.width(8.dp))
                        Text("امتیاز فعالیت امروز", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("\${insights.score} از ۱۰۰", style = MaterialTheme.typography.displaySmall)
                    Text(insights.message)
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(progress = { insights.score / 100f }, Modifier.fillMaxWidth())
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric(Modifier.weight(1f), "ساعت فعال", insights.activeHours.toString(), "ساعت")
                Metric(Modifier.weight(1f), "پیشرفت هدف", insights.goalProgress.toString(), "٪")
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, null)
                        Spacer(Modifier.width(8.dp))
                        Text("فعالیت ساعتی", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(12.dp))
                    val maxValue = max(1, hourly.maxOrNull() ?: 1)
                    hourly.forEachIndexed { hour, value ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(ActivityInsightsRepository.formatHour(hour), Modifier.width(52.dp))
                            Box(
                                Modifier.weight(1f).height(14.dp).background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.shapes.small
                                )
                            ) {
                                if (value > 0) {
                                    Box(
                                        Modifier.fillMaxWidth(value / maxValue.toFloat()).fillMaxHeight()
                                            .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                                    )
                                }
                            }
                            Text(value.toString(), Modifier.width(52.dp), textAlign = TextAlign.End)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("اوج فعالیت: \${ActivityInsightsRepository.formatHour(insights.peakHour)} — \${insights.peakSteps} قدم")
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null)
                        Spacer(Modifier.width(8.dp))
                        Text("پیش‌بینی رسیدن به هدف", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (insights.etaMinutes != null) {
                        Text("با سرعت فعلی، حدود \${insights.etaMinutes} دقیقه دیگر تا هدف باقی می‌ماند.")
                    } else {
                        Text("برای پیش‌بینی دقیق‌تر، ابتدا کمی فعالیت ثبت شود.")
                    }
                    Text("این زمان تقریبی است و با تغییر سرعت یا توقف، تغییر می‌کند.", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, null)
                        Spacer(Modifier.width(8.dp))
                        Text("رکوردهای هوشمند", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("بهترین روز در ۷ روز اخیر: \${insights.bestDaySteps} قدم")
                    Text("زنجیره فعلی رسیدن به هدف: \${insights.bestStreak} روز")
                }
            }
        }
    }
}

@Composable
private fun Metric(modifier: Modifier, title: String, value: String, unit: String) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(unit, style = MaterialTheme.typography.labelSmall)
        }
    }
}