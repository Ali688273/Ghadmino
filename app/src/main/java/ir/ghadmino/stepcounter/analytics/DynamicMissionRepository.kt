package ir.ghadmino.stepcounter.analytics
import android.content.Context
import ir.ghadmino.stepcounter.level.LevelRepository
import ir.ghadmino.stepcounter.reward.CoinWallet
data class DynamicMission(val id:String,val title:String,val reward:Int)
object DynamicMissionRepository{
 private const val PREFS="ghadmino_dynamic_missions"
 private val missions=listOf(DynamicMission("60","رسیدن به ۶۰٪ هدف امروز",15),DynamicMission("100","رسیدن به هدف کامل امروز",30),DynamicMission("extra","۲۰۰۰ قدم بیشتر از هدف",50))
 private fun today(c:Context)=java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).format(java.util.Date())
 fun list(c:Context):List<Pair<DynamicMission,Boolean>>{val p=c.getSharedPreferences(PREFS,0);if(p.getString("date",null)!=today(c))p.edit().clear().putString("date",today(c)).apply();return missions.map{it to p.getBoolean("claimed_${it.id}",false)}}
 fun ready(c:Context,m:DynamicMission):Boolean{val r=ActivityAnalyticsRepository.report(c);return when(m.id){"60"->r.today*100>=r.goal*60;"100"->r.today>=r.goal;"extra"->r.today>=r.goal+2000;else->false}}
 fun claim(c:Context,m:DynamicMission):Boolean{val p=c.getSharedPreferences(PREFS,0);if(p.getBoolean("claimed_${m.id}",false)||!ready(c,m))return false;CoinWallet.add(c,m.reward);LevelRepository.recordMission(c);p.edit().putBoolean("claimed_${m.id}",true).apply();return true}
}
