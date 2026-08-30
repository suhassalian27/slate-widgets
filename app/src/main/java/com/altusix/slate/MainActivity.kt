package com.altusix.slate

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.ui.dashboard.DashboardScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF000000),
                    surface = Color(0xFF141416)
                )
            ) {
                var pendingWidgetInfo by remember { mutableStateOf<SlateWidgetInfo?>(null) }

                DashboardScreen(
                    onWidgetSelect = { widget ->
                        if (widget.hasModeOption) {
                            pendingWidgetInfo = widget
                        } else {
                            pinWidgetToHomeScreen(this@MainActivity, widget.receiverClass)
                        }
                    }
                )

                if (pendingWidgetInfo != null) {
                    WidgetModeBottomSheet(
                        widgetInfo = pendingWidgetInfo!!,
                        onDismiss = { pendingWidgetInfo = null },
                        onModeSelected = { isResponsive ->
                            val widget = pendingWidgetInfo!!
                            pendingWidgetInfo = null

                            val prefs = getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("default_is_responsive", isResponsive).apply()

                            pinWidgetToHomeScreen(this@MainActivity, widget.receiverClass)
                        }
                    )
                }
            }
        }
    }

    private fun checkAndRequestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }

    private fun pinWidgetToHomeScreen(context: Context, receiverClass: Class<*>) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val myProvider = ComponentName(context, receiverClass)
                val success = appWidgetManager.requestPinAppWidget(myProvider, null, null)
                if (!success) {
                    Toast.makeText(context, "Pinning blocked by launcher.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetModeBottomSheet(
    widgetInfo: SlateWidgetInfo,
    onDismiss: () -> Unit,
    onModeSelected: (Boolean) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161618),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Choose Widget Mode", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = "Pick whichever works best on your HomeScreen.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF242428))
                        .clickable { onModeSelected(true) }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Responsive", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = "Adapts to cell size", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF242428))
                        .clickable { onModeSelected(false) }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Fixed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = "Native 1:1 ratio", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}