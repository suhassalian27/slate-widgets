package com.altusix.slate.widgets.quotes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.altusix.slate.R
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.createSupersampledCanvas
import com.altusix.slate.utils.getSafeBgColor
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius

// =========================================================================
// CANVAS BITMAP GENERATORS FOR SLATE "QUOTES" WIDGETS
// Minimal, clean, typography-forward, and breathable home-screen widgets.
// =========================================================================

/**
 * Clean multiline text wrapping helper with smart ellipsis truncation.
 */
private fun drawCleanWrappedText(
    canvas: Canvas,
    text: String,
    x: Float,
    startY: Float,
    maxWidth: Float,
    paint: Paint,
    lineSpacing: Float,
    maxLines: Int = 4,
    bottomLimit: Float = Float.MAX_VALUE,
    align: Paint.Align = Paint.Align.LEFT
): Float {
    if (text.isBlank() || maxLines <= 0) return startY
    val originalAlign = paint.textAlign
    paint.textAlign = align

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
                    val isFinalLine =
                        (linesDrawn == maxLines - 1) || (currentY + lineSpacing > bottomLimit)
                    if (isFinalLine) {
                        var truncated = currentLine
                        while (truncated.isNotEmpty() && paint.measureText("$truncated…") > maxWidth) {
                            truncated = truncated.dropLast(1)
                        }
                        canvas.drawText("$truncated…", x, currentY, paint)
                        linesDrawn++
                        paint.textAlign = originalAlign
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
            val isFinalLine =
                (linesDrawn == maxLines - 1 && !isLastRawLine) || (currentY + lineSpacing > bottomLimit && !isLastRawLine)
            if (isFinalLine) {
                var truncated = currentLine
                while (truncated.isNotEmpty() && paint.measureText("$truncated…") > maxWidth) {
                    truncated = truncated.dropLast(1)
                }
                canvas.drawText("$truncated…", x, currentY, paint)
                linesDrawn++
                paint.textAlign = originalAlign
                return currentY + lineSpacing
            } else {
                canvas.drawText(currentLine, x, currentY, paint)
            }
            currentY += lineSpacing
            linesDrawn++
        } else if (rawLine.isEmpty()) {
            if (currentY + lineSpacing > bottomLimit || linesDrawn >= maxLines) {
                paint.textAlign = originalAlign
                return currentY
            }
            currentY += lineSpacing
            linesDrawn++
        }
    }
    paint.textAlign = originalAlign
    return currentY
}

// -----------------------------------------------------------------------------
// MINIMALIST VECTOR ACCENT HELPERS
// -----------------------------------------------------------------------------

/**
 * Modern double quotation marks glyph (“ or ”)
 */
private fun drawModernDoubleQuotes(
    canvas: Canvas,
    x: Float,
    y: Float,
    size: Float,
    color: Int,
    isClosing: Boolean = false
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    val markW = size * 0.38f
    val markH = size * 0.75f
    val gap = size * 0.22f
    val corner = size * 0.12f

    // Draw two marks side by side
    val leftRect = RectF(x, y, x + markW, y + markH)
    val rightRect = RectF(x + markW + gap, y, x + (markW * 2) + gap, y + markH)

    canvas.drawRoundRect(leftRect, corner, corner, paint)
    canvas.drawRoundRect(rightRect, corner, corner, paint)
}

/**
 * Clean radiant geometric starburst / snowflake emblem
 */
private fun drawSparkEmblem(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = size * 0.12f
        strokeCap = Paint.Cap.ROUND
    }
    val half = size / 2f

    // Main cross (horizontal & vertical)
    canvas.drawLine(cx - half, cy, cx + half, cy, strokePaint)
    canvas.drawLine(cx, cy - half, cx, cy + half, strokePaint)

    // Diagonals
    val diag = half * 0.72f
    canvas.drawLine(cx - diag, cy - diag, cx + diag, cy + diag, strokePaint)
    canvas.drawLine(cx - diag, cy + diag, cx + diag, cy - diag, strokePaint)

    // Center radiant circle
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, size * 0.15f, fillPaint)
}

