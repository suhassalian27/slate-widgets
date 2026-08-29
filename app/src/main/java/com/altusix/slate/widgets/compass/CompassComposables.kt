package com.altusix.slate.widgets.compass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.createSupersampledCanvas
import com.altusix.slate.utils.getSafeBgColor
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius

// 1. ORBITAL PULSE COMPASS (2x2 / Minimal Floating Node Compass)
fun generateDotMatrixCompassBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#50FFFFFF")

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val size = minOf(w, h)
        val leftX = (w - size) / 2f
        val topY = (h - size) / 2f
        RectF(leftX, topY, leftX + size, topY + size)
    }

    val minDim = minOf(cardRect.width(), cardRect.height())
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val cx = cardRect.centerX()
    val cy = cardRect.centerY()
    val trackRadius = minDim * 0.36f

    val compassState = CompassState.readCurrentHeading(context, widgetId)
    val isActive = compassState.isActive
    val azimuth = if (isActive) compassState.azimuthDegrees else 0f

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1.0f
    }
    canvas.drawCircle(cx, cy, trackRadius, trackPaint)

    val coreRadius = trackRadius * 0.35f
    val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isActive) Color.argb(30, Color.red(accentColorInt), Color.green(accentColorInt), Color.blue(accentColorInt)) else Color.argb(15, 255, 255, 255)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, coreRadius, corePaint)

    canvas.save()
    if (isActive) {
        canvas.rotate(-azimuth, cx, cy)
    }

    val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isActive) accentColorInt else secondaryText
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1.5f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(cx, cy, cx, cy - trackRadius, rayPaint)

    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        style = Paint.Style.FILL
    }
    val tickDotR = scaleFactor * 1.8f
    canvas.drawCircle(cx + trackRadius, cy, tickDotR, tickPaint)
    canvas.drawCircle(cx, cy + trackRadius, tickDotR, tickPaint)
    canvas.drawCircle(cx - trackRadius, cy, tickDotR, tickPaint)

    val northNodeR = scaleFactor * 5.0f
    val northNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isActive) accentColorInt else primaryText
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy - trackRadius, northNodeR, northNodePaint)

    val cutoutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy - trackRadius, northNodeR * 0.45f, cutoutPaint)

    canvas.restore()

    val pivotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isActive) accentColorInt else primaryText
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, scaleFactor * 2.2f, pivotPaint)

    if (isActive) {
        val metaText = "${azimuth.toInt()}° ${compassState.cardinalDirection}"
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = minDim * 0.06f
            typeface = getSlateFont(context, weight = 600)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.04f
        }
        canvas.drawText(metaText, cx, cardRect.bottom - (minDim * 0.07f), metaPaint)
    }

    return bitmap
}

// 2. TACTICAL RADAR COMPASS (2x2 / Tap-Activated Tactical Radar)
fun generateTacticalRadarCompassBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#50FFFFFF")

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val size = minOf(w, h)
        val leftX = (w - size) / 2f
        val topY = (h - size) / 2f
        RectF(leftX, topY, leftX + size, topY + size)
    }

    val minDim = minOf(cardRect.width(), cardRect.height())
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val cx = cardRect.centerX()
    val cy = cardRect.centerY()
    val radius = minDim * 0.36f

    val compassState = CompassState.readCurrentHeading(context, widgetId)
    val isActive = compassState.isActive
    val azimuth = if (isActive) compassState.azimuthDegrees else 0f

    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1.0f
    }
    canvas.drawCircle(cx, cy, radius, ringPaint)

    if (isActive) {
        canvas.drawCircle(cx, cy, radius * 0.65f, ringPaint)
        canvas.drawLine(cx - radius, cy, cx + radius, cy, ringPaint)
        canvas.drawLine(cx, cy - radius, cx, cy + radius, ringPaint)

        val cardinalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = minDim * 0.075f
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }

        val northPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            textSize = minDim * 0.085f
            typeface = getSlateFont(context, weight = 800)
            textAlign = Paint.Align.CENTER
        }

        val labelOffset = radius * 0.82f
        val cardinals = listOf(
            "N" to 0f,
            "E" to 90f,
            "S" to 180f,
            "W" to 270f
        )

        for ((label, deg) in cardinals) {
            val angleRad = Math.toRadians((deg - azimuth - 90).toDouble())
            val lx = cx + (Math.cos(angleRad) * labelOffset).toFloat()
            val ly = cy + (Math.sin(angleRad) * labelOffset).toFloat()

            val paint = if (label == "N") northPaint else cardinalPaint
            val fm = paint.fontMetrics
            val textY = ly - ((fm.descent + fm.ascent) / 2f)

            canvas.drawText(label, lx, textY, paint)
        }
    }

    val pointerPath = Path().apply {
        moveTo(cx, cy - (minDim * 0.07f))
        lineTo(cx - (minDim * 0.03f), cy + (minDim * 0.045f))
        lineTo(cx, cy + (minDim * 0.02f))
        lineTo(cx + (minDim * 0.03f), cy + (minDim * 0.045f))
        close()
    }
    val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isActive) accentColorInt else secondaryText
        style = Paint.Style.FILL
    }
    canvas.drawPath(pointerPath, pointerPaint)

    if (isActive) {
        val metaText = "${azimuth.toInt()}° ${compassState.cardinalDirection}"
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = minDim * 0.06f
            typeface = getSlateFont(context, weight = 600)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.04f
        }
        canvas.drawText(metaText, cx, cardRect.bottom - (minDim * 0.07f), metaPaint)
    }

    return bitmap
}

