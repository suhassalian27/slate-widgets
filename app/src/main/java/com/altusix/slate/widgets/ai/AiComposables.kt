package com.altusix.slate.widgets.ai

import android.content.Context
import android.graphics.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.*
import com.altusix.slate.data.local.SlateWidgetConfig
import kotlin.math.cos
import kotlin.math.sin

enum class AiShapeStyle {
    SQUIRCLE,
    CIRCLE,
    HEXAGON,
    FRAMELESS,
    CAPSULE_LEFT,
    CAPSULE_RIGHT
}

// ============================================================================
// CORE TILE RENDERER
// ============================================================================
@Composable
fun AiTileIcon(
    target: AiTarget,
    config: SlateWidgetConfig,
    shapeStyle: AiShapeStyle = AiShapeStyle.SQUIRCLE,
    showTextLabel: Boolean = false,
    customText: String? = null,
    isPrimaryAccent: Boolean = false,
    widthDp: Int = 80,
    heightDp: Int = 80,
    modifier: GlanceModifier = GlanceModifier
) {
    val context = androidx.glance.LocalContext.current
    val isLight = config.themeMode == "LIGHT"
    val rawBgColor = Color(config.backgroundColorHex)
    val finalBgColor = rawBgColor.copy(alpha = config.opacity)
    val accentColor = Color(config.accentColorHex)

    val canvasW = widthDp * 3
    val canvasH = heightDp * 3

    val bitmap = generateTileBitmap(
        context = context,
        target = target,
        bgColor = finalBgColor,
        accentColor = accentColor,
        isLight = isLight,
        shapeStyle = shapeStyle,
        showTextLabel = showTextLabel,
        customText = customText,
        isPrimaryAccent = isPrimaryAccent,
        widthPx = canvasW,
        heightPx = canvasH
    )

    Box(
        modifier = modifier
            .clickable(actionStartActivity(AiLauncherUtils.getLaunchIntent(context, target))),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = target.title,
            modifier = GlanceModifier.fillMaxSize()
        )
    }
}

