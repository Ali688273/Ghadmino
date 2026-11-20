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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import ir.ghadmino.stepcounter.ads.GhadminoNativeAdCards
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import ir.ghadmino.stepcounter.activity.ActivityTimeRepository

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasActivityRecognitionPermission()) startStepService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GhadminoAdsManager.prepare(this)
        requestPermissions()
        setContent {
            var themeId by remember { mutableStateOf(getSharedPreferences("ghadmino_ui", Context.MODE_PRIVATE).getString("selected_theme", "default") ?: "default") }
            GhadminoTheme(themeId = themeId) {
                var fontScale by remember {
                    mutableFloatStateOf(
                        getSharedPreferences("ghadmino_ui", Context.MODE_PRIVATE)
                            .getFloat("font_scale", 1f)
                    )
                }
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = LocalDensity.current.density,
                        fontScale = LocalDensity.current.fontScale * fontScale
                    )
                ) {
                    GhadminoApp(
                        onThemeChanged = { newTheme ->
                            getSharedPreferences("ghadmino_ui", Context.MODE_PRIVATE).edit().putString("selected_theme", newTheme).apply()
                            themeId = newTheme
                        },
                        onFontScaleChanged = { scale ->
                            fontScale = scale
                            getSharedPreferences("ghadmino_ui", Context.MODE_PRIVATE)
                                .edit().putFloat("font_scale", scale).apply()
                        }
                    )
                }
            }
        }
        if (hasActivityRecognitionPermission()) startStepService()
        InactivityScheduler.schedule(this)
        WeeklyReportScheduler.schedule(this)
    }


    override fun onStart() {
        super.onStart()
        GhadminoAdsManager.onAppForegrounded(this)
    }

    override fun onStop() {
        GhadminoAdsManager.onAppBackgrounded()
        super.onStop()
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
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
fun GhadminoApp(onThemeChanged: (String) -> Unit, onFontScaleChanged: (Float) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ghadmino_ui", Context.MODE_PRIVATE) }
    var goal by remember { mutableIntStateOf(ProfileRepository.load(context).dailyGoal) }
    var steps by remember { mutableIntStateOf(ActivityAnalyticsRepository.today(context)) }
    var stepSource by remember { mutableStateOf(StepSourceRepository.get(context)) }
    var walkingMinutes by remember { mutableIntStateOf(ActivityTimeRepository.todayActiveMinutes(context)) }
    var showInitialProfile by remember { mutableStateOf(!ProfileRepository.isComplete(context)) }
    var selectedTheme by remember { mutableStateOf(prefs.getString("selected_theme", "default") ?: "default") }
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
            val manualSteps = ir.ghadmino.stepcounter.activity.ManualActivityRepository.stepsForDate(
                context,
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            )
            if (stepSource == StepSource.PHONE || (stepSource == StepSource.AUTO && StepCounterService.sensorAvailable)) {
                steps = (phoneSteps.toLong() + manualSteps).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            } else if (now - lastHealthRefresh >= 60000L) {
                val hc = try { HealthConnectRepository.todaySteps(context) } catch (_: Exception) { null }
                val baseSteps = hc ?: phoneSteps.toLong()
                steps = (baseSteps + manualSteps).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                lastHealthRefresh = now
            }
            if (steps != lastSavedSteps) {
                StepHistory.saveToday(context, steps)
                lastSavedSteps = steps
            }
            walkingMinutes = ActivityTimeRepository.todayActiveMinutes(context)
            if (now - lastInsightRefresh >= 10000L) {
                insights = ActivityInsightsRepository.calculate(context, goal)
                lastInsightRefresh = now
            }
            delay(1000)
        }
    }

    var lastAdNavigationKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(tab, morePage) {
        val navigationKey = tab.toString() + ":" + (morePage ?: "")
        if (lastAdNavigationKey != null && lastAdNavigationKey != navigationKey) {
            (context as? ComponentActivity)?.let { GhadminoAdsManager.onUserNavigation(it) }
        }
        lastAdNavigationKey = navigationKey
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
                    Text("قدمینو", fontWeight = FontWeight.Bold)
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
                    icon = { Icon(Icons.Default.MoreVert, null) },
                    label = { Text("بیشتر") }
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text("تنظیمات") }
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
                onManualActivityChanged = {
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                    val phoneStepsNow = ActivityAnalyticsRepository.today(context)
                    val manualStepsNow = ir.ghadmino.stepcounter.activity.ManualActivityRepository.stepsForDate(context, today)
                    val baseStepsNow = if (
                        stepSource == StepSource.PHONE ||
                        (stepSource == StepSource.AUTO && StepCounterService.sensorAvailable)
                    ) {
                        phoneStepsNow.toLong()
                    } else {
                        (try { HealthConnectRepository.todaySteps(context) } catch (_: Exception) { null })
                            ?: phoneStepsNow.toLong()
                    }
                    steps = (baseStepsNow + manualStepsNow).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    StepHistory.saveToday(context, steps)
                    walkingMinutes = ActivityTimeRepository.todayActiveMinutes(context) +
                        ir.ghadmino.stepcounter.activity.ManualActivityRepository.minutesForDate(context, today)
                    insights = ActivityInsightsRepository.calculate(context, goal)
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
            3 -> SettingsScreen(
                onProfile = { morePage = "profile" },
                onHistory = { morePage = "history" },
                onHealth = { morePage = "health" },
                onBackup = { morePage = "backup" },
                dailyGoal = goal,
                onDailyGoalChanged = {
                    goal = it
                    val updatedProfile = ProfileRepository.load(context).copy(dailyGoal = it)
                    ProfileRepository.save(context, updatedProfile)
                    prefs.edit().putInt("daily_goal", it).apply()
                },
                stepCounterAvailable = StepCounterService.sensorAvailable,
                selectedTheme = selectedTheme,
                onThemeSelected = { id ->
                    selectedTheme = id
                    onThemeChanged(id)
                },
                fontScale = context.getSharedPreferences("ghadmino_ui", Context.MODE_PRIVATE).getFloat("font_scale", 1f),
                onFontScaleChanged = onFontScaleChanged
            )
        }
    }

    when (morePage) {
        "profile" -> FullPageDialog("پروفایل", onClose = { morePage = null }) {
            ProfileScreen {
                val updatedProfile = ProfileRepository.load(context)
                profileName = updatedProfile.name
                goal = updatedProfile.dailyGoal
                morePage = null
            }
        }
        "achievements" -> FullPageDialog("دستاوردها", onClose = { morePage = null }) {
            AchievementsScreen()
        }
        "level" -> FullPageDialog("سطح و XP", onClose = { morePage = null }) {
            LevelScreen()
        }
        "extras" -> FullPageDialog("شخصی‌سازی", onClose = { morePage = null }) {
            ProfileExtrasScreen()
        }
        "smart" -> FullPageDialog("گزارش هوشمند", onClose = { morePage = null }) {
            ActivityIntelligenceScreen()
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
        "challenges" -> FullPageDialog("چالش‌ها", onClose = { morePage = null }) { ChallengesScreen() }
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
            FreeFeaturesScreen()
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
    onManualActivityChanged: () -> Unit
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
                String.format(java.util.Locale.US, "%.2f km", distanceKm),
                containerColor = Color(0xFFE8EEF5),
                contentColor = Color(0xFF34465A)
            )
            StatCard(
                Modifier.weight(1f),
                Icons.Default.LocalFireDepartment,
                "کالری",
                calories.toInt().toString() + " kcal",
                containerColor = Color(0xFFE8EEF5),
                contentColor = Color(0xFF34465A)
            )
            StatCard(
                Modifier.weight(1f),
                Icons.Default.DirectionsWalk,
                "زمان راه‌رفتن",
                walkingMinutes.toString() + " دقیقه",
                containerColor = Color(0xFFE8EEF5),
                contentColor = Color(0xFF34465A)
            )
        }

        ManualActivityScreen(onChanged = onManualActivityChanged)

        GhadminoBanner(modifier = Modifier.fillMaxWidth())

    }
}

