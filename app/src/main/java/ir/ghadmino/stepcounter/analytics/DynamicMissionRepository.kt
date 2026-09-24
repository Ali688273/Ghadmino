package ir.ghadmino.stepcounter.analytics

import android.content.Context
import ir.ghadmino.stepcounter.level.LevelRepository
import ir.ghadmino.stepcounter.reward.CoinWallet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DynamicMission(val id:String,val title:String,val reward:Int)

object DynamicMissionRepository{
    private const val PREFS="ghadmino_dynamic_missions"
    private val missions=listOf(
        DynamicMission("3000","۳۰۰۰ قدم امروز",10),
        DynamicMission("7000","۷۰۰۰ قدم امروز",20),
        DynamicMission("10000","۱۰٬۰۰۰ قدم امروز",35),
        DynamicMission("15000","۱۵٬۰۰۰ قدم امروز",60),
        DynamicMission("60","رسیدن به ۶۰٪ هدف امروز",15),
        DynamicMission("100","رسیدن به هدف کامل امروز",30),
        DynamicMission("extra","۲۰۰۰ قدم بیشتر از هدف",50),
        DynamicMission("streak","حفظ زنجیره ۳ روزه",75)
    )

    private fun today()=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date())

    fun list(c:Context):List<Pair<DynamicMission,Boolean>>{
        val p=c.getSharedPreferences(PREFS,0)
        if(p.getString("date",null)!=today()){
            p.edit().clear().putString("date",today()).apply()
        }
        return missions.map{it to p.getBoolean("claimed_"+it.id,false)}
    }

    fun ready(c:Context,m:DynamicMission):Boolean{
        val r=ActivityAnalyticsRepository.report(c)
        return when(m.id){
            "3000"->r.today>=3000
            "7000"->r.today>=7000
            "10000"->r.today>=10000
            "15000"->r.today>=15000
            "60"->r.today*100>=r.goal*60
            "100"->r.today>=r.goal
            "extra"->r.today>=r.goal+2000
            "streak"->r.streak>=3
            else->false
        }
    }

    @Synchronized
    fun claim(c:Context,m:DynamicMission):Boolean{
        val p=c.getSharedPreferences(PREFS,0)
        val key="claimed_"+m.id
        if(p.getBoolean(key,false)||!ready(c,m)) return false
        val claimed=CoinWallet.claimRewardOnce(c,"dynamic_mission_"+today()+"_"+m.id,m.reward)
        if(!claimed) return false
        p.edit().putBoolean(key,true).apply()
        LevelRepository.recordMission(c)
        return true
    }}