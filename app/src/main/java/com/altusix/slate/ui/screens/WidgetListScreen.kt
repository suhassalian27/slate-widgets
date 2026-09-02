package com.altusix.slate.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
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
import com.altusix.slate.widgets.games.getGamesWidgetsCatalog
import com.altusix.slate.widgets.google.getGoogleWidgetsCatalog
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private fun isFullWidthWidget(sizeText: String): Boolean {
    val clean = sizeText.lowercase()
    return clean.startsWith("5x") || clean.startsWith("4x") || clean == "3x2" || clean == "3x3"
}

@OptIn(ExperimentalFoundationApi::class)
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
    val gamesWidgets = remember { getGamesWidgetsCatalog() }
    val googleWidgets = remember { getGoogleWidgetsCatalog() }

    val categories = remember {
        listOf(
            "All", "AI", "App Folder", "App Launcher", "Battery", "Bluetooth",
            "Calculator", "Calendar", "Camera", "Clock - Analog", "Clock - Digital",
            "Clock - Hybrid", "Compass", "Contacts", "Device", "Games", "Google"
        )
    }

    val pagerState = rememberPagerState(initialPage = 0) { categories.size }
    val categoryListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Smoothly auto-center active category pill
    LaunchedEffect(pagerState.currentPage) {
        val layoutInfo = categoryListState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        val targetItem = visibleItems.firstOrNull { it.index == pagerState.currentPage }

        if (targetItem != null) {
            val centerOffset = (layoutInfo.viewportSize.width - targetItem.size) / 2
            categoryListState.animateScrollToItem(pagerState.currentPage, -centerOffset)
        } else {
            categoryListState.animateScrollToItem(pagerState.currentPage)
        }
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
            state = categoryListState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(categories, key = { _, title -> title }) { index, title ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else Color(0xFF1C1C1E))
                        .clickable {
                            coroutineScope.launch {
                                // Premium slow & smooth transition physics
                                pagerState.animateScrollToPage(
                                    page = index,
                                    animationSpec = spring(
                                        dampingRatio = 0.75f,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
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

        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val displayedWidgets = remember(page) {
                when (page) {
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
                    15 -> gamesWidgets
                    16 -> googleWidgets
                    else -> aiWidgets + appFolderWidgets + appLauncherWidgets + batteryWidgets + bluetoothWidgets +
                            calculatorWidgets + calendarWidgets + cameraWidgets + clockWidgets +
                            clockDigitalWidgets + clockHybridWidgets + compassWidgets + contactsWidgets +
                            deviceInfoWidgets + gamesWidgets + googleWidgets
                }
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

            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = displayedWidgets,
                    key = { _, widget -> widget.receiverClass.name },
                    span = { index, _ ->
                        val spanValue = if (index < widgetSpans.size) widgetSpans[index] else 3
                        GridItemSpan(spanValue)
                    }
                ) { index, widget ->
                    val span = if (index < widgetSpans.size) widgetSpans[index] else 3

                    // OFFSET-DRIVEN ANIMATION: Guarantees 0 glitches & ignores vertical scroll
                    Box(
                        modifier = Modifier.graphicsLayer {
                            // Calculate exactly how far this page is from the screen center
                            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            val baseAbsOffset = pageOffset.absoluteValue

                            // Add a subtle stagger multiplier based on index for the cascading pop effect
                            val staggerFactor = (index % 12) * 0.08f
                            val animatedOffset = (baseAbsOffset * (1f + staggerFactor)).coerceIn(0f, 1f)

                            // 1f = fully settled on screen. 0f = fully off screen.
                            val progress = 1f - animatedOffset

                            // Scale smoothly from 80% to 100% based on pager offset
                            val scale = 0.80f + (0.20f * progress)
                            scaleX = scale
                            scaleY = scale

                            // Fade opacity based on progress
                            alpha = progress.coerceIn(0f, 1f)

                            // Glide upwards by 60px as the page comes into view
                            translationY = (1f - progress) * 60f
                        }
                    ) {
                        SleekWidgetCard(
                            widgetInfo = widget,
                            themeSettings = themeSettings,
                            span = span,
                            onClick = { onWidgetSelect(widget) }
                        )
                    }
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

    val (previewHeightModifier, targetHeightDp) = when (widgetInfo.sizeText.lowercase()) {
        "5x1", "4x1" -> Modifier.fillMaxWidth().height(88.dp) to 88
        "5x2", "4x2" -> Modifier.fillMaxWidth().height(154.dp) to 154
        "3x2", "3x3" -> Modifier.fillMaxWidth().height(134.dp) to 134
        else -> Modifier.fillMaxWidth().aspectRatio(1.0f) to 160
    }

    val previewPadding = if (span == 2) 10.dp else 22.dp
    val targetWidthDp = if (span == 6) 320 else if (span == 2) 100 else 150

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(outerShape)
            .background(Color(0xFF111111))
            .clickable { onClick() }
            .padding(0.dp)
    ) {
        Box(
            modifier = previewHeightModifier
                .clip(innerShape)
                .background(Color(0xFF5A5C65)),
            contentAlignment = Alignment.Center
        ) {
            SlateWidgetPreviewImage(
                widgetInfo = widgetInfo,
                themeSettings = themeSettings,
                targetWidthDp = targetWidthDp,
                targetHeightDp = targetHeightDp,
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