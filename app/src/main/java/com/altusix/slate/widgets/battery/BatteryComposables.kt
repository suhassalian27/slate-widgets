package com.altusix.slate.widgets.battery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.altusix.slate.core.theme.SlateColors
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.createSupersampledCanvas
import com.altusix.slate.utils.getSafeBgColor
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius
import kotlin.collections.minusAssign
import kotlin.compareTo
import kotlin.div
import kotlin.math.roundToInt
import kotlin.text.compareTo
import kotlin.text.toFloat
import kotlin.times

private fun drawBoltIcon(
    context: Context,
    canvas: Canvas,
    left: Float,
    top: Float,
    size: Float,
    colorInt: Int
) {
    val resId = context.resources.getIdentifier("ic_bolt", "drawable", context.packageName)
    if (resId != 0) {
        val drawable = ContextCompat.getDrawable(context, resId)
        if (drawable != null) {
            drawable.setBounds(left.toInt(), top.toInt(), (left + size).toInt(), (top + size).toInt())
            drawable.setTint(colorInt)
            drawable.draw(canvas)
        }
    }
}

// ============================================================================
// CANVAS BITMAP GENERATORS FOR NATIVE REMOTE VIEWS
// ============================================================================

// 1. Dot Level Header (1:1 Square)
fun generateDotLevelMeterWithHeaderBitmap(
    context: Context,
    data: DetailedBatteryData,
    config: SlateWidgetConfig,
    isResponsive: Boolean = false,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val margin = scaleFactor * 1.5f
    // Always locked to a strict 1:1 square
    val cardSize = minOf(w, h) - (margin * 2f)
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f
    val cardRect = RectF(leftX, topY, leftX + cardSize, topY + cardSize)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val activeColor = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val dimColor = if (isLight) 0x1F000000 else 0x1AFFFFFF

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = cardSize * 0.07f

    // Header Percentage Text
    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardSize * 0.18f
        typeface = getSlateFont(context, weight = 500)
    }
    val fontMetricsPct = pctPaint.fontMetrics
    val headerCenterY = topY + pad + (cardSize * 0.08f)
    val pctY = headerCenterY - (fontMetricsPct.ascent + fontMetricsPct.descent) / 2f
    canvas.drawText("${data.percentage}%", leftX + pad, pctY, pctPaint)

    // Header Charging Status (Bottom-aligned with the percentage baseline)
    if (data.isCharging) {
        val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = activeColor
            textSize = cardSize * 0.06f
            textAlign = Paint.Align.RIGHT
            typeface = getSlateFont(context, weight = 700)
        }
        val fontMetricsStatus = statusPaint.fontMetrics

        // Align baseline directly with the percentage baseline
        val statusY = pctY

        val rightX = leftX + cardSize - pad
        val textW = statusPaint.measureText("CHARGING")
        val iconSize = cardSize * 0.065f
        val gap = cardSize * 0.015f
        val iconLeft = rightX - textW - gap - iconSize

        // Center the bolt icon vertically relative to the CHARGING capital letters
        val textCapCenterY = pctY + (fontMetricsStatus.ascent / 2f)
        val iconTop = textCapCenterY - (iconSize / 2f)

        drawBoltIcon(context, canvas, iconLeft, iconTop, iconSize, activeColor)
        canvas.drawText("CHARGING", rightX, statusY, statusPaint)
    }

    // 10x10 Dot Grid
    val columns = 10
    val rows = 10
    val gridTopY = topY + pad + (cardSize * 0.18f)
    val gridW = cardSize - (pad * 2f)
    val gridH = cardSize - pad - (gridTopY - topY)

    val cellW = gridW / columns
    val cellH = gridH / rows
    val dotRadius = minOf(cellW, cellH) * 0.38f

    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = activeColor
        style = Paint.Style.FILL
    }
    val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dimColor
        style = Paint.Style.FILL
    }

    val totalDots = columns * rows
    val activeDotsCount = (data.percentage.coerceIn(0, 100) * totalDots) / 100

    for (r in 0 until rows) {
        val rowFromBottom = (rows - 1) - r
        for (c in 0 until columns) {
            val dotIndex = rowFromBottom * columns + c
            val cx = leftX + pad + c * cellW + cellW / 2f
            val cy = gridTopY + r * cellH + cellH / 2f

            val paint = if (dotIndex < activeDotsCount) activePaint else dimPaint
            canvas.drawCircle(cx, cy, dotRadius, paint)
        }
    }

    return bitmap
}

// 2. Dot Level Tile Pure / Textless (Strict 1:1 Square)
fun generateDotLevelMeterPureBitmap(
    context: Context,
    data: DetailedBatteryData,
    config: SlateWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val margin = scaleFactor * 1.5f
    val cardSize = minOf(w, h) - (margin * 2f)
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f
    val cardRect = RectF(leftX, topY, leftX + cardSize, topY + cardSize)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val activeColor = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val dimColor = if (isLight) 0x1F000000 else 0x1AFFFFFF

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    // Outer edge padding: reduced from 0.10f to 0.065f to push grid closer to the edges
    val pad = cardSize * 0.065f
    val columns = 10
    val rows = 10
    val gridW = cardSize - (pad * 2f)
    val gridH = cardSize - (pad * 2f)

    val cellW = gridW / columns
    val cellH = gridH / rows

    // Dot radius: reduced from 0.32f to 0.26f to increase the breathing room/gap between dots
    val dotRadius = minOf(cellW, cellH) * 0.26f

    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = activeColor
        style = Paint.Style.FILL
    }
    val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dimColor
        style = Paint.Style.FILL
    }

    val totalDots = columns * rows
    val activeDotsCount = (data.percentage.coerceIn(0, 100) * totalDots) / 100

    for (r in 0 until rows) {
        val rowFromBottom = (rows - 1) - r
        for (c in 0 until columns) {
            val dotIndex = rowFromBottom * columns + c
            val cx = leftX + pad + c * cellW + cellW / 2f
            val cy = topY + pad + r * cellH + cellH / 2f

            val paint = if (dotIndex < activeDotsCount) activePaint else dimPaint
            canvas.drawCircle(cx, cy, dotRadius, paint)
        }
    }

    return bitmap
}

// 3. Minimal Linear (Adaptive: Square, Wide 4x1, and Tall 1x2/2x4)
fun generateBatteryMinimalLinearBitmap(
    context: Context,
    data: DetailedBatteryData,
    config: SlateWidgetConfig,
    isResponsive: Boolean = false,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val margin = scaleFactor * 1.5f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val cardSize = minOf(w, h) - (margin * 2f)
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }
    val cardW = cardRect.width()
    val cardH = cardRect.height()
    val aspect = cardW / cardH.coerceAtLeast(1f)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val trackColor = if (isLight) 0x14000000 else 0x1CFFFFFF
    val accentColor = config.accentColorHex.toInt() or 0xFF000000.toInt()

    // 1. Background Surface with Subtle Edge Border
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x12000000 else 0x1AFFFFFF
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.8f
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, borderPaint)

    // Shared Status Setup
    val statusText = if (data.isCharging) "CHARGING" else "BATTERY"
    val statusColor = if (data.isCharging) accentColor else secondaryTextColor
    val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = statusColor
        typeface = getSlateFont(context, weight = 700)
    }

    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        typeface = getSlateFont(context, weight = 700)
    }

    val pctRatio = (data.percentage.coerceIn(0, 100) / 100f)

    // =========================================================================
    // BRANCH 1: WIDE BAR MODE (4x1, 3x1, 5x1)
    // =========================================================================
    if (aspect >= 1.55f) {
        val padX = (cardW * 0.065f).coerceIn(scaleFactor * 14f, scaleFactor * 26f)
        val padY = (cardH * 0.14f).coerceIn(scaleFactor * 8f, scaleFactor * 16f)
        val availW = cardW - (padX * 2f)

        // Bottom Full-Width Linear Bar
        val barH = (cardH * 0.14f).coerceIn(scaleFactor * 6f, scaleFactor * 11f)
        val barBottom = cardRect.bottom - padY
        val barTop = barBottom - barH
        val barLeft = cardRect.left + padX
        val barRight = cardRect.right - padX

        val trackRect = RectF(barLeft, barTop, barRight, barBottom)
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = trackColor }
        canvas.drawRoundRect(trackRect, barH / 2f, barH / 2f, trackPaint)

        val fillW = availW * pctRatio
        if (fillW > 0f) {
            val fillRect = RectF(barLeft, barTop, barLeft + fillW.coerceAtLeast(barH), barBottom)
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
            canvas.drawRoundRect(fillRect, barH / 2f, barH / 2f, fillPaint)
        }

        // Top Row: Left Status, Right Percentage
        val topAreaH = barTop - cardRect.top - padY
        val topAreaCenterY = cardRect.top + padY + (topAreaH / 2f)

        val pctSize = (topAreaH * 0.88f).coerceIn(scaleFactor * 20f, scaleFactor * 42f)
        pctPaint.textSize = pctSize
        pctPaint.textAlign = Paint.Align.RIGHT
        val fmPct = pctPaint.fontMetrics
        val pctY = topAreaCenterY - (fmPct.ascent + fmPct.descent) / 2f
        canvas.drawText("${data.percentage}%", barRight, pctY, pctPaint)

        val statusSize = (pctSize * 0.38f).coerceIn(scaleFactor * 10f, scaleFactor * 15f)
        statusPaint.textSize = statusSize
        statusPaint.textAlign = Paint.Align.LEFT
        val fmStatus = statusPaint.fontMetrics
        val statusY = topAreaCenterY - (fmStatus.ascent + fmStatus.descent) / 2f

        if (data.isCharging) {
            val iconSize = statusSize * 1.1f
            val gap = scaleFactor * 4f
            val iconLeft = barLeft
            val iconTop = topAreaCenterY - (iconSize / 2f)
            drawBoltIcon(context, canvas, iconLeft, iconTop, iconSize, accentColor)
            canvas.drawText(statusText, iconLeft + iconSize + gap, statusY, statusPaint)
        } else {
            canvas.drawText(statusText, barLeft, statusY, statusPaint)
        }

        // =========================================================================
        // BRANCH 2: TALL / VERTICAL MODE (1x2, 2x4)
        // =========================================================================
    } else if (aspect < 0.72f) {
        val padX = cardW * 0.10f
        val padY = cardH * 0.07f
        val availW = cardW - (padX * 2f)

        // Top Status
        val statusSize = (cardW * 0.09f).coerceIn(scaleFactor * 10f, scaleFactor * 14f)
        statusPaint.textSize = statusSize
        statusPaint.textAlign = Paint.Align.LEFT
        val statusY = cardRect.top + padY + statusSize

        if (data.isCharging) {
            val iconSize = statusSize * 1.05f
            val gap = scaleFactor * 4f
            val iconTop = statusY - statusSize
            drawBoltIcon(context, canvas, cardRect.left + padX, iconTop, iconSize, accentColor)
            canvas.drawText(statusText, cardRect.left + padX + iconSize + gap, statusY, statusPaint)
        } else {
            canvas.drawText(statusText, cardRect.left + padX, statusY, statusPaint)
        }

        // Percentage
        val pctSize = (cardW * 0.32f).coerceIn(scaleFactor * 26f, scaleFactor * 52f)
        pctPaint.textSize = pctSize
        pctPaint.textAlign = Paint.Align.LEFT
        val fmPct = pctPaint.fontMetrics
        val pctY = statusY + (cardH * 0.03f) - fmPct.ascent
        canvas.drawText("${data.percentage}%", cardRect.left + padX, pctY, pctPaint)

        // Vertical Power Capsule Tank
        val tankTop = pctY + fmPct.descent + (cardH * 0.035f)
        val tankBottom = cardRect.bottom - padY
        val tankH = (tankBottom - tankTop).coerceAtLeast(scaleFactor * 20f)
        val tankCornerRadius = (availW / 2f).coerceAtMost(scaleFactor * 20f)

        val tankRect = RectF(cardRect.left + padX, tankTop, cardRect.right - padX, tankBottom)
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = trackColor }
        canvas.drawRoundRect(tankRect, tankCornerRadius, tankCornerRadius, trackPaint)

        // Fill rising up from the bottom of the tank
        val fillH = tankH * pctRatio
        if (fillH > 0f) {
            val fillTop = (tankBottom - fillH).coerceAtLeast(tankTop)
            val fillRect = RectF(cardRect.left + padX, fillTop, cardRect.right - padX, tankBottom)
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }

            // Clip to tank pill shape so bottom/top corners stay perfectly rounded
            val clipPath = Path().apply {
                addRoundRect(tankRect, tankCornerRadius, tankCornerRadius, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawRoundRect(fillRect, tankCornerRadius, tankCornerRadius, fillPaint)
            canvas.restore()
        }

        // =========================================================================
        // BRANCH 3: STANDARD SQUARE / BALANCED MODE (2x2)
        // =========================================================================
    } else {
        val pad = minOf(cardW, cardH) * 0.085f
        val availW = cardW - (pad * 2f)

        // Top Status Header
        val statusSize = (cardH * 0.08f).coerceIn(scaleFactor * 10f, scaleFactor * 15f)
        statusPaint.textSize = statusSize
        statusPaint.textAlign = Paint.Align.LEFT
        val statusTopY = cardRect.top + pad + statusSize

        if (data.isCharging) {
            val iconSize = statusSize * 1.1f
            val gap = scaleFactor * 4f
            val iconTop = statusTopY - statusSize
            drawBoltIcon(context, canvas, cardRect.left + pad, iconTop, iconSize, accentColor)
            canvas.drawText(statusText, cardRect.left + pad + iconSize + gap, statusTopY, statusPaint)
        } else {
            canvas.drawText(statusText, cardRect.left + pad, statusTopY, statusPaint)
        }

        // Center Hero Percentage
        val pctSize = (cardH * 0.32f).coerceIn(scaleFactor * 32f, scaleFactor * 64f)
        pctPaint.textSize = pctSize
        pctPaint.textAlign = Paint.Align.LEFT
        val fmPct = pctPaint.fontMetrics
        val pctCenterY = cardRect.top + (cardH * 0.52f)
        val pctY = pctCenterY - (fmPct.ascent + fmPct.descent) / 2f
        canvas.drawText("${data.percentage}%", cardRect.left + pad, pctY, pctPaint)

        // Bottom Full-Width Linear Bar
        val barH = (cardH * 0.08f).coerceIn(scaleFactor * 7f, scaleFactor * 14f)
        val barBottom = cardRect.bottom - pad
        val barTop = barBottom - barH
        val barLeft = cardRect.left + pad
        val barRight = cardRect.right - pad

        val trackRect = RectF(barLeft, barTop, barRight, barBottom)
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = trackColor }
        canvas.drawRoundRect(trackRect, barH / 2f, barH / 2f, trackPaint)

        val fillW = availW * pctRatio
        if (fillW > 0f) {
            val fillRect = RectF(barLeft, barTop, barLeft + fillW.coerceAtLeast(barH), barBottom)
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
            canvas.drawRoundRect(fillRect, barH / 2f, barH / 2f, fillPaint)
        }
    }

    return bitmap
}


