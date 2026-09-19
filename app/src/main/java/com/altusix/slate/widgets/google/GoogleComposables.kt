package com.altusix.slate.widgets.google

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.content.ContextCompat
import com.altusix.slate.R
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.createSupersampledCanvas
import com.altusix.slate.utils.getSafeBgColor
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius
import android.graphics.Shader
import android.graphics.RadialGradient

// 1. GOOGLE SEARCH CAPSULE (4x1)
fun generateGoogleSearchCapsuleBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val iconColor = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE

    val margin = scaleFactor * 2f
    val barHeight = (h - (margin * 2f)).coerceIn(scaleFactor * 58f, scaleFactor * 76f)
    val topY = (h - barHeight) / 2f
    val cardRect = RectF(margin, topY, w - margin, topY + barHeight)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    val capsuleRadius = cardRect.height() / 2f
    canvas.drawRoundRect(cardRect, capsuleRadius, capsuleRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(20, 0, 0, 0) else Color.argb(28, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1.2f
    }
    canvas.drawRoundRect(cardRect, capsuleRadius, capsuleRadius, borderPaint)

    val innerH = cardRect.height()
    val sideInset = innerH * 0.44f

    // 1. Google 'G' Logo (Accented)
    val gSize = innerH * 0.44f
    val gCx = cardRect.left + sideInset
    val gLeft = (gCx - (gSize / 2f)).toInt()
    val gTop = (cardRect.centerY() - (gSize / 2f)).toInt()
    ContextCompat.getDrawable(context, R.drawable.ic_google_logo)?.mutate()?.apply {
        setTint(accentColorInt)
        setBounds(gLeft, gTop, (gLeft + gSize).toInt(), (gTop + gSize).toInt())
        draw(canvas)
    }

    // 2. Trailing Icons with Generous Spacing
    val iconSize = (innerH * 0.36f).coerceIn(scaleFactor * 18f, scaleFactor * 24f)
    val iconStep = (innerH * 0.80f).coerceIn(scaleFactor * 48f, scaleFactor * 58f)
    val gRightBoundary = gCx + (gSize / 2f) + (scaleFactor * 24f)

    val lensCx = cardRect.right - sideInset
    val geminiCx = lensCx - iconStep
    val micCx = lensCx - (iconStep * 2f)

    val showLens = wDp >= 140 && (lensCx - iconSize / 2f) > gRightBoundary
    val showGemini = wDp >= 210 && (geminiCx - iconSize / 2f) > gRightBoundary
    val showMic = wDp >= 280 && (micCx - iconSize / 2f) > gRightBoundary

    if (showLens) {
        val lensLeft = (lensCx - iconSize / 2f).toInt()
        val lensTop = (cardRect.centerY() - iconSize / 2f).toInt()
        ContextCompat.getDrawable(context, R.drawable.ic_google_lens)?.mutate()?.apply {
            setTint(iconColor)
            setBounds(lensLeft, lensTop, (lensLeft + iconSize).toInt(), (lensTop + iconSize).toInt())
            draw(canvas)
        }
    }

    if (showGemini) {
        val geminiLeft = (geminiCx - iconSize / 2f).toInt()
        val geminiTop = (cardRect.centerY() - iconSize / 2f).toInt()
        ContextCompat.getDrawable(context, R.drawable.ic_google_gemini)?.mutate()?.apply {
            setTint(iconColor)
            setBounds(geminiLeft, geminiTop, (geminiLeft + iconSize).toInt(), (geminiTop + iconSize).toInt())
            draw(canvas)
        }
    }

    if (showMic) {
        val micLeft = (micCx - iconSize / 2f).toInt()
        val micTop = (cardRect.centerY() - iconSize / 2f).toInt()
        ContextCompat.getDrawable(context, R.drawable.ic_mic)?.mutate()?.apply {
            setTint(iconColor)
            setBounds(micLeft, micTop, (micLeft + iconSize).toInt(), (micTop + iconSize).toInt())
            draw(canvas)
        }
    }

    return bitmap
}

