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
import com.altusix.slate.widgets.common.renderUniversalBentoAsymmetric7
import com.altusix.slate.widgets.common.renderUniversalBentoHero6
import com.altusix.slate.widgets.common.renderUniversalBentoSide8
import kotlin.math.cos
import kotlin.math.sin
import com.altusix.slate.widgets.common.renderUniversalFolderGrid
import com.altusix.slate.widgets.common.renderUniversalBentoQuadrant7
import com.altusix.slate.widgets.common.renderUniversalTriangle4
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

private fun drawAiSlotVector(
    canvas: Canvas,
    context: Context,
    tileRect: RectF,
    target: AiTarget,
    accentColorInt: Int,
    scaleFactor: Float,
    isPrimaryAccent: Boolean = false,
    isLight: Boolean = false
) {
    val resId = context.resources.getIdentifier(target.drawableResName, "drawable", context.packageName)
    if (resId != 0) {
        val drawable = ContextCompat.getDrawable(context, resId)?.mutate() ?: return
        val tint = if (isPrimaryAccent) (if (isLight) Color.WHITE else Color.BLACK) else accentColorInt
        drawable.setTint(tint)

        val minDim = minOf(tileRect.width(), tileRect.height())
        val maxLogoSize = scaleFactor * 46f
        val actualLogoSize = (minDim * (if (isPrimaryAccent) 0.58f else 0.50f)).coerceIn(scaleFactor * 16f, maxLogoSize)

        val intrinsicW = drawable.intrinsicWidth.toFloat()
        val intrinsicH = drawable.intrinsicHeight.toFloat()
        var drawW = actualLogoSize
        var drawH = actualLogoSize

        if (intrinsicW > 0f && intrinsicH > 0f) {
            val aspect = intrinsicW / intrinsicH
            if (aspect > 1f) {
                drawH = actualLogoSize / aspect
            } else {
                drawW = actualLogoSize * aspect
            }
        }

        val l = (tileRect.centerX() - drawW / 2f).toInt()
        val t = (tileRect.centerY() - drawH / 2f).toInt()
        val r = (tileRect.centerX() + drawW / 2f).toInt()
        val b = (tileRect.centerY() + drawH / 2f).toInt()

        drawable.setBounds(l, t, r, b)
        drawable.draw(canvas)
    }
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

private fun getSmartAiHeroLabel(target: AiTarget): String {
    return when (target) {
        AiTarget.CHATGPT_TEXT -> "GPT"
        AiTarget.CHATGPT_VOICE -> "GPT Voice"
        else -> target.title
    }
}

private fun drawDualFlagshipPill(
    context: Context,
    target: AiTarget,
    labelText: String,
    accentColorInt: Int,
    isLight: Boolean,
    widthPx: Int,
    heightPx: Int,
    scaleFactor: Float
): Bitmap {
    val w = widthPx.coerceAtLeast(1)
    val h = heightPx.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val pillRect = RectF(0f, 0f, w.toFloat(), h.toFloat())
    val pillRadius = minOf(w.toFloat(), h.toFloat()) / 2f

    val tileBgColor = if (isLight) Color.parseColor("#0F000000") else Color.parseColor("#1C1C1E")
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tileBgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(pillRect, pillRadius, pillRadius, bgPaint)

    val iconColor = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val resId = context.resources.getIdentifier(target.drawableResName, "drawable", context.packageName)
    val drawable = if (resId != 0) ContextCompat.getDrawable(context, resId)?.mutate() else null
    drawable?.setTint(iconColor)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = iconColor
        typeface = getSlateFont(context, weight = 600)
        textAlign = Paint.Align.CENTER
    }

    val isVerticalStack = h > w * 0.85f

    if (isVerticalStack) {
        // --- 1. TALL / VERTICAL MODE (Icon on top, Label below) ---
        var iconSize = (minOf(w, h) * 0.38f).coerceIn(scaleFactor * 22f, scaleFactor * 44f)
        var textSize = (h * 0.16f).coerceIn(scaleFactor * 11f, scaleFactor * 17f)
        textPaint.textSize = textSize

        // Guard: shrink font size if text is wider than available pill width
        val maxTextW = (w * 0.82f).coerceAtLeast(1f)
        val measuredW = textPaint.measureText(labelText)
        if (measuredW > maxTextW) {
            textSize *= (maxTextW / measuredW)
            textPaint.textSize = textSize
        }

        val textGap = scaleFactor * 5f
        val totalH = iconSize + textGap + textSize
        val topY = (h - totalH) / 2f

        val iconCx = w / 2f
        val iconCy = topY + (iconSize / 2f)

        drawable?.let {
            val l = (iconCx - iconSize / 2f).toInt()
            val t = (iconCy - iconSize / 2f).toInt()
            it.setBounds(l, t, (l + iconSize).toInt(), (t + iconSize).toInt())
            it.draw(canvas)
        }

        val fm = textPaint.fontMetrics
        val textBaseline = topY + iconSize + textGap - fm.ascent
        canvas.drawText(labelText, w / 2f, textBaseline, textPaint)

    } else {
        // --- 2. HORIZONTAL MODE (Icon + Label side-by-side) ---
        var iconSize = (h * 0.44f).coerceIn(scaleFactor * 16f, scaleFactor * 30f)
        var textSize = (h * 0.36f).coerceIn(scaleFactor * 12f, scaleFactor * 19f)
        var textGap = scaleFactor * 8f
        textPaint.textSize = textSize

        var textW = textPaint.measureText(labelText)
        var totalContentW = iconSize + textGap + textW

        // Available width inside rounded pill ends (protecting semicircular caps)
        val maxContentW = (w - (h * 0.65f)).coerceAtLeast(1f)

        // Smart Auto-Scale: If label is long (e.g. "GPT Voice", "Perplexity"), scale down together
        if (totalContentW > maxContentW) {
            val scale = (maxContentW / totalContentW).coerceIn(0.55f, 1f)
            textSize *= scale
            iconSize *= scale
            textGap *= scale
            textPaint.textSize = textSize
            textW = textPaint.measureText(labelText)
            totalContentW = iconSize + textGap + textW
        }

        // Guaranteed safe start X coordinate (never negative)
        val contentStartX = ((w - totalContentW) / 2f).coerceAtLeast(h * 0.22f)
        val iconLeft = contentStartX.toInt()
        val iconTop = ((h - iconSize) / 2f).toInt()

        drawable?.let {
            it.setBounds(iconLeft, iconTop, (iconLeft + iconSize).toInt(), (iconTop + iconSize).toInt())
            it.draw(canvas)
        }

        val textX = contentStartX + iconSize + textGap + (textW / 2f)
        val fm = textPaint.fontMetrics
        val textY = (h / 2f) - ((fm.ascent + fm.descent) / 2f)
        canvas.drawText(labelText, textX, textY, textPaint)
    }

    return bitmap
}


