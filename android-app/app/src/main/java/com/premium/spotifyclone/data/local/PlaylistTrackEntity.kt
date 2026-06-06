package com.premium.spotifyclone.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playlist_tracks",
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["playlistId", "trackId"], unique = true),
    ],
)
data class PlaylistTrackEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val playlistId: String,
    val trackId: String,
    val orderIndex: Int,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val durationMs: Long,
    val audioUrl: String,
)
