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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
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
import com.altusix.slate.widgets.appfolder.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

class AppFolderWidgetConfigActivity : ComponentActivity() {

    data class InstalledAppItem(val label: String, val packageName: String, val icon: Drawable? = null)
    private enum class AppFolderColorTarget { BACKGROUND, ACCENT }

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetClassName: String = ""
    private var slotCount = 4

    @OptIn(ExperimentalMaterial3Api::class)
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
                var selectedTabKey by remember { mutableStateOf("APPS") }

                // Bottom Sheet State
                var showAppPickerSheet by remember { mutableStateOf(false) }
                var targetSlotIndex by remember { mutableIntStateOf(0) }
                var sheetSearchQuery by remember { mutableStateOf("") }

                val tabs = remember {
                    listOf(
                        ConfigTabItem("APPS", "Apps"),
                        ConfigTabItem("STYLE", "Widget Theme")
                    )
                }
                val widgetName = catalogItem?.name ?: ""

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

                        // Fix 1: Deduplicate by packageName so multi-activity apps (e.g. Amazon) don't duplicate
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
                        // Header
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

                        // Main Screen Assigned Slots Dock
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF141418))
                                .border(1.dp, Color(0xFF24242C), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AssignedSlotsRow(
                                slotCount = slotCount,
                                folderConfig = folderConfig,
                                installedApps = installedApps,
                                activeTargetIndex = targetSlotIndex,
                                accentColor = Color(selectedAccentHex),
                                onSlotSelected = { i ->
                                    targetSlotIndex = i
                                    showAppPickerSheet = true
                                },
                                onSlotRemove = { i ->
                                    val updatedSlots = folderConfig.slots.toMutableList()
                                    updatedSlots[i] = AppSlotConfig()
                                    folderConfig = folderConfig.copy(slots = updatedSlots)
                                }
                            )
                        }

                        // Toggle Options
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

                        // App Picker Trigger Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF141418))
                                .border(1.dp, Color(0xFF24242C), RoundedCornerShape(16.dp))
                                .clickable {
                                    val firstEmpty = folderConfig.slots.indexOfFirst { !it.isConfigured }
                                    targetSlotIndex = if (firstEmpty != -1) firstEmpty else 0
                                    showAppPickerSheet = true
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 1. Leading App Grid Icon in an Accent-Tinted Squircle
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

                            // 2. Clean, Standard Title & Subtitle
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Select Apps",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${installedApps.size} apps available",
                                    color = Color(0xFF8E8E93),
                                    fontSize = 12.sp
                                )
                            }

                            // 3. Universal Navigation Chevron
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF636366),
                                modifier = Modifier.size(20.dp)
                            )
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

                // =========================================================================
                // MODAL APP PICKER SHEET (WITH EMBEDDED ASSIGNED DOCK)
                // =========================================================================
                if (showAppPickerSheet) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                    ModalBottomSheet(
                        onDismissRequest = {
                            showAppPickerSheet = false
                            sheetSearchQuery = ""
                        },
                        sheetState = sheetState,
                        containerColor = Color(0xFF121216),
                        contentColor = Color.White,
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
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Assign Apps",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Editing Slot ${targetSlotIndex + 1} of $slotCount",
                                        color = Color(selectedAccentHex),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        showAppPickerSheet = false
                                        sheetSearchQuery = ""
                                    }
                                ) {
                                    Text("Done", color = Color(selectedAccentHex), fontWeight = FontWeight.Bold)
                                }
                            }

                            // Fix 2: Assigned Apps Dock Pinned Inside the Modal Popup
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF181820))
                                    .border(1.dp, Color(0xFF262630), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AssignedSlotsRow(
                                    slotCount = slotCount,
                                    folderConfig = folderConfig,
                                    installedApps = installedApps,
                                    activeTargetIndex = targetSlotIndex,
                                    accentColor = Color(selectedAccentHex),
                                    onSlotSelected = { i -> targetSlotIndex = i },
                                    onSlotRemove = { i ->
                                        val updatedSlots = folderConfig.slots.toMutableList()
                                        updatedSlots[i] = AppSlotConfig()
                                        folderConfig = folderConfig.copy(slots = updatedSlots)
                                    }
                                )
                            }

                            // Search Bar
                            OutlinedTextField(
                                value = sheetSearchQuery,
                                onValueChange = { sheetSearchQuery = it },
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
                                    if (sheetSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { sheetSearchQuery = "" }) {
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
                                    focusedBorderColor = Color(selectedAccentHex),
                                    unfocusedBorderColor = Color(0xFF282832),
                                    focusedContainerColor = Color(0xFF18181E),
                                    unfocusedContainerColor = Color(0xFF18181E),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            val filteredApps = remember(sheetSearchQuery, installedApps) {
                                if (sheetSearchQuery.isBlank()) installedApps
                                else installedApps.filter { it.label.contains(sheetSearchQuery, ignoreCase = true) }
                            }

                            // Scrollable Apps Grid
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(bottom = 16.dp)
                            ) {
                                items(filteredApps) { app ->
                                    val assignedIdx = folderConfig.slots.indexOfFirst { it.packageName == app.packageName }
                                    val isAssigned = assignedIdx != -1

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isAssigned) Color(0xFF22222E) else Color(0xFF16161C))
                                            .border(
                                                width = if (isAssigned) 1.5.dp else 1.dp,
                                                color = if (isAssigned) Color(selectedAccentHex) else Color(0xFF24242C),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                val updatedSlots = folderConfig.slots.toMutableList()
                                                if (isAssigned) {
                                                    // Toggle off if already assigned
                                                    updatedSlots[assignedIdx] = AppSlotConfig()
                                                    targetSlotIndex = assignedIdx
                                                } else {
                                                    // Assign directly to current active target slot
                                                    val slotToFill = targetSlotIndex.coerceIn(0, slotCount - 1)
                                                    updatedSlots[slotToFill] = AppSlotConfig(
                                                        packageName = app.packageName,
                                                        appName = app.label,
                                                        isConfigured = true
                                                    )
                                                    // Auto-advance target to next unconfigured slot
                                                    val nextEmpty = updatedSlots.indexOfFirst { !it.isConfigured }
                                                    if (nextEmpty != -1) {
                                                        targetSlotIndex = nextEmpty
                                                    }
                                                }
                                                folderConfig = folderConfig.copy(slots = updatedSlots)
                                            }
                                    ) {
                                        // 1. Center Content: App Icon + App Name
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 10.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
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

                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = app.label,
                                                color = if (isAssigned) Color.White else Color(0xFF8E8E93),
                                                fontSize = 11.sp,
                                                fontWeight = if (isAssigned) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // 2. Top-Right Corner Slot Badge with Centered Number
                                        if (isAssigned) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(5.dp)
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(selectedAccentHex)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${assignedIdx + 1}",
                                                    color = Color.Black,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    style = TextStyle(
                                                        platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false),
                                                        textAlign = TextAlign.Center
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Color Picker Dialog
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

// =========================================================================
// REUSABLE ASSIGNED SLOTS COMPONENTS (FIXES 2 & 3)
// =========================================================================

@Composable
private fun AssignedSlotsRow(
    slotCount: Int,
    folderConfig: AppFolderWidgetConfig,
    installedApps: List<AppFolderWidgetConfigActivity.InstalledAppItem>,
    activeTargetIndex: Int,
    accentColor: Color,
    onSlotSelected: (Int) -> Unit,
    onSlotRemove: (Int) -> Unit
) {
    if (slotCount <= 5) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until slotCount) {
                AssignedSlotItem(
                    index = i,
                    slot = folderConfig.slots.getOrElse(i) { AppSlotConfig() },
                    appItem = installedApps.find { it.packageName == folderConfig.slots.getOrNull(i)?.packageName },
                    isActive = activeTargetIndex == i,
                    accentColor = accentColor,
                    onClick = { onSlotSelected(i) },
                    onRemove = { onSlotRemove(i) }
                )
            }
        }
    } else {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(slotCount) { i ->
                AssignedSlotItem(
                    index = i,
                    slot = folderConfig.slots.getOrElse(i) { AppSlotConfig() },
                    appItem = installedApps.find { it.packageName == folderConfig.slots.getOrNull(i)?.packageName },
                    isActive = activeTargetIndex == i,
                    accentColor = accentColor,
                    onClick = { onSlotSelected(i) },
                    onRemove = { onSlotRemove(i) }
                )
            }
        }
    }
}

