package com.altusix.slate.widgets.appfolder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.createSupersampledCanvas
import com.altusix.slate.utils.getSafeBgColor
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius

fun getAppIconBitmap(context: Context, packageName: String, size: Int): Bitmap? {
    return try {
        val drawable: Drawable = context.packageManager.getApplicationIcon(packageName)
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            Bitmap.createScaledBitmap(drawable.bitmap, size, size, true)
        } else {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    } catch (_: Exception) { null }
}

private fun drawSlotContent(
    canvas: Canvas,
    context: Context,
    tileRect: RectF,
    slotConfig: AppSlotConfig,
    showAppNames: Boolean,
    isLight: Boolean,
    scaleFactor: Float,
    primaryText: Int,
    secondaryText: Int,
    isMicro: Boolean = false
) {
    val tileW = tileRect.width()
    val tileH = tileRect.height()
    val minDim = minOf(tileW, tileH)
    val showText = showAppNames && !isMicro && tileH >= scaleFactor * 32f

    val baseRatio = if (showText) 0.46f else 0.58f
    val maxIconSize = scaleFactor * (if (showText) 40f else 46f)
    val iconSize = (minDim * baseRatio).coerceIn(scaleFactor * 12f, maxIconSize)

    val iconCy = if (showText) tileRect.centerY() - (scaleFactor * 5f) else tileRect.centerY()
    val gap = scaleFactor * 6f
    val textY = iconCy + (iconSize / 2f) + gap + (scaleFactor * 8f)

    if (!slotConfig.isConfigured) {
        val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.parseColor("#E5E5EA") else Color.parseColor("#2C2C2E")
            style = Paint.Style.FILL
        }
        val radius = iconSize / 2f
        canvas.drawCircle(tileRect.centerX(), iconCy, radius, placeholderPaint)

        val plusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = radius * 1.1f
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }
        val bounds = Rect()
        plusPaint.getTextBounds("+", 0, 1, bounds)
        canvas.drawText("+", tileRect.centerX(), iconCy + (bounds.height() / 2f), plusPaint)

        if (showText) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = secondaryText
                typeface = getSlateFont(context, weight = 600)
                textAlign = Paint.Align.CENTER
            }
            val fontSize = (minDim * 0.11f).coerceIn(scaleFactor * 7f, scaleFactor * 11f)
            textPaint.textSize = fontSize
            canvas.drawText("Add App", tileRect.centerX(), textY, textPaint)
        }
    } else {
        val appIcon = getAppIconBitmap(context, slotConfig.packageName, iconSize.toInt())
        if (appIcon != null) {
            val iconRect = RectF(
                tileRect.centerX() - (iconSize / 2f),
                iconCy - (iconSize / 2f),
                tileRect.centerX() + (iconSize / 2f),
                iconCy + (iconSize / 2f)
            )
            canvas.drawBitmap(appIcon, null, iconRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }

        if (showText) {
            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryText
                typeface = getSlateFont(context, weight = 600)
                textAlign = Paint.Align.CENTER
            }
            val maxTextWidth = tileW * 0.88f
            var fontSize = (minDim * 0.12f).coerceIn(scaleFactor * 7f, scaleFactor * 11.5f)
            namePaint.textSize = fontSize
            while (namePaint.measureText(slotConfig.appName) > maxTextWidth && fontSize > scaleFactor * 6f) {
                fontSize -= scaleFactor * 0.5f
                namePaint.textSize = fontSize
            }
            canvas.drawText(slotConfig.appName, tileRect.centerX(), textY, namePaint)
        }
    }
}

// Universal App Folder Grid Generator with Concentric Inner Clipping & Unified Spacing
fun generateAppFolderGridBitmap(
    context: Context,
    config: SlateWidgetConfig,
    folderConfig: AppFolderWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int,
    cols: Int,
    rows: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val slotCount = cols * rows
    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    // 1. Dual-Mode Geometry (Width-Dominant for horizontal bars)
    val margin = scaleFactor * 1.5f
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

    // 2. Tightened Uniform Spacing (Scales from 1.8dp up to 5.5dp)
    val minDim = minOf(cardRect.width(), cardRect.height())
    val spacing = (minDim * 0.032f).coerceIn(scaleFactor * 1.8f, scaleFactor * 5.5f)

    // 3. Mathematical Concentric Inner Boundary (R_inner = R_outer - spacing)
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

    val innerCardBg = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    val innerCornerRadius = (minOf(tileW, tileH) * 0.20f)
        .coerceIn(scaleFactor * 2.0f, scaleFactor * 7.0f)
        .coerceAtMost(minOf(tileW, tileH) / 2f)

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

        if (folderConfig.showTileBackground) {
            canvas.drawPath(tilePath, tilePaint)
        }

        val slotConfig = folderConfig.slots.getOrElse(i) { AppSlotConfig() }
        drawSlotContent(
            canvas = canvas,
            context = context,
            tileRect = tileRect,
            slotConfig = slotConfig,
            showAppNames = folderConfig.showAppNames,
            isLight = isLight,
            scaleFactor = scaleFactor,
            primaryText = primaryText,
            secondaryText = secondaryText,
            isMicro = cols >= 4 && rows >= 2
        )
        canvas.restore()
    }
    return bitmap
}

