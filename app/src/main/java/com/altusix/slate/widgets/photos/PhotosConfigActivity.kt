package com.altusix.slate.widgets.photos

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altusix.slate.core.theme.ThemePreferences
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.ui.components.CustomColorPickerDialog
import com.altusix.slate.ui.components.RainbowCustomCircle
import com.altusix.slate.widgets.camera.parseAndLockIsResponsive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.hypot

private enum class ConfigColorTarget {
    BACKGROUND, ACCENT, CAPTION
}

private fun calculateLuminance(hex: Long): Float {
    val r = ((hex shr 16) and 0xFFL) / 255f
    val g = ((hex shr 8) and 0xFFL) / 255f
    val b = (hex and 0xFFL) / 255f
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

class PhotosConfigActivity : ComponentActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        }

        val widgetInfo = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)
        } else null
        val providerClassName = widgetInfo?.provider?.className ?: ""

        val isFixedThreeSlot = providerClassName.contains("CollageBento") || providerClassName.contains("FilmStrip")
        val isShapeWidget = providerClassName.let {
            it.contains("Square") || it.contains("Rectangle") || it.contains("Circle") ||
                    it.contains("Heart") || it.contains("Star") || it.contains("Flower") ||
                    it.contains("Clover") || it.contains("Blob")
        }

        val initialConfig = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val loaded = PhotosStorageManager.getConfig(this, widgetId)
            if (!isFixedThreeSlot) {
                // Filter out empty placeholder slots so the reel only contains real user photos
                val realItems = loaded.items.filter { !it.imagePath.isNullOrBlank() && File(it.imagePath).exists() }
                loaded.copy(
                    items = realItems,
                    currentIndex = 0.coerceAtMost(maxOf(0, realItems.size - 1)),
                    showCaption = if (isShapeWidget && realItems.isEmpty()) false else loaded.showCaption
                )
            } else {
                val list = loaded.items.toMutableList()
                while (list.size < 3) list.add(SlateMemoryItem(id = System.currentTimeMillis().toString()))
                loaded.copy(items = list.take(3))
            }
        } else {
            PhotosWidgetConfig.getDefaultConfig().copy(
                items = emptyList(),
                currentIndex = 0,
                showCaption = !isShapeWidget
            )
        }

        val initialSlateConfig = loadSlateWidgetConfig(this, widgetId)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0C0C0E),
                    surface = Color(0xFF16161B)
                )
            ) {
                var config by remember { mutableStateOf(initialConfig) }
                var selectedIndex by remember {
                    mutableStateOf(if (config.items.isEmpty()) 0 else config.currentIndex.coerceIn(0, config.items.size - 1))
                }

                // Active Tab: 0 = Photos & Memory, 1 = Widget Theme
                var selectedTab by remember { mutableIntStateOf(0) }

                // Theme States
                var selectedBgHex by remember { mutableLongStateOf(initialSlateConfig.backgroundColorHex) }
                var selectedAccentHex by remember { mutableLongStateOf(initialSlateConfig.accentColorHex) }
                var opacity by remember { mutableFloatStateOf(initialSlateConfig.opacity) }
                var isResponsive by remember {
                    mutableStateOf(if (widgetId == -1) false else parseAndLockIsResponsive(this@PhotosConfigActivity, widgetId))
                }

                val currentSlateConfig = remember(selectedBgHex, selectedAccentHex, opacity) {
                    SlateWidgetConfig(
                        themeMode = if (calculateLuminance(selectedBgHex) > 0.5f) "LIGHT" else "DARK",
                        backgroundColorHex = selectedBgHex,
                        opacity = opacity,
                        accentColorHex = selectedAccentHex
                    )
                }

                val widgetProviderClass = remember(widgetId) {
                    if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        val info = AppWidgetManager.getInstance(this@PhotosConfigActivity).getAppWidgetInfo(widgetId)
                        info?.provider?.className ?: ""
                    } else ""
                }

                val isFixedThreeSlotWidget = remember(widgetProviderClass) {
                    widgetProviderClass.contains("CollageBento") || widgetProviderClass.contains("FilmStrip")
                }

                val supportsCaption = remember(widgetProviderClass, selectedIndex) {
                    when {
                        widgetProviderClass.contains("FilmStrip") -> false
                        widgetProviderClass.contains("Locket") -> false
                        widgetProviderClass.contains("CollageBento") -> selectedIndex == 0
                        else -> true
                    }
                }

                val supportsDateStamp = remember(widgetProviderClass) {
                    widgetProviderClass.contains("Polaroid") || widgetProviderClass.contains("OnThisDay")
                }

                val supportsLocation = remember(widgetProviderClass) {
                    widgetProviderClass.contains("OnThisDay")
                }

                val activeItem = config.items.getOrNull(selectedIndex) ?: SlateMemoryItem()
                var caption by remember(activeItem.id) { mutableStateOf(activeItem.caption) }
                var dateText by remember(activeItem.id) { mutableStateOf(activeItem.dateText) }
                var location by remember(activeItem.id) { mutableStateOf(activeItem.location) }
                var filterStyle by remember(activeItem.id) { mutableStateOf(activeItem.filterStyle) }
                var captionFont by remember(activeItem.id) { mutableStateOf(activeItem.captionFont) }
                var captionColorHex by remember(activeItem.id) { mutableLongStateOf(activeItem.captionColorHex) }
                var currentImagePath by remember(activeItem.id) { mutableStateOf(activeItem.imagePath) }

                var rawPickedUri by remember { mutableStateOf<Uri?>(null) }
                var isPickingForNewSlot by remember { mutableStateOf(false) }
                var activeColorTarget by remember { mutableStateOf<ConfigColorTarget?>(null) }

                val photoPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri: Uri? ->
                    uri?.let { rawPickedUri = it }
                }

                fun syncActiveItem(
                    newPath: String? = currentImagePath,
                    newCap: String = caption,
                    newDate: String = dateText,
                    newLoc: String = location,
                    newFilter: MemoryFilterStyle = filterStyle,
                    newFont: CaptionFont = captionFont,
                    newColorHex: Long = captionColorHex
                ) {
                    val updatedItem = activeItem.copy(
                        imagePath = newPath,
                        caption = newCap,
                        dateText = newDate,
                        location = newLoc,
                        filterStyle = newFilter,
                        captionFont = newFont,
                        captionColorHex = newColorHex
                    )
                    val newList = config.items.toMutableList()
                    if (selectedIndex in newList.indices) {
                        newList[selectedIndex] = updatedItem
                        config = config.copy(
                            items = newList,
                            currentIndex = selectedIndex.coerceIn(0, maxOf(newList.size - 1, 0))
                        )
                    }
                }

                fun saveAndFinish() {
                    syncActiveItem()

                    if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        // 1. Save Photo Config
                        PhotosStorageManager.saveConfig(this@PhotosConfigActivity, widgetId, config)

                        // 2. Schedule or Cancel Auto Rotation
                        val providerInfo = AppWidgetManager.getInstance(this@PhotosConfigActivity).getAppWidgetInfo(widgetId)
                        providerInfo?.provider?.className?.let { className ->
                            try {
                                val clazz = Class.forName(className)
                                PhotosAlarmScheduler.scheduleRotation(
                                    context = this@PhotosConfigActivity,
                                    receiverClass = clazz,
                                    widgetId = widgetId,
                                    intervalMinutes = if (config.items.size > 1) config.rotationIntervalMinutes else 0
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        // 3. Save Theme Studio Attributes
                        val widgetPrefs = getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
                        widgetPrefs.edit()
                            .putString("widget_${widgetId}_theme_mode", currentSlateConfig.themeMode)
                            .putLong("widget_${widgetId}_bg_color", selectedBgHex)
                            .putFloat("widget_${widgetId}_opacity", opacity)
                            .putLong("widget_${widgetId}_accent_color", selectedAccentHex)
                            .putBoolean("widget_${widgetId}_is_responsive", isResponsive)
                            .putBoolean("widget_${widgetId}_has_custom_theme", true)
                            .apply()

                        updateAllPhotosWidgets(this@PhotosConfigActivity)

                        val resultIntent = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                    } else {
                        setResult(Activity.RESULT_OK)
                    }
                    finish()
                }

                if (rawPickedUri != null) {
                    SlateProEditorOverlay(
                        rawUri = rawPickedUri!!,
                        onDismiss = {
                            rawPickedUri = null
                            isPickingForNewSlot = false
                        },
                        onImageTransformed = { transformedUriStr ->
                            val transformedUri = Uri.parse(transformedUriStr)
                            val savedPath = PhotosStorageManager.copyUriToInternalStorage(
                                context = this@PhotosConfigActivity,
                                widgetId = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) widgetId else 9999,
                                index = System.currentTimeMillis().toInt(),
                                uri = transformedUri
                            )
                            if (savedPath != null) {
                                if (isPickingForNewSlot || config.items.isEmpty()) {
                                    val newItem = SlateMemoryItem(
                                        id = System.currentTimeMillis().toString(),
                                        imagePath = savedPath,
                                        caption = caption.ifBlank { "Memory ${config.items.size + 1}" },
                                        dateText = dateText.ifBlank { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()) },
                                        location = location,
                                        filterStyle = filterStyle,
                                        captionFont = captionFont,
                                        captionColorHex = captionColorHex
                                    )
                                    val updatedList = config.items + newItem
                                    val newIdx = updatedList.size - 1
                                    config = config.copy(items = updatedList, currentIndex = newIdx)
                                    selectedIndex = newIdx
                                    currentImagePath = savedPath
                                } else {
                                    currentImagePath = savedPath
                                    syncActiveItem(newPath = savedPath)
                                }
                            }
                            isPickingForNewSlot = false
                            rawPickedUri = null
                        }
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding(),
                        color = Color(0xFF0C0C0E)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                        ) {
                            // Top Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    IconButton(
                                        onClick = { finish() },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1C1C22))
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Cancel",
                                            tint = Color(0xFFD1D1D6),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "Photos & Memories",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }

                                Button(
                                    onClick = { saveAndFinish() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(selectedAccentHex),
                                        contentColor = if (calculateLuminance(selectedAccentHex) > 0.5f) Color.Black else Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Save", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            HorizontalDivider(color = Color(0xFF1E1E24), thickness = 1.dp)

                            // Sticky Live Widget Preview
                            val context = LocalContext.current
                            val previewData by produceState(
                                initialValue = Pair<Bitmap?, Float>(null, 1.0f),
                                config,
                                selectedIndex,
                                currentSlateConfig
                            ) {
                                value = withContext(Dispatchers.Default) {
                                    renderExactPhotoWidgetPreview(context, widgetId, config, currentSlateConfig)
                                }
                            }

                            val previewBitmap = previewData.first
                            val targetAspect = previewData.second

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF141418))
                                    .border(1.dp, Color(0xFF24242C), RoundedCornerShape(20.dp))
                                    .clickable {
                                        if (currentImagePath != null) {
                                            val f = File(currentImagePath!!)
                                            if (f.exists()) rawPickedUri = Uri.fromFile(f)
                                        } else {
                                            isPickingForNewSlot = config.items.isEmpty()
                                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (previewBitmap != null) {
                                    Image(
                                        bitmap = previewBitmap!!.asImageBitmap(),
                                        contentDescription = "Exact Widget Preview",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .padding(10.dp)
                                            .aspectRatio(targetAspect)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (currentImagePath != null) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color.Black.copy(alpha = 0.72f))
                                                    .clickable {
                                                        val f = File(currentImagePath!!)
                                                        if (f.exists()) rawPickedUri = Uri.fromFile(f)
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Text("Edit Crop", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color.Black.copy(alpha = 0.72f))
                                                .clickable {
                                                    isPickingForNewSlot = config.items.isEmpty()
                                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                                }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                if (currentImagePath == null) "Choose Photo" else "Change Photo",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }

                            // 2-Tab Segmented Selector
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF141418))
                                    .padding(3.dp)
                            ) {
                                listOf("Photos & Memory", "Widget Theme").forEachIndexed { idx, title ->
                                    val isTabSelected = selectedTab == idx
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(if (isTabSelected) Color(0xFF26262E) else Color.Transparent)
                                            .clickable { selectedTab = idx },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = title,
                                            color = if (isTabSelected) Color.White else Color(0xFF8E8E93),
                                            fontSize = 13.sp,
                                            fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFF1A1A22), thickness = 1.dp, modifier = Modifier.padding(top = 4.dp))

                            // Scrollable Body
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                if (selectedTab == 0) {

                                    // A. FIXED 3-SLOT TILES (Bento & FilmStrip)
                                    if (isFixedThreeSlotWidget) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "WIDGET TILES (3)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF8E8E93),
                                                letterSpacing = 0.5.sp
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                (0 until 3).forEach { idx ->
                                                    val item = config.items.getOrNull(idx)
                                                    val isSelected = idx == selectedIndex
                                                    val itemBmp = remember(item?.imagePath) {
                                                        item?.imagePath?.let {
                                                            val f = File(it)
                                                            if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
                                                        }
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(84.dp)
                                                            .clip(RoundedCornerShape(14.dp))
                                                            .background(Color(0xFF16161B))
                                                            .border(
                                                                width = if (isSelected) 2.dp else 1.dp,
                                                                color = if (isSelected) Color(selectedAccentHex) else Color(0xFF282830),
                                                                shape = RoundedCornerShape(14.dp)
                                                            )
                                                            .clickable {
                                                                syncActiveItem()
                                                                selectedIndex = idx
                                                                if (item?.imagePath == null) {
                                                                    isPickingForNewSlot = false
                                                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                                                }
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (itemBmp != null) {
                                                            Image(
                                                                bitmap = itemBmp.asImageBitmap(),
                                                                contentDescription = null,
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        } else {
                                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Add,
                                                                    contentDescription = null,
                                                                    tint = Color(selectedAccentHex),
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                                Spacer(modifier = Modifier.height(4.dp))
                                                                Text("Tile ${idx + 1}", fontSize = 11.sp, color = Color(0xFF8E8E93))
                                                            }
                                                        }

                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.BottomStart)
                                                                .padding(6.dp)
                                                                .clip(RoundedCornerShape(5.dp))
                                                                .background(Color.Black.copy(alpha = 0.75f))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("#${idx + 1}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        // B. SLIDESHOW REEL (Single / Slideshow Widgets)
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(selectedAccentHex))
                                                    )
                                                    Text(
                                                        text = "SLIDESHOW REEL (${config.items.size})",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF8E8E93),
                                                        letterSpacing = 0.5.sp
                                                    )
                                                }

                                                if (config.items.isNotEmpty()) {
                                                    Text(
                                                        text = "Clear All",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFFFF453A),
                                                        modifier = Modifier.clickable {
                                                            config = config.copy(
                                                                items = emptyList(),
                                                                currentIndex = 0,
                                                                rotationIntervalMinutes = 0
                                                            )
                                                            selectedIndex = 0
                                                            currentImagePath = null
                                                        }
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                // 1. "+ Add Photos" Card
                                                if (config.items.size < 6) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(76.dp)
                                                            .height(86.dp)
                                                            .clip(RoundedCornerShape(16.dp))
                                                            .background(Color(0xFF14161C))
                                                            .border(
                                                                width = 1.5.dp,
                                                                color = Color(selectedAccentHex).copy(alpha = 0.55f),
                                                                shape = RoundedCornerShape(16.dp)
                                                            )
                                                            .clickable {
                                                                isPickingForNewSlot = true
                                                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Add,
                                                                contentDescription = "Add Photo",
                                                                tint = Color(selectedAccentHex),
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = "Add Photos",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(selectedAccentHex)
                                                            )
                                                        }
                                                    }
                                                }

                                                // 2. Real Photo Thumbnails
                                                config.items.forEachIndexed { idx, item ->
                                                    val isSelected = idx == selectedIndex
                                                    val itemBmp = remember(item.imagePath) {
                                                        item.imagePath?.let {
                                                            val f = File(it)
                                                            if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
                                                        }
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .width(76.dp)
                                                            .height(86.dp)
                                                            .clip(RoundedCornerShape(16.dp))
                                                            .background(Color(0xFF1A1A22))
                                                            .border(
                                                                width = if (isSelected) 2.dp else 1.dp,
                                                                color = if (isSelected) Color(selectedAccentHex) else Color(0xFF282830),
                                                                shape = RoundedCornerShape(16.dp)
                                                            )
                                                            .clickable {
                                                                syncActiveItem()
                                                                selectedIndex = idx
                                                                config = config.copy(currentIndex = idx)
                                                                currentImagePath = item.imagePath
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (itemBmp != null) {
                                                            Image(
                                                                bitmap = itemBmp.asImageBitmap(),
                                                                contentDescription = null,
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        } else {
                                                            Text(
                                                                text = "#${idx + 1}",
                                                                color = if (isSelected) Color(selectedAccentHex) else Color(0xFF8E8E93),
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 14.sp
                                                            )
                                                        }

                                                        // Bottom-Left Index Badge
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.BottomStart)
                                                                .padding(6.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(Color.Black.copy(alpha = 0.72f))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "#${idx + 1}",
                                                                color = Color.White,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }

                                                        // Top-Right Close Button
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .padding(4.dp)
                                                                .size(22.dp)
                                                                .clip(CircleShape)
                                                                .background(Color.Black.copy(alpha = 0.75f))
                                                                .clickable {
                                                                    val curList = config.items.toMutableList()
                                                                    curList.removeAt(idx)
                                                                    val nextIdx = 0.coerceAtMost(maxOf(0, curList.size - 1))
                                                                    config = config.copy(items = curList, currentIndex = nextIdx)
                                                                    selectedIndex = nextIdx
                                                                    currentImagePath = curList.getOrNull(nextIdx)?.imagePath
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Remove",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // C. ROTATION INTERVAL (Only shown when 2+ photos exist)
                                        if (config.items.size > 1) {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "ROTATION INTERVAL",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF8E8E93),
                                                        letterSpacing = 0.5.sp
                                                    )
                                                    Text(
                                                        text = if (config.rotationIntervalMinutes == 0) "Manual" else "Every ${config.rotationIntervalMinutes}m",
                                                        fontSize = 12.sp,
                                                        color = Color(selectedAccentHex),
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }

                                                val intervals = listOf(
                                                    0 to "Manual",
                                                    15 to "15m",
                                                    30 to "30m",
                                                    60 to "1h",
                                                    180 to "3h"
                                                )

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color(0xFF141416))
                                                        .padding(3.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    intervals.forEach { (mins, label) ->
                                                        val isSelected = config.rotationIntervalMinutes == mins
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(36.dp)
                                                                .clip(RoundedCornerShape(9.dp))
                                                                .background(if (isSelected) Color(0xFF26262E) else Color.Transparent)
                                                                .clickable { config = config.copy(rotationIntervalMinutes = mins) },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = label,
                                                                color = if (isSelected) Color.White else Color(0xFF8E8E93),
                                                                fontSize = 12.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Filter Selection
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "AESTHETIC FILTER",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF8E8E93),
                                            letterSpacing = 0.5.sp
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            MemoryFilterStyle.values().forEach { style ->
                                                val isChosen = filterStyle == style
                                                FilterChip(
                                                    selected = isChosen,
                                                    onClick = {
                                                        filterStyle = style
                                                        syncActiveItem(newFilter = style)
                                                    },
                                                    label = {
                                                        Text(
                                                            text = style.label,
                                                            fontSize = 12.sp,
                                                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = Color(selectedAccentHex),
                                                        selectedLabelColor = if (calculateLuminance(selectedAccentHex) > 0.5f) Color.Black else Color.White,
                                                        containerColor = Color(0xFF1C1C22),
                                                        labelColor = Color(0xFFD1D1D6)
                                                    ),
                                                    border = FilterChipDefaults.filterChipBorder(
                                                        borderColor = if (isChosen) Color(selectedAccentHex) else Color(0xFF282830),
                                                        enabled = true,
                                                        selected = isChosen
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // Caption & Overlay
                                    if (supportsCaption) {
                                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "CAPTION & OVERLAY",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF8E8E93),
                                                    letterSpacing = 0.5.sp
                                                )

                                                Switch(
                                                    checked = config.showCaption,
                                                    onCheckedChange = { config = config.copy(showCaption = it) },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.Black,
                                                        checkedTrackColor = Color(selectedAccentHex),
                                                        uncheckedThumbColor = Color(0xFF8E8E93),
                                                        uncheckedTrackColor = Color(0xFF1C1C22)
                                                    )
                                                )
                                            }

                                            AnimatedVisibility(visible = config.showCaption) {
                                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                                    OutlinedTextField(
                                                        value = caption,
                                                        onValueChange = {
                                                            caption = it
                                                            syncActiveItem(newCap = it)
                                                        },
                                                        label = { Text("Caption / Title") },
                                                        placeholder = { Text("e.g. Featured Memory") },
                                                        singleLine = true,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = Color(selectedAccentHex),
                                                            unfocusedBorderColor = Color(0xFF2C2C35),
                                                            focusedLabelColor = Color(selectedAccentHex),
                                                            unfocusedLabelColor = Color(0xFF8E8E93),
                                                            focusedTextColor = Color.White,
                                                            unfocusedTextColor = Color.White,
                                                            focusedContainerColor = Color(0xFF16161B),
                                                            unfocusedContainerColor = Color(0xFF16161B)
                                                        )
                                                    )

                                                    if (supportsDateStamp) {
                                                        OutlinedTextField(
                                                            value = dateText,
                                                            onValueChange = {
                                                                dateText = it
                                                                syncActiveItem(newDate = it)
                                                            },
                                                            label = { Text("Date Stamp") },
                                                            placeholder = { Text("e.g. September 2024") },
                                                            singleLine = true,
                                                            modifier = Modifier.fillMaxWidth(),
                                                            shape = RoundedCornerShape(12.dp),
                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                focusedBorderColor = Color(selectedAccentHex),
                                                                unfocusedBorderColor = Color(0xFF2C2C35),
                                                                focusedLabelColor = Color(selectedAccentHex),
                                                                unfocusedLabelColor = Color(0xFF8E8E93),
                                                                focusedTextColor = Color.White,
                                                                unfocusedTextColor = Color.White,
                                                                focusedContainerColor = Color(0xFF16161B),
                                                                unfocusedContainerColor = Color(0xFF16161B)
                                                            )
                                                        )
                                                    }

                                                    if (supportsLocation) {
                                                        OutlinedTextField(
                                                            value = location,
                                                            onValueChange = {
                                                                location = it
                                                                syncActiveItem(newLoc = it)
                                                            },
                                                            label = { Text("Location (Optional)") },
                                                            placeholder = { Text("e.g. Pacific Coast, California") },
                                                            singleLine = true,
                                                            modifier = Modifier.fillMaxWidth(),
                                                            shape = RoundedCornerShape(12.dp),
                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                focusedBorderColor = Color(selectedAccentHex),
                                                                unfocusedBorderColor = Color(0xFF2C2C35),
                                                                focusedLabelColor = Color(selectedAccentHex),
                                                                unfocusedLabelColor = Color(0xFF8E8E93),
                                                                focusedTextColor = Color.White,
                                                                unfocusedTextColor = Color.White,
                                                                focusedContainerColor = Color(0xFF16161B),
                                                                unfocusedContainerColor = Color(0xFF16161B)
                                                            )
                                                        )
                                                    }

                                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Text("Font Style", color = Color(0xFF8E8E93), fontSize = 12.sp, fontWeight = FontWeight.Medium)

                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            CaptionFont.values().forEach { font ->
                                                                val isSelected = captionFont == font
                                                                Box(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .height(38.dp)
                                                                        .clip(RoundedCornerShape(10.dp))
                                                                        .background(if (isSelected) Color(selectedAccentHex) else Color(0xFF1C1C22))
                                                                        .clickable {
                                                                            captionFont = font
                                                                            syncActiveItem(newFont = font)
                                                                        },
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(
                                                                        text = font.label,
                                                                        color = if (isSelected) {
                                                                            if (calculateLuminance(selectedAccentHex) > 0.5f) Color.Black else Color.White
                                                                        } else Color(0xFFD1D1D6),
                                                                        fontSize = 12.sp,
                                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("Text Color", color = Color(0xFF8E8E93), fontSize = 12.sp, fontWeight = FontWeight.Medium)

                                                        val coreSwatches = listOf(
                                                            0xFFFFFFFFL,
                                                            0xFF121214L,
                                                            selectedAccentHex,
                                                            0xFF8E8E93L
                                                        )

                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            coreSwatches.forEach { hexVal ->
                                                                val isSelected = captionColorHex == hexVal
                                                                val swatchColor = Color(hexVal.toInt())

                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(32.dp)
                                                                        .clip(CircleShape)
                                                                        .background(swatchColor)
                                                                        .border(
                                                                            width = if (isSelected) 2.5.dp else 1.dp,
                                                                            color = if (isSelected) Color.White else Color(0xFF383842),
                                                                            shape = CircleShape
                                                                        )
                                                                        .clickable {
                                                                            captionColorHex = hexVal
                                                                            syncActiveItem(newColorHex = hexVal)
                                                                        },
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    if (isSelected) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Check,
                                                                            contentDescription = null,
                                                                            tint = if (calculateLuminance(hexVal) > 0.5f) Color.Black else Color.White,
                                                                            modifier = Modifier.size(13.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }

                                                            val isCustom = coreSwatches.none { it == captionColorHex }
                                                            RainbowCustomCircle(
                                                                isSelected = isCustom,
                                                                activeColor = if (isCustom) Color(captionColorHex.toInt()) else null,
                                                                onClick = { activeColorTarget = ConfigColorTarget.CAPTION }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // ==========================================
                                    // TAB 2: WIDGET THEME & APPEARANCE
                                    // ==========================================
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "BACKGROUND STYLE",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF8E8E93),
                                            letterSpacing = 0.5.sp
                                        )

                                        val bgPresets = listOf(
                                            0xFF161618L to "Matte",
                                            0xFF000000L to "AMOLED",
                                            0xFFFFFFFFL to "Light"
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            bgPresets.forEach { (hex, label) ->
                                                val isSelected = selectedBgHex == hex
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(42.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(if (isSelected) Color(0xFF26262E) else Color(0xFF141416))
                                                        .border(
                                                            1.dp,
                                                            if (isSelected) Color.White.copy(alpha = 0.5f) else Color(0xFF202024),
                                                            RoundedCornerShape(12.dp)
                                                        )
                                                        .clickable {
                                                            selectedBgHex = hex
                                                            if (hex == 0xFFFFFFFFL && selectedAccentHex == 0xFFFFFFFFL) {
                                                                selectedAccentHex = 0xFF000000L
                                                            } else if (hex != 0xFFFFFFFFL && selectedAccentHex == 0xFF000000L) {
                                                                selectedAccentHex = 0xFFFFFFFFL
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(hex))
                                                                .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                                        )
                                                        Text(
                                                            text = label,
                                                            color = if (isSelected) Color.White else Color(0xFF8E8E93),
                                                            fontSize = 12.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }

                                            val isCustomBg = bgPresets.none { it.first == selectedBgHex }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(42.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isCustomBg) Color(0xFF26262E) else Color(0xFF141416))
                                                    .border(
                                                        1.dp,
                                                        if (isCustomBg) Color.White.copy(alpha = 0.5f) else Color(0xFF202024),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { activeColorTarget = ConfigColorTarget.BACKGROUND },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    val rainbowBrush = Brush.sweepGradient(
                                                        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .size(9.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isCustomBg) Color(selectedBgHex) else Color.Transparent)
                                                            .then(if (!isCustomBg) Modifier.background(rainbowBrush) else Modifier)
                                                            .border(0.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                                    )
                                                    Text(
                                                        text = "Custom",
                                                        color = if (isCustomBg) Color.White else Color(0xFF8E8E93),
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isCustomBg) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Accent Color Selection
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "ACCENT COLOR",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF8E8E93),
                                            letterSpacing = 0.5.sp
                                        )

                                        val isLightBg = calculateLuminance(selectedBgHex) > 0.5f
                                        val accentPresets = if (isLightBg) {
                                            listOf(0xFF000000L, 0xFF00D166L, 0xFF2B80FFL, 0xFFFF3B30L, 0xFFFF9500L, 0xFFAF52DEL)
                                        } else {
                                            listOf(0xFFFFFFFFL, 0xFF00D166L, 0xFF2B80FFL, 0xFFFF3B30L, 0xFFFF9500L, 0xFFAF52DEL)
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            accentPresets.forEach { hex ->
                                                val isSelected = selectedAccentHex == hex
                                                val swatchColor = Color(hex)
                                                val scale by animateFloatAsState(if (isSelected) 1.15f else 1.0f, label = "circleScale")

                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .scale(scale)
                                                        .clickable { selectedAccentHex = hex },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .clip(CircleShape)
                                                                .border(1.8.dp, Color.White, CircleShape)
                                                        )
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(swatchColor)
                                                            .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isSelected) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = if (calculateLuminance(hex) > 0.6f) Color.Black else Color.White,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            val isCustomAccent = accentPresets.none { it == selectedAccentHex }
                                            RainbowCustomCircle(
                                                isSelected = isCustomAccent,
                                                activeColor = if (isCustomAccent) Color(selectedAccentHex) else null,
                                                onClick = { activeColorTarget = ConfigColorTarget.ACCENT }
                                            )
                                        }
                                    }

                                    // Surface Opacity Slider
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Surface Translucency", color = Color(0xFF8E8E93), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            Text("${(opacity * 100).toInt()}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Slider(
                                            value = opacity,
                                            onValueChange = { opacity = it },
                                            valueRange = 0.0f..1.0f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color.White,
                                                activeTrackColor = Color(selectedAccentHex),
                                                inactiveTrackColor = Color(0xFF282832)
                                            )
                                        )
                                    }

                                    // Sizing Mode (Responsive vs Fixed Aspect)
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "SIZING MODE",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF8E8E93),
                                            letterSpacing = 0.5.sp
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF141416))
                                                .padding(3.dp)
                                        ) {
                                            listOf(true to "Responsive", false to "Fixed Aspect").forEach { (resp, label) ->
                                                val isModeSelected = isResponsive == resp
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(38.dp)
                                                        .clip(RoundedCornerShape(9.dp))
                                                        .background(if (isModeSelected) Color(0xFF26262E) else Color.Transparent)
                                                        .clickable { isResponsive = resp },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = label,
                                                        color = if (isModeSelected) Color.White else Color(0xFF8E8E93),
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isModeSelected) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Shared Color Picker
                activeColorTarget?.let { target ->
                    val initColor = when (target) {
                        ConfigColorTarget.BACKGROUND -> Color(selectedBgHex)
                        ConfigColorTarget.ACCENT -> Color(selectedAccentHex)
                        ConfigColorTarget.CAPTION -> Color(captionColorHex.toInt())
                    }
                    val pickerTitle = when (target) {
                        ConfigColorTarget.BACKGROUND -> "Custom Background"
                        ConfigColorTarget.ACCENT -> "Custom Accent"
                        ConfigColorTarget.CAPTION -> "Caption Color"
                    }

                    CustomColorPickerDialog(
                        initialColor = initColor,
                        title = pickerTitle,
                        onDismiss = { activeColorTarget = null },
                        onColorSelected = { color ->
                            val hex = (color.toArgb().toLong() and 0xFFFFFFFFL)
                            when (target) {
                                ConfigColorTarget.BACKGROUND -> selectedBgHex = hex
                                ConfigColorTarget.ACCENT -> selectedAccentHex = hex
                                ConfigColorTarget.CAPTION -> {
                                    captionColorHex = hex
                                    syncActiveItem(newColorHex = hex)
                                }
                            }
                            activeColorTarget = null
                        }
                    )
                }
            }
        }
    }
}

