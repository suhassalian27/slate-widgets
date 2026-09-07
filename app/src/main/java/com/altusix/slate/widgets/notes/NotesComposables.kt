package com.altusix.slate.widgets.notes

import android.content.Context
import android.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.createSupersampledCanvas
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius

// =========================================================================
// CANVAS BITMAP GENERATORS FOR SLATE "NOTES" WIDGETS
// =========================================================================

/**
 * Helper to wrap and draw multi-line text cleanly within bounds,
 * supporting both explicit maxLines and dynamic bottomLimit clamping.
 */
private fun drawWrappedText(
    canvas: Canvas,
    text: String,
    x: Float,
    startY: Float,
    maxWidth: Float,
    paint: Paint,
    lineSpacing: Float,
    maxLines: Int = Int.MAX_VALUE,
    bottomLimit: Float = Float.MAX_VALUE
): Float {
    if (text.isBlank() || maxLines <= 0) return startY
    val lines = text.split("\n")
    var currentY = startY
    var linesDrawn = 0

    for (rawIndex in lines.indices) {
        if (linesDrawn >= maxLines || currentY > bottomLimit) break

        val rawLine = lines[rawIndex]
        val isLastRawLine = rawIndex == lines.size - 1

        val words = if (rawLine.isEmpty()) listOf("") else rawLine.split(" ")
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"

            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    val isFinalLine = (linesDrawn == maxLines - 1) || (currentY + lineSpacing > bottomLimit)
                    if (isFinalLine) {
                        var truncated = currentLine
                        while (truncated.isNotEmpty() && paint.measureText("$truncated…") > maxWidth) {
                            truncated = truncated.dropLast(1)
                        }
                        canvas.drawText("$truncated…", x, currentY, paint)
                        linesDrawn++
                        return currentY + lineSpacing
                    } else {
                        canvas.drawText(currentLine, x, currentY, paint)
                        currentY += lineSpacing
                        linesDrawn++
                    }
                }
                currentLine = word
            }
        }

        if (currentLine.isNotEmpty()) {
            val isFinalLine = (linesDrawn == maxLines - 1 && !isLastRawLine) || (currentY + lineSpacing > bottomLimit && !isLastRawLine)
            if (isFinalLine) {
                var truncated = currentLine
                while (truncated.isNotEmpty() && paint.measureText("$truncated…") > maxWidth) {
                    truncated = truncated.dropLast(1)
                }
                canvas.drawText("$truncated…", x, currentY, paint)
                linesDrawn++
                return currentY + lineSpacing
            } else {
                canvas.drawText(currentLine, x, currentY, paint)
            }
            currentY += lineSpacing
            linesDrawn++
        } else if (rawLine.isEmpty()) {
            if (currentY + lineSpacing > bottomLimit || linesDrawn >= maxLines) return currentY
            currentY += lineSpacing
            linesDrawn++
        }
    }
    return currentY
}

/**
 * Helper to draw an interactive checkbox at (cx, cy)
 */
private fun drawCheckbox(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    radius: Float,
    isDone: Boolean,
    accentColor: Int,
    borderColor: Int,
    scaleFactor: Float
) {
    val boxRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
    val corner = radius * 0.45f

    if (isDone) {
        // Filled accent background
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(boxRect, corner, corner, fillPaint)

        // White checkmark glyph
        val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
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
        // Outlined empty box
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 1.8f * scaleFactor
        }
        canvas.drawRoundRect(boxRect, corner, corner, strokePaint)
    }
}

