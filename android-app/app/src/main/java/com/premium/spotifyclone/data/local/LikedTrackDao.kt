package com.premium.spotifyclone.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LikedTrackDao {

    @Query("SELECT * FROM liked_tracks ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<LikedTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LikedTrackEntity)

    @Query("DELETE FROM liked_tracks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM liked_tracks WHERE id = :id")
    fun observeLikeCount(id: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM liked_tracks WHERE id = :id")
    suspend fun countById(id: String): Int

    /** Single DB transaction — avoids extra round-trips and lock churn when liking from the player UI. */
    @Transaction
    suspend fun toggleLike(entity: LikedTrackEntity): Boolean {
        return if (countById(entity.id) > 0) {
            deleteById(entity.id)
            false
        } else {
            insert(entity)
            true
        }
    }
}