// 2. GOOGLE WORKSPACE QUAD (2x2 / 4x1 / 1x4 Pivot)
fun generateGoogleWorkspaceQuadBitmap(
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

    // 1. Aspect Ratio Reflow Logic
    val isVertical = isResponsive && (hDp > wDp * 1.35f)
    val isHorizontal = isResponsive && (wDp > hDp * 1.35f)
    val cols = when {
        isHorizontal -> 4
        isVertical -> 1
        else -> 2
    }
    val rows = when {
        isHorizontal -> 1
        isVertical -> 4
        else -> 2
    }

    val targetRatio = cols.toFloat() / rows.toFloat()
    val margin = scaleFactor * 1.5f

    // Width-Dominant container fit for single-row bars
    val cardRect = if (isResponsive || rows == 1 || targetRatio >= 2.5f) {
        if (!isResponsive && targetRatio > 0f) {
            val maxAllowedH = h - (margin * 2f)
            val idealH = (w - (margin * 2f)) / targetRatio
            val cardH = idealH.coerceAtMost(maxAllowedH)
            val topY = (h - cardH) / 2f
            RectF(margin, topY, w - margin, topY + cardH)
        } else {
            RectF(margin, margin, w - margin, h - margin)
        }
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

    // 2. Uniform Proportional Spacing (1.8dp - 5.5dp)
    val minDim = minOf(cardRect.width(), cardRect.height())
    val spacing = (minDim * 0.032f).coerceIn(scaleFactor * 1.8f, scaleFactor * 5.5f)

    // 3. Exact Concentric Inner Boundary Path
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

    val availableW = innerCardRect.width() - (spacing * (cols - 1))
    val availableH = innerCardRect.height() - (spacing * (rows - 1))
    val tileW = availableW / cols
    val tileH = availableH / rows

    val innerCornerRadius = (minOf(tileW, tileH) * 0.20f)
        .coerceIn(scaleFactor * 2.0f, scaleFactor * 7.0f)
        .coerceAtMost(minOf(tileW, tileH) / 2f)

    val innerBgColor = if (isLight) Color.parseColor("#EAEAEF") else Color.parseColor("#161618")
    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = innerBgColor; style = Paint.Style.FILL }

    val iconDrawables = listOf(
        R.drawable.ic_google_logo,
        R.drawable.ic_youtube,
        R.drawable.ic_gmail,
        R.drawable.ic_drive
    )

    val maxIconSize = scaleFactor * 46f
    val iconSize = (minOf(tileW, tileH) * 0.44f).coerceIn(scaleFactor * 16f, maxIconSize).toInt()

    for (index in 0 until 4) {
        val col = index % cols
        val row = index / cols
        val left = innerCardRect.left + col * (tileW + spacing)
        val top = innerCardRect.top + row * (tileH + spacing)
        val tileRect = RectF(left, top, left + tileW, top + tileH)

        val isTopOuter = (row == 0)
        val isBottomOuter = (row == rows - 1)
        val isLeftOuter = (col == 0)
        val isRightOuter = (col == cols - 1)

        val tl = if (isTopOuter && isLeftOuter) 0f else innerCornerRadius
        val tr = if (isTopOuter && isRightOuter) 0f else innerCornerRadius
        val br = if (isBottomOuter && isRightOuter) 0f else innerCornerRadius
        val bl = if (isBottomOuter && isLeftOuter) 0f else innerCornerRadius

        val radii = floatArrayOf(tl, tl, tr, tr, br, br, bl, bl)
        val tilePath = Path().apply { addRoundRect(tileRect, radii, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(innerCardPath)
        canvas.clipPath(tilePath)
        canvas.drawRect(tileRect, innerPaint)

        val resId = iconDrawables[index]
        ContextCompat.getDrawable(context, resId)?.mutate()?.apply {
            setTint(accentColorInt)
            val iconLeft = (tileRect.centerX() - iconSize / 2f).toInt()
            val iconTop = (tileRect.centerY() - iconSize / 2f).toInt()
            setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
            draw(canvas)
        }
        canvas.restore()
    }

    return bitmap
}

// 3. GOOGLE TRIO BENTO (2x2: Top Google Banner + YouTube & Photos / 3x1 / 1x3 Pivot)
fun generateGoogleTrioBitmap(
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

    val innerCornerRadius = (minDim * 0.08f)
        .coerceIn(scaleFactor * 2.0f, scaleFactor * 7.0f)
        .coerceAtMost(innerCardRadius)

    val innerBgColor = if (isLight) Color.parseColor("#EAEAEF") else Color.parseColor("#161618")
    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = innerBgColor; style = Paint.Style.FILL }

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.06f
    }

    val aspectRatio = cardRect.width() / cardRect.height()

    when {
        // --- 1. TALL VERTICAL STRIP (1 Column x 3 Rows) ---
        isResponsive && aspectRatio < 0.72f -> {
            val tileW = innerCardRect.width()
            val tileH = (innerCardRect.height() - (spacing * 2f)) / 3f
            val iconSize = (minOf(tileW, tileH) * 0.44f).coerceIn(scaleFactor * 16f, scaleFactor * 46f).toInt()

            // Tile 0: Google (Top)
            val tile0 = RectF(innerCardRect.left, innerCardRect.top, innerCardRect.right, innerCardRect.top + tileH)
            val r0 = floatArrayOf(0f, 0f, 0f, 0f, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius)
            val p0 = Path().apply { addRoundRect(tile0, r0, Path.Direction.CW) }

            canvas.save()
            canvas.clipPath(innerCardPath)
            canvas.clipPath(p0)
            canvas.drawRect(tile0, innerPaint)

            var targetTextSize = tile0.height() * 0.36f
            textPaint.textSize = targetTextSize
            val maxW = tile0.width() * 0.82f
            val measuredW = textPaint.measureText("GOOGLE")
            if (measuredW > maxW) {
                targetTextSize *= (maxW / measuredW)
                textPaint.textSize = targetTextSize
            }
            if (targetTextSize >= scaleFactor * 13f) {
                val fm = textPaint.fontMetrics
                val textY = tile0.centerY() - ((fm.descent + fm.ascent) / 2f)
                canvas.drawText("GOOGLE", tile0.centerX(), textY, textPaint)
            } else {
                ContextCompat.getDrawable(context, R.drawable.ic_google_logo)?.mutate()?.apply {
                    setTint(accentColorInt)
                    setBounds((tile0.centerX() - iconSize / 2f).toInt(), (tile0.centerY() - iconSize / 2f).toInt(), (tile0.centerX() + iconSize / 2f).toInt(), (tile0.centerY() + iconSize / 2f).toInt())
                    draw(canvas)
                }
            }
            canvas.restore()

            // Tile 1: YouTube (Middle)
            val tile1 = RectF(innerCardRect.left, tile0.bottom + spacing, innerCardRect.right, tile0.bottom + spacing + tileH)
            val r1 = floatArrayOf(innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius)
            val p1 = Path().apply { addRoundRect(tile1, r1, Path.Direction.CW) }

            canvas.save()
            canvas.clipPath(innerCardPath)
            canvas.clipPath(p1)
            canvas.drawRect(tile1, innerPaint)
            ContextCompat.getDrawable(context, R.drawable.ic_youtube)?.mutate()?.apply {
                setTint(accentColorInt)
                setBounds((tile1.centerX() - iconSize / 2f).toInt(), (tile1.centerY() - iconSize / 2f).toInt(), (tile1.centerX() + iconSize / 2f).toInt(), (tile1.centerY() + iconSize / 2f).toInt())
                draw(canvas)
            }
            canvas.restore()

            // Tile 2: Photos (Bottom)
            val tile2 = RectF(innerCardRect.left, tile1.bottom + spacing, innerCardRect.right, innerCardRect.bottom)
            val r2 = floatArrayOf(innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, 0f, 0f, 0f, 0f)
            val p2 = Path().apply { addRoundRect(tile2, r2, Path.Direction.CW) }

            canvas.save()
            canvas.clipPath(innerCardPath)
            canvas.clipPath(p2)
            canvas.drawRect(tile2, innerPaint)
            ContextCompat.getDrawable(context, R.drawable.ic_goolge_photos)?.mutate()?.apply {
                setTint(accentColorInt)
                setBounds((tile2.centerX() - iconSize / 2f).toInt(), (tile2.centerY() - iconSize / 2f).toInt(), (tile2.centerX() + iconSize / 2f).toInt(), (tile2.centerY() + iconSize / 2f).toInt())
                draw(canvas)
            }
            canvas.restore()
        }

        // --- 2. WIDE HORIZONTAL STRIP (3 Columns x 1 Row) ---
        isResponsive && aspectRatio > 1.65f -> {
            val tileW = (innerCardRect.width() - (spacing * 2f)) / 3f
            val tileH = innerCardRect.height()
            val iconSize = (minOf(tileW, tileH) * 0.44f).coerceIn(scaleFactor * 16f, scaleFactor * 46f).toInt()

            // Tile 0: Google (Left)
            val tile0 = RectF(innerCardRect.left, innerCardRect.top, innerCardRect.left + tileW, innerCardRect.bottom)
            val r0 = floatArrayOf(0f, 0f, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, 0f, 0f)
            val p0 = Path().apply { addRoundRect(tile0, r0, Path.Direction.CW) }

            canvas.save()
            canvas.clipPath(innerCardPath)
            canvas.clipPath(p0)
            canvas.drawRect(tile0, innerPaint)

            var targetTextSize = tile0.height() * 0.36f
            textPaint.textSize = targetTextSize
            val maxW = tile0.width() * 0.82f
            val measuredW = textPaint.measureText("GOOGLE")
            if (measuredW > maxW) {
                targetTextSize *= (maxW / measuredW)
                textPaint.textSize = targetTextSize
            }
            if (targetTextSize >= scaleFactor * 13f) {
                val fm = textPaint.fontMetrics
                val textY = tile0.centerY() - ((fm.descent + fm.ascent) / 2f)
                canvas.drawText("GOOGLE", tile0.centerX(), textY, textPaint)
            } else {
                ContextCompat.getDrawable(context, R.drawable.ic_google_logo)?.mutate()?.apply {
                    setTint(accentColorInt)
                    setBounds((tile0.centerX() - iconSize / 2f).toInt(), (tile0.centerY() - iconSize / 2f).toInt(), (tile0.centerX() + iconSize / 2f).toInt(), (tile0.centerY() + iconSize / 2f).toInt())
                    draw(canvas)
                }
            }
            canvas.restore()

            // Tile 1: YouTube (Center)
            val tile1 = RectF(tile0.right + spacing, innerCardRect.top, tile0.right + spacing + tileW, innerCardRect.bottom)
            val r1 = floatArrayOf(innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius)
            val p1 = Path().apply { addRoundRect(tile1, r1, Path.Direction.CW) }

            canvas.save()
            canvas.clipPath(innerCardPath)
            canvas.clipPath(p1)
            canvas.drawRect(tile1, innerPaint)
            ContextCompat.getDrawable(context, R.drawable.ic_youtube)?.mutate()?.apply {
                setTint(accentColorInt)
                setBounds((tile1.centerX() - iconSize / 2f).toInt(), (tile1.centerY() - iconSize / 2f).toInt(), (tile1.centerX() + iconSize / 2f).toInt(), (tile1.centerY() + iconSize / 2f).toInt())
                draw(canvas)
            }
            canvas.restore()

            // Tile 2: Photos (Right)
            val tile2 = RectF(tile1.right + spacing, innerCardRect.top, innerCardRect.right, innerCardRect.bottom)
            val r2 = floatArrayOf(innerCornerRadius, innerCornerRadius, 0f, 0f, 0f, 0f, innerCornerRadius, innerCornerRadius)
            val p2 = Path().apply { addRoundRect(tile2, r2, Path.Direction.CW) }

            canvas.save()
            canvas.clipPath(innerCardPath)
            canvas.clipPath(p2)
            canvas.drawRect(tile2, innerPaint)
            ContextCompat.getDrawable(context, R.drawable.ic_goolge_photos)?.mutate()?.apply {
                setTint(accentColorInt)
                setBounds((tile2.centerX() - iconSize / 2f).toInt(), (tile2.centerY() - iconSize / 2f).toInt(), (tile2.centerX() + iconSize / 2f).toInt(), (tile2.centerY() + iconSize / 2f).toInt())
                draw(canvas)
            }
            canvas.restore()
        }

        // --- 3. STANDARD BENTO (Top Full-Width Banner + Bottom 2 Tiles) ---
        else -> {
            val availableH = innerCardRect.height() - spacing
            val topH = availableH * 0.48f
            val bottomH = availableH - topH
            val bottomTileW = (innerCardRect.width() - spacing) / 2f
            val iconSize = (minOf(bottomTileW, bottomH) * 0.44f).coerceIn(scaleFactor * 16f, scaleFactor * 46f).toInt()

            // Top Google Banner
            val topRect = RectF(innerCardRect.left, innerCardRect.top, innerCardRect.right, innerCardRect.top + topH)
            val topRadii = floatArrayOf(0f, 0f, 0f, 0f, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius)
            val topPath = Path().apply { addRoundRect(topRect, topRadii, Path.Direction.CW) }

            canvas.save()
            canvas.clipPath(innerCardPath)
            canvas.clipPath(topPath)
            canvas.drawRect(topRect, innerPaint)

            var targetTextSize = topRect.height() * 0.40f
            textPaint.textSize = targetTextSize
            val maxTextWidth = topRect.width() * 0.85f
            val measuredWidth = textPaint.measureText("GOOGLE")
            if (measuredWidth > maxTextWidth) {
                targetTextSize *= (maxTextWidth / measuredWidth)
                textPaint.textSize = targetTextSize
            }
            if (targetTextSize >= scaleFactor * 13f) {
                val fm = textPaint.fontMetrics
                val textY = topRect.centerY() - ((fm.descent + fm.ascent) / 2f)
                canvas.drawText("GOOGLE", topRect.centerX(), textY, textPaint)
            } else {
                ContextCompat.getDrawable(context, R.drawable.ic_google_logo)?.mutate()?.apply {
                    setTint(accentColorInt)
                    setBounds((topRect.centerX() - iconSize / 2f).toInt(), (topRect.centerY() - iconSize / 2f).toInt(), (topRect.centerX() + iconSize / 2f).toInt(), (topRect.centerY() + iconSize / 2f).toInt())
                    draw(canvas)
                }
            }
            canvas.restore()

            // Bottom-Left (YouTube)
            val bLeftRect = RectF(innerCardRect.left, topRect.bottom + spacing, innerCardRect.left + bottomTileW, innerCardRect.bottom)
            val bLeftRadii = floatArrayOf(innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, 0f, 0f)
            val bLeftPath = Path().apply { addRoundRect(bLeftRect, bLeftRadii, Path.Direction.CW) }

            canvas.save()
            canvas.clipPath(innerCardPath)
            canvas.clipPath(bLeftPath)
            canvas.drawRect(bLeftRect, innerPaint)
            ContextCompat.getDrawable(context, R.drawable.ic_youtube)?.mutate()?.apply {
                setTint(accentColorInt)
                setBounds((bLeftRect.centerX() - iconSize / 2f).toInt(), (bLeftRect.centerY() - iconSize / 2f).toInt(), (bLeftRect.centerX() + iconSize / 2f).toInt(), (bLeftRect.centerY() + iconSize / 2f).toInt())
                draw(canvas)
            }
            canvas.restore()

            // Bottom-Right (Photos)
            val bRightRect = RectF(bLeftRect.right + spacing, topRect.bottom + spacing, innerCardRect.right, innerCardRect.bottom)
            val bRightRadii = floatArrayOf(innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, 0f, 0f, innerCornerRadius, innerCornerRadius)
            val bRightPath = Path().apply { addRoundRect(bRightRect, bRightRadii, Path.Direction.CW) }

            canvas.save()
            canvas.clipPath(innerCardPath)
            canvas.clipPath(bRightPath)
            canvas.drawRect(bRightRect, innerPaint)
            ContextCompat.getDrawable(context, R.drawable.ic_goolge_photos)?.mutate()?.apply {
                setTint(accentColorInt)
                setBounds((bRightRect.centerX() - iconSize / 2f).toInt(), (bRightRect.centerY() - iconSize / 2f).toInt(), (bRightRect.centerX() + iconSize / 2f).toInt(), (bRightRect.centerY() + iconSize / 2f).toInt())
                draw(canvas)
            }
            canvas.restore()
        }
    }

    return bitmap
}

