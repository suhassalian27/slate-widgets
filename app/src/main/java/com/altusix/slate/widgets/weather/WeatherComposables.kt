package com.altusix.slate.widgets.weather

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.content.ContextCompat
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.createSupersampledCanvas
import com.altusix.slate.utils.getSafeBgColor
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius
import java.util.Calendar

// -------------------------------------------------------------------------
// VECTOR DRAWABLE HELPERS
// -------------------------------------------------------------------------

fun getDrawableResId(context: Context, resName: String): Int {
    return context.resources.getIdentifier(resName, "drawable", context.packageName)
}

fun isNightTime(weather: WeatherData): Boolean {
    return try {
        val cal = Calendar.getInstance()
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val sunriseParts = weather.sunrise.split(":")
        val sunsetParts = weather.sunset.split(":")
        val riseMinutes = (sunriseParts.getOrNull(0)?.toIntOrNull() ?: 6) * 60 + (sunriseParts.getOrNull(1)?.toIntOrNull() ?: 0)
        val setMinutes = (sunsetParts.getOrNull(0)?.toIntOrNull() ?: 19) * 60 + (sunsetParts.getOrNull(1)?.toIntOrNull() ?: 30)
        currentMinutes < riseMinutes || currentMinutes >= setMinutes
    } catch (_: Exception) {
        false
    }
}

fun getWeatherIconResId(context: Context, weatherCode: Int, isNight: Boolean = false): Int {
    val resName = when (weatherCode) {
        0 -> if (isNight) "ic_weather_clear_night" else "ic_weather_clear_day"
        1, 2 -> if (isNight) "ic_weather_partly_cloudy_night" else "ic_weather_partly_cloudy_day"
        3 -> "ic_weather_cloudy"
        45, 48 -> "ic_weather_fog"
        51, 53, 55, 56, 57 -> "ic_weather_drizzle"
        61, 63, 65, 80, 81, 82 -> "ic_weather_rain"
        66, 67 -> "ic_weather_sleet"
        71, 73, 75, 77, 85, 86 -> "ic_weather_snow"
        95, 96, 99 -> "ic_weather_thunderstorm"
        else -> "ic_weather_windy"
    }

    var id = getDrawableResId(context, resName)
    if (id == 0 && isNight && resName.endsWith("_night")) {
        id = getDrawableResId(context, resName.replace("_night", "_day"))
    }
    if (id == 0) {
        id = getDrawableResId(context, "ic_weather_cloudy")
    }
    return id
}

fun drawVectorDrawable(
    canvas: Canvas,
    context: Context,
    drawableResId: Int,
    bounds: RectF,
    tintColor: Int
) {
    if (drawableResId == 0) return
    try {
        val drawable = ContextCompat.getDrawable(context, drawableResId)?.mutate() ?: return
        drawable.setTint(tintColor)
        drawable.setBounds(
            bounds.left.toInt(),
            bounds.top.toInt(),
            bounds.right.toInt(),
            bounds.bottom.toInt()
        )
        drawable.draw(canvas)
    } catch (_: Exception) {}
}

fun drawWeatherIcon(
    canvas: Canvas,
    context: Context,
    weatherCode: Int,
    iconRect: RectF,
    color: Int,
    isNight: Boolean = false
) {
    val resId = getWeatherIconResId(context, weatherCode, isNight)
    if (resId != 0) {
        drawVectorDrawable(canvas, context, resId, iconRect, color)
    }
}

// -------------------------------------------------------------------------
// 10 CONCRETE WEATHER BITMAP GENERATORS
// -------------------------------------------------------------------------

