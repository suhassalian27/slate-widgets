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

    // 1. Draw Tile Container Background
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
            strokeWidth = 6f
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
                    fullCapRadius, fullCapRadius,       // Top-Left (Full curve)
                    squircleRadius, squircleRadius,     // Top-Right (Standard)
                    squircleRadius, squircleRadius,     // Bottom-Right (Standard)
                    fullCapRadius, fullCapRadius        // Bottom-Left (Full curve)
                )
                val path = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }
                canvas.drawPath(path, bgPaint)
                if (!isPrimaryAccent) canvas.drawPath(path, strokePaint)
            }
            AiShapeStyle.CAPSULE_RIGHT -> {
                val radii = floatArrayOf(
                    squircleRadius, squircleRadius,     // Top-Left (Standard)
                    fullCapRadius, fullCapRadius,       // Top-Right (Full curve)
                    fullCapRadius, fullCapRadius,       // Bottom-Right (Full curve)
                    squircleRadius, squircleRadius      // Bottom-Left (Standard)
                )
                val path = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }
                canvas.drawPath(path, bgPaint)
                if (!isPrimaryAccent) canvas.drawPath(path, strokePaint)
            }
        }
    }

    // 2. Draw Vector Logo & Optional Label
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
            val intrinsicW = drawable.intrinsicWidth.toFloat()
            val intrinsicH = drawable.intrinsicHeight.toFloat()

            var drawW = maxLogoSize
            var drawH = maxLogoSize

            if (intrinsicW > 0f && intrinsicH > 0f) {
                val aspectRatio = intrinsicW / intrinsicH
                if (aspectRatio > 1f) {
                    drawH = maxLogoSize / aspectRatio
                } else {
                    drawW = maxLogoSize * aspectRatio
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
private fun generateHeroPillBitmap(
    context: Context,
    target: AiTarget,
    labelText: String,
    accentColor: Color,
    bgColor: Color,
    isLight: Boolean,
    widthPx: Int,
    heightPx: Int
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

    val logoSize = h * 0.46f
    val textSize = h * 0.36f
    val itemGap = h * 0.16f
    val leftPadding = capsuleRadius * 0.75f

    val textPaint = Paint().apply {
        isAntiAlias = true
        color = contentColor
        this.textSize = textSize
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val resId = context.resources.getIdentifier(target.drawableResName, "drawable", context.packageName)
    if (resId != 0) {
        val drawable = ContextCompat.getDrawable(context, resId)
        if (drawable != null) {
            val logoTop = ((h - logoSize) / 2f).toInt()
            val logoLeft = leftPadding.toInt()
            val logoRight = (logoLeft + logoSize).toInt()
            val logoBottom = (logoTop + logoSize).toInt()

            drawable.setBounds(logoLeft, logoTop, logoRight, logoBottom)
            drawable.setTint(contentColor)
            drawable.draw(canvas)
        }
    }

    val fontMetrics = textPaint.fontMetrics
    val textX = leftPadding + logoSize + itemGap
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
    val rawBgColor = Color(config.backgroundColorHex)
    val finalBgColor = rawBgColor.copy(alpha = config.opacity)
    val accentColor = Color(config.accentColorHex)

    val canvasW = widthDp * 3
    val canvasH = heightDp * 3

    val bitmap = generateHeroPillBitmap(
        context = context,
        target = target,
        labelText = labelText,
        accentColor = accentColor,
        bgColor = finalBgColor,
        isLight = isLight,
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
            contentDescription = labelText,
            modifier = GlanceModifier.fillMaxSize()
        )
    }
}

// BAR 1: Primary Bar (Gemini Hero Pill + ChatGPT, Claude, Grok)
@Composable
fun AiBarPrimaryTile(config: SlateWidgetConfig) {
    val isLight = config.themeMode == "LIGHT"
    val cardBg = if (isLight) Color(0xFFE5E5EA) else Color(0xFF141416)
    val dividerColor = if (isLight) Color(0x33000000) else Color(0x33FFFFFF)

    // Detect actual launcher width
    val currentWidth = LocalSize.current.width

    val barHeight = 72.dp
    val outerPadding = 6.dp
    val tileH = (barHeight - (outerPadding * 2)).value.toInt() // 60px

    // 1. Dynamic icon sizing tiers
    val squareTileDp = when {
        currentWidth >= 360.dp -> 60.dp
        currentWidth >= 300.dp -> 48.dp
        else -> 40.dp
    }
    val iconTilePx = squareTileDp.value.toInt()

    // 2. Math-guaranteed Gemini width calculation (Prevents divider collision)
    val tileGap = 4.dp
    val dividerGap = 8.dp
    val rightSideOccupiedWidth = (squareTileDp * 3) + (tileGap * 2) + dividerGap + 1.dp + dividerGap
    val heroPillWidth = (currentWidth - rightSideOccupiedWidth - (outerPadding * 2)).coerceAtLeast(80.dp)

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
            // 3. Hero Pill (Gemini) dynamically fits remaining left space
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

            Spacer(GlanceModifier.defaultWeight())

            // 4. Vertical Divider
            Box(
                modifier = GlanceModifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(dividerColor)
            ) {}

            Spacer(GlanceModifier.width(dividerGap))

            // 5. Square 1:1 Secondary Icons
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
            // Far-Right Icon (Grok) - Hugs Outer Capsule Curve
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
                    shapeStyle = tileShape, // 👈 FIXED: Uses tileShape instead of hardcoded SQUIRCLE
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

// BAR 3: Capsule Bar with 1 Pill + 3 Circular Badges
@Composable
fun AiBarCapsuleTile(config: SlateWidgetConfig) {
    AiCardContainer(config) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AiTileIcon(
                target = AiTarget.CHATGPT_TEXT,
                config = config,
                showTextLabel = true,
                customText = "ChatGPT",
                modifier = GlanceModifier.defaultWeight().fillMaxHeight()
            )
            Spacer(GlanceModifier.width(10.dp))
            AiTileIcon(target = AiTarget.GEMINI_TEXT, config = config, shapeStyle = AiShapeStyle.CIRCLE, modifier = GlanceModifier.size(44.dp))
            Spacer(GlanceModifier.width(6.dp))
            AiTileIcon(target = AiTarget.GROK, config = config, shapeStyle = AiShapeStyle.CIRCLE, modifier = GlanceModifier.size(44.dp))
            Spacer(GlanceModifier.width(6.dp))
            AiTileIcon(target = AiTarget.DEEPSEEK, config = config, shapeStyle = AiShapeStyle.CIRCLE, modifier = GlanceModifier.size(44.dp))
        }
    }
}

// BAR 4: Dual Split Hero Bar (ChatGPT Voice + Gemini) + 3 Small Icons
@Composable
fun AiBarSplitActionTile(config: SlateWidgetConfig) {
    AiCardContainer(config) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AiTileIcon(target = AiTarget.CHATGPT_VOICE, config = config, isPrimaryAccent = true, modifier = GlanceModifier.size(48.dp))
            Spacer(GlanceModifier.width(6.dp))
            AiTileIcon(target = AiTarget.GEMINI_TEXT, config = config, isPrimaryAccent = true, modifier = GlanceModifier.size(48.dp))
            Spacer(GlanceModifier.width(10.dp))
            AiTileIcon(target = AiTarget.CLAUDE, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
            Spacer(GlanceModifier.width(6.dp))
            AiTileIcon(target = AiTarget.GROK, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
            Spacer(GlanceModifier.width(6.dp))
            AiTileIcon(target = AiTarget.COPILOT, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
        }
    }
}

// ============================================================================
// FOLDERS (2x2 & 4x2 GRID & BENTO LAYOUTS)
// ============================================================================

// FOLDER 1: Classic 4-Tile Grid (2x2)
@Composable
fun AiFolder4ClassicTile(config: SlateWidgetConfig) {
    AiCardContainer(config) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                AiTileIcon(target = AiTarget.GEMINI_TEXT, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                Spacer(GlanceModifier.width(8.dp))
                AiTileIcon(target = AiTarget.CHATGPT_TEXT, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
            }
            Spacer(GlanceModifier.height(8.dp))
            Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                AiTileIcon(target = AiTarget.GROK, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                Spacer(GlanceModifier.width(8.dp))
                AiTileIcon(target = AiTarget.CLAUDE, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
            }
        }
    }
}

// FOLDER 2: Bento Hero (2 Top Large + 4 Bottom Small - 6 Apps)
@Composable
fun AiFolder6BentoHeroTile(config: SlateWidgetConfig) {
    AiCardContainer(config) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                AiTileIcon(target = AiTarget.GEMINI_TEXT, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                Spacer(GlanceModifier.width(6.dp))
                AiTileIcon(target = AiTarget.CHATGPT_TEXT, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
            }
            Spacer(GlanceModifier.height(6.dp))
            Row(modifier = GlanceModifier.height(44.dp).fillMaxWidth()) {
                AiTileIcon(target = AiTarget.CLAUDE, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                Spacer(GlanceModifier.width(6.dp))
                AiTileIcon(target = AiTarget.GROK, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                Spacer(GlanceModifier.width(6.dp))
                AiTileIcon(target = AiTarget.DEEPSEEK, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                Spacer(GlanceModifier.width(6.dp))
                AiTileIcon(target = AiTarget.META_AI, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
            }
        }
    }
}

// FOLDER 3: Bento Side Split (2 Left Hero + 6 Right Small - 8 Apps)
@Composable
fun AiFolder8BentoSideTile(config: SlateWidgetConfig) {
    AiCardContainer(config) {
        Row(modifier = GlanceModifier.fillMaxSize()) {
            Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
                AiTileIcon(target = AiTarget.GEMINI_TEXT, config = config, modifier = GlanceModifier.defaultWeight().fillMaxWidth())
                Spacer(GlanceModifier.height(6.dp))
                AiTileIcon(target = AiTarget.CHATGPT_TEXT, config = config, modifier = GlanceModifier.defaultWeight().fillMaxWidth())
            }
            Spacer(GlanceModifier.width(6.dp))
            Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
                Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                    AiTileIcon(target = AiTarget.CLAUDE, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                    Spacer(GlanceModifier.width(4.dp))
                    AiTileIcon(target = AiTarget.GROK, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                }
                Spacer(GlanceModifier.height(4.dp))
                Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                    AiTileIcon(target = AiTarget.PERPLEXITY, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                    Spacer(GlanceModifier.width(4.dp))
                    AiTileIcon(target = AiTarget.COPILOT, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                }
                Spacer(GlanceModifier.height(4.dp))
                Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                    AiTileIcon(target = AiTarget.DEEPSEEK, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                    Spacer(GlanceModifier.width(4.dp))
                    AiTileIcon(target = AiTarget.META_AI, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                }
            }
        }
    }
}

// FOLDER 4: 3x3 Grid Folder (9 Apps)
@Composable
fun AiFolder9GridTile(config: SlateWidgetConfig) {
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
                        AiTileIcon(target = target, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                        if (cIdx < row.size - 1) Spacer(GlanceModifier.width(4.dp))
                    }
                }
                if (rIdx < grid.size - 1) Spacer(GlanceModifier.height(4.dp))
            }
        }
    }
}

// FOLDER 5: 5x2 Mega Folder (10 Apps - 4x2 Size)
@Composable
fun AiFolder10MegaTile(config: SlateWidgetConfig) {
    AiCardContainer(config) {
        val row1 = listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.COPILOT, AiTarget.CLAUDE, AiTarget.GROK)
        val row2 = listOf(AiTarget.PERPLEXITY, AiTarget.DEEPSEEK, AiTarget.META_AI, AiTarget.POE, AiTarget.PI)

        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                row1.forEachIndexed { idx, target ->
                    AiTileIcon(target = target, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                    if (idx < row1.size - 1) Spacer(GlanceModifier.width(6.dp))
                }
            }
            Spacer(GlanceModifier.height(6.dp))
            Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                row2.forEachIndexed { idx, target ->
                    AiTileIcon(target = target, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                    if (idx < row2.size - 1) Spacer(GlanceModifier.width(6.dp))
                }
            }
        }
    }
}

