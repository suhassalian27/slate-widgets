package com.altusix.slate.widgets.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.content.ContextCompat
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.getSafeBgColor
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius
import kotlin.math.cos
import kotlin.math.sin


enum class AiShapeStyle {
    SQUIRCLE,
    CIRCLE,
    HEXAGON,
    FRAMELESS,
    CAPSULE_LEFT,
    CAPSULE_RIGHT,
    CORNER_TOP_LEFT,
    CORNER_TOP_RIGHT,
    CORNER_BOTTOM_LEFT,
    CORNER_BOTTOM_RIGHT
}

fun generateTileBitmap(
    context: Context,
    target: AiTarget,
    bgColorInt: Int,
    accentColorInt: Int,
    isLight: Boolean,
    shapeStyle: AiShapeStyle,
    showTextLabel: Boolean = false,
    customText: String? = null,
    isPrimaryAccent: Boolean = false,
    forceSquare: Boolean = false,
    widthPx: Int,
    heightPx: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val scaleFactor = maxOf(density, 3.5f)
    val wPx = widthPx.coerceAtLeast(1)
    val hPx = heightPx.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = wPx.toFloat()
    val h = hPx.toFloat()
    val minDim = minOf(w, h)

    val currentBgColor = if (isPrimaryAccent) accentColorInt else bgColorInt
    val logoColor = if (isPrimaryAccent) (if (isLight) Color.WHITE else Color.BLACK) else accentColorInt

    if (shapeStyle != AiShapeStyle.FRAMELESS) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = currentBgColor
            style = Paint.Style.FILL
        }

        val margin = scaleFactor * 1f
        val cx = w / 2f
        val cy = h / 2f
        val halfTile = (minDim / 2f) - margin

        val outerRadius = getStandardCornerRadius(scaleFactor)
        val concentricRadius = (outerRadius - margin).coerceAtLeast(minDim * 0.20f)
        val squircleRadius = outerRadius.coerceAtMost(minDim * 0.26f)
        val fullCapRadius = (h - (margin * 2f)) / 2f

        val squareRect = RectF(cx - halfTile, cy - halfTile, cx + halfTile, cy + halfTile)
        val fullRect = RectF(margin, margin, w - margin, h - margin)
        val targetRect = if (forceSquare) squareRect else fullRect

        when (shapeStyle) {
            AiShapeStyle.SQUIRCLE -> {
                canvas.drawRoundRect(targetRect, squircleRadius, squircleRadius, bgPaint)
            }
            AiShapeStyle.CIRCLE -> {
                if (forceSquare) {
                    val radius = (minDim / 2f) - margin
                    canvas.drawCircle(cx, cy, radius, bgPaint)
                } else {
                    canvas.drawRoundRect(fullRect, fullCapRadius, fullCapRadius, bgPaint)
                }
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
            }
            AiShapeStyle.CORNER_TOP_LEFT -> {
                val radii = floatArrayOf(
                    concentricRadius, concentricRadius,
                    squircleRadius, squircleRadius,
                    squircleRadius, squircleRadius,
                    squircleRadius, squircleRadius
                )
                val path = Path().apply { addRoundRect(fullRect, radii, Path.Direction.CW) }
                canvas.drawPath(path, bgPaint)
            }
            AiShapeStyle.CORNER_TOP_RIGHT -> {
                val radii = floatArrayOf(
                    squircleRadius, squircleRadius,
                    concentricRadius, concentricRadius,
                    squircleRadius, squircleRadius,
                    squircleRadius, squircleRadius
                )
                val path = Path().apply { addRoundRect(fullRect, radii, Path.Direction.CW) }
                canvas.drawPath(path, bgPaint)
            }
            AiShapeStyle.CORNER_BOTTOM_RIGHT -> {
                val radii = floatArrayOf(
                    squircleRadius, squircleRadius,
                    squircleRadius, squircleRadius,
                    concentricRadius, concentricRadius,
                    squircleRadius, squircleRadius
                )
                val path = Path().apply { addRoundRect(fullRect, radii, Path.Direction.CW) }
                canvas.drawPath(path, bgPaint)
            }
            AiShapeStyle.CORNER_BOTTOM_LEFT -> {
                val radii = floatArrayOf(
                    squircleRadius, squircleRadius,
                    squircleRadius, squircleRadius,
                    squircleRadius, squircleRadius,
                    concentricRadius, concentricRadius
                )
                val path = Path().apply { addRoundRect(fullRect, radii, Path.Direction.CW) }
                canvas.drawPath(path, bgPaint)
            }
        }
    }

    val resId = context.resources.getIdentifier(target.drawableResName, "drawable", context.packageName)

    if (showTextLabel && !customText.isNullOrEmpty()) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = logoColor
            textSize = h * 0.38f
            textAlign = Paint.Align.CENTER
            typeface = getSlateFont(context, weight = 700)
        }
        val fontMetrics = textPaint.fontMetrics
        val textY = (h / 2f) - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(customText, w / 2f, textY, textPaint)
    } else if (resId != 0) {
        val drawable = ContextCompat.getDrawable(context, resId)
        if (drawable != null) {
            val maxLogoSize = minDim * 0.52f
            val actualLogoSize = if (isPrimaryAccent) minDim * 0.58f else maxLogoSize

            val intrinsicW = drawable.intrinsicWidth.toFloat()
            val intrinsicH = drawable.intrinsicHeight.toFloat()

            var drawW = actualLogoSize
            var drawH = actualLogoSize

            if (intrinsicW > 0f && intrinsicH > 0f) {
                val aspectRatio = intrinsicW / intrinsicH
                if (aspectRatio > 1f) drawH = actualLogoSize / aspectRatio else drawW = actualLogoSize * aspectRatio
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



// 1-9 SINGLE AI ICON (2x2)
fun generateSingleAiIconBitmap(
    context: Context,
    target: AiTarget,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val scaleFactor = maxOf(density, 3.5f)
    val w = (wDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())
    val h = (hDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    return generateTileBitmap(
        context = context,
        target = target,
        bgColorInt = bgColor,
        accentColorInt = accentColorInt,
        isLight = isLight,
        shapeStyle = AiShapeStyle.SQUIRCLE,
        forceSquare = !isResponsive,
        widthPx = w,
        heightPx = h
    )
}

// 10. AI PRIMARY BAR (4x1)
fun generateAiBarHeroPrimaryBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val scaleFactor = maxOf(density, 3.5f)
    val w = (wDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())
    val h = (hDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 4.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h.toFloat() - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w.toFloat() - (margin * 2f)) {
            cardW = w.toFloat() - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardW = cardRect.width()
    val cardH = cardRect.height()
    val aspectRatio = cardW / cardH

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }

    val tileBgColor = if (isLight) Color.parseColor("#0A000000") else Color.parseColor("#14FFFFFF")
    val iconTint = if (isLight) Color.BLACK else Color.WHITE

    if (aspectRatio >= 2.0f) {
        // ================================================================
        // 1. HORIZONTAL CAPSULE BAR MODE
        // ================================================================
        val cornerRadius = cardH / 2f
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

        val pad = cardH * 0.12f
        val innerH = cardH - (pad * 2f)
        val innerTopY = cardRect.top + pad

        val gap = innerH * 0.06f
        val divSpacing = innerH * 0.12f

        val availContentW = cardW - (pad * 2f)
        val rightTileCount = 3
        val rightTilesTotalW = (innerH * rightTileCount) + (gap * (rightTileCount - 1))

        val heroW = (availContentW - (divSpacing * 2f) - rightTilesTotalW).toInt().coerceAtLeast((innerH * 1.2f).toInt())
        val startX = cardRect.left + pad

        // A. Draw Gemini Text-Only Pill
        val heroBitmap = drawTextOnlyPillBitmap(
            context = context,
            labelText = "Gemini",
            accentColorInt = accentColorInt,
            isLight = isLight,
            widthPx = heroW,
            heightPx = innerH.toInt()
        )
        canvas.drawBitmap(heroBitmap, startX, innerTopY, null)

        // B. Draw Vertical Divider
        val divX = startX + heroW + divSpacing
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.parseColor("#26000000") else Color.parseColor("#26FFFFFF")
            strokeWidth = scaleFactor * 0.8f
        }
        canvas.drawLine(divX, cardRect.centerY() - (innerH * 0.28f), divX, cardRect.centerY() + (innerH * 0.28f), divPaint)

        // C. Draw Right Icons (ChatGPT, Claude, Grok)
        val rightStartX = divX + divSpacing
        val targets = listOf(
            Triple(AiTarget.CHATGPT_TEXT, AiShapeStyle.SQUIRCLE, 0),
            Triple(AiTarget.CLAUDE, AiShapeStyle.SQUIRCLE, 1),
            Triple(AiTarget.GROK, AiShapeStyle.CAPSULE_RIGHT, 2)
        )

        for ((target, shape, index) in targets) {
            val tileX = rightStartX + index * (innerH + gap)
            val tile = generateTileBitmap(
                context = context,
                target = target,
                bgColorInt = tileBgColor,
                accentColorInt = iconTint,
                isLight = isLight,
                shapeStyle = shape,
                widthPx = innerH.toInt(),
                heightPx = innerH.toInt()
            )
            canvas.drawBitmap(tile, tileX, innerTopY, null)
        }

    } else {
        // ================================================================
        // 2. TALL / SQUARE RESPONSIVE BENTO MODE
        // ================================================================
        val cornerRadius = getStandardCornerRadius(scaleFactor)
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

        val pad = minOf(cardW, cardH) * 0.06f
        val gap = minOf(cardW, cardH) * 0.035f

        val availW = cardW - (pad * 2f)
        val availH = cardH - (pad * 2f)

        val topH = ((availH - gap) * 0.48f).toInt().coerceAtLeast(1)
        val botH = (availH - gap - topH).toInt().coerceAtLeast(1)

        val startX = cardRect.left + pad
        val startY = cardRect.top + pad

        // Row 1: Gemini Text-Only Pill
        val heroBitmap = drawTextOnlyPillBitmap(
            context = context,
            labelText = "Gemini",
            accentColorInt = accentColorInt,
            isLight = isLight,
            widthPx = availW.toInt(),
            heightPx = topH
        )
        canvas.drawBitmap(heroBitmap, startX, startY, null)

        // Row 2: ChatGPT, Claude, Grok Icons (Concentric corner shape mapping)
        val botTileW = ((availW - (gap * 2f)) / 3f).toInt().coerceAtLeast(1)
        val botY = startY + topH + gap

        val bottomTargets = listOf(
            Pair(AiTarget.CHATGPT_TEXT, AiShapeStyle.CORNER_BOTTOM_LEFT),
            Pair(AiTarget.CLAUDE, AiShapeStyle.SQUIRCLE),
            Pair(AiTarget.GROK, AiShapeStyle.CORNER_BOTTOM_RIGHT)
        )

        bottomTargets.forEachIndexed { i, (target, shape) ->
            val tile = generateTileBitmap(
                context = context,
                target = target,
                bgColorInt = tileBgColor,
                accentColorInt = iconTint,
                isLight = isLight,
                shapeStyle = shape,
                widthPx = botTileW,
                heightPx = botH
            )
            val bx = startX + i * (botTileW + gap)
            canvas.drawBitmap(tile, bx, botY, null)
        }
    }

    return bitmap
}

private fun drawTextOnlyPillBitmap(
    context: Context,
    labelText: String,
    accentColorInt: Int,
    isLight: Boolean,
    widthPx: Int,
    heightPx: Int
): Bitmap {
    val wPx = widthPx.coerceAtLeast(1)
    val hPx = heightPx.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = wPx.toFloat()
    val h = hPx.toFloat()

    val contentColor = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val pillBgColor = if (isLight) {
        Color.parseColor("#0F000000")
    } else {
        Color.argb(30, Color.red(accentColorInt), Color.green(accentColorInt), Color.blue(accentColorInt))
    }

    val rect = RectF(0f, 0f, w, h)
    val capsuleRadius = rect.height() / 2f

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pillBgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, capsuleRadius, capsuleRadius, bgPaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = contentColor
        textSize = h * 0.38f
        typeface = getSlateFont(context, weight = 600)
        textAlign = Paint.Align.CENTER
    }

    val fontMetrics = textPaint.fontMetrics
    val textY = (h / 2f) - (fontMetrics.ascent + fontMetrics.descent) / 2f
    canvas.drawText(labelText, w / 2f, textY, textPaint)

    return bitmap
}

private fun drawPillBaseBitmap(
    context: Context,
    target: AiTarget,
    labelText: String,
    accentColorInt: Int,
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

    val contentColor = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val pillBgColor = if (isLight) {
        Color.parseColor("#0F000000")
    } else {
        Color.argb(30, Color.red(accentColorInt), Color.green(accentColorInt), Color.blue(accentColorInt))
    }

    val rect = RectF(0f, 0f, w, h)
    val capsuleRadius = rect.height() / 2f

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pillBgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, capsuleRadius, capsuleRadius, bgPaint)

    var logoSize = h * logoSizePercent
    var textSize = h * textSizePercent
    var itemGap = h * 0.10f

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = contentColor
        this.textSize = textSize
        typeface = getSlateFont(context, weight = 600)
    }

    var measuredTextWidth = textPaint.measureText(labelText)
    var totalContentWidth = logoSize + itemGap + measuredTextWidth

    // Auto-scale content down if squeezed into a narrow container
    val maxAvailW = w * 0.82f
    if (totalContentWidth > maxAvailW && maxAvailW > 0f) {
        val scale = maxAvailW / totalContentWidth
        logoSize *= scale
        textSize *= scale
        itemGap *= scale
        textPaint.textSize = textSize
        measuredTextWidth = textPaint.measureText(labelText)
        totalContentWidth = logoSize + itemGap + measuredTextWidth
    }

    val logoXStart = if (isCenteredLayout) (w - totalContentWidth) / 2f else capsuleRadius * 0.55f

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

