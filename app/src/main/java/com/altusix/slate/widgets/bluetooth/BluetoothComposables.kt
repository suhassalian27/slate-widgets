package com.altusix.slate.widgets.bluetooth

import android.content.Context
import android.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius
import com.altusix.slate.utils.createSupersampledCanvas

// =========================================================================
// CANVAS BITMAP GENERATORS FOR NATIVE REMOTE VIEWS
// =========================================================================

/**
 * 1. Bluetooth Earbuds Card (2x2 Square / Responsive)
 * Displays stylized earbud graphics, connection title, device label, and a battery level bar.
 */
fun generateEarbudsSquareBitmap(
    context: Context,
    deviceData: BluetoothDeviceData,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    // 1. Utilize shared utility for supersampled canvas and scale factor
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)

    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()

    // 2. Outer container corner radius
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)

    // Layout bounds
    val rect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val cardSize = minOf(w, h)
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardCornerRadius, cardCornerRadius, bgPaint)

    val cx = rect.centerX()
    val cardSize = minOf(rect.width(), rect.height())

    // Earbud Size Scale
    val baseScale = (cardSize / (96f * scaleFactor)).coerceAtLeast(0.45f)
    val earbudSpacing = 16f * scaleFactor * baseScale

    val statusTextSize = cardSize * 0.125f
    val nameTextSize = cardSize * 0.065f
    val barH = cardSize * 0.038f
    val barW = minOf(rect.width() * 0.65f, cardSize * 0.85f)

    val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (deviceData.isConnected) accentColor else if (slateConfig.themeMode == "LIGHT") Color(0xFF161618).toArgb() else Color.White.toArgb()
        textSize = statusTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (slateConfig.themeMode == "LIGHT") Color(0xFF636366).toArgb() else Color.Gray.toArgb()
        textSize = nameTextSize
        typeface = getSlateFont(context, weight = 400)
        textAlign = Paint.Align.CENTER
    }

    val statusMetrics = statusPaint.fontMetrics
    val nameMetrics = namePaint.fontMetrics

    val statusTextHeight = statusMetrics.descent - statusMetrics.ascent
    val nameTextHeight = nameMetrics.descent - nameMetrics.ascent

    val gapBudsToStatus = cardSize * 0.004f
    val gapStatusToName = cardSize * 0f
    val gapNameToBar = cardSize * 0.04f

    val earbudTopExtent = 18f * scaleFactor * baseScale
    val earbudBottomExtent = 23f * scaleFactor * baseScale

    val totalBlockH = earbudTopExtent + earbudBottomExtent + gapBudsToStatus +
            statusTextHeight + gapStatusToName + nameTextHeight + gapNameToBar + barH

    val blockTop = rect.top + (rect.height() - totalBlockH) / 2f
    val earbudCy = blockTop + earbudTopExtent

    val statusTopY = earbudCy + earbudBottomExtent + gapBudsToStatus
    val statusY = statusTopY - statusMetrics.ascent

    val nameTopY = statusTopY + statusTextHeight + gapStatusToName
    val nameY = nameTopY - nameMetrics.ascent

    val barTop = nameTopY + nameTextHeight + gapNameToBar
    val barLeft = cx - (barW / 2f)
    val barRect = RectF(barLeft, barTop, barLeft + barW, barTop + barH)

    // Draw Earbuds
    drawScaledEarbudGraphic(
        canvas = canvas,
        cx = cx - earbudSpacing,
        cy = earbudCy,
        angleDeg = -22f,
        scale = baseScale,
        accentColor = accentColor,
        isLightMode = slateConfig.themeMode == "LIGHT",
        density = scaleFactor
    )
    drawScaledEarbudGraphic(
        canvas = canvas,
        cx = cx + earbudSpacing,
        cy = earbudCy,
        angleDeg = 22f,
        scale = baseScale,
        accentColor = accentColor,
        isLightMode = slateConfig.themeMode == "LIGHT",
        density = scaleFactor
    )

    // Draw Texts
    val statusText = when {
        deviceData.needsPermission -> "GRANT PERM"
        deviceData.isConnected -> "Connected"
        else -> "CONNECT"
    }
    canvas.drawText(statusText, cx, statusY, statusPaint)

    val displayName = if (deviceData.needsPermission) "tap to allow access" else deviceData.deviceName.lowercase()
    canvas.drawText(displayName, cx, nameY, namePaint)

    // Draw Tracking Bar
    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (slateConfig.themeMode == "LIGHT") Color(0xFFE5E5EA).toArgb() else Color(0x22FFFFFF).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(barRect, barH / 2f, barH / 2f, trackPaint)

    // Draw Active Bar
    if (deviceData.isConnected) {
        val pct = (deviceData.batteryLevel.coerceIn(0, 100) / 100f)
        val activeBarRect = RectF(barLeft, barTop, barLeft + (barW * pct), barTop + barH)
        val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(activeBarRect, barH / 2f, barH / 2f, activePaint)
    }

    return bitmap
}

