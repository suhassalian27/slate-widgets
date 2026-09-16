package com.altusix.slate.widgets.weather

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HourlyForecastItem(
    val timeLabel: String, // e.g. "12 PM", "1 PM"
    val temp: Float,
    val weatherCode: Int,
    val rainProb: Int
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("time", timeLabel)
        put("temp", temp.toDouble())
        put("code", weatherCode)
        put("rain", rainProb)
    }

    companion object {
        fun fromJson(json: JSONObject): HourlyForecastItem = HourlyForecastItem(
            timeLabel = json.optString("time", "Now"),
            temp = json.optDouble("temp", 20.0).toFloat(),
            weatherCode = json.optInt("code", 1),
            rainProb = json.optInt("rain", 0)
        )
    }
}

data class DailyForecastItem(
    val dayLabel: String, // e.g. "Mon", "Tue"
    val maxTemp: Float,
    val minTemp: Float,
    val weatherCode: Int,
    val rainProb: Int
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("day", dayLabel)
        put("max", maxTemp.toDouble())
        put("min", minTemp.toDouble())
        put("code", weatherCode)
        put("rain", rainProb)
    }

    companion object {
        fun fromJson(json: JSONObject): DailyForecastItem = DailyForecastItem(
            dayLabel = json.optString("day", "Today"),
            maxTemp = json.optDouble("max", 22.0).toFloat(),
            minTemp = json.optDouble("min", 15.0).toFloat(),
            weatherCode = json.optInt("code", 1),
            rainProb = json.optInt("rain", 0)
        )
    }
}

data class WeatherCity(
    val id: Long = 0,
    val name: String,
    val country: String,
    val admin1: String = "",
    val latitude: Double,
    val longitude: Double
) {
    val displayName: String
        get() = if (admin1.isNotBlank() && country.isNotBlank()) "$name, $admin1, $country"
        else if (country.isNotBlank()) "$name, $country"
        else name

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("country", country)
        put("admin1", admin1)
        put("lat", latitude)
        put("lon", longitude)
    }

    companion object {
        fun fromJson(json: JSONObject): WeatherCity = WeatherCity(
            id = json.optLong("id", 0),
            name = json.optString("name", "San Francisco"),
            country = json.optString("country", "United States"),
            admin1 = json.optString("admin1", "California"),
            latitude = json.optDouble("lat", 37.7749),
            longitude = json.optDouble("lon", -122.4194)
        )
    }
}

