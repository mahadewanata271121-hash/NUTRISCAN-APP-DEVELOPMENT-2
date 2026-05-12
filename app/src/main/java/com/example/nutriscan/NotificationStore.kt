package com.example.nutriscan

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

data class NotificationModel(
    val title: String,
    val message: String,
    val time: String,
    val type: String // "ALERT", "REMINDER", "TIP"
)

object NotificationStore {
    private const val PREF_NAME = "notifications_pref"
    private const val KEY_NOTIFS = "notifs_list"

    fun saveNotification(context: Context, title: String, message: String, type: String) {
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString(KEY_NOTIFS, null)
        val typeToken = object : TypeToken<MutableList<NotificationModel>>() {}.type
        val list: MutableList<NotificationModel> = if (json == null) mutableListOf() else gson.fromJson(json, typeToken)

        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        list.add(0, NotificationModel(title, message, time, type))
        
        // Keep only last 20 notifications
        if (list.size > 20) list.removeAt(list.size - 1)

        sharedPref.edit().putString(KEY_NOTIFS, gson.toJson(list)).apply()
    }

    fun getNotifications(context: Context): List<NotificationModel> {
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = sharedPref.getString(KEY_NOTIFS, null) ?: return emptyList()
        val typeToken = object : TypeToken<List<NotificationModel>>() {}.type
        return Gson().fromJson(json, typeToken)
    }

    fun clearAll(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
