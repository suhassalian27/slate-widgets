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
import com.altusix.slate.utils.createSupersampledCanvas
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

        // Rule 4: Outer container corner radius uses scaleFactor
        val outerRadius = getStandardCornerRadius(scaleFactor)

        // Concentric inner tile radius: (outerRadius - pad).coerceAtLeast(scaleFactor * 6f)
        val concentricRadius = (outerRadius - margin)
            .coerceAtLeast(scaleFactor * 6f)
            .coerceAtLeast(0f)

        val squircleRadius = outerRadius
            .coerceAtMost(minDim * 0.26f)
            .coerceAtLeast(0f)

        val fullCapRadius = ((h - (margin * 2f)) / 2f).coerceAtLeast(0f)

        val squareRect = RectF(cx - halfTile, cy - halfTile, cx + halfTile, cy + halfTile)
        val fullRect = RectF(margin, margin, w - margin, h - margin)
        val targetRect = if (forceSquare) squareRect else fullRect

        when (shapeStyle) {
            AiShapeStyle.SQUIRCLE -> {
                canvas.drawRoundRect(targetRect, squircleRadius, squircleRadius, bgPaint)
            }
            AiShapeStyle.CIRCLE -> {
                if (forceSquare) {
                    val radius = ((minDim / 2f) - margin).coerceAtLeast(0f)
                    canvas.drawCircle(cx, cy, radius, bgPaint)
                } else {
                    canvas.drawRoundRect(fullRect, fullCapRadius, fullCapRadius, bgPaint)
                }
            }
            AiShapeStyle.HEXAGON -> {
                val hexPath = Path()
                val radius = ((minDim / 2f) - margin).coerceAtLeast(0f)
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
        val rawDrawable = ContextCompat.getDrawable(context, resId)
        if (rawDrawable != null) {
            val drawable = androidx.core.graphics.drawable.DrawableCompat.wrap(rawDrawable).mutate()
            val maxLogoSize = minDim * 0.52f
            val actualLogoSize = if (isPrimaryAccent) minDim * 0.58f else maxLogoSize

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
            androidx.core.graphics.drawable.DrawableCompat.setTint(drawable, logoColor)
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
    // Standard supersampled canvas destructuring
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)

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
        widthPx = canvas.width,
        heightPx = canvas.height
    )
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

    var textSize = h * 0.38f
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = contentColor
        this.textSize = textSize
        typeface = getSlateFont(context, weight = 600)
        textAlign = Paint.Align.CENTER
    }

    // Auto-fit guard to prevent text clipping in narrow pills
    val maxTextW = w - (capsuleRadius * 1.2f)
    val measuredW = textPaint.measureText(labelText)
    if (measuredW > maxTextW && maxTextW > 0f) {
        textPaint.textSize = textSize * (maxTextW / measuredW)
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

// 10. AI PRIMARY BAR (4x1)
fun generateAiBarHeroPrimaryBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val targetRatio = 4.0f
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
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
        val cornerRadius = cardH / 2f
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

        val pad = cardH * 0.08f
        val baseInnerH = cardH - (pad * 2f)
        val availW = cardW - (pad * 2f)

        // Mathematical width guard: Clamp innerH so total horizontal elements fit inside availW
        val maxInnerHFromWidth = availW / 5.1f
        val innerH = minOf(baseInnerH, maxInnerHFromWidth)

        val heroW = innerH * 1.6f
        val divSpacing = innerH * 0.12f
        val gap = innerH * 0.06f
        val rightTileW = innerH

        val totalGroupW = heroW + (divSpacing * 2f) + (rightTileW * 3f) + (gap * 2f)

        // Center group both horizontally and vertically inside cardRect
        val startX = cardRect.left + (cardW - totalGroupW) / 2f
        val innerTopY = cardRect.top + (cardH - innerH) / 2f

        // A. Gemini Hero Pill
        val heroBitmap = drawTextOnlyPillBitmap(
            context = context,
            labelText = "Gemini",
            accentColorInt = accentColorInt,
            isLight = isLight,
            widthPx = heroW.toInt(),
            heightPx = innerH.toInt()
        )
        canvas.drawBitmap(heroBitmap, startX, innerTopY, null)

        // B. Vertical Divider
        val divX = startX + heroW + divSpacing
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.parseColor("#26000000") else Color.parseColor("#26FFFFFF")
            strokeWidth = scaleFactor * 0.8f
        }
        canvas.drawLine(divX, cardRect.centerY() - (innerH * 0.28f), divX, cardRect.centerY() + (innerH * 0.28f), divPaint)

        // C. Right Icon Tiles
        val rightStartX = divX + divSpacing
        val targets = listOf(
            Triple(AiTarget.CHATGPT_TEXT, AiShapeStyle.SQUIRCLE, 0),
            Triple(AiTarget.CLAUDE, AiShapeStyle.SQUIRCLE, 1),
            Triple(AiTarget.GROK, AiShapeStyle.CAPSULE_RIGHT, 2)
        )

        for ((target, shape, index) in targets) {
            val tileX = rightStartX + index * (rightTileW + gap)
            val tile = generateTileBitmap(
                context = context,
                target = target,
                bgColorInt = tileBgColor,
                accentColorInt = iconTint,
                isLight = isLight,
                shapeStyle = shape,
                widthPx = rightTileW.toInt(),
                heightPx = innerH.toInt()
            )
            canvas.drawBitmap(tile, tileX, innerTopY, null)
        }
    } else {
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

        val heroBitmap = drawTextOnlyPillBitmap(
            context = context,
            labelText = "Gemini",
            accentColorInt = accentColorInt,
            isLight = isLight,
            widthPx = availW.toInt(),
            heightPx = topH
        )
        canvas.drawBitmap(heroBitmap, startX, startY, null)

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

// 11. AI DOCK BAR (4x1)
fun generateAiBarDock5Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val targetRatio = 4.0f
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
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
        val cornerRadius = cardH / 2f
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

        // Equal padding on all 4 sides
        val pad = cardH * 0.08f
        val innerH = cardH - (pad * 2f)
        val availW = cardW - (pad * 2f)

        val startX = cardRect.left + pad
        val innerTopY = cardRect.top + pad

        val list = listOf(
            AiTarget.GEMINI_TEXT,
            AiTarget.CHATGPT_TEXT,
            AiTarget.CLAUDE,
            AiTarget.GROK,
            AiTarget.PERPLEXITY
        )

        val gap = innerH * 0.06f
        val tileW = (availW - (gap * 4f)) / 5f

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
fun generateAiBarCapsuleBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val targetRatio = 4.0f
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
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
        val cornerRadius = cardH / 2f
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

        // Equal padding on all 4 sides
        val pad = cardH * 0.08f
        val innerH = cardH - (pad * 2f)
        val availW = cardW - (pad * 2f)

        val startX = cardRect.left + pad
        val innerTopY = cardRect.top + pad

        val items = listOf(
            Triple(AiTarget.CHATGPT_VOICE, AiShapeStyle.CAPSULE_LEFT, true),
            Triple(AiTarget.PERPLEXITY, AiShapeStyle.SQUIRCLE, false),
            Triple(AiTarget.CLAUDE, AiShapeStyle.SQUIRCLE, false),
            Triple(AiTarget.GEMINI_TEXT, AiShapeStyle.CAPSULE_RIGHT, true)
        )

        val gap = innerH * 0.06f
        val tileW = (availW - (gap * 3f)) / 4f

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
fun generateAiBarDualFlagshipBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val targetRatio = 4.0f
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
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
        val cornerRadius = cardH / 2f
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

        // Equal padding on all 4 sides
        val pad = cardH * 0.08f
        val innerH = cardH - (pad * 2f)
        val availW = cardW - (pad * 2f)

        val innerTopY = cardRect.top + pad
        val startX = cardRect.left + pad

        val gap = innerH * 0.08f
        val pillW = ((availW - gap) / 2f).toInt().coerceAtLeast(1)

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

// 14. AI QUAD FOLDER (2x2 Grid)
fun generateAiFolder4ClassicBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 1.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val maxCardRadius = minOf(cardRect.width(), cardRect.height()) / 2f
    val outerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(maxCardRadius)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    // Unified spacing and mathematical concentric inner boundary
    val minDim = minOf(cardRect.width(), cardRect.height())
    val spacing = (minDim * 0.032f).coerceIn(scaleFactor * 1.8f, scaleFactor * 5.5f)

    val innerCardRect = RectF(
        cardRect.left + spacing,
        cardRect.top + spacing,
        cardRect.right - spacing,
        cardRect.bottom - spacing
    )
    val innerCardRadius = maxOf(0f, outerRadius - spacing)
    val innerCardPath = Path().apply {
        addRoundRect(innerCardRect, innerCardRadius, innerCardRadius, Path.Direction.CW)
    }

    val tileW = (innerCardRect.width() - spacing) / 2f
    val tileH = (innerCardRect.height() - spacing) / 2f
    val innerCornerRadius = (minOf(tileW, tileH) * 0.20f)
        .coerceIn(scaleFactor * 2.0f, scaleFactor * 7.0f)
        .coerceAtMost(minOf(tileW, tileH) / 2f)

    val tileBgColor = if (isLight) Color.parseColor("#0A000000") else Color.parseColor("#14FFFFFF")

    val grid = listOf(
        listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT),
        listOf(AiTarget.PERPLEXITY, AiTarget.CLAUDE)
    )

    for (row in 0..1) {
        for (col in 0..1) {
            val target = grid[row][col]
            val tileLeft = innerCardRect.left + col * (tileW + spacing)
            val tileTop = innerCardRect.top + row * (tileH + spacing)
            val tileRect = RectF(tileLeft, tileTop, tileLeft + tileW, tileTop + tileH)

            val tl = if (row == 0 && col == 0) 0f else innerCornerRadius
            val tr = if (row == 0 && col == 1) 0f else innerCornerRadius
            val br = if (row == 1 && col == 1) 0f else innerCornerRadius
            val bl = if (row == 1 && col == 0) 0f else innerCornerRadius

            val radii = floatArrayOf(tl, tl, tr, tr, br, br, bl, bl)
            val tilePath = Path().apply { addRoundRect(tileRect, radii, Path.Direction.CW) }

            canvas.save()
            canvas.clipPath(innerCardPath)
            canvas.clipPath(tilePath)

            val tileBitmap = generateTileBitmap(
                context = context,
                target = target,
                bgColorInt = tileBgColor,
                accentColorInt = accentColorInt,
                isLight = isLight,
                shapeStyle = AiShapeStyle.FRAMELESS,
                widthPx = tileW.toInt(),
                heightPx = tileH.toInt()
            )
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tileBgColor
                style = Paint.Style.FILL
            }
            canvas.drawRect(tileRect, fillPaint)
            canvas.drawBitmap(tileBitmap, tileLeft, tileTop, null)
            canvas.restore()
        }
    }

    return bitmap
}

