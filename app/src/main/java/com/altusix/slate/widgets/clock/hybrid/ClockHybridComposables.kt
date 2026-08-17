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

// --- AUTHENTIC 6-SIDED 7-SEGMENT LCD RENDERER ---
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
    val ht = t / 2f          // Half thickness
    val g = t * 0.22f        // Uniform gap
    val midY = y + (h / 2f)

    val pathA = Path().apply {
        moveTo(x + ht + g, y + ht)
        lineTo(x + t + g, y)
        lineTo(x + w - t - g, y)
        lineTo(x + w - ht - g, y + ht)
        lineTo(x + w - t - g, y + t)
        lineTo(x + t + g, y + t)
        close()
    }

    val pathB = Path().apply {
        moveTo(x + w - ht, y + ht + g)
        lineTo(x + w, y + t + g)
        lineTo(x + w, midY - ht - g)
        lineTo(x + w - ht, midY - g)
        lineTo(x + w - t, midY - ht - g)
        lineTo(x + w - t, y + t + g)
        close()
    }

    val pathC = Path().apply {
        moveTo(x + w - ht, midY + g)
        lineTo(x + w, midY + ht + g)
        lineTo(x + w, y + h - t - g)
        lineTo(x + w - ht, y + h - ht - g)
        lineTo(x + w - t, y + h - t - g)
        lineTo(x + w - t, midY + ht + g)
        close()
    }

    val pathD = Path().apply {
        moveTo(x + ht + g, y + h - ht)
        lineTo(x + t + g, y + h - t)
        lineTo(x + w - t - g, y + h - t)
        lineTo(x + w - ht - g, y + h - ht)
        lineTo(x + w - t - g, y + h)
        lineTo(x + t + g, y + h)
        close()
    }

    val pathE = Path().apply {
        moveTo(x + ht, midY + g)
        lineTo(x + t, midY + ht + g)
        lineTo(x + t, y + h - t - g)
        lineTo(x + ht, y + h - ht - g)
        lineTo(x, y + h - t - g)
        lineTo(x, midY + ht + g)
        close()
    }

    val pathF = Path().apply {
        moveTo(x + ht, y + ht + g)
        lineTo(x + t, y + t + g)
        lineTo(x + t, midY - ht - g)
        lineTo(x + ht, midY - g)
        lineTo(x, midY - ht - g)
        lineTo(x, y + t + g)
        close()
    }

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
    val gap = cardRect.width() * 0.04f

    val timeState = HybridClockTimeState.now()
    val hourStr = timeState.hour24.toString().padStart(2, '0')
    val minStr = timeState.minute.toString().padStart(2, '0')
    val digitalTimeStr = "$hourStr:$minStr"
    val dateStr = "${timeState.dayOfWeek}, ${timeState.dayOfMonth} ${timeState.monthName}"

    val aspect = cardRect.width() / cardRect.height()

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

        val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, 3f * density, capPaint)
    }

    if (aspect >= 1.20f) {
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

// 2. MINIMAL DIAL HYBRID (2x2 Square / Full Dial, Stacked HH/MM Top Right, Accent Day)
fun generateMinimalDialHybridClockBitmap(
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
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#80FFFFFF")

    // Fixed 1:1 Aspect Ratio Card
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

    val timeState = HybridClockTimeState.now()
    val dialCenterX = cardRect.centerX()
    val dialCenterY = cardRect.centerY()
    val dialRadius = size * 0.42f

    // =========================================================================
    // 1. DIAL MARKS (Cardinal 12, 3, 6, 9 Ticks)
    // =========================================================================
    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        style = Paint.Style.STROKE
        strokeWidth = size * 0.012f
        strokeCap = Paint.Cap.ROUND
    }

    for (i in listOf(0, 3, 6, 9)) {
        val angleRad = Math.toRadians((i * 30).toDouble())
        val cos = Math.cos(angleRad).toFloat()
        val sin = Math.sin(angleRad).toFloat()
        val innerR = dialRadius * 0.88f
        val outerR = dialRadius * 0.98f
        canvas.drawLine(
            dialCenterX + cos * innerR,
            dialCenterY + sin * innerR,
            dialCenterX + cos * outerR,
            dialCenterY + sin * outerR,
            tickPaint
        )
    }

    // =========================================================================
    // 2. STACKED DIGITAL TIME (Top Right Overlay)
    // =========================================================================
    val hourStr = timeState.hour24.toString().padStart(2, '0')
    val minStr = timeState.minute.toString().padStart(2, '0')

    val digitalRightX = cardRect.right - (size * 0.12f)
    val digitalTopY = cardRect.top + (size * 0.15f)

    // Bold Hour Digits
    val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = size * 0.17f
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.RIGHT
    }
    val hourBounds = Rect()
    hourPaint.getTextBounds(hourStr, 0, hourStr.length, hourBounds)
    val hourY = digitalTopY + hourBounds.height()
    canvas.drawText(hourStr, digitalRightX, hourY, hourPaint)

    // Thin/Light Minute Digits
    val minPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = size * 0.17f
        typeface = getSlateFont(context, weight = 200)
        textAlign = Paint.Align.RIGHT
    }
    val minBounds = Rect()
    minPaint.getTextBounds(minStr, 0, minStr.length, minBounds)
    val minY = hourY + minBounds.height() + (size * 0.02f)
    canvas.drawText(minStr, digitalRightX, minY, minPaint)

    // =========================================================================
    // 3. ACCENT DAY LABEL (Bottom Center Accent)
    // =========================================================================
    val cal = java.util.Calendar.getInstance()
    val fullDayName = java.text.SimpleDateFormat("EEEE", java.util.Locale.ENGLISH).format(cal.time).uppercase()

    val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = size * 0.055f
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.08f
    }
    val dayY = cardRect.bottom - (size * 0.18f)
    canvas.drawText(fullDayName, dialCenterX, dayY, dayPaint)

    // =========================================================================
    // 4. ANALOG CLOCK HANDS
    // =========================================================================
    val hourAngleRad = Math.toRadians(((timeState.hour12 % 12 + timeState.minute / 60f) * 30f - 90f).toDouble())
    val minAngleRad = Math.toRadians(((timeState.minute + timeState.second / 60f) * 6f - 90f).toDouble())
    val secAngleRad = Math.toRadians(((timeState.second) * 6f - 90f).toDouble())

    // Hour Hand (Wide Rounded Capsule Bar)
    val hourHandLen = dialRadius * 0.45f
    val hourHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = size * 0.038f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        dialCenterX, dialCenterY,
        dialCenterX + (Math.cos(hourAngleRad) * hourHandLen).toFloat(),
        dialCenterY + (Math.sin(hourAngleRad) * hourHandLen).toFloat(),
        hourHandPaint
    )

    // Minute Hand (Crisp Solid Hand)
    val minHandLen = dialRadius * 0.76f
    val minHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = size * 0.020f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        dialCenterX, dialCenterY,
        dialCenterX + (Math.cos(minAngleRad) * minHandLen).toFloat(),
        dialCenterY + (Math.sin(minAngleRad) * minHandLen).toFloat(),
        minHandPaint
    )

    // Second Needle Hand (Thin Accent Red Line)
    val secHandLen = dialRadius * 0.85f
    val secHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = size * 0.008f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        dialCenterX, dialCenterY,
        dialCenterX + (Math.cos(secAngleRad) * secHandLen).toFloat(),
        dialCenterY + (Math.sin(secAngleRad) * secHandLen).toFloat(),
        secHandPaint
    )

    // Layered Center Cap Dot
    val capOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(30, 0, 0, 0) else Color.argb(40, 255, 255, 255)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(dialCenterX, dialCenterY, size * 0.065f, capOuterPaint)

    val capInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.FILL
    }
    canvas.drawCircle(dialCenterX, dialCenterY, size * 0.028f, capInnerPaint)

    return bitmap
}

