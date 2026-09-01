package com.altusix.slate.widgets.applauncher

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.createSupersampledCanvas
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius
import kotlin.math.cos
import kotlin.math.sin

private fun renderImageVectorOnCanvas(canvas: Canvas, imageVector: ImageVector, cx: Float, cy: Float, contentSize: Float, tintColor: Int) {
    val scale = contentSize / imageVector.defaultWidth.value
    val left = cx - contentSize / 2f
    val top = cy - contentSize / 2f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tintColor
        style = Paint.Style.FILL
    }

    drawVectorGroupOnCanvas(canvas, imageVector.root, left, top, scale, paint)
}

private fun drawVectorGroupOnCanvas(canvas: Canvas, group: VectorGroup, left: Float, top: Float, scale: Float, paint: Paint) {
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

fun getShapePath(shape: LauncherShape, rect: RectF, scaleFactor: Float): Path {
    val path = Path()
    val cx = rect.centerX()
    val cy = rect.centerY()
    val radius = minOf(rect.width(), rect.height()) / 2f
    val cornerRadius = 14f * scaleFactor

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
                PointF(cx, rect.top + 4f * scaleFactor),
                PointF(rect.right - 4f * scaleFactor, cy),
                PointF(cx, rect.bottom - 4f * scaleFactor),
                PointF(rect.left + 4f * scaleFactor, cy)
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
            val cleftY = cy - radius * 0.7085f
            val lobePeakY = cy - radius * 0.9175f
            val extremityY = cy - radius * 0.3675f
            val tipY = cy + radius * 0.9175f
            val lobeX = radius * 0.45f

            path.moveTo(cx, cleftY)
            path.cubicTo(cx + radius * 0.109f, cy - radius * 0.8365f, cx + radius * 0.276f, lobePeakY, cx + lobeX, lobePeakY)
            path.cubicTo(cx + radius * 0.758f, lobePeakY, cx + radius, cy - radius * 0.6755f, cx + radius, extremityY)
            path.cubicTo(cx + radius, cy + radius * 0.0105f, cx + radius * 0.66f, cy + radius * 0.3185f, cx, tipY)
            path.cubicTo(cx - radius * 0.66f, cy + radius * 0.3185f, cx - radius, cy + radius * 0.0105f, cx - radius, extremityY)
            path.cubicTo(cx - radius, cy - radius * 0.6755f, cx - radius * 0.758f, lobePeakY, cx - lobeX, lobePeakY)
            path.cubicTo(cx - radius * 0.276f, lobePeakY, cx - radius * 0.109f, cy - radius * 0.8365f, cx, cleftY)
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
            val rRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            val radii = floatArrayOf(bigR, bigR, bigR, bigR, smallR, smallR, bigR, bigR)
            path.addRoundRect(rRect, radii, Path.Direction.CW)
        }
        LauncherShape.BLOB_BOTTOM_LEFT -> {
            val bigR = radius * 0.92f
            val smallR = radius * 0.32f
            val rRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            val radii = floatArrayOf(bigR, bigR, bigR, bigR, bigR, bigR, smallR, smallR)
            path.addRoundRect(rRect, radii, Path.Direction.CW)
        }
        LauncherShape.BLOB_TOP_RIGHT -> {
            val bigR = radius * 0.92f
            val smallR = radius * 0.32f
            val rRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            val radii = floatArrayOf(bigR, bigR, smallR, smallR, bigR, bigR, bigR, bigR)
            path.addRoundRect(rRect, radii, Path.Direction.CW)
        }
        LauncherShape.BLOB_TOP_LEFT -> {
            val bigR = radius * 0.92f
            val smallR = radius * 0.32f
            val rRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            val radii = floatArrayOf(smallR, smallR, bigR, bigR, bigR, bigR, bigR, bigR)
            path.addRoundRect(rRect, radii, Path.Direction.CW)
        }
        else -> {
            val cardCornerRadius = getStandardCornerRadius(scaleFactor)
            path.addRoundRect(rect, cardCornerRadius, cardCornerRadius, Path.Direction.CW)
        }
    }
    return path
}

