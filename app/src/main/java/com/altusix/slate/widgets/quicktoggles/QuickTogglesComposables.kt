package com.altusix.slate.widgets.quicktoggles

import android.content.Context
import android.graphics.*
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.altusix.slate.R
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.createSupersampledCanvas
import com.altusix.slate.utils.getSafeBgColor
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius

// =========================================================================
// CANVAS BITMAP GENERATORS FOR SLATE "QUICK TOGGLES"
// =========================================================================

// -------------------------------------------------------------------------
// PRIVATE VECTOR & DRAWING HELPERS
// -------------------------------------------------------------------------

private fun drawCardBackground(
    canvas: Canvas,
    rect: RectF,
    config: SlateWidgetConfig,
    scaleFactor: Float,
    customCornerRadius: Float? = null
): Triple<Int, Int, Int> {
    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val cornerRadius = customCornerRadius ?: getStandardCornerRadius(scaleFactor)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(28, 0, 0, 0) else Color.argb(32, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * scaleFactor
    }
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

    val primaryTextColor = if (isLight) Color.BLACK else Color.WHITE
    val secondaryTextColor = if (isLight) Color.argb(170, 0, 0, 0) else Color.argb(170, 255, 255, 255)
    return Triple(primaryTextColor, secondaryTextColor, bgColor)
}

/**
 * Creates a rounded rectangle path with individual corner radii.
 */
private fun createCornerPath(
    rect: RectF,
    tl: Float,
    tr: Float,
    br: Float,
    bl: Float
): Path {
    val path = Path()
    val radii = floatArrayOf(tl, tl, tr, tr, br, br, bl, bl)
    path.addRoundRect(rect, radii, Path.Direction.CW)
    return path
}

/**
 * Draws an Android Vector Drawable scaled to [size] centered at ([cx], [cy]) with tint [color].
 */
private fun drawVectorDrawable(
    context: Context,
    canvas: Canvas,
    @DrawableRes resId: Int,
    cx: Float,
    cy: Float,
    size: Float,
    color: Int
) {
    val drawable = ContextCompat.getDrawable(context, resId)?.mutate() ?: return
    drawable.setTint(color)
    val half = size / 2f
    drawable.setBounds(
        (cx - half).toInt(),
        (cy - half).toInt(),
        (cx + half).toInt(),
        (cy + half).toInt()
    )
    drawable.draw(canvas)
}

/**
 * Centralized Icon Mapper for all Toggle Types using the project's vector drawables.
 */
private fun drawToggleIcon(
    context: Context,
    canvas: Canvas,
    type: ToggleType,
    cx: Float,
    cy: Float,
    size: Float,
    color: Int,
    isEnabled: Boolean,
    alertMode: AlertSliderMode? = null
) {
    val resId = when (type) {
        ToggleType.WIFI -> R.drawable.ic_wifi
        ToggleType.BLUETOOTH -> R.drawable.ic_bluetooth
        ToggleType.TORCH -> R.drawable.ic_flashlight
        ToggleType.RINGER, ToggleType.ALERT_SLIDER -> {
            when (alertMode) {
                AlertSliderMode.SILENT -> R.drawable.ic_bell_off
                AlertSliderMode.VIBRATE -> R.drawable.ic_phone_vibrate
                else -> R.drawable.ic_bell
            }
        }
        ToggleType.AUTOROTATE -> if (isEnabled) R.drawable.ic_screen_rotation else R.drawable.ic_screen_rotation_lock
        ToggleType.HOTSPOT -> if (isEnabled) R.drawable.ic_wifi_tethering else R.drawable.ic_wifi_tethering_off
        ToggleType.LOCATION -> R.drawable.ic_map_pin
        ToggleType.DARK_MODE -> if (isEnabled) R.drawable.ic_dark_mode else R.drawable.ic_light_mode
        ToggleType.AIRPLANE -> R.drawable.ic_airplane
        ToggleType.TIMEOUT -> R.drawable.ic_phone_lock
        ToggleType.BATTERY_SAVER -> R.drawable.ic_battery_saver
    }
    drawVectorDrawable(context, canvas, resId, cx, cy, size, color)
}

// =========================================================================
// 1. CONTROL CENTER DECK (4x2 - Full Bento Dashboard)
// =========================================================================

