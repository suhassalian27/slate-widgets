package com.altusix.slate.ui.config

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.altusix.slate.data.local.SlateDataStore
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.widgets.battery.ArcGaugeBatteryWidget
import com.altusix.slate.widgets.battery.EditorialStatsBatteryWidget
import com.altusix.slate.widgets.battery.HorizontalBatteryWidget
import com.altusix.slate.widgets.battery.MinimalBatteryWidget
import com.altusix.slate.widgets.battery.MultiDeviceBatteryWidget
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var dataStore: SlateDataStore
    private var widgetClassName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        dataStore = SlateDataStore(this)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Detect which receiver launched this config screen
        val widgetInfo = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)
        widgetClassName = widgetInfo?.provider?.className ?: ""

        setContent {
            SlateConfigTheme {
                var themeMode by remember { mutableStateOf("DARK") }
                var opacity by remember { mutableFloatStateOf(1.0f) }
                var selectedAccentHex by remember { mutableLongStateOf(0xFF00D166L) }

                val currentBgHex = if (themeMode == "LIGHT") 0xFFFFFFFFL else 0xFF161618L

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0D0D0E))
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Customize Widget",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Dynamic Live Preview Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF161618)),
                        contentAlignment = Alignment.Center
                    ) {
                        val previewBg = Color(currentBgHex).copy(alpha = opacity)
                        val textColor = if (themeMode == "LIGHT") Color.Black else Color.White

                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(previewBg)
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(22.dp))
                                .padding(14.dp)
                        ) {
                            if (widgetClassName.contains("ArcGaugeBatteryReceiver")) {
                                // Arc Gauge Preview
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "BATTERY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f))
                                        Text(text = "CHARGING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(selectedAccentHex))
                                    }

                                    val arcBitmap = remember(selectedAccentHex, themeMode) {
                                        generateArcGaugeBitmapPreview(85, Color(selectedAccentHex), textColor.copy(alpha = 0.15f))
                                    }

                                    Image(
                                        bitmap = arcBitmap.asImageBitmap(),
                                        contentDescription = "Arc Preview",
                                        modifier = Modifier.size(100.dp, 50.dp)
                                    )

                                    Text(text = "85%", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = textColor)
                                }
                            } else {
                                // Default Standard Battery Tile Preview
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "BATTERY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f))
                                        Text(text = "CHARGING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(selectedAccentHex))
                                    }
                                    Text(text = "85%", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = textColor)
                                    LinearProgressIndicator(
                                        progress = { 0.85f },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                        color = Color(selectedAccentHex),
                                        trackColor = textColor.copy(alpha = 0.15f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Style Mode (Dark / Light)
                    Text(text = "Style Mode", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF161618))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("DARK" to "Dark", "LIGHT" to "Light").forEach { (mode, label) ->
                            val isSelected = themeMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF2C2C30) else Color.Transparent)
                                    .clickable { themeMode = mode },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Accent Colors
                    Text(text = "Accent Color", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(0xFF00D166L, 0xFF2B80FFL, 0xFFFF3B30L, 0xFFFF9500L, 0xFFAF52DEL).forEach { hex ->
                            val isSelected = selectedAccentHex == hex
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .border(if (isSelected) 2.5.dp else 0.dp, Color.White, CircleShape)
                                    .clickable { selectedAccentHex = hex }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Opacity Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Opacity", color = Color.Gray, fontSize = 13.sp)
                        Text(text = "${(opacity * 100).toInt()}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = opacity,
                        onValueChange = { opacity = it },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color(0xFF2C2C30)
                        )
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Apply Button
                    Button(
                        onClick = {
                            saveAndFinish(
                                SlateWidgetConfig(
                                    themeMode = themeMode,
                                    backgroundColorHex = currentBgHex,
                                    opacity = opacity,
                                    accentColorHex = selectedAccentHex
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text(text = "Apply Widget", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    private fun saveAndFinish(config: SlateWidgetConfig) {
        lifecycleScope.launch {
            try {
                dataStore.saveWidgetConfig(appWidgetId, config)

                val glanceManager = GlanceAppWidgetManager(this@WidgetConfigActivity)
                val glanceId = try {
                    glanceManager.getGlanceIdBy(intent)
                } catch (e: Exception) {
                    try {
                        glanceManager.getGlanceIdBy(appWidgetId)
                    } catch (e2: Exception) {
                        null
                    }
                }

                // Dynamically trigger update on the exact widget type that opened config
                val widget = when {
                    widgetClassName.contains("ArcGaugeBatteryReceiver") -> ArcGaugeBatteryWidget()
                    widgetClassName.contains("MultiDeviceBatteryReceiver") -> MultiDeviceBatteryWidget()
                    widgetClassName.contains("HorizontalBatteryReceiver") -> HorizontalBatteryWidget()
                    widgetClassName.contains("EditorialStatsBatteryReceiver") -> EditorialStatsBatteryWidget()
                    else -> MinimalBatteryWidget()
                }

                if (glanceId != null) {
                    widget.update(this@WidgetConfigActivity, glanceId)
                } else {
                    widget.updateAll(this@WidgetConfigActivity)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                val resultValue = Intent().apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                setResult(Activity.RESULT_OK, resultValue)
                finish()
            }
        }
    }

    private fun generateArcGaugeBitmapPreview(percentage: Int, accentColor: Color, trackColor: Color): Bitmap {
        val bitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val strokeWidth = 32f
        val padding = strokeWidth / 2f + 4f
        val rectF = RectF(padding, padding, 200f - padding, 200f - padding)

        val trackPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            color = trackColor.toArgb()
        }
        val activePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            color = accentColor.toArgb()
        }

        canvas.drawArc(rectF, 210f, 120f, false, trackPaint)
        canvas.drawArc(rectF, 210f, (percentage / 100f) * 120f, false, activePaint)
        return bitmap
    }
}

@Composable
fun SlateConfigTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(background = Color(0xFF0D0D0E), surface = Color(0xFF161618)),
        content = content
    )
}