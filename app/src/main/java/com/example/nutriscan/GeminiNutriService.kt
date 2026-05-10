package com.example.nutriscan

import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.scale
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        val apiKey = "AIzaSyDztxgxelx074A5NwcDxKTQyVKnmhOHobk"
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = apiKey,
                generationConfig = generationConfig { temperature = 0.1f }
            )

            val prompt = """
                Analisis gizi makanan: $detectedLabel. 
                Konteks harian: $dailyContext.
                Berikan respon dalam format JSON murni:
                {
                  "nama_spesifik": "Nama makanan detail",
                  "berat_porsi": "150g",
                  "kalori": 200,
                  "protein": 10,
                  "karbohidrat": 30,
                  "lemak": 5,
                  "saran": "Tips gizi singkat (25 kata)"
                }
            """.trimIndent()

            val resizedBitmap = bitmap?.let { 
                val maxSize = 512
                val ratio = it.width.toFloat() / it.height.toFloat()
                val (fWidth, fHeight) = if (ratio > 1) maxSize to (maxSize / ratio).toInt() else (maxSize * ratio).toInt() to maxSize
                it.scale(fWidth, fHeight, true)
            }

            val inputContent = content {
                if (resizedBitmap != null) image(resizedBitmap)
                text(prompt)
            }

            val response = generativeModel.generateContent(inputContent)
            val responseText = response.text ?: return@withContext NutriResponse("", "", 0, 0, 0, 0, "", "[$timestamp] Error: Respon kosong.")

            val jsonStart = responseText.indexOf("{")
            val jsonEnd = responseText.lastIndexOf("}") + 1
            if (jsonStart == -1) return@withContext NutriResponse("", "", 0, 0, 0, 0, "", "[$timestamp] Error: Format JSON salah.")

            val json = JSONObject(responseText.substring(jsonStart, jsonEnd))
            NutriResponse(
                specificName = json.optString("nama_spesifik", detectedLabel),
                portionWeight = json.optString("berat_porsi", "1 porsi"),
                calories = json.optInt("kalori", 0),
                protein = json.optInt("protein", 0),
                carbs = json.optInt("karbohidrat", 0),
                fat = json.optInt("lemak", 0),
                advice = json.optString("saran", "Analisis berhasil.")
            )

        } catch (e: Exception) {
            val detail = e.localizedMessage ?: "Unknown Error"
            NutriResponse("", "", 0, 0, 0, 0, "", "[$timestamp] Gagal: $detail")
        }
    }
}
