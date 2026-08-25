package com.altusix.slate.widgets.deviceinfo

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.getSafeBgColor
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius

fun fetchDeviceInfoData(context: Context): DeviceInfoData {
    var batteryPct = 0
    var isCharging = false
    try {
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, iFilter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level >= 0 && scale > 0) batteryPct = ((level / scale.toFloat()) * 100).toInt()
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    } catch (_: Exception) {}

    var usedStorageGb = 0.0
    var totalStorageGb = 0.0
    var storagePct = 0
    try {
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availBlocks = stat.availableBlocksLong
        val totalBytes = totalBlocks * blockSize
        val freeBytes = availBlocks * blockSize
        val usedBytes = totalBytes - freeBytes
        totalStorageGb = totalBytes / (1024.0 * 1024.0 * 1024.0)
        usedStorageGb = usedBytes / (1024.0 * 1024.0 * 1024.0)
        if (totalBytes > 0) storagePct = ((usedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt()
    } catch (_: Exception) {}

    var usedRamGb = 0.0
    var totalRamGb = 0.0
    var ramPct = 0
    try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(mi)
        val totalBytes = mi.totalMem
        val freeBytes = mi.availMem
        val usedBytes = totalBytes - freeBytes
        totalRamGb = totalBytes / (1024.0 * 1024.0 * 1024.0)
        usedRamGb = usedBytes / (1024.0 * 1024.0 * 1024.0)
        if (totalBytes > 0) ramPct = ((usedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt()
    } catch (_: Exception) {}

    return DeviceInfoData(
        batteryPct = batteryPct,
        isCharging = isCharging,
        usedStorageGb = usedStorageGb,
        totalStorageGb = totalStorageGb,
        storagePct = storagePct,
        usedRamGb = usedRamGb,
        totalRamGb = totalRamGb,
        ramPct = ramPct,
        deviceModel = Build.MODEL ?: "Android",
        androidVersion = "Android ${Build.VERSION.RELEASE}"
    )
}

private fun isJustRefreshed(context: Context, widgetId: Int): Boolean {
    val prefs = context.getSharedPreferences("slate_deviceinfo_prefs", Context.MODE_PRIVATE)
    val lastRefresh = prefs.getLong("widget_${widgetId}_last_refresh", 0L)
    return (System.currentTimeMillis() - lastRefresh) < 2000L
}

private fun drawBentoTile(canvas: Canvas, tileRect: RectF, radius: Float, tilePaint: Paint) {
    canvas.drawRoundRect(tileRect, radius, radius, tilePaint)
}

private fun drawAutoFitText(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Float,
    maxW: Float,
    basePaint: Paint,
    minSize: Float
) {
    val paint = Paint(basePaint)
    var size = basePaint.textSize
    paint.textSize = size
    while (paint.measureText(text) > maxW && size > minSize) {
        size -= 0.5f
        paint.textSize = size
    }
    canvas.drawText(text, x, y, paint)
}

private fun drawRefreshBadge(
    canvas: Canvas,
    context: Context,
    rect: RectF,
    scaleFactor: Float,
    accentColorInt: Int
) {
    val badgeH = scaleFactor * 14f
    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = scaleFactor * 7.5f
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.06f
    }

    val label = "• UPDATED"
    val textW = textPaint.measureText(label)
    val badgeW = textW + (scaleFactor * 12f)

    val badgeX = rect.right - badgeW - (scaleFactor * 8f)
    val badgeY = rect.top + (scaleFactor * 8f)
    val badgeRect = RectF(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH)

    canvas.drawRoundRect(badgeRect, badgeH / 2f, badgeH / 2f, badgePaint)

    val bounds = Rect()
    textPaint.getTextBounds(label, 0, label.length, bounds)
    canvas.drawText(label, badgeRect.centerX(), badgeRect.centerY() + (bounds.height() / 2f) - (scaleFactor * 0.5f), textPaint)
}

// 1. DEVICE INFO MINI BENTO (2x2)
fun generateDeviceInfoMiniBento2x2Bitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val scaleFactor = maxOf(density, 3.5f)

    val w = (wDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())
    val h = (hDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val info = fetchDeviceInfoData(context)
    val wasRefreshed = isJustRefreshed(context, widgetId)
    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        val size = minOf(w, h).toFloat()
        val leftX = (w - size) / 2f
        val topY = (h - size) / 2f
        RectF(leftX, topY, leftX + size, topY + size)
    }

    val cornerRadius = getStandardCornerRadius(density)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val pad = (minOf(cardRect.width(), cardRect.height()) * 0.05f).coerceIn(scaleFactor * 6f, scaleFactor * 12f)
    val gap = (minOf(cardRect.width(), cardRect.height()) * 0.05f).coerceIn(scaleFactor * 6f, scaleFactor * 12f)
    val tileW = (cardRect.width() - (pad * 2f) - gap) / 2f
    val tileH = (cardRect.height() - (pad * 2f) - gap) / 2f
    val innerRadius = (cornerRadius - pad).coerceAtLeast(scaleFactor * 4f).coerceAtMost(minOf(tileW, tileH) * 0.22f)
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
        style = Paint.Style.FILL
    }

    val minTileDim = minOf(tileW, tileH)
    val padX = tileW * 0.10f
    val maxTextWidth = tileW - (padX * 2f)

    val labelSize = (minTileDim * 0.18f).coerceIn(scaleFactor * 6f, scaleFactor * 11f)
    val valueSize = (minTileDim * 0.28f).coerceIn(scaleFactor * 8f, scaleFactor * 18f)

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = labelSize
        typeface = getSlateFont(context, weight = 600)
    }

    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (wasRefreshed) accentColorInt else primaryText
        textSize = valueSize
        typeface = getSlateFont(context, weight = 700)
    }

    val tiles = listOf(
        Triple("STORAGE", "${info.storagePct}%", null),
        Triple("RAM", "${info.ramPct}%", null),
        Triple("BATTERY", "${info.batteryPct}%" + if (info.isCharging) " ⚡" else "", accentColorInt),
        Triple("MODEL", info.deviceModel.take(12), null)
    )

    for (i in tiles.indices) {
        val col = i % 2
        val row = i / 2
        val left = cardRect.left + pad + col * (tileW + gap)
        val top = cardRect.top + pad + row * (tileH + gap)
        val rect = RectF(left, top, left + tileW, top + tileH)

        drawBentoTile(canvas, rect, innerRadius, tilePaint)

        val (label, value, overrideColor) = tiles[i]
        val labelY = rect.top + (tileH * 0.35f)
        val valueY = rect.bottom - (tileH * 0.22f)

        drawAutoFitText(canvas, label, rect.left + padX, labelY, maxTextWidth, labelPaint, scaleFactor * 4f)

        val vPaint = Paint(valuePaint)
        if (overrideColor != null) vPaint.color = overrideColor
        drawAutoFitText(canvas, value, rect.left + padX, valueY, maxTextWidth, vPaint, scaleFactor * 5f)
    }

    if (wasRefreshed) {
        drawRefreshBadge(canvas, context, cardRect, scaleFactor, accentColorInt)
    }

    return bitmap
}

