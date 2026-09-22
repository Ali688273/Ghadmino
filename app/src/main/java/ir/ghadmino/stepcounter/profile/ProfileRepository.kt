package ir.ghadmino.stepcounter.profile

import android.content.Context

data class UserProfile(
    val name: String = "",
    val age: Int = 30,
    val heightCm: Int = 170,
    val weightKg: Float = 70f,
    val strideCm: Int = 70,
    val dailyGoal: Int = 8000
) {
    val bmi: Float
        get() {
            val meters = heightCm.coerceAtLeast(100) / 100f
            return weightKg.coerceAtLeast(1f) / (meters * meters)
        }
}

object ProfileRepository {
    private const val PREFS = "ghadmino_profile"

    fun load(context: Context): UserProfile {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return UserProfile(
            name = p.getString("name", "") ?: "",
            age = p.getInt("age", 30),
            heightCm = p.getInt("height_cm", 170),
            weightKg = p.getFloat("weight_kg", 70f),
            strideCm = p.getInt("stride_cm", 70),
            dailyGoal = p.getInt("daily_goal", 8000)
        )
    }

    fun save(context: Context, profile: UserProfile) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("name", profile.name.trim())
            .putInt("age", profile.age.coerceIn(10, 100))
            .putInt("height_cm", profile.heightCm.coerceIn(100, 230))
            .putFloat("weight_kg", profile.weightKg.coerceIn(20f, 250f))
            .putInt("stride_cm", profile.strideCm.coerceIn(30, 150))
            .putInt("daily_goal", profile.dailyGoal.coerceIn(1000, 30000))
            .apply()
    }
}
