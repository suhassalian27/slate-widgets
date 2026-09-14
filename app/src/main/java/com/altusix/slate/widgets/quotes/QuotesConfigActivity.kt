package com.altusix.slate.widgets.quotes

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altusix.slate.core.theme.ThemePreferences
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.ui.components.ConfigTabItem
import com.altusix.slate.ui.components.CustomColorPickerDialog
import com.altusix.slate.ui.components.RainbowCustomCircle
import com.altusix.slate.ui.components.SlateConfigScaffold

private enum class QuotesColorTarget { BACKGROUND, ACCENT }

private fun calculateLuminance(hex: Long): Float {
    val r = ((hex shr 16) and 0xFFL) / 255f
    val g = ((hex shr 8) and 0xFFL) / 255f
    val b = (hex and 0xFFL) / 255f
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

class QuotesConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        val widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val appWidgetInfo = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)
        } else null
        val widgetClassName = appWidgetInfo?.provider?.className ?: ""

        val catalogItem = getQuotesWidgetsCatalog().find { it.receiverClass.name == widgetClassName }
        val widgetName = catalogItem?.name ?: "Quotes"
        val hasModeOption = catalogItem?.hasModeOption ?: true

        val defaultTheme = ThemePreferences(this).getThemeSettings()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF0A0A0C), surface = Color(0xFF16161B))) {
                var selectedTabKey by remember { mutableStateOf("QUOTES") }
                var allQuotes by remember { mutableStateOf(QuotesStorageManager.getAllQuotes(this)) }
                var activeQuote by remember {
                    mutableStateOf(
                        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            QuotesStorageManager.getQuoteForWidget(this, widgetId)
                        } else {
                            QuotesStorageManager.CURATED_QUOTES.first()
                        }
                    )
                }

                // Standardized: "STATIC", "DAILY", "HOURLY"
                var rotationMode by remember { mutableStateOf("STATIC") }
                var selectedFilter by remember { mutableStateOf("ALL") }
                var selectedBgHex by remember { mutableLongStateOf(0xFF000000L) }
                var selectedAccentHex by remember { mutableLongStateOf(defaultTheme.accentHex) }
                var opacity by remember { mutableFloatStateOf(1.0f) }
                var isResponsive by remember { mutableStateOf(true) }
                var activePickerTarget by remember { mutableStateOf<QuotesColorTarget?>(null) }

                LaunchedEffect(widgetId) {
                    val prefs = getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
                    opacity = prefs.getFloat("widget_${widgetId}_opacity", 1.0f)
                    isResponsive = prefs.getBoolean("widget_${widgetId}_is_responsive", true)
                    selectedBgHex = prefs.getLong("widget_${widgetId}_bg_color", 0xFF000000L)
                    selectedAccentHex = prefs.getLong("widget_${widgetId}_accent_color", defaultTheme.accentHex)
                    rotationMode = QuotesStorageManager.getWidgetRotationMode(this@QuotesConfigActivity, widgetId)
                    selectedFilter = QuotesStorageManager.getWidgetRotationPool(this@QuotesConfigActivity, widgetId)
                }

                fun refreshQuotes() {
                    allQuotes = QuotesStorageManager.getAllQuotes(this)
                }

                val tabs = remember {
                    listOf(
                        ConfigTabItem("QUOTES", "Quotes"),
                        ConfigTabItem("STYLE", "Widget Theme")
                    )
                }

                val currentConfig = SlateWidgetConfig(
                    themeMode = if (calculateLuminance(selectedBgHex) > 0.5f) "LIGHT" else "DARK",
                    backgroundColorHex = selectedBgHex,
                    opacity = opacity,
                    accentColorHex = selectedAccentHex
                )

                SlateConfigScaffold(
                    title = "Quotes Studio",
                    subtitle = widgetName,
                    accentColor = Color(selectedAccentHex),
                    tabs = tabs,
                    selectedTabKey = selectedTabKey,
                    onTabSelected = { selectedTabKey = it },
                    onBackClick = { finish() },
                    onSaveClick = {
                        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            QuotesStorageManager.setQuoteForWidget(this@QuotesConfigActivity, widgetId, activeQuote.id)
                            QuotesStorageManager.setWidgetRotationMode(this@QuotesConfigActivity, widgetId, rotationMode, selectedFilter)
                            val isLight = calculateLuminance(selectedBgHex) > 0.5f

                            getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE).edit()
                                .putString("widget_${widgetId}_theme_mode", if (isLight) "LIGHT" else "DARK")
                                .putLong("widget_${widgetId}_bg_color", selectedBgHex)
                                .putLong("widget_${widgetId}_accent_color", selectedAccentHex)
                                .putFloat("widget_${widgetId}_opacity", opacity)
                                .putBoolean("widget_${widgetId}_is_responsive", isResponsive)
                                .putString("widget_${widgetId}_mode", if (isResponsive) "RESPONSIVE" else "FIXED")
                                .apply()
                        }
                        updateAllQuotesWidgets(this@QuotesConfigActivity)
                        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                        finish()
                    },
                    scrollable = selectedTabKey == "STYLE",
                    previewHeight = 175.dp,
                    previewContent = {
                        QuoteLivePreview(
                            widgetClassName = widgetClassName,
                            quote = activeQuote,
                            config = currentConfig,
                            isResponsive = isResponsive
                        )
                    }
                ) {
                    when (selectedTabKey) {
                        "QUOTES" -> {
                            QuotesUnifiedTab(
                                allQuotes = allQuotes,
                                activeQuote = activeQuote,
                                rotationMode = rotationMode,
                                selectedFilter = selectedFilter,
                                accentColor = Color(selectedAccentHex),
                                onRotationModeChanged = { newMode ->
                                    rotationMode = newMode
                                    if (newMode != "STATIC") {
                                        activeQuote = QuotesStorageManager.getRotatingQuote(allQuotes, newMode, selectedFilter, widgetId)
                                    }
                                },
                                onFilterChanged = { newFilter ->
                                    selectedFilter = newFilter
                                    if (rotationMode != "STATIC") {
                                        activeQuote = QuotesStorageManager.getRotatingQuote(allQuotes, rotationMode, newFilter, widgetId)
                                    }
                                },
                                onQuoteSelected = { selected ->
                                    activeQuote = selected
                                    if (rotationMode == "STATIC" && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                                        QuotesStorageManager.setQuoteForWidget(this@QuotesConfigActivity, widgetId, selected.id)
                                    }
                                },
                                onPinQuote = { selected ->
                                    activeQuote = selected
                                    rotationMode = "STATIC"
                                    if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                                        QuotesStorageManager.setQuoteForWidget(this@QuotesConfigActivity, widgetId, selected.id)
                                    }
                                    Toast.makeText(this@QuotesConfigActivity, "Quote pinned to widget", Toast.LENGTH_SHORT).show()
                                },
                                onToggleFavorite = { quoteId ->
                                    QuotesStorageManager.toggleFavoriteById(this@QuotesConfigActivity, quoteId)
                                    refreshQuotes()
                                },
                                onAddCustomQuote = { newQuote ->
                                    QuotesStorageManager.saveCustomQuote(this@QuotesConfigActivity, newQuote)
                                    refreshQuotes()
                                    activeQuote = newQuote
                                },
                                onDeleteCustomQuote = { quoteId ->
                                    QuotesStorageManager.deleteCustomQuote(this@QuotesConfigActivity, quoteId)
                                    refreshQuotes()
                                    if (activeQuote.id == quoteId) {
                                        activeQuote = QuotesStorageManager.CURATED_QUOTES.first()
                                    }
                                }
                            )
                        }
                        "STYLE" -> {
                            QuotesThemeTab(
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
                    val initialColor = if (activePickerTarget == QuotesColorTarget.BACKGROUND) {
                        Color(selectedBgHex)
                    } else {
                        Color(selectedAccentHex)
                    }
                    val dialogTitle = if (activePickerTarget == QuotesColorTarget.BACKGROUND) {
                        "Custom Background"
                    } else {
                        "Custom Accent"
                    }

                    CustomColorPickerDialog(
                        initialColor = initialColor,
                        title = dialogTitle,
                        onDismiss = { activePickerTarget = null },
                        onColorSelected = { picked ->
                            val hex = picked.toArgb().toLong() and 0xFFFFFFFFL
                            if (activePickerTarget == QuotesColorTarget.BACKGROUND) {
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

// -----------------------------------------------------------------------------
// TRUE LIVE WIDGET PREVIEW (With Safe Aspect Ratio Fallback)
// -----------------------------------------------------------------------------

@Composable
private fun QuoteLivePreview(
    widgetClassName: String,
    quote: QuoteItem,
    config: SlateWidgetConfig,
    isResponsive: Boolean
) {
    val context = LocalContext.current

    val receiver = remember(widgetClassName) {
        try {
            val resolvedName = if (widgetClassName.isNotBlank()) widgetClassName else {
                getQuotesWidgetsCatalog().firstOrNull()?.receiverClass?.name ?: ""
            }
            val clazz = Class.forName(resolvedName)
            clazz.getDeclaredConstructor().newInstance() as? BaseQuotesReceiver
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Fallback: If targetAspect is 0f (Organic Pebble, Vertical Capsule, Ceramic Disc), default to 1.0f (2x2 square)
    val effectiveAspect = remember(receiver) {
        val rawAspect = receiver?.targetAspect ?: 0f
        if (rawAspect > 0.05f) {
            rawAspect
        } else {
            1.0f
        }
    }

    val (defaultW, defaultH) = remember(effectiveAspect) {
        when {
            effectiveAspect >= 3.5f -> 320 to 72   // 4x1
            effectiveAspect >= 1.8f -> 320 to 156  // 4x2
            effectiveAspect <= 1.2f -> 156 to 156  // 2x2
            else -> 160 to 72                   // 2x1
        }
    }

    val maxDisplayW = 310f
    val maxDisplayH = 150f
    val (displayW, displayH) = remember(effectiveAspect) {
        if (maxDisplayW / maxDisplayH > effectiveAspect) {
            (maxDisplayH * effectiveAspect) to maxDisplayH
        } else {
            maxDisplayW to (maxDisplayW / effectiveAspect)
        }
    }

    val bitmap = remember(widgetClassName, quote, config, isResponsive, defaultW, defaultH) {
        try {
            QuotesStorageManager.previewQuoteOverride = quote
            receiver?.renderWidgetBitmap(
                context = context,
                appWidgetId = -1,
                config = config,
                isResponsive = isResponsive,
                wDp = defaultW,
                hDp = defaultH
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Widget Live Preview",
                modifier = Modifier.size(displayW.dp, displayH.dp)
            )
        } else {
            CircularProgressIndicator(
                color = Color(config.accentColorHex),
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        }
    }
}

// -----------------------------------------------------------------------------
// UNIFIED QUOTES TAB (Standardized Pinned / Daily / Hourly Rotation)
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuotesUnifiedTab(
    allQuotes: List<QuoteItem>,
    activeQuote: QuoteItem,
    rotationMode: String,
    selectedFilter: String,
    accentColor: Color,
    onRotationModeChanged: (String) -> Unit,
    onFilterChanged: (String) -> Unit,
    onQuoteSelected: (QuoteItem) -> Unit,
    onPinQuote: (QuoteItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onAddCustomQuote: (QuoteItem) -> Unit,
    onDeleteCustomQuote: (String) -> Unit
) {
    val hasFavorites = remember(allQuotes) { allQuotes.any { it.isFavorite } }
    val standardCategories = listOf("ALL", "MY QUOTES", "STOICISM", "MINDFULNESS", "LITERATURE", "PRODUCTIVITY", "SCIENCE", "AFFIRMATION")

    var showAddQuoteSheet by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }
    var customAuthor by remember { mutableStateOf("") }

    val filteredQuotes = remember(selectedFilter, allQuotes) {
        when (selectedFilter) {
            "FAVORITES" -> allQuotes.filter { it.isFavorite }
            "MY QUOTES" -> allQuotes.filter { it.category == "CUSTOM" }
            "ALL" -> allQuotes
            else -> allQuotes.filter { it.category.equals(selectedFilter, ignoreCase = true) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. Standardized Segmented Schedule Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF141416))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                "STATIC" to "Pinned",
                "DAILY" to "Daily",
                "HOURLY" to "Hourly"
            ).forEach { (mode, label) ->
                val isSelected = rotationMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFF2C2C30) else Color.Transparent)
                        .clickable { onRotationModeChanged(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color(0xFF8E8E93),
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }

        // Status caption linking the schedule directly to the selected pool
        val activeCategoryLabel = if (selectedFilter == "ALL") "All Library" else selectedFilter
        val scheduleHint = when (rotationMode) {
            "HOURLY" -> "Rotates every hour from $activeCategoryLabel"
            "DAILY" -> "Rotates once daily from $activeCategoryLabel"
            else -> "Pinned: displaying your selected quote continuously"
        }
        Text(
            text = scheduleHint,
            fontSize = 11.sp,
            color = if (rotationMode == "STATIC") Color(0xFF8E8E93) else accentColor,
            fontWeight = if (rotationMode == "STATIC") FontWeight.Normal else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
        )

        // 2. Horizontal Filter Bar (Defines both display and rotation pool)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasFavorites) {
                item {
                    val isFavSelected = selectedFilter == "FAVORITES"
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isFavSelected) Color(0xFFFF3B30) else Color(0xFF1E1E24))
                            .clickable { onFilterChanged("FAVORITES") }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorites",
                            tint = if (isFavSelected) Color.White else Color(0xFFFF5252),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            items(standardCategories) { cat ->
                val isSelected = selectedFilter == cat
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isSelected) accentColor else Color(0xFF1E1E24))
                        .clickable { onFilterChanged(cat) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) {
                            if (calculateLuminance(accentColor.toArgb().toLong()) > 0.5f) Color.Black else Color.White
                        } else Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // 3. Action Card for Custom Quotes (When browsing MY QUOTES)
        if (selectedFilter == "MY QUOTES") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .clickable { showAddQuoteSheet = true }
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Write New Custom Quote",
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
            }
        }

        // 4. Quotes List
        if (filteredQuotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedFilter == "MY QUOTES") "No custom quotes yet.\nTap above to write one!" else "No quotes found.",
                    color = Color(0xFF8E8E93),
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredQuotes, key = { it.id }) { quote ->
                    val isSelected = quote.id == activeQuote.id
                    QuoteListItem(
                        quote = quote,
                        isSelected = isSelected,
                        rotationMode = rotationMode,
                        accentColor = accentColor,
                        onSelect = { onQuoteSelected(quote) },
                        onPin = { onPinQuote(quote) },
                        onToggleFavorite = { onToggleFavorite(quote.id) },
                        onDelete = if (quote.category == "CUSTOM") { { onDeleteCustomQuote(quote.id) } } else null
                    )
                }
            }
        }
    }

    // Modal BottomSheet for Adding Quotes
    if (showAddQuoteSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddQuoteSheet = false },
            containerColor = Color(0xFF16161B),
            contentColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF383842)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "New Custom Quote",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it },
                    placeholder = { Text("Write your quote here...", color = Color(0xFF8E8E93), fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    textStyle = TextStyle(color = Color.White, fontSize = 13.5.sp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color(0xFF2E2E38),
                        focusedContainerColor = Color(0xFF0F0F12),
                        unfocusedContainerColor = Color(0xFF0F0F12)
                    )
                )

                OutlinedTextField(
                    value = customAuthor,
                    onValueChange = { customAuthor = it },
                    placeholder = { Text("Author (e.g. Me, Anonymous)", color = Color(0xFF8E8E93), fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 13.5.sp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color(0xFF2E2E38),
                        focusedContainerColor = Color(0xFF0F0F12),
                        unfocusedContainerColor = Color(0xFF0F0F12)
                    )
                )

                Button(
                    onClick = {
                        if (customText.isNotBlank()) {
                            val newQuote = QuoteItem(
                                id = "custom_${System.currentTimeMillis()}",
                                text = customText.trim(),
                                author = if (customAuthor.isBlank()) "Me" else customAuthor.trim(),
                                category = "CUSTOM"
                            )
                            onAddCustomQuote(newQuote)
                            customText = ""
                            customAuthor = ""
                            showAddQuoteSheet = false
                        }
                    },
                    enabled = customText.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        disabledContainerColor = accentColor.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = "Save Quote",
                        color = if (calculateLuminance(accentColor.toArgb().toLong()) > 0.5f) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// LIST ITEM COMPONENT
// -----------------------------------------------------------------------------

@Composable
private fun QuoteListItem(
    quote: QuoteItem,
    isSelected: Boolean,
    rotationMode: String,
    accentColor: Color,
    onSelect: () -> Unit,
    onPin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val isPinnedMode = rotationMode == "STATIC"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.08f) else Color(0xFF141418))
            .border(
                1.dp,
                if (isSelected) accentColor else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onSelect() }
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = quote.category,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        letterSpacing = 0.6.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPinnedMode && isSelected) {
                        Text(
                            text = "PINNED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else if (!isPinnedMode) {
                        IconButton(
                            onClick = onPin,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pin This Quote",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.Red.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (quote.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (quote.isFavorite) Color(0xFFFF5252) else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "“${quote.text}”",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (quote.sourceBook.isNotBlank()) "— ${quote.author}, ${quote.sourceBook}" else "— ${quote.author}",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 2: WIDGET THEME
// -----------------------------------------------------------------------------

@Composable
private fun QuotesThemeTab(
    selectedBgHex: Long,
    onBgHexSelected: (Long) -> Unit,
    selectedAccentHex: Long,
    onAccentHexSelected: (Long) -> Unit,
    opacity: Float,
    onOpacityChanged: (Float) -> Unit,
    isResponsive: Boolean,
    onResponsiveChanged: (Boolean) -> Unit,
    hasModeOption: Boolean,
    onOpenColorPicker: (QuotesColorTarget) -> Unit
) {
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
                onOpenColorPicker(QuotesColorTarget.BACKGROUND)
            }
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

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
                    onClick = { onAccentHexSelected(hex) }
                )
            }

            val isCustomAccent = accentPresets.none { it == selectedAccentHex }
            RainbowCustomCircle(
                isSelected = isCustomAccent,
                activeColor = if (isCustomAccent) Color(selectedAccentHex) else null,
                onClick = { onOpenColorPicker(QuotesColorTarget.ACCENT) }
            )
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

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
        ModernOpacitySlider(value = opacity, onValueChange = onOpacityChanged)
    }

    if (hasModeOption) {
        Spacer(modifier = Modifier.height(6.dp))

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
                val isLightColor = calculateLuminance(color.toArgb().toLong()) > 0.6f
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
