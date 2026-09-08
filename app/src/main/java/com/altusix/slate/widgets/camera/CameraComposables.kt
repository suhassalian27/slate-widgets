package com.altusix.slate.widgets.camera

import android.content.Context
import android.graphics.*
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import androidx.core.graphics.PathParser
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.createSupersampledCanvas
import com.altusix.slate.utils.drawConfigurePlaceholderState
import com.altusix.slate.utils.getSafeBgColor
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius

private fun loadAndCropImage(context: Context, uriStr: String?, targetW: Int, targetH: Int): Bitmap? {
    if (uriStr == null) return null
    return try {
        val uri = Uri.parse(uriStr)
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val original = BitmapFactory.decodeStream(inputStream) ?: return null

        val scale = maxOf(targetW.toFloat() / original.width, targetH.toFloat() / original.height)
        val scaledW = original.width * scale
        val scaledH = original.height * scale

        val scaledBitmap = Bitmap.createScaledBitmap(original, scaledW.toInt(), scaledH.toInt(), true)
        val cropX = ((scaledW - targetW) / 2f).toInt().coerceAtLeast(0)
        val cropY = ((scaledH - targetH) / 2f).toInt().coerceAtLeast(0)

        Bitmap.createBitmap(
            scaledBitmap,
            cropX,
            cropY,
            targetW.coerceAtMost(scaledBitmap.width - cropX),
            targetH.coerceAtMost(scaledBitmap.height - cropY)
        )
    } catch (_: Exception) {
        null
    }
}

fun drawConfiguredCaption(
    canvas: Canvas,
    context: Context,
    cardRect: RectF,
    polaroidChinRect: RectF?,
    cameraConfig: CameraWidgetConfig,
    config: SlateWidgetConfig,
    scaleFactor: Float
) {
    if (!cameraConfig.showCaption || cameraConfig.customCaption.isBlank()) return

    val captionText = cameraConfig.customCaption.trim()
    val isPolaroidChin = cameraConfig.borderStyle == PhotoFrameBorder.POLAROID &&
            polaroidChinRect != null &&
            cameraConfig.captionPosition == CaptionPosition.BOTTOM

    val targetBounds = if (isPolaroidChin) polaroidChinRect!! else cardRect

    val typeface = when (cameraConfig.captionFont) {
        CaptionFont.SANS -> getSlateFont(context, weight = 700)
        CaptionFont.SERIF -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
        CaptionFont.MONO -> Typeface.MONOSPACE
        CaptionFont.SCRIPT -> try {
            Typeface.create("cursive", Typeface.BOLD)
        } catch (_: Exception) {
            Typeface.create("casual", Typeface.BOLD)
        }
    }

    var textColor = cameraConfig.captionColorHex.toInt()
    if (isPolaroidChin && (textColor == Color.WHITE || (textColor and 0x00FFFFFF) == 0x00FFFFFF)) {
        textColor = Color.parseColor("#121214")
    }

    val baseSize = minOf(cardRect.width(), cardRect.height()) * 0.082f * cameraConfig.captionSize.scale
    val captionFontSize = baseSize.coerceIn(10f * scaleFactor, 36f * scaleFactor)

    val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = textColor
        this.textSize = captionFontSize
        this.typeface = typeface
        this.textAlign = Paint.Align.CENTER
        if (textColor != Color.parseColor("#121214") && !isPolaroidChin) {
            setShadowLayer(4.5f * scaleFactor, 0f, 1.5f * scaleFactor, Color.argb(210, 0, 0, 0))
        }
    }

    val vBias = cameraConfig.captionVerticalBias.coerceIn(0.08f, 0.92f)
    val hBias = cameraConfig.captionHorizontalBias.coerceIn(0.0f, 1.0f)

    // Automatically taper insets near the poles for circles and organic pebbles
    val distFromCenterY = kotlin.math.abs(vBias - 0.5f) * 2f
    val extraShapePad = if (cardRect.width() == cardRect.height()) {
        cardRect.width() * 0.18f * (distFromCenterY * distFromCenterY)
    } else {
        0f
    }
    val padX = (16f * scaleFactor) + extraShapePad
    val padY = 12f * scaleFactor

    val maxTextW = (targetBounds.width() - (padX * 2f)).coerceAtLeast(10f)
    var displayText = captionText
    if (captionPaint.measureText(displayText) > maxTextW) {
        while (displayText.isNotEmpty() && captionPaint.measureText("$displayText…") > maxTextW) {
            displayText = displayText.dropLast(1)
        }
        displayText = "$displayText…"
    }

    val measuredW = captionPaint.measureText(displayText)
    val minCenterX = targetBounds.left + padX + (measuredW / 2f)
    val maxCenterX = targetBounds.right - padX - (measuredW / 2f)
    val captionX = if (maxCenterX > minCenterX) {
        minCenterX + (maxCenterX - minCenterX) * hBias
    } else {
        targetBounds.centerX()
    }

    val metrics = captionPaint.fontMetrics
    val captionY = if (isPolaroidChin) {
        targetBounds.centerY() - ((metrics.ascent + metrics.descent) / 2f)
    } else {
        val availableY = (targetBounds.bottom - padY - metrics.descent) - (targetBounds.top + padY - metrics.ascent)
        (targetBounds.top + padY - metrics.ascent) + (availableY * vBias)
    }

    canvas.drawText(displayText, captionX, captionY, captionPaint)
}

