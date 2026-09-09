package com.altusix.slate.widgets.photos

import android.content.Context
import android.graphics.*
import androidx.compose.ui.graphics.toArgb
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.createSupersampledCanvas
import com.altusix.slate.utils.getSafeBgColor
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =========================================================================
// CANVAS BITMAP GENERATORS FOR SLATE "PHOTOS & MEMORIES" WIDGETS
// =========================================================================

fun getFilterColorMatrix(style: MemoryFilterStyle): ColorMatrixColorFilter? {
    return when (style) {
        MemoryFilterStyle.ORIGINAL -> null
        MemoryFilterStyle.MONOCHROME -> {
            val matrix = ColorMatrix().apply { setSaturation(0f) }
            ColorMatrixColorFilter(matrix)
        }
        MemoryFilterStyle.WARM_SEPIA -> {
            val matrix = ColorMatrix()
            matrix.setSaturation(0.25f)
            val sepia = ColorMatrix(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 15f,
                0.349f, 0.686f, 0.168f, 0f, 10f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.postConcat(sepia)
            ColorMatrixColorFilter(matrix)
        }
        MemoryFilterStyle.MOODY_DARK -> {
            val matrix = ColorMatrix(floatArrayOf(
                1.15f, 0f, 0f, 0f, -20f,
                0f, 1.15f, 0f, 0f, -20f,
                0f, 0f, 1.25f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            ))
            ColorMatrixColorFilter(matrix)
        }
        MemoryFilterStyle.RETRO_FILM -> {
            val matrix = ColorMatrix(floatArrayOf(
                0.92f, 0.06f, 0.04f, 0f, 22f,
                0.03f, 0.94f, 0.03f, 0f, 16f,
                0.03f, 0.06f, 0.82f, 0f, 20f,
                0f, 0f, 0f, 1f, 0f
            ))
            ColorMatrixColorFilter(matrix)
        }
        MemoryFilterStyle.GOLDEN_HOUR -> {
            val matrix = ColorMatrix(floatArrayOf(
                1.22f, 0f, 0f, 0f, 20f,
                0f, 1.10f, 0f, 0f, 12f,
                0f, 0f, 0.80f, 0f, -15f,
                0f, 0f, 0f, 1f, 0f
            ))
            ColorMatrixColorFilter(matrix)
        }
    }
}

private fun drawPhotoSurface(
    canvas: Canvas,
    item: SlateMemoryItem?,
    bounds: RectF,
    cornerRadius: Float,
    scaleFactor: Float,
    isLight: Boolean,
    accentColor: Int,
    fallbackSeed: Int = 0
) {
    val filter = item?.filterStyle ?: MemoryFilterStyle.ORIGINAL
    val colorFilter = getFilterColorMatrix(filter)

    val userBitmap = item?.imagePath?.let { PhotosStorageManager.loadBitmap(it) }

    if (userBitmap != null) {
        val shader = BitmapShader(userBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val matrix = Matrix()
        val bw = userBitmap.width.toFloat()
        val bh = userBitmap.height.toFloat()
        val scale = maxOf(bounds.width() / bw, bounds.height() / bh)
        val dx = bounds.left + (bounds.width() - bw * scale) / 2f
        val dy = bounds.top + (bounds.height() - bh * scale) / 2f
        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, dy)
        shader.setLocalMatrix(matrix)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.shader = shader
            this.colorFilter = colorFilter
        }
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, paint)
    } else {
        drawProceduralScenery(canvas, bounds, cornerRadius, scaleFactor, isLight, accentColor, fallbackSeed, colorFilter)
    }
}

private fun drawProceduralScenery(
    canvas: Canvas,
    bounds: RectF,
    cornerRadius: Float,
    scaleFactor: Float,
    isLight: Boolean,
    accentColor: Int,
    seed: Int,
    colorFilter: ColorFilter?
) {
    canvas.save()
    val clipPath = Path().apply {
        addRoundRect(bounds, cornerRadius, cornerRadius, Path.Direction.CW)
    }
    canvas.clipPath(clipPath)

    val (topColor, midColor, botColor) = when (seed % 3) {
        0 -> Triple(0xFF181B28.toInt(), 0xFF353C58.toInt(), 0xFF655268.toInt())
        1 -> Triple(0xFF22161A.toInt(), 0xFF582D33.toInt(), 0xFFB36746.toInt())
        else -> Triple(0xFF102027.toInt(), 0xFF24444F.toInt(), 0xFF4C7B8B.toInt())
    }

    val skyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            bounds.left, bounds.top,
            bounds.left, bounds.bottom,
            intArrayOf(topColor, midColor, botColor),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        this.colorFilter = colorFilter
    }
    canvas.drawRect(bounds, skyPaint)

    val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = when (seed % 3) {
            0 -> 0xFFF0F3FA.toInt()
            1 -> 0xFFFFDF85.toInt()
            else -> 0xFFE0F7FA.toInt()
        }
        this.colorFilter = colorFilter
    }
    val sunRadius = bounds.width() * 0.14f
    val sunX = bounds.left + bounds.width() * (if (seed % 2 == 0) 0.72f else 0.32f)
    val sunY = bounds.top + bounds.height() * 0.38f

    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            sunX, sunY, sunRadius * 2.2f,
            intArrayOf(Color.argb(90, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        this.colorFilter = colorFilter
    }
    canvas.drawCircle(sunX, sunY, sunRadius * 2.2f, glowPaint)
    canvas.drawCircle(sunX, sunY, sunRadius, sunPaint)

    val mountainPaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = when (seed % 3) {
            0 -> 0x882A2E44.toInt()
            1 -> 0x9948252C.toInt()
            else -> 0x881E3842.toInt()
        }
        style = Paint.Style.FILL
        this.colorFilter = colorFilter
    }
    val path1 = Path().apply {
        moveTo(bounds.left, bounds.bottom)
        lineTo(bounds.left, bounds.top + bounds.height() * 0.62f)
        lineTo(bounds.left + bounds.width() * 0.35f, bounds.top + bounds.height() * 0.48f)
        lineTo(bounds.left + bounds.width() * 0.65f, bounds.top + bounds.height() * 0.66f)
        lineTo(bounds.left + bounds.width() * 0.88f, bounds.top + bounds.height() * 0.52f)
        lineTo(bounds.right, bounds.top + bounds.height() * 0.58f)
        lineTo(bounds.right, bounds.bottom)
        close()
    }
    canvas.drawPath(path1, mountainPaint1)

    val mountainPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = when (seed % 3) {
            0 -> 0xFF141724.toInt()
            1 -> 0xFF2A151B.toInt()
            else -> 0xFF0E1A20.toInt()
        }
        style = Paint.Style.FILL
        this.colorFilter = colorFilter
    }
    val path2 = Path().apply {
        moveTo(bounds.left, bounds.bottom)
        lineTo(bounds.left, bounds.top + bounds.height() * 0.75f)
        lineTo(bounds.left + bounds.width() * 0.22f, bounds.top + bounds.height() * 0.58f)
        lineTo(bounds.left + bounds.width() * 0.52f, bounds.top + bounds.height() * 0.78f)
        lineTo(bounds.left + bounds.width() * 0.82f, bounds.top + bounds.height() * 0.60f)
        lineTo(bounds.right, bounds.top + bounds.height() * 0.72f)
        lineTo(bounds.right, bounds.bottom)
        close()
    }
    canvas.drawPath(path2, mountainPaint2)

    val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.4f * scaleFactor
    }
    val iconX = bounds.right - 18f * scaleFactor
    val iconY = bounds.top + 18f * scaleFactor
    val r = 5.5f * scaleFactor
    canvas.drawCircle(iconX, iconY, r, iconPaint)
    canvas.drawCircle(iconX, iconY, r * 0.45f, iconPaint)

    canvas.restore()
}

