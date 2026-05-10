package com.example.nutriscan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FoodDetector(context: Context) {

    private var foodInterpreter: Interpreter? = null
    private var drinkInterpreter: Interpreter? = null
    private var foodLabels: List<String> = emptyList()
    private var drinkLabels: List<String> = emptyList()
    private val inputSize = 640

    private val inputBuffer: ByteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3).apply {
        order(ByteOrder.nativeOrder())
    }
    private val pixels = IntArray(inputSize * inputSize)
    private val letterboxBitmap = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
    private val letterboxCanvas = Canvas(letterboxBitmap)
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)

    data class Recognition(
        val label: String,
        val confidence: Float,
        val isDrink: Boolean
    )

    init {
        try {
            val options = Interpreter.Options().apply {
                setNumThreads(2) 
            }
            foodInterpreter = Interpreter(FileUtil.loadMappedFile(context, "nutriscan_food_float32.tflite"), options)
            foodLabels = FileUtil.loadLabels(context, "labels_food.txt").map { it.trim() }

            drinkInterpreter = Interpreter(FileUtil.loadMappedFile(context, "nutriscan_drink_float32.tflite"), options)
            drinkLabels = FileUtil.loadLabels(context, "labels_drink.txt").map { it.trim() }
            
            Log.d("FoodDetector", "Optimized Dual AI Engine Ready.")
        } catch (e: Exception) {
            Log.e("FoodDetector", "Init Error: ${e.message}")
        }
    }

    @Synchronized
    fun analyzeFrame(bitmap: Bitmap): Recognition? {
        val fInterp = foodInterpreter ?: return null
        val dInterp = drinkInterpreter ?: return null

        try {
            preprocess(bitmap)
            inputBuffer.rewind()
            val foodRes = runInference(fInterp, foodLabels, inputBuffer, 0.45f, false)
            
            inputBuffer.rewind()
            val drinkRes = runInference(dInterp, drinkLabels, inputBuffer, 0.40f, true)

            return when {
                foodRes != null && drinkRes != null -> {
                    if (foodRes.confidence + 0.15f >= drinkRes.confidence) foodRes else drinkRes
                }
                foodRes != null -> foodRes
                drinkRes != null -> drinkRes
                else -> null
            }
        } catch (e: Exception) {
            return null
        }
    }

    private fun preprocess(src: Bitmap) {
        letterboxCanvas.drawColor(Color.rgb(114, 114, 114))
        val scale = inputSize.toFloat() / maxOf(src.width, src.height)
        val w = src.width * scale
        val h = src.height * scale
        val left = (inputSize - w) / 2f
        val top = (inputSize - h) / 2f

        val srcRect = Rect(0, 0, src.width, src.height)
        val dstRect = RectF(left, top, left + w, top + h)
        letterboxCanvas.drawBitmap(src, srcRect, dstRect, paint)

        letterboxBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        inputBuffer.rewind()
        for (p in pixels) {
            inputBuffer.putFloat(((p shr 16) and 0xFF) / 255f)
            inputBuffer.putFloat(((p shr 8) and 0xFF) / 255f)
            inputBuffer.putFloat((p and 0xFF) / 255f)
        }
    }

    fun detect(bitmap: Bitmap): String? = analyzeFrame(bitmap)?.label

    private fun runInference(interp: Interpreter, labels: List<String>, buffer: ByteBuffer, threshold: Float, isDrink: Boolean): Recognition? {
        val shape = interp.getOutputTensor(0).shape()
        val dim1 = shape[1]
        val dim2 = shape[2]
        val output = Array(1) { Array(dim1) { FloatArray(dim2) } }

        return try {
            interp.run(buffer, output)
            val best = findBest(output[0], dim1, dim2, labels, threshold)
            best?.let { Recognition(it.first, it.second, isDrink) }
        } catch (e: Exception) {
            null
        }
    }

    private fun findBest(output: Array<FloatArray>, dim1: Int, dim2: Int, labels: List<String>, threshold: Float): Pair<String, Float>? {
        var maxScore = 0f
        var maxIdx = -1
        val isChannelFirst = dim1 < dim2
        val numBoxes = if (isChannelFirst) dim2 else dim1
        val numChannels = if (isChannelFirst) dim1 else dim2
        val offset = 4

        for (i in 0 until numBoxes) {
            for (c in 0 until (numChannels - offset)) {
                if (c >= labels.size) break
                val score = if (isChannelFirst) output[offset + c][i] else output[i][offset + c]
                if (score > threshold && score > maxScore) {
                    maxScore = score
                    maxIdx = c
                }
            }
        }
        return if (maxIdx != -1) labels[maxIdx] to maxScore else null
    }

    fun close() {
        foodInterpreter?.close()
        drinkInterpreter?.close()
        letterboxBitmap.recycle()
    }
}
