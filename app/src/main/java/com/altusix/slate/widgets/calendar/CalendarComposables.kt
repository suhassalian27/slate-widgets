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
    val alphaInt = ((if (config.opacity < 0.15f) 1.0f else config.opacity) * 255).toInt()
    val rawHex = config.backgroundColorHex.toInt()
    val r = ((rawHex shr 16) and 0xFF)
    val g = ((rawHex shr 8) and 0xFF)
    val b = (rawHex and 0xFF)
    return Color.argb(alphaInt, r, g, b)
}

private fun getStandardCornerRadius(density: Float): Float = 22f * density

// 1. CAPSULE PILL (2x1) - Fixed Height & Auto-Fitting Text
fun generatePillCalendarBitmap(    context: Context,    state: CalendarPillState,    config: SlateWidgetConfig,    wDp: Int,    hDp: Int): Bitmap {
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

// 2. BASIC CALENDAR (4x2 Min / Dynamic Grid Height)
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

    // Colors & Paints
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#B3FFFFFF")
    val sundayAccent = Color.parseColor("#E53935")

    // Background Card
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = getSafeBgColor(config) }
    val cardRadius = 24f * density
    canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), cardRadius, cardRadius, bgPaint)

    // Calculate month days and starting day offset (0 for Mon, 6 for Sun)
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
    val startOffset = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 // Mon = 0 ... Sun = 6
    val currentDayNum = state.dayOfMonth.toIntOrNull() ?: -1

    // Dynamic Row Calculation (4, 5, or 6 date rows)
    val totalCells = startOffset + daysInMonth
    val dateRowsNeeded = kotlin.math.ceil(totalCells / 7.0).toInt()

    // Layout Padding & Dimensions
    val topPadding = 16f * density
    val bottomPadding = 16f * density
    val sidePadding = 20f * density
    val usableWidth = w - (sidePadding * 2)

    // 1. Month Header (Center Aligned)
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = 15f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val headerY = topPadding + headerPaint.textSize
    canvas.drawText(state.monthShort.uppercase(), w / 2f, headerY, headerPaint)

    // Grid Setup (Day-of-week header row + dynamic date rows)
    val gridTopY = headerY + (14f * density)
    val availableGridHeight = h - gridTopY - bottomPadding
    val totalGridRows = dateRowsNeeded + 1 // +1 for M T W T F S S row
    val rowHeight = availableGridHeight / totalGridRows
    val colWidth = usableWidth / 7f

    // 2. Day-of-Week Headers (Row 0)
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

    // 3. Days Grid (Rows 1..dateRowsNeeded)
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

        // Highlight Current Day Box
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

        // Draw Date Text
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


// 3. BIG DATE (2x2 Square / Responsive)
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
    val cardCornerRadius = 22f * density

    // Responsive fills cell bounds; Fixed centers a 1:1 square
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

    // 1. Top Header (AUG 2026)
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = cardSizeRef * 0.10f
        typeface = Typeface.DEFAULT_BOLD
    }
    val headerY = rect.top + pad + headerPaint.textSize
    canvas.drawText("${state.monthShort.uppercase()} ${state.year}", rect.left + pad, headerY, headerPaint)

    // 2. Giant Date Number (11)
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

    // 3. Day of Week (TUE)
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