private fun generateTileBitmap(
    context: Context,
    target: AiTarget,
    bgColor: Color,
    accentColor: Color,
    isLight: Boolean,
    shapeStyle: AiShapeStyle,
    showTextLabel: Boolean,
    customText: String?,
    isPrimaryAccent: Boolean,
    widthPx: Int,
    heightPx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = widthPx.toFloat()
    val h = heightPx.toFloat()
    val minDim = minOf(w, h)

    val currentBgColor = if (isPrimaryAccent) accentColor.toArgb() else bgColor.toArgb()
    val logoColor = if (isPrimaryAccent) Color.Black.toArgb() else if (isLight) Color(0xFF1C1C1E).toArgb() else Color.White.toArgb()
    val strokeColor = if (isLight) Color(0xFFD1D1D6).toArgb() else Color(0xFF2C2C2E).toArgb()

    if (shapeStyle != AiShapeStyle.FRAMELESS) {
        val bgPaint = Paint().apply {
            isAntiAlias = true
            color = currentBgColor
            style = Paint.Style.FILL
        }

        val strokePaint = Paint().apply {
            isAntiAlias = true
            color = strokeColor
            style = Paint.Style.STROKE
            strokeWidth = if (!isPrimaryAccent) 5f else 0f
        }

        val margin = 5f
        val rect = RectF(margin, margin, w - margin, h - margin)
        val squircleRadius = minDim * 0.28f
        val fullCapRadius = (h - (margin * 2f)) / 2f

        when (shapeStyle) {
            AiShapeStyle.SQUIRCLE -> {
                canvas.drawRoundRect(rect, squircleRadius, squircleRadius, bgPaint)
                if (!isPrimaryAccent) canvas.drawRoundRect(rect, squircleRadius, squircleRadius, strokePaint)
            }
            AiShapeStyle.CIRCLE -> {
                val radius = (minDim / 2f) - margin
                canvas.drawCircle(w / 2f, h / 2f, radius, bgPaint)
                if (!isPrimaryAccent) canvas.drawCircle(w / 2f, h / 2f, radius, strokePaint)
            }
            AiShapeStyle.HEXAGON -> {
                val hexPath = Path()
                val radius = (minDim / 2f) - margin
                val cx = w / 2f
                val cy = h / 2f
                for (i in 0 until 6) {
                    val angle = Math.toRadians((60 * i - 30).toDouble())
                    val x = cx + (radius * cos(angle)).toFloat()
                    val y = cy + (radius * sin(angle)).toFloat()
                    if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
                }
                hexPath.close()
                canvas.drawPath(hexPath, bgPaint)
                if (!isPrimaryAccent) canvas.drawPath(hexPath, strokePaint)
            }
            AiShapeStyle.FRAMELESS -> {}
            AiShapeStyle.CAPSULE_LEFT -> {
                val radii = floatArrayOf(
                    fullCapRadius, fullCapRadius,
                    squircleRadius, squircleRadius,
                    squircleRadius, squircleRadius,
                    fullCapRadius, fullCapRadius
                )
                val path = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }
                canvas.drawPath(path, bgPaint)
                if (!isPrimaryAccent) canvas.drawPath(path, strokePaint)
            }
            AiShapeStyle.CAPSULE_RIGHT -> {
                val radii = floatArrayOf(
                    squircleRadius, squircleRadius,
                    fullCapRadius, fullCapRadius,
                    fullCapRadius, fullCapRadius,
                    squircleRadius, squircleRadius
                )
                val path = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }
                canvas.drawPath(path, bgPaint)
                if (!isPrimaryAccent) canvas.drawPath(path, strokePaint)
            }
        }
    }

    val resId = context.resources.getIdentifier(target.drawableResName, "drawable", context.packageName)

    if (showTextLabel && !customText.isNullOrEmpty()) {
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = logoColor
            textSize = h * 0.38f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val fontMetrics = textPaint.fontMetrics
        val textY = (h / 2f) - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(customText, w / 2f, textY, textPaint)
    } else if (resId != 0) {
        val drawable = ContextCompat.getDrawable(context, resId)
        if (drawable != null) {
            val maxLogoSize = minDim * 0.48f
            val actualLogoSize = if (isPrimaryAccent) minDim * 0.54f else maxLogoSize

            val intrinsicW = drawable.intrinsicWidth.toFloat()
            val intrinsicH = drawable.intrinsicHeight.toFloat()

            var drawW = actualLogoSize
            var drawH = actualLogoSize

            if (intrinsicW > 0f && intrinsicH > 0f) {
                val aspectRatio = intrinsicW / intrinsicH
                if (aspectRatio > 1f) {
                    drawH = actualLogoSize / aspectRatio
                } else {
                    drawW = actualLogoSize * aspectRatio
                }
            }

            val left = ((w / 2f) - (drawW / 2f)).toInt()
            val top = ((h / 2f) - (drawH / 2f)).toInt()
            val right = ((w / 2f) + (drawW / 2f)).toInt()
            val bottom = ((h / 2f) + (drawH / 2f)).toInt()

            drawable.setBounds(left, top, right, bottom)
            drawable.setTint(logoColor)
            drawable.draw(canvas)
        }
    }

    return bitmap
}

