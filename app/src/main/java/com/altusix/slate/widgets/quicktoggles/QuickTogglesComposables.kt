package com.altusix.slate.widgets.quicktoggles

import android.content.Context
import android.graphics.*
import androidx.compose.ui.graphics.toArgb
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

// -------------------------------------------------------------------------
// MATHEMATICAL VECTOR GLYPHS
// -------------------------------------------------------------------------

private fun drawWifiGlyph(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int, isConnected: Boolean) {
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = size * 0.12f
        strokeCap = Paint.Cap.ROUND
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    val dotRadius = size * 0.11f
    val dotY = cy + size * 0.35f
    canvas.drawCircle(cx, dotY, dotRadius, fillPaint)

    // Inner Arc
    val r1 = size * 0.38f
    val rect1 = RectF(cx - r1, dotY - r1, cx + r1, dotY + r1)
    canvas.drawArc(rect1, 225f, 90f, false, strokePaint)

    // Outer Arc
    val r2 = size * 0.68f
    val rect2 = RectF(cx - r2, dotY - r2, cx + r2, dotY + r2)
    canvas.drawArc(rect2, 225f, 90f, false, strokePaint)
}

private fun drawBluetoothGlyph(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = size * 0.12f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    val h = size * 0.85f
    val w = size * 0.42f
    val top = cy - h / 2f
    val bottom = cy + h / 2f
    val midY = cy

    // Vertical spine
    canvas.drawLine(cx, top, cx, bottom, strokePaint)

    // Upper & Lower wings
    val path = Path().apply {
        moveTo(cx - w, top + h * 0.28f)
        lineTo(cx + w, bottom - h * 0.28f)
        lineTo(cx, bottom)
        lineTo(cx, top)
        lineTo(cx + w, top + h * 0.28f)
        lineTo(cx - w, bottom - h * 0.28f)
    }
    canvas.drawPath(path, strokePaint)
}

private fun drawTorchGlyph(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int, isOn: Boolean) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = size * 0.11f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    val h = size * 0.80f
    val w = size * 0.38f
    val top = cy - h / 2f
    val bottom = cy + h / 2f

    // Flashlight handle & head
    val path = Path().apply {
        // Head
        moveTo(cx - w * 0.9f, top)
        lineTo(cx + w * 0.9f, top)
        lineTo(cx + w * 0.6f, top + h * 0.28f)
        // Handle
        lineTo(cx + w * 0.5f, bottom)
        lineTo(cx - w * 0.5f, bottom)
        lineTo(cx - w * 0.6f, top + h * 0.28f)
        close()
    }
    canvas.drawPath(path, paint)

    // Power indicator switch
    val switchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy + h * 0.08f, size * 0.07f, switchPaint)

    // Radiance rays when ON
    if (isOn) {
        val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = size * 0.09f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(cx, top - size * 0.10f, cx, top - size * 0.28f, rayPaint)
        canvas.drawLine(cx - size * 0.25f, top - size * 0.08f, cx - size * 0.38f, top - size * 0.24f, rayPaint)
        canvas.drawLine(cx + size * 0.25f, top - size * 0.08f, cx + size * 0.38f, top - size * 0.24f, rayPaint)
    }
}

