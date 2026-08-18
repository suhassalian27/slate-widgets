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

fun getStandardCornerRadius(density: Float): Float = 22f * density

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
 * Renders a subtle gear icon watermark in the background with centered "Tap to Configure" overlay text.
 */
fun drawConfigurePlaceholderState(
    canvas: Canvas,
    context: Context,
    cardRect: RectF,
    config: SlateWidgetConfig,
    scaleFactor: Float
) {
    val isLight = config.themeMode == "LIGHT"
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE

    val cx = cardRect.centerX()
    val cy = cardRect.centerY()

    // 1. SUBTLE BACKGROUND GEAR WATERMARK (Centered & Low Opacity)
    val gearRadius = minOf(cardRect.width(), cardRect.height()) * 0.28f
    val innerRadius = gearRadius * 0.45f
    val toothDepth = gearRadius * 0.28f

    val gearAlpha = if (isLight) 22 else 18 // Low opacity watermark (~7-8% alpha)
    val gearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(
            gearAlpha,
            if (isLight) 0 else 255,
            if (isLight) 0 else 255,
            if (isLight) 0 else 255
        )
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 5.5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Outer Gear Rim Path (8 Teeth)
    val numTeeth = 8
    val gearPath = Path()
    val angleStep = Math.PI * 2 / numTeeth

    for (i in 0 until numTeeth) {
        val angle = i * angleStep
        val outerAngle1 = angle - (angleStep * 0.18)
        val outerAngle2 = angle + (angleStep * 0.18)
        val innerAngle = angle + (angleStep * 0.5)

        val rOuter = gearRadius + toothDepth
        val rInner = gearRadius

        val x1 = (cx + Math.cos(outerAngle1) * rOuter).toFloat()
        val y1 = (cy + Math.sin(outerAngle1) * rOuter).toFloat()

        val x2 = (cx + Math.cos(outerAngle2) * rOuter).toFloat()
        val y2 = (cy + Math.sin(outerAngle2) * rOuter).toFloat()

        val x3 = (cx + Math.cos(innerAngle) * rInner).toFloat()
        val y3 = (cy + Math.sin(innerAngle) * rInner).toFloat()

        if (i == 0) {
            gearPath.moveTo(x1, y1)
        } else {
            gearPath.lineTo(x1, y1)
        }
        gearPath.lineTo(x2, y2)
        gearPath.lineTo(x3, y3)
    }
    gearPath.close()

    canvas.drawPath(gearPath, gearPaint)
    canvas.drawCircle(cx, cy, innerRadius, gearPaint)

    // 2. PERFECTLY CENTERED OVERLAY TEXT
    val titleText = "Tap to Configure"

    val refTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        textSize = 100f
    }
    val measuredTitleW = refTitlePaint.measureText(titleText).coerceAtLeast(1f)
    val maxTitleW = cardRect.width() * 0.82f
    val maxTitleH = cardRect.height() * 0.10f
    val titleSize = minOf(maxTitleH, 100f * (maxTitleW / measuredTitleW)).coerceAtLeast(16f)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = titleSize
        typeface = getSlateFont(context, weight = 500)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.02f
    }

    // Precise mathematical vertical centering
    val textBaselineY = cy - ((titlePaint.descent() + titlePaint.ascent()) / 2f)

    canvas.drawText(titleText, cx, textBaselineY, titlePaint)
}
