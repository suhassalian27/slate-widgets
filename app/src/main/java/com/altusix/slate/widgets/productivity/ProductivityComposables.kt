package com.altusix.slate.widgets.productivity

import android.content.Context
import android.graphics.*
import androidx.compose.ui.graphics.toArgb
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.createSupersampledCanvas
import com.altusix.slate.utils.getSafeBgColor
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// =========================================================================
// CANVAS BITMAP GENERATORS FOR SLATE "PRODUCTIVITY" WIDGETS
// =========================================================================

// -------------------------------------------------------------------------
// PRIVATE VECTOR & DRAWING HELPERS
// -------------------------------------------------------------------------

private fun drawCardBackground(
    canvas: Canvas,
    rect: RectF,
    config: SlateWidgetConfig,
    scaleFactor: Float
): Pair<Int, Int> {
    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val cornerRadius = getStandardCornerRadius(scaleFactor)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(30, 0, 0, 0) else Color.argb(35, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

    val primaryTextColor = if (isLight) Color.BLACK else Color.WHITE
    val secondaryTextColor = if (isLight) Color.argb(170, 0, 0, 0) else Color.argb(170, 255, 255, 255)
    return Pair(primaryTextColor, secondaryTextColor)
}

private fun drawCheckmarkIcon(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    radius: Float,
    isCompleted: Boolean,
    accentColor: Int,
    secondaryColor: Int,
    scaleFactor: Float
) {
    val boxRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
    val corner = radius * 0.45f

    if (isCompleted) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(boxRect, corner, corner, fillPaint)

        val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2.2f * scaleFactor
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = Path().apply {
            moveTo(cx - radius * 0.48f, cy)
            lineTo(cx - radius * 0.10f, cy + radius * 0.42f)
            lineTo(cx + radius * 0.52f, cy - radius * 0.40f)
        }
        canvas.drawPath(path, checkPaint)
    } else {
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(80, Color.red(secondaryColor), Color.green(secondaryColor), Color.blue(secondaryColor))
            style = Paint.Style.STROKE
            strokeWidth = 1.6f * scaleFactor
        }
        canvas.drawRoundRect(boxRect, corner, corner, strokePaint)
    }
}

private fun drawFlameIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    val path = Path().apply {
        moveTo(cx, cy - size * 0.5f)
        cubicTo(cx + size * 0.35f, cy - size * 0.2f, cx + size * 0.45f, cy + size * 0.1f, cx + size * 0.3f, cy + size * 0.45f)
        cubicTo(cx + size * 0.15f, cy + size * 0.6f, cx - size * 0.15f, cy + size * 0.6f, cx - size * 0.3f, cy + size * 0.45f)
        cubicTo(cx - size * 0.45f, cy + size * 0.15f, cx - size * 0.2f, cy - size * 0.15f, cx, cy - size * 0.5f)
        close()
    }
    canvas.drawPath(path, paint)
}

private fun drawGlobeIcon(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int, scaleFactor: Float) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = 1.3f * scaleFactor
    }
    canvas.drawCircle(cx, cy, radius, paint)
    // Horizontal equator
    canvas.drawLine(cx - radius, cy, cx + radius, cy, paint)
    // Vertical meridian oval
    val oval = RectF(cx - radius * 0.45f, cy - radius, cx + radius * 0.45f, cy + radius)
    canvas.drawOval(oval, paint)
}

private fun drawClipboardIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int, scaleFactor: Float) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = 1.4f * scaleFactor
    }
    val w = size * 0.7f
    val h = size * 0.9f
    val board = RectF(cx - w / 2f, cy - h / 2f + size * 0.1f, cx + w / 2f, cy + h / 2f + size * 0.1f)
    canvas.drawRoundRect(board, 3f * scaleFactor, 3f * scaleFactor, paint)

    // Clip at top
    val clipW = w * 0.5f
    val clipH = size * 0.18f
    val clipRect = RectF(cx - clipW / 2f, cy - h / 2f, cx + clipW / 2f, cy - h / 2f + clipH)
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(clipRect, 2f * scaleFactor, 2f * scaleFactor, fillPaint)
}

private fun drawPillBadge(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Float,
    bgHex: Int,
    textHex: Int,
    fontSize: Float,
    context: Context,
    scaleFactor: Float
) {
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textHex
        textSize = fontSize
        typeface = getSlateFont(context, 700)
    }
    val textW = textPaint.measureText(text)
    val padH = 8f * scaleFactor
    val padV = 4f * scaleFactor
    val pillH = fontSize + padV * 2
    val pillW = textW + padH * 2
    val pillRect = RectF(x, y, x + pillW, y + pillH)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgHex
        style = Paint.Style.FILL
    }
    val corner = pillH / 2f
    canvas.drawRoundRect(pillRect, corner, corner, bgPaint)
    canvas.drawText(text, x + padH, y + pillH - padV * 1.3f, textPaint)
}

// =========================================================================
// 1. POMODORO FOCUS DIAL (2x2)
// =========================================================================

