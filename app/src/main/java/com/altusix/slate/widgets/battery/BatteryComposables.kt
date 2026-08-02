package com.altusix.slate.widgets.battery

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
import kotlin.math.roundToInt

// ============================================================================
// HELPER: Canvas Bitmap Background (Aspect-Matched to LocalSize)
// ============================================================================
private fun createRoundedBackgroundBitmap(
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
// WIDGET #1: MINIMAL 2x2 BATTERY TILE (Square Centered)
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
                .size(minDimensionDp) // Constrained to perfect square tile
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

// ============================================================================
// WIDGET #2: MULTI-DEVICE CARD (4x2 Locked Height)
// ============================================================================
@Composable
fun MultiDeviceBatteryCard(
    phonePct: Int,
    isCharging: Boolean,
    tempText: String,
    voltageText: String,
    config: SlateWidgetConfig
) {
    val size = LocalSize.current
    val isLight = config.themeMode == "LIGHT"
    val rawBgColor = Color(config.backgroundColorHex)
    val finalBgColor = rawBgColor.copy(alpha = config.opacity)

    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary else SlateColors.TextDarkPrimary
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary else SlateColors.TextDarkSecondary
    val trackColor = if (isLight) Color(0x1F000000) else Color(0x1FAFAFAF)
    val accentColor = Color(config.accentColorHex)

    // High-DPI canvas resolution (Scaled up to 900x500px for crisp QHD rendering)
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

// ============================================================================
// WIDGET #3: HORIZONTAL STRIP (4x1 Locked Height)
// ============================================================================
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
                .height(70.dp) // Locked 70dp height
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

private fun generateHorizontalStripBitmap(
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

// ============================================================================
// WIDGET #4: MINIMAL ARC GAUGE TILE (High-DPI Crisp Render)
// ============================================================================
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

    // High-DPI Canvas Resolution (scaled up by 3x density)
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

private fun generateArcGaugeBitmap(
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

// ============================================================================
// WIDGET #5: EDITORIAL STATS TILE (Square Centered)
// ============================================================================
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

    // High-DPI Segmented Bar Bitmap
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

private fun generateSegmentedBarBitmap(
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

// ============================================================================
// WIDGET #6: DOT MATRIX BATTERY LED (4x2 Locked Height)
// ============================================================================

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

    // Locked 152dp Card Height
    val cardHeightDp = 152.dp
    val canvasH = 450 // High-DPI canvas height
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

private fun generateDotMatrixLEDBitmap(
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

    // 1. Draw Outer Card Background
    val bgPaint = Paint().apply {
        isAntiAlias = true
        color = bgColor.toArgb()
        style = Paint.Style.FILL
    }
    val cornerRadiusPx = 54f
    canvas.drawRoundRect(0f, 0f, w, h, cornerRadiusPx, cornerRadiusPx, bgPaint)

    // 2. Fixed 9-Row Geometry
    val rows = 9
    val padding = 28f
    val availH = h - (padding * 2f)
    val availW = w - (padding * 2f)

    // Calculate required width for glyphs (5 dots per char + 1 dot gap)
    val glyphWidth = 5
    val glyphGap = 1
    val textWidthCols = text.length * glyphWidth + (text.length - 1) * glyphGap

    // Guarantee at least 1 column margin on left and right for 4-char strings ("100%")
    val minRequiredCols = textWidthCols + 2

    // Baseline cell size driven vertically by 9 rows
    val baseCellSize = availH / rows.toFloat()
    val initialCols = (availW / baseCellSize).roundToInt()

    // Dynamically increase column count if "100%" exceeds standard grid width
    val columns = maxOf(initialCols, minRequiredCols)

    // Recalculate exact cell size & dot radius based on dynamic column density
    val finalCellSize = minOf(availW / columns.toFloat(), availH / rows.toFloat())
    val dotRadius = finalCellSize * 0.38f

    val gridW = columns * finalCellSize
    val gridH = rows * finalCellSize

    // Center grid perfectly on both axes
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

    // 3. Draw Inactive Dim Grid
    for (r in 0 until rows) {
        for (c in 0 until columns) {
            val cx = startX + c * finalCellSize + finalCellSize / 2f
            val cy = startY + r * finalCellSize + finalCellSize / 2f
            canvas.drawCircle(cx, cy, dotRadius, dimPaint)
        }
    }

    // 4. Draw Centered Active Text (7 rows tall, startRow = 1)
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

// ============================================================================
// WIDGET #7: DOT LEVEL METER TILE (Square Centered)
// ============================================================================
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

    // Scaled High-Res Dot Matrix Bitmap
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

private fun generateDotLevelBitmap(
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

// ============================================================================
// WIDGET #8: DOT LEVEL METER CARD (4x2 Locked Height)
// ============================================================================

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

    val canvasW = (size.width.value * 3f).toInt().coerceAtLeast(300)
    val canvasH = (size.height.value * 3f).toInt().coerceAtLeast(200)
    val aspect = canvasW.toFloat() / canvasH.toFloat()

    val dynamicRows = if (aspect >= 1.0f) 6 else (6 / aspect).toInt().coerceIn(6, 16)
    val dynamicCols = if (aspect >= 1.0f) (6 * aspect).toInt().coerceIn(16, 40) else 10

    val bitmap = generateCenteredLevelBitmap(
        percentage = percentage,
        activeColor = activeColor,
        dimColor = dimColor,
        bgColor = finalBgColor,
        columns = dynamicCols,
        rows = dynamicRows,
        targetWidthPx = canvasW,
        targetHeightPx = canvasH
    )

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = "Dot Battery Level Card",
            modifier = GlanceModifier.fillMaxSize()
        )
    }
}

private fun generateCenteredLevelBitmap(
    percentage: Int,
    activeColor: Color,
    dimColor: Color,
    bgColor: Color,
    columns: Int,
    rows: Int,
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

    val paddingX = 40f
    val paddingY = 40f
    val availW = w - (paddingX * 2f)
    val availH = h - (paddingY * 2f)

    val cellW = availW / columns
    val cellH = availH / rows
    val cellSize = minOf(cellW, cellH)

    val gridW = columns * cellSize
    val gridH = rows * cellSize
    val startX = (w - gridW) / 2f
    val startY = (h - gridH) / 2f
    val dotRadius = cellSize * 0.35f

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

// ============================================================================
// WIDGET #9: 5-BAR SEGMENTED PILL BATTERY TILE (Square Centered)
// ============================================================================
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

    // High-DPI 5-Pill Gauge Bitmap
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

private fun generateFivePillGaugeBitmap(
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

// ============================================================================
// WIDGET #10: PIXEL ART HEART BATTERY TILE (Square Centered)
// ============================================================================
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

private fun generatePixelHeartBitmap(
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

// ============================================================================
// WIDGET #11: RESPONSIVE WAVY LIGHTNING BOLT (Square Centered / 4x2 Centered)
// ============================================================================
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

private fun generateWavyLightningBoltBitmap(
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
        moveTo(centerX - 48f,  -36f)
        lineTo(centerX + 115f, -36f)
        lineTo(centerX - 12f,  126f)
        lineTo(centerX + 145f, 126f)
        lineTo(centerX - 125f, 336f)
        lineTo(centerX - 42f,  162f)
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

// ============================================================================
// WIDGET #12: CIRCULAR DIAL BATTERY TILE (Square Centered)
// ============================================================================
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

private fun generateCircularGaugeBitmap(
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