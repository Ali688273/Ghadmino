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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ir.ghadmino.stepcounter.reward.CoinWallet
import ir.ghadmino.stepcounter.analytics.ActivityAnalyticsRepository
import ir.ghadmino.stepcounter.analytics.ActivityIntelligenceScreen
import ir.ghadmino.stepcounter.notification.InactivityScheduler
import ir.ghadmino.stepcounter.backup.BackupScreen
import ir.ghadmino.stepcounter.insights.ActivityInsightsRepository
import ir.ghadmino.stepcounter.insights.ActivityInsightsScreen
import ir.ghadmino.stepcounter.health.HealthConnectScreen
import ir.ghadmino.stepcounter.workout.WorkoutScreen
import ir.ghadmino.stepcounter.plan.StepPlanScreen
import ir.ghadmino.stepcounter.activity.ManualActivityScreen
import ir.ghadmino.stepcounter.history.ActivityCalendarScreen
import ir.ghadmino.stepcounter.challenge.ChallengesScreen
import ir.ghadmino.stepcounter.settings.SettingsScreen
import ir.ghadmino.stepcounter.profile.ProfileRepository
import ir.ghadmino.stepcounter.profile.ProfileScreen
import ir.ghadmino.stepcounter.profile.ProfileExtrasScreen
import ir.ghadmino.stepcounter.achievement.AchievementsScreen
import ir.ghadmino.stepcounter.level.LevelScreen
import ir.ghadmino.stepcounter.reward.RewardCenter
import ir.ghadmino.stepcounter.speed.SpeedTracker
import ir.ghadmino.stepcounter.stats.StatsRepository
import ir.ghadmino.stepcounter.stats.StatsScreen
import ir.ghadmino.stepcounter.step.StepCounterService
import ir.ghadmino.stepcounter.step.StepHistory
import ir.ghadmino.stepcounter.free.FreeFeaturesScreen
import ir.ghadmino.stepcounter.ui.theme.GhadminoTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var speedTracker: SpeedTracker
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        speedTracker.start()
        if (hasActivityRecognitionPermission()) startStepService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speedTracker = SpeedTracker(this)
        requestPermissions()
        setContent {
            var themeId by remember { mutableStateOf(CoinWallet.selectedTheme(this@MainActivity)) }
            GhadminoTheme(themeId = themeId) {
                GhadminoApp(speedTracker) { newTheme ->
                    CoinWallet.setSelectedTheme(this@MainActivity, newTheme)
                    themeId = newTheme
                }
            }
        }
        if (hasActivityRecognitionPermission()) startStepService()
        InactivityScheduler.schedule(this)
    }

    override fun onResume() { super.onResume(); if (::speedTracker.isInitialized) speedTracker.start() }
    override fun onPause() { if (::speedTracker.isInitialized) speedTracker.stop(); super.onPause() }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION); permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (permissions.isNotEmpty()) permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun hasActivityRecognitionPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    private fun startStepService() {
        if (!hasActivityRecognitionPermission()) return
        val intent = Intent(this, StepCounterService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (_: SecurityException) {
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GhadminoApp(speedTracker: SpeedTracker, onThemeChanged: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ghadmino_ui", Context.MODE_PRIVATE) }
    var goal by remember { mutableIntStateOf(ProfileRepository.load(context).dailyGoal) }
    var steps by remember { mutableIntStateOf(ActivityAnalyticsRepository.today(context)) }
    var currentSpeed by remember { mutableFloatStateOf(speedTracker.currentSpeedKmh) }
    var averageSpeed by remember { mutableFloatStateOf(speedTracker.averageSpeedKmh) }
    var minimumSpeed by remember { mutableFloatStateOf(speedTracker.minimumSpeedKmh) }
    var maximumSpeed by remember { mutableFloatStateOf(speedTracker.maximumSpeedKmh) }
    var coins by remember { mutableIntStateOf(CoinWallet.balance(context)) }
    var selectedTheme by remember { mutableStateOf(CoinWallet.selectedTheme(context)) }
    var tab by remember { mutableIntStateOf(0) }
    var morePage by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var profileName by remember { mutableStateOf(ProfileRepository.load(context).name) }
    var insights by remember { mutableStateOf(ActivityInsightsRepository.calculate(context, goal)) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        while (true) {
            steps = ActivityAnalyticsRepository.today(context)
            StepHistory.saveToday(context, steps)
            CoinWallet.syncStepReward(context, steps)
            if (steps >= goal) CoinWallet.claimGoalReward(context)
            coins = CoinWallet.balance(context)
            currentSpeed = speedTracker.currentSpeedKmh
            averageSpeed = speedTracker.averageSpeedKmh
            minimumSpeed = speedTracker.minimumSpeedKmh
            maximumSpeed = speedTracker.maximumSpeedKmh
            insights = ActivityInsightsRepository.calculate(context, goal)
            delay(1000)
        }
    }

    val progress = if (goal > 0) (steps.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val profile = ProfileRepository.load(context)
    val distanceKm = steps * profile.strideCm / 100000.0
    val calories = steps * profile.strideCm * profile.weightKg * 0.5 / 100000.0

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("قدمینو", fontWeight = FontWeight.Bold)
                        Text(
                            when (tab) {
                                0 -> "داشبورد فعالیت"
                                1 -> "آمار و گزارش‌ها"
                                2 -> "پاداش و فروشگاه"
                                else -> "بیشتر"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                actions = {
                    AssistChip(
                        onClick = { tab = 2 },
                        label = { Text(coins.toString()) },
                        leadingIcon = { Icon(Icons.Default.Paid, null) }
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.DirectionsWalk, null) },
                    label = { Text("خانه") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.BarChart, null) },
                    label = { Text("آمار") }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.Paid, null) },
                    label = { Text("پاداش") }
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text("بیشتر") }
                )
            }
        }
    ) { padding ->
        when (tab) {
            0 -> HomePage(
                padding = padding,
                steps = steps,
                goal = goal,
                progress = progress,
                distanceKm = distanceKm,
                calories = calories,
                currentSpeed = currentSpeed,
                averageSpeed = averageSpeed,
                minimumSpeed = minimumSpeed,
                maximumSpeed = maximumSpeed,
                onGoalChanged = {
                    goal = it
                    val profile = ProfileRepository.load(context)
                    ProfileRepository.save(context, profile.copy(dailyGoal = it))
                    prefs.edit().putInt("daily_goal", it).apply()
                }
            )
            1 -> StatsScreen(
                StatsRepository.load(context, goal),
                goal
            )
            2 -> RewardCenter(
                coins = coins,
                onCoinsChanged = { coins = CoinWallet.balance(context) },
                selectedTheme = selectedTheme,
                onBuyFreeze = {
                    info = if (CoinWallet.unlock(context, "streak_freeze", 250))
                        "محافظ زنجیره خریداری شد."
                    else
                        "سکه کافی نیست."
                    coins = CoinWallet.balance(context)
                },
                onBuyTheme = { id, cost ->
                    if (cost == 0) {
                        CoinWallet.setSelectedTheme(context, id)
                        selectedTheme = id
                        onThemeChanged(id)
                        info = "تم فعال شد."
                    } else if (CoinWallet.unlock(context, "theme_" + id, cost)) {
                        CoinWallet.setSelectedTheme(context, id)
                        selectedTheme = id
                        onThemeChanged(id)
                        coins = CoinWallet.balance(context)
                        info = "تم خریداری و فعال شد."
                    } else {
                        info = "سکه کافی نیست."
                    }
                }
            )
            3 -> MorePage(
                profileName = profileName,
                onOpen = { morePage = it }
            )
        }
    }

    when (morePage) {
        "profile" -> FullPageDialog("پروفایل", onClose = { morePage = null }) {
            ProfileScreen {
                profileName = ProfileRepository.load(context).name
            }
        }
        "achievements" -> FullPageDialog("دستاوردها", onClose = { morePage = null }) {
            AchievementsScreen { coins = CoinWallet.balance(context) }
        }
        "level" -> FullPageDialog("سطح و XP", onClose = { morePage = null }) {
            LevelScreen()
        }
        "extras" -> FullPageDialog("شخصی‌سازی", onClose = { morePage = null }) {
            ProfileExtrasScreen { coins = CoinWallet.balance(context) }
        }
        "smart" -> FullPageDialog("گزارش هوشمند", onClose = { morePage = null }) {
            ActivityIntelligenceScreen { coins = CoinWallet.balance(context) }
        }
        "insights" -> FullPageDialog("تحلیل فعالیت", onClose = { morePage = null }) {
            ActivityInsightsScreen(insights, ActivityInsightsRepository.hourly(context))
        }
        "calendar" -> FullPageDialog("تقویم فعالیت و گزارش روزانه", onClose = { morePage = null }) {
            ActivityCalendarScreen(goal)
        }
        "history" -> FullPageDialog("تاریخچه ۷ روزه", onClose = { morePage = null }) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StepHistory.recent(context).forEach {
                    ListItem(
                        headlineContent = { Text(it.first) },
                        trailingContent = { Text(it.second.toString() + " قدم") }
                    )
                }
            }
        }
        "manual_activity" -> FullPageDialog("ثبت فعالیت دستی", onClose = { morePage = null }) {
            ManualActivityScreen()
        }
        "challenges" -> FullPageDialog("چالش‌ها", onClose = { morePage = null }) { ChallengesScreen { coins = CoinWallet.balance(context) } }
        "settings" -> FullPageDialog("تنظیمات", onClose = { morePage = null }) { SettingsScreen() }
        "health" -> FullPageDialog("Health Connect", onClose = { morePage = null }) {
            HealthConnectScreen()
        }
        "workout" -> FullPageDialog("تمرین پیاده‌روی", onClose = { morePage = null }) {
            WorkoutScreen()
        }
        "plan" -> FullPageDialog("برنامه افزایش قدم", onClose = { morePage = null }) {
            StepPlanScreen()
        }
        "backup" -> FullPageDialog("پشتیبان‌گیری و بازیابی", onClose = { morePage = null }) {
            BackupScreen()
        }
        "free" -> FullPageDialog("۲۰ قابلیت رایگان", onClose = { morePage = null }) {
            FreeFeaturesScreen { coins = CoinWallet.balance(context) }
        }
        "goal" -> FullPageDialog("تنظیم هدف", onClose = { morePage = null }) {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("هدف روزانه: " + goal + " قدم")
                Slider(
                    value = goal.toFloat(),
                    onValueChange = {
                        goal = it.toInt()
                        val profile = ProfileRepository.load(context)
                        ProfileRepository.save(context, profile.copy(dailyGoal = goal))
                        prefs.edit().putInt("daily_goal", goal).apply()
                    },
                    valueRange = 1000f..30000f,
                    steps = 28
                )
                Text("هدف بین ۱۰۰۰ تا ۳۰۰۰۰ قدم قابل تنظیم است.")
            }
        }
    }

    LaunchedEffect(info) {
        info?.let { message ->
            snackbarHostState.showSnackbar(message)
            info = null
        }
    }
}

