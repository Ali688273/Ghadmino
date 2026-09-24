package ir.ghadmino.stepcounter.free

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HealthSyncState(val enabled:Boolean,val lastSyncMillis:Long)

object HealthSyncRepository {
    private const val PREFS="ghadmino_health_sync"
    fun load(c:Context)=c.getSharedPreferences(PREFS,0).let{HealthSyncState(it.getBoolean("enabled",true),it.getLong("last_sync",0L))}
    fun setEnabled(c:Context,value:Boolean){c.getSharedPreferences(PREFS,0).edit().putBoolean("enabled",value).apply()}
    fun markSynced(c:Context){c.getSharedPreferences(PREFS,0).edit().putLong("last_sync",System.currentTimeMillis()).apply()}
    fun lastSyncText(c:Context):String{val t=load(c).lastSyncMillis;if(t<=0)return "هنوز همگام‌سازی نشده";return SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(Date(t))}
}