// 2. STORAGE & RAM BAR CAPSULE (2x1)
fun generateStorageBarCapsule2x1Bitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val scaleFactor = maxOf(density, 3.5f)

    val w = (wDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())
    val h = (hDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val info = fetchDeviceInfoData(context)
    val wasRefreshed = isJustRefreshed(context, widgetId)
    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    val targetRatio = 2.0f
    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        var cardH = h.toFloat()
        var cardW = cardH * targetRatio
        if (cardW > w.toFloat()) {
            cardW = w.toFloat()
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cornerRadius = getStandardCornerRadius(density)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val padX = cardRect.width() * 0.06f
    val rowGap = (cardRect.height() * 0.12f).coerceIn(scaleFactor * 6f, scaleFactor * 14f)
    val barH = (cardRect.height() * 0.08f).coerceIn(scaleFactor * 4f, scaleFactor * 10f)
    val baseTextSize = (cardRect.height() * 0.13f).coerceIn(scaleFactor * 8f, scaleFactor * 13.5f)

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#E5E5EA") else Color.parseColor("#2C2C2E")
        style = Paint.Style.FILL
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColorInt
        style = Paint.Style.FILL
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        typeface = getSlateFont(context, weight = 600)
    }
    val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (wasRefreshed) accentColorInt else primaryText
        typeface = getSlateFont(context, weight = 700)
    }

    val items = listOf(
        Triple("Storage", "${String.format("%.1f", info.usedStorageGb)} / ${info.totalStorageGb.toInt()} GB", info.storagePct),
        Triple("RAM", "${String.format("%.1f", info.usedRamGb)} / ${info.totalRamGb.toInt()} GB", info.ramPct)
    )

    val availWidth = cardRect.width() - (padX * 2f)
    var unifiedTextSize = baseTextSize
    val testLabelPaint = Paint(labelPaint)
    val testValPaint = Paint(valPaint)

    items.forEach { (label, valStr, _) ->
        var tempSize = baseTextSize
        testLabelPaint.textSize = tempSize
        testValPaint.textSize = tempSize
        while ((testLabelPaint.measureText(label) + testValPaint.measureText(valStr) + (scaleFactor * 8f)) > availWidth && tempSize > scaleFactor * 5f) {
            tempSize -= 0.5f
            testLabelPaint.textSize = tempSize
            testValPaint.textSize = tempSize
        }
        if (tempSize < unifiedTextSize) {
            unifiedTextSize = tempSize
        }
    }

    labelPaint.textSize = unifiedTextSize
    valPaint.textSize = unifiedTextSize

    val singleRowH = unifiedTextSize + (scaleFactor * 4f) + barH
    val totalBlockH = (singleRowH * 2f) + rowGap
    val startY = cardRect.centerY() - (totalBlockH / 2f)

    for (i in items.indices) {
        val (label, valStr, pct) = items[i]
        val rowTop = startY + i * (singleRowH + rowGap)
        val textY = rowTop + unifiedTextSize

        canvas.drawText(label, cardRect.left + padX, textY, labelPaint)
        canvas.drawText(valStr, cardRect.right - padX - valPaint.measureText(valStr), textY, valPaint)

        val barTop = textY + (scaleFactor * 6f)
        val barRect = RectF(cardRect.left + padX, barTop, cardRect.right - padX, barTop + barH)
        canvas.drawRoundRect(barRect, barH / 2f, barH / 2f, trackPaint)

        val fillW = barRect.width() * (pct.coerceIn(0, 100) / 100f)
        if (fillW > 0) {
            val fillRect = RectF(barRect.left, barRect.top, barRect.left + fillW, barRect.bottom)
            canvas.drawRoundRect(fillRect, barH / 2f, barH / 2f, fillPaint)
        }
    }

    if (wasRefreshed) {
        drawRefreshBadge(canvas, context, cardRect, scaleFactor, accentColorInt)
    }

    return bitmap
}

