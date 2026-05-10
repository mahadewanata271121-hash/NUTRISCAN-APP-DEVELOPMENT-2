package com.example.nutriscan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnalysisResultActivity : AppCompatActivity() {

    private lateinit var ivCapturedImage: ImageView
    private lateinit var tvDetectedFoodName: TextView
    private lateinit var tvCalValue: TextView
    private lateinit var tvProteinValue: TextView
    private lateinit var tvCarbsValue: TextView
    private lateinit var tvFatValue: TextView
    private lateinit var tvAiAdvice: TextView
    private lateinit var pbProtein: ProgressBar
    private lateinit var pbCarbs: ProgressBar
    private lateinit var pbFat: ProgressBar
    private lateinit var rvScanHistory: RecyclerView
    private lateinit var btnSaveAnalysis: MaterialButton
    private lateinit var btnFavorite: ImageButton

    private val geminiService = GeminiNutriService()
    private var capturedBitmap: Bitmap? = null
    private var currentFoodName = ""
    private var currentCal = 0
    private var currentProt = 0
    private var currentCarbs = 0
    private var currentFat = 0
    private var isFavorite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis_result)

        initViews()
        loadLocalData()
        startSmartAnalysis()
        setupListeners()
    }

    private fun initViews() {
        ivCapturedImage = findViewById(R.id.ivCapturedImage)
        tvDetectedFoodName = findViewById(R.id.tvDetectedFoodName)
        tvCalValue = findViewById(R.id.tvCalValue)
        tvProteinValue = findViewById(R.id.tvProteinValue)
        tvCarbsValue = findViewById(R.id.tvCarbsValue)
        tvFatValue = findViewById(R.id.tvFatValue)
        tvAiAdvice = findViewById(R.id.tvAiAdvice)
        pbProtein = findViewById(R.id.pbProtein)
        pbCarbs = findViewById(R.id.pbCarbs)
        pbFat = findViewById(R.id.pbFat)
        rvScanHistory = findViewById(R.id.rvScanHistory)
        btnSaveAnalysis = findViewById(R.id.btnSaveAnalysis)
        btnFavorite = findViewById(R.id.btnFavorite)
        
        rvScanHistory.layoutManager = LinearLayoutManager(this)
        findViewById<TextView>(R.id.tvAnalysisTimestamp).text = 
            SimpleDateFormat("EEEE, d MMM yyyy • HH:mm", Locale("id", "ID")).format(Date())
    }

    private fun loadLocalData() {
        currentFoodName = intent.getStringExtra("DETECTED_FOOD") ?: "Makanan"
        tvDetectedFoodName.text = currentFoodName
        
        val path = intent.getStringExtra("IMAGE_PATH")
        if (!path.isNullOrEmpty()) {
            capturedBitmap = BitmapFactory.decodeFile(path)
            ivCapturedImage.setImageBitmap(capturedBitmap)
        }
    }

    private fun startSmartAnalysis() {
        tvAiAdvice.text = "Asisten AI sedang menganalisis porsi dan gizi secara visual..."

        val pref = getSharedPreferences("UserStats", Context.MODE_PRIVATE)
        val statsPref = getSharedPreferences("user_data", Context.MODE_PRIVATE)
        
        val consumedCal = pref.getFloat("consumed_calories", 0f).toInt()
        val targetCal = statsPref.getInt("daily_goal", 2000)
        val dailyContext = "User sudah mengonsumsi $consumedCal dari target $targetCal kkal hari ini."

        lifecycleScope.launch {
            val result = geminiService.getNutritionData(currentFoodName, capturedBitmap, dailyContext)
            
            if (result.errorMsg == null) {
                currentFoodName = result.specificName
                tvDetectedFoodName.text = "${result.specificName} (${result.portionWeight})"
                currentCal = result.calories
                currentProt = result.protein
                currentCarbs = result.carbs
                currentFat = result.fat
                updateUI(result.advice)
            } else {
                tvAiAdvice.text = "Gagal: ${result.errorMsg}"
            }
        }
    }

    private fun updateUI(advice: String) {
        val sharedPref = getSharedPreferences("user_data", MODE_PRIVATE)
        val targetCal = sharedPref.getInt("daily_goal", 2000)
        val targetCarbs = sharedPref.getInt("carbs_goal", (targetCal * 0.55 / 4).toInt())
        val targetProt = sharedPref.getInt("protein_goal", (targetCal * 0.20 / 4).toInt())
        val targetFat = sharedPref.getInt("fat_goal", (targetCal * 0.25 / 9).toInt())

        tvCalValue.text = "$currentCal kcal"
        tvAiAdvice.text = advice
        
        pbProtein.progress = if (targetProt > 0) (currentProt * 100 / targetProt).coerceAtMost(100) else 0
        tvProteinValue.text = "${currentProt}g / ${targetProt}g"
        
        pbCarbs.progress = if (targetCarbs > 0) (currentCarbs * 100 / targetCarbs).coerceAtMost(100) else 0
        tvCarbsValue.text = "${currentCarbs}g / ${targetCarbs}g"
        
        pbFat.progress = if (targetFat > 0) (currentFat * 100 / targetFat).coerceAtMost(100) else 0
        tvFatValue.text = "${currentFat}g / ${targetFat}g"
        
        updateRecentScans()
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        
        btnFavorite.setOnClickListener {
            isFavorite = !isFavorite
            btnFavorite.setImageResource(if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border)
            Toast.makeText(this, if (isFavorite) "Ditambahkan ke Favorit" else "Dihapus dari Favorit", Toast.LENGTH_SHORT).show()
        }

        btnSaveAnalysis.setOnClickListener {
            val pref = getSharedPreferences("UserStats", Context.MODE_PRIVATE)
            pref.edit().apply {
                putFloat("consumed_calories", pref.getFloat("consumed_calories", 0f) + currentCal)
                putFloat("consumed_protein", pref.getFloat("consumed_protein", 0f) + currentProt)
                putFloat("consumed_carbs", pref.getFloat("consumed_carbs", 0f) + currentCarbs)
                putFloat("consumed_fat", pref.getFloat("consumed_fat", 0f) + currentFat)
                apply()
            }
            Toast.makeText(this, "Data gizi disimpan!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun updateRecentScans() {
        val pref = getSharedPreferences("UserStats", Context.MODE_PRIVATE)
        val data = pref.getString("recent_scans_v2", "") ?: ""
        val items = data.split("#").filter { it.isNotEmpty() }.mapNotNull {
            val p = it.split("|")
            if (p.size >= 7) ScannedFood(p[0], p[1].toInt(), p[2].toInt(), p[3].toInt(), p[4].toInt(), p[5].toInt(), p[6]) else null
        }.toMutableList()

        val now = SimpleDateFormat("EEEE, d MMM yyyy • HH:mm", Locale("id", "ID")).format(Date())
        items.add(0, ScannedFood(currentFoodName, currentCal, currentProt, currentCarbs, currentFat, R.drawable.illustration22, now))
        
        val save = items.take(10).joinToString("#") { "${it.name}|${it.cal}|${it.prot}|${it.carbs}|${it.fat}|${it.imageRes}|${it.timestamp}" }
        pref.edit().putString("recent_scans_v2", save).apply()
        rvScanHistory.adapter = ScanHistoryAdapter(items.take(10))
    }

    data class ScannedFood(val name: String, val cal: Int, val prot: Int, val carbs: Int, val fat: Int, val imageRes: Int, val timestamp: String)

    class ScanHistoryAdapter(private val items: List<ScannedFood>) : RecyclerView.Adapter<ScanHistoryAdapter.ViewHolder>() {
        class ViewHolder(v: android.view.View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(R.id.tvFoodName)
            val tvDetails: TextView = v.findViewById(R.id.tvNutrientDetails)
            val tvDate: TextView = v.findViewById(R.id.tvDateTime)
            val ivFood: ImageView = v.findViewById(R.id.iv_food_icon)
        }
        override fun onCreateViewHolder(p: android.view.ViewGroup, t: Int) = ViewHolder(android.view.LayoutInflater.from(p.context).inflate(R.layout.item_scanned_food, p, false))
        override fun onBindViewHolder(h: ViewHolder, pos: Int) {
            val i = items[pos]
            h.tvName.text = i.name
            h.tvDetails.text = "${i.cal} kcal | P: ${i.prot}g | K: ${i.carbs}g"
            h.tvDate.text = i.timestamp
            h.ivFood.setImageResource(i.imageRes)
        }
        override fun getItemCount() = items.size
    }
}
