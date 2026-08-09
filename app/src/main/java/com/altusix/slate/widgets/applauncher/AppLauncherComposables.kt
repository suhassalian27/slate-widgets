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
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath

private fun renderImageVectorOnCanvas(
    canvas: Canvas,
    imageVector: ImageVector,
    cx: Float,
    cy: Float,
    contentSize: Float,
    tintColor: Int
) {
    val scale = contentSize / imageVector.defaultWidth.value
    val left = cx - contentSize / 2f
    val top = cy - contentSize / 2f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tintColor
        style = Paint.Style.FILL
    }

    drawVectorGroupOnCanvas(canvas, imageVector.root, left, top, scale, paint)
}

private fun drawVectorGroupOnCanvas(
    canvas: Canvas,
    group: VectorGroup,
    left: Float,
    top: Float,
    scale: Float,
    paint: Paint
) {
    for (node in group) {
        when (node) {
            is VectorPath -> {
                val composePath = node.pathData.toPath()
                val androidPath = composePath.asAndroidPath()
                androidPath.fillType = Path.FillType.EVEN_ODD

                val matrix = Matrix()
                matrix.postScale(scale, scale)
                matrix.postTranslate(left, top)
                androidPath.transform(matrix)

                canvas.drawPath(androidPath, paint)
            }
            is VectorGroup -> {
                drawVectorGroupOnCanvas(canvas, node, left, top, scale, paint)
            }
        }
    }
}

private fun getStandardCornerRadius(density: Float): Float = 22f * density

private fun addSmoothVerticesPath(path: Path, vertices: List<PointF>, cornerRadius: Float) {
    if (vertices.size < 3) return
    val n = vertices.size
    for (i in 0 until n) {
        val prev = vertices[(i - 1 + n) % n]
        val curr = vertices[i]
        val next = vertices[(i + 1) % n]

        val v1x = prev.x - curr.x
        val v1y = prev.y - curr.y
        val len1 = Math.hypot(v1x.toDouble(), v1y.toDouble()).toFloat()

        val v2x = next.x - curr.x
        val v2y = next.y - curr.y
        val len2 = Math.hypot(v2x.toDouble(), v2y.toDouble()).toFloat()

        val r = minOf(cornerRadius, len1 / 2f, len2 / 2f)

        val p1x = curr.x + (v1x / len1) * r
        val p1y = curr.y + (v1y / len1) * r

        val p2x = curr.x + (v2x / len2) * r
        val p2y = curr.y + (v2y / len2) * r

        if (i == 0) {
            path.moveTo(p1x, p1y)
        } else {
            path.lineTo(p1x, p1y)
        }
        path.quadTo(curr.x, curr.y, p2x, p2y)
    }
    path.close()
}

