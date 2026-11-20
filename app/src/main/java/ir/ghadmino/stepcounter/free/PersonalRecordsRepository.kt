package ir.ghadmino.stepcounter.free

import android.content.Context
import ir.ghadmino.stepcounter.activity.ManualActivityRepository
import ir.ghadmino.stepcounter.profile.ProfileRepository
import ir.ghadmino.stepcounter.step.StepHistory

data class PersonalRecords(
    val bestDaySteps:Int,
    val bestDayDate:String,
    val best7DayTotal:Int,
    val best30DayTotal:Int,
    val bestMonthTotal:Int,
    val longestStreak:Int,
    val currentStreak:Int,
    val activeDays30:Int,
    val bestDistanceKm:Double,
    val bestWorkoutDistanceMeters:Double,
    val bestWorkoutMinutes:Int,
    val activeMinutes30:Int
)

object PersonalRecordsRepository {
    fun calculate(context: Context): PersonalRecords {
        val rows=StepHistory.recent(context,3650)
        val best=rows.maxByOrNull { it.second }
        val profile=ProfileRepository.load(context)
        val manual=ManualActivityRepository.all(context)
        val best30Day=rolling(rows,30)
        val bestMonth=bestCalendarMonth(rows)
        val bestDistance=rows.maxOfOrNull { it.second * profile.strideCm / 100000.0 } ?: 0.0
        val recentDates = rows.take(30).mapTo(hashSetOf()) { it.first }
        val activeMinutes=manual.filter { it.date in recentDates }.sumOf { it.minutes }
        return PersonalRecords(
            bestDaySteps=best?.second ?: 0,
            bestDayDate=best?.first ?: "-",
            best7DayTotal=rolling(rows,7),
            best30DayTotal=best30Day,
            bestMonthTotal=bestMonth,
            longestStreak=longest(rows),
            currentStreak=current(context),
            activeDays30=rows.take(30).count { it.second>0 },
            bestDistanceKm=bestDistance,
            // Kept for compatibility with existing callers; the removed workout feature no longer records these values.
            bestWorkoutDistanceMeters=0.0,
            bestWorkoutMinutes=0,
            activeMinutes30=activeMinutes
        )
    }

    private fun rolling(rows:List<Pair<String,Int>>,window:Int):Int {
        var best=0
        for(i in rows.indices) {
            var total=0
            for(j in i until minOf(i+window,rows.size)) total+=rows[j].second
            if(total>best) best=total
        }
        return best
    }

    private fun bestCalendarMonth(rows: List<Pair<String, Int>>): Int =
        rows.groupBy { it.first.take(7) }.values.maxOfOrNull { group -> group.sumOf { it.second } } ?: 0

    private fun current(context:Context):Int {
        var n=0
        for(row in StepHistory.recent(context,3650)) {
            if(row.second>0) n++ else break
        }
        return n
    }

    private fun longest(rows:List<Pair<String,Int>>):Int {
        var best=0
        var run=0
        for(row in rows) {
            if(row.second>0) { run++; if(run>best)best=run } else run=0
        }
        return best
    }
}
