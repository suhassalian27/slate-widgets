package com.altusix.slate.widgets.health

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.content.ContextCompat
import com.altusix.slate.R
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.createSupersampledCanvas
import com.altusix.slate.utils.getSafeBgColor
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius

// 1. KINETIC TRI-RING (2x2 - PURE CIRCLE)
fun generateHealthKineticTriRingBitmap(context: Context, config: SlateWidgetConfig, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val size = minOf(w, h)
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawOval(cardRect, bgPaint)

    val cx = cardRect.centerX()
    val cy = cardRect.centerY()
    val radius = size / 2f
    val uiScale = (size / (160f * scaleFactor)).coerceIn(0.5f, 3.0f)
    val activity = getDailyActivitySummary(context)

    val ringStroke = scaleFactor * 7.5f * uiScale
    val ringGap = scaleFactor * 4.5f * uiScale

    val rOuter = radius * 0.80f
    val rMid = rOuter - ringStroke - ringGap
    val rInner = rMid - ringStroke - ringGap

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(25, 0, 0, 0) else Color.argb(35, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    // Outer Ring: Steps
    val stepFraction = if (activity.stepTarget > 0) (activity.steps.toFloat() / activity.stepTarget).coerceIn(0f, 1f) else 0f
    trackPaint.strokeWidth = ringStroke
    canvas.drawCircle(cx, cy, rOuter, trackPaint)
    arcPaint.strokeWidth = ringStroke
    arcPaint.color = accentColorInt
    if (stepFraction > 0f) {
        canvas.drawArc(RectF(cx - rOuter, cy - rOuter, cx + rOuter, cy + rOuter), -90f, 360f * stepFraction, false, arcPaint)
    }

    // Middle Ring: Active Burn (500 kcal target)
    val burnFraction = (activity.activeKcal.toFloat() / 500f).coerceIn(0f, 1f)
    trackPaint.strokeWidth = ringStroke
    canvas.drawCircle(cx, cy, rMid, trackPaint)
    arcPaint.strokeWidth = ringStroke
    arcPaint.color = if (isLight) Color.parseColor("#FF9500") else Color.parseColor("#FF9F0A")
    if (burnFraction > 0f) {
        canvas.drawArc(RectF(cx - rMid, cy - rMid, cx + rMid, cy + rMid), -90f, 360f * burnFraction, false, arcPaint)
    }

    // Inner Ring: Active Time (45 min target)
    val timeFraction = (activity.activeMinutes.toFloat() / activity.activeTargetMinutes).coerceIn(0f, 1f)
    trackPaint.strokeWidth = ringStroke
    canvas.drawCircle(cx, cy, rInner, trackPaint)
    arcPaint.strokeWidth = ringStroke
    arcPaint.color = if (isLight) Color.parseColor("#34C759") else Color.parseColor("#30D158")
    if (timeFraction > 0f) {
        canvas.drawArc(RectF(cx - rInner, cy - rInner, cx + rInner, cy + rInner), -90f, 360f * timeFraction, false, arcPaint)
    }

    // Center Readout
    if (!activity.isPermissionGranted) {
        val alertPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            textSize = scaleFactor * 9.5f * uiScale
            typeface = getSlateFont(context, weight = 800)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("TAP TO", cx, cy - (scaleFactor * 2f * uiScale), alertPaint)
        canvas.drawText("ACTIVATE", cx, cy + (scaleFactor * 10f * uiScale), alertPaint)
    } else {
        val stepNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
            textSize = scaleFactor * 20f * uiScale
            typeface = getSlateFont(context, weight = 800)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${activity.steps}", cx, cy + (scaleFactor * 2f * uiScale), stepNumPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            textSize = scaleFactor * 7.5f * uiScale
            typeface = getSlateFont(context, weight = 800)
            textAlign = Paint.Align.CENTER
        }
        val pct = (stepFraction * 100).toInt()
        canvas.drawText("$pct% • STEPS", cx, cy + (scaleFactor * 13f * uiScale), labelPaint)
    }

    return bitmap
}

// 2. PEDOMETER CHRONOGRAPH (2x2)
fun generateHealthPedometerChronoBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
    val leftX = if (isResponsive) margin else (w - cardSize) / 2f
    val topY = if (isResponsive) margin else (h - cardSize) / 2f
    val cardW = if (isResponsive) w - (margin * 2f) else cardSize
    val cardH = if (isResponsive) h - (margin * 2f) else cardSize
    val cardRect = RectF(leftX, topY, leftX + cardW, topY + cardH)

    val effectiveDim = minOf(cardW, cardH)
    val uiScale = (effectiveDim / (160f * scaleFactor)).coerceIn(0.5f, 3.0f)
    val cardCornerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(effectiveDim / 2f)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val cx = cardRect.centerX()
    val topSectionCy = cardRect.top + (cardH * 0.44f)
    val activity = getDailyActivitySummary(context)

    val arcRadius = effectiveDim * 0.36f
    val arcRect = RectF(cx - arcRadius, topSectionCy - arcRadius, cx + arcRadius, topSectionCy + arcRadius)

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(25, 0, 0, 0) else Color.argb(35, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 4.2f * uiScale
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawArc(arcRect, 150f, 240f, false, trackPaint)

    val arcProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 4.2f * uiScale
        strokeCap = Paint.Cap.ROUND
    }
    val stepFraction = if (activity.stepTarget > 0) (activity.steps.toFloat() / activity.stepTarget).coerceIn(0f, 1f) else 0f
    if (stepFraction > 0f) {
        canvas.drawArc(arcRect, 150f, 240f * stepFraction, false, arcProgressPaint)
    }

    val runnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, topSectionCy - arcRadius * 0.62f, scaleFactor * 2.8f * uiScale, runnerPaint)

    val stepCountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
        textSize = scaleFactor * 25f * uiScale
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("${activity.steps}", cx, topSectionCy + (scaleFactor * 7f * uiScale), stepCountPaint)

    val goalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#8E8E93")
        textSize = scaleFactor * 7.5f * uiScale
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("GOAL ${activity.stepTarget}", cx, topSectionCy + (scaleFactor * 17f * uiScale), goalPaint)

    // Bottom Telemetry Cards with Concentric Corner Radii
    val pad = effectiveDim * 0.055f
    val gap = effectiveDim * 0.04f
    val bH = effectiveDim * 0.22f
    val bY = cardRect.bottom - pad - bH
    val bW = (cardW - (pad * 2f) - gap) / 2f

    val vfOuterRad = (cardCornerRadius - pad).coerceAtLeast(scaleFactor * 8f)
    val vfInnerRad = scaleFactor * 8f

    val tileBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(18, 0, 0, 0) else Color.argb(32, 255, 255, 255)
        style = Paint.Style.FILL
    }

    // Left Tile (Bottom-Left corner matches outer widget curve)
    val leftTileRect = RectF(cardRect.left + pad, bY, cardRect.left + pad + bW, bY + bH)
    val leftTileRadii = floatArrayOf(vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfOuterRad, vfOuterRad)
    canvas.drawPath(Path().apply { addRoundRect(leftTileRect, leftTileRadii, Path.Direction.CW) }, tileBgPaint)

    // Right Tile (Bottom-Right corner matches outer widget curve)
    val rightTileRect = RectF(cardRect.right - pad - bW, bY, cardRect.right - pad, bY + bH)
    val rightTileRadii = floatArrayOf(vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfOuterRad, vfOuterRad, vfInnerRad, vfInnerRad)
    canvas.drawPath(Path().apply { addRoundRect(rightTileRect, rightTileRadii, Path.Direction.CW) }, tileBgPaint)

    val subValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
        textSize = scaleFactor * 11.5f * uiScale
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
    }
    val subLblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = scaleFactor * 6.5f * uiScale
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
    }

    canvas.drawText("${activity.distanceKm} KM", leftTileRect.centerX(), leftTileRect.centerY() - (scaleFactor * 0.5f * uiScale), subValPaint)
    canvas.drawText("DISTANCE", leftTileRect.centerX(), leftTileRect.centerY() + (scaleFactor * 8.5f * uiScale), subLblPaint)

    canvas.drawText("${activity.activeKcal}", rightTileRect.centerX(), rightTileRect.centerY() - (scaleFactor * 0.5f * uiScale), subValPaint)
    canvas.drawText("KCAL BURN", rightTileRect.centerX(), rightTileRect.centerY() + (scaleFactor * 8.5f * uiScale), subLblPaint)

    return bitmap
}

