package com.altusix.slate.widgets.weather

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.utils.createSupersampledCanvas
import com.altusix.slate.utils.getSafeBgColor
import com.altusix.slate.utils.getSlateFont
import com.altusix.slate.utils.getStandardCornerRadius
import java.util.Calendar

/**
 * Universal vector weather icon renderer.
 * Draws razor-sharp glyphs at any resolution without bitmap compression artifacts.
 */
fun drawWeatherIcon(
    canvas: Canvas,
    weatherCode: Int,
    iconRect: RectF,
    color: Int,
    isNight: Boolean = false
) {
    val l = iconRect.left
    val t = iconRect.top
    val w = iconRect.width()
    val h = iconRect.height()

    fun x(pct: Float): Float = l + w * pct
    fun y(pct: Float): Float = t + h * pct

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = w * 0.08f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Helper: draw smooth cloud shape
    fun drawCloud(cloudRect: RectF, paint: Paint) {
        val cl = cloudRect.left
        val ct = cloudRect.top
        val cw = cloudRect.width()
        val ch = cloudRect.height()
        fun cx(p: Float) = cl + cw * p
        fun cy(p: Float) = ct + ch * p

        val p = Path().apply {
            moveTo(cx(0.20f), cy(0.76f))
            lineTo(cx(0.80f), cy(0.76f))
            quadTo(cx(0.92f), cy(0.76f), cx(0.92f), cy(0.62f))
            quadTo(cx(0.92f), cy(0.48f), cx(0.80f), cy(0.48f))
            cubicTo(cx(0.80f), cy(0.28f), cx(0.58f), cy(0.26f), cx(0.52f), cy(0.40f))
            cubicTo(cx(0.46f), cy(0.32f), cx(0.32f), cy(0.34f), cx(0.30f), cy(0.48f))
            quadTo(cx(0.16f), cy(0.48f), cx(0.16f), cy(0.62f))
            quadTo(cx(0.16f), cy(0.76f), cx(0.20f), cy(0.76f))
            close()
        }
        canvas.drawPath(p, paint)
    }

    when (weatherCode) {
        // Clear Sky
        0 -> {
            if (!isNight) {
                // Sun
                val sunRadius = w * 0.22f
                val cx = iconRect.centerX()
                val cy = iconRect.centerY()
                canvas.drawCircle(cx, cy, sunRadius, fillPaint)

                // 8 Sun Rays
                val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = color
                    style = Paint.Style.STROKE
                    strokeWidth = w * 0.075f
                    strokeCap = Paint.Cap.ROUND
                }
                val r1 = sunRadius + (w * 0.08f)
                val r2 = sunRadius + (w * 0.18f)
                for (i in 0 until 8) {
                    val angle = Math.toRadians(i * 45.0)
                    val x1 = cx + (r1 * Math.cos(angle)).toFloat()
                    val y1 = cy + (r1 * Math.sin(angle)).toFloat()
                    val x2 = cx + (r2 * Math.cos(angle)).toFloat()
                    val y2 = cy + (r2 * Math.sin(angle)).toFloat()
                    canvas.drawLine(x1, y1, x2, y2, rayPaint)
                }
            } else {
                // Crescent Moon
                val moonPath = Path().apply {
                    val mr = RectF(x(0.24f), y(0.18f), x(0.80f), y(0.82f))
                    arcTo(mr, -90f, 260f, false)
                    quadTo(x(0.48f), y(0.50f), x(0.52f), y(0.18f))
                    close()
                }
                canvas.drawPath(moonPath, fillPaint)
            }
        }

        // Mainly Clear / Partly Cloudy
        1, 2 -> {
            if (!isNight) {
                // Sun peeking behind cloud at top right
                val sunRadius = w * 0.16f
                canvas.drawCircle(x(0.66f), y(0.34f), sunRadius, fillPaint)
                val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = color
                    style = Paint.Style.STROKE
                    strokeWidth = w * 0.06f
                    strokeCap = Paint.Cap.ROUND
                }
                for (deg in listOf(0, 45, 90, 315)) {
                    val rad = Math.toRadians(deg.toDouble())
                    val x1 = x(0.66f) + (sunRadius * 1.2f * Math.cos(rad)).toFloat()
                    val y1 = y(0.34f) + (sunRadius * 1.2f * Math.sin(rad)).toFloat()
                    val x2 = x(0.66f) + (sunRadius * 1.6f * Math.cos(rad)).toFloat()
                    val y2 = y(0.34f) + (sunRadius * 1.6f * Math.sin(rad)).toFloat()
                    canvas.drawLine(x1, y1, x2, y2, rayPaint)
                }
            } else {
                // Moon peeking behind cloud
                canvas.drawCircle(x(0.66f), y(0.34f), w * 0.14f, fillPaint)
            }
            // Foreground cloud
            drawCloud(RectF(x(0.08f), y(0.28f), x(0.92f), y(0.86f)), fillPaint)
        }

        // Overcast / Cloudy
        3 -> {
            // Background cloud
            val bgCloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.argb(140, Color.red(color), Color.green(color), Color.blue(color))
                style = Paint.Style.FILL
            }
            drawCloud(RectF(x(0.24f), y(0.16f), x(0.94f), y(0.68f)), bgCloudPaint)
            // Foreground cloud
            drawCloud(RectF(x(0.08f), y(0.32f), x(0.84f), y(0.86f)), fillPaint)
        }

        // Fog / Mist
        45, 48 -> {
            drawCloud(RectF(x(0.14f), y(0.16f), x(0.86f), y(0.64f)), fillPaint)
            // Horizontal mist lines
            val mistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeWidth = w * 0.065f
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(x(0.22f), y(0.72f), x(0.78f), y(0.72f), mistPaint)
            canvas.drawLine(x(0.16f), y(0.82f), x(0.84f), y(0.82f), mistPaint)
            canvas.drawLine(x(0.28f), y(0.92f), x(0.72f), y(0.92f), mistPaint)
        }

        // Drizzle / Light Rain
        51, 53, 55, 56, 57, 61 -> {
            drawCloud(RectF(x(0.10f), y(0.14f), x(0.90f), y(0.64f)), fillPaint)
            val dropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeWidth = w * 0.07f
                strokeCap = Paint.Cap.ROUND
            }
            // 3 slanted drops
            val dropY1 = y(0.72f)
            val dropY2 = y(0.88f)
            val dx = w * 0.04f
            canvas.drawLine(x(0.32f), dropY1, x(0.32f) - dx, dropY2, dropPaint)
            canvas.drawLine(x(0.50f), dropY1, x(0.50f) - dx, dropY2, dropPaint)
            canvas.drawLine(x(0.68f), dropY1, x(0.68f) - dx, dropY2, dropPaint)
        }

        // Moderate / Heavy Rain / Showers
        63, 65, 66, 67, 80, 81, 82 -> {
            drawCloud(RectF(x(0.10f), y(0.12f), x(0.90f), y(0.60f)), fillPaint)
            val dropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeWidth = w * 0.065f
                strokeCap = Paint.Cap.ROUND
            }
            val dx = w * 0.05f
            // 4 longer rain streaks
            canvas.drawLine(x(0.26f), y(0.68f), x(0.26f) - dx, y(0.92f), dropPaint)
            canvas.drawLine(x(0.42f), y(0.66f), x(0.42f) - dx, y(0.90f), dropPaint)
            canvas.drawLine(x(0.58f), y(0.68f), x(0.58f) - dx, y(0.92f), dropPaint)
            canvas.drawLine(x(0.74f), y(0.66f), x(0.74f) - dx, y(0.90f), dropPaint)
        }

        // Snow / Snow Showers
        71, 73, 75, 77, 85, 86 -> {
            drawCloud(RectF(x(0.10f), y(0.14f), x(0.90f), y(0.64f)), fillPaint)
            val snowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.FILL
            }
            canvas.drawCircle(x(0.30f), y(0.76f), w * 0.055f, snowPaint)
            canvas.drawCircle(x(0.50f), y(0.84f), w * 0.055f, snowPaint)
            canvas.drawCircle(x(0.70f), y(0.76f), w * 0.055f, snowPaint)
            canvas.drawCircle(x(0.40f), y(0.92f), w * 0.045f, snowPaint)
            canvas.drawCircle(x(0.60f), y(0.92f), w * 0.045f, snowPaint)
        }

        // Thunderstorm
        95, 96, 99 -> {
            drawCloud(RectF(x(0.10f), y(0.12f), x(0.90f), y(0.60f)), fillPaint)
            // Sharp lightning bolt
            val boltPath = Path().apply {
                moveTo(x(0.54f), y(0.58f))
                lineTo(x(0.42f), y(0.74f))
                lineTo(x(0.50f), y(0.74f))
                lineTo(x(0.38f), y(0.94f))
                lineTo(x(0.62f), y(0.70f))
                lineTo(x(0.52f), y(0.70f))
                close()
            }
            canvas.drawPath(boltPath, fillPaint)
        }

        // Default Wind / Breezy
        else -> {
            val windPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeWidth = w * 0.075f
                strokeCap = Paint.Cap.ROUND
            }
            val w1 = Path().apply {
                moveTo(x(0.16f), y(0.34f))
                lineTo(x(0.66f), y(0.34f))
                quadTo(x(0.82f), y(0.34f), x(0.82f), y(0.24f))
                quadTo(x(0.82f), y(0.14f), x(0.72f), y(0.14f))
            }
            val w2 = Path().apply {
                moveTo(x(0.12f), y(0.54f))
                lineTo(x(0.76f), y(0.54f))
                quadTo(x(0.88f), y(0.54f), x(0.88f), y(0.64f))
                quadTo(x(0.88f), y(0.74f), x(0.78f), y(0.74f))
            }
            val w3 = Path().apply {
                moveTo(x(0.22f), y(0.74f))
                lineTo(x(0.58f), y(0.74f))
            }
            canvas.drawPath(w1, windPaint)
            canvas.drawPath(w2, windPaint)
            canvas.drawPath(w3, windPaint)
        }
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

    // LEFT SECTION: Current Conditions
    val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 11f
        typeface = getSlateFont(context, weight = 700)
        letterSpacing = 0.08f
    }
    canvas.drawText(weather.cityName.uppercase(), cardRect.left + pad, cardRect.top + pad + (scaleFactor * 10f), cityPaint)

    // Current Temp
    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 46f
        typeface = getSlateFont(context, weight = 800)
    }
    val tempStr = WeatherPreferences.formatTemp(weather.currentTemp, unit)
    canvas.drawText(tempStr, cardRect.left + pad, cardRect.top + pad + (scaleFactor * 52f), tempPaint)

    // Condition Text
    val condPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 14f
        typeface = getSlateFont(context, weight = 600)
    }
    canvas.drawText(weather.conditionText, cardRect.left + pad, cardRect.top + pad + (scaleFactor * 72f), condPaint)

    // High / Low Pill
    val hiLoStr = "H: ${WeatherPreferences.formatTemp(weather.tempMax, unit)}   L: ${WeatherPreferences.formatTemp(weather.tempMin, unit)}"
    val hiLoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 11.5f
        typeface = getSlateFont(context, weight = 600)
    }
    canvas.drawText(hiLoStr, cardRect.left + pad, cardRect.top + pad + (scaleFactor * 90f), hiLoPaint)

    // CENTER DIVIDER
    val dividerX = cardRect.left + pad + halfW + (scaleFactor * 8f)
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(24, 0, 0, 0) else Color.argb(32, 255, 255, 255)
        strokeWidth = scaleFactor * 1f
    }
    canvas.drawLine(dividerX, cardRect.top + pad, dividerX, cardRect.bottom - pad, dividerPaint)

    // RIGHT SECTION: 5-Day Forecast List
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

        // Day name
        canvas.drawText(item.dayLabel, rightLeft, cy + (scaleFactor * 4f), dayNamePaint)

        // Weather icon
        val iconSize = scaleFactor * 14f
        val iconX = rightLeft + (rightWidth * 0.40f)
        val iconRect = RectF(iconX - (iconSize / 2f), cy - (iconSize / 2f), iconX + (iconSize / 2f), cy + (iconSize / 2f))
        drawWeatherIcon(canvas, item.weatherCode, iconRect, accentColor)

        // Rain % indicator if significant
        if (item.rainProb >= 20) {
            val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#38ACFF")
                textSize = scaleFactor * 8.5f
                typeface = getSlateFont(context, weight = 700)
            }
            canvas.drawText("${item.rainProb}%", iconX + (scaleFactor * 10f), cy + (scaleFactor * 3.5f), rainPaint)
        }

        // Temp range
        val rangeText = "${WeatherPreferences.formatTempValue(item.maxTemp, unit)}°  ${WeatherPreferences.formatTempValue(item.minTemp, unit)}°"
        canvas.drawText(rangeText, cardRect.right - pad, cy + (scaleFactor * 4f), rangePaint)
    }

    return bitmap
}

