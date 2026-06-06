package com.premium.spotifyclone.data.repository

import com.premium.spotifyclone.data.local.AppDatabase
import com.premium.spotifyclone.data.local.SearchHistoryEntity
import com.premium.spotifyclone.data.models.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SearchHistoryRepository(database: AppDatabase) {
    private val dao = database.searchHistoryDao()

    fun observeHistory(): Flow<List<Track>> = dao.observeHistory().map { entities ->
        entities.map { e ->
            Track(
                id = e.trackId,
                title = e.title,
                artist = e.artist,
                coverUrl = e.coverUrl,
                durationMs = e.durationMs,
                audioUrl = e.audioUrl
            )
        }
    }

    suspend fun addToHistory(track: Track) {
        dao.insert(
            SearchHistoryEntity(
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                coverUrl = track.coverUrl ?: "",
                durationMs = track.durationMs,
                audioUrl = track.audioUrl ?: "",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeFromHistory(trackId: String) {
        dao.deleteById(trackId)
    }

    suspend fun clearHistory() {
        dao.clearAll()
    }
}
