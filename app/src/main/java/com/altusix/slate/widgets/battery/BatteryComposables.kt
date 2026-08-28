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
import kotlin.math.roundToInt

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
    canvas.drawRoundRect(RectF(leftX, topY, leftX + cardSize, topY + cardSize), cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = cardSize * 0.07f

    // Header Percentage Text
    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardSize * 0.18f
        typeface = getSlateFont(context, weight = 600)
    }
    val fontMetricsPct = pctPaint.fontMetrics
    val headerCenterY = topY + pad + (cardSize * 0.08f)
    val pctY = headerCenterY - (fontMetricsPct.ascent + fontMetricsPct.descent) / 2f
    canvas.drawText("${data.percentage}%", leftX + pad, pctY, pctPaint)

    // Header Charging Status
    if (data.isCharging) {
        val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = activeColor
            textSize = cardSize * 0.07f
            textAlign = Paint.Align.RIGHT
            typeface = getSlateFont(context, weight = 700)
        }
        val fontMetricsStatus = statusPaint.fontMetrics
        val statusY = headerCenterY - (fontMetricsStatus.ascent + fontMetricsStatus.descent) / 2f

        val rightX = leftX + cardSize - pad
        val textW = statusPaint.measureText("CHARGING")
        val iconSize = cardSize * 0.07f
        val gap = cardSize * 0.015f
        val iconLeft = rightX - textW - gap - iconSize
        val iconTop = headerCenterY - (iconSize / 2f)

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

// 2. Dot Level Tile Pure / Textless (1:1 Square)
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
    canvas.drawRoundRect(RectF(leftX, topY, leftX + cardSize, topY + cardSize), cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = cardSize * 0.10f
    val columns = 10
    val rows = 10
    val gridW = cardSize - (pad * 2f)
    val gridH = cardSize - (pad * 2f)

    val cellW = gridW / columns
    val cellH = gridH / rows
    val dotRadius = minOf(cellW, cellH) * 0.32f

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

// 3. Minimal Linear (1:1 Square)
fun generateBatteryMinimalLinearBitmap(
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

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val trackColor = if (isLight) 0x1F000000 else 0x1FAFAFAF
    val accentColor = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    canvas.drawRoundRect(RectF(leftX, topY, leftX + cardSize, topY + cardSize), cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = cardSize * 0.12f

    val headerCenterY = topY + pad + (cardSize * 0.04f)
    val statusColor = if (data.isCharging) accentColor else secondaryTextColor
    val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = statusColor
        textSize = cardSize * 0.08f
        typeface = getSlateFont(context, weight = 700)
    }
    val fontMetricsStatus = statusPaint.fontMetrics
    val statusY = headerCenterY - (fontMetricsStatus.ascent + fontMetricsStatus.descent) / 2f

    if (data.isCharging) {
        val iconSize = cardSize * 0.075f
        val gap = cardSize * 0.015f
        val iconLeft = leftX + pad
        val iconTop = headerCenterY - (iconSize / 2f)
        drawBoltIcon(context, canvas, iconLeft, iconTop, iconSize, accentColor)
        canvas.drawText("CHARGING", iconLeft + iconSize + gap, statusY, statusPaint)
    } else {
        canvas.drawText("BATTERY", leftX + pad, statusY, statusPaint)
    }

    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardSize * 0.32f
        typeface = getSlateFont(context, weight = 700)
    }
    val fontMetricsPct = pctPaint.fontMetrics
    val textY = topY + (cardSize * 0.50f) - (fontMetricsPct.ascent + fontMetricsPct.descent) / 2f
    canvas.drawText("${data.percentage}%", leftX + pad, textY, pctPaint)

    val barHeight = cardSize * 0.05f
    val barBottom = topY + cardSize - pad
    val barTop = barBottom - barHeight
    val barWidth = cardSize - (pad * 2f)

    val trackRect = RectF(leftX + pad, barTop, leftX + pad + barWidth, barBottom)
    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = trackColor }
    canvas.drawRoundRect(trackRect, barHeight / 2f, barHeight / 2f, trackPaint)

    val fillWidth = barWidth * (data.percentage.coerceIn(0, 100) / 100f)
    if (fillWidth > 0f) {
        val fillRect = RectF(leftX + pad, barTop, leftX + pad + fillWidth, barBottom)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
        canvas.drawRoundRect(fillRect, barHeight / 2f, barHeight / 2f, fillPaint)
    }

    return bitmap
}

