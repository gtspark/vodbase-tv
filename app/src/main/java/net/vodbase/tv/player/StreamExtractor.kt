package net.vodbase.tv.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import javax.inject.Inject
import javax.inject.Singleton

data class ExtractedStream(
    val videoUrl: String,
    val audioUrl: String?,
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

        // Prefer adaptive (video-only + separate audio) for highest resolution (1080p+)
        val videoOnly = info.videoOnlyStreams
            .sortedByDescending { it.resolution?.replace("p", "")?.toIntOrNull() ?: 0 }
        val audioOnly = info.audioStreams
            .sortedByDescending { it.averageBitrate }

        if (videoOnly.isNotEmpty() && audioOnly.isNotEmpty()) {
            val best = videoOnly.first()
            return@withContext ExtractedStream(
                videoUrl = best.content,
                audioUrl = audioOnly.first().content,
                resolution = best.resolution ?: "unknown",
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
                resolution = best.resolution ?: "unknown",
                isAdaptive = false
            )
        }

        throw IllegalStateException("No streams found for $youtubeId")
    }
}
