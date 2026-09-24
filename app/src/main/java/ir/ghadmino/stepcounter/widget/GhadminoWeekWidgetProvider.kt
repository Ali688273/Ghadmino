package ir.ghadmino.stepcounter.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import ir.ghadmino.stepcounter.MainActivity
import ir.ghadmino.stepcounter.R
import ir.ghadmino.stepcounter.step.StepHistory

class GhadminoWeekWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val rows = StepHistory.recent(context, 7).reversed()
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_ghadmino_week)
            val text = if (rows.isEmpty()) "داده‌ای نیست" else rows.joinToString("   ") { row -> row.first.takeLast(2) + ": " + row.second }
            views.setTextViewText(R.id.week_widget_title, "قدمینو — ۷ روز اخیر")
            views.setTextViewText(R.id.week_widget_values, text)
            views.setOnClickPendingIntent(R.id.week_widget_root, PendingIntent.getActivity(context, 9100 + id, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            manager.updateAppWidget(id, views)
        }
    }
}
