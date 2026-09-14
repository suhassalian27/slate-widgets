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
// Minimal, clean, typography-forward, auto-scaling home-screen widgets.
// =========================================================================

// -----------------------------------------------------------------------------
// DYNAMIC TEXT MEASUREMENT & AUTO-SCALING ENGINE
// -----------------------------------------------------------------------------

private data class AutoFitResult(
    val lines: List<String>,
    val textSize: Float,
    val lineSpacing: Float,
    val totalHeight: Float,
    val ascent: Float,
    val descent: Float
)

/**
 * Splits text into wrapped lines respecting word boundaries and container width.
 */
private fun breakTextIntoLines(text: String, paint: Paint, maxWidth: Float): List<String> {
    if (text.isBlank()) return emptyList()
    val result = mutableListOf<String>()
    val paragraphs = text.split("\n")

    for (paragraph in paragraphs) {
        if (paragraph.isEmpty()) {
            result.add("")
            continue
        }
        val words = paragraph.split(" ")
        var currentLine = ""

        for (word in words) {
            if (word.isEmpty()) continue
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"

            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    result.add(currentLine)
                    currentLine = ""
                }
                // Handle individual words wider than maxWidth
                if (paint.measureText(word) > maxWidth) {
                    var sub = ""
                    for (ch in word) {
                        if (paint.measureText(sub + ch) <= maxWidth) {
                            sub += ch
                        } else {
                            if (sub.isNotEmpty()) result.add(sub)
                            sub = ch.toString()
                        }
                    }
                    currentLine = sub
                } else {
                    currentLine = word
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            result.add(currentLine)
        }
    }
    return result
}

/**
 * Iteratively scales down text size and proportional line spacing until the entire text
 * fits within [availableHeight] and [maxLines].
 */
private fun autoFitTextLayout(
    text: String,
    paint: Paint,
    maxWidth: Float,
    availableHeight: Float,
    initialTextSize: Float,
    minTextSize: Float,
    lineSpacingRatio: Float = 1.34f,
    maxLines: Int = 8
): AutoFitResult {
    var size = initialTextSize
    val step = maxOf(0.4f, (initialTextSize - minTextSize) / 14f)

    while (size >= minTextSize) {
        paint.textSize = size
        val spacing = size * lineSpacingRatio
        val lines = breakTextIntoLines(text, paint, maxWidth)
        val ascent = -paint.ascent()
        val descent = paint.descent()
        val blockHeight = if (lines.isEmpty()) 0f else (lines.size - 1) * spacing + ascent + descent

        if (lines.size <= maxLines && blockHeight <= availableHeight) {
            return AutoFitResult(lines, size, spacing, blockHeight, ascent, descent)
        }
        size -= step
    }

    // Min size fallback: fit as many lines as possible and gracefully ellipsize the last line
    paint.textSize = minTextSize
    val spacing = minTextSize * lineSpacingRatio
    val lines = breakTextIntoLines(text, paint, maxWidth)
    val ascent = -paint.ascent()
    val descent = paint.descent()

    val maxFitLines = maxOf(1, ((availableHeight - ascent - descent) / spacing).toInt() + 1)
    val visibleLineCount = minOf(lines.size, minOf(maxLines, maxFitLines))

    val finalLines = mutableListOf<String>()
    for (i in 0 until visibleLineCount) {
        if (i == visibleLineCount - 1 && visibleLineCount < lines.size) {
            var elided = lines[i]
            while (elided.isNotEmpty() && paint.measureText("$elided…") > maxWidth) {
                elided = elided.dropLast(1)
            }
            finalLines.add(if (elided.isNotEmpty()) "$elided…" else lines[i])
        } else {
            finalLines.add(lines[i])
        }
    }

    val blockHeight = if (finalLines.isEmpty()) 0f else (finalLines.size - 1) * spacing + ascent + descent
    return AutoFitResult(finalLines, minTextSize, spacing, blockHeight, ascent, descent)
}

/**
 * Draws auto-fitted text with optional vertical centering within [topLimit, bottomLimit].
 */
private fun drawAutoFitText(
    canvas: Canvas,
    text: String,
    x: Float,
    topLimit: Float,
    bottomLimit: Float,
    maxWidth: Float,
    paint: Paint,
    initialTextSize: Float,
    minTextSize: Float,
    lineSpacingRatio: Float = 1.34f,
    maxLines: Int = 8,
    align: Paint.Align = Paint.Align.LEFT,
    verticalCenter: Boolean = false
): Float {
    if (text.isBlank()) return topLimit
    val availableHeight = maxOf(10f, bottomLimit - topLimit)
    val result = autoFitTextLayout(
        text = text,
        paint = paint,
        maxWidth = maxWidth,
        availableHeight = availableHeight,
        initialTextSize = initialTextSize,
        minTextSize = minTextSize,
        lineSpacingRatio = lineSpacingRatio,
        maxLines = maxLines
    )

    paint.textSize = result.textSize
    val prevAlign = paint.textAlign
    paint.textAlign = align

    val startBaselineY = if (verticalCenter) {
        val extraMargin = maxOf(0f, (availableHeight - result.totalHeight) / 2f)
        topLimit + extraMargin + result.ascent
    } else {
        topLimit + result.ascent
    }

    var currentY = startBaselineY
    for (line in result.lines) {
        canvas.drawText(line, x, currentY, paint)
        currentY += result.lineSpacing
    }

    paint.textAlign = prevAlign
    return currentY - result.lineSpacing + result.descent
}

// -----------------------------------------------------------------------------
// MINIMALIST VECTOR ACCENT HELPERS
// -----------------------------------------------------------------------------

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

    val leftRect = RectF(x, y, x + markW, y + markH)
    val rightRect = RectF(x + markW + gap, y, x + (markW * 2) + gap, y + markH)

    canvas.drawRoundRect(leftRect, corner, corner, paint)
    canvas.drawRoundRect(rightRect, corner, corner, paint)
}

private fun drawSparkEmblem(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = size * 0.12f
        strokeCap = Paint.Cap.ROUND
    }
    val half = size / 2f

    canvas.drawLine(cx - half, cy, cx + half, cy, strokePaint)
    canvas.drawLine(cx, cy - half, cx, cy + half, strokePaint)

    val diag = half * 0.72f
    canvas.drawLine(cx - diag, cy - diag, cx + diag, cy + diag, strokePaint)
    canvas.drawLine(cx - diag, cy + diag, cx + diag, cy - diag, strokePaint)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, size * 0.15f, fillPaint)
}