// 1. Weather Horizon (4x2)
fun generateWeatherHorizonBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val accentColor = config.accentColorHex.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 2.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val weather = WeatherPreferences.getCachedWeatherData(context)
    val unit = WeatherPreferences.getUnit(context)
    val pad = scaleFactor * 16f
    val halfW = (cardRect.width() - (pad * 2f)) * 0.48f

    // LEFT: Current Weather
    val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 11f
        typeface = getSlateFont(context, weight = 700)
        letterSpacing = 0.08f
    }
    canvas.drawText(weather.cityName.uppercase(), cardRect.left + pad, cardRect.top + pad + (scaleFactor * 10f), cityPaint)

    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 46f
        typeface = getSlateFont(context, weight = 800)
    }
    canvas.drawText(WeatherPreferences.formatTemp(weather.currentTemp, unit), cardRect.left + pad, cardRect.top + pad + (scaleFactor * 52f), tempPaint)

    val condPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 14f
        typeface = getSlateFont(context, weight = 600)
    }
    canvas.drawText(weather.conditionText, cardRect.left + pad, cardRect.top + pad + (scaleFactor * 72f), condPaint)

    val hiLoStr = "H: ${WeatherPreferences.formatTemp(weather.tempMax, unit)}   L: ${WeatherPreferences.formatTemp(weather.tempMin, unit)}"
    val hiLoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 11.5f
        typeface = getSlateFont(context, weight = 600)
    }
    canvas.drawText(hiLoStr, cardRect.left + pad, cardRect.top + pad + (scaleFactor * 90f), hiLoPaint)

    // DIVIDER
    val dividerX = cardRect.left + pad + halfW + (scaleFactor * 8f)
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(24, 0, 0, 0) else Color.argb(32, 255, 255, 255)
        strokeWidth = scaleFactor * 1f
    }
    canvas.drawLine(dividerX, cardRect.top + pad, dividerX, cardRect.bottom - pad, dividerPaint)

    // RIGHT: 5-Day Forecast
    val rightLeft = dividerX + (scaleFactor * 12f)
    val rightWidth = cardRect.right - pad - rightLeft
    val dailyList = weather.dailyForecast.take(5)
    val rowH = (cardRect.height() - (pad * 2f)) / 5f

    val dayNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 11f
        typeface = getSlateFont(context, weight = 600)
    }
    val rangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 11f
        typeface = getSlateFont(context, weight = 600)
        textAlign = Paint.Align.RIGHT
    }

    for (i in dailyList.indices) {
        val item = dailyList[i]
        val cy = cardRect.top + pad + (i * rowH) + (rowH / 2f)

        canvas.drawText(item.dayLabel, rightLeft, cy + (scaleFactor * 4f), dayNamePaint)

        val iconSize = scaleFactor * 16f
        val iconX = rightLeft + (rightWidth * 0.40f)
        val iconRect = RectF(iconX - (iconSize / 2f), cy - (iconSize / 2f), iconX + (iconSize / 2f), cy + (iconSize / 2f))
        drawWeatherIcon(canvas, context, item.weatherCode, iconRect, accentColor, isNight = false)

        if (item.rainProb >= 20) {
            val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#38ACFF")
                textSize = scaleFactor * 8.5f
                typeface = getSlateFont(context, weight = 700)
            }
            canvas.drawText("${item.rainProb}%", iconX + (scaleFactor * 11f), cy + (scaleFactor * 3.5f), rainPaint)
        }

        val rangeText = "${WeatherPreferences.formatTempValue(item.maxTemp, unit)}°  ${WeatherPreferences.formatTempValue(item.minTemp, unit)}°"
        canvas.drawText(rangeText, cardRect.right - pad, cy + (scaleFactor * 4f), rangePaint)
    }

    return bitmap
}

