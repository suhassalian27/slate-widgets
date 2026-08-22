package com.altusix.slate

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.widgets.ai.getAiWidgetsCatalog
import com.altusix.slate.widgets.applauncher.getAppLauncherWidgetsCatalog
import com.altusix.slate.widgets.battery.getBatteryWidgetsCatalog
import com.altusix.slate.widgets.bluetooth.getBluetoothWidgetsCatalog
import com.altusix.slate.widgets.calculator.getCalculatorWidgetsCatalog
import com.altusix.slate.widgets.calendar.getCalendarWidgetsCatalog
import com.altusix.slate.widgets.clock.analog.getClockAnalogWidgetsCatalog
import com.altusix.slate.widgets.clock.digital.getClockDigitalWidgetsCatalog
import com.altusix.slate.widgets.clock.hybrid.getClockHybridWidgetsCatalog
import com.altusix.slate.widgets.camera.getCameraWidgetsCatalog
import com.altusix.slate.widgets.compass.getCompassWidgetsCatalog

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val aiWidgets = getAiWidgetsCatalog()
        val batteryWidgets = getBatteryWidgetsCatalog()
        val appLauncherWidgets = getAppLauncherWidgetsCatalog()
        val bluetoothWidgets = getBluetoothWidgetsCatalog()
        val calculatorWidgets = getCalculatorWidgetsCatalog()
        val calendarWidgets = getCalendarWidgetsCatalog()
        val clockWidgets = getClockAnalogWidgetsCatalog()
        val clockDigitalWidgets = getClockDigitalWidgetsCatalog()
        val clockHybridWidgets = getClockHybridWidgetsCatalog()
        val cameraWidgets = getCameraWidgetsCatalog()
        val compassWidgets = getCompassWidgetsCatalog()

        val categories = listOf("All", "AI", "Battery", "App Launcher", "Bluetooth", "Calculator", "Calendar", "Camera", "Clock - Analog", "Clock - Digital", "Clock - Hybrid", "Compass")

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF000000),
                    surface = Color(0xFF141416)
                )
            ) {
                var selectedCategoryIndex by remember { mutableIntStateOf(0) }
                var pendingWidgetInfo by remember { mutableStateOf<SlateWidgetInfo?>(null) }

                val displayedWidgets = when (selectedCategoryIndex) {
                    1 -> aiWidgets
                    2 -> batteryWidgets
                    3 -> appLauncherWidgets
                    4 -> bluetoothWidgets
                    5 -> calculatorWidgets
                    6 -> calendarWidgets
                    7 -> cameraWidgets
                    8 -> clockWidgets
                    9 -> clockDigitalWidgets
                    10 -> clockHybridWidgets
                    11 -> compassWidgets
                    else -> aiWidgets + batteryWidgets + appLauncherWidgets + bluetoothWidgets + calculatorWidgets + calendarWidgets + cameraWidgets + clockWidgets + clockDigitalWidgets + clockHybridWidgets + compassWidgets
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF000000))
                ) {
                    Text(
                        text = "Widgets",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 14.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(categories) { index, title ->
                            val isSelected = selectedCategoryIndex == index
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else Color(0xFF1C1C1E))
                                    .clickable { selectedCategoryIndex = index }
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSelected) Color.Black else Color(0xFF8E8E93),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = displayedWidgets,
                            span = { widget ->
                                val isFullWidth = widget.sizeText.startsWith("4x") || widget.sizeText == "3x2"
                                if (isFullWidth) GridItemSpan(2) else GridItemSpan(1)
                            }
                        ) { widget ->
                            SleekWidgetCard(widgetInfo = widget) {
                                if (widget.hasModeOption) {
                                    pendingWidgetInfo = widget
                                } else {
                                    pinWidgetToHomeScreen(this@MainActivity, widget.receiverClass)
                                }
                            }
                        }
                    }
                }

                // WIDGET MODE BOTTOM SHEET (Displays BEFORE system pinning dialog)
                if (pendingWidgetInfo != null) {
                    WidgetModeBottomSheet(
                        widgetInfo = pendingWidgetInfo!!,
                        onDismiss = { pendingWidgetInfo = null },
                        onModeSelected = { isResponsive ->
                            val widget = pendingWidgetInfo!!
                            pendingWidgetInfo = null

                            // Save default responsive preference so the new widget instance reads it
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
                    this.data = Uri.parse("package:$packageName")
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Choose Widget Mode", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = "Pick whichever works best on your HomeScreen.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier.weight(1f).height(110.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF242428)).clickable { onModeSelected(true) }.padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Responsive", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = "Adapts to cell size", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                Box(
                    modifier = Modifier.weight(1f).height(110.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF242428)).clickable { onModeSelected(false) }.padding(16.dp),
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

@Composable
fun SleekWidgetCard(widgetInfo: SlateWidgetInfo, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        val previewModifier = when (widgetInfo.sizeText) {
            "4x1" -> Modifier.fillMaxWidth().height(68.dp)
            "4x2" -> Modifier.fillMaxWidth().height(136.dp)
            "3x2" -> Modifier.fillMaxWidth().height(116.dp)
            "1x2" -> Modifier.fillMaxWidth().aspectRatio(0.58f)
            "2x1" -> Modifier.fillMaxWidth().aspectRatio(2.0f)
            else -> Modifier.fillMaxWidth().aspectRatio(1.0f)
        }
        Box(
            modifier = previewModifier.clip(RoundedCornerShape(22.dp)).background(Color(0xFF141416)).border(1.dp, Color(0xFF242428), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.align(Alignment.TopStart).padding(10.dp).clip(CircleShape).background(Color(0xFF222226)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text(text = widgetInfo.sizeText, color = Color(0xFF8E8E93), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp).clip(CircleShape).background(Color(0xFF222226)).border(0.5.dp, Color(0xFF323238), CircleShape), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Text(text = widgetInfo.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = widgetInfo.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 2.dp))
        Text(text = widgetInfo.category, color = Color(0xFF636366), fontSize = 11.sp, modifier = Modifier.padding(start = 2.dp))
    }
}