// 4. Minimal Ring (1:1 Square)
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
    val cardSize = minOf(w, h) - (margin * 2f)
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f

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
    canvas.drawRoundRect(RectF(leftX, topY, leftX + cardSize, topY + cardSize), cardCornerRadius, cardCornerRadius, bgPaint)

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

// 5. Arc Battery (1:1 Square)
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
    val cardSize = minOf(w, h) - (margin * 2f)
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f

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
    canvas.drawRoundRect(RectF(leftX, topY, leftX + cardSize, topY + cardSize), cardCornerRadius, cardCornerRadius, bgPaint)

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

// 6. Editorial Stats (1:1 Square)
fun generateEditorialStatsBitmap(
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

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val trackColorInt = if (isLight) 0x1F000000 else 0x2EFFFFFF

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    canvas.drawRoundRect(RectF(leftX, topY, leftX + cardSize, topY + cardSize), cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = cardSize * 0.12f

    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardSize * 0.30f
        typeface = getSlateFont(context, weight = 700)
    }
    val fontMetricsPct = pctPaint.fontMetrics
    val pctY = topY + pad + (cardSize * 0.14f) - (fontMetricsPct.ascent + fontMetricsPct.descent) / 2f
    canvas.drawText("${data.percentage}%", leftX + pad, pctY, pctPaint)

    val statPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = cardSize * 0.085f
        typeface = getSlateFont(context, weight = 600)
    }
    val fontMetricsStat = statPaint.fontMetrics
    val statY1 = topY + pad + (cardSize * 0.44f) - (fontMetricsStat.ascent + fontMetricsStat.descent) / 2f
    val statY2 = topY + pad + (cardSize * 0.56f) - (fontMetricsStat.ascent + fontMetricsStat.descent) / 2f

    canvas.drawText("• ${data.healthText}", leftX + pad, statY1, statPaint)
    canvas.drawText("• ${data.secondaryStatText}", leftX + pad, statY2, statPaint)

    val barH = (cardSize * 0.10f).toInt()
    val barW = (cardSize - (pad * 2f)).toInt()
    val barBitmap = generateSegmentedBarBitmap(data.percentage, accentColorInt, trackColorInt, barW, barH)
    canvas.drawBitmap(barBitmap, leftX + pad, topY + cardSize - pad - barH, null)

    return bitmap
}

