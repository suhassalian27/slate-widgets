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


@Composable
private fun AiFolderCardContainer(
    config: SlateWidgetConfig,
    content: @Composable () -> Unit
) {
    val isLight = config.themeMode == "LIGHT"
    val cardBg = if (isLight) Color(0xFFE5E5EA) else Color(0xFF141416)

    val currentWidth = LocalSize.current.width
    val currentHeight = LocalSize.current.height
    // Lock outer card to a strict 1:1 square card centered in the cell
    val squareCardSize = minOf(currentWidth, currentHeight)

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .size(squareCardSize)
                .cornerRadius(24.dp)
                .background(cardBg)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
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
    val squareCardSize = minOf(currentWidth, currentHeight)

    val outerPadding = 10.dp
    val gap = 8.dp
    val availSize = squareCardSize - (outerPadding * 2)

    val tileSizeDp = ((availSize - gap) / 2).value.toInt().coerceAtLeast(36)

    AiFolderCardContainer(config) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().height(tileSizeDp.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AiTileIcon(target = AiTarget.GEMINI_TEXT, config = config, shapeStyle = AiShapeStyle.SQUIRCLE, widthDp = tileSizeDp, heightDp = tileSizeDp, modifier = GlanceModifier.size(tileSizeDp.dp))
                Spacer(GlanceModifier.width(gap))
                AiTileIcon(target = AiTarget.CHATGPT_TEXT, config = config, shapeStyle = AiShapeStyle.SQUIRCLE, widthDp = tileSizeDp, heightDp = tileSizeDp, modifier = GlanceModifier.size(tileSizeDp.dp))
            }
            Spacer(GlanceModifier.height(gap))
            Row(
                modifier = GlanceModifier.fillMaxWidth().height(tileSizeDp.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AiTileIcon(target = AiTarget.GROK, config = config, shapeStyle = AiShapeStyle.SQUIRCLE, widthDp = tileSizeDp, heightDp = tileSizeDp, modifier = GlanceModifier.size(tileSizeDp.dp))
                Spacer(GlanceModifier.width(gap))
                AiTileIcon(target = AiTarget.CLAUDE, config = config, shapeStyle = AiShapeStyle.SQUIRCLE, widthDp = tileSizeDp, heightDp = tileSizeDp, modifier = GlanceModifier.size(tileSizeDp.dp))
            }
        }
    }
}

// FOLDER 2: Bento Hero (2 Top Large + 4 Bottom Small)
@Composable
fun AiFolder6BentoHeroTile(config: SlateWidgetConfig) {
    val isLight = config.themeMode == "LIGHT"
    val cardBg = if (isLight) Color(0xFFE5E5EA) else Color(0xFF141416)

    val currentWidth = LocalSize.current.width
    val currentHeight = LocalSize.current.height

    val outerPadding = 12.dp
    val gap = 6.dp

    val availW = currentWidth - (outerPadding * 2)
    val availH = currentHeight - (outerPadding * 2)

    // Constrain small tile size by BOTH width and height limits
    val botTileFromW = (availW - (gap * 3)) / 4
    val botTileFromH = (availH - (gap * 2)) / 3

    val botTileSize = minOf(botTileFromW, botTileFromH).value.toInt().coerceAtLeast(20)
    val gapPx = gap.value.toInt()
    val topTileSize = (botTileSize * 2) + gapPx

    // Calculate exact outer bounds that strictly fit inside cell boundaries
    val cardW = ((botTileSize * 4) + (gapPx * 3)).dp + (outerPadding * 2)
    val cardH = ((botTileSize * 3) + (gapPx * 2)).dp + (outerPadding * 2)

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .width(cardW)
                .height(cardH)
                .cornerRadius(24.dp)
                .background(cardBg)
                .padding(outerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top Row: 2 Big Squares
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AiTileIcon(
                        target = AiTarget.GEMINI_TEXT,
                        config = config,
                        shapeStyle = AiShapeStyle.SQUIRCLE,
                        widthDp = topTileSize,
                        heightDp = topTileSize,
                        modifier = GlanceModifier.size(topTileSize.dp)
                    )
                    Spacer(GlanceModifier.width(gap))
                    AiTileIcon(
                        target = AiTarget.CHATGPT_TEXT,
                        config = config,
                        shapeStyle = AiShapeStyle.SQUIRCLE,
                        widthDp = topTileSize,
                        heightDp = topTileSize,
                        modifier = GlanceModifier.size(topTileSize.dp)
                    )
                }

                Spacer(GlanceModifier.height(gap))

                // Bottom Row: 4 Small Squares
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val bottomList = listOf(AiTarget.CLAUDE, AiTarget.GROK, AiTarget.DEEPSEEK, AiTarget.META_AI)
                    bottomList.forEachIndexed { index, target ->
                        AiTileIcon(
                            target = target,
                            config = config,
                            shapeStyle = AiShapeStyle.SQUIRCLE,
                            widthDp = botTileSize,
                            heightDp = botTileSize,
                            modifier = GlanceModifier.size(botTileSize.dp)
                        )
                        if (index < bottomList.size - 1) Spacer(GlanceModifier.width(gap))
                    }
                }
            }
        }
    }
}

