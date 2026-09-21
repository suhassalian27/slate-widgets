package com.altusix.slate.widgets.bluetooth

import android.content.Context
import android.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.altusix.slate.core.theme.SlateColors
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius
import com.altusix.slate.utils.createSupersampledCanvas
import com.altusix.slate.utils.getSafeBgColor
import kotlin.div
import kotlin.text.toFloat
import kotlin.times

// =========================================================================
// CANVAS BITMAP GENERATORS FOR NATIVE REMOTE VIEWS
// =========================================================================

/**
 * 1. Bluetooth Earbuds Card (2x2 Square / Adaptive Wide)
 * Proportional lockup with expanded max size limits.
 */
fun generateEarbudsSquareBitmap(
    context: Context,
    deviceData: BluetoothDeviceData,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = slateConfig.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(slateConfig)
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val trackColor = if (isLight) 0x14000000 else 0x1EFFFFFF

    // 1. Unified 1.5dp outer boundary margin
    val margin = scaleFactor * 1.5f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val targetRatio = 1.0f
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardW = cardRect.width()
    val cardH = cardRect.height()
    val aspect = cardW / cardH.coerceAtLeast(1f)
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)

    // 2. Card Surface & Edge Border
    val alphaInt = (slateConfig.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(
            alphaInt,
            android.graphics.Color.red(bgColor),
            android.graphics.Color.green(bgColor),
            android.graphics.Color.blue(bgColor)
        )
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x12000000 else 0x1AFFFFFF
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.8f
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, borderPaint)

    canvas.save()
    val clipPath = Path().apply {
        addRoundRect(cardRect, cardCornerRadius, cardCornerRadius, Path.Direction.CW)
    }
    canvas.clipPath(clipPath)

    // Status & Label Setup
    val statusText = when {
        deviceData.needsPermission -> "PERMISSION"
        deviceData.isConnected -> "CONNECTED"
        else -> "DISCONNECTED"
    }

    val statusColor = when {
        deviceData.needsPermission -> 0xFFFF9500.toInt()
        deviceData.isConnected -> accentColor
        else -> secondaryTextColor
    }

    val displayName = when {
        deviceData.needsPermission -> "Tap to allow access"
        deviceData.isConnected -> deviceData.deviceName.ifEmpty { "Bluetooth Audio" }
        else -> "No device connected"
    }

    val isWideLayout = aspect >= 1.45f

    // =========================================================================
    // BRANCH 1: WIDE RESPONSIVE MODE (2x1, 3x1, 4x1 -> aspect >= 1.45)
    // =========================================================================
    if (isWideLayout) {
        val padX = (cardW * 0.08f).coerceIn(scaleFactor * 14f, scaleFactor * 26f)
        val padY = (cardH * 0.12f).coerceIn(scaleFactor * 8f, scaleFactor * 18f)

        // Right Zone: Earbuds Graphic
        val rightZoneW = cardW * 0.38f
        val boxW = rightZoneW - padX
        val boxH = cardH - (padY * 2f)

        val baseScale = minOf(boxW / (64f * scaleFactor), boxH / (46f * scaleFactor)).coerceIn(0.40f, 2.2f)
        val earbudSpacing = 14f * scaleFactor * baseScale

        val earbudCx = cardRect.right - padX - (boxW / 2f)
        val earbudCy = cardRect.centerY()

        drawScaledEarbudGraphic(
            canvas = canvas,
            cx = earbudCx - earbudSpacing,
            cy = earbudCy,
            angleDeg = -22f,
            scale = baseScale,
            accentColor = accentColor,
            isLightMode = isLight,
            density = scaleFactor
        )
        drawScaledEarbudGraphic(
            canvas = canvas,
            cx = earbudCx + earbudSpacing,
            cy = earbudCy,
            angleDeg = 22f,
            scale = baseScale,
            accentColor = accentColor,
            isLightMode = isLight,
            density = scaleFactor
        )

        // Left Zone: Centered Stack
        val textLeft = cardRect.left + padX
        val maxTextW = (earbudCx - (boxW / 2f) - textLeft - (scaleFactor * 10f)).coerceAtLeast(1f)

        val statusSize = (cardH * 0.13f).coerceIn(scaleFactor * 10f, scaleFactor * 16f)
        val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statusColor
            textSize = statusSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.LEFT
        }
        val fmStatus = statusPaint.fontMetrics
        val statusH = fmStatus.descent - fmStatus.ascent

        var nameSize = (cardH * 0.20f).coerceIn(scaleFactor * 13f, scaleFactor * 24f)
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = nameSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.LEFT
        }
        while (namePaint.measureText(displayName) > maxTextW && nameSize > scaleFactor * 10f) {
            nameSize -= scaleFactor * 0.5f
            namePaint.textSize = nameSize
        }
        val fmName = namePaint.fontMetrics
        val nameH = fmName.descent - fmName.ascent

        val barH = (cardH * 0.09f).coerceIn(scaleFactor * 5f, scaleFactor * 8.5f)
        val barW = maxTextW.coerceAtMost(cardW * 0.44f)

        val gap1 = scaleFactor * 4f
        val gap2 = scaleFactor * 8f
        val totalLeftStackH = statusH + gap1 + nameH + gap2 + barH

        val stackTop = cardRect.top + (cardH - totalLeftStackH) / 2f

        val statusY = stackTop - fmStatus.ascent
        val tagLine = if (deviceData.isConnected && deviceData.batteryLevel >= 0) {
            "$statusText  •  ${deviceData.batteryLevel}%"
        } else {
            statusText
        }
        canvas.drawText(tagLine, textLeft, statusY, statusPaint)

        val nameY = stackTop + statusH + gap1 - fmName.ascent
        canvas.drawText(displayName, textLeft, nameY, namePaint)

        val barTop = stackTop + statusH + gap1 + nameH + gap2
        val trackRect = RectF(textLeft, barTop, textLeft + barW, barTop + barH)
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = trackColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(trackRect, barH / 2f, barH / 2f, trackPaint)

        if (deviceData.isConnected && deviceData.batteryLevel > 0) {
            val fillW = barW * (deviceData.batteryLevel.coerceIn(0, 100) / 100f)
            val fillRect = RectF(textLeft, barTop, textLeft + fillW.coerceAtLeast(barH), barTop + barH)
            val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(fillRect, barH / 2f, barH / 2f, activePaint)
        }

        // =========================================================================
        // BRANCH 2: SQUARE & TALL MODE (Expanded Max Size Bounds)
        // =========================================================================
    } else {
        val cx = cardRect.centerX()
        // Raised ceiling from 240f to 340f for larger, bolder widgets
        val refDim = minOf(cardW, cardH).coerceAtMost(scaleFactor * 340f)
        val padX = cardW * 0.08f
        val maxTextW = cardW - (padX * 2f)

        // 1. Scaled Earbuds Graphic (42% of reference dimension)
        val budH = refDim * 0.42f
        val baseScale = minOf((cardW * 0.58f) / (64f * scaleFactor), budH / (46f * scaleFactor)).coerceIn(0.40f, 2.4f)
        val earbudSpacing = 14f * scaleFactor * baseScale

        // 2. Status Text
        val squareStatusText = if (deviceData.isConnected && deviceData.batteryLevel >= 0) {
            "${deviceData.batteryLevel}%"
        } else {
            statusText
        }

        var statusSize = (refDim * 0.095f).coerceIn(scaleFactor * 10f, scaleFactor * 20f)
        val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statusColor
            textSize = statusSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }
        while (statusPaint.measureText(squareStatusText) > maxTextW && statusSize > scaleFactor * 8f) {
            statusSize -= scaleFactor * 0.5f
            statusPaint.textSize = statusSize
        }
        val fmStatus = statusPaint.fontMetrics
        val statusH = fmStatus.descent - fmStatus.ascent

        // 3. Subtitle Line (Device Name)
        var nameSize = (refDim * 0.072f).coerceIn(scaleFactor * 9f, scaleFactor * 15f)
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryTextColor
            textSize = nameSize
            typeface = getSlateFont(context, weight = 500)
            textAlign = Paint.Align.CENTER
        }
        while (namePaint.measureText(displayName) > maxTextW && nameSize > scaleFactor * 7.5f) {
            nameSize -= scaleFactor * 0.5f
            namePaint.textSize = nameSize
        }
        val fmName = namePaint.fontMetrics
        val nameH = fmName.descent - fmName.ascent

        // 4. Battery Bar
        val barH = (refDim * 0.048f).coerceIn(scaleFactor * 4.5f, scaleFactor * 8.5f)
        val barW = (refDim * 0.60f).coerceIn(scaleFactor * 55f, maxTextW * 0.88f)

        // 5. Proportional Gaps
        val gap1 = refDim * 0.038f // Buds -> Status
        val gap2 = refDim * 0.016f // Status -> Name
        val gap3 = refDim * 0.042f // Name -> Bar

        val totalClusterH = budH + gap1 + statusH + gap2 + nameH + gap3 + barH
        val clusterTop = cardRect.top + (cardH - totalClusterH) / 2f

        // Draw Earbuds
        val earbudCy = clusterTop + (budH / 2f)
        drawScaledEarbudGraphic(
            canvas = canvas,
            cx = cx - earbudSpacing,
            cy = earbudCy,
            angleDeg = -22f,
            scale = baseScale,
            accentColor = accentColor,
            isLightMode = isLight,
            density = scaleFactor
        )
        drawScaledEarbudGraphic(
            canvas = canvas,
            cx = cx + earbudSpacing,
            cy = earbudCy,
            angleDeg = 22f,
            scale = baseScale,
            accentColor = accentColor,
            isLightMode = isLight,
            density = scaleFactor
        )

        // Draw Status Text
        val statusY = clusterTop + budH + gap1 - fmStatus.ascent
        canvas.drawText(squareStatusText, cx, statusY, statusPaint)

        // Draw Device Name
        val nameY = clusterTop + budH + gap1 + statusH + gap2 - fmName.ascent
        canvas.drawText(displayName, cx, nameY, namePaint)

        // Draw Battery Bar
        val barTop = clusterTop + budH + gap1 + statusH + gap2 + nameH + gap3
        val barLeft = cx - (barW / 2f)
        val trackRect = RectF(barLeft, barTop, barLeft + barW, barTop + barH)
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = trackColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(trackRect, barH / 2f, barH / 2f, trackPaint)

        if (deviceData.isConnected && deviceData.batteryLevel > 0) {
            val fillW = barW * (deviceData.batteryLevel.coerceIn(0, 100) / 100f)
            val fillRect = RectF(barLeft, barTop, barLeft + fillW.coerceAtLeast(barH), barTop + barH)
            val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(fillRect, barH / 2f, barH / 2f, activePaint)
        }
    }

    canvas.restore()
    return bitmap
}

