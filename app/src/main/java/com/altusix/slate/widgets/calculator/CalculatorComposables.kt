package com.altusix.slate.widgets.calculator

import android.content.Context
import android.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.altusix.slate.data.local.SlateWidgetConfig

// =========================================================================
// GEOMETRY & CONTRAST UTILITIES
// =========================================================================

private fun getStandardCornerRadius(density: Float): Float = 22f * density

private fun getOpTextColor(bg: Int): Int {
    val r = ((bg shr 16) and 0xFF) / 255f
    val g = ((bg shr 8) and 0xFF) / 255f
    val b = (bg and 0xFF) / 255f
    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
    return if (luminance > 0.5f) Color(0xFF121214).toArgb() else Color.White.toArgb()
}

// =========================================================================
// SMART MULTI-LINE DISPLAY RENDERER
// =========================================================================

/**
 * Renders expression and result text inside [displayRect].
 * Automatically scales down long results, wraps onto new lines, moves upward,
 * and keeps the expression text positioned directly above the result.
 */
private fun drawCalculatorDisplay(
    canvas: Canvas,
    displayRect: RectF,
    exprText: String,
    resultText: String,
    primaryTextColor: Int,
    secondaryTextColor: Int,
    density: Float,
    isCenterAligned: Boolean = false
) {
    val margin = 10f * density
    val topBottomMargin = 6f * density
    val maxWidth = (displayRect.width() - (margin * 2f)).coerceAtLeast(10f)

    canvas.save()
    canvas.clipRect(displayRect) // Strict clipping inside display container

    var resultTextSize = displayRect.height() * 0.42f
    val resultPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = resultTextSize
        typeface = Typeface.DEFAULT_BOLD
        textAlign = if (isCenterAligned) Paint.Align.CENTER else Paint.Align.RIGHT
    }

    // 1. Auto-scale font size down for long numbers
    var textWidth = resultPaint.measureText(resultText)
    val minTextSize = displayRect.height() * 0.20f
    while (textWidth > maxWidth && resultTextSize > minTextSize) {
        resultTextSize -= 1f * density
        resultPaint.textSize = resultTextSize
        textWidth = resultPaint.measureText(resultText)
    }

    // 2. Wrap result text into multiple lines if it still exceeds width
    val resultLines = mutableListOf<String>()
    if (textWidth > maxWidth) {
        var remaining = resultText
        while (remaining.isNotEmpty()) {
            var chunkLength = remaining.length
            while (chunkLength > 0 && resultPaint.measureText(remaining.takeLast(chunkLength)) > maxWidth) {
                chunkLength--
            }
            if (chunkLength == 0) chunkLength = 1
            resultLines.add(0, remaining.takeLast(chunkLength))
            remaining = remaining.dropLast(chunkLength)
        }
    } else {
        resultLines.add(resultText)
    }

    // 3. Draw result lines starting from bottom and moving up
    val fmRes = resultPaint.fontMetrics
    val lineH = (fmRes.descent - fmRes.ascent) * 0.88f
    val targetX = if (isCenterAligned) displayRect.centerX() else displayRect.right - margin

    var currentY = displayRect.bottom - topBottomMargin - fmRes.descent
    val topResultLineY: Float

    for (i in resultLines.indices.reversed()) {
        val line = resultLines[i]
        canvas.drawText(line, targetX, currentY, resultPaint)
        if (i > 0) {
            currentY -= lineH
        }
    }
    topResultLineY = currentY + fmRes.ascent

    // 4. Position expression text directly above the highest result line
    val cleanExpr = exprText.ifEmpty { " " }
    val exprPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = minOf(displayRect.height() * 0.18f, resultTextSize * 0.55f)
        typeface = Typeface.DEFAULT
        textAlign = if (isCenterAligned) Paint.Align.CENTER else Paint.Align.RIGHT
    }

    val fmExpr = exprPaint.fontMetrics
    val exprY = topResultLineY - (2f * density) - fmExpr.descent

    var trimmedExpr = cleanExpr
    while (trimmedExpr.length > 1 && exprPaint.measureText(trimmedExpr) > maxWidth) {
        trimmedExpr = trimmedExpr.drop(1)
    }

    canvas.drawText(trimmedExpr, targetX, exprY, exprPaint)

    canvas.restore()
}

// =========================================================================
// CANVAS BITMAP GENERATORS
// =========================================================================

/**
 * 1. Standard 2x2 Calculator
 */
