package com.altusix.slate.widgets.contacts

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.drawConfigurePlaceholderState
import com.altusix.slate.utils.getSafeBgColor
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.altusix.slate.R

/**
 * Draws an official Android Vector Drawable directly onto a Canvas with exact positioning and color tinting.
 */
fun drawVectorIcon(
    canvas: Canvas,
    context: Context,
    @DrawableRes resId: Int,
    cx: Float,
    cy: Float,
    size: Float,
    tintColorInt: Int
) {
    val drawable = ContextCompat.getDrawable(context, resId)?.mutate() ?: return
    drawable.colorFilter = PorterDuffColorFilter(tintColorInt, PorterDuff.Mode.SRC_IN)

    val halfSize = size / 2f
    val left = (cx - halfSize).toInt()
    val top = (cy - halfSize).toInt()
    val right = (cx + halfSize).toInt()
    val bottom = (cy + halfSize).toInt()

    drawable.setBounds(left, top, right, bottom)
    drawable.draw(canvas)
}

private fun loadContactPhoto(context: Context, photoUriStr: String?): Bitmap? {
    if (photoUriStr.isNullOrEmpty()) return null
    return try {
        val uri = Uri.parse(photoUriStr)
        if (uri.scheme == "file") {
            BitmapFactory.decodeFile(uri.path)
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }
    } catch (_: Exception) {
        null
    }
}

private fun drawAvatarNode(
    canvas: Canvas,
    context: Context,
    cx: Float,
    cy: Float,
    radius: Float,
    photoUriStr: String?,
    initials: String,
    accentColorInt: Int,
    fallbackTextColor: Int
) {
    val photoBitmap = loadContactPhoto(context, photoUriStr)

    if (photoBitmap != null) {
        canvas.save()
        val circlePath = Path().apply {
            addCircle(cx, cy, radius, Path.Direction.CW)
        }
        canvas.clipPath(circlePath)

        val srcRect = Rect(0, 0, photoBitmap.width, photoBitmap.height)
        val dstRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(photoBitmap, srcRect, dstRect, paint)

        canvas.restore()
    } else {
        val avatarBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius, avatarBgPaint)

        val initialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fallbackTextColor
            textSize = radius * 0.72f
            typeface = getSlateFont(context, weight = 800)
            textAlign = Paint.Align.CENTER
        }
        val bounds = Rect()
        initialPaint.getTextBounds(initials, 0, initials.length, bounds)
        canvas.drawText(initials, cx, cy + (bounds.height() / 2f), initialPaint)
    }
}