/**
 * 2. Bluetooth Circular Dial Widget (Strict 1:1 Circle)
 * Audio-segmented 36-dash radial gauge, inner precision halo,
 * centered TWS earbud graphics, percentage readout, and multi-line status text.
 */
fun generateBluetoothCircularDialBitmap(
    context: Context,
    deviceData: BluetoothDeviceData,
    slateConfig: SlateWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val margin = scaleFactor * 1.5f
    val cardSize = minOf(w, h) - (margin * 2f)
    val cx = w / 2f
    val cy = h / 2f

    val isLight = slateConfig.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(slateConfig)
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val dimColor = if (isLight) 0x14000000 else 0x1CFFFFFF

    val alphaInt = (slateConfig.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(
            alphaInt,
            android.graphics.Color.red(bgColor),
            android.graphics.Color.green(bgColor),
            android.graphics.Color.blue(bgColor)
        )
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, cardSize / 2f, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x12000000 else 0x1AFFFFFF
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.8f
    }
    canvas.drawCircle(cx, cy, cardSize / 2f, borderPaint)

    // Gauge and Halo Logic
    val dynamicScale = (cardSize / 300f).coerceAtLeast(0.5f)
    val ringMargin = cardSize * 0.055f
    val outerRadius = (cardSize / 2f) - ringMargin

    val totalDashes = 36
    val activeDashes = if (deviceData.isConnected) {
        ((deviceData.batteryLevel.coerceIn(0, 100) / 100f) * totalDashes).toInt()
    } else {
        0
    }

    val dashLength = 11f * dynamicScale
    val dashStrokeWidth = 3.6f * dynamicScale
    val dashOuterR = outerRadius - (dashStrokeWidth / 2f)
    val dashInnerR = dashOuterR - dashLength

    val activeDashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        strokeWidth = dashStrokeWidth
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    val inactiveDashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dimColor
        strokeWidth = dashStrokeWidth
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    for (i in 0 until totalDashes) {
        val angleDeg = -90f + (i * (360f / totalDashes))
        val angleRad = Math.toRadians(angleDeg.toDouble())

        val startX = cx + (dashInnerR * Math.cos(angleRad)).toFloat()
        val startY = cy + (dashInnerR * Math.sin(angleRad)).toFloat()
        val endX = cx + (dashOuterR * Math.cos(angleRad)).toFloat()
        val endY = cy + (dashOuterR * Math.sin(angleRad)).toFloat()

        val paint = if (i < activeDashes) activeDashPaint else inactiveDashPaint
        canvas.drawLine(startX, startY, endX, endY, paint)
    }

    val haloRadius = dashInnerR - (8f * dynamicScale)
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x18000000 else 0x22FFFFFF
        strokeWidth = 1.4f * dynamicScale
        style = Paint.Style.STROKE
    }
    canvas.drawCircle(cx, cy, haloRadius, haloPaint)

    val notchDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (deviceData.isConnected) accentColor else (if (isLight) 0x30000000 else 0x40FFFFFF)
        style = Paint.Style.FILL
    }
    val dotRadius = 2.2f * dynamicScale
    for (angle in listOf(-90f, 0f, 90f, 180f)) {
        val rad = Math.toRadians(angle.toDouble())
        val notchX = cx + (haloRadius * Math.cos(rad)).toFloat()
        val notchY = cy + (haloRadius * Math.sin(rad)).toFloat()
        canvas.drawCircle(notchX, notchY, dotRadius, notchDotPaint)
    }

    // Earbud & Typography Layout
    val earbudScale = (cardSize / (140f * scaleFactor)).coerceAtLeast(0.40f)
    val earbudSpacing = cardSize * 0.12f

    val pctTextSize = cardSize * 0.12f
    val statusTextSize = cardSize * 0.042f

    val gapBudsToPct = cardSize * 0.04f
    val gapPctToStatus = cardSize * 0.04f
    val lineSpacing = cardSize * 0.050f

    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()

    val (line1, line2) = when {
        deviceData.needsPermission -> Pair("PERMISSION", "REQUIRED")
        deviceData.isConnected -> splitToTwoLines(deviceData.deviceName.uppercase(), maxCharsPerLine = 15)
        else -> Pair("NO DEVICE", "CONNECTED")
    }
    val hasLine2 = !line2.isNullOrEmpty()

    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = pctTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = statusTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.05f
    }

    val pctOpticalH = pctTextSize * 0.75f
    val statusOpticalH = statusTextSize * 0.75f

    val earbudTopExtent = 18f * scaleFactor * earbudScale
    val earbudBottomExtent = 23f * scaleFactor * earbudScale

    val totalBlockH = earbudTopExtent + earbudBottomExtent +
            gapBudsToPct + pctOpticalH +
            gapPctToStatus + statusOpticalH +
            (if (hasLine2) lineSpacing else 0f)

    val blockOffsetY = cardSize * 0.025f
    val blockTop = cy - (totalBlockH / 2f) + blockOffsetY

    val earbudCy = blockTop + earbudTopExtent
    val pctBaselineY = earbudCy + earbudBottomExtent + gapBudsToPct + pctOpticalH
    val line1BaselineY = pctBaselineY + gapPctToStatus + statusOpticalH

    // 1. Draw Earbuds
    drawScaledEarbudGraphic(
        canvas = canvas,
        cx = cx - earbudSpacing,
        cy = earbudCy,
        angleDeg = -20f,
        scale = earbudScale,
        accentColor = accentColor,
        isLightMode = isLight,
        density = scaleFactor
    )
    drawScaledEarbudGraphic(
        canvas = canvas,
        cx = cx + earbudSpacing,
        cy = earbudCy,
        angleDeg = 20f,
        scale = earbudScale,
        accentColor = accentColor,
        isLightMode = isLight,
        density = scaleFactor
    )

    // 2. Draw Percentage
    val pctText = when {
        deviceData.needsPermission -> "GRANT"
        deviceData.isConnected -> "${deviceData.batteryLevel}%"
        else -> "--%"
    }
    canvas.drawText(pctText, cx, pctBaselineY, pctPaint)

    // 3. Draw Status Lines
    canvas.drawText(line1, cx, line1BaselineY, statusPaint)
    if (hasLine2) {
        val line2BaselineY = line1BaselineY + lineSpacing
        canvas.drawText(line2!!, cx, line2BaselineY, statusPaint)
    }

    return bitmap
}