// ============================================================================
// EXACT WIDGET BITMAP GENERATOR FOR PHOTOS CONFIG VIEWPORT
// ============================================================================

private fun renderExactPhotoWidgetPreview(
    context: Context,
    widgetId: Int,
    photoConfig: PhotosWidgetConfig,
    slateConfig: SlateWidgetConfig
): Pair<Bitmap, Float> {
    val info = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
        AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetId)
    } else null
    val providerClass = info?.provider?.className ?: ""

    val activeItem = photoConfig.currentItem
    val isResponsive = false

    return when {
        providerClass.contains("OnThisDay") -> {
            Pair(
                generateOnThisDayBitmap(
                    context = context,
                    item = activeItem,
                    slateConfig = slateConfig,
                    isResponsive = isResponsive,
                    wDp = 320,
                    hDp = 160,
                    showCaption = photoConfig.showCaption
                ),
                2.05f
            )
        }
        providerClass.contains("FilmStrip") -> {
            Pair(generateFilmStripBitmap(context, photoConfig.items, slateConfig, isResponsive, 320, 160), 2.1f)
        }
        providerClass.contains("CollageBento") -> {
            Pair(
                generateCollageBentoBitmap(
                    context = context,
                    items = photoConfig.items,
                    slateConfig = slateConfig,
                    isResponsive = isResponsive,
                    wDp = 320,
                    hDp = 160,
                    showCaption = photoConfig.showCaption
                ),
                2.05f
            )
        }
        providerClass.contains("Carousel") -> {
            Pair(
                generatePhotoCarouselBitmap(
                    context = context,
                    config = photoConfig,
                    slateConfig = slateConfig,
                    isResponsive = isResponsive,
                    wDp = 200,
                    hDp = 200,
                    showCaption = photoConfig.showCaption
                ),
                1.0f
            )
        }
        providerClass.contains("Stamp") -> {
            Pair(
                generatePhotoStampBitmap(
                    context = context,
                    item = activeItem,
                    slateConfig = slateConfig,
                    isResponsive = isResponsive,
                    wDp = 200,
                    hDp = 200,
                    showCaption = photoConfig.showCaption
                ),
                1.0f
            )
        }
        providerClass.contains("Locket") -> {
            Pair(generateLocketMemoryBitmap(context, activeItem, slateConfig, isResponsive, 200, 200), 1.0f)
        }
        providerClass.contains("ClockOverlay") -> {
            Pair(
                generatePhotoClockOverlayBitmap(
                    context = context,
                    item = activeItem,
                    slateConfig = slateConfig,
                    isResponsive = isResponsive,
                    wDp = 200,
                    hDp = 200,
                    showCaption = photoConfig.showCaption
                ),
                1.0f
            )
        }
        providerClass.contains("Stacked") -> {
            Pair(
                generateStackedMemoryBitmap(
                    context = context,
                    item = activeItem,
                    slateConfig = slateConfig,
                    isResponsive = isResponsive,
                    wDp = 200,
                    hDp = 200,
                    showCaption = photoConfig.showCaption
                ),
                1.0f
            )
        }
        providerClass.contains("Taped") -> {
            Pair(
                generateTapedPolaroidBitmap(
                    context = context,
                    item = activeItem,
                    slateConfig = slateConfig,
                    isResponsive = isResponsive,
                    wDp = 200,
                    hDp = 200,
                    showCaption = photoConfig.showCaption
                ),
                1.0f
            )
        }
        providerClass.contains("PushPin") -> {
            Pair(
                generatePushPinBitmap(
                    context = context,
                    item = activeItem,
                    slateConfig = slateConfig,
                    isResponsive = isResponsive,
                    wDp = 200,
                    hDp = 200,
                    showCaption = photoConfig.showCaption
                ),
                1.0f
            )
        }
        providerClass.contains("PhotosSquare") -> {
            Pair(generateShapedPhotoBitmap(context, activeItem, PhotoShape.SQUARE, slateConfig, isResponsive, 200, 200, photoConfig.showCaption), 1.0f)
        }
        providerClass.contains("PhotosRectangle") -> {
            Pair(generateShapedPhotoBitmap(context, activeItem, PhotoShape.RECTANGLE, slateConfig, isResponsive, 320, 160, photoConfig.showCaption), 2.0f)
        }
        providerClass.contains("PhotosCircle") -> {
            Pair(generateShapedPhotoBitmap(context, activeItem, PhotoShape.CIRCLE, slateConfig, isResponsive, 200, 200, photoConfig.showCaption), 1.0f)
        }
        providerClass.contains("PhotosHeart") -> {
            Pair(generateShapedPhotoBitmap(context, activeItem, PhotoShape.HEART, slateConfig, isResponsive, 200, 200, photoConfig.showCaption), 1.0f)
        }
        providerClass.contains("PhotosFlower") -> {
            Pair(generateShapedPhotoBitmap(context, activeItem, PhotoShape.FLOWER, slateConfig, isResponsive, 200, 200, photoConfig.showCaption), 1.0f)
        }
        providerClass.contains("PhotosStar") -> {
            Pair(generateShapedPhotoBitmap(context, activeItem, PhotoShape.STAR, slateConfig, isResponsive, 200, 200, photoConfig.showCaption), 1.0f)
        }
        providerClass.contains("PhotosClover") -> {
            Pair(generateShapedPhotoBitmap(context, activeItem, PhotoShape.CLOVER, slateConfig, isResponsive, 200, 200, photoConfig.showCaption), 1.0f)
        }
        providerClass.contains("PhotosBlob") -> {
            Pair(generateShapedPhotoBitmap(context, activeItem, PhotoShape.BLOB, slateConfig, isResponsive, 200, 200, photoConfig.showCaption), 1.0f)
        }
        else -> {
            Pair(
                generatePolaroidMemoryBitmap(
                    context = context,
                    item = activeItem,
                    slateConfig = slateConfig,
                    isResponsive = isResponsive,
                    wDp = 200,
                    hDp = 200,
                    showCaption = photoConfig.showCaption
                ),
                1.0f
            )
        }
    }
}

