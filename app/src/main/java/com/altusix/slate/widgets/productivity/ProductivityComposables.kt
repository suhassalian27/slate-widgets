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
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
// 1. POMODORO FOCUS DIAL (2x2 - Hero Dial with In-Pill Live Countdown)
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

    // 1. Fixed Mode Square Card Anchoring
    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val size = minOf(w, h)
        val leftX = (w - size) / 2f
        val topY = (h - size) / 2f
        RectF(leftX, topY, leftX + size, topY + size)
    }

    val (primaryTextColor, secondaryTextColor) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"

    val minDim = minOf(cardRect.width(), cardRect.height())
    val cx = cardRect.centerX()

    // 2. Real-Time Countdown & Angle Calculations
    val effectiveRemaining = if (timer.isRunning && timer.lastTimestamp > 0L) {
        val elapsed = ((System.currentTimeMillis() - timer.lastTimestamp) / 1000).toInt()
        maxOf(0, timer.remainingSeconds - elapsed)
    } else {
        timer.remainingSeconds
    }
    val mins = effectiveRemaining / 60
    val secs = effectiveRemaining % 60
    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

    val totalSecs = timer.totalSeconds.coerceAtLeast(60)
    val progress = ((totalSecs - effectiveRemaining).toFloat() / totalSecs.toFloat()).coerceIn(0f, 1f)
    val sweepAngle = progress * 360f

    // 3. Enriched Bottom Controls Geometry
    val bottomCenterY = cardRect.bottom - (minDim * 0.125f)
    val pillH = minDim * 0.145f
    val pillW = minDim * 0.46f
    val btnR = pillH / 2f
    val btnGap = minDim * 0.035f

    // 4. Maximum Hero Dial Space (Occupies entire upper deck)
    val bottomDockTop = bottomCenterY - (pillH / 2f)
    val availableUpperH = bottomDockTop - cardRect.top
    val cy = cardRect.top + (availableUpperH / 2f) + (minDim * 0.015f)
    val dialR = (availableUpperH / 2f) * 0.88f

    // 5. Dial Tick Marks (60 radial marks floating outside the disc)
    val rOuterTicks = dialR
    val majorTickLen = minDim * 0.040f
    val minorTickLen = minDim * 0.022f

    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    canvas.save()
    for (i in 0 until 60) {
        val isMajor = (i % 5 == 0)
        tickPaint.strokeWidth = if (isMajor) 1.8f * scaleFactor else 1.0f * scaleFactor
        tickPaint.color = if (isMajor) {
            primaryTextColor
        } else {
            Color.argb(70, Color.red(primaryTextColor), Color.green(primaryTextColor), Color.blue(primaryTextColor))
        }

        val startY = cy - rOuterTicks
        val endY = startY + (if (isMajor) majorTickLen else minorTickLen)
        canvas.drawLine(cx, startY, cx, endY, tickPaint)
        canvas.rotate(6f, cx, cy)
    }
    canvas.restore()

    // 6. Subtle Translucent Base Disc
    val discR = rOuterTicks - majorTickLen - (minDim * 0.030f)
    val discBounds = RectF(cx - discR, cy - discR, cx + discR, cy + discR)

    val discBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(12, 0, 0, 0) else Color.argb(14, 255, 255, 255)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, discR, discBgPaint)

    // 7. Time Timer Pie Wedge (Clockwise 12 o'clock sweep)
    if (sweepAngle > 0.5f) {
        val wedgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(
                if (isLight) 95 else 130,
                Color.red(accentColor),
                Color.green(accentColor),
                Color.blue(accentColor)
            )
            style = Paint.Style.FILL
        }
        canvas.drawArc(discBounds, -90f, sweepAngle, true, wedgePaint)
    }

    // 8. Needle Hand & Two-Tier Hub Knob
    val handAngleRad = Math.toRadians((-90f + sweepAngle).toDouble())
    val handLen = discR * 0.92f
    val tipX = (cx + handLen * kotlin.math.cos(handAngleRad)).toFloat()
    val tipY = (cy + handLen * kotlin.math.sin(handAngleRad)).toFloat()

    val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3.5f * scaleFactor
        strokeCap = Paint.Cap.ROUND
        setShadowLayer(4f * scaleFactor, 0f, 1.5f * scaleFactor, 0x66000000)
    }
    canvas.drawLine(cx, cy, tipX, tipY, handPaint)

    // Frosted collar + solid white cap
    val collarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(85, 255, 255, 255)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, minDim * 0.046f, collarPaint)

    val hubCapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        setShadowLayer(2f * scaleFactor, 0f, 1f * scaleFactor, 0x44000000)
    }
    canvas.drawCircle(cx, cy, minDim * 0.028f, hubCapPaint)

    // 9. Enlarged Clustered Controls (-5 • [▶/❚❚ TIME] • +5)
    val leftBtnCx = cx - (pillW / 2f) - btnGap - btnR
    val rightBtnCx = cx + (pillW / 2f) + btnGap + btnR

    val circleBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x14000000 else 0x18FFFFFF
        style = Paint.Style.FILL
    }
    val circleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = pillH * 0.38f
        typeface = getSlateFont(context, 700)
        textAlign = Paint.Align.CENTER
    }

    // Left Circle: -5
    canvas.drawCircle(leftBtnCx, bottomCenterY, btnR, circleBgPaint)
    val circleBaseline = bottomCenterY - ((circleTextPaint.fontMetrics.ascent + circleTextPaint.fontMetrics.descent) / 2f)
    canvas.drawText("-5", leftBtnCx, circleBaseline, circleTextPaint)

    // Right Circle: +5
    canvas.drawCircle(rightBtnCx, bottomCenterY, btnR, circleBgPaint)
    canvas.drawText("+5", rightBtnCx, circleBaseline, circleTextPaint)

    // Center Pill containing State Icon & Live Time Readout
    val pillRect = RectF(cx - pillW / 2f, bottomCenterY - pillH / 2f, cx + pillW / 2f, bottomCenterY + pillH / 2f)
    val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (timer.isRunning) Color.argb(45, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)) else accentColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillBgPaint)

    // Combined Action Icon + Live Countdown Readout
    val buttonLabel = when {
        effectiveRemaining <= 0 -> "↺  RESET"
        timer.isRunning -> "❚❚  $formattedTime"
        else -> "▶  $formattedTime"
    }

    val buttonTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (timer.isRunning) accentColor else if (calculateLuminance(accentColor.toLong()) > 0.5f) Color.BLACK else Color.WHITE
        textSize = pillH * 0.38f
        typeface = getSlateFont(context, 800)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.01f
    }
    val pillBaseline = pillRect.centerY() - ((buttonTextPaint.fontMetrics.ascent + buttonTextPaint.fontMetrics.descent) / 2f)
    canvas.drawText(buttonLabel, pillRect.centerX(), pillBaseline, buttonTextPaint)

    return bitmap
}

