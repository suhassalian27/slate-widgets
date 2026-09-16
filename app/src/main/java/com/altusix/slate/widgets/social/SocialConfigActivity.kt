package com.altusix.slate.widgets.social

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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.altusix.slate.core.theme.ThemePreferences
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.ui.components.*
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

private enum class SocialColorTarget { BACKGROUND, ACCENT }

private fun calculateLuminance(hex: Long): Float {
    val r = ((hex shr 16) and 0xFFL) / 255f
    val g = ((hex shr 8) and 0xFFL) / 255f
    val b = (hex and 0xFFL) / 255f
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

private fun Drawable.toImageBitmap(tintColor: Int? = null): ImageBitmap {
    val width = intrinsicWidth.coerceAtLeast(1)
    val height = intrinsicHeight.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val mutated = mutate()
    if (tintColor != null) {
        mutated.setTint(tintColor)
    }
    mutated.setBounds(0, 0, canvas.width, canvas.height)
    mutated.draw(canvas)
    return bitmap.asImageBitmap()
}

class SocialConfigActivity : ComponentActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetClassName: String = ""
    private var slotCount = 4
    private var layoutTag = "QUAD_4"

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0A0C)
                ) {
                    var socialConfig by remember {
                        mutableStateOf(SocialStorageManager.load(this@SocialConfigActivity, widgetId, slotCount, layoutTag))
                    }
                    var selectedBgHex by remember { mutableLongStateOf(0xFF161618L) }
                    var selectedAccentHex by remember { mutableLongStateOf(defaultTheme.accentHex) }
                    var opacity by remember { mutableFloatStateOf(1.0f) }
                    var isResponsive by remember { mutableStateOf(true) }
                    var activePickerTarget by remember { mutableStateOf<SocialColorTarget?>(null) }
                    var activeSlotIndex by remember { mutableIntStateOf(initialSlotIndex) }
                    var selectedTabKey by remember { mutableStateOf("APPS") }

                    val tabs = remember {
                        listOf(
                            ConfigTabItem("APPS", "Platforms & Slots"),
                            ConfigTabItem("STYLE", "Widget Theme")
                        )
                    }
                    val widgetName = catalogItem?.name ?: "Social Studio"

                    LaunchedEffect(widgetId) {
                        val prefs = getSharedPreferences("slate_widget_prefs", MODE_PRIVATE)
                        opacity = prefs.getFloat("widget_${widgetId}_opacity", 1.0f)
                        selectedBgHex = prefs.getLong("widget_${widgetId}_bg_color", 0xFF161618L)
                        selectedAccentHex = prefs.getLong("widget_${widgetId}_accent_color", defaultTheme.accentHex)

                        val modeKey = "widget_${widgetId}_mode"
                        val isResponsiveKey = "widget_${widgetId}_is_responsive"
                        isResponsive = if (prefs.contains(modeKey)) {
                            prefs.getString(modeKey, "RESPONSIVE") == "RESPONSIVE"
                        } else {
                            prefs.getBoolean(isResponsiveKey, true)
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

                        getSharedPreferences("slate_widget_prefs", MODE_PRIVATE).edit()
                            .putString("widget_${widgetId}_theme_mode", currentSlateConfig.themeMode)
                            .putLong("widget_${widgetId}_bg_color", currentSlateConfig.backgroundColorHex)
                            .putFloat("widget_${widgetId}_opacity", currentSlateConfig.opacity)
                            .putLong("widget_${widgetId}_accent_color", currentSlateConfig.accentColorHex)
                            .putBoolean("widget_${widgetId}_is_responsive", isResponsive)
                            .putString("widget_${widgetId}_mode", if (isResponsive) "RESPONSIVE" else "FIXED")
                            .commit()

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

                        val existingIndex = updatedSlots.indexOfFirst { it.presetId.equals(preset.id, ignoreCase = true) }

                        // Tapping the assigned icon again unselects it
                        if (existingIndex == activeSlotIndex) {
                            updatedSlots[activeSlotIndex] = SocialSlotConfig()
                            socialConfig = socialConfig.copy(slots = updatedSlots)
                            return
                        }

                        // Remove from previous slot to enforce uniqueness
                        if (existingIndex != -1) {
                            updatedSlots[existingIndex] = SocialSlotConfig()
                        }

                        // Assign to active slot
                        updatedSlots[activeSlotIndex] = SocialSlotConfig(
                            packageName = preset.packageName,
                            appName = preset.name,
                            presetId = preset.id,
                            isConfigured = true
                        )
                        socialConfig = socialConfig.copy(slots = updatedSlots)

                        // Smart cursor: advance to the next unconfigured slot
                        val nextEmpty = (0 until slotCount).firstOrNull { idx -> !updatedSlots[idx].isConfigured }
                        activeSlotIndex = nextEmpty ?: ((activeSlotIndex + 1) % slotCount)
                    }

                    SlateConfigScaffold(
                        title = "Social Studio",
                        subtitle = widgetName.ifEmpty { null },
                        accentColor = Color(selectedAccentHex),
                        tabs = tabs,
                        selectedTabKey = selectedTabKey,
                        onTabSelected = { selectedTabKey = it },
                        onBackClick = { finish() },
                        onSaveClick = { saveAndFinish() },
                        scrollable = selectedTabKey == "STYLE",
                        previewHeight = 165.dp,
                        previewContent = {
                            val context = LocalContext.current
                            val previewBitmap = remember(socialConfig, currentSlateConfig, isResponsive, widgetClassName) {
                                when {
                                    widgetClassName.contains("Bar5") -> generateSocialBar5Bitmap(
                                        context = context,
                                        config = currentSlateConfig,
                                        isResponsive = isResponsive,
                                        wDp = 240,
                                        hDp = 70,
                                        widgetId = 0,
                                        socialConfig = socialConfig
                                    )
                                    widgetClassName.contains("Quad4") -> generateSocialGridBitmap(context, currentSlateConfig, socialConfig, isResponsive, 140, 140, 0, cols = 2, rows = 2)
                                    widgetClassName.contains("Matrix9") -> generateSocialGridBitmap(context, currentSlateConfig, socialConfig, isResponsive, 140, 140, 0, cols = 3, rows = 3)
                                    widgetClassName.contains("Deck10") -> generateSocialDeck10Bitmap(context, currentSlateConfig, isResponsive, 220, 110, 0, socialConfig)
                                    widgetClassName.contains("Bento10Top") -> generateSocialBento10TopBitmap(context, currentSlateConfig, isResponsive, 220, 110, 0)
                                    widgetClassName.contains("Bento10Left") -> generateSocialBento10LeftBitmap(context, currentSlateConfig, isResponsive, 220, 110, 0)
                                    widgetClassName.contains("Orbit6") -> generateSocialOrbit6Bitmap(
                                        context = context,
                                        config = currentSlateConfig,
                                        isResponsive = isResponsive,
                                        wDp = 140,
                                        hDp = 140,
                                        widgetId = 0,
                                        socialConfig = socialConfig
                                    )
                                    widgetClassName.contains("Messaging4") -> generateSocialMessaging4Bitmap(context, currentSlateConfig, isResponsive, 220, 70, 0, socialConfig)
                                    widgetClassName.contains("Stream3") -> generateSocialStream3Bitmap(context, currentSlateConfig, isResponsive, 180, 70, 0, socialConfig)
                                    widgetClassName.contains("Octa8") -> generateSocialOcta8Bitmap(
                                        context = context,
                                        config = currentSlateConfig,
                                        isResponsive = isResponsive,
                                        wDp = 220,
                                        hDp = 110,
                                        widgetId = 0,
                                        socialConfig = socialConfig
                                    )
                                    widgetClassName.contains("Twin2") -> generateSocialGridBitmap(context, currentSlateConfig, socialConfig, isResponsive, 90, 160, 0, cols = 1, rows = 2)
                                    widgetClassName.contains("Micro1") -> generateSocialGridBitmap(context, currentSlateConfig, socialConfig, isResponsive, 100, 100, 0, cols = 1, rows = 1)
                                    else -> generateSocialGridBitmap(context, currentSlateConfig, socialConfig, isResponsive, 140, 140, 0, cols = 2, rows = 2)
                                }
                            }

                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Social Widget Preview",
                                modifier = Modifier.size(if (slotCount >= 8 || widgetClassName.contains("Bar5") || widgetClassName.contains("Messaging4")) 175.dp else 135.dp)
                            )
                        }
                    ) {
                        when (selectedTabKey) {
                            "APPS" -> {
                                SocialPlatformsTab(
                                    slotCount = slotCount,
                                    activeSlotIndex = activeSlotIndex,
                                    socialConfig = socialConfig,
                                    accentColor = Color(selectedAccentHex),
                                    onSelectSlot = { activeSlotIndex = it },
                                    onClearSlot = { index ->
                                        val updated = socialConfig.slots.toMutableList()
                                        updated[index] = SocialSlotConfig()
                                        socialConfig = socialConfig.copy(slots = updated)
                                    },
                                    onResetDefaults = {
                                        socialConfig = socialConfig.copy(
                                            slots = SocialStorageManager.getDefaultSlotsForLayout(slotCount, layoutTag)
                                        )
                                    },
                                    onClearAll = {
                                        socialConfig = socialConfig.copy(slots = List(slotCount) { SocialSlotConfig() })
                                    },
                                    onAssignPreset = { assignPresetToSlot(it) }
                                )
                            }
                            "STYLE" -> {
                                SocialThemeTab(
                                    socialConfig = socialConfig,
                                    onSocialConfigChanged = { socialConfig = it },
                                    selectedBgHex = selectedBgHex,
                                    onBgHexSelected = { selectedBgHex = it },
                                    selectedAccentHex = selectedAccentHex,
                                    onAccentHexSelected = { selectedAccentHex = it },
                                    opacity = opacity,
                                    onOpacityChanged = { opacity = it },
                                    isResponsive = isResponsive,
                                    onResponsiveChanged = { isResponsive = it },
                                    hasModeOption = hasModeOption,
                                    onOpenColorPicker = { activePickerTarget = it }
                                )
                            }
                        }
                    }

                    if (activePickerTarget != null) {
                        val initialColor = if (activePickerTarget == SocialColorTarget.BACKGROUND) Color(selectedBgHex) else Color(selectedAccentHex)
                        CustomColorPickerDialog(
                            initialColor = initialColor,
                            title = if (activePickerTarget == SocialColorTarget.BACKGROUND) "Custom Background" else "Custom Accent",
                            onDismiss = { activePickerTarget = null },
                            onColorSelected = { color ->
                                val hex = (color.toArgb().toLong() and 0xFFFFFFFFL)
                                if (activePickerTarget == SocialColorTarget.BACKGROUND) {
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
}

// -----------------------------------------------------------------------------
// TAB 1: PLATFORMS & SLOTS (Centered, Tight Spacing, No Ghost Padding)
// -----------------------------------------------------------------------------

@Composable
private fun SocialPlatformsTab(
    slotCount: Int,
    activeSlotIndex: Int,
    socialConfig: SocialWidgetConfig,
    accentColor: Color,
    onSelectSlot: (Int) -> Unit,
    onClearSlot: (Int) -> Unit,
    onResetDefaults: () -> Unit,
    onClearAll: () -> Unit,
    onAssignPreset: (SocialAppPreset) -> Unit
) {
    val context = LocalContext.current
    val accentHex = accentColor.toArgb().toLong() and 0xFFFFFFFFL
    val onAccentColor = if (calculateLuminance(accentHex) > 0.5f) Color.Black else Color.White

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Slots Status Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SLOTS (${socialConfig.slots.count { it.isConfigured }}/$slotCount) — ACTIVE: #${activeSlotIndex + 1}",
                color = Color(0xFF8E8E93),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Reset",
                    color = accentColor,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onResetDefaults() }
                )
                Text(
                    text = "Clear",
                    color = Color(0xFFFF453A),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onClearAll() }
                )
            }
        }

        // 2. Horizontal Slot Carousel
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF141418))
                .border(1.dp, Color(0xFF222228), RoundedCornerShape(14.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(slotCount) { i ->
                val slot = socialConfig.slots.getOrElse(i) { SocialSlotConfig() }
                val isSelected = activeSlotIndex == i
                val presetRes = if (slot.presetId.isNotBlank()) getSocialPresetDrawableResId(context, slot.presetId) else 0

                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0xFF22222C) else if (slot.isConfigured) Color(0xFF18181E) else Color(0xFF101014))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) accentColor else if (slot.isConfigured) Color(0xFF2E2E38) else Color(0xFF1E1E24),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectSlot(i) },
                    contentAlignment = Alignment.Center
                ) {
                    if (slot.isConfigured) {
                        val iconDrawable = remember(slot.presetId) {
                            if (presetRes != 0) ContextCompat.getDrawable(context, presetRes) else null
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            if (iconDrawable != null) {
                                Image(
                                    bitmap = iconDrawable.toImageBitmap(android.graphics.Color.WHITE),
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

                        // Remove badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .size(13.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30))
                                .clickable { onClearSlot(i) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(7.5.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "${i + 1}",
                            color = if (isSelected) accentColor else Color(0xFF636366),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Compact 4x4 Grid Container
        SectionTitle(title = "Social Platforms")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF141418))
                .border(1.dp, Color(0xFF222228), RoundedCornerShape(14.dp))
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SocialStorageManager.SOCIAL_PRESETS.chunked(4).forEach { rowPresets ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowPresets.forEach { preset ->
                        val presetRes = getSocialPresetDrawableResId(context, preset.id)
                        val iconDrawable = remember(preset.id) {
                            if (presetRes != 0) ContextCompat.getDrawable(context, presetRes) else null
                        }
                        val assignedSlotIndex = socialConfig.slots.indexOfFirst { it.presetId.equals(preset.id, ignoreCase = true) }
                        val isAssigned = assignedSlotIndex != -1
                        val isCurrentSlot = assignedSlotIndex == activeSlotIndex

                        val cardBg = if (isAssigned) accentColor else Color(0xFF18181F)
                        val cardBorder = when {
                            isCurrentSlot -> Color.White
                            isAssigned -> Color.Transparent
                            else -> Color(0xFF262630)
                        }

                        val contentTint = if (isAssigned) onAccentColor else Color(0xFFD4D4D8)
                        val labelColor = if (isAssigned) onAccentColor else Color(0xFF8E8E93)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(cardBg)
                                .border(
                                    width = if (isCurrentSlot) 1.8.dp else 1.dp,
                                    color = cardBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onAssignPreset(preset) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                if (iconDrawable != null) {
                                    Image(
                                        bitmap = iconDrawable.toImageBitmap(android.graphics.Color.WHITE),
                                        contentDescription = preset.name,
                                        colorFilter = ColorFilter.tint(contentTint),
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(contentTint.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = preset.name.take(1),
                                            color = contentTint,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = preset.name,
                                    color = labelColor,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 2.dp),
                                    style = TextStyle(
                                        fontSize = 9.sp,
                                        fontWeight = if (isAssigned) FontWeight.Bold else FontWeight.Medium,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    )
                                )
                            }

                            // Minimalist circular pip for assigned slots
                            if (isAssigned) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(3.dp)
                                        .size(13.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrentSlot) onAccentColor.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.35f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${assignedSlotIndex + 1}",
                                        color = onAccentColor,
                                        textAlign = TextAlign.Center,
                                        style = TextStyle(
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            platformStyle = PlatformTextStyle(includeFontPadding = false)
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
}

// -----------------------------------------------------------------------------
// TAB 2: WIDGET THEME & STYLE
// -----------------------------------------------------------------------------

@Composable
private fun SocialThemeTab(
    socialConfig: SocialWidgetConfig,
    onSocialConfigChanged: (SocialWidgetConfig) -> Unit,
    selectedBgHex: Long,
    onBgHexSelected: (Long) -> Unit,
    selectedAccentHex: Long,
    onAccentHexSelected: (Long) -> Unit,
    opacity: Float,
    onOpacityChanged: (Float) -> Unit,
    isResponsive: Boolean,
    onResponsiveChanged: (Boolean) -> Unit,
    hasModeOption: Boolean,
    onOpenColorPicker: (SocialColorTarget) -> Unit
) {
    val isLightBg = ((selectedBgHex shr 16 and 0xFFL) * 0.2126f + (selectedBgHex shr 8 and 0xFFL) * 0.7152f + (selectedBgHex and 0xFFL) * 0.0722f) / 255f > 0.5f

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 1. Icon Styling
        SectionTitle(title = "Icon Style")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF141416))
                .padding(4.dp),
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
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFF2C2C30) else Color.Transparent)
                        .clickable { onSocialConfigChanged(socialConfig.copy(iconStyle = styleKey)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color(0xFF8E8E93),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }

        // 2. Display Toggles
        SectionTitle(title = "Display Options")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF141418))
                    .border(1.dp, Color(0xFF24242C), RoundedCornerShape(12.dp))
                    .clickable { onSocialConfigChanged(socialConfig.copy(showAppNames = !socialConfig.showAppNames)) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("App Labels", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Switch(
                    checked = socialConfig.showAppNames,
                    onCheckedChange = { onSocialConfigChanged(socialConfig.copy(showAppNames = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color(selectedAccentHex),
                        uncheckedThumbColor = Color(0xFF8E8E93),
                        uncheckedTrackColor = Color(0xFF1C1C22)
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF141418))
                    .border(1.dp, Color(0xFF24242C), RoundedCornerShape(12.dp))
                    .clickable { onSocialConfigChanged(socialConfig.copy(showTileBackground = !socialConfig.showTileBackground)) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tile Cards", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Switch(
                    checked = socialConfig.showTileBackground,
                    onCheckedChange = { onSocialConfigChanged(socialConfig.copy(showTileBackground = it)) },
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

        // 3. Background Presets
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
                    onBgHexSelected(hex)
                    if (hex == 0xFFFFFFFFL && selectedAccentHex == 0xFFFFFFFFL) onAccentHexSelected(0xFF000000L)
                    else if (hex != 0xFFFFFFFFL && selectedAccentHex == 0xFF000000L) onAccentHexSelected(0xFFFFFFFFL)
                }
            }

            val isCustomBg = bgPresets.none { it.first == selectedBgHex }
            RainbowPickerChip(
                isSelected = isCustomBg,
                activeColor = if (isCustomBg) Color(selectedBgHex) else null,
                modifier = Modifier.weight(1f)
            ) {
                onOpenColorPicker(SocialColorTarget.BACKGROUND)
            }
        }

        // 4. Accent Color Swatches
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
                    onClick = { onAccentHexSelected(hex) }
                )
            }

            val isCustomAccent = accentPresets.none { it == selectedAccentHex }
            RainbowCustomCircle(
                isSelected = isCustomAccent,
                activeColor = if (isCustomAccent) Color(selectedAccentHex) else null,
                onClick = { onOpenColorPicker(SocialColorTarget.ACCENT) }
            )
        }

        // 5. Surface Translucency
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
        ModernOpacitySlider(value = opacity, onValueChange = onOpacityChanged)

        // 6. Sizing Mode
        if (hasModeOption) {
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
                            .clickable { onResponsiveChanged(responsiveVal) },
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

// -----------------------------------------------------------------------------
// HELPER COMPONENTS
// -----------------------------------------------------------------------------

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
