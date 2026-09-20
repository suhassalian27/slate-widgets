package com.altusix.slate.widgets.common

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.widget.RemoteViews
import com.altusix.slate.R
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.createSupersampledCanvas
import com.altusix.slate.utils.getSafeBgColor
import com.altusix.slate.utils.getStandardCornerRadius

// =========================================================================
// CENTRALIZED DESIGN TOKENS & STANDARDIZED CONSTANTS
// Change any value here to instantly update all folder & bento widgets.
// =========================================================================

// Margins & Dimensions (dp)
const val FOLDER_MARGIN_DP = 1.5f
const val FOLDER_TRIANGLE_MARGIN_DP = 0.75f

// Spacing & Gutters
const val FOLDER_SPACING_RATIO = 0.035f
const val FOLDER_MIN_SPACING_DP = 1.8f
const val FOLDER_MAX_SPACING_DP = 6.0f

// Corner Radii Proportions & Limits
const val FOLDER_CORNER_RATIO_GRID = 0.20f
const val FOLDER_CORNER_RATIO_BENTO = 0.08f
const val FOLDER_MIN_CORNER_DP = 2.0f
const val FOLDER_MAX_CORNER_DP = 7.0f

// Standard Theme Colors
const val COLOR_INNER_BG_LIGHT_HEX = "#F2F2F7"
const val COLOR_INNER_BG_DARK_HEX = "#111111"

/**
 * Resolves the inner tile background color.
 * Change either hex constant above to re-theme all widgets at once.
 */
fun getFolderInnerBgColor(isLight: Boolean, customColor: Int? = null): Int {
    if (customColor != null) return customColor
    return if (isLight) Color.parseColor(COLOR_INNER_BG_LIGHT_HEX) else Color.parseColor(COLOR_INNER_BG_DARK_HEX)
}

/**
 * Smart Area-Aware Spacing.
 * Uses sqrt(w * h) so elongated 1-row bars (4x1, 5x1) compute the same gap
 * as equivalent 2x2 grids, while still scaling down gracefully on compact widgets.
 */
fun getFolderSpacing(w: Float, h: Float, scaleFactor: Float): Float {
    val effectiveDim = kotlin.math.sqrt((w * h).toDouble()).toFloat()
    return (effectiveDim * FOLDER_SPACING_RATIO).coerceIn(
        scaleFactor * FOLDER_MIN_SPACING_DP,
        scaleFactor * FOLDER_MAX_SPACING_DP
    )
}

fun getFolderSpacing(rect: RectF, scaleFactor: Float): Float =
    getFolderSpacing(rect.width(), rect.height(), scaleFactor)

/**
 * Calculates uniform inner corner radius with min/max clamps.
 */
fun getFolderInnerCornerRadius(
    dim: Float,
    scaleFactor: Float,
    ratio: Float = FOLDER_CORNER_RATIO_GRID,
    maxAllowed: Float = Float.MAX_VALUE
): Float {
    return (dim * ratio)
        .coerceIn(scaleFactor * FOLDER_MIN_CORNER_DP, scaleFactor * FOLDER_MAX_CORNER_DP)
        .coerceAtMost(maxAllowed)
}

// =========================================================================
// UNIVERSAL RENDER ENGINES
// =========================================================================

/**
 * 1. Universal Grid Layout Engine (Social, AppFolder, AI, Contacts, Google)
 */
fun renderUniversalFolderGrid(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    cols: Int,
    rows: Int,
    showTileBackground: Boolean = true,
    customTileBgColor: Int? = null,
    drawSlot: (canvas: Canvas, tileRect: RectF, index: Int, scaleFactor: Float, isMicro: Boolean) -> Unit
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val slotCount = cols * rows
    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)

    val margin = scaleFactor * FOLDER_MARGIN_DP
    val targetRatio = cols.toFloat() / rows.toFloat()

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

    val minDim = minOf(cardRect.width(), cardRect.height())
    val spacing = getFolderSpacing(cardRect, scaleFactor)

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

    val innerCardBg = getFolderInnerBgColor(isLight, customTileBgColor)
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    val innerCornerRadius = getFolderInnerCornerRadius(
        dim = minOf(tileW, tileH),
        scaleFactor = scaleFactor,
        ratio = FOLDER_CORNER_RATIO_GRID,
        maxAllowed = minOf(tileW, tileH) / 2f
    )

    for (i in 0 until slotCount) {
        val col = i % cols
        val row = i / cols

        val tileLeft = innerCardRect.left + col * (tileW + spacing)
        val tileTop = innerCardRect.top + row * (tileH + spacing)
        val tileRect = RectF(tileLeft, tileTop, tileLeft + tileW, tileTop + tileH)

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

        if (showTileBackground) {
            canvas.drawPath(tilePath, tilePaint)
        }

        drawSlot(canvas, tileRect, i, scaleFactor, cols >= 4 && rows >= 2)
        canvas.restore()
    }

    return bitmap
}

