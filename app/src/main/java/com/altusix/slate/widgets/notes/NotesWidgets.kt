package com.altusix.slate.widgets.notes

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import com.altusix.slate.R
import androidx.compose.ui.graphics.toArgb
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.core.theme.ThemePreferences
import com.altusix.slate.data.local.SlateWidgetConfig

fun getNotesWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("Sticky Note Pad", "2x2", "Notes", NotesStickyPadReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Multi-Note Stack", "2x2", "Notes", NotesStackReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Desk Memo Pad", "4x2", "Notes", NotesDeskMemoReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Checklist Tasks", "4x2", "Notes", NotesChecklistReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Dot Grid Scratchpad", "2x2", "Notes", NotesDotGridReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Classic Legal Pad", "4x2", "Notes", NotesLegalPadReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Quick Thought Strip", "4x1", "Notes", NotesQuickThoughtReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Torn Receipt Log", "2x2", "Notes", NotesTornReceiptReceiver::class.java, hasModeOption = true),
    )
}

fun updateAllNotesWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        NotesStickyPadReceiver::class.java,
        NotesDeskMemoReceiver::class.java,
        NotesChecklistReceiver::class.java,
        NotesLegalPadReceiver::class.java,
        NotesQuickThoughtReceiver::class.java,
        NotesTornReceiptReceiver::class.java,
        NotesDotGridReceiver::class.java,
        NotesStackReceiver::class.java
    )
    for (receiverClass in receivers) {
        val ids = manager.getAppWidgetIds(ComponentName(context, receiverClass)) ?: intArrayOf()
        if (ids.isNotEmpty()) {
            val intent = Intent(context, receiverClass).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}

private fun loadSlateWidgetConfig(context: Context, widgetId: Int): SlateWidgetConfig {
    val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val bgKey = "widget_${widgetId}_bg_color"

    if (!widgetPrefs.contains(bgKey) && widgetId != -1) {
        val globalSettings = ThemePreferences(context).getThemeSettings()
        val isLight = (((globalSettings.bgHex shr 16 and 0xFFL) * 0.2126f) +
                ((globalSettings.bgHex shr 8 and 0xFFL) * 0.7152f) +
                ((globalSettings.bgHex and 0xFFL) * 0.0722f)) / 255f > 0.5f

        widgetPrefs.edit()
            .putString("widget_${widgetId}_theme_mode", if (isLight) "LIGHT" else "DARK")
            .putLong("widget_${widgetId}_bg_color", globalSettings.bgHex)
            .putLong("widget_${widgetId}_accent_color", globalSettings.accentHex)
            .putFloat("widget_${widgetId}_opacity", globalSettings.opacity)
            .apply()
    }

    val globalSettings = ThemePreferences(context).getThemeSettings()
    val bgColor = widgetPrefs.getLong("widget_${widgetId}_bg_color", globalSettings.bgHex)
    val opacity = widgetPrefs.getFloat("widget_${widgetId}_opacity", globalSettings.opacity)
    val accentColor = widgetPrefs.getLong("widget_${widgetId}_accent_color", globalSettings.accentHex)

    val isLight = (((bgColor shr 16 and 0xFFL) * 0.2126f) +
            ((bgColor shr 8 and 0xFFL) * 0.7152f) +
            ((bgColor and 0xFFL) * 0.0722f)) / 255f > 0.5f
    val mode = widgetPrefs.getString("widget_${widgetId}_theme_mode", if (isLight) "LIGHT" else "DARK")
        ?: if (isLight) "LIGHT" else "DARK"

    return SlateWidgetConfig(
        themeMode = mode,
        backgroundColorHex = bgColor,
        opacity = opacity,
        accentColorHex = accentColor
    )
}

private fun parseAndLockIsResponsive(context: Context, widgetId: Int): Boolean {
    val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val modeKey = "widget_${widgetId}_mode"
    val isResponsiveKey = "widget_${widgetId}_is_responsive"

    if (widgetPrefs.contains(modeKey)) {
        return widgetPrefs.getString(modeKey, "RESPONSIVE") == "RESPONSIVE"
    }
    if (widgetPrefs.contains(isResponsiveKey)) {
        return widgetPrefs.getBoolean(isResponsiveKey, true)
    }

    val launcherPrefs = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
    val defaultResponsive = launcherPrefs.getBoolean("default_is_responsive", true)
    widgetPrefs.edit().putBoolean(isResponsiveKey, defaultResponsive).apply()
    return defaultResponsive
}

abstract class BaseNotesReceiver(private val layoutResId: Int) : AppWidgetProvider() {

    companion object {
        const val ACTION_EDIT_NOTE = "com.altusix.slate.notes.ACTION_EDIT_NOTE"
        const val ACTION_TOGGLE_CHECK = "com.altusix.slate.notes.ACTION_TOGGLE_CHECK"
        const val ACTION_CYCLE_PAGE = "com.altusix.slate.notes.ACTION_CYCLE_PAGE"
        const val EXTRA_ITEM_INDEX = "extra_item_index"
        const val EXTRA_DELTA = "extra_delta"
        const val EXTRA_EDIT_MODE = "extra_edit_mode"
    }

    // Default mode is TEXT_ONLY; overridden to CHECKLIST_ONLY in checklist receivers
    protected open val widgetEditMode: String = "TEXT_ONLY"

    abstract fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        wDp: Int,
        hDp: Int
    ): Bitmap

    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        when (intent.action) {
            ACTION_EDIT_NOTE -> {
                val mode = intent.getStringExtra(EXTRA_EDIT_MODE) ?: widgetEditMode
                val editIntent = Intent(context, NoteEditActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    putExtra(EXTRA_EDIT_MODE, mode)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(editIntent)
                return
            }
            ACTION_TOGGLE_CHECK -> {
                val index = intent.getIntExtra(EXTRA_ITEM_INDEX, -1)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && index != -1) {
                    NotesStorageManager.toggleChecklistItem(context, appWidgetId, index)
                    updateSingleWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                }
                return
            }
            ACTION_CYCLE_PAGE -> {
                val delta = intent.getIntExtra(EXTRA_DELTA, 1)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val totalPages = NotesStorageManager.getMockNotesStack().size
                    NotesStorageManager.cycleStackPage(context, appWidgetId, delta, totalPages)
                    updateSingleWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                }
                return
            }
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) updateSingleWidget(context, appWidgetManager, id)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        updateSingleWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    private fun updateSingleWidget(context: Context, manager: AppWidgetManager, id: Int) {
        try {
            val config = loadSlateWidgetConfig(context, id)
            val options = manager.getAppWidgetOptions(id)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160) ?: 160
            val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160) ?: 160
            val wDp = if (wDpRaw <= 0) 160 else wDpRaw
            val hDp = if (hDpRaw <= 0) 160 else hDpRaw

            val bitmap = renderWidgetBitmap(context, id, config, wDp, hDp)
            val views = RemoteViews(context.packageName, layoutResId)
            views.setImageViewBitmap(R.id.widget_canvas_surface, bitmap)

            setupTouchTargets(context, views, id)
            manager.updateAppWidget(id, views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    protected open fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        val editIntent = Intent(context, this.javaClass).apply {
            action = ACTION_EDIT_NOTE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_EDIT_MODE, widgetEditMode)
            data = Uri.parse("slate_notes://$appWidgetId/edit")
        }
        val editPi = PendingIntent.getBroadcast(
            context,
            (appWidgetId * 41 + 1),
            editIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            views.setOnClickPendingIntent(R.id.btn_note_open_edit, editPi)
        } catch (_: Exception) {}
        try {
            views.setOnClickPendingIntent(R.id.btn_note_open_edit_alt, editPi)
        } catch (_: Exception) {}
    }
}

