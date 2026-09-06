package com.altusix.slate.widgets.media

import android.content.Context
import android.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.createSupersampledCanvas
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius
import kotlin.math.cos
import kotlin.math.sin

// =========================================================================
// CANVAS BITMAP GENERATORS FOR SLATE "MUSIC & MEDIA" WIDGETS
// =========================================================================

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000L
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "%d:%02d".format(min, sec)
}

/**
 * Helper to draw a play or pause button centered at (cx, cy)
 */
private fun drawPlayPauseIcon(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    radius: Float,
    isPlaying: Boolean,
    color: Int,
    fillCircleBg: Boolean = false,
    circleBgColor: Int = 0
) {
    if (fillCircleBg) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = circleBgColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius, bgPaint)
    }

    val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    val iconR = radius * (if (fillCircleBg) 0.44f else 0.85f)

    if (isPlaying) {
        // Two vertical pause bars
        val barW = iconR * 0.32f
        val barH = iconR * 1.3f
        val gap = iconR * 0.22f
        val r = barW * 0.35f
        canvas.drawRoundRect(RectF(cx - gap - barW, cy - barH / 2f, cx - gap, cy + barH / 2f), r, r, iconPaint)
        canvas.drawRoundRect(RectF(cx + gap, cy - barH / 2f, cx + gap + barW, cy + barH / 2f), r, r, iconPaint)
    } else {
        // Right-pointing play triangle
        val path = Path().apply {
            moveTo(cx - iconR * 0.45f, cy - iconR * 0.65f)
            lineTo(cx + iconR * 0.65f, cy)
            lineTo(cx - iconR * 0.45f, cy + iconR * 0.65f)
            close()
        }
        canvas.drawPath(path, iconPaint)
    }
}

/**
 * Helper to draw skip next/previous icons centered at (cx, cy)
 */
private fun drawSkipIcon(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    radius: Float,
    isNext: Boolean,
    color: Int
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    val r = radius * 0.52f
    val dir = if (isNext) 1f else -1f

    val path = Path().apply {
        // Arrow triangle
        moveTo(cx - dir * r * 0.5f, cy - r * 0.6f)
        lineTo(cx + dir * r * 0.3f, cy)
        lineTo(cx - dir * r * 0.5f, cy + r * 0.6f)
        close()
    }
    canvas.drawPath(path, paint)

    // Stop vertical line
    val lineW = r * 0.22f
    val lineH = r * 1.15f
    val lineX = cx + dir * r * 0.45f
    val corner = lineW * 0.3f
    canvas.drawRoundRect(
        RectF(lineX - lineW / 2f, cy - lineH / 2f, lineX + lineW / 2f, cy + lineH / 2f),
        corner, corner, paint
    )
}

/**
 * Draw cropped/scaled bitmap inside a target rect with rounded corners
 */
private fun drawRoundedBitmap(
    canvas: Canvas,
    bitmap: Bitmap,
    rect: RectF,
    cornerRadius: Float
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val matrix = Matrix()
    val srcW = bitmap.width.toFloat()
    val srcH = bitmap.height.toFloat()
    val dstW = rect.width()
    val dstH = rect.height()

    val scale = maxOf(dstW / srcW, dstH / srcH)
    val dx = rect.left + (dstW - srcW * scale) / 2f
    val dy = rect.top + (dstH - srcH * scale) / 2f

    matrix.setScale(scale, scale)
    matrix.postTranslate(dx, dy)

    val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    shader.setLocalMatrix(matrix)
    paint.shader = shader

    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
}

/**
 * Draw circular cropped bitmap
 */
private fun drawCircularBitmap(
    canvas: Canvas,
    bitmap: Bitmap,
    cx: Float,
    cy: Float,
    radius: Float
) {
    val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
    drawRoundedBitmap(canvas, bitmap, rect, radius)
}

