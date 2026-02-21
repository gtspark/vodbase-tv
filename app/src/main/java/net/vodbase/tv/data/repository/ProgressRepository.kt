package net.vodbase.tv.data.repository

import net.vodbase.tv.data.api.VodBaseApi
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepository @Inject constructor(
    private val api: VodBaseApi,
    private val authRepository: AuthRepository
) {
    private val watchedCache = ConcurrentHashMap<String, MutableSet<String>>()

    data class ResumeInfo(
        val vodId: String,
        val title: String?,
        val currentTime: Double,
        val duration: Double
    )

    suspend fun getResumePosition(streamer: String): Pair<String, Double>? {
        val info = getResumeInfo(streamer) ?: return null
        return Pair(info.vodId, info.currentTime)
    }

    suspend fun getResumeInfo(streamer: String): ResumeInfo? {
        val token = authRepository.getDeviceToken() ?: return null
        return try {
            val progress = api.getProgress(streamer, token)
            progress.lastWatchedVideo?.let { lw ->
                if (lw.resumeAvailable) ResumeInfo(lw.id, lw.title, lw.currentTime, lw.duration) else null
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
            val set = java.util.Collections.synchronizedSet(response.watchedVideos.toMutableSet())
            watchedCache[streamer] = set
            set
        } catch (e: Exception) {
            emptySet()
        }
    }

    suspend fun markWatched(streamer: String, videoId: String, title: String, duration: String) {
        watchedCache.getOrPut(streamer) { java.util.Collections.synchronizedSet(mutableSetOf()) }.add(videoId)
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