// 1. Sticky Note Pad (2x2 - Dog-Ear Corner)
class NotesStickyPadReceiver : BaseNotesReceiver(R.layout.widget_notes_card_layout) {
    override val widgetEditMode: String = "TEXT_ONLY"
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val note = if (appWidgetId == -1) SlateNoteData.getDefaultNote() else NotesStorageManager.getNoteForWidget(context, appWidgetId, "Sticky Note")
        val isResponsive = if (appWidgetId == -1) false else parseAndLockIsResponsive(context, appWidgetId)
        return generateStickyNoteBitmap(context, note, config, isResponsive, wDp, hDp)
    }
}

// 2. Desk Memo Pad (4x2 - Taped Ruled Pad)
class NotesDeskMemoReceiver : BaseNotesReceiver(R.layout.widget_notes_card_layout) {
    override val widgetEditMode: String = "TEXT_ONLY"

    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val note = if (appWidgetId == -1) SlateNoteData.getDefaultNote() else NotesStorageManager.getNoteForWidget(context, appWidgetId, "Desk Memo")
        val isResponsive = if (appWidgetId == -1) true else parseAndLockIsResponsive(context, appWidgetId)
        return generateDeskMemoBitmap(context, note, config, isResponsive, wDp, hDp)
    }
}

// 3. Checklist Tasks (4x2 - Adaptive Dynamic Rows)
class NotesChecklistReceiver : BaseNotesReceiver(R.layout.widget_notes_checklist_4x2_layout) {
    override val widgetEditMode: String = "CHECKLIST_ONLY"

