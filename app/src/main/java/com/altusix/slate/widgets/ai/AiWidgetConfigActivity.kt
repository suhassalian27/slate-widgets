package com.altusix.slate.widgets.ai

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
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
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.altusix.slate.core.theme.ThemePreferences
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.ui.components.*

// =========================================================================
// AI WIDGET PERSISTENCE MODEL
// =========================================================================

data class AiWidgetConfig(
    val slots: List<AiTarget?> = emptyList()
) {
    companion object {
        fun load(context: Context, widgetId: Int, defaultCount: Int, defaultTargets: List<AiTarget>): AiWidgetConfig {
            val prefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
            val raw = prefs.getString("widget_${widgetId}_ai_slots", null)
            if (raw.isNullOrBlank()) {
                val initial = List(defaultCount) { i -> defaultTargets.getOrNull(i) }
                return AiWidgetConfig(initial)
            }
            val loaded = raw.split(",").map { name ->
                if (name.isBlank() || name == "EMPTY") null
                else try { AiTarget.valueOf(name.trim()) } catch (_: Exception) { null }
            }
            val padded = List(defaultCount) { i -> loaded.getOrNull(i) }
            return AiWidgetConfig(padded)
        }

        fun save(context: Context, widgetId: Int, config: AiWidgetConfig) {
            val prefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
            val serialized = config.slots.joinToString(",") { it?.name ?: "EMPTY" }
            prefs.edit().putString("widget_${widgetId}_ai_slots", serialized).apply()
        }
    }
}

// =========================================================================
// CONFIGURATION ACTIVITY
// =========================================================================

class AiWidgetConfigActivity : ComponentActivity() {

    private enum class AiColorTarget { BACKGROUND, ACCENT }

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

        val catalogItem = getAiWidgetsCatalog().find { it.receiverClass.name == widgetClassName }
        val widgetName = catalogItem?.name ?: "AI Widget"
        val hasModeOption = catalogItem?.hasModeOption ?: true

        // Check if widget is a single fixed AI icon (e.g. ChatGPT, Gemini, Claude)
        val singleTarget = getSingleAiTarget(widgetClassName)
        val isSingleAi = singleTarget != null