// 3. INTERACTIVE HYDRATION CELL (2x2)
fun generateHealthHydrationBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    val margin = scaleFactor * 1.5f
    val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
    val leftX = if (isResponsive) margin else (w - cardSize) / 2f
    val topY = if (isResponsive) margin else (h - cardSize) / 2f
    val cardW = if (isResponsive) w - (margin * 2f) else cardSize
    val cardH = if (isResponsive) h - (margin * 2f) else cardSize
    val cardRect = RectF(leftX, topY, leftX + cardW, topY + cardH)

    val effectiveDim = minOf(cardW, cardH)
    val uiScale = (effectiveDim / (160f * scaleFactor)).coerceIn(0.5f, 3.0f)
    val cardCornerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(effectiveDim / 2f)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = effectiveDim * 0.055f
    val gap = effectiveDim * 0.04f
    val currentMl = getHydrationMl(context)
    val targetMl = 2500
    val fillFraction = (currentMl.toFloat() / targetMl).coerceIn(0f, 1f)

    val halfW = (cardW - (pad * 2f) - gap) / 2f

    val vfOuterRad = (cardCornerRadius - pad).coerceAtLeast(scaleFactor * 8f)
    val vfInnerRad = scaleFactor * 8f

    val capBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(18, 0, 0, 0) else Color.argb(32, 255, 255, 255)
        style = Paint.Style.FILL
    }

    val capsuleRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.left + pad + halfW, cardRect.bottom - pad)
    val capRadii = floatArrayOf(vfOuterRad, vfOuterRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfOuterRad, vfOuterRad)
    val capPath = Path().apply { addRoundRect(capsuleRect, capRadii, Path.Direction.CW) }
    canvas.drawPath(capPath, capBgPaint)

    canvas.save()
    canvas.clipPath(capPath)
    val liquidH = capsuleRect.height() * fillFraction
    val liquidTop = capsuleRect.bottom - liquidH
    val liquidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(110, Color.red(accentColorInt), Color.green(accentColorInt), Color.blue(accentColorInt))
        style = Paint.Style.FILL
    }
    canvas.drawRect(capsuleRect.left, liquidTop, capsuleRect.right, capsuleRect.bottom, liquidPaint)

    if (fillFraction in 0.01f..0.99f) {
        val crestPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.STROKE
            strokeWidth = scaleFactor * 2.0f * uiScale
        }
        canvas.drawLine(capsuleRect.left, liquidTop, capsuleRect.right, liquidTop, crestPaint)
    }
    canvas.restore()

    fun drawVector(resId: Int, cx: Float, cy: Float, maxDim: Float, tint: Int? = null) {
        val drawable = ContextCompat.getDrawable(context, resId)?.mutate() ?: return
        if (tint != null) drawable.setTint(tint)
        val intrinsicW = drawable.intrinsicWidth.toFloat()
        val intrinsicH = drawable.intrinsicHeight.toFloat()
        var drawW = maxDim
        var drawH = maxDim
        if (intrinsicW > 0f && intrinsicH > 0f) {
            val aspect = intrinsicW / intrinsicH
            if (aspect > 1f) drawH = maxDim / aspect else drawW = maxDim * aspect
        }
        val l = (cx - drawW / 2f).toInt()
        val t = (cy - drawH / 2f).toInt()
        val r = (cx + drawW / 2f).toInt()
        val b = (cy + drawH / 2f).toInt()
        drawable.setBounds(l, t, r, b)
        drawable.draw(canvas)
    }

    val dropCx = capsuleRect.centerX()
    val dropCy = capsuleRect.top + (scaleFactor * 26f * uiScale)
    val dropDim = scaleFactor * 16f * uiScale
    drawVector(R.drawable.ic_water_drop, dropCx, dropCy, dropDim, accentColorInt)

    val mlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
        textSize = scaleFactor * 16f * uiScale
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("$currentMl", dropCx, capsuleRect.centerY() + (scaleFactor * 3f * uiScale), mlPaint)

    val capSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#AEAEB2")
        textSize = scaleFactor * 7.5f * uiScale
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("OF 2,500 ML", dropCx, capsuleRect.centerY() + (scaleFactor * 13f * uiScale), capSubPaint)

    val btnH = (capsuleRect.height() - gap) / 2f
    val topBtnRect = RectF(cardRect.left + pad + halfW + gap, cardRect.top + pad, cardRect.right - pad, cardRect.top + pad + btnH)
    val botBtnRect = RectF(cardRect.left + pad + halfW + gap, cardRect.top + pad + btnH + gap, cardRect.right - pad, cardRect.bottom - pad)

    val topBtnRadii = floatArrayOf(vfInnerRad, vfInnerRad, vfOuterRad, vfOuterRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad)
    canvas.drawPath(Path().apply { addRoundRect(topBtnRect, topBtnRadii, Path.Direction.CW) }, capBgPaint)

    val botBtnRadii = floatArrayOf(vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfOuterRad, vfOuterRad, vfInnerRad, vfInnerRad)
    canvas.drawPath(Path().apply { addRoundRect(botBtnRect, botBtnRadii, Path.Direction.CW) }, capBgPaint)

    val btnValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = scaleFactor * 14f * uiScale
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
    }
    val btnSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
        textSize = scaleFactor * 8.5f * uiScale
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    canvas.drawText("+250", topBtnRect.centerX(), topBtnRect.centerY() - (scaleFactor * 1f * uiScale), btnValPaint)
    canvas.drawText("ML", topBtnRect.centerX(), topBtnRect.centerY() + (scaleFactor * 10f * uiScale), btnSubPaint)

    canvas.drawText("+500", botBtnRect.centerX(), botBtnRect.centerY() - (scaleFactor * 1f * uiScale), btnValPaint)
    canvas.drawText("ML", botBtnRect.centerX(), botBtnRect.centerY() + (scaleFactor * 10f * uiScale), btnSubPaint)

    return bitmap
}

