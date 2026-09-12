package ir.ghadmino.stepcounter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF176B5B),
        secondary = Color(0xFF4C635D),
        tertiary = Color(0xFF8A5000),
        background = Color(0xFFF8FAF9),
        surface = Color(0xFFF8FAF9)
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF53DBC0),
        secondary = Color(0xFFB1CCC5),
        tertiary = Color(0xFFFFB95C)
    )

@Composable
fun GhadminoTheme(
    darkTheme: Boolean =
        isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

    MaterialTheme(
        colorScheme =
            if (darkTheme) {
                DarkColors
            } else {
                LightColors
            },
        content = content
    )
}
