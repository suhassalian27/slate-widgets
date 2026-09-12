package com.altusix.slate.widgets.productivity

import android.app.usage.UsageStatsManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
    val history: Map<String, Boolean> = emptyMap()
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
    val timeBlocks: List<TimeBlockItem> = emptyList(),
    val goal: GoalMilestoneItem = GoalMilestoneItem(),
    val habitRings: List<HabitRingItem> = listOf(
        HabitRingItem("r1", "Focus", 0, 4, "hrs", 0xFFFF5E3A),
        HabitRingItem("r2", "Read", 0, 30, "mins", 0xFF30D158),
        HabitRingItem("r3", "Workout", 0, 45, "mins", 0xFF0A84FF)
    ),
    val pipeline: TaskPipelineData = TaskPipelineData(),
    val bookmarks: List<BookmarkItem> = listOf(
        BookmarkItem("b1", "GitHub", "https://github.com", "github.com"),
        BookmarkItem("b2", "YouTube", "https://youtube.com", "youtube.com"),
        BookmarkItem("b3", "Reddit", "https://reddit.com", "reddit.com"),
        BookmarkItem("b4", "X", "https://x.com", "x.com")
    ),
    val showBookmarkFavicons: Boolean = true,
    val showBookmarkUrl: Boolean = true,
    val bookmarkPageIndex: Int = 0,
    val clipboardSnippets: List<ClipboardSnippetItem> = emptyList(),
    val screenTime: ScreenTimeData = ScreenTimeData()
) {
    companion object {
        fun getDefaultConfig() = ProductivityWidgetConfig()
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
            if (item.title.isBlank()) return
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

    fun advancePipelineStage(context: Context, widgetId: Int, stage: String) {
        val current = getConfig(context, widgetId)
        val p = current.pipeline
        val updated = when (stage) {
            "TODO" -> p.copy(todoCount = p.todoCount + 1)
            "ACTIVE" -> {
                if (p.todoCount > 0) {
                    p.copy(todoCount = p.todoCount - 1, inProgressCount = p.inProgressCount + 1)
                } else {
                    p.copy(inProgressCount = p.inProgressCount + 1)
                }
            }
            "DONE" -> {
                if (p.inProgressCount > 0) {
                    p.copy(inProgressCount = p.inProgressCount - 1, doneCount = p.doneCount + 1)
                } else if (p.todoCount > 0) {
                    p.copy(todoCount = p.todoCount - 1, doneCount = p.doneCount + 1)
                } else {
                    p.copy(doneCount = p.doneCount + 1)
                }
            }
            else -> p
        }
        saveConfig(context, widgetId, current.copy(pipeline = updated))
    }

    fun cycleBookmarkPage(context: Context, widgetId: Int, delta: Int) {
        val current = getConfig(context, widgetId)
        val pageSize = 4
        val totalPages = kotlin.math.ceil(current.bookmarks.size / pageSize.toFloat()).toInt().coerceAtLeast(1)
        var newPage = current.bookmarkPageIndex + delta
        if (newPage >= totalPages) newPage = 0
        if (newPage < 0) newPage = totalPages - 1
        saveConfig(context, widgetId, current.copy(bookmarkPageIndex = newPage))
    }

    fun resolveLiveScreenTime(context: Context): ScreenTimeData {
        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return ScreenTimeData()

            // 1. Midnight today in device timezone
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTime = cal.timeInMillis
            val endTime = System.currentTimeMillis()

            val events = usageStatsManager.queryEvents(startTime, endTime) ?: return ScreenTimeData()
            val event = android.app.usage.UsageEvents.Event()

            // 2. Identify launcher & system packages to ignore
            val pm = context.packageManager
            val launcherPkg = try {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                pm.resolveActivity(intent, 0)?.activityInfo?.packageName
            } catch (_: Exception) { null }

            val ignoredPackages = setOfNotNull(
                launcherPkg,
                "com.android.systemui",
                "com.google.android.apps.nexuslauncher",
                "com.sec.android.app.launcher",
                context.packageName
            )

            var unlockCount = 0
            val appDurations = mutableMapOf<String, Long>()
            var currentForegroundApp: String? = null
            var sessionStartTime = 0L
            var isScreenInteractive = true

            // 3. Replay exact interactive events
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName
                val timestamp = event.timeStamp

                when (event.eventType) {
                    // KEYGUARD_HIDDEN (18) = Exact device unlock
                    18 -> unlockCount++

                    // SCREEN_INTERACTIVE (15)
                    15 -> isScreenInteractive = true

                    // SCREEN_NON_INTERACTIVE (16)
                    16 -> {
                        isScreenInteractive = false
                        if (currentForegroundApp != null && sessionStartTime > 0L) {
                            val elapsed = timestamp - sessionStartTime
                            if (elapsed > 0 && currentForegroundApp !in ignoredPackages) {
                                appDurations[currentForegroundApp] = (appDurations[currentForegroundApp] ?: 0L) + elapsed
                            }
                            currentForegroundApp = null
                            sessionStartTime = 0L
                        }
                    }

                    // ACTIVITY_RESUMED (1)
                    1 -> {
                        if (currentForegroundApp != null && sessionStartTime > 0L) {
                            val elapsed = timestamp - sessionStartTime
                            if (elapsed > 0 && currentForegroundApp !in ignoredPackages) {
                                appDurations[currentForegroundApp] = (appDurations[currentForegroundApp] ?: 0L) + elapsed
                            }
                        }
                        if (isScreenInteractive && pkg !in ignoredPackages) {
                            currentForegroundApp = pkg
                            sessionStartTime = timestamp
                        } else {
                            currentForegroundApp = null
                            sessionStartTime = 0L
                        }
                    }

                    // ACTIVITY_PAUSED (2)
                    2 -> {
                        if (currentForegroundApp == pkg && sessionStartTime > 0L) {
                            val elapsed = timestamp - sessionStartTime
                            if (elapsed > 0 && pkg !in ignoredPackages) {
                                appDurations[pkg] = (appDurations[pkg] ?: 0L) + elapsed
                            }
                            currentForegroundApp = null
                            sessionStartTime = 0L
                        }
                    }
                }
            }

            // Close trailing active session
            if (currentForegroundApp != null && sessionStartTime > 0L && isScreenInteractive) {
                val elapsed = endTime - sessionStartTime
                if (elapsed > 0 && currentForegroundApp !in ignoredPackages) {
                    appDurations[currentForegroundApp] = (appDurations[currentForegroundApp] ?: 0L) + elapsed
                }
            }

            // 4. Resolve human-readable App Labels & Top Apps
            val sortedApps = appDurations.entries
                .filter { it.value >= 60_000L } // Minimum 1 minute
                .sortedByDescending { it.value }

            val topCategories = sortedApps.take(3).map { entry ->
                val appName = try {
                    val appInfo = pm.getApplicationInfo(entry.key, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    entry.key.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: entry.key
                }
                val minutes = (entry.value / (1000 * 60)).toInt()
                Pair(appName, minutes)
            }

            val totalMs = appDurations.values.sum()
            val totalMinutes = (totalMs / (1000 * 60)).toInt()

            return ScreenTimeData(
                totalMinutesToday = totalMinutes,
                limitMinutes = 240,
                pickupsCount = unlockCount,
                topCategories = topCategories
            )
        } catch (_: Exception) {
            return ScreenTimeData()
        }
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
        root.put("showBookmarkFavicons", config.showBookmarkFavicons)
        root.put("showBookmarkUrl", config.showBookmarkUrl)
        root.put("bookmarkPageIndex", config.bookmarkPageIndex)

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

            val showFavicons = root.optBoolean("showBookmarkFavicons", true)
            val showUrl = root.optBoolean("showBookmarkUrl", true)
            val pageIndex = root.optInt("bookmarkPageIndex", 0)

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
                showBookmarkFavicons = showFavicons,
                showBookmarkUrl = showUrl,
                bookmarkPageIndex = pageIndex,
                clipboardSnippets = if (sList.isNotEmpty()) sList else ProductivityWidgetConfig.getDefaultConfig().clipboardSnippets,
                screenTime = ScreenTimeData()
            )
        } catch (_: Exception) {
            ProductivityWidgetConfig.getDefaultConfig()
        }
    }
}

// =========================================================================
// 4. URL & DOMAIN EXTRACTION HELPERS
// =========================================================================

fun extractDomainAndTitle(inputUrl: String): Pair<String, String> {
    var clean = inputUrl.trim()
    if (clean.isBlank()) return "" to ""
    if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
        clean = "https://$clean"
    }
    return try {
        val uri = android.net.Uri.parse(clean)
        val host = uri.host?.removePrefix("www.") ?: clean
        val parts = host.split(".")
        val rawName = if (parts.size >= 2) parts[parts.size - 2] else host
        val formattedTitle = when (rawName.lowercase(java.util.Locale.getDefault())) {
            "github" -> "GitHub"
            "youtube" -> "YouTube"
            "reddit" -> "Reddit"
            "x", "twitter" -> "X"
            "figma" -> "Figma"
            "notion" -> "Notion"
            "linkedin" -> "LinkedIn"
            "stackoverflow" -> "StackOverflow"
            else -> rawName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
        }
        Pair(host, formattedTitle)
    } catch (_: Exception) {
        Pair(clean, clean)
    }
}

// =========================================================================
// 5. HABIT COMPUTATION HELPERS
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

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager ?: return false
    val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    }
    return mode == android.app.AppOpsManager.MODE_ALLOWED
}