// 2. Weather Bento Glance (4x2)
fun generateWeatherBentoGlanceBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val accentColor = config.accentColorHex.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 2.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val weather = WeatherPreferences.getCachedWeatherData(context)
    val unit = WeatherPreferences.getUnit(context)
    val isNight = isNightTime(weather)

    val pad = scaleFactor * 8f
    val gap = scaleFactor * 8f
    val innerCardBg = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    val concentricRadius = (outerRadius - pad).coerceAtLeast(scaleFactor * 6f)
    val sq = scaleFactor * 10f

    // LEFT MAIN CARD
    val leftW = (cardRect.width() - (pad * 2f) - gap) * 0.52f
    val leftRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.left + pad + leftW, cardRect.bottom - pad)
    val leftRadii = floatArrayOf(concentricRadius, concentricRadius, sq, sq, sq, sq, concentricRadius, concentricRadius)
    val leftPath = Path().apply { addRoundRect(leftRect, leftRadii, Path.Direction.CW) }
    canvas.drawPath(leftPath, tilePaint)

    val innerPad = scaleFactor * 12f
    val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 10.5f
        typeface = getSlateFont(context, weight = 700)
    }
    canvas.drawText(weather.cityName.uppercase(), leftRect.left + innerPad, leftRect.top + innerPad + (scaleFactor * 8f), cityPaint)

    val mainIconSize = scaleFactor * 32f
    val mainIconRect = RectF(
        leftRect.right - innerPad - mainIconSize,
        leftRect.top + innerPad,
        leftRect.right - innerPad,
        leftRect.top + innerPad + mainIconSize
    )
    drawWeatherIcon(canvas, context, weather.weatherCode, mainIconRect, accentColor, isNight)

    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 40f
        typeface = getSlateFont(context, weight = 800)
    }
    canvas.drawText(WeatherPreferences.formatTemp(weather.currentTemp, unit), leftRect.left + innerPad, leftRect.top + (scaleFactor * 68f), tempPaint)

    val condPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 13f
        typeface = getSlateFont(context, weight = 600)
    }
    canvas.drawText(weather.conditionText, leftRect.left + innerPad, leftRect.bottom - innerPad - (scaleFactor * 12f), condPaint)

    val hlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 10f
        typeface = getSlateFont(context, weight = 600)
    }
    canvas.drawText("H: ${WeatherPreferences.formatTemp(weather.tempMax, unit)}  L: ${WeatherPreferences.formatTemp(weather.tempMin, unit)}", leftRect.left + innerPad, leftRect.bottom - innerPad, hlPaint)

    // RIGHT TOP CARD: Feels Like & Humidity
    val rightLeft = leftRect.right + gap
    val rightW = cardRect.right - pad - rightLeft
    val subH = (cardRect.height() - (pad * 2f) - gap) / 2f

    val topRect = RectF(rightLeft, cardRect.top + pad, cardRect.right - pad, cardRect.top + pad + subH)
    val topRadii = floatArrayOf(sq, sq, concentricRadius, concentricRadius, sq, sq, sq, sq)
    val topPath = Path().apply { addRoundRect(topRect, topRadii, Path.Direction.CW) }
    canvas.drawPath(topPath, tilePaint)

    val metricHalfW = rightW / 2f
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 9f
        typeface = getSlateFont(context, weight = 700)
    }
    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 17f
        typeface = getSlateFont(context, weight = 700)
    }

    // Feels Like with Thermometer Icon
    val thermoRes = getDrawableResId(context, "ic_thermometer")
    if (thermoRes != 0) {
        drawVectorDrawable(canvas, context, thermoRes, RectF(topRect.left + (scaleFactor * 10f), topRect.top + (scaleFactor * 8f), topRect.left + (scaleFactor * 20f), topRect.top + (scaleFactor * 18f)), secondaryText)
        canvas.drawText("FEELS LIKE", topRect.left + (scaleFactor * 23f), topRect.top + (scaleFactor * 16f), labelPaint)
    } else {
        canvas.drawText("FEELS LIKE", topRect.left + (scaleFactor * 10f), topRect.top + (scaleFactor * 16f), labelPaint)
    }
    canvas.drawText(WeatherPreferences.formatTemp(weather.feelsLike, unit), topRect.left + (scaleFactor * 10f), topRect.top + (scaleFactor * 36f), valuePaint)

    // Humidity with Drop Icon
    val humidRes = getDrawableResId(context, "ic_humidity")
    if (humidRes != 0) {
        drawVectorDrawable(canvas, context, humidRes, RectF(topRect.left + metricHalfW + (scaleFactor * 6f), topRect.top + (scaleFactor * 8f), topRect.left + metricHalfW + (scaleFactor * 16f), topRect.top + (scaleFactor * 18f)), secondaryText)
        canvas.drawText("HUMIDITY", topRect.left + metricHalfW + (scaleFactor * 19f), topRect.top + (scaleFactor * 16f), labelPaint)
    } else {
        canvas.drawText("HUMIDITY", topRect.left + metricHalfW + (scaleFactor * 6f), topRect.top + (scaleFactor * 16f), labelPaint)
    }
    canvas.drawText("${weather.humidity}%", topRect.left + metricHalfW + (scaleFactor * 6f), topRect.top + (scaleFactor * 36f), valuePaint)

    // RIGHT BOTTOM CARD: Wind & UV Index
    val btmRect = RectF(rightLeft, topRect.bottom + gap, cardRect.right - pad, cardRect.bottom - pad)
    val btmRadii = floatArrayOf(sq, sq, sq, sq, concentricRadius, concentricRadius, sq, sq)
    val btmPath = Path().apply { addRoundRect(btmRect, btmRadii, Path.Direction.CW) }
    canvas.drawPath(btmPath, tilePaint)

    // Wind with Windy Icon
    val windRes = getDrawableResId(context, "ic_weather_windy")
    if (windRes != 0) {
        drawVectorDrawable(canvas, context, windRes, RectF(btmRect.left + (scaleFactor * 10f), btmRect.top + (scaleFactor * 8f), btmRect.left + (scaleFactor * 20f), btmRect.top + (scaleFactor * 18f)), secondaryText)
        canvas.drawText("WIND", btmRect.left + (scaleFactor * 23f), btmRect.top + (scaleFactor * 16f), labelPaint)
    } else {
        canvas.drawText("WIND", btmRect.left + (scaleFactor * 10f), btmRect.top + (scaleFactor * 16f), labelPaint)
    }
    canvas.drawText("${weather.windSpeedKmH.toInt()} km/h", btmRect.left + (scaleFactor * 10f), btmRect.top + (scaleFactor * 36f), valuePaint)

    // UV Index with Sun/UV Icon
    val uvRes = getDrawableResId(context, "ic_uv_index")
    if (uvRes != 0) {
        drawVectorDrawable(canvas, context, uvRes, RectF(btmRect.left + metricHalfW + (scaleFactor * 6f), btmRect.top + (scaleFactor * 8f), btmRect.left + metricHalfW + (scaleFactor * 16f), btmRect.top + (scaleFactor * 18f)), secondaryText)
        canvas.drawText("UV INDEX", btmRect.left + metricHalfW + (scaleFactor * 19f), btmRect.top + (scaleFactor * 16f), labelPaint)
    } else {
        canvas.drawText("UV INDEX", btmRect.left + metricHalfW + (scaleFactor * 6f), btmRect.top + (scaleFactor * 16f), labelPaint)
    }
    canvas.drawText(String.format("%.1f", weather.uvIndex), btmRect.left + metricHalfW + (scaleFactor * 6f), btmRect.top + (scaleFactor * 36f), valuePaint)

    return bitmap
}

