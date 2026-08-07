package com.altusix.slate.widgets.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.altusix.slate.data.local.SlateWidgetConfig
import kotlin.math.cos
import kotlin.math.sin

enum class AiShapeStyle {
    SQUIRCLE,
    CIRCLE,
    HEXAGON,
    FRAMELESS,
    CAPSULE_LEFT,
    CAPSULE_RIGHT
}

// ============================================================================
// CORE TILE RENDERER (PURE CANVAS)
// ============================================================================

fun generateTileBitmap(
    context: Context,
    target: AiTarget,
    bgColor: Color,
    accentColor: Color,
    isLight: Boolean,
    shapeStyle: AiShapeStyle,
    showTextLabel: Boolean = false,
    customText: String? = null,
    isPrimaryAccent: Boolean = false,
    widthPx: Int,
    heightPx: Int
): Bitmap {
    val wPx = widthPx.coerceAtLeast(1)
    val hPx = heightPx.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = wPx.toFloat()
    val h = hPx.toFloat()
    val minDim = minOf(w, h)

    val currentBgColor = if (isPrimaryAccent) accentColor.toArgb() else bgColor.toArgb()
    val logoColor = if (isPrimaryAccent) {
        if (isLight) Color.White.toArgb() else Color.Black.toArgb()
    } else {
        accentColor.toArgb()
    }
    val strokeColor = if (isLight) Color(0xFFD1D1D6).toArgb() else Color(0xFF2C2C2E).toArgb()

    if (shapeStyle != AiShapeStyle.FRAMELESS) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = currentBgColor
            style = Paint.Style.FILL
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strokeColor
            style = Paint.Style.STROKE
            strokeWidth = if (!isPrimaryAccent) 4f else 0f
        }

        val margin = 4f
        val cx = w / 2f
        val cy = h / 2f
        val halfTile = (minDim / 2f) - margin
        val squircleRadius = minDim * 0.28f
        val fullCapRadius = (h - (margin * 2f)) / 2f

        val squareRect = RectF(cx - halfTile, cy - halfTile, cx + halfTile, cy + halfTile)
        val fullRect = RectF(margin, margin, w - margin, h - margin)

        when (shapeStyle) {
            AiShapeStyle.SQUIRCLE -> {
                canvas.drawRoundRect(fullRect, squircleRadius, squircleRadius, bgPaint)
                if (!isPrimaryAccent) canvas.drawRoundRect(fullRect, squircleRadius, squircleRadius, strokePaint)
            }
            AiShapeStyle.CIRCLE -> {
                val radius = (minDim / 2f) - margin
                canvas.drawCircle(cx, cy, radius, bgPaint)
                if (!isPrimaryAccent) canvas.drawCircle(cx, cy, radius, strokePaint)
            }
            AiShapeStyle.HEXAGON -> {
                val hexPath = Path()
                val radius = (minDim / 2f) - margin
                for (i in 0 until 6) {
                    val angle = Math.toRadians((60 * i - 30).toDouble())
                    val x = cx + (radius * cos(angle)).toFloat()
                    val y = cy + (radius * sin(angle)).toFloat()
                    if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
                }
                hexPath.close()
                canvas.drawPath(hexPath, bgPaint)
                if (!isPrimaryAccent) canvas.drawPath(hexPath, strokePaint)
            }
            AiShapeStyle.FRAMELESS -> {}
            AiShapeStyle.CAPSULE_LEFT -> {
                val radii = floatArrayOf(
                    fullCapRadius, fullCapRadius,
                    squircleRadius, squircleRadius,
                    squircleRadius, squircleRadius,
                    fullCapRadius, fullCapRadius
                )
                val path = Path().apply { addRoundRect(fullRect, radii, Path.Direction.CW) }
                canvas.drawPath(path, bgPaint)
                if (!isPrimaryAccent) canvas.drawPath(path, strokePaint)
            }
            AiShapeStyle.CAPSULE_RIGHT -> {
                val radii = floatArrayOf(
                    squircleRadius, squircleRadius,
                    fullCapRadius, fullCapRadius,
                    fullCapRadius, fullCapRadius,
                    squircleRadius, squircleRadius
                )
                val path = Path().apply { addRoundRect(fullRect, radii, Path.Direction.CW) }
                canvas.drawPath(path, bgPaint)
                if (!isPrimaryAccent) canvas.drawPath(path, strokePaint)
            }
        }
    }

    val resId = context.resources.getIdentifier(target.drawableResName, "drawable", context.packageName)

    if (showTextLabel && !customText.isNullOrEmpty()) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = logoColor
            textSize = h * 0.38f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val fontMetrics = textPaint.fontMetrics
        val textY = (h / 2f) - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(customText, w / 2f, textY, textPaint)
    } else if (resId != 0) {
        val drawable = ContextCompat.getDrawable(context, resId)
        if (drawable != null) {
            val maxLogoSize = minDim * 0.58f
            val actualLogoSize = if (isPrimaryAccent) minDim * 0.65f else maxLogoSize

            val intrinsicW = drawable.intrinsicWidth.toFloat()
            val intrinsicH = drawable.intrinsicHeight.toFloat()

            var drawW = actualLogoSize
            var drawH = actualLogoSize

            if (intrinsicW > 0f && intrinsicH > 0f) {
                val aspectRatio = intrinsicW / intrinsicH
                if (aspectRatio > 1f) {
                    drawH = actualLogoSize / aspectRatio
                } else {
                    drawW = actualLogoSize * aspectRatio
                }
            }

            val left = ((w / 2f) - (drawW / 2f)).toInt()
            val top = ((h / 2f) - (drawH / 2f)).toInt()
            val right = ((w / 2f) + (drawW / 2f)).toInt()
            val bottom = ((h / 2f) + (drawH / 2f)).toInt()

            drawable.setBounds(left, top, right, bottom)
            drawable.setTint(logoColor)
            drawable.draw(canvas)
        }
    }

    return bitmap
}