// 2. Weather Bento Glance (4x2 / Bento Grid Structure)
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

    val pad = scaleFactor * 8f
    val gap = scaleFactor * 8f
    val innerCardBg = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerCardBg
        style = Paint.Style.FILL
    }

    val concentricRadius = (outerRadius - pad).coerceAtLeast(scaleFactor * 6f)
    val sq = scaleFactor * 10f

    // LEFT BIG CARD: Current Weather & Icon
    val leftW = (cardRect.width() - (pad * 2f) - gap) * 0.52f
    val leftRect = RectF(cardRect.left + pad, cardRect.top + pad, cardRect.left + pad + leftW, cardRect.bottom - pad)
    val leftRadii = floatArrayOf(concentricRadius, concentricRadius, sq, sq, sq, sq, concentricRadius, concentricRadius)
    val leftPath = Path().apply { addRoundRect(leftRect, leftRadii, Path.Direction.CW) }
    canvas.drawPath(leftPath, tilePaint)

    val innerPad = scaleFactor * 12f
    // City name
    val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 10.5f
        typeface = getSlateFont(context, weight = 700)
    }
    canvas.drawText(weather.cityName.uppercase(), leftRect.left + innerPad, leftRect.top + innerPad + (scaleFactor * 8f), cityPaint)

    // Weather Icon
    val mainIconSize = scaleFactor * 32f
    val mainIconRect = RectF(
        leftRect.right - innerPad - mainIconSize,
        leftRect.top + innerPad,
        leftRect.right - innerPad,
        leftRect.top + innerPad + mainIconSize
    )
    drawWeatherIcon(canvas, weather.weatherCode, mainIconRect, accentColor)

    // Huge Temp
    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 40f
        typeface = getSlateFont(context, weight = 800)
    }
    canvas.drawText(WeatherPreferences.formatTemp(weather.currentTemp, unit), leftRect.left + innerPad, leftRect.top + (scaleFactor * 68f), tempPaint)

    // Condition
    val condPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 13f
        typeface = getSlateFont(context, weight = 600)
    }
    canvas.drawText(weather.conditionText, leftRect.left + innerPad, leftRect.bottom - innerPad - (scaleFactor * 12f), condPaint)

    // High / Low
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

    // Feels Like
    canvas.drawText("FEELS LIKE", topRect.left + (scaleFactor * 10f), topRect.top + (scaleFactor * 16f), labelPaint)
    canvas.drawText(WeatherPreferences.formatTemp(weather.feelsLike, unit), topRect.left + (scaleFactor * 10f), topRect.top + (scaleFactor * 36f), valuePaint)

    // Humidity
    canvas.drawText("HUMIDITY", topRect.left + metricHalfW + (scaleFactor * 6f), topRect.top + (scaleFactor * 16f), labelPaint)
    canvas.drawText("${weather.humidity}%", topRect.left + metricHalfW + (scaleFactor * 6f), topRect.top + (scaleFactor * 36f), valuePaint)

    // RIGHT BOTTOM CARD: Wind & UV Index
    val btmRect = RectF(rightLeft, topRect.bottom + gap, cardRect.right - pad, cardRect.bottom - pad)
    val btmRadii = floatArrayOf(sq, sq, sq, sq, concentricRadius, concentricRadius, sq, sq)
    val btmPath = Path().apply { addRoundRect(btmRect, btmRadii, Path.Direction.CW) }
    canvas.drawPath(btmPath, tilePaint)

    // Wind
    canvas.drawText("WIND", btmRect.left + (scaleFactor * 10f), btmRect.top + (scaleFactor * 16f), labelPaint)
    canvas.drawText("${weather.windSpeedKmH.toInt()} km/h", btmRect.left + (scaleFactor * 10f), btmRect.top + (scaleFactor * 36f), valuePaint)

    // UV Index
    canvas.drawText("UV INDEX", btmRect.left + metricHalfW + (scaleFactor * 6f), btmRect.top + (scaleFactor * 16f), labelPaint)
    canvas.drawText(String.format("%.1f", weather.uvIndex), btmRect.left + metricHalfW + (scaleFactor * 6f), btmRect.top + (scaleFactor * 36f), valuePaint)

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

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val weather = WeatherPreferences.getCachedWeatherData(context)
    val unit = WeatherPreferences.getUnit(context)

    // Sun Arc Calculations
    val cx = cardRect.centerX()
    val cy = cardRect.top + (cardRect.height() * 0.50f)
    val arcRadius = cardRect.width() * 0.36f

    val arcRect = RectF(cx - arcRadius, cy - arcRadius, cx + arcRadius, cy + arcRadius)

    // Arc Track (180 to 0 degrees, top half)
    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(20, 0, 0, 0) else Color.argb(30, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = scaleFactor * 3.5f
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawArc(arcRect, 180f, 180f, false, trackPaint)

    // Current time daylight progress ratio (0.0 to 1.0)
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

    // Glowing Sun Marker
    val sunAngleRad = Math.toRadians((180.0 + sweepAngle).toDouble())
    val markerX = cx + (arcRadius * Math.cos(sunAngleRad)).toFloat()
    val markerY = cy + (arcRadius * Math.sin(sunAngleRad)).toFloat()

    val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(markerX, markerY, scaleFactor * 5f, markerPaint)

    // Sunrise & Sunset Labels at base of arc
    val timeLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 8.5f
        typeface = getSlateFont(context, weight = 600)
    }
    canvas.drawText("↑ ${weather.sunrise}", cx - arcRadius, cy + (scaleFactor * 14f), timeLabelPaint)
    val setPaint = Paint(timeLabelPaint).apply { textAlign = Paint.Align.RIGHT }
    canvas.drawText("↓ ${weather.sunset}", cx + arcRadius, cy + (scaleFactor * 14f), setPaint)

    // Center Weather Info
    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 34f
        typeface = getSlateFont(context, weight = 800)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(WeatherPreferences.formatTemp(weather.currentTemp, unit), cx, cy - (scaleFactor * 4f), tempPaint)

    // Condition
    val condPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 12.5f
        typeface = getSlateFont(context, weight = 600)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(weather.conditionText, cx, cardRect.bottom - (scaleFactor * 26f), condPaint)

    // City & High/Low Pill
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

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val weather = WeatherPreferences.getCachedWeatherData(context)
    val unit = WeatherPreferences.getUnit(context)
    val pad = scaleFactor * 14f

    // Left Location & Condition
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
    val iconSize = scaleFactor * 22f
    val iconRect = RectF(centerLeft, cardRect.centerY() - (iconSize / 2f), centerLeft + iconSize, cardRect.centerY() + (iconSize / 2f))
    drawWeatherIcon(canvas, weather.weatherCode, iconRect, accentColor)

    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 24f
        typeface = getSlateFont(context, weight = 800)
    }
    canvas.drawText(WeatherPreferences.formatTemp(weather.currentTemp, unit), centerLeft + iconSize + (scaleFactor * 8f), cardRect.centerY() + (scaleFactor * 8f), tempPaint)

    // Right 3 Hourly Forecast Pills
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

    val outerRadius = getStandardCornerRadius(scaleFactor)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((config.opacity.coerceIn(0f, 1f) * 255).toInt(), Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(cardRect, outerRadius, outerRadius, bgPaint)

    val weather = WeatherPreferences.getCachedWeatherData(context)
    val unit = WeatherPreferences.getUnit(context)
    val pad = scaleFactor * 16f

    // Header Condition Tag
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 10f
        typeface = getSlateFont(context, weight = 700)
        letterSpacing = 0.12f
    }
    canvas.drawText(weather.conditionText.uppercase(), cardRect.left + pad, cardRect.top + pad + (scaleFactor * 10f), headerPaint)

    // Giant Temp
    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 52f
        typeface = getSlateFont(context, weight = 800)
    }
    canvas.drawText(WeatherPreferences.formatTemp(weather.currentTemp, unit), cardRect.left + pad, cardRect.top + (scaleFactor * 78f), tempPaint)

    // Weather Icon at top right
    val iconSize = scaleFactor * 32f
    val iconRect = RectF(cardRect.right - pad - iconSize, cardRect.top + pad, cardRect.right - pad, cardRect.top + pad + iconSize)
    drawWeatherIcon(canvas, weather.weatherCode, iconRect, accentColor)

    // Bottom City Badge & Range
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