private fun calculateLuminance(colorLong: Long): Float {
    val r = ((colorLong shr 16) and 0xFFL) / 255f
    val g = ((colorLong shr 8) and 0xFFL) / 255f
    val b = (colorLong and 0xFFL) / 255f
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}


// =========================================================================
// 2. HABIT STREAK MATRIX (Balanced Proportional Scaling with Max Font Caps)
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

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val targetRatio = 2.0f
        var cardH = h
        var cardW = cardH * targetRatio
        if (cardW > w) {
            cardW = w
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val (primaryTextColor, secondaryTextColor) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayCal = Calendar.getInstance()
    val todayStr = sdf.format(todayCal.time)
    val isTodayDone = habit.history[todayStr] == true

    val streak = calculateHabitStreak(habit.history)
    val (weekDone, _) = calculateWeekCompletion(habit.history)

    val cardW = cardRect.width()
    val cardH = cardRect.height()
    val baseDim = minOf(cardH, cardW * 0.55f)

    // 1. Uniform Proportional Margins
    val padX = cardW * 0.065f
    val padY = cardH * 0.075f

    val contentLeft = cardRect.left + padX
    val contentRight = cardRect.right - padX
    val contentTop = cardRect.top + padY
    val contentBottom = cardRect.bottom - padY

    // 2. Controlled Header Bounds
    val headerH = (baseDim * 0.24f).coerceIn(24f * scaleFactor, 46f * scaleFactor)

    // Max Typography Caps: Title capped at 18sp, Subtitle capped at 12sp
    val titleSize = (headerH * 0.48f).coerceAtMost(18f * scaleFactor)
    val subSize = (headerH * 0.32f).coerceAtMost(12f * scaleFactor)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = titleSize
        typeface = getSlateFont(context, 700)
    }
    val habitName = habit.name.ifBlank { "Workout" }
    canvas.drawText(habitName, contentLeft, contentTop + titleSize, titlePaint)

    val metaText = "🔥 $streak  ·  $weekDone/3 this week"
    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = subSize
        typeface = getSlateFont(context, 500)
    }
    canvas.drawText(metaText, contentLeft, contentTop + titleSize + subSize + (3f * scaleFactor), subPaint)

    // Check Action Button (Capped at 19dp radius)
    val toggleBtnR = (headerH * 0.44f).coerceAtMost(19f * scaleFactor)
    val toggleBtnCx = contentRight - toggleBtnR
    val toggleBtnCy = contentTop + (headerH / 2f)

    val toggleBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isTodayDone) accentColor else if (isLight) Color.argb(16, 0, 0, 0) else Color.argb(24, 255, 255, 255)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(toggleBtnCx, toggleBtnCy, toggleBtnR, toggleBgPaint)

    if (!isTodayDone) {
        val toggleBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.argb(28, 0, 0, 0) else Color.argb(36, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = maxOf(1f * scaleFactor, toggleBtnR * 0.07f)
        }
        canvas.drawCircle(toggleBtnCx, toggleBtnCy, toggleBtnR, toggleBorder)
    }

    val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isTodayDone) {
            if (calculateLuminance(accentColor.toLong()) > 0.5f) Color.BLACK else Color.WHITE
        } else {
            secondaryTextColor
        }
        style = Paint.Style.STROKE
        strokeWidth = (toggleBtnR * 0.16f).coerceIn(1.6f * scaleFactor, 2.6f * scaleFactor)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val checkPath = Path().apply {
        val cr = toggleBtnR * 0.38f
        moveTo(toggleBtnCx - cr, toggleBtnCy)
        lineTo(toggleBtnCx - (cr * 0.25f), toggleBtnCy + (cr * 0.75f))
        lineTo(toggleBtnCx + (cr * 1.05f), toggleBtnCy - (cr * 0.65f))
    }
    canvas.drawPath(checkPath, checkPaint)

    // 3. Edge-to-Edge Dot Matrix Grid
    val gridTop = contentTop + headerH + (baseDim * 0.04f)
    val availW = (contentRight - contentLeft).coerceAtLeast(10f)
    val availH = (contentBottom - gridTop).coerceAtLeast(10f)

    val rows = 7
    val todayRow = (todayCal.get(Calendar.DAY_OF_WEEK) + 5) % 7

    val rowPitch = availH / rows.toFloat()
    val dotR = (rowPitch * 0.36f).coerceAtLeast(3f * scaleFactor)

    val cols = kotlin.math.round((availW - (2f * dotR)) / rowPitch).toInt().coerceAtLeast(5) + 1
    val colPitch = (availW - (2f * dotR)) / (cols - 1).toFloat()

    val totalGridH = (rows - 1) * rowPitch + (2f * dotR)
    val gridStartY = gridTop + ((availH - totalGridH) / 2f) + dotR
    val gridStartX = contentLeft + dotR

    val filledPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }
    val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(18, 0, 0, 0) else Color.argb(26, 255, 255, 255)
        style = Paint.Style.FILL
    }
    val todayRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        style = Paint.Style.STROKE
        strokeWidth = (dotR * 0.28f).coerceIn(1.3f * scaleFactor, 2.2f * scaleFactor)
    }

    for (c in 0 until cols) {
        val weeksAgo = (cols - 1) - c
        for (r in 0 until rows) {
            if (c == cols - 1 && r > todayRow) continue

            val dayOffset = (weeksAgo * 7) + (todayRow - r)
            val tempCal = todayCal.clone() as Calendar
            tempCal.add(Calendar.DAY_OF_YEAR, -dayOffset)
            val dateStr = sdf.format(tempCal.time)

            val isDone = habit.history[dateStr] == true
            val isToday = (c == cols - 1 && r == todayRow)

            val dotCx = gridStartX + (c * colPitch)
            val dotCy = gridStartY + (r * rowPitch)

            when {
                isDone -> {
                    canvas.drawCircle(dotCx, dotCy, dotR, filledPaint)
                }
                isToday -> {
                    canvas.drawCircle(dotCx, dotCy, dotR - (todayRingPaint.strokeWidth / 2f), todayRingPaint)
                }
                else -> {
                    canvas.drawCircle(dotCx, dotCy, dotR, emptyPaint)
                }
            }
        }
    }

    return bitmap
}


