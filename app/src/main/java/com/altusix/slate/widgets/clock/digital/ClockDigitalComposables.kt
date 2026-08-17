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



// 1. MINIMAL DIVIDER DIGITAL (2x2 / Stacked Time with Accent Line Divider)
fun generateMinimalDividerDigitalClockBitmap(    context: Context,    config: SlateWidgetConfig,    isResponsive: Boolean,    wDp: Int,    hDp: Int): Bitmap {
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

// 2. COMPACT BLOCK DIGITAL (2x2 / Full-Card Centered 4-Digit Time)
fun generateCompactBlockDigitalClockBitmap(    context: Context,    config: SlateWidgetConfig,    isResponsive: Boolean,    wDp: Int,    hDp: Int): Bitmap {
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

// 3. TYPOGRAPHIC WORD CLOCK (2x2 / Editorial Stacked Word Time)
fun generateTextWordClockBitmap(    context: Context,    config: SlateWidgetConfig,    isResponsive: Boolean,    wDp: Int,    hDp: Int): Bitmap {
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
fun generateGiantHourCapsuleDigitalClockBitmap(    context: Context,    config: SlateWidgetConfig,    isResponsive: Boolean,    wDp: Int,    hDp: Int): Bitmap {
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
fun generateModern3dLedHorizontalDigitalClockBitmap(    context: Context,    config: SlateWidgetConfig,    isResponsive: Boolean,    wDp: Int,    hDp: Int): Bitmap {
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

// 14. MINIMAL STACKED DIGITAL (1x2 / Clean Two-Tone Vertical Time with Accent Divider)
fun generateMinimalStackedDigitalClockBitmap(    context: Context,    config: SlateWidgetConfig,    isResponsive: Boolean,    wDp: Int,    hDp: Int): Bitmap {
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
fun generateTextFont1DigitalClockBitmap(    context: Context,    config: SlateWidgetConfig,    isResponsive: Boolean,    wDp: Int,    hDp: Int,): Bitmap {
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

fun generateTextCustomFontDigitalClockBitmap(    context: Context,    config: SlateWidgetConfig,    isResponsive: Boolean,    wDp: Int,    hDp: Int,    fontResId: Int): Bitmap {
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