// 3. FULL SYSTEM DASHBOARD BENTO (4x2)
fun generateDeviceInfoDashboard4x2Bitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val scaleFactor = maxOf(density, 3.5f)

    val w = (wDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())
    val h = (hDp * scaleFactor).toInt().coerceAtLeast((60 * scaleFactor).toInt())

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val info = fetchDeviceInfoData(context)
    val wasRefreshed = isJustRefreshed(context, widgetId)
    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val accentColorInt = config.accentColorHex.toInt() or 0xFF000000.toInt()
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")

    val targetRatio = 2.0f
    val cardRect = if (isResponsive) {
        RectF(0f, 0f, w.toFloat(), h.toFloat())
    } else {
        var cardH = h.toFloat()
        var cardW = cardH * targetRatio
        if (cardW > w.toFloat()) {
            cardW = w.toFloat()
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val cornerRadius = getStandardCornerRadius(density)
    val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

    val pad = (minOf(cardRect.width(), cardRect.height()) * 0.05f).coerceIn(scaleFactor * 6f, scaleFactor * 12f)
    val gap = (minOf(cardRect.width(), cardRect.height()) * 0.05f).coerceIn(scaleFactor * 6f, scaleFactor * 12f)
    val halfW = (cardRect.width() - (pad * 2f) - gap) / 2f
    val halfH = (cardRect.height() - (pad * 2f) - gap) / 2f
    val innerRadius = (cornerRadius - pad).coerceAtLeast(scaleFactor * 4f).coerceAtMost(minOf(halfW, halfH) * 0.22f)
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
        style = Paint.Style.FILL
    }

    val minTileDim = minOf(halfW, halfH)

    val labelSize = (minTileDim * 0.18f).coerceIn(scaleFactor * 6f, scaleFactor * 11f)
    val valueSize = (minTileDim * 0.28f).coerceIn(scaleFactor * 8f, scaleFactor * 18f)
    val subSize = (minTileDim * 0.20f).coerceIn(scaleFactor * 7f, scaleFactor * 13f)

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = labelSize
        typeface = getSlateFont(context, weight = 600)
    }
    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (wasRefreshed) accentColorInt else primaryText
        textSize = valueSize
        typeface = getSlateFont(context, weight = 700)
    }
    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = subSize
        typeface = getSlateFont(context, weight = 600)
    }

    val tilePadX = halfW * 0.10f
    val tilePadY = (minTileDim * 0.12f).coerceAtMost(scaleFactor * 12f)
    val textGap = (minTileDim * 0.08f).coerceIn(scaleFactor * 3f, scaleFactor * 7f)

    // Big Left Card: Storage Details
    val bLeft = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.left + pad + halfW, cardRect.bottom - pad)
    drawBentoTile(canvas, bLeft, innerRadius, tilePaint)
    val bLeftMaxW = bLeft.width() - (tilePadX * 2f)

    val bLeftLine1Y = bLeft.top + tilePadY + labelSize
    val bLeftLine2Y = bLeftLine1Y + textGap + valueSize
    val bLeftLine3Y = bLeftLine2Y + textGap + subSize

    drawAutoFitText(canvas, "STORAGE DISK", bLeft.left + tilePadX, bLeftLine1Y, bLeftMaxW, labelPaint, scaleFactor * 4f)
    drawAutoFitText(canvas, "${info.storagePct}% Used", bLeft.left + tilePadX, bLeftLine2Y, bLeftMaxW, valuePaint, scaleFactor * 5f)
    drawAutoFitText(canvas, "${String.format("%.1f", info.usedStorageGb)} / ${info.totalStorageGb.toInt()} GB", bLeft.left + tilePadX, bLeftLine3Y, bLeftMaxW, subPaint, scaleFactor * 4f)

    // Top Right: RAM
    val tr1 = RectF(cardRect.left + pad + halfW + gap, cardRect.top + pad, cardRect.right - pad, cardRect.top + pad + halfH)
    drawBentoTile(canvas, tr1, innerRadius, tilePaint)
    val tr1MaxW = tr1.width() - (tilePadX * 2f)

    val tr1Line1Y = tr1.top + tilePadY + labelSize
    val tr1Line2Y = tr1Line1Y + textGap + valueSize

    drawAutoFitText(canvas, "RAM MEMORY", tr1.left + tilePadX, tr1Line1Y, tr1MaxW, labelPaint, scaleFactor * 4f)
    drawAutoFitText(canvas, "${info.ramPct}% (${String.format("%.1f", info.usedRamGb)}GB)", tr1.left + tilePadX, tr1Line2Y, tr1MaxW, valuePaint, scaleFactor * 5f)

    // Bottom Right: Battery
    val tr2 = RectF(cardRect.left + pad + halfW + gap, cardRect.top + pad + halfH + gap, cardRect.right - pad, cardRect.bottom - pad)
    drawBentoTile(canvas, tr2, innerRadius, tilePaint)
    val tr2MaxW = tr2.width() - (tilePadX * 2f)

    val tr2Line1Y = tr2.top + tilePadY + labelSize
    val tr2Line2Y = tr2Line1Y + textGap + valueSize

    drawAutoFitText(canvas, "BATTERY", tr2.left + tilePadX, tr2Line1Y, tr2MaxW, labelPaint, scaleFactor * 4f)
    val batPaint = Paint(valuePaint).apply { color = accentColorInt }
    drawAutoFitText(canvas, "${info.batteryPct}%" + if (info.isCharging) " ⚡" else "", tr2.left + tilePadX, tr2Line2Y, tr2MaxW, batPaint, scaleFactor * 5f)

    if (wasRefreshed) {
        drawRefreshBadge(canvas, context, cardRect, scaleFactor, accentColorInt)
    }

    return bitmap
}