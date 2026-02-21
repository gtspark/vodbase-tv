package net.vodbase.tv.data.repository

import net.vodbase.tv.data.api.VodBaseApi
import net.vodbase.tv.data.model.Vod
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VodRepository @Inject constructor(
    private val api: VodBaseApi
) {
    private val cache = mutableMapOf<String, List<Vod>>()
    private val etags = mutableMapOf<String, String>()

    suspend fun getVods(streamer: String, forceRefresh: Boolean = false): List<Vod> {
        if (!forceRefresh && cache.containsKey(streamer)) {
            return cache[streamer]!!
        }

        val etag = if (forceRefresh) null else etags[streamer]
        val response = api.getVods(streamer, etag)

        if (response.code() == 304) {
            return cache[streamer] ?: emptyList()
        }

        if (response.isSuccessful) {
            val body = response.body()!!
            cache[streamer] = body.vods
            response.headers()["ETag"]?.let { etags[streamer] = it }
            return body.vods
        }

        return cache[streamer] ?: emptyList()
    }

    fun getVodById(streamer: String, vodId: String): Vod? {
        return cache[streamer]?.find { it.id == vodId }
    }

    fun getVodsByEra(streamer: String, era: String): List<Vod> {
        return cache[streamer]?.filter { it.era == era } ?: emptyList()
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
}
