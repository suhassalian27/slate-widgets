package com.altusix.slate.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.altusix.slate.core.theme.SlateThemeSettings

private enum class ThemePickerTarget {
    BACKGROUND, ACCENT
}

@Composable
fun ThemeScreen(
    themeSettings: SlateThemeSettings,
    onThemeChanged: (SlateThemeSettings) -> Unit
) {
    val scrollState = rememberScrollState()
    var activePickerTarget by remember { mutableStateOf<ThemePickerTarget?>(null) }

    val isLightBg = remember(themeSettings.bgHex) {
        calculateLuminance(themeSettings.bgHex) > 0.5f
    }
    val previewTextColor = if (isLightBg) Color.Black else Color.White

    val accentPresets = remember(isLightBg) {
        if (isLightBg) {
            listOf(0xFF000000L, 0xFF00E676L, 0xFF2979FFL, 0xFFFF1744L, 0xFFFF9100L, 0xFF7C4DFFL)
        } else {
            listOf(0xFFFFFFFFL, 0xFF00E676L, 0xFF2979FFL, 0xFFFF1744L, 0xFFFF9100L, 0xFF7C4DFFL)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 140.dp)
    ) {
        Text(
            text = "Theme Studio",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 1. HERO PREVIEW STAGE WITH AMBIENT GLOW
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF121215))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .blur(50.dp)
                    .background(themeSettings.accentColor.copy(alpha = 0.35f), CircleShape)
            )

            val previewBg = Color(themeSettings.bgHex).copy(alpha = themeSettings.opacity)

            Box(
                modifier = Modifier
                    .size(160.dp, 150.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(previewBg)
                    .border(
                        width = 1.dp,
                        color = if (isLightBg) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BATTERY",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = previewTextColor.copy(alpha = 0.5f),
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(themeSettings.accentColor.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "85%",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeSettings.accentColor
                            )
                        }
                    }

                    Text(
                        text = "Charging",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = previewTextColor
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(previewTextColor.copy(alpha = 0.12f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.85f)
                                .clip(CircleShape)
                                .background(themeSettings.accentColor)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. BACKGROUND STYLE SECTION
        SectionHeader(icon = Icons.Outlined.Palette, title = "Background Style")
        Spacer(modifier = Modifier.height(12.dp))

        val bgOptions = listOf(
            Triple(0xFF161618L, "Matte", Color(0xFF161618)),
            Triple(0xFF000000L, "AMOLED", Color(0xFF000000)),
            Triple(0xFFFFFFFFL, "Light", Color(0xFFF5F5F7))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            bgOptions.forEach { (hex, title, cardBg) ->
                val isSelected = themeSettings.bgHex == hex
                BackgroundCardTile(
                    title = title,
                    previewColor = cardBg,
                    isSelected = isSelected,
                    modifier = Modifier.weight(1f)
                ) {
                    var newAccent = themeSettings.accentHex
                    if (hex == 0xFFFFFFFFL && themeSettings.accentHex == 0xFFFFFFFFL) {
                        newAccent = 0xFF000000L
                    } else if (hex != 0xFFFFFFFFL && themeSettings.accentHex == 0xFF000000L) {
                        newAccent = 0xFFFFFFFFL
                    }
                    onThemeChanged(themeSettings.copy(bgHex = hex, accentHex = newAccent))
                }
            }

            val isCustomBg = bgOptions.none { it.first == themeSettings.bgHex }
            CustomBackgroundCardTile(
                isSelected = isCustomBg,
                customColor = if (isCustomBg) Color(themeSettings.bgHex) else null,
                modifier = Modifier.weight(1f)
            ) {
                activePickerTarget = ThemePickerTarget.BACKGROUND
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. ACCENT COLOR SECTION
        SectionHeader(icon = Icons.Outlined.ColorLens, title = "Accent Color")
        Spacer(modifier = Modifier.height(14.dp))

        SurfaceCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                accentPresets.forEach { hex ->
                    val isSelected = themeSettings.accentHex == hex
                    GlowSwatchCircle(
                        color = Color(hex),
                        isSelected = isSelected,
                        onClick = { onThemeChanged(themeSettings.copy(accentHex = hex)) }
                    )
                }

                val isCustomAccent = accentPresets.none { it == themeSettings.accentHex }
                CustomRainbowSwatchCircle(
                    isSelected = isCustomAccent,
                    activeColor = if (isCustomAccent) Color(themeSettings.accentHex) else null,
                    onClick = { activePickerTarget = ThemePickerTarget.ACCENT }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. OPACITY SECTION
        SectionHeader(icon = Icons.Outlined.Opacity, title = "Opacity")
        Spacer(modifier = Modifier.height(12.dp))

        SurfaceCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Surface Translucency",
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(themeSettings.accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${(themeSettings.opacity * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeSettings.accentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                ModernOpacitySlider(
                    value = themeSettings.opacity,
                    accentColor = themeSettings.accentColor,
                    onValueChange = { newOpacity ->
                        onThemeChanged(themeSettings.copy(opacity = newOpacity))
                    }
                )
            }
        }
    }

    activePickerTarget?.let { target ->
        val initialColor = if (target == ThemePickerTarget.BACKGROUND) {
            Color(themeSettings.bgHex)
        } else {
            Color(themeSettings.accentHex)
        }

        CustomColorPickerDialog(
            initialColor = initialColor,
            title = if (target == ThemePickerTarget.BACKGROUND) "Custom Background" else "Custom Accent",
            onDismiss = { activePickerTarget = null },
            onColorSelected = { color ->
                val hex = (color.toArgb().toLong() and 0xFFFFFFFFL)
                if (target == ThemePickerTarget.BACKGROUND) {
                    onThemeChanged(themeSettings.copy(bgHex = hex))
                } else {
                    onThemeChanged(themeSettings.copy(accentHex = hex))
                }
                activePickerTarget = null
            }
        )
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF8E8E93),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
private fun SurfaceCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141417))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column { content() }
    }
}

@Composable
private fun BackgroundCardTile(
    title: String,
    previewColor: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val animatedBorderColor by animateColorAsState(
        if (isSelected) Color.White else Color.White.copy(alpha = 0.08f),
        label = "tileBorder"
    )
    val animatedBg by animateColorAsState(
        if (isSelected) Color(0xFF222226) else Color(0xFF141417),
        label = "tileBg"
    )

    Column(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(animatedBg)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = animatedBorderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(previewColor)
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (title == "Light") Color.Black else Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (title == "Light") Color.White else Color.Black,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF8E8E93)
        )
    }
}

