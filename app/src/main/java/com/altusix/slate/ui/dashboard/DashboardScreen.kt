package com.altusix.slate.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.altusix.slate.ui.components.BottomNavBar
import com.altusix.slate.ui.components.NavItem
import com.altusix.slate.ui.screens.WidgetListScreen

@Composable
fun DashboardScreen(
    onWidgetSelect: (com.altusix.slate.core.model.SlateWidgetInfo) -> Unit
) {
    var currentTab by remember { mutableStateOf(NavItem.WIDGETS) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Main screen content spans the full window height
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentTab) {
                NavItem.WIDGETS -> WidgetListScreen(onWidgetSelect = onWidgetSelect)
                NavItem.WALLPAPER -> PlaceholderScreen("Wallpaper Screen")
                NavItem.THEME -> PlaceholderScreen("Theme Screen")
                NavItem.SETTINGS -> PlaceholderScreen("Settings Screen")
            }
        }

        // 2. Navigation bar overlays on top at the bottom edge
        BottomNavBar(
            selectedItem = currentTab,
            onItemSelected = { currentTab = it },
            accentColor = Color(0xFF7C4DFF),
            barBackgroundColor = Color(0xFF161618),
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = title,
            color = Color.White
        )
    }
}