private fun loadSlateWidgetConfig(context: Context, widgetId: Int): SlateWidgetConfig {
    val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val hasCustomTheme = widgetPrefs.getBoolean("widget_${widgetId}_has_custom_theme", false)
    val globalSettings = ThemePreferences(context).getThemeSettings()

    val bgColor = if (hasCustomTheme) {
        widgetPrefs.getLong("widget_${widgetId}_bg_color", globalSettings.bgHex)
    } else {
        globalSettings.bgHex
    }
    val opacity = if (hasCustomTheme) {
        widgetPrefs.getFloat("widget_${widgetId}_opacity", globalSettings.opacity)
    } else {
        globalSettings.opacity
    }
    val accentColor = if (hasCustomTheme) {
        widgetPrefs.getLong("widget_${widgetId}_accent_color", globalSettings.accentHex)
    } else {
        globalSettings.accentHex
    }

    val isLight = (((bgColor shr 16 and 0xFFL) * 0.2126f) +
            ((bgColor shr 8 and 0xFFL) * 0.7152f) +
            ((bgColor and 0xFFL) * 0.0722f)) / 255f > 0.5f
    val mode = if (hasCustomTheme) {
        widgetPrefs.getString("widget_${widgetId}_theme_mode", if (isLight) "LIGHT" else "DARK")
            ?: if (isLight) "LIGHT" else "DARK"
    } else {
        if (isLight) "LIGHT" else "DARK"
    }

    return SlateWidgetConfig(
        themeMode = mode,
        backgroundColorHex = bgColor,
        opacity = opacity,
        accentColorHex = accentColor
    )
}