// 3. BOLD TYPOGRAPHIC HYBRID (2x2 Square / Stacked Giant Hour & Minute Display)
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

    val capsuleW = size * 0.78f
    val capsuleH = size * 0.13f
    val capsuleY = topY + size * 0.79f
    val capsuleRect = RectF(clockCx - (capsuleW / 2f), capsuleY, clockCx + (capsuleW / 2f), capsuleY + capsuleH)

    val capsuleBgColor = if (isLight) Color.argb(20, 0, 0, 0) else Color.argb(26, 255, 255, 255)
    val capsulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = capsuleBgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(capsuleRect, capsuleH / 2f, capsuleH / 2f, capsulePaint)

    val capsuleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(15, 0, 0, 0) else Color.argb(20, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = size * 0.006f
    }
    canvas.drawRoundRect(capsuleRect, capsuleH / 2f, capsuleH / 2f, capsuleStrokePaint)

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

// 4. LCD SEVEN SEGMENT HYBRID (High-DPI 2x2 / Centered 7-Segment LCD)
fun generateLcdSevenSegmentDigitalClockBitmap(
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

    val digitW = size * 0.245f
    val digitH = size * 0.42f
    val digitGap = size * 0.035f
    val twoDigitsW = (digitW * 2f) + digitGap

    val columnGap = size * 0.08f
    val dateTextSize = size * 0.082f

    val totalContentW = twoDigitsW + columnGap + dateTextSize
    val sidePadding = (size - totalContentW) / 2f

    val startX = leftX + sidePadding
    val rightCenterX = startX + twoDigitsW + columnGap + (dateTextSize / 2f)

    val hourY = topY + size * 0.065f
    val minY = topY + size * 0.515f

    val hourStr = timeState.hour24.padStart(2, '0')
    drawAngled7SegmentDigit(canvas, hourStr[0], startX, hourY, digitW, digitH, activePaint, inactivePaint)
    drawAngled7SegmentDigit(canvas, hourStr[1], startX + digitW + digitGap, hourY, digitW, digitH, activePaint, inactivePaint)

    val minStr = timeState.minute.padStart(2, '0')
    drawAngled7SegmentDigit(canvas, minStr[0], startX, minY, digitW, digitH, activePaint, inactivePaint)
    drawAngled7SegmentDigit(canvas, minStr[1], startX + digitW + digitGap, minY, digitW, digitH, activePaint, inactivePaint)

    val dotRadius = size * 0.024f
    val dotY = hourY + (digitH * 0.22f)
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawCircle(rightCenterX, dotY, dotRadius, dotPaint)

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

// 5. ASYMMETRIC SLANTED HYBRID (2x2 / Minimal Bottom-Weighted Layout)
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

    val dateLeftX = leftX + size * 0.10f
    val timeRightX = leftX + size * 0.90f

    val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        typeface = getSlateFont(context, weight = 600, isItalic = true)
        textSize = size * 0.35f
        textAlign = Paint.Align.RIGHT
    }
    val hourY = topY + size * 0.58f
    drawAutoFitText(canvas, timeState.hour12, timeRightX, hourY, size * 0.45f, hourPaint)

    val minPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        typeface = getSlateFont(context, weight = 200, isItalic = true)
        textSize = size * 0.35f
        textAlign = Paint.Align.RIGHT
    }
    val minY = topY + size * 0.88f
    drawAutoFitText(canvas, timeState.minute, timeRightX, minY, size * 0.45f, minPaint)

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