/**
 * 2. Universal 6-App Bento Hero (2 Hero Top + 4 Small Bottom)
 */
fun renderUniversalBentoHero6(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    showTileBackground: Boolean = true,
    customTileBgColor: Int? = null,
    drawSlot: (canvas: Canvas, tileRect: RectF, index: Int, scaleFactor: Float, isMicro: Boolean) -> Unit
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)

    val margin = scaleFactor * FOLDER_MARGIN_DP
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
    val spacing = getFolderSpacing(cardRect, scaleFactor)

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

    val innerCardBg = getFolderInnerBgColor(isLight, customTileBgColor)
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    val innerCornerRadius = getFolderInnerCornerRadius(
        dim = minDim,
        scaleFactor = scaleFactor,
        ratio = FOLDER_CORNER_RATIO_BENTO,
        maxAllowed = innerCardRadius
    )

    val topH = ((innerCardRect.height() - spacing) * 0.58f).coerceAtLeast(1f)
    val topTileW = (innerCardRect.width() - spacing) / 2f

    // Top 2 Hero Tiles (0 and 1)
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
        if (showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSlot(canvas, rect, i, scaleFactor, false)
        canvas.restore()
    }

    // Bottom 4 Small Tiles (2 to 5)
    val botY = innerCardRect.top + topH + spacing
    val botH = innerCardRect.height() - topH - spacing
    val botTileW = (innerCardRect.width() - (spacing * 3f)) / 4f

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
        if (showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSlot(canvas, rect, i + 2, scaleFactor, true)
        canvas.restore()
    }

    return bitmap
}

/**
 * 3. Universal 8-App Bento Side (2 Hero Left + 6 Small Right)
 */
fun renderUniversalBentoSide8(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    showTileBackground: Boolean = true,
    customTileBgColor: Int? = null,
    drawSlot: (canvas: Canvas, tileRect: RectF, index: Int, scaleFactor: Float, isMicro: Boolean) -> Unit
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)

    val margin = scaleFactor * FOLDER_MARGIN_DP
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
    val spacing = getFolderSpacing(cardRect, scaleFactor)

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

    val innerCardBg = getFolderInnerBgColor(isLight, customTileBgColor)
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    val innerCornerRadius = getFolderInnerCornerRadius(
        dim = minDim,
        scaleFactor = scaleFactor,
        ratio = FOLDER_CORNER_RATIO_BENTO,
        maxAllowed = innerCardRadius
    )

    val leftTileW = (innerCardRect.width() - spacing) / 2f
    val leftTileH = (innerCardRect.height() - spacing) / 2f

    // Left 2 Hero Tiles (0 and 1)
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
        if (showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSlot(canvas, rect, i, scaleFactor, false)
        canvas.restore()
    }

    // Right 6 Small Tiles (2 to 7, 3 rows of 2)
    val rightStartX = innerCardRect.left + leftTileW + spacing
    val rightW = innerCardRect.width() - leftTileW - spacing
    val rightColW = (rightW - spacing) / 2f
    val rightRowH = (innerCardRect.height() - (spacing * 2f)) / 3f

    for (r in 0..2) {
        for (c in 0..1) {
            val index = 2 + (r * 2 + c)
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
            if (showTileBackground) canvas.drawPath(tilePath, tilePaint)
            drawSlot(canvas, rect, index, scaleFactor, true)
            canvas.restore()
        }
    }

    return bitmap
}

/**
 * 4. Universal 10-App Bento Top Renderer (2 Large Top + 8 Small Bottom)
 */
