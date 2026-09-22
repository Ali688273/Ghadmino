package ir.ghadmino.stepcounter.reward

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.ghadmino.stepcounter.step.StepCounterService

data class ThemeOffer(val id: String, val title: String, val cost: Int, val emoji: String)

private val themeOffers = listOf(
    ThemeOffer("ocean", "اقیانوس", 500, "🌊"),
    ThemeOffer("forest", "جنگل", 1000, "🌿"),
    ThemeOffer("sunset", "غروب", 1500, "🌅"),
    ThemeOffer("rose", "رز", 2000, "🌹"),
    ThemeOffer("royal", "سلطنتی", 3500, "👑")
)

@Composable
fun RewardCenter(
    coins: Int,
    adAvailable: Boolean,
    selectedTheme: String,
    onWatchAd: () -> Unit,
    onBuyFreeze: () -> Unit,
    onBuyTheme: (id: String, cost: Int) -> Unit,
    onCoinsChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    var steps by remember { mutableIntStateOf(StepCounterService.todaySteps) }

    LaunchedEffect(Unit) {
        while (true) {
            steps = StepCounterService.todaySteps
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(Modifier.padding(20.dp)) {
                Row {
                    Icon(Icons.Default.Paid, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "کیف پول سکه",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    coins.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text("سکه قابل استفاده")
                Spacer(Modifier.height(10.dp))
                Text(
                    "با قدم‌زدن، مأموریت‌ها و دستاوردها سکه جمع کن و برای امکانات برنامه خرج کن."
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row {
                    Icon(Icons.Default.EmojiEvents, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "مأموریت‌های امروز",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(10.dp))

                MissionRow(
                    title = "۳۰۰۰ قدم",
                    reward = 10,
                    completed = steps >= 3000,
                    claimed = isMissionClaimed(context, "3000"),
                    onClaim = {
                        CoinWallet.claimDailyMission(context, "3000", 3000, 10, steps)
                        onCoinsChanged()
                    }
                )
                MissionRow(
                    title = "۷۰۰۰ قدم",
                    reward = 20,
                    completed = steps >= 7000,
                    claimed = isMissionClaimed(context, "7000"),
                    onClaim = {
                        CoinWallet.claimDailyMission(context, "7000", 7000, 20, steps)
                        onCoinsChanged()
                    }
                )
                MissionRow(
                    title = "۱۰۰۰۰ قدم",
                    reward = 40,
                    completed = steps >= 10000,
                    claimed = isMissionClaimed(context, "10000"),
                    onClaim = {
                        CoinWallet.claimDailyMission(context, "10000", 10000, 40, steps)
                        onCoinsChanged()
                    }
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row {
                    Icon(Icons.Default.Star, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "دستاوردهای سکه‌ای",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("۱۰ هزار قدم مجموعی  →  +۲۵ سکه")
                Text("۲۵ هزار قدم مجموعی →  +۵۰ سکه")
                Text("۵۰ هزار قدم مجموعی →  +۱۰۰ سکه")
                Text("۱۰۰ هزار قدم مجموعی → +۲۵۰ سکه")
                Text("۲۵۰ هزار قدم مجموعی → +۵۰۰ سکه")
                Text("۵۰۰ هزار قدم مجموعی → +۷۵۰ سکه")
                Text("۱ میلیون قدم مجموعی → +۱۵۰۰ سکه")
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "تبلیغ جایزه‌ای",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text("پس از اتصال واقعی تبلیغات، با تماشای کامل تبلیغ +۱۰۰ سکه دریافت می‌کنی.")
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onWatchAd,
                    enabled = adAvailable,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (adAvailable) "تماشای تبلیغ +۱۰۰ سکه" else "در مرحله نهایی فعال می‌شود")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row {
                    Icon(Icons.Default.ShoppingCart, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "فروشگاه قدمینو",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("تم‌های قابل خرید", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))

                themeOffers.forEach { offer ->
                    key(offer.id) {
                        val unlocked = CoinWallet.isUnlocked(context, "theme_" + offer.id)
                        val active = selectedTheme == offer.id

                        OutlinedButton(
                            onClick = {
                                if (!unlocked) {
                                    onBuyTheme(offer.id, offer.cost)
                                } else {
                                    onBuyTheme(offer.id, 0)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                if (unlocked) Icons.Default.CheckCircle else Icons.Default.Paid,
                                null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when {
                                    active -> offer.emoji + " " + offer.title + " • فعال"
                                    unlocked -> offer.emoji + " " + offer.title + " • باز شده"
                                    else -> offer.emoji + " " + offer.title + " • " + offer.cost + " سکه"
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onBuyFreeze,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🛡 محافظ زنجیره — ۲۵۰ سکه")
                }
            }
        }
    }
}

@Composable
private fun MissionRow(
    title: String,
    reward: Int,
    completed: Boolean,
    claimed: Boolean,
    onClaim: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Row {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Text("+" + reward + " سکه")
        }
        Spacer(Modifier.height(5.dp))
        Button(
            onClick = onClaim,
            enabled = completed && !claimed,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when {
                    claimed -> "پاداش دریافت شد ✓"
                    completed -> "دریافت پاداش"
                    else -> "ادامه بده تا " + title
                }
            )
        }
    }
}

private fun isMissionClaimed(
    context: android.content.Context,
    missionId: String
): Boolean {
    val prefs = context.getSharedPreferences("ghadmino_coins", android.content.Context.MODE_PRIVATE)
    val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        .format(java.util.Date())
    return prefs.getString("mission_" + missionId + "_date", null) == date
}