// 1. FIXED 4x2 WIDE PHOTO FRAME SHOWCASE
fun generatePhotoFrame4x2Bitmap(
    context: Context,
    config: SlateWidgetConfig,
    cameraConfig: CameraWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = getSafeBgColor(config)

    val targetRatio = 2.0f
    var cardH = h
    var cardW = cardH * targetRatio

    if (cardW > w) {
        cardW = w
        cardH = cardW / targetRatio
    }

    val leftX = (w - cardW) / 2f
    val topY = (h - cardH) / 2f
    val cardRect = RectF(leftX, topY, leftX + cardW, topY + cardH)

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val clipPath = Path().apply {
        addRoundRect(cardRect, cardCornerRadius, cardCornerRadius, Path.Direction.CW)
    }
    canvas.save()
    canvas.clipPath(clipPath)

    if (cameraConfig.photoUri.isNullOrEmpty()) {
        drawConfigurePlaceholderState(canvas, context, cardRect, config, scaleFactor)
        canvas.restore()
        return bitmap
    }

    val loadedBitmap = loadAndCropImage(context, cameraConfig.photoUri, cardRect.width().toInt(), cardRect.height().toInt())

    if (loadedBitmap != null) {
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (cameraConfig.filterStyle) {
            PhotoFilterStyle.GRAYSCALE -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            PhotoFilterStyle.SEPIA -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(0.393f, 0.769f, 0.189f, 0f, 0f, 0.349f, 0.686f, 0.168f, 0f, 0f, 0.272f, 0.534f, 0.131f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)))
            PhotoFilterStyle.DARK_DIM -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setScale(0.7f, 0.7f, 0.7f, 1f) })
            PhotoFilterStyle.VINTAGE -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(0.9f, 0.1f, 0.1f, 0f, 20f, 0.1f, 0.8f, 0.1f, 0f, 15f, 0.1f, 0.1f, 0.6f, 0f, 10f, 0f, 0f, 0f, 1f, 0f)))
            PhotoFilterStyle.COOL_BLUE -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(0.7f, 0f, 0.2f, 0f, 0f, 0f, 0.9f, 0.2f, 0f, 0f, 0f, 0.2f, 1.2f, 0f, 20f, 0f, 0f, 0f, 1f, 0f)))
            PhotoFilterStyle.WARM_GOLD -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(1.2f, 0.1f, 0f, 0f, 15f, 0.1f, 1.1f, 0f, 0f, 10f, 0f, 0f, 0.8f, 0f, -10f, 0f, 0f, 0f, 1f, 0f)))
            PhotoFilterStyle.HIGH_CONTRAST -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(1.4f, -0.1f, -0.1f, 0f, -20f, -0.1f, 1.4f, -0.1f, 0f, -20f, -0.1f, -0.1f, 1.4f, 0f, -20f, 0f, 0f, 0f, 1f, 0f)))
            else -> {}
        }

        canvas.drawBitmap(loadedBitmap, null, cardRect, imagePaint)

        var polaroidRect: RectF? = null

        when (cameraConfig.borderStyle) {
            PhotoFrameBorder.POLAROID -> {
                val polaroidBottomH = cardRect.height() * 0.26f
                polaroidRect = RectF(cardRect.left, cardRect.bottom - polaroidBottomH, cardRect.right, cardRect.bottom)
                canvas.drawRect(polaroidRect, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL })
            }
            PhotoFrameBorder.VIGNETTE -> {
                val vigGradient = RadialGradient(cardRect.centerX(), cardRect.centerY(), cardRect.width() * 0.65f, intArrayOf(Color.TRANSPARENT, Color.argb(190, 0, 0, 0)), floatArrayOf(0.55f, 1.0f), Shader.TileMode.CLAMP)
                canvas.drawRect(cardRect, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = vigGradient })
            }
            PhotoFrameBorder.THIN_BORDER -> {
                val strokeW = scaleFactor * 3.5f
                val inset = strokeW / 2f
                val insetRect = RectF(cardRect.left + inset, cardRect.top + inset, cardRect.right - inset, cardRect.bottom - inset)
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = strokeW }
                val innerRadius = (cardCornerRadius - inset).coerceAtLeast(scaleFactor * 4f)
                canvas.drawRoundRect(insetRect, innerRadius, innerRadius, borderPaint)
            }
            PhotoFrameBorder.INNER_OUTLINE -> {
                val gap = scaleFactor * 8f
                val strokeW = scaleFactor * 2f
                val outlineRect = RectF(cardRect.left + gap, cardRect.top + gap, cardRect.right - gap, cardRect.bottom - gap)
                val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(220, 255, 255, 255); style = Paint.Style.STROKE; strokeWidth = strokeW }
                val innerRadius = (cardCornerRadius - gap).coerceAtLeast(scaleFactor * 6f)
                canvas.drawRoundRect(outlineRect, innerRadius, innerRadius, outlinePaint)
            }
            PhotoFrameBorder.FILM_STRIP -> {
                val barH = cardRect.height() * 0.12f
                val stripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.FILL }
                canvas.drawRect(cardRect.left, cardRect.top, cardRect.right, cardRect.top + barH, stripPaint)
                canvas.drawRect(cardRect.left, cardRect.bottom - barH, cardRect.right, cardRect.bottom, stripPaint)

                val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
                val numHoles = 9
                val holeW = cardRect.width() * 0.04f
                val holeH = barH * 0.5f
                val step = cardRect.width() / numHoles

                for (i in 0 until numHoles) {
                    val hLeft = cardRect.left + (i * step) + (step - holeW) / 2f
                    val topHole = RectF(hLeft, cardRect.top + (barH - holeH) / 2f, hLeft + holeW, cardRect.top + (barH + holeH) / 2f)
                    val botHole = RectF(hLeft, cardRect.bottom - barH + (barH - holeH) / 2f, hLeft + holeW, cardRect.bottom - (barH - holeH) / 2f)
                    canvas.drawRoundRect(topHole, scaleFactor * 2f, scaleFactor * 2f, holePaint)
                    canvas.drawRoundRect(botHole, scaleFactor * 2f, scaleFactor * 2f, holePaint)
                }
            }
            else -> {}
        }

        drawConfiguredCaption(
            canvas = canvas,
            context = context,
            cardRect = cardRect,
            polaroidChinRect = polaroidRect,
            cameraConfig = cameraConfig,
            config = config,
            scaleFactor = scaleFactor
        )
    }

    canvas.restore()
    return bitmap
}

