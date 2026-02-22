package net.vodbase.tv.data.model

import androidx.compose.runtime.Immutable

@Immutable
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

    /** Formats ISO date "2026-02-12T21:00:54Z" → "Feb 12, 2026" in local timezone */
    val formattedDate: String? get() {
        val d = date ?: return null
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val parsed = sdf.parse(d) ?: return d
            val out = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US)
            out.format(parsed)
        } catch (_: Exception) { d }
    }

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

@Immutable
data class SeriesInfo(
    val name: String,
    val part: Int
)