// 7. MULTI-DEVICE STATS BENTO (4x2 / Adaptive)
fun generateMultiDeviceBatteryBitmap(context: Context, data: DetailedBatteryData, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val accentColor = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val trackColor = if (isLight) 0x1F000000 else 0x1FAFAFAF
    val tileBgColor = if (isLight) 0x0A000000 else 0x18FFFFFF

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
    val aspectRatio = cardW / cardH
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = minOf(cardW, cardH) * 0.08f
    val gap = minOf(cardW, cardH) * 0.06f
    val concentricRadius = (cardCornerRadius - pad).coerceAtLeast(scaleFactor * 6f)
    val sq = (scaleFactor * 8f).coerceAtMost(minOf(cardW, cardH) * 0.22f)
    val tileBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tileBgColor }
    val trackPaintObj = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = trackColor }

    fun drawTile(rect: RectF, radii: FloatArray) {
        val path = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }
        canvas.drawPath(path, tileBgPaint)
    }

    val heroRect: RectF
    val tempRect: RectF
    val voltRect: RectF
    val heroRadii: FloatArray
    val tempRadii: FloatArray
    val voltRadii: FloatArray

    if (aspectRatio >= 1.1f) {
        val availW = cardW - (pad * 2f)
        val availH = cardH - (pad * 2f)
        val heroW = availW * 0.40f
        val heroH = availH

        heroRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.left + pad + heroW, cardRect.top + pad + heroH)
        heroRadii = floatArrayOf(concentricRadius, concentricRadius, sq, sq, sq, sq, concentricRadius, concentricRadius)

        val rightX = cardRect.left + pad + heroW + gap
        val rightW = availW - heroW - gap
        val statH = (availH - gap) / 2f

        tempRect = RectF(rightX, cardRect.top + pad, rightX + rightW, cardRect.top + pad + statH)
        tempRadii = floatArrayOf(sq, sq, concentricRadius, concentricRadius, sq, sq, sq, sq)

        voltRect = RectF(rightX, cardRect.top + pad + statH + gap, rightX + rightW, cardRect.top + pad + availH)
        voltRadii = floatArrayOf(sq, sq, sq, sq, concentricRadius, concentricRadius, sq, sq)
    } else {
        val availW = cardW - (pad * 2f)
        val availH = cardH - (pad * 2f)
        val heroH = availH * 0.48f
        val heroW = availW

        heroRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.left + pad + heroW, cardRect.top + pad + heroH)
        heroRadii = floatArrayOf(concentricRadius, concentricRadius, concentricRadius, concentricRadius, sq, sq, sq, sq)

        val botY = cardRect.top + pad + heroH + gap
        val botH = availH - heroH - gap
        val statW = (availW - gap) / 2f

        tempRect = RectF(cardRect.left + pad, botY, cardRect.left + pad + statW, botY + botH)
        tempRadii = floatArrayOf(sq, sq, sq, sq, sq, sq, concentricRadius, concentricRadius)

        voltRect = RectF(cardRect.left + pad + statW + gap, botY, cardRect.right - pad, botY + botH)
        voltRadii = floatArrayOf(sq, sq, sq, sq, concentricRadius, concentricRadius, sq, sq)
    }

    // RENDER HERO TILE
    drawTile(heroRect, heroRadii)
    val heroH = heroRect.height()
    val heroW = heroRect.width()
    val heroPad = minOf(heroW, heroH) * 0.12f

    val heroTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = (heroH * 0.09f).coerceAtLeast(scaleFactor * 7f)
        typeface = getSlateFont(context, weight = 700)
    }
    val fontMetricsHeroTag = heroTagPaint.fontMetrics
    val heroTagY = heroRect.top + heroPad + (heroH * 0.04f) - (fontMetricsHeroTag.ascent + fontMetricsHeroTag.descent) / 2f
    canvas.drawText("PHONE", heroRect.left + heroPad, heroTagY, heroTagPaint)

    val statusColor = if (data.isCharging) accentColor else secondaryTextColor
    val heroStatusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = statusColor
        textSize = (heroH * 0.075f).coerceAtLeast(scaleFactor * 6.5f)
        typeface = getSlateFont(context, weight = 700)
    }
    val fontMetricsHeroStatus = heroStatusPaint.fontMetrics
    val statusCenterY = heroRect.top + heroPad + (heroH * 0.17f)
    val heroStatusY = statusCenterY - (fontMetricsHeroStatus.ascent + fontMetricsHeroStatus.descent) / 2f

    if (data.isCharging) {
        val iconSize = heroH * 0.075f
        val iconGap = heroH * 0.015f
        val iconLeft = heroRect.left + heroPad
        val iconTop = statusCenterY - (iconSize / 2f)
        drawBoltIcon(context, canvas, iconLeft, iconTop, iconSize, accentColor)
        canvas.drawText("CHARGING", iconLeft + iconSize + iconGap, heroStatusY, heroStatusPaint)
    } else {
        canvas.drawText("BATTERY", heroRect.left + heroPad, heroStatusY, heroStatusPaint)
    }

    val pctText = "${data.percentage}%"
    var heroPctTextSize = heroH * 0.32f
    val heroPctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = heroPctTextSize
        typeface = getSlateFont(context, weight = 700)
    }

    val maxHeroPctW = heroW - (heroPad * 2f)
    while (heroPctPaint.measureText(pctText) > maxHeroPctW && heroPctTextSize > scaleFactor * 8f) {
        heroPctTextSize -= scaleFactor * 0.5f
        heroPctPaint.textSize = heroPctTextSize
    }

    val fontMetricsHeroPct = heroPctPaint.fontMetrics
    val heroPctY = heroRect.top + (heroH * 0.50f) - (fontMetricsHeroPct.ascent + fontMetricsHeroPct.descent) / 2f
    canvas.drawText(pctText, heroRect.left + heroPad, heroPctY, heroPctPaint)

    val barH = heroH * 0.07f
    val barTop = heroRect.bottom - heroPad - barH
    val heroTrackRect = RectF(heroRect.left + heroPad, barTop, heroRect.right - heroPad, barTop + barH)
    canvas.drawRoundRect(heroTrackRect, barH / 2f, barH / 2f, trackPaintObj)

    val fillW = (heroTrackRect.width()) * (data.percentage.coerceIn(0, 100) / 100f)
    if (fillW > 0f) {
        val heroFillRect = RectF(heroTrackRect.left, barTop, heroTrackRect.left + fillW, barTop + barH)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
        canvas.drawRoundRect(heroFillRect, barH / 2f, barH / 2f, fillPaint)
    }

    // STAT CARD RENDERER (BALANCED LABEL & VALUE SIZE + VALUE ABOVE PROGRESS BAR)
    fun renderStatCard(rect: RectF, radii: FloatArray, fullLabel: String, shortLabel: String, valText: String, fillRatio: Float) {
        drawTile(rect, radii)
        val cardTileH = rect.height()
        val cardTileW = rect.width()

        val statPadX = cardTileW * 0.10f
        val statPadY = cardTileH * 0.12f
        val availTileW = cardTileW - (statPadX * 2f)

        // Progress bar at the bottom
        val statBarH = (cardTileH * 0.10f).coerceAtLeast(scaleFactor * 3.5f)
        val statBarTop = rect.bottom - statPadY - statBarH

        // Label paint - Increased size to balance prominently with the value
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryTextColor
            textSize = (cardTileH * 0.22f).coerceIn(scaleFactor * 8f, scaleFactor * 16f)
            typeface = getSlateFont(context, weight = 700)
        }

        // Value paint - Prominent bold value text
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = (cardTileH * 0.24f).coerceIn(scaleFactor * 9f, scaleFactor * 18f)
            typeface = getSlateFont(context, weight = 700)
        }

        val valW = valuePaint.measureText(valText)
        val minGap = cardTileH * 0.08f

        var displayLabel = fullLabel
        if (labelPaint.measureText(displayLabel) + valW + minGap > availTileW) {
            displayLabel = shortLabel
        }

        val fitsSideBySide = (labelPaint.measureText(displayLabel) + valW + minGap <= availTileW)

        if (fitsSideBySide) {
            // SIDE-BY-SIDE: Both label and value aligned at top with balanced sizes
            valuePaint.textAlign = Paint.Align.RIGHT
            val fontMetricsLabel = labelPaint.fontMetrics
            val fontMetricsVal = valuePaint.fontMetrics

            val labelY = rect.top + statPadY + (cardTileH * 0.10f) - (fontMetricsLabel.ascent + fontMetricsLabel.descent) / 2f
            val valY = rect.top + statPadY + (cardTileH * 0.10f) - (fontMetricsVal.ascent + fontMetricsVal.descent) / 2f

            canvas.drawText(displayLabel, rect.left + statPadX, labelY, labelPaint)
            canvas.drawText(valText, rect.right - statPadX, valY, valuePaint)
        } else {
            // STACKED: Label at top, Value placed directly above the progress bar
            labelPaint.textAlign = Paint.Align.LEFT
            valuePaint.textAlign = Paint.Align.RIGHT

            while (labelPaint.measureText(displayLabel) > availTileW && labelPaint.textSize > scaleFactor * 6f) {
                labelPaint.textSize -= scaleFactor * 0.5f
            }
            while (valuePaint.measureText(valText) > availTileW && valuePaint.textSize > scaleFactor * 8f) {
                valuePaint.textSize -= scaleFactor * 0.5f
            }

            val fontMetricsLabel = labelPaint.fontMetrics
            val fontMetricsVal = valuePaint.fontMetrics

            val labelY = rect.top + statPadY + (cardTileH * 0.04f) - fontMetricsLabel.ascent
            canvas.drawText(displayLabel, rect.left + statPadX, labelY, labelPaint)

            val valY = statBarTop - (cardTileH * 0.06f) - fontMetricsVal.descent
            canvas.drawText(valText, rect.right - statPadX, valY, valuePaint)
        }

        val trackRect = RectF(rect.left + statPadX, statBarTop, rect.right - statPadX, statBarTop + statBarH)
        canvas.drawRoundRect(trackRect, statBarH / 2f, statBarH / 2f, trackPaintObj)

        val fillRect = RectF(trackRect.left, statBarTop, trackRect.left + (trackRect.width() * fillRatio), statBarTop + statBarH)
        val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
        canvas.drawRoundRect(fillRect, statBarH / 2f, statBarH / 2f, activePaint)
    }

    // RENDER TEMP & VOLT TILES
    val tempVal = data.tempText.replace("°C", "").toFloatOrNull() ?: 35f
    val tempRatio = (tempVal / 50f).coerceIn(0.1f, 1f)
    renderStatCard(tempRect, tempRadii, "TEMPERATURE", "TEMP", data.tempText, tempRatio)

    val voltVal = data.voltageText.replace("V", "").toFloatOrNull() ?: 3.8f
    val voltRatio = (voltVal / 4.4f).coerceIn(0.1f, 1f)
    renderStatCard(voltRect, voltRadii, "VOLTAGE", "VOLT", data.voltageText, voltRatio)

    return bitmap
}