fun generatePomodoroTimerBitmap(
    context: Context,
    timer: FocusTimerState,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val (primaryTextColor, secondaryTextColor) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()

    val cx = cardRect.centerX()
    val cy = cardRect.top + cardRect.height() * 0.44f
    val ringRadius = minOf(cardRect.width(), cardRect.height()) * 0.30f
    val strokeW = 7f * scaleFactor

    // Background track arc
    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(30, Color.red(primaryTextColor), Color.green(primaryTextColor), Color.blue(primaryTextColor))
        style = Paint.Style.STROKE
        strokeWidth = strokeW
        strokeCap = Paint.Cap.ROUND
    }
    val arcRect = RectF(cx - ringRadius, cy - ringRadius, cx + ringRadius, cy + ringRadius)
    canvas.drawArc(arcRect, 135f, 270f, false, trackPaint)

    // Progress arc
    val progress = ((timer.totalSeconds - timer.remainingSeconds).toFloat() / timer.totalSeconds.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val sweepAngle = 270f * progress
    if (sweepAngle > 0f) {
        val progPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(arcRect, 135f, sweepAngle, false, progPaint)
    }

    // Phase Pill above countdown
    val phaseText = timer.phase
    val phaseBg = Color.argb(45, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
    val phasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = 9.5f * scaleFactor
        typeface = getSlateFont(context, 700)
    }
    val phaseW = phasePaint.measureText(phaseText)
    val pillH = 16f * scaleFactor
    val pillW = phaseW + 14f * scaleFactor
    val pillY = cy - ringRadius * 0.52f
    val pillRect = RectF(cx - pillW / 2f, pillY, cx + pillW / 2f, pillY + pillH)
    val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = phaseBg; style = Paint.Style.FILL }
    canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillBgPaint)
    canvas.drawText(phaseText, cx - phaseW / 2f, pillY + pillH * 0.72f, phasePaint)

    // Large Countdown Time Text
    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = 28f * scaleFactor
        typeface = getSlateFont(context, 800)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(timer.formattedTime, cx, cy + 9f * scaleFactor, timePaint)

    // Session Tracker Dots (● ● ○ ○)
    val dotY = cy + ringRadius * 0.52f
    val dotRadius = 3f * scaleFactor
    val dotSpacing = 10f * scaleFactor
    val totalDotsW = (timer.maxSessions - 1) * dotSpacing
    val startDotX = cx - totalDotsW / 2f

    for (s in 1..timer.maxSessions) {
        val dotX = startDotX + (s - 1) * dotSpacing
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (s <= timer.currentSession) accentColor else Color.argb(60, Color.red(secondaryTextColor), Color.green(secondaryTextColor), Color.blue(secondaryTextColor))
            style = Paint.Style.FILL
        }
        canvas.drawCircle(dotX, dotY, dotRadius, dotPaint)
    }

    // Bottom Action Controls Deck (Reset Left, Play/Pause Center-Right)
    val bottomY = cardRect.bottom - 26f * scaleFactor
    val btnH = 30f * scaleFactor

    // Reset button pill
    val resetW = 40f * scaleFactor
    val resetX = cardRect.left + 16f * scaleFactor
    val resetRect = RectF(resetX, bottomY, resetX + resetW, bottomY + btnH)
    val resetBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(25, Color.red(primaryTextColor), Color.green(primaryTextColor), Color.blue(primaryTextColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(resetRect, 10f * scaleFactor, 10f * scaleFactor, resetBgPaint)
    val resetTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = 13f * scaleFactor
        typeface = getSlateFont(context, 700)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("↺", resetRect.centerX(), resetRect.centerY() + 4f * scaleFactor, resetTextPaint)

    // Play / Pause main pill
    val playX = resetX + resetW + 8f * scaleFactor
    val playW = cardRect.right - 16f * scaleFactor - playX
    val playRect = RectF(playX, bottomY, playX + playW, bottomY + btnH)
    val playBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (timer.isRunning) Color.argb(40, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)) else accentColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(playRect, 10f * scaleFactor, 10f * scaleFactor, playBgPaint)

    val playLabel = if (timer.isRunning) "❚❚  PAUSE" else "▶  START"
    val playTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (timer.isRunning) accentColor else Color.BLACK
        textSize = 11.5f * scaleFactor
        typeface = getSlateFont(context, 700)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(playLabel, playRect.centerX(), playRect.centerY() + 4f * scaleFactor, playTextPaint)

    return bitmap
}

// =========================================================================
// 2. HABIT STREAK MATRIX (4x2)
// =========================================================================