// ============================================================================
// FEATURE: 8-HANDLE INTERACTIVE CROP ENGINE
// ============================================================================

enum class CropRatio(val label: String, val ratio: Float?) {
    FREEFORM("Freeform", null),
    SQUARE("1:1", 1.0f),
    PORTRAIT("3:4", 0.75f),
    LANDSCAPE("16:9", 1.77f)
}

private enum class DragHandle { NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, TOP_CENTER, BOTTOM_CENTER, LEFT_CENTER, RIGHT_CENTER, CENTER }

val FlipHorizontalIcon = ImageVector.Builder(
    name = "FlipHorizontal", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
).apply {
    path(stroke = SolidColor(Color.White), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(9f, 4f); lineTo(2f, 12f); lineTo(9f, 20f); close()
        moveTo(15f, 4f); lineTo(22f, 12f); lineTo(15f, 20f); close()
        moveTo(12f, 2f); lineTo(12f, 22f)
    }
}.build()

val FlipVerticalIcon = ImageVector.Builder(
    name = "FlipVertical", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
).apply {
    path(stroke = SolidColor(Color.White), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(4f, 9f); lineTo(12f, 2f); lineTo(20f, 9f); close()
        moveTo(4f, 15f); lineTo(12f, 22f); lineTo(20f, 15f); close()
        moveTo(2f, 12f); lineTo(22f, 12f)
    }
}.build()