// 3. Weather Daylight Arc (2x2)
fun generateWeatherDaylightArcBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val accentColor = config.accentColorHex.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 1.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val size = minOf(w - (margin * 2f), h - (margin * 2f))
        val leftX = (w - size) / 2f
        val topY = (h - size) / 2f
        RectF(leftX, topY, leftX + size, topY + size)
    }

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val weather = WeatherPreferences.getCachedWeatherData(context)
    val unit = WeatherPreferences.getUnit(context)

    val cx = cardRect.centerX()
    val cy = cardRect.top + (cardRect.height() * 0.50f)
    val arcRadius = minOf(cardRect.width(), cardRect.height()) * 0.36f
    val arcRect = RectF(cx - arcRadius, cy - arcRadius, cx + arcRadius, cy + arcRadius)

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(20, 0, 0, 0) else Color.argb(30, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 3.5f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawArc(arcRect, 180f, 180f, false, trackPaint)

    val cal = Calendar.getInstance()
    val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val sunriseParts = weather.sunrise.split(":")
    val sunsetParts = weather.sunset.split(":")
    val riseMinutes = (sunriseParts.getOrNull(0)?.toIntOrNull() ?: 6) * 60 + (sunriseParts.getOrNull(1)?.toIntOrNull() ?: 0)
    val setMinutes = (sunsetParts.getOrNull(0)?.toIntOrNull() ?: 19) * 60 + (sunsetParts.getOrNull(1)?.toIntOrNull() ?: 30)

    val daylightRatio = ((currentMinutes - riseMinutes).toFloat() / (setMinutes - riseMinutes).toFloat()).coerceIn(0f, 1f)
    val sweepAngle = daylightRatio * 180f

    val activeArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 3.5f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawArc(arcRect, 180f, sweepAngle, false, activeArcPaint)

    val sunAngleRad = Math.toRadians((180.0 + sweepAngle).toDouble())
    val markerX = cx + (arcRadius * Math.cos(sunAngleRad)).toFloat()
    val markerY = cy + (arcRadius * Math.sin(sunAngleRad)).toFloat()

    val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(markerX, markerY, scaleFactor * 5f, markerPaint)

    // Sunrise / Sunset with icons
    val timeLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 8.5f
        typeface = getSlateFont(context, weight = 600)
    }
    val sunriseRes = getDrawableResId(context, "ic_sunrise")
    if (sunriseRes != 0) {
        drawVectorDrawable(canvas, context, sunriseRes, RectF(cx - arcRadius, cy + (scaleFactor * 5f), cx - arcRadius + (scaleFactor * 12f), cy + (scaleFactor * 17f)), secondaryText)
        canvas.drawText(weather.sunrise, cx - arcRadius + (scaleFactor * 14f), cy + (scaleFactor * 14f), timeLabelPaint)
    } else {
        canvas.drawText("↑ ${weather.sunrise}", cx - arcRadius, cy + (scaleFactor * 14f), timeLabelPaint)
    }

    val sunsetRes = getDrawableResId(context, "ic_sunset")
    if (sunsetRes != 0) {
        drawVectorDrawable(canvas, context, sunsetRes, RectF(cx + arcRadius - (scaleFactor * 34f), cy + (scaleFactor * 5f), cx + arcRadius - (scaleFactor * 22f), cy + (scaleFactor * 17f)), secondaryText)
        val setPaint = Paint(timeLabelPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText(weather.sunset, cx + arcRadius, cy + (scaleFactor * 14f), setPaint)
    } else {
        val setPaint = Paint(timeLabelPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("↓ ${weather.sunset}", cx + arcRadius, cy + (scaleFactor * 14f), setPaint)
    }

    // Center Temp
    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 34f
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(WeatherPreferences.formatTemp(weather.currentTemp, unit), cx, cy - (scaleFactor * 4f), tempPaint)

    val condPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 12.5f
        typeface = getSlateFont(context, weight = 600)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(weather.conditionText, cx, cardRect.bottom - (scaleFactor * 26f), condPaint)

    val pillText = "${weather.cityName}  •  H: ${WeatherPreferences.formatTemp(weather.tempMax, unit)}  L: ${WeatherPreferences.formatTemp(weather.tempMin, unit)}"
    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 9.5f
        typeface = getSlateFont(context, weight = 600)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(pillText, cx, cardRect.bottom - (scaleFactor * 12f), pillPaint)

    return bitmap
}

// 4. Weather Pill Dock (4x1)
fun generateWeatherPillDockBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val accentColor = config.accentColorHex.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 4.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val weather = WeatherPreferences.getCachedWeatherData(context)
    val unit = WeatherPreferences.getUnit(context)
    val isNight = isNightTime(weather)
    val pad = scaleFactor * 14f

    val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 13f
        typeface = getSlateFont(context, weight = 700)
    }
    canvas.drawText(weather.cityName, cardRect.left + pad, cardRect.centerY() - (scaleFactor * 3f), cityPaint)

    val condPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 10f
        typeface = getSlateFont(context, weight = 600)
    }
    canvas.drawText("${weather.conditionText} • H: ${WeatherPreferences.formatTemp(weather.tempMax, unit)} L: ${WeatherPreferences.formatTemp(weather.tempMin, unit)}", cardRect.left + pad, cardRect.centerY() + (scaleFactor * 11f), condPaint)

    // Center Temp & Icon
    val centerLeft = cardRect.left + (cardRect.width() * 0.44f)
    val iconSize = scaleFactor * 24f
    val iconRect = RectF(centerLeft, cardRect.centerY() - (iconSize / 2f), centerLeft + iconSize, cardRect.centerY() + (iconSize / 2f))
    drawWeatherIcon(canvas, context, weather.weatherCode, iconRect, accentColor, isNight)

    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 24f
        typeface = getSlateFont(context, weight = 800)
    }
    canvas.drawText(WeatherPreferences.formatTemp(weather.currentTemp, unit), centerLeft + iconSize + (scaleFactor * 8f), cardRect.centerY() + (scaleFactor * 8f), tempPaint)

    // Right Hourly Pills
    val hourlyItems = weather.hourlyForecast.drop(1).take(3)
    val pillStartX = cardRect.right - pad - (scaleFactor * 115f)
    val pillW = scaleFactor * 36f
    val pillGap = scaleFactor * 4f

    val hourLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 8.5f
        typeface = getSlateFont(context, weight = 600)
        textAlign = Paint.Align.CENTER
    }
    val hourTempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 10f
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    for (i in hourlyItems.indices) {
        val item = hourlyItems[i]
        val px = pillStartX + i * (pillW + pillGap)
        val pRect = RectF(px, cardRect.top + (scaleFactor * 8f), px + pillW, cardRect.bottom - (scaleFactor * 8f))

        val innerCardBg = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
        val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = innerCardBg
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(pRect, scaleFactor * 8f, scaleFactor * 8f, tilePaint)

        canvas.drawText(item.timeLabel, pRect.centerX(), pRect.top + (scaleFactor * 11f), hourLabelPaint)
        canvas.drawText(WeatherPreferences.formatTemp(item.temp, unit), pRect.centerX(), pRect.bottom - (scaleFactor * 6f), hourTempPaint)
    }

    return bitmap
}

