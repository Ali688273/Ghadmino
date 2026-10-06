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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
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
import ir.ghadmino.stepcounter.reward.RewardCenter
import ir.ghadmino.stepcounter.analytics.ActivityAnalyticsRepository
import ir.ghadmino.stepcounter.analytics.ActivityIntelligenceScreen
import ir.ghadmino.stepcounter.notification.InactivityScheduler
import ir.ghadmino.stepcounter.backup.BackupScreen
import ir.ghadmino.stepcounter.insights.ActivityInsightsRepository
import ir.ghadmino.stepcounter.insights.ActivityInsightsScreen
import ir.ghadmino.stepcounter.health.HealthConnectScreen
import ir.ghadmino.stepcounter.health.HealthConnectRepository
import ir.ghadmino.stepcounter.free.StepSource
import ir.ghadmino.stepcounter.free.StepSourceRepository
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
import ir.ghadmino.stepcounter.stats.StatsRepository
import ir.ghadmino.stepcounter.stats.StatsScreen
import ir.ghadmino.stepcounter.step.StepCounterService
import ir.ghadmino.stepcounter.step.StepHistory
import ir.ghadmino.stepcounter.free.FreeFeaturesScreen
import ir.ghadmino.stepcounter.free.WeeklyReportScheduler
import ir.ghadmino.stepcounter.ui.theme.GhadminoTheme
import ir.ghadmino.stepcounter.ads.GhadminoAdsManager
import ir.ghadmino.stepcounter.ads.GhadminoBanner
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import ir.ghadmino.stepcounter.activity.ActivityTimeRepository

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasActivityRecognitionPermission()) startStepService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()
        setContent {
            var themeId by remember { mutableStateOf(CoinWallet.selectedTheme(this@MainActivity)) }
            GhadminoTheme(themeId = themeId) {
                GhadminoApp { newTheme ->
                    CoinWallet.setSelectedTheme(this@MainActivity, newTheme)
                    themeId = newTheme
                }
            }
        }
        if (hasActivityRecognitionPermission()) startStepService()
        InactivityScheduler.schedule(this)
        WeeklyReportScheduler.schedule(this)
    }


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
fun GhadminoApp(onThemeChanged: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ghadmino_ui", Context.MODE_PRIVATE) }
    var goal by remember { mutableIntStateOf(ProfileRepository.load(context).dailyGoal) }
    var steps by remember { mutableIntStateOf(ActivityAnalyticsRepository.today(context)) }
    var stepSource by remember { mutableStateOf(StepSourceRepository.get(context)) }
    var walkingMinutes by remember { mutableIntStateOf(ActivityTimeRepository.todayActiveMinutes(context)) }
    var showInitialProfile by remember { mutableStateOf(!ProfileRepository.isComplete(context)) }
    var coins by remember { mutableIntStateOf(CoinWallet.balance(context)) }
    var selectedTheme by remember { mutableStateOf(CoinWallet.selectedTheme(context)) }
    var tab by remember { mutableIntStateOf(0) }
    var morePage by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var profileName by remember { mutableStateOf(ProfileRepository.load(context).name) }
    var insights by remember { mutableStateOf(ActivityInsightsRepository.calculate(context, goal)) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        var lastHealthRefresh = 0L
        var lastSavedSteps = steps
        var lastInsightRefresh = 0L
        while (true) {
            stepSource = StepSourceRepository.get(context)
            val phoneSteps = ActivityAnalyticsRepository.today(context)
            val now = System.currentTimeMillis()
            if (stepSource == StepSource.PHONE || (stepSource == StepSource.AUTO && StepCounterService.sensorAvailable)) {
                steps = phoneSteps
            } else if (now - lastHealthRefresh >= 60000L) {
                val hc = try { HealthConnectRepository.todaySteps(context) } catch (_: Exception) { null }
                steps = if (hc != null) hc.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt() else phoneSteps
                lastHealthRefresh = now
            }
            if (steps != lastSavedSteps) {
                StepHistory.saveToday(context, steps)
                CoinWallet.syncStepReward(context, steps)
                if (steps >= goal) CoinWallet.claimGoalReward(context)
                lastSavedSteps = steps
            }
            // Small one-time rewards are checked every cycle so they are not
            // missed when the step value changed before the UI was ready.
            CoinWallet.claimDailyLoginReward(context)
            coins = CoinWallet.balance(context)
            walkingMinutes = ActivityTimeRepository.todayActiveMinutes(context)
            if (now - lastInsightRefresh >= 10000L) {
                insights = ActivityInsightsRepository.calculate(context, goal)
                lastInsightRefresh = now
            }
            delay(1000)
        }
    }

    LaunchedEffect(tab) {
        if (tab == 1) {
            (context as? ComponentActivity)?.let { activity ->
                GhadminoAdsManager.showInterstitial(activity)
            }
        }
    }

    val progress = if (goal > 0) (steps.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val profile = ProfileRepository.load(context)
    val distanceKm = steps * profile.strideCm / 100000.0
    val activeSeconds = ActivityTimeRepository.todayActiveSeconds(context)
    val distanceMeters = steps * profile.strideCm.toDouble() / 100.0
    val walkingSpeedKmh = if (activeSeconds > 0) (distanceMeters / activeSeconds * 3.6).coerceIn(1.5, 7.5) else 4.0
    val walkingSpeedMPerMin = walkingSpeedKmh * 1000.0 / 60.0
    val met = ((0.1 * walkingSpeedMPerMin + 3.5) / 3.5).coerceIn(2.0, 6.8)
    val calories = if (activeSeconds > 0) met * profile.weightKg * activeSeconds / 3600.0 else 0.0

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                title = {
                    Column {
                        Text("قدمینو", fontWeight = FontWeight.Bold)
                        Text(
                            when (tab) {
                                0 -> "داشبورد فعالیت"
                                1 -> "آمار و گزارش‌ها"
                                2 -> "بیشتر"
                                else -> "بیشتر"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                actions = { }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
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
                walkingMinutes = walkingMinutes,
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
            2 -> MorePage(
                profileName = profileName,
                onOpen = { morePage = it }
            )
        }
    }

    when (morePage) {
        "profile" -> FullPageDialog("پروفایل", onClose = { morePage = null }) {
            ProfileScreen {
                profileName = ProfileRepository.load(context).name
                morePage = null
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
        "history" -> FullPageDialog("تاریخچه ۳۰ روز اخیر", onClose = { morePage = null }) {
            androidx.compose.foundation.lazy.LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(StepHistory.recent(context, 30)) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text(item.first) },
                            trailingContent = { Text(item.second.toString() + " قدم") }
                        )
                    }
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
        "rewards" -> FullPageDialog("پاداش و امکانات رایگان", onClose = { morePage = null }) {
            RewardCenter(
                coins = coins,
                selectedTheme = selectedTheme,
                onBuyFreeze = { info = "این بخش در نسخه رایگان فعال است." },
                onBuyTheme = { id, _ ->
                    CoinWallet.setSelectedTheme(context, id)
                    selectedTheme = id
                    onThemeChanged(id)
                },
                onCoinsChanged = { coins = CoinWallet.balance(context) }
            )
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
                        goal = ((it / 1000f).roundToInt() * 1000).coerceIn(1000, 30000)
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

    if (showInitialProfile) {
        Dialog(onDismissRequest = {}) {
            Card(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .padding(12.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("اطلاعات اولیه", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("برای محاسبه دقیق‌تر کالری، مسافت و فعالیت، اطلاعات بدنی خودت را وارد کن.")
                    Spacer(Modifier.height(12.dp))
                    ProfileScreen {
                        ProfileRepository.markComplete(context)
                        showInitialProfile = false
                        profileName = ProfileRepository.load(context).name
                        goal = ProfileRepository.load(context).dailyGoal
                    }
                }
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
    walkingMinutes: Int,
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
                0,
                "مسافت",
                String.format("%.2f km", distanceKm)
            )
            StatCard(
                Modifier.weight(1f),
                Icons.Default.LocalFireDepartment,
                1,
                "کالری",
                calories.toInt().toString() + " kcal"
            )
            StatCard(
                Modifier.weight(1f),
                Icons.Default.DirectionsWalk,
                2,
                "زمان راه‌رفتن",
                walkingMinutes.toString() + " دقیقه"
            )
        }

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("هدف روزانه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(goal.toString() + " قدم", style = MaterialTheme.typography.titleLarge)
                Slider(
                    value = goal.toFloat(),
                    onValueChange = {
                        onGoalChanged(((it / 1000f).roundToInt() * 1000).coerceIn(1000, 30000))
                    },
                    valueRange = 1000f..30000f,
                    steps = 28
                )
            }
        }

        GhadminoBanner(modifier = Modifier.fillMaxWidth())

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
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("امکانات قدمینو", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("بخش‌های کاربردی برنامه در صفحه‌های جداگانه قرار گرفته‌اند.")
        GhadminoBanner(modifier = Modifier.fillMaxWidth())
        MoreItem("👤", if (profileName.isBlank()) "پروفایل" else profileName, "اطلاعات بدنی و هدف‌ها") { onOpen("profile") }
        MoreItem("🏆", "دستاوردها", "مدال‌ها و پاداش‌های پیشرفت") { onOpen("achievements") }
        MoreItem("🎁", "پاداش و امکانات رایگان", "فعالیت‌ها، پیشرفت و امکانات بدون قفل و بدون سکه") { onOpen("rewards") }
        MoreItem("⭐", "سطح و XP", "سطح کاربر و میزان پیشرفت") { onOpen("level") }
        MoreItem("🎨", "شخصی‌سازی رایگان", "قاب‌ها، نشان‌ها و امکانات بدون پرداخت") { onOpen("extras") }
        MoreItem("📅", "تقویم فعالیت", "انتخاب هر روز و مشاهده گزارش واقعی همان روز") { onOpen("calendar") }
        MoreItem("📋", "تاریخچه", "مشاهده قدم‌های روزهای اخیر") { onOpen("history") }
        MoreItem("📊", "گزارش هوشمند", "روند، رکورد، پیش‌بینی و ماموریت‌های روزانه") { onOpen("smart") }
        MoreItem("📈", "تحلیل فعالیت", "امتیاز، فعالیت ساعتی و پیش‌بینی هدف") { onOpen("insights") }
        MoreItem("🎯", "تنظیم هدف", "تغییر هدف روزانه قدم‌ها") { onOpen("goal") }
        MoreItem("❤️", "Health Connect", "اتصال قدمینو به داده‌های سلامت اندروید") { onOpen("health") }
        MoreItem("🏃", "تمرین پیاده‌روی", "شروع، توقف و ثبت یک جلسه واقعی") { onOpen("workout") }
        MoreItem("📆", "برنامه افزایش قدم", "هدف‌گذاری تدریجی و قابل پیگیری") { onOpen("plan") }
        MoreItem("💾", "پشتیبان‌گیری", "ذخیره و بازیابی رایگان اطلاعات روی فایل") { onOpen("backup") }
        MoreItem("🧰", "۲۰ قابلیت رایگان", "۲۰ ابزار کاربردی برای منبع قدم، Health Connect، رکورد، هدف، خروجی و عیب‌یابی") { onOpen("free") }
        MoreItem("➕", "ثبت فعالیت دستی", "ثبت قدم یا فعالیتی که حسگر ثبت نکرده") { onOpen("manual_activity") }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun MoreItem(
    emoji: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    val tone = (title.hashCode() and Int.MAX_VALUE) % 4
    val container = when (tone) {
        0 -> MaterialTheme.colorScheme.primaryContainer
        1 -> MaterialTheme.colorScheme.secondaryContainer
        2 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
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
            color = MaterialTheme.colorScheme.background,
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Text("‹", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                )
                Box(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp).navigationBarsPadding()
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier,
    icon: ImageVector,
    tone: Int = 0,
    title: String,
    value: String
) {
    val container = when (tone % 3) {
        0 -> MaterialTheme.colorScheme.primaryContainer
        1 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null)
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}
