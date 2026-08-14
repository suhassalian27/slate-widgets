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
import androidx.core.content.res.ResourcesCompat
import com.altusix.slate.R



private fun getSafeBgColor(config: SlateWidgetConfig): Int {
    return try {
        config.backgroundColorHex.toInt() or 0xFF000000.toInt()
    } catch (_: Exception) {
        if (config.themeMode == "LIGHT") Color.WHITE else Color.parseColor("#161618")
    }
}

private data class SquirclePoint(
    val x: Float,
    val y: Float
)

private fun getSquircleBoundaryPoint(
    angleRad: Double,
    halfW: Float,
    halfH: Float,
    cornerRadius: Float
): SquirclePoint {
    val u = Math.cos(angleRad).toFloat()
    val v = Math.sin(angleRad).toFloat()
    val absU = Math.abs(u).coerceAtLeast(0.0001f)
    val absV = Math.abs(v).coerceAtLeast(0.0001f)

    val tBox = minOf(halfW / absU, halfH / absV)
    val boxX = tBox * u
    val boxY = tBox * v

    val innerW = halfW - cornerRadius
    val innerH = halfH - cornerRadius

    return if (Math.abs(boxX) <= innerW || Math.abs(boxY) <= innerH) {
        SquirclePoint(boxX, boxY)
    } else {
        val cx = if (u >= 0) innerW else -innerW
        val cy = if (v >= 0) innerH else -innerH

        val b = u * cx + v * cy
        val k = cx * cx + cy * cy - cornerRadius * cornerRadius
        val discriminant = Math.max(0f, b * b - k)
        val t = b + Math.sqrt(discriminant.toDouble()).toFloat()

        SquirclePoint(t * u, t * v)
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

// 2. BAUHAUS GEOMETRIC DIAL (2x2 Square / Minimalist Radial Squircle Face)
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
    val secondaryText = if (isLight) Color.parseColor("#757575") else Color.parseColor("#8E8E93")

    // Fixed 1:1 Square layout container centered within bounds
    val size = minOf(w, h).toFloat()
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    val cardRadius = size * 0.18f
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val clockCx = cardRect.centerX()
    val clockCy = cardRect.centerY()

    // Fully proportional squircle boundary coordinates
    val margin = size * 0.06f
    val halfW = (size / 2f) - margin
    val halfH = (size / 2f) - margin
    val squircleRadius = cardRadius * 0.82f

    val timeState = AnalogClockTimeState.now()

    // 1. Render 60 Radial Micro-Ticks (Aesthetic Center-Aligned Lines)
    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    for (i in 0 until 60) {
        val angleRad = Math.toRadians(i * 6.0 - 90.0)
        val sqPoint = getSquircleBoundaryPoint(angleRad, halfW, halfH, squircleRadius)

        val x2 = clockCx + sqPoint.x
        val y2 = clockCy + sqPoint.y

        val isCardinal = (i % 15 == 0)
        val isHour = (i % 5 == 0)

        // Radial length pointing inward toward clock center
        val tickLen = when {
            isCardinal -> size * 0.12f
            isHour -> size * 0.085f
            else -> size * 0.035f
        }

        val rOuter = Math.hypot(sqPoint.x.toDouble(), sqPoint.y.toDouble()).toFloat()
        val rInner = (rOuter - tickLen).coerceAtLeast(0f)

        val cosA = Math.cos(angleRad).toFloat()
        val sinA = Math.sin(angleRad).toFloat()

        val x1 = clockCx + rInner * cosA
        val y1 = clockCy + rInner * sinA

        tickPaint.strokeWidth = when {
            isCardinal -> size * 0.016f
            isHour -> size * 0.012f
            else -> size * 0.007f
        }

        tickPaint.color = if (isHour) primaryText else secondaryText
        canvas.drawLine(x1, y1, x2, y2, tickPaint)
    }

    // 2. Proportional Clock Hands
    val hourLen = halfW * 0.45f
    val hourHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = size * 0.022f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        clockCx, clockCy,
        (clockCx + hourLen * Math.cos(timeState.hourAngleRad)).toFloat(),
        (clockCy + hourLen * Math.sin(timeState.hourAngleRad)).toFloat(),
        hourHandPaint
    )

    val minLen = halfW * 0.75f
    val minHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = size * 0.014f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        clockCx, clockCy,
        (clockCx + minLen * Math.cos(timeState.minuteAngleRad)).toFloat(),
        (clockCy + minLen * Math.sin(timeState.minuteAngleRad)).toFloat(),
        minHandPaint
    )

    val secLen = halfW * 0.85f
    val secTailLen = halfW * 0.18f
    val secHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = size * 0.009f
        strokeCap = Paint.Cap.ROUND
    }
    val secTipX = (clockCx + secLen * Math.cos(timeState.secondAngleRad)).toFloat()
    val secTipY = (clockCy + secLen * Math.sin(timeState.secondAngleRad)).toFloat()
    val secTailX = (clockCx - secTailLen * Math.cos(timeState.secondAngleRad)).toFloat()
    val secTailY = (clockCy - secTailLen * Math.sin(timeState.secondAngleRad)).toFloat()

    canvas.drawLine(secTailX, secTailY, secTipX, secTipY, secHandPaint)

    // Center Hub Pivot
    val hubRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawCircle(clockCx, clockCy, size * 0.022f, hubRingPaint)

    val hubCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(clockCx, clockCy, size * 0.011f, hubCenterPaint)

    return bitmap
}


