package ir.ghadmino.stepcounter.free

import android.content.Context
import ir.ghadmino.stepcounter.reward.CoinWallet
import ir.ghadmino.stepcounter.step.StepHistory
import java.util.Calendar
import java.util.Locale

data class PeriodicGoalState(val weeklyTarget:Int,val monthlyTarget:Int,val weeklyProgress:Int,val monthlyProgress:Int)
object PeriodicGoalRepository {
    private const val PREFS="ghadmino_periodic_goals"
    fun load(c:Context):PeriodicGoalState { val p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE); val rows=StepHistory.recent(c,31); val dow=(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)+5)%7; return PeriodicGoalState(p.getInt("weekly_target",50000),p.getInt("monthly_target",200000),rows.take(dow+1).sumOf { row -> row.second },rows.filter { row -> row.first.startsWith(monthKey()) }.sumOf { row -> row.second }) }
    fun save(c:Context,w:Int,m:Int){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putInt("weekly_target",w.coerceIn(1000,500000)).putInt("monthly_target",m.coerceIn(5000,2000000)).apply()}
    fun claimWeekly(c:Context)=CoinWallet.claimRewardOnce(c,"weekly_reward_"+weekKey(),50)
    fun claimMonthly(c:Context)=CoinWallet.claimRewardOnce(c,"monthly_reward_"+monthKey(),150)
    private fun weekKey():String{val c=Calendar.getInstance();return String.format(Locale.US,"%04d-%02d",c.get(Calendar.YEAR),c.get(Calendar.WEEK_OF_YEAR))}
    private fun monthKey():String{val c=Calendar.getInstance();return String.format(Locale.US,"%04d-%02d",c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1)}
}
