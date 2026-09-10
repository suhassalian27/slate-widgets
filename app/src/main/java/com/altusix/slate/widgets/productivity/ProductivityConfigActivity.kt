package com.altusix.slate.widgets.productivity

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altusix.slate.core.theme.ThemePreferences
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.ui.components.ConfigTabItem
import com.altusix.slate.ui.components.CustomColorPickerDialog
import com.altusix.slate.ui.components.RainbowCustomCircle
import com.altusix.slate.ui.components.SlateConfigScaffold
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape

private enum class ProductivityColorTarget { BACKGROUND, ACCENT }

class ProductivityConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        val widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val appWidgetInfo = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)
        } else null
        val widgetClassName = appWidgetInfo?.provider?.className ?: ""

        val catalogItem = getProductivityWidgetsCatalog().find { it.receiverClass.name == widgetClassName }
        val widgetName = catalogItem?.name ?: ""
        val hasModeOption = catalogItem?.hasModeOption ?: true

        val initialTab = intent?.getStringExtra(BaseProductivityReceiver.EXTRA_TAB) ?: when {
            widgetClassName.contains("Pomodoro") -> "TIMER"
            widgetClassName.contains("HabitMatrix") -> "HABIT"
            widgetClassName.contains("Bookmarks") -> "BOOKMARKS"
            widgetClassName.contains("Clipboard") -> "CLIPBOARD"
            widgetClassName.contains("ScreenTime") -> "SCREENTIME"
            widgetClassName.contains("Eisenhower") -> "EISENHOWER"
            widgetClassName.contains("Timeline") -> "TIMELINE"
            widgetClassName.contains("Goal") -> "GOAL"
            widgetClassName.contains("HabitRings") -> "RINGS"
            widgetClassName.contains("Pipeline") -> "PIPELINE"
            else -> "TOP3"
        }

        val existingConfig = ProductivityStorageManager.getConfig(this, widgetId)
        val defaultTheme = ThemePreferences(this).getThemeSettings()

        val contentTabLabel = when (initialTab) {
            "TIMER" -> "Pomodoro"
            "HABIT" -> "Habit"
            "BOOKMARKS" -> "Bookmarks"
            "CLIPBOARD" -> "Clipboard"
            "SCREENTIME" -> "Screen Time"
            "EISENHOWER" -> "Matrix"
            "TIMELINE" -> "Timeline"
            "GOAL" -> "Milestone"
            "RINGS" -> "Rings"
            "PIPELINE" -> "Pipeline"
            else -> "Top 3"
        }

        val tabs = listOf(
            ConfigTabItem("CONTENT", contentTabLabel),
            ConfigTabItem("STYLE", "Widget Theme")
        )

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF0A0A0C), surface = Color(0xFF16161B))) {
                var selectedTabKey by remember { mutableStateOf("CONTENT") }
                var config by remember { mutableStateOf(existingConfig) }

                var selectedBgHex by remember { mutableLongStateOf(0xFF161618L) }
                var selectedAccentHex by remember { mutableLongStateOf(defaultTheme.accentHex) }
                var opacity by remember { mutableFloatStateOf(1.0f) }
                var isResponsive by remember { mutableStateOf(true) }
                var activePickerTarget by remember { mutableStateOf<ProductivityColorTarget?>(null) }

                LaunchedEffect(widgetId) {
                    val prefs = getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
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
                    if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        ProductivityStorageManager.saveConfig(this@ProductivityConfigActivity, widgetId, config)
                        saveSlateWidgetConfig(this@ProductivityConfigActivity, widgetId, currentSlateConfig, isResponsive)
                    }
                    updateAllProductivityWidgets(this@ProductivityConfigActivity)
                    val resultIntent = Intent().apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }

                SlateConfigScaffold(
                    title = "Productivity",
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
                        ProductivityWidgetLivePreview(
                            tab = initialTab,
                            config = config,
                            slateConfig = currentSlateConfig,
                            isResponsive = isResponsive
                        )
                    }
                ) {
                    if (selectedTabKey == "CONTENT") {
                        val accentColor = Color(selectedAccentHex)
                        when (initialTab) {
                            "TOP3" -> Top3Editor(config.top3Tasks, accentColor) { config = config.copy(top3Tasks = it) }
                            "TIMER" -> TimerEditor(config.focusTimer, accentColor) { config = config.copy(focusTimer = it) }
                            "HABIT" -> HabitEditor(config.habit, accentColor) { config = config.copy(habit = it) }
                            "BOOKMARKS" -> BookmarksEditor(config.bookmarks, accentColor) { config = config.copy(bookmarks = it) }
                            "CLIPBOARD" -> ClipboardEditor(config.clipboardSnippets, accentColor) { config = config.copy(clipboardSnippets = it) }
                            "SCREENTIME" -> ScreenTimeEditor(this@ProductivityConfigActivity, config.screenTime, accentColor) { config = config.copy(screenTime = it) }
                            "EISENHOWER" -> EisenhowerEditor(config.eisenhowerTasks, accentColor) { config = config.copy(eisenhowerTasks = it) }
                            "TIMELINE" -> TimelineEditor(config.timeBlocks, accentColor) { config = config.copy(timeBlocks = it) }
                            "GOAL" -> GoalEditor(config.goal, accentColor) { config = config.copy(goal = it) }
                            "RINGS" -> RingsEditor(config.habitRings, accentColor) { config = config.copy(habitRings = it) }
                            "PIPELINE" -> PipelineEditor(config.pipeline, accentColor) { config = config.copy(pipeline = it) }
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
                                    activePickerTarget = ProductivityColorTarget.BACKGROUND
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
                                    onClick = { activePickerTarget = ProductivityColorTarget.ACCENT }
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
                    val initialColor = if (target == ProductivityColorTarget.BACKGROUND) Color(selectedBgHex) else Color(selectedAccentHex)
                    CustomColorPickerDialog(
                        initialColor = initialColor,
                        title = if (target == ProductivityColorTarget.BACKGROUND) "Custom Background" else "Custom Accent",
                        onDismiss = { activePickerTarget = null },
                        onColorSelected = { color ->
                            val hex = (color.toArgb().toLong() and 0xFFFFFFFFL)
                            if (target == ProductivityColorTarget.BACKGROUND) selectedBgHex = hex else selectedAccentHex = hex
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
}

// =========================================================================
// LIVE PREVIEW CONTAINER (1:1 Exact Canvas Renderer)
// =========================================================================

@Composable
private fun ProductivityWidgetLivePreview(
    tab: String,
    config: ProductivityWidgetConfig,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean
) {
    val context = LocalContext.current

    // Geometry matched to widget aspect ratios
    val (wDp, hDp) = when (tab) {
        "PIPELINE" -> 280 to 70
        "HABIT", "TOP3", "TIMELINE", "BOOKMARKS" -> 260 to 130
        else -> 140 to 140
    }

    val previewBitmap = remember(tab, config, slateConfig, isResponsive) {
        when (tab) {
            "TIMER" -> generatePomodoroTimerBitmap(context, config.focusTimer, slateConfig, isResponsive, wDp, hDp)
            "HABIT" -> generateHabitMatrixBitmap(context, config.habit, slateConfig, isResponsive, wDp, hDp)
            "TOP3" -> generateTop3TasksBitmap(context, config.top3Tasks, slateConfig, isResponsive, wDp, hDp)
            "EISENHOWER" -> generateEisenhowerMatrixBitmap(context, config.eisenhowerTasks, slateConfig, isResponsive, wDp, hDp)
            "TIMELINE" -> generateTimeBlockTimelineBitmap(context, config.timeBlocks, slateConfig, isResponsive, wDp, hDp)
            "GOAL" -> generateGoalMilestoneBitmap(context, config.goal, slateConfig, isResponsive, wDp, hDp)
            "RINGS" -> generateHabitRingsBitmap(context, config.habitRings, slateConfig, isResponsive, wDp, hDp)
            "PIPELINE" -> generateTaskPipelineBitmap(context, config.pipeline, slateConfig, isResponsive, wDp, hDp)
            "BOOKMARKS" -> generateBookmarkListBitmap(context, config.bookmarks, slateConfig, isResponsive, wDp, hDp)
            "SCREENTIME" -> {
                val liveData = ProductivityStorageManager.resolveLiveScreenTime(context).copy(limitMinutes = config.screenTime.limitMinutes)
                generateScreenTimeBitmap(context, liveData, slateConfig, isResponsive, wDp, hDp)
            }
            "CLIPBOARD" -> generateClipboardVaultBitmap(context, config.clipboardSnippets, slateConfig, isResponsive, wDp, hDp)
            else -> generateTop3TasksBitmap(context, config.top3Tasks, slateConfig, isResponsive, wDp, hDp)
        }
    }

    Image(
        bitmap = previewBitmap.asImageBitmap(),
        contentDescription = "Widget Live Preview",
        modifier = Modifier.size(wDp.dp, hDp.dp)
    )
}

// =========================================================================
// TAB EDITORS
// =========================================================================

@Composable
private fun Top3Editor(
    tasks: List<Top3TaskItem>,
    accentColor: Color,
    onUpdate: (List<Top3TaskItem>) -> Unit
) {
    SectionTitle(title = "Today's Top 3 Priority Wins")
    Text("Set your 3 most impactful outcomes. Checkboxes can be tapped directly on your home screen.", fontSize = 12.sp, color = Color(0xFF8E8E93))

    for (i in 0 until 3) {
        val task = tasks.getOrElse(i) { Top3TaskItem("t_${i + 1}", "", false, i + 1) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF141418))
                .border(1.dp, Color(0xFF24242C), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (task.isCompleted) accentColor else Color(0xFF22222A))
                    .clickable {
                        val updated = tasks.toMutableList()
                        while (updated.size <= i) updated.add(Top3TaskItem("t_${updated.size + 1}", "", false, updated.size + 1))
                        updated[i] = task.copy(isCompleted = !task.isCompleted)
                        onUpdate(updated)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.Black, modifier = Modifier.size(16.dp))
                }
            }

            OutlinedTextField(
                value = task.title,
                onValueChange = { newTitle: String ->
                    val updated = tasks.toMutableList()
                    while (updated.size <= i) updated.add(Top3TaskItem("t_${updated.size + 1}", "", false, updated.size + 1))
                    updated[i] = task.copy(title = newTitle)
                    onUpdate(updated)
                },
                placeholder = { Text("Priority ${i + 1} task...", color = Color(0xFF8E8E93), fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
private fun TimerEditor(
    timer: FocusTimerState,
    accentColor: Color,
    onUpdate: (FocusTimerState) -> Unit
) {
    SectionTitle(title = "Focus Interval Duration")

    val durationOptions = listOf(15 to "15m", 25 to "25m", 45 to "45m", 60 to "60m")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF141416))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        durationOptions.forEach { (mins, label) ->
            val totalSecs = mins * 60
            val isSelected = timer.totalSeconds == totalSecs
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) accentColor else Color.Transparent)
                    .clickable {
                        onUpdate(timer.copy(totalSeconds = totalSecs, remainingSeconds = totalSecs, isRunning = false))
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.Black else Color(0xFF8E8E93),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }

    Spacer(Modifier.height(4.dp))
    SectionTitle(title = "Daily Sessions Target")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF141418))
            .border(1.dp, Color(0xFF24242C), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Completed Sessions", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("${timer.currentSession} of ${timer.maxSessions} target completed", color = Color(0xFF8E8E93), fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22222A))
                    .clickable { onUpdate(timer.copy(currentSession = maxOf(0, timer.currentSession - 1))) },
                contentAlignment = Alignment.Center
            ) {
                Text("-", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Text("${timer.currentSession}", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .clickable { onUpdate(timer.copy(currentSession = timer.currentSession + 1)) },
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HabitEditor(
    habit: HabitItem,
    accentColor: Color,
    onUpdate: (HabitItem) -> Unit
) {
    val streak = calculateHabitStreak(habit.history)
    val (weekDone, _) = calculateWeekCompletion(habit.history)
    val totalLogged = habit.history.values.count { it }

    SectionTitle(title = "Habit Details")

    OutlinedTextField(
        value = habit.name,
        onValueChange = { text: String -> onUpdate(habit.copy(name = text)) },
        label = { Text("Habit Name") },
        placeholder = { Text("e.g. Workout, Read, Meditate", color = Color(0xFF8E8E93)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentColor,
            unfocusedBorderColor = Color(0xFF24242C),
            focusedContainerColor = Color(0xFF141418),
            unfocusedContainerColor = Color(0xFF141418)
        )
    )

    Spacer(Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionTitle(title = "Habit Analytics")
        if (habit.history.isNotEmpty()) {
            Text(
                text = "Reset History",
                color = Color(0xFFFF453A),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    onUpdate(habit.copy(history = emptyMap(), streakCount = 0))
                }
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF141418))
            .border(1.dp, Color(0xFF24242C), RoundedCornerShape(14.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🔥 $streak", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "Active Streak", color = Color(0xFF8E8E93), fontSize = 11.sp)
        }

        Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color(0xFF24242C)))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$weekDone/7", color = accentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "This Week", color = Color(0xFF8E8E93), fontSize = 11.sp)
        }

        Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color(0xFF24242C)))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$totalLogged", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "Total Days", color = Color(0xFF8E8E93), fontSize = 11.sp)
        }
    }
}