@Composable
private fun HomePage(
    padding: PaddingValues,
    steps: Int,
    goal: Int,
    progress: Float,
    distanceKm: Double,
    calories: Double,
    currentSpeed: Float,
    averageSpeed: Float,
    minimumSpeed: Float,
    maximumSpeed: Float,
    onGoalChanged: (Int) -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.DirectionsWalk, null, Modifier.size(58.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    steps.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold
                )
                Text("قدم امروز", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(progress = progress, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text((progress * 100).toInt().toString() + "٪ از هدف " + goal)
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                Modifier.weight(1f),
                Icons.Default.Route,
                "مسافت",
                String.format("%.2f km", distanceKm)
            )
            StatCard(
                Modifier.weight(1f),
                Icons.Default.LocalFireDepartment,
                "کالری",
                calories.toInt().toString() + " kcal"
            )
        }

        SpeedCard(currentSpeed, averageSpeed, minimumSpeed, maximumSpeed)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("هدف روزانه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(goal.toString() + " قدم", style = MaterialTheme.typography.titleLarge)
                Slider(
                    value = goal.toFloat(),
                    onValueChange = { onGoalChanged(it.toInt()) },
                    valueRange = 1000f..30000f,
                    steps = 28
                )
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("وضعیت قدم‌شمار", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (StepCounterService.sensorAvailable)
                        "✓ حسگر قدم‌شمار فعال است"
                    else
                        "⚠ حسگر قدم‌شمار روی این گوشی پیدا نشد"
                )
                Text("شمارش قدم‌ها در پس‌زمینه ادامه پیدا می‌کند.")
            }
        }
    }
}