private fun drawSoundGlyph(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int, mode: AlertSliderMode) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = size * 0.11f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    when (mode) {
        AlertSliderMode.RING -> {
            // Bell Body
            val bellPath = Path().apply {
                moveTo(cx - size * 0.36f, cy + size * 0.25f)
                lineTo(cx + size * 0.36f, cy + size * 0.25f)
                cubicTo(cx + size * 0.28f, cy - size * 0.10f, cx + size * 0.22f, cy - size * 0.38f, cx, cy - size * 0.38f)
                cubicTo(cx - size * 0.22f, cy - size * 0.38f, cx - size * 0.28f, cy - size * 0.10f, cx - size * 0.36f, cy + size * 0.25f)
            }
            canvas.drawPath(bellPath, paint)

            // Clapper Dot
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx, cy + size * 0.38f, size * 0.08f, dotPaint)
            // Top knot
            canvas.drawLine(cx, cy - size * 0.38f, cx, cy - size * 0.46f, paint)
        }
        AlertSliderMode.VIBRATE -> {
            // Center Phone
            val phoneRect = RectF(cx - size * 0.22f, cy - size * 0.42f, cx + size * 0.22f, cy + size * 0.42f)
            canvas.drawRoundRect(phoneRect, size * 0.08f, size * 0.08f, paint)

            // Vibration brackets left & right
            val waveL = Path().apply {
                moveTo(cx - size * 0.36f, cy - size * 0.22f)
                lineTo(cx - size * 0.46f, cy)
                lineTo(cx - size * 0.36f, cy + size * 0.22f)
            }
            canvas.drawPath(waveL, paint)

            val waveR = Path().apply {
                moveTo(cx + size * 0.36f, cy - size * 0.22f)
                lineTo(cx + size * 0.46f, cy)
                lineTo(cx + size * 0.36f, cy + size * 0.22f)
            }
            canvas.drawPath(waveR, paint)
        }
        AlertSliderMode.SILENT -> {
            // Muted Bell with Slash
            val bellPath = Path().apply {
                moveTo(cx - size * 0.36f, cy + size * 0.25f)
                lineTo(cx + size * 0.36f, cy + size * 0.25f)
                cubicTo(cx + size * 0.28f, cy - size * 0.10f, cx + size * 0.22f, cy - size * 0.38f, cx, cy - size * 0.38f)
                cubicTo(cx - size * 0.22f, cy - size * 0.38f, cx - size * 0.28f, cy - size * 0.10f, cx - size * 0.36f, cy + size * 0.25f)
            }
            canvas.drawPath(bellPath, paint)

            // Diagonal slash
            val slashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeWidth = size * 0.12f
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(cx - size * 0.45f, cy + size * 0.45f, cx + size * 0.45f, cy - size * 0.45f, slashPaint)
        }
    }
}

private fun drawAutoRotateGlyph(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int, isAuto: Boolean) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = size * 0.11f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Tilted Phone Box
    canvas.save()
    canvas.rotate(45f, cx, cy)
    val phoneRect = RectF(cx - size * 0.22f, cy - size * 0.34f, cx + size * 0.22f, cy + size * 0.34f)
    canvas.drawRoundRect(phoneRect, size * 0.06f, size * 0.06f, paint)
    canvas.restore()

    // Curved Orbit Arrows
    val r = size * 0.48f
    val arcRect = RectF(cx - r, cy - r, cx + r, cy + r)

    // Top Right Arc
    canvas.drawArc(arcRect, 270f, 75f, false, paint)
    // Bottom Left Arc
    canvas.drawArc(arcRect, 90f, 75f, false, paint)

    // Arrowheads
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    // Top arrow
    val arrowTop = Path().apply {
        moveTo(cx + r * 0.95f, cy - r * 0.35f)
        lineTo(cx + r * 1.15f, cy - r * 0.05f)
        lineTo(cx + r * 0.75f, cy - r * 0.05f)
        close()
    }
    canvas.drawPath(arrowTop, fillPaint)
}

private fun drawHotspotGlyph(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = size * 0.11f
        strokeCap = Paint.Cap.ROUND
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    // Center node
    canvas.drawCircle(cx, cy, size * 0.14f, fillPaint)

    // Outer Waves
    val r1 = size * 0.34f
    val rect1 = RectF(cx - r1, cy - r1, cx + r1, cy + r1)
    canvas.drawArc(rect1, 135f, 90f, false, strokePaint)
    canvas.drawArc(rect1, 315f, 90f, false, strokePaint)

    val r2 = size * 0.54f
    val rect2 = RectF(cx - r2, cy - r2, cx + r2, cy + r2)
    canvas.drawArc(rect2, 135f, 90f, false, strokePaint)
    canvas.drawArc(rect2, 315f, 90f, false, strokePaint)
}

