package ir.ghadmino.stepcounter.stats

import android.content.Context
import android.content.Intent
import ir.ghadmino.stepcounter.analytics.ActivityAnalyticsRepository
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import ir.ghadmino.stepcounter.ads.GhadminoBanner
import ir.ghadmino.stepcounter.step.StepHistory
import ir.ghadmino.stepcounter.settings.SettingsRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

private data class Point(val label: String, val value: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(stats: GhadminoStats, goal: Int) {
    val context = LocalContext.current
    val useSolarCalendar = SettingsRepository.load(context).useSolarCalendar
    var period by remember { mutableIntStateOf(7) }
    var selectedMonthOffset by remember { mutableIntStateOf(0) }
    var type by remember { mutableStateOf("line") }
    var trendExpanded by remember { mutableStateOf(false) }
    var recordsExpanded by remember { mutableStateOf(false) }
    var recentExpanded by remember { mutableStateOf(false) }
    val data = remember(period, useSolarCalendar, selectedMonthOffset) {
        chartData(context, period, useSolarCalendar, selectedMonthOffset)
    }
    val report = remember(period, goal, data) {
        if (period == 30) {
            val total = data.sumOf { it.value }
            val successfulDays = data.count { it.value >= goal }
            PeriodReport(
                days = data.size,
                totalSteps = total,
                averageSteps = if (data.isEmpty()) 0 else total / data.size,
                bestDay = "" to (data.maxOfOrNull { it.value } ?: 0),
                goalDays = successfulDays,
                goalRate = if (data.isEmpty()) 0 else successfulDays * 100 / data.size
            )
        } else StatsRepository.period(context, goal, period)
    }

    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(14.dp)
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("آمار و گزارش فعالیت", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("گزارش هفتگی، ماهانه و سالانه با نمودار قابل انتخاب.")
                    }
                }
            }
            item { GhadminoBanner(Modifier.fillMaxWidth()) }
            item { PeriodSelector(period) { period = it } }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Metric(Modifier.weight(1f), "مجموع", report.totalSteps.toString(), "قدم")
                    Metric(Modifier.weight(1f), "میانگین", report.averageSteps.toString(), "قدم/روز")
                    Metric(Modifier.weight(1f), "موفق", report.goalDays.toString(), "روز")
                }
            }
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BarChart, null)
                            Spacer(Modifier.width(8.dp))
                            Text("نمودار فعالیت", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        if (period == 30) {
                            Spacer(Modifier.height(10.dp))
                            MonthNavigator(
                                monthOffset = selectedMonthOffset,
                                useSolarCalendar = useSolarCalendar,
                                onPrevious = { selectedMonthOffset = (selectedMonthOffset - 1).coerceAtLeast(-120) },
                                onNext = { selectedMonthOffset = (selectedMonthOffset + 1).coerceAtMost(0) }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = type == "line", onClick = { type = "line" }, label = { Text("خطی") })
                            FilterChip(selected = type == "bar", onClick = { type = "bar" }, label = { Text("میله‌ای") })
                        }
                        Spacer(Modifier.height(10.dp))
                        ActivityChart(data, type == "line", goal, period == 365)
                    }
                }
            }
            item {
                ExpandableReportCard(
                    title = "روند و پیشرفت",
                    subtitle = "مقایسه عملکرد و هدف روزانه",
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    expanded = trendExpanded,
                    onToggle = { trendExpanded = !trendExpanded },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    val trend = StatsRepository.trend(context, goal)
                    Text(
                        when {
                            trend > 0 -> "این هفته نسبت به هفته قبل " + trend + "٪ بیشتر بوده است."
                            trend < 0 -> "این هفته نسبت به هفته قبل " + (-trend) + "٪ کمتر بوده است."
                            else -> "تغییر محسوسی نسبت به هفته قبل ثبت نشده است."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DetailPill(Modifier.weight(1f), "هدف روزانه", goal.toString() + " قدم")
                        DetailPill(Modifier.weight(1f), "زنجیره فعلی", stats.streak.toString() + " روز")
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            item {
                ExpandableReportCard(
                    title = "رکوردها",
                    subtitle = "بهترین عملکردهای ثبت‌شده",
                    icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    expanded = recordsExpanded,
                    onToggle = { recordsExpanded = !recordsExpanded }
                ) {
                    DetailPill(
                        Modifier.fillMaxWidth(),
                        "بهترین روز",
                        StatsRepository.label(stats.bestDay.first, useSolarCalendar) + " — " + stats.bestDay.second + " قدم"
                    )
                    Spacer(Modifier.height(8.dp))
                    DetailPill(
                        Modifier.fillMaxWidth(),
                        "رکورد زنجیره",
                        stats.longestStreak.value.toString() + " روز"
                    )
                    Spacer(Modifier.height(8.dp))
                    DetailPill(
                        Modifier.fillMaxWidth(),
                        "بازه ثبت رکورد زنجیره",
                        if (stats.longestStreak.value > 0) {
                            StatsRepository.label(stats.longestStreak.startDate, useSolarCalendar) + " تا " +
                                StatsRepository.label(stats.longestStreak.endDate, useSolarCalendar)
                        } else {
                            "هنوز رکوردی ثبت نشده است"
                        }
                    )
                }
            }
            item {
                ExpandableReportCard(
                    title = "۷ روز اخیر",
                    subtitle = "مشاهده قدم‌های هر روز",
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    expanded = recentExpanded,
                    onToggle = { recentExpanded = !recentExpanded }
                ) {
                    val recent = StepHistory.recent(context, 7)
                    recent.forEachIndexed { index, row ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(StatsRepository.label(row.first, useSolarCalendar), style = MaterialTheme.typography.bodyMedium)
                            Text(row.second.toString() + " قدم", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        if (index != recent.lastIndex) Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Route, null)
                        Spacer(Modifier.width(8.dp))
                        Text("مسافت و کالری تخمینی‌اند و بر اساس پروفایل و فعالیت ثبت‌شده محاسبه می‌شوند.")
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, ActivityAnalyticsRepository.shareText(context))
                        }
                        context.startActivity(Intent.createChooser(intent, "اشتراک گزارش"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("اشتراک گزارش فعالیت")
                }
            }
        }
    }
}

@Composable
private fun ExpandableReportCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    expanded: Boolean,
    onToggle: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { icon() }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "بستن جزئیات" else "نمایش جزئیات",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    Modifier.fillMaxWidth().padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun DetailPill(modifier: Modifier, label: String, value: String) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSelector(current: Int, onSelected: (Int) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("بازه گزارش", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = current == 7, onClick = { onSelected(7) }, label = { Text("هفتگی") })
                FilterChip(selected = current == 30, onClick = { onSelected(30) }, label = { Text("ماهانه") })
                FilterChip(selected = current == 365, onClick = { onSelected(365) }, label = { Text("سالانه") })
            }
        }
    }
}

