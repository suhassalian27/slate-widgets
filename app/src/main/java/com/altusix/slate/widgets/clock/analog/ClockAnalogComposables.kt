package com.altusix.slate.widgets.clock.analog

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.altusix.slate.data.local.SlateWidgetConfig
import java.util.Calendar

private fun getSafeBgColor(config: SlateWidgetConfig): Int {
    return try {
        config.backgroundColorHex.toInt() or 0xFF000000.toInt()
    } catch (_: Exception) {
        if (config.themeMode == "LIGHT") Color.WHITE else Color.parseColor("#161618")
    }
}

// 1. ANALOG PRECISION DIAL (2x2 Square / Minimalist Architectural Watch Face)
fun generateAnalogPrecisionClockBitmap(
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
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#757575") else Color.parseColor("#9E9E9E")

    val size = minOf(w, h).toFloat()
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawOval(cardRect, bgPaint)

    val clockCx = cardRect.centerX()
    val clockCy = cardRect.centerY()
    val clockRadius = (size / 2f) - (12f * density)

    val timeState = AnalogClockTimeState.now()

    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    for (i in 0 until 12) {
        val angleRad = Math.toRadians((i * 30f - 90f).toDouble())
        val isCardinal = (i % 3 == 0)
        val outerR = clockRadius
        val innerR = if (isCardinal) clockRadius - (12f * density) else clockRadius - (7f * density)

        val x1 = (clockCx + innerR * Math.cos(angleRad)).toFloat()
        val y1 = (clockCy + innerR * Math.sin(angleRad)).toFloat()
        val x2 = (clockCx + outerR * Math.cos(angleRad)).toFloat()
        val y2 = (clockCy + outerR * Math.sin(angleRad)).toFloat()

        tickPaint.strokeWidth = if (isCardinal) 2.4f * density else 1.4f * density
        tickPaint.color = if (isCardinal) primaryText else secondaryText
        canvas.drawLine(x1, y1, x2, y2, tickPaint)
    }

    val hourLength = clockRadius * 0.52f
    val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = 3.8f * density
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        clockCx, clockCy,
        (clockCx + hourLength * Math.cos(timeState.hourAngleRad)).toFloat(),
        (clockCy + hourLength * Math.sin(timeState.hourAngleRad)).toFloat(),
        hourPaint
    )

    val minLength = clockRadius * 0.82f
    val minPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * density
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        clockCx, clockCy,
        (clockCx + minLength * Math.cos(timeState.minuteAngleRad)).toFloat(),
        (clockCy + minLength * Math.sin(timeState.minuteAngleRad)).toFloat(),
        minPaint
    )

    val secLength = clockRadius * 0.90f
    val secTailLength = clockRadius * 0.18f
    val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        (clockCx - secTailLength * Math.cos(timeState.secondAngleRad)).toFloat(),
        (clockCy - secTailLength * Math.sin(timeState.secondAngleRad)).toFloat(),
        (clockCx + secLength * Math.cos(timeState.secondAngleRad)).toFloat(),
        (clockCy + secLength * Math.sin(timeState.secondAngleRad)).toFloat(),
        secPaint
    )

    val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawCircle(clockCx, clockCy, 3.5f * density, hubPaint)

    val hubCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(clockCx, clockCy, 1.8f * density, hubCenterPaint)

    return bitmap
}

