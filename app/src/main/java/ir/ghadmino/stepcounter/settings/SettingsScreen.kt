package ir.ghadmino.stepcounter.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.ghadmino.stepcounter.notification.InactivityScheduler
import ir.ghadmino.stepcounter.notification.ReminderScheduler
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onProfile: () -> Unit,
    onHealth: () -> Unit,
    onBackup: () -> Unit,
    dailyGoal: Int,
    onDailyGoalChanged: (Int) -> Unit,
    stepCounterAvailable: Boolean,
    selectedTheme: String,
    onThemeSelected: (String) -> Unit,
    fontScale: Float,
    onFontScaleChanged: (Float) -> Unit
) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(SettingsRepository.load(context)) }
    val themes = listOf(
        Triple("default", "سبز زمردی", 0xFF176B5B),
        Triple("ocean", "آبی اقیانوسی", 0xFF006C8E),
        Triple("sunset", "غروب گرم", 0xFF9C3D00),
        Triple("forest", "جنگلی", 0xFF356A3B),
        Triple("royal", "بنفش سلطنتی", 0xFF5D3B8C),
        Triple("rose", "رز صورتی", 0xFF9A405C)
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("تنظیمات قدمینو", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("اطلاعات، اتصال سلامت و ظاهر برنامه را از اینجا مدیریت کن.")
            }
        }

        Text("حساب و اطلاعات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        SettingActionCard("👤", "اطلاعات بدنی و هدف", "نام، سن، قد، وزن، طول گام و هدف روزانه", onProfile)
        SettingActionCard("❤️", "اتصال قدمینو به داده‌های سلامت", "مدیریت اتصال و خواندن اطلاعات از Health Connect", onHealth)
        SettingActionCard("💾", "پشتیبان‌گیری و بازیابی", "ذخیره یا برگرداندن اطلاعات قدمینو از فایل", onBackup)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("هدف روزانه", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("هدف فعلی: $dailyGoal قدم")
                Slider(
                    value = dailyGoal.toFloat(),
                    onValueChange = {
                        onDailyGoalChanged(((it / 1000f).roundToInt() * 1000).coerceIn(1000, 30000))
                    },
                    valueRange = 1000f..30000f,
                    steps = 28
                )
                Text("هدف را بین ۱۰۰۰ تا ۳۰٬۰۰۰ قدم تنظیم کن.", style = MaterialTheme.typography.bodySmall)
            }
        }

        Divider()
        Text("تقویم و تاریخ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("نوع تقویم", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "قالب تاریخ‌ها در تقویم فعالیت و گزارش‌های آماری با این انتخاب تغییر می‌کند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = settings.useSolarCalendar,
                        onClick = {
                            settings = settings.copy(useSolarCalendar = true)
                            SettingsRepository.save(context, settings)
                        },
                        label = { Text("خورشیدی (شمسی)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !settings.useSolarCalendar,
                        onClick = {
                            settings = settings.copy(useSolarCalendar = false)
                            SettingsRepository.save(context, settings)
                        },
                        label = { Text("میلادی") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    if (settings.useSolarCalendar) "تقویم فعلی: خورشیدی" else "تقویم فعلی: میلادی",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Divider()
        Text("رنگ و تم برنامه", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("با انتخاب هر تم، رنگ پس‌زمینه و کارت‌های رنگی برنامه هم تغییر می‌کند.")
        themes.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { item ->
                    val id = item.first
                    val color = androidx.compose.ui.graphics.Color(item.third)
                    Card(
                        Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedTheme == id) color.copy(alpha = 0.22f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (selectedTheme == id)
                            androidx.compose.foundation.BorderStroke(2.dp, color)
                        else null
                    ) {
                        TextButton(
                            onClick = { onThemeSelected(id) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    Modifier.size(24.dp),
                                    shape = MaterialTheme.shapes.small,
                                    color = color
                                ) {}
                                Spacer(Modifier.height(5.dp))
                                Text(item.second, color = MaterialTheme.colorScheme.onSurface)
                                if (selectedTheme == id) Text("انتخاب‌شده", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        Divider()
        Text("اندازه نوشته‌ها", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("اندازه متن‌های برنامه را مطابق سلیقه‌ات تنظیم کن.")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    when {
                        fontScale < 0.96f -> "کوچک"
                        fontScale > 1.08f -> "بزرگ"
                        else -> "معمولی"
                    } + " • " + (fontScale * 100).toInt() + "٪",
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = fontScale,
                    onValueChange = onFontScaleChanged,
                    valueRange = 0.85f..1.30f,
                    steps = 8
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("کوچک")
                    Text("معمولی")
                    Text("بزرگ")
                }
                TextButton(
                    onClick = { onFontScaleChanged(1f) },
                    modifier = Modifier.align(Alignment.End)
                ) { Text("بازگشت به اندازه پیش‌فرض") }
            }
        }

        Divider()
        Text("قدم‌شمار و عملکرد", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, null)
                    Spacer(Modifier.width(8.dp))
                    Text("وضعیت قدم‌شمار", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text(
                    if (stepCounterAvailable)
                        "✓ حسگر قدم‌شمار فعال است"
                    else
                        "⚠ حسگر قدم‌شمار روی این گوشی پیدا نشد"
                )
                Text("شمارش قدم‌ها در پس‌زمینه ادامه پیدا می‌کند.")
            }
        }

        Text("یادآوری‌ها و عملکرد", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null)
                    Spacer(Modifier.width(8.dp))
                    Text("یادآوری هدف", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text("اگر در ساعت انتخاب‌شده هنوز به هدف نرسیده باشی، یادآوری می‌شود.")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("فعال")
                    Switch(settings.remindersEnabled, {
                        settings = settings.copy(remindersEnabled = it)
                        SettingsRepository.save(context, settings)
                        ReminderScheduler.schedule(context)
                    })
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("زمان یادآوری", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(String.format("%02d:%02d", settings.reminderHour, settings.reminderMinute))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(18 to 0, 20 to 0, 21 to 30).forEach { (h, m) ->
                        OutlinedButton({
                            settings = settings.copy(reminderHour = h, reminderMinute = m)
                            SettingsRepository.save(context, settings)
                            ReminderScheduler.schedule(context)
                        }) { Text(String.format("%02d:%02d", h, m)) }
                    }
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("هشدار کم‌تحرکی", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("اگر برای مدت طولانی قدمی ثبت نشود و هنوز هدف روزانه کامل نشده باشد، یک یادآوری دریافت می‌کنی.")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("فعال")
                    Switch(settings.inactivityEnabled, {
                        settings = settings.copy(inactivityEnabled = it)
                        SettingsRepository.save(context, settings)
                        InactivityScheduler.schedule(context)
                    })
                }
                Text("زمان فعلی: ${settings.inactivityMinutes} دقیقه")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(60, 120, 180).forEach { m ->
                        OutlinedButton({
                            settings = settings.copy(inactivityMinutes = m)
                            SettingsRepository.save(context, settings)
                            InactivityScheduler.schedule(context)
                        }) { Text("$m دقیقه") }
                    }
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BatteryAlert, null)
                    Spacer(Modifier.width(8.dp))
                    Text("ثبت قدم در پس‌زمینه", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text("سرویس فعال پس‌زمینه برای ادامه شمارش بعد از بستن صفحه استفاده می‌شود.")
                Text("اگر گوشی شمارش را متوقف می‌کند، محدودیت Battery Saver یا بهینه‌سازی باتری را بررسی کن.")
                Button(onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    } catch (_: Exception) {
                        try { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (_: Exception) {}
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("باز کردن تنظیمات باتری") }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, null)
                    Spacer(Modifier.width(8.dp))
                    Text("دقت سرعت", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text("برای سرعت دقیق‌تر، GPS روشن و مجوز موقعیت مکانی فعال باشد.")
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("مجوزها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("مجوز فعالیت بدنی و در صورت استفاده از قابلیت سرعت، مجوز موقعیت مکانی باید توسط کاربر تأیید شود.")
                }
            }
        }
    }
}

@Composable
private fun SettingActionCard(
    emoji: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onClick) { Text("ورود") }
        }
    }
}
