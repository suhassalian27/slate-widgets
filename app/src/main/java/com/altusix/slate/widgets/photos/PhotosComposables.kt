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
        // Locked square aspect ratio in fixed mode
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

    // 1. Background Polaroid Card
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

    // 2. Uniform Border Margins (Top, Left, Right are identical across Fixed & Responsive)
    val minDimension = minOf(cardRect.width(), cardRect.height())
    val uniformBorderMargin = minDimension * 0.065f

    // Chin height scales proportionally with widget height
    val chinHeight = if (showCaption) {
        cardRect.height() * 0.23f
    } else {
        uniformBorderMargin
    }

    val photoBounds = RectF(
        cardRect.left + uniformBorderMargin,
        cardRect.top + uniformBorderMargin,
        cardRect.right - uniformBorderMargin,
        cardRect.bottom - chinHeight
    )

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

    // 3. Top Washi Tape Strip (Proportional to widget size)
    val tapeW = minDimension * 0.30f
    val tapeH = (uniformBorderMargin * 0.85f).coerceAtLeast(6f * scaleFactor)
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

    // 4. Proportional Typography (No hard caps; mathematically centered block)
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

        // Font sizes strictly proportional to chin height (no clamping bounds)
        val captionFontSize = chinHeight * 0.26f
        val dateFontSize = captionFontSize * 0.72f

        val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = captionTypeface
            this.textSize = captionFontSize
            this.color = resolvedTextColor
        }

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = getSlateFont(context, weight = 500)
            this.textSize = dateFontSize
            this.color = accentColor
        }

        val maxTextW = photoBounds.width() - 4f * scaleFactor
        val truncatedCaption = truncateText(captionText, maxTextW, captionPaint)
        val truncatedDate = truncateText(dateText, maxTextW, datePaint)

        val hasCaption = truncatedCaption.isNotBlank()
        val hasDate = truncatedDate.isNotBlank()

        // Exact line measurements via font metrics
        val capMetrics = captionPaint.fontMetrics
        val dateMetrics = datePaint.fontMetrics

        val capLineH = -capMetrics.ascent + capMetrics.descent
        val dateLineH = -dateMetrics.ascent + dateMetrics.descent
        // Line gap scales proportionally to font size (tight typographic rhythm)
        val lineGap = captionFontSize * 0.16f

        val totalBlockHeight = when {
            hasCaption && hasDate -> capLineH + lineGap + dateLineH
            hasCaption -> capLineH
            hasDate -> dateLineH
            else -> 0f
        }

        // Center the combined text block in the chin area
        val chinCenterY = (photoBounds.bottom + cardRect.bottom) / 2f
        val blockTopY = chinCenterY - (totalBlockHeight / 2f)
        val textLeft = photoBounds.left + 2f * scaleFactor

        if (hasCaption && hasDate) {
            val captionBaselineY = blockTopY - capMetrics.ascent
            // Second baseline steps down by exact glyph bounds + lineGap
            val dateBaselineY = captionBaselineY + capMetrics.descent + lineGap - dateMetrics.ascent

            canvas.drawText(truncatedCaption, textLeft, captionBaselineY, captionPaint)
            canvas.drawText(truncatedDate, textLeft, dateBaselineY, datePaint)
        } else if (hasCaption) {
            val captionBaselineY = chinCenterY - ((capMetrics.ascent + capMetrics.descent) / 2f)
            canvas.drawText(truncatedCaption, textLeft, captionBaselineY, captionPaint)
        } else if (hasDate) {
            val dateBaselineY = chinCenterY - ((dateMetrics.ascent + dateMetrics.descent) / 2f)
            canvas.drawText(truncatedDate, textLeft, dateBaselineY, datePaint)
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
    hDp: Int,
    showCaption: Boolean = true
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
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

    // 1. Base Card Surface
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x1A000000 else 0x22FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

    // 2. Proportional Insets
    val pad = 12f * scaleFactor
    val innerRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.bottom - pad)

    // Left Photo Frame (~44% width)
    val photoW = innerRect.width() * 0.44f
    val photoBounds = RectF(innerRect.left, innerRect.top, innerRect.left + photoW, innerRect.bottom)
    val photoCorner = (cornerRadius - pad * 0.4f).coerceAtLeast(8f * scaleFactor)

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
        color = if (isLight) 0x1A000000 else 0x24FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1f * scaleFactor
    }
    canvas.drawRoundRect(photoBounds, photoCorner, photoCorner, photoBorder)

    // 3. Right Content Column (Top-Right Aligned with Proportional + Max Bounds)
    val rightLeft = photoBounds.right + 14f * scaleFactor
    val maxTextW = (innerRect.right - rightLeft).coerceAtLeast(10f)

    // Scale ratio relative to standard 4x2 height (~140dp), strictly clamped
    val baseH = 140f * scaleFactor
    val scaleRatio = (innerRect.height() / baseH).coerceIn(0.85f, 1.25f)

    // Font Sizing: scales proportionally with widget size, but has strict MAX SIZE caps
    val pillH = (16f * scaleFactor * scaleRatio).coerceIn(13f * scaleFactor, 19f * scaleFactor)
    val pillTextSize = (8.5f * scaleFactor * scaleRatio).coerceIn(7f * scaleFactor, 10f * scaleFactor)
    val headlineSize = (14.5f * scaleFactor * scaleRatio).coerceIn(11.5f * scaleFactor, 18f * scaleFactor)
    val metaSize = (9.5f * scaleFactor * scaleRatio).coerceIn(8f * scaleFactor, 12f * scaleFactor)
    val quoteSize = (11f * scaleFactor * scaleRatio).coerceIn(9f * scaleFactor, 13.5f * scaleFactor)

    // Gaps between lines: proportional without ballooning
    val gapPillToHeadline = (8.5f * scaleFactor * scaleRatio).coerceAtMost(12f * scaleFactor)
    val gapHeadlineToDate = (5.5f * scaleFactor * scaleRatio).coerceAtMost(8f * scaleFactor)
    val gapDateToLoc = (4f * scaleFactor * scaleRatio).coerceAtMost(6f * scaleFactor)
    val gapLocToQuote = (6.5f * scaleFactor * scaleRatio).coerceAtMost(9f * scaleFactor)

    var curY = innerRect.top + (8f * scaleFactor * scaleRatio)

    // 1. "ON THIS DAY" Pill Badge
    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        alpha = if (isLight) 35 else 50
        style = Paint.Style.FILL
    }
    val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 700)
        textSize = pillTextSize
        color = accentColor
    }
    val pillLabel = "ON THIS DAY"
    val pillLabelW = pillTextPaint.measureText(pillLabel)
    val pillPadX = 8f * scaleFactor
    val pillRect = RectF(
        rightLeft,
        curY,
        rightLeft + pillLabelW + pillPadX * 2,
        curY + pillH
    )
    canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillPaint)

    val pillMetrics = pillTextPaint.fontMetrics
    val pillBaseline = pillRect.centerY() - ((pillMetrics.ascent + pillMetrics.descent) / 2f)
    canvas.drawText(pillLabel, rightLeft + pillPadX, pillBaseline, pillTextPaint)

    curY += pillH + gapPillToHeadline

    // 2. Headline: "1 YEAR AGO"
    val titleColor = if (isLight) 0xFF1C1C1E.toInt() else 0xFFF2F2F7.toInt()
    val headlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 800)
        textSize = headlineSize
        color = titleColor
    }
    val headlineText = "1 YEAR AGO"
    val headMetrics = headlinePaint.fontMetrics
    val headlineBaseline = curY - headMetrics.ascent
    canvas.drawText(headlineText, rightLeft, headlineBaseline, headlinePaint)

    curY += (-headMetrics.ascent + headMetrics.descent) + gapHeadlineToDate

    // 3. Date
    val metaColor = if (isLight) 0xFF636366.toInt() else 0xFF98989D.toInt()
    val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 500)
        textSize = metaSize
        color = metaColor
    }
    val dateText = item?.dateText?.takeIf { it.isNotBlank() } ?: "September 2024"
    val truncatedDate = truncateText(dateText, maxTextW, metaPaint)
    val dateMetrics = metaPaint.fontMetrics
    val dateBaseline = curY - dateMetrics.ascent
    canvas.drawText(truncatedDate, rightLeft, dateBaseline, metaPaint)

    curY += (-dateMetrics.ascent + dateMetrics.descent)

    // 4. Location on its own dedicated line
    val locationText = item?.location?.trim().orEmpty()
    if (locationText.isNotBlank()) {
        curY += gapDateToLoc
        val truncatedLoc = truncateText(locationText, maxTextW, metaPaint)
        val locBaseline = curY - dateMetrics.ascent
        canvas.drawText(truncatedLoc, rightLeft, locBaseline, metaPaint)
        curY += (-dateMetrics.ascent + dateMetrics.descent)
    }

    // 5. Quote Caption
    if (showCaption) {
        val caption = item?.caption?.trim().orEmpty()
        if (caption.isNotBlank()) {
            curY += gapLocToQuote

            val font = item?.captionFont ?: CaptionFont.SANS
            val captionTypeface = getPhotoCaptionTypeface(context, font)

            val rawCaptionColor = item?.captionColorHex ?: (if (isLight) 0xFF3A3A3CL else 0xFFD1D1D6L)
            var resolvedCaptionColor = rawCaptionColor.toInt()
            if (isLight && (resolvedCaptionColor == Color.WHITE || (resolvedCaptionColor and 0x00FFFFFF) == 0x00FFFFFF)) {
                resolvedCaptionColor = Color.parseColor("#1C1C1E")
            }

            val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = captionTypeface
                this.textSize = quoteSize
                this.color = resolvedCaptionColor
            }
            val quoteMetrics = quotePaint.fontMetrics
            val quoteBaseline = curY - quoteMetrics.ascent
            val quoteFormatted = truncateText("“$caption”", maxTextW, quotePaint)
            canvas.drawText(quoteFormatted, rightLeft, quoteBaseline, quotePaint)
        }
    }

    return bitmap
}