// 6. ASYMMETRIC OVERLAY HYBRID (2x2 / Translucent Giant Right Time with Bottom-Left Date)
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

    val now = java.util.Date()
    val fullDayName = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault()).format(now).uppercase()
    val fullMonthDay = java.text.SimpleDateFormat("MMMM d", java.util.Locale.getDefault()).format(now).uppercase()

    val dateLeftX = leftX + size * 0.08f
    val dayNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        typeface = getSlateFont(context, weight = 300, isItalic = false)
        textSize = size * 0.12f
        textAlign = Paint.Align.LEFT
    }
    val dayNameY = topY + size * 0.81f
    drawAutoFitText(canvas, fullDayName, dateLeftX, dayNameY, size * 0.65f, dayNamePaint)

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

// 7. GRADIENT TALL HYBRID (4x2 / Smart Adaptive Dual-Mode)
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

    val accentBottom = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val hsv = FloatArray(3)
    Color.colorToHSV(accentBottom, hsv)
    hsv[0] = (hsv[0] - 18f + 360f) % 360f
    hsv[1] = (hsv[1] * 0.75f).coerceIn(0f, 1f)
    hsv[2] = (hsv[2] * 1.15f).coerceIn(0f, 1f)
    val accentTop = Color.HSVToColor(hsv)

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

        canvas.drawText(hourStr, timeStartX, timeY, timePaint)

        val dotRadius = timeTextSize * 0.038f
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.shader = shader
        }
        val dot1Y = timeY - (timeBounds.height() * 0.28f)
        val dot2Y = timeY - (timeBounds.height() * 0.68f)
        canvas.drawCircle(colonCx, dot1Y, dotRadius, dotPaint)
        canvas.drawCircle(colonCx, dot2Y, dotRadius, dotPaint)

        canvas.drawText(minStr, minStartX, timeY, timePaint)

        val currentDayWeekW = datePaintAccent.measureText(dayWeekStr)
        canvas.drawText(dayWeekStr, dateRightX, dateTopY, datePaintAccent)
        canvas.drawText("$dayNumStr ", dateRightX - currentDayWeekW, dateTopY, datePaintPrimary)

        val line2Y = dateTopY + dateLineGap
        canvas.drawText(dateLine2, dateRightX, line2Y, datePaintSecondary)

    } else {
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

        val line2Y = dateTopY + (dateTextSize * 1.08f)
        canvas.drawText(dateLine2, dateRightX, line2Y, datePaintSecondary)

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

        canvas.drawText(hourStr, timeStartX, timeY, timePaint)

        val dotRadius = timeTextSize * 0.038f
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.shader = shader
        }
        val dot1Y = timeY - (timeBounds.height() * 0.28f)
        val dot2Y = timeY - (timeBounds.height() * 0.68f)
        canvas.drawCircle(colonCx, dot1Y, dotRadius, dotPaint)
        canvas.drawCircle(colonCx, dot2Y, dotRadius, dotPaint)

        canvas.drawText(minStr, minStartX, timeY, timePaint)
    }

    return bitmap
}

