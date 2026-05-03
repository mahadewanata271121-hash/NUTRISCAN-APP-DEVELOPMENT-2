package com.example.nutriscan

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class NotificationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        // 1. Aksi klik tombol back (ikon panah kiri di header)
        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        // 2. Aksi klik tombol besar "Kembali ke Beranda" di bagian bawah
        val btnGoHome = findViewById<MaterialButton>(R.id.btn_go_home)
        btnGoHome.setOnClickListener {
            // Karena HomeActivity biasanya sudah ada di stack, cukup panggil finish()
            // untuk kembali ke sana.
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
    }
}
