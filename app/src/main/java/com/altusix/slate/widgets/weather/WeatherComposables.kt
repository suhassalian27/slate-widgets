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

// 1. Weather Horizon (4x2 / Editorial Forecast)
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

    val cardW = cardRect.width()
    val cardH = cardRect.height()

    // 1. Proportional UI Scaling: Scales text and icons accurately in both Fixed & Responsive modes
    val baseRefW = scaleFactor * 260f
    val baseRefH = scaleFactor * 130f
    val uiScale = minOf(cardW / baseRefW, cardH / baseRefH).coerceIn(0.6f, 1.45f)

    val outerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(minOf(cardW, cardH) / 2f)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val weather = WeatherPreferences.getCachedWeatherData(context)
    val unit = WeatherPreferences.getUnit(context)

    val padX = (cardW * 0.05f).coerceIn(scaleFactor * 12f, scaleFactor * 20f)
    val padY = (cardH * 0.07f).coerceIn(scaleFactor * 8f, scaleFactor * 16f)

    // 2. Right-Anchored & Bounded Forecast Table
    val rightEnd = cardRect.right - padX
    val idealForecastW = scaleFactor * 140f * uiScale
    val maxAllowedForecastW = (cardW - (padX * 2f)) * 0.46f
    val forecastW = minOf(idealForecastW, maxAllowedForecastW)
    val forecastStartX = rightEnd - forecastW

    // 3. Divider Bar: Positioned comfortably to the right (giving ~52-54% space to current conditions)
    val dividerX = forecastStartX - (scaleFactor * 14f * uiScale)
    val showDivider = cardW >= scaleFactor * 170f
    if (showDivider) {
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.argb(20, 0, 0, 0) else Color.argb(28, 255, 255, 255)
            strokeWidth = scaleFactor * 1f
        }
        canvas.drawLine(dividerX, cardRect.top + padY, dividerX, cardRect.bottom - padY, dividerPaint)
    }

    // -------------------------------------------------------------------------
    // LEFT SECTION: Current Weather
    // -------------------------------------------------------------------------
    val maxLeftW = if (showDivider) dividerX - (cardRect.left + padX) - (scaleFactor * 10f) else cardW * 0.48f

    val cityTextSize = (scaleFactor * 11f * uiScale).coerceIn(scaleFactor * 7.5f, scaleFactor * 13f)
    val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = cityTextSize
        typeface = getSlateFont(context, weight = 700)
        letterSpacing = 0.06f
    }
    val cityY = cardRect.top + padY + cityTextSize
    var displayCity = weather.cityName.uppercase()
    if (cityPaint.measureText(displayCity) > maxLeftW) {
        while (displayCity.length > 3 && cityPaint.measureText("$displayCity…") > maxLeftW) {
            displayCity = displayCity.dropLast(1)
        }
        displayCity = "$displayCity…"
    }
    canvas.drawText(displayCity, cardRect.left + padX, cityY, cityPaint)

    val tempTextSize = (scaleFactor * 44f * uiScale).coerceIn(scaleFactor * 22f, scaleFactor * 52f)
    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = tempTextSize
        typeface = getSlateFont(context, weight = 800)
    }
    val tempStr = WeatherPreferences.formatTemp(weather.currentTemp, unit)
    val tempY = cityY + (tempTextSize * 0.94f) + (scaleFactor * 2f * uiScale)
    canvas.drawText(tempStr, cardRect.left + padX, tempY, tempPaint)

    // High / Low Pill (pinned to bottom)
    val hiLoTextSize = (scaleFactor * 11f * uiScale).coerceIn(scaleFactor * 7.5f, scaleFactor * 12.5f)
    val hiLoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = hiLoTextSize
        typeface = getSlateFont(context, weight = 600)
    }
    val hiLoY = cardRect.bottom - padY
    val hiLoStr = "H: ${WeatherPreferences.formatTemp(weather.tempMax, unit)}  L: ${WeatherPreferences.formatTemp(weather.tempMin, unit)}"
    canvas.drawText(hiLoStr, cardRect.left + padX, hiLoY, hiLoPaint)

    // Condition Text (anchored above High/Low with collision check)
    val condTextSize = (scaleFactor * 13f * uiScale).coerceIn(scaleFactor * 8.5f, scaleFactor * 15f)
    val condPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = condTextSize
        typeface = getSlateFont(context, weight = 600)
    }
    val condY = hiLoY - hiLoTextSize - (scaleFactor * 4f * uiScale)
    if (condY > tempY + (scaleFactor * 2f)) {
        var condStr = weather.conditionText
        if (condPaint.measureText(condStr) > maxLeftW) {
            while (condStr.length > 3 && condPaint.measureText("$condStr…") > maxLeftW) {
                condStr = condStr.dropLast(1)
            }
            condStr = "$condStr…"
        }
        canvas.drawText(condStr, cardRect.left + padX, condY, condPaint)
    }

    // -------------------------------------------------------------------------
    // RIGHT SECTION: 5-Day Forecast Table (Strict Right-Alignment & No Overlap)
    // -------------------------------------------------------------------------
    if (forecastW > scaleFactor * 50f) {
        val daysCount = 5
        val dailyList = weather.dailyForecast.take(daysCount)
        val rowH = (cardRect.height() - (padY * 2f)) / daysCount.toFloat()

        val dayTextSize = (scaleFactor * 11f * uiScale).coerceIn(scaleFactor * 7.5f, scaleFactor * 12.5f)
        val rangeTextSize = (scaleFactor * 11f * uiScale).coerceIn(scaleFactor * 7.5f, scaleFactor * 12.5f)
        val rainTextSize = (scaleFactor * 8.5f * uiScale).coerceIn(scaleFactor * 6.5f, scaleFactor * 9.5f)

        val dayNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryText
            textSize = dayTextSize
            typeface = getSlateFont(context, weight = 600)
        }
        val rangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryText
            textSize = rangeTextSize
            typeface = getSlateFont(context, weight = 600)
            textAlign = Paint.Align.RIGHT
        }
        val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38ACFF")
            textSize = rainTextSize
            typeface = getSlateFont(context, weight = 700)
        }

        // Sub-column measurements for perfectly aligned temperatures
        val minTempColW = rangePaint.measureText("19°")
        val maxTempColW = rangePaint.measureText("29°")
        val tempColGap = scaleFactor * 6f * uiScale
        val tempBlockLeft = rightEnd - minTempColW - tempColGap - maxTempColW

        val maxDayW = dayNamePaint.measureText("Today")
        val dayBlockRight = forecastStartX + maxDayW

        // Middle clearance calculation: guarantees elements never collide
        val middleSpace = maxOf(0f, tempBlockLeft - dayBlockRight - (scaleFactor * 6f * uiScale))
        val desiredIconSize = (rowH * 0.46f).coerceIn(scaleFactor * 10f, scaleFactor * 18f)
        val iconSize = minOf(desiredIconSize, middleSpace)

        val sampleRainW = rainPaint.measureText("75%")
        val neededForRain = iconSize + (scaleFactor * 3f) + sampleRainW
        val canShowRain = middleSpace >= (neededForRain + scaleFactor * 4f * uiScale)

        for (i in dailyList.indices) {
            val item = dailyList[i]
            val cy = cardRect.top + padY + (i * rowH) + (rowH / 2f)

            // 1. Day Label (Left-aligned)
            canvas.drawText(item.dayLabel, forecastStartX, cy + (dayTextSize * 0.35f), dayNamePaint)

            // 2. Weather Icon & Rain % (Centered in the middle clearance zone)
            if (middleSpace >= iconSize) {
                if (canShowRain && item.rainProb >= 20) {
                    val rainStr = "${item.rainProb}%"
                    val totalW = iconSize + (scaleFactor * 3f) + rainPaint.measureText(rainStr)
                    val groupLeft = dayBlockRight + (middleSpace - totalW) / 2f + (scaleFactor * 3f * uiScale)
                    val iconRect = RectF(groupLeft, cy - (iconSize / 2f), groupLeft + iconSize, cy + (iconSize / 2f))
                    drawWeatherIcon(canvas, context, item.weatherCode, iconRect, accentColor, isNight = false)
                    canvas.drawText(rainStr, iconRect.right + (scaleFactor * 3f), cy + (rainTextSize * 0.35f), rainPaint)
                } else {
                    val iconCenterX = dayBlockRight + (middleSpace / 2f) + (scaleFactor * 3f * uiScale)
                    val iconRect = RectF(iconCenterX - (iconSize / 2f), cy - (iconSize / 2f), iconCenterX + (iconSize / 2f), cy + (iconSize / 2f))
                    drawWeatherIcon(canvas, context, item.weatherCode, iconRect, accentColor, isNight = false)
                }
            }

            // 3. Temperatures (Strictly right-aligned with tabular columns)
            val maxStr = "${WeatherPreferences.formatTempValue(item.maxTemp, unit)}°"
            val minStr = "${WeatherPreferences.formatTempValue(item.minTemp, unit)}°"
            canvas.drawText(maxStr, rightEnd - minTempColW - tempColGap, cy + (rangeTextSize * 0.35f), rangePaint)
            canvas.drawText(minStr, rightEnd, cy + (rangeTextSize * 0.35f), rangePaint)
        }
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

    // Calibrated proportional scalar anchored to standard 4x2 widget bounds (300dp x 145dp)
    val baseRefH = scaleFactor * 145f
    val baseRefW = scaleFactor * 300f
    val propScale = if (isResponsive) {
        minOf(cardRect.width() / baseRefW, cardRect.height() / baseRefH)
    } else {
        cardRect.height() / baseRefH
    }.coerceIn(0.55f, 1.18f)
    val s = scaleFactor * propScale

    val outerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(minOf(cardRect.width(), cardRect.height()) / 2f)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val weather = WeatherPreferences.getCachedWeatherData(context)
    val unit = WeatherPreferences.getUnit(context)
    val isNight = isNightTime(weather)

    val pad = s * 8f
    val gap = s * 8f
    val innerCardBg = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    val concentricRadius = (outerRadius - pad).coerceAtLeast(s * 6f)
    val sq = s * 10f

    // -------------------------------------------------------------------------
    // LEFT MAIN CARD
    // -------------------------------------------------------------------------
    val leftW = (cardRect.width() - (pad * 2f) - gap) * 0.52f
    val leftRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.left + pad + leftW, cardRect.bottom - pad)
    val leftRadii = floatArrayOf(concentricRadius, concentricRadius, sq, sq, sq, sq, concentricRadius, concentricRadius)
    val leftPath = Path().apply { addRoundRect(leftRect, leftRadii, Path.Direction.CW) }
    canvas.drawPath(leftPath, tilePaint)

    val innerPad = s * 12f

    val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = s * 10f
        typeface = getSlateFont(context, weight = 700)
    }
    canvas.drawText(weather.cityName.uppercase(), leftRect.left + innerPad, leftRect.top + innerPad + (s * 8f), cityPaint)

    val mainIconSize = s * 30f
    val mainIconRect = RectF(
        leftRect.right - innerPad - mainIconSize,
        leftRect.top + innerPad,
        leftRect.right - innerPad,
        leftRect.top + innerPad + mainIconSize
    )
    drawWeatherIcon(canvas, context, weather.weatherCode, mainIconRect, accentColor, isNight)

    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = s * 38f
        typeface = getSlateFont(context, weight = 800)
    }
    canvas.drawText(WeatherPreferences.formatTemp(weather.currentTemp, unit), leftRect.left + innerPad, leftRect.top + (s * 64f), tempPaint)

    val condPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = s * 12f
        typeface = getSlateFont(context, weight = 600)
    }
    canvas.drawText(weather.conditionText, leftRect.left + innerPad, leftRect.bottom - innerPad - (s * 11f), condPaint)

    val hlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = s * 9.5f
        typeface = getSlateFont(context, weight = 600)
    }
    canvas.drawText("H: ${WeatherPreferences.formatTemp(weather.tempMax, unit)}  L: ${WeatherPreferences.formatTemp(weather.tempMin, unit)}", leftRect.left + innerPad, leftRect.bottom - innerPad, hlPaint)

    // -------------------------------------------------------------------------
    // RIGHT SUB-CARDS (Metrics)
    // -------------------------------------------------------------------------
    val rightLeft = leftRect.right + gap
    val rightW = cardRect.right - pad - rightLeft
    val subH = (cardRect.height() - (pad * 2f) - gap) / 2f

    val metricHalfW = rightW / 2f
    val metricIconSize = s * 10f

    // Dynamically sized label paint to ensure "FEELS LIKE" never collides with Col 2
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        typeface = getSlateFont(context, weight = 700)
    }
    val maxLabelW = metricHalfW - (s * 16f) - metricIconSize
    var labelSize = s * 7.5f
    labelPaint.textSize = labelSize
    while (labelPaint.measureText("FEELS LIKE") > maxLabelW && labelSize > s * 5.8f) {
        labelSize -= s * 0.3f
        labelPaint.textSize = labelSize
    }

    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = s * 16f
        typeface = getSlateFont(context, weight = 700)
    }

    // RIGHT TOP CARD: Feels Like & Humidity
    val topRect = RectF(rightLeft, cardRect.top + pad, cardRect.right - pad, cardRect.top + pad + subH)
    val topRadii = floatArrayOf(sq, sq, concentricRadius, concentricRadius, sq, sq, sq, sq)
    val topPath = Path().apply { addRoundRect(topRect, topRadii, Path.Direction.CW) }
    canvas.drawPath(topPath, tilePaint)

    // Col 1: Feels Like
    val thermoRes = getDrawableResId(context, "ic_thermometer")
    if (thermoRes != 0) {
        drawVectorDrawable(canvas, context, thermoRes, RectF(topRect.left + (s * 8f), topRect.top + (s * 7.5f), topRect.left + (s * 8f) + metricIconSize, topRect.top + (s * 7.5f) + metricIconSize), secondaryText)
        canvas.drawText("FEELS LIKE", topRect.left + (s * 9f) + metricIconSize + (s * 2.5f), topRect.top + (s * 15.5f), labelPaint)
    } else {
        canvas.drawText("FEELS LIKE", topRect.left + (s * 8f), topRect.top + (s * 15.5f), labelPaint)
    }
    canvas.drawText(WeatherPreferences.formatTemp(weather.feelsLike, unit), topRect.left + (s * 8f), topRect.top + (s * 35f), valuePaint)

    // Col 2: Humidity
    val humidRes = getDrawableResId(context, "ic_humidity")
    if (humidRes != 0) {
        drawVectorDrawable(canvas, context, humidRes, RectF(topRect.left + metricHalfW + (s * 4f), topRect.top + (s * 7.5f), topRect.left + metricHalfW + (s * 4f) + metricIconSize, topRect.top + (s * 7.5f) + metricIconSize), secondaryText)
        canvas.drawText("HUMIDITY", topRect.left + metricHalfW + (s * 5f) + metricIconSize + (s * 2.5f), topRect.top + (s * 15.5f), labelPaint)
    } else {
        canvas.drawText("HUMIDITY", topRect.left + metricHalfW + (s * 4f), topRect.top + (s * 15.5f), labelPaint)
    }
    canvas.drawText("${weather.humidity}%", topRect.left + metricHalfW + (s * 4f), topRect.top + (s * 35f), valuePaint)

    // RIGHT BOTTOM CARD: Wind & UV Index
    val btmRect = RectF(rightLeft, topRect.bottom + gap, cardRect.right - pad, cardRect.bottom - pad)
    val btmRadii = floatArrayOf(sq, sq, sq, sq, concentricRadius, concentricRadius, sq, sq)
    val btmPath = Path().apply { addRoundRect(btmRect, btmRadii, Path.Direction.CW) }
    canvas.drawPath(btmPath, tilePaint)

    // Col 1: Wind
    val windRes = getDrawableResId(context, "ic_weather_windy")
    if (windRes != 0) {
        drawVectorDrawable(canvas, context, windRes, RectF(btmRect.left + (s * 8f), btmRect.top + (s * 7.5f), btmRect.left + (s * 8f) + metricIconSize, btmRect.top + (s * 7.5f) + metricIconSize), secondaryText)
        canvas.drawText("WIND", btmRect.left + (s * 9f) + metricIconSize + (s * 2.5f), btmRect.top + (s * 15.5f), labelPaint)
    } else {
        canvas.drawText("WIND", btmRect.left + (s * 8f), btmRect.top + (s * 15.5f), labelPaint)
    }
    canvas.drawText("${weather.windSpeedKmH.toInt()} km/h", btmRect.left + (s * 8f), btmRect.top + (s * 35f), valuePaint)

    // Col 2: UV Index
    val uvRes = getDrawableResId(context, "ic_uv_index")
    if (uvRes != 0) {
        drawVectorDrawable(canvas, context, uvRes, RectF(btmRect.left + metricHalfW + (s * 4f), btmRect.top + (s * 7.5f), btmRect.left + metricHalfW + (s * 4f) + metricIconSize, btmRect.top + (s * 7.5f) + metricIconSize), secondaryText)
        canvas.drawText("UV INDEX", btmRect.left + metricHalfW + (s * 5f) + metricIconSize + (s * 2.5f), btmRect.top + (s * 15.5f), labelPaint)
    } else {
        canvas.drawText("UV INDEX", btmRect.left + metricHalfW + (s * 4f), btmRect.top + (s * 15.5f), labelPaint)
    }
    canvas.drawText(String.format("%.1f", weather.uvIndex), btmRect.left + metricHalfW + (s * 4f), btmRect.top + (s * 35f), valuePaint)

    return bitmap
}