// 11. AI DOCK BAR (4x1)
fun generateAiBarDock5Bitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val scaleFactor = maxOf(density, 3.5f)
    val w = (wDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())
    val h = (hDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 4.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h.toFloat() - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w.toFloat() - (margin * 2f)) {
            cardW = w.toFloat() - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardW = cardRect.width()
    val cardH = cardRect.height()
    val aspectRatio = cardW / cardH

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }

    val tileBgColor = if (isLight) Color.parseColor("#0A000000") else Color.parseColor("#14FFFFFF")
    val iconTint = accentColorInt

    if (aspectRatio >= 2.0f) {
        // ================================================================
        // 1. HORIZONTAL CAPSULE BAR MODE
        // ================================================================
        val cornerRadius = cardH / 2f
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

        val pad = cardH * 0.12f
        val baseInnerH = cardH - (pad * 2f)

        val list = listOf(
            AiTarget.GEMINI_TEXT,
            AiTarget.CHATGPT_TEXT,
            AiTarget.CLAUDE,
            AiTarget.GROK,
            AiTarget.PERPLEXITY
        )

        val availContentW = cardW - (pad * 2f)
        val gapRatio = 0.08f
        val minGap = baseInnerH * gapRatio
        val requiredW = 5 * baseInnerH + 4 * minGap

        val innerH = if (requiredW > availContentW) {
            (availContentW / (5f + 4 * gapRatio)).coerceAtLeast(scaleFactor * 12f)
        } else {
            baseInnerH
        }

        val gap = innerH * gapRatio
        val tileW = if (requiredW > availContentW) {
            innerH
        } else {
            (availContentW - 4 * gap) / 5f
        }

        val innerTopY = cardRect.centerY() - (innerH / 2f)
        val totalGroupW = 5 * tileW + 4 * gap
        val startX = cardRect.left + (cardW - totalGroupW) / 2f

        list.forEachIndexed { index, target ->
            val tileShape = when (index) {
                0 -> AiShapeStyle.CAPSULE_LEFT
                list.size - 1 -> AiShapeStyle.CAPSULE_RIGHT
                else -> AiShapeStyle.SQUIRCLE
            }
            val tile = generateTileBitmap(
                context = context,
                target = target,
                bgColorInt = tileBgColor,
                accentColorInt = iconTint,
                isLight = isLight,
                shapeStyle = tileShape,
                widthPx = tileW.toInt(),
                heightPx = innerH.toInt()
            )
            val xPos = startX + index * (tileW + gap)
            canvas.drawBitmap(tile, xPos, innerTopY, null)
        }
    } else {
        // ================================================================
        // 2. TALL / SQUARE RESPONSIVE BENTO MODE
        // ================================================================
        val cornerRadius = getStandardCornerRadius(scaleFactor)
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

        val pad = minOf(cardW, cardH) * 0.06f
        val gap = minOf(cardW, cardH) * 0.035f

        val availW = cardW - (pad * 2f)
        val availH = cardH - (pad * 2f)

        val topH = ((availH - gap) * 0.46f).toInt().coerceAtLeast(1)
        val botH = (availH - gap - topH).toInt().coerceAtLeast(1)

        val startX = cardRect.left + pad
        val startY = cardRect.top + pad

        // Row 1: Gemini & ChatGPT (Concentric outer corner shapes)
        val topTileW = ((availW - gap) / 2f).toInt().coerceAtLeast(1)
        val topTargets = listOf(
            Pair(AiTarget.GEMINI_TEXT, AiShapeStyle.CORNER_TOP_LEFT),
            Pair(AiTarget.CHATGPT_TEXT, AiShapeStyle.CORNER_TOP_RIGHT)
        )
        topTargets.forEachIndexed { i, (target, shape) ->
            val tile = generateTileBitmap(
                context = context,
                target = target,
                bgColorInt = tileBgColor,
                accentColorInt = iconTint,
                isLight = isLight,
                shapeStyle = shape,
                widthPx = topTileW,
                heightPx = topH
            )
            val bx = startX + i * (topTileW + gap)
            canvas.drawBitmap(tile, bx, startY, null)
        }

        // Row 2: Claude, Grok, Perplexity (Concentric outer corner shapes)
        val botTileW = ((availW - (gap * 2f)) / 3f).toInt().coerceAtLeast(1)
        val botY = startY + topH + gap
        val botTargets = listOf(
            Pair(AiTarget.CLAUDE, AiShapeStyle.CORNER_BOTTOM_LEFT),
            Pair(AiTarget.GROK, AiShapeStyle.SQUIRCLE),
            Pair(AiTarget.PERPLEXITY, AiShapeStyle.CORNER_BOTTOM_RIGHT)
        )
        botTargets.forEachIndexed { i, (target, shape) ->
            val tile = generateTileBitmap(
                context = context,
                target = target,
                bgColorInt = tileBgColor,
                accentColorInt = iconTint,
                isLight = isLight,
                shapeStyle = shape,
                widthPx = botTileW,
                heightPx = botH
            )
            val bx = startX + i * (botTileW + gap)
            canvas.drawBitmap(tile, bx, botY, null)
        }
    }

    return bitmap
}

