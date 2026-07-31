package com.altusix.slate.widgets.battery

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
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

@Composable
fun MinimalBatteryTile(
    percentage: Int,
    isCharging: Boolean,
    config: SlateWidgetConfig
) {
    val isLight = config.themeMode == "LIGHT"

    // Resolve background fill with user opacity
    val rawBgColor = Color(config.backgroundColorHex)
    val finalBgColor = rawBgColor.copy(alpha = config.opacity)

    // Resolve Text Colors based on theme mode
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
                // Top Header Row
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
                            text = "⚡",
                            style = TextStyle(
                                color = ColorProvider(accentColor),
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Big Minimal Percentage Display
                Text(
                    text = "$percentage%",
                    style = TextStyle(
                        color = ColorProvider(primaryTextColor),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Clean Progress Indicator
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