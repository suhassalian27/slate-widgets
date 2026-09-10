package com.altusix.slate.widgets.productivity

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altusix.slate.core.theme.ThemePreferences

class ProductivityEditActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val initialTab = intent?.getStringExtra(BaseProductivityReceiver.EXTRA_TAB) ?: "TOP3"
        val existingConfig = ProductivityStorageManager.getConfig(this, widgetId)
        val themePrefs = ThemePreferences(this).getThemeSettings()
        val accentColor = themePrefs.accentColor

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0C0C0E),
                    surface = Color(0xFF16161B)
                )
            ) {
                var selectedTab by remember { mutableStateOf(initialTab) }
                var config by remember { mutableStateOf(existingConfig) }

                fun saveAndFinish() {
                    if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        ProductivityStorageManager.saveConfig(this@ProductivityEditActivity, widgetId, config)
                    }
                    updateAllProductivityWidgets(this@ProductivityEditActivity)
                    setResult(Activity.RESULT_OK)
                    finish()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0A0C)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        // Top App Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = "Productivity Studio",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            IconButton(
                                onClick = { saveAndFinish() },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Save",
                                    tint = Color.Black
                                )
                            }
                        }

                        // Tab Selector Row
                        val tabs = listOf(
                            "TOP3" to "Top 3 Wins",
                            "TIMER" to "Pomodoro",
                            "HABIT" to "Habit",
                            "BOOKMARKS" to "Bookmarks",
                            "CLIPBOARD" to "Clipboard",
                            "SCREENTIME" to "Screen Time",
                            "EISENHOWER" to "Eisenhower",
                            "TIMELINE" to "Timeline",
                            "GOAL" to "Milestone",
                            "RINGS" to "Rings",
                            "PIPELINE" to "Pipeline"
                        )

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tabs) { (tabKey, tabLabel) ->
                                val isSelected = selectedTab == tabKey
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) accentColor else Color(0xFF1E1E24))
                                        .clickable { selectedTab = tabKey }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = tabLabel,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.Black else Color(0xFFB0B0B8)
                                    )
                                }
                            }
                        }

                        // Tab Content Body
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            when (selectedTab) {
                                "TOP3" -> Top3Editor(config.top3Tasks, accentColor) { updated ->
                                    config = config.copy(top3Tasks = updated)
                                }
                                "TIMER" -> TimerEditor(config.focusTimer, accentColor) { updated ->
                                    config = config.copy(focusTimer = updated)
                                }
                                "HABIT" -> HabitEditor(config.habit, accentColor) { updated ->
                                    config = config.copy(habit = updated)
                                }
                                "BOOKMARKS" -> BookmarksEditor(config.bookmarks, accentColor) { updated ->
                                    config = config.copy(bookmarks = updated)
                                }
                                "CLIPBOARD" -> ClipboardEditor(config.clipboardSnippets, accentColor) { updated ->
                                    config = config.copy(clipboardSnippets = updated)
                                }
                                "SCREENTIME" -> ScreenTimeEditor(this@ProductivityEditActivity, config.screenTime, accentColor) { updated ->
                                    config = config.copy(screenTime = updated)
                                }
                                "EISENHOWER" -> EisenhowerEditor(config.eisenhowerTasks, accentColor) { updated ->
                                    config = config.copy(eisenhowerTasks = updated)
                                }
                                "TIMELINE" -> TimelineEditor(config.timeBlocks, accentColor) { updated ->
                                    config = config.copy(timeBlocks = updated)
                                }
                                "GOAL" -> GoalEditor(config.goal, accentColor) { updated ->
                                    config = config.copy(goal = updated)
                                }
                                "RINGS" -> RingsEditor(config.habitRings, accentColor) { updated ->
                                    config = config.copy(habitRings = updated)
                                }
                                "PIPELINE" -> PipelineEditor(config.pipeline, accentColor) { updated ->
                                    config = config.copy(pipeline = updated)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
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
    Text("Today's Top 3 Priority Wins", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    Text("Set your 3 most impactful outcomes. Widgets allow instantaneous checkbox tapping directly on your home screen.", fontSize = 13.sp, color = Color.Gray)

    for (i in 0 until 3) {
        val task = tasks.getOrElse(i) { Top3TaskItem("t_${i+1}", "", false, i + 1) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF16161B))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (task.isCompleted) accentColor else Color(0xFF2C2C34))
                    .clickable {
                        val updated = tasks.toMutableList()
                        while (updated.size <= i) updated.add(Top3TaskItem("t_${updated.size+1}", "", false, updated.size + 1))
                        updated[i] = task.copy(isCompleted = !task.isCompleted)
                        onUpdate(updated)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.Black, modifier = Modifier.size(18.dp))
                }
            }

            OutlinedTextField(
                value = task.title,
                onValueChange = { newTitle ->
                    val updated = tasks.toMutableList()
                    while (updated.size <= i) updated.add(Top3TaskItem("t_${updated.size+1}", "", false, updated.size + 1))
                    updated[i] = task.copy(title = newTitle)
                    onUpdate(updated)
                },
                placeholder = { Text("Priority ${i + 1} task...", color = Color.DarkGray) },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color(0xFF2C2C34)
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
    Text("Pomodoro Focus Dial Settings", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

    val durationOptions = listOf(15 to "15m", 25 to "25m", 45 to "45m", 60 to "60m")
    Text("Focus Interval Duration", fontSize = 14.sp, color = Color.Gray)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        durationOptions.forEach { (mins, label) ->
            val totalSecs = mins * 60
            val isSelected = timer.totalSeconds == totalSecs
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) accentColor else Color(0xFF1E1E24))
                    .clickable {
                        onUpdate(timer.copy(totalSeconds = totalSecs, remainingSeconds = totalSecs, isRunning = false))
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(label, color = if (isSelected) Color.Black else Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Text("Daily Sessions Target", fontSize = 14.sp, color = Color.Gray)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF16161B))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Completed ${timer.currentSession} of ${timer.maxSessions} sessions", color = Color.White)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onUpdate(timer.copy(currentSession = maxOf(0, timer.currentSession - 1))) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C34))
            ) { Text("-") }
            Button(
                onClick = { onUpdate(timer.copy(currentSession = timer.currentSession + 1)) },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) { Text("+", color = Color.Black) }
        }
    }
}

