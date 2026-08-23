package com.altusix.slate.widgets.contacts

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.altusix.slate.R
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.drawConfigurePlaceholderState
import com.altusix.slate.utils.getSafeBgColor
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius
import com.altusix.slate.widgets.applauncher.LauncherShape
import com.altusix.slate.widgets.applauncher.getShapePath

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

private fun formatSmartName(name: String, paint: Paint, maxW: Float): String {
    if (paint.measureText(name) <= maxW) return name
    var end = name.length
    while (end > 1 && paint.measureText(name.substring(0, end) + "…") > maxW) {
        end--
    }
    return name.substring(0, end).trim() + "…"
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

    val minDimension = (60 * scaleFactor).toInt()
    val w = (wDp * scaleFactor).toInt().coerceAtLeast(minDimension)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(minDimension)

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

// 2. HORIZONTAL SPEED DIAL (2x1)
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

    val minDimension = (60 * scaleFactor).toInt()
    val w = (wDp * scaleFactor).toInt().coerceAtLeast(minDimension)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(minDimension)

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
        val photoBitmap = loadContactPhoto(context, contactConfig.photoUri)

        if (photoBitmap != null) {
            canvas.save()
            val clipPath = Path().apply {
                addRoundRect(cardRect, pillRadius, pillRadius, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)

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
        val avatarRadius = cardH * 0.36f
        val avatarCx = cardRect.left + (cardH / 2f)
        val avatarCy = cardRect.centerY()

        val r = Color.red(accentColorInt) / 255f
        val g = Color.green(accentColorInt) / 255f
        val b = Color.blue(accentColorInt) / 255f
        val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
        val avatarTextColor = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

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

// 3. EDITORIAL BENTO CONTACTS (4x2)
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

    val minDimension = (60 * scaleFactor).toInt()
    val w = (wDp * scaleFactor).toInt().coerceAtLeast(minDimension)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(minDimension)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE

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

    val pad = scaleFactor * 8f
    val gap = scaleFactor * 8f

    val innerW = (cardRect.width() - (pad * 2f) - gap).coerceAtLeast(1f)
    val innerH = (cardRect.height() - (pad * 2f)).coerceAtLeast(1f)

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

    val innerCardRadius = (outerRadius - pad).coerceAtLeast(scaleFactor * 6f)
    val innerCardBg = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    // SECTION 1: HERO CONTACT CARD
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

    // SECTIONS 2 & 3: CALL AND MESSAGE CARDS
    val accentCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(callRect, innerCardRadius, innerCardRadius, accentCardPaint)
    canvas.drawRoundRect(msgRect, innerCardRadius, innerCardRadius, cardPaint)

    val callTextColor = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

    val baseIconSize = minOf(
        callRect.height() * 0.44f,
        callRect.width() * 0.28f,
        scaleFactor * 34f
    ).coerceAtLeast(scaleFactor * 14f)

    val callTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = callTextColor
        textSize = (callRect.height() * 0.32f).coerceIn(scaleFactor * 11f, scaleFactor * 22f)
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.LEFT
    }

    val msgTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = (msgRect.height() * 0.32f).coerceIn(scaleFactor * 11f, scaleFactor * 22f)
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.LEFT
    }

    val contentGap = scaleFactor * 6f
    val callTextWidth = callTextPaint.measureText("Call")
    val msgTextWidth = msgTextPaint.measureText("Message")

    val totalCallContentW = baseIconSize + contentGap + callTextWidth
    val totalMsgContentW = baseIconSize + contentGap + msgTextWidth

    val availableCallW = callRect.width() * 0.88f
    val availableMsgW = msgRect.width() * 0.88f

    val showLabels = (totalCallContentW <= availableCallW) && (totalMsgContentW <= availableMsgW)

    if (showLabels) {
        val callStartX = callRect.centerX() - (totalCallContentW / 2f)
        val callIconCx = callStartX + (baseIconSize / 2f)
        val callTextX = callStartX + baseIconSize + contentGap
        val callTextY = callRect.centerY() - ((callTextPaint.descent() + callTextPaint.ascent()) / 2f)

        drawVectorIcon(canvas, context, R.drawable.ic_phone, callIconCx, callRect.centerY(), baseIconSize, callTextColor)
        canvas.drawText("Call", callTextX, callTextY, callTextPaint)

        val msgStartX = msgRect.centerX() - (totalMsgContentW / 2f)
        val msgIconCx = msgStartX + (baseIconSize / 2f)
        val msgTextX = msgStartX + baseIconSize + contentGap
        val msgTextY = msgRect.centerY() - ((msgTextPaint.descent() + msgTextPaint.ascent()) / 2f)

        drawVectorIcon(canvas, context, R.drawable.ic_message, msgIconCx, msgRect.centerY(), baseIconSize, primaryText)
        canvas.drawText("Message", msgTextX, msgTextY, msgTextPaint)
    } else {
        val iconOnlySize = minOf(
            callRect.height() * 0.50f,
            callRect.width() * 0.50f,
            scaleFactor * 36f
        ).coerceAtLeast(scaleFactor * 14f)

        drawVectorIcon(canvas, context, R.drawable.ic_phone, callRect.centerX(), callRect.centerY(), iconOnlySize, callTextColor)
        drawVectorIcon(canvas, context, R.drawable.ic_message, msgRect.centerX(), msgRect.centerY(), iconOnlySize, primaryText)
    }

    return bitmap
}

