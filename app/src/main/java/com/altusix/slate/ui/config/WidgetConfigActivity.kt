package com.altusix.slate.ui.config

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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
import androidx.compose.ui.platform.LocalContext
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
import com.altusix.slate.widgets.quicktoggles.QuickToggleLivePreview
import com.altusix.slate.widgets.quicktoggles.getQuickTogglesWidgetsCatalog
import com.altusix.slate.widgets.quicktoggles.updateAllQuickTogglesWidgets
import com.altusix.slate.widgets.quotes.QuotesConfigActivity
import com.altusix.slate.widgets.quotes.getQuotesWidgetsCatalog
import com.altusix.slate.widgets.quotes.updateAllQuotesWidgets
import com.altusix.slate.widgets.social.SocialConfigActivity
import com.altusix.slate.widgets.social.getSocialWidgetsCatalog
import com.altusix.slate.widgets.social.updateAllSocialWidgets
import com.altusix.slate.widgets.weather.WeatherConfigActivity
import com.altusix.slate.widgets.weather.getWeatherWidgetsCatalog
import com.altusix.slate.widgets.weather.updateAllWeatherWidgets
import com.altusix.slate.widgets.ai.AiWidgetConfigActivity
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
        val isAiWidget = getAiWidgetsCatalog().any { it.receiverClass.name == widgetClassName }

        if (isAiWidget) {
            val forwardIntent = Intent(this, AiWidgetConfigActivity::class.java).apply {
                intent?.extras?.let { putExtras(it) }
                addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            }
            startActivity(forwardIntent)
            finish()
            return
        }

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

        val isQuotes = getQuotesWidgetsCatalog().any { it.receiverClass.name == widgetClassName }
        if (isQuotes) {
            val forwardIntent = Intent(this, QuotesConfigActivity::class.java).apply {
                intent?.extras?.let { putExtras(it) }
                addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            }
            startActivity(forwardIntent)
            finish()
            return
        }

        val isSocial = getSocialWidgetsCatalog().any { it.receiverClass.name == widgetClassName }
        if (isSocial) {
            val forwardIntent = Intent(this, SocialConfigActivity::class.java).apply {
                intent?.extras?.let { putExtras(it) }
                addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            }
            startActivity(forwardIntent)
            finish()
            return
        }

        val isWeather = getWeatherWidgetsCatalog().any { it.receiverClass.name == widgetClassName }
        if (isWeather) {
            val forwardIntent = Intent(this, WeatherConfigActivity::class.java).apply {
                intent?.extras?.let { putExtras(it) }
                addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            }
            startActivity(forwardIntent)
            finish()
            return
        }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF0A0A0C), surface = Color(0xFF16161B))) {
                val context = LocalContext.current

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
                            getProductivityWidgetsCatalog() +
                            getQuickTogglesWidgetsCatalog() +
                            getQuotesWidgetsCatalog() +
                            getSocialWidgetsCatalog() +
                            getWeatherWidgetsCatalog()

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

                val isQuickToggles = remember(widgetClassName) {
                    getQuickTogglesWidgetsCatalog().any { it.receiverClass.name == widgetClassName }
                }


                // -----------------------------------------------------------------
                // LIVE HOMESCREEN BOUNDS & RATIO EXTRACTION
                // -----------------------------------------------------------------
                val (screenWDp, screenHDp) = remember(appWidgetId, isResponsive) {
                    val manager = AppWidgetManager.getInstance(context)
                    val options = manager?.getAppWidgetOptions(appWidgetId)
                    val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

                    val fallbackSize = when (catalogItem?.sizeText) {
                        "4x1" -> 280 to 70
                        "4x2" -> 280 to 130
                        "3x1" -> 220 to 70
                        "3x2" -> 220 to 130
                        "2x1" -> 160 to 80
                        "1x2" -> 80 to 160
                        "1x1" -> 80 to 80
                        else -> 150 to 150
                    }

                    val rawW = if (isLandscape) {
                        options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0).takeIf { it != null && it > 0 }
                            ?: options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, fallbackSize.first) ?: fallbackSize.first
                    } else {
                        options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).takeIf { it != null && it > 0 }
                            ?: options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, fallbackSize.first) ?: fallbackSize.first
                    }

                    val rawH = if (isLandscape) {
                        options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0).takeIf { it != null && it > 0 }
                            ?: options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, fallbackSize.second) ?: fallbackSize.second
                    } else {
                        options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0).takeIf { it != null && it > 0 }
                            ?: options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, fallbackSize.second) ?: fallbackSize.second
                    }

                    (if (rawW > 0) rawW else fallbackSize.first) to (if (rawH > 0) rawH else fallbackSize.second)
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
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isQuickToggles) {
                                QuickToggleLivePreview(
                                    widgetClassName = widgetClassName,
                                    config = SlateWidgetConfig(
                                        themeMode = if (isLightBg) "LIGHT" else "DARK",
                                        backgroundColorHex = selectedBgHex,
                                        opacity = opacity,
                                        accentColorHex = selectedAccentHex
                                    ),
                                    isResponsive = isResponsive,
                                    appWidgetId = appWidgetId
                                )
                            } else {
                                val currentConfig = SlateWidgetConfig(
                                    themeMode = if (isLightBg) "LIGHT" else "DARK",
                                    backgroundColorHex = selectedBgHex,
                                    opacity = opacity,
                                    accentColorHex = selectedAccentHex
                                )

                                val previewBitmap = remember(selectedBgHex, selectedAccentHex, opacity, isResponsive, screenWDp, screenHDp, widgetClassName) {
                                    try {
                                        val receiverClass = Class.forName(widgetClassName)
                                        val receiver = receiverClass.getDeclaredConstructor().newInstance()

                                        val targetAspect = try {
                                            val getter = receiverClass.methods.firstOrNull { it.name == "getTargetAspect" }
                                            (getter?.invoke(receiver) as? Float) ?: 1.0f
                                        } catch (_: Exception) {
                                            1.0f
                                        }

                                        val effWDp: Int
                                        val effHDp: Int
                                        if (!isResponsive && targetAspect > 0f) {
                                            val curAspect = screenWDp.toFloat() / screenHDp.toFloat()
                                            if (curAspect > targetAspect) {
                                                effWDp = maxOf(1, (screenHDp * targetAspect).toInt())
                                                effHDp = screenHDp
                                            } else {
                                                effWDp = screenWDp
                                                effHDp = maxOf(1, (screenWDp / targetAspect).toInt())
                                            }
                                        } else {
                                            effWDp = screenWDp
                                            effHDp = screenHDp
                                        }

                                        val method6 = receiverClass.methods.firstOrNull {
                                            (it.name == "renderWidgetBitmap" || it.name == "renderBitmap") && it.parameterTypes.size == 6
                                        }
                                        if (method6 != null) {
                                            method6.invoke(receiver, context, appWidgetId, currentConfig, isResponsive, effWDp, effHDp) as? Bitmap
                                        } else {
                                            val method5 = receiverClass.methods.firstOrNull {
                                                (it.name == "renderWidgetBitmap" || it.name == "renderBitmap") && it.parameterTypes.size == 5
                                            }
                                            if (method5 != null) {
                                                val types = method5.parameterTypes
                                                if (types[1] == SlateWidgetConfig::class.java && types[2] == java.lang.Boolean.TYPE) {
                                                    method5.invoke(receiver, context, currentConfig, isResponsive, effWDp, effHDp) as? Bitmap
                                                } else if (types[1] == java.lang.Integer.TYPE && types[2] == SlateWidgetConfig::class.java) {
                                                    method5.invoke(receiver, context, appWidgetId, currentConfig, effWDp, effHDp) as? Bitmap
                                                } else {
                                                    null
                                                }
                                            } else {
                                                null
                                            }
                                        }
                                    } catch (e: Exception) {
                                        null
                                    }
                                }

                                if (previewBitmap != null) {
                                    val cellAspect = (screenWDp.toFloat() / screenHDp.toFloat()).coerceIn(0.25f, 4.5f)
                                    val maxBoxW = 280f
                                    val maxBoxH = 150f

                                    val (cellBoxW, cellBoxH) = if (cellAspect > (maxBoxW / maxBoxH)) {
                                        maxBoxW.dp to (maxBoxW / cellAspect).dp
                                    } else {
                                        (maxBoxH * cellAspect).dp to maxBoxH.dp
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(cellBoxW, cellBoxH)
                                            .clip(RoundedCornerShape(20.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val bmpAspect = previewBitmap.width.toFloat() / previewBitmap.height.toFloat()
                                        val (imgW, imgH) = if (bmpAspect > (cellBoxW.value / cellBoxH.value)) {
                                            cellBoxW to (cellBoxW / bmpAspect)
                                        } else {
                                            (cellBoxH * bmpAspect) to cellBoxH
                                        }
                                        Image(
                                            bitmap = previewBitmap.asImageBitmap(),
                                            contentDescription = "Widget Preview",
                                            modifier = Modifier
                                                .size(imgW, imgH)
                                                .clip(RoundedCornerShape(16.dp))
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(148.dp)
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(Color(selectedBgHex).copy(alpha = opacity)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = widgetName, color = textColor, fontSize = 13.sp)
                                    }
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
        updateAllQuickTogglesWidgets(this)
        updateAllQuotesWidgets(this)
        updateAllSocialWidgets(this)
        updateAllWeatherWidgets(this)

        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        finish()
    }
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