// 2. BAUHAUS GEOMETRIC DIAL (2x2 Square / Bold Architectural Edges)
fun generateBauhausClockBitmap(
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
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#636366")

    // Fixed 1:1 Square layout container
    val size = minOf(w, h).toFloat()
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    val cardRadius = 22f * density
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val clockCx = cardRect.centerX()
    val clockCy = cardRect.centerY()
    val maxRadius = (size / 2f)

    val timeState = AnalogClockTimeState.now()

    // 1. Cardinal Numbers Pushed to Outer Boundary Edges
    val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = 16f * density
        typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val numOffset = maxRadius - (22f * density) // Anchors numbers close to the container boundary
    val fm = numPaint.fontMetrics
    val textYCenter = -((fm.descent + fm.ascent) / 2f)

    // Render 12, 03, 06, 09
    canvas.drawText("12", clockCx, clockCy - numOffset + textYCenter, numPaint)
    canvas.drawText("03", clockCx + numOffset, clockCy + textYCenter, numPaint)
    canvas.drawText("06", clockCx, clockCy + numOffset + textYCenter, numPaint)
    canvas.drawText("09", clockCx - numOffset, clockCy + textYCenter, numPaint)

    // 2. Minor Hour Dots Along Outer Perimeter
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        style = Paint.Style.FILL
    }
    for (i in 0 until 12) {
        if (i % 3 != 0) {
            val angleRad = Math.toRadians((i * 30f - 90f).toDouble())
            val dx = (clockCx + numOffset * Math.cos(angleRad)).toFloat()
            val dy = (clockCy + numOffset * Math.sin(angleRad)).toFloat()
            canvas.drawCircle(dx, dy, 2.2f * density, dotPaint)
        }
    }

    // 3. Hands Rescaled for Expanded Outer Boundaries
    val clockRadius = maxRadius - (14f * density)

    // Hour Hand
    val hourLen = clockRadius * 0.42f
    val hourHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = 4.5f * density
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        clockCx, clockCy,
        (clockCx + hourLen * Math.cos(timeState.hourAngleRad)).toFloat(),
        (clockCy + hourLen * Math.sin(timeState.hourAngleRad)).toFloat(),
        hourHandPaint
    )

    // Minute Hand
    val minLen = clockRadius * 0.72f
    val minHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = 2.4f * density
        strokeCap = Paint.Cap.ROUND
    }
    val minTipX = (clockCx + minLen * Math.cos(timeState.minuteAngleRad)).toFloat()
    val minTipY = (clockCy + minLen * Math.sin(timeState.minuteAngleRad)).toFloat()
    canvas.drawLine(clockCx, clockCy, minTipX, minTipY, minHandPaint)

    // Accent Counterweight Second Hand
    val secLen = clockRadius * 0.82f
    val secTailLen = clockRadius * 0.24f
    val secHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = 1.4f * density
        strokeCap = Paint.Cap.ROUND
    }
    val secTipX = (clockCx + secLen * Math.cos(timeState.secondAngleRad)).toFloat()
    val secTipY = (clockCy + secLen * Math.sin(timeState.secondAngleRad)).toFloat()
    val secTailX = (clockCx - secTailLen * Math.cos(timeState.secondAngleRad)).toFloat()
    val secTailY = (clockCy - secTailLen * Math.sin(timeState.secondAngleRad)).toFloat()

    canvas.drawLine(secTailX, secTailY, secTipX, secTipY, secHandPaint)

    // Counterweight Disc on Second Hand
    val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawCircle(secTailX, secTailY, 4.5f * density, discPaint)

    // Center Pivot Hub
    val pivotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(clockCx, clockCy, 3.2f * density, discPaint)
    canvas.drawCircle(clockCx, clockCy, 1.6f * density, pivotPaint)

    return bitmap
}