private fun drawSmileGlyph(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int) {
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = radius * 0.18f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawCircle(cx, cy, radius, strokePaint)

    val eyeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    val eyeOffsetX = radius * 0.36f
    val eyeOffsetY = radius * 0.22f
    val eyeRadius = radius * 0.14f
    canvas.drawCircle(cx - eyeOffsetX, cy - eyeOffsetY, eyeRadius, eyeFill)
    canvas.drawCircle(cx + eyeOffsetX, cy - eyeOffsetY, eyeRadius, eyeFill)

    val smileRect = RectF(cx - radius * 0.52f, cy - radius * 0.10f, cx + radius * 0.52f, cy + radius * 0.56f)
    canvas.drawArc(smileRect, 25f, 130f, false, strokePaint)
}

private fun drawVectorDrawable(
    context: Context,
    canvas: Canvas,
    @androidx.annotation.DrawableRes resId: Int,
    cx: Float,
    cy: Float,
    size: Float,
    color: Int
) {
    val drawable = androidx.core.content.ContextCompat.getDrawable(context, resId)?.mutate() ?: return
    drawable.setTint(color)
    val half = size / 2f
    drawable.setBounds(
        (cx - half).toInt(),
        (cy - half).toInt(),
        (cx + half).toInt(),
        (cy + half).toInt()
    )
    drawable.draw(canvas)
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

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.bg }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.subtleBorder
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

    val padH = 28f * scaleFactor
    val padV = 20f * scaleFactor

    // Top-left accent quotes
    drawModernDoubleQuotes(canvas, padH, padV, 18f * scaleFactor, theme.accent)

    // Bottom author line
    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        textSize = 10.5f * scaleFactor
        color = theme.secondaryText
    }
    val authorText = if (quote.sourceBook.isNotBlank()) "— ${quote.author}, ${quote.sourceBook}" else "— ${quote.author}"
    val authorY = cardRect.bottom - padV
    canvas.drawText(authorText, padH, authorY, authorPaint)

    // Bottom-right closing quotes
    drawModernDoubleQuotes(
        canvas,
        cardRect.right - padH - 14f * scaleFactor,
        cardRect.bottom - padV - 14f * scaleFactor,
        16f * scaleFactor,
        theme.accent,
        isClosing = true
    )

    // Auto-fit body quote
    val serifTypeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = serifTypeface
        color = theme.primaryText
    }

    val topLimit = padV + (22f * scaleFactor)
    val bottomLimit = authorY - (14f * scaleFactor)
    val contentW = cardRect.width() - (padH * 2)

    drawAutoFitText(
        canvas = canvas,
        text = quote.text,
        x = padH,
        topLimit = topLimit,
        bottomLimit = bottomLimit,
        maxWidth = contentW,
        paint = bodyPaint,
        initialTextSize = 18f * scaleFactor,
        minTextSize = 11f * scaleFactor,
        maxLines = 6,
        verticalCenter = true
    )

    return bitmap
}

// =========================================================================
// 2. RADIANT MANTRA (2x2)
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
    val pad = 18f * scaleFactor

    // Top spark emblem
    drawSparkEmblem(canvas, cx, pad + 14f * scaleFactor, 22f * scaleFactor, theme.accent)

    // Bottom author
    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        textSize = 9.5f * scaleFactor
        color = theme.secondaryText
        textAlign = Paint.Align.CENTER
    }
    val authorY = cardRect.bottom - pad
    canvas.drawText(quote.author, cx, authorY, authorPaint)

    // Auto-fit statement
    val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 600)
        color = theme.primaryText
    }

    val topLimit = pad + (30f * scaleFactor)
    val bottomLimit = authorY - (12f * scaleFactor)
    val contentW = cardRect.width() - (pad * 2)

    drawAutoFitText(
        canvas = canvas,
        text = quote.text,
        x = cx,
        topLimit = topLimit,
        bottomLimit = bottomLimit,
        maxWidth = contentW,
        paint = quotePaint,
        initialTextSize = 15f * scaleFactor,
        minTextSize = 10f * scaleFactor,
        maxLines = 6,
        align = Paint.Align.CENTER,
        verticalCenter = true
    )

    return bitmap
}

// =========================================================================
// 3. MODERN ACCENT QUOTES SPREAD (4x2)
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

    // Card background
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.bg }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.subtleBorder
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

    val padH = 24f * scaleFactor
    val padV = 18f * scaleFactor

    // 1. Top Metadata Tag: // CATEGORY
    val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 700)
        textSize = 9.5f * scaleFactor
        color = theme.accent
        letterSpacing = 0.10f
    }
    val tagY = padV + (10f * scaleFactor)
    canvas.drawText("// ${quote.category.uppercase()}", padH, tagY, tagPaint)

    // 2. Bottom Author & Source
    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        textSize = 10.5f * scaleFactor
        color = theme.secondaryText
    }
    val authorText = if (quote.sourceBook.isNotBlank()) "— ${quote.author}, ${quote.sourceBook}" else "— ${quote.author}"
    val authorY = cardRect.bottom - padV
    val textLeft = padH + (14f * scaleFactor)
    canvas.drawText(authorText, textLeft, authorY, authorPaint)

    // 3. Layout Limits for Quote Block
    val topLimit = tagY + (12f * scaleFactor)
    val bottomLimit = authorY - (14f * scaleFactor)
    val contentW = cardRect.width() - textLeft - padH

    // 4. Vertical Accent Rail
    val railWidth = 3f * scaleFactor
    val railRect = RectF(
        padH,
        topLimit + (2f * scaleFactor),
        padH + railWidth,
        bottomLimit - (2f * scaleFactor)
    )
    val railPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.accent
        style = Paint.Style.FILL
    }
    val railRadius = railWidth / 2f
    canvas.drawRoundRect(railRect, railRadius, railRadius, railPaint)

    // 5. Auto-fit Modern Body Typography
    val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 600)
        color = theme.primaryText
        letterSpacing = -0.01f
    }

    drawAutoFitText(
        canvas = canvas,
        text = quote.text,
        x = textLeft,
        topLimit = topLimit,
        bottomLimit = bottomLimit,
        maxWidth = contentW,
        paint = quotePaint,
        initialTextSize = 17.5f * scaleFactor,
        minTextSize = 11.5f * scaleFactor,
        maxLines = 5,
        verticalCenter = true
    )

    return bitmap
}


