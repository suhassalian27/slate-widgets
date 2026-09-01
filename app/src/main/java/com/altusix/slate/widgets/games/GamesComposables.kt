package com.altusix.slate.widgets.games

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.createSupersampledCanvas
import com.altusix.slate.utils.getSafeBgColor
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private fun findWinningPattern(board: IntArray): IntArray? {
    val lines = arrayOf(
        intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8),
        intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8),
        intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)
    )
    for (line in lines) {
        if (board[line[0]] != 0 && board[line[0]] == board[line[1]] && board[line[1]] == board[line[2]]) {
            return line
        }
    }
    return null
}

private fun drawSparkleStar(canvas: android.graphics.Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
    val path = Path().apply {
        moveTo(cx, cy - size)
        quadTo(cx, cy, cx + size, cy)
        quadTo(cx, cy, cx, cy + size)
        quadTo(cx, cy, cx - size, cy)
        quadTo(cx, cy, cx, cy - size)
        close()
    }
    canvas.drawPath(path, paint)
}

// 1. TIC TAC TOE INTERACTIVE (2x2)
fun generateTicTacToeWidgetBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int,
    state: TicTacToeState = TicTacToeState()
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#50FFFFFF")

    val isAccentLight = ((Color.red(accentColorInt) * 0.2126f) + (Color.green(accentColorInt) * 0.7152f) + (Color.blue(accentColorInt) * 0.0722f)) / 255f > 0.5f
    val activeIconColor = if (isAccentLight) Color.parseColor("#1C1C1E") else Color.WHITE

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val size = minOf(w, h)
        val leftX = (w - size) / 2f
        val topY = (h - size) / 2f
        RectF(leftX, topY, leftX + size, topY + size)
    }

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val minDim = minOf(cardRect.width(), cardRect.height())
    val pad = (minDim * 0.05f).coerceIn(scaleFactor * 6f, scaleFactor * 12f)
    val gap = (minDim * 0.035f).coerceIn(scaleFactor * 3f, scaleFactor * 8f)

    // Top Header Control Bar
    val headerH = (cardRect.height() - (pad * 2f)) * 0.18f
    val headerRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.top + pad + headerH)
    val innerRadius = (cardCornerRadius - pad).coerceAtLeast(scaleFactor * 6f).coerceAtMost(headerH * 0.35f)

    val isGameOver = state.winner != 0
    val winningPattern = if (state.winner in 1..2) findWinningPattern(state.board) else null
    val isPlayerWin = state.winner == 1 || (!state.isVsRobot && state.winner in 1..2)
    val isRobotWin = state.isVsRobot && state.winner == 2
    val isDraw = state.winner == 3

    val modeW = headerRect.width() * 0.52f
    val modeRect = RectF(headerRect.left, headerRect.top, headerRect.left + modeW, headerRect.bottom)
    val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#E5E5EA") else Color.parseColor("#1C1C1E")
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(modeRect, innerRadius, innerRadius, pillBgPaint)

    val halfModeW = modeRect.width() / 2f
    val activeRect = if (state.isVsRobot) {
        RectF(modeRect.left, modeRect.top, modeRect.left + halfModeW, modeRect.bottom)
    } else {
        RectF(modeRect.left + halfModeW, modeRect.top, modeRect.right, modeRect.bottom)
    }
    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(activeRect, innerRadius, innerRadius, activePaint)

    // Robot Icon
    val cx1 = modeRect.left + (halfModeW / 2f)
    val cy1 = modeRect.centerY()
    val rColor = if (state.isVsRobot) activeIconColor else secondaryText
    val rPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = rColor; style = Paint.Style.FILL }

    val headW = modeRect.height() * 0.42f
    val headH = modeRect.height() * 0.30f
    val headRect = RectF(cx1 - headW / 2f, cy1 - headH / 2f + scaleFactor * 1.5f, cx1 + headW / 2f, cy1 + headH / 2f + scaleFactor * 1.5f)
    canvas.drawRoundRect(headRect, scaleFactor * 2.5f, scaleFactor * 2.5f, rPaint)

    val antennaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = rColor; style = Paint.Style.STROKE; strokeWidth = scaleFactor * 1.5f; strokeCap = Paint.Cap.ROUND }
    canvas.drawLine(cx1, headRect.top, cx1, headRect.top - scaleFactor * 3f, antennaPaint)
    canvas.drawCircle(cx1, headRect.top - scaleFactor * 4f, scaleFactor * 1.5f, rPaint)

    val eyeColor = if (state.isVsRobot) accentColorInt else (if (isLight) Color.parseColor("#E5E5EA") else Color.parseColor("#1C1C1E"))
    val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = eyeColor; style = Paint.Style.FILL }
    val eyeR = scaleFactor * 1.5f
    canvas.drawCircle(cx1 - headW * 0.22f, headRect.centerY(), eyeR, eyePaint)
    canvas.drawCircle(cx1 + headW * 0.22f, headRect.centerY(), eyeR, eyePaint)

    // Person Icon
    val cx2 = modeRect.left + halfModeW + (halfModeW / 2f)
    val cy2 = modeRect.centerY()
    val pColor = if (!state.isVsRobot) activeIconColor else secondaryText
    val pPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pColor; style = Paint.Style.FILL }

    val pHeadR = modeRect.height() * 0.13f
    val pHeadCy = cy2 - (modeRect.height() * 0.11f)
    canvas.drawCircle(cx2, pHeadCy, pHeadR, pPaint)

    val pBodyW = modeRect.height() * 0.38f
    val pBodyH = modeRect.height() * 0.28f
    val pBodyTop = pHeadCy + pHeadR + (scaleFactor * 1.5f)
    val pBodyRect = RectF(cx2 - pBodyW / 2f, pBodyTop, cx2 + pBodyW / 2f, pBodyTop + pBodyH)
    canvas.drawArc(pBodyRect, 180f, 180f, true, pPaint)

    // Reset Button (Glows/Highlights when game finishes)
    val resetW = headerRect.width() * 0.38f
    val resetRect = RectF(headerRect.right - resetW, headerRect.top, headerRect.right, headerRect.bottom)

    if (isGameOver) {
        val resetBgColor = if (isPlayerWin) accentColorInt else (if (isLight) Color.parseColor("#E5E5EA") else Color.parseColor("#2C2C2E"))
        val resetGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = resetBgColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(resetRect, innerRadius, innerRadius, resetGlowPaint)
    } else {
        canvas.drawRoundRect(resetRect, innerRadius, innerRadius, pillBgPaint)
    }

    val resetTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isGameOver && isPlayerWin) activeIconColor else primaryText
        textSize = resetRect.height() * 0.38f
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }
    val fm = resetTextPaint.fontMetrics
    val textY = resetRect.centerY() - (fm.ascent + fm.descent) / 2f
    canvas.drawText("RESET", resetRect.centerX(), textY, resetTextPaint)

    // 3x3 Grid
    val gridTop = headerRect.bottom + gap
    val gridRect = RectF(cardRect.left + pad, gridTop, cardRect.right - pad, cardRect.bottom - pad)

    val cellW = (gridRect.width() - (gap * 2f)) / 3f
    val cellH = (gridRect.height() - (gap * 2f)) / 3f
    val cellRadius = (cardCornerRadius - pad).coerceAtLeast(scaleFactor * 6f).coerceAtMost(minOf(cellW, cellH) * 0.22f)

    val cellCenters = Array(9) { floatArrayOf(0f, 0f) }

    val defaultCellColor = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
    val dimmedCellColor = if (isLight) Color.parseColor("#E5E5EA") else Color.parseColor("#141416")

    val baseCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val xPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 3.5f
        strokeCap = Paint.Cap.ROUND
    }

    val oPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 3.5f
    }

    for (row in 0..2) {
        for (col in 0..2) {
            val idx = row * 3 + col
            val left = gridRect.left + col * (cellW + gap)
            val top = gridRect.top + row * (cellH + gap)
            val cellBox = RectF(left, top, left + cellW, top + cellH)

            val isWinningCell = winningPattern?.contains(idx) == true

            baseCellPaint.color = when {
                winningPattern != null && isWinningCell -> {
                    if (isPlayerWin) {
                        Color.argb(45, Color.red(accentColorInt), Color.green(accentColorInt), Color.blue(accentColorInt))
                    } else {
                        Color.argb(45, 255, 69, 58)
                    }
                }
                isGameOver -> dimmedCellColor
                else -> defaultCellColor
            }

            canvas.drawRoundRect(cellBox, cellRadius, cellRadius, baseCellPaint)

            val cx = cellBox.centerX()
            val cy = cellBox.centerY()
            cellCenters[idx][0] = cx
            cellCenters[idx][1] = cy

            val symbolR = minOf(cellW, cellH) * 0.26f
            val isDimmedSymbol = isGameOver && !isWinningCell

            when (state.board[idx]) {
                1 -> {
                    xPaint.alpha = if (isDimmedSymbol) 60 else 255
                    canvas.drawLine(cx - symbolR, cy - symbolR, cx + symbolR, cy + symbolR, xPaint)
                    canvas.drawLine(cx + symbolR, cy - symbolR, cx - symbolR, cy + symbolR, xPaint)
                }
                2 -> {
                    oPaint.alpha = if (isDimmedSymbol) 60 else 255
                    canvas.drawCircle(cx, cy, symbolR, oPaint)
                }
            }
        }
    }

    // Winning Strike-Through Line
    if (winningPattern != null) {
        val startIdx = winningPattern[0]
        val endIdx = winningPattern[2]

        val startX = cellCenters[startIdx][0]
        val startY = cellCenters[startIdx][1]
        val endX = cellCenters[endIdx][0]
        val endY = cellCenters[endIdx][1]

        val angle = atan2(endY - startY, endX - startX)
        val extend = minOf(cellW, cellH) * 0.38f

        val lineStartX = startX - (cos(angle) * extend)
        val lineStartY = startY - (sin(angle) * extend)
        val lineEndX = endX + (cos(angle) * extend)
        val lineEndY = endY + (sin(angle) * extend)

        val strokeLineColor = if (isPlayerWin) accentColorInt else Color.parseColor("#FF453A")

        // Outer strike glow
        val strikeGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, Color.red(strokeLineColor), Color.green(strokeLineColor), Color.blue(strokeLineColor))
            style = Paint.Style.STROKE
            strokeWidth = scaleFactor * 9f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(lineStartX, lineStartY, lineEndX, lineEndY, strikeGlowPaint)

        // Core solid strike line
        val strikeCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strokeLineColor
            style = Paint.Style.STROKE
            strokeWidth = scaleFactor * 4.5f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(lineStartX, lineStartY, lineEndX, lineEndY, strikeCorePaint)

        // Celebration Sparkles (on Player Win)
        if (isPlayerWin) {
            val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColorInt
                style = Paint.Style.FILL
            }

            val sparkles = listOf(
                floatArrayOf(gridRect.left + cellW * 0.2f, gridRect.top - (gap * 0.6f), scaleFactor * 4f),
                floatArrayOf(gridRect.right - cellW * 0.25f, gridRect.top - (gap * 0.4f), scaleFactor * 5f),
                floatArrayOf(gridRect.left + cellW * 0.4f, gridRect.bottom + (gap * 0.5f), scaleFactor * 3.5f),
                floatArrayOf(gridRect.right - cellW * 0.5f, gridRect.bottom + (gap * 0.6f), scaleFactor * 4.5f),
                floatArrayOf(cardRect.left + pad * 1.5f, gridRect.centerY() - cellH * 0.5f, scaleFactor * 3.5f),
                floatArrayOf(cardRect.right - pad * 1.5f, gridRect.centerY() + cellH * 0.5f, scaleFactor * 4f)
            )

            for (spark in sparkles) {
                drawSparkleStar(canvas, spark[0], spark[1], spark[2], sparkPaint)
            }

            val confettiDots = listOf(
                floatArrayOf(gridRect.centerX() - cellW * 0.8f, gridRect.top + cellH * 0.2f, scaleFactor * 1.8f),
                floatArrayOf(gridRect.centerX() + cellW * 0.85f, gridRect.top + cellH * 0.35f, scaleFactor * 2.2f),
                floatArrayOf(gridRect.centerX() - cellW * 0.9f, gridRect.bottom - cellH * 0.3f, scaleFactor * 2f),
                floatArrayOf(gridRect.centerX() + cellW * 0.75f, gridRect.bottom - cellH * 0.15f, scaleFactor * 1.8f)
            )
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryText
                alpha = 180
                style = Paint.Style.FILL
            }
            for (dot in confettiDots) {
                canvas.drawCircle(dot[0], dot[1], dot[2], dotPaint)
            }
        }
    } else if (isDraw) {
        // Subtle draw indication: draw a small, balanced status dot in the header gap
        val drawDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            style = Paint.Style.FILL
        }
        val drawDotR = scaleFactor * 2f
        canvas.drawCircle(gridRect.centerX() - scaleFactor * 6f, gridTop - gap / 2f, drawDotR, drawDotPaint)
        canvas.drawCircle(gridRect.centerX(), gridTop - gap / 2f, drawDotR, drawDotPaint)
        canvas.drawCircle(gridRect.centerX() + scaleFactor * 6f, gridTop - gap / 2f, drawDotR, drawDotPaint)
    }

    return bitmap
}

