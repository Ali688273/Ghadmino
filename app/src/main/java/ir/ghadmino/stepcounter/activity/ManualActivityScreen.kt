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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        Icons.Default.DirectionsWalk,
                        contentDescription = null,
                        modifier = Modifier.padding(7.dp).size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "ثبت فعالیت دستی",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (expanded) "بستن" else "افزودن",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.Add,
                    contentDescription = if (expanded) "بستن" else "افزودن",
                    modifier = Modifier.size(19.dp),
                    tint = MaterialTheme.colorScheme.primary
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