@Composable
private fun MorePage(
    profileName: String,
    onOpen: (String) -> Unit
) {
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "امکانات قدمینو",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "ابزارها و گزارش‌های برنامه را از این بخش باز کن.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 21.sp
            )
        }
        GhadminoBanner(modifier = Modifier.fillMaxWidth())
        MoreItem("🏆", "دستاوردها", "مدال‌ها و نشان‌هایی که با فعالیت و پیشرفت به دست می‌آوری.") { onOpen("achievements") }
        MoreItem("🎁", "امکانات رایگان", "ابزارهای کاربردی قدمینو که بدون پرداخت در دسترس هستند.") { onOpen("free") }
        MoreItem("⭐", "سطح و XP", "سطح فعلی و امتیاز تجربه‌ای را که کسب کرده‌ای ببین.") { onOpen("level") }
        MoreItem("🎨", "شخصی‌سازی رایگان", "قاب و نشان پروفایل، پنج تم ویجت و آمار تکمیلی فعالیت؛ همه رایگان.") { onOpen("extras") }
        MoreItem("📅", "تقویم فعالیت", "فعالیت روزهای گذشته را انتخاب کن و گزارش هر روز را ببین.") { onOpen("calendar") }
        MoreItem("📊", "گزارش هوشمند", "روند فعالیت، رکوردها و پیش‌بینی پیشرفتت را بررسی کن.") { onOpen("smart") }
        MoreItem("📈", "تحلیل فعالیت", "امتیاز فعالیت، زمان‌های پرتحرک و وضعیت رسیدن به هدف را ببین.") { onOpen("insights") }
        MoreItem("🏃", "تمرین پیاده‌روی", "یک جلسه پیاده‌روی را شروع کن، متوقف کن و ثبت کن.") { onOpen("workout") }
        MoreItem("📆", "برنامه افزایش قدم", "برای افزایش تدریجی قدم‌ها برنامه بساز و پیشرفتت را دنبال کن.") { onOpen("plan") }
        MoreItem("🧰", "۲۰ قابلیت رایگان", "ابزارهای منبع قدم، اتصال سلامت، رکوردها، هدف‌گذاری، خروجی و عیب‌یابی.") { onOpen("free") }
        GhadminoNativeAdCards(videoOnly = true)
        Spacer(Modifier.height(24.dp))
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
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(46.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(Modifier.width(12.dp))
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 25.sp
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 21.sp
                )
            }
            Spacer(Modifier.width(6.dp))
            TextButton(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text("ورود", fontWeight = FontWeight.SemiBold)
            }
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
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
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
    title: String,
    value: String,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier.heightIn(min = 112.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = contentColor)
            Spacer(Modifier.height(6.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1
            )
            Text(
                value,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}