// 5. Weather Editorial Capsule (2x2)
fun generateWeatherEditorialBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val accentColor = config.accentColorHex.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 1.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val size = minOf(w - (margin * 2f), h - (margin * 2f))
        val leftX = (w - size) / 2f
        val topY = (h - size) / 2f
        RectF(leftX, topY, leftX + size, topY + size)
    }

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val weather = WeatherPreferences.getCachedWeatherData(context)
    val unit = WeatherPreferences.getUnit(context)
    val isNight = isNightTime(weather)
    val pad = scaleFactor * 16f

    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 10f
        typeface = getSlateFont(context, weight = 700)
        letterSpacing = 0.12f
    }
    canvas.drawText(weather.conditionText.uppercase(), cardRect.left + pad, cardRect.top + pad + (scaleFactor * 10f), headerPaint)

    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 52f
        typeface = getSlateFont(context, weight = 800)
    }
    canvas.drawText(WeatherPreferences.formatTemp(weather.currentTemp, unit), cardRect.left + pad, cardRect.top + (scaleFactor * 78f), tempPaint)

    // Top Right Icon
    val iconSize = scaleFactor * 32f
    val iconRect = RectF(cardRect.right - pad - iconSize, cardRect.top + pad, cardRect.right - pad, cardRect.top + pad + iconSize)
    drawWeatherIcon(canvas, context, weather.weatherCode, iconRect, accentColor, isNight)

    val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 13f
        typeface = getSlateFont(context, weight = 700)
    }
    canvas.drawText(weather.cityName, cardRect.left + pad, cardRect.bottom - pad - (scaleFactor * 12f), cityPaint)

    val rangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 10.5f
        typeface = getSlateFont(context, weight = 600)
    }
    canvas.drawText("H: ${WeatherPreferences.formatTemp(weather.tempMax, unit)}  •  L: ${WeatherPreferences.formatTemp(weather.tempMin, unit)}", cardRect.left + pad, cardRect.bottom - pad, rangePaint)

    return bitmap
}

