package com.altusix.slate.ui.config

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altusix.slate.core.theme.ThemePreferences
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.ui.components.*
import com.altusix.slate.widgets.applauncher.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppLauncherConfigActivity : ComponentActivity() {

    data class InstalledAppItem(val label: String, val packageName: String, val icon: Drawable? = null)
    private enum class LauncherColorTarget { BACKGROUND, ACCENT }

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetClassName: String = ""

    private fun navigateToHomeScreenAndFinish() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    @OptIn(ExperimentalMaterial3Api::class)
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

        val catalogItem = getAppLauncherWidgetsCatalog().find { it.receiverClass.name == widgetClassName }
        val widgetName = catalogItem?.name ?: "App Launcher"
        val hasModeOption = catalogItem?.hasModeOption ?: false
        val defaultTheme = ThemePreferences(this).getThemeSettings()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF0A0A0C), surface = Color(0xFF16161B))) {
                var config by remember { mutableStateOf(AppLauncherWidgetConfig.load(this@AppLauncherConfigActivity, appWidgetId)) }
                var selectedBgHex by remember { mutableLongStateOf(0xFF161618L) }
                var selectedAccentHex by remember { mutableLongStateOf(defaultTheme.accentHex) }
                var opacity by remember { mutableFloatStateOf(1.0f) }
                var isResponsive by remember { mutableStateOf(true) }
                var activePickerTarget by remember { mutableStateOf<LauncherColorTarget?>(null) }

                var installedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
                var showAppPickerSheet by remember { mutableStateOf(false) }
                var showEmojiPickerSheet by remember { mutableStateOf(false) }
                var showVectorPickerSheet by remember { mutableStateOf(false) }

                var selectedTabKey by remember { mutableStateOf("APP") }

                val tabs = remember {
                    listOf(
                        ConfigTabItem("APP", "App"),
                        ConfigTabItem("STYLE", "Widget Theme")
                    )
                }

                val emojis = remember {
                    listOf(
                        "🚀", "🔥", "⚡", "✨", "🎧", "🎮",
                        "❤️", "😂", "😎", "👍", "☕", "💡",
                        "🌟", "🎉", "📚", "💪", "🍀", "🎯",
                        "🍕", "🏀", "✈️", "🌙", "🎵", "🔔"
                    )
                }
                val vectorIcons = AppLauncherVectorIcons.icons

                val is2x1Widget = remember(widgetClassName) {
                    widgetClassName.contains("CustomText", ignoreCase = true) ||
                            widgetClassName.contains("Rectangle", ignoreCase = true) ||
                            widgetClassName.contains("Pill", ignoreCase = true) ||
                            widgetClassName.contains("Glitch", ignoreCase = true)
                }

                LaunchedEffect(appWidgetId) {
                    val prefs = getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
                    opacity = prefs.getFloat("widget_${appWidgetId}_opacity", 1.0f)
                    isResponsive = prefs.getBoolean("widget_${appWidgetId}_is_responsive", true)
                    selectedBgHex = prefs.getLong("widget_${appWidgetId}_bg_color", 0xFF161618L)
                    selectedAccentHex = prefs.getLong("widget_${appWidgetId}_accent_color", defaultTheme.accentHex)

                    withContext(Dispatchers.IO) {
                        val pm = packageManager
                        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }

                        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
                        } else {
                            @Suppress("DEPRECATION")
                            pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
                        }

                        val apps = resolved.map {
                            InstalledAppItem(
                                label = it.loadLabel(pm).toString(),
                                packageName = it.activityInfo.packageName,
                                icon = try { it.loadIcon(pm) } catch (_: Exception) { null }
                            )
                        }
                            .distinctBy { it.packageName }
                            .sortedBy { it.label }

                        withContext(Dispatchers.Main) { installedApps = apps }
                    }
                }

                val currentSlateConfig = remember(selectedBgHex, selectedAccentHex, opacity) {
                    val themeMode = if (calculateLuminance(selectedBgHex) > 0.5f) "LIGHT" else "DARK"
                    SlateWidgetConfig(
                        themeMode = themeMode,
                        backgroundColorHex = selectedBgHex,
                        opacity = opacity,
                        accentColorHex = selectedAccentHex
                    )
                }

                fun saveAndFinish() {
                    val finalConfig = config.copy(isResponsive = isResponsive)
                    AppLauncherWidgetConfig.save(this@AppLauncherConfigActivity, appWidgetId, finalConfig)
                    saveSlateWidgetConfig(this@AppLauncherConfigActivity, appWidgetId, currentSlateConfig, isResponsive)
                    updateAllAppLauncherWidgets(this@AppLauncherConfigActivity)

                    val manager = AppWidgetManager.getInstance(this@AppLauncherConfigActivity)
                    val info = manager.getAppWidgetInfo(appWidgetId)
                    if (info?.provider != null) {
                        val updateIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                            component = info.provider
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
                        }
                        sendBroadcast(updateIntent)
                    }

                    setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                    navigateToHomeScreenAndFinish()
                }

                val selectedApp = installedApps.find { it.packageName == config.packageName }

                SlateConfigScaffold(
                    title = "App Launcher",
                    subtitle = widgetName.ifEmpty { null },
                    accentColor = Color(selectedAccentHex),
                    tabs = tabs,
                    selectedTabKey = selectedTabKey,
                    onTabSelected = { selectedTabKey = it },
                    onBackClick = { finish() },
                    onSaveClick = { saveAndFinish() },
                    scrollable = true,
                    previewHeight = 180.dp,
                    previewContent = {
                        val context = LocalContext.current

                        val (screenWDp, screenHDp) = remember(appWidgetId, isResponsive) {
                            val manager = AppWidgetManager.getInstance(context)
                            val options = manager?.getAppWidgetOptions(appWidgetId)
                            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

                            val fallbackW = if (is2x1Widget) 200 else 130
                            val fallbackH = if (is2x1Widget) 100 else 130

                            val rawW = if (isLandscape) {
                                options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0).takeIf { it != null && it > 0 }
                                    ?: options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, fallbackW) ?: fallbackW
                            } else {
                                options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).takeIf { it != null && it > 0 }
                                    ?: options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, fallbackW) ?: fallbackW
                            }

                            val rawH = if (isLandscape) {
                                options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0).takeIf { it != null && it > 0 }
                                    ?: options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, fallbackH) ?: fallbackH
                            } else {
                                options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0).takeIf { it != null && it > 0 }
                                    ?: options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, fallbackH) ?: fallbackH
                            }

                            val finalW = if (rawW <= 0) fallbackW else rawW
                            val finalH = if (rawH <= 0) fallbackH else rawH
                            finalW to finalH
                        }

                        val previewConfig = remember(config, isResponsive) {
                            config.copy(isResponsive = isResponsive)
                        }
                        val previewBitmap = remember(previewConfig, currentSlateConfig, isResponsive, widgetClassName, screenWDp, screenHDp) {
                            when {
                                widgetClassName.contains("Pill", ignoreCase = true) ->
                                    generatePillLauncherBitmap(context, currentSlateConfig, previewConfig, screenWDp, screenHDp)
                                is2x1Widget ->
                                    generateRectangleLauncherBitmap(context, currentSlateConfig, previewConfig, screenWDp, screenHDp)
                                else ->
                                    generateAdaptiveLauncherBitmap(context, currentSlateConfig, previewConfig, screenWDp, screenHDp)
                            }
                        }

                        val aspect = (screenWDp.toFloat() / screenHDp.toFloat().coerceAtLeast(1f)).coerceIn(0.25f, 4.0f)
                        val maxBoxW = 240f
                        val maxBoxH = 140f

                        val (dispW, dispH) = if (aspect > (maxBoxW / maxBoxH)) {
                            maxBoxW.dp to (maxBoxW / aspect).dp
                        } else {
                            (maxBoxH * aspect).dp to maxBoxH.dp
                        }

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Preview",
                                modifier = Modifier.size(dispW, dispH)
                            )
                        }
                    }
                ) {
                    if (selectedTabKey == "APP") {
                        // 1. Target Application Tile (Clean label without package text)
                        SectionTitle(title = "Target Application")

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF141418))
                                .border(1.dp, Color(0xFF24242C), RoundedCornerShape(16.dp))
                                .clickable { showAppPickerSheet = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (selectedApp?.icon != null) {
                                Image(
                                    bitmap = selectedApp.icon.toImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(38.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(selectedAccentHex).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Apps,
                                        contentDescription = null,
                                        tint = Color(selectedAccentHex),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Text(
                                text = selectedApp?.label ?: "Select Application",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF636366),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // 2. Icon Style Segmented Bar
                        SectionTitle(title = "Icon Style")

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF141418))
                                .border(1.dp, Color(0xFF24242C), RoundedCornerShape(14.dp))
                                .padding(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    LauncherIconType.APP_ICON to "App Icon",
                                    LauncherIconType.EMOJI to "Emoji",
                                    LauncherIconType.VECTOR_ICON to "Icons",
                                    LauncherIconType.CUSTOM_TEXT to "Text"
                                ).forEach { (type, label) ->
                                    val isSelected = config.iconType == type
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color(0xFF26262E) else Color.Transparent)
                                            .clickable {
                                                config = config.copy(iconType = type)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.White else Color(0xFF8E8E93),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Clean Selection Tiles (Triggers Modal Sheets)
                        when (config.iconType) {
                            LauncherIconType.APP_ICON -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF141418))
                                        .border(1.dp, Color(0xFF24242C), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (selectedApp?.icon != null) {
                                        Image(
                                            bitmap = selectedApp.icon.toImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF24242C))
                                        )
                                    }
                                    Text(
                                        text = "Native ${selectedApp?.label ?: "App"} Icon",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            LauncherIconType.EMOJI -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF141418))
                                        .border(1.dp, Color(0xFF24242C), RoundedCornerShape(16.dp))
                                        .clickable { showEmojiPickerSheet = true }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF22222A)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = config.selectedEmoji.ifEmpty { "🚀" }, fontSize = 20.sp)
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Choose Emoji",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Selected: ${config.selectedEmoji.ifEmpty { "🚀" }}",
                                            color = Color(0xFF8E8E93),
                                            fontSize = 12.sp
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Color(0xFF636366),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            LauncherIconType.VECTOR_ICON -> {
                                val currentIcon = vectorIcons.find { it.name.equals(config.selectedVectorResName, ignoreCase = true) }
                                    ?: vectorIcons.first()

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF141418))
                                        .border(1.dp, Color(0xFF24242C), RoundedCornerShape(16.dp))
                                        .clickable { showVectorPickerSheet = true }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(selectedAccentHex).copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = currentIcon.imageVector,
                                            contentDescription = null,
                                            tint = Color(selectedAccentHex),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Choose Vector Icon",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = currentIcon.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                            color = Color(0xFF8E8E93),
                                            fontSize = 12.sp
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Color(0xFF636366),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            LauncherIconType.CUSTOM_TEXT -> {
                                OutlinedTextField(
                                    value = config.customText,
                                    onValueChange = { config = config.copy(customText = it) },
                                    placeholder = { Text("Display text (e.g. Read, Notes)", color = Color(0xFF8E8E93), fontSize = 13.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(selectedAccentHex),
                                        unfocusedBorderColor = Color(0xFF24242C),
                                        focusedContainerColor = Color(0xFF141418),
                                        unfocusedContainerColor = Color(0xFF141418),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(36.dp))
                    } else {
                        // TAB 2: WIDGET THEME & STYLE
                        val isLightBg = calculateLuminance(selectedBgHex) > 0.5f

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle(title = "Background")
                            val bgPresets = listOf(
                                0xFF161618L to "Matte",
                                0xFF000000L to "AMOLED",
                                0xFFFFFFFFL to "Light"
                            )

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
                                        if (hex == 0xFFFFFFFFL && selectedAccentHex == 0xFFFFFFFFL) selectedAccentHex = 0xFF000000L
                                        else if (hex != 0xFFFFFFFFL && selectedAccentHex == 0xFF000000L) selectedAccentHex = 0xFFFFFFFFL
                                    }
                                }

                                val isCustomBg = bgPresets.none { it.first == selectedBgHex }
                                RainbowPickerChip(
                                    isSelected = isCustomBg,
                                    activeColor = if (isCustomBg) Color(selectedBgHex) else null,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    activePickerTarget = LauncherColorTarget.BACKGROUND
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionTitle(title = "Accent Color")
                            val accentPresets = if (isLightBg) {
                                listOf(0xFF000000L, 0xFF00D166L, 0xFF2B80FFL, 0xFFFF3B30L, 0xFFFF9500L, 0xFFAF52DEL)
                            } else {
                                listOf(0xFFFFFFFFL, 0xFF00D166L, 0xFF2B80FFL, 0xFFFF3B30L, 0xFFFF9500L, 0xFFAF52DEL)
                            }

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
                                    onClick = { activePickerTarget = LauncherColorTarget.ACCENT }
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
                            ModernOpacitySlider(value = opacity, onValueChange = { opacity = it })
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
                                    listOf(true to "Responsive", false to "Fixed Aspect").forEach { (responsiveVal, label) ->
                                        val isSelected = isResponsive == responsiveVal
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) Color(0xFF26262E) else Color.Transparent)
                                                .clickable { isResponsive = responsiveVal },
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

                        Spacer(modifier = Modifier.height(36.dp))
                    }
                }

                // -------------------------------------------------------------
                // MODAL BOTTOM SHEETS
                // -------------------------------------------------------------

                // 1. App Picker Sheet
                if (showAppPickerSheet) {
                    AppPickerBottomSheet(
                        installedApps = installedApps,
                        selectedPackageName = config.packageName,
                        accentColor = Color(selectedAccentHex),
                        onDismiss = { showAppPickerSheet = false },
                        onAppSelected = { app ->
                            config = config.copy(packageName = app.packageName, customText = app.label)
                            showAppPickerSheet = false
                        }
                    )
                }

                // 2. Emoji Picker Sheet
                if (showEmojiPickerSheet) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                    ModalBottomSheet(
                        onDismissRequest = { showEmojiPickerSheet = false },
                        sheetState = sheetState,
                        containerColor = Color(0xFF121216),
                        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF32323E)) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.65f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Select Emoji",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = { showEmojiPickerSheet = false }) {
                                    Text("Done", color = Color(selectedAccentHex), fontWeight = FontWeight.Bold)
                                }
                            }

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(6),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 20.dp)
                            ) {
                                items(emojis) { emoji ->
                                    val isSelected = config.iconType == LauncherIconType.EMOJI && config.selectedEmoji == emoji
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) Color(0xFF262630) else Color(0xFF181820))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) Color(selectedAccentHex) else Color(0xFF262630),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                config = config.copy(iconType = LauncherIconType.EMOJI, selectedEmoji = emoji)
                                                showEmojiPickerSheet = false
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 22.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Vector Icon Picker Sheet
                if (showVectorPickerSheet) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                    ModalBottomSheet(
                        onDismissRequest = { showVectorPickerSheet = false },
                        sheetState = sheetState,
                        containerColor = Color(0xFF121216),
                        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF32323E)) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.65f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Select Icon",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = { showVectorPickerSheet = false }) {
                                    Text("Done", color = Color(selectedAccentHex), fontWeight = FontWeight.Bold)
                                }
                            }

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(6),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 20.dp)
                            ) {
                                items(vectorIcons) { iconItem ->
                                    val isSelected = config.iconType == LauncherIconType.VECTOR_ICON &&
                                            config.selectedVectorResName.equals(iconItem.name, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) Color(0xFF262630) else Color(0xFF181820))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) Color(selectedAccentHex) else Color(0xFF262630),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                config = config.copy(
                                                    iconType = LauncherIconType.VECTOR_ICON,
                                                    selectedVectorResName = iconItem.name
                                                )
                                                showVectorPickerSheet = false
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = iconItem.imageVector,
                                            contentDescription = iconItem.name,
                                            tint = if (isSelected) Color(selectedAccentHex) else Color(0xFFB0B0B8),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Color Picker Dialog
                activePickerTarget?.let { target ->
                    val initialColor = if (target == LauncherColorTarget.BACKGROUND) Color(selectedBgHex) else Color(selectedAccentHex)
                    CustomColorPickerDialog(
                        initialColor = initialColor,
                        title = if (target == LauncherColorTarget.BACKGROUND) "Custom Background" else "Custom Accent",
                        onDismiss = { activePickerTarget = null },
                        onColorSelected = { color ->
                            val hex = (color.toArgb().toLong() and 0xFFFFFFFFL)
                            if (target == LauncherColorTarget.BACKGROUND) selectedBgHex = hex else selectedAccentHex = hex
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

    private fun saveSlateWidgetConfig(context: Context, widgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean) {
        val prefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("widget_${widgetId}_theme_mode", config.themeMode)
            putLong("widget_${widgetId}_bg_color", config.backgroundColorHex)
            putFloat("widget_${widgetId}_opacity", config.opacity)
            putLong("widget_${widgetId}_accent_color", config.accentColorHex)
            putBoolean("widget_${widgetId}_is_responsive", isResponsive)
            putString("widget_${widgetId}_mode", if (isResponsive) "RESPONSIVE" else "FIXED")
            apply()
        }
    }
}

private fun Drawable.toImageBitmap(): ImageBitmap {
    val width = intrinsicWidth.coerceAtLeast(1)
    val height = intrinsicHeight.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap.asImageBitmap()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerBottomSheet(
    installedApps: List<AppLauncherConfigActivity.InstalledAppItem>,
    selectedPackageName: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    onAppSelected: (AppLauncherConfigActivity.InstalledAppItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121216),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF32323E)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Select Application",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${installedApps.size} apps installed",
                        color = Color(0xFF8E8E93),
                        fontSize = 12.sp
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("Done", color = accentColor, fontWeight = FontWeight.Bold)
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search installed apps...", color = Color(0xFF8E8E93), fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color(0xFF8E8E93),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color(0xFF282832),
                    focusedContainerColor = Color(0xFF18181E),
                    unfocusedContainerColor = Color(0xFF18181E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredApps) { app ->
                    val isSelected = app.packageName == selectedPackageName
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF22222E) else Color(0xFF16161C))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) accentColor else Color(0xFF24242C),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onAppSelected(app) }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (app.icon != null) {
                            Image(
                                bitmap = app.icon.toImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2C2C30))
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = app.label,
                            color = if (isSelected) Color.White else Color(0xFFE0E0E6),
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(accentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.Black,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
