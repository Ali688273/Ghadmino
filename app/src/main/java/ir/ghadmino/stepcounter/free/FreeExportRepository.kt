package ir.ghadmino.stepcounter.free

import android.content.Context
import android.content.Intent
import ir.ghadmino.stepcounter.step.StepHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FreeExportRepository {
    fun csv(c:Context):String { val b=StringBuilder("date,steps\n"); StepHistory.recent(c,365).forEach { row -> b.append(row.first).append(',').append(row.second).append('\n') }; return b.toString() }
    fun json(c:Context):String { val body=StepHistory.recent(c,365).joinToString(",") { row -> "{\"date\":\""+row.first+"\",\"steps\":"+row.second+"}" }; return "{\"version\":1,\"days\":["+body+"]}" }
    fun share(c:Context,text:String,mime:String){val i=Intent(Intent.ACTION_SEND).apply{type=mime;putExtra(Intent.EXTRA_TEXT,text);addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)};c.startActivity(Intent.createChooser(i,"اشتراک‌گذاری داده‌های قدمینو").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
}
