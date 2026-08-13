package com.altusix.slate.widgets.calendar

import android.content.Context
import android.graphics.*
import com.altusix.slate.data.local.SlateWidgetConfig
import kotlin.div
import kotlin.text.compareTo
import kotlin.text.toFloat
import kotlin.times


// --- Helper for safe background color ---
private fun getSafeBgColor(config: SlateWidgetConfig): Int {
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val rawHex = config.backgroundColorHex.toInt()
    val r = (rawHex shr 16) and 0xFF
    val g = (rawHex shr 8) and 0xFF
    val b = rawHex and 0xFF
    return Color.argb(alphaInt, r, g, b)
}

private fun getStandardCornerRadius(density: Float): Float = 22f * density

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

    val padding = 6f * density
    val availW = w - (padding * 2f)
    val availH = h - (padding * 2f)

    val targetHDp = 65f
    val bodyH = (targetHDp * density).coerceAtMost(availH)
    val bodyW = availW

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

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val shellBgColor = getSafeBgColor(config)
    val strokeColor = if (isLight) Color.parseColor("#D1D1D6") else Color.parseColor("#2C2C2E")
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

    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, shellPaint)
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, strokePaint)

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

    val textStartX = dividerX + (bodyH * 0.14f)
    val maxAllowedTextW = (cardRect.right - textStartX - (bodyH * 0.14f)).coerceAtLeast(10f)

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

// 2. BASIC CALENDAR (4x2 Min)
fun generateBasicCalendarBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast((110 * density).toInt())
    val h = (hDp * density).toInt().coerceAtLeast((110 * density).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val isLight = config.themeMode == "LIGHT"

    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val sundayAccent = Color.parseColor("#E53935")

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = getSafeBgColor(config) }
    val cardRadius = getStandardCornerRadius(density)
    canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), cardRadius, cardRadius, bgPaint)

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
    val currentDayNum = state.dayOfMonth.toIntOrNull() ?: -1

    val totalCells = startOffset + daysInMonth
    val dateRowsNeeded = kotlin.math.ceil(totalCells / 7.0).toInt()

    val topPadding = 16f * density
    val bottomPadding = 16f * density
    val sidePadding = 20f * density
    val usableWidth = w - (sidePadding * 2)

    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = 15f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val headerY = topPadding + headerPaint.textSize
    canvas.drawText(state.monthShort.uppercase(), w / 2f, headerY, headerPaint)

    val gridTopY = headerY + (14f * density)
    val availableGridHeight = h - gridTopY - bottomPadding
    val totalGridRows = dateRowsNeeded + 1
    val rowHeight = availableGridHeight / totalGridRows
    val colWidth = usableWidth / 7f

    val dayHeaderLabels = arrayOf("M", "T", "W", "T", "F", "S", "S")
    val dayHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 13f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    val dayHeaderY = gridTopY + (rowHeight * 0.35f)
    for (col in 0..6) {
        val cx = sidePadding + (col * colWidth) + (colWidth / 2f)
        dayHeaderPaint.color = if (col == 6) sundayAccent else primaryText
        canvas.drawText(dayHeaderLabels[col], cx, dayHeaderY, dayHeaderPaint)
    }

    val dateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 13f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }

    var currentCol = startOffset
    var currentRow = 1

    for (day in 1..daysInMonth) {
        val cx = sidePadding + (currentCol * colWidth) + (colWidth / 2f)
        val cy = gridTopY + (currentRow * rowHeight) + (rowHeight * 0.35f)

        if (day == currentDayNum) {
            val boxSize = (rowHeight * 0.85f).coerceAtMost(colWidth * 0.85f)
            val rect = RectF(
                cx - (boxSize / 2f),
                cy - (boxSize / 2f) - (2f * density),
                cx + (boxSize / 2f),
                cy + (boxSize / 2f) - (2f * density)
            )
            canvas.drawRoundRect(rect, 8f * density, 8f * density, outlinePaint)
        }

        dateTextPaint.color = when {
            currentCol == 6 -> sundayAccent
            else -> primaryText
        }

        val textY = cy + (dateTextPaint.textSize / 3f) - (2f * density)
        canvas.drawText(day.toString(), cx, textY, dateTextPaint)

        currentCol++
        if (currentCol > 6) {
            currentCol = 0
            currentRow++
        }
    }

    return bitmap
}

// 3. BIG DATE (2x2 Square)
fun generateBigDateBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val cardCornerRadius = getStandardCornerRadius(density)

    val rect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val cardSize = minOf(w, h).toFloat()
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
        typeface = Typeface.DEFAULT_BOLD
    }
    val headerY = rect.top + pad + headerPaint.textSize
    canvas.drawText("${state.monthShort.uppercase()} ${state.year}", rect.left + pad, headerY, headerPaint)

    val dateText = state.dayOfMonth
    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = cardSizeRef * 0.42f
        typeface = Typeface.DEFAULT_BOLD
    }

    val dateBounds = Rect()
    datePaint.getTextBounds(dateText, 0, dateText.length, dateBounds)

    val remainingHeight = rect.bottom - headerY
    val dateY = headerY + (remainingHeight / 2f) + (dateBounds.height() / 2f) - (4f * density)
    canvas.drawText(dateText, rect.left + pad, dateY, datePaint)

    val dateWidth = datePaint.measureText(dateText)
    val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = cardSizeRef * 0.12f
        typeface = Typeface.DEFAULT_BOLD
    }

    val dayX = rect.left + pad + dateWidth + (cardSizeRef * 0.05f)
    canvas.drawText(state.dayOfWeekShort.uppercase(), dayX, dateY - (dateBounds.height() * 0.10f), dayPaint)

    return bitmap
}

// 4. MONTH OVERLAY CALENDAR (4x2 Min)
fun generateWatermarkCalendarBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast((110 * density).toInt())
    val h = (hDp * density).toInt().coerceAtLeast((110 * density).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val isLight = config.themeMode == "LIGHT"

    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val sundayAccent = Color.parseColor("#E53935")
    val watermarkColor = if (isLight) Color.parseColor("#12000000") else Color.parseColor("#1AFFFFFF")

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = getSafeBgColor(config) }
    val cardRadius = getStandardCornerRadius(density)
    canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), cardRadius, cardRadius, bgPaint)

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
    val currentDayNum = state.dayOfMonth.toIntOrNull() ?: -1

    val totalCells = startOffset + daysInMonth
    val dateRowsNeeded = kotlin.math.ceil(totalCells / 7.0).toInt()

    val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = watermarkColor
        textSize = minOf(w * 0.38f, h * 0.72f)
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val watermarkBounds = Rect()
    val watermarkText = state.monthShort.uppercase()
    watermarkPaint.getTextBounds(watermarkText, 0, watermarkText.length, watermarkBounds)
    val watermarkY = (h / 2f) + (watermarkBounds.height() / 2f) - watermarkBounds.bottom + (4f * density)
    canvas.drawText(watermarkText, w / 2f, watermarkY, watermarkPaint)

    val topPadding = 18f * density
    val bottomPadding = 16f * density
    val sidePadding = 20f * density
    val usableWidth = w - (sidePadding * 2)

    val gridTopY = topPadding
    val availableGridHeight = h - gridTopY - bottomPadding
    val totalGridRows = dateRowsNeeded + 1
    val rowHeight = availableGridHeight / totalGridRows
    val colWidth = usableWidth / 7f

    val dayHeaderLabels = arrayOf("M", "T", "W", "T", "F", "S", "S")
    val dayHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 13f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    val dayHeaderY = gridTopY + (rowHeight * 0.35f)
    for (col in 0..6) {
        val cx = sidePadding + (col * colWidth) + (colWidth / 2f)
        dayHeaderPaint.color = if (col == 6) sundayAccent else primaryText
        canvas.drawText(dayHeaderLabels[col], cx, dayHeaderY, dayHeaderPaint)
    }

    val dateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 13f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }

    var currentCol = startOffset
    var currentRow = 1

    for (day in 1..daysInMonth) {
        val cx = sidePadding + (currentCol * colWidth) + (colWidth / 2f)
        val cy = gridTopY + (currentRow * rowHeight) + (rowHeight * 0.35f)

        if (day == currentDayNum) {
            val boxSize = (rowHeight * 0.85f).coerceAtMost(colWidth * 0.85f)
            val rect = RectF(
                cx - (boxSize / 2f),
                cy - (boxSize / 2f) - (2f * density),
                cx + (boxSize / 2f),
                cy + (boxSize / 2f) - (2f * density)
            )
            canvas.drawRoundRect(rect, 8f * density, 8f * density, outlinePaint)
        }

        dateTextPaint.color = when {
            currentCol == 6 -> sundayAccent
            else -> primaryText
        }

        val textY = cy + (dateTextPaint.textSize / 3f) - (2f * density)
        canvas.drawText(day.toString(), cx, textY, dateTextPaint)

        currentCol++
        if (currentCol > 6) {
            currentCol = 0
            currentRow++
        }
    }

    return bitmap
}

// 5. CALENDAR PAGE (2x2 Square / Responsive Single Card)
fun generateCalendarPageBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bodyBgColor = getSafeBgColor(config)

    // 1. Calculate Card Rect (Responsive fills container; Fixed centers 1:1 square)
    val rect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val cardSize = minOf(w, h).toFloat()
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardRadius = 22f * density

    // Draw Card Body Background
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bodyBgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardRadius, cardRadius, bodyPaint)

    // 2. Top Accent Banner
    val bannerH = rect.height() * 0.28f
    val bannerRect = RectF(rect.left, rect.top, rect.right, rect.top + bannerH)

    val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }

    // Clip top corners of banner to card bounds
    canvas.save()
    val clipPath = Path().apply {
        addRoundRect(rect, cardRadius, cardRadius, Path.Direction.CW)
    }
    canvas.clipPath(clipPath)
    canvas.drawRect(bannerRect, bannerPaint)
    canvas.restore()

    // 3. Top Banner Text: Weekday Name
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
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    // AUTO-SCALE DOWN BANNER TEXT ONLY WHEN CROPPED
    val maxBannerWidth = bannerRect.width() * 0.86f // Leave 7% padding on each side
    val measuredBannerWidth = bannerTextPaint.measureText(fullDayName)
    if (measuredBannerWidth > maxBannerWidth) {
        bannerTextPaint.textSize = baseBannerTextSize * (maxBannerWidth / measuredBannerWidth)
    }

    val bannerTextY = bannerRect.centerY() + (bannerTextPaint.textSize / 3f) - (1f * density)
    canvas.drawText(fullDayName, bannerRect.centerX(), bannerTextY, bannerTextPaint)

    // 4. Lower Body Content (Giant Date & Full Month Name - Vertically Centered)
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
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    val baseMonthTextSize = bodyAreaRect.height() * 0.14f
    val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = baseMonthTextSize
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.08f
    }

    // AUTO-SCALE DOWN DATE & MONTH ONLY WHEN CROPPED
    val maxBodyWidth = bodyAreaRect.width() * 0.88f

    val measuredDateWidth = datePaint.measureText(dateText)
    if (measuredDateWidth > maxBodyWidth) {
        datePaint.textSize = baseDateTextSize * (maxBodyWidth / measuredDateWidth)
    }

    val measuredMonthWidth = monthPaint.measureText(fullMonthName)
    if (measuredMonthWidth > maxBodyWidth) {
        monthPaint.textSize = baseMonthTextSize * (maxBodyWidth / measuredMonthWidth)
    }

    // Measure exact bounds to center both lines as a single block
    val dateBounds = Rect()
    datePaint.getTextBounds(dateText, 0, dateText.length, dateBounds)

    val monthBounds = Rect()
    monthPaint.getTextBounds(fullMonthName, 0, fullMonthName.length, monthBounds)

    val gap = bodyAreaRect.height() * 0.08f
    val totalBlockHeight = dateBounds.height() + gap + monthBounds.height()

    val verticalOffset = -5f * density
    val blockTop = (bodyAreaRect.centerY() - (totalBlockHeight / 2f)) + verticalOffset

    val dateY = blockTop + dateBounds.height() - dateBounds.bottom
    val monthY = blockTop + dateBounds.height() + gap + monthBounds.height() - monthBounds.bottom

    canvas.drawText(dateText, bodyAreaRect.centerX(), dateY, datePaint)
    canvas.drawText(fullMonthName, bodyAreaRect.centerX(), monthY, monthPaint)

    return bitmap
}

