package com.example.nutriscan

import android.animation.ValueAnimator
import android.content.Context
import android.widget.ProgressBar
import android.view.View
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
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

        val notificationIcon = findViewById<ImageView>(R.id.notification_icon)
        notificationIcon.setOnClickListener {
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
        }

        auth = FirebaseAuth.getInstance()
        greetingText = findViewById(R.id.greeting_text)

        val userAvatar = findViewById<ImageView>(R.id.user_avatar)
        userAvatar.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser == null || currentUser.isAnonymous) {
                showGuestProfileSheet()
            } else {
                val intent = Intent(this, EditProfileActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            }
        }

        setupGreeting()
        setupDateRecyclerView()
        
        checkDataCompleteness()
        val sharedPref = getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val initialNudgeShown = sharedPref.getBoolean("initial_nudge_shown", false)
        if (!isPhysicalDataComplete && !initialNudgeShown && auth.currentUser != null && !auth.currentUser!!.isAnonymous) {
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
        val currentUser = auth.currentUser
        val sharedPref = getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "Dewa")
        greetingText.text = if (currentUser == null || currentUser.isAnonymous) "Selamat Pagi Tamu!" else "Selamat Pagi $name!"
    }

    private fun setupDateRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.date_recycler_view) ?: return
        val tvMonthYear = findViewById<TextView>(R.id.tv_month_year)
        
        val today = Calendar.getInstance()
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        tvMonthYear?.text = monthYearFormat.format(today.time)

        val dates = mutableListOf<DateItem>()
        val tempCal = today.clone() as Calendar
        
        // Logika untuk memulai minggu dari hari Senin
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
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)
                val currentTotalMinutes = (hour * 60) + minute
                if (currentTotalMinutes == 0 && progressBar.progress > 1400) {
                    animateProgressReset(progressBar)
                } else {
                    progressBar.progress = currentTotalMinutes
                }
                handler.postDelayed(this, 60000)
            }
        }
        handler.post(dayProgressRunnable)
    }

    private fun animateProgressReset(progressBar: ProgressBar) {
        val animator = ValueAnimator.ofInt(progressBar.progress, 0)
        animator.duration = 1000
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { progressBar.progress = it.animatedValue as Int }
        animator.start()
    }

    private fun checkAndShowTutorial() {
        val currentUser = auth.currentUser
        if (currentUser != null && !currentUser.isAnonymous) {
            val sharedPref = getSharedPreferences("user_data", Context.MODE_PRIVATE)
            val tutorialShown = sharedPref.getBoolean("tutorial_shown_${currentUser.uid}", false)
            if (!tutorialShown) {
                showTutorialBottomSheet()
            }
        }
    }

    private fun showTutorialBottomSheet() {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.layout_tutorial_sheet, null)
        val title = view.findViewById<TextView>(R.id.tv_tutorial_title)
        val desc = view.findViewById<TextView>(R.id.tv_tutorial_desc)
        val btnNext = view.findViewById<MaterialButton>(R.id.btn_tutorial_next)
        var step = 1
        
        btnNext.setOnClickListener {
            step++
            when (step) {
                2 -> { title.text = "AI Camera Nutriscan"; desc.text = "Gunakan kamera pintar kami untuk mengenali makanan Anda secara instan." }
                3 -> { title.text = "Pantau Progres Anda"; desc.text = "Lihat perkembangan nutrisi Anda melalui grafik harian." }
                4 -> { title.text = "Edukasi Gizi"; desc.text = "Dapatkan tips kesehatan harian."; btnNext.text = "Mulai Sekarang" }
                else -> { markTutorialAsShown(); dialog.dismiss() }
            }
        }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun markTutorialAsShown() {
        val currentUser = auth.currentUser ?: return
        getSharedPreferences("user_data", Context.MODE_PRIVATE).edit().putBoolean("tutorial_shown_${currentUser.uid}", true).apply()
    }

    private fun handleAiIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("OPEN_CAMERA", false) == true) {
            handleAiClick()
            intent.removeExtra("OPEN_CAMERA")
        }
    }

    fun handleAiClick() {
        val currentUser = auth.currentUser
        if (currentUser == null || currentUser.isAnonymous) {
            showGuestRestrictedDialog()
        } else if (!isPhysicalDataComplete) {
            showIncompleteDataDialog()
        } else {
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun showGuestRestrictedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Akses Terbatas")
            .setMessage("Silakan buat akun terlebih dahulu untuk menikmati akses penuh.")
            .setPositiveButton("Daftar Sekarang") { _, _ -> startActivity(Intent(this, RegisterActivity::class.java)) }
            .setNegativeButton("Nanti Saja", null)
            .show()
    }

    private fun checkDataCompleteness() {
        val sharedPref = getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val h = sharedPref.getString("height", "")
        val w = sharedPref.getString("weight", "")
        val b = sharedPref.getString("birthdate", "")
        val g = sharedPref.getString("gender", "")
        isPhysicalDataComplete = !h.isNullOrEmpty() && !w.isNullOrEmpty() && !b.isNullOrEmpty() && !g.isNullOrEmpty()
    }

    private fun updateButtonsVisualState() {
        val currentUser = auth.currentUser
        val isGuest = currentUser == null || currentUser.isAnonymous
        val guestAlpha = if (isGuest) 0.4f else 1.0f
        findViewById<View>(R.id.nav_ai_icon)?.alpha = if (isPhysicalDataComplete) 1.0f else 0.4f
        findViewById<View>(R.id.calories_card)?.alpha = guestAlpha
        findViewById<View>(R.id.nutrients_grid)?.alpha = guestAlpha
    }

    private fun showInitialNudgeDialog() {
        AlertDialog.Builder(this)
            .setTitle("Halo!")
            .setMessage("Lengkapi profil agar hasil lebih akurat.")
            .setPositiveButton("Lengkapi Profil") { _, _ -> startActivity(Intent(this, EditProfileActivity::class.java)) }
            .setNegativeButton("Nanti Saja", null)
            .show()
    }

    private fun showIncompleteDataDialog() {
        AlertDialog.Builder(this)
            .setTitle("Data Belum Lengkap!")
            .setMessage("Lengkapi data fisik di Edit Profil.")
            .setPositiveButton("Lengkapi Sekarang") { _, _ -> startActivity(Intent(this, EditProfileActivity::class.java)) }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun showGuestProfileSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_guest_profile_sheet, null)
        view.findViewById<MaterialButton>(R.id.btn_login_now).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, Page4Activity::class.java))
        }
        dialog.setContentView(view)
        dialog.show()
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
        val caloriesCard = findViewById<MaterialCardView>(R.id.calories_card)
        val progressBar = findViewById<CircularProgressBar>(R.id.calories_progress_bar)
        val tvValue = findViewById<TextView>(R.id.calories_text)
        val tvLabel = findViewById<TextView>(R.id.total_kalori_label)
        val tvUnit = findViewById<TextView>(R.id.tv_calories_unit)
        val maxLabel = findViewById<TextView>(R.id.max_calories_label)

        val sharedPref = getSharedPreferences("user_data", MODE_PRIVATE)
        val target = if (isPhysicalDataComplete) sharedPref.getInt("daily_goal", 2000).toFloat() else 2000f
        val consumed = getSharedPreferences("UserStats", MODE_PRIVATE).getFloat("consumed_calories", 0f)
        
        progressBar.progressMax = target / 0.76f
        if (animate) progressBar.setProgressWithAnimation(consumed.coerceAtMost(target), 1500)
        else progressBar.progress = consumed.coerceAtMost(target)

        val dangerColor = Color.parseColor("#D32F2F")
        if (consumed > target) {
            caloriesCard.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
            progressBar.progressBarColor = dangerColor
            tvValue.setTextColor(dangerColor)
            tvLabel?.setTextColor(dangerColor)
            tvUnit?.setTextColor(dangerColor)
        } else {
            val normal = ContextCompat.getColor(this, R.color.card_calories_text)
            caloriesCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_calories_bg))
            progressBar.progressBarColor = normal
            tvValue.setTextColor(normal)
            tvLabel?.setTextColor(normal)
            tvUnit?.setTextColor(normal)
        }
        tvValue.text = consumed.toInt().toString()
        maxLabel.text = target.toInt().toString()
    }

    private fun setupNutrientsProgressBar(animate: Boolean) {
        val sharedPref = getSharedPreferences("user_data", MODE_PRIVATE)
        val targetCal = if (isPhysicalDataComplete) sharedPref.getInt("daily_goal", 2000).toFloat() else 2000f
        val targetCarbs = sharedPref.getInt("carbs_goal", (targetCal * 0.55 / 4).toInt())
        val targetProt = sharedPref.getInt("protein_goal", (targetCal * 0.20 / 4).toInt())

        val stats = getSharedPreferences("UserStats", MODE_PRIVATE)
        updateNutrientCard(R.id.carbs_card, R.id.carbs_progress, R.id.carbs_value, R.id.carbs_label, 
            stats.getFloat("consumed_carbs", 0f), targetCarbs, animate, "#157BBF")
        updateNutrientCard(R.id.protein_card, R.id.protein_progress, R.id.protein_value, R.id.protein_label, 
            stats.getFloat("consumed_protein", 0f), targetProt, animate, "#3D9471")
    }

    private fun updateNutrientCard(cardId: Int, barId: Int, valId: Int, labelId: Int, consumed: Float, target: Int, animate: Boolean, normalBarColor: String) {
        val card = findViewById<MaterialCardView>(cardId)
        val bar = findViewById<ProgressBar>(barId)
        val tvVal = findViewById<TextView>(valId)
        val tvLabel = findViewById<TextView>(labelId)
        
        bar.max = target * 100
        val displayConsumed = consumed.toInt()
        tvVal.text = "${displayConsumed}g / ${target}g"
        
        if (animate) {
            val animator = ValueAnimator.ofInt(target * 100, displayConsumed.coerceAtMost(target) * 100)
            animator.duration = 1500
            animator.addUpdateListener { bar.progress = it.animatedValue as Int }
            animator.start()
        } else {
            bar.progress = displayConsumed.coerceAtMost(target) * 100
        }

        val dangerColor = Color.parseColor("#D32F2F")
        if (consumed > target) {
            card.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
            setProgressBarColors(bar, dangerColor, Color.parseColor("#FFCDD2"))
            tvVal.setTextColor(dangerColor)
            tvLabel?.setTextColor(dangerColor)
        } else {
            card.setCardBackgroundColor(ContextCompat.getColor(this, if (cardId == R.id.carbs_card) R.color.card_carbs_bg else R.color.card_protein_bg))
            setProgressBarColors(bar, Color.BLACK, Color.parseColor(normalBarColor))
            val normalText = ContextCompat.getColor(this, if (cardId == R.id.carbs_card) R.color.card_carbs_text else R.color.card_protein_text)
            tvVal.setTextColor(normalText)
            tvLabel?.setTextColor(normalText)
        }
    }

    private fun setProgressBarColors(bar: ProgressBar, progressColor: Int, backgroundColor: Int) {
        val drawable = bar.progressDrawable?.mutate() as? LayerDrawable ?: return
        drawable.findDrawableByLayerId(android.R.id.background)?.setTint(backgroundColor)
        drawable.findDrawableByLayerId(android.R.id.progress)?.setTint(progressColor)
    }

    private fun setupResetButtons() {
        val resetAction = {
            if (isPhysicalDataComplete) {
                getSharedPreferences("UserStats", MODE_PRIVATE).edit().clear().apply()
                setupCaloriesProgressBar(true); setupNutrientsProgressBar(true)
            }
        }
        findViewById<View>(R.id.btn_reset_calories).setOnClickListener { resetAction() }
        findViewById<View>(R.id.btn_reset_experiment).setOnClickListener { resetAction() }
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
        val sharedPref = getSharedPreferences("UserStats", Context.MODE_PRIVATE)
        val lastDateStr = sharedPref.getString("last_opened_date", "")
        val currentDateStr = Calendar.getInstance().let { "${it.get(Calendar.YEAR)}-${it.get(Calendar.MONTH)}-${it.get(Calendar.DAY_OF_MONTH)}" }
        if (lastDateStr != "" && lastDateStr != currentDateStr) {
            sharedPref.edit().clear().putString("last_opened_date", currentDateStr).apply()
            setupCaloriesProgressBar(true); setupNutrientsProgressBar(true)
        } else if (lastDateStr == "") sharedPref.edit().putString("last_opened_date", currentDateStr).apply()
    }

    private fun openCamera() = startActivity(Intent(this, AiCameraActivity::class.java))
}
