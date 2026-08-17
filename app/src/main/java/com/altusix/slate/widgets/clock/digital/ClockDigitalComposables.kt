package com.altusix.slate.widgets.clock.digital

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.altusix.slate.data.local.SlateWidgetConfig
import androidx.core.content.res.ResourcesCompat
import com.altusix.slate.R

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
fun drawAutoFitText(    canvas: Canvas,    text: String,    cx: Float,    cy: Float,    maxAllowedWidth: Float,    basePaint: Paint) {
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

// --- AUTHENTIC 6-SIDED 7-SEGMENT LCD RENDERER (UNIFORM PARALLEL SEAMS) ---
private fun drawAngled7SegmentDigit(
    canvas: Canvas,
    digitChar: Char,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    activePaint: Paint,
    inactivePaint: Paint
) {
    val states = when (digitChar) {
        '0' -> booleanArrayOf(true, true, true, true, true, true, false)
        '1' -> booleanArrayOf(false, true, true, false, false, false, false)
        '2' -> booleanArrayOf(true, true, false, true, true, false, true)
        '3' -> booleanArrayOf(true, true, true, true, false, false, true)
        '4' -> booleanArrayOf(false, true, true, false, false, true, true)
        '5' -> booleanArrayOf(true, false, true, true, false, true, true)
        '6' -> booleanArrayOf(true, false, true, true, true, true, true)
        '7' -> booleanArrayOf(true, true, true, false, false, false, false)
        '8' -> booleanArrayOf(true, true, true, true, true, true, true)
        '9' -> booleanArrayOf(true, true, true, true, false, true, true)
        else -> booleanArrayOf(false, false, false, false, false, false, false)
    }

    val t = w * 0.16f       // Segment thickness
    val ht = t / 2f          // Half thickness (creates exact 45° miter points)
    val g = t * 0.22f        // Uniform diagonal gap distance between segments
    val midY = y + (h / 2f)

    // Segment A (Top Horizontal Hexagon)
    val pathA = Path().apply {
        moveTo(x + ht + g, y + ht)
        lineTo(x + t + g, y)
        lineTo(x + w - t - g, y)
        lineTo(x + w - ht - g, y + ht)
        lineTo(x + w - t - g, y + t)
        lineTo(x + t + g, y + t)
        close()
    }

    // Segment B (Top Right Vertical Hexagon)
    val pathB = Path().apply {
        moveTo(x + w - ht, y + ht + g)
        lineTo(x + w, y + t + g)
        lineTo(x + w, midY - ht - g)
        lineTo(x + w - ht, midY - g)
        lineTo(x + w - t, midY - ht - g)
        lineTo(x + w - t, y + t + g)
        close()
    }

    // Segment C (Bottom Right Vertical Hexagon)
    val pathC = Path().apply {
        moveTo(x + w - ht, midY + g)
        lineTo(x + w, midY + ht + g)
        lineTo(x + w, y + h - t - g)
        lineTo(x + w - ht, y + h - ht - g)
        lineTo(x + w - t, y + h - t - g)
        lineTo(x + w - t, midY + ht + g)
        close()
    }

    // Segment D (Bottom Horizontal Hexagon)
    val pathD = Path().apply {
        moveTo(x + ht + g, y + h - ht)
        lineTo(x + t + g, y + h - t)
        lineTo(x + w - t - g, y + h - t)
        lineTo(x + w - ht - g, y + h - ht)
        lineTo(x + w - t - g, y + h)
        lineTo(x + t + g, y + h)
        close()
    }

    // Segment E (Bottom Left Vertical Hexagon)
    val pathE = Path().apply {
        moveTo(x + ht, midY + g)
        lineTo(x + t, midY + ht + g)
        lineTo(x + t, y + h - t - g)
        lineTo(x + ht, y + h - ht - g)
        lineTo(x, y + h - t - g)
        lineTo(x, midY + ht + g)
        close()
    }

    // Segment F (Top Left Vertical Hexagon)
    val pathF = Path().apply {
        moveTo(x + ht, y + ht + g)
        lineTo(x + t, y + t + g)
        lineTo(x + t, midY - ht - g)
        lineTo(x + ht, midY - g)
        lineTo(x, midY - ht - g)
        lineTo(x, y + t + g)
        close()
    }

    // Segment G (Middle Horizontal Hexagon)
    val pathG = Path().apply {
        moveTo(x + ht + g, midY)
        lineTo(x + t + g, midY - ht)
        lineTo(x + w - t - g, midY - ht)
        lineTo(x + w - ht - g, midY)
        lineTo(x + w - t - g, midY + ht)
        lineTo(x + t + g, midY + ht)
        close()
    }

    val paths = arrayOf(pathA, pathB, pathC, pathD, pathE, pathF, pathG)

    for (i in 0..6) {
        val paint = if (states[i]) activePaint else inactivePaint
        canvas.drawPath(paths[i], paint)
    }
}

// --- FUZZY TIME WORD CONVERTER ---

private data class WordClockState(
    val topWord: String,
    val midWord: String,
    val bottomWord: String
)

private fun getFuzzyWordTimeState(hour24: Int, minute: Int): WordClockState {
    val hourWords = arrayOf(
        "Twelve", "One", "Two", "Three", "Four", "Five",
        "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve"
    )

    val currentHour12 = if (hour24 % 12 == 0) 12 else hour24 % 12
    val nextHour12 = if ((hour24 + 1) % 12 == 0) 12 else (hour24 + 1) % 12

    return when (minute) {
        in 0..2 -> WordClockState(hourWords[currentHour12], "O'Clock", "")
        in 3..7 -> WordClockState("Five", "past", hourWords[currentHour12])
        in 8..12 -> WordClockState("Ten", "past", hourWords[currentHour12])
        in 13..17 -> WordClockState("Quarter", "to", hourWords[nextHour12]) // Or "past"
        in 18..22 -> WordClockState("Twenty", "past", hourWords[currentHour12])
        in 23..27 -> WordClockState("Twenty Five", "past", hourWords[currentHour12])
        in 28..32 -> WordClockState("Half", "past", hourWords[currentHour12])
        in 33..37 -> WordClockState("Twenty Five", "to", hourWords[nextHour12])
        in 38..42 -> WordClockState("Twenty", "to", hourWords[nextHour12])
        in 43..47 -> WordClockState("Quarter", "to", hourWords[nextHour12])
        in 48..52 -> WordClockState("Ten", "to", hourWords[nextHour12])
        in 53..57 -> WordClockState("Five", "to", hourWords[nextHour12])
        else -> WordClockState(hourWords[nextHour12], "O'Clock", "")
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
    val timeFont = getSlateFont(context, weight = 700, isItalic = false)
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

// 2. MINIMAL DIVIDER DIGITAL (2x2 / Stacked Time with Accent Line Divider)
fun generateMinimalDividerDigitalClockBitmap(
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

    // 1. Card Container & Corner Radius Standard
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

    // 2. Stacked Typography (Hours & Minutes)
    val timeFont = getSlateFont(context, weight = 600, isItalic = false)
    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        typeface = timeFont
        textSize = size * 0.33f
        textAlign = Paint.Align.CENTER
    }

    // Hour (Top)
    val hourY = topY + size * 0.36f
    drawAutoFitText(canvas, timeState.hour12, clockCx, hourY, size * 0.72f, timePaint)

    // 3. Accent Line Divider
    val dividerW = size * 0.34f
    val dividerY = topY + size * 0.44f
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = size * 0.024f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(clockCx - (dividerW / 2f), dividerY, clockCx + (dividerW / 2f), dividerY, dividerPaint)

    // Minute (Bottom)
    val minY = topY + size * 0.73f
    drawAutoFitText(canvas, timeState.minute, clockCx, minY, size * 0.72f, timePaint)

    // 4. Accent AM/PM Label
    val amPmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        typeface = getSlateFont(context, weight = 700, isItalic = false)
        textSize = size * 0.105f
        textAlign = Paint.Align.CENTER
    }
    val amPmY = topY + size * 0.86f
    drawAutoFitText(canvas, timeState.amPm, clockCx, amPmY, size * 0.5f, amPmPaint)

    return bitmap
}

// 3. LCD SEVEN SEGMENT DIGITAL (High-DPI 2x2 / Centered 7-Segment LCD)
fun generateLcdSevenSegmentDigitalClockBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    // 1. High-DPI Render Scaling (3.5x multiplier eliminates blurriness on QHD screens)
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val w = (wDp * scaleFactor).toInt().coerceAtLeast(420)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(420)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#80FFFFFF")

    val size = minOf(w, h).toFloat()
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    val cornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val timeState = DigitalClockTimeState.now()

    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.FILL
    }
    val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(18, 0, 0, 0) else Color.argb(22, 255, 255, 255)
        style = Paint.Style.FILL
    }

    // 2. Balanced Proportions (Ratio ~ 1:1.75 to eliminate squishing)
    val digitW = size * 0.245f
    val digitH = size * 0.42f
    val digitGap = size * 0.035f
    val twoDigitsW = (digitW * 2f) + digitGap

    val columnGap = size * 0.08f
    val dateTextSize = size * 0.082f

    // Mathematical Symmetrical Centering
    val totalContentW = twoDigitsW + columnGap + dateTextSize
    val sidePadding = (size - totalContentW) / 2f

    val startX = leftX + sidePadding
    val rightCenterX = startX + twoDigitsW + columnGap + (dateTextSize / 2f)

    val hourY = topY + size * 0.065f
    val minY = topY + size * 0.515f

    // Hours (2 Digits)
    val hourStr = timeState.hour24.padStart(2, '0')
    drawAngled7SegmentDigit(canvas, hourStr[0], startX, hourY, digitW, digitH, activePaint, inactivePaint)
    drawAngled7SegmentDigit(canvas, hourStr[1], startX + digitW + digitGap, hourY, digitW, digitH, activePaint, inactivePaint)

    // Minutes (2 Digits)
    val minStr = timeState.minute.padStart(2, '0')
    drawAngled7SegmentDigit(canvas, minStr[0], startX, minY, digitW, digitH, activePaint, inactivePaint)
    drawAngled7SegmentDigit(canvas, minStr[1], startX + digitW + digitGap, minY, digitW, digitH, activePaint, inactivePaint)

    // Accent Status Dot
    val dotRadius = size * 0.024f
    val dotY = hourY + (digitH * 0.22f)
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawCircle(rightCenterX, dotY, dotRadius, dotPaint)

    // Rotated Vertical Date Strip
    val dateText = "${timeState.dayOfMonth} ${timeState.monthName}"
    val dayText = "•  ${timeState.dayOfWeek}"

    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = dateTextSize
        textAlign = Paint.Align.LEFT
    }

    val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        typeface = getSlateFont(context, weight = 500, isItalic = false)
        textSize = size * 0.072f
        textAlign = Paint.Align.LEFT
    }

    canvas.save()
    val rotateX = rightCenterX
    val rotateY = minY + digitH - (size * 0.015f)
    canvas.rotate(-90f, rotateX, rotateY)

    canvas.drawText(dateText, rotateX, rotateY + (datePaint.textSize * 0.32f), datePaint)
    val dateWidth = datePaint.measureText(dateText)
    canvas.drawText(dayText, rotateX + dateWidth + (size * 0.028f), rotateY + (dayPaint.textSize * 0.32f), dayPaint)
    canvas.restore()

    return bitmap
}