// 4. Minimal Ring (Strict 1:1 Square)
fun generateBatteryMinimalRingBitmap(
    context: Context,
    data: DetailedBatteryData,
    config: SlateWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val margin = scaleFactor * 1.5f
    // Locked strictly to a centered 1:1 square
    val cardSize = minOf(w, h) - (margin * 2f)
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f
    val cardRect = RectF(leftX, topY, leftX + cardSize, topY + cardSize)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val trackColor = if (isLight) 0x1F000000 else 0x1FAFAFAF
    val accentColor = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val strokeW = cardSize * 0.07f
    val arcRadius = cardSize * 0.36f
    val centerX = leftX + (cardSize / 2f)
    val arcCenterY = topY + (cardSize * 0.53f)

    val arcRect = RectF(
        centerX - arcRadius,
        arcCenterY - arcRadius,
        centerX + arcRadius,
        arcCenterY + arcRadius
    )

    val startAngle = 135f
    val maxSweepAngle = 270f
    val currentSweep = maxSweepAngle * (data.percentage.coerceIn(0, 100) / 100f)

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = trackColor
        style = Paint.Style.STROKE
        strokeWidth = strokeW
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawArc(arcRect, startAngle, maxSweepAngle, false, trackPaint)

    if (currentSweep > 0f) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(arcRect, startAngle, currentSweep, false, fillPaint)
    }

    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardSize * 0.24f
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val fontMetricsPct = pctPaint.fontMetrics
    val textOffset = if (data.isCharging) cardSize * 0.04f else 0f
    val textY = arcCenterY - textOffset - (fontMetricsPct.ascent + fontMetricsPct.descent) / 2f
    canvas.drawText("${data.percentage}%", centerX, textY, pctPaint)

    if (data.isCharging) {
        val iconSize = cardSize * 0.10f
        val iconLeft = centerX - (iconSize / 2f)
        val iconTop = textY + fontMetricsPct.descent + (cardSize * 0.02f)

        drawBoltIcon(context, canvas, iconLeft, iconTop, iconSize, accentColor)
    }

    return bitmap
}

// 5. Arc Battery (Strict 1:1 Square)
fun generateArcGaugeTileBitmap(
    context: Context,
    data: DetailedBatteryData,
    config: SlateWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val margin = scaleFactor * 1.5f
    // Locked strictly to a centered 1:1 square
    val cardSize = minOf(w, h) - (margin * 2f)
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f
    val cardRect = RectF(leftX, topY, leftX + cardSize, topY + cardSize)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val trackColorInt = if (isLight) 0x1F000000 else Color.argb(51, Color.red(accentColorInt), Color.green(accentColorInt), Color.blue(accentColorInt))

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = cardSize * 0.10f

    if (data.isCharging) {
        val iconSize = cardSize * 0.10f
        val iconLeft = leftX + cardSize - pad - iconSize
        val iconTop = topY + pad
        drawBoltIcon(context, canvas, iconLeft, iconTop, iconSize, accentColorInt)
    }

    val arcToTextGap = cardSize * -0.02f

    val gaugeW = (cardSize * 0.82f).toInt()
    val gaugeH = (cardSize * 0.41f).toInt()
    val arcBitmap = generateArcGaugeBitmap(data.percentage, accentColorInt, trackColorInt, gaugeW, gaugeH)

    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardSize * 0.28f
        textAlign = Paint.Align.CENTER
        typeface = getSlateFont(context, weight = 700)
    }
    val fontMetricsPct = pctPaint.fontMetrics
    val textHeight = fontMetricsPct.descent - fontMetricsPct.ascent

    val totalBlockH = gaugeH + arcToTextGap + textHeight
    val startY = topY + (cardSize - totalBlockH) / 2f

    val arcLeft = leftX + (cardSize - gaugeW) / 2f
    val arcTop = startY
    canvas.drawBitmap(arcBitmap, arcLeft, arcTop, null)

    val textY = arcTop + gaugeH + arcToTextGap - fontMetricsPct.ascent
    canvas.drawText("${data.percentage}%", leftX + (cardSize / 2f), textY, pctPaint)

    return bitmap
}

