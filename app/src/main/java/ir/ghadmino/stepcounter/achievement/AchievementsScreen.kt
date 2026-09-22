package ir.ghadmino.stepcounter.achievement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ir.ghadmino.stepcounter.reward.CoinWallet

@Composable
fun AchievementsScreen(onChanged: () -> Unit = {}) {
    val context = LocalContext.current
    var items by remember { mutableStateOf(AchievementRepository.evaluate(context)) }
    var coins by remember { mutableIntStateOf(CoinWallet.balance(context)) }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("دستاوردها و مدال‌ها", style = MaterialTheme.typography.headlineSmall)
        Text("با پیشرفت واقعی، مدال‌ها باز می‌شوند و پاداش سکه‌ای فقط یک‌بار پرداخت می‌شود.")
        items.forEach { item ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(item.icon, style = MaterialTheme.typography.headlineMedium)
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text(item.description)
                        Text("پاداش: " + item.reward + " سکه", style = MaterialTheme.typography.labelMedium)
                    }
                    Icon(
                        if (item.unlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                        contentDescription = null
                    )
                }
            }
        }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(16.dp)) {
                Text("موجودی", style = MaterialTheme.typography.labelMedium)
                Text(coins.toString() + " سکه", style = MaterialTheme.typography.headlineSmall)
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