// 8. Dot Matrix LED (4x2)
fun generateDotMatrixLEDBitmap(
    context: Context,
    text: String,
    activeColorInt: Int,
    dimColorInt: Int,
    bgColorInt: Int,
    targetWidthPx: Int,
    targetHeightPx: Int
): Bitmap {
    val scaleFactor = maxOf(context.resources.displayMetrics.density, 3.5f)
    val bitmap = Bitmap.createBitmap(targetWidthPx.coerceAtLeast(1), targetHeightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = targetWidthPx.toFloat()
    val h = targetHeightPx.toFloat()

    val cardW = w
    val maxCardH = cardW * 0.48f
    val cardH = minOf(h, maxCardH)

    val leftX = 0f
    val topY = (h - cardH) / 2f

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColorInt
        style = Paint.Style.FILL
    }
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    canvas.drawRoundRect(RectF(leftX, topY, leftX + cardW, topY + cardH), cardCornerRadius, cardCornerRadius, bgPaint)

    val rows = 9
    val glyphWidth = 5
    val glyphGap = 1
    val textWidthCols = text.length * glyphWidth + (text.length - 1) * glyphGap

    var cellSize = cardH / (rows + 2f)
    var columns = ((cardW / cellSize) - 2f).toInt()

    if (columns < textWidthCols) {
        columns = textWidthCols
        cellSize = cardW / (columns + 2f)
    }

    val dotRadius = cellSize * 0.38f
    val gridW = columns * cellSize
    val gridH = rows * cellSize

    val startX = leftX + (cardW - gridW) / 2f
    val startY = topY + (cardH - gridH) / 2f

    val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dimColorInt
        style = Paint.Style.FILL
    }

    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = activeColorInt
        style = Paint.Style.FILL
    }

    for (r in 0 until rows) {
        for (c in 0 until columns) {
            val cx = startX + c * cellSize + cellSize / 2f
            val cy = startY + r * cellSize + cellSize / 2f
            canvas.drawCircle(cx, cy, dotRadius, dimPaint)
        }
    }

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

    val glyphHeight = 7
    val startRow = (rows - glyphHeight) / 2
    var startCol = (columns - textWidthCols) / 2

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

