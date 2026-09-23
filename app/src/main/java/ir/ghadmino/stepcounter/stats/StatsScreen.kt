package ir.ghadmino.stepcounter.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import ir.ghadmino.stepcounter.speed.SpeedTracker
import ir.ghadmino.stepcounter.speed.SpeedTracker
import kotlin.math.max

@Composable
fun StatsScreen(stats: GhadminoStats, goal: Int) {
    val context = LocalContext.current
    val speed = SpeedTracker(context)
    val week = stats.days.take(7)
    val weekTotal = week.sumOf { it.second }
    val weekAverage = if (week.isEmpty()) 0 else weekTotal / week.size
    val previousWeek = stats.days.drop(7).take(7).sumOf { it.second }
    val trend = when {
        weekTotal > previousWeek -> "روند این هفته صعودی است"
        weekTotal < previousWeek -> "روند این هفته نزولی است"
        else -> "روند این هفته ثابت است"
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric(Modifier.weight(1f), "مجموع ۳۰ روز", stats.total.toString(), "قدم")
                Metric(Modifier.weight(1f), "میانگین", stats.average.toString(), "قدم/روز")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric(Modifier.weight(1f), "رکورد", stats.bestDay.second.toString(), "قدم")
                Metric(Modifier.weight(1f), "روز موفق", stats.goalDays.toString(), "روز")
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, null)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("گزارش هفتگی", style = MaterialTheme.typography.titleLarge)
                        Text(weekTotal.toString() + " قدم • میانگین " + weekAverage + " قدم در روز")
                        Text(trend, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, null)
                        Spacer(Modifier.width(8.dp))
                        Text("تحلیل روند", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("این هفته: " + weekTotal + " قدم")
                    Text("هفته قبل: " + previousWeek + " قدم")
                    Text("هدف روزانه: " + goal + " قدم")
                    Text("روزهای رسیدن به هدف: " + stats.goalDays)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("فعالیت‌های دستی", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text("قدم دستی: " + stats.manualSteps)
                    Text("زمان دستی: " + stats.manualMinutes + " دقیقه")
                    Text("کالری دستی: " + stats.manualCalories + " kcal")
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalFireDepartment, null)
                    Spacer(Modifier.width(8.dp))
                    Text("زنجیره: " + stats.streak + " روز پشت‌سرهم", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BarChart, null)
                        Spacer(Modifier.width(8.dp))
                        Text("نمودار ۱۴ روز اخیر", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(12.dp))
                    val chart = stats.days.take(14).reversed()
                    val maxValue = max(1, chart.maxOfOrNull { it.second } ?: 1)
                    Row(
                        Modifier.fillMaxWidth().height(170.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        chart.forEach { item ->
                            Column(
                                Modifier.weight(1f).fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Text(
                                    if (item.second >= 1000) (item.second / 1000).toString() + "k"
                                    else item.second.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(Modifier.height(3.dp))
                                Box(
                                    Modifier.fillMaxWidth().height(
                                        (135f * item.second / maxValue).coerceAtLeast(4f).dp
                                    ).background(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.shapes.small
                                    )
                                )
                                Text(StatsRepository.label(item.first), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("هدف روزانه: " + goal + " قدم", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("خلاصه سرعت امروز", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Metric(Modifier.weight(1f), "میانگین", String.format("%.1f", speed.averageSpeedKmh), "km/h")
                        Metric(Modifier.weight(1f), "کمینه", String.format("%.1f", speed.minimumSpeedKmh), "km/h")
                        Metric(Modifier.weight(1f), "بیشینه", String.format("%.1f", speed.maximumSpeedKmh), "km/h")
                    }
                }
            }
        }
        item { SpeedChart() }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "بهترین روز: " + StatsRepository.label(stats.bestDay.first) +
                        " — " + stats.bestDay.second + " قدم"
                )
            }
        }
        item { Text("تاریخچه ۳۰ روزه", style = MaterialTheme.typography.titleLarge) }
        items(stats.days) { item ->
            ListItem(
                headlineContent = { Text(StatsRepository.label(item.first)) },
                supportingContent = {
                    Text(
                        if (item.second >= goal) "هدف تکمیل شد"
                        else "پیشرفت: " + item.second + " از " + goal
                    )
                },
                trailingContent = { Text(item.second.toString() + " قدم") }
            )
        }
    }
}

@Composable
private fun SpeedChart() {
    val context = LocalContext.current
    val points = SpeedHistoryRepository.recent(context, 7)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, null)
                Spacer(Modifier.width(8.dp))
                Text("نمودار سرعت", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(6.dp))
            Text("میانگین سرعت ثبت‌شده در روزهای اخیر (km/h)", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            if (points.isEmpty()) {
                Text("هنوز داده سرعت کافی ثبت نشده است.")
            } else {
                val maxSpeed = max(1f, points.maxOfOrNull { it.second } ?: 1f)
                Row(
                    Modifier.fillMaxWidth().height(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    points.forEach { item ->
                        Column(
                            Modifier.weight(1f).fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(String.format("%.1f", item.second), style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(3.dp))
                            Box(
                                Modifier.fillMaxWidth().height(
                                    (120f * item.second / maxSpeed).coerceAtLeast(4f).dp
                                ).background(MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.small)
                            )
                            Text(StatsRepository.label(item.first), style = MaterialTheme.typography.labelSmall)
                        }
                    }
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
