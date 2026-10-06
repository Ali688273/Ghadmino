package ir.ghadmino.stepcounter.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.ghadmino.stepcounter.widget.GhadminoWidgetProvider

@Composable
fun ProfileExtrasScreen(onChanged: () -> Unit = {}) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(ProfileExtrasRepository.selected(context)) }
    val selectedItem = ProfileExtrasRepository.items.firstOrNull { it.id == selected }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("شخصی‌سازی کاملاً رایگان", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("همه قاب‌ها، نشان‌ها و امکانات این بخش بدون سکه و بدون پرداخت در دسترس هستند.")
                    selectedItem?.let {
                        Spacer(Modifier.height(12.dp))
                        Text("آیتم فعال: " + it.emoji + " " + it.title, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("قاب و نشان پروفایل", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(ProfileExtrasRepository.items.filter { it.id.startsWith("frame_") || it.id.startsWith("badge_") }, key = { it.id }) { item ->
            ExtraItemCard(item, selected == item.id) {
                ProfileExtrasRepository.buyOrSelect(context, item.id, item.cost)
                selected = item.id
                GhadminoWidgetProvider.updateAll(context)
                onChanged()
            }
        }

        item {
            Text("امکانات ویژه، بدون قفل", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(ProfileExtrasRepository.items.filter { !it.id.startsWith("frame_") && !it.id.startsWith("badge_") }, key = { it.id }) { item ->
            ExtraItemCard(item, selected == item.id) {
                ProfileExtrasRepository.buyOrSelect(context, item.id, item.cost)
                selected = item.id
                onChanged()
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text("قفل سکه‌ای حذف شده است؛ هیچ قابلیت این صفحه نیاز به پرداخت یا سکه ندارد.", Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun ExtraItemCard(item: ProfileExtra, selected: Boolean, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(item.emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold)
                Text(if (selected) "فعال است" else "رایگان — برای انتخاب لمس کن", style = MaterialTheme.typography.bodySmall)
            }
            if (selected) {
                Icon(Icons.Default.CheckCircle, null)
            } else {
                Button(onClick = onClick) { Text("فعال‌سازی رایگان") }
            }
        }
    }
}