fun generateCalculator2x2Bitmap(
    context: Context,
    calcState: CalculatorState,
    slateConfig: SlateWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = slateConfig.themeMode == "LIGHT"
    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()

    val cardCornerRadius = getStandardCornerRadius(density)
    val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    canvas.drawRoundRect(rect, cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = 8f * density
    val innerRect = RectF(rect.left + pad, rect.top + pad, rect.right - pad, rect.bottom - pad)

    val displayHeight = innerRect.height() * 0.28f
    val displayRect = RectF(innerRect.left, innerRect.top, innerRect.right, innerRect.top + displayHeight)

    val primaryTextColor = if (isLight) Color(0xFF161618).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF8E8E93).toArgb() else Color(0x99FFFFFF).toArgb()

    // Smart Display Render
    drawCalculatorDisplay(
        canvas, displayRect,
        calcState.expression, calcState.resultText,
        primaryTextColor, secondaryTextColor, density
    )

    // Keypad Grid
    val keypadTop = displayRect.bottom + (4f * density)
    val keypadH = innerRect.bottom - keypadTop

    val btnBgColor = if (isLight) Color(0xFFEFEFF4).toArgb() else Color(0xFF1E1E22).toArgb()
    val keyGrid = listOf(
        listOf("AC", "DEL", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "=")
    )

    val rowCount = 5
    val rowH = keypadH / rowCount
    val singleColW = innerRect.width() / 4f
    val gap = 2f * density

    for (r in 0 until rowCount) {
        val rowKeys = keyGrid[r]
        val colCount = rowKeys.size
        val rowY = keypadTop + (r * rowH)

        for (c in 0 until colCount) {
            val key = rowKeys[c]
            val keyW = if (key == "0") singleColW * 2f else singleColW
            val keyX = if (r == 4 && c > 0) innerRect.left + singleColW * (c + 1) else innerRect.left + singleColW * c

            val btnRect = RectF(keyX + gap, rowY + gap, keyX + keyW - gap, rowY + rowH - gap)
            val isOp = key in listOf("÷", "×", "-", "+", "=")
            val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (isOp) accentColor else btnBgColor }

            val defaultBtnRadius = 10f * density
            val bottomNestedRadius = (cardCornerRadius - pad).coerceAtLeast(8f * density)

            val radii = FloatArray(8) { defaultBtnRadius }
            if (r == 4 && c == 0) {
                radii[6] = bottomNestedRadius; radii[7] = bottomNestedRadius
            } else if (r == 4 && c == colCount - 1) {
                radii[4] = bottomNestedRadius; radii[5] = bottomNestedRadius
            }

            val path = Path().apply { addRoundRect(btnRect, radii, Path.Direction.CW) }
            canvas.drawPath(path, btnPaint)

            val labelSize = minOf(singleColW * 0.34f, rowH * 0.38f)
            val keyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isOp) getOpTextColor(accentColor) else primaryTextColor
                textSize = labelSize
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val fontMetrics = keyTextPaint.fontMetrics
            val baseline = btnRect.centerY() - ((fontMetrics.descent + fontMetrics.ascent) / 2f)
            canvas.drawText(key, btnRect.centerX(), baseline, keyTextPaint)
        }
    }

    return bitmap
}

/**
 * 2. Split Capsule Calculator (2x2)
 */
