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
        DynamicMission("60","رسیدن به ۶۰٪ هدف امروز",15),
        DynamicMission("100","رسیدن به هدف کامل امروز",30),
        DynamicMission("extra","۲۰۰۰ قدم بیشتر از هدف",50)
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
            "60"->r.today*100>=r.goal*60
            "100"->r.today>=r.goal
            "extra"->r.today>=r.goal+2000
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