package com.altusix.slate.widgets.battery

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.altusix.slate.core.theme.SlateColors
import com.altusix.slate.core.theme.SlateShapes
import com.altusix.slate.data.local.SlateWidgetConfig

// ============================================================================
// WIDGET #1: MINIMAL 2x2 BATTERY TILE
// ============================================================================
@Composable
fun MinimalBatteryTile(
    percentage: Int,
    isCharging: Boolean,
    config: SlateWidgetConfig
) {
    val isLight = config.themeMode == "LIGHT"
    val rawBgColor = Color(config.backgroundColorHex)
    val finalBgColor = rawBgColor.copy(alpha = config.opacity)

    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary else SlateColors.TextDarkPrimary
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary else SlateColors.TextDarkSecondary
    val trackColor = if (isLight) Color(0x1F000000) else Color(0x1FAFAFAF)
    val accentColor = Color(config.accentColorHex)

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .width(152.dp)
                .height(152.dp)
                .cornerRadius(SlateShapes.CornerLarge)
                .background(finalBgColor)
                .padding(16.dp)
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
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    if (isCharging) {
                        Text(
                            text = "CHARGING",
                            style = TextStyle(
                                color = ColorProvider(accentColor),
                                fontSize = 10.sp,
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
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                LinearProgressIndicator(
                    progress = (percentage.coerceIn(0, 100) / 100f),
                    modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                    color = ColorProvider(accentColor),
                    backgroundColor = ColorProvider(trackColor)
                )
            }
        }
    }
}

// ============================================================================
// WIDGET #2: MULTI-DEVICE CARD (4x2)
// ============================================================================
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

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(180.dp)
                .cornerRadius(SlateShapes.CornerLarge)
                .background(finalBgColor)
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

                Spacer(modifier = GlanceModifier.height(12.dp))

                DeviceBatteryRow(
                    name = "TEMPERATURE",
                    pctText = tempText,
                    pctRatio = 0.45f,
                    accentColor = accentColor,
                    textColor = primaryTextColor,
                    subTextColor = secondaryTextColor,
                    trackColor = trackColor
                )

                Spacer(modifier = GlanceModifier.height(12.dp))

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

        Spacer(modifier = GlanceModifier.height(5.dp))

        LinearProgressIndicator(
            progress = pctRatio.coerceIn(0f, 1f),
            modifier = GlanceModifier.fillMaxWidth().height(5.dp),
            color = ColorProvider(accentColor),
            backgroundColor = ColorProvider(trackColor)
        )
    }
}

// ============================================================================
// WIDGET #3: HORIZONTAL STRIP (4x1)
// ============================================================================
@Composable
fun HorizontalBatteryStrip(
    percentage: Int,
    isCharging: Boolean,
    config: SlateWidgetConfig
) {
    val isLight = config.themeMode == "LIGHT"
    val rawBgColor = Color(config.backgroundColorHex)
    val finalBgColor = rawBgColor.copy(alpha = config.opacity)

    val primaryTextColor = if (isLight) SlateColors.TextLightPrimary else SlateColors.TextDarkPrimary
    val secondaryTextColor = if (isLight) SlateColors.TextLightSecondary else SlateColors.TextDarkSecondary
    val trackColor = if (isLight) Color(0x1F000000) else Color(0x1FAFAFAF)
    val accentColor = Color(config.accentColorHex)

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(64.dp)
                .cornerRadius(SlateShapes.CornerMedium)
                .background(finalBgColor)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            text = "BATTERY",
                            style = TextStyle(
                                color = ColorProvider(secondaryTextColor),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (isCharging) {
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Text(
                                text = "• CHARGING",
                                style = TextStyle(
                                    color = ColorProvider(accentColor),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "$percentage%",
                            style = TextStyle(
                                color = ColorProvider(primaryTextColor),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = (percentage.coerceIn(0, 100) / 100f),
                        modifier = GlanceModifier.fillMaxWidth().height(5.dp),
                        color = ColorProvider(accentColor),
                        backgroundColor = ColorProvider(trackColor)
                    )
                }
            }
        }
    }
}