@Composable
private fun AiCardContainer(
    config: SlateWidgetConfig,
    content: @Composable () -> Unit
) {
    val isLight = config.themeMode == "LIGHT"
    val cardBg = if (isLight) Color(0xFFE5E5EA) else Color(0xFF141416)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(cardBg)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// ============================================================================
// BARS (4x1 / 3x1)
// ============================================================================

private fun drawPillBaseBitmap(
    context: Context,
    target: AiTarget,
    labelText: String,
    accentColor: Color,
    isLight: Boolean,
    widthPx: Int,
    heightPx: Int,
    logoSizePercent: Float = 0.46f,
    textSizePercent: Float = 0.36f,
    isCenteredLayout: Boolean = true
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val w = widthPx.toFloat()
    val h = heightPx.toFloat()

    val contentColor = if (isLight) Color(0xFF1C1C1E).toArgb() else Color.White.toArgb()
    val pillBgColor = accentColor.copy(alpha = 0.20f).toArgb()
    val strokeColor = accentColor.copy(alpha = 0.55f).toArgb()

    val margin = 2f
    val rect = RectF(margin, margin, w - margin, h - margin)
    val capsuleRadius = (h - (margin * 2f)) / 2f

    val bgPaint = Paint().apply {
        isAntiAlias = true
        color = pillBgColor
        style = Paint.Style.FILL
    }

    val strokePaint = Paint().apply {
        isAntiAlias = true
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
    }

    canvas.drawRoundRect(rect, capsuleRadius, capsuleRadius, bgPaint)
    canvas.drawRoundRect(rect, capsuleRadius, capsuleRadius, strokePaint)

    val logoSize = h * logoSizePercent
    val textSize = h * textSizePercent
    val itemGap = h * 0.14f

    val textPaint = Paint().apply {
        isAntiAlias = true
        color = contentColor
        this.textSize = textSize
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val measuredTextWidth = textPaint.measureText(labelText)
    val totalContentWidth = logoSize + itemGap + measuredTextWidth
    val logoXStart = if (isCenteredLayout) {
        (w - totalContentWidth) / 2f
    } else {
        capsuleRadius * 0.75f
    }

    val resId = context.resources.getIdentifier(target.drawableResName, "drawable", context.packageName)
    if (resId != 0) {
        val drawable = ContextCompat.getDrawable(context, resId)
        if (drawable != null) {
            val logoTop = ((h - logoSize) / 2f).toInt()
            val logoLeft = logoXStart.toInt()
            val logoRight = (logoLeft + logoSize).toInt()
            val logoBottom = (logoTop + logoSize).toInt()

            drawable.setBounds(logoLeft, logoTop, logoRight, logoBottom)
            drawable.setTint(contentColor)
            drawable.draw(canvas)
        }
    }

    val fontMetrics = textPaint.fontMetrics
    val textX = logoXStart + logoSize + itemGap
    val textY = (h / 2f) - (fontMetrics.ascent + fontMetrics.descent) / 2f

    canvas.drawText(labelText, textX, textY, textPaint)

    return bitmap
}

@Composable
fun PrimaryHeroPill(
    target: AiTarget,
    labelText: String,
    config: SlateWidgetConfig,
    widthDp: Int = 140,
    heightDp: Int = 60,
    modifier: GlanceModifier = GlanceModifier
) {
    val context = androidx.glance.LocalContext.current
    val isLight = config.themeMode == "LIGHT"
    val accentColor = Color(config.accentColorHex)

    val bitmap = drawPillBaseBitmap(
        context = context,
        target = target,
        labelText = labelText,
        accentColor = accentColor,
        isLight = isLight,
        widthPx = widthDp * 3,
        heightPx = heightDp * 3,
        isCenteredLayout = false
    )

    Box(
        modifier = modifier
            .clickable(actionStartActivity(AiLauncherUtils.getLaunchIntent(context, target))),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = labelText,
            modifier = GlanceModifier.fillMaxSize()
        )
    }
}

@Composable
fun SplitHeroAction(
    target: AiTarget,
    labelText: String,
    config: SlateWidgetConfig,
    widthDp: Int = 140,
    heightDp: Int = 60,
    modifier: GlanceModifier = GlanceModifier
) {
    val context = androidx.glance.LocalContext.current
    val isLight = config.themeMode == "LIGHT"
    val accentColor = Color(config.accentColorHex)

    val bitmap = drawPillBaseBitmap(
        context = context,
        target = target,
        labelText = labelText,
        accentColor = accentColor,
        isLight = isLight,
        widthPx = widthDp * 3,
        heightPx = heightDp * 3,
        isCenteredLayout = true
    )

    Box(
        modifier = modifier
            .clickable(actionStartActivity(AiLauncherUtils.getLaunchIntent(context, target))),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = labelText,
            modifier = GlanceModifier.fillMaxSize()
        )
    }
}