// 1. SINGLE AVATAR CAPSULE (1x2)
fun generateSingleAvatarCapsuleBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val w = (wDp * scaleFactor).toInt().coerceAtLeast(240)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(480)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val targetRatio = 0.5f
        var cardW = w.toFloat()
        var cardH = cardW / targetRatio
        if (cardH > h.toFloat()) {
            cardH = h.toFloat()
            cardW = cardH * targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cardW = cardRect.width()
    val cardH = cardRect.height()
    val cardRadius = minOf(cardW, cardH) / 2f
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()

    // 1. Capsule Background
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    val contactConfig = ContactsWidgetPreferences.loadConfig(context, widgetId)
    if (!contactConfig.isConfigured) {
        drawConfigurePlaceholderState(canvas, context, cardRect, config, scaleFactor)
        return bitmap
    }

    val cx = cardRect.centerX()

    // 2. Avatar Node (Anchored safely inside the top capsule arc)
    val avatarRadius = (cardW * 0.35f).coerceAtMost(cardH * 0.22f)
    val avatarCy = cardRect.top + (cardW / 2f) + (avatarRadius * 0.15f)

    val r = Color.red(accentColorInt) / 255f
    val g = Color.green(accentColorInt) / 255f
    val b = Color.blue(accentColorInt) / 255f
    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
    val avatarTextColor = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

    drawAvatarNode(
        canvas, context, cx, avatarCy, avatarRadius,
        contactConfig.photoUri, contactConfig.initials,
        accentColorInt, avatarTextColor
    )

    // 3. Name & Phone Text Stack (Width-bounded & auto-scaling)
    val maxTextWidth = cardW * 0.82f

    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }
    var nameFontSize = (cardW * 0.13f).coerceIn(scaleFactor * 12f, scaleFactor * 24f)
    namePaint.textSize = nameFontSize
    while (namePaint.measureText(contactConfig.contactName) > maxTextWidth && nameFontSize > scaleFactor * 10f) {
        nameFontSize -= scaleFactor * 0.8f
        namePaint.textSize = nameFontSize
    }

    val displayName = formatSmartName(contactConfig.contactName, namePaint, maxTextWidth)
    val nameY = avatarCy + avatarRadius + (cardH * 0.08f)
    canvas.drawText(displayName, cx, nameY, namePaint)

    val phonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        typeface = getSlateFont(context, weight = 500)
        textAlign = Paint.Align.CENTER
    }
    var phoneFontSize = (cardW * 0.09f).coerceIn(scaleFactor * 10f, scaleFactor * 18f)
    phonePaint.textSize = phoneFontSize
    while (phonePaint.measureText(contactConfig.phoneNumber) > maxTextWidth && phoneFontSize > scaleFactor * 8f) {
        phoneFontSize -= scaleFactor * 0.8f
        phonePaint.textSize = phoneFontSize
    }

    val displayPhone = formatSmartName(contactConfig.phoneNumber, phonePaint, maxTextWidth)
    canvas.drawText(displayPhone, cx, nameY + (phoneFontSize * 1.35f), phonePaint)

    // 4. Action Badge Pill (Anchored safely inside the bottom capsule arc)
    val badgeW = cardW * 0.74f
    val badgeH = (cardW * 0.24f).coerceAtMost(cardH * 0.14f)
    val badgeY = cardRect.bottom - (cardW / 2f)
    val badgeRect = RectF(cx - badgeW / 2f, badgeY - badgeH / 2f, cx + badgeW / 2f, badgeY + badgeH / 2f)

    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(badgeRect, badgeH / 2f, badgeH / 2f, badgePaint)

    val badgeTextColor = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE
    val badgeLabel = contactConfig.actionType.badgeText
    val actionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = badgeTextColor
        textSize = badgeH * 0.44f
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.06f
    }
    val actionBounds = Rect()
    actionPaint.getTextBounds(badgeLabel, 0, badgeLabel.length, actionBounds)
    canvas.drawText(badgeLabel, cx, badgeY + (actionBounds.height() / 2f), actionPaint)

    return bitmap
}

