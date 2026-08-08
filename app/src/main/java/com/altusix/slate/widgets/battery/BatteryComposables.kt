package com.altusix.slate.widgets.battery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.altusix.slate.core.theme.SlateColors
import com.altusix.slate.data.local.SlateWidgetConfig
import kotlin.math.floor
import kotlin.math.roundToInt

// Standardized outer card corner radius (22dp)
private fun getStandardCornerRadius(density: Float): Float = 22f * density

// ============================================================================
// HELPER: Canvas Bitmap Background
// ============================================================================
fun createRoundedBackgroundBitmap(
    color: Color,
    widthPx: Int = 300,
    heightPx: Int = 300,
    cornerRadiusPx: Float = 40f
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(10), heightPx.coerceAtLeast(10), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgPaint = Paint().apply {
        isAntiAlias = true
        this.color = color.toArgb()
        style = Paint.Style.FILL
    }

    val rect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
    canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, bgPaint)

    return bitmap
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
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cardSize = minOf(w, h).toFloat()
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f

    val isLight = config.themeMode == "LIGHT"
    val bgColor = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val activeColor = Color(config.accentColorHex).toArgb()
    val dimColor = if (isLight) 0x1F000000 else 0x1AFFFFFF

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    val cardCornerRadius = getStandardCornerRadius(density)
    canvas.drawRoundRect(
        RectF(leftX, topY, leftX + cardSize, topY + cardSize),
        cardCornerRadius,
        cardCornerRadius,
        bgPaint
    )

    val pad = cardSize * 0.12f

    // Header Text
    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardSize * 0.18f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    canvas.drawText("${data.percentage}%", leftX + pad, topY + pad + (cardSize * 0.14f), pctPaint)

    val statusText = if (data.isCharging) "⚡ CHARGING" else "BATTERY"
    val statusColor = if (data.isCharging) activeColor else secondaryTextColor
    val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = statusColor
        textSize = cardSize * 0.07f
        textAlign = Paint.Align.RIGHT
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    canvas.drawText(statusText, leftX + cardSize - pad, topY + pad + (cardSize * 0.14f), statusPaint)

    // 10x10 Dot Grid (Fills Bottom-Up)
    val columns = 10
    val rows = 10
    val gridTopY = topY + pad + (cardSize * 0.22f)
    val gridW = cardSize - (pad * 2f)
    val gridH = cardSize - pad - (gridTopY - topY)

    val cellW = gridW / columns
    val cellH = gridH / rows
    val dotRadius = minOf(cellW, cellH) * 0.35f

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
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cardSize = minOf(w, h).toFloat()
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f

    val isLight = config.themeMode == "LIGHT"
    val bgColor = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val activeColor = Color(config.accentColorHex).toArgb()
    val dimColor = if (isLight) 0x1F000000 else 0x1AFFFFFF

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    val cardCornerRadius = getStandardCornerRadius(density)
    canvas.drawRoundRect(
        RectF(leftX, topY, leftX + cardSize, topY + cardSize),
        cardCornerRadius,
        cardCornerRadius,
        bgPaint
    )

    val pad = cardSize * 0.14f
    val columns = 10
    val rows = 10
    val gridW = cardSize - (pad * 2f)
    val gridH = cardSize - (pad * 2f)

    val cellW = gridW / columns
    val cellH = gridH / rows
    val dotRadius = minOf(cellW, cellH) * 0.36f

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
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cardSize = minOf(w, h).toFloat()
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f

    val isLight = config.themeMode == "LIGHT"
    val bgColor = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val trackColor = if (isLight) 0x1F000000 else 0x1FAFAFAF
    val accentColor = Color(config.accentColorHex).toArgb()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    val cardCornerRadius = getStandardCornerRadius(density)
    canvas.drawRoundRect(
        RectF(leftX, topY, leftX + cardSize, topY + cardSize),
        cardCornerRadius,
        cardCornerRadius,
        bgPaint
    )

    val pad = cardSize * 0.12f

    val statusText = if (data.isCharging) "⚡ CHARGING" else "BATTERY"
    val statusColor = if (data.isCharging) accentColor else secondaryTextColor

    val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = statusColor
        textSize = cardSize * 0.08f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    canvas.drawText(statusText, leftX + pad, topY + pad + (cardSize * 0.08f), statusPaint)

    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardSize * 0.32f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val pctText = "${data.percentage}%"
    val textY = topY + (cardSize * 0.58f)
    canvas.drawText(pctText, leftX + pad, textY, pctPaint)

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
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cardSize = minOf(w, h).toFloat()
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f

    val isLight = config.themeMode == "LIGHT"
    val bgColor = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val trackColor = if (isLight) 0x1F000000 else 0x1FAFAFAF
    val accentColor = Color(config.accentColorHex).toArgb()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    val cardCornerRadius = getStandardCornerRadius(density)
    canvas.drawRoundRect(
        RectF(leftX, topY, leftX + cardSize, topY + cardSize),
        cardCornerRadius,
        cardCornerRadius,
        bgPaint
    )

    val strokeW = cardSize * 0.065f
    val arcMargin = cardSize * 0.14f
    val arcRect = RectF(
        leftX + arcMargin,
        topY + arcMargin,
        leftX + cardSize - arcMargin,
        topY + cardSize - arcMargin
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

    val centerX = leftX + (cardSize / 2f)
    val centerY = topY + (cardSize / 2f)

    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardSize * 0.24f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    val pctText = "${data.percentage}%"
    val textBounds = Rect()
    pctPaint.getTextBounds(pctText, 0, pctText.length, textBounds)
    val textY = centerY + (textBounds.height() / 2f) - (cardSize * 0.02f)
    canvas.drawText(pctText, centerX, textY, pctPaint)

    val statusText = if (data.isCharging) "⚡ CHARGING" else "BATTERY"
    val statusColor = if (data.isCharging) accentColor else secondaryTextColor

    val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = statusColor
        textSize = cardSize * 0.075f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    val statusY = textY + (cardSize * 0.16f)
    canvas.drawText(statusText, centerX, statusY, statusPaint)

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
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cardSize = minOf(w, h).toFloat()
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f

    val isLight = config.themeMode == "LIGHT"
    val bgColor = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val accentColor = Color(config.accentColorHex)
    val trackColor = if (isLight) Color(0x1F000000) else accentColor.copy(alpha = 0.2f)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    val cardCornerRadius = getStandardCornerRadius(density)
    canvas.drawRoundRect(
        RectF(leftX, topY, leftX + cardSize, topY + cardSize),
        cardCornerRadius,
        cardCornerRadius,
        bgPaint
    )

    val pad = cardSize * 0.10f

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = cardSize * 0.075f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    canvas.drawText("BATTERY", leftX + pad, topY + pad + (cardSize * 0.075f), textPaint)

    if (data.isCharging) {
        val chargingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor.toArgb()
            textSize = cardSize * 0.07f
            textAlign = Paint.Align.RIGHT
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText("⚡ CHARGING", leftX + cardSize - pad, topY + pad + (cardSize * 0.075f), chargingPaint)
    }

    val gaugeW = (cardSize * 0.72f).toInt()
    val gaugeH = (cardSize * 0.36f).toInt()
    val arcBitmap = generateArcGaugeBitmap(data.percentage, accentColor, trackColor, gaugeW, gaugeH)

    val arcLeft = leftX + (cardSize - gaugeW) / 2f
    val arcTop = topY + (cardSize * 0.22f)
    canvas.drawBitmap(arcBitmap, arcLeft, arcTop, null)

    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardSize * 0.26f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val textY = arcTop + gaugeH + (cardSize * 0.26f)
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
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cardSize = minOf(w, h).toFloat()
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f

    val isLight = config.themeMode == "LIGHT"
    val bgColor = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val accentColor = Color(config.accentColorHex)
    val trackColor = if (isLight) Color(0x1F000000) else Color(0x2EFFFFFF)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    val cardCornerRadius = getStandardCornerRadius(density)
    canvas.drawRoundRect(
        RectF(leftX, topY, leftX + cardSize, topY + cardSize),
        cardCornerRadius,
        cardCornerRadius,
        bgPaint
    )

    val pad = cardSize * 0.12f

    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardSize * 0.30f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    canvas.drawText("${data.percentage}%", leftX + pad, topY + pad + (cardSize * 0.26f), pctPaint)

    val statPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = cardSize * 0.085f
    }
    canvas.drawText("• ${data.healthText}", leftX + pad, topY + pad + (cardSize * 0.44f), statPaint)
    canvas.drawText("• ${data.secondaryStatText}", leftX + pad, topY + pad + (cardSize * 0.56f), statPaint)

    val barH = (cardSize * 0.10f).toInt()
    val barW = (cardSize - (pad * 2f)).toInt()
    val barBitmap = generateSegmentedBarBitmap(data.percentage, accentColor, trackColor, barW, barH)
    canvas.drawBitmap(barBitmap, leftX + pad, topY + cardSize - pad - barH, null)

    return bitmap
}