// 4. WEEKLY ACTIVITY MATRIX (4x2 BENTO DASHBOARD)
fun generateHealthWeeklyMatrixBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    // 1. Base Plate
    val margin = scaleFactor * 1.5f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val targetRatio = 2.0f
        var targetH = h - (margin * 2f)
        var targetW = targetH * targetRatio
        if (targetW > w - (margin * 2f)) {
            targetW = w - (margin * 2f)
            targetH = targetW / targetRatio
        }
        val leftX = (w - targetW) / 2f
        val topY = (h - targetH) / 2f
        RectF(leftX, topY, leftX + targetW, topY + targetH)
    }

    val cardW = cardRect.width()
    val cardH = cardRect.height()
    val effectiveDim = minOf(cardW, cardH)
    val uiScale = (effectiveDim / (140f * scaleFactor)).coerceIn(0.5f, 3.0f)
    val cardCornerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(effectiveDim / 2f)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    // Dynamic split ratio based on width availability
    val pad = effectiveDim * 0.06f
    val gap = effectiveDim * 0.04f
    val isNarrow = (cardW / cardH) < 1.35f
    val leftSplitRatio = if (isNarrow) 0.38f else 0.34f
    val leftW = (cardW - (pad * 2f) - gap) * leftSplitRatio
    val rightW = (cardW - (pad * 2f) - gap) - leftW

    val vfOuterRad = (cardCornerRadius - pad).coerceAtLeast(scaleFactor * 8f)
    val vfInnerRad = scaleFactor * 8f

    val bentoBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(18, 0, 0, 0) else Color.argb(32, 255, 255, 255)
        style = Paint.Style.FILL
    }

    // 2. Left Bento Hero Tile
    val leftRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.left + pad + leftW, cardRect.bottom - pad)
    val leftRadii = floatArrayOf(vfOuterRad, vfOuterRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfOuterRad, vfOuterRad)
    canvas.drawPath(Path().apply { addRoundRect(leftRect, leftRadii, Path.Direction.CW) }, bentoBgPaint)

    val activity = getDailyActivitySummary(context)
    val lCx = leftRect.centerX()

    // Gauge geometry fits available tile height & width
    val gaugeCy = leftRect.top + (leftRect.height() * 0.40f)
    val gaugeR = minOf(leftRect.width() * 0.36f, leftRect.height() * 0.24f)

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(25, 0, 0, 0) else Color.argb(35, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 3.8f * uiScale
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawCircle(lCx, gaugeCy, gaugeR, trackPaint)

    val stepFraction = if (activity.stepTarget > 0) (activity.steps.toFloat() / activity.stepTarget).coerceIn(0f, 1f) else 0f
    if (stepFraction > 0f) {
        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.STROKE
            strokeWidth = scaleFactor * 3.8f * uiScale
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(RectF(lCx - gaugeR, gaugeCy - gaugeR, lCx + gaugeR, gaugeCy + gaugeR), -90f, 360f * stepFraction, false, arcPaint)
    }

    val heroStepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
        textSize = (gaugeR * 0.62f).coerceIn(scaleFactor * 10f, scaleFactor * 22f)
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("${activity.steps}", lCx, gaugeCy + (heroStepPaint.textSize * 0.22f), heroStepPaint)

    val heroSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = (gaugeR * 0.28f).coerceIn(scaleFactor * 5.5f, scaleFactor * 10f)
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("STEPS", lCx, gaugeCy + (gaugeR * 0.65f), heroSubPaint)

    // Left Sub-telemetry: auto-adapts between single-line and stacked lines
    val maxLeftTextW = leftRect.width() - (scaleFactor * 8f * uiScale)
    val subMetricPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#AEAEB2")
        textSize = scaleFactor * 7.5f * uiScale
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val fullMetricText = "${activity.activeKcal} KCAL • ${activity.distanceKm} KM"
    val singleLineFits = subMetricPaint.measureText(fullMetricText) <= maxLeftTextW

    if (singleLineFits) {
        canvas.drawText(fullMetricText, lCx, leftRect.bottom - (scaleFactor * 8f * uiScale), subMetricPaint)
    } else {
        val line1 = "${activity.activeKcal} KCAL"
        val line2 = "${activity.distanceKm} KM"
        val longestLine = maxOf(subMetricPaint.measureText(line1), subMetricPaint.measureText(line2))
        if (longestLine > maxLeftTextW && longestLine > 0f) {
            subMetricPaint.textSize *= (maxLeftTextW / longestLine) * 0.95f
        }
        val lineSpacing = subMetricPaint.textSize * 1.22f
        val line2Y = leftRect.bottom - (scaleFactor * 7f * uiScale)
        val line1Y = line2Y - lineSpacing
        canvas.drawText(line1, lCx, line1Y, subMetricPaint)
        canvas.drawText(line2, lCx, line2Y, subMetricPaint)
    }

    // 3. Right Bento Matrix Tile
    val rightRect = RectF(leftRect.right + gap, cardRect.top + pad, cardRect.right - pad, cardRect.bottom - pad)
    val rightRadii = floatArrayOf(vfInnerRad, vfInnerRad, vfOuterRad, vfOuterRad, vfOuterRad, vfOuterRad, vfInnerRad, vfInnerRad)
    canvas.drawPath(Path().apply { addRoundRect(rightRect, rightRadii, Path.Direction.CW) }, bentoBgPaint)

    val chartPadX = (rightRect.width() * 0.06f).coerceIn(scaleFactor * 6f, scaleFactor * 14f)
    val availableHeaderW = rightRect.width() - (chartPadX * 2f)

    // Header Collision Engine
    val chartHeaderY = rightRect.top + (scaleFactor * 15f * uiScale)
    val chartTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
        textSize = (scaleFactor * 9f * uiScale).coerceAtLeast(scaleFactor * 7.5f)
        typeface = getSlateFont(context, weight = 800)
    }

    val avgSteps = activity.steps / 7
    val avgPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = (scaleFactor * 7.5f * uiScale).coerceAtLeast(scaleFactor * 6.5f)
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.RIGHT
    }

    val fullTitle = "WEEKLY ACTIVITY"
    val shortTitle = "ACTIVITY"
    val pillText = "AVG ${avgSteps}/D"
    val minGap = scaleFactor * 6f * uiScale

    val fullTitleW = chartTitlePaint.measureText(fullTitle)
    val shortTitleW = chartTitlePaint.measureText(shortTitle)
    val pillW = avgPillPaint.measureText(pillText)

    when {
        fullTitleW + pillW + minGap <= availableHeaderW -> {
            canvas.drawText(fullTitle, rightRect.left + chartPadX, chartHeaderY, chartTitlePaint)
            canvas.drawText(pillText, rightRect.right - chartPadX, chartHeaderY, avgPillPaint)
        }
        shortTitleW + pillW + minGap <= availableHeaderW -> {
            canvas.drawText(shortTitle, rightRect.left + chartPadX, chartHeaderY, chartTitlePaint)
            canvas.drawText(pillText, rightRect.right - chartPadX, chartHeaderY, avgPillPaint)
        }
        else -> {
            // Drop pill entirely when narrow to prevent text smash
            val displayTitle = if (shortTitleW <= availableHeaderW) shortTitle else fullTitle
            val titleTextW = chartTitlePaint.measureText(displayTitle)
            if (titleTextW > availableHeaderW && titleTextW > 0f) {
                chartTitlePaint.textSize *= (availableHeaderW / titleTextW) * 0.95f
            }
            canvas.drawText(displayTitle, rightRect.left + chartPadX, chartHeaderY, chartTitlePaint)
        }
    }

    // 7-Day Bar Matrix Setup
    val days = getWeeklyStepStats(context)
    val labelBaselineY = rightRect.bottom - (scaleFactor * 8f * uiScale)
    val chartBottom = labelBaselineY - (scaleFactor * 10f * uiScale)
    val chartTop = chartHeaderY + (scaleFactor * 10f * uiScale)
    val chartHeight = (chartBottom - chartTop).coerceAtLeast(scaleFactor * 16f)

    val chartAreaW = rightRect.width() - (chartPadX * 2f)
    val barSlotWidth = chartAreaW / days.size
    val barWidth = (barSlotWidth * 0.44f).coerceIn(scaleFactor * 3.5f, scaleFactor * 12f)
    val barCorner = barWidth / 2f

    // Dashed Target Baseline across chart
    val targetFraction = (10000f / 14000f).coerceIn(0.1f, 0.95f)
    val targetLineY = chartBottom - (chartHeight * targetFraction)
    val targetLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(40, 0, 0, 0) else Color.argb(40, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 0.8f * uiScale
        pathEffect = DashPathEffect(floatArrayOf(scaleFactor * 2.5f * uiScale, scaleFactor * 2.5f * uiScale), 0f)
    }
    canvas.drawLine(rightRect.left + chartPadX, targetLineY, rightRect.right - chartPadX, targetLineY, targetLinePaint)

    // Bar Slot Tracks & Live Fills
    val pillarTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(16, 0, 0, 0) else Color.argb(22, 255, 255, 255)
        style = Paint.Style.FILL
    }
    val activeBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    val dayLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = (barSlotWidth * 0.44f).coerceIn(scaleFactor * 5.5f, scaleFactor * 8.5f)
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    for (i in days.indices) {
        val stat = days[i]
        val barCenterX = rightRect.left + chartPadX + (i * barSlotWidth) + (barSlotWidth / 2f)

        // Architectural Track Pillar
        val trackRect = RectF(barCenterX - barWidth / 2f, chartTop, barCenterX + barWidth / 2f, chartBottom)
        canvas.drawRoundRect(trackRect, barCorner, barCorner, pillarTrackPaint)

        // Step Fill
        if (stat.steps > 0) {
            val fillFraction = (stat.steps.toFloat() / 14000f).coerceIn(0.08f, 1f)
            val fillHeight = (chartHeight * fillFraction).coerceAtLeast(barWidth)
            val fillTop = chartBottom - fillHeight
            val fillRect = RectF(barCenterX - barWidth / 2f, fillTop, barCenterX + barWidth / 2f, chartBottom)

            activeBarPaint.color = if (stat.isToday) accentColorInt else if (isLight) Color.argb(80, 0, 0, 0) else Color.argb(110, 255, 255, 255)
            canvas.drawRoundRect(fillRect, barCorner, barCorner, activeBarPaint)
        } else if (stat.isToday) {
            val dotRect = RectF(barCenterX - barWidth / 2f, chartBottom - barWidth, barCenterX + barWidth / 2f, chartBottom)
            activeBarPaint.color = Color.argb(120, Color.red(accentColorInt), Color.green(accentColorInt), Color.blue(accentColorInt))
            canvas.drawRoundRect(dotRect, barCorner, barCorner, activeBarPaint)
        }

        // Day Indicator
        dayLabelPaint.color = if (stat.isToday) accentColorInt else if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#8E8E93")
        canvas.drawText(stat.dayLabel, barCenterX, labelBaselineY, dayLabelPaint)
    }

    return bitmap
}