// 3. CHRONO ARCHITECT DUAL DIAL (4x2 / Precision Sub-Dials & Date Window)
fun generateChronoArchitectClockBitmap(
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
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#757575") else Color.parseColor("#9E9E9E")
    val cardBgTrack = if (isLight) Color.parseColor("#0F000000") else Color.parseColor("#18FFFFFF")

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

    val cardRadius = 22f * density
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val padX = cardRect.width() * 0.06f
    val padY = cardRect.height() * 0.08f

    val leftWidth = cardRect.width() * 0.50f
    val clockCx = cardRect.left + padX + (leftWidth - padX) / 2f
    val clockCy = cardRect.centerY()
    val mainRadius = (minOf(cardRect.height() - (padY * 2f), leftWidth)) / 2f - (2f * density)

    val timeState = AnalogClockTimeState.now()

    val mainRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cardBgTrack
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * density
    }
    canvas.drawCircle(clockCx, clockCy, mainRadius, mainRingPaint)

    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    for (i in 0 until 60) {
        val angleRad = Math.toRadians((i * 6f - 90f).toDouble())
        val isHour = (i % 5 == 0)
        val outerR = mainRadius - (2f * density)
        val innerR = if (isHour) outerR - (7f * density) else outerR - (3.5f * density)

        val x1 = (clockCx + innerR * Math.cos(angleRad)).toFloat()
        val y1 = (clockCy + innerR * Math.sin(angleRad)).toFloat()
        val x2 = (clockCx + outerR * Math.cos(angleRad)).toFloat()
        val y2 = (clockCy + outerR * Math.sin(angleRad)).toFloat()

        tickPaint.strokeWidth = if (isHour) 2.0f * density else 1.0f * density
        tickPaint.color = if (isHour) primaryText else secondaryText
        canvas.drawLine(x1, y1, x2, y2, tickPaint)
    }

    val hourLen = mainRadius * 0.50f
    val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = 3.6f * density
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        clockCx, clockCy,
        (clockCx + hourLen * Math.cos(timeState.hourAngleRad)).toFloat(),
        (clockCy + hourLen * Math.sin(timeState.hourAngleRad)).toFloat(),
        hourPaint
    )

    val minLen = mainRadius * 0.78f
    val minPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * density
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        clockCx, clockCy,
        (clockCx + minLen * Math.cos(timeState.minuteAngleRad)).toFloat(),
        (clockCy + minLen * Math.sin(timeState.minuteAngleRad)).toFloat(),
        minPaint
    )

    val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.FILL
    }
    canvas.drawCircle(clockCx, clockCy, 3f * density, hubPaint)

    val rightLeftX = cardRect.left + leftWidth + (cardRect.width() * 0.02f)
    val rightWidth = cardRect.right - rightLeftX - padX

    val subRadius = (cardRect.height() - (padY * 2f)) * 0.22f
    val sub1Cx = rightLeftX + (rightWidth * 0.30f)
    val sub1Cy = cardRect.top + padY + subRadius + (2f * density)

    val sub2Cx = rightLeftX + (rightWidth * 0.30f)
    val sub2Cy = cardRect.bottom - padY - subRadius - (2f * density)

    val subBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cardBgTrack
        style = Paint.Style.FILL
    }
    canvas.drawCircle(sub1Cx, sub1Cy, subRadius, subBgPaint)
    canvas.drawCircle(sub2Cx, sub2Cy, subRadius, subBgPaint)

    val secSubHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = 1.4f * density
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        sub1Cx, sub1Cy,
        (sub1Cx + (subRadius * 0.8f) * Math.cos(timeState.secondAngleRad)).toFloat(),
        (sub1Cy + (subRadius * 0.8f) * Math.sin(timeState.secondAngleRad)).toFloat(),
        secSubHandPaint
    )

    val cal = Calendar.getInstance()
    val hour24 = cal.get(Calendar.HOUR_OF_DAY)
    val hour24AngleRad = Math.toRadians(((hour24 / 24f) * 360f - 90f).toDouble())
    val dayNightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = 1.6f * density
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        sub2Cx, sub2Cy,
        (sub2Cx + (subRadius * 0.75f) * Math.cos(hour24AngleRad)).toFloat(),
        (sub2Cy + (subRadius * 0.75f) * Math.sin(hour24AngleRad)).toFloat(),
        dayNightPaint
    )

    val subPivotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawCircle(sub1Cx, sub1Cy, 2f * density, subPivotPaint)
    canvas.drawCircle(sub2Cx, sub2Cy, 2f * density, dayNightPaint)

    val dateBoxLeft = rightLeftX + (rightWidth * 0.60f)
    val dateBoxTop = cardRect.centerY() - (14f * density)
    val dateBoxRight = cardRect.right - padX
    val dateBoxBottom = cardRect.centerY() + (14f * density)
    val dateRect = RectF(dateBoxLeft, dateBoxTop, dateBoxRight, dateBoxBottom)

    val dateBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(dateRect, 6f * density, 6f * density, dateBgPaint)

    val monthShort = when (cal.get(Calendar.MONTH)) {
        Calendar.JANUARY -> "JAN"
        Calendar.FEBRUARY -> "FEB"
        Calendar.MARCH -> "MAR"
        Calendar.APRIL -> "APR"
        Calendar.MAY -> "MAY"
        Calendar.JUNE -> "JUN"
        Calendar.JULY -> "JUL"
        Calendar.AUGUST -> "AUG"
        Calendar.SEPTEMBER -> "SEP"
        Calendar.OCTOBER -> "OCT"
        Calendar.NOVEMBER -> "NOV"
        else -> "DEC"
    }
    val dateStr = "${cal.get(Calendar.DAY_OF_MONTH)} $monthShort"

    val r = Color.red(accentColorInt)
    val g = Color.green(accentColorInt)
    val b = Color.blue(accentColorInt)
    val badgeLuminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    val activeTextColor = if (badgeLuminance > 0.65) Color.parseColor("#161618") else Color.WHITE

    val dateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = activeTextColor
        textSize = 10.5f * density
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val fmDate = dateTextPaint.fontMetrics
    val dateY = dateRect.centerY() - ((fmDate.descent + fmDate.ascent) / 2f)
    canvas.drawText(dateStr, dateRect.centerX(), dateY, dateTextPaint)

    return bitmap
}

