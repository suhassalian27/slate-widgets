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
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.ui.components.CustomColorPickerDialog
import com.altusix.slate.ui.components.RainbowCustomCircle
import com.altusix.slate.ui.components.SlateConfigScaffold
import com.altusix.slate.widgets.ai.getAiWidgetsCatalog
import com.altusix.slate.widgets.ai.updateAllAiFolderWidgets
import com.altusix.slate.widgets.ai.updateAllAiWidgets
import com.altusix.slate.widgets.appfolder.getAppFolderWidgetsCatalog
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
import com.altusix.slate.widgets.camera.CameraWidgetConfigActivity
import com.altusix.slate.widgets.camera.getCameraWidgetsCatalog
import com.altusix.slate.widgets.clock.analog.getClockAnalogWidgetsCatalog
import com.altusix.slate.widgets.clock.analog.updateAllClockAnalogWidgets
import com.altusix.slate.widgets.clock.digital.getClockDigitalWidgetsCatalog
import com.altusix.slate.widgets.clock.digital.updateAllClockDigitalWidgets
import com.altusix.slate.widgets.clock.hybrid.getClockHybridWidgetsCatalog
import com.altusix.slate.widgets.clock.hybrid.updateAllClockHybridWidgets
import com.altusix.slate.widgets.contacts.getContactsWidgetsCatalog
import com.altusix.slate.widgets.deviceinfo.updateAllDeviceInfoWidgets
import com.altusix.slate.widgets.media.getMediaWidgetsCatalog
import com.altusix.slate.widgets.media.updateAllMediaWidgets
import com.altusix.slate.widgets.notes.getNotesWidgetsCatalog
import com.altusix.slate.widgets.notes.updateAllNotesWidgets
import com.altusix.slate.widgets.photos.PhotosConfigActivity
import com.altusix.slate.widgets.photos.getPhotosWidgetsCatalog
import com.altusix.slate.widgets.photos.updateAllPhotosWidgets
import com.altusix.slate.widgets.productivity.BaseProductivityReceiver
import com.altusix.slate.widgets.productivity.ProductivityConfigActivity
import com.altusix.slate.widgets.productivity.getProductivityWidgetsCatalog
import com.altusix.slate.widgets.productivity.updateAllProductivityWidgets

enum class ColorPickerTarget {
    BACKGROUND, ACCENT
}