@Composable
private fun HabitEditor(
    habit: HabitItem,
    accentColor: Color,
    onUpdate: (HabitItem) -> Unit
) {
    Text("Habit Tracker & Streak Settings", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

    OutlinedTextField(
        value = habit.name,
        onValueChange = { onUpdate(habit.copy(name = it)) },
        label = { Text("Habit Name") },
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentColor,
            unfocusedBorderColor = Color(0xFF2C2C34)
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF16161B))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Current Consecutive Streak", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("${habit.streakCount} days running", color = Color.Gray, fontSize = 13.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onUpdate(habit.copy(streakCount = maxOf(0, habit.streakCount - 1))) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C34))
            ) { Text("-") }
            Button(
                onClick = { onUpdate(habit.copy(streakCount = habit.streakCount + 1)) },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) { Text("+", color = Color.Black) }
        }
    }
}

@Composable
private fun BookmarksEditor(
    bookmarks: List<BookmarkItem>,
    accentColor: Color,
    onUpdate: (List<BookmarkItem>) -> Unit
) {
    Text("Quick Launch Bookmarks (Up to 4)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

    for (i in 0 until 4) {
        val item = bookmarks.getOrElse(i) { BookmarkItem("b_${i+1}", "", "", "") }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF16161B))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = item.title,
                onValueChange = { newTitle ->
                    val updated = bookmarks.toMutableList()
                    while (updated.size <= i) updated.add(BookmarkItem("b_${updated.size+1}", "", "", ""))
                    updated[i] = item.copy(title = newTitle)
                    onUpdate(updated)
                },
                placeholder = { Text("Bookmark Label (e.g. GitHub)", color = Color.DarkGray) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color(0xFF2C2C34)
                )
            )

            OutlinedTextField(
                value = item.url,
                onValueChange = { newUrl ->
                    val host = Uri.parse(if (!newUrl.startsWith("http")) "https://$newUrl" else newUrl).host ?: newUrl
                    val updated = bookmarks.toMutableList()
                    while (updated.size <= i) updated.add(BookmarkItem("b_${updated.size+1}", "", "", ""))
                    updated[i] = item.copy(url = newUrl, domain = host)
                    onUpdate(updated)
                },
                placeholder = { Text("Destination URL (e.g. github.com)", color = Color.DarkGray) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color(0xFF8E8E93), fontSize = 13.sp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color(0xFF2C2C34)
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
    Text("Clipboard Quick-Copy Vault (2 Slots)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

    for (i in 0 until 2) {
        val item = snippets.getOrElse(i) { ClipboardSnippetItem("c_${i+1}", "", "") }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF16161B))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = item.label,
                onValueChange = { newLabel ->
                    val updated = snippets.toMutableList()
                    while (updated.size <= i) updated.add(ClipboardSnippetItem("c_${updated.size+1}", "", ""))
                    updated[i] = item.copy(label = newLabel)
                    onUpdate(updated)
                },
                placeholder = { Text("Snippet Label (e.g. Hex Code, Standup)", color = Color.DarkGray) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color(0xFF2C2C34)
                )
            )

            OutlinedTextField(
                value = item.content,
                onValueChange = { newContent ->
                    val updated = snippets.toMutableList()
                    while (updated.size <= i) updated.add(ClipboardSnippetItem("c_${updated.size+1}", "", ""))
                    updated[i] = item.copy(content = newContent)
                    onUpdate(updated)
                },
                placeholder = { Text("Text content to copy...", color = Color.DarkGray) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color(0xFF8E8E93), fontSize = 13.sp),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color(0xFF2C2C34)
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
    Text("Screen Time Balance Settings", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    Text("Slate reads daily app usage directly from Android's UsageStats subsystem.", fontSize = 13.sp, color = Color.Gray)

    Button(
        onClick = {
            try {
                activity.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            } catch (_: Exception) {}
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E24))
    ) {
        Text("Grant / Check Usage Access Permission", color = accentColor)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF16161B))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Daily Screen Time Limit", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("${screenTime.limitMinutes / 60}h ${screenTime.limitMinutes % 60}m target", color = Color.Gray, fontSize = 13.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onUpdate(screenTime.copy(limitMinutes = maxOf(60, screenTime.limitMinutes - 30))) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C34))
            ) { Text("-30m") }
            Button(
                onClick = { onUpdate(screenTime.copy(limitMinutes = screenTime.limitMinutes + 30)) },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) { Text("+30m", color = Color.Black) }
        }
    }
}

