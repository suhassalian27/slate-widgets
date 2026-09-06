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
// 4. MINI CAPSULE (2x1)
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
    val isLight = slateConfig.themeMode == "LIGHT"
    val primaryTextColor = if (isLight) Color(0xFF141416).toArgb() else Color.White.toArgb()
    val secondaryTextColor = if (isLight) Color(0xFF6C6C70).toArgb() else Color(0xFF8E8E93).toArgb()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val aspect = 2f
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

    val pad = cardRect.height() * 0.16f
    val artR = cardRect.height() / 2f - pad
    val artCx = cardRect.left + pillRadius
    val artCy = cardRect.centerY()

    if (artwork != null && !artwork.isRecycled) {
        drawCircularBitmap(canvas, artwork, artCx, artCy, artR)
    } else {
        val artBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(artCx, artCy, artR, artBgPaint)
        val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.White.toArgb() else Color.Black.toArgb()
            textSize = artR * 0.9f
            textAlign = Paint.Align.CENTER
            typeface = getSlateFont(context, 700)
        }
        canvas.drawText("♫", artCx, artCy + artR * 0.32f, glyphPaint)
    }

    // Play/Pause button on Right
    val btnR = cardRect.height() * 0.22f
    val playX = cardRect.right - pillRadius * 0.75f
    drawPlayPauseIcon(
        canvas,
        playX,
        artCy,
        btnR * 1.25f,
        isPlaying = state.isPlaying,
        color = if (isLight) Color.White.toArgb() else Color.Black.toArgb(),
        fillCircleBg = true,
        circleBgColor = accentColor
    )

    // Text in middle
    val textLeft = artCx + artR + pad * 1.2f
    val maxTextW = (playX - btnR * 1.5f - textLeft).coerceAtLeast(10f)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = cardRect.height() * 0.24f
        typeface = getSlateFont(context, 700)
    }
    val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = cardRect.height() * 0.17f
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

    val titleY = artCy - cardRect.height() * 0.04f
    canvas.drawText(titleDisplay, textLeft, titleY, titlePaint)
    canvas.drawText(artistDisplay, textLeft, titleY + cardRect.height() * 0.25f, artistPaint)

    return bitmap
}

// =========================================================================
// 5. RETRO CASSETTE TAPE (4x2)
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

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val aspect = 2f
        val cardW = minOf(w, h * aspect)
        val cardH = cardW / aspect
        RectF((w - cardW) / 2f, (h - cardH) / 2f, (w + cardW) / 2f, (h + cardH) / 2f)
    }

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = cardRect.height() * 0.09f

    // 1. Inner Cassette Body
    val cassetteRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.bottom - pad)
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFFECECEF).toArgb() else Color(0xFF18181B).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cassetteRect, cardCornerRadius * 0.7f, cardCornerRadius * 0.7f, bodyPaint)

    // Corner Screws
    val screwPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFFB0B0B5).toArgb() else Color(0xFF333338).toArgb()
        style = Paint.Style.FILL
    }
    val screwR = 3.5f * scaleFactor
    val sPad = pad * 1.5f
    canvas.drawCircle(cassetteRect.left + sPad, cassetteRect.top + sPad, screwR, screwPaint)
    canvas.drawCircle(cassetteRect.right - sPad, cassetteRect.top + sPad, screwR, screwPaint)
    canvas.drawCircle(cassetteRect.left + sPad, cassetteRect.bottom - sPad, screwR, screwPaint)
    canvas.drawCircle(cassetteRect.right - sPad, cassetteRect.bottom - sPad, screwR, screwPaint)

    // 2. Center Cassette Sticker Label
    val stickerRect = RectF(
        cassetteRect.left + pad * 1.2f,
        cassetteRect.top + pad * 1.1f,
        cassetteRect.right - pad * 1.2f,
        cassetteRect.top + cassetteRect.height() * 0.58f
    )
    val stickerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFFFFFFFF).toArgb() else Color(0xFF222227).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(stickerRect, 8f * scaleFactor, 8f * scaleFactor, stickerPaint)

    // Accent line on sticker
    val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }
    canvas.drawRect(stickerRect.left, stickerRect.top + stickerRect.height() * 0.42f, stickerRect.right, stickerRect.top + stickerRect.height() * 0.46f, stripePaint)

    // SIDE A badge
    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = stickerRect.height() * 0.22f
        typeface = getSlateFont(context, 700)
    }
    canvas.drawText("SIDE A", stickerRect.left + pad * 0.8f, stickerRect.top + stickerRect.height() * 0.32f, badgePaint)

    // Track Title on Sticker
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = stickerRect.height() * 0.24f
        typeface = getSlateFont(context, 700)
    }
    val titleDisplay = state.title.take(28)
    canvas.drawText(titleDisplay, stickerRect.left + pad * 0.8f, stickerRect.bottom - pad * 0.6f, titlePaint)

    // 3. Central Magnetic Tape Spools Window
    val windowRect = RectF(
        cassetteRect.left + cassetteRect.width() * 0.22f,
        cassetteRect.top + cassetteRect.height() * 0.62f,
        cassetteRect.right - cassetteRect.width() * 0.22f,
        cassetteRect.bottom - pad * 1.2f
    )
    val winPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFFDCDCE0).toArgb() else Color(0xFF0D0D10).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(windowRect, 6f * scaleFactor, 6f * scaleFactor, winPaint)

    // Spools (Left and Right)
    val spoolR = windowRect.height() * 0.38f
    val leftSpoolX = windowRect.left + windowRect.width() * 0.26f
    val rightSpoolX = windowRect.right - windowRect.width() * 0.26f
    val spoolY = windowRect.centerY()

    val spoolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.White.toArgb() else Color(0xFF26262C).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(leftSpoolX, spoolY, spoolR, spoolPaint)
    canvas.drawCircle(rightSpoolX, spoolY, spoolR, spoolPaint)

    // Spool Cog Teeth
    val cogPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }
    for (ang in 0 until 360 step 60) {
        val rad = Math.toRadians(ang.toDouble()).toFloat()
        val cxL = leftSpoolX + cos(rad) * (spoolR * 0.6f)
        val cyL = spoolY + sin(rad) * (spoolR * 0.6f)
        canvas.drawCircle(cxL, cyL, 2.5f * scaleFactor, cogPaint)

        val cxR = rightSpoolX + cos(rad) * (spoolR * 0.6f)
        val cyR = spoolY + sin(rad) * (spoolR * 0.6f)
        canvas.drawCircle(cxR, cyR, 2.5f * scaleFactor, cogPaint)
    }

    // Play/Pause icon in center of window
    drawPlayPauseIcon(
        canvas,
        windowRect.centerX(),
        spoolY,
        spoolR * 0.75f,
        isPlaying = state.isPlaying,
        color = if (isLight) Color.White.toArgb() else Color.Black.toArgb(),
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
