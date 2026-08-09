package com.altusix.slate.widgets.bluetooth

import android.content.Context
import android.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.altusix.slate.data.local.SlateWidgetConfig

// =========================================================================
// HELPER: Geometry & Dimension Utilities
// =========================================================================

private fun getStandardCornerRadius(density: Float): Float = 22f * density

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
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val cardCornerRadius = getStandardCornerRadius(density)

    // 1. Calculate Bounds (Responsive vs Fixed Aspect Ratio)
    val rect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val cardSize = minOf(w, h).toFloat()
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    // 2. Draw Card Background
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardCornerRadius, cardCornerRadius, bgPaint)

    val cx = rect.centerX()

    // 3. Proportional Text & Battery Bar Metrics
    val statusTextSize = minOf(rect.width() * 0.12f, rect.height() * 0.11f)
    val nameTextSize = minOf(rect.width() * 0.07f, rect.height() * 0.065f)
    val barH = rect.height() * 0.038f
    val barW = rect.width() * 0.62f

    val gapNameToBar = rect.height() * 0.04f
    val gapStatusToName = rect.height() * 0.02f

    val bottomPadding = rect.height() * 0.08f
    val barTop = rect.bottom - bottomPadding - barH
    val barLeft = cx - (barW / 2f)
    val barRect = RectF(barLeft, barTop, barLeft + barW, barTop + barH)

    val nameY = barTop - gapNameToBar
    val statusY = nameY - nameTextSize - gapStatusToName

    // 4. Position Earbuds in Remaining Space Above Text Group
    val availableTopHeight = (statusY - statusTextSize - rect.top).coerceAtLeast(10f)
    val earbudCy = rect.top + (availableTopHeight * 0.52f)
    val baseScale = (minOf(rect.width(), availableTopHeight * 2f) / (115f * density)).coerceAtLeast(0.45f)
    val earbudSpacing = 16f * density * baseScale

    drawScaledEarbudGraphic(
        canvas = canvas,
        cx = cx - earbudSpacing,
        cy = earbudCy,
        angleDeg = -22f,
        scale = baseScale,
        accentColor = accentColor,
        isLightMode = slateConfig.themeMode == "LIGHT",
        density = density
    )
    drawScaledEarbudGraphic(
        canvas = canvas,
        cx = cx + earbudSpacing,
        cy = earbudCy,
        angleDeg = 22f,
        scale = baseScale,
        accentColor = accentColor,
        isLightMode = slateConfig.themeMode == "LIGHT",
        density = density
    )

    // 5. Render Status Title Text
    val statusText = when {
        deviceData.needsPermission -> "GRANT PERM"
        deviceData.isConnected -> "Connected"
        else -> "CONNECT"
    }
    val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (deviceData.isConnected) accentColor else if (slateConfig.themeMode == "LIGHT") Color(0xFF161618).toArgb() else Color.White.toArgb()
        textSize = statusTextSize
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(statusText, cx, statusY, statusPaint)

    // 6. Render Subtitle / Device Name
    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (slateConfig.themeMode == "LIGHT") Color(0xFF636366).toArgb() else Color.Gray.toArgb()
        textSize = nameTextSize
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.CENTER
    }
    val displayName = if (deviceData.needsPermission) "tap to allow access" else deviceData.deviceName.lowercase()
    canvas.drawText(displayName, cx, nameY, namePaint)

    // 7. Render Battery Level Bar
    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (slateConfig.themeMode == "LIGHT") Color(0xFFE5E5EA).toArgb() else Color(0x22FFFFFF).toArgb()
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

    val earbudCy = cy - (cardSize * 0.15f)
    val earbudSpacing = cardSize * 0.11f
    val earbudScale = (cardSize / (165f * density)).coerceAtLeast(0.42f)

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

    val primaryTextColor = if (isLight) Color(0xFF161618).toArgb() else Color(0xFFFFFFFF).toArgb()
    val secondaryTextColor = if (isLight) Color(0x99000000).toArgb() else Color(0x99FFFFFF).toArgb()

    val pctText = when {
        deviceData.needsPermission -> "GRANT"
        deviceData.isConnected -> "${deviceData.batteryLevel}%"
        else -> "--%"
    }

    val pctTextSize = cardSize * 0.11f
    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = pctTextSize
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val pctY = cy + (cardSize * 0.11f)
    canvas.drawText(pctText, cx, pctY, pctPaint)

    val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = cardSize * 0.042f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.05f
    }

    val (line1, line2) = when {
        deviceData.needsPermission -> Pair("PERMISSION", "REQUIRED")
        deviceData.isConnected -> splitToTwoLines(deviceData.deviceName.uppercase(), maxCharsPerLine = 15)
        else -> Pair("NO DEVICE", "CONNECTED")
    }

    val line1Y = pctY + (cardSize * 0.075f)
    canvas.drawText(line1, cx, line1Y, statusPaint)

    if (!line2.isNullOrEmpty()) {
        val line2Y = line1Y + (cardSize * 0.052f)
        canvas.drawText(line2, cx, line2Y, statusPaint)
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

    val innerRadius = ringRadius - (ringStrokeWidth / 2f)
    val innerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, innerRadius, innerBgPaint)

    val earbudCy = cy - (innerRadius * 0.30f)
    val earbudSpacing = innerRadius * 0.22f
    val earbudScale = (innerRadius / (75f * density)).coerceAtLeast(0.38f)

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

    val primaryTextColor = if (isLight) Color(0xFF161618).toArgb() else Color(0xFFFFFFFF).toArgb()
    val secondaryTextColor = if (isLight) Color(0x99000000).toArgb() else Color(0x99FFFFFF).toArgb()

    val statusText = when {
        deviceData.needsPermission -> "GRANT"
        deviceData.isConnected -> "${deviceData.batteryLevel}%"
        else -> "OFF"
    }

    val statusTextSize = innerRadius * 0.26f
    val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = statusTextSize
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val statusY = cy + (innerRadius * 0.18f)
    canvas.drawText(statusText, cx, statusY, statusPaint)

    val nameTextSize = innerRadius * 0.11f
    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = nameTextSize
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.CENTER
    }

    val (line1, line2) = when {
        deviceData.needsPermission -> Pair("tap to allow access", null)
        deviceData.isConnected -> splitToTwoLines(deviceData.deviceName.lowercase(), maxCharsPerLine = 16)
        else -> Pair("no device connected", null)
    }

    val line1Y = statusY + (innerRadius * 0.18f)
    canvas.drawText(line1, cx, line1Y, namePaint)

    if (!line2.isNullOrEmpty()) {
        val line2Y = line1Y + (innerRadius * 0.14f)
        canvas.drawText(line2, cx, line2Y, namePaint)
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
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = slateConfig.themeMode == "LIGHT"
    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val cardCornerRadius = getStandardCornerRadius(density)

    // 1. Calculate Bounds (Responsive vs Fixed Aspect Ratio)
    val rect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val cardSize = minOf(w, h).toFloat()
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    // 2. Draw Main Background Card
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardCornerRadius, cardCornerRadius, bgPaint)

    // Proportional Padding relative to rect
    val paddingHorizontal = rect.width() * 0.065f
    val paddingTopBottom = rect.height() * 0.065f

    val contentRect = RectF(
        rect.left + paddingHorizontal,
        rect.top + paddingTopBottom,
        rect.right - paddingHorizontal,
        rect.bottom - paddingTopBottom
    )

    // 3. Increased Volume Capsule Width (Capped at 56dp max width for a wider slider)
    val maxVolumeWidth = 56f * density
    val rightW = (contentRect.width() * 0.26f).coerceAtMost(maxVolumeWidth)
    val gap = (contentRect.width() * 0.05f).coerceIn(8f * density, 16f * density)

    val leftW = contentRect.width() - rightW - gap
    val leftRect = RectF(contentRect.left, contentRect.top, contentRect.left + leftW, contentRect.bottom)
    val rightRect = RectF(contentRect.right - rightW, contentRect.top, contentRect.right, contentRect.bottom)

    // =========================================================================
    // LEFT SECTION: Grouped Bottom Typography
    // =========================================================================
    val leftCx = leftRect.centerX()

    val statusTextSize = minOf(leftRect.width() * 0.18f, leftRect.height() * 0.15f)
    val nameTextSize = minOf(leftRect.width() * 0.10f, leftRect.height() * 0.085f)
    val barH = leftRect.height() * 0.038f
    val barW = leftRect.width() * 0.80f

    val gapNameToBar = leftRect.height() * 0.045f
    val gapStatusToName = leftRect.height() * 0.025f

    val bottomPadding = leftRect.height() * 0.03f
    val barTop = leftRect.bottom - bottomPadding - barH
    val barLeft = leftCx - (barW / 2f)
    val barRect = RectF(barLeft, barTop, barLeft + barW, barTop + barH)

    val nameY = barTop - gapNameToBar
    val statusY = nameY - nameTextSize - gapStatusToName

    // 4. Position & Scale TWS Earbuds
    val availableTopHeight = (statusY - statusTextSize - leftRect.top).coerceAtLeast(10f)
    val earbudCy = leftRect.top + (availableTopHeight * 0.48f)

    val maxScaleForHeight = availableTopHeight / (45f * density)
    val maxScaleForWidth = leftRect.width() / (75f * density)
    val baseScale = minOf(maxScaleForHeight, maxScaleForWidth).coerceAtLeast(0.35f)

    val earbudSpacing = 16f * density * baseScale

    drawScaledEarbudGraphic(
        canvas = canvas,
        cx = leftCx - earbudSpacing,
        cy = earbudCy,
        angleDeg = -20f,
        scale = baseScale,
        accentColor = accentColor,
        isLightMode = isLight,
        density = density
    )
    drawScaledEarbudGraphic(
        canvas = canvas,
        cx = leftCx + earbudSpacing,
        cy = earbudCy,
        angleDeg = 20f,
        scale = baseScale,
        accentColor = accentColor,
        isLightMode = isLight,
        density = density
    )

    // 5. Render Status Title Text
    val statusText = when {
        deviceData.needsPermission -> "GRANT PERM"
        deviceData.isConnected -> "Connected"
        else -> "CONNECT"
    }
    val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (deviceData.isConnected) accentColor else if (isLight) Color(0xFF161618).toArgb() else Color.White.toArgb()
        textSize = statusTextSize
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(statusText, leftCx, statusY, statusPaint)

    // 6. Render Subtitle / Device Name
    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF636366).toArgb() else Color.Gray.toArgb()
        textSize = nameTextSize
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.CENTER
    }
    val displayName = if (deviceData.needsPermission) "tap to allow access" else deviceData.deviceName.lowercase()
    canvas.drawText(displayName.take(18), leftCx, nameY, namePaint)

    // 7. Render Battery Level Bar
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

    // =========================================================================
    // RIGHT SECTION: Volume Capsule & Controls (+ / -) with Dynamic Contrast
    // =========================================================================
    val volCapsuleRadius = rightRect.width() / 2f
    val volBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFFE5E5EA).toArgb() else Color(0xFF222226).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rightRect, volCapsuleRadius, volCapsuleRadius, volBgPaint)

    // Filled Volume Track
    val volPct = (deviceData.volumeLevel.coerceIn(0, 100) / 100f)
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

    // Plus (+) and Minus (-) Control Coordinates
    val plusCx = rightRect.centerX()
    val plusCy = rightRect.top + (rightRect.height() * 0.18f)

    val minusCx = rightRect.centerX()
    val minusCy = rightRect.bottom - (rightRect.height() * 0.18f)

    val iconLen = rightRect.width() * 0.18f
    val strokeW = (rightRect.width() * 0.08f).coerceAtLeast(2.2f)

    // Dynamic contrast color helper based on whether volume fill covers the icon
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

    // Draw Plus (+) Icon at Top
    val plusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = getIconColor(plusCy)
        strokeWidth = strokeW
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(plusCx - iconLen, plusCy, plusCx + iconLen, plusCy, plusPaint)
    canvas.drawLine(plusCx, plusCy - iconLen, plusCx, plusCy + iconLen, plusPaint)

    // Draw Minus (-) Icon at Bottom
    val minusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = getIconColor(minusCy)
        strokeWidth = strokeW
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(minusCx - iconLen, minusCy, minusCx + iconLen, minusCy, minusPaint)

    return bitmap
}

// =========================================================================
// PRIVATE GRAPHIC DRAWING HELPERS
// =========================================================================

private fun drawScaledEarbudGraphic(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    angleDeg: Float,
    scale: Float,
    accentColor: Int,
    isLightMode: Boolean,
    density: Float
) {
    canvas.save()
    canvas.rotate(angleDeg, cx, cy)

    val sideDirection = if (angleDeg <= 0f) -1f else 1f

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

/**
 * Splits device name string into two balanced lines for circular widget display.
 */
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