// 3. MINIMALIST BEZEL COMPASS (2x2 / Tap-Activated Ring Compass)
fun generateMinimalistBezelCompassBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#50FFFFFF")

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val size = minOf(w, h)
        val leftX = (w - size) / 2f
        val topY = (h - size) / 2f
        RectF(leftX, topY, leftX + size, topY + size)
    }

    val minDim = minOf(cardRect.width(), cardRect.height())
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val cx = cardRect.centerX()
    val cy = cardRect.centerY()
    val radius = minDim * 0.36f

    val compassState = CompassState.readCurrentHeading(context, widgetId)
    val isActive = compassState.isActive
    val azimuth = if (isActive) compassState.azimuthDegrees else 0f

    val dialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1.2f
    }
    canvas.drawCircle(cx, cy, radius, dialPaint)

    canvas.save()
    if (isActive) canvas.rotate(-azimuth, cx, cy)

    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1.0f
        strokeCap = Paint.Cap.ROUND
    }

    for (i in 0 until 360 step 15) {
        val angleRad = Math.toRadians((i - 90).toDouble())
        val isMajor = i % 90 == 0
        val innerR = if (isMajor) radius * 0.82f else radius * 0.90f

        val x1 = cx + (Math.cos(angleRad) * innerR).toFloat()
        val y1 = cy + (Math.sin(angleRad) * innerR).toFloat()
        val x2 = cx + (Math.cos(angleRad) * radius).toFloat()
        val y2 = cy + (Math.sin(angleRad) * radius).toFloat()

        tickPaint.color = if (isMajor && isActive) primaryText else secondaryText
        canvas.drawLine(x1, y1, x2, y2, tickPaint)
    }

    val northNeedlePath = Path().apply {
        moveTo(cx, cy - radius + (minDim * 0.02f))
        lineTo(cx - (minDim * 0.03f), cy)
        lineTo(cx + (minDim * 0.03f), cy)
        close()
    }
    val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isActive) accentColorInt else secondaryText
        style = Paint.Style.FILL
    }
    canvas.drawPath(northNeedlePath, needlePaint)
    canvas.restore()

    if (isActive) {
        val r = Color.red(accentColorInt) / 255f
        val g = Color.green(accentColorInt) / 255f
        val b = Color.blue(accentColorInt) / 255f
        val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
        val badgeTextColor = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

        val headingText = "${azimuth.toInt()}° ${compassState.cardinalDirection}"

        val badgeH = minDim * 0.16f
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeTextColor
            textSize = badgeH * 0.48f
            typeface = getSlateFont(context, weight = 600)
            textAlign = Paint.Align.CENTER
        }

        val textW = headingPaint.measureText(headingText)
        val badgeW = maxOf(minDim * 0.34f, textW + (minDim * 0.08f))
        val badgeRect = RectF(cx - badgeW / 2f, cy - badgeH / 2f, cx + badgeW / 2f, cy + badgeH / 2f)

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(badgeRect, badgeH / 2f, badgeH / 2f, badgePaint)

        val bounds = Rect()
        headingPaint.getTextBounds(headingText, 0, headingText.length, bounds)
        canvas.drawText(headingText, cx, cy + (bounds.height() / 2f), headingPaint)
    }

    return bitmap
}