fun getShapePath(shape: LauncherShape, rect: RectF, density: Float): Path {
    val path = Path()
    val cx = rect.centerX()
    val cy = rect.centerY()
    val radius = minOf(rect.width(), rect.height()) / 2f
    val cornerRadius = 14f * density

    when (shape) {
        LauncherShape.SQUIRCLE -> {
            val halfW = rect.width() / 2f
            val halfH = rect.height() / 2f
            val n = 3.8
            val steps = 180

            for (i in 0 until steps) {
                val t = (2.0 * Math.PI * i / steps)
                val cosT = cos(t)
                val sinT = sin(t)
                val x = cx + halfW * Math.signum(cosT).toFloat() * Math.pow(Math.abs(cosT), 2.0 / n).toFloat()
                val y = cy + halfH * Math.signum(sinT).toFloat() * Math.pow(Math.abs(sinT), 2.0 / n).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
        }
        LauncherShape.CIRCLE -> {
            path.addCircle(cx, cy, radius, Path.Direction.CW)
        }
        LauncherShape.M3_PENTAGON -> {
            val pts = mutableListOf<PointF>()
            for (i in 0 until 5) {
                val angle = Math.toRadians((72 * i - 90).toDouble())
                pts.add(PointF((cx + radius * cos(angle)).toFloat(), (cy + radius * sin(angle)).toFloat()))
            }
            addSmoothVerticesPath(path, pts, cornerRadius)
        }
        LauncherShape.M3_OCTAGON -> {
            val pts = mutableListOf<PointF>()
            for (i in 0 until 8) {
                val angle = Math.toRadians((45 * i - 22.5).toDouble())
                pts.add(PointF((cx + radius * cos(angle)).toFloat(), (cy + radius * sin(angle)).toFloat()))
            }
            addSmoothVerticesPath(path, pts, cornerRadius * 0.75f)
        }
        LauncherShape.M3_DIAMOND -> {
            val pts = listOf(
                PointF(cx, rect.top + 4f * density),
                PointF(rect.right - 4f * density, cy),
                PointF(cx, rect.bottom - 4f * density),
                PointF(rect.left + 4f * density, cy)
            )
            addSmoothVerticesPath(path, pts, cornerRadius * 1.2f)
        }
        LauncherShape.TRIANGLE -> {
            val topMargin = radius * 0.12f
            val pts = listOf(
                PointF(cx, rect.top + topMargin),
                PointF(rect.right - topMargin, rect.bottom - topMargin),
                PointF(rect.left + topMargin, rect.bottom - topMargin)
            )
            addSmoothVerticesPath(path, pts, cornerRadius * 1.3f)
        }
        LauncherShape.STAR_5 -> {
            val pts = mutableListOf<PointF>()
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) radius * 0.95f else radius * 0.48f
                val angle = Math.toRadians((36 * i - 90).toDouble())
                pts.add(PointF((cx + r * cos(angle)).toFloat(), (cy + r * sin(angle)).toFloat()))
            }
            addSmoothVerticesPath(path, pts, cornerRadius * 0.85f)
        }
        LauncherShape.PIXEL_STAR -> {
            val pts = mutableListOf<PointF>()
            for (i in 0 until 16) {
                val r = if (i % 2 == 0) radius * 0.95f else radius * 0.65f
                val angle = Math.toRadians((22.5 * i - 90).toDouble())
                pts.add(PointF((cx + r * cos(angle)).toFloat(), (cy + r * sin(angle)).toFloat()))
            }
            addSmoothVerticesPath(path, pts, cornerRadius * 0.60f)
        }
        LauncherShape.HEART -> {
            val cleftY      = cy - radius * 0.7085f   // dip between the two lobes
            val lobePeakY   = cy - radius * 0.9175f   // height of the rounded lobe tops
            val extremityY  = cy - radius * 0.3675f   // widest point, left/right
            val tipY        = cy + radius * 0.9175f   // bottom point

            val lobeX       = radius * 0.45f          // x of each lobe's peak

            path.moveTo(cx, cleftY)

            // Segment A: cleft -> right lobe peak
            path.cubicTo(
                cx + radius * 0.109f, cy - radius * 0.8365f,
                cx + radius * 0.276f, lobePeakY,
                cx + lobeX, lobePeakY
            )

            // Segment B: right lobe peak -> right widest point
            path.cubicTo(
                cx + radius * 0.758f, lobePeakY,
                cx + radius, cy - radius * 0.6755f,
                cx + radius, extremityY
            )

            // Segment C: right widest point -> bottom tip
            path.cubicTo(
                cx + radius, cy + radius * 0.0105f,
                cx + radius * 0.66f, cy + radius * 0.3185f,
                cx, tipY
            )

            // Segment D: bottom tip -> left widest point (mirror of C)
            path.cubicTo(
                cx - radius * 0.66f, cy + radius * 0.3185f,
                cx - radius, cy + radius * 0.0105f,
                cx - radius, extremityY
            )

            // Segment E: left widest point -> left lobe peak (mirror of B)
            path.cubicTo(
                cx - radius, cy - radius * 0.6755f,
                cx - radius * 0.758f, lobePeakY,
                cx - lobeX, lobePeakY
            )

            // Segment F: left lobe peak -> back to cleft (mirror of A)
            path.cubicTo(
                cx - radius * 0.276f, lobePeakY,
                cx - radius * 0.109f, cy - radius * 0.8365f,
                cx, cleftY
            )

            path.close()
        }
        LauncherShape.M3_FLOWER -> {
            val petals = 12
            val pts = mutableListOf<PointF>()
            for (i in 0 until petals * 2) {
                val r = if (i % 2 == 0) radius else radius * 0.82f
                val angle = Math.toRadians((360.0 / (petals * 2)) * i)
                pts.add(PointF((cx + r * cos(angle)).toFloat(), (cy + r * sin(angle)).toFloat()))
            }
            addSmoothVerticesPath(path, pts, cornerRadius * 0.45f)
        }
        LauncherShape.M3_CLOVER -> {
            val lobeR = radius * 0.52f
            path.addCircle(cx - lobeR * 0.5f, cy - lobeR * 0.5f, lobeR, Path.Direction.CW)
            path.addCircle(cx + lobeR * 0.5f, cy - lobeR * 0.5f, lobeR, Path.Direction.CW)
            path.addCircle(cx - lobeR * 0.5f, cy + lobeR * 0.5f, lobeR, Path.Direction.CW)
            path.addCircle(cx + lobeR * 0.5f, cy + lobeR * 0.5f, lobeR, Path.Direction.CW)
        }
        LauncherShape.BLOB_BOTTOM_RIGHT -> {
            val bigR = radius * 0.92f
            val smallR = radius * 0.32f
            val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            val radii = floatArrayOf(
                bigR, bigR,       // top-left
                bigR, bigR,       // top-right
                smallR, smallR,   // bottom-right — the pinched corner
                bigR, bigR        // bottom-left
            )
            path.addRoundRect(rect, radii, Path.Direction.CW)
        }

        LauncherShape.BLOB_BOTTOM_LEFT -> {
            val bigR = radius * 0.92f
            val smallR = radius * 0.32f
            val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            val radii = floatArrayOf(
                bigR, bigR,       // top-left
                bigR, bigR,       // top-right
                bigR, bigR,       // bottom-right
                smallR, smallR    // bottom-left — the pinched corner
            )
            path.addRoundRect(rect, radii, Path.Direction.CW)
        }

        LauncherShape.BLOB_TOP_RIGHT -> {
            val bigR = radius * 0.92f
            val smallR = radius * 0.32f
            val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            val radii = floatArrayOf(
                bigR, bigR,       // top-left
                smallR, smallR,   // top-right — the pinched corner
                bigR, bigR,       // bottom-right
                bigR, bigR        // bottom-left
            )
            path.addRoundRect(rect, radii, Path.Direction.CW)
        }

        LauncherShape.BLOB_TOP_LEFT -> {
            val bigR = radius * 0.92f
            val smallR = radius * 0.32f
            val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            val radii = floatArrayOf(
                smallR, smallR,   // top-left — the pinched corner
                bigR, bigR,       // top-right
                bigR, bigR,       // bottom-right
                bigR, bigR        // bottom-left
            )
            path.addRoundRect(rect, radii, Path.Direction.CW)
        }

    }
    return path
}

