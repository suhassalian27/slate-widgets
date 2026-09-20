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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.appwidget.AppWidgetManager
import android.os.Bundle
import com.altusix.slate.widgets.common.renderUniversalFolderGrid

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
    customCornerRadius: Float? = null,
    drawBorder: Boolean = true
): Triple<Int, Int, Int> {
    val isLight = config.themeMode == "LIGHT"
    var bgColor = getSafeBgColor(config)

    // Safety Guard: If opacity or alpha is 0/uninitialized, fall back to solid theme background
    if (Color.alpha(bgColor) < 20) {
        bgColor = if (isLight) Color.WHITE else Color.rgb(24, 24, 26)
    }

    val cornerRadius = customCornerRadius ?: getStandardCornerRadius(scaleFactor)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

    if (drawBorder) {
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.argb(28, 0, 0, 0) else Color.argb(32, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * scaleFactor
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
    }

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
        ToggleType.DND -> if (isEnabled) R.drawable.ic_do_not_disturb_on else R.drawable.ic_do_not_disturb_off
        ToggleType.DATA -> R.drawable.ic_wifi
    }
    drawVectorDrawable(context, canvas, resId, cx, cy, size, color)
}

// =========================================================================
// REUSABLE TOGGLE CELL RENDERER
// =========================================================================

private fun drawQuickToggleSlot(
    canvas: Canvas,
    context: Context,
    tileRect: RectF,
    item: ToggleItemState,
    alertMode: AlertSliderMode?,
    accentColor: Int,
    isLight: Boolean,
    scaleFactor: Float,
    primaryTextColor: Int,
    secondaryTextColor: Int
) {
    val isEnabled = item.isEnabled
    val tileW = tileRect.width()
    val tileH = tileRect.height()

    // When enabled, draw solid accent fill.
    // When disabled, do nothing here: FolderWidgetKit's tilePaint has ALREADY drawn the background.
    if (isEnabled) {
        val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(tileRect, activePaint)
    }

    // Dynamic Contrast Colors
    val isAccentLight = (((accentColor shr 16 and 0xFF) * 0.2126f) +
            ((accentColor shr 8 and 0xFF) * 0.7152f) +
            ((accentColor and 0xFF) * 0.0722f)) / 255f > 0.65f
    val activeContentColor = if (isAccentLight) Color.BLACK else Color.WHITE
    val activeSubColor = if (isAccentLight) Color.argb(185, 0, 0, 0) else Color.argb(200, 255, 255, 255)

    val contentColor = if (isEnabled) activeContentColor else primaryTextColor
    val subColor = if (isEnabled) activeSubColor else secondaryTextColor
    val iconColor = if (isEnabled) activeContentColor else secondaryTextColor

    val cx = tileRect.centerX()
    val showSubtitle = tileH >= 46f * scaleFactor && tileW >= 56f * scaleFactor
    val showTitle = tileH >= 34f * scaleFactor && tileW >= 42f * scaleFactor

    val fontBold = getSlateFont(context, 700)
    val fontRegular = getSlateFont(context, 500)

    val titleSize = (tileH * 0.17f).coerceIn(9.5f * scaleFactor, 13f * scaleFactor)
    val subSize = (tileH * 0.13f).coerceIn(8.0f * scaleFactor, 10.5f * scaleFactor)
    val iconSize = when {
        showSubtitle && showTitle -> (tileH * 0.36f).coerceIn(20f * scaleFactor, 30f * scaleFactor)
        showTitle -> (tileH * 0.44f).coerceIn(22f * scaleFactor, 36f * scaleFactor)
        else -> (minOf(tileW, tileH) * 0.52f).coerceIn(20f * scaleFactor, 38f * scaleFactor)
    }

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = contentColor
        typeface = fontBold
        textSize = titleSize
        textAlign = Paint.Align.CENTER
    }

    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = subColor
        typeface = fontRegular
        textSize = subSize
        textAlign = Paint.Align.CENTER
    }

    if (showSubtitle && showTitle) {
        val iconCy = tileRect.top + (tileH * 0.34f)
        drawToggleIcon(context, canvas, item.type, cx, iconCy, iconSize, iconColor, isEnabled, alertMode)

        val titleBaseline = iconCy + (iconSize / 2f) + (titleSize * 0.95f) + (2f * scaleFactor)
        val maxTextW = tileW - (10f * scaleFactor)
        val elidedTitle = android.text.TextUtils.ellipsize(
            item.label, android.text.TextPaint(titlePaint), maxTextW.coerceAtLeast(10f), android.text.TextUtils.TruncateAt.END
        ).toString()
        canvas.drawText(elidedTitle, cx, titleBaseline, titlePaint)

        val subBaseline = titleBaseline + subSize + (2.5f * scaleFactor)
        val elidedSub = android.text.TextUtils.ellipsize(
            item.subtitle, android.text.TextPaint(subPaint), maxTextW.coerceAtLeast(10f), android.text.TextUtils.TruncateAt.END
        ).toString()
        canvas.drawText(elidedSub, cx, subBaseline, subPaint)
    } else if (showTitle) {
        val iconCy = tileRect.top + (tileH * 0.40f)
        drawToggleIcon(context, canvas, item.type, cx, iconCy, iconSize, iconColor, isEnabled, alertMode)

        val titleBaseline = tileRect.bottom - (tileH * 0.16f)
        val maxTextW = tileW - (10f * scaleFactor)
        val elidedTitle = android.text.TextUtils.ellipsize(
            item.label, android.text.TextPaint(titlePaint), maxTextW.coerceAtLeast(10f), android.text.TextUtils.TruncateAt.END
        ).toString()
        canvas.drawText(elidedTitle, cx, titleBaseline, titlePaint)
    } else {
        drawToggleIcon(context, canvas, item.type, cx, tileRect.centerY(), iconSize, iconColor, isEnabled, alertMode)
    }
}