// =========================================================================
// 3. DAILY TOP 3 WINS (Proportional with Clean Font Ceilings)
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

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val targetRatio = 2.0f
        var cardH = h
        var cardW = cardH * targetRatio
        if (cardW > w) {
            cardW = w
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val (primaryTextColor, secondaryTextColor) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"

    val cardW = cardRect.width()
    val cardH = cardRect.height()
    val baseDim = minOf(cardH, cardW * 0.55f)

    val padX = cardW * 0.065f
    val padY = cardH * 0.075f

    val contentLeft = cardRect.left + padX
    val contentRight = cardRect.right - padX
    val contentTop = cardRect.top + padY
    val contentBottom = cardRect.bottom - padY

    // 1. Header (Capped at 15sp title)
    val headerH = (baseDim * 0.22f).coerceIn(20f * scaleFactor, 40f * scaleFactor)
    val titleSize = (headerH * 0.50f).coerceAtMost(15f * scaleFactor)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = titleSize
        typeface = getSlateFont(context, 800)
        letterSpacing = 0.04f
    }
    val titleBaseline = contentTop + (headerH * 0.68f)
    canvas.drawText("DAILY TOP 3 WINS", contentLeft, titleBaseline, titlePaint)

    val validTasks = tasks.take(3).filter { it.title.isNotBlank() }
    val isEmptyState = validTasks.isEmpty()
    val validDone = validTasks.count { it.isCompleted }

    // Badge Sizing (Capped at 11.5sp text, 24dp height)
    val badgeText = if (isEmptyState) "+ SET WINS" else "$validDone/3 DONE"
    val badgeH = (headerH * 0.84f).coerceAtMost(24f * scaleFactor)
    val badgeTextSize = (badgeH * 0.46f).coerceAtMost(11.5f * scaleFactor)

    val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isEmptyState) {
            if (calculateLuminance(accentColor.toLong()) > 0.5f) Color.BLACK else Color.WHITE
        } else if (validDone > 0) {
            accentColor
        } else {
            secondaryTextColor
        }
        textSize = badgeTextSize
        typeface = getSlateFont(context, 800)
        letterSpacing = 0.03f
    }

    val badgeTextW = badgeTextPaint.measureText(badgeText)
    val badgePadX = badgeH * 0.42f
    val badgeW = badgeTextW + (badgePadX * 2f)
    val badgeRect = RectF(
        contentRight - badgeW,
        contentTop + ((headerH - badgeH) / 2f),
        contentRight,
        contentTop + ((headerH + badgeH) / 2f)
    )

    val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = when {
            isEmptyState -> accentColor
            validDone > 0 -> Color.argb(35, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
            else -> if (isLight) Color.argb(15, 0, 0, 0) else Color.argb(20, 255, 255, 255)
        }
        style = Paint.Style.FILL
    }
    val badgeCorner = badgeH * 0.30f
    canvas.drawRoundRect(badgeRect, badgeCorner, badgeCorner, badgeBgPaint)
    val badgeBaseline = badgeRect.centerY() - ((badgeTextPaint.fontMetrics.ascent + badgeTextPaint.fontMetrics.descent) / 2f)
    canvas.drawText(badgeText, badgeRect.centerX() - (badgeTextW / 2f), badgeBaseline, badgeTextPaint)

    // 2. Priority List Rows (Task text capped at 15sp, Index at 12.5sp, Box capped at 22dp)
    val listTop = contentTop + headerH + (baseDim * 0.04f)
    val availableListH = contentBottom - listTop
    val rowH = availableListH / 3f

    val numSize = (rowH * 0.32f).coerceAtMost(12.5f * scaleFactor)
    val taskTextSize = (rowH * 0.38f).coerceAtMost(15f * scaleFactor)
    val boxSize = minOf(rowH * 0.48f, cardW * 0.09f).coerceAtMost(22f * scaleFactor)
    val boxRadius = boxSize * 0.28f

    val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(110, Color.red(secondaryTextColor), Color.green(secondaryTextColor), Color.blue(secondaryTextColor))
        textSize = numSize
        typeface = getSlateFont(context, 700)
    }

    val taskTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = taskTextSize
        typeface = getSlateFont(context, 600)
    }

    val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(70, Color.red(secondaryTextColor), Color.green(secondaryTextColor), Color.blue(secondaryTextColor))
        textSize = taskTextSize * 0.95f
        typeface = getSlateFont(context, 500)
    }

    val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(12, 0, 0, 0) else Color.argb(16, 255, 255, 255)
        strokeWidth = (baseDim * 0.008f).coerceIn(1f * scaleFactor, 1.8f * scaleFactor)
    }

    for (i in 0 until 3) {
        val task = tasks.getOrNull(i)
        val rowTop = listTop + (i * rowH)
        val rowBottom = rowTop + rowH
        val rowCenterY = (rowTop + rowBottom) / 2f

        if (i > 0) {
            canvas.drawLine(contentLeft, rowTop, contentRight, rowTop, divPaint)
        }

        val indexStr = String.format(Locale.getDefault(), "%02d", i + 1)
        val numBaseline = rowCenterY - ((numPaint.fontMetrics.ascent + numPaint.fontMetrics.descent) / 2f)
        canvas.drawText(indexStr, contentLeft, numBaseline, numPaint)

        val textStartX = contentLeft + (numSize * 2.8f)
        val boxRight = contentRight
        val boxLeft = boxRight - boxSize
        val maxTextW = (boxLeft - (baseDim * 0.05f)) - textStartX

        val hasTitle = task != null && task.title.isNotBlank()
        val isDone = hasTitle && task!!.isCompleted

        if (hasTitle) {
            taskTextPaint.color = if (isDone) {
                Color.argb(100, Color.red(secondaryTextColor), Color.green(secondaryTextColor), Color.blue(secondaryTextColor))
            } else {
                primaryTextColor
            }
            taskTextPaint.isStrikeThruText = isDone
            val textBaseline = rowCenterY - ((taskTextPaint.fontMetrics.ascent + taskTextPaint.fontMetrics.descent) / 2f)
            val elided = android.text.TextUtils.ellipsize(
                task!!.title,
                android.text.TextPaint(taskTextPaint),
                maxTextW,
                android.text.TextUtils.TruncateAt.END
            ).toString()
            canvas.drawText(elided, textStartX, textBaseline, taskTextPaint)
        } else {
            val textBaseline = rowCenterY - ((placeholderPaint.fontMetrics.ascent + placeholderPaint.fontMetrics.descent) / 2f)
            canvas.drawText("Set priority ${i + 1}...", textStartX, textBaseline, placeholderPaint)
        }

        val boxRect = RectF(boxLeft, rowCenterY - (boxSize / 2f), boxRight, rowCenterY + (boxSize / 2f))

        if (hasTitle) {
            if (isDone) {
                val filledBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentColor
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(boxRect, boxRadius, boxRadius, filledBoxPaint)

                val checkMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (calculateLuminance(accentColor.toLong()) > 0.5f) Color.BLACK else Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = (boxSize * 0.13f).coerceIn(1.8f * scaleFactor, 2.8f * scaleFactor)
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                val checkPath = Path().apply {
                    val cx = boxRect.centerX()
                    val cy = boxRect.centerY()
                    val s = boxSize * 0.28f
                    moveTo(cx - s, cy)
                    lineTo(cx - (s * 0.25f), cy + (s * 0.75f))
                    lineTo(cx + (s * 1.05f), cy - (s * 0.65f))
                }
                canvas.drawPath(checkPath, checkMarkPaint)
            } else {
                val emptyBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isLight) Color.argb(45, 0, 0, 0) else Color.argb(55, 255, 255, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = (boxSize * 0.08f).coerceIn(1.3f * scaleFactor, 2.0f * scaleFactor)
                }
                canvas.drawRoundRect(boxRect, boxRadius, boxRadius, emptyBoxPaint)
            }
        } else {
            val mutedBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isLight) Color.argb(18, 0, 0, 0) else Color.argb(22, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 1.0f * scaleFactor
            }
            canvas.drawRoundRect(boxRect, boxRadius, boxRadius, mutedBoxPaint)
        }
    }

    return bitmap
}