// 12. AI CAPSULE BAR (4x1)
fun generateAiBarCapsuleBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val scaleFactor = maxOf(density, 3.5f)
    val w = (wDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())
    val h = (hDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 4.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h.toFloat() - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w.toFloat() - (margin * 2f)) {
            cardW = w.toFloat() - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardW = cardRect.width()
    val cardH = cardRect.height()
    val aspectRatio = cardW / cardH

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }

    val tileBgColor = if (isLight) Color.parseColor("#0A000000") else Color.parseColor("#14FFFFFF")

    if (aspectRatio >= 2.0f) {
        // ================================================================
        // 1. HORIZONTAL CAPSULE BAR MODE
        // ================================================================
        val cornerRadius = cardH / 2f
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

        val pad = cardH * 0.12f
        val baseInnerH = cardH - (pad * 2f)

        val items = listOf(
            Triple(AiTarget.CHATGPT_VOICE, AiShapeStyle.CAPSULE_LEFT, true),
            Triple(AiTarget.PERPLEXITY, AiShapeStyle.SQUIRCLE, false),
            Triple(AiTarget.CLAUDE, AiShapeStyle.SQUIRCLE, false),
            Triple(AiTarget.GEMINI_TEXT, AiShapeStyle.CAPSULE_RIGHT, true)
        )

        val availContentW = cardW - (pad * 2f)
        val gapRatio = 0.08f
        val minGap = baseInnerH * gapRatio
        val requiredW = 4 * baseInnerH + 3 * minGap

        val innerH = if (requiredW > availContentW) {
            (availContentW / (4f + 3 * gapRatio)).coerceAtLeast(scaleFactor * 12f)
        } else {
            baseInnerH
        }

        val gap = innerH * gapRatio
        val tileW = if (requiredW > availContentW) {
            innerH
        } else {
            (availContentW - 3 * gap) / 4f
        }

        val innerTopY = cardRect.centerY() - (innerH / 2f)
        val totalGroupW = 4 * tileW + 3 * gap
        val startX = cardRect.left + (cardW - totalGroupW) / 2f

        items.forEachIndexed { index, (target, shape, isAccent) ->
            val tile = generateTileBitmap(
                context = context,
                target = target,
                bgColorInt = tileBgColor,
                accentColorInt = accentColorInt,
                isLight = isLight,
                shapeStyle = shape,
                isPrimaryAccent = isAccent,
                widthPx = tileW.toInt(),
                heightPx = innerH.toInt()
            )
            val xPos = startX + index * (tileW + gap)
            canvas.drawBitmap(tile, xPos, innerTopY, null)
        }
    } else {
        // ================================================================
        // 2. TALL / SQUARE RESPONSIVE BENTO MODE
        // ================================================================
        val cornerRadius = getStandardCornerRadius(scaleFactor)
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

        val pad = minOf(cardW, cardH) * 0.06f
        val gap = minOf(cardW, cardH) * 0.035f

        val availW = cardW - (pad * 2f)
        val availH = cardH - (pad * 2f)

        val topH = ((availH - gap) * 0.50f).toInt().coerceAtLeast(1)
        val botH = (availH - gap - topH).toInt().coerceAtLeast(1)

        val startX = cardRect.left + pad
        val startY = cardRect.top + pad

        // Row 1: Primary Accents (ChatGPT Voice & Gemini) - Concentric outer corner shapes
        val tileW = ((availW - gap) / 2f).toInt().coerceAtLeast(1)
        val topItems = listOf(
            Triple(AiTarget.CHATGPT_VOICE, AiShapeStyle.CORNER_TOP_LEFT, true),
            Triple(AiTarget.GEMINI_TEXT, AiShapeStyle.CORNER_TOP_RIGHT, true)
        )
        topItems.forEachIndexed { i, (target, shape, isAccent) ->
            val tile = generateTileBitmap(
                context = context,
                target = target,
                bgColorInt = tileBgColor,
                accentColorInt = accentColorInt,
                isLight = isLight,
                shapeStyle = shape,
                isPrimaryAccent = isAccent,
                widthPx = tileW,
                heightPx = topH
            )
            val bx = startX + i * (tileW + gap)
            canvas.drawBitmap(tile, bx, startY, null)
        }

        // Row 2: Secondary Tiles (Perplexity & Claude) - Concentric outer corner shapes
        val botY = startY + topH + gap
        val botItems = listOf(
            Triple(AiTarget.PERPLEXITY, AiShapeStyle.CORNER_BOTTOM_LEFT, false),
            Triple(AiTarget.CLAUDE, AiShapeStyle.CORNER_BOTTOM_RIGHT, false)
        )
        botItems.forEachIndexed { i, (target, shape, isAccent) ->
            val tile = generateTileBitmap(
                context = context,
                target = target,
                bgColorInt = tileBgColor,
                accentColorInt = accentColorInt,
                isLight = isLight,
                shapeStyle = shape,
                isPrimaryAccent = isAccent,
                widthPx = tileW,
                heightPx = botH
            )
            val bx = startX + i * (tileW + gap)
            canvas.drawBitmap(tile, bx, botY, null)
        }
    }

    return bitmap
}