// =========================================================================
// 1. VINYL TURNTABLE (2x2)
// =========================================================================
fun generateVinylPlayerBitmap(
    context: Context,
    state: SlateMediaState,
    artwork: Bitmap?,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"
    val primaryTextColor = if (isLight) Color(0xFF141416).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF8E8E93).toArgb() else Color(0xFFA1A1A6).toArgb()

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val contentW = cardRect.width()
    val contentH = cardRect.height()
    val effectiveDim = minOf(contentW, contentH)
    val uiScale = (effectiveDim / (160f * scaleFactor)).coerceIn(0.5f, 2.5f)
    val pad = contentW * 0.07f

    // 1. Bottom-Up Controls & Compact Typography
    val btnR = scaleFactor * 11.5f * uiScale
    val ctrlY = cardRect.bottom - (scaleFactor * 16f * uiScale)
    val ctrlSpacing = minOf(contentW * 0.26f, scaleFactor * 42f * uiScale)

    val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = scaleFactor * 8.2f * uiScale
        typeface = getSlateFont(context, 400)
    }
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = scaleFactor * 10.8f * uiScale
        typeface = getSlateFont(context, 700)
    }

    val artistY = ctrlY - (btnR * 1.30f) - (scaleFactor * 5f * uiScale)
    val titleY = artistY - artistPaint.textSize - (scaleFactor * 3.5f * uiScale)

    // 2. Horizontally Centered & Enlarged Vinyl Stage
    val availableVinylTop = cardRect.top + (scaleFactor * 8f * uiScale)
    val availableVinylBottom = titleY - titlePaint.textSize - (scaleFactor * 6f * uiScale)
    val vinylCenterY = (availableVinylTop + availableVinylBottom) / 2f
    val vinylCenterX = cardRect.centerX()

    val maxRadiusByH = (availableVinylBottom - availableVinylTop) / 2f
    val maxRadiusByW = (contentW / 2f) - (pad * 0.7f)
    val vinylRadius = minOf(maxRadiusByH, maxRadiusByW).coerceAtLeast(scaleFactor * 26f)

    // 3. Rotating Grooves (Grooves rotate while Center Art stays straight)
    val vinylRotation = if (state.isPlaying && state.positionMs > 0L) {
        ((state.positionMs / 1000f) * 45f) % 360f
    } else {
        0f
    }

    // Rotated vinyl outer body & grooves
    canvas.save()
    canvas.rotate(vinylRotation, vinylCenterX, vinylCenterY)

    val vinylPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF202024).toArgb() else Color(0xFF111113).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(vinylCenterX, vinylCenterY, vinylRadius, vinylPaint)

    val groovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0x28FFFFFF).toArgb() else Color(0x15FFFFFF).toArgb()
        style = Paint.Style.STROKE
        strokeWidth = 1.0f * scaleFactor
    }
    for (i in 1..4) {
        val r = vinylRadius * (0.42f + i * 0.12f)
        canvas.drawCircle(vinylCenterX, vinylCenterY, r, groovePaint)
    }
    canvas.restore()

    // 4. Straight Center Vinyl Label (Unrotated)
    val labelRadius = vinylRadius * 0.42f
    if (artwork != null && !artwork.isRecycled) {
        drawCircularBitmap(canvas, artwork, vinylCenterX, vinylCenterY, labelRadius)
    } else {
        val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(vinylCenterX, vinylCenterY, labelRadius, labelBgPaint)
        val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.White.toArgb() else Color.Black.toArgb()
            style = Paint.Style.FILL
            textSize = labelRadius * 0.8f
            textAlign = Paint.Align.CENTER
            typeface = getSlateFont(context, 700)
        }
        canvas.drawText("♪", vinylCenterX, vinylCenterY + labelRadius * 0.30f, notePaint)
    }

    // Center Spindle Hole
    val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.White.toArgb() else Color.Black.toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(vinylCenterX, vinylCenterY, labelRadius * 0.20f, holePaint)

    // 5. Reactive Tonearm
    val armPivotX = cardRect.right - pad * 1.1f
    val armPivotY = cardRect.top + pad * 1.2f

    val pivotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color(0xFF6E6E73).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(armPivotX, armPivotY, vinylRadius * 0.12f, pivotPaint)

    val armPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color(0xFFA1A1A6).toArgb()
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * scaleFactor
        strokeCap = Paint.Cap.ROUND
    }

    val armTargetX = if (state.isPlaying) vinylCenterX + vinylRadius * 0.60f else armPivotX - vinylRadius * 0.12f
    val armTargetY = if (state.isPlaying) vinylCenterY + vinylRadius * 0.35f else armPivotY + vinylRadius * 0.75f
    val armMidX = if (state.isPlaying) armPivotX - vinylRadius * 0.18f else armPivotX - vinylRadius * 0.05f
    val armMidY = if (state.isPlaying) armPivotY + vinylRadius * 0.50f else armPivotY + vinylRadius * 0.45f

    val armPath = Path().apply {
        moveTo(armPivotX, armPivotY)
        lineTo(armMidX, armMidY)
        lineTo(armTargetX, armTargetY)
    }
    canvas.drawPath(armPath, armPaint)

    val cartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(
        RectF(
            armTargetX - 3.5f * scaleFactor,
            armTargetY - 2.5f * scaleFactor,
            armTargetX + 7.5f * scaleFactor,
            armTargetY + 5.5f * scaleFactor
        ),
        1.5f * scaleFactor, 1.5f * scaleFactor, cartPaint
    )

    // 6. Track Info Typography
    val maxTextW = contentW - pad * 2f

    val titleDisplay = if (titlePaint.measureText(state.title) > maxTextW) {
        var t = state.title
        while (t.isNotEmpty() && titlePaint.measureText("$t…") > maxTextW) t = t.dropLast(1)
        "$t…"
    } else state.title

    val artistDisplay = if (artistPaint.measureText(state.artist) > maxTextW) {
        var a = state.artist
        while (a.isNotEmpty() && artistPaint.measureText("$a…") > maxTextW) a = a.dropLast(1)
        "$a…"
    } else state.artist

    canvas.drawText(titleDisplay, cardRect.left + pad, titleY, titlePaint)
    canvas.drawText(artistDisplay, cardRect.left + pad, artistY, artistPaint)

    // 7. Controls Deck
    drawSkipIcon(canvas, cardRect.centerX() - ctrlSpacing, ctrlY, btnR, isNext = false, color = secondaryTextColor)

    drawPlayPauseIcon(
        canvas,
        cardRect.centerX(),
        ctrlY,
        btnR * 1.15f,
        isPlaying = state.isPlaying,
        color = if (isLight) Color.White.toArgb() else Color.Black.toArgb(),
        fillCircleBg = true,
        circleBgColor = accentColor
    )

    drawSkipIcon(canvas, cardRect.centerX() + ctrlSpacing, ctrlY, btnR, isNext = true, color = secondaryTextColor)

    return bitmap
}

