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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altusix.slate.core.theme.ThemePreferences
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
                            .background(Color(0xFF0C0C0E))
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .imePadding(),
                        color = Color(0xFF0C0C0E)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
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

                            // 2. Scrollable Configuration Body
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                // Live Preview Card
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(210.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF16161B))
                                        .border(1.dp, Color(0xFF282830), RoundedCornerShape(20.dp))
                                        .clickable {
                                            if (config.photoUri != null) {
                                                rawPickedUri = Uri.parse(config.photoUri)
                                            } else {
                                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val imageBitmap = rememberUriImageBitmap(config.photoUri)

                                    if (imageBitmap != null) {
                                        val colorMatrix = remember(config.filterStyle) {
                                            when (config.filterStyle) {
                                                PhotoFilterStyle.GRAYSCALE -> ColorMatrix().apply { setToSaturation(0f) }
                                                PhotoFilterStyle.SEPIA -> ColorMatrix(floatArrayOf(0.393f, 0.769f, 0.189f, 0f, 0f, 0.349f, 0.686f, 0.168f, 0f, 0f, 0.272f, 0.534f, 0.131f, 0f, 0f, 0f, 0f, 0f, 1f, 0f))
                                                PhotoFilterStyle.DARK_DIM -> ColorMatrix().apply { setToScale(0.7f, 0.7f, 0.7f, 1f) }
                                                PhotoFilterStyle.VINTAGE -> ColorMatrix(floatArrayOf(0.9f, 0.1f, 0.1f, 0f, 20f, 0.1f, 0.8f, 0.1f, 0f, 15f, 0.1f, 0.1f, 0.6f, 0f, 10f, 0f, 0f, 0f, 1f, 0f))
                                                PhotoFilterStyle.COOL_BLUE -> ColorMatrix(floatArrayOf(0.7f, 0f, 0.2f, 0f, 0f, 0f, 0.9f, 0.2f, 0f, 0f, 0f, 0.2f, 1.2f, 0f, 20f, 0f, 0f, 0f, 1f, 0f))
                                                PhotoFilterStyle.WARM_GOLD -> ColorMatrix(floatArrayOf(1.2f, 0.1f, 0f, 0f, 15f, 0.1f, 1.1f, 0f, 0f, 10f, 0f, 0f, 0.8f, 0f, -10f, 0f, 0f, 0f, 1f, 0f))
                                                PhotoFilterStyle.HIGH_CONTRAST -> ColorMatrix(floatArrayOf(1.4f, -0.1f, -0.1f, 0f, -20f, -0.1f, 1.4f, -0.1f, 0f, -20f, -0.1f, -0.1f, 1.4f, 0f, -20f, 0f, 0f, 0f, 1f, 0f))
                                                else -> ColorMatrix()
                                            }
                                        }

                                        Image(
                                            bitmap = imageBitmap,
                                            contentDescription = "Live Preview",
                                            contentScale = ContentScale.Crop,
                                            colorFilter = ColorFilter.colorMatrix(colorMatrix),
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        val isPolaroid = config.borderStyle == PhotoFrameBorder.POLAROID

                                        when (config.borderStyle) {
                                            PhotoFrameBorder.POLAROID -> {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .fillMaxHeight(0.24f)
                                                        .align(Alignment.BottomCenter)
                                                        .background(Color.White),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (config.customCaption.isNotEmpty()) {
                                                        Text(
                                                            text = config.customCaption,
                                                            color = Color(0xFF121214),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }
                                            PhotoFrameBorder.VIGNETTE -> Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Brush.radialGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                                            )
                                            PhotoFrameBorder.THIN_BORDER -> Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(2.dp)
                                                    .border(3.5.dp, Color.White, RoundedCornerShape(18.dp))
                                            )
                                            PhotoFrameBorder.INNER_OUTLINE -> Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(8.dp)
                                                    .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                                            )
                                            PhotoFrameBorder.FILM_STRIP -> {
                                                Column(
                                                    modifier = Modifier.fillMaxSize(),
                                                    verticalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Box(modifier = Modifier.fillMaxWidth().height(16.dp).background(Color.Black))
                                                    Box(modifier = Modifier.fillMaxWidth().height(16.dp).background(Color.Black))
                                                }
                                            }
                                            else -> {}
                                        }

                                        if (config.customCaption.isNotEmpty() && !isPolaroid) {
                                            Text(
                                                text = config.customCaption,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(14.dp)
                                            )
                                        }

                                        // Action Buttons
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color.Black.copy(alpha = 0.65f))
                                                    .clickable {
                                                        config.photoUri?.let { rawPickedUri = Uri.parse(it) }
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Text("Edit Crop", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color.Black.copy(alpha = 0.65f))
                                                    .clickable {
                                                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Text("Change Photo", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    } else {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(accentColor.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Choose Photo",
                                                    tint = accentColor,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Text(
                                                text = "Tap to choose a photo",
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Opens camera directly on launcher tap",
                                                color = Color(0xFF8E8E93),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

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

                                // Caption Overlay
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "CAPTION OVERLAY",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF8E8E93),
                                        letterSpacing = 0.5.sp
                                    )

                                    OutlinedTextField(
                                        value = config.customCaption,
                                        onValueChange = { config = config.copy(customCaption = it) },
                                        label = { Text("Caption (Optional)") },
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
                                }
                            }
                        }
                    }
                }
            }
        }
    }
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

@Composable
private fun rememberUriImageBitmap(uriStr: String?): ImageBitmap? {
    val context = LocalContext.current
    return remember(uriStr) {
        if (uriStr.isNullOrEmpty()) null
        else {
            try {
                val uri = Uri.parse(uriStr)
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            } catch (_: Exception) { null }
        }
    }
}