// 7. Multi-Device Stats Bento (4x2)
fun generateMultiDeviceBatteryBitmap(
    context: Context,
    data: DetailedBatteryData,
    config: SlateWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val accentColor = Color(config.accentColorHex).toArgb()
    val trackColor = if (isLight) 0x1F000000 else 0x1FAFAFAF
    val tileBgColor = if (isLight) 0x0A000000 else 0x18FFFFFF

    val cardW = w.toFloat()
    val maxCardH = cardW * 0.48f
    val cardH = minOf(h.toFloat(), maxCardH)

    val leftX = 0f
    val topY = (h - cardH) / 2f

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    val cardCornerRadius = getStandardCornerRadius(density)
    canvas.drawRoundRect(RectF(leftX, topY, leftX + cardW, topY + cardH), cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = cardH * 0.08f
    val gap = cardH * 0.06f

    val availW = cardW - (pad * 2f)
    val availH = cardH - (pad * 2f)

    val heroW = availW * 0.40f
    val heroH = availH

    val rightX = leftX + pad + heroW + gap
    val rightW = availW - heroW - gap
    val statH = (availH - gap) / 2f

    val tileBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tileBgColor }
    val tileCornerRadius = cardH * 0.12f

    val heroRect = RectF(leftX + pad, topY + pad, leftX + pad + heroW, topY + pad + heroH)
    canvas.drawRoundRect(heroRect, tileCornerRadius, tileCornerRadius, tileBgPaint)

    val heroPad = heroH * 0.12f

    val heroTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = heroH * 0.09f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    canvas.drawText("PHONE", heroRect.left + heroPad, heroRect.top + heroPad + (heroH * 0.08f), heroTagPaint)

    val statusText = if (data.isCharging) "⚡ CHARGING" else "BATTERY"
    val statusColor = if (data.isCharging) accentColor else secondaryTextColor
    val heroStatusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = statusColor
        textSize = heroH * 0.075f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    canvas.drawText(statusText, heroRect.left + heroPad, heroRect.top + heroPad + (heroH * 0.21f), heroStatusPaint)

    val heroPctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = heroH * 0.32f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    canvas.drawText("${data.percentage}%", heroRect.left + heroPad, heroRect.top + (heroH * 0.65f), heroPctPaint)

    val barH = heroH * 0.07f
    val barTop = heroRect.bottom - heroPad - barH
    val heroTrackRect = RectF(heroRect.left + heroPad, barTop, heroRect.right - heroPad, barTop + barH)
    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = trackColor }
    canvas.drawRoundRect(heroTrackRect, barH / 2f, barH / 2f, trackPaint)

    val fillW = (heroTrackRect.width()) * (data.percentage.coerceIn(0, 100) / 100f)
    if (fillW > 0f) {
        val heroFillRect = RectF(heroTrackRect.left, barTop, heroTrackRect.left + fillW, barTop + barH)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
        canvas.drawRoundRect(heroFillRect, barH / 2f, barH / 2f, fillPaint)
    }

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = statH * 0.18f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = statH * 0.28f
        textAlign = Paint.Align.RIGHT
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }

    val tempRect = RectF(rightX, topY + pad, rightX + rightW, topY + pad + statH)
    canvas.drawRoundRect(tempRect, tileCornerRadius, tileCornerRadius, tileBgPaint)

    val statPadX = rightW * 0.08f
    val statPadY = statH * 0.22f

    canvas.drawText("TEMPERATURE", tempRect.left + statPadX, tempRect.top + statPadY + (statH * 0.12f), labelPaint)
    canvas.drawText(data.tempText, tempRect.right - statPadX, tempRect.top + statPadY + (statH * 0.22f), valuePaint)

    val tempBarH = statH * 0.12f
    val tempBarTop = tempRect.bottom - statPadY - tempBarH
    val tempTrackRect = RectF(tempRect.left + statPadX, tempBarTop, tempRect.right - statPadX, tempBarTop + tempBarH)
    canvas.drawRoundRect(tempTrackRect, tempBarH / 2f, tempBarH / 2f, trackPaint)

    val tempVal = data.tempText.replace("°C", "").toFloatOrNull() ?: 35f
    val tempRatio = (tempVal / 50f).coerceIn(0.1f, 1f)
    val tempFillRect = RectF(tempTrackRect.left, tempBarTop, tempTrackRect.left + (tempTrackRect.width() * tempRatio), tempBarTop + tempBarH)
    canvas.drawRoundRect(tempFillRect, tempBarH / 2f, tempBarH / 2f, activePaint)

    val voltRect = RectF(rightX, topY + pad + statH + gap, rightX + rightW, topY + pad + availH)
    canvas.drawRoundRect(voltRect, tileCornerRadius, tileCornerRadius, tileBgPaint)

    canvas.drawText("VOLTAGE", voltRect.left + statPadX, voltRect.top + statPadY + (statH * 0.12f), labelPaint)
    canvas.drawText(data.voltageText, voltRect.right - statPadX, voltRect.top + statPadY + (statH * 0.22f), valuePaint)

    val voltBarH = statH * 0.12f
    val voltBarTop = voltRect.bottom - statPadY - voltBarH
    val voltTrackRect = RectF(voltRect.left + statPadX, voltBarTop, voltRect.right - statPadX, voltBarTop + voltBarH)
    canvas.drawRoundRect(voltTrackRect, voltBarH / 2f, voltBarH / 2f, trackPaint)

    val voltVal = data.voltageText.replace("V", "").toFloatOrNull() ?: 3.8f
    val voltRatio = (voltVal / 4.4f).coerceIn(0.1f, 1f)
    val voltFillRect = RectF(voltTrackRect.left, voltBarTop, voltTrackRect.left + (voltTrackRect.width() * voltRatio), voltBarTop + voltBarH)
    canvas.drawRoundRect(voltFillRect, voltBarH / 2f, voltBarH / 2f, activePaint)

    return bitmap
}

