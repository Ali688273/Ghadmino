package ir.ghadmino.stepcounter.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ir.ghadmino.stepcounter.reward.CoinWallet

@Composable
fun ProfileExtrasScreen(onChanged: () -> Unit = {}) {
    val context = LocalContext.current
    var coins by remember { mutableIntStateOf(CoinWallet.balance(context)) }
    var selected by remember { mutableStateOf(ProfileExtrasRepository.selected(context)) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("شخصی‌سازی پروفایل", style = MaterialTheme.typography.titleLarge)
        Text("قاب، نشان و امکانات جانبی را با سکه باز کن.")

        ProfileExtrasRepository.items.forEach { item ->
            val unlocked = ProfileExtrasRepository.isUnlocked(context, item.id)
            OutlinedButton(
                onClick = {
                    if (ProfileExtrasRepository.buyOrSelect(context, item.id, item.cost)) {
                        selected = item.id
                        coins = CoinWallet.balance(context)
                        onChanged()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (selected == item.id) item.emoji + " " + item.title + " • فعال"
                    else if (unlocked) item.emoji + " " + item.title + " • باز شده"
                    else item.emoji + " " + item.title + " • " + item.cost + " سکه"
                )
            }
        }

        Text("موجودی: " + coins + " سکه", style = MaterialTheme.typography.labelLarge)
    }
}
