package ir.ghadmino.stepcounter.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun ActivityInsightsScreen(insights: ActivityInsights, hourly: List<Int>) {
    var hourlyExpanded by remember { mutableStateOf(false) }
    var predictionExpanded by remember { mutableStateOf(false) }
    var recordsExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Analytics, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(8.dp))
                        Text("امتیاز فعالیت امروز", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Text("${insights.score} از ۱۰۰", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text(insights.message, style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(
                        progress = insights.score / 100f,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric(Modifier.weight(1f), "ساعت فعال", insights.activeHours.toString(), "ساعت")
                Metric(Modifier.weight(1f), "پیشرفت هدف", insights.goalProgress.toString(), "٪")
            }
        }

        item {
            ExpandableInsightCard(
                title = "فعالیت ساعتی",
                subtitle = "اوج فعالیت: ${ActivityInsightsRepository.formatHour(insights.peakHour)} • ${insights.peakSteps} قدم",
                icon = { Icon(Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.primary) },
                expanded = hourlyExpanded,
                onToggle = { hourlyExpanded = !hourlyExpanded }
            ) {
                val maxValue = max(1, hourly.maxOrNull() ?: 1)
                hourly.forEachIndexed { hour, value ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(ActivityInsightsRepository.formatHour(hour), Modifier.width(54.dp), style = MaterialTheme.typography.labelMedium)
                        Box(
                            Modifier.weight(1f).height(13.dp).background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.shapes.small
                            )
                        ) {
                            if (value > 0) {
                                Box(
                                    Modifier.fillMaxWidth((value / maxValue.toFloat()).coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                                )
                            }
                        }
                        Text(value.toString(), Modifier.width(54.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Text(
                    "تعداد قدم‌های هر ساعت بر اساس داده‌های ثبت‌شده در همین گوشی",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            ExpandableInsightCard(
                title = "پیش‌بینی رسیدن به هدف",
                subtitle = if (insights.etaMinutes != null) "حدود ${insights.etaMinutes} دقیقه تا هدف" else "هنوز اطلاعات کافی برای پیش‌بینی نیست",
                icon = { Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.primary) },
                expanded = predictionExpanded,
                onToggle = { predictionExpanded = !predictionExpanded }
            ) {
                if (insights.etaMinutes != null) {
                    Text(
                        "با توجه به روند فعلی فعالیت، حدود ${insights.etaMinutes} دقیقه دیگر تا رسیدن به هدف روزانه باقی مانده است.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    Text("برای پیش‌بینی، ابتدا باید مقداری فعالیت در طول روز ثبت شود.")
                }
                Text(
                    "این زمان تخمینی است؛ اگر سرعت راه‌رفتن تغییر کند یا فعالیت متوقف شود، پیش‌بینی نیز تغییر می‌کند.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            ExpandableInsightCard(
                title = "رکوردهای فعالیت",
                subtitle = "بهترین روز و زنجیره رسیدن به هدف",
                icon = { Icon(Icons.Default.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary) },
                expanded = recordsExpanded,
                onToggle = { recordsExpanded = !recordsExpanded }
            ) {
                InsightValueRow("بهترین روز در ۷ روز اخیر", "${insights.bestDaySteps} قدم")
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                InsightValueRow("زنجیره فعلی رسیدن به هدف", "${insights.bestStreak} روز")
            }
        }

        item {
            Text(
                "امتیاز و پیش‌بینی‌ها تقریبی‌اند و فقط بر اساس داده‌های ثبت‌شده در برنامه محاسبه می‌شوند.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun ExpandableInsightCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) { icon() }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "بستن جزئیات" else "نمایش جزئیات",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    Modifier.fillMaxWidth().padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun InsightValueRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Metric(modifier: Modifier, title: String, value: String, unit: String) {
    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}
