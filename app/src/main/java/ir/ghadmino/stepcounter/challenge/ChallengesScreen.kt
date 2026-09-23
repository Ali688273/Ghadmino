package ir.ghadmino.stepcounter.challenge

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.ghadmino.stepcounter.reward.CoinWallet

@Composable
fun ChallengesScreen(onChanged: () -> Unit = {}) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var coins by remember(refresh) { mutableIntStateOf(CoinWallet.balance(context)) }
    var message by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(10.dp)
    ) {
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.EmojiEvents, null, Modifier.size(34.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "چالش‌های قدمینو",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text("با استمرار روزانه چالش‌ها را کامل کن و سکه بگیر.")
                    }
                    Text(coins.toString() + " 🪙", fontWeight = FontWeight.Bold)
                }
            }
        }

        items(ChallengeRepository.challenges, key = { it.id }) { challenge ->
            val progress = remember(refresh, challenge.id) {
                ChallengeRepository.details(context, challenge)
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                challenge.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(challenge.description)
                        }
                        Icon(
                            if (progress.completed) Icons.Default.CheckCircle
                            else Icons.Default.Lock,
                            contentDescription = null
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = progress.percent / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(6.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            progress.completedDays.toString() +
                                " از " + progress.totalDays + " روز"
                        )
                        Text(progress.percent.toString() + "٪")
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("پاداش: " + challenge.reward + " سکه")

                    Spacer(Modifier.height(10.dp))

                    when {
                        progress.claimed -> {
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = { Text("پاداش دریافت شده ✓") }
                            )
                        }
                        progress.completed -> {
                            Button(
                                onClick = {
                                    if (ChallengeRepository.claim(context, challenge)) {
                                        refresh++
                                        message = "پاداش «" + challenge.title + "» دریافت شد."
                                        onChanged()
                                    } else {
                                        message = "این پاداش قبلاً دریافت شده یا چالش کامل نیست."
                                    }
                                }
                            ) {
                                Text("دریافت " + challenge.reward + " سکه")
                            }
                        }
                        else -> {
                            Text(
                                "برای تکمیل، " +
                                    (challenge.days - progress.completedDays).coerceAtLeast(0) +
                                    " روز دیگر لازم است.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        message?.let { text ->
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text,
                        Modifier.padding(14.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "نحوه محاسبه",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "هر چالش بر اساس روزهای متوالی از امروز به عقب بررسی می‌شود. " +
                            "اگر یک روز هدف را از دست بدهی، زنجیره همان چالش از ابتدا محاسبه می‌شود."
                    )
                }
            }
        }
    }
}