// 6. Weather Hourly Ribbon (4x1)
fun generateWeatherHourlyRibbonBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val accentColor = config.accentColorHex.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 4.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val weather = WeatherPreferences.getCachedWeatherData(context)
    val unit = WeatherPreferences.getUnit(context)
    val pad = scaleFactor * 12f

    val leftW = scaleFactor * 75f
    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 26f
        typeface = getSlateFont(context, weight = 800)
    }
    canvas.drawText(WeatherPreferences.formatTemp(weather.currentTemp, unit), cardRect.left + pad, cardRect.centerY() - (scaleFactor * 2f), tempPaint)

    val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 9.5f
        typeface = getSlateFont(context, weight = 700)
    }
    canvas.drawText(weather.cityName, cardRect.left + pad, cardRect.centerY() + (scaleFactor * 11f), cityPaint)

    val divX = cardRect.left + pad + leftW
    val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(20, 0, 0, 0) else Color.argb(30, 255, 255, 255)
        strokeWidth = scaleFactor * 1f
    }
    canvas.drawLine(divX, cardRect.top + pad, divX, cardRect.bottom - pad, divPaint)

    val hourly = weather.hourlyForecast.take(5)
    val ribbonLeft = divX + (scaleFactor * 12f)
    val ribbonW = cardRect.right - pad - ribbonLeft
    val nodeStep = ribbonW / maxOf(1, hourly.size - 1)

    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 8.5f
        typeface = getSlateFont(context, weight = 600)
        textAlign = Paint.Align.CENTER
    }
    val hTempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 10.5f
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    for (i in hourly.indices) {
        val item = hourly[i]
        val nx = ribbonLeft + i * nodeStep
        val iconSize = scaleFactor * 15f
        val iRect = RectF(nx - (iconSize / 2f), cardRect.centerY() - (iconSize / 2f) - (scaleFactor * 2f), nx + (iconSize / 2f), cardRect.centerY() + (iconSize / 2f) - (scaleFactor * 2f))

        canvas.drawText(item.timeLabel, nx, cardRect.top + pad + (scaleFactor * 6f), timePaint)
        drawWeatherIcon(canvas, context, item.weatherCode, iRect, accentColor, isNight = false)
        canvas.drawText(WeatherPreferences.formatTemp(item.temp, unit), nx, cardRect.bottom - pad, hTempPaint)
    }

    return bitmap
}

// 7. Weather Minimalist Dual (2x2)
fun generateWeatherMinimalistDualBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val accentColor = config.accentColorHex.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 1.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val size = minOf(w - (margin * 2f), h - (margin * 2f))
        val leftX = (w - size) / 2f
        val topY = (h - size) / 2f
        RectF(leftX, topY, leftX + size, topY + size)
    }

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val weather = WeatherPreferences.getCachedWeatherData(context)
    val unit = WeatherPreferences.getUnit(context)
    val isNight = isNightTime(weather)
    val pad = scaleFactor * 14f
    val cx = cardRect.centerX()

    // Left Half: Weather Vector Icon
    val iconSize = scaleFactor * 48f
    val iconLeft = cardRect.left + (cardRect.width() * 0.25f) - (iconSize / 2f)
    val iconTop = cardRect.centerY() - (iconSize / 2f)
    val iconRect = RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
    drawWeatherIcon(canvas, context, weather.weatherCode, iconRect, accentColor, isNight)

    // Center Divider
    val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(18, 0, 0, 0) else Color.argb(26, 255, 255, 255)
        strokeWidth = scaleFactor * 1f
    }
    canvas.drawLine(cx, cardRect.top + pad, cx, cardRect.bottom - pad, divPaint)

    // Right Half: Stacked Info
    val rightX = cx + (scaleFactor * 12f)
    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 36f
        typeface = getSlateFont(context, weight = 800)
    }
    canvas.drawText(WeatherPreferences.formatTemp(weather.currentTemp, unit), rightX, cardRect.top + (scaleFactor * 52f), tempPaint)

    val condPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 12f
        typeface = getSlateFont(context, weight = 600)
    }
    canvas.drawText(weather.conditionText, rightX, cardRect.top + (scaleFactor * 72f), condPaint)

    val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 10f
        typeface = getSlateFont(context, weight = 700)
    }
    canvas.drawText(weather.cityName.uppercase(), rightX, cardRect.top + (scaleFactor * 90f), cityPaint)

    val hlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 9.5f
        typeface = getSlateFont(context, weight = 600)
    }
    canvas.drawText("H: ${WeatherPreferences.formatTemp(weather.tempMax, unit)}  L: ${WeatherPreferences.formatTemp(weather.tempMin, unit)}", rightX, cardRect.bottom - pad, hlPaint)

    return bitmap
}