// =========================================================================
// 2. BENTO MEDIA PLAYER (4x2 - FIXED RATIO)
// =========================================================================
fun generateBentoMediaBitmap(
    context: Context,
    state: SlateMediaState,
    artwork: Bitmap?,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"
    val primaryTextColor = if (isLight) Color(0xFF141416).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF6C6C70).toArgb() else Color(0xFF8E8E93).toArgb()

    // 1. Fixed Aspect Ratio (2.1:1) Centered Base Plate
    val idealAspect = 2.10f
    var cardW = w
    var cardH = cardW / idealAspect
    if (cardH > h) {
        cardH = h
        cardW = cardH * idealAspect
    }
    val leftX = (w - cardW) / 2f
    val topY = (h - cardH) / 2f
    val cardRect = RectF(leftX, topY, leftX + cardW, topY + cardH)

    val cardCornerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(cardH / 2f)
    val uiScale = (cardH / (130f * scaleFactor)).coerceIn(0.55f, 2.5f)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    // 2. Left Cell: Full-Bleed Square Album Art
    val artSize = cardH
    val artRect = RectF(cardRect.left, cardRect.top, cardRect.left + artSize, cardRect.bottom)
    val artRadii = floatArrayOf(
        cardCornerRadius, cardCornerRadius, // Top-Left
        0f, 0f,                             // Top-Right
        0f, 0f,                             // Bottom-Right
        cardCornerRadius, cardCornerRadius  // Bottom-Left
    )
    val artClipPath = Path().apply { addRoundRect(artRect, artRadii, Path.Direction.CW) }

    canvas.save()
    canvas.clipPath(artClipPath)
    if (artwork != null && !artwork.isRecycled) {
        val srcW = artwork.width.toFloat()
        val srcH = artwork.height.toFloat()
        val scale = maxOf(artRect.width() / srcW, artRect.height() / srcH)
        val dx = artRect.left + (artRect.width() - srcW * scale) / 2f
        val dy = artRect.top + (artRect.height() - srcH * scale) / 2f

        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(dx, dy)
        }
        val artPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(artwork, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(matrix)
            }
        }
        canvas.drawRect(artRect, artPaint)
    } else {
        val artBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color(0xFFE5E5EA).toArgb() else Color(0xFF1E1E22).toArgb()
            style = Paint.Style.FILL
        }
        canvas.drawRect(artRect, artBgPaint)

        val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = artSize * 0.36f
            textAlign = Paint.Align.CENTER
            typeface = getSlateFont(context, 700)
        }
        canvas.drawText("♫", artRect.centerX(), artRect.centerY() + artSize * 0.12f, glyphPaint)
    }
    canvas.restore()

    // 3. Right Cell: Top Equalizer & Full-Width Metadata
    val padRight = scaleFactor * 16f * uiScale
    val rightLeft = artRect.right + padRight
    val rightRight = cardRect.right - padRight
    val availableW = rightRight - rightLeft

    // Equalizer: Top Right
    val eqBarCount = 5
    val eqBarW = scaleFactor * 2.4f * uiScale
    val eqStartX = rightRight - (eqBarCount * eqBarW * 2f)
    val eqBaselineY = cardRect.top + (cardH * 0.22f)

    val eqPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }
    val eqHeights = if (state.isPlaying) floatArrayOf(0.7f, 1.0f, 0.45f, 0.85f, 0.6f) else floatArrayOf(0.2f, 0.2f, 0.2f, 0.2f, 0.2f)
    for (i in 0 until eqBarCount) {
        val bH = scaleFactor * 10f * uiScale * eqHeights[i]
        val bX = eqStartX + i * eqBarW * 2f
        canvas.drawRoundRect(
            RectF(bX, eqBaselineY - bH, bX + eqBarW, eqBaselineY),
            eqBarW * 0.5f, eqBarW * 0.5f, eqPaint
        )
    }

    // Title & Artist
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = scaleFactor * 12.5f * uiScale
        typeface = getSlateFont(context, 700)
    }
    val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = scaleFactor * 9.5f * uiScale
        typeface = getSlateFont(context, 400)
    }

    val titleDisplay = if (titlePaint.measureText(state.title) > availableW) {
        var t = state.title
        while (t.isNotEmpty() && titlePaint.measureText("$t…") > availableW) t = t.dropLast(1)
        "$t…"
    } else state.title

    val artistDisplay = if (artistPaint.measureText(state.artist) > availableW) {
        var a = state.artist
        while (a.isNotEmpty() && artistPaint.measureText("$a…") > availableW) a = a.dropLast(1)
        "$a…"
    } else state.artist

    val titleY = cardRect.top + (cardH * 0.38f)
    val artistY = titleY + (scaleFactor * 13.5f * uiScale)

    canvas.drawText(titleDisplay, rightLeft, titleY, titlePaint)
    canvas.drawText(artistDisplay, rightLeft, artistY, artistPaint)

    // 4. Media Controls Deck (1:1 Synchronized to XML 1/3 Partitions)
    val colLeft = artRect.right
    val colRight = cardRect.right
    val colW = colRight - colLeft

    // Button centers at 1/6, 3/6, and 5/6 of the controls column
    val prevCx = colLeft + (colW * (1f / 6f))
    val playCx = colLeft + (colW * (3f / 6f))
    val nextCx = colLeft + (colW * (5f / 6f))
    val controlsCenterY = cardRect.top + (cardH * 0.73f)

    val playBtnR = scaleFactor * 15.5f * uiScale
    val skipBtnR = scaleFactor * 13.0f * uiScale

    // Prev Button (Centered inside btn_media_prev slot)
    drawSkipIcon(canvas, prevCx, controlsCenterY, skipBtnR, isNext = false, color = secondaryTextColor)

    // Play / Pause Circle (Centered inside btn_media_play_pause slot)
    drawPlayPauseIcon(
        canvas,
        playCx,
        controlsCenterY,
        playBtnR,
        isPlaying = state.isPlaying,
        color = if (isLight) Color.White.toArgb() else Color.Black.toArgb(),
        fillCircleBg = true,
        circleBgColor = accentColor
    )

    // Next Button (Centered inside btn_media_next slot)
    drawSkipIcon(canvas, nextCx, controlsCenterY, skipBtnR, isNext = true, color = secondaryTextColor)

    return bitmap
}

// =========================================================================
// 3. CAPSULE PILL PLAYER (4x1 - FIXED RATIO)
// =========================================================================
fun generateCapsulePillBitmap(
    context: Context,
    state: SlateMediaState,
    artwork: Bitmap?,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"
    val primaryTextColor = if (isLight) Color(0xFF141416).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF6C6C70).toArgb() else Color(0xFF8E8E93).toArgb()

    // 1. Locked 4x1 Aspect Ratio (3.9:1) Centered Base Plate
    val idealAspect = 3.90f
    var cardW = w
    var cardH = cardW / idealAspect
    if (cardH > h) {
        cardH = h
        cardW = cardH * idealAspect
    }
    val leftX = (w - cardW) / 2f
    val topY = (h - cardH) / 2f
    val cardRect = RectF(leftX, topY, leftX + cardW, topY + cardH)

    val pillRadius = cardH / 2f
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, pillRadius, pillRadius, bgPaint)

    val pad = cardH * 0.13f

    // 2. Circular Album Art
    val artR = (cardH / 2f) - pad
    val artCx = cardRect.left + pillRadius
    val artCy = cardRect.centerY()

    if (artwork != null && !artwork.isRecycled) {
        drawCircularBitmap(canvas, artwork, artCx, artCy, artR)
    } else {
        val artBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color(0xFFE5E5EA).toArgb() else Color(0xFF222226).toArgb()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(artCx, artCy, artR, artBgPaint)
        val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = artR * 0.9f
            textAlign = Paint.Align.CENTER
            typeface = getSlateFont(context, 700)
        }
        canvas.drawText("♪", artCx, artCy + artR * 0.32f, glyphPaint)
    }

    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color(0x30FFFFFF).toArgb()
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * scaleFactor
    }
    canvas.drawCircle(artCx, artCy, artR, ringPaint)

    // 3. Media Controls Deck (Synchronized 1:1 to XML 58% / 42% Split)
    val colLeft = cardRect.left + cardW * 0.58f
    val colW = cardW * 0.42f

    // Button centers at 1/3.1, 1.55/3.1, and 2.6/3.1 of the controls column
    val prevCx = colLeft + (colW * (0.50f / 3.1f))
    val playCx = colLeft + (colW * (1.55f / 3.1f))
    val nextCx = colLeft + (colW * (2.60f / 3.1f))
    val controlsCy = cardRect.centerY()

    val skipBtnR = cardH * 0.17f
    val playBtnR = cardH * 0.25f

    // Prev Button
    drawSkipIcon(canvas, prevCx, controlsCy, skipBtnR, isNext = false, color = secondaryTextColor)

    // Play / Pause Circle
    drawPlayPauseIcon(
        canvas,
        playCx,
        controlsCy,
        playBtnR,
        isPlaying = state.isPlaying,
        color = if (isLight) Color.White.toArgb() else Color.Black.toArgb(),
        fillCircleBg = true,
        circleBgColor = accentColor
    )

    // Next Button
    drawSkipIcon(canvas, nextCx, controlsCy, skipBtnR, isNext = true, color = secondaryTextColor)

    // 4. Reduced Typography & Middle Track Info
    val textLeft = artCx + artR + (cardH * 0.14f)
    val textRight = colLeft - (cardH * 0.08f)
    val maxTextW = (textRight - textLeft).coerceAtLeast(10f)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardH * 0.18f
        typeface = getSlateFont(context, 700)
    }
    val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = cardH * 0.13f
        typeface = getSlateFont(context, 400)
    }

    val titleDisplay = if (titlePaint.measureText(state.title) > maxTextW) {
        var t = state.title
        while (t.isNotEmpty() && titlePaint.measureText("$t…") > maxTextW) t = t.dropLast(1)
        "$t…"
    } else state.title

    val artistDisplay = if (artistPaint.measureText(state.artist) > maxTextW) {
        var a = state.artist
        while (a.isNotEmpty() && artistPaint.measureText("$a…") > maxTextW) a = a.dropLast(1)
        "$a…"
    } else state.artist

    val titleY = artCy - (cardH * 0.03f)
    val artistY = titleY + (titlePaint.textSize * 0.85f) + (scaleFactor * 3.5f)

    canvas.drawText(titleDisplay, textLeft, titleY, titlePaint)
    canvas.drawText(artistDisplay, textLeft, artistY, artistPaint)

    return bitmap
}