// 8. SCRIPT OVERLAY HYBRID (4x2 / Centered Pastel Time with Cursive Day & Uppercase Date)
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

    val timeState = DigitalClockTimeState.now()
    val hourStr = timeState.hour24.padStart(2, '0')
    val minStr = timeState.minute.padStart(2, '0')
    val timeText = "$hourStr:$minStr"

    val cal = java.util.Calendar.getInstance()
    val fullDayName = java.text.SimpleDateFormat("EEEE", java.util.Locale.ENGLISH).format(cal.time).lowercase()
    val fullMonthName = java.text.SimpleDateFormat("MMMM d", java.util.Locale.ENGLISH).format(cal.time).uppercase()

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

    var scriptTextSize = cardRect.height() * 0.32f
    val scriptPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = scriptTextSize
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

// 9. SPLIT FLAP HYBRID (4x2 / Centered Dual Flip Cards & Tight Date Stack)
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

    val timeState = DigitalClockTimeState.now()
    val hourStr = timeState.hour24.padStart(2, '0')
    val minStr = timeState.minute.padStart(2, '0')

    val cal = java.util.Calendar.getInstance()
    val dateStr = java.text.SimpleDateFormat("EEE, d MMM", java.util.Locale.ENGLISH).format(cal.time)

    val tileGapX = cardRect.width() * 0.05f
    val usableW = cardRect.width() - (padX * 2f)
    val maxTileW = (usableW - tileGapX) / 2f

    val usableH = cardRect.height() - (padY * 2f)
    val maxTileH = usableH * 0.72f

    val tileW = minOf(maxTileW, maxTileH * 1.05f)
    val tileH = minOf(maxTileH, tileW / 1.05f)

    val totalTilesW = (tileW * 2f) + tileGapX
    val tileStartX = cardRect.centerX() - (totalTilesW / 2f)

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

    val gapY = (tileH * 0.16f).coerceIn(8f * density, 18f * density)

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

        val midY = tileRect.centerY()
        canvas.drawLine(tileRect.left, midY, tileRect.right, midY, splitLinePaint)

        val pinW = 4.5f * density
        val pinH = 3f * density
        val leftPin = RectF(tileRect.left, midY - (pinH / 2f), tileRect.left + pinW, midY + (pinH / 2f))
        val rightPin = RectF(tileRect.right - pinW, midY - (pinH / 2f), tileRect.right, midY + (pinH / 2f))
        canvas.drawRoundRect(leftPin, 1f * density, 1f * density, pinPaint)
        canvas.drawRoundRect(rightPin, 1f * density, 1f * density, pinPaint)
    }

    val hourTileRect = RectF(tileStartX, tileTopY, tileStartX + tileW, tileTopY + tileH)
    val minTileRect = RectF(hourTileRect.right + tileGapX, tileTopY, hourTileRect.right + tileGapX + tileW, tileTopY + tileH)

    drawFlipCard(hourTileRect, hourStr)
    drawFlipCard(minTileRect, minStr)

    canvas.drawText(dateStr, cardRect.centerX(), dateY, datePaint)

    return bitmap
}