// 2. PHOTO FRAME & GALLERY (2x2 / Responsive & Fixed Aspect Photo Display)
fun generatePhotoFrameCameraBitmap(
    context: Context,
    config: SlateWidgetConfig,
    cameraConfig: CameraWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = getSafeBgColor(config)

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val targetRatio = 1.0f
        var cardH = h
        var cardW = cardH * targetRatio

        if (cardW > w) {
            cardW = w
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

    val clipPath = Path().apply {
        addRoundRect(cardRect, cardCornerRadius, cardCornerRadius, Path.Direction.CW)
    }
    canvas.save()
    canvas.clipPath(clipPath)

    if (cameraConfig.photoUri.isNullOrEmpty()) {
        drawConfigurePlaceholderState(canvas, context, cardRect, config, scaleFactor)
        canvas.restore()
        return bitmap
    }

    val loadedBitmap = loadAndCropImage(
        context = context,
        uriStr = cameraConfig.photoUri,
        targetW = cardRect.width().toInt(),
        targetH = cardRect.height().toInt()
    )

    if (loadedBitmap != null) {
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (cameraConfig.filterStyle) {
            PhotoFilterStyle.GRAYSCALE -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            PhotoFilterStyle.SEPIA -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(0.393f, 0.769f, 0.189f, 0f, 0f, 0.349f, 0.686f, 0.168f, 0f, 0f, 0.272f, 0.534f, 0.131f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)))
            PhotoFilterStyle.DARK_DIM -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setScale(0.7f, 0.7f, 0.7f, 1f) })
            PhotoFilterStyle.VINTAGE -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(0.9f, 0.1f, 0.1f, 0f, 20f, 0.1f, 0.8f, 0.1f, 0f, 15f, 0.1f, 0.1f, 0.6f, 0f, 10f, 0f, 0f, 0f, 1f, 0f)))
            PhotoFilterStyle.COOL_BLUE -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(0.7f, 0f, 0.2f, 0f, 0f, 0f, 0.9f, 0.2f, 0f, 0f, 0f, 0.2f, 1.2f, 0f, 20f, 0f, 0f, 0f, 1f, 0f)))
            PhotoFilterStyle.WARM_GOLD -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(1.2f, 0.1f, 0f, 0f, 15f, 0.1f, 1.1f, 0f, 0f, 10f, 0f, 0f, 0.8f, 0f, -10f, 0f, 0f, 0f, 1f, 0f)))
            PhotoFilterStyle.HIGH_CONTRAST -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(1.4f, -0.1f, -0.1f, 0f, -20f, -0.1f, 1.4f, -0.1f, 0f, -20f, -0.1f, -0.1f, 1.4f, 0f, -20f, 0f, 0f, 0f, 1f, 0f)))
            else -> {}
        }

        canvas.drawBitmap(loadedBitmap, null, cardRect, imagePaint)

        var polaroidRect: RectF? = null

        when (cameraConfig.borderStyle) {
            PhotoFrameBorder.POLAROID -> {
                val polaroidBottomH = cardRect.height() * 0.24f
                polaroidRect = RectF(cardRect.left, cardRect.bottom - polaroidBottomH, cardRect.right, cardRect.bottom)
                val polPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
                canvas.drawRect(polaroidRect, polPaint)
            }
            PhotoFrameBorder.VIGNETTE -> {
                val vigGradient = RadialGradient(cardRect.centerX(), cardRect.centerY(), cardRect.width() * 0.7f, intArrayOf(Color.TRANSPARENT, Color.argb(190, 0, 0, 0)), floatArrayOf(0.55f, 1.0f), Shader.TileMode.CLAMP)
                canvas.drawRect(cardRect, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = vigGradient })
            }
            PhotoFrameBorder.THIN_BORDER -> {
                val strokeW = scaleFactor * 3.5f
                val inset = strokeW / 2f
                val insetRect = RectF(cardRect.left + inset, cardRect.top + inset, cardRect.right - inset, cardRect.bottom - inset)
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = strokeW }
                val innerRadius = (cardCornerRadius - inset).coerceAtLeast(scaleFactor * 4f)
                canvas.drawRoundRect(insetRect, innerRadius, innerRadius, borderPaint)
            }
            PhotoFrameBorder.INNER_OUTLINE -> {
                val gap = scaleFactor * 8f
                val strokeW = scaleFactor * 2f
                val outlineRect = RectF(cardRect.left + gap, cardRect.top + gap, cardRect.right - gap, cardRect.bottom - gap)
                val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(220, 255, 255, 255); style = Paint.Style.STROKE; strokeWidth = strokeW }
                val innerRadius = (cardCornerRadius - gap).coerceAtLeast(scaleFactor * 6f)
                canvas.drawRoundRect(outlineRect, innerRadius, innerRadius, outlinePaint)
            }
            PhotoFrameBorder.FILM_STRIP -> {
                val barH = cardRect.height() * 0.08f
                val stripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.FILL }
                canvas.drawRect(cardRect.left, cardRect.top, cardRect.right, cardRect.top + barH, stripPaint)
                canvas.drawRect(cardRect.left, cardRect.bottom - barH, cardRect.right, cardRect.bottom, stripPaint)

                val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
                val numHoles = 5
                val holeW = cardRect.width() * 0.08f
                val holeH = barH * 0.5f
                val step = cardRect.width() / numHoles

                for (i in 0 until numHoles) {
                    val hLeft = cardRect.left + (i * step) + (step - holeW) / 2f
                    val topHole = RectF(hLeft, cardRect.top + (barH - holeH) / 2f, hLeft + holeW, cardRect.top + (barH + holeH) / 2f)
                    val botHole = RectF(hLeft, cardRect.bottom - barH + (barH - holeH) / 2f, hLeft + holeW, cardRect.bottom - (barH - holeH) / 2f)
                    canvas.drawRoundRect(topHole, scaleFactor * 2f, scaleFactor * 2f, holePaint)
                    canvas.drawRoundRect(botHole, scaleFactor * 2f, scaleFactor * 2f, holePaint)
                }
            }
            else -> {}
        }

        drawConfiguredCaption(
            canvas = canvas,
            context = context,
            cardRect = cardRect,
            polaroidChinRect = polaroidRect,
            cameraConfig = cameraConfig,
            config = config,
            scaleFactor = scaleFactor
        )
    }

    canvas.restore()
    return bitmap
}