// =========================================================================
// 3. 35mm FILM STRIP (4x2 / Adaptive Photobooth Strip)
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

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val aspect = 2.1f
        val cardW = minOf(w, h * aspect)
        val cardH = cardW / aspect
        RectF((w - cardW) / 2f, (h - cardH) / 2f, (w + cardW) / 2f, (h + cardH) / 2f)
    }

    val bgColor = getSafeBgColor(slateConfig)
    val alphaInt = (slateConfig.opacity.coerceIn(0f, 1f) * 255).toInt()
    val isLight = slateConfig.themeMode == "LIGHT"

    // 1. Film Canister Base Card
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    val corner = getStandardCornerRadius(scaleFactor)
    canvas.drawRoundRect(cardRect, corner, corner, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x1A000000 else 0x22FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, corner, corner, borderPaint)

    val defaultList = if (items.isNotEmpty()) items else PhotosWidgetConfig.getDefaultConfig().items
    val sprocketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x2B000000 else 0x33FFFFFF
        style = Paint.Style.FILL
    }
    val frameBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x1E000000 else 0x30FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1f * scaleFactor
    }

    // Determine layout: True if taller than wide (Portrait Photobooth strip)
    val isVertical = isResponsive && (cardRect.height() > cardRect.width() * 1.08f)

    if (isVertical) {
        // =====================================================================
        // VERTICAL PHOTOBOOTH STRIP (Left & Right Sprocket Holes)
        // =====================================================================
        val trackW = (cardRect.width() * 0.14f).coerceIn(12f * scaleFactor, 22f * scaleFactor)
        val sprockW = 4.8f * scaleFactor
        val sprockH = 7.5f * scaleFactor
        val sprockCorner = 1.5f * scaleFactor
        val sprockGap = 12f * scaleFactor

        // Draw Left & Right Sprocket Holes
        var sy = cardRect.top + 10f * scaleFactor
        while (sy + sprockH < cardRect.bottom - 8f * scaleFactor) {
            val leftHole = RectF(
                cardRect.left + (trackW - sprockW) / 2f,
                sy,
                cardRect.left + (trackW + sprockW) / 2f,
                sy + sprockH
            )
            canvas.drawRoundRect(leftHole, sprockCorner, sprockCorner, sprocketPaint)

            val rightX = cardRect.right - trackW
            val rightHole = RectF(
                rightX + (trackW - sprockW) / 2f,
                sy,
                rightX + (trackW + sprockW) / 2f,
                sy + sprockH
            )
            canvas.drawRoundRect(rightHole, sprockCorner, sprockCorner, sprocketPaint)

            sy += sprockH + sprockGap
        }

        // Inner Vertical Frames
        val photoAreaLeft = cardRect.left + trackW
        val photoAreaRight = cardRect.right - trackW
        val frameW = photoAreaRight - photoAreaLeft

        // 3 frames for tall widgets, 2 frames for shorter heights
        val aspect = cardRect.height() / cardRect.width()
        val frameCount = if (aspect >= 1.6f) 3 else 2

        val padV = 8f * scaleFactor
        val frameGap = 6f * scaleFactor
        val availableH = cardRect.height() - (padV * 2)
        val frameH = (availableH - frameGap * (frameCount - 1)) / frameCount

        for (i in 0 until frameCount) {
            val fy = cardRect.top + padV + i * (frameH + frameGap)
            val frameRect = RectF(photoAreaLeft, fy, photoAreaRight, fy + frameH)
            val item = defaultList.getOrNull(i)

            drawPhotoSurface(
                canvas = canvas,
                item = item,
                bounds = frameRect,
                cornerRadius = 4f * scaleFactor,
                scaleFactor = scaleFactor,
                isLight = isLight,
                accentColor = slateConfig.accentColorHex.toInt(),
                fallbackSeed = i
            )
            canvas.drawRoundRect(frameRect, 4f * scaleFactor, 4f * scaleFactor, frameBorder)
        }
    } else {
        // =====================================================================
        // HORIZONTAL FILM STRIP (Top & Bottom Sprocket Holes)
        // =====================================================================
        val trackH = (cardRect.height() * 0.15f).coerceIn(12f * scaleFactor, 22f * scaleFactor)
        val sprockW = 7.5f * scaleFactor
        val sprockH = 4.8f * scaleFactor
        val sprockCorner = 1.5f * scaleFactor
        val sprockGap = 12f * scaleFactor

        // Draw Top & Bottom Sprocket Holes
        var sx = cardRect.left + 10f * scaleFactor
        while (sx + sprockW < cardRect.right - 8f * scaleFactor) {
            val topHole = RectF(
                sx,
                cardRect.top + (trackH - sprockH) / 2f,
                sx + sprockW,
                cardRect.top + (trackH + sprockH) / 2f
            )
            canvas.drawRoundRect(topHole, sprockCorner, sprockCorner, sprocketPaint)

            val botY = cardRect.bottom - trackH
            val botHole = RectF(
                sx,
                botY + (trackH - sprockH) / 2f,
                sx + sprockW,
                botY + (trackH + sprockH) / 2f
            )
            canvas.drawRoundRect(botHole, sprockCorner, sprockCorner, sprocketPaint)

            sx += sprockW + sprockGap
        }

        // Inner Horizontal Frames (Cleanly centered between tracks without text offsets)
        val padV = 5f * scaleFactor
        val photoAreaTop = cardRect.top + trackH + padV
        val photoAreaBottom = cardRect.bottom - trackH - padV
        val frameH = photoAreaBottom - photoAreaTop

        // 3 frames for wide widgets, 2 frames for compact widths
        val aspect = cardRect.width() / cardRect.height()
        val frameCount = if (aspect >= 1.6f) 3 else 2

        val padH = 8f * scaleFactor
        val frameGap = 6f * scaleFactor
        val availableW = cardRect.width() - (padH * 2)
        val frameW = (availableW - frameGap * (frameCount - 1)) / frameCount

        for (i in 0 until frameCount) {
            val fx = cardRect.left + padH + i * (frameW + frameGap)
            val frameRect = RectF(fx, photoAreaTop, fx + frameW, photoAreaBottom)
            val item = defaultList.getOrNull(i)

            drawPhotoSurface(
                canvas = canvas,
                item = item,
                bounds = frameRect,
                cornerRadius = 4f * scaleFactor,
                scaleFactor = scaleFactor,
                isLight = isLight,
                accentColor = slateConfig.accentColorHex.toInt(),
                fallbackSeed = i
            )
            canvas.drawRoundRect(frameRect, 4f * scaleFactor, 4f * scaleFactor, frameBorder)
        }
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
    hDp: Int,
    showCaption: Boolean = true
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

    // 1. Left Hero Tile (~54% width)
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

    // Hero Tile Caption Pill
    val heroCaption = heroItem?.caption?.trim().orEmpty()
    if (showCaption && heroCaption.isNotBlank()) {
        val pillH = 22f * scaleFactor
        val pillPad = 6f * scaleFactor
        val pillRect = RectF(
            heroRect.left + pillPad,
            heroRect.bottom - pillH - pillPad,
            heroRect.right - pillPad,
            heroRect.bottom - pillPad
        )
        val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xAA000000.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillBg)

        val font = heroItem?.captionFont ?: CaptionFont.SANS
        val captionTypeface = getPhotoCaptionTypeface(context, font)
        val textColor = (heroItem?.captionColorHex ?: 0xFFFFFFFFL).toInt()

        val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = captionTypeface
            this.textSize = 9.5f * scaleFactor
            this.color = textColor
        }

        val truncatedHeroText = truncateText(heroCaption, pillRect.width() - 14f * scaleFactor, pillTextPaint)
        val baselineY = pillRect.centerY() - ((pillTextPaint.fontMetrics.ascent + pillTextPaint.fontMetrics.descent) / 2f)
        canvas.drawText(truncatedHeroText, pillRect.left + 8f * scaleFactor, baselineY, pillTextPaint)
    }

    // 2. Right Column (Top & Bottom Sub-Tiles)
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

    // 3. Tile Inner Borders
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
    hDp: Int,
    showCaption: Boolean = config.showCaption
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

    // 1. Draw Photo Surface
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

    // 2. Caption Overlay (respects font style, custom text color, size & toggle)
    if (showCaption) {
        val caption = item?.caption?.trim().orEmpty()
        if (caption.isNotBlank()) {
            val font = item?.captionFont ?: CaptionFont.SANS
            val captionTypeface = getPhotoCaptionTypeface(context, font)

            val rawColorHex = item?.captionColorHex ?: 0xFFFFFFFFL
            val resolvedTextColor = rawColorHex.toInt()

            // Calculate luminance to pick a contrasting gradient backdrop and text shadow
            val isColorDark = (((Color.red(resolvedTextColor) * 0.299f) +
                    (Color.green(resolvedTextColor) * 0.587f) +
                    (Color.blue(resolvedTextColor) * 0.114f)) / 255f) < 0.45f

            val gradientEndColor = if (isColorDark) Color.argb(160, 255, 255, 255) else Color.argb(175, 0, 0, 0)

            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    photoBounds.left, photoBounds.bottom - 52f * scaleFactor,
                    photoBounds.left, photoBounds.bottom,
                    intArrayOf(Color.TRANSPARENT, gradientEndColor),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
            }

            canvas.save()
            val clipP = Path().apply { addRoundRect(photoBounds, photoCorner, photoCorner, Path.Direction.CW) }
            canvas.clipPath(clipP)
            canvas.drawRect(photoBounds, shadowPaint)

            // Scaled up to 13.5f for comfortable readability on 2x2
            val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = captionTypeface
                this.textSize = 13.5f * scaleFactor
                this.color = resolvedTextColor
                setShadowLayer(
                    3f * scaleFactor,
                    0f,
                    1f * scaleFactor,
                    if (isColorDark) Color.argb(110, 255, 255, 255) else Color.argb(180, 0, 0, 0)
                )
            }

            val maxCaptionWidth = photoBounds.width() - 20f * scaleFactor
            val truncCap = truncateText(caption, maxCaptionWidth, capPaint)
            canvas.drawText(
                truncCap,
                photoBounds.left + 10f * scaleFactor,
                photoBounds.bottom - 10f * scaleFactor,
                capPaint
            )
            canvas.restore()
        }
    }

    // 3. Navigation Deck (Buttons & Dots)
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
    hDp: Int,
    showCaption: Boolean = true
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    // 1. Safe Outer Inset: Prevents Samsung One UI's rounded launcher corners from cutting into the teeth
    val outerPad = (minOf(w, h) * 0.055f).coerceIn(8f * scaleFactor, 18f * scaleFactor)
    val cardRect = if (isResponsive) {
        RectF(outerPad, outerPad, w - outerPad, h - outerPad)
    } else {
        val size = minOf(w, h) - (outerPad * 2f)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val isLight = slateConfig.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(slateConfig)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val alphaInt = (slateConfig.opacity.coerceIn(0f, 1f) * 255).toInt()
    val stampPaperColor = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))

    // 2. Mathematically Symmetrical Stamp Perforation Teeth
    val minDim = minOf(cardRect.width(), cardRect.height())
    val toothR = (minDim * 0.024f).coerceIn(3.2f * scaleFactor, 6.0f * scaleFactor)
    val cornerGap = toothR * 2.2f

    val lenX = cardRect.width() - (cornerGap * 2f)
    val lenY = cardRect.height() - (cornerGap * 2f)
    val idealPitch = toothR * 2.85f

    val countX = maxOf(4, (lenX / idealPitch).toInt())
    val stepX = lenX / countX
    val countY = maxOf(4, (lenY / idealPitch).toInt())
    val stepY = lenY / countY

    val stampPath = Path().apply {
        // Start Top-Left
        moveTo(cardRect.left, cardRect.top)

        // Top Edge (Left to Right)
        for (i in 0 until countX) {
            val cx = cardRect.left + cornerGap + (i + 0.5f) * stepX
            lineTo(cx - toothR, cardRect.top)
            arcTo(RectF(cx - toothR, cardRect.top - toothR, cx + toothR, cardRect.top + toothR), 180f, -180f, false)
        }
        lineTo(cardRect.right, cardRect.top)

        // Right Edge (Top to Bottom)
        for (j in 0 until countY) {
            val cy = cardRect.top + cornerGap + (j + 0.5f) * stepY
            lineTo(cardRect.right, cy - toothR)
            arcTo(RectF(cardRect.right - toothR, cy - toothR, cardRect.right + toothR, cy + toothR), 270f, -180f, false)
        }
        lineTo(cardRect.right, cardRect.bottom)

        // Bottom Edge (Right to Left)
        for (i in (countX - 1) downTo 0) {
            val cx = cardRect.left + cornerGap + (i + 0.5f) * stepX
            lineTo(cx + toothR, cardRect.bottom)
            arcTo(RectF(cx - toothR, cardRect.bottom - toothR, cx + toothR, cardRect.bottom + toothR), 0f, -180f, false)
        }
        lineTo(cardRect.left, cardRect.bottom)

        // Left Edge (Bottom to Top)
        for (j in (countY - 1) downTo 0) {
            val cy = cardRect.top + cornerGap + (j + 0.5f) * stepY
            lineTo(cardRect.left, cy + toothR)
            arcTo(RectF(cardRect.left - toothR, cy - toothR, cardRect.left + toothR, cy + toothR), 90f, -180f, false)
        }
        close()
    }

    val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = stampPaperColor
        style = Paint.Style.FILL
    }
    canvas.drawPath(stampPath, stampPaint)

    val stampBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x2A000000 else 0x33FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1f * scaleFactor
    }
    canvas.drawPath(stampPath, stampBorderPaint)

    // 3. Proportional Inner Margins & Bottom Chin
    val paperMargin = (minDim * 0.07f).coerceIn(9f * scaleFactor, 18f * scaleFactor)
    val chinHeight = if (showCaption) {
        (cardRect.height() * 0.17f).coerceIn(24f * scaleFactor, 50f * scaleFactor)
    } else {
        paperMargin
    }

    val photoBounds = RectF(
        cardRect.left + paperMargin,
        cardRect.top + paperMargin,
        cardRect.right - paperMargin,
        cardRect.bottom - chinHeight
    )

    drawPhotoSurface(
        canvas = canvas,
        item = item,
        bounds = photoBounds,
        cornerRadius = 3f * scaleFactor,
        scaleFactor = scaleFactor,
        isLight = isLight,
        accentColor = accentColor,
        fallbackSeed = 2
    )

    val innerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x24000000 else 0x33FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1f * scaleFactor
    }
    canvas.drawRect(photoBounds, innerStroke)



    // 5. Postal Cancellation Wave Mark
    val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x3A000000 else 0x48FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    val wavePath = Path()
    val wx = photoBounds.left - 4f * scaleFactor
    val wy = photoBounds.bottom - 10f * scaleFactor
    val waveW = (photoBounds.width() * 0.35f).coerceIn(35f * scaleFactor, 70f * scaleFactor)
    val stepW = waveW / 4f
    val waveAmp = 4f * scaleFactor

    wavePath.moveTo(wx, wy)
    wavePath.quadTo(wx + stepW, wy - waveAmp, wx + stepW * 2, wy)
    wavePath.quadTo(wx + stepW * 3, wy + waveAmp, wx + stepW * 4, wy)
    canvas.drawPath(wavePath, inkPaint)

    // 6. Proportional Caption with Strict Max-Font-Size Clamp
    if (showCaption) {
        val caption = item?.caption?.trim()?.takeIf { it.isNotEmpty() }?.uppercase() ?: "SLATE POST • 2024"

        val font = item?.captionFont ?: CaptionFont.SANS
        val captionTypeface = getPhotoCaptionTypeface(context, font)

        val rawColorHex = item?.captionColorHex ?: (if (isLight) 0xFF1C1C1EL else 0xFFF2F2F7L)
        var resolvedTextColor = rawColorHex.toInt()
        if (isLight && (resolvedTextColor == Color.WHITE || (resolvedTextColor and 0x00FFFFFF) == 0x00FFFFFF)) {
            resolvedTextColor = Color.parseColor("#121214")
        }

        // Proportional to chin height, clamped between 9.5dp and 16dp max
        val captionFontSize = (chinHeight * 0.38f).coerceIn(9.5f * scaleFactor, 16f * scaleFactor)

        val postPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = captionTypeface
            this.textSize = captionFontSize
            this.color = resolvedTextColor
            this.letterSpacing = 0.04f
        }

        val maxTextW = cardRect.width() - (paperMargin * 2f)
        val truncPost = truncateText(caption, maxTextW, postPaint)

        val postMetrics = postPaint.fontMetrics
        val chinCenterY = (photoBounds.bottom + cardRect.bottom) / 2f
        val textY = chinCenterY - ((postMetrics.ascent + postMetrics.descent) / 2f)

        canvas.drawText(truncPost, photoBounds.left, textY, postPaint)
    }

    return bitmap
}