// ============================================================================
// PILL BASE RENDERER
// ============================================================================

fun drawPillBaseBitmap(
    context: Context,
    target: AiTarget,
    labelText: String,
    accentColor: Color,
    isLight: Boolean,
    widthPx: Int,
    heightPx: Int,
    logoSizePercent: Float = 0.38f,
    textSizePercent: Float = 0.30f,
    isCenteredLayout: Boolean = false
): Bitmap {
    val wPx = widthPx.coerceAtLeast(1)
    val hPx = heightPx.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = wPx.toFloat()
    val h = hPx.toFloat()

    val contentColor = if (isLight) Color(0xFF1C1C1E).toArgb() else Color.White.toArgb()
    val pillBgColor = accentColor.copy(alpha = 0.16f).toArgb()
    val strokeColor = accentColor.copy(alpha = 0.70f).toArgb()

    val strokeW = 3f
    val rect = RectF(strokeW / 2f, strokeW / 2f, w - strokeW / 2f, h - strokeW / 2f)
    val capsuleRadius = rect.height() / 2f

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pillBgColor
        style = Paint.Style.FILL
    }

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = strokeW
    }

    canvas.drawRoundRect(rect, capsuleRadius, capsuleRadius, bgPaint)
    canvas.drawRoundRect(rect, capsuleRadius, capsuleRadius, strokePaint)

    val logoSize = h * logoSizePercent
    val textSize = h * textSizePercent
    val itemGap = h * 0.12f

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = contentColor
        this.textSize = textSize
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val measuredTextWidth = textPaint.measureText(labelText)
    val totalContentWidth = logoSize + itemGap + measuredTextWidth

    val logoXStart = if (isCenteredLayout) {
        (w - totalContentWidth) / 2f
    } else {
        capsuleRadius * 0.55f
    }

    val resId = context.resources.getIdentifier(target.drawableResName, "drawable", context.packageName)
    if (resId != 0) {
        val drawable = ContextCompat.getDrawable(context, resId)
        if (drawable != null) {
            val logoTop = ((h - logoSize) / 2f).toInt()
            val logoLeft = logoXStart.toInt()
            val logoRight = (logoLeft + logoSize).toInt()
            val logoBottom = (logoTop + logoSize).toInt()

            drawable.setBounds(logoLeft, logoTop, logoRight, logoBottom)
            drawable.setTint(contentColor)
            drawable.draw(canvas)
        }
    }

    val fontMetrics = textPaint.fontMetrics
    val textX = logoXStart + logoSize + itemGap
    val textY = (h / 2f) - (fontMetrics.ascent + fontMetrics.descent) / 2f

    canvas.drawText(labelText, textX, textY, textPaint)

    return bitmap
}

// ============================================================================
// AI BARS (CANVAS COMPOSITES FOR 4x1 WIDGETS)
// ============================================================================

