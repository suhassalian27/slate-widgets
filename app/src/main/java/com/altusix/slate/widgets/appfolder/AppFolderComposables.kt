package com.altusix.slate.widgets.appfolder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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

    val iconRatio = if (showText) 0.46f else 0.58f
    val iconSize = (minDim * iconRatio).coerceAtLeast(scaleFactor * 12f)

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

// Universal App Folder Grid Generator with Concentric Per-Corner Radii
fun generateAppFolderGridBitmap(
    context: Context,
    config: SlateWidgetConfig,
    folderConfig: AppFolderWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int,
    cols: Int = when (folderConfig.slotCount) {
        3 -> if (wDp >= hDp) 3 else 1
        4 -> if (wDp > hDp * 2.5f) 4 else 2
        5 -> 5
        8 -> 4
        9 -> 3
        else -> 2
    },
    rows: Int = when (folderConfig.slotCount) {
        3 -> if (wDp >= hDp) 1 else 3
        4 -> if (wDp > hDp * 2.5f) 1 else 2
        5 -> 1
        8 -> 2
        9 -> 3
        else -> 2
    }
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val slotCount = cols * rows
    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    // 1. Dual-Mode Geometry
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

    // 2. Proportional Padding & Edge Fill
    val minDim = minOf(cardRect.width(), cardRect.height())
    val pad = (minDim * 0.045f).coerceIn(scaleFactor * 4f, scaleFactor * 9f)
    val gap = (minDim * 0.035f).coerceIn(scaleFactor * 2.5f, scaleFactor * 6f)

    val availableW = cardRect.width() - (pad * 2f) - (gap * (cols - 1))
    val availableH = cardRect.height() - (pad * 2f) - (gap * (rows - 1))
    val tileW = availableW / cols
    val tileH = availableH / rows

    val innerCardBg = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    // 3. Concentric Per-Corner Radius
    val minTileDim = minOf(tileW, tileH)
    val squircleRadius = minTileDim * 0.28f
    val concentricRadius = (outerRadius - pad).coerceIn(squircleRadius, minTileDim / 2f)

    for (i in 0 until slotCount) {
        val col = i % cols
        val row = i / cols

        val tileLeft = cardRect.left + pad + col * (tileW + gap)
        val tileTop = cardRect.top + pad + row * (tileH + gap)
        val tileRect = RectF(tileLeft, tileTop, tileLeft + tileW, tileTop + tileH)

        val slotConfig = folderConfig.slots.getOrElse(i) { AppSlotConfig() }

        // Concentric Corner Radii Mapping for Edge Tiles
        val tl = if (col == 0 && row == 0) concentricRadius else squircleRadius
        val tr = if (col == cols - 1 && row == 0) concentricRadius else squircleRadius
        val br = if (col == cols - 1 && row == rows - 1) concentricRadius else squircleRadius
        val bl = if (col == 0 && row == rows - 1) concentricRadius else squircleRadius

        val radii = floatArrayOf(tl, tl, tr, tr, br, br, bl, bl)
        val tilePath = Path().apply { addRoundRect(tileRect, radii, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(tilePath)

        if (folderConfig.showTileBackground) {
            canvas.drawPath(tilePath, tilePaint)
        }

        drawSlotContent(
            canvas, context, tileRect, slotConfig, folderConfig.showAppNames,
            isLight, scaleFactor, primaryText, secondaryText
        )
        canvas.restore()
    }
    return bitmap
}

// 1. 4-APP FOLDER (2x2)
fun generateAppFolder4Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val folderConfig = AppFolderWidgetConfig.load(context, widgetId, 4)
    return generateAppFolderGridBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId, cols = 2, rows = 2)
}
fun generateAppFolder4Bitmap(context: Context, config: SlateWidgetConfig, folderConfig: AppFolderWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
    generateAppFolderGridBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId, cols = 2, rows = 2)

// 2. 8-APP FOLDER (4x2)
fun generateAppFolder8Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val folderConfig = AppFolderWidgetConfig.load(context, widgetId, 8)
    return generateAppFolderGridBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId, cols = 4, rows = 2)
}
fun generateAppFolder8Bitmap(context: Context, config: SlateWidgetConfig, folderConfig: AppFolderWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
    generateAppFolderGridBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId, cols = 4, rows = 2)

// 3. 3-APP HORIZONTAL (3x1)
fun generateAppFolderHorizontal3Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val folderConfig = AppFolderWidgetConfig.load(context, widgetId, 3)
    return generateAppFolderGridBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId, cols = 3, rows = 1)
}
fun generateAppFolderHorizontal3Bitmap(context: Context, config: SlateWidgetConfig, folderConfig: AppFolderWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
    generateAppFolderGridBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId, cols = 3, rows = 1)

// 4. 3-APP VERTICAL (1x3)
fun generateAppFolderVertical3Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val folderConfig = AppFolderWidgetConfig.load(context, widgetId, 3)
    return generateAppFolderGridBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId, cols = 1, rows = 3)
}
fun generateAppFolderVertical3Bitmap(context: Context, config: SlateWidgetConfig, folderConfig: AppFolderWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
    generateAppFolderGridBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId, cols = 1, rows = 3)

