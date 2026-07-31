package com.altusix.slate.widgets.battery

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    if (isCharging) {
                        Text(
                            text = "CHARGING",
                            style = TextStyle(
                                color = ColorProvider(accentColor),
                                fontSize = 9.sp,
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
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                LinearProgressIndicator(
                    progress = (percentage.coerceIn(0, 100) / 100f),
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .cornerRadius(SlateShapes.CapsuleRadius),
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
    watchPct: Int = 82,
    budsPct: Int = 94,
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
                    name = "PHONE",
                    pct = phonePct,
                    accentColor = accentColor,
                    textColor = primaryTextColor,
                    subTextColor = secondaryTextColor,
                    trackColor = trackColor
                )

                Spacer(modifier = GlanceModifier.height(12.dp))

                DeviceBatteryRow(
                    name = "WATCH",
                    pct = watchPct,
                    accentColor = accentColor,
                    textColor = primaryTextColor,
                    subTextColor = secondaryTextColor,
                    trackColor = trackColor
                )

                Spacer(modifier = GlanceModifier.height(12.dp))

                DeviceBatteryRow(
                    name = "BUDS",
                    pct = budsPct,
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
    pct: Int,
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
                text = "$pct%",
                style = TextStyle(
                    color = ColorProvider(textColor),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(5.dp))

        LinearProgressIndicator(
            progress = (pct.coerceIn(0, 100) / 100f),
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(5.dp)
                .cornerRadius(SlateShapes.CapsuleRadius),
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
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (isCharging) {
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Text(
                                text = "• CHARGING",
                                style = TextStyle(
                                    color = ColorProvider(accentColor),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "$percentage%",
                            style = TextStyle(
                                color = ColorProvider(primaryTextColor),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = (percentage.coerceIn(0, 100) / 100f),
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .cornerRadius(SlateShapes.CapsuleRadius),
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

    val gaugeBitmap = remember(percentage, accentColor, trackColor) {
        generateArcGaugeBitmap(
            percentage = percentage,
            accentColor = accentColor,
            trackColor = trackColor
        )
    }

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
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    if (isCharging) {
                        Text(
                            text = "CHARGING",
                            style = TextStyle(
                                color = ColorProvider(accentColor),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                Image(
                    provider = ImageProvider(gaugeBitmap),
                    contentDescription = "Battery Level Arc Gauge",
                    modifier = GlanceModifier
                        .width(110.dp)
                        .height(55.dp)
                )

                Spacer(modifier = GlanceModifier.height(4.dp))

                Text(
                    text = "$percentage%",
                    style = TextStyle(
                        color = ColorProvider(primaryTextColor),
                        fontSize = 36.sp,
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