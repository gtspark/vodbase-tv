package net.vodbase.tv.data.model

data class Vod(
    val id: String = "",
    val title: String = "",
    val url: String = "",
    val type: String = "",
    val platform: String? = null,
    val gameContent: String? = null,
    val duration: String = "0:00",
    val date: String? = null,
    val era: String = "",
    val thumbnail: String = "",
    val description: String? = null,
    val video_id: String? = null,
    val series: SeriesInfo? = null
) {
    val youtubeId: String get() = video_id ?: id.removePrefix("youtube_")

    val durationSeconds: Long get() {
        val parts = duration.split(":").reversed()
        var total = 0L
        parts.forEachIndexed { i, p ->
            total += (p.toLongOrNull() ?: 0) * when(i) {
                0 -> 1L
                1 -> 60L
                2 -> 3600L
                else -> 0L
            }
        }
        return total
    }
}

data class SeriesInfo(
    val name: String,
    val part: Int
)