// 10. AI PRIMARY BAR (4x1)
fun generateAiBarHeroPrimaryBitmap(
    context: Context,
    config: SlateWidgetConfig,
    aiConfig: AiWidgetConfig,
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

    val defaultTargets = listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.CLAUDE, AiTarget.GROK)
    val effectiveTargets = List(4) { i -> aiConfig.slots.getOrNull(i) ?: defaultTargets[i] }
    val heroTarget = effectiveTargets[0]
    val heroLabel = getSmartAiHeroLabel(heroTarget)

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

        val maxInnerHFromWidth = availW / 5.1f
        val innerH = minOf(baseInnerH, maxInnerHFromWidth)

        val heroW = innerH * 1.6f
        val divSpacing = innerH * 0.12f
        val gap = innerH * 0.06f
        val rightTileW = innerH

        val totalGroupW = heroW + (divSpacing * 2f) + (rightTileW * 3f) + (gap * 2f)
        val startX = cardRect.left + (cardW - totalGroupW) / 2f
        val innerTopY = cardRect.top + (cardH - innerH) / 2f

        // A. Dynamic Hero Pill
        val heroBitmap = drawTextOnlyPillBitmap(
            context = context,
            labelText = heroLabel,
            accentColorInt = accentColorInt,
            isLight = isLight,
            widthPx = heroW.toInt(),
            heightPx = innerH.toInt()
        )
        canvas.drawBitmap(heroBitmap, startX, innerTopY, null)

        // B. Divider
        val divX = startX + heroW + divSpacing
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.parseColor("#26000000") else Color.parseColor("#26FFFFFF")
            strokeWidth = scaleFactor * 0.8f
        }
        canvas.drawLine(divX, cardRect.centerY() - (innerH * 0.28f), divX, cardRect.centerY() + (innerH * 0.28f), divPaint)

        // C. Dynamic Right Icon Tiles
        val rightStartX = divX + divSpacing
        val targets = listOf(
            Triple(effectiveTargets[1], AiShapeStyle.SQUIRCLE, 0),
            Triple(effectiveTargets[2], AiShapeStyle.SQUIRCLE, 1),
            Triple(effectiveTargets[3], AiShapeStyle.CAPSULE_RIGHT, 2)
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
            labelText = heroLabel,
            accentColorInt = accentColorInt,
            isLight = isLight,
            widthPx = availW.toInt(),
            heightPx = topH
        )
        canvas.drawBitmap(heroBitmap, startX, startY, null)

        val botTileW = ((availW - (gap * 2f)) / 3f).toInt().coerceAtLeast(1)
        val botY = startY + topH + gap

        val bottomTargets = listOf(
            Pair(effectiveTargets[1], AiShapeStyle.CORNER_BOTTOM_LEFT),
            Pair(effectiveTargets[2], AiShapeStyle.SQUIRCLE),
            Pair(effectiveTargets[3], AiShapeStyle.CORNER_BOTTOM_RIGHT)
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

fun generateAiBarHeroPrimaryBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val defaultTargets = listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.CLAUDE, AiTarget.GROK)
    val aiConfig = AiWidgetConfig.load(context, widgetId, 4, defaultTargets)
    return generateAiBarHeroPrimaryBitmap(context, config, aiConfig, isResponsive, wDp, hDp, widgetId)
}