// 4. ASYMMETRIC SLANTED DIGITAL (2x2 / Extreme Outer Alignments with Accent Minutes)
fun generateAsymmetricSlantedDigitalClockBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val w = (wDp * scaleFactor).toInt().coerceAtLeast(420)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(420)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE

    val size = minOf(w, h).toFloat()
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    val cornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val timeState = DigitalClockTimeState.now()

    // Outer Margins (Pushes Date to far left and Time flush to far right)
    val dateLeftX = leftX + size * 0.10f
    val timeRightX = leftX + size * 0.90f

    // 1. Bold Slanted Hour (Top Right)
    val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        typeface = getSlateFont(context, weight = 600, isItalic = true)
        textSize = size * 0.35f
        textAlign = Paint.Align.RIGHT
    }
    val hourY = topY + size * 0.58f
    drawAutoFitText(canvas, timeState.hour12, timeRightX, hourY, size * 0.45f, hourPaint)

    // 2. Accent Slanted Minute (Bottom Right)
    val minPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        typeface = getSlateFont(context, weight = 200, isItalic = true)
        textSize = size * 0.35f
        textAlign = Paint.Align.RIGHT
    }
    val minY = topY + size * 0.88f
    drawAutoFitText(canvas, timeState.minute, timeRightX, minY, size * 0.45f, minPaint)

    // 3. Slanted Date Label (Bottom Far Left)
    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        typeface = getSlateFont(context, weight = 200, isItalic = true)
        textSize = size * 0.16f
        textAlign = Paint.Align.LEFT
    }
    val dateLabel = "${timeState.dayOfMonth} ${timeState.monthName.take(3).uppercase()}"
    drawAutoFitText(canvas, dateLabel, dateLeftX, minY, size * 0.38f, datePaint)

    return bitmap
}

// 5. COMPACT BLOCK DIGITAL (2x2 / Full-Card Centered 4-Digit Time)
fun generateCompactBlockDigitalClockBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val w = (wDp * scaleFactor).toInt().coerceAtLeast(420)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(420)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE

    val size = minOf(w, h).toFloat()
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    val cornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val timeState = DigitalClockTimeState.now()
    val clockCx = cardRect.centerX()

    // Custom Font Fallback Pipeline
    val customTypeface = try {
        ResourcesCompat.getFont(context, R.font.outward_block) ?: Typeface.DEFAULT_BOLD
    } catch (_: Exception) {
        Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }

    // 4-Digit 24-Hour Time Format (e.g. "1948")
    val timeString = "${timeState.hour24.padStart(2, '0')}${timeState.minute.padStart(2, '0')}"

    // Full-Card Scaled Block Time Display
    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        typeface = customTypeface
        textSize = size * 1.3f
        textAlign = Paint.Align.CENTER
    }

    // Exact Mathematical Vertical Centering
    val timeY = topY + (size * 0.955f)
    drawAutoFitText(canvas, timeString, clockCx, timeY, size * 0.86f, timePaint)

    return bitmap
}

