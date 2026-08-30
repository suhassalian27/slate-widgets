package com.altusix.slate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.core.theme.SlateThemeSettings
import com.altusix.slate.ui.components.SlateWidgetPreviewImage
import com.altusix.slate.widgets.ai.getAiWidgetsCatalog
import com.altusix.slate.widgets.appfolder.getAppFolderWidgetsCatalog
import com.altusix.slate.widgets.applauncher.getAppLauncherWidgetsCatalog
import com.altusix.slate.widgets.battery.getBatteryWidgetsCatalog
import com.altusix.slate.widgets.bluetooth.getBluetoothWidgetsCatalog
import com.altusix.slate.widgets.calculator.getCalculatorWidgetsCatalog
import com.altusix.slate.widgets.calendar.getCalendarWidgetsCatalog
import com.altusix.slate.widgets.camera.getCameraWidgetsCatalog
import com.altusix.slate.widgets.clock.analog.getClockAnalogWidgetsCatalog
import com.altusix.slate.widgets.clock.digital.getClockDigitalWidgetsCatalog
import com.altusix.slate.widgets.clock.hybrid.getClockHybridWidgetsCatalog
import com.altusix.slate.widgets.compass.getCompassWidgetsCatalog
import com.altusix.slate.widgets.contacts.getContactsWidgetsCatalog
import com.altusix.slate.widgets.deviceinfo.getDeviceInfoWidgetsCatalog

private fun isFullWidthWidget(sizeText: String): Boolean {
    val clean = sizeText.lowercase()
    return clean.startsWith("5x") || clean.startsWith("4x") || clean == "3x2" || clean == "3x3"
}

@Composable
fun WidgetListScreen(
    themeSettings: SlateThemeSettings,
    onWidgetSelect: (SlateWidgetInfo) -> Unit
) {
    val aiWidgets = remember { getAiWidgetsCatalog() }
    val batteryWidgets = remember { getBatteryWidgetsCatalog() }
    val appFolderWidgets = remember { getAppFolderWidgetsCatalog() }
    val appLauncherWidgets = remember { getAppLauncherWidgetsCatalog() }
    val bluetoothWidgets = remember { getBluetoothWidgetsCatalog() }
    val calculatorWidgets = remember { getCalculatorWidgetsCatalog() }
    val calendarWidgets = remember { getCalendarWidgetsCatalog() }
    val clockWidgets = remember { getClockAnalogWidgetsCatalog() }
    val clockDigitalWidgets = remember { getClockDigitalWidgetsCatalog() }
    val clockHybridWidgets = remember { getClockHybridWidgetsCatalog() }
    val cameraWidgets = remember { getCameraWidgetsCatalog() }
    val compassWidgets = remember { getCompassWidgetsCatalog() }
    val contactsWidgets = remember { getContactsWidgetsCatalog() }
    val deviceInfoWidgets = remember { getDeviceInfoWidgetsCatalog() }

    val categories = remember {
        listOf(
            "All", "AI", "App Folder", "App Launcher", "Battery", "Bluetooth",
            "Calculator", "Calendar", "Camera", "Clock - Analog", "Clock - Digital",
            "Clock - Hybrid", "Compass", "Contacts", "Device"
        )
    }

    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    val displayedWidgets = when (selectedCategoryIndex) {
        1 -> aiWidgets
        2 -> appFolderWidgets
        3 -> appLauncherWidgets
        4 -> batteryWidgets
        5 -> bluetoothWidgets
        6 -> calculatorWidgets
        7 -> calendarWidgets
        8 -> cameraWidgets
        9 -> clockWidgets
        10 -> clockDigitalWidgets
        11 -> clockHybridWidgets
        12 -> compassWidgets
        13 -> contactsWidgets
        14 -> deviceInfoWidgets
        else -> aiWidgets + appFolderWidgets + appLauncherWidgets + batteryWidgets + bluetoothWidgets +
                calculatorWidgets + calendarWidgets + cameraWidgets + clockWidgets +
                clockDigitalWidgets + clockHybridWidgets + compassWidgets + contactsWidgets + deviceInfoWidgets
    }

    val widgetSpans = remember(displayedWidgets) {
        val spans = IntArray(displayedWidgets.size)
        var i = 0
        while (i < displayedWidgets.size) {
            if (isFullWidthWidget(displayedWidgets[i].sizeText)) {
                spans[i] = 6
                i++
            } else {
                var j = i
                while (j < displayedWidgets.size) {
                    if (isFullWidthWidget(displayedWidgets[j].sizeText)) break
                    j++
                }
                val count = j - i
                if (count == 1) {
                    spans[i] = 6
                } else if (count % 2 == 0) {
                    for (k in i until j) {
                        spans[k] = 3
                    }
                } else {
                    val twoPerRowEnd = j - 3
                    for (k in i until twoPerRowEnd) {
                        spans[k] = 3
                    }
                    for (k in twoPerRowEnd until j) {
                        spans[k] = 2
                    }
                }
                i = j
            }
        }
        spans
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .statusBarsPadding()
    ) {
        Text(
            text = "Widgets",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 18.dp)
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
            columns = GridCells.Fixed(6),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(
                items = displayedWidgets,
                span = { index, _ ->
                    val spanValue = if (index < widgetSpans.size) widgetSpans[index] else 3
                    GridItemSpan(spanValue)
                }
            ) { index, widget ->
                val span = if (index < widgetSpans.size) widgetSpans[index] else 3
                SleekWidgetCard(
                    widgetInfo = widget,
                    themeSettings = themeSettings,
                    span = span
                ) {
                    onWidgetSelect(widget)
                }
            }
        }
    }
}