// 3. CIRCULAR PHOTO FRAME SHOWCASE
fun generatePhotoFrameCircleBitmap(
    context: Context,
    config: SlateWidgetConfig,
    cameraConfig: CameraWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = getSafeBgColor(config)

    val size = minOf(w, h)
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawOval(cardRect, bgPaint)

    val clipPath = Path().apply { addOval(cardRect, Path.Direction.CW) }
    canvas.save()
    canvas.clipPath(clipPath)

    if (cameraConfig.photoUri.isNullOrEmpty()) {
        drawConfigurePlaceholderState(canvas, context, cardRect, config, scaleFactor)
        canvas.restore()
        return bitmap
    }

    val loadedBitmap = loadAndCropImage(context, cameraConfig.photoUri, cardRect.width().toInt(), cardRect.height().toInt())

    if (loadedBitmap != null) {
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (cameraConfig.filterStyle) {
            PhotoFilterStyle.GRAYSCALE -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            PhotoFilterStyle.SEPIA -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(0.393f, 0.769f, 0.189f, 0f, 0f, 0.349f, 0.686f, 0.168f, 0f, 0f, 0.272f, 0.534f, 0.131f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)))
            PhotoFilterStyle.DARK_DIM -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setScale(0.7f, 0.7f, 0.7f, 1f) })
            PhotoFilterStyle.VINTAGE -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(0.9f, 0.1f, 0.1f, 0f, 20f, 0.1f, 0.8f, 0.1f, 0f, 15f, 0.1f, 0.1f, 0.6f, 0f, 10f, 0f, 0f, 0f, 1f, 0f)))
            PhotoFilterStyle.COOL_BLUE -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(0.7f, 0f, 0.2f, 0f, 0f, 0f, 0.9f, 0.2f, 0f, 0f, 0f, 0.2f, 1.2f, 0f, 20f, 0f, 0f, 0f, 1f, 0f)))
            PhotoFilterStyle.WARM_GOLD -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(1.2f, 0.1f, 0f, 0f, 15f, 0.1f, 1.1f, 0f, 0f, 10f, 0f, 0f, 0.8f, 0f, -10f, 0f, 0f, 0f, 1f, 0f)))
            PhotoFilterStyle.HIGH_CONTRAST -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(1.4f, -0.1f, -0.1f, 0f, -20f, -0.1f, 1.4f, -0.1f, 0f, -20f, -0.1f, -0.1f, 1.4f, 0f, -20f, 0f, 0f, 0f, 1f, 0f)))
            else -> {}
        }

        canvas.drawBitmap(loadedBitmap, null, cardRect, imagePaint)

        var polaroidRect: RectF? = null

        when (cameraConfig.borderStyle) {
            PhotoFrameBorder.POLAROID -> {
                val polaroidBottomH = cardRect.height() * 0.25f
                polaroidRect = RectF(cardRect.left, cardRect.bottom - polaroidBottomH, cardRect.right, cardRect.bottom)
                val polPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
                canvas.drawRect(polaroidRect, polPaint)
            }
            PhotoFrameBorder.VIGNETTE -> {
                val vigGradient = RadialGradient(cardRect.centerX(), cardRect.centerY(), cardRect.width() * 0.5f, intArrayOf(Color.TRANSPARENT, Color.argb(200, 0, 0, 0)), floatArrayOf(0.6f, 1.0f), Shader.TileMode.CLAMP)
                canvas.drawOval(cardRect, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = vigGradient })
            }
            PhotoFrameBorder.THIN_BORDER -> {
                val strokeW = scaleFactor * 3.5f
                val inset = strokeW / 2f
                val insetRect = RectF(cardRect.left + inset, cardRect.top + inset, cardRect.right - inset, cardRect.bottom - inset)
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = strokeW }
                canvas.drawOval(insetRect, borderPaint)
            }
            PhotoFrameBorder.INNER_OUTLINE -> {
                val gap = scaleFactor * 10f
                val strokeW = scaleFactor * 2f
                val outlineRect = RectF(cardRect.left + gap, cardRect.top + gap, cardRect.right - gap, cardRect.bottom - gap)
                val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(220, 255, 255, 255); style = Paint.Style.STROKE; strokeWidth = strokeW }
                canvas.drawOval(outlineRect, outlinePaint)
            }
            else -> {}
        }

        drawConfiguredCaption(
            canvas = canvas,
            context = context,
            cardRect = cardRect,
            polaroidChinRect = polaroidRect,
            cameraConfig = cameraConfig,
            config = config,
            scaleFactor = scaleFactor
        )
    }

    canvas.restore()
    return bitmap
}

