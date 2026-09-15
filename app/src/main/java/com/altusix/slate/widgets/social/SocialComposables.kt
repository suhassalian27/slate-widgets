package com.altusix.slate.widgets.social

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

/**
 * Loads high-res application icon from PackageManager as fallback.
 */
fun getAppIconBitmap(context: Context, packageName: String, size: Int): Bitmap? {
    if (packageName.isBlank()) return null
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

/**
 * Returns brand color for social presets.
 */
fun getSocialBrandColor(presetId: String, isLight: Boolean): Int {
    return when (presetId.lowercase()) {
        "instagram" -> Color.parseColor("#E4405F")
        "facebook" -> Color.parseColor("#1877F2")
        "x" -> if (isLight) Color.parseColor("#0F1419") else Color.parseColor("#E7E9EA")
        "discord" -> Color.parseColor("#5865F2")
        "reddit" -> Color.parseColor("#FF4500")
        "snapchat" -> Color.parseColor("#FFFC00")
        "pinterest" -> Color.parseColor("#BD081C")
        "messenger" -> Color.parseColor("#0084FF")
        "tiktok" -> Color.parseColor("#00F2FE")
        "twitch" -> Color.parseColor("#9146FF")
        "threads" -> if (isLight) Color.parseColor("#111111") else Color.parseColor("#EEEEEE")
        "whatsapp" -> Color.parseColor("#25D366")
        "telegram" -> Color.parseColor("#24A1DE")
        "youtube" -> Color.parseColor("#FF0000")
        "linkedin" -> Color.parseColor("#0A66C2")
        "spotify" -> Color.parseColor("#1DB954")
        else -> if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    }
}

/**
 * Vector rendering for 16 social platform glyphs.
 */
fun drawPresetVectorIcon(
    canvas: Canvas,
    context: Context,
    presetId: String,
    iconRect: RectF,
    glyphColor: Int,
    cutoutColor: Int
) {
    val l = iconRect.left
    val t = iconRect.top
    val w = iconRect.width()
    val h = iconRect.height()

    fun x(pct: Float): Float = l + w * pct
    fun y(pct: Float): Float = t + h * pct

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = glyphColor
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = glyphColor
        style = Paint.Style.STROKE
        strokeWidth = w * 0.085f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val cutoutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cutoutColor
        style = Paint.Style.FILL
    }

    when (presetId.lowercase()) {
        "instagram" -> {
            val outerRect = RectF(x(0.12f), y(0.12f), x(0.88f), y(0.88f))
            canvas.drawRoundRect(outerRect, w * 0.22f, w * 0.22f, strokePaint)
            canvas.drawCircle(iconRect.centerX(), iconRect.centerY(), w * 0.22f, strokePaint)
            canvas.drawCircle(x(0.70f), y(0.30f), w * 0.055f, fillPaint)
        }
        "facebook" -> {
            val fPath = Path().apply {
                moveTo(x(0.30f), y(0.44f))
                lineTo(x(0.70f), y(0.44f))
                moveTo(x(0.49f), y(0.88f))
                lineTo(x(0.49f), y(0.32f))
                quadTo(x(0.49f), y(0.14f), x(0.70f), y(0.14f))
            }
            val fbStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = glyphColor
                style = Paint.Style.STROKE
                strokeWidth = w * 0.15f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            canvas.drawPath(fPath, fbStroke)
        }
        "x" -> {
            val xPath = Path().apply {
                moveTo(x(0.18f), y(0.18f))
                lineTo(x(0.40f), y(0.18f))
                lineTo(x(0.82f), y(0.82f))
                lineTo(x(0.60f), y(0.82f))
                close()
                moveTo(x(0.82f), y(0.18f))
                lineTo(x(0.66f), y(0.18f))
                lineTo(x(0.18f), y(0.82f))
                lineTo(x(0.34f), y(0.82f))
                close()
            }
            canvas.drawPath(xPath, fillPaint)
        }
        "discord" -> {
            val dPath = Path().apply {
                moveTo(x(0.24f), y(0.28f))
                quadTo(x(0.50f), y(0.35f), x(0.76f), y(0.28f))
                quadTo(x(0.88f), y(0.44f), x(0.85f), y(0.72f))
                quadTo(x(0.74f), y(0.80f), x(0.63f), y(0.74f))
                lineTo(x(0.59f), y(0.67f))
                lineTo(x(0.41f), y(0.67f))
                lineTo(x(0.37f), y(0.74f))
                quadTo(x(0.26f), y(0.80f), x(0.15f), y(0.72f))
                quadTo(x(0.12f), y(0.44f), x(0.24f), y(0.28f))
                close()
            }
            canvas.drawPath(dPath, fillPaint)
            canvas.drawCircle(x(0.36f), y(0.51f), w * 0.08f, cutoutPaint)
            canvas.drawCircle(x(0.64f), y(0.51f), w * 0.08f, cutoutPaint)
        }
        "reddit" -> {
            val headRect = RectF(x(0.22f), y(0.36f), x(0.78f), y(0.76f))
            canvas.drawOval(headRect, fillPaint)
            canvas.drawCircle(x(0.20f), y(0.52f), w * 0.09f, fillPaint)
            canvas.drawCircle(x(0.80f), y(0.52f), w * 0.09f, fillPaint)
            // Antenna
            val antennaPath = Path().apply {
                moveTo(x(0.50f), y(0.36f))
                lineTo(x(0.58f), y(0.20f))
                lineTo(x(0.68f), y(0.22f))
            }
            val antPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = glyphColor
                style = Paint.Style.STROKE
                strokeWidth = w * 0.065f
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawPath(antennaPath, antPaint)
            canvas.drawCircle(x(0.72f), y(0.22f), w * 0.06f, fillPaint)
            // Eyes & smile
            canvas.drawCircle(x(0.38f), y(0.54f), w * 0.055f, cutoutPaint)
            canvas.drawCircle(x(0.62f), y(0.54f), w * 0.055f, cutoutPaint)
            val smilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = cutoutColor
                style = Paint.Style.STROKE
                strokeWidth = w * 0.05f
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawArc(RectF(x(0.42f), y(0.58f), x(0.58f), y(0.68f)), 20f, 140f, false, smilePaint)
        }
        "snapchat" -> {
            val gPath = Path().apply {
                moveTo(x(0.50f), y(0.18f))
                cubicTo(x(0.34f), y(0.18f), x(0.30f), y(0.32f), x(0.30f), y(0.44f))
                quadTo(x(0.18f), y(0.48f), x(0.18f), y(0.54f))
                quadTo(x(0.24f), y(0.58f), x(0.32f), y(0.54f))
                quadTo(x(0.30f), y(0.72f), x(0.22f), y(0.82f))
                quadTo(x(0.32f), y(0.84f), x(0.40f), y(0.80f))
                quadTo(x(0.50f), y(0.85f), x(0.60f), y(0.80f))
                quadTo(x(0.68f), y(0.84f), x(0.78f), y(0.82f))
                quadTo(x(0.70f), y(0.72f), x(0.68f), y(0.54f))
                quadTo(x(0.76f), y(0.58f), x(0.82f), y(0.54f))
                quadTo(x(0.82f), y(0.48f), x(0.70f), y(0.44f))
                cubicTo(x(0.70f), y(0.32f), x(0.66f), y(0.18f), x(0.50f), y(0.18f))
                close()
            }
            canvas.drawPath(gPath, fillPaint)
        }
        "pinterest" -> {
            canvas.drawCircle(iconRect.centerX(), iconRect.centerY(), w * 0.38f, strokePaint)
            val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = glyphColor
                style = Paint.Style.STROKE
                strokeWidth = w * 0.10f
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(x(0.47f), y(0.28f), x(0.41f), y(0.72f), stemPaint)
            val bowlRect = RectF(x(0.38f), y(0.28f), x(0.66f), y(0.52f))
            canvas.drawArc(bowlRect, -80f, 180f, false, stemPaint)
        }
        "messenger" -> {
            val bubbleRect = RectF(x(0.16f), y(0.16f), x(0.84f), y(0.74f))
            val bPath = Path().apply {
                addOval(bubbleRect, Path.Direction.CW)
                moveTo(x(0.34f), y(0.70f))
                lineTo(x(0.22f), y(0.84f))
                lineTo(x(0.44f), y(0.72f))
            }
            canvas.drawPath(bPath, fillPaint)
            val boltPath = Path().apply {
                moveTo(x(0.58f), y(0.32f))
                lineTo(x(0.36f), y(0.48f))
                lineTo(x(0.47f), y(0.48f))
                lineTo(x(0.42f), y(0.62f))
                lineTo(x(0.64f), y(0.44f))
                lineTo(x(0.53f), y(0.44f))
                close()
            }
            canvas.drawPath(boltPath, cutoutPaint)
        }
        "tiktok" -> {
            val notePath = Path().apply {
                val headRect = RectF(x(0.26f), y(0.56f), x(0.50f), y(0.76f))
                addOval(headRect, Path.Direction.CW)
                moveTo(x(0.47f), y(0.64f))
                lineTo(x(0.47f), y(0.22f))
                quadTo(x(0.60f), y(0.22f), x(0.74f), y(0.32f))
                lineTo(x(0.74f), y(0.44f))
                quadTo(x(0.60f), y(0.36f), x(0.47f), y(0.38f))
            }
            canvas.drawPath(notePath, fillPaint)
        }
        "twitch" -> {
            val tPath = Path().apply {
                moveTo(x(0.22f), y(0.18f))
                lineTo(x(0.78f), y(0.18f))
                lineTo(x(0.78f), y(0.64f))
                lineTo(x(0.62f), y(0.64f))
                lineTo(x(0.48f), y(0.78f))
                lineTo(x(0.48f), y(0.64f))
                lineTo(x(0.22f), y(0.64f))
                close()
            }
            canvas.drawPath(tPath, fillPaint)
            val slitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = cutoutColor
                style = Paint.Style.STROKE
                strokeWidth = w * 0.08f
                strokeCap = Paint.Cap.SQUARE
            }
            canvas.drawLine(x(0.42f), y(0.34f), x(0.42f), y(0.48f), slitPaint)
            canvas.drawLine(x(0.58f), y(0.34f), x(0.58f), y(0.48f), slitPaint)
        }
        "threads" -> {
            val tPath = Path().apply {
                val inner = RectF(x(0.36f), y(0.36f), x(0.64f), y(0.64f))
                addOval(inner, Path.Direction.CW)
                moveTo(x(0.62f), y(0.48f))
                arcTo(RectF(x(0.20f), y(0.20f), x(0.80f), y(0.80f)), 0f, 290f, false)
            }
            canvas.drawPath(tPath, strokePaint)
        }
        "whatsapp" -> {
            val bubbleRect = RectF(x(0.16f), y(0.16f), x(0.84f), y(0.76f))
            val bPath = Path().apply {
                addOval(bubbleRect, Path.Direction.CW)
                moveTo(x(0.32f), y(0.72f))
                lineTo(x(0.18f), y(0.86f))
                lineTo(x(0.42f), y(0.74f))
            }
            canvas.drawPath(bPath, fillPaint)
            val phonePath = Path().apply {
                moveTo(x(0.36f), y(0.38f))
                quadTo(x(0.46f), y(0.46f), x(0.54f), y(0.56f))
                lineTo(x(0.62f), y(0.64f))
            }
            val pPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = cutoutColor
                style = Paint.Style.STROKE
                strokeWidth = w * 0.12f
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawPath(phonePath, pPaint)
        }
        "telegram" -> {
            val planePath = Path().apply {
                moveTo(x(0.82f), y(0.22f))
                lineTo(x(0.18f), y(0.52f))
                lineTo(x(0.40f), y(0.64f))
                lineTo(x(0.48f), y(0.80f))
                lineTo(x(0.58f), y(0.70f))
                close()
            }
            canvas.drawPath(planePath, fillPaint)
        }
        "youtube" -> {
            val ytRect = RectF(x(0.14f), y(0.26f), x(0.86f), y(0.74f))
            canvas.drawRoundRect(ytRect, w * 0.14f, w * 0.14f, fillPaint)
            val triPath = Path().apply {
                moveTo(x(0.42f), y(0.38f))
                lineTo(x(0.64f), y(0.50f))
                lineTo(x(0.42f), y(0.62f))
                close()
            }
            canvas.drawPath(triPath, cutoutPaint)
        }
        "linkedin" -> {
            val inRect = RectF(x(0.16f), y(0.16f), x(0.84f), y(0.84f))
            canvas.drawRoundRect(inRect, w * 0.16f, w * 0.16f, fillPaint)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = cutoutColor
                textSize = w * 0.44f
                typeface = getSlateFont(context, weight = 800)
                textAlign = Paint.Align.CENTER
            }
            val b = Rect()
            textPaint.getTextBounds("in", 0, 2, b)
            canvas.drawText("in", inRect.centerX(), inRect.centerY() + (b.height() / 2f), textPaint)
        }
        "spotify" -> {
            canvas.drawCircle(iconRect.centerX(), iconRect.centerY(), w * 0.38f, fillPaint)
            val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = cutoutColor
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
            wavePaint.strokeWidth = w * 0.07f
            canvas.drawArc(RectF(x(0.30f), y(0.32f), x(0.70f), y(0.56f)), -145f, 110f, false, wavePaint)
            wavePaint.strokeWidth = w * 0.06f
            canvas.drawArc(RectF(x(0.33f), y(0.40f), x(0.67f), y(0.62f)), -145f, 110f, false, wavePaint)
            wavePaint.strokeWidth = w * 0.05f
            canvas.drawArc(RectF(x(0.36f), y(0.48f), x(0.64f), y(0.68f)), -145f, 110f, false, wavePaint)
        }
        else -> {
            // Default elegant social bubble
            canvas.drawCircle(iconRect.centerX(), iconRect.centerY(), w * 0.38f, strokePaint)
        }
    }
}

