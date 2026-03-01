package net.vodbase.tv.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        private val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        private val AUTOPLAY_NEXT = booleanPreferencesKey("autoplay_next")

        val QUALITY_OPTIONS = listOf("auto", "1080", "720", "480", "360")
        val SPEED_OPTIONS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    }

    val videoQuality: Flow<String> = dataStore.data.map { it[VIDEO_QUALITY] ?: "auto" }
    val playbackSpeed: Flow<Float> = dataStore.data.map { it[PLAYBACK_SPEED] ?: 1.0f }
    val autoplayNext: Flow<Boolean> = dataStore.data.map { it[AUTOPLAY_NEXT] ?: true }

    suspend fun setVideoQuality(quality: String) {
        dataStore.edit { it[VIDEO_QUALITY] = quality }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        dataStore.edit { it[PLAYBACK_SPEED] = speed }
    }

    suspend fun setAutoplayNext(enabled: Boolean) {
        dataStore.edit { it[AUTOPLAY_NEXT] = enabled }
    }
}
