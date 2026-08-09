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

    // 3. Tightly Grouped Text & Battery Bar Metrics
    val statusTextSize = minOf(rect.width() * 0.12f, 22f * density)
    val nameTextSize = minOf(rect.width() * 0.07f, 12f * density)
    val barH = minOf(rect.height() * 0.038f, 5f * density)
    val barW = rect.width() * 0.62f

    // Fixed spacing between grouped text & line elements
    val gapNameToBar = 12f * density
    val gapStatusToName = 6f * density

    val bottomPadding = (rect.height() * 0.08f).coerceAtLeast(14f * density)
    val barTop = rect.bottom - bottomPadding - barH
    val barLeft = cx - (barW / 2f)
    val barRect = RectF(barLeft, barTop, barLeft + barW, barTop + barH)

    val nameY = barTop - gapNameToBar
    val statusY = nameY - nameTextSize - gapStatusToName

    // 4. Position Earbuds in Remaining Space Above Text Group
    val availableTopHeight = (statusY - statusTextSize - rect.top).coerceAtLeast(10f)
    val earbudCy = rect.top + (availableTopHeight * 0.52f)
    val baseScale = (minOf(rect.width(), availableTopHeight * 2f) / (115f * density)).coerceAtLeast(0.45f)
    val earbudSpacing = rect.width() * 0.15f

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

    // Lighter Dark Greyish tone for clear contrast against dark cards
    val bodyColor = if (isLightMode) Color(0xFF2C2C30).toArgb() else Color(0xFF282828).toArgb()
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