package com.altusix.slate.widgets.calendar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.createSupersampledCanvas
import com.altusix.slate.utils.getSafeBgColor
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius

// 1. CAPSULE PILL (2x1)
fun generatePillCalendarBitmap(context: Context, state: CalendarPillState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val margin = scaleFactor * 1.5f
    val targetRatio = 2.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)
    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val shellBgColor = getSafeBgColor(config)
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val dividerColor = if (isLight) Color.parseColor("#1A000000") else Color.parseColor("#26FFFFFF")

    val shellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = shellBgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, shellPaint)

    val aspectRatio = cardRect.width() / cardRect.height()

    if (aspectRatio >= 1.2f) {
        val bodyH = cardRect.height()
        val padX = bodyH * 0.18f

        val accentW = (bodyH * 0.05f).coerceIn(3.5f * scaleFactor, 8f * scaleFactor)
        val accentH = bodyH * 0.48f
        val accentRect = RectF(cardRect.left + padX, cardRect.centerY() - (accentH / 2f), cardRect.left + padX + accentW, cardRect.centerY() + (accentH / 2f))
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(accentRect, accentW / 2f, accentW / 2f, accentPaint)

        val gap = bodyH * 0.12f
        val dateStartX = accentRect.right + gap

        var dateTextSize = bodyH * 0.46f
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = dateTextSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.LEFT
        }
        val fmDate = datePaint.fontMetrics
        val dateY = cardRect.centerY() - ((fmDate.descent + fmDate.ascent) / 2f)
        canvas.drawText(state.dayOfMonth, dateStartX, dateY, datePaint)

        val dateW = datePaint.measureText(state.dayOfMonth)
        val dividerX = dateStartX + dateW + gap
        val strokeW = (2f * scaleFactor).coerceAtLeast(1.5f * scaleFactor)
        val dividerH = bodyH * 0.44f
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dividerColor
            style = Paint.Style.STROKE
            strokeWidth = strokeW
        }
        canvas.drawLine(dividerX, cardRect.centerY() - (dividerH / 2f), dividerX, cardRect.centerY() + (dividerH / 2f), dividerPaint)

        val textStartX = dividerX + strokeW + gap
        val maxAllowedTextW = (cardRect.right - textStartX - padX).coerceAtLeast(10f * scaleFactor)

        var monthTextSize = bodyH * 0.26f
        val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = monthTextSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.LEFT
        }
        if (monthPaint.measureText(state.monthShort) > maxAllowedTextW) {
            monthTextSize *= (maxAllowedTextW / monthPaint.measureText(state.monthShort))
            monthPaint.textSize = monthTextSize
        }
        val fmM = monthPaint.fontMetrics

        var dayTextSize = bodyH * 0.20f
        val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = dayTextSize
            typeface = getSlateFont(context, weight = 400)
            textAlign = Paint.Align.LEFT
        }
        if (dayPaint.measureText(state.dayOfWeekFull) > maxAllowedTextW) {
            dayTextSize *= (maxAllowedTextW / dayPaint.measureText(state.dayOfWeekFull))
            dayPaint.textSize = dayTextSize
        }
        val fmD = dayPaint.fontMetrics

        val textGap = bodyH * 0.03f
        val mH = -fmM.ascent + fmM.descent
        val dH = -fmD.ascent + fmD.descent
        val totalTextStackH = mH + textGap + dH

        val textStackTop = cardRect.centerY() - (totalTextStackH / 2f)
        val monthY = textStackTop - fmM.ascent
        val dayY = monthY + fmM.descent + textGap - fmD.ascent

        canvas.drawText(state.monthShort, textStartX, monthY, monthPaint)
        canvas.drawText(state.dayOfWeekFull, textStartX, dayY, dayPaint)

    } else {
        val maxW = cardRect.width() * 0.84f

        val accentW = (cardRect.width() * 0.32f).coerceAtMost(cardRect.height() * 0.20f)
        val accentH = (cardRect.height() * 0.035f).coerceIn(3.5f * scaleFactor, 8f * scaleFactor)
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }

        var dateTextSize = minOf(cardRect.width() * 0.42f, cardRect.height() * 0.30f)
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = dateTextSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }
        if (datePaint.measureText(state.dayOfMonth) > maxW) {
            dateTextSize *= (maxW / datePaint.measureText(state.dayOfMonth))
            datePaint.textSize = dateTextSize
        }
        val fmDate = datePaint.fontMetrics

        var monthTextSize = minOf(cardRect.width() * 0.22f, cardRect.height() * 0.16f)
        val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = monthTextSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }
        if (monthPaint.measureText(state.monthShort) > maxW) {
            monthTextSize *= (maxW / monthPaint.measureText(state.monthShort))
            monthPaint.textSize = monthTextSize
        }
        val fmM = monthPaint.fontMetrics

        var dayTextSize = minOf(cardRect.width() * 0.15f, cardRect.height() * 0.11f)
        val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = dayTextSize
            typeface = getSlateFont(context, weight = 400)
            textAlign = Paint.Align.CENTER
        }
        if (dayPaint.measureText(state.dayOfWeekFull) > maxW) {
            dayTextSize *= (maxW / dayPaint.measureText(state.dayOfWeekFull))
            dayPaint.textSize = dayTextSize
        }
        val fmD = dayPaint.fontMetrics

        val gap1 = cardRect.height() * 0.05f
        val gap2 = cardRect.height() * 0.02f
        val gap3 = cardRect.height() * 0.015f

        val dateH = -fmDate.ascent + fmDate.descent
        val monthH = -fmM.ascent + fmM.descent
        val dayH = -fmD.ascent + fmD.descent

        val totalStackH = accentH + gap1 + dateH + gap2 + monthH + gap3 + dayH
        val stackTop = cardRect.centerY() - (totalStackH / 2f)

        val accentRect = RectF(cardRect.centerX() - (accentW / 2f), stackTop, cardRect.centerX() + (accentW / 2f), stackTop + accentH)
        canvas.drawRoundRect(accentRect, accentH / 2f, accentH / 2f, accentPaint)

        val dateY = stackTop + accentH + gap1 - fmDate.ascent
        val monthY = dateY + fmDate.descent + gap2 - fmM.ascent
        val dayY = monthY + fmM.descent + gap3 - fmD.ascent

        val cx = cardRect.centerX()
        canvas.drawText(state.dayOfMonth, cx, dateY, datePaint)
        canvas.drawText(state.monthShort, cx, monthY, monthPaint)
        canvas.drawText(state.dayOfWeekFull, cx, dayY, dayPaint)
    }

    return bitmap
}

fun generatePillCalendarBitmap(context: Context, state: CalendarPillState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generatePillCalendarBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 2. BASIC CALENDAR (4x2)
fun generateBasicCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val margin = scaleFactor * 1.5f
    val targetRatio = 2.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val sundayAccent = Color.parseColor("#E53935")

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = getSafeBgColor(config) }
    val cardRadius = getStandardCornerRadius(scaleFactor)
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val cal = java.util.Calendar.getInstance().apply {
        val yearInt = state.year.toString().toIntOrNull() ?: get(java.util.Calendar.YEAR)
        try {
            val date = java.text.SimpleDateFormat("MMM", java.util.Locale.ENGLISH).parse(state.monthShort)
            if (date != null) {
                val tempCal = java.util.Calendar.getInstance().apply { time = date }
                set(java.util.Calendar.MONTH, tempCal.get(java.util.Calendar.MONTH))
            }
        } catch (_: Exception) {}
        set(java.util.Calendar.YEAR, yearInt)
        set(java.util.Calendar.DAY_OF_MONTH, 1)
    }

    val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val startOffset = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
    val currentDayNum = state.dayOfMonth.trim().toIntOrNull() ?: java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)

    val totalCells = startOffset + daysInMonth
    val dateRowsNeeded = kotlin.math.ceil(totalCells / 7.0).toInt()

    val monthTitle = try {
        java.text.SimpleDateFormat("MMMM", java.util.Locale.ENGLISH).format(cal.time).uppercase()
    } catch (_: Exception) {
        if (state.monthShort.isNotBlank()) state.monthShort.uppercase() else "AUGUST"
    }

    val topPadding = cardRect.height() * 0.04f
    val bottomPadding = cardRect.height() * 0.04f
    val sidePadding = cardRect.width() * 0.04f
    val usableWidth = cardRect.width() - (sidePadding * 2f)

    val headerTextSize = (cardRect.height() * 0.13f).coerceIn(12f * scaleFactor, 22f * scaleFactor)
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = headerTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.08f
    }

    val fmHeader = headerPaint.fontMetrics
    val headerY = cardRect.top + topPadding - fmHeader.ascent
    canvas.drawText(monthTitle, cardRect.centerX(), headerY, headerPaint)

    val gapToGrid = cardRect.height() * 0.02f
    val gridTopY = headerY + fmHeader.descent + gapToGrid
    val availableGridHeight = cardRect.bottom - gridTopY - bottomPadding
    val totalGridRows = dateRowsNeeded + 1
    val rowHeight = availableGridHeight / totalGridRows
    val colWidth = usableWidth / 7f

    val fontScale = minOf(colWidth * 0.50f, rowHeight * 0.52f).coerceAtLeast(9f * scaleFactor)

    val dayHeaderLabels = arrayOf("M", "T", "W", "T", "F", "S", "S")
    val dayHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = fontScale
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val fmDayHeader = dayHeaderPaint.fontMetrics
    val dayHeaderY = gridTopY + (rowHeight / 2f) - ((fmDayHeader.descent + fmDayHeader.ascent) / 2f)

    for (col in 0..6) {
        val cx = cardRect.left + sidePadding + (col * colWidth) + (colWidth / 2f)
        dayHeaderPaint.color = if (col == 6) sundayAccent else primaryText
        canvas.drawText(dayHeaderLabels[col], cx, dayHeaderY, dayHeaderPaint)
    }

    val dateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = fontScale
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val r = Color.red(accentColorInt)
    val g = Color.green(accentColorInt)
    val b = Color.blue(accentColorInt)
    val badgeLuminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    val activeTextColor = if (badgeLuminance > 0.65) Color.parseColor("#161618") else Color.WHITE

    val activeBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }

    var currentCol = startOffset
    var currentRow = 1

    for (day in 1..daysInMonth) {
        val cx = cardRect.left + sidePadding + (currentCol * colWidth) + (colWidth / 2f)
        val cy = gridTopY + (currentRow * rowHeight) + (rowHeight / 2f)

        if (day == currentDayNum) {
            val badgeSize = minOf(colWidth * 0.85f, rowHeight * 0.85f)
            val badgeRect = RectF(cx - (badgeSize / 2f), cy - (badgeSize / 2f), cx + (badgeSize / 2f), cy + (badgeSize / 2f))
            val badgeRadius = (scaleFactor * 6f).coerceAtMost(badgeSize * 0.28f)
            canvas.drawRoundRect(badgeRect, badgeRadius, badgeRadius, activeBadgePaint)

            dateTextPaint.color = activeTextColor
        } else {
            dateTextPaint.color = if (currentCol == 6) sundayAccent else primaryText
        }

        val fmDate = dateTextPaint.fontMetrics
        val textY = cy - ((fmDate.descent + fmDate.ascent) / 2f)
        canvas.drawText(day.toString(), cx, textY, dateTextPaint)

        currentCol++
        if (currentCol > 6) {
            currentCol = 0
            currentRow++
        }
    }

    return bitmap
}

fun generateBasicCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateBasicCalendarBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 3. BIG DATE (2x2 Square)
fun generateBigDateBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)

    val margin = scaleFactor * 1.5f
    val rect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardCornerRadius, cardCornerRadius, bgPaint)

    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#B3FFFFFF")

    val cardSizeRef = minOf(rect.width(), rect.height())
    val pad = cardSizeRef * 0.12f

    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = cardSizeRef * 0.10f
        typeface = getSlateFont(context, weight = 700)
    }
    val headerY = rect.top + pad + headerPaint.textSize
    canvas.drawText("${state.monthShort.uppercase()} ${state.year}", rect.left + pad, headerY, headerPaint)

    val dateText = state.dayOfMonth
    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = cardSizeRef * 0.42f
        typeface = getSlateFont(context, weight = 700)
    }

    val dateBounds = Rect()
    datePaint.getTextBounds(dateText, 0, dateText.length, dateBounds)

    val remainingHeight = rect.bottom - headerY
    val dateY = headerY + (remainingHeight / 2f) + (dateBounds.height() / 2f) - (4f * scaleFactor)
    canvas.drawText(dateText, rect.left + pad, dateY, datePaint)

    val dateWidth = datePaint.measureText(dateText)
    val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = cardSizeRef * 0.12f
        typeface = getSlateFont(context, weight = 700)
    }

    val dayX = rect.left + pad + dateWidth + (cardSizeRef * 0.05f)
    canvas.drawText(state.dayOfWeekShort.uppercase(), dayX, dateY - (dateBounds.height() * 0.10f), dayPaint)

    return bitmap
}

fun generateBigDateBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateBigDateBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 4. MONTH OVERLAY CALENDAR (4x2)
fun generateWatermarkCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val margin = scaleFactor * 1.5f
    val targetRatio = 2.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val sundayAccent = Color.parseColor("#E53935")
    val watermarkColor = if (isLight) Color.parseColor("#12000000") else Color.parseColor("#1AFFFFFF")

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = getSafeBgColor(config) }
    val cardRadius = getStandardCornerRadius(scaleFactor)
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val cal = java.util.Calendar.getInstance().apply {
        val yearInt = state.year.toString().toIntOrNull() ?: get(java.util.Calendar.YEAR)
        try {
            val parsedDate = java.text.SimpleDateFormat("MMM", java.util.Locale.ENGLISH).parse(state.monthShort)
            if (parsedDate != null) {
                val tempCal = java.util.Calendar.getInstance().apply { time = parsedDate }
                set(java.util.Calendar.MONTH, tempCal.get(java.util.Calendar.MONTH))
            }
        } catch (_: Exception) {}
        set(java.util.Calendar.YEAR, yearInt)
        set(java.util.Calendar.DAY_OF_MONTH, 1)
    }

    val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val startOffset = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
    val currentDayNum = state.dayOfMonth.trim().toIntOrNull()
        ?: state.monthShort.trim().toIntOrNull()
        ?: java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)

    val totalCells = startOffset + daysInMonth
    val dateRowsNeeded = kotlin.math.ceil(totalCells / 7.0).toInt()

    val monthName = java.text.SimpleDateFormat("MMM", java.util.Locale.ENGLISH).format(cal.time).uppercase()

    val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = watermarkColor
        textSize = minOf(cardRect.width() * 0.38f, cardRect.height() * 0.72f)
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }
    val watermarkBounds = Rect()
    watermarkPaint.getTextBounds(monthName, 0, monthName.length, watermarkBounds)
    val watermarkY = cardRect.centerY() + (watermarkBounds.height() / 2f) - watermarkBounds.bottom
    canvas.drawText(monthName, cardRect.centerX(), watermarkY, watermarkPaint)

    val topPadding = cardRect.height() * 0.04f
    val bottomPadding = cardRect.height() * 0.04f
    val sidePadding = cardRect.width() * 0.04f
    val usableWidth = cardRect.width() - (sidePadding * 2f)

    val gridTopY = cardRect.top + topPadding
    val availableGridHeight = cardRect.bottom - gridTopY - bottomPadding
    val totalGridRows = dateRowsNeeded + 1
    val rowHeight = availableGridHeight / totalGridRows
    val colWidth = usableWidth / 7f

    val fontScale = minOf(colWidth * 0.50f, rowHeight * 0.52f).coerceAtLeast(9f * scaleFactor)

    val dayHeaderLabels = arrayOf("M", "T", "W", "T", "F", "S", "S")
    val dayHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = fontScale
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val fmDayHeader = dayHeaderPaint.fontMetrics
    val dayHeaderY = gridTopY + (rowHeight / 2f) - ((fmDayHeader.descent + fmDayHeader.ascent) / 2f)
    for (col in 0..6) {
        val cx = cardRect.left + sidePadding + (col * colWidth) + (colWidth / 2f)
        dayHeaderPaint.color = if (col == 6) sundayAccent else primaryText
        canvas.drawText(dayHeaderLabels[col], cx, dayHeaderY, dayHeaderPaint)
    }

    val dateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = fontScale
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val r = Color.red(accentColorInt)
    val g = Color.green(accentColorInt)
    val b = Color.blue(accentColorInt)
    val badgeLuminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    val activeTextColor = if (badgeLuminance > 0.65) Color.parseColor("#161618") else Color.WHITE

    val activeBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }

    var currentCol = startOffset
    var currentRow = 1

    for (day in 1..daysInMonth) {
        val cx = cardRect.left + sidePadding + (currentCol * colWidth) + (colWidth / 2f)
        val cy = gridTopY + (currentRow * rowHeight) + (rowHeight / 2f)

        if (day == currentDayNum) {
            val badgeSize = minOf(colWidth * 0.85f, rowHeight * 0.85f)
            val badgeRect = RectF(cx - (badgeSize / 2f), cy - (badgeSize / 2f), cx + (badgeSize / 2f), cy + (badgeSize / 2f))
            val badgeRadius = (scaleFactor * 6f).coerceAtMost(badgeSize * 0.28f)
            canvas.drawRoundRect(badgeRect, badgeRadius, badgeRadius, activeBadgePaint)

            dateTextPaint.color = activeTextColor
        } else {
            dateTextPaint.color = if (currentCol == 6) sundayAccent else primaryText
        }

        val fmDate = dateTextPaint.fontMetrics
        val textY = cy - ((fmDate.descent + fmDate.ascent) / 2f)
        canvas.drawText(day.toString(), cx, textY, dateTextPaint)

        currentCol++
        if (currentCol > 6) {
            currentCol = 0
            currentRow++
        }
    }

    return bitmap
}

fun generateWatermarkCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateWatermarkCalendarBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 5. CALENDAR PAGE (2x2 Square)
fun generateCalendarPageBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bodyBgColor = getSafeBgColor(config)

    val margin = scaleFactor * 1.5f
    val rect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)

    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bodyBgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardRadius, cardRadius, bodyPaint)

    val bannerH = rect.height() * 0.28f
    val bannerRect = RectF(rect.left, rect.top, rect.right, rect.top + bannerH)

    val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }

    canvas.save()
    val clipPath = Path().apply { addRoundRect(rect, cardRadius, cardRadius, Path.Direction.CW) }
    canvas.clipPath(clipPath)
    canvas.drawRect(bannerRect, bannerPaint)
    canvas.restore()

    val r = ((accentColorInt shr 16) and 0xFF) / 255f
    val g = ((accentColorInt shr 8) and 0xFF) / 255f
    val b = (accentColorInt and 0xFF) / 255f
    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
    val bannerTextColor = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

    val fullDayName = when (state.dayOfWeekShort.uppercase()) {
        "MON" -> "MONDAY"
        "TUE" -> "TUESDAY"
        "WED" -> "WEDNESDAY"
        "THU" -> "THURSDAY"
        "FRI" -> "FRIDAY"
        "SAT" -> "SATURDAY"
        "SUN" -> "SUNDAY"
        else -> state.dayOfWeekShort.uppercase()
    }

    val baseBannerTextSize = bannerH * 0.48f
    val bannerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bannerTextColor
        textSize = baseBannerTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val maxBannerWidth = bannerRect.width() * 0.86f
    val measuredBannerWidth = bannerTextPaint.measureText(fullDayName)
    if (measuredBannerWidth > maxBannerWidth) {
        bannerTextPaint.textSize = baseBannerTextSize * (maxBannerWidth / measuredBannerWidth)
    }

    val bannerTextY = bannerRect.centerY() + (bannerTextPaint.textSize / 3f) - (1f * scaleFactor)
    canvas.drawText(fullDayName, bannerRect.centerX(), bannerTextY, bannerTextPaint)

    val bodyAreaRect = RectF(rect.left, bannerRect.bottom, rect.right, rect.bottom)
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#636366") else Color.parseColor("#B3FFFFFF")

    val fullMonthName = when (state.monthShort.uppercase()) {
        "JAN" -> "JANUARY"
        "FEB" -> "FEBRUARY"
        "MAR" -> "MARCH"
        "APR" -> "APRIL"
        "MAY" -> "MAY"
        "JUN" -> "JUNE"
        "JUL" -> "JULY"
        "AUG" -> "AUGUST"
        "SEP" -> "SEPTEMBER"
        "OCT" -> "OCTOBER"
        "NOV" -> "NOVEMBER"
        "DEC" -> "DECEMBER"
        else -> state.monthShort.uppercase()
    }

    val dateText = state.dayOfMonth
    val baseDateTextSize = bodyAreaRect.height() * 0.64f
    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = baseDateTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val baseMonthTextSize = bodyAreaRect.height() * 0.14f
    val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = baseMonthTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.08f
    }

    val maxBodyWidth = bodyAreaRect.width() * 0.88f
    val measuredDateWidth = datePaint.measureText(dateText)
    if (measuredDateWidth > maxBodyWidth) {
        datePaint.textSize = baseDateTextSize * (maxBodyWidth / measuredDateWidth)
    }

    val measuredMonthWidth = monthPaint.measureText(fullMonthName)
    if (measuredMonthWidth > maxBodyWidth) {
        monthPaint.textSize = baseMonthTextSize * (maxBodyWidth / measuredMonthWidth)
    }

    val dateBounds = Rect()
    datePaint.getTextBounds(dateText, 0, dateText.length, dateBounds)

    val monthBounds = Rect()
    monthPaint.getTextBounds(fullMonthName, 0, fullMonthName.length, monthBounds)

    val gap = bodyAreaRect.height() * 0.08f
    val totalBlockHeight = dateBounds.height() + gap + monthBounds.height()

    val verticalOffset = -5f * scaleFactor
    val blockTop = (bodyAreaRect.centerY() - (totalBlockHeight / 2f)) + verticalOffset

    val dateY = blockTop + dateBounds.height() - dateBounds.bottom
    val monthY = blockTop + dateBounds.height() + gap + monthBounds.height() - monthBounds.bottom

    canvas.drawText(dateText, bodyAreaRect.centerX(), dateY, datePaint)
    canvas.drawText(fullMonthName, bodyAreaRect.centerX(), monthY, monthPaint)

    return bitmap
}

fun generateCalendarPageBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateCalendarPageBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 6. INLINE HEADER DATE (2x2 Square)
fun generateInlineHeaderDateBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    val margin = scaleFactor * 1.5f
    val rect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardRadius, cardRadius, bgPaint)

    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#B3FFFFFF")
    val sundayAccent = Color.parseColor("#E53935")

    val cardSizeRef = minOf(rect.width(), rect.height())
    val padX = rect.width() * 0.12f

    val dayText = state.dayOfWeekShort.lowercase().replaceFirstChar { it.uppercase() }
    val monthText = state.monthShort.lowercase().replaceFirstChar { it.uppercase() }

    val isSunday = state.dayOfWeekShort.uppercase() == "SUN"
    val dayColor = if (isSunday) sundayAccent else accentColorInt

    var baseHeaderSize = cardSizeRef * 0.16f

    val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dayColor
        textSize = baseHeaderSize
        typeface = getSlateFont(context, weight = 700)
    }

    val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = baseHeaderSize
        typeface = getSlateFont(context, weight = 700)
    }

    val maxHeaderWidth = rect.width() - (padX * 2f)
    val headerGapX = cardSizeRef * 0.04f

    fun totalHeaderWidth() = dayPaint.measureText(dayText) + headerGapX + monthPaint.measureText(monthText)

    if (totalHeaderWidth() > maxHeaderWidth) {
        val scale = maxHeaderWidth / totalHeaderWidth()
        baseHeaderSize *= scale
        dayPaint.textSize = baseHeaderSize
        monthPaint.textSize = baseHeaderSize
    }

    val dayW = dayPaint.measureText(dayText)
    val monthW = monthPaint.measureText(monthText)
    val combinedHeaderW = dayW + headerGapX + monthW
    val headerX = rect.centerX() - (combinedHeaderW / 2f)

    val headerBounds = Rect()
    dayPaint.getTextBounds(dayText, 0, dayText.length, headerBounds)
    val headerHeight = headerBounds.height().toFloat()

    val dateText = state.dayOfMonth
    var baseDateTextSize = cardSizeRef * 0.58f

    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = baseDateTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val maxDateWidth = rect.width() - (padX * 2f)
    if (datePaint.measureText(dateText) > maxDateWidth) {
        baseDateTextSize *= (maxDateWidth / datePaint.measureText(dateText))
        datePaint.textSize = baseDateTextSize
    }

    val dateBounds = Rect()
    datePaint.getTextBounds(dateText, 0, dateText.length, dateBounds)
    val dateHeight = dateBounds.height().toFloat()

    val verticalGap = cardSizeRef * 0.08f
    val totalBlockHeight = headerHeight + verticalGap + dateHeight
    val blockTop = rect.centerY() - (totalBlockHeight / 2f)

    val headerY = blockTop + headerHeight - headerBounds.bottom
    val dateY = blockTop + headerHeight + verticalGap + dateHeight - dateBounds.bottom

    canvas.drawText(dayText, headerX, headerY, dayPaint)
    canvas.drawText(monthText, headerX + dayW + headerGapX, headerY, monthPaint)
    canvas.drawText(dateText, rect.centerX(), dateY, datePaint)

    return bitmap
}

fun generateInlineHeaderDateBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateInlineHeaderDateBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 7. FLIP CALENDAR (2x2 Square)
fun generateSplitFlapCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)

    val margin = scaleFactor * 1.5f
    val rect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardRadius, cardRadius, bgPaint)

    val cardSizeRef = minOf(rect.width(), rect.height())
    val padH = rect.width() * 0.08f
    val padV = rect.height() * 0.08f
    val gapY = cardSizeRef * 0.04f

    val usableH = rect.height() - (padV * 2f)
    val tileH = (usableH - gapY) / 2f
    val tileRadius = (cardRadius - padV).coerceAtLeast(scaleFactor * 6f)

    val tileBgColor = if (isLight) Color.parseColor("#E5E5EA") else Color.parseColor("#222226")
    val tileBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tileBgColor
        style = Paint.Style.FILL
    }

    val splitLineColor = if (isLight) Color.parseColor("#C7C7CC") else Color.parseColor("#141416")
    val splitLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = splitLineColor
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * scaleFactor
    }

    val pinColor = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#48484A")
    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pinColor
        style = Paint.Style.FILL
    }

    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE

    fun drawFlapTile(tileRect: RectF, text: String, textSizeRatio: Float, textColor: Int) {
        canvas.drawRoundRect(tileRect, tileRadius, tileRadius, tileBgPaint)

        val midY = tileRect.centerY()
        canvas.drawLine(tileRect.left, midY, tileRect.right, midY, splitLinePaint)

        val pinW = 5f * scaleFactor
        val pinH = 3.5f * scaleFactor
        val pinMargin = 4f * scaleFactor

        val leftPin = RectF(tileRect.left + pinMargin, midY - (pinH / 2f), tileRect.left + pinMargin + pinW, midY + (pinH / 2f))
        val rightPin = RectF(tileRect.right - pinMargin - pinW, midY - (pinH / 2f), tileRect.right - pinMargin, midY + (pinH / 2f))
        canvas.drawRoundRect(leftPin, 1f * scaleFactor, 1f * scaleFactor, pinPaint)
        canvas.drawRoundRect(rightPin, 1f * scaleFactor, 1f * scaleFactor, pinPaint)

        var baseTextSize = tileRect.height() * textSizeRatio
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = baseTextSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }

        val maxTextW = tileRect.width() * 0.74f
        if (textPaint.measureText(text) > maxTextW) {
            baseTextSize *= (maxTextW / textPaint.measureText(text))
            textPaint.textSize = baseTextSize
        }

        val bounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, bounds)
        val textY = midY + (bounds.height() / 2f) - bounds.bottom
        canvas.drawText(text, tileRect.centerX(), textY, textPaint)
    }

    val topTileRect = RectF(rect.left + padH, rect.top + padV, rect.right - padH, rect.top + padV + tileH)
    val bottomTileRect = RectF(rect.left + padH, topTileRect.bottom + gapY, rect.right - padH, rect.bottom - padV)

    val weekdayText = state.dayOfWeekShort.uppercase()
    val dayNumText = state.dayOfMonth.padStart(2, '0')

    drawFlapTile(topTileRect, weekdayText, 0.48f, primaryText)
    drawFlapTile(bottomTileRect, dayNumText, 0.58f, primaryText)

    return bitmap
}

