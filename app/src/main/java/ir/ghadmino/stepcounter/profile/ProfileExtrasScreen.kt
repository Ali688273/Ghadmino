package ir.ghadmino.stepcounter.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
fun ProfileExtrasScreen(onChanged: () -> Unit = {}) {
    val context = LocalContext.current
    var coins by remember { mutableIntStateOf(CoinWallet.balance(context)) }
    var selected by remember { mutableStateOf(ProfileExtrasRepository.selected(context)) }
    var message by remember { mutableStateOf<String?>(null) }

    val unlockedCount = ProfileExtrasRepository.items.count {
        ProfileExtrasRepository.isUnlocked(context, it.id)
    }
    val selectedItem = ProfileExtrasRepository.items.firstOrNull { it.id == selected }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(12.dp)
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
                        Icon(Icons.Default.Paid, null, Modifier.size(30.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "فروشگاه شخصی‌سازی",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                unlockedCount.toString() + " از " +
                                    ProfileExtrasRepository.items.size + " آیتم باز شده"
                            )
                        }
                        Text(
                            coins.toString() + " 🪙",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    selectedItem?.let {
                        Spacer(Modifier.height(12.dp))
                        Divider()
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(it.emoji, style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("آیتم فعال", style = MaterialTheme.typography.labelMedium)
                                Text(it.title, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.CheckCircle, null)
                        }
                    }
                }
            }
        }

        item {
            Text(
                "قاب و نشان پروفایل",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(
            ProfileExtrasRepository.items.filter {
                it.id.startsWith("frame_") || it.id.startsWith("badge_")
            },
            key = { it.id }
        ) { item ->
            ExtraItemCard(
                item = item,
                unlocked = ProfileExtrasRepository.isUnlocked(context, item.id),
                selected = selected == item.id,
                onClick = {
                    val success = ProfileExtrasRepository.buyOrSelect(
                        context, item.id, item.cost
                    )
                    if (success) {
                        selected = item.id
                        coins = CoinWallet.balance(context)
                        message = "«" + item.title + "» فعال شد."
                        onChanged()
                    } else {
                        message = "سکه کافی نیست."
                    }
                }
            )
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text(
                "امکانات ویژه",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(
            ProfileExtrasRepository.items.filter {
                !it.id.startsWith("frame_") && !it.id.startsWith("badge_")
            },
            key = { it.id }
        ) { item ->
            ExtraItemCard(
                item = item,
                unlocked = ProfileExtrasRepository.isUnlocked(context, item.id),
                selected = selected == item.id,
                onClick = {
                    val success = ProfileExtrasRepository.buyOrSelect(
                        context, item.id, item.cost
                    )
                    if (success) {
                        selected = item.id
                        coins = CoinWallet.balance(context)
                        message = "«" + item.title + "» فعال شد."
                        onChanged()
                    } else {
                        message = "سکه کافی نیست."
                    }
                }
            )
        }

        message?.let { text ->
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (text == "سکه کافی نیست.")
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer
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
                    Text("موجودی سکه", style = MaterialTheme.typography.labelMedium)
                    Text(
                        coins.toString() + " سکه",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("سکه‌ها از قدم‌زدن، مأموریت‌ها و دستاوردها به دست می‌آیند.")
                }
            }
        }
    }
}

@Composable
private fun ExtraItemCard(
    item: ProfileExtra,
    unlocked: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(item.emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        selected -> "فعال است"
                        unlocked -> "باز شده؛ آماده انتخاب"
                        else -> item.cost.toString() + " سکه"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (selected) {
                Icon(Icons.Default.CheckCircle, null)
            } else {
                Button(onClick = onClick) {
                    if (!unlocked) {
                        Icon(Icons.Default.Lock, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(if (unlocked) "انتخاب" else "باز کردن")
                }
            }
        }
    }
}