// 4. ORGANIC BLOB PHOTO FRAME (2x2 / Asymmetric Pebble Display)
fun generatePhotoFrameBlobCameraBitmap(
    context: Context,
    config: SlateWidgetConfig,
    cameraConfig: CameraWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = getSafeBgColor(config)

    val size = minOf(w, h)
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    val svgPathData = "M26.2,15.8C14.3,35.9,-28.7,38.6,-38.3,19.9C-47.8,1.2,-23.9,-39,-2.4,-40.4C19.1,-41.8,38.2,-4.3,26.2,15.8Z"
    val rawPath = PathParser.createPathFromPathData(svgPathData)

    val bounds = RectF()
    rawPath.computeBounds(bounds, true)

    val matrix = Matrix().apply {
        setRectToRect(bounds, cardRect, Matrix.ScaleToFit.CENTER)
    }
    val blobPath = Path()
    rawPath.transform(matrix, blobPath)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawPath(blobPath, bgPaint)

    canvas.save()
    canvas.clipPath(blobPath)

    if (cameraConfig.photoUri.isNullOrEmpty()) {
        drawConfigurePlaceholderState(canvas, context, cardRect, config, scaleFactor)
        canvas.restore()
        return bitmap
    }

    val loadedBitmap = loadAndCropImage(context, cameraConfig.photoUri, cardRect.width().toInt(), cardRect.height().toInt())

    if (loadedBitmap != null) {
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (cameraConfig.filterStyle) {
            PhotoFilterStyle.GRAYSCALE -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            PhotoFilterStyle.SEPIA -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(0.393f, 0.769f, 0.189f, 0f, 0f, 0.349f, 0.686f, 0.168f, 0f, 0f, 0.272f, 0.534f, 0.131f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)))
            PhotoFilterStyle.DARK_DIM -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setScale(0.7f, 0.7f, 0.7f, 1f) })
            PhotoFilterStyle.VINTAGE -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(0.9f, 0.1f, 0.1f, 0f, 20f, 0.1f, 0.8f, 0.1f, 0f, 15f, 0.1f, 0.1f, 0.6f, 0f, 10f, 0f, 0f, 0f, 1f, 0f)))
            PhotoFilterStyle.COOL_BLUE -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(0.7f, 0f, 0.2f, 0f, 0f, 0f, 0.9f, 0.2f, 0f, 0f, 0f, 0.2f, 1.2f, 0f, 20f, 0f, 0f, 0f, 1f, 0f)))
            PhotoFilterStyle.WARM_GOLD -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(1.2f, 0.1f, 0f, 0f, 15f, 0.1f, 1.1f, 0f, 0f, 10f, 0f, 0f, 0.8f, 0f, -10f, 0f, 0f, 0f, 1f, 0f)))
            PhotoFilterStyle.HIGH_CONTRAST -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(1.4f, -0.1f, -0.1f, 0f, -20f, -0.1f, 1.4f, -0.1f, 0f, -20f, -0.1f, -0.1f, 1.4f, 0f, -20f, 0f, 0f, 0f, 1f, 0f)))
            else -> {}
        }

        canvas.drawBitmap(loadedBitmap, null, cardRect, imagePaint)

        var polaroidRect: RectF? = null

        when (cameraConfig.borderStyle) {
            PhotoFrameBorder.POLAROID -> {
                val polaroidBottomH = cardRect.height() * 0.25f
                polaroidRect = RectF(cardRect.left, cardRect.bottom - polaroidBottomH, cardRect.right, cardRect.bottom)
                canvas.drawRect(polaroidRect, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL })
            }
            PhotoFrameBorder.VIGNETTE -> {
                val vigGradient = RadialGradient(cardRect.centerX(), cardRect.centerY(), cardRect.width() * 0.5f, intArrayOf(Color.TRANSPARENT, Color.argb(200, 0, 0, 0)), floatArrayOf(0.6f, 1.0f), Shader.TileMode.CLAMP)
                canvas.drawPath(blobPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = vigGradient })
            }
            PhotoFrameBorder.THIN_BORDER -> {
                val strokeW = scaleFactor * 3.5f
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = strokeW }
                canvas.drawPath(blobPath, borderPaint)
            }
            PhotoFrameBorder.INNER_OUTLINE -> {
                val strokeW = scaleFactor * 2f
                val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(220, 255, 255, 255); style = Paint.Style.STROKE; strokeWidth = strokeW }
                canvas.drawPath(blobPath, outlinePaint)
            }
            else -> {}
        }

        drawConfiguredCaption(
            canvas = canvas,
            context = context,
            cardRect = cardRect,
            polaroidChinRect = polaroidRect,
            cameraConfig = cameraConfig,
            config = config,
            scaleFactor = scaleFactor
        )
    }

    canvas.restore()
    return bitmap
}