/**
 * Minimalist smiley face outline glyph
 */
private fun drawSmileGlyph(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int) {
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = radius * 0.18f
        strokeCap = Paint.Cap.ROUND
    }

    // Outer circle
    canvas.drawCircle(cx, cy, radius, strokePaint)

    // Eyes
    val eyeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    val eyeOffsetX = radius * 0.36f
    val eyeOffsetY = radius * 0.22f
    val eyeRadius = radius * 0.14f
    canvas.drawCircle(cx - eyeOffsetX, cy - eyeOffsetY, eyeRadius, eyeFill)
    canvas.drawCircle(cx + eyeOffsetX, cy - eyeOffsetY, eyeRadius, eyeFill)

    // Smile arc
    val smileRect =
        RectF(cx - radius * 0.52f, cy - radius * 0.10f, cx + radius * 0.52f, cy + radius * 0.56f)
    canvas.drawArc(smileRect, 25f, 130f, false, strokePaint)
}

/**
 * Minimalist lotus / sprout outline glyph
 */
private fun drawLotusGlyph(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = size * 0.11f
        strokeCap = Paint.Cap.ROUND
    }
    val path = Path().apply {
        // Center petal
        moveTo(cx, cy - size * 0.45f)
        quadTo(cx - size * 0.25f, cy, cx, cy + size * 0.40f)
        quadTo(cx + size * 0.25f, cy, cx, cy - size * 0.45f)
    }
    canvas.drawPath(path, strokePaint)

    // Left outer curve
    val leftPath = Path().apply {
        moveTo(cx - size * 0.42f, cy - size * 0.15f)
        quadTo(cx - size * 0.35f, cy + size * 0.32f, cx, cy + size * 0.40f)
    }
    canvas.drawPath(leftPath, strokePaint)

    // Right outer curve
    val rightPath = Path().apply {
        moveTo(cx + size * 0.42f, cy - size * 0.15f)
        quadTo(cx + size * 0.35f, cy + size * 0.32f, cx, cy + size * 0.40f)
    }
    canvas.drawPath(rightPath, strokePaint)
}

// -----------------------------------------------------------------------------
// THEME RESOLUTION
// -----------------------------------------------------------------------------

private data class ResolvedTheme(
    val isLight: Boolean,
    val bg: Int,
    val primaryText: Int,
    val secondaryText: Int,
    val subtleBorder: Int,
    val accent: Int
)

private fun resolveTheme(config: SlateWidgetConfig): ResolvedTheme {
    val isLight = config.themeMode == "LIGHT" || (
            (((config.backgroundColorHex shr 16 and 0xFFL) * 0.2126f +
                    (config.backgroundColorHex shr 8 and 0xFFL) * 0.7152f +
                    (config.backgroundColorHex and 0xFFL) * 0.0722f) / 255f) > 0.5f
            )
    val accent = config.accentColorHex.toInt()
    val bg = getSafeBgColor(config)

    return if (isLight) {
        ResolvedTheme(
            isLight = true,
            bg = bg,
            primaryText = 0xFF111111.toInt(),
            secondaryText = 0xFF666666.toInt(),
            subtleBorder = 0x18000000.toInt(),
            accent = accent
        )
    } else {
        ResolvedTheme(
            isLight = false,
            bg = bg,
            primaryText = 0xFFFFFFFF.toInt(),
            secondaryText = 0xFF8E8E93.toInt(),
            subtleBorder = 0x1AFFFFFF.toInt(),
            accent = accent
        )
    }
}

// =========================================================================
// 1. EDITORIAL PULL QUOTE (4x2)
// High-fashion, breathable editorial spread with accent quotes and serif body
// =========================================================================