// BAR 1: Hero Primary Bar (Pill expands directly to vertical divider)
@Composable
fun AiBarHeroPrimary(config: SlateWidgetConfig) {
    val isLight = config.themeMode == "LIGHT"
    val cardBg = if (isLight) Color(0xFFE5E5EA) else Color(0xFF141416)
    val dividerColor = if (isLight) Color(0x33000000) else Color(0x33FFFFFF)

    val currentWidth = LocalSize.current.width

    val barHeight = 72.dp
    val outerPadding = 6.dp
    val tileH = (barHeight - (outerPadding * 2)).value.toInt()

    val squareTileDp = when {
        currentWidth >= 360.dp -> 54.dp
        currentWidth >= 300.dp -> 46.dp
        else -> 40.dp
    }
    val iconTilePx = squareTileDp.value.toInt()

    val tileGap = 4.dp
    val dividerGap = 8.dp
    val rightSideWidth = (squareTileDp * 3) + (tileGap * 2) + dividerGap + 1.dp + dividerGap
    val heroPillWidth = (currentWidth - rightSideWidth - (outerPadding * 2)).coerceAtLeast(90.dp)

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(barHeight)
                .cornerRadius(34.dp)
                .background(cardBg)
                .padding(outerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gemini Hero Pill expands to the divider
            PrimaryHeroPill(
                target = AiTarget.GEMINI_TEXT,
                labelText = "Gemini",
                config = config,
                widthDp = heroPillWidth.value.toInt(),
                heightDp = tileH,
                modifier = GlanceModifier
                    .width(heroPillWidth)
                    .fillMaxHeight()
            )

            Spacer(GlanceModifier.width(dividerGap))

            Box(
                modifier = GlanceModifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(dividerColor)
            ) {}

            Spacer(GlanceModifier.width(dividerGap))

            AiTileIcon(
                target = AiTarget.CHATGPT_TEXT,
                config = config,
                shapeStyle = AiShapeStyle.SQUIRCLE,
                widthDp = iconTilePx,
                heightDp = iconTilePx,
                modifier = GlanceModifier.size(squareTileDp)
            )
            Spacer(GlanceModifier.width(tileGap))
            AiTileIcon(
                target = AiTarget.CLAUDE,
                config = config,
                shapeStyle = AiShapeStyle.SQUIRCLE,
                widthDp = iconTilePx,
                heightDp = iconTilePx,
                modifier = GlanceModifier.size(squareTileDp)
            )
            Spacer(GlanceModifier.width(tileGap))
            AiTileIcon(
                target = AiTarget.GROK,
                config = config,
                shapeStyle = AiShapeStyle.CAPSULE_RIGHT,
                widthDp = iconTilePx,
                heightDp = iconTilePx,
                modifier = GlanceModifier.size(squareTileDp)
            )
        }
    }
}

// BAR 2: Uniform 5-Icon Dock Bar
@Composable
fun AiBarDock5Tile(config: SlateWidgetConfig) {
    val isLight = config.themeMode == "LIGHT"
    val cardBg = if (isLight) Color(0xFFE5E5EA) else Color(0xFF141416)

    val currentWidth = LocalSize.current.width
    val outerPadding = 6.dp
    val tileGap = 4.dp
    val barHeight = 72.dp
    val listSize = 5

    val availW = currentWidth - (outerPadding * 2) - (tileGap * (listSize - 1))
    val tileW = (availW / listSize).value.toInt().coerceAtLeast(32)
    val tileH = (barHeight - (outerPadding * 2)).value.toInt().coerceAtLeast(32)

    val list = listOf(
        AiTarget.GEMINI_TEXT,
        AiTarget.CHATGPT_TEXT,
        AiTarget.CLAUDE,
        AiTarget.GROK,
        AiTarget.PERPLEXITY
    )

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(barHeight)
                .cornerRadius(34.dp)
                .background(cardBg)
                .padding(outerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            list.forEachIndexed { index, target ->
                val tileShape = when (index) {
                    0 -> AiShapeStyle.CAPSULE_LEFT
                    list.size - 1 -> AiShapeStyle.CAPSULE_RIGHT
                    else -> AiShapeStyle.SQUIRCLE
                }

                AiTileIcon(
                    target = target,
                    config = config,
                    shapeStyle = tileShape,
                    widthDp = tileW,
                    heightDp = tileH,
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                )

                if (index < list.size - 1) {
                    Spacer(GlanceModifier.width(tileGap))
                }
            }
        }
    }
}

