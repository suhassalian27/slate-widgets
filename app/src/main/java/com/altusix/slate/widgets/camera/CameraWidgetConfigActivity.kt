package com.altusix.slate.widgets.camera

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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.hypot

class CameraWidgetConfigActivity : ComponentActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setResult(Activity.RESULT_CANCELED)

        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            widgetId = intent?.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        }

        val initialConfig = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            CameraWidgetPreferences.loadConfig(this, widgetId)
        } else {
            CameraWidgetConfig()
        }

        val themePrefs = ThemePreferences(this).getThemeSettings()
        val accentColor = themePrefs.accentColor

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0C0C0E),
                    surface = Color(0xFF16161B)
                )
            ) {
                var config by remember { mutableStateOf(initialConfig) }
                var rawPickedUri by remember { mutableStateOf<Uri?>(null) }
                var showColorPicker by remember { mutableStateOf(false) }
                var showAdvanced by remember { mutableStateOf(false) }

                val photoPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri: Uri? ->
                    uri?.let { rawPickedUri = it }
                }

                fun saveAndFinish() {
                    if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        CameraWidgetPreferences.saveConfig(this@CameraWidgetConfigActivity, widgetId, config)
                        val appWidgetManager = AppWidgetManager.getInstance(this@CameraWidgetConfigActivity)
                        updateCameraWidget(this@CameraWidgetConfigActivity, appWidgetManager, widgetId)
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
                        onDismiss = { rawPickedUri = null },
                        onImageTransformed = { editedUriStr ->
                            config = config.copy(photoUri = editedUriStr)
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
                            // 1. Top Bar
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
                                        text = "Camera Frame Setup",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }

                                Button(
                                    onClick = { saveAndFinish() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accentColor,
                                        contentColor = Color.Black
                                    ),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Save", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            HorizontalDivider(color = Color(0xFF1E1E24), thickness = 1.dp)

                            // 2. PINNED / STICKY LIVE PREVIEW
                            val context = LocalContext.current
                            val slateConfig = remember(widgetId) { loadSlateWidgetConfig(context, widgetId) }

                            val previewData by produceState(
                                initialValue = Pair<Bitmap?, Float>(null, 1.0f),
                                config,
                                slateConfig
                            ) {
                                value = withContext(Dispatchers.Default) {
                                    renderExactWidgetPreview(context, widgetId, config, slateConfig)
                                }
                            }

                            val previewBitmap = previewData.first
                            val targetAspect = previewData.second

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF141418))
                                    .border(1.dp, Color(0xFF24242C), RoundedCornerShape(20.dp))
                                    .clickable {
                                        if (config.photoUri != null) {
                                            rawPickedUri = Uri.parse(config.photoUri)
                                        } else {
                                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (previewBitmap != null && !config.photoUri.isNullOrEmpty()) {
                                    Image(
                                        bitmap = previewBitmap.asImageBitmap(),
                                        contentDescription = "Exact Widget Preview",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .padding(10.dp)
                                            .aspectRatio(targetAspect)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color.Black.copy(alpha = 0.70f))
                                                .clickable {
                                                    config.photoUri?.let { rawPickedUri = Uri.parse(it) }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                "Edit Crop",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color.Black.copy(alpha = 0.70f))
                                                .clickable {
                                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                                }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                "Change Photo",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(accentColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Choose Photo",
                                                tint = accentColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Text(
                                            text = "Tap to choose a photo",
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Live preview reflects your exact home screen frame",
                                            color = Color(0xFF8E8E93),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFF1A1A22), thickness = 1.dp)

                            // 3. SCROLLABLE CONTROLS BODY
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                // Frame Border Style
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "FRAME BORDER STYLE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF8E8E93),
                                        letterSpacing = 0.5.sp
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        PhotoFrameBorder.values().forEach { border ->
                                            val isSelected = config.borderStyle == border
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { config = config.copy(borderStyle = border) },
                                                label = {
                                                    Text(
                                                        text = border.label,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = accentColor,
                                                    selectedLabelColor = Color.Black,
                                                    containerColor = Color(0xFF1C1C22),
                                                    labelColor = Color(0xFFD1D1D6)
                                                ),
                                                border = FilterChipDefaults.filterChipBorder(
                                                    borderColor = if (isSelected) accentColor else Color(0xFF282830),
                                                    enabled = true,
                                                    selected = isSelected
                                                )
                                            )
                                        }
                                    }
                                }

                                // Aesthetic Filters
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
                                        PhotoFilterStyle.values().forEach { style ->
                                            val isChosen = config.filterStyle == style
                                            FilterChip(
                                                selected = isChosen,
                                                onClick = { config = config.copy(filterStyle = style) },
                                                label = {
                                                    Text(
                                                        text = style.label,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = accentColor,
                                                    selectedLabelColor = Color.Black,
                                                    containerColor = Color(0xFF1C1C22),
                                                    labelColor = Color(0xFFD1D1D6)
                                                ),
                                                border = FilterChipDefaults.filterChipBorder(
                                                    borderColor = if (isChosen) accentColor else Color(0xFF282830),
                                                    enabled = true,
                                                    selected = isChosen
                                                )
                                            )
                                        }
                                    }
                                }

                                // Caption Overlay Section
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "CAPTION OVERLAY",
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
                                                checkedTrackColor = accentColor,
                                                uncheckedThumbColor = Color(0xFF8E8E93),
                                                uncheckedTrackColor = Color(0xFF1C1C22)
                                            )
                                        )
                                    }

                                    AnimatedVisibility(visible = config.showCaption) {
                                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            OutlinedTextField(
                                                value = config.customCaption,
                                                onValueChange = { config = config.copy(customCaption = it) },
                                                label = { Text("Caption Text") },
                                                placeholder = { Text("e.g. Captured with Slate") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = accentColor,
                                                    unfocusedBorderColor = Color(0xFF2C2C35),
                                                    focusedLabelColor = accentColor,
                                                    unfocusedLabelColor = Color(0xFF8E8E93),
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedContainerColor = Color(0xFF16161B),
                                                    unfocusedContainerColor = Color(0xFF16161B)
                                                )
                                            )

                                            // 1. Text Size (S / M / L)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Text Size", color = Color(0xFF8E8E93), fontSize = 12.sp, fontWeight = FontWeight.Medium)

                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(Color(0xFF1C1C22))
                                                        .padding(2.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    CaptionSize.values().forEach { size ->
                                                        val isSelected = config.captionSize == size
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(if (isSelected) accentColor else Color.Transparent)
                                                                .clickable {
                                                                    config = config.copy(
                                                                        captionSize = size
                                                                    )
                                                                }
                                                                .padding(horizontal = 16.dp, vertical = 7.dp)
                                                        ) {
                                                            Text(
                                                                text = size.label,
                                                                color = if (isSelected) Color.Black else Color(0xFF8E8E93),
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // 2. Text Color: Swatches + Rainbow Custom Picker
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Text Color", color = Color(0xFF8E8E93), fontSize = 12.sp, fontWeight = FontWeight.Medium)

                                                val coreSwatches = listOf(
                                                    0xFFFFFFFFL, // White
                                                    0xFF121214L, // Black
                                                    accentColor.toArgb().toLong() and 0xFFFFFFFFL, // Accent
                                                    0xFF8E8E93L  // Gray
                                                )

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    coreSwatches.forEach { hexVal ->
                                                        val isSelected = config.captionColorHex == hexVal
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
                                                                .clickable { config = config.copy(captionColorHex = hexVal) },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            if (isSelected) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = if (hexVal == 0xFFFFFFFFL) Color.Black else Color.White,
                                                                    modifier = Modifier.size(13.dp)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    val isCustom = coreSwatches.none { it == config.captionColorHex }
                                                    RainbowCustomCircle(
                                                        isSelected = isCustom,
                                                        activeColor = if (isCustom) Color(config.captionColorHex.toInt()) else null,
                                                        onClick = { showColorPicker = true }
                                                    )
                                                }
                                            }

                                            // 3. Position (Bottom / Center / Top)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Position", color = Color(0xFF8E8E93), fontSize = 12.sp, fontWeight = FontWeight.Medium)

                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(Color(0xFF1C1C22))
                                                        .padding(2.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    CaptionPosition.values().forEach { pos ->
                                                        val isSelected = config.captionPosition == pos
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(if (isSelected) accentColor else Color.Transparent)
                                                                .clickable {
                                                                    config = config.copy(
                                                                        captionPosition = pos,
                                                                        captionVerticalBias = pos.biasY
                                                                    )
                                                                }
                                                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                                        ) {
                                                            Text(
                                                                text = pos.label,
                                                                color = if (isSelected) Color.Black else Color(0xFF8E8E93),
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // 4. Alignment (Left / Center / Right)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Alignment", color = Color(0xFF8E8E93), fontSize = 12.sp, fontWeight = FontWeight.Medium)

                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(Color(0xFF1C1C22))
                                                        .padding(2.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    CaptionAlignment.values().forEach { align ->
                                                        val isSelected = config.captionAlignment == align
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(if (isSelected) accentColor else Color.Transparent)
                                                                .clickable {
                                                                    config = config.copy(
                                                                        captionAlignment = align,
                                                                        captionHorizontalBias = align.biasX
                                                                    )
                                                                }
                                                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                                        ) {
                                                            Text(
                                                                text = align.label,
                                                                color = if (isSelected) Color.Black else Color(0xFF8E8E93),
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // 5. Advanced Options Toggle
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFF16161C))
                                                    .clickable { showAdvanced = !showAdvanced }
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Advanced Styling",
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Icon(
                                                    imageVector = if (showAdvanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    tint = Color(0xFF8E8E93)
                                                )
                                            }

                                            AnimatedVisibility(visible = showAdvanced) {
                                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                                    // Font Style Selector (Sans / Serif / Mono / Script)
                                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Text("Font Style", color = Color(0xFF8E8E93), fontSize = 12.sp, fontWeight = FontWeight.Medium)

                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            CaptionFont.values().forEach { font ->
                                                                val isSelected = config.captionFont == font
                                                                Box(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .height(36.dp)
                                                                        .clip(RoundedCornerShape(10.dp))
                                                                        .background(if (isSelected) accentColor else Color(0xFF1C1C22))
                                                                        .clickable { config = config.copy(captionFont = font) },
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(
                                                                        text = font.label,
                                                                        color = if (isSelected) Color.Black else Color(0xFFD1D1D6),
                                                                        fontSize = 12.sp,
                                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    // Vertical Position Slider
                                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("Vertical Fine-Tune", color = Color(0xFF8E8E93), fontSize = 12.sp)
                                                            Text("${(config.captionVerticalBias * 100).toInt()}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                        }

                                                        Slider(
                                                            value = config.captionVerticalBias,
                                                            onValueChange = { config = config.copy(captionVerticalBias = it) },
                                                            valueRange = 0.08f..0.92f,
                                                            colors = SliderDefaults.colors(
                                                                thumbColor = accentColor,
                                                                activeTrackColor = accentColor,
                                                                inactiveTrackColor = Color(0xFF282832)
                                                            )
                                                        )
                                                    }

                                                    // Horizontal Position Slider
                                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("Horizontal Fine-Tune", color = Color(0xFF8E8E93), fontSize = 12.sp)
                                                            Text("${(config.captionHorizontalBias * 100).toInt()}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                        }

                                                        Slider(
                                                            value = config.captionHorizontalBias,
                                                            onValueChange = { config = config.copy(captionHorizontalBias = it) },
                                                            valueRange = 0.0f..1.0f,
                                                            colors = SliderDefaults.colors(
                                                                thumbColor = accentColor,
                                                                activeTrackColor = accentColor,
                                                                inactiveTrackColor = Color(0xFF282832)
                                                            )
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
                }

                // Shared Color Picker Dialog from ui.components
                if (showColorPicker) {
                    CustomColorPickerDialog(
                        initialColor = Color(config.captionColorHex.toInt()),
                        title = "Caption Color",
                        onDismiss = { showColorPicker = false },
                        onColorSelected = { color ->
                            val hex = (color.toArgb().toLong() and 0xFFFFFFFFL)
                            config = config.copy(captionColorHex = hex)
                            showColorPicker = false
                        }
                    )
                }
            }
        }
    }
}

// ============================================================================
// EXACT WIDGET BITMAP GENERATOR FOR CONFIG VIEWPORT
// ============================================================================

private fun renderExactWidgetPreview(
    context: Context,
    widgetId: Int,
    cameraConfig: CameraWidgetConfig,
    slateConfig: SlateWidgetConfig
): Pair<Bitmap, Float> {
    val info = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
        AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetId)
    } else null
    val providerClass = info?.provider?.className ?: ""

    return when {
        providerClass.contains("4x2") -> {
            Pair(generatePhotoFrame4x2Bitmap(context, slateConfig, cameraConfig, 320, 160), 2.0f)
        }
        providerClass.contains("Circle") -> {
            Pair(generatePhotoFrameCircleBitmap(context, slateConfig, cameraConfig, 200, 200), 1.0f)
        }
        providerClass.contains("FluidBlob") -> {
            Pair(generatePhotoFrameFluidBlobCameraBitmap(context, slateConfig, cameraConfig, 200, 200), 1.0f)
        }
        providerClass.contains("Blob") -> {
            Pair(generatePhotoFrameBlobCameraBitmap(context, slateConfig, cameraConfig, 200, 200), 1.0f)
        }
        else -> {
            Pair(generatePhotoFrameCameraBitmap(context, slateConfig, cameraConfig, isResponsive = false, 200, 200), 1.0f)
        }
    }
}

private fun loadSlateWidgetConfig(context: Context, widgetId: Int): SlateWidgetConfig {
    val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val globalSettings = ThemePreferences(context).getThemeSettings()
    val bgColor = widgetPrefs.getLong("widget_${widgetId}_bg_color", globalSettings.bgHex)
    val opacity = widgetPrefs.getFloat("widget_${widgetId}_opacity", globalSettings.opacity)
    val accentColor = widgetPrefs.getLong("widget_${widgetId}_accent_color", globalSettings.accentHex)

    val isLight = (((bgColor shr 16 and 0xFFL) * 0.2126f) +
            ((bgColor shr 8 and 0xFFL) * 0.7152f) +
            ((bgColor and 0xFFL) * 0.0722f)) / 255f > 0.5f
    val mode = widgetPrefs.getString("widget_${widgetId}_theme_mode", if (isLight) "LIGHT" else "DARK")
        ?: if (isLight) "LIGHT" else "DARK"

    return SlateWidgetConfig(
        themeMode = mode,
        backgroundColorHex = bgColor,
        opacity = opacity,
        accentColorHex = accentColor
    )
}

// ============================================================================
// 8-HANDLE INTERACTIVE CROP ENGINE
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
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1C1E))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0A0A0C))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CropRatio.values().forEach { ratio ->
                        val isSelected = selectedRatio == ratio
                        val animatedBg by animateColorAsState(
                            if (isSelected) Color(0xFF2C2C30) else Color.Transparent,
                            label = "ratioBg"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(animatedBg)
                                .clickable { selectedRatio = ratio },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ratio.label,
                                color = if (isSelected) Color.White else Color(0xFF8E8E93),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EditorActionButton(Icons.Default.Refresh, "Rotate") {
                        rotationAngle = (rotationAngle + 90f) % 360f
                    }
                    EditorActionButton(FlipHorizontalIcon, "Flip H") {
                        flipH = !flipH
                    }
                    EditorActionButton(FlipVerticalIcon, "Flip V") {
                        flipV = !flipV
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFF1C1C1E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color(0xFF8E8E93),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
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