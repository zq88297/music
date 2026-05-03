package com.example.rearlyrics.media

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.rearlyrics.model.LyricsRepository

class MediaNotificationListenerService : NotificationListenerService() {
    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        refreshFromControllers(controllers.orEmpty())
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        attachSessionListener()
    }

    override fun onListenerDisconnected() {
        runCatching {
            val manager = getSystemService(MediaSessionManager::class.java)
            manager?.removeOnActiveSessionsChangedListener(sessionListener)
        }
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        attachSessionListener()
    }

    private fun attachSessionListener() {
        val manager = getSystemService(MediaSessionManager::class.java) ?: return
        val component = ComponentName(this, javaClass)
        runCatching {
            manager.addOnActiveSessionsChangedListener(sessionListener, component)
            refreshFromControllers(manager.getActiveSessions(component))
        }.onFailure {
            Log.w(TAG, "Unable to inspect active media sessions", it)
        }
    }

    private fun refreshFromControllers(controllers: List<MediaController>) {
        val active = controllers.firstOrNull { controller ->
            controller.playbackState?.state != null && controller.metadata != null
        } ?: controllers.firstOrNull()

        val metadata = active?.metadata
        val metadataDump = dumpMetadata(metadata)
        val extrasDump = dumpExtras(active)
        val candidates = collectLyricCandidates(metadata, active)
        val lyricCandidate = candidates.firstOrNull(::looksLikeLyrics).orEmpty()

        LyricsRepository.updatePlayback(
            packageName = active?.packageName,
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
            lyricCandidate = lyricCandidate,
            metadataDump = metadataDump,
            extrasDump = extrasDump,
            detectedLyricCandidates = candidates.filter(::looksLikeLyrics),
        )
    }

    private fun collectLyricCandidates(
        metadata: MediaMetadata?,
        controller: MediaController?,
    ): List<String> {
        return buildList {
            add(metadata?.getString("android.media.metadata.LYRIC"))
            add(metadata?.getString("android.media.metadata.LYRICS"))
            add(metadata?.getString("lyric"))
            add(metadata?.getString("lyrics"))

            controller?.extras?.keySet()?.forEach { key ->
                add(controller.extras?.getString(key))
            }
            metadata?.keySet()?.forEach { key ->
                add(metadata.getString(key))
            }
        }.filterNotNull()
            .filterNotNull()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
    }

    private fun looksLikeLyrics(text: String): Boolean {
        if (text.isBlank()) return false
        val normalized = text.trim()
        if (normalized.length < 8) return false
        if ('\n' in normalized) return true
        val spacedWords = normalized.split(' ').count { it.length > 1 }
        return spacedWords >= 6
    }

    private fun dumpMetadata(metadata: MediaMetadata?): List<String> {
        return metadata?.keySet()
            ?.sorted()
            ?.mapNotNull { key ->
                metadata.getString(key)?.takeIf { it.isNotBlank() }?.let { value ->
                    "$key=${value.take(180)}"
                }
            }
            .orEmpty()
    }

    private fun dumpExtras(controller: MediaController?): List<String> {
        val extras = controller?.extras ?: return emptyList()
        return extras.keySet()
            .sorted()
            .mapNotNull { key ->
                extras.get(key)?.toString()?.takeIf { it.isNotBlank() }?.let { value ->
                    "$key=${value.take(180)}"
                }
            }
    }

    companion object {
        private const val TAG = "RearLyricsListener"
    }
}