/**
 * 2. Bluetooth Circular Dial Widget (2x2 Square / Responsive)
 * Features a unique audio-segmented 36-dash radial gauge, inner precision halo,
 * centered TWS earbud graphics, percentage readout, and multi-line status text.
 */
fun generateBluetoothCircularDialBitmap(
    context: Context,
    deviceData: BluetoothDeviceData,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = slateConfig.themeMode == "LIGHT"
    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val dimColor = if (isLight) Color(0xFFE5E5EA).toArgb() else Color(0x1CFFFFFF).toArgb()

    val rect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val cardSize = minOf(w, h).toFloat()
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardSize = minOf(rect.width(), rect.height())
    val cx = rect.centerX()
    val cy = rect.centerY()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, cardSize / 2f, bgPaint)

    // Gauge and Halo Logic
    val dynamicScale = (cardSize / 300f).coerceAtLeast(0.5f)
    val margin = cardSize * 0.055f
    val outerRadius = (cardSize / 2f) - margin

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
        color = if (isLight) Color(0x18000000).toArgb() else Color(0x22FFFFFF).toArgb()
        strokeWidth = 1.4f * dynamicScale
        style = Paint.Style.STROKE
    }
    canvas.drawCircle(cx, cy, haloRadius, haloPaint)

    val notchDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (deviceData.isConnected) accentColor else (if (isLight) Color(0x30000000).toArgb() else Color(0x40FFFFFF).toArgb())
        style = Paint.Style.FILL
    }
    val dotRadius = 2.2f * dynamicScale
    for (angle in listOf(-90f, 0f, 90f, 180f)) {
        val rad = Math.toRadians(angle.toDouble())
        val notchX = cx + (haloRadius * Math.cos(rad)).toFloat()
        val notchY = cy + (haloRadius * Math.sin(rad)).toFloat()
        canvas.drawCircle(notchX, notchY, dotRadius, notchDotPaint)
    }

    // =========================================================================
    // 🎛️ SPACING AND SIZE CONTROLS
    // =========================================================================

    // 1. Earbud Size (Decrease divisor to make buds larger, default is 145f)
    val earbudScaleDivisor = 140f
    val earbudScale = (cardSize / (earbudScaleDivisor * density)).coerceAtLeast(0.40f)
    val earbudSpacing = cardSize * 0.12f

    // 2. Text Sizes
    val pctTextSize = cardSize * 0.12f
    val statusTextSize = cardSize * 0.042f

    // 3. Gaps (Drastically reduced to pull elements tight together)
    val gapBudsToPct = cardSize * 0.04f
    val gapPctToStatus = cardSize * 0.04f
    val lineSpacing = cardSize * 0.050f

    // =========================================================================

    val primaryTextColor = if (isLight) Color(0xFF161618).toArgb() else Color(0xFFFFFFFF).toArgb()
    val secondaryTextColor = if (isLight) Color(0x99000000).toArgb() else Color(0x99FFFFFF).toArgb()

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

    // Optical text height calculation (removes invisible Android font padding for exact centering)
    val pctOpticalH = pctTextSize * 0.75f
    val statusOpticalH = statusTextSize * 0.75f

    // Earbud bounds height calculation
    val earbudTopExtent = 18f * density * earbudScale
    val earbudBottomExtent = 23f * density * earbudScale

    // Unified Block Height Calculation
    val totalBlockH = earbudTopExtent + earbudBottomExtent +
            gapBudsToPct + pctOpticalH +
            gapPctToStatus + statusOpticalH +
            (if (hasLine2) lineSpacing else 0f)

    val blockOffsetY = cardSize * 0.025f

    // Center the entire block perfectly to the canvas center (cy)
    val blockTop = cy - (totalBlockH / 2f) + blockOffsetY

    // Component Y placements (Calculated sequentially from top to bottom)
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
        density = density
    )
    drawScaledEarbudGraphic(
        canvas = canvas,
        cx = cx + earbudSpacing,
        cy = earbudCy,
        angleDeg = 20f,
        scale = earbudScale,
        accentColor = accentColor,
        isLightMode = isLight,
        density = density
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
 * 3. Bluetooth Ring Widget (2x2 Square / Responsive)
 * Features a thick outer battery progress ring, inner background circle,
 * TWS earbud graphics, status/percentage readout, and multi-line device label.
 */
fun generateBluetoothRingBitmap(
    context: Context,
    deviceData: BluetoothDeviceData,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = slateConfig.themeMode == "LIGHT"
    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val dimColor = if (isLight) Color(0xFFE5E5EA).toArgb() else Color(0x22FFFFFF).toArgb()

    val rect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val cardSize = minOf(w, h).toFloat()
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardSize = minOf(rect.width(), rect.height())
    val cx = rect.centerX()
    val cy = rect.centerY()

    // 1. Draw Outer Ring
    val ringStrokeWidth = cardSize * 0.13f
    val ringRadius = (cardSize / 2f) - (ringStrokeWidth / 2f) - (2f * density)

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
    val innerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, innerRadius, innerBgPaint)

    // =========================================================================
    // 🎛️ SPACING AND SIZE CONTROLS (Matched to Widget 2)
    // =========================================================================

    // 1. Earbud Size (Decrease divisor to make buds larger, default is 145f)
    val earbudScaleDivisor = 145f
    val earbudScale = (cardSize / (earbudScaleDivisor * density)).coerceAtLeast(0.40f)
    val earbudSpacing = cardSize * 0.12f

    // 2. Text Sizes
    val pctTextSize = cardSize * 0.12f
    val statusTextSize = cardSize * 0.042f

    // 3. Unified Gap (Equal space between Earbuds <-> Percentage <-> Status)
    val elementGap = cardSize * 0.03f
    val lineSpacing = cardSize * 0.050f

    // =========================================================================

    val primaryTextColor = if (isLight) Color(0xFF161618).toArgb() else Color(0xFFFFFFFF).toArgb()
    val secondaryTextColor = if (isLight) Color(0x99000000).toArgb() else Color(0x99FFFFFF).toArgb()

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

    // Optical text height calculation (removes invisible Android font padding for exact centering)
    val pctMetrics = pctPaint.fontMetrics
    val statusMetrics = statusPaint.fontMetrics

    val pctOpticalH = pctTextSize * 0.75f
    val statusOpticalH = statusTextSize * 0.75f

    // Earbud bounds height calculation
    val earbudTopExtent = 18f * density * earbudScale
    val earbudBottomExtent = 23f * density * earbudScale

    // Unified Block Height Calculation
    val totalBlockH = earbudTopExtent + earbudBottomExtent +
            elementGap + pctOpticalH +
            elementGap + statusOpticalH +
            (if (hasLine2) lineSpacing else 0f)

    val blockOffsetY = cardSize * 0.025f

    // Center the entire block perfectly to the canvas center (cy)
    val blockTop = cy - (totalBlockH / 2f) + blockOffsetY

    // Component Y placements (Calculated sequentially from top to bottom)
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
        density = density
    )
    drawScaledEarbudGraphic(
        canvas = canvas,
        cx = cx + earbudSpacing,
        cy = earbudCy,
        angleDeg = 20f,
        scale = earbudScale,
        accentColor = accentColor,
        isLightMode = isLight,
        density = density
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
 * 4. Bluetooth Earbuds Volume Control Widget (2x2 / Responsive)
 */
fun generateEarbudsVolumeControlBitmap(
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
    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)

    val rect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val cardSize = minOf(w, h)
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardCornerRadius, cardCornerRadius, bgPaint)

    val paddingHorizontal = rect.width() * 0.065f
    val paddingTopBottom = rect.height() * 0.05f

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
        color = if (deviceData.isConnected) accentColor else if (isLight) Color(0xFF161618).toArgb() else Color.White.toArgb()
        textSize = statusTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF636366).toArgb() else Color.Gray.toArgb()
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

    // Draw Left Panel
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
        color = if (isLight) Color(0xFFE5E5EA).toArgb() else Color(0x22FFFFFF).toArgb()
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
        color = if (isLight) Color(0xFFE5E5EA).toArgb() else Color(0xFF222226).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rightRect, volCapsuleRadius, volCapsuleRadius, volBgPaint)

    // Fallback volume level for clear rendering
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
        val clipPath = Path().apply {
            addRoundRect(rightRect, volCapsuleRadius, volCapsuleRadius, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)
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
            if (luminance > 0.5f) Color(0xFF121214).toArgb() else Color.White.toArgb()
        } else {
            if (isLight) Color(0xFF2C2C30).toArgb() else Color.White.toArgb()
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
 * 5. Bluetooth Tri-Battery Studio Dock Widget (Fixed Proportional Aspect Ratio)
 */
fun generateBluetoothTriBatteryDockBitmap(
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
    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)

    val cardSize = minOf(w, h)
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f
    val rect = RectF(leftX, topY, leftX + cardSize, topY + cardSize)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardCornerRadius, cardCornerRadius, bgPaint)

    val paddingH = cardSize * 0.065f
    val paddingV = cardSize * 0.065f
    val contentRect = RectF(rect.left + paddingH, rect.top + paddingV, rect.right - paddingH, rect.bottom - paddingV)

    val primaryTextColor = if (isLight) Color(0xFF161618).toArgb() else Color(0xFFFFFFFF).toArgb()
    val secondaryTextColor = if (isLight) Color(0x99000000).toArgb() else Color(0x99FFFFFF).toArgb()
    val podBgColor = if (isLight) Color(0xFFF2F2F7).toArgb() else Color(0xFF1E1E22).toArgb()

    val headerTextSize = contentRect.width() * 0.058f
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = headerTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.LEFT
        letterSpacing = 0.05f
    }

    val headerY = contentRect.top + headerTextSize
    val deviceTitle = if (deviceData.needsPermission) "GRANT PERMISSION" else if (deviceData.isConnected) deviceData.deviceName.uppercase() else "DISCONNECTED"
    canvas.drawText(deviceTitle.take(18), contentRect.left, headerY, headerPaint)

    val dotRadius = headerTextSize * 0.35f
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (deviceData.isConnected) accentColor else Color(0xFFFF3B30).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(contentRect.right - dotRadius, headerY - (headerTextSize * 0.35f), dotRadius, dotPaint)

    val headerHeight = headerTextSize + (cardSize * 0.045f)
    val podsArea = RectF(contentRect.left, contentRect.top + headerHeight, contentRect.right, contentRect.bottom)

    val podGap = podsArea.width() * 0.04f
    val topRowH = (podsArea.height() - podGap) * 0.57f
    val bottomRowH = (podsArea.height() - podGap) * 0.43f

    val podW = (podsArea.width() - podGap) / 2f
    val podRadius = (cardCornerRadius - paddingH).coerceAtLeast(scaleFactor * 6f)

    val podBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = podBgColor
        style = Paint.Style.FILL
    }

    val trackBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFFE5E5EA).toArgb() else Color(0x22FFFFFF).toArgb()
        style = Paint.Style.FILL
    }

    val activeBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }

    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = topRowH * 0.18f
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.RIGHT
    }

    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = topRowH * 0.22f
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.RIGHT
    }

    val earbudScale = (topRowH * 0.45f) / (38f * scaleFactor)

    // POD 1: LEFT EARBUD
    val leftPod = RectF(podsArea.left, podsArea.top, podsArea.left + podW, podsArea.top + topRowH)
    canvas.drawRoundRect(leftPod, podRadius, podRadius, podBgPaint)

    val leftBudCx = leftPod.left + (leftPod.width() * 0.26f)
    val leftBudCy = leftPod.top + (leftPod.height() * 0.38f)

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

    val textRightX = leftPod.right - (leftPod.width() * 0.10f)
    canvas.drawText("L", textRightX, leftPod.top + (leftPod.height() * 0.28f), badgePaint)

    val leftPctText = if (deviceData.isConnected) "${deviceData.leftBattery}%" else "--"
    canvas.drawText(leftPctText, textRightX, leftPod.top + (leftPod.height() * 0.58f), pctPaint)

    val miniBarH = leftPod.height() * 0.065f
    val miniBarRect = RectF(
        leftPod.left + (leftPod.width() * 0.10f),
        leftPod.bottom - (leftPod.height() * 0.16f),
        leftPod.right - (leftPod.width() * 0.10f),
        leftPod.bottom - (leftPod.height() * 0.16f) + miniBarH
    )
    canvas.drawRoundRect(miniBarRect, miniBarH / 2f, miniBarH / 2f, trackBarPaint)
    if (deviceData.isConnected) {
        val pct = deviceData.leftBattery.coerceIn(0, 100) / 100f
        val activeRect = RectF(miniBarRect.left, miniBarRect.top, miniBarRect.left + (miniBarRect.width() * pct), miniBarRect.bottom)
        canvas.drawRoundRect(activeRect, miniBarH / 2f, miniBarH / 2f, activeBarPaint)
    }

    // POD 2: RIGHT EARBUD
    val rightPod = RectF(podsArea.right - podW, podsArea.top, podsArea.right, podsArea.top + topRowH)
    canvas.drawRoundRect(rightPod, podRadius, podRadius, podBgPaint)

    val rightBudCx = rightPod.left + (rightPod.width() * 0.26f)
    val rightBudCy = rightPod.top + (rightPod.height() * 0.38f)

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

    val rightTextRightX = rightPod.right - (rightPod.width() * 0.10f)
    canvas.drawText("R", rightTextRightX, rightPod.top + (rightPod.height() * 0.28f), badgePaint)

    val rightPctText = if (deviceData.isConnected) "${deviceData.rightBattery}%" else "--"
    canvas.drawText(rightPctText, rightTextRightX, rightPod.top + (rightPod.height() * 0.58f), pctPaint)

    val rightMiniBarRect = RectF(
        rightPod.left + (rightPod.width() * 0.10f),
        rightPod.bottom - (rightPod.height() * 0.16f),
        rightPod.right - (rightPod.width() * 0.10f),
        rightPod.bottom - (rightPod.height() * 0.16f) + miniBarH
    )
    canvas.drawRoundRect(rightMiniBarRect, miniBarH / 2f, miniBarH / 2f, trackBarPaint)
    if (deviceData.isConnected) {
        val pct = deviceData.rightBattery.coerceIn(0, 100) / 100f
        val activeRect = RectF(rightMiniBarRect.left, rightMiniBarRect.top, rightMiniBarRect.left + (rightMiniBarRect.width() * pct), rightMiniBarRect.bottom)
        canvas.drawRoundRect(activeRect, miniBarH / 2f, miniBarH / 2f, activeBarPaint)
    }

    // POD 3: CHARGING CASE
    val casePod = RectF(podsArea.left, podsArea.bottom - bottomRowH, podsArea.right, podsArea.bottom)
    canvas.drawRoundRect(casePod, podRadius, podRadius, podBgPaint)

    val caseScale = (bottomRowH * 0.45f) / (17f * scaleFactor)
    val caseCx = casePod.left + (casePod.width() * 0.16f)
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

    val textStartX = casePod.left + (casePod.width() * 0.33f)

    val caseTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = casePod.height() * 0.20f
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.LEFT
        letterSpacing = 0.06f
    }
    canvas.drawText("CASE", textStartX, casePod.top + (casePod.height() * 0.38f), caseTitlePaint)

    val casePctText = if (deviceData.isConnected) "${deviceData.caseBattery}%" else "--"
    val casePctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = casePod.height() * 0.26f
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.LEFT
    }
    canvas.drawText(casePctText, textStartX, casePod.top + (casePod.height() * 0.72f), casePctPaint)

    val caseBarW = casePod.width() * 0.30f
    val caseBarH = casePod.height() * 0.12f
    val caseBarRect = RectF(
        casePod.right - (casePod.width() * 0.08f) - caseBarW,
        casePod.centerY() - (caseBarH / 2f),
        casePod.right - (casePod.width() * 0.08f),
        casePod.centerY() + (caseBarH / 2f)
    )
    canvas.drawRoundRect(caseBarRect, caseBarH / 2f, caseBarH / 2f, trackBarPaint)
    if (deviceData.isConnected) {
        val pct = deviceData.caseBattery.coerceIn(0, 100) / 100f
        val activeRect = RectF(caseBarRect.left, caseBarRect.top, caseBarRect.left + (caseBarRect.width() * pct), caseBarRect.bottom)
        canvas.drawRoundRect(activeRect, caseBarH / 2f, caseBarH / 2f, activeBarPaint)
    }

    return bitmap
}