// 13. AI DUAL FLAGSHIP BAR (4x1)
fun generateAiBarDualFlagshipBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val scaleFactor = maxOf(density, 3.5f)
    val w = (wDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())
    val h = (hDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 4.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h.toFloat() - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w.toFloat() - (margin * 2f)) {
            cardW = w.toFloat() - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardW = cardRect.width()
    val cardH = cardRect.height()
    val aspectRatio = cardW / cardH

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }

    if (aspectRatio >= 2.0f) {
        // ================================================================
        // 1. HORIZONTAL CAPSULE BAR MODE
        // ================================================================
        val cornerRadius = cardH / 2f
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

        val pad = cardH * 0.12f
        val innerH = cardH - (pad * 2f)
        val innerTopY = cardRect.centerY() - (innerH / 2f)

        val availContentW = cardW - (pad * 2f)
        val gap = innerH * 0.10f
        val pillW = ((availContentW - gap) / 2f).toInt().coerceAtLeast(1)

        val startX = cardRect.left + pad

        // GPT Pill
        val leftPill = drawPillBaseBitmap(
            context = context,
            target = AiTarget.CHATGPT_TEXT,
            labelText = "GPT",
            accentColorInt = accentColorInt,
            isLight = isLight,
            widthPx = pillW,
            heightPx = innerH.toInt(),
            isCenteredLayout = true
        )
        canvas.drawBitmap(leftPill, startX, innerTopY, null)

        // Gemini Pill
        val rightPill = drawPillBaseBitmap(
            context = context,
            target = AiTarget.GEMINI_TEXT,
            labelText = "Gemini",
            accentColorInt = accentColorInt,
            isLight = isLight,
            widthPx = pillW,
            heightPx = innerH.toInt(),
            isCenteredLayout = true
        )
        canvas.drawBitmap(rightPill, startX + pillW + gap, innerTopY, null)
    } else {
        // ================================================================
        // 2. TALL / SQUARE RESPONSIVE BENTO MODE
        // ================================================================
        val cornerRadius = getStandardCornerRadius(scaleFactor)
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

        val pad = minOf(cardW, cardH) * 0.06f
        val gap = minOf(cardW, cardH) * 0.035f

        val availW = cardW - (pad * 2f)
        val availH = cardH - (pad * 2f)

        val pillH = ((availH - gap) / 2f).toInt().coerceAtLeast(1)

        val startX = cardRect.left + pad
        val startY = cardRect.top + pad

        val topPill = drawPillBaseBitmap(
            context = context,
            target = AiTarget.CHATGPT_TEXT,
            labelText = "GPT",
            accentColorInt = accentColorInt,
            isLight = isLight,
            widthPx = availW.toInt(),
            heightPx = pillH,
            isCenteredLayout = true
        )
        canvas.drawBitmap(topPill, startX, startY, null)

        val botPill = drawPillBaseBitmap(
            context = context,
            target = AiTarget.GEMINI_TEXT,
            labelText = "Gemini",
            accentColorInt = accentColorInt,
            isLight = isLight,
            widthPx = availW.toInt(),
            heightPx = pillH,
            isCenteredLayout = true
        )
        canvas.drawBitmap(botPill, startX, startY + pillH + gap, null)
    }

    return bitmap
}

