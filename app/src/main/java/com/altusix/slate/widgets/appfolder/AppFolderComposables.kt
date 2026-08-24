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

// Universal App Folder Grid Generator (2x2, 4x2)
fun generateAppFolderGridBitmap(context: Context, config: SlateWidgetConfig, folderConfig: AppFolderWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val slotCount = folderConfig.slotCount
    val minDimension = (60 * scaleFactor).toInt()
    val w = (wDp * scaleFactor).toInt().coerceAtLeast(minDimension)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(minDimension)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    val targetRatio = when (slotCount) {
        4 -> 1.0f
        8 -> 2.0f
        else -> 1.0f
    }

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        var cardH = h.toFloat()
        var cardW = cardH * targetRatio
        if (cardW > w.toFloat()) {
            cardW = w.toFloat()
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val (cols, rows) = when (slotCount) {
        4 -> 2 to 2
        8 -> 4 to 2
        else -> 2 to 2
    }

    val pad = scaleFactor * 8f
    val gap = scaleFactor * 8f
    val availableW = cardRect.width() - (pad * 2f) - (gap * (cols - 1))
    val availableH = cardRect.height() - (pad * 2f) - (gap * (rows - 1))
    val tileW = availableW / cols
    val tileH = availableH / rows

    val innerCardRadius = (outerRadius - pad).coerceAtLeast(scaleFactor * 6f)
    val innerCardBg = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    for (i in 0 until slotCount) {
        val col = i % cols
        val row = i / cols

        val tileLeft = cardRect.left + pad + col * (tileW + gap)
        val tileTop = cardRect.top + pad + row * (tileH + gap)
        val tileRect = RectF(tileLeft, tileTop, tileLeft + tileW, tileTop + tileH)

        val slotConfig = folderConfig.slots.getOrElse(i) { AppSlotConfig() }

        canvas.save()
        val tilePath = Path().apply { addRoundRect(tileRect, innerCardRadius, innerCardRadius, Path.Direction.CW) }
        canvas.clipPath(tilePath)

        if (folderConfig.showTileBackground) {
            canvas.drawRoundRect(tileRect, innerCardRadius, innerCardRadius, tilePaint)
        }

        val iconRatio = if (folderConfig.showAppNames) 0.42f else 0.54f
        val iconSize = (minOf(tileW, tileH) * iconRatio).coerceAtLeast(scaleFactor * 14f)
        val iconCy = if (folderConfig.showAppNames) tileRect.top + (tileH * 0.38f) else tileRect.centerY()
        val textY = tileRect.bottom - (scaleFactor * 6f)

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

            if (folderConfig.showAppNames) {
                val maxTextWidth = tileW * 0.88f
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = secondaryText
                    typeface = getSlateFont(context, weight = 600)
                    textAlign = Paint.Align.CENTER
                }
                var fontSize = (tileH * 0.12f).coerceIn(scaleFactor * 7.5f, scaleFactor * 12f)
                textPaint.textSize = fontSize
                canvas.drawText("Add App", tileRect.centerX(), textY, textPaint)
            }
        } else {
            val appIcon = getAppIconBitmap(context, slotConfig.packageName, iconSize.toInt())
            if (appIcon != null) {
                val iconRect = RectF(tileRect.centerX() - (iconSize / 2f), iconCy - (iconSize / 2f), tileRect.centerX() + (iconSize / 2f), iconCy + (iconSize / 2f))
                canvas.drawBitmap(appIcon, null, iconRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            }

            if (folderConfig.showAppNames) {
                val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = primaryText
                    typeface = getSlateFont(context, weight = 600)
                    textAlign = Paint.Align.CENTER
                }
                val maxTextWidth = tileW * 0.88f
                var fontSize = (tileH * 0.13f).coerceIn(scaleFactor * 7.5f, scaleFactor * 13f)
                namePaint.textSize = fontSize
                while (namePaint.measureText(slotConfig.appName) > maxTextWidth && fontSize > scaleFactor * 6.5f) {
                    fontSize -= scaleFactor * 0.5f
                    namePaint.textSize = fontSize
                }
                canvas.drawText(slotConfig.appName, tileRect.centerX(), textY, namePaint)
            }
        }
        canvas.restore()
    }
    return bitmap
}

// 1. 4-APP FOLDER (2x2)
fun generateAppFolder4Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val folderConfig = AppFolderWidgetConfig.load(context, widgetId, 4)
    return generateAppFolderGridBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId)
}

// 2. 8-APP FOLDER (4x2)
fun generateAppFolder8Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val folderConfig = AppFolderWidgetConfig.load(context, widgetId, 8)
    return generateAppFolderGridBitmap(context, config, folderConfig, isResponsive, wDp, hDp, widgetId)
}