// =========================================================================
// 1. SKEUOMORPHIC STICKY NOTE (2x2 - DOG-EAR CORNER FOLD)
// =========================================================================
fun generateStickyNoteBitmap(
    context: Context,
    note: SlateNoteData,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"
    val secondaryTextColor = if (isLight) Color(0xFF4A4A4E).toArgb() else Color(0xFFD1D1D6).toArgb()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val cardCornerRadius = 8f * scaleFactor

    // 1. Fixed Proportional Dog-Ear Fold
    val foldSize = (26f * scaleFactor).coerceAtMost(minOf(cardRect.width(), cardRect.height()) * 0.22f)

    // Main Paper Body (Cut diagonally at top-right corner)
    val paperPath = Path().apply {
        moveTo(cardRect.left + cardCornerRadius, cardRect.top)
        lineTo(cardRect.right - foldSize, cardRect.top)
        lineTo(cardRect.right, cardRect.top + foldSize)
        lineTo(cardRect.right, cardRect.bottom - cardCornerRadius)
        quadTo(cardRect.right, cardRect.bottom, cardRect.right - cardCornerRadius, cardRect.bottom)
        lineTo(cardRect.left + cardCornerRadius, cardRect.bottom)
        quadTo(cardRect.left, cardRect.bottom, cardRect.left, cardRect.bottom - cardCornerRadius)
        lineTo(cardRect.left, cardRect.top + cardCornerRadius)
        quadTo(cardRect.left, cardRect.top, cardRect.left + cardCornerRadius, cardRect.top)
        close()
    }

    val paperPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawPath(paperPath, paperPaint)

    // Fold Crease Line
    val creasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0x20000000).toArgb() else Color(0x30000000).toArgb()
        strokeWidth = 1.0f * scaleFactor
    }
    canvas.drawLine(cardRect.right - foldSize, cardRect.top, cardRect.right, cardRect.top + foldSize, creasePaint)

    // Dog-Ear Fold Flap
    val flapPath = Path().apply {
        moveTo(cardRect.right - foldSize, cardRect.top)
        lineTo(cardRect.right - foldSize, cardRect.top + foldSize)
        lineTo(cardRect.right, cardRect.top + foldSize)
        close()
    }

    val flapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            cardRect.right - foldSize, cardRect.top,
            cardRect.right, cardRect.top + foldSize,
            intArrayOf(
                if (isLight) Color(0xFFF2ECE1).toArgb() else Color(0xFF34343A).toArgb(),
                if (isLight) Color(0xFFDFD6C6).toArgb() else Color(0xFF222226).toArgb()
            ),
            null,
            Shader.TileMode.CLAMP
        )
        style = Paint.Style.FILL
    }
    canvas.drawPath(flapPath, flapPaint)

    val flapStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0x20000000).toArgb() else Color(0x28FFFFFF).toArgb()
        style = Paint.Style.STROKE
        strokeWidth = 1.0f * scaleFactor
    }
    canvas.drawPath(flapPath, flapStroke)

    // 2. Fixed Dimension Padding (Independent of widget width/height changes)
    val padX = 20f * scaleFactor
    val padTop = 20f * scaleFactor
    val padBottom = 14f * scaleFactor
    val titleToBodyGap = 10f * scaleFactor

    val fScale = note.fontScaleMultiplier

    // 3. Accent Note Title
    val titleTextSize = 16f * scaleFactor * fScale
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = titleTextSize
        typeface = getSlateFont(context, 700)
    }

    val maxTitleW = cardRect.width() - (padX * 2f) - foldSize
    val displayTitle = if (titlePaint.measureText(note.title) > maxTitleW) {
        var t = note.title
        while (t.isNotEmpty() && titlePaint.measureText("$t…") > maxTitleW) t = t.dropLast(1)
        "$t…"
    } else note.title

    val titleY = cardRect.top + padTop + (titleTextSize * 0.85f)
    canvas.drawText(displayTitle, cardRect.left + padX, titleY, titlePaint)

    // 4. Body Text Flow
    val bodyTextSize = 13.5f * scaleFactor * fScale
    val contentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = bodyTextSize
        typeface = getSlateFont(context, 400)
    }

    val lineSpacing = bodyTextSize * 1.42f
    val startY = titleY + titleToBodyGap + (bodyTextSize * 0.85f)
    val bottomLimit = cardRect.bottom - padBottom

    drawWrappedText(
        canvas = canvas,
        text = note.content,
        x = cardRect.left + padX,
        startY = startY,
        maxWidth = cardRect.width() - (padX * 2f),
        paint = contentPaint,
        lineSpacing = lineSpacing,
        bottomLimit = bottomLimit
    )

    return bitmap
}

// =========================================================================
// 1b. DESK MEMO PAD (4x2 - HORIZONTAL RULED NOTEPAD)
// =========================================================================
fun generateDeskMemoBitmap(
    context: Context,
    note: SlateNoteData,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"
    val primaryTextColor = if (isLight) Color(0xFF141416).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF6C6C70).toArgb() else Color(0xFF8E8E93).toArgb()

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val aspect = 2f
        val cardW = minOf(w, h * aspect)
        val cardH = cardW / aspect
        RectF((w - cardW) / 2f, (h - cardH) / 2f, (w + cardW) / 2f, (h + cardH) / 2f)
    }

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val padX = 18f * scaleFactor
    val fScale = note.fontScaleMultiplier

    // 1. Frosted Adhesive Tape Strip
    val tapeW = (cardRect.width() * 0.22f).coerceIn(60f * scaleFactor, 110f * scaleFactor)
    val tapeH = 10f * scaleFactor
    val tapeTop = cardRect.top + 2f * scaleFactor
    val tapeBottom = tapeTop + tapeH
    val tapeRect = RectF(
        cardRect.centerX() - tapeW / 2f,
        tapeTop,
        cardRect.centerX() + tapeW / 2f,
        tapeBottom
    )
    val tapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0x35000000).toArgb() else Color(0x28FFFFFF).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(tapeRect, 3f * scaleFactor, 3f * scaleFactor, tapePaint)

    // 2. Dynamic Title Clearance & Baseline Anchor
    val titleTextSize = 15.5f * scaleFactor * fScale
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = titleTextSize
        typeface = getSlateFont(context, 700)
    }

    val catPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = (10f * scaleFactor * fScale).coerceIn(9f * scaleFactor, 14f * scaleFactor)
        typeface = getSlateFont(context, 700)
        textAlign = Paint.Align.RIGHT
    }

// Top cap-height stays locked at a comfortable distance below tape & card top
    val titleTopClearance = maxOf(tapeBottom + (5f * scaleFactor), cardRect.top + (16f * scaleFactor))
    val titleMetrics = titlePaint.fontMetrics
    val titleY = titleTopClearance - titleMetrics.ascent // ascent is negative

