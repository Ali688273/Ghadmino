package ir.ghadmino.stepcounter.ads

import android.app.Activity
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.AndroidView
import ir.ghadmino.stepcounter.reward.CoinWallet

@Composable
fun AdRewardCard(
    onCoinsChanged: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.CardGiftcard, contentDescription = null)
            Text("سکه رایگان با تبلیغ")
            Text("اگر ویدیوی جایزه‌ای تپسل آماده نباشد، ادیوری به‌صورت خودکار امتحان می‌شود.")

            Button(
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    busy = true
                    message = null
                    GhadminoAdsManager.showRewarded(
                        activity = activity,
                        onReward = {
                            CoinWallet.add(activity, 25)
                            onCoinsChanged()
                        },
                        onFinished = {
                            busy = false
                            message = it
                            onCoinsChanged()
                        }
                    )
                }
            ) {
                Text(if (busy) "در حال آماده‌سازی تبلیغ..." else "تماشای تبلیغ و دریافت +۲۵ سکه")
            }

            message?.let { Text(it) }
        }
    }
}

@Composable
fun GhadminoBanner(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = {
            FrameLayout(it).also { container ->
                GhadminoAdsManager.loadBanner(activity, container)
            }
        }
    )
}
