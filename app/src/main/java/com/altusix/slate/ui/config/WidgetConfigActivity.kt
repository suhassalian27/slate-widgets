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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.altusix.slate.widgets.ai.getAiWidgetsCatalog
import com.altusix.slate.widgets.ai.updateAllAiFolderWidgets
import com.altusix.slate.widgets.ai.updateAllAiWidgets
import com.altusix.slate.widgets.applauncher.getAppLauncherWidgetsCatalog
import com.altusix.slate.widgets.applauncher.updateAllAppLauncherWidgets
import com.altusix.slate.widgets.battery.getBatteryWidgetsCatalog
import com.altusix.slate.widgets.battery.updateAllBatteryWidgets
import com.altusix.slate.widgets.bluetooth.getBluetoothWidgetsCatalog
import com.altusix.slate.widgets.bluetooth.updateAllBluetoothWidgets
import com.altusix.slate.widgets.calculator.getCalculatorWidgetsCatalog
import com.altusix.slate.widgets.calculator.updateAllCalculatorWidgets
import com.altusix.slate.widgets.calendar.getCalendarWidgetsCatalog
import com.altusix.slate.widgets.calendar.updateAllCalendarWidgets
import com.altusix.slate.widgets.camera.getCameraWidgetsCatalog
import com.altusix.slate.widgets.clock.analog.getClockAnalogWidgetsCatalog
import com.altusix.slate.widgets.clock.analog.updateAllClockAnalogWidgets
import com.altusix.slate.widgets.clock.digital.getClockDigitalWidgetsCatalog
import com.altusix.slate.widgets.clock.digital.updateAllClockDigitalWidgets
import com.altusix.slate.widgets.clock.hybrid.getClockHybridWidgetsCatalog
import com.altusix.slate.widgets.clock.hybrid.updateAllClockHybridWidgets
import com.altusix.slate.widgets.appfolder.getAppFolderWidgetsCatalog
import com.altusix.slate.widgets.applauncher.getAppLauncherWidgetsCatalog
import com.altusix.slate.ui.config.AppFolderWidgetConfigActivity
import com.altusix.slate.ui.config.AppLauncherConfigActivity
enum class ColorPickerTarget {
    BACKGROUND, ACCENT
}

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetClassName: String = ""

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        window.setBackgroundDrawableResource(android.R.color.transparent)

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

        // Catalog-based routing: Matches all 19 shape launchers and app folders cleanly
        val isAppFolder = getAppFolderWidgetsCatalog().any { it.receiverClass.name == widgetClassName }
        val isAppLauncher = getAppLauncherWidgetsCatalog().any { it.receiverClass.name == widgetClassName }

        if (isAppFolder) {
            val forwardIntent = Intent(this, com.altusix.slate.ui.config.AppFolderWidgetConfigActivity::class.java).apply {
                intent?.extras?.let { putExtras(it) }
                addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            }
            startActivity(forwardIntent)
            finish()
            return
        }

        if (isAppLauncher) {
            val forwardIntent = Intent(this, com.altusix.slate.ui.config.AppLauncherConfigActivity::class.java).apply {
                intent?.extras?.let { putExtras(it) }
                addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            }
            startActivity(forwardIntent)
            finish()
            return
        }

        setContent {
            SlateConfigTheme {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

                val catalogItem = remember(widgetClassName) {
                    val allWidgets = getClockDigitalWidgetsCatalog() +
                            getClockAnalogWidgetsCatalog() +
                            getClockHybridWidgetsCatalog() +
                            getBatteryWidgetsCatalog() +
                            getAiWidgetsCatalog() +
                            getBluetoothWidgetsCatalog() +
                            getAppLauncherWidgetsCatalog() +
                            getCalendarWidgetsCatalog() +
                            getCalculatorWidgetsCatalog() +
                            getCameraWidgetsCatalog()

                    allWidgets.find { it.receiverClass.name == widgetClassName }
                }

                val defaultWidgetOpacity = catalogItem?.defaultOpacity ?: 1.0f
                val hasModeOption = catalogItem?.hasModeOption ?: false

                var selectedBgHex by remember { mutableLongStateOf(0xFF161618L) }
                var selectedAccentHex by remember { mutableLongStateOf(0xFFFFFFFFL) }
                var opacity by remember { mutableFloatStateOf(1.0f) }
                var isResponsive by remember { mutableStateOf(true) }
                var activePickerTarget by remember { mutableStateOf<ColorPickerTarget?>(null) }

                LaunchedEffect(appWidgetId) {
                    val prefs = getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)

                    opacity = prefs.getFloat("widget_${appWidgetId}_opacity", defaultWidgetOpacity)
                    isResponsive = prefs.getBoolean("widget_${appWidgetId}_is_responsive", true)

                    if (prefs.contains("widget_${appWidgetId}_bg_color")) {
                        selectedBgHex = prefs.getLong("widget_${appWidgetId}_bg_color", 0xFF161618L)
                        selectedAccentHex = prefs.getLong("widget_${appWidgetId}_accent_color", 0xFFFFFFFFL)
                    }
                }

                ModalBottomSheet(
                    onDismissRequest = { finish() },
                    sheetState = sheetState,
                    containerColor = Color(0xFF0A0A0C),
                    contentColor = Color.White,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF2C2C30)) },
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                ) {
                    SlateWidgetConfigSheetContent(
                        widgetClassName = widgetClassName,
                        widgetName = catalogItem?.name ?: "",
                        selectedBgHex = selectedBgHex,
                        selectedAccentHex = selectedAccentHex,
                        opacity = opacity,
                        isResponsive = isResponsive,
                        hasModeOption = hasModeOption,
                        onBgHexChanged = { selectedBgHex = it },
                        onAccentHexChanged = { selectedAccentHex = it },
                        onOpacityChanged = { opacity = it },
                        onResponsiveChanged = { isResponsive = it },
                        onPickerTargetRequested = { activePickerTarget = it },
                        onDismiss = { finish() },
                        onApplyClicked = {
                            saveAndFinish(
                                config = SlateWidgetConfig(
                                    themeMode = if (calculateLuminance(selectedBgHex) > 0.5f) "LIGHT" else "DARK",
                                    backgroundColorHex = selectedBgHex,
                                    opacity = opacity,
                                    accentColorHex = selectedAccentHex
                                ),
                                isResponsive = isResponsive
                            )
                        }
                    )
                }

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

    private fun saveAndFinish(config: SlateWidgetConfig, isResponsive: Boolean) {
        val prefs = getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("widget_${appWidgetId}_theme_mode", config.themeMode)
            .putLong("widget_${appWidgetId}_bg_color", config.backgroundColorHex)
            .putFloat("widget_${appWidgetId}_opacity", config.opacity)
            .putLong("widget_${appWidgetId}_accent_color", config.accentColorHex)
            .putBoolean("widget_${appWidgetId}_is_responsive", isResponsive)
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
        updateAllCalculatorWidgets(this)
        updateAllClockDigitalWidgets(this)
        updateAllClockAnalogWidgets(this)
        updateAllClockHybridWidgets(this)

        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        finish()
    }
}

