package com.altusix.slate.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.hypot

data class ConfigTabItem(
    val key: String,
    val label: String
)

private fun isLightColor(color: Color): Boolean {
    val argb = color.toArgb()
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    return (0.2126f * r + 0.7152f * g + 0.0722f * b) > 0.55f
}

@Composable
fun SlateConfigScaffold(
    title: String,
    subtitle: String? = null,
    accentColor: Color,
    tabs: List<ConfigTabItem> = emptyList(),
    selectedTabKey: String = "",
    onTabSelected: (String) -> Unit = {},
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    scrollable: Boolean = true,
    previewHeight: Dp = 195.dp,
    previewContent: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val contentOnAccent = if (isLightColor(accentColor)) Color.Black else Color.White

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A0A0C)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
        ) {
            // 1. Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16161B))
                            .border(1.dp, Color(0xFF26262E), CircleShape)
                            .clickable { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFFE4E4E8),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                fontSize = 11.sp,
                                color = Color(0xFF8E8E93),
                                maxLines = 1
                            )
                        }
                    }
                }

                Button(
                    onClick = onSaveClick,
                    shape = RoundedCornerShape(19.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = contentOnAccent
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = contentOnAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Save",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF16161C), thickness = 1.dp)

            // 2. High-Contrast Studio Showcase Viewport
            if (previewContent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(previewHeight)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF282A36), // Balanced center stage light
                                    Color(0xFF171820),
                                    Color(0xFF0E0F14),
                                    Color(0xFF090A0D)
                                ),
                                radius = 620f
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x35FFFFFF),
                                    Color(0x12FFFFFF),
                                    Color(0x05FFFFFF)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Dot Grid Background
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val spacing = 22.dp.toPx()
                        val dotRadius = 1.0.dp.toPx()
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val maxDist = hypot(cx, cy)

                        val cols = (size.width / spacing).toInt() + 1
                        val rows = (size.height / spacing).toInt() + 1

                        for (i in 0 until cols) {
                            for (j in 0 until rows) {
                                val px = i * spacing
                                val py = j * spacing
                                val dist = hypot(px - cx, py - cy)
                                val alpha = (1f - (dist / maxDist)).coerceIn(0.06f, 0.26f)

                                drawCircle(
                                    color = Color.White.copy(alpha = alpha),
                                    radius = dotRadius,
                                    center = Offset(px, py)
                                )
                            }
                        }
                    }

                    // Content renders cleanly without an artificial rectangular shadow
                    previewContent()
                }
            }

            // 3. Segmented Control Track
            if (tabs.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF141418))
                        .border(1.dp, Color(0xFF22222A), RoundedCornerShape(14.dp))
                        .padding(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tabs.forEach { tab ->
                            val isSelected = tab.key == selectedTabKey
                            val animatedBg by animateColorAsState(
                                targetValue = if (isSelected) accentColor else Color.Transparent,
                                label = "segmentBg"
                            )
                            val animatedText by animateColorAsState(
                                targetValue = if (isSelected) contentOnAccent else Color(0xFF8E8E93),
                                label = "segmentText"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(animatedBg)
                                    .clickable { onTabSelected(tab.key) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = animatedText
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. Form Controls Section
            if (scrollable) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    content()
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    content()
                }
            }
        }
    }
}