package com.altusix.slate.widgets.google

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
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

// 2. GOOGLE WORKSPACE QUAD (2x2)
fun generateGoogleWorkspaceQuadBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    // 1. Dual-Mode Container Geometry (Section B: Rule 3)
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

    // 2. Smart Adaptive Layouts (Aspect Ratio Branching)
    val cardW = cardRect.width()
    val cardH = cardRect.height()
    val aspectRatio = cardW / cardH

    val (cols, rows) = when {
        isResponsive && aspectRatio >= 1.35f -> 4 to 1
        isResponsive && aspectRatio <= 0.70f -> 1 to 4
        else -> 2 to 2
    }

    val pad = (minOf(cardW, cardH) * 0.055f).coerceAtLeast(scaleFactor * 6f)
    val gap = (minOf(cardW, cardH) * 0.040f).coerceIn(scaleFactor * 5f, scaleFactor * 9f)

    val availableW = cardW - (pad * 2f) - (gap * (cols - 1))
    val availableH = cardH - (pad * 2f) - (gap * (rows - 1))

    val tileW = availableW / cols
    val tileH = availableH / rows

    // Concentric nesting: Outer curve matches container radius, inner curve stays subtle
    val outerR = (cardCornerRadius - pad).coerceAtLeast(scaleFactor * 6f)
    val innerR = (scaleFactor * 6f).coerceAtMost(minOf(tileW, tileH) * 0.22f)

    val innerBgColor = if (isLight) Color.parseColor("#EAEAEF") else Color.parseColor("#161618")
    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = innerBgColor }

    val iconDrawables = listOf(
        R.drawable.ic_google_logo,
        R.drawable.ic_youtube,
        R.drawable.ic_gmail,
        R.drawable.ic_drive
    )

    val iconSize = (minOf(tileW, tileH) * 0.44f).toInt().coerceAtLeast((scaleFactor * 16f).toInt())

    for (index in 0 until 4) {
        val col = index % cols
        val row = index / cols
        val left = cardRect.left + pad + col * (tileW + gap)
        val top = cardRect.top + pad + row * (tileH + gap)
        val tileRect = RectF(left, top, left + tileW, top + tileH)

        val radii = when {
            cols == 2 && rows == 2 -> when (index) {
                0 -> floatArrayOf(outerR, outerR, innerR, innerR, innerR, innerR, innerR, innerR)
                1 -> floatArrayOf(innerR, innerR, outerR, outerR, innerR, innerR, innerR, innerR)
                2 -> floatArrayOf(innerR, innerR, innerR, innerR, innerR, innerR, outerR, outerR)
                else -> floatArrayOf(innerR, innerR, innerR, innerR, outerR, outerR, innerR, innerR)
            }
            cols == 4 -> when (index) {
                0 -> floatArrayOf(outerR, outerR, innerR, innerR, innerR, innerR, outerR, outerR)
                3 -> floatArrayOf(innerR, innerR, outerR, outerR, outerR, outerR, innerR, innerR)
                else -> floatArrayOf(innerR, innerR, innerR, innerR, innerR, innerR, innerR, innerR)
            }
            else -> when (index) { // cols == 1 (Vertical 1x4 stack)
                0 -> floatArrayOf(outerR, outerR, outerR, outerR, innerR, innerR, innerR, innerR)
                3 -> floatArrayOf(innerR, innerR, innerR, innerR, outerR, outerR, outerR, outerR)
                else -> floatArrayOf(innerR, innerR, innerR, innerR, innerR, innerR, innerR, innerR)
            }
        }

        val tilePath = Path().apply {
            addRoundRect(tileRect, radii, Path.Direction.CW)
        }
        canvas.drawPath(tilePath, innerPaint)

        val resId = iconDrawables[index]
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

// 3. GOOGLE TRIO BENTO (2x2: Top Google Bar + YouTube & Photos)
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
    val aspectRatio = cardW / cardH

    val pad = (minOf(cardW, cardH) * 0.055f).coerceAtLeast(scaleFactor * 6f)
    val gap = (minOf(cardW, cardH) * 0.040f).coerceIn(scaleFactor * 5f, scaleFactor * 9f)

    val outerR = (cardCornerRadius - pad).coerceAtLeast(scaleFactor * 8f)
    val innerR = (scaleFactor * 7f)

    val innerBgColor = if (isLight) Color.parseColor("#EAEAEF") else Color.parseColor("#161618")
    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = innerBgColor }

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.06f
    }

    // Smart Adaptive Layout Branching
    when {
        // --- 1. TALL VERTICAL STRIP (1 Column x 3 Rows) ---
        isResponsive && aspectRatio < 0.72f -> {
            val availableW = cardW - (pad * 2f)
            val availableH = cardH - (pad * 2f) - (gap * 2f)
            val tileW = availableW
            val tileH = availableH / 3f

            val iconSize = (minOf(tileW, tileH) * 0.44f).toInt().coerceAtLeast((scaleFactor * 16f).toInt())

            // Tile 0: Google (Top)
            val tile0 = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.top + pad + tileH)
            val r0 = floatArrayOf(outerR, outerR, outerR, outerR, innerR, innerR, innerR, innerR)
            canvas.drawPath(Path().apply { addRoundRect(tile0, r0, Path.Direction.CW) }, innerPaint)

            // Auto-scale "GOOGLE" or fall back to 'G' logo if too narrow
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
                    val iconLeft = (tile0.centerX() - iconSize / 2f).toInt()
                    val iconTop = (tile0.centerY() - iconSize / 2f).toInt()
                    setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                    draw(canvas)
                }
            }

            // Tile 1: YouTube (Middle)
            val tile1 = RectF(cardRect.left + pad, tile0.bottom + gap, cardRect.right - pad, tile0.bottom + gap + tileH)
            val r1 = floatArrayOf(innerR, innerR, innerR, innerR, innerR, innerR, innerR, innerR)
            canvas.drawPath(Path().apply { addRoundRect(tile1, r1, Path.Direction.CW) }, innerPaint)

            ContextCompat.getDrawable(context, R.drawable.ic_youtube)?.mutate()?.apply {
                setTint(accentColorInt)
                val iconLeft = (tile1.centerX() - iconSize / 2f).toInt()
                val iconTop = (tile1.centerY() - iconSize / 2f).toInt()
                setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                draw(canvas)
            }

            // Tile 2: Photos (Bottom)
            val tile2 = RectF(cardRect.left + pad, tile1.bottom + gap, cardRect.right - pad, cardRect.bottom - pad)
            val r2 = floatArrayOf(innerR, innerR, innerR, innerR, outerR, outerR, outerR, outerR)
            canvas.drawPath(Path().apply { addRoundRect(tile2, r2, Path.Direction.CW) }, innerPaint)

            ContextCompat.getDrawable(context, R.drawable.ic_photos)?.mutate()?.apply {
                setTint(accentColorInt)
                val iconLeft = (tile2.centerX() - iconSize / 2f).toInt()
                val iconTop = (tile2.centerY() - iconSize / 2f).toInt()
                setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                draw(canvas)
            }
        }

        // --- 2. WIDE HORIZONTAL STRIP (3 Columns x 1 Row) ---
        isResponsive && aspectRatio > 1.65f -> {
            val availableW = cardW - (pad * 2f) - (gap * 2f)
            val availableH = cardH - (pad * 2f)
            val tileW = availableW / 3f
            val tileH = availableH

            val iconSize = (minOf(tileW, tileH) * 0.44f).toInt().coerceAtLeast((scaleFactor * 16f).toInt())

            // Tile 0: Google (Left)
            val tile0 = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.left + pad + tileW, cardRect.bottom - pad)
            val r0 = floatArrayOf(outerR, outerR, innerR, innerR, innerR, innerR, outerR, outerR)
            canvas.drawPath(Path().apply { addRoundRect(tile0, r0, Path.Direction.CW) }, innerPaint)

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
                    val iconLeft = (tile0.centerX() - iconSize / 2f).toInt()
                    val iconTop = (tile0.centerY() - iconSize / 2f).toInt()
                    setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                    draw(canvas)
                }
            }

            // Tile 1: YouTube (Center)
            val tile1 = RectF(tile0.right + gap, cardRect.top + pad, tile0.right + gap + tileW, cardRect.bottom - pad)
            val r1 = floatArrayOf(innerR, innerR, innerR, innerR, innerR, innerR, innerR, innerR)
            canvas.drawPath(Path().apply { addRoundRect(tile1, r1, Path.Direction.CW) }, innerPaint)

            ContextCompat.getDrawable(context, R.drawable.ic_youtube)?.mutate()?.apply {
                setTint(accentColorInt)
                val iconLeft = (tile1.centerX() - iconSize / 2f).toInt()
                val iconTop = (tile1.centerY() - iconSize / 2f).toInt()
                setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                draw(canvas)
            }

            // Tile 2: Photos (Right)
            val tile2 = RectF(tile1.right + gap, cardRect.top + pad, cardRect.right - pad, cardRect.bottom - pad)
            val r2 = floatArrayOf(innerR, innerR, outerR, outerR, outerR, outerR, innerR, innerR)
            canvas.drawPath(Path().apply { addRoundRect(tile2, r2, Path.Direction.CW) }, innerPaint)

            ContextCompat.getDrawable(context, R.drawable.ic_photos)?.mutate()?.apply {
                setTint(accentColorInt)
                val iconLeft = (tile2.centerX() - iconSize / 2f).toInt()
                val iconTop = (tile2.centerY() - iconSize / 2f).toInt()
                setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                draw(canvas)
            }
        }

        // --- 3. STANDARD BENTO (Top Full-Width Banner + Bottom 2 Tiles) ---
        else -> {
            val availableW = cardW - (pad * 2f)
            val availableH = cardH - (pad * 2f) - gap

            val topH = availableH * 0.48f
            val bottomH = availableH - topH
            val bottomTileW = (availableW - gap) / 2f

            val iconSize = (minOf(bottomTileW, bottomH) * 0.44f).toInt().coerceAtLeast((scaleFactor * 16f).toInt())

            // Top Google Banner
            val topRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.top + pad + topH)
            val topRadii = floatArrayOf(outerR, outerR, outerR, outerR, innerR, innerR, innerR, innerR)
            canvas.drawPath(Path().apply { addRoundRect(topRect, topRadii, Path.Direction.CW) }, innerPaint)

            // Dynamic text scaling with boundary protection
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
                    val iconLeft = (topRect.centerX() - iconSize / 2f).toInt()
                    val iconTop = (topRect.centerY() - iconSize / 2f).toInt()
                    setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                    draw(canvas)
                }
            }

            // Bottom-Left (YouTube)
            val bLeftRect = RectF(cardRect.left + pad, topRect.bottom + gap, cardRect.left + pad + bottomTileW, cardRect.bottom - pad)
            val bLeftRadii = floatArrayOf(innerR, innerR, innerR, innerR, innerR, innerR, outerR, outerR)
            canvas.drawPath(Path().apply { addRoundRect(bLeftRect, bLeftRadii, Path.Direction.CW) }, innerPaint)

            ContextCompat.getDrawable(context, R.drawable.ic_youtube)?.mutate()?.apply {
                setTint(accentColorInt)
                val iconLeft = (bLeftRect.centerX() - iconSize / 2f).toInt()
                val iconTop = (bLeftRect.centerY() - iconSize / 2f).toInt()
                setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                draw(canvas)
            }

            // Bottom-Right (Photos)
            val bRightRect = RectF(bLeftRect.right + gap, topRect.bottom + gap, cardRect.right - pad, cardRect.bottom - pad)
            val bRightRadii = floatArrayOf(innerR, innerR, innerR, innerR, outerR, outerR, innerR, innerR)
            canvas.drawPath(Path().apply { addRoundRect(bRightRect, bRightRadii, Path.Direction.CW) }, innerPaint)

            ContextCompat.getDrawable(context, R.drawable.ic_photos)?.mutate()?.apply {
                setTint(accentColorInt)
                val iconLeft = (bRightRect.centerX() - iconSize / 2f).toInt()
                val iconTop = (bRightRect.centerY() - iconSize / 2f).toInt()
                setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                draw(canvas)
            }
        }
    }

    return bitmap
}