private fun truncateText(text: String, maxWidth: Float, paint: Paint): String {
    if (paint.measureText(text) <= maxWidth) return text
    var truncated = text
    while (truncated.isNotEmpty() && paint.measureText("$truncated…") > maxWidth) {
        truncated = truncated.dropLast(1)
    }
    return if (truncated.isEmpty()) "" else "$truncated…"
}

fun getPhotoCaptionTypeface(context: Context, font: CaptionFont): Typeface {
    return when (font) {
        CaptionFont.SANS -> getSlateFont(context, weight = 700)
        CaptionFont.SERIF -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
        CaptionFont.MONO -> Typeface.MONOSPACE
        CaptionFont.SCRIPT -> try {
            val cursive = Typeface.create("cursive", Typeface.BOLD)
            if (cursive != Typeface.DEFAULT) cursive else Typeface.create("casual", Typeface.BOLD)
        } catch (_: Exception) {
            Typeface.create("casual", Typeface.BOLD)
        }
    }
}

// =========================================================================
// 1. POLAROID MEMORY (2x2)
// =========================================================================
fun generatePolaroidMemoryBitmap(
    context: Context,
    item: SlateMemoryItem?,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    showCaption: Boolean = true
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        // Locked square aspect in fixed mode
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val isLight = slateConfig.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(slateConfig)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val alphaInt = (slateConfig.opacity.coerceIn(0f, 1f) * 255).toInt()
    val cardBg = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
    val frameBorderColor = if (isLight) 0x22000000 else 0x22FFFFFF
    val cardCorner = getStandardCornerRadius(scaleFactor)

    // 1. Background Card
    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cardBg
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCorner, cardCorner, cardPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = frameBorderColor
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, cardCorner, cardCorner, borderPaint)

    // 2. Uniform Border Padding (Top, Left, Right match identically)
    // Anchored to shorter dimension so stretching doesn't inflate side gutters
    val minDimension = minOf(cardRect.width(), cardRect.height())
    val uniformBorderMargin = (minDimension * 0.065f).coerceAtLeast(10f * scaleFactor)

    // Chin height scales proportionally with widget height
    val chinHeight = if (showCaption) {
        (cardRect.height() * 0.24f).coerceAtLeast(uniformBorderMargin * 2.2f)
    } else {
        uniformBorderMargin // Equal margin all around when caption is disabled
    }

    val photoBounds = RectF(
        cardRect.left + uniformBorderMargin,
        cardRect.top + uniformBorderMargin,
        cardRect.right - uniformBorderMargin,
        cardRect.bottom - chinHeight
    )

    // Inner photo corner radius concentric to card corner
    val photoCorner = (cardCorner - uniformBorderMargin * 0.5f).coerceAtLeast(4f * scaleFactor)
    drawPhotoSurface(
        canvas = canvas,
        item = item,
        bounds = photoBounds,
        cornerRadius = photoCorner,
        scaleFactor = scaleFactor,
        isLight = isLight,
        accentColor = accentColor,
        fallbackSeed = 0
    )

    val photoInnerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x1A000000 else 0x33000000
        style = Paint.Style.STROKE
        strokeWidth = 1f * scaleFactor
    }
    canvas.drawRoundRect(photoBounds, photoCorner, photoCorner, photoInnerBorder)

    // 3. Top Washi Tape Strip
    val tapeW = (minDimension * 0.30f).coerceAtLeast(36f * scaleFactor)
    val tapeH = (uniformBorderMargin * 0.85f).coerceAtLeast(8f * scaleFactor)
    val tapeRect = RectF(
        cardRect.centerX() - tapeW / 2f,
        cardRect.top + (uniformBorderMargin - tapeH) / 2f,
        cardRect.centerX() + tapeW / 2f,
        cardRect.top + (uniformBorderMargin + tapeH) / 2f
    )
    val tapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(140, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(tapeRect, 3f * scaleFactor, 3f * scaleFactor, tapePaint)

    // 4. Proportional Typography (Scales with widget & chin size)
    if (showCaption) {
        val captionText = item?.caption?.trim() ?: "Summer Memories"
        val dateText = item?.dateText?.trim() ?: "September 2024"

        val font = item?.captionFont ?: CaptionFont.SANS
        val captionTypeface = getPhotoCaptionTypeface(context, font)

        val rawColorHex = item?.captionColorHex ?: (if (isLight) 0xFF1C1C1EL else 0xFFF2F2F7L)
        var resolvedTextColor = rawColorHex.toInt()

        if (isLight && (resolvedTextColor == Color.WHITE || (resolvedTextColor and 0x00FFFFFF) == 0x00FFFFFF)) {
            resolvedTextColor = Color.parseColor("#121214")
        }

        // Font size proportional to chin height and widget size
        val captionFontSize = (chinHeight * 0.28f).coerceIn(11f * scaleFactor, 26f * scaleFactor)
        val dateFontSize = (captionFontSize * 0.76f).coerceAtLeast(8.5f * scaleFactor)

        val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = captionTypeface
            this.textSize = captionFontSize
            this.color = resolvedTextColor
        }

        val maxTextW = photoBounds.width() - 4f * scaleFactor
        val truncatedCaption = truncateText(captionText, maxTextW, captionPaint)

        // Center the text block vertically within the chin space
        val totalTextGap = 6f * scaleFactor
        val captionY = photoBounds.bottom + (chinHeight * 0.44f)

        if (truncatedCaption.isNotBlank()) {
            canvas.drawText(truncatedCaption, photoBounds.left + 2f * scaleFactor, captionY, captionPaint)
        }

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = getSlateFont(context, weight = 500)
            this.textSize = dateFontSize
            this.color = accentColor
        }
        val truncatedDate = truncateText(dateText, maxTextW, datePaint)
        if (truncatedDate.isNotBlank()) {
            canvas.drawText(truncatedDate, photoBounds.left + 2f * scaleFactor, captionY + dateFontSize + totalTextGap, datePaint)
        }
    }

    return bitmap
}

