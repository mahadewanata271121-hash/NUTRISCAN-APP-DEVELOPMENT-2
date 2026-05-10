package com.example.nutriscan

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

class ScanningOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }
    
    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val cornerPath = Path()
    private var scanLineY = 0f
    private val scanRect = RectF()
    private var animator: ValueAnimator? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        
        val rectSize = w * 0.75f
        val left = (w - rectSize) / 2
        val top = (h - rectSize) / 3f
        scanRect.set(left, top, left + rectSize, top + rectSize)
        
        if (!isInEditMode) {
            startAnimation()
        } else {
            scanLineY = scanRect.top + (scanRect.height() / 2)
        }
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(scanRect.top, scanRect.bottom).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener {
                scanLineY = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (scanRect.isEmpty) return

        // 1. Gambar Overlay Gelap
        paint.color = Color.parseColor("#99000000")
        paint.style = Paint.Style.FILL
        paint.shader = null
        
        canvas.drawRect(0f, 0f, width.toFloat(), scanRect.top, paint)
        canvas.drawRect(0f, scanRect.bottom, width.toFloat(), height.toFloat(), paint)
        canvas.drawRect(0f, scanRect.top, scanRect.left, scanRect.bottom, paint)
        canvas.drawRect(scanRect.right, scanRect.top, width.toFloat(), scanRect.bottom, paint)

        // 2. Gambar Siku
        drawCorners(canvas)

        // 3. Gambar Garis Scan
        if (scanLineY >= scanRect.top && scanLineY <= scanRect.bottom) {
            canvas.drawLine(scanRect.left, scanLineY, scanRect.right, scanLineY, linePaint)
            
            val gradient = LinearGradient(0f, scanLineY - 100, 0f, scanLineY,
                intArrayOf(Color.TRANSPARENT, Color.parseColor("#30FFFFFF")),
                null, Shader.TileMode.CLAMP)
            paint.shader = gradient
            canvas.drawRect(scanRect.left, scanLineY - 100, scanRect.right, scanLineY, paint)
            paint.shader = null
        }
    }

    private fun drawCorners(canvas: Canvas) {
        val len = 60f
        cornerPath.reset()
        cornerPath.moveTo(scanRect.left, scanRect.top + len)
        cornerPath.lineTo(scanRect.left, scanRect.top)
        cornerPath.lineTo(scanRect.left + len, scanRect.top)
        
        cornerPath.moveTo(scanRect.right - len, scanRect.top)
        cornerPath.lineTo(scanRect.right, scanRect.top)
        cornerPath.lineTo(scanRect.right, scanRect.top + len)
        
        cornerPath.moveTo(scanRect.right, scanRect.bottom - len)
        cornerPath.lineTo(scanRect.right, scanRect.bottom)
        cornerPath.lineTo(scanRect.right - len, scanRect.bottom)
        
        cornerPath.moveTo(scanRect.left + len, scanRect.bottom)
        cornerPath.lineTo(scanRect.left, scanRect.bottom)
        cornerPath.lineTo(scanRect.left, scanRect.bottom - len)

        canvas.drawPath(cornerPath, cornerPaint)
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }
}