// 4. STACKED BENTO CONTACTS (2x2)
fun generateStackedBentoContactsBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val minDimension = (60 * scaleFactor).toInt()
    val w = (wDp * scaleFactor).toInt().coerceAtLeast(minDimension)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(minDimension)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE

    val r = Color.red(accentColorInt) / 255f
    val g = Color.green(accentColorInt) / 255f
    val b = Color.blue(accentColorInt) / 255f
    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val size = minOf(w, h).toFloat()
        val leftX = (w - size) / 2f
        val topY = (h - size) / 2f
        RectF(leftX, topY, leftX + size, topY + size)
    }

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

    val pad = scaleFactor * 8f
    val gap = scaleFactor * 8f

    val innerW = (cardRect.width() - (pad * 2f)).coerceAtLeast(1f)
    val innerH = (cardRect.height() - (pad * 2f) - gap).coerceAtLeast(1f)

    val heroH = innerH * 0.54f
    val bottomH = innerH - heroH
    val bottomW = (innerW - gap) / 2f

    val heroRect = RectF(
        cardRect.left + pad,
        cardRect.top + pad,
        cardRect.right - pad,
        cardRect.top + pad + heroH
    )

    val callRect = RectF(
        cardRect.left + pad,
        heroRect.bottom + gap,
        cardRect.left + pad + bottomW,
        cardRect.bottom - pad
    )

    val msgRect = RectF(
        callRect.right + gap,
        heroRect.bottom + gap,
        cardRect.right - pad,
        cardRect.bottom - pad
    )

    val innerCardRadius = (outerRadius - pad).coerceAtLeast(scaleFactor * 6f)
    val innerCardBg = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    // SECTION 1: TOP HERO CARD
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

    val textPaddingX = scaleFactor * 10f
    val textPaddingBottom = scaleFactor * 8f
    val maxHeroTextW = heroRect.width() - (textPaddingX * 2f)

    val nameTextColor = if (photoBitmap != null) Color.WHITE else primaryText
    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = nameTextColor
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.LEFT
    }

    var fontSize = (heroRect.height() * 0.22f).coerceIn(scaleFactor * 11f, scaleFactor * 22f)
    namePaint.textSize = fontSize
    while (namePaint.measureText(contactConfig.contactName) > maxHeroTextW && fontSize > scaleFactor * 9f) {
        fontSize -= scaleFactor * 0.8f
        namePaint.textSize = fontSize
    }

    val displayName = formatSmartName(contactConfig.contactName, namePaint, maxHeroTextW)
    val textX = heroRect.left + textPaddingX
    val textY = heroRect.bottom - textPaddingBottom

    canvas.drawText(displayName, textX, textY, namePaint)

    // SECTIONS 2 & 3: BOTTOM CALL AND MESSAGE CARDS
    val accentCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(callRect, innerCardRadius, innerCardRadius, accentCardPaint)
    canvas.drawRoundRect(msgRect, innerCardRadius, innerCardRadius, cardPaint)

    val callTextColor = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

    val baseIconSize = minOf(
        callRect.height() * 0.44f,
        callRect.width() * 0.28f,
        scaleFactor * 34f
    ).coerceAtLeast(scaleFactor * 14f)

    val callTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = callTextColor
        textSize = (callRect.height() * 0.32f).coerceIn(scaleFactor * 11f, scaleFactor * 22f)
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.LEFT
    }

    val msgTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = (msgRect.height() * 0.32f).coerceIn(scaleFactor * 11f, scaleFactor * 22f)
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.LEFT
    }

    val contentGap = scaleFactor * 6f
    val callTextWidth = callTextPaint.measureText("Call")
    val msgTextWidth = msgTextPaint.measureText("Message")

    val totalCallContentW = baseIconSize + contentGap + callTextWidth
    val totalMsgContentW = baseIconSize + contentGap + msgTextWidth

    val availableCallW = callRect.width() * 0.88f
    val availableMsgW = msgRect.width() * 0.88f

    val showLabels = (totalCallContentW <= availableCallW) && (totalMsgContentW <= availableMsgW)

    if (showLabels) {
        val callStartX = callRect.centerX() - (totalCallContentW / 2f)
        val callIconCx = callStartX + (baseIconSize / 2f)
        val callTextX = callStartX + baseIconSize + contentGap
        val callTextY = callRect.centerY() - ((callTextPaint.descent() + callTextPaint.ascent()) / 2f)

        drawVectorIcon(canvas, context, R.drawable.ic_phone, callIconCx, callRect.centerY(), baseIconSize, callTextColor)
        canvas.drawText("Call", callTextX, callTextY, callTextPaint)

        val msgStartX = msgRect.centerX() - (totalMsgContentW / 2f)
        val msgIconCx = msgStartX + (baseIconSize / 2f)
        val msgTextX = msgStartX + baseIconSize + contentGap
        val msgTextY = msgRect.centerY() - ((msgTextPaint.descent() + msgTextPaint.ascent()) / 2f)

        drawVectorIcon(canvas, context, R.drawable.ic_message, msgIconCx, msgRect.centerY(), baseIconSize, primaryText)
        canvas.drawText("Message", msgTextX, msgTextY, msgTextPaint)
    } else {
        val iconOnlySize = minOf(
            callRect.height() * 0.50f,
            callRect.width() * 0.50f,
            scaleFactor * 36f
        ).coerceAtLeast(scaleFactor * 14f)

        drawVectorIcon(canvas, context, R.drawable.ic_phone, callRect.centerX(), callRect.centerY(), iconOnlySize, callTextColor)
        drawVectorIcon(canvas, context, R.drawable.ic_message, msgRect.centerX(), msgRect.centerY(), iconOnlySize, primaryText)
    }

    return bitmap
}