fun generateSplitCalculatorBitmap(
    context: Context,
    calcState: CalculatorState,
    slateConfig: SlateWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = slateConfig.themeMode == "LIGHT"
    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val cardCornerRadius = getStandardCornerRadius(density)

    val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    canvas.drawRoundRect(rect, cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = 8f * density
    val innerRect = RectF(rect.left + pad, rect.top + pad, rect.right - pad, rect.bottom - pad)

    val displayHeight = innerRect.height() * 0.28f
    val displayRect = RectF(innerRect.left, innerRect.top, innerRect.right, innerRect.top + displayHeight)

    val primaryTextColor = if (isLight) Color(0xFF161618).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF8E8E93).toArgb() else Color(0x99FFFFFF).toArgb()

    drawCalculatorDisplay(
        canvas, displayRect,
        calcState.expression, calcState.resultText,
        primaryTextColor, secondaryTextColor, density
    )

    val keypadTop = displayRect.bottom + (4f * density)
    val keypadH = innerRect.bottom - keypadTop

    val maxCapsuleWidth = 72f * density
    val rightW = (innerRect.width() * 0.22f).coerceAtMost(maxCapsuleWidth)
    val leftW = innerRect.width() - rightW

    val btnBgColor = if (isLight) Color(0xFFEFEFF4).toArgb() else Color(0xFF1E1E22).toArgb()
    val gap = 2.5f * density

    val rightCapsuleRect = RectF(innerRect.right - rightW + gap, keypadTop + gap, innerRect.right - gap, innerRect.bottom - gap)
    val capsulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
    canvas.drawRoundRect(rightCapsuleRect, rightW / 2f, rightW / 2f, capsulePaint)

    val rowCount = 5
    val rowH = keypadH / rowCount
    val leftColW = leftW / 3f

    val leftGrid = listOf(
        listOf("AC", "DEL", "%"),
        listOf("7", "8", "9"),
        listOf("4", "5", "6"),
        listOf("1", "2", "3"),
        listOf("0", ".")
    )

    for (r in 0 until rowCount) {
        val keys = leftGrid[r]
        val rowY = keypadTop + (r * rowH)
        for (c in keys.indices) {
            val key = keys[c]
            val keyW = if (key == "0") leftColW * 2f else leftColW
            val keyX = if (r == 4 && c > 0) innerRect.left + leftColW * (c + 1) else innerRect.left + leftColW * c

            val btnRect = RectF(keyX + gap, rowY + gap, keyX + keyW - gap, rowY + rowH - gap)
            val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = btnBgColor }

            val radii = FloatArray(8) { 10f * density }
            if (r == 4 && c == 0) {
                val bottomNestedRadius = (cardCornerRadius - pad).coerceAtLeast(8f * density)
                radii[6] = bottomNestedRadius; radii[7] = bottomNestedRadius
            }
            val path = Path().apply { addRoundRect(btnRect, radii, Path.Direction.CW) }
            canvas.drawPath(path, btnPaint)

            val labelSize = minOf(leftColW * 0.30f, rowH * 0.36f)
            val keyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryTextColor
                textSize = labelSize
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val fm = keyTextPaint.fontMetrics
            canvas.drawText(key, btnRect.centerX(), btnRect.centerY() - ((fm.descent + fm.ascent) / 2f), keyTextPaint)
        }
    }

    val opKeys = listOf("÷", "×", "-", "+", "=")
    val opRowH = rightCapsuleRect.height() / 5f
    val opTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = getOpTextColor(accentColor)
        textSize = minOf(rightW * 0.40f, opRowH * 0.42f)
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    for (i in opKeys.indices) {
        val opY = rightCapsuleRect.top + (i * opRowH) + (opRowH / 2f)
        val fm = opTextPaint.fontMetrics
        canvas.drawText(opKeys[i], rightCapsuleRect.centerX(), opY - ((fm.descent + fm.ascent) / 2f), opTextPaint)
    }

    return bitmap
}

/**
 * 3. Studio 4x2 Calculator
 */
