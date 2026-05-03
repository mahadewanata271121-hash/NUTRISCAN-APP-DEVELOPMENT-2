package com.example.nutriscan

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButtonToggleGroup
import java.util.Calendar
import java.util.Locale

class AnalysisHistoryActivity : AppCompatActivity() {

    private lateinit var caloriesChart: MountainChartView
    private lateinit var carbsChart: MountainChartView
    private lateinit var proteinChart: MountainChartView
    private lateinit var fatChart: MountainChartView
    private lateinit var toggleGroup: MaterialButtonToggleGroup

    private var userBmr: Float = 2000f
    private var carbsTarget: Float = 250f
    private var proteinTarget: Float = 100f
    private var fatTarget: Float = 65f
    private var isVisitor: Boolean = false

    // 0 = Jan-Jun, 1 = Jul-Dec
    private var currentMonthOffset = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis_history)

        setupMountainCharts()
        setupIntervalToggle()
        setupPaginationButtons()

        findViewById<ImageButton>(R.id.btn_back_history)?.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        findViewById<ImageButton>(R.id.btn_view_favorites)?.setOnClickListener {
            val intent = Intent(this, FavoritesActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshData()
        NavigationHelper.setupBottomNavigation(this, "Scan")
    }

    private fun refreshData() {
        val sharedData = getSharedPreferences("user_data", Context.MODE_PRIVATE)
        isVisitor = sharedData.getBoolean("is_visitor", false)

        userBmr = sharedData.getInt("daily_goal", 2000).toFloat()
        carbsTarget = sharedData.getInt("carbs_goal", 250).toFloat()
        proteinTarget = sharedData.getInt("protein_goal", 100).toFloat()
        fatTarget = sharedData.getInt("fat_goal", 65).toFloat()

        val sharedStats = getSharedPreferences("UserStats", MODE_PRIVATE)
        val consumedCal = sharedStats.getFloat("consumed_calories", 0f)

        setupBarChart(sharedStats, consumedCal, userBmr)

        val tvFoodList = findViewById<TextView>(R.id.tv_food_list)
        val historyData = sharedStats.getString("daily_food_history", "")
        if (!historyData.isNullOrEmpty()) {
            val buildTeks = StringBuilder()
            historyData.split("#").forEach { item ->
                val detail = item.split("|")
                if (detail.size == 3) {
                    buildTeks.append("${detail[1]}      ${detail[0]}      ${detail[2]}\n\n")
                }
            }
            tvFoodList?.text = buildTeks.toString()
        } else {
            tvFoodList?.text = getString(R.string.empty_daily_notes)
        }

        updateChartsBySelectedToggle()
    }

    private fun updateChartsBySelectedToggle() {
        val interval = when (toggleGroup.checkedButtonId) {
            R.id.btn_1m -> "1B"
            R.id.btn_6m -> "6B"
            R.id.btn_1y -> "1T"
            else -> "1M"
        }
        updateCharts(interval)
        updatePaginationVisibility(interval)
    }

    private fun setupBarChart(sharedStats: android.content.SharedPreferences, consumedCal: Float, dailyGoal: Float) {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dayMapping = mapOf(
            Calendar.MONDAY to "Mon",
            Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu",
            Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat",
            Calendar.SUNDAY to "Sun"
        )
        val graphDays = listOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
            Calendar.SUNDAY
        )
        val barIds = listOf<Int>(
            R.id.bar_senin,
            R.id.bar_selasa,
            R.id.bar_rabu,
            R.id.bar_kamis,
            R.id.bar_jumat,
            R.id.bar_sabtu,
            R.id.bar_minggu
        )
        val textIds = principle_text_ids
        val limitIds = principle_limit_ids

        var totalCaloriesForAvg = 0f
        var daysWithData = 0
        for (i in graphDays.indices) {
            val currentGraphDay = graphDays[i]
            val dayKey = dayMapping[currentGraphDay] ?: ""
            val value = when {
                currentGraphDay == dayOfWeek -> consumedCal
                isPastDay(currentGraphDay, dayOfWeek) -> sharedStats.getFloat("history_cal_$dayKey", 0f)
                else -> -1f
            }
            if (value >= 0) {
                setupBar(barIds[i], textIds[i], limitIds[i], value, dailyGoal)
                if (value > 0) {
                    totalCaloriesForAvg += value
                    daysWithData++
                }
            } else {
                hideBar(barIds[i], textIds[i], limitIds[i], dailyGoal)
            }
        }
        val avgCal = if (daysWithData > 0) totalCaloriesForAvg / daysWithData else 0f
        findViewById<TextView>(R.id.tv_average_total)?.text = if (isVisitor) {
            "Invalid"
        } else {
            String.format(Locale.US, "%,.0f kkal", avgCal)
        }
    }

    private val principle_text_ids = listOf<Int>(
        R.id.tv_val_senin, R.id.tv_val_selasa, R.id.tv_val_rabu, R.id.tv_val_kamis,
        R.id.tv_val_jumat, R.id.tv_val_sabtu, R.id.tv_val_minggu
    )

    private val principle_limit_ids = listOf<Int>(
        R.id.tv_limit_senin, R.id.tv_limit_selasa, R.id.tv_limit_rabu, R.id.tv_limit_kamis,
        R.id.tv_limit_jumat, R.id.tv_limit_sabtu, R.id.tv_limit_minggu
    )

    private fun setupMountainCharts() {
        caloriesChart = findViewById(R.id.chart_calories)
        carbsChart = findViewById(R.id.chart_carbs)
        proteinChart = findViewById(R.id.chart_protein)
        fatChart = findViewById(R.id.chart_fat)
    }

    private fun setupIntervalToggle() {
        toggleGroup = findViewById(R.id.interval_toggle_group)
        toggleGroup.addOnButtonCheckedListener { _, _, isChecked ->
            if (isChecked) {
                // Selalu mulai dari Jan-Jun saat ganti interval
                currentMonthOffset = 0
                updateChartsBySelectedToggle()
            }
        }
    }

    private fun setupPaginationButtons() {
        val nextBtns = listOf<Int>(R.id.btn_next_cal, R.id.btn_next_carbs, R.id.btn_next_prot, R.id.btn_next_fat)

        nextBtns.forEach { id ->
            findViewById<ImageButton>(id)?.setOnClickListener {
                // Toggle antara 0 (Jan-Jun) dan 1 (Jul-Des)
                currentMonthOffset = if (currentMonthOffset == 0) 1 else 0
                updateChartsBySelectedToggle()
            }
        }
    }

    private fun updatePaginationVisibility(interval: String) {
        val is6B = interval == "6B"
        val visibility = if (is6B) View.VISIBLE else View.GONE
        
        // Jan-Jun (offset 0) -> Panah Kanan (rotation 180)
        // Jul-Des (offset 1) -> Panah Kiri (rotation 0)
        val rotation = if (currentMonthOffset == 0) 180f else 0f

        listOf<Int>(
            R.id.btn_next_cal, R.id.btn_next_carbs, R.id.btn_next_prot, R.id.btn_next_fat
        ).forEach { id ->
            findViewById<ImageButton>(id)?.apply {
                this.visibility = visibility
                this.rotation = rotation
            }
        }
    }

    private fun getWeeklyAverage(): Quadruple<Float, Float, Float, Float> {
        val sharedStats = getSharedPreferences("UserStats", MODE_PRIVATE)
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dayMapping = mapOf(
            Calendar.MONDAY to "Mon",
            Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu",
            Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat",
            Calendar.SUNDAY to "Sun"
        )
        val graphDays = listOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
            Calendar.SUNDAY
        )

        var totalCal = 0f
        var totalCarbs = 0f
        var totalProt = 0f
        var totalFat = 0f
        var daysWithData = 0

        for (day in graphDays) {
            val dayKey = dayMapping[day] ?: ""
            val cal = if (day == dayOfWeek) sharedStats.getFloat("consumed_calories", 0f)
            else sharedStats.getFloat("history_cal_$dayKey", 0f)
            val carbs = if (day == dayOfWeek) sharedStats.getFloat("consumed_carbs", 0f)
            else sharedStats.getFloat("history_carbs_$dayKey", 0f)
            val prot = if (day == dayOfWeek) sharedStats.getFloat("consumed_protein", 0f)
            else sharedStats.getFloat("history_protein_$dayKey", 0f)
            val fat = if (day == dayOfWeek) sharedStats.getFloat("consumed_fat", 0f)
            else sharedStats.getFloat("history_fat_$dayKey", 0f)

            if (cal > 0 || carbs > 0 || prot > 0 || fat > 0) {
                totalCal += cal
                totalCarbs += carbs
                totalProt += prot
                totalFat += fat
                daysWithData++
            }
        }

        return if (daysWithData > 0) {
            Quadruple(totalCal / daysWithData, totalCarbs / daysWithData, totalProt / daysWithData, totalFat / daysWithData)
        } else {
            Quadruple(0f, 0f, 0f, 0f)
        }
    }

    private fun updateCharts(interval: String) {
        if (isVisitor) {
            caloriesChart.setData(emptyList(), emptyList(), Color.GRAY)
            carbsChart.setData(emptyList(), emptyList(), Color.GRAY)
            proteinChart.setData(emptyList(), emptyList(), Color.GRAY)
            fatChart.setData(emptyList(), emptyList(), Color.GRAY)
            return
        }

        val labels: List<String>
        val calPoints = mutableListOf<Float>()
        val carbsPoints = mutableListOf<Float>()
        val proteinPoints = mutableListOf<Float>()
        val fatPoints = mutableListOf<Float>()

        val calendar = Calendar.getInstance()

        when (interval) {
            "1M" -> { // Weekly Interval (Current Week)
                labels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
                val sharedStats = getSharedPreferences("UserStats", MODE_PRIVATE)
                val todayIdxInWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> 1
                    Calendar.WEDNESDAY -> 2
                    Calendar.THURSDAY -> 3
                    Calendar.FRIDAY -> 4
                    Calendar.SATURDAY -> 5
                    Calendar.SUNDAY -> 6
                    else -> 0
                }
                val dayMapping = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                for (i in 0 until 7) {
                    if (i < todayIdxInWeek) {
                        val k = dayMapping[i]
                        calPoints.add(sharedStats.getFloat("history_cal_$k", 0f))
                        carbsPoints.add(sharedStats.getFloat("history_carbs_$k", 0f))
                        proteinPoints.add(sharedStats.getFloat("history_protein_$k", 0f))
                        fatPoints.add(sharedStats.getFloat("history_fat_$k", 0f))
                    } else if (i == todayIdxInWeek) {
                        calPoints.add(sharedStats.getFloat("consumed_calories", 0f))
                        carbsPoints.add(sharedStats.getFloat("consumed_carbs", 0f))
                        proteinPoints.add(sharedStats.getFloat("consumed_protein", 0f))
                        fatPoints.add(sharedStats.getFloat("consumed_fat", 0f))
                    } else {
                        calPoints.add(-1f)
                        carbsPoints.add(-1f)
                        proteinPoints.add(-1f)
                        fatPoints.add(-1f)
                    }
                }
            }
            "1B" -> { // Monthly Interval (4 Weeks)
                labels = listOf("Minggu 1", "Minggu 2", "Minggu 3", "Minggu 4")
                val currentWeekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH).coerceIn(1, 4)
                val (avgCal, avgCarbs, avgProt, avgFat) = getWeeklyAverage()

                for (i in 1..4) {
                    if (i == currentWeekOfMonth) {
                        calPoints.add(avgCal)
                        carbsPoints.add(avgCarbs)
                        proteinPoints.add(avgProt)
                        fatPoints.add(avgFat)
                    } else if (i < currentWeekOfMonth) {
                        calPoints.add(0f)
                        carbsPoints.add(0f)
                        proteinPoints.add(0f)
                        fatPoints.add(0f)
                    } else {
                        calPoints.add(-1f)
                        carbsPoints.add(-1f)
                        proteinPoints.add(-1f)
                        fatPoints.add(-1f)
                    }
                }
            }
            "6B" -> { // 6 Months Interval (Jan-Jun or Jul-Dec)
                val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
                
                // if offset 0 -> Jan-Jun, if offset 1 -> Jul-Dec
                val startMonth = if (currentMonthOffset == 0) 0 else 6
                labels = monthNames.subList(startMonth, startMonth + 6)
                
                val realTodayMonth = calendar.get(Calendar.MONTH)
                val (avgCal, avgCarbs, avgProt, avgFat) = getWeeklyAverage()

                for (i in 0 until 6) {
                    val targetMonth = startMonth + i
                    if (targetMonth == realTodayMonth) {
                        calPoints.add(avgCal)
                        carbsPoints.add(avgCarbs)
                        proteinPoints.add(avgProt)
                        fatPoints.add(avgFat)
                    } else if (targetMonth < realTodayMonth) {
                        calPoints.add(0f)
                        carbsPoints.add(0f)
                        proteinPoints.add(0f)
                        fatPoints.add(0f)
                    } else {
                        calPoints.add(-1f)
                        carbsPoints.add(-1f)
                        proteinPoints.add(-1f)
                        fatPoints.add(-1f)
                    }
                }
            }
            "1T" -> { // 1 Year Interval (12 Months)
                labels = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
                val realTodayMonth = calendar.get(Calendar.MONTH)
                val (avgCal, avgCarbs, avgProt, avgFat) = getWeeklyAverage()

                for (i in 0 until 12) {
                    if (i == realTodayMonth) {
                        calPoints.add(avgCal)
                        carbsPoints.add(avgCarbs)
                        proteinPoints.add(avgProt)
                        fatPoints.add(avgFat)
                    } else if (i < realTodayMonth) {
                        calPoints.add(0f)
                        carbsPoints.add(0f)
                        proteinPoints.add(0f)
                        fatPoints.add(0f)
                    } else {
                        calPoints.add(-1f)
                        carbsPoints.add(-1f)
                        proteinPoints.add(-1f)
                        fatPoints.add(-1f)
                    }
                }
            }
            else -> labels = emptyList()
        }

        caloriesChart.setData(calPoints, labels, ContextCompat.getColor(this, R.color.orange_primary), userBmr * 0.8f, userBmr, userBmr * 1.2f)
        carbsChart.setData(carbsPoints, labels, ContextCompat.getColor(this, R.color.illu_blue), carbsTarget * 0.8f, carbsTarget, carbsTarget * 1.2f)
        proteinChart.setData(proteinPoints, labels, ContextCompat.getColor(this, R.color.illu_green_healthy), proteinTarget * 0.8f, proteinTarget, proteinTarget * 1.2f)
        fatChart.setData(fatPoints, labels, ContextCompat.getColor(this, R.color.illu_purple), fatTarget * 0.8f, fatTarget, fatTarget * 1.2f)
    }

    private fun isPastDay(graphDay: Int, today: Int): Boolean {
        val order = mapOf(
            Calendar.MONDAY to 0,
            Calendar.TUESDAY to 1,
            Calendar.WEDNESDAY to 2,
            Calendar.THURSDAY to 3,
            Calendar.FRIDAY to 4,
            Calendar.SATURDAY to 5,
            Calendar.SUNDAY to 6
        )
        return (order[graphDay] ?: 0) < (order[today] ?: 0)
    }

    private fun setupBar(barId: Int, textId: Int, limitId: Int, value: Float, dailyGoal: Float) {
        val bar = findViewById<ProgressBar>(barId)
        val text = findViewById<TextView>(textId)
        val limitText = findViewById<TextView>(limitId)
        bar?.visibility = View.VISIBLE
        limitText?.visibility = View.VISIBLE
        val maxVal = if (dailyGoal <= 0) 2000f else dailyGoal
        limitText?.text = formatLimit(maxVal)
        bar?.max = (maxVal * 100).toInt()
        ObjectAnimator.ofInt(bar, "progress", 0, (value * 100).toInt()).apply {
            duration = 1500
            interpolator = DecelerateInterpolator()
            start()
        }
        text?.text = if (value >= 10000) String.format(Locale.US, "%.1fk", value / 1000) else value.toInt().toString()
        text?.visibility = View.VISIBLE
    }

    private fun hideBar(barId: Int, textId: Int, limitId: Int, dailyGoal: Float) {
        val bar = findViewById<ProgressBar>(barId)
        val limitText = findViewById<TextView>(limitId)
        limitText?.text = formatLimit(dailyGoal)
        bar?.max = (dailyGoal * 100).toInt()
        bar?.progress = 0
        bar?.alpha = 0.2f
        findViewById<TextView>(textId)?.visibility = View.INVISIBLE
    }

    private fun formatLimit(value: Float): String =
        if (value >= 10000) String.format(Locale.US, "%.1fk", value / 1000) else value.toInt().toString()

    data class Quadruple<out A, out B, out C, out D>(val first: A, val second: B, val third: C, val fourth: D)
}
