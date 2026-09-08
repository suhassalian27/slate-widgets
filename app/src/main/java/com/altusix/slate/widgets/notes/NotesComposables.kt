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

fun createCheckmarkBitmap(context: Context, isDone: Boolean, accentColor: Int, borderColor: Int): Bitmap {
    val sizePx = (20f * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val radius = sizePx / 2f
    val corner = 5f * context.resources.displayMetrics.density

    val boxRect = RectF(1.5f, 1.5f, sizePx - 1.5f, sizePx - 1.5f)
    if (isDone) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(boxRect, corner, corner, fillPaint)

        val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2.2f * context.resources.displayMetrics.density
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = Path().apply {
            moveTo(sizePx * 0.26f, sizePx * 0.50f)
            lineTo(sizePx * 0.45f, sizePx * 0.70f)
            lineTo(sizePx * 0.76f, sizePx * 0.30f)
        }
        canvas.drawPath(path, checkPaint)
    } else {
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 1.8f * context.resources.displayMetrics.density
        }
        canvas.drawRoundRect(boxRect, corner, corner, strokePaint)
    }
    return bitmap
}

fun getTintedVectorBitmap(context: Context, resId: Int, tintColor: Int): Bitmap? {
    val drawable = androidx.core.content.ContextCompat.getDrawable(context, resId) ?: return null
    val density = context.resources.displayMetrics.density
    val size = (16f * density).toInt().coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    drawable.mutate().apply {
        setTint(tintColor)
        setBounds(0, 0, size, size)
        draw(canvas)
    }
    return bmp
}

fun generateCardSurfaceBitmap(
    context: Context,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = androidx.compose.ui.graphics.Color(slateConfig.backgroundColorHex)
        .copy(alpha = slateConfig.opacity).toArgb()
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
    return bitmap
}