// =========================================================================
// 4. IMMERSIVE ARTWORK CANVAS (4x1 - RESPONSIVE & FIXED)
// =========================================================================
fun generateMiniCapsuleBitmap(
    context: Context,
    state: SlateMediaState,
    artwork: Bitmap?,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()

    // Base card bounds
    val margin = scaleFactor * 1.5f
    val availW = w - (margin * 2f)
    val availH = h - (margin * 2f)

    val cardRect = if (isResponsive) {
        // Responsive: Card takes full widget canvas
        RectF(margin, margin, margin + availW, margin + availH)
    } else {
        val idealAspect = 3f
        var cW = availW
        var cH = cW / idealAspect
        if (cH > availH) {
            cH = availH
            cW = cH * idealAspect
        }
        val leftX = (w - cW) / 2f
        val topY = (h - cH) / 2f
        RectF(leftX, topY, leftX + cW, topY + cH)
    }

    val cardW = cardRect.width()
    val cardH = cardRect.height()
    val cardCornerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(cardH / 2f)
    val uiScale = (cardH / (70f * scaleFactor)).coerceIn(0.55f, 2.2f)

    val cardClipPath = Path().apply {
        addRoundRect(cardRect, cardCornerRadius, cardCornerRadius, Path.Direction.CW)
    }

    canvas.save()
    canvas.clipPath(cardClipPath)

    // Full-Bleed Edge-to-Edge Art within Card Bounds
    if (artwork != null && !artwork.isRecycled) {
        val srcW = artwork.width.toFloat()
        val srcH = artwork.height.toFloat()
        val scale = maxOf(cardW / srcW, cardH / srcH)
        val dx = cardRect.left + (cardW - srcW * scale) / 2f
        val dy = cardRect.top + (cardH - srcH * scale) / 2f

        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(dx, dy)
        }
        val artPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(artwork, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(matrix)
            }
        }
        canvas.drawRect(cardRect, artPaint)
    } else {
        val fallbackBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(cardRect, fallbackBgPaint)

        val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(40, 255, 255, 255)
            textSize = cardH * 0.70f
            textAlign = Paint.Align.CENTER
            typeface = getSlateFont(context, 800)
        }
        canvas.drawText("♪", cardRect.centerX(), cardRect.centerY() + (cardH * 0.24f), notePaint)
    }

    // Scrim overlay
    val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            cardRect.left, 0f, cardRect.right, 0f,
            intArrayOf(
                android.graphics.Color.argb(235, 10, 10, 14),
                android.graphics.Color.argb(180, 10, 10, 14),
                android.graphics.Color.argb(110, 10, 10, 14),
                android.graphics.Color.argb(145, 10, 10, 14)
            ),
            floatArrayOf(0.0f, 0.44f, 0.68f, 1.0f),
            Shader.TileMode.CLAMP
        )
        style = Paint.Style.FILL
    }
    canvas.drawRect(cardRect, scrimPaint)

    // Border highlight
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(45, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.0f * scaleFactor
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, strokePaint)

    // Right Controls
    val colW = cardW * 0.40f
    val colLeft = cardRect.right - colW
    val slotW = colW / 3f
    val controlsCy = cardRect.centerY()

    val prevCx = colLeft + (slotW * 0.5f)
    val playCx = colLeft + (slotW * 1.5f)
    val nextCx = colLeft + (slotW * 2.5f)

    val playBtnR = minOf(cardH * 0.24f, slotW * 0.38f, scaleFactor * 22f * uiScale).coerceAtLeast(scaleFactor * 8f)
    val skipBtnR = playBtnR * 0.62f
    val skipBackplateR = skipBtnR * 1.30f

    val glassDiscPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(35, 255, 255, 255)
        style = Paint.Style.FILL
    }
    val glassDiscStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(45, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.0f * scaleFactor
    }

    // Prev Button
    canvas.drawCircle(prevCx, controlsCy, skipBackplateR, glassDiscPaint)
    canvas.drawCircle(prevCx, controlsCy, skipBackplateR, glassDiscStroke)
    drawSkipIcon(canvas, prevCx, controlsCy, skipBtnR, isNext = false, color = android.graphics.Color.WHITE)

    // Play/Pause Button
    val rR = android.graphics.Color.red(accentColor)
    val rG = android.graphics.Color.green(accentColor)
    val rB = android.graphics.Color.blue(accentColor)
    val playLum = (0.2126f * (rR / 255f)) + (0.7152f * (rG / 255f)) + (0.0722f * (rB / 255f))
    val playIconColor = if (playLum > 0.55f) android.graphics.Color.BLACK else android.graphics.Color.WHITE

    drawPlayPauseIcon(
        canvas,
        playCx,
        controlsCy,
        playBtnR,
        isPlaying = state.isPlaying,
        color = playIconColor,
        fillCircleBg = true,
        circleBgColor = accentColor
    )

    // Next Button
    canvas.drawCircle(nextCx, controlsCy, skipBackplateR, glassDiscPaint)
    canvas.drawCircle(nextCx, controlsCy, skipBackplateR, glassDiscStroke)
    drawSkipIcon(canvas, nextCx, controlsCy, skipBtnR, isNext = true, color = android.graphics.Color.WHITE)

    // Left Metadata
    val padLeft = (cardH * 0.16f).coerceIn(scaleFactor * 8f, scaleFactor * 24f)
    val textLeft = cardRect.left + padLeft
    val maxTextW = (colLeft - textLeft - (scaleFactor * 8f * uiScale)).coerceAtLeast(scaleFactor * 30f)

    val eqBarCount = 4
    val eqBarW = scaleFactor * 1.8f * uiScale
    val eqGap = scaleFactor * 2.0f * uiScale
    val eqStartX = textLeft
    val eqBaselineY = cardRect.top + (cardH * 0.32f)

    val eqPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }
    val eqHeights = if (state.isPlaying) floatArrayOf(0.6f, 1.0f, 0.45f, 0.85f) else floatArrayOf(0.2f, 0.2f, 0.2f, 0.2f)
    for (i in 0 until eqBarCount) {
        val bH = scaleFactor * 7.5f * uiScale * eqHeights[i]
        val bX = eqStartX + i * (eqBarW + eqGap)
        canvas.drawRoundRect(
            RectF(bX, eqBaselineY - bH, bX + eqBarW, eqBaselineY),
            eqBarW * 0.5f, eqBarW * 0.5f, eqPaint
        )
    }

    val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(175, 255, 255, 255)
        textSize = (scaleFactor * 6.5f * uiScale).coerceIn(scaleFactor * 5.5f, scaleFactor * 10f)
        typeface = getSlateFont(context, 700)
    }
    canvas.drawText("NOW PLAYING", eqStartX + (eqBarCount * (eqBarW + eqGap)) + (scaleFactor * 4f), eqBaselineY, tagPaint)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = (cardH * 0.20f).coerceIn(scaleFactor * 9f, scaleFactor * 16f)
        typeface = getSlateFont(context, 700)
    }
    val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(190, 255, 255, 255)
        textSize = (cardH * 0.14f).coerceIn(scaleFactor * 7f, scaleFactor * 13f)
        typeface = getSlateFont(context, 400)
    }

    val titleDisplay = if (titlePaint.measureText(state.title) > maxTextW) {
        var t = state.title
        while (t.isNotEmpty() && titlePaint.measureText("$t…") > maxTextW) t = t.dropLast(1)
        "$t…"
    } else state.title

    val artistDisplay = if (artistPaint.measureText(state.artist) > maxTextW) {
        var a = state.artist
        while (a.isNotEmpty() && artistPaint.measureText("$a…") > maxTextW) a = a.dropLast(1)
        "$a…"
    } else state.artist

    val titleY = eqBaselineY + (titlePaint.textSize * 1.12f)
    val artistY = titleY + (artistPaint.textSize * 1.25f)

    canvas.drawText(titleDisplay, textLeft, titleY, titlePaint)
    canvas.drawText(artistDisplay, textLeft, artistY, artistPaint)

    canvas.restore()

    return bitmap
}