// 8. Dot Matrix LED (4x2)
fun generateDotMatrixLEDBitmap(
    text: String,
    activeColor: Color,
    dimColor: Color,
    bgColor: Color,
    targetWidthPx: Int,
    targetHeightPx: Int
): Bitmap {
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
        color = bgColor.toArgb()
        style = Paint.Style.FILL
    }
    // Estimated density scale for 22dp corner radius
    val cardCornerRadius = 22f * (cardH / 150f).coerceAtLeast(1.0f)
    canvas.drawRoundRect(RectF(leftX, topY, leftX + cardW, topY + cardH), cardCornerRadius, cardCornerRadius, bgPaint)

    val rows = 9
    val padY = cardH * 0.12f
    val padX = cardW * 0.06f
    val availH = cardH - (padY * 2f)
    val availW = cardW - (padX * 2f)

    val glyphWidth = 5
    val glyphGap = 1
    val textWidthCols = text.length * glyphWidth + (text.length - 1) * glyphGap

    val minRequiredCols = textWidthCols + 2
    val baseCellSize = availH / rows.toFloat()
    val initialCols = (availW / baseCellSize).roundToInt()
    val columns = maxOf(initialCols, minRequiredCols)

    val finalCellSize = minOf(availW / columns.toFloat(), availH / rows.toFloat())
    val dotRadius = finalCellSize * 0.38f

    val gridW = columns * finalCellSize
    val gridH = rows * finalCellSize

    val startX = leftX + (cardW - gridW) / 2f
    val startY = topY + (cardH - gridH) / 2f

    val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dimColor.toArgb()
        style = Paint.Style.FILL
    }

    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = activeColor.toArgb()
        style = Paint.Style.FILL
    }

    for (r in 0 until rows) {
        for (c in 0 until columns) {
            val cx = startX + c * finalCellSize + finalCellSize / 2f
            val cy = startY + r * finalCellSize + finalCellSize / 2f
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
                            val cx = startX + c * finalCellSize + finalCellSize / 2f
                            val cy = startY + targetRow * finalCellSize + finalCellSize / 2f
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
    percentage: Int,
    activeColor: Color,
    dimColor: Color,
    bgColor: Color,
    targetWidthPx: Int,
    targetHeightPx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(targetWidthPx.coerceAtLeast(1), targetHeightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = targetWidthPx.toFloat()
    val h = targetHeightPx.toFloat()

    val columns = 20
    val rows = 5

    val padX = w * 0.05f
    val padY = padX

    val maxAvailW = w - (padX * 2f)
    val maxAvailH = h - (padY * 2f)

    val cellSize = minOf(maxAvailW / columns, maxAvailH / rows)
    val gridW = columns * cellSize
    val gridH = rows * cellSize

    val cardW = gridW + (padX * 2f)
    val cardH = gridH + (padY * 2f)

    val leftX = (w - cardW) / 2f
    val topY = (h - cardH) / 2f

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor.toArgb()
        style = Paint.Style.FILL
    }
    val cardCornerRadius = 22f * (cardH / 100f).coerceAtLeast(1.0f)
    canvas.drawRoundRect(RectF(leftX, topY, leftX + cardW, topY + cardH), cardCornerRadius, cardCornerRadius, bgPaint)

    val startX = leftX + padX
    val startY = topY + padY
    val dotRadius = cellSize * 0.36f

    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = activeColor.toArgb()
        style = Paint.Style.FILL
    }

    val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dimColor.toArgb()
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
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cardW = w.toFloat()
    val maxCardH = cardW * 0.28f
    val cardH = minOf(h.toFloat(), maxCardH)

    val leftX = 0f
    val topY = (h - cardH) / 2f

    val isLight = config.themeMode == "LIGHT"
    val bgColor = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val activeColor = Color(config.accentColorHex).toArgb()
    val trackColor = if (isLight) 0x1F000000 else 0x1AFFFFFF

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    val cardCornerRadius = getStandardCornerRadius(density)
    canvas.drawRoundRect(RectF(leftX, topY, leftX + cardW, topY + cardH), cardCornerRadius, cardCornerRadius, bgPaint)

    val padX = cardW * 0.06f

    // Vertically Centered Percentage Text
    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardH * 0.46f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val pctText = "${data.percentage}%"
    val pctTextWidth = pctPaint.measureText(pctText)

    val textBounds = Rect()
    pctPaint.getTextBounds(pctText, 0, pctText.length, textBounds)
    val textY = topY + (cardH / 2f) + (textBounds.height() / 2f)

    val textStartX = leftX + padX
    canvas.drawText(pctText, textStartX, textY, pctPaint)

    // Right 10-Segment Pill Gauge
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
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cardSize = minOf(w, h).toFloat()
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f

    val isLight = config.themeMode == "LIGHT"
    val bgColor = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val accentColor = Color(config.accentColorHex).toArgb()
    val dimColor = if (isLight) 0x1F000000 else 0x26FFFFFF
    val enclosureBgColor = if (isLight) 0x0F000000 else 0x1AFFFFFF

    // Outer Background Card (Standard 22dp Radius)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    val cardCornerRadius = 22f * density
    canvas.drawRoundRect(
        RectF(leftX, topY, leftX + cardSize, topY + cardSize),
        cardCornerRadius,
        cardCornerRadius,
        bgPaint
    )

    val pad = cardSize * 0.10f

    // Header Text: Clean Percentage (Accent colored if charging)
    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (data.isCharging) accentColor else primaryTextColor
        textSize = cardSize * 0.20f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val textY = topY + pad + (cardSize * 0.15f)
    canvas.drawText("${data.percentage}%", leftX + (cardSize / 2f), textY, pctPaint)

    // Center Capsule Enclosure
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

    // 5 Inner Vertical Bars
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

    // Footer Status Text
    val footerText = if (data.isCharging) "CHARGING" else "DISCHARGING"
    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (data.isCharging) accentColor else secondaryTextColor
        textSize = cardSize * 0.075f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val footerY = topY + cardSize - pad - (cardSize * 0.02f)
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
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 1. Lock outer card to a 1:1 square centered in bounds
    val cardSize = minOf(w, h).toFloat()
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f

    val isLight = config.themeMode == "LIGHT"
    val bgColor = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val accentColor = Color(config.accentColorHex).toArgb()
    val dimColor = if (isLight) 0x1F000000 else 0x1AFFFFFF

    // 2. Outer Background Card (Standard 22dp Radius)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    val cardCornerRadius = 22f * density
    canvas.drawRoundRect(
        RectF(leftX, topY, leftX + cardSize, topY + cardSize),
        cardCornerRadius,
        cardCornerRadius,
        bgPaint
    )

    // 3. Heart Grid Matrix
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

    // Fill Bottom-Up
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
    wDp: Int,
    hDp: Int,
    isWide: Boolean
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val accentColor = Color(config.accentColorHex).toArgb()
    val dimColor = if (isLight) 0x1F000000 else 0x2BFFFFFF

    // 1. Lock Height: Set a hard max-height (approx standard 2-row height).
    // This guarantees the height never changes when the widget expands horizontally.
    val maxCardH = 156f * density
    val cardH = minOf(h.toFloat(), maxCardH)

    // 2. Lock Width: If it's a square (2x2), width is locked to the height. If wide (4x2), it fills horizontal space.
    val cardW = if (isWide) w.toFloat() else cardH

    val leftX = (w - cardW) / 2f
    val topY = (h - cardH) / 2f

    // Outer Background Card (Standard 22dp Radius)
    val cardCornerRadius = 22f * density
    val cardRect = RectF(leftX, topY, leftX + cardW, topY + cardH)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    canvas.save()
    val cardClipPath = Path().apply {
        addRoundRect(cardRect, cardCornerRadius, cardCornerRadius, Path.Direction.CW)
    }
    canvas.clipPath(cardClipPath)

    // 3. Bolt Path Geometry scaled proportionally to the locked cardH
    val scale = cardH / 300f
    val centerX = if (isWide) leftX + cardW - (150f * scale) else leftX + (cardW / 2f)

    val boltPath = Path().apply {
        moveTo(centerX - (48f * scale), topY - (36f * scale))
        lineTo(centerX + (115f * scale), topY - (36f * scale))
        lineTo(centerX - (12f * scale), topY + (126f * scale))
        lineTo(centerX + (145f * scale), topY + (126f * scale))
        lineTo(centerX - (125f * scale), topY + (336f * scale))
        lineTo(centerX - (42f * scale), topY + (162f * scale))
        lineTo(centerX - (145f * scale), topY + (162f * scale))
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

    // Draw Unfilled Dim Bolt
    canvas.drawPath(boltPath, dimPaint)

    // 4. Liquid Wave Fill Animation
    val fillProgress = data.percentage.coerceIn(0, 100) / 100f
    val minFillY = topY + (336f * scale)
    val maxFillY = topY - (36f * scale)
    val fillY = minFillY - ((minFillY - maxFillY) * fillProgress)

    if (fillProgress > 0f) {
        val wavePath = Path().apply {
            val waveAmplitude = 10f * scale
            val waveLength = 250f * scale

            moveTo(leftX - (100f * scale), fillY)

            var x = leftX - (100f * scale)
            var isUp = true
            while (x < leftX + cardW + (100f * scale)) {
                val nextX = x + (waveLength / 2f)
                val midX = x + ((nextX - x) / 2f)
                val controlY = if (isUp) fillY - waveAmplitude else fillY + waveAmplitude

                quadTo(midX, controlY, nextX, fillY)
                x = nextX
                isUp = !isUp
            }

            lineTo(leftX + cardW + (100f * scale), topY + cardH + (100f * scale))
            lineTo(leftX - (100f * scale), topY + cardH + (100f * scale))
            close()
        }

        canvas.save()
        canvas.clipPath(wavePath)
        canvas.drawPath(boltPath, activePaint)
        canvas.restore()
    }

    // 5. Text Overlay for Wide Card (4x2)
    if (isWide) {
        val padX = leftX + (22f * density)
        val padY = topY + (20f * density)

        val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = 22f * density
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText("${data.percentage}% / ${data.tempText}", padX, padY + (18f * density), pctPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryTextColor
            textSize = 12f * density
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val subText = if (data.isCharging) "Charging • ${data.voltageText}" else "Discharging • ${data.voltageText}"
        canvas.drawText(subText, padX, padY + (38f * density), subPaint)

        val botPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = 12f * density
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val botText = if (data.isCharging) "Fast Charging Active" else "Battery Normal"
        canvas.drawText(botText, padX, topY + cardH - (18f * density), botPaint)
    }

    canvas.restore()
    return bitmap
}

// 14. Circular Dial
fun generateCircularGaugeBitmap(
    percentage: Int,
    isCharging: Boolean,
    accentColor: Color,
    dimColor: Color,
    iconColor: Color,
    bgColor: Color,
    isLight: Boolean,
    scale: Float,
    widthPx: Int,
    heightPx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = widthPx.toFloat()
    val h = heightPx.toFloat()

    val cardSize = minOf(w, h)
    val cx = w / 2f
    val cy = h / 2f

    // 1. Draw Background Circle
    val bgPaint = Paint().apply {
        isAntiAlias = true
        color = bgColor.toArgb()
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
        color = dimColor.toArgb()
        style = Paint.Style.STROKE
        strokeWidth = ringStrokeWidth
        strokeCap = Paint.Cap.ROUND
    }

    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor.toArgb()
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

    // 2. Tick Marks Around Inner Ring
    val tickColor = if (isLight) Color(0x20000000).toArgb() else Color(0x28FFFFFF).toArgb()
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

    // 3. Battery Icon Rendering (Repositioned for balanced vertical stack)
    val iconY = cy - (cardSize * 0.125f)
    val batW = cardSize * 0.085f
    val batH = cardSize * 0.135f

    val shellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = iconColor.toArgb()
        style = Paint.Style.STROKE
        strokeWidth = (2.5f * dynamicScale).coerceAtLeast(2f)
    }

    val fillColor = if (percentage <= 20 && !isCharging) {
        Color(0xFFFF3B30).toArgb()
    } else {
        accentColor.toArgb()
    }

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
        val boltColor = if (isLight) Color(0xFF000000).toArgb() else Color(0xFFFFFFFF).toArgb()
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

    // 4. Percentage & Status Text Rendering (Spaced explicitly underneath icon)
    val primaryTextColor = if (isLight) Color(0xFF000000).toArgb() else Color(0xFFFFFFFF).toArgb()
    val secondaryTextColor = if (isLight) Color(0x99000000).toArgb() else Color(0x99FFFFFF).toArgb()

    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardSize * 0.155f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val textY = cy + (cardSize * 0.095f)
    canvas.drawText("${percentage}%", cx, textY, pctPaint)

    val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = cardSize * 0.048f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.06f
    }
    val statusText = if (isCharging) "CHARGING" else "DISCHARGING"
    val statusY = textY + (cardSize * 0.075f)
    canvas.drawText(statusText, cx, statusY, statusPaint)

    return bitmap
}