// 3. CYBER SKELETON RING DIAL (2x2 Square / Circular Skeleton Face)
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

    // 1. Calculate Fixed Circular Card Bounds
    val size = minOf(w, h).toFloat()
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    // Draw perfect circular container
    canvas.drawOval(cardRect, bgPaint)

    val clockCx = cardRect.centerX()
    val clockCy = cardRect.centerY()
    val clockRadius = (size / 2f) - (size * 0.04f)

    val timeState = AnalogClockTimeState.now()

    // 2. Crosshair Guidelines
    val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#15000000") else Color.parseColor("#20FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = size * 0.008f
    }
    canvas.drawLine(clockCx - clockRadius, clockCy, clockCx + clockRadius, clockCy, crosshairPaint)
    canvas.drawLine(clockCx, clockCy - clockRadius, clockCx, clockCy + clockRadius, crosshairPaint)

    // 3. Concentric Ring Tracks
    val ringTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#1F000000") else Color.parseColor("#2AFFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = size * 0.012f
    }
    canvas.drawCircle(clockCx, clockCy, clockRadius, ringTrackPaint)
    canvas.drawCircle(clockCx, clockCy, clockRadius * 0.65f, ringTrackPaint)

    // 4. Precision Perimeter Micro-ticks (60 Ticks)
    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    for (i in 0 until 60) {
        val angleRad = Math.toRadians((i * 6f - 90f).toDouble())
        val isMajor = (i % 5 == 0)
        val outerR = clockRadius
        val innerR = if (isMajor) outerR - (size * 0.065f) else outerR - (size * 0.032f)

        val x1 = (clockCx + innerR * Math.cos(angleRad)).toFloat()
        val y1 = (clockCy + innerR * Math.sin(angleRad)).toFloat()
        val x2 = (clockCx + outerR * Math.cos(angleRad)).toFloat()
        val y2 = (clockCy + outerR * Math.sin(angleRad)).toFloat()

        tickPaint.strokeWidth = if (isMajor) size * 0.016f else size * 0.008f
        tickPaint.color = if (isMajor) accentColorInt else secondaryText
        canvas.drawLine(x1, y1, x2, y2, tickPaint)
    }

    // 5. Proportional Hands Rendering
    val hourLen = clockRadius * 0.52f
    val hourHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = size * 0.024f
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
        strokeWidth = size * 0.015f
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
        strokeWidth = size * 0.010f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        (clockCx - secTailLen * Math.cos(timeState.secondAngleRad)).toFloat(),
        (clockCy - secTailLen * Math.sin(timeState.secondAngleRad)).toFloat(),
        (clockCx + secLen * Math.cos(timeState.secondAngleRad)).toFloat(),
        (clockCy + secLen * Math.sin(timeState.secondAngleRad)).toFloat(),
        secPaint
    )

    // Open Skeleton Hub
    val hubRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = size * 0.015f
    }
    canvas.drawCircle(clockCx, clockCy, size * 0.032f, hubRingPaint)

    val hubDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.FILL
    }
    canvas.drawCircle(clockCx, clockCy, size * 0.012f, hubDotPaint)

    return bitmap
}

// 4. SCULPTED PILL MINIMAL DIAL (2x2 Square / Ultra-Minimal Capsule Hands & Orbital Node)
fun generateSculptedPillClockBitmap(
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

    // 1. Calculate Fixed Circular Card Bounds
    val size = minOf(w, h).toFloat()
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    // Draw perfect circular container
    canvas.drawOval(cardRect, bgPaint)

    val clockCx = cardRect.centerX()
    val clockCy = cardRect.centerY()
    val radius = size / 2f

    val timeState = AnalogClockTimeState.now()

    // 2. Subtle Outer Orbital Ring Track & Accent Satellite Node
    val trackRadius = radius - (size * 0.08f)
    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#0F000000") else Color.parseColor("#14FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = size * 0.008f
    }
    canvas.drawCircle(clockCx, clockCy, trackRadius, trackPaint)

    // Orbiting Accent Node (Seconds Satellite)
    val satX = (clockCx + trackRadius * Math.cos(timeState.secondAngleRad)).toFloat()
    val satY = (clockCy + trackRadius * Math.sin(timeState.secondAngleRad)).toFloat()

    val satPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawCircle(satX, satY, size * 0.022f, satPaint)

    // 3. Hour Hand: Bold Sculpted Capsule / Pill Blade
    val hourLen = trackRadius * 0.42f
    val hourWidth = size * 0.085f
    val hourTipX = (clockCx + hourLen * Math.cos(timeState.hourAngleRad)).toFloat()
    val hourTipY = (clockCy + hourLen * Math.sin(timeState.hourAngleRad)).toFloat()

    val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = hourWidth
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(clockCx, clockCy, hourTipX, hourTipY, hourPaint)

    // 4. Minute Hand: Sleek Tapered Pill Needle
    val minLen = trackRadius * 0.76f
    val minWidth = size * 0.038f
    val minTipX = (clockCx + minLen * Math.cos(timeState.minuteAngleRad)).toFloat()
    val minTipY = (clockCy + minLen * Math.sin(timeState.minuteAngleRad)).toFloat()

    val minPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        style = Paint.Style.STROKE
        strokeWidth = minWidth
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(clockCx, clockCy, minTipX, minTipY, minPaint)

    // 5. Center Hub Cutout / Pivot Ring
    val hubOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(clockCx, clockCy, size * 0.028f, hubOuterPaint)

    val hubInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawCircle(clockCx, clockCy, size * 0.014f, hubInnerPaint)

    return bitmap
}

// 5. BOLD TYPOGRAPHIC CARDINAL DIAL (2x2 Square / Giant Overlaid Cardinal Numerals)
fun generateBoldTypographyClockBitmap(
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

    // Mid-tone numeral color with clean opacity depth
    val numeralColor = if (isLight) Color.parseColor("#3B000000") else Color.parseColor("#45FFFFFF")

    // 1. Calculate Fixed Square Card Bounds
    val size = minOf(w, h).toFloat()
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    val cardRadius = size * 0.18f
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val clockCx = cardRect.centerX()
    val clockCy = cardRect.centerY()

    val timeState = AnalogClockTimeState.now()

    // 2. Proportional Cardinal Numerals (Sized & Offset to Prevent Collisions)
    val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = numeralColor
        textSize = size * 0.35f
        typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val fm = numPaint.fontMetrics
    val textYCenter = -((fm.descent + fm.ascent) / 2f)

    val numOffset = size * 0.26f

    // "12" Top
    canvas.drawText("12", clockCx, clockCy - numOffset + textYCenter, numPaint)
    // "3" Right
    canvas.drawText("3", clockCx + numOffset, clockCy + textYCenter, numPaint)
    // "6" Bottom
    canvas.drawText("6", clockCx, clockCy + numOffset + textYCenter, numPaint)
    // "9" Left
    canvas.drawText("9", clockCx - numOffset, clockCy + textYCenter, numPaint)

    // 3. Heavy Capsule Clock Hands
    // Hour Hand
    val hourLen = size * 0.21f
    val hourHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = size * 0.048f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        clockCx, clockCy,
        (clockCx + hourLen * Math.cos(timeState.hourAngleRad)).toFloat(),
        (clockCy + hourLen * Math.sin(timeState.hourAngleRad)).toFloat(),
        hourHandPaint
    )

    // Minute Hand
    val minLen = size * 0.34f
    val minHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = size * 0.038f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        clockCx, clockCy,
        (clockCx + minLen * Math.cos(timeState.minuteAngleRad)).toFloat(),
        (clockCy + minLen * Math.sin(timeState.minuteAngleRad)).toFloat(),
        minHandPaint
    )

    // Second Hand
    val secLen = size * 0.38f
    val secTailLen = size * 0.08f
    val secHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = size * 0.010f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        (clockCx - secTailLen * Math.cos(timeState.secondAngleRad)).toFloat(),
        (clockCy - secTailLen * Math.sin(timeState.secondAngleRad)).toFloat(),
        (clockCx + secLen * Math.cos(timeState.secondAngleRad)).toFloat(),
        (clockCy + secLen * Math.sin(timeState.secondAngleRad)).toFloat(),
        secHandPaint
    )

    // 4. Center Hub Pivot
    val hubOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.FILL
    }
    canvas.drawCircle(clockCx, clockCy, size * 0.038f, hubOuterPaint)

    val hubCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawCircle(clockCx, clockCy, size * 0.018f, hubCenterPaint)

    return bitmap
}