/**
 * Universal slot renderer: draws icon (vector or package icon), placeholder, or app name.
 */
private fun drawSocialSlot(
    canvas: Canvas,
    context: Context,
    tileRect: RectF,
    slotConfig: SocialSlotConfig,
    socialConfig: SocialWidgetConfig,
    isLight: Boolean,
    scaleFactor: Float,
    primaryText: Int,
    secondaryText: Int,
    accentColor: Int,
    tileBgColor: Int,
    isMicro: Boolean = false
) {
    val tileW = tileRect.width()
    val tileH = tileRect.height()
    val minDim = minOf(tileW, tileH)
    val showText = socialConfig.showAppNames && !isMicro && tileH >= scaleFactor * 32f

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
            canvas.drawText("Add", tileRect.centerX(), textY, textPaint)
        }
    } else {
        val iconRect = RectF(
            tileRect.centerX() - (iconSize / 2f),
            iconCy - (iconSize / 2f),
            tileRect.centerX() + (iconSize / 2f),
            iconCy + (iconSize / 2f)
        )

        // Resolve icon color
        val glyphColor = when (socialConfig.iconStyle) {
            "ACCENT" -> accentColor
            "ORIGINAL" -> getSocialBrandColor(slotConfig.presetId, isLight)
            else -> primaryText
        }

        val cutoutColor = if (socialConfig.showTileBackground) tileBgColor else (if (isLight) Color.WHITE else Color.BLACK)

        // Check if preset vector exists
        val isPreset = slotConfig.presetId.isNotBlank() && SocialStorageManager.getPresetById(slotConfig.presetId) != null

        if (socialConfig.iconStyle == "ORIGINAL" && !isPreset) {
            val appIcon = getAppIconBitmap(context, slotConfig.packageName, iconSize.toInt())
            if (appIcon != null) {
                canvas.drawBitmap(appIcon, null, iconRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            } else {
                drawPresetVectorIcon(canvas, context, slotConfig.presetId, iconRect, glyphColor, cutoutColor)
            }
        } else if (isPreset) {
            drawPresetVectorIcon(canvas, context, slotConfig.presetId, iconRect, glyphColor, cutoutColor)
        } else {
            val appIcon = getAppIconBitmap(context, slotConfig.packageName, iconSize.toInt())
            if (appIcon != null) {
                canvas.drawBitmap(appIcon, null, iconRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            } else {
                // Monogram fallback
                val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = glyphColor
                    textSize = iconSize * 0.55f
                    typeface = getSlateFont(context, weight = 700)
                    textAlign = Paint.Align.CENTER
                }
                val letter = slotConfig.appName.firstOrNull()?.uppercase() ?: "S"
                val bounds = Rect()
                monoPaint.getTextBounds(letter, 0, letter.length, bounds)
                canvas.drawText(letter, tileRect.centerX(), iconCy + (bounds.height() / 2f), monoPaint)
            }
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

/**
 * Universal Grid Layout Generator for Social widgets (1x1, 1x2, 3x1, 4x1, 5x1, 2x2, 4x2, 5x2, 3x3).
 */
fun generateSocialGridBitmap(
    context: Context,
    config: SlateWidgetConfig,
    socialConfig: SocialWidgetConfig,
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
    val accentColor = config.accentColorHex.toInt()

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

        val slot = socialConfig.slots.getOrElse(i) { SocialSlotConfig() }

        val tl = if (col == 0 && row == 0) concentricRadius else squircleRadius
        val tr = if (col == cols - 1 && row == 0) concentricRadius else squircleRadius
        val br = if (col == cols - 1 && row == rows - 1) concentricRadius else squircleRadius
        val bl = if (col == 0 && row == rows - 1) concentricRadius else squircleRadius

        val radii = floatArrayOf(tl, tl, tr, tr, br, br, bl, bl)
        val tilePath = Path().apply { addRoundRect(tileRect, radii, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(tilePath)

        if (socialConfig.showTileBackground) {
            canvas.drawPath(tilePath, tilePaint)
        }

        drawSocialSlot(
            canvas = canvas,
            context = context,
            tileRect = tileRect,
            slotConfig = slot,
            socialConfig = socialConfig,
            isLight = isLight,
            scaleFactor = scaleFactor,
            primaryText = primaryText,
            secondaryText = secondaryText,
            accentColor = accentColor,
            tileBgColor = innerCardBg,
            isMicro = cols >= 4 && rows >= 2
        )
        canvas.restore()
    }

    return bitmap
}

// ==========================================
// 12 Concrete Bitmap Generator Implementations
// ==========================================

// 1. Social Bar (5 Apps Row - 4x1 / 5x1)
fun generateSocialBar5Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val socialConfig = SocialStorageManager.load(context, widgetId, 5, "BAR_5")
    return generateSocialGridBitmap(context, config, socialConfig, isResponsive, wDp, hDp, widgetId, cols = 5, rows = 1)
}

// 2. Social Quad (4 Apps Grid - 2x2)
fun generateSocialQuad4Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val socialConfig = SocialStorageManager.load(context, widgetId, 4, "QUAD_4")
    return generateSocialGridBitmap(context, config, socialConfig, isResponsive, wDp, hDp, widgetId, cols = 2, rows = 2)
}

// 3. Social Matrix (9 Apps Grid - 3x3 / 2x2)
fun generateSocialMatrix9Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val socialConfig = SocialStorageManager.load(context, widgetId, 9, "MATRIX_9")
    return generateSocialGridBitmap(context, config, socialConfig, isResponsive, wDp, hDp, widgetId, cols = 3, rows = 3)
}

// 4. Social Deck (10 Apps 2x5 Grid - 4x2)
fun generateSocialDeck10Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val socialConfig = SocialStorageManager.load(context, widgetId, 10, "DECK_10")
    return generateSocialGridBitmap(context, config, socialConfig, isResponsive, wDp, hDp, widgetId, cols = 5, rows = 2)
}