// 6. ASYMMETRIC OVERLAY DIGITAL (2x2 / Background Muted Time with Accent Day & Refined Typography)
fun generateAsymmetricOverlayDigitalClockBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val w = (wDp * scaleFactor).toInt().coerceAtLeast(420)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(420)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE

    val size = minOf(w, h).toFloat()
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    val cornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val timeState = DigitalClockTimeState.now()

    // 1. Subtle Muted Background Time Digits
    val timeAlpha = if (isLight) 32 else 45
    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(timeAlpha, Color.red(primaryText), Color.green(primaryText), Color.blue(primaryText))
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = size * 0.48f
        textAlign = Paint.Align.RIGHT
    }

    val timeRightX = leftX + size * 0.94f
    val hourY = topY + size * 0.44f
    val minY = topY + size * 0.83f

    canvas.drawText(timeState.hour24.padStart(2, '0'), timeRightX, hourY, timePaint)
    canvas.drawText(timeState.minute.padStart(2, '0'), timeRightX, minY, timePaint)

    // Guaranteed Full Date Names ("SATURDAY" & "AUGUST 15")
    val now = java.util.Date()
    val fullDayName = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault()).format(now).uppercase()
    val fullMonthDay = java.text.SimpleDateFormat("MMMM d", java.util.Locale.getDefault()).format(now).uppercase()

    // 2. Full Day of Week Label (Accent Color, Size 0.12f, Normal Weight)
    val dateLeftX = leftX + size * 0.08f
    val dayNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        typeface = getSlateFont(context, weight = 300, isItalic = false)
        textSize = size * 0.12f
        textAlign = Paint.Align.LEFT
    }
    val dayNameY = topY + size * 0.81f
    drawAutoFitText(canvas, fullDayName, dateLeftX, dayNameY, size * 0.65f, dayNamePaint)

    // 3. Full Month & Date Label (Primary Color, Size 0.08f, Normal Weight)
    val dateSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        typeface = getSlateFont(context, weight = 300, isItalic = false)
        textSize = size * 0.08f
        textAlign = Paint.Align.LEFT
    }
    val dateSubY = topY + size * 0.90f
    drawAutoFitText(canvas, fullMonthDay, dateLeftX, dateSubY, size * 0.65f, dateSubPaint)

    return bitmap
}

// 7. TYPOGRAPHIC WORD CLOCK (2x2 / Editorial Stacked Word Time)
fun generateTextWordClockBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val w = (wDp * scaleFactor).toInt().coerceAtLeast(420)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(420)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE

    val size = minOf(w, h).toFloat()
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    val cornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val timeState = DigitalClockTimeState.now()
    val wordTime = getFuzzyWordTimeState(timeState.hour24.toIntOrNull() ?: 12, timeState.minute.toIntOrNull() ?: 0)

    val startX = leftX + size * 0.12f
    val maxTextWidth = size * 0.76f

    // 1. Heavy Ultra-Bold Paint for Primary Time Words ("Quarter", "Nine")
    val mainWordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        typeface = getSlateFont(context, weight = 800, isItalic = false)
        textSize = size * 0.22f
        textAlign = Paint.Align.LEFT
        letterSpacing = -0.02f
    }

    // 2. Refined Light Italic Paint for Connector Words ("to", "past")
    val connectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(215, Color.red(primaryText), Color.green(primaryText), Color.blue(primaryText))
        typeface = getSlateFont(context, weight = 200, isItalic = true)
        textSize = size * 0.13f
        textAlign = Paint.Align.LEFT
        letterSpacing = 0.04f
    }

    // 3. Compact Stack Layout Math
    if (wordTime.bottomWord.isEmpty()) {
        val line1Y = topY + size * 0.44f
        val line2Y = topY + size * 0.68f
        drawAutoFitText(canvas, wordTime.topWord, startX, line1Y, maxTextWidth, mainWordPaint)
        drawAutoFitText(canvas, wordTime.midWord, startX, line2Y, maxTextWidth, connectorPaint)
    } else {
        val line1Y = topY + size * 0.35f
        val line2Y = topY + size * 0.53f
        val line3Y = topY + size * 0.77f

        drawAutoFitText(canvas, wordTime.topWord, startX, line1Y, maxTextWidth, mainWordPaint)
        drawAutoFitText(canvas, wordTime.midWord, startX, line2Y, maxTextWidth, connectorPaint)
        drawAutoFitText(canvas, wordTime.bottomWord, startX, line3Y, maxTextWidth, mainWordPaint)
    }

    return bitmap
}

// 8. GIANT HOUR CAPSULE DIGITAL (2x2 / Giant Hour with Accent Minute Pill)
fun generateGiantHourCapsuleDigitalClockBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val w = (wDp * scaleFactor).toInt().coerceAtLeast(420)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(420)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE

    val size = minOf(w, h).toFloat()
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    val cornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val timeState = DigitalClockTimeState.now()
    val clockCx = cardRect.centerX()

    // 1. Giant Hour Text
    val hourStr = timeState.hour12
    val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        typeface = getSlateFont(context, weight = 800)
        textSize = size * 0.68f
        textAlign = Paint.Align.CENTER
    }

    val hourY = cardRect.centerY() - ((hourPaint.descent() + hourPaint.ascent()) / 2f) - (size * 0.035f)
    drawAutoFitText(canvas, hourStr, clockCx, hourY, size * 0.82f, hourPaint)

    // 2. Bottom Right Accent Minute Pill
    val minStr = timeState.minute.padStart(2, '0')
    val minTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = getContrastColor(accentColorInt)
        typeface = getSlateFont(context, weight = 800)
        textSize = size * 0.115f
        textAlign = Paint.Align.CENTER
    }

    val minTextW = minTextPaint.measureText(minStr)
    val pillPaddingH = size * 0.045f
    val pillW = (minTextW + (pillPaddingH * 2f)).coerceAtLeast(size * 0.22f)
    val pillH = size * 0.145f
    val pillRadius = size * 0.045f

    val pillRight = leftX + size * 0.92f
    val pillBottom = topY + size * 0.92f
    val pillRect = RectF(pillRight - pillW, pillBottom - pillH, pillRight, pillBottom)

    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(pillRect, pillRadius, pillRadius, pillPaint)

    val minTextY = pillRect.centerY() - ((minTextPaint.descent() + minTextPaint.ascent()) / 2f)
    canvas.drawText(minStr, pillRect.centerX(), minTextY, minTextPaint)

    return bitmap
}