// 5. FLUID BLOB PHOTO FRAME (2x2 / Organic Wave Display)
fun generatePhotoFrameFluidBlobCameraBitmap(
    context: Context,
    config: SlateWidgetConfig,
    cameraConfig: CameraWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = getSafeBgColor(config)

    val size = minOf(w, h)
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    val svgPathData = "M59.2,-30.4C70,-15.5,67.4,11,55.3,31.2C43.2,51.4,21.6,65.4,3,63.7C-15.7,62,-31.3,44.6,-44.1,23.9C-57,3.3,-66.9,-20.5,-59.1,-33.7C-51.3,-46.9,-25.6,-49.5,-0.7,-49C24.2,-48.6,48.4,-45.3,59.2,-30.4Z"
    val rawPath = PathParser.createPathFromPathData(svgPathData)

    val bounds = RectF()
    rawPath.computeBounds(bounds, true)

    val matrix = Matrix().apply {
        setRectToRect(bounds, cardRect, Matrix.ScaleToFit.CENTER)
    }
    val blobPath = Path()
    rawPath.transform(matrix, blobPath)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawPath(blobPath, bgPaint)

    canvas.save()
    canvas.clipPath(blobPath)

    if (cameraConfig.photoUri.isNullOrEmpty()) {
        drawConfigurePlaceholderState(canvas, context, cardRect, config, scaleFactor)
        canvas.restore()
        return bitmap
    }

    val loadedBitmap = loadAndCropImage(context, cameraConfig.photoUri, cardRect.width().toInt(), cardRect.height().toInt())

    if (loadedBitmap != null) {
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (cameraConfig.filterStyle) {
            PhotoFilterStyle.GRAYSCALE -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            PhotoFilterStyle.SEPIA -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(0.393f, 0.769f, 0.189f, 0f, 0f, 0.349f, 0.686f, 0.168f, 0f, 0f, 0.272f, 0.534f, 0.131f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)))
            PhotoFilterStyle.DARK_DIM -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setScale(0.7f, 0.7f, 0.7f, 1f) })
            PhotoFilterStyle.VINTAGE -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(0.9f, 0.1f, 0.1f, 0f, 20f, 0.1f, 0.8f, 0.1f, 0f, 15f, 0.1f, 0.1f, 0.6f, 0f, 10f, 0f, 0f, 0f, 1f, 0f)))
            PhotoFilterStyle.COOL_BLUE -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(0.7f, 0f, 0.2f, 0f, 0f, 0f, 0.9f, 0.2f, 0f, 0f, 0f, 0.2f, 1.2f, 0f, 20f, 0f, 0f, 0f, 1f, 0f)))
            PhotoFilterStyle.WARM_GOLD -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(1.2f, 0.1f, 0f, 0f, 15f, 0.1f, 1.1f, 0f, 0f, 10f, 0f, 0f, 0.8f, 0f, -10f, 0f, 0f, 0f, 1f, 0f)))
            PhotoFilterStyle.HIGH_CONTRAST -> imagePaint.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(1.4f, -0.1f, -0.1f, 0f, -20f, -0.1f, 1.4f, -0.1f, 0f, -20f, -0.1f, -0.1f, 1.4f, 0f, -20f, 0f, 0f, 0f, 1f, 0f)))
            else -> {}
        }

        canvas.drawBitmap(loadedBitmap, null, cardRect, imagePaint)

        var polaroidRect: RectF? = null

        when (cameraConfig.borderStyle) {
            PhotoFrameBorder.POLAROID -> {
                val polaroidBottomH = cardRect.height() * 0.25f
                polaroidRect = RectF(cardRect.left, cardRect.bottom - polaroidBottomH, cardRect.right, cardRect.bottom)
                canvas.drawRect(polaroidRect, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL })
            }
            PhotoFrameBorder.VIGNETTE -> {
                val vigGradient = RadialGradient(cardRect.centerX(), cardRect.centerY(), cardRect.width() * 0.5f, intArrayOf(Color.TRANSPARENT, Color.argb(200, 0, 0, 0)), floatArrayOf(0.6f, 1.0f), Shader.TileMode.CLAMP)
                canvas.drawPath(blobPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = vigGradient })
            }
            PhotoFrameBorder.THIN_BORDER -> {
                val strokeW = scaleFactor * 3.5f
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = strokeW }
                canvas.drawPath(blobPath, borderPaint)
            }
            PhotoFrameBorder.INNER_OUTLINE -> {
                val strokeW = scaleFactor * 2f
                val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(220, 255, 255, 255); style = Paint.Style.STROKE; strokeWidth = strokeW }
                canvas.drawPath(blobPath, outlinePaint)
            }
            else -> {}
        }

        drawConfiguredCaption(
            canvas = canvas,
            context = context,
            cardRect = cardRect,
            polaroidChinRect = polaroidRect,
            cameraConfig = cameraConfig,
            config = config,
            scaleFactor = scaleFactor
        )
    }

    canvas.restore()
    return bitmap
}