// 8. Weather Compact Dial (2x2)
fun generateWeatherCompactDialBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val accentColor = config.accentColorHex.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 1.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val size = minOf(w - (margin * 2f), h - (margin * 2f))
        val leftX = (w - size) / 2f
        val topY = (h - size) / 2f
        RectF(leftX, topY, leftX + size, topY + size)
    }

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val weather = WeatherPreferences.getCachedWeatherData(context)
    val unit = WeatherPreferences.getUnit(context)
    val isNight = isNightTime(weather)
    val pad = scaleFactor * 12f

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 8.5f
        typeface = getSlateFont(context, weight = 700)
    }
    val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 11.5f
        typeface = getSlateFont(context, weight = 700)
    }

    // Top-Left: WIND with icon
    val windRes = getDrawableResId(context, "ic_weather_windy")
    if (windRes != 0) {
        drawVectorDrawable(canvas, context, windRes, RectF(cardRect.left + pad, cardRect.top + pad, cardRect.left + pad + (scaleFactor * 12f), cardRect.top + pad + (scaleFactor * 12f)), secondaryText)
        canvas.drawText("WIND", cardRect.left + pad + (scaleFactor * 15f), cardRect.top + pad + (scaleFactor * 9f), labelPaint)
    } else {
        canvas.drawText("WIND", cardRect.left + pad, cardRect.top + pad + (scaleFactor * 8f), labelPaint)
    }
    canvas.drawText("${weather.windSpeedKmH.toInt()} km/h", cardRect.left + pad, cardRect.top + pad + (scaleFactor * 22f), valPaint)

    // Top-Right: HUMIDITY with icon
    val humidRes = getDrawableResId(context, "ic_humidity")
    val rLabel = Paint(labelPaint).apply { textAlign = Paint.Align.RIGHT }
    val rVal = Paint(valPaint).apply { textAlign = Paint.Align.RIGHT }
    if (humidRes != 0) {
        drawVectorDrawable(canvas, context, humidRes, RectF(cardRect.right - pad - (scaleFactor * 52f), cardRect.top + pad, cardRect.right - pad - (scaleFactor * 42f), cardRect.top + pad + (scaleFactor * 12f)), secondaryText)
    }
    canvas.drawText("HUMIDITY", cardRect.right - pad, cardRect.top + pad + (scaleFactor * 8f), rLabel)
    canvas.drawText("${weather.humidity}%", cardRect.right - pad, cardRect.top + pad + (scaleFactor * 22f), rVal)

    // Bottom-Left: RAIN CHANCE with icon
    val rainRes = getDrawableResId(context, "ic_rain_chance")
    if (rainRes != 0) {
        drawVectorDrawable(canvas, context, rainRes, RectF(cardRect.left + pad, cardRect.bottom - pad - (scaleFactor * 24f), cardRect.left + pad + (scaleFactor * 12f), cardRect.bottom - pad - (scaleFactor * 12f)), secondaryText)
        canvas.drawText("RAIN", cardRect.left + pad + (scaleFactor * 15f), cardRect.bottom - pad - (scaleFactor * 14f), labelPaint)
    } else {
        canvas.drawText("RAIN", cardRect.left + pad, cardRect.bottom - pad - (scaleFactor * 14f), labelPaint)
    }
    canvas.drawText("${weather.rainChance}%", cardRect.left + pad, cardRect.bottom - pad, valPaint)

    // Bottom-Right: UV INDEX with icon
    val uvRes = getDrawableResId(context, "ic_uv_index")
    if (uvRes != 0) {
        drawVectorDrawable(canvas, context, uvRes, RectF(cardRect.right - pad - (scaleFactor * 48f), cardRect.bottom - pad - (scaleFactor * 24f), cardRect.right - pad - (scaleFactor * 38f), cardRect.bottom - pad - (scaleFactor * 12f)), secondaryText)
    }
    canvas.drawText("UV INDEX", cardRect.right - pad, cardRect.bottom - pad - (scaleFactor * 14f), rLabel)
    canvas.drawText(String.format("%.1f", weather.uvIndex), cardRect.right - pad, cardRect.bottom - pad, rVal)

    // Center: Icon & Temp
    val cx = cardRect.centerX()
    val cy = cardRect.centerY()
    val centerIconSize = scaleFactor * 26f
    val iconRect = RectF(cx - (centerIconSize / 2f), cy - (scaleFactor * 27f), cx + (centerIconSize / 2f), cy - (scaleFactor * 1f))
    drawWeatherIcon(canvas, context, weather.weatherCode, iconRect, accentColor, isNight)

    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 32f
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(WeatherPreferences.formatTemp(weather.currentTemp, unit), cx, cy + (scaleFactor * 18f), tempPaint)

    val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 9f
        typeface = getSlateFont(context, weight = 600)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(weather.cityName, cx, cy + (scaleFactor * 30f), cityPaint)

    return bitmap
}

