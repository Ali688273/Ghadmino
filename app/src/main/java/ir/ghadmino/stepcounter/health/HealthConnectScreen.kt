package ir.ghadmino.stepcounter.health

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import ir.ghadmino.stepcounter.step.StepCounterService
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun HealthConnectScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val availability = remember { HealthConnectRepository.availability(context) }

    var granted by remember { mutableStateOf(false) }
    var externalSteps by remember { mutableStateOf<Long?>(null) }
    var externalDistance by remember { mutableStateOf<Double?>(null) }
    var externalCalories by remember { mutableStateOf<Double?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    val permissions = remember {
        setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getWritePermission(StepsRecord::class)
        )
    }

    suspend fun refreshData() {
        externalSteps = runCatching { HealthConnectRepository.todaySteps(context) }.getOrNull()
        externalDistance = runCatching { HealthConnectRepository.todayDistanceMeters(context) }.getOrNull()
        externalCalories = runCatching { HealthConnectRepository.todayCalories(context) }.getOrNull()
    }

    val launcher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        granted = grantedPermissions.containsAll(permissions)
        if (granted) scope.launch { refreshData() }
    }

    LaunchedEffect(availability) {
        if (availability == HealthConnectClient.SDK_AVAILABLE) {
            val client = HealthConnectRepository.client(context)
            granted = runCatching {
                client?.permissionController?.getGrantedPermissions()?.containsAll(permissions) == true
            }.getOrDefault(false)
            if (granted) scope.launch { refreshData() }
        }
    }

    val localSteps = maxOf(
        StepCounterService.todaySteps,
        StepCounterService.persistedTodaySteps(context)
    )
    val difference = externalSteps?.let { abs(it - localSteps.toLong()) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    "اتصال رایگان به Health Connect",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text("داده‌های سلامت امروز از Health Connect خوانده می‌شوند و شمارنده داخلی قدمینو جداگانه باقی می‌ماند.")
            }
        }

        when (availability) {
            HealthConnectClient.SDK_UNAVAILABLE -> {
                StatusCard(
                    "Health Connect روی این دستگاه در دسترس نیست.",
                    "در این حالت شمارش داخلی قدمینو بدون تغییر ادامه پیدا می‌کند."
                )
            }

            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                StatusCard(
                    "Health Connect نیاز به نصب یا به‌روزرسانی دارد.",
                    "بعد از نصب یا به‌روزرسانی، دوباره این صفحه را باز کن."
                )
                Button(
                    onClick = {
                        runCatching {
                            context.startActivity(HealthConnectRepository.manageDataIntent(context))
                        }.onFailure {
                            message = "امکان باز کردن Health Connect پیدا نشد."
                        }
                    },
                    Modifier.fillMaxWidth()
                ) { Text("باز کردن Health Connect") }
            }

            else -> {
                StatusCard(
                    if (granted) "✓ دسترسی خواندن فعال است." else "دسترسی خواندن هنوز فعال نشده.",
                    "قدمینو فقط داده‌های لازم برای نمایش آمار را می‌خواند."
                )

                if (!granted) {
                    Button(
                        onClick = { launcher.launch(permissions) },
                        Modifier.fillMaxWidth()
                    ) { Text("اتصال Health Connect") }
                } else {
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("قدم داخلی قدمینو", style = MaterialTheme.typography.labelLarge)
                            Text(
                                localSteps.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(10.dp))
                            Text("قدم ثبت‌شده در Health Connect", style = MaterialTheme.typography.labelLarge)
                            Text(
                                externalSteps?.toString() ?: "—",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (difference == null) "مقایسه فعلاً در دسترس نیست."
                                else "اختلاف دو منبع: $difference قدم"
                            )
                        }
                    }

                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("داده‌های سلامت امروز", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("مسافت Health Connect: " +
                                (externalDistance?.let { String.format("%.2f km", it / 1000.0) } ?: "—"))
                            Text("کالری Health Connect: " +
                                (externalCalories?.let { String.format("%.0f kcal", it) } ?: "—"))
                        }
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                refreshData()
                                message = if (externalSteps == null) "خواندن اطلاعات انجام نشد."
                                else "اطلاعات امروز به‌روزرسانی شد."
                            }
                        },
                        Modifier.fillMaxWidth()
                    ) { Text("به‌روزرسانی اطلاعات") }

                    Button(
                        onClick = {
                            scope.launch {
                                val ok = runCatching {
                                    HealthConnectRepository.writeTodaySteps(context, localSteps.toLong())
                                }.getOrDefault(false)
                                message = if (ok) {
                                    "قدم‌های امروز به Health Connect ارسال شد."
                                } else {
                                    "ارسال قدم‌ها انجام نشد؛ مجوز نوشتن را بررسی کن."
                                }
                                if (ok) refreshData()
                            }
                        },
                        Modifier.fillMaxWidth()
                    ) { Text("ارسال قدم‌های امروز به Health Connect") }
                }
            }
        }

        OutlinedButton(
            onClick = {
                runCatching {
                    context.startActivity(HealthConnectRepository.manageDataIntent(context))
                }.onFailure {
                    message = "امکان باز کردن مدیریت Health Connect وجود ندارد."
                }
            },
            Modifier.fillMaxWidth()
        ) { Text("مدیریت دسترسی‌های Health Connect") }

        Text(
            "ارسال به Health Connect اختیاری است؛ قدمینو همچنان شمارنده داخلی خود را منبع اصلی می‌داند. رکورد روزانه با شناسه ثابت به‌روزرسانی می‌شود تا هر بار رکورد جدید و تکراری ساخته نشود.",
            style = MaterialTheme.typography.bodySmall
        )

        message?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun StatusCard(title: String, description: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(description)
        }
    }
}