// 5. Social Bento Top (10 Apps - 2 Large Top + 8 Small Bottom)
fun generateSocialBento10TopBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val socialConfig = SocialStorageManager.load(context, widgetId, 10, "BENTO_10_TOP")
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val accentColor = config.accentColorHex.toInt()

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
    val innerCardBg = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
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

    // Top 2 Big Tiles (Slots 0 and 1)
    for (i in 0..1) {
        val rect = RectF(cardRect.left + pad + i * (bigW + gap), cardRect.top + pad, cardRect.left + pad + i * (bigW + gap) + bigW, cardRect.top + pad + topH)
        val slot = socialConfig.slots.getOrElse(i) { SocialSlotConfig() }
        val tilePath = Path().apply { addRoundRect(rect, bigRadiiList[i], Path.Direction.CW) }

        if (socialConfig.showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSocialSlot(canvas, context, rect, slot, socialConfig, isLight, scaleFactor, primaryText, secondaryText, accentColor, innerCardBg, isMicro = false)
    }

    // Bottom 8 Small Tiles (Slots 2 to 9, 2 rows of 4)
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
        val slot = socialConfig.slots.getOrElse(i + 2) { SocialSlotConfig() }
        val tilePath = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }

        if (socialConfig.showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSocialSlot(canvas, context, rect, slot, socialConfig, isLight, scaleFactor, primaryText, secondaryText, accentColor, innerCardBg, isMicro = true)
    }

    return bitmap
}