@Composable
private fun EisenhowerEditor(
    tasks: List<EisenhowerItem>,
    accentColor: Color,
    onUpdate: (List<EisenhowerItem>) -> Unit
) {
    Text("Eisenhower Priority Quadrants", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

    val quadrants = listOf(
        "Q1_DO" to "Quadrant 1: Urgent & Important (Do)",
        "Q2_SCHEDULE" to "Quadrant 2: Not Urgent & Important (Schedule)",
        "Q3_DELEGATE" to "Quadrant 3: Urgent & Not Important (Delegate)",
        "Q4_DROP" to "Quadrant 4: Not Urgent & Not Important (Drop)"
    )

    quadrants.forEachIndexed { index, (quadKey, label) ->
        val item = tasks.firstOrNull { it.quadrant == quadKey } ?: EisenhowerItem("e_$index", "", quadKey)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF16161B))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, fontSize = 13.sp, color = accentColor, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = item.title,
                onValueChange = { newTitle ->
                    val updated = tasks.filter { it.quadrant != quadKey }.toMutableList()
                    updated.add(item.copy(title = newTitle))
                    onUpdate(updated)
                },
                placeholder = { Text("Task description...", color = Color.DarkGray) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color(0xFF2C2C34)
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
    Text("Day Time-Blocks (4 Items)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

    for (i in 0 until 4) {
        val block = blocks.getOrElse(i) { TimeBlockItem("tb_$i", "", 9 + (i * 2), 0, 11 + (i * 2), 0, "FOCUS") }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF16161B))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Block ${i + 1} (${block.timeSpanText})", fontSize = 13.sp, color = accentColor, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = block.title,
                onValueChange = { newTitle ->
                    val updated = blocks.toMutableList()
                    while (updated.size <= i) updated.add(TimeBlockItem("tb_${updated.size}", "", 9, 0, 11, 0))
                    updated[i] = block.copy(title = newTitle)
                    onUpdate(updated)
                },
                placeholder = { Text("Activity / Focus session...", color = Color.DarkGray) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color(0xFF2C2C34)
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
    Text("Goal Milestone Countdown", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

    OutlinedTextField(
        value = goal.title,
        onValueChange = { onUpdate(goal.copy(title = it)) },
        label = { Text("Goal Title") },
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentColor,
            unfocusedBorderColor = Color(0xFF2C2C34)
        )
    )

    OutlinedTextField(
        value = goal.deadlineDateText,
        onValueChange = { onUpdate(goal.copy(deadlineDateText = it)) },
        label = { Text("Target Deadline (e.g. Oct 31, Q4)") },
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentColor,
            unfocusedBorderColor = Color(0xFF2C2C34)
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF16161B))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Current Progress", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("${goal.currentProgress}% completed", color = Color.Gray, fontSize = 13.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onUpdate(goal.copy(currentProgress = maxOf(0, goal.currentProgress - 5))) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C34))
            ) { Text("-5%") }
            Button(
                onClick = { onUpdate(goal.copy(currentProgress = minOf(100, goal.currentProgress + 5))) },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) { Text("+5%", color = Color.Black) }
        }
    }
}