// FOLDER 6: Asymmetric Bento (1 Large + 2 Medium + 4 Small - 7 Apps)
@Composable
fun AiFolder7AsymmetricTile(config: SlateWidgetConfig) {
    AiCardContainer(config) {
        Row(modifier = GlanceModifier.fillMaxSize()) {
            Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
                AiTileIcon(target = AiTarget.CHATGPT_TEXT, config = config, isPrimaryAccent = true, modifier = GlanceModifier.defaultWeight().fillMaxWidth())
                Spacer(GlanceModifier.height(4.dp))
                Row(modifier = GlanceModifier.height(44.dp).fillMaxWidth()) {
                    AiTileIcon(target = AiTarget.GEMINI_TEXT, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                    Spacer(GlanceModifier.width(4.dp))
                    AiTileIcon(target = AiTarget.CLAUDE, config = config, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                }
            }
            Spacer(GlanceModifier.width(6.dp))
            Column(modifier = GlanceModifier.width(52.dp).fillMaxHeight()) {
                val sideList = listOf(AiTarget.GROK, AiTarget.DEEPSEEK, AiTarget.PERPLEXITY, AiTarget.META_AI)
                sideList.forEachIndexed { idx, target ->
                    AiTileIcon(target = target, config = config, modifier = GlanceModifier.defaultWeight().fillMaxWidth())
                    if (idx < sideList.size - 1) Spacer(GlanceModifier.height(4.dp))
                }
            }
        }
    }
}

// FOLDER 7: Floating Wallpaper Matrix (Frameless 6 Apps)
@Composable
fun AiFolderFloatingMatrixTile(config: SlateWidgetConfig) {
    val list = listOf(
        listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.CLAUDE),
        listOf(AiTarget.GROK, AiTarget.DEEPSEEK, AiTarget.PERPLEXITY)
    )
    Column(modifier = GlanceModifier.fillMaxSize()) {
        list.forEachIndexed { rIdx, row ->
            Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                row.forEachIndexed { cIdx, target ->
                    AiTileIcon(target = target, config = config, shapeStyle = AiShapeStyle.SQUIRCLE, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                    if (cIdx < row.size - 1) Spacer(GlanceModifier.width(8.dp))
                }
            }
            if (rIdx < list.size - 1) Spacer(GlanceModifier.height(8.dp))
        }
    }
}