data class WeatherData(
    val cityName: String = "San Francisco",
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194,
    val currentTemp: Float = 21f,
    val feelsLike: Float = 20f,
    val tempMax: Float = 24f,
    val tempMin: Float = 14f,
    val weatherCode: Int = 1,
    val conditionText: String = "Partly Cloudy",
    val humidity: Int = 62,
    val windSpeedKmH: Float = 14f,
    val uvIndex: Float = 4.2f,
    val rainChance: Int = 10,
    val sunrise: String = "06:24",
    val sunset: String = "19:48",
    val hourlyForecast: List<HourlyForecastItem> = emptyList(),
    val dailyForecast: List<DailyForecastItem> = emptyList(),
    val lastUpdatedMillis: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("city", cityName)
        put("lat", latitude)
        put("lon", longitude)
        put("temp", currentTemp.toDouble())
        put("feels", feelsLike.toDouble())
        put("max", tempMax.toDouble())
        put("min", tempMin.toDouble())
        put("code", weatherCode)
        put("condition", conditionText)
        put("humidity", humidity)
        put("wind", windSpeedKmH.toDouble())
        put("uv", uvIndex.toDouble())
        put("rain", rainChance)
        put("sunrise", sunrise)
        put("sunset", sunset)
        put("updated", lastUpdatedMillis)

        val hourlyArr = JSONArray()
        hourlyForecast.forEach { hourlyArr.put(it.toJson()) }
        put("hourly", hourlyArr)

        val dailyArr = JSONArray()
        dailyForecast.forEach { dailyArr.put(it.toJson()) }
        put("daily", dailyArr)
    }

    companion object {
        fun fromJson(json: JSONObject): WeatherData {
            val hourlyList = mutableListOf<HourlyForecastItem>()
            val hourlyArr = json.optJSONArray("hourly")
            if (hourlyArr != null) {
                for (i in 0 until hourlyArr.length()) {
                    hourlyArr.optJSONObject(i)?.let { hourlyList.add(HourlyForecastItem.fromJson(it)) }
                }
            }

            val dailyList = mutableListOf<DailyForecastItem>()
            val dailyArr = json.optJSONArray("daily")
            if (dailyArr != null) {
                for (i in 0 until dailyArr.length()) {
                    dailyArr.optJSONObject(i)?.let { dailyList.add(DailyForecastItem.fromJson(it)) }
                }
            }

            return WeatherData(
                cityName = json.optString("city", "San Francisco"),
                latitude = json.optDouble("lat", 37.7749),
                longitude = json.optDouble("lon", -122.4194),
                currentTemp = json.optDouble("temp", 21.0).toFloat(),
                feelsLike = json.optDouble("feels", 20.0).toFloat(),
                tempMax = json.optDouble("max", 24.0).toFloat(),
                tempMin = json.optDouble("min", 14.0).toFloat(),
                weatherCode = json.optInt("code", 1),
                conditionText = json.optString("condition", "Partly Cloudy"),
                humidity = json.optInt("humidity", 62),
                windSpeedKmH = json.optDouble("wind", 14.0).toFloat(),
                uvIndex = json.optDouble("uv", 4.2).toFloat(),
                rainChance = json.optInt("rain", 10),
                sunrise = json.optString("sunrise", "06:24"),
                sunset = json.optString("sunset", "19:48"),
                hourlyForecast = if (hourlyList.isNotEmpty()) hourlyList else getDefaultHourly(),
                dailyForecast = if (dailyList.isNotEmpty()) dailyList else getDefaultDaily(),
                lastUpdatedMillis = json.optLong("updated", System.currentTimeMillis())
            )
        }

        fun getDefaultHourly(): List<HourlyForecastItem> = listOf(
            HourlyForecastItem("Now", 21f, 1, 0),
            HourlyForecastItem("1 PM", 23f, 1, 0),
            HourlyForecastItem("2 PM", 24f, 2, 5),
            HourlyForecastItem("3 PM", 23f, 2, 10),
            HourlyForecastItem("4 PM", 22f, 3, 15),
            HourlyForecastItem("5 PM", 20f, 1, 0)
        )

        fun getDefaultDaily(): List<DailyForecastItem> = listOf(
            DailyForecastItem("Today", 24f, 14f, 1, 10),
            DailyForecastItem("Tue", 23f, 15f, 2, 20),
            DailyForecastItem("Wed", 20f, 13f, 61, 75),
            DailyForecastItem("Thu", 21f, 12f, 3, 10),
            DailyForecastItem("Fri", 25f, 16f, 0, 0),
            DailyForecastItem("Sat", 26f, 17f, 1, 5),
            DailyForecastItem("Sun", 24f, 15f, 2, 15)
        )

        fun createDemoData(cityName: String = "San Francisco"): WeatherData = WeatherData(
            cityName = cityName,
            latitude = 37.7749,
            longitude = -122.4194,
            currentTemp = 21f,
            feelsLike = 20f,
            tempMax = 24f,
            tempMin = 14f,
            weatherCode = 1,
            conditionText = "Partly Cloudy",
            humidity = 62,
            windSpeedKmH = 14f,
            uvIndex = 4.2f,
            rainChance = 10,
            sunrise = "06:24",
            sunset = "19:48",
            hourlyForecast = getDefaultHourly(),
            dailyForecast = getDefaultDaily(),
            lastUpdatedMillis = System.currentTimeMillis()
        )
    }
}

object WeatherPreferences {
    private const val PREFS_NAME = "slate_weather_prefs"

    const val UNIT_CELSIUS = "CELSIUS"
    const val UNIT_FAHRENHEIT = "FAHRENHEIT"

    fun getUnit(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("temp_unit", UNIT_CELSIUS) ?: UNIT_CELSIUS
    }

    fun setUnit(context: Context, unit: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("temp_unit", unit).apply()
    }

    fun getSelectedCity(context: Context): WeatherCity {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("selected_city", null)
        return if (!jsonStr.isNullOrBlank()) {
            try {
                WeatherCity.fromJson(JSONObject(jsonStr))
            } catch (_: Exception) {
                WeatherCity(1, "San Francisco", "United States", "California", 37.7749, -122.4194)
            }
        } else {
            WeatherCity(1, "San Francisco", "United States", "California", 37.7749, -122.4194)
        }
    }

    fun setSelectedCity(context: Context, city: WeatherCity) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("selected_city", city.toJson().toString()).apply()
    }

    fun isAutoGpsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("auto_gps_enabled", false)
    }

    fun setAutoGpsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_gps_enabled", enabled).apply()
    }

    fun getCachedWeatherData(context: Context): WeatherData {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("cached_weather_data", null)
        return if (!jsonStr.isNullOrBlank()) {
            try {
                WeatherData.fromJson(JSONObject(jsonStr))
            } catch (_: Exception) {
                WeatherData.createDemoData()
            }
        } else {
            WeatherData.createDemoData()
        }
    }

    fun setCachedWeatherData(context: Context, data: WeatherData) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("cached_weather_data", data.toJson().toString()).apply()
    }

    /**
     * Converts temperature according to user preference.
     */
    fun formatTemp(celsius: Float, unit: String): String {
        val value = if (unit == UNIT_FAHRENHEIT) (celsius * 9f / 5f) + 32f else celsius
        return "${Math.round(value)}°"
    }

    fun formatTempValue(celsius: Float, unit: String): Int {
        val value = if (unit == UNIT_FAHRENHEIT) (celsius * 9f / 5f) + 32f else celsius
        return Math.round(value)
    }
}

