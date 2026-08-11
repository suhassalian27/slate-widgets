package com.altusix.slate.widgets.calendar

import android.content.Context
import android.graphics.*
import com.altusix.slate.data.local.SlateWidgetConfig

// --- Helper for safe background color ---
private fun getSafeBgColor(config: SlateWidgetConfig): Int {
    val alphaInt = ((if (config.opacity < 0.15f) 1.0f else config.opacity) * 255).toInt()
    val rawHex = config.backgroundColorHex.toInt()
    val r = ((rawHex shr 16) and 0xFF)
    val g = ((rawHex shr 8) and 0xFF)
    val b = (rawHex and 0xFF)
    return Color.argb(alphaInt, r, g, b)
}

// 1. CAPSULE PILL (2x1) - Fixed Height & Auto-Fitting Text
fun generatePillCalendarBitmap(
    context: Context,
    state: CalendarPillState,
    config: SlateWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val widthPx = (wDp * density).toInt().coerceAtLeast((140 * density).toInt())
    val heightPx = (hDp * density).toInt().coerceAtLeast((50 * density).toInt())

    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = widthPx.toFloat()
    val h = heightPx.toFloat()

    // 1. EXACT BOUNDS & FIXED HEIGHT CALCULATIONS
    val padding = 6f * density
    val availW = w - (padding * 2f)
    val availH = h - (padding * 2f)

    // Lock height to 62dp max so resizing on home screen expands width only
    val targetHDp = 65f
    val bodyH = (targetHDp * density).coerceAtMost(availH)
    val bodyW = availW // Stretch horizontally across 2x1 cell space

    val startX = (w - bodyW) / 2f
    val startY = (h - bodyH) / 2f

    val strokeW = (bodyH * 0.045f).coerceIn(3f, 6f)
    val cardRect = RectF(
        startX + (strokeW / 2f),
        startY + (strokeW / 2f),
        startX + bodyW - (strokeW / 2f),
        startY + bodyH - (strokeW / 2f)
    )
    val cardRadius = bodyH * 0.28f

    // Colors
    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val shellBgColor = if (isLight) Color.parseColor("#FFFFFF") else Color.parseColor("#141416")
    val strokeColor = if (isLight) Color.parseColor("#FFFFFF") else Color.parseColor("#141416")
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    val shellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = shellBgColor
        style = Paint.Style.FILL
    }

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = strokeW
    }

    // 2. DRAW SLATE CARD CONTAINER
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, shellPaint)
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, strokePaint)

    // 3. LEFT FAR-EDGE ACCENT BAR
    val accentBarW = (bodyH * 0.05f).coerceAtLeast(3f * density)
    val accentBarMargin = bodyH * 0.18f
    val accentBarRect = RectF(
        cardRect.left + accentBarMargin,
        cardRect.top + accentBarMargin,
        cardRect.left + accentBarMargin + accentBarW,
        cardRect.bottom - accentBarMargin
    )
    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(accentBarRect, accentBarW / 2f, accentBarW / 2f, accentPaint)

    // 4. DATE NUMBER
    val dateStartX = accentBarRect.right + (bodyH * 0.14f)
    val dateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = bodyH * 0.48f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.LEFT
    }
    val fmDate = dateTextPaint.fontMetrics
    val centerY = cardRect.centerY()
    val dateY = centerY - ((fmDate.descent + fmDate.ascent) / 2f)
    canvas.drawText(state.dayOfMonth, dateStartX, dateY, dateTextPaint)

    // 5. VERTICAL DIVIDER LINE
    val dateW = dateTextPaint.measureText(state.dayOfMonth)
    val dividerX = dateStartX + dateW + (bodyH * 0.14f)
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = (bodyH * 0.035f).coerceAtLeast(2f)
    }
    canvas.drawLine(
        dividerX,
        cardRect.top + (bodyH * 0.22f),
        dividerX,
        cardRect.bottom - (bodyH * 0.22f),
        dividerPaint
    )

    // 6. RIGHT SIDE DETAILS (Month & Full Day with Dynamic Bounds Fitting)
    val textStartX = dividerX + (bodyH * 0.14f)
    val maxAllowedTextW = (cardRect.right - textStartX - (bodyH * 0.14f)).coerceAtLeast(10f)

    // Month (AUG)
    var monthTextSize = bodyH * 0.28f
    val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = monthTextSize
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.LEFT
    }
    val monthTextWidth = monthPaint.measureText(state.monthShort)
    if (monthTextWidth > maxAllowedTextW) {
        monthTextSize *= (maxAllowedTextW / monthTextWidth)
        monthPaint.textSize = monthTextSize
    }
    canvas.drawText(state.monthShort, textStartX, cardRect.top + (bodyH * 0.42f), monthPaint)

    // Full Weekday Name (Auto-scales down if long, e.g. "Wednesday")
    var dayTextSize = bodyH * 0.22f
    val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = dayTextSize
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.LEFT
    }
    val dayTextWidth = dayPaint.measureText(state.dayOfWeekFull)
    if (dayTextWidth > maxAllowedTextW) {
        dayTextSize *= (maxAllowedTextW / dayTextWidth)
        dayPaint.textSize = dayTextSize
    }
    canvas.drawText(state.dayOfWeekFull, textStartX, cardRect.top + (bodyH * 0.74f), dayPaint)

    return bitmap
}

