package ir.ghadmino.stepcounter.activity

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ManualActivity(
    val id: Long,
    val date: String,
    val title: String,
    val steps: Int,
    val minutes: Int,
    val calories: Int
)

object ManualActivityRepository {
    private const val PREFS = "ghadmino_manual_activity"
    private const val KEY = "items"

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun all(context: Context): List<ManualActivity> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        ManualActivity(
                            id = o.optLong("id"),
                            date = o.optString("date", today()),
                            title = o.optString("title", "فعالیت دستی"),
                            steps = o.optInt("steps", 0),
                            minutes = o.optInt("minutes", 0),
                            calories = o.optInt("calories", 0)
                        )
                    )
                }
            }.sortedByDescending { it.id }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun add(
        context: Context,
        title: String,
        steps: Int,
        minutes: Int,
        calories: Int,
        date: String = today()
    ) {
        val safeSteps = steps.coerceAtLeast(0)
        val safeMinutes = minutes.coerceAtLeast(0)
        val safeCalories = calories.coerceAtLeast(0)
        if (safeSteps == 0 && safeMinutes == 0 && safeCalories == 0) return

        val items = all(context).toMutableList()
        items.add(
            ManualActivity(
                id = System.currentTimeMillis(),
                date = date,
                title = title.trim().ifBlank { "فعالیت دستی" },
                steps = safeSteps,
                minutes = safeMinutes,
                calories = safeCalories
            )
        )
        save(context, items)
    }

    fun remove(context: Context, id: Long) {
        save(context, all(context).filterNot { it.id == id })
    }

    fun stepsForDate(context: Context, date: String): Int =
        all(context).filter { it.date == date }.sumOf { it.steps }

    fun minutesForDate(context: Context, date: String): Int =
        all(context).filter { it.date == date }.sumOf { it.minutes }

    fun caloriesForDate(context: Context, date: String): Int =
        all(context).filter { it.date == date }.sumOf { it.calories }

    private fun save(context: Context, items: List<ManualActivity>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("date", item.date)
                    .put("title", item.title)
                    .put("steps", item.steps)
                    .put("minutes", item.minutes)
                    .put("calories", item.calories)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, array.toString())
            .apply()
    }
}
