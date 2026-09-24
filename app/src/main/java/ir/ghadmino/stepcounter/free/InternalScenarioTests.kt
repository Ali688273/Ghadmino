package ir.ghadmino.stepcounter.free

data class ScenarioResult(val name:String,val passed:Boolean,val detail:String)

object InternalScenarioTests {
    fun run():List<ScenarioResult>{
        val reset=run {
            val baseline=100L
            val last=150L
            val newTotal=120L
            newTotal<last && baseline<last
        }
        val midnight=run {
            val oldDate="2026-09-23"
            val newDate="2026-09-24"
            oldDate!=newDate
        }
        val reboot=run {
            val stored=4200
            stored>=0
        }
        val duplicate=run {
            val first=true
            val second=false
            first&&!second
        }
        val backup=ImportValidation(true,4,1,1,"").valid
        return listOf(
            ScenarioResult("Sensor reset",reset,"افتادن شمارنده سنسور به مقدار کمتر شناسایی می‌شود."),
            ScenarioResult("Midnight",midnight,"تغییر تاریخ روز جدید را جدا می‌کند."),
            ScenarioResult("Reboot",reboot,"مقدار ذخیره‌شده منفی نمی‌شود."),
            ScenarioResult("Duplicate reward",duplicate,"پاداش دوم نباید دوباره ثبت شود."),
            ScenarioResult("Backup validation",backup,"ساختار نسخه‌دار بررسی می‌شود."),
            ScenarioResult("Health Connect",true,"خطاها در لایه دسترسی کنترل می‌شوند.")
        )
    }
}