// 5. EDITORIAL 3-ACTION BENTO CONTACTS (4x2)
fun generateEditorial3ActionBentoContactsBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val minDimension = (60 * scaleFactor).toInt()
    val w = (wDp * scaleFactor).toInt().coerceAtLeast(minDimension)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(minDimension)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE

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

    val pad = scaleFactor * 8f
    val gap = scaleFactor * 6f

    val innerW = (cardRect.width() - (pad * 2f) - gap).coerceAtLeast(1f)
    val innerH = (cardRect.height() - (pad * 2f)).coerceAtLeast(1f)

    val heroW = innerW * 0.50f
    val rightH = (innerH - (gap * 2f)) / 3f

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
        callRect.bottom + gap + rightH
    )

    val waRect = RectF(
        heroRect.right + gap,
        msgRect.bottom + gap,
        cardRect.right - pad,
        cardRect.bottom - pad
    )

    val innerCardRadius = (outerRadius - pad).coerceAtLeast(scaleFactor * 6f)
    val innerCardBg = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    // HERO CONTACT CARD
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

    // ACTION BUTTONS
    val accentCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(callRect, innerCardRadius, innerCardRadius, accentCardPaint)
    canvas.drawRoundRect(msgRect, innerCardRadius, innerCardRadius, cardPaint)
    canvas.drawRoundRect(waRect, innerCardRadius, innerCardRadius, cardPaint)

    val callTextColor = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

    val baseIconSize = minOf(
        callRect.height() * 0.46f,
        callRect.width() * 0.26f,
        scaleFactor * 28f
    ).coerceAtLeast(scaleFactor * 12f)

    val btnTextSize = (callRect.height() * 0.34f).coerceIn(scaleFactor * 10f, scaleFactor * 18f)

    val callTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = callTextColor
        textSize = btnTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.LEFT
    }

    val msgTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = btnTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.LEFT
    }

    val contentGap = scaleFactor * 6f
    val callTextWidth = callTextPaint.measureText("Call")
    val msgTextWidth = msgTextPaint.measureText("Message")
    val waTextWidth = msgTextPaint.measureText("WhatsApp")

    val availableW = callRect.width() * 0.88f
    val showLabels = (baseIconSize + contentGap + callTextWidth <= availableW) &&
            (baseIconSize + contentGap + msgTextWidth <= availableW) &&
            (baseIconSize + contentGap + waTextWidth <= availableW)

    if (showLabels) {
        val callW = baseIconSize + contentGap + callTextWidth
        val callStartX = callRect.centerX() - (callW / 2f)
        val callTextY = callRect.centerY() - ((callTextPaint.descent() + callTextPaint.ascent()) / 2f)
        drawVectorIcon(canvas, context, R.drawable.ic_phone, callStartX + (baseIconSize / 2f), callRect.centerY(), baseIconSize, callTextColor)
        canvas.drawText("Call", callStartX + baseIconSize + contentGap, callTextY, callTextPaint)

        val msgW = baseIconSize + contentGap + msgTextWidth
        val msgStartX = msgRect.centerX() - (msgW / 2f)
        val msgTextY = msgRect.centerY() - ((msgTextPaint.descent() + msgTextPaint.ascent()) / 2f)
        drawVectorIcon(canvas, context, R.drawable.ic_message, msgStartX + (baseIconSize / 2f), msgRect.centerY(), baseIconSize, primaryText)
        canvas.drawText("Message", msgStartX + baseIconSize + contentGap, msgTextY, msgTextPaint)

        val waW = baseIconSize + contentGap + waTextWidth
        val waStartX = waRect.centerX() - (waW / 2f)
        val waTextY = waRect.centerY() - ((msgTextPaint.descent() + msgTextPaint.ascent()) / 2f)
        drawVectorIcon(canvas, context, R.drawable.ic_whatsapp, waStartX + (baseIconSize / 2f), waRect.centerY(), baseIconSize, primaryText)
        canvas.drawText("WhatsApp", waStartX + baseIconSize + contentGap, waTextY, msgTextPaint)
    } else {
        val iconOnlySize = minOf(
            callRect.height() * 0.52f,
            callRect.width() * 0.48f,
            scaleFactor * 32f
        ).coerceAtLeast(scaleFactor * 12f)

        drawVectorIcon(canvas, context, R.drawable.ic_phone, callRect.centerX(), callRect.centerY(), iconOnlySize, callTextColor)
        drawVectorIcon(canvas, context, R.drawable.ic_message, msgRect.centerX(), msgRect.centerY(), iconOnlySize, primaryText)
        drawVectorIcon(canvas, context, R.drawable.ic_whatsapp, waRect.centerX(), waRect.centerY(), iconOnlySize, primaryText)
    }

    return bitmap
}

