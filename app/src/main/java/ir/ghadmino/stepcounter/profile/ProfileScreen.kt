package ir.ghadmino.stepcounter.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

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
    var selectedFrame by remember { mutableStateOf(ProfileExtrasRepository.selectedFrame(context)) }
    var selectedBadge by remember { mutableStateOf(ProfileExtrasRepository.selectedBadge(context)) }
    val selectedBadgeItem = ProfileExtrasRepository.badges.firstOrNull { it.id == selectedBadge }
    val frameColor = when (selectedFrame) {
        "frame_blue" -> Color(0xFF2687D8)
        "frame_gold" -> Color(0xFFD6A52D)
        "frame_violet" -> Color(0xFF8657C8)
        "frame_green" -> Color(0xFF29966A)
        else -> Color.Transparent
    }

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
                        modifier = Modifier.size(68.dp),
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 4.dp,
                        border = if (frameColor == Color.Transparent) null else BorderStroke(3.dp, frameColor)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "👤",
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(if (selectedFrame == "none") "پروفایل پایه" else "قاب شخصی")
                            if (selectedBadgeItem != null && selectedBadge != "none") {
                                Text(selectedBadgeItem.emoji)
                                Text(selectedBadgeItem.title, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Icon(Icons.Default.EmojiEvents, null)
                }
                Spacer(Modifier.height(10.dp))
                Text("قاب و نشان انتخابی از بخش شخصی‌سازی رایگان در این پروفایل نمایش داده می‌شوند.")
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