// 1. AI Primary Bar
fun generateAiBarHeroPrimaryBitmap(
    context: Context,
    config: SlateWidgetConfig,
    widthPx: Int,
    heightPx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = widthPx.toFloat()
    val h = heightPx.toFloat()
    val density = context.resources.displayMetrics.density

    // Lock capsule height to a fixed 58dp equivalent so stretching horizontally doesn't increase thickness
    val maxBarH = 72f * density
    val barH = minOf(h, maxBarH)
    val topY = (h - barH) / 2f
    val bottomY = topY + barH

    val isLight = config.themeMode == "LIGHT"
    val cardBg = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val accentColor = Color(config.accentColorHex)
    val dividerColor = if (isLight) 0x26000000 else 0x26FFFFFF

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
    val barCornerRadius = barH / 2f
    canvas.drawRoundRect(RectF(0f, topY, w, bottomY), barCornerRadius, barCornerRadius, bgPaint)

    val pad = barH * 0.12f
    val innerH = (barH - (pad * 2f)).toInt()
    val innerTopY = topY + pad
    val gap = barH * 0.08f
    val dividerSpacing = gap * 1.2f

    val rightTileCount = 3
    val rightTilesTotalW = (innerH * rightTileCount) + (gap * (rightTileCount - 1))

    val heroW = (w - (pad * 2f) - rightTilesTotalW - (dividerSpacing * 2f)).coerceAtLeast(innerH * 1.8f).toInt()

    // 1. Hero Pill (Gemini)
    val heroBitmap = drawPillBaseBitmap(
        context = context,
        target = AiTarget.GEMINI_TEXT,
        labelText = "Gemini",
        accentColor = accentColor,
        isLight = isLight,
        widthPx = heroW,
        heightPx = innerH,
        logoSizePercent = 0.48f,
        textSizePercent = 0.34f,
        isCenteredLayout = false
    )
    canvas.drawBitmap(heroBitmap, pad, innerTopY, null)

    // 2. Vertical Divider
    val divX = pad + heroW + dividerSpacing
    val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dividerColor
        strokeWidth = 2f
    }
    canvas.drawLine(divX, topY + (barH * 0.28f), divX, topY + (barH * 0.72f), divPaint)

    // 3. Right Icons (ChatGPT, Claude, Grok)
    val rightStartX = divX + dividerSpacing
    val targets = listOf(
        Triple(AiTarget.CHATGPT_TEXT, AiShapeStyle.SQUIRCLE, 0),
        Triple(AiTarget.CLAUDE, AiShapeStyle.SQUIRCLE, 1),
        Triple(AiTarget.GROK, AiShapeStyle.CAPSULE_RIGHT, 2)
    )

    val tileBgColor = if (isLight) Color(0x0A000000) else Color(0x14FFFFFF)
    val iconTint = if (isLight) Color.Black else Color.White

    for ((target, shape, index) in targets) {
        val tileX = rightStartX + index * (innerH + gap)
        val tile = generateTileBitmap(
            context = context,
            target = target,
            bgColor = tileBgColor,
            accentColor = iconTint,
            isLight = isLight,
            shapeStyle = shape,
            widthPx = innerH,
            heightPx = innerH
        )
        canvas.drawBitmap(tile, tileX, innerTopY, null)
    }

    return bitmap
}

// 2. AI Dock Bar
fun generateAiBarDock5Bitmap(
    context: Context,
    config: SlateWidgetConfig,
    widthPx: Int,
    heightPx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = widthPx.toFloat()
    val h = heightPx.toFloat()
    val density = context.resources.displayMetrics.density

    val maxBarH = 72f * density
    val barH = minOf(h, maxBarH)
    val topY = (h - barH) / 2f
    val bottomY = topY + barH

    val isLight = config.themeMode == "LIGHT"
    val cardBg = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val accentColor = Color(config.accentColorHex)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
    val barCornerRadius = barH / 2f
    canvas.drawRoundRect(RectF(0f, topY, w, bottomY), barCornerRadius, barCornerRadius, bgPaint)

    val pad = barH * 0.12f
    val innerH = (barH - (pad * 2f)).toInt()
    val innerTopY = topY + pad
    val gap = barH * 0.08f

    val list = listOf(
        AiTarget.GEMINI_TEXT,
        AiTarget.CHATGPT_TEXT,
        AiTarget.CLAUDE,
        AiTarget.GROK,
        AiTarget.PERPLEXITY
    )

    val tileW = ((w - (pad * 2f) - (gap * (list.size - 1))) / list.size).toInt()
    val tileBgColor = if (isLight) Color(0x0A000000) else Color(0x14FFFFFF)
    val iconTint = accentColor

    list.forEachIndexed { index, target ->
        val tileShape = when (index) {
            0 -> AiShapeStyle.CAPSULE_LEFT
            list.size - 1 -> AiShapeStyle.CAPSULE_RIGHT
            else -> AiShapeStyle.SQUIRCLE
        }

        val tile = generateTileBitmap(
            context = context,
            target = target,
            bgColor = tileBgColor,
            accentColor = iconTint,
            isLight = isLight,
            shapeStyle = tileShape,
            widthPx = tileW,
            heightPx = innerH
        )
        val xPos = pad + index * (tileW + gap)
        canvas.drawBitmap(tile, xPos, innerTopY, null)
    }

    return bitmap
}