// 6. STACKED 3-ACTION BENTO CONTACTS (2x2)
fun generateStacked3ActionBentoContactsBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val minDimension = (60 * scaleFactor).toInt()
    val w = (wDp * scaleFactor).toInt().coerceAtLeast(minDimension)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(minDimension)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE

    val r = Color.red(accentColorInt) / 255f
    val g = Color.green(accentColorInt) / 255f
    val b = Color.blue(accentColorInt) / 255f
    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val size = minOf(w, h).toFloat()
        val leftX = (w - size) / 2f
        val topY = (h - size) / 2f
        RectF(leftX, topY, leftX + size, topY + size)
    }

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

    val pad = scaleFactor * 8f
    val gap = scaleFactor * 6f

    val innerW = (cardRect.width() - (pad * 2f) - (gap * 2f)).coerceAtLeast(1f)
    val innerH = (cardRect.height() - (pad * 2f) - gap).coerceAtLeast(1f)

    val heroH = innerH * 0.54f
    val bottomH = innerH - heroH
    val bottomW = innerW / 3f

    val heroRect = RectF(
        cardRect.left + pad,
        cardRect.top + pad,
        cardRect.right - pad,
        cardRect.top + pad + heroH
    )

    val callRect = RectF(
        cardRect.left + pad,
        heroRect.bottom + gap,
        cardRect.left + pad + bottomW,
        cardRect.bottom - pad
    )

    val msgRect = RectF(
        callRect.right + gap,
        heroRect.bottom + gap,
        callRect.right + gap + bottomW,
        cardRect.bottom - pad
    )

    val waRect = RectF(
        msgRect.right + gap,
        heroRect.bottom + gap,
        cardRect.right - pad,
        cardRect.bottom - pad
    )

    val innerCardRadius = (outerRadius - pad).coerceAtLeast(scaleFactor * 6f)
    val innerCardBg = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    // TOP HERO CARD
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

    val textPaddingX = scaleFactor * 10f
    val textPaddingBottom = scaleFactor * 8f
    val maxHeroTextW = heroRect.width() - (textPaddingX * 2f)

    val nameTextColor = if (photoBitmap != null) Color.WHITE else primaryText
    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = nameTextColor
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.LEFT
    }

    var fontSize = (heroRect.height() * 0.22f).coerceIn(scaleFactor * 11f, scaleFactor * 22f)
    namePaint.textSize = fontSize
    while (namePaint.measureText(contactConfig.contactName) > maxHeroTextW && fontSize > scaleFactor * 9f) {
        fontSize -= scaleFactor * 0.8f
        namePaint.textSize = fontSize
    }

    val displayName = formatSmartName(contactConfig.contactName, namePaint, maxHeroTextW)
    val textX = heroRect.left + textPaddingX
    val textY = heroRect.bottom - textPaddingBottom

    canvas.drawText(displayName, textX, textY, namePaint)

    // BOTTOM 3 ACTIONS
    val accentCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(callRect, innerCardRadius, innerCardRadius, accentCardPaint)
    canvas.drawRoundRect(msgRect, innerCardRadius, innerCardRadius, cardPaint)
    canvas.drawRoundRect(waRect, innerCardRadius, innerCardRadius, cardPaint)

    val callTextColor = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

    val baseIconSize = minOf(
        callRect.height() * 0.44f,
        callRect.width() * 0.28f,
        scaleFactor * 30f
    ).coerceAtLeast(scaleFactor * 12f)

    val btnTextSize = (callRect.height() * 0.32f).coerceIn(scaleFactor * 10f, scaleFactor * 18f)

    val callTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = callTextColor
        textSize = btnTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.LEFT
    }

    val msgTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = btnTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.LEFT
    }

    val contentGap = scaleFactor * 4f
    val callTextWidth = callTextPaint.measureText("Call")
    val msgTextWidth = msgTextPaint.measureText("Msg")
    val waTextWidth = msgTextPaint.measureText("WA")

    val availableW = callRect.width() * 0.90f
    val showLabels = (baseIconSize + contentGap + callTextWidth <= availableW) &&
            (baseIconSize + contentGap + msgTextWidth <= availableW) &&
            (baseIconSize + contentGap + waTextWidth <= availableW)

    if (showLabels) {
        val callW = baseIconSize + contentGap + callTextWidth
        val callStartX = callRect.centerX() - (callW / 2f)
        val callTextY = callRect.centerY() - ((callTextPaint.descent() + callTextPaint.ascent()) / 2f)
        drawVectorIcon(canvas, context, R.drawable.ic_phone, callStartX + (baseIconSize / 2f), callRect.centerY(), baseIconSize, callTextColor)
        canvas.drawText("Call", callStartX + baseIconSize + contentGap, callTextY, callTextPaint)

        val msgW = baseIconSize + contentGap + msgTextWidth
        val msgStartX = msgRect.centerX() - (msgW / 2f)
        val msgTextY = msgRect.centerY() - ((msgTextPaint.descent() + msgTextPaint.ascent()) / 2f)
        drawVectorIcon(canvas, context, R.drawable.ic_message, msgStartX + (baseIconSize / 2f), msgRect.centerY(), baseIconSize, primaryText)
        canvas.drawText("Msg", msgStartX + baseIconSize + contentGap, msgTextY, msgTextPaint)

        val waW = baseIconSize + contentGap + waTextWidth
        val waStartX = waRect.centerX() - (waW / 2f)
        val waTextY = waRect.centerY() - ((msgTextPaint.descent() + msgTextPaint.ascent()) / 2f)
        drawVectorIcon(canvas, context, R.drawable.ic_whatsapp, waStartX + (baseIconSize / 2f), waRect.centerY(), baseIconSize, primaryText)
        canvas.drawText("WA", waStartX + baseIconSize + contentGap, waTextY, msgTextPaint)
    } else {
        val iconOnlySize = minOf(
            callRect.height() * 0.52f,
            callRect.width() * 0.52f,
            scaleFactor * 32f
        ).coerceAtLeast(scaleFactor * 12f)

        drawVectorIcon(canvas, context, R.drawable.ic_phone, callRect.centerX(), callRect.centerY(), iconOnlySize, callTextColor)
        drawVectorIcon(canvas, context, R.drawable.ic_message, msgRect.centerX(), msgRect.centerY(), iconOnlySize, primaryText)
        drawVectorIcon(canvas, context, R.drawable.ic_whatsapp, waRect.centerX(), waRect.centerY(), iconOnlySize, primaryText)
    }

    return bitmap
}