// ============================================================================
// WIDGET #4: MINIMAL ARC GAUGE TILE (2x2)
// ============================================================================
@Composable
fun ArcGaugeBatteryTile(
    percentage: Int,
    isCharging: Boolean,
    config: SlateWidgetConfig
) {
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
        trackColor = trackColor
    )

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .width(152.dp)
                .height(152.dp)
                .cornerRadius(SlateShapes.CornerLarge)
                .background(finalBgColor)
                .padding(14.dp)
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
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    if (isCharging) {
                        Text(
                            text = "CHARGING",
                            style = TextStyle(
                                color = ColorProvider(accentColor),
                                fontSize = 10.sp,
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
                        .width(110.dp)
                        .height(55.dp)
                )

                Spacer(modifier = GlanceModifier.height(4.dp))

                Text(
                    text = "$percentage%",
                    style = TextStyle(
                        color = ColorProvider(primaryTextColor),
                        fontSize = 38.sp,
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
    widthPx: Int = 220,
    heightPx: Int = 110
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
        this.strokeWidth = strokeWidth
        strokeCap = Paint.Cap.BUTT
        color = trackColor.toArgb()
    }

    val activePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
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
// WIDGET #5: EDITORIAL STATS TILE (2x2)
// ============================================================================
@Composable
fun EditorialStatsBatteryTile(
    percentage: Int,
    healthText: String,
    secondaryStatText: String,
    config: SlateWidgetConfig
) {
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
        trackColor = trackColor
    )

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .width(152.dp)
                .height(152.dp)
                .cornerRadius(SlateShapes.CornerLarge)
                .background(finalBgColor)
                .padding(16.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "$percentage%",
                    style = TextStyle(
                        color = ColorProvider(primaryTextColor),
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height(6.dp))

                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Text(
                        text = "• ",
                        style = TextStyle(
                            color = ColorProvider(accentColor),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = healthText,
                        style = TextStyle(
                            color = ColorProvider(secondaryTextColor),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(3.dp))

                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Text(
                        text = "• ",
                        style = TextStyle(
                            color = ColorProvider(secondaryTextColor),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = secondaryStatText,
                        style = TextStyle(
                            color = ColorProvider(secondaryTextColor),
                            fontSize = 12.sp,
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
                        .height(18.dp)
                )
            }
        }
    }
}

private fun generateSegmentedBarBitmap(
    percentage: Int,
    accentColor: Color,
    trackColor: Color,
    widthPx: Int = 240,
    heightPx: Int = 36,
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
        canvas.drawRoundRect(rect, 4f, 4f, paint)
    }

    return bitmap
}



// ============================================================================
// WIDGET #6: DOT MATRIX BATTERY LED (4x2 Wide - 9 Rows Tight Wrap)
// ============================================================================

@Composable
fun DotMatrixBatteryLEDCard(
    percentage: Int,
    isCharging: Boolean,
    config: SlateWidgetConfig
) {
    val isLight = config.themeMode == "LIGHT"
    val rawBgColor = Color(config.backgroundColorHex)
    val finalBgColor = rawBgColor.copy(alpha = config.opacity)

    val activeColor = if (isLight) SlateColors.TextLightPrimary else Color.White
    val dimColor = if (isLight) Color(0x1F000000) else Color(0x1AFFFFFF)

    val matrixBitmap = generateDotMatrixLEDBitmap(
        text = "$percentage%",
        activeColor = activeColor,
        dimColor = dimColor,
        bgColor = finalBgColor,
        columns = 25,
        rows = 9
    )

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(matrixBitmap),
            contentDescription = "Dot Matrix Battery LED Display",
            modifier = GlanceModifier.fillMaxSize()
        )
    }
}

private fun generateDotMatrixLEDBitmap(
    text: String,
    activeColor: Color,
    dimColor: Color,
    bgColor: Color,
    columns: Int = 25,
    rows: Int = 9
): Bitmap {
    val cellSize = 20f
    val marginX = 24f
    val marginY = 20f

    val gridW = columns * cellSize
    val gridH = rows * cellSize

    val canvasW = (gridW + marginX * 2f).toInt()
    val canvasH = (gridH + marginY * 2f).toInt()

    val bitmap = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val dotRadius = cellSize * 0.35f
    val startX = marginX
    val startY = marginY

    // 1. Draw Tight Dark Background Card
    val bgPaint = Paint().apply {
        isAntiAlias = true
        color = bgColor.toArgb()
        style = Paint.Style.FILL
    }
    val cornerRadiusPx = 44f
    canvas.drawRoundRect(
        0f, 0f, canvasW.toFloat(), canvasH.toFloat(),
        cornerRadiusPx, cornerRadiusPx, bgPaint
    )

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

    // 2. Draw Background Dot Grid Across All 9 Rows
    for (r in 0 until rows) {
        for (c in 0 until columns) {
            val cx = startX + c * cellSize + cellSize / 2f
            val cy = startY + r * cellSize + cellSize / 2f
            canvas.drawCircle(cx, cy, dotRadius, dimPaint)
        }
    }

    // 3. Clean 5x7 Dot Typography Map
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

    // Center digits vertically (startRow = 1 leaves 1 row top & 1 row bottom)
    val startRow = (rows - 7) / 2
    val textWidthCols = text.length * 5 + (text.length - 1) * 1
    var startCol = (columns - textWidthCols) / 2

    text.forEach { char ->
        val glyph = fontMap[char]
        if (glyph != null && startCol + 5 <= columns) {
            for (r in 0..6) {
                val rowBits = glyph[r]
                for (bit in 0..4) {
                    if ((rowBits and (1 shl (4 - bit))) != 0) {
                        val c = startCol + bit
                        val targetRow = startRow + r
                        val cx = startX + c * cellSize + cellSize / 2f
                        val cy = startY + targetRow * cellSize + cellSize / 2f
                        canvas.drawCircle(cx, cy, dotRadius, activePaint)
                    }
                }
            }
            startCol += 6
        }
    }

    return bitmap
}


// ============================================================================
// WIDGET #7: DOT LEVEL METER TILE
// ============================================================================

@Composable
fun DotLevelMeterTile(
    percentage: Int,
    isCharging: Boolean,
    config: SlateWidgetConfig
) {
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
        aspectRatioHeight = 250
    )

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .width(152.dp)
                .height(152.dp)
                .cornerRadius(SlateShapes.CornerLarge)
                .background(finalBgColor)
                .padding(16.dp),
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

// ============================================================================
// WIDGET #8: DOT LEVEL METER CARD
// ============================================================================

@Composable
fun DotLevelMeterCard(
    percentage: Int,
    isCharging: Boolean,
    config: SlateWidgetConfig
) {
    val isLight = config.themeMode == "LIGHT"
    val rawBgColor = Color(config.backgroundColorHex)
    val finalBgColor = rawBgColor.copy(alpha = config.opacity)

    val activeColor = Color(config.accentColorHex)
    val dimColor = if (isLight) Color(0x1F000000) else Color(0x1AFFFFFF)

    val bitmap = generateCenteredLevelBitmap(
        percentage = percentage,
        activeColor = activeColor,
        dimColor = dimColor,
        bgColor = finalBgColor,
        columns = 20,
        rows = 5
    )

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = "20x5 Dot Battery Level Card",
            modifier = GlanceModifier.fillMaxSize()
        )
    }
}

