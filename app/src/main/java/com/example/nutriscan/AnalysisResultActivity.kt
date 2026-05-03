package com.example.nutriscan

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnalysisResultActivity : AppCompatActivity() {

    private lateinit var ivCapturedImage: ImageView
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
    private var currentFoodName = "Ayam Goreng"
    private var currentCal = 260
    private var currentProt = 22
    private var currentCarbs = 5
    private var currentFat = 17
    private var currentImageRes = R.drawable.illustration22

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis_result)

        initViews()
        setupDummyData()
        setupListeners()
        
        resetFavoriteState()

        if (savedInstanceState == null) {
            updateRecentScans()
        } else {
            isFavorite = savedInstanceState.getBoolean("is_favorite", false)
            updateFavoriteIcon()
            displayRecentScans()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("is_favorite", isFavorite)
    }

    private fun initViews() {
        ivCapturedImage = findViewById(R.id.ivCapturedImage)
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
        
        val sdf = SimpleDateFormat("EEEE, d MMM yyyy • HH:mm", Locale("id", "ID"))
        tvAnalysisTimestamp.text = sdf.format(Date())
    }

    private fun setupDummyData() {
        val byteArray = intent.getByteArrayExtra("CAPTURED_IMAGE")
        if (byteArray != null) {
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            ivCapturedImage.setImageBitmap(bitmap)
        }

        val goalProt = 60
        val goalCarbs = 300
        val goalFat = 65

        tvCalValue.text = "$currentCal kcal"
        
        // Memastikan warna progress bar tetap Hitam (Versi Terbaik)
        setProgressBarColors(pbProtein, Color.BLACK, Color.parseColor("#3D9471"))
        pbProtein.progress = (currentProt * 100 / goalProt).coerceAtMost(100)
        tvProteinValue.text = "${currentProt}g / ${goalProt}g"

        setProgressBarColors(pbCarbs, Color.BLACK, Color.parseColor("#157BBF"))
        pbCarbs.progress = (currentCarbs * 100 / goalCarbs).coerceAtMost(100)
        tvCarbsValue.text = "${currentCarbs}g / ${goalCarbs}g"

        setProgressBarColors(pbFat, Color.BLACK, Color.parseColor("#9484D9"))
        pbFat.progress = (currentFat * 100 / goalFat).coerceAtMost(100)
        tvFatValue.text = "${currentFat}g / ${goalFat}g"
    }

    private fun setProgressBarColors(progressBar: ProgressBar, progressColor: Int, backgroundColor: Int) {
        val drawable = progressBar.progressDrawable?.mutate() as? LayerDrawable ?: return
        val background = drawable.findDrawableByLayerId(android.R.id.background)
        val progress = drawable.findDrawableByLayerId(android.R.id.progress)
        
        background?.setTint(backgroundColor)
        progress?.setTint(progressColor)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        btnFavorite.setOnClickListener { toggleFavorite() }
        btnSaveAnalysis.setOnClickListener {
            saveDataToDailyLog()
            Toast.makeText(this, "Analisis berhasil disimpan ke Catatan Harian", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun updateRecentScans() {
        val pref = getSharedPreferences("UserStats", Context.MODE_PRIVATE)
        val recentScansData = pref.getString("recent_scans_v2", "") ?: ""
        
        val items = recentScansData.split("#").filter { it.isNotEmpty() }.mapNotNull {
            val parts = it.split("|")
            try {
                if (parts.size >= 7) {
                    ScannedFood(parts[0], parts[1].toInt(), parts[2].toInt(), parts[3].toInt(), parts[4].toInt(), parts[5].toInt(), parts[6])
                } else null
            } catch (e: Exception) { null }
        }.toMutableList()

        val currentTime = SimpleDateFormat("EEEE, d MMM yyyy • HH:mm", Locale("id", "ID")).format(Date())
        val currentScan = ScannedFood(currentFoodName, currentCal, currentProt, currentCarbs, currentFat, currentImageRes, currentTime)
        items.add(0, currentScan)

        val finalItems = if (items.size > 10) items.take(10) else items
        val dataToSave = finalItems.joinToString("#") { 
            "${it.name}|${it.cal}|${it.prot}|${it.carbs}|${it.fat}|${it.imageRes}|${it.timestamp}"
        }
        pref.edit().putString("recent_scans_v2", dataToSave).apply()

        rvScanHistory.adapter = ScanHistoryAdapter(finalItems)
    }

    private fun displayRecentScans() {
        val pref = getSharedPreferences("UserStats", Context.MODE_PRIVATE)
        val recentScansData = pref.getString("recent_scans_v2", "") ?: ""
        val items = recentScansData.split("#").filter { it.isNotEmpty() }.mapNotNull {
            val parts = it.split("|")
            try {
                if (parts.size >= 7) {
                    ScannedFood(parts[0], parts[1].toInt(), parts[2].toInt(), parts[3].toInt(), parts[4].toInt(), parts[5].toInt(), parts[6])
                } else null
            } catch (e: Exception) { null }
        }
        rvScanHistory.adapter = ScanHistoryAdapter(items)
    }

    private fun resetFavoriteState() {
        isFavorite = false
        updateFavoriteIcon()
    }

    private fun toggleFavorite() {
        val pref = getSharedPreferences("UserStats", Context.MODE_PRIVATE)
        val editor = pref.edit()
        var favorites = pref.getString("favorite_foods", "") ?: ""

        if (isFavorite) {
            val entryToRemove = "$currentFoodName|$currentCal|$currentProt|$currentCarbs|$currentFat|$currentImageRes#"
            favorites = favorites.replace(entryToRemove, "")
            isFavorite = false
            Toast.makeText(this, "Dihapus dari favorit", Toast.LENGTH_SHORT).show()
        } else {
            favorites += "$currentFoodName|$currentCal|$currentProt|$currentCarbs|$currentFat|$currentImageRes#"
            isFavorite = true
            Toast.makeText(this, "Ditambahkan ke favorit", Toast.LENGTH_SHORT).show()
        }

        editor.putString("favorite_foods", favorites).apply()
        updateFavoriteIcon()
    }

    private fun updateFavoriteIcon() {
        btnFavorite.setImageResource(if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border)
    }

    private fun saveDataToDailyLog() {
        val pref = getSharedPreferences("UserStats", Context.MODE_PRIVATE)
        val editor = pref.edit()
        
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val entry = "$currentFoodName|$time|$currentCal kkal"
        val oldHistory = pref.getString("daily_food_history", "")
        
        editor.putString("daily_food_history", if (oldHistory.isNullOrEmpty()) entry else "$oldHistory#$entry")
        editor.putFloat("consumed_calories", pref.getFloat("consumed_calories", 0f) + currentCal)
        editor.putFloat("consumed_protein", pref.getFloat("consumed_protein", 0f) + currentProt)
        editor.putFloat("consumed_carbs", pref.getFloat("consumed_carbs", 0f) + currentCarbs)
        editor.putFloat("consumed_fat", pref.getFloat("consumed_fat", 0f) + currentFat)
        editor.apply()
    }

    data class ScannedFood(val name: String, val cal: Int, val prot: Int, val carbs: Int, val fat: Int, val imageRes: Int, val timestamp: String)

    class ScanHistoryAdapter(private val items: List<ScannedFood>) : RecyclerView.Adapter<ScanHistoryAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvFoodName)
            val tvDetails: TextView = view.findViewById(R.id.tvNutrientDetails)
            val tvDate: TextView = view.findViewById(R.id.tvDateTime)
            val ivFood: ImageView = view.findViewById(R.id.iv_food_icon)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_scanned_food, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.name
            holder.tvDetails.text = "${item.cal} kcal | P: ${item.prot}g | K: ${item.carbs}g | L: ${item.fat}g"
            holder.tvDate.text = item.timestamp
            holder.ivFood.setImageResource(item.imageRes)
        }

        override fun getItemCount() = items.size
    }
}