// 6. Editorial Stats (Adaptive: Square, Wide 4x1, and Tall 1x2/2x4)
fun generateEditorialStatsBitmap(
    context: Context,
    data: DetailedBatteryData,
    config: SlateWidgetConfig,
    isResponsive: Boolean = false,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val margin = scaleFactor * 1.5f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val cardSize = minOf(w, h) - (margin * 2f)
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }
    val cardW = cardRect.width()
    val cardH = cardRect.height()
    val aspect = cardW / cardH.coerceAtLeast(1f)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val trackColorInt = if (isLight) 0x1F000000 else 0x22FFFFFF

    // 1. Background Surface with Subtle Edge Border
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x12000000 else 0x1AFFFFFF
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.8f
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, borderPaint)

    // Shared Paint Setup
    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        typeface = getSlateFont(context, weight = 700)
    }

    val statPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        typeface = getSlateFont(context, weight = 600)
    }

    // =========================================================================
    // BRANCH 1: WIDE BAR MODE (4x1, 3x1, 5x1)
    // =========================================================================
    if (aspect >= 1.55f) {
        val padX = (cardW * 0.06f).coerceIn(scaleFactor * 14f, scaleFactor * 26f)
        val padY = (cardH * 0.13f).coerceIn(scaleFactor * 8f, scaleFactor * 16f)
        val availW = cardW - (padX * 2f)

        // Bottom Full-Width Segmented Bar
        val barH = (cardH * 0.16f).coerceIn(scaleFactor * 8f, scaleFactor * 14f).toInt()
        val barW = availW.toInt()
        val barTopY = cardRect.bottom - padY - barH

        val barBitmap = generateSegmentedBarBitmap(data.percentage, accentColorInt, trackColorInt, barW, barH)
        canvas.drawBitmap(barBitmap, cardRect.left + padX, barTopY, null)

        // Top Area Bounds
        val topAreaH = barTopY - (cardRect.top + padY)
        val topAreaCenterY = cardRect.top + padY + (topAreaH / 2f)

        // 1. Percentage on the Left
        val pctSize = (topAreaH * 0.88f).coerceIn(scaleFactor * 22f, scaleFactor * 42f)
        pctPaint.textSize = pctSize
        pctPaint.textAlign = Paint.Align.LEFT
        val fmPct = pctPaint.fontMetrics
        val pctY = topAreaCenterY - (fmPct.ascent + fmPct.descent) / 2f
        val pctText = "${data.percentage}%"
        canvas.drawText(pctText, cardRect.left + padX, pctY, pctPaint)
        val pctWidth = pctPaint.measureText(pctText)

        // 2. Charging Status Tag on the Far Right
        val rightEdgeX = cardRect.right - padX
        var statRightBound = rightEdgeX

        if (data.isCharging) {
            val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColorInt
                textSize = (pctSize * 0.38f).coerceIn(scaleFactor * 10f, scaleFactor * 14f)
                typeface = getSlateFont(context, weight = 700)
                textAlign = Paint.Align.RIGHT
            }
            val fmStatus = statusPaint.fontMetrics
            val statusY = topAreaCenterY - (fmStatus.ascent + fmStatus.descent) / 2f
            val statusText = "CHARGING"
            val textW = statusPaint.measureText(statusText)
            val iconSize = statusPaint.textSize * 1.1f
            val gap = scaleFactor * 4f

            val iconLeft = rightEdgeX - textW - gap - iconSize
            val iconTop = topAreaCenterY - (iconSize / 2f)
            drawBoltIcon(context, canvas, iconLeft, iconTop, iconSize, accentColorInt)
            canvas.drawText(statusText, rightEdgeX, statusY, statusPaint)

            statRightBound = iconLeft - (scaleFactor * 14f)
        }

        // 3. Middle Stacked Metadata Stats
        val statsStartX = cardRect.left + padX + pctWidth + (scaleFactor * 16f)
        if (statsStartX < statRightBound) {
            val statSize = (topAreaH * 0.32f).coerceIn(scaleFactor * 10f, scaleFactor * 13f)
            statPaint.textSize = statSize
            statPaint.textAlign = Paint.Align.LEFT
            val fmStat = statPaint.fontMetrics
            val lineSpacing = scaleFactor * 2f

            val stat1Y = topAreaCenterY - lineSpacing - fmStat.descent
            val stat2Y = topAreaCenterY + lineSpacing - fmStat.ascent

            canvas.drawText("• ${data.healthText}", statsStartX, stat1Y, statPaint)
            canvas.drawText("• ${data.secondaryStatText}", statsStartX, stat2Y, statPaint)
        }

        // =========================================================================
        // BRANCH 2: TALL / VERTICAL MODE (1x2, 2x4)
        // =========================================================================
    } else if (aspect < 0.72f) {
        val padX = cardW * 0.10f
        val padY = cardH * 0.07f
        val availW = cardW - (padX * 2f)

        // Top Row: Percentage + Charging Bolt
        val pctSize = (cardW * 0.32f).coerceIn(scaleFactor * 26f, scaleFactor * 52f)
        pctPaint.textSize = pctSize
        pctPaint.textAlign = Paint.Align.LEFT
        val fmPct = pctPaint.fontMetrics
        val pctY = cardRect.top + padY - fmPct.ascent
        canvas.drawText("${data.percentage}%", cardRect.left + padX, pctY, pctPaint)

        if (data.isCharging) {
            val iconSize = pctSize * 0.42f
            val iconLeft = cardRect.right - padX - iconSize
            val iconTop = pctY + fmPct.ascent + ((pctSize - iconSize) / 2f)
            drawBoltIcon(context, canvas, iconLeft, iconTop, iconSize, accentColorInt)
        }

        // Middle: Stacked Metadata Lines
        val statSize = (cardW * 0.088f).coerceIn(scaleFactor * 11f, scaleFactor * 14f)
        statPaint.textSize = statSize
        statPaint.textAlign = Paint.Align.LEFT
        val fmStat = statPaint.fontMetrics
        val statGap = scaleFactor * 5f

        val stat1Y = pctY + fmPct.descent + (scaleFactor * 10f) - fmStat.ascent
        val stat2Y = stat1Y + (fmStat.descent - fmStat.ascent) + statGap
        canvas.drawText("• ${data.healthText}", cardRect.left + padX, stat1Y, statPaint)
        canvas.drawText("• ${data.secondaryStatText}", cardRect.left + padX, stat2Y, statPaint)

        // Bottom: Editorial Vertical Segmented Ladder
        val ladderTopY = stat2Y + fmStat.descent + (cardH * 0.035f)
        val ladderBottomY = cardRect.bottom - padY
        val ladderH = (ladderBottomY - ladderTopY).coerceAtLeast(scaleFactor * 30f)

        val numSegments = 16
        val segGap = scaleFactor * 3.5f
        val totalGaps = segGap * (numSegments - 1)
        val segH = (ladderH - totalGaps) / numSegments
        val activeCount = ((data.percentage.coerceIn(0, 100) / 100f) * numSegments).toInt()

        for (i in 0 until numSegments) {
            val fromBottom = (numSegments - 1) - i
            val segTop = ladderTopY + i * (segH + segGap)
            val segBottom = segTop + segH
            val isActive = fromBottom < activeCount

            val segPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isActive) accentColorInt else trackColorInt
                style = Paint.Style.FILL
            }
            val r = segH / 2f
            canvas.drawRoundRect(RectF(cardRect.left + padX, segTop, cardRect.right - padX, segBottom), r, r, segPaint)
        }

        // =========================================================================
        // BRANCH 3: STANDARD SQUARE / BALANCED MODE (2x2)
        // =========================================================================
    } else {
        val pad = minOf(cardW, cardH) * 0.09f
        val availW = cardW - (pad * 2f)

        // 1. Header Percentage
        val pctSize = (cardH * 0.30f).coerceIn(scaleFactor * 32f, scaleFactor * 62f)
        pctPaint.textSize = pctSize
        pctPaint.textAlign = Paint.Align.LEFT
        val fmPct = pctPaint.fontMetrics
        val pctY = cardRect.top + pad - fmPct.ascent
        canvas.drawText("${data.percentage}%", cardRect.left + pad, pctY, pctPaint)

        if (data.isCharging) {
            val iconSize = cardH * 0.09f
            val iconLeft = cardRect.right - pad - iconSize
            val iconTop = cardRect.top + pad + (cardH * 0.02f)
            drawBoltIcon(context, canvas, iconLeft, iconTop, iconSize, accentColorInt)
        }

        // 2. Middle Metadata Stats
        val statSize = (cardH * 0.082f).coerceIn(scaleFactor * 11f, scaleFactor * 15f)
        statPaint.textSize = statSize
        statPaint.textAlign = Paint.Align.LEFT
        val fmStat = statPaint.fontMetrics
        val statLineHeight = (fmStat.descent - fmStat.ascent) + (scaleFactor * 4f)

        val stat1Y = pctY + fmPct.descent + (cardH * 0.04f) - fmStat.ascent
        val stat2Y = stat1Y + statLineHeight
        canvas.drawText("• ${data.healthText}", cardRect.left + pad, stat1Y, statPaint)
        canvas.drawText("• ${data.secondaryStatText}", cardRect.left + pad, stat2Y, statPaint)

        // 3. Bottom Segmented Bar Spanning Full Width
        val barH = (cardH * 0.10f).coerceIn(scaleFactor * 8f, scaleFactor * 15f).toInt()
        val barW = availW.toInt()
        val barBitmap = generateSegmentedBarBitmap(data.percentage, accentColorInt, trackColorInt, barW, barH)
        canvas.drawBitmap(barBitmap, cardRect.left + pad, cardRect.bottom - pad - barH, null)
    }

    return bitmap
}