// 6. INLINE HEADER DATE (2x2 Square / Responsive Single Card)
fun generateInlineHeaderDateBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    // 1. Calculate Card Rect (Responsive fills container; Fixed centers 1:1 square)
    val rect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val cardSize = minOf(w, h).toFloat()
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardRadius = 22f * density
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardRadius, cardRadius, bgPaint)

    // 2. Colors & Reference Scaling
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#B3FFFFFF")
    val sundayAccent = Color.parseColor("#E53935")

    val cardSizeRef = minOf(rect.width(), rect.height())
    val padX = rect.width() * 0.12f

    // Format Day & Month Text (Title Case: "Tue", "Aug")
    val dayText = state.dayOfWeekShort.lowercase().replaceFirstChar { it.uppercase() }
    val monthText = state.monthShort.lowercase().replaceFirstChar { it.uppercase() }

    val isSunday = state.dayOfWeekShort.uppercase() == "SUN"
    val dayColor = if (isSunday) sundayAccent else accentColorInt

    // 3. Header Paints & Width Measurement
    var baseHeaderSize = cardSizeRef * 0.16f

    val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dayColor
        textSize = baseHeaderSize
        typeface = Typeface.DEFAULT_BOLD
    }

    val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = baseHeaderSize
        typeface = Typeface.DEFAULT_BOLD
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

    // 4. Giant Date Number Measurement
    val dateText = state.dayOfMonth
    var baseDateTextSize = cardSizeRef * 0.58f

    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = baseDateTextSize
        typeface = Typeface.DEFAULT_BOLD
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

    // 5. Center Both as a Single Compact Stack
    val verticalGap = cardSizeRef * 0.08f // Tight gap between header and date
    val totalBlockHeight = headerHeight + verticalGap + dateHeight

    val blockTop = rect.centerY() - (totalBlockHeight / 2f)

    val headerY = blockTop + headerHeight - headerBounds.bottom
    val dateY = blockTop + headerHeight + verticalGap + dateHeight - dateBounds.bottom

    // Draw Header Line
    canvas.drawText(dayText, headerX, headerY, dayPaint)
    canvas.drawText(monthText, headerX + dayW + headerGapX, headerY, monthPaint)

    // Draw Date Text
    canvas.drawText(dateText, rect.centerX(), dateY, datePaint)

    return bitmap
}

// 7. FLIP CALENDAR (2x2 Square / Responsive Single Card)
fun generateSplitFlapCalendarBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    // 1. Calculate Outer Card Rect (Responsive fills container; Fixed centers 1:1 square)
    val rect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val cardSize = minOf(w, h).toFloat()
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardRadius = 22f * density
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardRadius, cardRadius, bgPaint)

    // 2. Dimensions & Tile Colors
    val cardSizeRef = minOf(rect.width(), rect.height())
    val padH = rect.width() * 0.08f
    val padV = rect.height() * 0.08f
    val gapY = cardSizeRef * 0.04f

    val usableH = rect.height() - (padV * 2f)
    val tileH = (usableH - gapY) / 2f
    val tileRadius = 14f * density

    val tileBgColor = if (isLight) Color.parseColor("#E5E5EA") else Color.parseColor("#222226")
    val tileBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tileBgColor
        style = Paint.Style.FILL
    }

    val splitLineColor = if (isLight) Color.parseColor("#C7C7CC") else Color.parseColor("#141416")
    val splitLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = splitLineColor
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }

    val pinColor = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#48484A")
    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pinColor
        style = Paint.Style.FILL
    }

    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE

    // 3. Helper to Draw Flat Split-Flap Tile
    fun drawFlapTile(tileRect: RectF, text: String, textSizeRatio: Float, textColor: Int) {
        // Draw Tile Background
        canvas.drawRoundRect(tileRect, tileRadius, tileRadius, tileBgPaint)

        // Draw Center Split Line
        val midY = tileRect.centerY()
        canvas.drawLine(tileRect.left, midY, tileRect.right, midY, splitLinePaint)

        // Draw Side Hinge Pins
        val pinW = 5f * density
        val pinH = 3.5f * density
        val pinMargin = 4f * density

        val leftPin = RectF(
            tileRect.left + pinMargin,
            midY - (pinH / 2f),
            tileRect.left + pinMargin + pinW,
            midY + (pinH / 2f)
        )
        val rightPin = RectF(
            tileRect.right - pinMargin - pinW,
            midY - (pinH / 2f),
            tileRect.right - pinMargin,
            midY + (pinH / 2f)
        )
        canvas.drawRoundRect(leftPin, 1f * density, 1f * density, pinPaint)
        canvas.drawRoundRect(rightPin, 1f * density, 1f * density, pinPaint)

        // Measure & Auto-Scale Text
        var baseTextSize = tileRect.height() * textSizeRatio
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = baseTextSize
            typeface = Typeface.DEFAULT_BOLD
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

    // 4. Render Top & Bottom Tiles
    val topTileRect = RectF(
        rect.left + padH,
        rect.top + padV,
        rect.right - padH,
        rect.top + padV + tileH
    )

    val bottomTileRect = RectF(
        rect.left + padH,
        topTileRect.bottom + gapY,
        rect.right - padH,
        rect.bottom - padV
    )

    val weekdayText = state.dayOfWeekShort.uppercase()
    val dayNumText = state.dayOfMonth.padStart(2, '0')

    drawFlapTile(topTileRect, weekdayText, 0.48f, primaryText)
    drawFlapTile(bottomTileRect, dayNumText, 0.58f, primaryText)

    return bitmap
}

// 8. STACKED HEADER DATE (2x2 Square / Responsive Single Card)
fun generateStackedHeaderDateBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)

    // 1. Calculate Card Rect (Responsive fills container; Fixed centers 1:1 square)
    val rect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val cardSize = minOf(w, h).toFloat()
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardCornerRadius = 22f * density
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardCornerRadius, cardCornerRadius, bgPaint)

    // 2. Exact Colors Matching Reference Design
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = Color.parseColor("#8E8E93")
    val redAccent = if (isLight) Color.parseColor("#FF3B30") else Color.parseColor("#FF453A")

    val cardSizeRef = minOf(rect.width(), rect.height())
    val padX = rect.width() * 0.12f
    val maxAvailableWidth = rect.width() - (padX * 2f)

    // Full Month Name (Uppercase)
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

    // Title Case Weekday (e.g., "Tuesday")
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

    // 3. Proportional Font Sizes & Clean Typefaces
    var monthSize = cardSizeRef * 0.10f
    var weekdaySize = cardSizeRef * 0.15f
    var dateSize = cardSizeRef * 0.42f

    val monthFont = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    val weekdayFont = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    val dateFont = Typeface.create("sans-serif-light", Typeface.NORMAL)

    val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = monthSize
        typeface = monthFont
        textAlign = Paint.Align.LEFT
        letterSpacing = 0.05f
    }

    val weekdayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = redAccent
        textSize = weekdaySize
        typeface = weekdayFont
        textAlign = Paint.Align.LEFT
    }

    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = dateSize
        typeface = dateFont
        textAlign = Paint.Align.LEFT
    }

    // Auto-scale down only if text exceeds available container width
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

    // 4. Measure Text Bounds for Vertically Centered Stack
    val monthBounds = Rect()
    monthPaint.getTextBounds(fullMonthName, 0, fullMonthName.length, monthBounds)

    val weekdayBounds = Rect()
    weekdayPaint.getTextBounds(weekdayTitle, 0, weekdayTitle.length, weekdayBounds)

    val dateBounds = Rect()
    datePaint.getTextBounds(dateText, 0, dateText.length, dateBounds)

    val gap1 = cardSizeRef * 0.035f // Tight gap between Month & Weekday
    val gap2 = cardSizeRef * 0.055f // Clean gap between Weekday & Date

    val monthH = monthBounds.height().toFloat()
    val weekdayH = weekdayBounds.height().toFloat()
    val dateH = dateBounds.height().toFloat()

    val totalBlockHeight = monthH + gap1 + weekdayH + gap2 + dateH

    // Vertically center the complete 3-element stack inside rect
    val blockTop = rect.centerY() - (totalBlockHeight / 2f)
    val leftX = rect.left + padX

    // Calculate Y baselines
    val monthY = blockTop + monthH - monthBounds.bottom
    val weekdayY = blockTop + monthH + gap1 + weekdayH - weekdayBounds.bottom
    val dateY = blockTop + monthH + gap1 + weekdayH + gap2 + dateH - dateBounds.bottom

    // 5. Draw Left-Aligned Text Stack
    canvas.drawText(fullMonthName, leftX, monthY, monthPaint)
    canvas.drawText(weekdayTitle, leftX, weekdayY, weekdayPaint)
    canvas.drawText(dateText, leftX, dateY, datePaint)

    return bitmap
}

// 9. SIDEBAR MONTH DATE (2x2 Square / Responsive Single Card)
fun generateSideBarDateBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    // 1. Calculate Card Rect (Responsive fills container; Fixed centers 1:1 square)
    val rect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val cardSize = minOf(w, h).toFloat()
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardRadius = 22f * density

    // Draw Main Card Body Background
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardRadius, cardRadius, bodyPaint)

    // 2. Left Side Accent Strip (~30% of Card Width)
    val stripW = rect.width() * 0.30f
    val stripRect = RectF(rect.left, rect.top, rect.left + stripW, rect.bottom)

    val stripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }

    // Clip left corners of strip to match card bounds
    canvas.save()
    val clipPath = Path().apply {
        addRoundRect(rect, cardRadius, cardRadius, Path.Direction.CW)
    }
    canvas.clipPath(clipPath)
    canvas.drawRect(stripRect, stripPaint)
    canvas.restore()

    // 3. Vertical Month Text inside Left Strip
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

    // Auto-calculate high-contrast text color against the accent background
    val r = ((accentColorInt shr 16) and 0xFF) / 255f
    val g = ((accentColorInt shr 8) and 0xFF) / 255f
    val b = (accentColorInt and 0xFF) / 255f
    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
    val stripTextColor = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

    var baseMonthTextSize = stripW * 0.42f
    val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = stripTextColor
        textSize = baseMonthTextSize
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.08f
    }

    val maxMonthHeight = stripRect.height() * 0.82f
    if (monthPaint.measureText(fullMonthName) > maxMonthHeight) {
        baseMonthTextSize *= (maxMonthHeight / monthPaint.measureText(fullMonthName))
        monthPaint.textSize = baseMonthTextSize
    }

    // Rotate text -90 degrees to render vertically reading bottom-to-top
    canvas.save()
    val stripCx = stripRect.centerX()
    val stripCy = stripRect.centerY()
    canvas.rotate(-90f, stripCx, stripCy)

    val monthBounds = Rect()
    monthPaint.getTextBounds(fullMonthName, 0, fullMonthName.length, monthBounds)
    val monthTextY = stripCy + (monthBounds.height() / 2f) - monthBounds.bottom

    canvas.drawText(fullMonthName, stripCx, monthTextY, monthPaint)
    canvas.restore()

    // 4. Giant Date Number in Right Body Area
    val rightAreaRect = RectF(stripRect.right, rect.top, rect.right, rect.bottom)
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE

    val dateText = state.dayOfMonth
    var baseDateSize = rightAreaRect.width() * 0.55f

    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = baseDateSize
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
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