// 6. Social Bento Left (10 Apps - 2 Large Left + 8 Small Right)
fun generateSocialBento10LeftBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val socialConfig = SocialStorageManager.load(context, widgetId, 10, "BENTO_10_LEFT")
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val accentColor = config.accentColorHex.toInt()

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
    val innerCardBg = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
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

    // Left 2 Big Tiles (Slots 0 and 1)
    for (i in 0..1) {
        val rect = RectF(cardRect.left + pad, cardRect.top + pad + i * (bigH + gap), cardRect.left + pad + leftW, cardRect.top + pad + i * (bigH + gap) + bigH)
        val slot = socialConfig.slots.getOrElse(i) { SocialSlotConfig() }
        val tilePath = Path().apply { addRoundRect(rect, bigRadiiList[i], Path.Direction.CW) }

        if (socialConfig.showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSocialSlot(canvas, context, rect, slot, socialConfig, isLight, scaleFactor, primaryText, secondaryText, accentColor, innerCardBg, isMicro = false)
    }

    // Right 8 Small Tiles (Slots 2 to 9, 4 rows of 2)
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
        val slot = socialConfig.slots.getOrElse(i + 2) { SocialSlotConfig() }
        val tilePath = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }

        if (socialConfig.showTileBackground) canvas.drawPath(tilePath, tilePaint)
        drawSocialSlot(canvas, context, rect, slot, socialConfig, isLight, scaleFactor, primaryText, secondaryText, accentColor, innerCardBg, isMicro = true)
    }

    return bitmap
}

