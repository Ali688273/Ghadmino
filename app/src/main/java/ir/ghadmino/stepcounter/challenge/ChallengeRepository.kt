package ir.ghadmino.stepcounter.challenge
import android.content.Context
import ir.ghadmino.stepcounter.reward.CoinWallet
import ir.ghadmino.stepcounter.step.StepHistory
data class Challenge(val id:String,val title:String,val description:String,val days:Int,val dailyTarget:Int,val reward:Int)
object ChallengeRepository{
 val challenges=listOf(Challenge("seven_day_5k","چالش ۷ روزه","۷ روز پیاپی حداقل ۵۰۰۰ قدم",7,5000,150),Challenge("seven_day_10k","چالش ۷ روزه حرفه‌ای","۷ روز پیاپی حداقل ۱۰۰۰۰ قدم",7,10000,300),Challenge("thirty_day_8k","چالش ۳۰ روزه","۳۰ روز پیاپی حداقل ۸۰۰۰ قدم",30,8000,1000))
 fun progress(c:Context,ch:Challenge):Int{var n=0;for(i in 0 until ch.days)if(StepHistory.get(c,dateOffset(i))>=ch.dailyTarget)n++ else break;return n}
 fun isClaimed(c:Context,id:String)=c.getSharedPreferences("ghadmino_challenges",Context.MODE_PRIVATE).getBoolean("claimed_"+id,false)
 fun claim(c:Context,ch:Challenge):Boolean{if(progress(c,ch)<ch.days||isClaimed(c,ch.id))return false;CoinWallet.add(c,ch.reward);c.getSharedPreferences("ghadmino_challenges",Context.MODE_PRIVATE).edit().putBoolean("claimed_"+ch.id,true).apply();return true}
 private fun dateOffset(d:Int):String{val x=java.util.Calendar.getInstance();x.add(java.util.Calendar.DAY_OF_YEAR,-d);return java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).format(x.time)}
}