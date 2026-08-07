package com.altusix.slate.widgets.battery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.altusix.slate.core.theme.SlateColors
import com.altusix.slate.data.local.SlateWidgetConfig
import kotlin.math.floor
import kotlin.math.roundToInt

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

fun generateMinimalBatteryBitmap(
    context: Context,
    data: DetailedBatteryData,
    config: SlateWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt()
    val h = (hDp * density).toInt()

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = Color(config.backgroundColorHex).copy(alpha = config.opacity)
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val trackColor = if (isLight) 0x1F000000 else 0x1FAFAFAF
    val accentColor = Color(config.accentColorHex).toArgb()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor.toArgb() }
    val cornerRadius = 24f * density
    canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), cornerRadius, cornerRadius, bgPaint)

    val pad = 16f * density

    // Header
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = 10f * density
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    canvas.drawText("BATTERY", pad, pad + 10f * density, textPaint)

    if (data.isCharging) {
        val chargingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 9f * density
            textAlign = Paint.Align.RIGHT
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText("CHARGING", w - pad, pad + 10f * density, chargingPaint)
    }

    // Main Percentage Text
    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = 38f * density
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    canvas.drawText("${data.percentage}%", pad, h / 2f + 12f * density, pctPaint)

    // Progress Bar
    val barHeight = 6f * density
    val barTop = h - pad - barHeight
    val barRect = RectF(pad, barTop, w - pad, barTop + barHeight)
    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = trackColor }
    canvas.drawRoundRect(barRect, barHeight / 2f, barHeight / 2f, trackPaint)

    val fillWidth = (w - (pad * 2f)) * (data.percentage.coerceIn(0, 100) / 100f)
    if (fillWidth > 0f) {
        val fillRect = RectF(pad, barTop, pad + fillWidth, barTop + barHeight)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
        canvas.drawRoundRect(fillRect, barHeight / 2f, barHeight / 2f, fillPaint)
    }

    return bitmap
}

fun generateMultiDeviceBatteryBitmap(
    context: Context,
    data: DetailedBatteryData,
    config: SlateWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt()
    val h = (hDp * density).toInt()

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = Color(config.backgroundColorHex).copy(alpha = config.opacity)
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val trackColor = if (isLight) 0x1F000000 else 0x1FAFAFAF
    val accentColor = Color(config.accentColorHex).toArgb()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor.toArgb() }
    val cornerRadius = 24f * density
    canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), cornerRadius, cornerRadius, bgPaint)

    val pad = 16f * density
    val rows = listOf(
        Pair(if (data.isCharging) "PHONE • CHARGING" else "PHONE LEVEL", "${data.percentage}%" to (data.percentage / 100f)),
        Pair("TEMPERATURE", data.tempText to 0.45f),
        Pair("VOLTAGE", data.voltageText to 0.82f)
    )

    var currentY = pad + 10f * density
    val rowHeight = (h - (pad * 2f)) / 3f

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = 10f * density
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = 12f * density
        textAlign = Paint.Align.RIGHT
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = trackColor }
    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }

    for ((title, info) in rows) {
        canvas.drawText(title, pad, currentY, labelPaint)
        canvas.drawText(info.first, w - pad, currentY, valPaint)

        val barTop = currentY + 6f * density
        val barHeight = 5f * density
        val barRect = RectF(pad, barTop, w - pad, barTop + barHeight)
        canvas.drawRoundRect(barRect, barHeight / 2f, barHeight / 2f, trackPaint)

        val fillW = (w - (pad * 2f)) * info.second.coerceIn(0f, 1f)
        if (fillW > 0f) {
            val fillRect = RectF(pad, barTop, pad + fillW, barTop + barHeight)
            canvas.drawRoundRect(fillRect, barHeight / 2f, barHeight / 2f, activePaint)
        }

        currentY += rowHeight
    }

    return bitmap
}

fun generateArcGaugeTileBitmap(
    context: Context,
    data: DetailedBatteryData,
    config: SlateWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt()
    val h = (hDp * density).toInt()

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = Color(config.backgroundColorHex).copy(alpha = config.opacity)
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val accentColor = Color(config.accentColorHex)
    val trackColor = if (isLight) Color(0x1F000000) else accentColor.copy(alpha = 0.2f)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor.toArgb() }
    val cornerRadius = 24f * density
    canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), cornerRadius, cornerRadius, bgPaint)

    val pad = 14f * density

    // Header
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = 10f * density
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    canvas.drawText("BATTERY", pad, pad + 10f * density, textPaint)

    if (data.isCharging) {
        val chargingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor.toArgb()
            textSize = 9f * density
            textAlign = Paint.Align.RIGHT
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText("CHARGING", w - pad, pad + 10f * density, chargingPaint)
    }

    // Arc Gauge
    val gaugeW = (110f * density).toInt()
    val gaugeH = (55f * density).toInt()
    val arcBitmap = generateArcGaugeBitmap(data.percentage, accentColor, trackColor, gaugeW, gaugeH)

    val arcLeft = (w - gaugeW) / 2f
    val arcTop = pad + 18f * density
    canvas.drawBitmap(arcBitmap, arcLeft, arcTop, null)

    // Percentage Text
    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = 34f * density
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    canvas.drawText("${data.percentage}%", w / 2f, arcTop + gaugeH + 32f * density, pctPaint)

    return bitmap
}

