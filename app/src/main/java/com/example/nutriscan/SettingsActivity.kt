package com.example.nutriscan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            enableReminders(true)
        } else {
            Toast.makeText(this, "Izin notifikasi ditolak. Pengingat tidak bisa muncul.", Toast.LENGTH_LONG).show()
            findViewById<MaterialSwitch>(R.id.switch_reminders)?.isChecked = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        NavigationHelper.setupBottomNavigation(this, "Settings")
        initSettingsLogic()
    }

    private fun initSettingsLogic() {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        
        val tvTargetCalories = findViewById<TextView>(R.id.tv_target_calories_settings)
        val btnLogout = findViewById<LinearLayout>(R.id.btn_logout)
        val menuProfile = findViewById<LinearLayout>(R.id.menu_profile)

        lifecycleScope.launch(Dispatchers.IO) {
            val pref = getSharedPreferences("user_data", Context.MODE_PRIVATE)
            val authPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            
            val dailyGoal = pref.getInt("daily_goal", 0)
            val isGuest = currentUser == null || currentUser.isAnonymous || authPref.getBoolean("skipped_login", false)

            withContext(Dispatchers.Main) {
                if (isGuest) {
                    applyGuestState()
                    tvTargetCalories?.text = "-"
                } else {
                    tvTargetCalories?.text = if (dailyGoal > 0) "$dailyGoal kkal" else "0 kkal"
                    
                    btnLogout?.setOnClickListener { handleLogout(auth) }
                    menuProfile?.setOnClickListener {
                        startActivity(Intent(this@SettingsActivity, EditProfileActivity::class.java))
                        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                    }
                }
            }
        }

        setupActiveSwitches()

        findViewById<ImageButton>(R.id.btn_back_settings)?.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
    }

    private fun applyGuestState() {
        val fadedAlpha = 0.4f
        findViewById<MaterialCardView>(R.id.card_account)?.alpha = fadedAlpha
        findViewById<MaterialCardView>(R.id.card_target)?.alpha = fadedAlpha
        
        findViewById<LinearLayout>(R.id.menu_profile)?.isClickable = false
        findViewById<LinearLayout>(R.id.menu_profile)?.isEnabled = false
    }

    private fun handleLogout(auth: FirebaseAuth) {
        AlertDialog.Builder(this)
            .setTitle("Keluar")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                auth.signOut()
                getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                
                val intent = Intent(this, Page4Activity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun setupActiveSwitches() {
        val sharedPref = getSharedPreferences("SettingsPref", Context.MODE_PRIVATE)
        
        // Mode Gelap
        val switchTheme = findViewById<MaterialSwitch>(R.id.switch_theme)
        val isDarkMode = sharedPref.getBoolean("isDarkMode", false)
        switchTheme?.isChecked = isDarkMode
        switchTheme?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != sharedPref.getBoolean("isDarkMode", false)) {
                sharedPref.edit().putBoolean("isDarkMode", isChecked).apply()
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
            }
        }

        // Pengingat Makan
        val switchReminders = findViewById<MaterialSwitch>(R.id.switch_reminders)
        val isReminderOn = sharedPref.getBoolean("isReminderOn", false)
        switchReminders?.isChecked = isReminderOn
        
        switchReminders?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkNotificationPermission()
            } else {
                enableReminders(false)
            }
        }

        // Laporan Harian
        val switchDailyReport = findViewById<MaterialSwitch>(R.id.switch_daily_report)
        switchDailyReport?.isEnabled = false
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                enableReminders(true)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            enableReminders(true)
        }
    }

    private fun enableReminders(enable: Boolean) {
        val sharedPref = getSharedPreferences("SettingsPref", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean("isReminderOn", enable).apply()
        
        if (enable) {
            ReminderManager.scheduleAllReminders(this)
            Toast.makeText(this, "Pengingat Makan aktif", Toast.LENGTH_SHORT).show()
        } else {
            ReminderManager.cancelAllReminders(this)
            Toast.makeText(this, "Pengingat dimatikan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        NavigationHelper.setupBottomNavigation(this, "Settings")
    }
}