// Category tag on the right
    val catText = note.category.uppercase()
    val catWidth = catPaint.measureText(catText)
    canvas.drawText(catText, cardRect.right - padX, titleY, catPaint)

// Pencil icon paint & measurements (placed first)
    val editIconSize = 11.5f * scaleFactor * fScale
    val editPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = editIconSize
        typeface = getSlateFont(context, 700)
    }
    val editIconText = "✎"
    val editIconW = editPaint.measureText(editIconText)
    val editGap = 6f * scaleFactor

// Draw pencil icon before the title
    val pencilX = cardRect.left + padX
    canvas.drawText(editIconText, pencilX, titleY - 1.5f * scaleFactor, editPaint)

// Calculate remaining width and draw the title
    val titleStartX = pencilX + editIconW + editGap
    val maxTitleW = cardRect.right - padX - catWidth - (12f * scaleFactor) - titleStartX
    val displayTitle = if (titlePaint.measureText(note.title) > maxTitleW) {
        var t = note.title
        while (t.isNotEmpty() && titlePaint.measureText("$t…") > maxTitleW) t = t.dropLast(1)
        "$t…"
    } else note.title

    canvas.drawText(displayTitle, titleStartX, titleY, titlePaint)

    // 3. Dynamic Ruled Horizontal Lines
    // Starts below the lowest descending character (g, j, p, q, y)
    val lineStartY = titleY + maxOf(titleMetrics.descent + (4f * scaleFactor), 10f * scaleFactor)
    val lineSpacing = 21.5f * scaleFactor * fScale
    val bottomLimit = cardRect.bottom - (12f * scaleFactor)
    val availableHeight = (bottomLimit - lineStartY).coerceAtLeast(0f)
    val numLines = (availableHeight / lineSpacing).toInt().coerceAtLeast(2)

    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFFE5E5EA).toArgb() else Color(0x18FFFFFF).toArgb()
        strokeWidth = 1f * scaleFactor
    }

    for (i in 0 until numLines) {
        val lineY = lineStartY + i * lineSpacing
        canvas.drawLine(cardRect.left + padX, lineY, cardRect.right - padX, lineY, linePaint)
    }

    // 4. Note Content Typography
    val bodyTextSize = 12.5f * scaleFactor * fScale
    val contentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF2C2C2E).toArgb() else Color(0xFFD1D1D6).toArgb()
        textSize = bodyTextSize
        typeface = getSlateFont(context, 400)
    }

    drawWrappedText(
        canvas = canvas,
        text = note.content,
        x = cardRect.left + padX,
        startY = lineStartY + (lineSpacing * 0.72f),
        maxWidth = cardRect.width() - (padX * 2f),
        paint = contentPaint,
        lineSpacing = lineSpacing,
        maxLines = numLines,
        bottomLimit = bottomLimit
    )

    return bitmap
}

// =========================================================================
// 2. INTERACTIVE CHECKLIST (4x2)
// =========================================================================
fun generateChecklistBitmap(
    context: Context,
    note: SlateNoteData,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"
    val primaryTextColor = if (isLight) Color(0xFF141416).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF6C6C70).toArgb() else Color(0xFF8E8E93).toArgb()

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val aspect = 2f
        val cardW = minOf(w, h * aspect)
        val cardH = cardW / aspect
        RectF((w - cardW) / 2f, (h - cardH) / 2f, (w + cardW) / 2f, (h + cardH) / 2f)
    }

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = cardRect.height() * 0.10f

    // 1. Header (Title + Progress Badge + Add Button)
    val headerY = cardRect.top + pad * 1.5f
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardRect.height() * 0.125f
        typeface = getSlateFont(context, 700)
    }
    canvas.drawText(note.title.take(22), cardRect.left + pad, headerY, titlePaint)

    // Progress Pill Badge ("3/5 Done")
    val badgeText = "${note.completedCount}/${note.totalCount} Done"
    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = cardRect.height() * 0.082f
        typeface = getSlateFont(context, 700)
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText(badgeText, cardRect.right - pad * 2.2f, headerY, badgePaint)

    // Add / Edit Button icon at top right
    val plusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = cardRect.height() * 0.11f
        typeface = getSlateFont(context, 700)
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText("+", cardRect.right - pad, headerY, plusPaint)

    // 2. Checklist Rows (Up to 5 rows)
    val rowStartY = headerY + pad * 0.9f
    val rowH = (cardRect.bottom - pad * 0.8f - rowStartY) / 5f
    val checkR = rowH * 0.28f

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = rowH * 0.52f
        typeface = getSlateFont(context, 500)
    }

    val maxItemCount = minOf(5, note.items.size)
    for (i in 0 until 5) {
        val cy = rowStartY + i * rowH + rowH / 2f
        val cx = cardRect.left + pad + checkR

        if (i < maxItemCount) {
            val item = note.items[i]
            drawCheckbox(
                canvas = canvas,
                cx = cx,
                cy = cy,
                radius = checkR,
                isDone = item.isDone,
                accentColor = accentColor,
                borderColor = secondaryTextColor,
                scaleFactor = scaleFactor
            )

            // Task Text
            val textLeft = cx + checkR + pad * 0.8f
            val maxTextW = cardRect.width() - textLeft - pad

            textPaint.color = if (item.isDone) secondaryTextColor else primaryTextColor
            val displayText = if (textPaint.measureText(item.text) > maxTextW) {
                var t = item.text
                while (t.isNotEmpty() && textPaint.measureText("$t…") > maxTextW) t = t.dropLast(1)
                "$t…"
            } else item.text

            val textBaseY = cy + rowH * 0.18f
            canvas.drawText(displayText, textLeft, textBaseY, textPaint)

            // Strikethrough line if done
            if (item.isDone) {
                val measuredW = textPaint.measureText(displayText)
                val strikePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = secondaryTextColor
                    strokeWidth = 1.5f * scaleFactor
                }
                canvas.drawLine(textLeft, cy, textLeft + measuredW, cy, strikePaint)
            }
        } else {
            // Empty placeholder row
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isLight) Color(0x30000000).toArgb() else Color(0x20FFFFFF).toArgb()
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx, cy, 3f * scaleFactor, dotPaint)
            val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isLight) Color(0x40000000).toArgb() else Color(0x25FFFFFF).toArgb()
                textSize = rowH * 0.44f
                typeface = getSlateFont(context, 400)
            }
            canvas.drawText("Tap to add item...", cx + checkR + pad * 0.8f, cy + rowH * 0.15f, placeholderPaint)
        }
    }

    return bitmap
}