// 10. QUADRANT GRID DATE (2x2 Square / Responsive Single Card)
fun generateGridQuadrantCalendarBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)

    // 1. Calculate Card Rect (Responsive fills container; Fixed centers 1:1 square)
    val rect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val cardSize = minOf(w, h).toFloat()
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardRadius = 22f * density
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardRadius, cardRadius, bgPaint)

    // 2. Draw Subtle Crosshair Grid Dividers
    val dividerColor = if (isLight) Color.parseColor("#18000000") else Color.parseColor("#1FFFFFFF")
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dividerColor
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }

    val cx = rect.centerX()
    val cy = rect.centerY()

    // Vertical & Horizontal Grid Lines
    canvas.drawLine(cx, rect.top, cx, rect.bottom, dividerPaint)
    canvas.drawLine(rect.left, cy, rect.right, cy, dividerPaint)

    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE

    // 3. Top-Left Cell: Giant Date Number
    val topLeftRect = RectF(rect.left, rect.top, cx, cy)
    val dateText = state.dayOfMonth

    var baseDateSize = topLeftRect.height() * 0.58f
    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = baseDateSize
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
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

    // 4. Bottom-Right Cell: Stacked Weekday & Month Text
    val bottomRightRect = RectF(cx, cy, rect.right, rect.bottom)
    val dayText = state.dayOfWeekShort.uppercase()
    val monthText = state.monthShort.uppercase()

    var baseTextSize = bottomRightRect.height() * 0.28f
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = baseTextSize
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
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

// 11. DIAGONAL SPLIT DATE (2x2 Square / Responsive Single Card)
fun generateDiagonalSplitDateBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    // 1. Calculate Card Rect (Responsive fills container; Fixed centers 1:1 square)
    val rect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val cardSize = minOf(w, h).toFloat()
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardRadius = 22f * density

    // 2. Base Background (Top-Left Main BG)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardRadius, cardRadius, bgPaint)

    // 3. Diagonal Split Accent Polygon (Bottom-Right Half)
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

    // Clip accent triangle to match outer card corners
    canvas.save()
    val cardClipPath = Path().apply {
        addRoundRect(rect, cardRadius, cardRadius, Path.Direction.CW)
    }
    canvas.clipPath(cardClipPath)
    canvas.drawPath(diagonalPath, accentPaint)
    canvas.restore()

    // 4. Contrast Calculations & Color Setup
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE

    val r = ((accentColorInt shr 16) and 0xFF) / 255f
    val g = ((accentColorInt shr 8) and 0xFF) / 255f
    val b = (accentColorInt and 0xFF) / 255f
    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
    val accentTextColor = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

    val cardSizeRef = minOf(rect.width(), rect.height())
    val pad = cardSizeRef * 0.12f

    // 5. Top-Left Content: Weekday Short ("WED")
    val weekdayText = state.dayOfWeekShort.uppercase()
    var baseWeekdaySize = cardSizeRef * 0.15f

    val weekdayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = baseWeekdaySize
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
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

    // 6. Bottom-Right Content: Giant Date Number ("14")
    val dateText = state.dayOfMonth
    var baseDateSize = cardSizeRef * 0.46f

    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentTextColor
        textSize = baseDateSize
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
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

// 12. SPLIT DASHBOARD CALENDAR (4x2)
fun generateSplitDashboardCalendarBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast((220 * density).toInt())
    val h = (hDp * density).toInt().coerceAtLeast((110 * density).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val targetRatio = 2.0f
        var cardH = h.toFloat()
        var cardW = cardH * targetRatio

        if (cardW > w.toFloat()) {
            cardW = w.toFloat()
            cardH = cardW / targetRatio
        }

        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(density)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val dimText = if (isLight) Color.parseColor("#C7C7CC") else Color.parseColor("#48484A")

    val r = ((accentColorInt shr 16) and 0xFF) / 255f
    val g = ((accentColorInt shr 8) and 0xFF) / 255f
    val b = (accentColorInt and 0xFF) / 255f
    val accentLuminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
    val activeTextColor = if (accentLuminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

    val padX = cardRect.width() * 0.06f
    val padY = cardRect.height() * 0.08f
    val leftW = cardRect.width() * 0.36f

    val gridLeft = cardRect.left + leftW + (cardRect.width() * 0.02f)
    val gridW = cardRect.right - gridLeft - padX
    val gridTop = cardRect.top + padY
    val gridH = cardRect.height() - (padY * 2f)

    val colW = gridW / 7f
    val totalGridRows = 6f
    val rowH = gridH / totalGridRows

    val fontScale = minOf(colW * 0.45f, rowH * 0.48f).coerceAtLeast(8f * density)

    fun getRowBaseline(row: Int): Float = gridTop + (row * rowH) + (rowH * 0.62f)

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

    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = fontScale
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
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
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
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
        val r = (cellIndex / 7) + 1
        val cx = gridLeft + (c * colW) + (colW / 2f)
        val cy = getRowBaseline(r)

        dateNumPaint.color = dimText
        canvas.drawText(dayNum.toString(), cx, cy, dateNumPaint)
        cellIndex++
    }

    for (day in 1..daysInMonth) {
        val c = cellIndex % 7
        val r = (cellIndex / 7) + 1
        if (r >= totalGridRows.toInt()) break

        val cx = gridLeft + (c * colW) + (colW / 2f)

        val drawY = if (day == currentDayNum) {
            val badgeRadius = minOf(colW * 0.40f, rowH * 0.42f)
            val badgeCenterY = gridTop + (r * rowH) + (rowH / 2f)
            val badgeRect = RectF(cx - badgeRadius, badgeCenterY - badgeRadius, cx + badgeRadius, badgeCenterY + badgeRadius)
            canvas.drawRoundRect(badgeRect, 6f * density, 6f * density, activeBadgePaint)

            dateNumPaint.color = activeTextColor
            dateNumPaint.typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)

            val fm = dateNumPaint.fontMetrics
            badgeCenterY - ((fm.descent + fm.ascent) / 2f)
        } else {
            dateNumPaint.color = primaryText
            dateNumPaint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            getRowBaseline(r)
        }

        canvas.drawText(day.toString(), cx, drawY, dateNumPaint)
        cellIndex++
    }

    var nextMonthDay = 1
    while (cellIndex < (totalGridRows.toInt() - 1) * 7) {
        val c = cellIndex % 7
        val r = (cellIndex / 7) + 1
        val cx = gridLeft + (c * colW) + (colW / 2f)
        val cy = getRowBaseline(r)

        dateNumPaint.color = dimText
        dateNumPaint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        canvas.drawText(nextMonthDay.toString(), cx, cy, dateNumPaint)
        nextMonthDay++
        cellIndex++
    }

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

    val leftX = cardRect.left + padX
    val maxLeftTextW = leftW - padX

    val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = (fontScale * 0.95f).coerceAtMost(cardRect.height() * 0.10f)
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
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
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }
    if (weekdayPaint.measureText(weekdayTitle) > maxLeftTextW) {
        weekdayPaint.textSize *= (maxLeftTextW / weekdayPaint.measureText(weekdayTitle))
    }
    val weekdayY = monthY + (weekdayPaint.textSize * 1.08f)
    canvas.drawText(weekdayTitle, leftX, weekdayY, weekdayPaint)

    val targetGiantBaseline = getRowBaseline(5)
    val topOfGiantArea = weekdayY + (6f * density)
    val availableGiantH = (targetGiantBaseline - topOfGiantArea).coerceAtLeast(10f)

    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = availableGiantH * 0.92f
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        textAlign = Paint.Align.LEFT
    }
    if (datePaint.measureText(state.dayOfMonth) > maxLeftTextW) {
        datePaint.textSize *= (maxLeftTextW / datePaint.measureText(state.dayOfMonth))
    }

    canvas.drawText(state.dayOfMonth, leftX, targetGiantBaseline, datePaint)

    return bitmap
}

// 13. FOCUS TIMELINE CALENDAR (4x2)
fun generateFocusTimelineCalendarBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast((220 * density).toInt())
    val h = (hDp * density).toInt().coerceAtLeast((110 * density).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val targetRatio = 2.0f
        var cardH = h.toFloat()
        var cardW = cardH * targetRatio

        if (cardW > w.toFloat()) {
            cardW = w.toFloat()
            cardH = cardW / targetRatio
        }

        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(density)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val rightBlockW = cardRect.width() * 0.36f
    val rightBlockRect = RectF(
        cardRect.right - rightBlockW,
        cardRect.top,
        cardRect.right,
        cardRect.bottom
    )

    val rightBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }

    canvas.save()
    val clipPath = Path().apply {
        addRoundRect(cardRect, cardRadius, cardRadius, Path.Direction.CW)
    }
    canvas.clipPath(clipPath)
    canvas.drawRect(rightBlockRect, rightBgPaint)
    canvas.restore()

    val r = ((accentColorInt shr 16) and 0xFF) / 255f
    val g = ((accentColorInt shr 8) and 0xFF) / 255f
    val b = (accentColorInt and 0xFF) / 255f
    val accentLuminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
    val giantDateTextColor = if (accentLuminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

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

    val leftAreaRect = RectF(
        cardRect.left,
        cardRect.top,
        rightBlockRect.left,
        cardRect.bottom
    )

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
        strokeWidth = 1f * density
    }

    val lineLeftX = leftAreaRect.left + padX
    val lineRightX = leftAreaRect.right - (padX * 0.8f)

    canvas.drawLine(lineLeftX, divider1Y, lineRightX, divider1Y, dividerPaint)
    canvas.drawLine(lineLeftX, divider2Y, lineRightX, divider2Y, dividerPaint)

    var baseFontSize = (rowH * 0.38f).coerceAtLeast(10f * density)

    val inactiveTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryLeftText
        textSize = baseFontSize
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        textAlign = Paint.Align.LEFT
    }

    val activeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryLeftText
        textSize = baseFontSize * 1.1f
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }

    val todayLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryLeftText
        textSize = baseFontSize
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        textAlign = Paint.Align.LEFT
    }

    val accentBarW = 3.5f * density
    val activeTextStartX = lineLeftX + accentBarW + (8f * density)
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
    val accentBarRect = RectF(
        lineLeftX,
        row2CenterY - (accentBarH / 2f),
        lineLeftX + accentBarW,
        row2CenterY + (accentBarH / 2f)
    )

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
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }

    val measuredGiantW = giantDatePaint.measureText(state.dayOfMonth)
    if (measuredGiantW > maxGiantTextW) {
        giantDatePaint.textSize = maxGiantTextH * (maxGiantTextW / measuredGiantW)
    }

    val fmGiant = giantDatePaint.fontMetrics
    val giantDateY = rightBlockRect.centerY() - ((fmGiant.descent + fmGiant.ascent) / 2f)
    canvas.drawText(state.dayOfMonth, rightBlockRect.centerX(), giantDateY, giantDatePaint)

    return bitmap
}

