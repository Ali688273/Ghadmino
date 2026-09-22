package ir.ghadmino.stepcounter

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.ghadmino.stepcounter.reward.CoinWallet
import ir.ghadmino.stepcounter.reward.RewardCenter
import ir.ghadmino.stepcounter.speed.SpeedTracker
import ir.ghadmino.stepcounter.stats.StatsRepository
import ir.ghadmino.stepcounter.stats.StatsScreen
import ir.ghadmino.stepcounter.step.StepCounterService
import ir.ghadmino.stepcounter.step.StepHistory
import ir.ghadmino.stepcounter.ui.theme.GhadminoTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var speedTracker: SpeedTracker
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { speedTracker.start() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speedTracker = SpeedTracker(this)
        requestPermissions()
        setContent { GhadminoTheme { GhadminoApp(speedTracker) } }
        startStepService()
    }

    override fun onResume() {
        super.onResume()
        if (::speedTracker.isInitialized) speedTracker.start()
    }

    override fun onPause() {
        if (::speedTracker.isInitialized) speedTracker.stop()
        super.onPause()
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (permissions.isNotEmpty()) permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun startStepService() {
        val intent = Intent(this, StepCounterService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GhadminoApp(speedTracker: SpeedTracker) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ghadmino_ui", Context.MODE_PRIVATE) }
    var goal by remember { mutableIntStateOf(prefs.getInt("daily_goal", 8000)) }
    var steps by remember { mutableIntStateOf(StepCounterService.todaySteps) }
    var currentSpeed by remember { mutableFloatStateOf(speedTracker.currentSpeedKmh) }
    var averageSpeed by remember { mutableFloatStateOf(speedTracker.averageSpeedKmh) }
    var minimumSpeed by remember { mutableFloatStateOf(speedTracker.minimumSpeedKmh) }
    var maximumSpeed by remember { mutableFloatStateOf(speedTracker.maximumSpeedKmh) }
    var coins by remember { mutableIntStateOf(CoinWallet.balance(context)) }
    var showStats by remember { mutableStateOf(false) }
    var showRewards by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            steps = StepCounterService.todaySteps
            StepHistory.saveToday(context, steps)
            CoinWallet.syncStepReward(context, steps)
            if (steps >= goal) CoinWallet.claimGoalReward(context)
            coins = CoinWallet.balance(context)
            currentSpeed = speedTracker.currentSpeedKmh
            averageSpeed = speedTracker.averageSpeedKmh
            minimumSpeed = speedTracker.minimumSpeedKmh
            maximumSpeed = speedTracker.maximumSpeedKmh
            delay(1000)
        }
    }

    val progress = if (goal > 0) (steps.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val distanceKm = steps * 0.00075
    val calories = steps * 0.04

    Scaffold(topBar = {
        TopAppBar(
            title = { Column { Text("قدم‌شمار قدمینو", fontWeight = FontWeight.Bold); Text("فعالیت امروز", style = MaterialTheme.typography.labelSmall) } },
            actions = {
                AssistChip(onClick = { showRewards = true }, label = { Text(coins.toString()) }, leadingIcon = { Icon(Icons.Default.Paid, null) })
                IconButton(onClick = { showStats = true }) { Icon(Icons.Default.BarChart, "آمار") }
                IconButton(onClick = { showHistory = true }) { Icon(Icons.Default.Route, "تاریخچه") }
                IconButton(onClick = { showGoalDialog = true }) { Icon(Icons.Default.Settings, "تنظیمات") }
            }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsWalk, null, Modifier.size(54.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(steps.toString(), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
                    Text("قدم امروز", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text((progress * 100).toInt().toString() + "٪ از هدف")
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Modifier.weight(1f), { Icon(Icons.Default.Route, null) }, "مسافت", String.format("%.2f km", distanceKm))
                StatCard(Modifier.weight(1f), { Icon(Icons.Default.LocalFireDepartment, null) }, "کالری", calories.toInt().toString() + " kcal")
            }
            Spacer(Modifier.height(12.dp))
            SpeedCard(currentSpeed, averageSpeed, minimumSpeed, maximumSpeed)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { showStats = true }, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.BarChart, null); Spacer(Modifier.width(8.dp)); Text("آمار حرفه‌ای و نمودار ۳۰ روزه")
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { showRewards = true }, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Paid, null); Spacer(Modifier.width(8.dp)); Text("مرکز جایزه و فروشگاه سکه")
            }
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("هدف روزانه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(goal.toString() + " قدم", style = MaterialTheme.typography.titleLarge)
                    Slider(value = goal.toFloat(), onValueChange = { goal = it.toInt(); prefs.edit().putInt("daily_goal", goal).apply() }, valueRange = 1000f..30000f, steps = 28)
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(18.dp)) {
                    Text("وضعیت قدم‌شمار", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(if (StepCounterService.sensorAvailable) "✓ حسگر قدم‌شمار فعال است" else "⚠ حسگر قدم‌شمار روی این گوشی پیدا نشد")
                    Text("شمارش قدم‌ها در پس‌زمینه ادامه پیدا می‌کند.")
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("هر ۱۰۰۰ قدم = ۵ سکه • هدف روزانه = ۲۰ سکه", style = MaterialTheme.typography.labelMedium)
            Text("قدمینو • نسخه 1.1.0", style = MaterialTheme.typography.labelSmall)
        }
    }

    if (showStats) {
        AlertDialog(onDismissRequest = { showStats = false }, title = { Text("آمار حرفه‌ای") },
            text = { StatsScreen(StatsRepository.load(context, goal), goal) },
            confirmButton = { Button(onClick = { showStats = false }) { Text("بستن") } })
    }

    if (showRewards) {
        AlertDialog(onDismissRequest = { showRewards = false }, title = { Text("مرکز جایزه و فروشگاه") },
            text = {
                RewardCenter(
                    coins = coins,
                    adAvailable = false,
                    onWatchAd = { info = "اتصال تبلیغ جایزه‌ای به SDK تپسل/ادیوری در مرحله تبلیغات انجام می‌شود؛ سکه فقط پس از تکمیل واقعی تبلیغ ثبت خواهد شد." },
                    onBuyFreeze = {
                        info = if (CoinWallet.unlock(context, "streak_freeze", 250)) "محافظ زنجیره خریداری شد." else "سکه کافی نیست."
                        coins = CoinWallet.balance(context)
                    },
                    onBuyTheme = {
                        info = if (CoinWallet.unlock(context, "premium_theme", 1000)) "تم ویژه باز شد و در مرحله تم‌ها فعال می‌شود." else "سکه کافی نیست."
                        coins = CoinWallet.balance(context)
                    }
                )
            },
            confirmButton = { Button(onClick = { showRewards = false }) { Text("بستن") } })
    }

    if (showHistory) {
        AlertDialog(onDismissRequest = { showHistory = false }, title = { Text("۷ روز اخیر") },
            text = { Column { StepHistory.recent(context).forEach { Text(it.first + "   " + it.second + " قدم", Modifier.padding(vertical = 5.dp)) } } },
            confirmButton = { Button(onClick = { showHistory = false }) { Text("بستن") } })
    }

    if (showGoalDialog) {
        AlertDialog(onDismissRequest = { showGoalDialog = false }, title = { Text("تنظیم هدف") },
            text = { Column { Text("هدف فعلی: " + goal + " قدم"); Slider(value = goal.toFloat(), onValueChange = { goal = it.toInt(); prefs.edit().putInt("daily_goal", goal).apply() }, valueRange = 1000f..30000f, steps = 28) } },
            confirmButton = { Button(onClick = { showGoalDialog = false }) { Text("ذخیره") } })
    }

    info?.let { text ->
        LaunchedEffect(text) { delay(3000); info = null }
    }
}

@Composable
fun SpeedCard(current: Float, average: Float, minimum: Float, maximum: Float) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Speed, null, Modifier.size(30.dp)); Spacer(Modifier.size(10.dp)); Text("سرعت حرکت", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(14.dp))
            Text("سرعت فعلی")
            Text(String.format("%.1f km/h", current), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SpeedValue("میانگین", average); SpeedValue("کمترین", minimum); SpeedValue("بیشترین", maximum)
            }
        }
    }
}

@Composable
fun SpeedValue(title: String, value: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.labelMedium)
        Text(String.format("%.1f", value), fontWeight = FontWeight.Bold)
        Text("km/h", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun StatCard(modifier: Modifier, icon: @Composable () -> Unit, title: String, value: String) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) { icon(); Spacer(Modifier.height(8.dp)); Text(title, style = MaterialTheme.typography.labelMedium); Text(value, fontWeight = FontWeight.Bold) }
    }
}
