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
// 8. MINDFUL SMILE CARD (2x2)
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
    val pad = 18f * scaleFactor

    drawSmileGlyph(canvas, cx, pad + 14f * scaleFactor, 13f * scaleFactor, theme.accent)

    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        textSize = 9.5f * scaleFactor
        color = theme.secondaryText
        textAlign = Paint.Align.CENTER
    }
    val authorY = cardRect.bottom - pad
    canvas.drawText(quote.author, cx, authorY, authorPaint)

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
        initialTextSize = 14.5f * scaleFactor,
        minTextSize = 10f * scaleFactor,
        maxLines = 6,
        align = Paint.Align.CENTER,
        verticalCenter = true
    )

    return bitmap
}



// =========================================================================
// 9. MINIMAL CAPSULE (2x1)
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

    drawSparkEmblem(canvas, padH + 10f * scaleFactor, cy, 14f * scaleFactor, theme.accent)

    val textStartX = padH + 28f * scaleFactor
    val availableW = cardRect.width() - textStartX - padH

    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        textSize = 9f * scaleFactor
        color = theme.secondaryText
    }
    val authorY = cardRect.bottom - (9f * scaleFactor)
    canvas.drawText("— ${quote.author}", textStartX, authorY, authorPaint)

    val mantraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 600)
        color = theme.primaryText
        letterSpacing = -0.01f
    }

    drawAutoFitText(
        canvas = canvas,
        text = quote.text,
        x = textStartX,
        topLimit = cardRect.top + (8f * scaleFactor),
        bottomLimit = authorY - (10f * scaleFactor),
        maxWidth = availableW,
        paint = mantraPaint,
        initialTextSize = 12.5f * scaleFactor,
        minTextSize = 8.5f * scaleFactor,
        maxLines = 3,
        verticalCenter = true
    )

    return bitmap
}

// =========================================================================
// 10. BOLD CONDENSED STATEMENT (4x2)
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
    val padV = 20f * scaleFactor

    drawModernDoubleQuotes(canvas, padH, padV, 18f * scaleFactor, theme.accent)

    val boldFont = getSlateFont(context, weight = 800)
    val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = boldFont
        textSize = 10f * scaleFactor
        color = theme.secondaryText
        letterSpacing = 0.06f
    }
    val authorY = cardRect.bottom - padV
    canvas.drawText(quote.author.uppercase(), padH, authorY, authorPaint)

    drawModernDoubleQuotes(
        canvas,
        cardRect.right - padH - 14f * scaleFactor,
        cardRect.bottom - padV - 14f * scaleFactor,
        16f * scaleFactor,
        theme.accent,
        isClosing = true
    )

    val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = boldFont
        color = theme.primaryText
        letterSpacing = 0.02f
    }

    val topLimit = padV + (24f * scaleFactor)
    val bottomLimit = authorY - (14f * scaleFactor)
    val contentW = cardRect.width() - (padH * 2)

    drawAutoFitText(
        canvas = canvas,
        text = quote.text.uppercase(),
        x = padH,
        topLimit = topLimit,
        bottomLimit = bottomLimit,
        maxWidth = contentW,
        paint = quotePaint,
        initialTextSize = 17f * scaleFactor,
        minTextSize = 11f * scaleFactor,
        maxLines = 6,
        verticalCenter = true
    )

    return bitmap
}