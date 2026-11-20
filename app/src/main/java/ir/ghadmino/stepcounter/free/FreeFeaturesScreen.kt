package ir.ghadmino.stepcounter.free

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.ghadmino.stepcounter.health.HealthConnectRepository
import ir.ghadmino.stepcounter.step.StepHistory
import kotlinx.coroutines.launch

@Composable
fun FreeFeaturesScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf(StepSourceRepository.get(context)) }
    var message by remember { mutableStateOf<String?>(null) }
    var diagnosticsResult by remember { mutableStateOf<Diagnostics?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            message = null
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            Modifier.fillMaxSize()
                .padding(padding)
                .padding(14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("ابزارهای تکمیلی", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "فقط ابزارهایی که در بخش‌های دیگر برنامه جایگزین ندارند.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FeatureSection(
                title = "منبع ثبت قدم",
                description = "منبع مورد استفاده قدمینو را انتخاب کن. دو منبع با هم جمع نمی‌شوند."
            ) {
                Text("منبع انتخاب‌شده: ${StepSourceRepository.label(source)}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SourceButton("گوشی", StepSource.PHONE, source) {
                        source = it
                        StepSourceRepository.set(context, it)
                    }
                    SourceButton("Health Connect", StepSource.HEALTH_CONNECT, source) {
                        source = it
                        StepSourceRepository.set(context, it)
                    }
                    SourceButton("خودکار", StepSource.AUTO, source) {
                        source = it
                        StepSourceRepository.set(context, it)
                    }
                }
                Text(
                    "برای اتصال، مجوزها و مشاهده اطلاعات Health Connect از بخش «اتصال سلامت» استفاده کن.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val count = try {
                                HealthConnectRepository.syncRecentDays(context, StepHistory.recent(context, 30))
                            } catch (_: Exception) {
                                0
                            }
                            message = if (count > 0) {
                                "$count روز از تاریخچه به Health Connect ارسال شد."
                            } else {
                                "ارسال تاریخچه انجام نشد؛ اتصال و مجوز نوشتن را بررسی کن."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("ارسال تاریخچه ۳۰ روز اخیر به Health Connect") }
            }

            FeatureSection(
                title = "خروجی تاریخچه قدم‌ها",
                description = "از سوابق قدم‌ها در قالب CSV یا JSON خروجی بگیر؛ این خروجی با فایل پشتیبان کامل برنامه فرق دارد."
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { FreeExportRepository.share(context, FreeExportRepository.csv(context), "text/csv") },
                        modifier = Modifier.weight(1f)
                    ) { Text("خروجی CSV") }
                    Button(
                        onClick = { FreeExportRepository.share(context, FreeExportRepository.json(context), "application/json") },
                        modifier = Modifier.weight(1f)
                    ) { Text("خروجی JSON") }
                }
                OutlinedButton(
                    onClick = {
                        val valid = FreeExportRepository.isValidJson(FreeExportRepository.json(context))
                        message = if (valid) "ساختار خروجی JSON سالم است." else "ساختار خروجی JSON معتبر نیست."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("بررسی سلامت خروجی JSON") }
            }

            FeatureSection(
                title = "عیب‌یابی قدم‌شمار",
                description = "وضعیت حسگر، مجوزها، سرویس و آخرین ثبت داده را بررسی کن."
            ) {
                Button(
                    onClick = { scope.launch { diagnosticsResult = diagnostics(context) } },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("اجرای عیب‌یابی") }
                diagnosticsResult?.let { result ->
                    DiagnosticRow("حسگر قدم", result.sensor)
                    DiagnosticRow("مجوز فعالیت بدنی", result.activityPermission)
                    DiagnosticRow("مجوز اعلان", result.notificationPermission)
                    DiagnosticRow("مجوز مکان برای قابلیت سرعت", result.locationPermission)
                    DiagnosticText("Health Connect", result.healthConnect)
                    DiagnosticText("وضعیت سرویس قدم‌شمار", result.serviceState)
                    DiagnosticText("آخرین دریافت سنسور", DiagnosticsHistoryRepository.text(context, "sensor"))
                    DiagnosticText("آخرین ذخیره قدم", DiagnosticsHistoryRepository.text(context, "save"))
                    Text(
                        "اگر شمارش در پس‌زمینه متوقف می‌شود، محدودیت بهینه‌سازی باتری گوشی را در تنظیمات بررسی کن.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureSection(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember(title) { mutableStateOf(false) }
    Card(
        Modifier.fillMaxWidth().animateContentSize(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (expanded) androidx.compose.material.icons.Icons.Default.ExpandLess
                    else androidx.compose.material.icons.Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "بستن" else "باز کردن",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = content
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceButton(
    label: String,
    value: StepSource,
    current: StepSource,
    onClick: (StepSource) -> Unit
) {
    FilterChip(
        selected = current == value,
        onClick = { onClick(value) },
        label = { Text(label) }
    )
}

@Composable
private fun DiagnosticRow(label: String, ok: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(if (ok) "✓ فعال" else "⚠ بررسی شود", color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun DiagnosticText(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}
