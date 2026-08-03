package com.altusix.slate.widgets.ai

import android.content.Context
import android.graphics.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import com.altusix.slate.data.local.SlateWidgetConfig
import kotlin.math.cos
import kotlin.math.sin

enum class AiShapeStyle {
    SQUIRCLE,
    CIRCLE,
    HEXAGON,
    FRAMELESS
}

@Composable
fun AiShortcutTile(
    target: AiTarget,
    config: SlateWidgetConfig,
    shapeStyle: AiShapeStyle = AiShapeStyle.SQUIRCLE
) {
    val context = androidx.glance.LocalContext.current
    val isLight = config.themeMode == "LIGHT"
    val rawBgColor = Color(config.backgroundColorHex)
    val finalBgColor = rawBgColor.copy(alpha = config.opacity)
    val accentColor = Color(config.accentColorHex)

    val canvasSizePx = 360

    val bitmap = generateAiLogoBitmap(
        context = context,
        target = target,
        bgColor = finalBgColor,
        accentColor = accentColor,
        isLight = isLight,
        shapeStyle = shapeStyle,
        sizePx = canvasSizePx
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(
                actionStartActivity(
                    AiLauncherUtils.getLaunchIntent(context, target)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = target.title,
            modifier = GlanceModifier.fillMaxSize()
        )
    }
}

private fun generateAiLogoBitmap(
    context: Context,
    target: AiTarget,
    bgColor: Color,
    accentColor: Color,
    isLight: Boolean,
    shapeStyle: AiShapeStyle,
    sizePx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val s = sizePx.toFloat()
    val center = s / 2f

    val logoColor = if (isLight) Color(0xFF1C1C1E).toArgb() else Color.White.toArgb()
    val strokeColor = if (isLight) Color(0xFFD1D1D6).toArgb() else Color(0xFF2C2C2E).toArgb()

    // 1. Background Container Shape
    if (shapeStyle != AiShapeStyle.FRAMELESS) {
        val bgPaint = Paint().apply {
            isAntiAlias = true
            color = bgColor.toArgb()
            style = Paint.Style.FILL
        }

        val strokePaint = Paint().apply {
            isAntiAlias = true
            color = strokeColor
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }

        when (shapeStyle) {
            AiShapeStyle.SQUIRCLE -> {
                val margin = 12f
                val rect = RectF(margin, margin, s - margin, s - margin)
                val radius = s * 0.28f
                canvas.drawRoundRect(rect, radius, radius, bgPaint)
                canvas.drawRoundRect(rect, radius, radius, strokePaint)
            }
            AiShapeStyle.CIRCLE -> {
                canvas.drawCircle(center, center, center - 12f, bgPaint)
                canvas.drawCircle(center, center, center - 12f, strokePaint)
            }
            AiShapeStyle.HEXAGON -> {
                val hexPath = Path()
                val radius = center - 12f
                for (i in 0 until 6) {
                    val angle = Math.toRadians((60 * i - 30).toDouble())
                    val x = center + (radius * cos(angle)).toFloat()
                    val y = center + (radius * sin(angle)).toFloat()
                    if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
                }
                hexPath.close()
                canvas.drawPath(hexPath, bgPaint)
                canvas.drawPath(hexPath, strokePaint)
            }
            AiShapeStyle.FRAMELESS -> {}
        }
    }

// 2. Render Official Vector Drawable Resource
    val resId = context.resources.getIdentifier(target.drawableResName, "drawable", context.packageName)

    if (resId != 0) {
        val drawable = ContextCompat.getDrawable(context, resId)
        if (drawable != null) {
            val maxLogoSize = s * 0.48f

            val intrinsicW = drawable.intrinsicWidth.toFloat()
            val intrinsicH = drawable.intrinsicHeight.toFloat()

            var drawW = maxLogoSize
            var drawH = maxLogoSize

            // Maintain intrinsic aspect ratio so non-square SVGs never get squeezed
            if (intrinsicW > 0f && intrinsicH > 0f) {
                val aspectRatio = intrinsicW / intrinsicH
                if (aspectRatio > 1f) {
                    // Wider than tall
                    drawH = maxLogoSize / aspectRatio
                } else {
                    // Taller than wide
                    drawW = maxLogoSize * aspectRatio
                }
            }

            val left = (center - (drawW / 2f)).toInt()
            val top = (center - (drawH / 2f)).toInt()
            val right = (center + (drawW / 2f)).toInt()
            val bottom = (center + (drawH / 2f)).toInt()

            drawable.setBounds(left, top, right, bottom)
            drawable.setTint(logoColor)
            drawable.draw(canvas)
        }
    } else {
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = logoColor
            textSize = s * 0.28f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val fontMetrics = textPaint.fontMetrics
        val textY = center - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(target.title.take(1), center, textY, textPaint)
    }

    return bitmap
}