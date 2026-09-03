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

// 14. AI QUAD FOLDER (4x2 / Responsive)
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
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val size = minOf(w, h) - (margin * 2f)
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
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 2.0f

    // Always enforce fixed 2:1 aspect ratio container to prevent vertical clipping
    var cardH = h - (margin * 2f)
    var cardW = cardH * targetRatio
    if (cardW > w - (margin * 2f)) {
        cardW = w - (margin * 2f)
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
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 2.0f

    // Enforce fixed 2:1 aspect ratio container to eliminate empty black space
    var cardH = h - (margin * 2f)
    var cardW = cardH * targetRatio
    if (cardW > w - (margin * 2f)) {
        cardW = w - (margin * 2f)
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

// 17. AI 3x3 GRID FOLDER (2x2 / Responsive Multi-Axis AI Hub)
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

    // 1. Dual-Mode Geometry
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
    val cardCornerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(maxCardRadius)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    // 2. Uniform Tight Spacing & Edge Fill
    val minDim = minOf(cardRect.width(), cardRect.height())
    val gap = (minDim * 0.035f).coerceIn(scaleFactor * 2.5f, scaleFactor * 6f)
    val pad = (minDim * 0.045f).coerceIn(scaleFactor * 4f, scaleFactor * 9f)

    val availW = cardRect.width() - (pad * 2f)
    val availH = cardRect.height() - (pad * 2f)
    val tileW = (availW - (gap * 2f)) / 3f
    val tileH = (availH - (gap * 2f)) / 3f

    // 3. Concentric Corner Curvature
    val minTileDim = minOf(tileW, tileH)
    val baseTileRadius = minTileDim * 0.28f
    val outerEdgeRadius = (cardCornerRadius - pad).coerceIn(baseTileRadius, minTileDim / 2f)

    val tileBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(22, 0, 0, 0) else Color.argb(38, 255, 255, 255)
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
            val tileLeft = cardRect.left + pad + col * (tileW + gap)
            val tileTop = cardRect.top + pad + row * (tileH + gap)
            val tileRect = RectF(tileLeft, tileTop, tileLeft + tileW, tileTop + tileH)

            // Outer corner concentric rounding
            val tl = if (row == 0 && col == 0) outerEdgeRadius else baseTileRadius
            val tr = if (row == 0 && col == 2) outerEdgeRadius else baseTileRadius
            val br = if (row == 2 && col == 2) outerEdgeRadius else baseTileRadius
            val bl = if (row == 2 && col == 0) outerEdgeRadius else baseTileRadius

            val radii = floatArrayOf(
                tl, tl,
                tr, tr,
                br, br,
                bl, bl
            )
            val tilePath = Path().apply {
                addRoundRect(tileRect, radii, Path.Direction.CW)
            }
            canvas.drawPath(tilePath, tileBgPaint)

            // Center icon vector inside tile
            val iconSize = (minTileDim * 0.54f).toInt()
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
        }
    }

    return bitmap
}

// 18. AI MEGA FOLDER (4x2 / Adaptive Aspect Ratio)
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

    val margin = scaleFactor * 1.5f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val targetRatio = 2.0f
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

    val cornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val tileBgColor = if (isLight) Color.parseColor("#0A000000") else Color.parseColor("#14FFFFFF")

    if (aspectRatio >= 1.1f) {
        // ================================================================
        // 1. WIDE MODE (5 Columns x 2 Rows)
        // ================================================================
        val pad = cardH * 0.05f
        val gap = cardH * 0.02f

        val availW = cardW - (pad * 2f)
        val availH = cardH - (pad * 2f)

        val tileW = ((availW - (gap * 4f)) / 5f).toInt().coerceAtLeast(1)
        val tileH = ((availH - gap) / 2f).toInt().coerceAtLeast(1)

        val startX = cardRect.left + pad
        val startY = cardRect.top + pad

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
    } else {
        // ================================================================
        // 2. TALL / VERTICAL MODE (2 Columns x 5 Rows)
        // ================================================================
        val pad = cardW * 0.06f
        val gap = cardW * 0.035f

        val availW = cardW - (pad * 2f)
        val availH = cardH - (pad * 2f)

        val tileW = ((availW - gap) / 2f).toInt().coerceAtLeast(1)
        val tileH = ((availH - (gap * 4f)) / 5f).toInt().coerceAtLeast(1)

        val startX = cardRect.left + pad
        val startY = cardRect.top + pad

        val items = listOf(
            Pair(AiTarget.GEMINI_TEXT, AiShapeStyle.CORNER_TOP_LEFT),
            Pair(AiTarget.CHATGPT_TEXT, AiShapeStyle.CORNER_TOP_RIGHT),
            Pair(AiTarget.COPILOT, AiShapeStyle.SQUIRCLE),
            Pair(AiTarget.CLAUDE, AiShapeStyle.SQUIRCLE),
            Pair(AiTarget.GROK, AiShapeStyle.SQUIRCLE),
            Pair(AiTarget.PERPLEXITY, AiShapeStyle.SQUIRCLE),
            Pair(AiTarget.DEEPSEEK, AiShapeStyle.SQUIRCLE),
            Pair(AiTarget.META_AI, AiShapeStyle.SQUIRCLE),
            Pair(AiTarget.POE, AiShapeStyle.CORNER_BOTTOM_LEFT),
            Pair(AiTarget.PI, AiShapeStyle.CORNER_BOTTOM_RIGHT)
        )

        items.forEachIndexed { i, (target, shape) ->
            val r = i / 2
            val c = i % 2
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
            val x = startX + c * (tileW + gap)
            val y = startY + r * (tileH + gap)
            canvas.drawBitmap(tile, x, y, null)
        }
    }

    return bitmap
}