fun generateHabitMatrixBitmap(
    context: Context,
    habit: HabitItem,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val aspect = 2.0f
        val cardW = minOf(w, h * aspect)
        val cardH = cardW / aspect
        RectF((w - cardW) / 2f, (h - cardH) / 2f, (w + cardW) / 2f, (h + cardH) / 2f)
    }

    val (primaryTextColor, secondaryTextColor) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()

    // 1. Header (Habit Name & Streak Pill)
    val headerTop = cardRect.top + 16f * scaleFactor
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = 15f * scaleFactor
        typeface = getSlateFont(context, 700)
    }
    canvas.drawText(habit.name, cardRect.left + 18f * scaleFactor, headerTop + 14f * scaleFactor, titlePaint)

    val streakBadgeText = "🔥 ${habit.streakCount} DAYS"
    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = 11f * scaleFactor
        typeface = getSlateFont(context, 700)
    }
    val badgeW = badgePaint.measureText(streakBadgeText) + 16f * scaleFactor
    val badgeH = 22f * scaleFactor
    val badgeX = cardRect.right - 18f * scaleFactor - badgeW
    val badgeY = headerTop
    val badgeRect = RectF(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH)
    val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(35, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(badgeRect, 8f * scaleFactor, 8f * scaleFactor, badgeBg)
    canvas.drawText(streakBadgeText, badgeX + 8f * scaleFactor, badgeY + 15f * scaleFactor, badgePaint)

    // 2. 7x5 Contribution Grid
    val gridStartX = cardRect.left + 18f * scaleFactor
    val gridStartY = headerTop + 36f * scaleFactor
    val availableW = cardRect.width() - 36f * scaleFactor
    val cols = 5
    val rows = 7
    val cellGap = 5.5f * scaleFactor
    val cellSize = minOf((availableW - (cols - 1) * cellGap) / cols, 15f * scaleFactor)
    val cellCorner = cellSize * 0.35f

    val cal = Calendar.getInstance()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayStr = sdf.format(Date())

    for (c in 0 until cols) {
        for (r in 0 until rows) {
            val dayIndex = (cols - 1 - c) * rows + (rows - 1 - r)
            val tempCal = cal.clone() as Calendar
            tempCal.add(Calendar.DAY_OF_YEAR, -dayIndex)
            val dateStr = sdf.format(tempCal.time)
            val isCompleted = habit.history[dateStr] ?: false
            val isToday = dateStr == todayStr

            val cx = gridStartX + c * (cellSize + cellGap)
            val cy = gridStartY + r * (cellSize + cellGap)
            val cellRect = RectF(cx, cy, cx + cellSize, cy + cellSize)

            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = when {
                    isCompleted -> accentColor
                    else -> Color.argb(35, Color.red(primaryTextColor), Color.green(primaryTextColor), Color.blue(primaryTextColor))
                }
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(cellRect, cellCorner, cellCorner, fillPaint)

            if (isToday) {
                val todayBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = primaryTextColor
                    style = Paint.Style.STROKE
                    strokeWidth = 1.6f * scaleFactor
                }
                canvas.drawRoundRect(cellRect, cellCorner, cellCorner, todayBorder)
            }
        }
    }

    // 3. Right Side: Today's Toggle Action Pill
    val rightSideX = gridStartX + cols * (cellSize + cellGap) + 14f * scaleFactor
    val todayDone = habit.history[todayStr] ?: false
    val actionBtnW = cardRect.right - 18f * scaleFactor - rightSideX
    val actionBtnH = 42f * scaleFactor
    val actionBtnY = gridStartY + 35f * scaleFactor
    val actionRect = RectF(rightSideX, actionBtnY, rightSideX + actionBtnW, actionBtnY + actionBtnH)

    val actionBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (todayDone) accentColor else Color.argb(30, Color.red(primaryTextColor), Color.green(primaryTextColor), Color.blue(primaryTextColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(actionRect, 12f * scaleFactor, 12f * scaleFactor, actionBg)

    val actionLabel = if (todayDone) "✓ TODAY DONE" else "○ MARK TODAY"
    val actionTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (todayDone) Color.BLACK else primaryTextColor
        textSize = 11.5f * scaleFactor
        typeface = getSlateFont(context, 700)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(actionLabel, actionRect.centerX(), actionRect.centerY() + 4f * scaleFactor, actionTextPaint)

    return bitmap
}

// =========================================================================
// 3. DAILY TOP 3 WINS (4x2)
// =========================================================================

fun generateTop3TasksBitmap(
    context: Context,
    tasks: List<Top3TaskItem>,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val aspect = 2.0f
        val cardW = minOf(w, h * aspect)
        val cardH = cardW / aspect
        RectF((w - cardW) / 2f, (h - cardH) / 2f, (w + cardW) / 2f, (h + cardH) / 2f)
    }

    val (primaryTextColor, secondaryTextColor) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()

    // 1. Header (RULE OF 3 & Progress Counter)
    val headerY = cardRect.top + 16f * scaleFactor
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = 14f * scaleFactor
        typeface = getSlateFont(context, 700)
    }
    canvas.drawText("DAILY TOP 3 WINS", cardRect.left + 18f * scaleFactor, headerY + 12f * scaleFactor, headerPaint)

    val completedCount = tasks.take(3).count { it.isCompleted }
    val countBadgeText = "$completedCount/3 DONE"
    val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = 11f * scaleFactor
        typeface = getSlateFont(context, 700)
    }
    val countBadgeW = countPaint.measureText(countBadgeText) + 16f * scaleFactor
    val countBadgeH = 20f * scaleFactor
    val countBadgeX = cardRect.right - 18f * scaleFactor - countBadgeW
    val countRect = RectF(countBadgeX, headerY, countBadgeX + countBadgeW, headerY + countBadgeH)
    val countBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(35, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(countRect, 8f * scaleFactor, 8f * scaleFactor, countBg)
    canvas.drawText(countBadgeText, countBadgeX + 8f * scaleFactor, headerY + 14f * scaleFactor, countPaint)

    // 2. Three Distinct Task Rows
    val rowStartY = headerY + 32f * scaleFactor
    val rowHeight = (cardRect.bottom - rowStartY - 12f * scaleFactor) / 3f
    val checkRadius = 9f * scaleFactor

    for (i in 0 until minOf(3, tasks.size)) {
        val task = tasks[i]
        val rowCenterY = rowStartY + i * rowHeight + rowHeight / 2f

        // Priority Badge Circle (01, 02, 03)
        val numX = cardRect.left + 26f * scaleFactor
        val numBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (task.isCompleted) Color.argb(40, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)) else Color.argb(25, Color.red(primaryTextColor), Color.green(primaryTextColor), Color.blue(primaryTextColor))
            style = Paint.Style.FILL
        }
        canvas.drawCircle(numX, rowCenterY, 9f * scaleFactor, numBgPaint)

        val numTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (task.isCompleted) accentColor else secondaryTextColor
            textSize = 9.5f * scaleFactor
            typeface = getSlateFont(context, 700)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(String.format(Locale.getDefault(), "%02d", i + 1), numX, rowCenterY + 3.5f * scaleFactor, numTextPaint)

        // Checkbox on Right
        val checkX = cardRect.right - 26f * scaleFactor
        drawCheckmarkIcon(canvas, checkX, rowCenterY, checkRadius, task.isCompleted, accentColor, secondaryTextColor, scaleFactor)

        // Task Title text with Strikethrough if completed
        val textStartX = numX + 16f * scaleFactor
        val maxTextW = checkX - checkRadius - textStartX - 10f * scaleFactor

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (task.isCompleted) Color.argb(120, Color.red(primaryTextColor), Color.green(primaryTextColor), Color.blue(primaryTextColor)) else primaryTextColor
            textSize = 12.5f * scaleFactor
            typeface = getSlateFont(context, if (task.isCompleted) 400 else 600)
            isStrikeThruText = task.isCompleted
        }

        var displayTitle = task.title
        while (displayTitle.isNotEmpty() && textPaint.measureText("$displayTitle…") > maxTextW) {
            displayTitle = displayTitle.dropLast(1)
        }
        val finalTitle = if (displayTitle != task.title) "$displayTitle…" else displayTitle
        canvas.drawText(finalTitle, textStartX, rowCenterY + 4.5f * scaleFactor, textPaint)

        // Divider stroke between rows
        if (i < 2) {
            val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(18, Color.red(primaryTextColor), Color.green(primaryTextColor), Color.blue(primaryTextColor))
                strokeWidth = 1f * scaleFactor
            }
            val divY = rowStartY + (i + 1) * rowHeight
            canvas.drawLine(cardRect.left + 18f * scaleFactor, divY, cardRect.right - 18f * scaleFactor, divY, divPaint)
        }
    }

    return bitmap
}