// 7. MULTI-DEVICE STATS BENTO (4x2 Bento: 1 Hero Left + 2 Stacked Right)
fun generateMultiDeviceBatteryBitmap(
    context: Context,
    data: DetailedBatteryData,
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
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val accentColor = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val trackColor = if (isLight) 0x14000000 else 0x1FFFFFFF
    val tileBgColor = if (isLight) 0x0A000000 else 0x14FFFFFF

    val margin = scaleFactor * 1.5f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val targetRatio = 2.0f
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
    val aspectRatio = cardW / cardH.coerceAtLeast(1f)
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)

    // Outer Card Fill & Stroke
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val outerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x12000000 else 0x1AFFFFFF
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.8f
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, outerBorderPaint)

    // UNIFIED EQUAL SPACING: Outer padding and inner gaps are identical
    val spacing = (minOf(cardW, cardH) * 0.055f).coerceIn(scaleFactor * 6f, scaleFactor * 14f)
    val pad = spacing
    val gap = spacing

    val concentricRadius = (cardCornerRadius - pad).coerceAtLeast(scaleFactor * 8f)
    val sq = (scaleFactor * 8f).coerceAtMost(concentricRadius * 0.5f)

    val tileBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tileBgColor }
    val tileBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x10000000 else 0x14FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.75f
    }
    val trackPaintObj = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = trackColor }
    val fillPaintObj = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }

    fun drawTileFrame(rect: RectF, radii: FloatArray): Path {
        val path = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }
        canvas.drawPath(path, tileBgPaint)
        canvas.drawPath(path, tileBorderPaint)
        return path
    }

    val heroRect: RectF
    val tempRect: RectF
    val voltRect: RectF
    val heroRadii: FloatArray
    val tempRadii: FloatArray
    val voltRadii: FloatArray

    val availW = cardW - (pad * 2f)
    val availH = cardH - (pad * 2f)

    // =========================================================================
    // GEOMETRY PARTITIONING
    // =========================================================================
    when {
        // Mode 1: Tall / Column (1x2, 1x3) -> Stacked Rows
        isResponsive && aspectRatio < 0.85f -> {
            val totalGaps = gap * 2f
            val heroH = (availH - totalGaps) * 0.46f
            val statH = (availH - totalGaps - heroH) / 2f

            heroRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.top + pad + heroH)
            heroRadii = floatArrayOf(concentricRadius, concentricRadius, concentricRadius, concentricRadius, sq, sq, sq, sq)

            val tempTop = heroRect.bottom + gap
            tempRect = RectF(cardRect.left + pad, tempTop, cardRect.right - pad, tempTop + statH)
            tempRadii = floatArrayOf(sq, sq, sq, sq, sq, sq, sq, sq)

            val voltTop = tempRect.bottom + gap
            voltRect = RectF(cardRect.left + pad, voltTop, cardRect.right - pad, cardRect.bottom - pad)
            voltRadii = floatArrayOf(sq, sq, sq, sq, concentricRadius, concentricRadius, concentricRadius, concentricRadius)
        }

        // Mode 2: Ultra-Wide Ribbon (5x1, aspect >= 3.0) -> 3 Columns side-by-side
        isResponsive && aspectRatio >= 3.0f -> {
            val totalGaps = gap * 2f
            val heroW = (availW - totalGaps) * 0.38f
            val statW = (availW - totalGaps - heroW) / 2f

            heroRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.left + pad + heroW, cardRect.bottom - pad)
            heroRadii = floatArrayOf(concentricRadius, concentricRadius, sq, sq, sq, sq, concentricRadius, concentricRadius)

            val tempLeft = heroRect.right + gap
            tempRect = RectF(tempLeft, cardRect.top + pad, tempLeft + statW, cardRect.bottom - pad)
            tempRadii = floatArrayOf(sq, sq, sq, sq, sq, sq, sq, sq)

            val voltLeft = tempRect.right + gap
            voltRect = RectF(voltLeft, cardRect.top + pad, cardRect.right - pad, cardRect.bottom - pad)
            voltRadii = floatArrayOf(sq, sq, concentricRadius, concentricRadius, concentricRadius, concentricRadius, sq, sq)
        }

        // Mode 3: STANDARD BENTO (Fixed Mode & Standard 4x2 / 3x2) -> 1 Hero Left + 2 Stacked Right
        else -> {
            val heroW = (availW - gap) * 0.44f
            heroRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.left + pad + heroW, cardRect.bottom - pad)
            heroRadii = floatArrayOf(concentricRadius, concentricRadius, sq, sq, sq, sq, concentricRadius, concentricRadius)

            val rightX = heroRect.right + gap
            val rightW = availW - heroW - gap
            val statH = (availH - gap) / 2f

            tempRect = RectF(rightX, cardRect.top + pad, rightX + rightW, cardRect.top + pad + statH)
            tempRadii = floatArrayOf(sq, sq, concentricRadius, concentricRadius, sq, sq, sq, sq)

            voltRect = RectF(rightX, tempRect.bottom + gap, rightX + rightW, cardRect.bottom - pad)
            voltRadii = floatArrayOf(sq, sq, sq, sq, concentricRadius, concentricRadius, sq, sq)
        }
    }

    // =========================================================================
    // 1. RENDER HERO TILE (Percentage Directly on Top of Progress Bar)
    // =========================================================================
    val heroPath = drawTileFrame(heroRect, heroRadii)
    canvas.save()
    canvas.clipPath(heroPath)

    val hPadX = (heroRect.width() * 0.10f).coerceIn(scaleFactor * 8f, scaleFactor * 16f)
    val hPadY = (heroRect.height() * 0.10f).coerceIn(scaleFactor * 6f, scaleFactor * 14f)
    val heroContentW = heroRect.width() - (hPadX * 2f)

    // A. Bottom Linear Capsule Bar
    val barH = (heroRect.height() * 0.10f).coerceIn(scaleFactor * 5f, scaleFactor * 11f)
    val barBottom = heroRect.bottom - hPadY
    val barTop = barBottom - barH
    val heroTrackRect = RectF(heroRect.left + hPadX, barTop, heroRect.right - hPadX, barBottom)
    canvas.drawRoundRect(heroTrackRect, barH / 2f, barH / 2f, trackPaintObj)

    val fillRatio = (data.percentage.coerceIn(0, 100) / 100f)
    val fillW = heroTrackRect.width() * fillRatio
    if (fillW > 0f) {
        val heroFillRect = RectF(heroTrackRect.left, barTop, heroTrackRect.left + fillW.coerceAtLeast(barH), barBottom)
        canvas.drawRoundRect(heroFillRect, barH / 2f, barH / 2f, fillPaintObj)
    }

    // B. Top Header: "PHONE" on Left, "⚡ CHARGING" on Right
    val topHeaderH = (heroRect.height() * 0.22f).coerceIn(scaleFactor * 12f, scaleFactor * 24f)
    val topHeaderTop = heroRect.top + hPadY
    val tagSize = (topHeaderH * 0.60f).coerceIn(scaleFactor * 8.5f, scaleFactor * 13f)

    val heroTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = tagSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.LEFT
    }
    val fmTag = heroTagPaint.fontMetrics
    val tagY = topHeaderTop + (topHeaderH / 2f) - (fmTag.ascent + fmTag.descent) / 2f
    canvas.drawText("PHONE", heroRect.left + hPadX, tagY, heroTagPaint)

    if (data.isCharging) {
        val chargePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = tagSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.RIGHT
        }
        val fmCharge = chargePaint.fontMetrics
        val boltSize = tagSize * 1.05f
        val boltGap = scaleFactor * 3f
        val chargeRight = heroRect.right - hPadX

        val fullText = "CHARGING"
        val fullTextW = chargePaint.measureText(fullText)
        val tagW = heroTagPaint.measureText("PHONE")

        if (heroContentW - tagW - (scaleFactor * 8f) >= fullTextW + boltGap + boltSize) {
            val iconLeft = chargeRight - fullTextW - boltGap - boltSize
            val iconTop = tagY + fmCharge.ascent + ((tagSize - boltSize) / 2f)
            drawBoltIcon(context, canvas, iconLeft, iconTop, boltSize, accentColor)
            canvas.drawText(fullText, chargeRight, tagY, chargePaint)
        } else {
            val iconLeft = chargeRight - boltSize
            val iconTop = tagY + fmTag.ascent + ((tagSize - boltSize) / 2f)
            drawBoltIcon(context, canvas, iconLeft, iconTop, boltSize, accentColor)
        }
    }

    // C. Percentage Grounded Directly Above the Progress Bar
    val availablePctH = (barTop - (tagY + fmTag.descent) - (scaleFactor * 6f)).coerceAtLeast(1f)
    var pctTextSize = (availablePctH * 0.72f).coerceIn(scaleFactor * 18f, scaleFactor * 48f)

    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = pctTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.LEFT
    }

    val pctText = "${data.percentage}%"
    while (pctPaint.measureText(pctText) > heroContentW && pctTextSize > scaleFactor * 10f) {
        pctTextSize -= scaleFactor * 0.5f
        pctPaint.textSize = pctTextSize
    }

    val fmPct = pctPaint.fontMetrics
    // Placed directly on top of the bar with a 4dp gap
    val gapAboveBar = scaleFactor * 4f
    val pctY = barTop - gapAboveBar - fmPct.descent
    canvas.drawText(pctText, heroRect.left + hPadX, pctY, pctPaint)
    canvas.restore()

    // =========================================================================
    // 2. RENDER STAT CARDS (TEMPERATURE & VOLTAGE)
    // =========================================================================
    fun renderStatCard(rect: RectF, radii: FloatArray, fullLabel: String, shortLabel: String, valText: String, statRatio: Float) {
        val tilePath = drawTileFrame(rect, radii)
        canvas.save()
        canvas.clipPath(tilePath)

        val sPadX = (rect.width() * 0.10f).coerceIn(scaleFactor * 8f, scaleFactor * 16f)
        val sPadY = (rect.height() * 0.11f).coerceIn(scaleFactor * 6f, scaleFactor * 12f)
        val availTileW = rect.width() - (sPadX * 2f)

        // Bottom Progress Bar
        val sBarH = (rect.height() * 0.11f).coerceIn(scaleFactor * 4f, scaleFactor * 8f)
        val sBarBottom = rect.bottom - sPadY
        val sBarTop = sBarBottom - sBarH
        val sTrackRect = RectF(rect.left + sPadX, sBarTop, rect.right - sPadX, sBarBottom)
        canvas.drawRoundRect(sTrackRect, sBarH / 2f, sBarH / 2f, trackPaintObj)

        val sFillW = sTrackRect.width() * statRatio.coerceIn(0.05f, 1f)
        val sFillRect = RectF(sTrackRect.left, sBarTop, sTrackRect.left + sFillW.coerceAtLeast(sBarH), sBarBottom)
        canvas.drawRoundRect(sFillRect, sBarH / 2f, sBarH / 2f, fillPaintObj)

        val contentTop = rect.top + sPadY
        val contentAvailH = (sBarTop - contentTop - (scaleFactor * 2f)).coerceAtLeast(1f)

        val isWideStat = rect.width() >= rect.height() * 1.85f

        if (isWideStat) {
            // Side-by-Side Mode for flat tiles
            val textSize = (contentAvailH * 0.70f).coerceIn(scaleFactor * 10f, scaleFactor * 20f)
            val sValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryTextColor
                this.textSize = textSize
                typeface = getSlateFont(context, weight = 700)
                textAlign = Paint.Align.RIGHT
            }

            val sLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = secondaryTextColor
                this.textSize = (textSize * 0.75f).coerceIn(scaleFactor * 8.5f, scaleFactor * 13f)
                typeface = getSlateFont(context, weight = 700)
                textAlign = Paint.Align.LEFT
            }

            var labelToUse = fullLabel
            val valW = sValPaint.measureText(valText)
            val gapBetween = scaleFactor * 6f

            if (sLabelPaint.measureText(labelToUse) + valW + gapBetween > availTileW) {
                labelToUse = shortLabel
            }
            while (sLabelPaint.measureText(labelToUse) + valW + gapBetween > availTileW && sLabelPaint.textSize > scaleFactor * 7f) {
                sLabelPaint.textSize -= scaleFactor * 0.5f
            }

            val fmVal = sValPaint.fontMetrics
            val fmLabel = sLabelPaint.fontMetrics
            val centerY = contentTop + (contentAvailH / 2f)

            canvas.drawText(labelToUse, rect.left + sPadX, centerY - (fmLabel.ascent + fmLabel.descent) / 2f, sLabelPaint)
            canvas.drawText(valText, rect.right - sPadX, centerY - (fmVal.ascent + fmVal.descent) / 2f, sValPaint)
        } else {
            // Stacked Vertical Layout: Label at Top, Value Grounded on Top of Bar
            val titleSlotH = contentAvailH * 0.40f
            val valueSlotH = contentAvailH * 0.60f

            val sLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = secondaryTextColor
                textSize = (titleSlotH * 0.80f).coerceIn(scaleFactor * 7.5f, scaleFactor * 12f)
                typeface = getSlateFont(context, weight = 700)
                textAlign = Paint.Align.LEFT
            }

            var labelToUse = fullLabel
            if (sLabelPaint.measureText(labelToUse) > availTileW) {
                labelToUse = shortLabel
            }
            while (sLabelPaint.measureText(labelToUse) > availTileW && sLabelPaint.textSize > scaleFactor * 6.5f) {
                sLabelPaint.textSize -= scaleFactor * 0.5f
            }

            val fmLabel = sLabelPaint.fontMetrics
            val labelY = contentTop + (titleSlotH / 2f) - (fmLabel.ascent + fmLabel.descent) / 2f
            canvas.drawText(labelToUse, rect.left + sPadX, labelY, sLabelPaint)

            val sValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryTextColor
                textSize = (valueSlotH * 0.82f).coerceIn(scaleFactor * 11f, scaleFactor * 24f)
                typeface = getSlateFont(context, weight = 700)
                textAlign = Paint.Align.LEFT
            }

            while (sValPaint.measureText(valText) > availTileW && sValPaint.textSize > scaleFactor * 8f) {
                sValPaint.textSize -= scaleFactor * 0.5f
            }

            val fmVal = sValPaint.fontMetrics
            val valY = sBarTop - (scaleFactor * 3.5f) - fmVal.descent
            canvas.drawText(valText, rect.left + sPadX, valY, sValPaint)
        }

        canvas.restore()
    }

    val tempVal = data.tempText.replace("°C", "").trim().toFloatOrNull() ?: 35f
    val tempRatio = (tempVal / 50f).coerceIn(0.1f, 1f)
    renderStatCard(tempRect, tempRadii, "TEMPERATURE", "TEMP", data.tempText, tempRatio)

    val voltVal = data.voltageText.replace("V", "").trim().toFloatOrNull() ?: 3.8f
    val voltRatio = ((voltVal - 3.2f) / (4.4f - 3.2f)).coerceIn(0.1f, 1f)
    renderStatCard(voltRect, voltRadii, "VOLTAGE", "VOLT", data.voltageText, voltRatio)

    return bitmap
}