fun generateControlCenterDeckBitmap(
    context: Context,
    state: QuickTogglesState,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

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

    val (primaryTextColor, secondaryTextColor, _) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"

    val outerRadius = getStandardCornerRadius(scaleFactor)

    // Equal padding on all 4 sides
    val pad = (minOf(cardRect.width(), cardRect.height()) * 0.05f).coerceIn(6f * scaleFactor, 12f * scaleFactor)
    val gap = (minOf(cardRect.width(), cardRect.height()) * 0.04f).coerceIn(5f * scaleFactor, 10f * scaleFactor)
    val contentRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.bottom - pad)

    // Responsive columns: Automatically switches to 2x4 in tall portrait widgets
    val isTallMode = isResponsive && (cardRect.width() / cardRect.height() < 1.15f)
    val cols = if (isTallMode) 2 else 4
    val rows = if (isTallMode) 4 else 2

    val cellW = (contentRect.width() - (gap * (cols - 1))) / cols
    val cellH = (contentRect.height() - (gap * (rows - 1))) / rows

    // Concentric corner radius matches the outer widget curve
    val defaultInnerR = (scaleFactor * 8f).coerceAtMost(minOf(cellW, cellH) * 0.22f)
    val outerCornerR = (outerRadius - pad)
        .coerceAtLeast(defaultInnerR)
        .coerceAtMost(minOf(cellW, cellH) * 0.48f)

    // Dynamic contrast colors for solid accent tiles
    val isAccentLight = (((accentColor shr 16 and 0xFF) * 0.2126f) +
            ((accentColor shr 8 and 0xFF) * 0.7152f) +
            ((accentColor and 0xFF) * 0.0722f)) / 255f > 0.65f
    val activeContentColor = if (isAccentLight) Color.BLACK else Color.WHITE
    val activeSubColor = if (isAccentLight) Color.argb(185, 0, 0, 0) else Color.argb(200, 255, 255, 255)

    val toggles = listOf(
        state.wifi,
        state.bluetooth,
        state.torch,
        state.ringer,
        state.autoRotate,
        state.hotspot,
        state.location,
        state.darkMode
    )

    val fontRegular = getSlateFont(context, 500)
    val fontBold = getSlateFont(context, 700)

    val showSubtitle = cellH >= 46f * scaleFactor
    val showTitle = cellH >= 34f * scaleFactor

    val titleSize = (cellH * 0.17f).coerceIn(9.5f * scaleFactor, 12.5f * scaleFactor)
    val subSize = (cellH * 0.13f).coerceIn(8.0f * scaleFactor, 10.5f * scaleFactor)
    val iconSize = if (showSubtitle && showTitle) {
        (cellH * 0.36f).coerceIn(20f * scaleFactor, 30f * scaleFactor)
    } else if (showTitle) {
        (cellH * 0.44f).coerceIn(22f * scaleFactor, 36f * scaleFactor)
    } else {
        (minOf(cellW, cellH) * 0.55f).coerceIn(24f * scaleFactor, 42f * scaleFactor)
    }

    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * scaleFactor
    }

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = fontBold
        textSize = titleSize
        textAlign = Paint.Align.CENTER
    }

    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = fontRegular
        textSize = subSize
        textAlign = Paint.Align.CENTER
    }

    for (idx in toggles.indices) {
        val r = idx / cols
        val c = idx % cols
        val left = contentRect.left + c * (cellW + gap)
        val top = contentRect.top + r * (cellH + gap)
        val cellRect = RectF(left, top, left + cellW, top + cellH)

        val item = toggles[idx]
        val isEnabled = item.isEnabled

        // Outer-facing corners match the widget's outer border radius
        val tl = if (r == 0 && c == 0) outerCornerR else defaultInnerR
        val tr = if (r == 0 && c == cols - 1) outerCornerR else defaultInnerR
        val br = if (r == rows - 1 && c == cols - 1) outerCornerR else defaultInnerR
        val bl = if (r == rows - 1 && c == 0) outerCornerR else defaultInnerR
        val cellPath = createCornerPath(cellRect, tl, tr, br, bl)

        // Solid accent background when active
        if (isEnabled) {
            tilePaint.color = accentColor
            canvas.drawPath(cellPath, tilePaint)
        } else {
            tilePaint.color = if (isLight) Color.argb(16, 0, 0, 0) else Color.argb(22, 255, 255, 255)
            canvas.drawPath(cellPath, tilePaint)

            borderPaint.color = if (isLight) Color.argb(12, 0, 0, 0) else Color.argb(18, 255, 255, 255)
            canvas.drawPath(cellPath, borderPaint)
        }

        val cx = cellRect.centerX()
        val iconColor = if (isEnabled) activeContentColor else secondaryTextColor

        if (showSubtitle && showTitle) {
            val iconCy = cellRect.top + (cellH * 0.34f)
            drawToggleIcon(
                context = context,
                canvas = canvas,
                type = item.type,
                cx = cx,
                cy = iconCy,
                size = iconSize,
                color = iconColor,
                isEnabled = isEnabled,
                alertMode = if (item.type == ToggleType.RINGER) state.alertSlider else null
            )

            // Title
            titlePaint.color = if (isEnabled) activeContentColor else primaryTextColor
            val titleBaseline = iconCy + (iconSize / 2f) + (titleSize * 0.95f) + (2f * scaleFactor)
            val maxTextW = cellW - (10f * scaleFactor)
            val elidedTitle = android.text.TextUtils.ellipsize(
                item.label,
                android.text.TextPaint(titlePaint),
                maxTextW.coerceAtLeast(10f),
                android.text.TextUtils.TruncateAt.END
            ).toString()
            canvas.drawText(elidedTitle, cx, titleBaseline, titlePaint)

            // Subtitle
            subPaint.color = if (isEnabled) activeSubColor else secondaryTextColor
            val subBaseline = titleBaseline + subSize + (2.5f * scaleFactor)
            val elidedSub = android.text.TextUtils.ellipsize(
                item.subtitle,
                android.text.TextPaint(subPaint),
                maxTextW.coerceAtLeast(10f),
                android.text.TextUtils.TruncateAt.END
            ).toString()
            canvas.drawText(elidedSub, cx, subBaseline, subPaint)

        } else if (showTitle) {
            val iconCy = cellRect.top + (cellH * 0.40f)
            drawToggleIcon(
                context = context,
                canvas = canvas,
                type = item.type,
                cx = cx,
                cy = iconCy,
                size = iconSize,
                color = iconColor,
                isEnabled = isEnabled,
                alertMode = if (item.type == ToggleType.RINGER) state.alertSlider else null
            )

            titlePaint.color = if (isEnabled) activeContentColor else primaryTextColor
            val titleBaseline = cellRect.bottom - (cellH * 0.16f)
            val maxTextW = cellW - (10f * scaleFactor)
            val elidedTitle = android.text.TextUtils.ellipsize(
                item.label,
                android.text.TextPaint(titlePaint),
                maxTextW.coerceAtLeast(10f),
                android.text.TextUtils.TruncateAt.END
            ).toString()
            canvas.drawText(elidedTitle, cx, titleBaseline, titlePaint)

        } else {
            // Compact icon-only fallback
            drawToggleIcon(
                context = context,
                canvas = canvas,
                type = item.type,
                cx = cx,
                cy = cellRect.centerY(),
                size = iconSize,
                color = iconColor,
                isEnabled = isEnabled,
                alertMode = if (item.type == ToggleType.RINGER) state.alertSlider else null
            )
        }
    }

    return bitmap
}