fun generateSplitFlapCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateSplitFlapCalendarBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 8. STACKED HEADER DATE (2x2 Square)
fun generateStackedHeaderDateBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)

    val margin = scaleFactor * 1.5f
    val rect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardCornerRadius, cardCornerRadius, bgPaint)

    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = Color.parseColor("#8E8E93")
    val redAccent = if (isLight) Color.parseColor("#FF3B30") else Color.parseColor("#FF453A")

    val cardSizeRef = minOf(rect.width(), rect.height())
    val padX = rect.width() * 0.12f
    val maxAvailableWidth = rect.width() - (padX * 2f)

    val fullMonthName = when (state.monthShort.uppercase()) {
        "JAN" -> "JANUARY"
        "FEB" -> "FEBRUARY"
        "MAR" -> "MARCH"
        "APR" -> "APRIL"
        "MAY" -> "MAY"
        "JUN" -> "JUNE"
        "JUL" -> "JULY"
        "AUG" -> "AUGUST"
        "SEP" -> "SEPTEMBER"
        "OCT" -> "OCTOBER"
        "NOV" -> "NOVEMBER"
        "DEC" -> "DECEMBER"
        else -> state.monthShort.uppercase()
    }

    val weekdayTitle = when (state.dayOfWeekShort.uppercase()) {
        "MON" -> "Monday"
        "TUE" -> "Tuesday"
        "WED" -> "Wednesday"
        "THU" -> "Thursday"
        "FRI" -> "Friday"
        "SAT" -> "Saturday"
        "SUN" -> "Sunday"
        else -> state.dayOfWeekShort.lowercase().replaceFirstChar { it.uppercase() }
    }

    val dateText = state.dayOfMonth

    var monthSize = cardSizeRef * 0.10f
    var weekdaySize = cardSizeRef * 0.15f
    var dateSize = cardSizeRef * 0.42f

    val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = monthSize
        typeface = getSlateFont(context, weight = 500)
        textAlign = Paint.Align.LEFT
        letterSpacing = 0.05f
    }

    val weekdayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = redAccent
        textSize = weekdaySize
        typeface = getSlateFont(context, weight = 500)
        textAlign = Paint.Align.LEFT
    }

    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = dateSize
        typeface = getSlateFont(context, weight = 300)
        textAlign = Paint.Align.LEFT
    }

    if (monthPaint.measureText(fullMonthName) > maxAvailableWidth) {
        monthSize *= (maxAvailableWidth / monthPaint.measureText(fullMonthName))
        monthPaint.textSize = monthSize
    }

    if (weekdayPaint.measureText(weekdayTitle) > maxAvailableWidth) {
        weekdaySize *= (maxAvailableWidth / weekdayPaint.measureText(weekdayTitle))
        weekdayPaint.textSize = weekdaySize
    }

    if (datePaint.measureText(dateText) > maxAvailableWidth) {
        dateSize *= (maxAvailableWidth / datePaint.measureText(dateText))
        datePaint.textSize = dateSize
    }

    val monthBounds = Rect()
    monthPaint.getTextBounds(fullMonthName, 0, fullMonthName.length, monthBounds)

    val weekdayBounds = Rect()
    weekdayPaint.getTextBounds(weekdayTitle, 0, weekdayTitle.length, weekdayBounds)

    val dateBounds = Rect()
    datePaint.getTextBounds(dateText, 0, dateText.length, dateBounds)

    val gap1 = cardSizeRef * 0.035f
    val gap2 = cardSizeRef * 0.055f

    val monthH = monthBounds.height().toFloat()
    val weekdayH = weekdayBounds.height().toFloat()
    val dateH = dateBounds.height().toFloat()

    val totalBlockHeight = monthH + gap1 + weekdayH + gap2 + dateH
    val blockTop = rect.centerY() - (totalBlockHeight / 2f)
    val leftX = rect.left + padX

    val monthY = blockTop + monthH - monthBounds.bottom
    val weekdayY = blockTop + monthH + gap1 + weekdayH - weekdayBounds.bottom
    val dateY = blockTop + monthH + gap1 + weekdayH + gap2 + dateH - dateBounds.bottom

    canvas.drawText(fullMonthName, leftX, monthY, monthPaint)
    canvas.drawText(weekdayTitle, leftX, weekdayY, weekdayPaint)
    canvas.drawText(dateText, leftX, dateY, datePaint)

    return bitmap
}

fun generateStackedHeaderDateBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateStackedHeaderDateBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 9. SIDEBAR MONTH DATE (2x2 Square)
fun generateSideBarDateBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    val margin = scaleFactor * 1.5f
    val rect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)

    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardRadius, cardRadius, bodyPaint)

    val stripW = rect.width() * 0.30f
    val stripRect = RectF(rect.left, rect.top, rect.left + stripW, rect.bottom)

    val stripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }

    canvas.save()
    val clipPath = Path().apply { addRoundRect(rect, cardRadius, cardRadius, Path.Direction.CW) }
    canvas.clipPath(clipPath)
    canvas.drawRect(stripRect, stripPaint)
    canvas.restore()

    val fullMonthName = when (state.monthShort.uppercase()) {
        "JAN" -> "JANUARY"
        "FEB" -> "FEBRUARY"
        "MAR" -> "MARCH"
        "APR" -> "APRIL"
        "MAY" -> "MAY"
        "JUN" -> "JUNE"
        "JUL" -> "JULY"
        "AUG" -> "AUGUST"
        "SEP" -> "SEPTEMBER"
        "OCT" -> "OCTOBER"
        "NOV" -> "NOVEMBER"
        "DEC" -> "DECEMBER"
        else -> state.monthShort.uppercase()
    }

    val r = ((accentColorInt shr 16) and 0xFF) / 255f
    val g = ((accentColorInt shr 8) and 0xFF) / 255f
    val b = (accentColorInt and 0xFF) / 255f
    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
    val stripTextColor = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

    var baseMonthTextSize = stripW * 0.42f
    val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = stripTextColor
        textSize = baseMonthTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.08f
    }

    val maxMonthHeight = stripRect.height() * 0.82f
    if (monthPaint.measureText(fullMonthName) > maxMonthHeight) {
        baseMonthTextSize *= (maxMonthHeight / monthPaint.measureText(fullMonthName))
        monthPaint.textSize = baseMonthTextSize
    }

    canvas.save()
    val stripCx = stripRect.centerX()
    val stripCy = stripRect.centerY()
    canvas.rotate(-90f, stripCx, stripCy)

    val monthBounds = Rect()
    monthPaint.getTextBounds(fullMonthName, 0, fullMonthName.length, monthBounds)
    val monthTextY = stripCy + (monthBounds.height() / 2f) - monthBounds.bottom

    canvas.drawText(fullMonthName, stripCx, monthTextY, monthPaint)
    canvas.restore()

    val rightAreaRect = RectF(stripRect.right, rect.top, rect.right, rect.bottom)
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE

    val dateText = state.dayOfMonth
    var baseDateSize = rightAreaRect.width() * 0.55f

    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = baseDateSize
        typeface = getSlateFont(context, weight = 500)
        textAlign = Paint.Align.CENTER
    }

    val maxDateW = rightAreaRect.width() * 0.85f
    if (datePaint.measureText(dateText) > maxDateW) {
        baseDateSize *= (maxDateW / datePaint.measureText(dateText))
        datePaint.textSize = baseDateSize
    }

    val dateBounds = Rect()
    datePaint.getTextBounds(dateText, 0, dateText.length, dateBounds)
    val dateY = rightAreaRect.centerY() + (dateBounds.height() / 2f) - dateBounds.bottom

    canvas.drawText(dateText, rightAreaRect.centerX(), dateY, datePaint)

    return bitmap
}

fun generateSideBarDateBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateSideBarDateBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 10. QUADRANT GRID DATE (2x2 Square)
fun generateGridQuadrantCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)

    val margin = scaleFactor * 1.5f
    val rect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardRadius, cardRadius, bgPaint)

    val dividerColor = if (isLight) Color.parseColor("#18000000") else Color.parseColor("#1FFFFFFF")
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dividerColor
        style = Paint.Style.STROKE
        strokeWidth = 1f * scaleFactor
    }

    val cx = rect.centerX()
    val cy = rect.centerY()

    canvas.drawLine(cx, rect.top, cx, rect.bottom, dividerPaint)
    canvas.drawLine(rect.left, cy, rect.right, cy, dividerPaint)

    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE

    val topLeftRect = RectF(rect.left, rect.top, cx, cy)
    val dateText = state.dayOfMonth

    var baseDateSize = topLeftRect.height() * 0.58f
    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = baseDateSize
        typeface = getSlateFont(context, weight = 500)
        textAlign = Paint.Align.CENTER
    }

    val maxCellW = topLeftRect.width() * 0.80f
    if (datePaint.measureText(dateText) > maxCellW) {
        baseDateSize *= (maxCellW / datePaint.measureText(dateText))
        datePaint.textSize = baseDateSize
    }

    val dateBounds = Rect()
    datePaint.getTextBounds(dateText, 0, dateText.length, dateBounds)
    val dateY = topLeftRect.centerY() + (dateBounds.height() / 2f) - dateBounds.bottom

    canvas.drawText(dateText, topLeftRect.centerX(), dateY, datePaint)

    val bottomRightRect = RectF(cx, cy, rect.right, rect.bottom)
    val dayText = state.dayOfWeekShort.uppercase()
    val monthText = state.monthShort.uppercase()

    var baseTextSize = bottomRightRect.height() * 0.28f
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = baseTextSize
        typeface = getSlateFont(context, weight = 500)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.05f
    }

    val longestWordW = maxOf(textPaint.measureText(dayText), textPaint.measureText(monthText))
    if (longestWordW > maxCellW) {
        baseTextSize *= (maxCellW / longestWordW)
        textPaint.textSize = baseTextSize
    }

    val dayBounds = Rect()
    textPaint.getTextBounds(dayText, 0, dayText.length, dayBounds)
    val monthBounds = Rect()
    textPaint.getTextBounds(monthText, 0, monthText.length, monthBounds)

    val stackGap = bottomRightRect.height() * 0.05f
    val totalStackH = dayBounds.height() + stackGap + monthBounds.height()
    val stackTop = bottomRightRect.centerY() - (totalStackH / 2f)

    val dayY = stackTop + dayBounds.height() - dayBounds.bottom
    val monthY = stackTop + dayBounds.height() + stackGap + monthBounds.height() - monthBounds.bottom

    canvas.drawText(dayText, bottomRightRect.centerX(), dayY, textPaint)
    canvas.drawText(monthText, bottomRightRect.centerX(), monthY, textPaint)

    return bitmap
}

fun generateGridQuadrantCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateGridQuadrantCalendarBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 11. DIAGONAL SPLIT DATE (2x2 Square)
fun generateDiagonalSplitDateBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    val margin = scaleFactor * 1.5f
    val rect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardRadius, cardRadius, bgPaint)

    val diagonalPath = Path().apply {
        moveTo(rect.left, rect.bottom)
        lineTo(rect.right, rect.top)
        lineTo(rect.right, rect.bottom)
        close()
    }

    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }

    canvas.save()
    val cardClipPath = Path().apply { addRoundRect(rect, cardRadius, cardRadius, Path.Direction.CW) }
    canvas.clipPath(cardClipPath)
    canvas.drawPath(diagonalPath, accentPaint)
    canvas.restore()

    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE

    val r = ((accentColorInt shr 16) and 0xFF) / 255f
    val g = ((accentColorInt shr 8) and 0xFF) / 255f
    val b = (accentColorInt and 0xFF) / 255f
    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
    val accentTextColor = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

    val cardSizeRef = minOf(rect.width(), rect.height())
    val pad = cardSizeRef * 0.12f

    val weekdayText = state.dayOfWeekShort.uppercase()
    var baseWeekdaySize = cardSizeRef * 0.15f

    val weekdayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = baseWeekdaySize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.LEFT
        letterSpacing = 0.06f
    }

    val maxTopLeftW = (rect.width() / 2f) - pad
    if (weekdayPaint.measureText(weekdayText) > maxTopLeftW) {
        baseWeekdaySize *= (maxTopLeftW / weekdayPaint.measureText(weekdayText))
        weekdayPaint.textSize = baseWeekdaySize
    }

    val weekdayX = rect.left + pad
    val weekdayY = rect.top + pad + weekdayPaint.textSize
    canvas.drawText(weekdayText, weekdayX, weekdayY, weekdayPaint)

    val dateText = state.dayOfMonth
    var baseDateSize = cardSizeRef * 0.46f

    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentTextColor
        textSize = baseDateSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.RIGHT
    }

    val maxDateW = (rect.width() / 2f) - pad
    if (datePaint.measureText(dateText) > maxDateW) {
        baseDateSize *= (maxDateW / datePaint.measureText(dateText))
        datePaint.textSize = baseDateSize
    }

    val dateBounds = Rect()
    datePaint.getTextBounds(dateText, 0, dateText.length, dateBounds)

    val dateX = rect.right - pad
    val dateY = rect.bottom - pad - dateBounds.bottom

    canvas.drawText(dateText, dateX, dateY, datePaint)

    return bitmap
}

fun generateDiagonalSplitDateBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateDiagonalSplitDateBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 12. SPLIT DASHBOARD CALENDAR (4x2)
fun generateSplitDashboardCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val margin = scaleFactor * 1.5f
    val targetRatio = 2.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    val cardRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val dimText = if (isLight) Color.parseColor("#C7C7CC") else Color.parseColor("#48484A")

    val r = Color.red(accentColorInt)
    val g = Color.green(accentColorInt)
    val b = Color.blue(accentColorInt)
    val accentLuminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    val activeTextColor = if (accentLuminance > 0.65) Color.parseColor("#161618") else Color.WHITE

    val fullMonthName = when (state.monthShort.uppercase()) {
        "JAN" -> "JANUARY"
        "FEB" -> "FEBRUARY"
        "MAR" -> "MARCH"
        "APR" -> "APRIL"
        "MAY" -> "MAY"
        "JUN" -> "JUNE"
        "JUL" -> "JULY"
        "AUG" -> "AUGUST"
        "SEP" -> "SEPTEMBER"
        "OCT" -> "OCTOBER"
        "NOV" -> "NOVEMBER"
        "DEC" -> "DECEMBER"
        else -> state.monthShort.uppercase()
    }

    val weekdayTitle = when (state.dayOfWeekShort.uppercase()) {
        "MON" -> "Monday"
        "TUE" -> "Tuesday"
        "WED" -> "Wednesday"
        "THU" -> "Thursday"
        "FRI" -> "Friday"
        "SAT" -> "Saturday"
        "SUN" -> "Sunday"
        else -> state.dayOfWeekShort.lowercase().replaceFirstChar { it.uppercase() }
    }

    val cal = java.util.Calendar.getInstance().apply {
        val yearInt = state.year.toIntOrNull() ?: get(java.util.Calendar.YEAR)
        try {
            val parsedDate = java.text.SimpleDateFormat("MMM", java.util.Locale.ENGLISH).parse(state.monthShort)
            if (parsedDate != null) {
                val tempCal = java.util.Calendar.getInstance().apply { time = parsedDate }
                set(java.util.Calendar.MONTH, tempCal.get(java.util.Calendar.MONTH))
            }
        } catch (_: Exception) {}
        set(java.util.Calendar.YEAR, yearInt)
        set(java.util.Calendar.DAY_OF_MONTH, 1)
    }

    val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val firstDaySunIndex = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
    val currentDayNum = state.dayOfMonth.toIntOrNull() ?: -1

    val headers = arrayOf("S", "M", "T", "W", "T", "F", "S")
    val todayColIndex = (firstDaySunIndex + currentDayNum - 1) % 7

    val aspectRatio = cardRect.width() / cardRect.height()

    if (aspectRatio >= 1.2f) {
        val padX = cardRect.width() * 0.05f
        val padY = cardRect.height() * 0.08f
        val leftW = cardRect.width() * 0.34f

        val gridLeft = cardRect.left + leftW + (cardRect.width() * 0.02f)
        val gridW = cardRect.right - gridLeft - padX
        val gridTop = cardRect.top + padY
        val gridH = cardRect.height() - (padY * 2f)

        val colW = gridW / 7f
        val totalGridRows = 6f
        val rowH = gridH / totalGridRows

        val fontScale = minOf(colW * 0.45f, rowH * 0.48f).coerceAtLeast(8f * scaleFactor)
        fun getRowBaseline(row: Int): Float = gridTop + (row * rowH) + (rowH * 0.62f)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontScale
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }

        val row0Baseline = getRowBaseline(0)
        for (c in 0..6) {
            val cx = gridLeft + (c * colW) + (colW / 2f)
            headerPaint.color = if (c == todayColIndex) accentColorInt else secondaryText
            canvas.drawText(headers[c], cx, row0Baseline, headerPaint)
        }

        val dateNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontScale
            typeface = getSlateFont(context, weight = 400)
            textAlign = Paint.Align.CENTER
        }

        val activeBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }

        val prevCal = (cal.clone() as java.util.Calendar).apply { add(java.util.Calendar.MONTH, -1) }
        val prevMaxDays = prevCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

        var cellIndex = 0
        for (i in (firstDaySunIndex - 1) downTo 0) {
            val dayNum = prevMaxDays - i
            val c = cellIndex % 7
            val rIdx = (cellIndex / 7) + 1
            val cx = gridLeft + (c * colW) + (colW / 2f)
            val cy = getRowBaseline(rIdx)

            dateNumPaint.color = dimText
            canvas.drawText(dayNum.toString(), cx, cy, dateNumPaint)
            cellIndex++
        }

        for (day in 1..daysInMonth) {
            val c = cellIndex % 7
            val rIdx = (cellIndex / 7) + 1
            if (rIdx >= totalGridRows.toInt()) break

            val cx = gridLeft + (c * colW) + (colW / 2f)

            val drawY = if (day == currentDayNum) {
                val badgeRadius = minOf(colW * 0.40f, rowH * 0.42f)
                val badgeCenterY = gridTop + (rIdx * rowH) + (rowH / 2f)
                val badgeRect = RectF(cx - badgeRadius, badgeCenterY - badgeRadius, cx + badgeRadius, badgeCenterY + badgeRadius)
                canvas.drawRoundRect(badgeRect, 6f * scaleFactor, 6f * scaleFactor, activeBadgePaint)

                dateNumPaint.color = activeTextColor
                dateNumPaint.typeface = getSlateFont(context, weight = 700)

                val fm = dateNumPaint.fontMetrics
                badgeCenterY - ((fm.descent + fm.ascent) / 2f)
            } else {
                dateNumPaint.color = primaryText
                dateNumPaint.typeface = getSlateFont(context, weight = 400)
                getRowBaseline(rIdx)
            }

            canvas.drawText(day.toString(), cx, drawY, dateNumPaint)
            cellIndex++
        }

        var nextMonthDay = 1
        while (cellIndex < (totalGridRows.toInt() - 1) * 7) {
            val c = cellIndex % 7
            val rIdx = (cellIndex / 7) + 1
            val cx = gridLeft + (c * colW) + (colW / 2f)
            val cy = getRowBaseline(rIdx)

            dateNumPaint.color = dimText
            dateNumPaint.typeface = getSlateFont(context, weight = 400)
            canvas.drawText(nextMonthDay.toString(), cx, cy, dateNumPaint)
            nextMonthDay++
            cellIndex++
        }

        val leftX = cardRect.left + padX
        val maxLeftTextW = leftW - padX

        val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = (fontScale * 0.95f).coerceAtMost(cardRect.height() * 0.10f)
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.LEFT
            letterSpacing = 0.08f
        }
        if (monthPaint.measureText(fullMonthName) > maxLeftTextW) {
            monthPaint.textSize *= (maxLeftTextW / monthPaint.measureText(fullMonthName))
        }
        val monthY = getRowBaseline(0)
        canvas.drawText(fullMonthName, leftX, monthY, monthPaint)

        val weekdayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            textSize = (fontScale * 1.30f).coerceAtMost(cardRect.height() * 0.14f)
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.LEFT
        }
        if (weekdayPaint.measureText(weekdayTitle) > maxLeftTextW) {
            weekdayPaint.textSize *= (maxLeftTextW / weekdayPaint.measureText(weekdayTitle))
        }
        val weekdayY = monthY + (weekdayPaint.textSize * 1.08f)
        canvas.drawText(weekdayTitle, leftX, weekdayY, weekdayPaint)

        val targetGiantBaseline = getRowBaseline(5)
        val topOfGiantArea = weekdayY + (6f * scaleFactor)
        val availableGiantH = (targetGiantBaseline - topOfGiantArea).coerceAtLeast(10f)

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = availableGiantH * 0.92f
            typeface = getSlateFont(context, weight = 300)
            textAlign = Paint.Align.LEFT
        }
        if (datePaint.measureText(state.dayOfMonth) > maxLeftTextW) {
            datePaint.textSize *= (maxLeftTextW / datePaint.measureText(state.dayOfMonth))
        }

        canvas.drawText(state.dayOfMonth, leftX, targetGiantBaseline, datePaint)
    } else {
        val padX = cardRect.width() * 0.06f
        val padY = cardRect.height() * 0.06f

        val headerH = cardRect.height() * 0.18f
        val headerRect = RectF(cardRect.left + padX, cardRect.top + padY, cardRect.right - padX, cardRect.top + padY + headerH)

        val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = headerH * 0.38f
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.LEFT
            letterSpacing = 0.06f
        }
        val fmM = monthPaint.fontMetrics
        val headerCenterY = headerRect.centerY()
        val monthY = headerCenterY - ((fmM.descent + fmM.ascent) / 2f)
        canvas.drawText(fullMonthName, headerRect.left, monthY, monthPaint)

        val dateText = "${state.dayOfMonth} $weekdayTitle"
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = headerH * 0.58f
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.RIGHT
        }
        val fmD = datePaint.fontMetrics
        val dateY = headerCenterY - ((fmD.descent + fmD.ascent) / 2f)
        canvas.drawText(dateText, headerRect.right, dateY, datePaint)

        val gridTop = headerRect.bottom + (cardRect.height() * 0.02f)
        val gridH = cardRect.bottom - padY - gridTop
        val gridW = cardRect.width() - (padX * 2f)

        val colW = gridW / 7f
        val totalGridRows = 6f
        val rowH = gridH / totalGridRows

        val fontScale = minOf(colW * 0.45f, rowH * 0.48f).coerceAtLeast(8f * scaleFactor)
        fun getRowBaseline(row: Int): Float = gridTop + (row * rowH) + (rowH * 0.62f)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontScale
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }

        val row0Baseline = getRowBaseline(0)
        for (c in 0..6) {
            val cx = cardRect.left + padX + (c * colW) + (colW / 2f)
            headerPaint.color = if (c == todayColIndex) accentColorInt else secondaryText
            canvas.drawText(headers[c], cx, row0Baseline, headerPaint)
        }

        val dateNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontScale
            typeface = getSlateFont(context, weight = 400)
            textAlign = Paint.Align.CENTER
        }

        val activeBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }

        val prevCal = (cal.clone() as java.util.Calendar).apply { add(java.util.Calendar.MONTH, -1) }
        val prevMaxDays = prevCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

        var cellIndex = 0
        for (i in (firstDaySunIndex - 1) downTo 0) {
            val dayNum = prevMaxDays - i
            val c = cellIndex % 7
            val rIdx = (cellIndex / 7) + 1
            val cx = cardRect.left + padX + (c * colW) + (colW / 2f)
            val cy = getRowBaseline(rIdx)

            dateNumPaint.color = dimText
            canvas.drawText(dayNum.toString(), cx, cy, dateNumPaint)
            cellIndex++
        }

        for (day in 1..daysInMonth) {
            val c = cellIndex % 7
            val rIdx = (cellIndex / 7) + 1
            if (rIdx >= totalGridRows.toInt()) break

            val cx = cardRect.left + padX + (c * colW) + (colW / 2f)

            val drawY = if (day == currentDayNum) {
                val badgeRadius = minOf(colW * 0.40f, rowH * 0.42f)
                val badgeCenterY = gridTop + (rIdx * rowH) + (rowH / 2f)
                val badgeRect = RectF(cx - badgeRadius, badgeCenterY - badgeRadius, cx + badgeRadius, badgeCenterY + badgeRadius)
                canvas.drawRoundRect(badgeRect, 6f * scaleFactor, 6f * scaleFactor, activeBadgePaint)

                dateNumPaint.color = activeTextColor
                dateNumPaint.typeface = getSlateFont(context, weight = 700)

                val fm = dateNumPaint.fontMetrics
                badgeCenterY - ((fm.descent + fm.ascent) / 2f)
            } else {
                dateNumPaint.color = primaryText
                dateNumPaint.typeface = getSlateFont(context, weight = 400)
                getRowBaseline(rIdx)
            }

            canvas.drawText(day.toString(), cx, drawY, dateNumPaint)
            cellIndex++
        }

        var nextMonthDay = 1
        while (cellIndex < (totalGridRows.toInt() - 1) * 7) {
            val c = cellIndex % 7
            val rIdx = (cellIndex / 7) + 1
            val cx = cardRect.left + padX + (c * colW) + (colW / 2f)
            val cy = getRowBaseline(rIdx)

            dateNumPaint.color = dimText
            dateNumPaint.typeface = getSlateFont(context, weight = 400)
            canvas.drawText(nextMonthDay.toString(), cx, cy, dateNumPaint)
            nextMonthDay++
            cellIndex++
        }
    }

    return bitmap
}

