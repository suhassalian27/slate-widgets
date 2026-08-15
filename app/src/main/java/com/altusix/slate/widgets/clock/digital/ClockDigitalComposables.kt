package com.altusix.slate.widgets.clock.digital

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.altusix.slate.data.local.SlateWidgetConfig

// --- SHARED GEOMETRY & THEMING HELPERS ---

private fun getSafeBgColor(config: SlateWidgetConfig): Int {
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val rawHex = config.backgroundColorHex.toInt()
    val r = (rawHex shr 16) and 0xFF
    val g = (rawHex shr 8) and 0xFF
    val b = rawHex and 0xFF
    return Color.argb(alphaInt, r, g, b)
}

private fun getStandardCornerRadius(density: Float): Float = 22f * density

/**
 * Dynamic Accent Contrast luminance evaluation
 */
fun getContrastColor(colorInt: Int): Int {
    val r = Color.red(colorInt)
    val g = Color.green(colorInt)
    val b = Color.blue(colorInt)
    val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    return if (luminance > 0.5) Color.parseColor("#121214") else Color.WHITE
}

/**
 * Auto-fits text width inside available bounds without clipping
 */
fun drawAutoFitText(
    canvas: Canvas,
    text: String,
    cx: Float,
    cy: Float,
    maxAllowedWidth: Float,
    basePaint: Paint
) {
    val measuredWidth = basePaint.measureText(text)
    if (measuredWidth > maxAllowedWidth && maxAllowedWidth > 0f) {
        val originalSize = basePaint.textSize
        basePaint.textSize = originalSize * (maxAllowedWidth / measuredWidth)
        canvas.drawText(text, cx, cy, basePaint)
        basePaint.textSize = originalSize
    } else {
        canvas.drawText(text, cx, cy, basePaint)
    }
}

// ============================================================================
// WIDGET BITMAP GENERATORS
// ============================================================================

// 1. BOLD TYPOGRAPHIC DIGITAL (2x2 Square / Stacked Giant Hour & Minute Display)
fun generateBoldTypographicDigitalClockBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast((100 * density).toInt())
    val h = (hDp * density).toInt().coerceAtLeast((100 * density).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    // 1. Card Layout & Corner Radius
    val size = minOf(w, h).toFloat()
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    val cornerRadius = getStandardCornerRadius(density)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val clockCx = cardRect.centerX()
    val timeState = DigitalClockTimeState.now()

    // 2. Stacked Typography (Hour & Minute)
    val timeFont = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        typeface = timeFont
        textSize = size * 0.38f
        textAlign = Paint.Align.CENTER
    }

    val hourY = topY + size * 0.36f
    drawAutoFitText(canvas, timeState.hour12, clockCx, hourY, size * 0.82f, hourPaint)

    val minPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        typeface = timeFont
        textSize = size * 0.38f
        textAlign = Paint.Align.CENTER
    }
    val minY = topY + size * 0.70f
    drawAutoFitText(canvas, timeState.minute, clockCx, minY, size * 0.82f, minPaint)

    // 3. Modern Translucent Date Capsule
    val capsuleW = size * 0.78f
    val capsuleH = size * 0.13f
    val capsuleY = topY + size * 0.79f
    val capsuleRect = RectF(clockCx - (capsuleW / 2f), capsuleY, clockCx + (capsuleW / 2f), capsuleY + capsuleH)

    // Soft translucent fill tint (8% black in Light mode / 10% white in Dark mode)
    val capsuleBgColor = if (isLight) Color.argb(20, 0, 0, 0) else Color.argb(26, 255, 255, 255)
    val capsulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = capsuleBgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(capsuleRect, capsuleH / 2f, capsuleH / 2f, capsulePaint)

    // Subtly outline the capsule border for crisp definition
    val capsuleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(15, 0, 0, 0) else Color.argb(20, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = size * 0.006f
    }
    canvas.drawRoundRect(capsuleRect, capsuleH / 2f, capsuleH / 2f, capsuleStrokePaint)

    // Date & AM/PM Label
    val dateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = capsuleH * 0.48f
        textAlign = Paint.Align.CENTER
    }

    val dateLabel = "${timeState.dayOfWeek}, ${timeState.monthName} ${timeState.dayOfMonth}  •  ${timeState.amPm}"
    val textY = capsuleRect.centerY() - ((dateTextPaint.descent() + dateTextPaint.ascent()) / 2f)
    drawAutoFitText(canvas, dateLabel, clockCx, textY, capsuleW * 0.88f, dateTextPaint)

    return bitmap
}