// 10. VERTICAL CAPSULE HYBRID (1x2 / Unified Pill Capsule Monolith with Constant Gap)
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

    val cardRadius = cardRect.width() / 2f
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val padX = cardRect.width() * 0.10f
    val padY = cardRect.height() * 0.08f

    val timeState = DigitalClockTimeState.now()
    val hourStr = timeState.hour24.padStart(2, '0')
    val minStr = timeState.minute.padStart(2, '0')

    val cal = java.util.Calendar.getInstance()
    val dayWeekStr = java.text.SimpleDateFormat("EEE", java.util.Locale.ENGLISH).format(cal.time).uppercase()
    val dayNumStr = timeState.dayOfMonth
    val badgeText = "$dayWeekStr $dayNumStr"

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

    val badgeW = cardRect.width() - (padX * 2f)
    val badgeHeight = (timeTextSize * 0.42f).coerceIn(22f * density, 42f * density)
    val badgeRadius = badgeHeight / 2f

    val gapY = (cardRect.width() * 0.08f).coerceIn(6f * density, 16f * density)

    val totalStackH = digitH + gapY + badgeHeight + gapY + digitH
    val stackTopY = cardRect.centerY() - (totalStackH / 2f)

    val hourY = stackTopY + digitH - timeBounds.bottom
    val badgeTop = stackTopY + digitH + gapY
    val minY = badgeTop + badgeHeight + gapY + digitH - timeBounds.bottom

    canvas.drawText(hourStr, cardRect.centerX(), hourY, timePaint)

    val badgeLeft = cardRect.centerX() - (badgeW / 2f)
    val badgeRect = RectF(badgeLeft, badgeTop, badgeLeft + badgeW, badgeTop + badgeHeight)

    val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(badgeRect, badgeRadius, badgeRadius, badgeBgPaint)

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
    canvas.drawText(minStr, cardRect.centerX(), minY, timePaint)

    return bitmap
}

