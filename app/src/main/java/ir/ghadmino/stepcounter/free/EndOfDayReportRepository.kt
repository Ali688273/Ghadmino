package ir.ghadmino.stepcounter.free

import android.content.Context
import ir.ghadmino.stepcounter.profile.ProfileRepository
import ir.ghadmino.stepcounter.step.StepHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EndOfDayReport(val steps:Int,val goal:Int,val progress:Int,val distanceKm:Double,val calories:Double,val activeDays7:Int,val weeklyTotal:Int,val bestDay:Int)

object EndOfDayReportRepository {
    fun today(c:Context):EndOfDayReport{
        val p=ProfileRepository.load(c)
        val steps=StepHistory.get(c,today())
        val goal=p.dailyGoal.coerceAtLeast(1)
        val rows=StepHistory.recent(c,7)
        return EndOfDayReport(steps,goal,(steps*100/goal).coerceIn(0,100),steps*p.strideCm/100000.0,steps*p.strideCm*p.weightKg*0.5/100000.0,rows.count{it.second>0},rows.sumOf{it.second},rows.maxOfOrNull{it.second}?:0)
    }
    private fun today()=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date())
}
