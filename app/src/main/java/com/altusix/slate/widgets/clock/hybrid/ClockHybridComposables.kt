package com.altusix.slate.widgets.clock.hybrid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.altusix.slate.R
import com.altusix.slate.data.local.SlateWidgetConfig

// --- SHARED GEOMETRY & THEMING HELPERS ---

private fun getSafeBgColor(config: SlateWidgetConfig): Int {
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val rawHex = config.backgroundColorHex.toInt()
    val r = (rawHex shr 16) and 0xFF
    val g = (rawHex shr 8) and 0xFF
    val b = rawHex and 0xFF
    return Color.argb(alphaInt, r, g, b)
}

private fun getStandardCornerRadius(density: Float): Float = 22f * density

fun getSlateFont(
    context: Context,
    weight: Int = 400,
    isItalic: Boolean = false
): Typeface {
    val fontRes = if (isItalic) {
        R.font.inter_tight_italic_variable
    } else {
        R.font.inter_tight_variable
    }

    return try {
        val baseTypeface = ResourcesCompat.getFont(context, fontRes) ?: Typeface.DEFAULT
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            Typeface.create(baseTypeface, weight, isItalic)
        } else {
            val style = when {
                weight >= 700 && isItalic -> Typeface.BOLD_ITALIC
                weight >= 700 -> Typeface.BOLD
                isItalic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
            Typeface.create(baseTypeface, style)
        }
    } catch (_: Exception) {
        Typeface.create(Typeface.SANS_SERIF, if (weight >= 700) Typeface.BOLD else Typeface.NORMAL)
    }
}

