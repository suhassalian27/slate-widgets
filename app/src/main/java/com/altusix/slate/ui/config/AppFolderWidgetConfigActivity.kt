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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.altusix.slate.widgets.appfolder.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppFolderWidgetConfigActivity : ComponentActivity() {

    data class InstalledAppItem(val label: String, val packageName: String, val icon: Drawable? = null)
    private enum class AppFolderColorTarget { BACKGROUND, ACCENT }

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetClassName: String = ""
    private var slotCount = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val appWidgetInfo = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)
        widgetClassName = appWidgetInfo?.provider?.className ?: ""
        slotCount = determineSlotCount(widgetClassName)

        val catalogItem = getAppFolderWidgetsCatalog().find { it.receiverClass.name == widgetClassName }
        val hasModeOption = catalogItem?.hasModeOption ?: true
        val defaultTheme = ThemePreferences(this).getThemeSettings()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF0A0A0C), surface = Color(0xFF16161B))) {
                var folderConfig by remember { mutableStateOf(AppFolderWidgetConfig.load(this@AppFolderWidgetConfigActivity, widgetId, slotCount)) }
                var selectedBgHex by remember { mutableLongStateOf(0xFF161618L) }
                var selectedAccentHex by remember { mutableLongStateOf(defaultTheme.accentHex) }
                var opacity by remember { mutableFloatStateOf(1.0f) }
                var isResponsive by remember { mutableStateOf(true) }
                var activePickerTarget by remember { mutableStateOf<AppFolderColorTarget?>(null) }

                var installedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
                var searchQuery by remember { mutableStateOf("") }
                var selectedTabKey by remember { mutableStateOf("APPS") }

                val tabs = remember {
                    listOf(
                        ConfigTabItem("APPS", "Apps"),
                        ConfigTabItem("STYLE", "Widget Theme")
                    )
                }

                LaunchedEffect(widgetId) {
                    val prefs = getSharedPreferences("slate_widget_prefs", MODE_PRIVATE)
                    opacity = prefs.getFloat("widget_${widgetId}_opacity", 1.0f)
                    isResponsive = prefs.getBoolean("widget_${widgetId}_is_responsive", true)
                    selectedBgHex = prefs.getLong("widget_${widgetId}_bg_color", 0xFF161618L)
                    selectedAccentHex = prefs.getLong("widget_${widgetId}_accent_color", defaultTheme.accentHex)

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
                    AppFolderWidgetConfig.save(this@AppFolderWidgetConfigActivity, widgetId, folderConfig)
                    saveSlateWidgetConfig(this@AppFolderWidgetConfigActivity, widgetId, currentSlateConfig, isResponsive)
                    updateAllAppFolderWidgets(this@AppFolderWidgetConfigActivity)
                    val resultIntent = Intent().apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }

                SlateConfigScaffold(
                    title = "App Folder",
                    accentColor = Color(selectedAccentHex),
                    tabs = tabs,
                    selectedTabKey = selectedTabKey,
                    onTabSelected = { selectedTabKey = it },
                    onBackClick = { finish() },
                    onSaveClick = { saveAndFinish() },
                    scrollable = selectedTabKey == "STYLE",
                    previewHeight = 180.dp,
                    previewContent = {
                        val context = LocalContext.current
                        val previewBitmap = remember(folderConfig, currentSlateConfig, isResponsive, widgetClassName) {
                            when {
                                widgetClassName.contains("Triangle4") -> generateAppFolderTriangle4Bitmap(context, currentSlateConfig, folderConfig, false, 140, 140, 0)
                                widgetClassName.contains("Horizontal3") -> generateAppFolderHorizontal3Bitmap(context, currentSlateConfig, folderConfig, isResponsive, 180, 80, 0)
                                widgetClassName.contains("Vertical3") -> generateAppFolderVertical3Bitmap(context, currentSlateConfig, folderConfig, isResponsive, 80, 180, 0)
                                widgetClassName.contains("Row4") -> generateAppFolderRow4Bitmap(context, currentSlateConfig, folderConfig, isResponsive, 220, 80, 0)
                                widgetClassName.contains("Row5") -> generateAppFolderRow5Bitmap(context, currentSlateConfig, folderConfig, isResponsive, 240, 80, 0)
                                widgetClassName.contains("Circle6") -> generateAppFolderCircle6Bitmap(context, currentSlateConfig, folderConfig, isResponsive, 140, 140, 0)
                                widgetClassName.contains("Bento7") -> generateAppFolderBento7Bitmap(context, currentSlateConfig, folderConfig, isResponsive, 140, 140, 0)
                                widgetClassName.contains("Grid9") -> generateAppFolderGrid9Bitmap(context, currentSlateConfig, folderConfig, isResponsive, 140, 140, 0)
                                widgetClassName.contains("Bento10Left") -> generateAppFolderBento10LeftBitmap(context, currentSlateConfig, folderConfig, isResponsive, 220, 120, 0)
                                widgetClassName.contains("Bento10Top") -> generateAppFolderBento10TopBitmap(context, currentSlateConfig, folderConfig, isResponsive, 220, 120, 0)
                                widgetClassName.contains("Folder8") -> generateAppFolder8Bitmap(context, currentSlateConfig, folderConfig, isResponsive, 220, 120, 0)
                                else -> generateAppFolder4Bitmap(context, currentSlateConfig, folderConfig, isResponsive, 130, 130, 0)
                            }
                        }

                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Folder Preview",
                            modifier = Modifier.size(if (slotCount >= 8) 180.dp else 130.dp)
                        )
                    }
                ) {
                    if (selectedTabKey == "APPS") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ASSIGNED APPS (${folderConfig.slots.count { it.isConfigured }}/$slotCount)",
                                color = Color(0xFF8E8E93),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )

                            if (folderConfig.slots.any { it.isConfigured }) {
                                Text(
                                    text = "Clear All",
                                    color = Color(0xFFFF453A),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        folderConfig = folderConfig.copy(slots = List(slotCount) { AppSlotConfig() })
                                    }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF141418))
                                .border(1.dp, Color(0xFF24242C), RoundedCornerShape(16.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (i in 0 until slotCount) {
                                val slot = folderConfig.slots.getOrElse(i) { AppSlotConfig() }
                                val appItem = installedApps.find { it.packageName == slot.packageName }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (slot.isConfigured) Color(0xFF22222A) else Color(0xFF18181E))
                                        .border(
                                            width = 1.dp,
                                            color = if (slot.isConfigured) Color(selectedAccentHex).copy(alpha = 0.6f) else Color(0xFF26262E),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            if (slot.isConfigured) {
                                                val updatedSlots = folderConfig.slots.toMutableList()
                                                updatedSlots[i] = AppSlotConfig()
                                                folderConfig = folderConfig.copy(slots = updatedSlots)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (slot.isConfigured && appItem?.icon != null) {
                                        Image(
                                            bitmap = appItem.icon.toImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(2.dp)
                                                .size(15.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFF3B30)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = Color.White,
                                                modifier = Modifier.size(9.dp)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "${i + 1}",
                                            color = Color(0xFF636366),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF141418))
                                    .border(1.dp, Color(0xFF24242C), RoundedCornerShape(14.dp))
                                    .clickable { folderConfig = folderConfig.copy(showAppNames = !folderConfig.showAppNames) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("App Labels", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Switch(
                                    checked = folderConfig.showAppNames,
                                    onCheckedChange = { folderConfig = folderConfig.copy(showAppNames = it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = Color(selectedAccentHex),
                                        uncheckedThumbColor = Color(0xFF8E8E93),
                                        uncheckedTrackColor = Color(0xFF1C1C22)
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF141418))
                                    .border(1.dp, Color(0xFF24242C), RoundedCornerShape(14.dp))
                                    .clickable { folderConfig = folderConfig.copy(showTileBackground = !folderConfig.showTileBackground) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tile Cards", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Switch(
                                    checked = folderConfig.showTileBackground,
                                    onCheckedChange = { folderConfig = folderConfig.copy(showTileBackground = it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = Color(selectedAccentHex),
                                        uncheckedThumbColor = Color(0xFF8E8E93),
                                        uncheckedTrackColor = Color(0xFF1C1C22)
                                    )
                                )
                            }
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search installed apps...", color = Color(0xFF8E8E93), fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(selectedAccentHex),
                                unfocusedBorderColor = Color(0xFF24242C),
                                focusedContainerColor = Color(0xFF141418),
                                unfocusedContainerColor = Color(0xFF141418),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        val filteredApps = remember(searchQuery, installedApps) {
                            if (searchQuery.isBlank()) installedApps
                            else installedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredApps) { app ->
                                val isSelected = folderConfig.slots.any { it.packageName == app.packageName }
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color(0xFF22222A) else Color(0xFF141418))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color(selectedAccentHex) else Color(0xFF24242C),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            val updatedSlots = folderConfig.slots.toMutableList()
                                            if (isSelected) {
                                                val idx = updatedSlots.indexOfFirst { it.packageName == app.packageName }
                                                if (idx != -1) updatedSlots[idx] = AppSlotConfig()
                                            } else {
                                                val emptyIdx = updatedSlots.indexOfFirst { !it.isConfigured }
                                                if (emptyIdx != -1) {
                                                    updatedSlots[emptyIdx] = AppSlotConfig(
                                                        packageName = app.packageName,
                                                        appName = app.label,
                                                        isConfigured = true
                                                    )
                                                }
                                            }
                                            folderConfig = folderConfig.copy(slots = updatedSlots)
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (app.icon != null) {
                                        Image(
                                            bitmap = app.icon.toImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF2C2C30))
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = app.label,
                                        color = if (isSelected) Color.White else Color(0xFF8E8E93),
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
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
                                    activePickerTarget = AppFolderColorTarget.BACKGROUND
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
                                    onClick = { activePickerTarget = AppFolderColorTarget.ACCENT }
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
                                                .background(if (isSelected) Color(0xFF2C2C30) else Color.Transparent)
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

                activePickerTarget?.let { target ->
                    val initialColor = if (target == AppFolderColorTarget.BACKGROUND) Color(selectedBgHex) else Color(selectedAccentHex)
                    CustomColorPickerDialog(
                        initialColor = initialColor,
                        title = if (target == AppFolderColorTarget.BACKGROUND) "Custom Background" else "Custom Accent",
                        onDismiss = { activePickerTarget = null },
                        onColorSelected = { color ->
                            val hex = (color.toArgb().toLong() and 0xFFFFFFFFL)
                            if (target == AppFolderColorTarget.BACKGROUND) selectedBgHex = hex else selectedAccentHex = hex
                            activePickerTarget = null
                        }
                    )
                }
            }
        }
    }

    private fun determineSlotCount(className: String): Int {
        return when {
            className.contains("10") -> 10
            className.contains("9") -> 9
            className.contains("8") -> 8
            className.contains("7") -> 7
            className.contains("6") -> 6
            className.contains("5") -> 5
            className.contains("3") -> 3
            else -> 4
        }
    }

    private fun calculateLuminance(hex: Long): Float {
        val r = ((hex shr 16) and 0xFFL) / 255f
        val g = ((hex shr 8) and 0xFFL) / 255f
        val b = (hex and 0xFFL) / 255f
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    private fun saveSlateWidgetConfig(context: Context, widgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean) {
        val prefs = context.getSharedPreferences("slate_widget_prefs", MODE_PRIVATE)
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