// =========================================================================
// 5. RETRO CASSETTE TAPE (4x2 - FIXED RATIO)
// =========================================================================
fun generateCassetteTapeBitmap(
    context: Context,
    state: SlateMediaState,
    artwork: Bitmap?,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"
    val primaryTextColor = if (isLight) Color(0xFF141416).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF6C6C70).toArgb() else Color(0xFF8E8E93).toArgb()

    // 1. Fixed Aspect Ratio (1.65:1) Centered Tape Shell
    val idealAspect = 1.65f
    var cardW = w
    var cardH = cardW / idealAspect
    if (cardH > h) {
        cardH = h
        cardW = cardH * idealAspect
    }
    val leftX = (w - cardW) / 2f
    val topY = (h - cardH) / 2f
    val cassetteRect = RectF(leftX, topY, leftX + cardW, topY + cardH)

    val uiScale = (cardH / (130f * scaleFactor)).coerceIn(0.55f, 2.2f)
    val shellCornerR = scaleFactor * 12f * uiScale

    // 2. Cassette Shell (Theme bgColor)
    val shellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cassetteRect, shellCornerR, shellCornerR, shellPaint)

    // Outer Contour Ridge Stroke
    val shellBevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) android.graphics.Color.argb(35, 0, 0, 0) else android.graphics.Color.argb(45, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(cassetteRect, shellCornerR, shellCornerR, shellBevelPaint)

    // Side Grip Cutouts
    val gripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) android.graphics.Color.argb(30, 0, 0, 0) else android.graphics.Color.argb(35, 255, 255, 255)
        style = Paint.Style.FILL
    }
    val gripW = scaleFactor * 3.5f * uiScale
    val gripH = cardH * 0.26f
    val gripY = cassetteRect.centerY() - (gripH * 0.20f)
    canvas.drawRect(cassetteRect.left, gripY, cassetteRect.left + gripW, gripY + gripH, gripPaint)
    canvas.drawRect(cassetteRect.right - gripW, gripY, cassetteRect.right, gripY + gripH, gripPaint)

    // 4 Corner Screws (+)
    val screwHeadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) android.graphics.Color.argb(120, 0, 0, 0) else android.graphics.Color.argb(140, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.0f * scaleFactor
        strokeCap = Paint.Cap.ROUND
    }
    val screwHolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) android.graphics.Color.argb(40, 0, 0, 0) else android.graphics.Color.argb(60, 0, 0, 0)
        style = Paint.Style.FILL
    }
    val screwR = scaleFactor * 3.0f * uiScale
    val sPadX = scaleFactor * 8.0f * uiScale
    val sPadY = scaleFactor * 8.0f * uiScale
    val screwPositions = floatArrayOf(
        cassetteRect.left + sPadX, cassetteRect.top + sPadY,
        cassetteRect.right - sPadX, cassetteRect.top + sPadY,
        cassetteRect.left + sPadX, cassetteRect.bottom - sPadY,
        cassetteRect.right - sPadX, cassetteRect.bottom - sPadY
    )
    for (i in screwPositions.indices step 2) {
        val sx = screwPositions[i]
        val sy = screwPositions[i + 1]
        canvas.drawCircle(sx, sy, screwR, screwHolePaint)
        canvas.drawLine(sx - screwR * 0.55f, sy, sx + screwR * 0.55f, sy, screwHeadPaint)
        canvas.drawLine(sx, sy - screwR * 0.55f, sx, sy + screwR * 0.55f, screwHeadPaint)
    }

    // 3. Bottom Trapezoid Head Plate
    val trapWBottom = cardW * 0.74f
    val trapWTop = cardW * 0.62f
    val trapH = cardH * 0.22f
    val trapBottom = cassetteRect.bottom - (scaleFactor * 2.5f)
    val trapTop = trapBottom - trapH
    val trapLeftBottom = cassetteRect.centerX() - (trapWBottom / 2f)
    val trapRightBottom = cassetteRect.centerX() + (trapWBottom / 2f)
    val trapLeftTop = cassetteRect.centerX() - (trapWTop / 2f)
    val trapRightTop = cassetteRect.centerX() + (trapWTop / 2f)

    val trapPath = Path().apply {
        moveTo(trapLeftBottom, trapBottom)
        lineTo(trapLeftTop, trapTop)
        lineTo(trapRightTop, trapTop)
        lineTo(trapRightBottom, trapBottom)
        close()
    }
    val trapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) android.graphics.Color.argb(20, 0, 0, 0) else android.graphics.Color.argb(25, 255, 255, 255)
        style = Paint.Style.FILL
    }
    canvas.drawPath(trapPath, trapPaint)
    canvas.drawPath(trapPath, shellBevelPaint)

    // 4 Capstan & Guide Holes
    val holeY = trapTop + (trapH * 0.50f)
    val holeOuterR = scaleFactor * 3.6f * uiScale
    val holeInnerR = scaleFactor * 2.6f * uiScale
    val outerHoleDist = trapWBottom * 0.35f
    val innerHoleDist = trapWBottom * 0.15f
    val cX = cassetteRect.centerX()

    canvas.drawCircle(cX - outerHoleDist, holeY, holeOuterR, screwHolePaint)
    canvas.drawCircle(cX + outerHoleDist, holeY, holeOuterR, screwHolePaint)
    canvas.drawCircle(cX - innerHoleDist, holeY, holeInnerR, screwHolePaint)
    canvas.drawCircle(cX + innerHoleDist, holeY, holeInnerR, screwHolePaint)

    // 4. Adhesive Label Sticker
    val stickerMarginX = cardW * 0.07f
    val stickerTop = cassetteRect.top + (scaleFactor * 7.5f * uiScale)
    val stickerBottom = cassetteRect.top + (cardH * 0.65f)
    val stickerRect = RectF(cassetteRect.left + stickerMarginX, stickerTop, cassetteRect.right - stickerMarginX, stickerBottom)
    val stickerCornerR = scaleFactor * 7f * uiScale

    val stickerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) android.graphics.Color.argb(245, 255, 255, 255) else android.graphics.Color.argb(26, 255, 255, 255)
        style = Paint.Style.FILL
    }
    val stickerStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) android.graphics.Color.argb(40, 0, 0, 0) else android.graphics.Color.argb(40, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.0f * scaleFactor
    }
    canvas.drawRoundRect(stickerRect, stickerCornerR, stickerCornerR, stickerBgPaint)
    canvas.drawRoundRect(stickerRect, stickerCornerR, stickerCornerR, stickerStrokePaint)

    // Dynamic Duration Header (Actual Song Time)
    val durationText = if (state.durationMs > 0L) formatTime(state.durationMs) else "--:--"
    val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = scaleFactor * 6.5f * uiScale
        typeface = getSlateFont(context, 700)
    }
    canvas.drawText("SIDE A  •  $durationText", stickerRect.left + (scaleFactor * 10f * uiScale), stickerTop + (scaleFactor * 11f * uiScale), tagPaint)

    val hifiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = scaleFactor * 6.0f * uiScale
        typeface = getSlateFont(context, 400)
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText("TYPE I  HIGH BIAS", stickerRect.right - (scaleFactor * 10f * uiScale), stickerTop + (scaleFactor * 11f * uiScale), hifiPaint)

    // Centered Track Title
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = scaleFactor * 11.5f * uiScale
        typeface = getSlateFont(context, 700)
        textAlign = Paint.Align.CENTER
    }
    val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = scaleFactor * 8.5f * uiScale
        typeface = getSlateFont(context, 400)
        textAlign = Paint.Align.CENTER
    }

    val maxTitleW = stickerRect.width() - (scaleFactor * 24f * uiScale)
    val titleDisplay = if (titlePaint.measureText(state.title) > maxTitleW) {
        var t = state.title
        while (t.isNotEmpty() && titlePaint.measureText("$t…") > maxTitleW) t = t.dropLast(1)
        "$t…"
    } else state.title

    val artistDisplay = if (artistPaint.measureText(state.artist) > maxTitleW) {
        var a = state.artist
        while (a.isNotEmpty() && artistPaint.measureText("$a…") > maxTitleW) a = a.dropLast(1)
        "$a…"
    } else state.artist

    val titleY = stickerTop + (cardH * 0.22f)
    canvas.drawText(titleDisplay, stickerRect.centerX(), titleY, titlePaint)

    // Horizontal Accent Stripe (Full Sticker Width)
    val stripeY = titleY + (scaleFactor * 5f * uiScale)
    val stripeH = scaleFactor * 3.0f * uiScale
    val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }
    canvas.drawRect(stickerRect.left, stripeY, stickerRect.right, stripeY + stripeH, stripePaint)

    // Centered Artist
    val artistY = stripeY + stripeH + (scaleFactor * 11f * uiScale)
    canvas.drawText(artistDisplay, stickerRect.centerX(), artistY, artistPaint)

    // 5. Central Magnetic Tape Window
    val winW = cardW * 0.62f
    val winH = cardH * 0.35f
    val winLeft = cassetteRect.centerX() - (winW / 2f)
    val winTop = cassetteRect.top + (cardH * 0.49f)
    val winRect = RectF(winLeft, winTop, winLeft + winW, winTop + winH)
    val winCornerR = scaleFactor * 8f * uiScale

    val winBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) android.graphics.Color.argb(240, 228, 228, 232) else android.graphics.Color.argb(235, 14, 14, 18)
        style = Paint.Style.FILL
    }
    val winStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) android.graphics.Color.argb(45, 0, 0, 0) else android.graphics.Color.argb(45, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.0f * scaleFactor
    }
    canvas.drawRoundRect(winRect, winCornerR, winCornerR, winBgPaint)
    canvas.drawRoundRect(winRect, winCornerR, winCornerR, winStrokePaint)

    // 6. Magnetic Tape Spools & Play Button
    val spoolCenterY = winRect.centerY()
    val spoolSpacing = winW * 0.31f
    val leftSpoolX = winRect.centerX() - spoolSpacing
    val rightSpoolX = winRect.centerX() + spoolSpacing

    val minSpoolHubR = winH * 0.33f
    val maxTapePackR = winH * 0.45f

    val progress = state.progress.coerceIn(0f, 1f)
    val leftTapeR = minSpoolHubR + ((maxTapePackR - minSpoolHubR) * (1.0f - progress))
    val rightTapeR = minSpoolHubR + ((maxTapePackR - minSpoolHubR) * progress)

    val tapePackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) android.graphics.Color.argb(190, 70, 50, 45) else android.graphics.Color.argb(240, 32, 25, 22)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(leftSpoolX, spoolCenterY, leftTapeR, tapePackPaint)
    canvas.drawCircle(rightSpoolX, spoolCenterY, rightTapeR, tapePackPaint)

    val spoolRotation = if (state.isPlaying && state.positionMs > 0L) {
        ((state.positionMs / 1000f) * 60f) % 360f
    } else 0f

    val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) android.graphics.Color.WHITE else android.graphics.Color.argb(235, 38, 38, 44)
        style = Paint.Style.FILL
    }
    val cogToothPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }

    // Left Spool (Prev)
    canvas.drawCircle(leftSpoolX, spoolCenterY, minSpoolHubR, hubPaint)
    canvas.save()
    canvas.rotate(spoolRotation, leftSpoolX, spoolCenterY)
    for (deg in 0 until 360 step 60) {
        val rad = Math.toRadians(deg.toDouble()).toFloat()
        val toothX = leftSpoolX + kotlin.math.cos(rad) * (minSpoolHubR * 0.68f)
        val toothY = spoolCenterY + kotlin.math.sin(rad) * (minSpoolHubR * 0.68f)
        canvas.drawCircle(toothX, toothY, scaleFactor * 1.8f * uiScale, cogToothPaint)
    }
    canvas.restore()
    drawSkipIcon(canvas, leftSpoolX, spoolCenterY, minSpoolHubR * 0.65f, isNext = false, color = primaryTextColor)

    // Right Spool (Next)
    canvas.drawCircle(rightSpoolX, spoolCenterY, minSpoolHubR, hubPaint)
    canvas.save()
    canvas.rotate(spoolRotation, rightSpoolX, spoolCenterY)
    for (deg in 0 until 360 step 60) {
        val rad = Math.toRadians(deg.toDouble()).toFloat()
        val toothX = rightSpoolX + kotlin.math.cos(rad) * (minSpoolHubR * 0.68f)
        val toothY = spoolCenterY + kotlin.math.sin(rad) * (minSpoolHubR * 0.68f)
        canvas.drawCircle(toothX, toothY, scaleFactor * 1.8f * uiScale, cogToothPaint)
    }
    canvas.restore()
    drawSkipIcon(canvas, rightSpoolX, spoolCenterY, minSpoolHubR * 0.65f, isNext = true, color = primaryTextColor)

    // Center Play / Pause Accent Disc
    val playBtnR = minSpoolHubR * 1.10f
    val rR = android.graphics.Color.red(accentColor)
    val rG = android.graphics.Color.green(accentColor)
    val rB = android.graphics.Color.blue(accentColor)
    val playLum = (0.2126f * (rR / 255f)) + (0.7152f * (rG / 255f)) + (0.0722f * (rB / 255f))
    val playIconColor = if (playLum > 0.55f) android.graphics.Color.BLACK else android.graphics.Color.WHITE

    drawPlayPauseIcon(
        canvas,
        winRect.centerX(),
        spoolCenterY,
        playBtnR,
        isPlaying = state.isPlaying,
        color = playIconColor,
        fillCircleBg = true,
        circleBgColor = accentColor
    )

    return bitmap
}

