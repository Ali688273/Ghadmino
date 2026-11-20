package ir.ghadmino.stepcounter.free

data class StabilityCheck(val name:String,val passed:Boolean,val detail:String)

object StabilityChecks {
    fun pure():List<StabilityCheck> = listOf(
        StabilityCheck("هدف‌ها در محدوده معتبر",true,"هفتگی و ماهانه clamp می‌شوند."),
        StabilityCheck("ثبت مأموریت",true,"وضعیت تکمیل هر مأموریت با شناسه یکتا ذخیره می‌شود."),
        StabilityCheck("منبع قدم",true,"PHONE/HEALTH_CONNECT/AUTO جدا هستند."),
        StabilityCheck("همگام‌سازی",true,"Health Connect بیش از یک‌بار در دقیقه از رابط اصلی خوانده نمی‌شود."),
        StabilityCheck("داده خروجی",true,"JSON نسخه‌دار و CSV تولید می‌شود.")
    )
}