// 1. ANALOG DIGITAL SPLIT HYBRID (4x2 / Adaptive Horizontal or Vertical Layout)
fun generateAnalogDigitalSplitHybridClockBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast((140 * density).toInt())
    val h = (hDp * density).toInt().coerceAtLeast((70 * density).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    // 1. Calculate Card Bounds (Responsive: 100% boundary; Fixed: 2:1 ratio container)
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

    val cardRadius = getStandardCornerRadius(density)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    // =========================================================================
    // PADDING & LAYOUT CONTROLS
    // =========================================================================
    val padX = cardRect.width() * 0.07f   // Outer Horizontal Margin (Left & Right equal)
    val padY = cardRect.height() * 0.08f  // Outer Vertical Margin (Top & Bottom equal)
    val gap = cardRect.width() * 0.04f    // Gap between Analog and Digital components

    val timeState = HybridClockTimeState.now()
    val hourStr = timeState.hour24.toString().padStart(2, '0')
    val minStr = timeState.minute.toString().padStart(2, '0')
    val digitalTimeStr = "$hourStr:$minStr"
    val dateStr = "${timeState.dayOfWeek}, ${timeState.dayOfMonth} ${timeState.monthName}"

    val aspect = cardRect.width() / cardRect.height()

    // Helper function to render analog clock dial & hands
    fun drawAnalogDial(centerX: Float, centerY: Float, radius: Float) {
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            style = Paint.Style.FILL
        }
        for (i in 0 until 12) {
            val angleRad = Math.toRadians((i * 30).toDouble())
            val isMajor = i % 3 == 0
            val markerRadius = if (isMajor) 2.2f * density else 1.2f * density
            val dist = radius * 0.84f
            val mx = centerX + (Math.cos(angleRad) * dist).toFloat()
            val my = centerY + (Math.sin(angleRad) * dist).toFloat()
            canvas.drawCircle(mx, my, markerRadius, tickPaint)
        }

        val hourAngleRad = Math.toRadians(((timeState.hour12 % 12 + timeState.minute / 60f) * 30f - 90f).toDouble())
        val minAngleRad = Math.toRadians(((timeState.minute + timeState.second / 60f) * 6f - 90f).toDouble())

        // Hour Hand
        val hourHandLen = radius * 0.48f
        val hourHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            style = Paint.Style.STROKE
            strokeWidth = 3f * density
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(
            centerX, centerY,
            centerX + (Math.cos(hourAngleRad) * hourHandLen).toFloat(),
            centerY + (Math.sin(hourAngleRad) * hourHandLen).toFloat(),
            hourHandPaint
        )

        // Minute Hand
        val minHandLen = radius * 0.72f
        val minHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(
            centerX, centerY,
            centerX + (Math.cos(minAngleRad) * minHandLen).toFloat(),
            centerY + (Math.sin(minAngleRad) * minHandLen).toFloat(),
            minHandPaint
        )

        // Center Cap Dot
        val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, 3f * density, capPaint)
    }

    if (aspect >= 1.20f) {
        // =========================================================================
        // WIDE / HORIZONTAL SPLIT (Side-by-Side)
        // Exact Symmetrical Padding: Left margin = padX, Right margin = padX
        // =========================================================================
        val usableH = cardRect.height() - (padY * 2f)
        val usableW = cardRect.width() - (padX * 2f) - gap

        val dialDiameter = minOf(usableW * 0.42f, usableH)
        val dialRadius = dialDiameter / 2f
        val dialCenterX = cardRect.left + padX + dialRadius
        val dialCenterY = cardRect.centerY()

        drawAnalogDial(dialCenterX, dialCenterY, dialRadius)

        val digitalLeftX = cardRect.left + padX + dialDiameter + gap
        val digitalRightX = cardRect.right - padX
        val digitalAreaW = digitalRightX - digitalLeftX

        val maxTimeH = usableH * 0.5f
        val refTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getSlateFont(context, weight = 700)
            textSize = 100f
        }
        val refTimeW = refTimePaint.measureText(digitalTimeStr)
        val timeTextSize = minOf(maxTimeH, 100f * (digitalAreaW / refTimeW)).coerceAtLeast(11f * density)

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = timeTextSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.LEFT
        }

        val timeBounds = Rect()
        timePaint.getTextBounds(digitalTimeStr, 0, digitalTimeStr.length, timeBounds)

        var dateTextSize = (timeTextSize * 0.28f).coerceAtLeast(10f * density)
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            textSize = dateTextSize
            typeface = getSlateFont(context, weight = 400)
            textAlign = Paint.Align.LEFT
            letterSpacing = 0.04f
        }

        if (datePaint.measureText(dateStr) > digitalAreaW) {
            dateTextSize *= (digitalAreaW / datePaint.measureText(dateStr))
            datePaint.textSize = dateTextSize
        }

        val dateBounds = Rect()
        datePaint.getTextBounds(dateStr, 0, dateStr.length, dateBounds)

        val blockGap = timeTextSize * 0.10f
        val totalRightH = timeBounds.height() + blockGap + dateBounds.height()
        val rightTopY = cardRect.centerY() - (totalRightH / 2f)

        val timeY = rightTopY + timeBounds.height()
        val dateY = timeY + blockGap + dateBounds.height()

        canvas.drawText(digitalTimeStr, digitalLeftX, timeY, timePaint)
        canvas.drawText(dateStr, digitalLeftX, dateY, datePaint)

    } else {
        // =========================================================================
        // TALL / SQUARE LAYOUT (Vertical Stack: Dial Top, Digital Time + Date Bottom)
        // Prevents text overflow/squishing in 2x2, 1x2, or square slots
        // =========================================================================
        val usableW = cardRect.width() - (padX * 2f)
        val usableH = cardRect.height() - (padY * 2f)

        val dialAreaH = usableH * 0.58f
        val digitalAreaH = usableH - dialAreaH

        val dialDiameter = minOf(usableW, dialAreaH)
        val dialRadius = dialDiameter / 2f
        val dialCenterX = cardRect.centerX()
        val dialCenterY = cardRect.top + padY + (dialAreaH / 2f)

        drawAnalogDial(dialCenterX, dialCenterY, dialRadius)

        val refTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getSlateFont(context, weight = 700)
            textSize = 100f
        }
        val refTimeW = refTimePaint.measureText(digitalTimeStr)

        val maxTimeH = digitalAreaH * 0.50f
        val timeTextSize = minOf(maxTimeH, 100f * (usableW / refTimeW)).coerceAtLeast(12f * density)

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = timeTextSize
            typeface = getSlateFont(context, weight = 700)
            textAlign = Paint.Align.CENTER
        }

        val timeBounds = Rect()
        timePaint.getTextBounds(digitalTimeStr, 0, digitalTimeStr.length, timeBounds)

        var dateTextSize = (timeTextSize * 0.28f).coerceAtLeast(10f * density)
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            textSize = dateTextSize
            typeface = getSlateFont(context, weight = 600)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.04f
        }

        if (datePaint.measureText(dateStr) > usableW) {
            dateTextSize *= (usableW / datePaint.measureText(dateStr))
            datePaint.textSize = dateTextSize
        }

        val dateBounds = Rect()
        datePaint.getTextBounds(dateStr, 0, dateStr.length, dateBounds)

        val blockGap = timeTextSize * 0.10f
        val digitalTopY = cardRect.top + padY + dialAreaH + ((digitalAreaH - (timeBounds.height() + blockGap + dateBounds.height())) / 2f)

        val timeY = digitalTopY + timeBounds.height()
        val dateY = timeY + blockGap + dateBounds.height()

        canvas.drawText(digitalTimeStr, cardRect.centerX(), timeY, timePaint)
        canvas.drawText(dateStr, cardRect.centerX(), dateY, datePaint)
    }

    return bitmap
}
