package com.altusix.slate.widgets.calculator

import android.content.Context
import android.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.altusix.slate.data.local.SlateWidgetConfig

// =========================================================================
// HELPER: Geometry & Dimension Utilities
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
// CANVAS BITMAP GENERATOR
// =========================================================================

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

    // Unified Slate outer card radius
    val cardCornerRadius = getStandardCornerRadius(density)
    val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())

    // 1. Draw Main Outer Card
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardCornerRadius, cardCornerRadius, bgPaint)

    // Tiny uniform padding around all controls so buttons never touch the outer border
    val pad = 8f * density
    val innerRect = RectF(rect.left + pad, rect.top + pad, rect.right - pad, rect.bottom - pad)

    // 2. Render Display Region
    val displayHeight = innerRect.height() * 0.26f
    val displayRect = RectF(innerRect.left, innerRect.top, innerRect.right, innerRect.top + displayHeight)

    val primaryTextColor = if (isLight) Color(0xFF161618).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF8E8E93).toArgb() else Color(0x99FFFFFF).toArgb()

    // Expression Text
    val exprText = calcState.expression.ifEmpty { " " }
    val exprPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = minOf(displayHeight * 0.26f, innerRect.width() * 0.055f)
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText(exprText.takeLast(24), displayRect.right - (10f * density), displayRect.top + (displayHeight * 0.38f), exprPaint)

    // Result Text
    val resultPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = minOf(displayHeight * 0.48f, innerRect.width() * 0.11f)
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText(calcState.resultText, displayRect.right - (10f * density), displayRect.top + (displayHeight * 0.85f), resultPaint)

    // 3. Render Keypad Grid
    val keypadTop = displayRect.bottom + (4f * density)
    val keypadH = innerRect.bottom - keypadTop

    val btnBgColor = if (isLight) Color(0xFFEFEFF4).toArgb() else Color(0xFF1E1E22).toArgb()
    val opBtnBgColor = accentColor

    fun getOpTextColor(bg: Int): Int {
        val r = ((bg shr 16) and 0xFF) / 255f
        val g = ((bg shr 8) and 0xFF) / 255f
        val b = (bg and 0xFF) / 255f
        val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
        return if (luminance > 0.5f) Color(0xFF121214).toArgb() else Color.White.toArgb()
    }

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

            val btnRect = RectF(
                keyX + gap,
                rowY + gap,
                keyX + keyW - gap,
                rowY + rowH - gap
            )

            val isOp = key in listOf("÷", "×", "-", "+", "=")
            val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isOp) opBtnBgColor else btnBgColor
                style = Paint.Style.FILL
            }

            // Key Corner Radius: Standard button radius (10dp) with bottom nested corners matching card curve
            val defaultBtnRadius = 10f * density
            val bottomNestedRadius = (cardCornerRadius - pad).coerceAtLeast(8f * density)

            val radii = FloatArray(8) { defaultBtnRadius }
            if (r == 4 && c == 0) { // '0' button (Bottom-Left)
                radii[6] = bottomNestedRadius
                radii[7] = bottomNestedRadius
            } else if (r == 4 && c == colCount - 1) { // '=' button (Bottom-Right)
                radii[4] = bottomNestedRadius
                radii[5] = bottomNestedRadius
            }

            val path = Path().apply {
                addRoundRect(btnRect, radii, Path.Direction.CW)
            }
            canvas.drawPath(path, btnPaint)

            // Button Key Label
            val labelTextSize = minOf(singleColW * 0.34f, rowH * 0.38f)
            val keyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isOp) getOpTextColor(opBtnBgColor) else primaryTextColor
                textSize = labelTextSize
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
 * 2. Split Capsule Calculator Bitmap Generator (2x2)
 * Features a capped max-width operator capsule to prevent over-stretching.
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

    // Top Display
    val displayHeight = innerRect.height() * 0.26f
    val displayRect = RectF(innerRect.left, innerRect.top, innerRect.right, innerRect.top + displayHeight)

    val primaryTextColor = if (isLight) Color(0xFF161618).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF8E8E93).toArgb() else Color(0x99FFFFFF).toArgb()

    val exprPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = minOf(displayHeight * 0.24f, innerRect.width() * 0.055f)
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText(calcState.expression.ifEmpty { " " }.takeLast(24), displayRect.right - (10f * density), displayRect.top + (displayHeight * 0.38f), exprPaint)

    val resultPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = minOf(displayHeight * 0.48f, innerRect.width() * 0.11f)
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText(calcState.resultText, displayRect.right - (10f * density), displayRect.top + (displayHeight * 0.85f), resultPaint)

    // Keypad Geometry with Capped Capsule Width
    val keypadTop = displayRect.bottom + (4f * density)
    val keypadH = innerRect.bottom - keypadTop

    // Cap the right capsule width so it stays sleek on wide widgets
    val maxCapsuleWidth = 72f * density
    val rightW = (innerRect.width() * 0.22f).coerceAtMost(maxCapsuleWidth)
    val leftW = innerRect.width() - rightW

    val btnBgColor = if (isLight) Color(0xFFEFEFF4).toArgb() else Color(0xFF1E1E22).toArgb()
    val gap = 2.5f * density

    // Draw Right Operator Capsule Background
    val rightCapsuleRect = RectF(innerRect.right - rightW + gap, keypadTop + gap, innerRect.right - gap, innerRect.bottom - gap)
    val capsulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
    canvas.drawRoundRect(rightCapsuleRect, rightW / 2f, rightW / 2f, capsulePaint)

    val rowCount = 5
    val rowH = keypadH / rowCount
    val leftColW = leftW / 3f

    // Left Numbers Grid
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
                radii[6] = bottomNestedRadius
                radii[7] = bottomNestedRadius
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

    // Right Operators Inside Capsule
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
 * 3. Ribbon Express Calculator Bitmap Generator (4x1 Horizontal)
 * Features a dual-row compact keypad with full number input capabilities.
 */