// BAR 3: Multi-Modal Action Bar
@Composable
fun AiBarCapsuleTile(config: SlateWidgetConfig) {
    val isLight = config.themeMode == "LIGHT"
    val cardBg = if (isLight) Color(0xFFE5E5EA) else Color(0xFF141416)

    val currentWidth = LocalSize.current.width
    val outerPadding = 6.dp
    val tileGap = 4.dp
    val barHeight = 72.dp
    val listSize = 4

    val availW = currentWidth - (outerPadding * 2) - (tileGap * (listSize - 1))
    val tileW = (availW / listSize).value.toInt().coerceAtLeast(32)
    val tileH = (barHeight - (outerPadding * 2)).value.toInt().coerceAtLeast(32)

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(barHeight)
                .cornerRadius(34.dp)
                .background(cardBg)
                .padding(outerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AiTileIcon(
                target = AiTarget.CHATGPT_VOICE,
                config = config,
                shapeStyle = AiShapeStyle.CAPSULE_LEFT,
                isPrimaryAccent = true,
                widthDp = tileW,
                heightDp = tileH,
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
            )
            Spacer(GlanceModifier.width(tileGap))
            AiTileIcon(
                target = AiTarget.PERPLEXITY,
                config = config,
                shapeStyle = AiShapeStyle.SQUIRCLE,
                widthDp = tileW,
                heightDp = tileH,
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
            )
            Spacer(GlanceModifier.width(tileGap))
            AiTileIcon(
                target = AiTarget.CLAUDE,
                config = config,
                shapeStyle = AiShapeStyle.SQUIRCLE,
                widthDp = tileW,
                heightDp = tileH,
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
            )
            Spacer(GlanceModifier.width(tileGap))
            AiTileIcon(
                target = AiTarget.GEMINI_TEXT,
                config = config,
                shapeStyle = AiShapeStyle.CAPSULE_RIGHT,
                isPrimaryAccent = true,
                widthDp = tileW,
                heightDp = tileH,
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
            )
        }
    }
}

// BAR 4: Dual Flagship Dock (Each hero pill stretches 50% edge-to-edge)
@Composable
fun AiBarDualFlagship(config: SlateWidgetConfig) {
    val isLight = config.themeMode == "LIGHT"
    val cardBg = if (isLight) Color(0xFFE5E5EA) else Color(0xFF141416)

    val currentWidth = LocalSize.current.width
    val barHeight = 72.dp
    val outerPadding = 6.dp
    val tileH = (barHeight - (outerPadding * 2)).value.toInt()
    val halfWidth = ((currentWidth - (outerPadding * 2) - 8.dp) / 2).coerceAtLeast(80.dp)

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(barHeight)
                .cornerRadius(34.dp)
                .background(cardBg)
                .padding(outerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SplitHeroAction(
                target = AiTarget.CHATGPT_TEXT,
                labelText = "GPT",
                config = config,
                widthDp = halfWidth.value.toInt(),
                heightDp = tileH,
                modifier = GlanceModifier
                    .width(halfWidth)
                    .fillMaxHeight()
            )
            Spacer(GlanceModifier.width(8.dp))
            SplitHeroAction(
                target = AiTarget.GEMINI_TEXT,
                labelText = "Gemini",
                config = config,
                widthDp = halfWidth.value.toInt(),
                heightDp = tileH,
                modifier = GlanceModifier
                    .width(halfWidth)
                    .fillMaxHeight()
            )
        }
    }
}

// ============================================================================
// FOLDERS
// ============================================================================