data class State2048(
    val board: IntArray = IntArray(16),
    val score: Int = 0,
    val bestScore: Int = 0,
    val isGameOver: Boolean = false,
    val hasWon: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as State2048
        return board.contentEquals(other.board) && score == other.score && bestScore == other.bestScore && isGameOver == other.isGameOver && hasWon == other.hasWon
    }

    override fun hashCode(): Int = board.contentHashCode() * 31 + score.hashCode()
}

// 2. 2048 MICRO INTERACTIVE (2x2)
fun generate2048WidgetBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int,
    state: State2048 = State2048()
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#70FFFFFF")

    val isAccentLight = ((Color.red(accentColorInt) * 0.2126f) + (Color.green(accentColorInt) * 0.7152f) + (Color.blue(accentColorInt) * 0.0722f)) / 255f > 0.5f
    val onAccentTextColor = if (isAccentLight) Color.parseColor("#1C1C1E") else Color.WHITE

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val size = minOf(w, h)
        val leftX = (w - size) / 2f
        val topY = (h - size) / 2f
        RectF(leftX, topY, leftX + size, topY + size)
    }

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val minCardDim = minOf(cardRect.width(), cardRect.height())
    val pad = (minCardDim * 0.045f).coerceIn(scaleFactor * 4f, scaleFactor * 10f)
    val gap = (minCardDim * 0.022f).coerceIn(scaleFactor * 2f, scaleFactor * 5f)

    val availableH = cardRect.height() - (pad * 2f)

    // 1. TOP HEADER: SCORE, BEST & RESET
    val headerH = (availableH * 0.14f).coerceIn(scaleFactor * 20f, scaleFactor * 48f)
    val headerRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.top + pad + headerH)

    val pillW = (headerRect.width() - (gap * 2f)) / 3f
    val minPillDim = minOf(headerH, pillW)
    val pillRadius = (minPillDim * 0.32f).coerceAtLeast(scaleFactor * 4f)

    val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#E5E5EA") else Color.parseColor("#1C1C1E")
        style = Paint.Style.FILL
    }

    val scoreRect = RectF(headerRect.left, headerRect.top, headerRect.left + pillW, headerRect.bottom)
    val bestRect = RectF(scoreRect.right + gap, headerRect.top, scoreRect.right + gap + pillW, headerRect.bottom)
    val resetRect = RectF(bestRect.right + gap, headerRect.top, headerRect.right, headerRect.bottom)

    canvas.drawRoundRect(scoreRect, pillRadius, pillRadius, pillBgPaint)
    canvas.drawRoundRect(bestRect, pillRadius, pillRadius, pillBgPaint)

    // Reset button glow when game ends
    if (state.isGameOver) {
        val resetGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(resetRect, pillRadius, pillRadius, resetGlowPaint)
    } else {
        canvas.drawRoundRect(resetRect, pillRadius, pillRadius, pillBgPaint)
    }

    // Header Typography (Score is styled with Accent Color)
    val labelTextSize = (minOf(headerH * 0.22f, pillW * 0.20f)).coerceAtLeast(scaleFactor * 6f)
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = labelTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    fun calcScoreTextSize(text: String): Float {
        val byHeight = headerH * 0.38f
        val byWidth = (pillW * 0.85f) / maxOf(text.length, 2) * 1.6f
        return minOf(byHeight, byWidth).coerceAtLeast(scaleFactor * 7f)
    }

    val scoreStr = "${state.score}"
    val bestStr = "${state.bestScore}"

    val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = calcScoreTextSize(scoreStr)
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }
    val bestPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = calcScoreTextSize(bestStr)
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    canvas.drawText("SCORE", scoreRect.centerX(), scoreRect.top + headerH * 0.36f, labelPaint)
    canvas.drawText(scoreStr, scoreRect.centerX(), scoreRect.top + headerH * 0.82f, scorePaint)

    canvas.drawText("BEST", bestRect.centerX(), bestRect.top + headerH * 0.36f, labelPaint)
    canvas.drawText(bestStr, bestRect.centerX(), bestRect.top + headerH * 0.82f, bestPaint)

    // Reset button icon / text
    val resetTextSize = minOf(headerH * 0.32f, (pillW * 0.85f) / 5.2f * 1.6f)
    if (pillW < scaleFactor * 36f || resetTextSize < scaleFactor * 7.5f) {
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (state.isGameOver) onAccentTextColor else primaryText
            style = Paint.Style.STROKE
            strokeWidth = scaleFactor * 1.8f
            strokeCap = Paint.Cap.ROUND
        }
        val iconR = minPillDim * 0.22f
        val arcRect = RectF(resetRect.centerX() - iconR, resetRect.centerY() - iconR, resetRect.centerX() + iconR, resetRect.centerY() + iconR)
        canvas.drawArc(arcRect, 45f, 270f, false, iconPaint)

        val arrowPath = Path().apply {
            val ax = resetRect.centerX() + iconR * 0.7f
            val ay = resetRect.centerY() - iconR * 0.7f
            moveTo(ax - scaleFactor * 2.5f, ay)
            lineTo(ax, ay)
            lineTo(ax, ay + scaleFactor * 2.5f)
        }
        canvas.drawPath(arrowPath, iconPaint)
    } else {
        val resetTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (state.isGameOver) onAccentTextColor else primaryText
            textSize = resetTextSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }
        val fm = resetTextPaint.fontMetrics
        canvas.drawText("RESET", resetRect.centerX(), resetRect.centerY() - (fm.ascent + fm.descent) / 2f, resetTextPaint)
    }

    // 2. 4x4 TILE GRID (Themed progressively using Accent Color)
    val navH = (availableH * 0.14f).coerceIn(scaleFactor * 20f, scaleFactor * 48f)
    val gridTop = headerRect.bottom + gap
    val gridBottom = cardRect.bottom - pad - navH - gap
    val gridRect = RectF(cardRect.left + pad, gridTop, cardRect.right - pad, gridBottom)

    val cellW = (gridRect.width() - (gap * 3f)) / 4f
    val cellH = (gridRect.height() - (gap * 3f)) / 4f
    val minCellDim = minOf(cellW, cellH)
    val cellRadius = (minCellDim * 0.22f).coerceAtLeast(scaleFactor * 4f)

    val cellBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val tileTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    for (row in 0..3) {
        for (col in 0..3) {
            val idx = row * 4 + col
            val valNum = state.board[idx]
            val left = gridRect.left + col * (cellW + gap)
            val top = gridRect.top + row * (cellH + gap)
            val cellBox = RectF(left, top, left + cellW, top + cellH)

            cellBgPaint.color = get2048CellColor(valNum, isLight, accentColorInt)
            canvas.drawRoundRect(cellBox, cellRadius, cellRadius, cellBgPaint)

            if (valNum > 0) {
                val textColor = get2048TextColor(valNum, isLight, onAccentTextColor)
                tileTextPaint.color = textColor
                tileTextPaint.textSize = when {
                    valNum >= 1024 -> minCellDim * 0.30f
                    valNum >= 128 -> minCellDim * 0.36f
                    valNum >= 16 -> minCellDim * 0.42f
                    else -> minCellDim * 0.48f
                }
                val tfm = tileTextPaint.fontMetrics
                val textY = cellBox.centerY() - (tfm.ascent + tfm.descent) / 2f
                canvas.drawText("$valNum", cellBox.centerX(), textY, tileTextPaint)
            }
        }
    }

    // 3. BOTTOM DIRECTIONAL NAVIGATION (Interactive Accent-tinted Arrow buttons)
    val navRect = RectF(cardRect.left + pad, cardRect.bottom - pad - navH, cardRect.right - pad, cardRect.bottom - pad)
    val navSlotW = (navRect.width() - (gap * 3f)) / 4f
    val minNavDim = minOf(navH, navSlotW)
    val navRadius = (minNavDim * 0.32f).coerceAtLeast(scaleFactor * 4f)

    val navPillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(35, Color.red(accentColorInt), Color.green(accentColorInt), Color.blue(accentColorInt))
        style = Paint.Style.FILL
    }

    val arrowSymbols = arrayOf("←", "↑", "↓", "→")
    val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = (minNavDim * 0.52f).coerceAtLeast(scaleFactor * 8f)
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    for (i in 0..3) {
        val slotLeft = navRect.left + i * (navSlotW + gap)
        val slotBox = RectF(slotLeft, navRect.top, slotLeft + navSlotW, navRect.bottom)
        canvas.drawRoundRect(slotBox, navRadius, navRadius, navPillBg)

        val afm = arrowPaint.fontMetrics
        val aY = slotBox.centerY() - (afm.ascent + afm.descent) / 2f
        canvas.drawText(arrowSymbols[i], slotBox.centerX(), aY, arrowPaint)
    }

    return bitmap
}

