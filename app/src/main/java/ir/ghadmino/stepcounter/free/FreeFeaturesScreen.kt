package ir.ghadmino.stepcounter.free

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.ghadmino.stepcounter.health.HealthConnectRepository
import ir.ghadmino.stepcounter.analytics.ActivityAnalyticsRepository
import ir.ghadmino.stepcounter.reward.CoinWallet
import ir.ghadmino.stepcounter.step.StepHistory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

@Composable
fun FreeFeaturesScreen(onCoinsChanged: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf(StepSourceRepository.get(context)) }
    var records by remember { mutableStateOf(PersonalRecordsRepository.calculate(context)) }
    var goals by remember { mutableStateOf(PeriodicGoalRepository.load(context)) }
    var weeklyTarget by remember { mutableIntStateOf(goals.weeklyTarget) }
    var monthlyTarget by remember { mutableIntStateOf(goals.monthlyTarget) }
    var healthSteps by remember { mutableStateOf<Long?>(null) }
    var phoneSteps by remember { mutableIntStateOf(ActivityAnalyticsRepository.today(context)) }
    var message by remember { mutableStateOf<String?>(null) }
    var diag by remember { mutableStateOf<Diagnostics?>(null) }
    var scenarios by remember { mutableStateOf<List<ScenarioResult>>(emptyList()) }
    var login by remember { mutableStateOf(DailyLoginRepository.state(context)) }
    var syncState by remember { mutableStateOf(HealthSyncRepository.load(context)) }
    var endReport by remember { mutableStateOf(EndOfDayReportRepository.today(context)) }
    var selectedHeatmapDay by remember { mutableStateOf<Pair<String,Int>?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let { value -> snackbar.showSnackbar(value); message = null }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(14.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("امکانات رایگان قدمینو", style = MaterialTheme.typography.headlineSmall)
            Text("محلی، بدون حساب کاربری و بدون API پولی.")

            Section("۱ و ۲ — منبع قدم و Health Connect") {
                Text("منبع فعلی: " + StepSourceRepository.label(source))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SourceButton("گوشی", StepSource.PHONE, source) { source = it; StepSourceRepository.set(context, it) }
                    SourceButton("Health Connect", StepSource.HEALTH_CONNECT, source) { source = it; StepSourceRepository.set(context, it) }
                    SourceButton("خودکار", StepSource.AUTO, source) { source = it; StepSourceRepository.set(context, it) }
                }
                Button(onClick = {
                    phoneSteps = ActivityAnalyticsRepository.today(context)
                    scope.launch { healthSteps = try { HealthConnectRepository.todaySteps(context) } catch (_: Exception) { null } }
                }) { Text("نمایش هر دو منبع") }
                Text("گوشی: " + phoneSteps + " قدم")
                if (healthSteps != null) Text("Health Connect: " + healthSteps + " قدم")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sync فعال")
                    Switch(checked = syncState.enabled, onCheckedChange = {
                        HealthSyncRepository.setEnabled(context, it)
                        syncState = HealthSyncRepository.load(context)
                    })
                }
                Text("آخرین Sync: " + HealthSyncRepository.lastSyncText(context))
                Button(enabled = syncState.enabled, onClick = {
                    scope.launch {
                        val local = StepHistory.get(context, todayKey())
                        val ok = try { HealthConnectRepository.writeTodaySteps(context, local.toLong()) } catch (_: Exception) { false }
                        if (ok) HealthSyncRepository.markSynced(context)
                        syncState = HealthSyncRepository.load(context)
                        message = if (ok) "همگام‌سازی امروز انجام شد." else "همگام‌سازی انجام نشد؛ مجوز Health Connect را بررسی کن."
                    }
                }) { Text("همگام‌سازی قدم امروز") }
                Button(onClick = {
                    scope.launch {
                        val count = try { HealthConnectRepository.syncRecentDays(context, StepHistory.recent(context, 30)) } catch (_: Exception) { 0 }
                        message = if (count > 0) count.toString() + " روز به Health Connect همگام شد." else "همگام‌سازی ۳۰ روزه انجام نشد."
                    }
                }) { Text("بازهمگام‌سازی ۳۰ روز اخیر") }
                Text("دو منبع با هم جمع نمی‌شوند تا دوباره‌شماری رخ ندهد.")
            }

            Section("۳ — نمای سالانه فعالیت") { AnnualHeatmap(context) { selectedHeatmapDay = it } ; selectedHeatmapDay?.let { Text("روز ${it.first}: ${it.second} قدم") } }

            Section("۴ — روند ۷، ۳۰ و ۹۰ روزه") {
                MiniTrend(context, 7)
                MiniTrend(context, 30)
                MiniTrend(context, 90)
            }

            Section("۵ — رکوردهای شخصی") {
                RecordRow("بهترین روز", records.bestDaySteps.toString() + " قدم — " + records.bestDayDate)
                RecordRow("بهترین ۷ روز", records.best7DayTotal.toString() + " قدم")
                RecordRow("بهترین ۳۰ روز", records.best30DayTotal.toString() + " قدم")
                RecordRow("زنجیره فعلی", records.currentStreak.toString() + " روز")
                RecordRow("طولانی‌ترین زنجیره", records.longestStreak.toString() + " روز")
                RecordRow("روزهای فعال ۳۰ روز", records.activeDays30.toString())
                RecordRow("بیشترین مسافت تمرین", String.format(Locale.US, "%.2f km", records.bestWorkoutDistanceMeters / 1000.0))
                RecordRow("بیشترین زمان تمرین", records.bestWorkoutMinutes.toString() + " دقیقه")
            }

            Section("۶ و ۷ — هدف هفتگی و ماهانه") {
                GoalRow("هفتگی", goals.weeklyProgress, weeklyTarget)
                Slider(value = weeklyTarget.toFloat(), onValueChange = { weeklyTarget = it.toInt() }, valueRange = 10000f..150000f, steps = 27)
                GoalRow("ماهانه", goals.monthlyProgress, monthlyTarget)
                Slider(value = monthlyTarget.toFloat(), onValueChange = { monthlyTarget = it.toInt() }, valueRange = 50000f..500000f, steps = 17)
                Button(onClick = {
                    PeriodicGoalRepository.save(context, weeklyTarget, monthlyTarget)
                    goals = PeriodicGoalRepository.load(context)
                    message = "اهداف هفتگی و ماهانه ذخیره شد."
                }) { Text("ذخیره اهداف") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        message = if (goals.weeklyProgress >= goals.weeklyTarget && PeriodicGoalRepository.claimWeekly(context)) {
                            onCoinsChanged(); "۵۰ سکه هدف هفتگی دریافت شد."
                        } else "هدف هفتگی کامل نشده یا جایزه قبلاً دریافت شده است."
                    }) { Text("جایزه هفتگی") }
                    Button(onClick = {
                        message = if (goals.monthlyProgress >= goals.monthlyTarget && PeriodicGoalRepository.claimMonthly(context)) {
                            onCoinsChanged(); "۱۵۰ سکه هدف ماهانه دریافت شد."
                        } else "هدف ماهانه کامل نشده یا جایزه قبلاً دریافت شده است."
                    }) { Text("جایزه ماهانه") }
                }
            }

            Section("۸ — پاداش ورود روزانه") {
                Text("زنجیره ورود: " + login.streak + " روز — پاداش امروز: " + login.reward + " سکه")
                Button(enabled = !login.claimed, onClick = {
                    login = DailyLoginRepository.claim(context)
                    if (login.claimed) { message = login.reward.toString() + " سکه دریافت شد."; onCoinsChanged() }
                }) { Text("دریافت پاداش") }
            }

            Section("۹ و ۱۰ — سطح، XP و مأموریت‌های محلی") {
                Text("سطح، XP، دستاوردها و مأموریت‌های قبلی برنامه حفظ شده‌اند.")
                Text("ماموریت‌های محلی: رسیدن به ۶۰٪ هدف، رسیدن به هدف، و ۲۰۰۰ قدم بیشتر از هدف.")
            }

            Section("۱۱ و ۱۲ — گزارش روزانه و هفتگی") {
                endReport = EndOfDayReportRepository.today(context)
                Text("امروز: " + endReport.steps + " قدم — " + endReport.progress + "% هدف")
                Text("مسافت تقریبی: " + String.format(Locale.US, "%.2f", endReport.distanceKm) + " km")
                Text("کالری تقریبی: " + endReport.calories.toInt() + " kcal")
                Text("فعالیت در ۷ روز: " + endReport.activeDays7 + " روز")
                Text("بهترین روز هفته: " + endReport.bestDay + " قدم")
                Text("۷ روز: " + endReport.weeklyTotal + " قدم")
                Text("گزارش‌ها محلی‌اند و اجرای مداوم پس‌زمینه برایشان انجام نمی‌شود.")
            }

            Section("۱۳ — شاخص تقریبی فعالیت") {
                val active = StepHistory.recent(context, 7).count { row -> row.second >= 3000 }
                Text("روزهای دارای فعالیت قابل‌توجه در ۷ روز: " + active)
                Text("این شاخص آماری است و تشخیص پزشکی یا تشخیص قطعی نوع فعالیت نیست.")
            }

            Section("۱۴ و ۱۵ — خروجی و اعتبارسنجی") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { FreeExportRepository.share(context, FreeExportRepository.csv(context), "text/csv") }) { Text("CSV") }
                    Button(onClick = { FreeExportRepository.share(context, FreeExportRepository.json(context), "application/json") }) { Text("JSON") }
                }
                Button(onClick = {
                    val json = FreeExportRepository.json(context)
                    message = if (json.contains("\"version\":1") && json.contains("\"days\":[")) "خروجی سالم است." else "خروجی نیاز به بررسی دارد."
                }) { Text("اعتبارسنجی خروجی") }
            }

            Section("۱۶ — ویجت") {
                Text("ویجت اصلی حفظ شده و ویجت خلاصه ۷ روزه نیز اضافه می‌شود.")
            }

            Section("۱۷ — روشن و تیره") {
                Text("تم برنامه از حالت روشن/تیره سیستم پشتیبانی می‌کند.")
            }

            Section("۱۸ — زبان") {
                Text("داده‌ها مستقل از زبان ذخیره می‌شوند. ترجمه کامل رابط فعلی در مرحله جداگانه انجام می‌شود تا متن‌های سخت‌کدشده ناقص نشوند.")
            }

            Section("۱۹ — عیب‌یابی") {
                Button(onClick = { scope.launch { diag = diagnostics(context) } }) { Text("اجرای عیب‌یابی") }
                diag?.let {
                    CheckRow("حسگر قدم", it.sensor)
                    CheckRow("مجوز فعالیت", it.activityPermission)
                    CheckRow("اعلان", it.notificationPermission)
                    CheckRow("مکان برای سرعت/تمرین", it.locationPermission)
                    Text("Health Connect: " + it.healthConnect)
                    Text("سرویس قدم‌شمار: " + it.serviceState)
                    Text("آخرین دریافت سنسور: " + DiagnosticsHistoryRepository.text(context,"sensor"))
                    Text("آخرین ذخیره قدم: " + DiagnosticsHistoryRepository.text(context,"save"))
                    Text("محدودیت باتری: بررسی تنظیمات بهینه‌سازی باتری گوشی توصیه می‌شود.")
                }
            }

            Section("۲۰ — کنترل پایداری") {
                Text("پاداش‌ها یک‌بار مصرف‌اند و خطاهای Health Connect در رابط کنترل می‌شوند.")
                Button(onClick = { scenarios = InternalScenarioTests.run(); message = "بررسی‌های داخلی اجرا شد." }) { Text("اجرای تست‌های داخلی") }
                scenarios.forEach { result -> Text(if(result.passed) "✓ " + result.name else "⚠ " + result.name) }
                Button(onClick = {
                    records = PersonalRecordsRepository.calculate(context)
                    goals = PeriodicGoalRepository.load(context)
                    message = "داده‌ها تازه‌سازی شد."
                }) { Text("تازه‌سازی امن") }
            }
        }
    }
}

