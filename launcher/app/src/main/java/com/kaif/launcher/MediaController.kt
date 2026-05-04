package com.kaif.launcher

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent

class MediaController(private val context: Context) {

    private val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

    data class MediaInfo(
        val title: String,
        val artist: String,
        val isPlaying: Boolean
    )

    fun getInfo(): MediaInfo? {
        return try {
            // Try with NotificationService component
            val component = ComponentName(context, NotificationService::class.java)
            val controllers = try {
                msm.getActiveSessions(component)
            } catch (e: Exception) {
                // Fallback — try with null
                msm.getActiveSessions(null)
            }
            val active = controllers.firstOrNull { it.playbackState != null }
                ?: controllers.firstOrNull()
                ?: return null
            val meta    = active.metadata ?: return null
            val title   = meta.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: return null
            val artist  = meta.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: ""
            val state   = active.playbackState?.state
            val playing = state == PlaybackState.STATE_PLAYING
            MediaInfo(title, artist, playing)
        } catch (e: Exception) {
            null
        }
    }

    fun playPause() = sendKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    fun next()      = sendKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    fun prev()      = sendKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)

    private fun sendKey(keyCode: Int) {
        try {
            val component = ComponentName(context, NotificationService::class.java)
            val controllers = try {
                msm.getActiveSessions(component)
            } catch (e: Exception) {
                msm.getActiveSessions(null)
            }
            controllers.firstOrNull()?.dispatchMediaButtonEvent(
                KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            )
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    controllers.firstOrNull()?.dispatchMediaButtonEvent(
                        KeyEvent(KeyEvent.ACTION_UP, keyCode)
                    )
                } catch (e: Exception) { }
            }, 100)
        } catch (e: Exception) { }
    }
}
