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
import ir.ghadmino.stepcounter.notification.ReminderScheduler

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(SettingsRepository.load(context)) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "یادآوری هوشمند",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text("اگر در ساعت انتخاب‌شده هنوز به هدف روزانه نرسیده باشی، قدمینو یادآوری می‌کند.")
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("فعال")
                    Switch(
                        checked = settings.remindersEnabled,
                        onCheckedChange = {
                            settings = settings.copy(remindersEnabled = it)
                            SettingsRepository.save(context, settings)
                            ReminderScheduler.schedule(context)
                        }
                    )
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("زمان یادآوری", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(String.format("%02d:%02d", settings.reminderHour, settings.reminderMinute))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(18 to 0, 20 to 0, 21 to 30).forEach { (hour, minute) ->
                        OutlinedButton(
                            onClick = {
                                settings = settings.copy(reminderHour = hour, reminderMinute = minute)
                                SettingsRepository.save(context, settings)
                                ReminderScheduler.schedule(context)
                            }
                        ) {
                            Text(String.format("%02d:%02d", hour, minute))
                        }
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BatteryAlert, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "ثبت قدم در پس‌زمینه",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("قدمینو از سرویس فعال پس‌زمینه استفاده می‌کند تا بعد از بستن صفحه برنامه هم شمارش ادامه داشته باشد.")
                Spacer(Modifier.height(8.dp))
                Text("اگر گوشی شمارش را متوقف می‌کند، محدودیت Battery Saver یا بهینه‌سازی باتری را بررسی کن.")
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("باز کردن تنظیمات باتری")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "دقت سرعت",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text("سرعت با ترکیب داده GPS و فاصله بین موقعیت‌ها محاسبه و نرم می‌شود تا پرش‌های لحظه‌ای کمتر شوند.")
                Text("برای سرعت دقیق‌تر، GPS روشن و مجوز موقعیت مکانی فعال باشد.")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("نکته مهم", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("بعد از نصب، مجوز فعالیت بدنی و موقعیت مکانی باید حداقل یک‌بار توسط کاربر تأیید شود. پس از آن، سرویس می‌تواند در پس‌زمینه و بعد از راه‌اندازی مجدد گوشی ادامه دهد.")
                }
            }
        }
    }
}
