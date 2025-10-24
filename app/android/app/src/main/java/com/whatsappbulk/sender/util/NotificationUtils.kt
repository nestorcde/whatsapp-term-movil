package com.whatsappbulk.sender.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.whatsappbulk.sender.R

object NotificationUtils {
    const val CHANNEL_ID = "execution-progress"
    const val CHANNEL_NAME = "Ejecución de Campañas"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
    }

    fun buildProgressNotification(
        context: Context,
        title: String,
        content: String,
        progress: Int,
        max: Int,
        ongoing: Boolean = true
    ): Notification {
        ensureChannel(context)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
            .setProgress(max, progress, false)
            .build()
    }
}

