package com.aatorque.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class GaugeShape { CIRCULAR, LINEAR_HORIZONTAL, LINEAR_VERTICAL, TEXT }

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

    var reverseSweep = false
    var needleStyle = 0     // 0=dot, 1=line, 2=triangle, 3=image
    var backgroundStyle = 0 // 0=arc outline, 1=none, 2=filled circle

    var needleDrawable: Drawable? = null
    var dialBackground: Drawable? = null

    var bgColor = Color.DKGRAY
    var accentColor = Color.CYAN
    var needleColor = Color.RED
    var redlineColor = Color.RED

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val bgFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val needleLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val redlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val textValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
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
        bgFillPaint.color = Color.argb(
            (Color.alpha(bgColor) * 0.3f).toInt(),
            Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor)
        )
        accentPaint.color = accentColor
        needlePaint.color = needleColor
        needleLinePaint.color = needleColor
        redlinePaint.color = redlineColor

        accentPaint.setShadowLayer(15f, 0f, 0f, accentColor)
        redlinePaint.setShadowLayer(15f, 0f, 0f, redlineColor)
        needlePaint.setShadowLayer(10f, 0f, 0f, needleColor)
        needleLinePaint.setShadowLayer(8f, 0f, 0f, needleColor)
        textValuePaint.color = accentColor
        textValuePaint.setShadowLayer(12f, 0f, 0f, accentColor)

        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val strokeW = Math.min(w, h) * 0.08f
        bgPaint.strokeWidth = strokeW
        accentPaint.strokeWidth = strokeW
        redlinePaint.strokeWidth = strokeW
        needleLinePaint.strokeWidth = strokeW * 0.5f

        val padding = strokeW + 15f
        rectF.set(padding, padding, w - padding, h - padding)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        when (shape) {
            GaugeShape.CIRCULAR -> drawCircular(canvas)
            GaugeShape.LINEAR_HORIZONTAL -> drawLinearHorizontal(canvas)
            GaugeShape.LINEAR_VERTICAL -> drawLinearVertical(canvas)
            GaugeShape.TEXT -> drawText(canvas)
        }
    }

    private fun drawText(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val textSize = min(width, height) * 0.40f
        textValuePaint.textSize = textSize
        val valueStr = if (isPreviewMode) "65" else {
            val v = currentVal
            if (v == v.toLong().toFloat()) v.toLong().toString() else "%.1f".format(v)
        }
        val fm = textValuePaint.fontMetrics
        canvas.drawText(valueStr, cx, cy - (fm.ascent + fm.descent) / 2f, textValuePaint)
    }

    private fun drawCircular(canvas: Canvas) {
        val baseStart = 135f
        val totalSweep = 270f

        val range = maxVal - minVal
        if (range <= 0) return

        var valuePercentage = (currentVal - minVal) / range
        if (isPreviewMode) valuePercentage = 0.65f

        val activeSweep = totalSweep * valuePercentage

        // Dial background image (drawn behind everything)
        dialBackground?.let { bg ->
            bg.setBounds(0, 0, width, height)
            bg.draw(canvas)
        }

        // Background fill (style 2)
        if (backgroundStyle == 2) {
            canvas.drawOval(rectF, bgFillPaint)
        }

        // Background arc (style 0)
        if (backgroundStyle != 1) {
            if (reverseSweep) {
                canvas.drawArc(rectF, baseStart + totalSweep, -totalSweep, false, bgPaint)
            } else {
                canvas.drawArc(rectF, baseStart, totalSweep, false, bgPaint)
            }
        }

        val redlinePercentage = (redlineThreshold - minVal) / range

        // backgroundStyle 1 = None: skip all arc drawing, show only needle/image
        if (backgroundStyle != 1) {
            if (reverseSweep) {
                val arcStart = baseStart + totalSweep
                if (valuePercentage > redlinePercentage && redlinePercentage in 0f..1f) {
                    val redlineSweep = totalSweep * (valuePercentage - redlinePercentage)
                    canvas.drawArc(rectF, arcStart, -redlineSweep, false, redlinePaint)
                    canvas.drawArc(rectF, arcStart - redlineSweep, -(activeSweep - redlineSweep), false, accentPaint)
                } else {
                    val sweep = if (activeSweep < 1f && !isPreviewMode) 1f else activeSweep
                    canvas.drawArc(rectF, arcStart, -sweep, false, accentPaint)
                }
            } else {
                if (valuePercentage > redlinePercentage && redlinePercentage in 0f..1f) {
                    val normalSweep = totalSweep * redlinePercentage
                    val redlineSweep = totalSweep * (valuePercentage - redlinePercentage)
                    canvas.drawArc(rectF, baseStart, normalSweep, false, accentPaint)
                    canvas.drawArc(rectF, baseStart + normalSweep, redlineSweep, false, redlinePaint)
                } else {
                    val sweep = if (activeSweep < 1f && !isPreviewMode) 1f else activeSweep
                    canvas.drawArc(rectF, baseStart, sweep, false, accentPaint)
                }
            }
        }

        // Needle position
        val needleAngleDeg = if (reverseSweep) baseStart + totalSweep - activeSweep else baseStart + activeSweep
        val angle = Math.toRadians(needleAngleDeg.toDouble())
        val cx = width / 2f
        val cy = height / 2f
        val radius = rectF.width() / 2f - bgPaint.strokeWidth

        val nx = cx + cos(angle).toFloat() * radius
        val ny = cy + sin(angle).toFloat() * radius

        when (needleStyle) {
            1 -> { // line from center
                needleLinePaint.style = Paint.Style.STROKE
                canvas.drawLine(cx, cy, nx, ny, needleLinePaint)
            }
            2 -> { // triangle pointer
                val tipX = cx + cos(angle).toFloat() * (radius + bgPaint.strokeWidth * 0.5f)
                val tipY = cy + sin(angle).toFloat() * (radius + bgPaint.strokeWidth * 0.5f)
                val perpAngle = angle + Math.PI / 2
                val baseHalf = bgPaint.strokeWidth * 0.5f
                val b1x = nx + cos(perpAngle).toFloat() * baseHalf
                val b1y = ny + sin(perpAngle).toFloat() * baseHalf
                val b2x = nx - cos(perpAngle).toFloat() * baseHalf
                val b2y = ny - sin(perpAngle).toFloat() * baseHalf
                val path = Path()
                path.moveTo(tipX, tipY)
                path.lineTo(b1x, b1y)
                path.lineTo(b2x, b2y)
                path.close()
                canvas.drawPath(path, needlePaint)
            }
            3 -> { // image needle — rotate drawable to match arc position
                needleDrawable?.let { drawable ->
                    canvas.save()
                    // +90f matches Speedometer.drawIndicator()'s rotate(90f + degree) convention
                    canvas.rotate(needleAngleDeg + 90f, cx, cy)
                    drawable.setBounds(0, 0, width, height)
                    drawable.draw(canvas)
                    canvas.restore()
                }
            }
            else -> { // dot (default)
                canvas.drawCircle(nx, ny, bgPaint.strokeWidth * 0.6f, needlePaint)
            }
        }
    }

    private fun drawLinearHorizontal(canvas: Canvas) {
        val w = width.toFloat() - 30f
        val cy = height / 2f

        if (backgroundStyle == 2) {
            bgFillPaint.style = Paint.Style.FILL
            canvas.drawRect(15f, cy - bgPaint.strokeWidth, w + 15f, cy + bgPaint.strokeWidth, bgFillPaint)
        }
        if (backgroundStyle != 1) {
            canvas.drawLine(15f, cy, w + 15f, cy, bgPaint)
        }

        val range = maxVal - minVal
        if (range <= 0) return
        var valuePercentage = (currentVal - minVal) / range
        if (isPreviewMode) valuePercentage = 0.65f

        val endX = if (reverseSweep) w + 15f - w * valuePercentage else 15f + w * valuePercentage
        val startX = if (reverseSweep) w + 15f else 15f
        canvas.drawLine(startX, cy, endX, cy, accentPaint)

        when (needleStyle) {
            1 -> canvas.drawLine(endX, cy - bgPaint.strokeWidth, endX, cy + bgPaint.strokeWidth, needleLinePaint)
            2 -> {
                val path = Path()
                val dir = if (reverseSweep) -1f else 1f
                path.moveTo(endX + dir * bgPaint.strokeWidth * 0.8f, cy)
                path.lineTo(endX - dir * bgPaint.strokeWidth * 0.3f, cy - bgPaint.strokeWidth * 0.6f)
                path.lineTo(endX - dir * bgPaint.strokeWidth * 0.3f, cy + bgPaint.strokeWidth * 0.6f)
                path.close()
                canvas.drawPath(path, needlePaint)
            }
            else -> canvas.drawCircle(endX, cy, bgPaint.strokeWidth * 0.8f, needlePaint)
        }
    }

    private fun drawLinearVertical(canvas: Canvas) {
        val h = height.toFloat() - 30f
        val cx = width / 2f

        if (backgroundStyle == 2) {
            bgFillPaint.style = Paint.Style.FILL
            canvas.drawRect(cx - bgPaint.strokeWidth, 15f, cx + bgPaint.strokeWidth, h + 15f, bgFillPaint)
        }
        if (backgroundStyle != 1) {
            canvas.drawLine(cx, h + 15f, cx, 15f, bgPaint)
        }

        val range = maxVal - minVal
        if (range <= 0) return
        var valuePercentage = (currentVal - minVal) / range
        if (isPreviewMode) valuePercentage = 0.65f

        val endY = if (reverseSweep) 15f + h * (1f - valuePercentage) else h + 15f - h * valuePercentage
        val startY = if (reverseSweep) 15f else h + 15f
        canvas.drawLine(cx, startY, cx, endY, accentPaint)

        when (needleStyle) {
            1 -> canvas.drawLine(cx - bgPaint.strokeWidth, endY, cx + bgPaint.strokeWidth, endY, needleLinePaint)
            2 -> {
                val path = Path()
                val dir = if (reverseSweep) 1f else -1f
                path.moveTo(cx, endY + dir * bgPaint.strokeWidth * 0.8f)
                path.lineTo(cx - bgPaint.strokeWidth * 0.6f, endY - dir * bgPaint.strokeWidth * 0.3f)
                path.lineTo(cx + bgPaint.strokeWidth * 0.6f, endY - dir * bgPaint.strokeWidth * 0.3f)
                path.close()
                canvas.drawPath(path, needlePaint)
            }
            else -> canvas.drawCircle(cx, endY, bgPaint.strokeWidth * 0.8f, needlePaint)
        }
    }
}