// 3. AI Capsule Bar
fun generateAiBarCapsuleBitmap(
    context: Context,
    config: SlateWidgetConfig,
    widthPx: Int,
    heightPx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = widthPx.toFloat()
    val h = heightPx.toFloat()
    val density = context.resources.displayMetrics.density

    // Lock capsule height to 72dp max equivalent to prevent vertical bloating when stretched
    val maxBarH = 72f * density
    val barH = minOf(h, maxBarH)
    val topY = (h - barH) / 2f
    val bottomY = topY + barH

    val isLight = config.themeMode == "LIGHT"
    val cardBg = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val accentColor = Color(config.accentColorHex)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
    val barCornerRadius = barH / 2f
    canvas.drawRoundRect(RectF(0f, topY, w, bottomY), barCornerRadius, barCornerRadius, bgPaint)

    val pad = barH * 0.12f
    val innerH = (barH - (pad * 2f)).toInt()
    val innerTopY = topY + pad
    val gap = barH * 0.08f

    val items = listOf(
        Triple(AiTarget.CHATGPT_VOICE, AiShapeStyle.CAPSULE_LEFT, true),
        Triple(AiTarget.PERPLEXITY, AiShapeStyle.SQUIRCLE, false),
        Triple(AiTarget.CLAUDE, AiShapeStyle.SQUIRCLE, false),
        Triple(AiTarget.GEMINI_TEXT, AiShapeStyle.CAPSULE_RIGHT, true)
    )

    val tileW = ((w - (pad * 2f) - (gap * (items.size - 1))) / items.size).toInt()
    val tileBgColor = if (isLight) Color(0x0A000000) else Color(0x14FFFFFF)

    items.forEachIndexed { index, (target, shape, isAccent) ->
        val tile = generateTileBitmap(
            context = context,
            target = target,
            bgColor = tileBgColor,
            accentColor = accentColor,
            isLight = isLight,
            shapeStyle = shape,
            isPrimaryAccent = isAccent,
            widthPx = tileW,
            heightPx = innerH
        )
        val xPos = pad + index * (tileW + gap)
        canvas.drawBitmap(tile, xPos, innerTopY, null)
    }

    return bitmap
}

// 4. AI Dual Flagship Bar
fun generateAiBarDualFlagshipBitmap(
    context: Context,
    config: SlateWidgetConfig,
    widthPx: Int,
    heightPx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = widthPx.toFloat()
    val h = heightPx.toFloat()
    val density = context.resources.displayMetrics.density

    // Lock capsule height to 72dp max equivalent to prevent vertical expansion when stretched
    val maxBarH = 72f * density
    val barH = minOf(h, maxBarH)
    val topY = (h - barH) / 2f
    val bottomY = topY + barH

    val isLight = config.themeMode == "LIGHT"
    val cardBg = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val accentColor = Color(config.accentColorHex)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
    val barCornerRadius = barH / 2f
    canvas.drawRoundRect(RectF(0f, topY, w, bottomY), barCornerRadius, barCornerRadius, bgPaint)

    val pad = barH * 0.12f
    val innerH = (barH - (pad * 2f)).toInt()
    val innerTopY = topY + pad
    val gap = barH * 0.08f

    val pillW = ((w - (pad * 2f) - gap) / 2f).toInt()

    val leftPill = drawPillBaseBitmap(
        context = context,
        target = AiTarget.CHATGPT_TEXT,
        labelText = "GPT",
        accentColor = accentColor,
        isLight = isLight,
        widthPx = pillW,
        heightPx = innerH,
        logoSizePercent = 0.42f,
        textSizePercent = 0.34f,
        isCenteredLayout = true
    )
    canvas.drawBitmap(leftPill, pad, innerTopY, null)

    val rightPill = drawPillBaseBitmap(
        context = context,
        target = AiTarget.GEMINI_TEXT,
        labelText = "Gemini",
        accentColor = accentColor,
        isLight = isLight,
        widthPx = pillW,
        heightPx = innerH,
        logoSizePercent = 0.42f,
        textSizePercent = 0.34f,
        isCenteredLayout = true
    )
    canvas.drawBitmap(rightPill, pad + pillW + gap, innerTopY, null)

    return bitmap
}

// ============================================================================
// AI FOLDERS (CANVAS COMPOSITES)
// ============================================================================

