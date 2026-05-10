package com.example.nutriscan

import android.animation.ValueAnimator
import android.content.Context
import android.widget.ProgressBar
import android.view.View
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var greetingText: TextView
    private var backPressedOnce = false
    private var isPhysicalDataComplete = false

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var dayProgressRunnable: Runnable

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) openCamera() else Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        greetingText = findViewById(R.id.greeting_text)

        findViewById<ImageView>(R.id.notification_icon).setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

        findViewById<ImageView>(R.id.user_avatar).setOnClickListener {
            val user = auth.currentUser
            if (user == null || user.isAnonymous) showGuestProfileSheet()
            else {
                startActivity(Intent(this, EditProfileActivity::class.java))
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            }
        }

        setupGreeting()
        setupDateRecyclerView()
        checkDataCompleteness()

        val sharedPref = getSharedPreferences("user_data", Context.MODE_PRIVATE)
        if (!isPhysicalDataComplete && !sharedPref.getBoolean("initial_nudge_shown", false) && auth.currentUser != null && !auth.currentUser!!.isAnonymous) {
            showInitialNudgeDialog()
            sharedPref.edit().putBoolean("initial_nudge_shown", true).apply()
        }

        setupCaloriesProgressBar(true)
        setupNutrientsProgressBar(true)
        setupResetButtons()
        setupBackButtonHandler()
        
        NavigationHelper.setupBottomNavigation(this, "Home")
        checkAndResetDailyData()
        updateButtonsVisualState()
        handleAiIntent(intent)
        checkAndShowTutorial()
        setupDayProgressBar()
    }

    private fun setupGreeting() {
        val name = getSharedPreferences("user_data", Context.MODE_PRIVATE).getString("name", "Dewa")
        greetingText.text = if (auth.currentUser == null || auth.currentUser!!.isAnonymous) "Selamat Pagi Tamu!" else "Selamat Pagi $name!"
    }

    private fun setupDateRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.date_recycler_view) ?: return
        val tvMonthYear = findViewById<TextView>(R.id.tv_month_year)
        val today = Calendar.getInstance()
        tvMonthYear?.text = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(today.time)

        val dates = mutableListOf<DateItem>()
        val tempCal = today.clone() as Calendar
        val dayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
        val daysToSubtract = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        tempCal.add(Calendar.DAY_OF_YEAR, -daysToSubtract)

        for (i in 0 until 7) {
            val isToday = tempCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                          tempCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
            dates.add(DateItem(tempCal.clone() as Calendar, isToday))
            tempCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = DateAdapter(dates)
    }

    private fun setupDayProgressBar() {
        val progressBar = findViewById<ProgressBar>(R.id.day_progress_bar) ?: return
        dayProgressRunnable = object : Runnable {
            override fun run() {
                val cal = Calendar.getInstance()
                val minutes = (cal.get(Calendar.HOUR_OF_DAY) * 60) + cal.get(Calendar.MINUTE)
                if (minutes == 0 && progressBar.progress > 1400) animateProgressReset(progressBar)
                else progressBar.progress = minutes
                handler.postDelayed(this, 60000)
            }
        }
        handler.post(dayProgressRunnable)
    }

    private fun animateProgressReset(progressBar: ProgressBar) {
        ValueAnimator.ofInt(progressBar.progress, 0).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
            addUpdateListener { progressBar.progress = it.animatedValue as Int }
            start()
        }
    }

    override fun onResume() {
        super.onResume()
        checkDataCompleteness()
        updateButtonsVisualState()
        setupCaloriesProgressBar(true)
        setupNutrientsProgressBar(true)
        NavigationHelper.setupBottomNavigation(this, "Home")
    }

    private fun setupCaloriesProgressBar(animate: Boolean) {
        val progressBar = findViewById<CircularProgressBar>(R.id.calories_progress_bar) ?: return
        val sharedPref = getSharedPreferences("user_data", MODE_PRIVATE)
        val target = if (isPhysicalDataComplete) sharedPref.getInt("daily_goal", 2000).toFloat() else 2000f
        val consumed = getSharedPreferences("UserStats", MODE_PRIVATE).getFloat("consumed_calories", 0f)
        
        progressBar.progressMax = target / 0.76f
        if (animate) progressBar.setProgressWithAnimation(consumed.coerceAtMost(target), 1500)
        else progressBar.progress = consumed.coerceAtMost(target)

        val color = if (consumed > target) Color.parseColor("#D32F2F") else ContextCompat.getColor(this, R.color.card_calories_text)
        progressBar.progressBarColor = color
        findViewById<TextView>(R.id.calories_text)?.text = consumed.toInt().toString()
        findViewById<TextView>(R.id.max_calories_label)?.text = target.toInt().toString()
    }

    private fun setupNutrientsProgressBar(animate: Boolean) {
        val sharedPref = getSharedPreferences("user_data", MODE_PRIVATE)
        val targetCal = if (isPhysicalDataComplete) sharedPref.getInt("daily_goal", 2000).toFloat() else 2000f
        val tCarbs = sharedPref.getInt("carbs_goal", (targetCal * 0.55 / 4).toInt())
        val tProt = sharedPref.getInt("protein_goal", (targetCal * 0.20 / 4).toInt())

        val stats = getSharedPreferences("UserStats", MODE_PRIVATE)
        updateNutrientCard(R.id.carbs_card, R.id.carbs_progress, R.id.carbs_value, R.id.carbs_label, stats.getFloat("consumed_carbs", 0f), tCarbs, animate, "#157BBF")
        updateNutrientCard(R.id.protein_card, R.id.protein_progress, R.id.protein_value, R.id.protein_label, stats.getFloat("consumed_protein", 0f), tProt, animate, "#3D9471")
    }

    private fun updateNutrientCard(cardId: Int, barId: Int, valId: Int, labelId: Int, consumed: Float, target: Int, animate: Boolean, color: String) {
        val bar = findViewById<ProgressBar>(barId) ?: return
        val tvVal = findViewById<TextView>(valId) ?: return
        bar.max = target * 100
        tvVal.text = "${consumed.toInt()}g / ${target}g"

        if (animate) {
            ValueAnimator.ofInt(0, consumed.toInt().coerceAtMost(target) * 100).apply {
                duration = 1500
                addUpdateListener { bar.progress = it.animatedValue as Int }
                start()
            }
        } else bar.progress = consumed.toInt().coerceAtMost(target) * 100

        val (progressCol, bgCol) = if (consumed > target) Color.parseColor("#D32F2F") to Color.parseColor("#FFCDD2") 
                                   else Color.BLACK to Color.parseColor(color)
        (bar.progressDrawable?.mutate() as? LayerDrawable)?.let {
            it.findDrawableByLayerId(android.R.id.background)?.setTint(bgCol)
            it.findDrawableByLayerId(android.R.id.progress)?.setTint(progressCol)
        }
    }

    private fun checkDataCompleteness() {
        val pref = getSharedPreferences("user_data", Context.MODE_PRIVATE)
        isPhysicalDataComplete = !pref.getString("height", "").isNullOrEmpty() && !pref.getString("weight", "").isNullOrEmpty()
    }

    private fun updateButtonsVisualState() {
        val isGuest = auth.currentUser == null || auth.currentUser!!.isAnonymous
        findViewById<View>(R.id.calories_card)?.alpha = if (isGuest) 0.4f else 1.0f
        findViewById<View>(R.id.nav_ai_icon)?.alpha = if (isPhysicalDataComplete) 1.0f else 0.4f
    }

    private fun setupResetButtons() {
        findViewById<View>(R.id.btn_reset_calories)?.setOnClickListener {
            if (isPhysicalDataComplete) {
                getSharedPreferences("UserStats", MODE_PRIVATE).edit().clear().apply()
                setupCaloriesProgressBar(true); setupNutrientsProgressBar(true)
            }
        }
    }

    private fun setupBackButtonHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backPressedOnce) finishAffinity() else {
                    backPressedOnce = true
                    Toast.makeText(this@HomeActivity, "Tekan lagi untuk keluar", Toast.LENGTH_SHORT).show()
                    handler.postDelayed({ backPressedOnce = false }, 2000)
                }
            }
        })
    }

    private fun checkAndResetDailyData() {
        val pref = getSharedPreferences("UserStats", Context.MODE_PRIVATE)
        val today = Calendar.getInstance().let { "${it.get(Calendar.YEAR)}-${it.get(Calendar.MONTH)}-${it.get(Calendar.DAY_OF_MONTH)}" }
        if (pref.getString("last_opened_date", "") != "" && pref.getString("last_opened_date", "") != today) {
            pref.edit().clear().putString("last_opened_date", today).apply()
        } else if (pref.getString("last_opened_date", "") == "") pref.edit().putString("last_opened_date", today).apply()
    }

    fun handleAiClick() {
        if (auth.currentUser == null || auth.currentUser!!.isAnonymous) showGuestRestrictedDialog()
        else if (!isPhysicalDataComplete) showIncompleteDataDialog()
        else requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    private fun showGuestRestrictedDialog() {
        AlertDialog.Builder(this).setTitle("Akses Terbatas").setMessage("Silakan buat akun terlebih dahulu.")
            .setPositiveButton("Daftar") { _, _ -> startActivity(Intent(this, RegisterActivity::class.java)) }
            .setNegativeButton("Nanti", null).show()
    }

    private fun showIncompleteDataDialog() {
        AlertDialog.Builder(this).setTitle("Data Belum Lengkap").setMessage("Lengkapi data fisik di profil.")
            .setPositiveButton("Lengkapi") { _, _ -> startActivity(Intent(this, EditProfileActivity::class.java)) }
            .setNegativeButton("Tutup", null).show()
    }

    private fun showInitialNudgeDialog() {
        AlertDialog.Builder(this).setTitle("Lengkapi Profil").setMessage("Lengkapi data fisik Anda untuk mendapatkan rekomendasi kalori yang personal.")
            .setPositiveButton("Lengkapi Sekarang") { _, _ -> startActivity(Intent(this, EditProfileActivity::class.java)) }
            .setNegativeButton("Nanti", null).show()
    }

    private fun showGuestProfileSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_guest_profile_sheet, null)
        view.findViewById<MaterialButton>(R.id.btn_login_now).setOnClickListener {
            dialog.dismiss(); startActivity(Intent(this, Page4Activity::class.java))
        }
        dialog.setContentView(view); dialog.show()
    }

    private fun checkAndShowTutorial() {
        val user = auth.currentUser
        if (user != null && !user.isAnonymous) {
            val pref = getSharedPreferences("user_data", Context.MODE_PRIVATE)
            if (!pref.getBoolean("tutorial_shown_${user.uid}", false)) showTutorialBottomSheet()
        }
    }

    private fun showTutorialBottomSheet() {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.layout_tutorial_sheet, null)
        view.findViewById<MaterialButton>(R.id.btn_tutorial_next).setOnClickListener {
            getSharedPreferences("user_data", Context.MODE_PRIVATE).edit().putBoolean("tutorial_shown_${auth.currentUser?.uid}", true).apply()
            dialog.dismiss()
        }
        dialog.setContentView(view); dialog.show()
    }

    private fun handleAiIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("OPEN_CAMERA", false) == true) {
            handleAiClick(); intent.removeExtra("OPEN_CAMERA")
        }
    }

    private fun openCamera() = startActivity(Intent(this, AiCameraActivity::class.java))
}