fun generateSplitDashboardCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateSplitDashboardCalendarBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 13. FOCUS TIMELINE CALENDAR (4x2)
fun generateFocusTimelineCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    val margin = scaleFactor * 1.5f
    val targetRatio = 2.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val r = Color.red(accentColorInt)
    val g = Color.green(accentColorInt)
    val b = Color.blue(accentColorInt)
    val accentLuminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    val giantDateTextColor = if (accentLuminance > 0.65) Color.parseColor("#121214") else Color.WHITE

    val primaryLeftText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryLeftText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val dividerColor = if (isLight) Color.parseColor("#1F000000") else Color.parseColor("#26FFFFFF")

    val cal = java.util.Calendar.getInstance()
    val dayFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.ENGLISH)

    val todayName = dayFormat.format(cal.time)
    cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
    val prevDayName = dayFormat.format(cal.time)
    cal.add(java.util.Calendar.DAY_OF_YEAR, 2)
    val nextDayName = dayFormat.format(cal.time)

    val aspectRatio = cardRect.width() / cardRect.height()

    if (aspectRatio >= 1.2f) {
        val rightBlockW = cardRect.width() * 0.36f
        val rightBlockRect = RectF(cardRect.right - rightBlockW, cardRect.top, cardRect.right, cardRect.bottom)

        val rightBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }

        canvas.save()
        val clipPath = Path().apply { addRoundRect(cardRect, cardRadius, cardRadius, Path.Direction.CW) }
        canvas.clipPath(clipPath)
        canvas.drawRect(rightBlockRect, rightBgPaint)
        canvas.restore()

        val leftAreaRect = RectF(cardRect.left, cardRect.top, rightBlockRect.left, cardRect.bottom)

        val padX = leftAreaRect.width() * 0.10f
        val padY = leftAreaRect.height() * 0.16f
        val timelineH = leftAreaRect.height() - (padY * 2f)
        val rowH = timelineH / 3f

        val row1CenterY = leftAreaRect.top + padY + (rowH * 0.5f)
        val row2CenterY = leftAreaRect.top + padY + (rowH * 1.5f)
        val row3CenterY = leftAreaRect.top + padY + (rowH * 2.5f)

        val divider1Y = leftAreaRect.top + padY + rowH
        val divider2Y = leftAreaRect.top + padY + (rowH * 2f)

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dividerColor
            style = Paint.Style.STROKE
            strokeWidth = 1f * scaleFactor
        }

        val lineLeftX = leftAreaRect.left + padX
        val lineRightX = leftAreaRect.right - (padX * 0.8f)

        canvas.drawLine(lineLeftX, divider1Y, lineRightX, divider1Y, dividerPaint)
        canvas.drawLine(lineLeftX, divider2Y, lineRightX, divider2Y, dividerPaint)

        var baseFontSize = (rowH * 0.38f).coerceAtLeast(10f * scaleFactor)

        val inactiveTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryLeftText
            textSize = baseFontSize
            typeface = getSlateFont(context, weight = 400)
            textAlign = Paint.Align.LEFT
        }

        val activeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryLeftText
            textSize = baseFontSize * 1.1f
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.LEFT
        }

        val todayLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryLeftText
            textSize = baseFontSize
            typeface = getSlateFont(context, weight = 400)
            textAlign = Paint.Align.LEFT
        }

        val accentBarW = 3.5f * scaleFactor
        val activeTextStartX = lineLeftX + accentBarW + (8f * scaleFactor)
        val maxAllowedLeftW = lineRightX - activeTextStartX

        val totalActiveWidth = activeTextPaint.measureText(todayName) + todayLabelPaint.measureText(" — today")
        if (totalActiveWidth > maxAllowedLeftW) {
            val scaleRatio = maxAllowedLeftW / totalActiveWidth
            baseFontSize *= scaleRatio
            inactiveTextPaint.textSize = baseFontSize
            activeTextPaint.textSize = baseFontSize * 1.1f
            todayLabelPaint.textSize = baseFontSize
        }

        val fmInactive = inactiveTextPaint.fontMetrics
        val textY1 = row1CenterY - ((fmInactive.descent + fmInactive.ascent) / 2f)
        canvas.drawText(prevDayName, lineLeftX, textY1, inactiveTextPaint)

        val accentBarH = rowH * 0.52f
        val accentBarRect = RectF(lineLeftX, row2CenterY - (accentBarH / 2f), lineLeftX + accentBarW, row2CenterY + (accentBarH / 2f))

        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(accentBarRect, accentBarW / 2f, accentBarW / 2f, accentPaint)

        val fmActive = activeTextPaint.fontMetrics
        val textY2 = row2CenterY - ((fmActive.descent + fmActive.ascent) / 2f)

        canvas.drawText(todayName, activeTextStartX, textY2, activeTextPaint)

        val dayNameWidth = activeTextPaint.measureText(todayName)
        canvas.drawText(" — today", activeTextStartX + dayNameWidth, textY2, todayLabelPaint)

        val textY3 = row3CenterY - ((fmInactive.descent + fmInactive.ascent) / 2f)
        canvas.drawText(nextDayName, lineLeftX, textY3, inactiveTextPaint)

        val maxGiantTextW = rightBlockRect.width() * 0.80f
        val maxGiantTextH = rightBlockRect.height() * 0.65f

        val giantDatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = giantDateTextColor
            textSize = maxGiantTextH
            typeface = getSlateFont(context, weight = 300)
            textAlign = Paint.Align.CENTER
        }

        val measuredGiantW = giantDatePaint.measureText(state.dayOfMonth)
        if (measuredGiantW > maxGiantTextW) {
            giantDatePaint.textSize = maxGiantTextH * (maxGiantTextW / measuredGiantW)
        }

        val fmGiant = giantDatePaint.fontMetrics
        val giantDateY = rightBlockRect.centerY() - ((fmGiant.descent + fmGiant.ascent) / 2f)
        canvas.drawText(state.dayOfMonth, rightBlockRect.centerX(), giantDateY, giantDatePaint)
    } else {
        val topHeaderH = cardRect.height() * 0.40f
        val topHeaderRect = RectF(cardRect.left, cardRect.top, cardRect.right, cardRect.top + topHeaderH)

        val topBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }

        canvas.save()
        val clipPath = Path().apply { addRoundRect(cardRect, cardRadius, cardRadius, Path.Direction.CW) }
        canvas.clipPath(clipPath)
        canvas.drawRect(topHeaderRect, topBgPaint)
        canvas.restore()

        val dateStr = "${state.dayOfMonth} ${state.monthShort.uppercase()}"
        val giantDatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = giantDateTextColor
            textSize = topHeaderH * 0.55f
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }
        val maxW = topHeaderRect.width() * 0.86f
        if (giantDatePaint.measureText(dateStr) > maxW) {
            giantDatePaint.textSize = (topHeaderH * 0.55f) * (maxW / giantDatePaint.measureText(dateStr))
        }
        val fmGiant = giantDatePaint.fontMetrics
        val giantY = topHeaderRect.centerY() - ((fmGiant.descent + fmGiant.ascent) / 2f)
        canvas.drawText(dateStr, topHeaderRect.centerX(), giantY, giantDatePaint)

        val bottomAreaRect = RectF(cardRect.left, topHeaderRect.bottom, cardRect.right, cardRect.bottom)
        val padX = bottomAreaRect.width() * 0.08f
        val padY = bottomAreaRect.height() * 0.10f
        val timelineH = bottomAreaRect.height() - (padY * 2f)
        val rowH = timelineH / 3f

        val row1CenterY = bottomAreaRect.top + padY + (rowH * 0.5f)
        val row2CenterY = bottomAreaRect.top + padY + (rowH * 1.5f)
        val row3CenterY = bottomAreaRect.top + padY + (rowH * 2.5f)

        val lineLeftX = bottomAreaRect.left + padX
        val lineRightX = bottomAreaRect.right - padX

        val divider1Y = bottomAreaRect.top + padY + rowH
        val divider2Y = bottomAreaRect.top + padY + (rowH * 2f)

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dividerColor
            style = Paint.Style.STROKE
            strokeWidth = 1f * scaleFactor
        }
        canvas.drawLine(lineLeftX, divider1Y, lineRightX, divider1Y, dividerPaint)
        canvas.drawLine(lineLeftX, divider2Y, lineRightX, divider2Y, dividerPaint)

        var baseFontSize = (rowH * 0.42f).coerceAtLeast(10f * scaleFactor)

        val inactiveTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryLeftText
            textSize = baseFontSize
            typeface = getSlateFont(context, weight = 400)
            textAlign = Paint.Align.LEFT
        }

        val activeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryLeftText
            textSize = baseFontSize * 1.1f
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.LEFT
        }

        val todayLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryLeftText
            textSize = baseFontSize
            typeface = getSlateFont(context, weight = 400)
            textAlign = Paint.Align.LEFT
        }

        val accentBarW = 3.5f * scaleFactor
        val activeTextStartX = lineLeftX + accentBarW + (8f * scaleFactor)

        val fmInactive = inactiveTextPaint.fontMetrics
        val textY1 = row1CenterY - ((fmInactive.descent + fmInactive.ascent) / 2f)
        canvas.drawText(prevDayName, lineLeftX, textY1, inactiveTextPaint)

        val accentBarH = rowH * 0.52f
        val accentBarRect = RectF(lineLeftX, row2CenterY - (accentBarH / 2f), lineLeftX + accentBarW, row2CenterY + (accentBarH / 2f))
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(accentBarRect, accentBarW / 2f, accentBarW / 2f, accentPaint)

        val fmActive = activeTextPaint.fontMetrics
        val textY2 = row2CenterY - ((fmActive.descent + fmActive.ascent) / 2f)
        canvas.drawText(todayName, activeTextStartX, textY2, activeTextPaint)

        val dayNameWidth = activeTextPaint.measureText(todayName)
        canvas.drawText(" — today", activeTextStartX + dayNameWidth, textY2, todayLabelPaint)

        val textY3 = row3CenterY - ((fmInactive.descent + fmInactive.ascent) / 2f)
        canvas.drawText(nextDayName, lineLeftX, textY3, inactiveTextPaint)
    }

    return bitmap
}

fun generateFocusTimelineCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateFocusTimelineCalendarBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 14. ANALOG TIMELINE HYBRID (4x2)
fun generateAnalogTimelineCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    val margin = scaleFactor * 1.5f
    val targetRatio = 2.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val targetRatioVal = 2.0f
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatioVal
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatioVal
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    val r = Color.red(accentColorInt)
    val g = Color.green(accentColorInt)
    val b = Color.blue(accentColorInt)
    val accentLuminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    val activeTextColor = if (accentLuminance > 0.65) Color.parseColor("#121214") else Color.WHITE

    val timeCal = java.util.Calendar.getInstance()
    val hours = timeCal.get(java.util.Calendar.HOUR)
    val minutes = timeCal.get(java.util.Calendar.MINUTE)
    val seconds = timeCal.get(java.util.Calendar.SECOND)
    val millis = timeCal.get(java.util.Calendar.MILLISECOND)

    val secondsWithMillis = seconds + (millis / 1000f)
    val minutesWithSeconds = minutes + (secondsWithMillis / 60f)
    val hoursWithMinutes = (hours % 12) + (minutesWithSeconds / 60f)

    val hourAngle = Math.toRadians((hoursWithMinutes * 30f - 90f).toDouble())
    val minuteAngle = Math.toRadians((minutesWithSeconds * 6f - 90f).toDouble())
    val secondAngle = Math.toRadians((secondsWithMillis * 6f - 90f).toDouble())

    val cal = java.util.Calendar.getInstance()
    val dayFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.ENGLISH)

    val todayDay = dayFormat.format(cal.time).uppercase()
    val todayNum = cal.get(java.util.Calendar.DAY_OF_MONTH).toString()

    cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
    val prevDay = dayFormat.format(cal.time).uppercase()
    val prevNum = cal.get(java.util.Calendar.DAY_OF_MONTH).toString()

    cal.add(java.util.Calendar.DAY_OF_YEAR, 2)
    val nextDay = dayFormat.format(cal.time).uppercase()
    val nextNum = cal.get(java.util.Calendar.DAY_OF_MONTH).toString()

    val aspectRatio = cardRect.width() / cardRect.height()

    if (aspectRatio >= 1.2f) {
        val padX = cardRect.width() * 0.07f
        val padY = cardRect.height() * 0.10f

        val clockDiameter = (cardRect.height() - (padY * 2f)).coerceAtMost(cardRect.width() * 0.32f)
        val clockCx = cardRect.left + padX + (clockDiameter / 2f)
        val clockCy = cardRect.centerY()
        val clockRadius = clockDiameter / 2f

        val clockRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            style = Paint.Style.STROKE
            strokeWidth = (clockDiameter * 0.035f).coerceAtLeast(1.5f * scaleFactor)
        }
        canvas.drawCircle(clockCx, clockCy, clockRadius - (clockRingPaint.strokeWidth / 2f), clockRingPaint)

        val hourHandLength = clockRadius * 0.48f
        val hourHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            style = Paint.Style.STROKE
            strokeWidth = (clockDiameter * 0.05f).coerceAtLeast(2.5f * scaleFactor)
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(clockCx, clockCy, (clockCx + hourHandLength * Math.cos(hourAngle)).toFloat(), (clockCy + hourHandLength * Math.sin(hourAngle)).toFloat(), hourHandPaint)

        val minHandLength = clockRadius * 0.72f
        val minHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            style = Paint.Style.STROKE
            strokeWidth = (clockDiameter * 0.035f).coerceAtLeast(1.8f * scaleFactor)
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(clockCx, clockCy, (clockCx + minHandLength * Math.cos(minuteAngle)).toFloat(), (clockCy + minHandLength * Math.sin(minuteAngle)).toFloat(), minHandPaint)

        val secHandLength = clockRadius * 0.82f
        val secHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.STROKE
            strokeWidth = (clockDiameter * 0.025f).coerceAtLeast(1.2f * scaleFactor)
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(clockCx, clockCy, (clockCx + secHandLength * Math.cos(secondAngle)).toFloat(), (clockCy + secHandLength * Math.sin(secondAngle)).toFloat(), secHandPaint)

        val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            style = Paint.Style.FILL
        }
        canvas.drawCircle(clockCx, clockCy, clockDiameter * 0.04f, capPaint)

        val timelineLeft = clockCx + clockRadius + (cardRect.width() * 0.06f)
        val timelineRight = cardRect.right - padX
        val timelineW = timelineRight - timelineLeft
        val timelineTop = cardRect.top + padY
        val timelineH = cardRect.height() - (padY * 2f)
        val rowH = timelineH / 3f

        val pillCenterY = timelineTop + (rowH * 1.5f)
        val pillHeight = rowH * 0.82f
        val pillRect = RectF(timelineLeft, pillCenterY - (pillHeight / 2f), timelineRight, pillCenterY + (pillHeight / 2f))
        val pillRadius = pillHeight * 0.28f
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(pillRect, pillRadius, pillRadius, pillPaint)

        val fontScale = (rowH * 0.42f).coerceAtLeast(10f * scaleFactor)
        val sidePad = timelineW * 0.06f

        val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = fontScale
            typeface = getSlateFont(context, weight = 500)
        }

        val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = activeTextColor
            textSize = fontScale * 1.1f
            typeface = getSlateFont(context, weight = 700)
        }

        fun drawTimelineRow(row: Int, dayText: String, dateText: String, isToday: Boolean) {
            val centerY = timelineTop + (row * rowH) + (rowH / 2f)
            val paint = if (isToday) activePaint else inactivePaint

            val fm = paint.fontMetrics
            val textY = centerY - ((fm.descent + fm.ascent) / 2f)

            val leftX = timelineLeft + sidePad
            val rightX = timelineRight - sidePad

            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(dayText, leftX, textY, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(dateText, rightX, textY, paint)
        }

        drawTimelineRow(0, prevDay, prevNum, isToday = false)
        drawTimelineRow(1, todayDay, todayNum, isToday = true)
        drawTimelineRow(2, nextDay, nextNum, isToday = false)
    } else {
        val padX = cardRect.width() * 0.08f
        val padY = cardRect.height() * 0.08f

        val topClockH = cardRect.height() * 0.48f
        val clockDiameter = topClockH.coerceAtMost(cardRect.width() * 0.50f)
        val clockCx = cardRect.centerX()
        val clockCy = cardRect.top + padY + (topClockH / 2f)
        val clockRadius = clockDiameter / 2f

        val clockRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            style = Paint.Style.STROKE
            strokeWidth = (clockDiameter * 0.035f).coerceAtLeast(1.5f * scaleFactor)
        }
        canvas.drawCircle(clockCx, clockCy, clockRadius - (clockRingPaint.strokeWidth / 2f), clockRingPaint)

        val hourHandLength = clockRadius * 0.48f
        val hourHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            style = Paint.Style.STROKE
            strokeWidth = (clockDiameter * 0.05f).coerceAtLeast(2.5f * scaleFactor)
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(clockCx, clockCy, (clockCx + hourHandLength * Math.cos(hourAngle)).toFloat(), (clockCy + hourHandLength * Math.sin(hourAngle)).toFloat(), hourHandPaint)

        val minHandLength = clockRadius * 0.72f
        val minHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            style = Paint.Style.STROKE
            strokeWidth = (clockDiameter * 0.035f).coerceAtLeast(1.8f * scaleFactor)
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(clockCx, clockCy, (clockCx + minHandLength * Math.cos(minuteAngle)).toFloat(), (clockCy + minHandLength * Math.sin(minuteAngle)).toFloat(), minHandPaint)

        val secHandLength = clockRadius * 0.82f
        val secHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.STROKE
            strokeWidth = (clockDiameter * 0.025f).coerceAtLeast(1.2f * scaleFactor)
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(clockCx, clockCy, (clockCx + secHandLength * Math.cos(secondAngle)).toFloat(), (clockCy + secHandLength * Math.sin(secondAngle)).toFloat(), secHandPaint)

        val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            style = Paint.Style.FILL
        }
        canvas.drawCircle(clockCx, clockCy, clockDiameter * 0.04f, capPaint)

        val timelineTop = clockCy + clockRadius + (cardRect.height() * 0.04f)
        val timelineBottom = cardRect.bottom - padY
        val timelineH = timelineBottom - timelineTop
        val rowH = timelineH / 3f

        val timelineLeft = cardRect.left + padX
        val timelineRight = cardRect.right - padX
        val timelineW = timelineRight - timelineLeft

        val pillCenterY = timelineTop + (rowH * 1.5f)
        val pillHeight = rowH * 0.82f
        val pillRect = RectF(timelineLeft, pillCenterY - (pillHeight / 2f), timelineRight, pillCenterY + (pillHeight / 2f))
        val pillRadius = pillHeight * 0.28f
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(pillRect, pillRadius, pillRadius, pillPaint)

        val fontScale = (rowH * 0.42f).coerceAtLeast(10f * scaleFactor)
        val sidePad = timelineW * 0.06f

        val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = fontScale
            typeface = getSlateFont(context, weight = 500)
        }

        val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = activeTextColor
            textSize = fontScale * 1.1f
            typeface = getSlateFont(context, weight = 700)
        }

        fun drawTimelineRow(row: Int, dayText: String, dateText: String, isToday: Boolean) {
            val centerY = timelineTop + (row * rowH) + (rowH / 2f)
            val paint = if (isToday) activePaint else inactivePaint

            val fm = paint.fontMetrics
            val textY = centerY - ((fm.descent + fm.ascent) / 2f)

            val leftX = timelineLeft + sidePad
            val rightX = timelineRight - sidePad

            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(dayText, leftX, textY, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(dateText, rightX, textY, paint)
        }

        drawTimelineRow(0, prevDay, prevNum, isToday = false)
        drawTimelineRow(1, todayDay, todayNum, isToday = true)
        drawTimelineRow(2, nextDay, nextNum, isToday = false)
    }

    return bitmap
}

fun generateAnalogTimelineCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateAnalogTimelineCalendarBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 15. WEEK PROGRESS CALENDAR (4x2)
fun generateWeekProgressCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    val margin = scaleFactor * 1.5f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val targetRatio = 2.0f
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val futureStrokeColor = if (isLight) Color.parseColor("#C7C7CC") else Color.parseColor("#48484A")

    val padX = cardRect.width() * 0.08f
    val padY = cardRect.height() * 0.12f
    val availW = cardRect.width() - (padX * 2f)

    val cal = java.util.Calendar.getInstance()
    val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
    val currentDayIndex = if (dayOfWeek == java.util.Calendar.SUNDAY) 7 else dayOfWeek - 1

    val headerY = cardRect.top + padY + (cardRect.height() * 0.18f)
    val capsulesY = cardRect.top + padY + (cardRect.height() * 0.38f)
    val footerY = cardRect.bottom - padY - (cardRect.height() * 0.02f)

    val dayText = state.dayOfWeekShort.uppercase()
    val dateText = state.dayOfMonth

    var baseHeaderSize = (cardRect.height() * 0.15f).coerceAtLeast(12f * scaleFactor)

    val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = baseHeaderSize
        typeface = getSlateFont(context, weight = 700)
        letterSpacing = 0.05f
    }

    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = baseHeaderSize
        typeface = getSlateFont(context, weight = 400)
    }

    val spaceW = baseHeaderSize * 0.35f
    val totalHeaderW = dayPaint.measureText(dayText) + spaceW + datePaint.measureText(dateText)

    if (totalHeaderW > availW) {
        val scale = availW / totalHeaderW
        baseHeaderSize *= scale
        dayPaint.textSize = baseHeaderSize
        datePaint.textSize = baseHeaderSize
    }

    val startX = cardRect.left + padX
    canvas.drawText(dayText, startX, headerY, dayPaint)
    val dayW = dayPaint.measureText(dayText)
    canvas.drawText(dateText, startX + dayW + spaceW, headerY, datePaint)

    val gap = (availW * 0.022f).coerceAtLeast(4f * scaleFactor)
    val capsuleW = (availW - (gap * 6f)) / 7f
    val capsuleH = (cardRect.height() * 0.07f).coerceIn(5f * scaleFactor, 12f * scaleFactor)
    val capsuleRadius = capsuleH / 2f

    val filledPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val strokePillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = futureStrokeColor
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }

    val cTop = capsulesY - (capsuleH / 2f)
    val cBottom = cTop + capsuleH

    for (i in 1..7) {
        val cLeft = startX + (i - 1) * (capsuleW + gap)
        val cRight = cLeft + capsuleW
        val capsuleRect = RectF(cLeft, cTop, cRight, cBottom)

        when {
            i < currentDayIndex -> {
                filledPillPaint.color = primaryText
                canvas.drawRoundRect(capsuleRect, capsuleRadius, capsuleRadius, filledPillPaint)
            }
            i == currentDayIndex -> {
                filledPillPaint.color = accentColorInt
                canvas.drawRoundRect(capsuleRect, capsuleRadius, capsuleRadius, filledPillPaint)
            }
            else -> {
                val inset = strokePillPaint.strokeWidth / 2f
                val insetRect = RectF(cLeft + inset, cTop + inset, cRight - inset, cBottom - inset)
                canvas.drawRoundRect(insetRect, capsuleRadius, capsuleRadius, strokePillPaint)
            }
        }
    }

    val footerText = "$currentDayIndex of 7 days this week gone"
    var baseFooterSize = (cardRect.height() * 0.10f).coerceAtLeast(10f * scaleFactor)

    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = baseFooterSize
        typeface = getSlateFont(context, weight = 400)
        textAlign = Paint.Align.LEFT
    }

    if (footerPaint.measureText(footerText) > availW) {
        baseFooterSize *= (availW / footerPaint.measureText(footerText))
        footerPaint.textSize = baseFooterSize
    }

    canvas.drawText(footerText, startX, footerY, footerPaint)

    return bitmap
}