// 14. AI QUAD FOLDER (4x2 / Responsive)
fun generateAiFolder4ClassicBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val scaleFactor = maxOf(density, 3.5f)
    val w = (wDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())
    val h = (hDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val size = minOf(w, h).toFloat() - (margin * 2f)
        val leftX = (w - size) / 2f
        val topY = (h - size) / 2f
        RectF(leftX, topY, leftX + size, topY + size)
    }

    val cardW = cardRect.width()
    val cardH = cardRect.height()

    val cornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    // Dynamic padding and gaps relative to container dimensions
    val padX = cardW * 0.08f
    val padY = cardH * 0.08f
    val gapX = cardW * 0.04f
    val gapY = cardH * 0.04f

    val availW = cardW - (padX * 2f)
    val availH = cardH - (padY * 2f)

    // Expand tile width and height independently so inner tiles fill 100% of resized bounds
    val tileW = ((availW - gapX) / 2f).toInt().coerceAtLeast(1)
    val tileH = ((availH - gapY) / 2f).toInt().coerceAtLeast(1)

    val startX = cardRect.left + padX
    val startY = cardRect.top + padY

    val tileBgColor = if (isLight) Color.parseColor("#0A000000") else Color.parseColor("#14FFFFFF")

    val grid = listOf(
        listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT),
        listOf(AiTarget.PERPLEXITY, AiTarget.CLAUDE)
    )

    grid.forEachIndexed { r, row ->
        row.forEachIndexed { c, target ->
            val tile = generateTileBitmap(
                context = context,
                target = target,
                bgColorInt = tileBgColor,
                accentColorInt = accentColorInt,
                isLight = isLight,
                shapeStyle = AiShapeStyle.SQUIRCLE,
                widthPx = tileW,
                heightPx = tileH
            )
            val xPos = startX + c * (tileW + gapX)
            val yPos = startY + r * (tileH + gapY)
            canvas.drawBitmap(tile, xPos, yPos, null)
        }
    }

    return bitmap
}

