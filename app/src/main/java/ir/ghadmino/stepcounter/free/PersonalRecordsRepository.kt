package ir.ghadmino.stepcounter.free

import android.content.Context
import ir.ghadmino.stepcounter.step.StepHistory

data class PersonalRecords(val bestDaySteps:Int,val bestDayDate:String,val best7DayTotal:Int,val best30DayTotal:Int,val longestStreak:Int,val currentStreak:Int,val activeDays30:Int)

object PersonalRecordsRepository {
    fun calculate(context: Context): PersonalRecords {
        val rows=StepHistory.recent(context,365)
        val best=rows.maxByOrNull { row -> row.second }
        return PersonalRecords(best?.second ?: 0,best?.first ?: "-",rolling(rows,7),rolling(rows,30),longest(rows),current(rows),rows.take(30).count { row -> row.second>0 })
    }
    private fun rolling(rows:List<Pair<String,Int>>,window:Int):Int { var best=0; for(i in rows.indices){ var total=0; for(j in i until minOf(i+window,rows.size)) total+=rows[j].second; if(total>best) best=total }; return best }
    private fun current(context:Context):Int { var n=0; for(row in StepHistory.recent(context,365)){ if(row.second>0)n++ else break }; return n }
    private fun longest(rows:List<Pair<String,Int>>):Int { var best=0; var run=0; for(row in rows){ if(row.second>0){run++; if(run>best)best=run}else run=0 }; return best }
}
