package ir.ghadmino.stepcounter\n\nimport android.Manifest\nimport android.content.Context\nimport android.content.Intent\nimport android.content.pm.PackageManager\nimport android.os.Build\nimport android.os.Bundle\nimport androidx.activity.ComponentActivity\nimport androidx.activity.compose.setContent\nimport androidx.activity.result.contract.ActivityResultContracts\nimport androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll\nimport androidx.compose.material.icons.Icons\nimport androidx.compose.material.icons.filled.BarChart\nimport androidx.compose.material.icons.filled.DirectionsWalk\nimport androidx.compose.material.icons.filled.LocalFireDepartment\nimport androidx.compose.material.icons.filled.Paid\nimport androidx.compose.material.icons.filled.Route\nimport androidx.compose.material.icons.filled.Settings\nimport androidx.compose.material.icons.filled.Speed\nimport androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector\nimport androidx.compose.runtime.*\nimport androidx.compose.ui.Alignment\nimport androidx.compose.ui.Modifier\nimport androidx.compose.ui.platform.LocalContext\nimport androidx.compose.ui.text.font.FontWeight\nimport androidx.compose.ui.unit.dp\nimport ir.ghadmino.stepcounter.reward.CoinWallet\nimport ir.ghadmino.stepcounter.profile.ProfileRepository\nimport ir.ghadmino.stepcounter.profile.ProfileScreen\nimport ir.ghadmino.stepcounter.profile.ProfileExtrasScreen\nimport ir.ghadmino.stepcounter.achievement.AchievementsScreen\nimport ir.ghadmino.stepcounter.level.LevelScreen\nimport ir.ghadmino.stepcounter.reward.RewardCenter\nimport ir.ghadmino.stepcounter.speed.SpeedTracker\nimport ir.ghadmino.stepcounter.stats.StatsRepository\nimport ir.ghadmino.stepcounter.stats.StatsScreen\nimport ir.ghadmino.stepcounter.step.StepCounterService\nimport ir.ghadmino.stepcounter.step.StepHistory\nimport ir.ghadmino.stepcounter.ui.theme.GhadminoTheme\nimport kotlinx.coroutines.delay\n\nclass MainActivity : ComponentActivity() {\n    private lateinit var speedTracker: SpeedTracker\n    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { speedTracker.start() }\n\n    override fun onCreate(savedInstanceState: Bundle?) {\n        super.onCreate(savedInstanceState)\n        speedTracker = SpeedTracker(this)\n        requestPermissions()\n        setContent {\n            var themeId by remember { mutableStateOf(CoinWallet.selectedTheme(this@MainActivity)) }\n            GhadminoTheme(themeId = themeId) {\n                GhadminoApp(speedTracker) { newTheme ->\n                    CoinWallet.setSelectedTheme(this@MainActivity, newTheme)\n                    themeId = newTheme\n                }\n            }\n        }\n        startStepService()\n    }\n\n    override fun onResume() { super.onResume(); if (::speedTracker.isInitialized) speedTracker.start() }\n    override fun onPause() { if (::speedTracker.isInitialized) speedTracker.stop(); super.onPause() }\n\n    private fun requestPermissions() {\n        val permissions = mutableListOf<String>()\n        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)\n        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.POST_NOTIFICATIONS)\n        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {\n            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION); permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)\n        }\n        if (permissions.isNotEmpty()) permissionLauncher.launch(permissions.toTypedArray())\n    }\n\n    private fun startStepService() {\n        val intent = Intent(this, StepCounterService::class.java)\n        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)\n    }\n}\n\n@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GhadminoApp(speedTracker: SpeedTracker, onThemeChanged: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ghadmino_ui", Context.MODE_PRIVATE) }
    var goal by remember { mutableIntStateOf(prefs.getInt("daily_goal", 8000)) }
    var steps by remember { mutableIntStateOf(StepCounterService.todaySteps) }
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
    val snackbarHostState = remember { SnackbarHostState() }

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
                    prefs.edit().putInt("daily_goal", it).apply()
                }
            )
            1 -> StatsScreen(
                StatsRepository.load(context, goal),
                goal
            )
            2 -> RewardCenter(
                coins = coins,
                adAvailable = false,
                onCoinsChanged = { coins = CoinWallet.balance(context) },
                selectedTheme = selectedTheme,
                onWatchAd = {
                    info = "تبلیغ جایزه‌ای در مرحله نهایی تبلیغات متصل می‌شود."
                },
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
                LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth())
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
        MoreItem("📅", "تاریخچه", "مشاهده قدم‌های روزهای اخیر") { onOpen("history") }
        MoreItem("🎯", "تنظیم هدف", "تغییر هدف روزانه قدم‌ها") { onOpen("goal") }
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
    BasicAlertDialog(onDismissRequest = onClose) {
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
