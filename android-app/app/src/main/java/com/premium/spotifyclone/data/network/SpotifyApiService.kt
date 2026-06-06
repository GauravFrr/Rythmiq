package com.premium.spotifyclone.data.network

import com.premium.spotifyclone.data.models.Track
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.Response
import okhttp3.ResponseBody
import com.premium.spotifyclone.data.api.SyncRequest

interface SpotifyApiService {
    @GET("/api/tracks/recent")
    suspend fun getRecentlyPlayed(): List<Track>

    @GET("/api/recommendations")
    suspend fun getMadeForYou(
        @Query("userId") userId: String? = null,
        @Query("limit") limit: Int = 15
    ): List<Track>

    @GET("/api/recommendations")
    suspend fun getNextRecommendations(
        @Query("currentSongId") currentSongId: String,
        @Query("excludeIds") excludeIds: String,
        @Query("limit") limit: Int = 10
    ): List<Track>

    @POST("/api/plays/log")
    suspend fun logPlay(@Body playEvent: PlayEvent): PlayLogResponse

    @GET("/api/music/search")
    suspend fun searchMusic(@Query("q") query: String): List<Track>

    @GET("/api/music/searchAll")
    suspend fun searchAll(@Query("q") query: String): SearchAllResponse

    @GET("/api/music/artist")
    suspend fun getArtistTracks(@Query("name") name: String): List<Track>

    @GET("/api/music/album")
    suspend fun getAlbumTracks(@Query("id") id: String): List<Track>

    @GET("/api/music/playlist")
    suspend fun getPlaylistTracks(@Query("id") id: String): List<Track>

    @GET("/api/music/recommend")
    suspend fun getRecommendedSongs(@Query("id") id: String): List<Track>

    @POST("/api/auth/sync")
    suspend fun syncUser(@Body body: SyncRequest): Response<ResponseBody>

    @GET("/api/auth/check-username")
    suspend fun checkUsername(@Query("username") username: String): Response<UsernameAvailabilityResponse>

    @GET("/api/music/stream")
    suspend fun getStreamUrl(@Query("q") searchKey: String): StreamResponse

    // ── History / Recommendation API ────────────────────────────────────
    @POST("/api/history/log")
    suspend fun logPlay(
        @retrofit2.http.Body body: LogPlayRequest
    ): retrofit2.Response<LogPlayResponse>

    // ── Artist Follow API ─────────────────────────────────────────────────
    @GET("/api/artist/is-following")
    suspend fun isFollowingArtist(
        @Query("artistId") artistId: String
    ): FollowStatusResponse

    @POST("/api/artist/follow")
    suspend fun followArtist(
        @retrofit2.http.Body body: FollowArtistRequest
    ): FollowStatusResponse

    @DELETE("/api/artist/follow")
    suspend fun unfollowArtist(
        @retrofit2.http.Body body: FollowArtistRequest
    ): FollowStatusResponse

    @GET("/api/artist/following")
    suspend fun getFollowingArtists(): List<FollowedArtist>
}

data class PlayEvent(
    val songId: String,
    val songName: String,
    val artistId: String?,
    val artistName: String,
    val albumId: String?,
    val genre: String?,
    val language: String?,
    val duration: Int,
    val listenedDuration: Int,
    val skipped: Boolean,
    val liked: Boolean,
    val addedToPlaylist: Boolean,
    val hourOfDay: Int,
    val dayOfWeek: Int,
    val source: String
)

data class PlayLogResponse(
    val success: Boolean,
    val completionRate: Float?
)

data class SearchAllResponse(
    val songs: List<Track>,
    val artists: List<SearchEntity>,
    val albums: List<SearchEntity>,
    val playlists: List<SearchEntity>
)

data class SearchEntity(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String
)

data class StreamResponse(
    val streamUrl: String,
    val videoId: String? = null
)

data class LogPlayRequest(
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val coverUrl: String? = null,
    val language: String? = "hindi",
    val genre: String? = "bollywood",
    val durationMs: Long = 0,
    val playDurationMs: Long = 0,
    val isSkip: Boolean = false,
    val source: String = "home"
)

data class LogPlayResponse(
    val success: Boolean,
    val message: String? = null
)

data class FollowArtistRequest(
    val artistId: String,
    val artistName: String,
    val imageUrl: String? = null
)

data class FollowStatusResponse(
    val following: Boolean,
    val success: Boolean? = null
)

data class FollowedArtist(
    @com.google.gson.annotations.SerializedName("artist_id") val artistId: String,
    @com.google.gson.annotations.SerializedName("artist_name") val artistName: String,
    @com.google.gson.annotations.SerializedName("image_url") val imageUrl: String?
)

data class UsernameAvailabilityResponse(
    val available: Boolean
)