val SwapIconVector = ImageVector.Builder(
    name = "SwapIcon", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
).apply {
    path(stroke = SolidColor(Color.White), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(16f, 3f); lineTo(21f, 8f); lineTo(16f, 13f)
        moveTo(21f, 8f); lineTo(3f, 8f)
        moveTo(8f, 21f); lineTo(3f, 16f); lineTo(8f, 11f)
        moveTo(3f, 16f); lineTo(21f, 16f)
    }
}.build()

@Composable
fun SlateProEditorOverlay(
    rawUri: Uri,
    onDismiss: () -> Unit,
    onImageTransformed: (String) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val paddingPx = with(density) { 20.dp.toPx() }

    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var flipH by remember { mutableStateOf(false) }
    var flipV by remember { mutableStateOf(false) }
    var selectedRatio by remember { mutableStateOf(CropRatio.FREEFORM) }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val rawBitmap = remember(rawUri) {
        try {
            context.contentResolver.openInputStream(rawUri)?.use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) { null }
    }

    val imageRect = remember(rawBitmap, containerSize, rotationAngle, paddingPx) {
        if (rawBitmap == null || containerSize == IntSize.Zero) Rect.Zero
        else {
            val isRotated = (rotationAngle.toInt() / 90) % 2 != 0
            val imgW = if (isRotated) rawBitmap.height.toFloat() else rawBitmap.width.toFloat()
            val imgH = if (isRotated) rawBitmap.width.toFloat() else rawBitmap.height.toFloat()

            val availW = (containerSize.width.toFloat() - (paddingPx * 2)).coerceAtLeast(1f)
            val availH = (containerSize.height.toFloat() - (paddingPx * 2)).coerceAtLeast(1f)

            val fitScale = minOf(availW / imgW, availH / imgH)
            val drawW = imgW * fitScale
            val drawH = imgH * fitScale

            val left = (containerSize.width - drawW) / 2f
            val top = (containerSize.height - drawH) / 2f
            Rect(left, top, left + drawW, top + drawH)
        }
    }

    var cropRect by remember { mutableStateOf(Rect.Zero) }

    LaunchedEffect(imageRect, selectedRatio) {
        if (imageRect != Rect.Zero) {
            val targetRatio = selectedRatio.ratio ?: (imageRect.width / imageRect.height)
            var boxW = imageRect.width
            var boxH = boxW / targetRatio

            if (boxH > imageRect.height) {
                boxH = imageRect.height
                boxW = boxH * targetRatio
            }

            val left = imageRect.left + (imageRect.width - boxW) / 2f
            val top = imageRect.top + (imageRect.height - boxH) / 2f
            cropRect = Rect(left, top, left + boxW, top + boxH)
        }
    }

    var activeHandle by remember { mutableStateOf(DragHandle.NONE) }

    val currentCropRect by rememberUpdatedState(cropRect)
    val currentImageRect by rememberUpdatedState(imageRect)
    val currentRatio by rememberUpdatedState(selectedRatio)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A0A0C)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF1C1C1E)).clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White, modifier = Modifier.size(20.dp)) }

                Text(text = "Edit Photo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)

                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .clickable {
                            if (rawBitmap != null && cropRect != Rect.Zero && imageRect != Rect.Zero) {
                                val processed = cropBitmapFromBounds(
                                    context = context,
                                    source = rawBitmap,
                                    rotation = rotationAngle,
                                    flipH = flipH,
                                    flipV = flipV,
                                    cropRect = cropRect,
                                    imageRect = imageRect
                                )
                                processed?.let { onImageTransformed(it) }
                            }
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Done", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black)
                    .onGloballyPositioned { containerSize = it.size },
                contentAlignment = Alignment.Center
            ) {
                if (rawBitmap != null) {
                    val previewBitmap = remember(rawBitmap) { rawBitmap.asImageBitmap() }

                    Image(
                        bitmap = previewBitmap,
                        contentDescription = "Edit Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .graphicsLayer {
                                rotationZ = rotationAngle
                                scaleX = if (flipH) -1f else 1f
                                scaleY = if (flipV) -1f else 1f
                            }
                    )

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { touch ->
                                        val thresh = 36.dp.toPx()
                                        val r = currentCropRect
                                        val cx = r.left + r.width / 2f
                                        val cy = r.top + r.height / 2f

                                        val handles = listOf(
                                            DragHandle.TOP_LEFT to Pair(r.left, r.top),
                                            DragHandle.TOP_RIGHT to Pair(r.right, r.top),
                                            DragHandle.BOTTOM_LEFT to Pair(r.left, r.bottom),
                                            DragHandle.BOTTOM_RIGHT to Pair(r.right, r.bottom),
                                            DragHandle.TOP_CENTER to Pair(cx, r.top),
                                            DragHandle.BOTTOM_CENTER to Pair(cx, r.bottom),
                                            DragHandle.LEFT_CENTER to Pair(r.left, cy),
                                            DragHandle.RIGHT_CENTER to Pair(r.right, cy)
                                        )

                                        val hit = handles
                                            .map { (handle, pos) -> handle to hypot(touch.x - pos.first, touch.y - pos.second) }
                                            .filter { it.second <= thresh }
                                            .minByOrNull { it.second }

                                        activeHandle = when {
                                            hit != null -> hit.first
                                            r.contains(touch) -> DragHandle.CENTER
                                            else -> DragHandle.NONE
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val rect = currentCropRect
                                        val img = currentImageRect
                                        if (rect == Rect.Zero || img == Rect.Zero || activeHandle == DragHandle.NONE) return@detectDragGestures

                                        val minSize = 90f
                                        val targetRatio = currentRatio.ratio

                                        var newL = rect.left
                                        var newT = rect.top
                                        var newR = rect.right
                                        var newB = rect.bottom

                                        if (activeHandle == DragHandle.CENTER) {
                                            val dx = dragAmount.x.coerceIn(img.left - rect.left, img.right - rect.right)
                                            val dy = dragAmount.y.coerceIn(img.top - rect.top, img.bottom - rect.bottom)
                                            newL += dx; newR += dx
                                            newT += dy; newB += dy
                                        } else if (targetRatio == null) {
                                            when (activeHandle) {
                                                DragHandle.TOP_LEFT -> {
                                                    newL = (rect.left + dragAmount.x).coerceIn(img.left, rect.right - minSize)
                                                    newT = (rect.top + dragAmount.y).coerceIn(img.top, rect.bottom - minSize)
                                                }
                                                DragHandle.TOP_RIGHT -> {
                                                    newR = (rect.right + dragAmount.x).coerceIn(rect.left + minSize, img.right)
                                                    newT = (rect.top + dragAmount.y).coerceIn(img.top, rect.bottom - minSize)
                                                }
                                                DragHandle.BOTTOM_LEFT -> {
                                                    newL = (rect.left + dragAmount.x).coerceIn(img.left, rect.right - minSize)
                                                    newB = (rect.bottom + dragAmount.y).coerceIn(rect.top + minSize, img.bottom)
                                                }
                                                DragHandle.BOTTOM_RIGHT -> {
                                                    newR = (rect.right + dragAmount.x).coerceIn(rect.left + minSize, img.right)
                                                    newB = (rect.bottom + dragAmount.y).coerceIn(rect.top + minSize, img.bottom)
                                                }
                                                DragHandle.TOP_CENTER -> newT = (rect.top + dragAmount.y).coerceIn(img.top, rect.bottom - minSize)
                                                DragHandle.BOTTOM_CENTER -> newB = (rect.bottom + dragAmount.y).coerceIn(rect.top + minSize, img.bottom)
                                                DragHandle.LEFT_CENTER -> newL = (rect.left + dragAmount.x).coerceIn(img.left, rect.right - minSize)
                                                DragHandle.RIGHT_CENTER -> newR = (rect.right + dragAmount.x).coerceIn(rect.left + minSize, img.right)
                                                else -> {}
                                            }
                                        } else {
                                            val cx = rect.left + rect.width / 2f
                                            val cy = rect.top + rect.height / 2f

                                            when (activeHandle) {
                                                DragHandle.TOP_CENTER -> {
                                                    val proposedT = (rect.top + dragAmount.y).coerceIn(img.top, rect.bottom - minSize)
                                                    val proposedH = rect.bottom - proposedT
                                                    val proposedW = proposedH * targetRatio

                                                    val maxW = minOf(cx - img.left, img.right - cx) * 2f
                                                    val finalW = minOf(proposedW, maxW)
                                                    val finalH = finalW / targetRatio

                                                    newT = rect.bottom - finalH
                                                    newL = cx - finalW / 2f
                                                    newR = cx + finalW / 2f
                                                }
                                                DragHandle.BOTTOM_CENTER -> {
                                                    val proposedB = (rect.bottom + dragAmount.y).coerceIn(rect.top + minSize, img.bottom)
                                                    val proposedH = proposedB - rect.top
                                                    val proposedW = proposedH * targetRatio

                                                    val maxW = minOf(cx - img.left, img.right - cx) * 2f
                                                    val finalW = minOf(proposedW, maxW)
                                                    val finalH = finalW / targetRatio

                                                    newB = rect.top + finalH
                                                    newL = cx - finalW / 2f
                                                    newR = cx + finalW / 2f
                                                }
                                                DragHandle.LEFT_CENTER -> {
                                                    val proposedL = (rect.left + dragAmount.x).coerceIn(img.left, rect.right - minSize)
                                                    val proposedW = rect.right - proposedL
                                                    val proposedH = proposedW / targetRatio

                                                    val maxH = minOf(cy - img.top, img.bottom - cy) * 2f
                                                    val finalH = minOf(proposedH, maxH)
                                                    val finalW = finalH * targetRatio

                                                    newL = rect.right - finalW
                                                    newT = cy - finalH / 2f
                                                    newB = cy + finalH / 2f
                                                }
                                                DragHandle.RIGHT_CENTER -> {
                                                    val proposedR = (rect.right + dragAmount.x).coerceIn(rect.left + minSize, img.right)
                                                    val proposedW = proposedR - rect.left
                                                    val proposedH = proposedW / targetRatio

                                                    val maxH = minOf(cy - img.top, img.bottom - cy) * 2f
                                                    val finalH = minOf(proposedH, maxH)
                                                    val finalW = finalH * targetRatio

                                                    newR = rect.left + finalW
                                                    newT = cy - finalH / 2f
                                                    newB = cy + finalH / 2f
                                                }
                                                DragHandle.BOTTOM_RIGHT -> {
                                                    val deltaW = dragAmount.x + dragAmount.y * targetRatio
                                                    val maxW = img.right - rect.left
                                                    val maxH = img.bottom - rect.top
                                                    val allowedW = minOf(maxW, maxH * targetRatio)
                                                    val newW = (rect.width + deltaW / 2f).coerceIn(minSize, allowedW)
                                                    newR = rect.left + newW
                                                    newB = rect.top + (newW / targetRatio)
                                                }
                                                DragHandle.TOP_LEFT -> {
                                                    val deltaW = -dragAmount.x - dragAmount.y * targetRatio
                                                    val maxW = rect.right - img.left
                                                    val maxH = rect.bottom - img.top
                                                    val allowedW = minOf(maxW, maxH * targetRatio)
                                                    val newW = (rect.width + deltaW / 2f).coerceIn(minSize, allowedW)
                                                    newL = rect.right - newW
                                                    newT = rect.bottom - (newW / targetRatio)
                                                }
                                                DragHandle.TOP_RIGHT -> {
                                                    val deltaW = dragAmount.x - dragAmount.y * targetRatio
                                                    val maxW = img.right - rect.left
                                                    val maxH = img.bottom - rect.top
                                                    val allowedW = minOf(maxW, maxH * targetRatio)
                                                    val newW = (rect.width + deltaW / 2f).coerceIn(minSize, allowedW)
                                                    newR = rect.left + newW
                                                    newT = rect.bottom - (newW / targetRatio)
                                                }
                                                DragHandle.BOTTOM_LEFT -> {
                                                    val deltaW = -dragAmount.x + dragAmount.y * targetRatio
                                                    val maxW = rect.right - img.left
                                                    val maxH = img.bottom - rect.top
                                                    val allowedW = minOf(maxW, maxH * targetRatio)
                                                    val newW = (rect.width + deltaW / 2f).coerceIn(minSize, allowedW)
                                                    newL = rect.right - newW
                                                    newB = rect.top + (newW / targetRatio)
                                                }
                                                else -> {}
                                            }
                                        }

                                        cropRect = Rect(newL, newT, newR, newB)
                                    },
                                    onDragEnd = { activeHandle = DragHandle.NONE },
                                    onDragCancel = { activeHandle = DragHandle.NONE }
                                )
                            }
                    ) {
                        if (cropRect != Rect.Zero) {
                            val overlayColor = Color.Black.copy(alpha = 0.65f)

                            drawRect(overlayColor, topLeft = Offset(0f, 0f), size = Size(size.width, cropRect.top))
                            drawRect(overlayColor, topLeft = Offset(0f, cropRect.bottom), size = Size(size.width, size.height - cropRect.bottom))
                            drawRect(overlayColor, topLeft = Offset(0f, cropRect.top), size = Size(cropRect.left, cropRect.height))
                            drawRect(overlayColor, topLeft = Offset(cropRect.right, cropRect.top), size = Size(size.width - cropRect.right, cropRect.height))

                            drawRect(
                                color = Color.White,
                                topLeft = cropRect.topLeft,
                                size = cropRect.size,
                                style = Stroke(width = 1.5.dp.toPx())
                            )

                            val handleRadius = 6.5.dp.toPx()
                            val cx = cropRect.left + cropRect.width / 2f
                            val cy = cropRect.top + cropRect.height / 2f

                            val points = listOf(
                                Offset(cropRect.left, cropRect.top),
                                Offset(cropRect.right, cropRect.top),
                                Offset(cropRect.left, cropRect.bottom),
                                Offset(cropRect.right, cropRect.bottom),
                                Offset(cx, cropRect.top),
                                Offset(cx, cropRect.bottom),
                                Offset(cropRect.left, cy),
                                Offset(cropRect.right, cy)
                            )

                            points.forEach { point ->
                                drawCircle(color = Color.White, radius = handleRadius, center = point)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Controls Island
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF141416))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF0A0A0C)).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CropRatio.values().forEach { ratio ->
                        val isSelected = selectedRatio == ratio
                        val animatedBg by animateColorAsState(if (isSelected) Color(0xFF2C2C30) else Color.Transparent, label = "ratioBg")

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(animatedBg)
                                .clickable { selectedRatio = ratio },
                            contentAlignment = Alignment.Center
                        ) { Text(text = ratio.label, color = if (isSelected) Color.White else Color(0xFF8E8E93), fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    EditorActionButton(Icons.Default.Refresh, "Rotate") { rotationAngle = (rotationAngle + 90f) % 360f }
                    EditorActionButton(FlipHorizontalIcon, "Flip H") { flipH = !flipH }
                    EditorActionButton(FlipVerticalIcon, "Flip V") { flipV = !flipV }
                }
            }
        }
    }
}

