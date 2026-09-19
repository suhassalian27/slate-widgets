package com.altusix.slate.widgets.ai

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.altusix.slate.core.theme.ThemePreferences
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.ui.components.*

enum class AiColorTarget { BACKGROUND, ACCENT }

class AiConfigActivity : ComponentActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetClassName: String = ""

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

        val appWidgetInfo = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)
        } else null
        widgetClassName = appWidgetInfo?.provider?.className ?: ""

        val catalogItem = getAiWidgetsCatalog().find { it.receiverClass.name == widgetClassName }
        val widgetName = catalogItem?.name ?: "AI Widget"
        val defaultTheme = ThemePreferences(this).getThemeSettings()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF0A0A0C), surface = Color(0xFF16161B))) {
                Surface(
                    modifier = Modifier.fillMaxSize().imePadding(),
                    color = Color(0xFF0A0A0C)
                ) {
                    var selectedBgHex by remember { mutableLongStateOf(0xFF161618L) }
                    var selectedAccentHex by remember { mutableLongStateOf(defaultTheme.accentHex) }
                    var opacity by remember { mutableFloatStateOf(1.0f) }
                    var isResponsive by remember { mutableStateOf(true) }
                    var activePickerTarget by remember { mutableStateOf<AiColorTarget?>(null) }
                    var selectedTabKey by remember { mutableStateOf("SERVICES") }

                    val tabs = remember {
                        listOf(
                            ConfigTabItem("SERVICES", "Services"),
                            ConfigTabItem("STYLE", "Widget Theme")
                        )
                    }

                    LaunchedEffect(widgetId) {
                        val prefs = getSharedPreferences("slate_widget_prefs", MODE_PRIVATE)
                        opacity = prefs.getFloat("widget_${widgetId}_opacity", 1.0f)
                        val modeStr = prefs.getString("widget_${widgetId}_mode", null)
                        isResponsive = if (modeStr != null) {
                            modeStr == "RESPONSIVE"
                        } else {
                            prefs.getBoolean("widget_${widgetId}_is_responsive", true)
                        }
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
                        val prefs = getSharedPreferences("slate_widget_prefs", MODE_PRIVATE)
                        prefs.edit().apply {
                            putString("widget_${widgetId}_theme_mode", currentSlateConfig.themeMode)
                            putLong("widget_${widgetId}_bg_color", currentSlateConfig.backgroundColorHex)
                            putFloat("widget_${widgetId}_opacity", currentSlateConfig.opacity)
                            putLong("widget_${widgetId}_accent_color", currentSlateConfig.accentColorHex)
                            putBoolean("widget_${widgetId}_is_responsive", isResponsive)
                            putString("widget_${widgetId}_mode", if (isResponsive) "RESPONSIVE" else "FIXED")
                            apply()
                        }

                        // Immediate direct sync
                        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            try {
                                val receiverClass = Class.forName(widgetClassName)
                                val receiverInstance = receiverClass.getDeclaredConstructor().newInstance()
                                if (receiverInstance is BaseAiReceiver) {
                                    receiverInstance.updateWidget(this@AiConfigActivity, AppWidgetManager.getInstance(this@AiConfigActivity), widgetId)
                                }
                            } catch (_: Exception) {}
                        }
                        updateAllAiWidgets(this@AiConfigActivity)
                        updateAllAiFolderWidgets(this@AiConfigActivity)

                        val resultIntent = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }

                    val sizeText = catalogItem?.sizeText ?: "2x2"
                    val (slotWDp, slotHDp) = remember(sizeText) {
                        when (sizeText) {
                            "4x1" -> 280 to 60
                            "4x2" -> 288 to 120
                            "3x2" -> 240 to 120
                            else -> 150 to 150
                        }
                    }

                    val context = LocalContext.current
                    val previewBitmap = remember(currentSlateConfig, isResponsive, widgetClassName) {
                        renderAiPreviewBitmap(context, widgetClassName, currentSlateConfig, isResponsive, slotWDp, slotHDp, widgetId)
                    }

                    SlateConfigScaffold(
                        title = "AI Studio",
                        subtitle = widgetName,
                        accentColor = Color(selectedAccentHex),
                        tabs = tabs,
                        selectedTabKey = selectedTabKey,
                        onTabSelected = { selectedTabKey = it },
                        onBackClick = { finish() },
                        onSaveClick = { saveAndFinish() },
                        scrollable = selectedTabKey == "STYLE",
                        previewHeight = 180.dp,
                        previewContent = {
                            Box(
                                modifier = Modifier
                                    .width(slotWDp.dp)
                                    .height(slotHDp.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (previewBitmap != null) {
                                    Image(
                                        bitmap = previewBitmap.asImageBitmap(),
                                        contentDescription = "AI Widget Preview",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    ) {
                        if (selectedTabKey == "SERVICES") {
                            AiServicesTab(context = context, accentColor = Color(selectedAccentHex))
                        } else {
                            AiStyleTab(
                                selectedBgHex = selectedBgHex,
                                onBgSelect = { selectedBgHex = it },
                                selectedAccentHex = selectedAccentHex,
                                onAccentSelect = { selectedAccentHex = it },
                                opacity = opacity,
                                onOpacityChange = { opacity = it },
                                isResponsive = isResponsive,
                                onResponsiveChange = { isResponsive = it },
                                onPickCustomColor = { activePickerTarget = it }
                            )
                        }
                    }

                    activePickerTarget?.let { target ->
                        val initialColor = if (target == AiColorTarget.BACKGROUND) Color(selectedBgHex) else Color(selectedAccentHex)
                        CustomColorPickerDialog(
                            initialColor = initialColor,
                            title = if (target == AiColorTarget.BACKGROUND) "Custom Background" else "Custom Accent Color",
                            onColorSelected = { pickedColor ->
                                val hex = pickedColor.toArgb().toLong() and 0xFFFFFFFFL
                                if (target == AiColorTarget.BACKGROUND) {
                                    selectedBgHex = hex
                                } else {
                                    selectedAccentHex = hex
                                }
                                activePickerTarget = null
                            },
                            onDismiss = { activePickerTarget = null }
                        )
                    }
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
}

private fun renderAiPreviewBitmap(
    context: Context,
    className: String,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap? {
    return try {
        val clazz = Class.forName(className)
        val constructor = clazz.getDeclaredConstructor()
        constructor.isAccessible = true
        val instance = constructor.newInstance()
        if (instance is BaseAiReceiver) {
            instance.renderWidgetBitmap(context, widgetId, config, isResponsive, wDp, hDp)
        } else null
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun AiServicesTab(context: Context, accentColor: Color) {
    val pm = context.packageManager
    val services = remember {
        listOf(
            AiTarget.GEMINI_TEXT,
            AiTarget.CHATGPT_TEXT,
            AiTarget.CHATGPT_VOICE,
            AiTarget.CLAUDE,
            AiTarget.GROK,
            AiTarget.PERPLEXITY,
            AiTarget.DEEPSEEK,
            AiTarget.COPILOT,
            AiTarget.META_AI
        )
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    ) {
        item {
            Text(
                text = "SUPPORTED AI ENGINES",
                color = Color(0xFF8E8E93),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        items(services) { target ->
            val isInstalled = remember(target) {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        pm.getPackageInfo(target.packageName, PackageManager.PackageInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getPackageInfo(target.packageName, 0)
                    }
                    true
                } catch (_: Exception) {
                    false
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF141418))
                    .border(1.dp, Color(0xFF24242C), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = target.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isInstalled) "App Installed • Direct Launch" else "Web / Play Store Fallback",
                        color = if (isInstalled) Color(0xFF34C759) else Color(0xFF8E8E93),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .clickable {
                            val intent = AiLauncherUtils.getLaunchIntent(context, target)
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Test Launch",
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AiStyleTab(
    selectedBgHex: Long,
    onBgSelect: (Long) -> Unit,
    selectedAccentHex: Long,
    onAccentSelect: (Long) -> Unit,
    opacity: Float,
    onOpacityChange: (Float) -> Unit,
    isResponsive: Boolean,
    onResponsiveChange: (Boolean) -> Unit,
    onPickCustomColor: (AiColorTarget) -> Unit
) {
    val bgPresets = listOf(
        0xFF161618L to "Matte",
        0xFF000000L to "AMOLED",
        0xFFFFFFFFL to "Light"
    )
    val isLightBg = ((selectedBgHex shr 16 and 0xFFL) * 0.2126f + (selectedBgHex shr 8 and 0xFFL) * 0.7152f + (selectedBgHex and 0xFFL) * 0.0722f) / 255f > 0.5f
    val accentPresets = if (isLightBg) {
        listOf(0xFF000000L, 0xFF00D166L, 0xFF2B80FFL, 0xFFFF3B30L, 0xFFFF9500L, 0xFFAF52DEL)
    } else {
        listOf(0xFFFFFFFFL, 0xFF00D166L, 0xFF2B80FFL, 0xFFFF3B30L, 0xFFFF9500L, 0xFFAF52DEL)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Background", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onBgSelect(hex)
                            if (hex == 0xFFFFFFFFL && selectedAccentHex == 0xFFFFFFFFL) {
                                onAccentSelect(0xFF000000L)
                            } else if (hex != 0xFFFFFFFFL && selectedAccentHex == 0xFF000000L) {
                                onAccentSelect(0xFFFFFFFFL)
                            }
                        }
                    )
                }
                val isCustomBg = bgPresets.none { it.first == selectedBgHex }
                RainbowPickerChip(
                    isSelected = isCustomBg,
                    activeColor = if (isCustomBg) Color(selectedBgHex) else null,
                    modifier = Modifier.weight(1f),
                    onClick = { onPickCustomColor(AiColorTarget.BACKGROUND) }
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Accent Color", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                accentPresets.forEach { hex ->
                    ProfessionalSwatchCircle(
                        color = Color(hex),
                        isSelected = selectedAccentHex == hex,
                        onClick = { onAccentSelect(hex) }
                    )
                }
                val isCustomAccent = accentPresets.none { it == selectedAccentHex }
                RainbowCustomCircle(
                    isSelected = isCustomAccent,
                    activeColor = if (isCustomAccent) Color(selectedAccentHex) else null,
                    onClick = { onPickCustomColor(AiColorTarget.ACCENT) }
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Surface Translucency", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "${(opacity * 100).toInt()}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            ModernOpacitySlider(value = opacity, onValueChange = onOpacityChange)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Sizing Mode", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
                            .clickable { onResponsiveChange(responsiveVal) },
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