fun generateEditorialQuoteBitmap(
    context: Context,
    quote: QuoteItem,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val theme = resolveTheme(config)
    val cardRect = RectF(0f, 0f, wDp * scaleFactor, hDp * scaleFactor)
    val cornerRadius = getStandardCornerRadius(scaleFactor)

    // Base background & border
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.bg }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.subtleBorder
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

    val padH = 28f * scaleFactor
    val padV = 22f * scaleFactor

    // Top-left accent quotation marks “
    drawModernDoubleQuotes(canvas, padH, padV, 18f * scaleFactor, theme.accent)

    // Body font: Elegant, readable serif
    val serifTypeface = ResourcesCompat.getFont(context, R.font.saint_regular) ?: Typeface.create(
        Typeface.SERIF,
        Typeface.NORMAL
    )
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = serifTypeface
        textSize = 17.5f * scaleFactor
        color = theme.primaryText
    }
    val contentW = cardRect.width() - (padH * 2)
    val startY = padV + 38f * scaleFactor
    val lineSpacing = 24f * scaleFactor

    val endY = drawCleanWrappedText(
        canvas = canvas,
        text = quote.text,
        x = padH,
        startY = startY,
        maxWidth = contentW,
        paint = bodyPaint,
        lineSpacing = lineSpacing,
        maxLines = 3,
        bottomLimit = cardRect.bottom - padV - 20f * scaleFactor
    )

    // Bottom-right closing quotation marks ”
    drawModernDoubleQuotes(
        canvas,
        cardRect.right - padH - 14f * scaleFactor,
        cardRect.bottom - padV - 14f * scaleFactor,
        16f * scaleFactor,
        theme.accent,
        isClosing = true
    )

    // Author line (Centered or Bottom-Left)
    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        textSize = 10.5f * scaleFactor
        color = theme.secondaryText
    }
    val authorText =
        if (quote.sourceBook.isNotBlank()) "— ${quote.author}, ${quote.sourceBook}" else "— ${quote.author}"
    canvas.drawText(authorText, padH, cardRect.bottom - padV, authorPaint)

    return bitmap
}

// =========================================================================
// 2. RADIANT MANTRA (2x2)
// Minimalist dark card with geometric spark/snowflake emblem and bold quote
// =========================================================================

fun generateRadiantMantraBitmap(
    context: Context,
    quote: QuoteItem,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val theme = resolveTheme(config)
    val cardRect = RectF(0f, 0f, wDp * scaleFactor, hDp * scaleFactor)
    val cornerRadius = getStandardCornerRadius(scaleFactor)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.bg }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.subtleBorder
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

    val cx = cardRect.centerX()
    val pad = 20f * scaleFactor

    // Top geometric radiant spark emblem
    drawSparkEmblem(canvas, cx, pad + 16f * scaleFactor, 24f * scaleFactor, theme.accent)

    // Center bold clean statement typography
    val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 600)
        textSize = 14.5f * scaleFactor
        color = theme.primaryText
    }
    val contentW = cardRect.width() - (pad * 2)
    val startY = cardRect.centerY() - 6f * scaleFactor
    val lineSpacing = 20f * scaleFactor

    drawCleanWrappedText(
        canvas = canvas,
        text = quote.text,
        x = cx,
        startY = startY,
        maxWidth = contentW,
        paint = quotePaint,
        lineSpacing = lineSpacing,
        maxLines = 4,
        bottomLimit = cardRect.bottom - pad - 18f * scaleFactor,
        align = Paint.Align.CENTER
    )

    // Bottom subtle author line
    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        textSize = 9.5f * scaleFactor
        color = theme.secondaryText
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(quote.author, cx, cardRect.bottom - pad, authorPaint)

    return bitmap
}

// =========================================================================
// 3. GOLDEN HOUR AURA (2x2)
// User-favorite ambient radial glow aura behind modern clean typography
// =========================================================================