// =========================================================================
// 6. SPECTRUM SOUNDWAVE (2x2)
// =========================================================================
fun generateSpectrumBitmap(
    context: Context,
    state: SlateMediaState,
    artwork: Bitmap?,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"
    val primaryTextColor = if (isLight) Color(0xFF141416).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF6C6C70).toArgb() else Color(0xFF8E8E93).toArgb()

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = cardRect.width() * 0.08f

    // 1. Equalizer Spectrum Bars (Upper 45%)
    val barCount = 14
    val spectrumBottom = cardRect.top + cardRect.height() * 0.48f
    val spectrumMaxH = cardRect.height() * 0.32f
    val totalSpectrumW = cardRect.width() - pad * 2f
    val barSpacing = totalSpectrumW / barCount
    val barW = barSpacing * 0.55f

    val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }

    // Patterned heights representing a soundwave
    val wavePattern = floatArrayOf(0.35f, 0.65f, 0.95f, 0.50f, 0.80f, 1.00f, 0.70f, 0.90f, 0.60f, 0.85f, 0.45f, 0.75f, 0.55f, 0.30f)
    for (i in 0 until barCount) {
        val mult = if (state.isPlaying) wavePattern[i % wavePattern.size] else 0.15f
        val bH = (spectrumMaxH * mult).coerceAtLeast(3f * scaleFactor)
        val bX = cardRect.left + pad + i * barSpacing + (barSpacing - barW) / 2f
        canvas.drawRoundRect(
            RectF(bX, spectrumBottom - bH, bX + barW, spectrumBottom),
            barW * 0.5f, barW * 0.5f, barPaint
        )
    }

    // Baseline divider
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFFE5E5EA).toArgb() else Color(0x20FFFFFF).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawRect(cardRect.left + pad, spectrumBottom + 2f * scaleFactor, cardRect.right - pad, spectrumBottom + 4f * scaleFactor, linePaint)

    // 2. Track Title & Artist
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardRect.height() * 0.080f
        typeface = getSlateFont(context, 700)
    }
    val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = cardRect.height() * 0.055f
        typeface = getSlateFont(context, 400)
    }

    val maxTextW = cardRect.width() - pad * 2f
    val titleDisplay = if (titlePaint.measureText(state.title) > maxTextW) {
        var t = state.title
        while (t.isNotEmpty() && titlePaint.measureText("$t…") > maxTextW) t = t.dropLast(1)
        "$t…"
    } else state.title

    val artistDisplay = if (artistPaint.measureText(state.artist) > maxTextW) {
        var a = state.artist
        while (a.isNotEmpty() && artistPaint.measureText("$a…") > maxTextW) a = a.dropLast(1)
        "$a…"
    } else state.artist

    val textY1 = spectrumBottom + cardRect.height() * 0.13f
    canvas.drawText(titleDisplay, cardRect.left + pad, textY1, titlePaint)
    canvas.drawText(artistDisplay, cardRect.left + pad, textY1 + cardRect.height() * 0.075f, artistPaint)

    // 3. Playback Controls at Bottom
    val ctrlY = cardRect.bottom - pad * 1.5f
    val ctrlSpacing = cardRect.width() * 0.26f
    val btnR = cardRect.width() * 0.075f

    drawSkipIcon(canvas, cardRect.centerX() - ctrlSpacing, ctrlY, btnR, isNext = false, color = secondaryTextColor)
    drawPlayPauseIcon(
        canvas,
        cardRect.centerX(),
        ctrlY,
        btnR * 1.15f,
        isPlaying = state.isPlaying,
        color = if (isLight) Color.White.toArgb() else Color.Black.toArgb(),
        fillCircleBg = true,
        circleBgColor = accentColor
    )
    drawSkipIcon(canvas, cardRect.centerX() + ctrlSpacing, ctrlY, btnR, isNext = true, color = secondaryTextColor)

    return bitmap
}

