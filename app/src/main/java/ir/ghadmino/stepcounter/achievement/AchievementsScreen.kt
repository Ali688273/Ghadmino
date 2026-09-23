package ir.ghadmino.stepcounter.achievement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.ghadmino.stepcounter.reward.CoinWallet

@Composable
fun AchievementsScreen(onChanged: () -> Unit = {}) {
    val context = LocalContext.current
    var items by remember { mutableStateOf(AchievementRepository.evaluate(context)) }
    var medals by remember { mutableStateOf(MedalRepository.evaluate(context)) }
    var coins by remember { mutableIntStateOf(CoinWallet.balance(context)) }
    var lifetimeSteps by remember { mutableIntStateOf(MedalRepository.lifetimeSteps(context)) }

    val unlockedCount = items.count { it.unlocked }
    val unlockedMedals = medals.count { it.unlocked }
    val nextMedal = medals.firstOrNull { !it.unlocked }

    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, null, Modifier.size(32.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "دستاوردها و مدال‌ها",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(unlockedCount.toString() + " از " + items.size + " دستاورد باز شده")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = (if (items.isEmpty()) 0f else unlockedCount.toFloat() / items.size),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("مدال‌ها: " + unlockedMedals + " از " + medals.size)
                    Text("مجموع قدم ثبت‌شده: " + formatNumber(lifetimeSteps))
                    nextMedal?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "مدال بعدی: " + it.title + " • " +
                                it.requiredAchievements + " دستاورد لازم"
                        )
                    }
                }
            }
        }

        item {
            Text(
                "گالری مدال‌ها",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(medals, key = { "medal_" + it.id }) { medal ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (medal.unlocked)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(medal.emoji, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            medal.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(medal.description)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (medal.unlocked) "✓ باز شده"
                            else "پیشرفت: " + unlockedCount + "/" +
                                medal.requiredAchievements + " دستاورد",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Icon(
                        if (medal.unlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                        contentDescription = null
                    )
                }
            }
        }

        item {
            Text(
                "دستاوردهای قابل دریافت",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(items, key = { "achievement_" + it.id }) { item ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.icon, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(item.description)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "پاداش: " + item.reward + " سکه",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Icon(
                        if (item.unlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                        contentDescription = null
                    )
                }
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Paid, null)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("موجودی سکه", style = MaterialTheme.typography.labelMedium)
                        Text(
                            coins.toString() + " سکه",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text("سکه‌ها برای تم، قاب، نشان و امکانات فروشگاه قابل استفاده‌اند.")
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val updated = AchievementRepository.evaluate(context)
        items = updated
        medals = MedalRepository.evaluate(context)
        coins = CoinWallet.balance(context)
        lifetimeSteps = MedalRepository.lifetimeSteps(context)
        onChanged()
    }
}

private fun formatNumber(value: Int): String =
    "%,d".format(java.util.Locale.US, value).replace(',', '٬')