@Composable private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
@Composable private fun SourceButton(label: String, value: StepSource, current: StepSource, onClick: (StepSource) -> Unit) {
    FilterChip(selected = current == value, onClick = { onClick(value) }, label = { Text(label) })
}
@Composable private fun RecordRow(a: String, b: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(a); Text(b) }
}
@Composable private fun GoalRow(label: String, current: Int, target: Int) {
    val p = if (target <= 0) 0f else (current.toFloat() / target).coerceIn(0f, 1f)
    Text(label + ": " + current + " / " + target)
    LinearProgressIndicator(progress = p, modifier = Modifier.fillMaxWidth())
}
@Composable private fun CheckRow(label: String, ok: Boolean) { Text(if (ok) "✓ " + label else "⚠ " + label) }
@Composable private fun MiniTrend(context: Context, days: Int) {
    val rows = StepHistory.recent(context, days).reversed()
    val maxValue = max(1, rows.maxOfOrNull { row -> row.second } ?: 1)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(days.toString() + " روز")
        rows.takeLast(minOf(rows.size, 30)).forEach { row ->
            val width = (row.second.toFloat() / maxValue * 260f).coerceIn(2f, 260f)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.first.takeLast(2), Modifier.width(26.dp))
                Box(Modifier.width(width.dp).height(8.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
                Spacer(Modifier.width(5.dp))
                Text(row.second.toString())
            }
        }
    }
}
@Composable private fun AnnualHeatmap(context: Context, onDayClick: (Pair<String,Int>) -> Unit) {
    val rows = StepHistory.recent(context, 365)
    val maxValue = max(1, rows.maxOfOrNull { row -> row.second } ?: 1)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        rows.take(365).reversed().forEach { row ->
            val level = (row.second.toFloat() / maxValue * 4f).toInt().coerceIn(0, 4)
            Box(
                Modifier.size(11.dp)
                    .clickable { onDayClick(row) }
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f + level * 0.2f), RoundedCornerShape(2.dp))
            )
        }
    }
}
private fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
private fun dayKey(): String = todayKey()