// 11. PILL CAPSULE HYBRID (1x2 Fixed / Bold Arc-Matched Dial, Digital Time, Date Badge & Vintage Motif)
fun generatePillCapsuleHybridClockBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean, // Kept for signature compatibility
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
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#80FFFFFF")

    // Enforce Fixed 1:2 Target Aspect Ratio
    val targetRatio = 0.5f
    var cardW = w.toFloat()
    var cardH = cardW / targetRatio

    if (cardH > h.toFloat()) {
        cardH = h.toFloat()
        cardW = cardH * targetRatio
    }

    val leftX = (w - cardW) / 2f
    val topY = (h - cardH) / 2f
    val cardRect = RectF(leftX, topY, leftX + cardW, topY + cardH)

    // Outer Pill Capsule Radius
    val cardRadius = cardRect.width() / 2f
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val timeState = HybridClockTimeState.now()
    val hourStr = timeState.hour24.toString().padStart(2, '0')
    val minStr = timeState.minute.toString().padStart(2, '0')
    val digitalTimeStr = "$hourStr:$minStr"
    val dateStr = "${timeState.dayOfWeek} ${timeState.dayOfMonth}"

    // =========================================================================
    // 1. TOP SECTION: ANALOG DIAL MATCHED TO TOP ARC
    // =========================================================================
    val dialCenterX = cardRect.centerX()
    val dialCenterY = cardRect.top + cardRadius

    val dialInset = cardW * 0.08f
    val dialRadius = cardRadius - dialInset

    // Outer Circle Ring
    val outerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#30000000") else Color.parseColor("#40FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 2.0f * density
    }
    canvas.drawCircle(dialCenterX, dialCenterY, dialRadius, outerCirclePaint)

    // Major Ticks & Minor Dots
    val majorTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * density
        strokeCap = Paint.Cap.ROUND
    }
    val minorDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        style = Paint.Style.FILL
    }

    for (i in 0 until 12) {
        val angleRad = Math.toRadians((i * 30).toDouble())
        val cos = Math.cos(angleRad).toFloat()
        val sin = Math.sin(angleRad).toFloat()

        if (i % 3 == 0) {
            val innerR = dialRadius * 0.74f
            val outerR = dialRadius * 0.90f
            canvas.drawLine(
                dialCenterX + cos * innerR,
                dialCenterY + sin * innerR,
                dialCenterX + cos * outerR,
                dialCenterY + sin * outerR,
                majorTickPaint
            )
        } else {
            val dotR = dialRadius * 0.82f
            canvas.drawCircle(
                dialCenterX + cos * dotR,
                dialCenterY + sin * dotR,
                1.4f * density,
                minorDotPaint
            )
        }
    }

    // Hands
    val hourAngleRad = Math.toRadians(((timeState.hour12 % 12 + timeState.minute / 60f) * 30f - 90f).toDouble())
    val minAngleRad = Math.toRadians(((timeState.minute + timeState.second / 60f) * 6f - 90f).toDouble())

    val hourHandLen = dialRadius * 0.48f
    val hourHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = 3.2f * density
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        dialCenterX, dialCenterY,
        dialCenterX + (Math.cos(hourAngleRad) * hourHandLen).toFloat(),
        dialCenterY + (Math.sin(hourAngleRad) * hourHandLen).toFloat(),
        hourHandPaint
    )

    val minHandLen = dialRadius * 0.74f
    val minHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * density
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        dialCenterX, dialCenterY,
        dialCenterX + (Math.cos(minAngleRad) * minHandLen).toFloat(),
        dialCenterY + (Math.sin(minAngleRad) * minHandLen).toFloat(),
        minHandPaint
    )

    val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.FILL
    }
    canvas.drawCircle(dialCenterX, dialCenterY, 3.0f * density, capPaint)

    // =========================================================================
    // 2. MIDDLE SECTION: DIGITAL TIME & DATE BADGE
    // =========================================================================
    val timeMaxW = cardW * 0.82f
    val refTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 700)
        textSize = 100f
    }
    val refTimeW = refTimePaint.measureText(digitalTimeStr)
    val timeTextSize = minOf(cardW * 0.28f, 100f * (timeMaxW / refTimeW))

    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = timeTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }
    val timeBounds = Rect()
    timePaint.getTextBounds(digitalTimeStr, 0, digitalTimeStr.length, timeBounds)
    val digitH = timeBounds.height().toFloat()

    // Date Badge Dimensions
    val badgeW = cardW * 0.76f
    val badgeH = (cardW * 0.17f).coerceIn(22f * density, 40f * density)
    val badgeRadius = badgeH / 2f

    val gap1 = cardW * 0.13f
    val gap2 = cardW * 0.075f

    val dialBottom = dialCenterY + dialRadius
    val digitalTimeY = dialBottom + gap1 + digitH - timeBounds.bottom
    val badgeTop = dialBottom + gap1 + digitH + gap2

    // Draw Digital Time
    canvas.drawText(digitalTimeStr, cardRect.centerX(), digitalTimeY, timePaint)

    // Draw Date Badge
    val badgeRect = RectF(cardRect.centerX() - (badgeW / 2f), badgeTop, cardRect.centerX() + (badgeW / 2f), badgeTop + badgeH)

    val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(badgeRect, badgeRadius, badgeRadius, badgeBgPaint)

    val r = Color.red(accentColorInt) / 255f
    val g = Color.green(accentColorInt) / 255f
    val b = Color.blue(accentColorInt) / 255f
    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
    val badgeTextColor = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

    var badgeTextSize = badgeH * 0.48f
    val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = badgeTextColor
        textSize = badgeTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.06f
    }

    if (badgeTextPaint.measureText(dateStr) > badgeRect.width() * 0.82f) {
        badgeTextSize *= ((badgeRect.width() * 0.82f) / badgeTextPaint.measureText(dateStr))
        badgeTextPaint.textSize = badgeTextSize
    }

    val badgeBounds = Rect()
    badgeTextPaint.getTextBounds(dateStr, 0, dateStr.length, badgeBounds)
    val badgeTextY = badgeRect.centerY() + (badgeBounds.height() / 2f) - (1f * density)

    canvas.drawText(dateStr, badgeRect.centerX(), badgeTextY, badgeTextPaint)

    // =========================================================================
    // 3. BOTTOM SECTION: SYMMETRICAL VINTAGE LINE MOTIF
    // =========================================================================
    val motifCenterY = badgeTop + badgeH + ((cardRect.bottom - (badgeTop + badgeH)) / 2f)
    val motifW = cardW * 0.52f
    val motifH = motifW * 0.32f
    val cx = cardRect.centerX()

    val motifStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#35000000") else Color.parseColor("#40FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
        strokeCap = Paint.Cap.ROUND
    }

    val motifFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#35000000") else Color.parseColor("#40FFFFFF")
        style = Paint.Style.FILL
    }

    // Central Diamond Emblem
    val diamondSize = 4f * density
    val diamondPath = Path().apply {
        moveTo(cx, motifCenterY - diamondSize)
        lineTo(cx + diamondSize, motifCenterY)
        lineTo(cx, motifCenterY + diamondSize)
        lineTo(cx - diamondSize, motifCenterY)
        close()
    }
    canvas.drawPath(diamondPath, motifFillPaint)

    // Top & Bottom Vertical Accent Dots
    canvas.drawCircle(cx, motifCenterY - (8f * density), 1.2f * density, motifFillPaint)
    canvas.drawCircle(cx, motifCenterY + (8f * density), 1.2f * density, motifFillPaint)

    // Symmetrical Curlicue Scrollwork Wings
    val leftWing = Path().apply {
        moveTo(cx - (6f * density), motifCenterY)
        cubicTo(
            cx - (motifW * 0.22f), motifCenterY - (motifH * 0.45f),
            cx - (motifW * 0.38f), motifCenterY + (motifH * 0.50f),
            cx - (motifW * 0.48f), motifCenterY - (motifH * 0.15f)
        )
    }

    val rightWing = Path().apply {
        moveTo(cx + (6f * density), motifCenterY)
        cubicTo(
            cx + (motifW * 0.22f), motifCenterY - (motifH * 0.45f),
            cx + (motifW * 0.38f), motifCenterY + (motifH * 0.50f),
            cx + (motifW * 0.48f), motifCenterY - (motifH * 0.15f)
        )
    }

    canvas.drawPath(leftWing, motifStrokePaint)
    canvas.drawPath(rightWing, motifStrokePaint)

    // Wing Terminal Dots
    canvas.drawCircle(cx - (motifW * 0.48f), motifCenterY - (motifH * 0.15f), 1.4f * density, motifFillPaint)
    canvas.drawCircle(cx + (motifW * 0.48f), motifCenterY - (motifH * 0.15f), 1.4f * density, motifFillPaint)

    // Subymmetrical Bottom Parallel Arc
    val arcBounds = RectF(
        cx - (motifW * 0.30f),
        motifCenterY + (2f * density),
        cx + (motifW * 0.30f),
        motifCenterY + (motifH * 0.65f)
    )
    canvas.drawArc(arcBounds, 25f, 130f, false, motifStrokePaint)

    return bitmap
}

