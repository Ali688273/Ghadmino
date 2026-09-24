package ir.ghadmino.stepcounter.free

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticsHistoryRepository {
    private const val PREFS="ghadmino_diagnostics"
    fun markSensor(c:Context){c.getSharedPreferences(PREFS,0).edit().putLong("sensor",System.currentTimeMillis()).apply()}
    fun markSave(c:Context){c.getSharedPreferences(PREFS,0).edit().putLong("save",System.currentTimeMillis()).apply()}
    fun text(c:Context,key:String):String{val t=c.getSharedPreferences(PREFS,0).getLong(key,0L);return if(t<=0)"ثبت نشده" else SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).format(Date(t))}
}