@Composable
private fun AssignedSlotItem(
    index: Int,
    slot: AppSlotConfig,
    appItem: AppFolderWidgetConfigActivity.InstalledAppItem?,
    isActive: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    // Unclipped parent box with 5dp outer room for the top-right badge
    Box(
        modifier = Modifier
            .padding(top = 5.dp, end = 5.dp)
            .size(54.dp)
    ) {
        // Main slot tile body
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(if (slot.isConfigured) Color(0xFF1E1E26) else Color(0xFF15151B))
                .border(
                    width = if (isActive) 2.dp else 1.dp,
                    color = if (isActive) accentColor else if (slot.isConfigured) Color(0xFF32323E) else Color(0xFF24242C),
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (slot.isConfigured && appItem?.icon != null) {
                Image(
                    bitmap = appItem.icon.toImageBitmap(),
                    contentDescription = slot.appName,
                    modifier = Modifier.size(30.dp)
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = if (isActive) accentColor else Color(0xFF555562),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${index + 1}",
                        color = if (isActive) accentColor else Color(0xFF63636E),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Fix 3: Standard Cutout Remove Badge (Completely Unclipped)
        if (slot.isConfigured) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 5.dp, y = (-5).dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF282834))
                    .border(1.5.dp, Color(0xFF121216), CircleShape)
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(11.dp)
                )
            }
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