private fun drawAirplaneGlyph(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    val path = Path().apply {
        moveTo(cx, cy - size * 0.44f) // Nose
        lineTo(cx + size * 0.08f, cy - size * 0.15f)
        lineTo(cx + size * 0.45f, cy + size * 0.08f) // Right wing tip
        lineTo(cx + size * 0.45f, cy + size * 0.18f)
        lineTo(cx + size * 0.08f, cy + size * 0.08f)
        lineTo(cx + size * 0.08f, cy + size * 0.32f)
        lineTo(cx + size * 0.22f, cy + size * 0.42f) // Right tail
        lineTo(cx + size * 0.22f, cy + size * 0.48f)
        lineTo(cx, cy + size * 0.42f)
        lineTo(cx - size * 0.22f, cy + size * 0.48f)
        lineTo(cx - size * 0.22f, cy + size * 0.42f) // Left tail
        lineTo(cx - size * 0.08f, cy + size * 0.32f)
        lineTo(cx - size * 0.08f, cy + size * 0.08f)
        lineTo(cx - size * 0.45f, cy + size * 0.18f)
        lineTo(cx - size * 0.45f, cy + size * 0.08f) // Left wing tip
        lineTo(cx - size * 0.08f, cy - size * 0.15f)
        close()
    }
    canvas.drawPath(path, fillPaint)
}

private fun drawDarkModeGlyph(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int, isDark: Boolean) {
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    if (isDark) {
        // Crescent Moon
        val r = size * 0.40f
        val moonPath = Path().apply {
            addCircle(cx, cy, r, Path.Direction.CW)
        }
        val cutoutPath = Path().apply {
            addCircle(cx + r * 0.55f, cy - r * 0.35f, r * 0.85f, Path.Direction.CW)
        }
        moonPath.op(cutoutPath, Path.Op.DIFFERENCE)
        canvas.drawPath(moonPath, fillPaint)
    } else {
        // Sun with rays
        val r = size * 0.25f
        canvas.drawCircle(cx, cy, r, fillPaint)

        val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = size * 0.10f
            strokeCap = Paint.Cap.ROUND
        }
        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45).toDouble())
            val x1 = cx + (r * 1.35f) * Math.cos(angle).toFloat()
            val y1 = cy + (r * 1.35f) * Math.sin(angle).toFloat()
            val x2 = cx + (r * 1.75f) * Math.cos(angle).toFloat()
            val y2 = cy + (r * 1.75f) * Math.sin(angle).toFloat()
            canvas.drawLine(x1, y1, x2, y2, rayPaint)
        }
    }
}

private fun drawLocationGlyph(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = size * 0.11f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    val r = size * 0.32f
    val pinY = cy - size * 0.10f
    val path = Path().apply {
        arcTo(RectF(cx - r, pinY - r, cx + r, pinY + r), 140f, 260f)
        lineTo(cx, cy + size * 0.44f)
        close()
    }
    canvas.drawPath(path, paint)
    canvas.drawCircle(cx, pinY, size * 0.11f, fillPaint)
}

private fun drawTimeoutGlyph(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = size * 0.11f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val r = size * 0.42f
    canvas.drawCircle(cx, cy, r, paint)

    // Clock Hands (12 & 3)
    val handPath = Path().apply {
        moveTo(cx, cy - r * 0.65f)
        lineTo(cx, cy)
        lineTo(cx + r * 0.55f, cy)
    }
    canvas.drawPath(handPath, paint)
}