fun generateRibbonCalculatorBitmap(
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

    val pad = 6f * density
    val innerRect = RectF(rect.left + pad, rect.top + pad, rect.right - pad, rect.bottom - pad)

    // Left Display Region (30% Width)
    val displayW = innerRect.width() * 0.30f
    val keypadW = innerRect.width() - displayW

    val primaryTextColor = if (isLight) Color(0xFF161618).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF8E8E93).toArgb() else Color(0x99FFFFFF).toArgb()

    val exprPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = innerRect.height() * 0.20f
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText(calcState.expression.takeLast(12), innerRect.left + displayW - (6f * density), innerRect.top + (innerRect.height() * 0.32f), exprPaint)

    val resultPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = innerRect.height() * 0.42f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.RIGHT
    }
    val fmRes = resultPaint.fontMetrics
    canvas.drawText(calcState.resultText.take(6), innerRect.left + displayW - (6f * density), innerRect.bottom - (4f * density), resultPaint)

    // Right Keypad Region (70% Width, Split into Top/Bottom Rows)
    val rowH = innerRect.height() / 2f
    val topKeys = listOf("1", "2", "3", "4", "5", "AC", "÷")
    val bottomKeys = listOf("6", "7", "8", "9", "0", "DEL", "=")

    val btnBgColor = if (isLight) Color(0xFFEFEFF4).toArgb() else Color(0xFF1E1E22).toArgb()
    val colW = keypadW / 7f
    val gap = 1.5f * density

    fun drawKeyRow(keys: List<String>, rowY: Float) {
        val startX = innerRect.left + displayW
        for (c in keys.indices) {
            val key = keys[c]
            val btnX = startX + (c * colW)
            val btnRect = RectF(btnX + gap, rowY + gap, btnX + colW - gap, rowY + rowH - gap)

            val isEq = key == "="
            val isOp = key in listOf("÷", "×", "-", "+", "=")
            val btnColor = if (isEq) accentColor else if (isOp) Color(accentColor).copy(alpha = 0.25f).toArgb() else btnBgColor

            val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = btnColor }
            val r = minOf(btnRect.width(), btnRect.height()) * 0.25f
            canvas.drawRoundRect(btnRect, r, r, btnPaint)

            val keyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isEq) getOpTextColor(accentColor) else primaryTextColor
                textSize = minOf(colW * 0.42f, rowH * 0.45f)
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val fm = keyTextPaint.fontMetrics
            canvas.drawText(key, btnRect.centerX(), btnRect.centerY() - ((fm.descent + fm.ascent) / 2f), keyTextPaint)
        }
    }

    drawKeyRow(topKeys, innerRect.top)
    drawKeyRow(bottomKeys, innerRect.top + rowH)

    return bitmap
}