fun generateGoldenHourCardBitmap(
    context: Context,
    quote: QuoteItem,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val theme = resolveTheme(config)
    val cardRect = RectF(0f, 0f, wDp * scaleFactor, hDp * scaleFactor)
    val cornerRadius = getStandardCornerRadius(scaleFactor)

    // Base background
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.bg }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    // Warm radial glow aura originating from top-right
    val auraCx = cardRect.right - (10f * scaleFactor)
    val auraCy = cardRect.top + (10f * scaleFactor)
    val auraRadius = cardRect.width() * 0.90f
    val glowColor = theme.accent
    val auraShader = RadialGradient(
        auraCx, auraCy, auraRadius,
        intArrayOf(
            Color(glowColor).copy(alpha = if (theme.isLight) 0.25f else 0.32f).toArgb(),
            Color(glowColor).copy(alpha = 0.08f).toArgb(),
            Color.Transparent.toArgb()
        ),
        floatArrayOf(0f, 0.55f, 1f),
        Shader.TileMode.CLAMP
    )
    val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = auraShader }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, auraPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.subtleBorder
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

    val pad = 20f * scaleFactor

    // Top Category Pill
    val tagText = "• ${quote.category.uppercase()} •"
    val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 700)
        textSize = 8.5f * scaleFactor
        color = theme.accent
        letterSpacing = 0.12f
    }
    canvas.drawText(tagText, pad, pad + 10f * scaleFactor, tagPaint)

    // Main Quote Body
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 600)
        textSize = 14.5f * scaleFactor
        color = theme.primaryText
    }
    val contentW = cardRect.width() - (pad * 2)
    val startY = pad + 36f * scaleFactor
    val lineSpacing = 20f * scaleFactor

    drawCleanWrappedText(
        canvas = canvas,
        text = "“${quote.text}”",
        x = pad,
        startY = startY,
        maxWidth = contentW,
        paint = bodyPaint,
        lineSpacing = lineSpacing,
        maxLines = 4,
        bottomLimit = cardRect.bottom - pad - 22f * scaleFactor
    )

    // Bottom Author Line with glowing dot
    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        textSize = 10f * scaleFactor
        color = theme.secondaryText
    }
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.accent
        style = Paint.Style.FILL
    }
    val authorY = cardRect.bottom - pad
    canvas.drawCircle(
        pad + 3f * scaleFactor,
        authorY - 3.5f * scaleFactor,
        2.5f * scaleFactor,
        dotPaint
    )
    canvas.drawText(quote.author, pad + 10f * scaleFactor, authorY, authorPaint)

    return bitmap
}

// =========================================================================
// 4. PUNCHY HORIZON BANNER (4x1)
// Clean horizontal dock strip: Left accent icon + bold readable statement
// =========================================================================

fun generatePunchyHorizonBitmap(
    context: Context,
    quote: QuoteItem,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val theme = resolveTheme(config)
    val cardRect = RectF(0f, 0f, wDp * scaleFactor, hDp * scaleFactor)
    val cornerRadius = getStandardCornerRadius(scaleFactor)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.bg }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.subtleBorder
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

    val padH = 18f * scaleFactor
    val cy = cardRect.centerY()

    // Left accent smile or spark glyph
    drawSmileGlyph(canvas, padH + 12f * scaleFactor, cy, 12f * scaleFactor, theme.accent)

    // Statement typography
    val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 600)
        textSize = 14f * scaleFactor
        color = theme.primaryText
        letterSpacing = -0.01f
    }

    val textStartX = padH + 34f * scaleFactor
    val availableW = cardRect.width() - textStartX - padH

    // Format single line or 2 clean lines
    drawCleanWrappedText(
        canvas = canvas,
        text = quote.text,
        x = textStartX,
        startY = cy - 2f * scaleFactor,
        maxWidth = availableW,
        paint = quotePaint,
        lineSpacing = 18f * scaleFactor,
        maxLines = 2,
        bottomLimit = cardRect.bottom - 4f * scaleFactor
    )

    return bitmap
}

