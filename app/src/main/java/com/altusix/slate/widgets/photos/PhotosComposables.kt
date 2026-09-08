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
import kotlin.math.cos
import kotlin.math.sin

// =========================================================================
// CANVAS BITMAP GENERATORS FOR SLATE "PHOTOS & MEMORIES" WIDGETS
// =========================================================================

/**
 * Creates a ColorMatrixColorFilter corresponding to the chosen MemoryFilterStyle.
 */
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

/**
 * Draws the user photo bitmap cropped to fill bounds, or a rich procedural landscape fallback.
 */
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
        // High-aesthetic procedural scenic artwork
        drawProceduralScenery(canvas, bounds, cornerRadius, scaleFactor, isLight, accentColor, fallbackSeed, colorFilter)
    }
}

/**
 * Renders an artistic procedural scenic canvas with mountains, glowing celestial body, and gradients.
 */
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
        0 -> Triple(0xFF181B28.toInt(), 0xFF353C58.toInt(), 0xFF655268.toInt()) // Twilight Alpine
        1 -> Triple(0xFF22161A.toInt(), 0xFF582D33.toInt(), 0xFFB36746.toInt()) // Golden Horizon
        else -> Triple(0xFF102027.toInt(), 0xFF24444F.toInt(), 0xFF4C7B8B.toInt()) // Pacific Mist
    }

    // Sky gradient
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

    // Celestial body (Sun / Moon)
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

    // Soft celestial glow
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

    // Layer 1: Distant Mountain Ridge
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

    // Layer 2: Foreground Peaks / Dunes
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

    // Minimal camera aperture glyph watermark
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

/**
 * Truncates text with an ellipsis if it exceeds maxWidth.
 */