// =========================================================================
// 2. ON THIS DAY (TIME MACHINE) (4x2)
// =========================================================================
fun generateOnThisDayBitmap(
    context: Context,
    item: SlateMemoryItem?,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val aspect = 2.05f
        val cardW = minOf(w, h * aspect)
        val cardH = cardW / aspect
        RectF((w - cardW) / 2f, (h - cardH) / 2f, (w + cardW) / 2f, (h + cardH) / 2f)
    }

    val bgColor = getSafeBgColor(slateConfig)
    val cornerRadius = getStandardCornerRadius(scaleFactor)
    val isLight = slateConfig.themeMode == "LIGHT"
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val alphaInt = (slateConfig.opacity.coerceIn(0f, 1f) * 255).toInt()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val pad = 12f * scaleFactor
    val innerRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.bottom - pad)

    val photoW = innerRect.width() * 0.44f
    val photoBounds = RectF(innerRect.left, innerRect.top, innerRect.left + photoW, innerRect.bottom)
    val photoCorner = 14f * scaleFactor

    drawPhotoSurface(
        canvas = canvas,
        item = item,
        bounds = photoBounds,
        cornerRadius = photoCorner,
        scaleFactor = scaleFactor,
        isLight = isLight,
        accentColor = accentColor,
        fallbackSeed = 1
    )

    val photoBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x1A000000 else 0x22FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(photoBounds, photoCorner, photoCorner, photoBorder)

    val rightLeft = photoBounds.right + 14f * scaleFactor
    val maxTextW = innerRect.right - rightLeft

    var curY = innerRect.top + 14f * scaleFactor
    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        alpha = if (isLight) 35 else 50
        style = Paint.Style.FILL
    }
    val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 700)
        textSize = 8.5f * scaleFactor
        color = accentColor
    }
    val pillLabel = "ON THIS DAY"
    val pillLabelW = pillTextPaint.measureText(pillLabel)
    val pillH = 16f * scaleFactor
    val pillPadX = 8f * scaleFactor
    val pillRect = RectF(rightLeft, curY - 10f * scaleFactor, rightLeft + pillLabelW + pillPadX * 2, curY - 10f * scaleFactor + pillH)
    canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillPaint)
    canvas.drawText(pillLabel, rightLeft + pillPadX, curY + 1.5f * scaleFactor, pillTextPaint)

    curY += 24f * scaleFactor
    val titleColor = if (isLight) 0xFF1C1C1E.toInt() else 0xFFF2F2F7.toInt()
    val headlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 800)
        textSize = 15f * scaleFactor
        color = titleColor
    }
    val headlineText = "1 YEAR AGO"
    canvas.drawText(headlineText, rightLeft, curY, headlinePaint)

    curY += 16f * scaleFactor
    val metaColor = if (isLight) 0xFF636366.toInt() else 0xFF98989D.toInt()
    val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        textSize = 10f * scaleFactor
        color = metaColor
    }
    val dateText = item?.dateText ?: "September 8, 2024"
    val locText = item?.location?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""
    val fullMeta = truncateText("$dateText$locText", maxTextW, metaPaint)
    canvas.drawText(fullMeta, rightLeft, curY, metaPaint)

    curY += 18f * scaleFactor
    val captionColor = if (isLight) 0xFF3A3A3C.toInt() else 0xFFD1D1D6.toInt()
    val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 400, isItalic = true)
        textSize = 11f * scaleFactor
        color = captionColor
    }
    val caption = item?.caption ?: "Golden hour horizon over the cliffs"
    val quoteFormatted = truncateText("“$caption”", maxTextW, quotePaint)
    canvas.drawText(quoteFormatted, rightLeft, curY, quotePaint)

    return bitmap
}

// =========================================================================
// 3. 35mm FILM STRIP (4x2)
// =========================================================================
fun generateFilmStripBitmap(
    context: Context,
    items: List<SlateMemoryItem>,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val aspect = 2.1f
        val cardW = minOf(w, h * aspect)
        val cardH = cardW / aspect
        RectF((w - cardW) / 2f, (h - cardH) / 2f, (w + cardW) / 2f, (h + cardH) / 2f)
    }

    val bgColor = getSafeBgColor(slateConfig)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val alphaInt = (slateConfig.opacity.coerceIn(0f, 1f) * 255).toInt()
    val isLight = slateConfig.themeMode == "LIGHT"

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    val corner = getStandardCornerRadius(scaleFactor)
    canvas.drawRoundRect(cardRect, corner, corner, bgPaint)

    val sprocketTrackH = cardRect.height() * 0.16f
    val sprockW = 8.5f * scaleFactor
    val sprockH = 5.5f * scaleFactor
    val sprockCorner = 1.8f * scaleFactor
    val sprockGap = 13.5f * scaleFactor

    val sprocketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x33000000 else 0x33FFFFFF
        style = Paint.Style.FILL
    }

    var sx = cardRect.left + 10f * scaleFactor
    while (sx + sprockW < cardRect.right - 8f * scaleFactor) {
        val topHole = RectF(sx, cardRect.top + (sprocketTrackH - sprockH) / 2f, sx + sprockW, cardRect.top + (sprocketTrackH + sprockH) / 2f)
        canvas.drawRoundRect(topHole, sprockCorner, sprockCorner, sprocketPaint)

        val botY = cardRect.bottom - sprocketTrackH
        val botHole = RectF(sx, botY + (sprocketTrackH - sprockH) / 2f, sx + sprockW, botY + (sprocketTrackH + sprockH) / 2f)
        canvas.drawRoundRect(botHole, sprockCorner, sprockCorner, sprocketPaint)

        sx += sprockW + sprockGap
    }

    // Markings dynamically bound to Theme Studio Accent Color
    val accentMarkingsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 700)
        textSize = 7.5f * scaleFactor
        color = accentColor
    }
    canvas.drawText("SLATE 400 • 36 EXP", cardRect.left + 16f * scaleFactor, cardRect.top + sprocketTrackH - 2f * scaleFactor, accentMarkingsPaint)

    val photoAreaTop = cardRect.top + sprocketTrackH
    val photoAreaBottom = cardRect.bottom - sprocketTrackH

    val frameCount = 3
    val padH = 8f * scaleFactor
    val availableW = cardRect.width() - padH * 2
    val frameGap = 6f * scaleFactor
    val frameW = (availableW - frameGap * (frameCount - 1)) / frameCount

    val frameLabels = listOf("▸ 01A", "▸ 02A", "▸ 03A")
    val defaultList = if (items.isNotEmpty()) items else PhotosWidgetConfig.getDefaultConfig().items

    for (i in 0 until frameCount) {
        val fx = cardRect.left + padH + i * (frameW + frameGap)
        val frameRect = RectF(fx, photoAreaTop + 3f * scaleFactor, fx + frameW, photoAreaBottom - 10f * scaleFactor)
        val item = defaultList.getOrNull(i)

        drawPhotoSurface(
            canvas = canvas,
            item = item,
            bounds = frameRect,
            cornerRadius = 4f * scaleFactor,
            scaleFactor = scaleFactor,
            isLight = isLight,
            accentColor = accentColor,
            fallbackSeed = i
        )

        val frameBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) 0x22000000 else 0x33FFFFFF
            style = Paint.Style.STROKE
            strokeWidth = 1f * scaleFactor
        }
        canvas.drawRoundRect(frameRect, 4f * scaleFactor, 4f * scaleFactor, frameBorder)

        val numY = photoAreaBottom - 1f * scaleFactor
        canvas.drawText(frameLabels[i], fx + 2f * scaleFactor, numY, accentMarkingsPaint)
    }

    return bitmap
}