fun generateStudioCalculator4x2Bitmap(
    context: Context,
    calcState: CalculatorState,
    slateConfig: SlateWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = slateConfig.themeMode == "LIGHT"
    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val cardCornerRadius = getStandardCornerRadius(density)

    val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    canvas.drawRoundRect(rect, cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = 8f * density
    val innerRect = RectF(rect.left + pad, rect.top + pad, rect.right - pad, rect.bottom - pad)

    val gap = 3f * density
    val primaryTextColor = if (isLight) Color(0xFF161618).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF8E8E93).toArgb() else Color(0x99FFFFFF).toArgb()
    val btnBgColor = if (isLight) Color(0xFFEFEFF4).toArgb() else Color(0xFF1E1E22).toArgb()

    val leftWidth = innerRect.width() * 0.56f
    val leftRect = RectF(innerRect.left, innerRect.top, innerRect.left + leftWidth, innerRect.bottom)
    val rightRect = RectF(leftRect.right, innerRect.top, innerRect.right, innerRect.bottom)

    val numRows = 4
    val rowH = leftRect.height() / numRows
    val btnCornerR = 10f * density

    // Left Numpad
    val keyGrid = listOf(
        listOf("7", "8", "9", "÷"),
        listOf("4", "5", "6", "×"),
        listOf("1", "2", "3", "-"),
        listOf("0", ".", "%", "+")
    )

    val numCols = 4
    val colW = leftRect.width() / numCols

    for (r in 0 until numRows) {
        val rowY1 = leftRect.top + (r * rowH)
        val rowY2 = leftRect.top + ((r + 1) * rowH)
        val keys = keyGrid[r]

        for (c in 0 until numCols) {
            val key = keys[c]
            val colX1 = leftRect.left + (c * colW)
            val colX2 = leftRect.left + ((c + 1) * colW)

            val btnRect = RectF(colX1 + gap, rowY1 + gap, colX2 - gap, rowY2 - gap)

            val isOpColumn = c == numCols - 1
            val btnColor = if (isOpColumn) accentColor else btnBgColor

            val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = btnColor }
            canvas.drawRoundRect(btnRect, btnCornerR, btnCornerR, btnPaint)

            val labelSize = minOf(colW * 0.36f, rowH * 0.40f)
            val keyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isOpColumn) getOpTextColor(accentColor) else primaryTextColor
                textSize = labelSize
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }

            val fm = keyTextPaint.fontMetrics
            val baseline = btnRect.centerY() - ((fm.descent + fm.ascent) / 2f)
            canvas.drawText(key, btnRect.centerX(), baseline, keyTextPaint)
        }
    }

    // Right Display Screen & Utility Row
    val displayCardRect = RectF(
        rightRect.left + gap,
        rightRect.top + gap,
        rightRect.right - gap,
        rightRect.top + (3 * rowH) - gap
    )

    val screenBgColor = if (isLight) Color(0xFFE8E8ED).toArgb() else Color(0xFF121215).toArgb()
    val screenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = screenBgColor }
    val screenRadius = 14f * density
    canvas.drawRoundRect(displayCardRect, screenRadius, screenRadius, screenPaint)

    // Smart Display Render
    drawCalculatorDisplay(
        canvas, displayCardRect,
        calcState.expression, calcState.resultText,
        primaryTextColor, secondaryTextColor, density
    )

    // AC / DEL Row
    val utilRowY1 = rightRect.top + (3 * rowH)
    val utilRowY2 = rightRect.bottom

    val utilityKeys = listOf("AC", "DEL")
    val utilColW = rightRect.width() / 2f
    val utilBtnBgColor = if (isLight) Color(0xFFE2E2E8).toArgb() else Color(0xFF28282E).toArgb()

    for (i in utilityKeys.indices) {
        val key = utilityKeys[i]
        val btnX1 = rightRect.left + (i * utilColW)
        val btnX2 = rightRect.left + ((i + 1) * utilColW)

        val btnRect = RectF(btnX1 + gap, utilRowY1 + gap, btnX2 - gap, utilRowY2 - gap)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = utilBtnBgColor }
        canvas.drawRoundRect(btnRect, btnCornerR, btnCornerR, fillPaint)

        val labelSize = minOf(colW * 0.36f, rowH * 0.40f)
        val keyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = labelSize
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val fm = keyTextPaint.fontMetrics
        val baseline = btnRect.centerY() - ((fm.descent + fm.ascent) / 2f)
        canvas.drawText(key, btnRect.centerX(), baseline, keyTextPaint)
    }

    return bitmap
}

/**
 * 4. Circular Stage Calculator (2x2 Circle)
 */
