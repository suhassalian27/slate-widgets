package com.altusix.slate.widgets.applauncher

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.ui.config.SlateConfigTheme

class AppLauncherConfigActivity : ComponentActivity() {

    private data class InstalledAppItem(
        val label: String,
        val packageName: String
    )

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetClassName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val widgetInfo = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)
        widgetClassName = widgetInfo?.provider?.className ?: ""

        setContent {
            SlateConfigTheme {
                var config by remember { mutableStateOf(AppLauncherWidgetConfig.load(this, appWidgetId)) }
                var installedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
                var searchQuery by remember { mutableStateOf("") }
                var selectedIconTab by remember { mutableIntStateOf(0) }

                LaunchedEffect(Unit) {
                    val pm = packageManager
                    val mainIntent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
                    val resolvedActivities = pm.queryIntentActivities(mainIntent, 0)
                    installedApps = resolvedActivities.map {
                        InstalledAppItem(
                            label = it.loadLabel(pm).toString(),
                            packageName = it.activityInfo.packageName
                        )
                    }.sortedBy { it.label }
                }

                val emojis = listOf(
                    "😂", "❤️", "😍", "🤣", "😊",
                    "🙏", "😭", "🥰", "😘", "👍",
                    "💕", "😁", "🔥", "🥺", "😅",
                    "🤔", "😎", "😢", "👏", "🙌",
                    "✨", "🚀", "🎧", "🎮", "⚡"
                )

                val customIconNames = listOf(
                    "ic_sparkle", "ic_rocket", "ic_heart", "ic_star", "ic_flame",
                    "ic_chat", "ic_camera", "ic_music", "ic_setting", "ic_folder"
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0D0D0E))
                        .padding(20.dp)
                ) {
                    Text(
                        text = "App Launcher Setup",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // LIVE PREVIEW CARD
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF161618)),
                        contentAlignment = Alignment.Center
                    ) {
                        val previewBitmap = remember(config) {
                            val slateConfig = SlateWidgetConfig(
                                themeMode = config.themeMode,
                                backgroundColorHex = if (config.themeMode == "LIGHT") 0xFFFFFFFFL else 0xFF161618L,
                                opacity = config.opacity,
                                accentColorHex = config.accentColorHex
                            )
                            if (widgetClassName.contains("CustomText", ignoreCase = true)) {
                                generateTextLauncherBitmap(
                                    context = this@AppLauncherConfigActivity,
                                    slateConfig = slateConfig,
                                    launcherConfig = config,
                                    wDp = 200,
                                    hDp = 100
                                )
                            } else {
                                generateAdaptiveLauncherBitmap(
                                    context = this@AppLauncherConfigActivity,
                                    slateConfig = slateConfig,
                                    launcherConfig = config,
                                    wDp = 120,
                                    hDp = 120
                                )
                            }
                        }
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Preview",
                            modifier = Modifier.size(110.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // APP SELECTOR SEARCH
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Select App...", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color(0xFF2C2C30),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    val filteredApps = installedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
                    if (searchQuery.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.height(100.dp).fillMaxWidth().padding(top = 4.dp)) {
                            items(filteredApps) { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            config = config.copy(packageName = app.packageName, customText = app.label.take(4))
                                            searchQuery = ""
                                        }
                                        .padding(vertical = 6.dp, horizontal = 10.dp)
                                ) {
                                    Text(text = app.label, color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ICON TYPE TABS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF161618)),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("App Icon", "Emoji", "Icons", "Text").forEachIndexed { index, label ->
                            val isSelected = selectedIconTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF2C2C30) else Color.Transparent)
                                    .clickable {
                                        selectedIconTab = index
                                        config = when (index) {
                                            0 -> config.copy(iconType = LauncherIconType.APP_ICON)
                                            1 -> config.copy(iconType = LauncherIconType.EMOJI)
                                            2 -> config.copy(iconType = LauncherIconType.VECTOR_ICON)
                                            else -> config.copy(iconType = LauncherIconType.CUSTOM_TEXT)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = label, color = if (isSelected) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // DYNAMIC PICKER AREA
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedIconTab) {
                            1 -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(5),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(emojis) { emoji ->
                                        val isSelected = config.selectedEmoji == emoji
                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) Color(0xFF2C2C30) else Color(0xFF161618))
                                                .border(1.dp, if (isSelected) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
                                                .clickable { config = config.copy(selectedEmoji = emoji) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = emoji, fontSize = 20.sp)
                                        }
                                    }
                                }
                            }
                            2 -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(5),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(customIconNames) { iconName ->
                                        val isSelected = config.selectedVectorResName == iconName
                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) Color(0xFF2C2C30) else Color(0xFF161618))
                                                .border(1.dp, if (isSelected) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
                                                .clickable { config = config.copy(selectedVectorResName = iconName) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "★", color = Color.White, fontSize = 18.sp)
                                        }
                                    }
                                }
                            }
                            3 -> {
                                OutlinedTextField(
                                    value = config.customText,
                                    onValueChange = { config = config.copy(customText = it) },
                                    label = { Text("Badge Text", color = Color.Gray) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color(0xFF2C2C30),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }
                            else -> {
                                Text(text = "Using default icon of the selected app.", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { finish() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161618))
                        ) {
                            Text(text = "Cancel", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                AppLauncherWidgetConfig.save(this@AppLauncherConfigActivity, appWidgetId, config)
                                updateAllAppLauncherWidgets(this@AppLauncherConfigActivity)
                                setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                                finish()
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC00))
                        ) {
                            Text(text = "Save", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}