// 7. Social Orbit (6 Apps Circular Orbit Dial - 2x2)
fun generateSocialOrbit6Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val socialConfig = SocialStorageManager.load(context, widgetId, 6, "ORBIT_6")
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val accentColor = config.accentColorHex.toInt()

    val margin = scaleFactor * 1.5f
    val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f
    val cardRect = RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    val cx = cardRect.centerX()
    val cy = cardRect.centerY()

    val outerRadius = cardSize / 2f
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, outerRadius, bgPaint)

    val orbitRadius = outerRadius * 0.60f
    val tileRadius = outerRadius * 0.38f

    // Subtle Orbit Track
    val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(16, 0, 0, 0) else Color.argb(22, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.8f
    }
    canvas.drawCircle(cx, cy, orbitRadius, guidePaint)

    // Frosted Center Hub
    val hubRadius = outerRadius * 0.14f
    val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(18, 0, 0, 0) else Color.argb(26, 255, 255, 255)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, hubRadius, hubPaint)

    val innerCardBg = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
    val tileBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    for (i in 0 until 6) {
        val angle = Math.toRadians((i * 60.0) - 90.0)
        val slotX = cx + (orbitRadius * Math.cos(angle)).toFloat()
        val slotY = cy + (orbitRadius * Math.sin(angle)).toFloat()

        val tileRect = RectF(slotX - tileRadius, slotY - tileRadius, slotX + tileRadius, slotY + tileRadius)
        val slot = socialConfig.slots.getOrElse(i) { SocialSlotConfig() }

        if (socialConfig.showTileBackground) {
            canvas.drawCircle(slotX, slotY, tileRadius * 0.75f, tileBgPaint)
        }

        drawSocialSlot(
            canvas = canvas,
            context = context,
            tileRect = tileRect,
            slotConfig = slot,
            socialConfig = socialConfig,
            isLight = isLight,
            scaleFactor = scaleFactor,
            primaryText = primaryText,
            secondaryText = secondaryText,
            accentColor = accentColor,
            tileBgColor = innerCardBg,
            isMicro = false
        )
    }

    return bitmap
}