private fun calculateLuminance(hex: Long): Float {
    val r = ((hex shr 16) and 0xFFL) / 255f
    val g = ((hex shr 8) and 0xFFL) / 255f
    val b = (hex and 0xFFL) / 255f
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetClassName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
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

        val isAppFolder = getAppFolderWidgetsCatalog().any { it.receiverClass.name == widgetClassName }
        val isAppLauncher = getAppLauncherWidgetsCatalog().any { it.receiverClass.name == widgetClassName }
        val isContacts = getContactsWidgetsCatalog().any { it.receiverClass.name == widgetClassName }

        if (isAppFolder) {
            val forwardIntent = Intent(this, AppFolderWidgetConfigActivity::class.java).apply {
                intent?.extras?.let { putExtras(it) }
                addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            }
            startActivity(forwardIntent)
            finish()
            return
        }

        if (isAppLauncher) {
            val forwardIntent = Intent(this, AppLauncherConfigActivity::class.java).apply {
                intent?.extras?.let { putExtras(it) }
                addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            }
            startActivity(forwardIntent)
            finish()
            return
        }

        if (isContacts) {
            val forwardIntent = Intent(this, ContactsWidgetConfigActivity::class.java).apply {
                intent?.extras?.let { putExtras(it) }
                addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            }
            startActivity(forwardIntent)
            finish()
            return
        }

        val isPhotos = getPhotosWidgetsCatalog().any { it.receiverClass.name == widgetClassName }
        if (isPhotos) {
            val forwardIntent = Intent(this, PhotosConfigActivity::class.java).apply {
                intent?.extras?.let { putExtras(it) }
                addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            }
            startActivity(forwardIntent)
            finish()
            return
        }

        val isCamera = getCameraWidgetsCatalog().any {
            it.receiverClass.name == widgetClassName &&
                    !it.receiverClass.name.contains("ShutterLauncher") &&
                    !it.receiverClass.name.contains("AperturePill")
        }
        if (isCamera) {
            val forwardIntent = Intent(this, CameraWidgetConfigActivity::class.java).apply {
                intent?.extras?.let { putExtras(it) }
                addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            }
            startActivity(forwardIntent)
            finish()
            return
        }

        val isProductivity = getProductivityWidgetsCatalog().any { it.receiverClass.name == widgetClassName }
        if (isProductivity) {
            val tab = when {
                widgetClassName.contains("Pomodoro") -> "TIMER"
                widgetClassName.contains("HabitMatrix") -> "HABIT"
                widgetClassName.contains("Top3") -> "TOP3"
                widgetClassName.contains("Bookmarks") -> "BOOKMARKS"
                widgetClassName.contains("Clipboard") -> "CLIPBOARD"
                widgetClassName.contains("ScreenTime") -> "SCREENTIME"
                widgetClassName.contains("Eisenhower") -> "EISENHOWER"
                widgetClassName.contains("Timeline") -> "TIMELINE"
                widgetClassName.contains("Goal") -> "GOAL"
                widgetClassName.contains("HabitRings") -> "RINGS"
                widgetClassName.contains("Pipeline") -> "PIPELINE"
                else -> "TOP3"
            }
            val forwardIntent = Intent(this, ProductivityConfigActivity::class.java).apply {
                intent?.extras?.let { putExtras(it) }
                putExtra(BaseProductivityReceiver.EXTRA_TAB, tab)
                addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            }
            startActivity(forwardIntent)
            finish()
            return
        }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF0A0A0C), surface = Color(0xFF16161B))) {
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
                            getCameraWidgetsCatalog() +
                            getMediaWidgetsCatalog() +
                            getNotesWidgetsCatalog() +
                            getPhotosWidgetsCatalog() +
                            getProductivityWidgetsCatalog()

                    allWidgets.find { it.receiverClass.name == widgetClassName }
                }

                val defaultWidgetOpacity = catalogItem?.defaultOpacity ?: 1.0f
                val hasModeOption = catalogItem?.hasModeOption ?: false
                val widgetName = catalogItem?.name ?: ""

                var selectedBgHex by remember { mutableLongStateOf(0xFF000000L) }
                var selectedAccentHex by remember { mutableLongStateOf(0xFFFFFFFFL) }
                var opacity by remember { mutableFloatStateOf(1.0f) }
                var isResponsive by remember { mutableStateOf(true) }
                var activePickerTarget by remember { mutableStateOf<ColorPickerTarget?>(null) }

                LaunchedEffect(appWidgetId) {
                    val prefs = getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)

                    opacity = prefs.getFloat("widget_${appWidgetId}_opacity", defaultWidgetOpacity)
                    isResponsive = prefs.getBoolean("widget_${appWidgetId}_is_responsive", true)

                    if (prefs.contains("widget_${appWidgetId}_bg_color")) {
                        selectedBgHex = prefs.getLong("widget_${appWidgetId}_bg_color", 0xFF000000L)
                        selectedAccentHex = prefs.getLong("widget_${appWidgetId}_accent_color", 0xFFFFFFFFL)
                    }
                }

                val isLightBg = remember(selectedBgHex) { calculateLuminance(selectedBgHex) > 0.5f }
                val textColor = if (isLightBg) Color.Black else Color.White

                val bgPresets = listOf(
                    0xFF161618L to "Matte",
                    0xFF000000L to "AMOLED",
                    0xFFFFFFFFL to "Light"
                )

                val accentPresets = if (isLightBg) {
                    listOf(0xFF000000L, 0xFF00D166L, 0xFF2B80FFL, 0xFFFF3B30L, 0xFFFF9500L, 0xFFAF52DEL)
                } else {
                    listOf(0xFFFFFFFFL, 0xFF00D166L, 0xFF2B80FFL, 0xFFFF3B30L, 0xFFFF9500L, 0xFFAF52DEL)
                }

                SlateConfigScaffold(
                    title = "Customize Widget",
                    subtitle = widgetName.ifEmpty { null },
                    accentColor = Color(selectedAccentHex),
                    onBackClick = { finish() },
                    onSaveClick = {
                        saveAndFinish(
                            config = SlateWidgetConfig(
                                themeMode = if (isLightBg) "LIGHT" else "DARK",
                                backgroundColorHex = selectedBgHex,
                                opacity = opacity,
                                accentColorHex = selectedAccentHex
                            ),
                            isResponsive = isResponsive
                        )
                    },
                    scrollable = true,
                    previewHeight = 180.dp,
                    previewContent = {
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
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle(title = "Background")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            bgPresets.forEach { (hex, label) ->
                                SelectableChip(
                                    label = label,
                                    isSelected = selectedBgHex == hex,
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

                            val isCustomBg = bgPresets.none { it.first == selectedBgHex }
                            RainbowPickerChip(
                                isSelected = isCustomBg,
                                activeColor = if (isCustomBg) Color(selectedBgHex) else null,
                                modifier = Modifier.weight(1f)
                            ) {
                                activePickerTarget = ColorPickerTarget.BACKGROUND
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionTitle(title = "Accent Color")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            accentPresets.forEach { hex ->
                                ProfessionalSwatchCircle(
                                    color = Color(hex),
                                    isSelected = selectedAccentHex == hex,
                                    onClick = { selectedAccentHex = hex }
                                )
                            }

                            val isCustomAccent = accentPresets.none { it == selectedAccentHex }
                            RainbowCustomCircle(
                                isSelected = isCustomAccent,
                                activeColor = if (isCustomAccent) Color(selectedAccentHex) else null,
                                onClick = { activePickerTarget = ColorPickerTarget.ACCENT }
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionTitle(title = "Surface Translucency")
                            Text(
                                text = "${(opacity * 100).toInt()}%",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        ModernOpacitySlider(
                            value = opacity,
                            onValueChange = { opacity = it }
                        )
                    }

                    if (hasModeOption) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle(title = "Sizing Mode")

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF141416))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf(true to "Responsive", false to "Fixed Aspect").forEach { (responsive, label) ->
                                    val isSelected = isResponsive == responsive
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color(0xFF2C2C30) else Color.Transparent)
                                            .clickable { isResponsive = responsive },
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
                    }
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
        updateAllDeviceInfoWidgets(this)
        updateAllMediaWidgets(this)
        updateAllNotesWidgets(this)
        updateAllPhotosWidgets(this)
        updateAllProductivityWidgets(this)

        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        finish()
    }
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