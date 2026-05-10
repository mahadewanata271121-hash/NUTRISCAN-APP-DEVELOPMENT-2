package com.example.nutriscan

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

class MountainChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var dataPoints: List<Float> = emptyList()
    private var labels: List<String> = emptyList()
    private val path = Path()
    private val fillPath = Path()
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 6f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY; strokeWidth = 2f; style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f); alpha = 90
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GRAY; textSize = 24f }
    private val boundaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 3f; style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f) }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.FILL }
    private val connectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; strokeWidth = 3f; style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f) }

    private val popupRectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val popupTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 28f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
    private val popupLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 3f; style = Paint.Style.STROKE }

    private var chartColor: Int = Color.BLUE
    private var animationProgress = 0f
    private var popupAlpha = 0f
    private var minGoal: Float = 0f
    private var targetGoal: Float = 0f
    private var maxGoal: Float = 0f

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setData(data: List<Float>, labels: List<String>, color: Int, min: Float = 0f, target: Float = 0f, max: Float = 0f) {
        this.dataPoints = data
        this.labels = labels
        this.chartColor = color
        this.minGoal = min
        this.targetGoal = target
        this.maxGoal = max
        paint.color = color
        
        animationProgress = 0f
        popupAlpha = 0f
        invalidate()
        startAnimation()
    }

    private fun startAnimation() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200; interpolator = DecelerateInterpolator()
            addUpdateListener { animationProgress = it.animatedValue as Float; invalidate() }
            start()
        }
        
        Handler(Looper.getMainLooper()).postDelayed({
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 400
                addUpdateListener { popupAlpha = it.animatedValue as Float; invalidate() }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        Handler(Looper.getMainLooper()).postDelayed({ fadeOutPopup() }, 3500)
                    }
                })
                start()
            }
        }, 1000)
    }

    private fun fadeOutPopup() {
        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 500; addUpdateListener { popupAlpha = it.animatedValue as Float; invalidate() }; start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        val pL = 280f; val pR = 60f; val pT = 60f; val pB = 100f
        val w = width.toFloat() - pL - pR
        val h = height.toFloat() - pT - pB
        if (w <= 0 || h <= 0) return

        val maxValueInData = dataPoints.filter { it >= 0 }.maxOrNull() ?: 1f
        val maxScale = maxOf(maxValueInData, maxGoal, 1f) * 1.3f
        
        drawGridAndBoundaries(canvas, pL, w, h, pT, maxScale)
        
        val stepX = if (dataPoints.size > 1) w / (dataPoints.size - 1) else 0f
        val scaleY = h / maxScale

        drawLabels(canvas, pL, stepX, h, pT)

        path.reset(); fillPath.reset()
        var lastValidIndex = -1
        for (i in dataPoints.indices) { if (dataPoints[i] >= 0) lastValidIndex = i }
        if (lastValidIndex == -1) return

        var latestX = 0f; var latestY = 0f; var latestVal = 0f
        
        // Start from origin (0 height at the first label position)
        path.moveTo(pL, pT + h)
        fillPath.moveTo(pL, pT + h)

        for (i in 0..lastValidIndex) {
            val x = pL + (i * stepX)
            val currentVal = dataPoints[i] * animationProgress
            val y = pT + h - (currentVal * scaleY)

            val prevX = if (i == 0) pL else pL + ((i - 1) * stepX)
            // If i=0, prevVal is 0 (origin). If i>0, use previous data point.
            val prevVal = (if (i == 0) 0f else dataPoints[i - 1]) * animationProgress
            val prevY = pT + h - (prevVal * scaleY)
            
            val dx = x - prevX
            path.cubicTo(prevX + dx/2f, prevY, prevX + dx/2f, y, x, y)
            fillPath.cubicTo(prevX + dx/2f, prevY, prevX + dx/2f, y, x, y)
            
            if (i == lastValidIndex) {
                latestX = x; latestY = y; latestVal = dataPoints[i]
            }
        }

        if (lastValidIndex >= 0) {
            fillPath.lineTo(latestX, pT + h); fillPath.close()
            fillPaint.shader = LinearGradient(0f, pT, 0f, pT + h, chartColor, Color.TRANSPARENT, Shader.TileMode.CLAMP)
            fillPaint.alpha = (80 * animationProgress).toInt()
            canvas.drawPath(fillPath, fillPaint)
            canvas.drawPath(path, paint)
            
            // Marker
            canvas.drawLine(latestX, pT + h, latestX, latestY, connectorPaint)
            canvas.drawCircle(latestX, latestY, 12f, dotPaint)
            
            // Dashed continuation
            if (lastValidIndex < dataPoints.size - 1) {
                val endX = pL + ((dataPoints.size - 1) * stepX)
                canvas.drawLine(latestX, pT + h, endX, pT + h, gridPaint)
            }
        }

        if (popupAlpha > 0f && latestVal >= 0) drawStatusPopup(canvas, latestX, latestY, latestVal)
    }

    private fun drawStatusPopup(canvas: Canvas, x: Float, y: Float, value: Float) {
        val (text, color) = when {
            value > maxGoal -> "Danger" to Color.parseColor("#FF5252")
            value < minGoal -> "Keep Up" to Color.parseColor("#1A9BEF")
            else -> "OK" to Color.parseColor("#4DBB8E")
        }
        val alphaInt = (popupAlpha * 255).toInt()
        popupRectPaint.color = color; popupRectPaint.alpha = alphaInt
        popupTextPaint.alpha = alphaInt
        popupLinePaint.color = color; popupLinePaint.alpha = alphaInt
        
        val tW = popupTextPaint.measureText(text)
        val rect = RectF(x - (tW/2 + 25f), y - 140f, x + (tW/2 + 25f), y - 85f)
        canvas.drawRoundRect(rect, 15f, 15f, popupRectPaint)
        canvas.drawText(text, x, y - 100f, popupTextPaint)
        
        canvas.drawLine(x, y - 85f, x, y - 15f, popupLinePaint)
    }

    private fun drawGridAndBoundaries(canvas: Canvas, pL: Float, width: Float, height: Float, pT: Float, maxScale: Float) {
        val scaleY = height / maxScale
        val unit = if (targetGoal > 500f) "kkal" else "g"
        
        val step = if (maxScale > 2000) 500f else if (maxScale > 1000) 250f else 100f
        var gV = 0f
        while (gV <= maxScale) {
            val y = pT + height - (gV * scaleY)
            canvas.drawLine(pL, y, pL + width, y, gridPaint)
            textPaint.apply { color = Color.parseColor("#CCCCCC"); textAlign = Paint.Align.RIGHT; typeface = Typeface.DEFAULT }
            canvas.drawText(gV.toInt().toString(), pL - 20f, y + 8f, textPaint)
            gV += step
        }

        fun drawBoundary(v: Float, label: String, color: Int) {
            if (v <= 0) return
            val y = pT + height - (v * scaleY)
            boundaryPaint.color = color
            canvas.drawLine(pL, y, pL + width, y, boundaryPaint)
            textPaint.apply { this.color = color; textAlign = Paint.Align.RIGHT; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            canvas.drawText("$label (${v.toInt()}$unit)", pL - 20f, y + 8f, textPaint)
        }

        drawBoundary(maxGoal, "Max", Color.parseColor("#FF5252"))
        drawBoundary(targetGoal, "Goal", Color.parseColor("#2D2F31"))
        drawBoundary(minGoal, "Min", Color.parseColor("#4DBB8E"))
        
        textPaint.apply { color = Color.parseColor("#AAAAAA"); textAlign = Paint.Align.RIGHT; typeface = Typeface.DEFAULT }
        canvas.drawText("0 (0$unit)", pL - 20f, pT + height + 8f, textPaint)
    }

    private fun drawLabels(canvas: Canvas, startX: Float, stepX: Float, h: Float, pT: Float) {
        textPaint.apply { color = Color.GRAY; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT }
        for (i in labels.indices) {
            canvas.drawText(labels[i], startX + (i * stepX), pT + h + 60f, textPaint)
        }
    }
}
