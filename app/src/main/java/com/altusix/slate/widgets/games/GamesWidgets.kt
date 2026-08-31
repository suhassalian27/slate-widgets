package com.altusix.slate.widgets.games

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.RemoteViews
import com.altusix.slate.R
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.data.local.SlateWidgetConfig

private fun loadSlateWidgetConfig(context: Context, widgetId: Int): SlateWidgetConfig {
    val prefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val themeMode = prefs.getString("widget_${widgetId}_theme_mode", "DARK") ?: "DARK"
    val bgColor = prefs.getLong("widget_${widgetId}_bg_color", 0xFF161618L)
    val opacity = prefs.getFloat("widget_${widgetId}_opacity", 1.0f)
    val accentColor = prefs.getLong("widget_${widgetId}_accent_color", 0xFFFFFFFFL)
    return SlateWidgetConfig(themeMode = themeMode, backgroundColorHex = bgColor, opacity = opacity, accentColorHex = accentColor)
}

private fun parseAndLockIsResponsive(context: Context, widgetId: Int): Boolean {
    val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val modeKey = "widget_${widgetId}_mode"
    val isResponsiveKey = "widget_${widgetId}_is_responsive"
    if (widgetPrefs.contains(modeKey)) return widgetPrefs.getString(modeKey, "RESPONSIVE") == "RESPONSIVE"
    if (widgetPrefs.contains(isResponsiveKey)) return widgetPrefs.getBoolean(isResponsiveKey, true)

    val launcherPrefs = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
    val defaultResponsive = launcherPrefs.getBoolean("default_is_responsive", true)
    widgetPrefs.edit().putBoolean(isResponsiveKey, defaultResponsive).apply()
    return defaultResponsive
}

private fun checkTicTacToeWinner(board: IntArray): Int {
    val lines = arrayOf(
        intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8),
        intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8),
        intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)
    )
    for (line in lines) {
        if (board[line[0]] != 0 && board[line[0]] == board[line[1]] && board[line[1]] == board[line[2]]) {
            return board[line[0]]
        }
    }
    if (board.none { it == 0 }) return 3
    return 0
}

private fun getTicTacToeState(context: Context, widgetId: Int): TicTacToeState {
    val prefs = context.getSharedPreferences("slate_tictactoe_prefs", Context.MODE_PRIVATE)
    val boardStr = prefs.getString("widget_${widgetId}_board", "0,0,0,0,0,0,0,0,0") ?: "0,0,0,0,0,0,0,0,0"
    val board = boardStr.split(",").map { it.trim().toIntOrNull() ?: 0 }.toIntArray()
    val winner = checkTicTacToeWinner(board)
    val turn = prefs.getInt("widget_${widgetId}_turn", 1)
    val isVsRobot = prefs.getBoolean("widget_${widgetId}_vs_robot", true)
    return TicTacToeState(board, turn, winner, isVsRobot)
}

private fun saveTicTacToeState(context: Context, widgetId: Int, state: TicTacToeState) {
    val prefs = context.getSharedPreferences("slate_tictactoe_prefs", Context.MODE_PRIVATE)
    val boardStr = state.board.joinToString(",")
    prefs.edit().putString("widget_${widgetId}_board", boardStr).putInt("widget_${widgetId}_turn", state.currentTurn).putBoolean("widget_${widgetId}_vs_robot", state.isVsRobot).apply()
}

abstract class BaseGamesReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) updateWidget(context, appWidgetManager, widgetId)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, newOptions: Bundle?) {
        updateWidget(context, appWidgetManager, widgetId)
    }

    fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) ?: 200
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200) ?: 200
        val wDp = if (wDpRaw <= 0) 200 else wDpRaw
        val hDp = if (hDpRaw <= 0) 200 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)

        renderAndApplyWidget(context, appWidgetManager, widgetId, config, isResponsive, wDp, hDp)
    }

    abstract fun renderAndApplyWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int)
}

fun getGamesWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo(name = "Tic Tac Toe", category = "Games", sizeText = "2x2", receiverClass = GamesTicTacToeReceiver::class.java, hasModeOption = true)
    )
}