// FOLDER 1: Classic 4-Tile Grid (2x2)
@Composable
fun AiFolder4ClassicTile(config: SlateWidgetConfig) {
    val currentWidth = LocalSize.current.width
    val currentHeight = LocalSize.current.height

    val outerPadding = 10.dp
    val gap = 8.dp

    val availW = currentWidth - (outerPadding * 2)
    val availH = currentHeight - (outerPadding * 2)

    val slotW = ((availW - gap) / 2).value.toInt().coerceAtLeast(36)
    val slotH = ((availH - gap) / 2).value.toInt().coerceAtLeast(36)

    AiCardContainer(config) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AiTileIcon(
                    target = AiTarget.GEMINI_TEXT,
                    config = config,
                    shapeStyle = AiShapeStyle.SQUIRCLE,
                    widthDp = slotW,
                    heightDp = slotH,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                )
                Spacer(GlanceModifier.width(gap))
                AiTileIcon(
                    target = AiTarget.CHATGPT_TEXT,
                    config = config,
                    shapeStyle = AiShapeStyle.SQUIRCLE,
                    widthDp = slotW,
                    heightDp = slotH,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                )
            }
            Spacer(GlanceModifier.height(gap))
            Row(
                modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AiTileIcon(
                    target = AiTarget.GROK,
                    config = config,
                    shapeStyle = AiShapeStyle.SQUIRCLE,
                    widthDp = slotW,
                    heightDp = slotH,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                )
                Spacer(GlanceModifier.width(gap))
                AiTileIcon(
                    target = AiTarget.CLAUDE,
                    config = config,
                    shapeStyle = AiShapeStyle.SQUIRCLE,
                    widthDp = slotW,
                    heightDp = slotH,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                )
            }
        }
    }
}

// FOLDER 2: Bento Hero (2 Top Large + 4 Bottom Small)
@Composable
fun AiFolder6BentoHeroTile(config: SlateWidgetConfig) {
    val currentWidth = LocalSize.current.width
    val currentHeight = LocalSize.current.height

    val outerPadding = 10.dp
    val gap = 6.dp

    val availW = currentWidth - (outerPadding * 2)
    val availH = currentHeight - (outerPadding * 2)

    val topSlotW = ((availW - gap) / 2).value.toInt().coerceAtLeast(36)
    val topSlotH = ((availH - gap) * 0.55f).value.toInt().coerceAtLeast(36)

    val botSlotW = ((availW - (gap * 3)) / 4).value.toInt().coerceAtLeast(28)
    val botSlotH = ((availH - gap) * 0.45f).value.toInt().coerceAtLeast(28)

    AiCardContainer(config) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AiTileIcon(
                    target = AiTarget.GEMINI_TEXT,
                    config = config,
                    shapeStyle = AiShapeStyle.SQUIRCLE,
                    widthDp = topSlotW,
                    heightDp = topSlotH,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                )
                Spacer(GlanceModifier.width(gap))
                AiTileIcon(
                    target = AiTarget.CHATGPT_TEXT,
                    config = config,
                    shapeStyle = AiShapeStyle.SQUIRCLE,
                    widthDp = topSlotW,
                    heightDp = topSlotH,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                )
            }

            Spacer(GlanceModifier.height(gap))

            Row(
                modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val bottomList = listOf(AiTarget.CLAUDE, AiTarget.GROK, AiTarget.DEEPSEEK, AiTarget.META_AI)
                bottomList.forEachIndexed { index, target ->
                    AiTileIcon(
                        target = target,
                        config = config,
                        shapeStyle = AiShapeStyle.SQUIRCLE,
                        widthDp = botSlotW,
                        heightDp = botSlotH,
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                    )
                    if (index < bottomList.size - 1) {
                        Spacer(GlanceModifier.width(gap))
                    }
                }
            }
        }
    }
}

