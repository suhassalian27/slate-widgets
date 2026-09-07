package com.altusix.slate.widgets.notes

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

        val existingNote = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            NotesStorageManager.getNoteForWidget(this, widgetId)
        } else {
            SlateNoteData.getDefaultNote()
        }

        val themePrefs = ThemePreferences(this).getThemeSettings()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0C0B0E),
                    surface = Color(0xFF16151A)
                )
            ) {
                var title by remember { mutableStateOf(existingNote.title) }
                var content by remember { mutableStateOf(existingNote.content) }
                var category by remember { mutableStateOf(existingNote.category) }
                var isChecklistMode by remember { mutableStateOf(existingNote.items.isNotEmpty()) }
                var checklistItems by remember { mutableStateOf(existingNote.items) }
                var newItemText by remember { mutableStateOf("") }

                val accentColor = themePrefs.accentColor

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0A0A0C))
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        // Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Edit Slate Note",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = { finish() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1C1C20))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Category Chips Row
                        val categories = listOf("Memo", "Focus", "Checklist", "Ideas", "Log")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categories.forEach { cat ->
                                val isSelected = category.equals(cat, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) accentColor else Color(0xFF1C1C20))
                                        .clickable { category = cat }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        color = if (isSelected) Color.Black else Color(0xFF8E8E93),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Title Input
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Note Title", color = Color(0xFF8E8E93)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color(0xFF2C2C30),
                                focusedContainerColor = Color(0xFF141418),
                                unfocusedContainerColor = Color(0xFF141418)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Mode Switcher (Notes vs Checklist)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF141418))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (!isChecklistMode) Color(0xFF24242A) else Color.Transparent)
                                    .clickable { isChecklistMode = false }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Text Note",
                                    color = if (!isChecklistMode) Color.White else Color(0xFF8E8E93),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isChecklistMode) Color(0xFF24242A) else Color.Transparent)
                                    .clickable { isChecklistMode = true }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Checklist (${checklistItems.size})",
                                    color = if (isChecklistMode) Color.White else Color(0xFF8E8E93),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Body Content or Checklist Column
                        if (!isChecklistMode) {
                            OutlinedTextField(
                                value = content,
                                onValueChange = { content = it },
                                label = { Text("Note Content", color = Color(0xFF8E8E93)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = Color(0xFF2C2C30),
                                    focusedContainerColor = Color(0xFF141418),
                                    unfocusedContainerColor = Color(0xFF141418)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                maxLines = 12
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                // Add item field
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = newItemText,
                                        onValueChange = { newItemText = it },
                                        placeholder = { Text("Add task item...", color = Color(0xFF6C6C70)) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = accentColor,
                                            unfocusedBorderColor = Color(0xFF2C2C30),
                                            focusedContainerColor = Color(0xFF141418),
                                            unfocusedContainerColor = Color(0xFF141418)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
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
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(accentColor)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Black)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Checklist List
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(checklistItems) { index, item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF141418))
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Checkbox box
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
                                                    Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.Black, modifier = Modifier.size(16.dp))
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Text(
                                                text = item.text,
                                                color = if (item.isDone) Color(0xFF6C6C70) else Color.White,
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
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFF8E8E93), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Save Button
                        Button(
                            onClick = {
                                val updatedNote = SlateNoteData(
                                    id = existingNote.id,
                                    title = if (title.isBlank()) "Untitled" else title.trim(),
                                    content = content.trim(),
                                    items = checklistItems,
                                    category = category,
                                    updatedAt = System.currentTimeMillis()
                                )

                                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                                    NotesStorageManager.saveNoteForWidget(this@NoteEditActivity, widgetId, updatedNote)
                                }
                                updateAllNotesWidgets(this@NoteEditActivity)
                                setResult(Activity.RESULT_OK)
                                finish()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                text = "Save Note",
                                color = Color.Black,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