// =========================================================================
// 3. INTERACTIVE CHECKLIST (2x2)
// =========================================================================
fun generateChecklist2x2Bitmap(
    context: Context,
    note: SlateNoteData,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"
    val primaryTextColor = if (isLight) Color(0xFF141416).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF6C6C70).toArgb() else Color(0xFF8E8E93).toArgb()

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = cardRect.width() * 0.09f

    // 1. Header (Title + Progress Count)
    val headerY = cardRect.top + pad * 1.8f
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardRect.height() * 0.095f
        typeface = getSlateFont(context, 700)
    }
    canvas.drawText(note.title.take(16), cardRect.left + pad, headerY, titlePaint)

    val countBadge = "${note.completedCount}/${note.totalCount}"
    val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = cardRect.height() * 0.082f
        typeface = getSlateFont(context, 700)
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText(countBadge, cardRect.right - pad, headerY, countPaint)

    // 2. Checklist Rows (3 prominent rows)
    val rowStartY = headerY + pad * 0.8f
    val rowH = (cardRect.bottom - pad - rowStartY) / 3f
    val checkR = rowH * 0.28f

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = rowH * 0.44f
        typeface = getSlateFont(context, 500)
    }

    for (i in 0 until 3) {
        val cy = rowStartY + i * rowH + rowH / 2f
        val cx = cardRect.left + pad + checkR

        if (i < note.items.size) {
            val item = note.items[i]
            drawCheckbox(
                canvas = canvas,
                cx = cx,
                cy = cy,
                radius = checkR,
                isDone = item.isDone,
                accentColor = accentColor,
                borderColor = secondaryTextColor,
                scaleFactor = scaleFactor
            )

            val textLeft = cx + checkR + pad * 0.8f
            val maxTextW = cardRect.width() - textLeft - pad

            textPaint.color = if (item.isDone) secondaryTextColor else primaryTextColor
            val displayText = if (textPaint.measureText(item.text) > maxTextW) {
                var t = item.text
                while (t.isNotEmpty() && textPaint.measureText("$t…") > maxTextW) t = t.dropLast(1)
                "$t…"
            } else item.text

            val textBaseY = cy + rowH * 0.16f
            canvas.drawText(displayText, textLeft, textBaseY, textPaint)

            if (item.isDone) {
                val measuredW = textPaint.measureText(displayText)
                val strikePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = secondaryTextColor
                    strokeWidth = 1.5f * scaleFactor
                }
                canvas.drawLine(textLeft, cy, textLeft + measuredW, cy, strikePaint)
            }
        }
    }

    return bitmap
}