// 9. MODERN 3D LED HORIZONTAL DIGITAL (4x2 / Tightly Bounded Max Fit with Custom BG & Accent Minutes)
fun generateModern3dLedHorizontalDigitalClockBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density

    // Create a tightly bound bitmap exactly matching the 2.4 aspect ratio
    val targetRatio = 2.4f
    val rawH = hDp * density
    val safeH = rawH.coerceIn(150f * density, 400f * density)

    val bitmapHeight = safeH.toInt()
    val bitmapWidth = (safeH * targetRatio).toInt()

    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE

    // --- THE FIX: Pull custom background color directly from user config ---
    val chassisBgColor = getSafeBgColor(config)

    val outerHighlightColor = if (isLight) Color.parseColor("#383437") else Color.parseColor("#FFFFFF")

    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.FILL
    }
    val accentActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // Transparent overlay creates realistic unlit segments automatically against any BG color
        color = if (isLight) Color.argb(22, 0, 0, 0) else Color.argb(28, 255, 255, 255)
        style = Paint.Style.FILL
    }

    val cardRect = RectF(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat())

    // 1. Mathematically Stable Pod Geometry
    val H = cardRect.height()

    val strokeW = H * 0.025f
    val inset = strokeW / 2f

    val top = cardRect.top + inset
    val bottom = cardRect.bottom - inset

    val usableW = cardRect.width() - (inset * 2f)
    val iDw = usableW * (0.65f / 2.4f)
    val iCw = usableW * (0.40f / 2.4f)
    val iOv = usableW * (0.15f / 2.4f)
    val chamfer = H * 0.18f

    fun createOctagonPath(l: Float, t: Float, r: Float, b: Float, c: Float): Path {
        return Path().apply {
            moveTo(l + c, t)
            lineTo(r - c, t)
            lineTo(r, t + c)
            lineTo(r, b - c)
            lineTo(r - c, b)
            lineTo(l + c, b)
            lineTo(l, b - c)
            lineTo(l, t + c)
            close()
        }
    }

    val p1L = inset
    val p1R = p1L + iDw
    val p2L = p1R - iOv
    val p2R = p2L + iDw
    val pCL = p2R - iOv
    val pCR = pCL + iCw
    val p3L = pCR - iOv
    val p3R = p3L + iDw
    val p4L = p3R - iOv
    val p4R = p4L + iDw

    val colonTopInset = H * 0.18f
    val colonChamfer = chamfer * 0.8f

    val pod1 = createOctagonPath(p1L, top, p1R, bottom, chamfer)
    val pod2 = createOctagonPath(p2L, top, p2R, bottom, chamfer)
    val podC = createOctagonPath(pCL, top + colonTopInset, pCR, bottom - colonTopInset, colonChamfer)
    val pod3 = createOctagonPath(p3L, top, p3R, bottom, chamfer)
    val pod4 = createOctagonPath(p4L, top, p4R, bottom, chamfer)

    val chassisPath = Path(pod1).apply {
        op(pod2, Path.Op.UNION)
        op(podC, Path.Op.UNION)
        op(pod3, Path.Op.UNION)
        op(pod4, Path.Op.UNION)
    }

    // 2. Render 3D Scalloped Chassis Fill & Outer Outline
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = chassisBgColor
        style = Paint.Style.FILL
    }
    val outerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = outerHighlightColor
        style = Paint.Style.STROKE
        strokeWidth = strokeW
        strokeJoin = Paint.Join.ROUND
    }

    canvas.drawPath(chassisPath, fillPaint)
    canvas.drawPath(chassisPath, outerBorderPaint)

    // 3. Centered 7-Segment Digits inside Pods
    val digitH = H * 0.82f
    val digitW = iDw * 0.68f
    val digitY = top + ((bottom - top - digitH) / 2f)

    val timeState = DigitalClockTimeState.now()
    val hourStr = timeState.hour24.padStart(2, '0')
    val minStr = timeState.minute.padStart(2, '0')

    fun getPodCenterX(l: Float, r: Float): Float = l + ((r - l) / 2f)

    // Hour Digits (Primary Color)
    val d1X = getPodCenterX(p1L, p1R) - (digitW / 2f)
    drawAngled7SegmentDigit(canvas, hourStr[0], d1X, digitY, digitW, digitH, activePaint, inactivePaint)

    val d2X = getPodCenterX(p2L, p2R) - (digitW / 2f)
    drawAngled7SegmentDigit(canvas, hourStr[1], d2X, digitY, digitW, digitH, activePaint, inactivePaint)

    // Colon Dots (Primary Color)
    val colonCx = getPodCenterX(pCL, pCR)
    val colonRadius = digitW * 0.14f
    val dot1Y = digitY + (digitH * 0.28f)
    val dot2Y = digitY + (digitH * 0.72f)
    canvas.drawCircle(colonCx, dot1Y, colonRadius, activePaint)
    canvas.drawCircle(colonCx, dot2Y, colonRadius, activePaint)

    // Minute Digits (Accent Color)
    val d3X = getPodCenterX(p3L, p3R) - (digitW / 2f)
    drawAngled7SegmentDigit(canvas, minStr[0], d3X, digitY, digitW, digitH, accentActivePaint, inactivePaint)

    val d4X = getPodCenterX(p4L, p4R) - (digitW / 2f)
    drawAngled7SegmentDigit(canvas, minStr[1], d4X, digitY, digitW, digitH, accentActivePaint, inactivePaint)

    return bitmap
}

