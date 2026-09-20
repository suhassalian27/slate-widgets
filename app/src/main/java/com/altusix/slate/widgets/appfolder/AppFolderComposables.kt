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
import com.altusix.slate.widgets.common.renderUniversalFolderGrid
import com.altusix.slate.widgets.common.renderUniversalBentoTop10
import com.altusix.slate.widgets.common.renderUniversalBentoLeft10
import com.altusix.slate.widgets.common.renderUniversalBentoQuadrant7
import com.altusix.slate.widgets.common.renderUniversalTriangle4

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

// Universal App Folder Grid Generator (Delegated to FolderWidgetKit)
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
    val isLight = config.themeMode == "LIGHT"
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    return renderUniversalFolderGrid(
        context = context,
        config = config,
        isResponsive = isResponsive,
        wDp = wDp,
        hDp = hDp,
        cols = cols,
        rows = rows,
        showTileBackground = folderConfig.showTileBackground
    ) { canvas, tileRect, index, scaleFactor, isMicro ->
        val slotConfig = folderConfig.slots.getOrElse(index) { AppSlotConfig() }
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
            isMicro = isMicro
        )
    }
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
    val isLight = config.themeMode == "LIGHT"
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    return renderUniversalBentoQuadrant7(
        context = context,
        config = config,
        isResponsive = isResponsive,
        wDp = wDp,
        hDp = hDp,
        showTileBackground = folderConfig.showTileBackground
    ) { canvas, tileRect, index, scaleFactor, isMicro ->
        val slotConfig = folderConfig.slots.getOrElse(index) { AppSlotConfig() }
        drawSlotContent(
            canvas = canvas,
            context = context,
            tileRect = tileRect,
            slotConfig = slotConfig,
            showAppNames = if (isMicro) false else folderConfig.showAppNames,
            isLight = isLight,
            scaleFactor = scaleFactor,
            primaryText = primaryText,
            secondaryText = secondaryText,
            isMicro = isMicro
        )
    }
}

fun generateAppFolderBento7Bitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
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

// 10. 10-APP BENTO LEFT (Delegated to FolderWidgetKit)
fun generateAppFolderBento10LeftBitmap(
    context: Context,
    config: SlateWidgetConfig,
    folderConfig: AppFolderWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val isLight = config.themeMode == "LIGHT"
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    return renderUniversalBentoLeft10(
        context = context,
        config = config,
        isResponsive = isResponsive,
        wDp = wDp,
        hDp = hDp,
        showTileBackground = folderConfig.showTileBackground
    ) { canvas, tileRect, index, scaleFactor, isMicro ->
        val slotConfig = folderConfig.slots.getOrElse(index) { AppSlotConfig() }
        drawSlotContent(
            canvas = canvas,
            context = context,
            tileRect = tileRect,
            slotConfig = slotConfig,
            showAppNames = if (isMicro) false else folderConfig.showAppNames,
            isLight = isLight,
            scaleFactor = scaleFactor,
            primaryText = primaryText,
            secondaryText = secondaryText,
            isMicro = isMicro
        )
    }
}

fun generateAppFolderBento10LeftBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val folderConfig = AppFolderWidgetConfig.load(context, widgetId, 10)
    return generateAppFolderBento10LeftBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId)
}

// 11. 10-APP BENTO TOP (Delegated to FolderWidgetKit)
fun generateAppFolderBento10TopBitmap(
    context: Context,
    config: SlateWidgetConfig,
    folderConfig: AppFolderWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val isLight = config.themeMode == "LIGHT"
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    return renderUniversalBentoTop10(
        context = context,
        config = config,
        isResponsive = isResponsive,
        wDp = wDp,
        hDp = hDp,
        showTileBackground = folderConfig.showTileBackground
    ) { canvas, tileRect, index, scaleFactor, isMicro ->
        val slotConfig = folderConfig.slots.getOrElse(index) { AppSlotConfig() }
        drawSlotContent(
            canvas = canvas,
            context = context,
            tileRect = tileRect,
            slotConfig = slotConfig,
            showAppNames = if (isMicro) false else folderConfig.showAppNames,
            isLight = isLight,
            scaleFactor = scaleFactor,
            primaryText = primaryText,
            secondaryText = secondaryText,
            isMicro = isMicro
        )
    }
}

fun generateAppFolderBento10TopBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
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
    val isLight = config.themeMode == "LIGHT"
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    return renderUniversalTriangle4(
        context = context,
        config = config,
        isResponsive = false,
        wDp = wDp,
        hDp = hDp,
        showTileBackground = folderConfig.showTileBackground
    ) { canvas, slotRect, index, scaleFactor, isMicro ->
        val slotConfig = folderConfig.slots.getOrElse(index) { AppSlotConfig() }
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
            isMicro = isMicro
        )
    }
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