        slotCount = if (isSingleAi) 1 else determineSlotCount(widgetClassName)
        val defaultTargets = if (isSingleAi) listOf(singleTarget!!) else determineDefaultTargets(widgetClassName, slotCount)
        val defaultTheme = ThemePreferences(this).getThemeSettings()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF0A0A0C), surface = Color(0xFF16161B))) {
                var aiConfig by remember {
                    mutableStateOf(AiWidgetConfig.load(this@AiWidgetConfigActivity, widgetId, slotCount, defaultTargets))
                }
                var selectedBgHex by remember { mutableLongStateOf(0xFF161618L) }
                var selectedAccentHex by remember { mutableLongStateOf(defaultTheme.accentHex) }
                var opacity by remember { mutableFloatStateOf(1.0f) }
                var isResponsive by remember { mutableStateOf(true) }
                var activePickerTarget by remember { mutableStateOf<AiColorTarget?>(null) }
                var selectedTabKey by remember { mutableStateOf(if (isSingleAi) "STYLE" else "MODELS") }

                var showAiPickerSheet by remember { mutableStateOf(false) }
                var targetSlotIndex by remember { mutableIntStateOf(0) }
                var sheetSearchQuery by remember { mutableStateOf("") }

                // Single-icon widgets hide the AI Models tab and only show Widget Theme
                val tabs = remember(isSingleAi) {
                    if (isSingleAi) {
                        emptyList()
                    } else {
                        listOf(
                            ConfigTabItem("MODELS", "AI Models"),
                            ConfigTabItem("STYLE", "Widget Theme")
                        )
                    }
                }

                val availableTargets = remember { AiTarget.entries.toList() }

                LaunchedEffect(widgetId) {
                    val prefs = getSharedPreferences("slate_widget_prefs", MODE_PRIVATE)
                    opacity = prefs.getFloat("widget_${widgetId}_opacity", 1.0f)
                    isResponsive = prefs.getBoolean("widget_${widgetId}_is_responsive", true)
                    selectedBgHex = prefs.getLong("widget_${widgetId}_bg_color", 0xFF161618L)
                    selectedAccentHex = prefs.getLong("widget_${widgetId}_accent_color", defaultTheme.accentHex)
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
                    if (!isSingleAi) {
                        AiWidgetConfig.save(this@AiWidgetConfigActivity, widgetId, aiConfig)
                    }
                    saveSlateWidgetConfig(this@AiWidgetConfigActivity, widgetId, currentSlateConfig, isResponsive)

                    updateAllAiWidgets(this@AiWidgetConfigActivity)
                    val manager = AppWidgetManager.getInstance(this@AiWidgetConfigActivity)
                    val info = manager.getAppWidgetInfo(widgetId)
                    if (info?.provider != null) {
                        val updateIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                            component = info.provider
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))
                        }
                        sendBroadcast(updateIntent)
                    }

                    val resultIntent = Intent().apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }

                SlateConfigScaffold(
                    title = if (isSingleAi) widgetName else "AI Studio",
                    subtitle = if (isSingleAi) null else widgetName,
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

                        val (screenWDp, screenHDp) = remember(widgetId, isResponsive) {
                            val manager = AppWidgetManager.getInstance(context)
                            val options = manager?.getAppWidgetOptions(widgetId)
                            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

                            val fallbackW = when {
                                isSingleAi -> 140
                                widgetClassName.contains("DualFlagship") || widgetClassName.contains("Primary") || widgetClassName.contains("Capsule") || widgetClassName.contains("Dock") -> 240
                                widgetClassName.contains("Mega") || widgetClassName.contains("Side") || widgetClassName.contains("BentoHero") -> 220
                                else -> 150
                            }
                            val fallbackH = when {
                                isSingleAi -> 140
                                widgetClassName.contains("DualFlagship") || widgetClassName.contains("Primary") || widgetClassName.contains("Capsule") || widgetClassName.contains("Dock") -> 75
                                widgetClassName.contains("Mega") || widgetClassName.contains("Side") || widgetClassName.contains("BentoHero") -> 120
                                else -> 150
                            }

                            val rawW = if (isLandscape) {
                                options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, fallbackW) ?: fallbackW
                            } else {
                                options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, fallbackW) ?: fallbackW
                            }

                            val rawH = if (isLandscape) {
                                options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, fallbackH) ?: fallbackH
                            } else {
                                options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, fallbackH) ?: fallbackH
                            }

                            val finalW = if (rawW <= 0) fallbackW else rawW
                            val finalH = if (rawH <= 0) fallbackH else rawH
                            finalW to finalH
                        }

                        val previewBitmap = remember(aiConfig, currentSlateConfig, isResponsive, widgetClassName, screenWDp, screenHDp) {
                            when {
                                singleTarget != null -> generateSingleAiIconBitmap(context, singleTarget, currentSlateConfig, isResponsive, screenWDp, screenHDp, widgetId)
                                widgetClassName.contains("DualFlagship") -> generateAiBarDualFlagshipBitmap(context, currentSlateConfig, aiConfig, isResponsive, screenWDp, screenHDp, widgetId)
                                widgetClassName.contains("Primary") -> generateAiBarHeroPrimaryBitmap(context, currentSlateConfig, aiConfig, isResponsive, screenWDp, screenHDp, widgetId)
                                widgetClassName.contains("Dock5") -> generateAiBarDock5Bitmap(context, currentSlateConfig, aiConfig, isResponsive, screenWDp, screenHDp, widgetId)
                                widgetClassName.contains("Capsule") -> generateAiBarCapsuleBitmap(context, currentSlateConfig, aiConfig, isResponsive, screenWDp, screenHDp, widgetId)
                                widgetClassName.contains("BentoHero") || widgetClassName.contains("Folder6") -> generateAiFolder6BentoHeroBitmap(context, currentSlateConfig, aiConfig, isResponsive, screenWDp, screenHDp, widgetId)
                                widgetClassName.contains("Side") || widgetClassName.contains("Folder8") -> generateAiFolder8BentoSideBitmap(context, currentSlateConfig, aiConfig, isResponsive, screenWDp, screenHDp, widgetId)
                                widgetClassName.contains("Grid") || widgetClassName.contains("Folder9") -> generateAiFolder9GridBitmap(context, currentSlateConfig, aiConfig, isResponsive, screenWDp, screenHDp, widgetId)
                                widgetClassName.contains("Mega") || widgetClassName.contains("Folder10") -> generateAiFolder10MegaBitmap(context, currentSlateConfig, aiConfig, isResponsive, screenWDp, screenHDp, widgetId)
                                widgetClassName.contains("Asymmetric") || widgetClassName.contains("Folder7") -> generateAiFolder7AsymmetricBitmap(context, currentSlateConfig, aiConfig, isResponsive, screenWDp, screenHDp, widgetId)
                                else -> generateAiFolder4ClassicBitmap(context, currentSlateConfig, aiConfig, isResponsive, screenWDp, screenHDp, widgetId)
                            }
                        }

                        val previewModifier = remember(widgetClassName, isSingleAi) {
                            when {
                                isSingleAi -> Modifier.size(130.dp)
                                widgetClassName.contains("DualFlagship") || widgetClassName.contains("Primary") || widgetClassName.contains("Capsule") || widgetClassName.contains("Dock") ->
                                    Modifier.size(width = 240.dp, height = 75.dp)
                                widgetClassName.contains("Mega") || widgetClassName.contains("Side") || widgetClassName.contains("BentoHero") ->
                                    Modifier.size(width = 220.dp, height = 120.dp)
                                widgetClassName.contains("Asymmetric") ->
                                    Modifier.size(width = 180.dp, height = 120.dp)
                                else ->
                                    Modifier.size(130.dp)
                            }
                        }

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "AI Widget Preview",
                                modifier = previewModifier
                            )
                        }
                    }
                ) {
                    if (!isSingleAi && selectedTabKey == "MODELS") {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ASSIGNED MODELS (${aiConfig.slots.count { it != null }}/$slotCount)",
                                color = Color(0xFF8E8E93),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )

                            Text(
                                text = "Reset",
                                color = Color(0xFFFF453A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    aiConfig = AiWidgetConfig(List(slotCount) { defaultTargets.getOrNull(it) })
                                }
                            )
                        }

                        // Main Screen Assigned AI Dock
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF141418))
                                .border(1.dp, Color(0xFF24242C), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AssignedAiSlotsRow(
                                slotCount = slotCount,
                                aiConfig = aiConfig,
                                activeTargetIndex = targetSlotIndex,
                                accentColor = Color(selectedAccentHex),
                                onSlotSelected = { i ->
                                    targetSlotIndex = i
                                    showAiPickerSheet = true
                                },
                                onSlotRemove = { i ->
                                    val updated = aiConfig.slots.toMutableList()
                                    updated[i] = null
                                    aiConfig = aiConfig.copy(slots = updated)
                                }
                            )
                        }

                        // Select AI Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF141418))
                                .border(1.dp, Color(0xFF24242C), RoundedCornerShape(16.dp))
                                .clickable {
                                    val firstEmpty = aiConfig.slots.indexOfFirst { it == null }
                                    targetSlotIndex = if (firstEmpty != -1) firstEmpty else 0
                                    showAiPickerSheet = true
                                }
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
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(selectedAccentHex),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Select AI Models",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${availableTargets.size} models supported",
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
                    } else {
                        // WIDGET THEME & STYLE TAB
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
                                    activePickerTarget = AiColorTarget.BACKGROUND
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
                                    onClick = { activePickerTarget = AiColorTarget.ACCENT }
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

                // Modal Picker Sheet (Only available for Multi-AI widgets)
                if (!isSingleAi && showAiPickerSheet) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                    ModalBottomSheet(
                        onDismissRequest = {
                            showAiPickerSheet = false
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
                                .fillMaxHeight(0.85f)
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
                                        text = "Assign AI Models",
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
                                        showAiPickerSheet = false
                                        sheetSearchQuery = ""
                                    }
                                ) {
                                    Text("Done", color = Color(selectedAccentHex), fontWeight = FontWeight.Bold)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF181820))
                                    .border(1.dp, Color(0xFF262630), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AssignedAiSlotsRow(
                                    slotCount = slotCount,
                                    aiConfig = aiConfig,
                                    activeTargetIndex = targetSlotIndex,
                                    accentColor = Color(selectedAccentHex),
                                    onSlotSelected = { i -> targetSlotIndex = i },
                                    onSlotRemove = { i ->
                                        val updated = aiConfig.slots.toMutableList()
                                        updated[i] = null
                                        aiConfig = aiConfig.copy(slots = updated)
                                    }
                                )
                            }

                            OutlinedTextField(
                                value = sheetSearchQuery,
                                onValueChange = { sheetSearchQuery = it },
                                placeholder = { Text("Search AI assistants...", color = Color(0xFF8E8E93), fontSize = 13.sp) },
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

                            val filteredTargets = remember(sheetSearchQuery, availableTargets) {
                                if (sheetSearchQuery.isBlank()) availableTargets
                                else availableTargets.filter { it.title.contains(sheetSearchQuery, ignoreCase = true) }
                            }

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(bottom = 16.dp)
                            ) {
                                items(filteredTargets) { target ->
                                    val assignedIdx = aiConfig.slots.indexOfFirst { it == target }
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
                                                val updated = aiConfig.slots.toMutableList()
                                                if (isAssigned) {
                                                    updated[assignedIdx] = null
                                                    targetSlotIndex = assignedIdx
                                                } else {
                                                    val slotToFill = targetSlotIndex.coerceIn(0, slotCount - 1)
                                                    updated[slotToFill] = target
                                                    val nextEmpty = updated.indexOfFirst { it == null }
                                                    if (nextEmpty != -1) targetSlotIndex = nextEmpty
                                                }
                                                aiConfig = aiConfig.copy(slots = updated)
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 10.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            AiTargetVectorImage(
                                                target = target,
                                                tint = if (isAssigned) Color.White else Color(0xFFD0D0D8),
                                                modifier = Modifier.size(34.dp)
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = target.title,
                                                color = if (isAssigned) Color.White else Color(0xFF8E8E93),
                                                fontSize = 11.sp,
                                                fontWeight = if (isAssigned) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

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
                    val initialColor = if (target == AiColorTarget.BACKGROUND) Color(selectedBgHex) else Color(selectedAccentHex)
                    CustomColorPickerDialog(
                        initialColor = initialColor,
                        title = if (target == AiColorTarget.BACKGROUND) "Custom Background" else "Custom Accent",
                        onDismiss = { activePickerTarget = null },
                        onColorSelected = { color ->
                            val hex = (color.toArgb().toLong() and 0xFFFFFFFFL)
                            if (target == AiColorTarget.BACKGROUND) selectedBgHex = hex else selectedAccentHex = hex
                            activePickerTarget = null
                        }
                    )
                }
            }
        }
    }

    private fun getSingleAiTarget(className: String): AiTarget? {
        return when {
            className.contains("GeminiText") -> AiTarget.GEMINI_TEXT
            className.contains("ChatGptText") -> AiTarget.CHATGPT_TEXT
            className.contains("ChatGptVoice") -> AiTarget.CHATGPT_VOICE
            className.contains("Claude") && !className.contains("Folder") && !className.contains("Bar") -> AiTarget.CLAUDE
            className.contains("Grok") && !className.contains("Folder") && !className.contains("Bar") -> AiTarget.GROK
            className.contains("Perplexity") && !className.contains("Folder") && !className.contains("Bar") -> AiTarget.PERPLEXITY
            className.contains("DeepSeek") && !className.contains("Folder") && !className.contains("Bar") -> AiTarget.DEEPSEEK
            className.contains("Copilot") && !className.contains("Folder") && !className.contains("Bar") -> AiTarget.COPILOT
            className.contains("MetaAi") && !className.contains("Folder") && !className.contains("Bar") -> AiTarget.META_AI
            else -> null
        }
    }

    private fun determineSlotCount(className: String): Int {
        return when {
            className.contains("10") || className.contains("Mega") -> 10
            className.contains("9") || className.contains("Grid") -> 9
            className.contains("8") || className.contains("Side") -> 8
            className.contains("7") || className.contains("Asymmetric") -> 7
            className.contains("6") || className.contains("BentoHero") -> 6
            className.contains("5") || className.contains("Dock5") -> 5
            className.contains("DualFlagship") -> 2
            else -> 4
        }
    }

    private fun determineDefaultTargets(className: String, count: Int): List<AiTarget> {
        return when {
            className.contains("DualFlagship") -> listOf(AiTarget.CHATGPT_TEXT, AiTarget.GEMINI_TEXT)
            className.contains("Primary") -> listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.CLAUDE, AiTarget.GROK)
            className.contains("Dock5") -> listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.CLAUDE, AiTarget.GROK, AiTarget.PERPLEXITY)
            className.contains("Capsule") -> listOf(AiTarget.CHATGPT_TEXT, AiTarget.PERPLEXITY, AiTarget.CLAUDE, AiTarget.GEMINI_TEXT)
            className.contains("6") || className.contains("BentoHero") -> listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.CLAUDE, AiTarget.GROK, AiTarget.DEEPSEEK, AiTarget.META_AI)
            className.contains("7") || className.contains("Asymmetric") -> listOf(AiTarget.CHATGPT_TEXT, AiTarget.GROK, AiTarget.COPILOT, AiTarget.GEMINI_TEXT, AiTarget.CLAUDE, AiTarget.PERPLEXITY, AiTarget.META_AI)
            className.contains("8") || className.contains("Side") -> listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.CLAUDE, AiTarget.GROK, AiTarget.PERPLEXITY, AiTarget.COPILOT, AiTarget.DEEPSEEK, AiTarget.META_AI)
            className.contains("9") || className.contains("Grid") -> listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.COPILOT, AiTarget.GROK, AiTarget.CLAUDE, AiTarget.DEEPSEEK, AiTarget.PERPLEXITY, AiTarget.META_AI, AiTarget.POE)
            className.contains("10") || className.contains("Mega") -> listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.COPILOT, AiTarget.CLAUDE, AiTarget.GROK, AiTarget.PERPLEXITY, AiTarget.DEEPSEEK, AiTarget.META_AI, AiTarget.POE, AiTarget.MISTRAL)
            else -> listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.PERPLEXITY, AiTarget.CLAUDE)
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
// ASSIGNED AI DOCK COMPONENTS
// =========================================================================