// 10. GRADIENT TALL DIGITAL (4x2 / Smart Adaptive Dual-Mode)
fun generateGradientTallDigitalClockBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast((140 * density).toInt())
    val h = (hDp * density).toInt().coerceAtLeast((70 * density).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    // Dynamic Gradient Accent Setup
    val accentBottom = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val hsv = FloatArray(3)
    Color.colorToHSV(accentBottom, hsv)
    hsv[0] = (hsv[0] - 18f + 360f) % 360f
    hsv[1] = (hsv[1] * 0.75f).coerceIn(0f, 1f)
    hsv[2] = (hsv[2] * 1.15f).coerceIn(0f, 1f)
    val accentTop = Color.HSVToColor(hsv)

    // 1. Calculate Card Bounds (Responsive: 100% boundary; Fixed: 2:1 ratio centered)
    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val targetRatio = 2.0f
        var cardH = h.toFloat()
        var cardW = cardH * targetRatio

        if (cardW > w.toFloat()) {
            cardW = w.toFloat()
            cardH = cardW / targetRatio
        }

        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(density)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val padX = cardRect.width() * 0.07f
    val padY = cardRect.height() * 0.08f

    // Time & Date Data
    val timeState = DigitalClockTimeState.now()
    val hourStr = timeState.hour24.padStart(2, '0')
    val minStr = timeState.minute.padStart(2, '0')

    val cal = java.util.Calendar.getInstance()
    val dayNumStr = timeState.dayOfMonth
    val dayWeekStr = java.text.SimpleDateFormat("EEE", java.util.Locale.ENGLISH).format(cal.time)
    val monthNumStr = String.format(java.util.Locale.getDefault(), "%02d", cal.get(java.util.Calendar.MONTH) + 1)
    val yearStr = cal.get(java.util.Calendar.YEAR).toString()
    val dateLine2 = "$yearStr.$monthNumStr"

    val aspect = cardRect.width() / cardRect.height()

    if (aspect >= 1.25f) {
        // =========================================================================
        // WIDE LAYOUT (Horizontal Split: Time Left, Date Vertically Centered Right)
        // =========================================================================
        val dateTextSize = (cardRect.height() * 0.125f).coerceIn(11f * density, 24f * density)
        val datePaintPrimary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = dateTextSize
            typeface = getSlateFont(context, weight = 500)
            textAlign = Paint.Align.RIGHT
        }
        val datePaintAccent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentTop
            textSize = dateTextSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.RIGHT
        }
        val datePaintSecondary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = dateTextSize * 0.88f
            typeface = getSlateFont(context, weight = 400)
            textAlign = Paint.Align.RIGHT
        }

        val dateRightX = cardRect.right - padX

        val dayWeekW = datePaintAccent.measureText(dayWeekStr)
        val line1W = datePaintPrimary.measureText("$dayNumStr ") + dayWeekW
        val line2W = datePaintSecondary.measureText(dateLine2)
        val maxDateLineW = maxOf(line1W, line2W)

        // Tightened vertical gap between date lines
        val dateLineGap = dateTextSize * 1.08f
        val totalDateBlockH = dateTextSize + dateLineGap
        val dateTopY = cardRect.centerY() - (totalDateBlockH / 2f) + dateTextSize

        val leftSectionW = cardRect.width() - (padX * 2.2f) - maxDateLineW
        val leftSectionH = cardRect.height() - (padY * 2f)

        val refPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getSlateFont(context, weight = 700)
            textSize = 100f
        }
        val refHourW = refPaint.measureText(hourStr)
        val refColonGap = 100f * 0.08f
        val refMinW = refPaint.measureText(minStr)
        val refTotalW = refHourW + refColonGap + refMinW

        val maxFromHeight = leftSectionH * 0.84f
        val maxFromWidth = 100f * (leftSectionW / refTotalW)
        val timeTextSize = minOf(maxFromHeight, maxFromWidth).coerceAtLeast(14f * density)

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getSlateFont(context, weight = 700)
            textSize = timeTextSize
        }

        val hourW = timePaint.measureText(hourStr)
        val colonGap = timeTextSize * 0.08f
        val minW = timePaint.measureText(minStr)

        val timeStartX = cardRect.left + padX

        val timeBounds = android.graphics.Rect()
        timePaint.getTextBounds("00", 0, 2, timeBounds)
        val timeY = cardRect.centerY() + (timeBounds.height() / 2f) - (2f * density)

        val gradientTopY = timeY - timeBounds.height()
        val gradientBottomY = timeY

        val shader = android.graphics.LinearGradient(
            0f, gradientTopY, 0f, gradientBottomY,
            accentTop, accentBottom,
            android.graphics.Shader.TileMode.CLAMP
        )
        timePaint.shader = shader

        val colonCx = timeStartX + hourW + (colonGap / 2f)
        val minStartX = timeStartX + hourW + colonGap

        // Draw Hours
        canvas.drawText(hourStr, timeStartX, timeY, timePaint)

        // Draw Colon
        val dotRadius = timeTextSize * 0.038f
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.shader = shader
        }
        val dot1Y = timeY - (timeBounds.height() * 0.28f)
        val dot2Y = timeY - (timeBounds.height() * 0.68f)
        canvas.drawCircle(colonCx, dot1Y, dotRadius, dotPaint)
        canvas.drawCircle(colonCx, dot2Y, dotRadius, dotPaint)

        // Draw Minutes
        canvas.drawText(minStr, minStartX, timeY, timePaint)

        // Draw Date Block (Right Section)
        val currentDayWeekW = datePaintAccent.measureText(dayWeekStr)
        canvas.drawText(dayWeekStr, dateRightX, dateTopY, datePaintAccent)
        canvas.drawText("$dayNumStr ", dateRightX - currentDayWeekW, dateTopY, datePaintPrimary)

        val line2Y = dateTopY + dateLineGap
        canvas.drawText(dateLine2, dateRightX, line2Y, datePaintSecondary)

    } else {
        // =========================================================================
        // TALL / SQUARE LAYOUT (Vertical Stack: Date Top, Centered Time Below)
        // =========================================================================
        // Increased date font size ratio from 0.08f to 0.12f
        val dateTextSize = (cardRect.height() * 0.12f).coerceIn(12f * density, 22f * density)
        val datePaintPrimary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = dateTextSize
            typeface = getSlateFont(context, weight = 500)
            textAlign = Paint.Align.RIGHT
        }
        val datePaintAccent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentTop
            textSize = dateTextSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.RIGHT
        }
        val datePaintSecondary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = dateTextSize * 0.88f
            typeface = getSlateFont(context, weight = 400)
            textAlign = Paint.Align.RIGHT
        }

        val dateRightX = cardRect.right - padX
        val dateTopY = cardRect.top + padY + dateTextSize

        val currentDayWeekW = datePaintAccent.measureText(dayWeekStr)
        canvas.drawText(dayWeekStr, dateRightX, dateTopY, datePaintAccent)
        canvas.drawText("$dayNumStr ", dateRightX - currentDayWeekW, dateTopY, datePaintPrimary)

        // Tightened line gap in tall layout
        val line2Y = dateTopY + (dateTextSize * 1.08f)
        canvas.drawText(dateLine2, dateRightX, line2Y, datePaintSecondary)

        // Time Area strictly in remaining vertical space below date block
        val timeAreaTop = line2Y + (cardRect.height() * 0.03f)
        val timeAreaH = (cardRect.bottom - padY - timeAreaTop).coerceAtLeast(10f)
        val timeAreaW = cardRect.width() - (padX * 2f)

        val refPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getSlateFont(context, weight = 700)
            textSize = 100f
        }
        val refHourW = refPaint.measureText(hourStr)
        val refColonGap = 100f * 0.08f
        val refMinW = refPaint.measureText(minStr)
        val refTotalW = refHourW + refColonGap + refMinW

        val maxFromHeight = timeAreaH * 0.88f
        val maxFromWidth = 100f * (timeAreaW / refTotalW)
        val timeTextSize = minOf(maxFromHeight, maxFromWidth).coerceAtLeast(14f * density)

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getSlateFont(context, weight = 700)
            textSize = timeTextSize
        }

        val hourW = timePaint.measureText(hourStr)
        val colonGap = timeTextSize * 0.08f
        val minW = timePaint.measureText(minStr)
        val totalTimeW = hourW + colonGap + minW

        val timeStartX = cardRect.left + padX + ((timeAreaW - totalTimeW) / 2f).coerceAtLeast(0f)

        val timeBounds = android.graphics.Rect()
        timePaint.getTextBounds("00", 0, 2, timeBounds)
        val timeY = timeAreaTop + (timeAreaH + timeBounds.height()) / 2f - (2f * density)

        val gradientTopY = timeY - timeBounds.height()
        val gradientBottomY = timeY

        val shader = android.graphics.LinearGradient(
            0f, gradientTopY, 0f, gradientBottomY,
            accentTop, accentBottom,
            android.graphics.Shader.TileMode.CLAMP
        )
        timePaint.shader = shader

        val colonCx = timeStartX + hourW + (colonGap / 2f)
        val minStartX = timeStartX + hourW + colonGap

        // Draw Hours
        canvas.drawText(hourStr, timeStartX, timeY, timePaint)

        // Draw Colon
        val dotRadius = timeTextSize * 0.038f
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.shader = shader
        }
        val dot1Y = timeY - (timeBounds.height() * 0.28f)
        val dot2Y = timeY - (timeBounds.height() * 0.68f)
        canvas.drawCircle(colonCx, dot1Y, dotRadius, dotPaint)
        canvas.drawCircle(colonCx, dot2Y, dotRadius, dotPaint)

        // Draw Minutes
        canvas.drawText(minStr, minStartX, timeY, timePaint)
    }

    return bitmap
}

// 11. SCRIPT OVERLAY DIGITAL (4x2 / Centered Pastel Time with Cursive Day & Uppercase Date)
fun generateScriptOverlayDigitalClockBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast((220 * density).toInt())
    val h = (hDp * density).toInt().coerceAtLeast((110 * density).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE

    // 1. Calculate Card Bounds (Responsive: 100% boundary; Fixed: 2:1 ratio container)
    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val targetRatio = 2.0f
        var cardH = h.toFloat()
        var cardW = cardH * targetRatio

        if (cardW > w.toFloat()) {
            cardW = w.toFloat()
            cardH = cardW / targetRatio
        }

        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(density)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val padX = cardRect.width() * 0.08f
    val padY = cardRect.height() * 0.10f

    // Time & Date Formats
    val timeState = DigitalClockTimeState.now()
    val hourStr = timeState.hour24.padStart(2, '0')
    val minStr = timeState.minute.padStart(2, '0')
    val timeText = "$hourStr:$minStr"

    val cal = java.util.Calendar.getInstance()
    val fullDayName = java.text.SimpleDateFormat("EEEE", java.util.Locale.ENGLISH).format(cal.time).lowercase()
    val fullMonthName = java.text.SimpleDateFormat("MMMM d", java.util.Locale.ENGLISH).format(cal.time).uppercase()

    // 2. Centered Giant Background Time (Translucent Soft Pastel Tint)
    val r = Color.red(accentColorInt)
    val g = Color.green(accentColorInt)
    val b = Color.blue(accentColorInt)
    val giantTimeColor = Color.argb(65, r, g, b)

    val maxTimeW = cardRect.width() - (padX * 1.2f)
    val maxTimeH = cardRect.height() * 0.88f

    val refTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 700)
        textSize = 100f
    }
    val refTimeW = refTimePaint.measureText(timeText)
    val maxFromWidth = 100f * (maxTimeW / refTimeW)
    val timeTextSize = minOf(maxTimeH, maxFromWidth)

    val giantTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = giantTimeColor
        textSize = timeTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val timeBounds = android.graphics.Rect()
    giantTimePaint.getTextBounds(timeText, 0, timeText.length, timeBounds)
    val timeCx = cardRect.centerX()
    val timeY = cardRect.centerY() + (timeBounds.height() / 2f) - (2f * density)

    canvas.drawText(timeText, timeCx, timeY, giantTimePaint)

    // 3. Render Uppercase Bold Date Line at Bottom-Left ("AUGUST 16")
    var dateTextSize = cardRect.height() * 0.115f
    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = dateTextSize
        typeface = getSlateFont(context, weight = 300)
        textAlign = Paint.Align.LEFT
        letterSpacing = 0.04f
    }

    val maxDateW = cardRect.width() * 0.70f
    if (datePaint.measureText(fullMonthName) > maxDateW) {
        dateTextSize *= (maxDateW / datePaint.measureText(fullMonthName))
        datePaint.textSize = dateTextSize
    }

    val dateX = cardRect.left + padX
    val dateY = cardRect.bottom - padY - (2f * density)

    // 4. Render Cursive / Handwriting Weekday ("sunday")
    var scriptTextSize = cardRect.height() * 0.32f
    val scriptPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = scriptTextSize
        // Native Cursive script preserved for handwritten day style
        typeface = Typeface.create("cursive", Typeface.NORMAL)
        textAlign = Paint.Align.LEFT
    }

    val maxScriptW = cardRect.width() * 0.75f
    if (scriptPaint.measureText(fullDayName) > maxScriptW) {
        scriptTextSize *= (maxScriptW / scriptPaint.measureText(fullDayName))
        scriptPaint.textSize = scriptTextSize
    }

    val scriptX = cardRect.left + padX
    val scriptY = dateY - dateTextSize - (2f * density)

    canvas.drawText(fullDayName, scriptX, scriptY, scriptPaint)
    canvas.drawText(fullMonthName, dateX, dateY, datePaint)

    return bitmap
}

