package com.altusix.slate.ui.config

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
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
import androidx.lifecycle.lifecycleScope
import com.altusix.slate.data.local.SlateDataStore
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.widgets.battery.updateAllBatteryWidgets
import com.altusix.slate.widgets.ai.updateAllAiWidgets
import com.altusix.slate.widgets.ai.updateAllAiFolderWidgets
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll

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

        val widgetInfo = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)
        widgetClassName = widgetInfo?.provider?.className ?: ""

        setContent {
            SlateConfigTheme {
                var themeMode by remember { mutableStateOf("DARK") }
                var opacity by remember { mutableFloatStateOf(1.0f) }
                var selectedAccentHex by remember { mutableLongStateOf(0xFF00D166L) }

                val currentBgHex = if (themeMode == "LIGHT") 0xFFFFFFFFL else 0xFF161618L

                LaunchedEffect(appWidgetId) {
                    val savedConfig = dataStore.getWidgetConfig(appWidgetId).firstOrNull()
                    if (savedConfig != null) {
                        themeMode = savedConfig.themeMode
                        opacity = savedConfig.opacity
                        selectedAccentHex = savedConfig.accentColorHex
                    }
                }

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
        val appCtx = applicationContext
        val targetWidgetClass = widgetClassName
        val targetAppWidgetId = appWidgetId

        // 1. Launch in the IO dispatcher to handle background polling and delays
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // 2. Save config to DataStore. This must complete first.
                dataStore.saveWidgetConfig(targetAppWidgetId, config)

                // 3. Send the broadcast. For brand new widgets, this forces Glance
                // to wake up, intercept the ID, and write it to its internal database.
                if (targetWidgetClass.isNotEmpty()) {
                    val updateIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                        component = ComponentName(appCtx, targetWidgetClass)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(targetAppWidgetId))
                    }
                    appCtx.sendBroadcast(updateIntent)
                }

                // 4. Poll for the GlanceId.
                // For existing widgets, this succeeds instantly on attempt 1.
                // For new widgets, it gives Glance the few milliseconds it needs to process the broadcast.
                var mappedGlanceId: androidx.glance.GlanceId? = null
                val glanceManager = GlanceAppWidgetManager(appCtx)

                for (attempt in 1..15) {
                    try {
                        mappedGlanceId = glanceManager.getGlanceIdBy(targetAppWidgetId)
                        break // We successfully found the mapped ID, break out of the loop
                    } catch (e: IllegalArgumentException) {
                        // Not mapped yet. Wait 100ms and try again.
                        kotlinx.coroutines.delay(100L)
                    }
                }

                // 5. Force a synchronous Compose update.
                // .update() is a suspend function. It will completely block this coroutine
                // until Glance has fully built the UI and pushed it to the system AppWidgetManager.
                if (mappedGlanceId != null && targetWidgetClass.isNotEmpty()) {
                    val receiverClass = Class.forName(targetWidgetClass)
                    val receiverInstance = receiverClass.getDeclaredConstructor().newInstance()
                    if (receiverInstance is GlanceAppWidgetReceiver) {
                        receiverInstance.glanceAppWidget.update(appCtx, mappedGlanceId)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // 6. We ONLY finish the activity after the RemoteViews are generated.
                // By switching to Main and finishing here, we guarantee the launcher
                // reads the fully themed widget the exact moment it hits the homescreen.
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    val resultValue = Intent().apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, targetAppWidgetId)
                    }
                    setResult(Activity.RESULT_OK, resultValue)
                    finish()
                }
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