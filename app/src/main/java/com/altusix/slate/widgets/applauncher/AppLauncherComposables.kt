package com.altusix.slate.widgets.applauncher

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.altusix.slate.data.local.SlateWidgetConfig
import kotlin.math.cos
import kotlin.math.sin

private fun getStandardCornerRadius(density: Float): Float = 22f * density

fun getShapePath(shape: LauncherShape, rect: RectF, density: Float): Path {
    val path = Path()
    val cx = rect.centerX()
    val cy = rect.centerY()
    val radius = minOf(rect.width(), rect.height()) / 2f

    when (shape) {
        LauncherShape.SQUIRCLE -> {
            val squircleRadius = getStandardCornerRadius(density)
            path.addRoundRect(rect, squircleRadius, squircleRadius, Path.Direction.CW)
        }
        LauncherShape.CIRCLE -> {
            path.addCircle(cx, cy, radius, Path.Direction.CW)
        }
        LauncherShape.M3_PENTAGON -> {
            for (i in 0 until 5) {
                val angle = Math.toRadians((72 * i - 90).toDouble())
                val x = cx + (radius * cos(angle)).toFloat()
                val y = cy + (radius * sin(angle)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
        }
        LauncherShape.M3_OCTAGON -> {
            for (i in 0 until 8) {
                val angle = Math.toRadians((45 * i - 22.5).toDouble())
                val x = cx + (radius * cos(angle)).toFloat()
                val y = cy + (radius * sin(angle)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
        }
        LauncherShape.M3_DIAMOND -> {
            path.moveTo(cx, rect.top)
            path.lineTo(rect.right, cy)
            path.lineTo(cx, rect.bottom)
            path.lineTo(rect.left, cy)
            path.close()
        }
        LauncherShape.M3_FLOWER -> {
            val petals = 12
            for (i in 0 until petals * 2) {
                val r = if (i % 2 == 0) radius else radius * 0.78f
                val angle = Math.toRadians((360.0 / (petals * 2)) * i)
                val x = cx + (r * cos(angle)).toFloat()
                val y = cy + (r * sin(angle)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
        }
        LauncherShape.M3_CLOVER -> {
            val lobeR = radius * 0.52f
            path.addCircle(cx - lobeR * 0.5f, cy - lobeR * 0.5f, lobeR, Path.Direction.CW)
            path.addCircle(cx + lobeR * 0.5f, cy - lobeR * 0.5f, lobeR, Path.Direction.CW)
            path.addCircle(cx - lobeR * 0.5f, cy + lobeR * 0.5f, lobeR, Path.Direction.CW)
            path.addCircle(cx + lobeR * 0.5f, cy + lobeR * 0.5f, lobeR, Path.Direction.CW)
        }
        LauncherShape.BLOB -> {
            path.moveTo(cx, rect.top)
            path.cubicTo(rect.right, rect.top, rect.right, cy, rect.right, rect.bottom)
            path.cubicTo(cx, rect.bottom, rect.left, rect.bottom, rect.left, cy)
            path.cubicTo(rect.left, rect.top, cx, rect.top, cx, rect.top)
            path.close()
        }
        LauncherShape.PIXEL_STAR -> {
            val points = 8
            for (i in 0 until points * 2) {
                val r = if (i % 2 == 0) radius else radius * 0.55f
                val angle = Math.toRadians((360.0 / (points * 2)) * i - 90)
                val x = cx + (r * cos(angle)).toFloat()
                val y = cy + (r * sin(angle)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
        }
    }
    return path
}

// 1. Adaptive Shape App Launcher (1x1)
fun generateAdaptiveLauncherBitmap(
    context: Context,
    slateConfig: SlateWidgetConfig,
    launcherConfig: AppLauncherWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = slateConfig.themeMode == "LIGHT"
    val cardBg = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()

    // 1. RESPONSIVE vs FIXED CALCULATION
    val rect = if (launcherConfig.isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val cardSize = minOf(w, h).toFloat()
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cardBg
        style = Paint.Style.FILL
    }

    val path = getShapePath(launcherConfig.shape, rect, density)
    canvas.drawPath(path, bgPaint)

    // Render Content (App Icon / Emoji / Vector / Text) with accentColor tint
    renderLauncherContent(context, canvas, rect, launcherConfig, accentColor, density)

    return bitmap
}

// 2. Rectangle App Launcher (2x1)
fun generateRectangleLauncherBitmap(
    context: Context,
    slateConfig: SlateWidgetConfig,
    launcherConfig: AppLauncherWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()

    val cardCornerRadius = getStandardCornerRadius(density)

    // RESPONSIVE vs FIXED RECTANGLE CALCULATION
    val rect = if (launcherConfig.isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        // Fixed Mode: 2:1 aspect ratio centered inside the grid cell
        val targetRatio = 2.0f
        var cardW = w.toFloat()
        var cardH = cardW / targetRatio
        if (cardH > h) {
            cardH = h.toFloat()
            cardW = cardH * targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardCornerRadius, cardCornerRadius, bgPaint)

    // Render App Icon / Emoji / Vector / Custom Text / EDIT placeholder
    renderLauncherContent(context, canvas, rect, launcherConfig, accentColor, density)

    return bitmap
}

// 3. Cyber Glitch Typography Launcher (2x1)
fun generateGlitchTextLauncherBitmap(
    context: Context,
    slateConfig: SlateWidgetConfig,
    launcherConfig: AppLauncherWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cardCornerRadius = getStandardCornerRadius(density)
    val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardCornerRadius, cardCornerRadius, bgPaint)

    val text = launcherConfig.customText.ifEmpty { "LAUNCH" }.uppercase()
    val textSize = h * 0.36f

    val cyanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color(0xFF00E5FF).toArgb()
        this.textSize = textSize
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }

    val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color(0xFFFF1744).toArgb()
        this.textSize = textSize
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }

    val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color(slateConfig.accentColorHex).toArgb()
        this.textSize = textSize
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }

    val fontMetrics = mainPaint.fontMetrics
    val textY = (h / 2f) - (fontMetrics.ascent + fontMetrics.descent) / 2f

    canvas.drawText(text, (w / 2f) - 3f * density, textY - 2f * density, cyanPaint)
    canvas.drawText(text, (w / 2f) + 3f * density, textY + 2f * density, redPaint)
    canvas.drawText(text, w / 2f, textY, mainPaint)

    return bitmap
}

// 4. Glowing Neon Halo Ring Launcher (1x1)
fun generateNeonRingLauncherBitmap(
    context: Context,
    slateConfig: SlateWidgetConfig,
    launcherConfig: AppLauncherWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cardSize = minOf(w, h).toFloat()
    val cx = w / 2f
    val cy = h / 2f
    val accentColor = Color(slateConfig.accentColorHex).toArgb()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
        style = Paint.Style.FILL
    }
    val cardCornerRadius = getStandardCornerRadius(density)
    canvas.drawRoundRect(RectF((w - cardSize) / 2f, (h - cardSize) / 2f, (w + cardSize) / 2f, (h + cardSize) / 2f), cardCornerRadius, cardCornerRadius, bgPaint)

    val ringR = (cardSize / 2f) - (16f * density)
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
    }
    canvas.drawCircle(cx, cy, ringR, ringPaint)

    val iconRect = RectF(cx - ringR * 0.55f, cy - ringR * 0.55f, cx + ringR * 0.55f, cy + ringR * 0.55f)
    renderLauncherContent(context, canvas, iconRect, launcherConfig, accentColor, density)

    return bitmap
}

private fun renderLauncherContent(
    context: Context,
    canvas: Canvas,
    rect: RectF,
    config: AppLauncherWidgetConfig,
    tintColor: Int,
    density: Float
) {
    val cx = rect.centerX()
    val cy = rect.centerY()
    val size = minOf(rect.width(), rect.height())

    // If package name is empty, show the "EDIT" prompt placeholder
    if (config.packageName.isEmpty() && config.iconType == LauncherIconType.APP_ICON) {
        renderUnconfiguredPlaceholder(canvas, cx, cy, size, tintColor)
        return
    }

    when (config.iconType) {
        LauncherIconType.APP_ICON -> {
            if (config.packageName.isNotEmpty()) {
                try {
                    val iconDrawable = context.packageManager.getApplicationIcon(config.packageName)
                    val iconSize = (size * 0.55f).toInt()
                    iconDrawable.setBounds(
                        (cx - iconSize / 2f).toInt(),
                        (cy - iconSize / 2f).toInt(),
                        (cx + iconSize / 2f).toInt(),
                        (cy + iconSize / 2f).toInt()
                    )
                    iconDrawable.draw(canvas)
                } catch (e: PackageManager.NameNotFoundException) {
                    renderUnconfiguredPlaceholder(canvas, cx, cy, size, tintColor)
                }
            } else {
                renderUnconfiguredPlaceholder(canvas, cx, cy, size, tintColor)
            }
        }
        LauncherIconType.EMOJI -> {
            val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size * 0.48f
                textAlign = Paint.Align.CENTER
            }
            val fontMetrics = emojiPaint.fontMetrics
            val textY = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText(config.selectedEmoji, cx, textY, emojiPaint)
        }
        LauncherIconType.VECTOR_ICON -> {
            val resId = context.resources.getIdentifier(config.selectedVectorResName, "drawable", context.packageName)
            if (resId != 0) {
                val drawable = ContextCompat.getDrawable(context, resId)
                if (drawable != null) {
                    val iconSize = (size * 0.50f).toInt()
                    drawable.setBounds(
                        (cx - iconSize / 2f).toInt(),
                        (cy - iconSize / 2f).toInt(),
                        (cx + iconSize / 2f).toInt(),
                        (cy + iconSize / 2f).toInt()
                    )
                    drawable.setTint(tintColor)
                    drawable.draw(canvas)
                }
            } else {
                renderUnconfiguredPlaceholder(canvas, cx, cy, size, tintColor)
            }
        }
        LauncherIconType.CUSTOM_TEXT -> {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                textSize = size * 0.28f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val fontMetrics = textPaint.fontMetrics
            val textY = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText(config.customText.take(4).uppercase(), cx, textY, textPaint)
        }
    }
}

private fun renderUnconfiguredPlaceholder(canvas: Canvas, cx: Float, cy: Float, size: Float, tintColor: Int) {
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tintColor
        textSize = size * 0.24f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val fontMetrics = textPaint.fontMetrics
    val textY = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f
    canvas.drawText("EDIT", cx, textY, textPaint)
}

private fun renderDefaultAndroidIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, tintColor: Int) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tintColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, size / 2.5f, paint)
}