// 8. Social Direct Messaging (4 Messaging Apps Dock - 4x1)
fun generateSocialMessaging4Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val socialConfig = SocialStorageManager.load(context, widgetId, 4, "MESSAGING_4")
    return generateSocialGridBitmap(context, config, socialConfig, isResponsive, wDp, hDp, widgetId, cols = 4, rows = 1)
}

// 9. Social Stream (3 Apps Horizontal Dock - 3x1)
fun generateSocialStream3Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val socialConfig = SocialStorageManager.load(context, widgetId, 3, "STREAM_3")
    return generateSocialGridBitmap(context, config, socialConfig, isResponsive, wDp, hDp, widgetId, cols = 3, rows = 1)
}

// 10. Social Octa Deck (8 Apps Grid - 4x2)
fun generateSocialOcta8Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val socialConfig = SocialStorageManager.load(context, widgetId, 8, "OCTA_8")
    return generateSocialGridBitmap(context, config, socialConfig, isResponsive, wDp, hDp, widgetId, cols = 4, rows = 2)
}

// 11. Social Twin Column (2 Apps Vertical Pill - 1x2)
fun generateSocialTwin2Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val socialConfig = SocialStorageManager.load(context, widgetId, 2, "TWIN_2")
    return generateSocialGridBitmap(context, config, socialConfig, isResponsive, wDp, hDp, widgetId, cols = 1, rows = 2)
}

// 12. Micro Social Launcher (1 App Single Tile - 1x1)
fun generateSocialMicro1Bitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val socialConfig = SocialStorageManager.load(context, widgetId, 1, "MICRO_1")
    return generateSocialGridBitmap(context, config, socialConfig, isResponsive, wDp, hDp, widgetId, cols = 1, rows = 1)
}