// =========================================================================
// 1. CONTROL CENTER DECK (4x2 or 2x4 Dynamic Grid)
// =========================================================================

fun generateControlCenterDeckBitmap(
    context: Context,
    state: QuickTogglesState,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val isLight = slateConfig.themeMode == "LIGHT"
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val primaryTextColor = if (isLight) Color.BLACK else Color.WHITE
    val secondaryTextColor = if (isLight) Color.argb(170, 0, 0, 0) else Color.argb(170, 255, 255, 255)

    val isTallMode = isResponsive && (wDp.toFloat() / hDp.toFloat() < 1.15f)
    val cols = if (isTallMode) 2 else 4
    val rows = if (isTallMode) 4 else 2

    val toggles = listOf(
        state.wifi, state.bluetooth, state.torch, state.ringer,
        state.autoRotate, state.hotspot, state.location, state.darkMode
    )

    return renderUniversalFolderGrid(
        context = context,
        config = slateConfig,
        isResponsive = isResponsive,
        wDp = wDp,
        hDp = hDp,
        cols = cols,
        rows = rows,
        showTileBackground = true
    ) { canvas, tileRect, index, scaleFactor, _ ->
        val item = toggles[index]
        drawQuickToggleSlot(
            canvas = canvas,
            context = context,
            tileRect = tileRect,
            item = item,
            alertMode = if (item.type == ToggleType.RINGER) state.alertSlider else null,
            accentColor = accentColor,
            isLight = isLight,
            scaleFactor = scaleFactor,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor
        )
    }
}

// =========================================================================
// 2. MINIMALIST ACTION TOOLBAR (5x1 / 1x5 Pivot)
// =========================================================================

fun generateMinimalistToolbarBitmap(
    context: Context,
    state: QuickTogglesState,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val isLight = slateConfig.themeMode == "LIGHT"
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val primaryTextColor = if (isLight) Color.BLACK else Color.WHITE
    val secondaryTextColor = if (isLight) Color.argb(170, 0, 0, 0) else Color.argb(170, 255, 255, 255)

    val isVertical = isResponsive && (hDp > wDp)
    val cols = if (isVertical) 1 else 5
    val rows = if (isVertical) 5 else 1

    val items = listOf(state.wifi, state.bluetooth, state.torch, state.ringer, state.autoRotate)

    return renderUniversalFolderGrid(
        context = context,
        config = slateConfig,
        isResponsive = isResponsive,
        wDp = wDp,
        hDp = hDp,
        cols = cols,
        rows = rows,
        showTileBackground = true
    ) { canvas, tileRect, index, scaleFactor, _ ->
        val item = items[index]
        drawQuickToggleSlot(
            canvas = canvas,
            context = context,
            tileRect = tileRect,
            item = item,
            alertMode = if (item.type == ToggleType.RINGER) state.alertSlider else null,
            accentColor = accentColor,
            isLight = isLight,
            scaleFactor = scaleFactor,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor
        )
    }
}