// 9. Weather Metro Trio (3x1)
fun generateWeatherMetroTrioBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val accentColor = config.accentColorHex.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 3.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        var cardH = h - (margin * 2f)
        var cardW = cardH * targetRatio
        if (cardW > w - (margin * 2f)) {
            cardW = w - (margin * 2f)
            cardH = cardW / targetRatio
        }
        val leftX = (w - cardW) / 2f
        val topY = (h - cardH) / 2f
        RectF(leftX, topY, leftX + cardW, topY + cardH)
    }

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val weather = WeatherPreferences.getCachedWeatherData(context)
    val unit = WeatherPreferences.getUnit(context)
    val panelW = cardRect.width() / 3f

    val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(18, 0, 0, 0) else Color.argb(26, 255, 255, 255)
        strokeWidth = scaleFactor * 1f
    }
    canvas.drawLine(cardRect.left + panelW, cardRect.top + (scaleFactor * 10f), cardRect.left + panelW, cardRect.bottom - (scaleFactor * 10f), divPaint)
    canvas.drawLine(cardRect.left + (panelW * 2f), cardRect.top + (scaleFactor * 10f), cardRect.left + (panelW * 2f), cardRect.bottom - (scaleFactor * 10f), divPaint)

    val days = weather.dailyForecast.take(3)
    val labels = listOf("TODAY", "TOMORROW", days.getOrNull(2)?.dayLabel?.uppercase() ?: "DAY")

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 9f
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }
    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 16f
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
    }

    for (i in 0 until 3) {
        val px = cardRect.left + (i * panelW) + (panelW / 2f)
        val d = days.getOrNull(i) ?: DailyForecastItem("Day", 20f, 14f, 1, 0)

        canvas.drawText(labels[i], px, cardRect.top + (scaleFactor * 18f), titlePaint)

        val iconSize = scaleFactor * 20f
        val iRect = RectF(px - (iconSize / 2f), cardRect.centerY() - (iconSize / 2f), px + (iconSize / 2f), cardRect.centerY() + (iconSize / 2f))
        drawWeatherIcon(canvas, context, d.weatherCode, iRect, accentColor, isNight = false)

        canvas.drawText("${WeatherPreferences.formatTempValue(d.maxTemp, unit)}°", px, cardRect.bottom - (scaleFactor * 10f), tempPaint)
    }

    return bitmap
}

// 10. Micro Weather (1x1)
fun generateWeatherMicroBitmap(
    context: Context,
    config: SlateWidgetConfig,
    isResponsive: Boolean,
    wDp: Int,
    hDp: Int,
    widgetId: Int
): Bitmap {
    val (bitmap, canvas, scaleFactor) = createSupersampledCanvas(wDp, hDp, context)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val isLight = config.themeMode == "LIGHT"
    val bgColor = getSafeBgColor(config)
    val primaryText = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    val secondaryText = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#99FFFFFF")
    val accentColor = config.accentColorHex.toInt()

    val margin = scaleFactor * 1.5f
    val targetRatio = 1.0f
    val cardRect = if (isResponsive) {
        RectF(margin, margin, w - margin, h - margin)
    } else {
        val size = minOf(w - (margin * 2f), h - (margin * 2f))
        val leftX = (w - size) / 2f
        val topY = (h - size) / 2f
        RectF(leftX, topY, leftX + size, topY + size)
    }

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val weather = WeatherPreferences.getCachedWeatherData(context)
    val unit = WeatherPreferences.getUnit(context)
    val isNight = isNightTime(weather)
    val pad = scaleFactor * 10f

    val iconSize = scaleFactor * 24f
    val iconRect = RectF(cardRect.right - pad - iconSize, cardRect.top + pad, cardRect.right - pad, cardRect.top + pad + iconSize)
    drawWeatherIcon(canvas, context, weather.weatherCode, iconRect, accentColor, isNight)

    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 28f
        typeface = getSlateFont(context, weight = 800)
    }
    canvas.drawText(WeatherPreferences.formatTemp(weather.currentTemp, unit), cardRect.left + pad, cardRect.centerY() + (scaleFactor * 8f), tempPaint)

    val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 8.5f
        typeface = getSlateFont(context, weight = 700)
    }
    canvas.drawText(weather.cityName, cardRect.left + pad, cardRect.bottom - pad, cityPaint)

    return bitmap
}