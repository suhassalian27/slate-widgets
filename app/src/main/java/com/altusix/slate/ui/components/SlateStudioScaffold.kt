package com.altusix.slate.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class StudioTabItem(
    val key: String,
    val label: String
)

@Composable
fun SlateStudioScaffold(
    title: String,
    accentColor: Color,
    tabs: List<StudioTabItem>,
    selectedTabKey: String,
    onTabSelected: (String) -> Unit,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A0A0C)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 1. Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                IconButton(
                    onClick = onSaveClick,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        tint = Color.Black
                    )
                }
            }

            // 2. Tab Navigation
            if (tabs.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tabs) { tab ->
                        val isSelected = tab.key == selectedTabKey
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) accentColor else Color(0xFF1E1E24))
                                .clickable { onTabSelected(tab.key) }
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = tab.label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else Color(0xFFB0B0B8)
                            )
                        }
                    }
                }
            }

            // 3. Scrollable Content Body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                content()
            }
        }
    }
}