// =========================================================================
// 7. EDITORIAL MEDIA CARD (2x2)
// =========================================================================
fun generateEditorialBitmap(
    context: Context,
    state: SlateMediaState,
    artwork: Bitmap?,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"
    val primaryTextColor = if (isLight) Color(0xFF141416).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF6C6C70).toArgb() else Color(0xFF8E8E93).toArgb()

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = cardRect.width() * 0.10f

    // 1. Editorial Header Tag
    val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = cardRect.height() * 0.052f
        typeface = getSlateFont(context, 700)
    }
    canvas.drawText("NOW PLAYING — SLATE", cardRect.left + pad, cardRect.top + pad * 1.4f, tagPaint)

    // 2. Large Stylized Title (Multiple lines or giant font)
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardRect.height() * 0.125f
        typeface = getSlateFont(context, 700)
    }
    val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = cardRect.height() * 0.065f
        typeface = getSlateFont(context, 400)
    }

    val maxTextW = cardRect.width() - pad * 2f
    val titleDisplay = if (titlePaint.measureText(state.title) > maxTextW) {
        var t = state.title
        while (t.isNotEmpty() && titlePaint.measureText("$t…") > maxTextW) t = t.dropLast(1)
        "$t…"
    } else state.title

    val titleY = cardRect.top + cardRect.height() * 0.38f
    canvas.drawText(titleDisplay, cardRect.left + pad, titleY, titlePaint)
    canvas.drawText(state.artist.take(30), cardRect.left + pad, titleY + cardRect.height() * 0.10f, artistPaint)

    // Hairline Divider
    val lineY = titleY + cardRect.height() * 0.18f
    val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFFD1D1D6).toArgb() else Color(0x25FFFFFF).toArgb()
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawLine(cardRect.left + pad, lineY, cardRect.right - pad, lineY, divPaint)

    // 3. Mini Progress Arc or Strip
    val progressW = (cardRect.width() - pad * 2f) * state.progress
    val progPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        strokeWidth = 2f * scaleFactor
    }
    canvas.drawLine(cardRect.left + pad, lineY, cardRect.left + pad + progressW, lineY, progPaint)

    // 4. Controls at bottom
    val ctrlY = cardRect.bottom - pad * 1.4f
    val btnR = cardRect.width() * 0.075f
    val ctrlSpacing = cardRect.width() * 0.26f

    drawSkipIcon(canvas, cardRect.centerX() - ctrlSpacing, ctrlY, btnR, isNext = false, color = secondaryTextColor)
    drawPlayPauseIcon(
        canvas,
        cardRect.centerX(),
        ctrlY,
        btnR * 1.15f,
        isPlaying = state.isPlaying,
        color = if (isLight) Color.White.toArgb() else Color.Black.toArgb(),
        fillCircleBg = true,
        circleBgColor = accentColor
    )
    drawSkipIcon(canvas, cardRect.centerX() + ctrlSpacing, ctrlY, btnR, isNext = true, color = secondaryTextColor)

    return bitmap
}