// 8. Dot Matrix LED (Strict Uniform Fixed Card: 4x2)
fun generateDotMatrixLEDBitmap(
    context: Context,
    text: String,
    activeColorInt: Int,
    dimColorInt: Int,
    bgColorInt: Int,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val margin = scaleFactor * 1.5f
    val maxAvailW = w - (margin * 2f)
    val maxAvailH = h - (margin * 2f)

    val rows = 9
    val columns = 23 // Accommodates "100%" (23 cols) without clipping, centers "87%" (17 cols) symmetrically

    // Exact padding ratio in units of cellSize between the card border and the outer dot grid
    val padRatio = 0.70f
    val totalColsUnits = columns + (padRatio * 2f)
    val totalRowsUnits = rows + (padRatio * 2f)

    // Cell size scaled so the card fits inside widget bounds while maintaining equal padding on all sides
    val cellSize = minOf(maxAvailW / totalColsUnits, maxAvailH / totalRowsUnits)
    val cardW = totalColsUnits * cellSize
    val cardH = totalRowsUnits * cellSize
    val pad = padRatio * cellSize

    // Center card inside widget canvas
    val leftX = (w - cardW) / 2f
    val topY = (h - cardH) / 2f
    val cardRect = RectF(leftX, topY, leftX + cardW, topY + cardH)

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColorInt
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    // Subtle edge border for dark surface elevation
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x1AFFFFFF
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.8f
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, borderPaint)

    // Dot Grid Bounds: Exactly 'pad' distance from top, bottom, left, and right of cardRect
    val dotRadius = cellSize * 0.36f
    val startX = leftX + pad
    val startY = topY + pad

    val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dimColorInt
        style = Paint.Style.FILL
    }

    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = activeColorInt
        style = Paint.Style.FILL
    }

    // 1. Draw Inactive Dim Dot Matrix
    for (r in 0 until rows) {
        for (c in 0 until columns) {
            val cx = startX + c * cellSize + cellSize / 2f
            val cy = startY + r * cellSize + cellSize / 2f
            canvas.drawCircle(cx, cy, dotRadius, dimPaint)
        }
    }

    // 2. Glyph Definitions (5x7)
    val fontMap = mapOf(
        '0' to arrayOf(0b01110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110),
        '1' to arrayOf(0b00100, 0b01100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110),
        '2' to arrayOf(0b01110, 0b10001, 0b00001, 0b00010, 0b00100, 0b01000, 0b11111),
        '3' to arrayOf(0b11110, 0b00001, 0b00001, 0b00110, 0b00001, 0b00001, 0b11110),
        '4' to arrayOf(0b00010, 0b00110, 0b01010, 0b10010, 0b11111, 0b00010, 0b00010),
        '5' to arrayOf(0b11111, 0b10000, 0b11110, 0b00001, 0b00001, 0b10001, 0b01110),
        '6' to arrayOf(0b00110, 0b01000, 0b10000, 0b11110, 0b10001, 0b10001, 0b01110),
        '7' to arrayOf(0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b01000, 0b01000),
        '8' to arrayOf(0b01110, 0b10001, 0b10001, 0b01110, 0b10001, 0b10001, 0b01110),
        '9' to arrayOf(0b01110, 0b10001, 0b10001, 0b01111, 0b00001, 0b00010, 0b01100),
        '%' to arrayOf(0b11001, 0b11010, 0b00010, 0b00100, 0b01000, 0b01011, 0b10011)
    )

    val glyphWidth = 5
    val glyphGap = 1
    val glyphHeight = 7
    val textWidthCols = text.length * glyphWidth + (text.length - 1) * glyphGap

    // Vertical center: 1 row of dim dots on top, 1 on bottom
    val startRow = (rows - glyphHeight) / 2
    // Horizontal center: Equal dim dots on left and right
    var startCol = (columns - textWidthCols) / 2

    // 3. Draw Active Characters
    text.forEach { char ->
        val glyph = fontMap[char]
        if (glyph != null && startCol + glyphWidth <= columns) {
            for (r in 0 until glyphHeight) {
                val rowBits = glyph[r]
                for (bit in 0 until glyphWidth) {
                    if ((rowBits and (1 shl (4 - bit))) != 0) {
                        val c = startCol + bit
                        val targetRow = startRow + r
                        if (targetRow in 0 until rows && c in 0 until columns) {
                            val cx = startX + c * cellSize + cellSize / 2f
                            val cy = startY + targetRow * cellSize + cellSize / 2f
                            canvas.drawCircle(cx, cy, dotRadius, activePaint)
                        }
                    }
                }
            }
            startCol += glyphWidth + glyphGap
        }
    }

    return bitmap
}

// 9. Dot Level Wide (Adaptive: 4x2 Bento, 4x1 Ribbon, 1x3 Tall Column)
fun generateCenteredLevelBitmap(
    context: Context,
    percentage: Int,
    config: SlateWidgetConfig,
    isResponsive: Boolean = false,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val margin = scaleFactor * 1.5f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val targetRatio = 2.0f
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
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val activeColor = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val dimColor = if (isLight) 0x14000000 else 0x1AFFFFFF

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x12000000 else 0x1AFFFFFF
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.8f
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, borderPaint)

    // Tight, balanced edge padding
    val pad = (minOf(cardW, cardH) * 0.065f).coerceIn(scaleFactor * 6f, scaleFactor * 14f)
    val availW = cardW - (pad * 2f)
    val availH = cardH - (pad * 2f)

    // Dynamically calculate both columns and rows based on aspect ratio
    val targetCellPx = scaleFactor * 16f
    val cols = (availW / targetCellPx).toInt().coerceAtLeast(3)
    val rows = (availH / targetCellPx).toInt().coerceAtLeast(3)

    // Ensure cell size fits both dimensions and keeps dots square
    val cellSize = minOf(availW / cols, availH / rows)
    val dotRadius = cellSize * 0.34f
    val gridW = cols * cellSize
    val gridH = rows * cellSize

    // Center the matrix inside the card
    val startX = cardRect.left + (cardW - gridW) / 2f
    val startY = cardRect.top + (cardH - gridH) / 2f

    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = activeColor
        style = Paint.Style.FILL
    }

    val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dimColor
        style = Paint.Style.FILL
    }

    val totalDots = cols * rows
    val activeDotsCount = (percentage.coerceIn(0, 100) * totalDots) / 100

    for (r in 0 until rows) {
        val rowFromBottom = (rows - 1) - r
        for (c in 0 until cols) {
            val dotIndex = rowFromBottom * cols + c
            val cx = startX + c * cellSize + cellSize / 2f
            val cy = startY + r * cellSize + cellSize / 2f

            val paint = if (dotIndex < activeDotsCount) activePaint else dimPaint
            canvas.drawCircle(cx, cy, dotRadius, paint)
        }
    }

    return bitmap
}

// Backward-compatible overload for legacy pixel-based calls
fun generateCenteredLevelBitmap(
    context: Context,
    percentage: Int,
    activeColorInt: Int,
    dimColorInt: Int,
    bgColorInt: Int,
    targetWidthPx: Int,
    targetHeightPx: Int,
    isResponsive: Boolean = false
): Bitmap {
    val scaleFactor = maxOf(context.resources.displayMetrics.density, 3.5f)
    val wDp = (targetWidthPx / scaleFactor).toInt().coerceAtLeast(1)
    val hDp = (targetHeightPx / scaleFactor).toInt().coerceAtLeast(1)
    val fakeConfig = SlateWidgetConfig(
        accentColorHex = (activeColorInt.toLong() and 0xFFFFFFFFL),
        backgroundColorHex = (bgColorInt.toLong() and 0xFFFFFFFFL)
    )
    return generateCenteredLevelBitmap(context, percentage, fakeConfig, isResponsive, wDp, hDp)
}