// =========================================================================
// 4. CLASSIC RULED LEGAL PAD (4x2)
// =========================================================================
fun generateLegalPadBitmap(
    context: Context,
    note: SlateNoteData,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"
    val primaryTextColor = if (isLight) Color(0xFF141416).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF6C6C70).toArgb() else Color(0xFF8E8E93).toArgb()

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val aspect = 2f
        val cardW = minOf(w, h * aspect)
        val cardH = cardW / aspect
        RectF((w - cardW) / 2f, (h - cardH) / 2f, (w + cardW) / 2f, (h + cardH) / 2f)
    }

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    // 1. Top Binding Header Strip (Slate Notebook Binding)
    val bindH = cardRect.height() * 0.16f
    val bindPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF2C2C2E).toArgb() else Color(0xFF1C1C1E).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(
        RectF(cardRect.left, cardRect.top, cardRect.right, cardRect.top + bindH),
        cardCornerRadius, cardCornerRadius, bindPaint
    )
    canvas.drawRect(cardRect.left, cardRect.top + bindH - cardCornerRadius, cardRect.right, cardRect.top + bindH, bindPaint)

    // Gold / accent stitching line across binding
    val stitchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        strokeWidth = 1.6f * scaleFactor
        pathEffect = DashPathEffect(floatArrayOf(6f * scaleFactor, 6f * scaleFactor), 0f)
    }
    canvas.drawLine(cardRect.left + 16f * scaleFactor, cardRect.top + bindH * 0.5f, cardRect.right - 16f * scaleFactor, cardRect.top + bindH * 0.5f, stitchPaint)

    val pad = cardRect.height() * 0.10f

    // 2. Red Vertical Margin Line (at ~20% of width)
    val marginX = cardRect.left + cardRect.width() * 0.18f
    val redMarginPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color(0x60E53935).toArgb()
        strokeWidth = 1.5f * scaleFactor
    }
    canvas.drawLine(marginX, cardRect.top + bindH, marginX, cardRect.bottom, redMarginPaint)

    // 3. Horizontal Ruled Lines
    val lineSpacing = cardRect.height() * 0.18f
    val ruledPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFFE5E5EA).toArgb() else Color(0x18FFFFFF).toArgb()
        strokeWidth = 1f * scaleFactor
    }
    for (i in 1..4) {
        val y = cardRect.top + bindH + i * lineSpacing
        canvas.drawLine(cardRect.left, y, cardRect.right, y, ruledPaint)
    }

    // 4. Note Title
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardRect.height() * 0.11f
        typeface = getSlateFont(context, 700)
    }
    canvas.drawText(note.title, marginX + pad * 0.8f, cardRect.top + bindH + lineSpacing * 0.72f, titlePaint)

    // 5. Multi-line Body Content
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF3A3A3C).toArgb() else Color(0xFFD1D1D6).toArgb()
        textSize = cardRect.height() * 0.088f
        typeface = getSlateFont(context, 400)
    }
    drawWrappedText(
        canvas = canvas,
        text = note.content,
        x = marginX + pad * 0.8f,
        startY = cardRect.top + bindH + lineSpacing * 1.72f,
        maxWidth = cardRect.right - marginX - pad * 1.5f,
        paint = bodyPaint,
        lineSpacing = lineSpacing,
        maxLines = 3
    )

    return bitmap
}

// =========================================================================
// 5. QUICK THOUGHT CAPSULE (4x1)
// =========================================================================
fun generateQuickThoughtBitmap(
    context: Context,
    note: SlateNoteData,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"
    val primaryTextColor = if (isLight) Color(0xFF141416).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF6C6C70).toArgb() else Color(0xFF8E8E93).toArgb()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val aspect = 4f
        val cardW = minOf(w, h * aspect)
        val cardH = cardW / aspect
        RectF((w - cardW) / 2f, (h - cardH) / 2f, (w + cardW) / 2f, (h + cardH) / 2f)
    }

    val pillRadius = cardRect.height() / 2f
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, pillRadius, pillRadius, bgPaint)

    val pad = cardRect.height() * 0.18f

    // 1. Tag Icon Badge (Left)
    val badgeR = cardRect.height() * 0.32f
    val badgeCx = cardRect.left + pillRadius
    val badgeCy = cardRect.centerY()
    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(badgeCx, badgeCy, badgeR, badgePaint)

    val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.White.toArgb() else Color.Black.toArgb()
        textSize = badgeR * 0.9f
        textAlign = Paint.Align.CENTER
        typeface = getSlateFont(context, 700)
    }
    canvas.drawText("✦", badgeCx, badgeCy + badgeR * 0.35f, iconPaint)

    // 2. Note Thought Text
    val textLeft = badgeCx + badgeR + pad * 1.1f
    val textRight = cardRect.right - pillRadius * 0.8f
    val maxTextW = (textRight - textLeft).coerceAtLeast(10f)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardRect.height() * 0.26f
        typeface = getSlateFont(context, 700)
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = cardRect.height() * 0.18f
        typeface = getSlateFont(context, 400)
    }

    val titleText = note.title
    val bodyText = note.content.replace("\n", " ")

    val displayTitle = if (titlePaint.measureText(titleText) > maxTextW) {
        var t = titleText
        while (t.isNotEmpty() && titlePaint.measureText("$t…") > maxTextW) t = t.dropLast(1)
        "$t…"
    } else titleText

    val displayBody = if (bodyPaint.measureText(bodyText) > maxTextW) {
        var b = bodyText
        while (b.isNotEmpty() && bodyPaint.measureText("$b…") > maxTextW) b = b.dropLast(1)
        "$b…"
    } else bodyText

    val titleY = cardRect.centerY() - cardRect.height() * 0.05f
    canvas.drawText(displayTitle, textLeft, titleY, titlePaint)
    canvas.drawText(displayBody, textLeft, titleY + cardRect.height() * 0.27f, bodyPaint)

    // Edit icon on the far right
    val editPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = cardRect.height() * 0.24f
        typeface = getSlateFont(context, 700)
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText("✎", cardRect.right - pad * 1.5f, cardRect.centerY() + cardRect.height() * 0.08f, editPaint)

    return bitmap
}

