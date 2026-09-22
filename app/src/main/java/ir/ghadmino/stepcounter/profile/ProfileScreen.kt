package ir.ghadmino.stepcounter.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("پروفایل کاربر", style = MaterialTheme.typography.headlineSmall)
        Text("اطلاعات بدنی برای محاسبات دقیق‌تر مسافت و کالری ذخیره می‌شود.")

        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("نام") }, singleLine = true)
        OutlinedTextField(age, { age = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("سن") }, singleLine = true)
        OutlinedTextField(height, { height = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("قد (سانتی‌متر)") }, singleLine = true)
        OutlinedTextField(weight, { weight = it.filter { ch -> ch.isDigit() || ch == '.' } }, Modifier.fillMaxWidth(), label = { Text("وزن (کیلوگرم)") }, singleLine = true)
        OutlinedTextField(stride, { stride = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("طول گام (سانتی‌متر)") }, singleLine = true)
        OutlinedTextField(goal, { goal = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("هدف روزانه (قدم)") }, singleLine = true)

        val current = profile
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("خلاصه سلامت", style = MaterialTheme.typography.titleMedium)
                Text(String.format("BMI: %.1f", current.bmi))
                Text("این عدد فقط یک شاخص تقریبی است.")
            }
        }

        Button(
            onClick = {
                val updated = UserProfile(
                    name = name,
                    age = age.toIntOrNull() ?: profile.age,
                    heightCm = height.toIntOrNull() ?: profile.heightCm,
                    weightKg = weight.toFloatOrNull() ?: profile.weightKg,
                    strideCm = stride.toIntOrNull() ?: profile.strideCm,
                    dailyGoal = goal.toIntOrNull() ?: profile.dailyGoal
                )
                ProfileRepository.save(context, updated)
                profile = ProfileRepository.load(context)
                onSaved()
            },
            Modifier.fillMaxWidth()
        ) { Text("ذخیره پروفایل") }
    }
}