// 3. Weather Daylight Arc (2x2 / Sun Daylight Progress Arc)
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

    val cardW = cardRect.width()
    val cardH = cardRect.height()

    // 1. Dual-Axis Proportional Scalar
    val propScale = ((cardW + cardH) / (scaleFactor * 280f)).coerceIn(0.65f, 1.65f)
    val s = scaleFactor * propScale

    val outerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(minOf(cardW, cardH) / 2f)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val weather = WeatherPreferences.getCachedWeatherData(context)
    val unit = WeatherPreferences.getUnit(context)
    val isNight = isNightTime(weather)

    val cx = cardRect.centerX()
    val padX = (cardW * 0.08f).coerceIn(s * 10f, s * 18f)

    // -------------------------------------------------------------------------
    // 1. TOP HEADER: City Name
    // -------------------------------------------------------------------------
    val cityTextSize = (s * 10f).coerceIn(s * 7.5f, s * 13f)
    val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = cityTextSize
        typeface = getSlateFont(context, weight = 700)
        letterSpacing = 0.08f
        textAlign = Paint.Align.CENTER
    }
    val cityY = cardRect.top + (cardH * 0.095f) + cityTextSize
    canvas.drawText(weather.cityName.uppercase(), cx, cityY, cityPaint)

    // -------------------------------------------------------------------------
    // 2. DAYLIGHT ARC & HORIZON (Lowered Placement)
    // -------------------------------------------------------------------------
    val horizonY = cardRect.top + (cardH * 0.57f)
    val maxRadiusByWidth = (cardW / 2f) - padX
    val maxRadiusByHeight = (horizonY - cityY - (s * 10f))
    val arcRadius = minOf(maxRadiusByWidth, maxRadiusByHeight).coerceAtLeast(s * 25f)
    val arcRect = RectF(cx - arcRadius, horizonY - arcRadius, cx + arcRadius, horizonY + arcRadius)

    // Dashed Horizon Line
    val horizonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(22, 0, 0, 0) else Color.argb(32, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = s * 1f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(s * 3.5f, s * 3f), 0f)
    }
    canvas.drawLine(cx - arcRadius - (s * 4f), horizonY, cx + arcRadius + (s * 4f), horizonY, horizonPaint)

    // Track Dome
    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(20, 0, 0, 0) else Color.argb(32, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = s * 3f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawArc(arcRect, 180f, 180f, false, trackPaint)

    // Daylight Progress Calculation
    val cal = Calendar.getInstance()
    val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val sunriseParts = weather.sunrise.split(":")
    val sunsetParts = weather.sunset.split(":")
    val riseMinutes = (sunriseParts.getOrNull(0)?.toIntOrNull() ?: 6) * 60 + (sunriseParts.getOrNull(1)?.toIntOrNull() ?: 0)
    val setMinutes = (sunsetParts.getOrNull(0)?.toIntOrNull() ?: 19) * 60 + (sunsetParts.getOrNull(1)?.toIntOrNull() ?: 30)

    val daylightRatio = ((currentMinutes - riseMinutes).toFloat() / (setMinutes - riseMinutes).toFloat()).coerceIn(0f, 1f)
    val sweepAngle = daylightRatio * 180f

    // Active Daylight Arc
    val activeArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.STROKE
        strokeWidth = s * 3.2f
        strokeCap = Paint.Cap.ROUND
    }
    if (sweepAngle > 0.5f) {
        canvas.drawArc(arcRect, 180f, sweepAngle, false, activeArcPaint)
    }

    // Sun Marker
    val sunAngleRad = Math.toRadians((180.0 + sweepAngle).toDouble())
    val markerX = cx + (arcRadius * Math.cos(sunAngleRad)).toFloat()
    val markerY = horizonY + (arcRadius * Math.sin(sunAngleRad)).toFloat()

    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(40, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        style = Paint.Style.FILL
    }
    canvas.drawCircle(markerX, markerY, s * 6.5f, haloPaint)

    val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(markerX, markerY, s * 3.8f, sunPaint)

    val coreHighlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(markerX, markerY, s * 1.5f, coreHighlight)

    // -------------------------------------------------------------------------
    // 3. INSIDE THE DOME: Weather Icon & Temperature
    // -------------------------------------------------------------------------
    val iconSize = (arcRadius * 0.30f).coerceIn(s * 15f, s * 26f)
    val iconCenterY = horizonY - (arcRadius * 0.68f)
    val iconRect = RectF(cx - (iconSize / 2f), iconCenterY - (iconSize / 2f), cx + (iconSize / 2f), iconCenterY + (iconSize / 2f))
    drawWeatherIcon(canvas, context, weather.weatherCode, iconRect, accentColor, isNight)

    val tempTextSize = (arcRadius * 0.46f).coerceIn(s * 24f, s * 42f)
    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = tempTextSize
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(WeatherPreferences.formatTemp(weather.currentTemp, unit), cx, horizonY - (s * 4f), tempPaint)

    // -------------------------------------------------------------------------
    // 4. AT ARC BASE: Sunrise & Sunset Timestamps
    // -------------------------------------------------------------------------
    val timeLabelTextSize = (s * 8.5f).coerceIn(s * 7f, s * 10.5f)
    val timeLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = timeLabelTextSize
        typeface = getSlateFont(context, weight = 600)
    }
    val metricIconSize = s * 10f
    val sunTimeY = horizonY + (s * 14f)

    // Sunrise (Left)
    val sunriseRes = getDrawableResId(context, "ic_sunrise")
    val leftFootX = cx - arcRadius
    if (sunriseRes != 0) {
        val sIconRect = RectF(leftFootX, sunTimeY - metricIconSize + (s * 1f), leftFootX + metricIconSize, sunTimeY + (s * 1f))
        drawVectorDrawable(canvas, context, sunriseRes, sIconRect, secondaryText)
        canvas.drawText(weather.sunrise, leftFootX + metricIconSize + (s * 3.5f), sunTimeY, timeLabelPaint)
    } else {
        canvas.drawText("↑ ${weather.sunrise}", leftFootX, sunTimeY, timeLabelPaint)
    }

    // Sunset (Right)
    val sunsetRes = getDrawableResId(context, "ic_sunset")
    val rightFootX = cx + arcRadius
    val setPaint = Paint(timeLabelPaint).apply { textAlign = Paint.Align.RIGHT }
    if (sunsetRes != 0) {
        val sunsetStrWidth = setPaint.measureText(weather.sunset)
        val sIconRect = RectF(rightFootX - sunsetStrWidth - metricIconSize - (s * 3.5f), sunTimeY - metricIconSize + (s * 1f), rightFootX - sunsetStrWidth - (s * 3.5f), sunTimeY + (s * 1f))
        drawVectorDrawable(canvas, context, sunsetRes, sIconRect, secondaryText)
        canvas.drawText(weather.sunset, rightFootX, sunTimeY, setPaint)
    } else {
        canvas.drawText("↓ ${weather.sunset}", rightFootX, sunTimeY, setPaint)
    }

    // -------------------------------------------------------------------------
    // 5. BOTTOM SECTION: Refined, Subtle Bottom Text Group
    // -------------------------------------------------------------------------
    val condTextSize = (s * 11.5f).coerceIn(s * 8.5f, s * 14.5f)
    val condPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = condTextSize
        typeface = getSlateFont(context, weight = 600)
        textAlign = Paint.Align.CENTER
    }

    val rangeTextSize = (s * 8.5f).coerceIn(s * 6.5f, s * 11f)
    val rangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = rangeTextSize
        typeface = getSlateFont(context, weight = 600)
        textAlign = Paint.Align.CENTER
    }

    // Tightly paired with a subtle gap
    val textGap = s * 3.5f
    val clusterH = condTextSize + textGap + rangeTextSize

    val availableBottom = (cardRect.bottom - (cardH * 0.05f)) - (sunTimeY + (s * 6f))
    val clusterCenterY = (sunTimeY + (s * 6f)) + (availableBottom / 2f)

    val condY = clusterCenterY - (clusterH / 2f) + condTextSize
    canvas.drawText(weather.conditionText, cx, condY, condPaint)

    val rangeY = condY + textGap + (rangeTextSize * 0.88f)
    val rangeStr = "H: ${WeatherPreferences.formatTemp(weather.tempMax, unit)}   L: ${WeatherPreferences.formatTemp(weather.tempMin, unit)}"
    canvas.drawText(rangeStr, cx, rangeY, rangePaint)

    return bitmap
}