/**
 * 3. Bluetooth Ring Widget (Strict 1:1 Circle)
 * Outer battery progress ring, inner background circle,
 * TWS earbud graphics, status/percentage readout, and multi-line device label.
 */
fun generateBluetoothRingBitmap(
    context: Context,
    deviceData: BluetoothDeviceData,
    slateConfig: SlateWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val margin = scaleFactor * 1.5f
    val cardSize = minOf(w, h) - (margin * 2f)
    val cx = w / 2f
    val cy = h / 2f

    val isLight = slateConfig.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(slateConfig)
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val dimColor = if (isLight) 0x14000000 else 0x22FFFFFF

    // 1. Draw Outer Ring
    val ringStrokeWidth = cardSize * 0.13f
    val ringRadius = (cardSize / 2f) - (ringStrokeWidth / 2f) - (scaleFactor * 1.5f)

    val ringRect = RectF(
        cx - ringRadius,
        cy - ringRadius,
        cx + ringRadius,
        cy + ringRadius
    )

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dimColor
        style = Paint.Style.STROKE
        strokeWidth = ringStrokeWidth
    }
    canvas.drawArc(ringRect, 0f, 360f, false, trackPaint)

    if (deviceData.isConnected) {
        val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.STROKE
            strokeWidth = ringStrokeWidth
            strokeCap = Paint.Cap.ROUND
        }
        val fillProgress = deviceData.batteryLevel.coerceIn(0, 100) / 100f
        val sweepAngle = fillProgress * 360f
        if (sweepAngle > 0f) {
            canvas.drawArc(ringRect, -90f, sweepAngle, false, activePaint)
        }
    }

    // 2. Draw Inner Background
    val innerRadius = ringRadius - (ringStrokeWidth / 2f)
    val alphaInt = (slateConfig.opacity.coerceIn(0f, 1f) * 255).toInt()
    val innerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(
            alphaInt,
            android.graphics.Color.red(bgColor),
            android.graphics.Color.green(bgColor),
            android.graphics.Color.blue(bgColor)
        )
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, innerRadius, innerBgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x12000000 else 0x1AFFFFFF
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.8f
    }
    canvas.drawCircle(cx, cy, innerRadius, borderPaint)

    // Earbud & Typography Layout
    val earbudScale = (cardSize / (145f * scaleFactor)).coerceAtLeast(0.40f)
    val earbudSpacing = cardSize * 0.12f

    val pctTextSize = cardSize * 0.12f
    val statusTextSize = cardSize * 0.042f

    val elementGap = cardSize * 0.03f
    val lineSpacing = cardSize * 0.050f

    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()

    val (line1, line2) = when {
        deviceData.needsPermission -> Pair("PERMISSION", "REQUIRED")
        deviceData.isConnected -> splitToTwoLines(deviceData.deviceName.uppercase(), maxCharsPerLine = 15)
        else -> Pair("NO DEVICE", "CONNECTED")
    }
    val hasLine2 = !line2.isNullOrEmpty()

    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = pctTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = statusTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.05f
    }

    val pctOpticalH = pctTextSize * 0.75f
    val statusOpticalH = statusTextSize * 0.75f

    val earbudTopExtent = 18f * scaleFactor * earbudScale
    val earbudBottomExtent = 23f * scaleFactor * earbudScale

    val totalBlockH = earbudTopExtent + earbudBottomExtent +
            elementGap + pctOpticalH +
            elementGap + statusOpticalH +
            (if (hasLine2) lineSpacing else 0f)

    val blockOffsetY = cardSize * 0.025f
    val blockTop = cy - (totalBlockH / 2f) + blockOffsetY

    val earbudCy = blockTop + earbudTopExtent
    val pctBaselineY = earbudCy + earbudBottomExtent + elementGap + pctOpticalH
    val line1BaselineY = pctBaselineY + elementGap + statusOpticalH

    // 1. Draw Earbuds
    drawScaledEarbudGraphic(
        canvas = canvas,
        cx = cx - earbudSpacing,
        cy = earbudCy,
        angleDeg = -20f,
        scale = earbudScale,
        accentColor = accentColor,
        isLightMode = isLight,
        density = scaleFactor
    )
    drawScaledEarbudGraphic(
        canvas = canvas,
        cx = cx + earbudSpacing,
        cy = earbudCy,
        angleDeg = 20f,
        scale = earbudScale,
        accentColor = accentColor,
        isLightMode = isLight,
        density = scaleFactor
    )

    // 2. Draw Percentage
    val pctText = when {
        deviceData.needsPermission -> "GRANT"
        deviceData.isConnected -> "${deviceData.batteryLevel}%"
        else -> "--%"
    }
    canvas.drawText(pctText, cx, pctBaselineY, pctPaint)

    // 3. Draw Status Lines
    canvas.drawText(line1, cx, line1BaselineY, statusPaint)
    if (hasLine2) {
        val line2BaselineY = line1BaselineY + lineSpacing
        canvas.drawText(line2!!, cx, line2BaselineY, statusPaint)
    }

    return bitmap
}

