package com.premium.spotifyclone.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistTrackDao {

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY orderIndex ASC, rowId ASC")
    fun observeForPlaylist(playlistId: String): Flow<List<PlaylistTrackEntity>>

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun maxOrderIndex(playlistId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PlaylistTrackEntity): Long

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun deleteTrack(playlistId: String, trackId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylist(playlistId: String)
}