// 15. AI BENTO FOLDER (6 Apps - 2 Hero Top + 4 Small Bottom)
fun generateAiFolder6BentoHeroBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 2.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val maxCardRadius = minOf(cardRect.width(), cardRect.height()) / 2f
    val outerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(maxCardRadius)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val minDim = minOf(cardRect.width(), cardRect.height())
    val spacing = (minDim * 0.032f).coerceIn(scaleFactor * 1.8f, scaleFactor * 5.5f)

    val innerCardRect = RectF(
        cardRect.left + spacing,
        cardRect.top + spacing,
        cardRect.right - spacing,
        cardRect.bottom - spacing
    )
    val innerCardRadius = maxOf(0f, outerRadius - spacing)
    val innerCardPath = Path().apply {
        addRoundRect(innerCardRect, innerCardRadius, innerCardRadius, Path.Direction.CW)
    }

    val innerCornerRadius = (minDim * 0.08f)
        .coerceIn(scaleFactor * 2.0f, scaleFactor * 7.0f)
        .coerceAtMost(innerCardRadius)

    val tileBgColor = if (isLight) Color.parseColor("#0A000000") else Color.parseColor("#14FFFFFF")
    val tileFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tileBgColor
        style = Paint.Style.FILL
    }

    // Top 2 Hero Tiles
    val topH = ((innerCardRect.height() - spacing) * 0.58f).coerceAtLeast(1f)
    val topTileW = (innerCardRect.width() - spacing) / 2f
    val topTargets = listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT)

    for (i in 0..1) {
        val left = innerCardRect.left + i * (topTileW + spacing)
        val top = innerCardRect.top
        val rect = RectF(left, top, left + topTileW, top + topH)

        val tl = if (i == 0) 0f else innerCornerRadius
        val tr = if (i == 1) 0f else innerCornerRadius
        val radii = floatArrayOf(tl, tl, tr, tr, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius)
        val tilePath = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(innerCardPath)
        canvas.clipPath(tilePath)
        canvas.drawRect(rect, tileFillPaint)

        val tileBitmap = generateTileBitmap(
            context = context,
            target = topTargets[i],
            bgColorInt = tileBgColor,
            accentColorInt = accentColorInt,
            isLight = isLight,
            shapeStyle = AiShapeStyle.FRAMELESS,
            widthPx = topTileW.toInt(),
            heightPx = topH.toInt()
        )
        canvas.drawBitmap(tileBitmap, left, top, null)
        canvas.restore()
    }

    // Bottom 4 Small Tiles
    val botY = innerCardRect.top + topH + spacing
    val botH = innerCardRect.height() - topH - spacing
    val botTileW = (innerCardRect.width() - (spacing * 3f)) / 4f
    val bottomTargets = listOf(AiTarget.CLAUDE, AiTarget.GROK, AiTarget.DEEPSEEK, AiTarget.META_AI)

    for (i in 0..3) {
        val left = innerCardRect.left + i * (botTileW + spacing)
        val rect = RectF(left, botY, left + botTileW, botY + botH)

        val bl = if (i == 0) 0f else innerCornerRadius
        val br = if (i == 3) 0f else innerCornerRadius
        val radii = floatArrayOf(innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, br, br, bl, bl)
        val tilePath = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(innerCardPath)
        canvas.clipPath(tilePath)
        canvas.drawRect(rect, tileFillPaint)

        val tileBitmap = generateTileBitmap(
            context = context,
            target = bottomTargets[i],
            bgColorInt = tileBgColor,
            accentColorInt = accentColorInt,
            isLight = isLight,
            shapeStyle = AiShapeStyle.FRAMELESS,
            widthPx = botTileW.toInt(),
            heightPx = botH.toInt()
        )
        canvas.drawBitmap(tileBitmap, left, botY, null)
        canvas.restore()
    }

    return bitmap
}

