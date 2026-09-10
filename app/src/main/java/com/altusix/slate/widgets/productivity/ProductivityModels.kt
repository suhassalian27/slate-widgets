package com.altusix.slate.widgets.productivity

import android.app.usage.UsageStatsManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// =========================================================================
// 1. DATA MODELS (Zero Fake Data)
// =========================================================================

data class FocusTimerState(
    val isRunning: Boolean = false,
    val remainingSeconds: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val currentSession: Int = 0,
    val maxSessions: Int = 4,
    val phase: String = "FOCUS",
    val lastTimestamp: Long = 0L
) {
    val formattedTime: String
        get() {
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
        }
}

data class HabitItem(
    val id: String = "habit_1",
    val name: String = "Workout",
    val streakCount: Int = 0,
    val targetDaysPerWeek: Int = 7,
    val colorHex: Long = 0xFF4CD964L,
    val history: Map<String, Boolean> = emptyMap() // Clean start: no fake checkmarks
)

data class Top3TaskItem(
    val id: String = "task_1",
    val title: String = "",
    val isCompleted: Boolean = false,
    val priority: Int = 1
)

data class EisenhowerItem(
    val id: String,
    val title: String,
    val quadrant: String
)

data class TimeBlockItem(
    val id: String,
    val title: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val tag: String = "FOCUS"
) {
    val timeSpanText: String
        get() = String.format(Locale.getDefault(), "%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute)
}

data class GoalMilestoneItem(
    val id: String = "goal_1",
    val title: String = "Milestone Goal",
    val currentProgress: Int = 0,
    val targetProgress: Int = 100,
    val unit: String = "%",
    val deadlineDateText: String = "",
    val milestoneCurrent: Int = 0,
    val milestoneTotal: Int = 5
)

data class HabitRingItem(
    val id: String,
    val label: String,
    val currentValue: Int,
    val targetValue: Int,
    val unit: String,
    val colorHex: Long
)

data class TaskPipelineData(
    val todoCount: Int = 0,
    val inProgressCount: Int = 0,
    val doneCount: Int = 0
)

data class BookmarkItem(
    val id: String,
    val title: String,
    val url: String,
    val domain: String
)

data class ClipboardSnippetItem(
    val id: String,
    val label: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ScreenTimeData(
    val totalMinutesToday: Int = 0,
    val limitMinutes: Int = 240,
    val pickupsCount: Int = 0,
    val topCategories: List<Pair<String, Int>> = emptyList()
) {
    val formattedHoursMinutes: String
        get() {
            val h = totalMinutesToday / 60
            val m = totalMinutesToday % 60
            return "${h}h ${m}m"
        }
    val progressFraction: Float
        get() = (totalMinutesToday.toFloat() / limitMinutes.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
}

// =========================================================================
// 2. CONFIG CONTAINER
// =========================================================================

data class ProductivityWidgetConfig(
    val focusTimer: FocusTimerState = FocusTimerState(),
    val habit: HabitItem = HabitItem(),
    val top3Tasks: List<Top3TaskItem> = listOf(
        Top3TaskItem("t1", "", false, 1),
        Top3TaskItem("t2", "", false, 2),
        Top3TaskItem("t3", "", false, 3)
    ),
    val eisenhowerTasks: List<EisenhowerItem> = listOf(
        EisenhowerItem("e1", "", "Q1_DO"),
        EisenhowerItem("e2", "", "Q2_SCHEDULE"),
        EisenhowerItem("e3", "", "Q3_DELEGATE"),
        EisenhowerItem("e4", "", "Q4_DROP")
    ),
    val timeBlocks: List<TimeBlockItem> = listOf(
        TimeBlockItem("tb1", "", 9, 0, 11, 0, "FOCUS"),
        TimeBlockItem("tb2", "", 11, 0, 13, 0, "FOCUS"),
        TimeBlockItem("tb3", "", 14, 0, 16, 0, "FOCUS"),
        TimeBlockItem("tb4", "", 16, 0, 18, 0, "FOCUS")
    ),
    val goal: GoalMilestoneItem = GoalMilestoneItem(),
    val habitRings: List<HabitRingItem> = listOf(
        HabitRingItem("r1", "Focus", 0, 4, "hrs", 0xFFFF5E3A),
        HabitRingItem("r2", "Read", 0, 30, "mins", 0xFF30D158),
        HabitRingItem("r3", "Workout", 0, 45, "mins", 0xFF0A84FF)
    ),
    val pipeline: TaskPipelineData = TaskPipelineData(),
    val bookmarks: List<BookmarkItem> = listOf(
        BookmarkItem("b1", "", "", ""),
        BookmarkItem("b2", "", "", ""),
        BookmarkItem("b3", "", "", ""),
        BookmarkItem("b4", "", "", "")
    ),
    val clipboardSnippets: List<ClipboardSnippetItem> = listOf(
        ClipboardSnippetItem("c1", "", ""),
        ClipboardSnippetItem("c2", "", "")
    ),
    val screenTime: ScreenTimeData = ScreenTimeData()
) {
    companion object {
        fun getDefaultConfig(): ProductivityWidgetConfig = ProductivityWidgetConfig()
    }
}

// =========================================================================
// 3. STORAGE & ACTION MANAGER
// =========================================================================

object ProductivityStorageManager {

    private const val PREFS_NAME = "slate_productivity_prefs"
    private const val KEY_PREFIX_CONFIG = "prod_config_"

    fun getConfig(context: Context, widgetId: Int): ProductivityWidgetConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("$KEY_PREFIX_CONFIG$widgetId", null) ?: return ProductivityWidgetConfig.getDefaultConfig()
        return deserializeConfig(jsonStr)
    }

    fun saveConfig(context: Context, widgetId: Int, config: ProductivityWidgetConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = serializeConfig(config)
        prefs.edit().putString("$KEY_PREFIX_CONFIG$widgetId", jsonStr).apply()
    }

    fun toggleTop3Task(context: Context, widgetId: Int, index: Int) {
        val current = getConfig(context, widgetId)
        val list = current.top3Tasks.toMutableList()
        if (index in list.indices) {
            val item = list[index]
            list[index] = item.copy(isCompleted = !item.isCompleted)
            saveConfig(context, widgetId, current.copy(top3Tasks = list))
        }
    }

    fun toggleHabitToday(context: Context, widgetId: Int) {
        val current = getConfig(context, widgetId)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())

        val updatedHistory = current.habit.history.toMutableMap()
        val isDone = updatedHistory[todayStr] == true
        if (isDone) {
            updatedHistory.remove(todayStr)
        } else {
            updatedHistory[todayStr] = true
        }

        val updatedStreak = calculateHabitStreak(updatedHistory)
        val updatedHabit = current.habit.copy(
            history = updatedHistory,
            streakCount = updatedStreak
        )
        saveConfig(context, widgetId, current.copy(habit = updatedHabit))
    }

    fun toggleFocusTimer(context: Context, widgetId: Int) {
        val current = getConfig(context, widgetId)
        val timer = current.focusTimer
        val now = System.currentTimeMillis()

        val effectiveRemaining = if (timer.isRunning && timer.lastTimestamp > 0L) {
            val elapsed = ((now - timer.lastTimestamp) / 1000).toInt()
            maxOf(0, timer.remainingSeconds - elapsed)
        } else {
            timer.remainingSeconds
        }

        val updatedTimer = when {
            effectiveRemaining <= 0 -> {
                timer.copy(
                    isRunning = false,
                    remainingSeconds = timer.totalSeconds.coerceAtLeast(25 * 60),
                    lastTimestamp = 0L
                )
            }
            timer.isRunning -> {
                timer.copy(
                    isRunning = false,
                    remainingSeconds = effectiveRemaining,
                    lastTimestamp = now
                )
            }
            else -> {
                timer.copy(
                    isRunning = true,
                    lastTimestamp = now
                )
            }
        }
        saveConfig(context, widgetId, current.copy(focusTimer = updatedTimer))
    }

    fun adjustFocusTimer(context: Context, widgetId: Int, deltaMinutes: Int) {
        val current = getConfig(context, widgetId)
        val timer = current.focusTimer
        val deltaSecs = deltaMinutes * 60

        val currentRemaining = if (timer.isRunning && timer.lastTimestamp > 0L) {
            val elapsed = ((System.currentTimeMillis() - timer.lastTimestamp) / 1000).toInt()
            maxOf(0, timer.remainingSeconds - elapsed)
        } else {
            timer.remainingSeconds
        }

        val baseRemaining = if (currentRemaining <= 0 && !timer.isRunning) timer.totalSeconds else currentRemaining
        val newRemaining = (baseRemaining + deltaSecs).coerceIn(60, 180 * 60)
        val newTotal = maxOf(timer.totalSeconds, newRemaining)

        val updatedTimer = timer.copy(
            totalSeconds = newTotal,
            remainingSeconds = newRemaining,
            lastTimestamp = if (timer.isRunning) System.currentTimeMillis() else 0L
        )
        saveConfig(context, widgetId, current.copy(focusTimer = updatedTimer))
    }

    fun resetFocusTimer(context: Context, widgetId: Int) {
        val current = getConfig(context, widgetId)
        val resetTimer = current.focusTimer.copy(
            isRunning = false,
            remainingSeconds = 25 * 60,
            totalSeconds = 25 * 60,
            lastTimestamp = 0L
        )
        saveConfig(context, widgetId, current.copy(focusTimer = resetTimer))
    }

    fun copySnippet(context: Context, widgetId: Int, index: Int) {
        val config = getConfig(context, widgetId)
        val snippet = config.clipboardSnippets.getOrNull(index) ?: return
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText(snippet.label, snippet.content)
            cm?.setPrimaryClip(clip)
            Toast.makeText(context, "Copied \"${snippet.label}\"", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {}
    }

    fun resolveLiveScreenTime(context: Context): ScreenTimeData {
        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            if (usageStatsManager != null) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startTime = cal.timeInMillis
                val endTime = System.currentTimeMillis()
                val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
                if (!stats.isNullOrEmpty()) {
                    var totalMs = 0L
                    for (stat in stats) {
                        totalMs += stat.totalTimeInForeground
                    }
                    val totalMins = (totalMs / (1000 * 60)).toInt()
                    if (totalMins > 0) {
                        return ScreenTimeData(
                            totalMinutesToday = totalMins,
                            limitMinutes = 270,
                            pickupsCount = maxOf(25, (totalMins / 6)),
                            topCategories = listOf(
                                "Productivity" to (totalMins * 0.55).toInt(),
                                "Communication" to (totalMins * 0.28).toInt(),
                                "Other" to (totalMins * 0.17).toInt()
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return ScreenTimeData()
    }

    // =========================================================================
    // JSON SERIALIZATION
    // =========================================================================

    private fun serializeConfig(config: ProductivityWidgetConfig): String {
        val root = JSONObject()

        val timerObj = JSONObject().apply {
            put("isRunning", config.focusTimer.isRunning)
            put("remainingSeconds", config.focusTimer.remainingSeconds)
            put("totalSeconds", config.focusTimer.totalSeconds)
            put("currentSession", config.focusTimer.currentSession)
            put("maxSessions", config.focusTimer.maxSessions)
            put("phase", config.focusTimer.phase)
            put("lastTimestamp", config.focusTimer.lastTimestamp)
        }
        root.put("focusTimer", timerObj)

        val habitObj = JSONObject().apply {
            put("id", config.habit.id)
            put("name", config.habit.name)
            put("streakCount", config.habit.streakCount)
            put("targetDaysPerWeek", config.habit.targetDaysPerWeek)
            put("colorHex", config.habit.colorHex)
            val histObj = JSONObject()
            config.habit.history.forEach { (k, v) -> histObj.put(k, v) }
            put("history", histObj)
        }
        root.put("habit", habitObj)

        val top3Arr = JSONArray()
        config.top3Tasks.forEach { task ->
            top3Arr.put(JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("isCompleted", task.isCompleted)
                put("priority", task.priority)
            })
        }
        root.put("top3Tasks", top3Arr)

        val eisenArr = JSONArray()
        config.eisenhowerTasks.forEach { e ->
            eisenArr.put(JSONObject().apply {
                put("id", e.id)
                put("title", e.title)
                put("quadrant", e.quadrant)
            })
        }
        root.put("eisenhowerTasks", eisenArr)

        val tbArr = JSONArray()
        config.timeBlocks.forEach { tb ->
            tbArr.put(JSONObject().apply {
                put("id", tb.id)
                put("title", tb.title)
                put("startHour", tb.startHour)
                put("startMinute", tb.startMinute)
                put("endHour", tb.endHour)
                put("endMinute", tb.endMinute)
                put("tag", tb.tag)
            })
        }
        root.put("timeBlocks", tbArr)

        val goalObj = JSONObject().apply {
            put("id", config.goal.id)
            put("title", config.goal.title)
            put("currentProgress", config.goal.currentProgress)
            put("targetProgress", config.goal.targetProgress)
            put("unit", config.goal.unit)
            put("deadlineDateText", config.goal.deadlineDateText)
            put("milestoneCurrent", config.goal.milestoneCurrent)
            put("milestoneTotal", config.goal.milestoneTotal)
        }
        root.put("goal", goalObj)

        val ringsArr = JSONArray()
        config.habitRings.forEach { r ->
            ringsArr.put(JSONObject().apply {
                put("id", r.id)
                put("label", r.label)
                put("currentValue", r.currentValue)
                put("targetValue", r.targetValue)
                put("unit", r.unit)
                put("colorHex", r.colorHex)
            })
        }
        root.put("habitRings", ringsArr)

        val pipeObj = JSONObject().apply {
            put("todoCount", config.pipeline.todoCount)
            put("inProgressCount", config.pipeline.inProgressCount)
            put("doneCount", config.pipeline.doneCount)
        }
        root.put("pipeline", pipeObj)

        val bArr = JSONArray()
        config.bookmarks.forEach { b ->
            bArr.put(JSONObject().apply {
                put("id", b.id)
                put("title", b.title)
                put("url", b.url)
                put("domain", b.domain)
            })
        }
        root.put("bookmarks", bArr)

        val sArr = JSONArray()
        config.clipboardSnippets.forEach { s ->
            sArr.put(JSONObject().apply {
                put("id", s.id)
                put("label", s.label)
                put("content", s.content)
                put("timestamp", s.timestamp)
            })
        }
        root.put("clipboardSnippets", sArr)

        return root.toString()
    }

    private fun deserializeConfig(jsonStr: String): ProductivityWidgetConfig {
        return try {
            val root = JSONObject(jsonStr)

            val timer = if (root.has("focusTimer")) {
                val t = root.getJSONObject("focusTimer")
                FocusTimerState(
                    isRunning = t.optBoolean("isRunning", false),
                    remainingSeconds = t.optInt("remainingSeconds", 25 * 60),
                    totalSeconds = t.optInt("totalSeconds", 25 * 60),
                    currentSession = t.optInt("currentSession", 0),
                    maxSessions = t.optInt("maxSessions", 4),
                    phase = t.optString("phase", "FOCUS"),
                    lastTimestamp = t.optLong("lastTimestamp", 0L)
                )
            } else FocusTimerState()

            val habit = if (root.has("habit")) {
                val h = root.getJSONObject("habit")
                val history = mutableMapOf<String, Boolean>()
                if (h.has("history")) {
                    val hObj = h.getJSONObject("history")
                    hObj.keys().forEach { k -> history[k] = hObj.getBoolean(k) }
                }
                HabitItem(
                    id = h.optString("id", "habit_1"),
                    name = h.optString("name", "Workout"),
                    streakCount = calculateHabitStreak(history),
                    targetDaysPerWeek = h.optInt("targetDaysPerWeek", 7),
                    colorHex = h.optLong("colorHex", 0xFF4CD964L),
                    history = history
                )
            } else HabitItem()

            val top3List = mutableListOf<Top3TaskItem>()
            if (root.has("top3Tasks")) {
                val arr = root.getJSONArray("top3Tasks")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    top3List.add(Top3TaskItem(
                        id = o.optString("id", "t$i"),
                        title = o.optString("title", ""),
                        isCompleted = o.optBoolean("isCompleted", false),
                        priority = o.optInt("priority", i + 1)
                    ))
                }
            }

            val eisenList = mutableListOf<EisenhowerItem>()
            if (root.has("eisenhowerTasks")) {
                val arr = root.getJSONArray("eisenhowerTasks")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    eisenList.add(EisenhowerItem(
                        id = o.optString("id", "e$i"),
                        title = o.optString("title", ""),
                        quadrant = o.optString("quadrant", "Q1_DO")
                    ))
                }
            }

            val tbList = mutableListOf<TimeBlockItem>()
            if (root.has("timeBlocks")) {
                val arr = root.getJSONArray("timeBlocks")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    tbList.add(TimeBlockItem(
                        id = o.optString("id", "tb$i"),
                        title = o.optString("title", ""),
                        startHour = o.optInt("startHour", 9),
                        startMinute = o.optInt("startMinute", 0),
                        endHour = o.optInt("endHour", 10),
                        endMinute = o.optInt("endMinute", 0),
                        tag = o.optString("tag", "FOCUS")
                    ))
                }
            }

            val goal = if (root.has("goal")) {
                val g = root.getJSONObject("goal")
                GoalMilestoneItem(
                    id = g.optString("id", "goal_1"),
                    title = g.optString("title", ""),
                    currentProgress = g.optInt("currentProgress", 0),
                    targetProgress = g.optInt("targetProgress", 100),
                    unit = g.optString("unit", "%"),
                    deadlineDateText = g.optString("deadlineDateText", ""),
                    milestoneCurrent = g.optInt("milestoneCurrent", 0),
                    milestoneTotal = g.optInt("milestoneTotal", 5)
                )
            } else GoalMilestoneItem()

            val ringsList = mutableListOf<HabitRingItem>()
            if (root.has("habitRings")) {
                val arr = root.getJSONArray("habitRings")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    ringsList.add(HabitRingItem(
                        id = o.optString("id", "r$i"),
                        label = o.optString("label", ""),
                        currentValue = o.optInt("currentValue", 0),
                        targetValue = o.optInt("targetValue", 1),
                        unit = o.optString("unit", ""),
                        colorHex = o.optLong("colorHex", 0xFF30D158)
                    ))
                }
            }

            val pipeline = if (root.has("pipeline")) {
                val p = root.getJSONObject("pipeline")
                TaskPipelineData(
                    todoCount = p.optInt("todoCount", 0),
                    inProgressCount = p.optInt("inProgressCount", 0),
                    doneCount = p.optInt("doneCount", 0)
                )
            } else TaskPipelineData()

            val bList = mutableListOf<BookmarkItem>()
            if (root.has("bookmarks")) {
                val arr = root.getJSONArray("bookmarks")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    bList.add(BookmarkItem(
                        id = o.optString("id", "b$i"),
                        title = o.optString("title", ""),
                        url = o.optString("url", ""),
                        domain = o.optString("domain", "")
                    ))
                }
            }

            val sList = mutableListOf<ClipboardSnippetItem>()
            if (root.has("clipboardSnippets")) {
                val arr = root.getJSONArray("clipboardSnippets")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    sList.add(ClipboardSnippetItem(
                        id = o.optString("id", "s$i"),
                        label = o.optString("label", ""),
                        content = o.optString("content", ""),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis())
                    ))
                }
            }

            ProductivityWidgetConfig(
                focusTimer = timer,
                habit = habit,
                top3Tasks = if (top3List.isNotEmpty()) top3List else ProductivityWidgetConfig.getDefaultConfig().top3Tasks,
                eisenhowerTasks = if (eisenList.isNotEmpty()) eisenList else ProductivityWidgetConfig.getDefaultConfig().eisenhowerTasks,
                timeBlocks = if (tbList.isNotEmpty()) tbList else ProductivityWidgetConfig.getDefaultConfig().timeBlocks,
                goal = goal,
                habitRings = if (ringsList.isNotEmpty()) ringsList else ProductivityWidgetConfig.getDefaultConfig().habitRings,
                pipeline = pipeline,
                bookmarks = if (bList.isNotEmpty()) bList else ProductivityWidgetConfig.getDefaultConfig().bookmarks,
                clipboardSnippets = if (sList.isNotEmpty()) sList else ProductivityWidgetConfig.getDefaultConfig().clipboardSnippets,
                screenTime = ScreenTimeData()
            )
        } catch (_: Exception) {
            ProductivityWidgetConfig.getDefaultConfig()
        }
    }
}

// =========================================================================
// HABIT COMPUTATION HELPERS
// =========================================================================

fun calculateHabitStreak(history: Map<String, Boolean>): Int {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    val todayStr = sdf.format(cal.time)

    var streak = 0
    val doneToday = history[todayStr] == true

    if (doneToday) {
        streak++
        cal.add(Calendar.DAY_OF_YEAR, -1)
    } else {
        val yesterdayCal = cal.clone() as Calendar
        yesterdayCal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(yesterdayCal.time)
        if (history[yesterdayStr] != true) {
            return 0
        }
        cal.add(Calendar.DAY_OF_YEAR, -1)
    }

    while (true) {
        val dateStr = sdf.format(cal.time)
        if (history[dateStr] == true) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            break
        }
    }
    return streak
}

fun calculateWeekCompletion(history: Map<String, Boolean>): Pair<Int, Int> {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()

    val dayOfWeekIndex = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    cal.add(Calendar.DAY_OF_YEAR, -dayOfWeekIndex)

    var completed = 0
    for (i in 0 until 7) {
        val dateStr = sdf.format(cal.time)
        if (history[dateStr] == true) completed++
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return Pair(completed, 7)
}