fun generateEditorialStatsBitmap(
    context: Context,
    data: DetailedBatteryData,
    config: SlateWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt()
    val h = (hDp * density).toInt()

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = Color(config.backgroundColorHex).copy(alpha = config.opacity)
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary.toArgb() else SlateColors.TextDarkPrimary.toArgb()
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary.toArgb() else SlateColors.TextDarkSecondary.toArgb()
    val accentColor = Color(config.accentColorHex)
    val trackColor = if (isLight) Color(0x1F000000) else Color(0x2EFFFFFF)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor.toArgb() }
    val cornerRadius = 24f * density
    canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), cornerRadius, cornerRadius, bgPaint)

    val pad = 16f * density

    // Big Percentage
    val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = 42f * density
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    canvas.drawText("${data.percentage}%", pad, pad + 38f * density, pctPaint)

    // Health text
    val statPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = 12f * density
    }
    canvas.drawText("• ${data.healthText}", pad, pad + 62f * density, statPaint)
    canvas.drawText("• ${data.secondaryStatText}", pad, pad + 80f * density, statPaint)

    // Segmented Bar at bottom
    val barH = (16f * density).toInt()
    val barW = (w - (pad * 2f)).toInt()
    val barBitmap = generateSegmentedBarBitmap(data.percentage, accentColor, trackColor, barW, barH)
    canvas.drawBitmap(barBitmap, pad, h - pad - barH, null)

    return bitmap
}

// ============================================================================
// TOP-LEVEL GAUGES & PATTERN DRAWING FUNCTIONS
// ============================================================================

