package com.example.nutriscan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("REMINDER_TYPE") ?: "MEAL"
        
        when (type) {
            "MEAL" -> handleMealReminder(context)
            "WATER" -> handleWaterReminder(context)
            "TIP" -> handleTipReminder(context)
        }
    }

    private fun handleMealReminder(context: Context) {
        val title = "Waktunya Makan!"
        val message = "Jangan lupa catat asupan nutrisimu agar target harian tetap terjaga."
        showAndSaveNotification(context, title, message, "REMINDER")
    }

    private fun handleWaterReminder(context: Context) {
        val title = "Waktunya Minum!"
        val message = "Hidrasi itu penting! Yuk, minum segelas air sekarang untuk menjaga fokusmu."
        showAndSaveNotification(context, title, message, "REMINDER")
    }

    private fun handleTipReminder(context: Context) {
        val tips = listOf(
            "Tahukah kamu? Beras merah mengandung serat lebih tinggi dibanding beras putih.",
            "Tips: Kunyah makanan lebih lama agar pencernaan bekerja lebih optimal.",
            "Info Gizi: Protein membantu perbaikan sel otot setelah beraktivitas.",
            "Tips: Hindari minuman manis berlebih untuk menjaga kestabilan gula darah."
        )
        val title = "Edukasi Gizi Hari Ini"
        val message = tips.random()
        showAndSaveNotification(context, title, message, "TIP")
    }

    private fun showAndSaveNotification(context: Context, title: String, message: String, type: String) {
        // 1. Save to internal store for the Notification Page
        NotificationStore.saveNotification(context, title, message, type)

        // 2. Show system notification tray
        val channelId = "nutriscan_reminder_channel"
        val notificationId = System.currentTimeMillis().toInt()

        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Nutriscan Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.lonceng_notif)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }
}
