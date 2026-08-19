package com.altusix.slate.widgets.camera

import android.content.Context
import android.graphics.*
import android.net.Uri
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.getSafeBgColor
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius
import android.graphics.*
import com.altusix.slate.utils.drawConfigurePlaceholderState
import com.altusix.slate.utils.getSafeBgColor
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius
import androidx.core.graphics.PathParser
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

        Bitmap.createBitmap(scaledBitmap, cropX, cropY, targetW.coerceAtMost(scaledBitmap.width - cropX), targetH.coerceAtMost(scaledBitmap.height - cropY))
    } catch (_: Exception) {
        null
    }
}

// 1. FIXED 4x2 WIDE PHOTO FRAME SHOWCASE
fun generatePhotoFrame4x2Bitmap(context: Context, config: SlateWidgetConfig, cameraConfig: CameraWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val w = (wDp * scaleFactor).toInt().coerceAtLeast(800)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(400)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgColor = getSafeBgColor(config)

    // Locked 2:1 Aspect Ratio Box
    val targetRatio = 2.0f
    var cardH = h.toFloat()
    var cardW = cardH * targetRatio

    if (cardW > w.toFloat()) {
        cardW = w.toFloat()
        cardH = cardW / targetRatio
    }

    val leftX = (w - cardW) / 2f
    val topY = (h - cardH) / 2f
    val cardRect = RectF(leftX, topY, leftX + cardW, topY + cardH)

    val cornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val clipPath = Path().apply {
        addRoundRect(cardRect, cornerRadius, cornerRadius, Path.Direction.CW)
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
                canvas.drawRoundRect(insetRect, (cornerRadius - inset).coerceAtLeast(4f), (cornerRadius - inset).coerceAtLeast(4f), borderPaint)
            }
            PhotoFrameBorder.INNER_OUTLINE -> {
                val gap = scaleFactor * 8f
                val strokeW = scaleFactor * 2f
                val outlineRect = RectF(cardRect.left + gap, cardRect.top + gap, cardRect.right - gap, cardRect.bottom - gap)
                val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(220, 255, 255, 255); style = Paint.Style.STROKE; strokeWidth = strokeW }
                canvas.drawRoundRect(outlineRect, (cornerRadius - gap).coerceAtLeast(6f), (cornerRadius - gap).coerceAtLeast(6f), outlinePaint)
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

        if (cameraConfig.customCaption.isNotEmpty()) {
            val captionText = cameraConfig.customCaption
            val isPolaroid = cameraConfig.borderStyle == PhotoFrameBorder.POLAROID
            val captionColor = if (isPolaroid) Color.parseColor("#121214") else Color.WHITE

            val refCaptionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = getSlateFont(context, weight = 700); textSize = 100f }
            val measuredCapW = refCaptionPaint.measureText(captionText).coerceAtLeast(1f)
            val maxCapW = cardRect.width() * 0.85f
            val maxCapH = if (isPolaroid) polaroidRect!!.height() * 0.5f else cardRect.height() * 0.12f
            val captionFontSize = minOf(maxCapH, 100f * (maxCapW / measuredCapW)).coerceAtLeast(18f)

            val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = captionColor
                textSize = captionFontSize
                typeface = getSlateFont(context, weight = 700)
                textAlign = if (isPolaroid) Paint.Align.CENTER else Paint.Align.LEFT
                if (!isPolaroid) setShadowLayer(6f, 0f, 2f, Color.BLACK)
            }

            if (isPolaroid && polaroidRect != null) {
                val captionX = polaroidRect.centerX()
                val captionY = polaroidRect.centerY() + (captionFontSize * 0.35f)
                canvas.drawText(captionText, captionX, captionY, captionPaint)
            } else {
                val captionX = cardRect.left + (cardRect.width() * 0.05f)
                val captionY = cardRect.bottom - (cardRect.height() * 0.08f)
                canvas.drawText(captionText, captionX, captionY, captionPaint)
            }
        }
    }

    canvas.restore()
    return bitmap
}