// =========================================================================
// 4. EISENHOWER PRIORITY MATRIX (2x2)
// =========================================================================

fun generateEisenhowerMatrixBitmap(
    context: Context,
    tasks: List<EisenhowerItem>,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val (primaryTextColor, secondaryTextColor) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)

    // Header
    val headerY = cardRect.top + 16f * scaleFactor
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = 13.5f * scaleFactor
        typeface = getSlateFont(context, 700)
    }
    canvas.drawText("PRIORITY MATRIX", cardRect.left + 16f * scaleFactor, headerY + 12f * scaleFactor, headerPaint)

    // 4 Quadrants
    val matrixTop = headerY + 26f * scaleFactor
    val matrixRect = RectF(cardRect.left + 12f * scaleFactor, matrixTop, cardRect.right - 12f * scaleFactor, cardRect.bottom - 12f * scaleFactor)
    val midX = matrixRect.centerX()
    val midY = matrixRect.centerY()

    // Crosshair divider lines
    val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(30, Color.red(primaryTextColor), Color.green(primaryTextColor), Color.blue(primaryTextColor))
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawLine(matrixRect.left, midY, matrixRect.right, midY, gridLinePaint)
    canvas.drawLine(midX, matrixTop, midX, matrixRect.bottom, gridLinePaint)

    val quadrants = listOf(
        Triple("DO NOW", 0xFFFF3B30, tasks.filter { it.quadrant == "Q1_DO" }),
        Triple("SCHEDULE", 0xFF0A84FF, tasks.filter { it.quadrant == "Q2_SCHEDULE" }),
        Triple("DELEGATE", 0xFFFF9500, tasks.filter { it.quadrant == "Q3_DELEGATE" }),
        Triple("ELIMINATE", 0xFF8E8E93, tasks.filter { it.quadrant == "Q4_DROP" })
    )

    val quadrantRects = listOf(
        RectF(matrixRect.left, matrixRect.top, midX, midY),
        RectF(midX, matrixRect.top, matrixRect.right, midY),
        RectF(matrixRect.left, midY, midX, matrixRect.bottom),
        RectF(midX, midY, matrixRect.right, matrixRect.bottom)
    )

    for (i in 0..3) {
        val (label, dotColor, qTasks) = quadrants[i]
        val qRect = quadrantRects[i]

        val dotX = qRect.left + 10f * scaleFactor
        val dotY = qRect.top + 14f * scaleFactor
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dotColor.toInt(); style = Paint.Style.FILL }
        canvas.drawCircle(dotX, dotY, 3.5f * scaleFactor, dotPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryTextColor
            textSize = 9.5f * scaleFactor
            typeface = getSlateFont(context, 700)
        }
        canvas.drawText(label, dotX + 7f * scaleFactor, dotY + 3.5f * scaleFactor, labelPaint)

        // Count Badge / Top Task preview
        val topTask = qTasks.firstOrNull()?.title ?: "${qTasks.size} tasks"
        val taskTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = 11f * scaleFactor
            typeface = getSlateFont(context, 600)
        }
        var displayTask = topTask
        val maxTaskW = qRect.width() - 20f * scaleFactor
        while (displayTask.isNotEmpty() && taskTextPaint.measureText("$displayTask…") > maxTaskW) {
            displayTask = displayTask.dropLast(1)
        }
        canvas.drawText(if (displayTask != topTask) "$displayTask…" else displayTask, dotX, dotY + 20f * scaleFactor, taskTextPaint)
    }

    return bitmap
}