// =========================================================================
// 5. TWO-TONE INSIGHT (2x2)
// Minimalist 2-tone typography: Line 1 in white, key insight in accent color
// =========================================================================

fun generateTwoToneInsightBitmap(
    context: Context,
    quote: QuoteItem,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val theme = resolveTheme(config)
    val cardRect = RectF(0f, 0f, wDp * scaleFactor, hDp * scaleFactor)
    val cornerRadius = getStandardCornerRadius(scaleFactor)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.bg }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.subtleBorder
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

    val cx = cardRect.centerX()
    val pad = 20f * scaleFactor

    // Split quote into two parts if punctuated, or first half / second half
    val parts = if (quote.text.contains(".")) {
        val split = quote.text.split(".", limit = 2)
        Pair(split[0] + ".", split.getOrNull(1)?.trim() ?: "")
    } else if (quote.text.contains("?")) {
        val split = quote.text.split("?", limit = 2)
        Pair(split[0] + "?", split.getOrNull(1)?.trim() ?: "")
    } else if (quote.text.contains("—") || quote.text.contains("-")) {
        val split = quote.text.split(Regex("[—-]", RegexOption.IGNORE_CASE), limit = 2)
        Pair(split[0].trim(), split.getOrNull(1)?.trim() ?: "")
    } else {
        val words = quote.text.split(" ")
        val mid = words.size / 2
        Pair(words.take(mid).joinToString(" "), words.drop(mid).joinToString(" "))
    }

    val part1Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        textSize = 14f * scaleFactor
        color = theme.primaryText
    }

    val part2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 700)
        textSize = 14.5f * scaleFactor
        color = theme.accent
    }

    val contentW = cardRect.width() - (pad * 2)
    val lineSpacing = 19f * scaleFactor
    val startY = cardRect.centerY() - 22f * scaleFactor

    val endY = drawCleanWrappedText(
        canvas = canvas,
        text = parts.first,
        x = cx,
        startY = startY,
        maxWidth = contentW,
        paint = part1Paint,
        lineSpacing = lineSpacing,
        maxLines = 2,
        align = Paint.Align.CENTER
    )

    if (parts.second.isNotBlank()) {
        drawCleanWrappedText(
            canvas = canvas,
            text = parts.second,
            x = cx,
            startY = endY + 2f * scaleFactor,
            maxWidth = contentW,
            paint = part2Paint,
            lineSpacing = lineSpacing,
            maxLines = 2,
            bottomLimit = cardRect.bottom - pad - 16f * scaleFactor,
            align = Paint.Align.CENTER
        )
    }

    // Bottom author line
    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        textSize = 9.5f * scaleFactor
        color = theme.secondaryText
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(quote.author, cx, cardRect.bottom - pad, authorPaint)

    return bitmap
}

// =========================================================================
// 6. MODERN ACCENT QUOTES SPREAD (4x2)
// Clean, bold statement flanked by accent quotes (top-left & bottom-right)
// =========================================================================

fun generateModernQuotesSpreadBitmap(
    context: Context,
    quote: QuoteItem,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val theme = resolveTheme(config)
    val cardRect = RectF(0f, 0f, wDp * scaleFactor, hDp * scaleFactor)
    val cornerRadius = getStandardCornerRadius(scaleFactor)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.bg }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.subtleBorder
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

    val padH = 28f * scaleFactor
    val padV = 22f * scaleFactor

    // Top-left accent quote marks
    drawModernDoubleQuotes(canvas, padH, padV, 18f * scaleFactor, theme.accent)

    // Center bold statement typography
    val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 700)
        textSize = 18f * scaleFactor
        color = theme.primaryText
        letterSpacing = -0.01f
    }
    val contentW = cardRect.width() - (padH * 2)
    val startY = padV + 42f * scaleFactor
    val lineSpacing = 24f * scaleFactor

    drawCleanWrappedText(
        canvas = canvas,
        text = quote.text,
        x = padH,
        startY = startY,
        maxWidth = contentW,
        paint = quotePaint,
        lineSpacing = lineSpacing,
        maxLines = 3,
        bottomLimit = cardRect.bottom - padV - 20f * scaleFactor
    )

    // Bottom-right closing quote marks
    drawModernDoubleQuotes(
        canvas,
        cardRect.right - padH - 14f * scaleFactor,
        cardRect.bottom - padV - 14f * scaleFactor,
        16f * scaleFactor,
        theme.accent,
        isClosing = true
    )

    // Bottom-center or left author
    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        textSize = 11f * scaleFactor
        color = theme.secondaryText
    }
    canvas.drawText(quote.author, padH, cardRect.bottom - padV, authorPaint)

    return bitmap
}

