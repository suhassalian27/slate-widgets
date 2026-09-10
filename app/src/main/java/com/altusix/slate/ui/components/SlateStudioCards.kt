package com.altusix.slate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StudioSectionHeader(
    title: String,
    subtitle: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun StudioCounterCard(
    label: String,
    countText: String,
    accentColor: Color,
    onMinusClick: () -> Unit,
    onPlusClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF16161B))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(countText, color = Color.Gray, fontSize = 13.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onMinusClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C34)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) { Text("-", fontSize = 16.sp, color = Color.White) }

            Button(
                onClick = onPlusClick,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) { Text("+", fontSize = 16.sp, color = Color.Black) }
        }
    }
}