// =========================================================================
// 4. EISENHOWER PRIORITY MATRIX (4x2 - Canonical Colors & Top-Aligned)
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

    // 1. 4x2 Fixed (2:1 Ratio) vs Responsive Card Bounds
    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val targetRatio = 2.0f
        var cardH = h
        var cardW = cardH * targetRatio
        if (cardW > w) {
            cardW = w
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val (primaryTextColor, secondaryTextColor) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val isLight = slateConfig.themeMode == "LIGHT"

    val cardW = cardRect.width()
    val cardH = cardRect.height()
    val baseDim = minOf(cardH, cardW * 0.55f)

    // 2. Uniform Margins & Crosshair Divider
    val padX = cardW * 0.055f
    val padY = cardH * 0.075f

    val midX = cardRect.centerX()
    val midY = cardRect.centerY()

    val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(18, 0, 0, 0) else Color.argb(25, 255, 255, 255)
        strokeWidth = maxOf(1f * scaleFactor, baseDim * 0.008f)
    }

    canvas.drawLine(midX, cardRect.top + padY, midX, cardRect.bottom - padY, divPaint)
    canvas.drawLine(cardRect.left + padX, midY, cardRect.right - padX, midY, divPaint)

    // 3. Strict Canonical Eisenhower Color Palette
    data class QuadrantSpec(
        val quadKey: String,
        val label: String,
        val colorInt: Int,
        val bounds: RectF
    )

    val innerGapX = cardW * 0.035f
    val innerGapY = cardH * 0.045f

    val quadrants = listOf(
        QuadrantSpec("Q1_DO", "DO NOW", Color.parseColor("#FF453A"), RectF(cardRect.left + padX, cardRect.top + padY, midX - innerGapX, midY - innerGapY)),
        QuadrantSpec("Q2_SCHEDULE", "SCHEDULE", Color.parseColor("#0A84FF"), RectF(midX + innerGapX, cardRect.top + padY, cardRect.right - padX, midY - innerGapY)),
        QuadrantSpec("Q3_DELEGATE", "DELEGATE", Color.parseColor("#FF9F0A"), RectF(cardRect.left + padX, midY + innerGapY, midX - innerGapX, cardRect.bottom - padY)),
        QuadrantSpec("Q4_DROP", "ELIMINATE", Color.parseColor("#8E8E93"), RectF(midX + innerGapX, midY + innerGapY, cardRect.right - padX, cardRect.bottom - padY))
    )

    val labelSize = (baseDim * 0.115f).coerceIn(11f * scaleFactor, 15f * scaleFactor)
    val dotRadius = labelSize * 0.28f

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = labelSize
        typeface = getSlateFont(context, 700)
        letterSpacing = 0.05f
        color = if (isLight) Color.parseColor("#1C1C1E") else Color.parseColor("#EBEBF5")
    }

    val taskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        typeface = getSlateFont(context, 500)
    }

    val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(65, Color.red(secondaryTextColor), Color.green(secondaryTextColor), Color.blue(secondaryTextColor))
        typeface = getSlateFont(context, 500)
    }

    val bulletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(90, 0, 0, 0) else Color.argb(120, 255, 255, 255)
        style = Paint.Style.FILL
    }

    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // 4. Render Quadrant Contents
    quadrants.forEach { quad ->
        val quadTasks = tasks.filter { it.quadrant == quad.quadKey && it.title.isNotBlank() }
        val qLeft = quad.bounds.left
        val qTop = quad.bounds.top
        val qWidth = quad.bounds.width()
        val qHeight = quad.bounds.height()

        // Quadrant Status Dot
        dotPaint.color = quad.colorInt
        val dotCy = qTop + (labelSize * 0.5f)
        val dotCx = qLeft + dotRadius
        canvas.drawCircle(dotCx, dotCy, dotRadius, dotPaint)

        // Quadrant Label
        val labelBaseline = qTop + labelSize
        canvas.drawText(quad.label, dotCx + (dotRadius * 2.4f), labelBaseline, labelPaint)

        // Top-Aligned Multi-Task List (Fixed 3-Row Pitch)
        val listStartY = labelBaseline + (baseDim * 0.05f)
        val availListH = (qTop + qHeight) - listStartY

        val maxSlots = 3
        val rowPitch = availListH / maxSlots.toFloat()
        val taskTextSize = (rowPitch * 0.56f).coerceIn(11f * scaleFactor, 15.5f * scaleFactor)
        val bulletR = maxOf(1.6f * scaleFactor, taskTextSize * 0.16f)

        taskPaint.textSize = taskTextSize

        if (quadTasks.isNotEmpty()) {
            val displayCount = minOf(maxSlots, quadTasks.size)
            for (i in 0 until displayCount) {
                val item = quadTasks[i]
                val itemCenterY = listStartY + (i * rowPitch) + (rowPitch / 2f)

                val bCx = qLeft + bulletR
                canvas.drawCircle(bCx, itemCenterY, bulletR, bulletPaint)

                val textStartX = bCx + (bulletR * 2.5f) + (4f * scaleFactor)
                val maxTextW = qLeft + qWidth - textStartX

                val textBaseline = itemCenterY - ((taskPaint.fontMetrics.ascent + taskPaint.fontMetrics.descent) / 2f)
                val elided = android.text.TextUtils.ellipsize(
                    item.title,
                    android.text.TextPaint(taskPaint),
                    maxTextW,
                    android.text.TextUtils.TruncateAt.END
                ).toString()
                canvas.drawText(elided, textStartX, textBaseline, taskPaint)
            }
        } else {
            placeholderPaint.textSize = taskTextSize
            val placeholderCenterY = listStartY + (rowPitch / 2f)
            val placeholderBaseline = placeholderCenterY - ((placeholderPaint.fontMetrics.ascent + placeholderPaint.fontMetrics.descent) / 2f)
            canvas.drawText("No tasks added", qLeft, placeholderBaseline, placeholderPaint)
        }
    }

    return bitmap
}