fun generateWeekProgressCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateWeekProgressCalendarBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 16. MODULAR MATRIX CALENDAR (4x2)
fun generateModularMatrixCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val margin = scaleFactor * 1.5f
    val targetRatio = 2.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = getSafeBgColor(config)
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val tileBgColor = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#18181A")
    val tileStrokeColor = if (isLight) Color.parseColor("#E5E5EA") else Color.parseColor("#2C2C2E")

    val r = Color.red(accentColorInt)
    val g = Color.green(accentColorInt)
    val b = Color.blue(accentColorInt)
    val accentLuminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    val activeTextColor = if (accentLuminance > 0.65) Color.parseColor("#121214") else Color.WHITE

    val padX = cardRect.width() * 0.06f
    val padY = cardRect.height() * 0.08f
    val usableW = cardRect.width() - (padX * 2f)

    val cal = java.util.Calendar.getInstance()
    val weekOfYear = cal.get(java.util.Calendar.WEEK_OF_YEAR)

    val tileBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val tileStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tileStrokeColor
        style = Paint.Style.STROKE
        strokeWidth = 1f * scaleFactor
    }

    val weekCal = (cal.clone() as java.util.Calendar).apply {
        firstDayOfWeek = java.util.Calendar.MONDAY
        set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
    }

    val todayNum = cal.get(java.util.Calendar.DAY_OF_MONTH)
    val todayMonth = cal.get(java.util.Calendar.MONTH)
    val dayNames = arrayOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    val fullMonthTitle = state.monthShort.uppercase()

    val aspectRatio = cardRect.width() / cardRect.height()

    if (aspectRatio >= 1.2f) {
        val headerText = "WEEK $weekOfYear OF 52  •  MAKE IT COUNT"
        var headerTextSize = (cardRect.height() * 0.08f).coerceAtLeast(8f * scaleFactor)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = headerTextSize
            typeface = getSlateFont(context, weight = 500)
            letterSpacing = 0.08f
            textAlign = Paint.Align.LEFT
        }

        if (headerPaint.measureText(headerText) > usableW) {
            headerTextSize *= (usableW / headerPaint.measureText(headerText))
            headerPaint.textSize = headerTextSize
        }

        val headerY = cardRect.top + padY + headerPaint.textSize
        canvas.drawText(headerText, cardRect.left + padX, headerY, headerPaint)

        val gridTop = headerY + (cardRect.height() * 0.06f)
        val gridH = cardRect.bottom - padY - gridTop

        val yearColW = usableW * 0.18f
        val colGap = usableW * 0.025f
        val rightGridLeft = cardRect.left + padX + yearColW + colGap
        val rightGridW = cardRect.right - padX - rightGridLeft

        val gapX = rightGridW * 0.022f
        val gapY = gridH * 0.06f
        val unitW = (rightGridW - (gapX * 4f)) / 5f
        val tileH = (gridH - gapY) / 2f
        val tileRadius = (cardRadius - padY).coerceAtLeast(scaleFactor * 6f)

        val row1Y = gridTop
        val row2Y = gridTop + tileH + gapY

        val row1CenterY = row1Y + (tileH / 2f)
        val row2CenterY = row2Y + (tileH / 2f)

        val yearStr = state.year
        val yearTop = if (yearStr.length >= 2) yearStr.substring(0, 2) else "20"
        val yearBottom = if (yearStr.length >= 4) yearStr.substring(2, 4) else "26"

        var yearTextSize = tileH * 0.88f
        val yearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = yearTextSize
            typeface = getSlateFont(context, weight = 300)
            textAlign = Paint.Align.CENTER
        }

        val maxYearW = yearColW * 0.92f
        val maxMeasuredYearW = maxOf(yearPaint.measureText(yearTop), yearPaint.measureText(yearBottom))
        if (maxMeasuredYearW > maxYearW) {
            yearTextSize *= (maxYearW / maxMeasuredYearW)
            yearPaint.textSize = yearTextSize
        }

        val yearCenterX = cardRect.left + padX + (yearColW / 2f)
        val fmYear = yearPaint.fontMetrics

        val year1Y = row1CenterY - ((fmYear.descent + fmYear.ascent) / 2f)
        val year2Y = row2CenterY - ((fmYear.descent + fmYear.ascent) / 2f)

        canvas.drawText(yearTop, yearCenterX, year1Y, yearPaint)
        canvas.drawText(yearBottom, yearCenterX, year2Y, yearPaint)

        for (i in 0..2) {
            val tLeft = rightGridLeft + (i * (unitW + gapX))
            val tRect = RectF(tLeft, row1Y, tLeft + unitW, row1Y + tileH)

            val tileDate = weekCal.get(java.util.Calendar.DAY_OF_MONTH)
            val tileMonth = weekCal.get(java.util.Calendar.MONTH)
            val isToday = (tileDate == todayNum && tileMonth == todayMonth)

            drawBentoDayTile(canvas, context, tRect, tileRadius, dayNames[i], tileDate.toString(), isToday, accentColorInt, activeTextColor, tileBgColor, primaryText, secondaryText, tileBgPaint, tileStrokePaint, scaleFactor)
            weekCal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }

        val monthTileLeft = rightGridLeft + (3 * (unitW + gapX))
        val monthTileW = (unitW * 2f) + gapX
        val monthTileRect = RectF(monthTileLeft, row1Y, monthTileLeft + monthTileW, row1Y + tileH)

        tileBgPaint.color = tileBgColor
        canvas.drawRoundRect(monthTileRect, tileRadius, tileRadius, tileBgPaint)
        canvas.drawRoundRect(monthTileRect, tileRadius, tileRadius, tileStrokePaint)

        var monthTextSize = (tileH * 0.36f).coerceAtLeast(10f * scaleFactor)
        val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = monthTextSize
            typeface = getSlateFont(context, weight = 700)
            letterSpacing = 0.10f
            textAlign = Paint.Align.CENTER
        }

        val maxMonthW = monthTileRect.width() * 0.85f
        if (monthPaint.measureText(fullMonthTitle) > maxMonthW) {
            monthTextSize *= (maxMonthW / monthPaint.measureText(fullMonthTitle))
            monthPaint.textSize = monthTextSize
        }

        val fmM = monthPaint.fontMetrics
        val monthY = monthTileRect.centerY() - ((fmM.descent + fmM.ascent) / 2f)
        canvas.drawText(fullMonthTitle, monthTileRect.centerX(), monthY, monthPaint)

        for (i in 3..6) {
            val colIdx = i - 3
            val tLeft = rightGridLeft + (colIdx * (unitW + gapX))
            val tRect = RectF(tLeft, row2Y, tLeft + unitW, row2Y + tileH)

            val tileDate = weekCal.get(java.util.Calendar.DAY_OF_MONTH)
            val tileMonth = weekCal.get(java.util.Calendar.MONTH)
            val isToday = (tileDate == todayNum && tileMonth == todayMonth)

            drawBentoDayTile(canvas, context, tRect, tileRadius, dayNames[i], tileDate.toString(), isToday, accentColorInt, activeTextColor, tileBgColor, primaryText, secondaryText, tileBgPaint, tileStrokePaint, scaleFactor)
            weekCal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }

        val iconTileLeft = rightGridLeft + (4 * (unitW + gapX))
        val iconTileRect = RectF(iconTileLeft, row2Y, iconTileLeft + unitW, row2Y + tileH)

        tileBgPaint.color = tileBgColor
        canvas.drawRoundRect(iconTileRect, tileRadius, tileRadius, tileBgPaint)
        canvas.drawRoundRect(iconTileRect, tileRadius, tileRadius, tileStrokePaint)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }
        val dotR = (minOf(unitW, tileH) * 0.08f).coerceIn(1.5f * scaleFactor, 4f * scaleFactor)
        val dotOffset = dotR * 2.2f
        val icCx = iconTileRect.centerX()
        val icCy = iconTileRect.centerY()

        canvas.drawCircle(icCx - dotOffset, icCy - dotOffset, dotR, dotPaint)
        canvas.drawCircle(icCx + dotOffset, icCy - dotOffset, dotR, dotPaint)
        canvas.drawCircle(icCx - dotOffset, icCy + dotOffset, dotR, dotPaint)
        canvas.drawCircle(icCx + dotOffset, icCy + dotOffset, dotR, dotPaint)
    } else {
        val headerText = "WEEK $weekOfYear OF 52"
        var headerTextSize = (cardRect.height() * 0.06f).coerceAtLeast(8f * scaleFactor)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = headerTextSize
            typeface = getSlateFont(context, weight = 500)
            letterSpacing = 0.08f
            textAlign = Paint.Align.CENTER
        }

        if (headerPaint.measureText(headerText) > usableW) {
            headerTextSize *= (usableW / headerPaint.measureText(headerText))
            headerPaint.textSize = headerTextSize
        }

        val headerY = cardRect.top + padY + headerPaint.textSize
        canvas.drawText(headerText, cardRect.centerX(), headerY, headerPaint)

        val gridTop = headerY + (cardRect.height() * 0.04f)
        val gridH = cardRect.bottom - padY - gridTop

        val gapX = usableW * 0.025f
        val gapY = gridH * 0.04f
        val unitW = (usableW - (gapX * 3f)) / 4f
        val tileH = (gridH - (gapY * 2f)) / 3f
        val tileRadius = (cardRadius - padY).coerceAtLeast(scaleFactor * 6f)

        val row1Y = gridTop
        val row2Y = gridTop + tileH + gapY
        val row3Y = gridTop + (tileH * 2f) + (gapY * 2f)

        val yearTileW = (unitW * 2f) + gapX
        val yearTileRect = RectF(cardRect.left + padX, row1Y, cardRect.left + padX + yearTileW, row1Y + tileH)

        tileBgPaint.color = tileBgColor
        canvas.drawRoundRect(yearTileRect, tileRadius, tileRadius, tileBgPaint)
        canvas.drawRoundRect(yearTileRect, tileRadius, tileRadius, tileStrokePaint)

        val yearStr = state.year
        var yearTextSize = (tileH * 0.45f).coerceAtLeast(10f * scaleFactor)
        val yearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = yearTextSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f
        }
        val fmYr = yearPaint.fontMetrics
        val yearY = yearTileRect.centerY() - ((fmYr.descent + fmYr.ascent) / 2f)
        canvas.drawText(yearStr, yearTileRect.centerX(), yearY, yearPaint)

        val monthTileLeft = yearTileRect.right + gapX
        val monthTileRect = RectF(monthTileLeft, row1Y, monthTileLeft + yearTileW, row1Y + tileH)

        canvas.drawRoundRect(monthTileRect, tileRadius, tileRadius, tileBgPaint)
        canvas.drawRoundRect(monthTileRect, tileRadius, tileRadius, tileStrokePaint)

        var monthTextSize = (tileH * 0.42f).coerceAtLeast(10f * scaleFactor)
        val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = monthTextSize
            typeface = getSlateFont(context, weight = 700)
            letterSpacing = 0.10f
            textAlign = Paint.Align.CENTER
        }
        val fmM = monthPaint.fontMetrics
        val monthY = monthTileRect.centerY() - ((fmM.descent + fmM.ascent) / 2f)
        canvas.drawText(fullMonthTitle, monthTileRect.centerX(), monthY, monthPaint)

        for (i in 0..3) {
            val tLeft = cardRect.left + padX + (i * (unitW + gapX))
            val tRect = RectF(tLeft, row2Y, tLeft + unitW, row2Y + tileH)

            val tileDate = weekCal.get(java.util.Calendar.DAY_OF_MONTH)
            val tileMonth = weekCal.get(java.util.Calendar.MONTH)
            val isToday = (tileDate == todayNum && tileMonth == todayMonth)

            drawBentoDayTile(canvas, context, tRect, tileRadius, dayNames[i], tileDate.toString(), isToday, accentColorInt, activeTextColor, tileBgColor, primaryText, secondaryText, tileBgPaint, tileStrokePaint, scaleFactor)
            weekCal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }

        for (i in 4..6) {
            val colIdx = i - 4
            val tLeft = cardRect.left + padX + (colIdx * (unitW + gapX))
            val tRect = RectF(tLeft, row3Y, tLeft + unitW, row3Y + tileH)

            val tileDate = weekCal.get(java.util.Calendar.DAY_OF_MONTH)
            val tileMonth = weekCal.get(java.util.Calendar.MONTH)
            val isToday = (tileDate == todayNum && tileMonth == todayMonth)

            drawBentoDayTile(canvas, context, tRect, tileRadius, dayNames[i], tileDate.toString(), isToday, accentColorInt, activeTextColor, tileBgColor, primaryText, secondaryText, tileBgPaint, tileStrokePaint, scaleFactor)
            weekCal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }

        val iconTileLeft = cardRect.left + padX + (3 * (unitW + gapX))
        val iconTileRect = RectF(iconTileLeft, row3Y, iconTileLeft + unitW, row3Y + tileH)

        tileBgPaint.color = tileBgColor
        canvas.drawRoundRect(iconTileRect, tileRadius, tileRadius, tileBgPaint)
        canvas.drawRoundRect(iconTileRect, tileRadius, tileRadius, tileStrokePaint)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }
        val dotR = (minOf(unitW, tileH) * 0.08f).coerceIn(1.5f * scaleFactor, 4f * scaleFactor)
        val dotOffset = dotR * 2.2f
        val icCx = iconTileRect.centerX()
        val icCy = iconTileRect.centerY()

        canvas.drawCircle(icCx - dotOffset, icCy - dotOffset, dotR, dotPaint)
        canvas.drawCircle(icCx + dotOffset, icCy - dotOffset, dotR, dotPaint)
        canvas.drawCircle(icCx - dotOffset, icCy + dotOffset, dotR, dotPaint)
        canvas.drawCircle(icCx + dotOffset, icCy + dotOffset, dotR, dotPaint)
    }

    return bitmap
}

fun generateModularMatrixCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateModularMatrixCalendarBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

private fun drawBentoDayTile(canvas: Canvas, context: Context, rect: RectF, radius: Float, dayName: String, dateNum: String, isToday: Boolean, accentColor: Int, activeTextColor: Int, tileBgColor: Int, primaryText: Int, secondaryText: Int, bgPaint: Paint, strokePaint: Paint, scaleFactor: Float) {
    if (isToday) {
        bgPaint.color = accentColor
        canvas.drawRoundRect(rect, radius, radius, bgPaint)
    } else {
        bgPaint.color = tileBgColor
        canvas.drawRoundRect(rect, radius, radius, bgPaint)
        canvas.drawRoundRect(rect, radius, radius, strokePaint)
    }

    val labelColor = if (isToday) activeTextColor else secondaryText
    val numColor = if (isToday) activeTextColor else primaryText
    val maxTileTextW = rect.width() * 0.85f
    val cx = rect.centerX()

    if (isToday) {
        var labelSize = (rect.height() * 0.22f).coerceAtLeast(7f * scaleFactor)
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor
            textSize = labelSize
            typeface = getSlateFont(context, weight = 500)
            letterSpacing = 0.04f
            textAlign = Paint.Align.CENTER
        }
        if (labelPaint.measureText(dayName) > maxTileTextW) {
            labelPaint.textSize = labelSize * (maxTileTextW / labelPaint.measureText(dayName))
        }

        var numSize = (rect.height() * 0.44f).coerceAtLeast(9f * scaleFactor)
        val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = numColor
            textSize = numSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }
        if (numPaint.measureText(dateNum) > maxTileTextW) {
            numPaint.textSize = numSize * (maxTileTextW / numPaint.measureText(dateNum))
        }

        val fmL = labelPaint.fontMetrics
        val fmN = numPaint.fontMetrics

        val gap = -2.5f * scaleFactor
        val labelH = -fmL.ascent + fmL.descent
        val numH = -fmN.ascent + fmN.descent
        val totalBlockH = labelH + gap + numH

        val blockTop = rect.centerY() - (totalBlockH / 2f)
        val labelY = blockTop - fmL.ascent
        val numY = labelY + fmL.descent + gap - fmN.ascent

        canvas.drawText(dayName, cx, labelY, labelPaint)
        canvas.drawText(dateNum, cx, numY, numPaint)
    } else {
        var labelSize = (rect.height() * 0.22f).coerceAtLeast(7f * scaleFactor)
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor
            textSize = labelSize
            typeface = getSlateFont(context, weight = 400)
            letterSpacing = 0.04f
            textAlign = Paint.Align.CENTER
        }
        if (labelPaint.measureText(dayName) > maxTileTextW) {
            labelPaint.textSize = labelSize * (maxTileTextW / labelPaint.measureText(dayName))
        }

        val fmL = labelPaint.fontMetrics
        val labelY = rect.centerY() - ((fmL.descent + fmL.ascent) / 2f)
        canvas.drawText(dayName, cx, labelY, labelPaint)
    }
}

// 17. ELEGANT OVERVIEW CALENDAR (4x2)
fun generateOverviewCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val margin = scaleFactor * 1.5f
    val targetRatio = 2.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)
    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val dimText = if (isLight) Color.parseColor("#C7C7CC") else Color.parseColor("#48484A")

    val r = Color.red(accentColorInt)
    val g = Color.green(accentColorInt)
    val b = Color.blue(accentColorInt)
    val accentLuminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    val activeTextColor = if (accentLuminance > 0.65) Color.parseColor("#161618") else Color.WHITE

    val fullMonthTitle = when (state.monthShort.uppercase()) {
        "JAN" -> "January"
        "FEB" -> "February"
        "MAR" -> "March"
        "APR" -> "April"
        "MAY" -> "May"
        "JUN" -> "June"
        "JUL" -> "July"
        "AUG" -> "August"
        "SEP" -> "September"
        "OCT" -> "October"
        "NOV" -> "November"
        "DEC" -> "December"
        else -> state.monthShort.lowercase().replaceFirstChar { it.uppercase() }
    }

    val cal = java.util.Calendar.getInstance().apply {
        val yearInt = state.year.toIntOrNull() ?: get(java.util.Calendar.YEAR)
        try {
            val parsedDate = java.text.SimpleDateFormat("MMM", java.util.Locale.ENGLISH).parse(state.monthShort)
            if (parsedDate != null) {
                val tempCal = java.util.Calendar.getInstance().apply { time = parsedDate }
                set(java.util.Calendar.MONTH, tempCal.get(java.util.Calendar.MONTH))
            }
        } catch (_: Exception) {}
        set(java.util.Calendar.YEAR, yearInt)
        set(java.util.Calendar.DAY_OF_MONTH, 1)
    }

    val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val firstDaySunIndex = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
    val currentDayNum = state.dayOfMonth.toIntOrNull() ?: -1

    val totalCells = firstDaySunIndex + daysInMonth
    val dateRowsNeeded = kotlin.math.ceil(totalCells / 7.0).toInt()
    val totalGridRows = dateRowsNeeded + 1

    val dayHeaderLabels = arrayOf("S", "M", "T", "W", "T", "F", "S")

    val activeBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }

    val aspectRatio = cardRect.width() / cardRect.height()

    if (aspectRatio >= 1.2f) {
        val padX = cardRect.width() * 0.06f
        val padY = cardRect.height() * 0.09f

        val leftSectionW = cardRect.width() * 0.28f
        val rightSectionLeft = cardRect.left + padX + leftSectionW + (cardRect.width() * 0.03f)
        val rightSectionW = cardRect.right - padX - rightSectionLeft

        val colW = rightSectionW / 7f

        var monthTitleSize = (cardRect.height() * 0.15f).coerceIn(11f * scaleFactor, 20f * scaleFactor)
        val monthTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = monthTitleSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.RIGHT
        }

        val fmM = monthTitlePaint.fontMetrics
        val monthY = cardRect.top + padY - fmM.ascent

        val gridTop = monthY + (4f * scaleFactor)
        val availableGridH = cardRect.bottom - padY - gridTop
        val rowH = availableGridH / totalGridRows

        val fontScale = minOf(colW * 0.44f, rowH * 0.48f).coerceIn(6f * scaleFactor, 12f * scaleFactor)

        val dateNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontScale
            typeface = getSlateFont(context, weight = 400)
            textAlign = Paint.Align.CENTER
        }

        val lastRowIndex = totalGridRows - 1
        val lastRowCenterY = gridTop + (lastRowIndex * rowH) + (rowH / 2f)
        val fmGrid = dateNumPaint.fontMetrics
        val lastRowGridBaseline = lastRowCenterY - ((fmGrid.descent + fmGrid.ascent) / 2f)

        val weekdayText = state.dayOfWeekShort.lowercase().replaceFirstChar { it.uppercase() }
        val dateText = state.dayOfMonth
        val leftMaxW = leftSectionW - (2f * scaleFactor)

        var baseWeekdaySize = (cardRect.height() * 0.18f).coerceIn(11f * scaleFactor, 22f * scaleFactor)
        val weekdayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = baseWeekdaySize
            typeface = getSlateFont(context, weight = 400)
            textAlign = Paint.Align.LEFT
        }
        if (weekdayPaint.measureText(weekdayText) > leftMaxW) {
            baseWeekdaySize *= (leftMaxW / weekdayPaint.measureText(weekdayText))
            weekdayPaint.textSize = baseWeekdaySize
        }

        val fmW = weekdayPaint.fontMetrics
        val weekdayY = cardRect.top + padY - fmW.ascent

        var baseDateSize = (cardRect.height() * 0.52f).coerceIn(26f * scaleFactor, 126f * scaleFactor)
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = baseDateSize
            typeface = getSlateFont(context, weight = 300)
            textAlign = Paint.Align.RIGHT
        }
        if (datePaint.measureText(dateText) > leftMaxW) {
            baseDateSize *= (leftMaxW / datePaint.measureText(dateText))
            datePaint.textSize = baseDateSize
        }

        val dateY = lastRowGridBaseline
        val leftX = cardRect.left + padX
        val dateRightX = cardRect.left + padX + leftSectionW

        canvas.drawText(weekdayText, leftX, weekdayY, weekdayPaint)
        canvas.drawText(dateText, dateRightX, dateY, datePaint)

        val dayHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = fontScale * 0.88f
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }

        val gridContentRightX = rightSectionLeft + (6.5f * colW) + (dayHeaderPaint.measureText("S") / 2f)
        if (monthTitlePaint.measureText(fullMonthTitle) > rightSectionW) {
            monthTitleSize *= (rightSectionW / monthTitlePaint.measureText(fullMonthTitle))
            monthTitlePaint.textSize = monthTitleSize
        }

        canvas.drawText(fullMonthTitle, gridContentRightX, monthY, monthTitlePaint)

        val row0CenterY = gridTop + (rowH / 2f)
        val fmH = dayHeaderPaint.fontMetrics
        val row0TextY = row0CenterY - ((fmH.descent + fmH.ascent) / 2f)

        for (c in 0..6) {
            val cx = rightSectionLeft + (c * colW) + (colW / 2f)
            canvas.drawText(dayHeaderLabels[c], cx, row0TextY, dayHeaderPaint)
        }

        val prevCal = (cal.clone() as java.util.Calendar).apply { add(java.util.Calendar.MONTH, -1) }
        val prevMaxDays = prevCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

        var cellIndex = 0

        for (i in (firstDaySunIndex - 1) downTo 0) {
            val dayNum = prevMaxDays - i
            val c = cellIndex % 7
            val rIdx = (cellIndex / 7) + 1
            val cx = rightSectionLeft + (c * colW) + (colW / 2f)
            val cy = gridTop + (rIdx * rowH) + (rowH / 2f)

            dateNumPaint.color = dimText
            dateNumPaint.typeface = getSlateFont(context, weight = 400)
            val fm = dateNumPaint.fontMetrics
            val textY = cy - ((fm.descent + fm.ascent) / 2f)
            canvas.drawText(dayNum.toString(), cx, textY, dateNumPaint)
            cellIndex++
        }

        for (day in 1..daysInMonth) {
            val c = cellIndex % 7
            val rIdx = (cellIndex / 7) + 1
            if (rIdx >= totalGridRows) break

            val cx = rightSectionLeft + (c * colW) + (colW / 2f)
            val cy = gridTop + (rIdx * rowH) + (rowH / 2f)

            if (day == currentDayNum) {
                val badgeRadius = minOf(colW * 0.40f, rowH * 0.42f)
                canvas.drawCircle(cx, cy, badgeRadius, activeBadgePaint)

                dateNumPaint.color = activeTextColor
                dateNumPaint.typeface = getSlateFont(context, weight = 700)

                val fm = dateNumPaint.fontMetrics
                val textY = cy - ((fm.descent + fm.ascent) / 2f)
                canvas.drawText(day.toString(), cx, textY, dateNumPaint)
            } else {
                dateNumPaint.color = primaryText
                dateNumPaint.typeface = getSlateFont(context, weight = 400)

                val fm = dateNumPaint.fontMetrics
                val textY = cy - ((fm.descent + fm.ascent) / 2f)
                canvas.drawText(day.toString(), cx, textY, dateNumPaint)
            }
            cellIndex++
        }

        var nextDayNum = 1
        while (cellIndex < (totalGridRows - 1) * 7) {
            val c = cellIndex % 7
            val rIdx = (cellIndex / 7) + 1
            val cx = rightSectionLeft + (c * colW) + (colW / 2f)
            val cy = gridTop + (rIdx * rowH) + (rowH / 2f)

            dateNumPaint.color = dimText
            dateNumPaint.typeface = getSlateFont(context, weight = 400)
            val fm = dateNumPaint.fontMetrics
            val textY = cy - ((fm.descent + fm.ascent) / 2f)
            canvas.drawText(nextDayNum.toString(), cx, textY, dateNumPaint)
            nextDayNum++
            cellIndex++
        }
    } else {
        val padX = cardRect.width() * 0.06f
        val padY = cardRect.height() * 0.06f

        val headerH = cardRect.height() * 0.18f
        val headerRect = RectF(cardRect.left + padX, cardRect.top + padY, cardRect.right - padX, cardRect.top + padY + headerH)

        val weekdayTitle = state.dayOfWeekShort.lowercase().replaceFirstChar { it.uppercase() }
        val leftInfoText = "${state.dayOfMonth} $weekdayTitle"

        val leftInfoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = headerH * 0.58f
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.LEFT
        }
        val fmL = leftInfoPaint.fontMetrics
        val headerCenterY = headerRect.centerY()
        val leftY = headerCenterY - ((fmL.descent + fmL.ascent) / 2f)
        canvas.drawText(leftInfoText, headerRect.left, leftY, leftInfoPaint)

        val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = headerH * 0.48f
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.RIGHT
        }
        val fmM = monthPaint.fontMetrics
        val monthY = headerCenterY - ((fmM.descent + fmM.ascent) / 2f)
        canvas.drawText(fullMonthTitle, headerRect.right, monthY, monthPaint)

        val gridTop = headerRect.bottom + (cardRect.height() * 0.02f)
        val gridH = cardRect.bottom - padY - gridTop
        val gridW = cardRect.width() - (padX * 2f)

        val colW = gridW / 7f
        val rowH = gridH / totalGridRows

        val fontScale = minOf(colW * 0.45f, rowH * 0.48f).coerceAtLeast(8f * scaleFactor)

        val dayHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = fontScale * 0.88f
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }

        val row0CenterY = gridTop + (rowH / 2f)
        val fmH = dayHeaderPaint.fontMetrics
        val row0TextY = row0CenterY - ((fmH.descent + fmH.ascent) / 2f)

        for (c in 0..6) {
            val cx = cardRect.left + padX + (c * colW) + (colW / 2f)
            canvas.drawText(dayHeaderLabels[c], cx, row0TextY, dayHeaderPaint)
        }

        val dateNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontScale
            typeface = getSlateFont(context, weight = 400)
            textAlign = Paint.Align.CENTER
        }

        val prevCal = (cal.clone() as java.util.Calendar).apply { add(java.util.Calendar.MONTH, -1) }
        val prevMaxDays = prevCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

        var cellIndex = 0

        for (i in (firstDaySunIndex - 1) downTo 0) {
            val dayNum = prevMaxDays - i
            val c = cellIndex % 7
            val rIdx = (cellIndex / 7) + 1
            val cx = cardRect.left + padX + (c * colW) + (colW / 2f)
            val cy = gridTop + (rIdx * rowH) + (rowH / 2f)

            dateNumPaint.color = dimText
            dateNumPaint.typeface = getSlateFont(context, weight = 400)
            val fm = dateNumPaint.fontMetrics
            val textY = cy - ((fm.descent + fm.ascent) / 2f)
            canvas.drawText(dayNum.toString(), cx, textY, dateNumPaint)
            cellIndex++
        }

        for (day in 1..daysInMonth) {
            val c = cellIndex % 7
            val rIdx = (cellIndex / 7) + 1
            if (rIdx >= totalGridRows) break

            val cx = cardRect.left + padX + (c * colW) + (colW / 2f)
            val cy = gridTop + (rIdx * rowH) + (rowH / 2f)

            if (day == currentDayNum) {
                val badgeRadius = minOf(colW * 0.40f, rowH * 0.42f)
                canvas.drawCircle(cx, cy, badgeRadius, activeBadgePaint)

                dateNumPaint.color = activeTextColor
                dateNumPaint.typeface = getSlateFont(context, weight = 700)

                val fm = dateNumPaint.fontMetrics
                val textY = cy - ((fm.descent + fm.ascent) / 2f)
                canvas.drawText(day.toString(), cx, textY, dateNumPaint)
            } else {
                dateNumPaint.color = primaryText
                dateNumPaint.typeface = getSlateFont(context, weight = 400)

                val fm = dateNumPaint.fontMetrics
                val textY = cy - ((fm.descent + fm.ascent) / 2f)
                canvas.drawText(day.toString(), cx, textY, dateNumPaint)
            }
            cellIndex++
        }

        var nextDayNum = 1
        while (cellIndex < (totalGridRows - 1) * 7) {
            val c = cellIndex % 7
            val rIdx = (cellIndex / 7) + 1
            val cx = cardRect.left + padX + (c * colW) + (colW / 2f)
            val cy = gridTop + (rIdx * rowH) + (rowH / 2f)

            dateNumPaint.color = dimText
            dateNumPaint.typeface = getSlateFont(context, weight = 400)
            val fm = dateNumPaint.fontMetrics
            val textY = cy - ((fm.descent + fm.ascent) / 2f)
            canvas.drawText(nextDayNum.toString(), cx, textY, dateNumPaint)
            nextDayNum++
            cellIndex++
        }
    }

    return bitmap
}

fun generateOverviewCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateOverviewCalendarBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 18. MINIMAL WEEK STRIP CALENDAR (4x2)
fun generateMinimalWeekStripCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    val margin = scaleFactor * 1.5f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val targetRatio = 2.0f
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#8A8A8E")

    val padX = cardRect.width() * 0.08f

    val cal = java.util.Calendar.getInstance()
    val dayOfWeekSun1 = cal.get(java.util.Calendar.DAY_OF_WEEK)
    val currentDayIndex = dayOfWeekSun1 - 1
    val weekOfYear = cal.get(java.util.Calendar.WEEK_OF_YEAR)

    val fullWeekdayName = when (state.dayOfWeekShort.uppercase()) {
        "MON" -> "Monday"
        "TUE" -> "Tuesday"
        "WED" -> "Wednesday"
        "THU" -> "Thursday"
        "FRI" -> "Friday"
        "SAT" -> "Saturday"
        "SUN" -> "Sunday"
        else -> state.dayOfWeekShort
    }
    val detailText = "$fullWeekdayName  ·  Week $weekOfYear"
    val dateText = state.dayOfMonth

    var dateFontSize = (cardRect.height() * 0.38f).coerceIn(28f * scaleFactor, 46f * scaleFactor)
    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = dateFontSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.LEFT
    }

    val gapBetweenDateAndStrip = 14f * scaleFactor
    val dateW = datePaint.measureText(dateText)

    val startX = cardRect.left + padX
    val rightStripLeft = startX + dateW + gapBetweenDateAndStrip

    val availableStripW = cardRect.right - padX - rightStripLeft

    var headerFontSize = (cardRect.height() * 0.16f).coerceIn(11f * scaleFactor, 16f * scaleFactor)
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = headerFontSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val dayHeaders = arrayOf("S", "M", "T", "W", "T", "F", "S")
    val letterWidth = headerPaint.measureText("W")
    val letterGap = ((availableStripW - (letterWidth * 7f)) / 6f).coerceIn(6f * scaleFactor, 18f * scaleFactor)

    var detailFontSize = (cardRect.height() * 0.11f).coerceIn(9f * scaleFactor, 13f * scaleFactor)
    val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = detailFontSize
        typeface = getSlateFont(context, weight = 500)
        textAlign = Paint.Align.LEFT
    }

    if (detailPaint.measureText(detailText) > availableStripW) {
        detailFontSize *= (availableStripW / detailPaint.measureText(detailText))
        detailPaint.textSize = detailFontSize
    }

    val fmD = datePaint.fontMetrics
    val fmH = headerPaint.fontMetrics
    val fmDet = detailPaint.fontMetrics

    val underlineH = 2.5f * scaleFactor
    val underlinePadding = 3f * scaleFactor
    val gapBetweenRows = 8f * scaleFactor

    val line1H = -fmH.ascent + fmH.descent
    val line2H = -fmDet.ascent + fmDet.descent
    val totalRightH = line1H + underlinePadding + underlineH + gapBetweenRows + line2H

    val centerY = cardRect.centerY()
    val rightBlockTop = centerY - (totalRightH / 2f)

    val headerY = rightBlockTop - fmH.ascent
    val underlineTop = headerY + fmH.descent + underlinePadding
    val detailY = underlineTop + underlineH + gapBetweenRows - fmDet.ascent

    val dateY = centerY - ((fmD.descent + fmD.ascent) / 2f)

    canvas.drawText(dateText, startX, dateY, datePaint)

    val activeColor = if (isLight) accentColorInt else Color.WHITE
    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = activeColor
        style = Paint.Style.FILL
    }

    for (i in 0..6) {
        val cx = rightStripLeft + (i * (letterWidth + letterGap)) + (letterWidth / 2f)

        if (i == currentDayIndex) {
            headerPaint.color = activeColor
            canvas.drawText(dayHeaders[i], cx, headerY, headerPaint)

            val underlineW = letterWidth * 0.90f
            val uLeft = cx - (underlineW / 2f)
            val uRect = RectF(uLeft, underlineTop, uLeft + underlineW, underlineTop + underlineH)
            canvas.drawRoundRect(uRect, underlineH / 2f, underlineH / 2f, accentPaint)
        } else {
            headerPaint.color = primaryText
            canvas.drawText(dayHeaders[i], cx, headerY, headerPaint)
        }
    }

    canvas.drawText(detailText, rightStripLeft, detailY, detailPaint)

    return bitmap
}