// 2. HORIZONTAL SPEED DIAL (2x1 / Smart Single-Contact Pill)
fun generateHorizontalSpeedDialBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val w = (wDp * scaleFactor).toInt().coerceAtLeast(240)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(180)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE

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

    val cardH = cardRect.height()
    val cardW = cardRect.width()
    val pillRadius = minOf(cardW, cardH) / 2f
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()

    // 1. Container Background
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, pillRadius, pillRadius, bgPaint)

    val contactConfig = ContactsWidgetPreferences.loadConfig(context, widgetId)
    if (!contactConfig.isConfigured) {
        drawConfigurePlaceholderState(canvas, context, cardRect, config, scaleFactor)
        return bitmap
    }

    val aspectRatio = cardW / cardH
    val isCompactMode = aspectRatio < 1.45f

    if (isCompactMode) {
        // ====================================================================
        // COMPACT RESPONSIVE MODE: Full-bleed Center-Cropped Image (Aspect Fill)
        // ====================================================================
        val photoBitmap = loadContactPhoto(context, contactConfig.photoUri)

        if (photoBitmap != null) {
            canvas.save()
            val clipPath = Path().apply {
                addRoundRect(cardRect, pillRadius, pillRadius, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)

            // Center-Crop / Aspect Fill Math (Prevents Image Stretching)
            val targetRatio = cardW / cardH
            val imgW = photoBitmap.width.toFloat()
            val imgH = photoBitmap.height.toFloat()
            val imgRatio = imgW / imgH

            val srcRect = if (imgRatio > targetRatio) {
                val cropW = imgH * targetRatio
                val left = (imgW - cropW) / 2f
                Rect(left.toInt(), 0, (left + cropW).toInt(), photoBitmap.height)
            } else {
                val cropH = imgW / targetRatio
                val top = (imgH - cropH) / 2f
                Rect(0, top.toInt(), photoBitmap.width, (top + cropH).toInt())
            }

            val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(photoBitmap, srcRect, cardRect, imagePaint)

            // Bottom Gradient Scrim for text contrast
            val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.LinearGradient(
                    0f, cardRect.centerY(), 0f, cardRect.bottom,
                    Color.TRANSPARENT, Color.argb(200, 0, 0, 0),
                    android.graphics.Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(cardRect, gradientPaint)
            canvas.restore()
        } else {
            // Fallback Initials Circle
            val avatarRadius = minOf(cardW, cardH) * 0.35f
            val avatarCy = cardRect.centerY() - (cardH * 0.08f)

            val r = Color.red(accentColorInt) / 255f
            val g = Color.green(accentColorInt) / 255f
            val b = Color.blue(accentColorInt) / 255f
            val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
            val avatarTextColor = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

            drawAvatarNode(
                canvas, context, cardRect.centerX(), avatarCy, avatarRadius,
                null, contactConfig.initials, accentColorInt, avatarTextColor
            )
        }

        // Overlay Name Text at the bottom (Width-bounded so text scales correctly in tall mode)
        val availableTextWidth = cardW * 0.82f
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }

        var fontSize = (cardW * 0.20f).coerceIn(scaleFactor * 13f, scaleFactor * 26f)
        namePaint.textSize = fontSize

        while (namePaint.measureText(contactConfig.contactName) > availableTextWidth && fontSize > scaleFactor * 11f) {
            fontSize -= scaleFactor * 0.8f
            namePaint.textSize = fontSize
        }

        val displayName = formatSmartName(contactConfig.contactName, namePaint, availableTextWidth)

        val bottomOffset = if (cardH > cardW * 1.5f) cardW * 0.38f else cardH * 0.14f
        val textY = cardRect.bottom - bottomOffset
        canvas.drawText(displayName, cardRect.centerX(), textY, namePaint)

    } else {
        // ====================================================================
        // STANDARD HORIZONTAL MODE: Left Avatar Circle + Smart Right Name
        // ====================================================================
        val avatarRadius = cardH * 0.36f
        val avatarCx = cardRect.left + (cardH / 2f)
        val avatarCy = cardRect.centerY()

        val r = Color.red(accentColorInt) / 255f
        val g = Color.green(accentColorInt) / 255f
        val b = Color.blue(accentColorInt) / 255f
        val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
        val avatarTextColor = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

        // Accent Ring around Avatar Node
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.STROKE
            strokeWidth = scaleFactor * 2.0f
        }
        canvas.drawCircle(avatarCx, avatarCy, avatarRadius + (scaleFactor * 1.8f), ringPaint)

        drawAvatarNode(
            canvas, context, avatarCx, avatarCy, avatarRadius,
            contactConfig.photoUri, contactConfig.initials,
            accentColorInt, avatarTextColor
        )

        // Smart Right Side Name Formatting
        val textLeft = avatarCx + avatarRadius + (cardH * 0.22f)
        val availableTextWidth = cardRect.right - textLeft - (cardH * 0.20f)

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.LEFT
        }

        var fontSize = cardH * 0.28f
        namePaint.textSize = fontSize
        while (namePaint.measureText(contactConfig.contactName) > availableTextWidth && fontSize > scaleFactor * 10f) {
            fontSize -= scaleFactor * 0.8f
            namePaint.textSize = fontSize
        }

        val displayName = formatSmartName(contactConfig.contactName, namePaint, availableTextWidth)

        val bounds = Rect()
        namePaint.getTextBounds(displayName, 0, displayName.length, bounds)
        val nameY = avatarCy + (bounds.height() / 2f)

        canvas.drawText(displayName, textLeft, nameY, namePaint)
    }

    return bitmap
}

/**
 * Truncates text with "..." if it exceeds max available width after downscaling.
 */