@Composable
private fun EditorActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier.size(46.dp).clip(CircleShape).background(Color(0xFF1C1C1E)),
            contentAlignment = Alignment.Center
        ) { Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp)) }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, color = Color(0xFF8E8E93), fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

private fun cropBitmapFromBounds(
    context: Context,
    source: Bitmap,
    rotation: Float,
    flipH: Boolean,
    flipV: Boolean,
    cropRect: Rect,
    imageRect: Rect
): String? {
    return try {
        val matrix = Matrix().apply {
            postRotate(rotation)
            postScale(if (flipH) -1f else 1f, if (flipV) -1f else 1f)
        }
        val transformed = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)

        val scaleX = transformed.width / imageRect.width
        val scaleY = transformed.height / imageRect.height

        val cropX = ((cropRect.left - imageRect.left) * scaleX).toInt().coerceIn(0, transformed.width - 1)
        val cropY = ((cropRect.top - imageRect.top) * scaleY).toInt().coerceIn(0, transformed.height - 1)

        val cropW = (cropRect.width * scaleX).toInt().coerceIn(1, transformed.width - cropX)
        val cropH = (cropRect.height * scaleY).toInt().coerceIn(1, transformed.height - cropY)

        val cropped = Bitmap.createBitmap(transformed, cropX, cropY, cropW, cropH)

        val outputFile = File(context.cacheDir, "slate_cropped_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outputFile).use { out -> cropped.compress(Bitmap.CompressFormat.JPEG, 92, out) }
        Uri.fromFile(outputFile).toString()
    } catch (_: Exception) { null }
}