// 19. AI ASYMMETRIC BENTO (3x2 / Adaptive Aspect Ratio)
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
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val targetRatio = 1.5f
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

    val cornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val tileBgColor = if (isLight) Color.parseColor("#0A000000") else Color.parseColor("#14FFFFFF")

    if (aspectRatio >= 1.1f) {
        // ================================================================
        // 1. WIDE MODE (Side Hero + Stacked Column + 4-Column Bottom Row)
        // ================================================================
        val pad = minOf(cardW, cardH) * 0.08f
        val gap = minOf(cardW, cardH) * 0.035f

        val availW = cardW - (pad * 2f)
        val availH = cardH - (pad * 2f)

        val topH = ((availH - gap) * 0.64f).toInt().coerceAtLeast(1)
        val botH = (availH - gap - topH).toInt().coerceAtLeast(1)

        val rightColW = (availW * 0.34f).toInt().coerceAtLeast(1)
        val heroW = (availW - gap - rightColW).toInt().coerceAtLeast(1)
        val rightTileH = ((topH - gap) / 2f).toInt().coerceAtLeast(1)

        val botTileW = ((availW - (gap * 3f)) / 4f).toInt().coerceAtLeast(1)

        val startX = cardRect.left + pad
        val startY = cardRect.top + pad

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
    } else {
        // ================================================================
        // 2. TALL / VERTICAL MODE (Top Hero Banner + 2 Columns x 3 Rows Grid)
        // ================================================================
        val pad = cardW * 0.06f
        val gap = cardW * 0.035f

        val availW = cardW - (pad * 2f)
        val availH = cardH - (pad * 2f)

        val heroH = (availH * 0.26f).toInt().coerceAtLeast(1)
        val remainingH = availH - gap - heroH
        val tileH = ((remainingH - (gap * 2f)) / 3f).toInt().coerceAtLeast(1)
        val tileW = ((availW - gap) / 2f).toInt().coerceAtLeast(1)

        val startX = cardRect.left + pad
        val startY = cardRect.top + pad

        // Hero Banner spanning full width
        val heroTile = generateTileBitmap(
            context = context,
            target = AiTarget.CHATGPT_TEXT,
            bgColorInt = tileBgColor,
            accentColorInt = accentColorInt,
            isLight = isLight,
            shapeStyle = AiShapeStyle.SQUIRCLE,
            isPrimaryAccent = true,
            widthPx = availW.toInt(),
            heightPx = heroH
        )
        canvas.drawBitmap(heroTile, startX, startY, null)

        // 3 Rows x 2 Columns
        val gridStartY = startY + heroH + gap
        val gridItems = listOf(
            Pair(AiTarget.GROK, AiShapeStyle.SQUIRCLE),
            Pair(AiTarget.COPILOT, AiShapeStyle.SQUIRCLE),
            Pair(AiTarget.GEMINI_TEXT, AiShapeStyle.SQUIRCLE),
            Pair(AiTarget.CLAUDE, AiShapeStyle.SQUIRCLE),
            Pair(AiTarget.PERPLEXITY, AiShapeStyle.CORNER_BOTTOM_LEFT),
            Pair(AiTarget.META_AI, AiShapeStyle.CORNER_BOTTOM_RIGHT)
        )

        gridItems.forEachIndexed { i, (target, shape) ->
            val r = i / 2
            val c = i % 2
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
            val bx = startX + c * (tileW + gap)
            val by = gridStartY + r * (tileH + gap)
            canvas.drawBitmap(tile, bx, by, null)
        }
    }

    return bitmap
}