// 4. GOOGLE 3x3 GRID FOLDER (2x2 / 9-App Hub)
fun generateGoogleGrid9Bitmap(
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

    val innerCardBg = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    val googleIcons = listOf(
        R.drawable.ic_google_logo,
        R.drawable.ic_chrome,
        R.drawable.ic_gmail,
        R.drawable.ic_maps,
        R.drawable.ic_youtube,
        R.drawable.ic_goolge_photos,
        R.drawable.ic_drive,
        R.drawable.ic_calendar,
        R.drawable.ic_gemini_live
    )

    val maxIconSize = scaleFactor * 42f
    val iconSize = (minOf(tileW, tileH) * 0.52f).coerceIn(scaleFactor * 14f, maxIconSize).toInt()

    for (row in 0..2) {
        for (col in 0..2) {
            val index = row * 3 + col
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
            canvas.drawRect(tileRect, tilePaint)

            if (index < googleIcons.size) {
                ContextCompat.getDrawable(context, googleIcons[index])?.mutate()?.apply {
                    setTint(accentColorInt)
                    val iconLeft = (tileRect.centerX() - iconSize / 2f).toInt()
                    val iconTop = (tileRect.centerY() - iconSize / 2f).toInt()
                    setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                    draw(canvas)
                }
            }
            canvas.restore()
        }
    }

    return bitmap
}

// 5. GOOGLE MEGA FOLDER (4x2: 5x2 / 2x5 Smart Pivot)
fun generateGoogleMegaFolder10Bitmap(
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

    val innerBgColor = if (isLight) Color.parseColor("#EAEAEF") else Color.parseColor("#161618")
    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = innerBgColor; style = Paint.Style.FILL }

    val iconDrawables = listOf(
        R.drawable.ic_google_logo,
        R.drawable.ic_youtube,
        R.drawable.ic_gmail,
        R.drawable.ic_drive,
        R.drawable.ic_goolge_photos,
        R.drawable.ic_maps,
        R.drawable.ic_calendar,
        R.drawable.ic_chrome,
        R.drawable.ic_playstore,
        R.drawable.ic_google_gemini
    )

    val maxIconSize = scaleFactor * 42f
    val iconSize = (minOf(tileW, tileH) * 0.44f).coerceIn(scaleFactor * 14f, maxIconSize).toInt()

    for (index in 0 until 10) {
        val col = index % cols
        val row = index / cols
        val left = innerCardRect.left + col * (tileW + spacing)
        val top = innerCardRect.top + row * (tileH + spacing)
        val tileRect = RectF(left, top, left + tileW, top + tileH)

        val tl = if (row == 0 && col == 0) 0f else innerCornerRadius
        val tr = if (row == 0 && col == cols - 1) 0f else innerCornerRadius
        val br = if (row == rows - 1 && col == cols - 1) 0f else innerCornerRadius
        val bl = if (row == rows - 1 && col == 0) 0f else innerCornerRadius

        val radii = floatArrayOf(tl, tl, tr, tr, br, br, bl, bl)
        val tilePath = Path().apply { addRoundRect(tileRect, radii, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(innerCardPath)
        canvas.clipPath(tilePath)
        canvas.drawRect(tileRect, innerPaint)

        ContextCompat.getDrawable(context, iconDrawables[index])?.mutate()?.apply {
            setTint(accentColorInt)
            val iconLeft = (tileRect.centerX() - iconSize / 2f).toInt()
            val iconTop = (tileRect.centerY() - iconSize / 2f).toInt()
            setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
            draw(canvas)
        }
        canvas.restore()
    }

    return bitmap
}

// 6. YOUTUBE & MEDIA DISCOVERY CAPSULE (3x1 / 4x1)
fun generateGoogleMediaCapsuleBitmap(
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

    // 1. Dual-Mode Geometry: 3.0f native aspect ratio for horizontal capsule
    val margin = scaleFactor * 1.5f
    val targetRatio = 3.0f
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

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val cardW = cardRect.width()
    val cardH = cardRect.height()
    val aspectRatio = cardW / cardH

    // 2. Aspect Ratio Branching: Reflow into 1x3 vertical column when squeezed thin
    val isVertical = isResponsive && aspectRatio < 0.85f
    val cols = if (isVertical) 1 else 3
    val rows = if (isVertical) 3 else 1

    val pad = (minOf(cardW, cardH) * 0.055f).coerceAtLeast(scaleFactor * 6f)
    val gap = (minOf(cardW, cardH) * 0.040f).coerceIn(scaleFactor * 5f, scaleFactor * 8f)

    val availableW = cardW - (pad * 2f) - (gap * (cols - 1))
    val availableH = cardH - (pad * 2f) - (gap * (rows - 1))

    val tileW = availableW / cols
    val tileH = availableH / rows

    val outerR = (cardCornerRadius - pad).coerceAtLeast(scaleFactor * 8f)
    val innerR = (scaleFactor * 7f).coerceAtMost(minOf(tileW, tileH) * 0.22f)

    val innerBgColor = if (isLight) Color.parseColor("#EAEAEF") else Color.parseColor("#161618")
    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = innerBgColor }

    // Center Discovery Tile Highlight (subtle luminous pulse plate)
    val centerTileBgColor = if (isLight) Color.parseColor("#E0E0E6") else Color.parseColor("#1E1E22")
    val centerTilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = centerTileBgColor }

    val iconDrawables = listOf(
        R.drawable.ic_youtube,
        R.drawable.ic_sound_search,
        R.drawable.ic_youtube_music
    )

    val baseIconSize = (minOf(tileW, tileH) * 0.44f).toInt().coerceAtLeast((scaleFactor * 16f).toInt())

    for (index in 0 until 3) {
        val col = if (isVertical) 0 else index
        val row = if (isVertical) index else 0

        val left = cardRect.left + pad + col * (tileW + gap)
        val top = cardRect.top + pad + row * (tileH + gap)
        val tileRect = RectF(left, top, left + tileW, top + tileH)

        // Asymmetric concentric radii mapping
        val radii = if (!isVertical) {
            when (index) {
                0 -> floatArrayOf(outerR, outerR, innerR, innerR, innerR, innerR, outerR, outerR)
                2 -> floatArrayOf(innerR, innerR, outerR, outerR, outerR, outerR, innerR, innerR)
                else -> floatArrayOf(innerR, innerR, innerR, innerR, innerR, innerR, innerR, innerR)
            }
        } else {
            when (index) {
                0 -> floatArrayOf(outerR, outerR, outerR, outerR, innerR, innerR, innerR, innerR)
                2 -> floatArrayOf(innerR, innerR, innerR, innerR, outerR, outerR, outerR, outerR)
                else -> floatArrayOf(innerR, innerR, innerR, innerR, innerR, innerR, innerR, innerR)
            }
        }

        val tilePath = Path().apply { addRoundRect(tileRect, radii, Path.Direction.CW) }
        canvas.drawPath(tilePath, if (index == 1) centerTilePaint else innerPaint)

        // Center Sound Search subtle boundary indicator
        if (index == 1) {
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(35, Color.red(accentColorInt), Color.green(accentColorInt), Color.blue(accentColorInt))
                style = Paint.Style.STROKE
                strokeWidth = scaleFactor * 1.2f
            }
            canvas.drawPath(tilePath, strokePaint)
        }

        val resId = iconDrawables[index]
        val iconSize = if (index == 1) (baseIconSize * 1.06f).toInt() else baseIconSize

        ContextCompat.getDrawable(context, resId)?.mutate()?.apply {
            setTint(accentColorInt)
            val iconLeft = (tileRect.centerX() - iconSize / 2f).toInt()
            val iconTop = (tileRect.centerY() - iconSize / 2f).toInt()
            setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
            draw(canvas)
        }
    }

    return bitmap
}

