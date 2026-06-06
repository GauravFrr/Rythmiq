package com.premium.spotifyclone.data.models

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val durationMs: Long,
    val audioUrl: String? = null,
    val isRecommended: Boolean = false,
    val artistId: String? = null
)