// =========================================================================
// 2. MINIMALIST ACTION TOOLBAR (4x1 - 5 PILLS)
// =========================================================================

fun generateMinimalistToolbarBitmap(
    context: Context,
    state: QuickTogglesState,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val targetAspect = 4.0f
        val currentAspect = w / h
        if (currentAspect > targetAspect) {
            val contentW = h * targetAspect
            RectF((w - contentW) / 2f, 0f, (w + contentW) / 2f, h)
        } else {
            val contentH = w / targetAspect
            RectF(0f, (h - contentH) / 2f, w, (h + contentH) / 2f)
        }
    }

    val (_, secondaryTextColor, _) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val pad = (minOf(cardRect.width(), cardRect.height()) * 0.08f).coerceIn(6f * scaleFactor, 10f * scaleFactor)
    val gap = (minOf(cardRect.width(), cardRect.height()) * 0.06f).coerceIn(4f * scaleFactor, 8f * scaleFactor)
    val contentRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.bottom - pad)

    val isAccentLight = (((accentColor shr 16 and 0xFF) * 0.2126f) +
            ((accentColor shr 8 and 0xFF) * 0.7152f) +
            ((accentColor and 0xFF) * 0.0722f)) / 255f > 0.65f
    val activeContentColor = if (isAccentLight) Color.BLACK else Color.WHITE

    val items = listOf(state.wifi, state.bluetooth, state.torch, state.ringer, state.autoRotate)
    val count = items.size
    val pillW = (contentRect.width() - (gap * (count - 1))) / count
    val pillH = contentRect.height()

    // Balanced Bento corner radius: governed by width to prevent ballooning
    val minDim = minOf(pillW, pillH)
    val defaultInnerR = (minDim * 0.22f).coerceIn(6f * scaleFactor, 12f * scaleFactor)
    val outerCornerR = (outerRadius - pad).coerceIn(defaultInnerR, minDim * 0.38f)

    for (i in 0 until count) {
        val item = items[i]
        val isEnabled = item.isEnabled
        val left = contentRect.left + i * (pillW + gap)
        val pillRect = RectF(left, contentRect.top, left + pillW, contentRect.top + pillH)

        val tl = if (i == 0) outerCornerR else defaultInnerR
        val bl = if (i == 0) outerCornerR else defaultInnerR
        val tr = if (i == count - 1) outerCornerR else defaultInnerR
        val br = if (i == count - 1) outerCornerR else defaultInnerR

        val path = createCornerPath(pillRect, tl, tr, br, bl)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isEnabled) accentColor else (if (isLight) Color.argb(16, 0, 0, 0) else Color.argb(22, 255, 255, 255))
            style = Paint.Style.FILL
        }
        canvas.drawPath(path, bgPaint)

        if (!isEnabled) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isLight) Color.argb(12, 0, 0, 0) else Color.argb(18, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 1f * scaleFactor
            }
            canvas.drawPath(path, borderPaint)
        }

        val cx = pillRect.centerX()
        val cy = pillRect.centerY()
        val iconSize = (minDim * 0.44f).coerceIn(18f * scaleFactor, 26f * scaleFactor)
        val iconColor = if (isEnabled) activeContentColor else secondaryTextColor

        drawToggleIcon(
            context = context,
            canvas = canvas,
            type = item.type,
            cx = cx,
            cy = cy,
            size = iconSize,
            color = iconColor,
            isEnabled = isEnabled,
            alertMode = if (item.type == ToggleType.RINGER) state.alertSlider else null
        )
    }

    return bitmap
}