// 5. 8-GLASS HYDRATION MATRIX (2x2 - MINIMAL WAVE GLASSWARE)
fun generateHealthEightGlassMatrixBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    // 1. Base Plate
    val margin = scaleFactor * 1.5f
    val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
    val leftX = if (isResponsive) margin else (w - cardSize) / 2f
    val topY = if (isResponsive) margin else (h - cardSize) / 2f
    val cardW = if (isResponsive) w - (margin * 2f) else cardSize
    val cardH = if (isResponsive) h - (margin * 2f) else cardSize
    val cardRect = RectF(leftX, topY, leftX + cardW, topY + cardH)

    val effectiveDim = minOf(cardW, cardH)
    val uiScale = (effectiveDim / (160f * scaleFactor)).coerceIn(0.5f, 3.0f)
    val cardCornerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(effectiveDim / 2f)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val pad = effectiveDim * 0.055f
    val gap = effectiveDim * 0.04f
    val currentMl = getHydrationMl(context)
    val targetMl = 2000

    val vfOuterRad = (cardCornerRadius - pad).coerceAtLeast(scaleFactor * 8f)
    val vfInnerRad = scaleFactor * 8f

    val tileBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(18, 0, 0, 0) else Color.argb(32, 255, 255, 255)
        style = Paint.Style.FILL
    }

    // 2. Top Bento Plate
    val topTileH = (cardH - (pad * 2f) - gap) * 0.67f
    val topTileRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.right - pad, cardRect.top + pad + topTileH)
    val topTileRadii = floatArrayOf(vfOuterRad, vfOuterRad, vfOuterRad, vfOuterRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad)
    canvas.drawPath(Path().apply { addRoundRect(topTileRect, topTileRadii, Path.Direction.CW) }, tileBgPaint)

    // Header Telemetry (Collision-guarded)
    val padX = scaleFactor * 12f * uiScale
    val headerY = topTileRect.top + (scaleFactor * 16f * uiScale)
    val availHeaderW = topTileRect.width() - (padX * 2f)

    val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
        textSize = (scaleFactor * 9f * uiScale).coerceIn(scaleFactor * 7.5f, scaleFactor * 13f)
        typeface = getSlateFont(context, weight = 800)
    }

    val volumePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = (scaleFactor * 8.5f * uiScale).coerceIn(scaleFactor * 7f, scaleFactor * 12f)
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.RIGHT
    }

    val titleStr = "HYDRATION"
    val fullVolStr = "$currentMl / $targetMl ML"
    val shortVolStr = "$currentMl ML"

    val titleW = headerTitlePaint.measureText(titleStr)
    val fullVolW = volumePaint.measureText(fullVolStr)
    val minGap = scaleFactor * 8f * uiScale

    canvas.drawText(titleStr, topTileRect.left + padX, headerY, headerTitlePaint)
    val volToDraw = if (titleW + fullVolW + minGap <= availHeaderW) fullVolStr else shortVolStr
    canvas.drawText(volToDraw, topTileRect.right - padX, headerY, volumePaint)

    // 3. 8 Glasses Grid (Clamped by both cell width & height to prevent overlap)
    val gridTopY = headerY + (scaleFactor * 6f * uiScale)
    val gridBotY = topTileRect.bottom - (scaleFactor * 6f * uiScale)
    val gridH = (gridBotY - gridTopY).coerceAtLeast(scaleFactor * 20f)
    val gridW = (topTileRect.width() - (padX * 2f)).coerceAtLeast(scaleFactor * 20f)

    val cellW = gridW / 4f
    val cellH = gridH / 2f

    // Constrain by both width and height with guaranteed minimum margin
    val maxFitW = cellW * 0.72f
    val maxFitH = cellH * 0.78f
    val glassMaxDim = minOf(maxFitW, maxFitH).coerceAtLeast(scaleFactor * 10f)

    fun drawVector(resId: Int, cx: Float, cy: Float, maxDim: Float, tint: Int? = null) {
        val drawable = ContextCompat.getDrawable(context, resId)?.mutate() ?: return
        if (tint != null) drawable.setTint(tint)
        val intrinsicW = drawable.intrinsicWidth.toFloat()
        val intrinsicH = drawable.intrinsicHeight.toFloat()
        var drawW = maxDim
        var drawH = maxDim
        if (intrinsicW > 0f && intrinsicH > 0f) {
            val aspect = intrinsicW / intrinsicH
            if (aspect > 1f) drawH = maxDim / aspect else drawW = maxDim * aspect
        }
        val l = (cx - drawW / 2f).toInt()
        val t = (cy - drawH / 2f).toInt()
        val r = (cx + drawW / 2f).toInt()
        val b = (cy + drawH / 2f).toInt()
        drawable.setBounds(l, t, r, b)
        drawable.draw(canvas)
    }

    val waveResId = context.resources.getIdentifier("ic_glass_wave", "drawable", context.packageName).takeIf { it != 0 }
    val emptyResId = context.resources.getIdentifier("ic_glass_empty", "drawable", context.packageName).takeIf { it != 0 }
    val emptyTint = if (isLight) Color.argb(45, 0, 0, 0) else Color.argb(40, 255, 255, 255)

    val fallbackFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    val fallbackEmptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = emptyTint
        style = Paint.Style.STROKE
        strokeWidth = (scaleFactor * 1.3f * uiScale).coerceIn(1f, scaleFactor * 2.2f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    for (row in 0..1) {
        for (col in 0..3) {
            val index = row * 4 + col
            val cCx = topTileRect.left + padX + (col * cellW) + (cellW / 2f)
            val cCy = gridTopY + (row * cellH) + (cellH / 2f)
            val isFilled = currentMl >= (index + 1) * 250

            if (isFilled) {
                if (waveResId != null) {
                    drawVector(waveResId, cCx, cCy, glassMaxDim, accentColorInt)
                } else {
                    val gH = glassMaxDim
                    val gW = gH * 0.65f
                    val wT = gW * 0.90f
                    val wB = gW * 0.62f
                    val tY = cCy - gH / 2f
                    val bY = cCy + gH / 2f

                    val framePath = Path().apply {
                        moveTo(cCx - wT / 2f, tY)
                        lineTo(cCx + wT / 2f, tY)
                        lineTo(cCx + wB / 2f, bY)
                        lineTo(cCx - wB / 2f, bY)
                        close()
                    }
                    fallbackFillPaint.style = Paint.Style.STROKE
                    fallbackFillPaint.strokeWidth = (scaleFactor * 1.4f * uiScale).coerceIn(1f, scaleFactor * 2.2f)
                    canvas.drawPath(framePath, fallbackFillPaint)

                    val fluidPath = Path().apply {
                        val fTopL = tY + gH * 0.28f
                        val fTopR = tY + gH * 0.20f
                        moveTo(cCx - wT * 0.40f, fTopL)
                        quadTo(cCx - wT * 0.10f, fTopL + gH * 0.08f, cCx, tY + gH * 0.24f)
                        quadTo(cCx + wT * 0.25f, fTopR - gH * 0.08f, cCx + wT * 0.40f, fTopR)
                        lineTo(cCx + wB * 0.38f, bY - scaleFactor * 1.5f)
                        lineTo(cCx - wB * 0.38f, bY - scaleFactor * 1.5f)
                        close()
                    }
                    fallbackFillPaint.style = Paint.Style.FILL
                    canvas.drawPath(fluidPath, fallbackFillPaint)
                }
            } else {
                if (emptyResId != null) {
                    drawVector(emptyResId, cCx, cCy, glassMaxDim, emptyTint)
                } else {
                    val gH = glassMaxDim
                    val gW = gH * 0.65f
                    val wT = gW * 0.90f
                    val wB = gW * 0.62f
                    val tY = cCy - gH / 2f
                    val bY = cCy + gH / 2f

                    val framePath = Path().apply {
                        moveTo(cCx - wT / 2f, tY)
                        lineTo(cCx + wT / 2f, tY)
                        lineTo(cCx + wB / 2f, bY)
                        lineTo(cCx - wB / 2f, bY)
                        close()
                    }
                    canvas.drawPath(framePath, fallbackEmptyPaint)
                }
            }
        }
    }

    // 4. Bottom Tactile Action Buttons
    val btnH = cardRect.bottom - pad - (topTileRect.bottom + gap)
    val btnW = (cardW - (pad * 2f) - gap) / 2f
    val btnTopY = topTileRect.bottom + gap

    val leftBtnRect = RectF(cardRect.left + pad, btnTopY, cardRect.left + pad + btnW, cardRect.bottom - pad)
    val leftBtnRadii = floatArrayOf(vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfOuterRad, vfOuterRad)
    canvas.drawPath(Path().apply { addRoundRect(leftBtnRect, leftBtnRadii, Path.Direction.CW) }, tileBgPaint)

    val rightBtnRect = RectF(cardRect.right - pad - btnW, btnTopY, cardRect.right - pad, cardRect.bottom - pad)
    val rightBtnRadii = floatArrayOf(vfInnerRad, vfInnerRad, vfInnerRad, vfInnerRad, vfOuterRad, vfOuterRad, vfInnerRad, vfInnerRad)
    canvas.drawPath(Path().apply { addRoundRect(rightBtnRect, rightBtnRadii, Path.Direction.CW) }, tileBgPaint)

    val btnValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = minOf(scaleFactor * 13f * uiScale, btnH * 0.36f, btnW * 0.24f).coerceAtLeast(scaleFactor * 9f)
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
    }
    val btnSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
        textSize = minOf(scaleFactor * 7.5f * uiScale, btnH * 0.22f, btnW * 0.16f).coerceAtLeast(scaleFactor * 6f)
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val btnTextSpacing = btnValPaint.textSize * 0.50f
    canvas.drawText("−250", leftBtnRect.centerX(), leftBtnRect.centerY() - (btnTextSpacing * 0.15f), btnValPaint)
    canvas.drawText("ML", leftBtnRect.centerX(), leftBtnRect.centerY() + btnTextSpacing + (scaleFactor * 3f * uiScale), btnSubPaint)

    canvas.drawText("+250", rightBtnRect.centerX(), rightBtnRect.centerY() - (btnTextSpacing * 0.15f), btnValPaint)
    canvas.drawText("ML", rightBtnRect.centerX(), rightBtnRect.centerY() + btnTextSpacing + (scaleFactor * 3f * uiScale), btnSubPaint)

    return bitmap
}

