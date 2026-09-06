package com.altusix.slate.widgets.media

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.service.notification.NotificationListenerService
import android.view.KeyEvent

class SlateMediaNotificationService : NotificationListenerService() {

    companion object {
        @Volatile
        private var activeController: MediaController? = null

        fun getActiveController(): MediaController? = activeController

        fun handlePlayPause(context: Context) {
            val controller = activeController
            if (controller != null) {
                val pbState = controller.playbackState?.state
                if (pbState == PlaybackState.STATE_PLAYING) {
                    controller.transportControls.pause()
                } else {
                    controller.transportControls.play()
                }
            } else {
                // Generic fallback media key event via AudioManager
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
            }
        }

        fun handleNext(context: Context) {
            val controller = activeController
            if (controller != null) {
                controller.transportControls.skipToNext()
            } else {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT))
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT))
            }
        }

        fun handlePrev(context: Context) {
            val controller = activeController
            if (controller != null) {
                controller.transportControls.skipToPrevious()
            } else {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
            }
        }

        fun openActivePlayer(context: Context) {
            // 1. Recover active controller if the static in-memory reference was cleared
            var controller = activeController
            if (controller == null) {
                try {
                    val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                    val component = ComponentName(context, SlateMediaNotificationService::class.java)
                    val controllers = sessionManager?.getActiveSessions(component)
                    controller = controllers?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                        ?: controllers?.firstOrNull()
                    if (controller != null) activeController = controller
                } catch (_: Exception) {}
            }

            // 2. Launch directly via session activity (deep link into the player's UI)
            if (controller?.sessionActivity != null) {
                try {
                    controller.sessionActivity?.send()
                    return
                } catch (_: Exception) {}
            }

            // 3. Fallback: Launch whichever app is actively broadcasting media
            val targetPkg = controller?.packageName ?: MediaStateManager.loadState(context).packageName
            if (!targetPkg.isNullOrBlank()) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    context.startActivity(launchIntent)
                    return
                }
            }

            // 4. Default media app fallbacks
            val defaultPackages = listOf(
                "com.spotify.music",
                "com.google.android.apps.youtube.music",
                "com.apple.android.music",
                "com.soundcloud.android"
            )
            for (pkg in defaultPackages) {
                val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return
                }
            }

            try {
                val musicIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_MUSIC)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(musicIntent)
            } catch (_: Exception) {}
        }
    }

    private var sessionManager: MediaSessionManager? = null

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveSession(controllers)
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            syncCurrentMedia()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            syncCurrentMedia()
        }

        override fun onSessionDestroyed() {
            activeController = null
            syncCurrentMedia()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        initMediaSessions()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        sessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
    }

    private fun initMediaSessions() {
        try {
            sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val component = ComponentName(this, SlateMediaNotificationService::class.java)
            sessionManager?.addOnActiveSessionsChangedListener(sessionsChangedListener, component)
            val controllers = sessionManager?.getActiveSessions(component)
            updateActiveSession(controllers)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateActiveSession(controllers: List<MediaController>?) {
        activeController?.unregisterCallback(controllerCallback)

        if (controllers.isNullOrEmpty()) {
            activeController = null
            syncCurrentMedia()
            return
        }

        // Prefer playing session, otherwise pick the first available
        val playingController = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers.firstOrNull()

        activeController = playingController
        activeController?.registerCallback(controllerCallback)
        syncCurrentMedia()
    }

    private fun syncCurrentMedia() {
        val controller = activeController
        if (controller == null) {
            MediaStateManager.saveState(this, SlateMediaState(title = "No Media Playing", artist = "Tap to play music", isPlaying = false))
            updateAllMediaWidgets(this)
            return
        }

        val metadata = controller.metadata
        val playbackState = controller.playbackState

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.description?.title?.toString()
            ?: "Unknown Track"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.description?.subtitle?.toString()
            ?: "Unknown Artist"
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        val position = playbackState?.position ?: 0L
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val packageName = controller.packageName

        var artBitmap: Bitmap? = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.description?.iconBitmap

        // Downsample very large album art to protect memory
        if (artBitmap != null && (artBitmap.width > 512 || artBitmap.height > 512)) {
            val maxDim = 512
            val w = artBitmap.width
            val h = artBitmap.height
            val scale = maxDim.toFloat() / maxOf(w, h)
            val newW = (w * scale).toInt().coerceAtLeast(1)
            val newH = (h * scale).toInt().coerceAtLeast(1)
            try {
                artBitmap = Bitmap.createScaledBitmap(artBitmap, newW, newH, true)
            } catch (_: Exception) {}
        }

        val state = SlateMediaState(
            title = title,
            artist = artist,
            album = album,
            isPlaying = isPlaying,
            positionMs = position,
            durationMs = duration,
            packageName = packageName
        )

        MediaStateManager.saveState(this, state, artBitmap)
        updateAllMediaWidgets(this)
    }
}
