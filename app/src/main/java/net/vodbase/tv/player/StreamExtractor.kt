package net.vodbase.tv.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.youtube.dashmanifestcreators.YoutubeProgressiveDashManifestCreator
import org.schabi.newpipe.extractor.stream.StreamInfo
import net.vodbase.tv.data.model.Chapter
import javax.inject.Inject
import javax.inject.Singleton

data class ExtractedStream(
    val videoUrl: String,
    val audioUrl: String?,
    val hlsUrl: String?,
    val videoDashManifest: String?,
    val audioDashManifest: String?,
    val resolution: String,
    val isAdaptive: Boolean,
    val chapters: List<Chapter> = emptyList()
)

@Singleton
class StreamExtractor @Inject constructor() {

    init {
        NewPipe.init(DownloaderImpl.getInstance())
    }

    suspend fun extractStream(youtubeId: String, preferredHeightPx: Int? = null): ExtractedStream = withContext(Dispatchers.IO) {
        val url = "https://www.youtube.com/watch?v=$youtubeId"
        val info = StreamInfo.getInfo(ServiceList.YouTube, url)

        // Extract chapters from stream segments
        val chapters = try {
            info.streamSegments.map { seg ->
                Chapter(
                    title = seg.title,
                    startTimeMs = seg.startTimeSeconds.toLong() * 1000L,
                    previewUrl = seg.previewUrl
                )
            }
        } catch (_: Exception) { emptyList() }

        if (chapters.isNotEmpty()) {
            android.util.Log.i("VodPlayer", "Found ${chapters.size} chapters: ${chapters.joinToString { "'${it.title}' @${it.startTimeMs/1000}s" }}")
        }

        val hlsUrl = info.hlsUrl?.takeIf { it.isNotBlank() }

        val videoOnly = info.videoOnlyStreams
            .sortedByDescending { it.resolution?.replace("p", "")?.toIntOrNull() ?: 0 }
        val audioOnly = info.audioStreams
            .sortedByDescending { it.averageBitrate }

        if (videoOnly.isNotEmpty() && audioOnly.isNotEmpty()) {
            val bestVideo = if (preferredHeightPx != null) {
                videoOnly.firstOrNull {
                    (it.resolution?.replace("p", "")?.toIntOrNull() ?: 0) <= preferredHeightPx
                } ?: videoOnly.last()
            } else {
                videoOnly.first()
            }
            val bestAudio = audioOnly.first()
            val durationSec = info.duration

            // Generate synthetic DASH manifests for segment-based seeking
            var videoDash: String? = null
            var audioDash: String? = null

            try {
                val videoItag = bestVideo.itagItem
                if (videoItag != null && bestVideo.isUrl) {
                    videoDash = YoutubeProgressiveDashManifestCreator
                        .fromProgressiveStreamingUrl(bestVideo.content, videoItag, durationSec)
                    android.util.Log.i("VodPlayer", "DASH manifest OK for video (itag=${videoItag.id}, ${videoDash.length} chars)")
                }
            } catch (e: Exception) {
                android.util.Log.w("VodPlayer", "DASH video manifest failed: ${e.message}")
            }

            try {
                val audioItag = bestAudio.itagItem
                if (audioItag != null && bestAudio.isUrl) {
                    audioDash = YoutubeProgressiveDashManifestCreator
                        .fromProgressiveStreamingUrl(bestAudio.content, audioItag, durationSec)
                    android.util.Log.i("VodPlayer", "DASH manifest OK for audio (itag=${audioItag.id}, ${audioDash.length} chars)")
                }
            } catch (e: Exception) {
                android.util.Log.w("VodPlayer", "DASH audio manifest failed: ${e.message}")
            }

            return@withContext ExtractedStream(
                videoUrl = bestVideo.content,
                audioUrl = bestAudio.content,
                hlsUrl = hlsUrl,
                videoDashManifest = videoDash,
                audioDashManifest = audioDash,
                resolution = bestVideo.resolution ?: "unknown",
                isAdaptive = true,
                chapters = chapters
            )
        }

        val progressiveStreams = info.videoStreams
            .filter { !it.isVideoOnly }
            .sortedByDescending { it.resolution?.replace("p", "")?.toIntOrNull() ?: 0 }

        if (progressiveStreams.isNotEmpty()) {
            val best = if (preferredHeightPx != null) {
                progressiveStreams.firstOrNull {
                    (it.resolution?.replace("p", "")?.toIntOrNull() ?: 0) <= preferredHeightPx
                } ?: progressiveStreams.last()
            } else {
                progressiveStreams.first()
            }
            return@withContext ExtractedStream(
                videoUrl = best.content,
                audioUrl = null,
                hlsUrl = hlsUrl,
                videoDashManifest = null,
                audioDashManifest = null,
                resolution = best.resolution ?: "unknown",
                isAdaptive = false,
                chapters = chapters
            )
        }

        if (hlsUrl != null) {
            return@withContext ExtractedStream(
                videoUrl = hlsUrl,
                audioUrl = null,
                hlsUrl = hlsUrl,
                videoDashManifest = null,
                audioDashManifest = null,
                resolution = "auto",
                isAdaptive = false,
                chapters = chapters
            )
        }

        throw IllegalStateException("No streams found for $youtubeId")
    }
}