private fun truncateText(text: String, maxWidth: Float, paint: Paint): String {
    if (paint.measureText(text) <= maxWidth) return text
    var truncated = text
    while (truncated.isNotEmpty() && paint.measureText("$truncated…") > maxWidth) {
        truncated = truncated.dropLast(1)
    }
    return if (truncated.isEmpty()) "" else "$truncated…"
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
    val cardBg = if (isLight) 0xFFFDFBF7.toInt() else 0xFF1C1C20.toInt()
    val frameBorderColor = if (isLight) 0x22000000 else 0x22FFFFFF
    val cardCorner = 12f * scaleFactor

    // Background Polaroid Card
    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cardBg
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCorner, cardCorner, cardPaint)

    // Outer subtle border stroke
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = frameBorderColor
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, cardCorner, cardCorner, borderPaint)

    // Photo Aperture: Left/Right/Top inset ~ 8-9%, bottom chin ~24%
    val padX = cardRect.width() * 0.085f
    val padTop = cardRect.height() * 0.09f
    val chinHeight = cardRect.height() * 0.25f

    val photoBounds = RectF(
        cardRect.left + padX,
        cardRect.top + padTop,
        cardRect.right - padX,
        cardRect.bottom - chinHeight
    )

    // Inner photo outline & shadow
    val photoCorner = 4f * scaleFactor
    drawPhotoSurface(
        canvas = canvas,
        item = item,
        bounds = photoBounds,
        cornerRadius = photoCorner,
        scaleFactor = scaleFactor,
        isLight = isLight,
        accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb(),
        fallbackSeed = 0
    )

    // Photo frame inner bevel
    val photoInnerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x1A000000 else 0x33000000
        style = Paint.Style.STROKE
        strokeWidth = 1f * scaleFactor
    }
    canvas.drawRoundRect(photoBounds, photoCorner, photoCorner, photoInnerBorder)

    // Top Washi Tape strip badge
    val tapeW = cardRect.width() * 0.32f
    val tapeH = 11f * scaleFactor
    val tapeRect = RectF(
        cardRect.centerX() - tapeW / 2f,
        cardRect.top + 3f * scaleFactor,
        cardRect.centerX() + tapeW / 2f,
        cardRect.top + 3f * scaleFactor + tapeH
    )
    val tapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x66D1C7BD else 0x445A5868
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(tapeRect, 3f * scaleFactor, 3f * scaleFactor, tapePaint)

    // Bottom Chin Typography: Caption + Date Stamp
    val captionText = item?.caption ?: "Summer Memories"
    val dateText = item?.dateText ?: "September 2024"

    val textColor = if (isLight) 0xFF1C1C1E.toInt() else 0xFFEBEBF5.toInt()
    val subTextColor = if (isLight) 0xFF7C7C84.toInt() else 0xFF8E8E93.toInt()

    val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 600)
        textSize = 12f * scaleFactor
        color = textColor
    }

    val maxTextW = photoBounds.width()
    val truncatedCaption = truncateText(captionText, maxTextW, captionPaint)
    val captionY = photoBounds.bottom + (chinHeight * 0.44f)
    canvas.drawText(truncatedCaption, photoBounds.left + 2f * scaleFactor, captionY, captionPaint)

    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 400, isItalic = true)
        textSize = 9.5f * scaleFactor
        color = subTextColor
    }
    val truncatedDate = truncateText(dateText, maxTextW, datePaint)
    canvas.drawText(truncatedDate, photoBounds.left + 2f * scaleFactor, captionY + 14f * scaleFactor, datePaint)

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

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val pad = 12f * scaleFactor
    val innerRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.bottom - pad)

    // Left 44% Photo Frame
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

    // Left photo subtle border
    val photoBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x1A000000 else 0x22FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(photoBounds, photoCorner, photoCorner, photoBorder)

    // Right Content Area
    val rightLeft = photoBounds.right + 14f * scaleFactor
    val maxTextW = innerRect.right - rightLeft

    // Header Pill: "ON THIS DAY"
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

    // Time delta headline: e.g. "1 YEAR AGO"
    curY += 24f * scaleFactor
    val titleColor = if (isLight) 0xFF1C1C1E.toInt() else 0xFFF2F2F7.toInt()
    val headlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 800)
        textSize = 15f * scaleFactor
        color = titleColor
    }
    val headlineText = "1 YEAR AGO"
    canvas.drawText(headlineText, rightLeft, curY, headlinePaint)

    // Date & Location metadata
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

    // Quote Caption
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
// 3. 35mm FILM STRIP (4x2 / 4x1)
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

    // Rich Film Canister Carbon Black Base
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF121214.toInt()
        style = Paint.Style.FILL
    }
    val corner = 14f * scaleFactor
    canvas.drawRoundRect(cardRect, corner, corner, bgPaint)

    // Film Sprocket Tracks (Top & Bottom)
    val sprocketTrackH = cardRect.height() * 0.16f
    val sprockW = 8.5f * scaleFactor
    val sprockH = 5.5f * scaleFactor
    val sprockCorner = 1.8f * scaleFactor
    val sprockGap = 13.5f * scaleFactor

    val sprocketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2A2A30.toInt()
        style = Paint.Style.FILL
    }

    var sx = cardRect.left + 10f * scaleFactor
    while (sx + sprockW < cardRect.right - 8f * scaleFactor) {
        // Top hole
        val topHole = RectF(sx, cardRect.top + (sprocketTrackH - sprockH) / 2f, sx + sprockW, cardRect.top + (sprocketTrackH + sprockH) / 2f)
        canvas.drawRoundRect(topHole, sprockCorner, sprockCorner, sprocketPaint)

        // Bottom hole
        val botY = cardRect.bottom - sprocketTrackH
        val botHole = RectF(sx, botY + (sprocketTrackH - sprockH) / 2f, sx + sprockW, botY + (sprocketTrackH + sprockH) / 2f)
        canvas.drawRoundRect(botHole, sprockCorner, sprockCorner, sprocketPaint)

        sx += sprockW + sprockGap
    }

    // Gold / Amber Film Metadata Bar
    val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 700)
        textSize = 7.5f * scaleFactor
        color = 0xFFE5A93C.toInt()
    }
    canvas.drawText("SLATE 400 • 36 EXP", cardRect.left + 16f * scaleFactor, cardRect.top + sprocketTrackH - 2f * scaleFactor, goldPaint)

    // Center Frames: 3 Consecutive Negatives
    val photoAreaTop = cardRect.top + sprocketTrackH
    val photoAreaBottom = cardRect.bottom - sprocketTrackH
    val photoAreaH = photoAreaBottom - photoAreaTop

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
            cornerRadius = 3f * scaleFactor,
            scaleFactor = scaleFactor,
            isLight = false,
            accentColor = 0xFFE5A93C.toInt(),
            fallbackSeed = i
        )

        // Frame divider stroke
        val frameBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33FFFFFF
            style = Paint.Style.STROKE
            strokeWidth = 1f * scaleFactor
        }
        canvas.drawRoundRect(frameRect, 3f * scaleFactor, 3f * scaleFactor, frameBorder)

        // Frame number stamp under photo
        val numY = photoAreaBottom - 1f * scaleFactor
        canvas.drawText(frameLabels[i], fx + 2f * scaleFactor, numY, goldPaint)
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

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val pad = 10f * scaleFactor
    val gap = 7f * scaleFactor
    val inner = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.bottom - pad)

    val defaultList = if (items.isNotEmpty()) items else PhotosWidgetConfig.getDefaultConfig().items
    val tileCorner = 14f * scaleFactor

    // Left Hero Tile: 54% width, 100% height
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

    // Hero tile frosted bottom pill caption
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

    // Right Stacked Dual Sub-Tiles
    val rightX = heroRect.right + gap
    val subW = inner.right - rightX
    val subH = (inner.height() - gap) / 2f

    // Top sub-tile
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

    // Bottom sub-tile
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

    // Subtle outline strokes around each tile
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

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val pad = 10f * scaleFactor
    val inner = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.bottom - pad)

    // Upper Photo Canvas: ~80% height
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

    // Photo bottom subtle vignette shadow
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

    // Overlay Caption & Location
    val caption = item?.caption ?: "Summer Memories"
    val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 600)
        textSize = 10f * scaleFactor
        color = android.graphics.Color.WHITE
    }
    val truncCap = truncateText(caption, photoBounds.width() - 16f * scaleFactor, capPaint)
    canvas.drawText(truncCap, photoBounds.left + 8f * scaleFactor, photoBounds.bottom - 8f * scaleFactor, capPaint)
    canvas.restore()

    // Bottom Navigation Deck: [ < ] [ ● ○ ○ ] [ > ]
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

    // Chevron Glyphs
    val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0xFF333336.toInt() else 0xFFF2F2F7.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * scaleFactor
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    // Left chevron
    val lPath = Path().apply {
        moveTo(leftBtnX + 2f * scaleFactor, deckY - 4.5f * scaleFactor)
        lineTo(leftBtnX - 2.5f * scaleFactor, deckY)
        lineTo(leftBtnX + 2f * scaleFactor, deckY + 4.5f * scaleFactor)
    }
    canvas.drawPath(lPath, arrowPaint)

    // Right chevron
    val rPath = Path().apply {
        moveTo(rightBtnX - 2f * scaleFactor, deckY - 4.5f * scaleFactor)
        lineTo(rightBtnX + 2.5f * scaleFactor, deckY)
        lineTo(rightBtnX - 2f * scaleFactor, deckY + 4.5f * scaleFactor)
    }
    canvas.drawPath(rPath, arrowPaint)

    // Center pagination dots or indicator text (e.g. 01 / 03)
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
    val stampPaperColor = if (isLight) 0xFFFAF7F0.toInt() else 0xFF202024.toInt()
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()

    // 1. Draw Serrated Postage Stamp Border with Scalloped Perforations
    val stampPath = Path()
    val toothR = 3.6f * scaleFactor
    val step = toothR * 3f

    // Construct path with scalloped notches around edges
    stampPath.moveTo(cardRect.left, cardRect.top)
    // Top edge
    var cx = cardRect.left + step
    while (cx < cardRect.right - step) {
        stampPath.lineTo(cx - toothR, cardRect.top)
        stampPath.arcTo(RectF(cx - toothR, cardRect.top - toothR, cx + toothR, cardRect.top + toothR), 180f, -180f, false)
        cx += step
    }
    stampPath.lineTo(cardRect.right, cardRect.top)

    // Right edge
    var cy = cardRect.top + step
    while (cy < cardRect.bottom - step) {
        stampPath.lineTo(cardRect.right, cy - toothR)
        stampPath.arcTo(RectF(cardRect.right - toothR, cy - toothR, cardRect.right + toothR, cy + toothR), 270f, -180f, false)
        cy += step
    }
    stampPath.lineTo(cardRect.right, cardRect.bottom)

    // Bottom edge
    cx = cardRect.right - step
    while (cx > cardRect.left + step) {
        stampPath.lineTo(cx + toothR, cardRect.bottom)
        stampPath.arcTo(RectF(cx - toothR, cardRect.bottom - toothR, cx + toothR, cardRect.bottom + toothR), 0f, -180f, false)
        cx -= step
    }
    stampPath.lineTo(cardRect.left, cardRect.bottom)

    // Left edge
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

    // Subtle edge border
    val stampBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x33A09080 else 0x33FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1f * scaleFactor
    }
    canvas.drawPath(stampPath, stampBorderPaint)

    // 2. Inner Photo Frame: Rectangular inset with clean gap
    val inset = 12f * scaleFactor
    val photoBounds = RectF(cardRect.left + inset, cardRect.top + inset, cardRect.right - inset, cardRect.bottom - inset - 22f * scaleFactor)
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

    // Photo fine border
    val innerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x33000000 else 0x33FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1f * scaleFactor
    }
    canvas.drawRect(photoBounds, innerStroke)

    // 3. Denomination Badge (Top Right of Stamp) e.g. "45¢"
    val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 800)
        textSize = 10f * scaleFactor
        color = accentColor
    }
    val valText = "45¢"
    canvas.drawText(valText, photoBounds.right - valPaint.measureText(valText) - 4f * scaleFactor, photoBounds.top + 13f * scaleFactor, valPaint)

    // 4. Postal Cancellation Wave Mark (Over bottom-left corner of photo)
    val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) 0x444A3B32 else 0x44AAAAAA
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

    // 5. Bottom Stamp Label: e.g. "SLATE POST • 2024"
    val postPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = getSlateFont(context, weight = 700)
        textSize = 8.5f * scaleFactor
        color = if (isLight) 0xFF4A4038.toInt() else 0xFFC7C0B8.toInt()
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

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    // Locket Center Geometry (Portrait Oval / Circle)
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

    // Metallic Outer Rim / Bevel
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

    // Top Pendant Hinge loop
    val hingePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFD4AF37.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * scaleFactor
    }
    canvas.drawCircle(locketCenterX, locketBounds.top - 2.5f * scaleFactor, 4f * scaleFactor, hingePaint)

    // Clipped circular photo
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

    // Inner Glass Highlight
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

    // Bottom Caption Pill
    val caption = item?.caption ?: "Forever & Always"
    val dateText = item?.dateText ?: "September 2024"

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

    // 1. Full-Bleed Photo
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

    // 2. High-contrast Top and Bottom Vignettes
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

    // 3. Top Digital Clock Overlay (HH:mm)
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
        color = Color.argb(220, 255, 255, 255)
        setShadowLayer(3f * scaleFactor, 0f, 1.5f * scaleFactor, 0x88000000.toInt())
    }
    canvas.drawText(dateStr, timeX, timeY + 14f * scaleFactor, datePaint)

    // 4. Bottom Memory Frosted Pill: Caption
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

    // Outer card stroke
    val outerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x22FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, outerBorder)

    return bitmap
}