// =========================================================================
// 4. PUNCHY HORIZON BANNER (4x1)
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

    // 1. Base card background & border
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.bg }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.subtleBorder
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

    // 2. Full-height background watermark clipped to the card squircle
    canvas.save()
    val clipPath = Path().apply {
        addRoundRect(cardRect, cornerRadius, cornerRadius, Path.Direction.CW)
    }
    canvas.clipPath(clipPath)

    // Full widget-height quote mark
    val watermarkSize = cardRect.height() * 0.96f
    val watermarkCx = cardRect.left + (watermarkSize * 0.52f)
    val watermarkCy = cardRect.centerY()

    val watermarkColor = Color(theme.accent)
        .copy(alpha = if (theme.isLight) 0.12f else 0.18f)
        .toArgb()

    // Rotate 180° around its center to transform 99 closing glyph into 66 opening glyph
    canvas.save()
    canvas.rotate(180f, watermarkCx, watermarkCy)
    drawVectorDrawable(
        context = context,
        canvas = canvas,
        resId = R.drawable.ic_quotes,
        cx = watermarkCx,
        cy = watermarkCy,
        size = watermarkSize,
        color = watermarkColor
    )
    canvas.restore()
    canvas.restore()

    // 3. Foreground Quote Text
    val padH = 22f * scaleFactor
    val availableW = cardRect.width() - (padH * 2)

    val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 600)
        color = theme.primaryText
        letterSpacing = -0.01f
    }

    drawAutoFitText(
        canvas = canvas,
        text = quote.text,
        x = padH,
        topLimit = cardRect.top + (8f * scaleFactor),
        bottomLimit = cardRect.bottom - (8f * scaleFactor),
        maxWidth = availableW,
        paint = quotePaint,
        initialTextSize = 14f * scaleFactor,
        minTextSize = 9.5f * scaleFactor,
        maxLines = 3,
        verticalCenter = true
    )

    return bitmap
}


// =========================================================================
// 5. GOLDEN HOUR AURA (2x2)
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

    val pad = 18f * scaleFactor

    // Bottom author line with accent dot
    val authorY = cardRect.bottom - pad
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.accent
        style = Paint.Style.FILL
    }
    canvas.drawCircle(pad + 3f * scaleFactor, authorY - 3.5f * scaleFactor, 2.5f * scaleFactor, dotPaint)

    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        textSize = 10f * scaleFactor
        color = theme.secondaryText
    }
    canvas.drawText(quote.author, pad + 10f * scaleFactor, authorY, authorPaint)

    // Auto-fit quote text with full vertical breathing space
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 600)
        color = theme.primaryText
    }

    val topLimit = pad
    val bottomLimit = authorY - (14f * scaleFactor)
    val contentW = cardRect.width() - (pad * 2)

    drawAutoFitText(
        canvas = canvas,
        text = "“${quote.text}”",
        x = pad,
        topLimit = topLimit,
        bottomLimit = bottomLimit,
        maxWidth = contentW,
        paint = bodyPaint,
        initialTextSize = 15.5f * scaleFactor,
        minTextSize = 10.5f * scaleFactor,
        maxLines = 6,
        verticalCenter = true
    )

    return bitmap
}


// =========================================================================
// 6. TWO-TONE INSIGHT (2x2)
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
    val pad = 18f * scaleFactor

    // Bottom author line using accent color
    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 600)
        textSize = 10f * scaleFactor
        color = theme.accent
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.02f
    }
    val authorY = cardRect.bottom - pad
    canvas.drawText(quote.author, cx, authorY, authorPaint)

    val parts = if (quote.text.contains(".")) {
        val split = quote.text.split(".", limit = 2)
        Pair(split[0].trim() + ".", split.getOrNull(1)?.trim() ?: "")
    } else if (quote.text.contains("?")) {
        val split = quote.text.split("?", limit = 2)
        Pair(split[0].trim() + "?", split.getOrNull(1)?.trim() ?: "")
    } else if (quote.text.contains("—") || quote.text.contains("-")) {
        val split = quote.text.split(Regex("[—-]", RegexOption.IGNORE_CASE), limit = 2)
        Pair(split[0].trim(), split.getOrNull(1)?.trim() ?: "")
    } else {
        val words = quote.text.split(" ").filter { it.isNotBlank() }
        val mid = words.size / 2
        Pair(words.take(mid).joinToString(" "), words.drop(mid).joinToString(" "))
    }

    val part1Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        color = theme.primaryText
        textAlign = Paint.Align.CENTER
    }

    val part2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 700)
        color = theme.accent
        textAlign = Paint.Align.CENTER
    }

    val topLimit = cardRect.top + pad
    val bottomLimit = authorY - (12f * scaleFactor)
    val availableHeight = maxOf(10f, bottomLimit - topLimit)
    val contentW = cardRect.width() - (pad * 2)

    val initialSize = 14.5f * scaleFactor
    val minSize = 10f * scaleFactor
    var chosenSize = initialSize
    val step = (initialSize - minSize) / 12f

    var lines1: List<String> = emptyList()
    var lines2: List<String> = emptyList()
    var spacing = chosenSize * 1.32f
    var totalHeight = 0f

    while (chosenSize >= minSize) {
        part1Paint.textSize = chosenSize
        part2Paint.textSize = chosenSize
        spacing = chosenSize * 1.32f

        lines1 = breakTextIntoLines(parts.first, part1Paint, contentW)
        lines2 = if (parts.second.isNotBlank()) breakTextIntoLines(parts.second, part2Paint, contentW) else emptyList()

        val totalLines = lines1.size + lines2.size
        val ascent = -part1Paint.ascent()
        val descent = part1Paint.descent()
        totalHeight = if (totalLines == 0) 0f else (totalLines - 1) * spacing + ascent + descent

        if (totalLines <= 6 && totalHeight <= availableHeight) {
            break
        }
        chosenSize -= step
    }

    part1Paint.textSize = chosenSize
    part2Paint.textSize = chosenSize
    val ascent = -part1Paint.ascent()
    val extraMargin = maxOf(0f, (availableHeight - totalHeight) / 2f)
    var currentY = topLimit + extraMargin + ascent

    for (line in lines1) {
        canvas.drawText(line, cx, currentY, part1Paint)
        currentY += spacing
    }
    for (line in lines2) {
        canvas.drawText(line, cx, currentY, part2Paint)
        currentY += spacing
    }

    return bitmap
}