@Composable
private fun SlateWidgetConfigSheetContent(
    widgetClassName: String,
    widgetName: String,
    selectedBgHex: Long,
    selectedAccentHex: Long,
    opacity: Float,
    isResponsive: Boolean,
    hasModeOption: Boolean,
    onBgHexChanged: (Long) -> Unit,
    onAccentHexChanged: (Long) -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onResponsiveChanged: (Boolean) -> Unit,
    onPickerTargetRequested: (ColorPickerTarget) -> Unit,
    onDismiss: () -> Unit,
    onApplyClicked: () -> Unit
) {
    val isLightBg = remember(selectedBgHex) { calculateLuminance(selectedBgHex) > 0.5f }
    val textColor = if (isLightBg) Color.Black else Color.White

    val bgPresets = listOf(
        0xFF161618L to "Dark",
        0xFF000000L to "AMOLED",
        0xFFFFFFFFL to "Light"
    )

    val accentPresets = if (isLightBg) {
        listOf(0xFF000000L, 0xFF00D166L, 0xFF2B80FFL, 0xFFFF3B30L, 0xFFFF9500L, 0xFFAF52DEL)
    } else {
        listOf(0xFFFFFFFFL, 0xFF00D166L, 0xFF2B80FFL, 0xFFFF3B30L, 0xFFFF9500L, 0xFFAF52DEL)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.88f)
            .padding(horizontal = 22.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cancel",
                color = Color.Gray,
                fontSize = 15.sp,
                modifier = Modifier.clickable { onDismiss() }.padding(vertical = 8.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Customize Widget",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (widgetName.isNotEmpty()) {
                    Text(
                        text = widgetName,
                        fontSize = 11.sp,
                        color = Color(0xFF8E8E93),
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Text(
                text = "Apply",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.clickable { onApplyClicked() }.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF141416))
                .border(1.dp, Color(0xFF242428), RoundedCornerShape(24.dp)),
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            SectionTitle(title = "Background")
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
                        onBgHexChanged(hex)
                        if (hex == 0xFFFFFFFFL && selectedAccentHex == 0xFFFFFFFFL) {
                            onAccentHexChanged(0xFF000000L)
                        } else if (hex != 0xFFFFFFFFL && selectedAccentHex == 0xFF000000L) {
                            onAccentHexChanged(0xFFFFFFFFL)
                        }
                    }
                }

                val isCustomBg = bgPresets.none { it.first == selectedBgHex }
                RainbowPickerChip(
                    isSelected = isCustomBg,
                    activeColor = if (isCustomBg) Color(selectedBgHex) else null,
                    modifier = Modifier.weight(1f)
                ) {
                    onPickerTargetRequested(ColorPickerTarget.BACKGROUND)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle(title = "Accent Color")
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
                        onClick = { onAccentHexChanged(hex) }
                    )
                }

                val isCustomAccent = accentPresets.none { it == selectedAccentHex }
                RainbowCustomCircle(
                    isSelected = isCustomAccent,
                    activeColor = if (isCustomAccent) Color(selectedAccentHex) else null,
                    onClick = { onPickerTargetRequested(ColorPickerTarget.ACCENT) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle(title = "Opacity")
                Text(
                    text = "${(opacity * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            ModernOpacitySlider(
                value = opacity,
                onValueChange = onOpacityChanged,
//                valueRange = 0.0f..1.0f,
//                colors = SliderDefaults.colors(
//                    thumbColor = Color.White,
//                    activeTrackColor = Color.White,
//                    inactiveTrackColor = Color(0xFF242428)
//                )
            )

            if (hasModeOption) {
                Spacer(modifier = Modifier.height(24.dp))

                SectionTitle(title = "Sizing Mode")
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF141416))
                        .padding(4.dp)
                ) {
                    listOf(true to "Responsive", false to "Fixed Aspect").forEach { (responsive, label) ->
                        val isSelected = isResponsive == responsive
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF2C2C30) else Color.Transparent)
                                .clickable { onResponsiveChanged(responsive) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color(0xFF8E8E93),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun calculateLuminance(hex: Long): Float {
    val r = ((hex shr 16) and 0xFFL) / 255f
    val g = ((hex shr 8) and 0xFFL) / 255f
    val b = (hex and 0xFFL) / 255f
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
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

@Composable
fun SectionTitle(title: String) {
    Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
fun SelectableChip(
    label: String,
    isSelected: Boolean,
    colorPreview: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val animatedBg by animateColorAsState(if (isSelected) Color(0xFF26262A) else Color(0xFF141416), label = "chipBg")
    val animatedBorder by animateColorAsState(if (isSelected) Color.White.copy(alpha = 0.5f) else Color(0xFF202024), label = "chipBorder")

    Row(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(animatedBg)
            .border(1.dp, animatedBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(colorPreview)
                .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = if (isSelected) Color.White else Color(0xFF8E8E93),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun RainbowPickerChip(
    isSelected: Boolean,
    activeColor: Color?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val rainbowBrush = Brush.sweepGradient(
        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
    )
    val animatedBg by animateColorAsState(if (isSelected) Color(0xFF26262A) else Color(0xFF141416), label = "rainbowChipBg")
    val animatedBorder by animateColorAsState(if (isSelected) Color.White.copy(alpha = 0.5f) else Color(0xFF202024), label = "rainbowChipBorder")

    Row(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(animatedBg)
            .border(1.dp, animatedBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val dotModifier = Modifier
            .size(9.dp)
            .clip(CircleShape)
            .border(0.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)

        if (activeColor != null) {
            Box(modifier = dotModifier.background(activeColor))
        } else {
            Box(modifier = dotModifier.background(rainbowBrush))
        }

        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Custom",
            color = if (isSelected) Color.White else Color(0xFF8E8E93),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun ProfessionalSwatchCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isSelected) 1.15f else 1.0f, label = "circleScale")

    Box(
        modifier = Modifier
            .size(40.dp)
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

        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(color)
                .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                val isLightColor = remember(color) {
                    val argb = color.toArgb()
                    val r = ((argb shr 16) and 0xFF) / 255f
                    val g = ((argb shr 8) and 0xFF) / 255f
                    val b = (argb and 0xFF) / 255f
                    (0.2126f * r + 0.7152f * g + 0.0722f * b) > 0.6f
                }
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = if (isLightColor) Color.Black else Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
fun RainbowCustomCircle(
    isSelected: Boolean,
    activeColor: Color?,
    onClick: () -> Unit
) {
    val rainbowBrush = Brush.sweepGradient(
        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
    )
    val scale by animateFloatAsState(if (isSelected) 1.15f else 1.0f, label = "customCircleScale")

    Box(
        modifier = Modifier
            .size(40.dp)
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
            .size(30.dp)
            .clip(CircleShape)
            .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)

        val finalModifier = if (activeColor != null) {
            baseModifier.background(activeColor)
        } else {
            baseModifier.background(rainbowBrush)
        }

        Box(
            modifier = finalModifier,
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
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernOpacitySlider(
    value: Float,
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
                    .background(Color(0xFF18181C))
                    .border(0.5.dp, Color(0xFF242428), RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(sliderState.value)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.White.copy(alpha = 0.25f), Color.White)
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
                    .border(2.dp, Color(0xFF0A0A0C), CircleShape)
            )
        }
    )
}

@Composable
fun CustomColorPickerDialog(
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
        colorScheme = darkColorScheme(
            background = Color.Transparent,
            surface = Color(0xFF0A0A0C)
        ),
        content = content
    )
}