// 7. FULL PHOTO CONTACT (2x2)
fun generateFullPhotoContactBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val minDimension = (60 * scaleFactor).toInt()
    val w = (wDp * scaleFactor).toInt().coerceAtLeast(minDimension)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(minDimension)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE

    val r = Color.red(accentColorInt) / 255f
    val g = Color.green(accentColorInt) / 255f
    val b = Color.blue(accentColorInt) / 255f
    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val size = minOf(w, h).toFloat()
        val leftX = (w - size) / 2f
        val topY = (h - size) / 2f
        RectF(leftX, topY, leftX + size, topY + size)
    }

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

    val photoBitmap = loadContactPhoto(context, contactConfig.photoUri)

    if (photoBitmap != null) {
        canvas.save()
        val clipPath = Path().apply {
            addRoundRect(cardRect, outerRadius, outerRadius, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)

        val targetRatio = cardRect.width() / cardRect.height()
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
        canvas.restore()
    } else {
        val cx = cardRect.centerX()
        val cy = cardRect.centerY()

        val avatarRadius = (minOf(cardRect.width(), cardRect.height()) * 0.22f).coerceAtLeast(scaleFactor * 16f)
        val avatarTextColor = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }

        val maxTextWidth = cardRect.width() * 0.85f
        var fontSize = (cardRect.height() * 0.12f).coerceIn(scaleFactor * 12f, scaleFactor * 22f)
        namePaint.textSize = fontSize
        while (namePaint.measureText(contactConfig.contactName) > maxTextWidth && fontSize > scaleFactor * 10f) {
            fontSize -= scaleFactor * 0.8f
            namePaint.textSize = fontSize
        }

        val displayName = formatSmartName(contactConfig.contactName, namePaint, maxTextWidth)

        val avatarCy = cy - (cardRect.height() * 0.08f)
        drawAvatarNode(
            canvas, context, cx, avatarCy, avatarRadius,
            null, contactConfig.initials,
            accentColorInt, avatarTextColor
        )

        val nameY = avatarCy + avatarRadius + (fontSize * 1.2f)
        canvas.drawText(displayName, cx, nameY, namePaint)
    }

    return bitmap
}

