package com.altusix.slate.widgets.quotes

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altusix.slate.core.theme.ThemePreferences
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
                var selectedTabKey by remember { mutableStateOf("LIBRARY") }
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
                }

                fun refreshQuotes() {
                    allQuotes = QuotesStorageManager.getAllQuotes(this)
                }

                val tabs = listOf(
                    ConfigTabItem("LIBRARY", "Quotes"),
                    ConfigTabItem("CUSTOM", "My Quotes"),
                    ConfigTabItem("STYLE", "Widget Theme")
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
                        // Persist active quote for this widget
                        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            QuotesStorageManager.setQuoteForWidget(this@QuotesConfigActivity, widgetId, activeQuote.id)
                            val isLight = calculateLuminance(selectedBgHex) > 0.5f

                            getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE).edit()
                                .putString("widget_${widgetId}_theme_mode", if (isLight) "LIGHT" else "DARK")
                                .putLong("widget_${widgetId}_bg_color", selectedBgHex)
                                .putLong("widget_${widgetId}_accent_color", selectedAccentHex)
                                .putFloat("widget_${widgetId}_opacity", opacity)
                                .putBoolean("widget_${widgetId}_is_responsive", isResponsive)
                                .apply()
                        }
                        updateAllQuotesWidgets(this@QuotesConfigActivity)
                        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                        finish()
                    },
                    scrollable = selectedTabKey == "STYLE",
                    previewHeight = 175.dp,
                    previewContent = {
                        QuoteLivePreviewCard(
                            quote = activeQuote,
                            accentColor = Color(selectedAccentHex),
                            bgColor = Color(selectedBgHex),
                            opacity = opacity
                        )
                    }
                ) {
                    when (selectedTabKey) {
                        "LIBRARY" -> {
                            QuotesLibraryTab(
                                allQuotes = allQuotes,
                                activeQuoteId = activeQuote.id,
                                accentColor = Color(selectedAccentHex),
                                onQuoteSelected = { selected ->
                                    activeQuote = selected
                                    if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                                        QuotesStorageManager.setQuoteForWidget(this@QuotesConfigActivity, widgetId, selected.id)
                                    }
                                },
                                onToggleFavorite = { quoteId ->
                                    QuotesStorageManager.toggleFavoriteById(this@QuotesConfigActivity, quoteId)
                                    refreshQuotes()
                                }
                            )
                        }
                        "CUSTOM" -> {
                            QuotesCustomTab(
                                allQuotes = allQuotes.filter { it.category == "CUSTOM" },
                                activeQuoteId = activeQuote.id,
                                accentColor = Color(selectedAccentHex),
                                onAddQuote = { newQuote ->
                                    QuotesStorageManager.saveCustomQuote(this@QuotesConfigActivity, newQuote)
                                    refreshQuotes()
                                    activeQuote = newQuote
                                },
                                onDeleteQuote = { quoteId ->
                                    QuotesStorageManager.deleteCustomQuote(this@QuotesConfigActivity, quoteId)
                                    refreshQuotes()
                                    if (activeQuote.id == quoteId) {
                                        activeQuote = QuotesStorageManager.CURATED_QUOTES.first()
                                    }
                                },
                                onQuoteSelected = { selected ->
                                    activeQuote = selected
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

                // Custom Color Picker Dialog (for Background and Accent)
                if (activePickerTarget != null) {
                    val initialColor = if (activePickerTarget == QuotesColorTarget.BACKGROUND) {
                        Color(selectedBgHex)
                    } else {
                        Color(selectedAccentHex)
                    }
                    val dialogTitle = if (activePickerTarget == QuotesColorTarget.BACKGROUND) {
                        "Background Color"
                    } else {
                        "Accent Color"
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
// LIVE PREVIEW CARD
// -----------------------------------------------------------------------------

@Composable
private fun QuoteLivePreviewCard(
    quote: QuoteItem,
    accentColor: Color,
    bgColor: Color,
    opacity: Float
) {
    val isLight = calculateLuminance(bgColor.toArgb().toLong()) > 0.5f
    val textColor = if (isLight) Color(0xFF111111) else Color.White
    val secondaryColor = if (isLight) Color(0xFF555555) else Color(0xFF8E8E93)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(155.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(bgColor.copy(alpha = opacity))
            .border(1.dp, if (isLight) Color.Black.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Accent modern quote mark
                Text(
                    text = "“",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    lineHeight = 24.sp
                )

                Text(
                    text = quote.category.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = secondaryColor.copy(alpha = 0.7f),
                    letterSpacing = 1.2.sp
                )
            }

            Text(
                text = quote.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 3,
                lineHeight = 21.sp
            )

            Text(
                text = if (quote.sourceBook.isNotBlank()) "— ${quote.author}, ${quote.sourceBook}" else "— ${quote.author}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = secondaryColor
            )
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 1: CURATED QUOTES LIBRARY
// -----------------------------------------------------------------------------

@Composable
private fun QuotesLibraryTab(
    allQuotes: List<QuoteItem>,
    activeQuoteId: String,
    accentColor: Color,
    onQuoteSelected: (QuoteItem) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    val categories = listOf("ALL", "STOICISM", "MINDFULNESS", "LITERATURE", "PRODUCTIVITY", "SCIENCE", "AFFIRMATION", "FAVORITES")
    var selectedCategory by remember { mutableStateOf("ALL") }

    val filteredQuotes = remember(selectedCategory, allQuotes) {
        when (selectedCategory) {
            "ALL" -> allQuotes.filter { it.category != "CUSTOM" }
            "FAVORITES" -> allQuotes.filter { it.isFavorite }
            else -> allQuotes.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Category Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) accentColor else Color(0xFF1E1E24))
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = cat,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Quotes List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredQuotes, key = { it.id }) { quote ->
                val isPinned = quote.id == activeQuoteId
                QuoteListItem(
                    quote = quote,
                    isPinned = isPinned,
                    accentColor = accentColor,
                    onSelect = { onQuoteSelected(quote) },
                    onToggleFavorite = { onToggleFavorite(quote.id) }
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 2: MY CUSTOM QUOTES (INLINE CARD - NO POPUP MODAL)
// -----------------------------------------------------------------------------

@Composable
private fun QuotesCustomTab(
    allQuotes: List<QuoteItem>,
    activeQuoteId: String,
    accentColor: Color,
    onAddQuote: (QuoteItem) -> Unit,
    onDeleteQuote: (String) -> Unit,
    onQuoteSelected: (QuoteItem) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var inputAuthor by remember { mutableStateOf("") }
    var inputCategory by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // INLINE ADD QUOTE CARD (Matches Productivity Bookmark/Clipboard editor)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF141418))
                .border(1.dp, Color(0xFF24242C), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Add Custom Quote", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Write your quote or life mantra...", color = Color(0xFF8E8E93), fontSize = 12.5.sp) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color(0xFF24242C),
                    focusedContainerColor = Color(0xFF0C0C0E),
                    unfocusedContainerColor = Color(0xFF0C0C0E)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputAuthor,
                    onValueChange = { inputAuthor = it },
                    placeholder = { Text("Author (e.g. Me)", color = Color(0xFF8E8E93), fontSize = 12.5.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color(0xFF24242C),
                        focusedContainerColor = Color(0xFF0C0C0E),
                        unfocusedContainerColor = Color(0xFF0C0C0E)
                    )
                )

                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val newQuote = QuoteItem(
                                id = "custom_${System.currentTimeMillis()}",
                                text = inputText.trim(),
                                author = if (inputAuthor.isBlank()) "Me" else inputAuthor.trim(),
                                category = if (inputCategory.isBlank()) "CUSTOM" else inputCategory.trim().uppercase()
                            )
                            onAddQuote(newQuote)
                            inputText = ""
                            inputAuthor = ""
                            inputCategory = ""
                        }
                    },
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (allQuotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No custom quotes yet.\nUse the card above to add personal wisdom.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(allQuotes, key = { it.id }) { quote ->
                    val isPinned = quote.id == activeQuoteId
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isPinned) accentColor.copy(alpha = 0.08f) else Color(0xFF141418))
                            .border(1.dp, if (isPinned) accentColor else Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .clickable { onQuoteSelected(quote) }
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CUSTOM",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    letterSpacing = 0.8.sp
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isPinned) {
                                        Text("ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    IconButton(
                                        onClick = { onDeleteQuote(quote.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "“${quote.text}”", fontSize = 13.5.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "— ${quote.author}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 3: THEME & STYLE (Matches ProductivityConfigActivity exact design)
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Background Presets + Custom
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

        // 2. Accent Colors
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

        // 3. Surface Translucency
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

        // 4. Fixed Aspect vs Responsive (if supported)
        if (hasModeOption) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF141418))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Responsive Scaling", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Auto-fit canvas to widget dimensions", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                }
                Switch(
                    checked = isResponsive,
                    onCheckedChange = onResponsiveChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color(selectedAccentHex)
                    )
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// HELPER COMPONENTS (IDENTICAL TO PRODUCTIVITY CONFIG)
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

// -----------------------------------------------------------------------------
// LIST ITEM COMPONENT
// -----------------------------------------------------------------------------

@Composable
private fun QuoteListItem(
    quote: QuoteItem,
    isPinned: Boolean,
    accentColor: Color,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPinned) accentColor.copy(alpha = 0.08f) else Color(0xFF141418))
            .border(
                1.dp,
                if (isPinned) accentColor else Color.White.copy(alpha = 0.08f),
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
                    if (isPinned) {
                        Text(
                            text = "PINNED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
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
