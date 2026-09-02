package com.altusix.slate.widgets.google

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import com.altusix.slate.R
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.createSupersampledCanvas
import com.altusix.slate.utils.getSafeBgColor

// 1. GOOGLE SEARCH CAPSULE (4x1)
fun generateGoogleSearchCapsuleBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val iconColor = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE

    val margin = scaleFactor * 2f
    val barHeight = (h - (margin * 2f)).coerceIn(scaleFactor * 58f, scaleFactor * 76f)
    val topY = (h - barHeight) / 2f
    val cardRect = RectF(margin, topY, w - margin, topY + barHeight)

    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    val capsuleRadius = cardRect.height() / 2f
    canvas.drawRoundRect(cardRect, capsuleRadius, capsuleRadius, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(20, 0, 0, 0) else Color.argb(28, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 1.2f
    }
    canvas.drawRoundRect(cardRect, capsuleRadius, capsuleRadius, borderPaint)

    val innerH = cardRect.height()
    val sideInset = innerH * 0.44f

    // 1. Google 'G' Logo (Accented)
    val gSize = innerH * 0.44f
    val gCx = cardRect.left + sideInset
    val gLeft = (gCx - (gSize / 2f)).toInt()
    val gTop = (cardRect.centerY() - (gSize / 2f)).toInt()
    ContextCompat.getDrawable(context, R.drawable.ic_google_logo)?.mutate()?.apply {
        setTint(accentColorInt)
        setBounds(gLeft, gTop, (gLeft + gSize).toInt(), (gTop + gSize).toInt())
        draw(canvas)
    }

    // 2. Trailing Icons with Generous Spacing (~52dp step)
    val iconSize = (innerH * 0.36f).coerceIn(scaleFactor * 18f, scaleFactor * 24f)
    val iconStep = (innerH * 0.80f).coerceIn(scaleFactor * 48f, scaleFactor * 58f)
    val gRightBoundary = gCx + (gSize / 2f) + (scaleFactor * 24f)

    val lensCx = cardRect.right - sideInset
    val geminiCx = lensCx - iconStep
    val micCx = lensCx - (iconStep * 2f)

    // Dynamic degradation flags: drop icons cleanly if widget is squeezed thin
    val showLens = wDp >= 140 && (lensCx - iconSize / 2f) > gRightBoundary
    val showGemini = wDp >= 210 && (geminiCx - iconSize / 2f) > gRightBoundary
    val showMic = wDp >= 280 && (micCx - iconSize / 2f) > gRightBoundary

    if (showLens) {
        val lensLeft = (lensCx - iconSize / 2f).toInt()
        val lensTop = (cardRect.centerY() - iconSize / 2f).toInt()
        ContextCompat.getDrawable(context, R.drawable.ic_google_lens)?.mutate()?.apply {
            setTint(iconColor)
            setBounds(lensLeft, lensTop, (lensLeft + iconSize).toInt(), (lensTop + iconSize).toInt())
            draw(canvas)
        }
    }

    if (showGemini) {
        val geminiLeft = (geminiCx - iconSize / 2f).toInt()
        val geminiTop = (cardRect.centerY() - iconSize / 2f).toInt()
        ContextCompat.getDrawable(context, R.drawable.ic_google_gemini)?.mutate()?.apply {
            setTint(iconColor)
            setBounds(geminiLeft, geminiTop, (geminiLeft + iconSize).toInt(), (geminiTop + iconSize).toInt())
            draw(canvas)
        }
    }

    if (showMic) {
        val micLeft = (micCx - iconSize / 2f).toInt()
        val micTop = (cardRect.centerY() - iconSize / 2f).toInt()
        ContextCompat.getDrawable(context, R.drawable.ic_mic)?.mutate()?.apply {
            setTint(iconColor)
            setBounds(micLeft, micTop, (micLeft + iconSize).toInt(), (micTop + iconSize).toInt())
            draw(canvas)
        }
    }

    return bitmap
}