// =========================================================================
// 7. MINDFUL SMILE CARD (2x2)
// Clean mindful affirmation with smiling glyph at the top
// =========================================================================

fun generateMindfulSmileBitmap(
    context: Context,
    quote: QuoteItem,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val theme = resolveTheme(config)
    val cardRect = RectF(0f, 0f, wDp * scaleFactor, hDp * scaleFactor)
    val cornerRadius = getStandardCornerRadius(scaleFactor)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.bg }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.subtleBorder
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

    val cx = cardRect.centerX()
    val pad = 20f * scaleFactor

    // Top minimal smile glyph
    drawSmileGlyph(canvas, cx, pad + 16f * scaleFactor, 13f * scaleFactor, theme.accent)

    // Centered serene statement
    val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 600)
        textSize = 14f * scaleFactor
        color = theme.primaryText
    }
    val contentW = cardRect.width() - (pad * 2)
    val startY = cardRect.centerY() - 6f * scaleFactor
    val lineSpacing = 20f * scaleFactor

    drawCleanWrappedText(
        canvas = canvas,
        text = quote.text,
        x = cx,
        startY = startY,
        maxWidth = contentW,
        paint = quotePaint,
        lineSpacing = lineSpacing,
        maxLines = 4,
        bottomLimit = cardRect.bottom - pad - 18f * scaleFactor,
        align = Paint.Align.CENTER
    )

    // Author
    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        textSize = 9.5f * scaleFactor
        color = theme.secondaryText
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(quote.author, cx, cardRect.bottom - pad, authorPaint)

    return bitmap
}

// =========================================================================
// 8. DAILY INSIGHT BANNER (4x2)
// Header row with "Today's Insight" and accent icon, large readable quote
// =========================================================================

fun generateDailyInsightBitmap(
    context: Context,
    quote: QuoteItem,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val theme = resolveTheme(config)
    val cardRect = RectF(0f, 0f, wDp * scaleFactor, hDp * scaleFactor)
    val cornerRadius = getStandardCornerRadius(scaleFactor)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.bg }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.subtleBorder
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

    val padH = 26f * scaleFactor
    val padV = 20f * scaleFactor

    // Top Header: "Today's Insight" + Accent smile/spark icon on the right
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 600)
        textSize = 10f * scaleFactor
        color = theme.secondaryText
        letterSpacing = 0.06f
    }
    canvas.drawText("Today's Insight", padH, padV + 8f * scaleFactor, headerPaint)

    drawSmileGlyph(
        canvas,
        cardRect.right - padH - 6f * scaleFactor,
        padV + 4f * scaleFactor,
        9f * scaleFactor,
        theme.accent
    )

    // Clean prominent statement
    val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 600)
        textSize = 16.5f * scaleFactor
        color = theme.primaryText
    }
    val contentW = cardRect.width() - (padH * 2)
    val startY = padV + 38f * scaleFactor
    val lineSpacing = 23f * scaleFactor

    drawCleanWrappedText(
        canvas = canvas,
        text = quote.text,
        x = padH,
        startY = startY,
        maxWidth = contentW,
        paint = quotePaint,
        lineSpacing = lineSpacing,
        maxLines = 3,
        bottomLimit = cardRect.bottom - padV - 18f * scaleFactor
    )

    // Author
    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        textSize = 10.5f * scaleFactor
        color = theme.secondaryText
    }
    canvas.drawText("— ${quote.author}", padH, cardRect.bottom - padV, authorPaint)

    return bitmap
}