// =========================================================================
// 6. MINI THOUGHT CAPSULE (2x1)
// =========================================================================
fun generateMiniThoughtBitmap(
    context: Context,
    note: SlateNoteData,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"
    val primaryTextColor = if (isLight) Color(0xFF141416).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF6C6C70).toArgb() else Color(0xFF8E8E93).toArgb()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val aspect = 2f
        val cardW = minOf(w, h * aspect)
        val cardH = cardW / aspect
        RectF((w - cardW) / 2f, (h - cardH) / 2f, (w + cardW) / 2f, (h + cardH) / 2f)
    }

    val pillRadius = cardRect.height() / 2f
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, pillRadius, pillRadius, bgPaint)

    val pad = cardRect.height() * 0.16f
    val iconR = cardRect.height() * 0.30f
    val iconCx = cardRect.left + pillRadius
    val iconCy = cardRect.centerY()

    val iconBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(iconCx, iconCy, iconR, iconBgPaint)

    val iconTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.White.toArgb() else Color.Black.toArgb()
        textSize = iconR * 0.9f
        textAlign = Paint.Align.CENTER
        typeface = getSlateFont(context, 700)
    }
    canvas.drawText("✎", iconCx, iconCy + iconR * 0.35f, iconTextPaint)

    val textLeft = iconCx + iconR + pad * 1.1f
    val maxTextW = (cardRect.right - textLeft - pad).coerceAtLeast(10f)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardRect.height() * 0.24f
        typeface = getSlateFont(context, 700)
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = cardRect.height() * 0.17f
        typeface = getSlateFont(context, 400)
    }

    val displayTitle = note.title.take(16)
    val displayBody = note.content.replace("\n", " ").take(22)

    val titleY = cardRect.centerY() - cardRect.height() * 0.05f
    canvas.drawText(displayTitle, textLeft, titleY, titlePaint)
    canvas.drawText(displayBody, textLeft, titleY + cardRect.height() * 0.26f, bodyPaint)

    return bitmap
}

// =========================================================================
// 7. BENTO NOTES & TASKS (4x2)
// =========================================================================
fun generateBentoNoteBitmap(
    context: Context,
    note: SlateNoteData,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"
    val primaryTextColor = if (isLight) Color(0xFF141416).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF6C6C70).toArgb() else Color(0xFF8E8E93).toArgb()

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val aspect = 2f
        val cardW = minOf(w, h * aspect)
        val cardH = cardW / aspect
        RectF((w - cardW) / 2f, (h - cardH) / 2f, (w + cardW) / 2f, (h + cardH) / 2f)
    }

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = cardRect.height() * 0.08f
    val innerCornerRadius = cardCornerRadius * 0.65f

    // Bento Left Cell: Hero Note Card (48% width)
    val leftW = (cardRect.width() - pad * 3f) * 0.48f
    val leftRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.left + pad + leftW, cardRect.bottom - pad)
    val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFFF2F2F7).toArgb() else Color(0xFF18181B).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(leftRect, innerCornerRadius, innerCornerRadius, cellPaint)

    // Left Cell Content
    val leftPad = leftRect.height() * 0.12f
    val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = leftRect.height() * 0.085f
        typeface = getSlateFont(context, 700)
    }
    canvas.drawText("HERO NOTE", leftRect.left + leftPad, leftRect.top + leftPad * 1.6f, tagPaint)

    val heroTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = leftRect.height() * 0.14f
        typeface = getSlateFont(context, 700)
    }
    val titleY = leftRect.top + leftPad * 2.8f
    canvas.drawText(note.title.take(16), leftRect.left + leftPad, titleY, heroTitlePaint)

    val heroContentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = leftRect.height() * 0.095f
        typeface = getSlateFont(context, 400)
    }
    drawWrappedText(
        canvas = canvas,
        text = note.content,
        x = leftRect.left + leftPad,
        startY = titleY + leftPad * 1.3f,
        maxWidth = leftRect.width() - leftPad * 2f,
        paint = heroContentPaint,
        lineSpacing = leftRect.height() * 0.14f,
        maxLines = 3
    )

    // Bento Right Cell: Interactive Tasks (48% width)
    val rightRect = RectF(leftRect.right + pad, cardRect.top + pad, cardRect.right - pad, cardRect.bottom - pad)
    canvas.drawRoundRect(rightRect, innerCornerRadius, innerCornerRadius, cellPaint)

    // Right Cell Content
    val rightPad = rightRect.height() * 0.12f
    val taskHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = rightRect.height() * 0.12f
        typeface = getSlateFont(context, 700)
    }
    canvas.drawText("Tasks", rightRect.left + rightPad, rightRect.top + rightPad * 1.6f, taskHeaderPaint)

    val progressText = "${note.completedCount}/${note.totalCount}"
    val progPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = rightRect.height() * 0.095f
        typeface = getSlateFont(context, 700)
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText(progressText, rightRect.right - rightPad, rightRect.top + rightPad * 1.6f, progPaint)

    // Draw up to 3 task rows on right side
    val rowStartY = rightRect.top + rightPad * 2.4f
    val rowH = (rightRect.bottom - rightPad - rowStartY) / 3f
    val checkR = rowH * 0.28f

    val itemTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = rowH * 0.46f
        typeface = getSlateFont(context, 500)
    }

    for (i in 0 until minOf(3, note.items.size)) {
        val item = note.items[i]
        val cy = rowStartY + i * rowH + rowH / 2f
        val cx = rightRect.left + rightPad + checkR

        drawCheckbox(
            canvas = canvas,
            cx = cx,
            cy = cy,
            radius = checkR,
            isDone = item.isDone,
            accentColor = accentColor,
            borderColor = secondaryTextColor,
            scaleFactor = scaleFactor
        )

        itemTextPaint.color = if (item.isDone) secondaryTextColor else primaryTextColor
        val textLeft = cx + checkR + pad * 0.7f
        val maxW = rightRect.right - textLeft - rightPad
        val displayText = if (itemTextPaint.measureText(item.text) > maxW) {
            var t = item.text
            while (t.isNotEmpty() && itemTextPaint.measureText("$t…") > maxW) t = t.dropLast(1)
            "$t…"
        } else item.text

        canvas.drawText(displayText, textLeft, cy + rowH * 0.16f, itemTextPaint)
        if (item.isDone) {
            val mw = itemTextPaint.measureText(displayText)
            val strikePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = secondaryTextColor
                strokeWidth = 1.5f * scaleFactor
            }
            canvas.drawLine(textLeft, cy, textLeft + mw, cy, strikePaint)
        }
    }

    return bitmap
}