// 12. SPLIT FLAP DIGITAL (4x2 / Centered Dual Flip Cards & Tight Date Stack)
fun generateSplitFlapDigitalClockBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast((140 * density).toInt())
    val h = (hDp * density).toInt().coerceAtLeast((70 * density).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    // 1. Calculate Card Bounds (Responsive: 100% boundary; Fixed: 2:1 ratio container)
    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val targetRatio = 2.0f
        var cardH = h.toFloat()
        var cardW = cardH * targetRatio

        if (cardW > w.toFloat()) {
            cardW = w.toFloat()
            cardH = cardW / targetRatio
        }

        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(density)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val padX = cardRect.width() * 0.08f
    val padY = cardRect.height() * 0.08f

    // Time & Date Formats
    val timeState = DigitalClockTimeState.now()
    val hourStr = timeState.hour24.padStart(2, '0')
    val minStr = timeState.minute.padStart(2, '0')

    val cal = java.util.Calendar.getInstance()
    val dateStr = java.text.SimpleDateFormat("EEE, d MMM", java.util.Locale.ENGLISH).format(cal.time)

    // 2. Proportional Tile & Date Geometry Setup
    val tileGapX = cardRect.width() * 0.05f
    val usableW = cardRect.width() - (padX * 2f)
    val maxTileW = (usableW - tileGapX) / 2f

    // Reserve vertical space for tile stack while allocating 28% for date + gap
    val usableH = cardRect.height() - (padY * 2f)
    val maxTileH = usableH * 0.72f

    val tileW = minOf(maxTileW, maxTileH * 1.05f)
    val tileH = minOf(maxTileH, tileW / 1.05f)

    val totalTilesW = (tileW * 2f) + tileGapX
    val tileStartX = cardRect.centerX() - (totalTilesW / 2f)

    // Date Line Sizing & Exact Text Metrics
    var dateTextSize = (tileH * 0.22f).coerceIn(12f * density, 20f * density)
    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = dateTextSize
        typeface = getSlateFont(context, weight = 600)
        textAlign = Paint.Align.CENTER
    }

    val maxDateW = cardRect.width() - (padX * 2f)
    if (datePaint.measureText(dateStr) > maxDateW) {
        dateTextSize *= (maxDateW / datePaint.measureText(dateStr))
        datePaint.textSize = dateTextSize
    }

    val dateBounds = android.graphics.Rect()
    datePaint.getTextBounds(dateStr, 0, dateStr.length, dateBounds)
    val dateTextHeight = dateBounds.height().toFloat()

    // Fixed Proportional Gap between Tiles and Date
    val gapY = (tileH * 0.16f).coerceIn(8f * density, 18f * density)

    // 3. Center Entire Content Stack (Tiles + Gap + Date) Vertically in cardRect
    val totalBlockHeight = tileH + gapY + dateTextHeight
    val blockTopY = cardRect.centerY() - (totalBlockHeight / 2f)

    val tileTopY = blockTopY
    val dateY = blockTopY + tileH + gapY + dateTextHeight - dateBounds.bottom

    val tileRadius = (tileH * 0.14f).coerceIn(6f * density, 14f * density)
    val tileBgColor = if (isLight) Color.parseColor("#E5E5EA") else Color.parseColor("#222226")

    val tileBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tileBgColor
        style = Paint.Style.FILL
    }

    val splitLineColor = if (isLight) Color.parseColor("#C7C7CC") else Color.parseColor("#141416")
    val splitLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = splitLineColor
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }

    val pinColor = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#3A3A3C")
    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pinColor
        style = Paint.Style.FILL
    }

    // 4. Render Individual Flip Cards
    fun drawFlipCard(tileRect: RectF, digits: String) {
        canvas.drawRoundRect(tileRect, tileRadius, tileRadius, tileBgPaint)

        val maxTextW = tileRect.width() * 0.78f
        val maxTextH = tileRect.height() * 0.76f

        val refPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getSlateFont(context, weight = 700)
            textSize = 100f
        }
        val refW = refPaint.measureText(digits)
        val textScale = minOf(maxTextH / 100f, maxTextW / refW)
        val textSize = 100f * textScale

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            this.textSize = textSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }

        val textBounds = android.graphics.Rect()
        textPaint.getTextBounds(digits, 0, digits.length, textBounds)
        val textY = tileRect.centerY() + (textBounds.height() / 2f) - (2f * density)

        canvas.drawText(digits, tileRect.centerX(), textY, textPaint)

        // Draw Center Split Line & Hinge Pins
        val midY = tileRect.centerY()
        canvas.drawLine(tileRect.left, midY, tileRect.right, midY, splitLinePaint)

        val pinW = 4.5f * density
        val pinH = 3f * density
        val leftPin = RectF(tileRect.left, midY - (pinH / 2f), tileRect.left + pinW, midY + (pinH / 2f))
        val rightPin = RectF(tileRect.right - pinW, midY - (pinH / 2f), tileRect.right, midY + (pinH / 2f))
        canvas.drawRoundRect(leftPin, 1f * density, 1f * density, pinPaint)
        canvas.drawRoundRect(rightPin, 1f * density, 1f * density, pinPaint)
    }

    // Render Tiles
    val hourTileRect = RectF(tileStartX, tileTopY, tileStartX + tileW, tileTopY + tileH)
    val minTileRect = RectF(hourTileRect.right + tileGapX, tileTopY, hourTileRect.right + tileGapX + tileW, tileTopY + tileH)

    drawFlipCard(hourTileRect, hourStr)
    drawFlipCard(minTileRect, minStr)

    // Render Centered Date Line
    canvas.drawText(dateStr, cardRect.centerX(), dateY, datePaint)

    return bitmap
}