// 1. TIC TAC TOE INTERACTIVE (2x2)
class GamesTicTacToeReceiver : BaseGamesReceiver() {
    fun renderWidgetBitmap(context: Context, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        return generateTicTacToeWidgetBitmap(context, config, false, wDp, hDp, -1, TicTacToeState(intArrayOf(1, 2, 0, 0, 1, 0, 0, 0, 2), 1, 0, true))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        var state = getTicTacToeState(context, widgetId)

        when (intent.action) {
            "com.altusix.slate.ACTION_TIC_TAC_TOE_CELL" -> {
                val cellIdx = intent.getIntExtra("CELL_INDEX", -1)
                if (cellIdx in 0..8 && state.winner == 0 && state.board[cellIdx] == 0) {
                    val activeMark = if (state.isVsRobot) 1 else state.currentTurn
                    state.board[cellIdx] = activeMark
                    var winner = checkTicTacToeWinner(state.board)

                    if (winner == 0 && state.isVsRobot) {
                        val emptyIndices = state.board.indices.filter { state.board[it] == 0 }
                        if (emptyIndices.isNotEmpty()) {
                            val aiMove = emptyIndices.firstOrNull { idx ->
                                val testBoard = state.board.clone()
                                testBoard[idx] = 2
                                checkTicTacToeWinner(testBoard) == 2
                            } ?: emptyIndices.firstOrNull { idx ->
                                val testBoard = state.board.clone()
                                testBoard[idx] = 1
                                checkTicTacToeWinner(testBoard) == 1
                            } ?: if (state.board[4] == 0) 4 else emptyIndices.random()

                            state.board[aiMove] = 2
                            winner = checkTicTacToeWinner(state.board)
                        }
                    } else if (!state.isVsRobot) {
                        state = state.copy(currentTurn = if (state.currentTurn == 1) 2 else 1)
                    }

                    state = state.copy(winner = winner)
                    saveTicTacToeState(context, widgetId, state)
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateWidget(context, appWidgetManager, widgetId)
                }
            }
            "com.altusix.slate.ACTION_TIC_TAC_TOE_MODE" -> {
                val modeRobot = intent.getBooleanExtra("MODE_ROBOT", true)
                state = TicTacToeState(isVsRobot = modeRobot)
                saveTicTacToeState(context, widgetId, state)
                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateWidget(context, appWidgetManager, widgetId)
            }
            "com.altusix.slate.ACTION_TIC_TAC_TOE_RESET" -> {
                state = TicTacToeState(isVsRobot = state.isVsRobot)
                saveTicTacToeState(context, widgetId, state)
                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateWidget(context, appWidgetManager, widgetId)
            }
        }
    }

    override fun renderAndApplyWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_games_tictactoe_layout)
        val state = getTicTacToeState(context, widgetId)
        val bitmap = generateTicTacToeWidgetBitmap(context, config, isResponsive, wDp, hDp, widgetId, state)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val cellResIds = intArrayOf(R.id.cell_0, R.id.cell_1, R.id.cell_2, R.id.cell_3, R.id.cell_4, R.id.cell_5, R.id.cell_6, R.id.cell_7, R.id.cell_8)
        for (i in 0..8) {
            val cellIntent = Intent(context, GamesTicTacToeReceiver::class.java).apply {
                action = "com.altusix.slate.ACTION_TIC_TAC_TOE_CELL"
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra("CELL_INDEX", i)
            }
            val cellPendingIntent = PendingIntent.getBroadcast(context, widgetId * 100 + i, cellIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(cellResIds[i], cellPendingIntent)
        }

        val robotIntent = Intent(context, GamesTicTacToeReceiver::class.java).apply {
            action = "com.altusix.slate.ACTION_TIC_TAC_TOE_MODE"
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra("MODE_ROBOT", true)
        }
        views.setOnClickPendingIntent(R.id.btn_mode_vs_robot, PendingIntent.getBroadcast(context, widgetId * 100 + 20, robotIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

        val personIntent = Intent(context, GamesTicTacToeReceiver::class.java).apply {
            action = "com.altusix.slate.ACTION_TIC_TAC_TOE_MODE"
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra("MODE_ROBOT", false)
        }
        views.setOnClickPendingIntent(R.id.btn_mode_vs_person, PendingIntent.getBroadcast(context, widgetId * 100 + 21, personIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

        val resetIntent = Intent(context, GamesTicTacToeReceiver::class.java).apply {
            action = "com.altusix.slate.ACTION_TIC_TAC_TOE_RESET"
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        views.setOnClickPendingIntent(R.id.btn_reset, PendingIntent.getBroadcast(context, widgetId * 100 + 22, resetIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}