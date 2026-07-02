package com.aatorque.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

enum class GaugeShape { CIRCULAR, LINEAR_HORIZONTAL, LINEAR_VERTICAL }

class ModernTorqueGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var shape = GaugeShape.CIRCULAR
    var minVal = 0f
    var maxVal = 100f
    var currentVal = 0f
    var redlineThreshold = 80f
    var isPreviewMode = false

    var bgColor = Color.DKGRAY
    var accentColor = Color.CYAN
    var needleColor = Color.RED
    var redlineColor = Color.RED

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val redlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val rectF = RectF()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // Required for setShadowLayer
    }

    fun updateValue(value: Float) {
        currentVal = value.coerceIn(minVal, maxVal)
        invalidate()
    }

    fun setColors(bg: Int, accent: Int, needle: Int, redline: Int) {
        bgColor = bg
        accentColor = accent
        needleColor = needle
        redlineColor = redline
        
        bgPaint.color = bgColor
        accentPaint.color = accentColor
        needlePaint.color = needleColor
        redlinePaint.color = redlineColor
        
        // Add glowing neon effects
        accentPaint.setShadowLayer(15f, 0f, 0f, accentColor)
        redlinePaint.setShadowLayer(15f, 0f, 0f, redlineColor)
        needlePaint.setShadowLayer(10f, 0f, 0f, needleColor)
        
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val strokeW = Math.min(w, h) * 0.08f
        bgPaint.strokeWidth = strokeW
        accentPaint.strokeWidth = strokeW
        redlinePaint.strokeWidth = strokeW
        
        val padding = strokeW + 15f // shadow padding
        rectF.set(padding, padding, w - padding, h - padding)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        when (shape) {
            GaugeShape.CIRCULAR -> drawCircular(canvas)
            GaugeShape.LINEAR_HORIZONTAL -> drawLinearHorizontal(canvas)
            GaugeShape.LINEAR_VERTICAL -> drawLinearVertical(canvas)
        }
    }

    private fun drawCircular(canvas: Canvas) {
        val startAngle = 135f
        val sweepAngle = 270f
        
        // Draw background arc
        canvas.drawArc(rectF, startAngle, sweepAngle, false, bgPaint)
        
        val range = maxVal - minVal
        if (range <= 0) return
        
        var valuePercentage = (currentVal - minVal) / range
        if (isPreviewMode) {
            valuePercentage = 0.65f // Show a representative value in preview
        }
        
        val redlinePercentage = (redlineThreshold - minVal) / range
        
        // Draw active segments
        val activeSweep = sweepAngle * valuePercentage
        
        if (valuePercentage > redlinePercentage && redlinePercentage in 0f..1f) {
            val normalSweep = sweepAngle * redlinePercentage
            val redlineSweep = sweepAngle * (valuePercentage - redlinePercentage)
            
            canvas.drawArc(rectF, startAngle, normalSweep, false, accentPaint)
            canvas.drawArc(rectF, startAngle + normalSweep, redlineSweep, false, redlinePaint)
        } else {
            // Even if activeSweep is 0, let's draw a tiny dot so the accent color is visible if not preview mode
            val sweep = if (activeSweep < 1f && !isPreviewMode) 1f else activeSweep
            canvas.drawArc(rectF, startAngle, sweep, false, accentPaint)
        }
        
        // Draw needle indicator
        val angle = Math.toRadians((startAngle + activeSweep).toDouble())
        val cx = width / 2f
        val cy = height / 2f
        val radius = rectF.width() / 2f - bgPaint.strokeWidth
        
        val nx = cx + cos(angle).toFloat() * radius
        val ny = cy + sin(angle).toFloat() * radius
        
        canvas.drawCircle(nx, ny, bgPaint.strokeWidth * 0.6f, needlePaint)
    }

    private fun drawLinearHorizontal(canvas: Canvas) {
        val w = width.toFloat() - 30f
        val cy = height / 2f
        
        canvas.drawLine(15f, cy, w + 15f, cy, bgPaint)
        
        val range = maxVal - minVal
        if (range <= 0) return
        var valuePercentage = (currentVal - minVal) / range
        if (isPreviewMode) valuePercentage = 0.65f
        
        canvas.drawLine(15f, cy, 15f + w * valuePercentage, cy, accentPaint)
        canvas.drawCircle(15f + w * valuePercentage, cy, bgPaint.strokeWidth * 0.8f, needlePaint)
    }

    private fun drawLinearVertical(canvas: Canvas) {
        val h = height.toFloat() - 30f
        val cx = width / 2f
        
        canvas.drawLine(cx, h + 15f, cx, 15f, bgPaint)
        
        val range = maxVal - minVal
        if (range <= 0) return
        var valuePercentage = (currentVal - minVal) / range
        if (isPreviewMode) valuePercentage = 0.65f
        
        canvas.drawLine(cx, h + 15f, cx, h + 15f - (h * valuePercentage), accentPaint)
        canvas.drawCircle(cx, h + 15f - (h * valuePercentage), bgPaint.strokeWidth * 0.8f, needlePaint)
    }
}
