package com.altusix.slate.widgets.notes

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altusix.slate.core.theme.ThemePreferences

class NoteEditActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val editMode = intent?.getStringExtra(BaseNotesReceiver.EXTRA_EDIT_MODE) ?: "TEXT_ONLY"

        val existingNote = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            NotesStorageManager.getNoteForWidget(this, widgetId)
        } else {
            SlateNoteData.getDefaultNote()
        }

        val themePrefs = ThemePreferences(this).getThemeSettings()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0C0C0E),
                    surface = Color(0xFF16161B)
                )
            ) {
                var title by remember { mutableStateOf(existingNote.title) }
                var content by remember { mutableStateOf(existingNote.content) }
                var category by remember { mutableStateOf(existingNote.category) }
                var fontSize by remember { mutableStateOf(existingNote.fontSize) }

                val isChecklistOnly = editMode == "CHECKLIST_ONLY"
                val isTextOnly = editMode == "TEXT_ONLY"
                var isChecklistMode by remember {
                    mutableStateOf(if (isChecklistOnly) true else if (isTextOnly) false else existingNote.items.isNotEmpty())
                }
                var checklistItems by remember { mutableStateOf(existingNote.items) }
                var newItemText by remember { mutableStateOf("") }
                var categoryMenuExpanded by remember { mutableStateOf(false) }

                val accentColor = themePrefs.accentColor

                fun saveAndFinish() {
                    val finalItems = if (!isChecklistMode) emptyList() else checklistItems
                    val updatedNote = SlateNoteData(
                        id = existingNote.id,
                        title = if (title.isBlank()) "Untitled Note" else title.trim(),
                        content = content.trim(),
                        items = finalItems,
                        category = category,
                        fontSize = fontSize,
                        updatedAt = System.currentTimeMillis()
                    )

                    if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        NotesStorageManager.saveNoteForWidget(this@NoteEditActivity, widgetId, updatedNote)
                    }
                    updateAllNotesWidgets(this@NoteEditActivity)
                    setResult(Activity.RESULT_OK)
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
                        // 1. Top Bar: Back, Clear Category Dropdown, Save Action
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
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1A1A20))
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                // High-Contrast Category Dropdown Pill
                                Box {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFF18181F))
                                            .border(1.dp, Color(0xFF262630), RoundedCornerShape(20.dp))
                                            .clickable { categoryMenuExpanded = true }
                                            .padding(horizontal = 13.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "# $category",
                                            color = accentColor,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Select Category",
                                            tint = accentColor.copy(alpha = 0.85f),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = categoryMenuExpanded,
                                        onDismissRequest = { categoryMenuExpanded = false },
                                        modifier = Modifier.background(Color(0xFF1E1E26))
                                    ) {
                                        listOf("Memo", "Focus", "Ideas", "Checklist", "Log", "Personal").forEach { cat ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        cat,
                                                        color = if (category.equals(cat, ignoreCase = true)) accentColor else Color.White,
                                                        fontWeight = if (category.equals(cat, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                onClick = {
                                                    category = cat
                                                    categoryMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Done Button
                            Button(
                                onClick = { saveAndFinish() },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Done",
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 2. Focused Document Canvas
                        if (!isChecklistMode) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 20.dp)
                            ) {
                                Spacer(modifier = Modifier.height(8.dp))

                                TextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    placeholder = {
                                        Text(
                                            "Title",
                                            color = Color(0xFF3E3E44),
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        cursorColor = accentColor
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                TextField(
                                    value = content,
                                    onValueChange = { content = it },
                                    placeholder = {
                                        Text(
                                            "Write your thoughts...",
                                            color = Color(0xFF4C4C54),
                                            fontSize = 16.sp,
                                            lineHeight = 24.sp
                                        )
                                    },
                                    textStyle = TextStyle(
                                        color = Color(0xFFE2E2E6),
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp
                                    ),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        cursorColor = accentColor
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 350.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        } else {
                            // Checklist Mode
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 20.dp)
                            ) {
                                Spacer(modifier = Modifier.height(8.dp))

                                TextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    placeholder = {
                                        Text(
                                            "Checklist Title",
                                            color = Color(0xFF3E3E44),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        cursorColor = accentColor
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF16161C))
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextField(
                                        value = newItemText,
                                        onValueChange = { newItemText = it },
                                        placeholder = { Text("Add task item...", color = Color(0xFF5A5A62), fontSize = 14.sp) },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            cursorColor = accentColor
                                        ),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )

                                    IconButton(
                                        onClick = {
                                            if (newItemText.isNotBlank()) {
                                                checklistItems = checklistItems + SlateChecklistItem(text = newItemText.trim())
                                                newItemText = ""
                                            }
                                        },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(accentColor)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add",
                                            tint = Color.Black,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    itemsIndexed(checklistItems) { index, item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF141418))
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (item.isDone) accentColor else Color(0xFF26262C))
                                                    .clickable {
                                                        val updated = checklistItems.toMutableList()
                                                        updated[index] = item.copy(isDone = !item.isDone)
                                                        checklistItems = updated
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (item.isDone) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = "Done",
                                                        tint = Color.Black,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Text(
                                                text = item.text,
                                                color = if (item.isDone) Color(0xFF5A5A62) else Color.White,
                                                fontSize = 14.sp,
                                                modifier = Modifier.weight(1f)
                                            )

                                            IconButton(
                                                onClick = {
                                                    val updated = checklistItems.toMutableList()
                                                    updated.removeAt(index)
                                                    checklistItems = updated
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = Color(0xFF7A7A82),
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Bottom Accessory Bar (Aa Typography Indicator + S/M/L)
                        Surface(
                            color = Color(0xFF121216),
                            tonalElevation = 6.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val wordCount = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
                                val lineCount = if (content.isBlank()) 0 else content.lines().size
                                Text(
                                    text = if (isChecklistMode) "${checklistItems.size} items" else "$wordCount words • $lineCount lines",
                                    color = Color(0xFF6E6E76),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                // Typography Scale Control
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Aa",
                                        color = Color(0xFF8E8E98),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E1E26))
                                            .padding(2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        listOf("SMALL" to "S", "MEDIUM" to "M", "LARGE" to "L").forEach { (key, label) ->
                                            val isSelected = fontSize == key
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isSelected) accentColor else Color.Transparent)
                                                    .clickable { fontSize = key }
                                                    .padding(horizontal = 11.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (isSelected) Color.Black else Color(0xFF8E8E93),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
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
    }
}