// =========================================================================
// 3. CONNECTIVITY DUO BENTO (2x2)
// =========================================================================

fun generateConnectivityBentoBitmap(
    context: Context,
    state: QuickTogglesState,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val (primaryTextColor, secondaryTextColor, _) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val pad = (minOf(cardRect.width(), cardRect.height()) * 0.05f).coerceIn(6f * scaleFactor, 12f * scaleFactor)
    val gap = (minOf(cardRect.width(), cardRect.height()) * 0.04f).coerceIn(5f * scaleFactor, 10f * scaleFactor)
    val contentRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.bottom - pad)

    val halfH = (contentRect.height() - gap) / 2f
    val topTileRect = RectF(contentRect.left, contentRect.top, contentRect.right, contentRect.top + halfH)
    val bottomTileRect = RectF(contentRect.left, topTileRect.bottom + gap, contentRect.right, contentRect.bottom)

    val defaultInnerR = (scaleFactor * 8f).coerceAtMost(halfH * 0.25f)
    val outerCornerR = (outerRadius - pad).coerceAtLeast(defaultInnerR).coerceAtMost(halfH * 0.48f)

    val isAccentLight = (((accentColor shr 16 and 0xFF) * 0.2126f) +
            ((accentColor shr 8 and 0xFF) * 0.7152f) +
            ((accentColor and 0xFF) * 0.0722f)) / 255f > 0.65f
    val activeContentColor = if (isAccentLight) Color.BLACK else Color.WHITE
    val activeSubColor = if (isAccentLight) Color.argb(185, 0, 0, 0) else Color.argb(200, 255, 255, 255)

    val fontRegular = getSlateFont(context, 500)
    val fontBold = getSlateFont(context, 700)
    val iconSize = topTileRect.height() * 0.42f

    // --- Top: Wi-Fi Card ---
    val topPath = createCornerPath(topTileRect, outerCornerR, outerCornerR, defaultInnerR, defaultInnerR)
    val topPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (state.wifi.isEnabled) accentColor else (if (isLight) Color.argb(16, 0, 0, 0) else Color.argb(22, 255, 255, 255))
        style = Paint.Style.FILL
    }
    canvas.drawPath(topPath, topPaint)

    val iconCx = topTileRect.left + topTileRect.height() * 0.44f
    val iconCy = topTileRect.centerY()
    drawVectorDrawable(context, canvas, R.drawable.ic_wifi, iconCx, iconCy, iconSize, if (state.wifi.isEnabled) activeContentColor else secondaryTextColor)

    val textX = iconCx + topTileRect.height() * 0.38f
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = if (state.wifi.isEnabled) activeContentColor else primaryTextColor
        typeface = fontBold
        textSize = 13.5f * scaleFactor
    }
    canvas.drawText("Wi-Fi", textX, topTileRect.centerY() - 2f * scaleFactor, titlePaint)

    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = if (state.wifi.isEnabled) activeSubColor else secondaryTextColor
        typeface = fontRegular
        textSize = 10.5f * scaleFactor
    }
    val wifiSub = if (state.wifi.subtitle.length > 14) state.wifi.subtitle.take(13) + "…" else state.wifi.subtitle
    canvas.drawText(wifiSub, textX, topTileRect.centerY() + 13f * scaleFactor, subPaint)

    // --- Bottom: Bluetooth Card ---
    val bottomPath = createCornerPath(bottomTileRect, defaultInnerR, defaultInnerR, outerCornerR, outerCornerR)
    val bottomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (state.bluetooth.isEnabled) accentColor else (if (isLight) Color.argb(16, 0, 0, 0) else Color.argb(22, 255, 255, 255))
        style = Paint.Style.FILL
    }
    canvas.drawPath(bottomPath, bottomPaint)

    val btIconCy = bottomTileRect.centerY()
    drawVectorDrawable(context, canvas, R.drawable.ic_bluetooth, iconCx, btIconCy, iconSize, if (state.bluetooth.isEnabled) activeContentColor else secondaryTextColor)

    titlePaint.color = if (state.bluetooth.isEnabled) activeContentColor else primaryTextColor
    canvas.drawText("Bluetooth", textX, bottomTileRect.centerY() - 2f * scaleFactor, titlePaint)

    subPaint.color = if (state.bluetooth.isEnabled) activeSubColor else secondaryTextColor
    val btSub = if (state.bluetooth.subtitle.length > 14) state.bluetooth.subtitle.take(13) + "…" else state.bluetooth.subtitle
    canvas.drawText(btSub, textX, bottomTileRect.centerY() + 13f * scaleFactor, subPaint)

    return bitmap
}

// =========================================================================
// 4. QUAD ACTION MATRIX (2x2)
// =========================================================================

