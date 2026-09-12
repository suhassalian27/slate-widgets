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
// 1. CONTROL CENTER DECK (4x2 - 8 TILES)
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

    val paddingH = 12f * scaleFactor
    val paddingV = 10f * scaleFactor
    val contentRect = RectF(cardRect.left + paddingH, cardRect.top + paddingV, cardRect.right - paddingH, cardRect.bottom - paddingV)

    val cols = 4
    val rows = 2
    val gap = 7f * scaleFactor
    val cellW = (contentRect.width() - (gap * (cols - 1))) / cols
    val cellH = (contentRect.height() - (gap * (rows - 1))) / rows
    val cellRadius = 12f * scaleFactor

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

    val fontRegular = getSlateFont(context, 400)
    val fontBold = getSlateFont(context, 700)

    for (idx in toggles.indices) {
        val r = idx / cols
        val c = idx % cols
        val left = contentRect.left + c * (cellW + gap)
        val top = contentRect.top + r * (cellH + gap)
        val cellRect = RectF(left, top, left + cellW, top + cellH)

        val item = toggles[idx]
        val isEnabled = item.isEnabled

        // Cell background
        val tileBgColor = if (isEnabled) {
            Color.argb(42, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        } else {
            Color.argb(22, Color.red(secondaryTextColor), Color.green(secondaryTextColor), Color.blue(secondaryTextColor))
        }
        val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = tileBgColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cellRect, cellRadius, cellRadius, tilePaint)

        // Active border
        if (isEnabled) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.argb(90, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
                style = Paint.Style.STROKE
                strokeWidth = 1.2f * scaleFactor
            }
            canvas.drawRoundRect(cellRect, cellRadius, cellRadius, borderPaint)
        }

        // Draw Icon
        val iconSize = minOf(cellW * 0.38f, cellH * 0.44f)
        val iconCx = cellRect.left + cellW * 0.28f
        val iconCy = cellRect.centerY()
        val iconColor = if (isEnabled) accentColor else secondaryTextColor

        drawToggleIcon(
            context = context,
            canvas = canvas,
            type = item.type,
            cx = iconCx,
            cy = iconCy,
            size = iconSize,
            color = iconColor,
            isEnabled = isEnabled,
            alertMode = if (item.type == ToggleType.RINGER) state.alertSlider else null
        )

        // Draw Texts (Label + Subtitle)
        val textLeft = cellRect.left + cellW * 0.52f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = if (isEnabled) primaryTextColor else secondaryTextColor
            typeface = fontBold
            textSize = 10.5f * scaleFactor
        }
        canvas.drawText(item.label, textLeft, cellRect.centerY() - 1f * scaleFactor, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = if (isEnabled) accentColor else Color.argb(120, Color.red(secondaryTextColor), Color.green(secondaryTextColor), Color.blue(secondaryTextColor))
            typeface = fontRegular
            textSize = 8.5f * scaleFactor
        }
        val subText = if (item.subtitle.length > 8) item.subtitle.take(7) + "…" else item.subtitle
        canvas.drawText(subText, textLeft, cellRect.centerY() + 10f * scaleFactor, subPaint)
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

    val paddingH = 10f * scaleFactor
    val paddingV = 6f * scaleFactor
    val contentRect = RectF(cardRect.left + paddingH, cardRect.top + paddingV, cardRect.right - paddingH, cardRect.bottom - paddingV)

    val items = listOf(state.wifi, state.bluetooth, state.torch, state.ringer, state.autoRotate)
    val count = items.size
    val gap = 8f * scaleFactor
    val pillW = (contentRect.width() - (gap * (count - 1))) / count
    val pillH = contentRect.height()
    val pillRadius = pillH * 0.40f

    for (i in 0 until count) {
        val item = items[i]
        val isEnabled = item.isEnabled
        val left = contentRect.left + i * (pillW + gap)
        val pillRect = RectF(left, contentRect.top, left + pillW, contentRect.top + pillH)

        val pillBg = if (isEnabled) {
            Color.argb(45, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        } else {
            Color.argb(20, Color.red(secondaryTextColor), Color.green(secondaryTextColor), Color.blue(secondaryTextColor))
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = pillBg
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(pillRect, pillRadius, pillRadius, bgPaint)

        if (isEnabled) {
            val bPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.argb(90, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
                style = Paint.Style.STROKE
                strokeWidth = 1.2f * scaleFactor
            }
            canvas.drawRoundRect(pillRect, pillRadius, pillRadius, bPaint)
        }

        val cx = pillRect.centerX()
        val cy = pillRect.centerY() - 2f * scaleFactor
        val iconSize = pillH * 0.42f
        val iconColor = if (isEnabled) accentColor else secondaryTextColor

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

        // Active Status Dot underneath
        if (isEnabled) {
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = accentColor
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx, pillRect.bottom - 5.5f * scaleFactor, 1.8f * scaleFactor, dotPaint)
        }
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

    val pad = 12f * scaleFactor
    val contentW = cardRect.width() - pad * 2
    val halfH = (cardRect.height() - pad * 3) / 2f

    val topTileRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.left + pad + contentW, cardRect.top + pad + halfH)
    val bottomTileRect = RectF(cardRect.left + pad, topTileRect.bottom + pad, cardRect.left + pad + contentW, topTileRect.bottom + pad + halfH)
    val tileRadius = 14f * scaleFactor

    val fontRegular = getSlateFont(context, 400)
    val fontBold = getSlateFont(context, 700)

    // --- Top: Wi-Fi Card ---
    val wifiBg = if (state.wifi.isEnabled) Color.argb(40, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)) else Color.argb(22, 255, 255, 255)
    val wifiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = wifiBg }
    canvas.drawRoundRect(topTileRect, tileRadius, tileRadius, wifiPaint)

    val iconCx = topTileRect.left + topTileRect.height() * 0.42f
    val iconCy = topTileRect.centerY()
    val iconSize = topTileRect.height() * 0.44f
    drawVectorDrawable(context, canvas, R.drawable.ic_wifi, iconCx, iconCy, iconSize, if (state.wifi.isEnabled) accentColor else secondaryTextColor)

    val textX = iconCx + topTileRect.height() * 0.42f
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = primaryTextColor
        typeface = fontBold
        textSize = 13.5f * scaleFactor
    }
    canvas.drawText("Wi-Fi", textX, topTileRect.centerY() - 2f * scaleFactor, titlePaint)

    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = if (state.wifi.isEnabled) accentColor else secondaryTextColor
        typeface = fontRegular
        textSize = 10.5f * scaleFactor
    }
    val wifiSub = if (state.wifi.subtitle.length > 14) state.wifi.subtitle.take(13) + "…" else state.wifi.subtitle
    canvas.drawText(wifiSub, textX, topTileRect.centerY() + 13f * scaleFactor, subPaint)

    // --- Bottom: Bluetooth Card ---
    val btBg = if (state.bluetooth.isEnabled) Color.argb(40, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)) else Color.argb(22, 255, 255, 255)
    val btPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = btBg }
    canvas.drawRoundRect(bottomTileRect, tileRadius, tileRadius, btPaint)

    val btIconCx = bottomTileRect.left + bottomTileRect.height() * 0.42f
    val btIconCy = bottomTileRect.centerY()
    drawVectorDrawable(context, canvas, R.drawable.ic_bluetooth, btIconCx, btIconCy, iconSize, if (state.bluetooth.isEnabled) accentColor else secondaryTextColor)

    canvas.drawText("Bluetooth", textX, bottomTileRect.centerY() - 2f * scaleFactor, titlePaint)
    subPaint.color = if (state.bluetooth.isEnabled) accentColor else secondaryTextColor
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

    val pad = 10f * scaleFactor
    val gap = 8f * scaleFactor
    val contentW = cardRect.width() - pad * 2
    val contentH = cardRect.height() - pad * 2
    val cellW = (contentW - gap) / 2f
    val cellH = (contentH - gap) / 2f
    val cellRadius = 14f * scaleFactor

    val quads = listOf(state.wifi, state.bluetooth, state.torch, state.ringer)
    val fontBold = getSlateFont(context, 700)

    for (i in 0 until 4) {
        val r = i / 2
        val c = i % 2
        val left = cardRect.left + pad + c * (cellW + gap)
        val top = cardRect.top + pad + r * (cellH + gap)
        val rect = RectF(left, top, left + cellW, top + cellH)

        val item = quads[i]
        val isEnabled = item.isEnabled

        val bg = if (isEnabled) Color.argb(40, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)) else Color.argb(22, 255, 255, 255)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bg }
        canvas.drawRoundRect(rect, cellRadius, cellRadius, p)

        if (isEnabled) {
            val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(80, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
                style = Paint.Style.STROKE
                strokeWidth = 1.2f * scaleFactor
            }
            canvas.drawRoundRect(rect, cellRadius, cellRadius, bp)
        }

        val iconCx = rect.centerX()
        val iconCy = rect.centerY() - 8f * scaleFactor
        val iconSize = cellH * 0.40f
        val color = if (isEnabled) accentColor else secondaryTextColor

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
            this.color = if (isEnabled) primaryTextColor else secondaryTextColor
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

    val (primaryTextColor, secondaryTextColor, _) = drawCardBackground(canvas, cardRect, slateConfig, scaleFactor)
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()

    val fontBold = getSlateFont(context, 700)

    val trackPadH = 14f * scaleFactor
    val trackPadV = 16f * scaleFactor
    val trackRect = RectF(cardRect.left + trackPadH, cardRect.top + trackPadV, cardRect.right - trackPadH, cardRect.bottom - trackPadV)
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

    // Illuminated Sliding Thumb
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

    // Draw Notch Icons & Text
    for (i in 0 until notchCount) {
        val nMode = modes[i]
        val nCx = trackRect.left + i * notchW + notchW / 2f
        val isCurrent = (i == activeIdx)

        val iconSize = thumbRect.height() * 0.40f
        val iconColor = if (isCurrent) Color.BLACK else secondaryTextColor

        val resId = when (nMode) {
            AlertSliderMode.SILENT -> R.drawable.ic_bell_off
            AlertSliderMode.VIBRATE -> R.drawable.ic_phone_vibrate
            AlertSliderMode.RING -> R.drawable.ic_bell
        }
        drawVectorDrawable(context, canvas, resId, nCx, trackRect.centerY() - 6f * scaleFactor, iconSize, iconColor)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = if (isCurrent) Color.BLACK else secondaryTextColor
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
    val fontBold = getSlateFont(context, 700)

    val trackPadH = 12f * scaleFactor
    val trackPadV = 14f * scaleFactor
    val trackRect = RectF(cardRect.left + trackPadH, cardRect.top + trackPadV, cardRect.right - trackPadH, cardRect.bottom - trackPadV)
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

    // Sliding Thumb
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

    // Draw Icons in 3 vertical notches
    for (i in 0 until notchCount) {
        val nMode = modes[i]
        val nCy = trackRect.top + i * notchH + notchH / 2f
        val isCurrent = (i == activeIdx)

        val iconSize = thumbRect.width() * 0.42f
        val iconColor = if (isCurrent) Color.BLACK else secondaryTextColor

        val resId = when (nMode) {
            AlertSliderMode.RING -> R.drawable.ic_bell
            AlertSliderMode.VIBRATE -> R.drawable.ic_phone_vibrate
            AlertSliderMode.SILENT -> R.drawable.ic_bell_off
        }
        drawVectorDrawable(context, canvas, resId, trackRect.centerX(), nCy - 4f * scaleFactor, iconSize, iconColor)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = if (isCurrent) Color.BLACK else secondaryTextColor
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

    val cx = cardRect.centerX()
    val cy = cardRect.centerY() - 10f * scaleFactor
    val btnRadius = minOf(cardRect.width(), cardRect.height()) * 0.28f

    // Ambient Halo Glow when ON
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

    // Button Base
    val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isTorchOn) accentColor else Color.argb(30, 255, 255, 255)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, btnRadius, btnPaint)

    // Button Outline Ring
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isTorchOn) Color.argb(180, 255, 255, 255) else Color.argb(60, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * scaleFactor
    }
    canvas.drawCircle(cx, cy, btnRadius, ringPaint)

    // Torch Icon
    val iconSize = btnRadius * 0.95f
    val iconColor = if (isTorchOn) Color.BLACK else secondaryTextColor
    drawVectorDrawable(context, canvas, R.drawable.ic_flashlight, cx, cy, iconSize, iconColor)

    // Label Underneath
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

    val fontRegular = getSlateFont(context, 400)
    val fontBold = getSlateFont(context, 700)

    val padH = 14f * scaleFactor
    val iconR = cardRect.height() * 0.28f
    val iconCx = cardRect.left + padH + iconR
    val iconCy = cardRect.centerY()

    // Circular icon badge
    val badgeBg = if (item.isEnabled) Color.argb(45, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)) else Color.argb(22, 255, 255, 255)
    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = badgeBg }
    canvas.drawCircle(iconCx, iconCy, iconR, badgePaint)

    val iconSize = iconR * 1.18f
    val iconColor = if (item.isEnabled) accentColor else secondaryTextColor

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

    // Texts
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

    val padH = 12f * scaleFactor
    val padV = 10f * scaleFactor
    val contentRect = RectF(cardRect.left + padH, cardRect.top + padV, cardRect.right - padH, cardRect.bottom - padV)

    val cols = 3
    val rows = 2
    val gap = 8f * scaleFactor
    val cellW = (contentRect.width() - (gap * (cols - 1))) / cols
    val cellH = (contentRect.height() - (gap * (rows - 1))) / rows
    val cellRadius = 14f * scaleFactor

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

        val bg = if (isEnabled) Color.argb(38, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)) else Color.argb(22, 255, 255, 255)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bg }
        canvas.drawRoundRect(rect, cellRadius, cellRadius, p)

        if (isEnabled) {
            val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(80, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
                style = Paint.Style.STROKE
                strokeWidth = 1.2f * scaleFactor
            }
            canvas.drawRoundRect(rect, cellRadius, cellRadius, bp)
        }

        val iconCx = rect.left + rect.height() * 0.38f
        val iconCy = rect.centerY()
        val iconSize = rect.height() * 0.42f
        val iconColor = if (isEnabled) accentColor else secondaryTextColor

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
            this.color = primaryTextColor
            typeface = fontBold
            textSize = 11.5f * scaleFactor
        }
        canvas.drawText(item.label, textLeft, rect.centerY() - 2f * scaleFactor, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = if (isEnabled) accentColor else secondaryTextColor
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

    val cx = cardRect.centerX()
    val cy = cardRect.centerY() - 7f * scaleFactor
    val iconSize = cardRect.width() * 0.42f
    val iconColor = if (item.isEnabled) accentColor else secondaryTextColor

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
        this.color = if (item.isEnabled) primaryTextColor else secondaryTextColor
        typeface = fontBold
        textSize = 10f * scaleFactor
        textAlign = Paint.Align.CENTER
    }
    val label = if (item.type == ToggleType.RINGER) (alertMode?.label ?: "Sound") else item.label
    canvas.drawText(label, cx, cardRect.bottom - 10f * scaleFactor, labelPaint)

    return bitmap
}