// 11. AI DOCK BAR (5 Apps)
fun generateAiBarDock5Bitmap(
    context: Context,
    config: SlateWidgetConfig,
    aiConfig: AiWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val isVertical = isResponsive && (hDp > wDp)
    val cols = if (isVertical) 1 else 5
    val rows = if (isVertical) 5 else 1

    val defaultTargets = listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.CLAUDE, AiTarget.GROK, AiTarget.PERPLEXITY)
    val targets = List(5) { i -> aiConfig.slots.getOrNull(i) ?: defaultTargets[i] }

    return renderUniversalFolderGrid(
        context = context, config = config, isResponsive = isResponsive,
        wDp = wDp, hDp = hDp, cols = cols, rows = rows, showTileBackground = true
    ) { canvas, tileRect, index, scaleFactor, _ ->
        val target = targets.getOrElse(index) { AiTarget.GEMINI_TEXT }
        drawAiSlotVector(canvas, context, tileRect, target, accentColorInt, scaleFactor)
    }
}

fun generateAiBarDock5Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val defaultTargets = listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.CLAUDE, AiTarget.GROK, AiTarget.PERPLEXITY)
    val aiConfig = AiWidgetConfig.load(context, widgetId, 5, defaultTargets)
    return generateAiBarDock5Bitmap(context, config, aiConfig, isResponsive, wDp, hDp, widgetId)
}

// 12. AI CAPSULE BAR (4 Apps)
fun generateAiBarCapsuleBitmap(
    context: Context,
    config: SlateWidgetConfig,
    aiConfig: AiWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val isVertical = isResponsive && (hDp > wDp)
    val cols = if (isVertical) 1 else 4
    val rows = if (isVertical) 4 else 1

    val defaultTargets = listOf(AiTarget.CHATGPT_TEXT, AiTarget.PERPLEXITY, AiTarget.CLAUDE, AiTarget.GEMINI_TEXT)
    val targets = List(4) { i -> aiConfig.slots.getOrNull(i) ?: defaultTargets[i] }

    return renderUniversalFolderGrid(
        context = context, config = config, isResponsive = isResponsive,
        wDp = wDp, hDp = hDp, cols = cols, rows = rows, showTileBackground = true
    ) { canvas, tileRect, index, scaleFactor, _ ->
        val target = targets.getOrElse(index) { AiTarget.GEMINI_TEXT }
        drawAiSlotVector(canvas, context, tileRect, target, accentColorInt, scaleFactor)
    }
}