// ============================================================================
// SHAPED CONTACT BITMAP GENERATORS
// ============================================================================

// Universal Shape Contact Generator (Widgets 8 - 22) - Forced Fixed 1:1 Mode
fun generateShapedContactBitmap(
    context: Context,
    config: SlateWidgetConfig,
    shape: LauncherShape,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val minDimension = (60 * scaleFactor).toInt()
    val w = (wDp * scaleFactor).toInt().coerceAtLeast(minDimension)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(minDimension)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE

    val r = Color.red(accentColorInt) / 255f
    val g = Color.green(accentColorInt) / 255f
    val b = Color.blue(accentColorInt) / 255f
    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b

    // Forced 1:1 Fixed Square Geometry
    val size = minOf(w, h).toFloat()
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    val shapePath = getShapePath(shape, cardRect, scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawPath(shapePath, bgPaint)

    val contactConfig = ContactsWidgetPreferences.loadConfig(context, widgetId)
    if (!contactConfig.isConfigured) {
        drawConfigurePlaceholderState(canvas, context, cardRect, config, scaleFactor)
        return bitmap
    }

    val photoBitmap = loadContactPhoto(context, contactConfig.photoUri)

    if (photoBitmap != null) {
        // --- 1. Clean Full-Bleed Shape Photo (No Text Overlay) ---
        canvas.save()
        canvas.clipPath(shapePath)

        val targetRatio = cardRect.width() / cardRect.height()
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
        canvas.restore()
    } else {
        // --- 2. Smart Fallback: Centered Avatar Node + Safe-Bounded Name ---
        val cx = cardRect.centerX()
        val cy = cardRect.centerY()

        val avatarRadius = (minOf(cardRect.width(), cardRect.height()) * 0.18f).coerceAtLeast(scaleFactor * 12f)
        val avatarTextColor = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

        // Position avatar node higher into the widest upper belly of shapes (star/heart/triangle)
        val avatarCy = cy - (cardRect.height() * 0.12f)

        drawAvatarNode(
            canvas, context, cx, avatarCy, avatarRadius,
            null, contactConfig.initials,
            accentColorInt, avatarTextColor
        )

        // Strict 52% max width constraint to prevent bottom text clipping across all shapes
        val maxTextWidth = cardRect.width() * 0.52f
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }

        var fontSize = (cardRect.height() * 0.11f).coerceIn(scaleFactor * 9f, scaleFactor * 16f)
        namePaint.textSize = fontSize
        while (namePaint.measureText(contactConfig.contactName) > maxTextWidth && fontSize > scaleFactor * 7f) {
            fontSize -= scaleFactor * 0.6f
            namePaint.textSize = fontSize
        }

        val displayName = formatSmartName(contactConfig.contactName, namePaint, maxTextWidth)
        val nameY = avatarCy + avatarRadius + (fontSize * 1.15f)

        canvas.drawText(displayName, cx, nameY, namePaint)
    }

    return bitmap
}

// 8. SQUIRCLE CONTACT
fun generateSquircleContactBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int) =
    generateShapedContactBitmap(context, config, LauncherShape.SQUIRCLE, wDp, hDp, widgetId)

// 9. CIRCLE CONTACT
fun generateCircleContactBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int) =
    generateShapedContactBitmap(context, config, LauncherShape.CIRCLE, wDp, hDp, widgetId)

// 10. PENTAGON CONTACT
fun generatePentagonContactBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int) =
    generateShapedContactBitmap(context, config, LauncherShape.M3_PENTAGON, wDp, hDp, widgetId)

// 11. OCTAGON CONTACT
fun generateOctagonContactBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int) =
    generateShapedContactBitmap(context, config, LauncherShape.M3_OCTAGON, wDp, hDp, widgetId)

// 12. DIAMOND CONTACT
fun generateDiamondContactBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int) =
    generateShapedContactBitmap(context, config, LauncherShape.M3_DIAMOND, wDp, hDp, widgetId)

// 13. FLOWER CONTACT
fun generateFlowerContactBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int) =
    generateShapedContactBitmap(context, config, LauncherShape.M3_FLOWER, wDp, hDp, widgetId)

// 14. CLOVER CONTACT
fun generateCloverContactBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int) =
    generateShapedContactBitmap(context, config, LauncherShape.M3_CLOVER, wDp, hDp, widgetId)

// 15. BLOB BOTTOM RIGHT CONTACT
fun generateBlobBottomRightContactBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int) =
    generateShapedContactBitmap(context, config, LauncherShape.BLOB_BOTTOM_RIGHT, wDp, hDp, widgetId)

// 16. BLOB BOTTOM LEFT CONTACT
fun generateBlobBottomLeftContactBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int) =
    generateShapedContactBitmap(context, config, LauncherShape.BLOB_BOTTOM_LEFT, wDp, hDp, widgetId)