// 1. AI Quad Folder
fun generateAiFolder4ClassicBitmap(
    context: Context,
    config: SlateWidgetConfig,
    widthPx: Int,
    heightPx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = widthPx.toFloat()
    val h = heightPx.toFloat()

    // 1. Lock outer card to a 1:1 square centered in widget bounds
    val cardSize = minOf(w, h)
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f

    val isLight = config.themeMode == "LIGHT"
    val cardBg = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val accentColor = Color(config.accentColorHex)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
    val cardCornerRadius = cardSize * 0.20f
    canvas.drawRoundRect(RectF(leftX, topY, leftX + cardSize, topY + cardSize), cardCornerRadius, cardCornerRadius, bgPaint)

    // 2. Inner 2x2 Grid Layout
    val pad = cardSize * 0.08f
    val gap = cardSize * 0.05f
    val tileSize = ((cardSize - (pad * 2f) - gap) / 2f).toInt()
    val tileBgColor = if (isLight) Color(0x0A000000) else Color(0x14FFFFFF)

    val grid = listOf(
        listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT),
        listOf(AiTarget.PERPLEXITY, AiTarget.CLAUDE)
    )

    grid.forEachIndexed { r, row ->
        row.forEachIndexed { c, target ->
            val tile = generateTileBitmap(
                context = context,
                target = target,
                bgColor = tileBgColor,
                accentColor = accentColor,
                isLight = isLight,
                shapeStyle = AiShapeStyle.SQUIRCLE,
                widthPx = tileSize,
                heightPx = tileSize
            )
            val xPos = leftX + pad + c * (tileSize + gap)
            val yPos = topY + pad + r * (tileSize + gap)
            canvas.drawBitmap(tile, xPos, yPos, null)
        }
    }

    return bitmap
}

// 2. AI Bento Folder
fun generateAiFolder6BentoHeroBitmap(
    context: Context,
    config: SlateWidgetConfig,
    widthPx: Int,
    heightPx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = widthPx.toFloat()
    val h = heightPx.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val cardBg = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val accentColor = Color(config.accentColorHex)

    val pad = minOf(w, h) * 0.04f
    val gap = minOf(w, h) * 0.025f

    // Constrain tile size to fit within both width AND height bounds
    val maxBotW = (w - (pad * 2f) - (gap * 3f)) / 4f
    val maxBotH = (h - (pad * 2f) - (gap * 2f)) / 3f
    val botTileSize = minOf(maxBotW, maxBotH).toInt().coerceAtLeast(1)
    val topTileSize = (botTileSize * 2) + gap.toInt()

    val cardW = (topTileSize * 2) + gap + (pad * 2f)
    val cardH = topTileSize + botTileSize + gap + (pad * 2f)

    // Center layout inside widget container
    val leftX = (w - cardW) / 2f
    val topY = (h - cardH) / 2f

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
    val cardCornerRadius = minOf(cardW, cardH) * 0.16f
    canvas.drawRoundRect(RectF(leftX, topY, leftX + cardW, topY + cardH), cardCornerRadius, cardCornerRadius, bgPaint)

    val tileBgColor = if (isLight) Color(0x0A000000) else Color(0x14FFFFFF)
    val startX = leftX + pad
    val startY = topY + pad

    // Row 1: Top Hero Tiles
    val top1 = generateTileBitmap(
        context = context,
        target = AiTarget.GEMINI_TEXT,
        bgColor = tileBgColor,
        accentColor = accentColor,
        isLight = isLight,
        shapeStyle = AiShapeStyle.CAPSULE_LEFT,
        widthPx = topTileSize,
        heightPx = topTileSize
    )
    canvas.drawBitmap(top1, startX, startY, null)

    val top2 = generateTileBitmap(
        context = context,
        target = AiTarget.CHATGPT_TEXT,
        bgColor = tileBgColor,
        accentColor = accentColor,
        isLight = isLight,
        shapeStyle = AiShapeStyle.CAPSULE_RIGHT,
        widthPx = topTileSize,
        heightPx = topTileSize
    )
    canvas.drawBitmap(top2, startX + topTileSize + gap, startY, null)

    // Row 2: Bottom 4 Tiles
    val bottomItems = listOf(
        Pair(AiTarget.CLAUDE, AiShapeStyle.CAPSULE_LEFT),
        Pair(AiTarget.GROK, AiShapeStyle.SQUIRCLE),
        Pair(AiTarget.DEEPSEEK, AiShapeStyle.SQUIRCLE),
        Pair(AiTarget.META_AI, AiShapeStyle.CAPSULE_RIGHT)
    )
    val botY = startY + topTileSize + gap

    bottomItems.forEachIndexed { i, (target, shape) ->
        val tile = generateTileBitmap(
            context = context,
            target = target,
            bgColor = tileBgColor,
            accentColor = accentColor,
            isLight = isLight,
            shapeStyle = shape,
            widthPx = botTileSize,
            heightPx = botTileSize
        )
        val botX = startX + i * (botTileSize + gap)
        canvas.drawBitmap(tile, botX, botY, null)
    }

    return bitmap
}