fun generateAiBarCapsuleBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val defaultTargets = listOf(AiTarget.CHATGPT_TEXT, AiTarget.PERPLEXITY, AiTarget.CLAUDE, AiTarget.GEMINI_TEXT)
    val aiConfig = AiWidgetConfig.load(context, widgetId, 4, defaultTargets)
    return generateAiBarCapsuleBitmap(context, config, aiConfig, isResponsive, wDp, hDp, widgetId)
}

// 13. AI DUAL FLAGSHIP BAR (2 Apps)
fun generateAiBarDualFlagshipBitmap(
    context: Context,
    config: SlateWidgetConfig,
    aiConfig: AiWidgetConfig,
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

    val defaultTargets = listOf(AiTarget.CHATGPT_TEXT, AiTarget.GEMINI_TEXT)
    val leftTarget = aiConfig.slots.getOrNull(0) ?: defaultTargets[0]
    val rightTarget = aiConfig.slots.getOrNull(1) ?: defaultTargets[1]

    // Uses smart labels (e.g. "GPT", "GPT Voice", "Claude")
    val leftLabel = getSmartAiHeroLabel(leftTarget)
    val rightLabel = getSmartAiHeroLabel(rightTarget)

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

    if (aspectRatio >= 1.75f) {
        val cornerRadius = cardH / 2f
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

        val pad = cardH * 0.08f
        val innerH = cardH - (pad * 2f)
        val availW = cardW - (pad * 2f)
        val gap = innerH * 0.08f
        val pillW = ((availW - gap) / 2f).toInt().coerceAtLeast(1)

        val leftPill = drawDualFlagshipPill(
            context, leftTarget, leftLabel, accentColorInt, isLight, pillW, innerH.toInt(), scaleFactor
        )
        canvas.drawBitmap(leftPill, cardRect.left + pad, cardRect.top + pad, null)

        val rightPill = drawDualFlagshipPill(
            context, rightTarget, rightLabel, accentColorInt, isLight, pillW, innerH.toInt(), scaleFactor
        )
        canvas.drawBitmap(rightPill, cardRect.left + pad + pillW + gap, cardRect.top + pad, null)
    } else {
        val cornerRadius = if (aspectRatio < 0.85f) cardW / 2f else getStandardCornerRadius(scaleFactor)
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

        val pad = minOf(cardW, cardH) * 0.06f
        val gap = minOf(cardW, cardH) * 0.04f
        val availW = cardW - (pad * 2f)
        val availH = cardH - (pad * 2f)

        val pillH = ((availH - gap) / 2f).toInt().coerceAtLeast(1)
        val pillW = availW.toInt().coerceAtLeast(1)

        val startX = cardRect.left + pad
        val startY = cardRect.top + pad

        val topPill = drawDualFlagshipPill(
            context, leftTarget, leftLabel, accentColorInt, isLight, pillW, pillH, scaleFactor
        )
        canvas.drawBitmap(topPill, startX, startY, null)

        val botPill = drawDualFlagshipPill(
            context, rightTarget, rightLabel, accentColorInt, isLight, pillW, pillH, scaleFactor
        )
        canvas.drawBitmap(botPill, startX, startY + pillH + gap, null)
    }

    return bitmap
}

fun generateAiBarDualFlagshipBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val defaultTargets = listOf(AiTarget.CHATGPT_TEXT, AiTarget.GEMINI_TEXT)
    val aiConfig = AiWidgetConfig.load(context, widgetId, 2, defaultTargets)
    return generateAiBarDualFlagshipBitmap(context, config, aiConfig, isResponsive, wDp, hDp, widgetId)
}

// 14. AI QUAD FOLDER (4 Apps)
fun generateAiFolder4ClassicBitmap(
    context: Context,
    config: SlateWidgetConfig,
    aiConfig: AiWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val defaultTargets = listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.PERPLEXITY, AiTarget.CLAUDE)
    val targets = List(4) { i -> aiConfig.slots.getOrNull(i) ?: defaultTargets[i] }

    return renderUniversalFolderGrid(
        context = context, config = config, isResponsive = isResponsive,
        wDp = wDp, hDp = hDp, cols = 2, rows = 2, showTileBackground = true
    ) { canvas, tileRect, index, scaleFactor, _ ->
        val target = targets.getOrElse(index) { AiTarget.GEMINI_TEXT }
        drawAiSlotVector(canvas, context, tileRect, target, accentColorInt, scaleFactor)
    }
}