// 4. GOOGLE MEGA FOLDER (4x2 / 10 Google Apps Adaptive Bento)
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

    // 1. Dual-Mode Geometry
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

    val innerBgColor = if (isLight) Color.parseColor("#EAEAEF") else Color.parseColor("#161618")
    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = innerBgColor }

    val iconDrawables = listOf(
        R.drawable.ic_google_logo,
        R.drawable.ic_youtube,
        R.drawable.ic_gmail,
        R.drawable.ic_drive,
        R.drawable.ic_photos,
        R.drawable.ic_maps,
        R.drawable.ic_calendar,
        R.drawable.ic_chrome,
        R.drawable.ic_playstore,
        R.drawable.ic_google_gemini
    )

    if (aspectRatio >= 1.1f) {
        // ================================================================
        // 1. WIDE MODE (5 Columns x 2 Rows)
        // ================================================================
        val pad = cardH * 0.055f
        val gap = cardH * 0.035f

        val availW = cardW - (pad * 2f) - (gap * 4f)
        val availH = cardH - (pad * 2f) - gap

        val tileW = availW / 5f
        val tileH = availH / 2f

        val outerR = (cornerRadius - pad).coerceAtLeast(scaleFactor * 8f)
        val innerR = scaleFactor * 6f
        val iconSize = (minOf(tileW, tileH) * 0.44f).toInt().coerceAtLeast((scaleFactor * 14f).toInt())

        for (index in 0 until 10) {
            val col = index % 5
            val row = index / 5
            val left = cardRect.left + pad + col * (tileW + gap)
            val top = cardRect.top + pad + row * (tileH + gap)
            val tileRect = RectF(left, top, left + tileW, top + tileH)

            val radii = when (index) {
                0 -> floatArrayOf(outerR, outerR, innerR, innerR, innerR, innerR, innerR, innerR)
                4 -> floatArrayOf(innerR, innerR, outerR, outerR, innerR, innerR, innerR, innerR)
                5 -> floatArrayOf(innerR, innerR, innerR, innerR, innerR, innerR, outerR, outerR)
                9 -> floatArrayOf(innerR, innerR, innerR, innerR, outerR, outerR, innerR, innerR)
                else -> floatArrayOf(innerR, innerR, innerR, innerR, innerR, innerR, innerR, innerR)
            }

            val tilePath = Path().apply { addRoundRect(tileRect, radii, Path.Direction.CW) }
            canvas.drawPath(tilePath, innerPaint)

            ContextCompat.getDrawable(context, iconDrawables[index])?.mutate()?.apply {
                setTint(accentColorInt)
                val iconLeft = (tileRect.centerX() - iconSize / 2f).toInt()
                val iconTop = (tileRect.centerY() - iconSize / 2f).toInt()
                setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                draw(canvas)
            }
        }
    } else {
        // ================================================================
        // 2. TALL / VERTICAL MODE (2 Columns x 5 Rows)
        // ================================================================
        val pad = cardW * 0.055f
        val gap = cardW * 0.035f

        val availW = cardW - (pad * 2f) - gap
        val availH = cardH - (pad * 2f) - (gap * 4f)

        val tileW = availW / 2f
        val tileH = availH / 5f

        val outerR = (cornerRadius - pad).coerceAtLeast(scaleFactor * 8f)
        val innerR = scaleFactor * 6f
        val iconSize = (minOf(tileW, tileH) * 0.44f).toInt().coerceAtLeast((scaleFactor * 14f).toInt())

        for (index in 0 until 10) {
            val col = index % 2
            val row = index / 2
            val left = cardRect.left + pad + col * (tileW + gap)
            val top = cardRect.top + pad + row * (tileH + gap)
            val tileRect = RectF(left, top, left + tileW, top + tileH)

            val radii = when (index) {
                0 -> floatArrayOf(outerR, outerR, innerR, innerR, innerR, innerR, innerR, innerR)
                1 -> floatArrayOf(innerR, innerR, outerR, outerR, innerR, innerR, innerR, innerR)
                8 -> floatArrayOf(innerR, innerR, innerR, innerR, innerR, innerR, outerR, outerR)
                9 -> floatArrayOf(innerR, innerR, innerR, innerR, outerR, outerR, innerR, innerR)
                else -> floatArrayOf(innerR, innerR, innerR, innerR, innerR, innerR, innerR, innerR)
            }

            val tilePath = Path().apply { addRoundRect(tileRect, radii, Path.Direction.CW) }
            canvas.drawPath(tilePath, innerPaint)

            ContextCompat.getDrawable(context, iconDrawables[index])?.mutate()?.apply {
                setTint(accentColorInt)
                val iconLeft = (tileRect.centerX() - iconSize / 2f).toInt()
                val iconTop = (tileRect.centerY() - iconSize / 2f).toInt()
                setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                draw(canvas)
            }
        }
    }

    return bitmap
}
