package ir.ghadmino.stepcounter.ads

import android.app.Activity
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun GhadminoNativeAdCards() {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("پیشنهاد حمایت‌شده")
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    factory = {
                        FrameLayout(it).also { container ->
                            GhadminoAdsManager.loadNativeBanner(activity, container)
                        }
                    }
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("ویدیوی حمایت‌شده")
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    factory = {
                        FrameLayout(it).also { container ->
                            GhadminoAdsManager.loadNativeVideo(activity, container)
                        }
                    }
                )
            }
        }
    }
}