@Composable
private fun BookmarksEditor(
    bookmarks: List<BookmarkItem>,
    accentColor: Color,
    onUpdate: (List<BookmarkItem>) -> Unit
) {
    SectionTitle(title = "Quick Launch Bookmarks (Up to 4)")

    for (i in 0 until 4) {
        val item = bookmarks.getOrElse(i) { BookmarkItem("b_${i + 1}", "", "", "") }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF141418))
                .border(1.dp, Color(0xFF24242C), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = item.title,
                onValueChange = { newTitle: String ->
                    val updated = bookmarks.toMutableList()
                    while (updated.size <= i) updated.add(BookmarkItem("b_${updated.size + 1}", "", "", ""))
                    updated[i] = item.copy(title = newTitle)
                    onUpdate(updated)
                },
                placeholder = { Text("Slot ${i + 1} Label (e.g. GitHub)", color = Color(0xFF8E8E93), fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color(0xFF24242C),
                    focusedContainerColor = Color(0xFF0C0C0E),
                    unfocusedContainerColor = Color(0xFF0C0C0E)
                )
            )

            OutlinedTextField(
                value = item.url,
                onValueChange = { newUrl: String ->
                    val host = Uri.parse(if (!newUrl.startsWith("http")) "https://$newUrl" else newUrl).host ?: newUrl
                    val updated = bookmarks.toMutableList()
                    while (updated.size <= i) updated.add(BookmarkItem("b_${updated.size + 1}", "", "", ""))
                    updated[i] = item.copy(url = newUrl, domain = host)
                    onUpdate(updated)
                },
                placeholder = { Text("URL (e.g. github.com)", color = Color(0xFF8E8E93), fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color(0xFF8E8E93), fontSize = 13.sp),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color(0xFF24242C),
                    focusedContainerColor = Color(0xFF0C0C0E),
                    unfocusedContainerColor = Color(0xFF0C0C0E)
                )
            )
        }
    }
}

