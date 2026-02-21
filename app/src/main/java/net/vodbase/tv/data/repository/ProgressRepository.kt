package net.vodbase.tv.data.repository

import net.vodbase.tv.data.api.VodBaseApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepository @Inject constructor(
    private val api: VodBaseApi,
    private val authRepository: AuthRepository
) {
    private val watchedCache = mutableMapOf<String, MutableSet<String>>()

    suspend fun getResumePosition(streamer: String): Pair<String, Double>? {
        val token = authRepository.getDeviceToken() ?: return null
        return try {
            val progress = api.getProgress(streamer, token)
            progress.lastWatchedVideo?.let { lw ->
                if (lw.resumeAvailable) Pair(lw.id, lw.currentTime) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveProgress(streamer: String, videoId: String, title: String, url: String, currentTime: Double, duration: Double) {
        val token = authRepository.getDeviceToken() ?: return
        try {
            api.saveProgress(streamer, token, mapOf(
                "videoId" to videoId,
                "videoTitle" to title,
                "videoUrl" to url,
                "currentTime" to currentTime,
                "duration" to duration
            ))
        } catch (e: Exception) {
            // Silently fail - progress save is best-effort
        }
    }

    suspend fun getWatchedIds(streamer: String): Set<String> {
        if (watchedCache.containsKey(streamer)) return watchedCache[streamer]!!
        val token = authRepository.getDeviceToken() ?: return emptySet()
        return try {
            val response = api.getWatched(streamer, token)
            val set = response.watchedVideos.toMutableSet()
            watchedCache[streamer] = set
            set
        } catch (e: Exception) {
            emptySet()
        }
    }

    suspend fun markWatched(streamer: String, videoId: String, title: String, duration: String) {
        watchedCache.getOrPut(streamer) { mutableSetOf() }.add(videoId)
        val token = authRepository.getDeviceToken() ?: return
        try {
            api.trackWatch(token, mapOf(
                "videoId" to videoId,
                "streamer" to streamer,
                "title" to title,
                "duration" to duration
            ))
        } catch (e: Exception) {
            // Best effort
        }
    }

    fun isWatched(streamer: String, videoId: String): Boolean {
        return watchedCache[streamer]?.contains(videoId) == true
    }
}