fun generateQuadActionMatrixBitmap(
    context: Context,
    state: QuickTogglesState,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val (primaryTextColor, secondaryTextColor, _) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val pad = (minOf(cardRect.width(), cardRect.height()) * 0.05f).coerceIn(6f * scaleFactor, 12f * scaleFactor)
    val gap = (minOf(cardRect.width(), cardRect.height()) * 0.04f).coerceIn(5f * scaleFactor, 10f * scaleFactor)
    val contentRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.bottom - pad)

    val cellW = (contentRect.width() - gap) / 2f
    val cellH = (contentRect.height() - gap) / 2f

    val defaultInnerR = (scaleFactor * 8f).coerceAtMost(minOf(cellW, cellH) * 0.25f)
    val outerCornerR = (outerRadius - pad).coerceAtLeast(defaultInnerR).coerceAtMost(minOf(cellW, cellH) * 0.48f)

    val isAccentLight = (((accentColor shr 16 and 0xFF) * 0.2126f) +
            ((accentColor shr 8 and 0xFF) * 0.7152f) +
            ((accentColor and 0xFF) * 0.0722f)) / 255f > 0.65f
    val activeContentColor = if (isAccentLight) Color.BLACK else Color.WHITE

    val quads = listOf(state.wifi, state.bluetooth, state.torch, state.ringer)
    val fontBold = getSlateFont(context, 700)

    for (i in 0 until 4) {
        val r = i / 2
        val c = i % 2
        val left = contentRect.left + c * (cellW + gap)
        val top = contentRect.top + r * (cellH + gap)
        val rect = RectF(left, top, left + cellW, top + cellH)

        val item = quads[i]
        val isEnabled = item.isEnabled

        val tl = if (r == 0 && c == 0) outerCornerR else defaultInnerR
        val tr = if (r == 0 && c == 1) outerCornerR else defaultInnerR
        val br = if (r == 1 && c == 1) outerCornerR else defaultInnerR
        val bl = if (r == 1 && c == 0) outerCornerR else defaultInnerR

        val path = createCornerPath(rect, tl, tr, br, bl)

        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isEnabled) accentColor else (if (isLight) Color.argb(16, 0, 0, 0) else Color.argb(22, 255, 255, 255))
            style = Paint.Style.FILL
        }
        canvas.drawPath(path, p)

        val iconCx = rect.centerX()
        val iconCy = rect.centerY() - 8f * scaleFactor
        val iconSize = cellH * 0.42f
        val color = if (isEnabled) activeContentColor else secondaryTextColor

        drawToggleIcon(
            context = context,
            canvas = canvas,
            type = item.type,
            cx = iconCx,
            cy = iconCy,
            size = iconSize,
            color = color,
            isEnabled = isEnabled,
            alertMode = if (item.type == ToggleType.RINGER) state.alertSlider else null
        )

        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = if (isEnabled) activeContentColor else primaryTextColor
            typeface = fontBold
            textSize = 10.5f * scaleFactor
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(item.label, rect.centerX(), rect.bottom - 11f * scaleFactor, tp)
    }

    return bitmap
}

// =========================================================================
// 5. TACTILE ALERT SLIDER (HORIZONTAL 2x1)
// =========================================================================

fun generateAlertSliderHorizontalBitmap(
    context: Context,
    mode: AlertSliderMode,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val targetAspect = 2.0f
        val currentAspect = w / h
        if (currentAspect > targetAspect) {
            val contentW = h * targetAspect
            RectF((w - contentW) / 2f, 0f, (w + contentW) / 2f, h)
        } else {
            val contentH = w / targetAspect
            RectF(0f, (h - contentH) / 2f, w, (h + contentH) / 2f)
        }
    }

    val (_, secondaryTextColor, _) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()

    val isAccentLight = (((accentColor shr 16 and 0xFF) * 0.2126f) +
            ((accentColor shr 8 and 0xFF) * 0.7152f) +
            ((accentColor and 0xFF) * 0.0722f)) / 255f > 0.65f
    val activeContentColor = if (isAccentLight) Color.BLACK else Color.WHITE

    val fontBold = getSlateFont(context, 700)

    val pad = (minOf(cardRect.width(), cardRect.height()) * 0.07f).coerceIn(8f * scaleFactor, 16f * scaleFactor)
    val trackRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.bottom - pad)
    val trackRadius = trackRect.height() / 2f

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(32, 255, 255, 255)
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(trackRect, trackRadius, trackRadius, trackPaint)

    val notchCount = 3
    val notchW = trackRect.width() / notchCount.toFloat()
    val modes = listOf(AlertSliderMode.SILENT, AlertSliderMode.VIBRATE, AlertSliderMode.RING)
    val activeIdx = modes.indexOf(mode)

    val thumbPad = 4f * scaleFactor
    val thumbLeft = trackRect.left + activeIdx * notchW + thumbPad
    val thumbRight = trackRect.left + (activeIdx + 1) * notchW - thumbPad
    val thumbTop = trackRect.top + thumbPad
    val thumbBottom = trackRect.bottom - thumbPad
    val thumbRect = RectF(thumbLeft, thumbTop, thumbRight, thumbBottom)
    val thumbRadius = thumbRect.height() / 2f

    val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(thumbRect, thumbRadius, thumbRadius, thumbPaint)

    for (i in 0 until notchCount) {
        val nMode = modes[i]
        val nCx = trackRect.left + i * notchW + notchW / 2f
        val isCurrent = (i == activeIdx)

        val iconSize = thumbRect.height() * 0.40f
        val iconColor = if (isCurrent) activeContentColor else secondaryTextColor

        val resId = when (nMode) {
            AlertSliderMode.SILENT -> R.drawable.ic_bell_off
            AlertSliderMode.VIBRATE -> R.drawable.ic_phone_vibrate
            AlertSliderMode.RING -> R.drawable.ic_bell
        }
        drawVectorDrawable(context, canvas, resId, nCx, trackRect.centerY() - 6f * scaleFactor, iconSize, iconColor)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = if (isCurrent) activeContentColor else secondaryTextColor
            typeface = fontBold
            textSize = 9.5f * scaleFactor
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(nMode.label, nCx, trackRect.centerY() + 14f * scaleFactor, labelPaint)
    }

    return bitmap
}