// FOLDER 3: Bento Side Split (2 Left Hero + 6 Right Small - 8 Apps)
@Composable
fun AiFolder8BentoSideTile(config: SlateWidgetConfig) {
    val currentWidth = LocalSize.current.width
    val currentHeight = LocalSize.current.height

    val outerPadding = 10.dp
    val gap = 6.dp

    val availW = currentWidth - (outerPadding * 2)
    val availH = currentHeight - (outerPadding * 2)

    val leftW = ((availW - gap) / 2).value.toInt().coerceAtLeast(36)
    val leftH = ((availH - gap) / 2).value.toInt().coerceAtLeast(36)

    val rightW = ((availW - gap) / 4).value.toInt().coerceAtLeast(26)
    val rightH = ((availH - (gap * 2)) / 3).value.toInt().coerceAtLeast(26)

    AiCardContainer(config) {
        Row(modifier = GlanceModifier.fillMaxSize()) {
            Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
                AiTileIcon(
                    target = AiTarget.GEMINI_TEXT,
                    config = config,
                    widthDp = leftW,
                    heightDp = leftH,
                    modifier = GlanceModifier.defaultWeight().fillMaxWidth()
                )
                Spacer(GlanceModifier.height(gap))
                AiTileIcon(
                    target = AiTarget.CHATGPT_TEXT,
                    config = config,
                    widthDp = leftW,
                    heightDp = leftH,
                    modifier = GlanceModifier.defaultWeight().fillMaxWidth()
                )
            }
            Spacer(GlanceModifier.width(gap))
            Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
                val rightGrid = listOf(
                    listOf(AiTarget.CLAUDE, AiTarget.GROK),
                    listOf(AiTarget.PERPLEXITY, AiTarget.COPILOT),
                    listOf(AiTarget.DEEPSEEK, AiTarget.META_AI)
                )
                rightGrid.forEachIndexed { rIdx, row ->
                    Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                        row.forEachIndexed { cIdx, target ->
                            AiTileIcon(
                                target = target,
                                config = config,
                                widthDp = rightW,
                                heightDp = rightH,
                                modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                            )
                            if (cIdx < row.size - 1) Spacer(GlanceModifier.width(gap))
                        }
                    }
                    if (rIdx < rightGrid.size - 1) Spacer(GlanceModifier.height(gap))
                }
            }
        }
    }
}

// FOLDER 4: 3x3 Grid Folder (9 Apps)
@Composable
fun AiFolder9GridTile(config: SlateWidgetConfig) {
    val currentWidth = LocalSize.current.width
    val currentHeight = LocalSize.current.height

    val outerPadding = 10.dp
    val gap = 4.dp

    val availW = currentWidth - (outerPadding * 2)
    val availH = currentHeight - (outerPadding * 2)

    val slotW = ((availW - (gap * 2)) / 3).value.toInt().coerceAtLeast(28)
    val slotH = ((availH - (gap * 2)) / 3).value.toInt().coerceAtLeast(28)

    AiCardContainer(config) {
        val grid = listOf(
            listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.COPILOT),
            listOf(AiTarget.GROK, AiTarget.CLAUDE, AiTarget.DEEPSEEK),
            listOf(AiTarget.PERPLEXITY, AiTarget.META_AI, AiTarget.POE)
        )
        Column(modifier = GlanceModifier.fillMaxSize()) {
            grid.forEachIndexed { rIdx, row ->
                Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                    row.forEachIndexed { cIdx, target ->
                        AiTileIcon(
                            target = target,
                            config = config,
                            widthDp = slotW,
                            heightDp = slotH,
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                        )
                        if (cIdx < row.size - 1) Spacer(GlanceModifier.width(gap))
                    }
                }
                if (rIdx < grid.size - 1) Spacer(GlanceModifier.height(gap))
            }
        }
    }
}

// FOLDER 5: 5x2 Mega Folder (10 Apps - 4x2 Size)
@Composable
fun AiFolder10MegaTile(config: SlateWidgetConfig) {
    val currentWidth = LocalSize.current.width
    val currentHeight = LocalSize.current.height

    val outerPadding = 10.dp
    val gap = 6.dp

    val availW = currentWidth - (outerPadding * 2)
    val availH = currentHeight - (outerPadding * 2)

    val slotW = ((availW - (gap * 4)) / 5).value.toInt().coerceAtLeast(28)
    val slotH = ((availH - gap) / 2).value.toInt().coerceAtLeast(28)

    AiCardContainer(config) {
        val row1 = listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.COPILOT, AiTarget.CLAUDE, AiTarget.GROK)
        val row2 = listOf(AiTarget.PERPLEXITY, AiTarget.DEEPSEEK, AiTarget.META_AI, AiTarget.POE, AiTarget.PI)

        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                row1.forEachIndexed { idx, target ->
                    AiTileIcon(
                        target = target,
                        config = config,
                        widthDp = slotW,
                        heightDp = slotH,
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                    )
                    if (idx < row1.size - 1) Spacer(GlanceModifier.width(gap))
                }
            }
            Spacer(GlanceModifier.height(gap))
            Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                row2.forEachIndexed { idx, target ->
                    AiTileIcon(
                        target = target,
                        config = config,
                        widthDp = slotW,
                        heightDp = slotH,
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                    )
                    if (idx < row2.size - 1) Spacer(GlanceModifier.width(gap))
                }
            }
        }
    }
}

