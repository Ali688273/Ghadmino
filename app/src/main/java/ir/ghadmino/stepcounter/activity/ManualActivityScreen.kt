package ir.ghadmino.stepcounter.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ManualActivityScreen(onChanged: () -> Unit = {}) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }

    val items = remember(refresh) { ManualActivityRepository.all(context) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
                    Row {
                        Icon(Icons.Default.DirectionsWalk, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "ثبت فعالیت دستی",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("اگر گوشی همراهت نبوده یا فعالیتی توسط حسگر ثبت نشده، اینجا ثبتش کن.")
                }
            }
        }

        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("نام فعالیت") },
                placeholder = { Text("مثلاً پیاده‌روی عصر") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = steps,
                onValueChange = { steps = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("تعداد قدم") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = minutes,
                onValueChange = { minutes = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("مدت فعالیت (دقیقه)") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = calories,
                onValueChange = { calories = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("کالری تقریبی") },
                singleLine = true
            )
        }

        item {
            Button(
                onClick = {
                    ManualActivityRepository.add(
                        context = context,
                        title = title,
                        steps = steps.toIntOrNull() ?: 0,
                        minutes = minutes.toIntOrNull() ?: 0,
                        calories = calories.toIntOrNull() ?: 0
                    )
                    title = ""
                    steps = ""
                    minutes = ""
                    calories = ""
                    refresh++
                    onChanged()
                },
                enabled = (steps.toIntOrNull() ?: 0) > 0 ||
                    (minutes.toIntOrNull() ?: 0) > 0 ||
                    (calories.toIntOrNull() ?: 0) > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("ثبت فعالیت")
            }
        }

        item {
            Text(
                "فعالیت‌های ثبت‌شده",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (items.isEmpty()) {
            item { Text("هنوز فعالیت دستی ثبت نشده است.") }
        } else {
            items(items, key = { it.id }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(item.title) },
                        supportingContent = {
                            Text(
                                buildString {
                                    if (item.steps > 0) append(item.steps).append(" قدم")
                                    if (item.minutes > 0) {
                                        if (isNotEmpty()) append(" • ")
                                        append(item.minutes).append(" دقیقه")
                                    }
                                    if (item.calories > 0) {
                                        if (isNotEmpty()) append(" • ")
                                        append(item.calories).append(" kcal")
                                    }
                                    append(" • ").append(formatDate(item.date))
                                }
                            )
                        },
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    ManualActivityRepository.remove(context, item.id)
                                    refresh++
                                    onChanged()
                                }
                            ) {
                                Icon(Icons.Default.Delete, "حذف")
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun formatDate(date: String): String {
    return try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)
            ?: return date
        SimpleDateFormat("yyyy/MM/dd", Locale.US).format(parsed)
    } catch (_: Exception) {
        date
    }
}
