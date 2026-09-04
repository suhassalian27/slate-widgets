package com.altusix.slate.ui.config

import android.R
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
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.widgets.appfolder.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppFolderWidgetConfigActivity : ComponentActivity() {

    data class InstalledAppItem(val label: String, val packageName: String, val icon: Drawable? = null)

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetClassName: String = ""
    private var slotCount = 4

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        window.setBackgroundDrawableResource(R.color.transparent)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val appWidgetInfo = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)
        widgetClassName = appWidgetInfo?.provider?.className ?: ""
        slotCount = determineSlotCount(widgetClassName)

        setContent {
            SlateConfigTheme {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

                val catalogItem = remember(widgetClassName) {
                    getAppFolderWidgetsCatalog().find { it.receiverClass.name == widgetClassName }
                }
                val widgetName = catalogItem?.name ?: ""
                val hasModeOption = catalogItem?.hasModeOption ?: true

                var folderConfig by remember { mutableStateOf(AppFolderWidgetConfig.load(this, widgetId, slotCount)) }
                var selectedBgHex by remember { mutableLongStateOf(0xFF161618L) }
                var selectedAccentHex by remember { mutableLongStateOf(0xFFFFFFFFL) }
                var opacity by remember { mutableFloatStateOf(1.0f) }
                var isResponsive by remember { mutableStateOf(true) }
                var activePickerTarget by remember { mutableStateOf<ColorPickerTarget?>(null) }

                var installedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
                var searchQuery by remember { mutableStateOf("") }
                var selectedTab by remember { mutableIntStateOf(0) }

                LaunchedEffect(widgetId) {
                    val prefs = getSharedPreferences("slate_widget_prefs", MODE_PRIVATE)
                    opacity = prefs.getFloat("widget_${widgetId}_opacity", 1.0f)
                    isResponsive = prefs.getBoolean("widget_${widgetId}_is_responsive", true)
                    selectedBgHex = prefs.getLong("widget_${widgetId}_bg_color", 0xFF161618L)
                    selectedAccentHex = prefs.getLong("widget_${widgetId}_accent_color", 0xFFFFFFFFL)

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
                            InstalledAppItem(label = it.loadLabel(pm).toString(), packageName = it.activityInfo.packageName, icon = try { it.loadIcon(pm) } catch (_: Exception) { null })
                        }.sortedBy { it.label }

                        withContext(Dispatchers.Main) { installedApps = apps }
                    }
                }

                val currentSlateConfig = remember(selectedBgHex, selectedAccentHex, opacity) {
                    val themeMode = if (calculateLuminance(selectedBgHex) > 0.5f) "LIGHT" else "DARK"
                    SlateWidgetConfig(themeMode = themeMode, backgroundColorHex = selectedBgHex, opacity = opacity, accentColorHex = selectedAccentHex)
                }

                ModalBottomSheet(
                    onDismissRequest = { finish() },
                    sheetState = sheetState,
                    containerColor = Color(0xFF0A0A0C),
                    contentColor = Color.White,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF2C2C30)) },
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                ) {
                    AppFolderConfigSheetContent(
                        folderConfig = folderConfig,
                        slateConfig = currentSlateConfig,
                        widgetName = widgetName,
                        widgetClassName = widgetClassName,
                        hasModeOption = hasModeOption,
                        selectedBgHex = selectedBgHex,
                        selectedAccentHex = selectedAccentHex,
                        opacity = opacity,
                        isResponsive = isResponsive,
                        installedApps = installedApps,
                        searchQuery = searchQuery,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        onSearchQueryChanged = { searchQuery = it },
                        onFolderConfigChanged = { folderConfig = it },
                        onBgHexChanged = { selectedBgHex = it },
                        onAccentHexChanged = { selectedAccentHex = it },
                        onOpacityChanged = { opacity = it },
                        onResponsiveChanged = { isResponsive = it },
                        onPickerTargetRequested = { activePickerTarget = it },
                        onDismiss = { finish() },
                        onSaveClicked = {
                            AppFolderWidgetConfig.save(this@AppFolderWidgetConfigActivity, widgetId, folderConfig)
                            saveSlateWidgetConfig(this@AppFolderWidgetConfigActivity, widgetId, currentSlateConfig, isResponsive)
                            updateAllAppFolderWidgets(this@AppFolderWidgetConfigActivity)
                            setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                            finish()
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
                            if (target == ColorPickerTarget.BACKGROUND) selectedBgHex = hex else selectedAccentHex = hex
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

@Composable
private fun AppFolderConfigSheetContent(
    folderConfig: AppFolderWidgetConfig,
    slateConfig: SlateWidgetConfig,
    widgetName: String,
    widgetClassName: String,
    hasModeOption: Boolean,
    selectedBgHex: Long,
    selectedAccentHex: Long,
    opacity: Float,
    isResponsive: Boolean,
    installedApps: List<AppFolderWidgetConfigActivity.InstalledAppItem>,
    searchQuery: String,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onFolderConfigChanged: (AppFolderWidgetConfig) -> Unit,
    onBgHexChanged: (Long) -> Unit,
    onAccentHexChanged: (Long) -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onResponsiveChanged: (Boolean) -> Unit,
    onPickerTargetRequested: (ColorPickerTarget) -> Unit,
    onDismiss: () -> Unit,
    onSaveClicked: () -> Unit
) {
    val context = LocalContext.current
    val slotCount = folderConfig.slotCount
    val isLightBg = slateConfig.themeMode == "LIGHT"

    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    val bgPresets = listOf(0xFF161618L to "Dark", 0xFF000000L to "AMOLED", 0xFFFFFFFFL to "Light")
    val accentPresets = if (isLightBg) {
        listOf(0xFF000000L, 0xFF00D166L, 0xFF2B80FFL, 0xFFFF3B30L, 0xFFFF9500L, 0xFFAF52DEL)
    } else {
        listOf(0xFFFFFFFFL, 0xFF00D166L, 0xFF2B80FFL, 0xFFFF3B30L, 0xFFFF9500L, 0xFFAF52DEL)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .imePadding()
            .padding(horizontal = 22.dp)
            .padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Cancel", color = Color.Gray, fontSize = 15.sp, modifier = Modifier.clickable { onDismiss() }.padding(vertical = 8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Customize App Folder", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (widgetName.isNotEmpty()) {
                    Text(text = widgetName, fontSize = 11.sp, color = Color(0xFF8E8E93), fontWeight = FontWeight.Normal)
                }
            }
            Text(text = "Apply", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.clickable { onSaveClicked() }.padding(vertical = 8.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF141416))
                .border(1.dp, Color(0xFF242428), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            val previewBitmap = remember(folderConfig, slateConfig, isResponsive, widgetClassName) {
                when {
                    widgetClassName.contains("Triangle4") -> generateAppFolderTriangle4Bitmap(context, slateConfig, folderConfig, false, 140, 140, 0)
                    widgetClassName.contains("Horizontal3") -> generateAppFolderHorizontal3Bitmap(context, slateConfig, folderConfig, isResponsive, 180, 80, 0)
                    widgetClassName.contains("Vertical3") -> generateAppFolderVertical3Bitmap(context, slateConfig, folderConfig, isResponsive, 80, 180, 0)
                    widgetClassName.contains("Row4") -> generateAppFolderRow4Bitmap(context, slateConfig, folderConfig, isResponsive, 220, 80, 0)
                    widgetClassName.contains("Row5") -> generateAppFolderRow5Bitmap(context, slateConfig, folderConfig, isResponsive, 240, 80, 0)
                    widgetClassName.contains("Circle6") -> generateAppFolderCircle6Bitmap(context, slateConfig, folderConfig, isResponsive, 140, 140, 0)
                    widgetClassName.contains("Bento7") -> generateAppFolderBento7Bitmap(context, slateConfig, folderConfig, isResponsive, 140, 140, 0)
                    widgetClassName.contains("Grid9") -> generateAppFolderGrid9Bitmap(context, slateConfig, folderConfig, isResponsive, 140, 140, 0)
                    widgetClassName.contains("Bento10Left") -> generateAppFolderBento10LeftBitmap(context, slateConfig, folderConfig, isResponsive, 220, 120, 0)
                    widgetClassName.contains("Bento10Top") -> generateAppFolderBento10TopBitmap(context, slateConfig, folderConfig, isResponsive, 220, 120, 0)
                    widgetClassName.contains("Folder8") -> generateAppFolder8Bitmap(context, slateConfig, folderConfig, isResponsive, 220, 120, 0)
                    else -> generateAppFolder4Bitmap(context, slateConfig, folderConfig, isResponsive, 130, 130, 0)
                }
            }
            Image(bitmap = previewBitmap.asImageBitmap(), contentDescription = "Folder Preview", modifier = Modifier.size(if (slotCount >= 8) 190.dp else 130.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF141416)).padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Apps", "Style").forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(10.dp)).background(if (isSelected) Color(0xFF2C2C30) else Color.Transparent).clickable { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = label, color = if (isSelected) Color.White else Color(0xFF8E8E93), fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == 0) {
            Text(text = "Selected Apps (${folderConfig.slots.count { it.isConfigured }}/$slotCount)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF141416)).border(1.dp, Color(0xFF242428), RoundedCornerShape(16.dp)).padding(10.dp),
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
                            .background(if (slot.isConfigured) Color(0xFF2C2C30) else Color(0xFF1C1C1E))
                            .border(1.dp, if (slot.isConfigured) Color(0xFF3A3A3C) else Color(0xFF242428), RoundedCornerShape(12.dp))
                            .clickable {
                                if (slot.isConfigured) {
                                    val updatedSlots = folderConfig.slots.toMutableList()
                                    updatedSlots[i] = AppSlotConfig()
                                    onFolderConfigChanged(folderConfig.copy(slots = updatedSlots))
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (slot.isConfigured && appItem?.icon != null) {
                            Image(bitmap = appItem.icon.toImageBitmap(), contentDescription = null, modifier = Modifier.size(28.dp))
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(14.dp).clip(CircleShape).background(Color(0xFFFF3B30)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(8.dp))
                            }
                        } else {
                            Text(text = "${i + 1}", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF141416)).border(1.dp, Color(0xFF242428), RoundedCornerShape(14.dp)).clickable { onFolderConfigChanged(folderConfig.copy(showAppNames = !folderConfig.showAppNames)) }.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "App Labels", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Switch(checked = folderConfig.showAppNames, onCheckedChange = { onFolderConfigChanged(folderConfig.copy(showAppNames = it)) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF34C759)))
                }

                Row(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF141416)).border(1.dp, Color(0xFF242428), RoundedCornerShape(14.dp)).clickable { onFolderConfigChanged(folderConfig.copy(showTileBackground = !folderConfig.showTileBackground)) }.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Tile Cards", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Switch(checked = folderConfig.showTileBackground, onCheckedChange = { onFolderConfigChanged(folderConfig.copy(showTileBackground = it)) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF34C759)))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Search apps...", color = Color.Gray, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.White, unfocusedBorderColor = Color(0xFF242428), focusedContainerColor = Color(0xFF141416), unfocusedContainerColor = Color(0xFF141416), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredApps) { app ->
                    val isSelected = folderConfig.slots.any { it.packageName == app.packageName }
                    Column(
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (isSelected) Color(0xFF2C2C30) else Color(0xFF141416)).border(1.dp, if (isSelected) Color.White else Color(0xFF242428), RoundedCornerShape(12.dp)).clickable {
                            val updatedSlots = folderConfig.slots.toMutableList()
                            if (isSelected) {
                                val idx = updatedSlots.indexOfFirst { it.packageName == app.packageName }
                                if (idx != -1) updatedSlots[idx] = AppSlotConfig()
                            } else {
                                val emptyIdx = updatedSlots.indexOfFirst { !it.isConfigured }
                                if (emptyIdx != -1) updatedSlots[emptyIdx] = AppSlotConfig(
                                    packageName = app.packageName,
                                    appName = app.label,
                                    isConfigured = true
                                )
                            }
                            onFolderConfigChanged(folderConfig.copy(slots = updatedSlots))
                        }.padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (app.icon != null) Image(bitmap = app.icon.toImageBitmap(), contentDescription = null, modifier = Modifier.size(30.dp))
                        else Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(Color(0xFF2C2C30)))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = app.label, color = if (isSelected) Color.White else Color(0xFF8E8E93), fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

        } else {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column {
                    SectionTitle(title = "Background")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        bgPresets.forEach { (hex, label) ->
                            SelectableChip(label = label, isSelected = selectedBgHex == hex, colorPreview = Color(hex), modifier = Modifier.weight(1f)) {
                                onBgHexChanged(hex)
                                if (hex == 0xFFFFFFFFL && selectedAccentHex == 0xFFFFFFFFL) onAccentHexChanged(0xFF000000L)
                                else if (hex != 0xFFFFFFFFL && selectedAccentHex == 0xFF000000L) onAccentHexChanged(0xFFFFFFFFL)
                            }
                        }
                        val isCustomBg = bgPresets.none { it.first == selectedBgHex }
                        RainbowPickerChip(isSelected = isCustomBg, activeColor = if (isCustomBg) Color(selectedBgHex) else null, modifier = Modifier.weight(1f)) {
                            onPickerTargetRequested(ColorPickerTarget.BACKGROUND)
                        }
                    }
                }

                Column {
                    SectionTitle(title = "Accent Color")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        accentPresets.forEach { hex ->
                            ProfessionalSwatchCircle(color = Color(hex), isSelected = selectedAccentHex == hex, onClick = { onAccentHexChanged(hex) })
                        }
                        val isCustomAccent = accentPresets.none { it == selectedAccentHex }
                        RainbowCustomCircle(isSelected = isCustomAccent, activeColor = if (isCustomAccent) Color(selectedAccentHex) else null, onClick = { onPickerTargetRequested(ColorPickerTarget.ACCENT) })
                    }
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle(title = "Opacity")
                        Text(text = "${(opacity * 100).toInt()}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    ModernOpacitySlider(value = opacity, onValueChange = onOpacityChanged)
                }

                if (hasModeOption) {
                    Column {
                        SectionTitle(title = "Sizing Mode")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF141416)).padding(4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            listOf(true to "Responsive", false to "Fixed Aspect").forEach { (responsiveVal, label) ->
                                val isSelected = isResponsive == responsiveVal
                                Box(
                                    modifier = Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(10.dp)).background(if (isSelected) Color(0xFF2C2C30) else Color.Transparent).clickable { onResponsiveChanged(responsiveVal) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = label, color = if (isSelected) Color.White else Color(0xFF8E8E93), fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
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