// 14. ANALOG TIMELINE HYBRID (4x2 / Clock & Date Pill Strip)
fun generateAnalogTimelineCalendarBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast((220 * density).toInt())
    val h = (hDp * density).toInt().coerceAtLeast((110 * density).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val targetRatio = 2.0f
        var cardH = h.toFloat()
        var cardW = cardH * targetRatio

        if (cardW > w.toFloat()) {
            cardW = w.toFloat()
            cardH = cardW / targetRatio
        }

        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(density)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    val r = ((accentColorInt shr 16) and 0xFF) / 255f
    val g = ((accentColorInt shr 8) and 0xFF) / 255f
    val b = (accentColorInt and 0xFF) / 255f
    val accentLuminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
    val activeTextColor = if (accentLuminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

    val padX = cardRect.width() * 0.07f
    val padY = cardRect.height() * 0.10f

    val clockDiameter = (cardRect.height() - (padY * 2f)).coerceAtMost(cardRect.width() * 0.32f)
    val clockCx = cardRect.left + padX + (clockDiameter / 2f)
    val clockCy = cardRect.centerY()
    val clockRadius = clockDiameter / 2f

    val clockRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = (clockDiameter * 0.035f).coerceAtLeast(1.5f * density)
    }
    canvas.drawCircle(clockCx, clockCy, clockRadius - (clockRingPaint.strokeWidth / 2f), clockRingPaint)

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

    val hourHandLength = clockRadius * 0.48f
    val hourHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = (clockDiameter * 0.05f).coerceAtLeast(2.5f * density)
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        clockCx, clockCy,
        (clockCx + hourHandLength * Math.cos(hourAngle)).toFloat(),
        (clockCy + hourHandLength * Math.sin(hourAngle)).toFloat(),
        hourHandPaint
    )

    val minHandLength = clockRadius * 0.72f
    val minHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = (clockDiameter * 0.035f).coerceAtLeast(1.8f * density)
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        clockCx, clockCy,
        (clockCx + minHandLength * Math.cos(minuteAngle)).toFloat(),
        (clockCy + minHandLength * Math.sin(minuteAngle)).toFloat(),
        minHandPaint
    )

    val secHandLength = clockRadius * 0.82f
    val secHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = (clockDiameter * 0.025f).coerceAtLeast(1.2f * density)
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(
        clockCx, clockCy,
        (clockCx + secHandLength * Math.cos(secondAngle)).toFloat(),
        (clockCy + secHandLength * Math.sin(secondAngle)).toFloat(),
        secHandPaint
    )

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

    val pillCenterY = timelineTop + (rowH * 1.5f)
    val pillHeight = rowH * 0.82f
    val pillRect = RectF(
        timelineLeft,
        pillCenterY - (pillHeight / 2f),
        timelineRight,
        pillCenterY + (pillHeight / 2f)
    )
    val pillRadius = pillHeight * 0.28f
    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(pillRect, pillRadius, pillRadius, pillPaint)

    val fontScale = (rowH * 0.42f).coerceAtLeast(10f * density)
    val sidePad = timelineW * 0.06f

    val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = fontScale
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }

    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = activeTextColor
        textSize = fontScale * 1.1f
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
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

    return bitmap
}

// 15. WEEK PROGRESS CALENDAR (4x2 / Capsule Progress Tracker)
fun generateWeekProgressCalendarBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast((220 * density).toInt())
    val h = (hDp * density).toInt().coerceAtLeast((110 * density).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    // 1. Calculate Card Bounds (Responsive fills canvas; Fixed centers 2:1 ratio box)
    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val targetRatio = 2.0f
        var cardH = h.toFloat()
        var cardW = cardH * targetRatio

        if (cardW > w.toFloat()) {
            cardW = w.toFloat()
            cardH = cardW / targetRatio
        }

        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(density)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    // Theme Colors
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val futureStrokeColor = if (isLight) Color.parseColor("#C7C7CC") else Color.parseColor("#48484A")

    val padX = cardRect.width() * 0.08f
    val padY = cardRect.height() * 0.12f
    val availW = cardRect.width() - (padX * 2f)

    // Day of Week Index (Mon = 1 ... Sun = 7)
    val cal = java.util.Calendar.getInstance()
    val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
    val currentDayIndex = if (dayOfWeek == java.util.Calendar.SUNDAY) 7 else dayOfWeek - 1

    // Vertical Anchors
    val headerY = cardRect.top + padY + (cardRect.height() * 0.18f)
    val capsulesY = cardRect.top + padY + (cardRect.height() * 0.38f)
    val footerY = cardRect.bottom - padY - (cardRect.height() * 0.02f)

    // =========================================================================
    // ROW 1: REFINED TYPOGRAPHY PAIRING (e.g. "WED 12")
    // =========================================================================
    val dayText = state.dayOfWeekShort.uppercase()
    val dateText = state.dayOfMonth

    var baseHeaderSize = (cardRect.height() * 0.15f).coerceAtLeast(12f * density)

    val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = baseHeaderSize
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        letterSpacing = 0.05f
    }

    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = baseHeaderSize
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
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

    // =========================================================================
    // ROW 2: 7 SLEEK CAPSULES TRACKER
    // =========================================================================
    val gap = (availW * 0.022f).coerceAtLeast(4f * density)
    val capsuleW = (availW - (gap * 6f)) / 7f
    val capsuleH = (cardRect.height() * 0.07f).coerceIn(5f * density, 12f * density)
    val capsuleRadius = capsuleH / 2f

    val filledPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    val strokePillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = futureStrokeColor
        style = Paint.Style.STROKE
        strokeWidth = (1.2f * density)
    }

    val cTop = capsulesY - (capsuleH / 2f)
    val cBottom = cTop + capsuleH

    for (i in 1..7) {
        val cLeft = startX + (i - 1) * (capsuleW + gap)
        val cRight = cLeft + capsuleW
        val capsuleRect = RectF(cLeft, cTop, cRight, cBottom)

        when {
            i < currentDayIndex -> {
                // Past days: Solid Primary Color
                filledPillPaint.color = primaryText
                canvas.drawRoundRect(capsuleRect, capsuleRadius, capsuleRadius, filledPillPaint)
            }
            i == currentDayIndex -> {
                // Today: Solid Accent Color
                filledPillPaint.color = accentColorInt
                canvas.drawRoundRect(capsuleRect, capsuleRadius, capsuleRadius, filledPillPaint)
            }
            else -> {
                // Future days: Subtle Outlined Stroke
                val inset = strokePillPaint.strokeWidth / 2f
                val insetRect = RectF(
                    cLeft + inset,
                    cTop + inset,
                    cRight - inset,
                    cBottom - inset
                )
                canvas.drawRoundRect(insetRect, capsuleRadius, capsuleRadius, strokePillPaint)
            }
        }
    }

    // =========================================================================
    // ROW 3: FOOTER SUBTLE PROGRESS TEXT (e.g. "3 of 7 days this week gone")
    // =========================================================================
    val footerText = "$currentDayIndex of 7 days this week gone"
    var baseFooterSize = (cardRect.height() * 0.10f).coerceAtLeast(10f * density)

    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = baseFooterSize
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        textAlign = Paint.Align.LEFT
    }

    if (footerPaint.measureText(footerText) > availW) {
        baseFooterSize *= (availW / footerPaint.measureText(footerText))
        footerPaint.textSize = baseFooterSize
    }

    canvas.drawText(footerText, startX, footerY, footerPaint)

    return bitmap
}