// 9. Dot Level Wide (4x2)
fun generateCenteredLevelBitmap(
    context: Context,
    percentage: Int,
    activeColorInt: Int,
    dimColorInt: Int,
    bgColorInt: Int,
    targetWidthPx: Int,
    targetHeightPx: Int
): Bitmap {
    val scaleFactor = maxOf(context.resources.displayMetrics.density, 3.5f)
    val bitmap = Bitmap.createBitmap(targetWidthPx.coerceAtLeast(1), targetHeightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = targetWidthPx.toFloat()
    val h = targetHeightPx.toFloat()

    val cardW = w
    val maxCardH = cardW * 0.48f
    val cardH = minOf(h, maxCardH)

    val leftX = 0f
    val topY = (h - cardH) / 2f

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColorInt
        style = Paint.Style.FILL
    }
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    canvas.drawRoundRect(RectF(leftX, topY, leftX + cardW, topY + cardH), cardCornerRadius, cardCornerRadius, bgPaint)

    val rows = 5
    val cellSize = cardH / (rows + 2f)
    val columns = ((cardW / cellSize) - 2f).toInt().coerceAtLeast(5)

    val dotRadius = cellSize * 0.38f
    val gridW = columns * cellSize
    val gridH = rows * cellSize

    val startX = leftX + (cardW - gridW) / 2f
    val startY = topY + (cardH - gridH) / 2f

    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = activeColorInt
        style = Paint.Style.FILL
    }

    val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dimColorInt
        style = Paint.Style.FILL
    }

    val totalDots = columns * rows
    val activeDotsCount = (percentage.coerceIn(0, 100) * totalDots) / 100

    for (r in 0 until rows) {
        val rowFromBottom = (rows - 1) - r
        for (c in 0 until columns) {
            val dotIndex = rowFromBottom * columns + c
            val cx = startX + c * cellSize + cellSize / 2f
            val cy = startY + r * cellSize + cellSize / 2f

            val paint = if (dotIndex < activeDotsCount) activePaint else dimPaint
            canvas.drawCircle(cx, cy, dotRadius, paint)
        }
    }

    return bitmap
}

