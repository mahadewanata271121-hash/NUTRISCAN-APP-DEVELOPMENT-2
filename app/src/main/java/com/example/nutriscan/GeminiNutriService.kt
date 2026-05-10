package com.example.nutriscan

import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.scale
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

class GeminiNutriService {

    data class NutriResponse(
        val specificName: String,
        val portionWeight: String,
        val calories: Int,
        val protein: Int,
        val carbs: Int,
        val fat: Int,
        val advice: String,
        val healthRisk: String = "",
        val errorMsg: String? = null
    )

    suspend fun getNutritionData(
        detectedLabel: String,
        bitmap: Bitmap?,
        dailyContext: String
    ): NutriResponse = withContext(Dispatchers.IO) {

        val apiKey = BuildConfig.GEMINI_API_KEY
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        if (apiKey.isEmpty()) {
            return@withContext NutriResponse("", "", 0, 0, 0, 0, "", "", "[$timestamp] API Key belum diset.")
        }

        var scaledBitmap: Bitmap? = null
        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-2.5-flash", 
                apiKey = apiKey,
                generationConfig = generationConfig {
                    temperature = 0.1f // Sangat rendah untuk stabilitas format JSON
                    responseMimeType = "application/json"
                },
                // PERBAIKAN: Memaksa penggunaan v1beta karena JSON mode (responseMimeType) memerlukan endpoint ini
                requestOptions = RequestOptions(timeout = 180.seconds, apiVersion = "v1beta")
            )

            val prompt = """
                Bekerjalah sebagai pakar gizi profesional.
                Target: $detectedLabel.
                Konteks harian user: $dailyContext.
                
                Tugas: Berikan analisis gizi mendalam berdasarkan Tabel Komposisi Pangan Indonesia (TKPI).
                
                WAJIB merespon hanya dalam format JSON murni berikut:
                {
                  "nama_spesifik": "nama lengkap makanan", 
                  "berat_porsi": "estimasi berat (contoh: 150g)", 
                  "kalori": 100, 
                  "protein": 10, 
                  "karbohidrat": 20, 
                  "lemak": 5, 
                  "saran": "saran kesehatan singkat",
                  "risiko_kesehatan": "analisis singkat risiko kesehatan terkait bahan/porsi"
                }
                
                ATURAN KETAT:
                1. Field 'kalori', 'protein', 'karbohidrat', 'lemak' HARUS berupa angka murni (integer), dilarang menyertakan teks satuan.
                2. Field 'risiko_kesehatan' tidak boleh kosong. Analisis risiko terkait kandungan lemak jenuh, gula, garam, atau alergen.
                3. Jangan sertakan teks apapun di luar blok JSON.
            """.trimIndent()

            val imageToUse = bitmap?.let {
                if (it.isRecycled) return@let null
                val maxSize = 768
                if (it.width <= maxSize && it.height <= maxSize) it
                else {
                    val ratio = it.width.toFloat() / it.height.toFloat()
                    val (fWidth, fHeight) = if (ratio > 1) maxSize to (maxSize / ratio).toInt() else (maxSize * ratio).toInt() to maxSize
                    scaledBitmap = it.scale(fWidth.coerceAtLeast(1), fHeight.coerceAtLeast(1), true)
                    scaledBitmap
                }
            }

            val inputContent = content {
                if (imageToUse != null && !imageToUse.isRecycled) image(imageToUse)
                text(prompt)
            }

            val response = generativeModel.generateContent(inputContent)
            val responseText = response.text ?: ""

            val jsonStart = responseText.indexOf("{")
            val jsonEnd = responseText.lastIndexOf("}")

            if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) {
                return@withContext NutriResponse("", "", 0, 0, 0, 0, "", "", "[$timestamp] AI gagal memformat data JSON.")
            }

            val jsonStr = responseText.substring(jsonStart, jsonEnd + 1)
            val json = JSONObject(jsonStr)

            // Helper parsing angka yang lebih tangguh (alias dukungan nama key bahasa Inggris)
            fun getSafeInt(keyId: String, keyEn: String): Int {
                val raw = if (json.has(keyId)) json.optString(keyId) else json.optString(keyEn, "0")
                return raw.filter { it.isDigit() }.toIntOrNull() ?: 0
            }

            NutriResponse(
                specificName = json.optString("nama_spesifik", detectedLabel),
                portionWeight = json.optString("berat_porsi", "1 porsi"),
                calories = getSafeInt("kalori", "calories"),
                protein = getSafeInt("protein", "protein"),
                carbs = getSafeInt("karbohidrat", "carbohydrates"),
                fat = getSafeInt("lemak", "fat"),
                advice = json.optString("saran", "Analisis gizi berhasil."),
                healthRisk = json.optString("risiko_kesehatan", "Tidak terdeteksi risiko signifikan.")
            )

        } catch (t: Throwable) {
            val errMsg = t.message ?: ""
            val userFriendlyMsg = when {
                errMsg.contains("429") -> "[$timestamp] Terlalu banyak permintaan. Tunggu sebentar."
                else -> "[$timestamp] Gangguan: ${t.localizedMessage}"
            }
            NutriResponse("", "", 0, 0, 0, 0, "", "", userFriendlyMsg)
        } finally {
            if (scaledBitmap != null && scaledBitmap != bitmap) {
                scaledBitmap?.recycle()
            }
        }
    }
}
