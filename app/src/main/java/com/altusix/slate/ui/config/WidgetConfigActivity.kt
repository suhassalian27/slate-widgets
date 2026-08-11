package com.altusix.slate.ui.config

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.widgets.ai.updateAllAiFolderWidgets
import com.altusix.slate.widgets.ai.updateAllAiWidgets
import com.altusix.slate.widgets.applauncher.updateAllAppLauncherWidgets
import com.altusix.slate.widgets.battery.updateAllBatteryWidgets
import com.altusix.slate.widgets.bluetooth.updateAllBluetoothWidgets
import com.altusix.slate.widgets.calendar.updateAllCalendarWidgets

enum class ColorPickerTarget {
    BACKGROUND, ACCENT
}

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetClassName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val widgetInfo = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)
        widgetClassName = widgetInfo?.provider?.className ?: ""

        setContent {
            SlateConfigTheme {
                var selectedBgHex by remember { mutableLongStateOf(0xFF161618L) }
                var selectedAccentHex by remember { mutableLongStateOf(0xFFFFFFFFL) }
                var opacity by remember { mutableFloatStateOf(1.0f) }
                var activePickerTarget by remember { mutableStateOf<ColorPickerTarget?>(null) }

                // Load existing configuration from SharedPreferences
                LaunchedEffect(appWidgetId) {
                    val prefs = getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
                    if (prefs.contains("widget_${appWidgetId}_bg_color")) {
                        selectedBgHex = prefs.getLong("widget_${appWidgetId}_bg_color", 0xFF161618L)
                        opacity = prefs.getFloat("widget_${appWidgetId}_opacity", 1.0f)
                        selectedAccentHex = prefs.getLong("widget_${appWidgetId}_accent_color", 0xFFFFFFFFL)
                    }
                }

                // Determine contrast mode dynamically based on background luminance
                val isLightBg = remember(selectedBgHex) { calculateLuminance(selectedBgHex) > 0.5f }
                val textColor = if (isLightBg) Color.Black else Color.White

                // Background Presets
                val bgPresets = listOf(
                    0xFF161618L to "Dark",
                    0xFF000000L to "AMOLED",
                    0xFFFFFFFFL to "Light"
                )

                // Accent Presets
                val accentPresets = if (isLightBg) {
                    listOf(0xFF000000L, 0xFF00D166L, 0xFF2B80FFL, 0xFFFF3B30L, 0xFFFF9500L, 0xFFAF52DEL)
                } else {
                    listOf(0xFFFFFFFFL, 0xFF00D166L, 0xFF2B80FFL, 0xFFFF3B30L, 0xFFFF9500L, 0xFFAF52DEL)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF09090A))
                        .padding(horizontal = 22.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Customize Widget",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )

                    // 1. STUDIO LIVE PREVIEW CARD
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF1B1B1E), Color(0xFF121214))
                                )
                            )
                            .border(1.dp, Color(0xFF28282C), RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val previewBg = Color(selectedBgHex).copy(alpha = opacity)

                        Box(
                            modifier = Modifier
                                .size(148.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(previewBg)
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                                .padding(16.dp)
                        ) {
                            if (widgetClassName.contains("ArcGaugeBatteryReceiver")) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "BATTERY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f))
                                        Text(text = "CHARGING", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(selectedAccentHex))
                                    }

                                    val arcBitmap = remember(selectedAccentHex, selectedBgHex) {
                                        generateArcGaugeBitmapPreview(85, Color(selectedAccentHex), textColor.copy(alpha = 0.15f))
                                    }

                                    Image(
                                        bitmap = arcBitmap.asImageBitmap(),
                                        contentDescription = "Arc Preview",
                                        modifier = Modifier.size(100.dp, 50.dp)
                                    )

                                    Text(text = "85%", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = textColor)
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "BATTERY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f))
                                        Text(text = "CHARGING", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(selectedAccentHex))
                                    }
                                    Text(text = "85%", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = textColor)
                                    LinearProgressIndicator(
                                        progress = { 0.85f },
                                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                                        color = Color(selectedAccentHex),
                                        trackColor = textColor.copy(alpha = 0.15f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 2. BACKGROUND COLOR SECTION
                    SectionTitle(title = "Background", subtitle = "Presets & custom tint")
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bgPresets.forEach { (hex, label) ->
                            val isSelected = selectedBgHex == hex
                            SelectableChip(
                                label = label,
                                isSelected = isSelected,
                                colorPreview = Color(hex),
                                modifier = Modifier.weight(1f)
                            ) {
                                selectedBgHex = hex
                                if (hex == 0xFFFFFFFFL && selectedAccentHex == 0xFFFFFFFFL) {
                                    selectedAccentHex = 0xFF000000L
                                } else if (hex != 0xFFFFFFFFL && selectedAccentHex == 0xFF000000L) {
                                    selectedAccentHex = 0xFFFFFFFFL
                                }
                            }
                        }

                        // Custom BG Rainbow Chip
                        val isCustomBg = bgPresets.none { it.first == selectedBgHex }
                        RainbowPickerChip(
                            isSelected = isCustomBg,
                            activeColor = if (isCustomBg) Color(selectedBgHex) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            activePickerTarget = ColorPickerTarget.BACKGROUND
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3. ACCENT COLOR SECTION
                    SectionTitle(title = "Accent Color", subtitle = "Primary highlight & controls")
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        accentPresets.forEach { hex ->
                            val isSelected = selectedAccentHex == hex
                            ProfessionalSwatchCircle(
                                color = Color(hex),
                                isSelected = isSelected,
                                onClick = { selectedAccentHex = hex }
                            )
                        }

                        // Custom Accent Rainbow Button
                        val isCustomAccent = accentPresets.none { it == selectedAccentHex }
                        RainbowCustomCircle(
                            isSelected = isCustomAccent,
                            activeColor = if (isCustomAccent) Color(selectedAccentHex) else null,
                            onClick = { activePickerTarget = ColorPickerTarget.ACCENT }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 4. OPACITY SLIDER SECTION
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionTitle(title = "Opacity", subtitle = "Surface transparency")
                        Text(
                            text = "${(opacity * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    Slider(
                        value = opacity,
                        onValueChange = { opacity = it },
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color(0xFF242428)
                        )
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // 5. CTA APPLY BUTTON
                    Button(
                        onClick = {
                            saveAndFinish(
                                SlateWidgetConfig(
                                    themeMode = if (isLightBg) "LIGHT" else "DARK",
                                    backgroundColorHex = selectedBgHex,
                                    opacity = opacity,
                                    accentColorHex = selectedAccentHex
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text(text = "Apply Widget", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Custom Color Picker Dialog for both Background and Accent
                activePickerTarget?.let { target ->
                    val initialColor = if (target == ColorPickerTarget.BACKGROUND) Color(selectedBgHex) else Color(selectedAccentHex)
                    CustomColorPickerDialog(
                        initialColor = initialColor,
                        title = if (target == ColorPickerTarget.BACKGROUND) "Custom Background" else "Custom Accent",
                        onDismiss = { activePickerTarget = null },
                        onColorSelected = { color ->
                            val hex = (color.toArgb().toLong() and 0xFFFFFFFFL)
                            if (target == ColorPickerTarget.BACKGROUND) {
                                selectedBgHex = hex
                            } else {
                                selectedAccentHex = hex
                            }
                            activePickerTarget = null
                        }
                    )
                }
            }
        }
    }

    private fun calculateLuminance(hex: Long): Float {
        val r = ((hex shr 16) and 0xFFL) / 255f
        val g = ((hex shr 8) and 0xFFL) / 255f
        val b = (hex and 0xFFL) / 255f
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    private fun saveAndFinish(config: SlateWidgetConfig) {
        val prefs = getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("widget_${appWidgetId}_theme_mode", config.themeMode)
            .putLong("widget_${appWidgetId}_bg_color", config.backgroundColorHex)
            .putFloat("widget_${appWidgetId}_opacity", config.opacity)
            .putLong("widget_${appWidgetId}_accent_color", config.accentColorHex)
            .commit()

        val manager = AppWidgetManager.getInstance(this)
        val widgetInfo = manager.getAppWidgetInfo(appWidgetId)

        if (widgetInfo?.provider != null) {
            val updateIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                component = widgetInfo.provider
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            }
            sendBroadcast(updateIntent)
        }

        updateAllBatteryWidgets(this)
        updateAllAiWidgets(this)
        updateAllAiFolderWidgets(this)
        updateAllBluetoothWidgets(this)
        updateAllAppLauncherWidgets(this)
        updateAllCalendarWidgets(this)

        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        finish()
    }

    private fun generateArcGaugeBitmapPreview(percentage: Int, accentColor: Color, trackColor: Color): Bitmap {
        val bitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val strokeWidth = 32f
        val padding = strokeWidth / 2f + 4f
        val rectF = RectF(padding, padding, 200f - padding, 200f - padding)

        val trackPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            color = trackColor.toArgb()
        }
        val activePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            color = accentColor.toArgb()
        }

        canvas.drawArc(rectF, 210f, 120f, false, trackPaint)
        canvas.drawArc(rectF, 210f, (percentage / 100f) * 120f, false, activePaint)
        return bitmap
    }
}

// ============================================================================
// SLEEK RE-IMAGINED UI COMPONENTS
// ============================================================================

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(text = subtitle, color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
private fun SelectableChip(
    label: String,
    isSelected: Boolean,
    colorPreview: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val animatedBg by animateColorAsState(if (isSelected) Color(0xFF28282C) else Color(0xFF141416), label = "chipBg")
    val animatedBorder by animateColorAsState(if (isSelected) Color.White.copy(alpha = 0.6f) else Color(0xFF222226), label = "chipBorder")

    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(animatedBg)
            .border(1.dp, animatedBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(colorPreview)
                .border(0.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun RainbowPickerChip(
    isSelected: Boolean,
    activeColor: Color?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val rainbowBrush = Brush.sweepGradient(
        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
    )
    val animatedBorder by animateColorAsState(if (isSelected) Color.White else Color(0xFF222226), label = "rainbowBorder")

    val chipModifier = modifier
        .height(40.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF141416))
        .border(1.dp, animatedBorder, RoundedCornerShape(12.dp))
        .clickable { onClick() }
        .padding(horizontal = 10.dp)

    Row(
        modifier = chipModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val dotModifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .border(0.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)

        if (activeColor != null) {
            Box(modifier = dotModifier.background(activeColor))
        } else {
            Box(modifier = dotModifier.background(rainbowBrush))
        }

        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Custom",
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun ProfessionalSwatchCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isSelected) 1.12f else 1.0f, label = "circleScale")
    val isLightColor = remember(color) {
        val argb = color.toArgb()
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        (0.2126f * r + 0.7152f * g + 0.0722f * b) > 0.6f
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.15f),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (isLightColor) Color.Black else Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun RainbowCustomCircle(
    isSelected: Boolean,
    activeColor: Color?,
    onClick: () -> Unit
) {
    val rainbowBrush = Brush.sweepGradient(
        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
    )
    val scale by animateFloatAsState(if (isSelected) 1.12f else 1.0f, label = "customCircleScale")

    val circleModifier = Modifier
        .size(38.dp)
        .scale(scale)
        .clip(CircleShape)
        .border(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.2f),
            shape = CircleShape
        )
        .clickable { onClick() }

    val baseModifier = if (activeColor != null) {
        circleModifier.background(activeColor)
    } else {
        circleModifier.background(rainbowBrush)
    }

    Box(
        modifier = baseModifier,
        contentAlignment = Alignment.Center
    ) {
        if (isSelected && activeColor != null) {
            val isLightColor = remember(activeColor) {
                val argb = activeColor.toArgb()
                val r = ((argb shr 16) and 0xFF) / 255f
                val g = ((argb shr 8) and 0xFF) / 255f
                val b = (argb and 0xFF) / 255f
                (0.2126f * r + 0.7152f * g + 0.0722f * b) > 0.6f
            }
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (isLightColor) Color.Black else Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ============================================================================
// CUSTOM COLOR PICKER DIALOG (SWATCHES & SPECTRUM TABS)
// ============================================================================

@Composable
fun CustomColorPickerDialog(
    initialColor: Color,
    title: String,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Swatches, 1: Spectrum
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

@Composable
fun SlateConfigTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(background = Color(0xFF09090A), surface = Color(0xFF141416)),
        content = content
    )
}