package com.altusix.slate.widgets.social

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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SocialConfigActivity : ComponentActivity() {

    data class InstalledAppItem(val label: String, val packageName: String, val icon: Drawable? = null)
    private enum class SocialColorTarget { BACKGROUND, ACCENT }

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetClassName: String = ""
    private var slotCount = 4
    private var layoutTag = "QUAD_4"

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
        layoutTag = determineLayoutTag(widgetClassName)

        val catalogItem = getSocialWidgetsCatalog().find { it.receiverClass.name == widgetClassName }
        val hasModeOption = catalogItem?.hasModeOption ?: true
        val defaultTheme = ThemePreferences(this).getThemeSettings()
        val initialSlotIndex = intent?.getIntExtra("extra_slot_index", 0)?.coerceIn(0, slotCount - 1) ?: 0

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF0A0A0C), surface = Color(0xFF16161B))) {
                var socialConfig by remember {
                    mutableStateOf(SocialStorageManager.load(this@SocialConfigActivity, widgetId, slotCount, layoutTag))
                }
                var selectedBgHex by remember { mutableLongStateOf(0xFF161618L) }
                var selectedAccentHex by remember { mutableLongStateOf(defaultTheme.accentHex) }
                var opacity by remember { mutableFloatStateOf(1.0f) }
                var isResponsive by remember { mutableStateOf(true) }
                var activePickerTarget by remember { mutableStateOf<SocialColorTarget?>(null) }
                var activeSlotIndex by remember { mutableIntStateOf(initialSlotIndex) }

                var installedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
                var searchQuery by remember { mutableStateOf("") }
                var selectedTabKey by remember { mutableStateOf("APPS") }

                val tabs = remember {
                    listOf(
                        ConfigTabItem("APPS", "Apps & Slots"),
                        ConfigTabItem("STYLE", "Widget Theme")
                    )
                }
                val widgetName = catalogItem?.name ?: "Social Widget"

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
                    SocialStorageManager.save(this@SocialConfigActivity, widgetId, socialConfig)
                    saveSlateWidgetConfig(this@SocialConfigActivity, widgetId, currentSlateConfig, isResponsive)
                    updateAllSocialWidgets(this@SocialConfigActivity)
                    val resultIntent = Intent().apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }

                fun assignPresetToSlot(preset: SocialAppPreset) {
                    val updatedSlots = socialConfig.slots.toMutableList()
                    while (updatedSlots.size < slotCount) updatedSlots.add(SocialSlotConfig())
                    updatedSlots[activeSlotIndex] = SocialSlotConfig(
                        packageName = preset.packageName,
                        appName = preset.name,
                        presetId = preset.id,
                        isConfigured = true
                    )
                    socialConfig = socialConfig.copy(slots = updatedSlots)
                    activeSlotIndex = (activeSlotIndex + 1) % slotCount
                }

                fun assignInstalledAppToSlot(app: InstalledAppItem) {
                    val matchedPreset = SocialStorageManager.findPresetByPackage(app.packageName)
                    val updatedSlots = socialConfig.slots.toMutableList()
                    while (updatedSlots.size < slotCount) updatedSlots.add(SocialSlotConfig())
                    updatedSlots[activeSlotIndex] = SocialSlotConfig(
                        packageName = app.packageName,
                        appName = app.label,
                        presetId = matchedPreset?.id ?: "",
                        isConfigured = true
                    )
                    socialConfig = socialConfig.copy(slots = updatedSlots)
                    activeSlotIndex = (activeSlotIndex + 1) % slotCount
                }

                SlateConfigScaffold(
                    title = "Social",
                    subtitle = widgetName.ifEmpty { null },
                    accentColor = Color(selectedAccentHex),
                    tabs = tabs,
                    selectedTabKey = selectedTabKey,
                    onTabSelected = { selectedTabKey = it },
                    onBackClick = { finish() },
                    onSaveClick = { saveAndFinish() },
                    scrollable = selectedTabKey == "STYLE",
                    previewHeight = 175.dp,
                    previewContent = {
                        val context = LocalContext.current
                        val previewBitmap = remember(socialConfig, currentSlateConfig, isResponsive, widgetClassName) {
                            when {
                                widgetClassName.contains("Bar5") -> generateSocialGridBitmap(context, currentSlateConfig, socialConfig, isResponsive, 240, 70, 0, cols = 5, rows = 1)
                                widgetClassName.contains("Quad4") -> generateSocialGridBitmap(context, currentSlateConfig, socialConfig, isResponsive, 140, 140, 0, cols = 2, rows = 2)
                                widgetClassName.contains("Matrix9") -> generateSocialGridBitmap(context, currentSlateConfig, socialConfig, isResponsive, 140, 140, 0, cols = 3, rows = 3)
                                widgetClassName.contains("Deck10") -> generateSocialGridBitmap(context, currentSlateConfig, socialConfig, isResponsive, 220, 110, 0, cols = 5, rows = 2)
                                widgetClassName.contains("Bento10Top") -> generateSocialBento10TopBitmap(context, currentSlateConfig, isResponsive, 220, 110, 0)
                                widgetClassName.contains("Bento10Left") -> generateSocialBento10LeftBitmap(context, currentSlateConfig, isResponsive, 220, 110, 0)
                                widgetClassName.contains("Orbit6") -> generateSocialOrbit6Bitmap(context, currentSlateConfig, isResponsive, 140, 140, 0)
                                widgetClassName.contains("Messaging4") -> generateSocialGridBitmap(context, currentSlateConfig, socialConfig, isResponsive, 220, 70, 0, cols = 4, rows = 1)
                                widgetClassName.contains("Stream3") -> generateSocialGridBitmap(context, currentSlateConfig, socialConfig, isResponsive, 180, 70, 0, cols = 3, rows = 1)
                                widgetClassName.contains("Octa8") -> generateSocialGridBitmap(context, currentSlateConfig, socialConfig, isResponsive, 220, 110, 0, cols = 4, rows = 2)
                                widgetClassName.contains("Twin2") -> generateSocialGridBitmap(context, currentSlateConfig, socialConfig, isResponsive, 90, 160, 0, cols = 1, rows = 2)
                                widgetClassName.contains("Micro1") -> generateSocialGridBitmap(context, currentSlateConfig, socialConfig, isResponsive, 100, 100, 0, cols = 1, rows = 1)
                                else -> generateSocialGridBitmap(context, currentSlateConfig, socialConfig, isResponsive, 140, 140, 0, cols = 2, rows = 2)
                            }
                        }

                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Social Widget Preview",
                            modifier = Modifier.size(if (slotCount >= 8 || widgetClassName.contains("Bar5") || widgetClassName.contains("Messaging4")) 180.dp else 135.dp)
                        )
                    }
                ) {
                    if (selectedTabKey == "APPS") {
                        // Header info & Reset button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ASSIGNED SLOTS (${socialConfig.slots.count { it.isConfigured }}/$slotCount) - TAP TO EDIT",
                                color = Color(0xFF8E8E93),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Reset Defaults",
                                    color = Color(selectedAccentHex),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        socialConfig = socialConfig.copy(
                                            slots = SocialStorageManager.getDefaultSlotsForLayout(slotCount, layoutTag)
                                        )
                                    }
                                )
                                Text(
                                    text = "Clear All",
                                    color = Color(0xFFFF453A),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        socialConfig = socialConfig.copy(slots = List(slotCount) { SocialSlotConfig() })
                                    }
                                )
                            }
                        }

                        // Visual Slot Selector Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF141418))
                                .border(1.dp, Color(0xFF24242C), RoundedCornerShape(16.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (i in 0 until slotCount) {
                                val slot = socialConfig.slots.getOrElse(i) { SocialSlotConfig() }
                                val isSelected = activeSlotIndex == i
                                val appItem = installedApps.find { it.packageName == slot.packageName }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color(0xFF242430) else if (slot.isConfigured) Color(0xFF1A1A22) else Color(0xFF121216))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(selectedAccentHex) else if (slot.isConfigured) Color(selectedAccentHex).copy(alpha = 0.4f) else Color(0xFF24242C),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { activeSlotIndex = i },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (slot.isConfigured) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(2.dp)
                                        ) {
                                            if (appItem?.icon != null) {
                                                Image(
                                                    bitmap = appItem.icon.toImageBitmap(),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = slot.appName.take(3).uppercase(),
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Small remove button
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(2.dp)
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFF3B30))
                                                .clickable {
                                                    val updated = socialConfig.slots.toMutableList()
                                                    updated[i] = SocialSlotConfig()
                                                    socialConfig = socialConfig.copy(slots = updated)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = Color.White,
                                                modifier = Modifier.size(8.dp)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "${i + 1}",
                                            color = if (isSelected) Color(selectedAccentHex) else Color(0xFF636366),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Display Settings Row (Toggles & Icon Style)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // App Labels Toggle
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF141418))
                                    .border(1.dp, Color(0xFF24242C), RoundedCornerShape(12.dp))
                                    .clickable { socialConfig = socialConfig.copy(showAppNames = !socialConfig.showAppNames) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Labels", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Switch(
                                    checked = socialConfig.showAppNames,
                                    onCheckedChange = { socialConfig = socialConfig.copy(showAppNames = it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = Color(selectedAccentHex),
                                        uncheckedThumbColor = Color(0xFF8E8E93),
                                        uncheckedTrackColor = Color(0xFF1C1C22)
                                    ),
                                    modifier = Modifier.scale(0.8f)
                                )
                            }

                            // Tile Background Toggle
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF141418))
                                    .border(1.dp, Color(0xFF24242C), RoundedCornerShape(12.dp))
                                    .clickable { socialConfig = socialConfig.copy(showTileBackground = !socialConfig.showTileBackground) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tile Cards", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Switch(
                                    checked = socialConfig.showTileBackground,
                                    onCheckedChange = { socialConfig = socialConfig.copy(showTileBackground = it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = Color(selectedAccentHex),
                                        uncheckedThumbColor = Color(0xFF8E8E93),
                                        uncheckedTrackColor = Color(0xFF1C1C22)
                                    ),
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                        }

                        // Icon Style Segmented Control
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF141418))
                                .border(1.dp, Color(0xFF24242C), RoundedCornerShape(12.dp))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(
                                "MONOCHROME" to "Monochrome",
                                "ACCENT" to "Accent Tint",
                                "ORIGINAL" to "Brand Colors"
                            ).forEach { (styleKey, label) ->
                                val isSelected = socialConfig.iconStyle == styleKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(if (isSelected) Color(0xFF282832) else Color.Transparent)
                                        .clickable { socialConfig = socialConfig.copy(iconStyle = styleKey) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else Color(0xFF8E8E93),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Popular Social Presets Section
                        Text(
                            text = "POPULAR SOCIAL NETWORKS (ASSIGN TO SLOT ${activeSlotIndex + 1})",
                            color = Color(0xFF8E8E93),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF141418))
                                .border(1.dp, Color(0xFF24242C), RoundedCornerShape(14.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(SocialStorageManager.SOCIAL_PRESETS) { preset ->
                                val brandColor = Color(getSocialBrandColor(preset.id, isLight = false))
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1A1A20))
                                        .border(1.dp, Color(0xFF282832), RoundedCornerShape(8.dp))
                                        .clickable { assignPresetToSlot(preset) }
                                        .padding(horizontal = 6.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(brandColor)
                                    )
                                    Text(
                                        text = preset.name,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Search Bar for All Installed Apps
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search installed apps...", color = Color(0xFF8E8E93), fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
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
                            else installedApps.filter { it.label.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) }
                        }

                        // Installed Apps List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF141418))
                                .border(1.dp, Color(0xFF24242C), RoundedCornerShape(14.dp)),
                            contentPadding = PaddingValues(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredApps) { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1A1A22))
                                        .clickable { assignInstalledAppToSlot(app) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (app.icon != null) {
                                        Image(
                                            bitmap = app.icon.toImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF2C2C34)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(app.label.take(1), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.label,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = app.packageName,
                                            color = Color(0xFF8E8E93),
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
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
                                    activePickerTarget = SocialColorTarget.BACKGROUND
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
                                    onClick = { activePickerTarget = SocialColorTarget.ACCENT }
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
                    val initialColor = if (target == SocialColorTarget.BACKGROUND) Color(selectedBgHex) else Color(selectedAccentHex)
                    CustomColorPickerDialog(
                        initialColor = initialColor,
                        title = if (target == SocialColorTarget.BACKGROUND) "Custom Background" else "Custom Accent",
                        onDismiss = { activePickerTarget = null },
                        onColorSelected = { color ->
                            val hex = (color.toArgb().toLong() and 0xFFFFFFFFL)
                            if (target == SocialColorTarget.BACKGROUND) {
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

    private fun determineSlotCount(className: String): Int {
        return when {
            className.contains("Bar5") -> 5
            className.contains("Quad4") -> 4
            className.contains("Matrix9") -> 9
            className.contains("Deck10") -> 10
            className.contains("Bento10Top") -> 10
            className.contains("Bento10Left") -> 10
            className.contains("Orbit6") -> 6
            className.contains("Messaging4") -> 4
            className.contains("Stream3") -> 3
            className.contains("Octa8") -> 8
            className.contains("Twin2") -> 2
            className.contains("Micro1") -> 1
            else -> 4
        }
    }

    private fun determineLayoutTag(className: String): String {
        return when {
            className.contains("Bar5") -> "BAR_5"
            className.contains("Quad4") -> "QUAD_4"
            className.contains("Matrix9") -> "MATRIX_9"
            className.contains("Deck10") -> "DECK_10"
            className.contains("Bento10Top") -> "BENTO_10_TOP"
            className.contains("Bento10Left") -> "BENTO_10_LEFT"
            className.contains("Orbit6") -> "ORBIT_6"
            className.contains("Messaging4") -> "MESSAGING_4"
            className.contains("Stream3") -> "STREAM_3"
            className.contains("Octa8") -> "OCTA_8"
            className.contains("Twin2") -> "TWIN_2"
            className.contains("Micro1") -> "MICRO_1"
            else -> "QUAD_4"
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

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color(0xFF8E8E93),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun SelectableChip(
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
                .size(10.dp)
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
private fun RainbowPickerChip(
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
private fun ProfessionalSwatchCircle(
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
                val hex = (color.toArgb().toLong() and 0xFFFFFFFFL)
                val r = ((hex shr 16) and 0xFFL) / 255f
                val g = ((hex shr 8) and 0xFFL) / 255f
                val b = (hex and 0xFFL) / 255f
                val isLightColor = (0.2126f * r + 0.7152f * g + 0.0722f * b) > 0.6f
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
private fun ModernOpacitySlider(
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
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
    )
}
