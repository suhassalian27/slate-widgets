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

// 1. PHOTO FRAME & GALLERY (2x2 / Responsive & Fixed Aspect Photo Display)
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
