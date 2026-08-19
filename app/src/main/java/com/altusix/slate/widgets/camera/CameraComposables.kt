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
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

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
    val cardSize = minOf(w, h) * 0.84f
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

// 7. TAPED POLAROID PHOTO FRAME (2x2 / Masking Tape Mounted Display)
fun generatePhotoFrameTapedCameraBitmap(context: Context, config: SlateWidgetConfig, cameraConfig: CameraWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val w = (wDp * scaleFactor).toInt().coerceAtLeast(420)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(420)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = w / 2f
    val cy = h / 2f
    val cardSize = minOf(w, h) * 0.90f // Increased from 0.78f to fill more boundary space
    val cardRect = RectF(cx - cardSize / 2f, cy - cardSize / 2f, cx + cardSize / 2f, cy + cardSize / 2f)
    val cardRadius = scaleFactor * 12f

    val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(35, 0, 0, 0); style = Paint.Style.FILL }
    val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(35, 0, 0, 0)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1.0f
    }

    canvas.save()
    canvas.rotate(-3.5f, cx, cy)

    canvas.drawRoundRect(RectF(cardRect).apply { offset(0f, scaleFactor * 3f) }, cardRadius, cardRadius, shadowPaint)
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardBgPaint)
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardBorderPaint)

    val borderPadding = cardSize * 0.05f
    val bottomChin = cardSize * 0.20f
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
        drawMaskingTape(canvas, cardRect, scaleFactor)
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

    drawMaskingTape(canvas, cardRect, scaleFactor)

    canvas.restore()
    return bitmap
}
private fun drawMaskingTape(canvas: Canvas, cardRect: RectF, scaleFactor: Float) {
    val tapeW = cardRect.width() * 0.38f
    val tapeH = cardRect.height() * 0.11f

    // Position pulled down onto photo edge & slightly off-center to the left
    val tapeX = cardRect.centerX() - tapeW / 2f - (cardRect.width() * 0.0f)
    val tapeY = cardRect.top + (cardRect.height() * -0.034f)
    val left = tapeX
    val right = left + tapeW
    val top = tapeY
    val bottom = top + tapeH

    canvas.save()
    // Counter-rotate relative to polaroid frame for an imperfect, realistic tilt
    canvas.rotate(2.8f, (left + right) / 2f, (top + bottom) / 2f)

    val tapePath = Path().apply {
        // Top edge
        moveTo(left, top)
        lineTo(right, top)

        // Right torn edge
        lineTo(right - scaleFactor * 1.5f, top + tapeH * 0.18f)
        lineTo(right + scaleFactor * 0.6f, top + tapeH * 0.38f)
        lineTo(right - scaleFactor * 2.0f, top + tapeH * 0.62f)
        lineTo(right + scaleFactor * 0.4f, top + tapeH * 0.82f)
        lineTo(right, bottom)

        // Bottom edge
        lineTo(left, bottom)

        // Left torn edge
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

// 8. PUSH PIN POLAROID PHOTO FRAME (2x2 / Red Thumbtack Mounted Display)
fun generatePhotoFramePushPinCameraBitmap(context: Context, config: SlateWidgetConfig, cameraConfig: CameraWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val w = (wDp * scaleFactor).toInt().coerceAtLeast(420)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(420)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = w / 2f
    val cy = (h / 2f) + (scaleFactor * 8f) // Offset down to provide top headroom for the pin
    val cardSize = minOf(w, h) * 0.84f // Reduced slightly from 0.88f to prevent pin head clipping
    val cardRect = RectF(cx - cardSize / 2f, cy - cardSize / 2f, cx + cardSize / 2f, cy + cardSize / 2f)
    val cardRadius = scaleFactor * 12f

    val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(35, 0, 0, 0); style = Paint.Style.FILL }
    val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(35, 0, 0, 0)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1.0f
    }

    canvas.save()
    canvas.rotate(3.2f, cx, cy)

    canvas.drawRoundRect(RectF(cardRect).apply { offset(0f, scaleFactor * 3f) }, cardRadius, cardRadius, shadowPaint)
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardBgPaint)
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardBorderPaint)

    val borderPadding = cardSize * 0.05f
    val bottomChin = cardSize * 0.20f
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
        drawRedPushPin(canvas, cardRect)
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

    // 1. Long Soft Drop Shadow
    val shadowMat1 = matrixOf(1.2623f, -5.763f, 192.04f, 3.1595f, 2.4855f, -379.19f, 0f, 0f, 1f)
    val shadowGrad1 = RadialGradient(278.53f, 12.798f, 7.3868f, intArrayOf(Color.argb(250, 0, 0, 0), Color.argb(150, 0, 0, 0), Color.TRANSPARENT), floatArrayOf(0f, 0.51f, 1f), Shader.TileMode.CLAMP).apply {
        setLocalMatrix(matrixOf(1.6668f, -.43535f, -180.07f, .25023f, 1.4424f, -76.258f, 0f, 0f, 1f))
    }
    canvas.save()
    canvas.concat(shadowMat1)
    canvas.drawOval(ovalRect, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = shadowGrad1; alpha = 158 })
    canvas.restore()

    // 2. Large Red Base Dome
    val baseMat = matrixOf(3.4214f, 0f, -545.23f, 0f, 3.4413f, 495.42f, 0f, 0f, 1f)
    val baseGrad = RadialGradient(305.98f, 4.6951f, 8.9206f, intArrayOf(Color.parseColor("#F60000"), Color.parseColor("#B30000")), null, Shader.TileMode.CLAMP)
    canvas.save()
    canvas.concat(baseMat)
    canvas.drawOval(baseBounds, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = baseGrad })
    canvas.restore()

    // 3. Secondary Contact Shadow
    val shadowMat2 = matrixOf(1.3571f, -2.5511f, 136.44f, 2.2762f, 1.241f, -137.78f, 0f, 0f, 1f)
    canvas.save()
    canvas.concat(shadowMat2)
    canvas.drawOval(ovalRect, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = shadowGrad1; alpha = 77 })
    canvas.restore()

    // 4. Dark Inner Ring Shading on Base
    val innerShadowGrad = RadialGradient(302.83f, 4.6951f, 8.9206f, intArrayOf(Color.argb(149, 127, 0, 0), Color.argb(127, 132, 0, 0), Color.TRANSPARENT), floatArrayOf(0f, 0.6667f, 1f), Shader.TileMode.CLAMP)
    val innerShadowMat = matrixOf(2.1108f, 0f, -144.39f, 0f, 2.1231f, 495.36f, 0f, 0f, 1f)
    canvas.save()
    canvas.concat(innerShadowMat)
    canvas.drawOval(baseBounds, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = innerShadowGrad })
    canvas.restore()

    // 5. Bottom-left Highlight Curve
    val highlightPathBot = PathParser.createPathFromPathData("M474.2,531.26 c-6.4976,-5.2202 -8.2466,-8.1777 -7.2576,-14.181 2.8176,7.7769 6.9716,13.737 7.2576,14.181z")
    val highlightGradBot = RadialGradient(537.75f, 228.65f, 0.74646f, intArrayOf(Color.WHITE, Color.TRANSPARENT), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP).apply {
        setLocalMatrix(matrixOf(-4.0973f, -10.635f, 5102.7f, 2.6711f, -16.503f, 2861.5f, 0f, 0f, 1f))
    }
    canvas.drawPath(highlightPathBot, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = highlightGradBot; alpha = 188 })

    // 6. Top Angled Sphere Head
    val headMat = matrixOf(2.3962f, 0f, -216.26f, .18197f, 2.4872f, 421.67f, 0f, 0f, 1f)
    val headGrad = RadialGradient(302.66f, 3.251f, 8.9206f, intArrayOf(Color.parseColor("#D43500"), Color.parseColor("#D42400"), Color.parseColor("#D40000"), Color.parseColor("#950000")), floatArrayOf(0f, 0.48052f, 0.73611f, 1f), Shader.TileMode.CLAMP).apply {
        setLocalMatrix(matrixOf(.75426f, -.6773f, 77.596f, .68023f, .80831f, -205.09f, 0f, 0f, 1f))
    }
    canvas.save()
    canvas.concat(headMat)
    canvas.drawOval(baseBounds, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = headGrad })
    canvas.restore()

    // 7. Top-right Crescent Highlight
    val highlightPathTop = PathParser.createPathFromPathData("M514.32,468.93 c10.35,6.9283 13.318,10.867 12.538,18.896 -5.1338,-10.371 -12.058,-18.305 -12.538,-18.896z")
    val highlightGradTop = RadialGradient(537.75f, 228.65f, 0.74646f, intArrayOf(Color.WHITE, Color.TRANSPARENT), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP).apply {
        setLocalMatrix(matrixOf(5.8281f, 17.872f, -6695.6f, -3.599f, 21.975f, -2611.2f, 0f, 0f, 1f))
    }
    canvas.drawPath(highlightPathTop, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = highlightGradTop })

    canvas.restore()
}