@Composable
private fun ClipboardEditor(
    snippets: List<ClipboardSnippetItem>,
    accentColor: Color,
    onUpdate: (List<ClipboardSnippetItem>) -> Unit
) {
    SectionTitle(title = "Clipboard Vault (2 Slots)")

    for (i in 0 until 2) {
        val item = snippets.getOrElse(i) { ClipboardSnippetItem("c_${i + 1}", "", "") }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF141418))
                .border(1.dp, Color(0xFF24242C), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = item.label,
                onValueChange = { newLabel: String ->
                    val updated = snippets.toMutableList()
                    while (updated.size <= i) updated.add(ClipboardSnippetItem("c_${updated.size + 1}", "", ""))
                    updated[i] = item.copy(label = newLabel)
                    onUpdate(updated)
                },
                placeholder = { Text("Slot ${i + 1} Label (e.g. Address, IBAN)", color = Color(0xFF8E8E93), fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color(0xFF24242C),
                    focusedContainerColor = Color(0xFF0C0C0E),
                    unfocusedContainerColor = Color(0xFF0C0C0E)
                )
            )

            OutlinedTextField(
                value = item.content,
                onValueChange = { newContent: String ->
                    val updated = snippets.toMutableList()
                    while (updated.size <= i) updated.add(ClipboardSnippetItem("c_${updated.size + 1}", "", ""))
                    updated[i] = item.copy(content = newContent)
                    onUpdate(updated)
                },
                placeholder = { Text("Text content to copy...", color = Color(0xFF8E8E93), fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color(0xFF8E8E93), fontSize = 13.sp),
                maxLines = 3,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color(0xFF24242C),
                    focusedContainerColor = Color(0xFF0C0C0E),
                    unfocusedContainerColor = Color(0xFF0C0C0E)
                )
            )
        }
    }
}

