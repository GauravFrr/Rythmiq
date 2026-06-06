package com.premium.spotifyclone.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "liked_tracks")
data class LikedTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val durationMs: Long,
    val audioUrl: String,
)
