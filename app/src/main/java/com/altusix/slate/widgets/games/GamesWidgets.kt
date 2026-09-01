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
import com.altusix.slate.core.theme.ThemePreferences
import com.altusix.slate.data.local.SlateWidgetConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

private fun loadSlateWidgetConfig(context: Context, widgetId: Int): SlateWidgetConfig {
    val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val bgKey = "widget_${widgetId}_bg_color"

    // Snapshot and lock current global theme when the widget is first created
    if (!widgetPrefs.contains(bgKey) && widgetId != -1) {
        val globalSettings = ThemePreferences(context).getThemeSettings()
        widgetPrefs.edit()
            .putLong("widget_${widgetId}_bg_color", globalSettings.bgHex)
            .putLong("widget_${widgetId}_accent_color", globalSettings.accentHex)
            .putFloat("widget_${widgetId}_opacity", globalSettings.opacity)
            .apply()
    }

    val globalSettings = ThemePreferences(context).getThemeSettings()
    val bgColor = widgetPrefs.getLong("widget_${widgetId}_bg_color", globalSettings.bgHex)
    val opacity = widgetPrefs.getFloat("widget_${widgetId}_opacity", globalSettings.opacity)
    val accentColor = widgetPrefs.getLong("widget_${widgetId}_accent_color", globalSettings.accentHex)

    val isLight = (((bgColor shr 16 and 0xFFL) * 0.2126f) + ((bgColor shr 8 and 0xFFL) * 0.7152f) + ((bgColor and 0xFFL) * 0.0722f)) / 255f > 0.5f

    return SlateWidgetConfig(
        themeMode = if (isLight) "LIGHT" else "DARK",
        backgroundColorHex = bgColor,
        opacity = opacity,
        accentColorHex = accentColor
    )
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
        SlateWidgetInfo(name = "Tic Tac Toe", category = "Games", sizeText = "2x2", receiverClass = GamesTicTacToeReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "2048 Micro", category = "Games", sizeText = "2x2", receiverClass = Games2048Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Rock Paper Scissors", category = "Games", sizeText = "2x2", receiverClass = GamesRpsReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Dice Roller", category = "Games", sizeText = "2x2", receiverClass = GamesDiceReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Coin Flip", category = "Games", sizeText = "2x2", receiverClass = GamesCoinFlipReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Simon Sequence", category = "Games", sizeText = "2x2", receiverClass = GamesSimonReceiver::class.java, hasModeOption = true)
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


// --- 2048 GAME ENGINE ---
private fun get2048State(context: Context, widgetId: Int): State2048 {
    val prefs = context.getSharedPreferences("slate_2048_prefs", Context.MODE_PRIVATE)
    val boardStr = prefs.getString("widget_${widgetId}_board", null)
    val score = prefs.getInt("widget_${widgetId}_score", 0)
    val bestScore = prefs.getInt("widget_${widgetId}_best_score", 0)

    if (boardStr == null) {
        val initialBoard = IntArray(16)
        spawnRandomTile(initialBoard)
        spawnRandomTile(initialBoard)
        return State2048(board = initialBoard, score = 0, bestScore = bestScore)
    }

    val board = boardStr.split(",").map { it.trim().toIntOrNull() ?: 0 }.toIntArray()
    val isGameOver = is2048GameOver(board)
    val hasWon = board.any { it >= 2048 }

    return State2048(board, score, bestScore, isGameOver, hasWon)
}

private fun save2048State(context: Context, widgetId: Int, state: State2048) {
    val prefs = context.getSharedPreferences("slate_2048_prefs", Context.MODE_PRIVATE)
    val boardStr = state.board.joinToString(",")
    val newBest = maxOf(state.score, state.bestScore)
    prefs.edit()
        .putString("widget_${widgetId}_board", boardStr)
        .putInt("widget_${widgetId}_score", state.score)
        .putInt("widget_${widgetId}_best_score", newBest)
        .apply()
}

private fun spawnRandomTile(board: IntArray) {
    val emptyIndices = board.indices.filter { board[it] == 0 }
    if (emptyIndices.isNotEmpty()) {
        val target = emptyIndices.random()
        board[target] = if (Math.random() < 0.9) 2 else 4
    }
}

private fun is2048GameOver(board: IntArray): Boolean {
    if (board.any { it == 0 }) return false
    for (r in 0..3) {
        for (c in 0..3) {
            val v = board[r * 4 + c]
            if (c < 3 && v == board[r * 4 + (c + 1)]) return false
            if (r < 3 && v == board[(r + 1) * 4 + c]) return false
        }
    }
    return true
}

private fun slideAndMergeRow(row: IntArray): Pair<IntArray, Int> {
    val nonZero = row.filter { it != 0 }.toMutableList()
    var addedScore = 0
    val merged = ArrayList<Int>()

    var i = 0
    while (i < nonZero.size) {
        if (i + 1 < nonZero.size && nonZero[i] == nonZero[i + 1]) {
            val newVal = nonZero[i] * 2
            merged.add(newVal)
            addedScore += newVal
            i += 2
        } else {
            merged.add(nonZero[i])
            i += 1
        }
    }

    while (merged.size < 4) merged.add(0)
    return Pair(merged.toIntArray(), addedScore)
}

private fun process2048Move(currentState: State2048, direction: String): State2048 {
    val board = currentState.board.clone()
    var totalAddedScore = 0
    var changed = false

    when (direction) {
        "LEFT" -> {
            for (r in 0..3) {
                val row = intArrayOf(board[r * 4], board[r * 4 + 1], board[r * 4 + 2], board[r * 4 + 3])
                val (newRow, pts) = slideAndMergeRow(row)
                totalAddedScore += pts
                for (c in 0..3) {
                    if (board[r * 4 + c] != newRow[c]) changed = true
                    board[r * 4 + c] = newRow[c]
                }
            }
        }
        "RIGHT" -> {
            for (r in 0..3) {
                val row = intArrayOf(board[r * 4 + 3], board[r * 4 + 2], board[r * 4 + 1], board[r * 4])
                val (newRow, pts) = slideAndMergeRow(row)
                totalAddedScore += pts
                for (c in 0..3) {
                    if (board[r * 4 + (3 - c)] != newRow[c]) changed = true
                    board[r * 4 + (3 - c)] = newRow[c]
                }
            }
        }
        "UP" -> {
            for (c in 0..3) {
                val col = intArrayOf(board[c], board[4 + c], board[8 + c], board[12 + c])
                val (newCol, pts) = slideAndMergeRow(col)
                totalAddedScore += pts
                for (r in 0..3) {
                    if (board[r * 4 + c] != newCol[r]) changed = true
                    board[r * 4 + c] = newCol[r]
                }
            }
        }
        "DOWN" -> {
            for (c in 0..3) {
                val col = intArrayOf(board[12 + c], board[8 + c], board[4 + c], board[c])
                val (newCol, pts) = slideAndMergeRow(col)
                totalAddedScore += pts
                for (r in 0..3) {
                    if (board[(3 - r) * 4 + c] != newCol[r]) changed = true
                    board[(3 - r) * 4 + c] = newCol[r]
                }
            }
        }
    }

    if (changed) {
        spawnRandomTile(board)
    }

    val newScore = currentState.score + totalAddedScore
    val bestScore = maxOf(newScore, currentState.bestScore)
    val isGameOver = is2048GameOver(board)
    val hasWon = board.any { it >= 2048 }

    return State2048(board, newScore, bestScore, isGameOver, hasWon)
}

// 2. 2048 MICRO INTERACTIVE (2x2)
class Games2048Receiver : BaseGamesReceiver() {

    fun renderWidgetBitmap(context: Context, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val previewBoard = intArrayOf(
            2, 4, 8, 16,
            32, 64, 128, 256,
            512, 1024, 2048, 0,
            2, 4, 0, 0
        )
        return generate2048WidgetBitmap(context, config, false, wDp, hDp, -1, State2048(previewBoard, 3840, 5120))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        when (intent.action) {
            "com.altusix.slate.ACTION_2048_MOVE" -> {
                val dir = intent.getStringExtra("MOVE_DIRECTION") ?: return
                var state = get2048State(context, widgetId)
                if (!state.isGameOver) {
                    state = process2048Move(state, dir)
                    save2048State(context, widgetId, state)
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateWidget(context, appWidgetManager, widgetId)
                }
            }
            "com.altusix.slate.ACTION_2048_RESET" -> {
                val current = get2048State(context, widgetId)
                val newBoard = IntArray(16)
                spawnRandomTile(newBoard)
                spawnRandomTile(newBoard)
                val freshState = State2048(newBoard, 0, current.bestScore)
                save2048State(context, widgetId, freshState)
                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateWidget(context, appWidgetManager, widgetId)
            }
        }
    }

    override fun renderAndApplyWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_games_2048_layout)
        val state = get2048State(context, widgetId)
        val bitmap = generate2048WidgetBitmap(context, config, isResponsive, wDp, hDp, widgetId, state)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val directions = mapOf(
            R.id.btn_left to "LEFT",
            R.id.btn_up to "UP",
            R.id.btn_down to "DOWN",
            R.id.btn_right to "RIGHT"
        )

        for ((btnId, dir) in directions) {
            val moveIntent = Intent(context, Games2048Receiver::class.java).apply {
                action = "com.altusix.slate.ACTION_2048_MOVE"
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra("MOVE_DIRECTION", dir)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                widgetId * 1000 + btnId,
                moveIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(btnId, pendingIntent)
        }

        val resetIntent = Intent(context, Games2048Receiver::class.java).apply {
            action = "com.altusix.slate.ACTION_2048_RESET"
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        views.setOnClickPendingIntent(
            R.id.btn_reset,
            PendingIntent.getBroadcast(
                context,
                widgetId * 1000 + R.id.btn_reset,
                resetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}

// --- ROCK PAPER SCISSORS GAME ENGINE ---
private fun getRpsState(context: Context, widgetId: Int): RpsState {
    val prefs = context.getSharedPreferences("slate_rps_prefs", Context.MODE_PRIVATE)
    return RpsState(
        playerMove = prefs.getInt("widget_${widgetId}_p_move", 0),
        botMove = prefs.getInt("widget_${widgetId}_b_move", 0),
        result = prefs.getInt("widget_${widgetId}_result", 0),
        playerWins = prefs.getInt("widget_${widgetId}_p_wins", 0),
        botWins = prefs.getInt("widget_${widgetId}_b_wins", 0),
        streak = prefs.getInt("widget_${widgetId}_streak", 0)
    )
}

private fun saveRpsState(context: Context, widgetId: Int, state: RpsState) {
    val prefs = context.getSharedPreferences("slate_rps_prefs", Context.MODE_PRIVATE)
    prefs.edit()
        .putInt("widget_${widgetId}_p_move", state.playerMove)
        .putInt("widget_${widgetId}_b_move", state.botMove)
        .putInt("widget_${widgetId}_result", state.result)
        .putInt("widget_${widgetId}_p_wins", state.playerWins)
        .putInt("widget_${widgetId}_b_wins", state.botWins)
        .putInt("widget_${widgetId}_streak", state.streak)
        .apply()
}

// 3. ROCK PAPER SCISSORS INTERACTIVE (2x2)
class GamesRpsReceiver : BaseGamesReceiver() {

    fun renderWidgetBitmap(context: Context, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        return generateRpsWidgetBitmap(context, config, false, wDp, hDp, -1, RpsState(1, 3, 1, 8, 4, 3))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        when (intent.action) {
            "com.altusix.slate.ACTION_RPS_CHOICE" -> {
                val playerChoice = intent.getIntExtra("EXTRA_CHOICE", 0)
                if (playerChoice in 1..3) {
                    val current = getRpsState(context, widgetId)
                    val botChoice = (1..3).random()

                    // Result: 1: Player Win, 2: Bot Win, 3: Draw
                    val result = when {
                        playerChoice == botChoice -> 3
                        (playerChoice == 1 && botChoice == 3) ||
                                (playerChoice == 2 && botChoice == 1) ||
                                (playerChoice == 3 && botChoice == 2) -> 1
                        else -> 2
                    }

                    val newPWins = if (result == 1) current.playerWins + 1 else current.playerWins
                    val newBWins = if (result == 2) current.botWins + 1 else current.botWins
                    val newStreak = when (result) {
                        1 -> current.streak + 1
                        2 -> 0
                        else -> current.streak
                    }

                    val updatedState = RpsState(
                        playerMove = playerChoice,
                        botMove = botChoice,
                        result = result,
                        playerWins = newPWins,
                        botWins = newBWins,
                        streak = newStreak
                    )

                    saveRpsState(context, widgetId, updatedState)
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateWidget(context, appWidgetManager, widgetId)
                }
            }
            "com.altusix.slate.ACTION_RPS_RESET" -> {
                val resetState = RpsState()
                saveRpsState(context, widgetId, resetState)
                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateWidget(context, appWidgetManager, widgetId)
            }
        }
    }

    override fun renderAndApplyWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_games_rps_layout)
        val state = getRpsState(context, widgetId)
        val bitmap = generateRpsWidgetBitmap(context, config, isResponsive, wDp, hDp, widgetId, state)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val actions = mapOf(
            R.id.btn_rock to 1,
            R.id.btn_paper to 2,
            R.id.btn_scissors to 3
        )

        for ((btnId, choice) in actions) {
            val choiceIntent = Intent(context, GamesRpsReceiver::class.java).apply {
                action = "com.altusix.slate.ACTION_RPS_CHOICE"
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra("EXTRA_CHOICE", choice)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                widgetId * 1000 + btnId,
                choiceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(btnId, pendingIntent)
        }

        val resetIntent = Intent(context, GamesRpsReceiver::class.java).apply {
            action = "com.altusix.slate.ACTION_RPS_RESET"
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        views.setOnClickPendingIntent(
            R.id.btn_reset,
            PendingIntent.getBroadcast(
                context,
                widgetId * 1000 + R.id.btn_reset,
                resetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}


// --- DICE GAME ENGINE & PERSISTENCE ---
private fun getDiceState(context: Context, widgetId: Int): DiceState {
    val prefs = context.getSharedPreferences("slate_dice_prefs", Context.MODE_PRIVATE)
    val roll = prefs.getInt("widget_${widgetId}_roll", 6)
    val rot = prefs.getFloat("widget_${widgetId}_rot", 0f)
    return DiceState(currentRoll = roll, rotationAngle = rot, scale = 1.0f, offsetX = 0f, offsetY = 0f)
}

private fun saveDiceState(context: Context, widgetId: Int, state: DiceState) {
    val prefs = context.getSharedPreferences("slate_dice_prefs", Context.MODE_PRIVATE)
    prefs.edit()
        .putInt("widget_${widgetId}_roll", state.currentRoll)
        .putFloat("widget_${widgetId}_rot", state.rotationAngle)
        .apply()
}

// 4. DICE ROLLER INTERACTIVE (2x2)
class GamesDiceReceiver : BaseGamesReceiver() {

    fun renderWidgetBitmap(context: Context, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        return generateDiceWidgetBitmap(context, config, false, wDp, hDp, -1, DiceState(5, -4f, 1.0f))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        if (intent.action == "com.altusix.slate.ACTION_DICE_ROLL") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val options = appWidgetManager.getAppWidgetOptions(widgetId)
                    val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                    val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) ?: 200
                    val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200) ?: 200
                    val wDp = if (wDpRaw <= 0) 200 else wDpRaw
                    val hDp = if (hDpRaw <= 0) 200 else hDpRaw

                    val isResponsive = parseAndLockIsResponsive(context, widgetId)
                    val config = loadSlateWidgetConfig(context, widgetId)

                    val current = getDiceState(context, widgetId)
                    val finalRoll = (1..6).random()
                    val finalAngle = ((-4..4).random()).toFloat()
                    val spinDirection = if (Math.random() < 0.5) 1f else -1f
                    val totalSpinDegrees = (360f * 1.5f * spinDirection) + finalAngle

                    val rollSequence = intArrayOf(
                        (1..6).random(), (1..6).random(), (1..6).random(),
                        (1..6).random(), (1..6).random(), (1..6).random(),
                        (1..6).random(), (1..6).random(), (1..6).random(),
                        finalRoll, finalRoll, finalRoll, finalRoll, finalRoll, finalRoll, finalRoll
                    )

                    val totalFrames = 16

                    for (frame in 0 until totalFrames) {
                        val t = frame.toFloat() / (totalFrames - 1).toFloat()
                        val isFinal = frame == totalFrames - 1

                        // 1. Continuous Ease-Out Spin
                        val easeOutProgress = 1f - (1f - t).pow(2.4f)
                        val angle = if (isFinal) finalAngle else (current.rotationAngle + (totalSpinDegrees * easeOutProgress))

                        // 2. Controlled Bounce & Sway within safe inner limits
                        val decay = (1f - t).pow(1.8f)
                        val offsetY = if (isFinal) 0f else (-10f * sin(t * PI.toFloat() * 3.5f).coerceAtLeast(0f) * decay)
                        val offsetX = if (isFinal) 0f else (6f * cos(t * PI.toFloat() * 2.5f) * decay * spinDirection)

                        // 3. Subtle Elastic Scale
                        val scale = if (isFinal) 1.0f else (1.0f + (0.05f * sin(t * PI.toFloat() * 5f) * decay))

                        val faceValue = if (isFinal || t > 0.65f) finalRoll else rollSequence[frame]

                        val frameState = DiceState(
                            currentRoll = faceValue,
                            rotationAngle = angle,
                            scale = scale,
                            offsetX = offsetX,
                            offsetY = offsetY
                        )

                        if (isFinal) {
                            saveDiceState(context, widgetId, frameState)
                        }

                        val bitmap = generateDiceWidgetBitmap(context, config, isResponsive, wDp, hDp, widgetId, frameState)
                        val views = RemoteViews(context.packageName, R.layout.widget_canvas_container)
                        views.setImageViewBitmap(R.id.widget_canvas_image, bitmap)

                        val rollIntent = Intent(context, GamesDiceReceiver::class.java).apply {
                            action = "com.altusix.slate.ACTION_DICE_ROLL"
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            widgetId,
                            rollIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_canvas_image, pendingIntent)

                        appWidgetManager.updateAppWidget(widgetId, views)

                        if (!isFinal) {
                            val frameDelay = (45L + (35L * t.pow(1.5f)).toLong())
                            delay(frameDelay)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    override fun renderAndApplyWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_canvas_container)
        val state = getDiceState(context, widgetId)
        val bitmap = generateDiceWidgetBitmap(context, config, isResponsive, wDp, hDp, widgetId, state)
        views.setImageViewBitmap(R.id.widget_canvas_image, bitmap)

        val rollIntent = Intent(context, GamesDiceReceiver::class.java).apply {
            action = "com.altusix.slate.ACTION_DICE_ROLL"
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            widgetId,
            rollIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_canvas_image, pendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}


// --- COIN FLIP GAME ENGINE & PERSISTENCE ---
private fun getCoinFlipState(context: Context, widgetId: Int): CoinFlipState {
    val prefs = context.getSharedPreferences("slate_coin_prefs", Context.MODE_PRIVATE)
    val isHeads = prefs.getBoolean("widget_${widgetId}_is_heads", true)
    return CoinFlipState(isHeads = isHeads, flipAngleDeg = 0f, scale = 1.0f, offsetY = 0f)
}

private fun saveCoinFlipState(context: Context, widgetId: Int, state: CoinFlipState) {
    val prefs = context.getSharedPreferences("slate_coin_prefs", Context.MODE_PRIVATE)
    prefs.edit()
        .putBoolean("widget_${widgetId}_is_heads", state.isHeads)
        .apply()
}

// 5. COIN FLIP INTERACTIVE (2x2)
class GamesCoinFlipReceiver : BaseGamesReceiver() {

    fun renderWidgetBitmap(context: Context, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        return generateCoinFlipWidgetBitmap(context, config, false, wDp, hDp, -1, CoinFlipState(isHeads = true, flipAngleDeg = 0f, scale = 1.0f))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        if (intent.action == "com.altusix.slate.ACTION_COIN_FLIP") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val options = appWidgetManager.getAppWidgetOptions(widgetId)
                    val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                    val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) ?: 200
                    val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200) ?: 200
                    val wDp = if (wDpRaw <= 0) 200 else wDpRaw
                    val hDp = if (hDpRaw <= 0) 200 else hDpRaw

                    val isResponsive = parseAndLockIsResponsive(context, widgetId)
                    val config = loadSlateWidgetConfig(context, widgetId)

                    val currentState = getCoinFlipState(context, widgetId)
                    val finalIsHeads = kotlin.random.Random.nextBoolean()

                    // 5 full 360° revolutions for extended airtime
                    val extraTurns = if (currentState.isHeads == finalIsHeads) 0f else 180f
                    val totalFlipDegrees = (360f * 5f) + extraTurns

                    // 26 frames spanning ~1.8s total duration
                    val totalFrames = 26

                    for (frame in 0 until totalFrames) {
                        val t = frame.toFloat() / (totalFrames - 1).toFloat()
                        val isFinal = frame == totalFrames - 1

                        // 1. Smooth Decelerating Spin Curve
                        val easeProgress = 1f - (1f - t).pow(2.2f)
                        val angle = if (isFinal) 0f else (totalFlipDegrees * easeProgress)

                        // 2. High Parabolic Toss Arc
                        val decay = (1f - t).pow(1.3f)
                        val offsetY = if (isFinal) 0f else (-22f * sin(t * Math.PI.toFloat()).coerceAtLeast(0f) * decay)

                        // 3. Elevation Scale Pop
                        val scale = if (isFinal) 1.0f else (1.0f + (0.10f * sin(t * Math.PI.toFloat()) * decay))

                        val frameState = CoinFlipState(
                            isHeads = if (isFinal) finalIsHeads else currentState.isHeads,
                            flipAngleDeg = angle,
                            scale = scale,
                            offsetY = offsetY
                        )

                        if (isFinal) {
                            saveCoinFlipState(context, widgetId, frameState)
                        }

                        val bitmap = generateCoinFlipWidgetBitmap(context, config, isResponsive, wDp, hDp, widgetId, frameState)
                        val views = RemoteViews(context.packageName, R.layout.widget_canvas_container)
                        views.setImageViewBitmap(R.id.widget_canvas_image, bitmap)

                        val flipIntent = Intent(context, GamesCoinFlipReceiver::class.java).apply {
                            action = "com.altusix.slate.ACTION_COIN_FLIP"
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            widgetId,
                            flipIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_canvas_image, pendingIntent)

                        appWidgetManager.updateAppWidget(widgetId, views)

                        if (!isFinal) {
                            // Eased frame delay scaling from 48ms up to 105ms at landing
                            val frameDelay = (48L + (58L * t.pow(1.6f)).toLong())
                            delay(frameDelay)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    override fun renderAndApplyWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_canvas_container)
        val state = getCoinFlipState(context, widgetId)
        val bitmap = generateCoinFlipWidgetBitmap(context, config, isResponsive, wDp, hDp, widgetId, state)
        views.setImageViewBitmap(R.id.widget_canvas_image, bitmap)

        val flipIntent = Intent(context, GamesCoinFlipReceiver::class.java).apply {
            action = "com.altusix.slate.ACTION_COIN_FLIP"
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            widgetId,
            flipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_canvas_image, pendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}


// --- SIMON MEMORY ENGINE & PERSISTENCE ---
private fun getSimonState(context: Context, widgetId: Int): SimonState {
    val prefs = context.getSharedPreferences("slate_simon_prefs", Context.MODE_PRIVATE)
    val seqStr = prefs.getString("widget_${widgetId}_seq", "") ?: ""
    val sequence = if (seqStr.isEmpty()) emptyList() else seqStr.split(",").mapNotNull { it.trim().toIntOrNull() }
    val step = prefs.getInt("widget_${widgetId}_step", 0)
    val level = prefs.getInt("widget_${widgetId}_level", 0)
    val best = prefs.getInt("widget_${widgetId}_best", 0)
    val status = prefs.getInt("widget_${widgetId}_status", 0)

    return SimonState(sequence, step, -1, level, best, status)
}

private fun saveSimonState(context: Context, widgetId: Int, state: SimonState) {
    val prefs = context.getSharedPreferences("slate_simon_prefs", Context.MODE_PRIVATE)
    val seqStr = state.sequence.joinToString(",")
    val newBest = maxOf(state.level, state.bestLevel)
    prefs.edit()
        .putString("widget_${widgetId}_seq", seqStr)
        .putInt("widget_${widgetId}_step", state.playerStep)
        .putInt("widget_${widgetId}_level", state.level)
        .putInt("widget_${widgetId}_best", newBest)
        .putInt("widget_${widgetId}_status", state.status)
        .apply()
}

// 6. SIMON MEMORY SEQUENCE INTERACTIVE (2x2)
class GamesSimonReceiver : BaseGamesReceiver() {

    fun renderWidgetBitmap(context: Context, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        return generateSimonWidgetBitmap(context, config, false, wDp, hDp, -1, SimonState(listOf(0, 2, 1), 1, 0, 3, 7, 2))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        when (intent.action) {
            "com.altusix.slate.ACTION_SIMON_START_RESET" -> {
                val current = getSimonState(context, widgetId)
                val firstPad = (0..3).random()
                val newSeq = listOf(firstPad)
                val startState = SimonState(
                    sequence = newSeq,
                    playerStep = 0,
                    activeFlashPad = -1,
                    level = 1,
                    bestLevel = current.bestLevel,
                    status = 1 // WATCHING
                )
                saveSimonState(context, widgetId, startState)
                playSequence(context, widgetId, newSeq, 1, current.bestLevel)
            }
            "com.altusix.slate.ACTION_SIMON_PAD" -> {
                val padIndex = intent.getIntExtra("PAD_INDEX", -1)
                val current = getSimonState(context, widgetId)

                // Only accept input strictly during YOUR TURN (status == 2)
                if (padIndex in 0..3 && current.status == 2 && current.sequence.isNotEmpty()) {
                    val expected = current.sequence.getOrNull(current.playerStep)
                    if (expected == padIndex) {
                        val nextStep = current.playerStep + 1
                        if (nextStep == current.sequence.size) {
                            // Correct final step: Light up the pad, show SUCCESS, then transition
                            val nextPad = (0..3).random()
                            val nextSeq = current.sequence + nextPad
                            val nextLevel = current.level + 1
                            val newBest = maxOf(nextLevel, current.bestLevel)

                            handleLastInputSuccess(context, widgetId, padIndex, nextSeq, nextLevel, newBest)
                        } else {
                            // Intermediate step: Light up pad briefly and advance step
                            val advanceState = current.copy(playerStep = nextStep, activeFlashPad = padIndex)
                            saveSimonState(context, widgetId, advanceState)
                            flashIntermediatePad(context, widgetId, advanceState)
                        }
                    } else {
                        // Mistake: Lock input and trigger GAME OVER
                        val overState = current.copy(status = 4, activeFlashPad = -1)
                        saveSimonState(context, widgetId, overState)
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        updateWidget(context, appWidgetManager, widgetId)
                    }
                }
            }
        }
    }

    private fun flashIntermediatePad(context: Context, widgetId: Int, state: SimonState) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val (wDp, hDp) = getDimensions(context, appWidgetManager, widgetId)
                val isResponsive = parseAndLockIsResponsive(context, widgetId)
                val config = loadSlateWidgetConfig(context, widgetId)

                // Flash pad ON
                renderViews(context, appWidgetManager, widgetId, config, isResponsive, wDp, hDp, state)
                delay(150)

                // Flash pad OFF (Stay in YOUR TURN)
                val offState = state.copy(activeFlashPad = -1)
                saveSimonState(context, widgetId, offState)
                renderViews(context, appWidgetManager, widgetId, config, isResponsive, wDp, hDp, offState)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleLastInputSuccess(
        context: Context,
        widgetId: Int,
        lastPadIndex: Int,
        nextSeq: List<Int>,
        nextLevel: Int,
        newBest: Int
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val (wDp, hDp) = getDimensions(context, appWidgetManager, widgetId)
                val isResponsive = parseAndLockIsResponsive(context, widgetId)
                val config = loadSlateWidgetConfig(context, widgetId)

                // 1. Light up the final tapped pad
                val litState = SimonState(nextSeq, 0, lastPadIndex, nextLevel - 1, newBest, 2)
                renderViews(context, appWidgetManager, widgetId, config, isResponsive, wDp, hDp, litState)
                delay(180)

                // 2. Turn off pad and show NICE! success banner
                val successState = SimonState(nextSeq, 0, -1, nextLevel, newBest, 3)
                renderViews(context, appWidgetManager, widgetId, config, isResponsive, wDp, hDp, successState)
                delay(550)

                // 3. Play next round sequence
                saveSimonState(context, widgetId, successState.copy(status = 1))
                playSequenceInternal(context, appWidgetManager, widgetId, config, isResponsive, wDp, hDp, nextSeq, nextLevel, newBest)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun playSequence(context: Context, widgetId: Int, sequence: List<Int>, level: Int, bestLevel: Int) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val (wDp, hDp) = getDimensions(context, appWidgetManager, widgetId)
                val isResponsive = parseAndLockIsResponsive(context, widgetId)
                val config = loadSlateWidgetConfig(context, widgetId)

                delay(350)
                playSequenceInternal(context, appWidgetManager, widgetId, config, isResponsive, wDp, hDp, sequence, level, bestLevel)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun playSequenceInternal(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int,
        sequence: List<Int>,
        level: Int,
        bestLevel: Int
    ) {
        // Play through each step in sequence with clear flash and pause
        for (pad in sequence) {
            val litState = SimonState(sequence, 0, pad, level, bestLevel, 1)
            renderViews(context, appWidgetManager, widgetId, config, isResponsive, wDp, hDp, litState)
            delay(280)

            val unlitState = SimonState(sequence, 0, -1, level, bestLevel, 1)
            renderViews(context, appWidgetManager, widgetId, config, isResponsive, wDp, hDp, unlitState)
            delay(130)
        }

        delay(120)

        // Hand control to the player (YOUR TURN)
        val userTurnState = SimonState(sequence, 0, -1, level, bestLevel, 2)
        saveSimonState(context, widgetId, userTurnState)
        renderViews(context, appWidgetManager, widgetId, config, isResponsive, wDp, hDp, userTurnState)
    }

    private fun getDimensions(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int): Pair<Int, Int> {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) ?: 200
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200) ?: 200
        val wDp = if (wDpRaw <= 0) 200 else wDpRaw
        val hDp = if (hDpRaw <= 0) 200 else hDpRaw
        return Pair(wDp, hDp)
    }

    private fun renderViews(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int,
        state: SimonState
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_games_simon_layout)
        val bitmap = generateSimonWidgetBitmap(context, config, isResponsive, wDp, hDp, widgetId, state)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val pads = intArrayOf(R.id.btn_pad_0, R.id.btn_pad_1, R.id.btn_pad_2, R.id.btn_pad_3)
        for (i in 0..3) {
            val padIntent = Intent(context, GamesSimonReceiver::class.java).apply {
                action = "com.altusix.slate.ACTION_SIMON_PAD"
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra("PAD_INDEX", i)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                widgetId * 1000 + pads[i],
                padIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(pads[i], pendingIntent)
        }

        val resetIntent = Intent(context, GamesSimonReceiver::class.java).apply {
            action = "com.altusix.slate.ACTION_SIMON_START_RESET"
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        views.setOnClickPendingIntent(
            R.id.btn_reset,
            PendingIntent.getBroadcast(
                context,
                widgetId * 1000 + R.id.btn_reset,
                resetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    override fun renderAndApplyWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ) {
        val state = getSimonState(context, widgetId)
        renderViews(context, appWidgetManager, widgetId, config, isResponsive, wDp, hDp, state)
    }
}
