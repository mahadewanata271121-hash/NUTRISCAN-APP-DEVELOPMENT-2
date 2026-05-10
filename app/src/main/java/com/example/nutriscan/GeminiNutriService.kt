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
            return@withContext NutriResponse("", "", 0, 0, 0, 0, "", "[$timestamp] API Key belum diset.")
        }

        var scaledBitmap: Bitmap? = null
        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-2.5-flash", // PERBAIKAN: Menggunakan versi 2.5 Flash yang stabil
                apiKey = apiKey,
                generationConfig = generationConfig {
                    temperature = 0.4f
                    responseMimeType = "application/json"
                },
                requestOptions = RequestOptions(timeout = 180.seconds)
            )

            val prompt = """
                Analisis makanan: $detectedLabel. Konteks user: $dailyContext.
                Berikan respon JSON murni tanpa teks tambahan:
                {
                  "nama_spesifik": "...",
                  "berat_porsi": "...",
                  "kalori": 0,
                  "protein": 0,
                  "karbohidrat": 0,
                  "lemak": 0,
                  "saran": "..."
                }
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
                return@withContext NutriResponse("", "", 0, 0, 0, 0, "", "[$timestamp] AI tidak memberikan format data yang lengkap.")
            }

            val jsonStr = responseText.substring(jsonStart, jsonEnd + 1)
            val json = JSONObject(jsonStr)

            NutriResponse(
                specificName = json.optString("nama_spesifik", detectedLabel),
                portionWeight = json.optString("berat_porsi", "1 porsi"),
                calories = json.optInt("kalori", 0),
                protein = json.optInt("protein", 0),
                carbs = json.optInt("karbohidrat", 0),
                fat = json.optInt("lemak", 0),
                advice = json.optString("saran", "Analisis selesai.")
            )

        } catch (t: Throwable) {
            val errMsg = t.message ?: ""
            val userFriendlyMsg = when {
                errMsg.contains("429") -> "[$timestamp] Terlalu banyak permintaan (Limit API). Tunggu 1 menit."
                errMsg.contains("timeout") -> "[$timestamp] Koneksi lambat. Coba lagi."
                else -> "[$timestamp] Gangguan: ${t.localizedMessage}"
            }
            NutriResponse("", "", 0, 0, 0, 0, "", userFriendlyMsg)
        } finally {
            if (scaledBitmap != null && scaledBitmap != bitmap) {
                scaledBitmap?.recycle()
            }
        }
    }
}
