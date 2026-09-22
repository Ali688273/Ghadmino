package ir.ghadmino.stepcounter.level

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun LevelScreen() {
    val context = LocalContext.current
    var info by remember { mutableStateOf(LevelRepository.get(context)) }

    LaunchedEffect(Unit) { info = LevelRepository.get(context) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("سطح کاربر", style = MaterialTheme.typography.titleLarge)
            Text("سطح " + info.level, style = MaterialTheme.typography.headlineMedium)
            LinearProgressIndicator(progress = { info.progress }, modifier = Modifier.fillMaxWidth())
            Text(info.xp.toString() + " XP • " + info.currentLevelXp + "/" + info.nextLevelXp)
            Text("با قدم‌ها، مأموریت‌ها و دستاوردها XP بگیر و سطح خودت را بالا ببر.")
        }
    }
}