private fun buildRoundedTrianglePath(
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

// ============================================================================
// CONCRETE APP FOLDER GENERATORS
// ============================================================================

// 1. 4-APP FOLDER (2x2)
fun generateAppFolder4Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val folderConfig = AppFolderWidgetConfig.load(context, widgetId, 4)
    return generateAppFolderGridBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId, cols = 2, rows = 2)
}
fun generateAppFolder4Bitmap(context: Context, config: SlateWidgetConfig, folderConfig: AppFolderWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
    generateAppFolderGridBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId, cols = 2, rows = 2)

// 2. 8-APP FOLDER (4x2 / 2x4 Pivot)
fun generateAppFolder8Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val folderConfig = AppFolderWidgetConfig.load(context, widgetId, 8)
    return generateAppFolder8Bitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId)
}
fun generateAppFolder8Bitmap(context: Context, config: SlateWidgetConfig, folderConfig: AppFolderWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val isVertical = isResponsive && (hDp > wDp)
    val cols = if (isVertical) 2 else 4
    val rows = if (isVertical) 4 else 2
    return generateAppFolderGridBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId, cols = cols, rows = rows)
}

// 3. 3-APP HORIZONTAL (3x1 / 1x3 Pivot)
fun generateAppFolderHorizontal3Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val folderConfig = AppFolderWidgetConfig.load(context, widgetId, 3)
    return generateAppFolderHorizontal3Bitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId)
}
fun generateAppFolderHorizontal3Bitmap(context: Context, config: SlateWidgetConfig, folderConfig: AppFolderWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val isVertical = isResponsive && (hDp > wDp)
    val cols = if (isVertical) 1 else 3
    val rows = if (isVertical) 3 else 1
    return generateAppFolderGridBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId, cols = cols, rows = rows)
}

// 4. 3-APP VERTICAL (1x3)
fun generateAppFolderVertical3Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val folderConfig = AppFolderWidgetConfig.load(context, widgetId, 3)
    return generateAppFolderVertical3Bitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId)
}
fun generateAppFolderVertical3Bitmap(context: Context, config: SlateWidgetConfig, folderConfig: AppFolderWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
    generateAppFolderGridBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId, cols = 1, rows = 3)

// 5. 4-APP ROW (4x1 / 1x4 Pivot)
fun generateAppFolderRow4Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val folderConfig = AppFolderWidgetConfig.load(context, widgetId, 4)
    return generateAppFolderRow4Bitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId)
}
fun generateAppFolderRow4Bitmap(context: Context, config: SlateWidgetConfig, folderConfig: AppFolderWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val isVertical = isResponsive && (hDp > wDp)
    val cols = if (isVertical) 1 else 4
    val rows = if (isVertical) 4 else 1
    return generateAppFolderGridBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId, cols = cols, rows = rows)
}

// 6. 5-APP ROW (5x1 / 1x5 Pivot)
fun generateAppFolderRow5Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val folderConfig = AppFolderWidgetConfig.load(context, widgetId, 5)
    return generateAppFolderRow5Bitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId)
}
fun generateAppFolderRow5Bitmap(context: Context, config: SlateWidgetConfig, folderConfig: AppFolderWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val isVertical = isResponsive && (hDp > wDp)
    val cols = if (isVertical) 1 else 5
    val rows = if (isVertical) 5 else 1
    return generateAppFolderGridBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId, cols = cols, rows = rows)
}