// 5. 4-APP ROW (4x1)
fun generateAppFolderRow4Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val folderConfig = AppFolderWidgetConfig.load(context, widgetId, 4)
    return generateAppFolderGridBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId, cols = 4, rows = 1)
}
fun generateAppFolderRow4Bitmap(context: Context, config: SlateWidgetConfig, folderConfig: AppFolderWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
    generateAppFolderGridBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId, cols = 4, rows = 1)

// 6. 5-APP ROW (5x1)
fun generateAppFolderRow5Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val folderConfig = AppFolderWidgetConfig.load(context, widgetId, 5)
    return generateAppFolderGridBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId, cols = 5, rows = 1)
}
fun generateAppFolderRow5Bitmap(context: Context, config: SlateWidgetConfig, folderConfig: AppFolderWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
    generateAppFolderGridBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId, cols = 5, rows = 1)

// 7. 6-APP CIRCLE DIAL (2x2 / Minimal Dial)
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
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    // 1. Base Dial Plate
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
    val tileRadius = outerRadius * 0.42f

    // 2. Subtle Orbit Guide (Thin, low-contrast guide line)
    val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(16, 0, 0, 0) else Color.argb(22, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.8f
    }
    canvas.drawCircle(cx, cy, orbitRadius, guidePaint)

    // 3. Minimal Accent Center Ring (A clean open ring instead of clutter)
    val centerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1.4f
    }
    canvas.drawCircle(cx, cy, scaleFactor * 3.5f, centerRingPaint)

    // 4. App Slots
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    for (i in 0 until 6) {
        val angle = Math.toRadians((i * 60.0) - 90.0)
        val slotX = cx + (orbitRadius * Math.cos(angle)).toFloat()
        val slotY = cy + (orbitRadius * Math.sin(angle)).toFloat()

        val tileRect = RectF(slotX - tileRadius, slotY - tileRadius, slotX + tileRadius, slotY + tileRadius)
        val tileConfig = folderConfig.slots.getOrElse(i) { AppSlotConfig() }

        drawSlotContent(
            canvas = canvas,
            context = context,
            tileRect = tileRect,
            slotConfig = tileConfig,
            showAppNames = false,
            isLight = isLight,
            scaleFactor = scaleFactor,
            primaryText = Color.WHITE,
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
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val pad = scaleFactor * 8f
    val gap = scaleFactor * 8f
    val halfW = (cardRect.width() - (pad * 2f) - gap) / 2f
    val halfH = (cardRect.height() - (pad * 2f) - gap) / 2f
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
        style = Paint.Style.FILL
    }

    val concentricRadius = (outerRadius - pad).coerceAtLeast(scaleFactor * 6f)
    val sq = scaleFactor * 8f

    val bigRects = listOf(
        RectF(cardRect.left + pad, cardRect.top + pad, cardRect.left + pad + halfW, cardRect.top + pad + halfH),
        RectF(cardRect.left + pad + halfW + gap, cardRect.top + pad, cardRect.right - pad, cardRect.top + pad + halfH),
        RectF(cardRect.left + pad, cardRect.top + pad + halfH + gap, cardRect.left + pad + halfW, cardRect.bottom - pad)
    )

    val bigRadiiList = listOf(
        floatArrayOf(concentricRadius, concentricRadius, sq, sq, sq, sq, sq, sq),
        floatArrayOf(sq, sq, concentricRadius, concentricRadius, sq, sq, sq, sq),
        floatArrayOf(sq, sq, sq, sq, sq, sq, concentricRadius, concentricRadius)
    )

    for (i in 0..2) {
        val rect = bigRects[i]
        val slotConfig = folderConfig.slots.getOrElse(i) { AppSlotConfig() }
        val tilePath = Path().apply { addRoundRect(rect, bigRadiiList[i], Path.Direction.CW) }

        if (folderConfig.showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSlotContent(canvas, context, rect, slotConfig, folderConfig.showAppNames, isLight, scaleFactor, primaryText, secondaryText)
    }

    val q4Rect = RectF(cardRect.left + pad + halfW + gap, cardRect.top + pad + halfH + gap, cardRect.right - pad, cardRect.bottom - pad)
    val subW = (q4Rect.width() - gap) / 2f
    val subH = (q4Rect.height() - gap) / 2f

    val microRadiiList = listOf(
        floatArrayOf(sq, sq, sq, sq, sq, sq, sq, sq),
        floatArrayOf(sq, sq, sq, sq, sq, sq, sq, sq),
        floatArrayOf(sq, sq, sq, sq, sq, sq, sq, sq),
        floatArrayOf(sq, sq, sq, sq, concentricRadius, concentricRadius, sq, sq)
    )

    for (i in 0..3) {
        val col = i % 2
        val row = i / 2
        val subRect = RectF(q4Rect.left + col * (subW + gap), q4Rect.top + row * (subH + gap), q4Rect.left + col * (subW + gap) + subW, q4Rect.top + row * (subH + gap) + subH)
        val slotConfig = folderConfig.slots.getOrElse(i + 3) { AppSlotConfig() }
        val tilePath = Path().apply { addRoundRect(subRect, microRadiiList[i], Path.Direction.CW) }

        if (folderConfig.showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSlotContent(canvas, context, subRect, slotConfig, false, isLight, scaleFactor, primaryText, secondaryText, isMicro = true)
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

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val pad = scaleFactor * 8f
    val gap = scaleFactor * 8f
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
        style = Paint.Style.FILL
    }

    val leftW = (cardRect.width() - (pad * 2f) - gap) / 2f
    val bigH = (cardRect.height() - (pad * 2f) - gap) / 2f
    val concentricRadius = (outerRadius - pad).coerceAtLeast(scaleFactor * 6f)
    val sq = scaleFactor * 8f

    val bigRadiiList = listOf(
        floatArrayOf(concentricRadius, concentricRadius, sq, sq, sq, sq, sq, sq),
        floatArrayOf(sq, sq, sq, sq, sq, sq, concentricRadius, concentricRadius)
    )

    for (i in 0..1) {
        val rect = RectF(cardRect.left + pad, cardRect.top + pad + i * (bigH + gap), cardRect.left + pad + leftW, cardRect.top + pad + i * (bigH + gap) + bigH)
        val slotConfig = folderConfig.slots.getOrElse(i) { AppSlotConfig() }
        val tilePath = Path().apply { addRoundRect(rect, bigRadiiList[i], Path.Direction.CW) }

        if (folderConfig.showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSlotContent(canvas, context, rect, slotConfig, folderConfig.showAppNames, isLight, scaleFactor, primaryText, secondaryText)
    }

    val rightLeft = cardRect.left + pad + leftW + gap
    val rightW = cardRect.right - pad - rightLeft
    val microW = (rightW - gap) / 2f
    val microH = (cardRect.height() - (pad * 2f) - (gap * 3f)) / 4f

    for (i in 0..7) {
        val col = i % 2
        val row = i / 2
        val tr = if (col == 1 && row == 0) concentricRadius else sq
        val br = if (col == 1 && row == 3) concentricRadius else sq
        val radii = floatArrayOf(sq, sq, tr, tr, br, br, sq, sq)

        val rect = RectF(rightLeft + col * (microW + gap), cardRect.top + pad + row * (microH + gap), rightLeft + col * (microW + gap) + microW, cardRect.top + pad + row * (microH + gap) + microH)
        val slotConfig = folderConfig.slots.getOrElse(i + 2) { AppSlotConfig() }
        val tilePath = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }

        if (folderConfig.showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSlotContent(canvas, context, rect, slotConfig, false, isLight, scaleFactor, primaryText, secondaryText, isMicro = true)
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

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val pad = scaleFactor * 8f
    val gap = scaleFactor * 8f
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
        style = Paint.Style.FILL
    }

    val topH = (cardRect.height() - (pad * 2f) - gap) / 2f
    val bigW = (cardRect.width() - (pad * 2f) - gap) / 2f
    val concentricRadius = (outerRadius - pad).coerceAtLeast(scaleFactor * 6f)
    val sq = scaleFactor * 8f

    val bigRadiiList = listOf(
        floatArrayOf(concentricRadius, concentricRadius, sq, sq, sq, sq, sq, sq),
        floatArrayOf(sq, sq, concentricRadius, concentricRadius, sq, sq, sq, sq)
    )

    for (i in 0..1) {
        val rect = RectF(cardRect.left + pad + i * (bigW + gap), cardRect.top + pad, cardRect.left + pad + i * (bigW + gap) + bigW, cardRect.top + pad + topH)
        val slotConfig = folderConfig.slots.getOrElse(i) { AppSlotConfig() }
        val tilePath = Path().apply { addRoundRect(rect, bigRadiiList[i], Path.Direction.CW) }

        if (folderConfig.showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSlotContent(canvas, context, rect, slotConfig, folderConfig.showAppNames, isLight, scaleFactor, primaryText, secondaryText)
    }

    val bottomTop = cardRect.top + pad + topH + gap
    val microW = (cardRect.width() - (pad * 2f) - (gap * 3f)) / 4f
    val microH = (cardRect.bottom - pad - bottomTop - gap) / 2f

    for (i in 0..7) {
        val col = i % 4
        val row = i / 4
        val bl = if (col == 0 && row == 1) concentricRadius else sq
        val br = if (col == 3 && row == 1) concentricRadius else sq
        val radii = floatArrayOf(sq, sq, sq, sq, br, br, bl, bl)

        val rect = RectF(cardRect.left + pad + col * (microW + gap), bottomTop + row * (microH + gap), cardRect.left + pad + col * (microW + gap) + microW, bottomTop + row * (microH + gap) + microH)
        val slotConfig = folderConfig.slots.getOrElse(i + 2) { AppSlotConfig() }
        val tilePath = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }

        if (folderConfig.showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSlotContent(canvas, context, rect, slotConfig, false, isLight, scaleFactor, primaryText, secondaryText, isMicro = true)
    }
    return bitmap
}

fun generateAppFolderBento10TopBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val folderConfig = AppFolderWidgetConfig.load(context, widgetId, 10)
    return generateAppFolderBento10TopBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId)
}