// 16. AI SIDE BENTO FOLDER (8 Apps - 2 Stacked Left + 3x2 Right)
fun generateAiFolder8BentoSideBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 2.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val maxCardRadius = minOf(cardRect.width(), cardRect.height()) / 2f
    val outerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(maxCardRadius)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val minDim = minOf(cardRect.width(), cardRect.height())
    val spacing = (minDim * 0.032f).coerceIn(scaleFactor * 1.8f, scaleFactor * 5.5f)

    val innerCardRect = RectF(
        cardRect.left + spacing,
        cardRect.top + spacing,
        cardRect.right - spacing,
        cardRect.bottom - spacing
    )
    val innerCardRadius = maxOf(0f, outerRadius - spacing)
    val innerCardPath = Path().apply {
        addRoundRect(innerCardRect, innerCardRadius, innerCardRadius, Path.Direction.CW)
    }

    val innerCornerRadius = (minDim * 0.08f)
        .coerceIn(scaleFactor * 2.0f, scaleFactor * 7.0f)
        .coerceAtMost(innerCardRadius)

    val tileBgColor = if (isLight) Color.parseColor("#0A000000") else Color.parseColor("#14FFFFFF")
    val tileFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tileBgColor
        style = Paint.Style.FILL
    }

    // Left Hero Tiles (Gemini & ChatGPT)
    val leftTileW = (innerCardRect.width() - spacing) / 2f
    val leftTileH = (innerCardRect.height() - spacing) / 2f
    val leftTargets = listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT)

    for (i in 0..1) {
        val left = innerCardRect.left
        val top = innerCardRect.top + i * (leftTileH + spacing)
        val rect = RectF(left, top, left + leftTileW, top + leftTileH)

        val tl = if (i == 0) 0f else innerCornerRadius
        val bl = if (i == 1) 0f else innerCornerRadius
        val radii = floatArrayOf(tl, tl, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, bl, bl)
        val tilePath = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(innerCardPath)
        canvas.clipPath(tilePath)
        canvas.drawRect(rect, tileFillPaint)

        val tileBitmap = generateTileBitmap(
            context = context,
            target = leftTargets[i],
            bgColorInt = tileBgColor,
            accentColorInt = accentColorInt,
            isLight = isLight,
            shapeStyle = AiShapeStyle.FRAMELESS,
            widthPx = leftTileW.toInt(),
            heightPx = leftTileH.toInt()
        )
        canvas.drawBitmap(tileBitmap, left, top, null)
        canvas.restore()
    }

    // Right Grid Section (3 rows x 2 columns)
    val rightStartX = innerCardRect.left + leftTileW + spacing
    val rightW = innerCardRect.width() - leftTileW - spacing
    val rightColW = (rightW - spacing) / 2f
    val rightRowH = (innerCardRect.height() - (spacing * 2f)) / 3f

    val rightGrid = listOf(
        listOf(AiTarget.CLAUDE, AiTarget.GROK),
        listOf(AiTarget.PERPLEXITY, AiTarget.COPILOT),
        listOf(AiTarget.DEEPSEEK, AiTarget.META_AI)
    )

    for (r in 0..2) {
        for (c in 0..1) {
            val target = rightGrid[r][c]
            val left = rightStartX + c * (rightColW + spacing)
            val top = innerCardRect.top + r * (rightRowH + spacing)
            val rect = RectF(left, top, left + rightColW, top + rightRowH)

            val tr = if (r == 0 && c == 1) 0f else innerCornerRadius
            val br = if (r == 2 && c == 1) 0f else innerCornerRadius
            val radii = floatArrayOf(innerCornerRadius, innerCornerRadius, tr, tr, br, br, innerCornerRadius, innerCornerRadius)
            val tilePath = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }

            canvas.save()
            canvas.clipPath(innerCardPath)
            canvas.clipPath(tilePath)
            canvas.drawRect(rect, tileFillPaint)

            val tileBitmap = generateTileBitmap(
                context = context,
                target = target,
                bgColorInt = tileBgColor,
                accentColorInt = accentColorInt,
                isLight = isLight,
                shapeStyle = AiShapeStyle.FRAMELESS,
                widthPx = rightColW.toInt(),
                heightPx = rightRowH.toInt()
            )
            canvas.drawBitmap(tileBitmap, left, top, null)
            canvas.restore()
        }
    }

    return bitmap
}

