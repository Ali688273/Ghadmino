package ir.ghadmino.stepcounter.free

import android.content.Context
import android.content.Intent
import ir.ghadmino.stepcounter.step.StepHistory
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

object FreeExportRepository {
    fun csv(c: Context): String {
        val builder = StringBuilder("date,steps\n")
        StepHistory.recent(c, 365).forEach { row ->
            builder.append(row.first).append(',').append(row.second).append('\n')
        }
        return builder.toString()
    }

    fun json(c: Context): String {
        val days = JSONArray()
        StepHistory.recent(c, 365).forEach { row ->
            days.put(JSONObject().put("date", row.first).put("steps", row.second.coerceAtLeast(0)))
        }
        return JSONObject().put("version", 1).put("days", days).toString(2)
    }

    fun isValidJson(json: String): Boolean {
        return try {
            val root = JSONObject(json)
            if (root.optInt("version", -1) != 1) {
                false
            } else {
                val days = root.optJSONArray("days")
                if (days == null) {
                    false
                } else {
                    val datePattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}")
                    var valid = true
                    for (index in 0 until days.length()) {
                        val day = days.optJSONObject(index)
                        if (day == null ||
                            !datePattern.matcher(day.optString("date")).matches() ||
                            !day.has("steps") ||
                            day.optLong("steps", -1L) < 0L
                        ) {
                            valid = false
                            break
                        }
                    }
                    valid
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    fun share(c: Context, text: String, mime: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        c.startActivity(
            Intent.createChooser(intent, "اشتراک‌گذاری داده‌های قدمینو")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