/**
 * 4. Bluetooth Earbuds Volume Control Widget (Strict 1:1 Fixed Square)
 */
fun generateEarbudsVolumeControlBitmap(
    context: Context,
    deviceData: BluetoothDeviceData,
    slateConfig: SlateWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)

    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = slateConfig.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(slateConfig)
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val trackColor = if (isLight) 0x14000000 else 0x1EFFFFFF

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)

    // Strict 1:1 square bounds
    val margin = scaleFactor * 1.5f
    val cardSize = minOf(w, h) - (margin * 2f)
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f
    val rect = RectF(leftX, topY, leftX + cardSize, topY + cardSize)

    val alphaInt = (slateConfig.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(
            alphaInt,
            android.graphics.Color.red(bgColor),
            android.graphics.Color.green(bgColor),
            android.graphics.Color.blue(bgColor)
        )
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardCornerRadius, cardCornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x12000000 else 0x1AFFFFFF
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.8f
    }
    canvas.drawRoundRect(rect, cardCornerRadius, cardCornerRadius, borderPaint)

    val paddingHorizontal = cardSize * 0.065f
    val paddingTopBottom = cardSize * 0.05f

    val contentRect = RectF(
        rect.left + paddingHorizontal,
        rect.top + paddingTopBottom,
        rect.right - paddingHorizontal,
        rect.bottom - paddingTopBottom
    )

    val maxVolumeWidth = 56f * scaleFactor
    val rightW = (contentRect.width() * 0.26f).coerceAtMost(maxVolumeWidth)
    val gap = (contentRect.width() * 0.05f).coerceIn(8f * scaleFactor, 16f * scaleFactor)

    val leftW = contentRect.width() - rightW - gap
    val leftRect = RectF(contentRect.left, contentRect.top, contentRect.left + leftW, contentRect.bottom)
    val rightRect = RectF(contentRect.right - rightW, contentRect.top, contentRect.right, contentRect.bottom)

    val leftCx = leftRect.centerX()

    val baseScale = (leftRect.width() / (64f * scaleFactor)).coerceAtLeast(0.40f)
    val statusTextSize = leftRect.width() * 0.17f
    val nameTextSize = leftRect.width() * 0.1f
    val barH = leftRect.height() * 0.09f
    val barW = leftRect.width() * 0.85f

    val gapBudsToStatus = leftRect.height() * 0.02f
    val gapStatusToName = leftRect.height() * 0.002f
    val gapNameToBar = leftRect.height() * 0.04f

    val earbudSpacing = 16f * scaleFactor * baseScale

    val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (deviceData.isConnected) accentColor else primaryTextColor
        textSize = statusTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = nameTextSize
        typeface = getSlateFont(context, weight = 400)
        textAlign = Paint.Align.CENTER
    }

    val statusMetrics = statusPaint.fontMetrics
    val nameMetrics = namePaint.fontMetrics

    val statusTextHeight = statusMetrics.descent - statusMetrics.ascent
    val nameTextHeight = nameMetrics.descent - nameMetrics.ascent

    val earbudTopExtent = 18f * scaleFactor * baseScale
    val earbudBottomExtent = 23f * scaleFactor * baseScale

    val totalBlockH = earbudTopExtent + earbudBottomExtent + gapBudsToStatus +
            statusTextHeight + gapStatusToName + nameTextHeight + gapNameToBar + barH

    val blockOffsetY = leftRect.height() * 0.025f

    val blockTop = leftRect.top + (leftRect.height() - totalBlockH) / 2f + blockOffsetY
    val earbudCy = blockTop + earbudTopExtent

    val statusTopY = earbudCy + earbudBottomExtent + gapBudsToStatus
    val statusY = statusTopY - statusMetrics.ascent

    val nameTopY = statusTopY + statusTextHeight + gapStatusToName
    val nameY = nameTopY - nameMetrics.ascent

    val barTop = nameTopY + nameTextHeight + gapNameToBar
    val barLeft = leftCx - (barW / 2f)
    val barRect = RectF(barLeft, barTop, barLeft + barW, barTop + barH)

    // Draw Left Panel Earbuds
    drawScaledEarbudGraphic(
        canvas = canvas,
        cx = leftCx - earbudSpacing,
        cy = earbudCy,
        angleDeg = -25f,
        scale = baseScale,
        accentColor = accentColor,
        isLightMode = isLight,
        density = scaleFactor
    )
    drawScaledEarbudGraphic(
        canvas = canvas,
        cx = leftCx + earbudSpacing,
        cy = earbudCy,
        angleDeg = 25f,
        scale = baseScale,
        accentColor = accentColor,
        isLightMode = isLight,
        density = scaleFactor
    )

    val statusText = when {
        deviceData.needsPermission -> "GRANT PERM"
        deviceData.isConnected -> "Connected"
        else -> "CONNECT"
    }
    canvas.drawText(statusText, leftCx, statusY, statusPaint)

    val displayName = if (deviceData.needsPermission) "tap to allow access" else deviceData.deviceName.lowercase()
    canvas.drawText(displayName.take(18), leftCx, nameY, namePaint)

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = trackColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(barRect, barH / 2f, barH / 2f, trackPaint)

    if (deviceData.isConnected) {
        val pct = (deviceData.batteryLevel.coerceIn(0, 100) / 100f)
        val activeBarRect = RectF(barLeft, barTop, barLeft + (barW * pct), barTop + barH)
        val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(activeBarRect, barH / 2f, barH / 2f, activePaint)
    }

    // Right Rect: Volume Capsule Track
    val volCapsuleRadius = rightRect.width() / 2f
    val volBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x14000000 else 0xFF222226.toInt()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rightRect, volCapsuleRadius, volCapsuleRadius, volBgPaint)

    val rawVol = deviceData.volumeLevel
    val volPct = if (rawVol <= 0) 0.35f else (rawVol.coerceIn(0, 100) / 100f)
    val fillHeight = rightRect.height() * volPct
    val fillTop = rightRect.bottom - fillHeight

    if (volPct > 0f) {
        val volFillRect = RectF(
            rightRect.left,
            fillTop,
            rightRect.right,
            rightRect.bottom
        )
        val volActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }

        canvas.save()
        val volClipPath = Path().apply {
            addRoundRect(rightRect, volCapsuleRadius, volCapsuleRadius, Path.Direction.CW)
        }
        canvas.clipPath(volClipPath)
        canvas.drawRect(volFillRect, volActivePaint)
        canvas.restore()
    }

    val plusCx = rightRect.centerX()
    val plusCy = rightRect.top + (rightRect.height() * 0.18f)

    val minusCx = rightRect.centerX()
    val minusCy = rightRect.bottom - (rightRect.height() * 0.18f)

    val iconLen = rightRect.width() * 0.18f
    val strokeW = (rightRect.width() * 0.08f).coerceIn(2.5f * scaleFactor, 4.5f * scaleFactor)

    fun getIconColor(iconCy: Float): Int {
        val isCovered = fillTop <= iconCy && volPct > 0f
        return if (isCovered) {
            val r = ((accentColor shr 16) and 0xFF) / 255f
            val g = ((accentColor shr 8) and 0xFF) / 255f
            val b = (accentColor and 0xFF) / 255f
            val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
            if (luminance > 0.5f) 0xFF121214.toInt() else android.graphics.Color.WHITE
        } else {
            if (isLight) 0xFF2C2C30.toInt() else android.graphics.Color.WHITE
        }
    }

    // Plus Icon (+)
    val plusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = getIconColor(plusCy)
        strokeWidth = strokeW
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(plusCx - iconLen, plusCy, plusCx + iconLen, plusCy, plusPaint)
    canvas.drawLine(plusCx, plusCy - iconLen, plusCx, plusCy + iconLen, plusPaint)

    // Minus Icon (-)
    val minusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = getIconColor(minusCy)
        strokeWidth = strokeW
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(minusCx - iconLen, minusCy, minusCx + iconLen, minusCy, minusPaint)

    return bitmap
}

