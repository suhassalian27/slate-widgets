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