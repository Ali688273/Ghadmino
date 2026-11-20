package ir.ghadmino.stepcounter.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext

@Composable
fun ProfileExtrasScreen(onChanged: () -> Unit = {}) {
    val context = LocalContext.current
    var frame by remember { mutableStateOf(ProfileExtrasRepository.selectedFrame(context)) }
    var badge by remember { mutableStateOf(ProfileExtrasRepository.selectedBadge(context)) }
    var widgetTheme by remember { mutableStateOf(ProfileExtrasRepository.selectedWidgetTheme(context)) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("شخصی‌سازی رایگان", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("قاب پروفایل، نشان کنار نام و ظاهر ویجت را مستقل از هم انتخاب کن. همه گزینه‌ها رایگان‌اند.")
                    ProfilePreview(frame, badge)
                }
            }
        }

        item { SectionHeading("۱. قاب پروفایل", "قاب انتخابی دور کارت پروفایل نمایش داده می‌شود.") }
        items(ProfileExtrasRepository.frames, key = { "frame_" + it.id }) { item ->
            ChoiceCard(item, frame == item.id) {
                ProfileExtrasRepository.selectFrame(context, item.id)
                frame = item.id
                onChanged()
            }
        }

        item { SectionHeading("۲. نشان پروفایل", "نشان انتخابی کنار نامت در پروفایل دیده می‌شود.") }
        items(ProfileExtrasRepository.badges, key = { "badge_" + it.id }) { item ->
            ChoiceCard(item, badge == item.id) {
                ProfileExtrasRepository.selectBadge(context, item.id)
                badge = item.id
                onChanged()
            }
        }

        item { SectionHeading("۳. تم ویجت", "با انتخاب هر تم، ویجت صفحه اصلی گوشی فوراً به‌روزرسانی می‌شود.") }
        items(ProfileExtrasRepository.widgetThemes, key = { "widget_" + it.id }) { item ->
            ChoiceCard(item, widgetTheme == item.id) {
                ProfileExtrasRepository.selectWidgetTheme(context, item.id)
                widgetTheme = item.id
                onChanged()
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text("قاب، نشان و تم ویجت جداگانه ذخیره می‌شوند؛ می‌توانی هم‌زمان یک قاب، یک نشان و یک تم داشته باشی.",
                    Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun ProfilePreview(frame: String, badge: String) {
    val borderColor = frameColor(frame)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = if (borderColor == Color.Transparent) null else BorderStroke(3.dp, borderColor),
        tonalElevation = 2.dp
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Text("👤", Modifier.padding(12.dp), style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("کاربر قدمینو", fontWeight = FontWeight.Bold)
                Text("پیش‌نمایش پروفایل", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (badge != "none") {
                Text(ProfileExtrasRepository.badges.firstOrNull { it.id == badge }?.emoji.orEmpty(),
                    style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

private fun frameColor(id: String): Color = when (id) {
    "frame_blue" -> Color(0xFF2687D8)
    "frame_gold" -> Color(0xFFD6A52D)
    "frame_violet" -> Color(0xFF8657C8)
    "frame_green" -> Color(0xFF29966A)
    else -> Color.Transparent
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChoiceCard(item: ProfileExtra, selected: Boolean, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(item.emoji, style = MaterialTheme.typography.headlineSmall)
            if (item.id in listOf("green", "ocean", "violet", "sunset", "midnight")) {
                Spacer(Modifier.width(6.dp))
                Box(Modifier.size(width = 34.dp, height = 22.dp).background(widgetThemeColor(item.id), RoundedCornerShape(7.dp)))
            }
            Spacer(Modifier.width(12.dp))
            Text(item.title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            if (selected) {
                Icon(Icons.Default.CheckCircle, contentDescription = "انتخاب شده", tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text("فعال", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            } else {
                Text("انتخاب", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(Modifier.widthIn(max = 150.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

private fun widgetThemeColor(id: String): Color = when (id) {
    "ocean" -> Color(0xFF0284C7)
    "violet" -> Color(0xFF7C3AED)
    "sunset" -> Color(0xFFEA580C)
    "midnight" -> Color(0xFF1E293B)
    else -> Color(0xFF315E4B)
}