// 10. Precision Dash Battery Strip (Adaptive 20-Segment Meter)
fun generateHorizontalStripBitmap(
    context: Context,
    data: DetailedBatteryData,
    config: SlateWidgetConfig,
    isResponsive: Boolean = false,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val margin = scaleFactor * 1.5f
    val cardRect = RectF(margin, margin, w - margin, h - margin)

    val cardW = cardRect.width()
    val cardH = cardRect.height()
    val aspect = cardW / cardH.coerceAtLeast(1f)
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val activeColor = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val trackBgColor = if (isLight) 0x0A000000 else 0x12FFFFFF
    val trackBorderColor = if (isLight) 0x10000000 else 0x1AFFFFFF
    val inactiveSegmentColor = if (isLight) 0x16000000 else 0x1EFFFFFF

    // 1. Main Card Surface & Border
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x12000000 else 0x1AFFFFFF
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.8f
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, borderPaint)

    // 20 Precision Segments (1 Segment = 5%)
    val totalSegments = 20
    val activeSegmentsCount = ((data.percentage.coerceIn(0, 100) / 100f) * totalSegments).toInt()

    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = activeColor
        style = Paint.Style.FILL
    }
    val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = inactiveSegmentColor
        style = Paint.Style.FILL
    }
    val trackBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = trackBgColor
        style = Paint.Style.FILL
    }
    val trackBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = trackBorderColor
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.75f
    }

    // =========================================================================
    // BRANCH 1: INLINE STRIP (Reserved for true wide ribbons: 3x1, 4x1, 5x1 -> aspect >= 2.3)
    // =========================================================================
    if (aspect >= 2.3f) {
        val padX = (cardW * 0.055f).coerceIn(scaleFactor * 12f, scaleFactor * 22f)
        val padY = (cardH * 0.12f).coerceIn(scaleFactor * 6f, scaleFactor * 12f)
        val availH = cardH - (padY * 2f)
        val centerY = cardRect.top + (cardH / 2f)

        // A. Percentage Typography
        var pctSize = (availH * 0.80f).coerceIn(scaleFactor * 18f, scaleFactor * 40f)
        val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = pctSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.LEFT
        }

        val pctText = "${data.percentage}%"
        val maxPctW = cardW * 0.30f
        while (pctPaint.measureText(pctText) > maxPctW && pctSize > scaleFactor * 14f) {
            pctSize -= scaleFactor * 0.5f
            pctPaint.textSize = pctSize
        }

        val fmPct = pctPaint.fontMetrics
        val textY = centerY - (fmPct.ascent + fmPct.descent) / 2f
        canvas.drawText(pctText, cardRect.left + padX, textY, pctPaint)

        var contentRight = cardRect.left + padX + pctPaint.measureText(pctText)

        // Visual height of the numerals (baseline to cap height)
        val pctCapHeight = Math.abs(fmPct.ascent)

        // B. Charging Bolt Indicator
        if (data.isCharging) {
            val boltSize = (pctCapHeight * 0.80f).coerceAtLeast(scaleFactor * 11f)
            val iconLeft = contentRight + (scaleFactor * 5f)
            val iconTop = centerY - (boltSize / 2f)
            drawBoltIcon(context, canvas, iconLeft, iconTop, boltSize, activeColor)
            contentRight = iconLeft + boltSize
        }

        // C. Precision Dash Well with Pills at 90% of Percentage Height
        val wellLeft = contentRight + (scaleFactor * 12f)
        val wellRight = cardRect.right - padX
        val wellW = wellRight - wellLeft

        if (wellW > scaleFactor * 30f) {
            // Target pill height: 90% of the percentage text's capital height
            val segH = (pctCapHeight * 0.90f).coerceIn(scaleFactor * 10f, availH * 0.82f)
            val wellPaddingY = (scaleFactor * 4f).coerceIn(scaleFactor * 3f, scaleFactor * 6f)
            val wellH = segH + (wellPaddingY * 2f)
            val wellY = centerY - (wellH / 2f)
            val wellRadius = wellH / 2f
            val wellRect = RectF(wellLeft, wellY, wellRight, wellY + wellH)

            // Recessed track slot
            canvas.drawRoundRect(wellRect, wellRadius, wellRadius, trackBgPaint)
            canvas.drawRoundRect(wellRect, wellRadius, wellRadius, trackBorderPaint)

            // Usable width and inner segment calculations
            val insetX = wellH * 0.28f
            val usableW = (wellW - (insetX * 2f)).coerceAtLeast(1f)
            val segTop = wellY + wellPaddingY
            val gap = (scaleFactor * 2.2f).coerceAtMost(usableW * 0.02f)
            val segW = ((usableW - (gap * (totalSegments - 1))) / totalSegments).coerceAtLeast(1f)
            val segRadius = minOf(segW / 2f, segH / 2f)

            for (i in 0 until totalSegments) {
                val sLeft = wellLeft + insetX + i * (segW + gap)
                val sRight = sLeft + segW
                val segRect = RectF(sLeft, segTop, sRight, segTop + segH)
                val paint = if (i < activeSegmentsCount) activePaint else inactivePaint
                canvas.drawRoundRect(segRect, segRadius, segRadius, paint)
            }
        }

        // =========================================================================
        // BRANCH 2: TALL COLUMN (1x2, 1x3, 1x4 -> aspect < 0.90)
        // =========================================================================
    } else if (aspect < 0.90f) {
        val padX = cardW * 0.11f
        val padY = cardH * 0.08f
        val availW = cardW - (padX * 2f)

        val statusText = if (data.isCharging) "CHARGING" else "BATTERY"
        val statusColor = if (data.isCharging) activeColor else secondaryTextColor
        val statusSize = (cardW * 0.095f).coerceIn(scaleFactor * 9.5f, scaleFactor * 13f)

        val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statusColor
            textSize = statusSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.LEFT
        }
        val fmStatus = statusPaint.fontMetrics
        val statusY = cardRect.top + padY - fmStatus.ascent

        if (data.isCharging) {
            val iconSize = statusSize * 1.05f
            val iconTop = statusY + fmStatus.ascent + ((statusSize - iconSize) / 2f)
            drawBoltIcon(context, canvas, cardRect.left + padX, iconTop, iconSize, activeColor)
            canvas.drawText(statusText, cardRect.left + padX + iconSize + (scaleFactor * 4f), statusY, statusPaint)
        } else {
            canvas.drawText(statusText, cardRect.left + padX, statusY, statusPaint)
        }

        var pctSize = (cardW * 0.32f).coerceIn(scaleFactor * 24f, scaleFactor * 46f)
        val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = pctSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.LEFT
        }

        val pctText = "${data.percentage}%"
        while (pctPaint.measureText(pctText) > availW && pctSize > scaleFactor * 14f) {
            pctSize -= scaleFactor * 0.5f
            pctPaint.textSize = pctSize
        }

        val fmPct = pctPaint.fontMetrics
        val pctY = statusY + fmStatus.descent + (scaleFactor * 6f) - fmPct.ascent
        canvas.drawText(pctText, cardRect.left + padX, pctY, pctPaint)

        // Vertical Precision Ladder
        val wellTop = pctY + fmPct.descent + (cardH * 0.035f)
        val wellBottom = cardRect.bottom - padY
        val wellH = (wellBottom - wellTop).coerceAtLeast(scaleFactor * 30f)
        val wellRadius = scaleFactor * 12f
        val wellRect = RectF(cardRect.left + padX, wellTop, cardRect.right - padX, wellBottom)

        canvas.drawRoundRect(wellRect, wellRadius, wellRadius, trackBgPaint)
        canvas.drawRoundRect(wellRect, wellRadius, wellRadius, trackBorderPaint)

        val insetX = scaleFactor * 8f
        val insetY = scaleFactor * 8f
        val usableH = (wellH - (insetY * 2f)).coerceAtLeast(1f)
        val usableW = (availW - (insetX * 2f)).coerceAtLeast(1f)

        val gap = (scaleFactor * 2.2f).coerceAtMost(usableH * 0.02f)
        val segH = ((usableH - (gap * (totalSegments - 1))) / totalSegments).coerceAtLeast(1f)
        val segRadius = segH / 2f

        for (i in 0 until totalSegments) {
            val fromBottom = (totalSegments - 1) - i
            val sTop = wellTop + insetY + i * (segH + gap)
            val sBottom = sTop + segH
            val segRect = RectF(cardRect.left + padX + insetX, sTop, cardRect.left + padX + insetX + usableW, sBottom)
            val paint = if (fromBottom < activeSegmentsCount) activePaint else inactivePaint
            canvas.drawRoundRect(segRect, segRadius, segRadius, paint)
        }

        // =========================================================================
        // BRANCH 3: TOP-DOWN / BENTO MODE (Includes 2x1, 2x2, 3x2, 4x2 -> 0.90 <= aspect < 2.3)
        // =========================================================================
    } else {
        val pad = (minOf(cardW, cardH) * 0.085f).coerceIn(scaleFactor * 10f, scaleFactor * 18f)
        val availW = cardW - (pad * 2f)

        val statusText = if (data.isCharging) "CHARGING" else "BATTERY"
        val statusColor = if (data.isCharging) activeColor else secondaryTextColor
        val statusSize = (cardH * 0.085f).coerceIn(scaleFactor * 10f, scaleFactor * 14f)

        val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statusColor
            textSize = statusSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.LEFT
        }
        val fmStatus = statusPaint.fontMetrics
        val statusY = cardRect.top + pad - fmStatus.ascent

        if (data.isCharging) {
            val iconSize = statusSize * 1.1f
            val iconTop = statusY + fmStatus.ascent + ((statusSize - iconSize) / 2f)
            drawBoltIcon(context, canvas, cardRect.left + pad, iconTop, iconSize, activeColor)
            canvas.drawText(statusText, cardRect.left + pad + iconSize + (scaleFactor * 4f), statusY, statusPaint)
        } else {
            canvas.drawText(statusText, cardRect.left + pad, statusY, statusPaint)
        }

        // Substantial bottom well
        val wellH = (cardH * 0.22f).coerceIn(scaleFactor * 20f, scaleFactor * 32f)
        val wellY = cardRect.bottom - pad - wellH
        val wellRadius = wellH / 2f
        val wellRect = RectF(cardRect.left + pad, wellY, cardRect.right - pad, wellY + wellH)

        canvas.drawRoundRect(wellRect, wellRadius, wellRadius, trackBgPaint)
        canvas.drawRoundRect(wellRect, wellRadius, wellRadius, trackBorderPaint)

        val insetX = wellH * 0.28f
        val insetY = wellH * 0.18f
        val usableW = (availW - (insetX * 2f)).coerceAtLeast(1f)
        val segH = (wellH - (insetY * 2f)).coerceAtLeast(scaleFactor * 6f)
        val segTop = wellY + insetY

        val gap = (scaleFactor * 2.2f).coerceAtMost(usableW * 0.02f)
        val segW = ((usableW - (gap * (totalSegments - 1))) / totalSegments).coerceAtLeast(1f)
        val segRadius = minOf(segW / 2f, segH / 2f)

        for (i in 0 until totalSegments) {
            val sLeft = cardRect.left + pad + insetX + i * (segW + gap)
            val sRight = sLeft + segW
            val segRect = RectF(sLeft, segTop, sRight, segTop + segH)
            val paint = if (i < activeSegmentsCount) activePaint else inactivePaint
            canvas.drawRoundRect(segRect, segRadius, segRadius, paint)
        }

        // Center Hero Percentage
        val centerTop = statusY + fmStatus.descent
        val centerH = wellY - centerTop
        var pctSize = (centerH * 0.72f).coerceIn(scaleFactor * 26f, scaleFactor * 52f)

        val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = pctSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.LEFT
        }

        val pctText = "${data.percentage}%"
        while (pctPaint.measureText(pctText) > availW && pctSize > scaleFactor * 14f) {
            pctSize -= scaleFactor * 0.5f
            pctPaint.textSize = pctSize
        }

        val fmPct = pctPaint.fontMetrics
        val pctY = centerTop + (centerH / 2f) - (fmPct.ascent + fmPct.descent) / 2f
        canvas.drawText(pctText, cardRect.left + pad, pctY, pctPaint)
    }

    return bitmap
}


// 11. 5-Pill Gauge Tile (Strict 1:1 Square)
fun generateSegmentedPillTileBitmap(
    context: Context,
    data: DetailedBatteryData,
    config: SlateWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val margin = scaleFactor * 1.5f
    // Strictly locked to a centered 1:1 square
    val cardSize = minOf(w, h) - (margin * 2f)
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f
    val cardRect = RectF(leftX, topY, leftX + cardSize, topY + cardSize)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val accentColor = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val dimColor = if (isLight) 0x1F000000 else 0x26FFFFFF
    val enclosureBgColor = if (isLight) 0x0A000000 else 0x14FFFFFF

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x12000000 else 0x1AFFFFFF
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.8f
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, borderPaint)

    val pad = cardSize * 0.10f

    // Header Percentage Text
    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (data.isCharging) accentColor else primaryTextColor
        textSize = cardSize * 0.22f
        textAlign = Paint.Align.CENTER
        typeface = getSlateFont(context, weight = 700)
    }
    val fontMetricsPct = pctPaint.fontMetrics
    val textY = topY + pad + (cardSize * 0.08f) - (fontMetricsPct.ascent + fontMetricsPct.descent) / 2f
    canvas.drawText("${data.percentage}%", leftX + (cardSize / 2f), textY, pctPaint)

    // Recessed Capsule Well
    val enclosureW = cardSize * 0.78f
    val enclosureH = cardSize * 0.36f
    val enclosureLeft = leftX + (cardSize - enclosureW) / 2f
    val enclosureTop = topY + (cardSize * 0.37f)
    val enclosureRect = RectF(enclosureLeft, enclosureTop, enclosureLeft + enclosureW, enclosureTop + enclosureH)

    val enclosurePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = enclosureBgColor
        style = Paint.Style.FILL
    }
    val enclosureCornerRadius = enclosureH * 0.30f
    canvas.drawRoundRect(enclosureRect, enclosureCornerRadius, enclosureCornerRadius, enclosurePaint)

    val enclosureBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x10000000 else 0x1AFFFFFF
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.75f
    }
    canvas.drawRoundRect(enclosureRect, enclosureCornerRadius, enclosureCornerRadius, enclosureBorderPaint)

    // 5 Precision Vertical Pills
    val totalBars = 5
    val padX = enclosureW * 0.08f
    val padY = enclosureH * 0.14f

    val innerW = enclosureW - (padX * 2f)
    val innerH = enclosureH - (padY * 2f)

    val barSpacing = innerW * 0.06f
    val barW = (innerW - (barSpacing * (totalBars - 1))) / totalBars
    val barCornerRadius = barW * 0.35f

    val barDimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dimColor
        style = Paint.Style.FILL
    }
    val barActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }

    for (i in 0 until totalBars) {
        val barLeft = enclosureLeft + padX + i * (barW + barSpacing)
        val barRight = barLeft + barW
        val barTop = enclosureTop + padY
        val barBottom = barTop + innerH

        val fullBarRect = RectF(barLeft, barTop, barRight, barBottom)
        canvas.drawRoundRect(fullBarRect, barCornerRadius, barCornerRadius, barDimPaint)

        val barPct = (data.percentage - (i * 20)).coerceIn(0, 20)
        val fillRatio = barPct / 20f

        if (fillRatio > 0f) {
            val activeH = innerH * fillRatio
            val activeTop = barBottom - activeH
            val activeRect = RectF(barLeft, activeTop, barRight, barBottom)
            canvas.drawRoundRect(activeRect, barCornerRadius, barCornerRadius, barActivePaint)
        }
    }

    // Status Footer
    val footerText = if (data.isCharging) "CHARGING" else "DISCHARGING"
    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (data.isCharging) accentColor else secondaryTextColor
        textSize = cardSize * 0.075f
        textAlign = Paint.Align.CENTER
        typeface = getSlateFont(context, weight = 700)
    }
    val fontMetricsFooter = footerPaint.fontMetrics
    val footerY = topY + cardSize - pad - (cardSize * 0.02f) - (fontMetricsFooter.ascent + fontMetricsFooter.descent) / 2f
    canvas.drawText(footerText, leftX + (cardSize / 2f), footerY, footerPaint)

    return bitmap
}