// =========================================================================
// 8. TORN PERFORATED RECEIPT (2x2)
// =========================================================================
fun generateTornReceiptBitmap(
    context: Context,
    note: SlateNoteData,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"
    val primaryTextColor = if (isLight) Color(0xFF141416).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF6C6C70).toArgb() else Color(0xFF8E8E93).toArgb()

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = cardRect.width() * 0.08f

    // 1. Perforation Dots across the top
    val perfY = cardRect.top + pad * 1.2f
    val perfPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFFD1D1D6).toArgb() else Color(0x30FFFFFF).toArgb()
        style = Paint.Style.FILL
    }
    val dotCount = 12
    val dotSpacing = (cardRect.width() - pad * 2f) / dotCount
    for (i in 0..dotCount) {
        val dx = cardRect.left + pad + i * dotSpacing
        canvas.drawCircle(dx, perfY, 2.5f * scaleFactor, perfPaint)
    }

    // 2. Receipt Header Tag
    val headerY = perfY + pad * 1.5f
    val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = cardRect.height() * 0.052f
        typeface = getSlateFont(context, 700)
    }
    canvas.drawText("SLATE LOG / RECEIPT", cardRect.left + pad, headerY, tagPaint)

    // 3. Receipt Title
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardRect.height() * 0.085f
        typeface = getSlateFont(context, 700)
    }
    val titleY = headerY + cardRect.height() * 0.10f
    canvas.drawText(note.title.uppercase(), cardRect.left + pad, titleY, titlePaint)

    // Divider
    val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFFE5E5EA).toArgb() else Color(0x20FFFFFF).toArgb()
        strokeWidth = 1.2f * scaleFactor
        pathEffect = DashPathEffect(floatArrayOf(4f * scaleFactor, 4f * scaleFactor), 0f)
    }
    val divY = titleY + pad * 0.6f
    canvas.drawLine(cardRect.left + pad, divY, cardRect.right - pad, divY, divPaint)

    // 4. Content (Monospace receipt formatting)
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF2C2C2E).toArgb() else Color(0xFFD1D1D6).toArgb()
        textSize = cardRect.height() * 0.062f
        typeface = Typeface.MONOSPACE
    }
    drawWrappedText(
        canvas = canvas,
        text = note.content,
        x = cardRect.left + pad,
        startY = divY + pad * 1.1f,
        maxWidth = cardRect.width() - pad * 2f,
        paint = bodyPaint,
        lineSpacing = cardRect.height() * 0.095f,
        maxLines = 4
    )

    // 5. Stylized Barcode at Bottom
    val barcodeY = cardRect.bottom - pad * 1.6f
    val barcodeH = cardRect.height() * 0.10f
    val barcodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF141416).toArgb() else Color(0x90FFFFFF).toArgb()
        style = Paint.Style.FILL
    }

    val barWidths = intArrayOf(2, 4, 1, 3, 2, 5, 2, 1, 4, 2, 3, 1, 4, 2, 3, 1, 5, 2)
    var currBarX = cardRect.left + pad
    val maxBarcodeX = cardRect.right - pad
    for (bw in barWidths) {
        val barW = bw * scaleFactor * 1.2f
        if (currBarX + barW > maxBarcodeX) break
        canvas.drawRect(currBarX, barcodeY, currBarX + barW, barcodeY + barcodeH, barcodePaint)
        currBarX += barW + 3f * scaleFactor
    }

    return bitmap
}