fun generateMinimalWeekStripCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateMinimalWeekStripCalendarBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 19. VERTICAL TIME PILL WIDGET (4x2)
fun generateVerticalTimePillCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    val margin = scaleFactor * 1.5f
    val targetRatio = 2.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val pillBgColor = if (isLight) Color.parseColor("#E5E5EA") else Color.parseColor("#1C1C1E")

    val r = Color.red(accentColorInt)
    val g = Color.green(accentColorInt)
    val b = Color.blue(accentColorInt)
    val accentLuminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    val activeTextColor = if (accentLuminance > 0.65) Color.parseColor("#121214") else Color.WHITE

    val timeFormatter = java.text.SimpleDateFormat("HHmm", java.util.Locale.getDefault())
    val timeStr = timeFormatter.format(java.util.Date())

    val rawDayInt = state.dayOfMonth.toIntOrNull() ?: 1
    val dateNumStr = String.format(java.util.Locale.getDefault(), "%02d", rawDayInt)
    val dayStr = state.dayOfWeekShort.uppercase()

    val aspectRatio = cardRect.width() / cardRect.height()

    if (aspectRatio >= 1.2f) {
        val padX = cardRect.width() * 0.08f
        val padY = cardRect.height() * 0.12f

        val usableW = cardRect.width() - (padX * 2f)
        val usableH = cardRect.height() - (padY * 2f)

        val leftW = usableW * 0.22f
        val gapX = usableW * 0.06f
        val rightSectionLeft = cardRect.left + padX + leftW + gapX
        val rightSectionW = cardRect.right - padX - rightSectionLeft

        val pillGap = usableH * 0.10f
        val pillH = (usableH - pillGap) / 2f
        val pillRadius = (16f * scaleFactor).coerceAtMost(pillH / 2f)

        val topPillTop = cardRect.top + padY
        val topPillRect = RectF(rightSectionLeft, topPillTop, rightSectionLeft + rightSectionW, topPillTop + pillH)

        val bottomPillTop = topPillRect.bottom + pillGap
        val bottomPillRect = RectF(rightSectionLeft, bottomPillTop, rightSectionLeft + rightSectionW, bottomPillTop + pillH)

        val totalBarsSpanH = bottomPillRect.bottom - topPillRect.top

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            typeface = getSlateFont(context, weight = 700)
            letterSpacing = 0.06f
            textAlign = Paint.Align.CENTER
        }

        var calculatedTimeSize = 20f * scaleFactor
        timePaint.textSize = calculatedTimeSize
        val measuredTimeLength = timePaint.measureText(timeStr)

        if (measuredTimeLength > 0f) {
            calculatedTimeSize *= (totalBarsSpanH / measuredTimeLength)
            timePaint.textSize = calculatedTimeSize
        }

        val fmCheck = timePaint.fontMetrics
        val textThickness = -fmCheck.ascent + fmCheck.descent
        if (textThickness > leftW) {
            calculatedTimeSize *= (leftW / textThickness)
            timePaint.textSize = calculatedTimeSize
        }

        val textCenterX = cardRect.left + padX + (leftW / 2f)
        val textCenterY = (topPillRect.top + bottomPillRect.bottom) / 2f
        val fmTime = timePaint.fontMetrics
        val timeY = textCenterY - ((fmTime.descent + fmTime.ascent) / 2f)

        canvas.save()
        canvas.rotate(-90f, textCenterX, textCenterY)
        canvas.drawText(timeStr, textCenterX, timeY, timePaint)
        canvas.restore()

        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        pillPaint.color = accentColorInt
        canvas.drawRoundRect(topPillRect, pillRadius, pillRadius, pillPaint)

        var dateTextSize = (pillH * 0.48f).coerceIn(12f * scaleFactor, 22f * scaleFactor)
        val dateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = activeTextColor
            textSize = dateTextSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }

        if (dateTextPaint.measureText(dateNumStr) > topPillRect.width() * 0.80f) {
            dateTextSize *= ((topPillRect.width() * 0.80f) / dateTextPaint.measureText(dateNumStr))
            dateTextPaint.textSize = dateTextSize
        }

        val fmDate = dateTextPaint.fontMetrics
        val topTextY = topPillRect.centerY() - ((fmDate.descent + fmDate.ascent) / 2f)
        canvas.drawText(dateNumStr, topPillRect.centerX(), topTextY, dateTextPaint)

        pillPaint.color = pillBgColor
        canvas.drawRoundRect(bottomPillRect, pillRadius, pillRadius, pillPaint)

        var dayTextSize = (pillH * 0.44f).coerceIn(11f * scaleFactor, 20f * scaleFactor)
        val dayTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = dayTextSize
            typeface = getSlateFont(context, weight = 700)
            letterSpacing = 0.05f
            textAlign = Paint.Align.CENTER
        }

        if (dayTextPaint.measureText(dayStr) > bottomPillRect.width() * 0.80f) {
            dayTextSize *= ((bottomPillRect.width() * 0.80f) / dayTextPaint.measureText(dayStr))
            dayTextPaint.textSize = dayTextSize
        }

        val fmDay = dayTextPaint.fontMetrics
        val bottomTextY = bottomPillRect.centerY() - ((fmDay.descent + fmDay.ascent) / 2f)
        canvas.drawText(dayStr, bottomPillRect.centerX(), bottomTextY, dayTextPaint)
    } else {
        val padX = cardRect.width() * 0.08f
        val padY = cardRect.height() * 0.08f

        val timeAreaH = cardRect.height() * 0.35f
        val formattedTimeStr = if (timeStr.length == 4) "${timeStr.substring(0, 2)}:${timeStr.substring(2)}" else timeStr

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = timeAreaH * 0.70f
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.04f
        }

        val maxTimeW = cardRect.width() - (padX * 2f)
        if (timePaint.measureText(formattedTimeStr) > maxTimeW) {
            timePaint.textSize = (timeAreaH * 0.70f) * (maxTimeW / timePaint.measureText(formattedTimeStr))
        }

        val fmTime = timePaint.fontMetrics
        val timeCenterY = cardRect.top + padY + (timeAreaH / 2f)
        val timeY = timeCenterY - ((fmTime.descent + fmTime.ascent) / 2f)
        canvas.drawText(formattedTimeStr, cardRect.centerX(), timeY, timePaint)

        val pillsAreaTop = cardRect.top + padY + timeAreaH + (cardRect.height() * 0.02f)
        val pillsAreaH = cardRect.bottom - padY - pillsAreaTop

        val pillGap = pillsAreaH * 0.08f
        val pillH = (pillsAreaH - pillGap) / 2f
        val pillW = cardRect.width() - (padX * 2f)
        val pillRadius = (16f * scaleFactor).coerceAtMost(pillH / 2f)

        val topPillRect = RectF(cardRect.left + padX, pillsAreaTop, cardRect.right - padX, pillsAreaTop + pillH)
        val bottomPillRect = RectF(cardRect.left + padX, topPillRect.bottom + pillGap, cardRect.right - padX, topPillRect.bottom + pillGap + pillH)

        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        pillPaint.color = accentColorInt
        canvas.drawRoundRect(topPillRect, pillRadius, pillRadius, pillPaint)

        var dateTextSize = (pillH * 0.48f).coerceIn(12f * scaleFactor, 22f * scaleFactor)
        val dateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = activeTextColor
            textSize = dateTextSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }

        val fmDate = dateTextPaint.fontMetrics
        val topTextY = topPillRect.centerY() - ((fmDate.descent + fmDate.ascent) / 2f)
        canvas.drawText(dateNumStr, topPillRect.centerX(), topTextY, dateTextPaint)

        pillPaint.color = pillBgColor
        canvas.drawRoundRect(bottomPillRect, pillRadius, pillRadius, pillPaint)

        var dayTextSize = (pillH * 0.44f).coerceIn(11f * scaleFactor, 20f * scaleFactor)
        val dayTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = dayTextSize
            typeface = getSlateFont(context, weight = 700)
            letterSpacing = 0.05f
            textAlign = Paint.Align.CENTER
        }

        val fmDay = dayTextPaint.fontMetrics
        val bottomTextY = bottomPillRect.centerY() - ((fmDay.descent + fmDay.ascent) / 2f)
        canvas.drawText(dayStr, bottomPillRect.centerX(), bottomTextY, dayTextPaint)
    }

    return bitmap
}

fun generateVerticalTimePillCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateVerticalTimePillCalendarBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 20. TIMELINE PROGRESS CALENDAR (4x2)
fun generateTimelineProgressCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    val margin = scaleFactor * 1.5f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val targetRatio = 2.0f
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE

    val padX = cardRect.width() * 0.08f
    val padY = cardRect.height() * 0.12f
    val usableW = cardRect.width() - (padX * 2f)

    val cal = java.util.Calendar.getInstance().apply {
        val yearInt = state.year.toIntOrNull() ?: get(java.util.Calendar.YEAR)
        try {
            val parsedDate = java.text.SimpleDateFormat("MMM", java.util.Locale.ENGLISH).parse(state.monthShort)
            if (parsedDate != null) {
                val tempCal = java.util.Calendar.getInstance().apply { time = parsedDate }
                set(java.util.Calendar.MONTH, tempCal.get(java.util.Calendar.MONTH))
            }
        } catch (_: Exception) {}
        set(java.util.Calendar.YEAR, yearInt)
    }

    val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val currentDayNum = state.dayOfMonth.toIntOrNull()?.coerceIn(1, daysInMonth) ?: cal.get(java.util.Calendar.DAY_OF_MONTH)

    val fullMonthTitle = state.monthShort.uppercase()

    var monthTitleSize = (cardRect.height() * 0.15f).coerceIn(11f * scaleFactor, 18f * scaleFactor)
    val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = monthTitleSize
        typeface = getSlateFont(context, weight = 700)
        letterSpacing = 0.06f
        textAlign = Paint.Align.LEFT
    }

    if (monthPaint.measureText(fullMonthTitle) > usableW) {
        monthTitleSize *= (usableW / monthPaint.measureText(fullMonthTitle))
        monthPaint.textSize = monthTitleSize
    }

    val monthY = cardRect.top + padY - monthPaint.fontMetrics.ascent
    val startX = cardRect.left + padX
    val endX = cardRect.right - padX

    canvas.drawText(fullMonthTitle, startX, monthY, monthPaint)

    val timelineY = cardRect.centerY() + (cardRect.height() * 0.06f)
    val axisStrokeW = 1.8f * scaleFactor

    val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = axisStrokeW
    }

    canvas.drawLine(startX, timelineY, endX, timelineY, axisPaint)

    val standardTickH = 6f * scaleFactor
    val tickCount = 6
    for (i in 0 until tickCount) {
        val fraction = i / (tickCount - 1).toFloat()
        val tickX = startX + (fraction * usableW)
        canvas.drawLine(tickX, timelineY - standardTickH, tickX, timelineY, axisPaint)
    }

    val dayProgressRatio = if (daysInMonth > 1) (currentDayNum - 1) / (daysInMonth - 1).toFloat() else 0f
    val currentDayX = startX + (dayProgressRatio * usableW)

    val accentTickH = 14f * scaleFactor
    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = 2.4f * scaleFactor
        strokeCap = Paint.Cap.ROUND
    }

    canvas.drawLine(currentDayX, timelineY - accentTickH, currentDayX, timelineY, accentPaint)

    val currentDayStr = currentDayNum.toString()
    var dateNumSize = (cardRect.height() * 0.16f).coerceIn(11f * scaleFactor, 18f * scaleFactor)

    val dateNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = dateNumSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val halfNumW = dateNumPaint.measureText(currentDayStr) / 2f
    val clampedDayX = currentDayX.coerceIn(startX + halfNumW, endX - halfNumW)
    val dateNumY = timelineY - accentTickH - (3f * scaleFactor)

    canvas.drawText(currentDayStr, clampedDayX, dateNumY, dateNumPaint)

    var footerLabelSize = (cardRect.height() * 0.12f).coerceIn(9f * scaleFactor, 14f * scaleFactor)
    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = footerLabelSize
        typeface = getSlateFont(context, weight = 700)
    }

    val fmFooter = footerPaint.fontMetrics
    val footerY = timelineY + (10f * scaleFactor) - fmFooter.ascent

    footerPaint.textAlign = Paint.Align.LEFT
    canvas.drawText("1", startX, footerY, footerPaint)

    val endLabelStr = daysInMonth.toString()
    footerPaint.textAlign = Paint.Align.RIGHT
    canvas.drawText(endLabelStr, endX, footerY, footerPaint)

    return bitmap
}

fun generateTimelineProgressCalendarBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateTimelineProgressCalendarBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 21. PAGE FLIP DATE (2x2 Square)
fun generatePageFlipDateBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val rect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardSizeRef = minOf(rect.width(), rect.height())
    val cardRadius = getStandardCornerRadius(scaleFactor)
    val foldSize = cardSizeRef * 0.28f

    val mainCardPath = Path().apply {
        moveTo(rect.left + cardRadius, rect.top)
        lineTo(rect.right - cardRadius, rect.top)
        quadTo(rect.right, rect.top, rect.right, rect.top + cardRadius)
        lineTo(rect.right, rect.bottom - foldSize)
        lineTo(rect.right - foldSize, rect.bottom)
        lineTo(rect.left + cardRadius, rect.bottom)
        quadTo(rect.left, rect.bottom, rect.left, rect.bottom - cardRadius)
        lineTo(rect.left, rect.top + cardRadius)
        quadTo(rect.left, rect.top, rect.left + cardRadius, rect.top)
        close()
    }

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawPath(mainCardPath, bgPaint)

    val foldFlapPath = Path().apply {
        moveTo(rect.right - foldSize, rect.bottom)
        lineTo(rect.right - foldSize, rect.bottom - foldSize)
        lineTo(rect.right, rect.bottom - foldSize)
        close()
    }

    val flapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawPath(foldFlapPath, flapPaint)

    val creaseShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#26000000")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * scaleFactor
    }
    canvas.drawLine(rect.right - foldSize, rect.bottom, rect.right, rect.bottom - foldSize, creaseShadowPaint)

    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE

    val weekdayText = state.dayOfWeekShort.uppercase()
    val dateText = state.dayOfMonth

    var baseWeekdaySize = cardSizeRef * 0.11f
    var baseDateSize = cardSizeRef * 0.40f

    val weekdayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = baseWeekdaySize
        typeface = getSlateFont(context, weight = 500)
        textAlign = Paint.Align.LEFT
        letterSpacing = 0.12f
    }

    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = baseDateSize
        typeface = getSlateFont(context, weight = 500)
        textAlign = Paint.Align.LEFT
    }

    val maxContentWidth = cardSizeRef * 0.65f
    if (weekdayPaint.measureText(weekdayText) > maxContentWidth) {
        baseWeekdaySize *= (maxContentWidth / weekdayPaint.measureText(weekdayText))
        weekdayPaint.textSize = baseWeekdaySize
    }

    if (datePaint.measureText(dateText) > maxContentWidth) {
        baseDateSize *= (maxContentWidth / datePaint.measureText(dateText))
        datePaint.textSize = baseDateSize
    }

    val weekdayBounds = Rect()
    weekdayPaint.getTextBounds(weekdayText, 0, weekdayText.length, weekdayBounds)

    val dateBounds = Rect()
    datePaint.getTextBounds(dateText, 0, dateText.length, dateBounds)

    val leftMargin = rect.left + (cardSizeRef * 0.12f)
    val topMargin = rect.top + (cardSizeRef * 0.14f)

    val weekdayY = topMargin + weekdayBounds.height()
    val gapBetween = cardSizeRef * 0.07f
    val dateY = weekdayY + gapBetween + dateBounds.height()

    canvas.drawText(weekdayText, leftMargin, weekdayY, weekdayPaint)
    canvas.drawText(dateText, leftMargin, dateY, datePaint)

    return bitmap
}

fun generatePageFlipDateBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generatePageFlipDateBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 22. VERTICAL DATE WHEEL (2x2 Square)
fun generateVerticalDateWheelBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val rect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardRadius, cardRadius, bgPaint)

    val cardSizeRef = minOf(rect.width(), rect.height())
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val fadedText = if (isLight) Color.parseColor("#A0A0A5") else Color.parseColor("#4DFFFFFF")

    val cal = java.util.Calendar.getInstance()
    val yesterdayCal = (cal.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_MONTH, -1) }
    val tomorrowCal = (cal.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_MONTH, 1) }

    val prevDayText = yesterdayCal.get(java.util.Calendar.DAY_OF_MONTH).toString()
    val todayText = state.dayOfMonth
    val weekdayText = state.dayOfWeekShort.uppercase()
    val nextDayText = tomorrowCal.get(java.util.Calendar.DAY_OF_MONTH).toString()

    var centerTextSize = cardSizeRef * 0.20f
    val centerText = "$todayText $weekdayText"

    val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = centerTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val fadedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fadedText
        textSize = cardSizeRef * 0.15f
        typeface = getSlateFont(context, weight = 500)
        textAlign = Paint.Align.CENTER
    }

    val maxAllowedW = rect.width() * 0.64f
    if (centerPaint.measureText(centerText) > maxAllowedW) {
        centerTextSize *= (maxAllowedW / centerPaint.measureText(centerText))
        centerPaint.textSize = centerTextSize
    }

    val centerBounds = Rect()
    centerPaint.getTextBounds(centerText, 0, centerText.length, centerBounds)

    val nextBounds = Rect()
    fadedPaint.getTextBounds(nextDayText, 0, nextDayText.length, nextBounds)

    val cx = rect.centerX()
    val cy = rect.centerY() + (centerBounds.height() / 2f) - centerBounds.bottom

    val topY = rect.centerY() - (cardSizeRef * 0.22f)
    canvas.drawText(prevDayText, cx, topY, fadedPaint)
    canvas.drawText(centerText, cx, cy, centerPaint)

    val bottomY = rect.centerY() + (cardSizeRef * 0.24f) + nextBounds.height()
    canvas.drawText(nextDayText, cx, bottomY, fadedPaint)

    val centerTextWidth = centerPaint.measureText(centerText)
    val arrowGap = cardSizeRef * 0.055f
    val chevronWidth = cardSizeRef * 0.022f
    val chevronHeight = cardSizeRef * 0.038f
    val midY = rect.centerY()

    val chevronPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * scaleFactor
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    val leftChevronX = cx - (centerTextWidth / 2f) - arrowGap - chevronWidth
    val leftChevronPath = Path().apply {
        moveTo(leftChevronX + chevronWidth, midY - chevronHeight)
        lineTo(leftChevronX, midY)
        lineTo(leftChevronX + chevronWidth, midY + chevronHeight)
    }
    canvas.drawPath(leftChevronPath, chevronPaint)

    val rightChevronX = cx + (centerTextWidth / 2f) + arrowGap
    val rightChevronPath = Path().apply {
        moveTo(rightChevronX, midY - chevronHeight)
        lineTo(rightChevronX + chevronWidth, midY)
        lineTo(rightChevronX, midY + chevronHeight)
    }
    canvas.drawPath(rightChevronPath, chevronPaint)

    return bitmap
}

fun generateVerticalDateWheelBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateVerticalDateWheelBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 23. MONTH PROGRESS CAPSULE (2x2 Square)
fun generateMonthProgressCapsuleBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE

    val margin = scaleFactor * 1.5f
    val rect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardRadius, cardRadius, bgPaint)

    val cal = java.util.Calendar.getInstance()
    val currentDayNum = state.dayOfMonth.toIntOrNull() ?: cal.get(java.util.Calendar.DAY_OF_MONTH)
    val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val progress = (currentDayNum.toFloat() / daysInMonth.toFloat()).coerceIn(0f, 1f)
    val percentInt = (progress * 100).toInt()

    val fullWeekdayName = cal.getDisplayName(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.LONG, java.util.Locale.ENGLISH) ?: state.dayOfWeekShort
    val cardSizeRef = minOf(rect.width(), rect.height())

    val pillWidth = cardSizeRef * 0.10f
    val pillHeight = cardSizeRef * 0.58f
    val pillLeft = rect.left + (cardSizeRef * 0.12f)
    val pillTop = rect.centerY() - (pillHeight / 2f)

    val pillRect = RectF(pillLeft, pillTop, pillLeft + pillWidth, pillTop + pillHeight)
    val pillRadius = pillWidth / 2f

    val pillPath = Path().apply { addRoundRect(pillRect, pillRadius, pillRadius, Path.Direction.CW) }

    canvas.save()
    canvas.clipPath(pillPath)

    val fillHeight = pillHeight * progress
    val fillRect = RectF(pillRect.left, pillRect.bottom - fillHeight, pillRect.right, pillRect.bottom)
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawRect(fillRect, fillPaint)
    canvas.restore()

    val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = 2.0f * scaleFactor
    }
    canvas.drawPath(pillPath, outlinePaint)

    val textLeftMargin = pillRect.right + (cardSizeRef * 0.08f)

    val dateText = state.dayOfMonth
    val weekdayText = fullWeekdayName
    val subtext = "$percentInt% through ${state.monthShort}"

    var dateTextSize = cardSizeRef * 0.24f
    var weekdayTextSize = cardSizeRef * 0.08f
    var subtextSize = cardSizeRef * 0.07f

    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = dateTextSize
        typeface = getSlateFont(context, weight = 500)
        textAlign = Paint.Align.LEFT
    }

    val weekdayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = weekdayTextSize
        typeface = getSlateFont(context, weight = 400)
        textAlign = Paint.Align.LEFT
    }

    val subtextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = subtextSize
        typeface = getSlateFont(context, weight = 500)
        textAlign = Paint.Align.LEFT
    }

    val maxTextW = rect.right - textLeftMargin - (cardSizeRef * 0.10f)
    if (datePaint.measureText(dateText) > maxTextW) {
        dateTextSize *= (maxTextW / datePaint.measureText(dateText))
        datePaint.textSize = dateTextSize
    }
    if (weekdayPaint.measureText(weekdayText) > maxTextW) {
        weekdayTextSize *= (maxTextW / weekdayPaint.measureText(weekdayText))
        weekdayPaint.textSize = weekdayTextSize
    }
    if (subtextPaint.measureText(subtext) > maxTextW) {
        subtextSize *= (maxTextW / subtextPaint.measureText(subtext))
        subtextPaint.textSize = subtextSize
    }

    val dateBounds = Rect()
    datePaint.getTextBounds(dateText, 0, dateText.length, dateBounds)

    val weekdayBounds = Rect()
    weekdayPaint.getTextBounds(weekdayText, 0, weekdayText.length, weekdayBounds)

    val subtextBounds = Rect()
    subtextPaint.getTextBounds(subtext, 0, subtext.length, subtextBounds)

    val gap1 = cardSizeRef * 0.035f
    val gap2 = cardSizeRef * 0.03f

    val totalTextH = dateBounds.height() + gap1 + weekdayBounds.height() + gap2 + subtextBounds.height()
    val textTopY = rect.centerY() - (totalTextH / 2f)

    val dateY = textTopY + dateBounds.height()
    val weekdayY = dateY + gap1 + weekdayBounds.height()
    val subtextY = weekdayY + gap2 + subtextBounds.height()

    canvas.drawText(dateText, textLeftMargin, dateY, datePaint)
    canvas.drawText(weekdayText, textLeftMargin, weekdayY, weekdayPaint)
    canvas.drawText(subtext, textLeftMargin, subtextY, subtextPaint)

    return bitmap
}

fun generateMonthProgressCapsuleBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateMonthProgressCapsuleBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 24. TIMELINE PILLARS DATE (2x2 Square)
fun generateTimelinePillarsBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE

    val margin = scaleFactor * 1.5f
    val rect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardRadius, cardRadius, bgPaint)

    val aspectRatio = rect.width() / rect.height().coerceAtLeast(1f)
    val numPillars = when {
        aspectRatio < 0.82f -> 3
        aspectRatio > 1.35f -> 7
        else -> 5
    }
    val activeIndex = numPillars / 2

    val r = Color.red(accentColorInt)
    val g = Color.green(accentColorInt)
    val b = Color.blue(accentColorInt)
    val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    val activeTextColor = if (luminance > 0.65) Color.parseColor("#161618") else Color.WHITE

    val heightsRatio = when (numPillars) {
        3 -> floatArrayOf(0.65f, 1.0f, 0.65f)
        7 -> floatArrayOf(0.38f, 0.52f, 0.68f, 1.0f, 0.68f, 0.52f, 0.38f)
        else -> floatArrayOf(0.48f, 0.68f, 1.0f, 0.68f, 0.48f)
    }

    val usableWidth = rect.width() * 0.84f
    val gap = rect.width() * 0.025f
    val activeWidthScale = 1.25f

    val normalPillWidth = (usableWidth - ((numPillars - 1) * gap)) / (numPillars - 1 + activeWidthScale)
    val activePillWidth = normalPillWidth * activeWidthScale

    val startX = rect.centerX() - (usableWidth / 2f)
    val pillBottom = rect.bottom - (rect.height() * 0.12f)
    val maxPillHeight = rect.height() * 0.70f

    val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * scaleFactor
    }

    val activeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }

    val dayTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = normalPillWidth * 0.5f
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    var activeWeekdaySize = activePillWidth * 0.40f
    var activeDateSize = activePillWidth * 0.54f

    val activeWeekdayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = activeTextColor
        textSize = activeWeekdaySize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val activeDatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = activeTextColor
        textSize = activeDateSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val maxPillTextW = activePillWidth * 0.82f
    val weekdayStr = state.dayOfWeekShort.uppercase()
    val dateStr = state.dayOfMonth

    if (activeWeekdayPaint.measureText(weekdayStr) > maxPillTextW) {
        activeWeekdaySize *= (maxPillTextW / activeWeekdayPaint.measureText(weekdayStr))
        activeWeekdayPaint.textSize = activeWeekdaySize
    }

    if (activeDatePaint.measureText(dateStr) > maxPillTextW) {
        activeDateSize *= (maxPillTextW / activeDatePaint.measureText(dateStr))
        activeDatePaint.textSize = activeDateSize
    }

    val cal = java.util.Calendar.getInstance()
    var currentX = startX

    for (i in 0 until numPillars) {
        val offset = i - activeIndex
        val dayCal = (cal.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_MONTH, offset) }

        val currentPillWidth = if (i == activeIndex) activePillWidth else normalPillWidth
        val pillH = maxPillHeight * heightsRatio[i]
        val pillTop = pillBottom - pillH
        val pillRect = RectF(currentX, pillTop, currentX + currentPillWidth, pillBottom)
        val pillRadius = currentPillWidth / 2f
        val pillCenterX = pillRect.centerX()

        if (i == activeIndex) {
            canvas.drawRoundRect(pillRect, pillRadius, pillRadius, activeFillPaint)

            val weekdayBounds = Rect()
            activeWeekdayPaint.getTextBounds(weekdayStr, 0, weekdayStr.length, weekdayBounds)

            val dateBounds = Rect()
            activeDatePaint.getTextBounds(dateStr, 0, dateStr.length, dateBounds)

            val textGap = activePillWidth * 0.15f
            val totalBlockHeight = weekdayBounds.height() + textGap + dateBounds.height()
            val blockTopY = pillRect.centerY() - (totalBlockHeight / 2f)

            val weekdayY = blockTopY + weekdayBounds.height() - weekdayBounds.bottom
            val dateY = weekdayY + textGap + dateBounds.height() - dateBounds.bottom

            canvas.drawText(weekdayStr, pillCenterX, weekdayY, activeWeekdayPaint)
            canvas.drawText(dateStr, pillCenterX, dateY, activeDatePaint)
        } else {
            canvas.drawRoundRect(pillRect, pillRadius, pillRadius, outlinePaint)

            val dayLetter = dayCal.getDisplayName(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SHORT, java.util.Locale.ENGLISH)?.take(1)?.uppercase() ?: ""

            val letterBounds = Rect()
            dayTextPaint.getTextBounds(dayLetter, 0, dayLetter.length, letterBounds)
            val textY = pillRect.centerY() + (letterBounds.height() / 2f) - letterBounds.bottom

            canvas.drawText(dayLetter, pillCenterX, textY, dayTextPaint)
        }

        currentX += currentPillWidth + gap
    }

    return bitmap
}

fun generateTimelinePillarsBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateTimelinePillarsBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 25. TILTED BADGE FLIP DATE (2x2 Square)
fun generateTiltedBadgeFlipDateBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val rect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardRadius, cardRadius, bgPaint)

    val cardSizeRef = minOf(rect.width(), rect.height())

    val cardGap = cardSizeRef * 0.025f
    val groupGap = cardSizeRef * 0.05f

    val digitCardW = cardSizeRef * 0.22f
    val digitCardH = cardSizeRef * 0.36f
    val digitCardRadius = 8f * scaleFactor

    val badgeW = cardSizeRef * 0.36f
    val badgeH = cardSizeRef * 0.22f
    val badgeRadius = 8f * scaleFactor

    val totalGroupW = (digitCardW * 2f) + cardGap + groupGap + badgeW
    val startX = rect.centerX() - (totalGroupW / 2f)
    val centerY = rect.centerY()

    val flipCardBg = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val flipDigitColor = if (isLight) Color.WHITE else Color.BLACK
    val splitLineColor = if (isLight) Color.parseColor("#3A3A3C") else Color.parseColor("#1C1C1E")

    val r = Color.red(accentColorInt)
    val g = Color.green(accentColorInt)
    val b = Color.blue(accentColorInt)
    val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    val badgeTextColor = if (luminance > 0.65) Color.parseColor("#161618") else Color.WHITE

    val rawDate = state.dayOfMonth.padStart(2, '0')
    val digit1 = rawDate.getOrNull(0)?.toString() ?: "0"
    val digit2 = rawDate.getOrNull(1)?.toString() ?: "1"
    val digits = arrayOf(digit1, digit2)

    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = flipCardBg
        style = Paint.Style.FILL
    }

    val splitLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = splitLineColor
        style = Paint.Style.STROKE
        strokeWidth = 2.0f * scaleFactor
    }

    val digitTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = flipDigitColor
        textSize = digitCardH * 0.68f
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val digitCardTop = centerY - (digitCardH / 2f)

    for (i in 0..1) {
        val cardLeft = startX + (i * (digitCardW + cardGap))
        val cardRect = RectF(cardLeft, digitCardTop, cardLeft + digitCardW, digitCardTop + digitCardH)

        canvas.drawRoundRect(cardRect, digitCardRadius, digitCardRadius, cardPaint)

        val digitStr = digits[i]
        val digitBounds = Rect()
        digitTextPaint.getTextBounds(digitStr, 0, digitStr.length, digitBounds)
        val digitY = cardRect.centerY() + (digitBounds.height() / 2f) - digitBounds.bottom
        canvas.drawText(digitStr, cardRect.centerX(), digitY, digitTextPaint)

        canvas.drawLine(cardRect.left, cardRect.centerY(), cardRect.right, cardRect.centerY(), splitLinePaint)
    }

    val badgeLeft = startX + (digitCardW * 2f) + cardGap + groupGap
    val badgeCenterX = badgeLeft + (badgeW / 2f)
    val badgeCenterY = centerY

    val badgeRect = RectF(-badgeW / 2f, -badgeH / 2f, badgeW / 2f, badgeH / 2f)

    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }

    val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = badgeTextColor
        textSize = badgeH * 0.48f
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.08f
    }

    val weekdayStr = state.dayOfWeekShort.uppercase()
    val badgeTextBounds = Rect()
    badgeTextPaint.getTextBounds(weekdayStr, 0, weekdayStr.length, badgeTextBounds)
    val badgeTextY = (badgeTextBounds.height() / 2f) - badgeTextBounds.bottom

    canvas.save()
    canvas.translate(badgeCenterX, badgeCenterY)
    canvas.rotate(-8f)

    canvas.drawRoundRect(badgeRect, badgeRadius, badgeRadius, badgePaint)
    canvas.drawText(weekdayStr, 0f, badgeTextY, badgeTextPaint)

    canvas.restore()

    return bitmap
}

fun generateTiltedBadgeFlipDateBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateTiltedBadgeFlipDateBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 26. SOLAR LANDSCAPE DATE (2x2 Square)
fun generateSolarLandscapeDateBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE

    val margin = scaleFactor * 1.5f
    val rect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }

    val cardPath = Path().apply { addRoundRect(rect, cardRadius, cardRadius, Path.Direction.CW) }

    canvas.save()
    canvas.clipPath(cardPath)
    canvas.drawPath(cardPath, bgPaint)

    val cardSizeRef = minOf(rect.width(), rect.height())

    val opacityFactor = 1.0f
    val alphaInt = (255 * opacityFactor).toInt().coerceIn(0, 255)

    val artColorWithAlpha = Color.argb(alphaInt, Color.red(accentColorInt), Color.green(accentColorInt), Color.blue(accentColorInt))

    val bgArtFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = artColorWithAlpha
        style = Paint.Style.FILL
    }

    val bgArtStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = artColorWithAlpha
        style = Paint.Style.STROKE
        strokeWidth = 3.0f * scaleFactor
        strokeCap = Paint.Cap.ROUND
    }

    val sunCenterX = rect.right - (cardSizeRef * 0.22f)
    val sunCenterY = rect.top + (cardSizeRef * 0.22f)
    val sunRadius = cardSizeRef * 0.11f
    val rayInner = cardSizeRef * 0.14f
    val rayOuter = cardSizeRef * 0.18f

    canvas.drawCircle(sunCenterX, sunCenterY, sunRadius, bgArtFillPaint)

    for (angle in 0 until 360 step 45) {
        val rad = Math.toRadians(angle.toDouble())
        val x1 = sunCenterX + (rayInner * Math.cos(rad)).toFloat()
        val y1 = sunCenterY + (rayInner * Math.sin(rad)).toFloat()
        val x2 = sunCenterX + (rayOuter * Math.cos(rad)).toFloat()
        val y2 = sunCenterY + (rayOuter * Math.sin(rad)).toFloat()
        canvas.drawLine(x1, y1, x2, y2, bgArtStrokePaint)
    }

    val mtnLeft = rect.left + (cardSizeRef * 0.38f)
    val mtnRight = rect.right + (cardSizeRef * 0.05f)
    val mtnBottom = rect.bottom
    val mtnWidth = mtnRight - mtnLeft

    val peak1X = mtnLeft + (mtnWidth * 0.28f)
    val peak1Y = mtnBottom - (cardSizeRef * 0.22f)

    val peak2X = mtnLeft + (mtnWidth * 0.72f)
    val peak2Y = mtnBottom - (cardSizeRef * 0.34f)

    val valleyX = mtnLeft + (mtnWidth * 0.50f)
    val valleyY = mtnBottom - (cardSizeRef * 0.12f)

    val mtnPath = Path().apply {
        moveTo(mtnLeft, mtnBottom)
        lineTo(peak1X, peak1Y)
        lineTo(valleyX, valleyY)
        lineTo(peak2X, peak2Y)
        lineTo(mtnRight, mtnBottom)
        close()
    }
    canvas.drawPath(mtnPath, bgArtFillPaint)

    val cal = java.util.Calendar.getInstance()
    val fullWeekday = cal.getDisplayName(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.LONG, java.util.Locale.ENGLISH)?.uppercase() ?: state.dayOfWeekShort.uppercase()

    val monthStr = state.monthShort.uppercase()
    val dateStr = state.dayOfMonth

    var monthSize = cardSizeRef * 0.11f
    var dateSize = cardSizeRef * 0.34f
    var weekdaySize = cardSizeRef * 0.10f

    val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = monthSize
        typeface = getSlateFont(context, weight = 300)
        textAlign = Paint.Align.LEFT
        letterSpacing = 0.05f
    }

    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = dateSize
        typeface = getSlateFont(context, weight = 500)
        textAlign = Paint.Align.LEFT
    }

    val weekdayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = weekdaySize
        typeface = getSlateFont(context, weight = 300)
        textAlign = Paint.Align.LEFT
        letterSpacing = 0.05f
    }

    val maxTextW = cardSizeRef * 0.52f
    if (monthPaint.measureText(monthStr) > maxTextW) {
        monthSize *= (maxTextW / monthPaint.measureText(monthStr))
        monthPaint.textSize = monthSize
    }
    if (datePaint.measureText(dateStr) > maxTextW) {
        dateSize *= (maxTextW / datePaint.measureText(dateStr))
        datePaint.textSize = dateSize
    }
    if (weekdayPaint.measureText(fullWeekday) > maxTextW) {
        weekdaySize *= (maxTextW / weekdayPaint.measureText(fullWeekday))
        weekdayPaint.textSize = weekdaySize
    }

    val fmMonth = monthPaint.fontMetrics
    val fmDate = datePaint.fontMetrics
    val fmWeekday = weekdayPaint.fontMetrics

    val monthH = fmMonth.descent - fmMonth.ascent
    val dateH = fmDate.descent - fmDate.ascent
    val weekdayH = fmWeekday.descent - fmWeekday.ascent

    val gap1 = -cardSizeRef * 0.05f
    val gap2 = -cardSizeRef * 0.04f

    val totalTextH = monthH + gap1 + dateH + gap2 + weekdayH
    val textTopY = rect.centerY() - (totalTextH / 2f)

    val leftMargin = rect.left + (cardSizeRef * 0.12f)

    val monthY = textTopY - fmMonth.ascent
    val dateY = monthY + fmMonth.descent + gap1 - fmDate.ascent
    val weekdayY = dateY + fmDate.descent + gap2 - fmWeekday.ascent

    canvas.drawText(monthStr, leftMargin, monthY, monthPaint)
    canvas.drawText(dateStr, leftMargin, dateY, datePaint)
    canvas.drawText(fullWeekday, leftMargin, weekdayY, weekdayPaint)

    if (opacityFactor > 0.4f) {
        val r = Color.red(accentColorInt)
        val g = Color.green(accentColorInt)
        val b = Color.blue(accentColorInt)
        val mtnLuminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0

        val mtnTextColor = if (mtnLuminance > 0.60) bgColor else Color.WHITE

        val monthMtnPaint = Paint(monthPaint).apply { color = mtnTextColor }
        val dateMtnPaint = Paint(datePaint).apply { color = mtnTextColor }
        val weekdayMtnPaint = Paint(weekdayPaint).apply { color = mtnTextColor }

        canvas.save()
        canvas.clipPath(mtnPath)

        canvas.drawText(monthStr, leftMargin, monthY, monthMtnPaint)
        canvas.drawText(dateStr, leftMargin, dateY, dateMtnPaint)
        canvas.drawText(fullWeekday, leftMargin, weekdayY, weekdayMtnPaint)

        canvas.restore()
    }

    canvas.restore()

    return bitmap
}

fun generateSolarLandscapeDateBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateSolarLandscapeDateBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 27. YEAR SEGMENTED TRACK (4x2)
fun generateYearMatrixProgressBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#757575") else Color.parseColor("#9E9E9E")
    val trackBgColor = if (isLight) Color.parseColor("#15000000") else Color.parseColor("#1E1E22")

    val margin = scaleFactor * 1.5f
    val targetRatio = 2.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val cal = java.util.Calendar.getInstance()
    val currentMonthIdx = cal.get(java.util.Calendar.MONTH)
    val dayOfMonth = cal.get(java.util.Calendar.DAY_OF_MONTH)
    val maxDaysInCurrentMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val dayOfYear = cal.get(java.util.Calendar.DAY_OF_YEAR)
    val totalDaysInYear = cal.getActualMaximum(java.util.Calendar.DAY_OF_YEAR)

    val daysLeft = totalDaysInYear - dayOfYear
    val yearProgress = (dayOfYear.toFloat() / totalDaysInYear.toFloat()).coerceIn(0f, 1f)
    val percentInt = (yearProgress * 100).toInt()

    val monthLabels = arrayOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
    val numMonths = 12

    val trackBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = trackBgColor
        style = Paint.Style.FILL
    }
    val barFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.FILL
    }
    val barFillActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    val currentBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = (2.0f * scaleFactor).coerceAtMost(cardRect.width() * 0.01f)
    }

    val aspectRatio = cardRect.width() / cardRect.height()

    if (aspectRatio >= 1.2f) {
        val padX = cardRect.width() * 0.05f
        val padY = cardRect.height() * 0.08f

        val leftWidth = cardRect.width() * 0.32f
        val rightLeftX = cardRect.left + padX + leftWidth + (cardRect.width() * 0.03f)
        val rightWidth = cardRect.right - padX - rightLeftX

        val leftX = cardRect.left + padX

        val barGap = rightWidth * 0.025f
        val barWidth = (rightWidth - ((numMonths - 1) * barGap)) / numMonths
        val barRadius = barWidth / 2f

        val monthLabelSize = minOf(barWidth * 0.85f, cardRect.height() * 0.08f).coerceIn(3f * scaleFactor, 11f * scaleFactor)
        val monthLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = monthLabelSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }
        val monthLabelActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            textSize = monthLabelSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }

        val labelFm = monthLabelPaint.fontMetrics
        val monthLabelH = labelFm.descent - labelFm.ascent
        val chartTopY = cardRect.top + padY
        val chartBottomY = cardRect.bottom - padY - monthLabelH - (4f * scaleFactor)
        val chartHeight = (chartBottomY - chartTopY).coerceAtLeast(10f)

        var labelSize = (cardRect.height() * 0.065f).coerceIn(6f * scaleFactor, 10f * scaleFactor)
        var percentSize = (cardRect.height() * 0.26f).coerceIn(16f * scaleFactor, 36f * scaleFactor)
        var subtextSize = (cardRect.height() * 0.09f).coerceIn(8f * scaleFactor, 13f * scaleFactor)
        var dateStrSize = (cardRect.height() * 0.085f).coerceIn(7f * scaleFactor, 12f * scaleFactor)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = labelSize
            typeface = getSlateFont(context, weight = 500)
            textAlign = Paint.Align.LEFT
            letterSpacing = 0.08f
        }

        val percentStr = "$percentInt%"
        val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            textSize = percentSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.LEFT
        }

        val daysLeftStr = "$daysLeft days left"
        val subtextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = subtextSize
            typeface = getSlateFont(context, weight = 500)
            textAlign = Paint.Align.LEFT
        }

        val fullDateStr = "${state.monthShort.uppercase()} ${state.dayOfMonth}, ${state.dayOfWeekShort.uppercase()}"
        val dateStrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = dateStrSize
            typeface = getSlateFont(context, weight = 500)
            textAlign = Paint.Align.LEFT
            letterSpacing = 0.06f
        }

        val maxLeftTextW = leftWidth - (2f * scaleFactor)
        if (percentPaint.measureText(percentStr) > maxLeftTextW) {
            percentPaint.textSize *= (maxLeftTextW / percentPaint.measureText(percentStr))
        }
        if (subtextPaint.measureText(daysLeftStr) > maxLeftTextW) {
            subtextPaint.textSize *= (maxLeftTextW / subtextPaint.measureText(daysLeftStr))
        }
        if (dateStrPaint.measureText(fullDateStr) > maxLeftTextW) {
            dateStrPaint.textSize *= (maxLeftTextW / dateStrPaint.measureText(fullDateStr))
        }

        var fmLbl = labelPaint.fontMetrics
        var fmPct = percentPaint.fontMetrics
        var fmSub = subtextPaint.fontMetrics
        var fmDt = dateStrPaint.fontMetrics

        var hLbl = fmLbl.descent - fmLbl.ascent
        var hPct = fmPct.descent - fmPct.ascent
        var hSub = fmSub.descent - fmSub.ascent
        var hDt = fmDt.descent - fmDt.ascent

        var gap1 = cardRect.height() * 0.02f
        var gap2 = cardRect.height() * 0.02f
        var gap3 = cardRect.height() * 0.04f
        var gap4 = cardRect.height() * 0.04f
        var strokeWidthLine = (1.2f * scaleFactor).coerceAtMost(cardRect.height() * 0.015f)

        var totalLeftHeight = hLbl + gap1 + hPct + gap2 + hSub + gap3 + strokeWidthLine + gap4 + hDt
        if (totalLeftHeight > chartHeight) {
            val scale = chartHeight / totalLeftHeight
            labelPaint.textSize *= scale
            percentPaint.textSize *= scale
            subtextPaint.textSize *= scale
            dateStrPaint.textSize *= scale
            gap1 *= scale
            gap2 *= scale
            gap3 *= scale
            gap4 *= scale

            fmLbl = labelPaint.fontMetrics
            fmPct = percentPaint.fontMetrics
            fmSub = subtextPaint.fontMetrics
            fmDt = dateStrPaint.fontMetrics

            hLbl = fmLbl.descent - fmLbl.ascent
            hPct = fmPct.descent - fmPct.ascent
            hSub = fmSub.descent - fmSub.ascent
            hDt = fmDt.descent - fmDt.ascent
            totalLeftHeight = hLbl + gap1 + hPct + gap2 + hSub + gap3 + strokeWidthLine + gap4 + hDt
        }

        var currentY = chartTopY + ((chartHeight - totalLeftHeight) / 2f)

        val labelY = currentY - fmLbl.ascent
        canvas.drawText("YEAR PROGRESS", leftX, labelY, labelPaint)
        currentY += hLbl + gap1

        val percentY = currentY - fmPct.ascent
        canvas.drawText(percentStr, leftX, percentY, percentPaint)
        currentY += hPct + gap2

        val subtextY = currentY - fmSub.ascent
        canvas.drawText(daysLeftStr, leftX, subtextY, subtextPaint)
        currentY += hSub + gap3

        val dividerY = currentY + (strokeWidthLine / 2f)
        val dividerWidth = leftWidth * 0.60f
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthLine
            alpha = (255 * 0.35f).toInt()
        }
        canvas.drawLine(leftX, dividerY, leftX + dividerWidth, dividerY, dividerPaint)
        currentY += strokeWidthLine + gap4

        val dateStrY = currentY - fmDt.ascent
        canvas.drawText(fullDateStr, leftX, dateStrY, dateStrPaint)

        for (m in 0 until numMonths) {
            val barLeft = rightLeftX + (m * (barWidth + barGap))
            val barRect = RectF(barLeft, chartTopY, barLeft + barWidth, chartBottomY)
            val barCenterX = barRect.centerX()

            val monthFillRatio = when {
                m < currentMonthIdx -> 1.0f
                m == currentMonthIdx -> (dayOfMonth.toFloat() / maxDaysInCurrentMonth.toFloat()).coerceIn(0.05f, 1.0f)
                else -> 0.0f
            }

            canvas.drawRoundRect(barRect, barRadius, barRadius, trackBgPaint)

            if (monthFillRatio > 0f) {
                val fillHeight = chartHeight * monthFillRatio
                val fillRect = RectF(barRect.left, barRect.bottom - fillHeight, barRect.right, barRect.bottom)
                val pillPath = Path().apply { addRoundRect(barRect, barRadius, barRadius, Path.Direction.CW) }
                val fillPaintToUse = if (m == currentMonthIdx) barFillActivePaint else barFillPaint

                canvas.save()
                canvas.clipPath(pillPath)
                canvas.drawRect(fillRect, fillPaintToUse)
                canvas.restore()
            }

            if (m == currentMonthIdx) {
                canvas.drawRoundRect(barRect, barRadius, barRadius, currentBorderPaint)
            }

            val labelYPos = chartBottomY - labelFm.ascent + (2f * scaleFactor)
            val paintToUse = if (m == currentMonthIdx) monthLabelActivePaint else monthLabelPaint
            canvas.drawText(monthLabels[m], barCenterX, labelYPos, paintToUse)
        }
    } else {
        val padX = cardRect.width() * 0.06f
        val padY = cardRect.height() * 0.06f
        val usableW = cardRect.width() - (padX * 2f)

        val barGap = usableW * 0.025f
        val barWidth = (usableW - ((numMonths - 1) * barGap)) / numMonths
        val barRadius = barWidth / 2f

        val monthLabelSize = minOf(barWidth * 0.85f, cardRect.height() * 0.055f).coerceIn(3f * scaleFactor, 10f * scaleFactor)
        val monthLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = monthLabelSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }
        val monthLabelActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            textSize = monthLabelSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }
        val labelFm = monthLabelPaint.fontMetrics
        val monthLabelH = labelFm.descent - labelFm.ascent

        val topHeaderH = cardRect.height() * 0.28f
        val topHeaderRect = RectF(cardRect.left + padX, cardRect.top + padY, cardRect.right - padX, cardRect.top + padY + topHeaderH)

        var labelSize = (topHeaderH * 0.16f).coerceIn(6f * scaleFactor, 10f * scaleFactor)
        var percentSize = (topHeaderH * 0.42f).coerceIn(16f * scaleFactor, 32f * scaleFactor)
        var subtextSize = (topHeaderH * 0.16f).coerceIn(8f * scaleFactor, 13f * scaleFactor)
        var dateStrSize = (topHeaderH * 0.15f).coerceIn(7f * scaleFactor, 12f * scaleFactor)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = labelSize
            typeface = getSlateFont(context, weight = 500)
            textAlign = Paint.Align.LEFT
            letterSpacing = 0.08f
        }

        val percentStr = "$percentInt%"
        val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            textSize = percentSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.LEFT
        }

        val daysLeftStr = "$daysLeft days left"
        val subtextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = subtextSize
            typeface = getSlateFont(context, weight = 500)
            textAlign = Paint.Align.LEFT
        }

        val fullDateStr = "${state.monthShort.uppercase()} ${state.dayOfMonth}, ${state.dayOfWeekShort.uppercase()}"
        val dateStrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = dateStrSize
            typeface = getSlateFont(context, weight = 500)
            textAlign = Paint.Align.LEFT
            letterSpacing = 0.06f
        }

        if (labelPaint.measureText("YEAR PROGRESS") > usableW) {
            labelPaint.textSize *= (usableW / labelPaint.measureText("YEAR PROGRESS"))
        }
        if (percentPaint.measureText(percentStr) > usableW) {
            percentPaint.textSize *= (usableW / percentPaint.measureText(percentStr))
        }
        if (subtextPaint.measureText(daysLeftStr) > usableW) {
            subtextPaint.textSize *= (usableW / subtextPaint.measureText(daysLeftStr))
        }
        if (dateStrPaint.measureText(fullDateStr) > usableW) {
            dateStrPaint.textSize *= (usableW / dateStrPaint.measureText(fullDateStr))
        }

        var fmLbl = labelPaint.fontMetrics
        var fmPct = percentPaint.fontMetrics
        var fmSub = subtextPaint.fontMetrics
        var fmDt = dateStrPaint.fontMetrics

        var hLbl = fmLbl.descent - fmLbl.ascent
        var hPct = fmPct.descent - fmPct.ascent
        var hSub = fmSub.descent - fmSub.ascent
        var hDt = fmDt.descent - fmDt.ascent

        var g1 = topHeaderH * 0.02f
        var g2 = topHeaderH * 0.02f
        var g3 = topHeaderH * 0.02f

        var totalStackH = hLbl + g1 + hPct + g2 + hSub + g3 + hDt
        if (totalStackH > topHeaderH) {
            val s = topHeaderH / totalStackH
            labelPaint.textSize *= s
            percentPaint.textSize *= s
            subtextPaint.textSize *= s
            dateStrPaint.textSize *= s
            g1 *= s; g2 *= s; g3 *= s

            fmLbl = labelPaint.fontMetrics
            fmPct = percentPaint.fontMetrics
            fmSub = subtextPaint.fontMetrics
            fmDt = dateStrPaint.fontMetrics

            hLbl = fmLbl.descent - fmLbl.ascent
            hPct = fmPct.descent - fmPct.ascent
            hSub = fmSub.descent - fmSub.ascent
            hDt = fmDt.descent - fmDt.ascent
            totalStackH = hLbl + g1 + hPct + g2 + hSub + g3 + hDt
        }

        var startY = topHeaderRect.centerY() - (totalStackH / 2f)

        val y1 = startY - fmLbl.ascent
        canvas.drawText("YEAR PROGRESS", topHeaderRect.left, y1, labelPaint)
        startY += hLbl + g1

        val y2 = startY - fmPct.ascent
        canvas.drawText(percentStr, topHeaderRect.left, y2, percentPaint)
        startY += hPct + g2

        val y3 = startY - fmSub.ascent
        canvas.drawText(daysLeftStr, topHeaderRect.left, y3, subtextPaint)
        startY += hSub + g3

        val y4 = startY - fmDt.ascent
        canvas.drawText(fullDateStr, topHeaderRect.left, y4, dateStrPaint)

        val chartTopY = topHeaderRect.bottom + (cardRect.height() * 0.04f)
        val chartBottomY = cardRect.bottom - padY - monthLabelH - (4f * scaleFactor)
        val chartHeight = (chartBottomY - chartTopY).coerceAtLeast(10f)

        for (m in 0 until numMonths) {
            val barLeft = cardRect.left + padX + (m * (barWidth + barGap))
            val barRect = RectF(barLeft, chartTopY, barLeft + barWidth, chartBottomY)
            val barCenterX = barRect.centerX()

            val monthFillRatio = when {
                m < currentMonthIdx -> 1.0f
                m == currentMonthIdx -> (dayOfMonth.toFloat() / maxDaysInCurrentMonth.toFloat()).coerceIn(0.05f, 1.0f)
                else -> 0.0f
            }

            canvas.drawRoundRect(barRect, barRadius, barRadius, trackBgPaint)

            if (monthFillRatio > 0f) {
                val fillHeight = chartHeight * monthFillRatio
                val fillRect = RectF(barRect.left, barRect.bottom - fillHeight, barRect.right, barRect.bottom)
                val pillPath = Path().apply { addRoundRect(barRect, barRadius, barRadius, Path.Direction.CW) }
                val fillPaintToUse = if (m == currentMonthIdx) barFillActivePaint else barFillPaint

                canvas.save()
                canvas.clipPath(pillPath)
                canvas.drawRect(fillRect, fillPaintToUse)
                canvas.restore()
            }

            if (m == currentMonthIdx) {
                canvas.drawRoundRect(barRect, barRadius, barRadius, currentBorderPaint)
            }

            val labelYPos = chartBottomY - labelFm.ascent + (2f * scaleFactor)
            val paintToUse = if (m == currentMonthIdx) monthLabelActivePaint else monthLabelPaint
            canvas.drawText(monthLabels[m], barCenterX, labelYPos, paintToUse)
        }
    }

    return bitmap
}