// 7. GOOGLE LIGHTBAR HORIZON (2x2 / Minimal Clean Horizon Arc)
fun generateGoogleLightbarBitmap(
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

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }

    val cardPath = Path().apply {
        addRoundRect(cardRect, cardCornerRadius, cardCornerRadius, Path.Direction.CW)
    }
    canvas.save()
    canvas.clipPath(cardPath)
    canvas.drawPath(cardPath, bgPaint)

    val cardW = cardRect.width()
    val cardH = cardRect.height()
    val cx = cardRect.centerX()

    // 2. Smooth Top Ambient Aura
    val glowAlpha = if (isLight) 38 else 68
    val auraColor = Color.argb(glowAlpha, Color.red(accentColorInt), Color.green(accentColorInt), Color.blue(accentColorInt))
    val midAuraColor = Color.argb((glowAlpha * 0.35f).toInt(), Color.red(accentColorInt), Color.green(accentColorInt), Color.blue(accentColorInt))

    val glowRadius = cardH * 0.58f
    val radialShader = RadialGradient(
        cx, cardRect.top, glowRadius,
        intArrayOf(auraColor, midAuraColor, Color.TRANSPARENT),
        floatArrayOf(0.0f, 0.45f, 1.0f),
        Shader.TileMode.CLAMP
    )
    val ambientGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = radialShader
        style = Paint.Style.FILL
    }
    canvas.drawRect(cardRect, ambientGlowPaint)

    // 3. Thick, Solid Rainbow Arc (No faux-glow stroke passes)
    val arcStartX = cardRect.left
    val arcEndX = cardRect.right
    val arcStartY = cardRect.top + (cardH * 0.65f)
    val arcDipY = cardRect.top + (cardH * 0.81f)

    val arcPath = Path().apply {
        moveTo(arcStartX, arcStartY)
        quadTo(cx, arcDipY, arcEndX, arcStartY)
    }

    val googleColors = intArrayOf(
        Color.parseColor("#4285F4"), // Blue
        Color.parseColor("#EA4335"), // Red
        Color.parseColor("#FBBC05"), // Yellow
        Color.parseColor("#34A853")  // Green
    )
    val colorPositions = floatArrayOf(0.0f, 0.33f, 0.67f, 1.0f)
    val arcShader = LinearGradient(
        arcStartX, arcStartY, arcEndX, arcStartY,
        googleColors, colorPositions, Shader.TileMode.CLAMP
    )

    // Single crisp, bolder stroke (increased from 1.8f to 3.8f)
    val solidArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = arcShader
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 3.8f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawPath(arcPath, solidArcPaint)
    canvas.restore()

    // 4. Hero Accent Sparkle
    val sparkleSize = (cardH * 0.32f).coerceIn(scaleFactor * 44f, scaleFactor * 68f)
    val iconCenterY = cardRect.top + (cardH * 0.42f)

    ContextCompat.getDrawable(context, R.drawable.ic_gemini)?.mutate()?.apply {
        setTint(accentColorInt)
        setBounds(
            (cx - sparkleSize / 2f).toInt(),
            (iconCenterY - sparkleSize / 2f).toInt(),
            (cx + sparkleSize / 2f).toInt(),
            (iconCenterY + sparkleSize / 2f).toInt()
        )
        draw(canvas)
    }

    return bitmap
}

// 8. GOOGLE LENS VIEWFINDER (2x2 / Tactical Camera Reticle - Icon Only)
fun generateGoogleLensViewfinderBitmap(
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

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val cardW = cardRect.width()
    val cardH = cardRect.height()
    val cx = cardRect.centerX()
    val cy = cardRect.centerY()
    val minDim = minOf(cardW, cardH)

    // 2. Corner Viewfinder Brackets (Anchored to minDim to prevent distortion on wide aspects)
    val cornerInset = (minDim * 0.13f).coerceIn(scaleFactor * 8f, scaleFactor * 20f)
    val left = cardRect.left + cornerInset
    val right = cardRect.right - cornerInset
    val top = cardRect.top + cornerInset
    val bottom = cardRect.bottom - cornerInset

    val availW = (right - left).coerceAtLeast(scaleFactor * 12f)
    val availH = (bottom - top).coerceAtLeast(scaleFactor * 12f)

    // Guard arm length so opposing brackets never overlap in narrow axes
    val bracketLength = (minDim * 0.22f)
        .coerceIn(scaleFactor * 10f, scaleFactor * 26f)
        .coerceAtMost(availH * 0.36f)
        .coerceAtMost(availW * 0.36f)

    // Radius is locked strictly smaller than arm length to prevent inverted bezier loops
    val bracketRadius = (bracketLength * 0.48f).coerceAtMost(cardCornerRadius * 0.5f)
    val strokeW = (scaleFactor * 3.4f).coerceAtMost(bracketRadius * 0.9f)

    val googleRed = Color.parseColor("#EA4335")
    val googleYellow = Color.parseColor("#FBBC05")
    val googleBlue = Color.parseColor("#4285F4")
    val googleGreen = Color.parseColor("#34A853")

    fun drawBracket(color: Int, block: Path.() -> Unit) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = Path().apply(block)
        canvas.drawPath(path, paint)
    }

    // Top-Left (Red)
    drawBracket(googleRed) {
        moveTo(left, top + bracketLength)
        lineTo(left, top + bracketRadius)
        quadTo(left, top, left + bracketRadius, top)
        lineTo(left + bracketLength, top)
    }

    // Top-Right (Yellow)
    drawBracket(googleYellow) {
        moveTo(right - bracketLength, top)
        lineTo(right - bracketRadius, top)
        quadTo(right, top, right, top + bracketRadius)
        lineTo(right, top + bracketLength)
    }

    // Bottom-Left (Blue)
    drawBracket(googleBlue) {
        moveTo(left, bottom - bracketLength)
        lineTo(left, bottom - bracketRadius)
        quadTo(left, bottom, left + bracketRadius, bottom)
        lineTo(left + bracketLength, bottom)
    }

    // Bottom-Right (Green)
    drawBracket(googleGreen) {
        moveTo(right - bracketLength, bottom)
        lineTo(right - bracketRadius, bottom)
        quadTo(right, bottom, right, bottom - bracketRadius)
        lineTo(right, bottom - bracketLength)
    }

    // 3. Hero Centered Google Lens Glyph
    val lensSize = (minDim * 0.28f).coerceIn(scaleFactor * 26f, scaleFactor * 48f)

    ContextCompat.getDrawable(context, R.drawable.ic_google_lens)?.mutate()?.apply {
        setTint(accentColorInt)
        setBounds(
            (cx - lensSize / 2f).toInt(),
            (cy - lensSize / 2f).toInt(),
            (cx + lensSize / 2f).toInt(),
            (cy + lensSize / 2f).toInt()
        )
        draw(canvas)
    }

    return bitmap
}

// 9. GOOGLE SEARCH & ACTION DOCK (3x2 / True Fixed Floating Dock)
fun generateGoogleSearchDockBitmap(
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

    // 1. True Fixed Aspect Anchor (Fixed 2.08 ratio)
    val baseContentW = 260f * scaleFactor
    val baseContentH = 125f * scaleFactor
    val contentRatio = baseContentW / baseContentH

    val bounds = run {
        var fitW = w
        var fitH = fitW / contentRatio
        if (fitH > h) {
            fitH = h
            fitW = fitH * contentRatio
        }
        val left = (w - fitW) / 2f
        val top = (h - fitH) / 2f
        RectF(left, top, left + fitW, top + fitH)
    }

    val scale = bounds.width() / baseContentW

    // 2. Theme Background Fill with Opacity
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }

    // 3. Search Pill
    val pillH = 50f * scaleFactor * scale
    val pillRect = RectF(bounds.left, bounds.top, bounds.right, bounds.top + pillH)
    val pillRadius = pillH / 2f
    canvas.drawRoundRect(pillRect, pillRadius, pillRadius, tilePaint)

    // Pill: Google 'G' Logo
    val logoSize = (pillH * 0.48f).toInt()
    val logoLeft = (pillRect.left + pillH * 0.32f).toInt()
    val logoTop = (pillRect.centerY() - logoSize / 2f).toInt()
    ContextCompat.getDrawable(context, R.drawable.ic_google_logo)?.mutate()?.apply {
        setBounds(logoLeft, logoTop, logoLeft + logoSize, logoTop + logoSize)
        draw(canvas)
    }

    // Pill: "Search Google" Prompt
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#66666E") else Color.parseColor("#9E9EA6")
        textSize = 15f * scaleFactor * scale
        typeface = getSlateFont(context, weight = 400)
        textAlign = Paint.Align.LEFT
    }
    val textX = logoLeft + logoSize + (14f * scaleFactor * scale)
    val fm = textPaint.fontMetrics
    val textY = pillRect.centerY() - (fm.ascent + fm.descent) / 2f
    canvas.drawText("Search Google", textX, textY, textPaint)

    // Pill: Mic Glyph (Right)
    val pillMicSize = (pillH * 0.48f).toInt()
    val pillMicRight = (pillRect.right - pillH * 0.34f).toInt()
    val pillMicTop = (pillRect.centerY() - pillMicSize / 2f).toInt()
    ContextCompat.getDrawable(context, R.drawable.ic_mic)?.mutate()?.apply {
        setTint(if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE)
        setBounds(pillMicRight - pillMicSize, pillMicTop, pillMicRight, pillMicTop + pillMicSize)
        draw(canvas)
    }

