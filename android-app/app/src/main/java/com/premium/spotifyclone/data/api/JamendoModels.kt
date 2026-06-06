package com.premium.spotifyclone.data.api

import com.google.gson.annotations.SerializedName

data class JamendoResponse(
    @SerializedName("headers") val headers: JamendoHeaders,
    @SerializedName("results") val results: List<JamendoTrack>
)

data class JamendoHeaders(
    @SerializedName("status") val status: String,
    @SerializedName("code") val code: Int,
    @SerializedName("error_message") val errorMessage: String,
    @SerializedName("results_count") val resultsCount: Int
)

data class JamendoTrack(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("duration") val duration: Int,
    @SerializedName("artist_id") val artistId: String,
    @SerializedName("artist_name") val artistName: String,
    @SerializedName("album_name") val albumName: String?,
    @SerializedName("image") val image: String,
    @SerializedName("audio") val audio: String
)
