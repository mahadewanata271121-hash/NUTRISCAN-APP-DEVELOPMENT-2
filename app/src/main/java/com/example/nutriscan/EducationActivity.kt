package com.example.nutriscan

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog

class EducationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_education)

        // Setup Bottom Navigation
        NavigationHelper.setupBottomNavigation(this, "History")

        // 1. Tombol Back
        val btnBack = findViewById<ImageButton>(R.id.btn_back_education)
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        // 2. Tombol Baca 2 Menit (Featured)
        findViewById<Button>(R.id.btn_read_featured).setOnClickListener {
            showBottomSheet(R.layout.layout_article_detail, R.id.btn_close_article)
        }

        // 3. Mitos vs Fakta
        findViewById<View>(R.id.card_mitos_malam).setOnClickListener {
            showBottomSheet(R.layout.layout_article_mitos_malam, R.id.btn_close_mitos_malam)
        }
        findViewById<View>(R.id.card_mitos_gula).setOnClickListener {
            showBottomSheet(R.layout.layout_article_mitos_gula, R.id.btn_close_mitos_gula)
        }

        // 4. Panduan Pintar
        findViewById<View>(R.id.card_porsi_tangan).setOnClickListener {
            showBottomSheet(R.layout.layout_article_porsi_tangan, R.id.btn_close_porsi)
        }
        findViewById<View>(R.id.card_warung_tips).setOnClickListener {
            showBottomSheet(R.layout.layout_article_warung_tips, R.id.btn_close_warung)
        }
        findViewById<View>(R.id.card_cara_scan).setOnClickListener {
            showBottomSheet(R.layout.layout_article_scan_tips, R.id.btn_close_scan_tips)
        }

        // 5. Eksplorasi Tips (Original)
        findViewById<View>(R.id.item_defisit_click).setOnClickListener {
            showBottomSheet(R.layout.layout_article_defisit, R.id.btn_close_defisit)
        }
        findViewById<View>(R.id.item_karbo_click).setOnClickListener {
            showBottomSheet(R.layout.layout_article_karbo, R.id.btn_close_karbo)
        }
        
        // 6. Superfood Lokal
        findViewById<View>(R.id.item_superfood_click).setOnClickListener {
            showBottomSheet(R.layout.layout_article_superfood, R.id.btn_close_superfood)
        }
    }

    private fun showBottomSheet(layoutResId: Int, closeButtonId: Int) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(layoutResId, null)
        view.findViewById<Button>(closeButtonId)?.setOnClickListener {
            dialog.dismiss()
        }
        dialog.setContentView(view)
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        NavigationHelper.setupBottomNavigation(this, "History")
    }
}