// 1. ADAPTIVE SHAPE LAUNCHER (1x1 Standard Square Base)
fun generateAdaptiveLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardBg = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()

    val margin = scaleFactor * 1.5f
    val rect = if (launcherConfig.isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
        val leftX = (w - cardSize) / 2f
        val topY = (h - cardSize) / 2f
        RectF(leftX, topY, leftX + cardSize, topY + cardSize)
    }

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cardBg
        style = Paint.Style.FILL
    }

    if (launcherConfig.shape == LauncherShape.SQUIRCLE) {
        val cardCornerRadius = getStandardCornerRadius(scaleFactor)
        canvas.drawRoundRect(rect, cardCornerRadius, cardCornerRadius, bgPaint)
    } else {
        val path = getShapePath(launcherConfig.shape, rect, scaleFactor)
        canvas.drawPath(path, bgPaint)
    }

    renderLauncherContent(context, canvas, rect, launcherConfig, accentColor, scaleFactor)
    return bitmap
}

// 2. RECTANGLE LAUNCHER (2x1)
fun generateRectangleLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)

    val margin = scaleFactor * 1.5f
    val rect = if (launcherConfig.isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val targetRatio = 2.0f
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
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

    renderLauncherContent(context, canvas, rect, launcherConfig, accentColor, scaleFactor)
    return bitmap
}

// 2B. PILL LAUNCHER (2x1 Full Curve Capsule)
fun generatePillLauncherBitmap(
    context: Context,
    slateConfig: SlateWidgetConfig,
    launcherConfig: AppLauncherWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bgColor = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()

    val margin = scaleFactor * 1.5f
    val rect = if (launcherConfig.isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val targetRatio = 2.0f
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    // Full capsule curve: radius is half of the height
    val pillCornerRadius = rect.height() / 2f

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, pillCornerRadius, pillCornerRadius, bgPaint)

    renderLauncherContent(context, canvas, rect, launcherConfig, accentColor, scaleFactor)
    return bitmap
}

// SPECIFIC SQUIRCLE LAUNCHER (Strict Superellipse $n = 3.8$)
fun generateSquircleLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardBg = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()

    val margin = scaleFactor * 1.5f
    val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f
    val rect = RectF(leftX, topY, leftX + cardSize, topY + cardSize)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cardBg
        style = Paint.Style.FILL
    }

    val path = getShapePath(LauncherShape.SQUIRCLE, rect, scaleFactor)
    canvas.drawPath(path, bgPaint)

    renderLauncherContent(context, canvas, rect, launcherConfig, accentColor, scaleFactor)
    return bitmap
}

fun generatePentagonLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap =
    generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.M3_PENTAGON, isResponsive = false), wDp, hDp)

fun generateFlowerLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap =
    generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.M3_FLOWER, isResponsive = false), wDp, hDp)

fun generateCloverLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap =
    generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.M3_CLOVER, isResponsive = false), wDp, hDp)

fun generateDiamondLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap =
    generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.M3_DIAMOND, isResponsive = false), wDp, hDp)

fun generateOctagonLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap =
    generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.M3_OCTAGON, isResponsive = false), wDp, hDp)

fun generateCircleLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap =
    generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.CIRCLE, isResponsive = false), wDp, hDp)

fun generateBlobBottomRightLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap =
    generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.BLOB_BOTTOM_RIGHT, isResponsive = false), wDp, hDp)

fun generateBlobBottomLeftLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap =
    generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.BLOB_BOTTOM_LEFT, isResponsive = false), wDp, hDp)

fun generateBlobTopRightLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap =
    generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.BLOB_TOP_RIGHT, isResponsive = false), wDp, hDp)

fun generateBlobTopLeftLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap =
    generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.BLOB_TOP_LEFT, isResponsive = false), wDp, hDp)

fun generatePixelStarLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap =
    generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.PIXEL_STAR, isResponsive = false), wDp, hDp)