// 16. MODULAR MATRIX CALENDAR (4x2 / Row-Aligned Year & Bento Grid)
fun generateModularMatrixCalendarBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast((220 * density).toInt())
    val h = (hDp * density).toInt().coerceAtLeast((110 * density).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    // 1. Calculate Card Bounds (Responsive fills bounds; Fixed centers 2:1 ratio container)
    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val targetRatio = 2.0f
        var cardH = h.toFloat()
        var cardW = cardH * targetRatio

        if (cardW > w.toFloat()) {
            cardW = w.toFloat()
            cardH = cardW / targetRatio
        }

        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(density)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    // Theme Colors
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val tileBgColor = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#18181A")
    val tileStrokeColor = if (isLight) Color.parseColor("#E5E5EA") else Color.parseColor("#2C2C2E")

    // Dynamic contrast check for active day badge
    val r = ((accentColorInt shr 16) and 0xFF) / 255f
    val g = ((accentColorInt shr 8) and 0xFF) / 255f
    val b = (accentColorInt and 0xFF) / 255f
    val accentLuminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
    val activeTextColor = if (accentLuminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

    val padX = cardRect.width() * 0.06f
    val padY = cardRect.height() * 0.08f
    val usableW = cardRect.width() - (padX * 2f)

    // =========================================================================
    // TOP HEADER STATUS LINE
    // =========================================================================
    val cal = java.util.Calendar.getInstance()
    val weekOfYear = cal.get(java.util.Calendar.WEEK_OF_YEAR)
    val headerText = "WEEK $weekOfYear OF 52  •  MAKE IT COUNT"

    var headerTextSize = (cardRect.height() * 0.08f).coerceAtLeast(8f * density)
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = headerTextSize
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        letterSpacing = 0.08f
        textAlign = Paint.Align.LEFT
    }

    // Auto-fit Header Text
    if (headerPaint.measureText(headerText) > usableW) {
        headerTextSize *= (usableW / headerPaint.measureText(headerText))
        headerPaint.textSize = headerTextSize
    }

    val headerY = cardRect.top + padY + headerPaint.textSize
    canvas.drawText(headerText, cardRect.left + padX, headerY, headerPaint)

    // =========================================================================
    // GRID GEOMETRY: LEFT YEAR COLUMN + RIGHT 2-ROW BENTO TILES
    // =========================================================================
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
    val tileRadius = (tileH * 0.22f).coerceIn(6f * density, 12f * density)

    val row1Y = gridTop
    val row2Y = gridTop + tileH + gapY

    val row1CenterY = row1Y + (tileH / 2f)
    val row2CenterY = row2Y + (tileH / 2f)

    // =========================================================================
    // 1. LEFT STACKED YEAR ("20" aligned with Row 1, "26" aligned with Row 2)
    // =========================================================================
    val yearStr = state.year
    val yearTop = if (yearStr.length >= 2) yearStr.substring(0, 2) else "20"
    val yearBottom = if (yearStr.length >= 4) yearStr.substring(2, 4) else "26"

    var yearTextSize = tileH * 0.88f
    val yearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = yearTextSize
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }

    // Auto-fit Year Text to Column Width
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

    // =========================================================================
    // 2. RIGHT BENTO MATRIX TILES
    // =========================================================================
    val tileBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val tileStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tileStrokeColor
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }

    // Days Calculation (Monday to Sunday)
    val weekCal = (cal.clone() as java.util.Calendar).apply {
        firstDayOfWeek = java.util.Calendar.MONDAY
        set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
    }

    val todayNum = cal.get(java.util.Calendar.DAY_OF_MONTH)
    val todayMonth = cal.get(java.util.Calendar.MONTH)
    val dayNames = arrayOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

    // RENDER ROW 1: 3 Day Tiles + 1 Wide Month Tile
    for (i in 0..2) {
        val tLeft = rightGridLeft + (i * (unitW + gapX))
        val tRect = RectF(tLeft, row1Y, tLeft + unitW, row1Y + tileH)

        val tileDate = weekCal.get(java.util.Calendar.DAY_OF_MONTH)
        val tileMonth = weekCal.get(java.util.Calendar.MONTH)
        val isToday = (tileDate == todayNum && tileMonth == todayMonth)

        drawBentoDayTile(
            canvas = canvas,
            rect = tRect,
            radius = tileRadius,
            dayName = dayNames[i],
            dateNum = tileDate.toString(),
            isToday = isToday,
            accentColor = accentColorInt,
            activeTextColor = activeTextColor,
            tileBgColor = tileBgColor,
            primaryText = primaryText,
            secondaryText = secondaryText,
            bgPaint = tileBgPaint,
            strokePaint = tileStrokePaint,
            density = density
        )
        weekCal.add(java.util.Calendar.DAY_OF_MONTH, 1)
    }

    // Row 1 Tile 4: Wide Month Tile (Spans 2 units)
    val monthTileLeft = rightGridLeft + (3 * (unitW + gapX))
    val monthTileW = (unitW * 2f) + gapX
    val monthTileRect = RectF(monthTileLeft, row1Y, monthTileLeft + monthTileW, row1Y + tileH)

    tileBgPaint.color = tileBgColor
    canvas.drawRoundRect(monthTileRect, tileRadius, tileRadius, tileBgPaint)
    canvas.drawRoundRect(monthTileRect, tileRadius, tileRadius, tileStrokePaint)

    val fullMonthTitle = state.monthShort.uppercase()
    var monthTextSize = (tileH * 0.36f).coerceAtLeast(10f * density)
    val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = monthTextSize
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        letterSpacing = 0.10f
        textAlign = Paint.Align.CENTER
    }

    // Auto-fit Month Tile Text
    val maxMonthW = monthTileRect.width() * 0.85f
    if (monthPaint.measureText(fullMonthTitle) > maxMonthW) {
        monthTextSize *= (maxMonthW / monthPaint.measureText(fullMonthTitle))
        monthPaint.textSize = monthTextSize
    }

    val fmM = monthPaint.fontMetrics
    val monthY = monthTileRect.centerY() - ((fmM.descent + fmM.ascent) / 2f)
    canvas.drawText(fullMonthTitle, monthTileRect.centerX(), monthY, monthPaint)

    // RENDER ROW 2: 4 Day Tiles + 1 Dot Icon Badge Tile
    for (i in 3..6) {
        val colIdx = i - 3
        val tLeft = rightGridLeft + (colIdx * (unitW + gapX))
        val tRect = RectF(tLeft, row2Y, tLeft + unitW, row2Y + tileH)

        val tileDate = weekCal.get(java.util.Calendar.DAY_OF_MONTH)
        val tileMonth = weekCal.get(java.util.Calendar.MONTH)
        val isToday = (tileDate == todayNum && tileMonth == todayMonth)

        drawBentoDayTile(
            canvas = canvas,
            rect = tRect,
            radius = tileRadius,
            dayName = dayNames[i],
            dateNum = tileDate.toString(),
            isToday = isToday,
            accentColor = accentColorInt,
            activeTextColor = activeTextColor,
            tileBgColor = tileBgColor,
            primaryText = primaryText,
            secondaryText = secondaryText,
            bgPaint = tileBgPaint,
            strokePaint = tileStrokePaint,
            density = density
        )
        weekCal.add(java.util.Calendar.DAY_OF_MONTH, 1)
    }

    // Row 2 Tile 5: Minimal Accent Icon Tile
    val iconTileLeft = rightGridLeft + (4 * (unitW + gapX))
    val iconTileRect = RectF(iconTileLeft, row2Y, iconTileLeft + unitW, row2Y + tileH)

    tileBgPaint.color = tileBgColor
    canvas.drawRoundRect(iconTileRect, tileRadius, tileRadius, tileBgPaint)
    canvas.drawRoundRect(iconTileRect, tileRadius, tileRadius, tileStrokePaint)

    // Draw Minimal 4-Dot Grid Symbol
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    val dotR = (minOf(unitW, tileH) * 0.08f).coerceIn(1.5f * density, 4f * density)
    val dotOffset = dotR * 2.2f
    val icCx = iconTileRect.centerX()
    val icCy = iconTileRect.centerY()

    canvas.drawCircle(icCx - dotOffset, icCy - dotOffset, dotR, dotPaint)
    canvas.drawCircle(icCx + dotOffset, icCy - dotOffset, dotR, dotPaint)
    canvas.drawCircle(icCx - dotOffset, icCy + dotOffset, dotR, dotPaint)
    canvas.drawCircle(icCx + dotOffset, icCy + dotOffset, dotR, dotPaint)

    return bitmap
}

// Helper Renderer for Bento Matrix Day Tiles with Refined Typography & Tighter Spacing
private fun drawBentoDayTile(
    canvas: Canvas,
    rect: RectF,
    radius: Float,
    dayName: String,
    dateNum: String,
    isToday: Boolean,
    accentColor: Int,
    activeTextColor: Int,
    tileBgColor: Int,
    primaryText: Int,
    secondaryText: Int,
    bgPaint: Paint,
    strokePaint: Paint,
    density: Float
) {
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
        // Active Day: Reduced weekday text size & weight + Tight Gap between Day and Number
        var labelSize = (rect.height() * 0.22f).coerceAtLeast(7f * density)
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor
            textSize = labelSize
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.04f
            textAlign = Paint.Align.CENTER
        }
        if (labelPaint.measureText(dayName) > maxTileTextW) {
            labelPaint.textSize = labelSize * (maxTileTextW / labelPaint.measureText(dayName))
        }

        var numSize = (rect.height() * 0.44f).coerceAtLeast(9f * density)
        val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = numColor
            textSize = numSize
            typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        if (numPaint.measureText(dateNum) > maxTileTextW) {
            numPaint.textSize = numSize * (maxTileTextW / numPaint.measureText(dateNum))
        }

        val fmL = labelPaint.fontMetrics
        val fmN = numPaint.fontMetrics

        val gap = -2.5f * density
        val labelH = -fmL.ascent + fmL.descent
        val numH = -fmN.ascent + fmN.descent
        val totalBlockH = labelH + gap + numH

        val blockTop = rect.centerY() - (totalBlockH / 2f)
        val labelY = blockTop - fmL.ascent
        val numY = labelY + fmL.descent + gap - fmN.ascent

        canvas.drawText(dayName, cx, labelY, labelPaint)
        canvas.drawText(dateNum, cx, numY, numPaint)
    } else {
        // Inactive Day: Reduced font size & lighter weight for weekdays
        var labelSize = (rect.height() * 0.22f).coerceAtLeast(7f * density)
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor
            textSize = labelSize
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
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

// 17. ELEGANT OVERVIEW CALENDAR (4x2 / Giant Date & Month Grid)
fun generateOverviewCalendarBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast((220 * density).toInt())
    val h = (hDp * density).toInt().coerceAtLeast((110 * density).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    // 1. Calculate Card Bounds (Responsive fills bounds; Fixed centers 2:1 ratio container)
    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val targetRatio = 2.0f
        var cardH = h.toFloat()
        var cardW = cardH * targetRatio

        if (cardW > w.toFloat()) {
            cardW = w.toFloat()
            cardH = cardW / targetRatio
        }

        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(density)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    // Theme Colors
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val dimText = if (isLight) Color.parseColor("#C7C7CC") else Color.parseColor("#48484A")

    // Dynamic contrast check for active day badge
    val r = ((accentColorInt shr 16) and 0xFF) / 255f
    val g = ((accentColorInt shr 8) and 0xFF) / 255f
    val b = (accentColorInt and 0xFF) / 255f
    val accentLuminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
    val activeTextColor = if (accentLuminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

    val padX = cardRect.width() * 0.06f
    val padY = cardRect.height() * 0.09f

    // Partition: Left Section (~28%), Right Calendar Grid (~72%)
    val leftSectionW = cardRect.width() * 0.28f
    val rightSectionLeft = cardRect.left + padX + leftSectionW + (cardRect.width() * 0.03f)
    val rightSectionW = cardRect.right - padX - rightSectionLeft

    // =========================================================================
    // RIGHT SIDE CALENDAR COMPUTATIONS (Required first for baseline locking)
    // =========================================================================
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
    val firstDaySunIndex = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1 // 0 for Sun ... 6 for Sat
    val currentDayNum = state.dayOfMonth.toIntOrNull() ?: -1

    val totalCells = firstDaySunIndex + daysInMonth
    val dateRowsNeeded = kotlin.math.ceil(totalCells / 7.0).toInt()
    val totalGridRows = dateRowsNeeded + 1

    val colW = rightSectionW / 7f

    var monthTitleSize = (cardRect.height() * 0.15f).coerceIn(11f * density, 20f * density)
    val monthTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = monthTitleSize
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
    }

    val fmM = monthTitlePaint.fontMetrics
    val monthY = cardRect.top + padY - fmM.ascent

    val gridTop = monthY + (4f * density)
    val availableGridH = cardRect.bottom - padY - gridTop
    val rowH = availableGridH / totalGridRows

    val fontScale = minOf(colW * 0.44f, rowH * 0.48f).coerceIn(6f * density, 12f * density)

    val dateNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = fontScale
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }

    // Calculate baseline of the bottom-most row of the calendar grid
    val lastRowIndex = totalGridRows - 1
    val lastRowCenterY = gridTop + (lastRowIndex * rowH) + (rowH / 2f)
    val fmGrid = dateNumPaint.fontMetrics
    val lastRowGridBaseline = lastRowCenterY - ((fmGrid.descent + fmGrid.ascent) / 2f)

    // =========================================================================
    // LEFT SIDE: TOP-LEFT WEEKDAY & BASELINE-LOCKED GIANT DATE NUMBER
    // =========================================================================
    val weekdayText = state.dayOfWeekShort.lowercase().replaceFirstChar { it.uppercase() }
    val dateText = state.dayOfMonth

    val leftMaxW = leftSectionW - (2f * density)

    // 1. Weekday ("Wed") - Top Left Aligned
    var baseWeekdaySize = (cardRect.height() * 0.18f).coerceIn(11f * density, 22f * density)
    val weekdayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = baseWeekdaySize
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        textAlign = Paint.Align.LEFT
    }
    if (weekdayPaint.measureText(weekdayText) > leftMaxW) {
        baseWeekdaySize *= (leftMaxW / weekdayPaint.measureText(weekdayText))
        weekdayPaint.textSize = baseWeekdaySize
    }

    val fmW = weekdayPaint.fontMetrics
    val weekdayY = cardRect.top + padY - fmW.ascent

    // 2. Giant Date Number ("12") - Right Aligned to Left Section & Baseline Locked to Grid Bottom
    var baseDateSize = (cardRect.height() * 0.52f).coerceIn(26f * density, 126f * density)
    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = baseDateSize
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        textAlign = Paint.Align.RIGHT
    }
    if (datePaint.measureText(dateText) > leftMaxW) {
        baseDateSize *= (leftMaxW / datePaint.measureText(dateText))
        datePaint.textSize = baseDateSize
    }

    // Lock date baseline directly to the calendar grid's bottom row baseline
    val dateY = lastRowGridBaseline

    val leftX = cardRect.left + padX
    val dateRightX = cardRect.left + padX + leftSectionW

    canvas.drawText(weekdayText, leftX, weekdayY, weekdayPaint)
    canvas.drawText(dateText, dateRightX, dateY, datePaint)

    // =========================================================================
    // RENDER RIGHT SIDE: MONTH TITLE + DAY HEADERS + CALENDAR GRID
    // =========================================================================
    val dayHeaderLabels = arrayOf("S", "M", "T", "W", "T", "F", "S")
    val dayHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = fontScale * 0.88f
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    // Align Month Text Right Boundary to Match Grid Content Right Edge
    val gridContentRightX = rightSectionLeft + (6.5f * colW) + (dayHeaderPaint.measureText("S") / 2f)
    if (monthTitlePaint.measureText(fullMonthTitle) > rightSectionW) {
        monthTitleSize *= (rightSectionW / monthTitlePaint.measureText(fullMonthTitle))
        monthTitlePaint.textSize = monthTitleSize
    }

    canvas.drawText(fullMonthTitle, gridContentRightX, monthY, monthTitlePaint)

    // Row 0: Day Headers (S M T W T F S)
    val fmH = dayHeaderPaint.fontMetrics
    val row0CenterY = gridTop + (rowH / 2f)
    val row0TextY = row0CenterY - ((fmH.descent + fmH.ascent) / 2f)

    for (c in 0..6) {
        val cx = rightSectionLeft + (c * colW) + (colW / 2f)
        canvas.drawText(dayHeaderLabels[c], cx, row0TextY, dayHeaderPaint)
    }

    // Rows 1..dateRowsNeeded: Date Grid
    val activeBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }

    val prevCal = (cal.clone() as java.util.Calendar).apply { add(java.util.Calendar.MONTH, -1) }
    val prevMaxDays = prevCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

    var cellIndex = 0

    // Previous Month Days
    for (i in (firstDaySunIndex - 1) downTo 0) {
        val dayNum = prevMaxDays - i
        val c = cellIndex % 7
        val r = (cellIndex / 7) + 1
        val cx = rightSectionLeft + (c * colW) + (colW / 2f)
        val cy = gridTop + (r * rowH) + (rowH / 2f)

        dateNumPaint.color = dimText
        dateNumPaint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        val fm = dateNumPaint.fontMetrics
        val textY = cy - ((fm.descent + fm.ascent) / 2f)
        canvas.drawText(dayNum.toString(), cx, textY, dateNumPaint)
        cellIndex++
    }

    // Current Month Days
    for (day in 1..daysInMonth) {
        val c = cellIndex % 7
        val r = (cellIndex / 7) + 1
        if (r >= totalGridRows) break

        val cx = rightSectionLeft + (c * colW) + (colW / 2f)
        val cy = gridTop + (r * rowH) + (rowH / 2f)

        if (day == currentDayNum) {
            val badgeRadius = minOf(colW * 0.40f, rowH * 0.42f)
            canvas.drawCircle(cx, cy, badgeRadius, activeBadgePaint)

            dateNumPaint.color = activeTextColor
            dateNumPaint.typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)

            val fm = dateNumPaint.fontMetrics
            val textY = cy - ((fm.descent + fm.ascent) / 2f)
            canvas.drawText(day.toString(), cx, textY, dateNumPaint)
        } else {
            dateNumPaint.color = primaryText
            dateNumPaint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)

            val fm = dateNumPaint.fontMetrics
            val textY = cy - ((fm.descent + fm.ascent) / 2f)
            canvas.drawText(day.toString(), cx, textY, dateNumPaint)
        }
        cellIndex++
    }

    // Next Month Days
    var nextDayNum = 1
    while (cellIndex < (totalGridRows - 1) * 7) {
        val c = cellIndex % 7
        val r = (cellIndex / 7) + 1
        val cx = rightSectionLeft + (c * colW) + (colW / 2f)
        val cy = gridTop + (r * rowH) + (rowH / 2f)

        dateNumPaint.color = dimText
        dateNumPaint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        val fm = dateNumPaint.fontMetrics
        val textY = cy - ((fm.descent + fm.ascent) / 2f)
        canvas.drawText(nextDayNum.toString(), cx, textY, dateNumPaint)
        nextDayNum++
        cellIndex++
    }

    return bitmap
}