// 4. HORIZONTAL PILL COMPASS (2x1 / Tap-Activated Precision Tape Strip)
fun generateHorizontalPillCompassBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#60FFFFFF")

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val targetRatio = 2.0f
        var cardH = h
        var cardW = cardH * targetRatio

        if (cardW > w) {
            cardW = w
            cardH = cardW / targetRatio
        }

        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val pillH = cardRect.height()
    val pillRadius = pillH / 2f
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, pillRadius, pillRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(15, 0, 0, 0) else Color.argb(25, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1.0f
    }
    canvas.drawRoundRect(cardRect, pillRadius, pillRadius, borderPaint)

    val compassState = CompassState.readCurrentHeading(context, widgetId)
    val isActive = compassState.isActive
    val azimuth = if (isActive) compassState.azimuthDegrees else 0f

    val cx = cardRect.centerX()
    val cyBase = cardRect.centerY() - (if (isActive) pillH * 0.06f else 0f)

    val yBase = cyBase + (pillH * 0.04f)

    val pxPerDegree = pillH * 0.11f
    val tapeLeft = cardRect.left + (pillH * 0.35f)
    val tapeRight = cardRect.right - (pillH * 0.35f)

    canvas.save()
    canvas.clipRect(tapeLeft, cardRect.top, tapeRight, cardRect.bottom)

    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = pillH * 0.12f
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val startDeg = (azimuth - 90).toInt()
    val endDeg = (azimuth + 90).toInt()

    for (deg in startDeg..endDeg) {
        val normalizedDeg = (deg % 360 + 360) % 360
        val x = cx + (deg - azimuth) * pxPerDegree

        if (x in tapeLeft..tapeRight) {
            val isCardinal = normalizedDeg % 90 == 0
            val is30Deg = normalizedDeg % 30 == 0
            val is10Deg = normalizedDeg % 10 == 0

            val tickLen = when {
                isCardinal -> pillH * 0.22f
                is30Deg -> pillH * 0.16f
                is10Deg -> pillH * 0.10f
                else -> pillH * 0.06f
            }

            val yTickTop = yBase - tickLen

            tickPaint.color = when {
                isCardinal && isActive -> accentColorInt
                isCardinal -> primaryText
                is30Deg -> primaryText
                else -> secondaryText
            }

            tickPaint.strokeWidth = when {
                isCardinal -> scaleFactor * 2.0f
                is30Deg -> scaleFactor * 1.5f
                else -> scaleFactor * 1.0f
            }

            canvas.drawLine(x, yBase, x, yTickTop, tickPaint)

            if (is30Deg) {
                val labelStr = when (normalizedDeg) {
                    0 -> "N"
                    90 -> "E"
                    180 -> "S"
                    270 -> "W"
                    else -> "$normalizedDeg°"
                }

                labelPaint.color = when {
                    normalizedDeg == 0 && isActive -> accentColorInt
                    isCardinal -> primaryText
                    else -> secondaryText
                }

                val labelY = yBase - (pillH * 0.28f)
                canvas.drawText(labelStr, x, labelY, labelPaint)
            }
        }
    }
    canvas.restore()

    val reticlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isActive) accentColorInt else primaryText
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 2.2f
        strokeCap = Paint.Cap.ROUND
    }
    val reticleYTop = yBase - (pillH * 0.04f)
    val reticleYBottom = yBase + (pillH * 0.14f)
    canvas.drawLine(cx, reticleYBottom, cx, reticleYTop, reticlePaint)

    val reticleDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isActive) accentColorInt else primaryText
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, reticleYBottom + (scaleFactor * 1.5f), scaleFactor * 2.2f, reticleDotPaint)

    if (isActive) {
        val r = Color.red(accentColorInt) / 255f
        val g = Color.green(accentColorInt) / 255f
        val b = Color.blue(accentColorInt) / 255f
        val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
        val badgeTextColor = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

        val headingText = "${azimuth.toInt()}° ${compassState.cardinalDirection}"

        val badgeH = pillH * 0.22f
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeTextColor
            textSize = badgeH * 0.50f
            typeface = getSlateFont(context, weight = 800)
            textAlign = Paint.Align.CENTER
        }

        val textW = headingPaint.measureText(headingText)
        val badgeW = textW + (pillH * 0.22f)
        val badgeY = cardRect.bottom - (pillH * 0.18f)
        val badgeRect = RectF(cx - badgeW / 2f, badgeY - badgeH / 2f, cx + badgeW / 2f, badgeY + badgeH / 2f)

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(badgeRect, badgeH / 2f, badgeH / 2f, badgePaint)

        val bounds = Rect()
        headingPaint.getTextBounds(headingText, 0, headingText.length, bounds)
        canvas.drawText(headingText, cx, badgeY + (bounds.height() / 2f), headingPaint)
    }

    return bitmap
}

