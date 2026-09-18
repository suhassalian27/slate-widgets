package com.altusix.slate.widgets.weather

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WeatherConfigActivity : ComponentActivity() {

    private enum class WeatherColorTarget { BACKGROUND, ACCENT }

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetClassName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        // Ensure background scheduler is running
        WeatherSyncWorker.enqueue(this)

        widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val appWidgetInfo = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)
        } else null
        widgetClassName = appWidgetInfo?.provider?.className ?: ""

        val catalogItem = getWeatherWidgetsCatalog().find { it.receiverClass.name == widgetClassName }
        val hasModeOption = catalogItem?.hasModeOption ?: true
        val defaultTheme = ThemePreferences(this).getThemeSettings()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF0A0A0C), surface = Color(0xFF16161B))) {
                val coroutineScope = rememberCoroutineScope()

                var currentCity by remember { mutableStateOf(WeatherPreferences.getSelectedCity(this@WeatherConfigActivity)) }
                var currentUnit by remember { mutableStateOf(WeatherPreferences.getUnit(this@WeatherConfigActivity)) }
                var weatherData by remember { mutableStateOf(WeatherPreferences.getCachedWeatherData(this@WeatherConfigActivity)) }
                var isSyncing by remember { mutableStateOf(false) }

                var selectedBgHex by remember { mutableLongStateOf(0xFF161618L) }
                var selectedAccentHex by remember { mutableLongStateOf(defaultTheme.accentHex) }
                var opacity by remember { mutableFloatStateOf(1.0f) }
                var isResponsive by remember { mutableStateOf(true) }
                var activePickerTarget by remember { mutableStateOf<WeatherColorTarget?>(null) }

                var searchQuery by remember { mutableStateOf("") }
                var searchResults by remember { mutableStateOf<List<WeatherCity>>(emptyList()) }
                var isSearching by remember { mutableStateOf(false) }
                var searchJob by remember { mutableStateOf<Job?>(null) }

                var selectedTabKey by remember { mutableStateOf("LOCATION") }

                val tabs = remember {
                    listOf(
                        ConfigTabItem("LOCATION", "Location & Units"),
                        ConfigTabItem("STYLE", "Widget Theme")
                    )
                }
                val widgetName = catalogItem?.name ?: "Weather Widget"

                LaunchedEffect(widgetId) {
                    val prefs = getSharedPreferences("slate_widget_prefs", MODE_PRIVATE)
                    opacity = prefs.getFloat("widget_${widgetId}_opacity", 1.0f)
                    isResponsive = prefs.getBoolean("widget_${widgetId}_is_responsive", true)
                    selectedBgHex = prefs.getLong("widget_${widgetId}_bg_color", 0xFF161618L)
                    selectedAccentHex = prefs.getLong("widget_${widgetId}_accent_color", defaultTheme.accentHex)
                }

                // Debounced live city search
                LaunchedEffect(searchQuery) {
                    searchJob?.cancel()
                    if (searchQuery.trim().length >= 2) {
                        searchJob = coroutineScope.launch {
                            delay(400)
                            isSearching = true
                            val results = WeatherRepository.searchCities(searchQuery)
                            searchResults = results
                            isSearching = false
                        }
                    } else {
                        searchResults = emptyList()
                        isSearching = false
                    }
                }

                fun syncWeatherForCity(city: WeatherCity) {
                    coroutineScope.launch {
                        isSyncing = true
                        val fresh = WeatherRepository.fetchWeather(city.latitude, city.longitude, city.name)
                        if (fresh != null) {
                            WeatherPreferences.setCachedWeatherData(this@WeatherConfigActivity, fresh)
                            weatherData = fresh
                            // Immediately push fresh data to all active widgets on screen
                            updateAllWeatherWidgets(this@WeatherConfigActivity)
                        }
                        isSyncing = false
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
                    saveSlateWidgetConfig(this@WeatherConfigActivity, widgetId, currentSlateConfig, isResponsive)
                    // Ensure periodic worker is running
                    WeatherSyncWorker.enqueue(this@WeatherConfigActivity)
                    // Refresh widgets immediately
                    updateAllWeatherWidgets(this@WeatherConfigActivity)

                    if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        try {
                            val manager = AppWidgetManager.getInstance(this@WeatherConfigActivity)
                            val info = manager.getAppWidgetInfo(widgetId)
                            if (info != null) {
                                val receiverClass = Class.forName(info.provider.className)
                                val receiver = receiverClass.getDeclaredConstructor().newInstance() as? BaseWeatherReceiver
                                receiver?.updateSingleWidget(this@WeatherConfigActivity, manager, widgetId)
                            }
                        } catch (_: Exception) {}
                    }
                    val resultIntent = Intent().apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }

                SlateConfigScaffold(
                    title = "Weather",
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
                        val slot = remember(widgetClassName) {
                            when {
                                widgetClassName.contains("Horizon") || widgetClassName.contains("BentoGlance") || widgetClassName.contains("FluidPebble") ->
                                    WeatherPreviewSlot(288, 120, 260.dp, 108.dp)
                                widgetClassName.contains("PillDock") || widgetClassName.contains("HourlyRibbon") || widgetClassName.contains("SolarTrack") ->
                                    WeatherPreviewSlot(280, 60, 260.dp, 56.dp)
                                widgetClassName.contains("MetroTrio") ->
                                    WeatherPreviewSlot(240, 65, 230.dp, 62.dp)
                                widgetClassName.contains("Micro") ->
                                    WeatherPreviewSlot(100, 85, 95.dp, 80.dp)
                                else ->
                                    WeatherPreviewSlot(160, 133, 150.dp, 125.dp)
                            }
                        }

                        val previewBitmap = remember(weatherData, currentSlateConfig, isResponsive, widgetClassName, currentUnit) {
                            when {
                                widgetClassName.contains("Horizon") -> generateWeatherHorizonBitmap(context, currentSlateConfig, isResponsive, slot.slotWDp, slot.slotHDp, 0)
                                widgetClassName.contains("BentoGlance") -> generateWeatherBentoGlanceBitmap(context, currentSlateConfig, isResponsive, slot.slotWDp, slot.slotHDp, 0)
                                widgetClassName.contains("DaylightArc") -> generateWeatherDaylightArcBitmap(context, currentSlateConfig, isResponsive, slot.slotWDp, slot.slotHDp, 0)
                                widgetClassName.contains("PillDock") -> generateWeatherPillDockBitmap(context, currentSlateConfig, isResponsive, slot.slotWDp, slot.slotHDp, 0)
                                widgetClassName.contains("Editorial") -> generateWeatherEditorialBitmap(context, currentSlateConfig, isResponsive, slot.slotWDp, slot.slotHDp, 0)
                                widgetClassName.contains("MinimalistDual") -> generateWeatherMinimalistDualBitmap(context, currentSlateConfig, isResponsive, slot.slotWDp, slot.slotHDp, 0)
                                widgetClassName.contains("CompactDial") -> generateWeatherCompactDialBitmap(context, currentSlateConfig, isResponsive, slot.slotWDp, slot.slotHDp, 0)
                                widgetClassName.contains("MetroTrio") -> generateWeatherMetroTrioBitmap(context, currentSlateConfig, isResponsive, slot.slotWDp, slot.slotHDp, 0)
                                widgetClassName.contains("Micro") -> generateWeatherMicroBitmap(context, currentSlateConfig, isResponsive, slot.slotWDp, slot.slotHDp, 0)
                                widgetClassName.contains("Celestial") -> generateWeatherCelestialBitmap(context, currentSlateConfig, isResponsive, slot.slotWDp, slot.slotHDp, 0)
                                widgetClassName.contains("LunarSolo") -> generateWeatherLunarSoloBitmap(context, currentSlateConfig, isResponsive, slot.slotWDp, slot.slotHDp, 0)
                                widgetClassName.contains("OrbitDial") -> generateWeatherOrbitDialBitmap(context, currentSlateConfig, isResponsive, slot.slotWDp, slot.slotHDp, 0)
                                widgetClassName.contains("SolarTrack") -> generateWeatherSolarTrackBitmap(context, currentSlateConfig, isResponsive, slot.slotWDp, slot.slotHDp, 0)
                                widgetClassName.contains("FluidPebble") -> generateWeatherFluidPebbleBitmap(context, currentSlateConfig, isResponsive, slot.slotWDp, slot.slotHDp, 0)
                                else -> generateWeatherHorizonBitmap(context, currentSlateConfig, isResponsive, slot.slotWDp, slot.slotHDp, 0)
                            }
                        }

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(slot.displayW, slot.displayH)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF101014))
                                    .border(
                                        width = 1.dp,
                                        color = if (!isResponsive) Color(selectedAccentHex).copy(alpha = 0.45f) else Color(0xFF262630),
                                        shape = RoundedCornerShape(20.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = previewBitmap.asImageBitmap(),
                                    contentDescription = "Weather Widget Preview",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                ) {
                    if (selectedTabKey == "LOCATION") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF141418))
                                .border(1.dp, Color(0xFF24242C), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(selectedAccentHex).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(selectedAccentHex),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = currentCity.name,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (currentCity.country.isNotBlank()) "${currentCity.admin1.ifBlank { "" }} ${currentCity.country}".trim() else "Selected City",
                                        color = Color(0xFF8E8E93),
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1F1F26))
                                    .clickable(enabled = !isSyncing) { syncWeatherForCity(currentCity) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        color = Color(selectedAccentHex),
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text("Sync", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SectionTitle(title = "Temperature Unit")
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
                                    WeatherPreferences.UNIT_CELSIUS to "Celsius (°C)",
                                    WeatherPreferences.UNIT_FAHRENHEIT to "Fahrenheit (°F)"
                                ).forEach { (unitKey, label) ->
                                    val isSelected = currentUnit == unitKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(if (isSelected) Color(0xFF282832) else Color.Transparent)
                                            .clickable {
                                                currentUnit = unitKey
                                                WeatherPreferences.setUnit(this@WeatherConfigActivity, unitKey)
                                                updateAllWeatherWidgets(this@WeatherConfigActivity)
                                            },
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
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SectionTitle(title = "Search & Change City")
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search city (e.g. London, Tokyo, New York...)", color = Color(0xFF8E8E93), fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(16.dp)) },
                                trailingIcon = {
                                    if (isSearching) {
                                        CircularProgressIndicator(
                                            color = Color(selectedAccentHex),
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                },
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
                        }

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
                            val displayList = if (searchResults.isNotEmpty()) searchResults else DEFAULT_CITIES

                            items(displayList) { city ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (city.name == currentCity.name) Color(0xFF22222C) else Color(0xFF1A1A22))
                                        .clickable {
                                            currentCity = city
                                            WeatherPreferences.setSelectedCity(this@WeatherConfigActivity, city)
                                            syncWeatherForCity(city)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = city.name,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${city.admin1.ifBlank { "" }} ${city.country}".trim(),
                                            color = Color(0xFF8E8E93),
                                            fontSize = 10.sp
                                        )
                                    }

                                    if (city.name == currentCity.name) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color(selectedAccentHex),
                                            modifier = Modifier.size(16.dp)
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
                                    activePickerTarget = WeatherColorTarget.BACKGROUND
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionTitle(title = "Accent Color")
                            val accentPresets = if (isLightBg) {
                                listOf(0xFF000000L, 0xFF38ACFFL, 0xFFFF9500L, 0xFF00D166L, 0xFFFF3B30L, 0xFFAF52DEL)
                            } else {
                                listOf(0xFFFFFFFFL, 0xFF38ACFFL, 0xFFFF9500L, 0xFF00D166L, 0xFFFF3B30L, 0xFFAF52DEL)
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
                                    onClick = { activePickerTarget = WeatherColorTarget.ACCENT }
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
                    val initialColor = if (target == WeatherColorTarget.BACKGROUND) Color(selectedBgHex) else Color(selectedAccentHex)
                    CustomColorPickerDialog(
                        initialColor = initialColor,
                        title = if (target == WeatherColorTarget.BACKGROUND) "Custom Background" else "Custom Accent",
                        onDismiss = { activePickerTarget = null },
                        onColorSelected = { color ->
                            val hex = (color.toArgb().toLong() and 0xFFFFFFFFL)
                            if (target == WeatherColorTarget.BACKGROUND) {
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

    companion object {
        val DEFAULT_CITIES = listOf(
            WeatherCity(1, "San Francisco", "United States", "California", 37.7749, -122.4194),
            WeatherCity(2, "New York", "United States", "New York", 40.7128, -74.0060),
            WeatherCity(3, "London", "United Kingdom", "England", 51.5074, -0.1278),
            WeatherCity(4, "Tokyo", "Japan", "Tokyo", 35.6762, 139.6503),
            WeatherCity(5, "Paris", "France", "Île-de-France", 48.8566, 2.3522),
            WeatherCity(6, "Sydney", "Australia", "New South Wales", -33.8688, 151.2093),
            WeatherCity(7, "Berlin", "Germany", "Berlin", 52.5200, 13.4050),
            WeatherCity(8, "Bengaluru", "India", "Karnataka", 12.9716, 77.5946)
        )
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

private data class WeatherPreviewSlot(
    val slotWDp: Int,
    val slotHDp: Int,
    val displayW: androidx.compose.ui.unit.Dp,
    val displayH: androidx.compose.ui.unit.Dp
)
