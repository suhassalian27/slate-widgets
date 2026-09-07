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

    val btnR = scaleFactor * 11.5f * uiScale
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

    // 1. Text & Vinyl Stage Anchors (Vinyl position untouched)
    val baseAnchorY = cardRect.bottom - (scaleFactor * 16f * uiScale)
    val artistY = baseAnchorY - (btnR * 1.30f) - (scaleFactor * 5f * uiScale)
    val titleY = artistY - artistPaint.textSize - (scaleFactor * 3.5f * uiScale)

    // 2. Horizontally Centered & Sized Vinyl Stage
    val availableVinylTop = cardRect.top + (scaleFactor * 8f * uiScale)
    val availableVinylBottom = titleY - titlePaint.textSize - (scaleFactor * 6f * uiScale)
    val vinylCenterY = (availableVinylTop + availableVinylBottom) / 2f
    val vinylCenterX = cardRect.centerX()

    val maxRadiusByH = (availableVinylBottom - availableVinylTop) / 2f
    val maxRadiusByW = (contentW / 2f) - (pad * 0.7f)
    val vinylRadius = minOf(maxRadiusByH, maxRadiusByW).coerceAtLeast(scaleFactor * 26f)

    // 3. Rotating Grooves
    val vinylRotation = if (state.isPlaying && state.positionMs > 0L) {
        ((state.positionMs / 1000f) * 45f) % 360f
    } else {
        0f
    }

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

    // 4. Center Vinyl Label
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

    // 5. Boundary-Safe Tonearm Assembly
    val baseFlangeR = vinylRadius * 0.18f
    val rearStubLen = vinylRadius * 0.24f
    val armLength = vinylRadius * 0.98f

    // Bounds safety clamping: prevents tonearm elements from clipping outside cardRect
    val minPivotY = cardRect.top + rearStubLen + (scaleFactor * 6f * uiScale)
    val maxPivotY = vinylCenterY - (vinylRadius * 0.35f)
    val armPivotY = (vinylCenterY - vinylRadius * 0.70f).coerceIn(minPivotY, maxPivotY)

    val maxPivotX = cardRect.right - baseFlangeR - (scaleFactor * 4f * uiScale)
    val targetPivotX = vinylCenterX + (vinylRadius * 1.20f)
    val armPivotX = targetPivotX.coerceAtMost(maxPivotX)
    val squeezeOffset = (targetPivotX - armPivotX).coerceAtLeast(0f)

    // Tone-arm Base Flange
    val baseFlangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFFD8D8DC).toArgb() else Color(0xFF1B1B1E).toArgb()
        style = Paint.Style.FILL
    }
    val baseBevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) android.graphics.Color.argb(50, 0, 0, 0) else android.graphics.Color.argb(60, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawCircle(armPivotX, armPivotY, baseFlangeR, baseFlangePaint)
    canvas.drawCircle(armPivotX, armPivotY, baseFlangeR, baseBevelPaint)

    val biasDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF9E9EA4).toArgb() else Color(0xFF38383D).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(armPivotX + baseFlangeR * 0.50f, armPivotY + baseFlangeR * 0.35f, scaleFactor * 2.0f * uiScale, biasDotPaint)

    // Rest Cradle Post (Aligned to resting wand position)
    val progress = state.progress.coerceIn(0f, 1f)
    val restAngle = -9.5f - (squeezeOffset / (vinylRadius + 1f)) * 18f
    val playAngle = 14.5f + (8.0f * progress) - (squeezeOffset / (vinylRadius + 1f)) * 10f
    val armAngle = if (state.isPlaying) playAngle else restAngle

    val restRad = Math.toRadians(restAngle.toDouble()).toFloat()
    val restPostDist = armLength * 0.46f
    val restX = armPivotX + kotlin.math.sin(restRad) * restPostDist
    val restY = armPivotY + kotlin.math.cos(restRad) * restPostDist

    val restPostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF8E8E93).toArgb() else Color(0xFF333338).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(
        RectF(restX - scaleFactor * 2.5f * uiScale, restY - scaleFactor * 1.5f * uiScale, restX + scaleFactor * 5.0f * uiScale, restY + scaleFactor * 3.5f * uiScale),
        scaleFactor * 1.0f, scaleFactor * 1.0f, restPostPaint
    )

    // Dynamic Tonearm Structure
    canvas.save()
    canvas.rotate(armAngle, armPivotX, armPivotY)

    // A. Rear Stub & Counterweight
    val stubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFFA5A5AA).toArgb() else Color(0xFF8A8A90).toArgb()
        style = Paint.Style.FILL
    }
    val stubW = scaleFactor * 2.6f * uiScale
    canvas.drawRect(armPivotX - stubW / 2f, armPivotY - rearStubLen, armPivotX + stubW / 2f, armPivotY, stubPaint)

    val cwW = vinylRadius * 0.15f
    val cwH = vinylRadius * 0.13f
    val cwTop = armPivotY - (rearStubLen * 0.90f)
    val cwRect = RectF(armPivotX - cwW / 2f, cwTop, armPivotX + cwW / 2f, cwTop + cwH)
    val cwPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF4A4A4F).toArgb() else Color(0xFF2B2B30).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cwRect, scaleFactor * 2f * uiScale, scaleFactor * 2f * uiScale, cwPaint)

    val cwRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }
    canvas.drawRect(armPivotX - cwW / 2f, cwTop + cwH * 0.65f, armPivotX + cwW / 2f, cwTop + cwH * 0.85f, cwRingPaint)

    // B. Gimbal Ring & Pivot Housing
    val gimbalR = vinylRadius * 0.10f
    val gimbalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFFE5E5EA).toArgb() else Color(0xFFC0C0C6).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(armPivotX, armPivotY, gimbalR, gimbalPaint)

    val centerPinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF6C6C70).toArgb() else Color(0xFF222226).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(armPivotX, armPivotY, gimbalR * 0.35f, centerPinPaint)

    val leverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.White.toArgb() else Color(0xFFE5E5EA).toArgb()
        style = Paint.Style.FILL
    }
    canvas.save()
    canvas.rotate(-45f, armPivotX, armPivotY)
    canvas.drawRoundRect(
        RectF(armPivotX - gimbalR * 1.4f, armPivotY - scaleFactor * 2.0f * uiScale, armPivotX - gimbalR * 0.4f, armPivotY + scaleFactor * 2.0f * uiScale),
        scaleFactor * 1.5f, scaleFactor * 1.5f, leverPaint
    )
    canvas.restore()

    // C. Aluminum J-Bend Wand (Straight from pivot to cradle, with a subtle bend near tip)
    val bendStartY = armPivotY + (armLength * 0.72f)
    val bendXOffset = vinylRadius * 0.055f
    val wandTipX = armPivotX - bendXOffset
    val wandTipY = armPivotY + armLength

    val armWandPath = Path().apply {
        moveTo(armPivotX, armPivotY)
        lineTo(armPivotX, bendStartY)
        quadTo(armPivotX, bendStartY + (armLength * 0.14f), wandTipX, wandTipY)
    }

    val wandStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF6A6A70).toArgb() else Color(0xFFD4D4DA).toArgb()
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 2.2f * uiScale
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    canvas.drawPath(armWandPath, wandStrokePaint)

    // D. Headshell & Cartridge Assembly
    canvas.save()
    canvas.translate(wandTipX, wandTipY)
    canvas.rotate(16f)

    val hsW = scaleFactor * 7.5f * uiScale
    val hsH = scaleFactor * 14.5f * uiScale
    val hsRect = RectF(-hsW / 2f, 0f, hsW / 2f, hsH)
    val hsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF2C2C30).toArgb() else Color(0xFF1E1E22).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(hsRect, scaleFactor * 1.8f * uiScale, scaleFactor * 1.8f * uiScale, hsPaint)

    val fingerLiftPath = Path().apply {
        moveTo(hsW / 2f, hsH * 0.35f)
        quadTo(hsW / 2f + scaleFactor * 4.5f * uiScale, hsH * 0.25f, hsW / 2f + scaleFactor * 4.5f * uiScale, hsH * 0.05f)
    }
    val fingerLiftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFFA1A1A6).toArgb() else Color(0xFFC0C0C6).toArgb()
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1.2f * uiScale
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawPath(fingerLiftPath, fingerLiftPaint)

    val cartW = hsW * 0.70f
    val cartH = scaleFactor * 4.0f * uiScale
    val cartRect = RectF(-cartW / 2f, hsH - scaleFactor * 1.5f * uiScale, cartW / 2f, hsH + cartH)
    val cartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cartRect, scaleFactor * 1.0f * uiScale, scaleFactor * 1.0f * uiScale, cartPaint)

    canvas.restore()
    canvas.restore()

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

    // 7. Elevated Controls Deck
    val ctrlY = cardRect.bottom - (scaleFactor * 21.5f * uiScale)

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

    // 1. Equalizer Spectrum Bars (Shifted slightly upward)
    val barCount = 14
    val spectrumBottom = cardRect.top + cardRect.height() * 0.44f
    val spectrumMaxH = cardRect.height() * 0.28f
    val totalSpectrumW = cardRect.width() - pad * 2f
    val barSpacing = totalSpectrumW / barCount
    val barW = barSpacing * 0.55f

    val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }

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
        textSize = cardRect.height() * 0.078f
        typeface = getSlateFont(context, 700)
    }
    val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = cardRect.height() * 0.054f
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

    val textY1 = spectrumBottom + cardRect.height() * 0.115f
    val textY2 = textY1 + cardRect.height() * 0.070f
    canvas.drawText(titleDisplay, cardRect.left + pad, textY1, titlePaint)
    canvas.drawText(artistDisplay, cardRect.left + pad, textY2, artistPaint)

    // 3. Playback Controls (Elevated with ample bottom breathing room)
    val ctrlY = cardRect.bottom - (cardRect.height() * 0.185f)
    val ctrlSpacing = cardRect.width() * 0.26f
    val btnR = cardRect.width() * 0.074f

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
// 7. EDITORIAL MEDIA CARD (2x2 - RESPONSIVE & FIXED)
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

    // 1. Smart Aspect Ratio Engine (Prevents extreme aspect ratio distortion)
    val cardRect = if (isResponsive) {
        val minAspect = 0.78f
        val maxAspect = 1.50f
        var cW = w
        var cH = h
        if (cW / cH > maxAspect) {
            cW = cH * maxAspect
        } else if (cW / cH < minAspect) {
            cH = cW / minAspect
        }
        val leftX = (w - cW) / 2f
        val topY = (h - cH) / 2f
        RectF(leftX, topY, leftX + cW, topY + cH)
    } else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val cardW = cardRect.width()
    val cardH = cardRect.height()
    val cardCornerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(cardH / 2f)
    val effectiveDim = minOf(cardW, cardH)
    val uiScale = (effectiveDim / (160f * scaleFactor)).coerceIn(0.55f, 2.2f)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val padX = cardW * 0.10f
    val padY = cardH * 0.10f
    val textStartX = cardRect.left + padX
    val maxTextW = cardW - (padX * 2f)

    // 2. Playback Controls Deck (Elevated Bottom-Up Positioning)
    val ctrlY = cardRect.bottom - (cardH * 0.22f)
    val btnR = scaleFactor * 12.0f * uiScale
    val playBtnR = btnR * 1.25f
    val ctrlSpacing = minOf(cardW * 0.26f, scaleFactor * 44f * uiScale)

    val rR = android.graphics.Color.red(accentColor)
    val rG = android.graphics.Color.green(accentColor)
    val rB = android.graphics.Color.blue(accentColor)
    val playLum = (0.2126f * (rR / 255f)) + (0.7152f * (rG / 255f)) + (0.0722f * (rB / 255f))
    val playIconColor = if (playLum > 0.55f) android.graphics.Color.BLACK else android.graphics.Color.WHITE

    drawSkipIcon(canvas, cardRect.centerX() - ctrlSpacing, ctrlY, btnR, isNext = false, color = secondaryTextColor)
    drawPlayPauseIcon(
        canvas,
        cardRect.centerX(),
        ctrlY,
        playBtnR,
        isPlaying = state.isPlaying,
        color = playIconColor,
        fillCircleBg = true,
        circleBgColor = accentColor
    )
    drawSkipIcon(canvas, cardRect.centerX() + ctrlSpacing, ctrlY, btnR, isNext = true, color = secondaryTextColor)

    // 3. Editorial Tag
    val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        textSize = (scaleFactor * 7.5f * uiScale).coerceIn(scaleFactor * 6f, scaleFactor * 12f)
        typeface = getSlateFont(context, 700)
    }
    val tagY = cardRect.top + padY + tagPaint.textSize
    canvas.drawText("NOW PLAYING — SLATE", textStartX, tagY, tagPaint)

    // 4. Stylized Headline & Subtitle (Evenly distributed in upper section)
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = (cardH * 0.12f).coerceIn(scaleFactor * 11f, scaleFactor * 24f)
        typeface = getSlateFont(context, 700)
    }
    val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = (cardH * 0.072f).coerceIn(scaleFactor * 8.5f, scaleFactor * 16f)
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

    val titleY = tagY + (cardH * 0.16f)
    val artistY = titleY + (artistPaint.textSize * 1.35f)

    canvas.drawText(titleDisplay, textStartX, titleY, titlePaint)
    canvas.drawText(artistDisplay, textStartX, artistY, artistPaint)

    return bitmap
}