// FOLDER 3: Bento Side Split (2 Left Hero + 6 Right Small)
@Composable
fun AiFolder8BentoSideTile(config: SlateWidgetConfig) {
    val isLight = config.themeMode == "LIGHT"
    val cardBg = if (isLight) Color(0xFFE5E5EA) else Color(0xFF141416)

    val currentWidth = LocalSize.current.width
    val currentHeight = LocalSize.current.height

    val outerPadding = 12.dp
    val gap = 5.dp

    val availW = currentWidth - (outerPadding * 2)
    val availH = currentHeight - (outerPadding * 2)

    // Constrain right tile size by BOTH width and height limits
    val rightTileFromW = (availW - (gap * 2.5f)) / 3.5f
    val rightTileFromH = (availH - (gap * 2)) / 3

    val rightTileSize = minOf(rightTileFromW, rightTileFromH).value.toInt().coerceAtLeast(20)
    val gapPx = gap.value.toInt()
    val leftTileSize = (rightTileSize * 3 + gapPx) / 2

    // Calculate exact outer bounds that strictly fit inside cell boundaries
    val cardW = (leftTileSize + (rightTileSize * 2) + (gapPx * 2)).dp + (outerPadding * 2)
    val cardH = ((rightTileSize * 3) + (gapPx * 2)).dp + (outerPadding * 2)

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .width(cardW)
                .height(cardH)
                .cornerRadius(24.dp)
                .background(cardBg)
                .padding(outerPadding),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Left Column: 2 Big Stacked Squares
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AiTileIcon(
                        target = AiTarget.GEMINI_TEXT,
                        config = config,
                        shapeStyle = AiShapeStyle.SQUIRCLE,
                        widthDp = leftTileSize,
                        heightDp = leftTileSize,
                        modifier = GlanceModifier.size(leftTileSize.dp)
                    )
                    Spacer(GlanceModifier.height(gap))
                    AiTileIcon(
                        target = AiTarget.CHATGPT_TEXT,
                        config = config,
                        shapeStyle = AiShapeStyle.SQUIRCLE,
                        widthDp = leftTileSize,
                        heightDp = leftTileSize,
                        modifier = GlanceModifier.size(leftTileSize.dp)
                    )
                }

                Spacer(GlanceModifier.width(gap))

                // Right Block: 6 Small Squares (2 Columns x 3 Rows)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val rightGrid = listOf(
                        listOf(AiTarget.CLAUDE, AiTarget.GROK),
                        listOf(AiTarget.PERPLEXITY, AiTarget.COPILOT),
                        listOf(AiTarget.DEEPSEEK, AiTarget.META_AI)
                    )
                    rightGrid.forEachIndexed { rIdx, row ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            row.forEachIndexed { cIdx, target ->
                                AiTileIcon(
                                    target = target,
                                    config = config,
                                    shapeStyle = AiShapeStyle.SQUIRCLE,
                                    widthDp = rightTileSize,
                                    heightDp = rightTileSize,
                                    modifier = GlanceModifier.size(rightTileSize.dp)
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
}

// FOLDER 4: 3x3 Grid Folder (9 Apps)
@Composable
fun AiFolder9GridTile(config: SlateWidgetConfig) {
    val currentWidth = LocalSize.current.width
    val currentHeight = LocalSize.current.height
    val squareCardSize = minOf(currentWidth, currentHeight)

    val outerPadding = 10.dp
    val gap = 4.dp
    val availSize = squareCardSize - (outerPadding * 2)

    val tileSizeDp = ((availSize - (gap * 2)) / 3).value.toInt().coerceAtLeast(28)

    AiFolderCardContainer(config) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val grid = listOf(
                listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.COPILOT),
                listOf(AiTarget.GROK, AiTarget.CLAUDE, AiTarget.DEEPSEEK),
                listOf(AiTarget.PERPLEXITY, AiTarget.META_AI, AiTarget.POE)
            )
            grid.forEachIndexed { rIdx, row ->
                Row(
                    modifier = GlanceModifier.height(tileSizeDp.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEachIndexed { cIdx, target ->
                        AiTileIcon(target = target, config = config, widthDp = tileSizeDp, heightDp = tileSizeDp, modifier = GlanceModifier.size(tileSizeDp.dp))
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
    val isLight = config.themeMode == "LIGHT"
    val cardBg = if (isLight) Color(0xFFE5E5EA) else Color(0xFF141416)

    val currentWidth = LocalSize.current.width
    val currentHeight = LocalSize.current.height

    val outerPadding = 12.dp
    val gap = 6.dp

    val availW = currentWidth - (outerPadding * 2)
    val availH = currentHeight - (outerPadding * 2)

    // Calculate maximum possible square tile size fitting both width and height constraints
    val tileFromW = (availW - (gap * 4)) / 5
    val tileFromH = (availH - gap) / 2
    val tileSize = minOf(tileFromW, tileFromH).value.toInt().coerceAtLeast(24)

    val gapPx = gap.value.toInt()
    val cardW = ((tileSize * 5) + (gapPx * 4)).dp + (outerPadding * 2)
    val cardH = ((tileSize * 2) + gapPx).dp + (outerPadding * 2)

    val row1 = listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.COPILOT, AiTarget.CLAUDE, AiTarget.GROK)
    val row2 = listOf(AiTarget.PERPLEXITY, AiTarget.DEEPSEEK, AiTarget.META_AI, AiTarget.POE, AiTarget.PI)

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .width(cardW)
                .height(cardH)
                .cornerRadius(24.dp)
                .background(cardBg)
                .padding(outerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Row 1
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    row1.forEachIndexed { idx, target ->
                        AiTileIcon(
                            target = target,
                            config = config,
                            shapeStyle = AiShapeStyle.SQUIRCLE,
                            widthDp = tileSize,
                            heightDp = tileSize,
                            modifier = GlanceModifier.size(tileSize.dp)
                        )
                        if (idx < row1.size - 1) Spacer(GlanceModifier.width(gap))
                    }
                }

                Spacer(GlanceModifier.height(gap))

                // Row 2
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    row2.forEachIndexed { idx, target ->
                        AiTileIcon(
                            target = target,
                            config = config,
                            shapeStyle = AiShapeStyle.SQUIRCLE,
                            widthDp = tileSize,
                            heightDp = tileSize,
                            modifier = GlanceModifier.size(tileSize.dp)
                        )
                        if (idx < row2.size - 1) Spacer(GlanceModifier.width(gap))
                    }
                }
            }
        }
    }
}