// 6. CYBER CONDENSED CARDINAL DIAL (2x2 Circular / Ultra-Stylized Industrial Face)
fun generateCyberCondensedClockBitmap(
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

    // Subtle dark tint for background numerals
    val numeralColor = if (isLight) Color.parseColor("#22000000") else Color.parseColor("#2BFFFFFF")

    // Load custom font from res/font/outward_block.ttf
    val customTypeface = try {
        ResourcesCompat.getFont(context, R.font.outward_block) ?: Typeface.DEFAULT_BOLD
    } catch (_: Exception) {
        Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }

    // 1. Calculate Fixed Circular Card Bounds
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
    val radius = size / 2f

    val timeState = AnalogClockTimeState.now()

    // 2. High-Impact Typography using Custom Outward Block Font
    val sideNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = numeralColor
        textSize = size * 1f
        typeface = customTypeface
        textAlign = Paint.Align.CENTER
    }

    val fmSide = sideNumPaint.fontMetrics
    val sideYCenter = clockCy - ((fmSide.descent + fmSide.ascent) / 1.57f)

    // Draw Left "9" and Right "3"
    canvas.drawText("9", clockCx - (size * 0.25f), sideYCenter, sideNumPaint)
    canvas.drawText("3", clockCx + (size * 0.25f), sideYCenter, sideNumPaint)

    val topNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = numeralColor
        textSize = size * 0.5f
        typeface = customTypeface
        textAlign = Paint.Align.CENTER
    }

    val fmTop = topNumPaint.fontMetrics
    val topYCenter = (clockCy - size * 0.25f) - ((fmTop.descent + fmTop.ascent) / 2f)
    val bottomYCenter = (clockCy + size * 0.33f) - ((fmTop.descent + fmTop.ascent) / 2f)

    // Draw Top "12" and Bottom "06"
    canvas.drawText("12", clockCx, topYCenter, topNumPaint)
    canvas.drawText("06", clockCx, bottomYCenter, topNumPaint)

    // 3. Rotated Precision Hands
    val hourDeg = (timeState.hoursWithMinutes * 30f)
    val minDeg = (timeState.minutesWithSeconds * 6f)
    val secDeg = (timeState.secondsWithMillis * 6f)

    // A. HOUR HAND: Slotted Architectural Capsule Blade
    val hourWidth = size * 0.068f
    val hourLength = radius * 0.44f
    val hourTail = size * 0.025f
    val slotWidth = hourWidth * 0.35f

    val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.FILL
    }
    val slotCutoutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }

    canvas.save()
    canvas.rotate(hourDeg, clockCx, clockCy)
    val hourRect = RectF(
        clockCx - (hourWidth / 2f),
        clockCy - hourLength,
        clockCx + (hourWidth / 2f),
        clockCy + hourTail
    )
    canvas.drawRoundRect(hourRect, hourWidth / 2f, hourWidth / 2f, hourPaint)

    val slotRect = RectF(
        clockCx - (slotWidth / 2f),
        clockCy - (hourLength * 0.82f),
        clockCx + (slotWidth / 2f),
        clockCy - (hourLength * 0.20f)
    )
    canvas.drawRoundRect(slotRect, slotWidth / 2f, slotWidth / 2f, slotCutoutPaint)
    canvas.restore()

    // B. MINUTE HAND: Clean Minimal Precision Wand
    val minWidth = size * 0.018f
    val minLength = radius * 0.70f
    val minTail = radius * 0.10f

    val minPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = minWidth
        strokeCap = Paint.Cap.ROUND
    }

    canvas.save()
    canvas.rotate(minDeg, clockCx, clockCy)
    canvas.drawLine(
        clockCx, clockCy + minTail,
        clockCx, clockCy - minLength,
        minPaint
    )
    canvas.restore()

    // C. SECOND HAND: Accent Line with Dual Ring Pivot Hub
    val secLen = radius * 0.82f
    val secTailLen = radius * 0.18f
    val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = size * 0.009f
        strokeCap = Paint.Cap.ROUND
    }
    val secHubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    val secCutoutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }

    canvas.save()
    canvas.rotate(secDeg, clockCx, clockCy)
    canvas.drawLine(
        clockCx, clockCy + secTailLen,
        clockCx, clockCy - secLen,
        secPaint
    )
    canvas.drawCircle(clockCx, clockCy, size * 0.024f, secHubPaint)
    canvas.drawCircle(clockCx, clockCy, size * 0.012f, secCutoutPaint)
    canvas.restore()

    return bitmap
}