// 6. PURE CIRCLE HYDRO-CHRONO (2x2 - FIXED CIRCLE REMINDER)
fun generateHealthHydroChronoBitmap(context: Context, config: SlateWidgetConfig, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    // 1. Base Circular Plate
    val margin = scaleFactor * 1.5f
    val size = minOf(w - (margin * 2f), h - (margin * 2f))
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawOval(cardRect, bgPaint)

    val cx = cardRect.centerX()
    val cy = cardRect.centerY()
    val radius = size / 2f
    val uiScale = (size / (160f * scaleFactor)).coerceIn(0.5f, 3.0f)

    val currentMl = getHydrationMl(context)
    val targetMl = 2000
    val fillFraction = (currentMl.toFloat() / targetMl).coerceIn(0f, 1f)
    val reminder = getHydroReminderStatus(context)

    // 2. Thick Edge-Mounted Ring Gauge
    val strokeWidth = scaleFactor * 13.5f * uiScale
    val ringPad = scaleFactor * 3.5f * uiScale
    val trackR = radius - (strokeWidth / 2f) - ringPad
    val arcRect = RectF(cx - trackR, cy - trackR, cx + trackR, cy + trackR)

    // Background Inactive Track
    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(22, 0, 0, 0) else Color.argb(28, 255, 255, 255)
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawCircle(cx, cy, trackR, trackPaint)

    // Active Solid Sweep Arc
    if (fillFraction > 0f) {
        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
        }
        val sweepAngle = (360f * fillFraction).coerceIn(2f, 360f)
        canvas.drawArc(arcRect, -90f, sweepAngle, false, arcPaint)

        // Precision Start Anchor Dot (white inner eye on the 12 o'clock anchor cap)
        val startAnchorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.argb(180, 255, 255, 255) else Color.argb(210, 255, 255, 255)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy - trackR, strokeWidth * 0.22f, startAnchorPaint)
    }

    // 3. Center Telemetry & Droplet Icon
    val innerRadius = trackR - (strokeWidth / 2f)

    fun drawVector(resId: Int, vx: Float, vy: Float, maxDim: Float, tint: Int? = null) {
        val drawable = ContextCompat.getDrawable(context, resId)?.mutate() ?: return
        if (tint != null) drawable.setTint(tint)
        val intrinsicW = drawable.intrinsicWidth.toFloat()
        val intrinsicH = drawable.intrinsicHeight.toFloat()
        var drawW = maxDim
        var drawH = maxDim
        if (intrinsicW > 0f && intrinsicH > 0f) {
            val aspect = intrinsicW / intrinsicH
            if (aspect > 1f) drawH = maxDim / aspect else drawW = maxDim * aspect
        }
        val l = (vx - drawW / 2f).toInt()
        val t = (vy - drawH / 2f).toInt()
        val r = (vx + drawW / 2f).toInt()
        val b = (vy + drawH / 2f).toInt()
        drawable.setBounds(l, t, r, b)
        drawable.draw(canvas)
    }

    val dropCy = cy - (innerRadius * 0.44f)
    drawVector(R.drawable.ic_water_drop, cx, dropCy, scaleFactor * 15f * uiScale, accentColorInt)

    // Primary Intake Volume
    val volValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
        textSize = scaleFactor * 24f * uiScale
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("$currentMl", cx, cy + (scaleFactor * 5f * uiScale), volValPaint)

    // Sub-goal Text
    val goalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#8E8E93")
        textSize = scaleFactor * 7.5f * uiScale
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("OF $targetMl ML", cx, cy + (scaleFactor * 15f * uiScale), goalPaint)

    // 4. Reminder Status Capsule
    val badgeH = scaleFactor * 15f * uiScale
    val badgeY = cy + (innerRadius * 0.54f)

    val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = scaleFactor * 7.5f * uiScale
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
    }

    val badgeText = reminder.label
    val textW = badgeTextPaint.measureText(badgeText)
    val badgeW = textW + (scaleFactor * 14f * uiScale)
    val badgeRect = RectF(cx - badgeW / 2f, badgeY - badgeH / 2f, cx + badgeW / 2f, badgeY + badgeH / 2f)

    if (reminder.isDue) {
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(badgeRect, badgeH / 2f, badgeH / 2f, badgeBgPaint)

        val r = Color.red(accentColorInt) / 255f
        val g = Color.green(accentColorInt) / 255f
        val b = Color.blue(accentColorInt) / 255f
        val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
        badgeTextPaint.color = if (luminance > 0.5f) Color.parseColor("#121214") else Color.WHITE
    } else {
        val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.argb(40, 0, 0, 0) else Color.argb(45, 255, 255, 255)
            style = Paint.Style.STROKE
            this.strokeWidth = scaleFactor * 1.0f * uiScale
        }
        canvas.drawRoundRect(badgeRect, badgeH / 2f, badgeH / 2f, badgeBorderPaint)
        badgeTextPaint.color = accentColorInt
    }

    canvas.drawText(badgeText, cx, badgeY + (scaleFactor * 2.6f * uiScale), badgeTextPaint)

    return bitmap
}

