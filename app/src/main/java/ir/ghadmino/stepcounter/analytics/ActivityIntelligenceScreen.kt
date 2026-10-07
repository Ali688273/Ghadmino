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
fun ActivityIntelligenceScreen(onCoinsChanged: () -> Unit = {}) {
    val context = LocalContext.current
    var report by remember { mutableStateOf(ActivityAnalyticsRepository.report(context)) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("گزارش هوشمند", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("امروز", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${report.today} از ${report.goal} قدم")
                LinearProgressIndicator(
                    progress = report.progress / 100f,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
                Text("مسافت %.2f km • ${report.calories} kcal تخمینی".format(Locale.US, report.distanceKm))
                if (report.minutesToGoal > 0) {
                    Text("با نرخ فعلی حدود ${report.minutesToGoal} دقیقه تا هدف باقی مانده.")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("روند ۷ / ۳۰ / ۹۰ روز", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("۷ روز: ${report.week} قدم")
                Text("هفته قبل: ${report.previousWeek} قدم")
                Text("تغییر: ${report.change}%")
                Text("۳۰ روز: ${report.month} قدم")
                Text("میانگین ۳۰ روز: ${report.monthAverage}")
                Text("میانگین ۹۰ روز: ${report.average90}")
                Text("روزهای فعال ۳۰ روز: ${report.active30}")
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("رکوردها", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("بیشترین قدم: ${report.best.second} در ${report.best.first}")
                Text("زنجیره فعال: ${report.streak} روز")
                val speed = ActivityAnalyticsRepository.speedRecords(context)
                Text("بهترین میانگین سرعت: %.1f km/h".format(Locale.US, speed.first))
                Text("بیشترین سرعت: %.1f km/h".format(Locale.US, speed.second))
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("وضعیت امکانات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("تمام قابلیت‌های این گزارش برای همه کاربران آزاد است.")
                Text("برای استفاده از گزارش نیازی به خرید یا پرداخت نیست.")
            }
        }

        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, ActivityAnalyticsRepository.shareText(context))
                }
                context.startActivity(Intent.createChooser(intent, "اشتراک گزارش"))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("اشتراک گزارش")
        }

        OutlinedButton(
            onClick = { report = ActivityAnalyticsRepository.report(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("به‌روزرسانی")
        }
    }
}