fun generateStar5LauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap =
    generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.STAR_5, isResponsive = false), wDp, hDp)

fun generateHeartLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap =
    generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.HEART, isResponsive = false), wDp, hDp)

fun generateTriangleLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap =
    generateAdaptiveLauncherBitmap(context, slateConfig, launcherConfig.copy(shape = LauncherShape.TRIANGLE, isResponsive = false), wDp, hDp)

// 3. GLITCH TEXT LAUNCHER (2x2)
fun generateGlitchTextLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    val margin = scaleFactor * 1.5f
    val rect = RectF(margin, margin, w - margin, h - margin)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, cardCornerRadius, cardCornerRadius, bgPaint)

    val text = launcherConfig.customText.ifEmpty { "LAUNCH" }.uppercase()
    val textSize = minOf(rect.width(), rect.height()) * 0.28f
    val font = getSlateFont(context, weight = 800)

    val cyanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color(0xFF00E5FF).toArgb()
        this.textSize = textSize
        typeface = font
        textAlign = Paint.Align.CENTER
    }

    val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color(0xFFFF1744).toArgb()
        this.textSize = textSize
        typeface = font
        textAlign = Paint.Align.CENTER
    }

    val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color(slateConfig.accentColorHex).toArgb()
        this.textSize = textSize
        typeface = font
        textAlign = Paint.Align.CENTER
    }

    val fontMetrics = mainPaint.fontMetrics
    val textY = rect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2f
    val cx = rect.centerX()

    canvas.drawText(text, cx - (3f * scaleFactor), textY - (2f * scaleFactor), cyanPaint)
    canvas.drawText(text, cx + (3f * scaleFactor), textY + (2f * scaleFactor), redPaint)
    canvas.drawText(text, cx, textY, mainPaint)

    return bitmap
}

// 4. NEON RING LAUNCHER (1x1 Square)
fun generateNeonRingLauncherBitmap(context: Context, slateConfig: SlateWidgetConfig, launcherConfig: AppLauncherWidgetConfig, wDp: Int, hDp: Int): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val margin = scaleFactor * 1.5f
    val cardSize = minOf(w - (margin * 2f), h - (margin * 2f))
    val leftX = (w - cardSize) / 2f
    val topY = (h - cardSize) / 2f
    val rect = RectF(leftX, topY, leftX + cardSize, topY + cardSize)

    val cx = rect.centerX()
    val cy = rect.centerY()
    val accentColor = Color(slateConfig.accentColorHex).toArgb()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color(slateConfig.backgroundColorHex).copy(alpha = slateConfig.opacity).toArgb()
        style = Paint.Style.FILL
    }
    val cardCornerRadius = getStandardCornerRadius(scaleFactor)
    canvas.drawRoundRect(rect, cardCornerRadius, cardCornerRadius, bgPaint)

    val ringR = (cardSize / 2f) - (14f * scaleFactor)
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.STROKE
        strokeWidth = 3f * scaleFactor
    }
    canvas.drawCircle(cx, cy, ringR, ringPaint)

    val innerRadius = ringR - (6f * scaleFactor)
    val contentRect = RectF(cx - innerRadius, cy - innerRadius, cx + innerRadius, cy + innerRadius)
    renderLauncherContent(context, canvas, contentRect, launcherConfig, accentColor, scaleFactor)

    return bitmap
}