// 9. SHUTTER LAUNCHER (2x2 / Minimal Camera Trigger Display)
fun generateCameraShutterLauncherBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val size = minOf(w, h)
        val leftX = (w - size) / 2f
        val topY = (h - size) / 2f
        RectF(leftX, topY, leftX + size, topY + size)
    }

    val minDim = minOf(cardRect.width(), cardRect.height())
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val cx = cardRect.centerX()
    val cy = cardRect.centerY()

    val strokeW = minDim * 0.012f
    val bracketMargin = minDim * 0.25f
    val bracketLength = minDim * 0.075f

    val bracketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = strokeW
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    val bLeft = cx - bracketMargin
    val bRight = cx + bracketMargin
    val bTop = cy - bracketMargin
    val bBottom = cy + bracketMargin

    canvas.drawPath(Path().apply { moveTo(bLeft, bTop + bracketLength); lineTo(bLeft, bTop); lineTo(bLeft + bracketLength, bTop) }, bracketPaint)
    canvas.drawPath(Path().apply { moveTo(bRight - bracketLength, bTop); lineTo(bRight, bTop); lineTo(bRight, bTop + bracketLength) }, bracketPaint)
    canvas.drawPath(Path().apply { moveTo(bLeft, bBottom - bracketLength); lineTo(bLeft, bBottom); lineTo(bLeft + bracketLength, bBottom) }, bracketPaint)
    canvas.drawPath(Path().apply { moveTo(bRight - bracketLength, bBottom); lineTo(bRight, bBottom); lineTo(bRight, bBottom - bracketLength) }, bracketPaint)

    val outerRingRadius = minDim * 0.16f
    val innerCircleRadius = minDim * 0.095f

    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(230, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = strokeW * 0.95f
    }
    canvas.drawCircle(cx, cy, outerRingRadius, ringPaint)

    val shutterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, innerCircleRadius, shutterPaint)

    return bitmap
}