// 13. VERTICAL CAPSULE DIGITAL (1x2 / Unified Pill Capsule Monolith with Constant Gap)
fun generateVerticalCapsuleDigitalClockBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast((100 * density).toInt())
    val h = (hDp * density).toInt().coerceAtLeast((200 * density).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE

    // 1. Calculate Card Bounds (1:2 Target Aspect Ratio for 1x2 Fixed Mode)
    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val targetRatio = 0.5f
        var cardW = w.toFloat()
        var cardH = cardW / targetRatio

        if (cardH > h.toFloat()) {
            cardH = h.toFloat()
            cardW = cardH * targetRatio
        }

        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    // Outer Pill Capsule Shape (Half-Width Radius)
    val cardRadius = cardRect.width() / 2f
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val padX = cardRect.width() * 0.10f
    val padY = cardRect.height() * 0.08f

    // Time & Date Data
    val timeState = DigitalClockTimeState.now()
    val hourStr = timeState.hour24.padStart(2, '0')
    val minStr = timeState.minute.padStart(2, '0')

    val cal = java.util.Calendar.getInstance()
    val dayWeekStr = java.text.SimpleDateFormat("EEE", java.util.Locale.ENGLISH).format(cal.time).uppercase()
    val dayNumStr = timeState.dayOfMonth
    val badgeText = "$dayWeekStr $dayNumStr"

    // 2. Uniform Font Sizing
    val timeMaxW = cardRect.width() - (padX * 2f)
    val maxDigitH = (cardRect.height() - (padY * 2f)) * 0.30f

    val refPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 700)
        textSize = 100f
    }
    val refW = refPaint.measureText("00")
    val timeTextSize = minOf(maxDigitH, 100f * (timeMaxW / refW))

    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = timeTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val timeBounds = android.graphics.Rect()
    timePaint.getTextBounds("00", 0, 2, timeBounds)
    val digitH = timeBounds.height().toFloat()

    // Badge Dimensions
    val badgeW = cardRect.width() - (padX * 2f)
    val badgeHeight = (timeTextSize * 0.42f).coerceIn(22f * density, 42f * density)
    val badgeRadius = badgeHeight / 2f

    // Constant Gap between elements regardless of vertical stretching
    val gapY = (cardRect.width() * 0.08f).coerceIn(6f * density, 16f * density)

    // 3. Center Entire Stack (Hour + Gap + Badge + Gap + Minute) Vertically in cardRect
    val totalStackH = digitH + gapY + badgeHeight + gapY + digitH
    val stackTopY = cardRect.centerY() - (totalStackH / 2f)

    val hourY = stackTopY + digitH - timeBounds.bottom
    val badgeTop = stackTopY + digitH + gapY
    val minY = badgeTop + badgeHeight + gapY + digitH - timeBounds.bottom

    // Draw Top Hour Digits
    canvas.drawText(hourStr, cardRect.centerX(), hourY, timePaint)

    // Draw Center Accent Pill Badge
    val badgeLeft = cardRect.centerX() - (badgeW / 2f)
    val badgeRect = RectF(badgeLeft, badgeTop, badgeLeft + badgeW, badgeTop + badgeHeight)

    val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(badgeRect, badgeRadius, badgeRadius, badgeBgPaint)

    // Dynamic Contrast Color for Badge Text
    val r = Color.red(accentColorInt) / 255f
    val g = Color.green(accentColorInt) / 255f
    val b = Color.blue(accentColorInt) / 255f
    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
    val badgeTextColor = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

    var badgeTextSize = badgeHeight * 0.48f
    val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = badgeTextColor
        textSize = badgeTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.06f
    }

    val maxBadgeTextW = badgeRect.width() * 0.82f
    if (badgeTextPaint.measureText(badgeText) > maxBadgeTextW) {
        badgeTextSize *= (maxBadgeTextW / badgeTextPaint.measureText(badgeText))
        badgeTextPaint.textSize = badgeTextSize
    }

    val badgeTextBounds = android.graphics.Rect()
    badgeTextPaint.getTextBounds(badgeText, 0, badgeText.length, badgeTextBounds)
    val badgeTextY = badgeRect.centerY() + (badgeTextBounds.height() / 2f) - (1f * density)

    canvas.drawText(badgeText, badgeRect.centerX(), badgeTextY, badgeTextPaint)

    // Draw Bottom Minute Digits
    canvas.drawText(minStr, cardRect.centerX(), minY, timePaint)

    return bitmap
}

// 14. MINIMAL STACKED DIGITAL (1x2 / Clean Two-Tone Vertical Time with Accent Divider)
fun generateMinimalStackedDigitalClockBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast((100 * density).toInt())
    val h = (hDp * density).toInt().coerceAtLeast((200 * density).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE

    // 1. Calculate Card Bounds (1:2 Target Aspect Ratio for 1x2 Fixed Mode)
    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val targetRatio = 0.5f
        var cardW = w.toFloat()
        var cardH = cardW / targetRatio

        if (cardH > h.toFloat()) {
            cardH = h.toFloat()
            cardW = cardH * targetRatio
        }

        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    // Standard Card Radius (Not Pill Shaped)
    val cardRadius = getStandardCornerRadius(density)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val padX = cardRect.width() * 0.12f
    val padY = cardRect.height() * 0.10f

    // Time Data Only
    val timeState = DigitalClockTimeState.now()
    val hourStr = timeState.hour24.padStart(2, '0')
    val minStr = timeState.minute.padStart(2, '0')

    val usableW = cardRect.width() - (padX * 2f)
    val usableH = cardRect.height() - (padY * 2f)

    // Center Accent Divider Bar Specs
    val dividerW = usableW * 0.35f
    val dividerH = (2.5f * density).coerceAtLeast(2f)
    val dividerGapY = cardRect.height() * 0.04f

    val maxDigitH = (usableH - dividerH - (dividerGapY * 2f)) / 2f

    // 2. Uniform Font Sizing
    val refPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 700)
        textSize = 100f
    }
    val refW = refPaint.measureText("00")
    val timeTextSize = minOf(maxDigitH * 0.88f, 100f * (usableW / refW))

    val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = timeTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val minPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = timeTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val timeBounds = android.graphics.Rect()
    hourPaint.getTextBounds("00", 0, 2, timeBounds)
    val digitH = timeBounds.height().toFloat()

    // 3. Center Stack (Hour + Gap + Divider + Gap + Minute) Vertically
    val totalStackH = digitH + dividerGapY + dividerH + dividerGapY + digitH
    val stackTopY = cardRect.centerY() - (totalStackH / 2f)

    val hourY = stackTopY + digitH - timeBounds.bottom
    val dividerCenterY = stackTopY + digitH + dividerGapY + (dividerH / 2f)
    val minY = dividerCenterY + (dividerH / 2f) + dividerGapY + digitH - timeBounds.bottom

    // Draw Top Hour Digits (Primary Text)
    canvas.drawText(hourStr, cardRect.centerX(), hourY, hourPaint)

    // Draw Minimal Accent Center Divider Line
    val dividerLeft = cardRect.centerX() - (dividerW / 2f)
    val dividerRect = RectF(
        dividerLeft,
        dividerCenterY - (dividerH / 2f),
        dividerLeft + dividerW,
        dividerCenterY + (dividerH / 2f)
    )
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(dividerRect, dividerH / 2f, dividerH / 2f, dividerPaint)

    // Draw Bottom Minute Digits (Accent Text Color)
    canvas.drawText(minStr, cardRect.centerX(), minY, minPaint)

    return bitmap
}

