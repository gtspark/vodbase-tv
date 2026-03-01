package net.vodbase.tv.data.api

import retrofit2.Response
import retrofit2.http.*

data class VodsResponse(
    val vods: List<net.vodbase.tv.data.model.Vod>,
    val total: Int,
    val lastUpdated: String
)

data class QrCreateResponse(
    val success: Boolean,
    val token: String?
)

data class QrPollResponse(
    val status: String,  // "pending", "approved", "expired"
    val deviceToken: String?,
    val email: String?
)

data class ProgressResponse(
    val lastWatchedVideo: LastWatched?
)

data class LastWatched(
    val id: String,
    val title: String?,
    val currentTime: Double,
    val duration: Double,
    val resumeAvailable: Boolean
)

data class WatchedResponse(
    val watchedVideos: List<String>
)

interface VodBaseApi {
    @GET("/api/vodbase/vods/{streamer}")
    suspend fun getVods(
        @Path("streamer") streamer: String,
        @Header("If-None-Match") etag: String? = null
    ): Response<VodsResponse>

    @POST("/api/vodbase/qr-login/create")
    suspend fun createQrSession(): QrCreateResponse

    @GET("/api/vodbase/qr-login/poll/{token}")
    suspend fun pollQrSession(@Path("token") token: String): QrPollResponse

    @GET("/api/vodbase/progress/{theater}")
    suspend fun getProgress(
        @Path("theater") theater: String,
        @Header("X-Device-Token") token: String
    ): ProgressResponse

    @POST("/api/vodbase/progress/{theater}")
    suspend fun saveProgress(
        @Path("theater") theater: String,
        @Header("X-Device-Token") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<Any>

    @GET("/api/vodbase/watched/{streamer}")
    suspend fun getWatched(
        @Path("streamer") streamer: String,
        @Header("X-Device-Token") token: String
    ): WatchedResponse

    @POST("/api/vodbase/track-watch")
    suspend fun trackWatch(
        @Header("X-Device-Token") token: String,
        @Body body: Map<String, String>
    ): Response<Any>

    @DELETE("/api/vodbase/account")
    suspend fun deleteAccount(
        @Header("X-Device-Token") token: String
    ): Response<Any>
}
