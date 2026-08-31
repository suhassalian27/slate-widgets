package com.altusix.slate.widgets.games

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.createSupersampledCanvas
import com.altusix.slate.utils.getSafeBgColor
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius

// 1. TIC TAC TOE INTERACTIVE (2x2)
fun generateTicTacToeWidgetBitmap(
    context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int, state: TicTacToeState = TicTacToeState()): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#50FFFFFF")

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

    // Sleek Robot Icon (Left Slot)
    val cx1 = modeRect.left + (halfModeW / 2f)
    val cy1 = modeRect.centerY()
    val rColor = if (state.isVsRobot) Color.BLACK else secondaryText
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

    // Clean Material Person Icon (Right Slot)
    val cx2 = modeRect.left + halfModeW + (halfModeW / 2f)
    val cy2 = modeRect.centerY()
    val pColor = if (!state.isVsRobot) Color.BLACK else secondaryText
    val pPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pColor; style = Paint.Style.FILL }

    val pHeadR = modeRect.height() * 0.13f
    val pHeadCy = cy2 - (modeRect.height() * 0.11f)
    canvas.drawCircle(cx2, pHeadCy, pHeadR, pPaint)

    val pBodyW = modeRect.height() * 0.38f
    val pBodyH = modeRect.height() * 0.28f
    val pBodyTop = pHeadCy + pHeadR + (scaleFactor * 1.5f)
    val pBodyRect = RectF(cx2 - pBodyW / 2f, pBodyTop, cx2 + pBodyW / 2f, pBodyTop + pBodyH)
    canvas.drawArc(pBodyRect, 180f, 180f, true, pPaint)

    // Reset Button
    val resetW = headerRect.width() * 0.38f
    val resetRect = RectF(headerRect.right - resetW, headerRect.top, headerRect.right, headerRect.bottom)
    canvas.drawRoundRect(resetRect, innerRadius, innerRadius, pillBgPaint)

    val resetTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
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

    val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
        style = Paint.Style.FILL
    }

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
            val cellRect = RectF(left, top, left + cellW, top + cellH)

            canvas.drawRoundRect(cellRect, cellRadius, cellRadius, cellPaint)

            val cx = cellRect.centerX()
            val cy = cellRect.centerY()
            val symbolR = minOf(cellW, cellH) * 0.26f

            when (state.board[idx]) {
                1 -> {
                    canvas.drawLine(cx - symbolR, cy - symbolR, cx + symbolR, cy + symbolR, xPaint)
                    canvas.drawLine(cx + symbolR, cy - symbolR, cx - symbolR, cy + symbolR, xPaint)
                }
                2 -> {
                    canvas.drawCircle(cx, cy, symbolR, oPaint)
                }
            }
        }
    }

    return bitmap
}