// 9. SHUTTER LAUNCHER (2x2 / Minimal Camera Trigger Display)
fun generateCameraShutterLauncherBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val w = (wDp * scaleFactor).toInt().coerceAtLeast(420)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(420)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val size = minOf(w, h).toFloat()
        val leftX = (w - size) / 2f
        val topY = (h - size) / 2f
        RectF(leftX, topY, leftX + size, topY + size)
    }

    val minDim = minOf(cardRect.width(), cardRect.height())
    val cornerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val cx = cardRect.centerX()
    val cy = cardRect.centerY()

    // Refined, ultra-sleek stroke parameters
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

    // Top-Left Bracket
    canvas.drawPath(Path().apply { moveTo(bLeft, bTop + bracketLength); lineTo(bLeft, bTop); lineTo(bLeft + bracketLength, bTop) }, bracketPaint)

    // Top-Right Bracket
    canvas.drawPath(Path().apply { moveTo(bRight - bracketLength, bTop); lineTo(bRight, bTop); lineTo(bRight, bTop + bracketLength) }, bracketPaint)

    // Bottom-Left Bracket
    canvas.drawPath(Path().apply { moveTo(bLeft, bBottom - bracketLength); lineTo(bLeft, bBottom); lineTo(bLeft + bracketLength, bBottom) }, bracketPaint)

    // Bottom-Right Bracket
    canvas.drawPath(Path().apply { moveTo(bRight - bracketLength, bBottom); lineTo(bRight, bBottom); lineTo(bRight, bBottom - bracketLength) }, bracketPaint)

    // Sleek Camera Shutter Ring & Center Trigger
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
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val w = (wDp * scaleFactor).toInt().coerceAtLeast(480)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(240)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val isLight = config.themeMode == "LIGHT"
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val targetRatio = 2.0f
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

    val pillH = cardRect.height()
    val pillRadius = pillH / 2f
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()

    // 1. Solid Capsule Background
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, pillRadius, pillRadius, bgPaint)

    // 2. Subtle Micro Glass Outline
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(15, 0, 0, 0) else Color.argb(25, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1.0f
    }
    canvas.drawRoundRect(cardRect, pillRadius, pillRadius, borderPaint)

    val cy = cardRect.centerY()
    val paddingX = pillH * 0.42f

    // Fetch real device hardware specs
    val (mainText, subText) = getDeviceCameraSpecs(context)

    // Sleek, refined primary text (600 SemiBold instead of 800 ExtraBold)
    val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        typeface = getSlateFont(context, weight = 600)
        textSize = pillH * 0.22f
        textAlign = Paint.Align.LEFT
        letterSpacing = -0.01f
    }

    // Secondary lens metadata text
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

    // Clean text drawing without overlapping dots
    canvas.drawText(mainText, textLeft, mainY, mainPaint)
    canvas.drawText(subText, textLeft, subY, subPaint)

    // Right-side Minimal Tactile Shutter Trigger
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

            // Converts physical sensor focal length into 35mm full-frame equivalent
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