/**
 * 5. Bluetooth Tri-Battery Studio Dock Widget (Strict 1:1 Fixed Square - Fully Proportional)
 */
fun generateBluetoothTriBatteryDockBitmap(
    context: Context,
    deviceData: BluetoothDeviceData,
    slateConfig: SlateWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)

    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = slateConfig.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(slateConfig)
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val podBgColor = if (isLight) 0x0A000000 else 0x14FFFFFF
    val trackBarColor = if (isLight) 0x12000000 else 0x1AFFFFFF

    // Standard outer boundary margin
    val margin = scaleFactor * 1.5f
    val cardSize = minOf(w, h) - (margin * 2f)
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f
    val cardRect = RectF(leftX, topY, leftX + cardSize, topY + cardSize)

    // Proportional outer card corner radius
    val cardCornerRadius = cardSize * 0.11f

    // 1. Card Surface & Edge Border
    val alphaInt = (slateConfig.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(
            alphaInt,
            android.graphics.Color.red(bgColor),
            android.graphics.Color.green(bgColor),
            android.graphics.Color.blue(bgColor)
        )
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x12000000 else 0x1AFFFFFF
        style = Paint.Style.STROKE
        strokeWidth = (cardSize * 0.005f).coerceAtLeast(scaleFactor * 0.8f)
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, borderPaint)

    // 2. Proportional Content Padding
    val padH = cardSize * 0.055f
    val padV = cardSize * 0.055f
    val contentRect = RectF(cardRect.left + padH, cardRect.top + padV, cardRect.right - padH, cardRect.bottom - padV)

    // 3. Header Row: Device Name & Status Dot (Proportional to cardSize)
    val headerTextSize = (contentRect.width() * 0.055f).coerceIn(scaleFactor * 8.5f, cardSize * 0.065f)
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = headerTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.LEFT
        letterSpacing = 0.05f
    }

    val dotRadius = headerTextSize * 0.32f
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (deviceData.isConnected) accentColor else 0xFFFF3B30.toInt()
        style = Paint.Style.FILL
    }

    val deviceTitle = if (deviceData.needsPermission) "GRANT PERMISSION" else if (deviceData.isConnected) deviceData.deviceName.uppercase() else "DISCONNECTED"

    val fmHeader = headerPaint.fontMetrics
    val headerY = contentRect.top + headerTextSize - fmHeader.descent
    canvas.drawText(deviceTitle.take(20), contentRect.left, headerY, headerPaint)
    canvas.drawCircle(contentRect.right - dotRadius, headerY - (headerTextSize * 0.32f), dotRadius, dotPaint)

    // 4. Pods Area Layout
    val headerHeight = headerTextSize + (cardSize * 0.035f)
    val podsArea = RectF(contentRect.left, contentRect.top + headerHeight, contentRect.right, contentRect.bottom)

    val podGap = podsArea.width() * 0.035f
    val topRowH = (podsArea.height() - podGap) * 0.58f
    val bottomRowH = (podsArea.height() - podGap) * 0.42f
    val podW = (podsArea.width() - podGap) / 2f
    val podRadius = cardSize * 0.045f // Proportional pod corner radius

    val podBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = podBgColor
        style = Paint.Style.FILL
    }
    val podBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x10000000 else 0x16FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = (cardSize * 0.003f).coerceAtLeast(scaleFactor * 0.75f)
    }

    val trackBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = trackBarColor
        style = Paint.Style.FILL
    }
    val activeBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }

    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = (topRowH * 0.16f).coerceIn(scaleFactor * 8.5f, cardSize * 0.045f)
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.RIGHT
    }

    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = (topRowH * 0.24f).coerceIn(scaleFactor * 12f, cardSize * 0.075f)
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.RIGHT
    }

    val earbudScale = ((topRowH * 0.48f) / (46f * scaleFactor)).coerceIn(0.40f, 3.5f)
    val miniBarH = (topRowH * 0.075f).coerceIn(scaleFactor * 4f, cardSize * 0.022f)

    // POD 1: LEFT EARBUD
    val leftPod = RectF(podsArea.left, podsArea.top, podsArea.left + podW, podsArea.top + topRowH)
    canvas.drawRoundRect(leftPod, podRadius, podRadius, podBgPaint)
    canvas.drawRoundRect(leftPod, podRadius, podRadius, podBorderPaint)

    val leftBudCx = leftPod.left + (leftPod.width() * 0.28f)
    val leftBudCy = leftPod.top + (leftPod.height() * 0.40f)

    drawScaledEarbudGraphic(
        canvas = canvas,
        cx = leftBudCx,
        cy = leftBudCy,
        angleDeg = -15f,
        scale = earbudScale,
        accentColor = accentColor,
        isLightMode = isLight,
        density = scaleFactor
    )

    val textRightX = leftPod.right - (leftPod.width() * 0.12f)
    canvas.drawText("L", textRightX, leftPod.top + (leftPod.height() * 0.30f), badgePaint)

    val leftPctText = if (deviceData.isConnected) "${deviceData.leftBattery}%" else "--"
    canvas.drawText(leftPctText, textRightX, leftPod.top + (leftPod.height() * 0.58f), pctPaint)

    val leftMiniBarRect = RectF(
        leftPod.left + (leftPod.width() * 0.10f),
        leftPod.bottom - (leftPod.height() * 0.14f) - miniBarH,
        leftPod.right - (leftPod.width() * 0.10f),
        leftPod.bottom - (leftPod.height() * 0.14f)
    )
    canvas.drawRoundRect(leftMiniBarRect, miniBarH / 2f, miniBarH / 2f, trackBarPaint)
    if (deviceData.isConnected && deviceData.leftBattery > 0) {
        val pct = deviceData.leftBattery.coerceIn(0, 100) / 100f
        val fillW = leftMiniBarRect.width() * pct
        val activeRect = RectF(leftMiniBarRect.left, leftMiniBarRect.top, leftMiniBarRect.left + fillW.coerceAtLeast(miniBarH), leftMiniBarRect.bottom)
        canvas.drawRoundRect(activeRect, miniBarH / 2f, miniBarH / 2f, activeBarPaint)
    }

    // POD 2: RIGHT EARBUD
    val rightPod = RectF(podsArea.right - podW, podsArea.top, podsArea.right, podsArea.top + topRowH)
    canvas.drawRoundRect(rightPod, podRadius, podRadius, podBgPaint)
    canvas.drawRoundRect(rightPod, podRadius, podRadius, podBorderPaint)

    val rightBudCx = rightPod.left + (rightPod.width() * 0.28f)
    val rightBudCy = rightPod.top + (rightPod.height() * 0.40f)

    drawScaledEarbudGraphic(
        canvas = canvas,
        cx = rightBudCx,
        cy = rightBudCy,
        angleDeg = 15f,
        scale = earbudScale,
        accentColor = accentColor,
        isLightMode = isLight,
        density = scaleFactor
    )

    val rightTextRightX = rightPod.right - (rightPod.width() * 0.12f)
    canvas.drawText("R", rightTextRightX, rightPod.top + (rightPod.height() * 0.30f), badgePaint)

    val rightPctText = if (deviceData.isConnected) "${deviceData.rightBattery}%" else "--"
    canvas.drawText(rightPctText, rightTextRightX, rightPod.top + (rightPod.height() * 0.58f), pctPaint)

    val rightMiniBarRect = RectF(
        rightPod.left + (rightPod.width() * 0.10f),
        rightPod.bottom - (rightPod.height() * 0.14f) - miniBarH,
        rightPod.right - (rightPod.width() * 0.10f),
        rightPod.bottom - (rightPod.height() * 0.14f)
    )
    canvas.drawRoundRect(rightMiniBarRect, miniBarH / 2f, miniBarH / 2f, trackBarPaint)
    if (deviceData.isConnected && deviceData.rightBattery > 0) {
        val pct = deviceData.rightBattery.coerceIn(0, 100) / 100f
        val fillW = rightMiniBarRect.width() * pct
        val activeRect = RectF(rightMiniBarRect.left, rightMiniBarRect.top, rightMiniBarRect.left + fillW.coerceAtLeast(miniBarH), rightMiniBarRect.bottom)
        canvas.drawRoundRect(activeRect, miniBarH / 2f, miniBarH / 2f, activeBarPaint)
    }

    // POD 3: CHARGING CASE
    val casePod = RectF(podsArea.left, podsArea.bottom - bottomRowH, podsArea.right, podsArea.bottom)
    canvas.drawRoundRect(casePod, podRadius, podRadius, podBgPaint)
    canvas.drawRoundRect(casePod, podRadius, podRadius, podBorderPaint)

    val caseScale = ((bottomRowH * 0.48f) / (20f * scaleFactor)).coerceIn(0.40f, 3.5f)
    val caseCx = casePod.left + (casePod.width() * 0.13f)
    val caseCy = casePod.centerY()

    drawScaledCaseGraphic(
        canvas = canvas,
        cx = caseCx,
        cy = caseCy,
        scale = caseScale,
        accentColor = accentColor,
        isLightMode = isLight,
        density = scaleFactor
    )

    val textStartX = casePod.left + (casePod.width() * 0.27f)

    val caseTitleSize = (bottomRowH * 0.24f).coerceIn(scaleFactor * 8.5f, cardSize * 0.045f)
    val casePctSize = (bottomRowH * 0.38f).coerceIn(scaleFactor * 11f, cardSize * 0.07f)

    val caseTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = caseTitleSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.LEFT
        letterSpacing = 0.05f
    }
    val casePctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = casePctSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.LEFT
    }

    val fmCaseTitle = caseTitlePaint.fontMetrics
    val fmCasePct = casePctPaint.fontMetrics

    val caseTextStackH = (fmCaseTitle.descent - fmCaseTitle.ascent) + (cardSize * 0.01f) + (fmCasePct.descent - fmCasePct.ascent)
    val caseStackTop = casePod.top + (casePod.height() - caseTextStackH) / 2f

    val caseTitleY = caseStackTop - fmCaseTitle.ascent
    val casePctY = caseStackTop + (fmCaseTitle.descent - fmCaseTitle.ascent) + (cardSize * 0.01f) - fmCasePct.ascent

    canvas.drawText("CASE", textStartX, caseTitleY, caseTitlePaint)
    val casePctText = if (deviceData.isConnected) "${deviceData.caseBattery}%" else "--"
    canvas.drawText(casePctText, textStartX, casePctY, casePctPaint)

    // Case Battery Bar on the Right
    val caseBarW = casePod.width() * 0.32f
    val caseBarH = (bottomRowH * 0.15f).coerceIn(scaleFactor * 4.5f, cardSize * 0.03f)
    val caseBarRect = RectF(
        casePod.right - (casePod.width() * 0.08f) - caseBarW,
        casePod.centerY() - (caseBarH / 2f),
        casePod.right - (casePod.width() * 0.08f),
        casePod.centerY() + (caseBarH / 2f)
    )
    canvas.drawRoundRect(caseBarRect, caseBarH / 2f, caseBarH / 2f, trackBarPaint)
    if (deviceData.isConnected && deviceData.caseBattery > 0) {
        val pct = deviceData.caseBattery.coerceIn(0, 100) / 100f
        val fillW = caseBarRect.width() * pct
        val activeRect = RectF(caseBarRect.left, caseBarRect.top, caseBarRect.left + fillW.coerceAtLeast(caseBarH), caseBarRect.bottom)
        canvas.drawRoundRect(activeRect, caseBarH / 2f, caseBarH / 2f, activeBarPaint)
    }

    return bitmap
}

