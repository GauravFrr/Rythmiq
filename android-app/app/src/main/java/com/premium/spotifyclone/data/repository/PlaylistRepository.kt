package com.premium.spotifyclone.data.repository

import com.premium.spotifyclone.data.local.AppDatabase
import com.premium.spotifyclone.data.local.PlaylistEntity
import com.premium.spotifyclone.data.local.PlaylistTrackEntity
import com.premium.spotifyclone.data.local.PlaylistWithCovers
import com.premium.spotifyclone.data.models.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class PlaylistRepository(database: AppDatabase) {

    private val playlistDao = database.playlistDao()
    private val trackDao = database.playlistTrackDao()

    fun observePlaylists(): Flow<List<PlaylistEntity>> = playlistDao.observeAll()

    fun observePlaylistsWithCovers(): Flow<List<PlaylistWithCovers>> =
        playlistDao.observeAllWithTracks()

    fun observePlaylistTracks(playlistId: String): Flow<List<Track>> =
        trackDao.observeForPlaylist(playlistId).map { rows ->
            rows.map { e ->
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

    suspend fun getPlaylistName(id: String): String? = playlistDao.getById(id)?.name

    suspend fun createPlaylist(name: String): String {
        val id = UUID.randomUUID().toString()
        val label = name.trim().ifBlank { "My playlist" }
        playlistDao.insert(PlaylistEntity(id = id, name = label))
        return id
    }

    suspend fun renamePlaylist(id: String, name: String) {
        playlistDao.updateName(id, name.trim().ifBlank { "Playlist" })
    }

    suspend fun deletePlaylist(id: String) {
        trackDao.clearPlaylist(id)
        playlistDao.deleteById(id)
    }

    suspend fun addTrack(playlistId: String, track: Track) {
        val url = track.audioUrl ?: return
        val next = trackDao.maxOrderIndex(playlistId) + 1
        trackDao.insert(
            PlaylistTrackEntity(
                playlistId = playlistId,
                trackId = track.id,
                orderIndex = next,
                title = track.title,
                artist = track.artist,
                coverUrl = track.coverUrl,
                durationMs = track.durationMs,
                audioUrl = url
            )
        )
    }

    suspend fun removeTrack(playlistId: String, trackId: String) {
        trackDao.deleteTrack(playlistId, trackId)
    }
}