// =========================================================================
// 6. GOAL MILESTONE COUNTDOWN (Balanced Margins & Safe Footer Anchoring)
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

    // 1. True 1:1 Fixed Aspect vs Responsive Card Bounds
    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val targetRatio = 1.0f
        val size = minOf(w, h)
        val leftX = (w - size) / 2f
        val topY = (h - size) / 2f
        RectF(leftX, topY, leftX + size, topY + size)
    }

    val (primaryTextColor, secondaryTextColor) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"

    val cardW = cardRect.width()
    val cardH = cardRect.height()
    val baseDim = minOf(cardW, cardH)
    val aspectRatio = cardW / cardH.coerceAtLeast(1f)

    // Resolved Strings
    val titleText = goal.title.ifBlank { "Milestone Goal" }.uppercase(Locale.getDefault())
    val rawDate = goal.deadlineDateText.trim()
    val dueText = if (rawDate.isNotBlank()) {
        if (rawDate.startsWith("DUE", ignoreCase = true)) rawDate.uppercase(Locale.getDefault())
        else "DUE ${rawDate.uppercase(Locale.getDefault())}"
    } else ""

    val milestoneText = if (goal.milestoneTotal > 0) {
        "MILESTONE ${goal.milestoneCurrent}/${goal.milestoneTotal}"
    } else ""

    val percentText = "${goal.currentProgress}%"
    val progressFrac = (goal.currentProgress / 100f).coerceIn(0f, 1f)

    // =========================================================================
    // LAYOUT A: WIDE FORMAT (aspectRatio >= 1.55)
    // =========================================================================
    if (aspectRatio >= 1.55f) {
        // Balanced margins: padX scales with card height to match vertical padding
        val padY = (cardH * 0.13f).coerceIn(10f * scaleFactor, 16f * scaleFactor)
        val padX = (cardH * 0.16f).coerceIn(14f * scaleFactor, 22f * scaleFactor)

        val contentLeft = cardRect.left + padX
        val contentRight = cardRect.right - padX
        val contentTop = cardRect.top + padY
        val contentBottom = cardRect.bottom - padY
        val availW = contentRight - contentLeft

        val titleSize = (cardH * 0.15f).coerceIn(10f * scaleFactor, 13.5f * scaleFactor)
        val percentSize = (cardH * 0.38f).coerceIn(24f * scaleFactor, 42f * scaleFactor)
        val metaSize = (cardH * 0.135f).coerceIn(9.5f * scaleFactor, 12.5f * scaleFactor)
        val barH = (cardH * 0.09f).coerceIn(5.5f * scaleFactor, 9f * scaleFactor)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryTextColor
            textSize = titleSize
            typeface = getSlateFont(context, 700)
            letterSpacing = 0.03f
        }

        val duePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = metaSize
            typeface = getSlateFont(context, 700)
            textAlign = Paint.Align.RIGHT
        }

        val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = percentSize
            typeface = getSlateFont(context, 800)
        }

        val milestonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = metaSize
            typeface = getSlateFont(context, 700)
            textAlign = Paint.Align.RIGHT
        }

        // Row 1: Title (Left) & Due Date (Right)
        val row1Baseline = contentTop + titleSize
        val dueW = if (dueText.isNotEmpty()) duePaint.measureText(dueText) else 0f
        val maxTitleW = availW - dueW - (12f * scaleFactor)
        val elidedTitle = android.text.TextUtils.ellipsize(
            titleText,
            android.text.TextPaint(titlePaint),
            maxTitleW,
            android.text.TextUtils.TruncateAt.END
        ).toString()

        canvas.drawText(elidedTitle, contentLeft, row1Baseline, titlePaint)
        if (dueText.isNotEmpty()) {
            canvas.drawText(dueText, contentRight, row1Baseline, duePaint)
        }

        // Row 2: Percentage (Left) & Milestone (Right)
        val barY = contentBottom - barH
        val row2Baseline = barY - (8f * scaleFactor)
        canvas.drawText(percentText, contentLeft, row2Baseline, percentPaint)

        if (milestoneText.isNotEmpty()) {
            canvas.drawText(milestoneText, contentRight, row2Baseline, milestonePaint)
        }

        // Row 3: Progress Bar
        val barRect = RectF(contentLeft, barY, contentRight, barY + barH)
        val barRadius = barH / 2f

        val barTrack = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.argb(18, 0, 0, 0) else Color.argb(28, 255, 255, 255)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(barRect, barRadius, barRadius, barTrack)

        val progressW = barRect.width() * progressFrac
        if (progressW > 0f) {
            val progBar = RectF(barRect.left, barRect.top, barRect.left + maxOf(barH, progressW), barRect.bottom)
            val progPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(progBar, barRadius, barRadius, progPaint)
        }

        return bitmap
    }

    // =========================================================================
    // LAYOUT B: SQUARE & TALL FORMAT (aspectRatio < 1.55)
    // =========================================================================

    // Proportional insets clearing rounded corner curves
    val pad = (baseDim * 0.085f).coerceIn(14f * scaleFactor, 22f * scaleFactor)
    val contentLeft = cardRect.left + pad
    val contentRight = cardRect.right - pad
    val contentTop = cardRect.top + pad
    val contentBottom = cardRect.bottom - pad

    val availW = (contentRight - contentLeft).coerceAtLeast(10f)
    val availH = (contentBottom - contentTop).coerceAtLeast(10f)

    // Typography sizing
    val titleSize = (baseDim * 0.082f).coerceIn(10f * scaleFactor, 13.5f * scaleFactor)
    val footerTextSize = (baseDim * 0.076f).coerceIn(9.5f * scaleFactor, 12.5f * scaleFactor)
    val barH = (baseDim * 0.052f).coerceIn(5.5f * scaleFactor, 8.5f * scaleFactor)

    val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = footerTextSize
        typeface = getSlateFont(context, 700)
    }

    val duePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = footerTextSize
        typeface = getSlateFont(context, 700)
    }

    // Determine Single vs Stacked Footer layout
    val milestoneW = if (milestoneText.isNotEmpty()) metaPaint.measureText(milestoneText) else 0f
    val dueW = if (dueText.isNotEmpty()) duePaint.measureText(dueText) else 0f
    val gapBetweenFooter = 10f * scaleFactor

    val isFooterStacked = milestoneText.isNotEmpty() && dueText.isNotEmpty() &&
            (milestoneW + dueW + gapBetweenFooter > availW)

    val footerLinePitch = footerTextSize * 1.30f
    val footerTotalH = if (isFooterStacked) (footerLinePitch + footerTextSize) else footerTextSize

    // Anchor footer upwards from contentBottom so it never collides with the corner
    val footerBottomY = contentBottom
    val footerTopBoundary = footerBottomY - footerTotalH

    // Upper Content Allocation: Distribute Title, Percentage, and Bar inside the safe zone
    val availableUpperH = (footerTopBoundary - contentTop - (10f * scaleFactor)).coerceAtLeast(10f)
    val percentSize = (availableUpperH * 0.44f).coerceIn(24f * scaleFactor, 44f * scaleFactor)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = titleSize
        typeface = getSlateFont(context, 700)
        letterSpacing = 0.02f
    }

    val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = percentSize
        typeface = getSlateFont(context, 800)
    }

    val gap1 = (availableUpperH * 0.12f).coerceIn(4f * scaleFactor, 10f * scaleFactor)
    val gap2 = (availableUpperH * 0.15f).coerceIn(6f * scaleFactor, 12f * scaleFactor)

    // 1. Draw Title
    val titleY = contentTop + titleSize
    val elidedTitle = android.text.TextUtils.ellipsize(
        titleText,
        android.text.TextPaint(titlePaint),
        availW,
        android.text.TextUtils.TruncateAt.END
    ).toString()
    canvas.drawText(elidedTitle, contentLeft, titleY, titlePaint)

    // 2. Draw Percentage
    val percentY = titleY + gap1 + (percentSize * 0.88f)
    canvas.drawText(percentText, contentLeft, percentY, percentPaint)

    // 3. Draw Progress Bar
    val barY = percentY + gap2
    val barRect = RectF(contentLeft, barY, contentRight, barY + barH)
    val barRadius = barH / 2f

    val barTrack = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(18, 0, 0, 0) else Color.argb(28, 255, 255, 255)
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(barRect, barRadius, barRadius, barTrack)

    val progressW = barRect.width() * progressFrac
    if (progressW > 0f) {
        val progBar = RectF(barRect.left, barRect.top, barRect.left + maxOf(barH, progressW), barRect.bottom)
        val progPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(progBar, barRadius, barRadius, progPaint)
    }

    // 4. Draw Footer Anchored Above contentBottom
    if (isFooterStacked) {
        val line2Baseline = footerBottomY - (duePaint.fontMetrics.descent * 0.5f)
        val line1Baseline = line2Baseline - footerLinePitch

        if (milestoneText.isNotEmpty()) {
            val elidedMilestone = android.text.TextUtils.ellipsize(
                milestoneText,
                android.text.TextPaint(metaPaint),
                availW,
                android.text.TextUtils.TruncateAt.END
            ).toString()
            canvas.drawText(elidedMilestone, contentLeft, line1Baseline, metaPaint)
        }

        if (dueText.isNotEmpty()) {
            val elidedDue = android.text.TextUtils.ellipsize(
                dueText,
                android.text.TextPaint(duePaint),
                availW,
                android.text.TextUtils.TruncateAt.END
            ).toString()
            canvas.drawText(elidedDue, contentLeft, line2Baseline, duePaint)
        }
    } else {
        val baselineY = footerBottomY - (metaPaint.fontMetrics.descent * 0.5f)

        if (milestoneText.isNotEmpty()) {
            canvas.drawText(milestoneText, contentLeft, baselineY, metaPaint)
        }

        if (dueText.isNotEmpty()) {
            duePaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(dueText, contentRight, baselineY, duePaint)
        }
    }

    return bitmap
}