// =========================================================================
// 5. TIME-BLOCK DAY TIMELINE (4x2)
// =========================================================================

fun generateTimeBlockTimelineBitmap(
    context: Context,
    blocks: List<TimeBlockItem>,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val aspect = 2.0f
        val cardW = minOf(w, h * aspect)
        val cardH = cardW / aspect
        RectF((w - cardW) / 2f, (h - cardH) / 2f, (w + cardW) / 2f, (h + cardH) / 2f)
    }

    val (primaryTextColor, secondaryTextColor) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()

    // 1. Header with live clock
    val headerY = cardRect.top + 16f * scaleFactor
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = 14f * scaleFactor
        typeface = getSlateFont(context, 700)
    }
    canvas.drawText("TODAY'S SCHEDULE", cardRect.left + 18f * scaleFactor, headerY + 12f * scaleFactor, titlePaint)

    val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
    val nowTimeStr = sdfTime.format(Date())
    val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = 12f * scaleFactor
        typeface = getSlateFont(context, 700)
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText("NOW  $nowTimeStr", cardRect.right - 18f * scaleFactor, headerY + 12f * scaleFactor, clockPaint)

    // 2. Timeline Blocks (Display up to 3 blocks)
    val blockStartY = headerY + 30f * scaleFactor
    val blockH = (cardRect.bottom - blockStartY - 12f * scaleFactor) / 3f
    val cal = Calendar.getInstance()
    val currentHour = cal.get(Calendar.HOUR_OF_DAY)
    val currentMin = cal.get(Calendar.MINUTE)
    val currentMinutesVal = currentHour * 60 + currentMin

    for (i in 0 until minOf(3, blocks.size)) {
        val block = blocks[i]
        val blockTop = blockStartY + i * blockH
        val bRect = RectF(cardRect.left + 16f * scaleFactor, blockTop + 3f * scaleFactor, cardRect.right - 16f * scaleFactor, blockTop + blockH - 3f * scaleFactor)

        val blockStartMins = block.startHour * 60 + block.startMinute
        val blockEndMins = block.endHour * 60 + block.endMinute
        val isActive = currentMinutesVal in blockStartMins until blockEndMins

        val bBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isActive) Color.argb(40, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)) else Color.argb(20, Color.red(primaryTextColor), Color.green(primaryTextColor), Color.blue(primaryTextColor))
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(bRect, 8f * scaleFactor, 8f * scaleFactor, bBgPaint)

        if (isActive) {
            val activeIndicator = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                style = Paint.Style.FILL
            }
            val indRect = RectF(bRect.left, bRect.top, bRect.left + 4f * scaleFactor, bRect.bottom)
            canvas.drawRoundRect(indRect, 2f * scaleFactor, 2f * scaleFactor, activeIndicator)
        }

        // Time span
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isActive) accentColor else secondaryTextColor
            textSize = 10.5f * scaleFactor
            typeface = getSlateFont(context, 700)
        }
        canvas.drawText(block.timeSpanText, bRect.left + 12f * scaleFactor, bRect.centerY() + 3.5f * scaleFactor, timePaint)

        // Block Title
        val titleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = 11.5f * scaleFactor
            typeface = getSlateFont(context, if (isActive) 700 else 500)
        }
        canvas.drawText(block.title, bRect.left + 115f * scaleFactor, bRect.centerY() + 3.5f * scaleFactor, titleTextPaint)

        // Tag pill on right
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryTextColor
            textSize = 9f * scaleFactor
            typeface = getSlateFont(context, 700)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(block.tag, bRect.right - 12f * scaleFactor, bRect.centerY() + 3.5f * scaleFactor, tagPaint)
    }

    return bitmap
}

// =========================================================================
// 6. GOAL MILESTONE COUNTDOWN (2x2)
// =========================================================================

