package com.altusix.slate.widgets.clock.hybrid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.altusix.slate.R
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.widgets.clock.digital.DigitalClockTimeState
import com.altusix.slate.widgets.clock.digital.drawAutoFitText
import com.altusix.slate.widgets.clock.digital.getSlateFont

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

// 1. ANALOG DIGITAL SPLIT HYBRID (4x2 / Adaptive Horizontal or Vertical Layout)
fun generateAnalogDigitalSplitHybridClockBitmap(
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
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

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

    // =========================================================================
    // PADDING & LAYOUT CONTROLS
    // =========================================================================
    val padX = cardRect.width() * 0.07f   // Outer Horizontal Margin (Left & Right equal)
    val padY = cardRect.height() * 0.08f  // Outer Vertical Margin (Top & Bottom equal)
    val gap = cardRect.width() * 0.04f    // Gap between Analog and Digital components

    val timeState = HybridClockTimeState.now()
    val hourStr = timeState.hour24.toString().padStart(2, '0')
    val minStr = timeState.minute.toString().padStart(2, '0')
    val digitalTimeStr = "$hourStr:$minStr"
    val dateStr = "${timeState.dayOfWeek}, ${timeState.dayOfMonth} ${timeState.monthName}"

    val aspect = cardRect.width() / cardRect.height()

    // Helper function to render analog clock dial & hands
    fun drawAnalogDial(centerX: Float, centerY: Float, radius: Float) {
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            style = Paint.Style.FILL
        }
        for (i in 0 until 12) {
            val angleRad = Math.toRadians((i * 30).toDouble())
            val isMajor = i % 3 == 0
            val markerRadius = if (isMajor) 2.2f * density else 1.2f * density
            val dist = radius * 0.84f
            val mx = centerX + (Math.cos(angleRad) * dist).toFloat()
            val my = centerY + (Math.sin(angleRad) * dist).toFloat()
            canvas.drawCircle(mx, my, markerRadius, tickPaint)
        }

        val hourAngleRad = Math.toRadians(((timeState.hour12 % 12 + timeState.minute / 60f) * 30f - 90f).toDouble())
        val minAngleRad = Math.toRadians(((timeState.minute + timeState.second / 60f) * 6f - 90f).toDouble())

        // Hour Hand
        val hourHandLen = radius * 0.48f
        val hourHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            style = Paint.Style.STROKE
            strokeWidth = 3f * density
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(
            centerX, centerY,
            centerX + (Math.cos(hourAngleRad) * hourHandLen).toFloat(),
            centerY + (Math.sin(hourAngleRad) * hourHandLen).toFloat(),
            hourHandPaint
        )

        // Minute Hand
        val minHandLen = radius * 0.72f
        val minHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(
            centerX, centerY,
            centerX + (Math.cos(minAngleRad) * minHandLen).toFloat(),
            centerY + (Math.sin(minAngleRad) * minHandLen).toFloat(),
            minHandPaint
        )

        // Center Cap Dot
        val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, 3f * density, capPaint)
    }

    if (aspect >= 1.20f) {
        // =========================================================================
        // WIDE / HORIZONTAL SPLIT (Side-by-Side)
        // Exact Symmetrical Padding: Left margin = padX, Right margin = padX
        // =========================================================================
        val usableH = cardRect.height() - (padY * 2f)
        val usableW = cardRect.width() - (padX * 2f) - gap

        val dialDiameter = minOf(usableW * 0.42f, usableH)
        val dialRadius = dialDiameter / 2f
        val dialCenterX = cardRect.left + padX + dialRadius
        val dialCenterY = cardRect.centerY()

        drawAnalogDial(dialCenterX, dialCenterY, dialRadius)

        val digitalLeftX = cardRect.left + padX + dialDiameter + gap
        val digitalRightX = cardRect.right - padX
        val digitalAreaW = digitalRightX - digitalLeftX

        val maxTimeH = usableH * 0.5f
        val refTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getSlateFont(context, weight = 700)
            textSize = 100f
        }
        val refTimeW = refTimePaint.measureText(digitalTimeStr)
        val timeTextSize = minOf(maxTimeH, 100f * (digitalAreaW / refTimeW)).coerceAtLeast(11f * density)

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = timeTextSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.LEFT
        }

        val timeBounds = Rect()
        timePaint.getTextBounds(digitalTimeStr, 0, digitalTimeStr.length, timeBounds)

        var dateTextSize = (timeTextSize * 0.28f).coerceAtLeast(10f * density)
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            textSize = dateTextSize
            typeface = getSlateFont(context, weight = 400)
            textAlign = Paint.Align.LEFT
            letterSpacing = 0.04f
        }

        if (datePaint.measureText(dateStr) > digitalAreaW) {
            dateTextSize *= (digitalAreaW / datePaint.measureText(dateStr))
            datePaint.textSize = dateTextSize
        }

        val dateBounds = Rect()
        datePaint.getTextBounds(dateStr, 0, dateStr.length, dateBounds)

        val blockGap = timeTextSize * 0.10f
        val totalRightH = timeBounds.height() + blockGap + dateBounds.height()
        val rightTopY = cardRect.centerY() - (totalRightH / 2f)

        val timeY = rightTopY + timeBounds.height()
        val dateY = timeY + blockGap + dateBounds.height()

        canvas.drawText(digitalTimeStr, digitalLeftX, timeY, timePaint)
        canvas.drawText(dateStr, digitalLeftX, dateY, datePaint)

    } else {
        // =========================================================================
        // TALL / SQUARE LAYOUT (Vertical Stack: Dial Top, Digital Time + Date Bottom)
        // Prevents text overflow/squishing in 2x2, 1x2, or square slots
        // =========================================================================
        val usableW = cardRect.width() - (padX * 2f)
        val usableH = cardRect.height() - (padY * 2f)

        val dialAreaH = usableH * 0.58f
        val digitalAreaH = usableH - dialAreaH

        val dialDiameter = minOf(usableW, dialAreaH)
        val dialRadius = dialDiameter / 2f
        val dialCenterX = cardRect.centerX()
        val dialCenterY = cardRect.top + padY + (dialAreaH / 2f)

        drawAnalogDial(dialCenterX, dialCenterY, dialRadius)

        val refTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getSlateFont(context, weight = 700)
            textSize = 100f
        }
        val refTimeW = refTimePaint.measureText(digitalTimeStr)

        val maxTimeH = digitalAreaH * 0.50f
        val timeTextSize = minOf(maxTimeH, 100f * (usableW / refTimeW)).coerceAtLeast(12f * density)

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = timeTextSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }

        val timeBounds = Rect()
        timePaint.getTextBounds(digitalTimeStr, 0, digitalTimeStr.length, timeBounds)

        var dateTextSize = (timeTextSize * 0.28f).coerceAtLeast(10f * density)
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            textSize = dateTextSize
            typeface = getSlateFont(context, weight = 600)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.04f
        }

        if (datePaint.measureText(dateStr) > usableW) {
            dateTextSize *= (usableW / datePaint.measureText(dateStr))
            datePaint.textSize = dateTextSize
        }

        val dateBounds = Rect()
        datePaint.getTextBounds(dateStr, 0, dateStr.length, dateBounds)

        val blockGap = timeTextSize * 0.10f
        val digitalTopY = cardRect.top + padY + dialAreaH + ((digitalAreaH - (timeBounds.height() + blockGap + dateBounds.height())) / 2f)

        val timeY = digitalTopY + timeBounds.height()
        val dateY = timeY + blockGap + dateBounds.height()

        canvas.drawText(digitalTimeStr, cardRect.centerX(), timeY, timePaint)
        canvas.drawText(dateStr, cardRect.centerX(), dateY, datePaint)
    }

    return bitmap
}

// 2. BOLD TYPOGRAPHIC DIGITAL (2x2 Square / Stacked Giant Hour & Minute Display)
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

// 5. ASYMMETRIC OVERLAY DIGITAL (2x2 / Background Muted Time with Accent Day & Refined Typography)
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

// 6. GRADIENT TALL DIGITAL (4x2 / Smart Adaptive Dual-Mode)
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

// 7. SCRIPT OVERLAY DIGITAL (4x2 / Centered Pastel Time with Cursive Day & Uppercase Date)
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

// 8. SPLIT FLAP DIGITAL (4x2 / Centered Dual Flip Cards & Tight Date Stack)
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

// 9. VERTICAL CAPSULE DIGITAL (1x2 / Unified Pill Capsule Monolith with Constant Gap)
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