@Composable
private fun CustomBackgroundCardTile(
    isSelected: Boolean,
    customColor: Color?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val animatedBorderColor by animateColorAsState(
        if (isSelected) Color.White else Color.White.copy(alpha = 0.08f),
        label = "customTileBorder"
    )
    val animatedBg by animateColorAsState(
        if (isSelected) Color(0xFF222226) else Color(0xFF141417),
        label = "customTileBg"
    )
    val rainbowBrush = Brush.sweepGradient(
        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
    )

    Column(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(animatedBg)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = animatedBorderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(customColor ?: Color.Transparent)
                .then(
                    if (customColor == null) Modifier.background(rainbowBrush) else Modifier
                )
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        Text(
            text = "Custom",
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF8E8E93)
        )
    }
}

@Composable
private fun GlowSwatchCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "swatchScale"
    )

    Box(
        modifier = Modifier
            .size(42.dp)
            .scale(scale)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color)
                .border(0.5.dp, Color.White.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                val isLight = remember(color) { calculateLuminance(color.toArgb().toLong()) > 0.6f }
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (isLight) Color.Black else Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun CustomRainbowSwatchCircle(
    isSelected: Boolean,
    activeColor: Color?,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "customSwatchScale"
    )
    val rainbowBrush = Brush.sweepGradient(
        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
    )

    Box(
        modifier = Modifier
            .size(42.dp)
            .scale(scale)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(activeColor ?: Color.Transparent)
                .then(if (activeColor == null) Modifier.background(rainbowBrush) else Modifier)
                .border(0.5.dp, Color.White.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected && activeColor != null) {
                val isLight = remember(activeColor) { calculateLuminance(activeColor.toArgb().toLong()) > 0.6f }
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (isLight) Color.Black else Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernOpacitySlider(
    value: Float,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = 0.0f..1.0f,
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = Color.Transparent,
            inactiveTrackColor = Color.Transparent
        ),
        track = { sliderState ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1F1F24))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(sliderState.value)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(accentColor.copy(alpha = 0.4f), accentColor)
                            )
                        )
                )
            }
        },
        thumb = {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, Color(0xFF141417), CircleShape)
            )
        }
    )
}