/**
 * 6. Bluetooth Tri-Battery Circular Stage Widget (Strict 1:1 Fixed Circle)
 */
fun generateBluetoothTriBatteryCircleBitmap(
    context: Context,
    deviceData: BluetoothDeviceData,
    slateConfig: SlateWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)

    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = slateConfig.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(slateConfig)
    val accentColor = Color(slateConfig.accentColorHex).toArgb()

    val margin = scaleFactor * 1.5f
    val cardSize = minOf(w, h) - (margin * 2f)
    val cx = w / 2f
    val cy = h / 2f
    val radius = cardSize / 2f

    val alphaInt = (slateConfig.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(
            alphaInt,
            android.graphics.Color.red(bgColor),
            android.graphics.Color.green(bgColor),
            android.graphics.Color.blue(bgColor)
        )
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, radius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x12000000 else 0x1AFFFFFF
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.8f
    }
    canvas.drawCircle(cx, cy, radius, borderPaint)

    val earbudScale = (radius / (50f * scaleFactor)).coerceAtLeast(0.40f)

    drawScaledEarbudGraphic(
        canvas = canvas,
        cx = cx - (radius * 0.28f),
        cy = cy - (radius * 0.14f),
        angleDeg = -30f,
        scale = earbudScale,
        accentColor = accentColor,
        isLightMode = isLight,
        density = scaleFactor,
        flipTip = false
    )

    drawScaledEarbudGraphic(
        canvas = canvas,
        cx = cx + (radius * 0.28f),
        cy = cy - (radius * 0.14f),
        angleDeg = 30f,
        scale = earbudScale,
        accentColor = accentColor,
        isLightMode = isLight,
        density = scaleFactor,
        flipTip = true
    )

    val topMargin = radius * 0.05f
    val bottomMargin = radius * 0.0f

    val statusText = when {
        deviceData.needsPermission -> "PERMISSION"
        deviceData.isConnected -> "CONNECTED"
        else -> "DISCONNECTED"
    }

    val statusTextSize = (radius * 0.15f).coerceIn(scaleFactor * 8f, scaleFactor * 16f)
    val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (deviceData.isConnected) accentColor else 0xFFFF3B30.toInt()
        textSize = statusTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.08f
    }

    val topArcRadius = radius - topMargin - statusTextSize
    val topArcRect = RectF(cx - topArcRadius, cy - topArcRadius, cx + topArcRadius, cy + topArcRadius)

    val topPath = Path().apply {
        addArc(topArcRect, -82f, 92f)
    }
    canvas.drawTextOnPath(statusText, topPath, 0f, 0f, statusPaint)

    val lVal = if (deviceData.isConnected) "${deviceData.leftBattery}%" else "--"
    val rVal = if (deviceData.isConnected) "${deviceData.rightBattery}%" else "--"
    val cVal = if (deviceData.isConnected) "${deviceData.caseBattery}%" else "--"
    val batteryText = "L $lVal  /  R $rVal  /  C $cVal"

    val batteryTextSize = (radius * 0.12f).coerceIn(scaleFactor * 8f, scaleFactor * 15f)
    val batteryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (deviceData.isConnected) accentColor else 0xFFFF3B30.toInt()
        textSize = batteryTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.05f
    }

    val bottomArcRadius = radius - bottomMargin - batteryTextSize
    val bottomArcRect = RectF(cx - bottomArcRadius, cy - bottomArcRadius, cx + bottomArcRadius, cy + bottomArcRadius)

    val bottomPath = Path().apply {
        addArc(bottomArcRect, 205f, -110f)
    }
    canvas.drawTextOnPath(batteryText, bottomPath, 0f, 0f, batteryPaint)

    return bitmap
}