// =========================================================================
// 4. BENTO COLLAGE (4x2)
// =========================================================================
fun generateCollageBentoBitmap(
    context: Context,
    items: List<SlateMemoryItem>,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val aspect = 2.05f
        val cardW = minOf(w, h * aspect)
        val cardH = cardW / aspect
        RectF((w - cardW) / 2f, (h - cardH) / 2f, (w + cardW) / 2f, (h + cardH) / 2f)
    }

    val bgColor = getSafeBgColor(slateConfig)
    val cornerRadius = getStandardCornerRadius(scaleFactor)
    val isLight = slateConfig.themeMode == "LIGHT"
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val alphaInt = (slateConfig.opacity.coerceIn(0f, 1f) * 255).toInt()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val pad = 10f * scaleFactor
    val gap = 7f * scaleFactor
    val inner = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.bottom - pad)

    val defaultList = if (items.isNotEmpty()) items else PhotosWidgetConfig.getDefaultConfig().items
    val tileCorner = 14f * scaleFactor

    val heroW = (inner.width() - gap) * 0.54f
    val heroRect = RectF(inner.left, inner.top, inner.left + heroW, inner.bottom)
    val heroItem = defaultList.getOrNull(0)

    drawPhotoSurface(
        canvas = canvas,
        item = heroItem,
        bounds = heroRect,
        cornerRadius = tileCorner,
        scaleFactor = scaleFactor,
        isLight = isLight,
        accentColor = accentColor,
        fallbackSeed = 0
    )

    val pillH = 22f * scaleFactor
    val pillPad = 6f * scaleFactor
    val pillRect = RectF(heroRect.left + pillPad, heroRect.bottom - pillH - pillPad, heroRect.right - pillPad, heroRect.bottom - pillPad)
    val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xAA000000.toInt()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillBg)

    val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 600)
        textSize = 9.5f * scaleFactor
        color = android.graphics.Color.WHITE
    }
    val heroCaption = heroItem?.caption ?: "Featured Memory"
    val truncatedHeroText = truncateText(heroCaption, pillRect.width() - 14f * scaleFactor, pillTextPaint)
    canvas.drawText(truncatedHeroText, pillRect.left + 8f * scaleFactor, pillRect.centerY() + 3.5f * scaleFactor, pillTextPaint)

    val rightX = heroRect.right + gap
    val subW = inner.right - rightX
    val subH = (inner.height() - gap) / 2f

    val topSubRect = RectF(rightX, inner.top, inner.right, inner.top + subH)
    val sub1Item = defaultList.getOrNull(1)
    drawPhotoSurface(
        canvas = canvas,
        item = sub1Item,
        bounds = topSubRect,
        cornerRadius = tileCorner,
        scaleFactor = scaleFactor,
        isLight = isLight,
        accentColor = accentColor,
        fallbackSeed = 1
    )

    val botSubRect = RectF(rightX, topSubRect.bottom + gap, inner.right, inner.bottom)
    val sub2Item = defaultList.getOrNull(2)
    drawPhotoSurface(
        canvas = canvas,
        item = sub2Item,
        bounds = botSubRect,
        cornerRadius = tileCorner,
        scaleFactor = scaleFactor,
        isLight = isLight,
        accentColor = accentColor,
        fallbackSeed = 2
    )

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x1A000000 else 0x22FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(heroRect, tileCorner, tileCorner, strokePaint)
    canvas.drawRoundRect(topSubRect, tileCorner, tileCorner, strokePaint)
    canvas.drawRoundRect(botSubRect, tileCorner, tileCorner, strokePaint)

    return bitmap
}

// =========================================================================
// 5. PHOTO CAROUSEL SLIDESHOW (2x2)
// =========================================================================
fun generatePhotoCarouselBitmap(
    context: Context,
    config: PhotosWidgetConfig,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val bgColor = getSafeBgColor(slateConfig)
    val cornerRadius = getStandardCornerRadius(scaleFactor)
    val isLight = slateConfig.themeMode == "LIGHT"
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val alphaInt = (slateConfig.opacity.coerceIn(0f, 1f) * 255).toInt()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val pad = 10f * scaleFactor
    val inner = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.bottom - pad)

    val navDeckH = 26f * scaleFactor
    val photoBounds = RectF(inner.left, inner.top, inner.right, inner.bottom - navDeckH - 4f * scaleFactor)
    val photoCorner = 14f * scaleFactor

    val item = config.currentItem
    val totalCount = maxOf(config.items.size, 1)
    val activeIdx = config.currentIndex.coerceIn(0, totalCount - 1)

    drawPhotoSurface(
        canvas = canvas,
        item = item,
        bounds = photoBounds,
        cornerRadius = photoCorner,
        scaleFactor = scaleFactor,
        isLight = isLight,
        accentColor = accentColor,
        fallbackSeed = activeIdx
    )

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            photoBounds.left, photoBounds.bottom - 40f * scaleFactor,
            photoBounds.left, photoBounds.bottom,
            intArrayOf(Color.TRANSPARENT, Color.argb(160, 0, 0, 0)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.save()
    val clipP = Path().apply { addRoundRect(photoBounds, photoCorner, photoCorner, Path.Direction.CW) }
    canvas.clipPath(clipP)
    canvas.drawRect(photoBounds, shadowPaint)

    val caption = item?.caption ?: "Summer Memories"
    val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 600)
        textSize = 10f * scaleFactor
        color = android.graphics.Color.WHITE
    }
    val truncCap = truncateText(caption, photoBounds.width() - 16f * scaleFactor, capPaint)
    canvas.drawText(truncCap, photoBounds.left + 8f * scaleFactor, photoBounds.bottom - 8f * scaleFactor, capPaint)
    canvas.restore()

    val deckY = inner.bottom - navDeckH / 2f
    val btnRadius = 11f * scaleFactor
    val leftBtnX = inner.left + btnRadius + 4f * scaleFactor
    val rightBtnX = inner.right - btnRadius - 4f * scaleFactor

    val btnBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x1A000000 else 0x26FFFFFF
        style = Paint.Style.FILL
    }
    canvas.drawCircle(leftBtnX, deckY, btnRadius, btnBgPaint)
    canvas.drawCircle(rightBtnX, deckY, btnRadius, btnBgPaint)

    val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0xFF333336.toInt() else 0xFFF2F2F7.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * scaleFactor
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val lPath = Path().apply {
        moveTo(leftBtnX + 2f * scaleFactor, deckY - 4.5f * scaleFactor)
        lineTo(leftBtnX - 2.5f * scaleFactor, deckY)
        lineTo(leftBtnX + 2f * scaleFactor, deckY + 4.5f * scaleFactor)
    }
    canvas.drawPath(lPath, arrowPaint)

    val rPath = Path().apply {
        moveTo(rightBtnX - 2f * scaleFactor, deckY - 4.5f * scaleFactor)
        lineTo(rightBtnX + 2.5f * scaleFactor, deckY)
        lineTo(rightBtnX - 2f * scaleFactor, deckY + 4.5f * scaleFactor)
    }
    canvas.drawPath(rPath, arrowPaint)

    val dotCount = minOf(totalCount, 5)
    val dotGap = 7f * scaleFactor
    val totalDotsW = (dotCount - 1) * dotGap
    var dotX = inner.centerX() - totalDotsW / 2f

    val activeDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }
    val inactiveDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x44000000 else 0x44FFFFFF
        style = Paint.Style.FILL
    }

    for (d in 0 until dotCount) {
        val r = if (d == activeIdx % dotCount) 3.2f * scaleFactor else 2.2f * scaleFactor
        val p = if (d == activeIdx % dotCount) activeDotPaint else inactiveDotPaint
        canvas.drawCircle(dotX, deckY, r, p)
        dotX += dotGap
    }

    return bitmap
}