@Composable
private fun RingsEditor(
    rings: List<HabitRingItem>,
    accentColor: Color,
    onUpdate: (List<HabitRingItem>) -> Unit
) {
    Text("Weekly Habit Rings (3 Activities)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

    for (i in 0 until 3) {
        val ring = rings.getOrElse(i) { HabitRingItem("r_$i", "Activity ${i+1}", 3, 5, "hrs", 0xFF0A84FF) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF16161B))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(ring.label, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("${ring.currentValue} / ${ring.targetValue} ${ring.unit}", color = Color.Gray, fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val updated = rings.toMutableList()
                        while (updated.size <= i) updated.add(ring)
                        updated[i] = ring.copy(currentValue = maxOf(0, ring.currentValue - 1))
                        onUpdate(updated)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C34))
                ) { Text("-") }
                Button(
                    onClick = {
                        val updated = rings.toMutableList()
                        while (updated.size <= i) updated.add(ring)
                        updated[i] = ring.copy(currentValue = ring.currentValue + 1)
                        onUpdate(updated)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) { Text("+", color = Color.Black) }
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
    Text("Task Pipeline Counts", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

    val stages = listOf(
        Triple("To Do", pipeline.todoCount) { v: Int -> onUpdate(pipeline.copy(todoCount = v)) },
        Triple("In Progress", pipeline.inProgressCount) { v: Int -> onUpdate(pipeline.copy(inProgressCount = v)) },
        Triple("Done", pipeline.doneCount) { v: Int -> onUpdate(pipeline.copy(doneCount = v)) }
    )

    stages.forEach { (label, count, setter) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF16161B))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { setter(maxOf(0, count - 1)) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C34))
                ) { Text("-") }
                Text("$count", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                Button(
                    onClick = { setter(count + 1) },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) { Text("+", color = Color.Black) }
            }
        }
    }
}
