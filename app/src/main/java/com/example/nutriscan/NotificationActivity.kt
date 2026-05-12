package com.example.nutriscan

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class NotificationActivity : AppCompatActivity() {
    
    private lateinit var adapter: NotificationAdapter
    private lateinit var rvNotifications: RecyclerView
    private lateinit var emptyStateCard: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        rvNotifications = findViewById(R.id.rv_notifications)
        emptyStateCard = findViewById(R.id.empty_state_card)
        val btnClearAll = findViewById<TextView>(R.id.btn_clear_all)

        setupRecyclerView()
        updateUI()

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        findViewById<MaterialButton>(R.id.btn_go_home).setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        btnClearAll.setOnClickListener {
            NotificationStore.clearAll(this)
            updateUI()
        }
    }

    private fun setupRecyclerView() {
        rvNotifications.layoutManager = LinearLayoutManager(this)
        val notifications = NotificationStore.getNotifications(this)
        adapter = NotificationAdapter(notifications)
        rvNotifications.adapter = adapter
    }

    private fun updateUI() {
        val notifications = NotificationStore.getNotifications(this)
        if (notifications.isEmpty()) {
            rvNotifications.visibility = View.GONE
            emptyStateCard.visibility = View.VISIBLE
        } else {
            rvNotifications.visibility = View.VISIBLE
            emptyStateCard.visibility = View.GONE
            // Re-bind adapter if data changed
            adapter = NotificationAdapter(notifications)
            rvNotifications.adapter = adapter
        }
    }
}