// 15. AI BENTO FOLDER (4x2 / Fixed 2:1 Hero Bento)
fun generateAiFolder6BentoHeroBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val scaleFactor = maxOf(density, 3.5f)
    val w = (wDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())
    val h = (hDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 2.0f

    // Always enforce fixed 2:1 aspect ratio container to prevent vertical clipping
    var cardH = h.toFloat() - (margin * 2f)
    var cardW = cardH * targetRatio
    if (cardW > w.toFloat() - (margin * 2f)) {
        cardW = w.toFloat() - (margin * 2f)
        cardH = cardW / targetRatio
    }
    val leftX = (w - cardW) / 2f
    val topY = (h - cardH) / 2f
    val cardRect = RectF(leftX, topY, leftX + cardW, topY + cardH)

    val cornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    // Inner padding and gap calculations
    val pad = minOf(cardW, cardH) * 0.08f
    val gap = minOf(cardW, cardH) * 0.04f

    val availW = cardW - (pad * 2f)
    val availH = cardH - (pad * 2f)

    // Vertical distribution: 60% height for top hero row, 40% for bottom row
    val topH = ((availH - gap) * 0.60f).toInt().coerceAtLeast(1)
    val botH = (availH - gap - topH).toInt().coerceAtLeast(1)

    // Horizontal distribution: Top row has 2 hero tiles, Bottom row has 4 small tiles
    val topTileW = ((availW - gap) / 2f).toInt().coerceAtLeast(1)
    val botTileW = ((availW - (gap * 3f)) / 4f).toInt().coerceAtLeast(1)

    val tileBgColor = if (isLight) Color.parseColor("#0A000000") else Color.parseColor("#14FFFFFF")

    val startX = cardRect.left + pad
    val startY = cardRect.top + pad

    // Row 1: Top 2 Hero Tiles (Gemini & ChatGPT) - Concentric Outer Corner Shapes
    val topTargets = listOf(
        Pair(AiTarget.GEMINI_TEXT, AiShapeStyle.CORNER_TOP_LEFT),
        Pair(AiTarget.CHATGPT_TEXT, AiShapeStyle.CORNER_TOP_RIGHT)
    )

    topTargets.forEachIndexed { i, (target, shape) ->
        val tile = generateTileBitmap(
            context = context,
            target = target,
            bgColorInt = tileBgColor,
            accentColorInt = accentColorInt,
            isLight = isLight,
            shapeStyle = shape,
            widthPx = topTileW,
            heightPx = topH
        )
        val tx = startX + i * (topTileW + gap)
        canvas.drawBitmap(tile, tx, startY, null)
    }

    // Row 2: Bottom 4 Small Tiles (Claude, Grok, DeepSeek, Meta AI) - Concentric Outer Corner Shapes
    val botY = startY + topH + gap
    val bottomItems = listOf(
        Pair(AiTarget.CLAUDE, AiShapeStyle.CORNER_BOTTOM_LEFT),
        Pair(AiTarget.GROK, AiShapeStyle.SQUIRCLE),
        Pair(AiTarget.DEEPSEEK, AiShapeStyle.SQUIRCLE),
        Pair(AiTarget.META_AI, AiShapeStyle.CORNER_BOTTOM_RIGHT)
    )

    bottomItems.forEachIndexed { i, (target, shape) ->
        val tile = generateTileBitmap(
            context = context,
            target = target,
            bgColorInt = tileBgColor,
            accentColorInt = accentColorInt,
            isLight = isLight,
            shapeStyle = shape,
            widthPx = botTileW,
            heightPx = botH
        )
        val bx = startX + i * (botTileW + gap)
        canvas.drawBitmap(tile, bx, botY, null)
    }

    return bitmap
}

