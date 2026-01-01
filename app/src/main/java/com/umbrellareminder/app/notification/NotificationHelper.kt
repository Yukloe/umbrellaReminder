package com.umbrellareminder.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.umbrellareminder.app.MainActivity
import com.umbrellareminder.app.R

class NotificationHelper(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID = "umbrella_reminder_channel"
        const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showUmbrellaNotification(shouldTakeUmbrella: Boolean, locationName: String? = null, temperature: Double? = null) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.notification_title)
        val message = buildNotificationMessage(shouldTakeUmbrella, locationName, temperature)

        // Debug logging
        android.util.Log.d("NotificationHelper", "Showing notification: $message")
        android.util.Log.d("NotificationHelper", "Location: $locationName, Temperature: $temperature")

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, builder.build())
                android.util.Log.d("NotificationHelper", "Notification sent successfully")
            }
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "Failed to send notification: ${e.message}")
            // Handle case where notification permission is not granted
        }
    }

    private fun buildNotificationMessage(shouldTakeUmbrella: Boolean, locationName: String?, temperature: Double?): String {
        val baseMessage = if (shouldTakeUmbrella) {
            context.getString(R.string.notification_take_umbrella)
        } else {
            context.getString(R.string.notification_no_umbrella)
        }
        
        val locationText = locationName?.let { 
            if (it == "Your Location") "" else " in $it" 
        } ?: ""
        val temperatureText = temperature?.let { " • ${it.toInt()}°C" } ?: ""
        
        return "$baseMessage$locationText$temperatureText"
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            true // Notifications are enabled by default on older versions
        }
    }
}
