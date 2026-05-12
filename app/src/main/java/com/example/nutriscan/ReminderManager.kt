package com.example.nutriscan

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object ReminderManager {

    fun scheduleAllReminders(context: Context) {
        // Meal Reminders
        scheduleSpecificReminder(context, 8, 0, "MEAL", 100)
        scheduleSpecificReminder(context, 13, 0, "MEAL", 101)
        scheduleSpecificReminder(context, 19, 0, "MEAL", 102)

        // Water Reminders
        scheduleSpecificReminder(context, 10, 0, "WATER", 200)
        scheduleSpecificReminder(context, 15, 0, "WATER", 201)
        scheduleSpecificReminder(context, 21, 0, "WATER", 202)

        // Daily Tip
        scheduleSpecificReminder(context, 9, 0, "TIP", 300)
    }

    private fun scheduleSpecificReminder(context: Context, hour: Int, minute: Int, type: String, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("REMINDER_TYPE", type)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelAllReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        
        val idsToCancel = listOf(100, 101, 102, 200, 201, 202, 300)
        for (id in idsToCancel) {
            val pendingIntent = PendingIntent.getBroadcast(
                context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
