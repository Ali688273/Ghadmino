package ir.ghadmino.stepcounter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun GhadminoTheme(
    themeId: String = "default",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val light = when (themeId) {
        "ocean" -> lightColorScheme(
            primary = Color(0xFF006C8E), secondary = Color(0xFF41636D),
            tertiary = Color(0xFF635F87), background = Color(0xFFF6FAFC), surface = Color(0xFFF6FAFC)
        )
        "sunset" -> lightColorScheme(
            primary = Color(0xFF9C3D00), secondary = Color(0xFF76574B),
            tertiary = Color(0xFF675F8A), background = Color(0xFFFFF8F5), surface = Color(0xFFFFF8F5)
        )
        "forest" -> lightColorScheme(
            primary = Color(0xFF356A3B), secondary = Color(0xFF536653),
            tertiary = Color(0xFF6A5D2E), background = Color(0xFFF7FAF5), surface = Color(0xFFF7FAF5)
        )
        "royal" -> lightColorScheme(
            primary = Color(0xFF5D3B8C), secondary = Color(0xFF635A6E),
            tertiary = Color(0xFF7B4F00), background = Color(0xFFFAF8FF), surface = Color(0xFFFAF8FF)
        )
        "rose" -> lightColorScheme(
            primary = Color(0xFF9A405C), secondary = Color(0xFF765762),
            tertiary = Color(0xFF765A20), background = Color(0xFFFFF8F9), surface = Color(0xFFFFF8F9)
        )
        else -> lightColorScheme(
            primary = Color(0xFF176B5B), secondary = Color(0xFF4C635D),
            tertiary = Color(0xFF8A5000), background = Color(0xFFF8FAF9), surface = Color(0xFFF8FAF9)
        )
    }

    val dark = when (themeId) {
        "ocean" -> darkColorScheme(primary = Color(0xFF5DD5FF), secondary = Color(0xFFA7CBD5), tertiary = Color(0xFFC9C3FF))
        "sunset" -> darkColorScheme(primary = Color(0xFFFFB38A), secondary = Color(0xFFE8BDB0), tertiary = Color(0xFFD0C7FF))
        "forest" -> darkColorScheme(primary = Color(0xFF9BD59A), secondary = Color(0xFFB8D1B5), tertiary = Color(0xFFD9CA83))
        "royal" -> darkColorScheme(primary = Color(0xFFD7B8FF), secondary = Color(0xFFCDBDDB), tertiary = Color(0xFFFFB95C))
        "rose" -> darkColorScheme(primary = Color(0xFFFFB1C8), secondary = Color(0xFFE4BEC8), tertiary = Color(0xFFE4C47E))
        else -> darkColorScheme(primary = Color(0xFF53DBC0), secondary = Color(0xFFB1CCC5), tertiary = Color(0xFFFFB95C))
    }

    MaterialTheme(
        colorScheme = if (darkTheme) dark else light,
        content = content
    )
}