// 4. Weather Pill Dock (4x1 / Wide Bar)
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

    val cardW = cardRect.width()
    val cardH = cardRect.height()

    // 1. Proportional Scalar calibrated to standard 4x1 dimensions (~300dp x 75dp)
    val baseRefW = scaleFactor * 300f
    val baseRefH = scaleFactor * 75f
    val propScale = minOf(cardW / baseRefW, cardH / baseRefH).coerceIn(0.60f, 1.45f)
    val s = scaleFactor * propScale

    val outerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(minOf(cardW, cardH) / 2f)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val weather = WeatherPreferences.getCachedWeatherData(context)
    val unit = WeatherPreferences.getUnit(context)
    val isNight = isNightTime(weather)

    // Unified padding: Top, bottom, and right match identically
    val pad = (cardH * 0.095f).coerceIn(s * 6f, s * 8.5f)

    // Calculate exact corner inset at the bottom line to prevent border-radius clipping
    val cornerInset = if (pad < outerRadius && outerRadius > 0f) {
        val dy = outerRadius - pad
        (outerRadius - Math.sqrt((outerRadius * outerRadius - dy * dy).coerceAtLeast(0f).toDouble()).toFloat())
    } else {
        0f
    }
    val leftStartX = cardRect.left + pad + cornerInset + (s * 3.5f)

    // -------------------------------------------------------------------------
    // RIGHT SECTION: Hourly Forecast Pills (Concentric Outer Radius)
    // -------------------------------------------------------------------------
    val hourlyItems = weather.hourlyForecast.drop(1).take(3)
    val pillGap = s * 4f
    val pillW = (s * 34f).coerceIn(s * 24f, s * 42f)
    val totalPillsW = (hourlyItems.size * pillW) + ((hourlyItems.size - 1) * pillGap)

    val pillsEndX = cardRect.right - pad
    val pillsStartX = pillsEndX - totalPillsW

    val pillTop = cardRect.top + pad
    val pillBottom = cardRect.bottom - pad
    val pillH = pillBottom - pillTop

    val hourLabelTextSize = (pillH * 0.18f).coerceIn(s * 6.5f, s * 10f)
    val hourLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = hourLabelTextSize
        typeface = getSlateFont(context, weight = 600)
        textAlign = Paint.Align.CENTER
    }

    val hourTempTextSize = (pillH * 0.22f).coerceIn(s * 8f, s * 12.5f)
    val hourTempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = hourTempTextSize
        typeface = getSlateFont(context, weight = 700)
        textAlign = Paint.Align.CENTER
    }

    val pillIconSize = (pillH * 0.28f).coerceIn(s * 12f, s * 18f)
    val innerCardBg = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
    val pillTilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    val concentricRadius = (outerRadius - pad).coerceAtLeast(s * 6f).coerceAtMost(pillH / 2f)
    val innerRadius = (s * 7.5f).coerceIn(s * 5.5f, s * 9.5f)

    for (i in hourlyItems.indices) {
        val item = hourlyItems[i]
        val px = pillsStartX + i * (pillW + pillGap)
        val pRect = RectF(px, pillTop, px + pillW, pillBottom)

        if (i == hourlyItems.lastIndex) {
            // Concentric corner curvature matching widget's outer boundary
            val radii = floatArrayOf(
                innerRadius, innerRadius,
                concentricRadius, concentricRadius,
                concentricRadius, concentricRadius,
                innerRadius, innerRadius
            )
            val path = Path().apply { addRoundRect(pRect, radii, Path.Direction.CW) }
            canvas.drawPath(path, pillTilePaint)
        } else {
            canvas.drawRoundRect(pRect, innerRadius, innerRadius, pillTilePaint)
        }

        canvas.drawText(item.timeLabel, pRect.centerX(), pRect.top + (pillH * 0.23f), hourLabelPaint)

        val pillIconRect = RectF(
            pRect.centerX() - (pillIconSize / 2f),
            pRect.centerY() - (pillIconSize / 2f),
            pRect.centerX() + (pillIconSize / 2f),
            pRect.centerY() + (pillIconSize / 2f)
        )
        drawWeatherIcon(canvas, context, item.weatherCode, pillIconRect, accentColor, isNight = false)

        canvas.drawText(WeatherPreferences.formatTemp(item.temp, unit), pRect.centerX(), pRect.bottom - (pillH * 0.12f), hourTempPaint)
    }

    // -------------------------------------------------------------------------
    // LEFT TOP SECTION: Weather Vector Icon + Hero Degree (Pinned to Top)
    // -------------------------------------------------------------------------
    val maxLeftW = pillsStartX - leftStartX - (s * 10f)

    val iconSize = (cardH * 0.35f).coerceIn(s * 18f, s * 30f)
    val tempTextSize = (cardH * 0.40f).coerceIn(s * 20f, s * 36f)
    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = tempTextSize
        typeface = getSlateFont(context, weight = 800)
    }
    val tempStr = WeatherPreferences.formatTemp(weather.currentTemp, unit)

    val topRowH = maxOf(iconSize, tempTextSize * 0.86f)
    val iconRect = RectF(
        leftStartX,
        cardRect.top + pad + ((topRowH - iconSize) / 2f),
        leftStartX + iconSize,
        cardRect.top + pad + ((topRowH + iconSize) / 2f)
    )
    drawWeatherIcon(canvas, context, weather.weatherCode, iconRect, accentColor, isNight)

    val tempX = iconRect.right + (s * 6f)
    val tempY = cardRect.top + pad + (topRowH / 2f) + (tempTextSize * 0.35f)
    canvas.drawText(tempStr, tempX, tempY, tempPaint)

    // -------------------------------------------------------------------------
    // LEFT BOTTOM SECTION: City (Upper) & Condition + Range (Lower) (Pinned to Bottom)
    // -------------------------------------------------------------------------
    var condTextSize = (s * 9.2f).coerceIn(s * 7.2f, s * 11.5f)
    val condPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = condTextSize
        typeface = getSlateFont(context, weight = 600)
    }

    var cityTextSize = (s * 11.5f).coerceIn(s * 8.5f, s * 14f)
    val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = cityTextSize
        typeface = getSlateFont(context, weight = 700)
    }

    val condStr = "${weather.conditionText}  •  H: ${WeatherPreferences.formatTemp(weather.tempMax, unit)}  L: ${WeatherPreferences.formatTemp(weather.tempMin, unit)}"
    val condY = cardRect.bottom - pad
    val textGap = (cardH * 0.045f).coerceIn(s * 2.5f, s * 4f)
    val cityY = condY - condTextSize - textGap

    // Auto-scale City text size to prevent truncation when space exists
    var displayCity = weather.cityName
    while (cityPaint.measureText(displayCity) > maxLeftW && cityTextSize > s * 7.5f) {
        cityTextSize -= s * 0.3f
        cityPaint.textSize = cityTextSize
    }
    if (cityPaint.measureText(displayCity) > maxLeftW) {
        while (displayCity.length > 3 && cityPaint.measureText("$displayCity…") > maxLeftW) {
            displayCity = displayCity.dropLast(1)
        }
        displayCity = "$displayCity…"
    }
    canvas.drawText(displayCity, leftStartX, cityY, cityPaint)

    // Auto-scale Condition + Range string smoothly down to fit width
    var displayCond = condStr
    while (condPaint.measureText(displayCond) > maxLeftW && condTextSize > s * 6.5f) {
        condTextSize -= s * 0.3f
        condPaint.textSize = condTextSize
    }
    if (condPaint.measureText(displayCond) > maxLeftW) {
        while (displayCond.length > 3 && condPaint.measureText("$displayCond…") > maxLeftW) {
            displayCond = displayCond.dropLast(1)
        }
        displayCond = "$displayCond…"
    }
    canvas.drawText(displayCond, leftStartX, condY, condPaint)

    return bitmap
}

