package ir.ghadmino.stepcounter.analytics

import android.content.Context
import ir.ghadmino.stepcounter.profile.ProfileRepository
import ir.ghadmino.stepcounter.speed.SpeedHistoryRepository
import ir.ghadmino.stepcounter.step.StepCounterService
import ir.ghadmino.stepcounter.step.StepHistory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

data class ActivityReport(val today:Int,val goal:Int,val progress:Int,val distanceKm:Double,val calories:Int,val week:Int,val previousWeek:Int,val change:Int,val month:Int,val monthAverage:Int,val average90:Int,val best:Pair<String,Int>,val active30:Int,val streak:Int,val minutesToGoal:Int,val rate:Int)

object ActivityAnalyticsRepository {
 private const val PREFS="ghadmino_activity_intelligence"
 private const val LAST="last_activity_millis"
 private fun date(n:Int)=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Calendar.getInstance().apply{add(Calendar.DAY_OF_YEAR,-n)}.time)
 fun today(c:Context)=max(StepCounterService.todaySteps,StepCounterService.persistedTodaySteps(c))
 fun markActivity(c:Context){c.getSharedPreferences(PREFS,0).edit().putLong(LAST,System.currentTimeMillis()).apply()}
 fun lastActivity(c:Context)=c.getSharedPreferences(PREFS,0).getLong(LAST,0L)
 fun report(c:Context):ActivityReport{
  val p=ProfileRepository.load(c);val goal=p.dailyGoal.coerceAtLeast(1);val t=today(c);val d30=StepHistory.recent(c,30).toMutableList();val i=d30.indexOfFirst{it.first==date(0)}
  if(i>=0)d30[i]=date(0) to max(d30[i].second,t) else d30.add(0,date(0) to t)
  val d90=StepHistory.recent(c,90);val w=d30.take(7).sumOf{it.second};val pw=d30.drop(7).take(7).sumOf{it.second};val best=(d90+listOf(date(0) to t)).maxByOrNull{it.second}?: (date(0) to t)
  val stride=p.strideCm.coerceIn(30,150)/100.0;val distance=t*stride/1000.0;val calories=(t*stride*p.weightKg.coerceAtLeast(20f)*0.5/1000.0).roundToInt()
  val hours=(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)+Calendar.getInstance().get(Calendar.MINUTE)/60.0).coerceAtLeast(.25);val rate=(t/hours).roundToInt();val remain=(goal-t).coerceAtLeast(0)
  return ActivityReport(t,goal,(t*100L/goal).coerceIn(0,100).toInt(),distance,calories,w,pw,if(pw==0){if(w>0)100 else 0}else((w-pw)*100f/pw).roundToInt(),d30.sumOf{it.second},if(d30.isEmpty())0 else d30.sumOf{it.second}/d30.size,if(d90.isEmpty())0 else d90.sumOf{it.second}/d90.size,best,d30.count{it.second>0},longest(d30),if(remain==0||rate==0)0 else ceil(remain*60.0/rate).toInt(),rate)
 }
 fun speedRecords(c:Context)=SpeedHistoryRepository.bestAverage(c) to SpeedHistoryRepository.bestMaximum(c)
 fun shareText(c:Context):String{val r=report(c);return "گزارش فعالیت قدمینو\nامروز: ${r.today} از ${r.goal} قدم\nپیشرفت: ${r.progress}%\nمسافت: %.2f km\nکالری تخمینی: ${r.calories} kcal\n۷ روز: ${r.week} قدم\nتغییر هفتگی: ${r.change}%\n۳۰ روز: ${r.month} قدم\nمیانگین ۳۰ روز: ${r.monthAverage} قدم\nرکورد: ${r.best.second} قدم در ${r.best.first}"}
 private fun longest(d:List<Pair<String,Int>>):Int{var b=0;var n=0;var prev:String?=null;for((x,v)in d.sortedBy{it.first}){val next=prev!=null&&isNext(prev!!,x);n=if(v>0&&(n==0||next))n+1 else if(v>0)1 else 0;b=max(b,n);prev=x};return b}
 private fun isNext(a:String,b:String)=try{val f=SimpleDateFormat("yyyy-MM-dd",Locale.US);Calendar.getInstance().apply{time=f.parse(a)!!;add(Calendar.DAY_OF_YEAR,1)}.let{f.format(it.time)==b}}catch(_:Exception){false}
}