// =========================================================================
// 9. DOT GRID SCRATCHPAD (2x2)
// =========================================================================
fun generateDotGridBitmap(
    context: Context,
    note: SlateNoteData,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"
    val primaryTextColor = if (isLight) Color(0xFF141416).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF6C6C70).toArgb() else Color(0xFF8E8E93).toArgb()

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    // 1. Dot Grid Matrix Background
    val dotGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0x1F000000).toArgb() else Color(0x18FFFFFF).toArgb()
        style = Paint.Style.FILL
    }
    val gridSpacing = 20f * scaleFactor
    var gy = cardRect.top + gridSpacing
    while (gy < cardRect.bottom - gridSpacing / 2f) {
        var gx = cardRect.left + gridSpacing
        while (gx < cardRect.right - gridSpacing / 2f) {
            canvas.drawCircle(gx, gy, 1.4f * scaleFactor, dotGridPaint)
            gx += gridSpacing
        }
        gy += gridSpacing
    }

    val pad = cardRect.width() * 0.09f

    // 2. Title
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardRect.height() * 0.092f
        typeface = getSlateFont(context, 700)
    }
    val titleY = cardRect.top + pad * 1.8f
    canvas.drawText(note.title, cardRect.left + pad, titleY, titlePaint)

    // Accent line below title
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        strokeWidth = 2f * scaleFactor
    }
    val lineY = titleY + pad * 0.5f
    canvas.drawLine(cardRect.left + pad, lineY, cardRect.left + pad + 32f * scaleFactor, lineY, linePaint)

    // 3. Body Text
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF2C2C2E).toArgb() else Color(0xFFD1D1D6).toArgb()
        textSize = cardRect.height() * 0.068f
        typeface = getSlateFont(context, 400)
    }
    drawWrappedText(
        canvas = canvas,
        text = note.content,
        x = cardRect.left + pad,
        startY = lineY + pad * 1.3f,
        maxWidth = cardRect.width() - pad * 2f,
        paint = bodyPaint,
        lineSpacing = cardRect.height() * 0.115f,
        maxLines = 5
    )

    // 4. Dot Grid Badge in Corner
    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = cardRect.height() * 0.052f
        typeface = getSlateFont(context, 700)
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText("5MM GRID", cardRect.right - pad, cardRect.bottom - pad, badgePaint)

    return bitmap
}

// =========================================================================
// 10. MULTI-NOTE STACK (2x2)
// =========================================================================
fun generateNoteStackBitmap(
    context: Context,
    note: SlateNoteData,
    pageIndex: Int,
    totalPages: Int,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"
    val primaryTextColor = if (isLight) Color(0xFF141416).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF6C6C70).toArgb() else Color(0xFF8E8E93).toArgb()

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    // 1. Stacked Underneath Cards (Physical Layered Effect)
    val underPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFFE5E5EA).toArgb() else Color(0xFF18181B).toArgb()
        style = Paint.Style.FILL
    }
    val offset1 = 6f * scaleFactor
    val underRect1 = RectF(cardRect.left + offset1, cardRect.top - offset1, cardRect.right - offset1, cardRect.bottom - offset1)
    canvas.drawRoundRect(underRect1, cardCornerRadius, cardCornerRadius, underPaint)

    // 2. Top Foreground Card
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = cardRect.width() * 0.09f

    // 3. Title & Category
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardRect.height() * 0.088f
        typeface = getSlateFont(context, 700)
    }
    val titleY = cardRect.top + pad * 1.8f
    canvas.drawText(note.title, cardRect.left + pad, titleY, titlePaint)

    val catPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = cardRect.height() * 0.052f
        typeface = getSlateFont(context, 700)
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText(note.category.uppercase(), cardRect.right - pad, titleY, catPaint)

    // 4. Content
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF2C2C2E).toArgb() else Color(0xFFD1D1D6).toArgb()
        textSize = cardRect.height() * 0.066f
        typeface = getSlateFont(context, 400)
    }
    drawWrappedText(
        canvas = canvas,
        text = note.content,
        x = cardRect.left + pad,
        startY = titleY + pad * 1.1f,
        maxWidth = cardRect.width() - pad * 2f,
        paint = bodyPaint,
        lineSpacing = cardRect.height() * 0.11f,
        maxLines = 4
    )

    // 5. Stack Navigation Deck at Bottom: "<", "1 of 3", ">"
    val navY = cardRect.bottom - pad * 1.1f
    val navTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = cardRect.height() * 0.075f
        typeface = getSlateFont(context, 700)
    }

    // Prev "<"
    canvas.drawText("◀", cardRect.left + pad * 1.2f, navY, navTextPaint)

    // Page count center
    val pageCountText = "${pageIndex + 1} of $totalPages"
    val pagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = cardRect.height() * 0.065f
        typeface = getSlateFont(context, 600)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(pageCountText, cardRect.centerX(), navY, pagePaint)

    // Next ">"
    val nextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = cardRect.height() * 0.075f
        typeface = getSlateFont(context, 700)
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText("▶", cardRect.right - pad * 1.2f, navY, nextPaint)

    return bitmap
}