// 5. Weather Editorial Capsule (2x2 / Typographic Poster)
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

    val cardW = cardRect.width()
    val cardH = cardRect.height()

    // 1. Dual-Axis Proportional Scalar
    val propScale = ((cardW + cardH) / (scaleFactor * 280f)).coerceIn(0.65f, 1.65f)
    val s = scaleFactor * propScale

    val outerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(minOf(cardW, cardH) / 2f)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val weather = WeatherPreferences.getCachedWeatherData(context)
    val unit = WeatherPreferences.getUnit(context)
    val isNight = isNightTime(weather)

    val padX = (cardW * 0.085f).coerceIn(s * 11f, s * 20f)
    val padY = (cardH * 0.085f).coerceIn(s * 11f, s * 20f)

    // -------------------------------------------------------------------------
    // 1. TOP HEADER: Condition Label & Top-Right Weather Icon
    // -------------------------------------------------------------------------
    val iconSize = (s * 28f).coerceIn(s * 18f, s * 42f)
    val iconRect = RectF(
        cardRect.right - padX - iconSize,
        cardRect.top + padY,
        cardRect.right - padX,
        cardRect.top + padY + iconSize
    )
    drawWeatherIcon(canvas, context, weather.weatherCode, iconRect, accentColor, isNight)

    val maxHeaderW = iconRect.left - (cardRect.left + padX) - (s * 4f)
    var headerTextSize = (s * 9.5f).coerceIn(s * 7f, s * 13f)
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = headerTextSize
        typeface = getSlateFont(context, weight = 700)
        letterSpacing = if (maxHeaderW < s * 60f) 0.04f else 0.08f
    }

    var displayCondition = weather.conditionText.uppercase()

    // Smart Auto-Scale: Shrink text down to avoid premature "..." truncation
    while (headerPaint.measureText(displayCondition) > maxHeaderW && headerTextSize > s * 5.8f) {
        headerTextSize -= s * 0.35f
        headerPaint.textSize = headerTextSize
    }

    // Ellipsis fallback only if text still cannot fit at minimum size
    if (headerPaint.measureText(displayCondition) > maxHeaderW) {
        while (displayCondition.length > 3 && headerPaint.measureText("$displayCondition…") > maxHeaderW) {
            displayCondition = displayCondition.dropLast(1)
        }
        displayCondition = "$displayCondition…"
    }

    val headerY = cardRect.top + padY + headerTextSize
    canvas.drawText(displayCondition, cardRect.left + padX, headerY, headerPaint)

    // -------------------------------------------------------------------------
    // 2. BOTTOM GROUP: City Name & High/Low (Tight Cohesive Pairing)
    // -------------------------------------------------------------------------
    val rangeTextSize = (s * 9.2f).coerceIn(s * 7f, s * 12f)
    val rangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = rangeTextSize
        typeface = getSlateFont(context, weight = 600)
    }
    val rangeY = cardRect.bottom - padY
    val rangeStr = "H: ${WeatherPreferences.formatTemp(weather.tempMax, unit)}  •  L: ${WeatherPreferences.formatTemp(weather.tempMin, unit)}"

    val cityTextSize = (s * 13f).coerceIn(s * 9f, s * 16.5f)
    val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = cityTextSize
        typeface = getSlateFont(context, weight = 700)
    }
    val textGap = s * 3.5f
    val cityY = rangeY - rangeTextSize - textGap

    val maxCityW = cardW - (padX * 2f)
    var displayCity = weather.cityName
    if (cityPaint.measureText(displayCity) > maxCityW) {
        while (displayCity.length > 3 && cityPaint.measureText("$displayCity…") > maxCityW) {
            displayCity = displayCity.dropLast(1)
        }
        displayCity = "$displayCity…"
    }
    canvas.drawText(displayCity, cardRect.left + padX, cityY, cityPaint)
    canvas.drawText(rangeStr, cardRect.left + padX, rangeY, rangePaint)

    // -------------------------------------------------------------------------
    // 3. HERO TEMPERATURE (Trimmed down by ~15% for balanced proportions)
    // -------------------------------------------------------------------------
    val availableMiddleH = (cityY - cityTextSize) - headerY - (s * 5f)
    var tempTextSize = (s * 44f).coerceIn(s * 24f, s * 56f)
    if (tempTextSize * 0.94f > availableMiddleH) {
        tempTextSize = (availableMiddleH * 0.88f).coerceAtLeast(s * 18f)
    }

    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = tempTextSize
        typeface = getSlateFont(context, weight = 800)
    }
    val tempStr = WeatherPreferences.formatTemp(weather.currentTemp, unit)
    val tempY = (headerY + (s * 3f) + (tempTextSize * 0.90f)).coerceAtMost(cityY - cityTextSize - (s * 3.5f))
    canvas.drawText(tempStr, cardRect.left + padX, tempY, tempPaint)

    return bitmap
}