// 4. CYBER SKELETON RING DIAL (2x2 Square / Precision Crosshair)
fun generateCyberSkeletonClockBitmap(
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
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#757575") else Color.parseColor("#9E9E9E")

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val size = minOf(w, h).toFloat()
        val leftX = (w - size) / 2f
        val topY = (h - size) / 2f
        RectF(leftX, topY, leftX + size, topY + size)
    }

    val cardRadius = 22f * density
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val clockCx = cardRect.centerX()
    val clockCy = cardRect.centerY()
    val clockRadius = (minOf(cardRect.width(), cardRect.height()) / 2f) - (12f * density)

    val timeState = AnalogClockTimeState.now()

    val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#15000000") else Color.parseColor("#20FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }
    canvas.drawLine(clockCx - clockRadius, clockCy, clockCx + clockRadius, clockCy, crosshairPaint)
    canvas.drawLine(clockCx, clockCy - clockRadius, clockCx, clockCy + clockRadius, crosshairPaint)

    val ringTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#1F000000") else Color.parseColor("#2AFFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
    }
    canvas.drawCircle(clockCx, clockCy, clockRadius, ringTrackPaint)
    canvas.drawCircle(clockCx, clockCy, clockRadius * 0.65f, ringTrackPaint)

    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    for (i in 0 until 60) {
        val angleRad = Math.toRadians((i * 6f - 90f).toDouble())
        val isMajor = (i % 5 == 0)
        val outerR = clockRadius
        val innerR = if (isMajor) outerR - (8f * density) else outerR - (4f * density)

        val x1 = (clockCx + innerR * Math.cos(angleRad)).toFloat()
        val y1 = (clockCy + innerR * Math.sin(angleRad)).toFloat()
        val x2 = (clockCx + outerR * Math.cos(angleRad)).toFloat()
        val y2 = (clockCy + outerR * Math.sin(angleRad)).toFloat()

        tickPaint.strokeWidth = if (isMajor) 2f * density else 1f * density
        tickPaint.color = if (isMajor) accentColorInt else secondaryText
        canvas.drawLine(x1, y1, x2, y2, tickPaint)
    }

    val hourLen = clockRadius * 0.52f
    val hourHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = 3.5f * density
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        clockCx, clockCy,
        (clockCx + hourLen * Math.cos(timeState.hourAngleRad)).toFloat(),
        (clockCy + hourLen * Math.sin(timeState.hourAngleRad)).toFloat(),
        hourHandPaint
    )

    val minLen = clockRadius * 0.82f
    val minHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        clockCx, clockCy,
        (clockCx + minLen * Math.cos(timeState.minuteAngleRad)).toFloat(),
        (clockCy + minLen * Math.sin(timeState.minuteAngleRad)).toFloat(),
        minHandPaint
    )

    val secLen = clockRadius * 0.88f
    val secTailLen = clockRadius * 0.20f
    val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        (clockCx - secTailLen * Math.cos(timeState.secondAngleRad)).toFloat(),
        (clockCy - secTailLen * Math.sin(timeState.secondAngleRad)).toFloat(),
        (clockCx + secLen * Math.cos(timeState.secondAngleRad)).toFloat(),
        (clockCy + secLen * Math.sin(timeState.secondAngleRad)).toFloat(),
        secPaint
    )

    val hubRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    canvas.drawCircle(clockCx, clockCy, 4f * density, hubRingPaint)

    val hubDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.FILL
    }
    canvas.drawCircle(clockCx, clockCy, 1.5f * density, hubDotPaint)

    return bitmap
}