fun createCheckmarkBitmap(
    context: Context,
    isDone: Boolean,
    accentColor: Int,
    borderColor: Int,
    fScale: Float = 1.0f
): Bitmap {
    val sizeDp = (20f * fScale).coerceIn(16f, 26f)
    val sizePx = (sizeDp * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val corner = 5f * context.resources.displayMetrics.density

    val boxRect = RectF(1.5f, 1.5f, sizePx - 1.5f, sizePx - 1.5f)
    if (isDone) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(boxRect, corner, corner, fillPaint)

        val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2.2f * context.resources.displayMetrics.density
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = Path().apply {
            moveTo(sizePx * 0.26f, sizePx * 0.50f)
            lineTo(sizePx * 0.45f, sizePx * 0.70f)
            lineTo(sizePx * 0.76f, sizePx * 0.30f)
        }
        canvas.drawPath(path, checkPaint)
    } else {
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 1.8f * context.resources.displayMetrics.density
        }
        canvas.drawRoundRect(boxRect, corner, corner, strokePaint)
    }
    return bitmap
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
// 2. DESK MEMO PAD (4x2 - HORIZONTAL RULED NOTEPAD)
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
// 3. INTERACTIVE CHECKLIST (4x2)
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

    // 1. Proportional Header Band & Bottom Inset (Total Weight 6.8f)
    val totalWeight = 6.8f
    val headerBandH = cardRect.height() * (1.25f / totalWeight)
    val bottomMarginH = cardRect.height() * (0.55f / totalWeight)
    val rowStartY = cardRect.top + headerBandH
    val rowTotalH = cardRect.height() - headerBandH - bottomMarginH
    val slotH = rowTotalH / 5f

    // Typography
    val titleTextSize = 15.5f * scaleFactor * fScale
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = titleTextSize
        typeface = getSlateFont(context, 700)
    }

    val titleMetrics = titlePaint.fontMetrics
    val titleCenterY = cardRect.top + (headerBandH * 0.52f)
    val titleY = titleCenterY - ((titleMetrics.ascent + titleMetrics.descent) / 2f)

    // A. Render Vector Edit Icon (ic_pencil_alt) without circular background
    val iconSize = (14f * scaleFactor * fScale).coerceIn(12f * scaleFactor, 18f * scaleFactor)
    val iconRight = cardRect.right - padX
    val iconLeft = iconRight - iconSize
    val iconTop = titleCenterY - (iconSize / 2f)
    val iconBottom = iconTop + iconSize

    androidx.core.content.ContextCompat.getDrawable(context, com.altusix.slate.R.drawable.ic_pencil_alt)?.let { drawable ->
        val mutated = drawable.mutate()
        mutated.setTint(accentColor)
        mutated.setBounds(iconLeft.toInt(), iconTop.toInt(), iconRight.toInt(), iconBottom.toInt())
        mutated.draw(canvas)
    }

    // B. Progress Count Badge (Placed to the left of the Edit Icon)
    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = (11f * scaleFactor * fScale).coerceIn(10f * scaleFactor, 15f * scaleFactor)
        typeface = getSlateFont(context, 700)
        textAlign = Paint.Align.RIGHT
    }
    val badgeText = "${note.completedCount}/${note.totalCount}"
    val badgeGap = 8f * scaleFactor
    val badgeX = iconLeft - badgeGap
    canvas.drawText(badgeText, badgeX, titleY, badgePaint)
    val badgeWidth = badgePaint.measureText(badgeText)

    // C. Note Title (Fills remaining width)
    val titleStartX = cardRect.left + padX
    val maxTitleW = (badgeX - badgeWidth - (10f * scaleFactor)) - titleStartX
    val displayTitle = if (titlePaint.measureText(note.title) > maxTitleW) {
        var t = note.title
        while (t.isNotEmpty() && titlePaint.measureText("$t…") > maxTitleW) t = t.dropLast(1)
        "$t…"
    } else note.title

    canvas.drawText(displayTitle, titleStartX, titleY, titlePaint)

    // 2. Proportional Checklist Rows
    val checkR = (8.5f * scaleFactor * fScale).coerceAtMost(slotH * 0.32f)
    val checkCx = cardRect.left + padX + checkR

    val itemTextSize = (13f * scaleFactor * fScale).coerceAtMost(slotH * 0.48f)
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = itemTextSize
        typeface = getSlateFont(context, 500)
    }

    val textLeft = checkCx + checkR + (10f * scaleFactor)
    val maxTextW = cardRect.right - padX - textLeft

    for (i in 0 until 5) {
        val cy = rowStartY + (i * slotH) + (slotH / 2f)

        if (i < note.items.size) {
            val item = note.items[i]
            drawCheckbox(
                canvas = canvas,
                cx = checkCx,
                cy = cy,
                radius = checkR,
                isDone = item.isDone,
                accentColor = accentColor,
                borderColor = secondaryTextColor,
                scaleFactor = scaleFactor
            )

            textPaint.color = if (item.isDone) secondaryTextColor else primaryTextColor
            val displayText = if (textPaint.measureText(item.text) > maxTextW) {
                var t = item.text
                while (t.isNotEmpty() && textPaint.measureText("$t…") > maxTextW) t = t.dropLast(1)
                "$t…"
            } else item.text

            val textBaseY = cy + (itemTextSize * 0.35f)
            canvas.drawText(displayText, textLeft, textBaseY, textPaint)

            if (item.isDone) {
                val measuredW = textPaint.measureText(displayText)
                val strikePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = secondaryTextColor
                    strokeWidth = 1.4f * scaleFactor
                }
                canvas.drawLine(textLeft, cy, textLeft + measuredW, cy, strikePaint)
            }
        } else {
            // Empty placeholder row
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isLight) Color(0x22000000).toArgb() else Color(0x20FFFFFF).toArgb()
                style = Paint.Style.FILL
            }
            canvas.drawCircle(checkCx, cy, 2.5f * scaleFactor, dotPaint)

            val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isLight) Color(0x35000000).toArgb() else Color(0x25FFFFFF).toArgb()
                textSize = itemTextSize * 0.90f
                typeface = getSlateFont(context, 400)
            }
            canvas.drawText("Tap to add item...", textLeft, cy + (itemTextSize * 0.35f), placeholderPaint)
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
    val secondaryTextColor = if (isLight) Color(0xFF4A4A4E).toArgb() else Color(0xFFD1D1D6).toArgb()

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val aspect = 2f
        val cardW = minOf(w, h * aspect)
        val cardH = cardW / aspect
        RectF((w - cardW) / 2f, (h - cardH) / 2f, (w + cardW) / 2f, (h + cardH) / 2f)
    }

    // Base Paper Surface
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    // 1. Top Binding Header Strip with Accent Stitching
    val bindH = 26f * scaleFactor
    val bindPath = Path().apply {
        moveTo(cardRect.left + cardCornerRadius, cardRect.top)
        lineTo(cardRect.right - cardCornerRadius, cardRect.top)
        quadTo(cardRect.right, cardRect.top, cardRect.right, cardRect.top + cardCornerRadius)
        lineTo(cardRect.right, cardRect.top + bindH)
        lineTo(cardRect.left, cardRect.top + bindH)
        lineTo(cardRect.left, cardRect.top + cardCornerRadius)
        quadTo(cardRect.left, cardRect.top, cardRect.left + cardCornerRadius, cardRect.top)
        close()
    }
    val bindPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF242428).toArgb() else Color(0xFF141416).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawPath(bindPath, bindPaint)

    // Dashed Accent Stitching Line
    val stitchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        strokeWidth = 1.4f * scaleFactor
        pathEffect = DashPathEffect(floatArrayOf(5f * scaleFactor, 4f * scaleFactor), 0f)
    }
    canvas.drawLine(
        cardRect.left + 16f * scaleFactor,
        cardRect.top + bindH * 0.5f,
        cardRect.right - 16f * scaleFactor,
        cardRect.top + bindH * 0.5f,
        stitchPaint
    )

    // 2. Red Vertical Legal Margin Line
    val marginX = cardRect.left + (32f * scaleFactor).coerceAtMost(cardRect.width() * 0.20f)
    val redMarginPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0x75E53935).toArgb() else Color(0x55E53935).toArgb()
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawLine(marginX, cardRect.top + bindH, marginX, cardRect.bottom, redMarginPaint)

    val fScale = note.fontScaleMultiplier
    val padRight = 18f * scaleFactor
    val padLeft = marginX + (10f * scaleFactor)

    // 3. Dynamic Title Clearance & Baseline Anchor
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

    val titleTopClearance = cardRect.top + bindH + (10f * scaleFactor)
    val titleMetrics = titlePaint.fontMetrics
    val titleY = titleTopClearance - titleMetrics.ascent

    // Category Tag on the right
    val catText = note.category.uppercase()
    val catWidth = catPaint.measureText(catText)
    canvas.drawText(catText, cardRect.right - padRight, titleY, catPaint)

    // Pencil Icon placed before the title
    val editIconSize = 11.5f * scaleFactor * fScale
    val editPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = editIconSize
        typeface = getSlateFont(context, 700)
    }
    val editIconText = "✎"
    val editIconW = editPaint.measureText(editIconText)
    val editGap = 6f * scaleFactor

    val pencilX = padLeft
    canvas.drawText(editIconText, pencilX, titleY - 1.5f * scaleFactor, editPaint)

    // Title Text
    val titleStartX = pencilX + editIconW + editGap
    val maxTitleW = cardRect.right - padRight - catWidth - (12f * scaleFactor) - titleStartX
    val displayTitle = if (titlePaint.measureText(note.title) > maxTitleW) {
        var t = note.title
        while (t.isNotEmpty() && titlePaint.measureText("$t…") > maxTitleW) t = t.dropLast(1)
        "$t…"
    } else note.title

    canvas.drawText(displayTitle, titleStartX, titleY, titlePaint)

    // 4. Dynamic Ruled Horizontal Lines (Filling available vertical space)
    val lineStartY = titleY + maxOf(titleMetrics.descent + (4f * scaleFactor), 9f * scaleFactor)
    val lineSpacing = 21.5f * scaleFactor * fScale
    val bottomLimit = cardRect.bottom - (10f * scaleFactor)
    val availableHeight = (bottomLimit - lineStartY).coerceAtLeast(0f)
    val numLines = (availableHeight / lineSpacing).toInt().coerceAtLeast(2)

    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFFE5E5EA).toArgb() else Color(0x18FFFFFF).toArgb()
        strokeWidth = 1f * scaleFactor
    }

    for (i in 0 until numLines) {
        val lineY = lineStartY + i * lineSpacing
        canvas.drawLine(cardRect.left, lineY, cardRect.right, lineY, linePaint)
    }

    // 5. Note Content Typography
    val bodyTextSize = 12.5f * scaleFactor * fScale
    val contentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = bodyTextSize
        typeface = getSlateFont(context, 400)
    }

    drawWrappedText(
        canvas = canvas,
        text = note.content,
        x = padLeft,
        startY = lineStartY + (lineSpacing * 0.72f),
        maxWidth = cardRect.right - padRight - padLeft,
        paint = contentPaint,
        lineSpacing = lineSpacing,
        maxLines = numLines,
        bottomLimit = bottomLimit
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

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
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

    val fScale = note.fontScaleMultiplier

    // 1. Concentric Accent Icon Disc (Anchored inside the left cap)
    val badgeR = cardRect.height() * 0.33f
    val badgeCx = cardRect.left + pillRadius
    val badgeCy = cardRect.centerY()

    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(badgeCx, badgeCy, badgeR, badgePaint)

    // Vector Pencil Icon (mirrored horizontally to match Widget 6 orientation)
    val iconSize = (badgeR * 1.15f).toInt().coerceAtLeast(1)
    val iconLeft = (badgeCx - iconSize / 2f).toInt()
    val iconTop = (badgeCy - iconSize / 2f).toInt()
    val iconRight = iconLeft + iconSize
    val iconBottom = iconTop + iconSize

    val iconTint = if (isLight) Color.White.toArgb() else Color(0xFF121214).toArgb()
    androidx.core.content.ContextCompat.getDrawable(context, com.altusix.slate.R.drawable.ic_pencil_alt)?.let { drawable ->
        canvas.save()
        canvas.scale(-1f, 1f, badgeCx, badgeCy)
        val mutated = drawable.mutate()
        mutated.setTint(iconTint)
        mutated.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        mutated.draw(canvas)
        canvas.restore()
    }

    // 2. Text Bounds & Layout Geometry
    val textLeft = badgeCx + badgeR + (14f * scaleFactor)
    val textRight = cardRect.right - (pillRadius * 0.75f)
    val maxTextW = (textRight - textLeft).coerceAtLeast(10f)

    val titleTextSize = (14.5f * scaleFactor * fScale).coerceIn(12f * scaleFactor, 18f * scaleFactor)
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = titleTextSize
        typeface = getSlateFont(context, 700)
    }

    val bodyTextSize = (12f * scaleFactor * fScale).coerceIn(10f * scaleFactor, 15f * scaleFactor)
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = bodyTextSize
        typeface = getSlateFont(context, 400)
    }

    val titleText = note.title.trim()
    val rawBody = note.content.replace("\n", " ").trim()

    // 3. Mathematical Vertical Centering
    if (rawBody.isBlank()) {
        val titleMetrics = titlePaint.fontMetrics
        val titleY = cardRect.centerY() - ((titleMetrics.ascent + titleMetrics.descent) / 2f)

        val displayTitle = if (titlePaint.measureText(titleText) > maxTextW) {
            var t = titleText
            while (t.isNotEmpty() && titlePaint.measureText("$t…") > maxTextW) t = t.dropLast(1)
            "$t…"
        } else titleText

        canvas.drawText(displayTitle, textLeft, titleY, titlePaint)
    } else {
        val titleMetrics = titlePaint.fontMetrics
        val bodyMetrics = bodyPaint.fontMetrics
        val lineGap = 3.5f * scaleFactor

        val titleH = titleMetrics.descent - titleMetrics.ascent
        val bodyH = bodyMetrics.descent - bodyMetrics.ascent
        val totalBlockH = titleH + lineGap + bodyH

        val blockTop = cardRect.centerY() - (totalBlockH / 2f)
        val titleY = blockTop - titleMetrics.ascent
        val bodyY = titleY + titleMetrics.descent + lineGap - bodyMetrics.ascent

        val displayTitle = if (titlePaint.measureText(titleText) > maxTextW) {
            var t = titleText
            while (t.isNotEmpty() && titlePaint.measureText("$t…") > maxTextW) t = t.dropLast(1)
            "$t…"
        } else titleText

        val displayBody = if (bodyPaint.measureText(rawBody) > maxTextW) {
            var b = rawBody
            while (b.isNotEmpty() && bodyPaint.measureText("$b…") > maxTextW) b = b.dropLast(1)
            "$b…"
        } else rawBody

        canvas.drawText(displayTitle, textLeft, titleY, titlePaint)
        canvas.drawText(displayBody, textLeft, bodyY, bodyPaint)
    }

    return bitmap
}