// =========================================================================
// 7. DAILY Insight (4x2 - Minimalist Date & Italic Editorial)
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

    // 1. Base card background & border
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

    // 2. Top Header: Date only (quote type / category removed)
    val dateText = try {
        val formatter = java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.getDefault())
        formatter.format(java.util.Date()).uppercase()
    } catch (_: Exception) {
        "TODAY'S REFLECTION"
    }

    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 600)
        textSize = 9.5f * scaleFactor
        color = theme.accent
        letterSpacing = 0.12f
    }
    val headerY = padV + (10f * scaleFactor)
    canvas.drawText(dateText, padH, headerY, datePaint)

    // 3. Bottom Author Line
    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        textSize = 10.5f * scaleFactor
        color = theme.secondaryText
    }
    val authorText = if (quote.sourceBook.isNotBlank()) "— ${quote.author}, ${quote.sourceBook}" else "— ${quote.author}"
    val authorY = cardRect.bottom - padV
    canvas.drawText(authorText, padH, authorY, authorPaint)

    // 4. Clean Oblique Typography (pure quote text without quotation marks)
    val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 400)
        textSkewX = -0.16f // Precision modern italic angle
        color = theme.primaryText
        letterSpacing = -0.01f
    }

    val topLimit = headerY + (14f * scaleFactor)
    val bottomLimit = authorY - (14f * scaleFactor)
    val contentW = cardRect.width() - (padH * 2)

    drawAutoFitText(
        canvas = canvas,
        text = quote.text,
        x = padH,
        topLimit = topLimit,
        bottomLimit = bottomLimit,
        maxWidth = contentW,
        paint = quotePaint,
        initialTextSize = 16.5f * scaleFactor,
        minTextSize = 11f * scaleFactor,
        maxLines = 5,
        verticalCenter = true
    )

    return bitmap
}


// =========================================================================
// 8. CELESTIAL HORIZON (4x2)
// Eclipse-inspired solar rim glow anchored directly to planetary geometry
// =========================================================================

fun generateCelestialHorizonBitmap(
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

    // 1. Base card background & border
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.bg }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.subtleBorder
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

    // 2. Clip to widget squircle
    canvas.save()
    val cardClip = Path().apply {
        addRoundRect(cardRect, cornerRadius, cornerRadius, Path.Direction.CW)
    }
    canvas.clipPath(cardClip)

    // Responsive lock: use card height as reference unit
    val baselineH = cardRect.height()

    // 3. Planet Center & Radius (Untouched)
    val planetRadius = baselineH * 1f
    val planetCx = cardRect.right + (baselineH * 0.00f)
    val planetCy = cardRect.bottom + (baselineH * 0.24f)

    // 4. Multi-Layered Atmospheric Diffusion (Smooth Multi-Stop Cosine Falloff)
    val ambientCoronaRadius = baselineH * 1.55f
    val baseAlpha = if (theme.isLight) 0.30f else 0.40f
    val ambientCoronaShader = RadialGradient(
        planetCx - (planetRadius * 0.48f),
        planetCy - (planetRadius * 0.48f),
        ambientCoronaRadius,
        intArrayOf(
            Color(theme.accent).copy(alpha = baseAlpha).toArgb(),
            Color(theme.accent).copy(alpha = baseAlpha * 0.65f).toArgb(),
            Color(theme.accent).copy(alpha = baseAlpha * 0.35f).toArgb(),
            Color(theme.accent).copy(alpha = baseAlpha * 0.12f).toArgb(),
            Color(theme.accent).copy(alpha = 0.02f).toArgb(),
            android.graphics.Color.TRANSPARENT
        ),
        floatArrayOf(0.0f, 0.22f, 0.45f, 0.68f, 0.88f, 1.0f),
        Shader.TileMode.CLAMP
    )
    val ambientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = ambientCoronaShader
    }
    canvas.drawRect(cardRect, ambientPaint)

    // Tangential atmospheric bloom: All anchored strictly to planetRadius to prevent concentric steps
    val bloomLayers = listOf(
        // strokeWidth, blurRadius, alphaMultiplier
        Triple(56f * scaleFactor, 26f * scaleFactor, 0.06f),
        Triple(34f * scaleFactor, 15f * scaleFactor, 0.12f),
        Triple(18f * scaleFactor, 8f * scaleFactor, 0.24f),
        Triple(9f * scaleFactor, 4f * scaleFactor, 0.42f),
        Triple(3.5f * scaleFactor, 1.5f * scaleFactor, 0.70f)
    )
    for ((strokeW, blurR, alpha) in bloomLayers) {
        val bloomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeW
            color = Color(theme.accent).copy(alpha = if (theme.isLight) alpha * 0.75f else alpha).toArgb()
            maskFilter = android.graphics.BlurMaskFilter(
                blurR,
                android.graphics.BlurMaskFilter.Blur.NORMAL
            )
        }
        canvas.drawCircle(planetCx, planetCy, planetRadius, bloomPaint)
    }

    // 5. Dark Planetary Body (clean occlusion of inward bloom)
    val planetBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = theme.bg
    }
    canvas.drawCircle(planetCx, planetCy, planetRadius, planetBodyPaint)

    // 6. Razor Solar Rim Light (Sharp limb boundary)
    val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.4f * scaleFactor
        color = Color(theme.accent).copy(alpha = 0.90f).toArgb()
    }
    canvas.drawCircle(planetCx, planetCy, planetRadius, rimPaint)

    // Smooth apex solar highlight (seamless blend from white core into ambient corona)
    val apexHighlightShader = RadialGradient(
        cardRect.left + (cardRect.width() * 0.56f),
        cardRect.top + (baselineH * 0.52f),
        baselineH * 0.65f,
        intArrayOf(
            android.graphics.Color.WHITE,
            Color(theme.accent).copy(alpha = 0.95f).toArgb(),
            Color(theme.accent).copy(alpha = 0.55f).toArgb(),
            Color(theme.accent).copy(alpha = 0.15f).toArgb(),
            android.graphics.Color.TRANSPARENT
        ),
        floatArrayOf(0.0f, 0.20f, 0.45f, 0.72f, 1.0f),
        Shader.TileMode.CLAMP
    )

    // Soft incandescent halo at the apex
    val apexSoftBloom = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.2f * scaleFactor
        shader = apexHighlightShader
        maskFilter = android.graphics.BlurMaskFilter(2.0f * scaleFactor, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }
    canvas.drawCircle(planetCx, planetCy, planetRadius, apexSoftBloom)

    // White-hot hairline core
    val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.0f * scaleFactor
        shader = apexHighlightShader
    }
    canvas.drawCircle(planetCx, planetCy, planetRadius, corePaint)

    canvas.restore()

    // 7. Typography (Untouched)
    val padH = 26f * scaleFactor
    val padV = 22f * scaleFactor
    val textLimitW = cardRect.width() * 0.38f

    // Bottom author line
    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        textSize = 9.5f * scaleFactor
        color = theme.secondaryText
        letterSpacing = 0.04f
    }
    val authorText = if (quote.sourceBook.isNotBlank()) "— ${quote.author}, ${quote.sourceBook}" else "— ${quote.author}"
    val authorY = cardRect.bottom - padV
    canvas.drawText(authorText, padH, authorY, authorPaint)

    // Main quote block
    val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        color = theme.primaryText
        letterSpacing = 0.04f
    }

    drawAutoFitText(
        canvas = canvas,
        text = quote.text,
        x = padH,
        topLimit = padV + (4f * scaleFactor),
        bottomLimit = authorY - (14f * scaleFactor),
        maxWidth = textLimitW,
        paint = quotePaint,
        initialTextSize = 13.5f * scaleFactor,
        minTextSize = 9.0f * scaleFactor,
        maxLines = 6,
        verticalCenter = true
    )

    return bitmap
}


