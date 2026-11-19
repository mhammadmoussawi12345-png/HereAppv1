package com.hereapp.messenger.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object MessageNotificationChannels {
    const val CHANNEL_DIRECT_MESSAGES = "hereapp_direct_messages"

    fun ensureChannelsCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channel = NotificationChannel(
                CHANNEL_DIRECT_MESSAGES,
                "HereApp Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time notifications for new messages and conversation activity."
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
