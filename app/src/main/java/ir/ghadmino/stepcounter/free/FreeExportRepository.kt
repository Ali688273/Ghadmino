package ir.ghadmino.stepcounter.free

import android.content.Context
import android.content.Intent
import ir.ghadmino.stepcounter.step.StepHistory
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
        val body = StepHistory.recent(c, 365).joinToString(",") { row ->
            JSONObject()
                .put("date", row.first)
                .put("steps", row.second.coerceAtLeast(0))
                .toString()
        }
        return JSONObject()
            .put("version", 1)
            .put("days", org.json.JSONArray("[$body]"))
            .toString(2)
    }

    fun isValidJson(json: String): Boolean = try {
        val root = JSONObject(json)
        if (root.optInt("version", -1) != 1) return false
        val days = root.optJSONArray("days") ?: return false
        val datePattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}")
        for (index in 0 until days.length()) {
            val day = days.optJSONObject(index) ?: return false
            val date = day.optString("date")
            if (!datePattern.matcher(date).matches()) return false
            if (!day.has("steps") || day.optLong("steps", -1L) < 0L) return false
        }
        true
    } catch (_: Exception) {
        false
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
