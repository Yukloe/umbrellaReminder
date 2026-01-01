package com.umbrellareminder.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.umbrellareminder.app.scheduler.NotificationScheduler

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            // Reschedule the daily notification after device reboot
            val scheduler = NotificationScheduler(context)
            scheduler.scheduleDailyNotification()
        }
    }
}