/**
 * 4. Circular Stage Calculator Bitmap Generator (2x2 Circle)
 * Uses responsive proportional text offsets (colW * factor) so alignments stay pristine at all widget sizes.
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

    // 1. Background Surface
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    canvas.drawCircle(cx, cy, radius, bgPaint)

    val primaryTextColor = if (isLight) Color(0xFF161618).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF8E8E93).toArgb() else Color(0x99FFFFFF).toArgb()

    val totalHeight = 2f * radius
    val circleTop = cy - radius
    val circleBottom = cy + radius
    val circleLeft = cx - radius
    val circleRight = cx + radius

    // 2. Top Display Section (26% Height)
    val displayHeight = totalHeight * 0.26f
    val displayCy = circleTop + (displayHeight / 2f)

    val exprText = calcState.expression.ifEmpty { " " }
    val exprPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = minOf(displayHeight * 0.18f, radius * 0.11f)
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(exprText.takeLast(20), cx, displayCy - (displayHeight * -0.05f), exprPaint)

    val resultPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = minOf(displayHeight * 0.30f, radius * 0.26f)
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val fmRes = resultPaint.fontMetrics
    val resY = displayCy + (displayHeight * 0.28f) - ((fmRes.descent + fmRes.ascent) / 2f)
    canvas.drawText(calcState.resultText, cx, resY, resultPaint)

    // 3. Circle Clip Path
    val circleClipPath = Path().apply {
        addCircle(cx, cy, radius - (1.5f * density), Path.Direction.CW)
    }

    canvas.save()
    canvas.clipPath(circleClipPath)

    // 4. Keypad Grid Setup (74% Height)
    val keypadTop = circleTop + displayHeight
    val keypadHeight = totalHeight * 0.74f
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

    // Rows 1 to 4 (Main 4x4 Grid)
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

            // =========================================================================
            // PROPORTIONAL ALIGNMENT CONFIGURATION (ROWS 1-4)
            // =========================================================================
            val (alignMode, dxFactor, dyFactor) = when (key) {
                "AC" -> Triple(Paint.Align.CENTER, 0.08f, 0.0f)   // Shift right by 8% of cell width
                "7"  -> Triple(Paint.Align.CENTER, 0.0f, 0.0f)   // Shift right by 5% of cell width
                "1"  -> Triple(Paint.Align.CENTER, 0.20f, 0.0f)   // Shift right by 12% of cell width
                "÷"  -> Triple(Paint.Align.CENTER, -0.08f, 0.0f)  // Shift left by 8% of cell width
                "×"  -> Triple(Paint.Align.CENTER, -0.0f, 0.0f)  // Shift left by 5% of cell width
                "+"  -> Triple(Paint.Align.CENTER, -0.20f, 0.0f)  // Shift left by 12% of cell width
                else -> Triple(Paint.Align.CENTER, 0.0f, 0.0f)    // Centered
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

        // =========================================================================
        // PROPORTIONAL ALIGNMENT CONFIGURATION (ROW 5)
        // =========================================================================
        val (alignMode, dxFactor, dyFactor) = when (key) {
            "0"  -> Triple(Paint.Align.CENTER, 0.24f, -0.05f) // Shift left by 18% of bottom button width
            "."  -> Triple(Paint.Align.CENTER, -0.24f, -0.05f)  // Shift right by 18% of bottom button width
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

    // 5. Outer Circle Border Line
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0x1A000000).toArgb() else Color(0x22FFFFFF).toArgb()
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }
    canvas.drawCircle(cx, cy, radius - (1f * density), borderPaint)

    return bitmap
}