// =========================================================================
// 8. VINYL DISC PLAYER (2x2 - FIXED RATIO)
// =========================================================================
fun generateVinylDiscBitmap(
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

    // 1. Strict 1:1 Fixed Aspect Ratio (Centered Disc)
    val size = minOf(w, h)
    val cx = w / 2f
    val cy = h / 2f
    val vinylRadius = (size / 2f) - (scaleFactor * 3.5f)

    // 2. Vinyl Body (Theme Background Color)
    val vinylBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, vinylRadius, vinylBodyPaint)

    // Outer Rim Bevel
    val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) android.graphics.Color.argb(35, 0, 0, 0) else android.graphics.Color.argb(45, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawCircle(cx, cy, vinylRadius, rimPaint)

    val labelRadius = vinylRadius * 0.44f

    // 3. Rotating Grooves (Grooves spin beneath stationary ambient light)
    val vinylRotation = if (state.isPlaying && state.positionMs > 0L) {
        ((state.positionMs / 1000f) * 45f) % 360f
    } else 0f

    canvas.save()
    canvas.rotate(vinylRotation, cx, cy)

    val groovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) android.graphics.Color.argb(26, 0, 0, 0) else android.graphics.Color.argb(24, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.0f * scaleFactor
    }
    for (i in 1..7) {
        val r = labelRadius + (vinylRadius - labelRadius) * (i / 8f)
        canvas.drawCircle(cx, cy, r, groovePaint)
    }
    canvas.restore()

    // 4. Stationary Specular Highlights (Permanently centered over Prev and Next)
    val discBounds = RectF(cx - vinylRadius, cy - vinylRadius, cx + vinylRadius, cy + vinylRadius)
    val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) android.graphics.Color.argb(18, 255, 255, 255) else android.graphics.Color.argb(16, 255, 255, 255)
        style = Paint.Style.FILL
    }
    // Left highlight over Prev touch zone (centered at 180°)
    canvas.drawArc(discBounds, 160f, 40f, true, sheenPaint)
    // Right highlight over Next touch zone (centered at 0°)
    canvas.drawArc(discBounds, -20f, 40f, true, sheenPaint)

    // 5. Center Label: Full Circular Album Cover
    val labelRect = RectF(cx - labelRadius, cy - labelRadius, cx + labelRadius, cy + labelRadius)
    if (artwork != null && !artwork.isRecycled) {
        canvas.save()
        val labelPath = Path().apply { addCircle(cx, cy, labelRadius, Path.Direction.CW) }
        canvas.clipPath(labelPath)

        val srcW = artwork.width.toFloat()
        val srcH = artwork.height.toFloat()
        val scale = maxOf((labelRadius * 2f) / srcW, (labelRadius * 2f) / srcH)
        val dx = cx - (srcW * scale) / 2f
        val dy = cy - (srcH * scale) / 2f

        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(dx, dy)
        }
        val artPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(artwork, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(matrix)
            }
        }
        canvas.drawRect(labelRect, artPaint)
        canvas.restore()
    } else {
        val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color(0xFFE5E5EA).toArgb() else Color(0xFF202024).toArgb()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, labelRadius, labelBgPaint)
        val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
            textSize = labelRadius * 0.70f
            textAlign = Paint.Align.CENTER
            typeface = getSlateFont(context, 700)
        }
        canvas.drawText("♪", cx, cy + labelRadius * 0.26f, notePaint)
    }

    // Label Rim Border
    val labelRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(80, 0, 0, 0)
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * scaleFactor
    }
    canvas.drawCircle(cx, cy, labelRadius, labelRimPaint)

    // 6. Ultra-Faint Micro-Chevrons (Debossed into the wax inside the sheen zones)
    val trackDist = vinylRadius * 0.74f
    val prevCx = cx - trackDist
    val nextCx = cx + trackDist

    val cW = vinylRadius * 0.044f
    val cH = vinylRadius * 0.066f

    // Deboss groove shadow (creates tactile stamped depth)
    val shadowEtchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) android.graphics.Color.argb(18, 255, 255, 255) else android.graphics.Color.argb(32, 0, 0, 0)
        style = Paint.Style.STROKE
        strokeWidth = 1.0f * scaleFactor
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Ultra-faint rim stroke (~9% opacity)
    val microEtchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) android.graphics.Color.argb(24, 0, 0, 0) else android.graphics.Color.argb(24, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.0f * scaleFactor
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Left Micro-Chevron (<)
    val leftPath = Path().apply {
        moveTo(prevCx + cW, cy - cH)
        lineTo(prevCx - cW, cy)
        lineTo(prevCx + cW, cy + cH)
    }
    canvas.save()
    canvas.translate(0f, 0.6f * scaleFactor)
    canvas.drawPath(leftPath, shadowEtchPaint)
    canvas.restore()
    canvas.drawPath(leftPath, microEtchPaint)

    // Right Micro-Chevron (>)
    val rightPath = Path().apply {
        moveTo(nextCx - cW, cy - cH)
        lineTo(nextCx + cW, cy)
        lineTo(nextCx - cW, cy + cH)
    }
    canvas.save()
    canvas.translate(0f, 0.6f * scaleFactor)
    canvas.drawPath(rightPath, shadowEtchPaint)
    canvas.restore()
    canvas.drawPath(rightPath, microEtchPaint)

    // 7. Compact Center Stabilizer Puck (Play / Pause)
    val playBtnR = vinylRadius * 0.155f

    val puckShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(90, 0, 0, 0)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * scaleFactor
    }
    canvas.drawCircle(cx, cy, playBtnR + 0.8f * scaleFactor, puckShadowPaint)

    val rR = android.graphics.Color.red(accentColor)
    val rG = android.graphics.Color.green(accentColor)
    val rB = android.graphics.Color.blue(accentColor)
    val playLum = (0.2126f * (rR / 255f)) + (0.7152f * (rG / 255f)) + (0.0722f * (rB / 255f))
    val playIconColor = if (playLum > 0.55f) android.graphics.Color.BLACK else android.graphics.Color.WHITE

    drawPlayPauseIcon(
        canvas,
        cx,
        cy,
        playBtnR,
        isPlaying = state.isPlaying,
        color = playIconColor,
        fillCircleBg = true,
        circleBgColor = accentColor
    )

    return bitmap
}