// 4. Bottom Action Discs (Gemini Live, Lens, Chrome)
    val discDiameter = 54f * scaleFactor * scale
    val discRadius = discDiameter / 2f
    val gapBetweenPillAndDiscs = 16f * scaleFactor * scale
    val discCenterY = pillRect.bottom + gapBetweenPillAndDiscs + discRadius

    val actionIcons = listOf(
        R.drawable.ic_gemini_live,
        R.drawable.ic_google_lens,
        R.drawable.ic_chrome
    )

    val discCenters = floatArrayOf(
        bounds.left + (bounds.width() * 0.20f),
        bounds.centerX(),
        bounds.right - (bounds.width() * 0.20f)
    )

    for (i in 0..2) {
        val cx = discCenters[i]
        canvas.drawCircle(cx, discCenterY, discRadius, tilePaint)

        val iconSize = (discDiameter * 0.54f).toInt()
        val iconLeft = (cx - iconSize / 2f).toInt()
        val iconTop = (discCenterY - iconSize / 2f).toInt()

        ContextCompat.getDrawable(context, actionIcons[i])?.mutate()?.apply {
            setTint(accentColorInt)
            setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
            draw(canvas)
        }
    }

    return bitmap
}

// 10. YOUTUBE "VINYL & VIEWFINDER" DUAL-DIAL (2x2)
fun generateYouTubeVinylViewfinderBitmap(
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

    // 1. Base Slate Plate
    val margin = scaleFactor * 1.5f
    val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
    val leftX = if (isResponsive) margin else (w - cardSize) / 2f
    val topY = if (isResponsive) margin else (h - cardSize) / 2f
    val cardW = if (isResponsive) w - (margin * 2f) else cardSize
    val cardH = if (isResponsive) h - (margin * 2f) else cardSize
    val cardRect = RectF(leftX, topY, leftX + cardW, topY + cardH)

    val effectiveDim = minOf(cardW, cardH)
    val uiScale = (effectiveDim / (160f * scaleFactor)).coerceIn(0.5f, 3.0f)

    // Restored original corner radius calculation
    val maxCardRadius = effectiveDim / 2f
    val cardCornerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(maxCardRadius)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = effectiveDim * 0.055f
    val gap = effectiveDim * 0.04f
    val halfW = (cardW - (pad * 2f) - gap) / 2f
    val halfH = (cardH - (pad * 2f) - gap) / 2f

    // Quadrant Bounds
    val qTopLeft = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.left + pad + halfW, cardRect.top + pad + halfH)
    val qTopRight = RectF(cardRect.left + pad + halfW + gap, cardRect.top + pad, cardRect.right - pad, cardRect.top + pad + halfH)
    val qBottomLeft = RectF(cardRect.left + pad, cardRect.top + pad + halfH + gap, cardRect.left + pad + halfW, cardRect.bottom - pad)
    val qBottomRight = RectF(cardRect.left + pad + halfW + gap, cardRect.top + pad + halfH + gap, cardRect.right - pad, cardRect.bottom - pad)

    // Restored original tile radii
    val vfOuterRad = (cardCornerRadius - pad).coerceAtLeast(scaleFactor * 8f)
    val vfInnerRad = scaleFactor * 8f

    val tileBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(18, 0, 0, 0) else Color.argb(32, 255, 255, 255)
        style = Paint.Style.FILL
    }

    // 2. Quadrant Tile Backgrounds
    val vfRadii = floatArrayOf(vfOuterRad, vfOuterRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad)
    canvas.drawPath(Path().apply { addRoundRect(qTopLeft, vfRadii, Path.Direction.CW) }, tileBgPaint)

    val shortsRadii = floatArrayOf(vfInnerRad, vfInnerRad, vfOuterRad, vfOuterRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad)
    canvas.drawPath(Path().apply { addRoundRect(qTopRight, shortsRadii, Path.Direction.CW) }, tileBgPaint)

    val blRadii = floatArrayOf(vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfOuterRad, vfOuterRad)
    canvas.drawPath(Path().apply { addRoundRect(qBottomLeft, blRadii, Path.Direction.CW) }, tileBgPaint)

    // 3. Fluid Connecting Cable
    val ytIconMaxDim = minOf(halfW, halfH) * 0.56f
    val ytIconStartX = qTopLeft.centerX() + (ytIconMaxDim * 0.36f)
    val ytIconStartY = qTopLeft.centerY()

    val vinylCx = qBottomRight.centerX()
    val vinylCy = qBottomRight.centerY()

    val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(55, 0, 0, 0) else Color.argb(78, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1.4f * uiScale
        strokeCap = Paint.Cap.ROUND
    }

    val orbitalPath = Path().apply {
        moveTo(ytIconStartX, ytIconStartY)
        cubicTo(
            qTopRight.centerX(), ytIconStartY,
            qBottomLeft.centerX(), vinylCy,
            vinylCx, vinylCy
        )
    }
    canvas.drawPath(orbitalPath, guidePaint)

    fun drawVector(resId: Int, cx: Float, cy: Float, maxDim: Float, tint: Int? = null) {
        val drawable = ContextCompat.getDrawable(context, resId)?.mutate() ?: return
        if (tint != null) {
            drawable.setTint(tint)
        }
        val intrinsicW = drawable.intrinsicWidth.toFloat()
        val intrinsicH = drawable.intrinsicHeight.toFloat()
        var drawW = maxDim
        var drawH = maxDim
        if (intrinsicW > 0f && intrinsicH > 0f) {
            val aspect = intrinsicW / intrinsicH
            if (aspect > 1f) {
                drawH = maxDim / aspect
            } else {
                drawW = maxDim * aspect
            }
        }
        val l = (cx - drawW / 2f).toInt()
        val t = (cy - drawH / 2f).toInt()
        val r = (cx + drawW / 2f).toInt()
        val b = (cy + drawH / 2f).toInt()
        drawable.setBounds(l, t, r, b)
        drawable.draw(canvas)
    }

    // 4. TOP-LEFT: YouTube Icon
    drawVector(
        resId = R.drawable.ic_youtube,
        cx = qTopLeft.centerX(),
        cy = qTopLeft.centerY(),
        maxDim = ytIconMaxDim,
        tint = accentColorInt
    )

    // 5. Shared Typography
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
        textSize = scaleFactor * 12f * uiScale
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    // 6. TOP-RIGHT: Shorts Capsule
    val topTextBaselineY = qTopRight.bottom - (scaleFactor * 9f * uiScale)
    val iconTopCenterY = (qTopRight.top + topTextBaselineY - (scaleFactor * 12f * uiScale)) / 2f + (scaleFactor * 2f * uiScale)
    drawVector(
        resId = R.drawable.ic_youtube_shorts,
        cx = qTopRight.centerX(),
        cy = iconTopCenterY,
        maxDim = minOf(halfW, halfH) * 0.44f,
        tint = accentColorInt
    )
    canvas.drawText("Shorts", qTopRight.centerX(), topTextBaselineY, labelPaint)

    // 7. BOTTOM-LEFT: Liked Music / Mix Capsule
    val botTextBaselineY = qBottomLeft.bottom - (scaleFactor * 9f * uiScale)
    val iconBotCenterY = (qBottomLeft.top + botTextBaselineY - (scaleFactor * 12f * uiScale)) / 2f + (scaleFactor * 2f * uiScale)
    drawVector(
        resId = R.drawable.ic_soundwave,
        cx = qBottomLeft.centerX(),
        cy = iconBotCenterY,
        maxDim = minOf(halfW, halfH) * 0.42f,
        tint = accentColorInt
    )
    canvas.drawText("Mix", qBottomLeft.centerX(), botTextBaselineY, labelPaint)

    // 8. BOTTOM-RIGHT: Vinyl Record
    val vinylRadius = (minOf(halfW, halfH) * 0.96f) / 2f
    val vinylBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#1C1C1E") else Color.parseColor("#0D0D0E")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(vinylCx, vinylCy, vinylRadius, vinylBodyPaint)

    val groovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(45, 255, 255, 255) else Color.argb(35, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.7f * uiScale
    }
    canvas.drawCircle(vinylCx, vinylCy, vinylRadius * 0.82f, groovePaint)
    canvas.drawCircle(vinylCx, vinylCy, vinylRadius * 0.66f, groovePaint)
    canvas.drawCircle(vinylCx, vinylCy, vinylRadius * 0.50f, groovePaint)

    val spindleRadius = vinylRadius * 0.42f
    val labelDiscPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawCircle(vinylCx, vinylCy, spindleRadius, labelDiscPaint)

    val spindleIconColor = if (isLight) Color.WHITE else Color.BLACK
    drawVector(
        resId = R.drawable.ic_youtube_music,
        cx = vinylCx,
        cy = vinylCy,
        maxDim = spindleRadius * 1.30f,
        tint = spindleIconColor
    )

    return bitmap
}

