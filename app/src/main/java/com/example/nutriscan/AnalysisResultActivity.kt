package com.example.nutriscan

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
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
    private lateinit var tvAnalysisTimestamp: TextView
    private lateinit var pbProtein: ProgressBar
    private lateinit var pbCarbs: ProgressBar
    private lateinit var pbFat: ProgressBar
    private lateinit var rvScanHistory: RecyclerView
    private lateinit var btnSaveAnalysis: MaterialButton
    private lateinit var btnBack: ImageButton
    private lateinit var btnFavorite: ImageButton

    private var isFavorite = false
    private var currentFoodName = "Tidak Terdeteksi"
    private var currentCal = 0
    private var currentProt = 0
    private var currentCarbs = 0
    private var currentFat = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis_result)

        initViews()
        setupAnalysisData()
        setupListeners()
        updateRecentScans()
    }

    private fun initViews() {
        ivCapturedImage = findViewById(R.id.ivCapturedImage)
        tvDetectedFoodName = findViewById(R.id.tvDetectedFoodName)
        tvCalValue = findViewById(R.id.tvCalValue)
        tvProteinValue = findViewById(R.id.tvProteinValue)
        tvCarbsValue = findViewById(R.id.tvCarbsValue)
        tvFatValue = findViewById(R.id.tvFatValue)
        tvAnalysisTimestamp = findViewById(R.id.tvAnalysisTimestamp)
        pbProtein = findViewById(R.id.pbProtein)
        pbCarbs = findViewById(R.id.pbCarbs)
        pbFat = findViewById(R.id.pbFat)
        rvScanHistory = findViewById(R.id.rvScanHistory)
        btnSaveAnalysis = findViewById(R.id.btnSaveAnalysis)
        btnBack = findViewById(R.id.btnBack)
        btnFavorite = findViewById(R.id.btnFavorite)

        rvScanHistory.layoutManager = LinearLayoutManager(this)
        tvAnalysisTimestamp.text = SimpleDateFormat("EEEE, d MMM yyyy • HH:mm", Locale("id", "ID")).format(Date())
    }

    private fun setupAnalysisData() {
        try {
            // 1. Ambil Nama murni hasil deteksi AI (Clean spaces)
            currentFoodName = intent.getStringExtra("DETECTED_FOOD")?.trim() ?: "Objek Tidak Dikenali"
            tvDetectedFoodName.text = currentFoodName.replaceFirstChar { it.uppercase() }

            // 2. Ambil Gambar dari Jalur Cache (Anti Crash/Looping)
            val imagePath = intent.getStringExtra("IMAGE_PATH")
            if (!imagePath.isNullOrEmpty()) {
                val imgFile = File(imagePath)
                if (imgFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    ivCapturedImage.setImageBitmap(bitmap)
                }
            }

            // 3. Mapping Nutrisi Real (Mendukung 24 Label)
            mapNutritionData(currentFoodName)

            // 4. Update UI Nutrisi
            val goalProt = 60
            val goalCarbs = 300
            val goalFat = 65

            tvCalValue.text = "$currentCal kcal"
            setProgressBarColors(pbProtein, Color.BLACK, Color.parseColor("#3D9471"))
            pbProtein.progress = if (goalProt > 0) (currentProt * 100 / goalProt).coerceAtMost(100) else 0
            tvProteinValue.text = "${currentProt}g / ${goalProt}g"

            setProgressBarColors(pbCarbs, Color.BLACK, Color.parseColor("#157BBF"))
            pbCarbs.progress = if (goalCarbs > 0) (currentCarbs * 100 / goalCarbs).coerceAtMost(100) else 0
            tvCarbsValue.text = "${currentCarbs}g / ${goalCarbs}g"

            setProgressBarColors(pbFat, Color.BLACK, Color.parseColor("#9484D9"))
            pbFat.progress = if (goalFat > 0) (currentFat * 100 / goalFat).coerceAtMost(100) else 0
            tvFatValue.text = "${currentFat}g / ${goalFat}g"
            
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memuat hasil analisis", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mapNutritionData(label: String) {
        val cleanLabel = label.lowercase().trim()
        when {
            // DATA MINUMAN (Real dari AI)
            cleanLabel.contains("kopi") -> { currentCal = 2; currentProt = 0; currentCarbs = 0; currentFat = 0 }
            cleanLabel.contains("teh") -> { currentCal = 65; currentProt = 0; currentCarbs = 16; currentFat = 0 }
            cleanLabel.contains("susu") -> { currentCal = 150; currentProt = 8; currentCarbs = 12; currentFat = 8 }
            cleanLabel.contains("jus") -> { currentCal = 180; currentProt = 1; currentCarbs = 25; currentFat = 5 }
            cleanLabel.contains("soda") -> { currentCal = 140; currentProt = 0; currentCarbs = 35; currentFat = 0 }
            cleanLabel.contains("air") -> { currentCal = 0; currentProt = 0; currentCarbs = 0; currentFat = 0 }
            cleanLabel.contains("minuman") -> { currentCal = 100; currentProt = 0; currentCarbs = 20; currentFat = 0 }
            
            // DATA MAKANAN
            cleanLabel == "nasi putih" -> { currentCal = 130; currentProt = 2; currentCarbs = 28; currentFat = 0 }
            cleanLabel == "nasi goreng" -> { currentCal = 250; currentProt = 5; currentCarbs = 30; currentFat = 9 }
            cleanLabel == "rendang" -> { currentCal = 195; currentProt = 20; currentCarbs = 4; currentFat = 11 }
            cleanLabel == "bakso" -> { currentCal = 200; currentProt = 12; currentCarbs = 15; currentFat = 10 }
            cleanLabel == "sate ayam" -> { currentCal = 225; currentProt = 18; currentCarbs = 3; currentFat = 15 }
            cleanLabel == "tahu goreng" -> { currentCal = 35; currentProt = 2; currentCarbs = 1; currentFat = 3 }
            cleanLabel == "tempe goreng" -> { currentCal = 50; currentProt = 3; currentCarbs = 2; currentFat = 4 }
            cleanLabel == "ikan mujair" -> { currentCal = 125; currentProt = 18; currentCarbs = 0; currentFat = 6 }
            
            else -> { currentCal = 80; currentProt = 4; currentCarbs = 10; currentFat = 3 }
        }
    }

    private fun setProgressBarColors(progressBar: ProgressBar, progressColor: Int, backgroundColor: Int) {
        val drawable = progressBar.progressDrawable?.mutate() as? LayerDrawable ?: return
        drawable.findDrawableByLayerId(android.R.id.background)?.setTint(backgroundColor)
        drawable.findDrawableByLayerId(android.R.id.progress)?.setTint(progressColor)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        btnFavorite.setOnClickListener { 
            isFavorite = !isFavorite
            btnFavorite.setImageResource(if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border)
        }
        btnSaveAnalysis.setOnClickListener {
            val pref = getSharedPreferences("UserStats", Context.MODE_PRIVATE)
            val editor = pref.edit()
            editor.putFloat("consumed_calories", pref.getFloat("consumed_calories", 0f) + currentCal)
            editor.putFloat("consumed_protein", pref.getFloat("consumed_protein", 0f) + currentProt)
            editor.putFloat("consumed_carbs", pref.getFloat("consumed_carbs", 0f) + currentCarbs)
            editor.putFloat("consumed_fat", pref.getFloat("consumed_fat", 0f) + currentFat)
            editor.apply()
            Toast.makeText(this, "Tersimpan di Catatan Harian", Toast.LENGTH_SHORT).show()
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
