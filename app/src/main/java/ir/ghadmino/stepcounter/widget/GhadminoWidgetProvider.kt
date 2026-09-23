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
import ir.ghadmino.stepcounter.profile.ProfileRepository
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
        if (intent.action == ACTION_REFRESH) updateAll(context)
    }

    companion object {
        const val ACTION_REFRESH = "ir.ghadmino.stepcounter.widget.REFRESH"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, GhadminoWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return

            val steps = StepCounterService.todaySteps
            val goal = ProfileRepository.load(context).dailyGoal.coerceAtLeast(1)
            val progress = ((steps * 100L) / goal).coerceIn(0L, 100L)
            val selected = ProfileExtrasRepository.selected(context)

            ids.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.widget_ghadmino)
                views.setTextViewText(R.id.widget_steps, steps.toString())
                views.setTextViewText(R.id.widget_goal, "هدف $goal قدم")
                views.setTextViewText(R.id.widget_progress, "$progress٪")

                val background = when (selected) {
                    "widget_theme" -> R.drawable.widget_background_special
                    else -> R.drawable.widget_background
                }
                views.setInt(R.id.widget_root, "setBackgroundResource", background)

                views.setOnClickPendingIntent(
                    R.id.widget_root,
                    PendingIntent.getActivity(
                        context,
                        7000 + id,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                manager.updateAppWidget(id, views)
            }
        }
    }
}