// =========================================================================
// PRIVATE GRAPHIC DRAWING HELPER
// =========================================================================

private fun drawScaledEarbudGraphic(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    angleDeg: Float,
    scale: Float,
    accentColor: Int,
    isLightMode: Boolean,
    density: Float,
    flipTip: Boolean = false
) {
    canvas.save()
    canvas.rotate(angleDeg, cx, cy)

    val sideDirection = if (flipTip) 1f else (if (angleDeg <= 0f) -1f else 1f)

    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }

    val bodyColor = if (isLightMode) Color(0xFF2C2C30).toArgb() else Color(0xFF38383E).toArgb()
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bodyColor
        style = Paint.Style.FILL
    }

    // 1. Silicone Ear Tip
    val tipRadius = 9.5f * density * scale
    val tipCx = cx + (sideDirection * -9.5f * density * scale)
    val tipCy = cy - 8f * density * scale
    canvas.drawCircle(tipCx, tipCy, tipRadius, accentPaint)

    // 2. Main Earbud Head
    val headRadius = 12.5f * density * scale
    val headCy = cy - 8f * density * scale
    canvas.drawCircle(cx, headCy, headRadius, bodyPaint)

    // 3. Earbud Stem
    val stemW = 4.8f * density * scale
    val stemH = 25f * density * scale
    val stemRect = RectF(
        cx - stemW,
        cy - 4f * density * scale,
        cx + stemW,
        cy + stemH
    )
    canvas.drawRoundRect(stemRect, stemW, stemW, bodyPaint)

    // 4. Acoustic Sensor / Cutout Detail
    val sensorW = 2.2f * density * scale
    val sensorH = 3.8f * density * scale
    val sensorCx = cx + (sideDirection * 5.5f * density * scale)
    val sensorCy = cy - 13.5f * density * scale
    val sensorRect = RectF(
        sensorCx - sensorW,
        sensorCy - sensorH,
        sensorCx + sensorW,
        sensorCy + sensorH
    )
    canvas.drawRoundRect(sensorRect, sensorW, sensorW, accentPaint)

    // 5. Bottom Stem Accent / Charging Contact Bar
    val stripW = 1.6f * density * scale
    val stripH = 5.5f * density * scale
    val stripCy = cy + stemH - (5.5f * density * scale)
    val stripRect = RectF(
        cx - stripW,
        stripCy - (stripH / 2f),
        cx + stripW,
        stripCy + (stripH / 2f)
    )
    canvas.drawRoundRect(stripRect, stripW, stripW, accentPaint)

    canvas.restore()
}