    companion object {
        private val widgetDimensions = java.util.concurrent.ConcurrentHashMap<Int, Pair<Int, Int>>()

        fun resolveDimensions(context: Context, appWidgetId: Int): Pair<Int, Int> {
            val manager = AppWidgetManager.getInstance(context) ?: return Pair(260, 140)
            val options = manager.getAppWidgetOptions(appWidgetId) ?: return Pair(260, 140)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

            val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
            val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)

            // Portrait: Width is MIN_WIDTH, Height is MAX_HEIGHT
            // Landscape: Width is MAX_WIDTH, Height is MIN_HEIGHT
            val w = if (isLandscape) {
                if (maxW > 0) maxW else minW
            } else {
                if (minW > 0) minW else maxW
            }

            val h = if (isLandscape) {
                if (minH > 0) minH else maxH
            } else {
                if (maxH > 0) maxH else minH
            }

            return Pair(if (w > 0) w else 260, if (h > 0) h else 140)
        }
    }

    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        widgetDimensions[appWidgetId] = Pair(wDp, hDp)
        val isResponsive = if (appWidgetId == -1) false else parseAndLockIsResponsive(context, appWidgetId)

        if (appWidgetId == -1) {
            val note = NotesStorageManager.getNoteForWidget(context, -1, "Checklist")
            return generateChecklistBitmap(context, note, config, isResponsive, wDp, hDp)
        }

        return generateCardSurfaceBitmap(context, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        val config = loadSlateWidgetConfig(context, appWidgetId)
        val note = if (appWidgetId == -1) {
            NotesStorageManager.getNoteForWidget(context, -1, "Checklist")
        } else {
            NotesStorageManager.getNoteForWidget(context, appWidgetId, "Checklist")
        }

        val isResponsive = if (appWidgetId == -1) false else parseAndLockIsResponsive(context, appWidgetId)
        val (wDp, hDp) = widgetDimensions[appWidgetId] ?: resolveDimensions(context, appWidgetId)

        // Calculate Letterbox Margins for Fixed Mode
        val density = context.resources.displayMetrics.density
        val padH: Int
        val padV: Int
        val effectiveCardHDp: Int

        if (!isResponsive) {
            val targetAspect = 2.0f
            val currentAspect = wDp.toFloat() / hDp.toFloat()

            if (currentAspect > targetAspect) {
                val contentW = hDp * targetAspect
                padH = (((wDp - contentW) / 2f) * density).toInt()
                padV = 0
                effectiveCardHDp = hDp
            } else {
                val contentH = wDp / targetAspect
                padH = 0
                padV = (((hDp - contentH) / 2f) * density).toInt()
                effectiveCardHDp = contentH.toInt()
            }
        } else {
            padH = 0
            padV = 0
            effectiveCardHDp = hDp
        }

        views.setViewPadding(R.id.layout_checklist_content, padH, padV, padH, padV)

        val accentColor = androidx.compose.ui.graphics.Color(config.accentColorHex).toArgb()
        val isLight = config.themeMode == "LIGHT"
        val primaryText = if (isLight) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        val secondaryText = if (isLight) android.graphics.Color.parseColor("#6C6C70") else android.graphics.Color.parseColor("#8E8E93")
        val fScale = note.fontScaleMultiplier

        // Header View Binding
        views.setTextViewText(R.id.txt_checklist_title, note.title)
        views.setTextColor(R.id.txt_checklist_title, primaryText)
        views.setTextViewTextSize(R.id.txt_checklist_title, android.util.TypedValue.COMPLEX_UNIT_SP, 16f * fScale)

        views.setTextViewText(R.id.txt_checklist_count, "${note.completedCount}/${note.totalCount}")
        views.setTextColor(R.id.txt_checklist_count, accentColor)
        views.setTextViewTextSize(R.id.txt_checklist_count, android.util.TypedValue.COMPLEX_UNIT_SP, 13f * fScale)

        val editBmp = getTintedVectorBitmap(context, R.drawable.ic_pencil_alt, accentColor)
        if (editBmp != null) views.setImageViewBitmap(R.id.icon_checklist_edit, editBmp)

        val editIntent = Intent(context, this.javaClass).apply {
            action = ACTION_EDIT_NOTE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_EDIT_MODE, widgetEditMode)
            data = Uri.parse("slate_notes://$appWidgetId/edit")
        }
        val editPi = PendingIntent.getBroadcast(
            context,
            appWidgetId * 41 + 1,
            editIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_note_open_edit, editPi)

        // Dynamic Row Capacity Calculation
        val headerH = 42
        val bottomH = 6
        val availableH = (effectiveCardHDp - headerH - bottomH).coerceAtLeast(0)

        // Calibrated Row Heights per font scale
        val rowHDp = when (note.fontSize) {
            "SMALL" -> 31
            "LARGE" -> 39
            else -> 34 // "MEDIUM"
        }
        val maxFittingRows = (availableH / rowHDp).coerceAtLeast(1)

        views.removeAllViews(R.id.layout_dynamic_checklist_rows)

        val itemsToShow = minOf(note.items.size, maxFittingRows)
        val checkedBmp = createCheckmarkBitmap(context, isDone = true, accentColor, secondaryText, fScale)
        val uncheckedBmp = createCheckmarkBitmap(context, isDone = false, accentColor, secondaryText, fScale)

        for (i in 0 until itemsToShow) {
            val item = note.items[i]
            val rowView = RemoteViews(context.packageName, R.layout.widget_checklist_row_item)

            val textPaintFlags = if (item.isDone) {
                android.graphics.Paint.STRIKE_THRU_TEXT_FLAG or android.graphics.Paint.ANTI_ALIAS_FLAG
            } else {
                android.graphics.Paint.ANTI_ALIAS_FLAG
            }
            rowView.setTextViewText(R.id.row_task_text, item.text)
            rowView.setTextColor(R.id.row_task_text, if (item.isDone) secondaryText else primaryText)
            rowView.setInt(R.id.row_task_text, "setPaintFlags", textPaintFlags)
            rowView.setTextViewTextSize(R.id.row_task_text, android.util.TypedValue.COMPLEX_UNIT_SP, 14f * fScale)

            rowView.setImageViewBitmap(R.id.row_checkbox_icon, if (item.isDone) checkedBmp else uncheckedBmp)

            val checkIntent = Intent(context, this.javaClass).apply {
                action = ACTION_TOGGLE_CHECK
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(EXTRA_ITEM_INDEX, i)
                data = Uri.parse("slate_notes://$appWidgetId/check/$i")
            }
            val checkPi = PendingIntent.getBroadcast(
                context,
                appWidgetId * 1000 + i,
                checkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            rowView.setOnClickPendingIntent(R.id.row_click_target, checkPi)

            views.addView(R.id.layout_dynamic_checklist_rows, rowView)
        }

        // Empty state placeholder
        if (note.items.isEmpty()) {
            val emptyRow = RemoteViews(context.packageName, R.layout.widget_checklist_row_item)
            emptyRow.setTextViewText(R.id.row_task_text, "Tap to add items...")
            emptyRow.setTextColor(R.id.row_task_text, secondaryText)
            emptyRow.setTextViewTextSize(R.id.row_task_text, android.util.TypedValue.COMPLEX_UNIT_SP, 14f * fScale)
            emptyRow.setImageViewBitmap(R.id.row_checkbox_icon, uncheckedBmp)
            emptyRow.setOnClickPendingIntent(R.id.row_click_target, editPi)
            views.addView(R.id.layout_dynamic_checklist_rows, emptyRow)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        val dims = resolveDimensions(context, appWidgetId)
        widgetDimensions[appWidgetId] = dims
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }
}