fun generateAiFolder4ClassicBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val defaultTargets = listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.PERPLEXITY, AiTarget.CLAUDE)
    val aiConfig = AiWidgetConfig.load(context, widgetId, 4, defaultTargets)
    return generateAiFolder4ClassicBitmap(context, config, aiConfig, isResponsive, wDp, hDp, widgetId)
}

// 15. AI BENTO FOLDER (6 Apps)
fun generateAiFolder6BentoHeroBitmap(
    context: Context,
    config: SlateWidgetConfig,
    aiConfig: AiWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val defaultTargets = listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.CLAUDE, AiTarget.GROK, AiTarget.DEEPSEEK, AiTarget.META_AI)
    val targets = List(6) { i -> aiConfig.slots.getOrNull(i) ?: defaultTargets[i] }

    return renderUniversalBentoHero6(
        context = context, config = config, isResponsive = isResponsive,
        wDp = wDp, hDp = hDp, showTileBackground = true
    ) { canvas, tileRect, index, scaleFactor, _ ->
        val target = targets.getOrElse(index) { AiTarget.GEMINI_TEXT }
        drawAiSlotVector(canvas, context, tileRect, target, accentColorInt, scaleFactor)
    }
}

fun generateAiFolder6BentoHeroBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val defaultTargets = listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.CLAUDE, AiTarget.GROK, AiTarget.DEEPSEEK, AiTarget.META_AI)
    val aiConfig = AiWidgetConfig.load(context, widgetId, 6, defaultTargets)
    return generateAiFolder6BentoHeroBitmap(context, config, aiConfig, isResponsive, wDp, hDp, widgetId)
}

// 16. AI SIDE BENTO FOLDER (8 Apps)
fun generateAiFolder8BentoSideBitmap(
    context: Context,
    config: SlateWidgetConfig,
    aiConfig: AiWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val defaultTargets = listOf(
        AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.CLAUDE, AiTarget.GROK,
        AiTarget.PERPLEXITY, AiTarget.COPILOT, AiTarget.DEEPSEEK, AiTarget.META_AI
    )
    val targets = List(8) { i -> aiConfig.slots.getOrNull(i) ?: defaultTargets[i] }

    return renderUniversalBentoSide8(
        context = context, config = config, isResponsive = isResponsive,
        wDp = wDp, hDp = hDp, showTileBackground = true
    ) { canvas, tileRect, index, scaleFactor, _ ->
        val target = targets.getOrElse(index) { AiTarget.GEMINI_TEXT }
        drawAiSlotVector(canvas, context, tileRect, target, accentColorInt, scaleFactor)
    }
}

fun generateAiFolder8BentoSideBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val defaultTargets = listOf(
        AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.CLAUDE, AiTarget.GROK,
        AiTarget.PERPLEXITY, AiTarget.COPILOT, AiTarget.DEEPSEEK, AiTarget.META_AI
    )
    val aiConfig = AiWidgetConfig.load(context, widgetId, 8, defaultTargets)
    return generateAiFolder8BentoSideBitmap(context, config, aiConfig, isResponsive, wDp, hDp, widgetId)
}

// 17. AI 3x3 GRID FOLDER (9 Apps)
fun generateAiFolder9GridBitmap(
    context: Context,
    config: SlateWidgetConfig,
    aiConfig: AiWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val defaultTargets = listOf(
        AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.COPILOT,
        AiTarget.GROK, AiTarget.CLAUDE, AiTarget.DEEPSEEK,
        AiTarget.PERPLEXITY, AiTarget.META_AI, AiTarget.POE
    )
    val targets = List(9) { i -> aiConfig.slots.getOrNull(i) ?: defaultTargets[i] }

    return renderUniversalFolderGrid(
        context = context, config = config, isResponsive = isResponsive,
        wDp = wDp, hDp = hDp, cols = 3, rows = 3, showTileBackground = true
    ) { canvas, tileRect, index, scaleFactor, _ ->
        val target = targets.getOrElse(index) { AiTarget.GEMINI_TEXT }
        drawAiSlotVector(canvas, context, tileRect, target, accentColorInt, scaleFactor)
    }
}