// 11. GOOGLE MAPS "COMPASS & WAYPOINT" DUAL-DIAL (2x2)
fun generateGoogleMapsCompassBitmap(
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

    // 1. Base Slate Plate
    val margin = scaleFactor * 1.5f
    val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
    val leftX = if (isResponsive) margin else (w - cardSize) / 2f
    val topY = if (isResponsive) margin else (h - cardSize) / 2f
    val cardW = if (isResponsive) w - (margin * 2f) else cardSize
    val cardH = if (isResponsive) h - (margin * 2f) else cardSize
    val cardRect = RectF(leftX, topY, leftX + cardW, topY + cardH)

    val effectiveDim = minOf(cardW, cardH)
    val uiScale = (effectiveDim / (160f * scaleFactor)).coerceIn(0.5f, 3.0f)

    // Restored original corner radius calculation
    val maxCardRadius = effectiveDim / 2f
    val cardCornerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(maxCardRadius)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = effectiveDim * 0.055f
    val gap = effectiveDim * 0.04f
    val halfW = (cardW - (pad * 2f) - gap) / 2f
    val halfH = (cardH - (pad * 2f) - gap) / 2f

    // Quadrant Bounds
    val qTopLeft = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.left + pad + halfW, cardRect.top + pad + halfH)
    val qTopRight = RectF(cardRect.left + pad + halfW + gap, cardRect.top + pad, cardRect.right - pad, cardRect.top + pad + halfH)
    val qBottomLeft = RectF(cardRect.left + pad, cardRect.top + pad + halfH + gap, cardRect.left + pad + halfW, cardRect.bottom - pad)
    val qBottomRight = RectF(cardRect.left + pad + halfW + gap, cardRect.top + pad + halfH + gap, cardRect.right - pad, cardRect.bottom - pad)

    // Restored original tile radii
    val vfOuterRad = (cardCornerRadius - pad).coerceAtLeast(scaleFactor * 8f)
    val vfInnerRad = scaleFactor * 8f

    val tileBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(18, 0, 0, 0) else Color.argb(32, 255, 255, 255)
        style = Paint.Style.FILL
    }

    // 2. Quadrant Tile Backgrounds
    val tlRadii = floatArrayOf(vfOuterRad, vfOuterRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad)
    canvas.drawPath(Path().apply { addRoundRect(qTopLeft, tlRadii, Path.Direction.CW) }, tileBgPaint)

    val trRadii = floatArrayOf(vfInnerRad, vfInnerRad, vfOuterRad, vfOuterRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad)
    canvas.drawPath(Path().apply { addRoundRect(qTopRight, trRadii, Path.Direction.CW) }, tileBgPaint)

    val blRadii = floatArrayOf(vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfOuterRad, vfOuterRad)
    canvas.drawPath(Path().apply { addRoundRect(qBottomLeft, blRadii, Path.Direction.CW) }, tileBgPaint)

    fun drawVector(resId: Int, cx: Float, cy: Float, maxDim: Float, tint: Int? = null) {
        val drawable = ContextCompat.getDrawable(context, resId)?.mutate() ?: return
        if (tint != null) {
            drawable.setTint(tint)
        }
        val intrinsicW = drawable.intrinsicWidth.toFloat()
        val intrinsicH = drawable.intrinsicHeight.toFloat()
        var drawW = maxDim
        var drawH = maxDim
        if (intrinsicW > 0f && intrinsicH > 0f) {
            val aspect = intrinsicW / intrinsicH
            if (aspect > 1f) {
                drawH = maxDim / aspect
            } else {
                drawW = maxDim * aspect
            }
        }
        val l = (cx - drawW / 2f).toInt()
        val t = (cy - drawH / 2f).toInt()
        val r = (cx + drawW / 2f).toInt()
        val b = (cy + drawH / 2f).toInt()
        drawable.setBounds(l, t, r, b)
        drawable.draw(canvas)
    }

    // 3. Telemetry Wire
    val pinSize = minOf(halfW, halfH) * 0.48f
    val pinHeadRadius = pinSize * 0.36f
    val pinTipX = qTopLeft.centerX()
    val pinTipY = qTopLeft.centerY() + (pinSize * 0.45f)

    val compassRadius = (minOf(halfW, halfH) * 0.96f) / 2f
    val compassCx = qBottomRight.centerX()
    val compassCy = qBottomRight.centerY()

    val wirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(55, 0, 0, 0) else Color.argb(78, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1.4f * uiScale
        strokeCap = Paint.Cap.ROUND
    }

    val telemetryPath = Path().apply {
        moveTo(pinTipX, pinTipY)
        cubicTo(
            qTopRight.centerX(), pinTipY,
            qBottomLeft.centerX(), compassCy,
            compassCx - (compassRadius * 0.4f), compassCy
        )
    }
    canvas.drawPath(telemetryPath, wirePaint)

    // 4. TOP-LEFT: Google Maps Pin
    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    val pinPath = Path().apply {
        val headCy = qTopLeft.centerY() - (pinSize * 0.12f)
        arcTo(RectF(pinTipX - pinHeadRadius, headCy - pinHeadRadius, pinTipX + pinHeadRadius, headCy + pinHeadRadius), 140f, 260f, false)
        lineTo(pinTipX, pinTipY)
        close()
    }
    canvas.drawPath(pinPath, pinPaint)

    val pinHolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(220, 242, 242, 247) else Color.parseColor("#1C1C1E")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(qTopLeft.centerX(), qTopLeft.centerY() - (pinSize * 0.12f), pinSize * 0.15f, pinHolePaint)

    // 5. Shared Typography
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
        textSize = scaleFactor * 12f * uiScale
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    // 6. TOP-RIGHT: Commute
    val topTextBaselineY = qTopRight.bottom - (scaleFactor * 9f * uiScale)
    val iconTopCenterY = (qTopRight.top + topTextBaselineY - (scaleFactor * 12f * uiScale)) / 2f + (scaleFactor * 2f * uiScale)

    drawVector(
        resId = R.drawable.ic_arrow_turn_right,
        cx = qTopRight.centerX(),
        cy = iconTopCenterY,
        maxDim = minOf(halfW, halfH) * 0.38f,
        tint = accentColorInt
    )
    canvas.drawText("Commute", qTopRight.centerX(), topTextBaselineY, labelPaint)

    // 7. BOTTOM-LEFT: Home
    val botTextBaselineY = qBottomLeft.bottom - (scaleFactor * 9f * uiScale)
    val iconBotCenterY = (qBottomLeft.top + botTextBaselineY - (scaleFactor * 12f * uiScale)) / 2f + (scaleFactor * 2f * uiScale)

    drawVector(
        resId = R.drawable.ic_home,
        cx = qBottomLeft.centerX(),
        cy = iconBotCenterY,
        maxDim = minOf(halfW, halfH) * 0.38f,
        tint = accentColorInt
    )
    canvas.drawText("Home", qBottomLeft.centerX(), botTextBaselineY, labelPaint)

    // 8. BOTTOM-RIGHT: Compass Dial
    val compassBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#1C1C1E") else Color.parseColor("#0D0D0E")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(compassCx, compassCy, compassRadius, compassBodyPaint)

    val bezelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(45, 255, 255, 255) else Color.argb(35, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.8f * uiScale
    }
    canvas.drawCircle(compassCx, compassCy, compassRadius * 0.88f, bezelPaint)

    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(70, 255, 255, 255) else Color.argb(55, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1.0f * uiScale
        strokeCap = Paint.Cap.ROUND
    }
    val tickStart = compassRadius * 0.80f
    val tickEnd = compassRadius * 0.86f
    val cardTickStart = compassRadius * 0.74f

    for (i in 0 until 12) {
        val angleDeg = i * 30.0
        val rad = Math.toRadians(angleDeg)
        val isCardinal = i % 3 == 0
        val sR = if (isCardinal) cardTickStart else tickStart
        val x1 = compassCx + (sR * Math.cos(rad)).toFloat()
        val y1 = compassCy + (sR * Math.sin(rad)).toFloat()
        val x2 = compassCx + (tickEnd * Math.cos(rad)).toFloat()
        val y2 = compassCy + (tickEnd * Math.sin(rad)).toFloat()
        canvas.drawLine(x1, y1, x2, y2, tickPaint)
    }

    val cardinalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = scaleFactor * 7.5f * uiScale
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("N", compassCx, compassCy - (compassRadius * 0.60f), cardinalPaint)

    canvas.save()
    canvas.rotate(-32f, compassCx, compassCy)

    val needleLen = compassRadius * 0.68f
    val needleWidth = compassRadius * 0.16f

    val northNeedlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    val northPath = Path().apply {
        moveTo(compassCx, compassCy - needleLen)
        lineTo(compassCx + needleWidth / 2f, compassCy)
        lineTo(compassCx, compassCy - (needleLen * 0.18f))
        close()
    }
    canvas.drawPath(northPath, northNeedlePaint)

    val northFacetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, Color.red(accentColorInt), Color.green(accentColorInt), Color.blue(accentColorInt))
        style = Paint.Style.FILL
    }
    val northFacetPath = Path().apply {
        moveTo(compassCx, compassCy - needleLen)
        lineTo(compassCx - needleWidth / 2f, compassCy)
        lineTo(compassCx, compassCy - (needleLen * 0.18f))
        close()
    }
    canvas.drawPath(northFacetPath, northFacetPaint)

    val southNeedlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#E5E5EA") else Color.parseColor("#8E8E93")
        style = Paint.Style.FILL
    }
    val southPath = Path().apply {
        moveTo(compassCx, compassCy + needleLen)
        lineTo(compassCx + needleWidth / 2f, compassCy)
        lineTo(compassCx, compassCy + (needleLen * 0.18f))
        close()
    }
    canvas.drawPath(southPath, southNeedlePaint)

    val southFacetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#C7C7CC") else Color.parseColor("#636366")
        style = Paint.Style.FILL
    }
    val southFacetPath = Path().apply {
        moveTo(compassCx, compassCy + needleLen)
        lineTo(compassCx - needleWidth / 2f, compassCy)
        lineTo(compassCx, compassCy + (needleLen * 0.18f))
        close()
    }
    canvas.drawPath(southFacetPath, southFacetPaint)

    canvas.restore()

    val pivotRadius = compassRadius * 0.15f
    val pivotCapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.WHITE else Color.parseColor("#1C1C1E")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(compassCx, compassCy, pivotRadius, pivotCapPaint)

    val pivotDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawCircle(compassCx, compassCy, pivotRadius * 0.45f, pivotDotPaint)

    return bitmap
}

