package com.premium.spotifyclone.data.repository

import com.premium.spotifyclone.data.local.RecentlyPlayedDao
import com.premium.spotifyclone.data.local.RecentlyPlayedEntity
import com.premium.spotifyclone.data.models.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecentlyPlayedRepository(private val dao: RecentlyPlayedDao) {

    fun observeRecentlyPlayed(): Flow<List<Track>> {
        return dao.getRecentlyPlayedHistory().map { entities ->
            entities.map { entity ->
                Track(
                    id = entity.id,
                    title = entity.title,
                    artist = entity.artist,
                    coverUrl = entity.coverUrl,
                    audioUrl = entity.audioUrl,
                    durationMs = entity.durationMs
                )
            }
        }
    }

    suspend fun addTrack(track: Track) {
        val entity = RecentlyPlayedEntity(
            id = track.id,
            title = track.title,
            artist = track.artist,
            coverUrl = track.coverUrl,
            audioUrl = track.audioUrl,
            durationMs = track.durationMs,
            timestamp = System.currentTimeMillis()
        )
        dao.insertTrack(entity)
        dao.trimHistory() // Keep only 20
    }
}
