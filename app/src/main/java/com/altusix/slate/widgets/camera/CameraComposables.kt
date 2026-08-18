package com.altusix.slate.widgets.camera

import android.content.Context
import android.graphics.*
import android.net.Uri
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.getSafeBgColor
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius

fun generatePhotoFrameWidgetBitmap(
    context: Context,
    config: SlateWidgetConfig,
    cameraConfig: CameraWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val w = (wDp * scaleFactor).toInt().coerceAtLeast(420)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(420)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#80FFFFFF")

    // Aspect Ratio Standard (Fixed 1:1 vs Responsive)
    val cardRect = if (cameraConfig.isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val size = minOf(w, h).toFloat()
        val leftX = (w - size) / 2f
        val topY = (h - size) / 2f
        RectF(leftX, topY, leftX + size, topY + size)
    }

    // Corner Radius Standard
    val cornerRadius = getStandardCornerRadius(displayDensity) * scaleFactor / displayDensity
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
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.STROKE
            strokeWidth = scaleFactor * 3f
            strokeCap = Paint.Cap.ROUND
        }

        val cx = cardRect.centerX()
        val cy = cardRect.centerY() - (cardRect.height() * 0.08f)
        val iconSize = cardRect.width() * 0.18f

        val camRect = RectF(cx - iconSize, cy - (iconSize * 0.75f), cx + iconSize, cy + (iconSize * 0.75f))
        canvas.drawRoundRect(camRect, iconSize * 0.25f, iconSize * 0.25f, iconPaint)
        canvas.drawCircle(cx, cy, iconSize * 0.45f, iconPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = cardRect.width() * 0.075f
            typeface = getSlateFont(context, weight = 800)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = cardRect.width() * 0.052f
            typeface = getSlateFont(context, weight = 600)
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("Configure Photo", cx, cy + iconSize + (scaleFactor * 10f), titlePaint)
        canvas.drawText("Tap to select an image", cx, cy + iconSize + (scaleFactor * 24f), subPaint)

        canvas.restore()
        return bitmap
    }

    // =========================================================================
    // STATE B: PHOTO DISPLAYED WITH FILTERS & OVERLAYS
    // =========================================================================
    val loadedBitmap = loadAndCropImage(context, cameraConfig.photoUri, cardRect.width().toInt(), cardRect.height().toInt())

    if (loadedBitmap != null) {
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG)

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
                val cm = ColorMatrix().apply {
                    setScale(0.7f, 0.7f, 0.7f, 1f)
                }
                imagePaint.colorFilter = ColorMatrixColorFilter(cm)
            }
            else -> {}
        }

        canvas.drawBitmap(loadedBitmap, null, cardRect, imagePaint)

        when (cameraConfig.borderStyle) {
            PhotoFrameBorder.POLAROID -> {
                val polaroidBottomH = cardRect.height() * 0.22f
                val polaroidRect = RectF(cardRect.left, cardRect.bottom - polaroidBottomH, cardRect.right, cardRect.bottom)
                val polPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                canvas.drawRect(polaroidRect, polPaint)
            }
            PhotoFrameBorder.VIGNETTE -> {
                val vignetteGradient = RadialGradient(
                    cardRect.centerX(), cardRect.centerY(), cardRect.width() * 0.7f,
                    intArrayOf(Color.TRANSPARENT, Color.argb(180, 0, 0, 0)),
                    floatArrayOf(0.6f, 1.0f),
                    Shader.TileMode.CLAMP
                )
                val vigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = vignetteGradient
                }
                canvas.drawRect(cardRect, vigPaint)
            }
            PhotoFrameBorder.THIN_BORDER -> {
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(120, 255, 255, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = scaleFactor * 3f
                }
                canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)
            }
            else -> {}
        }

        if (cameraConfig.customCaption.isNotEmpty()) {
            val captionText = cameraConfig.customCaption
            val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (cameraConfig.borderStyle == PhotoFrameBorder.POLAROID) Color.parseColor("#1C1C1E") else Color.WHITE
                textSize = cardRect.width() * 0.065f
                typeface = getSlateFont(context, weight = 700)
                textAlign = Paint.Align.LEFT
                if (cameraConfig.borderStyle != PhotoFrameBorder.POLAROID) {
                    setShadowLayer(6f, 0f, 2f, Color.BLACK)
                }
            }

            val captionX = cardRect.left + (cardRect.width() * 0.08f)
            val captionY = cardRect.bottom - (cardRect.height() * 0.08f)
            canvas.drawText(captionText, captionX, captionY, captionPaint)
        }
    }

    canvas.restore()
    return bitmap
}

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