// 3. AI Side Bento Folder
fun generateAiFolder8BentoSideBitmap(
    context: Context,
    config: SlateWidgetConfig,
    widthPx: Int,
    heightPx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = widthPx.toFloat()
    val h = heightPx.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val cardBg = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val accentColor = Color(config.accentColorHex)

    val pad = minOf(w, h) * 0.04f
    val gap = minOf(w, h) * 0.025f

    // Constrain grid sizes to fit within both width and height bounds simultaneously
    val maxRightH = (h - (pad * 2f) - (gap * 2f)) / 3f
    val maxRightW = (w - (pad * 2f) - (gap * 2f)) / 3.5f
    val rightTileSize = minOf(maxRightW, maxRightH).toInt().coerceAtLeast(1)

    // Mathematically match two left hero tiles to the three right small tiles
    val leftTileSize = ((rightTileSize * 3 + gap) / 2f).toInt()

    val cardW = leftTileSize + (rightTileSize * 2) + (gap * 2f) + (pad * 2f)
    val cardH = (rightTileSize * 3) + (gap * 2f) + (pad * 2f)

    // Center card layout inside widget container
    val leftX = (w - cardW) / 2f
    val topY = (h - cardH) / 2f

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
    val cardCornerRadius = minOf(cardW, cardH) * 0.16f
    canvas.drawRoundRect(RectF(leftX, topY, leftX + cardW, topY + cardH), cardCornerRadius, cardCornerRadius, bgPaint)

    val tileBgColor = if (isLight) Color(0x0A000000) else Color(0x14FFFFFF)
    val startX = leftX + pad
    val startY = topY + pad

    // Left Hero Column (Outer edges hug left container curves)
    val left1 = generateTileBitmap(
        context = context,
        target = AiTarget.GEMINI_TEXT,
        bgColor = tileBgColor,
        accentColor = accentColor,
        isLight = isLight,
        shapeStyle = AiShapeStyle.CAPSULE_LEFT,
        widthPx = leftTileSize,
        heightPx = leftTileSize
    )
    canvas.drawBitmap(left1, startX, startY, null)

    val left2Y = startY + leftTileSize + gap
    val left2 = generateTileBitmap(
        context = context,
        target = AiTarget.CHATGPT_TEXT,
        bgColor = tileBgColor,
        accentColor = accentColor,
        isLight = isLight,
        shapeStyle = AiShapeStyle.CAPSULE_LEFT,
        widthPx = leftTileSize,
        heightPx = leftTileSize
    )
    canvas.drawBitmap(left2, startX, left2Y, null)

    // Right Grid (3 rows x 2 columns with top-right & bottom-right outer corner hugging)
    val rightGrid = listOf(
        listOf(Pair(AiTarget.CLAUDE, AiShapeStyle.SQUIRCLE), Pair(AiTarget.GROK, AiShapeStyle.CAPSULE_RIGHT)),
        listOf(Pair(AiTarget.PERPLEXITY, AiShapeStyle.SQUIRCLE), Pair(AiTarget.COPILOT, AiShapeStyle.SQUIRCLE)),
        listOf(Pair(AiTarget.DEEPSEEK, AiShapeStyle.SQUIRCLE), Pair(AiTarget.META_AI, AiShapeStyle.CAPSULE_RIGHT))
    )

    val rightStartX = startX + leftTileSize + gap
    rightGrid.forEachIndexed { r, row ->
        row.forEachIndexed { c, (target, shape) ->
            val tile = generateTileBitmap(
                context = context,
                target = target,
                bgColor = tileBgColor,
                accentColor = accentColor,
                isLight = isLight,
                shapeStyle = shape,
                widthPx = rightTileSize,
                heightPx = rightTileSize
            )
            val rx = rightStartX + c * (rightTileSize + gap)
            val ry = startY + r * (rightTileSize + gap)
            canvas.drawBitmap(tile, rx, ry, null)
        }
    }

    return bitmap
}

