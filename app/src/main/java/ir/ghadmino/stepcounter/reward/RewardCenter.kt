package ir.ghadmino.stepcounter.reward

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.ghadmino.stepcounter.level.LevelRepository
import ir.ghadmino.stepcounter.profile.ProfileRepository
import ir.ghadmino.stepcounter.step.StepCounterService

data class ThemeOffer(val id:String,val title:String,val cost:Int,val emoji:String)

private val themeOffers=listOf(
    ThemeOffer("ocean","اقیانوس",0,"🌊"),
    ThemeOffer("forest","جنگل",0,"🌿"),
    ThemeOffer("sunset","غروب",0,"🌅"),
    ThemeOffer("rose","رز",0,"🌹"),
    ThemeOffer("royal","سلطنتی",0,"👑")
)

@Composable
fun RewardCenter(
    coins:Int,
    selectedTheme:String,
    onBuyFreeze:()->Unit,
    onBuyTheme:(String,Int)->Unit,
    onCoinsChanged:()->Unit={}
){
    val context=LocalContext.current
    var steps by remember{mutableIntStateOf(maxOf(StepCounterService.todaySteps,StepCounterService.persistedTodaySteps(context)))}
    val levelInfo=remember(steps){LevelRepository.get(context)}
    val goal=remember{ProfileRepository.load(context).dailyGoal}

    LaunchedEffect(Unit){
        while(true){
            kotlinx.coroutines.delay(2000)
            steps=maxOf(StepCounterService.todaySteps,StepCounterService.persistedTodaySteps(context))
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)){
            Column(Modifier.padding(20.dp)){
                Text("پاداش و امکانات رایگان",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
                Text("در این نسخه همه قابلیت‌ها، تم‌ها و مسیر پیشرفت برای همه کاربران آزاد است.")
                Spacer(Modifier.height(8.dp))
                Text("قدم امروز: "+steps)
                Text("هدف امروز: "+goal)
            }
        }

        Card(Modifier.fillMaxWidth()){
            Column(Modifier.padding(16.dp)){
                Row{Icon(Icons.Default.EmojiEvents,null);Spacer(Modifier.width(8.dp));Text("سطح و پیشرفت",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
                Spacer(Modifier.height(8.dp))
                Text("سطح "+levelInfo.level+" • "+levelInfo.xp+" XP")
                Text("هر ۱۰۰ قدم = ۱ XP؛ فعالیت بیشتر یعنی پیشرفت بیشتر.")
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress=levelInfo.progress,modifier=Modifier.fillMaxWidth())
            }
        }

        Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer)){
            Column(Modifier.padding(16.dp)){
                Row{Icon(Icons.Default.CheckCircle,null);Spacer(Modifier.width(8.dp));Text("پاداش‌های فعالیت",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
                Spacer(Modifier.height(8.dp))
                Text("رسیدن به ۳۰٪، ۶۰٪ و ۱۰۰٪ هدف روزانه به‌عنوان نقاط پیشرفت ثبت می‌شود.")
                Text("رسیدن به ۳۰۰۰، ۷۰۰۰ و ۱۰۰۰۰ قدم هم در پیشرفت و دستاوردها ثبت می‌شود.")
                Text("برای استفاده از این امکانات نیازی به خرید یا پرداخت نیست.")
            }
        }

        Card(Modifier.fillMaxWidth()){
            Column(Modifier.padding(16.dp)){
                Row{Icon(Icons.Default.Star,null);Spacer(Modifier.width(8.dp));Text("تم‌های رایگان",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
                Spacer(Modifier.height(8.dp))
                themeOffers.forEach{offer->
                    OutlinedButton(
                        onClick={onBuyTheme(offer.id,0)},
                        modifier=Modifier.fillMaxWidth().padding(vertical=3.dp)
                    ){
                        Text(if(selectedTheme==offer.id) offer.emoji+" "+offer.title+" • فعال" else offer.emoji+" "+offer.title+" • رایگان")
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()){
            Column(Modifier.padding(16.dp)){
                Text("چرا این بخش وجود دارد؟",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                Text("سطح و XP فقط برای نمایش روند پیشرفت و ایجاد انگیزه هستند؛ هیچ قابلیت اصلی برنامه پشت آن قفل نمی‌شود.")
                Text("همه امکانات قدم‌شمار، آمار، نمودار، ثبت دستی، پشتیبان‌گیری و شخصی‌سازی برای همه کاربران آزاد است.")
            }
        }
    }
}