// =========================================================================
// 6. PHOTO STAMP (VINTAGE POSTAGE) (2x2)
// =========================================================================
fun generatePhotoStampBitmap(
    context: Context,
    item: SlateMemoryItem?,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val isLight = slateConfig.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(slateConfig)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val alphaInt = (slateConfig.opacity.coerceIn(0f, 1f) * 255).toInt()
    val stampPaperColor = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))

    val stampPath = Path()
    val toothR = 3.6f * scaleFactor
    val step = toothR * 3f

    stampPath.moveTo(cardRect.left, cardRect.top)
    var cx = cardRect.left + step
    while (cx < cardRect.right - step) {
        stampPath.lineTo(cx - toothR, cardRect.top)
        stampPath.arcTo(RectF(cx - toothR, cardRect.top - toothR, cx + toothR, cardRect.top + toothR), 180f, -180f, false)
        cx += step
    }
    stampPath.lineTo(cardRect.right, cardRect.top)

    var cy = cardRect.top + step
    while (cy < cardRect.bottom - step) {
        stampPath.lineTo(cardRect.right, cy - toothR)
        stampPath.arcTo(RectF(cardRect.right - toothR, cy - toothR, cardRect.right + toothR, cy + toothR), 270f, -180f, false)
        cy += step
    }
    stampPath.lineTo(cardRect.right, cardRect.bottom)

    cx = cardRect.right - step
    while (cx > cardRect.left + step) {
        stampPath.lineTo(cx + toothR, cardRect.bottom)
        stampPath.arcTo(RectF(cx - toothR, cardRect.bottom - toothR, cx + toothR, cardRect.bottom + toothR), 0f, -180f, false)
        cx -= step
    }
    stampPath.lineTo(cardRect.left, cardRect.bottom)

    cy = cardRect.bottom - step
    while (cy > cardRect.top + step) {
        stampPath.lineTo(cardRect.left, cy + toothR)
        stampPath.arcTo(RectF(cardRect.left - toothR, cy - toothR, cardRect.left + toothR, cy + toothR), 90f, -180f, false)
        cy -= step
    }
    stampPath.close()

    val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = stampPaperColor
        style = Paint.Style.FILL
    }
    canvas.drawPath(stampPath, stampPaint)

    val stampBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x33000000 else 0x33FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1f * scaleFactor
    }
    canvas.drawPath(stampPath, stampBorderPaint)

    val inset = 12f * scaleFactor
    val photoBounds = RectF(cardRect.left + inset, cardRect.top + inset, cardRect.right - inset, cardRect.bottom - inset - 22f * scaleFactor)
    drawPhotoSurface(
        canvas = canvas,
        item = item,
        bounds = photoBounds,
        cornerRadius = 4f * scaleFactor,
        scaleFactor = scaleFactor,
        isLight = isLight,
        accentColor = accentColor,
        fallbackSeed = 2
    )

    val innerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x33000000 else 0x33FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1f * scaleFactor
    }
    canvas.drawRect(photoBounds, innerStroke)

    val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 800)
        textSize = 10f * scaleFactor
        color = accentColor
    }
    val valText = "45¢"
    canvas.drawText(valText, photoBounds.right - valPaint.measureText(valText) - 4f * scaleFactor, photoBounds.top + 13f * scaleFactor, valPaint)

    val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x44000000 else 0x55FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    val wavePath = Path()
    val wx = photoBounds.left - 4f * scaleFactor
    val wy = photoBounds.bottom - 12f * scaleFactor
    wavePath.moveTo(wx, wy)
    wavePath.quadTo(wx + 15f * scaleFactor, wy - 6f * scaleFactor, wx + 30f * scaleFactor, wy)
    wavePath.quadTo(wx + 45f * scaleFactor, wy + 6f * scaleFactor, wx + 60f * scaleFactor, wy)
    canvas.drawPath(wavePath, inkPaint)

    val postPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 700)
        textSize = 8.5f * scaleFactor
        color = if (isLight) 0xFF1C1C1E.toInt() else 0xFFD1D1D6.toInt()
    }
    val caption = item?.caption?.uppercase() ?: "SLATE POST • 2024"
    val truncPost = truncateText(caption, cardRect.width() - inset * 2, postPaint)
    canvas.drawText(truncPost, photoBounds.left, cardRect.bottom - inset + 2f * scaleFactor, postPaint)

    return bitmap
}

// =========================================================================
// 7. LOCKET MEMORY (2x2)
// =========================================================================
fun generateLocketMemoryBitmap(
    context: Context,
    item: SlateMemoryItem?,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val bgColor = getSafeBgColor(slateConfig)
    val cornerRadius = getStandardCornerRadius(scaleFactor)
    val isLight = slateConfig.themeMode == "LIGHT"
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val alphaInt = (slateConfig.opacity.coerceIn(0f, 1f) * 255).toInt()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val pad = 12f * scaleFactor
    val locketCenterX = cardRect.centerX()
    val locketCenterY = cardRect.centerY() - 10f * scaleFactor
    val locketRadius = minOf(cardRect.width(), cardRect.height()) * 0.35f

    val locketBounds = RectF(
        locketCenterX - locketRadius,
        locketCenterY - locketRadius,
        locketCenterX + locketRadius,
        locketCenterY + locketRadius
    )

    val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = SweepGradient(
            locketCenterX, locketCenterY,
            intArrayOf(0xFFD4AF37.toInt(), 0xFFFFF2B2.toInt(), 0xFF997A15.toInt(), 0xFFD4AF37.toInt()),
            floatArrayOf(0f, 0.4f, 0.75f, 1f)
        )
        style = Paint.Style.STROKE
        strokeWidth = 3.5f * scaleFactor
    }
    canvas.drawCircle(locketCenterX, locketCenterY, locketRadius + 2.5f * scaleFactor, rimPaint)

    val hingePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFD4AF37.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * scaleFactor
    }
    canvas.drawCircle(locketCenterX, locketBounds.top - 2.5f * scaleFactor, 4f * scaleFactor, hingePaint)

    canvas.save()
    val clipPath = Path().apply {
        addCircle(locketCenterX, locketCenterY, locketRadius, Path.Direction.CW)
    }
    canvas.clipPath(clipPath)
    drawPhotoSurface(
        canvas = canvas,
        item = item,
        bounds = locketBounds,
        cornerRadius = locketRadius,
        scaleFactor = scaleFactor,
        isLight = isLight,
        accentColor = accentColor,
        fallbackSeed = 0
    )
    canvas.restore()

    val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            locketBounds.left, locketBounds.top,
            locketBounds.right, locketBounds.bottom,
            intArrayOf(Color.argb(80, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 0.6f),
            Shader.TileMode.CLAMP
        )
        style = Paint.Style.FILL
    }
    canvas.save()
    canvas.clipPath(clipPath)
    canvas.drawCircle(locketCenterX, locketCenterY, locketRadius, glassPaint)
    canvas.restore()

    val caption = item?.caption ?: "Forever & Always"
    val textY = cardRect.bottom - 16f * scaleFactor
    val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 600)
        textSize = 10.5f * scaleFactor
        color = if (isLight) 0xFF1C1C1E.toInt() else 0xFFF2F2F7.toInt()
    }
    val capW = capPaint.measureText(caption)
    val capX = (cardRect.centerX() - capW / 2f).coerceAtLeast(cardRect.left + pad)
    val trunc = truncateText(caption, cardRect.width() - pad * 2, capPaint)
    canvas.drawText(trunc, capX, textY, capPaint)

    return bitmap
}