// =========================================================================
// 9. MINDFUL SMILE CARD (2x2) - CASUAL HANDWRITTEN COASTER STYLE
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
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    // 1. Card Bounds (1:1 square in fixed aspect mode)
    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }
    val cornerRadius = getStandardCornerRadius(scaleFactor)

    // 2. Base Surface & Subtle Outer Border
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.bg }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.subtleBorder
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

    val cx = cardRect.centerX()
    val cy = cardRect.centerY()
    val minDim = minOf(cardRect.width(), cardRect.height())

    // 3. Casual / Handwriting Typeface
    val casualTypeface = try {
        val tf = Typeface.create("casual", Typeface.BOLD)
        if (tf != Typeface.DEFAULT) tf else getSlateFont(context, weight = 800)
    } catch (_: Exception) {
        getSlateFont(context, weight = 800)
    }

    val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = casualTypeface
        color = theme.primaryText
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.05f
    }

    val hasAuthor = quote.author.isNotBlank() &&
            !quote.author.equals("Unknown", ignoreCase = true) &&
            !quote.author.equals("Anonymous", ignoreCase = true)

    // Reserve bottom space for the author line so quote & author never collide
    val authorReservedH = if (hasAuthor) 26f * scaleFactor else 0f

    // 4. Auto-Wrap Text into Balanced Lines
    val textUpper = quote.text.trim().uppercase()
    val maxTextW = cardRect.width() * 0.76f
    val maxContentH = (cardRect.height() * 0.74f) - authorReservedH

    var textSize = (minDim * 0.115f).coerceIn(13f * scaleFactor, 22f * scaleFactor)
    val minTextSize = 9.5f * scaleFactor
    val smileH = 16f * scaleFactor
    val smileGap = 12f * scaleFactor

    var lines: List<String> = emptyList()
    var lineHeight = 0f

    while (textSize >= minTextSize) {
        quotePaint.textSize = textSize
        lineHeight = textSize * 1.34f

        val words = textUpper.split(Regex("\\s+"))
        val wrappedLines = mutableListOf<String>()
        var curLine = StringBuilder()

        for (word in words) {
            val candidate = if (curLine.isEmpty()) word else "$curLine $word"
            if (quotePaint.measureText(candidate) <= maxTextW) {
                curLine = StringBuilder(candidate)
            } else {
                if (curLine.isNotEmpty()) wrappedLines.add(curLine.toString())
                curLine = StringBuilder(word)
            }
        }
        if (curLine.isNotEmpty()) wrappedLines.add(curLine.toString())

        lines = wrappedLines
        val totalH = (lines.size * lineHeight) + smileGap + smileH

        if (totalH <= maxContentH && lines.size <= 5) {
            break
        }
        textSize -= 1f * scaleFactor
    }

    // 5. Draw Tilted Coaster Content (-5° Slant)
    canvas.save()
    canvas.rotate(-5.0f, cx, cy - (authorReservedH * 0.35f))

    val totalBlockHeight = (lines.size * lineHeight) + smileGap + smileH
    var textY = (cy - (authorReservedH * 0.35f)) - (totalBlockHeight / 2f) + (textSize * 0.90f)

    // Draw Quote Lines
    for (line in lines) {
        canvas.drawText(line, cx, textY, quotePaint)
        textY += lineHeight
    }

    // 6. Draw Hand-Drawn Smiley Face in THEME ACCENT COLOR
    val smileCenterY = textY - lineHeight + smileGap + (smileH * 0.35f)
    val eyeRadius = (textSize * 0.10f).coerceIn(1.6f * scaleFactor, 2.5f * scaleFactor)
    val eyeSpacing = (textSize * 0.55f).coerceIn(8.5f * scaleFactor, 13f * scaleFactor)
    val eyeY = smileCenterY - (4f * scaleFactor)

    // Eye Dots (Accent Color)
    val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.accent
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx - (eyeSpacing / 2f), eyeY, eyeRadius, eyePaint)
    canvas.drawCircle(cx + (eyeSpacing / 2f), eyeY, eyeRadius, eyePaint)

    // Curved Smile Mouth (Accent Color)
    val mouthW = (textSize * 1.18f).coerceIn(17f * scaleFactor, 26f * scaleFactor)
    val mouthH = mouthW * 0.65f
    val mouthRect = RectF(
        cx - (mouthW / 2f),
        smileCenterY - (mouthH * 0.35f),
        cx + (mouthW / 2f),
        smileCenterY + (mouthH * 0.65f)
    )

    val mouthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.accent
        style = Paint.Style.STROKE
        strokeWidth = (textSize * 0.13f).coerceIn(2.0f * scaleFactor, 3.0f * scaleFactor)
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawArc(mouthRect, 20f, 140f, false, mouthPaint)

    canvas.restore()

    // 7. Clear & Legible Author Line
    if (hasAuthor) {
        val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getSlateFont(context, weight = 600)
            textSize = (minDim * 0.072f).coerceIn(10f * scaleFactor, 12.5f * scaleFactor)
            color = theme.secondaryText
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.04f
        }
        val authorY = cardRect.bottom - (16f * scaleFactor)
        canvas.drawText("— ${quote.author}", cx, authorY, authorPaint)
    }

    return bitmap
}