fun generateGoalMilestoneBitmap(
    context: Context,
    goal: GoalMilestoneItem,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val (primaryTextColor, secondaryTextColor) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()

    // 1. Goal Title Header
    val headerY = cardRect.top + 16f * scaleFactor
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = 11f * scaleFactor
        typeface = getSlateFont(context, 700)
    }
    canvas.drawText(goal.title.uppercase(Locale.getDefault()), cardRect.left + 16f * scaleFactor, headerY + 10f * scaleFactor, titlePaint)

    // 2. Large Bold Percentage
    val percentText = "${goal.currentProgress}%"
    val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = 34f * scaleFactor
        typeface = getSlateFont(context, 800)
    }
    val percentY = headerY + 48f * scaleFactor
    canvas.drawText(percentText, cardRect.left + 16f * scaleFactor, percentY, percentPaint)

    // 3. Horizontal Progress Bar
    val barY = percentY + 16f * scaleFactor
    val barW = cardRect.width() - 32f * scaleFactor
    val barH = 7f * scaleFactor
    val barRect = RectF(cardRect.left + 16f * scaleFactor, barY, cardRect.left + 16f * scaleFactor + barW, barY + barH)

    val barTrack = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(30, Color.red(primaryTextColor), Color.green(primaryTextColor), Color.blue(primaryTextColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(barRect, barH / 2f, barH / 2f, barTrack)

    val progressW = barW * (goal.currentProgress / 100f).coerceIn(0f, 1f)
    if (progressW > 0f) {
        val progBar = RectF(barRect.left, barRect.top, barRect.left + progressW, barRect.bottom)
        val progPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor; style = Paint.Style.FILL }
        canvas.drawRoundRect(progBar, barH / 2f, barH / 2f, progPaint)
    }

    // 4. Milestone & Deadline Info Pill Deck
    val bottomY = cardRect.bottom - 26f * scaleFactor
    val milestoneText = "MILESTONE ${goal.milestoneCurrent}/${goal.milestoneTotal}"
    val deadlineText = "DUE ${goal.deadlineDateText.uppercase(Locale.getDefault())}"

    val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = 10.5f * scaleFactor
        typeface = getSlateFont(context, 700)
    }
    canvas.drawText(milestoneText, cardRect.left + 16f * scaleFactor, bottomY, metaPaint)

    val duePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = 10.5f * scaleFactor
        typeface = getSlateFont(context, 700)
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText(deadlineText, cardRect.right - 16f * scaleFactor, bottomY, duePaint)

    return bitmap
}

// =========================================================================
// 7. WEEKLY HABIT RINGS (2x2)
// =========================================================================

fun generateHabitRingsBitmap(
    context: Context,
    rings: List<HabitRingItem>,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val (primaryTextColor, secondaryTextColor) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)

    // Header
    val headerY = cardRect.top + 16f * scaleFactor
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = 13.5f * scaleFactor
        typeface = getSlateFont(context, 700)
    }
    canvas.drawText("HABIT RINGS", cardRect.left + 16f * scaleFactor, headerY + 12f * scaleFactor, headerPaint)

    // 3 Concentric Rings
    val cx = cardRect.centerX()
    val cy = cardRect.top + cardRect.height() * 0.48f
    val baseRadius = minOf(cardRect.width(), cardRect.height()) * 0.28f
    val ringStroke = 6f * scaleFactor
    val ringGap = 3.5f * scaleFactor

    val defaultColors = listOf(0xFFFF5E3A, 0xFF30D158, 0xFF0A84FF)

    for (i in 0 until minOf(3, rings.size)) {
        val ring = rings[i]
        val r = baseRadius - i * (ringStroke + ringGap)
        val arcRect = RectF(cx - r, cy - r, cx + r, cy + r)
        val colorHex = defaultColors.getOrElse(i) { 0xFF30D158 }.toInt()

        // Track
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(35, Color.red(colorHex), Color.green(colorHex), Color.blue(colorHex))
            style = Paint.Style.STROKE
            strokeWidth = ringStroke
        }
        canvas.drawCircle(cx, cy, r, trackPaint)

        // Progress Arc
        val progress = (ring.currentValue.toFloat() / ring.targetValue.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        val sweepAngle = 360f * progress
        if (sweepAngle > 0f) {
            val progPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorHex
                style = Paint.Style.STROKE
                strokeWidth = ringStroke
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawArc(arcRect, -90f, sweepAngle, false, progPaint)
        }
    }

    // Average percentage in center
    val avgFraction = if (rings.isNotEmpty()) rings.map { it.currentValue.toFloat() / it.targetValue.coerceAtLeast(1).toFloat() }.average().toFloat() else 0.8f
    val percentText = "${(avgFraction * 100).toInt()}%"
    val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = 14f * scaleFactor
        typeface = getSlateFont(context, 800)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(percentText, cx, cy + 5f * scaleFactor, percentPaint)

    // Bottom Legend (3 colored dots with labels)
    val legendY = cardRect.bottom - 16f * scaleFactor
    val dotRadius = 3f * scaleFactor
    val startX = cardRect.left + 16f * scaleFactor
    val availableW = cardRect.width() - 32f * scaleFactor
    val legendColW = availableW / minOf(3, rings.size).coerceAtLeast(1)

    for (i in 0 until minOf(3, rings.size)) {
        val ring = rings[i]
        val colX = startX + i * legendColW
        val colorHex = defaultColors.getOrElse(i) { 0xFF30D158 }.toInt()

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorHex; style = Paint.Style.FILL }
        canvas.drawCircle(colX + dotRadius, legendY - 3f * scaleFactor, dotRadius, dotPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryTextColor
            textSize = 9.5f * scaleFactor
            typeface = getSlateFont(context, 600)
        }
        var lbl = ring.label
        if (lbl.length > 8) lbl = lbl.take(7) + "…"
        canvas.drawText(lbl, colX + dotRadius * 2 + 5f * scaleFactor, legendY, labelPaint)
    }

    return bitmap
}

// =========================================================================
// 8. MINIMAL TASK PIPELINE (4x1)
// =========================================================================

