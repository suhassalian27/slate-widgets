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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
        val widgetName = catalogItem?.name ?: ""
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
                var selectedIconTab by remember {
                    mutableIntStateOf(
                        when (config.iconType) {
                            LauncherIconType.APP_ICON -> 0
                            LauncherIconType.EMOJI -> 1
                            LauncherIconType.VECTOR_ICON -> 2
                            LauncherIconType.CUSTOM_TEXT -> 3
                        }
                    )
                }
                var selectedTabKey by remember { mutableStateOf("APP") }

                val tabs = remember {
                    listOf(
                        ConfigTabItem("APP", "App"),
                        ConfigTabItem("STYLE", "Widget Theme")
                    )
                }

                val emojis = remember {
                    listOf("😂", "❤️", "😍", "🤣", "😊", "🙏", "😭", "🥰", "😘", "👍", "💕", "😁", "🔥", "🥺", "😅", "🤔", "😎", "😢", "👏", "🙌", "✨", "🚀", "🎧", "🎮", "⚡")
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
                        }.sortedBy { it.label }

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
                    AppLauncherWidgetConfig.save(this@AppLauncherConfigActivity, appWidgetId, config)
                    saveSlateWidgetConfig(this@AppLauncherConfigActivity, appWidgetId, currentSlateConfig, isResponsive)
                    updateAllAppLauncherWidgets(this@AppLauncherConfigActivity)
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
                        val previewBitmap = remember(config, currentSlateConfig, isResponsive, widgetClassName) {
                            if (widgetClassName.contains("Pill", ignoreCase = true)) {
                                generatePillLauncherBitmap(context, currentSlateConfig, config, 200, 100)
                            } else if (is2x1Widget) {
                                generateRectangleLauncherBitmap(context, currentSlateConfig, config, 200, 100)
                            } else {
                                generateAdaptiveLauncherBitmap(context, currentSlateConfig, config, 120, 120)
                            }
                        }
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Preview",
                            modifier = if (is2x1Widget) Modifier.size(width = 180.dp, height = 90.dp) else Modifier.size(120.dp)
                        )
                    }
                ) {
                    if (selectedTabKey == "APP") {
                        SectionTitle(title = "Target Application")

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF141418))
                                .border(1.dp, Color(0xFF24242C), RoundedCornerShape(16.dp))
                                .clickable { showAppPickerSheet = true }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (selectedApp?.icon != null) {
                                        Image(
                                            bitmap = selectedApp.icon.toImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(30.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF24242C)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                tint = Color(0xFF8E8E93),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }

                                    Text(
                                        text = selectedApp?.label ?: "Select Application...",
                                        color = if (selectedApp != null) Color.White else Color(0xFF8E8E93),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color(0xFF8E8E93)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        SectionTitle(title = "Icon Style")

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF141418))
                                .border(1.dp, Color(0xFF22222A), RoundedCornerShape(14.dp))
                                .padding(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("App Icon", "Emoji", "Icons", "Text").forEachIndexed { index, label ->
                                    val isSelected = selectedIconTab == index
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(if (isSelected) Color(0xFF26262E) else Color.Transparent)
                                            .clickable {
                                                selectedIconTab = index
                                                config = when (index) {
                                                    0 -> config.copy(iconType = LauncherIconType.APP_ICON)
                                                    1 -> config.copy(iconType = LauncherIconType.EMOJI)
                                                    2 -> config.copy(iconType = LauncherIconType.VECTOR_ICON)
                                                    else -> config.copy(iconType = LauncherIconType.CUSTOM_TEXT)
                                                }
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

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 130.dp, max = 220.dp)
                        ) {
                            when (selectedIconTab) {
                                1 -> {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(5),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(emojis) { emoji ->
                                            val isSelected = config.selectedEmoji == emoji
                                            Box(
                                                modifier = Modifier
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (isSelected) Color(0xFF26262E) else Color(0xFF141418))
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) Color(selectedAccentHex) else Color(0xFF24242C),
                                                        RoundedCornerShape(14.dp)
                                                    )
                                                    .clickable { config = config.copy(selectedEmoji = emoji) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = emoji, fontSize = 22.sp)
                                            }
                                        }
                                    }
                                }
                                2 -> {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(5),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(vectorIcons) { iconItem ->
                                            val isSelected = config.selectedVectorResName.equals(iconItem.name, ignoreCase = true)
                                            Box(
                                                modifier = Modifier
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (isSelected) Color(0xFF26262E) else Color(0xFF141418))
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) Color(selectedAccentHex) else Color(0xFF24242C),
                                                        RoundedCornerShape(14.dp)
                                                    )
                                                    .clickable { config = config.copy(selectedVectorResName = iconItem.name) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = iconItem.imageVector,
                                                    contentDescription = iconItem.name,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                3 -> {
                                    OutlinedTextField(
                                        value = config.customText,
                                        onValueChange = { config = config.copy(customText = it) },
                                        placeholder = { Text("Display text", color = Color(0xFF8E8E93)) },
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
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Displays the standard adaptive app icon.",
                                            color = Color(0xFF8E8E93),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
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
                    }
                }

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
        containerColor = Color(0xFF141418),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF2C2C35)) },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Application",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22222A))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search apps...", color = Color(0xFF8E8E93), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color(0xFF24242C),
                    focusedContainerColor = Color(0xFF0C0C0E),
                    unfocusedContainerColor = Color(0xFF0C0C0E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredApps) { app ->
                    val isSelected = app.packageName == selectedPackageName
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF22222A) else Color.Transparent)
                            .clickable { onAppSelected(app) }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (app.icon != null) {
                            Image(bitmap = app.icon.toImageBitmap(), contentDescription = null, modifier = Modifier.size(36.dp))
                        } else {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF2C2C30)))
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = app.label,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )

                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = accentColor, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}