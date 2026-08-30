package com.altusix.slate.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class NavItem(
    val title: String,
    val icon: ImageVector
) {
    WIDGETS("Widget", Icons.Outlined.GridView),
    WALLPAPER("Wallpaper", Icons.Outlined.Image),
    THEME("Theme", Icons.Outlined.ColorLens),
    SETTINGS("Setting", Icons.Outlined.Settings)
}

private fun calculateLuminance(color: Color): Float {
    return 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
}

@Composable
fun BottomNavBar(
    selectedItem: NavItem,
    onItemSelected: (NavItem) -> Unit,
    modifier: Modifier = Modifier,
    barBackgroundColor: Color = Color(0xFF0C0B0A),  // <--- Capsule background color
    accentColor: Color = Color(0xFF7C4DFF),         // <--- Active circle bubble color
    unselectedIconColor: Color = Color(0xFF8E8E93)  // <--- Inactive icon color
) {
    val items = remember { NavItem.entries.toTypedArray() }
    val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)

    val density = LocalDensity.current

    // --- 1. OUTER BAR SPACING & HEIGHT ---
    val outerPaddingHorizontal = 20.dp
    val outerPaddingBottom = 16.dp

    val topHeadroomDp = 28.dp
    val barHeightDp = 60.dp
    val totalHeightDp = topHeadroomDp + barHeightDp

    // --- 2. CRADLE & BUBBLE SHAPE CONTROLS ---
    val circleSizeDp = 52.dp        // Size of the purple circle bubble
    val cradleWidthDp = 56.dp       // Horizontal radius of the cradle opening
    val cradleDepthDp = 32.dp       // Vertical depth of the cradle cutout

    // Bezier Curve Fine-Tuning Ratios
    val cradleTopRatio = 0.50f
    val cradleBottomRatio = 0.52f

    // Inner side padding for tab slot centering
    val innerSidePaddingDp = 40.dp

    val topHeadroomPx = with(density) { topHeadroomDp.toPx() }
    val barHeightPx = with(density) { barHeightDp.toPx() }
    val cradleWidthPx = with(density) { cradleWidthDp.toPx() }
    val cradleDepthPx = with(density) { cradleDepthDp.toPx() }

    // Smart contrast calculations
    val accentLuminance = remember(accentColor) { calculateLuminance(accentColor) }
    val isLightAccent = accentLuminance > 0.55f
    val isDarkAccent = accentLuminance < 0.20f
    val activeIconColor = if (isLightAccent) Color(0xFF121214) else Color.White

    // Smart thin border stroke color based on accent theme luminance
    val bubbleBorderColor = remember(accentColor, isDarkAccent, isLightAccent) {
        when {
            isDarkAccent -> Color.White.copy(alpha = 0.30f)
            isLightAccent -> Color.Black.copy(alpha = 0.15f)
            else -> Color.White.copy(alpha = 0.20f)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = outerPaddingHorizontal, vertical = outerPaddingBottom)
            .height(totalHeightDp)
    ) {
        val totalWidthPx = with(density) { maxWidth.toPx() }
        val pillEndRadiusPx = barHeightPx / 2f

        // Prevent cradle from invading rounded stadium end-caps
        val minCenterX = pillEndRadiusPx + cradleWidthPx + with(density) { 4.dp.toPx() }
        val maxCenterX = totalWidthPx - pillEndRadiusPx - cradleWidthPx - with(density) { 4.dp.toPx() }

        val tabCentersPx = remember(totalWidthPx) {
            FloatArray(items.size) { i ->
                if (items.size > 1) {
                    minCenterX + i * (maxCenterX - minCenterX) / (items.size - 1)
                } else {
                    totalWidthPx / 2f
                }
            }
        }

        val targetCenterX = tabCentersPx.getOrElse(selectedIndex) { totalWidthPx / 2f }

        val animatedCenterX by animateFloatAsState(
            targetValue = targetCenterX,
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = Spring.StiffnessLow
            ),
            label = "BottomNavCurveAnimation"
        )

        // 1. Fully Customizable Floating Capsule Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeightDp)
        ) {
            val w = size.width
            val barTopY = topHeadroomPx
            val cX = animatedCenterX
            val cY = barTopY

            val curveStartX = cX - cradleWidthPx
            val curveEndX = cX + cradleWidthPx

            val path = Path().apply {
                moveTo(pillEndRadiusPx, barTopY)
                lineTo(curveStartX, barTopY)

                // Left wing Bezier curve using cradleTopRatio & cradleBottomRatio
                cubicTo(
                    x1 = cX - (cradleWidthPx * cradleTopRatio),
                    y1 = cY,
                    x2 = cX - (cradleWidthPx * cradleBottomRatio),
                    y2 = cY + cradleDepthPx,
                    x3 = cX,
                    y3 = cY + cradleDepthPx
                )
                // Right wing Bezier curve using cradleTopRatio & cradleBottomRatio
                cubicTo(
                    x1 = cX + (cradleWidthPx * cradleBottomRatio),
                    y1 = cY + cradleDepthPx,
                    x2 = cX + (cradleWidthPx * cradleTopRatio),
                    y2 = cY,
                    x3 = curveEndX,
                    y3 = cY
                )

                lineTo(w - pillEndRadiusPx, barTopY)

                // Right Stadium End Semicircle
                arcTo(
                    rect = Rect(w - barHeightPx, barTopY, w, barTopY + barHeightPx),
                    startAngleDegrees = -90f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )

                lineTo(pillEndRadiusPx, barTopY + barHeightPx)

                // Left Stadium End Semicircle
                arcTo(
                    rect = Rect(0f, barTopY, barHeightPx, barTopY + barHeightPx),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )

                close()
            }

            drawPath(path = path, color = barBackgroundColor)
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.12f),
                style = Stroke(width = 1.5f * density.density)
            )
        }

        // 2. Sliding Circle Bubble with Clean Thin Border Stroke
        val bubbleOffsetY = topHeadroomDp - (circleSizeDp / 2f) + 3.dp
        val bubbleOffsetDp = with(density) { (animatedCenterX - (with(density) { circleSizeDp.toPx() } / 2f)).toDp() }

        Box(
            modifier = Modifier
                .offset(x = bubbleOffsetDp, y = bubbleOffsetY)
                .size(circleSizeDp)
                .border(width = 1.dp, color = bubbleBorderColor, shape = CircleShape)
                .clip(CircleShape)
                .background(accentColor)
        )

        // 3. Navigation Items Row
        val tabSlotWidthDp = if (items.size > 1) {
            with(density) { ((maxCenterX - minCenterX) / (items.size - 1)).toDp() }
        } else {
            100.dp
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeightDp)
                .align(Alignment.BottomCenter)
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                val tabCenterX = tabCentersPx.getOrElse(index) { 0f }
                val tabLeftDp = with(density) { (tabCenterX - (with(density) { tabSlotWidthDp.toPx() } / 2f)).toDp() }

                val targetIconOffsetY = if (isSelected) (-26.5).dp else 0.dp

                val iconOffsetY by animateDpAsState(
                    targetValue = targetIconOffsetY,
                    animationSpec = spring(
                        dampingRatio = 0.68f,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "IconOffsetY"
                )

                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.0f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = 0.68f,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "IconScale"
                )

                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) activeIconColor else unselectedIconColor,
                    animationSpec = tween(180),
                    label = "IconTint"
                )

                val labelAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = tween(140),
                    label = "LabelAlpha"
                )

                val labelOffsetY by animateFloatAsState(
                    targetValue = if (isSelected) 0f else 6f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "LabelOffset"
                )

                Box(
                    modifier = Modifier
                        .offset(x = tabLeftDp)
                        .width(tabSlotWidthDp)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onItemSelected(item)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = iconTint,
                        modifier = Modifier
                            .offset(y = iconOffsetY)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                            .size(25.dp)
                    )

                    if (labelAlpha > 0.01f) {
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 5.dp)
                                .graphicsLayer {
                                    alpha = labelAlpha
                                    translationY = labelOffsetY
                                }
                        )
                    }
                }
            }
        }
    }
}
