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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.altusix.slate.widgets.battery.updateAllBatteryWidgets

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
                var themeMode by remember { mutableStateOf("DARK") }
                var opacity by remember { mutableFloatStateOf(1.0f) }
                var selectedAccentHex by remember { mutableLongStateOf(0xFFFFFFFFL) }
                var showColorPickerDialog by remember { mutableStateOf(false) }

                val currentBgHex = if (themeMode == "LIGHT") 0xFFFFFFFFL else 0xFF161618L

                // Load existing configuration from SharedPreferences
                LaunchedEffect(appWidgetId) {
                    val prefs = getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
                    if (prefs.contains("widget_${appWidgetId}_theme_mode")) {
                        themeMode = prefs.getString("widget_${appWidgetId}_theme_mode", "DARK") ?: "DARK"
                        opacity = prefs.getFloat("widget_${appWidgetId}_opacity", 1.0f)
                        val defaultAccent = if (themeMode == "LIGHT") 0xFF000000L else 0xFFFFFFFFL
                        selectedAccentHex = prefs.getLong("widget_${appWidgetId}_accent_color", defaultAccent)
                    }
                }

                val accentOptions = if (themeMode == "LIGHT") {
                    listOf(0xFF000000L, 0xFF00D166L, 0xFF2B80FFL, 0xFFFF3B30L, 0xFFFF9500L, 0xFFAF52DEL)
                } else {
                    listOf(0xFFFFFFFFL, 0xFF00D166L, 0xFF2B80FFL, 0xFFFF3B30L, 0xFFFF9500L, 0xFFAF52DEL)
                }

                val isCustomColorSelected = !accentOptions.contains(selectedAccentHex)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0D0D0E))
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Customize Widget",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF161618)),
                        contentAlignment = Alignment.Center
                    ) {
                        val previewBg = Color(currentBgHex).copy(alpha = opacity)
                        val textColor = if (themeMode == "LIGHT") Color.Black else Color.White

                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(previewBg)
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(22.dp))
                                .padding(14.dp)
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
                                        Text(text = "BATTERY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f))
                                        Text(text = "CHARGING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(selectedAccentHex))
                                    }

                                    val arcBitmap = remember(selectedAccentHex, themeMode) {
                                        generateArcGaugeBitmapPreview(85, Color(selectedAccentHex), textColor.copy(alpha = 0.15f))
                                    }

                                    Image(
                                        bitmap = arcBitmap.asImageBitmap(),
                                        contentDescription = "Arc Preview",
                                        modifier = Modifier.size(100.dp, 50.dp)
                                    )

                                    Text(text = "85%", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = textColor)
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
                                        Text(text = "BATTERY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f))
                                        Text(text = "CHARGING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(selectedAccentHex))
                                    }
                                    Text(text = "85%", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = textColor)
                                    LinearProgressIndicator(
                                        progress = { 0.85f },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                        color = Color(selectedAccentHex),
                                        trackColor = textColor.copy(alpha = 0.15f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(text = "Style Mode", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF161618))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("DARK" to "Dark", "LIGHT" to "Light").forEach { (mode, label) ->
                            val isSelected = themeMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF2C2C30) else Color.Transparent)
                                    .clickable {
                                        if (themeMode != mode) {
                                            themeMode = mode
                                            if (mode == "LIGHT" && selectedAccentHex == 0xFFFFFFFFL) {
                                                selectedAccentHex = 0xFF000000L
                                            } else if (mode == "DARK" && selectedAccentHex == 0xFF000000L) {
                                                selectedAccentHex = 0xFFFFFFFFL
                                            }
                                        }
                                    },
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

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(text = "Accent Color", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        accentOptions.forEach { hex ->
                            val isSelected = selectedAccentHex == hex
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color.Green else Color.White.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedAccentHex = hex }
                            )
                        }

                        // Rainbow Custom Color Button
                        val rainbowBrush = Brush.sweepGradient(
                            listOf(
                                Color.Red, Color.Yellow, Color.Green,
                                Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(rainbowBrush)
                                .border(
                                    width = if (isCustomColorSelected) 2.5.dp else 1.dp,
                                    color = if (isCustomColorSelected) Color.Green else Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable { showColorPickerDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCustomColorSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color(selectedAccentHex))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Opacity", color = Color.Gray, fontSize = 13.sp)
                        Text(text = "${(opacity * 100).toInt()}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = opacity,
                        onValueChange = { opacity = it },
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color(0xFF2C2C30)
                        )
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            saveAndFinish(
                                SlateWidgetConfig(
                                    themeMode = themeMode,
                                    backgroundColorHex = currentBgHex,
                                    opacity = opacity,
                                    accentColorHex = selectedAccentHex
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text(text = "Apply Widget", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                if (showColorPickerDialog) {
                    CustomColorPickerDialog(
                        initialColor = Color(selectedAccentHex),
                        onDismiss = { showColorPickerDialog = false },
                        onColorSelected = { color ->
                            selectedAccentHex = (color.toArgb().toLong() and 0xFFFFFFFFL)
                            showColorPickerDialog = false
                        }
                    )
                }
            }
        }
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
// CUSTOM COLOR PICKER DIALOG (SWATCHES & SPECTRUM TABS)
// ============================================================================

@Composable
fun CustomColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Swatches, 1: Spectrum
    var currentColor by remember { mutableStateOf(initialColor) }

    // HSV values for Spectrum mode
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
            0xFF212121L, 0xFFD32F2FL, 0xFFF57C00L, 0xFFFFB300L, 0xFF388E3CL, 0xFF0097A7L, 0xFF1976D2L, 0xFF7B1FA2L,
            0xFF000000L, 0xFFB71C1CL, 0xFFE65100L, 0xFFFF8F00L, 0xFF1B5E20L, 0xFF006064L, 0xFF0D47A1L, 0xFF4A148CL
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF242426))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tab Header: Swatches | Spectrum
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(Color(0xFF19191B)),
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
                                .background(if (isSelected) Color(0xFF323236) else Color.Transparent)
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
                    // Swatches View
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
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color)
                                    .border(
                                        width = if (currentColor == color) 2.dp else 0.dp,
                                        color = if (currentColor == color) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(4.dp)
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
                    // Spectrum View (2D Saturation/Hue Box + Value/Brightness Slider)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 2D Gradient Box (Hue & Saturation)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
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
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                val rainbowBrush = Brush.horizontalGradient(
                                    listOf(
                                        Color.Red, Color.Yellow, Color.Green,
                                        Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                    )
                                )
                                drawRect(brush = rainbowBrush)
                                val saturationBrush = Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black)
                                )
                                drawRect(brush = saturationBrush)
                            }

                            // Pointer Indicator
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

                        // Brightness Slider
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
                                    inactiveTrackColor = Color(0xFF19191B)
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

                // Color Preview & RGB / Hex Readout
                val argb = currentColor.toArgb()
                val hexStr = String.format("#%06X", 0xFFFFFF and argb)
                val red = (argb shrinkRight 16) and 0xFF
                val green = (argb shrinkRight 8) and 0xFF
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

                // Cancel / Done Actions
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

private infix fun Int.shrinkRight(bitCount: Int): Int = this shr bitCount

@Composable
fun SlateConfigTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(background = Color(0xFF0D0D0E), surface = Color(0xFF161618)),
        content = content
    )
}