// 17. AI 3x3 GRID FOLDER (3x3 Grid Hub)
fun generateAiFolder9GridBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 1.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val maxCardRadius = minOf(cardRect.width(), cardRect.height()) / 2f
    val outerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(maxCardRadius)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val minDim = minOf(cardRect.width(), cardRect.height())
    val spacing = (minDim * 0.032f).coerceIn(scaleFactor * 1.8f, scaleFactor * 5.5f)

    val innerCardRect = RectF(
        cardRect.left + spacing,
        cardRect.top + spacing,
        cardRect.right - spacing,
        cardRect.bottom - spacing
    )
    val innerCardRadius = maxOf(0f, outerRadius - spacing)
    val innerCardPath = Path().apply {
        addRoundRect(innerCardRect, innerCardRadius, innerCardRadius, Path.Direction.CW)
    }

    val tileW = (innerCardRect.width() - (spacing * 2f)) / 3f
    val tileH = (innerCardRect.height() - (spacing * 2f)) / 3f
    val innerCornerRadius = (minOf(tileW, tileH) * 0.20f)
        .coerceIn(scaleFactor * 2.0f, scaleFactor * 7.0f)
        .coerceAtMost(minOf(tileW, tileH) / 2f)

    val tileBgColor = if (isLight) Color.parseColor("#0A000000") else Color.parseColor("#14FFFFFF")
    val tileFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tileBgColor
        style = Paint.Style.FILL
    }

    val gridTargets = listOf(
        listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.COPILOT),
        listOf(AiTarget.GROK, AiTarget.CLAUDE, AiTarget.DEEPSEEK),
        listOf(AiTarget.PERPLEXITY, AiTarget.META_AI, AiTarget.POE)
    )

    for (row in 0..2) {
        for (col in 0..2) {
            val target = gridTargets[row][col]
            val tileLeft = innerCardRect.left + col * (tileW + spacing)
            val tileTop = innerCardRect.top + row * (tileH + spacing)
            val tileRect = RectF(tileLeft, tileTop, tileLeft + tileW, tileTop + tileH)

            val tl = if (row == 0 && col == 0) 0f else innerCornerRadius
            val tr = if (row == 0 && col == 2) 0f else innerCornerRadius
            val br = if (row == 2 && col == 2) 0f else innerCornerRadius
            val bl = if (row == 2 && col == 0) 0f else innerCornerRadius

            val radii = floatArrayOf(tl, tl, tr, tr, br, br, bl, bl)
            val tilePath = Path().apply { addRoundRect(tileRect, radii, Path.Direction.CW) }

            canvas.save()
            canvas.clipPath(innerCardPath)
            canvas.clipPath(tilePath)
            canvas.drawRect(tileRect, tileFillPaint)

            val iconSize = (minOf(tileW, tileH) * 0.52f).toInt()
            val iconCx = tileRect.centerX()
            val iconCy = tileRect.centerY()

            val resId = context.resources.getIdentifier(target.drawableResName, "drawable", context.packageName)
            if (resId != 0) {
                ContextCompat.getDrawable(context, resId)?.mutate()?.apply {
                    setTint(accentColorInt)
                    setBounds(
                        (iconCx - iconSize / 2f).toInt(),
                        (iconCy - iconSize / 2f).toInt(),
                        (iconCx + iconSize / 2f).toInt(),
                        (iconCy + iconSize / 2f).toInt()
                    )
                    draw(canvas)
                }
            }
            canvas.restore()
        }
    }

    return bitmap
}

