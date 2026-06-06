package com.premium.spotifyclone.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LikedTrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        SearchHistoryEntity::class,
        RecentlyPlayedEntity::class
    ],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun likedTrackDao(): LikedTrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistTrackDao(): PlaylistTrackDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun recentlyPlayedDao(): RecentlyPlayedDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            val i = instance
            if (i != null) return i

            return synchronized(this) {
                val i2 = instance
                if (i2 != null) {
                    i2
                } else {
                    val created = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "spotify_clone.db"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    instance = created
                    created
                }
            }
        }
    }
}