@Composable
private fun MonthNavigator(
    monthOffset: Int,
    useSolarCalendar: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val monthCalendar = Calendar.getInstance(
        if (useSolarCalendar) Locale.forLanguageTag("fa-IR-u-ca-persian") else Locale.US
    ).apply {
        add(Calendar.MONTH, monthOffset)
    }
    val monthTitle = if (useSolarCalendar) {
        val monthNames = listOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
        )
        monthNames[monthCalendar.get(Calendar.MONTH)] + " " +
            toPersianDigits(monthCalendar.get(Calendar.YEAR).toString())
    } else {
        SimpleDateFormat("MMMM yyyy", Locale.US).format(monthCalendar.time)
    }

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onPrevious, enabled = monthOffset > -120) {
            Text("ماه قبل")
        }
        Text(monthTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TextButton(onClick = onNext, enabled = monthOffset < 0) {
            Text("ماه بعد")
        }
    }
}

@Composable
private fun ActivityChart(data: List<Point>, line: Boolean, goal: Int, annual: Boolean) {
    if (data.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
            Text("هنوز داده‌ای ثبت نشده است.")
        }
        return
    }

    val maxValue = max(1, max(data.maxOfOrNull { it.value } ?: 0, goal))
    val scheme = MaterialTheme.colorScheme
    val outlineColor = scheme.outline.copy(alpha = 0.16f)
    val goalColor = scheme.error.copy(alpha = 0.55f)
    // رنگ پایه آرام و ملایم؛ سبز برای بهترین روز و قرمز برای کمترین روز
    val chartColor = Color(0xFF5B9B95)
    val bestColor = Color(0xFF2E9B65)
    val lowestColor = Color(0xFFD66B6B)
    val highlightExtremes = !annual && data.size > 1 &&
        (data.maxOfOrNull { it.value } ?: 0) != (data.minOfOrNull { it.value } ?: 0)
    val bestIndex = if (highlightExtremes) data.indices.maxByOrNull { data[it].value } else null
    val lowestIndex = if (highlightExtremes) data.indices.minByOrNull { data[it].value } else null

    Column {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // مقیاس عددی محور عمودی؛ پنج مقدار دقیقاً متناظر با خطوط راهنمای نمودار
            Column(
                Modifier.width(58.dp).height(270.dp).padding(vertical = 5.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                // در نمودار سالانه فقط پنج درجه‌بندی متناسب با مجموع ماه‌ها نمایش داده می‌شود.
                if (annual) {
                    for (step in 4 downTo 0) {
                        val tickValue = maxValue * step / 4
                        Text(
                            text = formatAnnualChartNumber(tickValue),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                } else {
                    val tickCount = (maxValue / 3000).coerceAtLeast(1)
                    for (step in tickCount downTo 0) {
                        Text(
                            text = formatChartNumber(step * 3000),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
            Canvas(Modifier.weight(1f).height(270.dp)) {
                val left = 4f
                val right = size.width - 6f
                val top = 12f
                val bottom = size.height - 18f
                val w = right - left
                val h = bottom - top

                for (i in 0..4) {
                    val y = top + h * i / 4f
                    drawLine(outlineColor, Offset(left, y), Offset(right, y), 1f)
                }

                val gy = bottom - h * (goal.toFloat() / maxValue).coerceIn(0f, 1f)
                drawLine(goalColor, Offset(left, gy), Offset(right, gy), 2f)

                if (line) {
                    val points = data.mapIndexed { index, point ->
                        val x = if (data.size == 1) left else left + w * index / (data.size - 1)
                        val y = bottom - h * point.value.toFloat() / maxValue
                        Offset(x, y)
                    }
                    val curve = Path()
                    if (points.isNotEmpty()) {
                        curve.moveTo(points.first().x, points.first().y)
                        for (index in 0 until points.lastIndex) {
                            val start = points[index]
                            val end = points[index + 1]
                            val previous = points[(index - 1).coerceAtLeast(0)]
                            val next = points[(index + 2).coerceAtMost(points.lastIndex)]
                            val dx = end.x - start.x
                            val minY = minOf(start.y, end.y)
                            val maxY = maxOf(start.y, end.y)
                            val control1 = Offset(
                                start.x + dx / 3f,
                                (start.y + (end.y - previous.y) / 6f).coerceIn(minY, maxY)
                            )
                            val control2 = Offset(
                                end.x - dx / 3f,
                                (end.y - (next.y - start.y) / 6f).coerceIn(minY, maxY)
                            )
                            curve.cubicTo(control1.x, control1.y, control2.x, control2.y, end.x, end.y)
                        }
                        drawPath(curve, chartColor, style = Stroke(width = 5f, cap = StrokeCap.Round))
                        points.forEachIndexed { index, point ->
                            val pointColor = when (index) {
                                bestIndex -> bestColor
                                lowestIndex -> lowestColor
                                else -> chartColor
                            }
                            drawCircle(pointColor, if (index == bestIndex || index == lowestIndex) 7f else 5f, point)
                        }
                    }
                } else {
                    val slotWidth = w / data.size
                    val bw = (slotWidth * 0.62f).coerceIn(3f, 30f)
                    data.forEachIndexed { index, point ->
                        val x = left + index * slotWidth + (slotWidth - bw) / 2f
                        val bh = h * point.value.toFloat() / maxValue
                        val barColor = when (index) {
                            bestIndex -> bestColor
                            lowestIndex -> lowestColor
                            else -> chartColor
                        }
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, bottom - bh),
                            size = androidx.compose.ui.geometry.Size(bw, bh),
                            cornerRadius = CornerRadius(bw / 2f, bw / 2f)
                        )
                    }
                }
            }
        }
        // برچسب‌های محور افقی (تاریخ هر روز یا ماه)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(end = 58.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                data.forEach {
                    Text(it.label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
        }
    }
}

private fun chartData(
    context: Context,
    period: Int,
    useSolarCalendar: Boolean,
    monthOffset: Int = 0
): List<Point> {
    val key = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    if (period == 30) {
        val monthCalendar = Calendar.getInstance(
            if (useSolarCalendar) Locale.forLanguageTag("fa-IR-u-ca-persian") else Locale.US
        ).apply {
            add(Calendar.MONTH, monthOffset)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val dayCount = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        // روز جاری را پیش از تنظیم تقویم روی روز اول ماه می‌خوانیم.
        val currentDay = Calendar.getInstance(
            if (useSolarCalendar) Locale.forLanguageTag("fa-IR-u-ca-persian") else Locale.US
        ).get(Calendar.DAY_OF_MONTH)
        // در ماه جاری روزهای آینده وارد محاسبه کمترین مقدار نشوند.
        val visibleDayCount = if (monthOffset == 0) minOf(dayCount, currentDay) else dayCount
        return (1..visibleDayCount).map { day ->
            val dateCalendar = (monthCalendar.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, day)
            }
            val date = dateCalendar.time
            // برچسب‌های ماهانه هم‌قالب هفتگی‌اند؛ برای جلوگیری از شلوغی فقط هر پنج روز و روز پایانی نمایش داده می‌شود.
            val showLabel = day == 1 || (day - 1) % 5 == 0 || day == dayCount
            val labelCalendar = Calendar.getInstance().apply { time = date }
            val label = if (!showLabel) {
                ""
            } else if (useSolarCalendar) {
                jalaliDayLabel(labelCalendar)
            } else {
                SimpleDateFormat("MM/dd", Locale.US).format(date)
            }
            Point(label, StepHistory.get(context, key.format(date)))
        }
    }

    val rows = StepHistory.recent(context, period).associate { it.first to it.second }
    return if (period < 365) {
        (period - 1 downTo 0).map { offset ->
            val date = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }.time
            val calendar = Calendar.getInstance().apply { time = date }
            val label = if (useSolarCalendar) jalaliDayLabel(calendar)
                else SimpleDateFormat("MM/dd", Locale.US).format(date)
            Point(label, rows[key.format(date)] ?: 0)
        }
    } else {
        val key = SimpleDateFormat("yyyy-MM", Locale.US)
        val label = SimpleDateFormat("MM", Locale.US)
        (11 downTo 0).map { offset ->
            val date = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, -offset)
            }.time
            val prefix = key.format(date)
            val calendar = Calendar.getInstance().apply { time = date }
            val label = if (useSolarCalendar) jalaliMonthLabel(calendar)
                else SimpleDateFormat("yyyy/MM", Locale.US).format(date)
            Point(label, rows.filterKeys { it.startsWith(prefix) }.values.sum())
        }
    }
}