// =========================================================================
// 3. CONNECTIVITY DUO BENTO (2x2 / 1x2 / 2x1 Pivot)
// =========================================================================

fun generateConnectivityBentoBitmap(
    context: Context,
    state: QuickTogglesState,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val isLight = slateConfig.themeMode == "LIGHT"
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val primaryTextColor = if (isLight) Color.BLACK else Color.WHITE
    val secondaryTextColor = if (isLight) Color.argb(170, 0, 0, 0) else Color.argb(170, 255, 255, 255)

    val isWide = isResponsive && (wDp > hDp * 1.35f)
    val cols = if (isWide) 2 else 1
    val rows = if (isWide) 1 else 2

    val items = listOf(state.wifi, state.bluetooth)

    return renderUniversalFolderGrid(
        context = context,
        config = slateConfig,
        isResponsive = isResponsive,
        wDp = wDp,
        hDp = hDp,
        cols = cols,
        rows = rows,
        showTileBackground = true
    ) { canvas, tileRect, index, scaleFactor, _ ->
        val item = items[index]
        drawQuickToggleSlot(
            canvas = canvas,
            context = context,
            tileRect = tileRect,
            item = item,
            alertMode = null,
            accentColor = accentColor,
            isLight = isLight,
            scaleFactor = scaleFactor,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor
        )
    }
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
    val isLight = slateConfig.themeMode == "LIGHT"
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val primaryTextColor = if (isLight) Color.BLACK else Color.WHITE
    val secondaryTextColor = if (isLight) Color.argb(170, 0, 0, 0) else Color.argb(170, 255, 255, 255)

    val quads = listOf(state.wifi, state.bluetooth, state.torch, state.ringer)

    return renderUniversalFolderGrid(
        context = context,
        config = slateConfig,
        isResponsive = isResponsive,
        wDp = wDp,
        hDp = hDp,
        cols = 2,
        rows = 2,
        showTileBackground = true
    ) { canvas, tileRect, index, scaleFactor, _ ->
        val item = quads[index]
        drawQuickToggleSlot(
            canvas = canvas,
            context = context,
            tileRect = tileRect,
            item = item,
            alertMode = if (item.type == ToggleType.RINGER) state.alertSlider else null,
            accentColor = accentColor,
            isLight = isLight,
            scaleFactor = scaleFactor,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor
        )
    }
}

