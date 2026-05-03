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
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        alpha = 110
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textSize = 24f
    }

    private val boundaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 3f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    
    private val connectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 4f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
    }

    private val popupRectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }
    private val popupTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val popupLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private var chartColor: Int = Color.BLUE
    private var animationProgress = 0f
    private var popupAlpha = 0f

    private var minGoal: Float = 0f
    private var targetGoal: Float = 0f
    private var maxGoal: Float = 0f

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        if (isInEditMode) {
            animationProgress = 1f
            popupAlpha = 1f
            dataPoints = listOf(30f, 45f, 20f, 80f, 60f, 40f, 70f)
            labels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
            chartColor = Color.parseColor("#FF5A16")
            minGoal = 50f
            targetGoal = 65f
            maxGoal = 80f
            paint.color = chartColor
        }
    }

    fun setData(data: List<Float>, labels: List<String>, color: Int, min: Float = 0f, target: Float = 0f, max: Float = 0f) {
        this.dataPoints = data
        this.labels = labels
        this.chartColor = color
        this.minGoal = min
        this.targetGoal = target
        this.maxGoal = max
        paint.color = color
        
        if (!isInEditMode) {
            animationProgress = 0f
            startAnimation()
        } else {
            animationProgress = 1f
            invalidate()
        }
    }

    private fun startAnimation() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                animationProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            addUpdateListener {
                popupAlpha = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    try {
                        Looper.myLooper()?.let {
                            Handler(it).postDelayed({ fadeOutPopup() }, 2000)
                        }
                    } catch (e: Exception) {}
                }
            })
            start()
        }
    }

    private fun fadeOutPopup() {
        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 500
            addUpdateListener {
                popupAlpha = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        val paddingLeft = 200f 
        val paddingRight = 50f
        val paddingTop = 60f
        val paddingBottom = 80f
        
        val w = width.toFloat() - paddingLeft - paddingRight
        val h = height.toFloat() - paddingTop - paddingBottom
        
        if (w <= 0 || h <= 0) return

        val maxValueInData = dataPoints.filter { it >= 0 }.maxOrNull() ?: 1f
        val maxScale = maxOf(maxValueInData, maxGoal, 1f) * 1.2f
        
        drawBoundaries(canvas, paddingLeft, w, h, paddingTop, maxScale)
        
        val startXOffset = 40f 
        val effectiveWidth = w - startXOffset
        val stepX = if (dataPoints.size > 1) effectiveWidth / (dataPoints.size - 1) else 0f
        val scaleY = h / maxScale

        path.reset()
        fillPath.reset()

        var lastValidIndex = -1
        for (i in dataPoints.indices) {
            if (dataPoints[i] >= 0) lastValidIndex = i
        }

        drawLabels(canvas, paddingLeft + startXOffset, stepX, h, paddingTop)

        if (lastValidIndex == -1) return

        var latestX = 0f
        var latestY = 0f
        var latestVal = 0f

        for (i in 0..lastValidIndex) {
            val x = paddingLeft + startXOffset + (i * stepX)
            val value = dataPoints[i]
            val currentVal = value * animationProgress
            val y = paddingTop + h - (currentVal * scaleY)

            val prevX = if (i == 0) paddingLeft else paddingLeft + startXOffset + ((i - 1) * stepX)
            val prevVal = if (i == 0) 0f else dataPoints[i - 1] * animationProgress
            val prevY = paddingTop + h - (prevVal * scaleY)

            if (i == 0) {
                path.moveTo(prevX, prevY)
                fillPath.moveTo(prevX, prevY)
            }

            val dx = x - prevX
            path.cubicTo(prevX + (dx / 2f), prevY, prevX + (dx / 2f), y, x, y)
            fillPath.cubicTo(prevX + (dx / 2f), prevY, prevX + (dx / 2f), y, x, y)
            
            if (i == lastValidIndex) {
                latestX = x
                latestY = y
                latestVal = value
                
                canvas.drawLine(x, paddingTop + h, x, y, connectorPaint)
                dotPaint.alpha = (animationProgress * 255).toInt().coerceIn(0, 255)
                canvas.drawCircle(x, y, 10f, dotPaint)
            }
        }

        if (lastValidIndex >= 0) {
            val lastX = paddingLeft + startXOffset + (lastValidIndex * stepX)
            fillPath.lineTo(lastX, paddingTop + h)
            fillPath.close()

            val gradient = LinearGradient(0f, paddingTop, 0f, paddingTop + h,
                chartColor, Color.TRANSPARENT, Shader.TileMode.CLAMP)
            fillPaint.shader = gradient
            fillPaint.alpha = (70 * animationProgress).toInt().coerceIn(0, 255)

            canvas.drawPath(fillPath, fillPaint)
            canvas.drawPath(path, paint)
        }

        if (popupAlpha > 0f && latestVal >= 0) {
            drawStatusPopup(canvas, latestX, latestY, latestVal)
        }
    }

    private fun drawStatusPopup(canvas: Canvas, x: Float, y: Float, value: Float) {
        val statusText: String
        val popupColor: Int
        
        when {
            value > maxGoal -> { statusText = "Danger"; popupColor = Color.parseColor("#FF5252") }
            value < minGoal -> { statusText = "Keepup"; popupColor = Color.parseColor("#1A9BEF") }
            else -> { statusText = "Ok"; popupColor = Color.parseColor("#4DBB8E") }
        }

        val alphaInt = (popupAlpha * 255).toInt().coerceIn(0, 255)
        popupRectPaint.color = popupColor
        popupRectPaint.alpha = alphaInt
        popupTextPaint.alpha = alphaInt
        popupLinePaint.color = popupColor
        popupLinePaint.alpha = alphaInt

        val textWidth = popupTextPaint.measureText(statusText)
        val rectPadding = 20f
        val rectHeight = 50f
        val rectWidth = textWidth + (rectPadding * 2)
        
        val popupY = y - 80f
        val rect = RectF(x - rectWidth/2, popupY - rectHeight, x + rectWidth/2, popupY)
        canvas.drawRoundRect(rect, 12f, 12f, popupRectPaint)
        canvas.drawText(statusText, x, popupY - 15f, popupTextPaint)
        canvas.drawLine(x, popupY, x, y - 10f, popupLinePaint)
    }

    private fun drawBoundaries(canvas: Canvas, paddingLeft: Float, width: Float, height: Float, paddingTop: Float, maxScale: Float) {
        if (maxScale <= 0) return
        val scaleY = height / maxScale
        val isCalorie = targetGoal > 500f
        val unit = if (isCalorie) "kkal" else "g"
        
        fun drawLine(value: Float, color: Int, labelPrefix: String, isBold: Boolean = false, isGrid: Boolean = false) {
            val y = paddingTop + height - (value * scaleY)
            if (isGrid) {
                gridPaint.color = color
                canvas.drawLine(paddingLeft, y, paddingLeft + width, y, gridPaint)
            } else {
                boundaryPaint.color = color
                canvas.drawLine(paddingLeft, y, paddingLeft + width, y, boundaryPaint)
            }
            
            textPaint.color = if (isGrid) Color.LTGRAY else color
            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            val label = if (labelPrefix.isEmpty()) "${value.toInt()}" else "$labelPrefix (${value.toInt()}$unit)"
            canvas.drawText(label, paddingLeft - 20f, y + 10f, textPaint)
        }

        drawLine(0f, Color.GRAY, "0")
        if (minGoal > 0) drawLine(minGoal, Color.parseColor("#4DBB8E"), "Min", true)
        if (targetGoal > 0) drawLine(targetGoal, Color.parseColor("#2D2F31"), "Goal", true)
        if (maxGoal > 0) drawLine(maxGoal, Color.parseColor("#FF5252"), "Max", true)
    }

    private fun drawLabels(canvas: Canvas, startX: Float, stepX: Float, height: Float, paddingTop: Float) {
        if (labels.isEmpty()) return
        textPaint.color = Color.GRAY
        textPaint.textAlign = Paint.Align.CENTER
        for (i in labels.indices) {
            val x = startX + (i * stepX)
            canvas.drawText(labels[i], x, paddingTop + height + 50f, textPaint)
        }
    }
}