// 7. HYDRO ARC DROPLET (2x2 - HORSESHOE ARC GAUGE WITH DYNAMIC WAVE SLOSH)
fun generateHealthHydroArcDropletBitmap(context: Context, config: SlateWidgetConfig, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()

    // 1. Base Circular Plate
    val margin = scaleFactor * 1.5f
    val size = minOf(w - (margin * 2f), h - (margin * 2f))
    val leftX = (w - size) / 2f
    val topY = (h - size) / 2f
    val cardRect = RectF(leftX, topY, leftX + size, topY + size)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawOval(cardRect, bgPaint)

    val cx = cardRect.centerX()
    val cy = cardRect.centerY()
    val radius = size / 2f
    val uiScale = (size / (160f * scaleFactor)).coerceIn(0.5f, 3.0f)

    val currentMl = getHydrationMl(context)
    val targetMl = 2500
    val fillFraction = (currentMl.toFloat() / targetMl).coerceIn(0f, 1f)

    // 2. Concentric Horseshoe Arc Track
    val strokeWidth = scaleFactor * 14f * uiScale
    val arcMargin = scaleFactor * 5f * uiScale
    val trackR = radius - (strokeWidth / 2f) - arcMargin
    val arcRect = RectF(cx - trackR, cy - trackR, cx + trackR, cy + trackR)

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(22, 0, 0, 0) else Color.argb(28, 255, 255, 255)
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawArc(arcRect, 140f, 260f, false, trackPaint)

    if (fillFraction > 0f) {
        val arcProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
        }
        val sweepAngle = (260f * fillFraction).coerceIn(2f, 260f)
        canvas.drawArc(arcRect, 140f, sweepAngle, false, arcProgressPaint)
    }

    // 3. Radial Calibration Ticks
    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(45, 0, 0, 0) else Color.argb(45, 255, 255, 255)
        style = Paint.Style.STROKE
        this.strokeWidth = scaleFactor * 1.3f * uiScale
        strokeCap = Paint.Cap.ROUND
    }
    val tickOuterR = trackR - (strokeWidth / 2f) - (scaleFactor * 3.5f * uiScale)
    val tickLen = scaleFactor * 3.2f * uiScale
    for (deg in 140..400 step 26) {
        val angleRad = Math.toRadians(deg.toDouble())
        val x1 = cx + ((tickOuterR - tickLen) * Math.cos(angleRad)).toFloat()
        val y1 = cy + ((tickOuterR - tickLen) * Math.sin(angleRad)).toFloat()
        val x2 = cx + (tickOuterR * Math.cos(angleRad)).toFloat()
        val y2 = cy + (tickOuterR * Math.sin(angleRad)).toFloat()
        canvas.drawLine(x1, y1, x2, y2, tickPaint)
    }

    // 4. Teardrop Reservoir (Slightly larger and lifted for bottom room)
    val dropTargetH = trackR * 0.98f
    val dropScale = dropTargetH / 222.55f
    val dropCy = cy - (scaleFactor * 8f * uiScale)

    val dropPath = try {
        androidx.core.graphics.PathParser.createPathFromPathData(
            "M174,47.75a254.19,254.19,0,0,0-41.45-38.3,8,8,0,0,0-9.18,0A254.19,254.19,0,0,0,82,47.75C54.51,79.32,40,112.6,40,144a88,88,0,0,0,176,0C216,112.6,201.49,79.32,174,47.75z"
        )
    } catch (e: Exception) {
        Path().apply {
            moveTo(128f, 9.45f)
            cubicTo(145f, 25f, 216f, 90f, 216f, 144f)
            arcTo(RectF(40f, 56f, 216f, 232f), 0f, 180f, false)
            cubicTo(40f, 90f, 111f, 25f, 128f, 9.45f)
            close()
        }
    }

    val transformMatrix = android.graphics.Matrix().apply {
        postTranslate(-128f, -121f)
        postScale(dropScale, dropScale)
        postTranslate(cx, dropCy)
    }
    dropPath.transform(transformMatrix)

    val dropBounds = RectF()
    dropPath.computeBounds(dropBounds, true)
    val dropBotY = dropBounds.bottom
    val dropH = dropBounds.height()
    val dropW = dropBounds.width()

    val dropBackingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(18, 0, 0, 0) else Color.argb(26, 255, 255, 255)
        style = Paint.Style.FILL
    }
    canvas.drawPath(dropPath, dropBackingPaint)

    // Dynamic Wave Shading
    canvas.save()
    canvas.clipPath(dropPath)

    val fluidLevelY = dropBotY - (dropH * fillFraction)
    val waveW = dropW * 1.5f

    val sipIndex = currentMl / 250
    if (sipIndex % 2 != 0) {
        canvas.scale(-1f, 1f, cx, fluidLevelY)
    }

    val waveAmp1 = scaleFactor * (4.2f + ((sipIndex % 3) * 0.6f)) * uiScale
    val waveAmp2 = scaleFactor * (3.8f + (((sipIndex + 1) % 3) * 0.5f)) * uiScale

    val backWavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, Color.red(accentColorInt), Color.green(accentColorInt), Color.blue(accentColorInt))
        style = Paint.Style.FILL
    }
    val backWavePath = Path().apply {
        moveTo(cx - waveW, dropBotY + scaleFactor * 10f)
        lineTo(cx - waveW, fluidLevelY - scaleFactor * 2f * uiScale)
        quadTo(cx - dropW * 0.35f, fluidLevelY - waveAmp1, cx + dropW * 0.12f, fluidLevelY)
        quadTo(cx + dropW * 0.55f, fluidLevelY + waveAmp2, cx + waveW, fluidLevelY)
        lineTo(cx + waveW, dropBotY + scaleFactor * 10f)
        close()
    }
    canvas.drawPath(backWavePath, backWavePaint)

    val frontWavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    val frontWavePath = Path().apply {
        moveTo(cx - waveW, dropBotY + scaleFactor * 10f)
        lineTo(cx - waveW, fluidLevelY)
        quadTo(cx - dropW * 0.35f, fluidLevelY + waveAmp1, cx, fluidLevelY)
        quadTo(cx + dropW * 0.35f, fluidLevelY - waveAmp1, cx + waveW, fluidLevelY)
        lineTo(cx + waveW, dropBotY + scaleFactor * 10f)
        close()
    }
    canvas.drawPath(frontWavePath, frontWavePaint)
    canvas.restore()

    val dropOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(32, 0, 0, 0) else Color.argb(40, 255, 255, 255)
        style = Paint.Style.STROKE
        this.strokeWidth = scaleFactor * 1.2f * uiScale
    }
    canvas.drawPath(dropPath, dropOutlinePaint)

    // 5. Lower Amount Telemetry (Shifted further down into opening)
    val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
        textSize = scaleFactor * 22f * uiScale
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
    }
    val numY = cy + (trackR * 0.77f)
    canvas.drawText("$currentMl", cx, numY, numPaint)

    val goalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#8E8E93")
        textSize = scaleFactor * 8.5f * uiScale
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("/$targetMl ML", cx, numY + (scaleFactor * 10f * uiScale), goalPaint)

    return bitmap
}

