package ir.ghadmino.stepcounter.ads

import android.app.Activity
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Shared Compose banner slot used on the home and statistics screens.
 * Adivery is primary; Ghadmino's own Tapsell Standard Banner is fallback.
 */
@Composable
fun GhadminoBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    AndroidView(
        modifier = modifier.fillMaxWidth().height(50.dp),
        factory = { viewContext ->
            FrameLayout(viewContext).also { container ->
                GhadminoAdsManager.loadBanner(activity, container)
            }
        },
        update = { }
    )
}
