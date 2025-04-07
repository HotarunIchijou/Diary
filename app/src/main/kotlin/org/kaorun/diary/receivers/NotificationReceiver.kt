package org.kaorun.diary.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.kaorun.diary.R

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "task_notifications"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Task Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        notificationManager.createNotificationChannel(channel)

        // Get the notification title from the intent
        val title = intent.getStringExtra("notification_title")

        // Build the notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.task_alt_24px)
            .build()

        // Show the notification
        notificationManager.notify(0, notification)
    }
}