// 7. 6-APP CIRCLE DIAL (2x2)
fun generateAppFolderCircle6Bitmap(
    context: Context,
    config: SlateWidgetConfig,
    folderConfig: AppFolderWidgetConfig,
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
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    val margin = scaleFactor * 1.5f
    val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f
    val cardRect = RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    val cx = cardRect.centerX()
    val cy = cardRect.centerY()

    val outerRadius = cardSize / 2f
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(
            (config.opacity.coerceIn(0f, 1f) * 255).toInt(),
            Color.red(bgColor),
            Color.green(bgColor),
            Color.blue(bgColor)
        )
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, outerRadius, bgPaint)

    val orbitRadius = outerRadius * 0.62f
    val tileRadius = outerRadius * 0.245f

    val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(16, 0, 0, 0) else Color.argb(22, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.8f
    }
    canvas.drawCircle(cx, cy, orbitRadius, guidePaint)

    val hubRadius = outerRadius * 0.12f
    val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(18, 0, 0, 0) else Color.argb(26, 255, 255, 255)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, hubRadius, hubPaint)

    val innerCardBg = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    for (i in 0 until 6) {
        val angle = Math.toRadians((i * 60.0) - 90.0)
        val slotX = cx + (orbitRadius * Math.cos(angle)).toFloat()
        val slotY = cy + (orbitRadius * Math.sin(angle)).toFloat()

        val tileRect = RectF(slotX - tileRadius, slotY - tileRadius, slotX + tileRadius, slotY + tileRadius)
        val tileConfig = folderConfig.slots.getOrElse(i) { AppSlotConfig() }

        if (folderConfig.showTileBackground) {
            canvas.drawCircle(slotX, slotY, tileRadius, tilePaint)
        }

        drawSlotContent(
            canvas = canvas,
            context = context,
            tileRect = tileRect,
            slotConfig = tileConfig,
            showAppNames = false,
            isLight = isLight,
            scaleFactor = scaleFactor,
            primaryText = if (isLight) Color.BLACK else Color.WHITE,
            secondaryText = secondaryText,
            isMicro = false
        )
    }

    return bitmap
}

fun generateAppFolderCircle6Bitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val folderConfig = AppFolderWidgetConfig.load(context, widgetId, 6)
    return generateAppFolderCircle6Bitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId)
}

// 8. 7-APP BENTO (2x2 / 3 Big + 4 Small)
fun generateAppFolderBento7Bitmap(
    context: Context,
    config: SlateWidgetConfig,
    folderConfig: AppFolderWidgetConfig,
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
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

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

    val halfW = (innerCardRect.width() - spacing) / 2f
    val halfH = (innerCardRect.height() - spacing) / 2f
    val innerCornerRadius = (minOf(halfW, halfH) * 0.20f)
        .coerceIn(scaleFactor * 2.0f, scaleFactor * 7.0f)
        .coerceAtMost(innerCardRadius)

    val innerCardBg = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
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
        val slotConfig = folderConfig.slots.getOrElse(i) { AppSlotConfig() }
        val tilePath = Path().apply { addRoundRect(rect, bigRadiiList[i], Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(innerCardPath)
        canvas.clipPath(tilePath)

        if (folderConfig.showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSlotContent(canvas, context, rect, slotConfig, folderConfig.showAppNames, isLight, scaleFactor, primaryText, secondaryText)
        canvas.restore()
    }

    // Bottom-right quadrant with 4 small tiles (Slots 3 to 6)
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

        if (folderConfig.showTileBackground) canvas.drawPath(tilePath, tilePaint)
        val slotConfig = folderConfig.slots.getOrElse(i + 3) { AppSlotConfig() }
        drawSlotContent(canvas, context, subRect, slotConfig, false, isLight, scaleFactor, primaryText, secondaryText, isMicro = true)
        canvas.restore()
    }
    return bitmap
}

fun generateAppFolderBento7Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val folderConfig = AppFolderWidgetConfig.load(context, widgetId, 7)
    return generateAppFolderBento7Bitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId)
}

// 9. 9-APP GRID (3x3)
fun generateAppFolderGrid9Bitmap(
    context: Context,
    config: SlateWidgetConfig,
    folderConfig: AppFolderWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap = generateAppFolderGridBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId, cols = 3, rows = 3)

fun generateAppFolderGrid9Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val folderConfig = AppFolderWidgetConfig.load(context, widgetId, 9)
    return generateAppFolderGrid9Bitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId)
}