@Composable
private fun AssignedAiSlotsRow(
    slotCount: Int,
    aiConfig: AiWidgetConfig,
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
                AssignedAiSlotItem(
                    index = i,
                    target = aiConfig.slots.getOrNull(i),
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
                AssignedAiSlotItem(
                    index = i,
                    target = aiConfig.slots.getOrNull(i),
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
private fun AssignedAiSlotItem(
    index: Int,
    target: AiTarget?,
    isActive: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(top = 5.dp, end = 5.dp)
            .size(54.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(if (target != null) Color(0xFF1E1E26) else Color(0xFF15151B))
                .border(
                    width = if (isActive) 2.dp else 1.dp,
                    color = if (isActive) accentColor else if (target != null) Color(0xFF32323E) else Color(0xFF24242C),
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (target != null) {
                AiTargetVectorImage(
                    target = target,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
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

        if (target != null) {
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

@Composable
fun AiTargetVectorImage(
    target: AiTarget,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resId = remember(target.drawableResName) {
        context.resources.getIdentifier(target.drawableResName, "drawable", context.packageName)
    }
    val imageBitmap = remember(resId, target.drawableResName) {
        if (resId != 0) {
            ContextCompat.getDrawable(context, resId)?.toImageBitmap()
        } else null
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = target.title,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(Color(0xFF2C2C30)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = target.title.take(1),
                color = tint,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
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

private fun Drawable.toImageBitmap(): ImageBitmap {
    val width = intrinsicWidth.coerceAtLeast(1)
    val height = intrinsicHeight.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap.asImageBitmap()
}