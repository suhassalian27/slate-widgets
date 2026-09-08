package com.altusix.slate.widgets.photos

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altusix.slate.core.theme.ThemePreferences
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotosConfigActivity : ComponentActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Set standard cancel result until user saves
        setResult(Activity.RESULT_CANCELED)

        widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        }

        val initialConfig = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            PhotosStorageManager.getConfig(this, widgetId)
        } else {
            PhotosWidgetConfig.getDefaultConfig()
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
                var selectedIndex by remember {
                    mutableStateOf(initialConfig.currentIndex.coerceIn(0, maxOf(initialConfig.items.size - 1, 0)))
                }

                // Selected item state
                val currentItems = config.items
                val activeItem = currentItems.getOrNull(selectedIndex) ?: SlateMemoryItem()

                var caption by remember(activeItem.id) { mutableStateOf(activeItem.caption) }
                var dateText by remember(activeItem.id) { mutableStateOf(activeItem.dateText) }
                var location by remember(activeItem.id) { mutableStateOf(activeItem.location) }
                var filterStyle by remember(activeItem.id) { mutableStateOf(activeItem.filterStyle) }
                var currentImagePath by remember(activeItem.id) { mutableStateOf(activeItem.imagePath) }

                // Photo picker launcher
                val photoPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri: Uri? ->
                    uri?.let {
                        val savedPath = PhotosStorageManager.copyUriToInternalStorage(
                            context = this@PhotosConfigActivity,
                            widgetId = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) widgetId else 9999,
                            index = System.currentTimeMillis().toInt(),
                            uri = it
                        )
                        if (savedPath != null) {
                            currentImagePath = savedPath
                            // Update the item in the list
                            val updatedItem = activeItem.copy(
                                imagePath = savedPath,
                                caption = caption,
                                dateText = dateText,
                                location = location,
                                filterStyle = filterStyle
                            )
                            val newList = config.items.toMutableList()
                            if (selectedIndex in newList.indices) {
                                newList[selectedIndex] = updatedItem
                            } else {
                                newList.add(updatedItem)
                            }
                            config = config.copy(items = newList)
                        }
                    }
                }

                fun saveAndFinish() {
                    // Update active item before persisting
                    val updatedItem = activeItem.copy(
                        imagePath = currentImagePath,
                        caption = caption.trim(),
                        dateText = dateText.trim(),
                        location = location.trim(),
                        filterStyle = filterStyle
                    )
                    val newList = config.items.toMutableList()
                    if (selectedIndex in newList.indices) {
                        newList[selectedIndex] = updatedItem
                    } else if (newList.isEmpty()) {
                        newList.add(updatedItem)
                    }

                    val finalConfig = config.copy(
                        items = newList,
                        currentIndex = selectedIndex.coerceIn(0, maxOf(newList.size - 1, 0))
                    )

                    if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        PhotosStorageManager.saveConfig(this@PhotosConfigActivity, widgetId, finalConfig)
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

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0C0C0E))
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .imePadding(),
                    color = Color(0xFF0C0C0E)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
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

                        // Main Scrollable Content
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // 2. Photo Preview & Picker Card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFF16161B))
                                    .border(1.dp, Color(0xFF282830), RoundedCornerShape(18.dp))
                                    .clickable {
                                        photoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val bmp = remember(currentImagePath) {
                                    currentImagePath?.let {
                                        val f = File(it)
                                        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
                                    }
                                }

                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Selected Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
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
                                            text = "Supports JPEG, PNG, HEIC from Gallery",
                                            color = Color(0xFF8E8E93),
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                // Overlay "Change" Pill
                                if (bmp != null) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(12.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.Black.copy(alpha = 0.65f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Change Photo",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // 3. Multi-Photo Thumbnail Bar
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "PHOTOS IN WIDGET (${config.items.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF8E8E93),
                                        letterSpacing = 0.5.sp
                                    )

                                    if (config.items.size < 6) {
                                        Text(
                                            text = "+ Add Slot",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = accentColor,
                                            modifier = Modifier.clickable {
                                                val newItem = SlateMemoryItem(
                                                    id = System.currentTimeMillis().toString(),
                                                    caption = "New Memory",
                                                    dateText = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
                                                )
                                                val updated = config.items + newItem
                                                config = config.copy(items = updated)
                                                selectedIndex = updated.size - 1
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
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF1C1C22))
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) accentColor else Color(0xFF2C2C35),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    // Flush current inputs to current item before switching
                                                    val updatedItem = activeItem.copy(
                                                        imagePath = currentImagePath,
                                                        caption = caption,
                                                        dateText = dateText,
                                                        location = location,
                                                        filterStyle = filterStyle
                                                    )
                                                    val curList = config.items.toMutableList()
                                                    if (selectedIndex in curList.indices) curList[selectedIndex] = updatedItem
                                                    config = config.copy(items = curList)
                                                    selectedIndex = idx
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
                                                    color = if (isSelected) accentColor else Color(0xFF8E8E93),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }

                                            // Delete icon if more than 1 item
                                            if (config.items.size > 1 && isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(2.dp)
                                                        .size(20.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Black.copy(alpha = 0.7f))
                                                        .clickable {
                                                            val curList = config.items.toMutableList()
                                                            curList.removeAt(idx)
                                                            config = config.copy(items = curList)
                                                            selectedIndex = 0.coerceAtMost(curList.size - 1)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Remove",
                                                        tint = Color(0xFFFF453A),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. Memory Details: Caption, Date, Location
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "MEMORY DETAILS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF8E8E93),
                                    letterSpacing = 0.5.sp
                                )

                                // Caption field
                                OutlinedTextField(
                                    value = caption,
                                    onValueChange = { caption = it },
                                    label = { Text("Caption / Title") },
                                    placeholder = { Text("e.g. Summer Memories") },
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

                                // Date field
                                OutlinedTextField(
                                    value = dateText,
                                    onValueChange = { dateText = it },
                                    label = { Text("Date Stamp") },
                                    placeholder = { Text("e.g. September 2024") },
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

                                // Location field
                                OutlinedTextField(
                                    value = location,
                                    onValueChange = { location = it },
                                    label = { Text("Location (Optional)") },
                                    placeholder = { Text("e.g. Pacific Coast, California") },
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

                            // 5. Aesthetic Visual Filters
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
                                            onClick = { filterStyle = style },
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
                        }
                    }
                }
            }
        }
    }
}