// 6. Weather Hourly Ribbon (4x1 / 24h Hourly Projections)
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

    // Left Current Block
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

    // Divider
    val divX = cardRect.left + pad + leftW
    val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isLight) Color.argb(20, 0, 0, 0) else Color.argb(30, 255, 255, 255)
        strokeWidth = scaleFactor * 1f
    }
    canvas.drawLine(divX, cardRect.top + pad, divX, cardRect.bottom - pad, divPaint)

    // Right 5 Hourly Nodes
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
        val iconSize = scaleFactor * 13f
        val iRect = RectF(nx - (iconSize / 2f), cardRect.centerY() - (iconSize / 2f) - (scaleFactor * 2f), nx + (iconSize / 2f), cardRect.centerY() + (iconSize / 2f) - (scaleFactor * 2f))

        canvas.drawText(item.timeLabel, nx, cardRect.top + pad + (scaleFactor * 6f), timePaint)
        drawWeatherIcon(canvas, item.weatherCode, iRect, accentColor)
        canvas.drawText(WeatherPreferences.formatTemp(item.temp, unit), nx, cardRect.bottom - pad, hTempPaint)
    }

    return bitmap
}

// 7. Weather Minimalist Dual (2x2 / Split Quadrant)
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
    val pad = scaleFactor * 14f

    val cx = cardRect.centerX()

    // Left Half: Large Weather Vector Icon
    val iconSize = scaleFactor * 48f
    val iconLeft = cardRect.left + (cardRect.width() * 0.25f) - (iconSize / 2f)
    val iconTop = cardRect.centerY() - (iconSize / 2f)
    val iconRect = RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
    drawWeatherIcon(canvas, weather.weatherCode, iconRect, accentColor)

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