// =========================================================================
// 6. VERTICAL ALERT SLIDER (1x2)
// =========================================================================

fun generateAlertSliderVerticalBitmap(
    context: Context,
    mode: AlertSliderMode,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val targetAspect = 0.5f
        val currentAspect = w / h
        if (currentAspect > targetAspect) {
            val contentW = h * targetAspect
            RectF((w - contentW) / 2f, 0f, (w + contentW) / 2f, h)
        } else {
            val contentH = w / targetAspect
            RectF(0f, (h - contentH) / 2f, w, (h + contentH) / 2f)
        }
    }

    val (_, secondaryTextColor, _) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()

    val isAccentLight = (((accentColor shr 16 and 0xFF) * 0.2126f) +
            ((accentColor shr 8 and 0xFF) * 0.7152f) +
            ((accentColor and 0xFF) * 0.0722f)) / 255f > 0.65f
    val activeContentColor = if (isAccentLight) Color.BLACK else Color.WHITE

    val fontBold = getSlateFont(context, 700)

    val pad = (minOf(cardRect.width(), cardRect.height()) * 0.08f).coerceIn(8f * scaleFactor, 14f * scaleFactor)
    val trackRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.bottom - pad)
    val trackRadius = trackRect.width() / 2f

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(32, 255, 255, 255)
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(trackRect, trackRadius, trackRadius, trackPaint)

    val notchCount = 3
    val notchH = trackRect.height() / notchCount.toFloat()
    val modes = listOf(AlertSliderMode.RING, AlertSliderMode.VIBRATE, AlertSliderMode.SILENT)
    val activeIdx = modes.indexOf(mode)

    val thumbPad = 4f * scaleFactor
    val thumbTop = trackRect.top + activeIdx * notchH + thumbPad
    val thumbBottom = trackRect.top + (activeIdx + 1) * notchH - thumbPad
    val thumbLeft = trackRect.left + thumbPad
    val thumbRight = trackRect.right - thumbPad
    val thumbRect = RectF(thumbLeft, thumbTop, thumbRight, thumbBottom)
    val thumbRadius = thumbRect.width() / 2f

    val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(thumbRect, thumbRadius, thumbRadius, thumbPaint)

    for (i in 0 until notchCount) {
        val nMode = modes[i]
        val nCy = trackRect.top + i * notchH + notchH / 2f
        val isCurrent = (i == activeIdx)

        val iconSize = thumbRect.width() * 0.42f
        val iconColor = if (isCurrent) activeContentColor else secondaryTextColor

        val resId = when (nMode) {
            AlertSliderMode.RING -> R.drawable.ic_bell
            AlertSliderMode.VIBRATE -> R.drawable.ic_phone_vibrate
            AlertSliderMode.SILENT -> R.drawable.ic_bell_off
        }
        drawVectorDrawable(context, canvas, resId, trackRect.centerX(), nCy - 4f * scaleFactor, iconSize, iconColor)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = if (isCurrent) activeContentColor else secondaryTextColor
            typeface = fontBold
            textSize = 8.5f * scaleFactor
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(nMode.label, trackRect.centerX(), nCy + 13f * scaleFactor, labelPaint)
    }

    return bitmap
}

// =========================================================================
// 7. FLASHLIGHT TORCH SWITCH (2x2)
// =========================================================================