private fun jalaliDayLabel(calendar: Calendar): String {
    val (_, month, day) = gregorianToJalali(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
    return toPersianDigits(day.toString() + "/" + month)
}

private fun jalaliDayNumber(calendar: Calendar): String {
    val (_, _, day) = gregorianToJalali(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
    return toPersianDigits(day.toString().padStart(2, '0'))
}

private fun jalaliMonthLabel(calendar: Calendar): String {
    val (year, month, _) = gregorianToJalali(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
    return toPersianDigits(year.toString() + "/" + month)
}

private fun toPersianDigits(value: String): String = value.map {
    when (it) {
        '0' -> '۰'; '1' -> '۱'; '2' -> '۲'; '3' -> '۳'; '4' -> '۴'
        '5' -> '۵'; '6' -> '۶'; '7' -> '۷'; '8' -> '۸'; '9' -> '۹'
        else -> it
    }
}.joinToString("")

private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
    val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    val gy2 = if (gm > 2) gy + 1 else gy
    var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gdm[gm - 1]
    var jy = -1595 + 33 * (days / 12053)
    days %= 12053
    jy += 4 * (days / 1461)
    days %= 1461
    if (days > 365) { jy += (days - 1) / 365; days = (days - 1) % 365 }
    val jm = if (days < 186) 1 + days / 31 else 7 + (days - 186) / 30
    val jd = 1 + if (days < 186) days % 31 else (days - 186) % 30
    return Triple(jy, jm, jd)
}
private fun formatChartNumber(value: Int): String =
    java.text.NumberFormat.getIntegerInstance(Locale.US).format(value)

private fun formatAnnualChartNumber(value: Int): String {
    val compact = when {
        value >= 1_000_000 -> {
            val amount = value / 1_000_000.0
            String.format(Locale.US, if (amount % 1.0 == 0.0) "%.0f" else "%.1f", amount) + " میلیون"
        }
        value >= 1_000 -> String.format(Locale.US, "%.0f", value / 1_000.0) + " هزار"
        else -> value.toString()
    }
    return toPersianDigits(compact)
}

@Composable
private fun Metric(modifier: Modifier, title: String, value: String, unit: String) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(unit, style = MaterialTheme.typography.labelSmall)
        }
    }
}