// 8. Weather Compact Dial (2x2 / 4-Corner Conditions Station)
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
    val pad = scaleFactor * 12f

    // 4 Corner Badges
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

    // Top-Left: WIND
    canvas.drawText("WIND", cardRect.left + pad, cardRect.top + pad + (scaleFactor * 8f), labelPaint)
    canvas.drawText("${weather.windSpeedKmH.toInt()} km/h", cardRect.left + pad, cardRect.top + pad + (scaleFactor * 22f), valPaint)

    // Top-Right: HUMIDITY
    val rLabel = Paint(labelPaint).apply { textAlign = Paint.Align.RIGHT }
    val rVal = Paint(valPaint).apply { textAlign = Paint.Align.RIGHT }
    canvas.drawText("HUMIDITY", cardRect.right - pad, cardRect.top + pad + (scaleFactor * 8f), rLabel)
    canvas.drawText("${weather.humidity}%", cardRect.right - pad, cardRect.top + pad + (scaleFactor * 22f), rVal)

    // Bottom-Left: RAIN CHANCE
    canvas.drawText("RAIN", cardRect.left + pad, cardRect.bottom - pad - (scaleFactor * 14f), labelPaint)
    canvas.drawText("${weather.rainChance}%", cardRect.left + pad, cardRect.bottom - pad, valPaint)

    // Bottom-Right: UV INDEX
    canvas.drawText("UV INDEX", cardRect.right - pad, cardRect.bottom - pad - (scaleFactor * 14f), rLabel)
    canvas.drawText(String.format("%.1f", weather.uvIndex), cardRect.right - pad, cardRect.bottom - pad, rVal)

    // Center Core: Temp & Icon
    val cx = cardRect.centerX()
    val cy = cardRect.centerY()

    val centerIconSize = scaleFactor * 24f
    val iconRect = RectF(cx - (centerIconSize / 2f), cy - (scaleFactor * 26f), cx + (centerIconSize / 2f), cy - (scaleFactor * 2f))
    drawWeatherIcon(canvas, weather.weatherCode, iconRect, accentColor)

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

