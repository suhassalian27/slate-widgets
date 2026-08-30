package com.altusix.slate.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

enum class NavItem(
    val title: String,
    val icon: ImageVector
) {
    WIDGETS("Widget", Icons.Outlined.GridView),
    WALLPAPER("Wallpaper", Icons.Outlined.Image),
    THEME("Theme", Icons.Outlined.ColorLens),
    SETTINGS("Setting", Icons.Outlined.Settings)
}

@Composable
fun BottomNavBar(
    selectedItem: NavItem,
    onItemSelected: (NavItem) -> Unit,
    modifier: Modifier = Modifier,
    barBackgroundColor: Color = Color(0xFF7B7BB7),
    accentColor: Color = Color(0xFF7C4DFF),
    unselectedIconColor: Color = Color(0xFF757575)
) {
    val items = remember { NavItem.entries.toTypedArray() }
    val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)

    val density = LocalDensity.current

    val topHeadroomDp = 34.dp
    val barHeightDp = 68.dp
    val totalHeightDp = topHeadroomDp + barHeightDp

    val circleSizeDp = 50.dp
    val curveRadiusDp = 66.dp
    val curveDepthDp = 40.dp

    // Added side padding to center the menu items with extra space on left/right
    val sidePaddingDp = 28.dp

    val curveRadiusPx = with(density) { curveRadiusDp.toPx() }
    val curveDepthPx = with(density) { curveDepthDp.toPx() }
    val sidePaddingPx = with(density) { sidePaddingDp.toPx() }

    val popScale = remember { Animatable(1f) }
    val popOffsetY = remember { Animatable(0f) }

    LaunchedEffect(selectedIndex) {
        launch {
            popScale.animateTo(1.20f, tween(120, easing = FastOutSlowInEasing))
            popScale.animateTo(
                1.0f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }
        launch {
            popOffsetY.animateTo(-12f, tween(120, easing = FastOutSlowInEasing))
            popOffsetY.animateTo(
                0f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeightDp)
    ) {
        val totalWidthPx = with(density) { maxWidth.toPx() }
        val usableWidthPx = (totalWidthPx - (sidePaddingPx * 2f)).coerceAtLeast(1f)

        // Calculate exact center position factoring in side padding
        val targetCenterXFraction = if (totalWidthPx > 0f) {
            (sidePaddingPx + (selectedIndex + 0.5f) * (usableWidthPx / items.size)) / totalWidthPx
        } else {
            (selectedIndex + 0.5f) * (1f / items.size)
        }

        val animatedCenterXFraction by animateFloatAsState(
            targetValue = targetCenterXFraction,
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = Spring.StiffnessLow
            ),
            label = "BottomNavCurveAnimation"
        )

        // 1. Curved Background Path Canvas starting from Y = 0 (No dead black gap above)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeightDp)
        ) {
            val w = size.width
            val h = size.height
            val centerX = animatedCenterXFraction * w

            val path = Path().apply {
                moveTo(0f, 0f)

                val curveStartX = (centerX - curveRadiusPx).coerceAtLeast(0f)
                lineTo(curveStartX, 0f)

                // Wide U-Shaped Bezier Cradle
                cubicTo(
                    x1 = centerX - (curveRadiusPx * 0.48f),
                    y1 = 0f,
                    x2 = centerX - (curveRadiusPx * 0.44f),
                    y2 = curveDepthPx,
                    x3 = centerX,
                    y3 = curveDepthPx
                )
                cubicTo(
                    x1 = centerX + (curveRadiusPx * 0.44f),
                    y1 = curveDepthPx,
                    x2 = centerX + (curveRadiusPx * 0.48f),
                    y2 = 0f,
                    x3 = (centerX + curveRadiusPx).coerceAtMost(w),
                    y3 = 0f
                )

                lineTo(w, 0f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }

            drawPath(path = path, color = barBackgroundColor)
        }

        // 2. Centered Navigation Items Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeightDp)
                .padding(horizontal = sidePaddingDp)
                .align(Alignment.BottomCenter)
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex

                val labelAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = tween(160),
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

                val iconAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 0f else 1f,
                    animationSpec = tween(140),
                    label = "IconAlpha"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
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
                        tint = unselectedIconColor,
                        modifier = Modifier
                            .size(24.dp)
                            .alpha(iconAlpha)
                    )

                    if (labelAlpha > 0.01f) {
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp)
                                .graphicsLayer {
                                    alpha = labelAlpha
                                    translationY = labelOffsetY
                                }
                        )
                    }
                }
            }
        }

        // 3. Floating Cradled Accent Button (Protrudes smoothly above the top edge)
        FloatingSelectedButton(
            item = items[selectedIndex],
            accentColor = accentColor,
            circleSizeDp = circleSizeDp,
            animatedFraction = animatedCenterXFraction,
            popScale = popScale.value,
            popOffsetY = popOffsetY.value,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}

@Composable
private fun FloatingSelectedButton(
    item: NavItem,
    accentColor: Color,
    circleSizeDp: Dp,
    animatedFraction: Float,
    popScale: Float,
    popOffsetY: Float,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(circleSizeDp)
    ) {
        val maxW = maxWidth
        val circleOffsetDp = (maxW * animatedFraction) - (circleSizeDp / 2f)

        Box(
            modifier = Modifier
                .offset(x = circleOffsetDp, y = (-14).dp)
                .graphicsLayer {
                    translationY = popOffsetY
                    scaleX = popScale
                    scaleY = popScale
                }
                .size(circleSizeDp)
                .shadow(elevation = 12.dp, shape = CircleShape, spotColor = accentColor)
                .clip(CircleShape)
                .background(accentColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}