// =========================================================================
// 8. MEDIA STREAMING DOCK (4x1)
// =========================================================================
fun generateMediaDockBitmap(
    context: Context,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val aspect = 4f
        val cardW = minOf(w, h * aspect)
        val cardH = cardW / aspect
        RectF((w - cardW) / 2f, (h - cardH) / 2f, (w + cardW) / 2f, (h + cardH) / 2f)
    }

    val pillRadius = cardRect.height() / 2f
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, pillRadius, pillRadius, bgPaint)

    val slotCount = 5
    val slotW = cardRect.width() / slotCount
    val cy = cardRect.centerY()
    val iconR = cardRect.height() * 0.28f

    val appNames = listOf("Spotify", "YT Music", "Apple", "SoundCloud", "Shazam")
    val appLetters = listOf("S", "▶", "", "☁", "⚡")

    for (i in 0 until slotCount) {
        val cx = cardRect.left + i * slotW + slotW / 2f

        // Squircle / Circle App Background
        val iconBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (i == 0) accentColor else (if (isLight) Color(0xFFE5E5EA).toArgb() else Color(0xFF222226).toArgb())
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy - cardRect.height() * 0.06f, iconR, iconBgPaint)

        // Glyph
        val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (i == 0) (if (isLight) Color.White.toArgb() else Color.Black.toArgb()) else (if (isLight) Color(0xFF141416).toArgb() else Color.White.toArgb())
            textSize = iconR * 0.95f
            textAlign = Paint.Align.CENTER
            typeface = getSlateFont(context, 700)
        }
        canvas.drawText(appLetters[i], cx, cy - cardRect.height() * 0.06f + iconR * 0.35f, glyphPaint)

        // Label below
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color(0xFF6C6C70).toArgb() else Color(0xFF8E8E93).toArgb()
            textSize = cardRect.height() * 0.12f
            textAlign = Paint.Align.CENTER
            typeface = getSlateFont(context, 500)
        }
        canvas.drawText(appNames[i], cx, cardRect.bottom - cardRect.height() * 0.12f, labelPaint)
    }

    return bitmap
}