// 4. Classic Legal Pad (4x2)
class NotesLegalPadReceiver : BaseNotesReceiver(R.layout.widget_notes_card_layout) {
    override val widgetEditMode: String = "TEXT_ONLY"

    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val note = if (appWidgetId == -1) {
            SlateNoteData.getDefaultNote()
        } else {
            NotesStorageManager.getNoteForWidget(context, appWidgetId, "Legal Pad")
        }
        val isResponsive = if (appWidgetId == -1) true else parseAndLockIsResponsive(context, appWidgetId)
        return generateLegalPadBitmap(context, note, config, isResponsive, wDp, hDp)
    }
}

// 5. Quick Thought Strip (4x1)
class NotesQuickThoughtReceiver : BaseNotesReceiver(R.layout.widget_notes_card_layout) {
    override val widgetEditMode: String = "TEXT_ONLY"

    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val note = if (appWidgetId == -1) {
            NotesStorageManager.getNoteForWidget(context, -1, "Thought")
        } else {
            NotesStorageManager.getNoteForWidget(context, appWidgetId, "Thought")
        }
        val isResponsive = if (appWidgetId == -1) true else parseAndLockIsResponsive(context, appWidgetId)
        return generateQuickThoughtBitmap(context, note, config, isResponsive, wDp, hDp)
    }
}