object WeatherRepository {

    fun getWeatherConditionText(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing Drizzle"
            61 -> "Slight Rain"
            63 -> "Moderate Rain"
            65 -> "Heavy Rain"
            66, 67 -> "Freezing Rain"
            71 -> "Slight Snow"
            73 -> "Moderate Snow"
            75 -> "Heavy Snow"
            77 -> "Snow Grains"
            80, 81, 82 -> "Rain Showers"
            85, 86 -> "Snow Showers"
            95 -> "Thunderstorm"
            96, 99 -> "Heavy Thunderstorm"
            else -> "Partly Cloudy"
        }
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownGpsLocation(context: Context): Pair<Double, Double>? {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val providers = lm.getProviders(true)
            var bestLoc: Location? = null
            for (p in providers) {
                val l = lm.getLastKnownLocation(p) ?: continue
                if (bestLoc == null || l.accuracy < bestLoc.accuracy) {
                    bestLoc = l
                }
            }
            if (bestLoc != null) Pair(bestLoc.latitude, bestLoc.longitude) else null
        } catch (_: Exception) { null }
    }

    suspend fun searchCities(query: String): List<WeatherCity> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()
        val encoded = Uri.encode(query.trim())
        val urlString = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=10&language=en&format=json"

        val list = mutableListOf<WeatherCity>()
        var conn: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "SlateWidgets/1.0")

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()

                val root = JSONObject(sb.toString())
                val results = root.optJSONArray("results")
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val obj = results.optJSONObject(i) ?: continue
                        list.add(
                            WeatherCity(
                                id = obj.optLong("id", i.toLong()),
                                name = obj.optString("name", ""),
                                country = obj.optString("country", ""),
                                admin1 = obj.optString("admin1", ""),
                                latitude = obj.optDouble("latitude", 0.0),
                                longitude = obj.optDouble("longitude", 0.0)
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            conn?.disconnect()
        }
        list
    }

    suspend fun fetchWeather(lat: Double, lon: Double, cityName: String): WeatherData? = withContext(Dispatchers.IO) {
        val urlString = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,uv_index" +
                "&hourly=temperature_2m,weather_code,precipitation_probability" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset" +
                "&timezone=auto"

        var conn: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "SlateWidgets/1.0")

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()

                val root = JSONObject(sb.toString())
                val current = root.optJSONObject("current") ?: return@withContext null
                val daily = root.optJSONObject("daily")
                val hourly = root.optJSONObject("hourly")

                val curTemp = current.optDouble("temperature_2m", 20.0).toFloat()
                val feelsLike = current.optDouble("apparent_temperature", curTemp.toDouble()).toFloat()
                val weatherCode = current.optInt("weather_code", 0)
                val humidity = current.optInt("relative_humidity_2m", 50)
                val windSpeed = current.optDouble("wind_speed_10m", 10.0).toFloat()
                val uvIndex = current.optDouble("uv_index", 3.0).toFloat()

                var maxTemp = curTemp + 3f
                var minTemp = curTemp - 5f
                var rainChance = 0
                var sunriseStr = "06:00"
                var sunsetStr = "19:00"

                val dailyList = mutableListOf<DailyForecastItem>()
                if (daily != null) {
                    val times = daily.optJSONArray("time")
                    val maxArr = daily.optJSONArray("temperature_2m_max")
                    val minArr = daily.optJSONArray("temperature_2m_min")
                    val codeArr = daily.optJSONArray("weather_code")
                    val rainArr = daily.optJSONArray("precipitation_probability_max")
                    val sunriseArr = daily.optJSONArray("sunrise")
                    val sunsetArr = daily.optJSONArray("sunset")

                    if (maxArr != null && maxArr.length() > 0) maxTemp = maxArr.optDouble(0, curTemp.toDouble()).toFloat()
                    if (minArr != null && minArr.length() > 0) minTemp = minArr.optDouble(0, curTemp.toDouble()).toFloat()
                    if (rainArr != null && rainArr.length() > 0) rainChance = rainArr.optInt(0, 0)
                    if (sunriseArr != null && sunriseArr.length() > 0) {
                        sunriseStr = extractTimePart(sunriseArr.optString(0, ""))
                    }
                    if (sunsetArr != null && sunsetArr.length() > 0) {
                        sunsetStr = extractTimePart(sunsetArr.optString(0, ""))
                    }

                    val count = times?.length() ?: 0
                    val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val sdfOut = SimpleDateFormat("EEE", Locale.US)

                    for (i in 0 until minOf(count, 7)) {
                        val dateStr = times?.optString(i, "") ?: ""
                        val dayLabel = if (i == 0) "Today" else {
                            try {
                                val d = sdfIn.parse(dateStr)
                                if (d != null) sdfOut.format(d) else "Day"
                            } catch (_: Exception) { "Day" }
                        }
                        val dMax = maxArr?.optDouble(i, 20.0)?.toFloat() ?: 20f
                        val dMin = minArr?.optDouble(i, 15.0)?.toFloat() ?: 15f
                        val dCode = codeArr?.optInt(i, 1) ?: 1
                        val dRain = rainArr?.optInt(i, 0) ?: 0
                        dailyList.add(DailyForecastItem(dayLabel, dMax, dMin, dCode, dRain))
                    }
                }

                val hourlyList = mutableListOf<HourlyForecastItem>()
                if (hourly != null) {
                    val hTimes = hourly.optJSONArray("time")
                    val hTemps = hourly.optJSONArray("temperature_2m")
                    val hCodes = hourly.optJSONArray("weather_code")
                    val hRains = hourly.optJSONArray("precipitation_probability")

                    val count = hTimes?.length() ?: 0
                    val sdfHourIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
                    val sdfHourOut = SimpleDateFormat("h a", Locale.US)
                    val nowMillis = System.currentTimeMillis()

                    var added = 0
                    for (i in 0 until count) {
                        val timeStr = hTimes?.optString(i, "") ?: continue
                        val date = try { sdfHourIn.parse(timeStr) } catch (_: Exception) { null }
                        if (date != null && date.time >= nowMillis - (30 * 60 * 1000L)) {
                            val label = if (added == 0) "Now" else sdfHourOut.format(date)
                            val hTemp = hTemps?.optDouble(i, curTemp.toDouble())?.toFloat() ?: curTemp
                            val hCode = hCodes?.optInt(i, weatherCode) ?: weatherCode
                            val hRain = hRains?.optInt(i, 0) ?: 0
                            hourlyList.add(HourlyForecastItem(label, hTemp, hCode, hRain))
                            added++
                            if (added >= 12) break
                        }
                    }
                }

                return@withContext WeatherData(
                    cityName = cityName,
                    latitude = lat,
                    longitude = lon,
                    currentTemp = curTemp,
                    feelsLike = feelsLike,
                    tempMax = maxTemp,
                    tempMin = minTemp,
                    weatherCode = weatherCode,
                    conditionText = getWeatherConditionText(weatherCode),
                    humidity = humidity,
                    windSpeedKmH = windSpeed,
                    uvIndex = uvIndex,
                    rainChance = rainChance,
                    sunrise = sunriseStr,
                    sunset = sunsetStr,
                    hourlyForecast = if (hourlyList.isNotEmpty()) hourlyList else WeatherData.getDefaultHourly(),
                    dailyForecast = if (dailyList.isNotEmpty()) dailyList else WeatherData.getDefaultDaily(),
                    lastUpdatedMillis = System.currentTimeMillis()
                )
            }
        } catch (_: Exception) {
        } finally {
            conn?.disconnect()
        }
        null
    }

    private fun extractTimePart(isoString: String): String {
        if (isoString.isBlank()) return "06:00"
        return try {
            val parts = isoString.split("T")
            if (parts.size > 1) {
                val timeParts = parts[1].split(":")
                "${timeParts[0]}:${timeParts[1]}"
            } else isoString
        } catch (_: Exception) { isoString }
    }

    /**
     * Resolves click intent:
     * 1. Attempts to launch system weather app or Google Weather
     * 2. Otherwise opens WeatherConfigActivity
     */
    fun createWeatherClickIntent(context: Context, widgetId: Int): Intent {
        val pm = context.packageManager
        // 1. Google Weather via Search intent
        val googleWeatherIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("dynact://velour/weather/ProxyActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (googleWeatherIntent.resolveActivity(pm) != null) {
            return googleWeatherIntent
        }

        // 2. Standard weather action or package intent
        val knownPackages = listOf(
            "com.google.android.apps.weather",
            "com.sec.android.daemonapp", // Samsung
            "com.miui.weather2", // Xiaomi
            "com.coloros.weather2", // Oppo / Realme
            "com.oneplus.weather" // OnePlus
        )
        for (pkg in knownPackages) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                return launchIntent.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            }
        }

        // 3. Fallback: Open Slate Weather Studio
        return Intent(context, WeatherConfigActivity::class.java).apply {
            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }
}

class WeatherUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val city = WeatherPreferences.getSelectedCity(context)
            val freshData = WeatherRepository.fetchWeather(city.latitude, city.longitude, city.name)
            if (freshData != null) {
                WeatherPreferences.setCachedWeatherData(context, freshData)
                updateAllWeatherWidgets(context)
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
