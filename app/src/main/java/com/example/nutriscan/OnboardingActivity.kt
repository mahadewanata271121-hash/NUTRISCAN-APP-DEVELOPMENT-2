package com.example.nutriscan

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

/**
 * OnboardingActivity - Performance Optimized
 * Menjamin transisi awal yang sangat mulus tanpa beban memori.
 */
class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        // View Caching untuk performa klik instan
        val btnNext = findViewById<FrameLayout>(R.id.next_button)
        
        btnNext.setOnClickListener {
            val intent = Intent(this, Page3Activity::class.java).apply {
                // Gunakan flag untuk mengelola tumpukan activity dengan efisien
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