// 2. BIG DATE (2x2)
fun generateDate2x2Bitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast((140 * density).toInt())
    val h = (hDp * density).toInt().coerceAtLeast((140 * density).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val isLight = config.themeMode == "LIGHT"

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = getSafeBgColor(config) }
    val cardRadius = 22f * density
    canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), cardRadius, cardRadius, bgPaint)

    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#B3FFFFFF")
    val pad = 16f * density

    // Top Header (AUG 2026)
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = 14f * density
        typeface = Typeface.DEFAULT_BOLD
    }
    canvas.drawText("${state.monthShort} ${state.year}", pad, pad + (12f * density), headerPaint)

    // Giant Date
    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = 64f * density
        typeface = Typeface.DEFAULT_BOLD
    }
    val dateY = h / 2f + (20f * density)
    canvas.drawText(state.dayOfMonth, pad, dateY, datePaint)

    // Day of Week
    val dateWidth = datePaint.measureText(state.dayOfMonth)
    val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = 16f * density
        typeface = Typeface.DEFAULT_BOLD
    }
    canvas.drawText(state.dayOfWeekShort, pad + dateWidth + (8f * density), dateY - (10f * density), dayPaint)

    return bitmap
}

// 3. MONTH GRID (2x2)
fun generateMonthGrid2x2Bitmap(context: Context, state: CalendarMonthState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast((140 * density).toInt())
    val h = (hDp * density).toInt().coerceAtLeast((140 * density).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val isLight = config.themeMode == "LIGHT"

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = getSafeBgColor(config) }
    val cardRadius = 22f * density
    canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), cardRadius, cardRadius, bgPaint)

    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val sundayRed = Color.parseColor("#FF3B30")
    val pad = 12f * density
    val contentW = w - (pad * 2f)

    // Header
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = 14f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(state.monthShort, w / 2f, pad + (10f * density), headerPaint)

    // Weekdays
    val headerY = pad + (28f * density)
    val headers = listOf("M", "T", "W", "T", "F", "S", "S")
    val colW = contentW / 7f
    val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    headers.forEachIndexed { i, title ->
        dayPaint.color = if (i == 6) sundayRed else primaryText
        canvas.drawText(title, pad + (i * colW) + (colW / 2f), headerY, dayPaint)
    }

    // Grid
    val gridY = headerY + (12f * density)
    val rowH = (h - gridY - pad) / 6f
    val todayBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }

    state.daysGrid.forEachIndexed { index, day ->
        val r = index / 7
        val c = index % 7
        if (r >= 6) return@forEachIndexed

        val cx = pad + (c * colW) + (colW / 2f)
        val cy = gridY + (r * rowH) + (rowH / 2f)

        if (day.isToday) {
            val badgeR = minOf(colW, rowH) * 0.42f
            canvas.drawRoundRect(RectF(cx - badgeR, cy - badgeR, cx + badgeR, cy + badgeR), 6f * density, 6f * density, todayBadgePaint)
        }

        if (day.isCurrentMonth) {
            dayPaint.color = if (day.isSunday) sundayRed else primaryText
            val fm = dayPaint.fontMetrics
            canvas.drawText(day.dayNumber.toString(), cx, cy - ((fm.descent + fm.ascent) / 2f), dayPaint)
        }
    }

    return bitmap
}