// =========================================================================
// 10. Ambient Bloom (4x2) - ETHEREAL AMBIENT ORB STYLE
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
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    // 1. Card Bounds
    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val aspect = 2.05f
        var cardH = h
        var cardW = cardH * aspect
        if (cardW > w) {
            cardW = w
            cardH = cardW / aspect
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }
    val cornerRadius = getStandardCornerRadius(scaleFactor)

    // 2. Base Surface & Outer Border
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.bg }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.subtleBorder
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

    // 3. Clip inside card squircle
    canvas.save()
    val cardClip = Path().apply {
        addRoundRect(cardRect, cornerRadius, cornerRadius, Path.Direction.CW)
    }
    canvas.clipPath(cardClip)

    val cx = cardRect.centerX()
    val cy = cardRect.centerY()
    val minDim = minOf(cardRect.width(), cardRect.height())

    // 4. Harmonic Accent Color Calculation for Center Orb
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(theme.accent, hsv)

    // Shift hue +28° to generate the dual-tone sunset aura
    val shiftedHsv = floatArrayOf(
        (hsv[0] + 28f) % 360f,
        (hsv[1] * 0.85f).coerceIn(0.35f, 0.90f),
        (hsv[2] * 1.05f).coerceIn(0.40f, 1.0f)
    )
    val secondaryAccentColor = android.graphics.Color.HSVToColor(shiftedHsv)

    // Warm apex highlight tint
    val highlightHsv = floatArrayOf(
        (hsv[0] - 16f + 360f) % 360f,
        (hsv[1] * 0.55f).coerceIn(0.20f, 0.70f),
        1.0f
    )
    val highlightColor = android.graphics.Color.HSVToColor(highlightHsv)

    // 5. Draw Center Ambient Glow Orb
    val orbRadius = minDim * 0.52f

    // Atmospheric radial falloff
    val outerHaloShader = android.graphics.RadialGradient(
        cx, cy,
        orbRadius * 1.45f,
        intArrayOf(
            Color(theme.accent).copy(alpha = if (theme.isLight) 0.35f else 0.42f).toArgb(),
            Color(secondaryAccentColor).copy(alpha = if (theme.isLight) 0.18f else 0.24f).toArgb(),
            Color(secondaryAccentColor).copy(alpha = 0.04f).toArgb(),
            android.graphics.Color.TRANSPARENT
        ),
        floatArrayOf(0.0f, 0.40f, 0.72f, 1.0f),
        android.graphics.Shader.TileMode.CLAMP
    )
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = outerHaloShader }
    canvas.drawCircle(cx, cy, orbRadius * 1.45f, haloPaint)

    // Soft Gaussian blurred core
    val coreShader = android.graphics.LinearGradient(
        cx, cy - orbRadius,
        cx, cy + orbRadius,
        intArrayOf(
            Color(highlightColor).copy(alpha = if (theme.isLight) 0.55f else 0.65f).toArgb(),
            Color(theme.accent).copy(alpha = if (theme.isLight) 0.48f else 0.58f).toArgb(),
            Color(secondaryAccentColor).copy(alpha = if (theme.isLight) 0.40f else 0.50f).toArgb()
        ),
        floatArrayOf(0.0f, 0.45f, 1.0f),
        android.graphics.Shader.TileMode.CLAMP
    )
    val coreOrbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = coreShader
        maskFilter = android.graphics.BlurMaskFilter(orbRadius * 0.34f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }
    canvas.drawCircle(cx, cy, orbRadius * 0.88f, coreOrbPaint)

    // 6. Editorial Serif Typography Setup
    val serifTypeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
    val maxTextW = cardRect.width() * 0.82f

    val hasAuthor = quote.author.isNotBlank() &&
            !quote.author.equals("Unknown", ignoreCase = true) &&
            !quote.author.equals("Anonymous", ignoreCase = true)
    val authorText = if (hasAuthor) "— ${quote.author.trim()}" else ""

    var textSize = (cardRect.height() * 0.125f).coerceIn(13.5f * scaleFactor, 22f * scaleFactor)
    val minTextSize = 9.5f * scaleFactor

    val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = serifTypeface
        color = theme.primaryText
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.02f
    }

    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = serifTypeface
        color = theme.primaryText
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.03f
    }

    var lines: List<String> = emptyList()
    var quoteLineH = 0f
    var authorSize = 0f
    var authorGap = 0f

    // 7. Auto-Fit Word Wrap
    while (textSize >= minTextSize) {
        quotePaint.textSize = textSize
        quoteLineH = textSize * 1.42f
        authorSize = (textSize * 0.68f).coerceIn(9.5f * scaleFactor, 12.5f * scaleFactor)
        authorPaint.textSize = authorSize
        authorGap = (textSize * 0.65f).coerceIn(8f * scaleFactor, 14f * scaleFactor)

        val words = quote.text.trim().split(Regex("\\s+"))
        val wrapped = mutableListOf<String>()
        var curLine = StringBuilder()

        for (word in words) {
            val candidate = if (curLine.isEmpty()) word else "$curLine $word"
            if (quotePaint.measureText(candidate) <= maxTextW) {
                curLine = StringBuilder(candidate)
            } else {
                if (curLine.isNotEmpty()) wrapped.add(curLine.toString())
                curLine = StringBuilder(word)
            }
        }
        if (curLine.isNotEmpty()) wrapped.add(curLine.toString())

        lines = wrapped

        val qMetrics = quotePaint.fontMetrics
        val quoteVisualH = (lines.size - 1) * quoteLineH + (qMetrics.descent - qMetrics.ascent)
        val spaceBelowCenter = (quoteVisualH / 2f) + (if (hasAuthor) authorGap + authorSize else 0f)
        val maxAllowedBelowCenter = (cardRect.height() / 2f) - (12f * scaleFactor)

        if (spaceBelowCenter <= maxAllowedBelowCenter || textSize <= minTextSize) {
            break
        }
        textSize -= 0.8f * scaleFactor
    }

    // 8. Draw Quote Centered Directly on (cx, cy)
    val qMetrics = quotePaint.fontMetrics
    val quoteVisualH = (lines.size - 1) * quoteLineH + (qMetrics.descent - qMetrics.ascent)

    // Aligns the visual vertical midpoint of the quote lines precisely with cy
    var textY = cy - (quoteVisualH / 2f) - qMetrics.ascent

    for (line in lines) {
        canvas.drawText(line, cx, textY, quotePaint)
        textY += quoteLineH
    }

    // 9. Draw Author Beneath the Quote Without Displacing It
    if (hasAuthor) {
        val lastLineBaseline = textY - quoteLineH
        val authorMetrics = authorPaint.fontMetrics
        val authorBaseline = lastLineBaseline + authorGap - authorMetrics.ascent
        canvas.drawText(authorText, cx, authorBaseline, authorPaint)
    }

    canvas.restore()

    return bitmap
}


// =========================================================================
// 11. ORGANIC PEBBLE (2x2) - FIXED-ASPECT SCALED SILHOUETTE
// =========================================================================