// 10. APERTURE LENS CAPSULE (2x1 / Ultra-Minimal Pro Camera Pill)
fun generateCameraAperturePillBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val isLight = config.themeMode == "LIGHT"
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val targetRatio = 2.0f
        var cardH = h
        var cardW = cardH * targetRatio

        if (cardW > w) {
            cardW = w
            cardH = cardW / targetRatio
        }

        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val pillH = cardRect.height()
    val pillRadius = pillH / 2f
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, pillRadius, pillRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(15, 0, 0, 0) else Color.argb(25, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1.0f
    }
    canvas.drawRoundRect(cardRect, pillRadius, pillRadius, borderPaint)

    val cy = cardRect.centerY()
    val paddingX = pillH * 0.42f

    val (mainText, subText) = getDeviceCameraSpecs(context)

    val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        typeface = getSlateFont(context, weight = 600)
        textSize = pillH * 0.22f
        textAlign = Paint.Align.LEFT
        letterSpacing = -0.01f
    }

    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        typeface = getSlateFont(context, weight = 500)
        textSize = pillH * 0.12f
        textAlign = Paint.Align.LEFT
        letterSpacing = 0.04f
    }

    val mainBounds = Rect()
    mainPaint.getTextBounds(mainText, 0, mainText.length, mainBounds)

    val subBounds = Rect()
    subPaint.getTextBounds(subText, 0, subText.length, subBounds)

    val textLeft = cardRect.left + paddingX
    val textGap = pillH * 0.08f
    val totalH = mainBounds.height() + textGap + subBounds.height()

    val startY = cy - (totalH / 2f)
    val mainY = startY + mainBounds.height()
    val subY = mainY + textGap + subBounds.height()

    canvas.drawText(mainText, textLeft, mainY, mainPaint)
    canvas.drawText(subText, textLeft, subY, subPaint)

    val shutterCx = cardRect.right - paddingX - (pillH * 0.08f)
    val outerTrackRadius = pillH * 0.25f
    val innerShutterRadius = pillH * 0.18f

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(12, 0, 0, 0) else Color.argb(20, 255, 255, 255)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(shutterCx, cy, outerTrackRadius, trackPaint)

    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(30, 0, 0, 0) else Color.argb(50, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1.2f
    }
    canvas.drawCircle(shutterCx, cy, outerTrackRadius, ringPaint)

    val shutterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawCircle(shutterCx, cy, innerShutterRadius, shutterPaint)

    return bitmap
}

private fun getDeviceCameraSpecs(context: Context): Pair<String, String> {
    return try {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        val backCameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: cameraManager?.cameraIdList?.firstOrNull()

        if (backCameraId != null) {
            val characteristics = cameraManager!!.getCameraCharacteristics(backCameraId)
            val aperture = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.firstOrNull()
            val focalLength = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull()
            val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)

            val apStr = if (aperture != null) "f/${aperture}" else "f/1.7"

            val fl35 = if (focalLength != null && sensorSize != null && sensorSize.width > 0) {
                ((focalLength * 36f) / sensorSize.width).toInt()
            } else null

            val flStr = if (fl35 != null && fl35 in 10..200) "${fl35}mm" else if (focalLength != null) "${focalLength.toInt()}mm" else "24mm"

            Pair(apStr, flStr)
        } else {
            Pair("f/1.7", "24mm")
        }
    } catch (e: Exception) {
        Pair("f/1.7", "24mm")
    }
}