// =========================================================================
// PRIVATE GRAPHIC DRAWING HELPERS
// =========================================================================

private fun drawScaledCaseGraphic(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    scale: Float,
    accentColor: Int,
    isLightMode: Boolean,
    density: Float
) {
    val bodyColor = if (isLightMode) Color(0xFF2C2C30).toArgb() else Color(0xFF38383E).toArgb()
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bodyColor
        style = Paint.Style.FILL
    }
    val seamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color(0xFF161618).toArgb()
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density * scale
    }
    val ledPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }

    val caseW = 24f * density * scale
    val caseH = 17f * density * scale
    val caseRect = RectF(cx - (caseW / 2f), cy - (caseH / 2f), cx + (caseW / 2f), cy + (caseH / 2f))
    canvas.drawRoundRect(caseRect, 5f * density * scale, 5f * density * scale, bodyPaint)

    val seamY = cy - (caseH * 0.18f)
    canvas.drawLine(caseRect.left + (2f * density * scale), seamY, caseRect.right - (2f * density * scale), seamY, seamPaint)

    val ledY = cy + (caseH * 0.22f)
    canvas.drawCircle(cx, ledY, 1.6f * density * scale, ledPaint)
}

private fun splitToTwoLines(text: String, maxCharsPerLine: Int): Pair<String, String?> {
    val words = text.trim().split("\\s+".toRegex())
    if (words.size <= 1 || text.length <= maxCharsPerLine) {
        return Pair(text.take(maxCharsPerLine), null)
    }

    var line1 = ""
    var line2 = ""
    for (word in words) {
        if ((line1 + " " + word).trim().length <= maxCharsPerLine) {
            line1 = (line1 + " " + word).trim()
        } else {
            line2 = (line2 + " " + word).trim()
        }
    }

    return Pair(line1, if (line2.isNotEmpty()) line2.take(maxCharsPerLine) else null)
}