// 10. 10-APP BENTO LEFT BIG (4x2 / 2 Big Left + 8 Small Right)
fun generateAppFolderBento10LeftBitmap(
    context: Context,
    config: SlateWidgetConfig,
    folderConfig: AppFolderWidgetConfig,
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
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

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

    val innerCardBg = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    val innerCornerRadius = (minDim * 0.08f)
        .coerceIn(scaleFactor * 2.0f, scaleFactor * 7.0f)
        .coerceAtMost(innerCardRadius)

    val leftW = (innerCardRect.width() - spacing) / 2f
    val bigH = (innerCardRect.height() - spacing) / 2f

    for (i in 0..1) {
        val rect = RectF(innerCardRect.left, innerCardRect.top + i * (bigH + spacing), innerCardRect.left + leftW, innerCardRect.top + i * (bigH + spacing) + bigH)
        val tl = if (i == 0) 0f else innerCornerRadius
        val bl = if (i == 1) 0f else innerCornerRadius
        val radii = floatArrayOf(tl, tl, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, bl, bl)

        val slotConfig = folderConfig.slots.getOrElse(i) { AppSlotConfig() }
        val tilePath = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(innerCardPath)
        canvas.clipPath(tilePath)

        if (folderConfig.showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSlotContent(canvas, context, rect, slotConfig, folderConfig.showAppNames, isLight, scaleFactor, primaryText, secondaryText, isMicro = false)
        canvas.restore()
    }

    val rightLeft = innerCardRect.left + leftW + spacing
    val rightW = innerCardRect.width() - leftW - spacing
    val microW = (rightW - spacing) / 2f
    val microH = (innerCardRect.height() - (3f * spacing)) / 4f

    for (i in 0..7) {
        val col = i % 2
        val row = i / 2
        val tr = if (col == 1 && row == 0) 0f else innerCornerRadius
        val br = if (col == 1 && row == 3) 0f else innerCornerRadius
        val radii = floatArrayOf(innerCornerRadius, innerCornerRadius, tr, tr, br, br, innerCornerRadius, innerCornerRadius)

        val rect = RectF(rightLeft + col * (microW + spacing), innerCardRect.top + row * (microH + spacing), rightLeft + col * (microW + spacing) + microW, innerCardRect.top + row * (microH + spacing) + microH)
        val slotConfig = folderConfig.slots.getOrElse(i + 2) { AppSlotConfig() }
        val tilePath = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(innerCardPath)
        canvas.clipPath(tilePath)

        if (folderConfig.showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSlotContent(canvas, context, rect, slotConfig, false, isLight, scaleFactor, primaryText, secondaryText, isMicro = true)
        canvas.restore()
    }
    return bitmap
}

fun generateAppFolderBento10LeftBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val folderConfig = AppFolderWidgetConfig.load(context, widgetId, 10)
    return generateAppFolderBento10LeftBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId)
}

// 11. 10-APP BENTO TOP BIG (4x2 / 2 Big Top + 8 Small Bottom)
fun generateAppFolderBento10TopBitmap(
    context: Context,
    config: SlateWidgetConfig,
    folderConfig: AppFolderWidgetConfig,
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
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

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

    val innerCardBg = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    val innerCornerRadius = (minDim * 0.08f)
        .coerceIn(scaleFactor * 2.0f, scaleFactor * 7.0f)
        .coerceAtMost(innerCardRadius)

    val bigW = (innerCardRect.width() - spacing) / 2f
    val topH = (innerCardRect.height() - spacing) / 2f

    for (i in 0..1) {
        val rect = RectF(innerCardRect.left + i * (bigW + spacing), innerCardRect.top, innerCardRect.left + i * (bigW + spacing) + bigW, innerCardRect.top + topH)
        val tl = if (i == 0) 0f else innerCornerRadius
        val tr = if (i == 1) 0f else innerCornerRadius
        val radii = floatArrayOf(tl, tl, tr, tr, innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius)

        val slotConfig = folderConfig.slots.getOrElse(i) { AppSlotConfig() }
        val tilePath = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(innerCardPath)
        canvas.clipPath(tilePath)

        if (folderConfig.showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSlotContent(canvas, context, rect, slotConfig, folderConfig.showAppNames, isLight, scaleFactor, primaryText, secondaryText, isMicro = false)
        canvas.restore()
    }

    val bottomTop = innerCardRect.top + topH + spacing
    val bottomH = innerCardRect.height() - topH - spacing
    val microW = (innerCardRect.width() - (3f * spacing)) / 4f
    val microH = (bottomH - spacing) / 2f

    for (i in 0..7) {
        val col = i % 4
        val row = i / 4
        val bl = if (col == 0 && row == 1) 0f else innerCornerRadius
        val br = if (col == 3 && row == 1) 0f else innerCornerRadius
        val radii = floatArrayOf(innerCornerRadius, innerCornerRadius, innerCornerRadius, innerCornerRadius, br, br, bl, bl)

        val rect = RectF(innerCardRect.left + col * (microW + spacing), bottomTop + row * (microH + spacing), innerCardRect.left + col * (microW + spacing) + microW, bottomTop + row * (microH + spacing) + microH)
        val slotConfig = folderConfig.slots.getOrElse(i + 2) { AppSlotConfig() }
        val tilePath = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(innerCardPath)
        canvas.clipPath(tilePath)

        if (folderConfig.showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSlotContent(canvas, context, rect, slotConfig, false, isLight, scaleFactor, primaryText, secondaryText, isMicro = true)
        canvas.restore()
    }
    return bitmap
}

fun generateAppFolderBento10TopBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val folderConfig = AppFolderWidgetConfig.load(context, widgetId, 10)
    return generateAppFolderBento10TopBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId)
}

