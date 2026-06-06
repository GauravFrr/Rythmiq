package com.premium.spotifyclone.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Transaction
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observeAllWithTracks(): Flow<List<PlaylistWithCovers>>

    @Insert
    suspend fun insert(entity: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun updateName(id: String, name: String)

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PlaylistEntity?
}