// 18. MINIMAL WEEK STRIP CALENDAR (4x2 / Date & Underlined Day Strip)
fun generateMinimalWeekStripCalendarBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast((220 * density).toInt())
    val h = (hDp * density).toInt().coerceAtLeast((110 * density).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    // 1. Card Bounds (Responsive fills bounds; Fixed centers 2:1 container)
    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val targetRatio = 2.0f
        var cardH = h.toFloat()
        var cardW = cardH * targetRatio

        if (cardW > w.toFloat()) {
            cardW = w.toFloat()
            cardH = cardW / targetRatio
        }

        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(density)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    // Colors
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#8A8A8E")

    val padX = cardRect.width() * 0.08f

    // Dates & Calendar Data
    val cal = java.util.Calendar.getInstance()
    val dayOfWeekSun1 = cal.get(java.util.Calendar.DAY_OF_WEEK) // 1 = Sun ... 7 = Sat
    val currentDayIndex = dayOfWeekSun1 - 1 // 0 = Sun ... 6 = Sat
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

    // Giant Date Sizing (Compact, Proportional)
    var dateFontSize = (cardRect.height() * 0.38f).coerceIn(28f * density, 46f * density)
    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = dateFontSize
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }

    // Tight gap following giant date number directly
    val gapBetweenDateAndStrip = 14f * density
    val dateW = datePaint.measureText(dateText)

    val startX = cardRect.left + padX
    val rightStripLeft = startX + dateW + gapBetweenDateAndStrip

    // Strip Width & Spacing
    val availableStripW = cardRect.right - padX - rightStripLeft

    var headerFontSize = (cardRect.height() * 0.16f).coerceIn(11f * density, 16f * density)
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = headerFontSize
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val dayHeaders = arrayOf("S", "M", "T", "W", "T", "F", "S")
    val letterWidth = headerPaint.measureText("W")
    val letterGap = ((availableStripW - (letterWidth * 7f)) / 6f).coerceIn(6f * density, 18f * density)

    var detailFontSize = (cardRect.height() * 0.11f).coerceIn(9f * density, 13f * density)
    val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = detailFontSize
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textAlign = Paint.Align.LEFT
    }

    if (detailPaint.measureText(detailText) > availableStripW) {
        detailFontSize *= (availableStripW / detailPaint.measureText(detailText))
        detailPaint.textSize = detailFontSize
    }

    // Font Metrics for Centering
    val fmD = datePaint.fontMetrics
    val fmH = headerPaint.fontMetrics
    val fmDet = detailPaint.fontMetrics

    val underlineH = 2.5f * density
    val underlinePadding = 3f * density
    val gapBetweenRows = 8f * density

    val line1H = -fmH.ascent + fmH.descent
    val line2H = -fmDet.ascent + fmDet.descent
    val totalRightH = line1H + underlinePadding + underlineH + gapBetweenRows + line2H

    val centerY = cardRect.centerY()
    val rightBlockTop = centerY - (totalRightH / 2f)

    val headerY = rightBlockTop - fmH.ascent
    val underlineTop = headerY + fmH.descent + underlinePadding
    val detailY = underlineTop + underlineH + gapBetweenRows - fmDet.ascent

    val dateY = centerY - ((fmD.descent + fmD.ascent) / 2f)

    // 1. Draw Giant Date Number
    canvas.drawText(dateText, startX, dateY, datePaint)

    // 2. Draw "S M T W T F S" Day Strip
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

    // 3. Draw Subtext ("Wednesday · Week 33")
    canvas.drawText(detailText, rightStripLeft, detailY, detailPaint)

    return bitmap
}

// 19. VERTICAL TIME PILL WIDGET (4x2 / Rotated Clock & Dual Capsule Stack)
fun generateVerticalTimePillCalendarBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast((220 * density).toInt())
    val h = (hDp * density).toInt().coerceAtLeast((110 * density).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    // 1. Calculate Card Bounds (Responsive fills bounds; Fixed centers 2:1 ratio container)
    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val targetRatio = 2.0f
        var cardH = h.toFloat()
        var cardW = cardH * targetRatio

        if (cardW > w.toFloat()) {
            cardW = w.toFloat()
            cardH = cardW / targetRatio
        }

        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(density)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    // Colors
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val pillBgColor = if (isLight) Color.parseColor("#E5E5EA") else Color.parseColor("#1C1C1E")

    // Dynamic contrast check for top accent pill
    val r = ((accentColorInt shr 16) and 0xFF) / 255f
    val g = ((accentColorInt shr 8) and 0xFF) / 255f
    val b = (accentColorInt and 0xFF) / 255f
    val accentLuminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
    val activeTextColor = if (accentLuminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

    val padX = cardRect.width() * 0.08f
    val padY = cardRect.height() * 0.12f

    val usableW = cardRect.width() - (padX * 2f)
    val usableH = cardRect.height() - (padY * 2f)

    val leftW = usableW * 0.22f
    val gapX = usableW * 0.06f
    val rightSectionLeft = cardRect.left + padX + leftW + gapX
    val rightSectionW = cardRect.right - padX - rightSectionLeft

    // =========================================================================
    // 2. RIGHT SIDE: DUAL STACKED CAPSULE PILLS (Fixed Corner Radius)
    // =========================================================================
    val pillGap = usableH * 0.10f
    val pillH = (usableH - pillGap) / 2f

    // FIXED CONSISTENT CORNER RADIUS: Capped at 16dp so corners don't balloon when shrunk
    val pillRadius = (16f * density).coerceAtMost(pillH / 2f)

    val topPillTop = cardRect.top + padY
    val topPillRect = RectF(rightSectionLeft, topPillTop, rightSectionLeft + rightSectionW, topPillTop + pillH)

    val bottomPillTop = topPillRect.bottom + pillGap
    val bottomPillRect = RectF(rightSectionLeft, bottomPillTop, rightSectionLeft + rightSectionW, bottomPillTop + pillH)

    // Total vertical span of the 2 bars combined
    val totalBarsSpanH = bottomPillRect.bottom - topPillRect.top

    // =========================================================================
    // 1. LEFT SIDE: VERTICALLY ROTATED TIME (Matches Height of 2 Bars)
    // =========================================================================
    val timeFormatter = java.text.SimpleDateFormat("HHmm", java.util.Locale.getDefault())
    val timeStr = timeFormatter.format(java.util.Date())

    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        letterSpacing = 0.06f
        textAlign = Paint.Align.CENTER
    }

    // Scale time text size so rotated text length exactly matches the 2-bar vertical span
    var calculatedTimeSize = 20f * density
    timePaint.textSize = calculatedTimeSize
    val measuredTimeLength = timePaint.measureText(timeStr)

    if (measuredTimeLength > 0f) {
        calculatedTimeSize *= (totalBarsSpanH / measuredTimeLength)
        timePaint.textSize = calculatedTimeSize
    }

    // Ensure rotated text thickness does not overflow left column width
    val fmCheck = timePaint.fontMetrics
    val textThickness = -fmCheck.ascent + fmCheck.descent
    if (textThickness > leftW) {
        calculatedTimeSize *= (leftW / textThickness)
        timePaint.textSize = calculatedTimeSize
    }

    val textCenterX = cardRect.left + padX + (leftW / 2f)
    val textCenterY = (topPillRect.top + bottomPillRect.bottom) / 2f // Center vertically with the 2 bars
    val fmTime = timePaint.fontMetrics
    val timeY = textCenterY - ((fmTime.descent + fmTime.ascent) / 2f)

    canvas.save()
    canvas.rotate(-90f, textCenterX, textCenterY)
    canvas.drawText(timeStr, textCenterX, timeY, timePaint)
    canvas.restore()

    // --- RENDER TOP ACCENT PILL (Date: "06" / "12") ---
    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    pillPaint.color = accentColorInt
    canvas.drawRoundRect(topPillRect, pillRadius, pillRadius, pillPaint)

    val rawDayInt = state.dayOfMonth.toIntOrNull() ?: 1
    val dateNumStr = String.format(java.util.Locale.getDefault(), "%02d", rawDayInt)

    var dateTextSize = (pillH * 0.48f).coerceIn(12f * density, 22f * density)
    val dateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = activeTextColor
        textSize = dateTextSize
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    if (dateTextPaint.measureText(dateNumStr) > topPillRect.width() * 0.80f) {
        dateTextSize *= ((topPillRect.width() * 0.80f) / dateTextPaint.measureText(dateNumStr))
        dateTextPaint.textSize = dateTextSize
    }

    val fmDate = dateTextPaint.fontMetrics
    val topTextY = topPillRect.centerY() - ((fmDate.descent + fmDate.ascent) / 2f)
    canvas.drawText(dateNumStr, topPillRect.centerX(), topTextY, dateTextPaint)

    // --- RENDER BOTTOM NEUTRAL PILL (Day of Week: "FRI" / "WED") ---
    pillPaint.color = pillBgColor
    canvas.drawRoundRect(bottomPillRect, pillRadius, pillRadius, pillPaint)

    val dayStr = state.dayOfWeekShort.uppercase()

    var dayTextSize = (pillH * 0.44f).coerceIn(11f * density, 20f * density)
    val dayTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = dayTextSize
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
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

    return bitmap
}

