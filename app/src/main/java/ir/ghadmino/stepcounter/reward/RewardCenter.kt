package ir.ghadmino.stepcounter.reward

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RewardCenter(
    coins: Int,
    adAvailable: Boolean,
    onWatchAd: () -> Unit,
    onBuyFreeze: () -> Unit,
    onBuyTheme: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(18.dp)) {
                Row {
                    Icon(Icons.Default.Paid, null)
                    Spacer(Modifier.width(8.dp))
                    Text("کیف پول سکه", style = MaterialTheme.typography.titleLarge)
                }
                Text(coins.toString() + " سکه", style = MaterialTheme.typography.displaySmall)
                Text("سکه‌ها برای قابلیت‌های ویژه و فروشگاه آینده ذخیره می‌شوند.")
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("تبلیغ جایزه‌ای", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(6.dp))
                Text("پس از اتصال شناسه تبلیغ، با تماشای کامل تبلیغ ۱۰۰ سکه واقعی دریافت می‌شود.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = onWatchAd, enabled = adAvailable, Modifier.fillMaxWidth()) {
                    Text(if (adAvailable) "تماشای تبلیغ +۱۰۰ سکه" else "تبلیغ هنوز به SDK متصل نشده")
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row {
                    Icon(Icons.Default.Star, null)
                    Spacer(Modifier.width(8.dp))
                    Text("پاداش فعالیت", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(5.dp))
                Text("هر ۱۰۰۰ قدم = ۵ سکه")
                Text("رسیدن به هدف روزانه = ۲۰ سکه")
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row {
                    Icon(Icons.Default.ShoppingCart, null)
                    Spacer(Modifier.width(8.dp))
                    Text("فروشگاه قدمینو", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onBuyFreeze, Modifier.fillMaxWidth()) { Text("محافظ زنجیره — ۲۵۰ سکه") }
                OutlinedButton(onClick = onBuyTheme, Modifier.fillMaxWidth()) { Text("تم ویژه — ۱۰۰۰ سکه") }
            }
        }
    }
}