// 4. AI 3x3 Grid Folder
fun generateAiFolder9GridBitmap(
    context: Context,
    config: SlateWidgetConfig,
    widthPx: Int,
    heightPx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = widthPx.toFloat()
    val h = heightPx.toFloat()

    // 1. Lock outer card to a 1:1 square centered in widget bounds
    val cardSize = minOf(w, h)
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f

    val isLight = config.themeMode == "LIGHT"
    val cardBg = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val accentColor = Color(config.accentColorHex)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
    val cardCornerRadius = cardSize * 0.20f
    canvas.drawRoundRect(RectF(leftX, topY, leftX + cardSize, topY + cardSize), cardCornerRadius, cardCornerRadius, bgPaint)

    // 2. Inner 3x3 Grid Layout
    val pad = cardSize * 0.07f
    val gap = cardSize * 0.035f
    val tileSize = ((cardSize - (pad * 2f) - (gap * 2f)) / 3f).toInt()
    val tileBgColor = if (isLight) Color(0x0A000000) else Color(0x14FFFFFF)

    val grid = listOf(
        listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.COPILOT),
        listOf(AiTarget.GROK, AiTarget.CLAUDE, AiTarget.DEEPSEEK),
        listOf(AiTarget.PERPLEXITY, AiTarget.META_AI, AiTarget.POE)
    )

    grid.forEachIndexed { r, row ->
        row.forEachIndexed { c, target ->
            val tile = generateTileBitmap(
                context = context,
                target = target,
                bgColor = tileBgColor,
                accentColor = accentColor,
                isLight = isLight,
                shapeStyle = AiShapeStyle.SQUIRCLE,
                widthPx = tileSize,
                heightPx = tileSize
            )
            val xPos = leftX + pad + c * (tileSize + gap)
            val yPos = topY + pad + r * (tileSize + gap)
            canvas.drawBitmap(tile, xPos, yPos, null)
        }
    }

    return bitmap
}

// 4. AI Mega Folder
fun generateAiFolder10MegaBitmap(
    context: Context,
    config: SlateWidgetConfig,
    widthPx: Int,
    heightPx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = widthPx.toFloat()
    val h = heightPx.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val cardBg = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val accentColor = Color(config.accentColorHex)

    val pad = minOf(w, h) * 0.04f
    val gap = minOf(w, h) * 0.02f

    // Constrain tile size against both width and height bounds
    val maxTileW = (w - (pad * 2f) - (gap * 4f)) / 5f
    val maxTileH = (h - (pad * 2f) - gap) / 2f
    val tileSize = minOf(maxTileW, maxTileH).toInt().coerceAtLeast(1)

    val cardW = (tileSize * 5) + (gap * 4f) + (pad * 2f)
    val cardH = (tileSize * 2) + gap + (pad * 2f)

    // Center container card inside widget bounds
    val leftX = (w - cardW) / 2f
    val topY = (h - cardH) / 2f

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
    val cardCornerRadius = minOf(cardW, cardH) * 0.18f
    canvas.drawRoundRect(RectF(leftX, topY, leftX + cardW, topY + cardH), cardCornerRadius, cardCornerRadius, bgPaint)

    val tileBgColor = if (isLight) Color(0x0A000000) else Color(0x14FFFFFF)
    val startX = leftX + pad
    val startY = topY + pad

    val row1 = listOf(
        Pair(AiTarget.GEMINI_TEXT, AiShapeStyle.CAPSULE_LEFT),
        Pair(AiTarget.CHATGPT_TEXT, AiShapeStyle.SQUIRCLE),
        Pair(AiTarget.COPILOT, AiShapeStyle.SQUIRCLE),
        Pair(AiTarget.CLAUDE, AiShapeStyle.SQUIRCLE),
        Pair(AiTarget.GROK, AiShapeStyle.CAPSULE_RIGHT)
    )

    val row2 = listOf(
        Pair(AiTarget.PERPLEXITY, AiShapeStyle.CAPSULE_LEFT),
        Pair(AiTarget.DEEPSEEK, AiShapeStyle.SQUIRCLE),
        Pair(AiTarget.META_AI, AiShapeStyle.SQUIRCLE),
        Pair(AiTarget.POE, AiShapeStyle.SQUIRCLE),
        Pair(AiTarget.PI, AiShapeStyle.CAPSULE_RIGHT)
    )

    row1.forEachIndexed { i, (target, shape) ->
        val tile = generateTileBitmap(
            context = context,
            target = target,
            bgColor = tileBgColor,
            accentColor = accentColor,
            isLight = isLight,
            shapeStyle = shape,
            widthPx = tileSize,
            heightPx = tileSize
        )
        val x = startX + i * (tileSize + gap)
        canvas.drawBitmap(tile, x, startY, null)
    }

    val row2Y = startY + tileSize + gap
    row2.forEachIndexed { i, (target, shape) ->
        val tile = generateTileBitmap(
            context = context,
            target = target,
            bgColor = tileBgColor,
            accentColor = accentColor,
            isLight = isLight,
            shapeStyle = shape,
            widthPx = tileSize,
            heightPx = tileSize
        )
        val x = startX + i * (tileSize + gap)
        canvas.drawBitmap(tile, x, row2Y, null)
    }

    return bitmap
}