// 20. TIMELINE PROGRESS CALENDAR (4x2 / Minimal Horizontal Axis)
fun generateTimelineProgressCalendarBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast((220 * density).toInt())
    val h = (hDp * density).toInt().coerceAtLeast((110 * density).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val bgColor = getSafeBgColor(config)

    // 1. Card Bounds (Responsive fills bounds; Fixed centers 2:1 ratio container)
    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val targetRatio = 2.0f
        var cardH = h.toFloat()
        var cardW = cardH * targetRatio

        if (cardW > w.toFloat()) {
            cardW = w.toFloat()
            cardH = cardW / targetRatio
        }

        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardRadius = getStandardCornerRadius(density)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    // Theme Colors
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    val padX = cardRect.width() * 0.08f
    val padY = cardRect.height() * 0.12f
    val usableW = cardRect.width() - (padX * 2f)

    // Date & Calendar Metrics
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

    // =========================================================================
    // 1. HEADER MONTH NAME (e.g. "SEPTEMBER")
    // =========================================================================
    val fullMonthTitle = when (state.monthShort.uppercase()) {
        "JAN" -> "SEPTEMBER" // Fallback example format matching upper standard
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

    var monthTitleSize = (cardRect.height() * 0.15f).coerceIn(11f * density, 18f * density)
    val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = monthTitleSize
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
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

    // =========================================================================
    // 2. TIMELINE AXIS & ACCENT MARKER
    // =========================================================================
    val timelineY = cardRect.centerY() + (cardRect.height() * 0.06f)
    val axisStrokeW = 1.8f * density

    val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = axisStrokeW
    }

    // Main Horizontal Baseline
    canvas.drawLine(startX, timelineY, endX, timelineY, axisPaint)

    // Standard Interval Ticks (6 segments across month span)
    val standardTickH = 6f * density
    val tickCount = 6
    for (i in 0 until tickCount) {
        val fraction = i / (tickCount - 1).toFloat()
        val tickX = startX + (fraction * usableW)
        canvas.drawLine(tickX, timelineY - standardTickH, tickX, timelineY, axisPaint)
    }

    // Accent Progress Indicator (Current Day Position)
    val dayProgressRatio = if (daysInMonth > 1) (currentDayNum - 1) / (daysInMonth - 1).toFloat() else 0f
    val currentDayX = startX + (dayProgressRatio * usableW)

    val accentTickH = 14f * density
    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = 2.4f * density
        strokeCap = Paint.Cap.ROUND
    }

    // Draw Accent Vertical Marker
    canvas.drawLine(currentDayX, timelineY - accentTickH, currentDayX, timelineY, accentPaint)

    // Current Day Number Text Above Marker
    val currentDayStr = currentDayNum.toString()
    var dateNumSize = (cardRect.height() * 0.16f).coerceIn(11f * density, 18f * density)

    val dateNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = dateNumSize
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val halfNumW = dateNumPaint.measureText(currentDayStr) / 2f
    val clampedDayX = currentDayX.coerceIn(startX + halfNumW, endX - halfNumW)
    val dateNumY = timelineY - accentTickH - (3f * density)

    canvas.drawText(currentDayStr, clampedDayX, dateNumY, dateNumPaint)

    // =========================================================================
    // 3. START & END BOUNDARY LABELS ("1" & "30" / "31")
    // =========================================================================
    var footerLabelSize = (cardRect.height() * 0.12f).coerceIn(9f * density, 14f * density)
    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = footerLabelSize
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }

    val fmFooter = footerPaint.fontMetrics
    val footerY = timelineY + (10f * density) - fmFooter.ascent

    // Left Bound "1"
    footerPaint.textAlign = Paint.Align.LEFT
    canvas.drawText("1", startX, footerY, footerPaint)

    // Right Bound (Total Days in Month)
    val endLabelStr = daysInMonth.toString()
    footerPaint.textAlign = Paint.Align.RIGHT
    canvas.drawText(endLabelStr, endX, footerY, footerPaint)

    return bitmap
}

// 21. PAGE FLIP DATE (2x2 Square / Responsive Single Card)
fun generatePageFlipDateBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    // 1. Calculate Card Rect (Responsive fills container; Fixed centers 1:1 square)
    val rect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val cardSize = minOf(w, h).toFloat()
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardSizeRef = minOf(rect.width(), rect.height())
    val cardRadius = 22f * density
    val foldSize = cardSizeRef * 0.28f

    // 2. Main Card Path (Bottom-right corner sliced diagonally along crease)
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

    // 3. Folded Flap Path (Back side of turned corner)
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
        strokeWidth = 1.5f * density
    }
    canvas.drawLine(
        rect.right - foldSize, rect.bottom,
        rect.right, rect.bottom - foldSize,
        creaseShadowPaint
    )

    // 4. Color & Typography Setup
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE

    val weekdayText = state.dayOfWeekShort.uppercase()
    val dateText = state.dayOfMonth

    // FONT SIZES (Controlled here)
    var baseWeekdaySize = cardSizeRef * 0.11f
    var baseDateSize = cardSizeRef * 0.40f // Increased from 0.38f

    val weekdayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = baseWeekdaySize
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) // Lighter weight than bold
        textAlign = Paint.Align.LEFT
        letterSpacing = 0.12f
    }

    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = baseDateSize
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) // Reduced weight to medium
        textAlign = Paint.Align.LEFT
    }

    // Auto-scale text bounds to avoid edge clipping
    val maxContentWidth = cardSizeRef * 0.65f
    if (weekdayPaint.measureText(weekdayText) > maxContentWidth) {
        baseWeekdaySize *= (maxContentWidth / weekdayPaint.measureText(weekdayText))
        weekdayPaint.textSize = baseWeekdaySize
    }

    if (datePaint.measureText(dateText) > maxContentWidth) {
        baseDateSize *= (maxContentWidth / datePaint.measureText(dateText))
        datePaint.textSize = baseDateSize
    }

    // 5. Position Text Stack (Left Aligned)
    val weekdayBounds = Rect()
    weekdayPaint.getTextBounds(weekdayText, 0, weekdayText.length, weekdayBounds)

    val dateBounds = Rect()
    datePaint.getTextBounds(dateText, 0, dateText.length, dateBounds)

    val leftMargin = rect.left + (cardSizeRef * 0.12f)
    val topMargin = rect.top + (cardSizeRef * 0.14f)

    val weekdayY = topMargin + weekdayBounds.height()
    val gapBetween = cardSizeRef * 0.07f // Increased from 0.04f for a little more space
    val dateY = weekdayY + gapBetween + dateBounds.height()

    // Render Text (Left aligned using leftMargin)
    canvas.drawText(weekdayText, leftMargin, weekdayY, weekdayPaint)
    canvas.drawText(dateText, leftMargin, dateY, datePaint)

    return bitmap
}

// 22. VERTICAL DATE WHEEL (2x2 Square / Responsive Single Card)
fun generateVerticalDateWheelBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    // 1. Calculate Card Rect (Responsive fills container; Fixed centers 1:1 square)
    val rect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val cardSize = minOf(w, h).toFloat()
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardRadius = 22f * density
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardRadius, cardRadius, bgPaint)

    // 2. Setup Relative Reference Sizes & Colors
    val cardSizeRef = minOf(rect.width(), rect.height())
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val fadedText = if (isLight) Color.parseColor("#A0A0A5") else Color.parseColor("#4DFFFFFF")

    // 3. Date Calculations (Yesterday, Today, Tomorrow)
    val cal = java.util.Calendar.getInstance()
    val yesterdayCal = (cal.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_MONTH, -1) }
    val tomorrowCal = (cal.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_MONTH, 1) }

    val prevDayText = yesterdayCal.get(java.util.Calendar.DAY_OF_MONTH).toString()
    val todayText = state.dayOfMonth
    val weekdayText = state.dayOfWeekShort.uppercase()
    val nextDayText = tomorrowCal.get(java.util.Calendar.DAY_OF_MONTH).toString()

    // 4. Typography Paints (Single space between date and weekday)
    var centerTextSize = cardSizeRef * 0.20f
    val centerText = "$todayText $weekdayText"

    val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = centerTextSize
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val fadedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fadedText
        textSize = cardSizeRef * 0.15f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }

    // Auto-scale center text if width exceeds card bounds
    val maxAllowedW = rect.width() * 0.64f
    if (centerPaint.measureText(centerText) > maxAllowedW) {
        centerTextSize *= (maxAllowedW / centerPaint.measureText(centerText))
        centerPaint.textSize = centerTextSize
    }

    // 5. Measure Heights & Vertical Baselines
    val centerBounds = Rect()
    centerPaint.getTextBounds(centerText, 0, centerText.length, centerBounds)

    val nextBounds = Rect()
    fadedPaint.getTextBounds(nextDayText, 0, nextDayText.length, nextBounds)

    val cx = rect.centerX()
    val cy = rect.centerY() + (centerBounds.height() / 2f) - centerBounds.bottom

    // Draw Top Muted Date
    val topY = rect.centerY() - (cardSizeRef * 0.22f)
    canvas.drawText(prevDayText, cx, topY, fadedPaint)

    // Draw Main Center Row ("13 THU")
    canvas.drawText(centerText, cx, cy, centerPaint)

    // Draw Bottom Muted Date
    val bottomY = rect.centerY() + (cardSizeRef * 0.24f) + nextBounds.height()
    canvas.drawText(nextDayText, cx, bottomY, fadedPaint)

    // 6. Draw Vector Chevron Arrows
    val centerTextWidth = centerPaint.measureText(centerText)
    val arrowGap = cardSizeRef * 0.055f
    val chevronWidth = cardSizeRef * 0.022f
    val chevronHeight = cardSizeRef * 0.038f
    val midY = rect.centerY()

    val chevronPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Left Chevron (<)
    val leftChevronX = cx - (centerTextWidth / 2f) - arrowGap - chevronWidth
    val leftChevronPath = Path().apply {
        moveTo(leftChevronX + chevronWidth, midY - chevronHeight)
        lineTo(leftChevronX, midY)
        lineTo(leftChevronX + chevronWidth, midY + chevronHeight)
    }
    canvas.drawPath(leftChevronPath, chevronPaint)

    // Right Chevron (>)
    val rightChevronX = cx + (centerTextWidth / 2f) + arrowGap
    val rightChevronPath = Path().apply {
        moveTo(rightChevronX, midY - chevronHeight)
        lineTo(rightChevronX + chevronWidth, midY)
        lineTo(rightChevronX, midY + chevronHeight)
    }
    canvas.drawPath(rightChevronPath, chevronPaint)

    return bitmap
}

