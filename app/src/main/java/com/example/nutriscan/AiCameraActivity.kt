package com.example.nutriscan

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class AiCameraActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private var imageCapture: ImageCapture? = null
    private var cameraControl: CameraControl? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var foodDetector: FoodDetector
    
    private lateinit var tvStatus: TextView
    private lateinit var statusIndicator: View
    private lateinit var btnCapture: ImageButton
    private var lastDetection: String? = null
    private var isProcessing = AtomicBoolean(false)
    
    private var lastAnalysisTime = 0L
    private val ANALYSIS_INTERVAL_MS = 1200L
    private val STRICT_THRESHOLD = 0.85f

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) processGalleryImage(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_camera)

        viewFinder = findViewById(R.id.viewFinder)
        tvStatus = findViewById(R.id.tvStatus)
        statusIndicator = findViewById(R.id.statusIndicator)
        btnCapture = findViewById(R.id.btnCapture)
        
        btnCapture.isEnabled = false
        btnCapture.alpha = 0.5f

        cameraExecutor = Executors.newSingleThreadExecutor()
        foodDetector = FoodDetector(this)

        startCamera()

        btnCapture.setOnClickListener {
            if (lastDetection != null && !isProcessing.get()) {
                takePhoto()
            }
        }

        findViewById<ImageButton>(R.id.btnClose).setOnClickListener { finish() }
        findViewById<MaterialCardView>(R.id.btnGallery).setOnClickListener {
            if (!isProcessing.get()) {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
        
        viewFinder.setOnClickListener { resetFocusAndExposure() }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = try {
                cameraProviderFuture.get()
            } catch (e: Exception) { return@addListener }
            
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also { it.setSurfaceProvider(viewFinder.surfaceProvider) }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        val currentTime = System.currentTimeMillis()
                        if (!isProcessing.get() && (currentTime - lastAnalysisTime) > ANALYSIS_INTERVAL_MS) {
                            lastAnalysisTime = currentTime
                            performRealTimeDetection(image)
                        } else {
                            image.close()
                        }
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, 
                    preview, imageCapture, imageAnalyzer
                )
            } catch (exc: Exception) { 
                Log.e("AiCamera", "Binding failed", exc) 
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun performRealTimeDetection(image: ImageProxy) {
        try {
            val bitmap = image.toBitmap()
            image.close()

            if (bitmap != null) {
                val result = foodDetector.analyzeFrame(bitmap)
                bitmap.recycle()

                runOnUiThread {
                    if (result != null && result.confidence > STRICT_THRESHOLD) {
                        lastDetection = result.label
                        tvStatus.text = "Terdeteksi: $lastDetection"
                        statusIndicator.setBackgroundResource(R.drawable.circle_green)
                        btnCapture.isEnabled = true
                        btnCapture.alpha = 1.0f
                    } else {
                        lastDetection = null
                        tvStatus.text = "Mencari Objek..."
                        statusIndicator.setBackgroundResource(R.drawable.circle_background_grey)
                        btnCapture.isEnabled = false
                        btnCapture.alpha = 0.5f
                    }
                }
            }
        } catch (e: Exception) {
            image.close()
        }
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return
        if (isProcessing.getAndSet(true)) return

        capture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.toBitmap()
                    val rotation = image.imageInfo.rotationDegrees
                    image.close()
                    
                    if (bitmap != null) {
                        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        if (bitmap != rotatedBitmap) bitmap.recycle()
                        
                        runOnUiThread {
                            navigateToResult(rotatedBitmap, lastDetection ?: "Makanan")
                        }
                    }
                    isProcessing.set(false)
                }
                override fun onError(exc: ImageCaptureException) {
                    runOnUiThread {
                        isProcessing.set(false)
                        Toast.makeText(this@AiCameraActivity, "Gagal mengambil gambar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    private fun processGalleryImage(uri: Uri) {
        if (isProcessing.getAndSet(true)) return
        cameraExecutor.execute {
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                    val bitmap = BitmapFactory.decodeStream(stream, null, options)
                    if (bitmap != null) {
                        val rotated = rotateImageIfRequired(bitmap, uri)
                        val result = foodDetector.analyzeFrame(rotated)
                        runOnUiThread {
                            isProcessing.set(false)
                            if (result != null && result.confidence > STRICT_THRESHOLD) {
                                navigateToResult(rotated, result.label)
                            } else {
                                Toast.makeText(this@AiCameraActivity, "Objek tidak terdeteksi", Toast.LENGTH_SHORT).show()
                                rotated.recycle()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { isProcessing.set(false) }
            }
        }
    }

    private fun resetFocusAndExposure() {
        val factory = viewFinder.meteringPointFactory
        val point = factory.createPoint(viewFinder.width / 2f, viewFinder.height / 2f)
        cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
    }

    private fun navigateToResult(bitmap: Bitmap, foodName: String) {
        try {
            val imagesDir = File(filesDir, "food_images").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(Date())
            val file = File(imagesDir, "IMG_$timeStamp.jpg")
            
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            
            val intent = Intent(this, AnalysisResultActivity::class.java).apply {
                putExtra("IMAGE_PATH", file.absolutePath)
                putExtra("DETECTED_FOOD", foodName)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
            bitmap.recycle()
            finish()
        } catch (e: Exception) {
            Log.e("AiCamera", "Save error: ${e.message}")
        }
    }

    private fun rotateImageIfRequired(img: Bitmap, uri: Uri): Bitmap {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return img
            val ei = ExifInterface(inputStream)
            val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            inputStream.close()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotate(img, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotate(img, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotate(img, 270f)
                else -> img
            }
        } catch (e: Exception) { img }
    }

    private fun rotate(img: Bitmap, deg: Float): Bitmap {
        val m = Matrix().apply { postRotate(deg) }
        val res = Bitmap.createBitmap(img, 0, 0, img.width, img.height, m, true)
        if (res != img) img.recycle()
        return res
    }

    override fun onDestroy() {
        isProcessing.set(true) 
        try {
            cameraExecutor.shutdownNow()
            foodDetector.close()
        } catch (e: Exception) {
            Log.e("AiCamera", "Error during cleanup: ${e.message}")
        }
        super.onDestroy()
    }
}