// =========================================================================
// 5. TACTILE ALERT SLIDER (HORIZONTAL 2x1 - Pure Pill Surface)
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

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val targetAspect = 2.0f
        var cardH = h
        var cardW = cardH * targetAspect
        if (cardW > w) {
            cardW = w
            cardH = cardW / targetAspect
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    // Outer card pill curvature
    val outerPillRadius = cardRect.height() / 2f
    val (_, secondaryTextColor, _) = drawCardBackground(
        canvas = canvas,
        rect = cardRect,
        config = slateConfig,
        scaleFactor = scaleFactor,
        customCornerRadius = outerPillRadius
    )
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()

    val isAccentLight = (((accentColor shr 16 and 0xFF) * 0.2126f) +
            ((accentColor shr 8 and 0xFF) * 0.7152f) +
            ((accentColor and 0xFF) * 0.0722f)) / 255f > 0.65f
    val activeContentColor = if (isAccentLight) Color.BLACK else Color.WHITE

    // Inset active zone (No gray track drawn underneath)
    val pad = (cardRect.height() * 0.06f).coerceIn(4f * scaleFactor, 8f * scaleFactor)
    val contentRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.bottom - pad)

    val notchCount = 3
    val notchW = contentRect.width() / notchCount.toFloat()
    val modes = listOf(AlertSliderMode.SILENT, AlertSliderMode.VIBRATE, AlertSliderMode.RING)
    val activeIdx = modes.indexOf(mode).coerceAtLeast(0)

    // Solid Accent Sliding Thumb
    val thumbLeft = contentRect.left + (activeIdx * notchW)
    val thumbRight = contentRect.left + ((activeIdx + 1) * notchW)
    val thumbTop = contentRect.top
    val thumbBottom = contentRect.bottom
    val thumbRect = RectF(thumbLeft, thumbTop, thumbRight, thumbBottom)

    // Concentric pill cap radius
    val outerCornerR = outerPillRadius - pad
    val innerCornerR = (thumbRect.height() * 0.24f).coerceIn(8f * scaleFactor, 16f * scaleFactor)

    val tl = if (activeIdx == 0) outerCornerR else innerCornerR
    val bl = if (activeIdx == 0) outerCornerR else innerCornerR
    val tr = if (activeIdx == 2) outerCornerR else innerCornerR
    val br = if (activeIdx == 2) outerCornerR else innerCornerR

    val thumbPath = createCornerPath(thumbRect, tl, tr, br, bl)

    val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }
    canvas.drawPath(thumbPath, thumbPaint)

    // Base icon size
    val baseIconSize = (thumbRect.height() * 0.44f).coerceIn(20f * scaleFactor, 36f * scaleFactor)

    for (i in 0 until notchCount) {
        val nMode = modes[i]
        val nCx = contentRect.left + (i * notchW) + (notchW / 2f)
        val nCy = contentRect.centerY()
        val isCurrent = (i == activeIdx)

        val iconColor = if (isCurrent) activeContentColor else secondaryTextColor
        val resId = when (nMode) {
            AlertSliderMode.SILENT -> R.drawable.ic_bell_off
            AlertSliderMode.VIBRATE -> R.drawable.ic_phone_vibrate
            AlertSliderMode.RING -> R.drawable.ic_bell
        }

        // Optical weight balancing
        val effectiveIconSize = when (nMode) {
            AlertSliderMode.VIBRATE -> baseIconSize * 1.12f
            AlertSliderMode.RING -> baseIconSize * 0.84f
            AlertSliderMode.SILENT -> baseIconSize * 0.84f
        }

        drawVectorDrawable(context, canvas, resId, nCx, nCy, effectiveIconSize, iconColor)
    }

    return bitmap
}

// =========================================================================
// 6. VERTICAL ALERT SLIDER (1x2 - Pure Pill Surface)
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

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val targetAspect = 0.5f
        var cardW = w
        var cardH = cardW / targetAspect
        if (cardH > h) {
            cardH = h
            cardW = cardH * targetAspect
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    // Outer card vertical pill curvature
    val outerPillRadius = cardRect.width() / 2f
    val (_, secondaryTextColor, _) = drawCardBackground(
        canvas = canvas,
        rect = cardRect,
        config = slateConfig,
        scaleFactor = scaleFactor,
        customCornerRadius = outerPillRadius
    )
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()

    val isAccentLight = (((accentColor shr 16 and 0xFF) * 0.2126f) +
            ((accentColor shr 8 and 0xFF) * 0.7152f) +
            ((accentColor and 0xFF) * 0.0722f)) / 255f > 0.65f
    val activeContentColor = if (isAccentLight) Color.BLACK else Color.WHITE

    // Inset active zone (No gray track drawn underneath)
    val pad = (cardRect.width() * 0.06f).coerceIn(4f * scaleFactor, 8f * scaleFactor)
    val contentRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.bottom - pad)

    val notchCount = 3
    val notchH = contentRect.height() / notchCount.toFloat()
    val modes = listOf(AlertSliderMode.RING, AlertSliderMode.VIBRATE, AlertSliderMode.SILENT)
    val activeIdx = modes.indexOf(mode).coerceAtLeast(0)

    // Solid Accent Sliding Thumb
    val thumbTop = contentRect.top + (activeIdx * notchH)
    val thumbBottom = contentRect.top + ((activeIdx + 1) * notchH)
    val thumbLeft = contentRect.left
    val thumbRight = contentRect.right
    val thumbRect = RectF(thumbLeft, thumbTop, thumbRight, thumbBottom)

    // Concentric pill cap radius
    val outerCornerR = outerPillRadius - pad
    val innerCornerR = (thumbRect.width() * 0.24f).coerceIn(8f * scaleFactor, 16f * scaleFactor)

    val tl = if (activeIdx == 0) outerCornerR else innerCornerR
    val tr = if (activeIdx == 0) outerCornerR else innerCornerR
    val bl = if (activeIdx == 2) outerCornerR else innerCornerR
    val br = if (activeIdx == 2) outerCornerR else innerCornerR

    val thumbPath = createCornerPath(thumbRect, tl, tr, br, bl)

    val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }
    canvas.drawPath(thumbPath, thumbPaint)

    // Base icon size
    val baseIconSize = (thumbRect.width() * 0.44f).coerceIn(20f * scaleFactor, 36f * scaleFactor)

    for (i in 0 until notchCount) {
        val nMode = modes[i]
        val nCx = contentRect.centerX()
        val nCy = contentRect.top + (i * notchH) + (notchH / 2f)
        val isCurrent = (i == activeIdx)

        val iconColor = if (isCurrent) activeContentColor else secondaryTextColor
        val resId = when (nMode) {
            AlertSliderMode.RING -> R.drawable.ic_bell
            AlertSliderMode.VIBRATE -> R.drawable.ic_phone_vibrate
            AlertSliderMode.SILENT -> R.drawable.ic_bell_off
        }

        // Optical weight balancing
        val effectiveIconSize = when (nMode) {
            AlertSliderMode.VIBRATE -> baseIconSize * 1.12f
            AlertSliderMode.RING -> baseIconSize * 0.84f
            AlertSliderMode.SILENT -> baseIconSize * 0.84f
        }

        drawVectorDrawable(context, canvas, resId, nCx, nCy, effectiveIconSize, iconColor)
    }

    return bitmap
}

