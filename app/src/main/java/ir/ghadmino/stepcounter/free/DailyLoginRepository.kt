package ir.ghadmino.stepcounter.free

import android.content.Context
import ir.ghadmino.stepcounter.reward.CoinWallet
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class LoginReward(val streak:Int,val reward:Int,val claimed:Boolean)

object DailyLoginRepository {
    private const val PREFS="ghadmino_daily_login"
    fun state(c:Context):LoginReward{
        val p=c.getSharedPreferences(PREFS,0)
        val today=key(0)
        val last=p.getString("last",null)
        val streak=if(last==today)p.getInt("streak",1) else p.getInt("streak",0)
        val reward=(5+(streak.coerceIn(1,7)-1)*3).coerceAtMost(23)
        return LoginReward(streak,reward,p.getBoolean("claimed_$today",false))
    }
    @Synchronized fun claim(c:Context):LoginReward{
        val p=c.getSharedPreferences(PREFS,0)
        val today=key(0)
        if(p.getBoolean("claimed_$today",false)) return state(c)
        val yesterday=key(1)
        val oldLast=p.getString("last",null)
        val oldStreak=p.getInt("streak",0)
        val streak=if(oldLast==yesterday) oldStreak+1 else 1
        val reward=(5+(streak.coerceIn(1,7)-1)*3).coerceAtMost(23)
        if(!CoinWallet.claimRewardOnce(c,"daily_login_v2_$today",reward)) return state(c)
        p.edit().putString("last",today).putInt("streak",streak).putBoolean("claimed_$today",true).apply()
        return LoginReward(streak,reward,true)
    }
    private fun key(offset:Int):String{val c=Calendar.getInstance();c.add(Calendar.DAY_OF_YEAR,-offset);return SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.time)}
}