fun renderUniversalBentoTop10(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    showTileBackground: Boolean = true,
    customTileBgColor: Int? = null,
    drawSlot: (canvas: Canvas, tileRect: RectF, index: Int, scaleFactor: Float, isMicro: Boolean) -> Unit
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)

    val margin = scaleFactor * FOLDER_MARGIN_DP
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
    val spacing = getFolderSpacing(cardRect, scaleFactor)

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

    val innerCardBg = getFolderInnerBgColor(isLight, customTileBgColor)
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    val innerCornerRadius = getFolderInnerCornerRadius(
        dim = minDim,
        scaleFactor = scaleFactor,
        ratio = FOLDER_CORNER_RATIO_BENTO,
        maxAllowed = innerCardRadius
    )

    val bigW = (innerCardRect.width() - spacing) / 2f
    val topH = (innerCardRect.height() - spacing) / 2f

    for (i in 0..1) {
        val left = innerCardRect.left + i * (bigW + spacing)
        val top = innerCardRect.top
        val rect = RectF(left, top, left + bigW, top + topH)

        val tl = if (i == 0) 0f else innerCornerRadius
        val tr = if (i == 1) 0f else innerCornerRadius
        val radii = floatArrayOf(tl, tl, tr, tr, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius)
        val tilePath = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(innerCardPath)
        canvas.clipPath(tilePath)
        if (showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSlot(canvas, rect, i, scaleFactor, false)
        canvas.restore()
    }

    val bottomTop = innerCardRect.top + topH + spacing
    val bottomH = innerCardRect.height() - topH - spacing
    val microW = (innerCardRect.width() - (3f * spacing)) / 4f
    val microH = (bottomH - spacing) / 2f

    for (i in 0..7) {
        val col = i % 4
        val row = i / 4
        val left = innerCardRect.left + col * (microW + spacing)
        val top = bottomTop + row * (microH + spacing)
        val rect = RectF(left, top, left + microW, top + microH)

        val bl = if (col == 0 && row == 1) 0f else innerCornerRadius
        val br = if (col == 3 && row == 1) 0f else innerCornerRadius
        val radii = floatArrayOf(innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, br, br, bl, bl)
        val tilePath = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(innerCardPath)
        canvas.clipPath(tilePath)
        if (showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSlot(canvas, rect, i + 2, scaleFactor, true)
        canvas.restore()
    }

    return bitmap
}

/**
 * 5. Universal 10-App Bento Left Renderer (2 Large Left + 8 Small Right)
 */
fun renderUniversalBentoLeft10(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    showTileBackground: Boolean = true,
    customTileBgColor: Int? = null,
    drawSlot: (canvas: Canvas, tileRect: RectF, index: Int, scaleFactor: Float, isMicro: Boolean) -> Unit
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)

    val margin = scaleFactor * FOLDER_MARGIN_DP
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
    val spacing = getFolderSpacing(cardRect, scaleFactor)

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

    val innerCardBg = getFolderInnerBgColor(isLight, customTileBgColor)
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    val innerCornerRadius = getFolderInnerCornerRadius(
        dim = minDim,
        scaleFactor = scaleFactor,
        ratio = FOLDER_CORNER_RATIO_BENTO,
        maxAllowed = innerCardRadius
    )

    val leftW = (innerCardRect.width() - spacing) / 2f
    val bigH = (innerCardRect.height() - spacing) / 2f

    for (i in 0..1) {
        val left = innerCardRect.left
        val top = innerCardRect.top + i * (bigH + spacing)
        val rect = RectF(left, top, left + leftW, top + bigH)

        val tl = if (i == 0) 0f else innerCornerRadius
        val bl = if (i == 1) 0f else innerCornerRadius
        val radii = floatArrayOf(tl, tl, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, bl, bl)
        val tilePath = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(innerCardPath)
        canvas.clipPath(tilePath)
        if (showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSlot(canvas, rect, i, scaleFactor, false)
        canvas.restore()
    }

    val rightLeft = innerCardRect.left + leftW + spacing
    val rightW = innerCardRect.width() - leftW - spacing
    val microW = (rightW - spacing) / 2f
    val microH = (innerCardRect.height() - (3f * spacing)) / 4f

    for (i in 0..7) {
        val col = i % 2
        val row = i / 2
        val left = rightLeft + col * (microW + spacing)
        val top = innerCardRect.top + row * (microH + spacing)
        val rect = RectF(left, top, left + microW, top + microH)

        val tr = if (col == 1 && row == 0) 0f else innerCornerRadius
        val br = if (col == 1 && row == 3) 0f else innerCornerRadius
        val radii = floatArrayOf(innerCornerRadius, innerCornerRadius, tr, tr, br, br, innerCornerRadius, innerCornerRadius)
        val tilePath = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(innerCardPath)
        canvas.clipPath(tilePath)
        if (showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSlot(canvas, rect, i + 2, scaleFactor, true)
        canvas.restore()
    }

    return bitmap
}

/**
 * 6. Universal 7-App Asymmetric Bento (1 Large Hero + 2 Stacked Right + 4 Small Bottom)
 */
fun renderUniversalBentoAsymmetric7(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    showTileBackground: Boolean = true,
    customTileBgColor: Int? = null,
    drawSlot: (canvas: Canvas, tileRect: RectF, index: Int, scaleFactor: Float, isMicro: Boolean) -> Unit
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)

    val margin = scaleFactor * FOLDER_MARGIN_DP
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
    val spacing = getFolderSpacing(cardRect, scaleFactor)

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

    val innerCardBg = getFolderInnerBgColor(isLight, customTileBgColor)
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    val innerCornerRadius = getFolderInnerCornerRadius(
        dim = minDim,
        scaleFactor = scaleFactor,
        ratio = FOLDER_CORNER_RATIO_BENTO,
        maxAllowed = innerCardRadius
    )

    val topH = ((innerCardRect.height() - spacing) * 0.62f).coerceAtLeast(1f)
    val rightColW = (innerCardRect.width() * 0.36f).coerceAtLeast(1f)
    val heroW = innerCardRect.width() - spacing - rightColW
    val rightTileH = (topH - spacing) / 2f

    // 1. Slot 0: Big Hero Tile (Top-Left)
    val heroRect = RectF(innerCardRect.left, innerCardRect.top, innerCardRect.left + heroW, innerCardRect.top + topH)
    val heroRadii = floatArrayOf(0f, 0f, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius)
    val heroPath = Path().apply { addRoundRect(heroRect, heroRadii, Path.Direction.CW) }

    canvas.save()
    canvas.clipPath(innerCardPath)
    canvas.clipPath(heroPath)
    if (showTileBackground) canvas.drawPath(heroPath, tilePaint)
    drawSlot(canvas, heroRect, 0, scaleFactor, false)
    canvas.restore()

    // 2. Slots 1 & 2: Stacked Right Column
    val rightX = innerCardRect.left + heroW + spacing
    for (i in 0..1) {
        val top = innerCardRect.top + i * (rightTileH + spacing)
        val rect = RectF(rightX, top, rightX + rightColW, top + rightTileH)

        val tr = if (i == 0) 0f else innerCornerRadius
        val radii = floatArrayOf(innerCornerRadius, innerCornerRadius, tr, tr, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius)
        val path = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(innerCardPath)
        canvas.clipPath(path)
        if (showTileBackground) canvas.drawPath(path, tilePaint)
        drawSlot(canvas, rect, i + 1, scaleFactor, false)
        canvas.restore()
    }

    // 3. Slots 3..6: Bottom Row of 4 Small Tiles
    val botY = innerCardRect.top + topH + spacing
    val botH = innerCardRect.height() - topH - spacing
    val botTileW = (innerCardRect.width() - (spacing * 3f)) / 4f

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
        if (showTileBackground) canvas.drawPath(path, tilePaint)
        drawSlot(canvas, rect, i + 3, scaleFactor, true)
        canvas.restore()
    }

    return bitmap
}