// 23. MONTH PROGRESS CAPSULE (2x2 Square / Responsive Single Card)
fun generateMonthProgressCapsuleBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE

    // 1. Calculate Card Rect
    val rect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val cardSize = minOf(w, h).toFloat()
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardRadius = 22f * density
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardRadius, cardRadius, bgPaint)

    // 2. Month Progress Math & Day Name
    val cal = java.util.Calendar.getInstance()
    val currentDayNum = state.dayOfMonth.toIntOrNull() ?: cal.get(java.util.Calendar.DAY_OF_MONTH)
    val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val progress = (currentDayNum.toFloat() / daysInMonth.toFloat()).coerceIn(0f, 1f)
    val percentInt = (progress * 100).toInt()

    val fullWeekdayName = cal.getDisplayName(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.LONG, java.util.Locale.ENGLISH) ?: state.dayOfWeekShort
    val cardSizeRef = minOf(rect.width(), rect.height())

    // 3. Draw Thinner Vertical Capsule Progress Bar
    val pillWidth = cardSizeRef * 0.10f
    val pillHeight = cardSizeRef * 0.58f
    val pillLeft = rect.left + (cardSizeRef * 0.12f)
    val pillTop = rect.centerY() - (pillHeight / 2f)

    val pillRect = RectF(pillLeft, pillTop, pillLeft + pillWidth, pillTop + pillHeight)
    val pillRadius = pillWidth / 2f

    val pillPath = Path().apply {
        addRoundRect(pillRect, pillRadius, pillRadius, Path.Direction.CW)
    }

    // Clip & fill progress height from bottom
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

    // Draw Capsule Outline
    val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = 2.0f * density // Slightly slimmer stroke outline
    }
    canvas.drawPath(pillPath, outlinePaint)

    // 4. Typography & Slimmer Weights
    val textLeftMargin = pillRect.right + (cardSizeRef * 0.08f)

    val dateText = state.dayOfMonth
    val weekdayText = fullWeekdayName
    val subtext = "$percentInt% through ${state.monthShort}"

    // Reduced font sizes (less bulky)
    var dateTextSize = cardSizeRef * 0.24f
    var weekdayTextSize = cardSizeRef * 0.08f
    var subtextSize = cardSizeRef * 0.07f

    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = dateTextSize
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) // Lighter weight than bold
        textAlign = Paint.Align.LEFT
    }

    val weekdayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = weekdayTextSize
        typeface = Typeface.create("sans-serif", Typeface.NORMAL) // Normal clean weight
        textAlign = Paint.Align.LEFT
    }

    val subtextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = subtextSize
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) // Clean medium weight
        textAlign = Paint.Align.LEFT
    }

    // Auto-scale width bounds
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

    // 5. Calculate Vertical Alignment with Capsule Center
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

    // Render Text Stack
    canvas.drawText(dateText, textLeftMargin, dateY, datePaint)
    canvas.drawText(weekdayText, textLeftMargin, weekdayY, weekdayPaint)
    canvas.drawText(subtext, textLeftMargin, subtextY, subtextPaint)

    return bitmap
}

// 24. TIMELINE PILLARS DATE (2x2 Square / Responsive Single Card)
fun generateTimelinePillarsBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE

    // 1. Calculate Card Rect
    val rect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val cardSize = minOf(w, h).toFloat()
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardRadius = 22f * density
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardRadius, cardRadius, bgPaint)

    // 2. Determine Dynamic Pillar Count (3 for narrow, 5 for square/standard, 7 for wide)
    val aspectRatio = rect.width() / rect.height().coerceAtLeast(1f)
    val numPillars = when {
        aspectRatio < 0.82f -> 3  // Narrow/Thin widget -> 3 Pills
        aspectRatio > 1.35f -> 7  // Wide widget -> 7 Pills
        else -> 5                 // Standard Square -> 5 Pills
    }
    val activeIndex = numPillars / 2

    // 3. Contrast Check for Active Pillar Text
    val r = Color.red(accentColorInt)
    val g = Color.green(accentColorInt)
    val b = Color.blue(accentColorInt)
    val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    val activeTextColor = if (luminance > 0.65) Color.parseColor("#161618") else Color.WHITE

    // 4. Pyramid Height Ratios
    val heightsRatio = when (numPillars) {
        3 -> floatArrayOf(0.65f, 1.0f, 0.65f)
        7 -> floatArrayOf(0.38f, 0.52f, 0.68f, 1.0f, 0.68f, 0.52f, 0.38f)
        else -> floatArrayOf(0.48f, 0.68f, 1.0f, 0.68f, 0.48f)
    }

    // 5. Geometry Setup & Variable Active Width
    val usableWidth = rect.width() * 0.84f
    val gap = rect.width() * 0.025f

    // --- ACTIVE PILLAR WIDTH  ---
    val activeWidthScale = 1.25f //

    val normalPillWidth = (usableWidth - ((numPillars - 1) * gap)) / (numPillars - 1 + activeWidthScale)
    val activePillWidth = normalPillWidth * activeWidthScale

    val startX = rect.centerX() - (usableWidth / 2f)
    val pillBottom = rect.bottom - (rect.height() * 0.12f)
    val maxPillHeight = rect.height() * 0.70f

    // 6. Paints Setup
    val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * density
    }

    val activeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }

    val dayTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = normalPillWidth * 0.5f
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    var activeWeekdaySize = activePillWidth * 0.40f
    var activeDateSize = activePillWidth * 0.54f

    val activeWeekdayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = activeTextColor
        textSize = activeWeekdaySize
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val activeDatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = activeTextColor
        textSize = activeDateSize
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    // Auto-scale active text if it exceeds active pillar width
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

    // 7. Render Pillars with Dynamic Horizontal Positioning
    val cal = java.util.Calendar.getInstance()
    var currentX = startX

    for (i in 0 until numPillars) {
        val offset = i - activeIndex
        val dayCal = (cal.clone() as java.util.Calendar).apply {
            add(java.util.Calendar.DAY_OF_MONTH, offset)
        }

        val currentPillWidth = if (i == activeIndex) activePillWidth else normalPillWidth
        val pillH = maxPillHeight * heightsRatio[i]
        val pillTop = pillBottom - pillH
        val pillRect = RectF(currentX, pillTop, currentX + currentPillWidth, pillBottom)
        val pillRadius = currentPillWidth / 2f
        val pillCenterX = pillRect.centerX()

        if (i == activeIndex) {
            // Active Today Pillar
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
            // Neighbor Pillars
            canvas.drawRoundRect(pillRect, pillRadius, pillRadius, outlinePaint)

            val dayLetter = dayCal.getDisplayName(
                java.util.Calendar.DAY_OF_WEEK,
                java.util.Calendar.SHORT,
                java.util.Locale.ENGLISH
            )?.take(1)?.uppercase() ?: ""

            val letterBounds = Rect()
            dayTextPaint.getTextBounds(dayLetter, 0, dayLetter.length, letterBounds)
            val textY = pillRect.centerY() + (letterBounds.height() / 2f) - letterBounds.bottom

            canvas.drawText(dayLetter, pillCenterX, textY, dayTextPaint)
        }

        // Advance X position for the next pillar
        currentX += currentPillWidth + gap
    }

    return bitmap
}

// 25. TILTED BADGE FLIP DATE (2x2 Square / Responsive Single Card)
fun generateTiltedBadgeFlipDateBitmap(
    context: Context,
    state: CalendarDateState,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    // 1. Calculate Card Rect
    val rect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val cardSize = minOf(w, h).toFloat()
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val cardRadius = 22f * density
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardRadius, cardRadius, bgPaint)

    val cardSizeRef = minOf(rect.width(), rect.height())

    // 2. Elements Sizing & Layout Geometry
    val cardGap = cardSizeRef * 0.025f
    val groupGap = cardSizeRef * 0.05f

    val digitCardW = cardSizeRef * 0.22f
    val digitCardH = cardSizeRef * 0.36f
    val digitCardRadius = 8f * density

    val badgeW = cardSizeRef * 0.36f
    val badgeH = cardSizeRef * 0.22f
    val badgeRadius = 8f * density

    val totalGroupW = (digitCardW * 2f) + cardGap + groupGap + badgeW
    val startX = rect.centerX() - (totalGroupW / 2f)
    val centerY = rect.centerY()

    // 3. Theme Colors for Flip Cards & Badge
    val flipCardBg = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val flipDigitColor = if (isLight) Color.WHITE else Color.BLACK
    val splitLineColor = if (isLight) Color.parseColor("#3A3A3C") else Color.parseColor("#1C1C1E")

    // Check contrast for text inside the accent badge
    val r = Color.red(accentColorInt)
    val g = Color.green(accentColorInt)
    val b = Color.blue(accentColorInt)
    val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    val badgeTextColor = if (luminance > 0.65) Color.parseColor("#161618") else Color.WHITE

    // 4. Draw Flip Digit Cards (Left side)
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
        strokeWidth = 2.0f * density
    }

    val digitTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = flipDigitColor
        textSize = digitCardH * 0.68f
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val digitCardTop = centerY - (digitCardH / 2f)

    for (i in 0..1) {
        val cardLeft = startX + (i * (digitCardW + cardGap))
        val cardRect = RectF(cardLeft, digitCardTop, cardLeft + digitCardW, digitCardTop + digitCardH)

        // Draw Flip Card Background
        canvas.drawRoundRect(cardRect, digitCardRadius, digitCardRadius, cardPaint)

        // Draw Digit Number
        val digitStr = digits[i]
        val digitBounds = Rect()
        digitTextPaint.getTextBounds(digitStr, 0, digitStr.length, digitBounds)
        val digitY = cardRect.centerY() + (digitBounds.height() / 2f) - digitBounds.bottom
        canvas.drawText(digitStr, cardRect.centerX(), digitY, digitTextPaint)

        // Draw Horizontal Split Line
        canvas.drawLine(
            cardRect.left,
            cardRect.centerY(),
            cardRect.right,
            cardRect.centerY(),
            splitLinePaint
        )
    }

    // 5. Draw Tilted Accent Weekday Badge (Right side)
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
        typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.08f
    }

    val weekdayStr = state.dayOfWeekShort.uppercase()
    val badgeTextBounds = Rect()
    badgeTextPaint.getTextBounds(weekdayStr, 0, weekdayStr.length, badgeTextBounds)
    val badgeTextY = (badgeTextBounds.height() / 2f) - badgeTextBounds.bottom

    canvas.save()
    canvas.translate(badgeCenterX, badgeCenterY)
    canvas.rotate(-8f) // Tilted -8 degrees counter-clockwise

    canvas.drawRoundRect(badgeRect, badgeRadius, badgeRadius, badgePaint)
    canvas.drawText(weekdayStr, 0f, badgeTextY, badgeTextPaint)

    canvas.restore()

    return bitmap
}