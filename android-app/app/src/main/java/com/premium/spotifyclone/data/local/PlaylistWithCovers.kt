package com.premium.spotifyclone.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class PlaylistWithCovers(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlistId"
    )
    val tracks: List<PlaylistTrackEntity>
)