// 6. Weather Minimalist Dual (2x2 / Split Quadrant)
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

    val cardW = cardRect.width()
    val cardH = cardRect.height()

    // 1. Dual-Axis Proportional Scalar: Responds fluidly across small, square, and tall cell sizes
    val propScale = ((cardW + cardH) / (scaleFactor * 280f)).coerceIn(0.65f, 1.65f)
    val s = scaleFactor * propScale

    val outerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(minOf(cardW, cardH) / 2f)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val weather = WeatherPreferences.getCachedWeatherData(context)
    val unit = WeatherPreferences.getUnit(context)
    val isNight = isNightTime(weather)

    val padX = (cardW * 0.085f).coerceIn(s * 10f, s * 20f)
    val padY = (cardH * 0.09f).coerceIn(s * 10f, s * 20f)

    // 2. Proportional Division: Left 44% (Icon) / Right 56% (Text) for optimal clearance
    val dividerX = cardRect.left + (cardW * 0.44f)

    // Center Divider Line
    val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(18, 0, 0, 0) else Color.argb(26, 255, 255, 255)
        strokeWidth = s * 1f
    }
    canvas.drawLine(dividerX, cardRect.top + padY, dividerX, cardRect.bottom - padY, divPaint)

    // -------------------------------------------------------------------------
    // LEFT PANE: Weather Vector Icon (Centered Vertically & Horizontally)
    // -------------------------------------------------------------------------
    val leftPaneW = dividerX - cardRect.left
    val iconSize = minOf(leftPaneW * 0.65f, cardH * 0.40f, s * 44f).coerceAtLeast(s * 18f)
    val iconLeft = cardRect.left + (leftPaneW - iconSize) / 2f
    val iconTop = cardRect.centerY() - (iconSize / 2f)
    val iconRect = RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
    drawWeatherIcon(canvas, context, weather.weatherCode, iconRect, accentColor, isNight)

    // -------------------------------------------------------------------------
    // RIGHT PANE: Top Block (Temp & Condition) + Bottom Block (City & High/Low)
    // -------------------------------------------------------------------------
    val rightX = dividerX + (s * 11f)
    val maxTextW = cardRect.right - padX - rightX

    // Sizing & Paints
    val tempTextSize = (s * 34f).coerceIn(s * 20f, s * 44f)
    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = tempTextSize
        typeface = getSlateFont(context, weight = 800)
    }

    var condTextSize = (s * 11.5f).coerceIn(s * 8f, s * 14.5f)
    val condPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = condTextSize
        typeface = getSlateFont(context, weight = 600)
    }
    var displayCondition = weather.conditionText
    while (condPaint.measureText(displayCondition) > maxTextW && condTextSize > s * 6.5f) {
        condTextSize -= s * 0.35f
        condPaint.textSize = condTextSize
    }
    if (condPaint.measureText(displayCondition) > maxTextW) {
        while (displayCondition.length > 3 && condPaint.measureText("$displayCondition…") > maxTextW) {
            displayCondition = displayCondition.dropLast(1)
        }
        displayCondition = "$displayCondition…"
    }

    var cityTextSize = (s * 9.5f).coerceIn(s * 7f, s * 12.5f)
    val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = cityTextSize
        typeface = getSlateFont(context, weight = 700)
        letterSpacing = 0.06f
    }
    var displayCity = weather.cityName.uppercase()
    while (cityPaint.measureText(displayCity) > maxTextW && cityTextSize > s * 6f) {
        cityTextSize -= s * 0.3f
        cityPaint.textSize = cityTextSize
    }
    if (cityPaint.measureText(displayCity) > maxTextW) {
        while (displayCity.length > 3 && cityPaint.measureText("$displayCity…") > maxTextW) {
            displayCity = displayCity.dropLast(1)
        }
        displayCity = "$displayCity…"
    }

    var hlTextSize = (s * 9f).coerceIn(s * 6.8f, s * 12f)
    val hlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = hlTextSize
        typeface = getSlateFont(context, weight = 600)
    }
    var hlStr = "H: ${WeatherPreferences.formatTemp(weather.tempMax, unit)}  L: ${WeatherPreferences.formatTemp(weather.tempMin, unit)}"
    while (hlPaint.measureText(hlStr) > maxTextW && hlTextSize > s * 5.8f) {
        hlTextSize -= s * 0.25f
        hlPaint.textSize = hlTextSize
    }

    // Top and Bottom Group Positioning with Overlap Prevention
    val topBlockH = (tempTextSize * 0.88f) + (s * 4f) + condTextSize
    val btmBlockH = cityTextSize + (s * 3.5f) + hlTextSize
    val availableH = cardH - (padY * 2f)

    if (topBlockH + btmBlockH + (s * 8f) <= availableH) {
        // Tall / Standard cell: Anchor top block to top, bottom block to bottom
        val tempY = cardRect.top + padY + (tempTextSize * 0.88f)
        val condY = tempY + (s * 4f) + condTextSize
        val hlY = cardRect.bottom - padY
        val cityY = hlY - hlTextSize - (s * 3.5f)

        canvas.drawText(WeatherPreferences.formatTemp(weather.currentTemp, unit), rightX, tempY, tempPaint)
        canvas.drawText(displayCondition, rightX, condY, condPaint)
        canvas.drawText(displayCity, rightX, cityY, cityPaint)
        canvas.drawText(hlStr, rightX, hlY, hlPaint)
    } else {
        // Tight / Compact cell: Vertically center the entire stack as a cohesive unit
        val compactGap = s * 3.5f
        val totalStackH = (tempTextSize * 0.88f) + compactGap + condTextSize + compactGap + cityTextSize + compactGap + hlTextSize
        val startY = cardRect.centerY() - (totalStackH / 2f) + (tempTextSize * 0.88f)

        val tempY = startY
        val condY = tempY + compactGap + condTextSize
        val cityY = condY + compactGap + cityTextSize
        val hlY = cityY + compactGap + hlTextSize

        canvas.drawText(WeatherPreferences.formatTemp(weather.currentTemp, unit), rightX, tempY, tempPaint)
        canvas.drawText(displayCondition, rightX, condY, condPaint)
        canvas.drawText(displayCity, rightX, cityY, cityPaint)
        canvas.drawText(hlStr, rightX, hlY, hlPaint)
    }

    return bitmap
}

