package ir.ghadmino.stepcounter.backup

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BackupScreen() {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(BackupRepository.exportJson(context).toByteArray(Charsets.UTF_8))
            }
            message = "پشتیبان با موفقیت ذخیره شد."
        } catch (_: Exception) {
            message = "ذخیره پشتیبان انجام نشد."
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val json = context.contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: throw IllegalArgumentException()
            val count = BackupRepository.importJson(context, json)
            message = "$count مورد از پشتیبان بازیابی شد. برنامه را یک‌بار باز و بسته کن."
        } catch (_: Exception) {
            message = "فایل پشتیبان معتبر نیست یا قابل خواندن نیست."
        }
    }

    Column(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(18.dp)) {
                Text("پشتیبان‌گیری رایگان", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("اطلاعات اصلی قدمینو به‌صورت فایل JSON روی گوشی یا فضای ابری انتخابی خودت ذخیره می‌شود؛ هیچ سرور پولی لازم نیست.")
            }
        }

        Button(
            onClick = { exportLauncher.launch("ghadmino-backup.json") },
            Modifier.fillMaxWidth()
        ) { Text("ساخت فایل پشتیبان") }

        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
            Modifier.fillMaxWidth()
        ) { Text("بازیابی از فایل پشتیبان") }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("چه چیزهایی پشتیبان می‌شوند؟", fontWeight = FontWeight.Bold)
                Text("پروفایل، تنظیمات، سکه‌ها، تاریخچه قدم‌ها، دستاوردها و چالش‌ها، شخصی‌سازی، جلسات تمرین و برنامه ۳۰ روزه.")
                Spacer(Modifier.height(6.dp))
                Text("فایل پشتیبان را فقط در اختیار افراد مورد اعتماد قرار بده.", style = MaterialTheme.typography.bodySmall)
            }
        }

        message?.let {
            Text(it, fontWeight = FontWeight.Bold)
        }
    }
}
