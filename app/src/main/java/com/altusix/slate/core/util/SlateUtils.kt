package com.altusix.slate.utils

import android.content.Context
import android.graphics.*
import androidx.core.content.res.ResourcesCompat
import com.altusix.slate.R
import com.altusix.slate.data.local.SlateWidgetConfig

fun getSafeBgColor(config: SlateWidgetConfig): Int {
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val rawHex = config.backgroundColorHex.toInt()
    val r = (rawHex shr 16) and 0xFF
    val g = (rawHex shr 8) and 0xFF
    val b = rawHex and 0xFF
    return Color.argb(alphaInt, r, g, b)
}

fun getStandardCornerRadius(multiplier: Float): Float = 14f * multiplier
data class SupersampledCanvas(
    val bitmap: Bitmap,
    val canvas: Canvas,
    val scaleFactor: Float
)
fun createSupersampledCanvas(wDp: Int, hDp: Int, context: Context): SupersampledCanvas {
    val density = context.resources.displayMetrics.density
    val scaleFactor = maxOf(density, 3.5f)
    val w = (wDp * scaleFactor).toInt().coerceAtLeast(1)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    return SupersampledCanvas(bitmap, canvas, scaleFactor)
}

fun getSlateFont(
    context: Context,
    weight: Int = 400,
    isItalic: Boolean = false
): Typeface {
    val fontRes = if (isItalic) {
        R.font.inter_tight_italic_variable
    } else {
        R.font.inter_tight_variable
    }

    return try {
        val baseTypeface = ResourcesCompat.getFont(context, fontRes) ?: Typeface.DEFAULT
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            Typeface.create(baseTypeface, weight, isItalic)
        } else {
            val style = when {
                weight >= 700 && isItalic -> Typeface.BOLD_ITALIC
                weight >= 700 -> Typeface.BOLD
                isItalic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
            Typeface.create(baseTypeface, style)
        }
    } catch (_: Exception) {
        Typeface.create(Typeface.SANS_SERIF, if (weight >= 700) Typeface.BOLD else Typeface.NORMAL)
    }
}

/**
 * Standardized configuration placeholder state used across all Slate widgets.
 * Renders a responsive, mathematically centered gear icon and auto-wrapping "Tap to Configure" text stack.
 */
fun drawConfigurePlaceholderState(
    canvas: Canvas,
    context: Context,
    cardRect: RectF,
    config: SlateWidgetConfig,
    scaleFactor: Float
) {
    val cx = cardRect.centerX()
    val cy = cardRect.centerY()

    val isLight = config.themeMode == "LIGHT"
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#60FFFFFF")
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE

    val cardW = cardRect.width()
    val cardH = cardRect.height()
    val minDim = minOf(cardW, cardH)
    val aspectRatio = cardW / cardH
    val isNarrow = aspectRatio < 0.70f

    // 1. SUBTLE BACKGROUND GEAR WATERMARK (Centered at cx, cy using minDim)
    val gearRadius = (minDim * 0.32f).coerceIn(scaleFactor * 22f, scaleFactor * 56f)
    val gearInnerR = gearRadius * 0.75f
    val holeR = gearRadius * 0.32f

    val gearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(24, Color.red(secondaryText), Color.green(secondaryText), Color.blue(secondaryText))
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 2.5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    val gearPath = Path()
    val teeth = 8
    val anglePerTooth = (2 * Math.PI / teeth).toFloat()
    val toothWidth = anglePerTooth * 0.28f

    for (i in 0 until teeth) {
        val angle = i * anglePerTooth
        val a1 = angle - toothWidth
        val a2 = angle - toothWidth * 0.5f
        val a3 = angle + toothWidth * 0.5f
        val a4 = angle + toothWidth

        val x1 = cx + gearInnerR * Math.cos(a1.toDouble()).toFloat()
        val y1 = cy + gearInnerR * Math.sin(a1.toDouble()).toFloat()
        val x2 = cx + gearRadius * Math.cos(a2.toDouble()).toFloat()
        val y2 = cy + gearRadius * Math.sin(a2.toDouble()).toFloat()
        val x3 = cx + gearRadius * Math.cos(a3.toDouble()).toFloat()
        val y3 = cy + gearRadius * Math.sin(a3.toDouble()).toFloat()
        val x4 = cx + gearInnerR * Math.cos(a4.toDouble()).toFloat()
        val y4 = cy + gearInnerR * Math.sin(a4.toDouble()).toFloat()

        if (i == 0) gearPath.moveTo(x1, y1) else gearPath.lineTo(x1, y1)
        gearPath.lineTo(x2, y2)
        gearPath.lineTo(x3, y3)
        gearPath.lineTo(x4, y4)
    }
    gearPath.close()

    canvas.drawPath(gearPath, gearPaint)
    canvas.drawCircle(cx, cy, holeR, gearPaint)

    // 2. FOREGROUND OVERLAY TEXT (Centered directly over cx, cy)
    val availableW = cardW * 0.80f
    val singleLineText = "Tap to Configure"

    val testPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 600)
    }

    var targetTextSize = (minDim * 0.16f).coerceIn(scaleFactor * 11f, scaleFactor * 22f)
    testPaint.textSize = targetTextSize

    val lines = if (isNarrow || testPaint.measureText(singleLineText) > availableW) {
        targetTextSize = (minDim * 0.18f).coerceIn(scaleFactor * 12f, scaleFactor * 24f)
        listOf("Tap to", "Configure")
    } else {
        listOf(singleLineText)
    }

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = targetTextSize
        typeface = getSlateFont(context, weight = 600)
        textAlign = Paint.Align.CENTER
    }

    // Auto-scale down if text exceeds bounds
    while (lines.any { textPaint.measureText(it) > availableW } && textPaint.textSize > scaleFactor * 9f) {
        textPaint.textSize -= scaleFactor * 0.8f
    }

    // 3. Perfect Vertical Centering around cy
    val fontMetrics = textPaint.fontMetrics
    val lineHeight = (fontMetrics.bottom - fontMetrics.top) * 0.90f
    val totalTextHeight = lines.size * lineHeight

    var currentTextY = cy - (totalTextHeight / 2f) - fontMetrics.top
    for (line in lines) {
        canvas.drawText(line, cx, currentTextY, textPaint)
        currentTextY += lineHeight
    }
}