// =========================================================================
// 7. FLASHLIGHT TORCH SWITCH (2x2 - Cinematic Spotlight)
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

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val isLight = slateConfig.themeMode == "LIGHT"
    val (primaryTextColor, secondaryTextColor, _) = drawCardBackground(
        canvas = canvas,
        rect = cardRect,
        config = slateConfig,
        scaleFactor = scaleFactor,
        customCornerRadius = outerRadius
    )

    val cw = cardRect.width()
    val ch = cardRect.height()
    val cx = cardRect.centerX()
    val cy = cardRect.centerY()

    // Clip light projection to the card's squircle boundary
    canvas.save()
    val cardClipPath = Path().apply {
        addRoundRect(cardRect, outerRadius, outerRadius, Path.Direction.CW)
    }
    canvas.clipPath(cardClipPath)

    if (isTorchOn) {
        // 1. Flashlight Icon anchored at bottom-center
        val iconSize = (minOf(cw, ch) * 0.22f).coerceIn(24f * scaleFactor, 46f * scaleFactor)
        val iconCx = cx
        val iconCy = cardRect.bottom - (ch * 0.22f)
        val lensY = iconCy - (iconSize / 2f)

        // 2. Parabolic Spotlight Beam
        val curveStartY = cardRect.top + (ch * 0.48f)
        val curveDipY = lensY - (6f * scaleFactor)

        val beamPath = Path().apply {
            moveTo(cardRect.left - 5f, cardRect.top - 5f)
            lineTo(cardRect.right + 5f, cardRect.top - 5f)
            lineTo(cardRect.right + 5f, curveStartY)
            quadTo(iconCx, curveDipY, cardRect.left - 5f, curveStartY)
            close()
        }

        // 3. Radiant Light Gradient (Clean fade tailored for both Dark & Light backgrounds)
        val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = if (isLight) {
                RadialGradient(
                    iconCx, curveDipY, ch * 0.88f,
                    intArrayOf(
                        Color.argb(240, 255, 236, 190), // Warm golden core
                        Color.argb(200, 255, 214, 150),
                        Color.argb(125, 245, 185, 115),
                        Color.argb(45, 235, 170, 95),
                        Color.argb(0, 255, 215, 150)    // Transparent warm amber falloff
                    ),
                    floatArrayOf(0.0f, 0.26f, 0.55f, 0.82f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            } else {
                RadialGradient(
                    iconCx, curveDipY, ch * 0.88f,
                    intArrayOf(
                        Color.argb(255, 255, 248, 222), // Glowing white-gold core
                        Color.argb(225, 255, 212, 148), // Warm golden light
                        Color.argb(140, 225, 148, 78),  // Amber diffusion
                        Color.argb(55, 145, 85, 35),    // Soft outer glow
                        Color.argb(0, 255, 210, 140)    // Transparent amber falloff
                    ),
                    floatArrayOf(0.0f, 0.24f, 0.54f, 0.80f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }
        }
        canvas.drawPath(beamPath, beamPaint)

        // 4. Optical Lens Edge Contour
        val edgePath = Path().apply {
            moveTo(cardRect.left, curveStartY)
            quadTo(iconCx, curveDipY, cardRect.right, curveStartY)
        }
        val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.6f * scaleFactor
            shader = if (isLight) {
                RadialGradient(
                    iconCx, curveDipY, cw * 0.65f,
                    intArrayOf(
                        Color.argb(130, 215, 145, 65), // Warm amber contour on white
                        Color.argb(40, 215, 145, 65),
                        Color.TRANSPARENT
                    ),
                    floatArrayOf(0.0f, 0.65f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            } else {
                RadialGradient(
                    iconCx, curveDipY, cw * 0.65f,
                    intArrayOf(
                        Color.argb(180, 255, 248, 230), // Glowing edge on dark
                        Color.argb(70, 255, 205, 130),
                        Color.TRANSPARENT
                    ),
                    floatArrayOf(0.0f, 0.55f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }
        }
        canvas.drawPath(edgePath, edgePaint)

        // 5. High-contrast flashlight icon (Dark on white, White on dark)
        val torchIconColor = if (isLight) Color.argb(230, 24, 24, 26) else Color.WHITE
        drawVectorDrawable(
            context = context,
            canvas = canvas,
            resId = R.drawable.ic_flashlight_filled,
            cx = iconCx,
            cy = iconCy,
            size = iconSize,
            color = torchIconColor
        )

    } else {
        // Minimalist OFF state
        val iconSize = (minOf(cw, ch) * 0.30f).coerceIn(28f * scaleFactor, 52f * scaleFactor)
        drawVectorDrawable(
            context = context,
            canvas = canvas,
            resId = R.drawable.ic_flashlight,
            cx = cx,
            cy = cy,
            size = iconSize,
            color = secondaryTextColor
        )
    }

    canvas.restore()
    return bitmap
}

// =========================================================================
// 8. GENERIC TOGGLE PILL (2x1 - Pure Borderless Pill)
// =========================================================================

fun generateTogglePillBitmap(
    context: Context,
    item: ToggleItemState,
    alertMode: AlertSliderMode? = null,
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
        val targetAspect = 2.0f
        var cardH = h
        var cardW = cardH * targetAspect
        if (cardW > w) {
            cardW = w
            cardH = cardW / targetAspect
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val (primaryTextColor, secondaryTextColor, _) = drawCardBackground(
        canvas = canvas,
        rect = cardRect,
        config = slateConfig,
        scaleFactor = scaleFactor,
        customCornerRadius = outerRadius,
        drawBorder = false
    )
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val isLight = slateConfig.themeMode == "LIGHT"

    val isAccentLight = (((accentColor shr 16 and 0xFF) * 0.2126f) +
            ((accentColor shr 8 and 0xFF) * 0.7152f) +
            ((accentColor and 0xFF) * 0.0722f)) / 255f > 0.65f
    val activeContentColor = if (isAccentLight) Color.BLACK else Color.WHITE

    val fontRegular = getSlateFont(context, 400)
    val fontBold = getSlateFont(context, 700)

    val cw = cardRect.width()
    val ch = cardRect.height()
    val isCompact = cw < 120f * scaleFactor

    if (isCompact) {
        val iconR = (minOf(cw, ch) * 0.36f).coerceIn(16f * scaleFactor, 36f * scaleFactor)
        val cx = cardRect.centerX()
        val cy = cardRect.centerY()

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (item.isEnabled) accentColor else (if (isLight) Color.argb(16, 0, 0, 0) else Color.argb(22, 255, 255, 255))
        }
        canvas.drawCircle(cx, cy, iconR, badgePaint)

        val iconSize = iconR * 1.15f
        val iconColor = if (item.isEnabled) activeContentColor else secondaryTextColor
        drawToggleIcon(context, canvas, item.type, cx, cy, iconSize, iconColor, item.isEnabled, alertMode)

    } else {
        val padH = (cw * 0.08f).coerceIn(10f * scaleFactor, 18f * scaleFactor)
        val iconR = (ch * 0.28f).coerceIn(16f * scaleFactor, 28f * scaleFactor)
        val iconCx = cardRect.left + padH + iconR
        val iconCy = cardRect.centerY()

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (item.isEnabled) accentColor else (if (isLight) Color.argb(16, 0, 0, 0) else Color.argb(22, 255, 255, 255))
        }
        canvas.drawCircle(iconCx, iconCy, iconR, badgePaint)

        val iconSize = iconR * 1.15f
        val iconColor = if (item.isEnabled) activeContentColor else secondaryTextColor
        drawToggleIcon(context, canvas, item.type, iconCx, iconCy, iconSize, iconColor, item.isEnabled, alertMode)

        val textLeft = iconCx + iconR + (12f * scaleFactor)
        val maxTextW = (cardRect.right - (12f * scaleFactor) - textLeft).coerceAtLeast(10f)

        val showSub = ch >= 48f * scaleFactor && item.subtitle.isNotBlank()
        val titleSize = (ch * 0.22f).coerceIn(11.5f * scaleFactor, 15f * scaleFactor)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = primaryTextColor
            typeface = fontBold
            textSize = titleSize
        }

        if (showSub) {
            val titleY = cardRect.centerY() - (2f * scaleFactor)
            val elidedTitle = android.text.TextUtils.ellipsize(
                item.label,
                android.text.TextPaint(titlePaint),
                maxTextW,
                android.text.TextUtils.TruncateAt.END
            ).toString()
            canvas.drawText(elidedTitle, textLeft, titleY, titlePaint)

            val subSize = (ch * 0.16f).coerceIn(9.5f * scaleFactor, 12f * scaleFactor)
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = if (item.isEnabled) accentColor else secondaryTextColor
                typeface = fontRegular
                textSize = subSize
            }
            val elidedSub = android.text.TextUtils.ellipsize(
                item.subtitle,
                android.text.TextPaint(subPaint),
                maxTextW,
                android.text.TextUtils.TruncateAt.END
            ).toString()
            canvas.drawText(elidedSub, textLeft, cardRect.centerY() + (13f * scaleFactor), subPaint)
        } else {
            val titleY = cardRect.centerY() + (titleSize * 0.35f)
            val elidedTitle = android.text.TextUtils.ellipsize(
                item.label,
                android.text.TextPaint(titlePaint),
                maxTextW,
                android.text.TextUtils.TruncateAt.END
            ).toString()
            canvas.drawText(elidedTitle, textLeft, titleY, titlePaint)
        }
    }

    return bitmap
}

// =========================================================================
// 9. SYSTEM UTILITY DECK (4x2 / 3x2 / 2x3 Pivot)
// =========================================================================

fun generateSystemUtilityDeckBitmap(
    context: Context,
    state: QuickTogglesState,
    slateConfig: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val isLight = slateConfig.themeMode == "LIGHT"
    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val primaryTextColor = if (isLight) Color.BLACK else Color.WHITE
    val secondaryTextColor = if (isLight) Color.argb(170, 0, 0, 0) else Color.argb(170, 255, 255, 255)

    val isTall = isResponsive && (hDp > wDp)
    val cols = if (isTall) 2 else 3
    val rows = if (isTall) 3 else 2

    val utilities = listOf(
        state.timeout,
        state.autoRotate,
        state.batterySaver,
        state.darkMode,
        state.airplane,
        state.hotspot
    )

    return renderUniversalFolderGrid(
        context = context,
        config = slateConfig,
        isResponsive = isResponsive,
        wDp = wDp,
        hDp = hDp,
        cols = cols,
        rows = rows,
        showTileBackground = true
    ) { canvas, tileRect, index, scaleFactor, _ ->
        val item = utilities[index]
        drawQuickToggleSlot(
            canvas = canvas,
            context = context,
            tileRect = tileRect,
            item = item,
            alertMode = null,
            accentColor = accentColor,
            isLight = isLight,
            scaleFactor = scaleFactor,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor
        )
    }
}

// =========================================================================
// 10. MICRO TOGGLE (1x1 - Centered Large Icon, No Text)
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

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w, h)
    } else {
        val size = minOf(w, h)
        RectF((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f)
    }

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val (primaryTextColor, secondaryTextColor, _) = drawCardBackground(
        canvas = canvas,
        rect = cardRect,
        config = slateConfig,
        scaleFactor = scaleFactor,
        customCornerRadius = outerRadius,
        drawBorder = false
    )

    val accentColor = androidx.compose.ui.graphics.Color(slateConfig.accentColorHex).toArgb()
    val isAccentLight = (((accentColor shr 16 and 0xFF) * 0.2126f) +
            ((accentColor shr 8 and 0xFF) * 0.7152f) +
            ((accentColor and 0xFF) * 0.0722f)) / 255f > 0.65f
    val activeContentColor = if (isAccentLight) Color.BLACK else Color.WHITE

    val pad = (minOf(cardRect.width(), cardRect.height()) * 0.06f).coerceIn(4f * scaleFactor, 8f * scaleFactor)
    val innerRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.bottom - pad)
    val innerR = (outerRadius - pad).coerceAtLeast(6f * scaleFactor)

    // Solid accent fill when active
    if (item.isEnabled) {
        val solidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(innerRect, innerR, innerR, solidPaint)
    }

    val cx = cardRect.centerX()
    val cy = cardRect.centerY()
    val iconSize = (minOf(cardRect.width(), cardRect.height()) * 0.52f).coerceIn(28f * scaleFactor, 58f * scaleFactor)
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

    return bitmap
}