// =========================================================================
// 6. BENTO NOTES & TASKS (4x2)
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
// 7. TORN PERFORATED RECEIPT (2x2)
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
    val secondaryTextColor = if (isLight) Color(0xFF4A4A4E).toArgb() else Color(0xFFD1D1D6).toArgb()

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    // Base Paper Card Surface
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val padX = 18f * scaleFactor
    val padBottom = 16f * scaleFactor
    val fScale = note.fontScaleMultiplier

    // 1. Perforation Dots across the top
    val perfY = cardRect.top + (14f * scaleFactor)
    val dotRadius = 2.2f * scaleFactor
    val perfPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0x35000000).toArgb() else Color(0x30FFFFFF).toArgb()
        style = Paint.Style.FILL
    }
    val dotGap = 12f * scaleFactor
    var currDotX = cardRect.left + padX
    val maxDotX = cardRect.right - padX
    while (currDotX <= maxDotX) {
        canvas.drawCircle(currDotX, perfY, dotRadius, perfPaint)
        currDotX += dotGap
    }

    // 2. Receipt Category Tag (Comfortable clearance below dots)
    val tagTextSize = (10f * scaleFactor * fScale).coerceIn(9f * scaleFactor, 13f * scaleFactor)
    val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = tagTextSize
        typeface = getSlateFont(context, 700)
    }
    val tagMetrics = tagPaint.fontMetrics
    val tagText = "# ${note.category.uppercase()}"

    // Clear 8dp gap below dots to avoid crowding
    val tagTop = perfY + dotRadius + (8f * scaleFactor)
    val tagY = tagTop - tagMetrics.ascent
    canvas.drawText(tagText, cardRect.left + padX, tagY, tagPaint)

    // 3. Receipt Title & Pencil Icon (Anchored closely below category tag)
    val titleTextSize = 15.5f * scaleFactor * fScale
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = titleTextSize
        typeface = getSlateFont(context, 700)
    }

    val titleMetrics = titlePaint.fontMetrics
    val tagBottom = tagY + tagMetrics.descent
    // Tight 4dp spacing between category tag and title
    val titleTop = tagBottom + (4f * scaleFactor)
    val titleY = titleTop - titleMetrics.ascent

    // Pencil Icon placed before title
    val editIconSize = 11.5f * scaleFactor * fScale
    val editPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = editIconSize
        typeface = getSlateFont(context, 700)
    }
    val editIconText = "✎"
    val editIconW = editPaint.measureText(editIconText)
    val editGap = 6f * scaleFactor

    val pencilX = cardRect.left + padX
    canvas.drawText(editIconText, pencilX, titleY - 1.5f * scaleFactor, editPaint)

    val titleStartX = pencilX + editIconW + editGap
    val maxTitleW = (cardRect.right - padX) - titleStartX
    val displayTitle = if (titlePaint.measureText(note.title.uppercase()) > maxTitleW) {
        var t = note.title.uppercase()
        while (t.isNotEmpty() && titlePaint.measureText("$t…") > maxTitleW) t = t.dropLast(1)
        "$t…"
    } else note.title.uppercase()

    canvas.drawText(displayTitle, titleStartX, titleY, titlePaint)

    // Dashed Receipt Divider Line
    val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFFDCDCE0).toArgb() else Color(0x22FFFFFF).toArgb()
        strokeWidth = 1.2f * scaleFactor
        pathEffect = DashPathEffect(floatArrayOf(4f * scaleFactor, 4f * scaleFactor), 0f)
    }
    val divY = titleY + titleMetrics.descent + (8f * scaleFactor)
    canvas.drawLine(cardRect.left + padX, divY, cardRect.right - padX, divY, divPaint)

    // 4. Stylized Barcode pinned at the bottom
    val barcodeH = 18f * scaleFactor
    val barcodeY = cardRect.bottom - padBottom - barcodeH
    val barcodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF141416).toArgb() else Color(0x80FFFFFF).toArgb()
        style = Paint.Style.FILL
    }

    val barPattern = intArrayOf(2, 4, 1, 3, 2, 5, 2, 1, 4, 2, 3, 1, 4, 2, 3, 1, 5, 2, 3, 1, 4, 2, 3, 1, 2)
    var currBarX = cardRect.left + padX
    val maxBarcodeX = cardRect.right - padX
    for (bw in barPattern) {
        val barW = bw * scaleFactor * 1.15f
        if (currBarX + barW > maxBarcodeX) break
        canvas.drawRect(currBarX, barcodeY, currBarX + barW, barcodeY + barcodeH, barcodePaint)
        currBarX += barW + (3f * scaleFactor)
    }

    // 5. Content (Monospace receipt formatting, fills space down to barcode)
    val bodyTextSize = 12f * scaleFactor * fScale
    val contentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = bodyTextSize
        typeface = Typeface.MONOSPACE
    }

    val lineSpacing = bodyTextSize * 1.45f
    val startY = divY + (10f * scaleFactor) + (bodyTextSize * 0.85f)
    val bottomLimit = barcodeY - (10f * scaleFactor)

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
// 8. DOT GRID SCRATCHPAD (2x2)
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
    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    // Base Paper Surface
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    // 1. Clipped Dot Grid Matrix
    canvas.save()
    val clipPath = Path().apply {
        addRoundRect(cardRect, cardCornerRadius, cardCornerRadius, Path.Direction.CW)
    }
    canvas.clipPath(clipPath)

    val dotGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0x1F000000).toArgb() else Color(0x18FFFFFF).toArgb()
        style = Paint.Style.FILL
    }
    val gridSpacing = 20f * scaleFactor
    var gy = cardRect.top + gridSpacing
    while (gy < cardRect.bottom) {
        var gx = cardRect.left + gridSpacing
        while (gx < cardRect.right) {
            canvas.drawCircle(gx, gy, 1.4f * scaleFactor, dotGridPaint)
            gx += gridSpacing
        }
        gy += gridSpacing
    }
    canvas.restore()

    val padX = 18f * scaleFactor
    val padTop = 16f * scaleFactor
    val padBottom = 16f * scaleFactor
    val fScale = note.fontScaleMultiplier

    // 2. Header: Title and Category Badge (No Pencil Icon)
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

    val titleTopClearance = cardRect.top + padTop
    val titleMetrics = titlePaint.fontMetrics
    val titleY = titleTopClearance - titleMetrics.ascent

    // Category Tag on the right
    val catText = note.category.uppercase()
    val catWidth = catPaint.measureText(catText)
    canvas.drawText(catText, cardRect.right - padX, titleY, catPaint)

    // Title Text starting cleanly at padX
    val titleStartX = cardRect.left + padX
    val maxTitleW = (cardRect.right - padX - catWidth - (12f * scaleFactor)) - titleStartX
    val displayTitle = if (titlePaint.measureText(note.title) > maxTitleW) {
        var t = note.title
        while (t.isNotEmpty() && titlePaint.measureText("$t…") > maxTitleW) t = t.dropLast(1)
        "$t…"
    } else note.title

    canvas.drawText(displayTitle, titleStartX, titleY, titlePaint)

    // Minimal Accent Underline below title
    val underlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        strokeWidth = 2f * scaleFactor
        strokeCap = Paint.Cap.ROUND
    }
    val underlineY = titleY + maxOf(titleMetrics.descent + (4f * scaleFactor), 6f * scaleFactor)
    val underlineLength = (28f * scaleFactor * fScale).coerceAtMost(titlePaint.measureText(displayTitle))
    canvas.drawLine(titleStartX, underlineY, titleStartX + underlineLength, underlineY, underlinePaint)

    // 3. Technical Grid Badge at Bottom Right
    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = (9.5f * scaleFactor * fScale).coerceIn(8.5f * scaleFactor, 12f * scaleFactor)
        typeface = getSlateFont(context, 700)
        textAlign = Paint.Align.RIGHT
    }
    val badgeMetrics = badgePaint.fontMetrics
    val badgeY = cardRect.bottom - padBottom
    canvas.drawText("5MM GRID", cardRect.right - padX, badgeY, badgePaint)

    // 4. Dynamic Body Text Flow (Fills height down to the bottom badge)
    val bodyTextSize = 12.5f * scaleFactor * fScale
    val contentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = bodyTextSize
        typeface = getSlateFont(context, 400)
    }

    val lineSpacing = bodyTextSize * 1.45f
    val startY = underlineY + (10f * scaleFactor) + (bodyTextSize * 0.85f)
    val bottomLimit = badgeY - (badgeMetrics.descent - badgeMetrics.ascent) - (6f * scaleFactor)

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
// 9. MULTI-NOTE STACK (2x2)
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
    val baseCardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    // Reserve top offset for the physical layered cards underneath
    val stackOffset = 6f * scaleFactor
    val cardRect = RectF(
        baseCardRect.left,
        baseCardRect.top + stackOffset,
        baseCardRect.right,
        baseCardRect.bottom
    )

    // 1. Stacked Underneath Card Layer (Physical Depth Effect)
    val underPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFFDCDCE0).toArgb() else Color(0xFF1C1C20).toArgb()
        style = Paint.Style.FILL
    }
    val underRect = RectF(
        cardRect.left + (8f * scaleFactor),
        cardRect.top - stackOffset,
        cardRect.right - (8f * scaleFactor),
        cardRect.bottom - stackOffset
    )
    canvas.drawRoundRect(underRect, cardCornerRadius, cardCornerRadius, underPaint)

    // 2. Foreground Card Plate
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val padX = 18f * scaleFactor
    val fScale = note.fontScaleMultiplier

    // 3. Header: Title and Category Badge (Zero Overlap)
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

    val titleTopClearance = cardRect.top + (16f * scaleFactor)
    val titleMetrics = titlePaint.fontMetrics
    val titleY = titleTopClearance - titleMetrics.ascent

    // Draw Category Tag
    val catText = note.category.uppercase()
    val catWidth = catPaint.measureText(catText)
    canvas.drawText(catText, cardRect.right - padX, titleY, catPaint)

    // Truncate Title to prevent colliding with Category
    val titleStartX = cardRect.left + padX
    val maxTitleW = (cardRect.right - padX - catWidth - (12f * scaleFactor)) - titleStartX
    val displayTitle = if (titlePaint.measureText(note.title) > maxTitleW) {
        var t = note.title
        while (t.isNotEmpty() && titlePaint.measureText("$t…") > maxTitleW) t = t.dropLast(1)
        "$t…"
    } else note.title
    canvas.drawText(displayTitle, titleStartX, titleY, titlePaint)

    // 4. Stack Navigation Deck at Bottom: "◀   2 of 3   ▶"
    val navY = cardRect.bottom - (16f * scaleFactor)
    val navMetricsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12f * scaleFactor
        typeface = getSlateFont(context, 700)
    }
    val navMetrics = navMetricsPaint.fontMetrics
    val navDeckTop = navY + navMetrics.ascent

    val navArrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = 12f * scaleFactor
        typeface = getSlateFont(context, 700)
    }

    // Prev "◀"
    canvas.drawText("◀", cardRect.left + padX + (4f * scaleFactor), navY, navArrowPaint)

    // Page indicator center
    val pageCountText = "${pageIndex + 1} of $totalPages"
    val pagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = (11f * scaleFactor * fScale).coerceIn(10f * scaleFactor, 14f * scaleFactor)
        typeface = getSlateFont(context, 600)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(pageCountText, cardRect.centerX(), navY, pagePaint)

    // Next "▶"
    val nextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = 12f * scaleFactor
        typeface = getSlateFont(context, 700)
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText("▶", cardRect.right - padX - (4f * scaleFactor), navY, nextPaint)

    // 5. Dynamic Body Text Flow (Fills height down to navigation deck)
    val bodyTextSize = 12.5f * scaleFactor * fScale
    val contentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = bodyTextSize
        typeface = getSlateFont(context, 400)
    }

    val lineSpacing = bodyTextSize * 1.45f
    val startY = titleY + titleMetrics.descent + (10f * scaleFactor) + (bodyTextSize * 0.85f)
    val bottomLimit = navDeckTop - (8f * scaleFactor)

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