// 12. OVERLAPPING TYPOGRAPHIC HYBRID (2x2 Square / Circular Watch Face with Custom Controls)
fun generateOverlappingTypographicHybridClockBitmap(
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
    val density = displayDensity

    val size = minOf(w, h).toFloat()
    val cx = w / 2f
    val cy = h / 2f
    val circleRadius = size / 2f

    // 1. CIRCULAR BOUNDARY & CANVAS CLIPPING
    val circlePath = Path().apply {
        addCircle(cx, cy, circleRadius, Path.Direction.CW)
    }

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawPath(circlePath, bgPaint)

    canvas.save()
    canvas.clipPath(circlePath)

    val faceBgColor = if (isLight) Color.parseColor("#EFEFEF") else Color.parseColor("#0A0A0C")
    val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = faceBgColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, circleRadius, facePaint)

    // =========================================================================
    // USER CONTROLS & MANUAL CONFIGURATION
    // =========================================================================
    val hourX = cx - circleRadius * 0.14f          // Top hours horizontal position
    val hourY = cy - circleRadius * 0.05f          // Top hours vertical baseline
    val minX  = cx - circleRadius * 0.84f          // Bottom minutes horizontal position
    val minY  = cy + circleRadius * 0.88f          // Bottom minutes vertical baseline
    val digitGap = circleRadius * 0.48f            // Spacing between digits

    val dateX = cx - circleRadius * 0.65f          // Top-Left date horizontal position
    val dateY = cy - circleRadius * 0.48f          // Top-Left date vertical position

    val knockoutStrokeWidth = circleRadius * 0.14f // Vertical cutout gap between top and bottom rows
    val digitStrokeWidth = circleRadius * 0.06f    // Cutout stroke separating overlapping digits
    val handOutlineWidth = 1.5f * density          // Background outline stroke around hands

    val dimDigitAlpha = 155                        // Opacity for 1st digits (0..255)
    val fullDigitAlpha = 255                       // Opacity for 2nd digits (0..255)
    // =========================================================================

    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE

    val r = Color.red(accentColorInt)
    val g = Color.green(accentColorInt)
    val b = Color.blue(accentColorInt)
    val dimAccentColor = Color.argb(dimDigitAlpha, r, g, b)
    val fullAccentColor = Color.argb(fullDigitAlpha, r, g, b)

    // 2. OVERLAPPING DIGITS CALCULATIONS & PAINTS
    val timeState = HybridClockTimeState.now()
    val hourStr = timeState.hour24.toString().padStart(2, '0')
    val minStr = timeState.minute.toString().padStart(2, '0')

    val displayTypeface = getSlateFont(context, weight = 900)
    val numTextSize = circleRadius * 1.12f

    // Fill Paints
    val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dimAccentColor
        textSize = numTextSize
        typeface = displayTypeface
        textAlign = Paint.Align.LEFT
    }

    val fullPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fullAccentColor
        textSize = numTextSize
        typeface = displayTypeface
        textAlign = Paint.Align.LEFT
    }

    // Digit Cutout Stroke Paint (Separates overlapping digits in the same row)
    val digitCutoutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = faceBgColor
        textSize = numTextSize
        typeface = displayTypeface
        textAlign = Paint.Align.LEFT
        style = Paint.Style.STROKE
        strokeWidth = digitStrokeWidth
        strokeJoin = Paint.Join.ROUND
    }

    // Row Cutout Stroke Paint (Separates top row from bottom row)
    val rowCutoutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = faceBgColor
        textSize = numTextSize
        typeface = displayTypeface
        textAlign = Paint.Align.LEFT
        style = Paint.Style.STROKE
        strokeWidth = knockoutStrokeWidth
        strokeJoin = Paint.Join.ROUND
    }

    val h1 = hourStr[0].toString()
    val h2 = hourStr[1].toString()
    val m1 = minStr[0].toString()
    val m2 = minStr[1].toString()

    // --- STEP 1: BOTTOM ROW MINUTES ---
    canvas.drawText(m1, minX, minY, dimPaint)
    canvas.drawText(m2, minX + digitGap, minY, digitCutoutPaint)
    canvas.drawText(m2, minX + digitGap, minY, fullPaint)

    // --- STEP 2: TOP ROW KNOCKOUT OVER BOTTOM ROW ---
    canvas.drawText(h1, hourX, hourY, rowCutoutPaint)
    canvas.drawText(h2, hourX + digitGap, hourY, rowCutoutPaint)

    // --- STEP 3: TOP ROW HOURS ---
    canvas.drawText(h1, hourX, hourY, dimPaint)
    canvas.drawText(h2, hourX + digitGap, hourY, digitCutoutPaint)
    canvas.drawText(h2, hourX + digitGap, hourY, fullPaint)

    // 3. TOP-LEFT DAY & MONTH OVERLAY
    val dateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = circleRadius * 0.14f
        typeface = getSlateFont(context, weight = 600)
        textAlign = Paint.Align.LEFT
    }

    val dateKnockoutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = faceBgColor
        textSize = circleRadius * 0.14f
        typeface = getSlateFont(context, weight = 600)
        textAlign = Paint.Align.LEFT
        style = Paint.Style.STROKE
        strokeWidth = circleRadius * 0.04f
        strokeJoin = Paint.Join.ROUND
    }

    val dayStr = timeState.dayOfWeek
    val monthStr = "${timeState.monthName} ${timeState.dayOfMonth}"
    val dateLineHeight = circleRadius * 0.15f

    canvas.drawText(dayStr, dateX, dateY, dateKnockoutPaint)
    canvas.drawText(dayStr, dateX, dateY, dateTextPaint)

    canvas.drawText(monthStr, dateX, dateY + dateLineHeight, dateKnockoutPaint)
    canvas.drawText(monthStr, dateX, dateY + dateLineHeight, dateTextPaint)

    // 4. ANALOG HANDS WITH BACKGROUND OUTLINE STROKES
    val hourAngleRad = Math.toRadians(((timeState.hour12 % 12 + timeState.minute / 60f) * 30f - 90f).toDouble())
    val minAngleRad = Math.toRadians(((timeState.minute + timeState.second / 60f) * 6f - 90f).toDouble())
    val secAngleRad = Math.toRadians(((timeState.second) * 6f - 90f).toDouble())

    val hourHandLen = circleRadius * 0.52f
    val hourHandWidth = circleRadius * 0.042f

    val minHandLen = circleRadius * 0.80f
    val minHandWidth = circleRadius * 0.032f

    val secHandLen = circleRadius * 0.88f
    val secHandWidth = circleRadius * 0.014f

    fun drawHandWithOutline(angleRad: Double, length: Float, strokeWidth: Float, colorInt: Int) {
        val stopX = cx + (Math.cos(angleRad) * length).toFloat()
        val stopY = cy + (Math.sin(angleRad) * length).toFloat()

        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = faceBgColor
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth + (handOutlineWidth * 2f)
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(cx, cy, stopX, stopY, outlinePaint)

        val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInt
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(cx, cy, stopX, stopY, handPaint)
    }

    drawHandWithOutline(hourAngleRad, hourHandLen, hourHandWidth, primaryText)
    drawHandWithOutline(minAngleRad, minHandLen, minHandWidth, primaryText)
    drawHandWithOutline(secAngleRad, secHandLen, secHandWidth, fullAccentColor)

    // Center Cap Dot
    val capOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = faceBgColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, circleRadius * 0.052f, capOutlinePaint)

    val capOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, circleRadius * 0.040f, capOuter)

    val capInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fullAccentColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, circleRadius * 0.020f, capInner)

    canvas.restore()

    return bitmap
}