// 7. Weather Compact Dial (2x2 / 4-Corner Conditions Station)
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

    val cardW = cardRect.width()
    val cardH = cardRect.height()

    // 1. Tightest-Axis Proportional Scalar: Prevents elements from staying oversized on compact sizes
    val baseRef = scaleFactor * 145f
    val propScale = minOf(cardW / baseRef, cardH / baseRef).coerceIn(0.55f, 1.35f)
    val s = scaleFactor * propScale

    val outerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(minOf(cardW, cardH) / 2f)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val weather = WeatherPreferences.getCachedWeatherData(context)
    val unit = WeatherPreferences.getUnit(context)
    val isNight = isNightTime(weather)

    val padX = (cardW * 0.08f).coerceIn(s * 10f, s * 18f)
    val padY = (cardH * 0.08f).coerceIn(s * 10f, s * 18f)

    // Corner metrics typography
    val labelTextSize = (s * 7.2f).coerceIn(s * 5.8f, s * 10f)
    val valTextSize = (s * 13.5f).coerceIn(s * 10f, s * 18f)
    val metricIconSize = (s * 9.5f).coerceIn(s * 7.5f, s * 13f)

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = labelTextSize
        typeface = getSlateFont(context, weight = 700)
        letterSpacing = 0.05f
    }
    val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = valTextSize
        typeface = getSlateFont(context, weight = 700)
    }

    val rLabel = Paint(labelPaint).apply { textAlign = Paint.Align.RIGHT }
    val rVal = Paint(valPaint).apply { textAlign = Paint.Align.RIGHT }

    // -------------------------------------------------------------------------
    // TOP CORNERS: WIND (Left) & HUMIDITY (Right)
    // -------------------------------------------------------------------------
    val topLabelY = cardRect.top + padY + labelTextSize
    val topValY = topLabelY + (s * 3.5f) + (valTextSize * 0.88f)

    // Top-Left: WIND
    val windRes = getDrawableResId(context, "ic_weather_windy")
    if (windRes != 0) {
        val iconRect = RectF(
            cardRect.left + padX,
            topLabelY - metricIconSize + (s * 1.2f),
            cardRect.left + padX + metricIconSize,
            topLabelY + (s * 1.2f)
        )
        drawVectorDrawable(canvas, context, windRes, iconRect, secondaryText)
        canvas.drawText("WIND", cardRect.left + padX + metricIconSize + (s * 3.5f), topLabelY, labelPaint)
    } else {
        canvas.drawText("WIND", cardRect.left + padX, topLabelY, labelPaint)
    }
    canvas.drawText("${weather.windSpeedKmH.toInt()} km/h", cardRect.left + padX, topValY, valPaint)

    // Top-Right: HUMIDITY
    val humidRes = getDrawableResId(context, "ic_humidity")
    val humidLabel = "HUMIDITY"
    val humidLabelW = rLabel.measureText(humidLabel)
    if (humidRes != 0) {
        val totalTopRightW = metricIconSize + (s * 3.5f) + humidLabelW
        val iconLeft = cardRect.right - padX - totalTopRightW
        val iconRect = RectF(
            iconLeft,
            topLabelY - metricIconSize + (s * 1.2f),
            iconLeft + metricIconSize,
            topLabelY + (s * 1.2f)
        )
        drawVectorDrawable(canvas, context, humidRes, iconRect, secondaryText)
    }
    canvas.drawText(humidLabel, cardRect.right - padX, topLabelY, rLabel)
    canvas.drawText("${weather.humidity}%", cardRect.right - padX, topValY, rVal)

    // -------------------------------------------------------------------------
    // BOTTOM CORNERS: RAIN (Left) & UV INDEX (Right)
    // -------------------------------------------------------------------------
    val btmValY = cardRect.bottom - padY
    val btmLabelY = btmValY - (valTextSize * 0.88f) - (s * 3.5f)

    // Bottom-Left: RAIN
    val rainRes = getDrawableResId(context, "ic_rain_chance")
    if (rainRes != 0) {
        val iconRect = RectF(
            cardRect.left + padX,
            btmLabelY - metricIconSize + (s * 1.2f),
            cardRect.left + padX + metricIconSize,
            btmLabelY + (s * 1.2f)
        )
        drawVectorDrawable(canvas, context, rainRes, iconRect, secondaryText)
        canvas.drawText("RAIN", cardRect.left + padX + metricIconSize + (s * 3.5f), btmLabelY, labelPaint)
    } else {
        canvas.drawText("RAIN", cardRect.left + padX, btmLabelY, labelPaint)
    }
    canvas.drawText("${weather.rainChance}%", cardRect.left + padX, btmValY, valPaint)

    // Bottom-Right: UV INDEX
    val uvRes = getDrawableResId(context, "ic_uv_index")
    val uvLabel = "UV INDEX"
    val uvLabelW = rLabel.measureText(uvLabel)
    if (uvRes != 0) {
        val totalBtmRightW = metricIconSize + (s * 3.5f) + uvLabelW
        val iconLeft = cardRect.right - padX - totalBtmRightW
        val iconRect = RectF(
            iconLeft,
            btmLabelY - metricIconSize + (s * 1.2f),
            iconLeft + metricIconSize,
            btmLabelY + (s * 1.2f)
        )
        drawVectorDrawable(canvas, context, uvRes, iconRect, secondaryText)
    }
    canvas.drawText(uvLabel, cardRect.right - padX, btmLabelY, rLabel)
    canvas.drawText(String.format(java.util.Locale.US, "%.1f", weather.uvIndex), cardRect.right - padX, btmValY, rVal)

    // -------------------------------------------------------------------------
    // CENTER HERO CORE: Bounded & Guaranteed Non-Colliding
    // -------------------------------------------------------------------------
    val cx = cardRect.centerX()

    // Measure exact clear vertical space between top and bottom corner metrics
    val topBoundaryY = topValY + (s * 4f)
    val bottomBoundaryY = btmLabelY - labelTextSize - (s * 4f)
    val availableCenterH = maxOf(0f, bottomBoundaryY - topBoundaryY)

    // Base proportional sizes
    var centerIconSize = (s * 24f).coerceIn(s * 16f, s * 34f)
    var tempTextSize = (s * 32f).coerceIn(s * 20f, s * 44f)
    var cityTextSize = (s * 9.5f).coerceIn(s * 7f, s * 12.5f)
    var gapIconToTemp = s * 2.5f
    var gapTempToCity = s * 2f

    var totalCoreH = centerIconSize + gapIconToTemp + (tempTextSize * 0.86f) + gapTempToCity + cityTextSize

    // Auto-compress hero stack if available height is tight
    if (totalCoreH > availableCenterH && availableCenterH > 0f) {
        val ratio = (availableCenterH / totalCoreH).coerceIn(0.60f, 1f)
        centerIconSize *= ratio
        tempTextSize *= ratio
        cityTextSize *= ratio
        gapIconToTemp *= ratio
        gapTempToCity *= ratio
        totalCoreH = centerIconSize + gapIconToTemp + (tempTextSize * 0.86f) + gapTempToCity + cityTextSize
    }

    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = tempTextSize
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
    }
    val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = cityTextSize
        typeface = getSlateFont(context, weight = 600)
        textAlign = Paint.Align.CENTER
    }

    val startY = cardRect.centerY() - (totalCoreH / 2f)

    val iconRect = RectF(
        cx - (centerIconSize / 2f),
        startY,
        cx + (centerIconSize / 2f),
        startY + centerIconSize
    )
    drawWeatherIcon(canvas, context, weather.weatherCode, iconRect, accentColor, isNight)

    val tempY = startY + centerIconSize + gapIconToTemp + (tempTextSize * 0.86f)
    canvas.drawText(WeatherPreferences.formatTemp(weather.currentTemp, unit), cx, tempY, tempPaint)

    val cityY = tempY + gapTempToCity + cityTextSize
    canvas.drawText(weather.cityName, cx, cityY, cityPaint)

    return bitmap
}