// 7. CAPSULE SKELETON ACCENT DIAL (2x2 Circular / Hollow Capsule Hands & Accent Tips)
fun generateCapsuleSkeletonClockBitmap(
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

    // 1. Calculate Fixed Circular Card Bounds
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
    val radius = size / 2f

    val timeState = AnalogClockTimeState.now()

    // 2. 12 Minimal Perimeter Radial Ticks
    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    val tickMargin = size * 0.08f
    val outerR = radius - tickMargin

    for (i in 0 until 12) {
        val angleRad = Math.toRadians((i * 30f - 90f).toDouble())
        val isCardinal = (i % 3 == 0)

        val tickLen = if (isCardinal) size * 0.08f else size * 0.05f
        val innerR = outerR - tickLen

        val x1 = (clockCx + innerR * Math.cos(angleRad)).toFloat()
        val y1 = (clockCy + innerR * Math.sin(angleRad)).toFloat()
        val x2 = (clockCx + outerR * Math.cos(angleRad)).toFloat()
        val y2 = (clockCy + outerR * Math.sin(angleRad)).toFloat()

        tickPaint.strokeWidth = if (isCardinal) size * 0.018f else size * 0.010f
        tickPaint.color = primaryText
        canvas.drawLine(x1, y1, x2, y2, tickPaint)
    }

    // 3. Date Window at 4:30 Position
    val cal = Calendar.getInstance()
    val dayStr = cal.get(Calendar.DAY_OF_MONTH).toString()

    val dateAngleRad = Math.toRadians(45.0)
    val dateDist = radius * 0.52f
    val dateCx = clockCx + (dateDist * Math.cos(dateAngleRad)).toFloat()
    val dateCy = clockCy + (dateDist * Math.sin(dateAngleRad)).toFloat()

    val dateBoxW = size * 0.16f
    val dateBoxH = size * 0.13f
    val dateRect = RectF(
        dateCx - dateBoxW / 2f,
        dateCy - dateBoxH / 2f,
        dateCx + dateBoxW / 2f,
        dateCy + dateBoxH / 2f
    )

    val dateBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = size * 0.012f
    }
    val dateBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(dateRect, size * 0.035f, size * 0.035f, dateBgPaint)
    canvas.drawRoundRect(dateRect, size * 0.035f, size * 0.035f, dateBorderPaint)

    val dateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = size * 0.075f
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val fmDate = dateTextPaint.fontMetrics
    val dateTextY = dateCy - ((fmDate.descent + fmDate.ascent) / 2f)
    canvas.drawText(dayStr, dateCx, dateTextY, dateTextPaint)

    // 4. Rotated Precision Capsule Frame Hands
    val hourDeg = (timeState.hoursWithMinutes * 30f)
    val minDeg = (timeState.minutesWithSeconds * 6f)
    val secDeg = (timeState.secondsWithMillis * 6f)

    fun drawCapsuleHand(length: Float, width: Float, deg: Float) {
        val strokeW = size * 0.014f
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            style = Paint.Style.STROKE
            strokeWidth = strokeW
        }
        val tipAccentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }

        canvas.save()
        canvas.rotate(deg, clockCx, clockCy)

        // Outer Frame
        val frameRect = RectF(
            clockCx - (width / 2f),
            clockCy - length,
            clockCx + (width / 2f),
            clockCy + (width * 0.35f)
        )
        val cornerR = width / 2f
        canvas.drawRoundRect(frameRect, cornerR, cornerR, framePaint)

        // Inner Accent Tip
        val tipMargin = strokeW * 1.2f
        val tipH = width * 0.75f
        val tipRect = RectF(
            clockCx - (width / 2f) + tipMargin,
            clockCy - length + tipMargin,
            clockCx + (width / 2f) - tipMargin,
            clockCy - length + tipMargin + tipH
        )
        val tipCornerR = tipRect.width() / 2f
        canvas.drawRoundRect(tipRect, tipCornerR, tipCornerR, tipAccentPaint)

        canvas.restore()
    }

    // A. Hour Hand (Shorter)
    drawCapsuleHand(radius * 0.42f, size * 0.082f, hourDeg)

    // B. Minute Hand (Longer)
    drawCapsuleHand(radius * 0.68f, size * 0.075f, minDeg)

    // C. Pivot Hub
    val pivotOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.FILL
    }
    val pivotInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    val pivotRadius = size * 0.035f
    canvas.drawCircle(clockCx, clockCy, pivotRadius, pivotOuterPaint)
    canvas.drawCircle(clockCx, clockCy, pivotRadius * 0.65f, pivotInnerPaint)

    // D. Second Hand
    val secLen = radius * 0.82f
    val secTailLen = radius * 0.15f
    val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = size * 0.008f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.save()
    canvas.rotate(secDeg, clockCx, clockCy)
    canvas.drawLine(clockCx, clockCy + secTailLen, clockCx, clockCy - secLen, secPaint)
    canvas.restore()

    return bitmap
}