// FOLDER 6: Asymmetric Bento (1 Large + 2 Medium + 4 Small - 7 Apps)
@Composable
fun AiFolder7AsymmetricTile(config: SlateWidgetConfig) {
    val isLight = config.themeMode == "LIGHT"
    val cardBg = if (isLight) Color(0xFFE5E5EA) else Color(0xFF141416)

    val currentWidth = LocalSize.current.width
    val currentHeight = LocalSize.current.height

    val outerPadding = 12.dp
    val gap = 5.dp
    val gapPx = gap.value.toInt()

    val availW = currentWidth - (outerPadding * 2)
    val availH = currentHeight - (outerPadding * 2)

    // Calculate small unit size S based on 4-unit grid (4S + 3g)
    val unitFromW = (availW - (gap * 3)) / 4
    val unitFromH = (availH - (gap * 3)) / 4
    val smallSize = minOf(unitFromW, unitFromH).value.toInt().coerceAtLeast(20)

    // Big tile dimensions: 3S + 2g (1:1 Perfect Square)
    val bigSize = (smallSize * 3) + (gapPx * 2)

    // Medium tile dimensions (under Big tile)
    val medW = ((bigSize - gapPx) / 2).coerceAtLeast(20)
    val medH = smallSize

    // Card dimensions: 4S + 3g (Equal bounds on all sides)
    val cardW = ((smallSize * 4) + (gapPx * 3)).dp + (outerPadding * 2)
    val cardH = cardW

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .width(cardW)
                .height(cardH)
                .cornerRadius(24.dp)
                .background(cardBg)
                .padding(outerPadding),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Left Column: 1 Big Square + 2 Medium Tiles
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1 Big Square Tile
                    AiTileIcon(
                        target = AiTarget.CHATGPT_TEXT,
                        config = config,
                        isPrimaryAccent = true,
                        widthDp = bigSize,
                        heightDp = bigSize,
                        modifier = GlanceModifier.size(bigSize.dp)
                    )

                    Spacer(GlanceModifier.height(gap))

                    // Bottom Row: 2 Medium Tiles
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AiTileIcon(
                            target = AiTarget.GEMINI_TEXT,
                            config = config,
                            shapeStyle = AiShapeStyle.SQUIRCLE,
                            widthDp = medW,
                            heightDp = medH,
                            modifier = GlanceModifier.size(width = medW.dp, height = medH.dp)
                        )
                        Spacer(GlanceModifier.width(gap))
                        AiTileIcon(
                            target = AiTarget.CLAUDE,
                            config = config,
                            shapeStyle = AiShapeStyle.SQUIRCLE,
                            widthDp = medW,
                            heightDp = medH,
                            modifier = GlanceModifier.size(width = medW.dp, height = medH.dp)
                        )
                    }
                }

                Spacer(GlanceModifier.width(gap))

                // Right Column: 4 Small Square Tiles Stacked Vertically
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val rightList = listOf(AiTarget.GROK, AiTarget.COPILOT, AiTarget.PERPLEXITY, AiTarget.META_AI)
                    rightList.forEachIndexed { idx, target ->
                        AiTileIcon(
                            target = target,
                            config = config,
                            shapeStyle = AiShapeStyle.SQUIRCLE,
                            widthDp = smallSize,
                            heightDp = smallSize,
                            modifier = GlanceModifier.size(smallSize.dp)
                        )
                        if (idx < rightList.size - 1) Spacer(GlanceModifier.height(gap))
                    }
                }
            }
        }
    }
}

