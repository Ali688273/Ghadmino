package ir.ghadmino.stepcounter.health

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import kotlinx.coroutines.launch
import ir.ghadmino.stepcounter.step.StepCounterService

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
            HealthPermission.getWritePermission(StepsRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
        )
    }

    val launcher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        granted = grantedPermissions.containsAll(permissions)
        if (granted) {
            scope.launch {
                externalSteps = runCatching { HealthConnectRepository.todaySteps(context) }.getOrNull()
                externalDistance = runCatching { HealthConnectRepository.todayDistanceMeters(context) }.getOrNull()
                externalCalories = runCatching { HealthConnectRepository.todayCalories(context) }.getOrNull()
            }
        }
    }

    LaunchedEffect(availability) {
        if (availability == androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE) {
            val client = HealthConnectRepository.client(context)
            granted = runCatching {
                client?.permissionController?.getGrantedPermissions()?.containsAll(permissions) == true
            }.getOrDefault(false)
            if (granted) {
                externalSteps = runCatching {
                    HealthConnectRepository.todaySteps(context)
                }.getOrNull()
            }
        }
    }

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    "اتصال رایگان به Health Connect",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text("قدمینو می‌تواند داده قدم را از مخزن سلامت خود اندروید بخواند. این قابلیت هزینه سرویس خارجی ندارد.")
            }
        }

        when (availability) {
            androidx.health.connect.client.HealthConnectClient.SDK_UNAVAILABLE -> {
                StatusCard(
                    "Health Connect روی این دستگاه در دسترس نیست.",
                    "در این حالت شمارش داخلی قدمینو بدون تغییر ادامه پیدا می‌کند."
                )
            }

            androidx.health.connect.client.HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                StatusCard(
                    "Health Connect نیاز به نصب یا به‌روزرسانی دارد.",
                    "بعد از نصب/به‌روزرسانی، دوباره این صفحه را باز کن."
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
                ) {
                    Text("باز کردن Health Connect")
                }
            }

            else -> {
                StatusCard(
                    if (granted) "✓ دسترسی قدم‌ها فعال است." else "دسترسی قدم‌ها هنوز فعال نشده.",
                    "می‌توانی هر زمان دسترسی را از تنظیمات Health Connect لغو کنی."
                )

                if (!granted) {
                    Button(
                        onClick = { launcher.launch(permissions) },
                        Modifier.fillMaxWidth()
                    ) {
                        Text("اتصال Health Connect")
                    }
                } else {
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("قدم داخلی قدمینو", style = MaterialTheme.typography.labelLarge)
                            Text(
                                StepCounterService.todaySteps.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("مسافت Health Connect")
                            Text(externalDistance?.let { String.format("%.2f km", it / 1000.0) } ?: "—")
                            Spacer(Modifier.height(8.dp))
                            Text("کالری Health Connect")
                            Text(externalCalories?.let { String.format("%.0f kcal", it) } ?: "—")
                            Spacer(Modifier.height(8.dp))
                            Text("قدم ثبت‌شده در Health Connect")
                            Text(
                                externalSteps?.toString() ?: "در حال خواندن…",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                externalSteps = runCatching { HealthConnectRepository.todaySteps(context) }.getOrNull()
                                externalDistance = runCatching { HealthConnectRepository.todayDistanceMeters(context) }.getOrNull()
                                externalCalories = runCatching { HealthConnectRepository.todayCalories(context) }.getOrNull()
                                message = if (externalSteps == null) {
                                    "خواندن اطلاعات انجام نشد."
                                } else {
                                    "اطلاعات امروز به‌روزرسانی شد."
                                }
                            }
                        },
                        Modifier.fillMaxWidth()
                    ) {
                        Text("به‌روزرسانی اطلاعات")
                    }
                }
            }
        }

        OutlinedButton(
            onClick = {
                runCatching {
                    context.startActivity(HealthConnectRepository.manageDataIntent(context))
                }
            },
            Modifier.fillMaxWidth()
        ) {
            Text("مدیریت دسترسی‌های Health Connect")
        }

        Text(
            "قدمینو فعلاً عدد داخلی خود را به‌عنوان شمارنده اصلی نگه می‌دارد تا دوباره‌شماری بین گوشی، ساعت و Health Connect ایجاد نشود.",
            style = MaterialTheme.typography.bodySmall
        )

        message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }
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
