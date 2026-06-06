package com.premium.spotifyclone.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val trackId: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val durationMs: Long,
    val audioUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)
