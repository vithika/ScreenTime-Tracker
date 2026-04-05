package com.example.screentimetracker

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScreenTimeWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                // Fetch data
                val totalMs      = UsageHelper.getTodayUsage(context)
                val totalMinutes = totalMs / (1000 * 60)
                val goalMinutes  = context.getSharedPreferences("screen_time_prefs", Context.MODE_PRIVATE)
                    .getInt("daily_goal_minutes", 120)

                val hours = totalMinutes / 60
                val mins  = totalMinutes % 60
                val timeText = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

                val currentStreak = StreakHelper.getStreak(context)
                val streakEmoji   = StreakHelper.getStreakEmoji(currentStreak)
                val points        = RewardHelper.getPoints(context)

                // Goal status
                val goalText = if (totalMinutes >= goalMinutes) {
                    "⚠️ Goal exceeded!"
                } else {
                    val remaining = goalMinutes - totalMinutes
                    "Goal: ${remaining}m remaining"
                }

                // Update widget UI on main thread
                val views = RemoteViews(context.packageName, R.layout.widget_layout).apply {
                    setTextViewText(R.id.widget_time,   timeText)
                    setTextViewText(R.id.widget_streak, "$streakEmoji $currentStreak day streak")
                    setTextViewText(R.id.widget_goal,   goalText)
                    setTextViewText(R.id.widget_points, "⭐ $points pts")
                }

                // Tap widget to open app
                val intent      = Intent(context, MainActivity::class.java)
                val pendingIntent = android.app.PendingIntent.getActivity(
                    context, 0, intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                            android.app.PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_time, pendingIntent)

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}