fun generateOrganicPebbleBitmap(
    context: Context,
    quote: QuoteItem,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val theme = resolveTheme(config)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    // 1. Lock fixed aspect ratio (0.82) to guarantee the pebble silhouette never distorts
    val targetAspect = 0.82f
    val margin = 4f * scaleFactor
    val availW = w - (margin * 2f)
    val availH = h - (margin * 2f)

    val (pw, ph) = if (availW / availH > targetAspect) {
        (availH * targetAspect) to availH
    } else {
        availW to (availW / targetAspect)
    }

    // Center the fixed pebble within the available cell bounds
    val l = (w - pw) / 2f
    val t = (h - ph) / 2f

    // 2. Fixed Organic Pebble Path (Tangent-Continuous Smooth Bezier)
    val pebblePath = Path().apply {
        // Top dome crest
        moveTo(l + pw * 0.48f, t + ph * 0.00f)

        // Top crest to upper-right shoulder
        cubicTo(
            l + pw * 0.68f, t + ph * 0.00f,
            l + pw * 0.84f, t + ph * 0.12f,
            l + pw * 0.92f, t + ph * 0.28f
        )

        // Upper-right shoulder down to rightmost curve
        cubicTo(
            l + pw * 1.00f, t + ph * 0.42f,
            l + pw * 1.00f, t + ph * 0.60f,
            l + pw * 0.90f, t + ph * 0.76f
        )

        // Right flank down to base
        cubicTo(
            l + pw * 0.80f, t + ph * 0.92f,
            l + pw * 0.64f, t + ph * 1.00f,
            l + pw * 0.48f, t + ph * 1.00f
        )

        // Base into lower-left heel
        cubicTo(
            l + pw * 0.32f, t + ph * 1.00f,
            l + pw * 0.18f, t + ph * 0.94f,
            l + pw * 0.11f, t + ph * 0.82f
        )

        // Lower-left heel up through left lateral bulge
        cubicTo(
            l + pw * 0.04f, t + ph * 0.70f,
            l + pw * 0.04f, t + ph * 0.54f,
            l + pw * 0.10f, t + ph * 0.42f
        )

        // Left bulge inward through waist concavity
        cubicTo(
            l + pw * 0.15f, t + ph * 0.32f,
            l + pw * 0.22f, t + ph * 0.24f,
            l + pw * 0.26f, t + ph * 0.14f
        )

        // Waist returning to top dome crest
        cubicTo(
            l + pw * 0.30f, t + ph * 0.04f,
            l + pw * 0.38f, t + ph * 0.00f,
            l + pw * 0.48f, t + ph * 0.00f
        )
        close()
    }

    // 3. Base Surface & Subtle Rim
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.bg
        style = Paint.Style.FILL
    }
    canvas.drawPath(pebblePath, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.subtleBorder
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawPath(pebblePath, borderPaint)

    // 4. Accent Dot
    val dotRadius = pw * 0.078f
    val dotCx = l + pw * 0.70f
    val dotCy = t + ph * 0.24f
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.accent
        style = Paint.Style.FILL
    }
    canvas.drawCircle(dotCx, dotCy, dotRadius, dotPaint)

    // 5. Proportional Typography (Locked to pw/ph so it never over-scales)
    val textLeft = l + pw * 0.22f
    val textTop = t + ph * 0.34f
    val maxTextW = pw * 0.54f
    val maxTextH = ph * 0.44f

    val initialTextSize = pw * 0.102f
    val minTextSize = pw * 0.060f

    val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        color = theme.primaryText
        letterSpacing = 0.01f
    }

    val finalBottom = drawAutoFitText(
        canvas = canvas,
        text = quote.text,
        x = textLeft,
        topLimit = textTop,
        bottomLimit = textTop + maxTextH,
        maxWidth = maxTextW,
        paint = quotePaint,
        initialTextSize = initialTextSize,
        minTextSize = minTextSize,
        maxLines = 6,
        lineSpacingRatio = 1.34f,
        verticalCenter = false
    )

    // 6. Proportional Signature Dash
    val dashW = pw * 0.16f
    val dashStroke = (pw * 0.013f).coerceAtLeast(1.8f * scaleFactor)
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.secondaryText
        style = Paint.Style.STROKE
        strokeWidth = dashStroke
        strokeCap = Paint.Cap.ROUND
    }
    val lineY = finalBottom + (ph * 0.05f)
    canvas.drawLine(textLeft, lineY, textLeft + dashW, lineY, linePaint)

    return bitmap
}

// =========================================================================
// 12. VERTICAL CAPSULE (2x2)
// Slender pill with stacked typography and anchored progress pin
// =========================================================================

fun generateVerticalCapsuleBitmap(
    context: Context,
    quote: QuoteItem,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val theme = resolveTheme(config)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    // 1. Slender Pill Geometry
    val capsuleH = h * 0.94f
    val capsuleW = minOf(w * 0.46f, capsuleH * 0.48f)
    val capsuleLeft = (w - capsuleW) / 2f
    val capsuleTop = (h - capsuleH) / 2f
    val capsuleRect = RectF(capsuleLeft, capsuleTop, capsuleLeft + capsuleW, capsuleTop + capsuleH)
    val cornerRadius = capsuleW / 2f

    // 2. Base Capsule & Border
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(45, 0, 0, 0)
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(RectF(capsuleRect).apply { offset(0f, 3f * scaleFactor) }, cornerRadius, cornerRadius, shadowPaint)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.bg
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(capsuleRect, cornerRadius, cornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.subtleBorder
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(capsuleRect, cornerRadius, cornerRadius, borderPaint)

    val padH = capsuleW * 0.20f
    val textLeft = capsuleRect.left + padH
    val maxTextW = capsuleW - (padH * 2f)

    // 3. Stacked Typography (Upper section)
    val textTop = capsuleRect.top + (capsuleW * 0.55f)
    val textBottomLimit = capsuleRect.top + (capsuleH * 0.62f)

    val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        color = theme.primaryText
        letterSpacing = 0.01f
    }

    val finalBottom = drawAutoFitText(
        canvas = canvas,
        text = quote.text,
        x = textLeft,
        topLimit = textTop,
        bottomLimit = textBottomLimit,
        maxWidth = maxTextW,
        paint = quotePaint,
        initialTextSize = 14f * scaleFactor,
        minTextSize = 9.5f * scaleFactor,
        maxLines = 7,
        lineSpacingRatio = 1.34f,
        verticalCenter = false
    )

    // 4. Vertical Anchor Line & Accent Dot
    val lineX = textLeft + (8f * scaleFactor)
    val lineTop = maxOf(finalBottom + (14f * scaleFactor), capsuleRect.top + capsuleH * 0.64f)
    val dotRadius = 3.5f * scaleFactor
    val dotCy = capsuleRect.bottom - (capsuleW * 0.46f)

    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.secondaryText
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
        alpha = if (theme.isLight) 90 else 120
    }
    canvas.drawLine(lineX, lineTop, lineX, dotCy - dotRadius, linePaint)

    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.accent
        style = Paint.Style.FILL
    }
    canvas.drawCircle(lineX, dotCy, dotRadius, dotPaint)

    return bitmap
}

// =========================================================================
// 13. HORIZON RIPPLE (4x2)
// Minimalist split card with geometric sunset water ripples
// =========================================================================