private fun generateCenteredLevelBitmap(
    percentage: Int,
    activeColor: Color,
    dimColor: Color,
    bgColor: Color,
    columns: Int = 20,
    rows: Int = 5
): Bitmap {
    val cellSize = 22f
    val marginX = 24f
    val marginY = 20f

    val gridW = columns * cellSize
    val gridH = rows * cellSize

    val canvasW = (gridW + marginX * 2f).toInt()
    val canvasH = (gridH + marginY * 2f).toInt()

    val bitmap = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Reduced multiplier (0.28f) creates distinctly larger gaps between dots
    val dotRadius = cellSize * 0.28f
    val startX = marginX
    val startY = marginY

    // 1. Draw Rounded Background Card Directly on Canvas
    val bgPaint = Paint().apply {
        isAntiAlias = true
        color = bgColor.toArgb()
        style = Paint.Style.FILL
    }
    val cornerRadiusPx = 44f
    canvas.drawRoundRect(
        0f, 0f, canvasW.toFloat(), canvasH.toFloat(),
        cornerRadiusPx, cornerRadiusPx, bgPaint
    )

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

    val totalDots = columns * rows // 100 dots
    val activeDotsCount = (percentage.coerceIn(0, 100) * totalDots) / 100
    val emptyDotsCount = totalDots - activeDotsCount // Drains top-to-bottom

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

private fun generateDotLevelBitmap(
    percentage: Int,
    activeColor: Color,
    dimColor: Color,
    columns: Int,
    rows: Int,
    aspectRatioHeight: Int
): Bitmap {
    val widthPx = columns * 25
    val heightPx = aspectRatioHeight
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

    val totalDots = columns * rows // 100 dots
    val activeDotsCount = (percentage.coerceIn(0, 100) * totalDots) / 100
    val emptyDotsCount = totalDots - activeDotsCount // Drains top-to-bottom

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