// =========================================================================
// 9. CORNER TURNTABLE DECK (2x2 - FIXED RATIO)
// =========================================================================
fun generateTurntableDeckBitmap(
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

    // 1. Strict 1:1 Fixed Square Geometry
    val size = minOf(w, h)
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)
    val cardCornerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(size / 2f)
    val uiScale = (size / (160f * scaleFactor)).coerceIn(0.5f, 2.5f)

    // Base Deck Plate
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val cardClipPath = Path().apply {
        addRoundRect(cardRect, cardCornerRadius, cardCornerRadius, Path.Direction.CW)
    }
    canvas.save()
    canvas.clipPath(cardClipPath)

    // 2. Top-Left Offset Vinyl Platter (Position kept intact)
    val spindleX = cardRect.left + (size * 0.28f)
    val spindleY = cardRect.top + (size * 0.26f)
    val platterR = size * 0.52f
    val vinylR = platterR * 0.94f
    val labelR = vinylR * 0.40f

    // Turntable Platter Rim
    val platterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFFD8D8DC).toArgb() else Color(0xFF222226).toArgb()
        style = Paint.Style.FILL
    }
    val platterStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) android.graphics.Color.argb(35, 0, 0, 0) else android.graphics.Color.argb(45, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawCircle(spindleX, spindleY, platterR, platterPaint)
    canvas.drawCircle(spindleX, spindleY, platterR, platterStroke)

    // Rotating Vinyl Disc & Grooves
    val vinylRotation = if (state.isPlaying && state.positionMs > 0L) {
        ((state.positionMs / 1000f) * 45f) % 360f
    } else 0f

    canvas.save()
    canvas.rotate(vinylRotation, spindleX, spindleY)

    val vinylPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF1E1E22).toArgb() else Color(0xFF101012).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(spindleX, spindleY, vinylR, vinylPaint)

    val groovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) android.graphics.Color.argb(22, 255, 255, 255) else android.graphics.Color.argb(18, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.0f * scaleFactor
    }
    for (i in 1..5) {
        val r = labelR + (vinylR - labelR) * (i / 6f)
        canvas.drawCircle(spindleX, spindleY, r, groovePaint)
    }
    canvas.restore()

    // Circular Album Art Label (Straight / Unrotated)
    val labelRect = RectF(spindleX - labelR, spindleY - labelR, spindleX + labelR, spindleY + labelR)
    if (artwork != null && !artwork.isRecycled) {
        canvas.save()
        val labelPath = Path().apply { addCircle(spindleX, spindleY, labelR, Path.Direction.CW) }
        canvas.clipPath(labelPath)

        val srcW = artwork.width.toFloat()
        val srcH = artwork.height.toFloat()
        val scale = maxOf((labelR * 2f) / srcW, (labelR * 2f) / srcH)
        val dx = spindleX - (srcW * scale) / 2f
        val dy = spindleY - (srcH * scale) / 2f

        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(dx, dy)
        }
        val artPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(artwork, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(matrix)
            }
        }
        canvas.drawRect(labelRect, artPaint)
        canvas.restore()
    } else {
        val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color(0xFFE5E5EA).toArgb() else Color(0xFF222226).toArgb()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(spindleX, spindleY, labelR, labelBgPaint)
        val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
            textSize = labelR * 0.70f
            textAlign = Paint.Align.CENTER
            typeface = getSlateFont(context, 700)
        }
        canvas.drawText("♪", spindleX, spindleY + labelR * 0.26f, notePaint)
    }

    // Center Spindle Hole
    val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.White.toArgb() else Color.Black.toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(spindleX, spindleY, labelR * 0.18f, holePaint)

    // 3. Top-Right Tonearm Assembly
    val armPivotX = cardRect.right - (size * 0.11f)
    val armPivotY = cardRect.top + (size * 0.11f)
    val armBaseR = size * 0.082f
    val armLength = size * 0.45f

    // Base Gimbal Flange
    val armBasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFFCECED2).toArgb() else Color(0xFF28282D).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(armPivotX, armPivotY, armBaseR, armBasePaint)
    canvas.drawCircle(armPivotX, armPivotY, armBaseR, platterStroke)

    val armCenterPin = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF55555A).toArgb() else Color(0xFF141416).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(armPivotX, armPivotY, armBaseR * 0.40f, armCenterPin)

    // Rest Cradle Post
    val restPostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF8E8E93).toArgb() else Color(0xFF333338).toArgb()
        style = Paint.Style.FILL
    }
    val restY = armPivotY + (armLength * 0.50f)
    canvas.drawRoundRect(
        RectF(armPivotX - scaleFactor * 2.5f * uiScale, restY, armPivotX + scaleFactor * 4.5f * uiScale, restY + scaleFactor * 3.5f * uiScale),
        scaleFactor * 1.0f, scaleFactor * 1.0f, restPostPaint
    )

    // Inward Clockwise Rotation onto the record grooves
    val progress = state.progress.coerceIn(0f, 1f)
    val armAngle = if (state.isPlaying) {
        24.0f + (7.5f * progress) // Swings clockwise onto the vinyl grooves
    } else {
        -2.0f // Rest position alongside the record
    }

    canvas.save()
    canvas.rotate(armAngle, armPivotX, armPivotY)

    // Counterweight Stub (top)
    val cwStubH = size * 0.07f
    val cwStubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF9E9EA4).toArgb() else Color(0xFF6E6E73).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(
        RectF(armPivotX - scaleFactor * 1.5f * uiScale, armPivotY - cwStubH, armPivotX + scaleFactor * 1.5f * uiScale, armPivotY),
        scaleFactor * 1f, scaleFactor * 1f, cwStubPaint
    )

    // Aluminum Wand Tube
    val wandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF7A7A80).toArgb() else Color(0xFFDCDCE0).toArgb()
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 2.2f * uiScale
        strokeCap = Paint.Cap.ROUND
    }
    val wandBendStartY = armPivotY + (armLength * 0.65f)
    val wandTipX = armPivotX - (size * 0.095f)
    val wandTipY = armPivotY + armLength

    val wandPath = Path().apply {
        moveTo(armPivotX, armPivotY)
        lineTo(armPivotX, wandBendStartY)
        quadTo(armPivotX, wandBendStartY + (armLength * 0.15f), wandTipX, wandTipY)
    }
    canvas.drawPath(wandPath, wandPaint)

    // Headshell Cartridge
    canvas.save()
    canvas.translate(wandTipX, wandTipY)
    canvas.rotate(24f)

    val hsW = scaleFactor * 7.5f * uiScale
    val hsH = scaleFactor * 14.5f * uiScale
    val hsRect = RectF(-hsW / 2f, 0f, hsW / 2f, hsH)
    val hsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color(0xFF2C2C30).toArgb() else Color(0xFF18181B).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(hsRect, scaleFactor * 1.5f * uiScale, scaleFactor * 1.5f * uiScale, hsPaint)

    val cartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(
        RectF(-hsW * 0.35f, hsH - scaleFactor * 1.5f * uiScale, hsW * 0.35f, hsH + scaleFactor * 3.0f * uiScale),
        scaleFactor * 1.0f * uiScale, scaleFactor * 1.0f * uiScale, cartPaint
    )

    canvas.restore()
    canvas.restore()

    // 4. Track Info Typography (Moved up to create clean separation)
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = (scaleFactor * 10.5f * uiScale).coerceIn(scaleFactor * 8.5f, scaleFactor * 16f)
        typeface = getSlateFont(context, 700)
        textAlign = Paint.Align.CENTER
    }
    val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = (scaleFactor * 8.0f * uiScale).coerceIn(scaleFactor * 6.5f, scaleFactor * 13f)
        typeface = getSlateFont(context, 400)
        textAlign = Paint.Align.CENTER
    }

    val maxTextW = size - (scaleFactor * 24f * uiScale)
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

    val textY1 = cardRect.bottom - (size * 0.30f)
    val textY2 = textY1 + (artistPaint.textSize * 1.30f)

    canvas.drawText(titleDisplay, cardRect.centerX(), textY1, titlePaint)
    canvas.drawText(artistDisplay, cardRect.centerX(), textY2, artistPaint)

    // 5. Minimal Playback Controls Deck
    val ctrlY = cardRect.bottom - (size * 0.11f)
    val ctrlSpacing = size * 0.26f
    val btnR = scaleFactor * 9.5f * uiScale
    val playBtnR = btnR * 1.25f

    val rR = android.graphics.Color.red(accentColor)
    val rG = android.graphics.Color.green(accentColor)
    val rB = android.graphics.Color.blue(accentColor)
    val playLum = (0.2126f * (rR / 255f)) + (0.7152f * (rG / 255f)) + (0.0722f * (rB / 255f))
    val playIconColor = if (playLum > 0.55f) android.graphics.Color.BLACK else android.graphics.Color.WHITE

    drawSkipIcon(canvas, cardRect.centerX() - ctrlSpacing, ctrlY, btnR, isNext = false, color = secondaryTextColor)

    drawPlayPauseIcon(
        canvas,
        cardRect.centerX(),
        ctrlY,
        playBtnR,
        isPlaying = state.isPlaying,
        color = playIconColor,
        fillCircleBg = true,
        circleBgColor = accentColor
    )

    drawSkipIcon(canvas, cardRect.centerX() + ctrlSpacing, ctrlY, btnR, isNext = true, color = secondaryTextColor)

    canvas.restore()

    return bitmap
}