fun generateAdaptiveLauncherBitmap(    context: Context,    slateConfig: SlateWidgetConfig,    launcherConfig: AppLauncherWidgetConfig,    wDp: Int,    hDp: Int): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cardBg = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()

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

    renderLauncherContent(context, canvas, rect, launcherConfig, accentColor, density)
    return bitmap
}

fun generateRectangleLauncherBitmap(    context: Context,    slateConfig: SlateWidgetConfig,    launcherConfig: AppLauncherWidgetConfig,    wDp: Int,    hDp: Int): Bitmap {
    val density = context.resources.displayMetrics.density
    val w = (wDp * density).toInt().coerceAtLeast(1)
    val h = (hDp * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val cardCornerRadius = getStandardCornerRadius(density)

    val rect = if (launcherConfig.isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
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

    renderLauncherContent(context, canvas, rect, launcherConfig, accentColor, density)
    return bitmap
}

fun generateSquircleLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    return generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.SQUIRCLE, isResponsive = false), wDp, hDp)
}

fun generatePentagonLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    return generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.M3_PENTAGON, isResponsive = false), wDp, hDp)
}

fun generateFlowerLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    return generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.M3_FLOWER, isResponsive = false), wDp, hDp)
}

fun generateCloverLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    return generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.M3_CLOVER, isResponsive = false), wDp, hDp)
}

fun generateDiamondLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    return generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.M3_DIAMOND, isResponsive = false), wDp, hDp)
}

fun generateOctagonLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    return generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.M3_OCTAGON, isResponsive = false), wDp, hDp)
}

fun generateCircleLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    return generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.CIRCLE, isResponsive = false), wDp, hDp)
}

fun generateBlobBottomRightLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    return generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.BLOB_BOTTOM_RIGHT, isResponsive = false), wDp, hDp)
}

fun generateBlobBottomLeftLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    return generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.BLOB_BOTTOM_LEFT, isResponsive = false), wDp, hDp)
}

fun generateBlobTopRightLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    return generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.BLOB_TOP_RIGHT, isResponsive = false), wDp, hDp)
}

fun generateBlobTopLeftLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    return generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.BLOB_TOP_LEFT, isResponsive = false), wDp, hDp)
}

fun generatePixelStarLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    return generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.PIXEL_STAR, isResponsive = false), wDp, hDp)
}

fun generateStar5LauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    return generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.STAR_5, isResponsive = false), wDp, hDp)
}

fun generateHeartLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    return generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.HEART, isResponsive = false), wDp, hDp)
}

fun generateTriangleLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    return generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.TRIANGLE, isResponsive = false), wDp, hDp)
}

