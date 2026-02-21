package net.vodbase.tv.data.model

data class Vod(
    val id: String,
    val title: String,
    val url: String,
    val type: String,
    val platform: String,
    val gameContent: String?,
    val duration: String,
    val date: String,
    val era: String,
    val thumbnail: String,
    val description: String?,
    val video_id: String,
    val series: SeriesInfo? = null
) {
    val youtubeId: String get() = video_id

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
