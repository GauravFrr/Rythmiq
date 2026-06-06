package com.premium.spotifyclone.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 20")
    fun observeHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE trackId = :trackId")
    suspend fun deleteById(trackId: String)

    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}
