package com.example.screentimetracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CHANNEL_ID = "screen_time_channel"
    private const val NOTIF_ID = 1001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Time Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when daily screen time goal is exceeded"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun sendGoalExceededNotification(context: Context, goalMinutes: Int) {
        val hours = goalMinutes / 60
        val mins = goalMinutes % 60
        val goalText = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Screen Time Goal Exceeded! 🚨")
            .setContentText("You've exceeded your daily goal of $goalText")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, notification)
    }
}