fun generateCircleCalculatorBitmap(
    context: Context,
    calcState: CalculatorState,
    slateConfig: SlateWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = slateConfig.themeMode == "LIGHT"
    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()

    val cardSize = minOf(w, h).toFloat()
    val cx = w / 2f
    val cy = h / 2f
    val radius = (cardSize / 2f) - (2f * density)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    canvas.drawCircle(cx, cy, radius, bgPaint)

    val primaryTextColor = if (isLight) Color(0xFF161618).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF8E8E93).toArgb() else Color(0x99FFFFFF).toArgb()

    val totalHeight = 2f * radius
    val circleTop = cy - radius
    val circleBottom = cy + radius
    val circleLeft = cx - radius
    val circleRight = cx + radius

    val displayHeight = totalHeight * 0.28f
    val displayRect = RectF(cx - (radius * 0.7f), circleTop + (4f * density), cx + (radius * 0.7f), circleTop + displayHeight)

    drawCalculatorDisplay(
        canvas, displayRect,
        calcState.expression, calcState.resultText,
        primaryTextColor, secondaryTextColor, density,
        isCenterAligned = true
    )

    val circleClipPath = Path().apply {
        addCircle(cx, cy, radius - (1.5f * density), Path.Direction.CW)
    }

    canvas.save()
    canvas.clipPath(circleClipPath)

    val keypadTop = circleTop + displayHeight
    val keypadHeight = totalHeight * 0.72f
    val numRows = 5
    val numCols = 4

    val rowH = keypadHeight / numRows
    val colW = (2f * radius) / numCols
    val gap = 2.5f * density

    val btnBgColor = if (isLight) Color(0xFFEFEFF4).toArgb() else Color(0xFF1E1E22).toArgb()
    val innerBtnRadius = 6f * density

    val keyGrid = listOf(
        listOf("AC", "DEL", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+")
    )

    for (r in 0 until 4) {
        val rowY1 = keypadTop + (r * rowH)
        val rowY2 = keypadTop + ((r + 1) * rowH)
        val keys = keyGrid[r]

        for (c in 0 until numCols) {
            val key = keys[c]
            val colX1 = circleLeft + (c * colW)
            val colX2 = circleLeft + ((c + 1) * colW)

            val drawX1 = if (c == 0) circleLeft - (20f * density) else colX1 + (gap / 2f)
            val drawX2 = if (c == numCols - 1) circleRight + (20f * density) else colX2 - (gap / 2f)
            val drawY1 = rowY1 + (gap / 2f)
            val drawY2 = rowY2 - (gap / 2f)

            val isOp = key in listOf("÷", "×", "-", "+")
            val btnColor = if (isOp) accentColor else btnBgColor
            val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = btnColor }

            val btnRect = RectF(drawX1, drawY1, drawX2, drawY2)
            canvas.drawRoundRect(btnRect, innerBtnRadius, innerBtnRadius, btnPaint)

            val (alignMode, dxFactor, dyFactor) = when (key) {
                "AC" -> Triple(Paint.Align.CENTER, 0.08f, 0.0f)
                "7"  -> Triple(Paint.Align.CENTER, 0.0f, 0.0f)
                "1"  -> Triple(Paint.Align.CENTER, 0.20f, 0.0f)
                "÷"  -> Triple(Paint.Align.CENTER, -0.08f, 0.0f)
                "×"  -> Triple(Paint.Align.CENTER, 0.0f, 0.0f)
                "+"  -> Triple(Paint.Align.CENTER, -0.20f, 0.0f)
                else -> Triple(Paint.Align.CENTER, 0.0f, 0.0f)
            }

            val labelSize = minOf(colW * 0.34f, rowH * 0.38f)
            val keyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isOp) getOpTextColor(accentColor) else primaryTextColor
                textSize = labelSize
                typeface = Typeface.DEFAULT_BOLD
                textAlign = alignMode
            }

            val cellCenterX = (colX1 + colX2) / 2f
            val cellCenterY = (rowY1 + rowY2) / 2f

            val textCx = cellCenterX + (colW * dxFactor)
            val textCy = cellCenterY + (rowH * dyFactor)

            val fontMetrics = keyTextPaint.fontMetrics
            val baseline = textCy - ((fontMetrics.descent + fontMetrics.ascent) / 2f)
            canvas.drawText(key, textCx, baseline, keyTextPaint)
        }
    }

    // Row 5: Bottom Segment ("0" and ".")
    val r5Y1 = keypadTop + (4 * rowH) + (gap / 2f)
    val r5Y2 = circleBottom + (20f * density)

    val row5Bounds = listOf(
        Triple("0", circleLeft - (20f * density), cx - (gap / 2f)),
        Triple(".", cx + (gap / 2f), circleRight + (20f * density))
    )

    for ((key, drawX1, drawX2) in row5Bounds) {
        val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = btnBgColor }

        val btnRect = RectF(drawX1, r5Y1, drawX2, r5Y2)
        canvas.drawRoundRect(btnRect, innerBtnRadius, innerBtnRadius, btnPaint)

        val (alignMode, dxFactor, dyFactor) = when (key) {
            "0"  -> Triple(Paint.Align.CENTER, 0.24f, -0.05f)
            "."  -> Triple(Paint.Align.CENTER, -0.24f, -0.05f)
            else -> Triple(Paint.Align.CENTER, 0.0f, 0.0f)
        }

        val labelSize = minOf(colW * 0.36f, rowH * 0.38f)
        val keyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = labelSize
            typeface = Typeface.DEFAULT_BOLD
            textAlign = alignMode
        }

        val btnWidth = btnRect.width()
        val textCx = (if (key == "0") cx - (radius / 2f) else cx + (radius / 2f)) + (btnWidth * dxFactor)
        val textCy = ((r5Y1 + (keypadTop + (5 * rowH))) / 2f) + (rowH * dyFactor)

        val fontMetrics = keyTextPaint.fontMetrics
        val baseline = textCy - ((fontMetrics.descent + fontMetrics.ascent) / 2f)
        canvas.drawText(key, textCx, baseline, keyTextPaint)
    }

    canvas.restore()

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0x1A000000).toArgb() else Color(0x22FFFFFF).toArgb()
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }
    canvas.drawCircle(cx, cy, radius - (1f * density), borderPaint)

    return bitmap
}