// 12. Pixel Heart Tile (Strict 1:1 Square)
fun generatePixelHeartBitmap(
    context: Context,
    data: DetailedBatteryData,
    config: SlateWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val margin = scaleFactor * 1.5f
    // Strictly locked to a centered 1:1 square
    val cardSize = minOf(w, h) - (margin * 2f)
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f
    val cardRect = RectF(leftX, topY, leftX + cardSize, topY + cardSize)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColor = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val dimColor = if (isLight) 0x1F000000 else 0x1AFFFFFF

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x12000000 else 0x1AFFFFFF
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.8f
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, borderPaint)

    val heartGrid = arrayOf(
        intArrayOf(0, 0, 1, 1, 1, 0, 0, 0, 1, 1, 1, 0, 0),
        intArrayOf(0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0),
        intArrayOf(0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0),
        intArrayOf(0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0)
    )

    val rows = heartGrid.size
    val cols = heartGrid[0].size

    var totalHeartPixels = 0
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if (heartGrid[r][c] == 1) totalHeartPixels++
        }
    }

    val activePixelsCount = ((data.percentage.coerceIn(0, 100) / 100f) * totalHeartPixels).toInt()

    val pad = cardSize * 0.12f
    val availW = cardSize - (pad * 2f)
    val availH = cardSize - (pad * 2f)

    val cellSize = minOf(availW / cols, availH / rows)
    val dotSize = cellSize * 0.84f
    val gap = (cellSize - dotSize) / 2f
    val dotCornerRadius = dotSize * 0.28f

    val gridW = cols * cellSize
    val gridH = rows * cellSize

    val offsetX = leftX + (cardSize - gridW) / 2f
    val offsetY = topY + (cardSize - gridH) / 2f

    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }

    val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dimColor
        style = Paint.Style.FILL
    }

    var currentPixelIndex = 0

    for (r in rows - 1 downTo 0) {
        for (c in 0 until cols) {
            if (heartGrid[r][c] == 1) {
                val pixelLeft = offsetX + c * cellSize + gap
                val pixelTop = offsetY + r * cellSize + gap
                val pixelRight = pixelLeft + dotSize
                val pixelBottom = pixelTop + dotSize

                val rect = RectF(pixelLeft, pixelTop, pixelRight, pixelBottom)
                val paint = if (currentPixelIndex < activePixelsCount) activePaint else dimPaint

                canvas.drawRoundRect(rect, dotCornerRadius, dotCornerRadius, paint)
                currentPixelIndex++
            }
        }
    }

    return bitmap
}

// 13. Lightning Bolt Tile (2x2 / 4x2)
fun generateWavyLightningBoltBitmap(
    context: Context,
    data: DetailedBatteryData,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    isWide: Boolean = false
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val accentColor = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val dimColor = if (isLight) 0x1F000000 else 0x2BFFFFFF

    val margin = scaleFactor * 1.5f
    val targetRatio = if (isWide) 2.0f else 1.0f

    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
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
    val leftX = cardRect.left
    val topY = cardRect.top

    val isWideLayout = isWide || (cardW / cardH >= 1.4f)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    canvas.save()
    val cardClipPath = Path().apply {
        addRoundRect(cardRect, cardCornerRadius, cardCornerRadius, Path.Direction.CW)
    }
    canvas.clipPath(cardClipPath)

    val scaleY = if (isWideLayout) {
        (cardH * 0.85f) / 372f
    } else {
        minOf((cardW * 0.85f) / 290f, (cardH * 0.85f) / 372f)
    }

    val scaleX = scaleY * 1.15f

    val centerX = if (isWideLayout) cardRect.right - (cardH / 2f) else cardRect.centerX()
    val centerY = cardRect.centerY()

    val boltPath = Path().apply {
        moveTo(centerX - (48f * scaleX), centerY - (186f * scaleY))
        lineTo(centerX + (115f * scaleX), centerY - (186f * scaleY))
        lineTo(centerX - (12f * scaleX), centerY - (24f * scaleY))
        lineTo(centerX + (145f * scaleX), centerY - (24f * scaleY))
        lineTo(centerX - (125f * scaleX), centerY + (186f * scaleY))
        lineTo(centerX - (42f * scaleX), centerY + (12f * scaleY))
        lineTo(centerX - (145f * scaleX), centerY + (12f * scaleY))
        close()
    }

    val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dimColor
        style = Paint.Style.FILL
    }

    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }

    canvas.drawPath(boltPath, dimPaint)

    val fillProgress = data.percentage.coerceIn(0, 100) / 100f
    val minFillY = centerY + (186f * scaleY)
    val maxFillY = centerY - (186f * scaleY)
    val fillY = minFillY - ((minFillY - maxFillY) * fillProgress)

    if (fillProgress > 0f) {
        val wavePath = Path().apply {
            val waveAmplitude = 10f * scaleY
            val waveLength = 250f * scaleX

            moveTo(cardRect.left - (100f * scaleX), fillY)

            var x = cardRect.left - (100f * scaleX)
            var isUp = true
            while (x < cardRect.right + (100f * scaleX)) {
                val nextX = x + (waveLength / 2f)
                val midX = x + ((nextX - x) / 2f)
                val controlY = if (isUp) fillY - waveAmplitude else fillY + waveAmplitude

                quadTo(midX, controlY, nextX, fillY)
                x = nextX
                isUp = !isUp
            }

            lineTo(cardRect.right + (100f * scaleX), cardRect.bottom + (100f * scaleY))
            lineTo(cardRect.left - (100f * scaleX), cardRect.bottom + (100f * scaleY))
            close()
        }

        canvas.save()
        canvas.clipPath(wavePath)
        canvas.drawPath(boltPath, activePaint)
        canvas.restore()
    }

    if (isWideLayout) {
        val padX = leftX + (cardH * 0.12f)
        val padY = topY + (cardH * 0.12f)

        val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = cardH * 0.15f
            typeface = getSlateFont(context, weight = 700)
        }
        val fontMetricsPct = pctPaint.fontMetrics
        val pctY = padY + (cardH * 0.10f) - (fontMetricsPct.ascent + fontMetricsPct.descent) / 2f
        canvas.drawText("${data.percentage}% / ${data.tempText}", padX, pctY, pctPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryTextColor
            textSize = cardH * 0.08f
            typeface = getSlateFont(context, weight = 600)
        }
        val fontMetricsSub = subPaint.fontMetrics
        val subY = padY + (cardH * 0.24f) - (fontMetricsSub.ascent + fontMetricsSub.descent) / 2f
        val subText = if (data.isCharging) "Charging • ${data.voltageText}" else "Discharging • ${data.voltageText}"
        canvas.drawText(subText, padX, subY, subPaint)

        val botPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = cardH * 0.08f
            typeface = getSlateFont(context, weight = 700)
        }
        val fontMetricsBot = botPaint.fontMetrics
        val botY = topY + cardH - (cardH * 0.12f) - (fontMetricsBot.ascent + fontMetricsBot.descent) / 2f
        val botText = if (data.isCharging) "Fast Charging Active" else "Battery Normal"
        canvas.drawText(botText, padX, botY, botPaint)
    }

    canvas.restore()
    return bitmap
}

// 14. Circular Dial
fun generateCircularGaugeBitmap(
    context: Context,
    percentage: Int,
    isCharging: Boolean,
    config: SlateWidgetConfig,
    widthPx: Int,
    heightPx: Int,
    isResponsive: Boolean = false
): Bitmap {
    val scaleFactor = maxOf(context.resources.displayMetrics.density, 3.5f)
    val w = widthPx.toFloat()
    val h = heightPx.toFloat()
    val cardSize = minOf(w, h)
    val cx = w / 2f
    val cy = h / 2f

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColor = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val dimColor = if (isLight) 0x1F000000 else 0x2BFFFFFF
    val iconColor = if (isLight) Color.BLACK else Color.WHITE

    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, cardSize / 2f, bgPaint)

    val dynamicScale = (cardSize / 300f).coerceAtLeast(0.5f)
    val ringStrokeWidth = cardSize * 0.060f
    val margin = cardSize * 0.05f
    val arcRadius = (cardSize / 2f) - margin - (ringStrokeWidth / 2f)

    val arcRect = RectF(
        cx - arcRadius,
        cy - arcRadius,
        cx + arcRadius,
        cy + arcRadius
    )

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dimColor
        style = Paint.Style.STROKE
        strokeWidth = ringStrokeWidth
        strokeCap = Paint.Cap.ROUND
    }

    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.STROKE
        strokeWidth = ringStrokeWidth
        strokeCap = Paint.Cap.ROUND
    }

    canvas.drawArc(arcRect, 0f, 360f, false, trackPaint)

    val fillProgress = percentage.coerceIn(0, 100) / 100f
    val sweepAngle = fillProgress * 360f

    if (sweepAngle > 0f) {
        canvas.drawArc(arcRect, -90f, sweepAngle, false, activePaint)
    }

    val tickColor = if (isLight) 0x20000000 else 0x28FFFFFF
    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tickColor
        strokeWidth = 2f * dynamicScale
        style = Paint.Style.STROKE
    }
    val tickInnerR = arcRadius - (ringStrokeWidth / 2f) - (6f * dynamicScale)
    val tickOuterR = tickInnerR - (10f * dynamicScale)

    for (i in 0 until 60) {
        val angleDeg = i * 6f
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val startX = cx + (tickInnerR * Math.cos(angleRad)).toFloat()
        val startY = cy + (tickInnerR * Math.sin(angleRad)).toFloat()
        val endX = cx + (tickOuterR * Math.cos(angleRad)).toFloat()
        val endY = cy + (tickOuterR * Math.sin(angleRad)).toFloat()
        canvas.drawLine(startX, startY, endX, endY, tickPaint)
    }

    val iconY = cy - (cardSize * 0.125f)
    val batW = cardSize * 0.085f
    val batH = cardSize * 0.135f

    val shellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = iconColor
        style = Paint.Style.STROKE
        strokeWidth = (2.5f * dynamicScale).coerceAtLeast(2f)
    }

    val fillColor = if (percentage <= 20 && !isCharging) 0xFFFF3B30.toInt() else accentColor

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }

    val bodyRect = RectF(
        cx - (batW / 2f),
        iconY - (batH / 2f) + (2f * dynamicScale),
        cx + (batW / 2f),
        iconY + (batH / 2f)
    )
    val capRect = RectF(
        cx - (batW * 0.22f),
        iconY - (batH / 2f) - (2f * dynamicScale),
        cx + (batW * 0.22f),
        iconY - (batH / 2f) + (2f * dynamicScale)
    )

    canvas.drawRoundRect(capRect, 2f * dynamicScale, 2f * dynamicScale, fillPaint)
    canvas.drawRoundRect(bodyRect, 4f * dynamicScale, 4f * dynamicScale, shellPaint)

    val innerMargin = 2.5f * dynamicScale
    val maxFillH = batH - (innerMargin * 2f)
    val currentFillH = (maxFillH * fillProgress).coerceAtLeast(2f * dynamicScale)

    val fillRect = RectF(
        bodyRect.left + innerMargin,
        bodyRect.bottom - innerMargin - currentFillH,
        bodyRect.right - innerMargin,
        bodyRect.bottom - innerMargin
    )
    canvas.drawRoundRect(fillRect, 2f * dynamicScale, 2f * dynamicScale, fillPaint)

    if (isCharging) {
        val boltColor = if (isLight) Color.BLACK else Color.WHITE
        val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = boltColor
            style = Paint.Style.FILL
        }

        val boltPath = Path().apply {
            moveTo(cx - (1.5f * dynamicScale), iconY - (5f * dynamicScale))
            lineTo(cx + (3.5f * dynamicScale), iconY - (5f * dynamicScale))
            lineTo(cx - (0.5f * dynamicScale), iconY)
            lineTo(cx + (2.5f * dynamicScale), iconY)
            lineTo(cx - (2.5f * dynamicScale), iconY + (5f * dynamicScale))
            lineTo(cx - (0.5f * dynamicScale), iconY + (1f * dynamicScale))
            lineTo(cx - (2.5f * dynamicScale), iconY + (1f * dynamicScale))
            close()
        }
        canvas.drawPath(boltPath, boltPaint)
    }

    val primaryTextColor = if (isLight) Color.BLACK else Color.WHITE
    val secondaryTextColor = if (isLight) 0x99000000.toInt() else 0x99FFFFFF.toInt()

    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardSize * 0.155f
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }
    val fontMetricsPct = pctPaint.fontMetrics
    val textY = cy + (cardSize * 0.095f) - (fontMetricsPct.ascent + fontMetricsPct.descent) / 2f
    canvas.drawText("${percentage}%", cx, textY, pctPaint)

    val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = cardSize * 0.048f
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.06f
    }
    val fontMetricsStatus = statusPaint.fontMetrics
    val statusText = if (isCharging) "CHARGING" else "DISCHARGING"
    val statusY = textY + (cardSize * 0.075f) - (fontMetricsStatus.ascent + fontMetricsStatus.descent) / 2f
    canvas.drawText(statusText, cx, statusY, statusPaint)

    return bitmap
}