// Progressive tile background colors derived from the user's Accent Color
private fun get2048CellColor(value: Int, isLight: Boolean, accentColor: Int): Int {
    if (value == 0) return if (isLight) Color.parseColor("#E5E5EA") else Color.parseColor("#141416")

    val r = Color.red(accentColor)
    val g = Color.green(accentColor)
    val b = Color.blue(accentColor)

    return when (value) {
        2 -> if (isLight) Color.argb(35, r, g, b) else Color.argb(45, r, g, b)
        4 -> if (isLight) Color.argb(65, r, g, b) else Color.argb(75, r, g, b)
        8 -> if (isLight) Color.argb(95, r, g, b) else Color.argb(105, r, g, b)
        16 -> if (isLight) Color.argb(125, r, g, b) else Color.argb(135, r, g, b)
        32 -> if (isLight) Color.argb(155, r, g, b) else Color.argb(165, r, g, b)
        64 -> if (isLight) Color.argb(185, r, g, b) else Color.argb(195, r, g, b)
        128 -> Color.argb(220, r, g, b)
        256, 512, 1024, 2048 -> accentColor
        else -> accentColor
    }
}

private fun get2048TextColor(value: Int, isLight: Boolean, onAccentText: Int): Int {
    return when {
        value <= 4 -> if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
        value <= 64 -> Color.WHITE
        else -> onAccentText
    }
}
