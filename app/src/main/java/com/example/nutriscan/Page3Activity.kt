package com.example.nutriscan

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class Page3Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContentView(R.layout.activity_page3)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Kita set padding bottom ke 0 agar layout tidak didorong ke atas oleh Navigation Bar
            // Ini akan membuat tampilan di HP sama dengan di Preview Android Studio
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val getStartedButton = findViewById<FrameLayout>(R.id.get_started_button)
        getStartedButton.setOnClickListener {
            val intent = Intent(this, Page4Activity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
