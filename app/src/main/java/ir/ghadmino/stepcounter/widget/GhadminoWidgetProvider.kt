package ir.ghadmino.stepcounter.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import ir.ghadmino.stepcounter.MainActivity
import ir.ghadmino.stepcounter.R
import ir.ghadmino.stepcounter.step.StepCounterService

class GhadminoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            updateAll(context)
        }
    }

    companion object {
        const val ACTION_REFRESH = "ir.ghadmino.stepcounter.widget.REFRESH"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, GhadminoWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return

            val steps = StepCounterService.todaySteps
            val goal = context.getSharedPreferences("ghadmino_ui", Context.MODE_PRIVATE)
                .getInt("daily_goal", 8000)
            val progress = if (goal > 0) ((steps * 100L) / goal).coerceIn(0L, 100L) else 0L

            ids.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.widget_ghadmino)
                views.setTextViewText(R.id.widget_steps, steps.toString())
                views.setTextViewText(R.id.widget_goal, "هدف $goal قدم")
                views.setTextViewText(R.id.widget_progress, "$progress٪")
                views.setOnClickPendingIntent(
                    R.id.widget_root,
                    android.app.PendingIntent.getActivity(
                        context,
                        7000 + id,
                        Intent(context, MainActivity::class.java),
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                            android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                )
                manager.updateAppWidget(id, views)
            }
        }
    }
}
