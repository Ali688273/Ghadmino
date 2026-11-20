package ir.ghadmino.stepcounter.activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ManualActivityScreen(onChanged: () -> Unit = {}) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DirectionsWalk, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "ثبت فعالیت دستی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (expanded) "برای بستن فرم لمس کن" else "افزودن قدم یا فعالیت ثبت‌نشده",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "بستن" else "باز کردن"
                )
            }
        }

        if (expanded) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("نام فعالیت") },
                placeholder = { Text("مثلاً پیاده‌روی عصر") },
                singleLine = true
            )
            OutlinedTextField(
                value = steps,
                onValueChange = { steps = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("تعداد قدم") },
                singleLine = true
            )
            OutlinedTextField(
                value = minutes,
                onValueChange = { minutes = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("مدت فعالیت (دقیقه)") },
                singleLine = true
            )
            OutlinedTextField(
                value = calories,
                onValueChange = { calories = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("کالری تقریبی") },
                singleLine = true
            )
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
                    expanded = false
                    onChanged()
                },
                enabled = (steps.toIntOrNull() ?: 0) > 0 ||
                    (minutes.toIntOrNull() ?: 0) > 0 ||
                    (calories.toIntOrNull() ?: 0) > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("ثبت فعالیت")
            }
        }
    }
}