@Composable
private fun ScreenTimeEditor(
    activity: ComponentActivity,
    screenTime: ScreenTimeData,
    accentColor: Color,
    onUpdate: (ScreenTimeData) -> Unit
) {
    SectionTitle(title = "Usage Access")
    Text("Slate reads daily app usage directly from Android's UsageStats subsystem.", fontSize = 12.sp, color = Color(0xFF8E8E93))

    Button(
        onClick = {
            try { activity.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) } catch (_: Exception) {}
        },
        modifier = Modifier.fillMaxWidth().height(42.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E24))
    ) {
        Text("Grant / Check Usage Access", color = accentColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }

    Spacer(Modifier.height(4.dp))
    SectionTitle(title = "Daily Screen Time Target")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF141418))
            .border(1.dp, Color(0xFF24242C), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Usage Limit", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("${screenTime.limitMinutes / 60}h ${screenTime.limitMinutes % 60}m daily goal", color = Color(0xFF8E8E93), fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { onUpdate(screenTime.copy(limitMinutes = maxOf(60, screenTime.limitMinutes - 30))) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22222A)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) { Text("-30m", color = Color.White, fontSize = 12.sp) }
            Button(
                onClick = { onUpdate(screenTime.copy(limitMinutes = screenTime.limitMinutes + 30)) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) { Text("+30m", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun EisenhowerEditor(
    tasks: List<EisenhowerItem>,
    accentColor: Color,
    onUpdate: (List<EisenhowerItem>) -> Unit
) {
    SectionTitle(title = "Eisenhower Quadrants")

    val quadrants = listOf(
        "Q1_DO" to "Q1: Urgent & Important (Do)",
        "Q2_SCHEDULE" to "Q2: Not Urgent & Important (Schedule)",
        "Q3_DELEGATE" to "Q3: Urgent & Not Important (Delegate)",
        "Q4_DROP" to "Q4: Not Urgent & Not Important (Drop)"
    )

    quadrants.forEachIndexed { index, (quadKey, label) ->
        val item = tasks.firstOrNull { it.quadrant == quadKey } ?: EisenhowerItem("e_$index", "", quadKey)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF141418))
                .border(1.dp, Color(0xFF24242C), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, fontSize = 12.sp, color = accentColor, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = item.title,
                onValueChange = { newTitle: String ->
                    val updated = tasks.filter { it.quadrant != quadKey }.toMutableList()
                    updated.add(item.copy(title = newTitle))
                    onUpdate(updated)
                },
                placeholder = { Text("Task description...", color = Color(0xFF8E8E93), fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color(0xFF24242C),
                    focusedContainerColor = Color(0xFF0C0C0E),
                    unfocusedContainerColor = Color(0xFF0C0C0E)
                )
            )
        }
    }
}

@Composable
private fun TimelineEditor(
    blocks: List<TimeBlockItem>,
    accentColor: Color,
    onUpdate: (List<TimeBlockItem>) -> Unit
) {
    SectionTitle(title = "Day Time-Blocks (4 Items)")

    for (i in 0 until 4) {
        val block = blocks.getOrElse(i) { TimeBlockItem("tb_$i", "", 9 + (i * 2), 0, 11 + (i * 2), 0, "FOCUS") }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF141418))
                .border(1.dp, Color(0xFF24242C), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Block ${i + 1} (${block.timeSpanText})", fontSize = 12.sp, color = accentColor, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = block.title,
                onValueChange = { newTitle: String ->
                    val updated = blocks.toMutableList()
                    while (updated.size <= i) updated.add(TimeBlockItem("tb_${updated.size}", "", 9, 0, 11, 0))
                    updated[i] = block.copy(title = newTitle)
                    onUpdate(updated)
                },
                placeholder = { Text("Activity / Focus session...", color = Color(0xFF8E8E93), fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color(0xFF24242C),
                    focusedContainerColor = Color(0xFF0C0C0E),
                    unfocusedContainerColor = Color(0xFF0C0C0E)
                )
            )
        }
    }
}