fun generateAiFolder9GridBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val defaultTargets = listOf(
        AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.COPILOT,
        AiTarget.GROK, AiTarget.CLAUDE, AiTarget.DEEPSEEK,
        AiTarget.PERPLEXITY, AiTarget.META_AI, AiTarget.POE
    )
    val aiConfig = AiWidgetConfig.load(context, widgetId, 9, defaultTargets)
    return generateAiFolder9GridBitmap(context, config, aiConfig, isResponsive, wDp, hDp, widgetId)
}

// 18. AI MEGA FOLDER (10 Apps)
fun generateAiFolder10MegaBitmap(
    context: Context,
    config: SlateWidgetConfig,
    aiConfig: AiWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val isVertical = isResponsive && (hDp > wDp)
    val cols = if (isVertical) 2 else 5
    val rows = if (isVertical) 5 else 2

    val defaultTargets = listOf(
        AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.COPILOT, AiTarget.CLAUDE, AiTarget.GROK,
        AiTarget.PERPLEXITY, AiTarget.DEEPSEEK, AiTarget.META_AI, AiTarget.POE, AiTarget.MISTRAL
    )
    val targets = List(10) { i -> aiConfig.slots.getOrNull(i) ?: defaultTargets[i] }

    return renderUniversalFolderGrid(
        context = context, config = config, isResponsive = isResponsive,
        wDp = wDp, hDp = hDp, cols = cols, rows = rows, showTileBackground = true
    ) { canvas, tileRect, index, scaleFactor, _ ->
        val target = targets.getOrElse(index) { AiTarget.GEMINI_TEXT }
        drawAiSlotVector(canvas, context, tileRect, target, accentColorInt, scaleFactor)
    }
}

fun generateAiFolder10MegaBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val defaultTargets = listOf(
        AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.COPILOT, AiTarget.CLAUDE, AiTarget.GROK,
        AiTarget.PERPLEXITY, AiTarget.DEEPSEEK, AiTarget.META_AI, AiTarget.POE, AiTarget.MISTRAL
    )
    val aiConfig = AiWidgetConfig.load(context, widgetId, 10, defaultTargets)
    return generateAiFolder10MegaBitmap(context, config, aiConfig, isResponsive, wDp, hDp, widgetId)
}

// 19. AI ASYMMETRIC BENTO (7 Apps)
fun generateAiFolder7AsymmetricBitmap(
    context: Context,
    config: SlateWidgetConfig,
    aiConfig: AiWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val isLight = config.themeMode == "LIGHT"
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val defaultTargets = listOf(
        AiTarget.CHATGPT_TEXT, AiTarget.GROK, AiTarget.COPILOT,
        AiTarget.GEMINI_TEXT, AiTarget.CLAUDE, AiTarget.PERPLEXITY, AiTarget.META_AI
    )
    val targets = List(7) { i -> aiConfig.slots.getOrNull(i) ?: defaultTargets[i] }

    return renderUniversalBentoAsymmetric7(
        context = context, config = config, isResponsive = isResponsive,
        wDp = wDp, hDp = hDp, showTileBackground = true
    ) { canvas, tileRect, index, scaleFactor, _ ->
        val target = targets.getOrElse(index) { AiTarget.CHATGPT_TEXT }
        val isHero = index == 0

        if (isHero) {
            val heroFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColorInt
                style = Paint.Style.FILL
            }
            canvas.drawRect(tileRect, heroFill)
        }

        drawAiSlotVector(
            canvas = canvas, context = context, tileRect = tileRect, target = target,
            accentColorInt = accentColorInt, scaleFactor = scaleFactor,
            isPrimaryAccent = isHero, isLight = isLight
        )
    }
}

fun generateAiFolder7AsymmetricBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val defaultTargets = listOf(
        AiTarget.CHATGPT_TEXT, AiTarget.GROK, AiTarget.COPILOT,
        AiTarget.GEMINI_TEXT, AiTarget.CLAUDE, AiTarget.PERPLEXITY, AiTarget.META_AI
    )
    val aiConfig = AiWidgetConfig.load(context, widgetId, 7, defaultTargets)
    return generateAiFolder7AsymmetricBitmap(context, config, aiConfig, isResponsive, wDp, hDp, widgetId)
}