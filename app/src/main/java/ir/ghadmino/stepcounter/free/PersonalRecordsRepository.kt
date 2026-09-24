package ir.ghadmino.stepcounter.free

import android.content.Context
import ir.ghadmino.stepcounter.activity.ManualActivityRepository
import ir.ghadmino.stepcounter.profile.ProfileRepository
import ir.ghadmino.stepcounter.step.StepHistory
import ir.ghadmino.stepcounter.workout.WorkoutRepository

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
        val rows=StepHistory.recent(context,365)
        val best=rows.maxByOrNull { it.second }
        val profile=ProfileRepository.load(context)
        val workouts=WorkoutRepository.load(context)
        val manual=ManualActivityRepository.all(context)
        val bestMonth=rolling(rows,30)
        val bestDistance=rows.maxOfOrNull { it.second * profile.strideCm / 100000.0 } ?: 0.0
        val activeMinutes=manual.filter { it.date in rows.take(30).map { r -> r.first } }.sumOf { it.minutes } +
            workouts.filter { it.durationMinutes > 0 }.take(30).sumOf { it.durationMinutes }
        return PersonalRecords(
            bestDaySteps=best?.second ?: 0,
            bestDayDate=best?.first ?: "-",
            best7DayTotal=rolling(rows,7),
            best30DayTotal=bestMonth,
            bestMonthTotal=bestMonth,
            longestStreak=longest(rows),
            currentStreak=current(context),
            activeDays30=rows.take(30).count { it.second>0 },
            bestDistanceKm=bestDistance,
            bestWorkoutDistanceMeters=workouts.maxOfOrNull { it.distanceMeters } ?: 0.0,
            bestWorkoutMinutes=workouts.maxOfOrNull { it.durationMinutes } ?: 0,
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

    private fun current(context:Context):Int {
        var n=0
        for(row in StepHistory.recent(context,365)) {
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