// 16. AI SIDE BENTO FOLDER (4x2 / Fixed 2:1 Hero Bento)
fun generateAiFolder8BentoSideBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val scaleFactor = maxOf(density, 3.5f)
    val w = (wDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())
    val h = (hDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 2.0f

    // Enforce fixed 2:1 aspect ratio container to eliminate empty black space
    var cardH = h.toFloat() - (margin * 2f)
    var cardW = cardH * targetRatio
    if (cardW > w.toFloat() - (margin * 2f)) {
        cardW = w.toFloat() - (margin * 2f)
        cardH = cardW / targetRatio
    }
    val leftX = (w - cardW) / 2f
    val topY = (h - cardH) / 2f
    val cardRect = RectF(leftX, topY, leftX + cardW, topY + cardH)

    val cornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    // Reduced gap and inner padding calculations for a tight, balanced bento
    val pad = minOf(cardW, cardH) * 0.08f
    val gap = minOf(cardW, cardH) * 0.03f

    val availW = cardW - (pad * 2f)
    val availH = cardH - (pad * 2f)

    // Left side: 2 stacked hero tiles
    val leftTileH = ((availH - gap) / 2f).toInt().coerceAtLeast(1)

    // Right side: 3 rows x 2 columns of small tiles
    val rightTileH = ((availH - (gap * 2f)) / 3f).toInt().coerceAtLeast(1)
    val rightColW = ((availW * 0.48f - gap) / 2f).toInt().coerceAtLeast(1)

    // Fill 100% of horizontal space dynamically between left and right sections
    val rightGridW = (rightColW * 2) + gap.toInt()
    val leftTileW = (availW - gap - rightGridW).toInt().coerceAtLeast(1)

    val tileBgColor = if (isLight) Color.parseColor("#0A000000") else Color.parseColor("#14FFFFFF")

    val startX = cardRect.left + pad
    val startY = cardRect.top + pad

    // Left Hero Section (Gemini & ChatGPT) - Concentric Outer Corners
    val left1 = generateTileBitmap(
        context = context,
        target = AiTarget.GEMINI_TEXT,
        bgColorInt = tileBgColor,
        accentColorInt = accentColorInt,
        isLight = isLight,
        shapeStyle = AiShapeStyle.CORNER_TOP_LEFT,
        widthPx = leftTileW,
        heightPx = leftTileH
    )
    canvas.drawBitmap(left1, startX, startY, null)

    val left2Y = startY + leftTileH + gap
    val left2 = generateTileBitmap(
        context = context,
        target = AiTarget.CHATGPT_TEXT,
        bgColorInt = tileBgColor,
        accentColorInt = accentColorInt,
        isLight = isLight,
        shapeStyle = AiShapeStyle.CORNER_BOTTOM_LEFT,
        widthPx = leftTileW,
        heightPx = leftTileH
    )
    canvas.drawBitmap(left2, startX, left2Y, null)

    // Right Grid Section (3 rows x 2 columns) - Concentric Outer Corners
    val rightGrid = listOf(
        listOf(Pair(AiTarget.CLAUDE, AiShapeStyle.SQUIRCLE), Pair(AiTarget.GROK, AiShapeStyle.CORNER_TOP_RIGHT)),
        listOf(Pair(AiTarget.PERPLEXITY, AiShapeStyle.SQUIRCLE), Pair(AiTarget.COPILOT, AiShapeStyle.SQUIRCLE)),
        listOf(Pair(AiTarget.DEEPSEEK, AiShapeStyle.SQUIRCLE), Pair(AiTarget.META_AI, AiShapeStyle.CORNER_BOTTOM_RIGHT))
    )

    val rightStartX = startX + leftTileW + gap
    rightGrid.forEachIndexed { r, row ->
        row.forEachIndexed { c, (target, shape) ->
            val tile = generateTileBitmap(
                context = context,
                target = target,
                bgColorInt = tileBgColor,
                accentColorInt = accentColorInt,
                isLight = isLight,
                shapeStyle = shape,
                widthPx = rightColW,
                heightPx = rightTileH
            )
            val rx = rightStartX + c * (rightColW + gap)
            val ry = startY + r * (rightTileH + gap)
            canvas.drawBitmap(tile, rx, ry, null)
        }
    }

    return bitmap
}

// 17. AI 3x3 GRID FOLDER (2x2 / Fixed)
fun generateAiFolder9GridBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val scaleFactor = maxOf(density, 3.5f)
    val w = (wDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())
    val h = (hDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val size = minOf(w, h).toFloat() - (margin * 2f)
        val leftX = (w - size) / 2f
        val topY = (h - size) / 2f
        RectF(leftX, topY, leftX + size, topY + size)
    }

    val cornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val pad = cardRect.width() * 0.07f
    val gap = cardRect.width() * 0.035f
    val tileSize = ((cardRect.width() - (pad * 2f) - (gap * 2f)) / 3f).toInt().coerceAtLeast(1)
    val tileBgColor = if (isLight) Color.parseColor("#0A000000") else Color.parseColor("#14FFFFFF")

    val grid = listOf(
        listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.COPILOT),
        listOf(AiTarget.GROK, AiTarget.CLAUDE, AiTarget.DEEPSEEK),
        listOf(AiTarget.PERPLEXITY, AiTarget.META_AI, AiTarget.POE)
    )

    grid.forEachIndexed { r, row ->
        row.forEachIndexed { c, target ->
            val tile = generateTileBitmap(context, target, tileBgColor, accentColorInt, isLight, AiShapeStyle.SQUIRCLE, widthPx = tileSize, heightPx = tileSize)
            val xPos = cardRect.left + pad + c * (tileSize + gap)
            val yPos = cardRect.top + pad + r * (tileSize + gap)
            canvas.drawBitmap(tile, xPos, yPos, null)
        }
    }

    return bitmap
}

// 18. AI MEGA FOLDER (4x2 / Fixed 2:1)
fun generateAiFolder10MegaBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val scaleFactor = maxOf(density, 3.5f)
    val w = (wDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())
    val h = (hDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 2.0f

    var cardH = h.toFloat() - (margin * 2f)
    var cardW = cardH * targetRatio
    if (cardW > w.toFloat() - (margin * 2f)) {
        cardW = w.toFloat() - (margin * 2f)
        cardH = cardW / targetRatio
    }
    val leftX = (w - cardW) / 2f
    val topY = (h - cardH) / 2f
    val cardRect = RectF(leftX, topY, leftX + cardW, topY + cardH)

    val cornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    // Lock padding and gap relative to cardH so top/bottom/left/right paddings and gaps are identical
    val pad = cardH * 0.05f
    val gap = cardH * 0.02f

    val availW = cardW - (pad * 2f)
    val availH = cardH - (pad * 2f)

    // Expand tile dimensions to fill 100% of the remaining container
    val tileW = ((availW - (gap * 4f)) / 5f).toInt().coerceAtLeast(1)
    val tileH = ((availH - gap) / 2f).toInt().coerceAtLeast(1)

    val startX = cardRect.left + pad
    val startY = cardRect.top + pad

    val tileBgColor = if (isLight) Color.parseColor("#0A000000") else Color.parseColor("#14FFFFFF")

    val row1 = listOf(
        Pair(AiTarget.GEMINI_TEXT, AiShapeStyle.CORNER_TOP_LEFT),
        Pair(AiTarget.CHATGPT_TEXT, AiShapeStyle.SQUIRCLE),
        Pair(AiTarget.COPILOT, AiShapeStyle.SQUIRCLE),
        Pair(AiTarget.CLAUDE, AiShapeStyle.SQUIRCLE),
        Pair(AiTarget.GROK, AiShapeStyle.CORNER_TOP_RIGHT)
    )
    val row2 = listOf(
        Pair(AiTarget.PERPLEXITY, AiShapeStyle.CORNER_BOTTOM_LEFT),
        Pair(AiTarget.DEEPSEEK, AiShapeStyle.SQUIRCLE),
        Pair(AiTarget.META_AI, AiShapeStyle.SQUIRCLE),
        Pair(AiTarget.POE, AiShapeStyle.SQUIRCLE),
        Pair(AiTarget.PI, AiShapeStyle.CORNER_BOTTOM_RIGHT)
    )

    row1.forEachIndexed { i, (target, shape) ->
        val tile = generateTileBitmap(
            context = context,
            target = target,
            bgColorInt = tileBgColor,
            accentColorInt = accentColorInt,
            isLight = isLight,
            shapeStyle = shape,
            widthPx = tileW,
            heightPx = tileH
        )
        val x = startX + i * (tileW + gap)
        canvas.drawBitmap(tile, x, startY, null)
    }

    val row2Y = startY + tileH + gap
    row2.forEachIndexed { i, (target, shape) ->
        val tile = generateTileBitmap(
            context = context,
            target = target,
            bgColorInt = tileBgColor,
            accentColorInt = accentColorInt,
            isLight = isLight,
            shapeStyle = shape,
            widthPx = tileW,
            heightPx = tileH
        )
        val x = startX + i * (tileW + gap)
        canvas.drawBitmap(tile, x, row2Y, null)
    }

    return bitmap
}