// 8. APEX ARROWHEAD CARDINAL DIAL (2x2 Circular / Accent Arrowhead Markers & Ball-Tip Hands)
fun generateApexArrowheadClockBitmap(
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

    // 1. Calculate Fixed Circular Card Bounds
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
    val radius = size / 2f

    val timeState = AnalogClockTimeState.now()

    // 2. Hour Markers (Cardinal Inward Arrowheads & Minor Line Ticks)
    val margin = size * 0.06f
    val outerR = radius - margin

    val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }

    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = size * 0.012f
        strokeCap = Paint.Cap.ROUND
    }

    val arrowLen = size * 0.16f
    val arrowWidth = size * 0.08f

    for (i in 0 until 12) {
        val angleRad = Math.toRadians((i * 30f - 90f).toDouble())
        val isCardinal = (i % 3 == 0)

        if (isCardinal) {
            // Draw Sharp Inward Arrowhead Path for 12, 3, 6, 9
            val cosA = Math.cos(angleRad).toFloat()
            val sinA = Math.sin(angleRad).toFloat()

            // Perpendicular unit vector
            val perpX = -sinA
            val perpY = cosA

            val baseX = clockCx + outerR * cosA
            val baseY = clockCy + outerR * sinA

            val tipX = clockCx + (outerR - arrowLen) * cosA
            val tipY = clockCy + (outerR - arrowLen) * sinA

            val corner1X = baseX + (arrowWidth / 2f) * perpX
            val corner1Y = baseY + (arrowWidth / 2f) * perpY

            val corner2X = baseX - (arrowWidth / 2f) * perpX
            val corner2Y = baseY - (arrowWidth / 2f) * perpY

            val arrowPath = android.graphics.Path().apply {
                moveTo(corner1X, corner1Y)
                lineTo(tipX, tipY)
                lineTo(corner2X, corner2Y)
                close()
            }
            canvas.drawPath(arrowPath, arrowPaint)
        } else {
            // Draw Clean Line Ticks for Minor Hours
            val tickLen = size * 0.06f
            val innerR = outerR - tickLen

            val x1 = (clockCx + innerR * Math.cos(angleRad)).toFloat()
            val y1 = (clockCy + innerR * Math.sin(angleRad)).toFloat()
            val x2 = (clockCx + outerR * Math.cos(angleRad)).toFloat()
            val y2 = (clockCy + outerR * Math.sin(angleRad)).toFloat()

            canvas.drawLine(x1, y1, x2, y2, tickPaint)
        }
    }

    // 3. Rotated Precision Ball-Tip Hands
    val hourDeg = (timeState.hoursWithMinutes * 30f)
    val minDeg = (timeState.minutesWithSeconds * 6f)
    val secDeg = (timeState.secondsWithMillis * 6f)

    val handLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = size * 0.016f
        strokeCap = Paint.Cap.ROUND
    }

    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }

    val dotBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = size * 0.008f
    }

    // A. Hour Hand (Ball-Tip Wand)
    val hourLen = radius * 0.44f
    val hourDotR = size * 0.026f
    canvas.save()
    canvas.rotate(hourDeg, clockCx, clockCy)
    canvas.drawLine(clockCx, clockCy, clockCx, clockCy - hourLen, handLinePaint)
    canvas.drawCircle(clockCx, clockCy - hourLen, hourDotR, dotPaint)
    canvas.drawCircle(clockCx, clockCy - hourLen, hourDotR, dotBorderPaint)
    canvas.restore()

    // B. Minute Hand (Longer Ball-Tip Wand)
    val minLen = radius * 0.70f
    val minDotR = size * 0.024f
    canvas.save()
    canvas.rotate(minDeg, clockCx, clockCy)
    canvas.drawLine(clockCx, clockCy, clockCx, clockCy - minLen, handLinePaint)
    canvas.drawCircle(clockCx, clockCy - minLen, minDotR, dotPaint)
    canvas.drawCircle(clockCx, clockCy - minLen, minDotR, dotBorderPaint)
    canvas.restore()

    // C. Pivot Ring Hub
    val pivotRadius = size * 0.032f
    canvas.drawCircle(clockCx, clockCy, pivotRadius, dotPaint)
    canvas.drawCircle(clockCx, clockCy, pivotRadius, dotBorderPaint)

    // D. Second Hand (Thin Needle)
    val secLen = radius * 0.82f
    val secTailLen = radius * 0.16f
    val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = size * 0.008f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.save()
    canvas.rotate(secDeg, clockCx, clockCy)
    canvas.drawLine(clockCx, clockCy + secTailLen, clockCx, clockCy - secLen, secPaint)
    canvas.restore()

    return bitmap
}

// 9. CONCENTRIC ORBITAL ARC DIAL (2x2 Circular / Swapped Rings & Customizable Controls)
fun generateConcentricOrbitalClockBitmap(
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
    val secondaryText = if (isLight) Color.parseColor("#20000000") else Color.parseColor("#25FFFFFF")

    // 1. Calculate Fixed Circular Card Bounds
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
    val radius = size / 2f

    val timeState = AnalogClockTimeState.now()

    // -------------------------------------------------------------------------
    // 🎛️ DESIGN TUNING CONTROLS (Adjust these variables to change geometry)
    // -------------------------------------------------------------------------
    val outerRadiusMultiplier = 0.76f  // Outer Ring Radius (Minute Arc)
    val innerRadiusMultiplier = 0.58f  // Inner Ring Radius (Hour Arc) -> WIDER GAP
    val secOrbitMultiplier    = 0.89f  // Orbiting Seconds Dot Radius

    val outerStrokeWidth      = size * 0.030f // Thicker Minute Ring
    val innerStrokeWidth      = size * 0.035f // Thicker Hour Ring
    val bgTrackStrokeWidth    = size * 0.018f // Dark Background Track Thickness
    val secondsDotSize        = size * 0.028f // Orbiting Seconds Satellite Size
    // -------------------------------------------------------------------------

    val outerRadius = radius * outerRadiusMultiplier
    val innerRadius = radius * innerRadiusMultiplier
    val secOrbitRadius = radius * secOrbitMultiplier

    // Background Ring Tracks
    val trackPaintOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        style = Paint.Style.STROKE
        strokeWidth = bgTrackStrokeWidth
    }
    val trackPaintInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        style = Paint.Style.STROKE
        strokeWidth = bgTrackStrokeWidth
    }

    canvas.drawCircle(clockCx, clockCy, outerRadius, trackPaintOuter)
    canvas.drawCircle(clockCx, clockCy, innerRadius, trackPaintInner)

    // 2. OUTER RING: MINUTE PROGRESS ARC
    val minProgress = timeState.minutesWithSeconds / 60f
    val minSweepAngle = (minProgress * 360f).coerceAtLeast(1f)

    val minArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = outerStrokeWidth
        strokeCap = Paint.Cap.ROUND
    }
    val outerRect = RectF(
        clockCx - outerRadius, clockCy - outerRadius,
        clockCx + outerRadius, clockCy + outerRadius
    )
    canvas.drawArc(outerRect, -90f, minSweepAngle, false, minArcPaint)

    // 3. INNER RING: HOUR PROGRESS ARC
    val hourProgress = (timeState.hoursWithMinutes % 12f) / 12f
    val hourSweepAngle = (hourProgress * 360f).coerceAtLeast(1f)

    val hourArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = innerStrokeWidth
        strokeCap = Paint.Cap.ROUND
    }
    val innerRect = RectF(
        clockCx - innerRadius, clockCy - innerRadius,
        clockCx + innerRadius, clockCy + innerRadius
    )
    canvas.drawArc(innerRect, -90f, hourSweepAngle, false, hourArcPaint)

    // 4. ORBITING SECONDS SATELLITE NODE
    val secX = (clockCx + secOrbitRadius * Math.cos(timeState.secondAngleRad)).toFloat()
    val secY = (clockCy + secOrbitRadius * Math.sin(timeState.secondAngleRad)).toFloat()

    val secDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawCircle(secX, secY, secondsDotSize, secDotPaint)

    return bitmap
}

