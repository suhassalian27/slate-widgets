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
    // Icon color tints directly with the selected Accent Color
    val logoColor = if (isPrimaryAccent) Color.Black.toArgb() else accentColor.toArgb()
    val strokeColor = if (isLight) Color(0xFFD1D1D6).toArgb() else Color(0xFF2C2C2E).toArgb()

    if (shapeStyle != AiShapeStyle.FRAMELESS) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = currentBgColor
            style = Paint.Style.FILL
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strokeColor
            style = Paint.Style.STROKE
            strokeWidth = if (!isPrimaryAccent) 5f else 0f
        }

        val margin = 5f
        val cx = w / 2f
        val cy = h / 2f
        val halfTile = (minDim / 2f) - margin
        val squircleRadius = minDim * 0.28f
        val fullCapRadius = (h - (margin * 2f)) / 2f

        // Centered 1:1 Square Rect
        val squareRect = RectF(cx - halfTile, cy - halfTile, cx + halfTile, cy + halfTile)
        val fullRect = RectF(margin, margin, w - margin, h - margin)

        when (shapeStyle) {
            AiShapeStyle.SQUIRCLE -> {
                canvas.drawRoundRect(squareRect, squircleRadius, squircleRadius, bgPaint)
                if (!isPrimaryAccent) canvas.drawRoundRect(squareRect, squircleRadius, squircleRadius, strokePaint)
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
            val maxLogoSize = minDim * 0.48f
            val actualLogoSize = if (isPrimaryAccent) minDim * 0.54f else maxLogoSize

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
    logoSizePercent: Float = 0.46f,
    textSizePercent: Float = 0.36f,
    isCenteredLayout: Boolean = true
): Bitmap {
    val wPx = widthPx.coerceAtLeast(1)
    val hPx = heightPx.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = wPx.toFloat()
    val h = hPx.toFloat()

    val contentColor = accentColor.toArgb()
    val pillBgColor = accentColor.copy(alpha = 0.20f).toArgb()
    val strokeColor = accentColor.copy(alpha = 0.55f).toArgb()

    val margin = 2f
    val rect = RectF(margin, margin, w - margin, h - margin)
    val capsuleRadius = (h - (margin * 2f)) / 2f

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pillBgColor
        style = Paint.Style.FILL
    }

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
    }

    canvas.drawRoundRect(rect, capsuleRadius, capsuleRadius, bgPaint)
    canvas.drawRoundRect(rect, capsuleRadius, capsuleRadius, strokePaint)

    val logoSize = h * logoSizePercent
    val textSize = h * textSizePercent
    val itemGap = h * 0.14f

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
        capsuleRadius * 0.75f
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

    val isLight = config.themeMode == "LIGHT"
    val cardBg = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val accentColor = Color(config.accentColorHex)
    val dividerColor = if (isLight) 0x33000000 else 0x33FFFFFF

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
    canvas.drawRoundRect(RectF(0f, 0f, w, h), h * 0.45f, h * 0.45f, bgPaint)

    val pad = h * 0.08f
    val innerH = (h - (pad * 2f)).toInt()

    // 1. Draw Hero Pill
    val heroW = (w * 0.42f).toInt()
    val heroBitmap = drawPillBaseBitmap(context, AiTarget.GEMINI_TEXT, "Gemini", accentColor, isLight, heroW, innerH, isCenteredLayout = false)
    canvas.drawBitmap(heroBitmap, pad, pad, null)

    // 2. Vertical Divider
    val divX = pad + heroW + (w * 0.03f)
    val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dividerColor
        strokeWidth = 3f
    }
    canvas.drawLine(divX, h * 0.3f, divX, h * 0.7f, divPaint)

    // 3. Right Icons
    val iconW = ((w - divX - (pad * 2f)) / 3.2f).toInt()
    val startIconX = divX + (w * 0.03f)

    val targets = listOf(
        Triple(AiTarget.CHATGPT_TEXT, AiShapeStyle.SQUIRCLE, startIconX),
        Triple(AiTarget.CLAUDE, AiShapeStyle.SQUIRCLE, startIconX + iconW + pad),
        Triple(AiTarget.GROK, AiShapeStyle.CAPSULE_RIGHT, startIconX + (iconW + pad) * 2f)
    )

    for ((target, shape, xPos) in targets) {
        val tile = generateTileBitmap(context, target, Color(config.backgroundColorHex).copy(alpha = config.opacity), accentColor, isLight, shape, widthPx = iconW, heightPx = innerH)
        canvas.drawBitmap(tile, xPos, pad, null)
    }

    return bitmap
}

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

    val isLight = config.themeMode == "LIGHT"
    val cardBg = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val accentColor = Color(config.accentColorHex)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
    canvas.drawRoundRect(RectF(0f, 0f, w, h), h * 0.45f, h * 0.45f, bgPaint)

    val list = listOf(
        AiTarget.GEMINI_TEXT,
        AiTarget.CHATGPT_TEXT,
        AiTarget.CLAUDE,
        AiTarget.GROK,
        AiTarget.PERPLEXITY
    )

    val pad = h * 0.08f
    val innerH = (h - (pad * 2f)).toInt()
    val gap = w * 0.015f
    val tileW = ((w - (pad * 2f) - (gap * (list.size - 1))) / list.size).toInt()

    list.forEachIndexed { index, target ->
        val tileShape = when (index) {
            0 -> AiShapeStyle.CAPSULE_LEFT
            list.size - 1 -> AiShapeStyle.CAPSULE_RIGHT
            else -> AiShapeStyle.SQUIRCLE
        }

        val tile = generateTileBitmap(context, target, Color(config.backgroundColorHex).copy(alpha = config.opacity), accentColor, isLight, tileShape, widthPx = tileW, heightPx = innerH)
        val xPos = pad + index * (tileW + gap)
        canvas.drawBitmap(tile, xPos, pad, null)
    }

    return bitmap
}

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

    val isLight = config.themeMode == "LIGHT"
    val cardBg = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val accentColor = Color(config.accentColorHex)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
    canvas.drawRoundRect(RectF(0f, 0f, w, h), h * 0.45f, h * 0.45f, bgPaint)

    val pad = h * 0.08f
    val innerH = (h - (pad * 2f)).toInt()
    val gap = w * 0.018f
    val tileW = ((w - (pad * 2f) - (gap * 3f)) / 4f).toInt()

    val items = listOf(
        Triple(AiTarget.CHATGPT_VOICE, AiShapeStyle.CAPSULE_LEFT, true),
        Triple(AiTarget.PERPLEXITY, AiShapeStyle.SQUIRCLE, false),
        Triple(AiTarget.CLAUDE, AiShapeStyle.SQUIRCLE, false),
        Triple(AiTarget.GEMINI_TEXT, AiShapeStyle.CAPSULE_RIGHT, true)
    )

    items.forEachIndexed { index, (target, shape, isAccent) ->
        val tile = generateTileBitmap(context, target, Color(config.backgroundColorHex).copy(alpha = config.opacity), accentColor, isLight, shape, isPrimaryAccent = isAccent, widthPx = tileW, heightPx = innerH)
        val xPos = pad + index * (tileW + gap)
        canvas.drawBitmap(tile, xPos, pad, null)
    }

    return bitmap
}

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

    val isLight = config.themeMode == "LIGHT"
    val cardBg = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val accentColor = Color(config.accentColorHex)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
    canvas.drawRoundRect(RectF(0f, 0f, w, h), h * 0.45f, h * 0.45f, bgPaint)

    val pad = h * 0.08f
    val innerH = (h - (pad * 2f)).toInt()
    val gap = w * 0.02f
    val pillW = ((w - (pad * 2f) - gap) / 2f).toInt()

    val leftPill = drawPillBaseBitmap(context, AiTarget.CHATGPT_TEXT, "GPT", accentColor, isLight, pillW, innerH, isCenteredLayout = true)
    canvas.drawBitmap(leftPill, pad, pad, null)

    val rightPill = drawPillBaseBitmap(context, AiTarget.GEMINI_TEXT, "Gemini", accentColor, isLight, pillW, innerH, isCenteredLayout = true)
    canvas.drawBitmap(rightPill, pad + pillW + gap, pad, null)

    return bitmap
}

