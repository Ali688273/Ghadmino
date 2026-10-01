package ir.ghadmino.stepcounter.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.FrameLayout
import ir.ghadmino.stepcounter.ads.GhadminoAdsManager
import ir.ghadmino.stepcounter.step.StepHistory
import kotlin.math.max

@Composable
fun StatsScreen(stats: GhadminoStats, goal: Int) {
    val context = LocalContext.current
    val days = remember { StepHistory.recent(context, 7) }
    val weekTotal = days.sumOf { it.second }
    val weekAverage = if (days.isEmpty()) 0 else weekTotal / days.size
    val goalDays = days.count { it.second >= goal }
    val bestDay = days.maxByOrNull { it.second } ?: ("" to 0)
    val previousWeek = remember { StepHistory.recent(context, 14).drop(7).sumOf { it.second } }
    val trendPercent = if (previousWeek == 0) {
        if (weekTotal > 0) 100 else 0
    } else {
        (((weekTotal - previousWeek) * 100f) / previousWeek).toInt()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        item {
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                factory = {
                    FrameLayout(it).also { container ->
                        GhadminoAdsManager.loadInstantBanner(context as android.app.Activity, container)
                    }
                }
            )
        }

        item {
            Text(
                "خلاصه فعالیت",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(Modifier.weight(1f), "۷ روز", weekTotal.toString(), "قدم")
                MetricCard(Modifier.weight(1f), "میانگین", weekAverage.toString(), "قدم/روز")
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(Modifier.weight(1f), "روزهای موفق", goalDays.toString(), "از " + days.size)
                MetricCard(Modifier.weight(1f), "رکورد این هفته", bestDay.second.toString(), "قدم")
            }
        }

        item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "روند فعالیت",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when {
                            trendPercent > 0 -> "این هفته نسبت به هفته قبل " + trendPercent + "٪ بیشتر"
                            trendPercent < 0 -> "این هفته نسبت به هفته قبل " + (-trendPercent) + "٪ کمتر"
                            else -> "تغییر محسوسی نسبت به هفته قبل ثبت نشده است"
                        }
                    )
                    Text("هدف روزانه: " + goal + " قدم")
                    Text("زنجیره فعلی: " + stats.streak + " روز")
                }
            }
        }

        item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "۷ روز اخیر",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    days.forEach { item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                StatsRepository.label(item.first),
                                modifier = Modifier.weight(1f)
                            )
                            LinearProgressIndicator(
                                progress = (item.second.toFloat() / max(1, goal)).coerceIn(0f, 1f),
                                modifier = Modifier
                                    .weight(1.6f)
                                    .height(7.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(item.second.toString())
                        }
                        if (item != days.last()) {
                            Divider()
                        }
                    }
                }
            }
        }

        item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "رکورد",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("بهترین روز: " + StatsRepository.label(bestDay.first) + " — " + bestDay.second + " قدم")
                    Text("زنجیره طولانی ثبت‌شده: " + stats.longestStreak.value + " روز")
                }
            }
        }

        item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BarChart, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "سرعت",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("برای جزئیات سرعت و تمرین، بخش «تمرین پیاده‌روی» را از «بیشتر» باز کن.")
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier,
    title: String,
    value: String,
    unit: String
) {
    OutlinedCard(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(unit, style = MaterialTheme.typography.labelSmall)
        }
    }
}