// 12. WORKSPACE "CHRONOMETER & COCKPIT" DUAL-DIAL (2x2)
fun generateGoogleWorkspaceChronoBitmap(
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

    // 1. Base Slate Plate
    val margin = scaleFactor * 1.5f
    val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
    val leftX = if (isResponsive) margin else (w - cardSize) / 2f
    val topY = if (isResponsive) margin else (h - cardSize) / 2f
    val cardW = if (isResponsive) w - (margin * 2f) else cardSize
    val cardH = if (isResponsive) h - (margin * 2f) else cardSize
    val cardRect = RectF(leftX, topY, leftX + cardW, topY + cardH)

    val effectiveDim = minOf(cardW, cardH)
    val uiScale = (effectiveDim / (160f * scaleFactor)).coerceIn(0.5f, 3.0f)

    // Restored original corner radius calculation
    val maxCardRadius = effectiveDim / 2f
    val cardCornerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(maxCardRadius)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = effectiveDim * 0.055f
    val gap = effectiveDim * 0.04f
    val halfW = (cardW - (pad * 2f) - gap) / 2f
    val halfH = (cardH - (pad * 2f) - gap) / 2f

    // Quadrant Bounds
    val qTopLeft = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.left + pad + halfW, cardRect.top + pad + halfH)
    val qTopRight = RectF(cardRect.left + pad + halfW + gap, cardRect.top + pad, cardRect.right - pad, cardRect.top + pad + halfH)
    val qBottomLeft = RectF(cardRect.left + pad, cardRect.top + pad + halfH + gap, cardRect.left + pad + halfW, cardRect.bottom - pad)
    val qBottomRight = RectF(cardRect.left + pad + halfW + gap, cardRect.top + pad + halfH + gap, cardRect.right - pad, cardRect.bottom - pad)

    // Restored original tile radii
    val vfOuterRad = (cardCornerRadius - pad).coerceAtLeast(scaleFactor * 8f)
    val vfInnerRad = scaleFactor * 8f

    val tileBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(18, 0, 0, 0) else Color.argb(32, 255, 255, 255)
        style = Paint.Style.FILL
    }

    // 2. Quadrant Tile Backgrounds
    val tlRadii = floatArrayOf(vfOuterRad, vfOuterRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad)
    canvas.drawPath(Path().apply { addRoundRect(qTopLeft, tlRadii, Path.Direction.CW) }, tileBgPaint)

    val trRadii = floatArrayOf(vfInnerRad, vfInnerRad, vfOuterRad, vfOuterRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad)
    canvas.drawPath(Path().apply { addRoundRect(qTopRight, trRadii, Path.Direction.CW) }, tileBgPaint)

    val blRadii = floatArrayOf(vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfOuterRad, vfOuterRad)
    canvas.drawPath(Path().apply { addRoundRect(qBottomLeft, blRadii, Path.Direction.CW) }, tileBgPaint)

    fun drawVector(resId: Int, cx: Float, cy: Float, maxDim: Float, tint: Int? = null) {
        val drawable = ContextCompat.getDrawable(context, resId)?.mutate() ?: return
        if (tint != null) {
            drawable.setTint(tint)
        }
        val intrinsicW = drawable.intrinsicWidth.toFloat()
        val intrinsicH = drawable.intrinsicHeight.toFloat()
        var drawW = maxDim
        var drawH = maxDim
        if (intrinsicW > 0f && intrinsicH > 0f) {
            val aspect = intrinsicW / intrinsicH
            if (aspect > 1f) {
                drawH = maxDim / aspect
            } else {
                drawW = maxDim * aspect
            }
        }
        val l = (cx - drawW / 2f).toInt()
        val t = (cy - drawH / 2f).toInt()
        val r = (cx + drawW / 2f).toInt()
        val b = (cy + drawH / 2f).toInt()
        drawable.setBounds(l, t, r, b)
        drawable.draw(canvas)
    }

    // Shared typography
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
        textSize = scaleFactor * 12f * uiScale
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    // 3. TOP-LEFT: Google Calendar Tile (Slot 0)
    val cal = java.util.Calendar.getInstance()
    val dayOfWeekStr = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(cal.time).uppercase()
    val dayOfMonthStr = java.text.SimpleDateFormat("dd", java.util.Locale.getDefault()).format(cal.time)

    val calSubLabelY = qTopLeft.bottom - (scaleFactor * 9f * uiScale)
    val calCenterY = (qTopLeft.top + calSubLabelY - (scaleFactor * 12f * uiScale)) / 2f

    val calHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = scaleFactor * 11.5f * uiScale
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
    }
    val calDayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
        textSize = scaleFactor * 26f * uiScale
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
    }

    canvas.drawText(dayOfWeekStr, qTopLeft.centerX(), calCenterY - (scaleFactor * 4f * uiScale), calHeaderPaint)
    canvas.drawText(dayOfMonthStr, qTopLeft.centerX(), calCenterY + (scaleFactor * 18f * uiScale), calDayPaint)
    canvas.drawText("Calendar", qTopLeft.centerX(), calSubLabelY, labelPaint)

    // 4. TOP-RIGHT: Gmail Tile (Slot 1)
    val gmailSubLabelY = qTopRight.bottom - (scaleFactor * 9f * uiScale)
    val gmailCenterY = (qTopRight.top + gmailSubLabelY - (scaleFactor * 12f * uiScale)) / 2f + (scaleFactor * 2f * uiScale)

    drawVector(
        resId = R.drawable.ic_gmail,
        cx = qTopRight.centerX(),
        cy = gmailCenterY,
        maxDim = minOf(halfW, halfH) * 0.46f,
        tint = accentColorInt
    )
    canvas.drawText("Gmail", qTopRight.centerX(), gmailSubLabelY, labelPaint)

    // 5. BOTTOM-LEFT: Google Meet Tile (Slot 2)
    val botTextBaselineY = qBottomLeft.bottom - (scaleFactor * 9f * uiScale)
    val iconBotCenterY = (qBottomLeft.top + botTextBaselineY - (scaleFactor * 12f * uiScale)) / 2f + (scaleFactor * 2f * uiScale)

    drawVector(
        resId = R.drawable.ic_google_meet,
        cx = qBottomLeft.centerX(),
        cy = iconBotCenterY,
        maxDim = minOf(halfW, halfH) * 0.46f,
        tint = accentColorInt
    )
    canvas.drawText("Meet", qBottomLeft.centerX(), botTextBaselineY, labelPaint)

    // 6. BOTTOM-RIGHT: Cockpit 24-Hour Chronometer Dial (Slot 3)
    val chronoRadius = (minOf(halfW, halfH) * 0.96f) / 2f
    val chronoCx = qBottomRight.centerX()
    val chronoCy = qBottomRight.centerY()

    val chronoBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#1C1C1E") else Color.parseColor("#0D0D0E")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(chronoCx, chronoCy, chronoRadius, chronoBodyPaint)

    val bezelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(45, 255, 255, 255) else Color.argb(35, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.8f * uiScale
    }
    canvas.drawCircle(chronoCx, chronoCy, chronoRadius * 0.88f, bezelPaint)

    val indexPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = scaleFactor * 9.5f * uiScale
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
    }
    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(80, 255, 255, 255) else Color.argb(60, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1.0f * uiScale
        strokeCap = Paint.Cap.ROUND
    }

    val hourTexts = mapOf(
        0 to "00",
        6 to "06",
        12 to "12",
        18 to "18"
    )

    for (hStep in 0 until 24) {
        val angleDeg = (hStep * 15.0) - 90.0
        val rad = Math.toRadians(angleDeg)
        if (hStep % 6 == 0) {
            val textR = chronoRadius * 0.65f
            val tx = chronoCx + (textR * Math.cos(rad)).toFloat()
            val ty = chronoCy + (textR * Math.sin(rad)).toFloat() + (scaleFactor * 3.2f * uiScale)
            canvas.drawText(hourTexts[hStep] ?: "", tx, ty, indexPaint)
        } else if (hStep % 2 == 0) {
            val rStart = chronoRadius * 0.78f
            val rEnd = chronoRadius * 0.86f
            val x1 = chronoCx + (rStart * Math.cos(rad)).toFloat()
            val y1 = chronoCy + (rStart * Math.sin(rad)).toFloat()
            val x2 = chronoCx + (rEnd * Math.cos(rad)).toFloat()
            val y2 = chronoCy + (rEnd * Math.sin(rad)).toFloat()
            canvas.drawLine(x1, y1, x2, y2, tickPaint)
        }
    }

    // Dynamic 24-Hour Indicator Hand
    val hourOfDay = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val minuteOfHour = cal.get(java.util.Calendar.MINUTE)
    val currentDayFraction = (hourOfDay + (minuteOfHour / 60f)) / 24f
    val handAngleDeg = (currentDayFraction * 360f) - 90f

    canvas.save()
    canvas.rotate(handAngleDeg, chronoCx, chronoCy)

    val handLen = chronoRadius * 0.58f
    val handWidth = chronoRadius * 0.12f

    val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    val chronoHandPath = Path().apply {
        moveTo(chronoCx + handLen, chronoCy)
        lineTo(chronoCx, chronoCy - handWidth / 2f)
        lineTo(chronoCx - (handLen * 0.22f), chronoCy)
        lineTo(chronoCx, chronoCy + handWidth / 2f)
        close()
    }
    canvas.drawPath(chronoHandPath, handPaint)

    val counterPivotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.WHITE else Color.parseColor("#1C1C1E")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(chronoCx, chronoCy, chronoRadius * 0.14f, counterPivotPaint)

    val centerPinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawCircle(chronoCx, chronoCy, chronoRadius * 0.06f, centerPinPaint)

    canvas.restore()

    return bitmap
}