// 8. Weather Metro Trio (3x1)
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

// 9. Micro Weather (1x1 / Minimalist Single App Tile)
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

    val cardW = cardRect.width()
    val cardH = cardRect.height()

    // 1. Dual-Axis Proportional Scalar anchored to a 1x1 base cell (~75dp x 75dp)
    val baseRef = scaleFactor * 75f
    val propScale = minOf(cardW / baseRef, cardH / baseRef).coerceIn(0.60f, 2.2f)
    val s = scaleFactor * propScale

    val outerRadius = getStandardCornerRadius(scaleFactor).coerceAtMost(minOf(cardW, cardH) / 2f)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val weather = WeatherPreferences.getCachedWeatherData(context)
    val unit = WeatherPreferences.getUnit(context)
    val isNight = isNightTime(weather)

    val padX = (cardW * 0.11f).coerceIn(s * 8f, s * 16f)
    val padY = (cardH * 0.11f).coerceIn(s * 8f, s * 16f)

    // -------------------------------------------------------------------------
    // 1. BOTTOM: City Name (Pinned to bottom-left with auto-shrink)
    // -------------------------------------------------------------------------
    var cityTextSize = (s * 8.5f).coerceIn(s * 6f, s * 13f)
    val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = cityTextSize
        typeface = getSlateFont(context, weight = 700)
    }
    val maxCityW = cardW - (padX * 2f)
    var displayCity = weather.cityName
    while (cityPaint.measureText(displayCity) > maxCityW && cityTextSize > s * 5.5f) {
        cityTextSize -= s * 0.3f
        cityPaint.textSize = cityTextSize
    }
    if (cityPaint.measureText(displayCity) > maxCityW) {
        while (displayCity.length > 3 && cityPaint.measureText("$displayCity…") > maxCityW) {
            displayCity = displayCity.dropLast(1)
        }
        displayCity = "$displayCity…"
    }
    val cityY = cardRect.bottom - padY
    canvas.drawText(displayCity, cardRect.left + padX, cityY, cityPaint)

    // -------------------------------------------------------------------------
    // 2. TOP-RIGHT: Weather Vector Icon
    // -------------------------------------------------------------------------
    val maxIconSize = minOf(cardW * 0.34f, cardH * 0.34f)
    val iconSize = (s * 20f).coerceIn(s * 14f, maxIconSize)
    val iconRect = RectF(
        cardRect.right - padX - iconSize,
        cardRect.top + padY,
        cardRect.right - padX,
        cardRect.top + padY + iconSize
    )
    drawWeatherIcon(canvas, context, weather.weatherCode, iconRect, accentColor, isNight)

    // -------------------------------------------------------------------------
    // 3. HERO TEMPERATURE: Non-Colliding Dynamic Positioning
    // -------------------------------------------------------------------------
    val tempStr = WeatherPreferences.formatTemp(weather.currentTemp, unit)
    val maxTempW = iconRect.left - (cardRect.left + padX) - (s * 2.5f)

    var tempTextSize = (s * 26f).coerceIn(s * 16f, s * 46f)
    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = tempTextSize
        typeface = getSlateFont(context, weight = 800)
    }

    // Auto-scale temperature font down if it risks touching the icon
    while (tempPaint.measureText(tempStr) > maxTempW && tempTextSize > s * 14f) {
        tempTextSize -= s * 0.5f
        tempPaint.textSize = tempTextSize
    }

    // Vertically center the temperature in the open space between card top and city name
    val topBound = cardRect.top + padY
    val bottomBound = cityY - cityTextSize - (s * 3.5f)
    val availableH = maxOf(0f, bottomBound - topBound)
    val tempY = topBound + (availableH / 2f) + (tempTextSize * 0.38f)

    canvas.drawText(tempStr, cardRect.left + padX, tempY, tempPaint)

    return bitmap
}
