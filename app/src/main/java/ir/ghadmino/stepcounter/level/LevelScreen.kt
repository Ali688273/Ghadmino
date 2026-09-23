package ir.ghadmino.stepcounter.level

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LevelScreen() {
    val context = LocalContext.current
    var info by remember { mutableStateOf(LevelRepository.get(context)) }

    LaunchedEffect(Unit) {
        info = LevelRepository.get(context)
    }

    val levelProgressPercent = (info.progress * 100).toInt().coerceIn(0, 100)
    val remainingXp = (info.nextLevelXp - info.xp).coerceAtLeast(0)
    val currentLevelStart = info.currentLevelXp
    val currentLevelEarned = (info.xp - currentLevelStart).coerceAtLeast(0)

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, Modifier.size(36.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("سطح کاربر", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "سطح " + info.level,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { info.progress },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        currentLevelStart.toString() + " XP",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        info.nextLevelXp.toString() + " XP",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    levelProgressPercent.toString() + "٪ پیشرفت • " +
                        remainingXp.toString() + " XP تا سطح بعد",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LevelMetric(Modifier.weight(1f), "XP کل", info.xp.toString())
            LevelMetric(Modifier.weight(1f), "XP این سطح", currentLevelEarned.toString())
            LevelMetric(Modifier.weight(1f), "تا بعدی", remainingXp.toString())
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "سیستم پیشرفت",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text("هر ۱۰۰ قدم در مجموع = ۱ XP")
                Text("تکمیل مأموریت‌ها = XP اضافه")
                Text("بازکردن دستاوردها = XP اضافه")
                Spacer(Modifier.height(8.dp))
                Text(
                    "هرچه بیشتر فعالیت کنی، سطح بالاتر می‌رود و پیشرفتت در پروفایل قابل پیگیری است.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    "قدم بعدی",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                if (remainingXp == 0) {
                    Text("تبریک! به سطح بعدی رسیده‌ای.")
                } else {
                    Text(
                        remainingXp.toString() +
                            " XP دیگر لازم داری تا سطح " + (info.level + 1) + "."
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelMetric(modifier: Modifier, title: String, value: String) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
