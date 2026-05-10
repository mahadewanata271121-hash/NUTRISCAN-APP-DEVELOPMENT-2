package com.example.nutriscan

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class NutriscanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Memastikan Tema diterapkan secara global sejak aplikasi pertama kali dijalankan
        // Ini mencegah warna berubah menjadi ungu default saat terjadi process death
        val sharedPref = getSharedPreferences("SettingsPref", MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("isDarkMode", false)
        
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}