// 10. TRIPLE ORBITAL DOTS DIAL (2x2 Circular / Minimalist 3-Node Planetary Clock)
fun generateTripleOrbitalDotsClockBitmap(
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
    val secondaryText = if (isLight) Color.parseColor("#20000000") else Color.parseColor("#25FFFFFF")

    // 1. Calculate Fixed Circular Card Bounds
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
    val radius = size / 2f

    val timeState = AnalogClockTimeState.now()

    // -------------------------------------------------------------------------
    // 🎛️ DESIGN TUNING CONTROLS
    // -------------------------------------------------------------------------
    val secOrbitMultiplier  = 0.86f // Outer Track (Seconds)
    val minOrbitMultiplier  = 0.68f // Middle Track (Minutes)
    val hourOrbitMultiplier = 0.50f // Inner Track (Hours)

    val trackStrokeWidth = size * 0.014f // Track Ring Thickness
    val hourDotRadius    = size * 0.038f // Inner Hour Node Radius
    val minDotRadius     = size * 0.030f // Middle Minute Node Radius
    val secDotRadius     = size * 0.022f // Outer Second Node Radius
    // -------------------------------------------------------------------------

    val secOrbitRadius  = radius * secOrbitMultiplier
    val minOrbitRadius  = radius * minOrbitMultiplier
    val hourOrbitRadius = radius * hourOrbitMultiplier

    // 2. Render 3 Concentric Track Rings
    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        style = Paint.Style.STROKE
        strokeWidth = trackStrokeWidth
    }

    canvas.drawCircle(clockCx, clockCy, secOrbitRadius, trackPaint)
    canvas.drawCircle(clockCx, clockCy, minOrbitRadius, trackPaint)
    canvas.drawCircle(clockCx, clockCy, hourOrbitRadius, trackPaint)

    // 3. Orbiting Nodes Geometry
    // A. INNER TRACK: Hour Node
    val hourX = (clockCx + hourOrbitRadius * Math.cos(timeState.hourAngleRad)).toFloat()
    val hourY = (clockCy + hourOrbitRadius * Math.sin(timeState.hourAngleRad)).toFloat()

    val hourDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.FILL
    }
    canvas.drawCircle(hourX, hourY, hourDotRadius, hourDotPaint)

    // B. MIDDLE TRACK: Minute Node
    val minX = (clockCx + minOrbitRadius * Math.cos(timeState.minuteAngleRad)).toFloat()
    val minY = (clockCy + minOrbitRadius * Math.sin(timeState.minuteAngleRad)).toFloat()

    val minDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.FILL
    }
    canvas.drawCircle(minX, minY, minDotRadius, minDotPaint)

    // C. OUTER TRACK: Second Node (Accent Color)
    val secX = (clockCx + secOrbitRadius * Math.cos(timeState.secondAngleRad)).toFloat()
    val secY = (clockCy + secOrbitRadius * Math.sin(timeState.secondAngleRad)).toFloat()

    val secDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawCircle(secX, secY, secDotRadius, secDotPaint)

    // Center Core Axis Dot
    val centerPivotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        style = Paint.Style.FILL
    }
    canvas.drawCircle(clockCx, clockCy, size * 0.015f, centerPivotPaint)

    return bitmap
}

// 11. SECTOR SWEEP ACCENT DIAL (2x2 Circular / High-Contrast Sector Face)
fun generateSectorSweepClockBitmap(
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

    // 1. Calculate Fixed Circular Card Bounds
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
    val radius = size / 2f

    val timeState = AnalogClockTimeState.now()

    // 2. Sector / Pie Wedge Geometry
    val dialMargin = size * 0.05f
    val dialRadius = radius - dialMargin

    val hourProgress = (timeState.hoursWithMinutes % 12f) / 12f
    val sweepAngle = (hourProgress * 360f).coerceAtLeast(0.5f)

    val sectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }

    val dialRect = RectF(
        clockCx - dialRadius, clockCy - dialRadius,
        clockCx + dialRadius, clockCy + dialRadius
    )
    // Draw filled wedge from 12 o'clock
    canvas.drawArc(dialRect, -90f, sweepAngle, true, sectorPaint)

    // 3. Cardinal Line Ticks
    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = size * 0.016f
        strokeCap = Paint.Cap.ROUND
    }

    val tickLen = size * 0.06f
    for (i in 0 until 4) {
        val angleRad = Math.toRadians((i * 90f - 90f).toDouble())
        val outerR = dialRadius
        val innerR = dialRadius - tickLen

        val x1 = (clockCx + innerR * Math.cos(angleRad)).toFloat()
        val y1 = (clockCy + innerR * Math.sin(angleRad)).toFloat()
        val x2 = (clockCx + outerR * Math.cos(angleRad)).toFloat()
        val y2 = (clockCy + outerR * Math.sin(angleRad)).toFloat()

        canvas.drawLine(x1, y1, x2, y2, tickPaint)
    }

    // 4. Rotated Hands with Dark Outline Casing (Prevents disappearing hands)
    val hourDeg = (timeState.hoursWithMinutes * 30f)
    val minDeg = (timeState.minutesWithSeconds * 6f)
    val secDeg = (timeState.secondsWithMillis * 6f)

    val outlineStrokeExtra = size * 0.012f

    // Helper to draw hands with dark outlines for high contrast
    fun drawContrastedHand(length: Float, handWidth: Float, deg: Float, handColor: Int) {
        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.STROKE
            strokeWidth = handWidth + outlineStrokeExtra
            strokeCap = Paint.Cap.ROUND
        }
        val mainHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = handColor
            style = Paint.Style.STROKE
            strokeWidth = handWidth
            strokeCap = Paint.Cap.ROUND
        }

        canvas.save()
        canvas.rotate(deg, clockCx, clockCy)
        // Dark outline casing
        canvas.drawLine(clockCx, clockCy, clockCx, clockCy - length, outlinePaint)
        // Primary hand stroke
        canvas.drawLine(clockCx, clockCy, clockCx, clockCy - length, mainHandPaint)
        canvas.restore()
    }

    // A. Hour Hand
    drawContrastedHand(dialRadius * 0.52f, size * 0.024f, hourDeg, primaryText)

    // B. Minute Hand
    drawContrastedHand(dialRadius * 0.78f, size * 0.018f, minDeg, primaryText)

    // C. Second Hand
    val secLen = dialRadius * 0.88f
    val secTailLen = dialRadius * 0.16f
    val secOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.STROKE
        strokeWidth = (size * 0.009f) + outlineStrokeExtra
        strokeCap = Paint.Cap.ROUND
    }
    val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = size * 0.009f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.save()
    canvas.rotate(secDeg, clockCx, clockCy)
    canvas.drawLine(clockCx, clockCy + secTailLen, clockCx, clockCy - secLen, secOutlinePaint)
    canvas.drawLine(clockCx, clockCy + secTailLen, clockCx, clockCy - secLen, secPaint)
    canvas.restore()

    // D. Center Pivot Hub (With Dark Contrast Casing)
    val pivotRadius = size * 0.028f
    val pivotBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    val pivotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.FILL
    }
    canvas.drawCircle(clockCx, clockCy, pivotRadius + (size * 0.006f), pivotBgPaint)
    canvas.drawCircle(clockCx, clockCy, pivotRadius, pivotPaint)

    return bitmap
}