// 18. AI MEGA FOLDER (10 Apps - 5x2 / 2x5 Smart Pivot)
fun generateAiFolder10MegaBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val isVertical = isResponsive && (hDp > wDp)
    val cols = if (isVertical) 2 else 5
    val rows = if (isVertical) 5 else 2

    val margin = scaleFactor * 1.5f
    val targetRatio = cols.toFloat() / rows.toFloat()

    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val maxCardRadius = minOf(cardRect.width(), cardRect.height()) / 2f
    val outerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(maxCardRadius)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val minDim = minOf(cardRect.width(), cardRect.height())
    val spacing = (minDim * 0.032f).coerceIn(scaleFactor * 1.8f, scaleFactor * 5.5f)

    val innerCardRect = RectF(
        cardRect.left + spacing,
        cardRect.top + spacing,
        cardRect.right - spacing,
        cardRect.bottom - spacing
    )
    val innerCardRadius = maxOf(0f, outerRadius - spacing)
    val innerCardPath = Path().apply {
        addRoundRect(innerCardRect, innerCardRadius, innerCardRadius, Path.Direction.CW)
    }

    val tileW = (innerCardRect.width() - (spacing * (cols - 1))) / cols
    val tileH = (innerCardRect.height() - (spacing * (rows - 1))) / rows
    val innerCornerRadius = (minOf(tileW, tileH) * 0.20f)
        .coerceIn(scaleFactor * 2.0f, scaleFactor * 7.0f)
        .coerceAtMost(minOf(tileW, tileH) / 2f)

    val tileBgColor = if (isLight) Color.parseColor("#0A000000") else Color.parseColor("#14FFFFFF")
    val tileFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tileBgColor
        style = Paint.Style.FILL
    }

    val targets = listOf(
        AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.COPILOT, AiTarget.CLAUDE, AiTarget.GROK,
        AiTarget.PERPLEXITY, AiTarget.DEEPSEEK, AiTarget.META_AI, AiTarget.POE, AiTarget.PI
    )

    for (i in 0 until 10) {
        val col = i % cols
        val row = i / cols
        val target = targets[i]

        val tileLeft = innerCardRect.left + col * (tileW + spacing)
        val tileTop = innerCardRect.top + row * (tileH + spacing)
        val tileRect = RectF(tileLeft, tileTop, tileLeft + tileW, tileTop + tileH)

        val tl = if (row == 0 && col == 0) 0f else innerCornerRadius
        val tr = if (row == 0 && col == cols - 1) 0f else innerCornerRadius
        val br = if (row == rows - 1 && col == cols - 1) 0f else innerCornerRadius
        val bl = if (row == rows - 1 && col == 0) 0f else innerCornerRadius

        val radii = floatArrayOf(tl, tl, tr, tr, br, br, bl, bl)
        val tilePath = Path().apply { addRoundRect(tileRect, radii, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(innerCardPath)
        canvas.clipPath(tilePath)
        canvas.drawRect(tileRect, tileFillPaint)

        val tileBitmap = generateTileBitmap(
            context = context,
            target = target,
            bgColorInt = tileBgColor,
            accentColorInt = accentColorInt,
            isLight = isLight,
            shapeStyle = AiShapeStyle.FRAMELESS,
            widthPx = tileW.toInt(),
            heightPx = tileH.toInt()
        )
        canvas.drawBitmap(tileBitmap, tileLeft, tileTop, null)
        canvas.restore()
    }

    return bitmap
}

// 19. AI ASYMMETRIC BENTO (7 Apps)
fun generateAiFolder7AsymmetricBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 1.5f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val maxCardRadius = minOf(cardRect.width(), cardRect.height()) / 2f
    val outerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(maxCardRadius)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val minDim = minOf(cardRect.width(), cardRect.height())
    val spacing = (minDim * 0.032f).coerceIn(scaleFactor * 1.8f, scaleFactor * 5.5f)

    val innerCardRect = RectF(
        cardRect.left + spacing,
        cardRect.top + spacing,
        cardRect.right - spacing,
        cardRect.bottom - spacing
    )
    val innerCardRadius = maxOf(0f, outerRadius - spacing)
    val innerCardPath = Path().apply {
        addRoundRect(innerCardRect, innerCardRadius, innerCardRadius, Path.Direction.CW)
    }

    val innerCornerRadius = (minDim * 0.08f)
        .coerceIn(scaleFactor * 2.0f, scaleFactor * 7.0f)
        .coerceAtMost(innerCardRadius)

    val tileBgColor = if (isLight) Color.parseColor("#0A000000") else Color.parseColor("#14FFFFFF")
    val tileFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tileBgColor
        style = Paint.Style.FILL
    }

    // Top Hero (Left) and 2 Stacked Right
    val topH = ((innerCardRect.height() - spacing) * 0.62f).coerceAtLeast(1f)
    val rightColW = (innerCardRect.width() * 0.36f).coerceAtLeast(1f)
    val heroW = innerCardRect.width() - spacing - rightColW
    val rightTileH = (topH - spacing) / 2f

    // Big Hero Tile (ChatGPT)
    val heroRect = RectF(innerCardRect.left, innerCardRect.top, innerCardRect.left + heroW, innerCardRect.top + topH)
    val heroRadii = floatArrayOf(0f, 0f, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius)
    val heroPath = Path().apply { addRoundRect(heroRect, heroRadii, Path.Direction.CW) }

    canvas.save()
    canvas.clipPath(innerCardPath)
    canvas.clipPath(heroPath)
    val heroFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColorInt; style = Paint.Style.FILL }
    canvas.drawRect(heroRect, heroFill)
    val heroBitmap = generateTileBitmap(
        context = context,
        target = AiTarget.CHATGPT_TEXT,
        bgColorInt = accentColorInt,
        accentColorInt = if (isLight) Color.WHITE else Color.BLACK,
        isLight = isLight,
        shapeStyle = AiShapeStyle.FRAMELESS,
        isPrimaryAccent = true,
        widthPx = heroW.toInt(),
        heightPx = topH.toInt()
    )
    canvas.drawBitmap(heroBitmap, innerCardRect.left, innerCardRect.top, null)
    canvas.restore()

    // 2 Stacked Right (Grok & Copilot)
    val rightX = innerCardRect.left + heroW + spacing
    val rightTargets = listOf(AiTarget.GROK, AiTarget.COPILOT)
    for (i in 0..1) {
        val top = innerCardRect.top + i * (rightTileH + spacing)
        val rect = RectF(rightX, top, rightX + rightColW, top + rightTileH)

        val tr = if (i == 0) 0f else innerCornerRadius
        val radii = floatArrayOf(innerCornerRadius, innerCornerRadius, tr, tr, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius)
        val path = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(innerCardPath)
        canvas.clipPath(path)
        canvas.drawRect(rect, tileFillPaint)
        val b = generateTileBitmap(
            context = context,
            target = rightTargets[i],
            bgColorInt = tileBgColor,
            accentColorInt = accentColorInt,
            isLight = isLight,
            shapeStyle = AiShapeStyle.FRAMELESS,
            widthPx = rightColW.toInt(),
            heightPx = rightTileH.toInt()
        )
        canvas.drawBitmap(b, rightX, top, null)
        canvas.restore()
    }

    // Bottom Row of 4 Small Tiles
    val botY = innerCardRect.top + topH + spacing
    val botH = innerCardRect.height() - topH - spacing
    val botTileW = (innerCardRect.width() - (spacing * 3f)) / 4f
    val botTargets = listOf(AiTarget.GEMINI_TEXT, AiTarget.CLAUDE, AiTarget.PERPLEXITY, AiTarget.META_AI)

    for (i in 0..3) {
        val left = innerCardRect.left + i * (botTileW + spacing)
        val rect = RectF(left, botY, left + botTileW, botY + botH)

        val bl = if (i == 0) 0f else innerCornerRadius
        val br = if (i == 3) 0f else innerCornerRadius
        val radii = floatArrayOf(innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, br, br, bl, bl)
        val path = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(innerCardPath)
        canvas.clipPath(path)
        canvas.drawRect(rect, tileFillPaint)
        val b = generateTileBitmap(
            context = context,
            target = botTargets[i],
            bgColorInt = tileBgColor,
            accentColorInt = accentColorInt,
            isLight = isLight,
            shapeStyle = AiShapeStyle.FRAMELESS,
            widthPx = botTileW.toInt(),
            heightPx = botH.toInt()
        )
        canvas.drawBitmap(b, left, botY, null)
        canvas.restore()
    }

    return bitmap
}