// 10. Sleek Minimal Battery Strip - Percentage Only (4x1)
fun generateHorizontalStripBitmap(
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
    val cardW = w - (margin * 2f)
    val maxCardH = cardW * 0.28f
    val cardH = minOf(h - (margin * 2f), maxCardH)

    val leftX = (w - cardW) / 2f
    val topY = (h - cardH) / 2f

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val activeColor = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val trackColor = if (isLight) 0x1F000000 else 0x1AFFFFFF

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    canvas.drawRoundRect(RectF(leftX, topY, leftX + cardW, topY + cardH), cardCornerRadius, cardCornerRadius, bgPaint)

    val padX = cardW * 0.06f

    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardH * 0.46f
        typeface = getSlateFont(context, weight = 700)
    }
    val pctText = "${data.percentage}%"
    val pctTextWidth = pctPaint.measureText(pctText)

    val fontMetricsPct = pctPaint.fontMetrics
    val textY = topY + (cardH / 2f) - (fontMetricsPct.ascent + fontMetricsPct.descent) / 2f

    val textStartX = leftX + padX
    canvas.drawText(pctText, textStartX, textY, pctPaint)

    val barStartX = textStartX + pctTextWidth + (cardW * 0.06f)
    val barEndX = leftX + cardW - padX
    val barWidth = barEndX - barStartX

    if (barWidth > 0) {
        val totalSegments = 10
        val gap = barWidth * 0.04f
        val segmentW = (barWidth - (gap * (totalSegments - 1))) / totalSegments
        val barH = cardH * 0.28f
        val barY = topY + (cardH - barH) / 2f
        val segmentRadius = barH / 2f

        val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = activeColor
            style = Paint.Style.FILL
        }
        val trackPaintObj = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = trackColor
            style = Paint.Style.FILL
        }

        val activeSegmentsCount = (data.percentage.coerceIn(0, 100) * totalSegments) / 100

        for (i in 0 until totalSegments) {
            val segLeft = barStartX + i * (segmentW + gap)
            val segRight = segLeft + segmentW
            val segRect = RectF(segLeft, barY, segRight, barY + barH)

            val paint = if (i < activeSegmentsCount) activePaint else trackPaintObj
            canvas.drawRoundRect(segRect, segmentRadius, segmentRadius, paint)
        }
    }

    return bitmap
}

// 11. 5-Pill Gauge Tile (1:1 Square)
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
    val cardSize = minOf(w, h) - (margin * 2f)
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val accentColor = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val dimColor = if (isLight) 0x1F000000 else 0x26FFFFFF
    val enclosureBgColor = if (isLight) 0x0F000000 else 0x1AFFFFFF

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    canvas.drawRoundRect(RectF(leftX, topY, leftX + cardSize, topY + cardSize), cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = cardSize * 0.10f

    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (data.isCharging) accentColor else primaryTextColor
        textSize = cardSize * 0.20f
        textAlign = Paint.Align.CENTER
        typeface = getSlateFont(context, weight = 700)
    }
    val fontMetricsPct = pctPaint.fontMetrics
    val textY = topY + pad + (cardSize * 0.08f) - (fontMetricsPct.ascent + fontMetricsPct.descent) / 2f
    canvas.drawText("${data.percentage}%", leftX + (cardSize / 2f), textY, pctPaint)

    val enclosureW = cardSize * 0.78f
    val enclosureH = cardSize * 0.38f
    val enclosureLeft = leftX + (cardSize - enclosureW) / 2f
    val enclosureTop = topY + (cardSize * 0.36f)
    val enclosureRect = RectF(enclosureLeft, enclosureTop, enclosureLeft + enclosureW, enclosureTop + enclosureH)

    val enclosurePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = enclosureBgColor
        style = Paint.Style.FILL
    }
    val enclosureCornerRadius = enclosureH * 0.30f
    canvas.drawRoundRect(enclosureRect, enclosureCornerRadius, enclosureCornerRadius, enclosurePaint)

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

// 12. Pixel Heart Tile (1:1 Square)
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
    val cardSize = minOf(w, h) - (margin * 2f)
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f

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
    canvas.drawRoundRect(RectF(leftX, topY, leftX + cardSize, topY + cardSize), cardCornerRadius, cardCornerRadius, bgPaint)

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
    heightPx: Int
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
    heightPx: Int
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
    heightPx: Int
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