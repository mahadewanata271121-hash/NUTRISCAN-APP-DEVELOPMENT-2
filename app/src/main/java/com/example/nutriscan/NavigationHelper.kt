package com.example.nutriscan

import android.app.ActivityOptions
import android.content.Intent
import android.util.Pair
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

object NavigationHelper {

    private val activityOrder = listOf("Home", "Ai", "Scan", "History", "Settings")

    fun setupBottomNavigation(activity: AppCompatActivity, currentActivity: String) {
        val navHome = activity.findViewById<ImageView>(R.id.nav_home_icon)
        val navAi = activity.findViewById<ImageView>(R.id.nav_ai_icon)
        val navScan = activity.findViewById<ImageView>(R.id.nav_scan_icon)
        val navHistory = activity.findViewById<ImageView>(R.id.nav_history_icon)
        val navSettings = activity.findViewById<ImageView>(R.id.nav_settings_icon)
        val navContainer = activity.findViewById<View>(R.id.bottom_navigation_container)

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val isGuest = currentUser == null || currentUser.isAnonymous

        val navItems = listOf(navHome, navAi, navScan, navHistory, navSettings)
        navItems.forEach { it?.isSelected = false }

        // Apply visual state for guests
        if (isGuest) {
            val guestAlpha = 0.4f
            navAi?.alpha = guestAlpha
            navScan?.alpha = guestAlpha
            navHistory?.alpha = guestAlpha
        }

        when (currentActivity) {
            "Home" -> navHome?.isSelected = true
            "Ai" -> navAi?.isSelected = true
            "Scan" -> navScan?.isSelected = true
            "History" -> navHistory?.isSelected = true
            "Settings" -> navSettings?.isSelected = true
        }

        navHome?.setOnClickListener { navigate(activity, navContainer, currentActivity, "Home", HomeActivity::class.java) }
        
        navAi?.setOnClickListener { 
            if (isGuest) {
                showGuestRestrictedDialog(activity)
            } else if (currentActivity == "Home") {
                (activity as? HomeActivity)?.handleAiClick()
            } else {
                navigate(activity, navContainer, currentActivity, "Ai", HomeActivity::class.java, true) 
            }
        }

        navScan?.setOnClickListener { 
            if (isGuest) {
                showGuestRestrictedDialog(activity)
            } else {
                navigate(activity, navContainer, currentActivity, "Scan", AnalysisHistoryActivity::class.java)
            }
        }

        navHistory?.setOnClickListener { 
            if (isGuest) {
                showGuestRestrictedDialog(activity)
            } else {
                navigate(activity, navContainer, currentActivity, "History", EducationActivity::class.java)
            }
        }

        navSettings?.setOnClickListener { navigate(activity, navContainer, currentActivity, "Settings", SettingsActivity::class.java) }
    }

    private fun showGuestRestrictedDialog(activity: AppCompatActivity) {
        AlertDialog.Builder(activity)
            .setTitle("Akses Terbatas")
            .setMessage("Terima kasih telah mencoba Nutriscan! Mohon maaf, fitur ini merupakan fitur eksklusif bagi pengguna terdaftar agar seluruh progres kesehatan Anda tersimpan secara aman. Silakan buat akun Anda terlebih dahulu untuk menikmati akses penuh.")
            .setPositiveButton("Daftar Sekarang") { _, _ ->
                activity.startActivity(Intent(activity, RegisterActivity::class.java))
            }
            .setNegativeButton("Nanti Saja", null)
            .setCancelable(false)
            .show()
    }

    private fun navigate(activity: AppCompatActivity, navContainer: View?, current: String, target: String, targetClass: Class<*>, openCamera: Boolean = false) {
        if (current == target && !openCamera) return

        val currentIndex = activityOrder.indexOf(current)
        val targetIndex = activityOrder.indexOf(target)

        val intent = Intent(activity, targetClass)
        if (openCamera) intent.putExtra("OPEN_CAMERA", true)

        // Shared Element Transition for Navigation Bar
        val options = if (navContainer != null) {
            ActivityOptions.makeSceneTransitionAnimation(
                activity,
                Pair.create(navContainer, "nav_bar_container")
            )
        } else {
            null
        }

        activity.startActivity(intent, options?.toBundle())

        if (targetIndex > currentIndex) {
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        } else if (targetIndex < currentIndex) {
            activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}
