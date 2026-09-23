package ir.ghadmino.stepcounter.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(onSaved: () -> Unit) {
    val context = LocalContext.current
    var profile by remember { mutableStateOf(ProfileRepository.load(context)) }
    var name by remember { mutableStateOf(profile.name) }
    var age by remember { mutableStateOf(profile.age.toString()) }
    var height by remember { mutableStateOf(profile.heightCm.toString()) }
    var weight by remember { mutableStateOf(profile.weightKg.toString()) }
    var stride by remember { mutableStateOf(profile.strideCm.toString()) }
    var goal by remember { mutableStateOf(profile.dailyGoal.toString()) }
    var selected by remember { mutableStateOf(ProfileExtrasRepository.selected(context)) }

    val selectedExtra = ProfileExtrasRepository.items.firstOrNull { it.id == selected }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                selectedExtra?.emoji ?: "👤",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (name.isBlank()) "کاربر قدمینو" else name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (selectedExtra == null) "پروفایل پایه"
                            else "آیتم فعال: " + selectedExtra.title
                        )
                    }
                    Icon(Icons.Default.EmojiEvents, null)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "شخصی‌سازی‌ها از فروشگاه پروفایل روی این کارت نمایش داده می‌شوند."
                )
            }
        }

        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("نام") }, singleLine = true)
        OutlinedTextField(age, { age = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("سن") }, singleLine = true)
        OutlinedTextField(height, { height = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("قد (سانتی‌متر)") }, singleLine = true)
        OutlinedTextField(weight, { weight = it.filter { ch -> ch.isDigit() || ch == '.' } }, Modifier.fillMaxWidth(), label = { Text("وزن (کیلوگرم)") }, singleLine = true)
        OutlinedTextField(stride, { stride = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("طول گام (سانتی‌متر)") }, singleLine = true)
        OutlinedTextField(goal, { goal = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("هدف روزانه (قدم)") }, singleLine = true)

        val current = UserProfile(
            name = name,
            age = age.toIntOrNull() ?: profile.age,
            heightCm = height.toIntOrNull() ?: profile.heightCm,
            weightKg = weight.toFloatOrNull() ?: profile.weightKg,
            strideCm = stride.toIntOrNull() ?: profile.strideCm,
            dailyGoal = goal.toIntOrNull() ?: profile.dailyGoal
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("خلاصه سلامت", style = MaterialTheme.typography.titleMedium)
                Text(String.format("BMI: %.1f", current.bmi))
                Text("این عدد فقط یک شاخص تقریبی است.")
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Paid, null)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("شخصی‌سازی فعال", style = MaterialTheme.typography.labelMedium)
                    Text(selectedExtra?.title ?: "هنوز آیتمی انتخاب نشده", fontWeight = FontWeight.Bold)
                    if (selectedExtra != null) Text(selectedExtra.emoji)
                }
            }
        }

        Button(
            onClick = {
                val updated = current
                ProfileRepository.save(context, updated)
                profile = ProfileRepository.load(context)
                onSaved()
            },
            Modifier.fillMaxWidth()
        ) { Text("ذخیره پروفایل") }
    }
}
