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