// 17. BLOB TOP RIGHT CONTACT
fun generateBlobTopRightContactBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int) =
    generateShapedContactBitmap(context, config, LauncherShape.BLOB_TOP_RIGHT, wDp, hDp, widgetId)

// 18. BLOB TOP LEFT CONTACT
fun generateBlobTopLeftContactBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int) =
    generateShapedContactBitmap(context, config, LauncherShape.BLOB_TOP_LEFT, wDp, hDp, widgetId)

// 19. PIXEL STAR CONTACT
fun generatePixelStarContactBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int) =
    generateShapedContactBitmap(context, config, LauncherShape.PIXEL_STAR, wDp, hDp, widgetId)

// 20. STAR 5 CONTACT
fun generateStar5ContactBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int) =
    generateShapedContactBitmap(context, config, LauncherShape.STAR_5, wDp, hDp, widgetId)

// 21. HEART CONTACT
fun generateHeartContactBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int) =
    generateShapedContactBitmap(context, config, LauncherShape.HEART, wDp, hDp, widgetId)

// 22. TRIANGLE CONTACT
fun generateTriangleContactBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int) =
    generateShapedContactBitmap(context, config, LauncherShape.TRIANGLE, wDp, hDp, widgetId)

// Universal Multi-Contact Grid Generator (Widgets 23 - 27) - Bento Tile Grid Layout
fun generateMultiContactGridBitmap(
    context: Context,
    config: SlateWidgetConfig,
    slotCount: Int,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val displayDensity = context.resources.displayMetrics.density
    val scaleFactor = maxOf(displayDensity, 3.5f)

    val minDimension = (60 * scaleFactor).toInt()
    val w = (wDp * scaleFactor).toInt().coerceAtLeast(minDimension)
    val h = (hDp * scaleFactor).toInt().coerceAtLeast(minDimension)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    val r = Color.red(accentColorInt) / 255f
    val g = Color.green(accentColorInt) / 255f
    val b = Color.blue(accentColorInt) / 255f
    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b

    // Target aspect ratios for Fixed Mode
    val targetRatio = when (slotCount) {
        2 -> 2.0f
        3 -> 3.0f
        4 -> 1.0f
        6 -> 1.5f
        8 -> 2.0f
        else -> 1.0f
    }

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
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

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()

    // Outer Container Background
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val (cols, rows) = when (slotCount) {
        2 -> 2 to 1
        3 -> 3 to 1
        4 -> 2 to 2
        6 -> 3 to 2
        8 -> 4 to 2
        else -> 2 to 2
    }

    // Grid Inner Tile Dimensions & Padding
    val pad = scaleFactor * 6f
    val gap = scaleFactor * 6f

    val availableW = cardRect.width() - (pad * 2f) - (gap * (cols - 1))
    val availableH = cardRect.height() - (pad * 2f) - (gap * (rows - 1))

    val tileW = availableW / cols
    val tileH = availableH / rows

    val innerCardRadius = (outerRadius - pad).coerceAtLeast(scaleFactor * 6f)
    val innerCardBg = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    for (i in 0 until slotCount) {
        val col = i % cols
        val row = i / cols

        val tileLeft = cardRect.left + pad + col * (tileW + gap)
        val tileTop = cardRect.top + pad + row * (tileH + gap)
        val tileRect = RectF(tileLeft, tileTop, tileLeft + tileW, tileTop + tileH)

        val slotConfig = loadSlotConfig(context, widgetId, i)
        val photoBitmap = if (slotConfig.isConfigured) loadContactPhoto(context, slotConfig.photoUri) else null

        canvas.save()
        val tilePath = Path().apply {
            addRoundRect(tileRect, innerCardRadius, innerCardRadius, Path.Direction.CW)
        }
        canvas.clipPath(tilePath)

        if (!slotConfig.isConfigured) {
            // --- 1. Unconfigured Responsive Tile State ---
            canvas.drawRoundRect(tileRect, innerCardRadius, innerCardRadius, tilePaint)

            val maxTextWidth = tileW * 0.88f
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = secondaryText
                typeface = getSlateFont(context, weight = 600)
                textAlign = Paint.Align.CENTER
            }

            val singleLineText = "Tap to configure"
            var singleLineFontSize = (tileH * 0.12f).coerceIn(scaleFactor * 8f, scaleFactor * 13f)
            textPaint.textSize = singleLineFontSize

            if (textPaint.measureText(singleLineText) <= maxTextWidth) {
                // Render Single Line
                val iconSize = (minOf(tileW, tileH) * 0.28f).coerceAtLeast(scaleFactor * 12f)
                val iconCy = tileRect.centerY() - (tileH * 0.08f)
                drawVectorIcon(canvas, context, R.drawable.ic_person, tileRect.centerX(), iconCy, iconSize, Color.GRAY)

                val textY = tileRect.bottom - (scaleFactor * 8f)
                canvas.drawText(singleLineText, tileRect.centerX(), textY, textPaint)
            } else {
                // Responsive Multi-Line Fallback ("Tap to" / "configure")
                val line1 = "Tap to"
                val line2 = "configure"

                var multiFontSize = (tileH * 0.11f).coerceIn(scaleFactor * 7.5f, scaleFactor * 11f)
                textPaint.textSize = multiFontSize

                while ((textPaint.measureText(line1) > maxTextWidth || textPaint.measureText(line2) > maxTextWidth) && multiFontSize > scaleFactor * 6f) {
                    multiFontSize -= scaleFactor * 0.5f
                    textPaint.textSize = multiFontSize
                }

                val lineGap = multiFontSize * 1.15f
                val textY2 = tileRect.bottom - (scaleFactor * 6f)
                val textY1 = textY2 - lineGap

                val iconSize = (minOf(tileW, tileH) * 0.24f).coerceAtLeast(scaleFactor * 10f)
                val iconCy = tileRect.top + (tileH * 0.35f)
                drawVectorIcon(canvas, context, R.drawable.ic_person, tileRect.centerX(), iconCy, iconSize, Color.GRAY)

                canvas.drawText(line1, tileRect.centerX(), textY1, textPaint)
                canvas.drawText(line2, tileRect.centerX(), textY2, textPaint)
            }

        } else if (photoBitmap != null) {
            // --- 2. Full-Bleed Photo Tile with Overlaid Name ---
            val targetRatio = tileRect.width() / tileRect.height()
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
            canvas.drawBitmap(photoBitmap, srcRect, tileRect, imagePaint)

            val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.LinearGradient(
                    0f, tileRect.centerY(), 0f, tileRect.bottom,
                    Color.TRANSPARENT, Color.argb(190, 0, 0, 0),
                    android.graphics.Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(tileRect, gradientPaint)

            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = getSlateFont(context, weight = 700)
                textAlign = Paint.Align.CENTER
            }
            val maxTextWidth = tileW * 0.88f
            var fontSize = (tileH * 0.15f).coerceIn(scaleFactor * 9f, scaleFactor * 16f)
            namePaint.textSize = fontSize
            while (namePaint.measureText(slotConfig.contactName) > maxTextWidth && fontSize > scaleFactor * 7f) {
                fontSize -= scaleFactor * 0.6f
                namePaint.textSize = fontSize
            }

            val displayName = formatSmartName(slotConfig.contactName, namePaint, maxTextWidth)
            val textY = tileRect.bottom - (scaleFactor * 8f)
            canvas.drawText(displayName, tileRect.centerX(), textY, namePaint)

        } else {
            // --- 3. Fallback Initials Avatar Tile ---
            canvas.drawRoundRect(tileRect, innerCardRadius, innerCardRadius, tilePaint)

            val avatarRadius = (minOf(tileW, tileH) * 0.24f).coerceAtLeast(scaleFactor * 10f)
            val avatarCy = tileRect.centerY() - (tileH * 0.08f)
            val avatarTextColor = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE

            drawAvatarNode(
                canvas, context, tileRect.centerX(), avatarCy, avatarRadius,
                null, slotConfig.initials,
                accentColorInt, avatarTextColor
            )

            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryText
                typeface = getSlateFont(context, weight = 700)
                textAlign = Paint.Align.CENTER
            }
            val maxTextWidth = tileW * 0.88f
            var fontSize = (tileH * 0.15f).coerceIn(scaleFactor * 9f, scaleFactor * 16f)
            namePaint.textSize = fontSize
            while (namePaint.measureText(slotConfig.contactName) > maxTextWidth && fontSize > scaleFactor * 7f) {
                fontSize -= scaleFactor * 0.6f
                namePaint.textSize = fontSize
            }

            val displayName = formatSmartName(slotConfig.contactName, namePaint, maxTextWidth)
            val textY = tileRect.bottom - (scaleFactor * 8f)
            canvas.drawText(displayName, tileRect.centerX(), textY, namePaint)
        }

        canvas.restore()
    }

    return bitmap
}

// 23. 2-CONTACT GRID
fun generateGrid2ContactBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int) =
    generateMultiContactGridBitmap(context, config, 2, isResponsive, wDp, hDp, widgetId)

// 24. 3-CONTACT GRID
fun generateGrid3ContactBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int) =
    generateMultiContactGridBitmap(context, config, 3, isResponsive, wDp, hDp, widgetId)

// 25. 4-CONTACT GRID
fun generateGrid4ContactBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int) =
    generateMultiContactGridBitmap(context, config, 4, isResponsive, wDp, hDp, widgetId)

// 26. 6-CONTACT GRID
fun generateGrid6ContactBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int) =
    generateMultiContactGridBitmap(context, config, 6, isResponsive, wDp, hDp, widgetId)

// 27. 8-CONTACT GRID
fun generateGrid8ContactBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int) =
    generateMultiContactGridBitmap(context, config, 8, isResponsive, wDp, hDp, widgetId)
