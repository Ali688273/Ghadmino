package ir.ghadmino.stepcounter.backup

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupRepository {
    private val prefsNames = listOf(
        "ghadmino_profile",
        "ghadmino_settings",
        "ghadmino_coins",
        "ghadmino_steps",
        "ghadmino_history",
        "ghadmino_challenges",
        "ghadmino_profile_extras",
        "ghadmino_workouts",
        "ghadmino_step_plan",
        "ghadmino_hourly",
        "ghadmino_ui",
        "ghadmino_achievements",
        "ghadmino_level",
        "ghadmino_speed",
        "ghadmino_speed_history",
        "ghadmino_manual_activity",
        "ghadmino_activity_intelligence",
        "ghadmino_dynamic_missions",
        "ghadmino_inactivity_notice"
    )

    fun exportJson(context: Context): String {
        val root = JSONObject()
        root.put("format", "ghadmino_backup")
        root.put("version", 4)
        root.put("scope", "profile,settings,coins,steps,history,achievements,level,speed,speed_history,workouts,manual_activity,step_plan,hourly,ui")
        root.put(
            "created_at",
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        )

        val all = JSONObject()
        prefsNames.forEach { name ->
            val src = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val obj = JSONObject()

            src.all.forEach { (key, value) ->
                when (value) {
                    is String -> obj.put(
                        key,
                        JSONObject().put("type", "string").put("value", value)
                    )

                    is Int -> obj.put(
                        key,
                        JSONObject().put("type", "int").put("value", value)
                    )

                    is Long -> obj.put(
                        key,
                        JSONObject().put("type", "long").put("value", value)
                    )

                    is Float -> obj.put(
                        key,
                        JSONObject().put("type", "float").put("value", value.toDouble())
                    )

                    is Boolean -> obj.put(
                        key,
                        JSONObject().put("type", "boolean").put("value", value)
                    )

                    is Set<*> -> obj.put(
                        key,
                        JSONObject()
                            .put("type", "strings")
                            .put("value", JSONArray(value.filterIsInstance<String>()))
                    )
                }
            }

            all.put(name, obj)
        }

        root.put("preferences", all)
        return root.toString(2)
    }

    fun importJson(context: Context, json: String): Int {
        val root = JSONObject(json)

        if (root.optString("format") != "ghadmino_backup") {
            throw IllegalArgumentException("فایل پشتیبان قدمینو معتبر نیست.")
        }

        val version = root.optInt("version", 1)
        if (version !in 1..4) {
            throw IllegalArgumentException("نسخه فایل پشتیبان با این نسخه قدمینو سازگار نیست.")
        }

        val all = root.optJSONObject("preferences")
            ?: throw IllegalArgumentException("اطلاعات پشتیبان ناقص است.")

        var count = 0

        all.keys().forEach { name ->
            if (!prefsNames.contains(name)) return@forEach

            val obj = all.optJSONObject(name) ?: return@forEach
            val editor = context
                .getSharedPreferences(name, Context.MODE_PRIVATE)
                .edit()

            obj.keys().forEach { key ->
                val item = obj.optJSONObject(key) ?: return@forEach

                when (item.optString("type")) {
                    "string" -> editor.putString(key, item.optString("value"))
                    "int" -> editor.putInt(key, item.optInt("value"))
                    "long" -> editor.putLong(key, item.optLong("value"))
                    "float" -> editor.putFloat(
                        key,
                        item.optDouble("value").toFloat()
                    )

                    "boolean" -> editor.putBoolean(
                        key,
                        item.optBoolean("value")
                    )

                    "strings" -> {
                        val array = item.optJSONArray("value") ?: JSONArray()
                        val set = buildSet {
                            for (i in 0 until array.length()) {
                                add(array.optString(i))
                            }
                        }
                        editor.putStringSet(key, set)
                    }

                    else -> return@forEach
                }

                count++
            }

            editor.apply()
        }

        return count
    }
}
