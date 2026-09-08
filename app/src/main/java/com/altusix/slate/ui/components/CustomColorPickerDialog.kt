package com.altusix.slate.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun CustomColorPickerDialog(
    initialColor: Color,
    title: String = "Custom Color",
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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22))
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

                            val indicatorX = (hue / 360f).coerceIn(0f, 1f)
                            val indicatorY = (1f - saturation).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(
                                        start = (indicatorX * 260).dp,
                                        top = (indicatorY * 135).dp
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
                        Text(text = "RGB", color = Color.Gray, fontSize = 10.sp)
                        Text(
                            text = "${(argb shr 16) and 0xFF}, ${(argb shr 8) and 0xFF}, ${argb and 0xFF}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
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

@Composable
fun RainbowCustomCircle(
    isSelected: Boolean,
    activeColor: Color?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val rainbowBrush = Brush.sweepGradient(
        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
    )
    val scale by animateFloatAsState(if (isSelected) 1.12f else 1.0f, label = "customCircleScale")

    Box(
        modifier = modifier
            .size(36.dp)
            .scale(scale)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(1.8.dp, Color.White, CircleShape)
            )
        }

        val baseModifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .border(0.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)

        val finalModifier = if (activeColor != null) {
            baseModifier.background(activeColor)
        } else {
            baseModifier.background(rainbowBrush)
        }

        Box(
            modifier = finalModifier,
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = if (activeColor != null && (activeColor.red * 0.299 + activeColor.green * 0.587 + activeColor.blue * 0.114) > 0.6) Color.Black else Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}