// 15. TEXT DIGITAL FONT 1 (4x2 / Pure Typographic Giant 4-Digit Time)
fun generateTextFont1DigitalClockBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast((220 * density).toInt())
    val h = (hDp * density).toInt().coerceAtLeast((110 * density).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    // Fixed 2:1 Container Bounds
    val targetRatio = 2.0f
    var cardH = h.toFloat()
    var cardW = cardH * targetRatio

    if (cardW > w.toFloat()) {
        cardW = w.toFloat()
        cardH = cardW / targetRatio
    }

    val leftX = (w - cardW) / 2f
    val topY = (h - cardH) / 2f
    val cardRect = RectF(leftX, topY, leftX + cardW, topY + cardH)

    val cardRadius = getStandardCornerRadius(density)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    // Time Data (4-digit format without colon)
    val timeState = DigitalClockTimeState.now()
    val hourStr = timeState.hour24.padStart(2, '0')
    val minStr = timeState.minute.padStart(2, '0')
    val timeText = "$hourStr$minStr"

    val padX = cardRect.width() * 0.05f
    val padY = cardRect.height() * 0.06f
    val maxW = cardRect.width() - (padX * 2f)
    val maxH = cardRect.height() - (padY * 2f)

    // Measure & scale text to fill maximal bounds cleanly
    val refPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 700)
        textSize = 100f
    }
    val refW = refPaint.measureText(timeText)
    val timeTextSize = minOf(maxH * 0.96f, 100f * (maxW / refW))

    // Text paint set to accent color
    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = timeTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val timeBounds = android.graphics.Rect()
    timePaint.getTextBounds(timeText, 0, timeText.length, timeBounds)
    val timeY = cardRect.centerY() + (timeBounds.height() / 2f) - (2f * density)

    canvas.drawText(timeText, cardRect.centerX(), timeY, timePaint)

    return bitmap
}

fun generateTextCustomFontDigitalClockBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    fontResId: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast((220 * density).toInt())
    val h = (hDp * density).toInt().coerceAtLeast((110 * density).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val customTypeface = androidx.core.content.res.ResourcesCompat.getFont(
        context,
        fontResId
    ) ?: Typeface.DEFAULT_BOLD

    // Fixed 2:1 Container Bounds
    val targetRatio = 2.0f
    var cardH = h.toFloat()
    var cardW = cardH * targetRatio

    if (cardW > w.toFloat()) {
        cardW = w.toFloat()
        cardH = cardW / targetRatio
    }

    val leftX = (w - cardW) / 2f
    val topY = (h - cardH) / 2f
    val cardRect = RectF(leftX, topY, leftX + cardW, topY + cardH)

    val cardRadius = getStandardCornerRadius(density)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    // Time Data (4-digit format without colon)
    val timeState = DigitalClockTimeState.now()
    val hourStr = timeState.hour24.padStart(2, '0')
    val minStr = timeState.minute.padStart(2, '0')
    val timeText = "$hourStr$minStr"

    val padX = cardRect.width() * 0.05f
    val padY = cardRect.height() * 0.06f
    val maxW = cardRect.width() - (padX * 2f)
    val maxH = cardRect.height() - (padY * 2f)

    val refPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = customTypeface
        textSize = 100f
    }
    val refW = refPaint.measureText(timeText)
    val timeTextSize = minOf(maxH * 0.96f, 100f * (maxW / refW))

    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = timeTextSize
        typeface = customTypeface
        textAlign = Paint.Align.CENTER
    }

    val timeBounds = android.graphics.Rect()
    timePaint.getTextBounds(timeText, 0, timeText.length, timeBounds)
    val timeY = cardRect.centerY() + (timeBounds.height() / 2f) - (2f * density)

    canvas.drawText(timeText, cardRect.centerX(), timeY, timePaint)

    return bitmap
}

// 16. TEXT DIGITAL FONT 2 (4x2 / Saint Regular)
fun generateTextFont2DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.basteleur_bold)

// 17. TEXT DIGITAL FONT 3 (4x2 / Saint Regular)
fun generateTextFont3DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.saint_regular)

// 18. TEXT DIGITAL FONT 4 (4x2 / Heal The Web)
fun generateTextFont4DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.heal_the_web)

// 19. TEXT DIGITAL FONT 5 (4x2 / Slibinas)
fun generateTextFont5DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.slibinas)

// 20. TEXT DIGITAL FONT 6 (4x2 / Trille GX)
fun generateTextFont6DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.trille_gx)

// 21. TEXT DIGITAL FONT 7 (4x2 / Kulture Type)
fun generateTextFont7DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.kulture_type)

// 22. TEXT DIGITAL FONT 8 (4x2 / Trickster)
fun generateTextFont8DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.trickster)

// 23. TEXT DIGITAL FONT 9 (4x2 / Kihim)
fun generateTextFont9DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.kihim)

// 24. TEXT DIGITAL FONT 10 (4x2 / Styro Extrabold)
fun generateTextFont10DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.styro_extrabold)

// 25. TEXT DIGITAL FONT 11 (4x2 / Le Murmure)
fun generateTextFont11DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.le_murmure)

// 26. TEXT DIGITAL FONT 12 (4x2 / Phosphene Font)
fun generateTextFont12DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.phosphene_font)

// 27. TEXT DIGITAL FONT 13 (4x2 / Sankofa Display)
fun generateTextFont13DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.sankofadisplay)

// 28. TEXT DIGITAL FONT 14 (4x2 / Bitcount Roman)
fun generateTextFont14DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.bitcount_roman)

// 29. TEXT DIGITAL FONT 15 (4x2 / Rubik Dirt)
fun generateTextFont15DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.rubikdirt)

// 30. TEXT DIGITAL FONT 16 (4x2 / Rubik Glitch)
fun generateTextFont16DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.rubikglitch)

// 31. TEXT DIGITAL FONT 17 (4x2 / Rubik Marker Hatch)
fun generateTextFont17DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.rubikmarkerhatch)

// 32. TEXT DIGITAL FONT 18 (4x2 / Modak)
fun generateTextFont18DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.modak)

// 33. TEXT DIGITAL FONT 19 (4x2 / Moolahlah)
fun generateTextFont19DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.moolahlah)

// 34. TEXT DIGITAL FONT 20 (4x2 / Caesar Dressing)
fun generateTextFont20DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.caesardressing)

// 35. TEXT DIGITAL FONT 21 (4x2 / Doto Rounded)
fun generateTextFont21DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.doto_rounded)

// 36. TEXT DIGITAL FONT 22 (4x2 / Blaka)
fun generateTextFont22DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.blaka)

// 37. TEXT DIGITAL FONT 23 (4x2 / Barrio)
fun generateTextFont23DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.barrio)

// 38. TEXT DIGITAL FONT 24 (4x2 / Blaka Hollow)
fun generateTextFont24DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.blakahollow)

// 39. TEXT DIGITAL FONT 25 (4x2 / Tourney)
fun generateTextFont25DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.tourney)

// 40. TEXT DIGITAL FONT 26 (4x2 / Sixtyfour)
fun generateTextFont26DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.sixtyfour)

// 41. TEXT DIGITAL FONT 27 (4x2 / Monoton)
fun generateTextFont27DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.monoton)

// 42. TEXT DIGITAL FONT 28 (4x2 / Matemasie)
fun generateTextFont28DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.matemasie)

// 43. TEXT DIGITAL FONT 29 (4x2 / Fascinate)
fun generateTextFont29DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.fascinate)

// 44. TEXT DIGITAL FONT 30 (4x2 / Foldit Bold)
fun generateTextFont30DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.foldit_bold)

// 45. TEXT DIGITAL FONT 31 (4x2 / Frijole)
fun generateTextFont31DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.frijole)

// 46. TEXT DIGITAL FONT 32 (4x2 / Fruktur)
fun generateTextFont32DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.fruktur)

// 47. TEXT DIGITAL FONT 33 (4x2 / Londrina Outline)
fun generateTextFont33DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.londrinaoutline)

// 48. TEXT DIGITAL FONT 34 (4x2 / Molle Italic)
fun generateTextFont34DigitalClockBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
    generateTextCustomFontDigitalClockBitmap(context, config, isResponsive, wDp, hDp, R.font.molle_italic)