// 8. LANDSCAPE RIDGE PEDOMETER (2x2 - SCANDINAVIAN MOUNTAIN ELEVATION)
fun generateHealthLandscapeRidgeBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryTextColor = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryTextColor = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#AEAEB2")

    // 1. Base Plate Geometry
    val margin = scaleFactor * 1.5f
    val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
    val leftX = if (isResponsive) margin else (w - cardSize) / 2f
    val topY = if (isResponsive) margin else (h - cardSize) / 2f
    val cardW = if (isResponsive) w - (margin * 2f) else cardSize
    val cardH = if (isResponsive) h - (margin * 2f) else cardSize
    val cardRect = RectF(leftX, topY, leftX + cardW, topY + cardH)

    val effectiveDim = minOf(cardW, cardH)
    val uiScale = (effectiveDim / (160f * scaleFactor)).coerceIn(0.5f, 3.0f)
    val cardCornerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(effectiveDim / 2f)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, bgPaint)

    val padX = cardW * 0.10f
    val padY = cardH * 0.10f
    val bottomBarH = scaleFactor * 26f * uiScale
    val hillBaseY = cardRect.bottom - bottomBarH

    // 2. Smooth Topographic Mountain Ridges
    canvas.save()
    val cardClipPath = Path().apply { addRoundRect(cardRect, cardCornerRadius, cardCornerRadius, Path.Direction.CW) }
    canvas.clipPath(cardClipPath)

    val rR = Color.red(accentColorInt)
    val rG = Color.green(accentColorInt)
    val rB = Color.blue(accentColorInt)
    val cL = cardRect.left
    val cR = cardRect.right

    // Layer 1: Background Tall Mountain Summit
    val backHillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(if (isLight) 60 else 45, rR, rG, rB)
        style = Paint.Style.FILL
    }
    val backHillPath = Path().apply {
        moveTo(cL, hillBaseY)
        cubicTo(
            cL + cardW * 0.28f, hillBaseY - cardH * 0.02f,
            cL + cardW * 0.44f, hillBaseY - cardH * 0.14f,
            cL + cardW * 0.58f, hillBaseY - cardH * 0.20f
        )
        cubicTo(
            cL + cardW * 0.68f, hillBaseY - cardH * 0.26f,
            cL + cardW * 0.74f, hillBaseY - cardH * 0.54f,
            cL + cardW * 0.82f, hillBaseY - cardH * 0.54f
        )
        cubicTo(
            cL + cardW * 0.90f, hillBaseY - cardH * 0.54f,
            cL + cardW * 0.95f, hillBaseY - cardH * 0.36f,
            cR, hillBaseY - cardH * 0.30f
        )
        lineTo(cR, hillBaseY)
        close()
    }
    canvas.drawPath(backHillPath, backHillPaint)

    // Layer 2: Mid-Range Rolling Ridge
    val midHillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(if (isLight) 115 else 90, rR, rG, rB)
        style = Paint.Style.FILL
    }
    val midHillPath = Path().apply {
        moveTo(cL, hillBaseY)
        cubicTo(
            cL + cardW * 0.16f, hillBaseY - cardH * 0.04f,
            cL + cardW * 0.26f, hillBaseY - cardH * 0.12f,
            cL + cardW * 0.38f, hillBaseY - cardH * 0.14f
        )
        cubicTo(
            cL + cardW * 0.48f, hillBaseY - cardH * 0.16f,
            cL + cardW * 0.56f, hillBaseY - cardH * 0.30f,
            cL + cardW * 0.68f, hillBaseY - cardH * 0.32f
        )
        cubicTo(
            cL + cardW * 0.78f, hillBaseY - cardH * 0.34f,
            cL + cardW * 0.86f, hillBaseY - cardH * 0.18f,
            cR, hillBaseY - cardH * 0.18f
        )
        lineTo(cR, hillBaseY)
        close()
    }
    canvas.drawPath(midHillPath, midHillPaint)

    // Layer 3: Foreground Low Slope
    val foreHillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(if (isLight) 190 else 160, rR, rG, rB)
        style = Paint.Style.FILL
    }
    val foreHillPath = Path().apply {
        moveTo(cL, hillBaseY)
        cubicTo(
            cL + cardW * 0.12f, hillBaseY - cardH * 0.02f,
            cL + cardW * 0.24f, hillBaseY - cardH * 0.08f,
            cL + cardW * 0.34f, hillBaseY - cardH * 0.09f
        )
        cubicTo(
            cL + cardW * 0.44f, hillBaseY - cardH * 0.10f,
            cL + cardW * 0.52f, hillBaseY - cardH * 0.04f,
            cL + cardW * 0.64f, hillBaseY - cardH * 0.06f
        )
        cubicTo(
            cL + cardW * 0.74f, hillBaseY - cardH * 0.08f,
            cL + cardW * 0.86f, hillBaseY - cardH * 0.16f,
            cR, hillBaseY - cardH * 0.06f
        )
        lineTo(cR, hillBaseY)
        close()
    }
    canvas.drawPath(foreHillPath, foreHillPaint)
    canvas.restore()

    // 3. Aspect-Preserving Vector Helper
    fun drawVector(resName: String, cx: Float, cy: Float, maxDim: Float, tint: Int? = null, flipX: Boolean = false, forcedAspect: Float? = null) {
        val resId = context.resources.getIdentifier(resName, "drawable", context.packageName).takeIf { it != 0 } ?: return
        val drawable = ContextCompat.getDrawable(context, resId)?.mutate() ?: return
        if (tint != null) drawable.setTint(tint)
        val intrinsicW = drawable.intrinsicWidth.toFloat()
        val intrinsicH = drawable.intrinsicHeight.toFloat()
        val aspect = forcedAspect ?: if (intrinsicW > 0f && intrinsicH > 0f) intrinsicW / intrinsicH else 1.0f

        var drawW = maxDim
        var drawH = maxDim
        if (aspect > 1f) {
            drawH = maxDim / aspect
        } else {
            drawW = maxDim * aspect
        }

        val l = (cx - drawW / 2f).toInt()
        val t = (cy - drawH / 2f).toInt()
        val r = (cx + drawW / 2f).toInt()
        val b = (cy + drawH / 2f).toInt()
        drawable.setBounds(l, t, r, b)

        canvas.save()
        if (flipX) {
            canvas.scale(-1f, 1f, cx, cy)
        }
        drawable.draw(canvas)
        canvas.restore()
    }

    // 4. Circular Badge with Dynamic Contrast Shoe Tint
    val badgeR = scaleFactor * 15f * uiScale
    val badgeCx = cardRect.left + padX + badgeR
    val badgeCy = cardRect.top + padY + badgeR

    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    canvas.drawCircle(badgeCx, badgeCy, badgeR, badgePaint)

    // Dynamic contrast calculation for shoe icon against accent plate
    val badgeLum = (0.2126f * (rR / 255f)) + (0.7152f * (rG / 255f)) + (0.0722f * (rB / 255f))
    val shoeTint = if (badgeLum > 0.58f) Color.parseColor("#121214") else Color.WHITE

    val shoeSize = badgeR * 1.25f
    drawVector(
        resName = "ic_sport_shoe",
        cx = badgeCx,
        cy = badgeCy,
        maxDim = shoeSize,
        tint = shoeTint,
        flipX = true
    )

    // 5. Step Count & Label
    val activity = getDailyActivitySummary(context)
    val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryTextColor
        textSize = (scaleFactor * 25f * uiScale).coerceAtMost(cardW * 0.22f)
        typeface = getSlateFont(context, weight = 800)
    }
    val stepsFormatted = String.format(java.util.Locale.US, "%,d", activity.steps)
    val numY = badgeCy + badgeR + (numPaint.textSize * 1.05f)
    canvas.drawText(stepsFormatted, cardRect.left + padX, numY, numPaint)

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryTextColor
        textSize = scaleFactor * 10f * uiScale
        typeface = getSlateFont(context, weight = 600)
    }
    canvas.drawText("steps", cardRect.left + padX, numY + (scaleFactor * 12.5f * uiScale), labelPaint)

    // 6. Bottom Telemetry Bar
    val bottomBarCenterY = hillBaseY + (bottomBarH / 2f)

    val distPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        textSize = scaleFactor * 10f * uiScale
        typeface = getSlateFont(context, weight = 700)
    }
    val fontMetrics = distPaint.fontMetrics
    val textBaselineY = bottomBarCenterY - ((fontMetrics.ascent + fontMetrics.descent) / 2f)
    canvas.drawText("${activity.distanceKm} km", cardRect.left + padX, textBaselineY, distPaint)

    // Proportional Map Marker (Forced 384:512 ratio safeguard)
    val markerH = scaleFactor * 13f * uiScale
    val markerW = markerH * (384f / 512f)
    val markerCx = cardRect.right - padX - (markerW / 2f)
    drawVector(
        resName = "ic_map_marker_alt",
        cx = markerCx,
        cy = bottomBarCenterY,
        maxDim = markerH,
        tint = accentColorInt,
        flipX = false,
        forcedAspect = (384f / 512f)
    )

    return bitmap
}