// =========================================================================
// 7. LOCKET MEMORY (2x2) - PURE FLOATING PENDANT DESIGN
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

    // 1. Proportional Sizing & Optical Centering
    val availableSize = minOf(w, h)
    val bailHeight = availableSize * 0.13f
    val cx = w / 2f
    // Offset slightly down so the entire pendant (locket + top bail) is optically centered
    val cy = (h / 2f) + (bailHeight * 0.38f)
    val locketRadius = (availableSize * 0.43f) - (bailHeight * 0.5f)

    val isLight = slateConfig.themeMode == "LIGHT"
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()

    // Gold Palette for Realistic Sweep & Metallic Lustre
    val goldColors = intArrayOf(
        0xFFEED688.toInt(),
        0xFFFFF6D1.toInt(),
        0xFFC9972E.toInt(),
        0xFF8A6014.toInt(),
        0xFFEED688.toInt(),
        0xFFFFF6D1.toInt(),
        0xFFB07F22.toInt(),
        0xFFEED688.toInt()
    )
    val goldPositions = floatArrayOf(0f, 0.18f, 0.38f, 0.55f, 0.70f, 0.85f, 0.94f, 1f)

    // 2. Soft Ambient Drop Shadow (Floats naturally over any home screen wallpaper)
    val shadowRadius = locketRadius + 6f * scaleFactor
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            cx, cy + 6f * scaleFactor, shadowRadius + 12f * scaleFactor,
            intArrayOf(Color.argb(90, 0, 0, 0), Color.argb(35, 0, 0, 0), Color.TRANSPARENT),
            floatArrayOf(0.72f, 0.90f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(cx, cy + 6f * scaleFactor, shadowRadius + 12f * scaleFactor, shadowPaint)

    // 3. Top Pendant Bail (Hanger Ring & Mounting Bracket)
    val bailOuterR = bailHeight * 0.52f
    val bailInnerR = bailHeight * 0.24f
    val bailCenterY = cy - locketRadius - bailOuterR + (3.5f * scaleFactor)

    val bailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = SweepGradient(cx, bailCenterY, goldColors, goldPositions)
        style = Paint.Style.STROKE
        strokeWidth = (bailOuterR - bailInnerR)
    }
    val bailStrokeRadius = (bailOuterR + bailInnerR) / 2f
    canvas.drawCircle(cx, bailCenterY, bailStrokeRadius, bailPaint)

    // Bail Highlight & Shadow Edge
    val bailRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1f * scaleFactor
    }
    canvas.drawCircle(cx, bailCenterY, bailOuterR, bailRimPaint)

    // Mounting Hinge Bracket at top of Locket
    val hingeW = bailOuterR * 1.3f
    val hingeH = 5f * scaleFactor
    val hingeRect = RectF(cx - hingeW / 2f, cy - locketRadius - hingeH / 2f, cx + hingeW / 2f, cy - locketRadius + hingeH / 2f)
    val hingePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            hingeRect.left, hingeRect.top, hingeRect.right, hingeRect.top,
            intArrayOf(0xFF996E17.toInt(), 0xFFFFEAA2.toInt(), 0xFF996E17.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRoundRect(hingeRect, 2f * scaleFactor, 2f * scaleFactor, hingePaint)

    // 4. Stepped Metallic Gold Outer Bezel
    // A. Outer Bevel Rim
    val outerBevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = SweepGradient(cx, cy, goldColors, goldPositions)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, locketRadius, outerBevelPaint)

    // B. Recessed Shadow Groove
    val grooveRadius = locketRadius - 3.5f * scaleFactor
    val groovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(95, 60, 40, 10)
        style = Paint.Style.STROKE
        strokeWidth = 2f * scaleFactor
    }
    canvas.drawCircle(cx, cy, grooveRadius, groovePaint)

    // C. Raised Inner Polished Gold Lip
    val innerLipRadius = locketRadius - 6.5f * scaleFactor
    val innerLipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = SweepGradient(cx, cy, goldColors, goldPositions)
        style = Paint.Style.STROKE
        strokeWidth = 3f * scaleFactor
    }
    canvas.drawCircle(cx, cy, innerLipRadius, innerLipPaint)

    // 5. Photo Aperture & Image Surface
    val photoRadius = innerLipRadius - 1.5f * scaleFactor
    val photoBounds = RectF(cx - photoRadius, cy - photoRadius, cx + photoRadius, cy + photoRadius)

    canvas.save()
    val clipPath = Path().apply {
        addCircle(cx, cy, photoRadius, Path.Direction.CW)
    }
    canvas.clipPath(clipPath)

    drawPhotoSurface(
        canvas = canvas,
        item = item,
        bounds = photoBounds,
        cornerRadius = photoRadius,
        scaleFactor = scaleFactor,
        isLight = isLight,
        accentColor = accentColor,
        fallbackSeed = 0
    )

    // 6. Realistic Inset Bezel Shadow (Deep recessed look)
    val innerShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            cx, cy, photoRadius,
            intArrayOf(Color.TRANSPARENT, Color.argb(40, 0, 0, 0), Color.argb(160, 0, 0, 0)),
            floatArrayOf(0f, 0.78f, 1f),
            Shader.TileMode.CLAMP
        )
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, photoRadius, innerShadowPaint)

    // 7. Domed Mineral Crystal / Specular Glare (Convex watch-lens highlight)
    val glareCenterX = cx - (photoRadius * 0.28f)
    val glareCenterY = cy - (photoRadius * 0.32f)
    val glareRadius = photoRadius * 0.95f
    val crystalGlarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            glareCenterX, glareCenterY, glareRadius,
            intArrayOf(Color.argb(110, 255, 255, 255), Color.argb(20, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, photoRadius, crystalGlarePaint)

    // Secondary Rim Reflection (Opposite bottom edge glow)
    val rimReflectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            cx, cy + (photoRadius * 0.4f), cx, cy + photoRadius,
            intArrayOf(Color.TRANSPARENT, Color.argb(60, 255, 255, 255)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, photoRadius, rimReflectionPaint)

    canvas.restore()

    // 8. Fine Chamfer Highlight Ring around the photo aperture
    val chamferPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(110, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1f * scaleFactor
    }
    canvas.drawCircle(cx, cy, photoRadius, chamferPaint)

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
    hDp: Int,
    showCaption: Boolean = true
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

    // 1. Photo Surface
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

    // 2. Gradients for Legibility
    val topGradient = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            cardRect.left, cardRect.top,
            cardRect.left, cardRect.top + cardRect.height() * 0.48f,
            intArrayOf(Color.argb(180, 0, 0, 0), Color.TRANSPARENT),
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

    // =====================================================================
    // 3. TYPOGRAPHY CONTROLS (Tweak these limits to adjust text scaling)
    // =====================================================================
    val minDim = minOf(cardRect.width(), cardRect.height())
    val pad = (minDim * 0.08f).coerceIn(12f * scaleFactor, 22f * scaleFactor)

    // Time Clock Size: minimum 26dp, maximum 38dp
    val minTimeSize = 26f * scaleFactor
    val maxTimeSize = 38f * scaleFactor // <-- EDIT MAX TIME SIZE HERE
    val timeFontSize = (minDim * 0.17f).coerceIn(minTimeSize, maxTimeSize)

    // Date Stamp Size: minimum 9.5dp, maximum 13dp
    val minDateSize = 9.5f * scaleFactor
    val maxDateSize = 13f * scaleFactor  // <-- EDIT MAX DATE SIZE HERE
    val dateFontSize = (timeFontSize * 0.32f).coerceIn(minDateSize, maxDateSize)

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    val now = Date()
    val timeStr = timeFormat.format(now)
    val dateStr = dateFormat.format(now).uppercase()

    // Time Clock Text
    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 800)
        textSize = timeFontSize
        color = android.graphics.Color.WHITE
        setShadowLayer(4f * scaleFactor, 0f, 2f * scaleFactor, 0x99000000.toInt())
    }

    val timeX = cardRect.left + pad
    val timeMetrics = timePaint.fontMetrics
    val timeBaseline = cardRect.top + pad - timeMetrics.ascent
    canvas.drawText(timeStr, timeX, timeBaseline, timePaint)

    // Date Stamp Text
    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 700)
        textSize = dateFontSize
        color = accentColor
        letterSpacing = 0.04f
        setShadowLayer(3f * scaleFactor, 0f, 1.5f * scaleFactor, 0x99000000.toInt())
    }

    val dateMetrics = datePaint.fontMetrics
    val dateGap = 4f * scaleFactor
    val dateBaseline = timeBaseline + timeMetrics.descent + dateGap - dateMetrics.ascent
    canvas.drawText(dateStr, timeX, dateBaseline, datePaint)

    // 4. Bottom Caption Pill
    if (showCaption) {
        val caption = item?.caption?.trim().orEmpty().ifEmpty { "Sunset Memories" }
        if (caption.isNotBlank()) {
            val pillH = (minDim * 0.12f).coerceIn(22f * scaleFactor, 34f * scaleFactor)
            val captionFontSize = (pillH * 0.44f).coerceIn(10f * scaleFactor, 14f * scaleFactor)

            val font = item?.captionFont ?: CaptionFont.SANS
            val captionTypeface = getPhotoCaptionTypeface(context, font)
            val rawColorHex = item?.captionColorHex ?: 0xFFFFFFFFL
            val resolvedTextColor = rawColorHex.toInt()

            val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = captionTypeface
                this.textSize = captionFontSize
                this.color = resolvedTextColor
            }

            val maxTextW = cardRect.width() - (pad * 2f) - (20f * scaleFactor)
            val truncCap = truncateText(caption, maxTextW, capPaint)

            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x88000000.toInt()
                style = Paint.Style.FILL
            }
            val pillRect = RectF(
                timeX,
                cardRect.bottom - pad - pillH,
                cardRect.right - pad,
                cardRect.bottom - pad
            )
            canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillPaint)

            val capMetrics = capPaint.fontMetrics
            val capBaseline = pillRect.centerY() - ((capMetrics.ascent + capMetrics.descent) / 2f)
            canvas.drawText(truncCap, pillRect.left + (pillH * 0.42f), capBaseline, capPaint)
        }
    }

    canvas.restore()

    // 5. Card Border
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