// 12. 4-APP TRIANGLE FOLDER (2x2)
fun generateAppFolderTriangle4Bitmap(
    context: Context,
    config: SlateWidgetConfig,
    folderConfig: AppFolderWidgetConfig,
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
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    val margin = scaleFactor * 0.75f
    val availW = w - (margin * 2f)
    val availH = h - (margin * 2f)
    val equilateralRatio = 0.8660254f

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

    val pad = (minOf(triW, triH) * 0.038f).coerceIn(scaleFactor * 3.5f, scaleFactor * 8f)
    val gap = (minOf(triW, triH) * 0.026f).coerceIn(scaleFactor * 2.5f, scaleFactor * 5.5f)

    val mainCentroid = PointF(cx, (topY + bottomY + bottomY) / 3f)
    val scalePad = (1f - (3f * pad / triH)).coerceAtLeast(0.1f)

    fun insetPoint(pt: PointF): PointF =
        PointF(mainCentroid.x + (pt.x - mainCentroid.x) * scalePad, mainCentroid.y + (pt.y - mainCentroid.y) * scalePad)

    val inApex = insetPoint(outerApex)
    val inBL = insetPoint(outerBL)
    val inBR = insetPoint(outerBR)

    val mAB = PointF((inApex.x + inBL.x) / 2f, (inApex.y + inBL.y) / 2f)
    val mAC = PointF((inApex.x + inBR.x) / 2f, (inApex.y + inBR.y) / 2f)
    val mBC = PointF((inBL.x + inBR.x) / 2f, (inBL.y + inBR.y) / 2f)

    val subTriangles = listOf(
        Triple(inApex, mAB, mAC),
        Triple(mAB, inBL, mBC),
        Triple(mAC, mBC, mAB),
        Triple(mAC, mBC, inBR)
    )

    val innerCardBg = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    val subH = triH * scalePad / 2f
    val shrinkFactor = (1f - (1.2f * gap / subH)).coerceIn(0.6f, 0.98f)
    val outerRad = (outerCornerRadius - pad).coerceAtLeast(scaleFactor * 6f)
    val innerRad = (gap * 1.2f).coerceIn(scaleFactor * 3.5f, scaleFactor * 8f)

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

        if (folderConfig.showTileBackground) {
            canvas.drawPath(tilePath, tilePaint)
        }

        val slotConfig = folderConfig.slots.getOrElse(i) { AppSlotConfig() }
        val tileBoxSize = subH * shrinkFactor * 0.82f
        val slotRect = RectF(
            centroid.x - tileBoxSize / 2f,
            centroid.y - tileBoxSize / 2f,
            centroid.x + tileBoxSize / 2f,
            centroid.y + tileBoxSize / 2f
        )

        canvas.save()
        canvas.clipPath(tilePath)
        drawSlotContent(
            canvas = canvas,
            context = context,
            tileRect = slotRect,
            slotConfig = slotConfig,
            showAppNames = folderConfig.showAppNames,
            isLight = isLight,
            scaleFactor = scaleFactor,
            primaryText = primaryText,
            secondaryText = secondaryText,
            isMicro = false
        )
        canvas.restore()
    }

    return bitmap
}

fun generateAppFolderTriangle4Bitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val folderConfig = AppFolderWidgetConfig.load(context, widgetId, 4)
    return generateAppFolderTriangle4Bitmap(context, config, folderConfig, false, wDp, hDp, widgetId)
}