// =========================================================================
// 7. WEEKLY HABIT RINGS (Header-Free, Fully Adaptive 3-Way Responsive Engine)
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

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val size = minOf(w, h)
        val leftX = (w - size) / 2f
        val topY = (h - size) / 2f
        RectF(leftX, topY, leftX + size, topY + size)
    }

    val (primaryTextColor, secondaryTextColor) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)

    val cardW = cardRect.width()
    val cardH = cardRect.height()
    val baseDim = minOf(cardW, cardH)
    val aspectRatio = cardW / cardH.coerceAtLeast(1f)

    val defaultColors = listOf(0xFFFF5E3A, 0xFF30D158, 0xFF0A84FF)
    val activeRings = rings.take(3)

    // Average percentage for center display
    val avgFraction = if (activeRings.isNotEmpty()) {
        activeRings.map { (it.currentValue.toFloat() / it.targetValue.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) }.average().toFloat()
    } else 0f
    val percentText = "${(avgFraction * 100).toInt()}%"

    // =========================================================================
    // LAYOUT 1: WIDE FORMAT (aspectRatio >= 1.35, e.g. 4x1, 4x2)
    // Left: Rings | Right: Detailed 3-row habit breakdown
    // =========================================================================
    if (aspectRatio >= 1.35f) {
        val padY = (cardH * 0.10f).coerceIn(10f * scaleFactor, 18f * scaleFactor)
        val padX = (cardH * 0.14f).coerceIn(14f * scaleFactor, 24f * scaleFactor)

        val contentLeft = cardRect.left + padX
        val contentRight = cardRect.right - padX
        val contentTop = cardRect.top + padY
        val contentBottom = cardRect.bottom - padY

        val availW = contentRight - contentLeft
        val availH = contentBottom - contentTop

        val ringBoxSize = minOf(availH, availW * 0.44f)
        val cx = contentLeft + (ringBoxSize / 2f)
        val cy = contentTop + (availH / 2f)

        val maxRadius = ringBoxSize * 0.48f
        val ringStroke = (ringBoxSize * 0.088f).coerceIn(5f * scaleFactor, 14f * scaleFactor)
        val ringGap = ringStroke * 0.35f

        // Draw Concentric Rings
        for (i in 0 until minOf(3, activeRings.size)) {
            val ring = activeRings[i]
            val r = maxRadius - (i * (ringStroke + ringGap))
            if (r <= 0f) continue

            val colorHex = if (ring.colorHex != 0L) ring.colorHex.toInt() else defaultColors.getOrElse(i) { 0xFF30D158 }.toInt()

            val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(32, Color.red(colorHex), Color.green(colorHex), Color.blue(colorHex))
                style = Paint.Style.STROKE
                strokeWidth = ringStroke
            }
            canvas.drawCircle(cx, cy, r, trackPaint)

            val progress = (ring.currentValue.toFloat() / ring.targetValue.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            val sweepAngle = 360f * progress
            if (sweepAngle > 0f) {
                val progPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colorHex
                    style = Paint.Style.STROKE
                    strokeWidth = ringStroke
                    strokeCap = Paint.Cap.ROUND
                }
                val arcRect = RectF(cx - r, cy - r, cx + r, cy + r)
                canvas.drawArc(arcRect, -90f, sweepAngle, false, progPaint)
            }
        }

        // Center Percentage
        val innermostRadius = maxRadius - (2 * (ringStroke + ringGap))
        val percentTextSize = (innermostRadius * 0.65f).coerceIn(12f * scaleFactor, 26f * scaleFactor)
        val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = percentTextSize
            typeface = getSlateFont(context, 800)
            textAlign = Paint.Align.CENTER
        }
        val percentBaseline = cy - ((percentPaint.fontMetrics.ascent + percentPaint.fontMetrics.descent) / 2f)
        canvas.drawText(percentText, cx, percentBaseline, percentPaint)

        // Right Column: Vertical List of Habits
        val listLeft = contentLeft + ringBoxSize + (availW * 0.08f)
        val listRight = contentRight
        val rowPitch = availH / minOf(3, activeRings.size).coerceAtLeast(1).toFloat()

        val labelTextSize = (rowPitch * 0.38f).coerceIn(10.5f * scaleFactor, 15f * scaleFactor)
        val dotR = maxOf(2.5f * scaleFactor, labelTextSize * 0.28f)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = labelTextSize
            typeface = getSlateFont(context, 600)
        }

        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryTextColor
            textSize = labelTextSize * 0.95f
            typeface = getSlateFont(context, 600)
            textAlign = Paint.Align.RIGHT
        }

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        for (i in 0 until minOf(3, activeRings.size)) {
            val ring = activeRings[i]
            val rowCenterY = contentTop + (i * rowPitch) + (rowPitch / 2f)
            val colorHex = if (ring.colorHex != 0L) ring.colorHex.toInt() else defaultColors.getOrElse(i) { 0xFF30D158 }.toInt()

            dotPaint.color = colorHex
            canvas.drawCircle(listLeft + dotR, rowCenterY, dotR, dotPaint)

            val valStr = "${ring.currentValue}/${ring.targetValue} ${ring.unit}".trim()
            val valW = valPaint.measureText(valStr)
            val valBaseline = rowCenterY - ((valPaint.fontMetrics.ascent + valPaint.fontMetrics.descent) / 2f)
            canvas.drawText(valStr, listRight, valBaseline, valPaint)

            val textStartX = listLeft + (dotR * 2.5f) + (6f * scaleFactor)
            val maxLabelW = (listRight - valW - (8f * scaleFactor)) - textStartX
            val labelBaseline = rowCenterY - ((labelPaint.fontMetrics.ascent + labelPaint.fontMetrics.descent) / 2f)

            val displayLabel = ring.label.ifBlank { "Habit ${i + 1}" }
            val elidedLabel = android.text.TextUtils.ellipsize(
                displayLabel,
                android.text.TextPaint(labelPaint),
                maxLabelW.coerceAtLeast(10f),
                android.text.TextUtils.TruncateAt.END
            ).toString()
            canvas.drawText(elidedLabel, textStartX, labelBaseline, labelPaint)
        }

        return bitmap
    }

    // =========================================================================
    // LAYOUT 2: TALL / VERTICAL FORMAT (aspectRatio <= 0.82, e.g. 2x3, 2x4)
    // Top: Maximize Ring Diameter | Bottom: Vertical 3-row Habit List
    // =========================================================================
    if (aspectRatio <= 0.82f) {
        val pad = (cardW * 0.08f).coerceIn(12f * scaleFactor, 22f * scaleFactor)
        val contentLeft = cardRect.left + pad
        val contentRight = cardRect.right - pad
        val contentTop = cardRect.top + pad
        val contentBottom = cardRect.bottom - pad

        val availW = contentRight - contentLeft
        val availH = contentBottom - contentTop

        // Rings take top section matching width
        val ringDiameter = minOf(availW * 0.90f, availH * 0.52f)
        val cx = cardRect.centerX()
        val cy = contentTop + (ringDiameter / 2f)

        val maxRadius = ringDiameter / 2f
        val ringStroke = (ringDiameter * 0.088f).coerceIn(5.5f * scaleFactor, 14f * scaleFactor)
        val ringGap = ringStroke * 0.35f

        for (i in 0 until minOf(3, activeRings.size)) {
            val ring = activeRings[i]
            val r = maxRadius - (i * (ringStroke + ringGap))
            if (r <= 0f) continue

            val colorHex = if (ring.colorHex != 0L) ring.colorHex.toInt() else defaultColors.getOrElse(i) { 0xFF30D158 }.toInt()

            val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(32, Color.red(colorHex), Color.green(colorHex), Color.blue(colorHex))
                style = Paint.Style.STROKE
                strokeWidth = ringStroke
            }
            canvas.drawCircle(cx, cy, r, trackPaint)

            val progress = (ring.currentValue.toFloat() / ring.targetValue.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            val sweepAngle = 360f * progress
            if (sweepAngle > 0f) {
                val progPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colorHex
                    style = Paint.Style.STROKE
                    strokeWidth = ringStroke
                    strokeCap = Paint.Cap.ROUND
                }
                val arcRect = RectF(cx - r, cy - r, cx + r, cy + r)
                canvas.drawArc(arcRect, -90f, sweepAngle, false, progPaint)
            }
        }

        // Center Percentage
        val innermostRadius = maxRadius - (2 * (ringStroke + ringGap))
        val percentTextSize = (innermostRadius * 0.65f).coerceIn(12f * scaleFactor, 28f * scaleFactor)
        val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = percentTextSize
            typeface = getSlateFont(context, 800)
            textAlign = Paint.Align.CENTER
        }
        val percentBaseline = cy - ((percentPaint.fontMetrics.ascent + percentPaint.fontMetrics.descent) / 2f)
        canvas.drawText(percentText, cx, percentBaseline, percentPaint)

        // Bottom Section: Stacks 3 habits vertically to prevent any truncation
        val listStartY = cy + maxRadius + (cardH * 0.04f)
        val listAvailH = (contentBottom - listStartY).coerceAtLeast(10f)
        val rowPitch = listAvailH / minOf(3, activeRings.size).coerceAtLeast(1).toFloat()

        val labelTextSize = (rowPitch * 0.40f).coerceIn(10f * scaleFactor, 14f * scaleFactor)
        val dotR = maxOf(2.5f * scaleFactor, labelTextSize * 0.28f)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = labelTextSize
            typeface = getSlateFont(context, 600)
        }

        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryTextColor
            textSize = labelTextSize * 0.95f
            typeface = getSlateFont(context, 600)
            textAlign = Paint.Align.RIGHT
        }

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        for (i in 0 until minOf(3, activeRings.size)) {
            val ring = activeRings[i]
            val rowCenterY = listStartY + (i * rowPitch) + (rowPitch / 2f)
            val colorHex = if (ring.colorHex != 0L) ring.colorHex.toInt() else defaultColors.getOrElse(i) { 0xFF30D158 }.toInt()

            dotPaint.color = colorHex
            canvas.drawCircle(contentLeft + dotR, rowCenterY, dotR, dotPaint)

            val valStr = "${ring.currentValue}/${ring.targetValue} ${ring.unit}".trim()
            val valW = valPaint.measureText(valStr)
            val valBaseline = rowCenterY - ((valPaint.fontMetrics.ascent + valPaint.fontMetrics.descent) / 2f)
            canvas.drawText(valStr, contentRight, valBaseline, valPaint)

            val textStartX = contentLeft + (dotR * 2.5f) + (6f * scaleFactor)
            val maxLabelW = (contentRight - valW - (8f * scaleFactor)) - textStartX
            val labelBaseline = rowCenterY - ((labelPaint.fontMetrics.ascent + labelPaint.fontMetrics.descent) / 2f)

            val displayLabel = ring.label.ifBlank { "Habit ${i + 1}" }
            val elidedLabel = android.text.TextUtils.ellipsize(
                displayLabel,
                android.text.TextPaint(labelPaint),
                maxLabelW.coerceAtLeast(10f),
                android.text.TextUtils.TruncateAt.END
            ).toString()
            canvas.drawText(elidedLabel, textStartX, labelBaseline, labelPaint)
        }

        return bitmap
    }

    // =========================================================================
    // LAYOUT 3: SQUARE & BALANCED FORMAT (0.82 < aspectRatio < 1.35, e.g. 2x2, 3x3)
    // Vertically Centered Unit: Maximize Rings + Clean Bottom Legend Row
    // =========================================================================
    val pad = (baseDim * 0.08f).coerceIn(12f * scaleFactor, 22f * scaleFactor)
    val contentLeft = cardRect.left + pad
    val contentRight = cardRect.right - pad
    val contentTop = cardRect.top + pad
    val contentBottom = cardRect.bottom - pad

    val availW = contentRight - contentLeft
    val availH = contentBottom - contentTop

    val legendTextSize = (baseDim * 0.085f).coerceIn(10f * scaleFactor, 14f * scaleFactor)
    val legendH = legendTextSize * 1.3f
    val gap = (baseDim * 0.07f).coerceIn(8f * scaleFactor, 16f * scaleFactor)

    // Rings allocate ~80% of available height
    val availRingH = (availH - legendH - gap).coerceAtLeast(20f)
    val ringDiameter = minOf(availRingH, availW * 0.88f)
    val cx = cardRect.centerX()

    val totalBlockH = ringDiameter + gap + legendH
    val startY = contentTop + ((availH - totalBlockH) / 2f).coerceAtLeast(0f)

    val cy = startY + (ringDiameter / 2f)
    val maxRadius = ringDiameter / 2f
    val ringStroke = (ringDiameter * 0.088f).coerceIn(5.5f * scaleFactor, 14f * scaleFactor)
    val ringGap = ringStroke * 0.38f

    for (i in 0 until minOf(3, activeRings.size)) {
        val ring = activeRings[i]
        val r = maxRadius - (i * (ringStroke + ringGap))
        if (r <= 0f) continue

        val colorHex = if (ring.colorHex != 0L) ring.colorHex.toInt() else defaultColors.getOrElse(i) { 0xFF30D158 }.toInt()

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(32, Color.red(colorHex), Color.green(colorHex), Color.blue(colorHex))
            style = Paint.Style.STROKE
            strokeWidth = ringStroke
        }
        canvas.drawCircle(cx, cy, r, trackPaint)

        val progress = (ring.currentValue.toFloat() / ring.targetValue.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        val sweepAngle = 360f * progress
        if (sweepAngle > 0f) {
            val progPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorHex
                style = Paint.Style.STROKE
                strokeWidth = ringStroke
                strokeCap = Paint.Cap.ROUND
            }
            val arcRect = RectF(cx - r, cy - r, cx + r, cy + r)
            canvas.drawArc(arcRect, -90f, sweepAngle, false, progPaint)
        }
    }

    // Proportional Center Percentage
    val innermostRadius = maxRadius - (2 * (ringStroke + ringGap))
    val percentTextSize = (innermostRadius * 0.5f).coerceIn(14f * scaleFactor, 32f * scaleFactor)
    val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = percentTextSize
        typeface = getSlateFont(context, 700)
        textAlign = Paint.Align.CENTER
    }
    val percentBaseline = cy - ((percentPaint.fontMetrics.ascent + percentPaint.fontMetrics.descent) / 2f)
    canvas.drawText(percentText, cx, percentBaseline, percentPaint)

    // Bottom Legend (Evenly Distributes 3 Columns)
    val legendCenterY = startY + ringDiameter + gap + (legendH / 2f)
    val legendColW = availW / minOf(3, activeRings.size).coerceAtLeast(1)
    val dotRadius = maxOf(2.5f * scaleFactor, legendTextSize * 0.28f)

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = legendTextSize
        typeface = getSlateFont(context, 600)
    }

    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    for (i in 0 until minOf(3, activeRings.size)) {
        val ring = activeRings[i]
        val colX = contentLeft + (i * legendColW)
        val colorHex = if (ring.colorHex != 0L) ring.colorHex.toInt() else defaultColors.getOrElse(i) { 0xFF30D158 }.toInt()

        dotPaint.color = colorHex
        val dotCy = legendCenterY
        val dotCx = colX + dotRadius
        canvas.drawCircle(dotCx, dotCy, dotRadius, dotPaint)

        val labelBaseline = legendCenterY - ((labelPaint.fontMetrics.ascent + labelPaint.fontMetrics.descent) / 2f)
        val textStartX = dotCx + dotRadius + (5f * scaleFactor)
        val maxLabelW = (colX + legendColW) - textStartX - (3f * scaleFactor)

        val displayLabel = ring.label.ifBlank { "Habit ${i + 1}" }
        val elided = android.text.TextUtils.ellipsize(
            displayLabel,
            android.text.TextPaint(labelPaint),
            maxLabelW.coerceAtLeast(10f),
            android.text.TextUtils.TruncateAt.END
        ).toString()
        canvas.drawText(elided, textStartX, labelBaseline, labelPaint)
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
    val ( bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
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