// 15. Vertical Pill
fun generateVerticalPillBitmap(
    context: Context,
    percentage: Int,
    isCharging: Boolean,
    config: SlateWidgetConfig,
    widthPx: Int,
    heightPx: Int,
    isResponsive: Boolean = false
): Bitmap {
    val scaleFactor = maxOf(context.resources.displayMetrics.density, 3.5f)
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = widthPx.toFloat()
    val h = heightPx.toFloat()

    val padding = minOf(w, h) * 0.05f
    val availW = w - (padding * 2f)
    val availH = h - (padding * 2f)

    val targetRatio = 2.2f
    var bodyW = availW
    var bodyH = bodyW * targetRatio

    if (bodyH > availH * 0.90f) {
        bodyH = availH * 0.90f
        bodyW = bodyH / targetRatio
    }

    val capH = bodyH * 0.045f
    val capW = bodyW * 0.40f
    val strokeW = (bodyW * 0.045f).coerceIn(6f, 12f)

    val totalH = bodyH + capH
    val startY = (h - totalH) / 2f
    val centerX = w / 2f

    val capRect = RectF(
        centerX - (capW / 2f),
        startY,
        centerX + (capW / 2f),
        startY + capH + strokeW
    )

    val bodyRect = RectF(
        centerX - (bodyW / 2f) + (strokeW / 2f),
        startY + capH,
        centerX + (bodyW / 2f) - (strokeW / 2f),
        startY + totalH - (strokeW / 2f)
    )

    val bodyRadius = bodyW * 0.16f
    val capRadius = 8f

    val isLight = config.themeMode == "LIGHT"
    val accentColor = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val isLowBattery = percentage <= 20 && !isCharging

    val shellBgColor = if (isLight) 0xFFFFFFFF.toInt() else 0xFF141416.toInt()
    val strokeColor = if (isLight) 0xFFD1D1D6.toInt() else 0xFF2C2C2E.toInt()
    val activeColor = if (isLowBattery) 0xFFFF3B30.toInt() else accentColor
    val dimColor = if (isLight) 0x14000000 else 0x1AFFFFFF
    val capColor = if (isCharging) activeColor else strokeColor

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = strokeW
    }

    val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = capColor
        style = Paint.Style.FILL
    }

    val shellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = shellBgColor
        style = Paint.Style.FILL
    }

    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = activeColor
        style = Paint.Style.FILL
    }

    val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dimColor
        style = Paint.Style.FILL
    }

    canvas.drawRoundRect(capRect, capRadius, capRadius, capPaint)
    canvas.drawRoundRect(bodyRect, bodyRadius, bodyRadius, shellPaint)
    canvas.drawRoundRect(bodyRect, bodyRadius, bodyRadius, strokePaint)

    val innerMargin = strokeW + (bodyW * 0.035f)
    val innerRect = RectF(
        bodyRect.left + innerMargin,
        bodyRect.top + innerMargin,
        bodyRect.right - innerMargin,
        bodyRect.bottom - innerMargin
    )
    val innerRadius = (bodyRadius - innerMargin).coerceAtLeast(8f)

    val innerClipPath = Path().apply {
        addRoundRect(innerRect, innerRadius, innerRadius, Path.Direction.CW)
    }

    canvas.save()
    canvas.clipPath(innerClipPath)

    val totalSegments = 5
    val gap = innerRect.height() * 0.03f
    val segmentH = (innerRect.height() - (gap * (totalSegments - 1))) / totalSegments
    val segmentRadius = (segmentH * 0.18f).coerceAtLeast(8f)

    val activeSegmentsCount = (percentage.coerceIn(0, 100) / 100f * totalSegments).toInt()

    for (i in 0 until totalSegments) {
        val segTop = innerRect.bottom - ((i + 1) * segmentH) - (i * gap)
        val segBottom = segTop + segmentH
        val segLeft = innerRect.left
        val segRight = innerRect.right

        val segRect = RectF(segLeft, segTop, segRight, segBottom)
        val paint = if (i < activeSegmentsCount) activePaint else dimPaint

        canvas.drawRoundRect(segRect, segmentRadius, segmentRadius, paint)
    }

    canvas.restore()

    return bitmap
}

// 16. Horizontal Pill
fun generateHorizontalPillBitmap(
    context: Context,
    percentage: Int,
    isCharging: Boolean,
    config: SlateWidgetConfig,
    widthPx: Int,
    heightPx: Int,
    isResponsive: Boolean = false
): Bitmap {
    val scaleFactor = maxOf(context.resources.displayMetrics.density, 3.5f)
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = widthPx.toFloat()
    val h = heightPx.toFloat()

    val padding = minOf(w, h) * 0.05f
    val availW = w - (padding * 2f)
    val availH = h - (padding * 2f)

    val targetRatio = 1.85f
    var bodyH = availH
    var bodyW = bodyH * targetRatio

    if (bodyW > availW * 0.90f) {
        bodyW = availW * 0.90f
        bodyH = bodyW / targetRatio
    }

    val capW = bodyW * 0.055f
    val capH = bodyH * 0.40f
    val strokeW = (bodyH * 0.045f).coerceIn(6f, 12f)

    val totalW = bodyW + capW
    val startX = (w - totalW) / 2f
    val centerY = h / 2f

    val bodyRect = RectF(
        startX + (strokeW / 2f),
        centerY - (bodyH / 2f),
        startX + bodyW - (strokeW / 2f),
        centerY + (bodyH / 2f)
    )

    val capRect = RectF(
        bodyRect.right - strokeW,
        centerY - (capH / 2f),
        startX + totalW,
        centerY + (capH / 2f)
    )

    val bodyRadius = bodyH * 0.16f
    val capRadius = 8f

    val isLight = config.themeMode == "LIGHT"
    val accentColor = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val isLowBattery = percentage <= 20 && !isCharging

    val shellBgColor = if (isLight) 0xFFFFFFFF.toInt() else 0xFF141416.toInt()
    val strokeColor = if (isLight) 0xFFD1D1D6.toInt() else 0xFF2C2C2E.toInt()
    val activeColor = if (isLowBattery) 0xFFFF3B30.toInt() else accentColor
    val dimColor = if (isLight) 0x14000000 else 0x1AFFFFFF
    val capColor = if (isCharging) activeColor else strokeColor

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = strokeW
    }

    val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = capColor
        style = Paint.Style.FILL
    }

    val shellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = shellBgColor
        style = Paint.Style.FILL
    }

    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = activeColor
        style = Paint.Style.FILL
    }

    val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dimColor
        style = Paint.Style.FILL
    }

    canvas.drawRoundRect(capRect, capRadius, capRadius, capPaint)
    canvas.drawRoundRect(bodyRect, bodyRadius, bodyRadius, shellPaint)
    canvas.drawRoundRect(bodyRect, bodyRadius, bodyRadius, strokePaint)

    val innerMargin = strokeW + (bodyH * 0.035f)
    val innerRect = RectF(
        bodyRect.left + innerMargin,
        bodyRect.top + innerMargin,
        bodyRect.right - innerMargin,
        bodyRect.bottom - innerMargin
    )
    val innerRadius = (bodyRadius - innerMargin).coerceAtLeast(8f)

    val innerClipPath = Path().apply {
        addRoundRect(innerRect, innerRadius, innerRadius, Path.Direction.CW)
    }

    canvas.save()
    canvas.clipPath(innerClipPath)

    val totalSegments = 5
    val gap = innerRect.width() * 0.03f
    val segmentW = (innerRect.width() - (gap * (totalSegments - 1))) / totalSegments
    val segmentRadius = (segmentW * 0.18f).coerceAtLeast(8f)

    val activeSegmentsCount = (percentage.coerceIn(0, 100) * totalSegments) / 100

    for (i in 0 until totalSegments) {
        val segLeft = innerRect.left + (i * segmentW) + (i * gap)
        val segRight = segLeft + segmentW
        val segTop = innerRect.top
        val segBottom = innerRect.bottom

        val segRect = RectF(segLeft, segTop, segRight, segBottom)
        val paint = if (i < activeSegmentsCount) activePaint else dimPaint

        canvas.drawRoundRect(segRect, segmentRadius, segmentRadius, paint)
    }

    canvas.restore()

    return bitmap
}

// Helper Arc Gauge Renderer
fun generateArcGaugeBitmap(
    percentage: Int,
    accentColorInt: Int,
    trackColorInt: Int,
    widthPx: Int,
    heightPx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val strokeWidth = widthPx * 0.18f
    val padding = strokeWidth / 2f + 4f
    val rectF = RectF(padding, padding, widthPx - padding, heightPx * 2f - padding)

    val startAngle = 210f
    val maxSweep = 120f
    val currentSweep = (percentage.coerceIn(0, 100) / 100f) * maxSweep

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        setStrokeWidth(strokeWidth)
        strokeCap = Paint.Cap.BUTT
        color = trackColorInt
    }

    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        setStrokeWidth(strokeWidth)
        strokeCap = Paint.Cap.BUTT
        color = accentColorInt
    }

    canvas.drawArc(rectF, startAngle, maxSweep, false, trackPaint)

    if (currentSweep > 0) {
        canvas.drawArc(rectF, startAngle, currentSweep, false, activePaint)
    }

    return bitmap
}

// Helper Segmented Bar Renderer
fun generateSegmentedBarBitmap(
    percentage: Int,
    accentColorInt: Int,
    trackColorInt: Int,
    widthPx: Int,
    heightPx: Int,
    totalSegments: Int = 20
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val activeSegments = (percentage.coerceIn(0, 100) / 100f * totalSegments).toInt()
    val segmentWidth = widthPx.toFloat() / totalSegments
    val barWidth = segmentWidth * 0.58f

    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = trackColorInt
        style = Paint.Style.FILL
    }

    for (i in 0 until totalSegments) {
        val left = i * segmentWidth
        val right = left + barWidth
        val paint = if (i < activeSegments) activePaint else trackPaint

        val rect = RectF(left, 0f, right, heightPx.toFloat())
        canvas.drawRoundRect(rect, 8f, 8f, paint)
    }

    return bitmap
}