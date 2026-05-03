package com.example.nutriscan

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
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
import java.io.InputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AiCameraActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            processGalleryImage(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_camera)

        viewFinder = findViewById(R.id.viewFinder)
        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()

        findViewById<ImageButton>(R.id.btnCapture).setOnClickListener { takePhoto() }
        findViewById<ImageButton>(R.id.btnClose).setOnClickListener { finish() }
        findViewById<MaterialCardView>(R.id.btnGallery).setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("AiCamera", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()
                    
                    // Standardized to 640px
                    val optimizedBitmap = resizeBitmap(bitmap, 640)
                    navigateToResult(optimizedBitmap)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("AiCamera", "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(this@AiCameraActivity, "Gagal mengambil foto", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun processGalleryImage(uri: Uri) {
        cameraExecutor.execute {
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = false
                    inSampleSize = 2 
                }
                val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()
                
                if (bitmap != null) {
                    val optimizedBitmap = resizeBitmap(bitmap, 640)
                    runOnUiThread { navigateToResult(optimizedBitmap) }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun resizeBitmap(source: Bitmap, maxLength: Int): Bitmap {
        val width = source.width
        val height = source.height
        val scale = maxLength.toFloat() / Math.max(width, height)
        if (scale >= 1.0) return source
        
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        val resized = Bitmap.createBitmap(source, 0, 0, width, height, matrix, true)
        if (resized != source) source.recycle()
        return resized
    }

    private fun navigateToResult(bitmap: Bitmap) {
        try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
            val byteArray = stream.toByteArray()
            
            if (byteArray.size > 1000000) {
                stream.reset()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 40, stream)
            }

            val intent = Intent(this, AnalysisResultActivity::class.java).apply {
                putExtra("CAPTURED_IMAGE", stream.toByteArray())
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: Gambar terlalu besar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        
        val rotation = image.imageInfo.rotationDegrees
        if (rotation == 0) return bitmap
        
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