// 4. MONTH OVERLAY CALENDAR (4x2 Min / Giant Background Month Watermark)
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

    // Colors & Paints
    val primaryText = if (isLight) Color.parseColor("#161618") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#B3FFFFFF")
    val sundayAccent = Color.parseColor("#E53935")
    val watermarkColor = if (isLight) Color.parseColor("#12000000") else Color.parseColor("#1AFFFFFF")

    // Background Card
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = getSafeBgColor(config) }
    val cardRadius = 24f * density
    canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), cardRadius, cardRadius, bgPaint)

    // Calculate month days and starting day offset (0 for Mon, 6 for Sun)
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
    val startOffset = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 // Mon = 0 ... Sun = 6
    val currentDayNum = state.dayOfMonth.toIntOrNull() ?: -1

    // Dynamic Row Calculation (4, 5, or 6 date rows)
    val totalCells = startOffset + daysInMonth
    val dateRowsNeeded = kotlin.math.ceil(totalCells / 7.0).toInt()

    // 1. GIANT BACKGROUND MONTH WATERMARK ("AUG")
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

    // Layout Padding & Dimensions
    val topPadding = 18f * density
    val bottomPadding = 16f * density
    val sidePadding = 20f * density
    val usableWidth = w - (sidePadding * 2)

    // Grid Setup
    val gridTopY = topPadding
    val availableGridHeight = h - gridTopY - bottomPadding
    val totalGridRows = dateRowsNeeded + 1
    val rowHeight = availableGridHeight / totalGridRows
    val colWidth = usableWidth / 7f

    // 2. Day-of-Week Headers
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

    // 3. Days Grid
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

        // Highlight Current Day Box
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

        // Draw Date Text
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

    // 1. Calculate Card Bounds (Responsive fills canvas; Fixed centers 2:1 aspect ratio box)
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

    // Dynamic contrast for active date badge
    val r = ((accentColorInt shr 16) and 0xFF) / 255f
    val g = ((accentColorInt shr 8) and 0xFF) / 255f
    val b = (accentColorInt and 0xFF) / 255f
    val accentLuminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
    val activeTextColor = if (accentLuminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

    // Proportional Layout Calculations
    val padX = cardRect.width() * 0.06f
    val padY = cardRect.height() * 0.08f
    val leftW = cardRect.width() * 0.36f

    // Grid Geometry
    val gridLeft = cardRect.left + leftW + (cardRect.width() * 0.02f)
    val gridW = cardRect.right - gridLeft - padX
    val gridTop = cardRect.top + padY
    val gridH = cardRect.height() - (padY * 2f)

    val colW = gridW / 7f
    val totalGridRows = 6f
    val rowH = gridH / totalGridRows

    val fontScale = minOf(colW * 0.45f, rowH * 0.48f).coerceAtLeast(8f * density)

    fun getRowBaseline(row: Int): Float = gridTop + (row * rowH) + (rowH * 0.62f)

    // =========================================================================
    // RIGHT SIDE: MONTH GRID VIEW
    // =========================================================================
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

    // Row 0: Headers (S M T W T F S)
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

    // Rows 1..5: Dates Grid
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

    // =========================================================================
    // LEFT SIDE: TIGHT HEADER + GRID-ALIGNED GIANT DATE
    // =========================================================================
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

    // 1. MONTH NAME
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

    // 2. WEEKDAY
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

    // 3. GIANT DATE
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

    // 1. Calculate Card Bounds
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

    // Draw Left Container Background
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    // 2. Right Accent Block (~36% Width)
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

    // Clip right accent block to outer card rounded corners
    canvas.save()
    val clipPath = Path().apply {
        addRoundRect(cardRect, cardRadius, cardRadius, Path.Direction.CW)
    }
    canvas.clipPath(clipPath)
    canvas.drawRect(rightBlockRect, rightBgPaint)
    canvas.restore()

    // Dynamic contrast for giant date text
    val r = ((accentColorInt shr 16) and 0xFF) / 255f
    val g = ((accentColorInt shr 8) and 0xFF) / 255f
    val b = (accentColorInt and 0xFF) / 255f
    val accentLuminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
    val giantDateTextColor = if (accentLuminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

    // 3. Colors for Left Section
    val primaryLeftText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryLeftText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val dividerColor = if (isLight) Color.parseColor("#1F000000") else Color.parseColor("#26FFFFFF")

    // 4. Day Name Calculations
    val cal = java.util.Calendar.getInstance()
    val dayFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.ENGLISH)

    val todayName = dayFormat.format(cal.time)

    cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
    val prevDayName = dayFormat.format(cal.time)

    cal.add(java.util.Calendar.DAY_OF_YEAR, 2)
    val nextDayName = dayFormat.format(cal.time)

    // 5. Left Timeline Section
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

    // Row 1: Yesterday
    val fmInactive = inactiveTextPaint.fontMetrics
    val textY1 = row1CenterY - ((fmInactive.descent + fmInactive.ascent) / 2f)
    canvas.drawText(prevDayName, lineLeftX, textY1, inactiveTextPaint)

    // Row 2: Today
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

    // Row 3: Tomorrow
    val textY3 = row3CenterY - ((fmInactive.descent + fmInactive.ascent) / 2f)
    canvas.drawText(nextDayName, lineLeftX, textY3, inactiveTextPaint)

    // 6. Right Side Block: Giant Date Number
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

    // 1. Calculate Card Bounds
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

    // Dynamic contrast for text inside active pill
    val r = ((accentColorInt shr 16) and 0xFF) / 255f
    val g = ((accentColorInt shr 8) and 0xFF) / 255f
    val b = (accentColorInt and 0xFF) / 255f
    val accentLuminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
    val activeTextColor = if (accentLuminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

    val padX = cardRect.width() * 0.07f
    val padY = cardRect.height() * 0.10f

    // =========================================================================
    // LEFT SIDE: MINIMAL ANALOG CLOCK
    // =========================================================================
    val clockDiameter = (cardRect.height() - (padY * 2f)).coerceAtMost(cardRect.width() * 0.32f)
    val clockCx = cardRect.left + padX + (clockDiameter / 2f)
    val clockCy = cardRect.centerY()
    val clockRadius = clockDiameter / 2f

    // Clock Dial Ring
    val clockRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.STROKE
        strokeWidth = (clockDiameter * 0.035f).coerceAtLeast(1.5f * density)
    }
    canvas.drawCircle(clockCx, clockCy, clockRadius - (clockRingPaint.strokeWidth / 2f), clockRingPaint)

    // Get current time including milliseconds for smooth sweeping hands
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

    // Hour Hand
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

    // Minute Hand
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

    // Second Hand (Accent Colored)
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

    // Center Cap Dot
    val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        style = Paint.Style.FILL
    }
    canvas.drawCircle(clockCx, clockCy, clockDiameter * 0.04f, capPaint)

    // =========================================================================
    // RIGHT SIDE: 3-ROW TIMELINE STRIP
    // =========================================================================
    val timelineLeft = clockCx + clockRadius + (cardRect.width() * 0.06f)
    val timelineRight = cardRect.right - padX
    val timelineW = timelineRight - timelineLeft
    val timelineTop = cardRect.top + padY
    val timelineH = cardRect.height() - (padY * 2f)
    val rowH = timelineH / 3f

    // Dates & Day Names (Yesterday, Today, Tomorrow)
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

    // Row 2 Active Pill Background (Today)
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

    // Text Formatting & Measurement
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

    // Helper to render day name (Left) & date number (Right)
    fun drawTimelineRow(row: Int, dayText: String, dateText: String, isToday: Boolean) {
        val centerY = timelineTop + (row * rowH) + (rowH / 2f)
        val paint = if (isToday) activePaint else inactivePaint

        val fm = paint.fontMetrics
        val textY = centerY - ((fm.descent + fm.ascent) / 2f)

        val leftX = timelineLeft + sidePad
        val rightX = timelineRight - sidePad

        // Draw Day Name (Left Aligned)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(dayText, leftX, textY, paint)

        // Draw Date Number (Right Aligned)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(dateText, rightX, textY, paint)
    }

    // Render Rows 0, 1, 2
    drawTimelineRow(0, prevDay, prevNum, isToday = false)
    drawTimelineRow(1, todayDay, todayNum, isToday = true)
    drawTimelineRow(2, nextDay, nextNum, isToday = false)

    return bitmap
}