// 19. AI ASYMMETRIC BENTO (3x2 / Fixed 1.5:1)
fun generateAiFolder7AsymmetricBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val scaleFactor = maxOf(density, 3.5f)
    val w = (wDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())
    val h = (hDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 1.5f

    // Enforce fixed 1.5:1 aspect ratio container to eliminate empty black space
    var cardH = h.toFloat() - (margin * 2f)
    var cardW = cardH * targetRatio
    if (cardW > w.toFloat() - (margin * 2f)) {
        cardW = w.toFloat() - (margin * 2f)
        cardH = cardW / targetRatio
    }
    val leftX = (w - cardW) / 2f
    val topY = (h - cardH) / 2f
    val cardRect = RectF(leftX, topY, leftX + cardW, topY + cardH)

    val cornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    // Inner padding and gap calculations
    val pad = minOf(cardW, cardH) * 0.08f
    val gap = minOf(cardW, cardH) * 0.035f

    val availW = cardW - (pad * 2f)
    val availH = cardH - (pad * 2f)

    // Vertical distribution: 64% height for top bento area, 36% for bottom row
    val topH = ((availH - gap) * 0.64f).toInt().coerceAtLeast(1)
    val botH = (availH - gap - topH).toInt().coerceAtLeast(1)

    // Horizontal distribution for top section: Hero tile + 1 column of 2 stacked small tiles
    val rightColW = (availW * 0.34f).toInt().coerceAtLeast(1)
    val heroW = (availW - gap - rightColW).toInt().coerceAtLeast(1)
    val rightTileH = ((topH - gap) / 2f).toInt().coerceAtLeast(1)

    // Bottom section: 4 small tiles spanning 100% of horizontal space
    val botTileW = ((availW - (gap * 3f)) / 4f).toInt().coerceAtLeast(1)

    val tileBgColor = if (isLight) Color.parseColor("#0A000000") else Color.parseColor("#14FFFFFF")

    val startX = cardRect.left + pad
    val startY = cardRect.top + pad

    // 1. Large Hero Tile (ChatGPT) - Top-Left Concentric Corner
    val bigTile = generateTileBitmap(
        context = context,
        target = AiTarget.CHATGPT_TEXT,
        bgColorInt = tileBgColor,
        accentColorInt = accentColorInt,
        isLight = isLight,
        shapeStyle = AiShapeStyle.CORNER_TOP_LEFT,
        isPrimaryAccent = true,
        widthPx = heroW,
        heightPx = topH
    )
    canvas.drawBitmap(bigTile, startX, startY, null)

    // 2. Right Stacked Tiles (Grok & Copilot)
    val rightX = startX + heroW + gap
    val topRightTile = generateTileBitmap(
        context = context,
        target = AiTarget.GROK,
        bgColorInt = tileBgColor,
        accentColorInt = accentColorInt,
        isLight = isLight,
        shapeStyle = AiShapeStyle.CORNER_TOP_RIGHT,
        widthPx = rightColW,
        heightPx = rightTileH
    )
    canvas.drawBitmap(topRightTile, rightX, startY, null)

    val midRightTile = generateTileBitmap(
        context = context,
        target = AiTarget.COPILOT,
        bgColorInt = tileBgColor,
        accentColorInt = accentColorInt,
        isLight = isLight,
        shapeStyle = AiShapeStyle.SQUIRCLE,
        widthPx = rightColW,
        heightPx = rightTileH
    )
    canvas.drawBitmap(midRightTile, rightX, startY + rightTileH + gap, null)

    // 3. Bottom Row (Gemini, Claude, Perplexity, Meta AI)
    val botY = startY + topH + gap
    val bottomItems = listOf(
        Pair(AiTarget.GEMINI_TEXT, AiShapeStyle.CORNER_BOTTOM_LEFT),
        Pair(AiTarget.CLAUDE, AiShapeStyle.SQUIRCLE),
        Pair(AiTarget.PERPLEXITY, AiShapeStyle.SQUIRCLE),
        Pair(AiTarget.META_AI, AiShapeStyle.CORNER_BOTTOM_RIGHT)
    )

    bottomItems.forEachIndexed { i, (target, shape) ->
        val tile = generateTileBitmap(
            context = context,
            target = target,
            bgColorInt = tileBgColor,
            accentColorInt = accentColorInt,
            isLight = isLight,
            shapeStyle = shape,
            widthPx = botTileW,
            heightPx = botH
        )
        val bx = startX + i * (botTileW + gap)
        canvas.drawBitmap(tile, bx, botY, null)
    }

    return bitmap
}