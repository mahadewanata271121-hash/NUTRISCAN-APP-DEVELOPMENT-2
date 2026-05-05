package com.example.nutriscan

import android.annotation.SuppressLint
import android.app.ProgressDialog
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class AiCameraActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private var imageCapture: ImageCapture? = null
    private var cameraControl: CameraControl? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var foodDetector: FoodDetector
    private var progressDialog: ProgressDialog? = null
    
    private lateinit var tvStatus: TextView
    private lateinit var statusIndicator: View
    private var lastDetection: String? = null
    private var isProcessing = AtomicBoolean(false)
    
    private var lastAnalysisTime = 0L
    private val ANALYSIS_INTERVAL_MS = 600L

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) processGalleryImage(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_camera)

        viewFinder = findViewById(R.id.viewFinder)
        tvStatus = findViewById(R.id.tvStatus)
        statusIndicator = findViewById(R.id.statusIndicator)
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        foodDetector = FoodDetector(this)

        startCamera()

        findViewById<ImageButton>(R.id.btnCapture).setOnClickListener { 
            if (!isProcessing.get()) takePhoto() 
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
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also { it.setSurfaceProvider(viewFinder.surfaceProvider) }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
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
                val camera = cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, 
                    preview, imageCapture, imageAnalyzer
                )
                cameraControl = camera.cameraControl
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

            val result = foodDetector.analyzeFrame(bitmap)
            bitmap.recycle()

            runOnUiThread {
                // Ambang batas ditingkatkan ke 0.55 agar lebih akurat dan tidak menebak sembarang objek
                if (result != null && result.confidence > 0.55f) {
                    lastDetection = result.label
                    val type = if (result.isDrink) "Minuman" else "Makanan"
                    tvStatus.text = "Terdeteksi: $lastDetection ($type)"
                    statusIndicator.setBackgroundResource(R.drawable.circle_green)
                } else {
                    // Reset deteksi jika tidak yakin agar tidak membawa hasil lama
                    lastDetection = null
                    tvStatus.text = "Objek tidak terdeteksi"
                    statusIndicator.setBackgroundResource(R.drawable.circle_background_grey)
                }
            }
        } catch (e: Exception) {
            image.close()
        }
    }

    private fun takePhoto() {
        if (isProcessing.getAndSet(true)) return
        showLoading(true)

        val finalResult = lastDetection
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.toBitmap()
                    val rotation = image.imageInfo.rotationDegrees
                    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    if (bitmap != rotatedBitmap) bitmap.recycle()
                    image.close()
                    
                    runOnUiThread {
                        if (finalResult != null) {
                            navigateToResult(rotatedBitmap, finalResult)
                            isProcessing.set(false)
                            showLoading(false)
                        } else {
                            // Cek sekali lagi dengan bitmap yang sudah dirotasi (lebih akurat)
                            cameraExecutor.execute {
                                val secondChance = foodDetector.detect(rotatedBitmap)
                                runOnUiThread {
                                    showLoading(false)
                                    isProcessing.set(false)
                                    if (secondChance != null) {
                                        navigateToResult(rotatedBitmap, secondChance)
                                    } else {
                                        Toast.makeText(this@AiCameraActivity, "Objek tidak terdeteksi", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                }
                override fun onError(exc: ImageCaptureException) {
                    showLoading(false)
                    isProcessing.set(false)
                    Toast.makeText(this@AiCameraActivity, "Gagal mengambil gambar", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun processGalleryImage(uri: Uri) {
        if (isProcessing.getAndSet(true)) return
        showLoading(true)
        cameraExecutor.execute {
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        val rotated = rotateImageIfRequired(bitmap, uri)
                        val result = foodDetector.detect(rotated)
                        runOnUiThread {
                            showLoading(false)
                            isProcessing.set(false)
                            if (result != null) {
                                navigateToResult(rotated, result)
                            } else {
                                Toast.makeText(this@AiCameraActivity, "Objek tidak terdeteksi", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { 
                    showLoading(false)
                    isProcessing.set(false)
                    Toast.makeText(this@AiCameraActivity, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun resetFocusAndExposure() {
        val factory = viewFinder.meteringPointFactory
        val centerPoint = factory.createPoint(viewFinder.width / 2f, viewFinder.height / 2f)
        val action = FocusMeteringAction.Builder(centerPoint).build()
        cameraControl?.startFocusAndMetering(action)
    }

    private fun showLoading(show: Boolean) {
        runOnUiThread {
            if (show) {
                if (progressDialog == null) {
                    progressDialog = ProgressDialog(this).apply {
                        setMessage("Menganalisis...")
                        setCancelable(false)
                    }
                }
                progressDialog?.show()
            } else {
                progressDialog?.dismiss()
                progressDialog = null
            }
        }
    }

    private fun navigateToResult(bitmap: Bitmap, foodName: String) {
        try {
            val file = File(cacheDir, "scan_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            val intent = Intent(this, AnalysisResultActivity::class.java).apply {
                putExtra("IMAGE_PATH", file.absolutePath)
                putExtra("DETECTED_FOOD", foodName)
            }
            startActivity(intent)
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
        super.onDestroy()
        foodDetector.close()
        cameraExecutor.shutdown()
    }
}
