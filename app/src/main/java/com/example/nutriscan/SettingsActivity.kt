package com.example.nutriscan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SettingsActivity - Performance Optimized
 * Dioptimalkan untuk pemuatan data profil yang instan dan manajemen memori saat logout.
 */
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

        // Optimasi: Load data fisik di background agar UI tidak stutter
        lifecycleScope.launch(Dispatchers.IO) {
            val pref = getSharedPreferences("user_data", Context.MODE_PRIVATE)
            val h = pref.getString("height", "")
            val w = pref.getString("weight", "")
            val b = pref.getString("birthdate", "")
            val g = pref.getString("gender", "")
            val dailyGoal = pref.getInt("daily_goal", 0)
            
            val isComplete = !h.isNullOrEmpty() && !w.isNullOrEmpty() && !b.isNullOrEmpty() && !g.isNullOrEmpty()
            val isGuest = currentUser == null || currentUser.isAnonymous

            withContext(Dispatchers.Main) {
                if (isGuest) {
                    applyGuestState()
                    tvTargetCalories?.text = "invalid"
                } else {
                    tvTargetCalories?.text = if (isComplete) "$dailyGoal kkal" else "0 kkal"
                    
                    btnLogout.setOnClickListener { handleLogout(auth) }
                    menuProfile.setOnClickListener {
                        startActivity(Intent(this@SettingsActivity, EditProfileActivity::class.java))
                        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                    }
                }
            }
        }

        setupPlaceholderSwitches()

        findViewById<ImageButton>(R.id.btn_back_settings).setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
    }

    private fun applyGuestState() {
        val fadedAlpha = 0.3f
        listOf<MaterialCardView?>(
            findViewById(R.id.card_account), findViewById(R.id.card_target),
            findViewById(R.id.card_preferences), findViewById(R.id.card_info)
        ).forEach { it?.alpha = fadedAlpha }
        
        findViewById<LinearLayout>(R.id.menu_profile)?.isClickable = false
    }

    private fun handleLogout(auth: FirebaseAuth) {
        AlertDialog.Builder(this)
            .setTitle("Keluar")
            .setMessage("Apakah Anda yakin ingin keluar dari akun?")
            .setPositiveButton("Ya") { _, _ ->
                auth.signOut()
                val intent = Intent(this, Page4Activity::class.java).apply {
                    // Membersihkan seluruh history activity agar memori benar-benar bersih
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
            AlertDialog.Builder(this)
                .setTitle("Fitur Segera Hadir")
                .setMessage("Nutriscan sedang terus dikembangkan. Fitur ini akan tersedia di pembaruan versi berikutnya!")
                .setPositiveButton("Oke", null).show()
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
