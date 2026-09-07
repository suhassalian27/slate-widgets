package com.altusix.slate.widgets.notes

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SlateChecklistItem(
    val id: String = System.currentTimeMillis().toString(),
    val text: String,
    val isDone: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("text", text)
        put("isDone", isDone)
    }

    companion object {
        fun fromJson(json: JSONObject): SlateChecklistItem {
            return SlateChecklistItem(
                id = json.optString("id", System.currentTimeMillis().toString()),
                text = json.optString("text", ""),
                isDone = json.optBoolean("isDone", false)
            )
        }
    }
}

data class SlateNoteData(
    val id: String = "default_note",
    val title: String = "Untitled Note",
    val content: String = "Tap here to start writing your thoughts...",
    val items: List<SlateChecklistItem> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
    val category: String = "Memo",
    val fontSize: String = "MEDIUM" // "SMALL", "MEDIUM", "LARGE"
) {
    val completedCount: Int get() = items.count { it.isDone }
    val totalCount: Int get() = items.size
    val progress: Float get() = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    val fontScaleMultiplier: Float
        get() = when (fontSize) {
            "SMALL" -> 1f   // Compact: fits maximum lines and tasks
            "LARGE" -> 1.60f   // Large: bold, prominent, and easy to read at a glance
            else -> 1.30f      // "MEDIUM": comfortable standard reading scale
        }

    fun toJson(): String {
        val json = JSONObject()
        json.put("id", id)
        json.put("title", title)
        json.put("content", content)
        json.put("updatedAt", updatedAt)
        json.put("category", category)
        json.put("fontSize", fontSize)

        val arr = JSONArray()
        for (item in items) {
            arr.put(item.toJson())
        }
        json.put("items", arr)
        return json.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): SlateNoteData {
            return try {
                val json = JSONObject(jsonStr)
                val itemsList = mutableListOf<SlateChecklistItem>()
                val arr = json.optJSONArray("items")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i)
                        if (obj != null) {
                            itemsList.add(SlateChecklistItem.fromJson(obj))
                        }
                    }
                }
                SlateNoteData(
                    id = json.optString("id", "default_note"),
                    title = json.optString("title", "Untitled Note"),
                    content = json.optString("content", "Tap here to start writing..."),
                    items = itemsList,
                    updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                    category = json.optString("category", "Memo"),
                    fontSize = json.optString("fontSize", "MEDIUM")
                )
            } catch (_: Exception) {
                getDefaultNote()
            }
        }

        fun getDefaultNote(): SlateNoteData {
            return SlateNoteData(
                id = "default_note",
                title = "Design Principles",
                content = "1. Keep it minimal & bold.\n2. Embrace negative space.\n3. Typography carries weight.",
                items = listOf(
                    SlateChecklistItem(text = "Wireframe new widgets", isDone = true),
                    SlateChecklistItem(text = "Refine canvas rendering", isDone = true),
                    SlateChecklistItem(text = "Theme studio sync", isDone = false),
                    SlateChecklistItem(text = "Review responsive layout", isDone = false)
                ),
                category = "Slate",
                fontSize = "MEDIUM"
            )
        }
    }
}

object NotesStorageManager {
    private const val PREFS_NAME = "slate_notes_prefs"

    fun getNoteForWidget(context: Context, widgetId: Int, defaultTitle: String = "Daily Focus"): SlateNoteData {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("widget_${widgetId}_data", null)
        if (!jsonStr.isNullOrBlank()) {
            return SlateNoteData.fromJson(jsonStr)
        }

        // Generate tailored default based on defaultTitle
        val initialNote = when {
            defaultTitle.contains("Checklist", ignoreCase = true) || defaultTitle.contains("Task", ignoreCase = true) -> {
                SlateNoteData(
                    title = "Today's Priorities",
                    content = "",
                    items = listOf(
                        SlateChecklistItem(text = "Review quarterly goals", isDone = true),
                        SlateChecklistItem(text = "Ship Slate widgets", isDone = true),
                        SlateChecklistItem(text = "Evening run & stretch", isDone = false),
                        SlateChecklistItem(text = "Read design journal", isDone = false),
                        SlateChecklistItem(text = "Prep tomorrow's agenda", isDone = false)
                    ),
                    category = "Checklist"
                )
            }
            defaultTitle.contains("Legal", ignoreCase = true) -> {
                SlateNoteData(
                    title = "Project Roadmap",
                    content = "• Phase 1: Core widget canvas architecture\n• Phase 2: Dynamic theme engine integration\n• Phase 3: Interactive touch state binding\n• Phase 4: Polish animations & typography",
                    category = "Roadmap"
                )
            }
            defaultTitle.contains("Thought", ignoreCase = true) -> {
                SlateNoteData(
                    title = "Daily Mantra",
                    content = "Simplicity is the ultimate sophistication.",
                    category = "Mantra"
                )
            }
            defaultTitle.contains("Receipt", ignoreCase = true) -> {
                SlateNoteData(
                    title = "EXPENSE / LOG",
                    content = "COFFEE ROASTERS     $4.50\nBOOKSTORE           $22.00\nDESIGN ASSETS       $15.00\n------------------------\nTOTAL               $41.50",
                    category = "Log"
                )
            }
            else -> SlateNoteData.getDefaultNote()
        }

        saveNoteForWidget(context, widgetId, initialNote)
        return initialNote
    }

    fun saveNoteForWidget(context: Context, widgetId: Int, note: SlateNoteData) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("widget_${widgetId}_data", note.toJson()).apply()
    }

    fun toggleChecklistItem(context: Context, widgetId: Int, itemIndex: Int): SlateNoteData {
        val note = getNoteForWidget(context, widgetId, "Checklist")
        if (itemIndex in note.items.indices) {
            val updatedItems = note.items.toMutableList()
            val current = updatedItems[itemIndex]
            updatedItems[itemIndex] = current.copy(isDone = !current.isDone)
            val updatedNote = note.copy(items = updatedItems, updatedAt = System.currentTimeMillis())
            saveNoteForWidget(context, widgetId, updatedNote)
            return updatedNote
        }
        return note
    }

    fun getStackPageIndex(context: Context, widgetId: Int): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("widget_${widgetId}_stack_index", 0)
    }

    fun cycleStackPage(context: Context, widgetId: Int, delta: Int, totalPages: Int): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt("widget_${widgetId}_stack_index", 0)
        val next = (current + delta + totalPages) % totalPages
        prefs.edit().putInt("widget_${widgetId}_stack_index", next).apply()
        return next
    }

    fun getMockNotesStack(): List<SlateNoteData> {
        return listOf(
            SlateNoteData(
                title = "1. Minimalist Vision",
                content = "Simplicity in form, depth in purpose. Build tools that feel like physical objects on slate stone.",
                category = "Manifesto"
            ),
            SlateNoteData(
                title = "2. Active Sprint",
                content = "Refining widget touch latency and sub-second canvas rasterization across Android 15.",
                category = "Engineering"
            ),
            SlateNoteData(
                title = "3. Reading List",
                content = "• The Design of Everyday Things\n• Dieter Rams: Ten Principles\n• Grid Systems in Graphic Design",
                category = "Inspiration"
            )
        )
    }
}
