package com.altusix.slate.widgets.applauncher

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.ui.config.SlateConfigTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppLauncherConfigActivity : ComponentActivity() {

    data class InstalledAppItem(
        val label: String,
        val packageName: String,
        val icon: Drawable? = null
    )

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetClassName: String = ""

    // Helper function to return directly to Home Screen
    private fun navigateToHomeScreenAndFinish() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

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
                var showAppPickerSheet by remember { mutableStateOf(false) }
                var selectedIconTab by remember { mutableIntStateOf(0) }

                // Lock initial responsive mode to this specific widget ID
                LaunchedEffect(appWidgetId) {
                    val prefs = getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
                    val prefix = "launcher_${appWidgetId}_"
                    if (!prefs.contains("${prefix}is_responsive")) {
                        val defaultResponsive = prefs.getBoolean("default_is_responsive", true)
                        config = config.copy(isResponsive = defaultResponsive)
                        prefs.edit().putBoolean("${prefix}is_responsive", defaultResponsive).apply()
                    }

                    withContext(Dispatchers.IO) {
                        val pm = packageManager
                        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
                        val resolved = pm.queryIntentActivities(mainIntent, 0)
                        val apps = resolved.map {
                            InstalledAppItem(
                                label = it.loadLabel(pm).toString(),
                                packageName = it.activityInfo.packageName,
                                icon = try { it.loadIcon(pm) } catch (e: Exception) { null }
                            )
                        }.sortedBy { it.label }
                        withContext(Dispatchers.Main) {
                            installedApps = apps
                        }
                    }
                }

                val selectedApp = installedApps.find { it.packageName == config.packageName }

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
                            .height(180.dp)
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
                            if (widgetClassName.contains("CustomText", ignoreCase = true) || widgetClassName.contains("Rectangle", ignoreCase = true)) {
                                generateRectangleLauncherBitmap(
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
                            modifier = Modifier.size(120.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // APP SELECTOR CARD
                    Text(text = "Target Application", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF161618))
                            .border(1.dp, Color(0xFF242428), RoundedCornerShape(16.dp))
                            .clickable { showAppPickerSheet = true }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectedApp?.icon != null) {
                                    Image(
                                        bitmap = selectedApp.icon.toImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2C2C30)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                }

                                Text(
                                    text = selectedApp?.label ?: "Select Application...",
                                    color = if (selectedApp != null) Color.White else Color.Gray,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ICON TYPE TABS
                    Text(text = "Icon Style", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF161618))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("App Icon", "Emoji", "Icons", "Text").forEachIndexed { index, label ->
                            val isSelected = selectedIconTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(11.dp))
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
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // DYNAMIC PICKER AREA
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (selectedIconTab) {
                            1 -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(5),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(emojis) { emoji ->
                                        val isSelected = config.selectedEmoji == emoji
                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isSelected) Color(0xFF2C2C30) else Color(0xFF161618))
                                                .border(1.dp, if (isSelected) Color.White else Color(0xFF242428), RoundedCornerShape(14.dp))
                                                .clickable { config = config.copy(selectedEmoji = emoji) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = emoji, fontSize = 22.sp)
                                        }
                                    }
                                }
                            }
                            2 -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(5),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(customIconNames) { iconName ->
                                        val isSelected = config.selectedVectorResName == iconName
                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isSelected) Color(0xFF2C2C30) else Color(0xFF161618))
                                                .border(1.dp, if (isSelected) Color.White else Color(0xFF242428), RoundedCornerShape(14.dp))
                                                .clickable { config = config.copy(selectedVectorResName = iconName) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "★", color = Color.White, fontSize = 20.sp)
                                        }
                                    }
                                }
                            }
                            3 -> {
                                OutlinedTextField(
                                    value = config.customText,
                                    onValueChange = { config = config.copy(customText = it) },
                                    placeholder = { Text("Badge Text (Max 4 chars)", color = Color.Gray) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color(0xFF242428),
                                        focusedContainerColor = Color(0xFF161618),
                                        unfocusedContainerColor = Color(0xFF161618),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Displays the standard icon of the target application.",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                navigateToHomeScreenAndFinish()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161618))
                        ) {
                            Text(text = "Cancel", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = {
                                AppLauncherWidgetConfig.save(this@AppLauncherConfigActivity, appWidgetId, config)
                                updateAllAppLauncherWidgets(this@AppLauncherConfigActivity)
                                setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                                navigateToHomeScreenAndFinish()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text(text = "Save Widget", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (showAppPickerSheet) {
                    AppPickerBottomSheet(
                        installedApps = installedApps,
                        selectedPackageName = config.packageName,
                        onDismiss = { showAppPickerSheet = false },
                        onAppSelected = { app ->
                            config = config.copy(
                                packageName = app.packageName,
                                customText = app.label.take(4)
                            )
                            showAppPickerSheet = false
                        }
                    )
                }
            }
        }
    }
}

private fun Drawable.toImageBitmap(): ImageBitmap {
    val width = intrinsicWidth.coerceAtLeast(1)
    val height = intrinsicHeight.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap.asImageBitmap()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerBottomSheet(
    installedApps: List<AppLauncherConfigActivity.InstalledAppItem>,
    selectedPackageName: String,
    onDismiss: () -> Unit,
    onAppSelected: (AppLauncherConfigActivity.InstalledAppItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161618),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Select Application", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search apps...", color = Color.Gray, fontSize = 13.sp) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color(0xFF242428),
                    focusedContainerColor = Color(0xFF0D0D0E),
                    unfocusedContainerColor = Color(0xFF0D0D0E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredApps) { app ->
                    val isSelected = app.packageName == selectedPackageName
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF2C2C30) else Color.Transparent)
                            .clickable { onAppSelected(app) }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (app.icon != null) {
                            Image(
                                bitmap = app.icon.toImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2C2C30))
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = app.label,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.Green,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}