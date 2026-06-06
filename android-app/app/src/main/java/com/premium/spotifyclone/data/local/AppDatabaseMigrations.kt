package com.premium.spotifyclone.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlists` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlist_tracks` (`rowId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `playlistId` TEXT NOT NULL, `trackId` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `coverUrl` TEXT NOT NULL, `durationMs` INTEGER NOT NULL, `audioUrl` TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_playlist_tracks_playlistId` ON `playlist_tracks` (`playlistId`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_playlist_tracks_playlistId_trackId` ON `playlist_tracks` (`playlistId`, `trackId`)"
        )
    }
}