fun generateYearMatrixProgressBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateYearMatrixProgressBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 28. ANALOG MONTH DASHBOARD (4x2)
fun generateAnalogCalendarHybridBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#757575") else Color.parseColor("#9E9E9E")
    val dimText = if (isLight) Color.parseColor("#D0D0D0") else Color.parseColor("#3A3A3C")

    val margin = scaleFactor * 1.5f
    val targetRatio = 2.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val cal = java.util.Calendar.getInstance()
    val currentDayNum = state.dayOfMonth.toIntOrNull() ?: cal.get(java.util.Calendar.DAY_OF_MONTH)

    val fullMonthName = when (state.monthShort.uppercase()) {
        "JAN" -> "JANUARY"
        "FEB" -> "FEBRUARY"
        "MAR" -> "MARCH"
        "APR" -> "APRIL"
        "MAY" -> "MAY"
        "JUN" -> "JUNE"
        "JUL" -> "JULY"
        "AUG" -> "AUGUST"
        "SEP" -> "SEPTEMBER"
        "OCT" -> "OCTOBER"
        "NOV" -> "NOVEMBER"
        "DEC" -> "DECEMBER"
        else -> state.monthShort.uppercase()
    }

    val timeCal = java.util.Calendar.getInstance()
    val hours = timeCal.get(java.util.Calendar.HOUR)
    val minutes = timeCal.get(java.util.Calendar.MINUTE)
    val seconds = timeCal.get(java.util.Calendar.SECOND)
    val millis = timeCal.get(java.util.Calendar.MILLISECOND)

    val secondsWithMillis = seconds + (millis / 1000f)
    val minutesWithSeconds = minutes + (secondsWithMillis / 60f)
    val hoursWithMinutes = (hours % 12) + (minutesWithSeconds / 60f)

    val hourAngle = Math.toRadians((hoursWithMinutes * 30f - 90f).toDouble())
    val minuteAngle = Math.toRadians((minutesWithSeconds * 6f - 90f).toDouble())
    val secondAngle = Math.toRadians((secondsWithMillis * 6f - 90f).toDouble())

    val dayHeaders = arrayOf("S", "M", "T", "W", "T", "F", "S")

    val monthCal = (cal.clone() as java.util.Calendar).apply { set(java.util.Calendar.DAY_OF_MONTH, 1) }
    val daysInMonth = monthCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val firstDaySunIndex = monthCal.get(java.util.Calendar.DAY_OF_WEEK) - 1

    val prevCal = (monthCal.clone() as java.util.Calendar).apply { add(java.util.Calendar.MONTH, -1) }
    val prevMaxDays = prevCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

    val totalCells = firstDaySunIndex + daysInMonth
    val dateRowsNeeded = kotlin.math.ceil(totalCells / 7.0).toInt()
    val totalGridRows = (dateRowsNeeded + 1).toFloat()

    val r = Color.red(accentColorInt)
    val g = Color.green(accentColorInt)
    val b = Color.blue(accentColorInt)
    val badgeLuminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    val activeBadgeTextColor = if (badgeLuminance > 0.65) Color.parseColor("#161618") else Color.WHITE

    val activeBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }

    val aspectRatio = cardRect.width() / cardRect.height()

    if (aspectRatio >= 1.2f) {
        val padX = cardRect.width() * 0.05f
        val padY = cardRect.height() * 0.08f

        val leftWidth = cardRect.width() * 0.38f
        val rightLeftX = cardRect.left + leftWidth + (cardRect.width() * 0.02f)
        val rightWidth = cardRect.right - rightLeftX - padX

        val clockDiameter = (cardRect.height() - (padY * 2f)).coerceAtMost(leftWidth - (padX * 0.5f))
        val clockCx = cardRect.left + padX + (leftWidth - padX) / 2f
        val clockCy = cardRect.centerY()
        val clockRadius = clockDiameter / 2f

        val clockRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.parseColor("#0F000000") else Color.parseColor("#16FFFFFF")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(clockCx, clockCy, clockRadius, clockRingPaint)

        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        for (i in 0 until 12) {
            val angleRad = Math.toRadians((i * 30f - 90f).toDouble())
            val innerR = if (i % 3 == 0) clockRadius * 0.76f else clockRadius * 0.85f
            val outerR = clockRadius * 0.90f
            val x1 = (clockCx + innerR * Math.cos(angleRad)).toFloat()
            val y1 = (clockCy + innerR * Math.sin(angleRad)).toFloat()
            val x2 = (clockCx + outerR * Math.cos(angleRad)).toFloat()
            val y2 = (clockCy + outerR * Math.sin(angleRad)).toFloat()
            tickPaint.strokeWidth = if (i % 3 == 0) 2.2f * scaleFactor else 1.2f * scaleFactor
            tickPaint.color = if (i % 3 == 0) primaryText else secondaryText
            canvas.drawLine(x1, y1, x2, y2, tickPaint)
        }

        val hourHandLength = clockRadius * 0.48f
        val hourHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText; style = Paint.Style.STROKE; strokeWidth = 3.5f * scaleFactor; strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(clockCx, clockCy, (clockCx + hourHandLength * Math.cos(hourAngle)).toFloat(), (clockCy + hourHandLength * Math.sin(hourAngle)).toFloat(), hourHandPaint)

        val minHandLength = clockRadius * 0.72f
        val minHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText; style = Paint.Style.STROKE; strokeWidth = 2.2f * scaleFactor; strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(clockCx, clockCy, (clockCx + minHandLength * Math.cos(minuteAngle)).toFloat(), (clockCy + minHandLength * Math.sin(minuteAngle)).toFloat(), minHandPaint)

        val secHandLength = clockRadius * 0.82f
        val secHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt; style = Paint.Style.STROKE; strokeWidth = 1.4f * scaleFactor; strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(clockCx, clockCy, (clockCx + secHandLength * Math.cos(secondAngle)).toFloat(), (clockCy + secHandLength * Math.sin(secondAngle)).toFloat(), secHandPaint)

        val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColorInt; style = Paint.Style.FILL }
        canvas.drawCircle(clockCx, clockCy, 3.5f * scaleFactor, capPaint)

        val headerTitle = "$fullMonthName ${cal.get(java.util.Calendar.YEAR)}"
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = 12f * scaleFactor
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.LEFT
            letterSpacing = 0.08f
        }

        val gridTopY = cardRect.top + padY + (18f * scaleFactor)
        val gridBottomY = cardRect.bottom - padY
        val gridHeight = gridBottomY - gridTopY

        canvas.drawText(headerTitle, rightLeftX, cardRect.top + padY + (11f * scaleFactor), headerPaint)

        val colWidth = rightWidth / 7f
        val rowHeight = gridHeight / totalGridRows

        val dayHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = 9.5f * scaleFactor
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }

        val dateNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f * scaleFactor
            typeface = getSlateFont(context, weight = 400)
            textAlign = Paint.Align.CENTER
        }

        val row0Y = gridTopY + (rowHeight * 0.65f)
        for (c in 0..6) {
            val cx = rightLeftX + (c * colWidth) + (colWidth / 2f)
            canvas.drawText(dayHeaders[c], cx, row0Y, dayHeaderPaint)
        }

        var cellIndex = 0

        for (i in (firstDaySunIndex - 1) downTo 0) {
            val dayNum = prevMaxDays - i
            val c = cellIndex % 7
            val rIdx = (cellIndex / 7) + 1
            val cx = rightLeftX + (c * colWidth) + (colWidth / 2f)
            val cy = gridTopY + (rIdx * rowHeight) + (rowHeight * 0.65f)

            dateNumPaint.color = dimText
            canvas.drawText(dayNum.toString(), cx, cy, dateNumPaint)
            cellIndex++
        }

        for (day in 1..daysInMonth) {
            val c = cellIndex % 7
            val rIdx = (cellIndex / 7) + 1
            if (rIdx >= totalGridRows.toInt()) break

            val cx = rightLeftX + (c * colWidth) + (colWidth / 2f)
            val cellCenterY = gridTopY + (rIdx * rowHeight) + (rowHeight / 2f)

            if (day == currentDayNum) {
                val badgeRadius = minOf(colWidth * 0.42f, rowHeight * 0.42f)
                val badgeRect = RectF(cx - badgeRadius, cellCenterY - badgeRadius, cx + badgeRadius, cellCenterY + badgeRadius)
                val badgeCorner = 6f * scaleFactor
                canvas.drawRoundRect(badgeRect, badgeCorner, badgeCorner, activeBadgePaint)

                dateNumPaint.color = activeBadgeTextColor
                dateNumPaint.typeface = getSlateFont(context, weight = 700)

                val fm = dateNumPaint.fontMetrics
                val drawY = cellCenterY - ((fm.descent + fm.ascent) / 2f)
                canvas.drawText(day.toString(), cx, drawY, dateNumPaint)
            } else {
                dateNumPaint.color = primaryText
                dateNumPaint.typeface = getSlateFont(context, weight = 400)
                val cy = gridTopY + (rIdx * rowHeight) + (rowHeight * 0.65f)
                canvas.drawText(day.toString(), cx, cy, dateNumPaint)
            }
            cellIndex++
        }

        var nextMonthDay = 1
        while (cellIndex < (totalGridRows.toInt() - 1) * 7) {
            val c = cellIndex % 7
            val rIdx = (cellIndex / 7) + 1
            val cx = rightLeftX + (c * colWidth) + (colWidth / 2f)
            val cy = gridTopY + (rIdx * rowHeight) + (rowHeight * 0.65f)

            dateNumPaint.color = dimText
            dateNumPaint.typeface = getSlateFont(context, weight = 400)
            canvas.drawText(nextMonthDay.toString(), cx, cy, dateNumPaint)
            nextMonthDay++
            cellIndex++
        }
    } else {
        val padX = cardRect.width() * 0.06f
        val padY = cardRect.height() * 0.06f

        val headerH = cardRect.height() * 0.22f
        val clockDiameter = headerH.coerceAtMost(cardRect.width() * 0.28f)
        val clockCx = cardRect.left + padX + (clockDiameter / 2f)
        val clockCy = cardRect.top + padY + (headerH / 2f)
        val clockRadius = clockDiameter / 2f

        val clockRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.parseColor("#0F000000") else Color.parseColor("#16FFFFFF")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(clockCx, clockCy, clockRadius, clockRingPaint)

        val hourHandLength = clockRadius * 0.48f
        val hourHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText; style = Paint.Style.STROKE; strokeWidth = 2.5f * scaleFactor; strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(clockCx, clockCy, (clockCx + hourHandLength * Math.cos(hourAngle)).toFloat(), (clockCy + hourHandLength * Math.sin(hourAngle)).toFloat(), hourHandPaint)

        val minHandLength = clockRadius * 0.72f
        val minHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText; style = Paint.Style.STROKE; strokeWidth = 1.8f * scaleFactor; strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(clockCx, clockCy, (clockCx + minHandLength * Math.cos(minuteAngle)).toFloat(), (clockCy + minHandLength * Math.sin(minuteAngle)).toFloat(), minHandPaint)

        val headerTitle = "$fullMonthName ${cal.get(java.util.Calendar.YEAR)}"
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = (headerH * 0.40f).coerceIn(11f * scaleFactor, 18f * scaleFactor)
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.LEFT
            letterSpacing = 0.08f
        }
        val fmM = headerPaint.fontMetrics
        val headerY = clockCy - ((fmM.descent + fmM.ascent) / 2f)
        canvas.drawText(headerTitle, clockCx + clockRadius + (10f * scaleFactor), headerY, headerPaint)

        val gridTop = cardRect.top + padY + headerH + (4f * scaleFactor)
        val gridH = cardRect.bottom - padY - gridTop
        val gridW = cardRect.width() - (padX * 2f)

        val colWidth = gridW / 7f
        val rowHeight = gridH / totalGridRows

        val dayHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = 9.5f * scaleFactor
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }

        val dateNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f * scaleFactor
            typeface = getSlateFont(context, weight = 400)
            textAlign = Paint.Align.CENTER
        }

        val row0Y = gridTop + (rowHeight * 0.65f)
        for (c in 0..6) {
            val cx = cardRect.left + padX + (c * colWidth) + (colWidth / 2f)
            canvas.drawText(dayHeaders[c], cx, row0Y, dayHeaderPaint)
        }

        var cellIndex = 0

        for (i in (firstDaySunIndex - 1) downTo 0) {
            val dayNum = prevMaxDays - i
            val c = cellIndex % 7
            val rIdx = (cellIndex / 7) + 1
            val cx = cardRect.left + padX + (c * colWidth) + (colWidth / 2f)
            val cy = gridTop + (rIdx * rowHeight) + (rowHeight * 0.65f)

            dateNumPaint.color = dimText
            canvas.drawText(dayNum.toString(), cx, cy, dateNumPaint)
            cellIndex++
        }

        for (day in 1..daysInMonth) {
            val c = cellIndex % 7
            val rIdx = (cellIndex / 7) + 1
            if (rIdx >= totalGridRows.toInt()) break

            val cx = cardRect.left + padX + (c * colWidth) + (colWidth / 2f)
            val cellCenterY = gridTop + (rIdx * rowHeight) + (rowHeight / 2f)

            if (day == currentDayNum) {
                val badgeRadius = minOf(colWidth * 0.42f, rowHeight * 0.42f)
                val badgeRect = RectF(cx - badgeRadius, cellCenterY - badgeRadius, cx + badgeRadius, cellCenterY + badgeRadius)
                val badgeCorner = 6f * scaleFactor
                canvas.drawRoundRect(badgeRect, badgeCorner, badgeCorner, activeBadgePaint)

                dateNumPaint.color = activeBadgeTextColor
                dateNumPaint.typeface = getSlateFont(context, weight = 700)

                val fm = dateNumPaint.fontMetrics
                val drawY = cellCenterY - ((fm.descent + fm.ascent) / 2f)
                canvas.drawText(day.toString(), cx, drawY, dateNumPaint)
            } else {
                dateNumPaint.color = primaryText
                dateNumPaint.typeface = getSlateFont(context, weight = 400)
                val cy = gridTop + (rIdx * rowHeight) + (rowHeight * 0.65f)
                canvas.drawText(day.toString(), cx, cy, dateNumPaint)
            }
            cellIndex++
        }

        var nextMonthDay = 1
        while (cellIndex < (totalGridRows.toInt() - 1) * 7) {
            val c = cellIndex % 7
            val rIdx = (cellIndex / 7) + 1
            val cx = cardRect.left + padX + (c * colWidth) + (colWidth / 2f)
            val cy = gridTop + (rIdx * rowHeight) + (rowHeight * 0.65f)

            dateNumPaint.color = dimText
            dateNumPaint.typeface = getSlateFont(context, weight = 400)
            canvas.drawText(nextMonthDay.toString(), cx, cy, dateNumPaint)
            nextMonthDay++
            cellIndex++
        }
    }

    return bitmap
}

fun generateAnalogCalendarHybridBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateAnalogCalendarHybridBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 29. ARCHITECTURAL ANALOG DASHBOARD (4x2)
fun generateArchitecturalAnalogReceiverBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#757575") else Color.parseColor("#9E9E9E")
    val trackBgColor = if (isLight) Color.parseColor("#12000000") else Color.parseColor("#1EFFFFFF")

    val margin = scaleFactor * 1.5f
    val targetRatio = 2.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val timeCal = java.util.Calendar.getInstance()
    val hours = timeCal.get(java.util.Calendar.HOUR)
    val minutes = timeCal.get(java.util.Calendar.MINUTE)
    val seconds = timeCal.get(java.util.Calendar.SECOND)
    val millis = timeCal.get(java.util.Calendar.MILLISECOND)

    val secondsWithMillis = seconds + (millis / 1000f)
    val minutesWithSeconds = minutes + (secondsWithMillis / 60f)
    val hoursWithMinutes = (hours % 12) + (minutesWithSeconds / 60f)

    val hourAngle = Math.toRadians((hoursWithMinutes * 30f - 90f).toDouble())
    val minuteAngle = Math.toRadians((minutesWithSeconds * 6f - 90f).toDouble())
    val secondAngle = Math.toRadians((secondsWithMillis * 6f - 90f).toDouble())

    val fullMonthName = try {
        val date = java.text.SimpleDateFormat("MMM", java.util.Locale.ENGLISH).parse(state.monthShort)
        if (date != null) {
            val tempCal = java.util.Calendar.getInstance().apply { time = date }
            java.text.SimpleDateFormat("MMMM", java.util.Locale.ENGLISH).format(tempCal.time).uppercase()
        } else state.monthShort.uppercase()
    } catch (_: Exception) {
        java.text.SimpleDateFormat("MMMM", java.util.Locale.ENGLISH).format(timeCal.time).uppercase()
    }

    val dayOfWeekFull = try {
        val date = java.text.SimpleDateFormat("EEE", java.util.Locale.ENGLISH).parse(state.dayOfWeekShort)
        if (date != null) {
            java.text.SimpleDateFormat("EEEE", java.util.Locale.ENGLISH).format(date).uppercase()
        } else state.dayOfWeekShort.uppercase()
    } catch (_: Exception) {
        java.text.SimpleDateFormat("EEEE", java.util.Locale.ENGLISH).format(timeCal.time).uppercase()
    }

    val dayNumInt = state.dayOfMonth.trim().toIntOrNull() ?: timeCal.get(java.util.Calendar.DAY_OF_MONTH)
    val dateStr = String.format(java.util.Locale.ENGLISH, "%02d", dayNumInt)
    val yearStr = timeCal.get(java.util.Calendar.YEAR).toString()
    val monthYearText = "$fullMonthName $yearStr"

    val totalSecondsInDay = 24 * 3600f
    val elapsedSecondsInDay = (timeCal.get(java.util.Calendar.HOUR_OF_DAY) * 3600) + (minutes * 60) + seconds
    val dayProgressRatio = (elapsedSecondsInDay / totalSecondsInDay).coerceIn(0f, 1f)
    val dayPercent = (dayProgressRatio * 100).toInt()

    val aspectRatio = cardRect.width() / cardRect.height()

    if (aspectRatio >= 1.2f) {
        val padY = cardRect.height() * 0.10f
        val availH = cardRect.height() - (padY * 2f)

        val clockDiameter = availH
        val leftWidth = clockDiameter
        val padX = minOf(cardRect.width() * 0.05f, availH * 0.15f)
        val gapX = availH * 0.08f

        val clockCx = cardRect.left + padX + (clockDiameter / 2f)
        val clockCy = cardRect.centerY()
        val clockRadius = (clockDiameter / 2f) - (2f * scaleFactor)

        val rightLeftX = cardRect.left + padX + leftWidth + gapX
        val rightWidth = cardRect.right - padX - rightLeftX

        val arcStrokeW = (availH * 0.035f).coerceAtLeast(2f * scaleFactor)
        val arcRect = RectF(clockCx - clockRadius, clockCy - clockRadius, clockCx + clockRadius, clockCy + clockRadius)

        val trackBgArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = trackBgColor; style = Paint.Style.STROKE; strokeWidth = arcStrokeW
        }
        canvas.drawOval(arcRect, trackBgArcPaint)

        // Skia Bug Guard: Only draw arc if sweepAngle >= 1f to prevent degenerate line projection
        val minProgressSweep = ((minutesWithSeconds / 60f) * 360f).coerceIn(0f, 360f)
        if (minProgressSweep >= 1f) {
            val activeArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColorInt; style = Paint.Style.STROKE; strokeWidth = arcStrokeW; strokeCap = Paint.Cap.ROUND
            }
            canvas.drawArc(arcRect, -90f, minProgressSweep, false, activeArcPaint)
        }

        val innerPlateRadius = clockRadius - (arcStrokeW * 1.5f)
        val platePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.parseColor("#08000000") else Color.parseColor("#12FFFFFF")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(clockCx, clockCy, innerPlateRadius, platePaint)

        val cardinalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText; textSize = innerPlateRadius * 0.36f; typeface = getSlateFont(context, weight = 700); textAlign = Paint.Align.CENTER; alpha = (255 * 0.20f).toInt()
        }
        val cardPosOffset = innerPlateRadius * 0.74f
        val cardinalFm = cardinalPaint.fontMetrics
        val textYCenter = -((cardinalFm.descent + cardinalFm.ascent) / 2f)

        canvas.drawText("12", clockCx, clockCy - cardPosOffset + textYCenter, cardinalPaint)
        canvas.drawText("3", clockCx + cardPosOffset, clockCy + textYCenter, cardinalPaint)
        canvas.drawText("6", clockCx, clockCy + cardPosOffset + textYCenter, cardinalPaint)
        canvas.drawText("9", clockCx - cardPosOffset, clockCy + textYCenter, cardinalPaint)

        val hourLength = innerPlateRadius * 0.48f
        val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText; style = Paint.Style.STROKE; strokeWidth = (availH * 0.045f).coerceAtLeast(2.5f * scaleFactor); strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(clockCx, clockCy, (clockCx + hourLength * Math.cos(hourAngle)).toFloat(), (clockCy + hourLength * Math.sin(hourAngle)).toFloat(), hourPaint)

        val minLength = innerPlateRadius * 0.78f
        val minTailLength = innerPlateRadius * 0.18f
        val minPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText; style = Paint.Style.STROKE; strokeWidth = (availH * 0.028f).coerceAtLeast(1.8f * scaleFactor); strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine((clockCx - minTailLength * Math.cos(minuteAngle)).toFloat(), (clockCy - minTailLength * Math.sin(minuteAngle)).toFloat(), (clockCx + minLength * Math.cos(minuteAngle)).toFloat(), (clockCy + minLength * Math.sin(minuteAngle)).toFloat(), minPaint)

        val secLength = innerPlateRadius * 0.82f
        val secTailLength = innerPlateRadius * 0.20f
        val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt; style = Paint.Style.STROKE; strokeWidth = (availH * 0.018f).coerceAtLeast(1.2f * scaleFactor); strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine((clockCx - secTailLength * Math.cos(secondAngle)).toFloat(), (clockCy - secTailLength * Math.sin(secondAngle)).toFloat(), (clockCx + secLength * Math.cos(secondAngle)).toFloat(), (clockCy + secLength * Math.sin(secondAngle)).toFloat(), secPaint)

        val centerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColorInt; style = Paint.Style.FILL }
        canvas.drawCircle(clockCx, clockCy, availH * 0.04f, centerRingPaint)

        val centerCutoutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor; style = Paint.Style.FILL }
        canvas.drawCircle(clockCx, clockCy, availH * 0.02f, centerCutoutPaint)

        var monthTitleSize = availH * 0.13f
        var dateNumSize = availH * 0.36f
        var dayTagSize = availH * 0.11f
        var progLabelSize = availH * 0.08f

        val monthTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt; textSize = monthTitleSize; typeface = getSlateFont(context, weight = 700); textAlign = Paint.Align.LEFT; letterSpacing = 0.08f
        }
        val dateNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText; textSize = dateNumSize; typeface = getSlateFont(context, weight = 700); textAlign = Paint.Align.LEFT
        }
        val dayTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText; textSize = dayTagSize; typeface = getSlateFont(context, weight = 500); textAlign = Paint.Align.LEFT; letterSpacing = 0.10f
        }
        val progLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText; textSize = progLabelSize; typeface = getSlateFont(context, weight = 700); textAlign = Paint.Align.LEFT; letterSpacing = 0.10f
        }
        val progValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText; textSize = progLabelSize; typeface = getSlateFont(context, weight = 700); textAlign = Paint.Align.RIGHT
        }

        if (monthTitlePaint.measureText(monthYearText) > rightWidth) {
            monthTitlePaint.textSize *= (rightWidth / monthTitlePaint.measureText(monthYearText))
        }
        if (dateNumPaint.measureText(dateStr) > rightWidth) {
            dateNumPaint.textSize *= (rightWidth / dateNumPaint.measureText(dateStr))
        }
        if (dayTagPaint.measureText(dayOfWeekFull) > rightWidth) {
            dayTagPaint.textSize *= (rightWidth / dayTagPaint.measureText(dayOfWeekFull))
        }

        var fmMonth = monthTitlePaint.fontMetrics
        var fmDate = dateNumPaint.fontMetrics
        var fmTag = dayTagPaint.fontMetrics
        var fmProg = progLabelPaint.fontMetrics

        var hMonth = fmMonth.descent - fmMonth.ascent
        var hDate = fmDate.descent - fmDate.ascent
        var hTag = fmTag.descent - fmTag.ascent
        var hProgLabel = fmProg.descent - fmProg.ascent

        var gap1 = availH * 0.02f
        var gap2 = availH * 0.02f
        var gapToProgress = availH * 0.06f
        var gapToBar = availH * 0.03f
        var barHeight = availH * 0.045f

        var totalDashHeight = hMonth + gap1 + hDate + gap2 + hTag + gapToProgress + hProgLabel + gapToBar + barHeight
        if (totalDashHeight > availH) {
            val s = availH / totalDashHeight
            monthTitlePaint.textSize *= s
            dateNumPaint.textSize *= s
            dayTagPaint.textSize *= s
            progLabelPaint.textSize *= s
            progValPaint.textSize *= s
            gap1 *= s; gap2 *= s; gapToProgress *= s; gapToBar *= s; barHeight *= s

            fmMonth = monthTitlePaint.fontMetrics
            fmDate = dateNumPaint.fontMetrics
            fmTag = dayTagPaint.fontMetrics
            fmProg = progLabelPaint.fontMetrics

            hMonth = fmMonth.descent - fmMonth.ascent
            hDate = fmDate.descent - fmDate.ascent
            hTag = fmTag.descent - fmTag.ascent
            hProgLabel = fmProg.descent - fmProg.ascent
            totalDashHeight = hMonth + gap1 + hDate + gap2 + hTag + gapToProgress + hProgLabel + gapToBar + barHeight
        }

        var dashStartY = cardRect.centerY() - (totalDashHeight / 2f)

        val monthY = dashStartY - fmMonth.ascent
        canvas.drawText(monthYearText, rightLeftX, monthY, monthTitlePaint)
        dashStartY += hMonth + gap1

        val dateY = dashStartY - fmDate.ascent
        canvas.drawText(dateStr, rightLeftX, dateY, dateNumPaint)
        dashStartY += hDate + gap2

        val tagY = dashStartY - fmTag.ascent
        canvas.drawText(dayOfWeekFull, rightLeftX, tagY, dayTagPaint)
        dashStartY += hTag + gapToProgress

        val progLabelY = dashStartY - fmProg.ascent
        canvas.drawText("DAY PROGRESS", rightLeftX, progLabelY, progLabelPaint)
        canvas.drawText("$dayPercent%", rightLeftX + rightWidth, progLabelY, progValPaint)
        dashStartY += hProgLabel + gapToBar

        val progressRect = RectF(rightLeftX, dashStartY, rightLeftX + rightWidth, dashStartY + barHeight)
        val progressRadius = barHeight / 2f

        val progressBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = trackBgColor; style = Paint.Style.FILL }
        canvas.drawRoundRect(progressRect, progressRadius, progressRadius, progressBgPaint)

        if (dayProgressRatio > 0f) {
            val fillW = (rightWidth * dayProgressRatio).coerceAtLeast(barHeight)
            val fillRect = RectF(rightLeftX, dashStartY, rightLeftX + fillW, dashStartY + barHeight)
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColorInt; style = Paint.Style.FILL }
            canvas.drawRoundRect(fillRect, progressRadius, progressRadius, fillPaint)
        }
    } else {
        val padX = cardRect.width() * 0.06f
        val padY = cardRect.height() * 0.06f
        val availH = cardRect.height() - (padY * 2f)
        val availW = cardRect.width() - (padX * 2f)

        val topHeaderH = availH * 0.35f
        val clockDiameter = topHeaderH.coerceAtMost(availW * 0.45f)
        val clockCx = cardRect.left + padX + (clockDiameter / 2f)
        val clockCy = cardRect.top + padY + (topHeaderH / 2f)
        val clockRadius = clockDiameter / 2f

        val innerPlateRadius = clockRadius - (3f * scaleFactor)
        val platePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.parseColor("#08000000") else Color.parseColor("#12FFFFFF")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(clockCx, clockCy, innerPlateRadius, platePaint)

        val hourLength = innerPlateRadius * 0.48f
        val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText; style = Paint.Style.STROKE; strokeWidth = 2.8f * scaleFactor; strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(clockCx, clockCy, (clockCx + hourLength * Math.cos(hourAngle)).toFloat(), (clockCy + hourLength * Math.sin(hourAngle)).toFloat(), hourPaint)

        val minLength = innerPlateRadius * 0.78f
        val minPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText; style = Paint.Style.STROKE; strokeWidth = 1.8f * scaleFactor; strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(clockCx, clockCy, (clockCx + minLength * Math.cos(minuteAngle)).toFloat(), (clockCy + minLength * Math.sin(minuteAngle)).toFloat(), minPaint)

        val secLength = innerPlateRadius * 0.82f
        val secTailLength = innerPlateRadius * 0.20f
        val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt; style = Paint.Style.STROKE; strokeWidth = 1.2f * scaleFactor; strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine((clockCx - secTailLength * Math.cos(secondAngle)).toFloat(), (clockCy - secTailLength * Math.sin(secondAngle)).toFloat(), (clockCx + secLength * Math.cos(secondAngle)).toFloat(), (clockCy + secLength * Math.sin(secondAngle)).toFloat(), secPaint)

        val centerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColorInt; style = Paint.Style.FILL }
        canvas.drawCircle(clockCx, clockCy, 2.8f * scaleFactor, centerRingPaint)

        val rightInfoLeft = clockCx + clockRadius + (10f * scaleFactor)
        val monthTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt; textSize = (topHeaderH * 0.32f).coerceIn(10f * scaleFactor, 16f * scaleFactor); typeface = getSlateFont(context, weight = 700); textAlign = Paint.Align.LEFT
        }
        val maxHeaderTextW = cardRect.right - padX - rightInfoLeft
        if (monthTitlePaint.measureText(monthYearText) > maxHeaderTextW) {
            monthTitlePaint.textSize *= (maxHeaderTextW / monthTitlePaint.measureText(monthYearText))
        }
        val fmM = monthTitlePaint.fontMetrics
        val monthY = clockCy - ((fmM.descent + fmM.ascent) / 2f)
        canvas.drawText(monthYearText, rightInfoLeft, monthY, monthTitlePaint)

        var dateNumSize = (availH * 0.28f).coerceIn(20f * scaleFactor, 42f * scaleFactor)
        var dayTagSize = (availH * 0.10f).coerceIn(9f * scaleFactor, 14f * scaleFactor)
        var progLabelSize = (availH * 0.08f).coerceIn(8f * scaleFactor, 11f * scaleFactor)

        val dateNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText; textSize = dateNumSize; typeface = getSlateFont(context, weight = 700); textAlign = Paint.Align.LEFT
        }
        val dayTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText; textSize = dayTagSize; typeface = getSlateFont(context, weight = 500); textAlign = Paint.Align.LEFT; letterSpacing = 0.08f
        }
        val progLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText; textSize = progLabelSize; typeface = getSlateFont(context, weight = 700); textAlign = Paint.Align.LEFT; letterSpacing = 0.08f
        }
        val progValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText; textSize = progLabelSize; typeface = getSlateFont(context, weight = 700); textAlign = Paint.Align.RIGHT
        }

        if (dateNumPaint.measureText(dateStr) > availW) {
            dateNumPaint.textSize *= (availW / dateNumPaint.measureText(dateStr))
        }
        if (dayTagPaint.measureText(dayOfWeekFull) > availW) {
            dayTagPaint.textSize *= (availW / dayTagPaint.measureText(dayOfWeekFull))
        }

        var fmDate = dateNumPaint.fontMetrics
        var fmTag = dayTagPaint.fontMetrics
        var fmProg = progLabelPaint.fontMetrics

        var hDate = fmDate.descent - fmDate.ascent
        var hTag = fmTag.descent - fmTag.ascent
        var hProg = fmProg.descent - fmProg.ascent

        var gap1 = availH * 0.015f
        var gap2 = availH * 0.04f
        var gapToBar = availH * 0.02f
        var barHeight = (availH * 0.04f).coerceIn(3.5f * scaleFactor, 6f * scaleFactor)

        val remainingH = availH - topHeaderH
        var totalBottomH = hDate + gap1 + hTag + gap2 + hProg + gapToBar + barHeight
        if (totalBottomH > remainingH) {
            val s = remainingH / totalBottomH
            dateNumPaint.textSize *= s
            dayTagPaint.textSize *= s
            progLabelPaint.textSize *= s
            progValPaint.textSize *= s
            gap1 *= s; gap2 *= s; gapToBar *= s; barHeight *= s

            fmDate = dateNumPaint.fontMetrics
            fmTag = dayTagPaint.fontMetrics
            fmProg = progLabelPaint.fontMetrics

            hDate = fmDate.descent - fmDate.ascent
            hTag = fmTag.descent - fmTag.ascent
            hProg = fmProg.descent - fmProg.ascent
            totalBottomH = hDate + gap1 + hTag + gap2 + hProg + gapToBar + barHeight
        }

        var dashStartY = cardRect.top + padY + topHeaderH + ((remainingH - totalBottomH) / 2f)

        val dateY = dashStartY - fmDate.ascent
        canvas.drawText(dateStr, cardRect.left + padX, dateY, dateNumPaint)
        dashStartY += hDate + gap1

        val tagY = dashStartY - fmTag.ascent
        canvas.drawText(dayOfWeekFull, cardRect.left + padX, tagY, dayTagPaint)
        dashStartY += hTag + gap2

        val progLabelY = dashStartY - fmProg.ascent
        canvas.drawText("DAY PROGRESS", cardRect.left + padX, progLabelY, progLabelPaint)
        canvas.drawText("$dayPercent%", cardRect.right - padX, progLabelY, progValPaint)
        dashStartY += hProg + gapToBar

        val progressRect = RectF(cardRect.left + padX, dashStartY, cardRect.right - padX, dashStartY + barHeight)
        val progressRadius = barHeight / 2f

        val progressBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = trackBgColor; style = Paint.Style.FILL }
        canvas.drawRoundRect(progressRect, progressRadius, progressRadius, progressBgPaint)

        if (dayProgressRatio > 0f) {
            val fillW = (availW * dayProgressRatio).coerceAtLeast(barHeight)
            val fillRect = RectF(cardRect.left + padX, dashStartY, cardRect.left + padX + fillW, dashStartY + barHeight)
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColorInt; style = Paint.Style.FILL }
            canvas.drawRoundRect(fillRect, progressRadius, progressRadius, fillPaint)
        }
    }

    return bitmap
}

fun generateArchitecturalAnalogDashboardBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateArchitecturalAnalogReceiverBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)

// 30. RADIAL ARC ORBITAL DASHBOARD (4x2)
fun generateRadialArcDashboardBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int = 0): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#757575") else Color.parseColor("#9E9E9E")
    val trackBgColor = if (isLight) Color.parseColor("#12000000") else Color.parseColor("#1EFFFFFF")

    val margin = scaleFactor * 1.5f
    val targetRatio = 2.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val timeCal = java.util.Calendar.getInstance()
    val hours = timeCal.get(java.util.Calendar.HOUR)
    val minutes = timeCal.get(java.util.Calendar.MINUTE)
    val seconds = timeCal.get(java.util.Calendar.SECOND)
    val millis = timeCal.get(java.util.Calendar.MILLISECOND)

    val secondsWithMillis = seconds + (millis / 1000f)
    val minutesWithSeconds = minutes + (secondsWithMillis / 60f)
    val hoursWithMinutes = (hours % 12) + (minutesWithSeconds / 60f)

    val hourSweep = ((hoursWithMinutes / 12f) * 360f).coerceIn(0f, 360f)
    val minuteSweep = ((minutesWithSeconds / 60f) * 360f).coerceIn(0f, 360f)
    val secondAngleRad = Math.toRadians((secondsWithMillis * 6f - 90f).toDouble())

    val fullMonthName = try {
        val date = java.text.SimpleDateFormat("MMM", java.util.Locale.ENGLISH).parse(state.monthShort)
        if (date != null) {
            val tempCal = java.util.Calendar.getInstance().apply { time = date }
            java.text.SimpleDateFormat("MMMM", java.util.Locale.ENGLISH).format(tempCal.time).uppercase()
        } else state.monthShort.uppercase()
    } catch (_: Exception) {
        java.text.SimpleDateFormat("MMMM", java.util.Locale.ENGLISH).format(timeCal.time).uppercase()
    }

    val dayOfWeekFull = try {
        val date = java.text.SimpleDateFormat("EEE", java.util.Locale.ENGLISH).parse(state.dayOfWeekShort)
        if (date != null) {
            java.text.SimpleDateFormat("EEEE", java.util.Locale.ENGLISH).format(date).uppercase()
        } else state.dayOfWeekShort.uppercase()
    } catch (_: Exception) {
        java.text.SimpleDateFormat("EEEE", java.util.Locale.ENGLISH).format(timeCal.time).uppercase()
    }

    val dayNumInt = state.dayOfMonth.trim().toIntOrNull() ?: timeCal.get(java.util.Calendar.DAY_OF_MONTH)
    val heroDateStr = "$dayNumInt $fullMonthName"

    val dayOfWeekIdx = timeCal.get(java.util.Calendar.DAY_OF_WEEK)
    val weekProgressRatio = (dayOfWeekIdx / 7f).coerceIn(0f, 1f)

    val dayOfMonth = timeCal.get(java.util.Calendar.DAY_OF_MONTH)
    val maxDaysInMonth = timeCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val monthProgressRatio = (dayOfMonth.toFloat() / maxDaysInMonth.toFloat()).coerceIn(0f, 1f)

    val dayOfYear = timeCal.get(java.util.Calendar.DAY_OF_YEAR)
    val totalDaysInYear = timeCal.getActualMaximum(java.util.Calendar.DAY_OF_YEAR)
    val yearProgressRatio = (dayOfYear.toFloat() / totalDaysInYear.toFloat()).coerceIn(0f, 1f)

    val hour12Str = if (hours == 0) "12" else hours.toString().padStart(2, '0')
    val minStr = minutes.toString().padStart(2, '0')
    val timeHubStr = "$hour12Str:$minStr"

    val aspectRatio = cardRect.width() / cardRect.height()

    if (aspectRatio >= 1.2f) {
        val padY = cardRect.height() * 0.09f
        val availH = cardRect.height() - (padY * 2f)

        // Adjusted clock diameter to 0.94f for balanced sizing
        val clockDiameter = availH * 0.94f
        val leftWidth = clockDiameter
        val padX = minOf(cardRect.width() * 0.05f, availH * 0.12f)
        val gapX = availH * 0.06f

        val clockCx = cardRect.left + padX + (clockDiameter / 2f)
        val clockCy = cardRect.centerY()

        val rightLeftX = cardRect.left + padX + leftWidth + gapX
        val rightWidth = cardRect.right - padX - rightLeftX

        val arcStrokeW = (availH * 0.034f).coerceAtLeast(2f * scaleFactor)
        val satOffset = arcStrokeW * 1.5f
        val outerRadius = (clockDiameter / 2f) - satOffset
        val innerRadius = outerRadius - (arcStrokeW * 1.8f)

        val arcBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = trackBgColor; style = Paint.Style.STROKE; strokeWidth = arcStrokeW
        }
        val hourArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt; style = Paint.Style.STROKE; strokeWidth = arcStrokeW; strokeCap = Paint.Cap.ROUND
        }
        val minArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText; style = Paint.Style.STROKE; strokeWidth = arcStrokeW * 0.75f; strokeCap = Paint.Cap.ROUND
        }

        val outerArcRect = RectF(clockCx - outerRadius, clockCy - outerRadius, clockCx + outerRadius, clockCy + outerRadius)
        canvas.drawOval(outerArcRect, arcBgPaint)
        if (hourSweep >= 1f) canvas.drawArc(outerArcRect, -90f, hourSweep, false, hourArcPaint)

        val innerArcRect = RectF(clockCx - innerRadius, clockCy - innerRadius, clockCx + innerRadius, clockCy + innerRadius)
        val minArcBgPaint = Paint(arcBgPaint).apply { strokeWidth = arcStrokeW * 0.75f }
        canvas.drawOval(innerArcRect, minArcBgPaint)
        if (minuteSweep >= 1f) canvas.drawArc(innerArcRect, -90f, minuteSweep, false, minArcPaint)

        // Satellite Dot (Second Hand)
        val satRadius = outerRadius + satOffset
        val satX = (clockCx + satRadius * Math.cos(secondAngleRad)).toFloat()
        val satY = (clockCy + satRadius * Math.sin(secondAngleRad)).toFloat()
        val satDotRadius = (arcStrokeW * 0.65f).coerceAtLeast(1.8f * scaleFactor)
        val satPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColorInt; style = Paint.Style.FILL }
        canvas.drawCircle(satX, satY, satDotRadius, satPaint)

        var hubTextSize = innerRadius * 0.55f
        val timeHubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText; textSize = hubTextSize; typeface = getSlateFont(context, weight = 300); textAlign = Paint.Align.CENTER
        }
        val maxHubW = innerRadius * 1.5f
        if (timeHubPaint.measureText(timeHubStr) > maxHubW) {
            timeHubPaint.textSize *= (maxHubW / timeHubPaint.measureText(timeHubStr))
        }
        val fmHub = timeHubPaint.fontMetrics
        val timeHubY = clockCy - ((fmHub.descent + fmHub.ascent) / 2f)
        canvas.drawText(timeHubStr, clockCx, timeHubY, timeHubPaint)

        // Right Content Block
        var dayTagSize = availH * 0.11f
        var dateSize = availH * 0.18f
        var progLabelSize = availH * 0.08f

        val dayTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt; textSize = dayTagSize; typeface = getSlateFont(context, weight = 700); textAlign = Paint.Align.LEFT; letterSpacing = 0.10f
        }
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText; textSize = dateSize; typeface = getSlateFont(context, weight = 700); textAlign = Paint.Align.LEFT
        }
        val progLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText; textSize = progLabelSize; typeface = getSlateFont(context, weight = 700); textAlign = Paint.Align.LEFT; letterSpacing = 0.08f
        }
        val progValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText; textSize = progLabelSize; typeface = getSlateFont(context, weight = 700); textAlign = Paint.Align.RIGHT
        }

        if (dayTagPaint.measureText(dayOfWeekFull) > rightWidth) {
            dayTagPaint.textSize *= (rightWidth / dayTagPaint.measureText(dayOfWeekFull))
        }
        if (datePaint.measureText(heroDateStr) > rightWidth) {
            datePaint.textSize *= (rightWidth / datePaint.measureText(heroDateStr))
        }

        var fmTag = dayTagPaint.fontMetrics
        var fmDate = datePaint.fontMetrics
        var fmProg = progLabelPaint.fontMetrics

        var hTag = fmTag.descent - fmTag.ascent
        var hDate = fmDate.descent - fmDate.ascent
        var hProg = fmProg.descent - fmProg.ascent

        var gapTagToDate = availH * 0.015f
        var gapDateToProgress = availH * 0.06f
        var rowGap = availH * 0.04f
        var barH = availH * 0.035f

        val progRowsHeight = 3 * (hProg + (2f * scaleFactor) + barH) + (2 * rowGap)
        var totalRightHeight = hTag + gapTagToDate + hDate + gapDateToProgress + progRowsHeight

        if (totalRightHeight > availH) {
            val s = availH / totalRightHeight
            dayTagPaint.textSize *= s
            datePaint.textSize *= s
            progLabelPaint.textSize *= s
            progValPaint.textSize *= s
            gapTagToDate *= s; gapDateToProgress *= s; rowGap *= s; barH *= s

            fmTag = dayTagPaint.fontMetrics
            fmDate = datePaint.fontMetrics
            fmProg = progLabelPaint.fontMetrics

            hTag = fmTag.descent - fmTag.ascent
            hDate = fmDate.descent - fmDate.ascent
            hProg = fmProg.descent - fmProg.ascent
            totalRightHeight = hTag + gapTagToDate + hDate + gapDateToProgress + (3 * (hProg + (2f * scaleFactor) + barH)) + (2 * rowGap)
        }

        var currentY = cardRect.centerY() - (totalRightHeight / 2f)

        val tagY = currentY - fmTag.ascent
        canvas.drawText(dayOfWeekFull, rightLeftX, tagY, dayTagPaint)
        currentY += hTag + gapTagToDate

        val dateY = currentY - fmDate.ascent
        canvas.drawText(heroDateStr, rightLeftX, dateY, datePaint)
        currentY += hDate + gapDateToProgress

        val progBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = trackBgColor; style = Paint.Style.FILL }
        val progFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColorInt; style = Paint.Style.FILL }
        val progSecondaryFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryText; style = Paint.Style.FILL }

        val progressRows = listOf(
            Triple("WEEK", (weekProgressRatio * 100).toInt(), weekProgressRatio),
            Triple("MONTH", (monthProgressRatio * 100).toInt(), monthProgressRatio),
            Triple("YEAR", (yearProgressRatio * 100).toInt(), yearProgressRatio)
        )

        for ((idx, row) in progressRows.withIndex()) {
            val (label, pct, ratio) = row
            val labelYPos = currentY - fmProg.ascent

            canvas.drawText(label, rightLeftX, labelYPos, progLabelPaint)
            canvas.drawText("$pct%", rightLeftX + rightWidth, labelYPos, progValPaint)
            currentY += hProg + (2f * scaleFactor)

            val trackRect = RectF(rightLeftX, currentY, rightLeftX + rightWidth, currentY + barH)
            val trackRadius = barH / 2f
            canvas.drawRoundRect(trackRect, trackRadius, trackRadius, progBgPaint)

            if (ratio > 0f) {
                val fillW = (rightWidth * ratio).coerceAtLeast(barH)
                val fillRect = RectF(rightLeftX, currentY, rightLeftX + fillW, currentY + barH)
                val fillPaintToUse = if (idx == 0) progFillPaint else progSecondaryFillPaint
                canvas.drawRoundRect(fillRect, trackRadius, trackRadius, fillPaintToUse)
            }

            currentY += barH + rowGap
        }
    } else {
        // Tall/Square Mode (< 1.2f)
        val padX = cardRect.width() * 0.06f
        val padY = cardRect.height() * 0.06f
        val availH = cardRect.height() - (padY * 2f)
        val availW = cardRect.width() - (padX * 2f)

        val topHeaderH = availH * 0.35f
        val clockDiameter = topHeaderH.coerceAtMost(availW * 0.45f)
        val clockCx = cardRect.left + padX + (clockDiameter / 2f)
        val clockCy = cardRect.top + padY + (topHeaderH / 2f)

        val arcStrokeW = (clockDiameter * 0.045f).coerceAtLeast(2f * scaleFactor)
        val satOffset = arcStrokeW * 1.5f
        val outerRadius = (clockDiameter / 2f) - satOffset
        val innerRadius = outerRadius - (arcStrokeW * 1.8f)

        val arcBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = trackBgColor; style = Paint.Style.STROKE; strokeWidth = arcStrokeW
        }
        val hourArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt; style = Paint.Style.STROKE; strokeWidth = arcStrokeW; strokeCap = Paint.Cap.ROUND
        }
        val minArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText; style = Paint.Style.STROKE; strokeWidth = arcStrokeW * 0.75f; strokeCap = Paint.Cap.ROUND
        }

        val outerArcRect = RectF(clockCx - outerRadius, clockCy - outerRadius, clockCx + outerRadius, clockCy + outerRadius)
        canvas.drawOval(outerArcRect, arcBgPaint)
        if (hourSweep >= 1f) canvas.drawArc(outerArcRect, -90f, hourSweep, false, hourArcPaint)

        val innerArcRect = RectF(clockCx - innerRadius, clockCy - innerRadius, clockCx + innerRadius, clockCy + innerRadius)
        val minArcBgPaint = Paint(arcBgPaint).apply { strokeWidth = arcStrokeW * 0.75f }
        canvas.drawOval(innerArcRect, minArcBgPaint)
        if (minuteSweep >= 1f) canvas.drawArc(innerArcRect, -90f, minuteSweep, false, minArcPaint)

        // Satellite Dot (Second Hand)
        val satRadius = outerRadius + satOffset
        val satX = (clockCx + satRadius * Math.cos(secondAngleRad)).toFloat()
        val satY = (clockCy + satRadius * Math.sin(secondAngleRad)).toFloat()
        val satDotRadius = (arcStrokeW * 0.65f).coerceAtLeast(1.8f * scaleFactor)
        val satPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColorInt; style = Paint.Style.FILL }
        canvas.drawCircle(satX, satY, satDotRadius, satPaint)

        var hubTextSize = innerRadius * 0.55f
        val timeHubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText; textSize = hubTextSize; typeface = getSlateFont(context, weight = 300); textAlign = Paint.Align.CENTER
        }
        val maxHubW = innerRadius * 1.5f
        if (timeHubPaint.measureText(timeHubStr) > maxHubW) {
            timeHubPaint.textSize *= (maxHubW / timeHubPaint.measureText(timeHubStr))
        }
        val fmHub = timeHubPaint.fontMetrics
        val timeHubY = clockCy - ((fmHub.descent + fmHub.ascent) / 2f)
        canvas.drawText(timeHubStr, clockCx, timeHubY, timeHubPaint)

        val rightInfoLeft = clockCx + outerRadius + (10f * scaleFactor)
        val maxHeaderTextW = (cardRect.right - padX - rightInfoLeft).coerceAtLeast(10f * scaleFactor)

        val dayTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt; textSize = (topHeaderH * 0.28f).coerceIn(9f * scaleFactor, 14f * scaleFactor); typeface = getSlateFont(context, weight = 700); textAlign = Paint.Align.LEFT; letterSpacing = 0.08f
        }
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText; textSize = (topHeaderH * 0.38f).coerceIn(12f * scaleFactor, 20f * scaleFactor); typeface = getSlateFont(context, weight = 700); textAlign = Paint.Align.LEFT
        }

        if (dayTagPaint.measureText(dayOfWeekFull) > maxHeaderTextW) {
            dayTagPaint.textSize *= (maxHeaderTextW / dayTagPaint.measureText(dayOfWeekFull))
        }
        if (datePaint.measureText(heroDateStr) > maxHeaderTextW) {
            datePaint.textSize *= (maxHeaderTextW / datePaint.measureText(heroDateStr))
        }

        val fmTag = dayTagPaint.fontMetrics
        val fmDate = datePaint.fontMetrics
        val tagY = clockCy - (2f * scaleFactor) - fmTag.descent
        val dateY = clockCy + (2f * scaleFactor) - fmDate.ascent

        canvas.drawText(dayOfWeekFull, rightInfoLeft, tagY, dayTagPaint)
        canvas.drawText(heroDateStr, rightInfoLeft, dateY, datePaint)

        // Bottom Progress Section
        val progAreaTop = clockCy + outerRadius + (availH * 0.04f)
        val progAreaH = cardRect.bottom - padY - progAreaTop

        var progLabelSize = (availH * 0.065f).coerceIn(8f * scaleFactor, 11f * scaleFactor)
        val progLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText; textSize = progLabelSize; typeface = getSlateFont(context, weight = 700); textAlign = Paint.Align.LEFT; letterSpacing = 0.08f
        }
        val progValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText; textSize = progLabelSize; typeface = getSlateFont(context, weight = 700); textAlign = Paint.Align.RIGHT
        }

        var fmProg = progLabelPaint.fontMetrics
        var hProg = fmProg.descent - fmProg.ascent
        var rowGap = availH * 0.03f
        var barH = availH * 0.03f

        var totalBottomH = 3 * (hProg + (2f * scaleFactor) + barH) + (2 * rowGap)
        if (totalBottomH > progAreaH) {
            val s = progAreaH / totalBottomH
            progLabelPaint.textSize *= s
            progValPaint.textSize *= s
            rowGap *= s; barH *= s
            fmProg = progLabelPaint.fontMetrics
            hProg = fmProg.descent - fmProg.ascent
            totalBottomH = 3 * (hProg + (2f * scaleFactor) + barH) + (2 * rowGap)
        }

        var currentY = progAreaTop + ((progAreaH - totalBottomH) / 2f)

        val progBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = trackBgColor; style = Paint.Style.FILL }
        val progFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColorInt; style = Paint.Style.FILL }
        val progSecondaryFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryText; style = Paint.Style.FILL }

        val progressRows = listOf(
            Triple("WEEK", (weekProgressRatio * 100).toInt(), weekProgressRatio),
            Triple("MONTH", (monthProgressRatio * 100).toInt(), monthProgressRatio),
            Triple("YEAR", (yearProgressRatio * 100).toInt(), yearProgressRatio)
        )

        for ((idx, row) in progressRows.withIndex()) {
            val (label, pct, ratio) = row
            val labelYPos = currentY - fmProg.ascent

            canvas.drawText(label, cardRect.left + padX, labelYPos, progLabelPaint)
            canvas.drawText("$pct%", cardRect.left + padX + availW, labelYPos, progValPaint)
            currentY += hProg + (2f * scaleFactor)

            val trackRect = RectF(cardRect.left + padX, currentY, cardRect.left + padX + availW, currentY + barH)
            val trackRadius = barH / 2f
            canvas.drawRoundRect(trackRect, trackRadius, trackRadius, progBgPaint)

            if (ratio > 0f) {
                val fillW = (availW * ratio).coerceAtLeast(barH)
                val fillRect = RectF(cardRect.left + padX, currentY, cardRect.left + padX + fillW, currentY + barH)
                val fillPaintToUse = if (idx == 0) progFillPaint else progSecondaryFillPaint
                canvas.drawRoundRect(fillRect, trackRadius, trackRadius, fillPaintToUse)
            }

            currentY += barH + rowGap
        }
    }

    return bitmap
}

fun generateRadialArcDashboardBitmap(context: Context, state: CalendarDateState, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap = generateRadialArcDashboardBitmap(context, state, config, isResponsive = true, wDp = wDp, hDp = hDp)