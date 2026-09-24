package ir.ghadmino.stepcounter.analytics
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
@Composable
fun ActivityIntelligenceScreen(onCoinsChanged:()->Unit={}){
 val c=LocalContext.current;var r by remember{mutableStateOf(ActivityAnalyticsRepository.report(c))};var ms by remember{mutableStateOf(DynamicMissionRepository.list(c))}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("گزارش هوشمند",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){Text("امروز",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("${r.today} از ${r.goal} قدم");LinearProgressIndicator(r.progress/100f,Modifier.fillMaxWidth().padding(vertical=8.dp));Text("مسافت %.2f km • ${r.calories} kcal تخمینی".format(Locale.US,r.distanceKm));if(r.minutesToGoal>0)Text("با نرخ فعلی حدود ${r.minutesToGoal} دقیقه تا هدف باقی مانده.")}}
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){Text("روند ۷ / ۳۰ / ۹۰ روز",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("۷ روز: ${r.week} قدم");Text("هفته قبل: ${r.previousWeek} قدم");Text("تغییر: ${r.change}%");Text("۳۰ روز: ${r.month} قدم");Text("میانگین ۳۰ روز: ${r.monthAverage}");Text("میانگین ۹۰ روز: ${r.average90}");Text("روزهای فعال ۳۰ روز: ${r.active30}")}}
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){Text("رکوردها",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("بیشترین قدم: ${r.best.second} در ${r.best.first}");Text("زنجیره فعال: ${r.streak} روز");val s=ActivityAnalyticsRepository.speedRecords(c);Text("بهترین میانگین سرعت: %.1f km/h".format(Locale.US,s.first));Text("بیشترین سرعت: %.1f km/h".format(Locale.US,s.second))}}
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){Text("ماموریت‌های روزانه",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);ms.forEach{(m,claimed)->val ready=DynamicMissionRepository.ready(c,m);Text("${m.title} — ${m.reward} سکه");if(claimed)Text("دریافت شده")else if(ready)Button(onClick={if(DynamicMissionRepository.claim(c,m)){ms=DynamicMissionRepository.list(c);onCoinsChanged()}}){Text("دریافت سکه")}else Text("در حال پیشرفت");Spacer(Modifier.height(6.dp))}}}
  Button(onClick={val i=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,ActivityAnalyticsRepository.shareText(c))};c.startActivity(Intent.createChooser(i,"اشتراک گزارش"))},Modifier.fillMaxWidth()){Text("اشتراک گزارش")}
  OutlinedButton(onClick={r=ActivityAnalyticsRepository.report(c);ms=DynamicMissionRepository.list(c)},Modifier.fillMaxWidth()){Text("به‌روزرسانی")}
 }
}