// 9. Weather Metro Trio (3x1 / 3-Day Projection Bar)
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

        val iconSize = scaleFactor * 18f
        val iRect = RectF(px - (iconSize / 2f), cardRect.centerY() - (iconSize / 2f), px + (iconSize / 2f), cardRect.centerY() + (iconSize / 2f))
        drawWeatherIcon(canvas, d.weatherCode, iRect, accentColor)

        canvas.drawText("${WeatherPreferences.formatTempValue(d.maxTemp, unit)}°", px, cardRect.bottom - (scaleFactor * 10f), tempPaint)
    }

    return bitmap
}

// 10. Micro Weather (1x1 / Minimalist Single App Tile)
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
    val pad = scaleFactor * 10f

    // Weather Icon at top-right
    val iconSize = scaleFactor * 22f
    val iconRect = RectF(cardRect.right - pad - iconSize, cardRect.top + pad, cardRect.right - pad, cardRect.top + pad + iconSize)
    drawWeatherIcon(canvas, weather.weatherCode, iconRect, accentColor)

    // Current Temp
    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryText
        textSize = scaleFactor * 28f
        typeface = getSlateFont(context, weight = 800)
    }
    canvas.drawText(WeatherPreferences.formatTemp(weather.currentTemp, unit), cardRect.left + pad, cardRect.centerY() + (scaleFactor * 8f), tempPaint)

    // City at bottom
    val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryText
        textSize = scaleFactor * 8.5f
        typeface = getSlateFont(context, weight = 700)
    }
    canvas.drawText(weather.cityName, cardRect.left + pad, cardRect.bottom - pad, cityPaint)

    return bitmap
}