fun generateGlitchTextLauncherBitmap(    context: Context,    slateConfig: SlateWidgetConfig,    launcherConfig: AppLauncherWidgetConfig,    wDp: Int,    hDp: Int): Bitmap {
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
    val textSize = h * 0.28f

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

fun generateNeonRingLauncherBitmap(    context: Context,    slateConfig: SlateWidgetConfig,    launcherConfig: AppLauncherWidgetConfig,    wDp: Int,    hDp: Int): Bitmap {
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

    val ringR = (cardSize / 2f) - (14f * density)
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
    }
    canvas.drawCircle(cx, cy, ringR, ringPaint)

    // Restrict content bounds inside the ring with breathing room
    val innerRadius = ringR - (6f * density)
    val contentRect = RectF(cx - innerRadius, cy - innerRadius, cx + innerRadius, cy + innerRadius)
    renderLauncherContent(context, canvas, contentRect, launcherConfig, accentColor, density)

    return bitmap
}
private fun renderLauncherContent(    context: Context,    canvas: Canvas,    rect: RectF,    config: AppLauncherWidgetConfig,    tintColor: Int,    density: Float
) {
    val baseSize = minOf(rect.width(), rect.height())

    // Safe shape-aware scale & vertical offset factors so content stays inside shape bounds
    val (scaleFactor, offsetYFactor) = when (config.shape) {
        LauncherShape.TRIANGLE -> 0.36f to 0.12f
        LauncherShape.STAR_5 -> 0.38f to 0.02f
        LauncherShape.PIXEL_STAR -> 0.40f to 0.0f
        LauncherShape.HEART -> 0.40f to -0.04f
        LauncherShape.M3_DIAMOND -> 0.40f to 0.0f
        LauncherShape.M3_PENTAGON -> 0.44f to 0.03f
        LauncherShape.M3_OCTAGON, LauncherShape.M3_FLOWER, LauncherShape.M3_CLOVER -> 0.46f to 0.0f
        LauncherShape.BLOB_BOTTOM_RIGHT, LauncherShape.BLOB_BOTTOM_LEFT,
        LauncherShape.BLOB_TOP_RIGHT, LauncherShape.BLOB_TOP_LEFT -> 0.44f to 0.0f
        else -> 0.48f to 0.0f
    }

    val cx = rect.centerX()
    val cy = rect.centerY() + (baseSize * offsetYFactor)
    val contentSize = baseSize * scaleFactor

    if (config.packageName.isEmpty() && config.iconType == LauncherIconType.APP_ICON) {
        renderUnconfiguredPlaceholder(canvas, cx, cy, contentSize, tintColor)
        return
    }

    when (config.iconType) {
        LauncherIconType.APP_ICON -> {
            if (config.packageName.isNotEmpty()) {
                try {
                    val iconDrawable = context.packageManager.getApplicationIcon(config.packageName)
                    val iconPx = contentSize.toInt()
                    iconDrawable.setBounds(
                        (cx - iconPx / 2f).toInt(),
                        (cy - iconPx / 2f).toInt(),
                        (cx + iconPx / 2f).toInt(),
                        (cy + iconPx / 2f).toInt()
                    )
                    iconDrawable.draw(canvas)
                } catch (e: PackageManager.NameNotFoundException) {
                    renderUnconfiguredPlaceholder(canvas, cx, cy, contentSize, tintColor)
                }
            } else {
                renderUnconfiguredPlaceholder(canvas, cx, cy, contentSize, tintColor)
            }
        }
        LauncherIconType.EMOJI -> {
            val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = contentSize * 0.95f
                textAlign = Paint.Align.CENTER
            }
            val fontMetrics = emojiPaint.fontMetrics
            val textY = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText(config.selectedEmoji, cx, textY, emojiPaint)
        }
        LauncherIconType.VECTOR_ICON -> {
            val imageVector = AppLauncherVectorIcons.findIcon(config.selectedVectorResName)
            if (imageVector != null) {
                renderImageVectorOnCanvas(canvas, imageVector, cx, cy, contentSize, tintColor)
            } else {
                renderUnconfiguredPlaceholder(canvas, cx, cy, contentSize, tintColor)
            }
        }
        LauncherIconType.CUSTOM_TEXT -> {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                textSize = contentSize * 0.55f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val fontMetrics = textPaint.fontMetrics
            val textY = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText(config.customText.take(4).uppercase(), cx, textY, textPaint)
        }
    }
}

private fun renderUnconfiguredPlaceholder(canvas: Canvas, cx: Float, cy: Float, contentSize: Float, tintColor: Int) {
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tintColor
        textSize = contentSize * 0.48f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val fontMetrics = textPaint.fontMetrics
    val textY = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f
    canvas.drawText("EDIT", cx, textY, textPaint)
}