fun generateHorizontalStripBitmap(
    percentage: Int,
    accentColor: Color,
    trackColor: Color,
    bgColor: Color,
    widthPx: Int,
    heightPx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = widthPx.toFloat()
    val h = heightPx.toFloat()

    val cornerRadiusPx = 36f
    val cardRect = RectF(0f, 0f, w, h)
    val bgPaint = Paint().apply {
        isAntiAlias = true
        color = bgColor.toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadiusPx, cornerRadiusPx, bgPaint)

    canvas.save()
    val cardClipPath = Path().apply {
        addRoundRect(cardRect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
    }
    canvas.clipPath(cardClipPath)

    val paddingX = 58f
    val barTop = 154f
    val barHeight = 22f
    val barRadius = barHeight / 2f

    val trackRect = RectF(paddingX, barTop, w - paddingX, barTop + barHeight)
    val trackPaint = Paint().apply {
        isAntiAlias = true
        color = trackColor.toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(trackRect, barRadius, barRadius, trackPaint)

    val fillProgress = percentage.coerceIn(0, 100) / 100f
    if (fillProgress > 0f) {
        val totalBarWidth = w - (2f * paddingX)
        val fillWidth = totalBarWidth * fillProgress

        val fillPaint = Paint().apply {
            isAntiAlias = true
            color = accentColor.toArgb()
            style = Paint.Style.FILL
        }

        canvas.save()
        canvas.clipRect(paddingX, barTop, paddingX + fillWidth, barTop + barHeight)
        canvas.drawRoundRect(trackRect, barRadius, barRadius, fillPaint)
        canvas.restore()
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

fun generateDotMatrixLEDBitmap(
    text: String,
    activeColor: Color,
    dimColor: Color,
    bgColor: Color,
    targetWidthPx: Int,
    targetHeightPx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(targetWidthPx, targetHeightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = targetWidthPx.toFloat()
    val h = targetHeightPx.toFloat()

    val bgPaint = Paint().apply {
        isAntiAlias = true
        color = bgColor.toArgb()
        style = Paint.Style.FILL
    }
    val cornerRadiusPx = 54f
    canvas.drawRoundRect(0f, 0f, w, h, cornerRadiusPx, cornerRadiusPx, bgPaint)

    val rows = 9
    val padding = 28f
    val availH = h - (padding * 2f)
    val availW = w - (padding * 2f)

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

    val startX = (w - gridW) / 2f
    val startY = (h - gridH) / 2f

    val dimPaint = Paint().apply {
        isAntiAlias = true
        color = dimColor.toArgb()
        style = Paint.Style.FILL
    }

    val activePaint = Paint().apply {
        isAntiAlias = true
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
    val startRow = 1
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

fun generateDotLevelBitmap(
    percentage: Int,
    activeColor: Color,
    dimColor: Color,
    columns: Int,
    rows: Int,
    widthPx: Int,
    heightPx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cellW = widthPx.toFloat() / columns
    val cellH = heightPx.toFloat() / rows
    val dotRadius = minOf(cellW, cellH) * 0.36f

    val activePaint = Paint().apply {
        isAntiAlias = true
        color = activeColor.toArgb()
        style = Paint.Style.FILL
    }

    val dimPaint = Paint().apply {
        isAntiAlias = true
        color = dimColor.toArgb()
        style = Paint.Style.FILL
    }

    val totalDots = columns * rows
    val activeDotsCount = (percentage.coerceIn(0, 100) * totalDots) / 100
    val emptyDotsCount = totalDots - activeDotsCount

    for (i in 0 until totalDots) {
        val r = i / columns
        val c = i % columns

        val cx = c * cellW + cellW / 2f
        val cy = r * cellH + cellH / 2f

        val paint = if (i < emptyDotsCount) dimPaint else activePaint
        canvas.drawCircle(cx, cy, dotRadius, paint)
    }

    return bitmap
}

fun generateCenteredLevelBitmap(
    percentage: Int,
    activeColor: Color,
    dimColor: Color,
    bgColor: Color,
    targetWidthPx: Int,
    targetHeightPx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(targetWidthPx, targetHeightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = targetWidthPx.toFloat()
    val h = targetHeightPx.toFloat()

    val bgPaint = Paint().apply {
        isAntiAlias = true
        color = bgColor.toArgb()
        style = Paint.Style.FILL
    }
    val cornerRadiusPx = 54f
    canvas.drawRoundRect(0f, 0f, w, h, cornerRadiusPx, cornerRadiusPx, bgPaint)

    val rows = 5
    val cellSize = h / (rows + 1.6f)
    val paddingX = cellSize * 0.9f

    val availW = w - (paddingX * 2f)
    val columns = floor(availW / cellSize).toInt().coerceAtLeast(12)

    val gridW = columns * cellSize
    val gridH = rows * cellSize

    val startX = (w - gridW) / 2f
    val startY = (h - gridH) / 2f
    val dotRadius = cellSize * 0.32f

    val activePaint = Paint().apply {
        isAntiAlias = true
        color = activeColor.toArgb()
        style = Paint.Style.FILL
    }

    val dimPaint = Paint().apply {
        isAntiAlias = true
        color = dimColor.toArgb()
        style = Paint.Style.FILL
    }

    val totalDots = columns * rows
    val activeDotsCount = (percentage.coerceIn(0, 100) * totalDots) / 100
    val emptyDotsCount = totalDots - activeDotsCount

    for (i in 0 until totalDots) {
        val r = i / columns
        val c = i % columns

        val cx = startX + c * cellSize + cellSize / 2f
        val cy = startY + r * cellSize + cellSize / 2f

        val paint = if (i < emptyDotsCount) dimPaint else activePaint
        canvas.drawCircle(cx, cy, dotRadius, paint)
    }

    return bitmap
}

fun generateFivePillGaugeBitmap(
    percentage: Int,
    accentColor: Color,
    dimColor: Color,
    containerBgColor: Color,
    widthPx: Int,
    heightPx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val enclosurePaint = Paint().apply {
        isAntiAlias = true
        color = containerBgColor.toArgb()
        style = Paint.Style.FILL
    }
    val cornerRadius = heightPx * 0.28f
    val enclosureRect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
    canvas.drawRoundRect(enclosureRect, cornerRadius, cornerRadius, enclosurePaint)

    val paddingX = widthPx * 0.07f
    val paddingY = heightPx * 0.14f
    val innerW = widthPx - (paddingX * 2f)
    val innerH = heightPx - (paddingY * 2f)

    val totalBars = 5
    val spacing = innerW * 0.05f
    val barWidth = (innerW - (spacing * (totalBars - 1))) / totalBars
    val barCornerRadius = barWidth * 0.35f

    val dimPaint = Paint().apply {
        isAntiAlias = true
        color = dimColor.toArgb()
        style = Paint.Style.FILL
    }

    val activePaint = Paint().apply {
        isAntiAlias = true
        color = accentColor.toArgb()
        style = Paint.Style.FILL
    }

    for (i in 0 until totalBars) {
        val left = paddingX + i * (barWidth + spacing)
        val right = left + barWidth
        val top = paddingY
        val bottom = heightPx - paddingY

        val barRect = RectF(left, top, right, bottom)
        canvas.drawRoundRect(barRect, barCornerRadius, barCornerRadius, dimPaint)

        val barPct = (percentage - (i * 20)).coerceIn(0, 20)
        val subSteps = barPct / 5
        val fillRatio = subSteps * 0.25f

        if (fillRatio > 0f) {
            val activeHeight = innerH * fillRatio
            val activeTop = bottom - activeHeight
            val activeRect = RectF(left, activeTop, right, bottom)
            canvas.drawRoundRect(activeRect, barCornerRadius, barCornerRadius, activePaint)
        }
    }

    return bitmap
}

fun generatePixelHeartBitmap(
    percentage: Int,
    accentColor: Color,
    dimColor: Color,
    widthPx: Int = 280,
    heightPx: Int = 280
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

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

    val activePixelsCount = ((percentage.coerceIn(0, 100) / 100f) * totalHeartPixels).toInt()

    val cellSize = minOf(widthPx.toFloat() / cols, heightPx.toFloat() / rows)
    val dotSize = cellSize * 0.84f
    val gap = (cellSize - dotSize) / 2f
    val cornerRadius = dotSize * 0.28f

    val offsetX = (widthPx - (cols * cellSize)) / 2f
    val offsetY = (heightPx - (rows * cellSize)) / 2f

    val activePaint = Paint().apply {
        isAntiAlias = true
        color = accentColor.toArgb()
        style = Paint.Style.FILL
    }

    val dimPaint = Paint().apply {
        isAntiAlias = true
        color = dimColor.toArgb()
        style = Paint.Style.FILL
    }

    var currentPixelIndex = 0

    for (r in rows - 1 downTo 0) {
        for (c in 0 until cols) {
            if (heartGrid[r][c] == 1) {
                val left = offsetX + c * cellSize + gap
                val top = offsetY + r * cellSize + gap
                val right = left + dotSize
                val bottom = top + dotSize

                val rect = RectF(left, top, right, bottom)
                val paint = if (currentPixelIndex < activePixelsCount) activePaint else dimPaint

                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
                currentPixelIndex++
            }
        }
    }

    return bitmap
}

fun generateWavyLightningBoltBitmap(
    percentage: Int,
    accentColor: Color,
    dimColor: Color,
    bgColor: Color,
    widthPx: Int,
    heightPx: Int,
    isWide: Boolean
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = widthPx.toFloat()
    val h = heightPx.toFloat()

    val cornerRadiusPx = 44f
    val cardRect = RectF(0f, 0f, w, h)
    val bgPaint = Paint().apply {
        isAntiAlias = true
        color = bgColor.toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadiusPx, cornerRadiusPx, bgPaint)

    canvas.save()
    val cardClipPath = Path().apply {
        addRoundRect(cardRect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
    }
    canvas.clipPath(cardClipPath)

    val centerX = if (isWide) w - 150f else w / 2f

    val boltPath = Path().apply {
        moveTo(centerX - 48f, -36f)
        lineTo(centerX + 115f, -36f)
        lineTo(centerX - 12f, 126f)
        lineTo(centerX + 145f, 126f)
        lineTo(centerX - 125f, 336f)
        lineTo(centerX - 42f, 162f)
        lineTo(centerX - 145f, 162f)
        close()
    }

    val dimPaint = Paint().apply {
        isAntiAlias = true
        color = dimColor.toArgb()
        style = Paint.Style.FILL
    }

    val activePaint = Paint().apply {
        isAntiAlias = true
        color = accentColor.toArgb()
        style = Paint.Style.FILL
    }

    canvas.drawPath(boltPath, dimPaint)

    val maxFillY = -36f
    val minFillY = 336f
    val fillProgress = percentage.coerceIn(0, 100) / 100f
    val fillY = minFillY - ((minFillY - maxFillY) * fillProgress)

    if (fillProgress > 0f) {
        val wavePath = Path().apply {
            val waveAmplitude = 10f
            val waveLength = 250f

            moveTo(-100f, fillY)

            var x = -100f
            var isUp = true
            while (x < w + 100f) {
                val nextX = x + (waveLength / 2f)
                val midX = x + ((nextX - x) / 2f)
                val controlY = if (isUp) fillY - waveAmplitude else fillY + waveAmplitude

                quadTo(midX, controlY, nextX, fillY)
                x = nextX
                isUp = !isUp
            }

            lineTo(w + 100f, h + 100f)
            lineTo(-100f, h + 100f)
            close()
        }

        canvas.save()
        canvas.clipPath(wavePath)
        canvas.drawPath(boltPath, activePaint)
        canvas.restore()
    }

    canvas.restore()
    return bitmap
}

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
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = widthPx.toFloat()
    val h = heightPx.toFloat()
    val cx = w / 2f
    val cy = h / 2f

    val bgPaint = Paint().apply {
        isAntiAlias = true
        color = bgColor.toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, w / 2f, bgPaint)

    val ringStrokeWidth = 22f * scale
    val margin = 20f * scale
    val arcRadius = (w / 2f) - margin - (ringStrokeWidth / 2f)
    val arcRect = RectF(
        cx - arcRadius,
        cy - arcRadius,
        cx + arcRadius,
        cy + arcRadius
    )

    val trackPaint = Paint().apply {
        isAntiAlias = true
        color = dimColor.toArgb()
        style = Paint.Style.STROKE
        setStrokeWidth(ringStrokeWidth)
        strokeCap = Paint.Cap.ROUND
    }

    val activePaint = Paint().apply {
        isAntiAlias = true
        color = accentColor.toArgb()
        style = Paint.Style.STROKE
        setStrokeWidth(ringStrokeWidth)
        strokeCap = Paint.Cap.ROUND
    }

    canvas.drawArc(arcRect, 0f, 360f, false, trackPaint)

    val fillProgress = percentage.coerceIn(0, 100) / 100f
    val sweepAngle = fillProgress * 360f

    if (sweepAngle > 0f) {
        canvas.drawArc(arcRect, -90f, sweepAngle, false, activePaint)
    }

    val tickColor = if (isLight) Color(0x20000000).toArgb() else Color(0x28FFFFFF).toArgb()
    val tickPaint = Paint().apply {
        isAntiAlias = true
        color = tickColor
        setStrokeWidth(2f * scale)
        style = Paint.Style.STROKE
    }
    val tickInnerR = arcRadius - (ringStrokeWidth / 2f) - (6f * scale)
    val tickOuterR = tickInnerR - (10f * scale)

    for (i in 0 until 60) {
        val angleDeg = i * 6f
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val startX = cx + (tickInnerR * Math.cos(angleRad)).toFloat()
        val startY = cy + (tickInnerR * Math.sin(angleRad)).toFloat()
        val endX = cx + (tickOuterR * Math.cos(angleRad)).toFloat()
        val endY = cy + (tickOuterR * Math.sin(angleRad)).toFloat()
        canvas.drawLine(startX, startY, endX, endY, tickPaint)
    }

    val iconY = cy - (42f * scale)
    val batW = 24f * scale
    val batH = 38f * scale

    val shellPaint = Paint().apply {
        isAntiAlias = true
        color = iconColor.toArgb()
        style = Paint.Style.STROKE
        setStrokeWidth(3f * scale)
    }

    val fillColor = if (percentage <= 20 && !isCharging) {
        Color(0xFFFF3B30).toArgb()
    } else {
        accentColor.toArgb()
    }

    val fillPaint = Paint().apply {
        isAntiAlias = true
        color = fillColor
        style = Paint.Style.FILL
    }

    val bodyRect = RectF(
        cx - (batW / 2f),
        iconY - (batH / 2f) + (3f * scale),
        cx + (batW / 2f),
        iconY + (batH / 2f)
    )
    val capRect = RectF(
        cx - (5f * scale),
        iconY - (batH / 2f) - (3f * scale),
        cx + (5f * scale),
        iconY - (batH / 2f) + (3f * scale)
    )

    canvas.drawRoundRect(capRect, 2f * scale, 2f * scale, fillPaint)
    canvas.drawRoundRect(bodyRect, 6f * scale, 6f * scale, shellPaint)

    val innerMargin = 3.5f * scale
    val maxFillH = batH - (innerMargin * 2f)
    val currentFillH = (maxFillH * fillProgress).coerceAtLeast(2f * scale)

    val fillRect = RectF(
        bodyRect.left + innerMargin,
        bodyRect.bottom - innerMargin - currentFillH,
        bodyRect.right - innerMargin,
        bodyRect.bottom - innerMargin
    )
    canvas.drawRoundRect(fillRect, 3f * scale, 3f * scale, fillPaint)

    if (isCharging) {
        val boltColor = if (isLight) Color(0xFF000000).toArgb() else Color(0xFFFFFFFF).toArgb()
        val boltPaint = Paint().apply {
            isAntiAlias = true
            color = boltColor
            style = Paint.Style.FILL
        }

        val boltPath = Path().apply {
            moveTo(cx - (2f * scale), iconY - (11f * scale))
            lineTo(cx + (6f * scale), iconY - (11f * scale))
            lineTo(cx - (1f * scale), iconY - (1f * scale))
            lineTo(cx + (5f * scale), iconY - (1f * scale))
            lineTo(cx - (5f * scale), iconY + (11f * scale))
            lineTo(cx - (1f * scale), iconY + (1f * scale))
            lineTo(cx - (5f * scale), iconY + (1f * scale))
            close()
        }
        canvas.drawPath(boltPath, boltPaint)
    }

    return bitmap
}

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

    val bodyRadius = bodyW * 0.20f
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

    val bodyRadius = bodyH * 0.20f
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

    val activeSegmentsCount = (percentage.coerceIn(0, 100) / 100f * totalSegments).toInt()

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

// ============================================================================
// GLANCE COMPOSABLES (PRESERVED FOR DASHBOARD PREVIEWS)
// ============================================================================

@Composable
fun MinimalBatteryTile(
    percentage: Int,
    isCharging: Boolean,
    config: SlateWidgetConfig
) {
    val size = LocalSize.current
    val minDimensionDp = if (size.width < size.height) size.width else size.height
    val scale = (minDimensionDp.value / 152f).coerceAtLeast(0.6f)

    val isLight = config.themeMode == "LIGHT"
    val rawBgColor = Color(config.backgroundColorHex)
    val finalBgColor = rawBgColor.copy(alpha = config.opacity)

    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary else SlateColors.TextDarkPrimary
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary else SlateColors.TextDarkSecondary
    val trackColor = if (isLight) Color(0x1F000000) else Color(0x1FAFAFAF)
    val accentColor = Color(config.accentColorHex)

    val bgBitmap = createRoundedBackgroundBitmap(
        color = finalBgColor,
        widthPx = (300 * scale).toInt(),
        heightPx = (300 * scale).toInt(),
        cornerRadiusPx = 40f * scale
    )

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .size(minDimensionDp)
                .background(ImageProvider(bgBitmap))
                .padding((16 * scale).dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "BATTERY",
                        style = TextStyle(
                            color = ColorProvider(secondaryTextColor),
                            fontSize = (11 * scale).sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    if (isCharging) {
                        Text(
                            text = "CHARGING",
                            style = TextStyle(
                                color = ColorProvider(accentColor),
                                fontSize = (10 * scale).sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                Text(
                    text = "$percentage%",
                    style = TextStyle(
                        color = ColorProvider(primaryTextColor),
                        fontSize = (44 * scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                LinearProgressIndicator(
                    progress = (percentage.coerceIn(0, 100) / 100f),
                    modifier = GlanceModifier.fillMaxWidth().height((6 * scale).dp),
                    color = ColorProvider(accentColor),
                    backgroundColor = ColorProvider(trackColor)
                )
            }
        }
    }
}

@Composable
fun MultiDeviceBatteryCard(
    phonePct: Int,
    isCharging: Boolean,
    tempText: String,
    voltageText: String,
    config: SlateWidgetConfig
) {
    val isLight = config.themeMode == "LIGHT"
    val rawBgColor = Color(config.backgroundColorHex)
    val finalBgColor = rawBgColor.copy(alpha = config.opacity)

    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary else SlateColors.TextDarkPrimary
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary else SlateColors.TextDarkSecondary
    val trackColor = if (isLight) Color(0x1F000000) else Color(0x1FAFAFAF)
    val accentColor = Color(config.accentColorHex)

    val bgBitmap = createRoundedBackgroundBitmap(
        color = finalBgColor,
        widthPx = 900,
        heightPx = 500,
        cornerRadiusPx = 60f
    )

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(152.dp)
                .background(ImageProvider(bgBitmap))
                .padding(16.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                DeviceBatteryRow(
                    name = if (isCharging) "PHONE • CHARGING" else "PHONE LEVEL",
                    pctText = "$phonePct%",
                    pctRatio = phonePct / 100f,
                    accentColor = accentColor,
                    textColor = primaryTextColor,
                    subTextColor = secondaryTextColor,
                    trackColor = trackColor
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                DeviceBatteryRow(
                    name = "TEMPERATURE",
                    pctText = tempText,
                    pctRatio = 0.45f,
                    accentColor = accentColor,
                    textColor = primaryTextColor,
                    subTextColor = secondaryTextColor,
                    trackColor = trackColor
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                DeviceBatteryRow(
                    name = "VOLTAGE",
                    pctText = voltageText,
                    pctRatio = 0.82f,
                    accentColor = accentColor,
                    textColor = primaryTextColor,
                    subTextColor = secondaryTextColor,
                    trackColor = trackColor
                )
            }
        }
    }
}

@Composable
private fun DeviceBatteryRow(
    name: String,
    pctText: String,
    pctRatio: Float,
    accentColor: Color,
    textColor: Color,
    subTextColor: Color,
    trackColor: Color
) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = name,
                style = TextStyle(
                    color = ColorProvider(subTextColor),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = pctText,
                style = TextStyle(
                    color = ColorProvider(textColor),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        LinearProgressIndicator(
            progress = pctRatio.coerceIn(0f, 1f),
            modifier = GlanceModifier.fillMaxWidth().height(5.dp),
            color = ColorProvider(accentColor),
            backgroundColor = ColorProvider(trackColor)
        )
    }
}

@Composable
fun HorizontalBatteryStrip(
    percentage: Int,
    isCharging: Boolean,
    config: SlateWidgetConfig
) {
    val size = LocalSize.current
    val isLight = config.themeMode == "LIGHT"
    val rawBgColor = Color(config.backgroundColorHex)
    val finalBgColor = rawBgColor.copy(alpha = config.opacity)

    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary else SlateColors.TextDarkPrimary
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary else SlateColors.TextDarkSecondary
    val trackColor = if (isLight) Color(0x1F000000) else Color(0x2BFFFFFF)
    val accentColor = Color(config.accentColorHex)

    val canvasH = 240
    val aspect = (size.width.value / 70f).coerceAtLeast(1.0f)
    val canvasW = (canvasH * aspect).toInt()

    val compositeBitmap = generateHorizontalStripBitmap(
        percentage = percentage,
        accentColor = accentColor,
        trackColor = trackColor,
        bgColor = finalBgColor,
        widthPx = canvasW,
        heightPx = canvasH
    )

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(70.dp)
                .background(ImageProvider(compositeBitmap))
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "BATTERY",
                    style = TextStyle(
                        color = ColorProvider(secondaryTextColor),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                if (isCharging) {
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = "• CHARGING",
                        style = TextStyle(
                            color = ColorProvider(accentColor),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                Text(
                    text = "$percentage%",
                    style = TextStyle(
                        color = ColorProvider(primaryTextColor),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun ArcGaugeBatteryTile(
    percentage: Int,
    isCharging: Boolean,
    config: SlateWidgetConfig
) {
    val size = LocalSize.current
    val minDimensionDp = if (size.width < size.height) size.width else size.height
    val scale = (minDimensionDp.value / 152f).coerceAtLeast(0.6f)

    val isLight = config.themeMode == "LIGHT"
    val rawBgColor = Color(config.backgroundColorHex)
    val finalBgColor = rawBgColor.copy(alpha = config.opacity)

    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary else SlateColors.TextDarkPrimary
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary else SlateColors.TextDarkSecondary

    val accentColor = Color(config.accentColorHex)
    val trackColor = if (isLight) Color(0x1F000000) else accentColor.copy(alpha = 0.2f)

    val gaugeBitmap = generateArcGaugeBitmap(
        percentage = percentage,
        accentColor = accentColor,
        trackColor = trackColor,
        widthPx = (330 * scale).toInt(),
        heightPx = (165 * scale).toInt()
    )

    val bgBitmap = createRoundedBackgroundBitmap(
        color = finalBgColor,
        widthPx = (300 * scale).toInt(),
        heightPx = (300 * scale).toInt(),
        cornerRadiusPx = 40f * scale
    )

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .size(minDimensionDp)
                .background(ImageProvider(bgBitmap))
                .padding((14 * scale).dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "BATTERY",
                        style = TextStyle(
                            color = ColorProvider(secondaryTextColor),
                            fontSize = (11 * scale).sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    if (isCharging) {
                        Text(
                            text = "CHARGING",
                            style = TextStyle(
                                color = ColorProvider(accentColor),
                                fontSize = (10 * scale).sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                Image(
                    provider = ImageProvider(gaugeBitmap),
                    contentDescription = "Battery Arc Gauge",
                    modifier = GlanceModifier
                        .width((110 * scale).dp)
                        .height((55 * scale).dp)
                )

                Spacer(modifier = GlanceModifier.height((4 * scale).dp))

                Text(
                    text = "$percentage%",
                    style = TextStyle(
                        color = ColorProvider(primaryTextColor),
                        fontSize = (38 * scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.defaultWeight())
            }
        }
    }
}

@Composable
fun EditorialStatsBatteryTile(
    percentage: Int,
    healthText: String,
    secondaryStatText: String,
    config: SlateWidgetConfig
) {
    val size = LocalSize.current
    val minDimensionDp = if (size.width < size.height) size.width else size.height
    val scale = (minDimensionDp.value / 152f).coerceAtLeast(0.6f)

    val isLight = config.themeMode == "LIGHT"
    val rawBgColor = Color(config.backgroundColorHex)
    val finalBgColor = rawBgColor.copy(alpha = config.opacity)

    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary else SlateColors.TextDarkPrimary
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary else SlateColors.TextDarkSecondary

    val accentColor = Color(config.accentColorHex)
    val trackColor = if (isLight) Color(0x1F000000) else Color(0x2EFFFFFF)

    val barBitmap = generateSegmentedBarBitmap(
        percentage = percentage,
        accentColor = accentColor,
        trackColor = trackColor,
        widthPx = (480 * scale).toInt(),
        heightPx = (72 * scale).toInt()
    )

    val bgBitmap = createRoundedBackgroundBitmap(
        color = finalBgColor,
        widthPx = (300 * scale).toInt(),
        heightPx = (300 * scale).toInt(),
        cornerRadiusPx = 40f * scale
    )

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .size(minDimensionDp)
                .background(ImageProvider(bgBitmap))
                .padding((16 * scale).dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "$percentage%",
                    style = TextStyle(
                        color = ColorProvider(primaryTextColor),
                        fontSize = (46 * scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height((6 * scale).dp))

                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Text(
                        text = "• ",
                        style = TextStyle(
                            color = ColorProvider(accentColor),
                            fontSize = (13 * scale).sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = healthText,
                        style = TextStyle(
                            color = ColorProvider(secondaryTextColor),
                            fontSize = (12 * scale).sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height((3 * scale).dp))

                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Text(
                        text = "• ",
                        style = TextStyle(
                            color = ColorProvider(secondaryTextColor),
                            fontSize = (13 * scale).sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = secondaryStatText,
                        style = TextStyle(
                            color = ColorProvider(secondaryTextColor),
                            fontSize = (12 * scale).sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                Image(
                    provider = ImageProvider(barBitmap),
                    contentDescription = "Segmented Bar",
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height((18 * scale).dp)
                )
            }
        }
    }
}

@Composable
fun DotMatrixBatteryLEDCard(
    percentage: Int,
    isCharging: Boolean,
    config: SlateWidgetConfig
) {
    val size = LocalSize.current
    val isLight = config.themeMode == "LIGHT"
    val rawBgColor = Color(config.backgroundColorHex)
    val finalBgColor = rawBgColor.copy(alpha = config.opacity)

    val activeColor = if (isLight) SlateColors.TextLightPrimary else Color.White
    val dimColor = if (isLight) Color(0x1F000000) else Color(0x1AFFFFFF)

    val cardHeightDp = 152.dp
    val canvasH = 450
    val aspect = (size.width.value / cardHeightDp.value).coerceAtLeast(1.2f)
    val canvasW = (canvasH * aspect).toInt().coerceAtLeast(540)

    val matrixBitmap = generateDotMatrixLEDBitmap(
        text = "$percentage%",
        activeColor = activeColor,
        dimColor = dimColor,
        bgColor = finalBgColor,
        targetWidthPx = canvasW,
        targetHeightPx = canvasH
    )

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(cardHeightDp)
        ) {
            Image(
                provider = ImageProvider(matrixBitmap),
                contentDescription = "Dot Matrix Battery LED Display",
                modifier = GlanceModifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun DotLevelMeterTile(
    percentage: Int,
    isCharging: Boolean,
    config: SlateWidgetConfig
) {
    val size = LocalSize.current
    val minDimensionDp = if (size.width < size.height) size.width else size.height
    val scale = (minDimensionDp.value / 152f).coerceAtLeast(0.6f)

    val isLight = config.themeMode == "LIGHT"
    val rawBgColor = Color(config.backgroundColorHex)
    val finalBgColor = rawBgColor.copy(alpha = config.opacity)

    val activeColor = Color(config.accentColorHex)
    val dimColor = if (isLight) Color(0x1F000000) else Color(0x1AFFFFFF)

    val bitmap = generateDotLevelBitmap(
        percentage = percentage,
        activeColor = activeColor,
        dimColor = dimColor,
        columns = 10,
        rows = 10,
        widthPx = (450 * scale).toInt(),
        heightPx = (450 * scale).toInt()
    )

    val bgBitmap = createRoundedBackgroundBitmap(
        color = finalBgColor,
        widthPx = (300 * scale).toInt(),
        heightPx = (300 * scale).toInt(),
        cornerRadiusPx = 40f * scale
    )

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .size(minDimensionDp)
                .background(ImageProvider(bgBitmap))
                .padding((16 * scale).dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = "100-Dot Battery Level Tile",
                modifier = GlanceModifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun DotLevelMeterCard(
    percentage: Int,
    isCharging: Boolean,
    config: SlateWidgetConfig
) {
    val size = LocalSize.current
    val isLight = config.themeMode == "LIGHT"
    val rawBgColor = Color(config.backgroundColorHex)
    val finalBgColor = rawBgColor.copy(alpha = config.opacity)

    val activeColor = Color(config.accentColorHex)
    val dimColor = if (isLight) Color(0x1F000000) else Color(0x1AFFFFFF)

    val cardHeightDp = 152.dp
    val canvasH = 450
    val aspect = (size.width.value / cardHeightDp.value).coerceAtLeast(1.2f)
    val canvasW = (canvasH * aspect).toInt().coerceAtLeast(540)

    val bitmap = generateCenteredLevelBitmap(
        percentage = percentage,
        activeColor = activeColor,
        dimColor = dimColor,
        bgColor = finalBgColor,
        targetWidthPx = canvasW,
        targetHeightPx = canvasH
    )

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(cardHeightDp)
        ) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = "Dot Battery Level Card",
                modifier = GlanceModifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun SegmentedPillBatteryTile(
    percentage: Int,
    isCharging: Boolean,
    config: SlateWidgetConfig
) {
    val size = LocalSize.current
    val minDimensionDp = if (size.width < size.height) size.width else size.height
    val scale = (minDimensionDp.value / 152f).coerceAtLeast(0.6f)

    val isLight = config.themeMode == "LIGHT"
    val rawBgColor = Color(config.backgroundColorHex)
    val finalBgColor = rawBgColor.copy(alpha = config.opacity)

    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary else SlateColors.TextDarkPrimary
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary else SlateColors.TextDarkSecondary
    val accentColor = Color(config.accentColorHex)

    val dimColor = if (isLight) Color(0x1F000000) else Color(0x26FFFFFF)
    val containerBgColor = if (isLight) Color(0x0F000000) else Color(0x1AFFFFFF)

    val gaugeBitmap = generateFivePillGaugeBitmap(
        percentage = percentage,
        accentColor = accentColor,
        dimColor = dimColor,
        containerBgColor = containerBgColor,
        widthPx = (500 * scale).toInt(),
        heightPx = (215 * scale).toInt()
    )

    val bgBitmap = createRoundedBackgroundBitmap(
        color = finalBgColor,
        widthPx = (300 * scale).toInt(),
        heightPx = (300 * scale).toInt(),
        cornerRadiusPx = 40f * scale
    )

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .size(minDimensionDp)
                .background(ImageProvider(bgBitmap))
                .padding((14 * scale).dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    if (isCharging) {
                        Text(
                            text = "⚡ ",
                            style = TextStyle(
                                color = ColorProvider(accentColor),
                                fontSize = (18 * scale).sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Text(
                        text = "$percentage%",
                        style = TextStyle(
                            color = ColorProvider(primaryTextColor),
                            fontSize = (26 * scale).sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                Image(
                    provider = ImageProvider(gaugeBitmap),
                    contentDescription = "5-Bar Pill Battery Gauge",
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height((60 * scale).dp)
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                Text(
                    text = if (isCharging) "Charging" else "~ Discharging",
                    style = TextStyle(
                        color = ColorProvider(secondaryTextColor),
                        fontSize = (11 * scale).sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
fun PixelHeartBatteryTile(
    percentage: Int,
    isCharging: Boolean,
    config: SlateWidgetConfig
) {
    val size = LocalSize.current
    val minDimensionDp = if (size.width < size.height) size.width else size.height
    val scale = (minDimensionDp.value / 152f).coerceAtLeast(0.5f)

    val isLight = config.themeMode == "LIGHT"
    val rawBgColor = Color(config.backgroundColorHex)
    val finalBgColor = rawBgColor.copy(alpha = config.opacity)

    val accentColor = Color(config.accentColorHex)
    val dimColor = if (isLight) Color(0x2B000000) else Color(0x2BFFFFFF)

    val canvasSize = (280 * scale).toInt().coerceAtLeast(140)

    val heartBitmap = generatePixelHeartBitmap(
        percentage = percentage,
        accentColor = accentColor,
        dimColor = dimColor,
        widthPx = canvasSize,
        heightPx = canvasSize
    )

    val bgBitmap = createRoundedBackgroundBitmap(
        color = finalBgColor,
        widthPx = (300 * scale).toInt(),
        heightPx = (300 * scale).toInt(),
        cornerRadiusPx = 40f * scale
    )

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .size(minDimensionDp)
                .background(ImageProvider(bgBitmap))
                .padding((12 * scale).dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(heartBitmap),
                contentDescription = "Pixel Heart Battery Display",
                modifier = GlanceModifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun LightningBoltBatteryTile(
    percentage: Int,
    isCharging: Boolean,
    tempText: String,
    voltageText: String,
    config: SlateWidgetConfig
) {
    val size = LocalSize.current
    val isWide = size.width >= 200.dp
    val minDimensionDp = if (size.width < size.height) size.width else size.height

    val isLight = config.themeMode == "LIGHT"
    val rawBgColor = Color(config.backgroundColorHex)
    val finalBgColor = rawBgColor.copy(alpha = config.opacity)

    val accentColor = Color(config.accentColorHex)
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary else SlateColors.TextDarkPrimary
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary else SlateColors.TextDarkSecondary
    val dimColor = if (isLight) Color(0x2B000000) else Color(0x2BFFFFFF)

    val heightPx = 300
    val aspect = (size.width.value / 152f).coerceAtLeast(1.0f)
    val canvasW = (heightPx * aspect).toInt()
    val canvasH = heightPx

    val compositeBitmap = generateWavyLightningBoltBitmap(
        percentage = percentage,
        accentColor = accentColor,
        dimColor = dimColor,
        bgColor = finalBgColor,
        widthPx = canvasW,
        heightPx = canvasH,
        isWide = isWide
    )

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isWide) {
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(152.dp)
                    .background(ImageProvider(compositeBitmap))
                    .padding(horizontal = 22.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxHeight()
                            .defaultWeight(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            text = "$percentage% / $tempText",
                            style = TextStyle(
                                color = ColorProvider(primaryTextColor),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        Text(
                            text = if (isCharging) "Charging • Connected" else "Discharging • $voltageText",
                            style = TextStyle(
                                color = ColorProvider(secondaryTextColor),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )

                        Spacer(modifier = GlanceModifier.defaultWeight())

                        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                            Text(
                                text = "⚡ ",
                                style = TextStyle(
                                    color = ColorProvider(accentColor),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = if (isCharging) "Fast Charging Active" else "Battery Normal",
                                style = TextStyle(
                                    color = ColorProvider(primaryTextColor),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())
                }
            }
        } else {
            Box(
                modifier = GlanceModifier
                    .size(minDimensionDp)
                    .background(ImageProvider(compositeBitmap))
            ) {}
        }
    }
}

@Composable
fun CircularRingBatteryTile(
    percentage: Int,
    isCharging: Boolean,
    config: SlateWidgetConfig
) {
    val size = LocalSize.current
    val minDimensionDp = if (size.width < size.height) size.width else size.height
    val scale = (minDimensionDp.value / 152f).coerceAtLeast(0.5f)

    val percentFontSize = (28 * scale).sp
    val labelFontSize = (9 * scale).sp
    val topPadding = (18 * scale).dp

    val canvasSize = (300 * scale).toInt().coerceAtLeast(150)

    val isLight = config.themeMode == "LIGHT"
    val rawBgColor = Color(config.backgroundColorHex)
    val finalBgColor = rawBgColor.copy(alpha = config.opacity)

    val accentColor = Color(config.accentColorHex)
    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary else SlateColors.TextDarkPrimary
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary else SlateColors.TextDarkSecondary
    val dimColor = if (isLight) Color(0x1F000000) else Color(0x2BFFFFFF)

    val gaugeBitmap = generateCircularGaugeBitmap(
        percentage = percentage,
        isCharging = isCharging,
        accentColor = accentColor,
        dimColor = dimColor,
        iconColor = primaryTextColor,
        bgColor = finalBgColor,
        isLight = isLight,
        scale = scale,
        widthPx = canvasSize,
        heightPx = canvasSize
    )

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .size(minDimensionDp)
                .background(ImageProvider(gaugeBitmap)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = GlanceModifier.padding(top = topPadding),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "$percentage%",
                    style = TextStyle(
                        color = ColorProvider(primaryTextColor),
                        fontSize = percentFontSize,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height((1 * scale).dp))

                Text(
                    text = if (isCharging) "CHARGING" else "DISCHARGING",
                    style = TextStyle(
                        color = ColorProvider(secondaryTextColor),
                        fontSize = labelFontSize,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun VerticalBatteryPillTile(
    percentage: Int,
    isCharging: Boolean,
    config: SlateWidgetConfig
) {
    val size = LocalSize.current

    val canvasW = (size.width.value * 3f).toInt().coerceAtLeast(300)
    val canvasH = (size.height.value * 3f).toInt().coerceAtLeast(450)

    val pillBitmap = generateVerticalPillBitmap(
        percentage = percentage,
        isCharging = isCharging,
        config = config,
        widthPx = canvasW,
        heightPx = canvasH
    )

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(pillBitmap),
            contentDescription = "Vertical Hardware Battery Gauge",
            modifier = GlanceModifier.fillMaxSize()
        )
    }
}

@Composable
fun HorizontalBatteryPillTile(
    percentage: Int,
    isCharging: Boolean,
    config: SlateWidgetConfig
) {
    val size = LocalSize.current

    val canvasW = (size.width.value * 3f).toInt().coerceAtLeast(450)
    val canvasH = (size.height.value * 3f).toInt().coerceAtLeast(225)

    val pillBitmap = generateHorizontalPillBitmap(
        percentage = percentage,
        isCharging = isCharging,
        config = config,
        widthPx = canvasW,
        heightPx = canvasH
    )

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(pillBitmap),
            contentDescription = "Horizontal Hardware Battery Gauge",
            modifier = GlanceModifier.fillMaxSize()
        )
    }
}