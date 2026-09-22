package ir.ghadmino.stepcounter.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun StatsScreen(stats: GhadminoStats, goal: Int) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric(Modifier.weight(1f), "مجموع", stats.total.toString(), "قدم")
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
                    Row(Modifier.fillMaxWidth().height(170.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                        chart.forEach { item ->
                            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                Text(if (item.second >= 1000) (item.second / 1000).toString() + "k" else item.second.toString(), style = MaterialTheme.typography.labelSmall)
                                Spacer(Modifier.height(3.dp))
                                Box(Modifier.fillMaxWidth().height((135f * item.second / maxValue).coerceAtLeast(4f).dp).background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small))
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, null)
                Spacer(Modifier.width(8.dp))
                Text("بهترین روز: " + StatsRepository.label(stats.bestDay.first) + " — " + stats.bestDay.second + " قدم")
            }
        }
        item { Text("تاریخچه ۳۰ روزه", style = MaterialTheme.typography.titleLarge) }
        items(stats.days) { item ->
            ListItem(
                headlineContent = { Text(StatsRepository.label(item.first)) },
                supportingContent = { Text(if (item.second >= goal) "هدف تکمیل شد" else "پیشرفت: " + item.second + " از " + goal) },
                trailingContent = { Text(item.second.toString() + " قدم") }
            )
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