// =========================================================================
// 8. FULL-BLEED CLOCK OVERLAY (2x2)
// =========================================================================
fun generatePhotoClockOverlayBitmap(
    context: Context,
    item: SlateMemoryItem?,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val cornerRadius = getStandardCornerRadius(scaleFactor)
    val isLight = slateConfig.themeMode == "LIGHT"
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()

    drawPhotoSurface(
        canvas = canvas,
        item = item,
        bounds = cardRect,
        cornerRadius = cornerRadius,
        scaleFactor = scaleFactor,
        isLight = isLight,
        accentColor = accentColor,
        fallbackSeed = 1
    )

    val topGradient = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            cardRect.left, cardRect.top,
            cardRect.left, cardRect.top + cardRect.height() * 0.45f,
            intArrayOf(Color.argb(170, 0, 0, 0), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    val botGradient = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            cardRect.left, cardRect.bottom - cardRect.height() * 0.45f,
            cardRect.left, cardRect.bottom,
            intArrayOf(Color.TRANSPARENT, Color.argb(170, 0, 0, 0)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    canvas.save()
    val clipPath = Path().apply { addRoundRect(cardRect, cornerRadius, cornerRadius, Path.Direction.CW) }
    canvas.clipPath(clipPath)
    canvas.drawRect(cardRect, topGradient)
    canvas.drawRect(cardRect, botGradient)

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    val now = Date()
    val timeStr = timeFormat.format(now)
    val dateStr = dateFormat.format(now).uppercase()

    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 800)
        textSize = 28f * scaleFactor
        color = android.graphics.Color.WHITE
        setShadowLayer(4f * scaleFactor, 0f, 2f * scaleFactor, 0x88000000.toInt())
    }

    val pad = 14f * scaleFactor
    val timeX = cardRect.left + pad
    val timeY = cardRect.top + pad + 24f * scaleFactor
    canvas.drawText(timeStr, timeX, timeY, timePaint)

    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 600)
        textSize = 9.5f * scaleFactor
        color = accentColor
        setShadowLayer(3f * scaleFactor, 0f, 1.5f * scaleFactor, 0x88000000.toInt())
    }
    canvas.drawText(dateStr, timeX, timeY + 14f * scaleFactor, datePaint)

    val caption = item?.caption ?: "Sunset Memories"
    val pillH = 22f * scaleFactor
    val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        textSize = 10f * scaleFactor
        color = android.graphics.Color.WHITE
    }
    val truncCap = truncateText(caption, cardRect.width() - pad * 2 - 16f * scaleFactor, capPaint)

    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x88000000.toInt()
        style = Paint.Style.FILL
    }
    val pillRect = RectF(timeX, cardRect.bottom - pad - pillH, cardRect.right - pad, cardRect.bottom - pad)
    canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillPaint)
    canvas.drawText(truncCap, pillRect.left + 10f * scaleFactor, pillRect.centerY() + 3.5f * scaleFactor, capPaint)

    canvas.restore()

    val outerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x22FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, outerBorder)

    return bitmap
}

// =========================================================================
// 9. STACKED PHOTO FRAME (2x2)
// =========================================================================
fun generateStackedMemoryBitmap(
    context: Context,
    item: SlateMemoryItem?,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cx = w / 2f
    val cy = h / 2f
    val cardSize = minOf(w, h) * 0.84f
    val cardRect = RectF(cx - cardSize / 2f, cy - cardSize / 2f, cx + cardSize / 2f, cy + cardSize / 2f)

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val isLight = slateConfig.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(slateConfig)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val alphaInt = (slateConfig.opacity.coerceIn(0f, 1f) * 255).toInt()
    val cardBg = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))

    val underBg = if (isLight) {
        Color.argb((alphaInt * 0.9f).toInt(), 0xE5, 0xE5, 0xEA)
    } else {
        if (bgColor == 0xFF000000.toInt()) 0xFF141418.toInt() else Color.argb((alphaInt * 0.8f).toInt(), 0x18, 0x18, 0x1C)
    }

    val underPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = underBg
        style = Paint.Style.FILL
    }
    val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cardBg
        style = Paint.Style.FILL
    }
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(40, 0, 0, 0); style = Paint.Style.FILL }
    val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(35, 0, 0, 0) else Color.argb(35, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1f
    }

    canvas.save()
    canvas.rotate(-6f, cx, cy)
    canvas.drawRoundRect(RectF(cardRect).apply { offset(0f, scaleFactor * 2f) }, cardCornerRadius, cardCornerRadius, shadowPaint)
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, underPaint)
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, cardBorderPaint)
    canvas.restore()

    canvas.save()
    canvas.rotate(5f, cx, cy)
    canvas.drawRoundRect(RectF(cardRect).apply { offset(0f, scaleFactor * 2f) }, cardCornerRadius, cardCornerRadius, shadowPaint)
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, underPaint)
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, cardBorderPaint)
    canvas.restore()

    canvas.save()
    canvas.drawRoundRect(RectF(cardRect).apply { offset(0f, scaleFactor * 3f) }, cardCornerRadius, cardCornerRadius, shadowPaint)
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, cardBgPaint)
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, cardBorderPaint)

    val borderPadding = cardSize * 0.05f
    val bottomChin = cardSize * 0.18f
    val innerPhotoRect = RectF(cardRect.left + borderPadding, cardRect.top + borderPadding, cardRect.right - borderPadding, cardRect.bottom - bottomChin)

    val innerRadius = (cardCornerRadius - borderPadding)
        .coerceAtLeast(scaleFactor * 6f)
        .coerceAtMost(minOf(innerPhotoRect.width(), innerPhotoRect.height()) * 0.22f)

    drawPhotoSurface(
        canvas = canvas,
        item = item,
        bounds = innerPhotoRect,
        cornerRadius = innerRadius,
        scaleFactor = scaleFactor,
        isLight = isLight,
        accentColor = accentColor,
        fallbackSeed = 0
    )

    val captionText = item?.caption ?: "Summer Memories"
    if (captionText.isNotBlank()) {
        val polaroidChinRect = RectF(cardRect.left, cardRect.bottom - bottomChin, cardRect.right, cardRect.bottom)
        val refCaptionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = getSlateFont(context, weight = 700); textSize = 100f }
        val measuredCapW = refCaptionPaint.measureText(captionText).coerceAtLeast(1f)
        val maxCapW = cardRect.width() * 0.80f
        val maxCapH = polaroidChinRect.height() * 0.50f
        val captionFontSize = minOf(maxCapH, 100f * (maxCapW / measuredCapW)).coerceAtLeast(14f)

        val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.parseColor("#121214") else Color.parseColor("#F2F2F7")
            textSize = captionFontSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }

        val captionX = polaroidChinRect.centerX()
        val captionY = polaroidChinRect.centerY() + (captionFontSize * 0.35f)
        canvas.drawText(captionText, captionX, captionY, captionPaint)
    }

    canvas.restore()
    return bitmap
}