// FOLDER 6: Asymmetric Bento (1 Large + 2 Medium + 4 Small - 7 Apps)
@Composable
fun AiFolder7AsymmetricTile(config: SlateWidgetConfig) {
    val currentWidth = LocalSize.current.width
    val currentHeight = LocalSize.current.height

    val outerPadding = 10.dp
    val gap = 6.dp

    val availW = currentWidth - (outerPadding * 2)
    val availH = currentHeight - (outerPadding * 2)

    val mainW = ((availW - gap) * 0.60f).value.toInt().coerceAtLeast(36)
    val mainH = ((availH - gap) * 0.55f).value.toInt().coerceAtLeast(36)

    val subW = ((availW - gap) * 0.30f).value.toInt().coerceAtLeast(26)
    val subH = ((availH - gap) * 0.40f).value.toInt().coerceAtLeast(26)

    val sideW = ((availW - gap) * 0.35f).value.toInt().coerceAtLeast(24)
    val sideH = ((availH - (gap * 3)) / 4).value.toInt().coerceAtLeast(24)

    AiCardContainer(config) {
        Row(modifier = GlanceModifier.fillMaxSize()) {
            Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
                AiTileIcon(
                    target = AiTarget.CHATGPT_TEXT,
                    config = config,
                    isPrimaryAccent = true,
                    widthDp = mainW,
                    heightDp = mainH,
                    modifier = GlanceModifier.defaultWeight().fillMaxWidth()
                )
                Spacer(GlanceModifier.height(gap))
                Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                    AiTileIcon(
                        target = AiTarget.GEMINI_TEXT,
                        config = config,
                        widthDp = subW,
                        heightDp = subH,
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                    )
                    Spacer(GlanceModifier.width(gap))
                    AiTileIcon(
                        target = AiTarget.CLAUDE,
                        config = config,
                        widthDp = subW,
                        heightDp = subH,
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                    )
                }
            }
            Spacer(GlanceModifier.width(gap))
            Column(modifier = GlanceModifier.width(sideW.dp + 8.dp).fillMaxHeight()) {
                val sideList = listOf(AiTarget.GROK, AiTarget.DEEPSEEK, AiTarget.PERPLEXITY, AiTarget.META_AI)
                sideList.forEachIndexed { idx, target ->
                    AiTileIcon(
                        target = target,
                        config = config,
                        widthDp = sideW,
                        heightDp = sideH,
                        modifier = GlanceModifier.defaultWeight().fillMaxWidth()
                    )
                    if (idx < sideList.size - 1) Spacer(GlanceModifier.height(gap))
                }
            }
        }
    }
}

// FOLDER 7: Floating Wallpaper Matrix (Frameless 6 Apps)
@Composable
fun AiFolderFloatingMatrixTile(config: SlateWidgetConfig) {
    val currentWidth = LocalSize.current.width
    val currentHeight = LocalSize.current.height

    val gap = 8.dp
    val slotW = ((currentWidth - (gap * 2)) / 3).value.toInt().coerceAtLeast(32)
    val slotH = ((currentHeight - gap) / 2).value.toInt().coerceAtLeast(32)

    val list = listOf(
        listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.CLAUDE),
        listOf(AiTarget.GROK, AiTarget.DEEPSEEK, AiTarget.PERPLEXITY)
    )
    Column(modifier = GlanceModifier.fillMaxSize()) {
        list.forEachIndexed { rIdx, row ->
            Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                row.forEachIndexed { cIdx, target ->
                    AiTileIcon(
                        target = target,
                        config = config,
                        shapeStyle = AiShapeStyle.SQUIRCLE,
                        widthDp = slotW,
                        heightDp = slotH,
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                    )
                    if (cIdx < row.size - 1) Spacer(GlanceModifier.width(gap))
                }
            }
            if (rIdx < list.size - 1) Spacer(GlanceModifier.height(gap))
        }
    }
}