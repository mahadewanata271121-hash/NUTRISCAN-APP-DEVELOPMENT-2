package com.example.nutriscan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnalysisResultActivity : AppCompatActivity() {

    private var ivCapturedImage: ImageView? = null
    private var tvDetectedFoodName: TextView? = null
    private var tvCalValue: TextView? = null
    private var tvProteinValue: TextView? = null
    private var tvCarbsValue: TextView? = null
    private var tvFatValue: TextView? = null
    private var tvAiAdvice: TextView? = null
    private var pbProtein: ProgressBar? = null
    private var pbCarbs: ProgressBar? = null
    private var pbFat: ProgressBar? = null
    private var rvScanHistory: RecyclerView? = null
    private var btnSaveAnalysis: MaterialButton? = null
    private var btnFavorite: ImageButton? = null

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
        
        rvScanHistory?.layoutManager = LinearLayoutManager(this)
        findViewById<TextView>(R.id.tvAnalysisTimestamp)?.text = 
            SimpleDateFormat("EEEE, d MMM yyyy • HH:mm", Locale("id", "ID")).format(Date())
    }

    private fun loadLocalData() {
        currentFoodName = intent.getStringExtra("DETECTED_FOOD") ?: "Makanan"
        tvDetectedFoodName?.text = currentFoodName

        val path = intent.getStringExtra("IMAGE_PATH")
        if (!path.isNullOrEmpty()) {
            try {
                val file = File(path)
                if (!file.exists()) {
                    Log.e("AnalysisResult", "File gambar tidak ada di path: $path")
                    return
                }

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, options)

                // MEMPERBAIKI MISTERI CRASH: Menambahkan fungsi calculateInSampleSize yang hilang
                options.inSampleSize = calculateInSampleSize(options, 720, 720)
                options.inJustDecodeBounds = false

                val bitmap = BitmapFactory.decodeFile(path, options)
                if (bitmap != null) {
                    capturedBitmap = bitmap
                    ivCapturedImage?.setImageBitmap(capturedBitmap)
                }
            } catch (t: Throwable) {
                Log.e("AnalysisResult", "Gagal load gambar: ${t.message}")
            }
        }
    }

    // Fungsi pembantu untuk mencegah Out Of Memory (OOM) yang bikin force close
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun startSmartAnalysis() {
        tvAiAdvice?.text = "Asisten AI sedang menganalisis gizi..."

        val pref = getSharedPreferences("UserStats", Context.MODE_PRIVATE)
        val userPref = getSharedPreferences("user_data", Context.MODE_PRIVATE)

        val consumedCal = pref.getFloat("consumed_calories", 0f).toInt()
        val targetCal = userPref.getInt("daily_goal", 2000)
        val dailyContext = "User sudah mengonsumsi $consumedCal dari target $targetCal kkal hari ini."

        lifecycleScope.launch {
            try {
                val bitmapToAnalyze = capturedBitmap
                if (bitmapToAnalyze == null) {
                    tvAiAdvice?.text = "Gagal: Gambar tidak siap dianalisis."
                    return@launch
                }

                // Menjalankan API di background thread agar UI tidak beku
                val result = withContext(Dispatchers.IO) {
                    geminiService.getNutritionData(currentFoodName, bitmapToAnalyze, dailyContext)
                }

                // Memastikan Activity masih aktif sebelum update UI
                if (!isFinishing && !isDestroyed) {
                    if (result.errorMsg == null) {
                        currentFoodName = result.specificName
                        currentCal = result.calories
                        currentProt = result.protein
                        currentCarbs = result.carbs
                        currentFat = result.fat

                        tvDetectedFoodName?.text = "${result.specificName} (${result.portionWeight})"
                        updateUI(result.advice)
                    } else {
                        tvAiAdvice?.text = result.errorMsg
                    }
                }
            } catch (t: Throwable) {
                Log.e("AnalysisResult", "Fatal Analysis Error", t)
                if (!isFinishing) {
                    tvAiAdvice?.text = "Gagal memproses: ${t.localizedMessage}"
                }
            }
        }
    }

    private fun updateUI(advice: String) {
        val userPref = getSharedPreferences("user_data", MODE_PRIVATE)
        val targetCal = userPref.getInt("daily_goal", 2000)
        
        val tCarbs = userPref.getInt("carbs_goal", (targetCal * 0.50 / 4).toInt())
        val tProt = userPref.getInt("protein_goal", (targetCal * 0.20 / 4).toInt())
        val tFat = userPref.getInt("fat_goal", (targetCal * 0.30 / 9).toInt())

        tvCalValue?.text = "$currentCal kcal"
        tvAiAdvice?.text = advice
        
        pbProtein?.progress = if (tProt > 0) (currentProt * 100 / tProt).coerceAtMost(100) else 0
        tvProteinValue?.text = "${currentProt}g / ${tProt}g"
        
        pbCarbs?.progress = if (tCarbs > 0) (currentCarbs * 100 / tCarbs).coerceAtMost(100) else 0
        tvCarbsValue?.text = "${currentCarbs}g / ${tCarbs}g"
        
        pbFat?.progress = if (tFat > 0) (currentFat * 100 / tFat).coerceAtMost(100) else 0
        tvFatValue?.text = "${currentFat}g / ${tFat}g"
        
        updateRecentScans()
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener { finish() }
        btnFavorite?.setOnClickListener { toggleFavorite() }
        btnSaveAnalysis?.setOnClickListener { saveNutrientsToTotal() }
    }

    private fun toggleFavorite() {
        isFavorite = !isFavorite
        btnFavorite?.setImageResource(if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border)
        
        val pref = getSharedPreferences("UserStats", Context.MODE_PRIVATE)
        val favoritesData = pref.getString("favorite_foods", "") ?: ""
        val items = favoritesData.split("#").filter { it.isNotEmpty() }.toMutableList()
        
        val safeName = currentFoodName.replace("|", "-").replace("#", " ")
        val currentItemStr = "$safeName|$currentCal|$currentProt|$currentCarbs|$currentFat|${R.drawable.illustration22}"

        if (isFavorite) {
            if (!items.contains(currentItemStr)) items.add(0, currentItemStr)
            Toast.makeText(this, "Ditambahkan ke Favorit", Toast.LENGTH_SHORT).show()
        } else {
            items.remove(currentItemStr)
            Toast.makeText(this, "Dihapus dari Favorit", Toast.LENGTH_SHORT).show()
        }
        pref.edit().putString("favorite_foods", items.joinToString("#")).apply()
    }

    private fun saveNutrientsToTotal() {
        val pref = getSharedPreferences("UserStats", Context.MODE_PRIVATE)
        pref.edit().apply {
            putFloat("consumed_calories", pref.getFloat("consumed_calories", 0f) + currentCal)
            putFloat("consumed_protein", pref.getFloat("consumed_protein", 0f) + currentProt)
            putFloat("consumed_carbs", pref.getFloat("consumed_carbs", 0f) + currentCarbs)
            putFloat("consumed_fat", pref.getFloat("consumed_fat", 0f) + currentFat)
            apply()
        }
        Toast.makeText(this, "Berhasil simpan ke catatan harian!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun updateRecentScans() {
        val pref = getSharedPreferences("UserStats", Context.MODE_PRIVATE)
        val data = pref.getString("recent_scans_v2", "") ?: ""
        val items = data.split("#").filter { it.isNotEmpty() }.mapNotNull {
            val p = it.split("|")
            if (p.size >= 7) {
                ScannedFood(p[0], p[1].toIntOrNull() ?: 0, p[2].toIntOrNull() ?: 0, p[3].toIntOrNull() ?: 0, p[4].toIntOrNull() ?: 0, p[5].toIntOrNull() ?: 0, p[6])
            } else null
        }.toMutableList()

        val now = SimpleDateFormat("EEEE, d MMM yyyy • HH:mm", Locale("id", "ID")).format(Date())
        val safeName = currentFoodName.replace("|", "-").replace("#", " ")
        
        if (items.isEmpty() || items[0].name != safeName) {
            items.add(0, ScannedFood(safeName, currentCal, currentProt, currentCarbs, currentFat, R.drawable.illustration22, now))
        }
        
        val save = items.take(15).joinToString("#") { "${it.name}|${it.cal}|${it.prot}|${it.carbs}|${it.fat}|${it.imageRes}|${it.timestamp}" }
        pref.edit().putString("recent_scans_v2", save).apply()
        
        rvScanHistory?.adapter = ScanHistoryAdapter(items.take(10))
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
            h.tvDetails.text = "${i.cal} kcal | P: ${i.prot}g | K: ${i.carbs}g | L: ${i.fat}g"
            h.tvDate.text = i.timestamp
            h.ivFood.setImageResource(i.imageRes)
        }
        override fun getItemCount() = items.size
    }
}
