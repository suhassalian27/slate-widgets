package com.altusix.slate.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    content: @Composable ColumnScope.() -> Unit
) {
    val contentOnAccent = if (isLightColor(accentColor)) Color.Black else Color.White

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0C0C0E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 1. Elevated Navigation Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button + Header Hierarchy
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
                            contentDescription = "Cancel",
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
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF8E8E93),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Tactile Compound Action Button (Check + Text)
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

            HorizontalDivider(color = Color(0xFF18181E), thickness = 1.dp)

            // 2. Tab Navigation Rail
            if (tabs.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tabs) { tab ->
                        val isSelected = tab.key == selectedTabKey
                        val animatedBg by animateColorAsState(
                            targetValue = if (isSelected) accentColor else Color(0xFF16161B),
                            label = "tabBgAnim"
                        )
                        val animatedText by animateColorAsState(
                            targetValue = if (isSelected) contentOnAccent else Color(0xFF8E8E93),
                            label = "tabTextAnim"
                        )

                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(animatedBg)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else Color(0xFF24242C),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable { onTabSelected(tab.key) }
                                .padding(horizontal = 15.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.label,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = animatedText
                            )
                        }
                    }
                }
            }

            // 3. Main Form Body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                content()
            }
        }
    }
}