fun generateFlashlightTorchBitmap(
    context: Context,
    isTorchOn: Boolean,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val (primaryTextColor, secondaryTextColor, _) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val fontBold = getSlateFont(context, 700)

    val isAccentLight = (((accentColor shr 16 and 0xFF) * 0.2126f) +
            ((accentColor shr 8 and 0xFF) * 0.7152f) +
            ((accentColor and 0xFF) * 0.0722f)) / 255f > 0.65f
    val activeContentColor = if (isAccentLight) Color.BLACK else Color.WHITE

    val cx = cardRect.centerX()
    val cy = cardRect.centerY() - 10f * scaleFactor
    val btnRadius = minOf(cardRect.width(), cardRect.height()) * 0.28f

    if (isTorchOn) {
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, btnRadius * 1.8f,
                intArrayOf(Color.argb(90, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)), Color.TRANSPARENT),
                floatArrayOf(0.4f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, btnRadius * 1.8f, glowPaint)
    }

    val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isTorchOn) accentColor else Color.argb(30, 255, 255, 255)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, btnRadius, btnPaint)

    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isTorchOn) Color.argb(180, 255, 255, 255) else Color.argb(60, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * scaleFactor
    }
    canvas.drawCircle(cx, cy, btnRadius, ringPaint)

    val iconSize = btnRadius * 0.95f
    val iconColor = if (isTorchOn) activeContentColor else secondaryTextColor
    drawVectorDrawable(context, canvas, R.drawable.ic_flashlight, cx, cy, iconSize, iconColor)

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = primaryTextColor
        typeface = fontBold
        textSize = 14f * scaleFactor
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("FLASHLIGHT", cx, cardRect.bottom - 24f * scaleFactor, labelPaint)

    val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = if (isTorchOn) accentColor else secondaryTextColor
        typeface = fontBold
        textSize = 11f * scaleFactor
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(if (isTorchOn) "ON" else "OFF", cx, cardRect.bottom - 10f * scaleFactor, statusPaint)

    return bitmap
}

// =========================================================================
// 8. GENERIC TOGGLE PILL (2x1 - WI-FI, BLUETOOTH, SOUND, ROTATE)
// =========================================================================

fun generateTogglePillBitmap(
    context: Context,
    item: ToggleItemState,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val targetAspect = 2.0f
        val currentAspect = w / h
        if (currentAspect > targetAspect) {
            val contentW = h * targetAspect
            RectF((w - contentW) / 2f, 0f, (w + contentW) / 2f, h)
        } else {
            val contentH = w / targetAspect
            RectF(0f, (h - contentH) / 2f, w, (h + contentH) / 2f)
        }
    }

    val (primaryTextColor, secondaryTextColor, _) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()

    val isAccentLight = (((accentColor shr 16 and 0xFF) * 0.2126f) +
            ((accentColor shr 8 and 0xFF) * 0.7152f) +
            ((accentColor and 0xFF) * 0.0722f)) / 255f > 0.65f
    val activeContentColor = if (isAccentLight) Color.BLACK else Color.WHITE

    val fontRegular = getSlateFont(context, 400)
    val fontBold = getSlateFont(context, 700)

    val padH = 14f * scaleFactor
    val iconR = cardRect.height() * 0.28f
    val iconCx = cardRect.left + padH + iconR
    val iconCy = cardRect.centerY()

    // Full solid accent color on active badge
    val badgeBg = if (item.isEnabled) accentColor else Color.argb(22, 255, 255, 255)
    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = badgeBg }
    canvas.drawCircle(iconCx, iconCy, iconR, badgePaint)

    val iconSize = iconR * 1.18f
    val iconColor = if (item.isEnabled) activeContentColor else secondaryTextColor

    drawToggleIcon(
        context = context,
        canvas = canvas,
        type = item.type,
        cx = iconCx,
        cy = iconCy,
        size = iconSize,
        color = iconColor,
        isEnabled = item.isEnabled,
        alertMode = AlertSliderMode.RING
    )

    val textLeft = iconCx + iconR + 12f * scaleFactor
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = primaryTextColor
        typeface = fontBold
        textSize = 14.5f * scaleFactor
    }
    canvas.drawText(item.label, textLeft, cardRect.centerY() - 2f * scaleFactor, titlePaint)

    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = if (item.isEnabled) accentColor else secondaryTextColor
        typeface = fontRegular
        textSize = 11f * scaleFactor
    }
    val sub = if (item.subtitle.length > 18) item.subtitle.take(17) + "…" else item.subtitle
    canvas.drawText(sub, textLeft, cardRect.centerY() + 14f * scaleFactor, subPaint)

    return bitmap
}

// =========================================================================
// 9. SYSTEM UTILITY DECK (4x2 - 6 TILES + STATS)
// =========================================================================

