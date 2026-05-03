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
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Setup Bottom Navigation
        NavigationHelper.setupBottomNavigation(this, "Settings")

        val currentUser = FirebaseAuth.getInstance().currentUser
        
        val cardAccount = findViewById<MaterialCardView>(R.id.card_account)
        val cardTarget = findViewById<MaterialCardView>(R.id.card_target)
        val cardPreferences = findViewById<MaterialCardView>(R.id.card_preferences)
        val cardInfo = findViewById<MaterialCardView>(R.id.card_info)
        
        val tvTargetCalories = findViewById<TextView>(R.id.tv_target_calories_settings)
        
        val menuProfile = findViewById<LinearLayout>(R.id.menu_profile)
        val btnLogout = findViewById<LinearLayout>(R.id.btn_logout)

        val switchTheme = findViewById<MaterialSwitch>(R.id.switch_theme)
        val switchReminders = findViewById<MaterialSwitch>(R.id.switch_reminders)
        val switchDaily = findViewById<MaterialSwitch>(R.id.switch_daily_report)

        // DISABLING SWITCHES & ADDING POPUP
        val unavailableFeatureAction = {
            AlertDialog.Builder(this)
                .setTitle("Fitur Belum Tersedia")
                .setMessage("Mohon maaf, fitur ini saat ini belum tersedia. Nutriscan masih dalam tahap awal pengembangan. Nantikan pembaruan selanjutnya!")
                .setPositiveButton("Oke", null)
                .show()
        }

        arrayOf(switchTheme, switchReminders, switchDaily).forEach { switch ->
            switch?.let {
                it.isEnabled = false // Makes it look faded and unmovable
                it.isClickable = false
            }
        }

        // To detect "touch" on disabled switches, we set click listeners on their parent containers
        findViewById<LinearLayout>(R.id.container_reminders)?.setOnClickListener { unavailableFeatureAction() }
        findViewById<LinearLayout>(R.id.container_daily_report)?.setOnClickListener { unavailableFeatureAction() }
        findViewById<LinearLayout>(R.id.container_theme)?.setOnClickListener { unavailableFeatureAction() }

        val userPrefs = getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val h = userPrefs.getString("height", "")
        val w = userPrefs.getString("weight", "")
        val b = userPrefs.getString("birthdate", "")
        val g = userPrefs.getString("gender", "")
        val isPhysicalDataComplete = !h.isNullOrEmpty() && !w.isNullOrEmpty() && !b.isNullOrEmpty() && !g.isNullOrEmpty()

        if (currentUser == null || currentUser.isAnonymous) {
            val fadedAlpha = 0.3f
            // We keep account card accessible for logout if it's a guest? 
            // Actually visitor usually doesn't have "Edit Profile". 
            // But let's follow user's UI request.
            cardAccount?.alpha = fadedAlpha
            cardTarget?.alpha = fadedAlpha
            cardPreferences?.alpha = fadedAlpha
            cardInfo?.alpha = fadedAlpha

            tvTargetCalories?.text = "invalid"
            
            menuProfile?.isClickable = false
            btnLogout?.isClickable = true // Keep logout clickable even for guest? or not.
        } else {
            if (!isPhysicalDataComplete) {
                tvTargetCalories?.text = "0 kkal"
            } else {
                val dailyGoal = userPrefs.getInt("daily_goal", 0)
                tvTargetCalories?.text = "$dailyGoal kkal"
            }

            btnLogout.setOnClickListener {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, Page4Activity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }

            menuProfile.setOnClickListener {
                val intent = Intent(this, EditProfileActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            }
        }

        findViewById<ImageButton>(R.id.btn_back_settings).setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
    }

    override fun onResume() {
        super.onResume()
        NavigationHelper.setupBottomNavigation(this, "Settings")
    }
}