// 12. TRIPLE ROTATING RING DIAL (2x2 Circular / Concentric Ring Hand-Length Clock)
fun generateRotatingRingClockBitmap(
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
    val secondaryText = if (isLight) Color.parseColor("#18000000") else Color.parseColor("#22FFFFFF")

    // 1. Calculate Fixed Circular Card Bounds
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
    val radius = size / 2f

    val timeState = AnalogClockTimeState.now()

    // 2. Concentric Ring Boundary Radii
    val r0 = radius * 0.05f   // Center Axis Radius
    val r1 = radius * 0.38f   // Inner Ring Radius (Hour boundary)
    val r2 = radius * 0.64f   // Middle Ring Radius (Minute boundary)
    val r3 = radius * 0.88f   // Outer Ring Radius (Second boundary)

    // Draw Static Concentric Boundary Rings
    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        style = Paint.Style.STROKE
        strokeWidth = size * 0.010f
    }
    canvas.drawCircle(clockCx, clockCy, r1, trackPaint)
    canvas.drawCircle(clockCx, clockCy, r2, trackPaint)
    canvas.drawCircle(clockCx, clockCy, r3, trackPaint)

    // Rotation Angles
    val hourDeg = timeState.hoursWithMinutes * 30f
    val minDeg = timeState.minutesWithSeconds * 6f
    val secDeg = timeState.secondsWithMillis * 6f

    // 3. HOUR HAND: Solid Capsule Extending from Center to Inner Ring (r1)
    val hourWidth = size * 0.070f
    val hourHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.FILL
    }
    canvas.save()
    canvas.rotate(hourDeg, clockCx, clockCy)
    val hourRect = RectF(
        clockCx - (hourWidth / 2f),
        clockCy - r1,
        clockCx + (hourWidth / 2f),
        clockCy + r0
    )
    canvas.drawRoundRect(hourRect, hourWidth / 2f, hourWidth / 2f, hourHandPaint)
    canvas.restore()

    // 4. MINUTE HAND: Hollow Capsule Frame Extending from Center to Middle Ring (r2)
    val minWidth = size * 0.060f
    val minFramePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = size * 0.016f
    }
    canvas.save()
    canvas.rotate(minDeg, clockCx, clockCy)
    val minRect = RectF(
        clockCx - (minWidth / 2f),
        clockCy - r2,
        clockCx + (minWidth / 2f),
        clockCy + r0
    )
    canvas.drawRoundRect(minRect, minWidth / 2f, minWidth / 2f, minFramePaint)
    canvas.restore()

    // 5. SECOND HAND: Accent Needle Extending from Center to Outer Ring (r3)
    val secWidth = size * 0.020f
    val secHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.save()
    canvas.rotate(secDeg, clockCx, clockCy)
    val secRect = RectF(
        clockCx - (secWidth / 2f),
        clockCy - r3,
        clockCx + (secWidth / 2f),
        clockCy + r0
    )
    canvas.drawRoundRect(secRect, secWidth / 2f, secWidth / 2f, secHandPaint)
    canvas.restore()

    // 6. Center Pivot Node
    val centerPivotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        style = Paint.Style.FILL
    }
    canvas.drawCircle(clockCx, clockCy, r0 * 2f, centerPivotPaint)

    return bitmap
}