// =========================================================================
// 9. MINIMAL CAPSULE (2x1)
// Compact 2x1 card with accent glyph on the left and punchy mantra on the right
// =========================================================================

fun generateMinimalCapsuleBitmap(
    context: Context,
    quote: QuoteItem,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val theme = resolveTheme(config)
    val cardRect = RectF(0f, 0f, wDp * scaleFactor, hDp * scaleFactor)
    val cornerRadius = getStandardCornerRadius(scaleFactor)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.bg }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.subtleBorder
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

    val cy = cardRect.centerY()
    val padH = 16f * scaleFactor

    // Left accent spark
    drawSparkEmblem(canvas, padH + 10f * scaleFactor, cy, 14f * scaleFactor, theme.accent)

    val textStartX = padH + 28f * scaleFactor
    val availableW = cardRect.width() - textStartX - padH

    val mantraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 600)
        textSize = 13.5f * scaleFactor
        color = theme.primaryText
        letterSpacing = -0.01f
    }

    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        textSize = 9f * scaleFactor
        color = theme.secondaryText
    }

    var displayMantra = quote.text
    if (mantraPaint.measureText(displayMantra) > availableW) {
        while (displayMantra.isNotEmpty() && mantraPaint.measureText("$displayMantra…") > availableW) {
            displayMantra = displayMantra.dropLast(1)
        }
        displayMantra = "$displayMantra…"
    }

    canvas.drawText(displayMantra, textStartX, cy - 2f * scaleFactor, mantraPaint)
    canvas.drawText("— ${quote.author}", textStartX, cy + 12f * scaleFactor, authorPaint)

    return bitmap
}

// =========================================================================
// 10. BOLD CONDENSED STATEMENT (4x2)
// High-impact all-caps statement with accent quote marks top-left & bottom-right
// =========================================================================

fun generateBoldCondensedBitmap(
    context: Context,
    quote: QuoteItem,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val theme = resolveTheme(config)
    val cardRect = RectF(0f, 0f, wDp * scaleFactor, hDp * scaleFactor)
    val cornerRadius = getStandardCornerRadius(scaleFactor)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.bg }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.subtleBorder
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

    val padH = 28f * scaleFactor
    val padV = 22f * scaleFactor

    // Top-left accent quote marks
    drawModernDoubleQuotes(canvas, padH, padV, 18f * scaleFactor, theme.accent)

    // Center bold all-caps statement
    val boldFont = getSlateFont(context, weight = 800)
    val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = boldFont
        textSize = 17f * scaleFactor
        color = theme.primaryText
        letterSpacing = 0.02f
    }
    val contentW = cardRect.width() - (padH * 2)
    val startY = padV + 42f * scaleFactor
    val lineSpacing = 23f * scaleFactor

    drawCleanWrappedText(
        canvas = canvas,
        text = quote.text.uppercase(),
        x = padH,
        startY = startY,
        maxWidth = contentW,
        paint = quotePaint,
        lineSpacing = lineSpacing,
        maxLines = 3,
        bottomLimit = cardRect.bottom - padV - 20f * scaleFactor
    )

    // Bottom-right closing quote marks
    drawModernDoubleQuotes(
        canvas,
        cardRect.right - padH - 14f * scaleFactor,
        cardRect.bottom - padV - 14f * scaleFactor,
        16f * scaleFactor,
        theme.accent,
        isClosing = true
    )

    // Author
    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = boldFont
        textSize = 10f * scaleFactor
        color = theme.secondaryText
        letterSpacing = 0.06f
    }
    canvas.drawText(quote.author.uppercase(), padH, cardRect.bottom - padV, authorPaint)

    return bitmap
}