fun generateTaskPipelineBitmap(
    context: Context,
    pipeline: TaskPipelineData,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val aspect = 4.0f
        val cardW = minOf(w, h * aspect)
        val cardH = cardW / aspect
        RectF((w - cardW) / 2f, (h - cardH) / 2f, (w + cardW) / 2f, (h + cardH) / 2f)
    }

    val (primaryTextColor, secondaryTextColor) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()

    val cy = cardRect.centerY()
    val stageW = (cardRect.width() - 32f * scaleFactor) / 3f
    val stages = listOf(
        Triple("${pipeline.todoCount} TO DO", Color.argb(120, 255, 255, 255), primaryTextColor),
        Triple("${pipeline.inProgressCount} IN PROGRESS", 0xFFFF9500.toInt(), 0xFFFF9500.toInt()),
        Triple("${pipeline.doneCount} COMPLETED", accentColor, accentColor)
    )

    for (i in 0..2) {
        val (text, dotColor, textColor) = stages[i]
        val segX = cardRect.left + 16f * scaleFactor + i * stageW

        val dotX = segX + 8f * scaleFactor
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dotColor; style = Paint.Style.FILL }
        canvas.drawCircle(dotX, cy, 4f * scaleFactor, dotPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = 12f * scaleFactor
            typeface = getSlateFont(context, 700)
        }
        canvas.drawText(text, dotX + 8f * scaleFactor, cy + 4f * scaleFactor, textPaint)

        // Divider arrow or line
        if (i < 2) {
            val divX = segX + stageW - 4f * scaleFactor
            val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(40, Color.red(primaryTextColor), Color.green(primaryTextColor), Color.blue(primaryTextColor))
                textSize = 13f * scaleFactor
                typeface = getSlateFont(context, 400)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("›", divX, cy + 4f * scaleFactor, divPaint)
        }
    }

    return bitmap
}

// =========================================================================
// 9. BOOKMARK LIST (4x2)
// =========================================================================

fun generateBookmarkListBitmap(
    context: Context,
    bookmarks: List<BookmarkItem>,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val aspect = 2.0f
        val cardW = minOf(w, h * aspect)
        val cardH = cardW / aspect
        RectF((w - cardW) / 2f, (h - cardH) / 2f, (w + cardW) / 2f, (h + cardH) / 2f)
    }

    val (primaryTextColor, secondaryTextColor) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()

    // 1. Header
    val headerY = cardRect.top + 16f * scaleFactor
    drawGlobeIcon(canvas, cardRect.left + 24f * scaleFactor, headerY + 8f * scaleFactor, 7f * scaleFactor, accentColor, scaleFactor)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = 13.5f * scaleFactor
        typeface = getSlateFont(context, 700)
    }
    canvas.drawText("BOOKMARKS", cardRect.left + 38f * scaleFactor, headerY + 12f * scaleFactor, titlePaint)

    val countBadgeText = "${bookmarks.size} LINKS"
    val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = 10f * scaleFactor
        typeface = getSlateFont(context, 600)
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText(countBadgeText, cardRect.right - 18f * scaleFactor, headerY + 12f * scaleFactor, countPaint)

    // 2. Four Bookmark Rows
    val rowStartY = headerY + 28f * scaleFactor
    val rowH = (cardRect.bottom - rowStartY - 10f * scaleFactor) / 4f

    for (i in 0 until minOf(4, bookmarks.size)) {
        val b = bookmarks[i]
        val rowCenterY = rowStartY + i * rowH + rowH / 2f

        // Globe / Link Bullet
        val bulletX = cardRect.left + 24f * scaleFactor
        val bDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
            style = Paint.Style.FILL
        }
        canvas.drawCircle(bulletX, rowCenterY, 7f * scaleFactor, bDotPaint)

        val innerDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor; style = Paint.Style.FILL }
        canvas.drawCircle(bulletX, rowCenterY, 2.5f * scaleFactor, innerDot)

        // Title text
        val titleTextX = bulletX + 16f * scaleFactor
        val bTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = 12f * scaleFactor
            typeface = getSlateFont(context, 600)
        }
        canvas.drawText(b.title, titleTextX, rowCenterY + 4f * scaleFactor, bTitlePaint)

        // Domain pill on right
        val domainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryTextColor
            textSize = 9.5f * scaleFactor
            typeface = getSlateFont(context, 500)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("${b.domain} ↗", cardRect.right - 18f * scaleFactor, rowCenterY + 3.5f * scaleFactor, domainPaint)

        if (i < 3) {
            val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(16, Color.red(primaryTextColor), Color.green(primaryTextColor), Color.blue(primaryTextColor))
                strokeWidth = 1f * scaleFactor
            }
            val divY = rowStartY + (i + 1) * rowH
            canvas.drawLine(cardRect.left + 18f * scaleFactor, divY, cardRect.right - 18f * scaleFactor, divY, divPaint)
        }
    }

    return bitmap
}

// =========================================================================
// 10. SCREEN TIME BALANCE (2x2)
// =========================================================================