// 5. PURE CIRCLE COMPASS (2x2 / Minimal Circular Dial with North Triangle & Center Heading)
fun generatePureCircleCompassBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#50FFFFFF")

    val size = minOf(w, h)
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawOval(cardRect, bgPaint)

    val cx = cardRect.centerX()
    val cy = cardRect.centerY()
    val radius = size * 0.44f

    val compassState = CompassState.readCurrentHeading(context, widgetId)
    val isActive = compassState.isActive
    val azimuth = if (isActive) compassState.azimuthDegrees else 0f

    val innerRingRadius = radius * 0.82f
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1.0f
    }
    canvas.drawCircle(cx, cy, innerRingRadius, ringPaint)

    canvas.save()
    if (isActive) {
        canvas.rotate(-azimuth, cx, cy)
    }

    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    for (i in 0 until 360 step 15) {
        if (i == 0) continue

        val angleRad = Math.toRadians((i - 90).toDouble())
        val isMajor = i % 90 == 0
        val outerR = radius * 0.82f
        val innerR = if (isMajor) radius * 0.68f else radius * 0.74f

        val x1 = cx + (Math.cos(angleRad) * innerR).toFloat()
        val y1 = cy + (Math.sin(angleRad) * innerR).toFloat()
        val x2 = cx + (Math.cos(angleRad) * outerR).toFloat()
        val y2 = cy + (Math.sin(angleRad) * outerR).toFloat()

        tickPaint.color = if (isMajor && isActive) primaryText else secondaryText
        tickPaint.strokeWidth = if (isMajor) scaleFactor * 2.0f else scaleFactor * 1.2f
        canvas.drawLine(x1, y1, x2, y2, tickPaint)
    }

    val triangleH = radius * 0.18f
    val triangleW = radius * 0.16f
    val triTopY = cy - (radius * 0.84f)
    val triBottomY = triTopY + triangleH

    val northTrianglePath = Path().apply {
        moveTo(cx, triTopY)
        lineTo(cx - triangleW / 2f, triBottomY)
        lineTo(cx + triangleW / 2f, triBottomY)
        close()
    }

    val trianglePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isActive) accentColorInt else secondaryText
        style = Paint.Style.FILL
    }
    canvas.drawPath(northTrianglePath, trianglePaint)

    canvas.restore()

    val dirLetter = compassState.cardinalDirection
    val degText = "${azimuth.toInt()}°"

    val letterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isActive) primaryText else secondaryText
        textSize = radius * 0.42f
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val degreePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = radius * 0.22f
        typeface = getSlateFont(context, weight = 600)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.05f
    }

    if (isActive) {
        val letterBounds = Rect()
        letterPaint.getTextBounds(dirLetter, 0, dirLetter.length, letterBounds)

        val degreeBounds = Rect()
        degreePaint.getTextBounds(degText, 0, degText.length, degreeBounds)

        val gap = radius * 0.06f
        val totalH = letterBounds.height() + gap + degreeBounds.height()
        val startY = cy - (totalH / 2f) + letterBounds.height()

        canvas.drawText(dirLetter, cx, startY, letterPaint)
        canvas.drawText(degText, cx, startY + gap + degreeBounds.height(), degreePaint)
    } else {
        val letterBounds = Rect()
        letterPaint.getTextBounds("N", 0, 1, letterBounds)
        canvas.drawText("N", cx, cy + (letterBounds.height() / 2f), letterPaint)
    }

    return bitmap
}