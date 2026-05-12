package com.example.nutriscan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val sharedPref = context.getSharedPreferences("SettingsPref", Context.MODE_PRIVATE)
            val isReminderOn = sharedPref.getBoolean("isReminderOn", false)
            
            if (isReminderOn) {
                ReminderManager.scheduleAllReminders(context)
            }
        }
    }
}