fun generateScreenTimeBitmap(
    context: Context,
    screenTime: ScreenTimeData,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val (primaryTextColor, secondaryTextColor) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()

    // 1. Header
    val headerY = cardRect.top + 16f * scaleFactor
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = 10.5f * scaleFactor
        typeface = getSlateFont(context, 700)
    }
    canvas.drawText("SCREEN TIME", cardRect.left + 16f * scaleFactor, headerY + 10f * scaleFactor, headerPaint)

    // 2. Large Time Display
    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = 30f * scaleFactor
        typeface = getSlateFont(context, 800)
    }
    val timeY = headerY + 44f * scaleFactor
    canvas.drawText(screenTime.formattedHoursMinutes, cardRect.left + 16f * scaleFactor, timeY, timePaint)

    // 3. Limit Progress Bar
    val barY = timeY + 14f * scaleFactor
    val barW = cardRect.width() - 32f * scaleFactor
    val barH = 6f * scaleFactor
    val barRect = RectF(cardRect.left + 16f * scaleFactor, barY, cardRect.left + 16f * scaleFactor + barW, barY + barH)

    val barBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(30, Color.red(primaryTextColor), Color.green(primaryTextColor), Color.blue(primaryTextColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(barRect, barH / 2f, barH / 2f, barBg)

    val progW = barW * screenTime.progressFraction
    if (progW > 0f) {
        val progPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor; style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(barRect.left, barRect.top, barRect.left + progW, barRect.bottom), barH / 2f, barH / 2f, progPaint)
    }

    // 4. Limit Text & Pickups Pill
    val metaY = barY + 18f * scaleFactor
    val limitText = "Goal: ${screenTime.limitMinutes / 60}h ${screenTime.limitMinutes % 60}m"
    val limitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = 10f * scaleFactor
        typeface = getSlateFont(context, 600)
    }
    canvas.drawText(limitText, cardRect.left + 16f * scaleFactor, metaY, limitPaint)

    val pickupsText = "${screenTime.pickupsCount} pickups"
    val pickupsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = 10f * scaleFactor
        typeface = getSlateFont(context, 700)
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText(pickupsText, cardRect.right - 16f * scaleFactor, metaY, pickupsPaint)

    // 5. Category Split Bars at Bottom
    val catY = cardRect.bottom - 22f * scaleFactor
    val catColW = barW / screenTime.topCategories.size.coerceAtLeast(1)
    for (i in screenTime.topCategories.indices) {
        val (catName, mins) = screenTime.topCategories[i]
        val colX = cardRect.left + 16f * scaleFactor + i * catColW
        val catText = "$catName ${mins}m"
        val catPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryTextColor
            textSize = 9f * scaleFactor
            typeface = getSlateFont(context, 500)
        }
        canvas.drawText(catText, colX, catY, catPaint)
    }

    return bitmap
}

// =========================================================================
// 11. CLIPBOARD VAULT (2x2)
// =========================================================================

fun generateClipboardVaultBitmap(
    context: Context,
    snippets: List<ClipboardSnippetItem>,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val (primaryTextColor, secondaryTextColor) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()

    // 1. Header
    val headerY = cardRect.top + 16f * scaleFactor
    drawClipboardIcon(canvas, cardRect.left + 22f * scaleFactor, headerY + 8f * scaleFactor, 14f * scaleFactor, accentColor, scaleFactor)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = 13f * scaleFactor
        typeface = getSlateFont(context, 700)
    }
    canvas.drawText("CLIPBOARD VAULT", cardRect.left + 36f * scaleFactor, headerY + 12f * scaleFactor, titlePaint)

    // 2. Pinned Snippet Cards (Display 2 snippets)
    val snippetStartY = headerY + 28f * scaleFactor
    val snippetH = (cardRect.bottom - snippetStartY - 14f * scaleFactor) / 2f

    for (i in 0 until minOf(2, snippets.size)) {
        val snippet = snippets[i]
        val sTop = snippetStartY + i * snippetH
        val sRect = RectF(cardRect.left + 14f * scaleFactor, sTop + 3f * scaleFactor, cardRect.right - 14f * scaleFactor, sTop + snippetH - 3f * scaleFactor)

        val sBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(25, Color.red(primaryTextColor), Color.green(primaryTextColor), Color.blue(primaryTextColor))
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(sRect, 10f * scaleFactor, 10f * scaleFactor, sBgPaint)

        // Snippet Label
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 10f * scaleFactor
            typeface = getSlateFont(context, 700)
        }
        canvas.drawText(snippet.label.uppercase(Locale.getDefault()), sRect.left + 10f * scaleFactor, sRect.top + 14f * scaleFactor, labelPaint)

        // Snippet Content preview
        val contentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = 11.5f * scaleFactor
            typeface = getSlateFont(context, 500)
        }
        var displayContent = snippet.content.replace("\n", " ")
        val maxContentW = sRect.width() - 44f * scaleFactor
        while (displayContent.isNotEmpty() && contentPaint.measureText("$displayContent…") > maxContentW) {
            displayContent = displayContent.dropLast(1)
        }
        canvas.drawText(if (displayContent != snippet.content.replace("\n", " ")) "$displayContent…" else displayContent, sRect.left + 10f * scaleFactor, sRect.top + 29f * scaleFactor, contentPaint)

        // Copy icon pill on right
        val copyX = sRect.right - 24f * scaleFactor
        val copyY = sRect.centerY()
        val copyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryTextColor
            textSize = 12f * scaleFactor
            typeface = getSlateFont(context, 700)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("📋", copyX, copyY + 4f * scaleFactor, copyTextPaint)
    }

    return bitmap
}