// 5. HORIZON OFFSET ANALOG (4x2 / Off-Center Dial)
fun generateHorizonOffsetClockBitmap(
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
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#757575") else Color.parseColor("#9E9E9E")

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

    val cardRadius = 22f * density
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val padX = cardRect.width() * 0.06f
    val padY = cardRect.height() * 0.08f

    val leftWidth = cardRect.width() * 0.40f
    val clockCx = cardRect.left + padX + (leftWidth - padX) / 2f
    val clockCy = cardRect.centerY()
    val clockRadius = (minOf(cardRect.height() - (padY * 2f), leftWidth)) / 2f - (2f * density)

    val timeState = AnalogClockTimeState.now()

    val dialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#0A000000") else Color.parseColor("#14FFFFFF")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(clockCx, clockCy, clockRadius, dialPaint)

    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    for (i in 0 until 12) {
        val angleRad = Math.toRadians((i * 30f - 90f).toDouble())
        val outerR = clockRadius - (2f * density)
        val innerR = outerR - (5f * density)

        val x1 = (clockCx + innerR * Math.cos(angleRad)).toFloat()
        val y1 = (clockCy + innerR * Math.sin(angleRad)).toFloat()
        val x2 = (clockCx + outerR * Math.cos(angleRad)).toFloat()
        val y2 = (clockCy + outerR * Math.sin(angleRad)).toFloat()

        tickPaint.strokeWidth = 1.4f * density
        tickPaint.color = if (i % 3 == 0) primaryText else secondaryText
        canvas.drawLine(x1, y1, x2, y2, tickPaint)
    }

    val hourLen = clockRadius * 0.50f
    val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = 3.8f * density
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        clockCx, clockCy,
        (clockCx + hourLen * Math.cos(timeState.hourAngleRad)).toFloat(),
        (clockCy + hourLen * Math.sin(timeState.hourAngleRad)).toFloat(),
        hourPaint
    )

    val minLen = clockRadius * 0.80f
    val minPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = 2.0f * density
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        clockCx, clockCy,
        (clockCx + minLen * Math.cos(timeState.minuteAngleRad)).toFloat(),
        (clockCy + minLen * Math.sin(timeState.minuteAngleRad)).toFloat(),
        minPaint
    )

    val secLen = clockRadius * 0.86f
    val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        clockCx, clockCy,
        (clockCx + secLen * Math.cos(timeState.secondAngleRad)).toFloat(),
        (clockCy + secLen * Math.sin(timeState.secondAngleRad)).toFloat(),
        secPaint
    )

    val pivotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawCircle(clockCx, clockCy, 3f * density, pivotPaint)

    val rightLeftX = cardRect.left + leftWidth + (cardRect.width() * 0.05f)

    val cal = Calendar.getInstance()
    val monthFull = when (cal.get(Calendar.MONTH)) {
        Calendar.JANUARY -> "JANUARY"
        Calendar.FEBRUARY -> "FEBRUARY"
        Calendar.MARCH -> "MARCH"
        Calendar.APRIL -> "APRIL"
        Calendar.MAY -> "MAY"
        Calendar.JUNE -> "JUNE"
        Calendar.JULY -> "JULY"
        Calendar.AUGUST -> "AUGUST"
        Calendar.SEPTEMBER -> "SEPTEMBER"
        Calendar.OCTOBER -> "OCTOBER"
        Calendar.NOVEMBER -> "NOVEMBER"
        else -> "DECEMBER"
    }
    val yearStr = cal.get(Calendar.YEAR).toString()
    val dateDayStr = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')

    val dayOfWeekFull = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "MONDAY"
        Calendar.TUESDAY -> "TUESDAY"
        Calendar.WEDNESDAY -> "WEDNESDAY"
        Calendar.THURSDAY -> "THURSDAY"
        Calendar.FRIDAY -> "FRIDAY"
        Calendar.SATURDAY -> "SATURDAY"
        else -> "SUNDAY"
    }

    val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = 12f * density
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        textAlign = Paint.Align.LEFT
        letterSpacing = 0.08f
    }

    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = 42f * density
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }

    val dayTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = 10f * density
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textAlign = Paint.Align.LEFT
        letterSpacing = 0.10f
    }

    val fmM = monthPaint.fontMetrics
    val fmD = datePaint.fontMetrics
    val fmT = dayTagPaint.fontMetrics

    val hM = fmM.descent - fmM.ascent
    val hD = fmD.descent - fmD.ascent
    val hT = fmT.descent - fmT.ascent

    val gap1 = 2f * density
    val gap2 = 2f * density

    val totalHeight = hM + gap1 + hD + gap2 + hT
    var currentY = cardRect.centerY() - (totalHeight / 2f)

    val monthY = currentY - fmM.ascent
    canvas.drawText("$monthFull $yearStr", rightLeftX, monthY, monthPaint)
    currentY += hM + gap1

    val dateY = currentY - fmD.ascent
    canvas.drawText(dateDayStr, rightLeftX, dateY, datePaint)
    currentY += hD + gap2

    val dayY = currentY - fmT.ascent
    canvas.drawText(dayOfWeekFull, rightLeftX, dayY, dayTagPaint)

    return bitmap
}