private fun formatSmartName(name: String, paint: Paint, maxW: Float): String {
    if (paint.measureText(name) <= maxW) return name
    var end = name.length
    while (end > 1 && paint.measureText(name.substring(0, end) + "…") > maxW) {
        end--
    }
    return name.substring(0, end).trim() + "…"
}


// 3. EDITORIAL BENTO CONTACTS (4x2 / 3-Section Bento Widget)
fun generateEditorialBentoContactsBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val w = (wDp * scaleFactor).toInt().coerceAtLeast((240 * scaleFactor).toInt())
    val h = (hDp * scaleFactor).toInt().coerceAtLeast((120 * scaleFactor).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE

    // Compute luminance once at function scope for contrast checks
    val r = Color.red(accentColorInt) / 255f
    val g = Color.green(accentColorInt) / 255f
    val b = Color.blue(accentColorInt) / 255f
    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b

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

    // 1. Strict Fixed Outer Corner Radius
    val outerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val contactConfig = ContactsWidgetPreferences.loadConfig(context, widgetId)
    if (!contactConfig.isConfigured) {
        drawConfigurePlaceholderState(canvas, context, cardRect, config, scaleFactor)
        return bitmap
    }

    // 2. Strict Fixed DP Padding & Gap
    val pad = scaleFactor * 8f
    val gap = scaleFactor * 8f

    val innerW = cardRect.width() - (pad * 2f) - gap
    val innerH = cardRect.height() - (pad * 2f)

    val heroW = innerW * 0.52f
    val rightW = innerW - heroW
    val rightH = (innerH - gap) / 2f

    val heroRect = RectF(
        cardRect.left + pad,
        cardRect.top + pad,
        cardRect.left + pad + heroW,
        cardRect.bottom - pad
    )

    val callRect = RectF(
        heroRect.right + gap,
        cardRect.top + pad,
        cardRect.right - pad,
        cardRect.top + pad + rightH
    )

    val msgRect = RectF(
        heroRect.right + gap,
        callRect.bottom + gap,
        cardRect.right - pad,
        cardRect.bottom - pad
    )

    // Concentric Inner Corner Radius
    val innerCardRadius = (outerRadius - pad).coerceAtLeast(scaleFactor * 6f)
    val innerCardBg = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    // =========================================================================
    // SECTION 1: HERO CONTACT CARD (Left - Full-Bleed Image / Avatar)
    // =========================================================================
    val photoBitmap = loadContactPhoto(context, contactConfig.photoUri)

    canvas.save()
    val heroClipPath = Path().apply {
        addRoundRect(heroRect, innerCardRadius, innerCardRadius, Path.Direction.CW)
    }
    canvas.clipPath(heroClipPath)

    if (photoBitmap != null) {
        val targetRatio = heroRect.width() / heroRect.height()
        val imgW = photoBitmap.width.toFloat()
        val imgH = photoBitmap.height.toFloat()
        val imgRatio = imgW / imgH

        val srcRect = if (imgRatio > targetRatio) {
            val cropW = imgH * targetRatio
            val left = (imgW - cropW) / 2f
            Rect(left.toInt(), 0, (left + cropW).toInt(), photoBitmap.height)
        } else {
            val cropH = imgW / targetRatio
            val top = (imgH - cropH) / 2f
            Rect(0, top.toInt(), photoBitmap.width, (top + cropH).toInt())
        }

        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(photoBitmap, srcRect, heroRect, imagePaint)

        val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.LinearGradient(
                0f, heroRect.centerY(), 0f, heroRect.bottom,
                Color.TRANSPARENT, Color.argb(190, 0, 0, 0),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(heroRect, gradientPaint)
    } else {
        canvas.drawRoundRect(heroRect, innerCardRadius, innerCardRadius, cardPaint)

        val heroCx = heroRect.centerX()
        val avatarRadius = (minOf(heroRect.width(), heroRect.height()) * 0.28f).coerceAtLeast(scaleFactor * 12f)
        val avatarCy = heroRect.centerY() - (scaleFactor * 8f)

        val avatarTextColor = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

        drawAvatarNode(
            canvas, context, heroCx, avatarCy, avatarRadius,
            null, contactConfig.initials,
            accentColorInt, avatarTextColor
        )
    }
    canvas.restore()

    // Overlay Name Text at Bottom-Left of Hero Block
    val textPaddingX = scaleFactor * 10f
    val textPaddingBottom = scaleFactor * 10f
    val maxHeroTextW = heroRect.width() - (textPaddingX * 2f)

    val nameTextColor = if (photoBitmap != null) Color.WHITE else primaryText
    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = nameTextColor
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.LEFT
    }

    var fontSize = (heroRect.height() * 0.16f).coerceIn(scaleFactor * 11f, scaleFactor * 22f)
    namePaint.textSize = fontSize
    while (namePaint.measureText(contactConfig.contactName) > maxHeroTextW && fontSize > scaleFactor * 9f) {
        fontSize -= scaleFactor * 0.8f
        namePaint.textSize = fontSize
    }

    val displayName = formatSmartName(contactConfig.contactName, namePaint, maxHeroTextW)
    val textX = heroRect.left + textPaddingX
    val textY = heroRect.bottom - textPaddingBottom

    canvas.drawText(displayName, textX, textY, namePaint)

    // =========================================================================
    // SECTION 2: CALL CARD (Top Right)
    // =========================================================================
    val accentCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(callRect, innerCardRadius, innerCardRadius, accentCardPaint)

    val callTextColor = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE
    val callIconSize = (callRect.height() * 0.42f).coerceAtLeast(scaleFactor * 12f)

    val callTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = callTextColor
        textSize = (callRect.height() * 0.30f).coerceIn(scaleFactor * 10f, scaleFactor * 18f)
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.LEFT
    }

    val callTextWidth = callTextPaint.measureText("Call")
    val callGap = scaleFactor * 6f
    val totalCallContentW = callIconSize + callGap + callTextWidth
    val availableCallW = callRect.width() * 0.85f

    if (totalCallContentW <= availableCallW) {
        val startX = callRect.centerX() - (totalCallContentW / 2f)
        val iconCx = startX + (callIconSize / 2f)
        val textX = startX + callIconSize + callGap

        drawVectorIcon(canvas, context, R.drawable.ic_phone, iconCx, callRect.centerY(), callIconSize, callTextColor)

        val callBounds = Rect()
        callTextPaint.getTextBounds("Call", 0, 4, callBounds)
        canvas.drawText("Call", textX, callRect.centerY() + (callBounds.height() / 2f), callTextPaint)
    } else {
        drawVectorIcon(canvas, context, R.drawable.ic_phone, callRect.centerX(), callRect.centerY(), callIconSize * 1.1f, callTextColor)
    }

    // =========================================================================
    // SECTION 3: MESSAGE CARD (Bottom Right)
    // =========================================================================
    canvas.drawRoundRect(msgRect, innerCardRadius, innerCardRadius, cardPaint)

    val msgIconSize = (msgRect.height() * 0.42f).coerceAtLeast(scaleFactor * 12f)

    val msgTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = (msgRect.height() * 0.30f).coerceIn(scaleFactor * 10f, scaleFactor * 18f)
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.LEFT
    }

    val msgTextWidth = msgTextPaint.measureText("Message")
    val msgGap = scaleFactor * 6f
    val totalMsgContentW = msgIconSize + msgGap + msgTextWidth
    val availableMsgW = msgRect.width() * 0.85f

    if (totalMsgContentW <= availableMsgW) {
        val startX = msgRect.centerX() - (totalMsgContentW / 2f)
        val iconCx = startX + (msgIconSize / 2f)
        val textX = startX + msgIconSize + msgGap

        drawVectorIcon(canvas, context, R.drawable.ic_message, iconCx, msgRect.centerY(), msgIconSize, primaryText)

        val msgBounds = Rect()
        msgTextPaint.getTextBounds("Message", 0, 7, msgBounds)
        canvas.drawText("Message", textX, msgRect.centerY() + (msgBounds.height() / 2f), msgTextPaint)
    } else {
        drawVectorIcon(canvas, context, R.drawable.ic_message, msgRect.centerX(), msgRect.centerY(), msgIconSize * 1.1f, primaryText)
    }

    return bitmap
}