// =========================================================================
// 10. TAPED POLAROID PHOTO FRAME (2x2)
// =========================================================================
fun generateTapedPolaroidBitmap(
    context: Context,
    item: SlateMemoryItem?,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cx = w / 2f
    val cy = h / 2f
    val cardSize = minOf(w, h) * 0.90f
    val cardRect = RectF(cx - cardSize / 2f, cy - cardSize / 2f, cx + cardSize / 2f, cy + cardSize / 2f)

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val isLight = slateConfig.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(slateConfig)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val alphaInt = (slateConfig.opacity.coerceIn(0f, 1f) * 255).toInt()
    val cardBg = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))

    val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cardBg
        style = Paint.Style.FILL
    }
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(35, 0, 0, 0); style = Paint.Style.FILL }
    val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(35, 0, 0, 0) else Color.argb(35, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1.0f
    }

    canvas.save()
    canvas.rotate(-3.5f, cx, cy)

    canvas.drawRoundRect(RectF(cardRect).apply { offset(0f, scaleFactor * 3f) }, cardCornerRadius, cardCornerRadius, shadowPaint)
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, cardBgPaint)
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, cardBorderPaint)

    val borderPadding = cardSize * 0.05f
    val bottomChin = cardSize * 0.20f
    val innerPhotoRect = RectF(cardRect.left + borderPadding, cardRect.top + borderPadding, cardRect.right - borderPadding, cardRect.bottom - bottomChin)

    val innerRadius = (cardCornerRadius - borderPadding)
        .coerceAtLeast(scaleFactor * 6f)
        .coerceAtMost(minOf(innerPhotoRect.width(), innerPhotoRect.height()) * 0.22f)

    drawPhotoSurface(
        canvas = canvas,
        item = item,
        bounds = innerPhotoRect,
        cornerRadius = innerRadius,
        scaleFactor = scaleFactor,
        isLight = isLight,
        accentColor = accentColor,
        fallbackSeed = 1
    )

    val captionText = item?.caption ?: "Summer Memories"
    if (captionText.isNotBlank()) {
        val polaroidChinRect = RectF(cardRect.left, cardRect.bottom - bottomChin, cardRect.right, cardRect.bottom)
        val refCaptionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = getSlateFont(context, weight = 700); textSize = 100f }
        val measuredCapW = refCaptionPaint.measureText(captionText).coerceAtLeast(1f)
        val maxCapW = cardRect.width() * 0.80f
        val maxCapH = polaroidChinRect.height() * 0.50f
        val captionFontSize = minOf(maxCapH, 100f * (maxCapW / measuredCapW)).coerceAtLeast(14f)

        val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.parseColor("#121214") else Color.parseColor("#F2F2F7")
            textSize = captionFontSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }

        val captionX = polaroidChinRect.centerX()
        val captionY = polaroidChinRect.centerY() + (captionFontSize * 0.35f)
        canvas.drawText(captionText, captionX, captionY, captionPaint)
    }

    drawMaskingTape(canvas, cardRect, scaleFactor)
    canvas.restore()

    return bitmap
}

private fun drawMaskingTape(canvas: Canvas, cardRect: RectF, scaleFactor: Float) {
    val tapeW = cardRect.width() * 0.38f
    val tapeH = cardRect.height() * 0.11f

    val tapeX = cardRect.centerX() - tapeW / 2f
    val tapeY = cardRect.top + (cardRect.height() * -0.034f)
    val left = tapeX
    val right = left + tapeW
    val top = tapeY
    val bottom = top + tapeH

    canvas.save()
    canvas.rotate(2.8f, (left + right) / 2f, (top + bottom) / 2f)

    val tapePath = Path().apply {
        moveTo(left, top)
        lineTo(right, top)

        lineTo(right - scaleFactor * 1.5f, top + tapeH * 0.18f)
        lineTo(right + scaleFactor * 0.6f, top + tapeH * 0.38f)
        lineTo(right - scaleFactor * 2.0f, top + tapeH * 0.62f)
        lineTo(right + scaleFactor * 0.4f, top + tapeH * 0.82f)
        lineTo(right, bottom)

        lineTo(left, bottom)

        lineTo(left + scaleFactor * 1.8f, top + tapeH * 0.82f)
        lineTo(left - scaleFactor * 0.8f, top + tapeH * 0.58f)
        lineTo(left + scaleFactor * 1.2f, top + tapeH * 0.32f)
        lineTo(left - scaleFactor * 0.5f, top + tapeH * 0.14f)
        close()
    }

    val tapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(215, 246, 243, 235)
        style = Paint.Style.FILL
    }
    val tapeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(45, 170, 165, 150)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.8f
    }

    canvas.drawPath(tapePath, tapePaint)
    canvas.drawPath(tapePath, tapeBorderPaint)
    canvas.restore()
}