// 2. PHOTO FRAME & GALLERY (2x2 / Responsive & Fixed Aspect Photo Display)
fun generatePhotoFrameCameraBitmap(context: Context, config: SlateWidgetConfig, cameraConfig: CameraWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int)
: Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val w = (wDp * scaleFactor).toInt().coerceAtLeast(420)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(420)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgColor = getSafeBgColor(config)

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val targetRatio = 1.0f
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

    val cornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val clipPath = Path().apply {
        addRoundRect(cardRect, cornerRadius, cornerRadius, Path.Direction.CW)
    }
    canvas.save()
    canvas.clipPath(clipPath)

    // =========================================================================
    // STATE A: UNCONFIGURED EMPTY STATE ("Tap to Configure")
    // =========================================================================
    if (cameraConfig.photoUri.isNullOrEmpty()) {
        drawConfigurePlaceholderState(
            canvas = canvas,
            context = context,
            cardRect = cardRect,
            config = config,
            scaleFactor = scaleFactor
        )

        canvas.restore()
        return bitmap
    }

    // =========================================================================
    // STATE B: PHOTO DISPLAYED WITH FILTERS & OVERLAYS
    // =========================================================================
    val loadedBitmap = loadAndCropImage(
        context = context,
        uriStr = cameraConfig.photoUri,
        targetW = cardRect.width().toInt(),
        targetH = cardRect.height().toInt()
    )

    if (loadedBitmap != null) {
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Expanded ColorMatrix Presets
        when (cameraConfig.filterStyle) {
            PhotoFilterStyle.GRAYSCALE -> {
                val cm = ColorMatrix().apply { setSaturation(0f) }
                imagePaint.colorFilter = ColorMatrixColorFilter(cm)
            }
            PhotoFilterStyle.SEPIA -> {
                val cm = ColorMatrix().apply {
                    set(floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
                imagePaint.colorFilter = ColorMatrixColorFilter(cm)
            }
            PhotoFilterStyle.DARK_DIM -> {
                val cm = ColorMatrix().apply { setScale(0.7f, 0.7f, 0.7f, 1f) }
                imagePaint.colorFilter = ColorMatrixColorFilter(cm)
            }
            PhotoFilterStyle.VINTAGE -> {
                val cm = ColorMatrix().apply {
                    set(floatArrayOf(
                        0.9f, 0.1f, 0.1f, 0f, 20f,
                        0.1f, 0.8f, 0.1f, 0f, 15f,
                        0.1f, 0.1f, 0.6f, 0f, 10f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
                imagePaint.colorFilter = ColorMatrixColorFilter(cm)
            }
            PhotoFilterStyle.COOL_BLUE -> {
                val cm = ColorMatrix().apply {
                    set(floatArrayOf(
                        0.7f, 0f, 0.2f, 0f, 0f,
                        0f, 0.9f, 0.2f, 0f, 0f,
                        0f, 0.2f, 1.2f, 0f, 20f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
                imagePaint.colorFilter = ColorMatrixColorFilter(cm)
            }
            PhotoFilterStyle.WARM_GOLD -> {
                val cm = ColorMatrix().apply {
                    set(floatArrayOf(
                        1.2f, 0.1f, 0f, 0f, 15f,
                        0.1f, 1.1f, 0f, 0f, 10f,
                        0f, 0f, 0.8f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
                imagePaint.colorFilter = ColorMatrixColorFilter(cm)
            }
            PhotoFilterStyle.HIGH_CONTRAST -> {
                val cm = ColorMatrix().apply {
                    set(floatArrayOf(
                        1.4f, -0.1f, -0.1f, 0f, -20f,
                        -0.1f, 1.4f, -0.1f, 0f, -20f,
                        -0.1f, -0.1f, 1.4f, 0f, -20f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
                imagePaint.colorFilter = ColorMatrixColorFilter(cm)
            }
            else -> {}
        }

        canvas.drawBitmap(loadedBitmap, null, cardRect, imagePaint)

        // Expanded Frame Border Options
        var polaroidRect: RectF? = null

        when (cameraConfig.borderStyle) {
            PhotoFrameBorder.POLAROID -> {
                val polaroidBottomH = cardRect.height() * 0.24f
                polaroidRect = RectF(cardRect.left, cardRect.bottom - polaroidBottomH, cardRect.right, cardRect.bottom)
                val polPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                canvas.drawRect(polaroidRect, polPaint)
            }
            PhotoFrameBorder.VIGNETTE -> {
                val vignetteGradient = RadialGradient(
                    cardRect.centerX(), cardRect.centerY(), cardRect.width() * 0.7f,
                    intArrayOf(Color.TRANSPARENT, Color.argb(190, 0, 0, 0)),
                    floatArrayOf(0.55f, 1.0f),
                    Shader.TileMode.CLAMP
                )
                val vigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = vignetteGradient }
                canvas.drawRect(cardRect, vigPaint)
            }
            PhotoFrameBorder.THIN_BORDER -> {
                // Inset minimal border with 100% full opacity white line
                val strokeW = scaleFactor * 3.5f
                val inset = strokeW / 2f
                val insetRect = RectF(cardRect.left + inset, cardRect.top + inset, cardRect.right - inset, cardRect.bottom - inset)
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW
                }
                val borderRadius = (cornerRadius - inset).coerceAtLeast(4f)
                canvas.drawRoundRect(insetRect, borderRadius, borderRadius, borderPaint)
            }
            PhotoFrameBorder.INNER_OUTLINE -> {
                val gap = scaleFactor * 8f
                val strokeW = scaleFactor * 2f
                val outlineRect = RectF(cardRect.left + gap, cardRect.top + gap, cardRect.right - gap, cardRect.bottom - gap)
                val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(220, 255, 255, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW
                }
                val outlineRadius = (cornerRadius - gap).coerceAtLeast(6f)
                canvas.drawRoundRect(outlineRect, outlineRadius, outlineRadius, outlinePaint)
            }
            PhotoFrameBorder.FILM_STRIP -> {
                val barH = cardRect.height() * 0.08f
                val stripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    style = Paint.Style.FILL
                }
                canvas.drawRect(cardRect.left, cardRect.top, cardRect.right, cardRect.top + barH, stripPaint)
                canvas.drawRect(cardRect.left, cardRect.bottom - barH, cardRect.right, cardRect.bottom, stripPaint)

                // Hole punches
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

        // Caption Overlay Handling
        if (cameraConfig.customCaption.isNotEmpty()) {
            val captionText = cameraConfig.customCaption
            val isPolaroid = cameraConfig.borderStyle == PhotoFrameBorder.POLAROID
            val captionColor = if (isPolaroid) Color.parseColor("#121214") else Color.WHITE

            val refCaptionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = getSlateFont(context, weight = 700)
                textSize = 100f
            }
            val measuredCapW = refCaptionPaint.measureText(captionText).coerceAtLeast(1f)
            val maxCapW = cardRect.width() * 0.84f
            val maxCapH = if (isPolaroid) polaroidRect!!.height() * 0.45f else cardRect.height() * 0.08f
            val captionFontSize = minOf(maxCapH, 100f * (maxCapW / measuredCapW)).coerceAtLeast(16f)

            val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = captionColor
                textSize = captionFontSize
                typeface = getSlateFont(context, weight = 700)
                textAlign = if (isPolaroid) Paint.Align.CENTER else Paint.Align.LEFT
                if (!isPolaroid) {
                    setShadowLayer(6f, 0f, 2f, Color.BLACK)
                }
            }

            if (isPolaroid && polaroidRect != null) {
                // Strictly centered horizontally and vertically inside the white bottom chin
                val captionX = polaroidRect.centerX()
                val captionY = polaroidRect.centerY() + (captionFontSize * 0.35f)
                canvas.drawText(captionText, captionX, captionY, captionPaint)
            } else {
                val captionX = cardRect.left + (cardRect.width() * 0.08f)
                val captionY = cardRect.bottom - (cardRect.height() * 0.06f)
                canvas.drawText(captionText, captionX, captionY, captionPaint)
            }
        }
    }

    canvas.restore()
    return bitmap
}

// 3. CIRCULAR PHOTO FRAME SHOWCASE
fun generatePhotoFrameCircleBitmap(context: Context, config: SlateWidgetConfig, cameraConfig: CameraWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val w = (wDp * scaleFactor).toInt().coerceAtLeast(420)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(420)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgColor = getSafeBgColor(config)

    val size = minOf(w, h).toFloat()
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

        if (cameraConfig.customCaption.isNotEmpty()) {
            val captionText = cameraConfig.customCaption
            val isPolaroid = cameraConfig.borderStyle == PhotoFrameBorder.POLAROID
            val captionColor = if (isPolaroid) Color.parseColor("#121214") else Color.WHITE

            val refCaptionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = getSlateFont(context, weight = 700); textSize = 100f }
            val measuredCapW = refCaptionPaint.measureText(captionText).coerceAtLeast(1f)
            val maxCapW = cardRect.width() * 0.65f
            val maxCapH = if (isPolaroid) polaroidRect!!.height() * 0.45f else cardRect.height() * 0.10f
            val captionFontSize = minOf(maxCapH, 100f * (maxCapW / measuredCapW)).coerceAtLeast(16f)

            val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = captionColor
                textSize = captionFontSize
                typeface = getSlateFont(context, weight = 700)
                textAlign = Paint.Align.CENTER
                if (!isPolaroid) setShadowLayer(6f, 0f, 2f, Color.BLACK)
            }

            if (isPolaroid && polaroidRect != null) {
                val captionX = polaroidRect.centerX()
                val captionY = polaroidRect.centerY() + (captionFontSize * 0.35f)
                canvas.drawText(captionText, captionX, captionY, captionPaint)
            } else {
                val captionX = cardRect.centerX()
                val captionY = cardRect.bottom - (cardRect.height() * 0.12f)
                canvas.drawText(captionText, captionX, captionY, captionPaint)
            }
        }
    }

    canvas.restore()
    return bitmap
}

// 4. ORGANIC BLOB PHOTO FRAME (2x2 / Asymmetric Pebble Display)
fun generatePhotoFrameBlobCameraBitmap(context: Context, config: SlateWidgetConfig, cameraConfig: CameraWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val w = (wDp * scaleFactor).toInt().coerceAtLeast(420)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(420)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgColor = getSafeBgColor(config)

    val size = minOf(w, h).toFloat()
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    // Parse SVG Path & Matrix scale directly to cardRect
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

        if (cameraConfig.customCaption.isNotEmpty()) {
            val captionText = cameraConfig.customCaption
            val isPolaroid = cameraConfig.borderStyle == PhotoFrameBorder.POLAROID
            val captionColor = if (isPolaroid) Color.parseColor("#121214") else Color.WHITE

            val refCaptionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = getSlateFont(context, weight = 700); textSize = 100f }
            val measuredCapW = refCaptionPaint.measureText(captionText).coerceAtLeast(1f)
            val maxCapW = cardRect.width() * 0.65f
            val maxCapH = if (isPolaroid) polaroidRect!!.height() * 0.45f else cardRect.height() * 0.10f
            val captionFontSize = minOf(maxCapH, 100f * (maxCapW / measuredCapW)).coerceAtLeast(16f)

            val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = captionColor
                textSize = captionFontSize
                typeface = getSlateFont(context, weight = 700)
                textAlign = Paint.Align.CENTER
                if (!isPolaroid) setShadowLayer(6f, 0f, 2f, Color.BLACK)
            }

            if (isPolaroid && polaroidRect != null) {
                val captionX = polaroidRect.centerX()
                val captionY = polaroidRect.centerY() + (captionFontSize * 0.35f)
                canvas.drawText(captionText, captionX, captionY, captionPaint)
            } else {
                val captionX = cardRect.centerX()
                val captionY = cardRect.bottom - (cardRect.height() * 0.12f)
                canvas.drawText(captionText, captionX, captionY, captionPaint)
            }
        }
    }

    canvas.restore()
    return bitmap
}

// 5. FLUID BLOB PHOTO FRAME (2x2 / Organic Wave Display)
fun generatePhotoFrameFluidBlobCameraBitmap(context: Context, config: SlateWidgetConfig, cameraConfig: CameraWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val w = (wDp * scaleFactor).toInt().coerceAtLeast(420)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(420)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgColor = getSafeBgColor(config)

    val size = minOf(w, h).toFloat()
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    // Parse SVG Path & Matrix scale directly to cardRect
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

        if (cameraConfig.customCaption.isNotEmpty()) {
            val captionText = cameraConfig.customCaption
            val isPolaroid = cameraConfig.borderStyle == PhotoFrameBorder.POLAROID
            val captionColor = if (isPolaroid) Color.parseColor("#121214") else Color.WHITE

            val refCaptionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = getSlateFont(context, weight = 700); textSize = 100f }
            val measuredCapW = refCaptionPaint.measureText(captionText).coerceAtLeast(1f)
            val maxCapW = cardRect.width() * 0.65f
            val maxCapH = if (isPolaroid) polaroidRect!!.height() * 0.45f else cardRect.height() * 0.10f
            val captionFontSize = minOf(maxCapH, 100f * (maxCapW / measuredCapW)).coerceAtLeast(16f)

            val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = captionColor
                textSize = captionFontSize
                typeface = getSlateFont(context, weight = 700)
                textAlign = Paint.Align.CENTER
                if (!isPolaroid) setShadowLayer(6f, 0f, 2f, Color.BLACK)
            }

            if (isPolaroid && polaroidRect != null) {
                val captionX = polaroidRect.centerX()
                val captionY = polaroidRect.centerY() + (captionFontSize * 0.35f)
                canvas.drawText(captionText, captionX, captionY, captionPaint)
            } else {
                val captionX = cardRect.centerX()
                val captionY = cardRect.bottom - (cardRect.height() * 0.12f)
                canvas.drawText(captionText, captionX, captionY, captionPaint)
            }
        }
    }

    canvas.restore()
    return bitmap
}

// 6. STACKED PHOTO FRAME (2x2 / Layered Polaroid Stack Display)
fun generatePhotoFrameStackedCameraBitmap(context: Context, config: SlateWidgetConfig, cameraConfig: CameraWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val w = (wDp * scaleFactor).toInt().coerceAtLeast(420)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(420)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = w / 2f
    val cy = h / 2f
    val cardSize = minOf(w, h) * 0.82f
    val cardRect = RectF(cx - cardSize / 2f, cy - cardSize / 2f, cx + cardSize / 2f, cy + cardSize / 2f)
    val cardRadius = scaleFactor * 14f

    val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(40, 0, 0, 0); style = Paint.Style.FILL }
    val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(45, 0, 0, 0) // Subtle outline to separate overlapping stacked cards
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1f
    }

    // Bottom Card (Rotated -6°)
    canvas.save()
    canvas.rotate(-6f, cx, cy)
    canvas.drawRoundRect(RectF(cardRect).apply { offset(0f, scaleFactor * 2f) }, cardRadius, cardRadius, shadowPaint)
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardBgPaint)
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardBorderPaint)
    canvas.restore()

    // Middle Card (Rotated +5°)
    canvas.save()
    canvas.rotate(5f, cx, cy)
    canvas.drawRoundRect(RectF(cardRect).apply { offset(0f, scaleFactor * 2f) }, cardRadius, cardRadius, shadowPaint)
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardBgPaint)
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardBorderPaint)
    canvas.restore()

    // Top Card (Front / Rotated 0°)
    canvas.save()
    canvas.drawRoundRect(RectF(cardRect).apply { offset(0f, scaleFactor * 3f) }, cardRadius, cardRadius, shadowPaint)
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardBgPaint)
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardBorderPaint)

    // Inner Photo Bounds (Polaroid White Frame Padding)
    val borderPadding = cardSize * 0.05f
    val bottomChin = cardSize * 0.18f
    val innerPhotoRect = RectF(cardRect.left + borderPadding, cardRect.top + borderPadding, cardRect.right - borderPadding, cardRect.bottom - bottomChin)
    val innerRadius = (cardRadius - borderPadding).coerceAtLeast(4f)

    val innerClipPath = Path().apply { addRoundRect(innerPhotoRect, innerRadius, innerRadius, Path.Direction.CW) }
    canvas.save()
    canvas.clipPath(innerClipPath)

    val bgColor = getSafeBgColor(config)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val photoBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(innerPhotoRect, innerRadius, innerRadius, photoBgPaint)

    if (cameraConfig.photoUri.isNullOrEmpty()) {
        drawConfigurePlaceholderState(canvas, context, innerPhotoRect, config, scaleFactor)
        canvas.restore()
        canvas.restore()
        return bitmap
    }

    val loadedBitmap = loadAndCropImage(context, cameraConfig.photoUri, innerPhotoRect.width().toInt(), innerPhotoRect.height().toInt())

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

        canvas.drawBitmap(loadedBitmap, null, innerPhotoRect, imagePaint)

        if (cameraConfig.borderStyle == PhotoFrameBorder.VIGNETTE) {
            val vigGradient = RadialGradient(innerPhotoRect.centerX(), innerPhotoRect.centerY(), innerPhotoRect.width() * 0.5f, intArrayOf(Color.TRANSPARENT, Color.argb(200, 0, 0, 0)), floatArrayOf(0.6f, 1.0f), Shader.TileMode.CLAMP)
            canvas.drawRoundRect(innerPhotoRect, innerRadius, innerRadius, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = vigGradient })
        }
    }

    canvas.restore()

    // Caption Drawn Centered inside Bottom Polaroid Chin
    if (cameraConfig.customCaption.isNotEmpty()) {
        val captionText = cameraConfig.customCaption
        val polaroidChinRect = RectF(cardRect.left, cardRect.bottom - bottomChin, cardRect.right, cardRect.bottom)

        val refCaptionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = getSlateFont(context, weight = 700); textSize = 100f }
        val measuredCapW = refCaptionPaint.measureText(captionText).coerceAtLeast(1f)
        val maxCapW = cardRect.width() * 0.80f
        val maxCapH = polaroidChinRect.height() * 0.50f
        val captionFontSize = minOf(maxCapH, 100f * (maxCapW / measuredCapW)).coerceAtLeast(14f)

        val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#121214")
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