fun generateSystemUtilityDeckBitmap(
    context: Context,
    state: QuickTogglesState,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val targetAspect = 2.0f
        val currentAspect = w / h
        if (currentAspect > targetAspect) {
            val contentW = h * targetAspect
            RectF((w - contentW) / 2f, 0f, (w + contentW) / 2f, h)
        } else {
            val contentH = w / targetAspect
            RectF(0f, (h - contentH) / 2f, w, (h + contentH) / 2f)
        }
    }

    val (primaryTextColor, secondaryTextColor, _) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val pad = (minOf(cardRect.width(), cardRect.height()) * 0.05f).coerceIn(6f * scaleFactor, 12f * scaleFactor)
    val gap = (minOf(cardRect.width(), cardRect.height()) * 0.04f).coerceIn(5f * scaleFactor, 10f * scaleFactor)
    val contentRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.bottom - pad)

    val cols = 3
    val rows = 2
    val cellW = (contentRect.width() - (gap * (cols - 1))) / cols
    val cellH = (contentRect.height() - (gap * (rows - 1))) / rows

    val defaultInnerR = (scaleFactor * 8f).coerceAtMost(minOf(cellW, cellH) * 0.22f)
    val outerCornerR = (outerRadius - pad).coerceAtLeast(defaultInnerR).coerceAtMost(minOf(cellW, cellH) * 0.48f)

    val isAccentLight = (((accentColor shr 16 and 0xFF) * 0.2126f) +
            ((accentColor shr 8 and 0xFF) * 0.7152f) +
            ((accentColor and 0xFF) * 0.0722f)) / 255f > 0.65f
    val activeContentColor = if (isAccentLight) Color.BLACK else Color.WHITE
    val activeSubColor = if (isAccentLight) Color.argb(185, 0, 0, 0) else Color.argb(200, 255, 255, 255)

    val utilities = listOf(
        state.timeout,
        state.autoRotate,
        state.batterySaver,
        state.darkMode,
        state.airplane,
        state.hotspot
    )

    val fontRegular = getSlateFont(context, 400)
    val fontBold = getSlateFont(context, 700)

    for (i in utilities.indices) {
        val r = i / cols
        val c = i % cols
        val left = contentRect.left + c * (cellW + gap)
        val top = contentRect.top + r * (cellH + gap)
        val rect = RectF(left, top, left + cellW, top + cellH)

        val item = utilities[i]
        val isEnabled = item.isEnabled

        val tl = if (r == 0 && c == 0) outerCornerR else defaultInnerR
        val tr = if (r == 0 && c == cols - 1) outerCornerR else defaultInnerR
        val br = if (r == rows - 1 && c == cols - 1) outerCornerR else defaultInnerR
        val bl = if (r == rows - 1 && c == 0) outerCornerR else defaultInnerR
        val path = createCornerPath(rect, tl, tr, br, bl)

        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isEnabled) accentColor else (if (isLight) Color.argb(16, 0, 0, 0) else Color.argb(22, 255, 255, 255))
            style = Paint.Style.FILL
        }
        canvas.drawPath(path, p)

        val iconCx = rect.left + rect.height() * 0.38f
        val iconCy = rect.centerY()
        val iconSize = rect.height() * 0.42f
        val iconColor = if (isEnabled) activeContentColor else secondaryTextColor

        drawToggleIcon(
            context = context,
            canvas = canvas,
            type = item.type,
            cx = iconCx,
            cy = iconCy,
            size = iconSize,
            color = iconColor,
            isEnabled = isEnabled
        )

        val textLeft = iconCx + rect.height() * 0.35f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = if (isEnabled) activeContentColor else primaryTextColor
            typeface = fontBold
            textSize = 11.5f * scaleFactor
        }
        canvas.drawText(item.label, textLeft, rect.centerY() - 2f * scaleFactor, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = if (isEnabled) activeSubColor else secondaryTextColor
            typeface = fontRegular
            textSize = 9.5f * scaleFactor
        }
        val sub = if (item.subtitle.length > 10) item.subtitle.take(9) + "…" else item.subtitle
        canvas.drawText(sub, textLeft, rect.centerY() + 11f * scaleFactor, subPaint)
    }

    return bitmap
}

// =========================================================================
// 10. MICRO TOGGLE (1x1 - TORCH OR SOUND)
// =========================================================================

fun generateMicroToggleBitmap(
    context: Context,
    item: ToggleItemState,
    alertMode: AlertSliderMode?,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardRect = if (isResponsive) RectF(0f, 0f, w, h) else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val (primaryTextColor, secondaryTextColor, _) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val fontBold = getSlateFont(context, 700)

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val pad = (minOf(cardRect.width(), cardRect.height()) * 0.08f).coerceIn(6f * scaleFactor, 10f * scaleFactor)
    val innerRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.bottom - pad)
    val innerR = (outerRadius - pad).coerceAtLeast(8f * scaleFactor)

    val isAccentLight = (((accentColor shr 16 and 0xFF) * 0.2126f) +
            ((accentColor shr 8 and 0xFF) * 0.7152f) +
            ((accentColor and 0xFF) * 0.0722f)) / 255f > 0.65f
    val activeContentColor = if (isAccentLight) Color.BLACK else Color.WHITE

    // Solid accent card when active (matches reference 1x1 toggle widget)
    if (item.isEnabled) {
        val solidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(innerRect, innerR, innerR, solidPaint)
    }

    val cx = cardRect.centerX()
    val cy = cardRect.centerY() - 7f * scaleFactor
    val iconSize = cardRect.width() * 0.42f
    val iconColor = if (item.isEnabled) activeContentColor else secondaryTextColor

    drawToggleIcon(
        context = context,
        canvas = canvas,
        type = item.type,
        cx = cx,
        cy = cy,
        size = iconSize,
        color = iconColor,
        isEnabled = item.isEnabled,
        alertMode = alertMode
    )

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = if (item.isEnabled) activeContentColor else secondaryTextColor
        typeface = fontBold
        textSize = 10f * scaleFactor
        textAlign = Paint.Align.CENTER
    }
    val label = if (item.type == ToggleType.RINGER) (alertMode?.label ?: "Sound") else item.label
    canvas.drawText(label, cx, cardRect.bottom - 10f * scaleFactor, labelPaint)

    return bitmap
}