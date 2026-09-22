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
    var coins by remember { mutableIntStateOf(CoinWallet.balance(context)) }

    val unlockedCount = items.count { it.unlocked }

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
                            Text("دستاوردها و مدال‌ها", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(unlockedCount.toString() + " از " + items.size + " باز شده")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (items.isEmpty()) 0f else unlockedCount.toFloat() / items.size
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        items(items, key = { it.id }) { item ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.icon, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(item.description)
                        Spacer(Modifier.height(4.dp))
                        Text("پاداش: " + item.reward + " سکه", style = MaterialTheme.typography.labelMedium)
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
                        Text(coins.toString() + " سکه", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val updated = AchievementRepository.evaluate(context)
        items = updated
        coins = CoinWallet.balance(context)
        onChanged()
    }
}