// ============================================================================
// AI FOLDERS (CANVAS COMPOSITES)
// ============================================================================

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

    val isLight = config.themeMode == "LIGHT"
    val cardBg = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val accentColor = Color(config.accentColorHex)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
    canvas.drawRoundRect(RectF(0f, 0f, w, h), 48f, 48f, bgPaint)

    val pad = w * 0.08f
    val gap = w * 0.05f
    val tileSize = ((w - (pad * 2f) - gap) / 2f).toInt()

    val grid = listOf(
        listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT),
        listOf(AiTarget.GROK, AiTarget.CLAUDE)
    )

    grid.forEachIndexed { r, row ->
        row.forEachIndexed { c, target ->
            val tile = generateTileBitmap(context, target, Color(config.backgroundColorHex).copy(alpha = config.opacity), accentColor, isLight, AiShapeStyle.SQUIRCLE, widthPx = tileSize, heightPx = tileSize)
            val xPos = pad + c * (tileSize + gap)
            val yPos = pad + r * (tileSize + gap)
            canvas.drawBitmap(tile, xPos, yPos, null)
        }
    }

    return bitmap
}

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

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
    canvas.drawRoundRect(RectF(0f, 0f, w, h), 48f, 48f, bgPaint)

    val pad = w * 0.06f
    val gap = w * 0.03f

    val botTileSize = ((w - (pad * 2f) - (gap * 3f)) / 4f).toInt()
    val topTileSize = (botTileSize * 2) + gap.toInt()

    val top1 = generateTileBitmap(context, AiTarget.GEMINI_TEXT, Color(config.backgroundColorHex).copy(alpha = config.opacity), accentColor, isLight, AiShapeStyle.SQUIRCLE, widthPx = topTileSize, heightPx = topTileSize)
    canvas.drawBitmap(top1, pad, pad, null)

    val top2 = generateTileBitmap(context, AiTarget.CHATGPT_TEXT, Color(config.backgroundColorHex).copy(alpha = config.opacity), accentColor, isLight, AiShapeStyle.SQUIRCLE, widthPx = topTileSize, heightPx = topTileSize)
    canvas.drawBitmap(top2, pad + topTileSize + gap, pad, null)

    val bottomList = listOf(AiTarget.CLAUDE, AiTarget.GROK, AiTarget.DEEPSEEK, AiTarget.META_AI)
    val botY = pad + topTileSize + gap
    bottomList.forEachIndexed { i, target ->
        val tile = generateTileBitmap(context, target, Color(config.backgroundColorHex).copy(alpha = config.opacity), accentColor, isLight, AiShapeStyle.SQUIRCLE, widthPx = botTileSize, heightPx = botTileSize)
        val botX = pad + i * (botTileSize + gap)
        canvas.drawBitmap(tile, botX, botY, null)
    }

    return bitmap
}

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

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
    canvas.drawRoundRect(RectF(0f, 0f, w, h), 48f, 48f, bgPaint)

    val pad = w * 0.05f
    val gap = w * 0.025f

    val rightTileSize = ((h - (pad * 2f) - (gap * 2f)) / 3f).toInt()
    val leftTileSize = (rightTileSize * 1.5f + gap / 2f).toInt()

    val left1 = generateTileBitmap(context, AiTarget.GEMINI_TEXT, Color(config.backgroundColorHex).copy(alpha = config.opacity), accentColor, isLight, AiShapeStyle.SQUIRCLE, widthPx = leftTileSize, heightPx = leftTileSize)
    canvas.drawBitmap(left1, pad, pad, null)

    val left2 = generateTileBitmap(context, AiTarget.CHATGPT_TEXT, Color(config.backgroundColorHex).copy(alpha = config.opacity), accentColor, isLight, AiShapeStyle.SQUIRCLE, widthPx = leftTileSize, heightPx = leftTileSize)
    canvas.drawBitmap(left2, pad, pad + leftTileSize + gap, null)

    val rightGrid = listOf(
        listOf(AiTarget.CLAUDE, AiTarget.GROK),
        listOf(AiTarget.PERPLEXITY, AiTarget.COPILOT),
        listOf(AiTarget.DEEPSEEK, AiTarget.META_AI)
    )

    val rightStartX = pad + leftTileSize + gap
    rightGrid.forEachIndexed { r, row ->
        row.forEachIndexed { c, target ->
            val tile = generateTileBitmap(context, target, Color(config.backgroundColorHex).copy(alpha = config.opacity), accentColor, isLight, AiShapeStyle.SQUIRCLE, widthPx = rightTileSize, heightPx = rightTileSize)
            val rx = rightStartX + c * (rightTileSize + gap)
            val ry = pad + r * (rightTileSize + gap)
            canvas.drawBitmap(tile, rx, ry, null)
        }
    }

    return bitmap
}

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

    val isLight = config.themeMode == "LIGHT"
    val cardBg = Color(config.backgroundColorHex).copy(alpha = config.opacity).toArgb()
    val accentColor = Color(config.accentColorHex)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
    canvas.drawRoundRect(RectF(0f, 0f, w, h), 48f, 48f, bgPaint)

    val pad = w * 0.06f
    val gap = w * 0.03f
    val tileSize = ((w - (pad * 2f) - (gap * 2f)) / 3f).toInt()

    val grid = listOf(
        listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.COPILOT),
        listOf(AiTarget.GROK, AiTarget.CLAUDE, AiTarget.DEEPSEEK),
        listOf(AiTarget.PERPLEXITY, AiTarget.META_AI, AiTarget.POE)
    )

    grid.forEachIndexed { r, row ->
        row.forEachIndexed { c, target ->
            val tile = generateTileBitmap(context, target, Color(config.backgroundColorHex).copy(alpha = config.opacity), accentColor, isLight, AiShapeStyle.SQUIRCLE, widthPx = tileSize, heightPx = tileSize)
            val xPos = pad + c * (tileSize + gap)
            val yPos = pad + r * (tileSize + gap)
            canvas.drawBitmap(tile, xPos, yPos, null)
        }
    }

    return bitmap
}

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

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
    canvas.drawRoundRect(RectF(0f, 0f, w, h), 48f, 48f, bgPaint)

    val pad = w * 0.04f
    val gap = w * 0.02f
    val tileSize = ((w - (pad * 2f) - (gap * 4f)) / 5f).toInt()

    val row1 = listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.COPILOT, AiTarget.CLAUDE, AiTarget.GROK)
    val row2 = listOf(AiTarget.PERPLEXITY, AiTarget.DEEPSEEK, AiTarget.META_AI, AiTarget.POE, AiTarget.PI)

    row1.forEachIndexed { i, target ->
        val tile = generateTileBitmap(context, target, Color(config.backgroundColorHex).copy(alpha = config.opacity), accentColor, isLight, AiShapeStyle.SQUIRCLE, widthPx = tileSize, heightPx = tileSize)
        val x = pad + i * (tileSize + gap)
        canvas.drawBitmap(tile, x, pad, null)
    }

    val row2Y = pad + tileSize + gap
    row2.forEachIndexed { i, target ->
        val tile = generateTileBitmap(context, target, Color(config.backgroundColorHex).copy(alpha = config.opacity), accentColor, isLight, AiShapeStyle.SQUIRCLE, widthPx = tileSize, heightPx = tileSize)
        val x = pad + i * (tileSize + gap)
        canvas.drawBitmap(tile, x, row2Y, null)
    }

    return bitmap
}

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

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardBg }
    canvas.drawRoundRect(RectF(0f, 0f, w, h), 48f, 48f, bgPaint)

    val pad = w * 0.05f
    val gap = w * 0.025f

    val smallSize = ((w - (pad * 2f) - (gap * 3f)) / 4f).toInt()
    val bigSize = (smallSize * 3) + gap.toInt() * 2
    val medW = ((bigSize - gap.toInt()) / 2)

    val bigTile = generateTileBitmap(context, AiTarget.CHATGPT_TEXT, Color(config.backgroundColorHex).copy(alpha = config.opacity), accentColor, isLight, AiShapeStyle.SQUIRCLE, isPrimaryAccent = true, widthPx = bigSize, heightPx = bigSize)
    canvas.drawBitmap(bigTile, pad, pad, null)

    val medY = pad + bigSize + gap
    val med1 = generateTileBitmap(context, AiTarget.GEMINI_TEXT, Color(config.backgroundColorHex).copy(alpha = config.opacity), accentColor, isLight, AiShapeStyle.SQUIRCLE, widthPx = medW, heightPx = smallSize)
    canvas.drawBitmap(med1, pad, medY, null)

    val med2 = generateTileBitmap(context, AiTarget.CLAUDE, Color(config.backgroundColorHex).copy(alpha = config.opacity), accentColor, isLight, AiShapeStyle.SQUIRCLE, widthPx = medW, heightPx = smallSize)
    canvas.drawBitmap(med2, pad + medW + gap, medY, null)

    val rightX = pad + bigSize + gap
    val rightList = listOf(AiTarget.GROK, AiTarget.COPILOT, AiTarget.PERPLEXITY, AiTarget.META_AI)
    rightList.forEachIndexed { i, target ->
        val tile = generateTileBitmap(context, target, Color(config.backgroundColorHex).copy(alpha = config.opacity), accentColor, isLight, AiShapeStyle.SQUIRCLE, widthPx = smallSize, heightPx = smallSize)
        val ry = pad + i * (smallSize + gap)
        canvas.drawBitmap(tile, rightX, ry, null)
    }

    return bitmap
}