// =========================================================================
// 11. PUSH PIN POLAROID PHOTO FRAME (2x2)
// =========================================================================
fun generatePushPinBitmap(
    context: Context,
    item: SlateMemoryItem?,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cx = w / 2f
    val cy = (h / 2f) + (scaleFactor * 8f)
    val cardSize = minOf(w, h) * 0.84f
    val cardRect = RectF(cx - cardSize / 2f, cy - cardSize / 2f, cx + cardSize / 2f, cy + cardSize / 2f)

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val isLight = slateConfig.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(slateConfig)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val alphaInt = (slateConfig.opacity.coerceIn(0f, 1f) * 255).toInt()
    val cardBg = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))

    val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cardBg
        style = Paint.Style.FILL
    }
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(35, 0, 0, 0); style = Paint.Style.FILL }
    val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(35, 0, 0, 0) else Color.argb(35, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1.0f
    }

    canvas.save()
    canvas.rotate(3.2f, cx, cy)

    canvas.drawRoundRect(RectF(cardRect).apply { offset(0f, scaleFactor * 3f) }, cardCornerRadius, cardCornerRadius, shadowPaint)
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, cardBgPaint)
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, cardBorderPaint)

    val borderPadding = cardSize * 0.05f
    val bottomChin = cardSize * 0.20f
    val innerPhotoRect = RectF(cardRect.left + borderPadding, cardRect.top + borderPadding, cardRect.right - borderPadding, cardRect.bottom - bottomChin)

    val innerRadius = (cardCornerRadius - borderPadding)
        .coerceAtLeast(scaleFactor * 6f)
        .coerceAtMost(minOf(innerPhotoRect.width(), innerPhotoRect.height()) * 0.22f)

    drawPhotoSurface(
        canvas = canvas,
        item = item,
        bounds = innerPhotoRect,
        cornerRadius = innerRadius,
        scaleFactor = scaleFactor,
        isLight = isLight,
        accentColor = accentColor,
        fallbackSeed = 2
    )

    val captionText = item?.caption ?: "Summer Memories"
    if (captionText.isNotBlank()) {
        val polaroidChinRect = RectF(cardRect.left, cardRect.bottom - bottomChin, cardRect.right, cardRect.bottom)
        val refCaptionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = getSlateFont(context, weight = 700); textSize = 100f }
        val measuredCapW = refCaptionPaint.measureText(captionText).coerceAtLeast(1f)
        val maxCapW = cardRect.width() * 0.80f
        val maxCapH = polaroidChinRect.height() * 0.50f
        val captionFontSize = minOf(maxCapH, 100f * (maxCapW / measuredCapW)).coerceAtLeast(14f)

        val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.parseColor("#121214") else Color.parseColor("#F2F2F7")
            textSize = captionFontSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }

        val captionX = polaroidChinRect.centerX()
        val captionY = polaroidChinRect.centerY() + (captionFontSize * 0.35f)
        canvas.drawText(captionText, captionX, captionY, captionPaint)
    }

    drawRedPushPin(canvas, cardRect)
    canvas.restore()

    return bitmap
}

private fun drawRedPushPin(canvas: Canvas, cardRect: RectF) {
    val pinW = cardRect.width() * 0.32f
    val pinH = pinW * (123.82f / 131.64f)
    val pinX = cardRect.centerX() - (pinW * 0.48f)
    val pinY = cardRect.top - (pinH * 0.32f)
    val targetRect = RectF(pinX, pinY, pinX + pinW, pinY + pinH)

    val svgBounds = RectF(0f, 0f, 131.64f, 123.82f)
    val matrix = Matrix().apply { setRectToRect(svgBounds, targetRect, Matrix.ScaleToFit.CENTER) }

    canvas.save()
    canvas.concat(matrix)
    canvas.translate(-399.13f, -466.21f)

    val ovalRect = RectF(271.1432f, 4.3322f, 285.9168f, 21.2638f)
    val baseBounds = RectF(297.0594f, -4.2255f, 314.9006f, 13.6157f)

    fun matrixOf(vararg values: Float): Matrix = Matrix().apply { setValues(values) }

    val shadowMat1 = matrixOf(1.2623f, -5.763f, 192.04f, 3.1595f, 2.4855f, -379.19f, 0f, 0f, 1f)
    val shadowGrad1 = RadialGradient(278.53f, 12.798f, 7.3868f, intArrayOf(Color.argb(250, 0, 0, 0), Color.argb(150, 0, 0, 0), Color.TRANSPARENT), floatArrayOf(0f, 0.51f, 1f), Shader.TileMode.CLAMP).apply {
        setLocalMatrix(matrixOf(1.6668f, -.43535f, -180.07f, .25023f, 1.4424f, -76.258f, 0f, 0f, 1f))
    }
    canvas.save()
    canvas.concat(shadowMat1)
    canvas.drawOval(ovalRect, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = shadowGrad1; alpha = 158 })
    canvas.restore()

    val baseMat = matrixOf(3.4214f, 0f, -545.23f, 0f, 3.4413f, 495.42f, 0f, 0f, 1f)
    val baseGrad = RadialGradient(305.98f, 4.6951f, 8.9206f, intArrayOf(Color.parseColor("#F60000"), Color.parseColor("#B30000")), null, Shader.TileMode.CLAMP)
    canvas.save()
    canvas.concat(baseMat)
    canvas.drawOval(baseBounds, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = baseGrad })
    canvas.restore()

    val shadowMat2 = matrixOf(1.3571f, -2.5511f, 136.44f, 2.2762f, 1.241f, -137.78f, 0f, 0f, 1f)
    canvas.save()
    canvas.concat(shadowMat2)
    canvas.drawOval(ovalRect, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = shadowGrad1; alpha = 77 })
    canvas.restore()

    val innerShadowGrad = RadialGradient(302.83f, 4.6951f, 8.9206f, intArrayOf(Color.argb(149, 127, 0, 0), Color.argb(127, 132, 0, 0), Color.TRANSPARENT), floatArrayOf(0f, 0.6667f, 1f), Shader.TileMode.CLAMP)
    val innerShadowMat = matrixOf(2.1108f, 0f, -144.39f, 0f, 2.1231f, 495.36f, 0f, 0f, 1f)
    canvas.save()
    canvas.concat(innerShadowMat)
    canvas.drawOval(baseBounds, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = innerShadowGrad })
    canvas.restore()

    val highlightPathBot = androidx.core.graphics.PathParser.createPathFromPathData("M474.2,531.26 c-6.4976,-5.2202 -8.2466,-8.1777 -7.2576,-14.181 2.8176,7.7769 6.9716,13.737 7.2576,14.181z")
    val highlightGradBot = RadialGradient(537.75f, 228.65f, 0.74646f, intArrayOf(Color.WHITE, Color.TRANSPARENT), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP).apply {
        setLocalMatrix(matrixOf(-4.0973f, -10.635f, 5102.7f, 2.6711f, -16.503f, 2861.5f, 0f, 0f, 1f))
    }
    canvas.drawPath(highlightPathBot, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = highlightGradBot; alpha = 188 })

    val headMat = matrixOf(2.3962f, 0f, -216.26f, .18197f, 2.4872f, 421.67f, 0f, 0f, 1f)
    val headGrad = RadialGradient(302.66f, 3.251f, 8.9206f, intArrayOf(Color.parseColor("#D43500"), Color.parseColor("#D42400"), Color.parseColor("#D40000"), Color.parseColor("#950000")), floatArrayOf(0f, 0.48052f, 0.73611f, 1f), Shader.TileMode.CLAMP).apply {
        setLocalMatrix(matrixOf(.75426f, -.6773f, 77.596f, .68023f, .80831f, -205.09f, 0f, 0f, 1f))
    }
    canvas.save()
    canvas.concat(headMat)
    canvas.drawOval(baseBounds, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = headGrad })
    canvas.restore()

    val highlightPathTop = androidx.core.graphics.PathParser.createPathFromPathData("M514.32,468.93 c10.35,6.9283 13.318,10.867 12.538,18.896 -5.1338,-10.371 -12.058,-18.305 -12.538,-18.896z")
    val highlightGradTop = RadialGradient(537.75f, 228.65f, 0.74646f, intArrayOf(Color.WHITE, Color.TRANSPARENT), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP).apply {
        setLocalMatrix(matrixOf(5.8281f, 17.872f, -6695.6f, -3.599f, 21.975f, -2611.2f, 0f, 0f, 1f))
    }
    canvas.drawPath(highlightPathTop, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = highlightGradTop })

    canvas.restore()
}