// =========================================================================
// QUICK TOGGLES LIVE PREVIEW (True Home Screen 1:1 Scale)
// =========================================================================

@Composable
fun QuickToggleLivePreview(
    widgetClassName: String,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    appWidgetId: Int
) {
    val context = LocalContext.current
    val manager = remember { AppWidgetManager.getInstance(context) }

    // 1. Query the live launcher options assigned to this specific widget
    val options = remember(appWidgetId) {
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            manager?.getAppWidgetOptions(appWidgetId)
        } else null
    }

    val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val optW = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0) ?: 0
    else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
    val optH = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
    else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0) ?: 0

    // 2. Exact home screen cell dimensions (1 cell ≈ 72dp)
    val (defaultW, defaultH) = remember(widgetClassName) {
        when {
            widgetClassName.contains("Micro") -> 72 to 72
            widgetClassName.contains("Toolbar") -> 320 to 72
            widgetClassName.contains("SliderV") -> 72 to 156
            widgetClassName.contains("Pill") || widgetClassName.contains("SliderH") -> 160 to 72
            widgetClassName.contains("ControlCenter") || widgetClassName.contains("UtilityDeck") -> 320 to 156
            else -> 160 to 160 // Quad, Connectivity, Torch Switch (2x2)
        }
    }

    val actualWDp = if (optW > 0) optW else defaultW
    val actualHDp = if (optH > 0) optH else defaultH

    // 3. Keep 1:1 scale for pills/micros; gently downscale only if wider/taller than scaffold viewport
    val maxDisplayW = 310f
    val maxDisplayH = 150f
    val scale = if (actualWDp > maxDisplayW || actualHDp > maxDisplayH) {
        minOf(maxDisplayW / actualWDp.toFloat(), maxDisplayH / actualHDp.toFloat())
    } else {
        1.0f
    }

    val displayW = actualWDp * scale
    val displayH = actualHDp * scale

    val bitmap = remember(widgetClassName, config, isResponsive, actualWDp, actualHDp) {
        try {
            val receiverClass = Class.forName(widgetClassName)
            val receiver = receiverClass.getDeclaredConstructor().newInstance()
            (receiver as? BaseQuickTogglesReceiver)?.renderWidgetBitmap(
                context = context,
                appWidgetId = appWidgetId,
                config = config,
                isResponsive = isResponsive,
                wDp = actualWDp,
                hDp = actualHDp
            )
        } catch (_: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Quick Toggle Preview",
                modifier = Modifier.size(displayW.dp, displayH.dp)
            )
        } else {
            CircularProgressIndicator(
                color = androidx.compose.ui.graphics.Color(config.accentColorHex),
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        }
    }
}