@Composable
fun SleekWidgetCard(
    widgetInfo: SlateWidgetInfo,
    themeSettings: SlateThemeSettings,
    span: Int = 3,
    onClick: () -> Unit
) {
    val outerShape = RoundedCornerShape(14.dp)
    val innerShape = RoundedCornerShape(10.dp)

    val previewHeightModifier = when (widgetInfo.sizeText.lowercase()) {
        "5x1", "4x1" -> Modifier.fillMaxWidth().height(88.dp)
        "5x2", "4x2" -> Modifier.fillMaxWidth().height(154.dp)
        "3x2", "3x3" -> Modifier.fillMaxWidth().height(134.dp)
        else -> Modifier.fillMaxWidth().aspectRatio(1.0f)
    }

    val previewPadding = if (span == 2) 10.dp else 22.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(outerShape)
            .background(Color(0xFF111111))
            .clickable { onClick() }
            .padding(0.dp)
    ) {
        BoxWithConstraints(
            modifier = previewHeightModifier
                .clip(innerShape)
                .background(Color(0xFF41434B)),
            contentAlignment = Alignment.Center
        ) {
            val boxWidthDp = (maxWidth.value - (previewPadding.value * 2)).toInt().coerceAtLeast(60)
            val boxHeightDp = (maxHeight.value - (previewPadding.value * 2)).toInt().coerceAtLeast(60)

            SlateWidgetPreviewImage(
                widgetInfo = widgetInfo,
                themeSettings = themeSettings,
                targetWidthDp = boxWidthDp,
                targetHeightDp = boxHeightDp,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(previewPadding)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = widgetInfo.name,
                    color = Color.White,
                    fontSize = if (span == 2) 11.sp else 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = widgetInfo.sizeText.replace('x', '×'),
                        color = Color(0xFFA0A5B5),
                        fontSize = if (span == 2) 9.sp else 11.sp,
                        fontWeight = FontWeight.Normal
                    )

                    if (widgetInfo.hasModeOption) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF2C2D35))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "DUAL",
                                color = Color(0xFFB0B5C2),
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = getOutlinedStarIcon(),
                contentDescription = "Pin Widget",
                tint = Color.White,
                modifier = Modifier.size(if (span == 2) 18.dp else 22.dp)
            )
        }
    }
}

@Composable
private fun getOutlinedStarIcon(): ImageVector {
    return remember {
        ImageVector.Builder(
            name = "OutlinedStar",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f
        ) {
            moveTo(12f, 2f)
            lineTo(15.09f, 8.26f)
            lineTo(22f, 9.27f)
            lineTo(17f, 14.14f)
            lineTo(18.18f, 21.02f)
            lineTo(12f, 17.77f)
            lineTo(5.82f, 21.02f)
            lineTo(7f, 14.14f)
            lineTo(2f, 9.27f)
            lineTo(8.91f, 8.26f)
            close()
        }.build()
    }
}