/**
 * 6. Bluetooth Tri-Battery Circular Stage Widget (2x2 / Responsive)
 */
fun generateBluetoothTriBatteryCircleBitmap(
    context: Context,
    deviceData: BluetoothDeviceData,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = slateConfig.themeMode == "LIGHT"
    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()

    val cardSize = minOf(w, h).toFloat()
    val cx = w / 2f
    val cy = h / 2f
    val radius = (cardSize / 2f) - (2f * density)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, radius, bgPaint)

    val primaryTextColor = if (isLight) Color(0xFF161618).toArgb() else Color(0xFFFFFFFF).toArgb()

    val earbudScale = (radius / (50f * density)).coerceAtLeast(0.40f)

    val earbudSpread = radius * 0.2f

    drawScaledEarbudGraphic(
        canvas = canvas,
        cx = cx - (radius * 0.28f),
        cy = cy - (radius * 0.14f),
        angleDeg = -30f,
        scale = earbudScale,
        accentColor = accentColor,
        isLightMode = isLight,
        density = density,
        flipTip = false
    )

    drawScaledEarbudGraphic(
        canvas = canvas,
        cx = cx - (radius * -0.28f),
        cy = cy - (radius * -0.14f),
        angleDeg = 30f,
        scale = earbudScale,
        accentColor = accentColor,
        isLightMode = isLight,
        density = density,
        flipTip = true
    )

    val topMargin = radius * 0.05f
    val bottomMargin = radius * 0.0f

    val statusText = when {
        deviceData.needsPermission -> "PERMISSION"
        deviceData.isConnected -> "CONNECTED"
        else -> "DISCONNECTED"
    }

    val statusTextSize = (radius * 0.15f).coerceIn(8f * density, 16f * density)
    val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (deviceData.isConnected) accentColor else Color(0xFFFF3B30).toArgb()
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

    val batteryTextSize = (radius * 0.12f).coerceIn(8f * density, 15f * density)
    val batteryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (deviceData.isConnected) accentColor else Color(0xFFFF3B30).toArgb()
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