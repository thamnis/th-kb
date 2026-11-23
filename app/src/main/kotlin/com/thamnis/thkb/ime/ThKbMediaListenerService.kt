package com.thamnis.thkb.ime

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService

data class MediaPlayerSnapshot(
    val isPlaying: Boolean = false,
    val trackTitle: String = "",
    val hasActiveSession: Boolean = false,
    val listenerEnabled: Boolean = false,
)

class ThKbMediaListenerService : NotificationListenerService() {
    companion object {
        fun isNotificationAccessGranted(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ) ?: return false
            val componentName = ComponentName(context, ThKbMediaListenerService::class.java).flattenToString()
            return flat.split(':').any { it.equals(componentName, ignoreCase = true) }
        }

        private fun primaryController(context: Context): MediaController? {
            if (!isNotificationAccessGranted(context)) return null
            val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                ?: return null
            val component = ComponentName(context, ThKbMediaListenerService::class.java)
            val sessions = runCatching { msm.getActiveSessions(component) }.getOrNull().orEmpty()
            return sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: sessions.firstOrNull()
        }

        fun getSnapshot(context: Context, audioManager: AudioManager?): MediaPlayerSnapshot {
            val granted = isNotificationAccessGranted(context)
            val ctrl = primaryController(context)
            if (ctrl != null) {
                val state = ctrl.playbackState?.state
                val playing = state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
                val title = ctrl.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
                val artist = ctrl.metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
                val label = when {
                    title.isNotEmpty() && artist.isNotEmpty() -> "$title · $artist"
                    title.isNotEmpty() -> title
                    else -> ""
                }
                return MediaPlayerSnapshot(
                    isPlaying = playing,
                    trackTitle = label,
                    hasActiveSession = true,
                    listenerEnabled = true,
                )
            }
            return MediaPlayerSnapshot(
                isPlaying = audioManager?.isMusicActive == true,
                trackTitle = "",
                hasActiveSession = false,
                listenerEnabled = granted,
            )
        }

        fun togglePlayPauseViaSession(context: Context): Boolean {
            val ctrl = primaryController(context) ?: return false
            val state = ctrl.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING) {
                ctrl.transportControls.pause()
            } else {
                ctrl.transportControls.play()
            }
            return true
        }

        fun skipNextViaSession(context: Context): Boolean {
            val ctrl = primaryController(context) ?: return false
            ctrl.transportControls.skipToNext()
            return true
        }

        fun skipPrevViaSession(context: Context): Boolean {
            val ctrl = primaryController(context) ?: return false
            ctrl.transportControls.skipToPrevious()
            return true
        }

        fun seekRelativeViaSession(context: Context, deltaMs: Long): Boolean {
            val ctrl = primaryController(context) ?: return false
            val pb = ctrl.playbackState ?: return false
            val currentPos = if (pb.state == PlaybackState.STATE_PLAYING && pb.lastPositionUpdateTime > 0L) {
                val elapsed = SystemClock.elapsedRealtime() - pb.lastPositionUpdateTime
                pb.position + (elapsed * pb.playbackSpeed).toLong()
            } else {
                pb.position
            }
            val duration = ctrl.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: Long.MAX_VALUE
            val target = (currentPos + deltaMs).coerceIn(0L, if (duration > 0L) duration else Long.MAX_VALUE)
            ctrl.transportControls.seekTo(target)
            return true
        }
    }
}