// 15. Vertical Pill
fun generateVerticalPillBitmap(
    percentage: Int,
    isCharging: Boolean,
    config: SlateWidgetConfig,
    widthPx: Int,
    heightPx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
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
    val accentColor = Color(config.accentColorHex)
    val isLowBattery = percentage <= 20 && !isCharging

    val shellBgColor = if (isLight) Color(0xFFFFFFFF).toArgb() else Color(0xFF141416).toArgb()
    val strokeColor = if (isLight) Color(0xFFD1D1D6).toArgb() else Color(0xFF2C2C2E).toArgb()
    val activeColor = if (isLowBattery) Color(0xFFFF3B30).toArgb() else accentColor.toArgb()
    val dimColor = if (isLight) Color(0x14000000).toArgb() else Color(0x1AFFFFFF).toArgb()
    val capColor = if (isCharging) activeColor else strokeColor

    val strokePaint = Paint().apply {
        isAntiAlias = true
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = strokeW
    }

    val capPaint = Paint().apply {
        isAntiAlias = true
        color = capColor
        style = Paint.Style.FILL
    }

    val shellPaint = Paint().apply {
        isAntiAlias = true
        color = shellBgColor
        style = Paint.Style.FILL
    }

    val activePaint = Paint().apply {
        isAntiAlias = true
        color = activeColor
        style = Paint.Style.FILL
    }

    val dimPaint = Paint().apply {
        isAntiAlias = true
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
    percentage: Int,
    isCharging: Boolean,
    config: SlateWidgetConfig,
    widthPx: Int,
    heightPx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
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
    val accentColor = Color(config.accentColorHex)
    val isLowBattery = percentage <= 20 && !isCharging

    val shellBgColor = if (isLight) Color(0xFFFFFFFF).toArgb() else Color(0xFF141416).toArgb()
    val strokeColor = if (isLight) Color(0xFFD1D1D6).toArgb() else Color(0xFF2C2C2E).toArgb()
    val activeColor = if (isLowBattery) Color(0xFFFF3B30).toArgb() else accentColor.toArgb()
    val dimColor = if (isLight) Color(0x14000000).toArgb() else Color(0x1AFFFFFF).toArgb()
    val capColor = if (isCharging) activeColor else strokeColor

    val strokePaint = Paint().apply {
        isAntiAlias = true
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = strokeW
    }

    val capPaint = Paint().apply {
        isAntiAlias = true
        color = capColor
        style = Paint.Style.FILL
    }

    val shellPaint = Paint().apply {
        isAntiAlias = true
        color = shellBgColor
        style = Paint.Style.FILL
    }

    val activePaint = Paint().apply {
        isAntiAlias = true
        color = activeColor
        style = Paint.Style.FILL
    }

    val dimPaint = Paint().apply {
        isAntiAlias = true
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

fun generateArcGaugeBitmap(
    percentage: Int,
    accentColor: Color,
    trackColor: Color,
    widthPx: Int,
    heightPx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val strokeWidth = widthPx * 0.18f
    val padding = strokeWidth / 2f + 4f
    val rectF = RectF(padding, padding, widthPx - padding, heightPx * 2f - padding)

    val startAngle = 210f
    val maxSweep = 120f
    val currentSweep = (percentage.coerceIn(0, 100) / 100f) * maxSweep

    val trackPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        setStrokeWidth(strokeWidth)
        strokeCap = Paint.Cap.BUTT
        color = trackColor.toArgb()
    }

    val activePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        setStrokeWidth(strokeWidth)
        strokeCap = Paint.Cap.BUTT
        color = accentColor.toArgb()
    }

    canvas.drawArc(rectF, startAngle, maxSweep, false, trackPaint)

    if (currentSweep > 0) {
        canvas.drawArc(rectF, startAngle, currentSweep, false, activePaint)
    }

    return bitmap
}

fun generateSegmentedBarBitmap(
    percentage: Int,
    accentColor: Color,
    trackColor: Color,
    widthPx: Int,
    heightPx: Int,
    totalSegments: Int = 20
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val activeSegments = (percentage.coerceIn(0, 100) / 100f * totalSegments).toInt()
    val segmentWidth = widthPx.toFloat() / totalSegments
    val barWidth = segmentWidth * 0.58f

    val activePaint = Paint().apply {
        isAntiAlias = true
        color = accentColor.toArgb()
        style = Paint.Style.FILL
    }

    val trackPaint = Paint().apply {
        isAntiAlias = true
        color = trackColor.toArgb()
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