@Composable
private fun GoalEditor(
    goal: GoalMilestoneItem,
    accentColor: Color,
    onUpdate: (GoalMilestoneItem) -> Unit
) {
    SectionTitle(title = "Milestone Details")

    OutlinedTextField(
        value = goal.title,
        onValueChange = { text: String -> onUpdate(goal.copy(title = text)) },
        label = { Text("Goal Title") },
        placeholder = { Text("e.g. Launch Beta, Marathon", color = Color(0xFF8E8E93)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentColor,
            unfocusedBorderColor = Color(0xFF24242C),
            focusedContainerColor = Color(0xFF141418),
            unfocusedContainerColor = Color(0xFF141418)
        )
    )

    OutlinedTextField(
        value = goal.deadlineDateText,
        onValueChange = { text: String -> onUpdate(goal.copy(deadlineDateText = text)) },
        label = { Text("Target Deadline") },
        placeholder = { Text("e.g. Oct 31, Q4", color = Color(0xFF8E8E93)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentColor,
            unfocusedBorderColor = Color(0xFF24242C),
            focusedContainerColor = Color(0xFF141418),
            unfocusedContainerColor = Color(0xFF141418)
        )
    )

    Spacer(Modifier.height(4.dp))
    SectionTitle(title = "Completion Percentage")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF141418))
            .border(1.dp, Color(0xFF24242C), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Progress", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("${goal.currentProgress}% accomplished", color = Color(0xFF8E8E93), fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22222A))
                    .clickable { onUpdate(goal.copy(currentProgress = maxOf(0, goal.currentProgress - 5))) },
                contentAlignment = Alignment.Center
            ) {
                Text("-5", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Text("${goal.currentProgress}%", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .clickable { onUpdate(goal.copy(currentProgress = minOf(100, goal.currentProgress + 5))) },
                contentAlignment = Alignment.Center
            ) {
                Text("+5", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RingsEditor(
    rings: List<HabitRingItem>,
    accentColor: Color,
    onUpdate: (List<HabitRingItem>) -> Unit
) {
    SectionTitle(title = "Activity Rings (3 Metrics)")

    for (i in 0 until 3) {
        val ring = rings.getOrElse(i) { HabitRingItem("r_$i", "Activity ${i + 1}", 3, 5, "hrs", 0xFF0A84FF) }
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
                Text(ring.label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("${ring.currentValue} / ${ring.targetValue} ${ring.unit}", color = Color(0xFF8E8E93), fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22222A))
                        .clickable {
                            val updated = rings.toMutableList()
                            while (updated.size <= i) updated.add(ring)
                            updated[i] = ring.copy(currentValue = maxOf(0, ring.currentValue - 1))
                            onUpdate(updated)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("-", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Text("${ring.currentValue}", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                        .clickable {
                            val updated = rings.toMutableList()
                            while (updated.size <= i) updated.add(ring)
                            updated[i] = ring.copy(currentValue = ring.currentValue + 1)
                            onUpdate(updated)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PipelineEditor(
    pipeline: TaskPipelineData,
    accentColor: Color,
    onUpdate: (TaskPipelineData) -> Unit
) {
    SectionTitle(title = "Task Pipeline Stages")

    val stages = listOf(
        Triple("To Do", pipeline.todoCount) { v: Int -> onUpdate(pipeline.copy(todoCount = v)) },
        Triple("In Progress", pipeline.inProgressCount) { v: Int -> onUpdate(pipeline.copy(inProgressCount = v)) },
        Triple("Done", pipeline.doneCount) { v: Int -> onUpdate(pipeline.copy(doneCount = v)) }
    )

    stages.forEach { (label, count, setter) ->
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
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22222A))
                        .clickable { setter(maxOf(0, count - 1)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("-", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Text("$count", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                        .clickable { setter(count + 1) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =========================================================================
// HELPER COMPONENTS
// =========================================================================

@Composable
private fun SectionTitle(title: String) {
    Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
                .size(8.dp)
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
                val isLightColor = remember(color) {
                    val argb = color.toArgb()
                    val r = ((argb shr 16) and 0xFF) / 255f
                    val g = ((argb shr 8) and 0xFF) / 255f
                    val b = (argb and 0xFF) / 255f
                    (0.2126f * r + 0.7152f * g + 0.0722f * b) > 0.6f
                }
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
