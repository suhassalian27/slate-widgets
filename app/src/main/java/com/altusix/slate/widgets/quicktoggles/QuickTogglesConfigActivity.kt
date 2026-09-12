package com.altusix.slate.widgets.quicktoggles

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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

class QuickTogglesConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("slate_toggles_prefs", Context.MODE_PRIVATE)
        val themePrefs = ThemePreferences(this).getThemeSettings()
        val accentColor = themePrefs.accentColor

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0C0C0E),
                    surface = Color(0xFF16161B)
                )
            ) {
                var hapticFeedback by remember {
                    mutableStateOf(prefs.getBoolean("haptic_feedback", true))
                }

                // Check permissions dynamically
                val canWriteSettings by rememberUpdatedState(
                    Settings.System.canWrite(this@QuickTogglesConfigActivity)
                )

                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                val hasDndAccess by rememberUpdatedState(
                    nm?.isNotificationPolicyAccessGranted == true
                )

                fun saveAndFinish() {
                    prefs.edit().putBoolean("haptic_feedback", hapticFeedback).apply()
                    updateAllQuickTogglesWidgets(this@QuickTogglesConfigActivity)
                    setResult(Activity.RESULT_OK)
                    finish()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0A0C)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        // Top App Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = "Quick Toggles Studio",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            IconButton(
                                onClick = { saveAndFinish() },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Done",
                                    tint = Color.Black
                                )
                            }
                        }

                        // Settings Content
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Hardware Permissions & Integrations",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "Grant permissions below to enable direct in-place toggling for Auto-Rotate and Do Not Disturb without navigating away from the home screen.",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )

                            // Permission 1: System Write Settings (Auto-Rotate & Screen Timeout)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF16161B))
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Modify System Settings",
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (canWriteSettings) "Granted (Direct 1-tap rotation & timeout)" else "Required for direct rotation & timeout",
                                        color = if (canWriteSettings) Color(0xFF30D158) else Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                                Button(
                                    onClick = {
                                        try {
                                            startActivity(QuickTogglesStateManager.createWriteSettingsIntent())
                                        } catch (_: Exception) {}
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (canWriteSettings) Color(0xFF2C2C34) else accentColor
                                    )
                                ) {
                                    Text(
                                        text = if (canWriteSettings) "Manage" else "Grant",
                                        color = if (canWriteSettings) Color.White else Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Permission 2: Do Not Disturb Policy Access
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF16161B))
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Do Not Disturb Access",
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (hasDndAccess) "Granted (Full Silent & DND modes enabled)" else "Required to enter full Silent mode",
                                        color = if (hasDndAccess) Color(0xFF30D158) else Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                                Button(
                                    onClick = {
                                        try {
                                            startActivity(QuickTogglesStateManager.createNotificationPolicyIntent())
                                        } catch (_: Exception) {}
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (hasDndAccess) Color(0xFF2C2C34) else accentColor
                                    )
                                ) {
                                    Text(
                                        text = if (hasDndAccess) "Manage" else "Grant",
                                        color = if (hasDndAccess) Color.White else Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Tactile Behavior",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )

                            // Haptic Feedback Switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF16161B))
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Haptic Vibration on Tap",
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Vibrates gently when toggling Alert Sliders, Torch, or Sound modes",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                                Switch(
                                    checked = hapticFeedback,
                                    onCheckedChange = { hapticFeedback = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = accentColor
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