// 4. AI Asymmetric Bento
fun generateAiFolder7AsymmetricBitmap(
    context: Context,
    config: SlateWidgetConfig,
    widthPx: Int,
    heightPx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = widthPx.toFloat()
    val h = heightPx.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val cardBg = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val accentColor = Color(config.accentColorHex)

    val pad = minOf(w, h) * 0.04f
    val gap = minOf(w, h) * 0.025f

    val availW = w - 2 * pad
    val availH = h - 2 * pad

    val maxSmallW = (availW - 3 * gap) / 4f
    val maxSmallH = (availH - 3 * gap) / 4f

    val smallSize = minOf(maxSmallW, maxSmallH).toInt().coerceAtLeast(1)
    val bigSize = (smallSize * 3) + gap.toInt() * 2
    val medW = ((bigSize - gap.toInt()) / 2)

    val cardW = bigSize + gap + smallSize + (pad * 2f)
    val cardH = (smallSize * 4) + (gap * 3f) + (pad * 2f)

    val leftX = (w - cardW) / 2f
    val topY = (h - cardH) / 2f

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
    val cardCornerRadius = minOf(cardW, cardH) * 0.16f
    canvas.drawRoundRect(RectF(leftX, topY, leftX + cardW, topY + cardH), cardCornerRadius, cardCornerRadius, bgPaint)

    val tileBgColor = if (isLight) Color(0x0A000000) else Color(0x14FFFFFF)
    val startX = leftX + pad
    val startY = topY + pad

    // 1. Big Tile (ChatGPT, top-left primary)
    val bigTile = generateTileBitmap(
        context = context,
        target = AiTarget.CHATGPT_TEXT,
        bgColor = tileBgColor,
        accentColor = accentColor,
        isLight = isLight,
        shapeStyle = AiShapeStyle.SQUIRCLE,
        isPrimaryAccent = true,
        widthPx = bigSize,
        heightPx = bigSize
    )
    canvas.drawBitmap(bigTile, startX, startY, null)

    // 2. Medium Row (below big tile)
    val medY = startY + bigSize + gap

    // Gemini (bottom-left) - CAPSULE_LEFT hugs outer bottom-left corner
    val med1 = generateTileBitmap(
        context = context,
        target = AiTarget.GEMINI_TEXT,
        bgColor = tileBgColor,
        accentColor = accentColor,
        isLight = isLight,
        shapeStyle = AiShapeStyle.CAPSULE_LEFT,
        widthPx = medW,
        heightPx = smallSize
    )
    canvas.drawBitmap(med1, startX, medY, null)

    // Claude (bottom-center) - SQUIRCLE
    val med2 = generateTileBitmap(
        context = context,
        target = AiTarget.CLAUDE,
        bgColor = tileBgColor,
        accentColor = accentColor,
        isLight = isLight,
        shapeStyle = AiShapeStyle.SQUIRCLE,
        widthPx = medW,
        heightPx = smallSize
    )
    canvas.drawBitmap(med2, startX + medW + gap, medY, null)

    // 3. Right Small Stack (4 rows)
    val rightX = startX + bigSize + gap
    val rightItems = listOf(
        Pair(AiTarget.GROK, AiShapeStyle.CAPSULE_RIGHT),   // Grok (top-right) - CAPSULE_RIGHT hugs outer curve
        Pair(AiTarget.COPILOT, AiShapeStyle.SQUIRCLE),
        Pair(AiTarget.PERPLEXITY, AiShapeStyle.SQUIRCLE),
        Pair(AiTarget.META_AI, AiShapeStyle.CAPSULE_RIGHT)  // Meta AI (bottom-right) - CAPSULE_RIGHT hugs outer curve
    )

    rightItems.forEachIndexed { i, (target, shape) ->
        val tile = generateTileBitmap(
            context = context,
            target = target,
            bgColor = tileBgColor,
            accentColor = accentColor,
            isLight = isLight,
            shapeStyle = shape,
            widthPx = smallSize,
            heightPx = smallSize
        )
        val ry = startY + i * (smallSize + gap)
        canvas.drawBitmap(tile, rightX, ry, null)
    }

    return bitmap
}