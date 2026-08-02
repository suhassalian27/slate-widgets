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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altusix.slate.widgets.battery.*

data class SlateWidgetInfo(
    val name: String,
    val receiverClass: Class<*>
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val batteryWidgets = listOf(
            SlateWidgetInfo("Minimal Tile", MinimalBatteryReceiver::class.java),
            SlateWidgetInfo("Arc Battery", ArcGaugeBatteryReceiver::class.java),
            SlateWidgetInfo("Editorial", EditorialStatsBatteryReceiver::class.java),
            SlateWidgetInfo("Dot Level Tile", DotLevelMeterReceiver::class.java),
            SlateWidgetInfo("Multi-Device", MultiDeviceBatteryReceiver::class.java),
            SlateWidgetInfo("Dot Matrix LED", DotMatrixBatteryLEDReceiver::class.java),
            SlateWidgetInfo("Dot Level Wide", DotLevelMeterWideReceiver::class.java),
            SlateWidgetInfo("Battery Strip", HorizontalBatteryReceiver::class.java)
        )

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(background = Color(0xFF0D0D0E))
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0D0D0E)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Slate Widgets",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        // Category Section
                        Text(
                            text = "Battery",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 3-Column Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(batteryWidgets) { widget ->
                                WidgetGridItem(widgetInfo = widget) {
                                    pinWidgetToHomeScreen(this@MainActivity, widget.receiverClass)
                                }
                            }
                        }
                    }
                }
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
                    Toast.makeText(
                        context,
                        "Pinning blocked. Please allow 'Add Home screen shortcuts' in app settings.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Toast.makeText(context, "Launcher does not support direct widget pinning.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Requires Android 8.0+", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun WidgetGridItem(widgetInfo: SlateWidgetInfo, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161618))
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = widgetInfo.name,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}