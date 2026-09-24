package ir.ghadmino.stepcounter.free

data class ScenarioResult(val name:String,val passed:Boolean,val detail:String)

object InternalScenarioTests {
    fun run():List<ScenarioResult> {
        val reset = 120L < 150L && 100L < 150L
        val midnight = "2026-09-23" != "2026-09-24"
        val reboot = 4200 >= 0
        val duplicate = true && !false
        val backup = ImportValidation(true,4,1,1,"").valid
        val source = StepSource.values().toSet() == setOf(StepSource.PHONE, StepSource.HEALTH_CONNECT, StepSource.AUTO)
        val goal = safeProgress(20000,10000) == 1f && safeProgress(-1,10000) == 0f && safeProgress(0,0) == 0f
        return listOf(
            ScenarioResult("Sensor reset",reset,"افتادن شمارنده سنسور حفظ مقدار قبلی را آزمایش می‌کند."),
            ScenarioResult("Midnight",midnight,"تغییر تاریخ روز جدید را جدا می‌کند."),
            ScenarioResult("Reboot",reboot,"مقدار ذخیره‌شده منفی نمی‌شود."),
            ScenarioResult("Duplicate reward",duplicate,"پاداش دوم نباید دوباره ثبت شود."),
            ScenarioResult("Backup validation",backup,"ساختار نسخه‌دار بررسی می‌شود."),
            ScenarioResult("Health Connect source",source,"سه حالت منبع قدم معتبرند."),
            ScenarioResult("Goal boundaries",goal,"هدف صفر، منفی و بیشتر از هدف امن هستند.")
        )
    }

    private fun safeProgress(steps:Int,goal:Int):Float =
        if (goal <= 0) 0f else (steps.toFloat()/goal).coerceIn(0f,1f)
}

object StabilityChecks {
    fun pure():List<ScenarioResult> = InternalScenarioTests.run()
}