private fun drawBatterySaverGlyph(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = size * 0.10f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    // Battery Body
    val w = size * 0.70f
    val h = size * 0.40f
    val rect = RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f - size * 0.08f, cy + h / 2f)
    canvas.drawRoundRect(rect, size * 0.08f, size * 0.08f, paint)

    // Battery Tip
    val tipRect = RectF(cx + w / 2f - size * 0.08f, cy - h * 0.22f, cx + w / 2f, cy + h * 0.22f)
    canvas.drawRoundRect(tipRect, size * 0.04f, size * 0.04f, fillPaint)

    // Lightning Bolt in center
    val bolt = Path().apply {
        moveTo(cx - size * 0.02f, cy - size * 0.16f)
        lineTo(cx - size * 0.14f, cy + size * 0.02f)
        lineTo(cx - size * 0.02f, cy + size * 0.02f)
        lineTo(cx - size * 0.06f, cy + size * 0.18f)
        lineTo(cx + size * 0.12f, cy - size * 0.02f)
        lineTo(cx + size * 0.00f, cy - size * 0.02f)
        close()
    }
    canvas.drawPath(bolt, fillPaint)
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
        val iconSize = minOf(cellW * 0.38f, cellH * 0.42f)
        val iconCx = cellRect.left + cellW * 0.28f
        val iconCy = cellRect.centerY()
        val iconColor = if (isEnabled) accentColor else secondaryTextColor

        when (item.type) {
            ToggleType.WIFI -> drawWifiGlyph(canvas, iconCx, iconCy, iconSize, iconColor, isEnabled)
            ToggleType.BLUETOOTH -> drawBluetoothGlyph(canvas, iconCx, iconCy, iconSize, iconColor)
            ToggleType.TORCH -> drawTorchGlyph(canvas, iconCx, iconCy, iconSize, iconColor, isEnabled)
            ToggleType.RINGER -> drawSoundGlyph(canvas, iconCx, iconCy, iconSize, iconColor, state.alertSlider)
            ToggleType.AUTOROTATE -> drawAutoRotateGlyph(canvas, iconCx, iconCy, iconSize, iconColor, isEnabled)
            ToggleType.HOTSPOT -> drawHotspotGlyph(canvas, iconCx, iconCy, iconSize, iconColor)
            ToggleType.LOCATION -> drawLocationGlyph(canvas, iconCx, iconCy, iconSize, iconColor)
            ToggleType.DARK_MODE -> drawDarkModeGlyph(canvas, iconCx, iconCy, iconSize, iconColor, isEnabled)
            else -> {}
        }

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
        val iconSize = pillH * 0.40f
        val iconColor = if (isEnabled) accentColor else secondaryTextColor

        when (item.type) {
            ToggleType.WIFI -> drawWifiGlyph(canvas, cx, cy, iconSize, iconColor, isEnabled)
            ToggleType.BLUETOOTH -> drawBluetoothGlyph(canvas, cx, cy, iconSize, iconColor)
            ToggleType.TORCH -> drawTorchGlyph(canvas, cx, cy, iconSize, iconColor, isEnabled)
            ToggleType.RINGER -> drawSoundGlyph(canvas, cx, cy, iconSize, iconColor, state.alertSlider)
            ToggleType.AUTOROTATE -> drawAutoRotateGlyph(canvas, cx, cy, iconSize, iconColor, isEnabled)
            else -> {}
        }

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
    drawWifiGlyph(canvas, iconCx, iconCy, iconSize, if (state.wifi.isEnabled) accentColor else secondaryTextColor, state.wifi.isEnabled)

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
    drawBluetoothGlyph(canvas, btIconCx, btIconCy, iconSize, if (state.bluetooth.isEnabled) accentColor else secondaryTextColor)

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
        val iconSize = cellH * 0.38f
        val color = if (isEnabled) accentColor else secondaryTextColor

        when (item.type) {
            ToggleType.WIFI -> drawWifiGlyph(canvas, iconCx, iconCy, iconSize, color, isEnabled)
            ToggleType.BLUETOOTH -> drawBluetoothGlyph(canvas, iconCx, iconCy, iconSize, color)
            ToggleType.TORCH -> drawTorchGlyph(canvas, iconCx, iconCy, iconSize, color, isEnabled)
            ToggleType.RINGER -> drawSoundGlyph(canvas, iconCx, iconCy, iconSize, color, state.alertSlider)
            else -> {}
        }

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

    // Slider Track Container
    val trackPadH = 14f * scaleFactor
    val trackPadV = 16f * scaleFactor
    val trackRect = RectF(cardRect.left + trackPadH, cardRect.top + trackPadV, cardRect.right - trackPadH, cardRect.bottom - trackPadV)
    val trackRadius = trackRect.height() / 2f

    // Track Background (Sunken groove)
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

        val iconSize = thumbRect.height() * 0.38f
        val iconColor = if (isCurrent) Color.BLACK else secondaryTextColor
        drawSoundGlyph(canvas, nCx, trackRect.centerY() - 6f * scaleFactor, iconSize, iconColor, nMode)

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
    // Top is Ring, Middle is Vibrate, Bottom is Silent
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

        val iconSize = thumbRect.width() * 0.40f
        val iconColor = if (isCurrent) Color.BLACK else secondaryTextColor
        drawSoundGlyph(canvas, trackRect.centerX(), nCy - 4f * scaleFactor, iconSize, iconColor, nMode)

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
    val iconSize = btnRadius * 0.90f
    val iconColor = if (isTorchOn) Color.BLACK else secondaryTextColor
    drawTorchGlyph(canvas, cx, cy, iconSize, iconColor, isTorchOn)

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

    val iconSize = iconR * 1.15f
    val iconColor = if (item.isEnabled) accentColor else secondaryTextColor

    when (item.type) {
        ToggleType.WIFI -> drawWifiGlyph(canvas, iconCx, iconCy, iconSize, iconColor, item.isEnabled)
        ToggleType.BLUETOOTH -> drawBluetoothGlyph(canvas, iconCx, iconCy, iconSize, iconColor)
        ToggleType.TORCH -> drawTorchGlyph(canvas, iconCx, iconCy, iconSize, iconColor, item.isEnabled)
        ToggleType.RINGER -> drawSoundGlyph(canvas, iconCx, iconCy, iconSize, iconColor, AlertSliderMode.RING)
        ToggleType.AUTOROTATE -> drawAutoRotateGlyph(canvas, iconCx, iconCy, iconSize, iconColor, item.isEnabled)
        else -> {}
    }

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
        val iconSize = rect.height() * 0.40f
        val iconColor = if (isEnabled) accentColor else secondaryTextColor

        when (item.type) {
            ToggleType.TIMEOUT -> drawTimeoutGlyph(canvas, iconCx, iconCy, iconSize, iconColor)
            ToggleType.AUTOROTATE -> drawAutoRotateGlyph(canvas, iconCx, iconCy, iconSize, iconColor, isEnabled)
            ToggleType.BATTERY_SAVER -> drawBatterySaverGlyph(canvas, iconCx, iconCy, iconSize, iconColor)
            ToggleType.DARK_MODE -> drawDarkModeGlyph(canvas, iconCx, iconCy, iconSize, iconColor, isEnabled)
            ToggleType.AIRPLANE -> drawAirplaneGlyph(canvas, iconCx, iconCy, iconSize, iconColor)
            ToggleType.HOTSPOT -> drawHotspotGlyph(canvas, iconCx, iconCy, iconSize, iconColor)
            else -> {}
        }

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
    val iconSize = cardRect.width() * 0.38f
    val iconColor = if (item.isEnabled) accentColor else secondaryTextColor

    if (item.type == ToggleType.TORCH) {
        drawTorchGlyph(canvas, cx, cy, iconSize, iconColor, item.isEnabled)
    } else if (item.type == ToggleType.RINGER) {
        drawSoundGlyph(canvas, cx, cy, iconSize, iconColor, alertMode ?: AlertSliderMode.RING)
    }

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