private fun renderLauncherContent(
    context: Context,
    canvas: Canvas,
    rect: RectF,
    config: AppLauncherWidgetConfig,
    tintColor: Int,
    scaleFactor: Float
) {
    val baseH = rect.height()

    val (shapeScaleMultiplier, offsetYFactor) = when (config.shape) {
        LauncherShape.TRIANGLE -> 0.85f to 0.10f
        LauncherShape.STAR_5 -> 0.88f to 0.02f
        LauncherShape.HEART -> 0.90f to -0.03f
        LauncherShape.M3_PENTAGON -> 0.95f to 0.02f
        else -> 1.0f to 0.0f
    }

    val cx = rect.centerX()
    val cy = rect.centerY() + (baseH * offsetYFactor)

    // Unified ~0.20f baseline scale across all icon and typography formats
    val unifiedIconSize = (baseH * 0.22f * shapeScaleMultiplier).coerceAtLeast(scaleFactor * 8f)

    if (config.packageName.isEmpty() && config.iconType == LauncherIconType.APP_ICON) {
        renderUnconfiguredPlaceholder(context, canvas, cx, cy, baseH, tintColor, scaleFactor)
        return
    }

    when (config.iconType) {
        LauncherIconType.APP_ICON -> {
            if (config.packageName.isNotEmpty()) {
                try {
                    val iconDrawable = context.packageManager.getApplicationIcon(config.packageName)
                    // 0.24f accounts for the internal whitespace padding built into standard Android app icons
                    val iconPx = (baseH * 0.24f * shapeScaleMultiplier).toInt().coerceAtLeast((scaleFactor * 10f).toInt())
                    iconDrawable.setBounds(
                        (cx - iconPx / 2f).toInt(),
                        (cy - iconPx / 2f).toInt(),
                        (cx + iconPx / 2f).toInt(),
                        (cy + iconPx / 2f).toInt()
                    )
                    iconDrawable.draw(canvas)
                } catch (e: PackageManager.NameNotFoundException) {
                    renderUnconfiguredPlaceholder(context, canvas, cx, cy, baseH, tintColor, scaleFactor)
                }
            } else {
                renderUnconfiguredPlaceholder(context, canvas, cx, cy, baseH, tintColor, scaleFactor)
            }
        }
        LauncherIconType.EMOJI -> {
            val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = (baseH * 0.21f * shapeScaleMultiplier).coerceAtLeast(scaleFactor * 8f)
                textAlign = Paint.Align.CENTER
            }
            val fontMetrics = emojiPaint.fontMetrics
            val textY = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText(config.selectedEmoji, cx, textY, emojiPaint)
        }
        LauncherIconType.VECTOR_ICON -> {
            val imageVector = AppLauncherVectorIcons.findIcon(config.selectedVectorResName)
            if (imageVector != null) {
                renderImageVectorOnCanvas(canvas, imageVector, cx, cy, unifiedIconSize, tintColor)
            } else {
                renderUnconfiguredPlaceholder(context, canvas, cx, cy, baseH, tintColor, scaleFactor)
            }
        }
        LauncherIconType.CUSTOM_TEXT -> {
            val text = config.customText.ifEmpty { "APP" }.uppercase()

            val maxAllowedW = if (rect.width() > rect.height() * 1.3f) {
                rect.width() * 0.82f
            } else {
                rect.width() * 0.76f
            }
            val maxAllowedH = baseH * 0.35f

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                typeface = getSlateFont(context, weight = 700)
                textAlign = Paint.Align.CENTER
            }

            var targetTextSize = (baseH * 0.20f * shapeScaleMultiplier).coerceAtLeast(scaleFactor * 7f)
            textPaint.textSize = targetTextSize

            val measuredWidth = textPaint.measureText(text)
            if (measuredWidth > maxAllowedW) {
                targetTextSize *= (maxAllowedW / measuredWidth)
            }
            if (targetTextSize > maxAllowedH) {
                targetTextSize = maxAllowedH
            }
            textPaint.textSize = targetTextSize.coerceAtLeast(scaleFactor * 6f)

            val fontMetrics = textPaint.fontMetrics
            val textY = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText(text, cx, textY, textPaint)
        }
    }
}

private fun renderUnconfiguredPlaceholder(
    context: Context,
    canvas: Canvas,
    cx: Float,
    cy: Float,
    baseH: Float,
    tintColor: Int,
    scaleFactor: Float
) {
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tintColor
        textSize = (baseH * 0.20f).coerceAtLeast(scaleFactor * 7f)
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }
    val fontMetrics = textPaint.fontMetrics
    val textY = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f
    canvas.drawText("EDIT", cx, textY, textPaint)
}