// 13. GOOGLE DRIVE & KEEP (2x2)
fun generateGoogleDriveTapeReelBitmap(
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

    // 1. Base Slate Plate
    val margin = scaleFactor * 1.5f
    val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
    val leftX = if (isResponsive) margin else (w - cardSize) / 2f
    val topY = if (isResponsive) margin else (h - cardSize) / 2f
    val cardW = if (isResponsive) w - (margin * 2f) else cardSize
    val cardH = if (isResponsive) h - (margin * 2f) else cardSize
    val cardRect = RectF(leftX, topY, leftX + cardW, topY + cardH)

    val effectiveDim = minOf(cardW, cardH)
    val uiScale = (effectiveDim / (160f * scaleFactor)).coerceIn(0.5f, 3.0f)

    val maxCardRadius = effectiveDim / 2f
    val cardCornerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(maxCardRadius)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = effectiveDim * 0.055f
    val gap = effectiveDim * 0.04f
    val halfW = (cardW - (pad * 2f) - gap) / 2f
    val halfH = (cardH - (pad * 2f) - gap) / 2f

    // Quadrant Bounds
    val qTopLeft = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.left + pad + halfW, cardRect.top + pad + halfH)
    val qTopRight = RectF(cardRect.left + pad + halfW + gap, cardRect.top + pad, cardRect.right - pad, cardRect.top + pad + halfH)
    val qBottomLeft = RectF(cardRect.left + pad, cardRect.top + pad + halfH + gap, cardRect.left + pad + halfW, cardRect.bottom - pad)
    val qBottomRight = RectF(cardRect.left + pad + halfW + gap, cardRect.top + pad + halfH + gap, cardRect.right - pad, cardRect.bottom - pad)

    val vfOuterRad = (cardCornerRadius - pad).coerceAtLeast(scaleFactor * 8f)
    val vfInnerRad = scaleFactor * 8f

    val tileBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(18, 0, 0, 0) else Color.argb(32, 255, 255, 255)
        style = Paint.Style.FILL
    }

    // 2. Quadrant Tile Backgrounds (Slots 0, 1, 2)
    val tlRadii = floatArrayOf(vfOuterRad, vfOuterRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad)
    canvas.drawPath(Path().apply { addRoundRect(qTopLeft, tlRadii, Path.Direction.CW) }, tileBgPaint)

    val trRadii = floatArrayOf(vfInnerRad, vfInnerRad, vfOuterRad, vfOuterRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad)
    canvas.drawPath(Path().apply { addRoundRect(qTopRight, trRadii, Path.Direction.CW) }, tileBgPaint)

    val blRadii = floatArrayOf(vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfOuterRad, vfOuterRad)
    canvas.drawPath(Path().apply { addRoundRect(qBottomLeft, blRadii, Path.Direction.CW) }, tileBgPaint)

    fun drawVector(resId: Int, cx: Float, cy: Float, maxDim: Float, tint: Int? = null) {
        val drawable = ContextCompat.getDrawable(context, resId)?.mutate() ?: return
        if (tint != null) {
            drawable.setTint(tint)
        }
        val intrinsicW = drawable.intrinsicWidth.toFloat()
        val intrinsicH = drawable.intrinsicHeight.toFloat()
        var drawW = maxDim
        var drawH = maxDim
        if (intrinsicW > 0f && intrinsicH > 0f) {
            val aspect = intrinsicW / intrinsicH
            if (aspect > 1f) {
                drawH = maxDim / aspect
            } else {
                drawW = maxDim * aspect
            }
        }
        val l = (cx - drawW / 2f).toInt()
        val t = (cy - drawH / 2f).toInt()
        val r = (cx + drawW / 2f).toInt()
        val b = (cy + drawH / 2f).toInt()
        drawable.setBounds(l, t, r, b)
        drawable.draw(canvas)
    }

    // Shared typography
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
        textSize = scaleFactor * 12f * uiScale
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    // 3. TOP-LEFT: Google Drive (Slot 0)
    val driveSubLabelY = qTopLeft.bottom - (scaleFactor * 9f * uiScale)
    val driveCenterY = (qTopLeft.top + driveSubLabelY - (scaleFactor * 12f * uiScale)) / 2f + (scaleFactor * 2f * uiScale)

    drawVector(
        resId = R.drawable.ic_drive,
        cx = qTopLeft.centerX(),
        cy = driveCenterY,
        maxDim = minOf(halfW, halfH) * 0.46f,
        tint = accentColorInt
    )
    canvas.drawText("Drive", qTopLeft.centerX(), driveSubLabelY, labelPaint)

    // 4. TOP-RIGHT: Google Docs (Slot 1)
    val docsSubLabelY = qTopRight.bottom - (scaleFactor * 9f * uiScale)
    val docsCenterY = (qTopRight.top + docsSubLabelY - (scaleFactor * 12f * uiScale)) / 2f + (scaleFactor * 2f * uiScale)

    drawVector(
        resId = R.drawable.ic_google_docs,
        cx = qTopRight.centerX(),
        cy = docsCenterY,
        maxDim = minOf(halfW, halfH) * 0.46f,
        tint = accentColorInt
    )
    canvas.drawText("Docs", qTopRight.centerX(), docsSubLabelY, labelPaint)

    // 5. BOTTOM-LEFT: Google Sheets (Slot 2)
    val sheetsSubLabelY = qBottomLeft.bottom - (scaleFactor * 9f * uiScale)
    val sheetsCenterY = (qBottomLeft.top + sheetsSubLabelY - (scaleFactor * 12f * uiScale)) / 2f + (scaleFactor * 2f * uiScale)

    drawVector(
        resId = R.drawable.ic_google_sheets,
        cx = qBottomLeft.centerX(),
        cy = sheetsCenterY,
        maxDim = minOf(halfW, halfH) * 0.46f,
        tint = accentColorInt
    )
    canvas.drawText("Sheets", qBottomLeft.centerX(), sheetsSubLabelY, labelPaint)

    // 6. BOTTOM-RIGHT: Google Keep Bulb (Slot 3 - Scaled to Block Height)
    val kCx = qBottomRight.centerX()
    val kCy = qBottomRight.centerY()

// Match the full vertical height of the neighboring tile block
    val bulbH = halfH * 0.8f
    val bulbW = bulbH * 0.58f

    val keepDrawableId = context.resources.getIdentifier("ic_google_keep", "drawable", context.packageName).takeIf { it != 0 }
        ?: context.resources.getIdentifier("ic_keep", "drawable", context.packageName).takeIf { it != 0 }

    if (keepDrawableId != null) {
        drawVector(
            resId = keepDrawableId,
            cx = kCx,
            cy = kCy,
            maxDim = bulbH,
            tint = accentColorInt
        )
    } else {
        val domeR = bulbW / 2f
        val domeCy = (kCy - bulbH / 2f) + domeR
        val neckW = bulbW * 0.44f
        val neckBottom = domeCy + (domeR * 1.15f)

        val bulbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }

        // Glass Envelope Dome & Taper
        val bulbPath = Path().apply {
            arcTo(
                RectF(kCx - domeR, domeCy - domeR, kCx + domeR, domeCy + domeR),
                140f,
                260f,
                false
            )
            lineTo(kCx + neckW / 2f, neckBottom)
            lineTo(kCx - neckW / 2f, neckBottom)
            close()
        }
        canvas.drawPath(bulbPath, bulbPaint)

        // Base Screw Threads
        val threadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.STROKE
            strokeWidth = scaleFactor * 2.8f * uiScale
            strokeCap = Paint.Cap.ROUND
        }
        val spacing = scaleFactor * 5.0f * uiScale
        val t1Y = neckBottom + spacing
        val t2Y = t1Y + spacing
        val t3Y = t2Y + spacing

        canvas.drawLine(kCx - neckW * 0.42f, t1Y, kCx + neckW * 0.42f, t1Y, threadPaint)
        canvas.drawLine(kCx - neckW * 0.32f, t2Y, kCx + neckW * 0.32f, t2Y, threadPaint)
        canvas.drawLine(kCx - neckW * 0.18f, t3Y, kCx + neckW * 0.18f, t3Y, threadPaint)

        // Inner Filament Cutout
        val cutoutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.WHITE else Color.parseColor("#1C1C1E")
            style = Paint.Style.STROKE
            strokeWidth = scaleFactor * 3.4f * uiScale
            strokeCap = Paint.Cap.ROUND
        }
        val filamentPath = Path().apply {
            val span = domeR * 0.35f
            val fTop = domeCy - (domeR * 0.35f)
            val fBot = domeCy + (domeR * 0.30f)
            moveTo(kCx - span, fBot)
            lineTo(kCx - span, fTop + span)
            quadTo(kCx - span, fTop, kCx, fTop)
            quadTo(kCx + span, fTop, kCx + span, fTop + span)
            lineTo(kCx + span, fBot)
        }
        canvas.drawPath(filamentPath, cutoutPaint)
    }

    return bitmap
}
