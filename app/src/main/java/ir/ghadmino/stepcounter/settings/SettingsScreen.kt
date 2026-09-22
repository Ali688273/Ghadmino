package ir.ghadmino.stepcounter.settings
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
@Composable fun SettingsScreen(){
 val c=LocalContext.current;var s by remember{mutableStateOf(SettingsRepository.load(c))}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Notifications,null);Spacer(Modifier.width(8.dp));Text("یادآوری هوشمند",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)};Spacer(Modifier.height(6.dp));Text("اگر در ساعت انتخاب‌شده هنوز به هدف روزانه نرسیده باشی، قدمینو یادآوری می‌کند.");Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("فعال");Switch(checked=s.remindersEnabled,onCheckedChange={s=s.copy(remindersEnabled=it);SettingsRepository.save(c,s);ReminderScheduler.schedule(c)})}}}
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){Text("زمان یادآوری",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text(String.format("%02d:%02d",s.reminderHour,s.reminderMinute));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(18 to 0,20 to 0,21 to 30).forEach{(h,m)->OutlinedButton(onClick={s=s.copy(reminderHour=h,reminderMinute=m);SettingsRepository.save(c,s);ReminderScheduler.schedule(c)}){Text(String.format("%02d:%02d",h,m))}}}}}
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.BatteryAlert,null);Spacer(Modifier.width(8.dp));Text("دقت و مصرف باتری",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)};Spacer(Modifier.height(8.dp));Text("برای شمارش پایدار در پس‌زمینه، دسترسی فعالیت بدنی و اجرای سرویس قدم‌شمار را فعال نگه دار.");Text("اگر گوشی شمارش را متوقف می‌کند، محدودیت Battery Saver یا بهینه‌سازی باتری برای قدمینو را بررسی کن.")}}
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Speed,null);Spacer(Modifier.width(8.dp));Text("دقت سرعت",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)};Text("سرعت بر اساس داده‌های حرکتی و موقعیت محاسبه می‌شود و ممکن است در سکون یا فضای بسته نوسان داشته باشد.")}}
 }
}