/**
 * 7. Universal 7-App Bento Quadrant (3 Large Tiles + 1 Quadrant split into 4 Small Tiles)
 */
fun renderUniversalBentoQuadrant7(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    showTileBackground: Boolean = true,
    customTileBgColor: Int? = null,
    drawSlot: (canvas: Canvas, tileRect: RectF, index: Int, scaleFactor: Float, isMicro: Boolean) -> Unit
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)

    val margin = scaleFactor * FOLDER_MARGIN_DP
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
    val spacing = getFolderSpacing(cardRect, scaleFactor)

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

    val halfW = (innerCardRect.width() - spacing) / 2f
    val halfH = (innerCardRect.height() - spacing) / 2f
    val innerCornerRadius = getFolderInnerCornerRadius(
        dim = minOf(halfW, halfH),
        scaleFactor = scaleFactor,
        ratio = FOLDER_CORNER_RATIO_GRID,
        maxAllowed = innerCardRadius
    )

    val innerCardBg = getFolderInnerBgColor(isLight, customTileBgColor)
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    val bigRects = listOf(
        RectF(innerCardRect.left, innerCardRect.top, innerCardRect.left + halfW, innerCardRect.top + halfH),
        RectF(innerCardRect.left + halfW + spacing, innerCardRect.top, innerCardRect.right, innerCardRect.top + halfH),
        RectF(innerCardRect.left, innerCardRect.top + halfH + spacing, innerCardRect.left + halfW, innerCardRect.bottom)
    )

    val bigRadiiList = listOf(
        floatArrayOf(0f, 0f, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius),
        floatArrayOf(innerCornerRadius, innerCornerRadius, 0f, 0f, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius),
        floatArrayOf(innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, 0f, 0f)
    )

    for (i in 0..2) {
        val rect = bigRects[i]
        val tilePath = Path().apply { addRoundRect(rect, bigRadiiList[i], Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(innerCardPath)
        canvas.clipPath(tilePath)
        if (showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSlot(canvas, rect, i, scaleFactor, false)
        canvas.restore()
    }

    val q4Rect = RectF(innerCardRect.left + halfW + spacing, innerCardRect.top + halfH + spacing, innerCardRect.right, innerCardRect.bottom)
    val subW = (q4Rect.width() - spacing) / 2f
    val subH = (q4Rect.height() - spacing) / 2f

    for (i in 0..3) {
        val col = i % 2
        val row = i / 2
        val subRect = RectF(q4Rect.left + col * (subW + spacing), q4Rect.top + row * (subH + spacing), q4Rect.left + col * (subW + spacing) + subW, q4Rect.top + row * (subH + spacing) + subH)

        val br = if (col == 1 && row == 1) 0f else innerCornerRadius
        val radii = floatArrayOf(innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, br, br, innerCornerRadius, innerCornerRadius)
        val tilePath = Path().apply { addRoundRect(subRect, radii, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(innerCardPath)
        canvas.clipPath(tilePath)
        if (showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSlot(canvas, subRect, i + 3, scaleFactor, true)
        canvas.restore()
    }

    return bitmap
}

/**
 * 8. Universal Equilateral Triangle Path Builder
 */
fun buildRoundedTrianglePath(
    p0: PointF,
    p1: PointF,
    p2: PointF,
    r0: Float,
    r1: Float,
    r2: Float
): Path {
    val pts = arrayOf(p0, p1, p2)
    val radii = floatArrayOf(r0, r1, r2)
    val path = Path()

    val tStart = Array(3) { PointF() }
    val tEnd = Array(3) { PointF() }

    for (i in 0 until 3) {
        val prev = pts[(i + 2) % 3]
        val curr = pts[i]
        val next = pts[(i + 1) % 3]

        val vPrevX = prev.x - curr.x
        val vPrevY = prev.y - curr.y
        val lenPrev = Math.hypot(vPrevX.toDouble(), vPrevY.toDouble()).toFloat().coerceAtLeast(0.001f)

        val vNextX = next.x - curr.x
        val vNextY = next.y - curr.y
        val lenNext = Math.hypot(vNextX.toDouble(), vNextY.toDouble()).toFloat().coerceAtLeast(0.001f)

        val maxD = minOf(lenPrev, lenNext) * 0.45f
        val d = radii[i].coerceIn(0f, maxD)

        tStart[i] = PointF(curr.x + (vPrevX / lenPrev) * d, curr.y + (vPrevY / lenPrev) * d)
        tEnd[i] = PointF(curr.x + (vNextX / lenNext) * d, curr.y + (vNextY / lenNext) * d)
    }

    path.moveTo(tEnd[0].x, tEnd[0].y)
    path.lineTo(tStart[1].x, tStart[1].y)
    path.quadTo(pts[1].x, pts[1].y, tEnd[1].x, tEnd[1].y)

    path.lineTo(tStart[2].x, tStart[2].y)
    path.quadTo(pts[2].x, pts[2].y, tEnd[2].x, tEnd[2].y)

    path.lineTo(tStart[0].x, tStart[0].y)
    path.quadTo(pts[0].x, pts[0].y, tEnd[0].x, tEnd[0].y)

    path.close()
    return path
}

/**
 * 9. Universal 4-App Triforce Triangle Layout Engine
 * Equilateral geometry where the outer perimeter padding and the inner dividing gaps
 * are mathematically identical (both equal to G).
 */
fun renderUniversalTriangle4(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    showTileBackground: Boolean = true,
    customTileBgColor: Int? = null,
    drawSlot: (canvas: Canvas, slotRect: RectF, index: Int, scaleFactor: Float, isMicro: Boolean) -> Unit
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)

    val margin = scaleFactor * FOLDER_TRIANGLE_MARGIN_DP
    val availW = w - (margin * 2f)
    val availH = h - (margin * 2f)
    val equilateralRatio = 0.8660254f // sqrt(3) / 2

    var triW = availW
    var triH = triW * equilateralRatio
    if (triH > availH) {
        triH = availH
        triW = triH / equilateralRatio
    }

    val leftX = (w - triW) / 2f
    val topY = (h - triH) / 2f
    val rightX = leftX + triW
    val bottomY = topY + triH
    val cx = (leftX + rightX) / 2f

    val outerApex = PointF(cx, topY)
    val outerBL = PointF(leftX, bottomY)
    val outerBR = PointF(rightX, bottomY)

    val outerCornerRadius = (minOf(triW, triH) * 0.11f).coerceIn(scaleFactor * 8f, scaleFactor * 18f)
    val outerPath = buildRoundedTrianglePath(
        outerApex, outerBL, outerBR,
        outerCornerRadius, outerCornerRadius, outerCornerRadius
    )

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawPath(outerPath, bgPaint)

    // Standardized uniform gap & side padding (G)
    val gap = (minOf(triW, triH) * FOLDER_SPACING_RATIO).coerceIn(
        scaleFactor * FOLDER_MIN_SPACING_DP,
        scaleFactor * FOLDER_MAX_SPACING_DP
    )

    val mainCentroid = PointF(cx, (topY + bottomY + bottomY) / 3f)

    // Step 1: Inset parent triangle by G / 2
    val scaleParent = (1f - (1.5f * gap / triH)).coerceIn(0.5f, 0.99f)

    fun insetParent(pt: PointF): PointF =
        PointF(mainCentroid.x + (pt.x - mainCentroid.x) * scaleParent, mainCentroid.y + (pt.y - mainCentroid.y) * scaleParent)

    val inApex = insetParent(outerApex)
    val inBL = insetParent(outerBL)
    val inBR = insetParent(outerBR)

    // Midpoints of the inset parent triangle
    val mAB = PointF((inApex.x + inBL.x) / 2f, (inApex.y + inBL.y) / 2f)
    val mAC = PointF((inApex.x + inBR.x) / 2f, (inApex.y + inBR.y) / 2f)
    val mBC = PointF((inBL.x + inBR.x) / 2f, (inBL.y + inBR.y) / 2f)

    val subTriangles = listOf(
        Triple(inApex, mAB, mAC), // 0: Top
        Triple(mAB, inBL, mBC),   // 1: Bottom-Left
        Triple(mAC, mBC, mAB),   // 2: Center Inverted
        Triple(mAC, mBC, inBR)    // 3: Bottom-Right
    )

    val innerCardBg = getFolderInnerBgColor(isLight, customTileBgColor)
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    // Step 2: Inset each sub-triangle by G / 2 (creating total G on sides and between tiles)
    val subH = (triH * scaleParent) / 2f
    val shrinkFactor = (1f - (1.5f * gap / subH)).coerceIn(0.6f, 0.98f)

    val outerRad = (outerCornerRadius - gap).coerceAtLeast(scaleFactor * 4f)
    val innerRad = (gap * 0.85f).coerceIn(scaleFactor * 3f, scaleFactor * 7f)

    for (i in 0..3) {
        val (v0, v1, v2) = subTriangles[i]
        val cX = (v0.x + v1.x + v2.x) / 3f
        val cY = (v0.y + v1.y + v2.y) / 3f
        val centroid = PointF(cX, cY)

        fun shrink(pt: PointF): PointF =
            PointF(centroid.x + (pt.x - centroid.x) * shrinkFactor, centroid.y + (pt.y - centroid.y) * shrinkFactor)

        val q0 = shrink(v0)
        val q1 = shrink(v1)
        val q2 = shrink(v2)

        val (r0, r1, r2) = when (i) {
            0 -> Triple(outerRad, innerRad, innerRad)
            1 -> Triple(innerRad, outerRad, innerRad)
            2 -> Triple(innerRad, innerRad, innerRad)
            else -> Triple(innerRad, innerRad, outerRad)
        }

        val tilePath = buildRoundedTrianglePath(q0, q1, q2, r0, r1, r2)

        if (showTileBackground) {
            canvas.drawPath(tilePath, tilePaint)
        }

        val tileBoxSize = subH * shrinkFactor * 0.78f
        val slotRect = RectF(
            centroid.x - tileBoxSize / 2f,
            centroid.y - tileBoxSize / 2f,
            centroid.x + tileBoxSize / 2f,
            centroid.y + tileBoxSize / 2f
        )

        canvas.save()
        canvas.clipPath(tilePath)
        drawSlot(canvas, slotRect, i, scaleFactor, false)
        canvas.restore()
    }

    return bitmap
}

/**
 * 10. Universal 3-App Bento Trio (Top Full-Width Banner + 2 Bottom Tiles)
 */
fun renderUniversalBentoTrio3(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    showTileBackground: Boolean = true,
    customTileBgColor: Int? = null,
    drawSlot: (canvas: Canvas, tileRect: RectF, index: Int, scaleFactor: Float, isBanner: Boolean) -> Unit
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)

    val margin = scaleFactor * FOLDER_MARGIN_DP
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
    val spacing = getFolderSpacing(cardRect, scaleFactor)

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

    val innerCornerRadius = getFolderInnerCornerRadius(
        dim = minDim,
        scaleFactor = scaleFactor,
        ratio = FOLDER_CORNER_RATIO_BENTO,
        maxAllowed = innerCardRadius
    )

    val innerCardBg = getFolderInnerBgColor(isLight, customTileBgColor)
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    val aspectRatio = cardRect.width() / cardRect.height()

    when {
        // --- 1. TALL VERTICAL STRIP (1 Column x 3 Rows) ---
        isResponsive && aspectRatio < 0.72f -> {
            val tileW = innerCardRect.width()
            val tileH = (innerCardRect.height() - (spacing * 2f)) / 3f

            for (i in 0..2) {
                val top = innerCardRect.top + i * (tileH + spacing)
                val rect = RectF(innerCardRect.left, top, innerCardRect.right, top + tileH)

                val tl = if (i == 0) 0f else innerCornerRadius
                val tr = if (i == 0) 0f else innerCornerRadius
                val br = if (i == 2) 0f else innerCornerRadius
                val bl = if (i == 2) 0f else innerCornerRadius

                val radii = floatArrayOf(tl, tl, tr, tr, br, br, bl, bl)
                val path = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }

                canvas.save()
                canvas.clipPath(innerCardPath)
                canvas.clipPath(path)
                if (showTileBackground) canvas.drawPath(path, tilePaint)
                drawSlot(canvas, rect, i, scaleFactor, false)
                canvas.restore()
            }
        }

        // --- 2. WIDE HORIZONTAL STRIP (3 Columns x 1 Row) ---
        isResponsive && aspectRatio > 1.65f -> {
            val tileW = (innerCardRect.width() - (spacing * 2f)) / 3f
            val tileH = innerCardRect.height()

            for (i in 0..2) {
                val left = innerCardRect.left + i * (tileW + spacing)
                val rect = RectF(left, innerCardRect.top, left + tileW, innerCardRect.bottom)

                val tl = if (i == 0) 0f else innerCornerRadius
                val tr = if (i == 2) 0f else innerCornerRadius
                val br = if (i == 2) 0f else innerCornerRadius
                val bl = if (i == 0) 0f else innerCornerRadius

                val radii = floatArrayOf(tl, tl, tr, tr, br, br, bl, bl)
                val path = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }

                canvas.save()
                canvas.clipPath(innerCardPath)
                canvas.clipPath(path)
                if (showTileBackground) canvas.drawPath(path, tilePaint)
                drawSlot(canvas, rect, i, scaleFactor, false)
                canvas.restore()
            }
        }

        // --- 3. STANDARD BENTO (Top Full-Width Banner + 2 Bottom Tiles) ---
        else -> {
            val availableH = innerCardRect.height() - spacing
            val topH = availableH * 0.48f
            val bottomH = availableH - topH
            val bottomTileW = (innerCardRect.width() - spacing) / 2f

            // Top Banner (Slot 0)
            val topRect = RectF(innerCardRect.left, innerCardRect.top, innerCardRect.right, innerCardRect.top + topH)
            val topRadii = floatArrayOf(0f, 0f, 0f, 0f, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius)
            val topPath = Path().apply { addRoundRect(topRect, topRadii, Path.Direction.CW) }

            canvas.save()
            canvas.clipPath(innerCardPath)
            canvas.clipPath(topPath)
            if (showTileBackground) canvas.drawPath(topPath, tilePaint)
            drawSlot(canvas, topRect, 0, scaleFactor, true)
            canvas.restore()

            // Bottom-Left (Slot 1)
            val bLeftRect = RectF(innerCardRect.left, topRect.bottom + spacing, innerCardRect.left + bottomTileW, innerCardRect.bottom)
            val bLeftRadii = floatArrayOf(innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, 0f, 0f)
            val bLeftPath = Path().apply { addRoundRect(bLeftRect, bLeftRadii, Path.Direction.CW) }

            canvas.save()
            canvas.clipPath(innerCardPath)
            canvas.clipPath(bLeftPath)
            if (showTileBackground) canvas.drawPath(bLeftPath, tilePaint)
            drawSlot(canvas, bLeftRect, 1, scaleFactor, false)
            canvas.restore()

            // Bottom-Right (Slot 2)
            val bRightRect = RectF(bLeftRect.right + spacing, topRect.bottom + spacing, innerCardRect.right, innerCardRect.bottom)
            val bRightRadii = floatArrayOf(innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, 0f, 0f, innerCornerRadius, innerCornerRadius)
            val bRightPath = Path().apply { addRoundRect(bRightRect, bRightRadii, Path.Direction.CW) }

            canvas.save()
            canvas.clipPath(innerCardPath)
            canvas.clipPath(bRightPath)
            if (showTileBackground) canvas.drawPath(bRightPath, tilePaint)
            drawSlot(canvas, bRightRect, 2, scaleFactor, false)
            canvas.restore()
        }
    }

    return bitmap
}

// =========================================================================
// UNIVERSAL TOUCH BINDERS
// =========================================================================

/**
 * Binds launch intents for standard app/action slots.
 */
fun RemoteViews.bindFolderTouchSlots(
    context: Context,
    widgetId: Int,
    slotCount: Int,
    getIntent: (slotIndex: Int) -> Intent?
) {
    val touchSlotIds = intArrayOf(
        R.id.slot_0, R.id.slot_1, R.id.slot_2, R.id.slot_3, R.id.slot_4,
        R.id.slot_5, R.id.slot_6, R.id.slot_7, R.id.slot_8, R.id.slot_9
    )
    val legacyTouchSlotIds = intArrayOf(
        R.id.touch_slot_0, R.id.touch_slot_1, R.id.touch_slot_2, R.id.touch_slot_3,
        R.id.touch_slot_4, R.id.touch_slot_5, R.id.touch_slot_6, R.id.touch_slot_7,
        R.id.touch_slot_8, R.id.touch_slot_9
    )

    for (i in 0 until slotCount) {
        val intent = getIntent(i) ?: continue
        val pi = PendingIntent.getActivity(
            context,
            widgetId * 100 + i,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        touchSlotIds.getOrNull(i)?.let { setOnClickPendingIntent(it, pi) }
        legacyTouchSlotIds.getOrNull(i)?.let { setOnClickPendingIntent(it, pi) }
    }
}

/**
 * Binds direct PendingIntents for widgets with mixed broadcast & activity actions.
 */
fun RemoteViews.bindFolderTouchPendingIntents(
    slotCount: Int,
    getPendingIntent: (slotIndex: Int) -> PendingIntent?
) {
    val touchSlotIds = intArrayOf(
        R.id.slot_0, R.id.slot_1, R.id.slot_2, R.id.slot_3, R.id.slot_4,
        R.id.slot_5, R.id.slot_6, R.id.slot_7, R.id.slot_8, R.id.slot_9
    )
    val legacyTouchSlotIds = intArrayOf(
        R.id.touch_slot_0, R.id.touch_slot_1, R.id.touch_slot_2, R.id.touch_slot_3,
        R.id.touch_slot_4, R.id.touch_slot_5, R.id.touch_slot_6, R.id.touch_slot_7,
        R.id.touch_slot_8, R.id.touch_slot_9
    )

    for (i in 0 until slotCount) {
        val pi = getPendingIntent(i) ?: continue
        touchSlotIds.getOrNull(i)?.let { setOnClickPendingIntent(it, pi) }
        legacyTouchSlotIds.getOrNull(i)?.let { setOnClickPendingIntent(it, pi) }
    }
}