// 13. HOURGLASS DYNAMIC ACCENT DIAL (2x2 Square / Organic Curved Hourglass)
fun generateHourglassClockBitmap(
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

    val size = minOf(w, h).toFloat()
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, size * 0.18f, size * 0.18f, bgPaint)

    val clockCx = cardRect.centerX()
    val clockCy = cardRect.centerY()

    val timeState = AnalogClockTimeState.now()

    // 1. Organic Curved Hourglass Path Geometry
    val topWidth = size * 0.32f
    val waistWidth = size * 0.035f
    val halfH = size * 0.36f

    val topYPos = clockCy - halfH
    val botYPos = clockCy + halfH

    // Separate Top & Bottom Chambers for Precise Sand Clipping
    val topChamberPath = android.graphics.Path().apply {
        moveTo(clockCx - topWidth, topYPos)
        lineTo(clockCx + topWidth, topYPos)
        cubicTo(
            clockCx + topWidth * 0.45f, clockCy - halfH * 0.45f,
            clockCx + waistWidth, clockCy - size * 0.02f,
            clockCx + waistWidth, clockCy
        )
        lineTo(clockCx - waistWidth, clockCy)
        cubicTo(
            clockCx - waistWidth, clockCy - size * 0.02f,
            clockCx - topWidth * 0.45f, clockCy - halfH * 0.45f,
            clockCx - topWidth, topYPos
        )
        close()
    }

    val bottomChamberPath = android.graphics.Path().apply {
        moveTo(clockCx - waistWidth, clockCy)
        lineTo(clockCx + waistWidth, clockCy)
        cubicTo(
            clockCx + waistWidth, clockCy + size * 0.02f,
            clockCx + topWidth * 0.45f, clockCy + halfH * 0.45f,
            clockCx + topWidth, botYPos
        )
        lineTo(clockCx - topWidth, botYPos)
        cubicTo(
            clockCx - topWidth * 0.45f, clockCy + halfH * 0.45f,
            clockCx - waistWidth, clockCy + size * 0.02f,
            clockCx - waistWidth, clockCy
        )
        close()
    }

    val fullGlassPath = android.graphics.Path().apply {
        addPath(topChamberPath)
        addPath(bottomChamberPath)
    }

    // 2. 60-Minute Sand Progress Logic
    val minuteProgress = (timeState.minutesWithSeconds / 60f).coerceIn(0.001f, 0.999f)

    val sandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }

    // A. Top Chamber Sand (Depleting)
    val topSandY = topYPos + (minuteProgress * halfH)
    canvas.save()
    canvas.clipPath(topChamberPath)
    canvas.drawRect(
        clockCx - topWidth * 1.2f, topSandY,
        clockCx + topWidth * 1.2f, clockCy,
        sandPaint
    )
    canvas.restore()

    // B. Bottom Chamber Sand (Accumulating with natural parabolic heap)
    val bottomSandLevelY = botYPos - (minuteProgress * halfH)
    val moundHeight = size * 0.035f * Math.sin(minuteProgress * Math.PI).toFloat()

    val sandMoundPath = android.graphics.Path().apply {
        moveTo(clockCx - topWidth * 1.2f, botYPos + size * 0.1f)
        lineTo(clockCx - topWidth * 1.2f, bottomSandLevelY)
        quadTo(
            clockCx, bottomSandLevelY - moundHeight,
            clockCx + topWidth * 1.2f, bottomSandLevelY
        )
        lineTo(clockCx + topWidth * 1.2f, botYPos + size * 0.1f)
        close()
    }

    canvas.save()
    canvas.clipPath(bottomChamberPath)
    canvas.drawPath(sandMoundPath, sandPaint)
    canvas.restore()

    // C. Falling Sand Stream Line
    if (minuteProgress < 0.97f) {
        val streamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.STROKE
            strokeWidth = size * 0.010f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(clockCx, clockCy, clockCx, bottomSandLevelY - moundHeight, streamPaint)
    }

    // 3. Glass Vessel Outer Contour Stroke & Structural Caps
    val glassFramePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = size * 0.016f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    canvas.drawPath(fullGlassPath, glassFramePaint)

    // Structural Cap Bars (Top & Bottom)
    val capWidth = topWidth * 1.08f
    val capHeight = size * 0.022f
    val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.FILL
    }
    val topCapRect = RectF(clockCx - capWidth, topYPos - capHeight, clockCx + capWidth, topYPos + (capHeight * 0.3f))
    canvas.drawRoundRect(topCapRect, capHeight, capHeight, capPaint)

    val botCapRect = RectF(clockCx - capWidth, botYPos - (capHeight * 0.3f), clockCx + capWidth, botYPos + capHeight)
    canvas.drawRoundRect(botCapRect, capHeight, capHeight, capPaint)

    // Subtle Inner Glass Reflection Highlight
    val reflectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(45, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = size * 0.008f
        strokeCap = Paint.Cap.ROUND
    }
    val topReflectionPath = android.graphics.Path().apply {
        moveTo(clockCx - topWidth * 0.75f, topYPos + size * 0.03f)
        cubicTo(
            clockCx - topWidth * 0.35f, clockCy - halfH * 0.4f,
            clockCx - waistWidth * 1.5f, clockCy - size * 0.05f,
            clockCx - waistWidth * 1.5f, clockCy - size * 0.02f
        )
    }
    canvas.drawPath(topReflectionPath, reflectionPaint)

    // 4. Rotated Precision Hands with Contrast Casing
    val hourDeg = timeState.hoursWithMinutes * 30f
    val minDeg = timeState.minutesWithSeconds * 6f
    val secDeg = timeState.secondsWithMillis * 6f

    val outlineExtra = size * 0.012f

    fun drawContrastedPillHand(length: Float, width: Float, deg: Float, handColor: Int) {
        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 16, 16, 24)
            style = Paint.Style.STROKE
            strokeWidth = width + outlineExtra
            strokeCap = Paint.Cap.ROUND
        }
        val mainHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = handColor
            style = Paint.Style.STROKE
            strokeWidth = width
            strokeCap = Paint.Cap.ROUND
        }

        canvas.save()
        canvas.rotate(deg, clockCx, clockCy)
        canvas.drawLine(clockCx, clockCy, clockCx, clockCy - length, outlinePaint)
        canvas.drawLine(clockCx, clockCy, clockCx, clockCy - length, mainHandPaint)
        canvas.restore()
    }

    // A. Hour Hand
    drawContrastedPillHand(size * 0.21f, size * 0.028f, hourDeg, primaryText)

    // B. Minute Hand
    drawContrastedPillHand(size * 0.31f, size * 0.018f, minDeg, primaryText)

    // C. Second Hand
    val secLen = size * 0.34f
    val secTailLen = size * 0.07f
    val secOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 16, 16, 24)
        style = Paint.Style.STROKE
        strokeWidth = (size * 0.009f) + outlineExtra
        strokeCap = Paint.Cap.ROUND
    }
    val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = size * 0.009f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.save()
    canvas.rotate(secDeg, clockCx, clockCy)
    canvas.drawLine(clockCx, clockCy + secTailLen, clockCx, clockCy - secLen, secOutlinePaint)
    canvas.drawLine(clockCx, clockCy + secTailLen, clockCx, clockCy - secLen, secPaint)
    canvas.restore()

    // 5. Center Pivot Node (Architectural Ring Node)
    val pivotRadius = size * 0.026f
    val pivotOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 16, 16, 24)
        style = Paint.Style.FILL
    }
    val pivotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    val pivotCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.FILL
    }

    canvas.drawCircle(clockCx, clockCy, pivotRadius + (size * 0.006f), pivotOutlinePaint)
    canvas.drawCircle(clockCx, clockCy, pivotRadius, pivotPaint)
    canvas.drawCircle(clockCx, clockCy, pivotRadius * 0.45f, pivotCorePaint)

    return bitmap
}

/