private fun calculateLuminance(hex: Long): Float {
    val r = ((hex shr 16) and 0xFFL) / 255f
    val g = ((hex shr 8) and 0xFFL) / 255f
    val b = (hex and 0xFFL) / 255f
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

@Composable
private fun CustomColorPickerDialog(
    initialColor: Color,
    title: String,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentColor by remember { mutableStateOf(initialColor) }

    val initialHsv = FloatArray(3)
    android.graphics.Color.colorToHSV(initialColor.toArgb(), initialHsv)
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    val swatchColors = remember {
        listOf(
            0xFFFFFFFFL, 0xFFFFCDD2L, 0xFFFFE0B2L, 0xFFFFECB3L, 0xFFE8F5E9L, 0xFFE0F7FAL, 0xFFE3F2FDL, 0xFFF3E5F5L,
            0xFFE0E0E0L, 0xFFEF9A9AL, 0xFFFFCC80L, 0xFFFFE082L, 0xFFA5D6A7L, 0xFF80DEEAL, 0xFF90CAF9L, 0xFFCE93D8L,
            0xFFBDBDBDL, 0xFFE57373L, 0xFFFFB74DL, 0xFFFFD54FL, 0xFF81C784L, 0xFF4DD0E1L, 0xFF64B5F6L, 0xFFBA68C8L,
            0xFF757575L, 0xFFEF5350L, 0xFFFFA726L, 0xFFFFCA28L, 0xFF66BB6AL, 0xFF26C6DAL, 0xFF42A5F5L, 0xFFAB47BCL,
            0xFF424242L, 0xFFF44336L, 0xFFFF9800L, 0xFFFFC107L, 0xFF4CAF50L, 0xFF00BCD4L, 0xFF2196F3L, 0xFF9C27B0L,
            0xFF212121L, 0xFFD32F2FL, 0xFFF57C00L, 0xFFFFB300L, 0xFF388E3CL, 0xFF0097A7L, 0xFF1976D2L, 0xFF4A148CL,
            0xFF161618L, 0xFF121214L, 0xFF000000L, 0xFF1B5E20L, 0xFF006064L, 0xFF0D47A1L, 0xFF311B92L, 0xFF1A237EL
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C20))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(Color(0xFF121214)),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Swatches", "Spectrum").forEachIndexed { index, label ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(3.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) Color(0xFF2E2E34) else Color.Transparent)
                                .clickable { selectedTab = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(8),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(swatchColors) { hex ->
                            val color = Color(hex)
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(color)
                                    .border(
                                        width = if (currentColor == color) 2.dp else 0.dp,
                                        color = if (currentColor == color) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        currentColor = color
                                        android.graphics.Color.colorToHSV(color.toArgb(), initialHsv)
                                        hue = initialHsv[0]
                                        saturation = initialHsv[1]
                                        value = initialHsv[2]
                                    }
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        hue = (offset.x / size.width.toFloat()) * 360f
                                        saturation = (1f - (offset.y / size.height.toFloat())).coerceIn(0f, 1f)
                                        val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
                                        currentColor = Color(argb)
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, _ ->
                                        hue = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f
                                        saturation = (1f - (change.position.y / size.height.toFloat())).coerceIn(0f, 1f)
                                        val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
                                        currentColor = Color(argb)
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val rainbowBrush = Brush.horizontalGradient(
                                    listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                )
                                drawRect(brush = rainbowBrush)
                                val saturationBrush = Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black)
                                )
                                drawRect(brush = saturationBrush)
                            }

                            val indicatorX = (hue / 360f)
                            val indicatorY = 1f - saturation
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(
                                        start = (indicatorX * 280).dp,
                                        top = (indicatorY * 140).dp
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, Color.White, CircleShape)
                                        .background(currentColor)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Slider(
                                value = value,
                                onValueChange = {
                                    value = it
                                    val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
                                    currentColor = Color(argb)
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = currentColor,
                                    inactiveTrackColor = Color(0xFF121214)
                                )
                            )
                            Text(
                                text = "${(value * 100).toInt()}%",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val argb = currentColor.toArgb()
                val hexStr = String.format("#%06X", 0xFFFFFF and argb)
                val red = (argb shr 16) and 0xFF
                val green = (argb shr 8) and 0xFF
                val blue = argb and 0xFF

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(currentColor)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Hex", color = Color.Gray, fontSize = 10.sp)
                        Text(text = hexStr, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Red", color = Color.Gray, fontSize = 10.sp)
                        Text(text = "$red", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Green", color = Color.Gray, fontSize = 10.sp)
                        Text(text = "$green", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Blue", color = Color.Gray, fontSize = 10.sp)
                        Text(text = "$blue", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Cancel", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { onColorSelected(currentColor) }) {
                        Text(text = "Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}