package com.example.nutriscan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

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

        setupPlaceholderSwitches()

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
                // Reset status tamu saat logout agar sistem bersih
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

    private fun setupPlaceholderSwitches() {
        val switches = listOf<MaterialSwitch?>(
            findViewById(R.id.switch_theme),
            findViewById(R.id.switch_reminders),
            findViewById(R.id.switch_daily_report)
        )

        val showNotice = {
            Toast.makeText(this, "Fitur ini akan segera hadir!", Toast.LENGTH_SHORT).show()
        }

        switches.forEach { s ->
            s?.isEnabled = false
            s?.isClickable = false
        }

        findViewById<LinearLayout>(R.id.container_reminders)?.setOnClickListener { showNotice() }
        findViewById<LinearLayout>(R.id.container_daily_report)?.setOnClickListener { showNotice() }
        findViewById<LinearLayout>(R.id.container_theme)?.setOnClickListener { showNotice() }
    }

    override fun onResume() {
        super.onResume()
        NavigationHelper.setupBottomNavigation(this, "Settings")
    }
}
