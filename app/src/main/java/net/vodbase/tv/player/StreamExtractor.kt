package net.vodbase.tv.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream
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

        // Prefer progressive (video+audio) streams, fallback to adaptive
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

        // Adaptive: separate video + audio
        val videoOnly = info.videoOnlyStreams
            .sortedByDescending { it.resolution?.replace("p", "")?.toIntOrNull() ?: 0 }
        val audioOnly = info.audioStreams
            .sortedByDescending { it.averageBitrate }

        if (videoOnly.isNotEmpty()) {
            return@withContext ExtractedStream(
                videoUrl = videoOnly.first().content,
                audioUrl = audioOnly.firstOrNull()?.content,
                resolution = videoOnly.first().resolution ?: "unknown",
                isAdaptive = true
            )
        }

        throw IllegalStateException("No streams found for $youtubeId")
    }
}
