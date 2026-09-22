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
                    Icon(Icons.Default.Star, null, Modifier.size(34.dp))
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
                Text(
                    (info.progress * 100).toInt().toString() + "٪ تا سطح بعد",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LevelMetric(Modifier.weight(1f), "XP", info.xp.toString())
            LevelMetric(
                Modifier.weight(1f),
                "تا سطح بعد",
                (info.nextLevelXp - info.xp).coerceAtLeast(0).toString()
            )
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "چطور XP بگیری؟",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("• هر ۱۰۰ قدم در مجموع = ۱ XP")
                Text("• تکمیل مأموریت‌ها = XP اضافه")
                Text("• بازکردن دستاوردها = XP اضافه")
                Spacer(Modifier.height(8.dp))
                Text(
                    "با فعالیت واقعی، سطح به‌صورت خودکار افزایش پیدا می‌کند.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun LevelMetric(modifier: Modifier, title: String, value: String) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
