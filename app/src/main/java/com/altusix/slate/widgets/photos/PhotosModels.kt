package com.altusix.slate.widgets.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

enum class MemoryFilterStyle(val label: String) {
    ORIGINAL("Original"),
    MONOCHROME("Monochrome"),
    WARM_SEPIA("Warm Sepia"),
    MOODY_DARK("Moody Dark"),
    RETRO_FILM("Retro Film"),
    GOLDEN_HOUR("Golden Hour")
}

enum class CaptionFont(val label: String) {
    SANS("Sans"),
    SERIF("Serif"),
    MONO("Mono"),
    SCRIPT("Script")
}

data class SlateMemoryItem(
    val id: String = System.currentTimeMillis().toString(),
    val imagePath: String? = null,
    val caption: String = "Summer Memories",
    val dateText: String = "September 2024",
    val location: String = "Pacific Coast",
    val filterStyle: MemoryFilterStyle = MemoryFilterStyle.ORIGINAL,
    val captionFont: CaptionFont = CaptionFont.SANS,
    val captionColorHex: Long = 0xFFFFFFFFL
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("imagePath", imagePath ?: "")
        put("caption", caption)
        put("dateText", dateText)
        put("location", location)
        put("filterStyle", filterStyle.name)
        put("captionFont", captionFont.name)
        put("captionColorHex", captionColorHex)
    }

    companion object {
        fun fromJson(json: JSONObject): SlateMemoryItem {
            val filter = try {
                MemoryFilterStyle.valueOf(json.optString("filterStyle", MemoryFilterStyle.ORIGINAL.name))
            } catch (_: Exception) {
                MemoryFilterStyle.ORIGINAL
            }
            val font = try {
                CaptionFont.valueOf(json.optString("captionFont", CaptionFont.SANS.name))
            } catch (_: Exception) {
                CaptionFont.SANS
            }
            val path = json.optString("imagePath", "")
            return SlateMemoryItem(
                id = json.optString("id", System.currentTimeMillis().toString()),
                imagePath = if (path.isBlank()) null else path,
                caption = json.optString("caption", "Summer Memories"),
                dateText = json.optString("dateText", "September 2024"),
                location = json.optString("location", "Pacific Coast"),
                filterStyle = filter,
                captionFont = font,
                captionColorHex = json.optLong("captionColorHex", 0xFFFFFFFFL)
            )
        }
    }
}

data class PhotosWidgetConfig(
    val items: List<SlateMemoryItem> = emptyList(),
    val currentIndex: Int = 0,
    val showDate: Boolean = true,
    val showCaption: Boolean = true,
    val isResponsive: Boolean = false
) {
    val currentItem: SlateMemoryItem?
        get() = if (items.isNotEmpty()) items[currentIndex.coerceIn(0, items.size - 1)] else null

    fun toJson(): String {
        val json = JSONObject()
        json.put("currentIndex", currentIndex)
        json.put("showDate", showDate)
        json.put("showCaption", showCaption)
        json.put("isResponsive", isResponsive)

        val arr = JSONArray()
        for (item in items) {
            arr.put(item.toJson())
        }
        json.put("items", arr)
        return json.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): PhotosWidgetConfig {
            return try {
                val json = JSONObject(jsonStr)
                val itemsList = mutableListOf<SlateMemoryItem>()
                val arr = json.optJSONArray("items")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i)
                        if (obj != null) {
                            itemsList.add(SlateMemoryItem.fromJson(obj))
                        }
                    }
                }
                PhotosWidgetConfig(
                    items = itemsList,
                    currentIndex = json.optInt("currentIndex", 0),
                    showDate = json.optBoolean("showDate", true),
                    showCaption = json.optBoolean("showCaption", true),
                    isResponsive = json.optBoolean("isResponsive", false)
                )
            } catch (_: Exception) {
                getDefaultConfig()
            }
        }

        fun getDefaultConfig(): PhotosWidgetConfig {
            return PhotosWidgetConfig(
                items = listOf(
                    SlateMemoryItem(
                        id = "default_1",
                        caption = "Coastal Highway",
                        dateText = "September 2024",
                        location = "Big Sur, California",
                        filterStyle = MemoryFilterStyle.ORIGINAL
                    ),
                    SlateMemoryItem(
                        id = "default_2",
                        caption = "Golden Hour Horizon",
                        dateText = "August 2024",
                        location = "Kyoto, Japan",
                        filterStyle = MemoryFilterStyle.GOLDEN_HOUR
                    ),
                    SlateMemoryItem(
                        id = "default_3",
                        caption = "Mountain Mist",
                        dateText = "July 2024",
                        location = "Swiss Alps",
                        filterStyle = MemoryFilterStyle.MONOCHROME
                    )
                ),
                currentIndex = 0,
                showDate = true,
                showCaption = true,
                isResponsive = false
            )
        }
    }
}

object PhotosStorageManager {
    private const val PREFS_NAME = "slate_photos_widget_prefs"

    fun getConfig(context: Context, widgetId: Int): PhotosWidgetConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("widget_${widgetId}_config", null)
        if (!jsonStr.isNullOrBlank()) {
            return PhotosWidgetConfig.fromJson(jsonStr)
        }
        val def = PhotosWidgetConfig.getDefaultConfig()
        saveConfig(context, widgetId, def)
        return def
    }

    fun saveConfig(context: Context, widgetId: Int, config: PhotosWidgetConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("widget_${widgetId}_config", config.toJson()).apply()
    }

    fun cyclePhoto(context: Context, widgetId: Int, delta: Int): PhotosWidgetConfig {
        val config = getConfig(context, widgetId)
        if (config.items.isEmpty()) return config

        val count = config.items.size
        val next = (config.currentIndex + delta + count) % count
        val updated = config.copy(currentIndex = next)
        saveConfig(context, widgetId, updated)
        return updated
    }

    fun copyUriToInternalStorage(context: Context, widgetId: Int, index: Int, uri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "photos_widget")
            if (!dir.exists()) dir.mkdirs()

            val targetFile = File(dir, "photo_${widgetId}_$index.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun loadBitmap(path: String?, maxWidth: Int = 1024, maxHeight: Int = 1024): Bitmap? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.exists()) return null

        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

            val srcW = boundsOptions.outWidth
            val srcH = boundsOptions.outHeight
            var inSampleSize = 1

            while ((srcW / inSampleSize) > maxWidth || (srcH / inSampleSize) > maxHeight) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}