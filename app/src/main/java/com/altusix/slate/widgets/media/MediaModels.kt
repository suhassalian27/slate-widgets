package com.altusix.slate.widgets.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

data class SlateMediaState(
    val title: String = "No Media Playing",
    val artist: String = "Tap to play music",
    val album: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val packageName: String? = null
) {
    val hasActiveMedia: Boolean
        get() = title.isNotBlank() && title != "No Media Playing"

    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0.35f
}

object MediaStateManager {
    private const val PREFS_NAME = "slate_media_prefs"
    private const val KEY_TITLE = "media_title"
    private const val KEY_ARTIST = "media_artist"
    private const val KEY_ALBUM = "media_album"
    private const val KEY_IS_PLAYING = "media_is_playing"
    private const val KEY_POSITION = "media_position"
    private const val KEY_DURATION = "media_duration"
    private const val KEY_PACKAGE = "media_package"

    @Volatile
    private var inMemoryArtwork: Bitmap? = null

    fun saveState(context: Context, state: SlateMediaState, artwork: Bitmap? = null) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_TITLE, state.title)
            .putString(KEY_ARTIST, state.artist)
            .putString(KEY_ALBUM, state.album)
            .putBoolean(KEY_IS_PLAYING, state.isPlaying)
            .putLong(KEY_POSITION, state.positionMs)
            .putLong(KEY_DURATION, state.durationMs)
            .putString(KEY_PACKAGE, state.packageName)
            .apply()

        if (artwork != null) {
            inMemoryArtwork = artwork
            try {
                val file = File(context.cacheDir, "slate_media_artwork.png")
                FileOutputStream(file).use { out ->
                    artwork.compress(Bitmap.CompressFormat.PNG, 90, out)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (!state.hasActiveMedia) {
            inMemoryArtwork = null
            try {
                val file = File(context.cacheDir, "slate_media_artwork.png")
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
        }
    }

    fun loadState(context: Context): SlateMediaState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val title = prefs.getString(KEY_TITLE, "No Media Playing") ?: "No Media Playing"
        val artist = prefs.getString(KEY_ARTIST, "Tap to play music") ?: "Tap to play music"
        val album = prefs.getString(KEY_ALBUM, "") ?: ""
        val isPlaying = prefs.getBoolean(KEY_IS_PLAYING, false)
        val position = prefs.getLong(KEY_POSITION, 0L)
        val duration = prefs.getLong(KEY_DURATION, 0L)
        val pkg = prefs.getString(KEY_PACKAGE, null)

        return SlateMediaState(
            title = title,
            artist = artist,
            album = album,
            isPlaying = isPlaying,
            positionMs = position,
            durationMs = duration,
            packageName = pkg
        )
    }

    fun getArtwork(context: Context): Bitmap? {
        if (inMemoryArtwork != null && !inMemoryArtwork!!.isRecycled) {
            return inMemoryArtwork
        }
        return try {
            val file = File(context.cacheDir, "slate_media_artwork.png")
            if (file.exists()) {
                val bmp = BitmapFactory.decodeFile(file.absolutePath)
                inMemoryArtwork = bmp
                bmp
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getMockPreviewState(): Pair<SlateMediaState, Bitmap?> {
        val mockState = SlateMediaState(
            title = "Midnight City",
            artist = "M83 • Hurry Up, We're Dreaming",
            album = "Hurry Up, We're Dreaming",
            isPlaying = true,
            positionMs = 124000L,
            durationMs = 243000L,
            packageName = "com.spotify.music"
        )
        return Pair(mockState, inMemoryArtwork)
    }
}
