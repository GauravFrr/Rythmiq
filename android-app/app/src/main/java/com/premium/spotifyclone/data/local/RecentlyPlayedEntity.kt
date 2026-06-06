package com.premium.spotifyclone.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val audioUrl: String?,
    val durationMs: Long,
    val timestamp: Long
)