@Composable
private fun MorePage(
    profileName: String,
    onOpen: (String) -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("امکانات قدمینو", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("هر بخش صفحه جداگانه دارد تا برنامه خلوت و حرفه‌ای بماند.")

        MoreItem("👤", if (profileName.isBlank()) "پروفایل" else profileName, "اطلاعات بدنی و هدف‌ها") { onOpen("profile") }
        MoreItem("🏆", "دستاوردها", "مدال‌ها و پاداش‌های پیشرفت") { onOpen("achievements") }
        MoreItem("⭐", "سطح و XP", "سطح کاربر و میزان پیشرفت") { onOpen("level") }
        MoreItem("🎨", "شخصی‌سازی", "قاب، نشان و امکانات قابل خرید") { onOpen("extras") }
        MoreItem("📅", "تقویم فعالیت", "انتخاب هر روز و مشاهده گزارش واقعی همان روز") { onOpen("calendar") }
        MoreItem("📋", "تاریخچه", "مشاهده قدم‌های روزهای اخیر") { onOpen("history") }
        MoreItem("📊", "گزارش هوشمند", "روند، رکورد، پیش‌بینی و ماموریت‌های روزانه") { onOpen("smart") }
        MoreItem("📈", "تحلیل فعالیت", "امتیاز، فعالیت ساعتی و پیش‌بینی هدف") { onOpen("insights") }
        MoreItem("🎯", "تنظیم هدف", "تغییر هدف روزانه قدم‌ها") { onOpen("goal") }
        MoreItem("❤️", "Health Connect", "اتصال قدمینو به داده‌های سلامت اندروید") { onOpen("health") }
        MoreItem("🏃", "تمرین پیاده‌روی", "شروع، توقف و ثبت یک جلسه واقعی") { onOpen("workout") }
        MoreItem("📆", "برنامه افزایش قدم", "هدف‌گذاری تدریجی و قابل پیگیری") { onOpen("plan") }
        MoreItem("💾", "پشتیبان‌گیری", "ذخیره و بازیابی رایگان اطلاعات روی فایل") { onOpen("backup") }
        MoreItem("🧰", "۲۰ قابلیت رایگان", "منبع قدم، Health Connect، رکورد، هدف، خروجی و عیب‌یابی") { onOpen("free") }
        MoreItem("➕", "ثبت فعالیت دستی", "ثبت قدم یا فعالیتی که حسگر ثبت نکرده") { onOpen("manual_activity") }
    }
}

@Composable
private fun MoreItem(
    emoji: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onClick) { Text("ورود") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullPageDialog(
    title: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Surface(
            Modifier.fillMaxWidth().fillMaxHeight(0.92f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Text("‹", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                )
                Box(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun SpeedCard(current: Float, average: Float, minimum: Float, maximum: Float) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, null, Modifier.size(30.dp))
                Spacer(Modifier.size(10.dp))
                Text("سرعت حرکت", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            Text("سرعت فعلی")
            Text(
                String.format("%.1f km/h", current),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SpeedValue("میانگین", average)
                SpeedValue("کمترین", minimum)
                SpeedValue("بیشترین", maximum)
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
fun StatCard(modifier: Modifier, icon: ImageVector, title: String, value: String) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null)
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}
