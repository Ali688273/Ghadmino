package ir.ghadmino.stepcounter.reward

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    onBuyTheme: (id: String, cost: Int) -> Unit
) {
    val context = LocalContext.current
    val steps = StepCounterService.todaySteps

    Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(18.dp)) {
                Row { Icon(Icons.Default.Paid, null); Spacer(Modifier.width(8.dp)); Text("کیف پول سکه", style = MaterialTheme.typography.titleLarge) }
                Text(coins.toString() + " سکه", style = MaterialTheme.typography.displaySmall)
                Text("سکه‌ها را برای بازکردن امکانات و شخصی‌سازی خرج کن.")
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row { Icon(Icons.Default.EmojiEvents, null); Spacer(Modifier.width(8.dp)); Text("مأموریت‌های امروز", style = MaterialTheme.typography.titleLarge) }
                Spacer(Modifier.height(8.dp))
                Text("۳۰۰۰ قدم  •  +۱۰ سکه")
                Button(
                    onClick = { CoinWallet.claimDailyMission(context, "3000", 3000, 10, steps) },
                    enabled = steps >= 3000,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (steps >= 3000) "دریافت پاداش" else "ادامه بده تا ۳۰۰۰ قدم") }

                Spacer(Modifier.height(6.dp))
                Text("۷۰۰۰ قدم  •  +۲۰ سکه")
                Button(
                    onClick = { CoinWallet.claimDailyMission(context, "7000", 7000, 20, steps) },
                    enabled = steps >= 7000,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (steps >= 7000) "دریافت پاداش" else "ادامه بده تا ۷۰۰۰ قدم") }

                Spacer(Modifier.height(6.dp))
                Text("۱۰۰۰۰ قدم  •  +۴۰ سکه")
                Button(
                    onClick = { CoinWallet.claimDailyMission(context, "10000", 10000, 40, steps) },
                    enabled = steps >= 10000,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (steps >= 10000) "دریافت پاداش" else "ادامه بده تا ۱۰۰۰۰ قدم") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row { Icon(Icons.Default.Star, null); Spacer(Modifier.width(8.dp)); Text("دستاوردهای سکه‌ای", style = MaterialTheme.typography.titleLarge) }
                Spacer(Modifier.height(6.dp))
                Text("۱۰ هزار قدم مجموعی  →  +۲۵ سکه")
                Text("۲۵ هزار قدم مجموعی →  +۵۰ سکه")
                Text("۵۰ هزار قدم مجموعی →  +۱۰۰ سکه")
                Text("۱۰۰ هزار قدم مجموعی → +۲۵۰ سکه")
                Text("۲۵۰ هزار قدم مجموعی → +۵۰۰ سکه")
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("تبلیغ جایزه‌ای", style = MaterialTheme.typography.titleLarge) }
                Spacer(Modifier.height(6.dp))
                Text("با تماشای کامل تبلیغ، در مرحله نهایی +۱۰۰ سکه می‌گیری.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = onWatchAd, enabled = adAvailable, Modifier.fillMaxWidth()) {
                    Text(if (adAvailable) "تماشای تبلیغ +۱۰۰ سکه" else "اتصال تبلیغات در مرحله نهایی")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row { Icon(Icons.Default.ShoppingCart, null); Spacer(Modifier.width(8.dp)); Text("فروشگاه قدمینو", style = MaterialTheme.typography.titleLarge) }
                Spacer(Modifier.height(8.dp))
                Text("تم‌های قابل خرید", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                themeOffers.forEach { offer ->
                    key(offer.id) {
                        val unlocked = CoinWallet.isUnlocked(context, "theme_" + offer.id)
                        val active = selectedTheme == offer.id
                        OutlinedButton(
                            onClick = { if (!unlocked) onBuyTheme(offer.id, offer.cost) else onBuyTheme(offer.id, 0) },
                            Modifier.fillMaxWidth()
                        ) {
                            Icon(if (unlocked) Icons.Default.CheckCircle else Icons.Default.Paid, null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (active) offer.emoji + " " + offer.title + " • فعال"
                                else if (unlocked) offer.emoji + " " + offer.title + " • باز شده"
                                else offer.emoji + " " + offer.title + " • " + offer.cost + " سکه"
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = onBuyFreeze, Modifier.fillMaxWidth()) { Text("🛡 محافظ زنجیره — ۲۵۰ سکه") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("مخارج بعدی سکه", style = MaterialTheme.typography.titleMedium)
                Text("• قاب پروفایل و نشان‌های ویژه")
                Text("• ویجت‌های حرفه‌ای")
                Text("• گزارش‌ها و تحلیل‌های پیشرفته")
                Text("• شخصی‌سازی‌های بیشتر")
            }
        }
    }
}
