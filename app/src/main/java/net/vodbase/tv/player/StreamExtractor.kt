package net.vodbase.tv.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeProgressiveDashManifestCreator
import org.schabi.newpipe.extractor.stream.StreamInfo
import javax.inject.Inject
import javax.inject.Singleton

data class ExtractedStream(
    val videoUrl: String,
    val audioUrl: String?,
    val hlsUrl: String?,
    val videoDashManifest: String?,
    val audioDashManifest: String?,
    val resolution: String,
    val isAdaptive: Boolean
)

@Singleton
class StreamExtractor @Inject constructor() {

    init {
        NewPipe.init(DownloaderImpl.getInstance())
    }

    suspend fun extractStream(youtubeId: String): ExtractedStream = withContext(Dispatchers.IO) {
        val url = "https://www.youtube.com/watch?v=$youtubeId"
        val info = StreamInfo.getInfo(ServiceList.YouTube, url)

        // Prefer HLS (segment-based = fast seeking)
        val hlsUrl = info.hlsUrl?.takeIf { it.isNotBlank() }

        // Extract adaptive streams (video-only + audio-only)
        val videoOnly = info.videoOnlyStreams
            .sortedByDescending { it.resolution?.replace("p", "")?.toIntOrNull() ?: 0 }
        val audioOnly = info.audioStreams
            .sortedByDescending { it.averageBitrate }

        if (videoOnly.isNotEmpty() && audioOnly.isNotEmpty()) {
            val bestVideo = videoOnly.first()
            val bestAudio = audioOnly.first()
            val durationSec = info.duration // seconds

            // Generate synthetic DASH manifests from progressive URLs
            // This enables segment-based seeking (like HLS) instead of byte-range seeking
            var videoDash: String? = null
            var audioDash: String? = null

            try {
                val videoItag = bestVideo.itagItem
                if (videoItag != null && bestVideo.isUrl) {
                    videoDash = YoutubeProgressiveDashManifestCreator
                        .fromProgressiveStreamingUrl(bestVideo.content, videoItag, durationSec)
                    android.util.Log.i("VodPlayer", "Generated DASH manifest for video (itag=${videoItag.id})")
                }
            } catch (e: Exception) {
                android.util.Log.w("VodPlayer", "Failed to generate video DASH manifest: ${e.message}")
            }

            try {
                val audioItag = bestAudio.itagItem
                if (audioItag != null && bestAudio.isUrl) {
                    audioDash = YoutubeProgressiveDashManifestCreator
                        .fromProgressiveStreamingUrl(bestAudio.content, audioItag, durationSec)
                    android.util.Log.i("VodPlayer", "Generated DASH manifest for audio (itag=${audioItag.id})")
                }
            } catch (e: Exception) {
                android.util.Log.w("VodPlayer", "Failed to generate audio DASH manifest: ${e.message}")
            }

            return@withContext ExtractedStream(
                videoUrl = bestVideo.content,
                audioUrl = bestAudio.content,
                hlsUrl = hlsUrl,
                videoDashManifest = videoDash,
                audioDashManifest = audioDash,
                resolution = bestVideo.resolution ?: "unknown",
                isAdaptive = true
            )
        }

        // Fallback: progressive (combined video+audio, usually max 720p)
        val progressiveStreams = info.videoStreams
            .filter { !it.isVideoOnly }
            .sortedByDescending { it.resolution?.replace("p", "")?.toIntOrNull() ?: 0 }

        if (progressiveStreams.isNotEmpty()) {
            val best = progressiveStreams.first()
            return@withContext ExtractedStream(
                videoUrl = best.content,
                audioUrl = null,
                hlsUrl = hlsUrl,
                videoDashManifest = null,
                audioDashManifest = null,
                resolution = best.resolution ?: "unknown",
                isAdaptive = false
            )
        }

        // Last resort: HLS only
        if (hlsUrl != null) {
            return@withContext ExtractedStream(
                videoUrl = hlsUrl,
                audioUrl = null,
                hlsUrl = hlsUrl,
                videoDashManifest = null,
                audioDashManifest = null,
                resolution = "auto",
                isAdaptive = false
            )
        }

        throw IllegalStateException("No streams found for $youtubeId")
    }
}
