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

@Composable
fun SettingsScreen(){
 val context=LocalContext.current
 var settings by remember{mutableStateOf(SettingsRepository.load(context))}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Notifications,null);Spacer(Modifier.width(8.dp));Text("یادآوری هدف",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
   Text("اگر در ساعت انتخاب‌شده هنوز به هدف نرسیده باشی، یادآوری می‌شود.")
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("فعال");Switch(settings.remindersEnabled,{settings=settings.copy(remindersEnabled=it);SettingsRepository.save(context,settings);ReminderScheduler.schedule(context)})}
  }}
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){
   Text("زمان یادآوری",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   Text(String.format("%02d:%02d",settings.reminderHour,settings.reminderMinute))
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(18 to 0,20 to 0,21 to 30).forEach{(h,m)->OutlinedButton({settings=settings.copy(reminderHour=h,reminderMinute=m);SettingsRepository.save(context,settings);ReminderScheduler.schedule(context)}){Text(String.format("%02d:%02d",h,m))}}}
  }}
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){
   Text("هشدار کم‌تحرکی",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
   Text("اگر برای مدت طولانی قدمی ثبت نشود و هنوز هدف روزانه کامل نشده باشد، یک یادآوری دریافت می‌کنی.")
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("فعال");Switch(settings.inactivityEnabled,{settings=settings.copy(inactivityEnabled=it);SettingsRepository.save(context,settings);InactivityScheduler.schedule(context)})}
   Text("زمان فعلی: "+settings.inactivityMinutes+" دقیقه")
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(60,120,180).forEach{m->OutlinedButton({settings=settings.copy(inactivityMinutes=m);SettingsRepository.save(context,settings);InactivityScheduler.schedule(context)}){Text(m.toString()+" دقیقه")}}}
  }}
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.BatteryAlert,null);Spacer(Modifier.width(8.dp));Text("ثبت قدم در پس‌زمینه",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
   Text("سرویس فعال پس‌زمینه برای ادامه شمارش بعد از بستن صفحه استفاده می‌شود.")
   Text("اگر گوشی شمارش را متوقف می‌کند، محدودیت Battery Saver یا بهینه‌سازی باتری را بررسی کن.")
   Button({context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))},Modifier.fillMaxWidth()){Text("باز کردن تنظیمات باتری")}
  }}
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Speed,null);Spacer(Modifier.width(8.dp));Text("دقت سرعت",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)}
   Text("برای سرعت دقیق‌تر، GPS روشن و مجوز موقعیت مکانی فعال باشد.")
  }}
  if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q)Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){Text("مجوزها",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text("مجوز فعالیت بدنی و در صورت استفاده از سرعت/تمرین، موقعیت مکانی باید توسط کاربر تأیید شود.")}}
 }
}