fun generateHorizonRippleBitmap(
    context: Context,
    quote: QuoteItem,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val theme = resolveTheme(config)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val aspect = 2.05f
        var cardH = h
        var cardW = cardH * aspect
        if (cardW > w) {
            cardW = w
            cardH = cardW / aspect
        }
        RectF((w - cardW) / 2f, (h - cardH) / 2f, (w + cardW) / 2f, (h + cardH) / 2f)
    }
    val cornerRadius = getStandardCornerRadius(scaleFactor)

    // 1. Base Surface & Border
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.bg }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.subtleBorder
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

    // 2. Right Sunset Emblem
    val rightSectionW = cardRect.width() * 0.44f
    val sunCx = cardRect.right - (rightSectionW / 2f) - (8f * scaleFactor)
    val sunCy = cardRect.centerY() - (6f * scaleFactor)
    val sunR = minOf(rightSectionW * 0.38f, cardRect.height() * 0.28f)

    val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.accent
        style = Paint.Style.FILL
    }

    // Upper Semicircle Sun
    val sunRect = RectF(sunCx - sunR, sunCy - sunR, sunCx + sunR, sunCy + sunR)
    canvas.drawArc(sunRect, 180f, 180f, true, sunPaint)

    // Subtle Horizon Hairline
    val hairlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.accent
        style = Paint.Style.STROKE
        strokeWidth = 0.8f * scaleFactor
        alpha = 80
    }
    canvas.drawLine(sunCx - (sunR * 1.35f), sunCy, sunCx + (sunR * 1.35f), sunCy, hairlinePaint)

    // Rippled Water Reflection Bars
    val rippleData = listOf(
        // yOffset, halfWidth, barHeight
        Triple(4.5f * scaleFactor, sunR * 0.96f, 3.8f * scaleFactor),
        Triple(12.0f * scaleFactor, sunR * 0.84f, 3.2f * scaleFactor),
        Triple(18.5f * scaleFactor, sunR * 0.68f, 2.6f * scaleFactor),
        Triple(24.0f * scaleFactor, sunR * 0.50f, 2.0f * scaleFactor),
        Triple(28.5f * scaleFactor, sunR * 0.32f, 1.4f * scaleFactor)
    )

    for ((yOff, halfW, barH) in rippleData) {
        val barRect = RectF(
            sunCx - halfW,
            sunCy + yOff,
            sunCx + halfW,
            sunCy + yOff + barH
        )
        val barCorner = barH / 2f
        canvas.drawRoundRect(barRect, barCorner, barCorner, sunPaint)
    }

    // 3. Left Quote Typography
    val padH = cardRect.width() * 0.09f
    val maxTextW = cardRect.width() - rightSectionW - (padH * 1.2f)

    val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        color = theme.primaryText
        letterSpacing = 0.01f
    }

    drawAutoFitText(
        canvas = canvas,
        text = quote.text,
        x = cardRect.left + padH,
        topLimit = cardRect.top + (14f * scaleFactor),
        bottomLimit = cardRect.bottom - (14f * scaleFactor),
        maxWidth = maxTextW,
        paint = quotePaint,
        initialTextSize = 16f * scaleFactor,
        minTextSize = 11f * scaleFactor,
        maxLines = 5,
        lineSpacingRatio = 1.38f,
        verticalCenter = true
    )

    return bitmap
}

// =========================================================================
// 14. CERAMIC DISC (2x2)
// Circular coaster aesthetic with uppercase typography, bird motif & moon dot
// =========================================================================

fun generateCeramicDiscBitmap(
    context: Context,
    quote: QuoteItem,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val theme = resolveTheme(config)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val size = minOf(w, h)
    val cx = w / 2f
    val cy = h / 2f
    val discRadius = (size / 2f) - (4f * scaleFactor)

    // 1. Base Disc Surface & Shadow
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(45, 0, 0, 0)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy + (3f * scaleFactor), discRadius, shadowPaint)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.bg
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, discRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.subtleBorder
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawCircle(cx, cy, discRadius, borderPaint)

    // 2. Uppercase Geometric Typography
    val maxTextW = discRadius * 1.30f
    val textLeft = cx - (maxTextW * 0.48f)
    val topLimit = cy - (discRadius * 0.55f)
    val bottomLimit = cy + (discRadius * 0.20f)

    val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 600)
        color = theme.primaryText
        letterSpacing = 0.12f
    }

    val finalBottom = drawAutoFitText(
        canvas = canvas,
        text = quote.text.uppercase(),
        x = textLeft,
        topLimit = topLimit,
        bottomLimit = bottomLimit,
        maxWidth = maxTextW,
        paint = quotePaint,
        initialTextSize = 14.5f * scaleFactor,
        minTextSize = 10f * scaleFactor,
        maxLines = 5,
        lineSpacingRatio = 1.40f,
        verticalCenter = false
    )

    // 3. Horizontal Dash
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.primaryText
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * scaleFactor
        strokeCap = Paint.Cap.ROUND
    }
    val lineY = finalBottom + (14f * scaleFactor)
    canvas.drawLine(textLeft, lineY, textLeft + (22f * scaleFactor), lineY, linePaint)

    // 4. Soaring Bird Silhouette
    val birdCx = textLeft + maxTextW - (18f * scaleFactor)
    val birdCy = lineY + (2f * scaleFactor)
    val birdSize = 22f * scaleFactor

    val birdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.primaryText
        style = Paint.Style.FILL
    }
    val bs = birdSize / 24f
    val birdPath = Path().apply {
        moveTo(birdCx - 11f * bs, birdCy - 2f * bs)
        cubicTo(birdCx - 6f * bs, birdCy - 9f * bs, birdCx + 3f * bs, birdCy - 11f * bs, birdCx + 10f * bs, birdCy - 7f * bs)
        cubicTo(birdCx + 5f * bs, birdCy - 2f * bs, birdCx + 1f * bs, birdCy, birdCx + 2f * bs, birdCy + 3f * bs)
        cubicTo(birdCx + 6f * bs, birdCy + 7f * bs, birdCx + 11f * bs, birdCy + 9f * bs, birdCx + 10f * bs, birdCy + 10f * bs)
        cubicTo(birdCx + 5f * bs, birdCy + 8f * bs, birdCx - 1f * bs, birdCy + 6f * bs, birdCx - 5f * bs, birdCy + 8f * bs)
        cubicTo(birdCx - 6f * bs, birdCy + 4f * bs, birdCx - 5f * bs, birdCy + 1f * bs, birdCx - 11f * bs, birdCy - 2f * bs)
        close()
    }
    canvas.drawPath(birdPath, birdPaint)

    // 5. Bottom Accent Dot
    val dotR = discRadius * 0.13f
    val dotCy = cy + discRadius - dotR - (14f * scaleFactor)
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.accent
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, dotCy, dotR, dotPaint)

    return bitmap
}
