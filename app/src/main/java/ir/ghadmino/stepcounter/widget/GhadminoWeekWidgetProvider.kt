package ir.ghadmino.stepcounter.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import ir.ghadmino.stepcounter.MainActivity
import ir.ghadmino.stepcounter.R
import ir.ghadmino.stepcounter.profile.ProfileExtrasRepository
import ir.ghadmino.stepcounter.step.StepHistory

class GhadminoWeekWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        updateAll(context)
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, GhadminoWeekWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            val rows = StepHistory.recent(context, 7).reversed()
            val text = if (rows.isEmpty()) "داده‌ای نیست" else rows.joinToString("   ") { row -> row.first.takeLast(2) + ": " + row.second }
            val theme = ProfileExtrasRepository.selectedWidgetTheme(context)
            val background = when (theme) {
                "ocean" -> R.drawable.widget_background_ocean
                "violet" -> R.drawable.widget_background_violet
                "sunset" -> R.drawable.widget_background_sunset
                "midnight" -> R.drawable.widget_background_midnight
                else -> R.drawable.widget_background
            }
            ids.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.widget_ghadmino_week)
                views.setTextViewText(R.id.week_widget_title, "قدمینو — ۷ روز اخیر")
                views.setTextViewText(R.id.week_widget_values, text)
                views.setInt(R.id.week_widget_root, "setBackgroundResource", background)
                views.setOnClickPendingIntent(R.id.week_widget_root, PendingIntent.getActivity(context, 9100 + id, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                manager.updateAppWidget(id, views)
            }
        }
    }
}
