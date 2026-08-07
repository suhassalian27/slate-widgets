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
import com.altusix.slate.widgets.ai.*
import com.altusix.slate.widgets.battery.*

data class SlateWidgetInfo(
    val name: String,
    val sizeText: String,
    val category: String,
    val receiverClass: Class<*>
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val aiWidgets = listOf(
            SlateWidgetInfo("Gemini", "2x2", "AI", GeminiTextReceiver::class.java),
            SlateWidgetInfo("ChatGPT Text", "2x2", "AI", ChatGptTextReceiver::class.java),
            SlateWidgetInfo("ChatGPT Voice", "2x2", "AI", ChatGptVoiceReceiver::class.java),
            SlateWidgetInfo("Claude", "2x2", "AI", ClaudeReceiver::class.java),
            SlateWidgetInfo("Grok", "2x2", "AI", GrokReceiver::class.java),
            SlateWidgetInfo("Perplexity", "2x2", "AI", PerplexityReceiver::class.java),
            SlateWidgetInfo("DeepSeek", "2x2", "AI", DeepSeekReceiver::class.java),
            SlateWidgetInfo("Copilot", "2x2", "AI", CopilotReceiver::class.java),
            SlateWidgetInfo("Meta AI", "2x2", "AI", MetaAiReceiver::class.java),
            SlateWidgetInfo("AI Primary Bar", "4x1", "AI", AiBarPrimaryReceiver::class.java),
            SlateWidgetInfo("AI Dock Bar", "4x1", "AI", AiBarDock5Receiver::class.java),
            SlateWidgetInfo("AI Capsule Bar", "4x1", "AI", AiBarCapsuleReceiver::class.java),
            SlateWidgetInfo("AI Dual Flagship Bar", "4x1", "AI", AiBarDualFlagshipReceiver::class.java),
            SlateWidgetInfo("AI Quad Folder", "2x2", "AI", AiFolder4ClassicReceiver::class.java),
            SlateWidgetInfo("AI Bento Folder", "4x2", "AI", AiFolder6BentoHeroReceiver::class.java),
            SlateWidgetInfo("AI Side Bento Folder", "4x2", "AI", AiFolder8BentoSideReceiver::class.java),
            SlateWidgetInfo("AI 3x3 Grid Folder", "2x2", "AI", AiFolder9GridReceiver::class.java),
            SlateWidgetInfo("AI Mega Folder", "4x2", "AI", AiFolder10MegaReceiver::class.java),
            SlateWidgetInfo("AI Asymmetric Bento", "3x2", "AI", AiFolder7AsymmetricReceiver::class.java)
        )

        val batteryWidgets = listOf(
            SlateWidgetInfo("Minimal Tile", "2x2", "Battery", MinimalBatteryReceiver::class.java),
            SlateWidgetInfo("Arc Battery", "2x2", "Battery", ArcGaugeBatteryReceiver::class.java),
            SlateWidgetInfo("Editorial", "2x2", "Battery", EditorialStatsBatteryReceiver::class.java),
            SlateWidgetInfo("Dot Level Tile", "2x2", "Battery", DotLevelMeterReceiver::class.java),
            SlateWidgetInfo("Multi-Device", "4x2", "Battery", MultiDeviceBatteryReceiver::class.java),
            SlateWidgetInfo("Dot Matrix LED", "4x2", "Battery", DotMatrixBatteryLEDReceiver::class.java),
            SlateWidgetInfo("Dot Level Wide", "4x2", "Battery", DotLevelMeterWideReceiver::class.java),
            SlateWidgetInfo("Battery Strip", "4x1", "Battery", HorizontalBatteryReceiver::class.java),
            SlateWidgetInfo("5-Pill Gauge", "2x2", "Battery", SegmentedPillBatteryReceiver::class.java),
            SlateWidgetInfo("Pixel Heart", "2x2", "Battery", PixelHeartBatteryReceiver::class.java),
            SlateWidgetInfo("Lightning Bolt", "2x2", "Battery", LightningBoltBatteryReceiver::class.java),
            SlateWidgetInfo("Circular Dial", "2x2", "Battery", CircularRingBatteryReceiver::class.java),
            SlateWidgetInfo("Vertical Pill", "1x2", "Battery", VerticalBatteryPillReceiver::class.java),
            SlateWidgetInfo("Horizontal Pill", "2x1", "Battery", HorizontalBatteryPillReceiver::class.java)
        )

        val categories = listOf("All", "AI", "Battery")

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF000000),
                    surface = Color(0xFF141416)
                )
            ) {
                var selectedCategoryIndex by remember { mutableIntStateOf(0) }

                val displayedWidgets = when (selectedCategoryIndex) {
                    1 -> aiWidgets
                    2 -> batteryWidgets
                    else -> aiWidgets + batteryWidgets
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF000000))
                ) {
                    // Title Header
                    Text(
                        text = "Widgets",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 14.dp)
                    )

                    // Filter Pills
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

                    // Borderless Surface Grid
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
                                pinWidgetToHomeScreen(this@MainActivity, widget.receiverClass)
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
fun SleekWidgetCard(widgetInfo: SlateWidgetInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        // Hero Surface Card - Direct 1-layer geometry
        val previewModifier = when (widgetInfo.sizeText) {
            "4x1" -> Modifier.fillMaxWidth().height(68.dp)
            "4x2" -> Modifier.fillMaxWidth().height(136.dp)
            "3x2" -> Modifier.fillMaxWidth().height(116.dp)
            "1x2" -> Modifier.fillMaxWidth().aspectRatio(0.58f)
            "2x1" -> Modifier.fillMaxWidth().aspectRatio(2.0f)
            else -> Modifier.fillMaxWidth().aspectRatio(1.0f)
        }

        Box(
            modifier = previewModifier
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF141416))
                .border(1.dp, Color(0xFF242428), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Size Badge Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222226))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = widgetInfo.sizeText,
                    color = Color(0xFF8E8E93),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Quick Pin Action Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222226))
                    .border(0.5.dp, Color(0xFF323238), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Widget",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Preview Text
            Text(
                text = widgetInfo.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Clean External Metadata Labels
        Text(
            text = widgetInfo.name,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 2.dp)
        )
        Text(
            text = widgetInfo.category,
            color = Color(0xFF636366),
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}