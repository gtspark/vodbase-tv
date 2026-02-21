package net.vodbase.tv.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.vodbase.tv.data.api.VodBaseApi
import net.vodbase.tv.data.model.SeriesInfo
import net.vodbase.tv.data.model.Vod
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VodRepository @Inject constructor(
    private val api: VodBaseApi
) {
    private val cache = ConcurrentHashMap<String, List<Vod>>()
    private val etags = ConcurrentHashMap<String, String>()
    private val fetchMutex = Mutex()

    suspend fun getVods(streamer: String, forceRefresh: Boolean = false): List<Vod> {
        if (!forceRefresh && cache.containsKey(streamer)) {
            return cache[streamer]!!
        }

        return fetchMutex.withLock {
            // Re-check after acquiring lock (another coroutine may have populated it)
            if (!forceRefresh && cache.containsKey(streamer)) {
                return@withLock cache[streamer]!!
            }

            val etag = if (forceRefresh) null else etags[streamer]
            val response = api.getVods(streamer, etag)

            if (response.code() == 304) {
                return@withLock cache[streamer] ?: emptyList()
            }

            if (response.isSuccessful) {
                val body = response.body()!!
                val vods = autoDetectSeries(body.vods)
                cache[streamer] = vods
                response.headers()["ETag"]?.let { etags[streamer] = it }
                return@withLock vods
            }

            cache[streamer] ?: emptyList()
        }
    }

    fun getVodById(streamer: String, vodId: String): Vod? {
        return cache[streamer]?.find { it.id == vodId }
    }

    /**
     * Returns a VOD from the cache if available; fetches the channel's VOD list first
     * if the cache is cold. Returns null only if the VOD is not found after fetching.
     */
    suspend fun getVodByIdOrFetch(streamer: String, vodId: String): Vod? {
        var found = getVodById(streamer, vodId)
        if (found == null) {
            getVods(streamer)
            found = getVodById(streamer, vodId)
        }
        return found
    }

    fun searchVods(streamer: String, query: String): List<Vod> {
        if (query.length < 2) return emptyList()
        val q = query.lowercase()
        return cache[streamer]?.filter { vod ->
            vod.title.lowercase().contains(q) ||
            (vod.description?.lowercase()?.contains(q) == true) ||
            (vod.gameContent?.lowercase()?.contains(q) == true)
        } ?: emptyList()
    }

    fun getSeriesVods(streamer: String, seriesName: String): List<Vod> {
        return cache[streamer]?.filter { it.series?.name == seriesName }
            ?.sortedBy { it.series?.part ?: 0 } ?: emptyList()
    }

    companion object {
        /**
         * Port of web's autoDetectSeries() from auto-detect-series.js.
         * Patterns are ordered by priority - Sips-specific first, then generic.
         * Dates are stripped from series names for consistent grouping.
         */
        private data class PatternDef(val regex: Regex, val type: String)

        private val seriesPatterns = listOf(
            // Sips: "Sips Plays Game (date) - #X - Title"
            PatternDef("""^.*?Plays\s+(.+?)\s+\([^)]+\)\s+-\s+#(\d+)\s+-\s+.+$""".toRegex(RegexOption.IGNORE_CASE), "sips"),
            // Sips: "Sips Plays Game (date) - #X"
            PatternDef("""^.*?Plays\s+(.+?)\s+\([^)]+\)\s+-\s+#(\d+)(?:\s+.*)?$""".toRegex(RegexOption.IGNORE_CASE), "sips"),
            // Jerma: "Series Name (Part X)"
            PatternDef("""^(.+?)\s+\(Part\s+(\d+)\)(?:\s+.*)?$""".toRegex(RegexOption.IGNORE_CASE), "part"),
            // "Game Name - Subtitle Part X" (with optional trailing text)
            PatternDef("""^(.+?)\s-\s(.+?)\sPart\s(\d+)(?:\s+.*)?$""".toRegex(RegexOption.IGNORE_CASE), "subtitle_part"),
            // "Game Name Part X"
            PatternDef("""^(.+?)\sPart\s(\d+)$""".toRegex(RegexOption.IGNORE_CASE), "part"),
            // "Streamer Plays Game - Subtitle Part X"
            PatternDef("""^(.*?Plays\s+.+?)\s-\s(.+?)\sPart\s(\d+)$""".toRegex(RegexOption.IGNORE_CASE), "subtitle_part"),
            // "Streamer Plays Game Part X"
            PatternDef("""^(.*?Plays\s+.+?)\sPart\s(\d+)$""".toRegex(RegexOption.IGNORE_CASE), "part"),
            // "Game Name - #X - Title"
            PatternDef("""^(.+?)\s+-\s+#(\d+)\s+-\s+.+$""".toRegex(RegexOption.IGNORE_CASE), "hash"),
            // "Game Name #X"
            PatternDef("""^(.+?)\s+#(\d+)$""".toRegex(RegexOption.IGNORE_CASE), "hash")
        )

        private fun stripDates(name: String): String {
            return name
                .replace("""\s+\([^)]+\)\s*-?\s*$""".toRegex(), "")
                .replace("""\s+\([^)]+\)""".toRegex(), "")
                .replace("""\s*\(\s*$""".toRegex(), "")
                .replace("""\s*\)\s*$""".toRegex(), "")
                .trim()
        }

        fun autoDetectSeries(vods: List<Vod>): List<Vod> {
            return vods.map { vod ->
                for (pattern in seriesPatterns) {
                    val match = pattern.regex.find(vod.title) ?: continue

                    val seriesName: String
                    val part: Int

                    when (pattern.type) {
                        "sips" -> {
                            seriesName = "Sips Plays ${match.groupValues[1].trim()}"
                            part = match.groupValues[2].toIntOrNull() ?: continue
                        }
                        "subtitle_part" -> {
                            seriesName = match.groupValues[1].trim()
                            part = match.groupValues[3].toIntOrNull() ?: continue
                        }
                        else -> {
                            seriesName = match.groupValues[1].trim()
                            part = match.groupValues[2].toIntOrNull() ?: continue
                        }
                    }

                    val cleanName = stripDates(seriesName)
                    if (cleanName.isNotEmpty() && part > 0) {
                        return@map vod.copy(series = SeriesInfo(cleanName, part))
                    }
                }
                vod // no match, keep original
            }
        }
    }
}