// 7. Torn Receipt Log (2x2)
class NotesTornReceiptReceiver : BaseNotesReceiver(R.layout.widget_notes_card_layout) {
    override val widgetEditMode: String = "TEXT_ONLY"

    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val note = if (appWidgetId == -1) {
            NotesStorageManager.getNoteForWidget(context, -1, "Receipt")
        } else {
            NotesStorageManager.getNoteForWidget(context, appWidgetId, "Receipt")
        }
        val isResponsive = if (appWidgetId == -1) true else parseAndLockIsResponsive(context, appWidgetId)
        return generateTornReceiptBitmap(context, note, config, isResponsive, wDp, hDp)
    }
}

// 8. Dot Grid Scratchpad (2x2)
class NotesDotGridReceiver : BaseNotesReceiver(R.layout.widget_notes_card_layout) {
    override val widgetEditMode: String = "TEXT_ONLY"

    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val note = if (appWidgetId == -1) {
            SlateNoteData.getDefaultNote()
        } else {
            NotesStorageManager.getNoteForWidget(context, appWidgetId, "Grid")
        }
        val isResponsive = if (appWidgetId == -1) true else parseAndLockIsResponsive(context, appWidgetId)
        return generateDotGridBitmap(context, note, config, isResponsive, wDp, hDp)
    }
}

// 9. Multi-Note Stack (2x2)
class NotesStackReceiver : BaseNotesReceiver(R.layout.widget_notes_stack_layout) {
    override val widgetEditMode: String = "TEXT_ONLY"

    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val stack = if (appWidgetId == -1) {
            NotesStorageManager.getNotesStack(context, -1)
        } else {
            NotesStorageManager.getNotesStack(context, appWidgetId)
        }
        val page = if (appWidgetId == -1) 0 else NotesStorageManager.getStackPageIndex(context, appWidgetId)
        val note = stack[page % stack.size]
        val isResponsive = if (appWidgetId == -1) true else parseAndLockIsResponsive(context, appWidgetId)
        return generateNoteStackBitmap(context, note, page % stack.size, stack.size, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        super.setupTouchTargets(context, views, appWidgetId)
        val page = NotesStorageManager.getStackPageIndex(context, appWidgetId)

        // Bind main note area click to edit the CURRENT page
        val editIntent = Intent(context, NoteEditActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra("EXTRA_STACK_PAGE", page)
            putExtra(EXTRA_EDIT_MODE, widgetEditMode)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val editPi = PendingIntent.getActivity(
            context,
            appWidgetId * 59 + 3,
            editIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_note_open_edit, editPi)

        // Prev Arrow
        val prevIntent = Intent(context, this.javaClass).apply {
            action = ACTION_CYCLE_PAGE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_DELTA, -1)
            data = Uri.parse("slate_notes://$appWidgetId/stack/prev")
        }
        val prevPi = PendingIntent.getBroadcast(
            context,
            appWidgetId * 59 + 1,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_stack_prev, prevPi)

        // Next Arrow
        val nextIntent = Intent(context, this.javaClass).apply {
            action = ACTION_CYCLE_PAGE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_DELTA, 1)
            data = Uri.parse("slate_notes://$appWidgetId/stack/next")
        }
        val nextPi = PendingIntent.getBroadcast(
            context,
            appWidgetId * 59 + 2,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_stack_next, nextPi)
    }
}
